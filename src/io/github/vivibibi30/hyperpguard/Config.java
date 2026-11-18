package io.github.vivibibi30.hyperpguard;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 配置。
 *
 * UI 侧通过普通 SharedPreferences 读写；hook 侧在新 API 下无法直接读应用私有目录，
 * 因此使用各规则的默认值（与 UI 的默认值一致）。细粒度开关由「应用是否在作用域内」
 * 加默认规则共同决定。
 */
public final class Config {

    public static final String PKG = "io.github.vivibibi30.hyperpguard";
    public static final String PREFS = "pg";
    public static final String VERSION = "1.8.9";

    /* 全局 */
    public static final String K_MASTER = "master";
    public static final String K_PATH = "block_path_markers";
    public static final String K_SNIFF = "sniff_all";
    /** 自定义规则：域名 或 域名/路径，多条用 ;; 分隔 */
    public static final String K_CUSTOM = "custom_rules";

    /* 敏感数据：按应用独立，默认全部关闭 */
    public static final String K_LOC_SH = "loc_sh";       // SystemHelper 定位
    public static final String K_CELL_SH = "cell_sh";     // SystemHelper 基站
    public static final String K_WIFI_SH = "wifi_sh";     // SystemHelper Wi-Fi 扫描
    public static final String K_LOC_REG = "loc_reg";     // 运营商注册 定位
    public static final String K_CELL_REG = "cell_reg";   // 运营商注册 基站
    public static final String K_LOC_GP = "loc_gp";       // 反诈 定位
    public static final String K_WIFI_GP = "wifi_gp";     // 反诈 Wi-Fi 扫描
    public static final String K_SOCIAL_GP = "social_gp"; // 反诈 通讯录 / 通话
    public static final String K_LOC_SC = "loc_sc";       // 安全中心 定位
    public static final String K_SOCIAL_SC = "social_sc"; // 安全中心 通讯录 / 通话 / 短信
    public static final String K_CELL_DM = "cell_dm";     // 移动 DM 基站
    public static final String K_WIFI_DM = "wifi_dm";     // 移动 DM Wi-Fi 扫描

    /* 分组总开关 */
    public static final String K_G_IME_SOGOU = "g_ime_sogou";
    public static final String K_G_IME_MITYPE = "g_ime_mitype";
    public static final String K_G_IME_BAIDU = "g_ime_baidu";
    public static final String K_G_IME_IFLYTEK = "g_ime_iflytek";
    public static final String K_G_IME_WETYPE = "g_ime_wetype";
    public static final String K_G_IME_DOUBAO = "g_ime_doubao";
    public static final String K_G_SYSHELPER = "g_syshelper";
    public static final String K_G_GUARD = "g_guardprovider";
    public static final String K_G_TRUST = "g_trustservice";
    public static final String K_G_ANALYTICS = "g_analytics";
    public static final String K_G_AIASST = "g_aiasst";
    public static final String K_G_SECCENTER = "g_seccenter";
    public static final String K_G_PKG = "g_packageinstaller";
    public static final String K_G_AD_MARKET = "g_ad_market";
    public static final String K_G_AD_XMSF = "g_ad_xmsf";
    public static final String K_G_MSA = "g_msa";
    public static final String K_G_WM = "g_wmsvc";
    public static final String K_G_AICR = "g_aicr";

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
    public static final String K_ANALYTICS_DOH = "block_analytics_doh";
    public static final String K_ANALYTICS_ALLNET = "analytics_all_net";
    public static final String K_SYSHELPER_ALLNET = "syshelper_all_net";

    /* MSA 广告组件 */
    /** 广告使用画像：查询应用今日是否打开、最后使用时间 */
    public static final String K_MSA_USAGE = "block_msa_usage";
    /** 广告埋点：OneTrack 的 track / adTrack 方法 */
    public static final String K_MSA_TRACK = "block_msa_track";
    /** 开屏广告：requestSplashScreen 系列返回空 Intent */
    public static final String K_MSA_SPLASH = "block_msa_splash";

