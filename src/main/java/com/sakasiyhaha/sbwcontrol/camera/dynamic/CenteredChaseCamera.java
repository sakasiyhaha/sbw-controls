package com.sakasiyhaha.sbwcontrol.camera.dynamic;

import com.atsuishio.superbwarfare.data.vehicle.subdata.VehicleType;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.event.ClientMouseHandler;
import com.sakasiyhaha.sbwcontrol.camera.view.CameraAimMode;
import com.sakasiyhaha.sbwcontrol.camera.view.CameraViewMode;
import com.sakasiyhaha.sbwcontrol.camera.view.FixedWingCameraBackend;
import com.sakasiyhaha.sbwcontrol.client.CameraAccess;
import com.sakasiyhaha.sbwcontrol.client.ClientControlHandler;
import com.sakasiyhaha.sbwcontrol.config.ClientConfig;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ViewportEvent;
import org.joml.Matrix4d;
import org.joml.Vector3d;
import org.joml.Vector4d;

/**
 * 正交固定居中飞行相机。
 *
 * <p>三个通道严格分开：
 * <ul>
 *   <li>YawChannel：只读取偏航输入；</li>
 *   <li>PitchChannel：固定为载具 pitch；</li>
 *   <li>RollChannel：只读取滚转输入。</li>
 * </ul>
 *
 * <p>SW 的 GameRendererMixin 会先按相机 yaw 往 pose stack 上加一个 stock roll；
 * 这里先用“实际进入相机 setup 的 yaw”计算 stockRoll，再用 event.roll 抵消。
 *
 * <p>SW 的 freeCameraYaw/freeCameraPitch 是鼠标驱动的原生动态相机。
 * 本基线会在每帧结束时清零，避免它继续流入 CameraMixin / GameRendererMixin。
 */
@OnlyIn(Dist.CLIENT)
public final class CenteredChaseCamera {

    private final Spring1D yawChannel = new Spring1D(0.0D);
    private final Spring1D rollChannel = new Spring1D(0.0D);
    private final Spring1D pitchChannel = new Spring1D(0.0D);
    private final CameraDistanceController distanceController = new CameraDistanceController();
    private final Spring1D independentCollisionDistance = new Spring1D(0.0D);
    private boolean independentCollisionInitialized;
    private boolean lastDynamic = true;
    private double lastAppliedYaw = Double.NaN;
    private double lastAppliedPitch = Double.NaN;
    private double lastAppliedRoll = Double.NaN;
    private double lastYawOffset;
    private double lastRollOffset;
    private double lastPitchOffset;

    public double getYawOffset() {
        return lastYawOffset;
    }

    public double getRollOffset() {
        return lastRollOffset;
    }

    public double getPitchOffset() {
        return lastPitchOffset;
    }

    public void reset() {
        yawChannel.reset(0.0D);
        rollChannel.reset(0.0D);
        pitchChannel.reset(0.0D);
        distanceController.reset();
        independentCollisionDistance.reset(0.0D);
        independentCollisionInitialized = false;
        lastDynamic = true;
        lastAppliedYaw = Double.NaN;
        lastAppliedPitch = Double.NaN;
        lastAppliedRoll = Double.NaN;
        lastYawOffset = 0.0D;
        lastRollOffset = 0.0D;
        lastPitchOffset = 0.0D;
    }

