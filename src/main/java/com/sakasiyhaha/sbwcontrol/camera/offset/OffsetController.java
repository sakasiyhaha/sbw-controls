package com.sakasiyhaha.sbwcontrol.camera.offset;

import com.sakasiyhaha.sbwcontrol.config.ClientConfig;
import net.minecraft.util.Mth;

/**
 * yaw/pitch 动态偏移的唯一控制器。
 *
 * <p>职责边界：
 * <ul>
 *   <li>只处理目标偏移、回正、边缘误差圆、弹簧和绝对边界；</li>
 *   <li>不处理滚转、相机位置、FOV、座位或载具类型；</li>
 *   <li>同一帧只允许这里修改 offsetYaw / offsetPitch。</li>
 * </ul>
 *
 * <p>本类当前是“行为等价抽取”：默认参数和计算公式与抽取前的 CameraHandler 保持一致。
 */
public final class OffsetController {

    private static final double YAW_LIMIT = 12.0D;
    private static final double PITCH_LIMIT = 8.0D;

    private double offsetYaw;
    private double offsetPitch;
    private double yawVelocity;
    private double pitchVelocity;

    private boolean recenterActive;
    private boolean recenterHoldActive;
    private boolean recenterWasEnabled = true;
    private double recenterNeutralTime;
    private double recenterStartRadius;
    private double recenterAngle;
    private double recenterElapsed;
    private double recenterHoldElapsed;
    private double recenterHoldYaw;
    private double recenterHoldPitch;

    private double edgeBrakeState;
    private double edgeSpeedScale = 1.0D;
    private boolean edgeBrakeLatched;

    private OffsetPhase phase = OffsetPhase.NORMAL;

    /** 清空全部偏移状态。 */
    public void reset() {
        offsetYaw = 0.0D;
        offsetPitch = 0.0D;
        yawVelocity = 0.0D;
        pitchVelocity = 0.0D;
        recenterActive = false;
        recenterHoldActive = false;
        recenterWasEnabled = true;
        recenterNeutralTime = 0.0D;
        recenterStartRadius = 0.0D;
        recenterAngle = 0.0D;
        recenterElapsed = 0.0D;
        recenterHoldElapsed = 0.0D;
        recenterHoldYaw = 0.0D;
        recenterHoldPitch = 0.0D;
        edgeBrakeState = 0.0D;
        edgeSpeedScale = 1.0D;
        edgeBrakeLatched = false;
        phase = OffsetPhase.NORMAL;
    }

    /** 切换新旧引擎或刚性模式时，从当前可见偏移接续，不跳变。 */
    public void seed(double yaw, double pitch) {
        offsetYaw = yaw;
        offsetPitch = pitch;
        yawVelocity = 0.0D;
        pitchVelocity = 0.0D;
        recenterActive = false;
        recenterHoldActive = false;
        recenterNeutralTime = 0.0D;
        edgeBrakeState = 0.0D;
        edgeSpeedScale = 1.0D;
        edgeBrakeLatched = false;
        phase = OffsetPhase.NORMAL;
    }

