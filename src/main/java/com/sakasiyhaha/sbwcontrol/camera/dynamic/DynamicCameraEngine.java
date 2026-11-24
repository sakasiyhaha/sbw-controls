package com.sakasiyhaha.sbwcontrol.camera.dynamic;

import com.atsuishio.superbwarfare.data.vehicle.VehicleData;
import com.atsuishio.superbwarfare.data.vehicle.subdata.SeatInfo;
import com.atsuishio.superbwarfare.data.vehicle.subdata.VehicleType;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.sakasiyhaha.sbwcontrol.camera.offset.OffsetPhase;
import com.sakasiyhaha.sbwcontrol.client.CameraAccess;
import com.sakasiyhaha.sbwcontrol.client.ClientControlHandler;
import com.sakasiyhaha.sbwcontrol.config.ClientConfig;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.CameraType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ViewportEvent;

import java.util.List;

/**
 * 从零重写的动态相机引擎。
 *
 * <p>每帧只做四件事：计算上下文输入、更新偏移管线、更新滚转管线、更新机位管线。
 * 偏移、滚转、机位互不直接修改对方状态。
 */
@OnlyIn(Dist.CLIENT)
public final class DynamicCameraEngine {

    private final OffsetPipeline offsetPipeline = new OffsetPipeline();
    private final RollPipeline rollPipeline = new RollPipeline();
    private final PositionPipeline positionPipeline = new PositionPipeline();

    private int trackedVehicleId = -1;
    private double prevYaw;
    private double prevPitch;
    private double prevVehicleRoll = Double.NaN;
    private double smoothedYawRate;
    private double smoothedPitchRate;
    private double smoothedRollRate;
    private double lastFinalCameraYaw = Double.NaN;

    private double offsetYaw;
    private double offsetPitch;
    private OffsetPhase phase = OffsetPhase.NORMAL;
    private double edgeBrakeState;
    private double edgeSpeedScale = 1.0D;
    private double lastCamYaw;
    private double lastCamPitch;
    private double lastCamRoll;
    private double lastStockRoll;
    private double lastOrbitRoll;
    private double extraDistance;
    private double agility = 1.0D;
    private double activity;

    public void reset() {
        trackedVehicleId = -1;
        prevYaw = 0.0D;
        prevPitch = 0.0D;
        prevVehicleRoll = Double.NaN;
        smoothedYawRate = 0.0D;
        smoothedPitchRate = 0.0D;
        smoothedRollRate = 0.0D;
        lastFinalCameraYaw = Double.NaN;
        offsetYaw = 0.0D;
        offsetPitch = 0.0D;
        phase = OffsetPhase.NORMAL;
        edgeBrakeState = 0.0D;
        edgeSpeedScale = 1.0D;
        lastCamYaw = 0.0D;
        lastCamPitch = 0.0D;
        lastCamRoll = 0.0D;
        lastStockRoll = 0.0D;
        lastOrbitRoll = 0.0D;
        extraDistance = 0.0D;
        agility = 1.0D;
        activity = 0.0D;
        offsetPipeline.reset();
        rollPipeline.reset();
        positionPipeline.reset();
    }

    /** 新旧引擎切换时从当前偏移接续，避免切换瞬间跳变。 */
    public void seed(double yaw, double pitch) {
        offsetYaw = yaw;
        offsetPitch = pitch;
        offsetPipeline.seed(yaw, pitch);
        rollPipeline.reset();
        positionPipeline.reset();
        lastFinalCameraYaw = Double.NaN;
    }

