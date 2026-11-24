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

    /** 俯仰通道 → mouseMoveSpeedY 的缩放。 */
    public static final ForgeConfigSpec.DoubleValue PITCH_SCALE;
    /**
     * 服务端允许的最大偏航角速度（度/秒）。
     * 直升机走原版 mouseMoveSpeedX 公式反算，固定翼走直接 yRot。
     */
    public static final ForgeConfigSpec.DoubleValue MAX_YAW_RATE;
    /** 兼容旧代码/自测的别名，实际读取同一个 MAX_YAW_RATE。 */
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
        PITCH_SCALE = b.comment("pitch channel -> mouseMoveSpeedY (pitch rate = pitchSpeed * clamp(0.1~0.2 * value))")
                .defineInRange("pitch_scale", 20.0, 0.1, 64.0);
        MAX_YAW_RATE = b.comment("Maximum yaw rate in deg/s; the client only controls its sensitivity ratio within this cap")
                .defineInRange("max_yaw_rate", 90.0, 0.0, 180.0);
        EXTRA_YAW_RATE = MAX_YAW_RATE;
        b.pop();

        INPUT_TIMEOUT_TICKS = b.comment("Drop control input after this many ticks without a packet")
                .defineInRange("input_timeout_ticks", 5, 1, 100);
        LOG_APPLY = b.define("log_apply", false);

        SPEC = b.build();
    }

    private ServerConfig() {
    }
}
