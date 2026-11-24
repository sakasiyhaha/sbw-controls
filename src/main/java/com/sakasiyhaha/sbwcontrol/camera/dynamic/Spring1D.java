package com.sakasiyhaha.sbwcontrol.camera.dynamic;

import net.minecraft.util.Mth;

/**
 * 单轴固定子步弹簧。
 *
 * <p>固定 120Hz 子步，避免低帧率时一帧直接弹到目标；帧率只影响子步数，不改变时间常数。
 */
public final class Spring1D {

    private double value;
    private double velocity;

    public Spring1D(double initialValue) {
        this.value = initialValue;
    }

    public double value() {
        return value;
    }

    public double velocity() {
        return velocity;
    }

    public void set(double value, double velocity) {
        this.value = value;
        this.velocity = velocity;
    }

    public void reset(double value) {
        this.value = value;
        this.velocity = 0.0D;
    }

    /** 标准弹簧积分。 */
    public double step(double target, double smoothTime, double dampingRatio, double maxSpeed, double dt) {
        return step(target, smoothTime, dampingRatio, maxSpeed, dt, 0.0D, 1.0D);
    }

    /**
     * 带额外阻尼和速度上限缩放的弹簧积分。
     *
     * @param extraDamping    额外阻尼（1/s）
     * @param maxSpeedScale   速度上限比例（0.05~1）
     */
    public double step(double target, double smoothTime, double dampingRatio, double maxSpeed,
                       double dt, double extraDamping, double maxSpeedScale) {
        if (smoothTime <= 1.0E-4D || smoothTime < 0.02D) {
            value = target;
            velocity = 0.0D;
            return value;
        }
        double ratio = Mth.clamp(dampingRatio, 0.5D, 1.5D);
        double omega = 2.0D / smoothTime;
        double stiffness = omega * omega;
        double damping = 2.0D * ratio * omega + Math.max(0.0D, extraDamping);
        double speedLimit = Math.max(0.01D, maxSpeed * Mth.clamp(maxSpeedScale, 0.05D, 1.0D));
        int steps = Mth.clamp((int) Math.ceil(dt / (1.0D / 120.0D)), 1, 12);
        double h = Math.max(1.0E-4D, dt / steps);
        for (int i = 0; i < steps; i++) {
            double accel = (target - value) * stiffness - velocity * damping;
            velocity += accel * h;
            velocity = Mth.clamp(velocity, -speedLimit, speedLimit);
            value += velocity * h;
        }
        return value;
    }
}
