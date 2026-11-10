# 规则说明

每条规则的判定标准：**能从代码确认用途、不承载业务功能、不在关键路径上**。三条都满足才默认开启。

## 默认开启

| 应用 | 规则 | 依据 |
|---|---|---|
| MSA | `com.miui.systemAdSolution` 方法层 | 广告使用画像按「今天已打开 = true / 最后使用 = -1」返回；OneTrack 的 `track*` / `adTrack*` 返回空。类名跨版本会漂移，按修饰符 + 参数 + 返回类型 + 包名组合判定 |
| MSA | `requestSplashScreen` 系列 | 开屏广告：类名前缀 `com.miui.app.splashad.` / `com.miui.systemAdSolution.splashscreen.`，方法名以 `requestSplashScreen` 开头、返回 `Intent`、两个参数。返回空表示「没有可展示的广告」，系统按无广告分支进入应用，应用自身启动页不受影响。实测 2026.08 版本命中 2 个方法 |
| wmsvc | OneTrack 方法层 + `tracking.*.miui.com` `sdkconfig.*.xiaomi.com` | 网络管理服务的诊断埋点。327 个类的小 dex 里内嵌 OneTrack 2.0.9，`track*` / `adTrack*` 返回空；只停埋点，不影响网络设置与信号 |
| aicr | OneTrack 方法层 + 同上域名 | AI 内容识别组件内嵌 OneTrack；本地识别与模型推理不经过埋点 |
| 搜狗输入法 | `kpi2.android.shouji.sogou.com` `instantpb.android.shouji.sogou.com` | 搜狗自有统计链路，两个域名都来自 dex 常量；与 OneTrack 无关，只影响埋点 |
| 搜狗输入法 | `com.sohu.inputmethod.clipboard.ClipboardLogHelper` 的 static void 方法 | 剪贴板内容与长度的记录／回传入口，类名是保留下来的语义名；输入、候选与词库不经过该类 |
| 小米输入法 | OneTrack 方法层 + 同上域名 | 内嵌 OneTrack；自研击键统计（`duration_s` / `key_cnt` / `action_list`）类名混淆且无稳定域名，未覆盖 |
| 百度输入法 | `com.baidu.stats.*`、`com.baidu.speech.utils.analysis.*` 的 void 方法 | 百度统计 SDK 的记录与上传入口，以及语音识别的 ASR 采集上传；只停埋点，语音输入本身不受影响 |
| 讯飞输入法 | `com.iflytek.statssdk.*` 的 void 方法 | 讯飞统计 SDK 的记录与上传入口 |
| SystemHelper | `dm.wo.com.cn` | 唯一已知上报地址 `http://dm.wo.com.cn:18080/register`（明文 HTTP、应用层加密封装），硬编码在 `c.d.a.b.d` 常量中；组件为运营商设备管理 / 网络优化用途，非恶意软件 |
| guardprovider | `flash.sec.miui.com` | 应用特征上传接口 |
| guardprovider | `detection-aigc.engine.miui.com` | AI 通话预警云检测 |
| guardprovider | `fraud-detection.engine.miui.com` | 反诈配置拉取 |
| analytics | `*.miui.com` `*.xiaomi.com` `*.xiaomi.net` | 整域拦截，覆盖 tracking / om.tracking / 区域与备份域 / sdkconfig |
| analytics | `dns.google` `8.8.8.8` `8.8.4.4` | 6.33.0 国际版 DoH + IP 缓存重试通道 |
| securitycenter | `virus-engine.sec.miui.com` | 默认关闭：风险应用云查 |
| securitycenter | `framework.sec.miui.com` | 默认关闭：XSOF 合作方鉴权 |
| securitycenter | `data.sec.miui.com` | 默认关闭：月报、防窥/广告等配置接口 |
| securitycenter | `flash.sec.miui.com/user/cat` | 默认关闭：上传非系统应用包名数组（防窥分类） |
| securitycenter | `api.sec.miui.com/earthquake/volunteer/sensor/upload` | 默认开启：志愿者传感器，OAID + 经纬度 |
| securitycenter | `api.sec.miui.com/LBS/`、`/earthquake/`、`srv.sec.miui.com/earthquake/`、`gateway-papa.cloud.weizhi.com`、`eps.caict.ac.cn` | 默认关闭：地震预警 / 报警定位 / EPS |
| securitycenter | `srv.sec.miui.com/data/adb` | 默认关闭：ADB 授权验证 |
| guardprovider | `virus-engine.sec.miui.com` | 默认关闭：Flamingo 云查上传应用清单 |
| trustservice | `find.api.micloud.xiaomi.net` | 默认关闭：设备证书 / 密钥上报 |
| registration | `typdm.tydevice.com` | 默认开启：明文 HTTP 上传 CTEI / IMEI / MAC |
| registration | `zzhc.vnet.cn` | 默认关闭：设备注册（IMEI / IMSI / ICCID） |
| securitycenter | `fraud-detection.engine.miui.com` | 反诈配置 |
| securitycenter | `tracking.*.miui.com`、`sdkconfig.*.xiaomi.com` 共 10 条 | OneTrack 统计埋点；host 为裸域名（CN/RU/INTL/IN/IN/EU 五套），无路径依赖 |
| securitycenter | `flash.sec.miui.com` + `/detect/` | 检测接口 |
| packageinstaller | `api.installer.xiaomi.com` + `/avl/upload` | 上传接口 |
| packageinstaller | `api.installer.xiaomi.com` + `/statistics/appactive` | 活跃统计埋点 |
| packageinstaller | `api.installer.xiaomi.com` + `/info/layout` | 返回 `AdModel`，广告位配置 |
| market | `sdkconfig.ad.xiaomi.com` | 广告 SDK 配置 |
| xmsf | `adv.sec.miui.com` | 广告接口 |

