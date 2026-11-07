# SBW Controls — 宣传素材与推广指南

这份文档给想帮忙宣传 SBW Controls 的人使用：
- 可以直接复制短文案发到 QQ 群 / Discord / Bilibili / Reddit / 论坛；
- 可以按内容创作者 Brief 录视频；
- 可以按 outreach 模板联系 UP 主 / 主播 / 服主。

> 请先阅读最后的“宣传底线”：这是非官方附属，必须标注 Superb Warfare 原作者，不刷屏、不冒充官方。

---

## 1. 一句话定位

**中文：**

> 给《卓越前线》的固定翼和直升机加上“翻滚鸟”式鼠标操控 + 战地风动态第三人称瞄准相机。

**English:**

> Roll-bird style mouse flight controls and a BF-style dynamic third-person aiming camera for Superb Warfare.

**更短（标题/缩略图用）：**

- 卓越前线也能“翻滚鸟”了：鼠标滚转 + A/D 偏航 + 动态镜头
- 把卓越前线的飞行手感，调成战地/三角洲
- SBW Controls：卓越前线飞行操控与动态相机附属

---

## 2. 目标玩家

- 觉得卓越前线固定翼“太像原版”、想要鼠标滚转 + A/D 偏航的玩家；
- 喜欢战地 / 三角洲 / 战雷那种第三人称载具镜头，觉得原版镜头太“粘”的玩家；
- 想给每架固定翼/直升机单独调手感、又不想改 jar 的玩家；
- 做载具/空战内容的 UP 主、服主、整合包作者。

---

## 3. 五个卖点（发帖时挑 2~3 个用）

1. **翻滚鸟式操控**
   鼠标左右滚转，A/D 偏航，鼠标上下俯仰；也保留偏航鸟、偏滚鸟和完全自定义。

2. **BF 风格动态第三人称相机**
   转向滞后、滚转惯性、位置弹簧、自适应阻尼、速度 FOV、动态距离，压坡度和转弯时镜头会“慢半拍”地跟。

3. **第一人称/右键瞄准独立设置**
   右键放大不套用第三人称甩动；想加一点第一人称平滑再单独开，瞄得稳。

4. **一切都是可调的**
   游戏内 5 个设置分类、每页恢复默认、TOML、9 个原生按键、飞行中 `/sbwctl camera ...` 调参。

5. **联机自动降级，不挑服**
   服务端没装附属也能进服；客户端自动禁用方案切换，动态相机和按键仍然可用。

补充可信点：

- 不装 Mixin、不改卓越前线 jar；
- 坦克/汽车/船等其他载具完全不接管；
- 倒置后镜头能自动回正（±180° 连续角处理）；
- 支持固定翼和直升机两套默认流派，B 键和设置页双向同步。

---

## 4. 可直接复制的短文案

### 4.1 QQ 群 / Discord 中文短消息

> 【卓越前线附属】SBW Controls 1.0.0
>
> 给卓越前线的固定翼/直升机加了“翻滚鸟”操控：鼠标左右滚转、A/D 偏航；还有战地风动态第三人称相机（转向滞后/滚转惯性/位置弹簧/自适应阻尼）。右键瞄准放大有独立参数，不跟第三人称甩。
>
> 设置全在游戏里，支持 TOML 和 9 个原生按键；联机服务端没装也能进，会自动降级。
>
> 需要：MC 1.20.1 + Forge 47.3.0 + 卓越前线 0.8.9.2 + Kotlin for Forge 4.12.0
> 作者：sakasiyhaha，非官方附属，GPL-3.0
> 下载/源码：<GitHub / Modrinth / CurseForge 链接>

### 4.2 B站动态 / 小黑盒 / 论坛短帖

> 把卓越前线的飞行手感调成“翻滚鸟”是什么体验？
>
> 做了个附属 **SBW Controls**：
> - 鼠标滚转 + A/D 偏航，固定翼/直升机都能用
> - 第三人称动态相机：转弯滞后、压坡度惯性、机位弹簧、速度 FOV
> - 第一人称/右键瞄准有独立参数，不套第三人称
> - 游戏内 5 页设置 + TOML + 9 个原生按键
> - 服务端没装也能进，客户端自动降级
>
> 非官方附属，GPL-3.0，作者 sakasiyhaha。
> 想试的可以看：<链接>

