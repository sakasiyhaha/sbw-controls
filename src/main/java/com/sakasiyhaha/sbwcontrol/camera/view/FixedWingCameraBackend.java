package com.sakasiyhaha.sbwcontrol.camera.view;

/**
 * 固定翼相机的机位后端。
 */
public enum FixedWingCameraBackend {
    /** 完全独立机位，不读取卓越前线座位相机位置。 */
    INDEPENDENT,
    /** 沿用卓越前线座位机位，再覆盖角度。 */
    SW_OVERRIDE
}
