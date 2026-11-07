package com.sakasiyhaha.sbwcontrol.control;

/** 服务端保存的最近一次客户端控制输入（每个玩家一份）。通道归一化到 [-1, 1]。 */
public final class RemoteInput {

    public final float pitch;
    public final float roll;
    public final float yaw;
    public final ControlScheme scheme;
    public final boolean active;
    public final long gameTime;

    public RemoteInput(float pitch, float roll, float yaw, ControlScheme scheme, boolean active, long gameTime) {
        this.pitch = pitch;
        this.roll = roll;
        this.yaw = yaw;
        this.scheme = scheme;
        this.active = active;
        this.gameTime = gameTime;
    }
}
