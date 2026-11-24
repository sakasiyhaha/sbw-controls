package com.sakasiyhaha.sbwcontrol.camera.dynamic;

import com.sakasiyhaha.sbwcontrol.camera.offset.OffsetPhase;
import com.sakasiyhaha.sbwcontrol.config.ClientConfig;
import net.minecraft.util.Mth;

/**
 * 新动态相机的 yaw/pitch 偏移管线。
 *
 * <p>职责：
 * <ul>
 *   <li>目标偏移收缩到边缘误差圆；</li>
 *   <li>NORMAL 状态下的弹簧与单侧边缘制动；</li>
 *   <li>无输入后延迟进入 RETURN；</li>
 *   <li>RETURN 使用速度连续的 Hermite 轨迹，时长按距离/最大速度决定；</li>
 *   <li>HOLD / SETTLE 只处理中心误差圆残余。</li>
 * </ul>
 *
 * <p>该管线不处理滚转、机位和 FOV。
 */
public final class OffsetPipeline {

    private static final double YAW_LIMIT = 12.0D;
    private static final double PITCH_LIMIT = 8.0D;
    private static final double TWO_PI = Math.PI * 2.0D;

    public record Input(double targetYaw, double targetPitch,
                        boolean rawInputActive, double dt,
                        double speed, double activity, boolean firstPerson,
                        double lagYaw, double lagPitch, double springDamping) {
    }

    public record Output(double yaw, double pitch, double yawVelocity, double pitchVelocity,
                         OffsetPhase phase, double edgeBrakeState, double edgeSpeedScale) {
    }

    private final Spring1D yaw = new Spring1D(0.0D);
    private final Spring1D pitch = new Spring1D(0.0D);

    private OffsetPhase phase = OffsetPhase.NORMAL;
    private double neutralTime;

    private double returnElapsed;
    private double returnDuration;
    private double returnStartYaw;
    private double returnStartPitch;
    private double returnStartYawVelocity;
    private double returnStartPitchVelocity;
    private double returnTargetYaw;
    private double returnTargetPitch;
    private boolean returnToCircle;

    private double holdElapsed;
    private double settleElapsed;

    private double edgeBrakeState;
    private double edgeSpeedScale = 1.0D;
    private boolean edgeBrakeLatched;

    public void reset() {
        yaw.reset(0.0D);
        pitch.reset(0.0D);
        phase = OffsetPhase.NORMAL;
        neutralTime = 0.0D;
        returnElapsed = 0.0D;
        returnDuration = 0.0D;
        returnStartYaw = 0.0D;
        returnStartPitch = 0.0D;
        returnStartYawVelocity = 0.0D;
        returnStartPitchVelocity = 0.0D;
        returnTargetYaw = 0.0D;
        returnTargetPitch = 0.0D;
        returnToCircle = false;
        holdElapsed = 0.0D;
        settleElapsed = 0.0D;
        edgeBrakeState = 0.0D;
        edgeSpeedScale = 1.0D;
        edgeBrakeLatched = false;
    }

    public void seed(double offsetYaw, double offsetPitch) {
        yaw.set(offsetYaw, 0.0D);
        pitch.set(offsetPitch, 0.0D);
        phase = OffsetPhase.NORMAL;
        neutralTime = 0.0D;
        edgeBrakeState = 0.0D;
        edgeSpeedScale = 1.0D;
        edgeBrakeLatched = false;
    }

