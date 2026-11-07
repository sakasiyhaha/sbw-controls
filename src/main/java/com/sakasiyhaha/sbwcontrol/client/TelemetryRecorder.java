package com.sakasiyhaha.sbwcontrol.client;

import com.atsuishio.superbwarfare.data.vehicle.subdata.VehicleType;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.google.gson.JsonObject;
import com.sakasiyhaha.sbwcontrol.SbwControl;
import com.sakasiyhaha.sbwcontrol.config.ClientConfig;
import com.sakasiyhaha.sbwcontrol.key.ModKeys;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 每 tick 把飞行/相机/输入参数写进 {@code <gameDir>/sbwcontrol-telemetry/*.jsonl}。
 *
 * <p>文件供 DSH 的 {@code sbw-telemetry} 插件读取、统计，帮助把"手感"量化成：
 * 相机滞后、转向滞后、位置弹簧、输入/姿态角速度、FPS 等。
 * 默认关闭，游戏内用 {@code /sbwctl telemetry on|off|status} 控制，或改客户端配置。
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = SbwControl.MODID, value = Dist.CLIENT)
public final class TelemetryRecorder {

    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private static BufferedWriter writer;
    private static Path filePath;
    private static boolean running;
    private static int tickCounter;
    private static int flushCounter;
    private static int records;
    private static long startedAtMs;

    private static int lastVehicleId = Integer.MIN_VALUE;
    private static double lastVehicleRoll = Double.NaN;

    private TelemetryRecorder() {
    }

    public static boolean isRunning() {
        return running;
    }

    public static String status() {
        if (!running) {
            return "telemetry: off (use /sbwctl telemetry on)";
        }
        return String.format("telemetry: on, file=%s, records=%d, elapsed=%.1fs",
                filePath, records, (System.currentTimeMillis() - startedAtMs) / 1000.0D);
    }

    public static boolean start() {
        if (running) {
            return true;
        }
        Minecraft mc = Minecraft.getInstance();
        try {
            Path dir = mc.gameDirectory.toPath().resolve("sbwcontrol-telemetry");
            Files.createDirectories(dir);
            filePath = dir.resolve("telemetry-" + LocalDateTime.now().format(FILE_TIME) + ".jsonl");
            writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8);
            startedAtMs = System.currentTimeMillis();
            tickCounter = 0;
            flushCounter = 0;
            records = 0;
            lastVehicleId = Integer.MIN_VALUE;
            lastVehicleRoll = Double.NaN;
            running = true;
            writeHeader();
            writer.flush();
            SbwControl.LOGGER.info("[sbwcontrol] telemetry recording to {}", filePath.toAbsolutePath());
            return true;
        } catch (Throwable t) {
            running = false;
            closeQuietly();
            SbwControl.LOGGER.error("[sbwcontrol] failed to start telemetry", t);
            return false;
        }
    }

    public static void stop() {
        if (!running) {
            return;
        }
        running = false;
        try {
            if (writer != null) {
                JsonObject footer = new JsonObject();
                footer.addProperty("type", "footer");
                footer.addProperty("endedAt", System.currentTimeMillis());
                footer.addProperty("records", records);
                writer.write(footer.toString());
                writer.newLine();
                writer.flush();
                SbwControl.LOGGER.info("[sbwcontrol] telemetry stopped: {} records -> {}",
                        records, filePath == null ? "?" : filePath.toAbsolutePath());
            }
        } catch (IOException ignored) {
            // ignore
        } finally {
            closeQuietly();
        }
    }

    public static void setEnabled(boolean enabled) {
        if (enabled) {
            start();
        } else {
            stop();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!running) {
            if (ClientConfig.TELEMETRY_ENABLED.get()) {
                start();
            }
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }
        int interval = Math.max(1, ClientConfig.TELEMETRY_INTERVAL_TICKS.get());
        if (++tickCounter % interval != 0) {
            return;
        }
        try {
            writeRecord(mc, player);
            records++;
            if (++flushCounter >= 20) {
                flushCounter = 0;
                writer.flush();
            }
        } catch (Throwable t) {
            SbwControl.LOGGER.warn("[sbwcontrol] telemetry write failed, stopping", t);
            stop();
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        stop();
    }

    private static void writeHeader() throws IOException {
        JsonObject o = new JsonObject();
        o.addProperty("type", "header");
        o.addProperty("schema", 1);
        o.addProperty("mod", "sbwcontrol");
        o.addProperty("startedAt", startedAtMs);

        JsonObject cfg = new JsonObject();
        cfg.addProperty("aircraftScheme", ClientConfig.AIRCRAFT_SCHEME.get().name());
        cfg.addProperty("helicopterScheme", ClientConfig.HELICOPTER_SCHEME.get().name());
        cfg.addProperty("inputMode", ClientConfig.INPUT_MODE.get().name());
        cfg.addProperty("cameraPreset", ClientConfig.CAMERA_PRESET.get().name());
        cfg.addProperty("cameraEnabled", ClientConfig.CAMERA_ENABLED.get());
        cfg.addProperty("lagYaw", ClientConfig.CAMERA_LAG_YAW.get());
        cfg.addProperty("lagPitch", ClientConfig.CAMERA_LAG_PITCH.get());
        cfg.addProperty("lagRoll", ClientConfig.CAMERA_LAG_ROLL.get());
        cfg.addProperty("turnLagSeconds", ClientConfig.CAMERA_LEAD.get());
        cfg.addProperty("turnLagInvert", ClientConfig.CAMERA_TURN_LAG_INVERT.get());
        cfg.addProperty("positionLag", ClientConfig.CAMERA_POSITION_LAG.get());
        cfg.addProperty("adaptiveGain", ClientConfig.CAMERA_ADAPTIVE_GAIN.get());
        cfg.addProperty("springDamping", ClientConfig.CAMERA_SPRING_DAMPING.get());
        cfg.addProperty("rateSmoothing", ClientConfig.CAMERA_RATE_SMOOTHING.get());
        cfg.addProperty("softLimit", ClientConfig.CAMERA_SOFT_LIMIT.get());
        cfg.addProperty("rollRatio", ClientConfig.CAMERA_ROLL_RATIO.get());
        cfg.addProperty("centerLateral", ClientConfig.CAMERA_CENTER_LATERAL.get());
        cfg.addProperty("distanceGain", ClientConfig.CAMERA_DISTANCE_GAIN.get());
        cfg.addProperty("adsScale", ClientConfig.CAMERA_ADS_SCALE.get());
        cfg.addProperty("fovGain", ClientConfig.CAMERA_FOV_GAIN.get());
        o.add("config", cfg);

        writer.write(o.toString());
        writer.newLine();
    }

    private static void writeRecord(Minecraft mc, LocalPlayer player) throws IOException {
        JsonObject o = new JsonObject();
        o.addProperty("type", "tick");
        o.addProperty("t", System.currentTimeMillis());
        o.addProperty("tick", player.tickCount);

        double dt = Math.max(1.0E-4D, mc.getDeltaFrameTime());
        o.addProperty("dt", round(dt, 4));
        o.addProperty("fps", round(1.0D / dt, 2));
        o.addProperty("cameraType", mc.options.getCameraType().name());
        o.addProperty("firstPerson", mc.options.getCameraType() == net.minecraft.client.CameraType.FIRST_PERSON
                || ClientEventHandler.zoomVehicle);
        o.addProperty("zoom", ClientEventHandler.zoomVehicle);

        VehicleEntity vehicle = player.getVehicle() instanceof VehicleEntity v ? v : null;
        if (vehicle == null) {
            o.addProperty("riding", false);
            writer.write(o.toString());
            writer.newLine();
            return;
        }
        o.addProperty("riding", true);

        VehicleType type = vehicle.getVehicleType();
        boolean flying = type == VehicleType.AIRPLANE || type == VehicleType.HELICOPTER;
        o.addProperty("scheme", (type == VehicleType.HELICOPTER
                ? ClientConfig.HELICOPTER_SCHEME.get() : ClientConfig.AIRCRAFT_SCHEME.get()).name());
        o.addProperty("inputMode", ClientConfig.INPUT_MODE.get().name());
        o.addProperty("controlActive", ClientControlHandler.isActive());

        JsonObject ch = new JsonObject();
        ch.addProperty("pitch", round(ClientControlHandler.getPitch(), 4));
        ch.addProperty("roll", round(ClientControlHandler.getRoll(), 4));
        ch.addProperty("yaw", round(ClientControlHandler.getYaw(), 4));
        ch.addProperty("stickX", round(ClientControlHandler.getStickX(), 4));
        ch.addProperty("stickY", round(ClientControlHandler.getStickY(), 4));
        o.add("channels", ch);

        JsonObject keys = new JsonObject();
        keys.addProperty("yawL", ModKeys.YAW_LEFT.isDown());
        keys.addProperty("yawR", ModKeys.YAW_RIGHT.isDown());
        keys.addProperty("rollL", ModKeys.ROLL_LEFT.isDown());
        keys.addProperty("rollR", ModKeys.ROLL_RIGHT.isDown());
        o.add("keys", keys);

        JsonObject veh = new JsonObject();
        veh.addProperty("type", type.name());
        veh.addProperty("id", vehicle.getId());
        double yaw = vehicle.getYaw(1.0F);
        double pitch = vehicle.getPitch(1.0F);
        double roll = vehicle.getRoll(1.0F);
        veh.addProperty("yaw", round(yaw, 3));
        veh.addProperty("pitch", round(pitch, 3));
        veh.addProperty("roll", round(roll, 3));
        veh.addProperty("yawRate", round(Mth.wrapDegrees(vehicle.getYRot() - vehicle.yRotO) * 20.0D, 2));
        veh.addProperty("pitchRate", round((vehicle.getXRot() - vehicle.xRotO) * 20.0D, 2));
        if (vehicle.getId() != lastVehicleId || Double.isNaN(lastVehicleRoll)) {
            lastVehicleId = vehicle.getId();
            lastVehicleRoll = roll;
        }
        double rollRate = Mth.wrapDegrees(roll - lastVehicleRoll) * 20.0D;
        lastVehicleRoll = roll;
        veh.addProperty("rollRate", round(rollRate, 2));
        veh.addProperty("speed", round(vehicle.getDeltaMovement().length(), 3));
        veh.addProperty("onGround", vehicle.onGround());
        veh.addProperty("x", round(vehicle.getX(), 3));
        veh.addProperty("y", round(vehicle.getY(), 3));
        veh.addProperty("z", round(vehicle.getZ(), 3));
        o.add("vehicle", veh);

        if (flying && CameraHandler.isApplied()) {
            JsonObject cam = new JsonObject();
            cam.addProperty("yaw", round(CameraHandler.getLastCamYaw(), 3));
            cam.addProperty("pitch", round(CameraHandler.getLastCamPitch(), 3));
            cam.addProperty("eventRoll", round(CameraHandler.getLastCamRoll(), 3));
            cam.addProperty("stockRoll", round(CameraHandler.getLastStockRoll(), 3));
            cam.addProperty("offsetYaw", round(CameraHandler.getOffsetYaw(), 3));
            cam.addProperty("offsetPitch", round(CameraHandler.getOffsetPitch(), 3));
            cam.addProperty("extraDistance", round(CameraHandler.getExtraDistance(), 3));
            cam.addProperty("smoothedYawRate", round(CameraHandler.getSmoothedYawRate(), 2));
            cam.addProperty("smoothedPitchRate", round(CameraHandler.getSmoothedPitchRate(), 2));
            cam.addProperty("smoothedRollRate", round(CameraHandler.getSmoothedRollRate(), 2));
            cam.addProperty("activity", round(CameraHandler.getActivity(), 4));
            cam.addProperty("agility", round(CameraHandler.getAgility(), 4));
            var pos = mc.gameRenderer.getMainCamera().getPosition();
            cam.addProperty("x", round(pos.x, 3));
            cam.addProperty("y", round(pos.y, 3));
            cam.addProperty("z", round(pos.z, 3));
            o.add("camera", cam);
        }

        JsonObject params = new JsonObject();
        params.addProperty("turnLag", ClientConfig.CAMERA_LEAD.get());
        params.addProperty("positionLag", ClientConfig.CAMERA_POSITION_LAG.get());
        params.addProperty("adaptiveGain", ClientConfig.CAMERA_ADAPTIVE_GAIN.get());
        params.addProperty("springDamping", ClientConfig.CAMERA_SPRING_DAMPING.get());
        params.addProperty("rateSmoothing", ClientConfig.CAMERA_RATE_SMOOTHING.get());
        params.addProperty("softLimit", ClientConfig.CAMERA_SOFT_LIMIT.get());
        params.addProperty("lagYaw", ClientConfig.CAMERA_LAG_YAW.get());
        params.addProperty("lagPitch", ClientConfig.CAMERA_LAG_PITCH.get());
        params.addProperty("lagRoll", ClientConfig.CAMERA_LAG_ROLL.get());
        params.addProperty("rollRatio", ClientConfig.CAMERA_ROLL_RATIO.get());
        params.addProperty("centerLateral", ClientConfig.CAMERA_CENTER_LATERAL.get());
        params.addProperty("distanceGain", ClientConfig.CAMERA_DISTANCE_GAIN.get());
        params.addProperty("adsScale", ClientConfig.CAMERA_ADS_SCALE.get());
        o.add("params", params);

        writer.write(o.toString());
        writer.newLine();
    }

    private static double round(double value, int decimals) {
        double factor = Math.pow(10.0D, decimals);
        return Math.round(value * factor) / factor;
    }

    private static void closeQuietly() {
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException ignored) {
                // ignore
            }
            writer = null;
        }
    }
}
