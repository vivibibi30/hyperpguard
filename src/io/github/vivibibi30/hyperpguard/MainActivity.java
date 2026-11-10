package io.github.vivibibi30.hyperpguard;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;

import java.util.Collections;

import io.github.libxposed.service.XposedService;
import io.github.libxposed.service.XposedServiceHelper;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 卡片式开关面板。
 * 每组一个「启用」总开关（等同作用域开关），关闭时该组子项全部置灰不可操作。
 */
public class MainActivity extends Activity {

    private int BG = 0xFFF2F3F5;
    private int CARD = 0xFFFFFFFF;
    private int TEXT = 0xFF1A1A1A;
    private int SUB = 0xFF8A8A8E;
    private int LINE = 0xFFEFEFF0;
    private static final int ACCENT = 0xFF3478F6;
    private boolean nightMode;

    private static final String URL_REPO = "https://github.com/vivibibi30/hyperpguard";
    private static final String URL_CUSTOM_RULES_DOC =
            URL_REPO + "/blob/main/docs/CUSTOM-RULES.md";
    private static final String URL_ISSUES = URL_REPO + "/issues";
    private static final String URL_RELEASES = URL_REPO + "/releases";
    private static final String URL_MODULE =
            "https://modules.lsposed.org/module/io.github.vivibibi30.hyperpguard";
    private static final String API_LATEST =
            "https://api.github.com/repos/vivibibi30/hyperpguard/releases/latest";

    private static final class Item {
        final String name, desc, key;
        final boolean def;

        Item(String name, String desc, String key, boolean def) {
            this.name = name;
            this.desc = desc;
            this.key = key;
            this.def = def;
        }
    }

    private static final class Group {
        final String title, pkg, key, note;
        final Item[] items;

        Group(String title, String pkg, String key, Item[] items) {
            this(title, pkg, key, items, null);
        }

        Group(String title, String pkg, String key, Item[] items, String note) {
            this.title = title;
            this.pkg = pkg;
            this.key = key;
            this.items = items;
            this.note = note;
        }
    }

    /** 界面文案：中文原文查表，英文环境返回英文，查不到原样返回 */
    private static String T(String zh) {
        return I18n.t(zh);
    }

    /** 组总开关默认开启：状态由 LSPosed 真实作用域回填，不看本地默认值 */
    private static final Group[] GROUPS = {
            new Group(T("SystemHelper"), "com.mobiletools.systemhelper", Config.K_G_SYSHELPER, new Item[]{
                    new Item(T("拦截上报"), T("dm.wo.com.cn，设备 / SIM / 基站 / 应用列表"), Config.K_SYSHELPER, true),
                    new Item(T("整进程断网"), T("默认关：只拦上报域名与方法层。开启后该进程所有外网请求一并拦截，只留本机回环"), Config.K_SYSHELPER_ALLNET, false),
                    new Item(T("定位（GPS）"), T("只对本应用：位置查询返回空"), Config.K_LOC_SH, false),
                    new Item(T("基站信息"), T("只对本应用：基站查询返回空，不影响信号显示"), Config.K_CELL_SH, false),
                    new Item(T("Wi-Fi 扫描"), T("只对本应用：扫描列表置空，SSID / BSSID 清空"), Config.K_WIFI_SH, false),
            }, T("权限在开机激活时确认；本模块只拦上报，不回收已授予的权限")),
            new Group(T("系统安全组件"), "com.miui.guardprovider", Config.K_G_GUARD, new Item[]{
                    new Item(T("应用列表云查"), T("仅拦 flash.sec.miui.com，不动病毒库"), Config.K_GUARD_APPLIST, true),
                    new Item(T("AI 通话预警云检测"), T("detection-aigc.engine.miui.com"), Config.K_GUARD_AIGC, true),
                    new Item(T("反诈配置拉取"), T("fraud-detection.engine.miui.com"), Config.K_GUARD_FRAUD, true),
                    new Item(T("云查上传（Flamingo）"), T("virus-engine.sec.miui.com；关闭后不再上传应用清单，本地病毒库照常"), Config.K_GP_CLOUDSCAN, false),
                    new Item(T("静默录屏拦截"), T("阻止 createVirtualDisplay 整屏录制，不影响正常截屏"), Config.K_GP_SCREENGUARD, true),
                    new Item(T("引擎日志上传"), T("logupdate.avlyun.sec.miui.com；只影响日志回传，不影响本地查毒与病毒库更新"), Config.K_GP_ENGINELOG, true),
                    new Item(T("定位（GPS）"), T("只对本应用：位置查询返回空"), Config.K_LOC_GP, false),
                    new Item(T("Wi-Fi 扫描"), T("只对本应用：扫描列表置空，SSID / BSSID 清空"), Config.K_WIFI_GP, false),
                    new Item(T("通讯录 / 通话读取"), T("只对本应用：查询返回空；来电识别会受影响"), Config.K_SOCIAL_GP, false),
            }, T("AI 反诈逻辑在本组件的 AiGuardService 内，用于通话场景；SystemHelper 只做运营商侧上报")),
            new Group(T("安全中心"), "com.miui.securitycenter", Config.K_G_SECCENTER, new Item[]{
                    new Item(T("XSOF 行为查询拦截"), T("第三方查不到截图 / 通话 / 屏幕共享记录"), Config.K_XSOF, true),
                    new Item(T("反诈配置拉取"), T("fraud-detection.engine.miui.com"), Config.K_SECCENTER, true),
                    new Item(T("病毒库云查"), T("virus-engine.sec.miui.com，关闭后不再云端查毒"), Config.K_SC_VIRUS, false),
                    new Item(T("XSOF 合作方鉴权"), T("framework.sec.miui.com，可能影响第三方调用"), Config.K_SC_FRAMEWORK, false),
                    new Item(T("月报与配置上报"), T("data.sec.miui.com，含防窥/广告等接口"), Config.K_SC_DATA, false),
                    new Item(T("防窥分类上报"), T("上传非系统应用包名数组；关闭后防窥识别可能变差"), Config.K_SC_APPCLASS, false),
                    new Item(T("志愿者传感器上报"), T("OAID + 经纬度 + 时间；非志愿者无影响"), Config.K_SC_VOLUNTEER, true),
                    new Item(T("地震/报警/EPS 定位"), T("关闭后地震预警、紧急报警定位可能失效"), Config.K_SC_LOCATION, false),
                    new Item(T("ADB 授权验证"), T("srv.sec.miui.com/data/adb；关闭后 ADB 安装授权可能失败"), Config.K_SC_ADB, false),
                    new Item(T("行为库写入拦截"), T("拦截 screenshot / screen_share / voip_record 等 8 张表的写入"), Config.K_SC_DBGUARD, true),
                    new Item(T("设备标识拦截"), T("IMSI / ICCID / 手机号 / IMEI；打开后剩余流量、套餐查询会失效"), Config.K_SC_IDS, false),
                    new Item(T("广告位与推送下发"), T("adv 广告位、网络助手推送、满意度问卷；关闭后这几处不再拉取配置"), Config.K_SC_MISC, true),
                    new Item(T("统计埋点上报"), T("tracking.*.miui.com / sdkconfig.*.xiaomi.com；只影响统计，不影响功能"), Config.K_SC_TRACKING, true),
                    new Item(T("定位（GPS）"), T("只对本应用：位置查询返回空"), Config.K_LOC_SC, false),
                    new Item(T("通讯录 / 通话 / 短信读取"), T("只对本应用：查询返回空；来电识别会受影响"), Config.K_SOCIAL_SC, false),
            }),
            new Group(T("小米统计"), "com.miui.analytics", Config.K_G_ANALYTICS, new Item[]{
                    new Item(T("拦截上报"), T("tracking / om.tracking / sdkconfig"), Config.K_ANALYTICS, true),
                    new Item(T("阻断 DoH 旁路"), T("dns.google 与 8.8.8.8，防解析绕过域名规则"), Config.K_ANALYTICS_DOH, true),
                    new Item(T("全断网（推荐）"), T("该进程所有外网请求一并拦截；云端换域名也不用管"), Config.K_ANALYTICS_ALLNET, true),
            }),
            new Group(T("应用安装器"), "com.miui.packageinstaller", Config.K_G_PKG, new Item[]{
                    new Item(T("AVL 上报"), T("网络层 + 方法层；关闭后安装信息不再上传，病毒扫描结果可能缺失"), Config.K_PKG_AVL, true),
                    new Item(T("安装活跃统计"), T("网络层 + 方法层；关闭后不再上报安装活跃数据，无功能影响"), Config.K_PKG_STAT, true),
                    new Item(T("安装页广告位"), T("网络层 + 方法层；关闭后安装页不再拉取推荐位配置"), Config.K_AD_PKG, true),
                    new Item(T("设备标识（OAID）"), T("MSA SDK 返回空串；只影响广告归因，不影响安装"), Config.K_PKG_OAID, true),
                    new Item(T("云端配置下发"), T("阻断安装器云端参数，含「未查询到备案」提示；关闭后按内置默认值展示"), Config.K_PKG_CLOUD, true),
                    new Item(T("风险校验查询"), T("verify / tz.sec.xiaomi.com；打开后安装风险检测失效，包名与哈希不再外发"), Config.K_PKG_RISK, false),
                    new Item(T("游戏防篡改检查"), T("game/interceptcheck；打开后游戏安装时的完整性校验失效"), Config.K_PKG_GAME, false),
            }),
            new Group(T("应用商店"), "com.xiaomi.market", Config.K_G_AD_MARKET, new Item[]{
                    new Item(T("广告配置下发"), T("sdkconfig.ad.xiaomi.com"), Config.K_AD_MARKET, true),
                    new Item(T("使用统计上报"), T("tracking.miui.com/track；只影响统计，不影响下载与安装"), Config.K_MARKET_TRACK, true),
            }),
            new Group(T("小米服务框架"), "com.xiaomi.xmsf", Config.K_G_AD_XMSF, new Item[]{
                    new Item(T("广告接口"), T("adv.sec.miui.com"), Config.K_AD_XMSF, true),
                    new Item(T("使用统计上报"), T("tracking.miui.com/track；只影响统计，不影响推送与登录"), Config.K_XMSF_TRACK, true),
            }),
            new Group(T("广告组件（MSA）"), "com.miui.systemAdSolution", Config.K_G_MSA, new Item[]{
                    new Item(T("广告使用画像"), T("读取应用今日是否打开、最后使用时间；按少推广告的一侧返回"), Config.K_MSA_USAGE, true),
                    new Item(T("广告埋点上报"), T("OneTrack 的 track / adTrack 方法直接返回空"), Config.K_MSA_TRACK, true),
                    new Item(T("开屏广告"), T("requestSplashScreen 系列返回空 Intent；应用自己的启动页不受影响"), Config.K_MSA_SPLASH, true),
            }),
            new Group(T("网络管理服务"), "com.miui.wmsvc", Config.K_G_WM, new Item[]{
                    new Item(T("诊断统计上报"), T("OneTrack 埋点 + tracking.miui.com；只影响统计，不影响网络设置与信号"), Config.K_WM_TRACK, true),
            }),
            new Group(T("AI 内容识别"), "com.xiaomi.aicr", Config.K_G_AICR, new Item[]{
                    new Item(T("统计埋点"), T("OneTrack 埋点 + tracking.miui.com；不影响本地识别与模型推理"), Config.K_AICR_TRACK, true),
            }),
            new Group(T("搜狗输入法"), "com.sohu.inputmethod.sogou.xiaomi", Config.K_G_IME_SOGOU, new Item[]{
                    new Item(T("统计上报"), T("kpi2 / instantpb 两个埋点域名；只影响统计"), Config.K_SOGOU_PINGBACK, true),
                    new Item(T("剪贴板采集"), T("ClipboardLogHelper 记录剪贴板内容与长度；输入与候选不受影响"), Config.K_SOGOU_CLIPBOARD, true),
            }),
            new Group(T("小米输入法"), "com.xiaomi.type", Config.K_G_IME_MITYPE, new Item[]{
                    new Item(T("统计埋点"), T("OneTrack 埋点 + tracking.miui.com；击键统计没有稳定域名，暂未覆盖"), Config.K_MITYPE_TRACK, true),
            }),
            new Group(T("百度输入法"), "com.baidu.input_mi", Config.K_G_IME_BAIDU, new Item[]{
                    new Item(T("统计上报"), T("com.baidu.stats 记录与上传入口；只影响统计"), Config.K_BAIDU_STATS, true),
                    new Item(T("语音采集上传"), T("com.baidu.speech.utils.analysis；关闭后语音输入仍可用，只是不再上传 ASR 数据"), Config.K_BAIDU_SPEECH, true),
            }),
            new Group(T("讯飞输入法"), "com.iflytek.inputmethod.miui", Config.K_G_IME_IFLYTEK, new Item[]{
                    new Item(T("统计上报"), T("com.iflytek.statssdk 记录与上传入口；只影响统计"), Config.K_IFLYTEK_STATS, true),
            }),
            new Group(T("运营商服务"), "com.xiaomi.registration", Config.K_G_REGISTRATION, new Item[]{
                    new Item(T("设备注册（zzhc）"), T("IMEI/IMSI/ICCID；关闭后 VoLTE / 运营商功能可能异常"), Config.K_REG_ZZHC, false),
                    new Item(T("明文 CTEI 上报"), T("http://typdm.tydevice.com，IMEI / MAC / BSSID 明文 GET"), Config.K_REG_TYPDM, true),
                    new Item(T("定位（GPS）"), T("只对本应用：位置查询返回空"), Config.K_LOC_REG, false),
                    new Item(T("基站信息"), T("只对本应用：基站查询返回空，不影响信号显示"), Config.K_CELL_REG, false),
            }),
            new Group(T("移动 DM（RegService）"), "com.miui.dmregservice", Config.K_G_DMREG, new Item[]{
                    new Item(T("拦截上报"), T("fxltsbl.com；App 清单 / SIM 标识"), Config.K_DMREG, true),
                    new Item(T("基站信息"), T("只对本应用：基站查询返回空，不影响信号显示"), Config.K_CELL_DM, false),
                    new Item(T("Wi-Fi 扫描"), T("只对本应用：扫描列表置空，SSID / BSSID 清空"), Config.K_WIFI_DM, false),
            }),
            new Group(T("系统诊断（MiSight）"), "com.miui.misightservice", Config.K_G_MISIGHT, new Item[]{
                    new Item(T("日志上传"), T("feedback.miui.com/misight/upload；关闭后主动反馈传不了日志包"), Config.K_MISIGHT_UPLOAD, true),
                    new Item(T("售后检测"), T("port.sec.miui.com/mqsas；关闭后售后远程检测拿不到日志"), Config.K_MISIGHT_MQSAS, true),
                    new Item(T("客服诊断"), T("chat.kefu.mi.com/openapi/misight/diagnostic；关闭后在线客服远程诊断不可用"), Config.K_MISIGHT_KEFU, true),
                    new Item(T("事件日志上传"), T("event-log.cnbj1.mi-fds.com；诊断事件不再回传，本地记录不受影响"), Config.K_MISIGHT_EVENTLOG, true),
            }, T("触发条件是用户主动反馈或售后远程诊断；本地记录不等于已经上传")),
            new Group(T("小米可信服务"), "com.xiaomi.trustservice", Config.K_G_TRUST, new Item[]{
                    new Item(T("屏蔽 MRM 认证链"), T("会影响设备证书 / 查找设备"), Config.K_TRUST, false),
                    new Item(T("设备证书/密钥上报"), T("find.api.micloud.xiaomi.net；关闭后查找设备、设备证书可能异常"), Config.K_TRUST_FIND, false),
            }),
            new Group(T("翻译组件"), "com.xiaomi.aiasst.vision", Config.K_G_AIASST, new Item[]{
                    new Item(T("语音识别上传"), T("只断 /speech/ 路径，保留文字翻译"), Config.K_AIASST, false),
            }),
    };

