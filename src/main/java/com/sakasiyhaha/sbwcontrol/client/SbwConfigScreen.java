package com.sakasiyhaha.sbwcontrol.client;

import com.sakasiyhaha.sbwcontrol.camera.CameraPreset;
import com.sakasiyhaha.sbwcontrol.client.widget.ConfigSlider;
import com.sakasiyhaha.sbwcontrol.client.widget.EnumButton;
import com.sakasiyhaha.sbwcontrol.config.ClientConfig;
import com.sakasiyhaha.sbwcontrol.config.ServerConfig;
import com.sakasiyhaha.sbwcontrol.control.AxisSource;
import com.sakasiyhaha.sbwcontrol.control.ClientSupport;
import com.sakasiyhaha.sbwcontrol.control.ControlScheme;
import com.sakasiyhaha.sbwcontrol.control.InputMode;
import com.sakasiyhaha.sbwcontrol.key.ModKeys;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.controls.KeyBindsScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleFunction;

/**
 * 统一设置窗口：控制 / 相机 / 操作 / 服务端 / 诊断。
 *
 * <p>所有参数只在这里出现一次；相机内部再分 基础 / 动态 / 第一人称·准星 三个子页。
 * 按键不在本模组内单独做页：9 个快捷键已注册到原版“选项 → 控制 → 按键绑定”，
 * 这里只提供入口和恢复默认。
 */
@OnlyIn(Dist.CLIENT)
public final class SbwConfigScreen extends AbstractSbwScreen {

    private enum Tab {
        CONTROL("control"),
        CAMERA("camera"),
        OPERATION("operation"),
        SERVER("server"),
        DIAGNOSTICS("diagnostics");

        final String key;

        Tab(String key) {
            this.key = key;
        }
    }

    private enum CameraTab {
        BASE("base"),
        DYNAMICS("dynamics"),
        FIRST_PERSON("first_person");

        final String key;

        CameraTab(String key) {
            this.key = key;
        }
    }

    private Tab tab = Tab.CONTROL;
    private CameraTab cameraTab = CameraTab.BASE;
    private EnumButton<ControlScheme> aircraftSchemeButton;
    private EnumButton<ControlScheme> helicopterSchemeButton;
    private EnumButton<AxisSource> customPitchButton;
    private EnumButton<AxisSource> customRollButton;
    private EnumButton<AxisSource> customYawButton;

    public SbwConfigScreen(Screen parent) {
        super(parent, Component.translatable("screen.sbwcontrol.title"));
    }

    private void switchTab(Tab target) {
        if (tab == target) {
            return;
        }
        tab = target;
        rebuildWidgets();
    }

    private void switchCameraTab(CameraTab target) {
        if (cameraTab == target) {
            return;
        }
        cameraTab = target;
        rebuildWidgets();
    }

