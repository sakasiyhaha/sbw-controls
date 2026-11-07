package com.sakasiyhaha.sbwcontrol.client;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.sakasiyhaha.sbwcontrol.SbwControl;
import com.sakasiyhaha.sbwcontrol.camera.CameraPreset;
import com.sakasiyhaha.sbwcontrol.config.ClientConfig;
import com.sakasiyhaha.sbwcontrol.config.ServerConfig;
import com.sakasiyhaha.sbwcontrol.control.ClientSupport;
import com.sakasiyhaha.sbwcontrol.control.ControlScheme;
import com.sakasiyhaha.sbwcontrol.key.ModKeys;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 客户端调试命令 {@code /sbwctl}：
 * <ul>
 *   <li>{@code /sbwctl status} 打印当前方案、服务端支持、相机与载具状态</li>
 *   <li>{@code /sbwctl scheme aircraft|helicopter &lt;YAW_BIRD|ROLL_BIRD|ROLL_YAW_HYBRID|CUSTOM&gt;}</li>
 *   <li>{@code /sbwctl camera on|off}</li>
 *   <li>{@code /sbwctl debug on|off}</li>
 * </ul>
 * 方便实机测试时快速切换，不用翻设置界面。
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = SbwControl.MODID, value = Dist.CLIENT)
public final class ClientCommands {

