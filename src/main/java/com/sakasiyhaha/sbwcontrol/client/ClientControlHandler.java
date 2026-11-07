package com.sakasiyhaha.sbwcontrol.client;

import com.atsuishio.superbwarfare.data.vehicle.subdata.EngineType;
import com.atsuishio.superbwarfare.data.vehicle.subdata.VehicleType;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.sakasiyhaha.sbwcontrol.SbwControl;
import com.sakasiyhaha.sbwcontrol.control.AxisSource;
import com.sakasiyhaha.sbwcontrol.control.InputMode;
import com.sakasiyhaha.sbwcontrol.config.ClientConfig;
import com.sakasiyhaha.sbwcontrol.control.ClientSupport;
import com.sakasiyhaha.sbwcontrol.control.ControlScheme;
import com.sakasiyhaha.sbwcontrol.control.VehicleCategory;
import com.sakasiyhaha.sbwcontrol.key.ModKeys;
import com.sakasiyhaha.sbwcontrol.network.ControlNetwork;
import com.sakasiyhaha.sbwcontrol.network.CtlPacket;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 客户端输入层：读取鼠标/按键 → 按控制方案计算俯仰/滚转/偏航三通道 → 发给服务端。
 *
 * <p>时间点：LOWEST 优先级 + ClientTickEvent.END，确保在卓越前线自己的客户端 tick
 * 处理器（NORMAL）之后执行 —— 这样 {@link ModKeys#suppressSwVehicleStrafeKeys()} 才能
 * 在本 tick 之后生效（下一 tick 卓越前线就会发送清空的 A/D 输入位）。
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = SbwControl.MODID, value = Dist.CLIENT)
public final class ClientControlHandler {

    private static final float MAX_MOUSE_DELTA = 256.0F;

    private static double lastMouseX;
    private static double lastMouseY;
    private static boolean hasMousePos;
    private static boolean active;
    private static ControlScheme lastScheme = ControlScheme.YAW_BIRD;
    private static long lastWarnTick;

    private static float pitch;
    private static float roll;
    private static float yaw;
    /** STICK 模式：鼠标累积出的虚拟摇杆位置（-1..1）。 */
    private static float stickX;
    private static float stickY;

    private ClientControlHandler() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        handleToggles(mc, player);
        if (player == null) {
            resetState();
            return;
        }

        double mouseX = mc.mouseHandler.xpos();
        double mouseY = mc.mouseHandler.ypos();
        if (!hasMousePos) {
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            hasMousePos = true;
        }
        double dx = 0.0D;
        double dy = 0.0D;
        if (mc.mouseHandler.isMouseGrabbed()) {
            dx = Mth.clamp(mouseX - lastMouseX, -MAX_MOUSE_DELTA, MAX_MOUSE_DELTA);
            dy = Mth.clamp(mouseY - lastMouseY, -MAX_MOUSE_DELTA, MAX_MOUSE_DELTA);
        }
        lastMouseX = mouseX;
        lastMouseY = mouseY;

        VehicleEntity vehicle = player.getVehicle() instanceof VehicleEntity v ? v : null;
        ControlScheme scheme = schemeFor(vehicle);
        boolean inFlyingVehicle = vehicle != null
                && isFlyingVehicle(vehicle)
                && vehicle.getFirstPassenger() == player;
        // 盘旋自动驾驶 / 卓越前线自由视角（C）/ TOM6 特殊引擎 / 残骸：不接管，交还原版行为
        boolean specialMode = vehicle != null && (vehicle.getLoiterActive()
                || vehicle.isWreck()
                || vehicle.computed().getEngineType() == EngineType.TOM6);
        boolean freeCam = vehicle != null && ClientEventHandler.isFreeCam(player);
        boolean shouldApply = inFlyingVehicle
                && !specialMode
                && !freeCam
                && !scheme.isStock()
                && ClientSupport.isSupported()
                && ClientSupport.isAllowed(scheme);

        if (inFlyingVehicle && !scheme.isStock()
                && (!ClientSupport.isSupported() || !ClientSupport.isAllowed(scheme))) {
            long tick = player.tickCount;
            if (tick - lastWarnTick > 100L) {
                lastWarnTick = tick;
                player.displayClientMessage(Component.translatable("sbwcontrol.message.server.unsupported"), true);
            }
        }

        if (!shouldApply) {
            if (active) {
                ControlNetwork.sendToServer(new CtlPacket(0.0F, 0.0F, 0.0F, scheme, false));
                active = false;
                lastScheme = scheme;
            }
            pitch = 0.0F;
            roll = 0.0F;
            yaw = 0.0F;
            stickX = 0.0F;
            stickY = 0.0F;
            return;
        }

        // 压制卓越前线自带 A/D 的滚转语义（LOWEST 保证在它自己的 tick 处理器之后执行）
        ModKeys.suppressSwVehicleStrafeKeys();

