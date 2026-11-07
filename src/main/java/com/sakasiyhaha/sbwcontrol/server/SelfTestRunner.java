package com.sakasiyhaha.sbwcontrol.server;

import com.atsuishio.superbwarfare.data.vehicle.subdata.VehicleType;
import com.atsuishio.superbwarfare.entity.vehicle.A10Entity;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.sakasiyhaha.sbwcontrol.SbwControl;
import com.sakasiyhaha.sbwcontrol.config.ServerConfig;
import com.sakasiyhaha.sbwcontrol.control.ClientSupport;
import com.sakasiyhaha.sbwcontrol.control.ControlScheme;
import com.sakasiyhaha.sbwcontrol.control.RemoteInput;
import com.sakasiyhaha.sbwcontrol.network.SupportPacket;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * 无头自测：用 {@code gradlew -Dsbwcontrol.selftest=true runServer} 启动专用服务器，
 * 在出生点上方生成一架 A-10A，直接调用 {@link ServerControlHandler#applyToVehicle}
 * 验证「三通道 → 卓越前线公开输入字段」的写入是否精确、A/D 输入位是否被清理，
 * 然后把结果打到日志并关闭服务器。
 *
 * <p>这是对最脆弱的“跨 mod 字段注入”环节的自动化回归测试；实机手感仍需用户测试。
 */
public final class SelfTestRunner {

    private static int countdown = 40;
    private static boolean ran;

    private SelfTestRunner() {
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        SbwControl.LOGGER.info("[sbwcontrol][SELFTEST] server started, will run control injection test in {} ticks", countdown);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ran) {
            return;
        }
        if (--countdown > 0) {
            return;
        }
        ran = true;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        boolean pass;
        String detail;
        try {
            pass = runTest(server);
            detail = "field injection verified";
        } catch (Throwable t) {
            pass = false;
            detail = t.getClass().getSimpleName() + ": " + t.getMessage();
            SbwControl.LOGGER.error("[sbwcontrol][SELFTEST] exception", t);
        }
        if (pass) {
            SbwControl.LOGGER.info("[sbwcontrol][SELFTEST] PASS - {}", detail);
        } else {
            SbwControl.LOGGER.warn("[sbwcontrol][SELFTEST] FAIL - {}", detail);
        }
        if (server != null) {
            server.halt(false);
        }
    }

    private static boolean runTest(MinecraftServer server) {
        if (server == null) {
            throw new IllegalStateException("no server");
        }
        if (!ServerControlHandler.probe()) {
            throw new IllegalStateException("Superb Warfare vehicle API not detected");
        }
        ServerLevel level = server.overworld();
        if (level == null) {
            throw new IllegalStateException("no overworld");
        }
        EntityType<A10Entity> type = ModEntities.A_10A.get();
        A10Entity vehicle = type.create(level);
        if (vehicle == null) {
            throw new IllegalStateException("failed to create A-10A entity");
        }
        BlockPos spawn = level.getSharedSpawnPos();
        vehicle.moveTo(spawn.getX() + 0.5D, spawn.getY() + 40.0D, spawn.getZ() + 0.5D, 0.0F, 0.0F);
        level.addFreshEntity(vehicle);
        if (vehicle.getVehicleType() != VehicleType.AIRPLANE) {
            throw new IllegalStateException("unexpected vehicle type: " + vehicle.getVehicleType());
        }

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
            throw new IllegalStateException("yaw channel mismatch: got " + vehicle.getMouseMoveSpeedX() + " expected " + expectYaw);
        }
        if (Math.abs(vehicle.getMouseMoveSpeedY() - expectPitch) > eps) {
            throw new IllegalStateException("pitch channel mismatch: got " + vehicle.getMouseMoveSpeedY() + " expected " + expectPitch);
        }
        if (Math.abs(vehicle.getDeltaRot() - expectRoll) > eps) {
            throw new IllegalStateException("roll channel mismatch: got " + vehicle.getDeltaRot() + " expected " + expectRoll);
        }
        if (vehicle.leftInputDown() || vehicle.rightInputDown()) {
            throw new IllegalStateException("A/D input flags were not cleared");
        }
        double expectExtraYaw = inputYawDelta(yaw);
        double actualExtraYaw = net.minecraft.util.Mth.wrapDegrees(vehicle.getYRot());
        if (Math.abs(net.minecraft.util.Mth.wrapDegrees((float) (actualExtraYaw - expectExtraYaw))) > 0.05D) {
            throw new IllegalStateException("extra yaw delta mismatch: got " + actualExtraYaw
                    + " expected " + expectExtraYaw);
        }

        SbwControl.LOGGER.info("[sbwcontrol][SELFTEST] vehicle={} pos={} yawCh={} pitchCh={} rollCh={}",
                vehicle.getVehicleType(), vehicle.blockPosition(),
                vehicle.getMouseMoveSpeedX(), vehicle.getMouseMoveSpeedY(), vehicle.getDeltaRot());
        vehicle.discard();

        testServerCommands(server);
        testSupportAndFallback();
        return true;
    }

    /** 验证 /sbwctlserver 命令注册、配置写入与允许列表变更（跑完恢复原值）。 */
    private static void testServerCommands(MinecraftServer server) {
        var source = server.createCommandSourceStack();
        int status = server.getCommands().performPrefixedCommand(source, "sbwctlserver status");
        if (status <= 0) {
            throw new IllegalStateException("sbwctlserver status failed: result=" + status);
        }
        double originalRate = ServerConfig.EXTRA_YAW_RATE.get();
        boolean originalAllowRollBird = ServerConfig.ALLOW_ROLL_BIRD.get();
        try {
            int rate = server.getCommands().performPrefixedCommand(source, "sbwctlserver yawrate 12.5");
            if (rate <= 0 || Math.abs(ServerConfig.EXTRA_YAW_RATE.get() - 12.5D) > 1.0E-6D) {
                throw new IllegalStateException("sbwctlserver yawrate failed: result=" + rate
                        + " value=" + ServerConfig.EXTRA_YAW_RATE.get());
            }
            int allow = server.getCommands().performPrefixedCommand(source, "sbwctlserver allow ROLL_BIRD false");
            if (allow <= 0 || ServerConfig.ALLOW_ROLL_BIRD.get()) {
                throw new IllegalStateException("sbwctlserver allow failed: result=" + allow
                        + " value=" + ServerConfig.ALLOW_ROLL_BIRD.get());
            }
        } finally {
            ServerConfig.EXTRA_YAW_RATE.set(originalRate);
            ServerConfig.ALLOW_ROLL_BIRD.set(originalAllowRollBird);
            ServerConfig.SPEC.save();
        }
        SbwControl.LOGGER.info("[sbwcontrol][SELFTEST] command test: sbwctlserver status/yawrate/allow OK");
    }

    /** 验证 SupportPacket 编解码、客户端降级/允许位判断、服务端允许掩码。 */
    private static void testSupportAndFallback() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        SupportPacket.encode(new SupportPacket(true, 0b101), buffer);
        SupportPacket decoded = SupportPacket.decode(buffer);
        buffer.release();
        if (!decoded.supported || decoded.allowedMask != 0b101) {
            throw new IllegalStateException("SupportPacket roundtrip mismatch: "
                    + decoded.supported + "/" + decoded.allowedMask);
        }

        ClientSupport.accept(false, 0);
        if (ClientSupport.isSupported() || ClientSupport.isAllowed(ControlScheme.ROLL_BIRD)) {
            throw new IllegalStateException("client did not enter degraded mode");
        }
        ClientSupport.accept(true, ClientSupport.bit(ControlScheme.ROLL_BIRD));
        if (!ClientSupport.isSupported()
                || !ClientSupport.isAllowed(ControlScheme.ROLL_BIRD)
                || ClientSupport.isAllowed(ControlScheme.CUSTOM)) {
            throw new IllegalStateException("client allowed-mask handling mismatch");
        }
        ClientSupport.reset();

        boolean original = ServerConfig.ALLOW_ROLL_BIRD.get();
        try {
            ServerConfig.ALLOW_ROLL_BIRD.set(false);
            int mask = ServerControlHandler.allowedMask();
            if ((mask & ClientSupport.bit(ControlScheme.ROLL_BIRD)) != 0) {
                throw new IllegalStateException("server allowed mask still contains disabled scheme");
            }
            if ((mask & ClientSupport.bit(ControlScheme.YAW_BIRD)) == 0) {
                throw new IllegalStateException("stock scheme must always be allowed");
            }
        } finally {
            ServerConfig.ALLOW_ROLL_BIRD.set(original);
        }
        SbwControl.LOGGER.info("[sbwcontrol][SELFTEST] support/fallback test OK");
    }

    /** applyToVehicle 里额外直接偏航的期望增量（度）：yaw * 角速度 * 1 tick(0.05s)。 */
    private static double inputYawDelta(float yaw) {
        return yaw * ServerConfig.EXTRA_YAW_RATE.get() * 0.05D;
    }
}
