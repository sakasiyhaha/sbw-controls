package com.sakasiyhaha.sbwcontrol.client;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.sakasiyhaha.sbwcontrol.SbwControl;
import com.sakasiyhaha.sbwcontrol.config.ClientConfig;
import com.sakasiyhaha.sbwcontrol.control.ClientSupport;
import com.sakasiyhaha.sbwcontrol.key.ModKeys;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/** 调试 HUD：显示控制通道、服务端支持状态、载具姿态、相机偏移。默认关闭，可在设置界面/按键打开。 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = SbwControl.MODID, value = Dist.CLIENT)
public final class DebugHud {

    private DebugHud() {
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (!ClientConfig.DEBUG_HUD.get()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        List<String> lines = new ArrayList<>(8);
        lines.add("SBW Controls 1.0.0");
        lines.add("scheme=" + ClientControlHandler.getLastScheme()
                + " active=" + ClientControlHandler.isActive());
        lines.add(String.format("ch p=%.2f r=%.2f y=%.2f stick=%.2f/%.2f",
                ClientControlHandler.getPitch(),
                ClientControlHandler.getRoll(),
                ClientControlHandler.getYaw(),
                ClientControlHandler.getStickX(),
                ClientControlHandler.getStickY()));
        lines.add("keys yawL=" + ModKeys.YAW_LEFT.isDown()
                + " yawR=" + ModKeys.YAW_RIGHT.isDown()
                + " rollL=" + ModKeys.ROLL_LEFT.isDown()
                + " rollR=" + ModKeys.ROLL_RIGHT.isDown());
        lines.add("server=" + (ClientSupport.isSupported() ? "supported" : "NOT-supported")
                + " mask=0b" + Integer.toBinaryString(ClientSupport.allowedMask()));
        if (mc.player.getVehicle() instanceof VehicleEntity vehicle) {
            lines.add(String.format("veh=%s yaw=%.1f pitch=%.1f roll=%.1f speed=%.2f",
                    vehicle.getVehicleType(),
                    vehicle.getYaw(1.0F),
                    vehicle.getPitch(1.0F),
                    vehicle.getRoll(1.0F),
                    vehicle.getDeltaMovement().length()));
        }
        lines.add(String.format("cam dYaw=%.2f dPitch=%.2f dist=%.2f",
                CameraHandler.getOffsetYaw(),
                CameraHandler.getOffsetPitch(),
                CameraHandler.getExtraDistance()));

        int x = 4;
        int y = 4;
        for (String line : lines) {
            graphics.drawString(mc.font, line, x, y, 0xFFFFFF);
            y += 10;
        }
    }
}
