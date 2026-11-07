package com.sakasiyhaha.sbwcontrol.client;

import com.atsuishio.superbwarfare.data.vehicle.VehicleData;
import com.atsuishio.superbwarfare.data.vehicle.subdata.SeatInfo;
import com.atsuishio.superbwarfare.data.vehicle.subdata.VehicleType;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.sakasiyhaha.sbwcontrol.SbwControl;
import com.sakasiyhaha.sbwcontrol.camera.CameraPreset;
import com.sakasiyhaha.sbwcontrol.config.ClientConfig;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 第三人称动态瞄准相机（纯客户端、无 Mixin）。
 *
 * <p>时机：Forge 在卓越前线设置完载具相机之后才触发 {@code ComputeCameraAngles} 并把事件里的
 * yaw/pitch/roll 应用到相机，因此这里可以直接接管最终相机角度：
 * <ul>
 *   <li>滞后：相机角度向载具当前姿态做指数追赶（时间常数可配），转弯时产生“甩”的感觉。</li>
 *   <li>前导：按载具角速度给一个很小的反向偏移，让相机略微“提前”进入转向。</li>
 *   <li>速度 FOV：在 ComputeFov 里按速度放大 FOV（在卓越前线自己的 FOV 处理之前执行）。</li>
 *   <li>动态距离：第三人称按速度把相机往后拉一点（Camera.setPosition 反射调用）。</li>
 * </ul>
 * 仅在“使用卓越前线飞机相机的座位”（useAircraftCamera）上生效，避免干扰坦克等载具。
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = SbwControl.MODID, value = Dist.CLIENT)
public final class CameraHandler {

    private static int trackedVehicleId = -1;
    private static double prevYaw;
    private static double prevPitch;
    private static double offsetYaw;
    private static double offsetPitch;
    private static double extraDistance;
    /** 平滑后的相机滚转（总滚转，含 SW 自身的 pose 滚转）。 */
    private static double smoothedRoll = Double.NaN;
    /** 平滑后的角速度（度/秒），从 tick 级角度差计算，避免帧率抖动。 */
    private static double smoothedYawRate;
    private static double smoothedPitchRate;
    private static double smoothedRollRate;
    private static double prevVehicleRoll = Double.NaN;
    /** 供遥测/调试读取：最近一帧实际应用的相机角度、滚转、活动度。 */
    private static double lastCamYaw;
    private static double lastCamPitch;
    private static double lastCamRoll;
    private static double lastStockRoll;
    private static double lastAgility = 1.0D;
    private static double lastActivity;
    private static boolean lastApplied;

    /** 非线性（临界阻尼弹簧）平滑状态：速度会跨帧保留，避免每帧直接跳到目标造成"生硬帧感"。 */
    private static final double[] OFFSET_YAW_VEL = new double[1];
    private static final double[] OFFSET_PITCH_VEL = new double[1];
    private static final double[] ROLL_VEL = new double[1];
    private static final double[] REL_X_VEL = new double[1];
    private static final double[] REL_Y_VEL = new double[1];
    private static final double[] REL_Z_VEL = new double[1];
    /** 上一帧最终相机 yaw：SW 的 pose 滚转公式用的是上一帧相机 yaw（含我们的偏移）。 */
    private static double lastFinalCameraYaw = Double.NaN;
    /** 平滑后的“相机相对载具中心”向量，用于位置弹簧滞后。 */
    private static Vec3 smoothedRel;

