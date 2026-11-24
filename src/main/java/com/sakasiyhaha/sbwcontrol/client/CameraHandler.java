package com.sakasiyhaha.sbwcontrol.client;

import com.atsuishio.superbwarfare.data.vehicle.VehicleData;
import com.atsuishio.superbwarfare.data.vehicle.subdata.SeatInfo;
import com.atsuishio.superbwarfare.data.vehicle.subdata.VehicleType;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.sakasiyhaha.sbwcontrol.SbwControl;
import com.sakasiyhaha.sbwcontrol.camera.CameraPreset;
import com.sakasiyhaha.sbwcontrol.camera.view.CameraViewMode;
import com.sakasiyhaha.sbwcontrol.camera.view.VehicleViewMode;
import com.sakasiyhaha.sbwcontrol.camera.dynamic.CenteredChaseCamera;
import com.sakasiyhaha.sbwcontrol.camera.dynamic.DynamicCameraContext;
import com.sakasiyhaha.sbwcontrol.camera.dynamic.DynamicCameraEngine;
import com.sakasiyhaha.sbwcontrol.camera.offset.OffsetController;
import com.sakasiyhaha.sbwcontrol.camera.offset.OffsetPhase;
import com.sakasiyhaha.sbwcontrol.camera.offset.OffsetResult;
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

    private static final OffsetController OFFSET_CONTROLLER = new OffsetController();
    private static boolean offsetEngineWasV2 = true;
    private static final DynamicCameraEngine DYNAMIC_ENGINE = new DynamicCameraEngine();
    private static final CenteredChaseCamera CENTERED_CHASE = new CenteredChaseCamera();
    private static boolean dynamicEngineWasV3 = true;

    private static int trackedVehicleId = -1;
    private static double prevYaw;
    private static double prevPitch;
    private static double offsetYaw;
    private static double offsetPitch;
    /** 二维误差圆回正状态。 */
    private static boolean recenterActive;
    private static boolean recenterHoldActive;
    private static double recenterNeutralTime;
    /** 上一帧回正开关状态，用于关闭回正时清理旧速度。 */
    private static boolean recenterWasEnabled = true;
    /** 边缘制动的平滑状态：避免径向速度符号变化时阻尼/限速瞬间跳变。 */
    private static double edgeBrakeState;
    private static double edgeSpeedScale = 1.0D;
    private static boolean edgeBrakeLatched;
    private static OffsetPhase offsetPhase = OffsetPhase.NORMAL;
    /** 动态偏移的绝对最大角度；边缘误差圆在此基础上向内收缩。 */
    private static final double CAMERA_YAW_LIMIT = 12.0D;
    private static final double CAMERA_PITCH_LIMIT = 8.0D;
    private static double recenterStartRadius;
    private static double recenterAngle;
    private static double recenterElapsed;
    private static double recenterHoldElapsed;
    private static double recenterHoldYaw;
    private static double recenterHoldPitch;
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
    private static boolean lastHelicopter;
    private static boolean lastRollFollowEnabled;
    private static double lastRollCoupling;

    /** 非线性（临界阻尼弹簧）平滑状态：速度会跨帧保留，避免每帧直接跳到目标造成"生硬帧感"。 */
    private static final double[] OFFSET_YAW_VEL = new double[1];
    private static final double[] OFFSET_PITCH_VEL = new double[1];
    private static final double[] ROLL_VEL = new double[1];
    /** 相机机位绕机体纵轴的 Sticky 滚转（度）。 */
    private static double orbitRoll = Double.NaN;
    private static final double[] ORBIT_VEL = new double[1];
    private static double lastOrbitRoll;
    private static final double[] REL_X_VEL = new double[1];
    private static final double[] REL_Y_VEL = new double[1];
    private static final double[] REL_Z_VEL = new double[1];
    /** 上一帧最终相机 yaw：SW 的 pose 滚转公式用的是上一帧相机 yaw（含我们的偏移）。 */
    private static double lastFinalCameraYaw = Double.NaN;
    /** 平滑后的“相机相对载具中心”向量，用于位置弹簧滞后。 */
    private static Vec3 smoothedRel;

    private CameraHandler() {
    }

    /** 解析当前载具的 TPS 视角模式。 */
    private static CameraViewMode resolveViewMode(VehicleType type) {
        CameraViewMode global = ClientConfig.CAMERA_GLOBAL_VIEW_MODE.get();
        VehicleViewMode raw = type == VehicleType.HELICOPTER
                ? ClientConfig.CAMERA_HELICOPTER_VIEW_MODE.get()
                : ClientConfig.CAMERA_AIRPLANE_VIEW_MODE.get();
        return raw.resolve(global);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            resetTrack();
            return;
        }
        if (!(player.getVehicle() instanceof VehicleEntity vehicle)) {
            resetTrack();
            return;
        }
        VehicleType type = vehicle.getVehicleType();
        if (type != VehicleType.AIRPLANE && type != VehicleType.HELICOPTER) {
            resetTrack();
            return;
        }
        // 卓越前线原生自由相机自己控制视角，不能叠加 Sticky/动态偏移。
        if (ClientEventHandler.isFreeCam(player)) {
            resetTrack();
            return;
        }
        boolean helicopter = type == VehicleType.HELICOPTER;
        CameraPreset preset = ClientConfig.CAMERA_PRESET.get();
        boolean rollFollowEnabled = helicopter
                ? ClientConfig.CAMERA_ROLL_HELICOPTER_ENABLED.get()
                : ClientConfig.CAMERA_ROLL_AIRPLANE_ENABLED.get();
        double rollCoupling = rollFollowEnabled
                ? (helicopter ? ClientConfig.CAMERA_ROLL_HELICOPTER_COUPLING.get()
                        : ClientConfig.CAMERA_ROLL_AIRPLANE_COUPLING.get())
                : 0.0D;
        double categoryRollLag = helicopter
                ? ClientConfig.CAMERA_ROLL_HELICOPTER_LAG.get()
                : ClientConfig.CAMERA_ROLL_AIRPLANE_LAG.get();
        lastHelicopter = helicopter;
        lastRollFollowEnabled = rollFollowEnabled;
        lastRollCoupling = rollCoupling;
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
        CameraViewMode viewMode = resolveViewMode(type);
        if (fp) {
            // 第一人称/ADS 始终使用卓越前线原生相机。
            resetTrack();
            return;
        }
        if (viewMode == CameraViewMode.NATIVE) {
            resetTrack();
            return;
        }
        CENTERED_CHASE.apply(event, vehicle, seat, (float) event.getPartialTick(), viewMode);
        offsetYaw = CENTERED_CHASE.getYawOffset();
        offsetPitch = CENTERED_CHASE.getPitchOffset();
        offsetPhase = OffsetPhase.NORMAL;
        edgeBrakeState = 0.0D;
        edgeSpeedScale = 1.0D;
        lastCamYaw = event.getYaw();
        lastCamPitch = event.getPitch();
        lastCamRoll = event.getRoll();
        lastStockRoll = 0.0D;
        lastOrbitRoll = 0.0D;
        extraDistance = 0.0D;
        lastApplied = true;
        return;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        // 视角接管不修改 FOV：第一人称/ADS/超速 FOV 全部保持卓越前线原生。
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
     * 两段式软阈值：第一阈值开始弹性压缩，第二阈值硬停止。
     * 曲线在 T1 处一阶连续（导数 1），在 T2 处导数 0；超过 T2 直接钳制。
     */
    private static double twoStageLimit(double value, double soft, double hard) {
        double t2 = Math.max(0.25D, hard);
        double t1 = Mth.clamp(soft, 0.0D, t2 - 0.01D);
        double abs = Math.abs(value);
        if (abs <= t1) {
            return value;
        }
        if (abs >= t2) {
            return Math.copySign(t2, value);
        }
        double t = (abs - t1) / (t2 - t1);
        double f = t + t * t - t * t * t;
        return Math.copySign(t1 + (t2 - t1) * f, value);
    }

    /**
     * 带两段缓冲区的弹簧：进入第一阈值后逐步增加阻尼并限制最大速度，
     * 到第二阈值时硬钳制位置并清除向外速度。
     */
    private static double springStepLimited(double current, double target, double[] velocity,
                                            double smoothTime, double dampingRatio, double maxSpeed,
                                            double dt, double soft, double hard, double bufferDamping,
                                            double recenterDamping) {
        if (smoothTime <= 1.0E-4D || smoothTime < 0.02D) {
            velocity[0] = 0.0D;
            return target;
        }
        double t2 = Math.max(0.25D, hard);
        double t1 = Mth.clamp(soft, 0.0D, t2 - 0.01D);
        double ratio = Mth.clamp(dampingRatio, 0.5D, 1.5D);
        double omega = 2.0D / smoothTime;
        double stiffness = omega * omega;
        double baseDamping = 2.0D * ratio * omega;
        double extraGain = Mth.clamp(bufferDamping, 0.0D, 2.0D);
        int steps = Mth.clamp((int) Math.ceil(dt / (1.0D / 120.0D)), 1, 12);
        double h = Math.max(1.0E-4D, dt / steps);
        double value = Mth.clamp(current, -t2, t2);
        double v = velocity[0];
        for (int i = 0; i < steps; i++) {
            // 缓冲阻尼只在“向外推”时生效；回正时使用基础阻尼，
            // 否则离开第一阈值后额外阻尼突然消失，会产生“快到中心时突然加速”的硬回正。
            boolean outward = (value > 0.0D && v >= 0.0D) || (value < 0.0D && v <= 0.0D);
            double edge = outward
                    ? Mth.clamp((Math.abs(value) - t1) / (t2 - t1), 0.0D, 1.0D)
                    : 0.0D;
            double smooth = edge * edge * (3.0D - 2.0D * edge);
            double localMaxSpeed = Math.max(0.01D, maxSpeed * (1.0D - 0.7D * smooth));
            double damping = baseDamping + omega * extraGain * smooth;
            // 回正末段：在中心附近、速度朝中心时增加阻尼，避免弹簧在 0 点速度最大造成硬碰。
            boolean returning = value * v < 0.0D && Math.abs(value) < t1;
            if (returning) {
                double center = Mth.clamp(1.0D - Math.abs(value) / t1, 0.0D, 1.0D);
                damping += omega * Mth.clamp(recenterDamping, 0.0D, 2.0D) * center;
                localMaxSpeed *= (1.0D - 0.35D * center);
            }
            double accel = (target - value) * stiffness - v * damping;
            v += accel * h;
            v = Mth.clamp(v, -localMaxSpeed, localMaxSpeed);
            value += v * h;
            if (value > t2) {
                value = t2;
                v = Math.min(v, 0.0D);
            } else if (value < -t2) {
                value = -t2;
                v = Math.max(v, 0.0D);
            }
        }
        velocity[0] = v;
        return value;
    }

    /**
     * 物理弹簧积分：固定子步长（120Hz）积分质量-弹簧-阻尼系统，避免低帧率时“一帧直接弹到目标”。
     *
     * <p>smoothTime 仍沿用原来的相机滞后参数语义：自然频率 omega = 2 / smoothTime。
     * dampingRatio = 1 为临界阻尼；略小于 1 会有轻微回弹，减少“死板”感。
     */
    private static double springStep(double current, double target, double[] velocity,
                                     double smoothTime, double dampingRatio, double maxSpeed, double dt) {
        return springStep(current, target, velocity, smoothTime, dampingRatio, maxSpeed, dt,
                0.0D, 1.0D);
    }

    /**
     * 带可选边缘制动参数的弹簧。
     * extraDamping 只在外推动画里由边缘误差圆计算，回正/内收时为 0。
     */
    private static double springStep(double current, double target, double[] velocity,
                                     double smoothTime, double dampingRatio, double maxSpeed,
                                     double dt, double extraDamping, double maxSpeedScale) {
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
        double damping = 2.0D * ratio * omega + Math.max(0.0D, extraDamping);
        double speedLimit = Math.max(0.01D, maxSpeed * Mth.clamp(maxSpeedScale, 0.05D, 1.0D));
        int steps = Mth.clamp((int) Math.ceil(dt / (1.0D / 120.0D)), 1, 12);
        double h = Math.max(1.0E-4D, dt / steps);
        double value = current;
        double v = velocity[0];
        for (int i = 0; i < steps; i++) {
            double accel = (target - value) * stiffness - v * damping;
            v += accel * h;
            v = Mth.clamp(v, -speedLimit, speedLimit);
            value += v * h;
        }
        velocity[0] = v;
        return value;
    }

    /**
     * 完全刚性 Sticky：不做动态偏移、不做弹簧、不做位置滞后。
     * 只把相机 roll 直接设成机体 roll，并把机位绕机体纵轴刚性旋转。
     */
    private static void applyRigidSticky(ViewportEvent.ComputeCameraAngles event,
                                        VehicleEntity vehicle, int seat, float partial) {
        double camYaw = event.getYaw();
        double camPitch = event.getPitch();
        double stockRoll = unwrapNear(stockCameraRoll(vehicle, seat, partial, camYaw), lastStockRoll);
        double bodyRoll = unwrapNear(vehicle.getRoll(partial), stockRoll);
        int mode = ClientConfig.CAMERA_STICKY_ROLL_MODE.get();
        double eventRoll = switch (mode) {
            case 0 -> bodyRoll - stockRoll;
            case 2 -> stockRoll - bodyRoll;
            case 3 -> -bodyRoll;
            default -> bodyRoll;
        };
        eventRoll = Mth.wrapDegrees(eventRoll);
        event.setRoll((float) eventRoll);

        offsetYaw = 0.0D;
        offsetPitch = 0.0D;
        OFFSET_YAW_VEL[0] = 0.0D;
        OFFSET_PITCH_VEL[0] = 0.0D;
        OFFSET_CONTROLLER.reset();
        edgeBrakeState = 0.0D;
        edgeSpeedScale = 1.0D;
        edgeBrakeLatched = false;
        offsetPhase = OffsetPhase.NORMAL;
        smoothedRoll = bodyRoll;
        ROLL_VEL[0] = 0.0D;
        orbitRoll = bodyRoll;
        ORBIT_VEL[0] = 0.0D;
        smoothedRel = null;
        lastAgility = 1.0D;
        lastActivity = 0.0D;
        lastCamYaw = camYaw;
        lastCamPitch = camPitch;
        lastCamRoll = eventRoll;
        lastStockRoll = stockRoll;
        lastOrbitRoll = bodyRoll;
        lastFinalCameraYaw = camYaw;
        lastApplied = true;

        try {
            Minecraft mc = Minecraft.getInstance();
            Camera camera = mc.gameRenderer.getMainCamera();
            Vec3 position = camera.getPosition();
            org.joml.Matrix4d transform = vehicle.getClientVehicleTransform(partial);
            org.joml.Matrix4d inverse = new org.joml.Matrix4d(transform).invert();
            org.joml.Vector4d local = inverse.transform(new org.joml.Vector4d(
                    position.x, position.y, position.z, 1.0D));
            org.joml.Vector4d rotated = new org.joml.Matrix4d()
                    .rotateZ(Math.toRadians(bodyRoll))
                    .transform(local);
            org.joml.Vector4d world = transform.transform(rotated);
            CameraAccess.setPosition(camera, world.x, world.y, world.z);
        } catch (Throwable ignored) {
            // 刚性模式是视觉功能，变换失败时保留 SW 原机位。
        }
    }

    /** 复刻卓越前线 GameRendererMixin 加到 pose stack 上的滚转量（度）。 */
    private static double stockCameraRoll(VehicleEntity vehicle, int seat, float partial, double camYaw) {        double bodyYaw = Mth.lerp(partial, vehicle.yRotO, vehicle.getYRot());
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

    private static void cancelRecenter() {
        recenterActive = false;
        recenterHoldActive = false;
        recenterNeutralTime = 0.0D;
        // 回正动画产生的速度不能交给弹簧继续使用，否则会先继续回正再反向。
        OFFSET_YAW_VEL[0] = 0.0D;
        OFFSET_PITCH_VEL[0] = 0.0D;
    }

    private static void resetTrack() {
        trackedVehicleId = -1;
        offsetYaw = 0.0D;
        offsetPitch = 0.0D;
        recenterActive = false;
        recenterHoldActive = false;
        recenterNeutralTime = 0.0D;
        recenterWasEnabled = true;
        edgeBrakeState = 0.0D;
        edgeSpeedScale = 1.0D;
        edgeBrakeLatched = false;
        offsetPhase = OffsetPhase.NORMAL;
        OFFSET_CONTROLLER.reset();
        offsetEngineWasV2 = true;
        DYNAMIC_ENGINE.reset();
        CENTERED_CHASE.reset();
        dynamicEngineWasV3 = true;
        recenterStartRadius = 0.0D;
        recenterAngle = 0.0D;
        recenterElapsed = 0.0D;
        recenterHoldElapsed = 0.0D;
        recenterHoldYaw = 0.0D;
        recenterHoldPitch = 0.0D;
        extraDistance = 0.0D;
        smoothedRoll = Double.NaN;
        orbitRoll = Double.NaN;
        ORBIT_VEL[0] = 0.0D;
        lastOrbitRoll = 0.0D;
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

    public static OffsetPhase getOffsetPhase() {
        return offsetPhase;
    }

    public static double getEdgeBrakeState() {
        return edgeBrakeState;
    }

    public static double getEdgeSpeedScale() {
        return edgeSpeedScale;
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

    public static double getLastOrbitRoll() {
        return lastOrbitRoll;
    }

    public static boolean isLastHelicopter() {
        return lastHelicopter;
    }

    public static boolean isRollFollowEnabled() {
        return lastRollFollowEnabled;
    }

    public static double getRollCoupling() {
        return lastRollCoupling;
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
