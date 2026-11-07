package com.sakasiyhaha.sbwcontrol.client;

import com.atsuishio.superbwarfare.data.vehicle.subdata.VehicleType;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.sakasiyhaha.sbwcontrol.SbwControl;
import com.sakasiyhaha.sbwcontrol.config.ClientConfig;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 可选准星辅助：把卓越前线实际的炮口/机头方向投影到屏幕上，画一个小标记。
 *
 * <p>用途：动态相机会让中心准星在转弯时与真实弹道有轻微偏差；打开这个标记可以直观看到
 * “子弹实际会往哪飞”。默认关闭（config: `camera.nose_marker`），不影响正常准星。
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = SbwControl.MODID, value = Dist.CLIENT)
public final class NoseMarkerHud {

    private NoseMarkerHud() {
    }

    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || !ClientConfig.CAMERA_ENABLED.get() || !ClientConfig.CAMERA_NOSE_MARKER.get()) {
            return;
        }
        if (mc.options.hideGui || mc.options.getCameraType() == CameraType.FIRST_PERSON
                || ClientEventHandler.zoomVehicle) {
            return;
        }
        boolean replace = ClientConfig.CAMERA_REPLACE_CROSSHAIR.get();
        if (!ClientConfig.CAMERA_NOSE_MARKER.get() && !replace) {
            return;
        }
        if (!(player.getVehicle() instanceof VehicleEntity vehicle)) {
            return;
        }
        VehicleType type = vehicle.getVehicleType();
        if (type != VehicleType.AIRPLANE && type != VehicleType.HELICOPTER) {
            return;
        }
        try {
            float partial = event.getPartialTick();
            Vec3 origin = vehicle.getShootPos(player, partial);
            Vec3 direction = vehicle.getShootVec(player, partial);
            if (origin == null || direction == null || direction.lengthSqr() < 1.0E-6D) {
                return;
            }
            Vec3 target = origin.add(direction.normalize().scale(512.0D));
            Camera camera = mc.gameRenderer.getMainCamera();
            int[] screen = project(mc, camera, target);
            if (screen == null) {
                return;
            }
            int x = screen[0];
            int y = screen[1];
            GuiGraphics graphics = event.getGuiGraphics();
            int color = 0xFF55FF55;
            if (replace) {
                // 替换式准星：投影点画一个较大的十字 + 外圈，中心仍用白点。
                int gap = 5;
                int len = 11;
                graphics.fill(x - len, y - 1, x - gap, y + 1, color);
                graphics.fill(x + gap, y - 1, x + len, y + 1, color);
                graphics.fill(x - 1, y - len, x + 1, y - gap, color);
                graphics.fill(x - 1, y + gap, x + 1, y + len, color);
                // 简易外圈（四个角块）
                graphics.fill(x - 9, y - 9, x - 7, y - 7, color);
                graphics.fill(x + 7, y - 9, x + 9, y - 7, color);
                graphics.fill(x - 9, y + 7, x - 7, y + 9, color);
                graphics.fill(x + 7, y + 7, x + 9, y + 9, color);
                graphics.fill(x - 1, y - 1, x + 1, y + 1, 0xFFFFFFFF);
            } else {
                graphics.fill(x - 4, y - 1, x + 5, y + 1, color);
                graphics.fill(x - 1, y - 4, x + 1, y + 5, color);
                // 中心点再亮一点，方便远距离辨认
                graphics.fill(x - 1, y - 1, x + 1, y + 1, 0xFFFFFFFF);
            }
        } catch (Throwable ignored) {
            // 准星辅助是可选功能，任何异常都不应影响游戏
            ClientConfig.CAMERA_NOSE_MARKER.set(false);
            ClientConfig.SPEC.save();
        }
    }

    /** 世界坐标 → GUI 坐标（近似透视投影，忽略相机 roll；仅用于辅助标记）。 */
    private static int[] project(Minecraft mc, Camera camera, Vec3 world) {
        Vec3 cameraPos = camera.getPosition();
        double yaw = Math.toRadians(camera.getYRot());
        double pitch = Math.toRadians(camera.getXRot());
        double forwardX = -Math.sin(yaw) * Math.cos(pitch);
        double forwardY = -Math.sin(pitch);
        double forwardZ = Math.cos(yaw) * Math.cos(pitch);
        double rightX = -Math.cos(yaw);
        double rightZ = -Math.sin(yaw);
        // up = right × forward
        double upX = 0.0D * forwardZ - rightZ * forwardY;
        double upY = rightZ * forwardX - rightX * forwardZ;
        double upZ = rightX * forwardY - 0.0D * forwardX;
        double dx = world.x - cameraPos.x;
        double dy = world.y - cameraPos.y;
        double dz = world.z - cameraPos.z;
        double camX = dx * rightX + dz * rightZ;
        double camY = dx * upX + dy * upY + dz * upZ;
        double camZ = dx * forwardX + dy * forwardY + dz * forwardZ;
        if (camZ <= 0.05D) {
            return null;
        }
        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();
        double fov = mc.options.fov().get() * Math.PI / 180.0D;
        double tanHalf = Math.tan(fov / 2.0D);
        double aspect = (double) width / (double) height;
        double ndcX = camX / (camZ * tanHalf * aspect);
        double ndcY = camY / (camZ * tanHalf);
        if (Math.abs(ndcX) > 1.5D || Math.abs(ndcY) > 1.5D) {
            return null;
        }
        int x = (int) Math.round((ndcX * 0.5D + 0.5D) * width);
        int y = (int) Math.round((0.5D - ndcY * 0.5D) * height);
        return new int[]{x, y};
    }
}
