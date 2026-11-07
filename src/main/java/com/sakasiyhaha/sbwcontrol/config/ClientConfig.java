package com.sakasiyhaha.sbwcontrol.config;

import com.sakasiyhaha.sbwcontrol.camera.CameraPreset;
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
    /** 转向偏移阈值软化：0=旧硬钳制，1=tanh 软饱和（接近阈值时自动减速）。 */
    public static final ForgeConfigSpec.DoubleValue CAMERA_SOFT_LIMIT;
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
        HYBRID_MOUSE_YAW = b.defineInRange("hybrid_mouse_yaw", 0.25, 0.0, 1.0);
        b.pop();

        b.comment("Input feel").push("input");
        INPUT_MODE = b.comment("Mouse input model: RATE (delta = rate, stock-like) or STICK (accumulated virtual stick with auto-center)")
                .defineEnum("input_mode", InputMode.STICK);
        STICK_CENTER_TIME = b.comment("STICK mode: seconds for the virtual stick to center itself (0 = never center)")
                .defineInRange("stick_center_time", 1.2, 0.0, 5.0);
        STICK_GAIN = b.comment("STICK mode: mouse movement -> stick deflection gain")
                .defineInRange("stick_gain", 1.0, 0.1, 5.0);
        MOUSE_SENSITIVITY = b.comment("Mouse sensitivity for control channels (percent)")
                .defineInRange("mouse_sensitivity", 100, 10, 300);
        SMOOTHING = b.comment("Smoothing time in seconds (0 = raw input)")
                .defineInRange("smoothing", 0.08, 0.0, 1.0);
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
        CAMERA_LAG_ROLL = b.comment("Roll camera inertia in seconds (bigger = camera trails the aircraft roll more; 0 = rigid/vanilla-like)")
                .defineInRange("lag_roll", 0.10, 0.0, 0.5);
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
        CAMERA_SOFT_LIMIT = b.comment("Soft turn-lag limit: 0 = old hard clamp, 1 = smooth tanh saturation that decelerates near the limit")
                .defineInRange("soft_limit", 1.0, 0.0, 1.0);
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