    private CameraHandler() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || !ClientConfig.CAMERA_ENABLED.get()) {
            resetTrack();
            return;
        }
        if (!(player.getVehicle() instanceof VehicleEntity vehicle)) {
            resetTrack();
            return;
        }
        CameraPreset preset = ClientConfig.CAMERA_PRESET.get();
        if (preset == CameraPreset.VANILLA) {
            return;
        }
        VehicleType type = vehicle.getVehicleType();
        if (type != VehicleType.AIRPLANE && type != VehicleType.HELICOPTER) {
            resetTrack();
            return;
        }
        boolean zoomView = ClientEventHandler.zoomVehicle;
        boolean firstPersonView = mc.options.getCameraType() == CameraType.FIRST_PERSON || zoomView;
        if (firstPersonView && !ClientConfig.CAMERA_FIRST_PERSON.get()) {
            resetTrack();
            return;
        }
        boolean fp = firstPersonView;
        int seat = vehicle.getSeatIndex(player);
        if (!vehicle.useAircraftCamera(seat)) {
            resetTrack();
            return;
        }

        float partial = (float) event.getPartialTick();
        double vehicleYaw = vehicle.getYaw(partial);
        double vehiclePitch = vehicle.getPitch(partial);
        if (vehicle.getId() != trackedVehicleId) {
            trackedVehicleId = vehicle.getId();
            prevYaw = vehicleYaw;
            prevPitch = vehiclePitch;
            offsetYaw = 0.0D;
            offsetPitch = 0.0D;
            // 换载具时滚转目标可能从任意角度开始，避免用上一架载具的连续角做参照。
            smoothedRoll = Double.NaN;
            ROLL_VEL[0] = 0.0D;
            lastStockRoll = 0.0D;
        }
        double deltaYaw = Mth.wrapDegrees(vehicleYaw - prevYaw);
        double deltaPitch = vehiclePitch - prevPitch;
        prevYaw = vehicleYaw;
        prevPitch = vehiclePitch;

        double dt = Math.max(1.0E-3D, mc.getDeltaFrameTime());
        // 第一人称（含右键瞄准的第一人称放大）使用独立参数，不套用第三人称动态设置。
        double lagYaw = fp ? ClientConfig.CAMERA_FP_LAG_YAW.get()
                : ClientConfig.CAMERA_LAG_YAW.get() * preset.lagYawScale;
        double lagPitch = fp ? ClientConfig.CAMERA_FP_LAG_PITCH.get()
                : ClientConfig.CAMERA_LAG_PITCH.get() * preset.lagPitchScale;
        // 用 tick 级角速度（每 tick 角度差 × 20）而不是帧间插值差 / dt：
        // 后者在帧率波动或网络同步更新时会产生尖峰，导致相机（和飞机观感）抖动。
        double rawYawRate = Mth.wrapDegrees(vehicle.getYRot() - vehicle.yRotO) * 20.0D;
        double rawPitchRate = (vehicle.getXRot() - vehicle.xRotO) * 20.0D;
        double rateSmoothing = fp ? Math.min(0.10D, ClientConfig.CAMERA_RATE_SMOOTHING.get())
                : ClientConfig.CAMERA_RATE_SMOOTHING.get();
        smoothedYawRate += (rawYawRate - smoothedYawRate) * alpha(dt, rateSmoothing);
        smoothedPitchRate += (rawPitchRate - smoothedPitchRate) * alpha(dt, rateSmoothing);
        // 翻滚角速度（度/秒）：用于把“偏航/翻滚速度”也纳入相机阻尼的参考。
        double vehicleRoll = vehicle.getRoll(partial);
        if (Double.isNaN(prevVehicleRoll)) {
            prevVehicleRoll = vehicleRoll;
        }
        double rawRollRate = Mth.wrapDegrees(vehicleRoll - prevVehicleRoll) / dt;
        prevVehicleRoll = vehicleRoll;
        smoothedRollRate += (rawRollRate - smoothedRollRate) * alpha(dt, rateSmoothing);

        // 自适应阻尼：机动越剧烈（偏航/翻滚角速度越大），相机滞后时间越短，跟随越跟手。
        double adaptiveGain = fp ? ClientConfig.CAMERA_FP_ADAPTIVE_GAIN.get()
                : ClientConfig.CAMERA_ADAPTIVE_GAIN.get();
        double activity = Mth.clamp((Math.abs(smoothedYawRate) + Math.abs(smoothedRollRate)) / 120.0D,
                0.0D, 1.0D);
        double agility = 1.0D + adaptiveGain * activity;
        lastAgility = agility;
        lastActivity = activity;
        lagYaw /= agility;
        lagPitch /= agility;
        // lead 是“转向滞后秒数”：相机偏移量 = 角速度(°/s) × 滞后秒数。
        double lagSeconds = fp ? ClientConfig.CAMERA_FP_TURN_LAG.get()
                : ClientConfig.CAMERA_LEAD.get() * preset.leadScale;
        double turnSign = ClientConfig.CAMERA_TURN_LAG_INVERT.get() ? 1.0D : -1.0D;
        double turnSignPitch = ClientConfig.CAMERA_TURN_LAG_INVERT_PITCH.get() ? 1.0D : -1.0D;
        double softness = fp ? 0.0D : ClientConfig.CAMERA_SOFT_LIMIT.get();
        double targetYaw = softLimit(turnSign * smoothedYawRate * lagSeconds, 12.0D, softness);
        double targetPitch = softLimit(turnSignPitch * smoothedPitchRate * lagSeconds, 8.0D, softness);
        // 右键瞄准（第一人称放大）不再按第三人称 ADS 比例缩放；第一人称参数已独立。
        double adsScale = fp ? 1.0D
                : (ClientEventHandler.zoomVehicle ? ClientConfig.CAMERA_ADS_SCALE.get() : 1.0D);
        targetYaw *= adsScale;
        targetPitch *= adsScale;
        double springDamping = fp ? 1.0D : ClientConfig.CAMERA_SPRING_DAMPING.get();
        offsetYaw = springStep(offsetYaw, targetYaw, OFFSET_YAW_VEL, lagYaw, springDamping, 180.0D, dt);
        offsetPitch = springStep(offsetPitch, targetPitch, OFFSET_PITCH_VEL, lagPitch, springDamping, 120.0D, dt);
        // 以 SW/原版刚刚设置好的角度为基准叠加偏移，而不是重新覆盖成载具姿态：
        // 这样右键瞄准（SW 用炮口/视向量算相机）时不会被我们打断。
        double baseYaw = event.getYaw();
        double basePitch = event.getPitch();
        double finalYaw = baseYaw + offsetYaw;
        double finalPitch = basePitch + offsetPitch;
        event.setYaw((float) finalYaw);
        event.setPitch((float) finalPitch);
        lastCamYaw = finalYaw;
        lastCamPitch = finalPitch;
        lastApplied = true;

        // 相机滚转：第一人称/瞄准放大保持原版（或 FP 专用小参数），不套用第三人称滚转惯性。
        if (!fp) {
            // 原始公式在机体 roll 越过 ±180° 时会跳变 360°；先把它展开成连续角，
            // 否则弹簧会追一个突变目标，镜头完全倒置后无法平滑回到正位。
            double rawStockRoll = stockCameraRoll(vehicle, seat, partial,
                    Double.isNaN(lastFinalCameraYaw) ? baseYaw : lastFinalCameraYaw);
            double stockRoll = unwrapNear(rawStockRoll, lastStockRoll);
            double rollRatio = ClientConfig.CAMERA_ROLL_RATIO.get();
            double targetRoll = unwrapNear(stockRoll * rollRatio, smoothedRoll);
            if (Double.isNaN(smoothedRoll)) {
                smoothedRoll = targetRoll;
                ROLL_VEL[0] = 0.0D;
            } else {
                double rollLag = ClientConfig.CAMERA_LAG_ROLL.get() / agility;
                smoothedRoll = springStep(smoothedRoll, targetRoll, ROLL_VEL, rollLag, springDamping, 1080.0D, dt);
            }
            // 事件里的 roll 是绝对角度，等价角用 wrap 收回到 [-180,180]，避免叠加出 360°。
            double cameraRollOffset = Mth.wrapDegrees(smoothedRoll - stockRoll);
            event.setRoll((float) cameraRollOffset);
            lastCamRoll = cameraRollOffset;
            lastStockRoll = stockRoll;
        } else {
            event.setRoll(0.0F);
            lastCamRoll = 0.0D;
            lastStockRoll = 0.0D;
            smoothedRoll = Double.NaN;
        }
        lastFinalCameraYaw = finalYaw;

        // 第三人称位置修正：横向居中 + 按速度后拉 + 位置弹簧滞后。
        // 位置弹簧滞后：把“相机相对载具中心”的向量做时间平滑，载具转弯/滚转时相机机位会
        // 绕机身滞后摆动（不是只转视角），观感更接近战地/三角洲的弹簧臂相机。
        double distanceGain = ClientConfig.CAMERA_DISTANCE_GAIN.get() * preset.distanceScale;
        double centerFactor = ClientConfig.CAMERA_CENTER_LATERAL.get();
        double positionLag = fp ? 0.0D : ClientConfig.CAMERA_POSITION_LAG.get();
        extraDistance = 0.0D;
        if (!fp) {
            Camera camera = mc.gameRenderer.getMainCamera();
            Vec3 position = camera.getPosition();
            double desiredX = position.x;
            double desiredY = position.y;
            double desiredZ = position.z;
            boolean moved = false;

            org.joml.Matrix4d transform = null;
            org.joml.Vector4d origin = null;
            try {
                transform = vehicle.getClientVehicleTransform(partial);
                origin = transform.transform(new org.joml.Vector4d(0.0D, 0.0D, 0.0D, 1.0D));
            } catch (Throwable ignored) {
                transform = null;
                origin = null;
            }

            if (centerFactor > 0.0D && transform != null) {
                // 用载具变换把相机位置换算到机体局部坐标，只把局部 x（横向偏移）按比例拉回 0。
                // 这样压坡度/俯冲时都不会把垂直或前后分量误当横向，避免画面滑动/抖动。
                try {
                    org.joml.Matrix4d inverse = new org.joml.Matrix4d(transform).invert();
                    org.joml.Vector4d local = inverse.transform(new org.joml.Vector4d(
                            position.x, position.y, position.z, 1.0D));
                    double originalX = local.x;
                    local.x *= (1.0D - centerFactor);
                    if (Math.abs(originalX) > 1.0E-3D) {
                        org.joml.Vector4d world = transform.transform(local);
                        desiredX = world.x;
                        desiredY = world.y;
                        desiredZ = world.z;
                        moved = true;
                    }
                } catch (Throwable ignored) {
                    // 变换不可用时保持原版机位
                }
            }

            if (distanceGain > 0.0D) {
                double speed = vehicle.getDeltaMovement().length();
                double extra = Math.min(2.0D, speed * distanceGain * 0.5D) * adsScale;
                if (extra > 0.01D) {
                    double yawRad = Math.toRadians(baseYaw);
                    double pitchRad = Math.toRadians(basePitch);
                    double lookX = -Math.sin(yawRad) * Math.cos(pitchRad);
                    double lookY = -Math.sin(pitchRad);
                    double lookZ = Math.cos(yawRad) * Math.cos(pitchRad);
                    desiredX -= lookX * extra;
                    desiredY -= lookY * extra;
                    desiredZ -= lookZ * extra;
                    extraDistance = extra;
                    moved = true;
                }
            }

            if (origin != null && positionLag > 1.0E-4D) {
                Vec3 center = new Vec3(origin.x, origin.y, origin.z);
                Vec3 targetRel = new Vec3(desiredX, desiredY, desiredZ).subtract(center);
                if (smoothedRel == null) {
                    smoothedRel = targetRel;
                    REL_X_VEL[0] = 0.0D;
                    REL_Y_VEL[0] = 0.0D;
                    REL_Z_VEL[0] = 0.0D;
                } else {
                    double springTime = positionLag / agility;
                    smoothedRel = new Vec3(
                            springStep(smoothedRel.x, targetRel.x, REL_X_VEL, springTime, springDamping, 12.0D, dt),
                            springStep(smoothedRel.y, targetRel.y, REL_Y_VEL, springTime, springDamping, 8.0D, dt),
                            springStep(smoothedRel.z, targetRel.z, REL_Z_VEL, springTime, springDamping, 12.0D, dt));
                }
                Vec3 result = center.add(smoothedRel);
                desiredX = result.x;
                desiredY = result.y;
                desiredZ = result.z;
                moved = true;
            } else {
                smoothedRel = null;
            }

            if (moved) {
                CameraAccess.setPosition(camera, desiredX, desiredY, desiredZ);
            }
        } else {
            smoothedRel = null;
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || !ClientConfig.CAMERA_ENABLED.get()) {
            return;
        }
        CameraPreset preset = ClientConfig.CAMERA_PRESET.get();
        if (preset == CameraPreset.VANILLA) {
            return;
        }
        if (!(player.getVehicle() instanceof VehicleEntity vehicle)) {
            return;
        }
        VehicleType type = vehicle.getVehicleType();
        if (type != VehicleType.AIRPLANE && type != VehicleType.HELICOPTER) {
            return;
        }
        // 第一人称/右键瞄准的第一人称放大：使用第一人称自己的设置，不叠加第三人称速度 FOV。
        if (mc.options.getCameraType() == CameraType.FIRST_PERSON || ClientEventHandler.zoomVehicle) {
            return;
        }
        double speed = vehicle.getDeltaMovement().length();
        double gain = ClientConfig.CAMERA_FOV_GAIN.get() * preset.fovScale;
        if (ClientEventHandler.zoomVehicle) {
            gain *= ClientConfig.CAMERA_ADS_SCALE.get();
        }
        double multiplier = 1.0D + gain * Math.min(1.0D, speed / 2.0D);
        event.setFOV(event.getFOV() * multiplier);
    }

    private static double alpha(double dt, double lagSeconds) {
        if (lagSeconds <= 1.0E-4D) {
            return 1.0D;
        }
        return 1.0D - Math.exp(-dt / lagSeconds);
    }

    /** 把 angle 展开到离 reference 最近的等价角，消除 ±180° 处的 360° 跳变。 */
    private static double unwrapNear(double angle, double reference) {
        if (Double.isNaN(reference)) {
            return angle;
        }
        return reference + Mth.wrapDegrees(angle - reference);
    }

    /**
     * 软阈值：接近 ±limit 时自动减速，而不是撞到硬钳制后突然停住。
     * softness=0 保持原来的硬 clamp；softness=1 使用 tanh 饱和。
     */
    private static double softLimit(double value, double limit, double softness) {
        double hard = Mth.clamp(value, -limit, limit);
        if (softness <= 1.0E-4D) {
            return hard;
        }
        double soft = limit * Math.tanh(value / limit);
        return hard + (soft - hard) * Mth.clamp(softness, 0.0D, 1.0D);
    }

    /**
     * 物理弹簧积分：固定子步长（120Hz）积分质量-弹簧-阻尼系统，避免低帧率时“一帧直接弹到目标”。
     *
     * <p>smoothTime 仍沿用原来的相机滞后参数语义：自然频率 omega = 2 / smoothTime。
     * dampingRatio = 1 为临界阻尼；略小于 1 会有轻微回弹，减少“死板”感。
     */
    private static double springStep(double current, double target, double[] velocity,
                                     double smoothTime, double dampingRatio, double maxSpeed, double dt) {
        if (smoothTime <= 1.0E-4D) {
            velocity[0] = 0.0D;
            return target;
        }
        // 极小 smoothTime 下显式积分不稳定；低于 20ms 时直接按目标处理。
        if (smoothTime < 0.02D) {
            velocity[0] = 0.0D;
            return target;
        }
        double ratio = Mth.clamp(dampingRatio, 0.5D, 1.5D);
        double omega = 2.0D / smoothTime;
        double stiffness = omega * omega;
        double damping = 2.0D * ratio * omega;
        int steps = Mth.clamp((int) Math.ceil(dt / (1.0D / 120.0D)), 1, 12);
        double h = Math.max(1.0E-4D, dt / steps);
        double value = current;
        double v = velocity[0];
        for (int i = 0; i < steps; i++) {
            double accel = (target - value) * stiffness - v * damping;
            v += accel * h;
            v = Mth.clamp(v, -maxSpeed, maxSpeed);
            value += v * h;
        }
        velocity[0] = v;
        return value;
    }

    /** 复刻卓越前线 GameRendererMixin 加到 pose stack 上的滚转量（度）。 */
    private static double stockCameraRoll(VehicleEntity vehicle, int seat, float partial, double camYaw) {
        double bodyYaw = Mth.lerp(partial, vehicle.yRotO, vehicle.getYRot());
        double angle = Mth.wrapDegrees(camYaw - bodyYaw);
        java.util.List<SeatInfo> seats = VehicleData.compute(vehicle).seats();
        if (seat < 0 || seat >= seats.size()) {
            return 0.0D;
        }
        SeatInfo seatInfo = seats.get(seat);
        if (seatInfo != null && "VehicleFlat".equals(seatInfo.transform)) {
            angle = 0.0D;
        }
        double r = (Math.abs(angle) - 90.0D) / 90.0D;
        double r2;
        if (Math.abs(angle) <= 90.0D) {
            r2 = angle / 90.0D;
        } else if (angle < 0.0D) {
            r2 = -(180.0D + angle) / 90.0D;
        } else {
            r2 = (180.0D - angle) / 90.0D;
        }
        return -r * vehicle.getRoll(partial) - r2 * vehicle.getViewXRot(partial);
    }

    private static void resetTrack() {
        trackedVehicleId = -1;
        offsetYaw = 0.0D;
        offsetPitch = 0.0D;
        extraDistance = 0.0D;
        smoothedRoll = Double.NaN;
        smoothedYawRate = 0.0D;
        smoothedPitchRate = 0.0D;
        smoothedRollRate = 0.0D;
        prevVehicleRoll = Double.NaN;
        lastCamYaw = 0.0D;
        lastCamPitch = 0.0D;
        lastCamRoll = 0.0D;
        lastStockRoll = 0.0D;
        lastAgility = 1.0D;
        lastActivity = 0.0D;
        lastApplied = false;
        lastFinalCameraYaw = Double.NaN;
        smoothedRel = null;
        OFFSET_YAW_VEL[0] = 0.0D;
        OFFSET_PITCH_VEL[0] = 0.0D;
        ROLL_VEL[0] = 0.0D;
        REL_X_VEL[0] = 0.0D;
        REL_Y_VEL[0] = 0.0D;
        REL_Z_VEL[0] = 0.0D;
    }

    // ---- 调试用只读状态 ----

    public static double getOffsetYaw() {
        return offsetYaw;
    }

    public static double getOffsetPitch() {
        return offsetPitch;
    }

    public static double getExtraDistance() {
        return extraDistance;
    }

    public static double getLastCamYaw() {
        return lastCamYaw;
    }

    public static double getLastCamPitch() {
        return lastCamPitch;
    }

    public static double getLastCamRoll() {
        return lastCamRoll;
    }

    public static double getLastStockRoll() {
        return lastStockRoll;
    }

    public static double getAgility() {
        return lastAgility;
    }

    public static double getActivity() {
        return lastActivity;
    }

    public static double getSmoothedYawRate() {
        return smoothedYawRate;
    }

    public static double getSmoothedPitchRate() {
        return smoothedPitchRate;
    }

    public static double getSmoothedRollRate() {
        return smoothedRollRate;
    }

    public static boolean isApplied() {
        return lastApplied;
    }
}
