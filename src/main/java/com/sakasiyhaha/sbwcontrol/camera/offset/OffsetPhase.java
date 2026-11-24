package com.sakasiyhaha.sbwcontrol.camera.offset;

/** yaw/pitch 动态偏移的唯一状态来源。 */
public enum OffsetPhase {
    NORMAL,
    NEUTRAL_WAIT,
    RETURN,
    RECENTER,
    HOLD,
    SETTLE
}
