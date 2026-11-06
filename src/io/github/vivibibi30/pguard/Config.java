package io.github.vivibibi30.pguard;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 配置。
 *
 * UI 侧优先通过 XposedService#getRemotePreferences 读写；服务未连接时回退到本地
 * SharedPreferences。hook 侧通过 XposedInterface#getRemotePreferences 读取同一组
 * 远程偏好；读取失败时退回各规则的默认值（与 UI 默认值一致），不会静默放行。
 */
public final class Config {

    public static final String PKG = "io.github.vivibibi30.pguard";
    public static final String PREFS = "pg";
    public static final String VERSION = "1.1.1";

    /* 全局 */
    public static final String K_MASTER = "master";
    public static final String K_PATH = "block_path_markers";
    public static final String K_SNIFF = "sniff_all";

    /* 分组总开关 */
    public static final String K_G_SYSHELPER = "g_syshelper";
    public static final String K_G_GUARD = "g_guardprovider";
    public static final String K_G_TRUST = "g_trustservice";
    public static final String K_G_ANALYTICS = "g_analytics";
    public static final String K_G_AIASST = "g_aiasst";
    public static final String K_G_SECCENTER = "g_seccenter";
    public static final String K_G_PKG = "g_packageinstaller";
    public static final String K_G_AD_MARKET = "g_ad_market";
    public static final String K_G_AD_XMSF = "g_ad_xmsf";

    /* SystemHelper */
    public static final String K_SYSHELPER = "block_syshelper";

    /* guardprovider */
    public static final String K_GUARD_APPLIST = "block_guard_applist";
    public static final String K_GUARD_AIGC = "block_guard_aigc";
    public static final String K_GUARD_FRAUD = "block_guard_fraud";

    /* trustservice */
    public static final String K_TRUST = "block_trustservice";

    /* analytics */
    public static final String K_ANALYTICS = "block_analytics";
    public static final String K_STRICT = "strict_all";
    public static final String K_ANALYTICS_DOH = "block_analytics_doh";
    public static final String K_ANALYTICS_ALLNET = "analytics_all_net";
    public static final String K_SYSHELPER_ALLNET = "syshelper_all_net";

    /* 翻译组件 */
    public static final String K_AIASST = "block_aiasst";

    /* 安全中心 */
    public static final String K_SECCENTER = "block_securitycenter";
    public static final String K_SC_VIRUS = "block_sc_virus";
    public static final String K_SC_FRAMEWORK = "block_sc_framework";
    public static final String K_SC_DATA = "block_sc_data";
    public static final String K_SC_APPCLASS = "block_sc_appclass";
    public static final String K_SC_VOLUNTEER = "block_sc_volunteer";
    public static final String K_SC_LOCATION = "block_sc_location";
    public static final String K_SC_ADB = "block_sc_adb";
    public static final String K_GP_CLOUDSCAN = "block_gp_cloudscan";
    public static final String K_TRUST_FIND = "block_trust_find";
    public static final String K_G_REGISTRATION = "g_registration";
    public static final String K_REG_ZZHC = "block_reg_zzhc";
    public static final String K_REG_TYPDM = "block_reg_typdm";
    public static final String K_XSOF = "block_xsof";

    /* 安装器 */
    public static final String K_PKG_AVL = "block_packageinstaller_avl";
    public static final String K_PKG_STAT = "block_packageinstaller_stat";
    public static final String K_AD_PKG = "block_ad_pkg";

    /* 广告 */
    public static final String K_AD_MARKET = "block_ad_market";
    public static final String K_AD_XMSF = "block_ad_xmsf";

    /**
     * hook 侧读取的共享偏好。由 HookEntry 在目标进程里通过
     * XposedInterface#getRemotePreferences(group) 绑定；未绑定时退回默认值。
     */
    private static volatile SharedPreferences hookPrefs;

    private Config() {
    }

    public static void bindHookPrefs(SharedPreferences prefs) {
        hookPrefs = prefs;
    }

    public static boolean hasHookPrefs() {
        return hookPrefs != null;
    }

    /* ---------------- UI 侧 ---------------- */

    public static SharedPreferences ui(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static void uiSet(Context ctx, String key, boolean v) {
        ui(ctx).edit().putBoolean(key, v).apply();
    }

    /* ---------------- Hook 侧 ---------------- */

    public static boolean masterOn() {
        return get(K_MASTER, true);
    }

    public static boolean on(String key, boolean fallback) {
        return get(key, fallback);
    }

    public static boolean strict() {
        return get(K_STRICT, false);
    }

    private static boolean get(String key, boolean fallback) {
        SharedPreferences prefs = hookPrefs;
        if (prefs == null) return fallback;
        try {
            return prefs.getBoolean(key, fallback);
        } catch (Throwable ignored) {
            return fallback;
        }
    }
}
