package com.sakasiyhaha.sbwcontrol.camera.dynamic;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.sakasiyhaha.sbwcontrol.client.CameraAccess;
import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4d;
import org.joml.Vector4d;

/** 相机机位、动态距离、横向居中与位置弹簧的独立管线。 */
public final class PositionPipeline {

    public record Input(Camera camera, VehicleEntity vehicle, float partial,
                        double baseYaw, double basePitch, double adsScale,
                        double distanceGain, double centerFactor, double positionLag,
                        boolean rollFollow, double orbitRoll, double agility,
                        double springDamping, double dt, boolean firstPerson) {
    }

    public record Output(Vec3 position, double extraDistance, boolean moved) {
    }

    private final Spring1D relX = new Spring1D(0.0D);
    private final Spring1D relY = new Spring1D(0.0D);
    private final Spring1D relZ = new Spring1D(0.0D);
    private boolean relInitialized;

    public void reset() {
        relX.reset(0.0D);
        relY.reset(0.0D);
        relZ.reset(0.0D);
        relInitialized = false;
    }

    public Output update(Input input) {
        if (input.firstPerson()) {
            return new Output(input.camera().getPosition(), 0.0D, false);
        }
        Vec3 position = input.camera().getPosition();
        double desiredX = position.x;
        double desiredY = position.y;
        double desiredZ = position.z;
        double extraDistance = 0.0D;
        boolean moved = false;

        Matrix4d transform = null;
        Vector4d origin = null;
        try {
            transform = input.vehicle().getClientVehicleTransform(input.partial());
            origin = transform.transform(new Vector4d(0.0D, 0.0D, 0.0D, 1.0D));
        } catch (Throwable ignored) {
            transform = null;
        }

        if (input.centerFactor() > 0.0D && transform != null) {
            try {
                Matrix4d inverse = new Matrix4d(transform).invert();
                Vector4d local = inverse.transform(new Vector4d(
                        position.x, position.y, position.z, 1.0D));
                double originalX = local.x;
                local.x *= (1.0D - input.centerFactor());
                if (Math.abs(originalX) > 1.0E-3D) {
                    Vector4d world = transform.transform(local);
                    desiredX = world.x;
                    desiredY = world.y;
                    desiredZ = world.z;
                    moved = true;
                }
            } catch (Throwable ignored) {
                // 位置变换失败时保持 SW 原机位。
            }
        }

        if (input.distanceGain() > 0.0D) {
            double speed = input.vehicle().getDeltaMovement().length();
            double extra = Math.min(2.0D, speed * input.distanceGain() * 0.5D) * input.adsScale();
            if (extra > 0.01D) {
                double yaw = Math.toRadians(input.baseYaw());
                double pitch = Math.toRadians(input.basePitch());
                desiredX += Math.sin(yaw) * Math.cos(pitch) * extra;
                desiredY += Math.sin(pitch) * extra;
                desiredZ -= Math.cos(yaw) * Math.cos(pitch) * extra;
                extraDistance = extra;
                moved = true;
            }
        }

        if (input.rollFollow() && Math.abs(input.orbitRoll()) > 1.0E-4D && transform != null) {
            try {
                Matrix4d inverse = new Matrix4d(transform).invert();
                Vector4d local = inverse.transform(new Vector4d(desiredX, desiredY, desiredZ, 1.0D));
                Matrix4d roll = new Matrix4d().rotateZ(Math.toRadians(input.orbitRoll()));
                Vector4d world = transform.transform(roll.transform(local));
                desiredX = world.x;
                desiredY = world.y;
                desiredZ = world.z;
                moved = true;
            } catch (Throwable ignored) {
                // 保持原机位。
            }
        }

        if (origin != null && input.positionLag() > 1.0E-4D) {
            Vec3 center = new Vec3(origin.x, origin.y, origin.z);
            Vec3 targetRel = new Vec3(desiredX, desiredY, desiredZ).subtract(center);
            if (!relInitialized) {
                relX.set(targetRel.x, 0.0D);
                relY.set(targetRel.y, 0.0D);
                relZ.set(targetRel.z, 0.0D);
                relInitialized = true;
            } else {
                double springTime = input.positionLag() / Math.max(0.01D, input.agility());
                relX.step(targetRel.x, springTime, input.springDamping(), 12.0D, input.dt());
                relY.step(targetRel.y, springTime, input.springDamping(), 8.0D, input.dt());
                relZ.step(targetRel.z, springTime, input.springDamping(), 12.0D, input.dt());
            }
            desiredX = center.x + relX.value();
            desiredY = center.y + relY.value();
            desiredZ = center.z + relZ.value();
            moved = true;
        } else {
            relInitialized = false;
        }

        Vec3 result = new Vec3(desiredX, desiredY, desiredZ);
        if (moved) {
            CameraAccess.setPosition(input.camera(), desiredX, desiredY, desiredZ);
        }
        return new Output(result, extraDistance, moved);
    }
}
