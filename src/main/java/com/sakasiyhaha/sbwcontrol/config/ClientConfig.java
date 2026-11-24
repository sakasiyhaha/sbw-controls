package com.sakasiyhaha.sbwcontrol.config;

import com.sakasiyhaha.sbwcontrol.camera.CameraPreset;
import com.sakasiyhaha.sbwcontrol.camera.view.CameraAimMode;
import com.sakasiyhaha.sbwcontrol.camera.view.CameraDistanceMode;
import com.sakasiyhaha.sbwcontrol.camera.view.CameraViewMode;
import com.sakasiyhaha.sbwcontrol.camera.view.FixedWingCameraBackend;
import com.sakasiyhaha.sbwcontrol.camera.view.VehicleViewMode;
import com.sakasiyhaha.sbwcontrol.control.AxisSource;
import com.sakasiyhaha.sbwcontrol.control.ControlScheme;
import com.sakasiyhaha.sbwcontrol.control.InputMode;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * 客户端配置：控制方案、轴绑定/手感、相机、调试。
 *
 * <p>所有值都可以在游戏内设置界面修改（设置界面直接读写本 spec），
 * 也可以直接编辑 {@code config/sbwcontrol-client.toml}。
 */
public final class ClientConfig {

    public static final ForgeConfigSpec SPEC;

    // ---- 控制方案 ----
    public static final ForgeConfigSpec.EnumValue<ControlScheme> AIRCRAFT_SCHEME;
    public static final ForgeConfigSpec.EnumValue<ControlScheme> HELICOPTER_SCHEME;

    // ---- 自定义方案：三个通道各自的输入源 ----
    public static final ForgeConfigSpec.EnumValue<AxisSource> CUSTOM_PITCH_SOURCE;
    public static final ForgeConfigSpec.EnumValue<AxisSource> CUSTOM_ROLL_SOURCE;
    public static final ForgeConfigSpec.EnumValue<AxisSource> CUSTOM_YAW_SOURCE;

    // ---- 轴反转与增益（对所有方案生效，便于统一微调）----
    public static final ForgeConfigSpec.BooleanValue INVERT_PITCH;
    public static final ForgeConfigSpec.BooleanValue INVERT_ROLL;
    public static final ForgeConfigSpec.BooleanValue INVERT_YAW;
    public static final ForgeConfigSpec.BooleanValue INVERT_MOUSE_Y;
    public static final ForgeConfigSpec.DoubleValue PITCH_GAIN;
    public static final ForgeConfigSpec.DoubleValue ROLL_GAIN;
    public static final ForgeConfigSpec.DoubleValue YAW_GAIN;
    /** 偏航指令占服务端最大偏航速度的比例（0~1）。 */
    public static final ForgeConfigSpec.DoubleValue YAW_SENSITIVITY;
    /** 偏滚鸟预设里，鼠标 X 额外耦合到偏航的比例（0 = 纯翻滚鸟，1 = 偏航为主）。 */
    public static final ForgeConfigSpec.DoubleValue HYBRID_MOUSE_YAW;

    // ---- 输入手感 ----
    public static final ForgeConfigSpec.EnumValue<InputMode> INPUT_MODE;
    /** STICK 模式下摇杆回中时间（秒，0 = 不回中）。 */
    public static final ForgeConfigSpec.DoubleValue STICK_CENTER_TIME;
    /** STICK 模式下鼠标位移→摇杆偏转的增益。 */
    public static final ForgeConfigSpec.DoubleValue STICK_GAIN;
    public static final ForgeConfigSpec.IntValue MOUSE_SENSITIVITY;
    /** 输入平滑秒数（0 = 不平滑）。 */
    public static final ForgeConfigSpec.DoubleValue SMOOTHING;
    /** 死区（0~0.5）。 */
    public static final ForgeConfigSpec.DoubleValue DEADZONE;