### 4.3 Reddit / Discord English short post

> **SBW Controls 1.0.0 — roll-bird flight controls + dynamic third-person camera for Superb Warfare**
>
> - Mouse X roll, A/D yaw, mouse Y pitch (plus yaw-bird / hybrid / fully custom schemes)
> - BF-style third-person camera: turn lag, roll inertia, spring-arm position lag, adaptive damping, speed FOV
> - Separate first-person / ADS zoom camera settings
> - In-game settings UI, TOML, 9 native keybinds
> - Works on servers without the addon (client degrades gracefully)
>
> Requires MC 1.20.1, Forge 47.3.0, Superb Warfare 0.8.9.2, Kotlin for Forge 4.12.0.
> Unofficial addon by sakasiyhaha, GPL-3.0.
> Download/source: <links>

### 4.4 长帖标题备选

- 《卓越前线飞行操控改造：翻滚鸟 + 动态第三人称相机》
- 《我把卓越前线的固定翼飞成了翻滚鸟，还顺手改了镜头》
- 《SBW Controls：卓越前线飞行手感与镜头调参指南》
- 《Superb Warfare but with roll-bird controls and a BF-style camera》

---

## 5. 内容创作者 Brief（录视频用）

### 5.1 建议展示顺序

1. **原版对比**：同一架飞机，原版方案 vs 翻滚鸟，10 秒内看出鼠标滚转/A/D 偏航差异。
2. **A/D 偏航**：平飞时按 A/D，展示机头偏航而不是滚转。
3. **动态镜头**：连续左右压坡度转弯，展示镜头滞后/惯性；再切到“动态相机关闭”对比。
4. **参数对比**：把 `turnlag` 从 0 调到 0.4，或 `rolllag` 从 0 调到 0.2，展示镜头差异。
5. **第一人称/右键瞄准**：右键放大，说明默认不套第三人称动态；打开 FP 动态后差别。
6. **B 键切流派**：骑固定翼按 B，再去设置页看默认流派同步；骑坦克按 B 提示不生效。
7. **联机降级**：服务端不装附属时进服，展示 HUD 提示和“方案切换禁用，相机仍可用”。

### 5.2 缩略图/封面文案

- “卓越前线，翻滚鸟化”
- “鼠标滚转 + A/D 偏航”
- “BF 风动态镜头”
- “BF-style camera for Superb Warfare”

### 5.3 视频简介模板

> 本期使用 SBW Controls 附属：
> - 翻滚鸟操控 / BF 风动态第三人称相机
> - 需要 MC 1.20.1 + Forge 47.3.0 + 卓越前线 0.8.9.2 + Kotlin for Forge 4.12.0
> - 非官方附属，作者 sakasiyhaha，GPL-3.0
> - 下载/源码：<链接>
>
> 参数：动态相机开，预设 BATTLEFIELD，转向滞后 0.22s，滚转惯性 0.10s，第一人称动态关。

### 5.4 需要补充的素材

当前宣传素材只有：
- `docs/images/banner.png`
- `docs/images/logo.png`

建议录视频/截图时补充：
- 第三人称压坡度转弯的连续截图（3~4 张）
- 设置页“控制 / 相机”两张截图
- A/D 偏航 + 滚转的短 GIF
- 联机降级提示的截图

---

## 6. 联系创作者/服主的 outreach 模板

### 6.1 中文私信模板（B站/QQ/Discord）

> 你好，我是 sakasiyhaha，做了个卓越前线附属 **SBW Controls**：翻滚鸟鼠标操控 + 战地风动态第三人称相机，支持游戏内调参和联机降级。
>
> 看到你做卓越前线/载具/空战内容，觉得可能适合你的观众。想寄一份整合包/激活码不需要，mod 免费开源（GPL-3.0），如果感兴趣可以试用，觉得合适再考虑做内容；不方便也没关系。
>
> 介绍/下载：<链接>
> 需要的前置：MC 1.20.1 + Forge 47.3.0 + 卓越前线 0.8.9.2 + Kotlin for Forge 4.12.0
>
> 非官方附属，会标注卓越前线原作者。

