package com.sakasiyhaha.sbwcontrol.client;

import com.sakasiyhaha.sbwcontrol.config.ClientConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** 所有设置子界面的基类：统一返回、自动保存配置、标题渲染。 */
@OnlyIn(Dist.CLIENT)
public abstract class AbstractSbwScreen extends Screen {

    protected final Screen parent;

    protected AbstractSbwScreen(Screen parent, Component title) {
        super(title);
        this.parent = parent;
    }

    protected void addDoneButton(int centerX, int y) {
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(centerX - 100, y, 200, 20).build());
    }

    protected void addBackButton(int centerX, int y) {
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> onClose())
                .bounds(centerX - 100, y, 200, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 16, 0xFFFFFF);
    }

    @Override
    public void onClose() {
        ClientConfig.SPEC.save();
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }
}
