# PrivacyGuard

面向 MIUI / HyperOS 的隐私上报拦截模块（LSPosed / Xposed）。

[![Build APK](https://github.com/vivibibi30/pguard/actions/workflows/build.yml/badge.svg)](https://github.com/vivibibi30/pguard/actions/workflows/build.yml)

预装系统组件会持续采集设备标识、应用使用记录、位置、通话行为等信息，并通过网络或跨进程接口提供给厂商、运营商或第三方。本模块在**采集入口、本地存储、对外接口、网络出口**四个环节同时设防，按应用分组独立开关。

> 仅供个人在自有设备上学习研究使用。使用前请阅读 [DISCLAIMER.txt](DISCLAIMER.txt)。

---

## 功能

### 网络层

在 `java.net.URL#openConnection`、`InetAddress#getByName/getAllByName`、`Socket` 构造与 `connect` 四个位置拦截，同时覆盖 `HttpURLConnection` 与 OkHttp 两类客户端。规则形式为「域名后缀 +（可选）精确路径前缀」，命中即抛异常，连接不会建立。

### XSOF 行为拦截

小米安全中心的 XSOF 提供「某个时间窗口内是否发生过截图 / 屏幕共享 / 通话」等查询能力，第三方应用认证后即可调用。本模块在四条通道上同时封堵：

| 层 | 目标 | 处理 |
|---|---|---|
| Provider 查询 | `XSOFProvider.query()` | 返回空 Cursor |
| Provider 调用 | `XSOFProvider.call()` | 返回空 Bundle |
| Provider 写入 | `XSOFProvider.insert()` | 返回 null |
| AIDL 接口 | `MiSafetyDetectImpl` 的 7 个方法 | 回调 `(null, 1017)` |
| 本地写库 | `Ra.k` 的 27 个写入方法 | 直接返回，不落盘 |
| 风险评分 | `Pa.b` 评分方法 | 恒返回 0 |

结果是：第三方应用查不到行为记录，本地也不产生新记录。

### 隐私 API 层

不只拦网络出口，直接在数据来源处截断（框架公开签名，与组件版本无关）：

| 目标 | 处理 | 作用范围 |
|---|---|---|
| `ApplicationPackageManager#getInstalledPackages/getInstalledApplications` | 返回空列表 | 小米统计、系统安全组件 |
| `UsageStatsManager#queryEvents/queryUsageStats` | 返回 `null` / 空列表 | 小米统计 |
| `TelephonyManager#getDeviceId/getImei/getMeid/getSubscriberId/getSimSerialNumber` | 返回 `null` | 小米统计、系统安全组件、安全中心、SystemHelper、运营商服务 |
| `SmsManager#sendTextMessage/sendMultipartTextMessage/sendDataMessage` | 不发送 | 运营商服务（短信注册兜底） |
| `Context#sendBroadcast`（action = `com.xiaomi.xmsf.push.XMSF_UPLOAD_ACTIVE`） | 丢弃 | 小米统计交给 XMSF 代发的材料 |

### 方法层

| 目标 | Hook 点 | 效果 |
|---|---|---|
| SystemHelper | `c.d.a.b.f#a(String, byte[], byte[])` | 发送函数返回 false |
| SystemHelper | `c.d.a.b.e#a(String)` | 不再写入待发队列 |
| trustservice | `ServiceManager.getService/waitForService` 含 `mrm` | MRM 服务取不到，远程认证链断开 |
| analytics | `d.f.n#y(long,long)`（6.19.0 实测） | 应用使用记录不生成 |
| analytics | `onetrack.p.u#w` / `c.f.k#y` / `onetrack.q.c#a,b,c`（旧版） | 存在才挂，版本不符仅记 `hook miss` |
| analytics | 扫描所有 `(long, long) -> JSONObject` 方法（6.33.0 起包名变成 `a.b.a.*`） | 命中即置空，日志输出 `analytics scan scanned=N hooked=N` |

四层同时生效，任一层失效时其余层仍然工作。

### 广告

应用商店的广告配置下发、服务框架的广告接口、安装器的广告位配置一并拦截。

---

## 界面预览

| 总开关 / 需要 root 的功能 | 分组卡片与子项 |
|---|---|
| ![主界面](docs/images/ui-main.png) | ![分组卡片](docs/images/ui-cards.png) |

每组右上角的开关就是该应用在 LSPosed 里的作用域开关；组关闭时，组内子项置灰且不可操作。

---

## 覆盖组件

| 组件 | 包名 | 拦截内容 | 默认 |
|---|---|---|---|
| SystemHelper | `com.mobiletools.systemhelper` | 运营商遥测；默认整进程断网（无用户功能） | 开 |
| 系统安全组件 | `com.miui.guardprovider` | 应用列表云查、AI 通话预警云检测、反诈配置拉取；Flamingo 云查上传默认关闭 | 开 |
| 安全中心 | `com.miui.securitycenter` | XSOF 查询与写库、反诈配置拉取、志愿者传感器上报；云查 / 合作方鉴权 / 月报 / 防窥分类 / 地震报警定位 / ADB 验证默认关闭 | 开 |
| 小米统计 | `com.miui.analytics` | 整域 + DoH 旁路；默认整进程断网 | 开 |
| 应用安装器 | `com.miui.packageinstaller` | AVL 上报、活跃统计、安装页广告位 | 开 |
| 应用商店 | `com.xiaomi.market` | 广告配置下发 | 开 |
| 小米服务框架 | `com.xiaomi.xmsf` | 广告接口 | 开 |
| 小米可信服务 | `com.xiaomi.trustservice` | MRM 远程认证链（关）、设备证书/密钥上报（关） | 关 |
| 翻译组件 | `com.xiaomi.aiasst.vision` | 语音识别上传（`/speech/`） | 关 |
| 运营商服务 | `com.xiaomi.registration` | 设备注册（zzhc，关）、明文 CTEI 上报（开） | — |

默认关闭的两项（小米可信服务、翻译组件）会影响设备证书、查找设备与语音识别，需要时再单独开启。

---

## 已验证环境

| 项目 | 环境 |
|---|---|
| 系统 | Android 16 / HyperOS |
| 框架 | LSPosed 2.2.1（libxposed API 102） |
| 安全中心 | 12.8.6 |
| 系统安全组件 | 2.5.6 |
| SystemHelper | 3.0.1 |
| 应用安装器 | 正常拦截 `/avl/upload`、`/info/layout` |

其他机型与系统版本需自行检查日志中的 `hook miss` 与抓取模式记录。本模块不按版本号限制加载，类名或签名变化时方法层会自动跳过，网络层不受影响。

---

## 环境要求

- Android 10 及以上（minSdk 29）
- Root：KernelSU / Magisk / APatch
- LSPosed（支持 libxposed API 100+，实测 2.2.1）

本模块不使用 DexKit 或任何 DEX 扫描，APK 约 49 KB（含 libxposed service 与 AIDL），进程启动时开销可忽略。

---

## 安装

1. 安装 `PrivacyGuard-1.1.0.apk`
2. 打开 LSPosed → 模块，启用「隐私上报拦截」
3. 首次打开模块会自动向 LSPosed 请求全部作用域，允许即可；也可在 LSPosed 里手动勾选：

   ```
   com.mobiletools.systemhelper    com.miui.guardprovider
   com.miui.securitycenter         com.miui.analytics
   com.miui.packageinstaller       com.xiaomi.market
   com.xiaomi.xmsf                 com.xiaomi.trustservice
   com.xiaomi.aiasst.vision
   ```

4. 在模块里对目标应用点一次「重启」，让它重新加载即可生效

---

## 使用

### 分组开关

每个应用一张卡片，右上角是**该组总开关**。关掉后这个应用完全不注入，不产生任何 hook。

卡片内部是子项开关，可以只关某一条规则。例如只想要 XSOF 拦截、不要域名拦截，把「反诈配置拉取」关掉即可。

### 作用域联动

每个应用卡片右上角的「启用」开关**直接控制 LSPosed 作用域**，走的是 libxposed 官方接口：

| 动作 | 接口 |
|---|---|
| 读当前作用域 | `XposedService#getScope()` |
| 加入作用域 | `XposedService#requestScope(list, callback)` |
| 移出作用域 | `XposedService#removeScope(list)` |

界面上的组开关状态不是本地默认值，而是每次绑定服务、每次回到界面时用 `getScope()` 回填的真实结果；
切换时先回弹，等 `onScopeRequestApproved` 回调成功才真正落地。
应用总开关关闭时，其下子项变灰且不可操作；重新打开后恢复。

旧版本采用的「直接改 `modules_config.db`」已废弃：LSPosed 有内存缓存，改库不但不刷新界面，
还容易被覆盖回旧值。

### 重启

改完开关点卡片上的「重启」，模块会执行 `am force-stop <包名>`。应用下次启动时重新注入，新配置才会生效。

系统常驻组件（安全中心、服务框架）被停止后通常几秒内会被系统重新拉起，这正好等于自动完成重启。

### 抓取模式

打开后，**没有被拦截**的访问也会记录一条日志（同一域名 60 秒内只记一次）。

用途：发现漏网的上报域名。跑一段时间 → 关掉 → 查看记录，日志里带 `?` 的条目就是「访问了但没被规则命中」的目标。

平时保持关闭。

### 日志

「查看记录」读取系统 logcat 中本模块的输出，需要 root 授权。

「分享日志」直接拉起系统分享，可发到微信 / QQ / 邮件。

---

## 日志说明

| 前缀 | 含义 |
|---|---|
| `attach <包名>` | 模块注入某进程成功 |
| `X(host)` | 域名规则命中 |
| `X(host+path)` | 域名 + 路径规则命中 |
| `X(dns)` | DNS 解析层命中 |
| `? <tag> <host>` | 抓取模式记录：访问未被拦截 |
| `hook ok` | 方法层 hook 安装成功 |
| `hook miss` | 方法层类名或签名不匹配（域名层仍在工作） |
| `xsof provider hooked` | XSOF Provider 三个入口已拦截 |
| `xsof aidl hooked=N` | XSOF AIDL 接口拦截数量 |
| `xsof db write hooked=N` | XSOF 本地写库方法拦截数量 |
| `xsof score hooked=N` | 风险评分方法拦截数量 |

日志不输出电话号码、行为数据库内容或调用参数。

---

## 如何确认生效

1. 启用模块，并对目标应用点一次「重启」
2. 打开任意一个受保护的应用，触发一次它的上报行为（例如打开安全中心、安装一个应用）
3. 在模块里点「查看记录」，应看到：

   ```
   [PrivacyGuard] attach guardprovider, rules=3
   [PrivacyGuard] guardprovider X(host) flash.sec.miui.com
   ```

4. 检查 XSOF 部分是否全部为 `hooked`，没有 `miss`：

   ```
   [PrivacyGuard] xsof provider hooked
   [PrivacyGuard] xsof aidl hooked=7
   [PrivacyGuard] xsof db write hooked=27
   [PrivacyGuard] xsof score hooked=1
   ```

5. 若某个应用日志里只有 `attach` 没有 `X(...)`，说明规则没命中——开抓取模式跑一段时间，把 `?` 开头的域名补进规则。

---

## 构建

需要 JDK 11+、build-tools 35 及以上（`d8` / `aapt2` / `zipalign` / `apksigner`），以及一份 `android.jar`。

```sh
export ANDROID_JAR=/path/to/platforms/android-35/android.jar
export D8=/path/to/build-tools/35.0.0/d8
export AAPT2=/path/to/build-tools/35.0.0/aapt2
export ZIPALIGN=/path/to/build-tools/35.0.0/zipalign
export APKSIGNER=/path/to/build-tools/35.0.0/apksigner
sh build.sh
```

打包顺序：`javac → d8 → aapt2 link → 追加 classes.dex → 追加 META-INF → zipalign → apksigner`。

`aapt2 link` 需要一份带资源表的 jar。CI 用完整平台包；本地如果 `android.jar` 资源表被裁剪过，
放一份 `android-stub.jar` 在仓库根目录，脚本会自动优先使用它（也可以用 `AAPT2_INCLUDE` 指定）。

CI：`.github/workflows/build.yml` 在每次推送到 `main` 时自动构建，产物以 artifact 形式提供。

---

## 源码结构

| 文件 | 职责 |
|---|---|
| `HookEntry.java` | 入口，继承 `XposedModule`，在 `onPackageReady` 按包名分发 |
| `Config.java` | 开关常量；hook 侧通过 `XposedInterface#getRemotePreferences` 读取 UI 写入的远程偏好，读不到时退回默认值 |
| `MainActivity.java` | 分组开关面板、作用域联动、重启、日志查看与分享 |
| `assets/DISCLAIMER.txt` | 免责声明，应用内可查看 |

```
src/io/github/vivibibi30/pguard/   源码
res/values/                        app_name 等字符串资源
assets/                            免责声明
META-INF/xposed/                   模块声明（必须打到 APK 根）
libs/libxposed-api.jar             libxposed API（compileOnly，不打进 dex）
libs/libxposed-service.jar         service + interface 合并版（必须打进 dex）
libs/api-82.jar                    传统 Xposed API（编译兼容用）
docs/                              技术笔记与规则说明
build.sh                           一键构建
```

---

## 适配边界

- 拦截范围限于已经识别的路径与规则，不代表覆盖 HyperOS 的全部采集或联网行为
- 方法层依赖当前版本的类名与签名，系统组件升级后可能 `hook miss`；此时网络层仍然工作
- 使用 native 网络栈（如 Cronet）的请求不受 Java 层 hook 影响
- analytics 会把材料通过 XMSF 广播交出去，这一段属于跨进程交接，只拦 analytics 自身 UID 覆盖不到
- 域名名单可能漏项：地址可由配置下发；SystemHelper 与小米统计为整进程断网，其余组件依赖隐私 API 层与方法层
- 若某个域名同时承担上报与功能，本模块按路径拆分处理；未拆分的条目默认关闭
- 本地数据库不再增长不能单独证明不存在其他采集或上传途径
- 模块不修改系统 APK，不删除已有数据，停用后行为立即恢复

---

## 问题反馈

提交 Issue 时请提供：

- 机型与完整系统版本
- 目标组件的版本号
- 模块版本
- 相关日志（可在应用内「分享日志」导出，注意脱敏）

如涉及某次操作，请说明复现步骤、预期结果、实际结果，以及停用模块后是否能恢复。

---

## 参考来源

以下项目的公开资料提供了识别目标与验证思路。本项目代码独立编写，未复制、未反编译复用其实现。

- **HyperCeiler**（GPL-3.0，Sevtinge）— analytics 目标方法、`flash.sec.miui.com/detect/app`、安装器 `/avl/upload`、MRM 拦截思路
- **XSOF Privacy**（yunyizea）— 8 类行为记录的拦截范围、`(null, 1017)` 拒绝语义、按组组织 hook
- **Hyper 隐私卫士**（`io.github.hyperprivacyguard`）— SystemHelper 采集入口特征、guardprovider 应用列表采集
- **HyperOS4-XSOF-Analysis**（FAQQAQ）— 7 个组件的静态逆向记录：整域范围、`om.tracking.miui.com/track/mt`、区域与备份域、DoH 重试、`framework.sec.miui.com`、`virus-engine.sec.miui.com`、地震/报警/EPS 族、`typdm.tydevice.com` 明文上报、analytics 6.33.0 的 `a.b.a.*` 结构
- **libxposed API**（Apache-2.0）、**LSPosed** — 模块入口与作用域接口

第三方组件：`Xposed API 82`、`libxposed API 102`（均为 Apache-2.0，编译期依赖）。

感谢以上作者与维护者。

## 更新日志

### 1.1.1（对比 1.1.0）

| 组件 | 1.1.0 | 1.1.1 |
|---|---|---|
| SystemHelper | 拦截 `dm.wo.com.cn` 等规则 | 新增「全断网」子项（默认开），该进程所有非本机请求全部拦截 |
| 小米统计 | 整域拦截 + DoH 旁路 | 新增「全断网」子项（默认开） |
| 其余组件 | — | 不变 |

### 1.1.0（对比 1.0.1）

| 项 | 1.0.1 | 1.1.0 |
|---|---|---|
| 拦截层次 | 网络 + 方法 + XSOF | 增加隐私 API 层（应用列表 / 使用记录 / 设备标识 / 短信 / XMSF 代发广播） |
| 小米统计 | 仅域名 | 整域 + 备份域 + DoH + 签名扫描方法层 |
| 运营商服务 | 无 | 新分组：明文 CTEI 默认拦、设备注册默认关 |
| 安全中心 | 3 个子项 | 8 个子项，其中 5 个默认关闭 |
| 其余 | — | 修复组门 fallback 导致六段 hook 未安装 |

### 1.0.1（对比 1.0.0）

- 迁移到 libxposed API 102（`XposedModule` / `onPackageReady`）
- 作用域改为 `getScope` / `requestScope` / `removeScope`，不再改 `modules_config.db`
- 组开关按真实作用域回填；组关闭时子项置灰不可操作
- 「重启」「查看记录」的 root 提示移入设置卡片，启动不再自动 `su`
- 构建：固定签名并校验指纹，`META-INF` 打进 APK 根目录，CI 使用 build-tools 35

### 1.0.0

首个版本：四环节拦截、按应用分组、传统 Xposed API 82。

---

## 免责声明

- 本模块仅用于在**本人拥有完全控制权**的 Android 设备上限制预装组件的上报行为，不提供任何入侵、破解他人系统或获取他人数据的能力
- 不做商业销售、付费分发，不得移除声明后再次分发
- 模块自身不联网、不收集、不上传任何信息；开关配置存在本地，日志只在本地 logcat 读取
- 可能影响依赖同一接口的系统功能（设备证书、查找设备、病毒库更新、语音识别等），默认关闭的项目需自行评估后开启
- 按「现状」提供，不提供任何明示或暗示担保；因使用或无法使用本模块造成的损失由使用者自行承担

完整八条条款见 [DISCLAIMER.txt](DISCLAIMER.txt)。

---

## 许可

GPL-3.0，见 [LICENSE](LICENSE)。

使用前请阅读 [DISCLAIMER.txt](DISCLAIMER.txt)。

---

### English

Xposed module for MIUI / HyperOS that blocks data collection and uploads from
preinstalled system components: carrier telemetry, cloud app scanning,
installer stats, ads, and XSOF behaviour queries.

Interception happens at four stages — collection entry, local storage, IPC
interfaces, and network egress — with per-app switches. Requires root + LSPosed.
No DEX scanning; ~49 KB. Requires libxposed API 100+ (tested on LSPosed 2.2.1).

Personal use only. See [DISCLAIMER.txt](DISCLAIMER.txt).