## 默认关闭（会影响功能）

| 应用 | 规则 | 影响 |
|---|---|---|
| trustservice | `deviceapi.micloud.xiaomi.net` + MRM 屏蔽 | 远程认证、设备证书、查找设备 |
| 翻译组件 | `xiaomixiaoai.com` + `/speech/` | 语音识别；文字翻译不受影响 |

## 明确不拦

| 目标 | 原因 |
|---|---|
| `virus-engine.sec.miui.com` | 病毒库下载，拦了查杀能力下降 |
| `api.installer.xiaomi.com/interceptcheck` | 游戏安装安全拦截，属功能接口 |
| `find.api.micloud.xiaomi.net` | 设备证书（`PubCertRevise`），影响查找设备 |
| `account.*.xiaomi.com` / `account.*.xiaomixiaoai.com` | 登录与账号，拦了会导致应用不可用 |
| 安装器下载域名 | 应用下载安装主流程 |

## 设计约束

1. **域名规则必须使用完整子域**。使用 `xxx.com` 这类通配会连带拦掉同域下的功能接口，`sec.miui.com` 的教训即在此。
2. **路径规则使用精确前缀匹配**（`startsWith`），不做全路径 `contains`，避免跨进程误伤。
3. **方法层与域名层互不依赖**。方法层失配只影响自身，域名层继续工作；反之亦然。
4. **失败时倾向拦截**。读取不到配置、规则为空时按"拦截"处理，避免静默放行。
5. **可观测**。每次拦截、每次 hook 成功/失配都会落一条日志，便于定位是哪一层在起作用。

## 扩展新规则

1. 打开「记录未拦截访问」跑一段时间，收集目标进程实际访问的域名（日志中带 `?` 前缀）
2. 在该 APK 中确认域名的全部子域，排除功能接口
3. 在 `HookEntry` 对应分支增加 `Rule("host")` 或 `Rule("host", "/path")`
4. 在 `Config` 增加开关常量，在 `MainActivity` 增加对应条目
5. 重新构建，在目标应用上验证 `X(host)` / `X(host+path)` 日志（grep 用 `grep -F 'X(host'`）

若某接口同时承担上报与功能，优先按路径拆分，不要整域拦截。