    public Output update(Input input) {
        if (input.firstPerson()) {
            phase = OffsetPhase.NORMAL;
            yaw.step(input.targetYaw(), input.lagYaw(), input.springDamping(), 180.0D, input.dt());
            pitch.step(input.targetPitch(), input.lagPitch(), input.springDamping(), 120.0D, input.dt());
            return output();
        }

        double dt = input.dt();
        double targetYaw = input.targetYaw();
        double targetPitch = input.targetPitch();

        // 归一化边缘误差圆：动态余量 + 径向目标收缩。
        double speedNorm = Mth.clamp(input.speed() / 3.0D, 0.0D, 1.0D);
        double activity = Mth.clamp(input.activity(), 0.0D, 1.0D);
        double edgeMargin = Mth.clamp(ClientConfig.CAMERA_EDGE_RADIUS.get()
                        + ClientConfig.CAMERA_EDGE_SPEED_GAIN.get() * speedNorm
                        + ClientConfig.CAMERA_EDGE_ACTIVITY_GAIN.get() * activity,
                0.0D, 0.5D);
        double edgeMax = Math.max(0.05D, 1.0D - edgeMargin);
        double targetRadius = Math.hypot(targetYaw / YAW_LIMIT, targetPitch / PITCH_LIMIT);
        if (targetRadius > edgeMax && targetRadius > 1.0E-6D) {
            double scale = edgeMax / targetRadius;
            targetYaw *= scale;
            targetPitch *= scale;
        }

        double offsetX = yaw.value() / YAW_LIMIT;
        double offsetY = pitch.value() / PITCH_LIMIT;
        double radius = Math.hypot(offsetX, offsetY);
        double vx = yaw.velocity() / YAW_LIMIT;
        double vy = pitch.velocity() / PITCH_LIMIT;
        double radialVelocity = radius > 1.0E-6D ? (offsetX * vx + offsetY * vy) / radius : 0.0D;
        boolean outward = radialVelocity > 0.0D;
        double brakeStart = Math.max(0.0D, edgeMax - ClientConfig.CAMERA_EDGE_BRAKE_ZONE.get());
        double brakeStop = Math.max(0.0D, brakeStart - ClientConfig.CAMERA_EDGE_BRAKE_HYSTERESIS.get());
        if (outward && radius >= brakeStart) {
            edgeBrakeLatched = true;
        } else if (radius <= brakeStop) {
            edgeBrakeLatched = false;
        }
        double targetBrake = 0.0D;
        if (outward && edgeBrakeLatched) {
            double predicted = radius + radialVelocity * ClientConfig.CAMERA_EDGE_LOOKAHEAD.get();
            double t = Mth.clamp((predicted - brakeStart) / Math.max(1.0E-4D, edgeMax - brakeStart),
                    0.0D, 1.0D);
            targetBrake = t * t * (3.0D - 2.0D * t);
        }
        double releaseTime = Math.max(0.05D, ClientConfig.CAMERA_EDGE_BRAKE_RELEASE.get());
        edgeBrakeState += (targetBrake - edgeBrakeState) * (1.0D - Math.exp(-dt / releaseTime));
        edgeBrakeState = Mth.clamp(edgeBrakeState, 0.0D, 1.0D);
        double edgeExtraYaw = (2.0D / Math.max(0.02D, input.lagYaw()))
                * ClientConfig.CAMERA_EDGE_BRAKE.get() * edgeBrakeState;
        double edgeExtraPitch = (2.0D / Math.max(0.02D, input.lagPitch()))
                * ClientConfig.CAMERA_EDGE_BRAKE.get() * edgeBrakeState;
        edgeSpeedScale = 1.0D - ClientConfig.CAMERA_EDGE_SPEED_LIMIT.get() * edgeBrakeState;

        double targetMagnitude = Math.hypot(targetYaw, targetPitch);
        double offsetMagnitude = Math.hypot(yaw.value(), pitch.value());
        boolean inputActive = input.rawInputActive() || targetMagnitude > 0.08D;

        boolean recenterEnabled = ClientConfig.CAMERA_RECENTER_ENABLED.get();
        double delay = Math.max(0.0D, ClientConfig.CAMERA_RECENTER_DELAY.get());
        double centerRadius = Math.max(0.0D, ClientConfig.CAMERA_RECENTER_RADIUS.get()
                + ClientConfig.CAMERA_RECENTER_SPEED_GAIN.get() * speedNorm
                + ClientConfig.CAMERA_RECENTER_ACTIVITY_GAIN.get() * activity
                + ClientConfig.CAMERA_RECENTER_HYSTERESIS.get());

        // 输入优先：任何输入都立即终止 RETURN/HOLD/SETTLE。
        if (inputActive && phase != OffsetPhase.NORMAL) {
            phase = OffsetPhase.NORMAL;
            neutralTime = 0.0D;
        }

        if (phase != OffsetPhase.NORMAL) {
            return updateSpecialPhase(input, dt, targetYaw, targetPitch, targetMagnitude,
                    offsetMagnitude, centerRadius, recenterEnabled);
        }

        if (!input.rawInputActive() && targetMagnitude < 0.05D) {
            neutralTime += dt;
        } else {
            neutralTime = 0.0D;
        }

        if (!inputActive && targetMagnitude < 0.05D && neutralTime >= delay) {
            if (offsetMagnitude > centerRadius) {
                startReturn(input, dt, centerRadius, recenterEnabled);
                return updateSpecialPhase(input, dt, targetYaw, targetPitch, targetMagnitude,
                        offsetMagnitude, centerRadius, recenterEnabled);
            }
            if (recenterEnabled && offsetMagnitude > 1.0E-4D) {
                phase = OffsetPhase.HOLD;
                holdElapsed = 0.0D;
                yaw.set(yaw.value(), 0.0D);
                pitch.set(pitch.value(), 0.0D);
                return output();
            }
        }

        // NORMAL 弹簧 + 边缘单侧制动。
        yaw.step(targetYaw, input.lagYaw(), input.springDamping(), 180.0D, dt,
                edgeExtraYaw, edgeSpeedScale);
        pitch.step(targetPitch, input.lagPitch(), input.springDamping(), 120.0D, dt,
                edgeExtraPitch, edgeSpeedScale);
        guard();
        phase = (!input.rawInputActive() && targetMagnitude < 0.05D
                && neutralTime < delay) ? OffsetPhase.NEUTRAL_WAIT : OffsetPhase.NORMAL;
        return output();
    }