    // ---- 相机 ----
    public static final ForgeConfigSpec.BooleanValue CAMERA_ENABLED;
    public static final ForgeConfigSpec.EnumValue<CameraPreset> CAMERA_PRESET;
    /** 偏航相机滞后秒数（越大越“甩”）。 */
    public static final ForgeConfigSpec.DoubleValue CAMERA_LAG_YAW;
    public static final ForgeConfigSpec.DoubleValue CAMERA_LAG_PITCH;
    /** 相机滚转惯性平滑时间（秒）；0 = 完全跟机体滚转（原版粘合感）。 */
    public static final ForgeConfigSpec.DoubleValue CAMERA_LAG_ROLL;
    /** 固定翼 Sticky 滚转跟随开关。 */
    public static final ForgeConfigSpec.BooleanValue CAMERA_ROLL_AIRPLANE_ENABLED;
    /** 固定翼滚转耦合：0=旧行为，1=画面和机位完整随机体滚转。 */
    public static final ForgeConfigSpec.DoubleValue CAMERA_ROLL_AIRPLANE_COUPLING;
    /** 固定翼滚转跟随的弹簧滞后（秒）。 */
    public static final ForgeConfigSpec.DoubleValue CAMERA_ROLL_AIRPLANE_LAG;
    /** 直升机 Sticky 滚转跟随开关。 */
    public static final ForgeConfigSpec.BooleanValue CAMERA_ROLL_HELICOPTER_ENABLED;
    /** 直升机滚转耦合：0=旧行为，1=画面和机位完整随机体滚转。 */
    public static final ForgeConfigSpec.DoubleValue CAMERA_ROLL_HELICOPTER_COUPLING;
    /** 直升机滚转跟随的弹簧滞后（秒）。 */
    public static final ForgeConfigSpec.DoubleValue CAMERA_ROLL_HELICOPTER_LAG;
    /**
     * Sticky 滚转事件合成模式（校准用）：
     * 0 = event = smoothed - stock；1 = event = smoothed；2 = stock - smoothed；3 = -smoothed。
     */
    public static final ForgeConfigSpec.IntValue CAMERA_STICKY_ROLL_MODE;
    /** 完全刚性 Sticky：关闭 Sticky 下的动态偏移、弹簧、位置滞后，只做刚体跟随。 */
    public static final ForgeConfigSpec.BooleanValue CAMERA_STICKY_RIGID;
    /** 转向前导系数（0~1）。 */
    /**
     * 转向滞后秒数：相机偏移量 = 偏航/俯仰角速度 × 该值（越大，转弯时镜头朝反方向偏得越明显）。
     */
    public static final ForgeConfigSpec.DoubleValue CAMERA_LEAD;
    /** 速度 FOV 增益（0~0.5）。 */
    public static final ForgeConfigSpec.DoubleValue CAMERA_FOV_GAIN;
    /**
     * 转向滞后方向：false = 左偏航镜头偏右（转弯反方向滞后）；true = 反过来。
     * 方便不同相机/视野习惯下微调。
     */
    public static final ForgeConfigSpec.BooleanValue CAMERA_TURN_LAG_INVERT;
    /** 俯仰（上下）转向滞后方向：true = 上下偏移反向。 */
    public static final ForgeConfigSpec.BooleanValue CAMERA_TURN_LAG_INVERT_PITCH;
    /** 相机位置弹簧滞后（秒）：把相机相对载具中心的向量做平滑，转弯/滚转时机位绕机身滞后摆动。 */
    public static final ForgeConfigSpec.DoubleValue CAMERA_POSITION_LAG;
    /** 自适应阻尼增益：偏航/翻滚角速度越高，相机滞后时间越短（0=固定阻尼，1~3=机动时更跟手）。 */
    public static final ForgeConfigSpec.DoubleValue CAMERA_ADAPTIVE_GAIN;
    /** 弹簧阻尼比：1.0=临界阻尼（最稳），<1=更有弹性/轻微回弹，>1=更硬更快停。 */
    public static final ForgeConfigSpec.DoubleValue CAMERA_SPRING_DAMPING;
    /** 角速度低通时间（秒）：越大，输入变化越慢，镜头越不容易“瞬间弹一下”。 */
    public static final ForgeConfigSpec.DoubleValue CAMERA_RATE_SMOOTHING;
    /** 转向偏移阈值软化：旧版单段 tanh 参数，保留兼容但新逻辑不再使用。 */
    public static final ForgeConfigSpec.DoubleValue CAMERA_SOFT_LIMIT;
    /** 全局 TPS 视角模式。 */
    public static final ForgeConfigSpec.EnumValue<CameraViewMode> CAMERA_GLOBAL_VIEW_MODE;
    /** 固定翼 TPS 视角模式（可跟随全局）。 */
    public static final ForgeConfigSpec.EnumValue<VehicleViewMode> CAMERA_AIRPLANE_VIEW_MODE;
    /** 直升机 TPS 视角模式（可跟随全局）。 */
    public static final ForgeConfigSpec.EnumValue<VehicleViewMode> CAMERA_HELICOPTER_VIEW_MODE;
    /** 正交 YawChannel 增益（度 / 输入单位；负数改为反向拖尾）。 */
    public static final ForgeConfigSpec.DoubleValue CAMERA_CHANNEL_YAW_GAIN;
    /** 正交 YawChannel 平滑时间（秒）。 */
    public static final ForgeConfigSpec.DoubleValue CAMERA_CHANNEL_YAW_LAG;
    /** 正交 RollChannel 增益（度 / 输入单位；负数改为反向拖尾）。 */
    public static final ForgeConfigSpec.DoubleValue CAMERA_CHANNEL_ROLL_GAIN;
    /** 正交 RollChannel 平滑时间（秒）。 */
    public static final ForgeConfigSpec.DoubleValue CAMERA_CHANNEL_ROLL_LAG;
    /** 正交 PitchChannel 增益（度 / 输入单位）。 */
    public static final ForgeConfigSpec.DoubleValue CAMERA_CHANNEL_PITCH_GAIN;
    /** 正交 PitchChannel 平滑时间（秒）。 */
    public static final ForgeConfigSpec.DoubleValue CAMERA_CHANNEL_PITCH_LAG;
    /** 相机偏航通道反转。 */
    public static final ForgeConfigSpec.BooleanValue CAMERA_INVERT_YAW;
    /** 相机滚转通道反转。 */
    public static final ForgeConfigSpec.BooleanValue CAMERA_INVERT_ROLL;
    /** 相机俯仰通道反转。 */
    public static final ForgeConfigSpec.BooleanValue CAMERA_INVERT_PITCH;
    /** 跟随模式是否硬旋转：相机与固定翼持平、无额外动态。 */
    public static final ForgeConfigSpec.BooleanValue CAMERA_FOLLOW_HARD_ROTATION;
    /** 固定翼硬跟随时相机相对机身中心的抬高量（格）。 */
    public static final ForgeConfigSpec.DoubleValue CAMERA_HARD_FOLLOW_HEIGHT;
    /** 相机瞄准方式：锁定飞机中心 / 平行机体。 */
    public static final ForgeConfigSpec.EnumValue<CameraAimMode> CAMERA_AIM_MODE;
    /** 固定翼相机机位后端。 */
    public static final ForgeConfigSpec.EnumValue<FixedWingCameraBackend> CAMERA_FIXED_WING_BACKEND;
    /** 固定翼独立机位距离（格）。 */
    public static final ForgeConfigSpec.DoubleValue CAMERA_FIXED_WING_DISTANCE;
    /** 固定翼独立机位高度（格）。 */
    public static final ForgeConfigSpec.DoubleValue CAMERA_FIXED_WING_HEIGHT;
    /** 相机距离表达方式：额外距离 / 目标总距离。 */
    public static final ForgeConfigSpec.EnumValue<CameraDistanceMode> CAMERA_DISTANCE_MODE;
    /** 固定翼额外相机距离（格）。 */
    public static final ForgeConfigSpec.DoubleValue CAMERA_AIRPLANE_DISTANCE_EXTRA;
    /** 直升机额外相机距离（格）。 */
    public static final ForgeConfigSpec.DoubleValue CAMERA_HELICOPTER_DISTANCE_EXTRA;
    /** 固定翼目标总距离（格）。 */
    public static final ForgeConfigSpec.DoubleValue CAMERA_AIRPLANE_DISTANCE_TOTAL;
    /** 直升机目标总距离（格）。 */
    public static final ForgeConfigSpec.DoubleValue CAMERA_HELICOPTER_DISTANCE_TOTAL;
    /** 是否做相机地形碰撞保护。 */
    public static final ForgeConfigSpec.BooleanValue CAMERA_DISTANCE_COLLISION;
    /** 相机距离平滑时间（秒）。 */
    public static final ForgeConfigSpec.DoubleValue CAMERA_DISTANCE_SMOOTH;
    /** 是否使用新的 OffsetController（yaw/pitch 偏移子系统）。 */
    public static final ForgeConfigSpec.BooleanValue CAMERA_OFFSET_ENGINE_V2;
    /** 是否使用全新动态相机引擎。 */
    public static final ForgeConfigSpec.BooleanValue CAMERA_DYNAMIC_ENGINE_V3;
    /** 回正运动的最大角速度（度/秒）。 */
    public static final ForgeConfigSpec.DoubleValue CAMERA_RECENTER_MAX_SPEED;
    /** 回正运动的最大时长（秒）。 */
    public static final ForgeConfigSpec.DoubleValue CAMERA_RECENTER_MAX_TIME;
    /** 相机回正缓冲时间（秒）：松开转向后使用 slow-fast-slow 曲线回正。 */
    public static final ForgeConfigSpec.DoubleValue CAMERA_RECENTER_TIME;
    /** 是否启用中心回正。 */
    public static final ForgeConfigSpec.BooleanValue CAMERA_RECENTER_ENABLED;
    /** 松开输入后延迟多久开始回正。 */
    public static final ForgeConfigSpec.DoubleValue CAMERA_RECENTER_DELAY;
    /** 回正误差圆参数：基础半径、速度/活动度增益、迟滞与空闲收小。 */
    public static final ForgeConfigSpec.DoubleValue CAMERA_RECENTER_RADIUS;
    /** 速度让误差圆扩大的增益。 */
    public static final ForgeConfigSpec.DoubleValue CAMERA_RECENTER_SPEED_GAIN;
    /** 机动活动度让误差圆扩大的增益。 */
    public static final ForgeConfigSpec.DoubleValue CAMERA_RECENTER_ACTIVITY_GAIN;
    /** 回正迟滞半径（度）。 */
    public static final ForgeConfigSpec.DoubleValue CAMERA_RECENTER_HYSTERESIS;
    /** 停止输入后多少秒开始慢慢收小误差圆。 */
    public static final ForgeConfigSpec.DoubleValue CAMERA_RECENTER_IDLE_SETTLE;
    /** 边缘误差圆基础半径（归一化，1=最大偏移）。 */
    public static final ForgeConfigSpec.DoubleValue CAMERA_EDGE_RADIUS;
    /** 速度让边缘误差圆扩大的增益。 */
    public static final ForgeConfigSpec.DoubleValue CAMERA_EDGE_SPEED_GAIN;
    /** 机动活动度让边缘误差圆扩大的增益。 */
    public static final ForgeConfigSpec.DoubleValue CAMERA_EDGE_ACTIVITY_GAIN;
    /** 单侧预测制动的起始区间（归一化）。 */
    public static final ForgeConfigSpec.DoubleValue CAMERA_EDGE_BRAKE_ZONE;
    /** 单侧预测制动强度。 */
    public static final ForgeConfigSpec.DoubleValue CAMERA_EDGE_BRAKE;
    /** 边缘制动预测时间（秒）。 */
    public static final ForgeConfigSpec.DoubleValue CAMERA_EDGE_LOOKAHEAD;
    /** 边缘制动释放/平滑时间（秒）。 */
    public static final ForgeConfigSpec.DoubleValue CAMERA_EDGE_BRAKE_RELEASE;
    /** 边缘制动滞回（归一化半径）。 */
    public static final ForgeConfigSpec.DoubleValue CAMERA_EDGE_BRAKE_HYSTERESIS;
    /** 边缘最大速度限制比例；0=只靠阻尼，0.75=强限速。 */
    public static final ForgeConfigSpec.DoubleValue CAMERA_EDGE_SPEED_LIMIT;
    /** 横向居中：1 = 把卓越前线载具相机拉回机身中线（消除部分载具的左侧机位偏移），0 = 保持原版。 */
    public static final ForgeConfigSpec.DoubleValue CAMERA_CENTER_LATERAL;
    /** 第三人称动态距离增益（0~2 格/速度单位）。 */
    public static final ForgeConfigSpec.DoubleValue CAMERA_DISTANCE_GAIN;
    /** 右键瞄准时相机动态按此比例缩放。 */
    public static final ForgeConfigSpec.DoubleValue CAMERA_ADS_SCALE;
    /** 是否额外绘制“机头/炮口方向投影准星”（调试/辅助用，默认关）。 */
    public static final ForgeConfigSpec.BooleanValue CAMERA_NOSE_MARKER;
    /** 用本附属的投影准星替换卓越前线第三人称载具准星（默认关）。 */
    public static final ForgeConfigSpec.BooleanValue CAMERA_REPLACE_CROSSHAIR;
    /**
     * 相机滚转跟随比例（1.0 = 原版；0 = 画面完全不跟机体滚转，0.7 = 减弱）。
     * 实现：按卓越前线 GameRendererMixin 的同一公式算出它加到 pose 上的滚转量，
     * 再用 event.roll 抵消/缩放（Z 轴旋转可叠加）。
     */
    public static final ForgeConfigSpec.DoubleValue CAMERA_ROLL_RATIO;
    /** 第一人称是否也应用相机动态。 */
    public static final ForgeConfigSpec.BooleanValue CAMERA_FIRST_PERSON;
    /** 第一人称（含右键瞄准的第一人称放大）专用参数，避免套用第三人称动态设置。 */
    public static final ForgeConfigSpec.DoubleValue CAMERA_FP_LAG_YAW;
    public static final ForgeConfigSpec.DoubleValue CAMERA_FP_LAG_PITCH;
    public static final ForgeConfigSpec.DoubleValue CAMERA_FP_TURN_LAG;
    public static final ForgeConfigSpec.DoubleValue CAMERA_FP_ROLL_LAG;
    public static final ForgeConfigSpec.DoubleValue CAMERA_FP_POSITION_LAG;
    public static final ForgeConfigSpec.DoubleValue CAMERA_FP_ADAPTIVE_GAIN;

