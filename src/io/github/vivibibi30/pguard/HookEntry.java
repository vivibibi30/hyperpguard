package io.github.vivibibi30.pguard;

import android.content.SharedPreferences;
import android.os.Bundle;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;

/**
 * 模块入口（libxposed 新 API）。
 *
 * 所有 hook 在 onPackageReady 里安装，按包名分发。
 * 拦截分三类：
 *   1) 网络层：URL / InetAddress / Socket
 *   2) 方法层：直接返回固定值
 *   3) XSOF：Provider / AIDL / 写库 / 评分
 */
public class HookEntry extends XposedModule {

    private static final String TAG = "PrivacyGuard";

    private static final class Rule {
        final String host;
        final String path;

        Rule(String host) {
            this(host, null);
        }

        Rule(String host, String path) {
            this.host = host;
            this.path = path;
        }
    }

    private static final AtomicBoolean XSOF_HIT = new AtomicBoolean(false);
    private static final java.util.concurrent.ConcurrentHashMap<String, Long> SEEN =
            new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    public void onModuleLoaded(XposedModuleInterface.ModuleLoadedParam param) {
        log("module loaded: " + param.getProcessName());
        bindRemotePrefs();
    }

    @Override
    public void onPackageReady(XposedModuleInterface.PackageReadyParam param) {
        final String pkg = param.getPackageName();
        final ClassLoader cl = param.getClassLoader();
        if (pkg == null || cl == null) return;
        bindRemotePrefs();
        if (!masterOn()) return;

        try {
            dispatch(pkg, cl);
        } catch (Throwable t) {
            log("dispatch fail " + pkg + ": " + t);
        }
    }