    /** 组 key → 该组所有子项 Switch / 行，用于刷新真实状态（不置灰） */
    private final Map<String, List<Switch>> childSwitches = new LinkedHashMap<>();
    private final Map<String, List<LinearLayout>> childRows = new LinkedHashMap<>();
    /** 组 key → 组开关 */
    private final Map<String, Switch> groupSwitches = new LinkedHashMap<>();
    private final Map<String, TextView> scopeHints = new LinkedHashMap<>();
    private ScrollView scrollRoot;
    private AlertDialog shownDialog;
    private int appliedNight = -1;
    /** DISCLAIMER.txt 的版本号，变了就重新弹一次确认 */
    private static final int DISCLAIMER_VER = 14;
    private int restoreScrollY;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private XposedService xposed;
    private boolean updatingSwitch = false;
    private int bindRetry = 0;
    private boolean autoRequested = false;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        applyPalette(getResources().getConfiguration());
        applyWindowChrome();
        bindServiceHelper();
        restoreScrollY = b == null ? 0 : b.getInt("scrollY", 0);
        buildUi();
        try {
            registerReceiver(themeReceiver,
                    new android.content.IntentFilter(android.content.Intent.ACTION_CONFIGURATION_CHANGED));
        } catch (Throwable t) {
            android.util.Log.w("HyperPGuard", "theme receiver fail", t);
        }
        if (restoreScrollY > 0 && scrollRoot != null) {
            scrollRoot.post(() -> scrollRoot.scrollTo(0, restoreScrollY));
        }
        maybeShowDisclaimer();
    }

    /** 深浅色变化时不重建 Activity，避免对话框被关掉；只重建内容视图 */
    @Override
    public void onConfigurationChanged(android.content.res.Configuration cfg) {
        super.onConfigurationChanged(cfg);
        int y = scrollRoot == null ? 0 : scrollRoot.getScrollY();
        applyPalette(cfg);
        applyWindowChrome();
        buildUi();
        if (y > 0 && scrollRoot != null) {
            scrollRoot.post(() -> scrollRoot.scrollTo(0, y));
        }
        if (shownDialog != null && shownDialog.isShowing()) {
            styleDialog(shownDialog);
        }
    }

    private void applyPalette(android.content.res.Configuration cfg) {
        android.content.res.Configuration c =
                cfg != null ? cfg : getResources().getConfiguration();
        applyPalette((c.uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES);
    }

    /** 以最终判定的深浅色重设色板与窗口背景 */
    private void applyPalette(boolean night) {
        nightMode = night;
        appliedNight = night ? 1 : 0;
        if (nightMode) {
            BG = 0xFF000000;
            CARD = 0xFF1C1C1E;
            TEXT = 0xFFFFFFFF;
            SUB = 0xFF8E8E93;
            LINE = 0xFF2C2C2E;
        } else {
            BG = 0xFFF2F3F5;
            CARD = 0xFFFFFFFF;
            TEXT = 0xFF1A1A1A;
            SUB = 0xFF8A8A8E;
            LINE = 0xFFEFEFF0;
        }
        try {
            getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(
                    nightMode ? 0xFF000000 : BG));
        } catch (Throwable ignored) {
        }

    }

    private void applyWindowChrome() {
        // 状态栏透明，让背景延伸到状态栏下面；图标颜色按深浅模式切换
        try {
            getWindow().addFlags(
                    android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(0);
            getWindow().setNavigationBarColor(0);
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                // 30 起必须关掉 decorFitsSystemWindows 才会真的延伸到状态栏/导航栏下面
                getWindow().setDecorFitsSystemWindows(false);
            } else {
                int flags = android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
                if (!nightMode) flags |= android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                getWindow().getDecorView().setSystemUiVisibility(flags);
            }
        } catch (Throwable ignored) {
        }
    }

    private void buildUi() {
        ScrollView sv = new ScrollView(this);
        scrollRoot = sv;
        sv.setBackgroundColor(BG);
        sv.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(20) + statusBarHeight(), dp(16), dp(24) + navBarHeight());
        sv.addView(root, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = new TextView(this);
        title.setText("HyperPGuard");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 34);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(TEXT);
        title.setPadding(dp(12), 0, 0, dp(10));
        root.addView(title);



        // 全局设置
        root.addView(sectionRow("通用", "tip：开关打开 = 拦截该行为，关闭 = 放行"));
        LinearLayout global = card();
        makeSwitchRow(global, "总开关", "关闭后本模块在所有进程不生效",
                Config.K_MASTER, true, true);
        global.addView(divider());
        makeSwitchRow(global, "路径特征拦截", "路径规则（/avl/upload 等）是否生效",
                Config.K_PATH, true, true);
        global.addView(divider());
        makeSwitchRow(global, "记录未拦截访问", "把没命中规则的域名写进日志，排查漏拦时临时打开",
                Config.K_SNIFF, false, true);
        global.addView(divider());
        addFraudDbRow(global);
        global.addView(divider());
        addCustomRulesRow(global);
        global.addView(divider());
        addCleanLogRow(global);
        global.addView(divider());
        addHideIconRow(global);
        // 需要 root 的功能，提示写在这里
        TextView rootTip = new TextView(this);
        rootTip.setText("「重启」「查看日志」需要 root，首次使用请允许 KernelSU 授权");
        rootTip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        rootTip.setTextColor(SUB);
        rootTip.setPadding(0, dp(8), 0, 0);
        global.addView(rootTip);
        global.addView(buttonRow());
        root.addView(global);

        // 批量操作
        LinearLayout bulk = new LinearLayout(this);
        bulk.setOrientation(LinearLayout.HORIZONTAL);
        bulk.setPadding(0, 0, 0, dp(6));
        bulk.addView(bulkButton("全部启用", this::requestAll));
        bulk.addView(bulkButton("全部停用", this::removeAll));
        bulk.addView(bulkButton("全部重启", this::restartAll));
        root.addView(bulk);



        // 每个应用一张卡片
        root.addView(sectionTitle("组件拦截"));
        for (final Group g : GROUPS) {
            LinearLayout c = card();
            List<Switch> children = new ArrayList<>();
            List<LinearLayout> rows = new ArrayList<>();
            childSwitches.put(g.pkg, children);
            childRows.put(g.pkg, rows);

            c.addView(groupHeader(g, rows));
            if (g.note != null) c.addView(groupNote(g.note));
            TextView scopeHint = new TextView(this);
            scopeHint.setText(T("未在作用域，点此加入"));
            scopeHint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            scopeHint.setTextColor(ACCENT);
            scopeHint.setPadding(dp(4), dp(10), 0, dp(8));
            scopeHint.setVisibility(android.view.View.GONE);
            scopeHint.setOnClickListener(v -> ensureInScope(g));
            c.addView(scopeHint);
            scopeHints.put(g.pkg, scopeHint);
            for (Item it : g.items) {
                if (c.getChildCount() > 0) c.addView(divider());
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setMinimumHeight(dp(56));
        row.setPadding(0, dp(9), 0, dp(9));
                Switch sw = buildSwitchRow(row, it.name, it.desc, it.key, it.def, true, g);
                children.add(sw);
                rows.add(row);
                c.addView(row);
            }
            root.addView(c);
            setGroupUi(g, Config.get(this, g.key, true));
        }

        Button disclaimer = new Button(this);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dlp.topMargin = dp(4);
        disclaimer.setLayoutParams(dlp);
        disclaimer.setText("免责声明 / 使用条款");
        disclaimer.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        disclaimer.setBackground(null);
        disclaimer.setTextColor(SUB);
        disclaimer.setOnClickListener(v -> showDisclaimer());

        // 关于
        LinearLayout about = card();
        TextView aboutHead = new TextView(this);
        aboutHead.setText("关于");
        aboutHead.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        aboutHead.setTextColor(SUB);
        aboutHead.setPadding(0, dp(12), 0, 0);
        about.addView(aboutHead);
        about.addView(linkRow("GitHub 仓库", "vivibibi30/hyperpguard", URL_REPO));
        about.addView(line());
        about.addView(linkRow("LSPosed 模块页", "modules.lsposed.org", URL_MODULE));
        about.addView(line());
        about.addView(linkRow("问题反馈", "Issues", URL_ISSUES));
        about.addView(line());
        about.addView(infoRow("版本", "v" + Config.VERSION));
        Button checkUpdateBtn = new Button(this);
        checkUpdateBtn.setText("检查更新");
        checkUpdateBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        checkUpdateBtn.setBackground(null);
        checkUpdateBtn.setTextColor(ACCENT);
        checkUpdateBtn.setAllCaps(false);
        checkUpdateBtn.setPadding(0, dp(10), 0, dp(10));
        checkUpdateBtn.setOnClickListener(v -> checkUpdate());
        about.addView(checkUpdateBtn);
        root.addView(about);

        root.addView(disclaimer);


        setContentView(sv);
        I18n.apply(sv);
        // 深浅两种模式都要设状态栏图标颜色，否则浅色下会出现白图标压白底
        try {
            android.view.WindowInsetsController c =
                    getWindow().getDecorView().getWindowInsetsController();
            if (c != null) {
                int light = android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS;
                c.setSystemBarsAppearance(nightMode ? 0 : light, light);
            }
        } catch (Throwable ignored) {
        }
        }

    /** 服务未连接时暂存的动作，绑定成功后自动执行一次 */
    private Runnable pendingAction;

    /** 没连上就把动作排队，绑定成功后执行；4 秒还没连上才提示失败 */
    private void runWhenBound(final Runnable action) {
        if (xposed != null) {
            action.run();
            return;
        }
        pendingAction = action;
        bindServiceHelper();
        Toast.makeText(this, "正在连接 LSPosed…", Toast.LENGTH_SHORT).show();
        ui.postDelayed(() -> {
            if (pendingAction == action && xposed == null) {
                pendingAction = null;
                Toast.makeText(MainActivity.this,
                        "LSPosed 服务未连接，请确认已在 LSPosed 中启用本模块后重开本界面",
                        Toast.LENGTH_LONG).show();
            }
        }, 4000);
    }

    /** LSPosed 服务断开后重连：覆盖安装模块时服务端会重启一次 */
    private void bindServiceHelper() {
        try {
            XposedServiceHelper.registerListener(serviceListener);
        } catch (Throwable t) {
            android.util.Log.w("HyperPGuard", "registerListener fail", t);
        }
    }

    private final XposedServiceHelper.OnServiceListener serviceListener =
            new XposedServiceHelper.OnServiceListener() {
                @Override
                public void onServiceBind(XposedService service) {
                    xposed = service;
                    bindRetry = 0;
                    android.util.Log.i("HyperPGuard", "XposedService bound: "
                            + service.getFrameworkName() + " " + service.getFrameworkVersion());
                    ui.post(MainActivity.this::syncFromService);
                    final Runnable pending = pendingAction;
                    pendingAction = null;
                    if (pending != null) ui.post(pending);
                }

                @Override
                public void onServiceDied(XposedService service) {
                    xposed = null;
                    android.util.Log.w("HyperPGuard", "XposedService died");
                    bindRetry++;
                    if (bindRetry < 30) {
                        ui.postDelayed(MainActivity.this::bindServiceHelper, 2000);
                    }
                }
            };

    private final android.content.BroadcastReceiver themeReceiver =
            new android.content.BroadcastReceiver() {
                @Override
                public void onReceive(android.content.Context context, android.content.Intent intent) {
                    syncThemeIfNeeded();
                }
            };

    /** 系统深浅色变了就重建一次；onConfigurationChanged 没回调时靠它兜底 */
    private void syncThemeIfNeeded() {
        android.content.res.Configuration c =
                getApplicationContext().getResources().getConfiguration();
        int want = (c.uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES ? 1 : 0;
        // 部分 ROM 上配置对象会滞后，直接用 UiModeManager 的显式设置兜底
        try {
            android.app.UiModeManager um =
                    (android.app.UiModeManager) getSystemService(UI_MODE_SERVICE);
            if (um != null) {
                int mode = um.getNightMode();
                if (mode == android.app.UiModeManager.MODE_NIGHT_YES) want = 1;
                else if (mode == android.app.UiModeManager.MODE_NIGHT_NO) want = 0;
            }
        } catch (Throwable ignored) {
        }
        android.util.Log.i("HyperPGuard", "theme check want=" + want + " applied=" + appliedNight);
        if (appliedNight == -1 || want == appliedNight) return;
        int y = scrollRoot == null ? 0 : scrollRoot.getScrollY();
        applyPalette(want == 1);
        applyWindowChrome();
        buildUi();
        if (y > 0 && scrollRoot != null) {
            scrollRoot.post(() -> scrollRoot.scrollTo(0, y));
        }
        if (shownDialog != null && shownDialog.isShowing()) styleDialog(shownDialog);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (!hasFocus) return;
        syncThemeIfNeeded();
        // 系统主题广播可能晚于焦点变化，稍后再核一次
        ui.postDelayed(this::syncThemeIfNeeded, 400);
    }

    @Override
    protected void onDestroy() {
        try {
            unregisterReceiver(themeReceiver);
        } catch (Throwable ignored) {
        }
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        if (scrollRoot != null) out.putInt("scrollY", scrollRoot.getScrollY());
    }

    @Override
    protected void onResume() {
        super.onResume();
        syncThemeIfNeeded();
        if (xposed != null) {
            syncFromService();
        } else {
            bindServiceHelper();
        }
    }

    /* ---------------- 软重启 ---------------- */

    private void requestAdd(final String pkg, final Group g, final Switch sw) {
        if (xposed == null) {
            runWhenBound(() -> requestAdd(pkg, g, sw));
            return;
        }
        android.util.Log.i("HyperPGuard", "requestScope(" + pkg + ") sending");
        try {
            xposed.requestScope(Collections.singletonList(pkg),
                    new XposedService.OnScopeEventListener() {
                        @Override
                        public void onScopeRequestApproved(java.util.List<String> approved) {
                            ui.post(() -> commitState(g, sw, true,
                                    "已加入作用域：" + approved));
                        }

                        @Override
                        public void onScopeRequestFailed(String message) {
                            android.util.Log.w("HyperPGuard", "requestScope(" + pkg + ") failed: " + message);
                            ui.post(() -> {
                                showScopeFailure("「" + g.title + "」加入作用域失败", pkg, message);
                                syncFromService();
                            });
                        }
                    });
        } catch (Throwable t) {
            Toast.makeText(this, "请求失败：" + t, Toast.LENGTH_LONG).show();
        }
    }

    private void requestRemove(final String pkg, final Group g, final Switch sw) {
        if (xposed == null) return;
        try {
            xposed.removeScope(Collections.singletonList(pkg));
            commitState(g, sw, false, "已移出作用域：" + pkg);
        } catch (Throwable t) {
            Toast.makeText(this, "移除失败：" + t, Toast.LENGTH_LONG).show();
        }
    }

    /** 一次性请求全部作用域 */
    private void requestAll() {
        if (xposed == null) {
            runWhenBound(this::requestAll);
            return;
        }
        final java.util.List<String> all = new java.util.ArrayList<>();
        for (Group g : GROUPS) {
            if (isInstalled(g.pkg)) all.add(g.pkg);
        }
        if (all.isEmpty()) {
            Toast.makeText(this, "没有可加入的目标应用", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            xposed.requestScope(all, new XposedService.OnScopeEventListener() {
                @Override
                public void onScopeRequestApproved(java.util.List<String> approved) {
                    ui.post(() -> {
                        for (Group g : GROUPS) {
                            if (approved != null && approved.contains(g.pkg)) {
                                Config.uiSet(MainActivity.this, g.key, true);
                            }
                        }
                        Toast.makeText(MainActivity.this,
                                "已加入 " + (approved == null ? 0 : approved.size())
                                        + " 个作用域", Toast.LENGTH_SHORT).show();
                        syncFromService();
                    });
                }

                @Override
                public void onScopeRequestFailed(String message) {
                    ui.post(() -> showScopeFailure("批量加入作用域失败", "（多个应用）", message));
                }
            });
        } catch (Throwable t) {
            Toast.makeText(this, "请求失败：" + t, Toast.LENGTH_LONG).show();
        }
    }

    private void removeAll() {
        if (xposed == null) return;
        final java.util.List<String> all = new java.util.ArrayList<>();
        for (Group g : GROUPS) all.add(g.pkg);
        try {
            xposed.removeScope(all);
            for (Group g : GROUPS) Config.uiSet(this, g.key, false);
            Toast.makeText(this, "已移出全部作用域", Toast.LENGTH_SHORT).show();
            syncFromService();
        } catch (Throwable t) {
            Toast.makeText(this, "移除失败：" + t, Toast.LENGTH_LONG).show();
        }
    }

    /** LSPosed 确认后才更新开关与配置 */
    private void commitState(final Group g, final Switch sw, boolean on, String msg) {
        Config.uiSet(this, g.key, on);
        setGroupUi(g, on, true);
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void execSu(String cmd) {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", cmd});
            p.waitFor();
        } catch (Throwable t) {
            Logs.i("su failed: " + cmd + " -> " + t);
        }
    }

    /* ---------------- 环境检测 ---------------- */

    private boolean isInstalled(String pkg) {
        try {
            getPackageManager().getApplicationInfo(pkg, 0);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /* ---------------- 组状态联动（真实作用域驱动，不置灰） ---------------- */

    /**
     * 把一组的状态刷到界面上。
     * 子项永远可点、永远满透明度；on 只反映这一组真实是否在作用域里。
     */
    /**
     * 作用域请求被拒时给出可操作的说明。
     * 常见于系统 priv-app：LSPosed 的 requestScope 会拒绝，但管理器里手动勾选可以。
     */
    private void showScopeFailure(String title, String pkg, String message) {
        showStyled(new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage("包名：" + pkg
                        + "\n\nLSPosed 返回：" + (message == null || message.isEmpty() ? "（无）" : message)
                        + "\n\n可以在 LSPosed 管理器 → 模块 → HyperPGuard → 作用域 里手动勾选该应用。"
                        + "\n手动勾选后回到本界面，开关会跟着变成开启。")
                .setPositiveButton(T("知道了"), null));
    }

    private void setGroupUi(Group g, boolean on) {
        setGroupUi(g, on, false);
    }

    /** animate=false 用于作用域同步与回滚，避免状态还没确认就先播放动画 */
    private void setGroupUi(Group g, boolean on, boolean animate) {
        TextView scopeHint = scopeHints.get(g.pkg);
        if (scopeHint != null) {
            scopeHint.setVisibility(on ? android.view.View.GONE : android.view.View.VISIBLE);
        }
        updatingSwitch = true;
        Switch gs = groupSwitches.get(g.pkg);
        if (gs instanceof PgSwitch) {
            PgSwitch pg = (PgSwitch) gs;
            if (animate) {
                pg.setChecked(on);
            } else {
                pg.setCheckedImmediate(on);
            }
        } else if (gs != null) {
            gs.setChecked(on);
        }
        updatingSwitch = false;

        List<Switch> children = childSwitches.get(g.pkg);
        if (children != null) {
            for (Switch sw : children) {
                sw.setEnabled(on);
                sw.setAlpha(on ? 1f : 0.4f);
            }
        }
        List<LinearLayout> rows = childRows.get(g.pkg);
        if (rows != null) {
            for (LinearLayout r : rows) r.setAlpha(on ? 1f : 0.4f);
        }
    }

    private java.util.List<String> safeScope() {
        if (xposed == null) return null;
        try {
            java.util.List<String> sc = xposed.getScope();
            android.util.Log.i("HyperPGuard", "getScope -> " + sc);
            return sc;
        } catch (Throwable t) {
            android.util.Log.w("HyperPGuard", "getScope fail", t);
            return null;
        }
    }

    /**
     * 从 LSPosed 读真实作用域，刷新所有组开关与配置。
     * 这是「开关状态跟着框架走」的唯一入口：onServiceBind / onResume 都调它。
     */
    private void syncFromService() {
        java.util.List<String> scope = safeScope();
        for (Group g : GROUPS) {
            boolean on;
            if (scope == null) {
                on = Config.get(this, g.key, true);
            } else {
                on = scope.contains(g.pkg);
                Config.uiSet(this, g.key, on);
            }
            setGroupUi(g, on);
        }
        if (scope != null && scope.isEmpty() && !autoRequested) {
            // 首次安装且框架里还没有任何作用域：只自动请求一次，之后交给用户
            autoRequested = true;
            requestAll();
        }
    }

    /** 子项被打开时，如果该应用还没进作用域，顺手请求加入（不再靠置灰拦人） */
    private void ensureInScope(final Group g) {
        if (xposed == null) {
            runWhenBound(() -> ensureInScope(g));
            return;
        }
        java.util.List<String> cur = safeScope();
        if (cur != null && cur.contains(g.pkg)) return;
        try {
            xposed.requestScope(Collections.singletonList(g.pkg),
                    new XposedService.OnScopeEventListener() {
                        @Override
                        public void onScopeRequestApproved(java.util.List<String> approved) {
                            ui.post(() -> {
                                Config.uiSet(MainActivity.this, g.key, true);
                                setGroupUi(g, true);
                            });
                        }

                        @Override
                        public void onScopeRequestFailed(String message) {
                            ui.post(() -> showScopeFailure("「" + g.title + "」加入作用域失败",
                                    g.pkg, message));
                        }
                    });
        } catch (Throwable t) {
            Toast.makeText(this, "请求失败：" + t, Toast.LENGTH_LONG).show();
        }
    }

    private android.view.View bulkButton(final String label, final Runnable action) {
        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tv.setTextColor(ACCENT);
        tv.setGravity(Gravity.CENTER);
        tv.setBackground(pressBackground());
        tv.setPadding(0, dp(10), 0, dp(10));
        tv.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        tv.setOnClickListener(v -> action.run());
        return tv;
    }

    /* ---------------- 卡片与控件 ---------------- */

    /** 分组小标题：13sp 灰色，放在卡片外 */
    /** 分组行：左侧小标题，右侧可放一句说明（右对齐，右侧留白与左侧一致） */
    private android.view.View sectionRow(String title, String right) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.BOTTOM);
        row.setMinimumHeight(dp(35));
        row.setPadding(dp(12), dp(16), dp(12), dp(8));

        TextView left = new TextView(this);
        left.setText(title);
        left.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        left.setTextColor(SUB);
        row.addView(left, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        if (right != null && !right.isEmpty()) {
            TextView r = new TextView(this);
            r.setText(right);
            r.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            r.setTextColor(SUB);
            r.setMaxLines(1);
            r.setEllipsize(android.text.TextUtils.TruncateAt.END);
            row.addView(r);
        }
        return row;
    }

    private TextView sectionTitle(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tv.setTextColor(SUB);
        tv.setMinHeight(dp(35));
        tv.setGravity(Gravity.BOTTOM);
        tv.setPadding(dp(12), dp(16), 0, dp(8));
        return tv;
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD);
        bg.setCornerRadius(dp(20));
        c.setBackground(bg);
        c.setPadding(dp(16), dp(4), dp(16), dp(4));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(10);
        c.setLayoutParams(lp);
        return c;
    }

    /** 系统状态栏高度，用于沉浸式布局的顶部留白 */
    private int statusBarHeight() {
        try {
            int id = getResources().getIdentifier("status_bar_height", "dimen", "android");
            if (id > 0) return getResources().getDimensionPixelSize(id);
        } catch (Throwable ignored) {
        }
        return dp(24);
    }

    /** 系统导航栏高度，沉浸式布局下补底部留白 */
    private int navBarHeight() {
        try {
            int id = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
            if (id > 0) return getResources().getDimensionPixelSize(id);
        } catch (Throwable ignored) {
        }
        return 0;
    }

    /** 列表分隔线：0.75dp，左侧从内容边缘开始，和 Settings 的 item divider 一致 */
    private android.view.View divider() {
        android.view.View v = new android.view.View(this);
        v.setBackgroundColor(LINE);
        int h = Math.max(1, Math.round(getResources().getDisplayMetrics().density * 0.75f));
        v.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, h));
        return v;
    }

    private ViewGroup line() {
        LinearLayout l = new LinearLayout(this);
        l.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
        l.setBackgroundColor(LINE);
        return l;
    }

    private android.view.View groupNote(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tv.setTextColor(SUB);
        tv.setPadding(0, 0, 0, dp(8));
        return tv;
    }

    private ViewGroup groupHeader(final Group g, List<LinearLayout> rows) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(12), 0, dp(8));

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setPadding(0, 0, dp(12), 0);
        texts.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView t1 = new TextView(this);
        t1.setText(g.title);
        t1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        t1.setTextColor(TEXT);

        TextView t2 = new TextView(this);
        t2.setText(g.pkg);
        t2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        t2.setTextColor(SUB);
        t2.setMaxLines(1);
        t2.setEllipsize(android.text.TextUtils.TruncateAt.END);
        t2.setPadding(0, dp(2), 0, 0);

        texts.addView(t1);
        texts.addView(t2);
        row.addView(texts);

        TextView reboot = new TextView(this);
        reboot.setText("重启");
        reboot.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        reboot.setTextColor(ACCENT);
        reboot.setBackground(pressBackground());
        reboot.setPadding(dp(14), dp(8), dp(14), dp(8));
        reboot.setOnClickListener(v -> restartApp(g.pkg));
        row.addView(reboot);

        final Switch sw = new PgSwitch(this, ACCENT,
                nightMode ? 0xFF3A3A3C : 0xFFE9E9EB);
        sw.setLayoutParams(new LinearLayout.LayoutParams(dp(49), dp(28)));
        sw.setLayoutParams(new LinearLayout.LayoutParams(dp(49), dp(28)));
        groupSwitches.put(g.pkg, sw);
        sw.setChecked(Config.get(this, g.key, true));
        if (sw instanceof PgSwitch) {
            // 点击只上报目标状态，开关本身不动；等作用域真的变了再切
            ((PgSwitch) sw).setOnToggleRequest(target -> {
                if (updatingSwitch) return;
                if (target) {
                    requestAdd(g.pkg, g, sw);
                } else {
                    requestRemove(g.pkg, g, sw);
                }
            });
        }
        sw.setOnCheckedChangeListener(null);
        row.addView(sw);
        return row;
    }

    /** 自建一行并挂到 parent，返回该 Switch */
    private Switch makeSwitchRow(LinearLayout parent, String name, String desc,
                                 final String key, boolean def, boolean enabled) {
        LinearLayout row = new LinearLayout(this);
        parent.addView(row);
        return buildSwitchRow(row, name, desc, key, def, enabled);
    }

    /** 把控件填进已有 row，返回该 Switch */
    private Switch buildSwitchRow(LinearLayout row, String name, String desc,
                                  final String key, boolean def, boolean enabled) {
        return buildSwitchRow(row, name, desc, key, def, enabled, null);
    }

    /** 带所属组的版本：打开子项时顺手保证该应用在作用域内，不再靠置灰拦人 */
    private Switch buildSwitchRow(LinearLayout row, String name, String desc,
                                  final String key, boolean def, boolean enabled,
                                  final Group group) {
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(56));
        row.setPadding(0, dp(9), 0, dp(9));

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setPadding(0, 0, dp(12), 0);
        texts.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView t1 = new TextView(this);
        t1.setText(name);
        t1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        t1.setTextColor(TEXT);

        TextView t2 = new TextView(this);
        t2.setText(desc);
        t2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        t2.setTextColor(SUB);
        t2.setPadding(0, dp(2), 0, 0);

        texts.addView(t1);
        texts.addView(t2);
        row.addView(texts);

        Switch sw = new PgSwitch(this, ACCENT,
                nightMode ? 0xFF3A3A3C : 0xFFE9E9EB);
        sw.setLayoutParams(new LinearLayout.LayoutParams(dp(49), dp(28)));
        sw.setChecked(Config.get(this, key, def));
        sw.setEnabled(enabled);
        sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton v, boolean checked) {
                Config.uiSet(MainActivity.this, key, checked);
                if (checked && group != null) ensureInScope(group);
            }
        });
        row.addView(sw);
        return sw;
    }

    /* ---------------- 清理日志缓冲 ---------------- */

    private void addCleanLogRow(LinearLayout parent) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setMinimumHeight(dp(56));
        row.setPadding(dp(4), dp(9), dp(4), dp(9));
        row.setBackground(pressBackground());
        row.setClickable(true);

        TextView t1 = new TextView(this);
        t1.setText("清理日志缓冲");
        t1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        t1.setTextColor(TEXT);

        TextView t2 = new TextView(this);
        t2.setText("清空 logcat 环形缓冲；其他应用的日志会一起清掉，配置与截图不受影响");
        t2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        t2.setTextColor(SUB);
        t2.setPadding(0, dp(2), 0, 0);

        row.addView(t1);
        row.addView(t2);
        row.setOnClickListener(v -> showStyled(new AlertDialog.Builder(this)
                .setTitle(T("清空日志缓冲？"))
                .setMessage("会清空系统 logcat 的全部缓冲，包括其他应用的日志。"
                        + "模块配置、已保存的截图与行为库都不受影响。")
                .setPositiveButton(T("清空"), (d, w) -> new Thread(() -> {
                    execShell("logcat -b all -c");
                    ui.post(() -> Toast.makeText(this, "已清空", Toast.LENGTH_SHORT).show());
                }, "hyperpguard-clean-log").start())
                .setNegativeButton(T("取消"), null)));
        parent.addView(row);
    }

    /* ---------------- 自定义规则 ---------------- */

    private void addCustomRulesRow(LinearLayout parent) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setMinimumHeight(dp(56));
        row.setPadding(0, dp(9), 0, dp(9));
        row.setClickable(true);
        row.setBackground(pressBackground());
        row.setPadding(dp(4), row.getPaddingTop(), dp(4), row.getPaddingBottom());

        TextView t1 = new TextView(this);
        t1.setText("自定义规则");
        t1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        t1.setTextColor(TEXT);

        TextView t2 = new TextView(this);
        String raw = Config.customRulesRaw(this).trim();
        int count = raw.isEmpty() ? 0 : raw.split(";;").length;
        t2.setText(I18n.text("按域名、域名/路径 或 类名#方法 追加拦截，当前 %d 条", count));
        t2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        t2.setTextColor(SUB);
        t2.setPadding(0, dp(2), 0, 0);

        row.addView(t1);
        row.addView(t2);
        row.setOnClickListener(v -> showCustomRules());
        parent.addView(row);
    }

    private void openCustomRulesDoc() {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(URL_CUSTOM_RULES_DOC));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Throwable t) {
            Toast.makeText(this, "没有可以打开链接的应用", Toast.LENGTH_SHORT).show();
        }
    }

    private void renderRules(final LinearLayout ruleList) {
        ruleList.removeAllViews();
        String raw = Config.customRulesRaw(MainActivity.this).trim();
        if (raw.isEmpty()) {
            TextView empty = new TextView(MainActivity.this);
            empty.setText(T("还没有规则"));
            empty.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            empty.setTextColor(SUB);
            empty.setPadding(0, dp(14), 0, dp(8));
            ruleList.addView(empty);
            return;
        }
        for (String one : raw.split(";;")) {
            final String rule = one.trim();
            if (rule.isEmpty()) continue;
            LinearLayout row = new LinearLayout(MainActivity.this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setMinimumHeight(dp(44));
            TextView name = new TextView(MainActivity.this);
            name.setText(rule);
            name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            name.setTextColor(TEXT);
            name.setLayoutParams(new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            row.addView(name);
            TextView del = new TextView(MainActivity.this);
            del.setText(T("删除"));
            del.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            del.setTextColor(ACCENT);
            del.setTag("accent");
            del.setBackground(pressBackground());
            del.setPadding(dp(14), dp(8), dp(14), dp(8));
            del.setOnClickListener(v -> {
                StringBuilder sb = new StringBuilder();
                for (String x : Config.customRulesRaw(MainActivity.this).trim().split(";;")) {
                    String t = x.trim();
                    if (t.isEmpty() || t.equals(rule)) continue;
                    if (sb.length() > 0) sb.append(";;");
                    sb.append(t);
                }
                Config.setCustomRules(MainActivity.this, sb.toString());
                renderRules(ruleList);
            });
            row.addView(del);
            ruleList.addView(row);
        }
    }

    private void showCustomRules() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(4), dp(20), dp(6));

        final EditText input = new EditText(this);
        input.setHint(T("域名、域名/路径 或 类名#方法"));
        input.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        input.setTextColor(TEXT);
        input.setHintTextColor(SUB);
        input.setSingleLine(true);
        GradientDrawable fieldBg = new GradientDrawable();
        fieldBg.setColor(nightMode ? 0xFF2C2C2E : 0xFFF2F3F5);
        fieldBg.setCornerRadius(dp(12));
        input.setBackground(fieldBg);
        input.setPadding(dp(14), dp(12), dp(14), dp(12));
        box.addView(input);

        final LinearLayout ruleList = new LinearLayout(this);
        ruleList.setOrientation(LinearLayout.VERTICAL);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        actions.setPadding(0, dp(6), 0, dp(2));

        TextView add = new TextView(this);
        add.setText(T("添加"));
        add.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        add.setTextColor(ACCENT);
        add.setTag("accent");
        add.setGravity(Gravity.CENTER);
        add.setBackground(pressBackground());
        add.setPadding(0, dp(10), 0, dp(10));
        add.setOnClickListener(v -> {
            String val = input.getText().toString().trim();
            boolean host = val.matches("[A-Za-z0-9._\\-]+(/[A-Za-z0-9._\\-]*)*");
            boolean method = val.matches("[A-Za-z0-9._$]+#[A-Za-z0-9_$*<>]+");
            if (!host && !method) {
                Toast.makeText(this, "格式：域名、域名/路径 或 类名#方法", Toast.LENGTH_SHORT).show();
                return;
            }
            String raw = Config.customRulesRaw(this).trim();
            for (String one : raw.split(";;")) {
                if (one.trim().equals(val)) {
                    Toast.makeText(this, "已存在", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            Config.setCustomRules(this, raw.isEmpty() ? val : raw + ";;" + val);
            input.setText("");
            renderRules(ruleList);
        });
        actions.addView(add, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView clear = new TextView(this);
        clear.setText(T("清空"));
        clear.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        clear.setTextColor(ACCENT);
        clear.setTag("accent");
        clear.setGravity(Gravity.CENTER);
        clear.setBackground(pressBackground());
        clear.setPadding(0, dp(10), 0, dp(10));
        clear.setOnClickListener(v -> {
            Config.setCustomRules(this, "");
            renderRules(ruleList);
        });
        actions.addView(clear, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView doc = new TextView(this);
        doc.setText(T("使用说明"));
        doc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        doc.setTextColor(ACCENT);
        doc.setTag("accent");
        doc.setBackground(pressBackground());
        doc.setPadding(dp(4), dp(10), dp(4), dp(10));
        doc.setOnClickListener(v -> openCustomRulesDoc());

        TextView note = new TextView(this);
        note.setText(T("添加或删除后需要重启对应应用才生效"));
        note.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        note.setTextColor(SUB);

        box.addView(actions);
        box.addView(ruleList);
        box.addView(doc);
        box.addView(note);

        ScrollView sv = new ScrollView(this);
        sv.addView(box);

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(T("自定义规则"))
                .setView(sv)
                .setNegativeButton(T("关闭"), null)
                .create();
        dialog.show();
        trackDialog(dialog);
        renderRules(ruleList);
    }

    /* ---------------- 行为库状态 ---------------- */

    private static final String FRAUD_DB =
            "/data/misc/user/0/securitycenter/fraud_detection_data.db";
    private static java.util.Map<String, Integer> lastSeq;

    private void addFraudDbRow(LinearLayout parent) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setMinimumHeight(dp(56));
        row.setPadding(0, dp(9), 0, dp(9));
        row.setClickable(true);
        row.setBackground(pressBackground());
        row.setPadding(dp(4), row.getPaddingTop(), dp(4), row.getPaddingBottom());

        TextView t1 = new TextView(this);
        t1.setText("行为库状态");
        t1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        t1.setTextColor(TEXT);

        TextView t2 = new TextView(this);
        t2.setText("读安全中心 fraud_detection_data.db 的写入计数，用来判断拦截是否生效");
        t2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        t2.setTextColor(SUB);
        t2.setPadding(0, dp(2), 0, 0);

        row.addView(t1);
        row.addView(t2);
        row.setOnClickListener(v -> showFraudDb());
        parent.addView(row);
    }

    private void showFraudDb() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(8), dp(16), dp(8));

        TextView tv = new TextView(this);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tv.setTextColor(TEXT);
        tv.setTextIsSelectable(false);
        tv.setLongClickable(false);
        tv.setText("读取中…");
        box.addView(tv);

        TextView refresh = new TextView(this);
        refresh.setText(T("刷新"));
        refresh.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        refresh.setTextColor(ACCENT);
        refresh.setTag("accent");
        refresh.setGravity(Gravity.CENTER);
        refresh.setBackground(pressBackground());
        refresh.setPadding(0, dp(10), 0, dp(10));
        box.addView(refresh, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        ScrollView sv = new ScrollView(this);
        sv.addView(box);

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(T("行为库状态"))
                .setView(sv)
                .setNegativeButton(T("关闭"), null)
                .create();

        final Runnable load = () -> new Thread(() -> {
            final String text = readFraudDb();
            ui.post(() -> tv.setText(text));
        }, "hyperpguard-frauddb").start();

        refresh.setOnClickListener(v -> {
            tv.setText("读取中…");
            load.run();
        });
        dialog.show();
        trackDialog(dialog);
        load.run();
    }

    /** 只读计数：复制主库与 -wal 到私有目录，查 sqlite_sequence，不读任何记录内容 */
    private String readFraudDb() {
        String dir = getFilesDir().getAbsolutePath();
        execShell("cp " + FRAUD_DB + " " + dir + "/fd.db 2>/dev/null; "
                + "cp " + FRAUD_DB + "-wal " + dir + "/fd.db-wal 2>/dev/null; "
                + "chmod 644 " + dir + "/fd.db 2>/dev/null; "
                + "chmod 644 " + dir + "/fd.db-wal 2>/dev/null; echo ok");
        java.io.File f = new java.io.File(dir, "fd.db");
        if (!f.exists() || f.length() == 0) {
            return "读不到行为库。\n\n需要 root 授权，且设备上存在 "
                    + "securitycenter/fraud_detection_data.db。";
        }
        SQLiteDatabase db = null;
        try {
            db = SQLiteDatabase.openDatabase(f.getAbsolutePath(), null,
                    SQLiteDatabase.OPEN_READONLY);
            java.util.TreeMap<String, Integer> now = new java.util.TreeMap<>();
            android.database.Cursor c = db.rawQuery("select name, seq from sqlite_sequence", null);
            while (c.moveToNext()) now.put(c.getString(0), c.getInt(1));
            c.close();
            if (now.isEmpty()) return "库里没有写入记录。";
            StringBuilder sb = new StringBuilder();
            int total = 0;
            for (java.util.Map.Entry<String, Integer> e : now.entrySet()) {
                int v = e.getValue();
                total += v;
                sb.append(e.getKey()).append("  ").append(v);
                Integer old = lastSeq == null ? null : lastSeq.get(e.getKey());
                if (old != null && v != old) sb.append("  (+").append(v - old).append(')');
                sb.append('\n');
            }
            lastSeq = now;
            sb.append("\n共 ").append(now.size()).append(" 张表，累计 ").append(total).append(" 次写入");
            sb.append("\n\n这些数字只增不减。刷新后出现 (+N) 说明期间有写入；");
            sb.append("拦截生效时应当保持不变。");
            return sb.toString();
        } catch (Throwable t) {
            return "读取失败：" + t;
        } finally {
            if (db != null) try { db.close(); } catch (Throwable ignored) { }
            new java.io.File(dir, "fd.db-wal").delete();
            new java.io.File(dir, "fd.db-shm").delete();
            f.delete();
        }
    }

    /* ---------------- 桌面图标 ---------------- */

    private static final String LAUNCHER_ALIAS = "io.github.vivibibi30.hyperpguard.Launcher";

    private boolean launcherIconEnabled() {
        try {
            int st = getPackageManager().getComponentEnabledSetting(
                    new android.content.ComponentName(this, LAUNCHER_ALIAS));
            return st != android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        } catch (Throwable t) {
            return true;
        }
    }

    private void setLauncherIcon(boolean on) {
        try {
            getPackageManager().setComponentEnabledSetting(
                    new android.content.ComponentName(this, LAUNCHER_ALIAS),
                    on ? android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                       : android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    android.content.pm.PackageManager.DONT_KILL_APP);
        } catch (Throwable t) {
            Toast.makeText(this, "切换失败：" + t, Toast.LENGTH_LONG).show();
        }
    }

    /** 隐藏桌面图标，入口保留在 LSPosed 模块页 */
    private void addHideIconRow(LinearLayout parent) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(56));
        row.setPadding(0, dp(9), 0, dp(9));

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setPadding(0, 0, dp(12), 0);
        texts.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView t1 = new TextView(this);
        t1.setText("隐藏桌面图标");
        t1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        t1.setTextColor(TEXT);

        TextView t2 = new TextView(this);
        t2.setText("隐藏后从 LSPosed 模块页进入本界面");
        t2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        t2.setTextColor(SUB);
        t2.setPadding(0, dp(2), 0, 0);

        texts.addView(t1);
        texts.addView(t2);
        row.addView(texts);

        final Switch sw = new PgSwitch(this, ACCENT,
                nightMode ? 0xFF3A3A3C : 0xFFE9E9EB);
        sw.setLayoutParams(new LinearLayout.LayoutParams(dp(49), dp(28)));
        sw.setLayoutParams(new LinearLayout.LayoutParams(dp(49), dp(28)));
        sw.setChecked(!launcherIconEnabled());
        sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton v, boolean checked) {
                setLauncherIcon(!checked);
                boolean now = !launcherIconEnabled();
                if (now != checked) sw.setChecked(now);
                if (checked) {
                    Toast.makeText(MainActivity.this,
                            "图标已隐藏，可从 LSPosed 模块页进入", Toast.LENGTH_LONG).show();
                }
            }
        });
        row.addView(sw);
        parent.addView(row);
    }

    private ViewGroup buttonRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setMinimumHeight(dp(56));
        row.setPadding(0, dp(9), 0, dp(9));

        TextView view = new TextView(this);
        view.setText("查看日志");
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        view.setTextColor(ACCENT);
        view.setGravity(Gravity.CENTER);
        view.setBackground(pressBackground());
        view.setPadding(0, dp(10), 0, dp(10));
        view.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        view.setOnClickListener(v -> loadLog(false));
        row.addView(view);

        TextView share = new TextView(this);
        share.setText("分享日志");
        share.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        share.setTextColor(ACCENT);
        share.setGravity(Gravity.CENTER);
        share.setBackground(pressBackground());
        share.setPadding(0, dp(10), 0, dp(10));
        share.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        share.setOnClickListener(v -> loadLog(true));
        row.addView(share);

        TextView diag = new TextView(this);
        diag.setText("自检");
        diag.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        diag.setTextColor(ACCENT);
        diag.setGravity(Gravity.CENTER);
        diag.setBackground(pressBackground());
        diag.setPadding(0, dp(10), 0, dp(10));
        diag.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        diag.setOnClickListener(v -> showDiagnostics());
        row.addView(diag);

        return row;
    }

    /** 框架连接、真实作用域、开关数量、已注入进程，用来判断「为什么没拦住」 */
    private boolean diagRetried;

    private void showDiagnostics() {
        if (xposed == null && !diagRetried) {
            diagRetried = true;
            bindServiceHelper();
            ui.postDelayed(this::showDiagnostics, 1500);
            return;
        }
        diagRetried = false;
        Toast.makeText(this, "检测中…（需 root）", Toast.LENGTH_SHORT).show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                StringBuilder sb = new StringBuilder();
                sb.append("模块版本：").append(Config.VERSION).append('\n');
                sb.append("LSPosed：").append(xposed == null
                        ? "未连接（重开本界面会自动重连；已注入进程仍以下面的日志为准）"
                        : (xposed.getFrameworkName() + " " + xposed.getFrameworkVersion())).append('\n');

                java.util.List<String> scope = safeScope();
                sb.append("作用域：")
                        .append(scope == null ? "读取失败" : scope.size() + " 个").append("\n\n");

                for (Group g : GROUPS) {
                    boolean in = scope != null && scope.contains(g.pkg);
                    sb.append(in ? "[✓] " : "[ ] ").append(g.pkg);
                    if (!isInstalled(g.pkg)) sb.append("（未安装）");
                    sb.append('\n');
                }

                int local = Config.ui(MainActivity.this).getAll().size();
                int remote = 0;
                try {
                    String raw = android.provider.Settings.Global.getString(
                            getContentResolver(), Config.GLOBAL_KEY);
                    if (raw != null && !raw.isEmpty()) {
                        for (String kv : raw.split("\\|")) if (kv.indexOf('=') > 0) remote++;
                    }
                } catch (Throwable ignored) {
                }
                boolean master = Config.get(MainActivity.this, Config.K_MASTER, true);
                sb.append("\n总开关：").append(master ? "已开启" : "已关闭（所有进程都会跳过安装）");
                sb.append("\n本地开关 ").append(local).append(" 项，系统设置 ").append(remote).append(" 项\n");

                sb.append("\nLSPosed 加载记录：\n");
                String lsp = execShell(
                        "f=$(ls -t /data/adb/lspd/log/modules_*.log 2>/dev/null | head -1); "
                                + "[ -n \"$f\" ] && tail -n 4000 \"$f\""
                                + " | grep -oE '\\([a-z0-9_.]+\\)\\[io\\.github\\.vivibibi30\\.hyperpguard'"
                                + " | sed 's/^(//; s/).*//' | sort -u");
                sb.append(lsp.trim().isEmpty() ? "（日志里没有本模块的加载记录）" : lsp.trim());

                sb.append("\n安装失败记录：\n");
                String fails = execShell(
                        "logcat -d -b all -v time | grep -E 'HyperPGuard(\\( *[0-9]+\\))?: (stage|dispatch|net install|privacy install)' | tail -n 40");
                sb.append(fails.trim().isEmpty() ? "（没有）" : fails.trim());

                sb.append("\n最近 attach 日志：\n");
                String attach = execShell(
                        "logcat -d -b all -v time | grep -E 'HyperPGuard(\\( *[0-9]+\\))?: attach' | tail -n 15");
                sb.append(attach.trim().isEmpty()
                        ? "（logcat 缓冲里没有，长时间运行后会被冲掉）"
                        : attach.trim());

                final String body = sb.toString();
                ui.post(new Runnable() {
                    @Override
                    public void run() {
                        showText("环境自检", body);
                    }
                });
            }
        }).start();
    }

    private String execShell(String cmd) {
        Process p = null;
        try {
            p = Runtime.getRuntime().exec(new String[]{"su", "-c", cmd});
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append('\n');
            br.close();
            p.waitFor();
            return sb.toString();
        } catch (Throwable t) {
            return "执行失败：" + t;
        } finally {
            if (p != null) p.destroy();
        }
    }

    private void restartApp(final String pkg) {
        Toast.makeText(this, "正在重启 " + pkg + "（需 root）", Toast.LENGTH_SHORT).show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                String msg;
                try {
                    Process p = Runtime.getRuntime().exec(new String[]{"su", "-c",
                            "am force-stop " + pkg});
                    int code = p.waitFor();
                    msg = code == 0 ? (pkg + " 已重启") : (pkg + " 重启失败 code=" + code);
                } catch (Throwable t) {
                    msg = "重启失败：" + t;
                }
                final String m = msg;
                ui.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, m, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).start();
    }

    /** 重启作用域内全部应用，让 hook 重新加载 */
    private void restartAll() {
        final List<String> pkgs = new ArrayList<>();
        java.util.List<String> sc = safeScope();
        if (sc != null && !sc.isEmpty()) {
            pkgs.addAll(sc);
        } else {
            for (Group g : GROUPS) pkgs.add(g.pkg);
        }
        if (pkgs.isEmpty()) {
            Toast.makeText(this, "作用域为空", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, "正在重启 " + pkgs.size() + " 个应用（需 root）",
                Toast.LENGTH_SHORT).show();
        final StringBuilder cmd = new StringBuilder();
        for (String p : pkgs) cmd.append("am force-stop ").append(p).append("; ");
        new Thread(new Runnable() {
            @Override
            public void run() {
                String msg;
                try {
                    Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", cmd.toString()});
                    int code = p.waitFor();
                    msg = "已重启 " + pkgs.size() + " 个应用" + (code == 0 ? "" : "（code=" + code + "）");
                } catch (Throwable t) {
                    msg = "重启失败：" + t;
                }
                final String m = msg;
                ui.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, m, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).start();
    }

    /* ---------------- 日志 ---------------- */

    private void loadLog(final boolean share) {
        Toast.makeText(this, "正在读取日志，约需数秒（需 root）", Toast.LENGTH_LONG).show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                final String log = execLogcat();
                ui.post(new Runnable() {
                    @Override
                    public void run() {
                        if (share) doShare(log);
                        else showLog(log);
                    }
                });
            }
        }).start();
    }

    private String execLogcat() {
        String out = execShell(
                "logcat -d -b all -v time | grep -E 'HyperPGuard(\\( *[0-9]+\\))?: |blocked by HyperPGuard' | tail -n 600");
        if (out.trim().isEmpty()) {
            return "没有记录。\n\n"
                    + "这里只列模块自身的运行日志（attach、hook 结果）和被拦截的域名访问；"
                    + "没有发生拦截时就是空的，不代表模块没工作。\n\n"
                    + "可能原因：\n"
                    + "1. root 未授权（su 弹窗没点允许）\n"
                    + "2. 模块刚启用，目标应用还没重启\n"
                    + "3. 对应应用的「启用」开关未打开\n"
                    + "4. 旧日志已被 logcat 环形缓冲覆盖\n\n"
                    + "点「自检」可以看框架连接、作用域和已注入进程。";
        }
        return out;
    }

    private void showLog(final String log) {
        String body = "改动任一开关后，需要点该组右上角「重启」或「全部重启」，"
                + "目标应用重新加载规则才会生效。\n\n" + classify(log);
        showText("模块日志（" + log.split("\n").length + " 行）", body);
    }

    /** 每段最多显示的行数 */
    private static final int LOG_SECTION_LIMIT = 20;

    /** 按未拦截 / 已拦截 / 其他分段，避免混在一起看不出结果 */
    private static String classify(String log) {
        java.util.List<String> blocked = new java.util.ArrayList<>();
        java.util.List<String> passed = new java.util.ArrayList<>();
        java.util.List<String> other = new java.util.ArrayList<>();
        for (String line : log.split("\n")) {
            if (line.trim().isEmpty()) continue;
            if (line.contains("blocked by HyperPGuard")
                    || line.contains(" X(host") || line.contains(" X(strict)")) {
                blocked.add(line);
            } else if (line.contains(": ? ")) {
                passed.add(line);
            } else {
                other.add(line);
            }
        }
        if (blocked.isEmpty() && passed.isEmpty()) return log;
        StringBuilder sb = new StringBuilder();
        sb.append(section("未拦截", passed,
                "未拦截指该域名不在当前规则里，不是拦截失败。\n"));
        sb.append(section("已拦截", blocked, null));
        sb.append(section("模块日志", other, null));
        return sb.toString();
    }

    /** 常见上报域名的用途，用于给「未拦截」行加注 */
    private static final String[][] HOST_HINTS = {
            {"tracking.miui.com", "统计埋点"},
            {"msg.xiaomi.net", "推送接入调度"},
            {"adv.sec.miui.com", "广告位"},
            {"logupdate.avlyun", "病毒引擎日志"},
            {"rcpc-sdk", "腾讯 RCPC SDK"},
            {"net-assistant", "网络助手"},
            {"game.sec.miui.com", "游戏加速"},
            {"pre-api.miui.security", "游戏加速用户信息"},
            {"flash.sec.miui.com", "安全检测接口"},
            {"api.sec.miui.com", "安全中心接口"},
            {"data.sec.miui.com", "月报与配置上报"},
            {"framework.sec.miui.com", "XSOF 鉴权"},
            {"virus-engine", "病毒库云查"},
            {"deviceapi.micloud", "设备证书/查找设备"},
            {"find.api.micloud", "查找设备"},
            {"zzhc.vnet.cn", "运营商注册"},
            {"typdm.tydevice.com", "CTEI 明文上报"},
            {"fxltsbl.com", "移动 DM 上报"},
            {"dm.wo.com.cn", "运营商遥测"},
            {"xiaomimobile.com", "运营商合作服务"},
            {"m.qq.com", "腾讯 SDK"},
    };

    private static String hintFor(String line) {
        int i = line.indexOf(": ? ");
        if (i < 0) return null;
        String rest = line.substring(i + 4).trim();
        int sp = rest.indexOf(' ');
        if (sp < 0) return null;
        String target = rest.substring(sp + 1).trim();
        int slash = target.indexOf('/');
        String host = (slash > 0 ? target.substring(0, slash) : target).toLowerCase();
        for (String[] h : HOST_HINTS) {
            if (host.contains(h[0])) return h[1];
        }
        return null;
    }

    /** 每段只显示最近若干行，附总数与说明 */
    private static String section(String name, java.util.List<String> lines, String note) {
        if (lines.isEmpty()) return "";
        int total = lines.size();
        int from = Math.max(0, total - LOG_SECTION_LIMIT);
        StringBuilder sb = new StringBuilder();
        sb.append(name).append("（共 ").append(total).append(" 条");
        if (total > LOG_SECTION_LIMIT) sb.append("，显示最近 ").append(LOG_SECTION_LIMIT).append(" 条");
        sb.append("）\n");
        if (note != null) sb.append(note);
        for (int i = from; i < total; i++) {
            String line = lines.get(i);
            String hint = note == null ? null : hintFor(line);
            sb.append(hint == null ? line : line + "   ← " + hint).append('\n');
        }
        sb.append('\n');
        return sb.toString();
    }

    private void showText(String title, final String log) {
        TextView tv = new TextView(this);
        tv.setText(log);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tv.setTextIsSelectable(true);
        tv.setPadding(dp(14), dp(8), dp(14), dp(8));

        ScrollView sv = new ScrollView(this);
        sv.addView(tv);

        LinearLayout titleBar = new LinearLayout(this);
        titleBar.setOrientation(LinearLayout.HORIZONTAL);
        titleBar.setGravity(Gravity.CENTER_VERTICAL);
        titleBar.setPadding(dp(24), dp(18), dp(24), dp(6));
        TextView titleText = new TextView(this);
        titleText.setText(title);
        titleText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        titleText.setTypeface(null, Typeface.BOLD);
        titleText.setTextColor(TEXT);
        titleBar.addView(titleText, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        if (title != null && !title.contains("自检")) {
            TextView titleVersion = new TextView(this);
            titleVersion.setText("v" + Config.VERSION);
            titleVersion.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            titleVersion.setTextColor(SUB);
            titleBar.addView(titleVersion);
        }

        showStyled(new AlertDialog.Builder(this)
                .setCustomTitle(titleBar)
                .setView(sv)
                .setPositiveButton(T("复制"), (d, w) -> {
                    ClipboardManager cm =
                            (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    cm.setPrimaryClip(ClipData.newPlainText("pg", log));
                    Toast.makeText(this, "已复制", Toast.LENGTH_SHORT).show();
                })
                .setNeutralButton(T("分享"), (d, w) -> doShare(log))
                .setNegativeButton(T("关闭"), null));
    }

    private void doShare(String log) {
        if (log == null || log.trim().isEmpty()) {
            Toast.makeText(this, "没有可分享的内容", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_SUBJECT, "运行日志");
        i.putExtra(Intent.EXTRA_TEXT, log);
        startActivity(Intent.createChooser(i, "分享运行日志"));
    }

    /* ---------------- 关于 / 更新 ---------------- */

    /** 与 linkRow 同规格的信息行：右侧值用次要色，不可点击 */
    private ViewGroup infoRow(String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(56));
        row.setPadding(0, dp(9), 0, dp(9));
        TextView name = new TextView(this);
        name.setText(label);
        name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        name.setTextColor(TEXT);
        name.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(name);
        TextView val = new TextView(this);
        val.setText(value);
        val.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        val.setTextColor(SUB);
        row.addView(val);
        return row;
    }

    private ViewGroup linkRow(String label, String value, final String url) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(56));
        row.setPadding(0, dp(9), 0, dp(9));
        row.setClickable(true);
        row.setBackground(pressBackground());
        row.setPadding(dp(4), row.getPaddingTop(), dp(4), row.getPaddingBottom());
        TextView name = new TextView(this);
        name.setText(label);
        name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        name.setTextColor(TEXT);
        name.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(name);
        TextView link = new TextView(this);
        link.setText(value);
        link.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        link.setTextColor(ACCENT);
        row.addView(link);
        row.setOnClickListener(v -> openUrl(url));
        return row;
    }

    private void openUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)));
        } catch (Throwable t) {
            Toast.makeText(this, "没有可打开链接的应用", Toast.LENGTH_SHORT).show();
        }
    }

    /** 只在用户点击时请求一次 GitHub API 取最新 tag，不做后台轮询 */
    private void checkUpdate() {
        Toast.makeText(this, "正在检查更新…", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            String tag = null, err = null;
            java.net.HttpURLConnection c = null;
            try {
                c = (java.net.HttpURLConnection) new java.net.URL(API_LATEST).openConnection();
                c.setConnectTimeout(8000);
                c.setReadTimeout(8000);
                c.setRequestProperty("Accept", "application/vnd.github+json");
                c.setRequestProperty("User-Agent", "HyperPGuard/" + Config.VERSION);
                int code = c.getResponseCode();
                if (code == 200) {
                    BufferedReader br = new BufferedReader(
                            new InputStreamReader(c.getInputStream(), "UTF-8"));
                    StringBuilder sb = new StringBuilder();
                    String ln;
                    while ((ln = br.readLine()) != null) sb.append(ln);
                    br.close();
                    tag = new org.json.JSONObject(sb.toString()).optString("tag_name", "");
                } else {
                    err = "HTTP " + code;
                }
            } catch (Throwable t) {
                err = t.getClass().getSimpleName();
            } finally {
                if (c != null) c.disconnect();
            }
            final String fTag = tag, fErr = err;
            ui.post(() -> showUpdateResult(fTag, fErr));
        }, "hyperpguard-check-update").start();
    }

    private void showUpdateResult(String tag, String err) {
        if (err != null) {
            Toast.makeText(this, "检查失败：" + err, Toast.LENGTH_LONG).show();
            return;
        }
        String v = tag == null ? "" : tag.trim().replaceAll("^[vV]", "");
        if (v.length() == 0) {
            Toast.makeText(this, "没拿到版本号，稍后再试", Toast.LENGTH_SHORT).show();
            return;
        }
        if (compareVersion(v, Config.VERSION) > 0) {
            showStyled(new AlertDialog.Builder(this)
                    .setTitle(T("发现新版本 v") + v)
                    .setMessage("当前 v" + Config.VERSION + "，最新 v" + v + "。")
                    .setPositiveButton(T("打开发布页"), (d, w) -> openUrl(URL_RELEASES))
                    .setNegativeButton(T("稍后"), null));
        } else {
            Toast.makeText(this, "已是最新版本（v" + Config.VERSION + "）",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private static int compareVersion(String a, String b) {
        String[] x = a.split("\\.");
        String[] y = b.split("\\.");
        int n = Math.max(x.length, y.length);
        for (int i = 0; i < n; i++) {
            int xi = i < x.length ? versionPart(x[i]) : 0;
            int yi = i < y.length ? versionPart(y[i]) : 0;
            if (xi != yi) return xi > yi ? 1 : -1;
        }
        return 0;
    }

    private static int versionPart(String s) {
        try {
            return Integer.parseInt(s.replaceAll("[^0-9].*$", ""));
        } catch (Throwable t) {
            return 0;
        }
    }

    /* ---------------- 免责声明 ---------------- */

    private String readDisclaimerText() {
        String full;
        try {
            java.io.InputStream is = getAssets().open("DISCLAIMER.txt");
            BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append('\n');
            br.close();
            full = sb.toString();
        } catch (Throwable t) {
            return "免责声明读取失败：" + t;
        }
        if (!I18n.english()) return full;
        // 英文界面只展示文件末尾的 English Summary 段
        int idx = full.indexOf("English Summary");
        if (idx < 0) return full;
        return full.substring(idx + "English Summary".length()).trim();
    }

    /** 首次启动（或免责声明版本变化）时展示摘要并要求确认 */
    private void maybeShowDisclaimer() {
        try {
            final android.content.SharedPreferences sp = Config.ui(this);
            if (sp.getInt("disclaimer_ver", 0) == DISCLAIMER_VER) return;
            String brief = readDisclaimerBrief();
            AlertDialog.Builder b = new AlertDialog.Builder(this)
                    .setTitle(T("免责声明"))
                    .setMessage(brief)
                    .setPositiveButton(T("我已阅读"), (d, w) ->
                            sp.edit().putInt("disclaimer_ver", DISCLAIMER_VER).apply())
                    .setNeutralButton(T("查看完整条款"), (d, w) -> {
                        sp.edit().putInt("disclaimer_ver", DISCLAIMER_VER).apply();
                        showDisclaimer();
                    })
                    .setCancelable(false);
            showStyled(b);
        } catch (Throwable ignored) {
        }
    }

    /** 首启弹窗的摘要：单独一份文件，避免按字符截断句子 */
    private String readDisclaimerBrief() {
        try {
            java.io.InputStream is = getAssets().open("DISCLAIMER_BRIEF.txt");
            BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append('\n');
            br.close();
            String text = sb.toString().trim();
            int idx = text.indexOf("English Summary");
            if (idx < 0) return text;
            // 中文摘要不带英文段，英文界面只取 English Summary 之后
            if (I18n.english()) return text.substring(idx + "English Summary".length()).trim();
            return text.substring(0, idx).trim();
        } catch (Throwable t) {
            return readDisclaimerText();
        }
    }

    private void showDisclaimer() {
        TextView tv = new TextView(this);
        tv.setText(styleDisclaimer(readDisclaimerText()));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tv.setTextIsSelectable(true);
        tv.setLineSpacing(0f, 1.25f);
        tv.setTextColor(TEXT);
        int padH = dp(18);
        int padV = dp(10);
        tv.setPadding(padH, padV, padH, padV);

        ScrollView sv = new ScrollView(this);
        sv.addView(tv);

        showStyled(new AlertDialog.Builder(this)
                .setTitle(T("免责声明"))
                .setView(sv)
                .setPositiveButton(T("我知道了"), null));
    }

    /**
     * 免责声明排版：标题、版本行、章节、列表项分别上样式。
     * 文本已按段一行，列表项用 LeadingMarginSpan，折行后的行也保持缩进。
     */
    private CharSequence styleDisclaimer(String text) {
        android.text.SpannableStringBuilder sb = new android.text.SpannableStringBuilder();
        boolean en = I18n.english();
        boolean first = true;
        for (String raw : text.split("\n", -1)) {
            String line = raw.trim();
            if (line.isEmpty()) {
                if (sb.length() == 0) continue;
                if (sb.length() >= 2 && sb.charAt(sb.length() - 2) == '\n') continue;
                sb.append('\n');
                continue;
            }
            int start = sb.length();
            sb.append(line);
            int end = sb.length();
            sb.append('\n');
            if (first) {
                if (!en) {
                    sb.setSpan(new android.text.style.StyleSpan(Typeface.BOLD), start, end,
                            android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    sb.setSpan(new android.text.style.RelativeSizeSpan(1.15f), start, end,
                            android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                first = false;
            } else if (!en && line.startsWith("版本 ")) {
                sb.setSpan(new android.text.style.RelativeSizeSpan(0.85f), start, end,
                        android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                sb.setSpan(new android.text.style.ForegroundColorSpan(SUB), start, end,
                        android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (!en && line.matches("^[一二三四五六七八九十]+、.*")) {
                sb.setSpan(new android.text.style.StyleSpan(Typeface.BOLD), start, end,
                        android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (line.matches("^\\d+\\)\\s.*") || (en && line.startsWith("- "))) {
                sb.setSpan(new android.text.style.LeadingMarginSpan.Standard(0, dp(18)),
                        start, end + 1, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        return sb;
    }

    /** 按下时的高亮底色：浅色模式比背景深，深色模式比背景浅 */
    private android.graphics.drawable.Drawable pressBackground() {
        int pressed = nightMode ? 0xFF3A3A3C : 0xFFE5E5E5;
        android.graphics.drawable.GradientDrawable on =
                new android.graphics.drawable.GradientDrawable();
        on.setColor(pressed);
        on.setCornerRadius(dp(12));
        android.graphics.drawable.GradientDrawable off =
                new android.graphics.drawable.GradientDrawable();
        off.setColor(0x00000000);
        android.graphics.drawable.StateListDrawable d =
                new android.graphics.drawable.StateListDrawable();
        d.addState(new int[]{android.R.attr.state_pressed}, on);
        d.addState(new int[]{}, off);
        return d;
    }

    /** 对话框跟随深浅色：圆角卡片底、按钮强调色、正文与附注分色 */
    private void styleDialog(AlertDialog d) {
        if (d == null || d.getWindow() == null) return;
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD);
        bg.setCornerRadius(dp(20));
        d.getWindow().setBackgroundDrawable(bg);
        paintDialogText(d.getWindow().getDecorView());
    }

    private void paintDialogText(android.view.View v) {
        if (v == null) return;
        if (v instanceof EditText) {
            EditText et = (EditText) v;
            et.setTextColor(TEXT);
            et.setHintTextColor(SUB);
            GradientDrawable field = new GradientDrawable();
            field.setColor(nightMode ? 0xFF2C2C2E : 0xFFF2F3F5);
            field.setCornerRadius(dp(12));
            et.setBackground(field);
        } else if (v instanceof Button) {
            ((Button) v).setTextColor(ACCENT);
        } else if (v instanceof TextView) {
            TextView tv = (TextView) v;
            if (!"accent".equals(tv.getTag())) {
                float sp = tv.getTextSize() / getResources().getDisplayMetrics().scaledDensity;
                tv.setTextColor(sp <= 12.5f ? SUB : TEXT);
            }
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) paintDialogText(g.getChildAt(i));
        }
    }

    private AlertDialog showStyled(AlertDialog.Builder b) {
        AlertDialog d = b.create();
        d.show();
        trackDialog(d);
        return d;
    }

    /** 记录当前对话框，切深浅色时可以就地重新着色 */
    private void trackDialog(AlertDialog d) {
        shownDialog = d;
        d.setOnDismissListener(x -> {
            if (shownDialog == d) shownDialog = null;
        });
        styleDialog(d);
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }
}