    /**
     * 更新一帧偏移。
     *
     * @param targetYaw       已经包含 lead、soft limit、ADS、Sticky 归零的目标 yaw
     * @param targetPitch     同上，pitch
     * @param rawInputActive  本 tick 是否有原始输入
     * @param dt              渲染帧时间
     * @param speed           载具速度
     * @param activity        相机活动度（0~1）
     * @param firstPerson     是否第一人称
     * @param lagYaw          已按自适应调整后的 yaw 平滑时间
     * @param lagPitch        已按自适应调整后的 pitch 平滑时间
     * @param springDamping   弹簧阻尼比
     */
    public OffsetResult update(double targetYaw, double targetPitch,
                               boolean rawInputActive, double dt,
                               double speed, double activity, boolean firstPerson,
                               double lagYaw, double lagPitch, double springDamping) {
        if (firstPerson) {
            phase = OffsetPhase.NORMAL;
            offsetYaw = springStep(offsetYaw, targetYaw, dt, lagYaw, springDamping, 180.0D, 0.0D, 1.0D, true);
            offsetPitch = springStep(offsetPitch, targetPitch, dt, lagPitch, springDamping, 120.0D, 0.0D, 1.0D, false);
            return result();
        }

        double edgeMax = 1.0D;
        double edgeExtraYaw = 0.0D;
        double edgeExtraPitch = 0.0D;

        // 边缘误差圆：动态余量 + 归一化径向目标收缩。
        double speedNorm = Mth.clamp(speed / 3.0D, 0.0D, 1.0D);
        double edgeActivity = Mth.clamp(activity, 0.0D, 1.0D);
        double edgeMargin = Mth.clamp(ClientConfig.CAMERA_EDGE_RADIUS.get()
                        + ClientConfig.CAMERA_EDGE_SPEED_GAIN.get() * speedNorm
                        + ClientConfig.CAMERA_EDGE_ACTIVITY_GAIN.get() * edgeActivity,
                0.0D, 0.5D);
        edgeMax = Math.max(0.05D, 1.0D - edgeMargin);

        double ny = targetYaw / YAW_LIMIT;
        double np = targetPitch / PITCH_LIMIT;
        double targetRadius = Math.hypot(ny, np);
        if (targetRadius > edgeMax && targetRadius > 1.0E-6D) {
            double scale = edgeMax / targetRadius;
            targetYaw *= scale;
            targetPitch *= scale;
        }

        // 单侧预测制动：只有向外推时才产生目标制动量，实际用状态平滑释放。
        double oy = offsetYaw / YAW_LIMIT;
        double op = offsetPitch / PITCH_LIMIT;
        double offsetRadius = Math.hypot(oy, op);
        double brakeStart = Math.max(0.0D, edgeMax - ClientConfig.CAMERA_EDGE_BRAKE_ZONE.get());
        double brakeStop = Math.max(0.0D, brakeStart - ClientConfig.CAMERA_EDGE_BRAKE_HYSTERESIS.get());
        double targetBrake = 0.0D;
        if (offsetRadius > 1.0E-6D) {
            double vy = yawVelocity / YAW_LIMIT;
            double vp = pitchVelocity / PITCH_LIMIT;
            double radialVelocity = (oy * vy + op * vp) / offsetRadius;
            boolean outward = radialVelocity > 0.0D;
            if (outward && offsetRadius >= brakeStart) {
                edgeBrakeLatched = true;
            } else if (offsetRadius <= brakeStop) {
                edgeBrakeLatched = false;
            }
            if (outward && edgeBrakeLatched) {
                double predicted = offsetRadius + radialVelocity * ClientConfig.CAMERA_EDGE_LOOKAHEAD.get();
                double t = Mth.clamp((predicted - brakeStart)
                                / Math.max(1.0E-4D, edgeMax - brakeStart),
                        0.0D, 1.0D);
                targetBrake = t * t * (3.0D - 2.0D * t);
            }
        }
        double releaseTime = Math.max(0.05D, ClientConfig.CAMERA_EDGE_BRAKE_RELEASE.get());
        double brakeAlpha = 1.0D - Math.exp(-dt / releaseTime);
        edgeBrakeState += (targetBrake - edgeBrakeState) * brakeAlpha;
        edgeBrakeState = Mth.clamp(edgeBrakeState, 0.0D, 1.0D);
        edgeExtraYaw = (2.0D / Math.max(0.02D, lagYaw))
                * ClientConfig.CAMERA_EDGE_BRAKE.get() * edgeBrakeState;
        edgeExtraPitch = (2.0D / Math.max(0.02D, lagPitch))
                * ClientConfig.CAMERA_EDGE_BRAKE.get() * edgeBrakeState;
        edgeSpeedScale = 1.0D - ClientConfig.CAMERA_EDGE_SPEED_LIMIT.get() * edgeBrakeState;

        // 中心回正状态机：输入 > 回正；回正只在无输入的中立状态运行。
        double recenterTime = Math.max(0.05D, ClientConfig.CAMERA_RECENTER_TIME.get());
        boolean recenterEnabled = ClientConfig.CAMERA_RECENTER_ENABLED.get();
        double recenterDelay = Math.max(0.0D, ClientConfig.CAMERA_RECENTER_DELAY.get());
        if (!recenterEnabled) {
            if (recenterWasEnabled) {
                cancelRecenter();
            }
            recenterWasEnabled = false;
        } else {
            if (!recenterWasEnabled) {
                recenterNeutralTime = 0.0D;
            }
            recenterWasEnabled = true;
        }

        double centerRadius = ClientConfig.CAMERA_RECENTER_RADIUS.get();
        double hysteresis = ClientConfig.CAMERA_RECENTER_HYSTERESIS.get();
        double startRadius = Math.max(0.0D, centerRadius
                + ClientConfig.CAMERA_RECENTER_SPEED_GAIN.get() * speedNorm
                + ClientConfig.CAMERA_RECENTER_ACTIVITY_GAIN.get() * edgeActivity
                + hysteresis);
        double stopRadius = Math.max(0.0D, startRadius - hysteresis);
        double targetMagnitude = Math.hypot(targetYaw, targetPitch);
        double offsetMagnitude = Math.hypot(offsetYaw, offsetPitch);
        boolean targetWantsControl = targetMagnitude > 0.08D;
        boolean cancelRecenter = rawInputActive || targetWantsControl;

        if (!rawInputActive && targetMagnitude < 0.05D) {
            recenterNeutralTime += dt;
        } else {
            recenterNeutralTime = 0.0D;
        }

        if (recenterActive) {
            if (cancelRecenter) {
                cancelRecenter();
            } else {
                recenterElapsed += dt;
                double p = Math.min(1.0D, recenterElapsed / recenterTime);
                double ease = p * p * (3.0D - 2.0D * p);
                double previousYaw = offsetYaw;
                double previousPitch = offsetPitch;
                double r = recenterStartRadius * (1.0D - ease)
                        + Math.max(0.0D, stopRadius) * ease;
                offsetYaw = Math.cos(recenterAngle) * r;
                offsetPitch = Math.sin(recenterAngle) * r;
                yawVelocity = (offsetYaw - previousYaw) / Math.max(dt, 1.0E-4D);
                pitchVelocity = (offsetPitch - previousPitch) / Math.max(dt, 1.0E-4D);
                if (p >= 1.0D) {
                    recenterActive = false;
                    recenterHoldActive = true;
                    recenterHoldElapsed = 0.0D;
                    recenterHoldYaw = offsetYaw;
                    recenterHoldPitch = offsetPitch;
                    recenterNeutralTime = 0.0D;
                }
            }
        }

        if (!recenterActive && recenterHoldActive) {
            if (cancelRecenter) {
                cancelRecenter();
            } else {
                recenterHoldElapsed += dt;
                double idle = ClientConfig.CAMERA_RECENTER_IDLE_SETTLE.get();
                if (recenterHoldElapsed > idle) {
                    double settleTime = Math.max(0.15D, recenterTime * 2.0D);
                    double p = Math.min(1.0D, (recenterHoldElapsed - idle) / settleTime);
                    double ease = p * p * (3.0D - 2.0D * p);
                    offsetYaw = recenterHoldYaw * (1.0D - ease);
                    offsetPitch = recenterHoldPitch * (1.0D - ease);
                    yawVelocity = 0.0D;
                    pitchVelocity = 0.0D;
                    if (p >= 1.0D) {
                        offsetYaw = 0.0D;
                        offsetPitch = 0.0D;
                        recenterHoldActive = false;
                    }
                } else {
                    yawVelocity = 0.0D;
                    pitchVelocity = 0.0D;
                }
            }
        }

        if (!recenterActive && !recenterHoldActive) {
            if (recenterEnabled && !rawInputActive && targetMagnitude < 0.05D
                    && recenterNeutralTime >= recenterDelay) {
                if (offsetMagnitude > startRadius) {
                    recenterActive = true;
                    recenterStartRadius = offsetMagnitude;
                    recenterAngle = Math.atan2(offsetPitch, offsetYaw);
                    recenterElapsed = 0.0D;
                } else if (offsetMagnitude > 1.0E-4D) {
                    recenterHoldActive = true;
                    recenterHoldElapsed = 0.0D;
                    recenterHoldYaw = offsetYaw;
                    recenterHoldPitch = offsetPitch;
                    yawVelocity = 0.0D;
                    pitchVelocity = 0.0D;
                } else {
                    yawVelocity = 0.0D;
                    pitchVelocity = 0.0D;
                }
            } else {
                offsetYaw = springStep(offsetYaw, targetYaw, dt, lagYaw, springDamping, 180.0D,
                        edgeExtraYaw, edgeSpeedScale, true);
                offsetPitch = springStep(offsetPitch, targetPitch, dt, lagPitch, springDamping, 120.0D,
                        edgeExtraPitch, edgeSpeedScale, false);
            }
        }

        if (!recenterEnabled) {
            phase = OffsetPhase.NORMAL;
        } else if (recenterActive) {
            phase = OffsetPhase.RECENTER;
        } else if (recenterHoldActive) {
            phase = recenterHoldElapsed > ClientConfig.CAMERA_RECENTER_IDLE_SETTLE.get()
                    ? OffsetPhase.SETTLE : OffsetPhase.HOLD;
        } else if (!rawInputActive && targetMagnitude < 0.05D
                && recenterNeutralTime < recenterDelay) {
            phase = OffsetPhase.NEUTRAL_WAIT;
        } else {
            phase = OffsetPhase.NORMAL;
        }

        // 绝对圆形硬上限只做最后保险。
        double guardYaw = offsetYaw / YAW_LIMIT;
        double guardPitch = offsetPitch / PITCH_LIMIT;
        double guardRadius = Math.hypot(guardYaw, guardPitch);
        if (guardRadius > 1.0D) {
            double scale = 1.0D / guardRadius;
            offsetYaw *= scale;
            offsetPitch *= scale;
            yawVelocity *= scale;
            pitchVelocity *= scale;
        }
        return result();
    }

