package com.sakasiyhaha.sbwcontrol.client;

import com.sakasiyhaha.sbwcontrol.SbwControl;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;

/** 客户端初始化：注册 Mod 配置界面入口（Mods 列表 → Config），其余客户端事件用注解自动注册。 */
@OnlyIn(Dist.CLIENT)
public final class ClientSetup {

    private ClientSetup() {
    }

    public static void init() {
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> new SbwConfigScreen(parent)));
        SbwControl.LOGGER.info("[sbwcontrol] client setup done");
    }
}
