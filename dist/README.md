<p align="center">
  <img src="https://raw.githubusercontent.com/vivibibi30/hyperpguard/main/res/mipmap-xxxhdpi/ic_launcher.png" width="132" height="132" alt="HyperPGuard">
</p>

<h1 align="center">HyperPGuard</h1>

<p align="center">
  <a href="https://github.com/vivibibi30/hyperpguard/releases"><img src="https://img.shields.io/github/v/release/vivibibi30/hyperpguard?label=release&style=for-the-badge&logo=github&logoColor=white&labelColor=222" alt="release"></a>
  <img src="https://img.shields.io/badge/Android-10--16-3DDC84?style=for-the-badge&logo=android&logoColor=white&labelColor=222" alt="android">
  <img src="https://img.shields.io/badge/language-Java-ED8B00?style=for-the-badge&labelColor=222" alt="language">
  <a href="https://github.com/vivibibi30/hyperpguard/blob/main/LICENSE"><img src="https://img.shields.io/badge/license-GPL--3.0-white?style=for-the-badge&logo=gnu&logoColor=white&labelColor=222" alt="license"></a>
  <img src="https://img.shields.io/badge/status-按需维护-2da44e?style=for-the-badge&labelColor=222" alt="status">
</p>

> 源码、构建说明与完整文档：[vivibibi30/hyperpguard](https://github.com/vivibibi30/hyperpguard)
>
> **本仓库只是 LSPosed 的索引仓**：放 APK 与简介；源码、issue、更新日志都在源码仓。

## 支持的版本

Android 10 – Android 16（对应平台），LSPosed 2.2.x（模块依赖 libxposed API 102，兼容 100+）。

## 使用

1. 在 LSPosed 中启用「HyperPGuard」
2. 打开应用，首次会向 LSPosed 请求作用域，允许即可；也可手动勾选
3. 对目标应用点一次「重启」，或点批量行的「全部重启」，让应用重新加载
4. 「重启」「查看日志」「被采集数据」需要 root，首次使用请允许 KernelSU / Magisk 授权

作用域外的应用不会被注入。

## 功能（按能力分类，不逐一举目标组件与包名）

| 层面 | 控制对象 |
|---|---|
| 网络出口 | 目标进程内的 `URL`、`InetAddress`、`Socket`，规则为「域名后缀 + 可选路径」 |
| 本地存储 | 指定行为库的 `SQLiteDatabase` 写入，按表名过滤 |
| 跨应用接口 | Provider、AIDL、回调型方法，按签名匹配 |
| 采集入口 | 应用列表、使用记录、设备标识等系统查询接口 |

- 每个应用在界面里是一张卡片：右上角是它在 LSPosed 中的作用域开关，卡片内逐项规则可单独开关
- 支持自定义规则：域名、域名/路径、`类名#方法`
- 开关热重载：改完开关自动让已注入的进程换新配置，不必手动重启
- 行为库状态与自检：行为库读写计数、注入目标与实际挂载情况直接显示
- MiSight 事件库只读浏览：复制系统埋点库后查看事件与上传记录，查完即删快照，不上传
- **会影响系统功能的规则一律默认关闭**（云查、鉴权、月报、防窥分类、地震报警定位、ADB 验证、设备证书、运营商注册等），副标题写明代价
- 需要精确拦截范围时，请在自己的设备上对照界面里每个开关的描述；本页不列举具体包名

## 界面

- 底栏三个分页：**主页**（总开关、路径特征、记录未命中访问、开关热重载、行为库状态、MiSight 事件库、自定义规则、自检）、**组件拦截**（按应用分组的逐项开关，顶部可按名称与描述搜索）、**设置**（查看/分享/清理日志、隐藏桌面图标、配置导入导出、关于）。
- 深浅色跟随系统，切换后保留当前分页与滚动位置。

## 常见问题

- **某个应用开关打不开**：该包可能不存在、被冻结或与系统共享 UID，LSPosed 无法把它加入作用域
- **日志为空**：`logcat` 环形缓冲已覆盖，或目标应用还没重启过
- **功能异常**：对应的默认关闭子项被打开了，按需关掉即可
- **方法层出现 `hook miss`**：组件版本变化，网络层与隐私 API 层不受影响

## 说明

- 本模块只作用于**你拥有或获授权的设备**上的进程，不提供也不协助中断他人设备或服务
- 与设备厂商、应用开发者无隶属或合作关系；商标归各自权利人
- 模块本体不联网：仅在手动点「检查更新」时向 GitHub API 查询一次版本号，不发送设备标识或使用数据
- GPL-3.0；源码与问题反馈见 [vivibibi30/hyperpguard](https://github.com/vivibibi30/hyperpguard)