    /* 网络管理服务（com.miui.wmsvc） */
    /** OneTrack 诊断埋点 + tracking.miui.com */
    public static final String K_WM_TRACK = "block_wmsvc_track";

    /* AI 内容识别（com.xiaomi.aicr） */
    /** OneTrack 埋点 + tracking.miui.com */
    public static final String K_AICR_TRACK = "block_aicr_track";

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
    public static final String K_SC_DBGUARD = "block_sc_dbguard";
    /** 安全中心的设备标识默认放行：拦了 IMSI / ICCID / 手机号，套餐与流量查询会失效 */
    public static final String K_SC_IDS = "block_sc_ids";
    /** 安全中心的广告位、网络助手推送与满意度问卷 */
    public static final String K_SC_MISC = "block_sc_misc";
    /** 安全中心的 OneTrack 统计埋点（tracking.*.miui.com / sdkconfig.*.xiaomi.com） */
    public static final String K_SC_TRACKING = "block_sc_tracking";
    /** guardprovider 的安犬引擎日志上传 */
    public static final String K_GP_ENGINELOG = "block_gp_enginelog";
    public static final String K_GP_CLOUDSCAN = "block_gp_cloudscan";
    public static final String K_GP_SCREENGUARD = "block_gp_screenguard";
    public static final String K_TRUST_FIND = "block_trust_find";
    public static final String K_G_REGISTRATION = "g_registration";
    public static final String K_G_DMREG = "g_dmregservice";
    public static final String K_G_MISIGHT = "g_misightservice";
    public static final String K_MISIGHT_UPLOAD = "block_misight_upload";
    public static final String K_MISIGHT_MQSAS = "block_misight_mqsas";
    public static final String K_MISIGHT_KEFU = "block_misight_kefu";
    /** MiSight 事件日志上报域名 */
    public static final String K_MISIGHT_EVENTLOG = "block_misight_eventlog";
    public static final String K_DMREG = "block_dmreg";
    public static final String K_REG_ZZHC = "block_reg_zzhc";
    public static final String K_REG_TYPDM = "block_reg_typdm";
    public static final String K_XSOF = "block_xsof";

    /* 安装器 */
    public static final String K_PKG_AVL = "block_packageinstaller_avl";
    public static final String K_PKG_STAT = "block_packageinstaller_stat";
    public static final String K_AD_PKG = "block_ad_pkg";
    /** 安装器的 MSA OAID 获取 */
    public static final String K_PKG_OAID = "block_pkg_oaid";
    /** 安装器云端参数下发（含新未注册应用的备案提示） */
    public static final String K_PKG_CLOUD = "block_pkg_cloud";
    /** 安装时的云端风险校验与游戏防篡改检查（默认不拦，影响检测功能） */
    public static final String K_PKG_RISK = "block_pkg_risk";
    public static final String K_PKG_GAME = "block_pkg_game";

    /* 广告 */
    public static final String K_AD_MARKET = "block_ad_market";
    public static final String K_AD_XMSF = "block_ad_xmsf";
    /** 应用商店 / 服务框架自带的 OneTrack 埋点，路径 /track/ */
    public static final String K_MARKET_TRACK = "block_market_track";
    public static final String K_XMSF_TRACK = "block_xmsf_track";

    /* 搜狗输入法小米版 */
    /** 统计上报：kpi2 与 instantpb 两个域名，同属搜狗自有统计链路 */
    public static final String K_SOGOU_PINGBACK = "block_sogou_pingback";
    /** 剪贴板采集：ClipboardLogHelper */
    public static final String K_SOGOU_CLIPBOARD = "block_sogou_clipboard";
    /** 1.6.12 的两个统计域名键：1.6.13 起并入 K_SOGOU_PINGBACK，仅用于升级迁移 */
    @Deprecated
    public static final String K_SOGOU_KPI2_OLD = "block_sogou_kpi2";
    @Deprecated
    public static final String K_SOGOU_INSTANT_OLD = "block_sogou_instant";

