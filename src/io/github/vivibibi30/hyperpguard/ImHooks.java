package io.github.vivibibi30.hyperpguard;

import java.lang.reflect.Modifier;

import io.github.libxposed.api.XposedModule;

/**
 * 预装输入法的行为统计与剪贴板采集。
 *
 * 四个输入法各用一套自己的统计 SDK，但结构类似：都是「记录 + 攒批 + 定时上传」，
 * 记录入口全是 void 方法。把它们整体返回 null，只影响统计，不影响输入、候选与词库。
 *
 * - 搜狗输入法小米版：com.sohu.inputmethod.clipboard.ClipboardLogHelper 记录剪贴板
 *   内容与长度，配合 kpi2 / instantpb 两个上报域名使用；统计 SDK 类名混淆，走域名层。
 * - 百度输入法小米版：com.baidu.stats 统计 SDK，另有一套语音识别 AnalysisUpload 上传
 *   ASR 音频与词条。
 * - 讯飞输入法小米版：com.iflytek.statssdk，含 onEvent / onBinLog / 日志上传。
 * - 小米输入法：内嵌 OneTrack，另有自研击键统计（duration_s / key_cnt / action_list），
 *   后者类名混淆且无稳定域名，本模块只拦 OneTrack。
 */
final class ImHooks {

    private ImHooks() {
    }

    /** 搜狗：剪贴板记录类里的 static void 方法。类名是保留下来的语义名，不是混淆名。 */
    static int sogouClipboard(XposedModule m, ClassLoader cl) {
        java.util.List<String> cands = Hooks.classNames(
                cl, s -> s.equals("com.sohu.inputmethod.clipboard.ClipboardLogHelper"), 4);
        int n = Hooks.hookBySignature(m, cands, cl,
                method -> method.getReturnType() == void.class
                        && Modifier.isStatic(method.getModifiers()),
                chain -> null);
        Logs.i("sogou clipboard hooked=" + n);
        return n;
    }

    /** 百度：统计 SDK 的 void 方法（记录入口与上传都在其中）。 */
    static int baiduStats(XposedModule m, ClassLoader cl) {
        return voidPrefix(m, cl, "com.baidu.stats.");
    }

    /** 百度：语音识别的 ASR 采集与上传。 */
    static int baiduSpeech(XposedModule m, ClassLoader cl) {
        return voidPrefix(m, cl, "com.baidu.speech.utils.analysis.");
    }

    /** 讯飞：统计 SDK 的 void 方法。 */
    static int iflytekStats(XposedModule m, ClassLoader cl) {
        return voidPrefix(m, cl, "com.iflytek.statssdk.");
    }

    /**
     * 同一包前缀下所有类的 void 方法。
     *
     * 前缀必须是纯埋点包：这里的粒度是「整个 SDK 停写」，不能把界面或输入逻辑包进来。
     */
    private static int voidPrefix(XposedModule m, ClassLoader cl, String prefix) {
        java.util.List<String> cands = Hooks.classNames(cl, s -> s.startsWith(prefix), 400);
        int n = Hooks.hookBySignature(m, cands, cl,
                method -> method.getReturnType() == void.class
                        && !Modifier.isNative(method.getModifiers()),
                chain -> null);
        Logs.i("void hooks " + prefix + " classes=" + cands.size() + " hooked=" + n);
        return n;
    }
}
