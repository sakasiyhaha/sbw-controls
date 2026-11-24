package com.sakasiyhaha.sbwcontrol.server;

import com.atsuishio.superbwarfare.data.vehicle.subdata.EngineType;
import com.atsuishio.superbwarfare.data.vehicle.subdata.VehicleType;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.sakasiyhaha.sbwcontrol.SbwControl;
import com.sakasiyhaha.sbwcontrol.config.ServerConfig;
import com.sakasiyhaha.sbwcontrol.control.ClientSupport;
import com.sakasiyhaha.sbwcontrol.control.ControlScheme;
import com.sakasiyhaha.sbwcontrol.control.RemoteInput;
import com.sakasiyhaha.sbwcontrol.network.ControlNetwork;
import com.sakasiyhaha.sbwcontrol.network.CtlPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务端控制应用层（无 Mixin）。
 *
 * <p>原理：卓越前线的飞行手感由服务端 {@code aircraftEngine / helicopterEngine} 依据
 * {@code mouseMoveSpeedX/Y} 与 {@code deltaRot} 计算。本类在每 tick 开始（实体 tick 之前）
 * 把这些公开字段写成客户端发来的三通道值：
 * <ul>
 *   <li>偏航通道 → {@code mouseInput(x=yaw)}（引擎里鼠标 X 本来就负责偏航/滚转中的偏航项）</li>
 *   <li>俯仰通道 → {@code mouseInput(y=pitch)}（引擎里鼠标 Y 负责俯仰）</li>
 *   <li>滚转通道 → {@code setDeltaRot(...)}（引擎里 A/D 的滚转通道最终就是 deltaRot）</li>
 * </ul>
 * 同时清理 A/D 输入位，避免卓越前线把按键再当一次滚转输入。
 *
 * <p>插件不改变卓越前线的气动、失速、自动回正与伤害逻辑，只替换“输入来源”，
 * 因此版本更新时只要这些公开字段还在就能工作；字段缺失时通过反射探测并禁用（见 {@link #probe()}）。
 */
public final class ServerControlHandler {

    private static final Map<UUID, RemoteInput> INPUTS = new HashMap<>();
    /** 载具实体 id → 最新中心瞄准方向（服务端，供 getShootVec Mixin 覆盖使用）。 */
    private static final Map<Integer, Vec3> CENTER_AIM_DIRS = new ConcurrentHashMap<>();
    /** GameTest 用：没有真实玩家输入时也允许中心瞄准方向生效。 */
    private static volatile boolean CENTER_AIM_TEST_MODE = false;
    /** 滚转弹性按键的脉宽调制相位（每个载具一份）。 */
    private static final Map<Integer, Integer> ROLL_PULSE = new ConcurrentHashMap<>();
    private static int logCounter = 0;
    private static Boolean apiAvailable = null;
    private static double lastAnnouncedMaxYawRate = Double.NaN;

    private ServerControlHandler() {
    }

    /** 反射探测卓越前线接口是否可用（版本不匹配时安静禁用，不崩溃）。 */
    public static boolean probe() {
        if (apiAvailable != null) {
            return apiAvailable;
        }
        try {
            Class<?> vehicle = Class.forName("com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity");
            vehicle.getMethod("mouseInput", double.class, double.class);
            vehicle.getMethod("setDeltaRot", float.class);
            vehicle.getMethod("setLeftInputDown", boolean.class);
            vehicle.getMethod("setRightInputDown", boolean.class);
            apiAvailable = Boolean.TRUE;
        } catch (Throwable t) {
            SbwControl.LOGGER.warn("[sbwcontrol] Superb Warfare vehicle input API not found, control scheme application disabled", t);
            apiAvailable = Boolean.FALSE;
        }
        return apiAvailable;
    }

    /**
     * 服务端最大偏航速度发生变化时，私聊通知在线玩家。
     * 只在真正发生变化时发送，避免每 tick 刷屏。
     */
    private static void announceMaxYawRateIfChanged(MinecraftServer server) {
        double current = ServerConfig.MAX_YAW_RATE.get();
        if (Double.isNaN(lastAnnouncedMaxYawRate)) {
            lastAnnouncedMaxYawRate = current;
            return;
        }
        if (Math.abs(current - lastAnnouncedMaxYawRate) < 0.05D) {
            return;
        }
        lastAnnouncedMaxYawRate = current;
        Component message = Component.translatable(
                "sbwcontrol.message.max_yaw_rate_changed",
                String.format("%.1f", current));
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(message);
        }
    }

    public static void accept(ServerPlayer player, CtlPacket packet) {
        if (player == null) {
            return;
        }
        long now = player.serverLevel().getGameTime();
        if (!packet.active) {
            INPUTS.remove(player.getUUID());
            if (player.getVehicle() instanceof VehicleEntity vehicle) {
                CENTER_AIM_DIRS.remove(vehicle.getId());
                ROLL_PULSE.remove(vehicle.getId());
            }
            return;
        }
        INPUTS.put(player.getUUID(), new RemoteInput(packet.pitch, packet.roll, packet.yaw,
                packet.scheme, true, packet.centerAim, packet.aimYaw, packet.aimPitch, now));
    }

    public static int allowedMask() {
        int mask = ClientSupport.bit(ControlScheme.YAW_BIRD);
        if (ServerConfig.ALLOW_ROLL_BIRD.get()) {
            mask |= ClientSupport.bit(ControlScheme.ROLL_BIRD);
        }
        if (ServerConfig.ALLOW_ROLL_YAW_HYBRID.get()) {
            mask |= ClientSupport.bit(ControlScheme.ROLL_YAW_HYBRID);
        }
        if (ServerConfig.ALLOW_CUSTOM.get()) {
            mask |= ClientSupport.bit(ControlScheme.CUSTOM);
        }
        return mask;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || server.overworld() == null) {
            return;
        }
        announceMaxYawRateIfChanged(server);
        if (!probe() || INPUTS.isEmpty()) {
            return;
        }

        long now = server.overworld().getGameTime();
        int timeout = ServerConfig.INPUT_TIMEOUT_TICKS.get();
        boolean log = ServerConfig.LOG_APPLY.get();
        int mask = allowedMask();

        // 周期性补发支持包（防止登录阶段丢包导致客户端一直降级；每 100 tick 一次，成本极低）
        // 注意 supported 用 probe() 的结果：卓越前线接口缺失时告知客户端不可用，客户端会自动降级
        if (now % 100L == 0L) {
            boolean ready = probe();
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                ControlNetwork.sendSupport(player, ready, mask);
            }
        }

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            RemoteInput input = INPUTS.get(player.getUUID());
            if (input == null) {
                continue;
            }
            if (!input.active || now - input.gameTime > timeout) {
                INPUTS.remove(player.getUUID());
                if (player.getVehicle() instanceof VehicleEntity timedOut) {
                    CENTER_AIM_DIRS.remove(timedOut.getId());
                }
                continue;
            }
            if (input.scheme.isStock()) {
                continue;
            }
            if ((mask & ClientSupport.bit(input.scheme)) == 0) {
                continue;
            }
            if (!(player.getVehicle() instanceof VehicleEntity vehicle)) {
                continue;
            }
            if (vehicle.getFirstPassenger() != player) {
                continue;
            }
            VehicleType type = vehicle.getVehicleType();
            boolean airplane = type == VehicleType.AIRPLANE && ServerConfig.APPLY_AIRPLANE.get();
            boolean helicopter = type == VehicleType.HELICOPTER && ServerConfig.APPLY_HELICOPTER.get();
            if (!airplane && !helicopter) {
                continue;
            }
            // 盘旋自动驾驶 / 残骸 / TOM6 特殊引擎：不接管，交还原版行为
            if (vehicle.getLoiterActive() || vehicle.isWreck()
                    || vehicle.computed().getEngineType() == EngineType.TOM6) {
                continue;
            }

            // 关键：在卓越前线载具实体 tick 之前写入本 tick 的输入
            applyToVehicle(vehicle, input);
            if (input.centerAim && input.scheme == ControlScheme.ROLL_BIRD) {
                applyCenterAim(vehicle, player, input);
            } else {
                CENTER_AIM_DIRS.remove(vehicle.getId());
            }

            if (log && (logCounter++ % 20 == 0)) {
                SbwControl.LOGGER.info("[sbwcontrol] apply {} scheme={} pitch={} roll={} yaw={}",
                        player.getGameProfile().getName(), input.scheme,
                        input.pitch, input.roll, input.yaw);
            }
        }
    }

    /**
     * RollBird 中心锁定：使用客户端开火前的瞄准 yaw/pitch，而不是服务端当前机头。
     *
     * <p>A/D 是服务端直接改 yRot，客户端画面会滞后 L tick；如果把炮塔对到服务端
     * 当前机头，子弹会打向“未来”方向。这里反向补偿：
     * localTurretYaw = clientAimYaw - serverBodyYaw。
     * 这样炮口世界方向回到客户端画面方向；超出包线时 clamp。
     */
    private static void applyCenterAim(VehicleEntity vehicle, ServerPlayer player, RemoteInput input) {
        float aimYaw = input.aimYaw;
        float aimPitch = input.aimPitch;
        if (!Float.isFinite(aimYaw) || !Float.isFinite(aimPitch)) {
            aimYaw = vehicle.getYRot();
            aimPitch = vehicle.getXRot();
        }
        player.setYRot(aimYaw);
        player.setXRot(aimPitch);
        player.setYHeadRot(aimYaw);

        // 固定炮/无炮塔载具没有炮塔角可补偿；把客户端 aim 方向缓存下来，
        // getShootVec Mixin 在真正开火时用它覆盖实际弹道方向。
        CENTER_AIM_DIRS.put(vehicle.getId(), directionFromAngles(aimYaw, aimPitch));

        if (!vehicle.hasTurret()) {
            return;
        }
        // 卓越前线 turretYRot 在机体 yaw 的相反方向叠加：
        // getBarrelYRot() = -(turretYRot - yRot) = yRot - turretYRot。
        // 因此要让炮塔世界方向回到客户端 aimYaw，局部角必须是 bodyYaw - aimYaw。
        float localYaw = turretLocalYawForAim(vehicle.getYRot(), aimYaw);
        float localPitch = Mth.wrapDegrees(aimPitch - vehicle.getXRot());
        // SW turretAutoAim 的 yaw 包线是 [-turretMaxYaw, -turretMinYaw]，不是
        // [turretMinYaw, turretMaxYaw]；当前数据多对称，但按真实包线写。
        float minTurretYaw = -vehicle.getTurretMaxYaw();
        float maxTurretYaw = -vehicle.getTurretMinYaw();
        if (minTurretYaw > maxTurretYaw) {
            float swap = minTurretYaw;
            minTurretYaw = maxTurretYaw;
            maxTurretYaw = swap;
        }
        localYaw = Mth.clamp(localYaw, minTurretYaw, maxTurretYaw);
        float minPitch = -vehicle.getTurretMaxPitch() + vehicle.getCustomTurretMaxPitch();
        float maxPitch = -vehicle.getTurretMinPitch() - vehicle.getCustomTurretMinPitch();
        if (minPitch > maxPitch) {
            float swap = minPitch;
            minPitch = maxPitch;
            maxPitch = swap;
        }
        localPitch = Mth.clamp(localPitch, minPitch, maxPitch);
        vehicle.setTurretYRot(localYaw);
        vehicle.setTurretYRotO(localYaw);
        vehicle.setTurretXRot(localPitch);
        vehicle.setTurretXRotO(localPitch);
    }

    /**
     * 炮塔局部 yaw 与世界 yaw 的换算。公开给 GameTest 做实际 SW 变换验证：
     * 卓越前线 getBarrelYRot() = yRot - turretYRot，所以 local = body - aim。
     */
    public static float turretLocalYawForAim(float bodyYaw, float aimYaw) {
        return Mth.wrapDegrees(bodyYaw - aimYaw);
    }

    /** 玩家视图方向（MC 约定）→ 世界单位向量。 */
    private static Vec3 directionFromAngles(float yawDeg, float pitchDeg) {
        double yaw = Math.toRadians(yawDeg);
        double pitch = Math.toRadians(pitchDeg);
        double cosPitch = Math.cos(pitch);
        return new Vec3(-Math.sin(yaw) * cosPitch, -Math.sin(pitch), Math.cos(yaw) * cosPitch);
    }

    /**
     * 供 getShootVec Mixin 查询：只要存在仍处于 RollBird 中心瞄准的活动输入，
     * 就返回该载具缓存的客户端 aim 方向，否则返回 null（交还卓越前线原方向）。
     */
    public static Vec3 centerAimDirection(int vehicleId) {
        if (!CENTER_AIM_DIRS.containsKey(vehicleId)) {
            return null;
        }
        for (RemoteInput input : INPUTS.values()) {
            if (input != null && input.active && input.centerAim
                    && input.scheme == ControlScheme.ROLL_BIRD) {
                return CENTER_AIM_DIRS.get(vehicleId);
            }
        }
        return CENTER_AIM_TEST_MODE ? CENTER_AIM_DIRS.get(vehicleId) : null;
    }

    /** GameTest 专用开关。 */
    public static void setCenterAimTestMode(boolean enabled) {
        CENTER_AIM_TEST_MODE = enabled;
    }

    /** 保留按玩家查询的重载（兼容旧调用）。 */
    public static Vec3 centerAimDirection(ServerPlayer player, int vehicleId) {
        return centerAimDirection(vehicleId);
    }

    /** GameTest 与内部链路共用：把某载具的中心瞄准方向写入缓存。 */
    public static void putCenterAimDirection(int vehicleId, Vec3 direction) {
        if (direction != null) {
            CENTER_AIM_DIRS.put(vehicleId, direction);
        }
    }

    /** 由 yaw/pitch 计算并写入缓存（GameTest 用）。 */
    public static void putCenterAimDirection(int vehicleId, float aimYaw, float aimPitch) {
        CENTER_AIM_DIRS.put(vehicleId, directionFromAngles(aimYaw, aimPitch));
    }

    /**
     * 把一个三通道输入写进卓越前线载具的公开输入字段（自测也直接调用这个方法）。
     *
     * <p>RollBird 解耦：
     * - 直升机 A/D 偏航 → 原版 mouseMoveSpeedX，但按之前的数学公式反算，
     *   保持“每 tick 偏航 = yaw * max_yaw_rate * 0.05”的相同手感：
     *   SW 引擎 yRot += yawSpeed * clamp(2 * mouseX * synchedPropellerRot, -10, 10)
     *   ⇒ mouseX = yaw * max_yaw_rate / (40 * synchedPropellerRot)
     * - 固定翼 A/D 偏航 → 沿用直接 yRot 公式（固定翼引擎没有同形式的 mouse X 偏航项）。
     * - 鼠标左右滚转 → leftInputDown/rightInputDown 弹性按键（原版 A/D 滚转通道）。
     * - 俯仰 → mouseMoveSpeedY。
     * 炮弹方向由 getShootVec Mixin 固定到客户端当帧瞄准方向。
     *
     * <p>滚转模拟：按 |roll| 比例做 6 tick 脉宽调制，近似模拟鼠标的模拟量。
     */
    public static void applyToVehicle(VehicleEntity vehicle, RemoteInput input) {
        double pitchScale = ServerConfig.PITCH_SCALE.get();
        double maxYawRate = ServerConfig.MAX_YAW_RATE.get();
        double mouseYaw = helicopterMouseYaw(vehicle, input.yaw, maxYawRate);
        if (vehicle.getVehicleType() == VehicleType.AIRPLANE
                && maxYawRate > 0.0D && !vehicle.onGround()
                && Math.abs(input.yaw) > 1.0E-4F) {
            vehicle.setYRot(vehicle.getYRot() + (float) (input.yaw * maxYawRate * 0.05D));
        }
        vehicle.mouseInput(mouseYaw, input.pitch * pitchScale);
        applyRollKeyPulse(vehicle, input.roll);
    }

    /**
     * 直升机 A/D 偏航用原版 mouseMoveSpeedX，并按旧公式反算保持相同手感：
     * SW 引擎 yRot += yawSpeed * clamp(2 * mouseX * synchedPropellerRot, -10, 10)
     * 期望每 tick 偏航 = yaw * maxYawRate * 0.05
     * ⇒ mouseX = yaw * maxYawRate / (40 * synchedPropellerRot)
     */
    private static double helicopterMouseYaw(VehicleEntity vehicle, float yaw, double maxYawRate) {
        if (vehicle.getVehicleType() != VehicleType.HELICOPTER) {
            return 0.0D;
        }
        double propeller = Math.max((double) vehicle.getSynchedPropellerRot(), 0.25D);
        return yaw * (maxYawRate / 40.0D) / propeller;
    }

    /** 鼠标滚转 → 原版 A/D 键，6 tick 脉宽调制近似模拟模拟量。 */
    private static void applyRollKeyPulse(VehicleEntity vehicle, float roll) {
        int phase = ROLL_PULSE.getOrDefault(vehicle.getId(), 0);
        ROLL_PULSE.put(vehicle.getId(), (phase + 1) % 6);
        int duty = (int) Math.round(Math.abs(roll) * 6.0D);
        vehicle.setRightInputDown(roll > 1.0E-4F && phase < duty);
        vehicle.setLeftInputDown(roll < -1.0E-4F && phase < duty);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !player.level().isClientSide) {
            ControlNetwork.sendSupport(player, probe(), allowedMask());
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            INPUTS.remove(player.getUUID());
            if (player.getVehicle() instanceof VehicleEntity vehicle) {
                CENTER_AIM_DIRS.remove(vehicle.getId());
                ROLL_PULSE.remove(vehicle.getId());
            }
        }
    }

    public static void reset() {
        INPUTS.clear();
        CENTER_AIM_DIRS.clear();
        ROLL_PULSE.clear();
        lastAnnouncedMaxYawRate = Double.NaN;
    }

    /** 供命令/调试查询：当前缓存了多少玩家的控制输入。 */
    public static int inputCount() {
        return INPUTS.size();
    }
}