    // ---- 调试 ----
    public static final ForgeConfigSpec.BooleanValue DEBUG_HUD;
    /** 是否把每 tick 的飞行/相机参数写入 run/sbwcontrol-telemetry/*.jsonl（供 DSH 遥测插件分析）。 */
    public static final ForgeConfigSpec.BooleanValue TELEMETRY_ENABLED;
    /** 遥测采样间隔（tick，1 = 每 tick）。 */
    public static final ForgeConfigSpec.IntValue TELEMETRY_INTERVAL_TICKS;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.comment("Control scheme settings").push("control");
        AIRCRAFT_SCHEME = b
                .comment("Fixed-wing control scheme: YAW_BIRD (stock), ROLL_BIRD (mouse roll + A/D yaw), ROLL_YAW_HYBRID, CUSTOM")
                .defineEnum("aircraft_scheme", ControlScheme.ROLL_BIRD);
        HELICOPTER_SCHEME = b
                .comment("Helicopter control scheme: same values as aircraft")
                .defineEnum("helicopter_scheme", ControlScheme.ROLL_BIRD);

        b.comment("Only used when the scheme is CUSTOM").push("custom");
        CUSTOM_PITCH_SOURCE = b.defineEnum("pitch_source", AxisSource.MOUSE_Y);
        CUSTOM_ROLL_SOURCE = b.defineEnum("roll_source", AxisSource.MOUSE_X);
        CUSTOM_YAW_SOURCE = b.defineEnum("yaw_source", AxisSource.KEYS);
        b.pop();

