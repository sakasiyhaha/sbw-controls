# SBW Controls — Changelog

## 1.2.0 — 2026-11-24

### 核心修复：RollBird 开火方向

- 修复 A/D 偏航时普通炮弹/火箭弹偏向旋转切线的问题。
  - 直升机 A/D 偏航改用原版 `mouseMoveSpeedX`，并按旧公式反算：
    `mouseX = yaw * max_yaw_rate / (40 * syncedPropellerRot)`。
  - 鼠标滚转最初改为原版 A/D 弹性按键模拟，后因手感迟钝在同版本热修中恢复为无级 `deltaRot`。
  - 新增 `VehicleEntity.getShootVec` Mixin：服务端实际开火方向覆盖为客户端当帧瞄准方向，机炮/火箭弹/炮塔全部生效。
  - 炮塔 yaw 补偿符号修正为 `bodyYaw - aimYaw`，并按真实炮塔包线 clamp。
  - RollBird 中心瞄准现在在第一人称、FOLLOW、DYNAMIC 全部相机模式下生效。

### 相机

- 固定翼独立机位后端（`INDEPENDENT` / `SW_OVERRIDE`）：
  - 目标点使用插值后的飞机包围盒中心；
  - 相机相对机体的绝对距离/高度；
  - 独立碰撞射线 + 距离平滑；
  - 避免 SW 座位机位和 freeCamera 造成的抖动。
- 跟随模式 180° 滚转修复：
  - 载具 yaw/pitch/roll 使用最短角插值；
  - 相机 yaw/pitch/roll 跨 ±180° 连续化；
  - 硬跟随直接使用机体姿态，减少 look-at 与 roll 混合导致的翻转抽搐。
- 动态模式机位不随机体 roll 旋转。
- 固定的 120Hz 子步弹簧、阻尼、角速度平滑、软限幅（1.1.0 引入，继续保留）。

### 输入手感

- 鼠标滚转灵敏度调整：`roll_gain` 默认/活动值降低，减少“鼠标一动就大坡度”。
- `smoothing` 默认更快；`stick_center_time` 默认更短，松杆回中更快。
- RollBird 的 A/D 偏航继续沿用 `max_yaw_rate`、客户端 smoothing/sensitivity 参数函数。
- **滚转手感热修（同版本 1.2.0 覆盖）**：
  - 移除 6 tick 脉冲按键，恢复无级 `setDeltaRot(-roll * roll_scale)`；
  - 新增独立 `roll_smoothing`（默认 `0.02s`），不再与偏航共用 `smoothing`；
  - 新增 `roll_scale`（默认 `6.0`）及服务端 UI 滑条；
  - 解决滚转“大惯性、迟钝、跟不上鼠标”的问题。

### 配置 / UI

- 视角模式页新增固定翼相机后端、独立距离、独立高度。
- 相机距离页继续提供 EXTRA / TOTAL、固定翼/直升机距离、碰撞和距离平滑。
- 服务端页面：`max_yaw_rate`、`pitch_scale`、`roll_scale`、输入超时、允许方案、载具类型、日志。
- 输入手感页：`smoothing`、独立 `roll_smoothing`、增益、死区等。
- 清理废弃配置：`yaw_scale`、`separate_yaw` 及其 UI/命令/语言键（`roll_scale` 在滚转热修中恢复）。
- 清理重复注释和不再使用的测试辅助函数。

### 网络 / 兼容

- 控制包协议版本升级到 **3**；客户端和服务端都需要 `sbwcontrol-1.2.0`。
- 新增 Mixin 配置 `sbwcontrol.mixins.json`，已写入 `mods.toml`。
- 依赖不变：Minecraft 1.20.1、Forge 47.3.0、Superb Warfare 0.8.9.2、Kotlin for Forge 4.12.0。

### 测试

- Forge GameTest 4 项全部通过：
  - 三通道输入注入；
  - MI-28 炮塔中心瞄准符号；
  - AH-6 固定炮 `getShootVec` 中心瞄准覆盖；
  - 直升机 `mouseMoveSpeedX` 偏航公式。
- 独立 Python 数值验证：矩阵链、炮塔随动律、发射/命中误差、旧符号未来侧热力图。

### 已知边界

- 固定翼使用直接 `yRot` 偏航（固定翼引擎没有同形式的 mouse X 偏航项）。
- 动态相机下屏幕中心不等于机头，但炮弹方向以发射装置/机体前向为准。
- HUD 圆圈/工字仍由 Superb Warfare 实际发射方向投影；独立的弹道预判 HUD 尚未实现。
- 大滚转/大俯仰时 Superb Warfare 的欧拉近似可能有约 1°~7° 耦合误差。

---

## 1.1.0 — 2026-11-07

- 第三人称动态相机改为固定 120Hz 子步物理弹簧。
- 新增 `spring_damping`、`rate_smoothing`、`soft_limit`。
- 相机动态页面和遥测字段更新。
- 控制方案、多人降级、载具类别过滤保持不变。

## 1.0.0 — 2026-10

- 首个公开版本：Yaw-bird / Roll-bird / Hybrid / Custom。
- 第三人称相机预设、配置界面、按键绑定、客户端/服务端命令。
- 多人无附属模组时自动降级。