    /* 小米输入法 */
    /** OneTrack 埋点：方法层 + tracking/sdkconfig 域名（自研击键统计无稳定域名，不在此列） */
    public static final String K_MITYPE_TRACK = "block_mitype_track";
    /** 1.6.12 的域名单独开关：1.6.13 起并入 K_MITYPE_TRACK，仅用于升级迁移 */
    @Deprecated
    public static final String K_MITYPE_HOSTS_OLD = "block_mitype_hosts";

    /* 百度输入法小米版 */
    /** com.baidu.stats 统计 SDK */
    public static final String K_BAIDU_STATS = "block_baidu_stats";
    /** 语音识别 ASR 采集与上传 */
    public static final String K_BAIDU_SPEECH = "block_baidu_speech";

    /* 讯飞输入法小米版 */
    /** com.iflytek.statssdk 统计 SDK */
    public static final String K_IFLYTEK_STATS = "block_iflytek_stats";

    /* 微信输入法（com.tencent.wetype） */
    /** 灯塔埋点 h.trace.qq.com 与配置/灰度下发（rdelivery / rconfig / release） */
    public static final String K_WETYPE_TRACK = "block_wetype_track";
    /** Bugly 崩溃上报与 rmonitor 性能监控，含本地攒表后批量上传 */
    public static final String K_WETYPE_CRASH = "block_wetype_crash";
    /** 云输入与词库/皮肤分发，默认关：关闭后云联想与词库同步不可用 */
    public static final String K_WETYPE_CLOUD = "block_wetype_cloud";

    /* 豆包输入法（com.bytedance.android.doubaoime） */
    /** AppLog 埋点（mon / log.zijieapi.com、i.snssdk.com） */
    public static final String K_DOUBAO_TRACK = "block_doubao_track";
    /** 配置、灰度与校验下发，默认关：关闭后配置下发可能失败 */
    public static final String K_DOUBAO_CONFIG = "block_doubao_config";
    /** 云输入服务，默认关：关闭后云联想不可用 */
    public static final String K_DOUBAO_CLOUD = "block_doubao_cloud";

    /**
     * 各开关的默认值：全仓唯一一份。
     *
     * 界面（MainActivity 的 Item 与开关行）和 hook 侧（HookEntry / NetGuard / PrivacyApis）
     * 都通过 def(key) / on(key) 取值，调用点不再写 true / false 字面量。此前的两份默认值
     * 漂过一次：1.6.12 把搜狗剪贴板的界面默认值改成关闭，hook 侧仍是 true，结果界面显示
     * 「关」而实际一直在拦截。
     */
    private static final java.util.Map<String, Boolean> DEFAULTS = new java.util.HashMap<>();