    @Override
    protected void init() {
        aircraftSchemeButton = null;
        helicopterSchemeButton = null;
        customPitchButton = null;
        customRollButton = null;
        customYawButton = null;
        int cx = width / 2;
        int tabW = 58;
        int gap = 4;
        int total = Tab.values().length * tabW + (Tab.values().length - 1) * gap;
        int tabX = cx - total / 2;
        int tabY = 28;
        for (Tab value : Tab.values()) {
            final Tab target = value;
            Button button = Button.builder(Component.translatable("screen.sbwcontrol.tab." + value.key),
                    b -> switchTab(target)).bounds(tabX, tabY, tabW, 18).build();
            button.active = tab != target;
            addRenderableWidget(button);
            tabX += tabW + gap;
        }

        int contentTop = 56;
        if (tab == Tab.CAMERA) {
            int camW = 68;
            int camGap = 4;
            int camTotal = CameraTab.values().length * camW + (CameraTab.values().length - 1) * camGap;
            int camX = cx - camTotal / 2;
            int camY = 49;
            for (CameraTab value : CameraTab.values()) {
                final CameraTab target = value;
                Button button = Button.builder(Component.translatable(
                                "screen.sbwcontrol.camtab." + value.key),
                        b -> switchCameraTab(target)).bounds(camX, camY, camW, 18).build();
                button.active = cameraTab != target;
                addRenderableWidget(button);
                camX += camW + camGap;
            }
            contentTop = 74;
        }

        switch (tab) {
            case CONTROL -> buildControl(cx);
            case CAMERA -> buildCamera(cx, contentTop);
            case OPERATION -> buildOperation(cx, contentTop);
            case SERVER -> buildServer(cx, contentTop);
            case DIAGNOSTICS -> buildDiagnostics(cx, contentTop);
        }

        addRenderableWidget(Button.builder(Component.translatable("screen.sbwcontrol.reset_page"),
                        b -> resetPage())
                .bounds(cx - 155, height - 28, 100, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(cx - 50, height - 28, 100, 20).build());
    }

    private void buildControl(int cx) {
        int left = cx - 155;
        int right = cx + 5;
        int w = 150;
        int row = 56;
        int step = 20;

        aircraftSchemeButton = new EnumButton<>(left, row, w, 20,
                Component.translatable("screen.sbwcontrol.aircraft_default"),
                ClientConfig.AIRCRAFT_SCHEME.get(), ControlScheme.values(),
                ClientConfig.AIRCRAFT_SCHEME::set, SbwConfigScreen::schemeName);
        addRenderableWidget(aircraftSchemeButton);
        helicopterSchemeButton = new EnumButton<>(left, row + step, w, 20,
                Component.translatable("screen.sbwcontrol.helicopter_default"),
                ClientConfig.HELICOPTER_SCHEME.get(), ControlScheme.values(),
                ClientConfig.HELICOPTER_SCHEME::set, SbwConfigScreen::schemeName);
        addRenderableWidget(helicopterSchemeButton);

        Button other = Button.builder(Component.translatable("screen.sbwcontrol.other_vehicles"), b -> {
        }).bounds(left, row + step * 2, w, 20).build();
        other.active = false;
        addRenderableWidget(other);

        boolean custom = ClientConfig.AIRCRAFT_SCHEME.get() == ControlScheme.CUSTOM
                || ClientConfig.HELICOPTER_SCHEME.get() == ControlScheme.CUSTOM;
        customPitchButton = new EnumButton<>(left, row + step * 3, w, 20,
                Component.translatable("screen.sbwcontrol.custom_pitch"),
                ClientConfig.CUSTOM_PITCH_SOURCE.get(), AxisSource.values(),
                ClientConfig.CUSTOM_PITCH_SOURCE::set, SbwConfigScreen::axisName);
        customPitchButton.active = custom;
        addRenderableWidget(customPitchButton);
        customRollButton = new EnumButton<>(left, row + step * 4, w, 20,
                Component.translatable("screen.sbwcontrol.custom_roll"),
                ClientConfig.CUSTOM_ROLL_SOURCE.get(), AxisSource.values(),
                ClientConfig.CUSTOM_ROLL_SOURCE::set, SbwConfigScreen::axisName);
        customRollButton.active = custom;
        addRenderableWidget(customRollButton);
        customYawButton = new EnumButton<>(left, row + step * 5, w, 20,
                Component.translatable("screen.sbwcontrol.custom_yaw"),
                ClientConfig.CUSTOM_YAW_SOURCE.get(), AxisSource.values(),
                ClientConfig.CUSTOM_YAW_SOURCE::set, SbwConfigScreen::axisName);
        customYawButton.active = custom;
        addRenderableWidget(customYawButton);
    }

    @Override
    public void tick() {
        super.tick();
        // B 键/命令/TOML 改动后，设置页同步显示；反之设置页修改也会被 B 键从同一配置读走。
        if (aircraftSchemeButton != null) {
            aircraftSchemeButton.setCurrent(ClientConfig.AIRCRAFT_SCHEME.get());
        }
        if (helicopterSchemeButton != null) {
            helicopterSchemeButton.setCurrent(ClientConfig.HELICOPTER_SCHEME.get());
        }
        boolean custom = ClientConfig.AIRCRAFT_SCHEME.get() == ControlScheme.CUSTOM
                || ClientConfig.HELICOPTER_SCHEME.get() == ControlScheme.CUSTOM;
        if (customPitchButton != null) {
            customPitchButton.active = custom;
            customRollButton.active = custom;
            customYawButton.active = custom;
        }
    }

    private void buildCamera(int cx, int row) {
        int left = cx - 155;
        int right = cx + 5;
        int w = 150;
        int step = 20;

        switch (cameraTab) {
            case BASE -> {
                addRenderableWidget(toggle(left, row, w, "screen.sbwcontrol.camera",
                        ClientConfig.CAMERA_ENABLED::get, ClientConfig.CAMERA_ENABLED::set));
                addRenderableWidget(new EnumButton<>(left, row + step, w, 20,
                        Component.translatable("screen.sbwcontrol.camera_preset"),
                        ClientConfig.CAMERA_PRESET.get(), CameraPreset.values(),
                        ClientConfig.CAMERA_PRESET::set, SbwConfigScreen::presetName));
                addRenderableWidget(slider(left, row + step * 2, w, "screen.sbwcontrol.lag_yaw",
                        0.0, 1.0, ClientConfig.CAMERA_LAG_YAW.get(),
                        ClientConfig.CAMERA_LAG_YAW::set, sec()));
                addRenderableWidget(slider(left, row + step * 3, w, "screen.sbwcontrol.lag_pitch",
                        0.0, 1.0, ClientConfig.CAMERA_LAG_PITCH.get(),
                        ClientConfig.CAMERA_LAG_PITCH::set, sec()));
                addRenderableWidget(slider(left, row + step * 4, w, "screen.sbwcontrol.lead",
                        0.0, 0.6, ClientConfig.CAMERA_LEAD.get(),
                        ClientConfig.CAMERA_LEAD::set, sec()));

                addRenderableWidget(slider(right, row, w, "screen.sbwcontrol.fov_gain",
                        0.0, 0.5, ClientConfig.CAMERA_FOV_GAIN.get(),
                        ClientConfig.CAMERA_FOV_GAIN::set, num()));
                addRenderableWidget(slider(right, row + step, w, "screen.sbwcontrol.distance_gain",
                        0.0, 2.0, ClientConfig.CAMERA_DISTANCE_GAIN.get(),
                        ClientConfig.CAMERA_DISTANCE_GAIN::set, num()));
                addRenderableWidget(slider(right, row + step * 2, w, "screen.sbwcontrol.camera_ads",
                        0.0, 1.0, ClientConfig.CAMERA_ADS_SCALE.get(),
                        ClientConfig.CAMERA_ADS_SCALE::set, num()));
                addRenderableWidget(slider(right, row + step * 3, w, "screen.sbwcontrol.camera_roll_ratio",
                        0.0, 1.5, ClientConfig.CAMERA_ROLL_RATIO.get(),
                        ClientConfig.CAMERA_ROLL_RATIO::set, num()));
                addRenderableWidget(slider(right, row + step * 4, w, "screen.sbwcontrol.center_lateral",
                        0.0, 1.0, ClientConfig.CAMERA_CENTER_LATERAL.get(),
                        ClientConfig.CAMERA_CENTER_LATERAL::set, num()));
            }
            case DYNAMICS -> {
                addRenderableWidget(toggle(left, row, w, "screen.sbwcontrol.turn_lag_invert",
                        ClientConfig.CAMERA_TURN_LAG_INVERT::get, ClientConfig.CAMERA_TURN_LAG_INVERT::set));
                addRenderableWidget(toggle(left, row + step, w, "screen.sbwcontrol.turn_lag_invert_pitch",
                        ClientConfig.CAMERA_TURN_LAG_INVERT_PITCH::get, ClientConfig.CAMERA_TURN_LAG_INVERT_PITCH::set));
                addRenderableWidget(slider(left, row + step * 2, w, "screen.sbwcontrol.roll_lag",
                        0.0, 0.5, ClientConfig.CAMERA_LAG_ROLL.get(),
                        ClientConfig.CAMERA_LAG_ROLL::set, sec()));
                addRenderableWidget(slider(left, row + step * 3, w, "screen.sbwcontrol.position_lag",
                        0.0, 0.3, ClientConfig.CAMERA_POSITION_LAG.get(),
                        ClientConfig.CAMERA_POSITION_LAG::set, sec()));
                addRenderableWidget(slider(left, row + step * 4, w, "screen.sbwcontrol.spring_damping",
                        0.5, 1.5, ClientConfig.CAMERA_SPRING_DAMPING.get(),
                        ClientConfig.CAMERA_SPRING_DAMPING::set, num()));
                addRenderableWidget(slider(right, row, w, "screen.sbwcontrol.adaptive",
                        0.0, 5.0, ClientConfig.CAMERA_ADAPTIVE_GAIN.get(),
                        ClientConfig.CAMERA_ADAPTIVE_GAIN::set, num()));
                addRenderableWidget(slider(right, row + step, w, "screen.sbwcontrol.rate_smoothing",
                        0.0, 0.3, ClientConfig.CAMERA_RATE_SMOOTHING.get(),
                        ClientConfig.CAMERA_RATE_SMOOTHING::set, sec()));
                addRenderableWidget(slider(right, row + step * 2, w, "screen.sbwcontrol.soft_limit",
                        0.0, 1.0, ClientConfig.CAMERA_SOFT_LIMIT.get(),
                        ClientConfig.CAMERA_SOFT_LIMIT::set, num()));
            }
            case FIRST_PERSON -> {
                addRenderableWidget(toggle(left, row, w, "screen.sbwcontrol.first_person",
                        ClientConfig.CAMERA_FIRST_PERSON::get, ClientConfig.CAMERA_FIRST_PERSON::set));
                addRenderableWidget(slider(left, row + step, w, "screen.sbwcontrol.fp_lag_yaw",
                        0.0, 0.5, ClientConfig.CAMERA_FP_LAG_YAW.get(),
                        ClientConfig.CAMERA_FP_LAG_YAW::set, sec()));
                addRenderableWidget(slider(left, row + step * 2, w, "screen.sbwcontrol.fp_lag_pitch",
                        0.0, 0.5, ClientConfig.CAMERA_FP_LAG_PITCH.get(),
                        ClientConfig.CAMERA_FP_LAG_PITCH::set, sec()));
                addRenderableWidget(slider(left, row + step * 3, w, "screen.sbwcontrol.fp_turn_lag",
                        0.0, 0.3, ClientConfig.CAMERA_FP_TURN_LAG.get(),
                        ClientConfig.CAMERA_FP_TURN_LAG::set, sec()));
                addRenderableWidget(slider(left, row + step * 4, w, "screen.sbwcontrol.fp_roll_lag",
                        0.0, 0.3, ClientConfig.CAMERA_FP_ROLL_LAG.get(),
                        ClientConfig.CAMERA_FP_ROLL_LAG::set, sec()));
                addRenderableWidget(slider(right, row, w, "screen.sbwcontrol.fp_position_lag",
                        0.0, 0.2, ClientConfig.CAMERA_FP_POSITION_LAG.get(),
                        ClientConfig.CAMERA_FP_POSITION_LAG::set, sec()));
                addRenderableWidget(slider(right, row + step, w, "screen.sbwcontrol.fp_adaptive",
                        0.0, 3.0, ClientConfig.CAMERA_FP_ADAPTIVE_GAIN.get(),
                        ClientConfig.CAMERA_FP_ADAPTIVE_GAIN::set, num()));
                addRenderableWidget(toggle(right, row + step * 2, w, "screen.sbwcontrol.nose_marker",
                        ClientConfig.CAMERA_NOSE_MARKER::get, ClientConfig.CAMERA_NOSE_MARKER::set));
                addRenderableWidget(toggle(right, row + step * 3, w, "screen.sbwcontrol.replace_crosshair",
                        ClientConfig.CAMERA_REPLACE_CROSSHAIR::get, ClientConfig.CAMERA_REPLACE_CROSSHAIR::set));
            }
        }
    }

    private void buildOperation(int cx, int row) {
        int left = cx - 155;
        int right = cx + 5;
        int w = 150;
        int step = 19;

        addRenderableWidget(new EnumButton<>(left, row, w, 20,
                Component.translatable("screen.sbwcontrol.input_mode"),
                ClientConfig.INPUT_MODE.get(), InputMode.values(),
                ClientConfig.INPUT_MODE::set, mode -> Component.translatable("screen.sbwcontrol.mode." + mode.name())));
        addRenderableWidget(slider(left, row + step, w, "screen.sbwcontrol.sensitivity",
                10, 300, ClientConfig.MOUSE_SENSITIVITY.get(),
                value -> ClientConfig.MOUSE_SENSITIVITY.set((int) Math.round(value)),
                value -> String.format("%.0f%%", value)));
        addRenderableWidget(slider(left, row + step * 2, w, "screen.sbwcontrol.smoothing",
                0.0, 1.0, ClientConfig.SMOOTHING.get(),
                ClientConfig.SMOOTHING::set, sec()));
        addRenderableWidget(slider(left, row + step * 3, w, "screen.sbwcontrol.deadzone",
                0.0, 0.5, ClientConfig.DEADZONE.get(),
                ClientConfig.DEADZONE::set, num()));
        addRenderableWidget(slider(left, row + step * 4, w, "screen.sbwcontrol.pitch_gain",
                0.1, 5.0, ClientConfig.PITCH_GAIN.get(),
                ClientConfig.PITCH_GAIN::set, num()));
        addRenderableWidget(slider(left, row + step * 5, w, "screen.sbwcontrol.roll_gain",
                0.1, 5.0, ClientConfig.ROLL_GAIN.get(),
                ClientConfig.ROLL_GAIN::set, num()));
        addRenderableWidget(slider(left, row + step * 6, w, "screen.sbwcontrol.yaw_gain",
                0.1, 5.0, ClientConfig.YAW_GAIN.get(),
                ClientConfig.YAW_GAIN::set, num()));

        addRenderableWidget(slider(right, row, w, "screen.sbwcontrol.hybrid_mouse_yaw",
                0.0, 1.0, ClientConfig.HYBRID_MOUSE_YAW.get(),
                ClientConfig.HYBRID_MOUSE_YAW::set, num()));
        addRenderableWidget(slider(right, row + step, w, "screen.sbwcontrol.stick_center",
                0.0, 5.0, ClientConfig.STICK_CENTER_TIME.get(),
                ClientConfig.STICK_CENTER_TIME::set, sec()));
        addRenderableWidget(slider(right, row + step * 2, w, "screen.sbwcontrol.stick_gain",
                0.1, 5.0, ClientConfig.STICK_GAIN.get(),
                ClientConfig.STICK_GAIN::set, num()));
        addRenderableWidget(toggle(right, row + step * 3, w, "screen.sbwcontrol.invert_pitch",
                ClientConfig.INVERT_PITCH::get, ClientConfig.INVERT_PITCH::set));
        addRenderableWidget(toggle(right, row + step * 4, w, "screen.sbwcontrol.invert_roll",
                ClientConfig.INVERT_ROLL::get, ClientConfig.INVERT_ROLL::set));
        addRenderableWidget(toggle(right, row + step * 5, w, "screen.sbwcontrol.invert_yaw",
                ClientConfig.INVERT_YAW::get, ClientConfig.INVERT_YAW::set));
        addRenderableWidget(toggle(right, row + step * 6, w, "screen.sbwcontrol.invert_mouse_y",
                ClientConfig.INVERT_MOUSE_Y::get, ClientConfig.INVERT_MOUSE_Y::set));

        addRenderableWidget(Button.builder(Component.translatable("screen.sbwcontrol.open_keys"), b -> {
            if (minecraft != null) {
                minecraft.setScreen(new KeyBindsScreen(this, minecraft.options));
            }
        }).bounds(left, row + step * 7, w, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.sbwcontrol.reset_keys"),
                b -> resetKeybinds()).bounds(right, row + step * 7, w, 20).build());
    }

    private void buildServer(int cx, int row) {
        int left = cx - 155;
        int right = cx + 5;
        int w = 150;
        int step = 20;        boolean available = minecraft != null && minecraft.hasSingleplayerServer();
        if (!available) {
            return;
        }

        addRenderableWidget(slider(left, row, w, "screen.sbwcontrol.extra_yaw_rate",
                0.0, 180.0, ServerConfig.EXTRA_YAW_RATE.get(),
                ServerConfig.EXTRA_YAW_RATE::set, num()));
        addRenderableWidget(slider(left, row + step, w, "screen.sbwcontrol.yaw_scale",
                0.1, 64.0, ServerConfig.YAW_SCALE.get(),
                ServerConfig.YAW_SCALE::set, num()));
        addRenderableWidget(slider(left, row + step * 2, w, "screen.sbwcontrol.roll_scale",
                0.01, 20.0, ServerConfig.ROLL_SCALE.get(),
                ServerConfig.ROLL_SCALE::set, num()));
        addRenderableWidget(slider(left, row + step * 3, w, "screen.sbwcontrol.pitch_scale",
                0.1, 64.0, ServerConfig.PITCH_SCALE.get(),
                ServerConfig.PITCH_SCALE::set, num()));
        addRenderableWidget(slider(left, row + step * 4, w, "screen.sbwcontrol.input_timeout",
                1, 100, ServerConfig.INPUT_TIMEOUT_TICKS.get(),
                value -> ServerConfig.INPUT_TIMEOUT_TICKS.set((int) Math.round(value)),
                value -> String.format("%.0f", value)));

        addRenderableWidget(toggle(right, row, w, "screen.sbwcontrol.allow_roll_bird",
                ServerConfig.ALLOW_ROLL_BIRD::get, ServerConfig.ALLOW_ROLL_BIRD::set));
        addRenderableWidget(toggle(right, row + step, w, "screen.sbwcontrol.allow_hybrid",
                ServerConfig.ALLOW_ROLL_YAW_HYBRID::get, ServerConfig.ALLOW_ROLL_YAW_HYBRID::set));
        addRenderableWidget(toggle(right, row + step * 2, w, "screen.sbwcontrol.allow_custom",
                ServerConfig.ALLOW_CUSTOM::get, ServerConfig.ALLOW_CUSTOM::set));
        addRenderableWidget(toggle(right, row + step * 3, w, "screen.sbwcontrol.apply_airplane",
                ServerConfig.APPLY_AIRPLANE::get, ServerConfig.APPLY_AIRPLANE::set));
        addRenderableWidget(toggle(right, row + step * 4, w, "screen.sbwcontrol.apply_helicopter",
                ServerConfig.APPLY_HELICOPTER::get, ServerConfig.APPLY_HELICOPTER::set));
        addRenderableWidget(toggle(right, row + step * 5, w, "screen.sbwcontrol.log_apply",
                ServerConfig.LOG_APPLY::get, ServerConfig.LOG_APPLY::set));
    }

    private void buildDiagnostics(int cx, int row) {
        int left = cx - 155;
        int right = cx + 5;
        int w = 150;
        int step = 20;

        addRenderableWidget(toggle(left, row, w, "screen.sbwcontrol.debug_hud",
                ClientConfig.DEBUG_HUD::get, ClientConfig.DEBUG_HUD::set));
        addRenderableWidget(toggle(left, row + step, w, "screen.sbwcontrol.telemetry",
                ClientConfig.TELEMETRY_ENABLED::get, ClientConfig.TELEMETRY_ENABLED::set));
        addRenderableWidget(slider(left, row + step * 2, w, "screen.sbwcontrol.telemetry_interval",
                1, 20, ClientConfig.TELEMETRY_INTERVAL_TICKS.get(),
                value -> ClientConfig.TELEMETRY_INTERVAL_TICKS.set((int) Math.round(value)),
                value -> String.format("%.0f tick", value)));
    }

    private void resetPage() {
        switch (tab) {
            case CONTROL -> {
                ClientConfig.AIRCRAFT_SCHEME.set(ControlScheme.ROLL_BIRD);
                ClientConfig.HELICOPTER_SCHEME.set(ControlScheme.ROLL_BIRD);
                ClientConfig.CUSTOM_PITCH_SOURCE.set(AxisSource.MOUSE_Y);
                ClientConfig.CUSTOM_ROLL_SOURCE.set(AxisSource.MOUSE_X);
                ClientConfig.CUSTOM_YAW_SOURCE.set(AxisSource.KEYS);
                ClientConfig.SPEC.save();
            }
            case CAMERA -> {
                switch (cameraTab) {
                    case BASE -> {
                        ClientConfig.CAMERA_ENABLED.set(true);
                        ClientConfig.CAMERA_PRESET.set(CameraPreset.BATTLEFIELD);
                        ClientConfig.CAMERA_LAG_YAW.set(0.10D);
                        ClientConfig.CAMERA_LAG_PITCH.set(0.08D);
                        ClientConfig.CAMERA_LEAD.set(0.22D);
                        ClientConfig.CAMERA_FOV_GAIN.set(0.10D);
                        ClientConfig.CAMERA_DISTANCE_GAIN.set(0.5D);
                        ClientConfig.CAMERA_ADS_SCALE.set(0.35D);
                        ClientConfig.CAMERA_ROLL_RATIO.set(1.0D);
                        ClientConfig.CAMERA_CENTER_LATERAL.set(1.0D);
                    }
                    case DYNAMICS -> {
                        ClientConfig.CAMERA_TURN_LAG_INVERT.set(false);
                        ClientConfig.CAMERA_TURN_LAG_INVERT_PITCH.set(false);
                        ClientConfig.CAMERA_LAG_ROLL.set(0.10D);
                        ClientConfig.CAMERA_POSITION_LAG.set(0.08D);
                        ClientConfig.CAMERA_SPRING_DAMPING.set(0.90D);
                        ClientConfig.CAMERA_RATE_SMOOTHING.set(0.10D);
                        ClientConfig.CAMERA_SOFT_LIMIT.set(1.0D);
                        ClientConfig.CAMERA_ADAPTIVE_GAIN.set(1.5D);
                    }
                    case FIRST_PERSON -> {
                        ClientConfig.CAMERA_FIRST_PERSON.set(false);
                        ClientConfig.CAMERA_FP_LAG_YAW.set(0.03D);
                        ClientConfig.CAMERA_FP_LAG_PITCH.set(0.03D);
                        ClientConfig.CAMERA_FP_TURN_LAG.set(0.0D);
                        ClientConfig.CAMERA_FP_ROLL_LAG.set(0.0D);
                        ClientConfig.CAMERA_FP_POSITION_LAG.set(0.0D);
                        ClientConfig.CAMERA_FP_ADAPTIVE_GAIN.set(0.0D);
                        ClientConfig.CAMERA_NOSE_MARKER.set(false);
                        ClientConfig.CAMERA_REPLACE_CROSSHAIR.set(false);
                    }
                }
                ClientConfig.SPEC.save();
            }
            case OPERATION -> {
                ClientConfig.INPUT_MODE.set(InputMode.STICK);
                ClientConfig.STICK_CENTER_TIME.set(1.2D);
                ClientConfig.STICK_GAIN.set(1.0D);
                ClientConfig.MOUSE_SENSITIVITY.set(100);
                ClientConfig.SMOOTHING.set(0.08D);
                ClientConfig.DEADZONE.set(0.02D);
                ClientConfig.PITCH_GAIN.set(1.0D);
                ClientConfig.ROLL_GAIN.set(1.0D);
                ClientConfig.YAW_GAIN.set(1.0D);
                ClientConfig.HYBRID_MOUSE_YAW.set(0.25D);
                ClientConfig.INVERT_PITCH.set(false);
                ClientConfig.INVERT_ROLL.set(false);
                ClientConfig.INVERT_YAW.set(false);
                ClientConfig.INVERT_MOUSE_Y.set(false);
                ClientConfig.SPEC.save();
            }
            case SERVER -> {
                if (minecraft == null || !minecraft.hasSingleplayerServer()) {
                    return;
                }
                ServerConfig.EXTRA_YAW_RATE.set(30.0D);
                ServerConfig.YAW_SCALE.set(4.0D);
                ServerConfig.ROLL_SCALE.set(6.0D);
                ServerConfig.PITCH_SCALE.set(20.0D);
                ServerConfig.INPUT_TIMEOUT_TICKS.set(5);
                ServerConfig.ALLOW_ROLL_BIRD.set(true);
                ServerConfig.ALLOW_ROLL_YAW_HYBRID.set(true);
                ServerConfig.ALLOW_CUSTOM.set(true);
                ServerConfig.APPLY_AIRPLANE.set(true);
                ServerConfig.APPLY_HELICOPTER.set(true);
                ServerConfig.LOG_APPLY.set(false);
                ServerConfig.SPEC.save();
            }
            case DIAGNOSTICS -> {
                ClientConfig.DEBUG_HUD.set(false);
                ClientConfig.TELEMETRY_ENABLED.set(false);
                ClientConfig.TELEMETRY_INTERVAL_TICKS.set(1);
                ClientConfig.SPEC.save();
            }
        }
    }

    private void resetKeybinds() {
        KeyMapping[] mappings = {
                ModKeys.YAW_LEFT, ModKeys.YAW_RIGHT, ModKeys.ROLL_LEFT, ModKeys.ROLL_RIGHT,
                ModKeys.PITCH_UP, ModKeys.PITCH_DOWN,
                ModKeys.TOGGLE_SCHEME, ModKeys.TOGGLE_CAMERA, ModKeys.OPEN_CONFIG
        };
        for (KeyMapping mapping : mappings) {
            mapping.setKey(mapping.getDefaultKey());
        }
        KeyMapping.resetMapping();
        if (minecraft != null) {
            minecraft.options.save();
        }
    }

    private static Button toggle(int x, int y, int w, String labelKey,
                                 BooleanSupplier getter, Consumer<Boolean> setter) {
        String label = Component.translatable(labelKey).getString();
        Component initial = Component.literal(label + ": ")
                .append(Component.translatable(getter.getAsBoolean() ? "options.on" : "options.off"));
        return Button.builder(initial, button -> {
            setter.accept(!getter.getAsBoolean());
            button.setMessage(Component.literal(label + ": ")
                    .append(Component.translatable(getter.getAsBoolean() ? "options.on" : "options.off")));
        }).bounds(x, y, w, 20).build();
    }

    private static ConfigSlider slider(int x, int y, int w, String labelKey,
                                       double min, double max, double current,
                                       Consumer<Double> setter, DoubleFunction<String> formatter) {
        return new ConfigSlider(x, y, w, 20, Component.translatable(labelKey).getString(),
                min, max, current, setter, formatter);
    }

    private static DoubleFunction<String> sec() {
        return value -> String.format("%.2fs", value);
    }

    private static DoubleFunction<String> num() {
        return value -> String.format("%.2f", value);
    }

    static Component schemeName(ControlScheme scheme) {
        return Component.translatable("screen.sbwcontrol.scheme." + scheme.name());
    }

    static Component axisName(AxisSource source) {
        return Component.translatable("screen.sbwcontrol.axis." + source.name());
    }

    static Component presetName(CameraPreset preset) {
        return Component.translatable("screen.sbwcontrol.preset." + preset.name());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        // 操作页底部两行用来放“打开原版按键设置/恢复默认按键”，不再画状态文字，避免遮按钮。
        if (tab == Tab.OPERATION) {
            return;
        }
        boolean supported = ClientSupport.isSupported();
        Component server = Component.literal(supported
                ? "server: supported (mask=0b" + Integer.toBinaryString(ClientSupport.allowedMask()) + ")"
                : "server: NOT supported");
        int color = supported ? 0x55FF55 : 0xFF5555;
        graphics.drawCenteredString(font, server, width / 2, height - 56, color);
        if (tab == Tab.SERVER && (minecraft == null || !minecraft.hasSingleplayerServer())) {
            graphics.drawCenteredString(font, Component.translatable("screen.sbwcontrol.server_only"),
                    width / 2, height - 44, 0xFFAA55);
        } else if (tab == Tab.CONTROL) {
            graphics.drawCenteredString(font, Component.translatable("screen.sbwcontrol.control_hint"),
                    width / 2, height - 44, 0xA0A0A0);
        } else if (tab == Tab.DIAGNOSTICS) {
            graphics.drawCenteredString(font, Component.translatable("screen.sbwcontrol.telemetry_hint"),
                    width / 2, height - 44, 0xA0A0A0);
        } else {
            graphics.drawCenteredString(font, Component.translatable("screen.sbwcontrol.hint"),
                    width / 2, height - 44, 0xA0A0A0);
        }
    }
}
