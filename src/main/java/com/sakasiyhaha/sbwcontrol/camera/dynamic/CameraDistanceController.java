package com.sakasiyhaha.sbwcontrol.camera.dynamic;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.sakasiyhaha.sbwcontrol.client.CameraAccess;
import com.sakasiyhaha.sbwcontrol.config.ClientConfig;
import net.minecraft.client.Camera;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4d;
import org.joml.Vector4d;

/**
 * TPS 相机距离与碰撞。
 *
 * <p>通过卓越前线座位机位的局部坐标计算“基础距离”，再叠加额外距离
 * 或替换为目标总距离；最后按需做一次方块射线防穿模。
 */
@OnlyIn(Dist.CLIENT)
public final class CameraDistanceController {

    private final Spring1D distanceSmooth = new Spring1D(0.0D);
    private boolean initialized;
    private static double lastBaseDistance;
    private static double lastEffectiveDistance;

    public static double getLastBaseDistance() {
        return lastBaseDistance;
    }

    public static double getLastEffectiveDistance() {
        return lastEffectiveDistance;
    }

    public void reset() {
        initialized = false;
    }

    public void apply(Camera camera, VehicleEntity vehicle, Matrix4d transform,
                      Vector4d local, boolean helicopter, float partial, double dt,
                      boolean instant) {
        double baseDistance = Math.sqrt(local.y * local.y + local.z * local.z);
        double extra = helicopter
                ? ClientConfig.CAMERA_HELICOPTER_DISTANCE_EXTRA.get()
                : ClientConfig.CAMERA_AIRPLANE_DISTANCE_EXTRA.get();
        double total = helicopter
                ? ClientConfig.CAMERA_HELICOPTER_DISTANCE_TOTAL.get()
                : ClientConfig.CAMERA_AIRPLANE_DISTANCE_TOTAL.get();

        double targetDistance = switch (ClientConfig.CAMERA_DISTANCE_MODE.get()) {
            case EXTRA -> baseDistance + extra;
            case TOTAL -> total;
        };
        // 至少留出高度分量，避免目标距离小于相机高度时开根号无效。
        targetDistance = Math.max(targetDistance, Math.abs(local.y) + 0.25D);

        if (!initialized || instant) {
            distanceSmooth.reset(targetDistance);
            initialized = true;
        } else {
            targetDistance = distanceSmooth.step(targetDistance,
                    ClientConfig.CAMERA_DISTANCE_SMOOTH.get(),
                    1.0D, 120.0D, Math.max(1.0E-3D, dt));
        }

        double vertical = local.y;
        double horizontal = Math.sqrt(Math.max(0.0D, targetDistance * targetDistance - vertical * vertical));
        double sign = local.z < 0.0D ? -1.0D : 1.0D;
        local.z = sign * horizontal;

        Vector4d targetWorld = transform.transform(new Vector4d(local.x, local.y, local.z, 1.0D));
        Vec3 finalPos = new Vec3(targetWorld.x, targetWorld.y, targetWorld.z);

        if (ClientConfig.CAMERA_DISTANCE_COLLISION.get()) {
            Vec3 start = vehicle.position().add(0.0D, vehicle.getBbHeight() * 0.5D, 0.0D);
            HitResult hit = vehicle.level().clip(new ClipContext(
                    start, finalPos, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, vehicle));
            if (hit.getType() == HitResult.Type.BLOCK) {
                Vec3 direction = finalPos.subtract(start);
                if (direction.lengthSqr() > 1.0E-6D) {
                    finalPos = hit.getLocation().subtract(direction.normalize().scale(0.35D));
                }
            }
        }

        lastBaseDistance = baseDistance;
        lastEffectiveDistance = vehicle.position().distanceTo(finalPos);
        CameraAccess.setPosition(camera, finalPos.x, finalPos.y, finalPos.z);
    }
}
