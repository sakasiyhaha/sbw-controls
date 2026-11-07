package com.sakasiyhaha.sbwcontrol;

import com.mojang.logging.LogUtils;
import com.sakasiyhaha.sbwcontrol.config.ClientConfig;
import com.sakasiyhaha.sbwcontrol.config.ServerConfig;
import com.sakasiyhaha.sbwcontrol.network.ControlNetwork;
import com.sakasiyhaha.sbwcontrol.server.ServerControlHandler;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

/**
 * 卓越前线飞行操控附属（SBW Controls）。
 *
 * <p>职责边界：
 * <ul>
 *   <li>client 包：输入映射、动态相机、HUD、设置界面。仅在客户端加载。</li>
 *   <li>server 包：接收客户端的控制通道数据，在服务端 tick 开头写进卓越前线载具的输入字段。</li>
 *   <li>common 包：配置模型与网络协议，双端都安全。</li>
 * </ul>
 *
 * <p>设计要点：本附属需要修改的是卓越前线载具的“输入解释方式”，但通过其公开的
 * {@code mouseInput / setDeltaRot / setLeftInputDown / setRightInputDown} 字段接口实现，
 * 不使用 Mixin / 字节码注入，从而对卓越前线版本更新更稳。
 */
@Mod(SbwControl.MODID)
public final class SbwControl {

    public static final String MODID = "sbwcontrol";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SbwControl() {
        // 客户端配置：控制方案、轴绑定、手感、相机、HUD
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC, "sbwcontrol-client.toml");
        // 服务端配置：允许的方案、服务端应用增益、输入超时
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC, "sbwcontrol-server.toml");

        ControlNetwork.init();
        MinecraftForge.EVENT_BUS.register(ServerControlHandler.class);
        MinecraftForge.EVENT_BUS.register(com.sakasiyhaha.sbwcontrol.server.ServerCommands.class);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> com.sakasiyhaha.sbwcontrol.client.ClientSetup::init);
        }

        // 无头自测模式：gradlew -Dsbwcontrol.selftest=true runServer
        if (FMLEnvironment.dist == Dist.DEDICATED_SERVER && Boolean.getBoolean("sbwcontrol.selftest")) {
            MinecraftForge.EVENT_BUS.register(com.sakasiyhaha.sbwcontrol.server.SelfTestRunner.class);
            LOGGER.info("[sbwcontrol] headless self-test mode enabled");
        }

        LOGGER.info("[sbwcontrol] initialized ({})", FMLEnvironment.dist);
    }
}