    private void dispatch(String pkg, ClassLoader cl) {
        if ("com.mobiletools.systemhelper".equals(pkg)) {
            if (!Config.on(Config.K_G_SYSHELPER, true)) return;
            log("attach systemhelper");
            hookPrivacyApis(cl, false, false, true, false, false);
            hookMethodReturn(cl, "c.d.a.b.f", "a",
                    new Class<?>[]{String.class, byte[].class, byte[].class}, Boolean.FALSE);
            hookMethodReturn(cl, "c.d.a.b.e", "a", new Class<?>[]{String.class}, null);
            List<Rule> r = new ArrayList<>();
            r.add(new Rule("dm.wo.com.cn"));
            // 只上报设备/SIM/基站/应用列表，无用户功能：整进程断网
            guard(pkg, cl, "systemhelper", r, Config.on(Config.K_SYSHELPER_ALLNET, true));
            return;
        }

        if ("com.miui.guardprovider".equals(pkg)) {
            List<Rule> r = new ArrayList<>();
            if (Config.on(Config.K_GUARD_APPLIST, true)) r.add(new Rule("flash.sec.miui.com"));
            if (Config.on(Config.K_GUARD_AIGC, true)) r.add(new Rule("detection-aigc.engine.miui.com"));
            if (Config.on(Config.K_GUARD_FRAUD, true)) r.add(new Rule("fraud-detection.engine.miui.com"));
            if (Config.on(Config.K_GP_CLOUDSCAN, false)) r.add(new Rule("virus-engine.sec.miui.com"));
            if (r.isEmpty()) return;
            log("attach guardprovider, rules=" + r.size());
            hookPrivacyApis(cl, true, false, true, false, false);
            guard(pkg, cl, "guardprovider", r, false);
            return;
        }

        if ("com.xiaomi.trustservice".equals(pkg)) {
            if (!Config.on(Config.K_G_TRUST, true)) return;
            log("attach trustservice");
            hookMrm(cl);
            List<Rule> r = new ArrayList<>();
            r.add(new Rule("deviceapi.micloud.xiaomi.net"));
            if (Config.on(Config.K_TRUST_FIND, false)) {
                r.add(new Rule("find.api.micloud.xiaomi.net"));
            }
            guard(pkg, cl, "trustservice", r, false);
            return;
        }

        if ("com.miui.analytics".equals(pkg)) {
            if (!Config.on(Config.K_ANALYTICS, true)) return;
            log("attach analytics");
            hookAnalyticsMethods(cl);
            hookPrivacyApis(cl, true, true, true, false, true);
            List<Rule> r = new ArrayList<>();
            // 统计进程整域拦截：tracking(.miui.com) / 区域与 om.tracking / sdkconfig 全覆盖
            r.add(new Rule("miui.com"));
            r.add(new Rule("xiaomi.com"));
            r.add(new Rule("xiaomi.net"));
            // 印度/区域备份域不在上面三段内
            r.add(new Rule("onetrack.mi.com"));
            // 海外事件通道
            r.add(new Rule("pubsub.googleapis.com"));
            if (Config.on(Config.K_ANALYTICS_DOH, true)) {
                // 国际版在特定策略下用 DoH 解析后直连 IP，绕过按域名的规则
                r.add(new Rule("dns.google"));
                r.add(new Rule("8.8.8.8"));
                r.add(new Rule("8.8.4.4"));
            }
            // 纯统计进程：整进程断网，不依赖域名名单随云端变化
            guard(pkg, cl, "analytics", r, Config.on(Config.K_ANALYTICS_ALLNET, true));
            return;
        }

        if ("com.xiaomi.aiasst.vision".equals(pkg)) {
            if (!Config.on(Config.K_AIASST, false)) return;
            log("attach aiasst.vision");
            List<Rule> r = new ArrayList<>();
            r.add(new Rule("xiaomixiaoai.com", "/speech/"));
            guard(pkg, cl, "aiasst", r, false);
            return;
        }

        if ("com.miui.securitycenter".equals(pkg)) {
            if (!Config.on(Config.K_G_SECCENTER, true)) return;
            log("attach securitycenter");
            hookPrivacyApis(cl, false, false, true, false, false);
            if (Config.on(Config.K_XSOF, true)) hookXsof(cl);
            List<Rule> r = new ArrayList<>();
            r.add(new Rule("fraud-detection.engine.miui.com"));
            r.add(new Rule("flash.sec.miui.com", "/detect/"));
            if (Config.on(Config.K_SC_VIRUS, false)) {
                r.add(new Rule("virus-engine.sec.miui.com"));
            }
            if (Config.on(Config.K_SC_FRAMEWORK, false)) {
                r.add(new Rule("framework.sec.miui.com"));
            }
            if (Config.on(Config.K_SC_DATA, false)) {
                r.add(new Rule("data.sec.miui.com"));
            }
            if (Config.on(Config.K_SC_APPCLASS, false)) {
                // 上传非系统应用包名数组，用于防窥分类
                r.add(new Rule("flash.sec.miui.com", "/user/cat"));
            }
            if (Config.on(Config.K_SC_VOLUNTEER, true)) {
                // 志愿者传感器：OAID + 经纬度 + 时间，非志愿者无影响
                r.add(new Rule("api.sec.miui.com", "/earthquake/volunteer/sensor/upload"));
            }
            if (Config.on(Config.K_SC_LOCATION, false)) {
                // 地震 LBS / 白名单 / 志愿者登记 / 报警定位 / EPS
                r.add(new Rule("api.sec.miui.com", "/LBS/"));
                r.add(new Rule("api.sec.miui.com", "/earthquake/volunteer/"));
                r.add(new Rule("srv.sec.miui.com", "/earthquake/"));
                r.add(new Rule("gateway-papa.cloud.weizhi.com"));
                r.add(new Rule("eps.caict.ac.cn"));
            }
            if (Config.on(Config.K_SC_ADB, false)) {
                r.add(new Rule("srv.sec.miui.com", "/data/adb"));
            }
            guard(pkg, cl, "securitycenter", r, false);
            return;
        }

        if ("com.xiaomi.registration".equals(pkg)) {
            if (!Config.on(Config.K_G_REGISTRATION, true)) return;
            log("attach registration");
            hookPrivacyApis(cl, false, false, true, true, false);
            List<Rule> r = new ArrayList<>();
            if (Config.on(Config.K_REG_ZZHC, false)) {
                // 设备注册：IMEI/IMSI/ICCID/型号/网络，JSON 仅 Base64
                r.add(new Rule("zzhc.vnet.cn"));
            }
            if (Config.on(Config.K_REG_TYPDM, true)) {
                // 明文 HTTP 上传 CTEI/IMEI/MAC/BSSID
                r.add(new Rule("typdm.tydevice.com"));
            }
            if (r.isEmpty()) return;
            guard(pkg, cl, "registration", r, false);
            return;
        }

        if ("com.miui.packageinstaller".equals(pkg)) {
            if (!Config.on(Config.K_G_PKG, true)) return;
            List<Rule> r = new ArrayList<>();
            boolean pathOn = Config.on(Config.K_PATH, true);
            if (Config.on(Config.K_PKG_AVL, true)) {
                r.add(new Rule("api.installer.xiaomi.com", "/avl/upload"));
            }
            if (pathOn && Config.on(Config.K_PKG_STAT, true)) {
                r.add(new Rule("api.installer.xiaomi.com", "/statistics/appactive"));
            }
            if (pathOn && Config.on(Config.K_AD_PKG, true)) {
                r.add(new Rule("api.installer.xiaomi.com", "/info/layout"));
            }
            if (r.isEmpty()) return;
            log("attach packageinstaller, rules=" + r.size());
            guard(pkg, cl, "packageinstaller", r, false);
            return;
        }

        if ("com.xiaomi.market".equals(pkg)) {
            if (!Config.on(Config.K_G_AD_MARKET, true)) return;
            List<Rule> r = new ArrayList<>();
            r.add(new Rule("sdkconfig.ad.xiaomi.com"));
            guard(pkg, cl, "market", r, false);
            return;
        }

        if ("com.xiaomi.xmsf".equals(pkg)) {
            if (!Config.on(Config.K_G_AD_XMSF, true)) return;
            List<Rule> r = new ArrayList<>();
            r.add(new Rule("adv.sec.miui.com"));
            guard(pkg, cl, "xmsf", r, false);
        }
    }

