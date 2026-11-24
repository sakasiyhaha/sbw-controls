package com.sakasiyhaha.sbwcontrol.camera.view;

/**
 * 类型级视角模式：可以跟随全局默认，或显式覆盖。
 */
public enum VehicleViewMode {
    FOLLOW_GLOBAL,
    NATIVE,
    CENTERED_FOLLOW,
    CENTERED_DYNAMIC;

    public CameraViewMode resolve(CameraViewMode global) {
        return switch (this) {
            case FOLLOW_GLOBAL -> global;
            case NATIVE -> CameraViewMode.NATIVE;
            case CENTERED_FOLLOW -> CameraViewMode.CENTERED_FOLLOW;
            case CENTERED_DYNAMIC -> CameraViewMode.CENTERED_DYNAMIC;
        };
    }
}