    private ClientCommands() {
    }

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        var aircraftNode = Commands.literal("aircraft");
        addSchemeArgs(aircraftNode, true);
        var helicopterNode = Commands.literal("helicopter");
        addSchemeArgs(helicopterNode, false);
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("sbwctl")
                .then(Commands.literal("status").executes(context -> status(context.getSource())))
                .then(Commands.literal("selftest").executes(context -> selftest(context.getSource())))
                .then(Commands.literal("scheme").then(aircraftNode).then(helicopterNode))
                .then(Commands.literal("camera")
                        .then(Commands.literal("on").executes(context -> setCamera(context.getSource(), true)))
                        .then(Commands.literal("off").executes(context -> setCamera(context.getSource(), false)))
                        .then(Commands.literal("status").executes(context -> cameraStatus(context.getSource())))
                        .then(Commands.literal("turnlag").then(Commands.argument("seconds",
                                        DoubleArgumentType.doubleArg(0.0D, 0.6D))
                                .executes(context -> setCameraDouble(context.getSource(),
                                        DoubleArgumentType.getDouble(context, "seconds"),
                                        ClientConfig.CAMERA_LEAD::set, "lead", "s"))))
                        .then(Commands.literal("ads").then(Commands.argument("scale",
                                        DoubleArgumentType.doubleArg(0.0D, 1.0D))
                                .executes(context -> setCameraDouble(context.getSource(),
                                        DoubleArgumentType.getDouble(context, "scale"),
                                        ClientConfig.CAMERA_ADS_SCALE::set, "ads_scale", ""))))
                        .then(Commands.literal("rollratio").then(Commands.argument("ratio",
                                        DoubleArgumentType.doubleArg(0.0D, 1.5D))
                                .executes(context -> setCameraDouble(context.getSource(),
                                        DoubleArgumentType.getDouble(context, "ratio"),
                                        ClientConfig.CAMERA_ROLL_RATIO::set, "roll_ratio", ""))))
                        .then(Commands.literal("rolllag").then(Commands.argument("seconds",
                                        DoubleArgumentType.doubleArg(0.0D, 0.5D))
                                .executes(context -> setCameraDouble(context.getSource(),
                                        DoubleArgumentType.getDouble(context, "seconds"),
                                        ClientConfig.CAMERA_LAG_ROLL::set, "lag_roll", "s"))))
                        .then(Commands.literal("lateral").then(Commands.argument("factor",
                                        DoubleArgumentType.doubleArg(0.0D, 1.0D))
                                .executes(context -> setCameraDouble(context.getSource(),
                                        DoubleArgumentType.getDouble(context, "factor"),
                                        ClientConfig.CAMERA_CENTER_LATERAL::set, "center_lateral", ""))))
                        .then(Commands.literal("adaptive").then(Commands.argument("gain",
                                        DoubleArgumentType.doubleArg(0.0D, 5.0D))
                                .executes(context -> setCameraDouble(context.getSource(),
                                        DoubleArgumentType.getDouble(context, "gain"),
                                        ClientConfig.CAMERA_ADAPTIVE_GAIN::set, "adaptive_gain", ""))))
                        .then(Commands.literal("delta").executes(context -> applyDeltaPreset(context.getSource())))
                        .then(Commands.literal("poslag").then(Commands.argument("seconds",
                                        DoubleArgumentType.doubleArg(0.0D, 0.3D))
                                .executes(context -> setCameraDouble(context.getSource(),
                                        DoubleArgumentType.getDouble(context, "seconds"),
                                        ClientConfig.CAMERA_POSITION_LAG::set, "position_lag", "s")))))
                .then(Commands.literal("debug")
                        .then(Commands.literal("on").executes(context -> setDebug(context.getSource(), true)))
                        .then(Commands.literal("off").executes(context -> setDebug(context.getSource(), false))))
                .then(Commands.literal("telemetry")
                        .then(Commands.literal("on").executes(context -> setTelemetry(context.getSource(), true)))
                        .then(Commands.literal("off").executes(context -> setTelemetry(context.getSource(), false)))
                        .then(Commands.literal("status").executes(context -> telemetryStatus(context.getSource()))));
        event.getDispatcher().register(root);
    }

    private static void addSchemeArgs(LiteralArgumentBuilder<CommandSourceStack> parent, boolean aircraft) {
        for (ControlScheme scheme : ControlScheme.values()) {
            parent.then(Commands.literal(scheme.name()).executes(context -> {
                if (aircraft) {
                    ClientConfig.AIRCRAFT_SCHEME.set(scheme);
                } else {
                    ClientConfig.HELICOPTER_SCHEME.set(scheme);
                }
                ClientConfig.SPEC.save();
                Component name = Component.translatable("screen.sbwcontrol.scheme." + scheme.name());
                context.getSource().sendSuccess(
                        () -> Component.translatable("sbwcontrol.message.scheme.changed", name), true);
                return 1;
            }));
        }
    }

    private static int status(CommandSourceStack source) {
        String category = ClientConfig.AIRCRAFT_SCHEME.get().name() + " / " + ClientConfig.HELICOPTER_SCHEME.get().name();
        String server = ClientSupport.isSupported()
                ? "supported mask=0b" + Integer.toBinaryString(ClientSupport.allowedMask())
                : "NOT supported (control scheme disabled)";
        source.sendSuccess(() -> Component.literal("[sbwcontrol] schemes=" + category
                + " | camera=" + ClientConfig.CAMERA_PRESET.get().name()
                + " enabled=" + ClientConfig.CAMERA_ENABLED.get()
                + " | server=" + server), false);
        source.sendSuccess(() -> Component.literal("[sbwcontrol] keys yawL=" + ModKeys.YAW_LEFT.getName()
                + " yawR=" + ModKeys.YAW_RIGHT.getName()
                + " rollL=" + ModKeys.ROLL_LEFT.getName()
                + " rollR=" + ModKeys.ROLL_RIGHT.getName()
                + " | pressed yawL=" + ModKeys.YAW_LEFT.isDown()
                + " yawR=" + ModKeys.YAW_RIGHT.isDown()), false);
        if (source.getEntity() instanceof net.minecraft.client.player.LocalPlayer player
                && player.getVehicle() instanceof VehicleEntity vehicle) {
            source.sendSuccess(() -> Component.literal(String.format(
                    "[sbwcontrol] vehicle=%s yaw=%.1f pitch=%.1f roll=%.1f speed=%.2f",
                    vehicle.getVehicleType(), vehicle.getYaw(1.0F), vehicle.getPitch(1.0F),
                    vehicle.getRoll(1.0F), vehicle.getDeltaMovement().length())), false);
        }
        return 1;
    }

    private static int selftest(CommandSourceStack source) {
        boolean ok = true;
        ok = report(source, "client config loaded", ClientConfig.SPEC.isLoaded()) && ok;
        boolean serverSpecLoaded = ServerConfig.SPEC.isLoaded();
        source.sendSuccess(() -> Component.literal("[sbwctl][selftest] serverconfig loaded in this JVM="
                + serverSpecLoaded + " (singleplayer world only)"), false);
        ok = report(source, "key mappings registered",
                ModKeys.YAW_LEFT != null && ModKeys.YAW_RIGHT != null) && ok;
        Minecraft mc = Minecraft.getInstance();
        boolean listedInControls = false;
        if (mc.options != null && mc.options.keyMappings != null) {
            for (KeyMapping mapping : mc.options.keyMappings) {
                if (mapping == ModKeys.YAW_LEFT) {
                    listedInControls = true;
                    break;
                }
            }
        }
        ok = report(source, "key mappings listed in Controls screen", listedInControls) && ok;
        boolean supported = ClientSupport.isSupported();
        ok = report(source, "server support (world loaded)", supported) && ok;
        if (supported) {
            ok = report(source, "selected aircraft scheme allowed",
                    ClientSupport.isAllowed(ClientConfig.AIRCRAFT_SCHEME.get())) && ok;
        }
        if (source.getEntity() instanceof LocalPlayer player && player.getVehicle() instanceof VehicleEntity vehicle) {
            boolean active = ClientControlHandler.isActive();
            source.sendSuccess(() -> Component.literal(String.format(
                    "[sbwctl][selftest] vehicle=%s active=%s ch yaw=%.2f pitch=%.2f roll=%.2f | keys yawL=%s yawR=%s",
                    vehicle.getVehicleType(), active,
                    ClientControlHandler.getYaw(), ClientControlHandler.getPitch(), ClientControlHandler.getRoll(),
                    ModKeys.YAW_LEFT.isDown(), ModKeys.YAW_RIGHT.isDown())), false);
            ok = report(source, "control active", active) && ok;
        } else {
            source.sendSuccess(() -> Component.literal(
                    "[sbwctl][selftest] not riding an aircraft/helicopter; in-vehicle checks skipped"), false);
        }
        final boolean result = ok;
        source.sendSuccess(() -> Component.literal("[sbwctl][selftest] " + (result ? "PASS" : "FAIL")), false);
        return 1;
    }

    private static boolean report(CommandSourceStack source, String what, boolean ok) {
        source.sendSuccess(() -> Component.literal("[sbwctl][selftest] " + (ok ? "OK  " : "FAIL") + " " + what), false);
        return ok;
    }

    private static int applyDeltaPreset(CommandSourceStack source) {
        ClientConfig.CAMERA_PRESET.set(com.sakasiyhaha.sbwcontrol.camera.CameraPreset.DELTA);
        ClientConfig.CAMERA_LAG_YAW.set(0.08D);
        ClientConfig.CAMERA_LAG_PITCH.set(0.06D);
        ClientConfig.CAMERA_LEAD.set(0.12D);
        ClientConfig.CAMERA_LAG_ROLL.set(0.10D);
        ClientConfig.CAMERA_POSITION_LAG.set(0.07D);
        ClientConfig.CAMERA_DISTANCE_GAIN.set(0.4D);
        ClientConfig.CAMERA_ROLL_RATIO.set(1.0D);
        ClientConfig.CAMERA_ADS_SCALE.set(1.0D);
        ClientConfig.CAMERA_FOV_GAIN.set(0.1D);
        ClientConfig.CAMERA_CENTER_LATERAL.set(1.0D);
        ClientConfig.CAMERA_ADAPTIVE_GAIN.set(2.0D);
        ClientConfig.CAMERA_TURN_LAG_INVERT.set(false);
        ClientConfig.SPEC.save();
        source.sendSuccess(() -> Component.literal(
                "[sbwctl] Delta-style camera applied: preset=DELTA turnLag=0.12s posLag=0.07s rollLag=0.10s adaptive=2.0 distance=0.4 fov=0.1; "
                        + "tune with /sbwctl camera status"), true);
        return 1;
    }

    private static int cameraStatus(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal(String.format(
                "[sbwctl] camera preset=%s enabled=%s turnLag=%.2fs ads=%.2f rollRatio=%.2f rollLag=%.2fs lateral=%.0f%% posLag=%.2fs adaptive=%.2f lagYaw=%.2fs lagPitch=%.2fs",
                ClientConfig.CAMERA_PRESET.get(), ClientConfig.CAMERA_ENABLED.get(),
                ClientConfig.CAMERA_LEAD.get(), ClientConfig.CAMERA_ADS_SCALE.get(),
                ClientConfig.CAMERA_ROLL_RATIO.get(), ClientConfig.CAMERA_LAG_ROLL.get(),
                ClientConfig.CAMERA_CENTER_LATERAL.get() * 100.0D,
                ClientConfig.CAMERA_POSITION_LAG.get(), ClientConfig.CAMERA_ADAPTIVE_GAIN.get(),
                ClientConfig.CAMERA_LAG_YAW.get(), ClientConfig.CAMERA_LAG_PITCH.get())), false);
        return 1;
    }

    private static int setCameraDouble(CommandSourceStack source, double value,
                                       java.util.function.Consumer<Double> setter,
                                       String key, String unit) {
        setter.accept(value);
        ClientConfig.SPEC.save();
        source.sendSuccess(() -> Component.literal("[sbwctl] " + key + " = " + value + unit), true);
        return 1;
    }

    private static int setCamera(CommandSourceStack source, boolean enabled) {
        ClientConfig.CAMERA_ENABLED.set(enabled);
        ClientConfig.SPEC.save();
        source.sendSuccess(() -> Component.translatable("sbwcontrol.message.camera.toggled",
                Component.translatable(enabled ? "options.on" : "options.off")), true);
        return 1;
    }

    private static int setDebug(CommandSourceStack source, boolean enabled) {
        ClientConfig.DEBUG_HUD.set(enabled);
        ClientConfig.SPEC.save();
        source.sendSuccess(() -> Component.literal("debug HUD: " + (enabled ? "on" : "off")), true);
        return 1;
    }

    private static int setTelemetry(CommandSourceStack source, boolean enabled) {
        ClientConfig.TELEMETRY_ENABLED.set(enabled);
        ClientConfig.SPEC.save();
        TelemetryRecorder.setEnabled(enabled);
        source.sendSuccess(() -> Component.literal("[sbwctl] " + TelemetryRecorder.status()), true);
        return 1;
    }

    private static int telemetryStatus(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("[sbwctl] " + TelemetryRecorder.status()), false);
        return 1;
    }
}
