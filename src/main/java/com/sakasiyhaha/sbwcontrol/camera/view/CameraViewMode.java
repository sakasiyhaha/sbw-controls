package com.sakasiyhaha.sbwcontrol.camera.view;

/**
 * 飞机/直升机的第三人称视角模式。
 */
public enum CameraViewMode {
    /** 卓越前线原生座位机位。 */
    NATIVE,
    /** 强制机身中线，相机刚性跟随载具旋转，无动态通道。 */
    CENTERED_FOLLOW,
    /** 强制机身中线，启用正交动态通道。 */
    CENTERED_DYNAMIC
}
