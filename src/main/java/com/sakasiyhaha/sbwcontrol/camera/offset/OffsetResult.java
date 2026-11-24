package com.sakasiyhaha.sbwcontrol.camera.offset;

/** 一帧偏移控制器的只读输出。 */
public record OffsetResult(double yaw,
                           double pitch,
                           double yawVelocity,
                           double pitchVelocity,
                           OffsetPhase phase,
                           double edgeBrakeState,
                           double edgeSpeedScale) {
}
