package com.sakasiyhaha.sbwcontrol.control;

/**
 * 飞行控制流派（玩家社区黑话）：
 *
 * <ul>
 *   <li>{@link #YAW_BIRD} 偏航鸟：鼠标左右 = 偏航（机头直接随鼠标转），A/D = 滚转。卓越前线原版行为。</li>
 *   <li>{@link #ROLL_BIRD} 翻滚鸟：鼠标左右 = 滚转（压坡度），A/D = 偏航。战地/战雷常见操作。</li>
 *   <li>{@link #ROLL_YAW_HYBRID} 偏滚鸟：鼠标左右 = 滚转 + 可调比例的偏航耦合，A/D = 偏航。</li>
 *   <li>{@link #CUSTOM} 自定义：俯仰/滚转/偏航三个通道各自指定输入源（鼠标 X / 鼠标 Y / 按键对）。</li>
 * </ul>
 */
public enum ControlScheme {

    YAW_BIRD("yaw_bird"),
    ROLL_BIRD("roll_bird"),
    ROLL_YAW_HYBRID("roll_yaw_hybrid"),
    CUSTOM("custom");

    private final String id;

    ControlScheme(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    /** 网络传输使用 id（而不是 ordinal），避免以后插入枚举值导致协议错位。 */
    public static ControlScheme byId(String id) {
        if (id != null) {
            for (ControlScheme scheme : values()) {
                if (scheme.id.equals(id)) {
                    return scheme;
                }
            }
        }
        return YAW_BIRD;
    }

    public boolean isStock() {
        return this == YAW_BIRD;
    }
}