    /* ---------------- 方法层 ---------------- */

    private void hookAnalyticsMethods(ClassLoader cl) {
        // 旧版 analytics：类名随版本变化，存在才挂，不存在只留 miss 日志
        hookMethodReturn(cl, "com.miui.analytics.onetrack.p.u", "w",
                new Class<?>[]{long.class, long.class}, null);
        hookMethodReturn(cl, "com.miui.analytics.c.f.k", "y",
                new Class<?>[]{long.class, long.class}, null);
        hookMethodReturn(cl, "com.miui.analytics.onetrack.q.c", "a",
                new Class<?>[]{android.content.Context.class}, Integer.valueOf(0));
        hookMethodReturn(cl, "com.miui.analytics.onetrack.q.c", "b",
                new Class<?>[]{android.content.Context.class}, "NONE");
        hookMethodReturn(cl, "com.miui.analytics.onetrack.q.c", "c",
                new Class<?>[0], Boolean.FALSE);
        // 6.19.0：应用使用记录聚合（app_start / app_end / app_duration / radio）
        hookMethodReturn(cl, "com.miui.analytics.d.f.n", "y",
                new Class<?>[]{long.class, long.class}, null);
        // 6.33.0 起包名整体混淆成 a.b.a.*，类名逐版变化，改为按签名扫描
        hookAnalyticsUsageByScan(cl);
    }

