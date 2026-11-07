package com.sakasiyhaha.sbwcontrol.server;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.sakasiyhaha.sbwcontrol.SbwControl;
import com.sakasiyhaha.sbwcontrol.config.ServerConfig;
import com.sakasiyhaha.sbwcontrol.control.ClientSupport;
import com.sakasiyhaha.sbwcontrol.control.ControlScheme;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Locale;

/**
 * 服务器管理命令（需要 OP 权限等级 2）：{@code /sbwctlserver ...}
 *
 * <p>用途：联机服务器的服主不直接改 TOML 也能调整“允许哪些控制流派 / 服务端输入增益”，
 * 修改后立即保存 serverconfig 并广播支持包，客户端无需重连。
 *
 * <pre>
 * /sbwctlserver status
 * /sbwctlserver allow ROLL_BIRD|ROLL_YAW_HYBRID|CUSTOM|YAW_BIRD true|false
 * /sbwctlserver scale yaw|roll|pitch &lt;value&gt;
 * /sbwctlserver timeout &lt;ticks&gt;
 * </pre>
 */
public final class ServerCommands {

    private ServerCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        var allowNode = Commands.literal("allow");
        for (ControlScheme scheme : ControlScheme.values()) {
            allowNode.then(Commands.literal(scheme.name())
                    .then(Commands.literal("true").executes(context -> setAllowed(context.getSource(), scheme, true)))
                    .then(Commands.literal("false").executes(context -> setAllowed(context.getSource(), scheme, false))));
        }

        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("sbwctlserver")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("status").executes(context -> status(context.getSource())))
                .then(allowNode)
                .then(Commands.literal("scale")
                        .then(Commands.literal("yaw").then(Commands.argument("value", com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg(0.1D, 64.0D))
                                .executes(context -> setScale(context.getSource(), "yaw",
                                        com.mojang.brigadier.arguments.DoubleArgumentType.getDouble(context, "value")))))
                        .then(Commands.literal("roll").then(Commands.argument("value", com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg(0.01D, 20.0D))
                                .executes(context -> setScale(context.getSource(), "roll",
                                        com.mojang.brigadier.arguments.DoubleArgumentType.getDouble(context, "value")))))
                        .then(Commands.literal("pitch").then(Commands.argument("value", com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg(0.1D, 64.0D))
                                .executes(context -> setScale(context.getSource(), "pitch",
                                        com.mojang.brigadier.arguments.DoubleArgumentType.getDouble(context, "value"))))))
                .then(Commands.literal("timeout")
                        .then(Commands.argument("ticks", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 100))
                                .executes(context -> setTimeout(context.getSource(),
                                        com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "ticks")))))
                .then(Commands.literal("yawrate")
                        .then(Commands.argument("degPerSec", com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg(0.0D, 180.0D))
                                .executes(context -> setExtraYawRate(context.getSource(),
                                        com.mojang.brigadier.arguments.DoubleArgumentType.getDouble(context, "degPerSec")))));
        event.getDispatcher().register(root);
    }

    private static int status(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("[sbwcontrol] allowed mask=0b"
                + Integer.toBinaryString(ServerControlHandler.allowedMask())
                + " schemes=" + java.util.Arrays.toString(ControlScheme.values())
                + " scales(yaw/roll/pitch)=" + ServerConfig.YAW_SCALE.get() + "/"
                + ServerConfig.ROLL_SCALE.get() + "/" + ServerConfig.PITCH_SCALE.get()
                + " extraYawRate=" + ServerConfig.EXTRA_YAW_RATE.get() + " deg/s"
                + " timeout=" + ServerConfig.INPUT_TIMEOUT_TICKS.get()
                + " activeInputs=" + ServerControlHandler.inputCount()), false);
        return 1;
    }

    private static int setAllowed(CommandSourceStack source, ControlScheme scheme, boolean allowed) {
        if (scheme == ControlScheme.YAW_BIRD) {
            source.sendFailure(Component.literal("YAW_BIRD (stock) is always allowed"));
            return 0;
        }
        switch (scheme) {
            case ROLL_BIRD -> ServerConfig.ALLOW_ROLL_BIRD.set(allowed);
            case ROLL_YAW_HYBRID -> ServerConfig.ALLOW_ROLL_YAW_HYBRID.set(allowed);
            case CUSTOM -> ServerConfig.ALLOW_CUSTOM.set(allowed);
            default -> {
            }
        }
        ServerConfig.SPEC.save();
        broadcastSupport(source.getServer());
        source.sendSuccess(() -> Component.literal("[sbwcontrol] " + scheme.name() + " allowed=" + allowed), true);
        return 1;
    }

    private static int setScale(CommandSourceStack source, String axis, double value) {
        switch (axis.toLowerCase(Locale.ROOT)) {
            case "yaw" -> ServerConfig.YAW_SCALE.set(value);
            case "roll" -> ServerConfig.ROLL_SCALE.set(value);
            case "pitch" -> ServerConfig.PITCH_SCALE.set(value);
            default -> {
                return 0;
            }
        }
        ServerConfig.SPEC.save();
        source.sendSuccess(() -> Component.literal("[sbwcontrol] " + axis + "_scale=" + value + " (saved)"), true);
        return 1;
    }

    private static int setTimeout(CommandSourceStack source, int ticks) {
        ServerConfig.INPUT_TIMEOUT_TICKS.set(ticks);
        ServerConfig.SPEC.save();
        source.sendSuccess(() -> Component.literal("[sbwcontrol] input_timeout_ticks=" + ticks + " (saved)"), true);
        return 1;
    }

    private static int setExtraYawRate(CommandSourceStack source, double degPerSec) {
        ServerConfig.EXTRA_YAW_RATE.set(degPerSec);
        ServerConfig.SPEC.save();
        source.sendSuccess(() -> Component.literal("[sbwcontrol] extra_yaw_rate=" + degPerSec + " deg/s (saved)"), true);
        return 1;
    }

    /** 修改配置后广播给所有在线玩家（客户端据此更新降级状态/允许方案）。 */
    public static void broadcastSupport(MinecraftServer server) {
        if (server == null) {
            return;
        }
        int mask = ServerControlHandler.allowedMask();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            com.sakasiyhaha.sbwcontrol.network.ControlNetwork.sendSupport(player, true, mask);
        }
    }

    /** 供 status 显示的已激活输入数量（也方便排查联机问题）。 */
    public static int inputCount() {
        return ServerControlHandler.inputCount();
    }
}