    public void apply(ViewportEvent.ComputeCameraAngles event, DynamicCameraContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        VehicleEntity vehicle = ctx.vehicle();
        if (vehicle.getId() != trackedVehicleId) {
            reset();
            trackedVehicleId = vehicle.getId();
        }
        double vehicleYaw = vehicle.getYaw(ctx.partial());
        double vehiclePitch = vehicle.getPitch(ctx.partial());
        prevYaw = vehicleYaw;
        prevPitch = vehiclePitch;

        double dt = Math.max(1.0E-3D, ctx.dt());
        double lagYaw = ctx.firstPerson() ? ClientConfig.CAMERA_FP_LAG_YAW.get()
                : ClientConfig.CAMERA_LAG_YAW.get() * ctx.preset().lagYawScale;
        double lagPitch = ctx.firstPerson() ? ClientConfig.CAMERA_FP_LAG_PITCH.get()
                : ClientConfig.CAMERA_LAG_PITCH.get() * ctx.preset().lagPitchScale;

        double rawYawRate = Mth.wrapDegrees(vehicle.getYRot() - vehicle.yRotO) * 20.0D;
        double rawPitchRate = (vehicle.getXRot() - vehicle.xRotO) * 20.0D;
        double rateSmoothing = ctx.firstPerson()
                ? Math.min(0.10D, ClientConfig.CAMERA_RATE_SMOOTHING.get())
                : ClientConfig.CAMERA_RATE_SMOOTHING.get();
        smoothedYawRate += (rawYawRate - smoothedYawRate) * alpha(dt, rateSmoothing);
        smoothedPitchRate += (rawPitchRate - smoothedPitchRate) * alpha(dt, rateSmoothing);

        double vehicleRoll = vehicle.getRoll(ctx.partial());
        if (Double.isNaN(prevVehicleRoll)) {
            prevVehicleRoll = vehicleRoll;
        }
        double rawRollRate = Mth.wrapDegrees(vehicleRoll - prevVehicleRoll) / dt;
        prevVehicleRoll = vehicleRoll;
        smoothedRollRate += (rawRollRate - smoothedRollRate) * alpha(dt, rateSmoothing);

        double adaptiveGain = ctx.firstPerson() ? ClientConfig.CAMERA_FP_ADAPTIVE_GAIN.get()
                : ClientConfig.CAMERA_ADAPTIVE_GAIN.get();
        activity = Mth.clamp((Math.abs(smoothedYawRate) + Math.abs(smoothedRollRate)) / 120.0D,
                0.0D, 1.0D);
        agility = 1.0D + adaptiveGain * activity;
        double adjustedLagYaw = lagYaw / agility;
        double adjustedLagPitch = lagPitch / agility;

        double lagSeconds = ctx.firstPerson() ? ClientConfig.CAMERA_FP_TURN_LAG.get()
                : ClientConfig.CAMERA_LEAD.get() * ctx.preset().leadScale;
        double turnSign = ClientConfig.CAMERA_TURN_LAG_INVERT.get() ? 1.0D : -1.0D;
        double turnSignPitch = ClientConfig.CAMERA_TURN_LAG_INVERT_PITCH.get() ? 1.0D : -1.0D;
        double softness = ctx.firstPerson() ? 0.0D : ClientConfig.CAMERA_SOFT_LIMIT.get();
        double targetYaw = softLimit(turnSign * smoothedYawRate * lagSeconds, 12.0D, softness);
        double targetPitch = softLimit(turnSignPitch * smoothedPitchRate * lagSeconds, 8.0D, softness);
        if (ctx.rollFollowEnabled()) {
            targetYaw = 0.0D;
            targetPitch = 0.0D;
        }
        double adsScale = ctx.firstPerson() ? 1.0D
                : (ctx.zoom() ? ClientConfig.CAMERA_ADS_SCALE.get() : 1.0D);
        targetYaw *= adsScale;
        targetPitch *= adsScale;

        OffsetPipeline.Output offset = offsetPipeline.update(new OffsetPipeline.Input(
                targetYaw, targetPitch,
                ClientControlHandler.isRawInputActive(), dt,
                vehicle.getDeltaMovement().length(), activity,
                ctx.firstPerson(), adjustedLagYaw, adjustedLagPitch,
                ctx.firstPerson() ? 1.0D : ClientConfig.CAMERA_SPRING_DAMPING.get()));
        offsetYaw = offset.yaw();
        offsetPitch = offset.pitch();
        phase = offset.phase();
        edgeBrakeState = offset.edgeBrakeState();
        edgeSpeedScale = offset.edgeSpeedScale();

        double baseYaw = event.getYaw();
        double basePitch = event.getPitch();
        double finalYaw = baseYaw + offsetYaw;
        double finalPitch = basePitch + offsetPitch;
        event.setYaw((float) finalYaw);
        event.setPitch((float) finalPitch);
        lastCamYaw = finalYaw;
        lastCamPitch = finalPitch;

        if (!ctx.firstPerson()) {
            double stockRoll = stockCameraRoll(vehicle, ctx.seat(), ctx.partial(),
                    Double.isNaN(lastFinalCameraYaw) ? baseYaw : lastFinalCameraYaw);
            double categoryLag = ctx.rollFollowEnabled() ? ctx.rollLag() : ClientConfig.CAMERA_LAG_ROLL.get();
            RollPipeline.Output roll = rollPipeline.update(new RollPipeline.Input(
                    stockRoll, vehicle.getRoll(ctx.partial()),
                    ClientConfig.CAMERA_ROLL_RATIO.get(),
                    ctx.rollFollowEnabled(), ctx.rollCoupling(), categoryLag,
                    agility, ClientConfig.CAMERA_SPRING_DAMPING.get(), dt));
            event.setRoll((float) roll.eventRoll());
            lastCamRoll = roll.eventRoll();
            lastStockRoll = roll.stockRoll();
            lastOrbitRoll = roll.orbitRoll();
        } else {
            event.setRoll(0.0F);
            lastCamRoll = 0.0D;
            lastStockRoll = 0.0D;
            lastOrbitRoll = 0.0D;
            rollPipeline.reset();
        }
        lastFinalCameraYaw = finalYaw;

        if (!ctx.firstPerson()) {
            Camera camera = mc.gameRenderer.getMainCamera();
            PositionPipeline.Output pos = positionPipeline.update(new PositionPipeline.Input(
                    camera, vehicle, ctx.partial(), baseYaw, basePitch, adsScale,
                    ClientConfig.CAMERA_DISTANCE_GAIN.get() * ctx.preset().distanceScale,
                    ClientConfig.CAMERA_CENTER_LATERAL.get(),
                    ctx.rollFollowEnabled() ? 0.0D : ClientConfig.CAMERA_POSITION_LAG.get(),
                    ctx.rollFollowEnabled(), lastOrbitRoll, agility,
                    ClientConfig.CAMERA_SPRING_DAMPING.get(), dt, false));
            extraDistance = pos.extraDistance();
        } else {
            extraDistance = 0.0D;
            positionPipeline.reset();
        }
    }