    private Output updateSpecialPhase(Input input, double dt,
                                      double targetYaw, double targetPitch,
                                      double targetMagnitude, double offsetMagnitude,
                                      double centerRadius, boolean recenterEnabled) {
        if (phase == OffsetPhase.RETURN) {
            returnElapsed += dt;
            double t = returnDuration <= 1.0E-6D ? 1.0D : Mth.clamp(returnElapsed / returnDuration, 0.0D, 1.0D);
            double t2 = t * t;
            double t3 = t2 * t;
            double h00 = 2.0D * t3 - 3.0D * t2 + 1.0D;
            double h10 = t3 - 2.0D * t2 + t;
            double h01 = -2.0D * t3 + 3.0D * t2;
            double h11 = t3 - t2;
            double p0y = returnStartYaw;
            double p1y = returnTargetYaw;
            double p0p = returnStartPitch;
            double p1p = returnTargetPitch;
            double valueY = h00 * p0y + h10 * returnDuration * returnStartYawVelocity + h01 * p1y;
            double valueP = h00 * p0p + h10 * returnDuration * returnStartPitchVelocity + h01 * p1p;
            double dY = (6.0D * t2 - 6.0D * t) * p0y
                    + (3.0D * t2 - 4.0D * t + 1.0D) * returnDuration * returnStartYawVelocity
                    + (-6.0D * t2 + 6.0D * t) * p1y;
            double dP = (6.0D * t2 - 6.0D * t) * p0p
                    + (3.0D * t2 - 4.0D * t + 1.0D) * returnDuration * returnStartPitchVelocity
                    + (-6.0D * t2 + 6.0D * t) * p1p;
            yaw.set(valueY, dY / Math.max(returnDuration, 1.0E-4D));
            pitch.set(valueP, dP / Math.max(returnDuration, 1.0E-4D));
            if (t >= 1.0D) {
                yaw.set(returnTargetYaw, 0.0D);
                pitch.set(returnTargetPitch, 0.0D);
                if (recenterEnabled && returnToCircle) {
                    phase = OffsetPhase.HOLD;
                    holdElapsed = 0.0D;
                } else {
                    phase = OffsetPhase.NORMAL;
                    neutralTime = 0.0D;
                }
            }
            return output();
        }

        if (phase == OffsetPhase.HOLD) {
            if (input.rawInputActive() || targetMagnitude > 0.08D) {
                phase = OffsetPhase.NORMAL;
                return output();
            }
            holdElapsed += dt;
            if (holdElapsed > ClientConfig.CAMERA_RECENTER_IDLE_SETTLE.get()) {
                phase = OffsetPhase.SETTLE;
                settleElapsed = 0.0D;
            }
            return output();
        }

        if (phase == OffsetPhase.SETTLE) {
            if (input.rawInputActive() || targetMagnitude > 0.08D) {
                phase = OffsetPhase.NORMAL;
                return output();
            }
            settleElapsed += dt;
            double tau = Math.max(1.5D, ClientConfig.CAMERA_RECENTER_TIME.get() * 8.0D);
            double factor = Math.exp(-dt / tau);
            yaw.set(yaw.value() * factor, 0.0D);
            pitch.set(pitch.value() * factor, 0.0D);
            if (Math.hypot(yaw.value(), pitch.value()) < 0.01D) {
                yaw.reset(0.0D);
                pitch.reset(0.0D);
                phase = OffsetPhase.NORMAL;
            }
            return output();
        }

        return output();
    }