    static {
        // 全局
        DEFAULTS.put(K_MASTER, true);
        DEFAULTS.put(K_PATH, true);
        DEFAULTS.put(K_SNIFF, false);

        // 敏感数据：按应用独立，默认全部关闭
        DEFAULTS.put(K_LOC_SH, false);
        DEFAULTS.put(K_CELL_SH, false);
        DEFAULTS.put(K_WIFI_SH, false);
        DEFAULTS.put(K_LOC_REG, false);
        DEFAULTS.put(K_CELL_REG, false);
        DEFAULTS.put(K_LOC_GP, false);
        DEFAULTS.put(K_WIFI_GP, false);
        DEFAULTS.put(K_SOCIAL_GP, false);
        DEFAULTS.put(K_LOC_SC, false);
        DEFAULTS.put(K_SOCIAL_SC, false);
        DEFAULTS.put(K_CELL_DM, false);
        DEFAULTS.put(K_WIFI_DM, false);

        // 分组总开关
        DEFAULTS.put(K_G_IME_SOGOU, true);
        DEFAULTS.put(K_G_IME_MITYPE, true);
        DEFAULTS.put(K_G_IME_BAIDU, true);
        DEFAULTS.put(K_G_IME_IFLYTEK, true);
        DEFAULTS.put(K_G_SYSHELPER, true);
        DEFAULTS.put(K_G_GUARD, true);
        DEFAULTS.put(K_G_TRUST, true);
        DEFAULTS.put(K_G_ANALYTICS, true);
        DEFAULTS.put(K_G_AIASST, true);
        DEFAULTS.put(K_G_SECCENTER, true);
        DEFAULTS.put(K_G_PKG, true);
        DEFAULTS.put(K_G_AD_MARKET, true);
        DEFAULTS.put(K_G_AD_XMSF, true);
        DEFAULTS.put(K_G_MSA, true);
        DEFAULTS.put(K_G_WM, true);
        DEFAULTS.put(K_G_AICR, true);

        // SystemHelper
        DEFAULTS.put(K_SYSHELPER, true);

        // guardprovider
        DEFAULTS.put(K_GUARD_APPLIST, true);
        DEFAULTS.put(K_GUARD_AIGC, true);
        DEFAULTS.put(K_GUARD_FRAUD, true);

        // trustservice
        DEFAULTS.put(K_TRUST, false);

        // analytics
        DEFAULTS.put(K_ANALYTICS, true);
        DEFAULTS.put(K_ANALYTICS_DOH, true);
        DEFAULTS.put(K_ANALYTICS_ALLNET, true);
        DEFAULTS.put(K_SYSHELPER_ALLNET, false);

        // MSA 广告组件
        DEFAULTS.put(K_MSA_USAGE, true);
        DEFAULTS.put(K_MSA_TRACK, true);
        DEFAULTS.put(K_MSA_SPLASH, true);

        // 网络管理服务（com.miui.wmsvc）
        DEFAULTS.put(K_WM_TRACK, true);

        // AI 内容识别（com.xiaomi.aicr）
        DEFAULTS.put(K_AICR_TRACK, true);

        // 翻译组件
        DEFAULTS.put(K_AIASST, false);

        // 安全中心
        DEFAULTS.put(K_SECCENTER, true);
        DEFAULTS.put(K_SC_VIRUS, false);
        DEFAULTS.put(K_SC_FRAMEWORK, false);
        DEFAULTS.put(K_SC_DATA, false);
        DEFAULTS.put(K_SC_APPCLASS, false);
        DEFAULTS.put(K_SC_VOLUNTEER, true);
        DEFAULTS.put(K_SC_LOCATION, false);
        DEFAULTS.put(K_SC_ADB, false);
        DEFAULTS.put(K_SC_DBGUARD, true);
        DEFAULTS.put(K_SC_IDS, false);
        DEFAULTS.put(K_SC_MISC, true);
        DEFAULTS.put(K_SC_TRACKING, true);
        DEFAULTS.put(K_GP_ENGINELOG, true);
        DEFAULTS.put(K_GP_CLOUDSCAN, false);
        DEFAULTS.put(K_GP_SCREENGUARD, true);
        DEFAULTS.put(K_TRUST_FIND, false);
        DEFAULTS.put(K_G_REGISTRATION, true);
        DEFAULTS.put(K_G_DMREG, true);
        DEFAULTS.put(K_G_MISIGHT, true);
        DEFAULTS.put(K_MISIGHT_UPLOAD, true);
        DEFAULTS.put(K_MISIGHT_MQSAS, true);
        DEFAULTS.put(K_MISIGHT_KEFU, true);
        DEFAULTS.put(K_MISIGHT_EVENTLOG, true);
        DEFAULTS.put(K_DMREG, true);
        DEFAULTS.put(K_REG_ZZHC, false);
        DEFAULTS.put(K_REG_TYPDM, true);
        DEFAULTS.put(K_XSOF, true);

        // 安装器
        DEFAULTS.put(K_PKG_AVL, true);
        DEFAULTS.put(K_PKG_STAT, true);
        DEFAULTS.put(K_AD_PKG, true);
        DEFAULTS.put(K_PKG_OAID, true);
        DEFAULTS.put(K_PKG_CLOUD, true);
        DEFAULTS.put(K_PKG_RISK, false);
        DEFAULTS.put(K_PKG_GAME, false);

        // 广告
        DEFAULTS.put(K_AD_MARKET, true);
        DEFAULTS.put(K_AD_XMSF, true);
        DEFAULTS.put(K_MARKET_TRACK, true);
        DEFAULTS.put(K_XMSF_TRACK, true);

        // 搜狗输入法小米版
        DEFAULTS.put(K_SOGOU_PINGBACK, true);
        DEFAULTS.put(K_SOGOU_CLIPBOARD, false);

        // 小米输入法
        DEFAULTS.put(K_MITYPE_TRACK, true);

        // 百度输入法小米版
        DEFAULTS.put(K_BAIDU_STATS, true);
        DEFAULTS.put(K_BAIDU_SPEECH, false);

        // 讯飞输入法小米版
        DEFAULTS.put(K_IFLYTEK_STATS, true);
        // 微信输入法：只有纯上报项默认开，云输入与词库默认关
        DEFAULTS.put(K_G_IME_WETYPE, true);
        DEFAULTS.put(K_WETYPE_TRACK, true);
        DEFAULTS.put(K_WETYPE_CRASH, true);
        DEFAULTS.put(K_WETYPE_CLOUD, false);
        // 豆包输入法：同上，配置下发与云输入默认关
        DEFAULTS.put(K_G_IME_DOUBAO, true);
        DEFAULTS.put(K_DOUBAO_TRACK, true);
        DEFAULTS.put(K_DOUBAO_CONFIG, false);
        DEFAULTS.put(K_DOUBAO_CLOUD, false);
    }

