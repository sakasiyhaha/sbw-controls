package com.sakasiyhaha.sbwcontrol.client;

import com.atsuishio.superbwarfare.data.vehicle.subdata.VehicleType;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.sakasiyhaha.sbwcontrol.SbwControl;
import com.sakasiyhaha.sbwcontrol.config.ClientConfig;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 可选：隐藏卓越前线第三人称载具准星，改由 {@link NoseMarkerHud} 在“实际弹道投影点”画替换准星。
 *
 * <p>只取消 SW 自定义 HUD overlay {@code superbwarfare:vehicle_crosshair}，不碰第一人称/瞄准放大
 * （那些情况保留 SW 自己的准星），也不影响原版准星。
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = SbwControl.MODID, value = Dist.CLIENT)
public final class CrosshairReplacement {

    private static final String SW_NAMESPACE = "superbwarfare";
    private static final String SW_CROSSHAIR_PATH = "vehicle_crosshair";

    private CrosshairReplacement() {
    }

    @SubscribeEvent
    public static void onRenderOverlayPre(RenderGuiOverlayEvent.Pre event) {
        if (!ClientConfig.CAMERA_REPLACE_CROSSHAIR.get() || !ClientConfig.CAMERA_ENABLED.get()) {
            return;
        }
        ResourceLocation overlayId = event.getOverlay().id();
        // SW's CommonOverlay ID is "superbwarfare_vehicle_crosshair"; Forge prefixes the active
        // namespace, so accept both "<superbwarfare>:vehicle_crosshair" and
        // "<superbwarfare>:superbwarfare_vehicle_crosshair".
        if (!SW_NAMESPACE.equals(overlayId.getNamespace())
                || !overlayId.getPath().contains(SW_CROSSHAIR_PATH)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.options.getCameraType() != CameraType.THIRD_PERSON_BACK
                || ClientEventHandler.zoomVehicle || mc.player == null) {
            return;
        }
        if (!(mc.player.getVehicle() instanceof VehicleEntity vehicle)) {
            return;
        }
        VehicleType type = vehicle.getVehicleType();
        if (type != VehicleType.AIRPLANE && type != VehicleType.HELICOPTER) {
            return;
        }
        event.setCanceled(true);
    }
}
