package com.sakasiyhaha.sbwcontrol.gametest;

import com.atsuishio.superbwarfare.entity.vehicle.A10Entity;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.sakasiyhaha.sbwcontrol.SbwControl;
import com.sakasiyhaha.sbwcontrol.config.ServerConfig;
import com.sakasiyhaha.sbwcontrol.control.ControlScheme;
import com.sakasiyhaha.sbwcontrol.control.RemoteInput;
import com.sakasiyhaha.sbwcontrol.server.ServerControlHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.Mth;
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
                    new RemoteInput(pitch, roll, yaw, ControlScheme.ROLL_BIRD, true, 0L));

            double expectPitch = pitch * ServerConfig.PITCH_SCALE.get();
            double expectRoll = -roll * ServerConfig.ROLL_SCALE.get();
            double expectYaw = yaw * ServerConfig.YAW_SCALE.get();
            double eps = 1.0E-3D;
            if (Math.abs(vehicle.getMouseMoveSpeedX() - expectYaw) > eps) {
                helper.fail("yaw channel mismatch: " + vehicle.getMouseMoveSpeedX() + " != " + expectYaw);
                return;
            }
            if (Math.abs(vehicle.getMouseMoveSpeedY() - expectPitch) > eps) {
                helper.fail("pitch channel mismatch: " + vehicle.getMouseMoveSpeedY() + " != " + expectPitch);
                return;
            }
            if (Math.abs(vehicle.getDeltaRot() - expectRoll) > eps) {
                helper.fail("roll channel mismatch: " + vehicle.getDeltaRot() + " != " + expectRoll);
                return;
            }
            if (vehicle.leftInputDown() || vehicle.rightInputDown()) {
                helper.fail("A/D input flags were not cleared");
                return;
            }
            double expectedExtraYaw = yaw * ServerConfig.EXTRA_YAW_RATE.get() * 0.05D;
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
}