        if (!active) {
            player.displayClientMessage(Component.translatable("sbwcontrol.message.active",
                    Component.translatable("screen.sbwcontrol.scheme." + scheme.name())), true);
        }

        double sensitivity = ClientConfig.MOUSE_SENSITIVITY.get() / 100.0D;
        double pxToUnit = 0.02D * sensitivity;
        // 这是 tick 级逻辑（20 tick/s），固定用 0.05s；用帧时间会让回中/平滑比配置慢约 3 倍
        double dt = 0.05D;
        boolean stickMode = ClientConfig.INPUT_MODE.get() == InputMode.STICK;
        if (stickMode) {
            double stickGain = ClientConfig.STICK_GAIN.get();
            double centerTime = ClientConfig.STICK_CENTER_TIME.get();
            stickX = advanceStick(stickX, dx * pxToUnit * stickGain, centerTime, dt);
            stickY = advanceStick(stickY, dy * pxToUnit * stickGain, centerTime, dt);
        }

        float rawPitch;
        float rawRoll;
        float rawYaw;
        if (scheme == ControlScheme.CUSTOM) {
            rawPitch = resolveAxis(ClientConfig.CUSTOM_PITCH_SOURCE.get(), dx, dy, pxToUnit, stickMode,
                    ModKeys.PITCH_UP, ModKeys.PITCH_DOWN);
            rawRoll = resolveAxis(ClientConfig.CUSTOM_ROLL_SOURCE.get(), dx, dy, pxToUnit, stickMode,
                    ModKeys.ROLL_LEFT, ModKeys.ROLL_RIGHT);
            rawYaw = resolveAxis(ClientConfig.CUSTOM_YAW_SOURCE.get(), dx, dy, pxToUnit, stickMode,
                    ModKeys.YAW_LEFT, ModKeys.YAW_RIGHT);
        } else if (scheme == ControlScheme.ROLL_BIRD) {
            rawPitch = resolveAxis(AxisSource.MOUSE_Y, dx, dy, pxToUnit, stickMode,
                    ModKeys.PITCH_UP, ModKeys.PITCH_DOWN)
                    + keyAxis(ModKeys.PITCH_UP, ModKeys.PITCH_DOWN);
            rawRoll = resolveAxis(AxisSource.MOUSE_X, dx, dy, pxToUnit, stickMode,
                    ModKeys.ROLL_LEFT, ModKeys.ROLL_RIGHT)
                    + keyAxis(ModKeys.ROLL_LEFT, ModKeys.ROLL_RIGHT);
            rawYaw = keyAxis(ModKeys.YAW_LEFT, ModKeys.YAW_RIGHT);
        } else if (scheme == ControlScheme.ROLL_YAW_HYBRID) {
            rawPitch = resolveAxis(AxisSource.MOUSE_Y, dx, dy, pxToUnit, stickMode,
                    ModKeys.PITCH_UP, ModKeys.PITCH_DOWN)
                    + keyAxis(ModKeys.PITCH_UP, ModKeys.PITCH_DOWN);
            float rollAxis = resolveAxis(AxisSource.MOUSE_X, dx, dy, pxToUnit, stickMode,
                    ModKeys.ROLL_LEFT, ModKeys.ROLL_RIGHT)
                    + keyAxis(ModKeys.ROLL_LEFT, ModKeys.ROLL_RIGHT);
            rawRoll = rollAxis;
            rawYaw = keyAxis(ModKeys.YAW_LEFT, ModKeys.YAW_RIGHT)
                    + (float) (ClientConfig.HYBRID_MOUSE_YAW.get() * rollAxis);
        } else {
            rawPitch = 0.0F;
            rawRoll = 0.0F;
            rawYaw = 0.0F;
        }

        rawPitch = shape(rawPitch, ClientConfig.PITCH_GAIN.get(), ClientConfig.INVERT_PITCH.get());
        rawRoll = shape(rawRoll, ClientConfig.ROLL_GAIN.get(), ClientConfig.INVERT_ROLL.get());
        rawYaw = shape(rawYaw, ClientConfig.YAW_GAIN.get(), ClientConfig.INVERT_YAW.get());

        float alpha = smoothingAlpha();
        pitch += (rawPitch - pitch) * alpha;
        roll += (rawRoll - roll) * alpha;
        yaw += (rawYaw - yaw) * alpha;