    private void startReturn(Input input, double dt, double centerRadius, boolean recenterEnabled) {
        double offsetMagnitude = Math.hypot(yaw.value(), pitch.value());
        if (offsetMagnitude <= 1.0E-5D) {
            phase = OffsetPhase.NORMAL;
            return;
        }
        double dirYaw = yaw.value() / offsetMagnitude;
        double dirPitch = pitch.value() / offsetMagnitude;
        double targetRadius = recenterEnabled
                ? Math.max(0.0D, centerRadius - ClientConfig.CAMERA_RECENTER_HYSTERESIS.get())
                : 0.0D;
        returnStartYaw = yaw.value();
        returnStartPitch = pitch.value();
        returnStartYawVelocity = yaw.velocity();
        returnStartPitchVelocity = pitch.velocity();
        returnTargetYaw = dirYaw * targetRadius;
        returnTargetPitch = dirPitch * targetRadius;
        returnToCircle = recenterEnabled && targetRadius > 1.0E-4D;
        double distance = Math.hypot(returnTargetYaw - returnStartYaw, returnTargetPitch - returnStartPitch);
        double maxSpeed = Math.max(5.0D, ClientConfig.CAMERA_RECENTER_MAX_SPEED.get());
        double minTime = Math.max(0.05D, ClientConfig.CAMERA_RECENTER_TIME.get());
        double maxTime = Math.max(minTime, ClientConfig.CAMERA_RECENTER_MAX_TIME.get());
        returnDuration = Mth.clamp(Math.max(minTime, distance / maxSpeed), minTime, maxTime);
        returnElapsed = 0.0D;
        phase = OffsetPhase.RETURN;
    }

    private void guard() {
        double y = yaw.value() / YAW_LIMIT;
        double p = pitch.value() / PITCH_LIMIT;
        double radius = Math.hypot(y, p);
        if (radius > 1.0D) {
            double scale = 1.0D / radius;
            yaw.set(yaw.value() * scale, yaw.velocity() * scale);
            pitch.set(pitch.value() * scale, pitch.velocity() * scale);
        }
    }

    private Output output() {
        return new Output(yaw.value(), pitch.value(), yaw.velocity(), pitch.velocity(),
                phase, edgeBrakeState, edgeSpeedScale);
    }
}
