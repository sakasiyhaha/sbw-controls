package com.sakasiyhaha.sbwcontrol.gametest;

import com.atsuishio.superbwarfare.entity.vehicle.A10Entity;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleVecUtils;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.sakasiyhaha.sbwcontrol.SbwControl;
import com.sakasiyhaha.sbwcontrol.config.ServerConfig;
import com.sakasiyhaha.sbwcontrol.control.ControlScheme;
import com.sakasiyhaha.sbwcontrol.control.RemoteInput;
import com.sakasiyhaha.sbwcontrol.network.CtlPacket;
import com.sakasiyhaha.sbwcontrol.server.ServerControlHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Forge GameTest：在 GameTest 服务器里生成一架 A-10A，验证三通道→卓越前线公开字段的注入。
 * 对应 {@code gradlew runGameTestServer}；与无头自测互相独立。
 */
@GameTestHolder(SbwControl.MODID)
public final class SbwControlGameTests {

    private SbwControlGameTests() {
    }

    @PrefixGameTestTemplate(false)
    @GameTest(template = "empty", templateNamespace = SbwControl.MODID, timeoutTicks = 100)
    public static void serverFieldInjection(GameTestHelper helper) {
        A10Entity vehicle = null;
        try {
            var level = helper.getLevel();
            vehicle = ModEntities.A_10A.get().create(level);
            if (vehicle == null) {
                helper.fail("failed to create A-10A entity");
                return;
            }
            BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
            vehicle.moveTo(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 0.0F, 0.0F);
            level.addFreshEntity(vehicle);

            float pitch = 0.5F;
            float roll = 1.0F;
            float yaw = -1.0F;
            ServerControlHandler.applyToVehicle(vehicle,
                    new RemoteInput(pitch, roll, yaw, ControlScheme.ROLL_BIRD, true, false, 0.0F, 0.0F, 0L));

            double expectPitch = pitch * ServerConfig.PITCH_SCALE.get();
            double eps = 1.0E-3D;
            if (Math.abs(vehicle.getMouseMoveSpeedX()) > eps) {
                helper.fail("mouse X must stay 0 in direct-yaw RollBird: " + vehicle.getMouseMoveSpeedX());
                return;
            }
            if (Math.abs(vehicle.getMouseMoveSpeedY() - expectPitch) > eps) {
                helper.fail("pitch channel mismatch: " + vehicle.getMouseMoveSpeedY() + " != " + expectPitch);
                return;
            }
            if (!vehicle.rightInputDown() || vehicle.leftInputDown()) {
                helper.fail("roll key simulation mismatch: right=" + vehicle.rightInputDown()
                        + " left=" + vehicle.leftInputDown());
                return;
            }
            double expectedExtraYaw = yaw * ServerConfig.MAX_YAW_RATE.get() * 0.05D;
            double actualYaw = Mth.wrapDegrees(vehicle.getYRot());
            if (Math.abs(Mth.wrapDegrees((float) (actualYaw - expectedExtraYaw))) > 0.05D) {
                helper.fail("extra yaw mismatch: " + actualYaw + " != " + expectedExtraYaw);
                return;
            }
            helper.succeed();
        } catch (Throwable t) {
            helper.fail("field injection test failed: " + t);
        } finally {
            if (vehicle != null) {
                vehicle.discard();
            }
        }
    }

    /**
     * 用真实的 MI-28 炮塔变换验证中心瞄准符号：
     * 机体 yaw=30，客户端 aim=20，局部炮塔 yaw 应为 body-aim=10；
     * 实际 getBarrelVector() 的世界 yaw 应回到 20。
     */
    @PrefixGameTestTemplate(false)
    @GameTest(template = "empty", templateNamespace = SbwControl.MODID, timeoutTicks = 100)
    public static void turretCenterAimSign(GameTestHelper helper) {
        VehicleEntity vehicle = null;
        try {
            var level = helper.getLevel();
            vehicle = (VehicleEntity) ModEntities.MI_28.get().create(level);
            if (vehicle == null) {
                helper.fail("failed to create MI-28");
                return;
            }
            BlockPos pos = helper.absolutePos(new BlockPos(1, 5, 1));
            vehicle.moveTo(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 30.0F, 0.0F);
            level.addFreshEntity(vehicle);

            float bodyYaw = 30.0F;
            float aimYaw = 20.0F;
            vehicle.setYRot(bodyYaw);
            vehicle.setXRot(0.0F);
            float localYaw = ServerControlHandler.turretLocalYawForAim(bodyYaw, aimYaw);
            vehicle.setTurretYRot(localYaw);
            vehicle.setTurretYRotO(localYaw);
            vehicle.setTurretXRot(0.0F);
            vehicle.setTurretXRotO(0.0F);

            Vec3 barrel = vehicle.getBarrelVector(1.0F);
            // getYRotFromVector() 返回 atan2(x,z)，与 MC 机体 yaw 符号相反；
            // 卓越前线的 getBarrelYRot() 也显式取负。这里转成机体 yaw 约定。
            double actualYaw = -VehicleVecUtils.getYRotFromVector(barrel);
            double error = Math.abs(Mth.wrapDegrees((float) (actualYaw - aimYaw)));
            if (error > 0.25D) {
                helper.fail("turret yaw sign mismatch: local=" + localYaw
                        + " barrelYaw=" + actualYaw + " aim=" + aimYaw
                        + " error=" + error);
                return;
            }

            // 真实射击向量也必须回到 aim，而不仅是视觉 barrel。
            Vec3 shootVec = vehicle.getShootVec("Cannon", 1.0F);
            double shootYaw = -VehicleVecUtils.getYRotFromVector(shootVec);
            double shootError = Math.abs(Mth.wrapDegrees((float) (shootYaw - aimYaw)));
            if (shootError > 0.25D) {
                helper.fail("getShootVec(Cannon) mismatch: local=" + localYaw
                        + " shootYaw=" + shootYaw + " aim=" + aimYaw
                        + " error=" + shootError);
                return;
            }
            helper.succeed();
        } catch (Throwable t) {
            helper.fail("turret center aim sign test failed: " + t);
        } finally {
            if (vehicle != null) {
                vehicle.discard();
            }
        }
    }