        INVERT_PITCH = b.comment("Invert pitch channel").define("invert_pitch", false);
        INVERT_ROLL = b.comment("Invert roll channel").define("invert_roll", false);
        INVERT_YAW = b.comment("Invert yaw channel").define("invert_yaw", false);
        INVERT_MOUSE_Y = b.comment("Invert mouse Y (pitch axis when sourced from mouse)").define("invert_mouse_y", false);
        PITCH_GAIN = b.defineInRange("pitch_gain", 1.0, 0.1, 5.0);
        ROLL_GAIN = b.defineInRange("roll_gain", 1.0, 0.1, 5.0);
        YAW_GAIN = b.defineInRange("yaw_gain", 1.0, 0.1, 5.0);
        YAW_SENSITIVITY = b.comment("Fraction of the server max yaw rate used by the client (0.0~1.0)")
                .defineInRange("yaw_sensitivity", 0.7, 0.0, 1.0);
        HYBRID_MOUSE_YAW = b.defineInRange("hybrid_mouse_yaw", 0.25, 0.0, 1.0);
        b.pop();

        b.comment("Input feel").push("input");
        INPUT_MODE = b.comment("Mouse input model: RATE (delta = rate, stock-like) or STICK (accumulated virtual stick with auto-center)")
                .defineEnum("input_mode", InputMode.STICK);
        STICK_CENTER_TIME = b.comment("STICK mode: seconds for the virtual stick to center itself (0 = never center)")
                .defineInRange("stick_center_time", 0.60, 0.0, 5.0);
        STICK_GAIN = b.comment("STICK mode: mouse movement -> stick deflection gain")
                .defineInRange("stick_gain", 1.0, 0.1, 5.0);
        MOUSE_SENSITIVITY = b.comment("Mouse sensitivity for control channels (percent)")
                .defineInRange("mouse_sensitivity", 100, 10, 300);
        SMOOTHING = b.comment("Smoothing time in seconds (0 = raw input)")
                .defineInRange("smoothing", 0.04, 0.0, 1.0);
        DEADZONE = b.comment("Input deadzone (0~0.5)")
                .defineInRange("deadzone", 0.02, 0.0, 0.5);
        b.pop();

