package com.sakasiyhaha.sbwcontrol.control;

/**
 * 鼠标输入模型：
 * <ul>
 *   <li>{@link #RATE}：鼠标位移 = 角速度（松手立即回中，接近卓越前线原版手感）。</li>
 *   <li>{@link #STICK}：鼠标位移累积成"虚拟摇杆"位置，松手后按 auto-center 时间缓慢回中
 *       （更适合翻滚鸟：鼠标偏转保持滚转，回中后再停止）。</li>
 * </ul>
 */
public enum InputMode {
    RATE,
    STICK
}