    /** 某个键的默认值；未登记的键按 false 处理，并记一条日志便于自查 */
    public static boolean def(String key) {
        Boolean v = DEFAULTS.get(key);
        if (v == null) {
            Logs.i("config default missing: " + key);
            return false;
        }
        return v;
    }

    /* ---------------- 版本迁移 ---------------- */

    /** 迁移标记：每个迁移只跑一次。放在独立偏好里，不跟着 pg 一起同步进 Settings */
    private static final String K_MIGRATED_1613 = "migrated_1613";
    private static final String PREFS_MIGRATION = "pg_migration";

    /**
     * 1.6.13 把三个开关并进两个，旧键不再被读取。升级时把旧键上的选择搬过去：
     * 旧键里只要有一个被关掉，新键就取关——用户关过其中一条，说明他不想让这条链路继续。
     * 新键已经写过值时不动（说明升级后用户自己改过，以现值为准）。
     *
     * 迁移在模块界面启动时执行：hook 侧只读配置，没有写入口。
     */
    public static void migrateLegacyKeys(Context ctx) {
        SharedPreferences done = ctx.getSharedPreferences(PREFS_MIGRATION, Context.MODE_PRIVATE);
        SharedPreferences sp = ui(ctx);
        // 迁移标记不该跟着 pg 同步进 Settings：1.6.13 测试期内曾写在这里，见到就清掉
        if (sp.contains(K_MIGRATED_1613)) sp.edit().remove(K_MIGRATED_1613).apply();
        if (done.getBoolean(K_MIGRATED_1613, false)) return;
        mergeLegacy(ctx, sp, "sogou pingback",
                new String[]{K_SOGOU_KPI2_OLD, K_SOGOU_INSTANT_OLD}, K_SOGOU_PINGBACK);
        mergeLegacy(ctx, sp, "mitype hosts",
                new String[]{K_MITYPE_HOSTS_OLD}, K_MITYPE_TRACK);
        done.edit().putBoolean(K_MIGRATED_1613, true).apply();
    }