        b.comment("Dynamic third-person camera").push("camera");
        CAMERA_ENABLED = b.define("enabled", true);
        CAMERA_PRESET = b.defineEnum("preset", CameraPreset.BATTLEFIELD);
        CAMERA_LAG_YAW = b.comment("Yaw camera lag in seconds (bigger = more lag)")
                .defineInRange("lag_yaw", 0.10, 0.0, 1.0);
        CAMERA_LAG_PITCH = b.comment("Pitch camera lag in seconds")
                .defineInRange("lag_pitch", 0.08, 0.0, 1.0);
        CAMERA_LAG_ROLL = b.comment("Legacy roll cam inertia (fallback when Sticky roll follow is disabled)")
                .defineInRange("lag_roll", 0.10, 0.0, 0.5);
        CAMERA_ROLL_AIRPLANE_ENABLED = b.comment("Fixed-wing Sticky roll follow: camera roll and position rotate with the aircraft")
                .define("roll_airplane_enabled", true);
        CAMERA_ROLL_AIRPLANE_COUPLING = b.comment("Fixed-wing roll coupling: 0 = legacy behavior, 1 = full Sticky roll follow")
                .defineInRange("roll_airplane_coupling", 1.0, 0.0, 1.0);
        CAMERA_ROLL_AIRPLANE_LAG = b.comment("Fixed-wing Sticky roll spring lag in seconds")
                .defineInRange("roll_airplane_lag", 0.10, 0.0, 0.5);
        CAMERA_ROLL_HELICOPTER_ENABLED = b.comment("Helicopter Sticky roll follow: camera roll and position rotate with the aircraft")
                .define("roll_helicopter_enabled", true);
        CAMERA_ROLL_HELICOPTER_COUPLING = b.comment("Helicopter roll coupling: 0 = legacy behavior, 1 = full Sticky roll follow")
                .defineInRange("roll_helicopter_coupling", 1.0, 0.0, 1.0);
        CAMERA_ROLL_HELICOPTER_LAG = b.comment("Helicopter Sticky roll spring lag in seconds")
                .defineInRange("roll_helicopter_lag", 0.10, 0.0, 0.5);
        CAMERA_STICKY_ROLL_MODE = b.comment("Sticky roll event composition mode (calibration): 0=compensated, 1=direct, 2=reverse, 3=negative")
                .defineInRange("sticky_roll_mode", 1, 0, 3);
        CAMERA_STICKY_RIGID = b.comment("Fully rigid Sticky camera: no dynamic offsets, no springs, no position lag")
                .define("sticky_rigid", false);
        CAMERA_LEAD = b.comment("Turn lag in seconds: camera offset = angular rate (deg/s) * this value, so turning left makes the camera trail to the right (0~0.6)")
                .defineInRange("lead", 0.22, 0.0, 0.6);
        CAMERA_TURN_LAG_INVERT = b.comment("Invert turn-lag direction (true = turning left trails to the left instead)")
                .define("turn_lag_invert", false);
        CAMERA_TURN_LAG_INVERT_PITCH = b.comment("Invert vertical (pitch) turn-lag direction")
                .define("turn_lag_invert_pitch", false);
        CAMERA_FOV_GAIN = b.comment("Speed based FOV gain (0~0.5)")
                .defineInRange("fov_gain", 0.10, 0.0, 0.5);
        CAMERA_CENTER_LATERAL = b.comment("Center the third-person camera on the aircraft centerline (1 = center, 0 = keep vehicle's stock lateral offset)")
                .defineInRange("center_lateral", 1.0, 0.0, 1.0);
        CAMERA_POSITION_LAG = b.comment("Spring-arm camera position lag in seconds: camera offset around the aircraft trails when yawing/rolling (0~0.3)")
                .defineInRange("position_lag", 0.08, 0.0, 0.3);
        CAMERA_ADAPTIVE_GAIN = b.comment("Adaptive damping: shrink camera lag when yaw/roll angular speed is high (0 = fixed damping, 1~3 = more agile)")
                .defineInRange("adaptive_gain", 1.5, 0.0, 5.0);
        CAMERA_SPRING_DAMPING = b.comment("Spring damping ratio: 1.0 = critically damped, below 1 = slightly bouncy, above 1 = stiffer")
                .defineInRange("spring_damping", 0.90, 0.5, 1.5);
        CAMERA_RATE_SMOOTHING = b.comment("Angular-rate low-pass time in seconds; higher = softer onset, less instant camera kick (0~0.3)")
                .defineInRange("rate_smoothing", 0.10, 0.0, 0.3);
        CAMERA_SOFT_LIMIT = b.comment("Soft turn-offset limit: 0 = hard clamp, 1 = tanh compression near the maximum offset")
                .defineInRange("soft_limit", 1.0, 0.0, 1.0);
        CAMERA_GLOBAL_VIEW_MODE = b.comment("Global third-person view mode")
                .defineEnum("global_view_mode", CameraViewMode.CENTERED_DYNAMIC);
        CAMERA_AIRPLANE_VIEW_MODE = b.comment("Fixed-wing third-person view mode (FOLLOW_GLOBAL uses the global mode)")
                .defineEnum("airplane_view_mode", VehicleViewMode.FOLLOW_GLOBAL);
        CAMERA_HELICOPTER_VIEW_MODE = b.comment("Helicopter third-person view mode (FOLLOW_GLOBAL uses the global mode)")
                .defineEnum("helicopter_view_mode", VehicleViewMode.FOLLOW_GLOBAL);
        CAMERA_CHANNEL_YAW_GAIN = b.comment("Orthogonal YawChannel gain in degrees per input unit; negative reverses the trail")
                .defineInRange("channel_yaw_gain", 2.5, -8.0, 8.0);
        CAMERA_CHANNEL_YAW_LAG = b.comment("Orthogonal YawChannel smoothing time in seconds")
                .defineInRange("channel_yaw_lag", 0.12, 0.0, 0.5);
        CAMERA_CHANNEL_ROLL_GAIN = b.comment("Orthogonal RollChannel gain in degrees per input unit; negative reverses the trail")
                .defineInRange("channel_roll_gain", 1.5, -8.0, 8.0);
        CAMERA_CHANNEL_ROLL_LAG = b.comment("Orthogonal RollChannel smoothing time in seconds")
                .defineInRange("channel_roll_lag", 0.12, 0.0, 0.5);
        CAMERA_CHANNEL_PITCH_GAIN = b.comment("Orthogonal PitchChannel gain in degrees per input unit")
                .defineInRange("channel_pitch_gain", 1.0, -8.0, 8.0);
        CAMERA_CHANNEL_PITCH_LAG = b.comment("Orthogonal PitchChannel smoothing time in seconds")
                .defineInRange("channel_pitch_lag", 0.12, 0.0, 0.5);
        CAMERA_INVERT_YAW = b.comment("Invert the camera YawChannel movement")
                .define("camera_invert_yaw", false);
        CAMERA_INVERT_ROLL = b.comment("Invert the camera RollChannel movement")
                .define("camera_invert_roll", false);
        CAMERA_FOLLOW_HARD_ROTATION = b.comment("Follow mode hard rotation: lock the camera level with the aircraft")
                .define("follow_hard_rotation", true);
        CAMERA_HARD_FOLLOW_HEIGHT = b.comment("Fixed-wing hard-follow camera height offset in blocks")
                .defineInRange("hard_follow_height", 1.0, 0.0, 6.0);
        CAMERA_AIM_MODE = b.comment("Camera aim mode: CENTER looks at the aircraft center; PARALLEL keeps the body axis")
                .defineEnum("aim_mode", CameraAimMode.CENTER);
        CAMERA_FIXED_WING_BACKEND = b.comment("Fixed-wing camera position backend")
                .defineEnum("fixed_wing_backend", FixedWingCameraBackend.INDEPENDENT);
        CAMERA_FIXED_WING_DISTANCE = b.comment("Fixed-wing independent camera distance in blocks")
                .defineInRange("fixed_wing_distance", 18.0, 4.0, 80.0);
        CAMERA_FIXED_WING_HEIGHT = b.comment("Fixed-wing independent camera height in blocks")
                .defineInRange("fixed_wing_height", 2.0, 0.0, 12.0);
        CAMERA_INVERT_PITCH = b.comment("Invert the camera PitchChannel movement")
                .define("camera_invert_pitch", false);
        CAMERA_DISTANCE_MODE = b.comment("Camera distance mode: EXTRA = stock distance + extra; TOTAL = target total distance")
                .defineEnum("distance_mode", CameraDistanceMode.EXTRA);
        CAMERA_AIRPLANE_DISTANCE_EXTRA = b.comment("Fixed-wing extra TPS camera distance in blocks")
                .defineInRange("airplane_distance_extra", 9.0, 0.0, 30.0);
        CAMERA_HELICOPTER_DISTANCE_EXTRA = b.comment("Helicopter extra TPS camera distance in blocks")
                .defineInRange("helicopter_distance_extra", 3.0, 0.0, 30.0);
        CAMERA_AIRPLANE_DISTANCE_TOTAL = b.comment("Fixed-wing target total TPS camera distance in blocks")
                .defineInRange("airplane_distance_total", 20.0, 4.0, 80.0);
        CAMERA_HELICOPTER_DISTANCE_TOTAL = b.comment("Helicopter target total TPS camera distance in blocks")
                .defineInRange("helicopter_distance_total", 12.0, 4.0, 80.0);
        CAMERA_DISTANCE_COLLISION = b.comment("Clamp the TPS camera distance with a world ray collision check")
                .define("distance_collision", true);
        CAMERA_DISTANCE_SMOOTH = b.comment("TPS camera distance smoothing time in seconds")
                .defineInRange("distance_smooth", 0.12, 0.0, 0.5);
        CAMERA_OFFSET_ENGINE_V2 = b.comment("Use the isolated OffsetController for yaw/pitch dynamics (A/B toggle)")
                .define("offset_engine_v2", true);
        CAMERA_DYNAMIC_ENGINE_V3 = b.comment("Use the new from-scratch dynamic camera engine")
                .define("dynamic_camera_engine_v3", true);
        CAMERA_RECENTER_MAX_SPEED = b.comment("Maximum recenter speed in degrees per second")
                .defineInRange("recenter_max_speed", 20.0, 5.0, 90.0);
        CAMERA_RECENTER_MAX_TIME = b.comment("Maximum recenter duration in seconds")
                .defineInRange("recenter_max_time", 1.0, 0.25, 3.0);
        CAMERA_RECENTER_TIME = b.comment("Recenter buffer in seconds: after releasing yaw/pitch, ease back with a slow-fast-slow curve")
                .defineInRange("recenter_time", 0.25, 0.05, 0.6);
        CAMERA_RECENTER_ENABLED = b.comment("Enable the center recenter behavior")
                .define("recenter_enabled", true);
        CAMERA_RECENTER_DELAY = b.comment("Delay in seconds after input becomes neutral before recenter starts")
                .defineInRange("recenter_delay", 0.10, 0.0, 1.0);
        CAMERA_RECENTER_RADIUS = b.comment("Base radius of the recenter error circle in degrees")
                .defineInRange("recenter_radius", 0.35, 0.0, 1.5);
        CAMERA_RECENTER_SPEED_GAIN = b.comment("Extra recenter radius from speed (0~1)")
                .defineInRange("recenter_speed_gain", 0.30, 0.0, 1.0);
        CAMERA_RECENTER_ACTIVITY_GAIN = b.comment("Extra recenter radius from yaw/roll activity (0~1)")
                .defineInRange("recenter_activity_gain", 0.25, 0.0, 1.0);
        CAMERA_RECENTER_HYSTERESIS = b.comment("Hysteresis radius for starting/stopping recenter (degrees)")
                .defineInRange("recenter_hysteresis", 0.20, 0.0, 1.0);
        CAMERA_RECENTER_IDLE_SETTLE = b.comment("Seconds with no input before the residual error circle slowly shrinks to zero")
                .defineInRange("recenter_idle_settle", 1.5, 0.0, 5.0);
        CAMERA_EDGE_RADIUS = b.comment("Edge error-circle base radius in normalized units (1 = maximum offset)")
                .defineInRange("edge_radius", 0.03, 0.0, 0.12);
        CAMERA_EDGE_SPEED_GAIN = b.comment("Extra edge error radius from speed")
                .defineInRange("edge_speed_gain", 0.03, 0.0, 0.08);
        CAMERA_EDGE_ACTIVITY_GAIN = b.comment("Extra edge error radius from yaw/roll activity")
                .defineInRange("edge_activity_gain", 0.03, 0.0, 0.08);
        CAMERA_EDGE_BRAKE_ZONE = b.comment("Normalized edge zone where predictive braking starts")
                .defineInRange("edge_brake_zone", 0.30, 0.05, 0.60);
        CAMERA_EDGE_BRAKE = b.comment("Predictive edge braking strength")
                .defineInRange("edge_brake", 1.0, 0.0, 2.0);
        CAMERA_EDGE_LOOKAHEAD = b.comment("Predictive edge braking lookahead in seconds")
                .defineInRange("edge_lookahead", 0.10, 0.0, 0.20);
        CAMERA_EDGE_BRAKE_RELEASE = b.comment("Edge braking release/onset smoothing time in seconds")
                .defineInRange("edge_brake_release", 0.15, 0.05, 0.50);
        CAMERA_EDGE_BRAKE_HYSTERESIS = b.comment("Edge braking hysteresis in normalized radius")
                .defineInRange("edge_brake_hysteresis", 0.03, 0.0, 0.15);
        CAMERA_EDGE_SPEED_LIMIT = b.comment("How much edge braking scales down max speed: 0 = damping only, 0.75 = strong")
                .defineInRange("edge_speed_limit", 0.0, 0.0, 0.75);
        CAMERA_DISTANCE_GAIN = b.comment("Extra third-person camera distance per speed unit (0~2)")
                .defineInRange("distance_gain", 0.5, 0.0, 2.0);
        CAMERA_ADS_SCALE = b.comment("Scale of camera dynamics while aiming/zooming (0~1)")
                .defineInRange("ads_scale", 0.35, 0.0, 1.0);
        CAMERA_NOSE_MARKER = b.comment("Draw an extra marker at the projected nose/gun direction (debug/aim aid)")
                .define("nose_marker", false);
        CAMERA_REPLACE_CROSSHAIR = b.comment("Replace the third-person vehicle crosshair with the projected nose/gun reticle")
                .define("replace_crosshair", false);
        CAMERA_ROLL_RATIO = b.comment("Camera roll follow ratio: 1.0 = stock, 0.0 = camera does not roll with the aircraft, 0.7 = reduced")
                .defineInRange("roll_ratio", 1.0, 0.0, 1.5);
        CAMERA_FIRST_PERSON = b.comment("Also apply camera dynamics in first person")
                .define("first_person", false);
        b.comment("First-person / ADS zoom camera settings (independent from third-person)").push("first_person_cam");
        CAMERA_FP_LAG_YAW = b.comment("First-person yaw smoothing in seconds")
                .defineInRange("lag_yaw", 0.03, 0.0, 0.5);
        CAMERA_FP_LAG_PITCH = b.comment("First-person pitch smoothing in seconds")
                .defineInRange("lag_pitch", 0.03, 0.0, 0.5);
        CAMERA_FP_TURN_LAG = b.comment("First-person turn lag in seconds (0 = no reverse offset)")
                .defineInRange("turn_lag", 0.0, 0.0, 0.3);
        CAMERA_FP_ROLL_LAG = b.comment("First-person roll inertia in seconds")
                .defineInRange("roll_lag", 0.0, 0.0, 0.3);
        CAMERA_FP_POSITION_LAG = b.comment("First-person position spring in seconds (normally 0)")
                .defineInRange("position_lag", 0.0, 0.0, 0.2);
        CAMERA_FP_ADAPTIVE_GAIN = b.comment("First-person adaptive damping gain (0 = fixed)")
                .defineInRange("adaptive_gain", 0.0, 0.0, 3.0);
        b.pop();
        b.pop();

        DEBUG_HUD = b.comment("Show debug HUD (inputs, scheme, vehicle attitude)")
                .define("debug_hud", false);

        b.comment("Per-tick gameplay telemetry for the DSH sbw-telemetry plugin").push("telemetry");
        TELEMETRY_ENABLED = b.comment("Record flight/camera parameters to run/sbwcontrol-telemetry/*.jsonl")
                .define("enabled", false);
        TELEMETRY_INTERVAL_TICKS = b.comment("Telemetry sample interval in ticks (1 = every tick)")
                .defineInRange("interval_ticks", 1, 1, 20);
        b.pop();

        SPEC = b.build();
    }

    private ClientConfig() {
    }
}
