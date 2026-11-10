package io.github.vivibibi30.hyperpguard;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;

/**
 * 界面英文文案。
 *
 * 只做「整句查表」：中文是默认与唯一来源，查不到就原样返回，
 * 所以漏译只会退回中文，不会出现空字符串。设置页与常用对话框已覆盖，
 * 日志、诊断正文等长文本仍为中文。
 */
final class I18n {

    private static final Map<String, String> EN = new HashMap<>();
    private static Boolean english;

    private I18n() {
    }

    static boolean english() {
        if (english == null) {
            String lang = java.util.Locale.getDefault().getLanguage();
            english = lang != null && lang.startsWith("en");
        }
        return english;
    }

    static String t(String zh) {
        if (zh == null || zh.isEmpty() || !english()) return zh;
        String v = EN.get(zh);
        return v == null ? zh : v;
    }

    /** 带参数的翻译：中文模板与英文模板都用 %d / %s */
    static String text(String zh, Object... args) {
        if (!english()) return String.format(zh, args);
        String en = EN.get(zh);
        return String.format(en == null ? zh : en, args);
    }

    /** 把整棵界面树里的文本按表替换，用于程序化构建的界面 */
    static void apply(View root) {
        if (root == null || !english()) return;
        if (root instanceof TextView) {
            TextView tv = (TextView) root;
            CharSequence cs = tv.getText();
            if (cs != null) {
                String v = EN.get(cs.toString());
                if (v != null) tv.setText(v);
            }
        }
        if (root instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) root;
            for (int i = 0; i < g.getChildCount(); i++) apply(g.getChildAt(i));
        }
    }

    static {
        // 分组
        EN.put("系统安全组件", "System security");
        EN.put("安全中心", "Security Center");
        EN.put("小米统计", "Xiaomi Analytics");
        EN.put("应用安装器", "Package installer");
        EN.put("应用商店", "App store");
        EN.put("小米服务框架", "Mi services");
        EN.put("运营商服务", "Carrier services");
        EN.put("移动 DM（RegService）", "Mobile DM (RegService)");
        EN.put("系统诊断（MiSight）", "Diagnostics (MiSight)");
        EN.put("小米可信服务", "Xiaomi Trust Service");
        EN.put("翻译组件", "Translation");

        // SystemHelper
        EN.put("拦截上报", "Block reporting");
        EN.put("dm.wo.com.cn，设备 / SIM / 基站 / 应用列表",
                "dm.wo.com.cn — device / SIM / cell / installed apps");
        EN.put("整进程断网", "Block all traffic");
        EN.put("默认关：只拦上报域名与方法层。开启后该进程所有外网请求一并拦截，只留本机回环",
                "Off by default: only the report host and method hooks are blocked. When on, all outbound requests from this process are blocked, loopback only");
        EN.put("权限在开机激活时确认；本模块只拦上报，不回收已授予的权限",
                "Permissions are granted during setup; this module only blocks reporting and does not revoke granted permissions");

        // 通用子项
        EN.put("定位（GPS）", "Location (GPS)");
        EN.put("只对本应用：位置查询返回空", "For this app only: location queries return empty");
        EN.put("基站信息", "Cell info");
        EN.put("只对本应用：基站查询返回空，不影响信号显示",
                "For this app only: cell queries return empty; signal display unaffected");
        EN.put("Wi-Fi 扫描", "Wi-Fi scan");
        EN.put("只对本应用：扫描列表置空，SSID / BSSID 清空",
                "For this app only: scan results emptied, SSID / BSSID cleared");
        EN.put("通讯录 / 通话读取", "Contacts / call log");
        EN.put("通讯录 / 通话 / 短信读取", "Contacts / calls / SMS");
        EN.put("只对本应用：查询返回空；来电识别会受影响",
                "For this app only: queries return empty; caller ID may be affected");

        // guardprovider
        EN.put("应用列表云查", "Cloud app-list check");
        EN.put("仅拦 flash.sec.miui.com，不动病毒库",
                "Blocks flash.sec.miui.com only; virus database untouched");
        EN.put("AI 通话预警云检测", "AI call warning check");
        EN.put("反诈配置拉取", "Anti-fraud config fetch");
        EN.put("云查上传（Flamingo）", "Cloud scan upload (Flamingo)");
        EN.put("virus-engine.sec.miui.com；关闭后不再上传应用清单，本地病毒库照常",
                "virus-engine.sec.miui.com; app list is no longer uploaded, local virus database still works");
        EN.put("静默录屏拦截", "Block silent screen recording");
        EN.put("阻止 createVirtualDisplay 整屏录制，不影响正常截屏",
                "Blocks createVirtualDisplay; normal screenshots unaffected");
        EN.put("引擎日志上传", "Engine log upload");
        EN.put("logupdate.avlyun.sec.miui.com；只影响日志回传，不影响本地查毒与病毒库更新",
                "logupdate.avlyun.sec.miui.com; only log upload is affected, scanning and database updates still work");
        EN.put("AI 反诈逻辑在本组件的 AiGuardService 内，用于通话场景；SystemHelper 只做运营商侧上报",
                "AI anti-fraud runs in this component's AiGuardService for calls; SystemHelper only handles carrier-side reporting");

        // securitycenter
        EN.put("XSOF 行为查询拦截", "Block XSOF behaviour queries");
        EN.put("第三方查不到截图 / 通话 / 屏幕共享记录",
                "Third parties can no longer read screenshots, calls or screen-share records");
        EN.put("病毒库云查", "Virus database cloud check");
        EN.put("virus-engine.sec.miui.com，关闭后不再云端查毒",
                "virus-engine.sec.miui.com; cloud scanning stops when on");
        EN.put("XSOF 合作方鉴权", "XSOF partner auth");
        EN.put("framework.sec.miui.com，可能影响第三方调用",
                "framework.sec.miui.com; third-party calls may be affected");
        EN.put("月报与配置上报", "Monthly report and config");
        EN.put("data.sec.miui.com，含防窥/广告等接口",
                "data.sec.miui.com, includes anti-peek and ad endpoints");
        EN.put("防窥分类上报", "Anti-peek category upload");
        EN.put("上传非系统应用包名数组；关闭后防窥识别可能变差",
                "Uploads non-system package names; anti-peek accuracy may drop");
        EN.put("志愿者传感器上报", "Volunteer sensor upload");
        EN.put("OAID + 经纬度 + 时间；非志愿者无影响",
                "OAID + coordinates + time; no effect for non-volunteers");
        EN.put("地震/报警/EPS 定位", "Quake / emergency / EPS location");
        EN.put("关闭后地震预警、紧急报警定位可能失效",
                "Earthquake warning and emergency location may stop working");
        EN.put("ADB 授权验证", "ADB auth check");
        EN.put("srv.sec.miui.com/data/adb；关闭后 ADB 安装授权可能失败",
                "srv.sec.miui.com/data/adb; ADB install auth may fail");
        EN.put("行为库写入拦截", "Block behaviour DB writes");
        EN.put("拦截 screenshot / screen_share / voip_record 等 8 张表的写入",
                "Blocks writes to 8 tables including screenshot, screen_share and voip_record");
        EN.put("设备标识拦截", "Block device identifiers");
        EN.put("IMSI / ICCID / 手机号 / IMEI；打开后剩余流量、套餐查询会失效",
                "IMSI / ICCID / phone number / IMEI; data-usage and plan queries may fail");
        EN.put("广告位与推送下发", "Ad slots and push config");
        EN.put("adv 广告位、网络助手推送、满意度问卷；关闭后这几处不再拉取配置",
                "Ad slots, network assistant push and surveys; no config is fetched when on");

        // analytics
        EN.put("阻断 DoH 旁路", "Block DoH bypass");
        EN.put("dns.google 与 8.8.8.8，防解析绕过域名规则",
                "dns.google and 8.8.8.8, prevents DNS-based rule bypass");

        // packageinstaller
        EN.put("AVL 上报", "AVL upload");
        EN.put("网络层 + 方法层；关闭后安装信息不再上传，病毒扫描结果可能缺失",
                "Network + method layer; install info is no longer uploaded, scan results may be missing");
        EN.put("安装活跃统计", "Install activity stats");
        EN.put("网络层 + 方法层；关闭后不再上报安装活跃数据，无功能影响",
                "Network + method layer; no install activity is reported, no functional impact");
        EN.put("安装页广告位", "Installer ad slots");
        EN.put("网络层 + 方法层；关闭后安装页不再拉取推荐位配置",
                "Network + method layer; the installer no longer fetches recommended-app config");
        EN.put("设备标识（OAID）", "Device ID (OAID)");
        EN.put("MSA SDK 返回空串；只影响广告归因，不影响安装",
                "MSA SDK receives an empty string; ad attribution only, installing still works");
        EN.put("云端配置下发", "Cloud config push");
        EN.put("阻断安装器云端参数，含「未查询到备案」提示；关闭后按内置默认值展示",
                "Blocks installer cloud parameters including the unregistered-app notice; built-in defaults are used instead");
        EN.put("风险校验查询", "Risk verification query");
        EN.put("verify / tz.sec.xiaomi.com；打开后安装风险检测失效，包名与哈希不再外发",
                "verify / tz.sec.xiaomi.com; install risk check is bypassed, package name and hash are no longer sent");
        EN.put("游戏防篡改检查", "Game integrity check");
        EN.put("game/interceptcheck；打开后游戏安装时的完整性校验失效",
                "game/interceptcheck; integrity check on game installation is bypassed");

        // market / xmsf
        EN.put("广告配置下发", "Ad config push");
        EN.put("使用统计上报", "Usage stats upload");
        EN.put("tracking.miui.com/track；只影响统计，不影响下载与安装",
                "tracking.miui.com/track; statistics only, downloads and installs unaffected");
        EN.put("tracking.miui.com/track；只影响统计，不影响推送与登录",
                "tracking.miui.com/track; statistics only, push and sign-in unaffected");
        EN.put("广告接口", "Ad API");

        // registration
        EN.put("设备注册（zzhc）", "Device registration (zzhc)");
        EN.put("IMEI/IMSI/ICCID；关闭后 VoLTE / 运营商功能可能异常",
                "IMEI/IMSI/ICCID; VoLTE and carrier features may break");
        EN.put("明文 CTEI 上报", "Plaintext CTEI upload");
        EN.put("http://typdm.tydevice.com，IMEI / MAC / BSSID 明文 GET",
                "http://typdm.tydevice.com — IMEI / MAC / BSSID sent in a plain GET");

        // misight
        EN.put("日志上传", "Log upload");
        EN.put("feedback.miui.com/misight/upload；关闭后主动反馈传不了日志包",
                "feedback.miui.com/misight/upload; manual feedback can no longer attach logs");
        EN.put("售后检测", "After-sales diagnostics");
        EN.put("port.sec.miui.com/mqsas；关闭后售后远程检测拿不到日志",
                "port.sec.miui.com/mqsas; remote after-sales diagnostics get no logs");
        EN.put("客服诊断", "Support diagnostics");
        EN.put("chat.kefu.mi.com/openapi/misight/diagnostic；关闭后在线客服远程诊断不可用",
                "chat.kefu.mi.com/openapi/misight/diagnostic; online support diagnostics unavailable");
        EN.put("事件日志上传", "Event log upload");
        EN.put("event-log.cnbj1.mi-fds.com；诊断事件不再回传，本地记录不受影响",
                "event-log.cnbj1.mi-fds.com; diagnostic events are no longer sent, local records unaffected");
        EN.put("触发条件是用户主动反馈或售后远程诊断；本地记录不等于已经上传",
                "Triggered only by user-initiated feedback or remote after-sales diagnostics; local records do not mean uploaded");

        // trustservice
        EN.put("屏蔽 MRM 认证链", "Block MRM auth chain");
        EN.put("会影响设备证书 / 查找设备", "Affects device certificate and Find Device");
        EN.put("设备证书/密钥上报", "Device cert / key upload");
        EN.put("find.api.micloud.xiaomi.net；关闭后查找设备、设备证书可能异常",
                "find.api.micloud.xiaomi.net; Find Device and device certificates may fail");

        // aiasst
        EN.put("语音识别上传", "Speech upload");
        EN.put("只断 /speech/ 路径，保留文字翻译",
                "Only the /speech/ path is blocked; text translation still works");

        // 界面骨架
        EN.put("开关打开 = 拦截该行为，关闭 = 放行", "On = block, off = allow");
        EN.put("总开关", "Master switch");
        EN.put("关闭后本模块在所有进程不生效", "When off, the module does nothing in any process");
        EN.put("路径特征拦截", "Path matching");
        EN.put("路径规则（/avl/upload 等）是否生效", "Whether path rules such as /avl/upload apply");
        EN.put("记录未拦截访问", "Log unblocked hosts");
        EN.put("把没命中规则的域名写进日志，排查漏拦时临时打开",
                "Writes hosts that matched no rule to the log; enable temporarily when hunting for missed rules");
        EN.put("行为库状态", "Behaviour DB status");
        EN.put("自定义规则", "Custom rules");
        EN.put("按域名、域名/路径 或 类名#方法 追加拦截，当前 " + "%d" + " 条",
                "Domain, domain/path or class#method rules — %d total");
        EN.put("隐藏桌面图标", "Hide launcher icon");
        EN.put("隐藏后从 LSPosed 模块页进入本界面", "Open this screen from the LSPosed module page after hiding");
        EN.put("查看日志", "View log");
        EN.put("分享日志", "Share log");
        EN.put("自检", "Self-check");
        EN.put("全部启用", "Enable all");
        EN.put("全部停用", "Disable all");
        EN.put("全部重启", "Restart all");
        EN.put("重启", "Restart");
        EN.put("使用说明", "Usage guide");
        EN.put("知道了", "OK");
        EN.put("添加", "Add");
        EN.put("清空", "Clear");
        EN.put("关闭", "Close");
        EN.put("取消", "Cancel");
        EN.put("免责声明", "Disclaimer");
        EN.put("我已阅读", "I have read it");
        EN.put("查看完整条款", "View full terms");
        EN.put("清空日志缓冲？", "Clear the log buffer?");
        EN.put("我知道了", "Got it");
        EN.put("复制", "Copy");
        EN.put("分享", "Share");
        EN.put("稍后", "Later");
        EN.put("打开发布页", "Open releases");
        EN.put("检查更新", "Check for updates");
        EN.put("刷新", "Refresh");
        EN.put("关于", "About");
        EN.put("通用", "General");
        EN.put("组件拦截", "Components");
        EN.put("统计埋点上报", "Usage stats upload");
        EN.put("tracking.*.miui.com / sdkconfig.*.xiaomi.com；只影响统计，不影响功能",
                "tracking.*.miui.com / sdkconfig.*.xiaomi.com; statistics only, no functional impact");
        EN.put("广告组件（MSA）", "Ads (MSA)");
        EN.put("广告使用画像", "Ad usage profile");
        EN.put("读取应用今日是否打开、最后使用时间；按少推广告的一侧返回",
                "Reads whether apps were opened today and their last-used time; answers the fewer-ads side");
        EN.put("广告埋点上报", "Ad event tracking");
        EN.put("OneTrack 的 track / adTrack 方法直接返回空",
                "OneTrack track / adTrack methods return immediately");
        EN.put("开屏广告", "Splash ads");
        EN.put("requestSplashScreen 系列返回空 Intent；应用自己的启动页不受影响",
                "requestSplashScreen returns a null Intent; the app's own launch screen is untouched");
        EN.put("网络管理服务", "Network manager");
        EN.put("诊断统计上报", "Diagnostics upload");
        EN.put("OneTrack 埋点 + tracking.miui.com；只影响统计，不影响网络设置与信号",
                "OneTrack + tracking.miui.com; statistics only, network settings unaffected");
        EN.put("AI 内容识别", "AI content recognition");
        EN.put("统计埋点", "Event tracking");
        EN.put("OneTrack 埋点 + tracking.miui.com；不影响本地识别与模型推理",
                "OneTrack + tracking.miui.com; local recognition and inference stay intact");
        EN.put("搜狗输入法", "Sogou IME");
        EN.put("统计上报", "Usage stats");
        EN.put("kpi2 / instantpb 两个埋点域名；只影响统计",
                "kpi2 / instantpb endpoints; statistics only");
        EN.put("剪贴板采集", "Clipboard collection");
        EN.put("ClipboardLogHelper 记录剪贴板内容与长度；输入与候选不受影响",
                "ClipboardLogHelper records clipboard text and length; typing and candidates unaffected");
        EN.put("小米输入法", "Mi IME");
        EN.put("OneTrack 埋点 + tracking.miui.com；击键统计没有稳定域名，暂未覆盖",
                "OneTrack + tracking.miui.com; keystroke stats have no stable host and are not covered yet");
        EN.put("百度输入法", "Baidu IME");
        EN.put("com.baidu.stats 记录与上传入口；只影响统计",
                "com.baidu.stats record and upload entries; statistics only");
        EN.put("语音采集上传", "Voice data upload");
        EN.put("com.baidu.speech.utils.analysis；关闭后语音输入仍可用，只是不再上传 ASR 数据",
                "com.baidu.speech.utils.analysis; voice input still works, ASR data is no longer uploaded");
        EN.put("讯飞输入法", "iFlytek IME");
        EN.put("com.iflytek.statssdk 记录与上传入口；只影响统计",
                "com.iflytek.statssdk record and upload entries; statistics only");
        EN.put("删除", "Delete");
        EN.put("还没有规则", "No rules yet");
        EN.put("添加或删除后需要重启对应应用才生效", "Restart the target app after adding or removing a rule");
        EN.put("域名、域名/路径 或 类名#方法", "domain, domain/path or class#method");
        EN.put("环境自检", "Self-check");
        EN.put("全断网（推荐）", "Block all traffic (recommended)");
        EN.put("该进程所有外网请求一并拦截；云端换域名也不用管",
                "Every outbound request in this process is blocked; host changes do not matter");
        EN.put("发现新版本 v", "New version v");
        EN.put("fxltsbl.com；App 清单 / SIM 标识", "fxltsbl.com — installed apps / SIM identifiers");
    }
}