    /**
     * 版本无关的兜底：遍历 analytics 的 dex，把「(long, long) -> JSONObject」的方法全部置空。
     * 6.19.0 是 d.f.n#y/z，6.33.0 变成 a.b.a.e.h0.*，签名保持一致的都能命中。
     */
    private void hookAnalyticsUsageByScan(ClassLoader cl) {
        int scanned = 0, hooked = 0;
        try {
            java.util.List<String> names = new ArrayList<>();
            Class<?> bdcl = Class.forName("dalvik.system.BaseDexClassLoader", false, cl);
            java.lang.reflect.Field fPathList = bdcl.getDeclaredField("pathList");
            fPathList.setAccessible(true);
            Object pathList = fPathList.get(cl);
            if (pathList == null) return;
            java.lang.reflect.Field fElems = pathList.getClass().getDeclaredField("dexElements");
            fElems.setAccessible(true);
            Object[] elems = (Object[]) fElems.get(pathList);
            for (Object el : elems) {
                if (el == null) continue;
                try {
                    java.lang.reflect.Field fDex = el.getClass().getDeclaredField("dexFile");
                    fDex.setAccessible(true);
                    Object dex = fDex.get(el);
                    if (dex instanceof dalvik.system.DexFile) {
                        java.util.Enumeration<String> en = ((dalvik.system.DexFile) dex).entries();
                        while (en.hasMoreElements() && names.size() < 20000) {
                            names.add(en.nextElement());
                        }
                    }
                } catch (Throwable ignored) {
                }
            }
            for (String n : names) {
                if (!n.startsWith("com.miui.analytics") && !n.startsWith("a.b.a.")) continue;
                Class<?> c;
                try {
                    c = Class.forName(n, false, cl);
                } catch (Throwable t) {
                    continue;
                }
                scanned++;
                for (Method m : c.getDeclaredMethods()) {
                    Class<?>[] p = m.getParameterTypes();
                    if (p.length != 2 || p[0] != long.class || p[1] != long.class) continue;
                    if (m.getReturnType() != org.json.JSONObject.class) continue;
                    try {
                        hook(m).intercept(chain -> null);
                        hooked++;
                    } catch (Throwable ignored) {
                    }
                }
            }
            log("analytics scan scanned=" + scanned + " hooked=" + hooked);
        } catch (Throwable t) {
            log("analytics scan fail: " + t);
        }
    }

    /**
     * 隐私 API 层：从「数据来源」截断，而不是只截网络出口。
     * 全部按框架公开签名挂，版本无关；挂不上只记 miss。
     */
    private void hookPrivacyApis(ClassLoader cl, boolean appList, boolean usage,
                                 boolean ids, boolean sms, boolean xmsf) {
        if (appList) {
            hookMethodReturn(cl, "android.app.ApplicationPackageManager",
                    "getInstalledPackages", new Class<?>[]{int.class}, new ArrayList<Object>());
            hookMethodReturn(cl, "android.app.ApplicationPackageManager",
                    "getInstalledApplications", new Class<?>[]{int.class}, new ArrayList<Object>());
        }
        if (usage) {
            hookMethodReturn(cl, "android.app.usage.UsageStatsManager",
                    "queryEvents", new Class<?>[]{long.class, long.class}, null);
            hookMethodReturn(cl, "android.app.usage.UsageStatsManager",
                    "queryUsageStats", new Class<?>[]{int.class, long.class, long.class},
                    new ArrayList<Object>());
        }
        if (ids) {
            hookMethodReturn(cl, "android.telephony.TelephonyManager", "getDeviceId",
                    new Class<?>[0], null);
            hookMethodReturn(cl, "android.telephony.TelephonyManager", "getImei",
                    new Class<?>[0], null);
            hookMethodReturn(cl, "android.telephony.TelephonyManager", "getMeid",
                    new Class<?>[0], null);
            hookMethodReturn(cl, "android.telephony.TelephonyManager", "getSubscriberId",
                    new Class<?>[0], null);
            hookMethodReturn(cl, "android.telephony.TelephonyManager", "getSimSerialNumber",
                    new Class<?>[0], null);
        }
        if (sms) {
            hookMethodReturn(cl, "android.telephony.SmsManager", "sendTextMessage",
                    new Class<?>[]{String.class, String.class, String.class, String.class,
                            android.app.PendingIntent.class}, null);
            hookMethodReturn(cl, "android.telephony.SmsManager", "sendMultipartTextMessage",
                    new Class<?>[]{String.class, String.class, java.util.List.class,
                            java.util.List.class, java.util.List.class}, null);
            hookMethodReturn(cl, "android.telephony.SmsManager", "sendDataMessage",
                    new Class<?>[]{String.class, String.class, short.class, byte[].class,
                            android.app.PendingIntent.class}, null);
        }
        if (xmsf) {
            hookXmsfBroadcast(cl, "android.content.ContextWrapper");
            hookXmsfBroadcast(cl, "android.content.ContextImpl");
        }
    }

