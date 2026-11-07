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
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
    private static int logCounter = 0;
    private static Boolean apiAvailable = null;

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

    public static void accept(ServerPlayer player, CtlPacket packet) {
        if (player == null) {
            return;
        }
        long now = player.serverLevel().getGameTime();
        if (!packet.active) {
            INPUTS.remove(player.getUUID());
            return;
        }
        INPUTS.put(player.getUUID(), new RemoteInput(packet.pitch, packet.roll, packet.yaw,
                packet.scheme, true, now));
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

            if (log && (logCounter++ % 20 == 0)) {
                SbwControl.LOGGER.info("[sbwcontrol] apply {} scheme={} pitch={} roll={} yaw={}",
                        player.getGameProfile().getName(), input.scheme,
                        input.pitch, input.roll, input.yaw);
            }
        }
    }

    /**
     * 把一个三通道输入写进卓越前线载具的公开输入字段（自测也直接调用这个方法）。
     *
     * <p>方向约定（已按实机反馈校正）：客户端通道正方向 = 鼠标右/抬头/机头右偏；
     * 卓越前线的 deltaRot 对机体滚转是反向作用（正 deltaRot 使飞机向左压坡度），
     * 因此这里取负号，保证“鼠标向右 = 向右滚转”。
     */
    public static void applyToVehicle(VehicleEntity vehicle, RemoteInput input) {
        double yawScale = ServerConfig.YAW_SCALE.get();
        double pitchScale = ServerConfig.PITCH_SCALE.get();
        double rollScale = ServerConfig.ROLL_SCALE.get();
        double extraYawRate = ServerConfig.EXTRA_YAW_RATE.get();
        vehicle.mouseInput(input.yaw * yawScale, input.pitch * pitchScale);
        vehicle.setDeltaRot((float) (-input.roll * rollScale));
        // 卓越前线的偏航项被 rotSpeed 钳制（平飞约 6°/s），A/D 偏航几乎没感觉；
        // 这里再直接转 yRot，提供一个可配置的角速度（默认 30°/s）。地面交给原版滑行逻辑。
        if (extraYawRate > 0.0D && !vehicle.onGround() && Math.abs(input.yaw) > 1.0E-4F) {
            vehicle.setYRot(vehicle.getYRot() + (float) (input.yaw * extraYawRate * 0.05D));
        }
        // 清理 A/D 输入位，避免卓越前线再把按键解释成一次滚转
        vehicle.setLeftInputDown(false);
        vehicle.setRightInputDown(false);
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
        }
    }

    public static void reset() {
        INPUTS.clear();
    }

    /** 供命令/调试查询：当前缓存了多少玩家的控制输入。 */
    public static int inputCount() {
        return INPUTS.size();
    }
}
