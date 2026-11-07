package com.sakasiyhaha.sbwcontrol.key;

import com.atsuishio.superbwarfare.init.ModKeyMappings;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import com.sakasiyhaha.sbwcontrol.SbwControl;

/**
 * 本附属注册的全部按键。全部可以在“选项 → 控制 → 按键绑定”里重新绑定，
 * 分类为 {@code key.categories.sbwcontrol}。
 *
 * <p>默认值约定：
 * <ul>
 *   <li>偏航左/右 = A/D（翻滚鸟预设使用；与卓越前线自带的 A/D 滚转键同名，由本附属在
 *       方案激活时压制卓越前线那两个按键的状态，避免同时触发滚转）。</li>
 *   <li>滚转左/右 = Q/E（自定义方案/备用轴）。</li>
 *   <li>切换方案 = B；其余默认未绑定，避免与其它 mod 冲突，玩家自行设置。</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = SbwControl.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ModKeys {

    public static final String CATEGORY = "key.categories.sbwcontrol";

    public static final KeyMapping YAW_LEFT = key("yaw_left", GLFW.GLFW_KEY_A);
    public static final KeyMapping YAW_RIGHT = key("yaw_right", GLFW.GLFW_KEY_D);
    public static final KeyMapping ROLL_LEFT = key("roll_left", GLFW.GLFW_KEY_Q);
    public static final KeyMapping ROLL_RIGHT = key("roll_right", GLFW.GLFW_KEY_E);
    public static final KeyMapping PITCH_UP = key("pitch_up", InputConstants.UNKNOWN.getValue());
    public static final KeyMapping PITCH_DOWN = key("pitch_down", InputConstants.UNKNOWN.getValue());

    public static final KeyMapping TOGGLE_SCHEME = key("toggle_scheme", GLFW.GLFW_KEY_B);
    public static final KeyMapping TOGGLE_CAMERA = key("toggle_camera", InputConstants.UNKNOWN.getValue());
    public static final KeyMapping OPEN_CONFIG = key("open_config", InputConstants.UNKNOWN.getValue());

    private ModKeys() {
    }

    private static KeyMapping key(String name, int code) {
        return new KeyMapping("key.sbwcontrol." + name, KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM, code, CATEGORY);
    }

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(YAW_LEFT);
        event.register(YAW_RIGHT);
        event.register(ROLL_LEFT);
        event.register(ROLL_RIGHT);
        event.register(PITCH_UP);
        event.register(PITCH_DOWN);
        event.register(TOGGLE_SCHEME);
        event.register(TOGGLE_CAMERA);
        event.register(OPEN_CONFIG);
    }

    /**
     * 压制卓越前线自带的 A/D 载具键，使“翻滚鸟”方案下按 A/D 只会被本附属读作偏航，
     * 不会再让卓越前线把它当成滚转输入（否则服务端会同时滚转）。
     *
     * <p>必须在卓越前线自己的 ClientTickEvent 处理器之后调用（本附属用 LOWEST 优先级）。
     */
    public static void suppressSwVehicleStrafeKeys() {
        ModKeyMappings.MOVE_LEFT.setDown(false);
        ModKeyMappings.MOVE_RIGHT.setDown(false);
    }
}


