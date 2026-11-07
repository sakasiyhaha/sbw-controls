package com.sakasiyhaha.sbwcontrol.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * 服务端配置：是否允许各控制流派、服务端应用增益、输入超时。
 *
 * <p>服务端拥有最终裁决权：客户端选择的方案若不在允许列表内，服务端会忽略控制包并保持原版行为；
 * 客户端会收到 {@code SupportPacket} 得知哪些方案可用（HUD 会提示降级）。
 */
public final class ServerConfig {

    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue ALLOW_ROLL_BIRD;
    public static final ForgeConfigSpec.BooleanValue ALLOW_ROLL_YAW_HYBRID;
    public static final ForgeConfigSpec.BooleanValue ALLOW_CUSTOM;

    public static final ForgeConfigSpec.BooleanValue APPLY_AIRPLANE;
    public static final ForgeConfigSpec.BooleanValue APPLY_HELICOPTER;

    /** 偏航通道 → 卓越前线 mouseMoveSpeedX 的缩放（服务端调，客户端不用管）。 */
    public static final ForgeConfigSpec.DoubleValue YAW_SCALE;
    /** 滚转通道 → deltaRot 的缩放。 */
    public static final ForgeConfigSpec.DoubleValue ROLL_SCALE;
    /** 俯仰通道 → mouseMoveSpeedY 的缩放。 */
    public static final ForgeConfigSpec.DoubleValue PITCH_SCALE;
    /**
     * 偏航键附加的直接偏航角速度（度/秒）。
     * 卓越前线引擎里鼠标 X 的偏航项被 rotSpeed 钳制（平飞时约 0.3°/tick ≈ 6°/s），
     * A/D 偏航会显得几乎没反应；这里额外直接改 yRot，保证 A/D 有明确的偏航效果。
     */
    public static final ForgeConfigSpec.DoubleValue EXTRA_YAW_RATE;

    /** 超过多少 tick 没收到控制包就不再应用，回退原版。 */
    public static final ForgeConfigSpec.IntValue INPUT_TIMEOUT_TICKS;

    /** 调试用：打印每个被应用的控制状态（仅建议测试时打开）。 */
    public static final ForgeConfigSpec.BooleanValue LOG_APPLY;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.comment("Which control schemes the server allows").push("allowed");
        ALLOW_ROLL_BIRD = b.define("roll_bird", true);
        ALLOW_ROLL_YAW_HYBRID = b.define("roll_yaw_hybrid", true);
        ALLOW_CUSTOM = b.define("custom", true);
        b.pop();

        b.comment("Which vehicle types the server applies the custom control channels to").push("apply");
        APPLY_AIRPLANE = b.define("airplane", true);
        APPLY_HELICOPTER = b.define("helicopter", true);
        b.pop();

        b.comment("Server-side input scaling (tune when flight feel is too weak/strong)").push("scaling");
        // 默认值按卓越前线 aircraftEngine/helicopterEngine 的公式估算（所有载具的 Pitch/Yaw/RollSpeed 基本为 1，
        // 即通道 1.0 ≈ 每 tick 1 度左右的角速度），目标是：俯仰满杆约 40°/s、滚转满杆约 120°/s、偏航按键约 10~60°/s
        // （偏航受引擎 rotSpeed 钳制，实际随滚转角度变化）。实机试飞后可直接在 serverconfig 里改。
        YAW_SCALE = b.comment("yaw channel -> mouseMoveSpeedX (engine clamps by rotSpeed; larger may not help at level flight)")
                .defineInRange("yaw_scale", 4.0, 0.1, 64.0);
        ROLL_SCALE = b.comment("roll channel -> deltaRot (roll rate = rollSpeed * deltaRot, rollSpeed≈1)")
                .defineInRange("roll_scale", 6.0, 0.01, 20.0);
        PITCH_SCALE = b.comment("pitch channel -> mouseMoveSpeedY (pitch rate = pitchSpeed * clamp(0.1~0.2 * value))")
                .defineInRange("pitch_scale", 20.0, 0.1, 64.0);
        EXTRA_YAW_RATE = b.comment("extra direct yaw rate for A/D keys in deg/s (engine yaw via mouseMoveSpeedX is clamped to ~6 deg/s in level flight)")
                .defineInRange("extra_yaw_rate", 30.0, 0.0, 180.0);
        b.pop();

        INPUT_TIMEOUT_TICKS = b.comment("Drop control input after this many ticks without a packet")
                .defineInRange("input_timeout_ticks", 5, 1, 100);
        LOG_APPLY = b.define("log_apply", false);

        SPEC = b.build();
    }

    private ServerConfig() {
    }
}