    /** analytics 把材料广播给 XMSF 代发，这里在发出侧掐掉 */
    private void hookXmsfBroadcast(ClassLoader cl, String cls) {
        try {
            Class<?> c = Class.forName(cls, false, cl);
            Method m = c.getDeclaredMethod("sendBroadcast", android.content.Intent.class);
            hook(m).intercept(chain -> {
                Object a = chain.getArg(0);
                if (a instanceof android.content.Intent) {
                    String act = ((android.content.Intent) a).getAction();
                    if ("com.xiaomi.xmsf.push.XMSF_UPLOAD_ACTIVE".equals(act)) {
                        log("block XMSF upload broadcast");
                        return null;
                    }
                }
                return chain.proceed();
            });
            log("hook ok " + cls + "#sendBroadcast(XMSF 过滤)");
        } catch (Throwable t) {
            log("hook miss " + cls + "#sendBroadcast -> " + t);
        }
    }

    private void hookMrm(ClassLoader cl) {
        try {
            Class<?> sm = Class.forName("android.os.ServiceManager", false,
                    ClassLoader.getSystemClassLoader());
            for (Method m : sm.getDeclaredMethods()) {
                if (!m.getName().equals("getService") && !m.getName().equals("waitForService")) continue;
                if (m.getParameterCount() != 1) continue;
                hook(m).intercept(chain -> {
                    Object a0 = chain.getArg(0);
                    if (a0 instanceof String && ((String) a0).toLowerCase().contains("mrm")) {
                        return null;
                    }
                    return chain.proceed();
                });
            }
            log("mrm hooked");
        } catch (Throwable t) {
            log("mrm hook miss: " + t);
        }
    }

    private void hookXsof(ClassLoader cl) {
        try {
            Class<?> prov = Class.forName("com.xiaomi.security.xsof.XSOFProvider", false, cl);
            hookAllByName(prov, "query", chain -> {
                if (XSOF_HIT.compareAndSet(false, true)) log("xsof query blocked (first)");
                return new android.database.MatrixCursor(new String[]{"_id"});
            });
            hookAllByName(prov, "call", chain -> new Bundle());
            hookAllByName(prov, "insert", chain -> null);
            log("xsof provider hooked");
        } catch (Throwable t) {
            log("xsof provider miss: " + t);
        }

        try {
            Class<?> impl = Class.forName(
                    "com.xiaomi.security.xsof.MiSafetyDetectService$MiSafetyDetectImpl", false, cl);
            int n = 0;
            for (Method m : impl.getDeclaredMethods()) {
                Class<?>[] pts = m.getParameterTypes();
                if (pts.length < 2) continue;
                if (!pts[pts.length - 1].getName().contains("IMiSafetyDetectCallback")) continue;
                hook(m).intercept(chain -> {
                    Object cb = chain.getArg(chain.getArgs().size() - 1);
                    if (cb != null) {
                        try {
                            Method g2 = cb.getClass().getMethod("G2", String.class, int.class);
                            g2.invoke(cb, null, Integer.valueOf(1017));
                        } catch (Throwable ignored) {
                        }
                    }
                    return null;
                });
                n++;
            }
            log("xsof aidl hooked=" + n);
        } catch (Throwable t) {
            log("xsof aidl miss: " + t);
        }

        try {
            Class<?> rk = Class.forName("Ra.k", false, cl);
            int n = 0;
            for (Method m : rk.getDeclaredMethods()) {
                if (m.getReturnType() != void.class) continue;
                if (!java.lang.reflect.Modifier.isPublic(m.getModifiers())) continue;
                Class<?>[] pts = m.getParameterTypes();
                if (pts.length == 0) continue;
                if (!pts[0].getName().startsWith("Qa.")) continue;
                hook(m).intercept(chain -> null);
                n++;
            }
            log("xsof db write hooked=" + n);
        } catch (Throwable t) {
            log("xsof db write miss: " + t);
        }

        try {
            Class<?> fd = Class.forName("Pa.b", false, cl);
            int n = 0;
            for (Method m : fd.getDeclaredMethods()) {
                if (m.getParameterCount() != 3) continue;
                if (m.getReturnType() != float.class) continue;
                hook(m).intercept(chain -> Float.valueOf(0f));
                n++;
            }
            log("xsof score hooked=" + n);
        } catch (Throwable t) {
            log("xsof score miss: " + t);
        }
    }

