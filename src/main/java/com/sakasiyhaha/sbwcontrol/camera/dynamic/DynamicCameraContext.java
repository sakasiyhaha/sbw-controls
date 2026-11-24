package com.sakasiyhaha.sbwcontrol.camera.dynamic;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.sakasiyhaha.sbwcontrol.camera.CameraPreset;
import net.minecraft.client.player.LocalPlayer;

/** 一帧动态相机的只读上下文。 */
public record DynamicCameraContext(LocalPlayer player,
                                   VehicleEntity vehicle,
                                   int seat,
                                   float partial,
                                   boolean firstPerson,
                                   boolean zoom,
                                   boolean rollFollowEnabled,
                                   boolean helicopter,
                                   double rollCoupling,
                                   double rollLag,
                                   CameraPreset preset,
                                   double dt) {
}
