package io.github.vivibibi30.hyperpguard;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import io.github.libxposed.service.HookedTarget;
import io.github.libxposed.service.XposedService;

/**
 * 开关热生效：把新配置推给已注入的进程，不 force-stop。
 *
 * 走 libxposed API 102 的热重载：框架在新的模块类加载器里重放
 * {@code onHotReloaded}，{@link HookEntry} 卸载旧 hook 后按当前开关重装一遍，
 * 配置也随之重读——和「模块更新后自动热重载」是同一条链路，只是由界面主动发起。
 *
 * 框架不支持（API &lt; 102）、目标进程正在退出或框架拒绝时，结果会回到界面，
 * 由用户改用「重启」兜底；没有在运行的进程不会被打扰，下次启动本来就会读到新配置。
 */
final class HotReload {

    /** 回调都在主线程；message 是第一个失败原因，可能为 null */
    interface Callback {
        void onResult(int done, int failed, boolean unsupported, String message);
    }

    /** 热重载请求带上的标记，HookEntry 用它把日志和「模块更新」区分开 */
    static final String EXTRA_REASON = "reason";
    static final String REASON_CONFIG = "config";

    /** 回调迟迟不来时按超时收尾，避免界面一直以为还在进行中 */
    private static final long TIMEOUT_MS = 8000;

    private final XposedService service;
    /** null 表示作用域内全部已注入进程 */
    private final List<String> pkgs;
    private final Callback callback;
    private final Handler ui = new Handler(Looper.getMainLooper());

    private final Runnable timeout = this::finish;

    private final AtomicInteger left = new AtomicInteger();
    private final AtomicInteger done = new AtomicInteger();
    private final AtomicInteger failed = new AtomicInteger();
    private final AtomicBoolean finished = new AtomicBoolean();
    private volatile boolean unsupported;
    private volatile String firstError;

    HotReload(XposedService service, List<String> pkgs, Callback callback) {
        this.service = service;
        this.pkgs = pkgs;
        this.callback = callback;
    }

    /**
     * 发起热重载。
     *
     * @return 收到请求的目标进程数；0 表示没有需要重载的运行中进程
     */
    int start() {
        final List<HookedTarget> targets = pick();
        if (targets.isEmpty()) return 0;
        left.set(targets.size());
        Logs.i("hot reload requested: " + targets.size() + " process(es)"
                + (pkgs == null ? "" : " " + pkgs));
        final Bundle extras = new Bundle();
        extras.putString(EXTRA_REASON, REASON_CONFIG);
        for (final HookedTarget target : targets) {
            try {
                service.hotReloadModule(target, extras, (t, result) -> deliver(t, result));
            } catch (Throwable e) {
                Logs.i("hot reload " + name(target) + " request fail: " + e);
                failed.incrementAndGet();
                if (firstError == null) firstError = String.valueOf(e);
                if (left.decrementAndGet() <= 0) finish();
            }
        }
        ui.postDelayed(timeout, TIMEOUT_MS);
        return targets.size();
    }

    /** 按包名挑出要重载的进程：pkgs 为空表示全部，正在重载的跳过 */
    private List<HookedTarget> pick() {
        final List<HookedTarget> out = new ArrayList<>();
        List<HookedTarget> all;
        try {
            all = service.getRunningTargets();
        } catch (Throwable t) {
            Logs.i("hot reload list fail: " + t);
            return out;
        }
        if (all == null) return out;
        for (HookedTarget t : all) {
            if (t == null) continue;
            final String process = t.getProcessName();
            if (process == null) continue;
            if (pkgs != null && !matches(process)) continue;
            HookedTarget.State state = null;
            try {
                state = t.getState();
            } catch (Throwable ignored) {
            }
            if (state == HookedTarget.State.RELOADING) continue;
            out.add(t);
        }
        return out;
    }

    /** 进程名比对：com.x 也要匹配它的子进程 com.x:remote */
    private boolean matches(String process) {
        for (String pkg : pkgs) {
            if (pkg == null) continue;
            if (process.equals(pkg) || process.startsWith(pkg + ":")) return true;
        }
        return false;
    }

    /** 框架回调在 binder 线程，转到主线程再统计 */
    private void deliver(HookedTarget target, Object result) {
        final String name = name(target);
        final String status = statusName(result);
        final String message = messageOf(result);
        // IN_PROGRESS 是中间态，等最终结果再计数
        if ("IN_PROGRESS".equals(status)) {
            Logs.i("hot reload " + name + ": in progress");
            return;
        }
        ui.post(() -> {
            Logs.i("hot reload " + name + ": " + status
                    + (message == null || message.isEmpty() ? "" : " " + message));
            if ("SUCCEEDED".equals(status)) {
                done.incrementAndGet();
            } else {
                failed.incrementAndGet();
                if ("UNSUPPORTED".equals(status)) unsupported = true;
                if (firstError == null && message != null && !message.isEmpty()) {
                    firstError = message;
                }
            }
            if (left.decrementAndGet() <= 0) finish();
        });
    }

    /**
     * HotReloadResult 是 record（Java 16+），本模块按 --release 11 编译读不到它的类文件，
     * 所以按访问器名取值。取不到时返回 "null"，按失败计并保留原始日志。
     */
    private static String statusName(Object result) {
        Object v = accessor(result, "status");
        return v == null ? "null" : String.valueOf(v);
    }

    private static String messageOf(Object result) {
        Object v = accessor(result, "message");
        return v instanceof String ? (String) v : null;
    }

    private static Object accessor(Object result, String method) {
        if (result == null) return null;
        try {
            return result.getClass().getMethod(method).invoke(result);
        } catch (Throwable t) {
            Logs.i("hot reload result " + method + " fail: " + t);
            return null;
        }
    }

    private static String name(HookedTarget target) {
        if (target == null) return "?";
        try {
            String n = target.getProcessName();
            return n == null ? "?" : n;
        } catch (Throwable t) {
            return "?";
        }
    }

    private void finish() {
        if (!finished.compareAndSet(false, true)) return;
        ui.removeCallbacks(timeout);
        final int d = done.get();
        final int f = failed.get();
        final int pending = left.get();
        if (pending > 0) Logs.i("hot reload timeout, no result from " + pending + " process(es)");
        callback.onResult(d, f + Math.max(pending, 0), unsupported, firstError);
    }
}