        ControlNetwork.sendToServer(new CtlPacket(pitch, roll, yaw, scheme, true));
        active = true;
        lastScheme = scheme;
    }

    private static float resolveAxis(AxisSource source, double dx, double dy, double pxToUnit,
                                     boolean stickMode, KeyMapping negative, KeyMapping positive) {
        return switch (source) {
            case MOUSE_X -> stickMode ? stickX : mouseRoll(dx, pxToUnit);
            case MOUSE_Y -> stickMode ? stickY : mousePitch(dy, pxToUnit);
            case KEYS -> keyAxis(negative, positive);
        };
    }

    private static float advanceStick(float current, double delta, double centerTime, double dt) {
        float value = Mth.clamp((float) (current + delta), -1.0F, 1.0F);
        if (centerTime > 1.0E-4D) {
            value *= (float) Math.exp(-dt / centerTime);
        }
        return value;
    }

    private static float mouseRoll(double dx, double pxToUnit) {
        return Mth.clamp((float) (dx * pxToUnit), -1.0F, 1.0F);
    }

    private static float mousePitch(double dy, double pxToUnit) {
        // 与卓越前线原版一致：鼠标向下 = 低头（正通道）
        return Mth.clamp((float) (dy * pxToUnit), -1.0F, 1.0F);
    }

    private static float keyAxis(KeyMapping negative, KeyMapping positive) {
        int value = (positive.isDown() ? 1 : 0) - (negative.isDown() ? 1 : 0);
        return value;
    }

    private static float shape(float value, double gain, boolean invert) {
        double v = value * gain;
        if (invert) {
            v = -v;
        }
        double deadzone = ClientConfig.DEADZONE.get();
        if (Math.abs(v) < deadzone) {
            return 0.0F;
        }
        return Mth.clamp((float) v, -1.0F, 1.0F);
    }

    private static float smoothingAlpha() {
        double smoothing = ClientConfig.SMOOTHING.get();
        if (smoothing <= 1.0E-4D) {
            return 1.0F;
        }
        // tick 级平滑：每 tick 固定 0.05s
        return (float) (1.0D - Math.exp(-0.05D / smoothing));
    }

    private static ControlScheme schemeFor(VehicleEntity vehicle) {
        return switch (VehicleCategory.of(vehicle)) {
            case AIRPLANE -> ClientConfig.AIRCRAFT_SCHEME.get();
            case HELICOPTER -> ClientConfig.HELICOPTER_SCHEME.get();
            // 坦克/汽车/船等其他载具不接管，保持原版
            case OTHER -> ControlScheme.YAW_BIRD;
        };
    }

    private static boolean isFlyingVehicle(VehicleEntity vehicle) {
        return VehicleCategory.of(vehicle).isFlying();
    }

    private static void handleToggles(Minecraft mc, LocalPlayer player) {
        while (ModKeys.TOGGLE_SCHEME.consumeClick()) {
            toggleScheme(player);
        }
        while (ModKeys.TOGGLE_CAMERA.consumeClick()) {
            ClientConfig.CAMERA_ENABLED.set(!ClientConfig.CAMERA_ENABLED.get());
            ClientConfig.SPEC.save();
        }
        while (ModKeys.OPEN_CONFIG.consumeClick()) {
            mc.setScreen(new SbwConfigScreen(null));
        }
    }

    private static void toggleScheme(LocalPlayer player) {
        if (player == null) {
            return;
        }
        VehicleEntity vehicle = player.getVehicle() instanceof VehicleEntity v ? v : null;
        VehicleCategory category = VehicleCategory.of(vehicle);
        if (!category.isFlying()) {
            player.displayClientMessage(Component.translatable(vehicle == null
                    ? "sbwcontrol.message.scheme.not_riding"
                    : "sbwcontrol.message.scheme.other"), true);
            return;
        }

        ControlScheme next;
        if (category == VehicleCategory.HELICOPTER) {
            next = next(ClientConfig.HELICOPTER_SCHEME.get());
            ClientConfig.HELICOPTER_SCHEME.set(next);
        } else {
            next = next(ClientConfig.AIRCRAFT_SCHEME.get());
            ClientConfig.AIRCRAFT_SCHEME.set(next);
        }
        ClientConfig.SPEC.save();

        Component name = Component.translatable("screen.sbwcontrol.scheme." + next.name());
        Component message = Component.translatable(category == VehicleCategory.HELICOPTER
                ? "sbwcontrol.message.scheme.helicopter"
                : "sbwcontrol.message.scheme.airplane", name);
        player.displayClientMessage(message, true);
    }

    private static ControlScheme next(ControlScheme current) {
        ControlScheme[] values = ControlScheme.values();
        return values[(current.ordinal() + 1) % values.length];
    }

    private static void resetState() {
        active = false;
        pitch = 0.0F;
        roll = 0.0F;
        yaw = 0.0F;
        stickX = 0.0F;
        stickY = 0.0F;
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientSupport.reset();
        resetState();
        hasMousePos = false;
    }

    // ---- 调试用只读状态 ----

    public static boolean isActive() {
        return active;
    }

    public static ControlScheme getLastScheme() {
        return lastScheme;
    }

    public static float getPitch() {
        return pitch;
    }

    public static float getRoll() {
        return roll;
    }

    public static float getYaw() {
        return yaw;
    }

    public static float getStickX() {
        return stickX;
    }

    public static float getStickY() {
        return stickY;
    }
}