### 6.2 English outreach template

> Hi — I'm sakasiyhaha, author of **SBW Controls**, an unofficial Superb Warfare addon: roll-bird mouse controls plus a BF-style dynamic third-person camera, with in-game tuning and graceful multiplayer degradation.
>
> I thought it might fit your vehicle/aviation content. It's free and open source (GPL-3.0). Feel free to try it; no obligation to cover it.
>
> Links: <GitHub / Modrinth / CurseForge>
> Requirements: MC 1.20.1, Forge 47.3.0, Superb Warfare 0.8.9.2, Kotlin for Forge 4.12.0.
>
> Unofficial addon; Superb Warfare credits go to its authors.

### 6.3 服主/整合包作者模板

> 你好，我是 SBW Controls 的作者。这个附属服务端可选安装：不装时客户端自动降级，装了可用服务端允许列表/通道增益统一管理。
>
> 如果你们的服务器玩卓越前线，欢迎把这个 mod 加进整合包/服务器；源码 GPL-3.0，可按需改配置。需要我补服务端配置说明可以随时说。

---

## 7. 推荐发布/宣传渠道

- **Modrinth / CurseForge**：先上架，拿到正式项目链接；
- **GitHub**：源码、release、issue tracker；
- **B站 / 抖音 / 小黑盒**：录 30~60 秒对比视频；
- **QQ 群 / Discord**：卓越前线、Minecraft 载具、空战、整合包群；
- **Reddit**：r/feedthebeast、r/MinecraftMods（注意各版规，避免重复刷）；
- **MCMod**：中文百科条目，适合长期搜索流量；
- **服主/整合包作者**：私信而不是群发刷屏。

---

## 8. 宣传底线

1. **必须标注非官方附属**：SBW Controls 与 Superb Warfare 官方作者无隶属关系。
2. **必须标注原作者**：Superb Warfare by Atsuishio, Roki27, Light_Quanta and contributors；Kotlin for Forge by thedarkcolour。
3. **不要改作者名/仓库地址**：转发时保留 sakasiyhaha 与官方下载链接。
4. **不要打包虚假承诺**：“官方”“最强”“完美”这类词不要用；说清依赖和限制。
5. **不要刷屏**：群发/私信前先看群规；每个社区最多发一次，别重复顶帖。
6. **遵守 GPL-3.0**：转发/修改源码时保留许可证和源码链接；不要附加额外限制。
7. **下载来源**：只引导到 GitHub Releases / Modrinth / CurseForge 官方项目页，避免第三方转载包。

---

## 9. 平台文案字段（可直接填）

**Summary（中文）**  
卓越前线固定翼/直升机“翻滚鸟”操控 + 战地风动态第三人称瞄准相机，支持游戏内调参、TOML、原生按键与联机降级。

**Summary (English)**  
Roll-bird style mouse flight controls and a BF-style dynamic third-person aiming camera for Superb Warfare, fully configurable with in-game UI, TOML and native keybinds.

**Tags**  
`minecraft`, `forge`, `superb-warfare`, `flight-controls`, `vehicle-camera`, `camera`, `addon`, `1.20.1`

**License**  
GPL-3.0

**Source**  
<GitHub 仓库链接>

**Issue tracker**  
<GitHub Issues 链接>

---

## 10. 宣传素材路径

```
SBW-Controls-Pack-1.0.0/
├─ docs/
│  ├─ USAGE.zh-CN.md      # 中文详细使用说明
│  ├─ USAGE.en.md         # English user guide
│  ├─ PROMOTION.md        # 本文档
│  └─ images/
│     ├─ banner.png       # 1280×360 宣传横幅
│     └─ logo.png         # 方形图标
└─ source/
   └─ sbwcontrol-1.0.0-src.zip
```