    /** 把方法整体替换为固定返回值 */
    private void hookMethodReturn(ClassLoader cl, String cls, String name,
                                  Class<?>[] types, Object value) {
        try {
            Class<?> c = Class.forName(cls, false, cl);
            Method m = c.getDeclaredMethod(name, types);
            hook(m).intercept(chain -> value);
            log("hook ok " + cls + "#" + name);
        } catch (Throwable t) {
            log("hook miss " + cls + "#" + name + " -> " + t);
        }
    }

    private interface Interceptor {
        Object intercept(XposedInterface.Chain chain) throws Throwable;
    }

    private void hookAllByName(Class<?> c, String name, Interceptor interceptor) {
        for (Method m : c.getDeclaredMethods()) {
            if (!m.getName().equals(name)) continue;
            hook(m).intercept(interceptor::intercept);
        }
    }

    /* ---------------- 网络层 ---------------- */

    private void guard(final String pkg, ClassLoader cl, final String tag,
                       final List<Rule> rules, final boolean strictAll) {
        final boolean pathOn = Config.on(Config.K_PATH, true);
        final boolean sniffOn = Config.on(Config.K_SNIFF, false);

        try {
            Method open = URL.class.getDeclaredMethod("openConnection");
            hook(open).intercept(chain -> {
                URL u = (URL) chain.getThisObject();
                String host = u.getHost();
                if (host != null && check(tag, host, u.getPath(), rules, strictAll, pathOn, sniffOn)) {
                    throw new java.io.IOException("blocked by PrivacyGuard: " + host);
                }
                return chain.proceed();
            });
        } catch (Throwable t) {
            log(tag + " url hook miss: " + t);
        }

        for (String n : new String[]{"getByName", "getAllByName"}) {
            try {
                Method m = InetAddress.class.getDeclaredMethod(n, String.class);
                hook(m).intercept(chain -> {
                    String host = (String) chain.getArg(0);
                    if (host != null && check(tag, host, null, rules, strictAll, pathOn, false)) {
                        throw new java.net.UnknownHostException("blocked by PrivacyGuard: " + host);
                    }
                    return chain.proceed();
                });
            } catch (Throwable t) {
                log(tag + " dns hook miss: " + t);
            }
        }

        try {
            for (Constructor<?> ctor : java.net.Socket.class.getDeclaredConstructors()) {
                hook(ctor).intercept(chain -> {
                    String host = null;
                    for (Object a : chain.getArgs()) {
                        if (a instanceof InetSocketAddress) {
                            host = ((InetSocketAddress) a).getHostString();
                            break;
                        }
                        if (a instanceof String) {
                            host = (String) a;
                            break;
                        }
                        if (a instanceof InetAddress) {
                            host = ((InetAddress) a).getHostAddress();
                            break;
                        }
                    }
                    if (host != null && check(tag, host, null, rules, strictAll, false, false)) {
                        throw new java.io.IOException("blocked by PrivacyGuard: " + host);
                    }
                    return chain.proceed();
                });
            }
            for (Method m : java.net.Socket.class.getDeclaredMethods()) {
                if (!m.getName().equals("connect")) continue;
                if (m.getParameterCount() < 1) continue;
                if (!SocketAddress.class.isAssignableFrom(m.getParameterTypes()[0])) continue;
                hook(m).intercept(chain -> {
                    String host = null;
                    Object a0 = chain.getArg(0);
                    if (a0 instanceof InetSocketAddress) {
                        host = ((InetSocketAddress) a0).getHostString();
                    }
                    if (host != null && check(tag, host, null, rules, strictAll, false, false)) {
                        throw new java.io.IOException("blocked by PrivacyGuard: " + host);
                    }
                    return chain.proceed();
                });
            }
        } catch (Throwable t) {
            log(tag + " socket hook miss: " + t);
        }
    }