    /** 完全刚性 Sticky：角度/机位直接随机体滚转，不经过任何弹簧。 */
    public void applyRigid(ViewportEvent.ComputeCameraAngles event, DynamicCameraContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        VehicleEntity vehicle = ctx.vehicle();
        double camYaw = event.getYaw();
        double stockRoll = stockCameraRoll(vehicle, ctx.seat(), ctx.partial(), camYaw);
        double bodyRoll = stockRoll + Mth.wrapDegrees(vehicle.getRoll(ctx.partial()) - stockRoll);
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
        phase = OffsetPhase.NORMAL;
        edgeBrakeState = 0.0D;
        edgeSpeedScale = 1.0D;
        lastCamYaw = camYaw;
        lastCamPitch = event.getPitch();
        lastCamRoll = eventRoll;
        lastStockRoll = stockRoll;
        lastOrbitRoll = bodyRoll;
        lastFinalCameraYaw = camYaw;
        offsetPipeline.reset();
        rollPipeline.reset();
        positionPipeline.reset();
        try {
            Camera camera = mc.gameRenderer.getMainCamera();
            Vec3 position = camera.getPosition();
            org.joml.Matrix4d transform = vehicle.getClientVehicleTransform(ctx.partial());
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

    public void applyFov(ViewportEvent.ComputeFov event, DynamicCameraContext ctx) {        Minecraft mc = Minecraft.getInstance();
        if (ctx.preset() == com.sakasiyhaha.sbwcontrol.camera.CameraPreset.VANILLA) {
            return;
        }
        VehicleType type = ctx.vehicle().getVehicleType();
        if (type != VehicleType.AIRPLANE && type != VehicleType.HELICOPTER) {
            return;
        }
        if (ClientConfig.CAMERA_STICKY_RIGID.get()
                && (type == VehicleType.HELICOPTER
                ? ClientConfig.CAMERA_ROLL_HELICOPTER_ENABLED.get()
                : ClientConfig.CAMERA_ROLL_AIRPLANE_ENABLED.get())) {
            return;
        }
        if (mc.options.getCameraType() == CameraType.FIRST_PERSON || ctx.zoom()) {
            return;
        }
        double speed = ctx.vehicle().getDeltaMovement().length();
        double gain = ClientConfig.CAMERA_FOV_GAIN.get() * ctx.preset().fovScale;
        if (ctx.zoom()) {
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

    private static double softLimit(double value, double limit, double softness) {
        double hard = Mth.clamp(value, -limit, limit);
        if (softness <= 1.0E-4D) {
            return hard;
        }
        double soft = limit * Math.tanh(value / limit);
        return hard + (soft - hard) * Mth.clamp(softness, 0.0D, 1.0D);
    }

    private static double stockCameraRoll(VehicleEntity vehicle, int seat, float partial, double camYaw) {
        double bodyYaw = Mth.lerp(partial, vehicle.yRotO, vehicle.getYRot());
        double angle = Mth.wrapDegrees(camYaw - bodyYaw);
        List<SeatInfo> seats = VehicleData.compute(vehicle).seats();
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

    public double getOffsetYaw() {
        return offsetYaw;
    }

    public double getOffsetPitch() {
        return offsetPitch;
    }

    public OffsetPhase getPhase() {
        return phase;
    }

    public double getEdgeBrakeState() {
        return edgeBrakeState;
    }

    public double getEdgeSpeedScale() {
        return edgeSpeedScale;
    }

    public double getExtraDistance() {
        return extraDistance;
    }

    public double getLastCamYaw() {
        return lastCamYaw;
    }

    public double getLastCamPitch() {
        return lastCamPitch;
    }

    public double getLastCamRoll() {
        return lastCamRoll;
    }

    public double getLastStockRoll() {
        return lastStockRoll;
    }

    public double getLastOrbitRoll() {
        return lastOrbitRoll;
    }

    public double getAgility() {
        return agility;
    }

    public double getActivity() {
        return activity;
    }

    public double getSmoothedYawRate() {
        return smoothedYawRate;
    }

    public double getSmoothedPitchRate() {
        return smoothedPitchRate;
    }

    public double getSmoothedRollRate() {
        return smoothedRollRate;
    }
}
