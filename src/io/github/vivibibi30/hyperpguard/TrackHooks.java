package io.github.vivibibi30.hyperpguard;

import java.lang.reflect.Modifier;

import io.github.libxposed.api.XposedModule;

/**
 * 组件内嵌 OneTrack 埋点 SDK 的统一收口。
 *
 * OneTrack 是小米的统计 SDK，被 systemAdSolution / wmsvc / aicr / 输入法 / 商店等
 * 多个组件各自内嵌一份（类名都是 com.xiaomi.onetrack.OneTrack）。所有事件最终都从
 * track / adTrack 系列的 void 方法出去：拦掉它们，事件不进入 SDK 的队列，也就不会
 * 落盘、不会在下次联网时补发。
 *
 * 只拦 void 返回的方法：getter、isDisable、配置类方法保持原样，避免组件把它当成
 * 「SDK 未初始化」而走异常分支。
 */
final class TrackHooks {

    private TrackHooks() {
    }

    /** 返回挂上的方法数。类不存在时记一条 miss，不抛异常。 */
    static int install(XposedModule m, ClassLoader cl) {
        int n = 0;
        try {
            Class<?> c = Class.forName("com.xiaomi.onetrack.OneTrack", false, cl);
            for (java.lang.reflect.Method method : c.getDeclaredMethods()) {
                String name = method.getName();
                if (!name.startsWith("track") && !name.startsWith("adTrack")) continue;
                if (method.getReturnType() != void.class) continue;
                if (Modifier.isAbstract(method.getModifiers())) continue;
                try {
                    m.hook(method).intercept(chain -> null);
                    n++;
                } catch (Throwable ignored) {
                }
            }
            Logs.i("onetrack hooked=" + n);
        } catch (Throwable t) {
            Logs.i("onetrack miss: " + t);
        }
        return n;
    }
}
