package com.sakasiyhaha.sbwcontrol.network;

import com.sakasiyhaha.sbwcontrol.control.ControlScheme;
import net.minecraft.network.FriendlyByteBuf;

/**
 * 客户端 → 服务端：每 tick 发送一次的控制通道数据（归一化 [-1,1]）。
 * active=false 表示退出方案（服务端应停止应用）。
 */
public final class CtlPacket {

    public final float pitch;
    public final float roll;
    public final float yaw;
    public final ControlScheme scheme;
    public final boolean active;

    public CtlPacket(float pitch, float roll, float yaw, ControlScheme scheme, boolean active) {
        this.pitch = pitch;
        this.roll = roll;
        this.yaw = yaw;
        this.scheme = scheme;
        this.active = active;
    }

    public static void encode(CtlPacket msg, FriendlyByteBuf buf) {
        buf.writeFloat(msg.pitch);
        buf.writeFloat(msg.roll);
        buf.writeFloat(msg.yaw);
        buf.writeByte(msg.scheme.ordinal());
        buf.writeBoolean(msg.active);
    }

    public static CtlPacket decode(FriendlyByteBuf buf) {
        float pitch = buf.readFloat();
        float roll = buf.readFloat();
        float yaw = buf.readFloat();
        int scheme = buf.readUnsignedByte();
        boolean active = buf.readBoolean();
        ControlScheme[] values = ControlScheme.values();
        return new CtlPacket(pitch, roll, yaw,
                values[Math.floorMod(scheme, values.length)], active);
    }
}