    private void cancelRecenter() {
        recenterActive = false;
        recenterHoldActive = false;
        recenterNeutralTime = 0.0D;
        yawVelocity = 0.0D;
        pitchVelocity = 0.0D;
    }

    private OffsetResult result() {
        return new OffsetResult(offsetYaw, offsetPitch, yawVelocity, pitchVelocity,
                phase, edgeBrakeState, edgeSpeedScale);
    }

    /** 与旧 CameraHandler 完全一致的固定子步弹簧，额外参数用于边缘制动。 */
    private double springStep(double current, double target, double dt,
                              double smoothTime, double dampingRatio, double maxSpeed,
                              double extraDamping, double maxSpeedScale, boolean yawAxis) {
        if (smoothTime <= 1.0E-4D || smoothTime < 0.02D) {
            if (yawAxis) {
                yawVelocity = 0.0D;
            } else {
                pitchVelocity = 0.0D;
            }
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
        double velocity = yawAxis ? yawVelocity : pitchVelocity;
        for (int i = 0; i < steps; i++) {
            double accel = (target - value) * stiffness - velocity * damping;
            velocity += accel * h;
            velocity = Mth.clamp(velocity, -speedLimit, speedLimit);
            value += velocity * h;
        }
        if (yawAxis) {
            yawVelocity = velocity;
        } else {
            pitchVelocity = velocity;
        }
        return value;
    }
}