    private static void mergeLegacy(Context ctx, SharedPreferences sp, String what,
                                    String[] olds, String now) {
        if (sp.contains(now) || settingsValue(ctx, now) != null) {
            Logs.i("migrate skip " + what + ": " + now + " already set");
            return;
        }
        Boolean merged = null;
        for (String old : olds) {
            Boolean v = sp.contains(old) ? Boolean.valueOf(sp.getBoolean(old, false))
                    : settingsValue(ctx, old);
            if (v == null) continue;
            merged = merged == null ? v : Boolean.valueOf(merged && v);
        }
        if (merged == null) {
            Logs.i("migrate skip " + what + ": no legacy key");
            return;
        }
        Logs.i("migrate " + what + " -> " + now + " = " + merged);
        uiSet(ctx, now, merged);
    }

    /** Settings.Global 里那份配置的某个键；没有这个键时返回 null */
    private static Boolean settingsValue(Context ctx, String key) {
        try {
            String all = android.provider.Settings.Global.getString(
                    ctx.getContentResolver(), GLOBAL_KEY);
            if (all == null || all.isEmpty()) return null;
            for (String kv : all.split("\\|")) {
                int i = kv.indexOf('=');
                if (i > 0 && key.equals(kv.substring(0, i).trim())) {
                    return Boolean.valueOf("true".equals(kv.substring(i + 1).trim()));
                }
            }
        } catch (Throwable t) {
            Logs.i("settings read fail: " + key + " -> " + t);
        }
        return null;
    }

    private Config() {
    }

    /* ---------------- UI 侧 ---------------- */