    public void apply(ViewportEvent.ComputeCameraAngles event, VehicleEntity vehicle, int seat,
                      float partial, CameraViewMode mode) {
        Minecraft mc = Minecraft.getInstance();
        // 使用最短角插值，避免 roll 跨 ±180° 时 lerp 穿过 0° 造成抽搐。
        double vehicleYaw = rotLerp(partial, vehicle.getYaw(0.0F), vehicle.getYaw(1.0F));
        double vehiclePitch = rotLerp(partial, vehicle.getPitch(0.0F), vehicle.getPitch(1.0F));
        double bodyRoll = rotLerp(partial, vehicle.getRoll(0.0F), vehicle.getRoll(1.0F));
        double dt = Math.max(1.0E-3D, mc.getDeltaFrameTime());
        boolean dynamic = mode == CameraViewMode.CENTERED_DYNAMIC;
        boolean followRoll = mode == CameraViewMode.CENTERED_FOLLOW;
        boolean hardFollow = followRoll && ClientConfig.CAMERA_FOLLOW_HARD_ROTATION.get();

        if (!dynamic && lastDynamic) {
            yawChannel.reset(0.0D);
            rollChannel.reset(0.0D);
            pitchChannel.reset(0.0D);
        }
        lastDynamic = dynamic;

        // Yaw/Roll/Pitch 三个正交通道。输入已经按控制流派映射过：
        // ROLL_BIRD 时 roll=鼠标X、yaw=A/D；YAW_BIRD 时 yaw=鼠标X、roll=A/D。
        double yawDirection = ClientConfig.CAMERA_INVERT_YAW.get() ? 1.0D : -1.0D;
        // 滚转：关闭反转时使用正向动态，打开反转时取负向。
        double rollDirection = ClientConfig.CAMERA_INVERT_ROLL.get() ? -1.0D : 1.0D;
        double pitchDirection = ClientConfig.CAMERA_INVERT_PITCH.get() ? 1.0D : -1.0D;
        double yawTarget = dynamic
                ? ClientControlHandler.getYaw() * ClientConfig.CAMERA_CHANNEL_YAW_GAIN.get() * yawDirection
                : 0.0D;
        double rollTarget = dynamic
                ? ClientControlHandler.getRoll() * ClientConfig.CAMERA_CHANNEL_ROLL_GAIN.get() * rollDirection
                : 0.0D;
        double pitchTarget = dynamic
                ? ClientControlHandler.getPitch() * ClientConfig.CAMERA_CHANNEL_PITCH_GAIN.get() * pitchDirection
                : 0.0D;
        double damping = ClientConfig.CAMERA_SPRING_DAMPING.get();
        double yawOffset = yawChannel.step(yawTarget,
                ClientConfig.CAMERA_CHANNEL_YAW_LAG.get(), damping, 40.0D, dt);
        double rollOffset = rollChannel.step(rollTarget,
                ClientConfig.CAMERA_CHANNEL_ROLL_LAG.get(), damping, 30.0D, dt);
        double pitchOffset = pitchChannel.step(pitchTarget,
                ClientConfig.CAMERA_CHANNEL_PITCH_LAG.get(), damping, 30.0D, dt);
        lastYawOffset = yawOffset;
        lastRollOffset = rollOffset;
        lastPitchOffset = pitchOffset;

        // 关键：SW 的 stock roll 使用的是 Camera.setup 后的实际相机 yaw，
        // 其中包含 SW 的 freeCameraYaw。必须用这个值抵消，否则鼠标 freeCamera 会残留。
        // SW 的 stock roll 只在其 GameRendererMixin 条件
        // (!mainCamera.isDetached() || zoomVehicle) 成立时加到 pose stack。
        // 第三人称 detached=true，stock roll 不会执行；因此这里不能减去 stockRoll。
        // 跟随：完全跟随飞机滚转；动态：地面为正，只叠加滚转通道的小幅动态。
        double desiredRoll = followRoll ? bodyRoll + rollOffset : rollOffset;
        double eventRoll = Mth.wrapDegrees(desiredRoll);

        // 停止 SW 原生鼠标动态相机继续流入下一帧的 Camera.setup / stockRoll。
        ClientMouseHandler.freeCameraYaw = 0.0D;
        ClientMouseHandler.freeCameraPitch = 0.0D;

        double finalYaw = vehicleYaw + yawOffset;
        double finalPitch = vehiclePitch + pitchOffset;

        boolean independentFixedWing = vehicle.getVehicleType() == VehicleType.AIRPLANE
                && ClientConfig.CAMERA_FIXED_WING_BACKEND.get() == FixedWingCameraBackend.INDEPENDENT;

        // 先计算机位，再根据最终相机位置决定瞄准角。
        try {
            Camera camera = mc.gameRenderer.getMainCamera();
            Vec3 target;
            if (independentFixedWing) {
                // 完全独立机位：不读取卓越前线座位相机位置。
                // 目标点必须使用 partial tick 插值后的位置；直接使用 AABB center 会按 20Hz 跳动。
                double targetX = Mth.lerp(partial, vehicle.xo, vehicle.getX());
                double targetY = Mth.lerp(partial, vehicle.yo, vehicle.getY())
                        + vehicle.getBbHeight() * 0.5D;
                double targetZ = Mth.lerp(partial, vehicle.zo, vehicle.getZ());
                target = new Vec3(targetX, targetY, targetZ);

                // 自己构建姿态矩阵，避免 SW getClientVehicleTransform 中混入 freeCamera 抖动。
                Matrix4d orientation = new Matrix4d();
                orientation.rotateY(Math.toRadians(-vehicleYaw));
                orientation.rotateX(Math.toRadians(vehiclePitch));
                if (followRoll) {
                    // 跟随模式：机位也随飞机滚转绕机体中心旋转。
                    orientation.rotateZ(Math.toRadians(bodyRoll));
                }
                double distance = ClientConfig.CAMERA_FIXED_WING_DISTANCE.get();
                double height = ClientConfig.CAMERA_FIXED_WING_HEIGHT.get();
                Vector3d offset = orientation.transformDirection(new Vector3d(0.0D, height, -distance));
                Vec3 offsetVec = new Vec3(offset.x, offset.y, offset.z);
                double wantedDistance = offsetVec.length();
                Vec3 direction = wantedDistance > 1.0E-5D ? offsetVec.scale(1.0D / wantedDistance) : Vec3.ZERO;
                double allowedDistance = wantedDistance;

                if (ClientConfig.CAMERA_DISTANCE_COLLISION.get() && wantedDistance > 1.0E-5D) {
                    Vec3 collided = target.add(direction.scale(wantedDistance));
                    HitResult hit = vehicle.level().clip(new ClipContext(
                            target, collided, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, vehicle));
                    if (hit.getType() == HitResult.Type.BLOCK) {
                        allowedDistance = Math.max(0.5D, hit.getLocation().distanceTo(target) - 0.35D);
                    }
                }
                // 碰撞距离做短平滑，避免地形边缘 hit/no-hit 每帧跳变导致相机抖动。
                double smoothedDistance;
                if (!independentCollisionInitialized) {
                    independentCollisionDistance.reset(allowedDistance);
                    independentCollisionInitialized = true;
                    smoothedDistance = allowedDistance;
                } else {
                    smoothedDistance = independentCollisionDistance.step(
                            allowedDistance, 0.08D, 1.0D, 180.0D, Math.max(1.0E-3D, dt));
                }
                Vec3 desired = target.add(direction.scale(smoothedDistance));
                CameraAccess.setPosition(camera, desired.x, desired.y, desired.z);
            } else {
                Vec3 position = camera.getPosition();
                Matrix4d transform = vehicle.getClientVehicleTransform(partial);
                Matrix4d inverse = new Matrix4d(transform).invert();
                Vector4d local = inverse.transform(new Vector4d(
                        position.x, position.y, position.z, 1.0D));
                local.x = 0.0D;
                if (hardFollow && vehicle.getVehicleType() == VehicleType.AIRPLANE) {
                    local.y = Math.max(0.0D, ClientConfig.CAMERA_HARD_FOLLOW_HEIGHT.get());
                }
                boolean helicopter = vehicle.getVehicleType() == VehicleType.HELICOPTER;
                distanceController.apply(camera, vehicle, transform, local, helicopter, partial, dt, hardFollow);
                Vector4d targetWorld = transform.transform(new Vector4d(0.0D, 0.0D, 0.0D, 1.0D));
                target = new Vec3(targetWorld.x, targetWorld.y, targetWorld.z);
            }

            if (ClientConfig.CAMERA_AIM_MODE.get() == CameraAimMode.CENTER
                    && !(followRoll && hardFollow)) {
                Vec3 cameraPos = camera.getPosition();
                Vec3 direction = target.subtract(cameraPos);
                double horizontal = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
                if (horizontal > 1.0E-4D || Math.abs(direction.y) > 1.0E-4D) {
                    finalYaw = Math.toDegrees(-Math.atan2(direction.x, direction.z)) + yawOffset;
                    finalPitch = Math.toDegrees(-Math.atan2(direction.y, horizontal)) + pitchOffset;
                }
            }
        } catch (Throwable ignored) {
            // 位置计算失败时保留 SW 原机位。
        }

        event.setYaw((float) unwrapNear(finalYaw, lastAppliedYaw));
        event.setPitch((float) unwrapNear(finalPitch, lastAppliedPitch));
        // 第三人称直接输出最终相机 roll；跨 ±180° 时保持连续，避免翻转瞬间抽搐。
        double appliedRoll = unwrapNear(eventRoll, lastAppliedRoll);
        event.setRoll((float) appliedRoll);
        lastAppliedYaw = event.getYaw();
        lastAppliedPitch = event.getPitch();
        lastAppliedRoll = appliedRoll;
    }

    private static double unwrapNear(double value, double reference) {
        return Double.isNaN(reference) ? value : reference + Mth.wrapDegrees(value - reference);
    }

    private static double rotLerp(float partial, double from, double to) {
        return from + Mth.wrapDegrees(to - from) * partial;
    }
}
