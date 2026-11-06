package io.github.vivibibi30.pguard;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

    private static final int BG = 0xFFF2F3F5;
    private static final int CARD = 0xFFFFFFFF;
    private static final int TEXT = 0xFF1A1A1A;
    private static final int SUB = 0xFF8A8A8E;
    private static final int LINE = 0xFFEFEFF0;
    private static final int ACCENT = 0xFF3478F6;

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
        final String title, pkg, key;
        final Item[] items;

        Group(String title, String pkg, String key, Item[] items) {
            this.title = title;
            this.pkg = pkg;
            this.key = key;
            this.items = items;
        }
    }

    /** 组总开关默认开启：状态由 LSPosed 真实作用域回填，不看本地默认值 */
    private static final Group[] GROUPS = {
            new Group("SystemHelper", "com.mobiletools.systemhelper", Config.K_G_SYSHELPER, new Item[]{
                    new Item("拦截上报", "dm.wo.com.cn，设备 / SIM / 基站 / 应用列表", Config.K_SYSHELPER, true),
                    new Item("全断网（推荐）", "该进程所有外网请求一并拦截，只留本机回环；它没有用户功能", Config.K_SYSHELPER_ALLNET, true),
            }),
            new Group("系统安全组件", "com.miui.guardprovider", Config.K_G_GUARD, new Item[]{
                    new Item("应用列表云查", "仅拦 flash.sec.miui.com，不动病毒库", Config.K_GUARD_APPLIST, true),
                    new Item("AI 通话预警云检测", "detection-aigc.engine.miui.com", Config.K_GUARD_AIGC, true),
                    new Item("反诈配置拉取", "fraud-detection.engine.miui.com", Config.K_GUARD_FRAUD, true),
                    new Item("云查上传（Flamingo）", "virus-engine.sec.miui.com；关闭后不再上传应用清单，本地病毒库照常", Config.K_GP_CLOUDSCAN, false),
            }),
            new Group("安全中心", "com.miui.securitycenter", Config.K_G_SECCENTER, new Item[]{
                    new Item("XSOF 行为查询拦截", "第三方查不到截图 / 通话 / 屏幕共享记录", Config.K_XSOF, true),
                    new Item("反诈配置拉取", "fraud-detection.engine.miui.com", Config.K_SECCENTER, true),
                    new Item("病毒库云查", "virus-engine.sec.miui.com，关闭后不再云端查毒", Config.K_SC_VIRUS, false),
                    new Item("XSOF 合作方鉴权", "framework.sec.miui.com，可能影响第三方调用", Config.K_SC_FRAMEWORK, false),
                    new Item("月报与配置上报", "data.sec.miui.com，含防窥/广告等接口", Config.K_SC_DATA, false),
                    new Item("防窥分类上报", "上传非系统应用包名数组；关闭后防窥识别可能变差", Config.K_SC_APPCLASS, false),
                    new Item("志愿者传感器上报", "OAID + 经纬度 + 时间；非志愿者无影响", Config.K_SC_VOLUNTEER, true),
                    new Item("地震/报警/EPS 定位", "关闭后地震预警、紧急报警定位可能失效", Config.K_SC_LOCATION, false),
                    new Item("ADB 授权验证", "srv.sec.miui.com/data/adb；关闭后 ADB 安装授权可能失败", Config.K_SC_ADB, false),
            }),
            new Group("小米统计", "com.miui.analytics", Config.K_G_ANALYTICS, new Item[]{
                    new Item("拦截上报", "tracking / om.tracking / sdkconfig", Config.K_ANALYTICS, true),
                    new Item("阻断 DoH 旁路", "dns.google 与 8.8.8.8，防解析绕过域名规则", Config.K_ANALYTICS_DOH, true),
                    new Item("全断网（推荐）", "该进程所有外网请求一并拦截；云端换域名也不用管", Config.K_ANALYTICS_ALLNET, true),
            }),
            new Group("应用安装器", "com.miui.packageinstaller", Config.K_G_PKG, new Item[]{
                    new Item("AVL 上报", "…/avl/upload", Config.K_PKG_AVL, true),
                    new Item("安装活跃统计", "…/statistics/appactive", Config.K_PKG_STAT, true),
                    new Item("安装页广告位", "…/info/layout（AdModel）", Config.K_AD_PKG, true),
            }),
            new Group("应用商店", "com.xiaomi.market", Config.K_G_AD_MARKET, new Item[]{
                    new Item("广告配置下发", "sdkconfig.ad.xiaomi.com", Config.K_AD_MARKET, true),
            }),
            new Group("小米服务框架", "com.xiaomi.xmsf", Config.K_G_AD_XMSF, new Item[]{
                    new Item("广告接口", "adv.sec.miui.com", Config.K_AD_XMSF, true),
            }),
            new Group("运营商服务", "com.xiaomi.registration", Config.K_G_REGISTRATION, new Item[]{
                    new Item("设备注册（zzhc）", "IMEI/IMSI/ICCID；关闭后 VoLTE / 运营商功能可能异常", Config.K_REG_ZZHC, false),
                    new Item("明文 CTEI 上报", "http://typdm.tydevice.com，IMEI / MAC / BSSID 明文 GET", Config.K_REG_TYPDM, true),
            }),
            new Group("小米可信服务", "com.xiaomi.trustservice", Config.K_G_TRUST, new Item[]{
                    new Item("屏蔽 MRM 认证链", "会影响设备证书 / 查找设备", Config.K_TRUST, false),
                    new Item("设备证书/密钥上报", "find.api.micloud.xiaomi.net；关闭后查找设备、设备证书可能异常", Config.K_TRUST_FIND, false),
            }),
            new Group("翻译组件", "com.xiaomi.aiasst.vision", Config.K_G_AIASST, new Item[]{
                    new Item("语音识别上传", "只断 /speech/ 路径，保留文字翻译", Config.K_AIASST, false),
            }),
    };

    /** 组 key → 该组所有子项 Switch / 行，用于刷新真实状态（不置灰） */
    private final Map<String, List<Switch>> childSwitches = new LinkedHashMap<>();
    private final Map<String, List<LinearLayout>> childRows = new LinkedHashMap<>();
    /** 组 key → 组开关 */
    private final Map<String, Switch> groupSwitches = new LinkedHashMap<>();
    private final Handler ui = new Handler(Looper.getMainLooper());
    private XposedService xposed;
    private boolean updatingSwitch = false;
    private boolean autoRequested = false;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(BG);
        sv.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14), dp(18), dp(14), dp(24));
        sv.addView(root, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = new TextView(this);
        title.setText("隐私上报拦截");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(TEXT);
        root.addView(title);

        TextView sub = new TextView(this);
        sub.setText("v" + Config.VERSION + " · LSPosed 模块");
        sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        sub.setTextColor(SUB);
        sub.setPadding(0, dp(4), 0, dp(10));
        root.addView(sub);

        // 全局设置
        LinearLayout global = card();
        makeSwitchRow(global, "总开关", "关闭后本模块在所有进程不生效",
                Config.K_MASTER, true, true);
        global.addView(line());
        makeSwitchRow(global, "路径特征拦截", "路径规则（/avl/upload 等）是否生效",
                Config.K_PATH, true, true);
        global.addView(line());
        makeSwitchRow(global, "抓取模式", "记录「没被拦截」的访问，平时关掉",
                Config.K_SNIFF, false, true);
        global.addView(line());
        // 需要 root 的功能，提示写在这里
        TextView rootTip = new TextView(this);
        rootTip.setText("「重启」「查看记录」需要 root，首次使用请允许 KernelSU 授权");
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
        bulk.addView(bulkButton("全部启用", true));
        bulk.addView(bulkButton("全部停用", false));
        root.addView(bulk);



        // 每个应用一张卡片
        for (final Group g : GROUPS) {
            LinearLayout c = card();
            List<Switch> children = new ArrayList<>();
            List<LinearLayout> rows = new ArrayList<>();
            childSwitches.put(g.pkg, children);
            childRows.put(g.pkg, rows);

            c.addView(groupHeader(g, rows));
            for (Item it : g.items) {
                c.addView(line());
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(0, dp(10), 0, dp(10));
                Switch sw = buildSwitchRow(row, it.name, it.desc, it.key, it.def, true, g);
                children.add(sw);
                rows.add(row);
                c.addView(row);
            }
            root.addView(c);
            setGroupUi(g, prefs().getBoolean(g.key, true));
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
        root.addView(disclaimer);

        XposedServiceHelper.registerListener(new XposedServiceHelper.OnServiceListener() {
            @Override
            public void onServiceBind(XposedService service) {
                xposed = service;
                android.util.Log.i("PrivacyGuard", "XposedService bound: "
                        + service.getFrameworkName() + " " + service.getFrameworkVersion());
                ui.post(() -> {
                    migrateLocalPrefs();
                    syncFromService();
                });
            }

            @Override
            public void onServiceDied(XposedService service) {
                xposed = null;
            }
        });

        setContentView(sv);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (xposed != null) syncFromService();
    }

    /* ---------------- 软重启 ---------------- */

    private void requestAdd(final String pkg, final Group g, final Switch sw) {
        if (xposed == null) {
            Toast.makeText(this, "LSPosed 服务未连接，请确认已在 LSPosed 中启用本模块",
                    Toast.LENGTH_LONG).show();
            return;
        }
        android.util.Log.i("PrivacyGuard", "requestScope(" + pkg + ") sending");
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
                            android.util.Log.w("PrivacyGuard", "requestScope(" + pkg + ") failed: " + message);
                            ui.post(() -> Toast.makeText(MainActivity.this,
                                    "未加入作用域：" + message, Toast.LENGTH_LONG).show());
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
            Toast.makeText(this, "LSPosed 服务未连接", Toast.LENGTH_LONG).show();
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
                                setPref(g.key, true);
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
                    ui.post(() -> Toast.makeText(MainActivity.this,
                            "批量请求失败：" + message, Toast.LENGTH_LONG).show());
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
            for (Group g : GROUPS) setPref(g.key, false);
            Toast.makeText(this, "已移出全部作用域", Toast.LENGTH_SHORT).show();
            syncFromService();
        } catch (Throwable t) {
            Toast.makeText(this, "移除失败：" + t, Toast.LENGTH_LONG).show();
        }
    }

    /** LSPosed 确认后才更新开关与配置 */
    private void commitState(final Group g, final Switch sw, boolean on, String msg) {
        setPref(g.key, on);
        if (sw != null) {
            updatingSwitch = true;
            sw.setChecked(on);
            updatingSwitch = false;
        }
        setGroupUi(g, on);
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void execSu(String cmd) {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", cmd});
            p.waitFor();
        } catch (Throwable ignored) {
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
    private void setGroupUi(Group g, boolean on) {
        updatingSwitch = true;
        Switch gs = groupSwitches.get(g.pkg);
        if (gs != null) gs.setChecked(on);
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
            android.util.Log.i("PrivacyGuard", "getScope -> " + sc);
            return sc;
        } catch (Throwable t) {
            android.util.Log.w("PrivacyGuard", "getScope fail", t);
            return null;
        }
    }

    /**
     * UI 侧优先读写 LSPosed 远程偏好：hook 侧通过
     * XposedInterface#getRemotePreferences(Config.PREFS) 读同一份配置。
     * 服务未连接时退回本地 SharedPreferences（首次启动/框架未启用）。
     */
    private SharedPreferences prefs() {
        if (xposed != null) {
            try {
                SharedPreferences remote = xposed.getRemotePreferences(Config.PREFS);
                if (remote != null) return remote;
            } catch (Throwable ignored) {
            }
        }
        return Config.ui(this);
    }

    private void setPref(String key, boolean value) {
        try {
            prefs().edit().putBoolean(key, value).apply();
        } catch (Throwable t) {
            Config.uiSet(this, key, value);
        }
    }

    /**
     * 首次绑定服务时，把旧版本写在本地 SharedPreferences 的开关迁移到远程偏好，
     * 避免升级后 hook 侧读不到用户已有配置而退回默认值。
     */
    private void migrateLocalPrefs() {
        if (xposed == null) return;
        try {
            SharedPreferences remote = xposed.getRemotePreferences(Config.PREFS);
            if (remote == null || !remote.getAll().isEmpty()) return;
            SharedPreferences.Editor editor = remote.edit();
            for (Map.Entry<String, ?> entry : Config.ui(this).getAll().entrySet()) {
                Object value = entry.getValue();
                if (value instanceof Boolean) {
                    editor.putBoolean(entry.getKey(), (Boolean) value);
                }
            }
            editor.apply();
        } catch (Throwable t) {
            android.util.Log.w("PrivacyGuard", "migrate prefs fail", t);
        }
    }

    /** 服务绑定/回到前台时，把子开关也刷新为远程偏好的真实值。 */
    private void refreshChildSwitches() {
        for (Group g : GROUPS) {
            List<Switch> list = childSwitches.get(g.pkg);
            if (list == null) continue;
            for (int i = 0; i < list.size() && i < g.items.length; i++) {
                list.get(i).setChecked(prefs().getBoolean(g.items[i].key, g.items[i].def));
            }
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
                on = prefs().getBoolean(g.key, true);
            } else {
                on = scope.contains(g.pkg);
                setPref(g.key, on);
            }
            setGroupUi(g, on);
        }
        refreshChildSwitches();
        if (scope != null && scope.isEmpty() && !autoRequested) {
            // 首次安装且框架里还没有任何作用域：只自动请求一次，之后交给用户
            autoRequested = true;
            requestAll();
        }
    }

    /** 子项被打开时，如果该应用还没进作用域，顺手请求加入（不再靠置灰拦人） */
    private void ensureInScope(final Group g) {
        if (xposed == null) {
            Toast.makeText(this, "LSPosed 服务未连接，请确认已在 LSPosed 中启用本模块",
                    Toast.LENGTH_LONG).show();
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
                                setPref(g.key, true);
                                setGroupUi(g, true);
                            });
                        }

                        @Override
                        public void onScopeRequestFailed(String message) {
                            ui.post(() -> Toast.makeText(MainActivity.this,
                                    "「" + g.title + "」未加入作用域：" + message,
                                    Toast.LENGTH_LONG).show());
                        }
                    });
        } catch (Throwable t) {
            Toast.makeText(this, "请求失败：" + t, Toast.LENGTH_LONG).show();
        }
    }

    private android.view.View bulkButton(final String label, final boolean enable) {
        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tv.setTextColor(ACCENT);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(0, dp(8), 0, dp(8));
        tv.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        tv.setOnClickListener(v -> {
            if (enable) requestAll();
            else removeAll();
        });
        return tv;
    }

    /* ---------------- 卡片与控件 ---------------- */

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD);
        bg.setCornerRadius(dp(16));
        c.setBackground(bg);
        c.setPadding(dp(16), dp(4), dp(16), dp(6));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(12);
        c.setLayoutParams(lp);
        return c;
    }

    private ViewGroup line() {
        LinearLayout l = new LinearLayout(this);
        l.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
        l.setBackgroundColor(LINE);
        return l;
    }

    private ViewGroup groupHeader(final Group g, List<LinearLayout> rows) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(12), 0, dp(10));

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView t1 = new TextView(this);
        t1.setText(g.title);
        t1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        t1.setTypeface(null, Typeface.BOLD);
        t1.setTextColor(TEXT);

        TextView t2 = new TextView(this);
        t2.setText(g.pkg);
        t2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        t2.setTextColor(SUB);
        t2.setPadding(0, dp(2), 0, 0);

        texts.addView(t1);
        texts.addView(t2);
        row.addView(texts);

        TextView reboot = new TextView(this);
        reboot.setText("重启");
        reboot.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        reboot.setTextColor(ACCENT);
        reboot.setPadding(dp(12), dp(6), dp(8), dp(6));
        reboot.setOnClickListener(v -> restartApp(g.pkg));
        row.addView(reboot);

        final Switch sw = new Switch(this);
        groupSwitches.put(g.pkg, sw);
        sw.setChecked(prefs().getBoolean(g.key, true));
        sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton v, boolean checked) {
                if (updatingSwitch) return;
                boolean current = prefs().getBoolean(g.key, true);
                if (checked == current) return;
                // 先回滚，等 LSPosed 确认后再改
                updatingSwitch = true;
                sw.setChecked(!checked);
                updatingSwitch = false;
                if (checked) {
                    requestAdd(g.pkg, g, sw);
                } else {
                    requestRemove(g.pkg, g, sw);
                }
            }
        });
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
        row.setPadding(0, dp(10), 0, dp(10));

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView t1 = new TextView(this);
        t1.setText(name);
        t1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        t1.setTextColor(TEXT);

        TextView t2 = new TextView(this);
        t2.setText(desc);
        t2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        t2.setTextColor(SUB);
        t2.setPadding(0, dp(2), 0, 0);

        texts.addView(t1);
        texts.addView(t2);
        row.addView(texts);

        Switch sw = new Switch(this);
        sw.setChecked(prefs().getBoolean(key, def));
        sw.setEnabled(enabled);
        sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton v, boolean checked) {
                setPref(key, checked);
                if (checked && group != null) ensureInScope(group);
            }
        });
        row.addView(sw);
        return sw;
    }

    private ViewGroup buttonRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(10), 0, dp(10));

        TextView view = new TextView(this);
        view.setText("查看记录");
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        view.setTextColor(ACCENT);
        view.setGravity(Gravity.CENTER);
        view.setPadding(0, dp(8), 0, dp(8));
        view.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        view.setOnClickListener(v -> loadLog(false));
        row.addView(view);

        TextView share = new TextView(this);
        share.setText("分享日志");
        share.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        share.setTextColor(ACCENT);
        share.setGravity(Gravity.CENTER);
        share.setPadding(0, dp(8), 0, dp(8));
        share.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        share.setOnClickListener(v -> loadLog(true));
        row.addView(share);

        return row;
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

    /* ---------------- 日志 ---------------- */

    private void loadLog(final boolean share) {
        Toast.makeText(this, "读取中…（需 root）", Toast.LENGTH_SHORT).show();
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
        Process p = null;
        try {
            p = Runtime.getRuntime().exec(new String[]{"su", "-c",
                    "logcat -d -v time -t 30000 | grep -F PrivacyGuard | tail -n 600"});
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append('\n');
            br.close();
            p.waitFor();
            if (sb.length() == 0) {
                return "没有记录。可能原因：\n"
                        + "1. root 未授权（su 弹窗没点允许）\n"
                        + "2. 模块刚启用，目标应用还没重启\n"
                        + "3. 对应应用的「启用」开关未打开\n"
                        + "4. 旧日志已被 logcat 环形缓冲覆盖";
            }
            return sb.toString();
        } catch (Throwable t) {
            return "读取失败：" + t;
        } finally {
            if (p != null) p.destroy();
        }
    }

    private void showLog(final String log) {
        TextView tv = new TextView(this);
        tv.setText(log);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tv.setTextIsSelectable(true);
        tv.setPadding(dp(14), dp(8), dp(14), dp(8));

        ScrollView sv = new ScrollView(this);
        sv.addView(tv);

        new AlertDialog.Builder(this)
                .setTitle("拦截记录（" + log.split("\n").length + " 行）")
                .setView(sv)
                .setPositiveButton("复制", (d, w) -> {
                    ClipboardManager cm =
                            (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    cm.setPrimaryClip(ClipData.newPlainText("pg", log));
                    Toast.makeText(this, "已复制", Toast.LENGTH_SHORT).show();
                })
                .setNeutralButton("分享", (d, w) -> doShare(log))
                .setNegativeButton("关闭", null)
                .show();
    }

    private void doShare(String log) {
        if (log == null || log.trim().isEmpty()) {
            Toast.makeText(this, "没有可分享的内容", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_SUBJECT, "PrivacyGuard 拦截日志");
        i.putExtra(Intent.EXTRA_TEXT, log);
        startActivity(Intent.createChooser(i, "分享日志"));
    }

    /* ---------------- 免责声明 ---------------- */

    private void showDisclaimer() {
        String text;
        try {
            java.io.InputStream is = getAssets().open("DISCLAIMER.txt");
            BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append('\n');
            br.close();
            text = sb.toString();
        } catch (Throwable t) {
            text = "免责声明读取失败：" + t;
        }

        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tv.setTextIsSelectable(true);
        tv.setPadding(dp(14), dp(8), dp(14), dp(8));

        ScrollView sv = new ScrollView(this);
        sv.addView(tv);

        new AlertDialog.Builder(this)
                .setTitle("免责声明")
                .setView(sv)
                .setPositiveButton("我知道了", null)
                .show();
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }
}
