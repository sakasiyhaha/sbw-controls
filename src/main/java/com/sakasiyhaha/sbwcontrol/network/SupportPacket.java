package com.sakasiyhaha.sbwcontrol.network;

import net.minecraft.network.FriendlyByteBuf;

/**
 * 服务端 → 客户端：登录时告知服务端是否安装本附属、允许哪些控制流派。
 * 客户端据此决定是否启用“控制流派切换”，并在 HUD 上提示降级。
 */
public final class SupportPacket {

    public final boolean supported;
    public final int allowedMask;

    public SupportPacket(boolean supported, int allowedMask) {
        this.supported = supported;
        this.allowedMask = allowedMask;
    }

    public static void encode(SupportPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.supported);
        buf.writeInt(msg.allowedMask);
    }

    public static SupportPacket decode(FriendlyByteBuf buf) {
        return new SupportPacket(buf.readBoolean(), buf.readInt());
    }
}
