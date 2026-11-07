package com.sakasiyhaha.sbwcontrol.camera;

/**
 * 相机预设。预设只作为“系数倍率”，最终参数 = 配置里的基础值 × 预设倍率，
 * 因此用户选预设后仍然可以在配置界面里逐项微调（并通过 CUSTOM 保存自己的手感）。
 */
public enum CameraPreset {

    /** 战地 2042 风：明显滞后 + 转向滞后 + 速度 FOV。 */
    BATTLEFIELD(1.0, 1.0, 1.0, 1.0, 1.0),
    /** 三角洲行动风：第三人称相机相对稳定，滞后/弹簧幅度中等，速度 FOV 很轻。 */
    DELTA(0.55, 0.45, 0.8, 0.2, 0.3),
    /** 战争雷霆风：更稳、更贴机头。 */
    WARTHUNDER(0.5, 0.45, 0.55, 0.35, 0.35),
    /** 原版相机：全部倍率归零。 */
    VANILLA(0.0, 0.0, 0.0, 0.0, 0.0),
    /** 自定义：使用配置里的基础值原样（倍率 1）。 */
    CUSTOM(1.0, 1.0, 1.0, 1.0, 1.0);

    public final double lagYawScale;
    public final double lagPitchScale;
    public final double leadScale;
    public final double fovScale;
    public final double distanceScale;

    CameraPreset(double lagYawScale, double lagPitchScale, double leadScale, double fovScale, double distanceScale) {
        this.lagYawScale = lagYawScale;
        this.lagPitchScale = lagPitchScale;
        this.leadScale = leadScale;
        this.fovScale = fovScale;
        this.distanceScale = distanceScale;
    }
}
