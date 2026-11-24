package com.sakasiyhaha.sbwcontrol.camera.dynamic;

import net.minecraft.util.Mth;

/** 相机滚转与机位 orbit 的独立管线。 */
public final class RollPipeline {

    public record Input(double stockRoll, double bodyRoll, double rollRatio,
                        boolean rollFollow, double rollCoupling, double rollLag,
                        double agility, double springDamping, double dt) {
    }

    public record Output(double eventRoll, double orbitRoll, double smoothedRoll, double stockRoll) {
    }

    private final Spring1D smoothedRoll = new Spring1D(0.0D);
    private final Spring1D orbitRoll = new Spring1D(0.0D);
    private boolean rollInitialized;
    private boolean orbitInitialized;
    private double lastStockRoll;

    public void reset() {
        smoothedRoll.reset(0.0D);
        orbitRoll.reset(0.0D);
        rollInitialized = false;
        orbitInitialized = false;
        lastStockRoll = 0.0D;
    }

    public Output update(Input input) {
        double stockRoll = unwrapNear(input.stockRoll(), lastStockRoll);
        double bodyRoll = unwrapNear(input.bodyRoll(), stockRoll);
        double targetRoll;
        double lag;
        if (input.rollFollow()) {
            double mixed = stockRoll + Mth.wrapDegrees(bodyRoll - stockRoll) * input.rollCoupling();
            targetRoll = unwrapNear(mixed * input.rollRatio(), smoothedRoll.value());
            lag = input.rollLag();
        } else {
            targetRoll = unwrapNear(stockRoll * input.rollRatio(), smoothedRoll.value());
            lag = input.rollLag();
        }
        if (!rollInitialized) {
            smoothedRoll.set(targetRoll, 0.0D);
            rollInitialized = true;
        } else {
            double agility = input.rollFollow() ? 1.0D : Math.max(0.01D, input.agility());
            smoothedRoll.step(targetRoll, lag / agility, input.springDamping(), 1080.0D, input.dt());
        }

        if (input.rollFollow()) {
            orbitRoll.set(smoothedRoll.value(), 0.0D);
            orbitInitialized = true;
        } else if (!orbitInitialized) {
            orbitRoll.reset(0.0D);
            orbitInitialized = true;
        } else {
            orbitRoll.step(0.0D, lag / Math.max(0.01D, input.agility()),
                    input.springDamping(), 1080.0D, input.dt());
        }

        double cameraRollOffset;
        if (input.rollFollow()) {
            int mode = com.sakasiyhaha.sbwcontrol.config.ClientConfig.CAMERA_STICKY_ROLL_MODE.get();
            cameraRollOffset = switch (mode) {
                case 1 -> smoothedRoll.value();
                case 2 -> stockRoll - smoothedRoll.value();
                case 3 -> -smoothedRoll.value();
                default -> smoothedRoll.value() - stockRoll;
            };
        } else {
            cameraRollOffset = smoothedRoll.value() - stockRoll;
        }
        cameraRollOffset = Mth.wrapDegrees(cameraRollOffset);
        lastStockRoll = stockRoll;
        return new Output(cameraRollOffset, orbitRoll.value(), smoothedRoll.value(), stockRoll);
    }

    public double lastStockRoll() {
        return lastStockRoll;
    }

    public double orbitRoll() {
        return orbitRoll.value();
    }

    public double smoothedRoll() {
        return smoothedRoll.value();
    }

    private static double unwrapNear(double angle, double reference) {
        return reference + Mth.wrapDegrees(angle - reference);
    }
}
