package com.sakasiyhaha.sbwcontrol.network;

import com.sakasiyhaha.sbwcontrol.SbwControl;
import com.sakasiyhaha.sbwcontrol.control.ClientSupport;
import com.sakasiyhaha.sbwcontrol.server.ServerControlHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/** 本附属的网络通道（独立于卓越前线自己的通道，协议版本 1）。 */
public final class ControlNetwork {

    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(SbwControl.MODID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private ControlNetwork() {
    }

    public static void init() {
        int id = 0;
        CHANNEL.registerMessage(id++, CtlPacket.class, CtlPacket::encode, CtlPacket::decode,
                (msg, ctx) -> {
                    ctx.get().enqueueWork(() -> {
                        ServerPlayer sender = ctx.get().getSender();
                        if (sender != null) {
                            ServerControlHandler.accept(sender, msg);
                        }
                    });
                    ctx.get().setPacketHandled(true);
                });
        CHANNEL.registerMessage(id, SupportPacket.class, SupportPacket::encode, SupportPacket::decode,
                (msg, ctx) -> {
                    ctx.get().enqueueWork(() -> ClientSupport.accept(msg.supported, msg.allowedMask));
                    ctx.get().setPacketHandled(true);
                });
    }

    public static void sendToServer(CtlPacket msg) {
        CHANNEL.sendToServer(msg);
    }

    public static void sendSupport(ServerPlayer player, boolean supported, int allowedMask) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SupportPacket(supported, allowedMask));
    }
}