    private boolean check(String tag, String host, String path, List<Rule> rules,
                          boolean strictAll, boolean pathOn, boolean sniffOn) {
        String h = host.toLowerCase();

        if (strictAll) {
            if (!isLocal(h)) {
                log(tag + " X(strict) " + h);
                return true;
            }
            return false;
        }

        for (Rule r : rules) {
            if (!hostMatch(h, r.host)) continue;
            if (r.path == null) {
                log(tag + " X(host) " + h);
                return true;
            }
            if (pathOn && path != null && path.startsWith(r.path)) {
                log(tag + " X(host+path) " + h + path);
                return true;
            }
        }

        if (sniffOn) sniff(tag, h, path);
        return false;
    }

    private void sniff(String tag, String host, String path) {
        String key = tag + "|" + host;
        long now = System.currentTimeMillis();
        Long last = SEEN.get(key);
        if (last != null && now - last < 60000L) return;
        SEEN.put(key, now);
        log("? " + tag + " " + host + (path == null ? "" : path));
    }

    private static boolean hostMatch(String h, String suffix) {
        String s = suffix.toLowerCase();
        return h.equals(s) || h.endsWith("." + s);
    }

    /**
     * 只把合法的本机/私网 IP 字面量视为“本地”。
     * 旧实现用 host.startsWith("10.") 之类的字符串前缀判断，
     * 会让 10.evil.com / 192.168.evil.com / 172.16.evil.com / anything.local
     * 在 strictAll 模式下被误放行。
     */
    private static boolean isLocal(String h) {
        if (h == null) return false;
        String host = h.trim().toLowerCase();
        if (host.endsWith(".")) host = host.substring(0, host.length() - 1);
        if (host.startsWith("[") && host.endsWith("]")) {
            host = host.substring(1, host.length() - 1);
        }
        int zone = host.indexOf('%'); // IPv6 zone id, e.g. fe80::1%wlan0
        if (zone >= 0) host = host.substring(0, zone);
        if (host.equals("localhost")) return true;
        if (host.startsWith("::ffff:")) return isLocal(host.substring(7));

        if (host.indexOf(':') < 0) {
            String[] parts = host.split("\\.");
            if (parts.length != 4) return false;
            try {
                int a = Integer.parseInt(parts[0]);
                int b = Integer.parseInt(parts[1]);
                int c = Integer.parseInt(parts[2]);
                int d = Integer.parseInt(parts[3]);
                if (a < 0 || a > 255 || b < 0 || b > 255
                        || c < 0 || c > 255 || d < 0 || d > 255) {
                    return false;
                }
                return a == 127 || a == 10
                        || (a == 192 && b == 168)
                        || (a == 172 && b >= 16 && b <= 31)
                        || (a == 169 && b == 254);
            } catch (NumberFormatException e) {
                return false;
            }
        }

        return host.equals("::1") || host.startsWith("fe80:")
                || host.startsWith("fc") || host.startsWith("fd");
    }

    /* ---------------- 工具 ---------------- */

    /**
     * 读取 UI 侧通过 LSPosed 暴露的远程偏好。每个目标进程绑定一次；
     * 失败时保持 null，Config 会退回默认值（与旧行为一致，不会造成新的放行）。
     */
    private void bindRemotePrefs() {
        if (Config.hasHookPrefs()) return;
        try {
            SharedPreferences prefs = getRemotePreferences(Config.PREFS);
            if (prefs != null) {
                Config.bindHookPrefs(prefs);
                log("remote prefs bound");
            } else {
                log("remote prefs unavailable, using defaults");
            }
        } catch (Throwable t) {
            log("remote prefs fail: " + t);
        }
    }

    private boolean masterOn() {
        return Config.masterOn();
    }

    private void log(String s) {
        try {
            android.util.Log.i(TAG, s);
        } catch (Throwable ignored) {
        }
    }
}
