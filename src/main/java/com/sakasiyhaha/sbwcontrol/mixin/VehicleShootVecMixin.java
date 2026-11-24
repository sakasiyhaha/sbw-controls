package com.sakasiyhaha.sbwcontrol.mixin;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.sakasiyhaha.sbwcontrol.server.ServerControlHandler;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * RollBird 中心瞄准：把服务端普通炮弹的实际发射方向固定到客户端瞄准方向。
 *
 * <p>卓越前线的 getShootVec(weaponName, partialTicks) 只按炮塔/机体几何计算，不含相机、
 * 飞行时间或网络延迟；A/D 是服务端直接 setYRot，客户端画面滞后 L tick，固定炮（小鸟直升机
 * 等 "Vehicle" 局部方向）的子弹就会比客户端看到的机头超前 ωL。
 * 本 Mixin 只在服务端、且载具第一乘客处于 RollBird 中心瞄准时，用客户端 AimFrame
 * 方向覆盖返回值，让固定炮/炮塔炮都从客户端看到的机头方向出膛。
 */
@Mixin(VehicleEntity.class)
public abstract class VehicleShootVecMixin {

    @Inject(
            method = "getShootVec(Ljava/lang/String;F)Lnet/minecraft/world/phys/Vec3;",
            at = @At("RETURN"),
            cancellable = true,
            remap = false
    )
    private void sbw$centerAimShootVec(String weaponName, float partialTicks,
                                       CallbackInfoReturnable<Vec3> cir) {
        VehicleEntity self = (VehicleEntity) (Object) this;
        if (self.level().isClientSide()) {
            return;
        }
        Vec3 direction = ServerControlHandler.centerAimDirection(self.getId());
        if (direction != null) {
            cir.setReturnValue(direction);
        }
    }
}