    public static SharedPreferences ui(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static void uiSet(Context ctx, String key, boolean v) {
        SharedPreferences sp = ui(ctx);
        sp.edit().putBoolean(key, v).apply();
        invalidateCache();
        syncOut(ctx, sp, key);
    }

    /**
     * 把开关写进 Settings.Global 的一个自定义 key，目标进程读它。
     *
     * 以 Settings 里已有的键值打底，再叠加本地偏好与本次改动：
     * 本地偏好被清理过时，不会把其余开关一起冲成默认值。
     */
    private static void syncOut(Context ctx, SharedPreferences sp, String changed) {
        java.util.LinkedHashMap<String, String> map = new java.util.LinkedHashMap<>();
        String cur = null;
        try {
            cur = android.provider.Settings.Global.getString(
                    ctx.getContentResolver(), GLOBAL_KEY);
        } catch (Throwable t) {
            Logs.i("config read before sync fail: " + t);
        }
        if (cur != null && !cur.isEmpty()) {
            for (String kv : cur.split("\\|")) {
                int i = kv.indexOf('=');
                if (i <= 0) continue;
                String k = kv.substring(0, i).trim();
                String val = kv.substring(i + 1).trim();
                map.put(k, K_CUSTOM.equals(k) ? urlDecode(val) : val);
            }
        }
        for (java.util.Map.Entry<String, ?> e : sp.getAll().entrySet()) {
            Object v = e.getValue();
            if (v instanceof Boolean) map.put(e.getKey(), String.valueOf(v));
            else if (v instanceof String) map.put(e.getKey(), (String) v);
        }
        Object localChanged = sp.getAll().get(changed);
        map.put(changed, localChanged instanceof String
                ? (String) localChanged : String.valueOf(sp.getBoolean(changed, false)));

        final StringBuilder sb = new StringBuilder();
        for (java.util.Map.Entry<String, String> e : map.entrySet()) {
            String val = e.getValue();
            if (K_CUSTOM.equals(e.getKey())) {
                if (val == null || val.isEmpty()) continue;
                val = urlEncode(val);
            }
            if (sb.length() > 0) sb.append('|');
            sb.append(e.getKey()).append('=').append(val);
        }
        if (sb.length() == 0) return;
        final String dump = sb.toString();
        new Thread(() -> {
            Process p = null;
            try {
                p = Runtime.getRuntime().exec(new String[]{"su", "-c",
                        "settings put global " + GLOBAL_KEY + " '" + dump + "'"});
                p.waitFor();
            } catch (Throwable t) {
                Logs.i("config sync fail: " + t);
            } finally {
                if (p != null) try { p.destroy(); } catch (Throwable ignored) { }
            }
        }, "hyperpguard-config-sync").start();
    }

    /** 界面读取开关：本地没有时回落到 Settings，清过数据也不会显示成默认值 */
    public static boolean get(Context ctx, String key, boolean def) {
        SharedPreferences sp = ui(ctx);
        if (sp.contains(key)) return sp.getBoolean(key, def);
        Boolean remote = fallback(ctx).get(key);
        return remote != null ? remote : def;
    }

    /** Settings 里的整表，只在本地缺键时载入一次 */
    private static java.util.Map<String, Boolean> fallback(Context ctx) {
        if (FALLBACK != null) return FALLBACK;
        java.util.LinkedHashMap<String, Boolean> map = new java.util.LinkedHashMap<>();
        try {
            String v = android.provider.Settings.Global.getString(
                    ctx.getContentResolver(), GLOBAL_KEY);
            if (v != null && !v.isEmpty()) {
                for (String kv : v.split("\\|")) {
                    int i = kv.indexOf('=');
                    if (i <= 0) continue;
                    map.put(kv.substring(0, i).trim(), "true".equals(kv.substring(i + 1).trim()));
                }
            }
        } catch (Throwable t) {
            Logs.i("config fallback fail: " + t);
        }
        FALLBACK = map;
        return FALLBACK;
    }

    private static java.util.Map<String, Boolean> FALLBACK;

    /** 自定义规则原文，进程内只读一次 */
    private static volatile String CUSTOM = "";

    /** 界面改过开关后丢掉回落缓存，下次重新读 */
    public static void invalidateCache() {
        FALLBACK = null;
    }

    /* ---------------- Hook 侧 ---------------- */

    public static boolean masterOn() {
        return on(K_MASTER);
    }

    /** 唯一的对外写入：Settings.Global 下的一个自定义 key */
    public static final String GLOBAL_KEY = "hyperpguard_conf";

    /** hook 侧读到的开关，进程内缓存一份，读不到就用各规则的默认值 */
    private static final java.util.Map<String, Boolean> REMOTE =
            new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * 目标进程读取界面开关：查询本应用的只读 provider。
     * 不写文件、不写系统设置，查询失败时按默认值运行。
     */
    public static void load(android.content.Context ctx) {
        load(ctx, null);
    }

    /**
     * pkg 用于系统 Context 读配置被 AMS 拒绝时，换成该包自己的 Context 再读一次。
     * 系统 Context 的包名是 android，uid 与包名不一致会被 provider 拒。
     */
    public static void load(android.content.Context ctx, String pkg) {
        if (!REMOTE.isEmpty() || !CUSTOM.isEmpty()) return;
        if (ctx != null) loadFromSettings(ctx);
        if (REMOTE.isEmpty() && ctx != null && pkg != null) {
            try {
                loadFromSettings(ctx.createPackageContext(pkg, 0));
            } catch (Throwable t) {
                Logs.i("config pkg ctx fail: " + t);
            }
        }
        Logs.i(REMOTE.isEmpty() ? "config loaded: 0 (defaults)"
                : "config loaded: " + REMOTE.size());
    }

    /**
     * 热重载后强制重读：先清掉进程内缓存，再按当前配置读一遍。
     *
     * load() 是「没有就补」的语义，热重载时如果框架复用了同一个类加载器，
     * 缓存还在就会一直用旧开关，所以这里显式清空。
     */
    public static void reload(android.content.Context ctx) {
        REMOTE.clear();
        CUSTOM = "";
        invalidateCache();
        load(ctx);
    }

    private static void loadFromSettings(android.content.Context ctx) {
        try {
            String v = android.provider.Settings.Global.getString(
                    ctx.getContentResolver(), GLOBAL_KEY);
            if (v == null || v.isEmpty()) return;
            for (String kv : v.split("\\|")) {
                int i = kv.indexOf('=');
                if (i <= 0) continue;
                String k = kv.substring(0, i).trim();
                String val = kv.substring(i + 1).trim();
                if (K_CUSTOM.equals(k)) CUSTOM = urlDecode(val);
                else REMOTE.put(k, "true".equals(val));
            }
        } catch (Throwable t) {
            Logs.i("config read fail: " + t);
        }
    }

    public static void setCustomRules(Context ctx, String rules) {
        SharedPreferences sp = ui(ctx);
        sp.edit().putString(K_CUSTOM, rules).apply();
        invalidateCache();
        syncOut(ctx, sp, K_CUSTOM);
    }

    /** 界面读取：本地优先，被清过时回落到 Settings 里的同一份配置 */
    public static String customRulesRaw(Context ctx) {
        String v = ui(ctx).getString(K_CUSTOM, "");
        if (v != null && !v.isEmpty()) return v;
        try {
            String all = android.provider.Settings.Global.getString(
                    ctx.getContentResolver(), GLOBAL_KEY);
            if (all != null) {
                for (String kv : all.split("\\|")) {
                    int i = kv.indexOf('=');
                    if (i > 0 && K_CUSTOM.equals(kv.substring(0, i).trim())) {
                        return urlDecode(kv.substring(i + 1).trim());
                    }
                }
            }
        } catch (Throwable t) {
            Logs.i("custom rules read fail: " + t);
        }
        return "";
    }

    /** hook 侧解析：域名 或 域名/路径 */
    public static java.util.List<NetGuard.Rule> customRules() {
        java.util.List<NetGuard.Rule> out = new java.util.ArrayList<>();
        String s = CUSTOM;
        if (s == null || s.isEmpty()) return out;
        for (String item : s.split(";;")) {
            item = item.trim();
            if (item.isEmpty()) continue;
            if (item.indexOf('#') > 0) continue;   // 方法级规则，见 customMethodRules()
            int i = item.indexOf('/');
            if (i > 0) out.add(new NetGuard.Rule(item.substring(0, i), item.substring(i)));
            else out.add(new NetGuard.Rule(item));
        }
        return out;
    }

    /** 方法级规则：`类名#方法名`，方法名写 `*` 表示整类全部方法 */
    public static java.util.List<String> customMethodRules() {
        java.util.List<String> out = new java.util.ArrayList<>();
        String s = CUSTOM;
        if (s == null || s.isEmpty()) return out;
        for (String item : s.split(";;")) {
            item = item.trim();
            if (item.indexOf('#') <= 0 || item.endsWith("#")) continue;
            out.add(item);
        }
        return out;
    }

    private static String urlEncode(String s) {
        try {
            return java.net.URLEncoder.encode(s, "UTF-8");
        } catch (Throwable t) {
            return s;
        }
    }

    private static String urlDecode(String s) {
        try {
            return java.net.URLDecoder.decode(s, "UTF-8");
        } catch (Throwable t) {
            return s;
        }
    }

    public static int size() {
        return REMOTE.size();
    }

    /** hook 侧读取开关：配置里没有这个键时取 DEFAULTS 里的默认值 */
    public static boolean on(String key) {
        Boolean v = REMOTE.get(key);
        return v == null ? def(key) : v;
    }

    public static boolean strict() {
        return false;
    }
}