    /**
     * 用无炮塔的小鸟直升机验证开火方向覆盖（Unsafe 改写 ShootParameters.shootDirection）：
     * 机体 yaw=30，中心瞄准 aim=20；未覆盖时固定炮会沿 30°，覆盖后应沿 20°。
     */
    @PrefixGameTestTemplate(false)
    @GameTest(template = "empty", templateNamespace = SbwControl.MODID, timeoutTicks = 100)
    public static void fixedGunCenterAimOverride(GameTestHelper helper) {
        VehicleEntity vehicle = null;
        try {
            var level = helper.getLevel();
            vehicle = (VehicleEntity) ModEntities.AH_6.get().create(level);
            if (vehicle == null) {
                helper.fail("failed to create AH-6");
                return;
            }
            BlockPos pos = helper.absolutePos(new BlockPos(1, 5, 1));
            vehicle.moveTo(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 30.0F, 0.0F);
            level.addFreshEntity(vehicle);

            float aimYaw = 20.0F;
            ServerControlHandler.setCenterAimTestMode(true);
            ServerControlHandler.putCenterAimDirection(vehicle.getId(), aimYaw, 0.0F);

            Vec3 shootVec = vehicle.getShootVec("Cannon", 1.0F);
            double shootYaw = -VehicleVecUtils.getYRotFromVector(shootVec);
            double error = Math.abs(Mth.wrapDegrees((float) (shootYaw - aimYaw)));
            if (error > 0.5D) {
                helper.fail("fixed gun center aim override mismatch: shootYaw=" + shootYaw
                        + " aim=" + aimYaw + " error=" + error);
                return;
            }
            helper.succeed();
        } catch (Throwable t) {
            helper.fail("fixed gun center aim override test failed: " + t);
        } finally {
            ServerControlHandler.setCenterAimTestMode(false);
            if (vehicle != null) {
                vehicle.discard();
            }
        }
    }

    /**
     * 直升机 A/D 偏航走原版 mouseMoveSpeedX，并按旧公式反算：
     * mouseX = yaw * max_yaw_rate / (40 * synchedPropellerRot)。
     */
    @PrefixGameTestTemplate(false)
    @GameTest(template = "empty", templateNamespace = SbwControl.MODID, timeoutTicks = 100)
    public static void helicopterNativeYawFormula(GameTestHelper helper) {
        VehicleEntity vehicle = null;
        try {
            var level = helper.getLevel();
            vehicle = (VehicleEntity) ModEntities.MI_28.get().create(level);
            if (vehicle == null) {
                helper.fail("failed to create MI-28");
                return;
            }
            BlockPos pos = helper.absolutePos(new BlockPos(1, 5, 1));
            vehicle.moveTo(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 0.0F, 0.0F);
            level.addFreshEntity(vehicle);
            vehicle.setSynchedPropellerRot(0.5F);

            float yaw = -1.0F;
            ServerControlHandler.applyToVehicle(vehicle,
                    new RemoteInput(0.0F, 0.0F, yaw, ControlScheme.ROLL_BIRD, true, false, 0.0F, 0.0F, 0L));

            double expectedX = yaw * (ServerConfig.MAX_YAW_RATE.get() / 40.0D) / 0.5D;
            if (Math.abs(vehicle.getMouseMoveSpeedX() - expectedX) > 1.0E-3D) {
                helper.fail("helicopter mouse yaw mismatch: " + vehicle.getMouseMoveSpeedX()
                        + " != " + expectedX);
                return;
            }
            helper.succeed();
        } catch (Throwable t) {
            helper.fail("helicopter native yaw formula test failed: " + t);
        } finally {
            if (vehicle != null) {
                vehicle.discard();
            }
        }
    }
}
