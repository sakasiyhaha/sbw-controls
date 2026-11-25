package com.sakasiyhaha.sbwcontrol.client;

import com.sakasiyhaha.sbwcontrol.camera.dynamic.CameraDistanceController;
import com.sakasiyhaha.sbwcontrol.camera.view.CameraAimMode;
import com.sakasiyhaha.sbwcontrol.camera.view.CameraDistanceMode;
import com.sakasiyhaha.sbwcontrol.camera.view.CameraViewMode;
import com.sakasiyhaha.sbwcontrol.camera.view.FixedWingCameraBackend;
import com.sakasiyhaha.sbwcontrol.camera.view.VehicleViewMode;
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
 * 重置后的统一设置窗口。
 *
 * <p>主菜单只保留：
 * 载具控制 / 相机 / 输入手感 / 服务端 / 诊断。
 *
 * <p>相机页只保留当前正交相机：
 * 基础居中开关、YawChannel、RollChannel。
 * 旧的回正、误差圆、Sticky、V2/V3 等 UI 全部移除。
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

    private enum ViewTab {
        MODE("mode"),
        DYNAMIC("dynamic"),
        DISTANCE("distance");

        final String key;

        ViewTab(String key) {
            this.key = key;
        }
    }

    private Tab tab = Tab.CONTROL;
    private ViewTab viewTab = ViewTab.MODE;
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

    private void switchViewTab(ViewTab target) {
        if (viewTab == target) {
            return;
        }
        viewTab = target;
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
        int tabW = 70;
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
            int subW = 80;
            int subGap = 4;
            int subTotal = ViewTab.values().length * subW + (ViewTab.values().length - 1) * subGap;
            int subX = cx - subTotal / 2;
            int subY = 49;
            for (ViewTab value : ViewTab.values()) {
                final ViewTab target = value;
                Button button = Button.builder(
                                Component.translatable("screen.sbwcontrol.viewtab." + value.key),
                                b -> switchViewTab(target))
                        .bounds(subX, subY, subW, 18).build();
                button.active = viewTab != target;
                addRenderableWidget(button);
                subX += subW + subGap;
            }
            contentTop = 74;
        }
        switch (tab) {
            case CONTROL -> buildControl(cx);
            case CAMERA -> buildCamera(cx, contentTop);
            case OPERATION -> buildInput(cx, contentTop);
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

        customPitchButton = new EnumButton<>(right, row, w, 20,
                Component.translatable("screen.sbwcontrol.custom_pitch"),
                ClientConfig.CUSTOM_PITCH_SOURCE.get(), AxisSource.values(),
                ClientConfig.CUSTOM_PITCH_SOURCE::set, SbwConfigScreen::axisName);
        addRenderableWidget(customPitchButton);

        customRollButton = new EnumButton<>(right, row + step, w, 20,
                Component.translatable("screen.sbwcontrol.custom_roll"),
                ClientConfig.CUSTOM_ROLL_SOURCE.get(), AxisSource.values(),
                ClientConfig.CUSTOM_ROLL_SOURCE::set, SbwConfigScreen::axisName);
        addRenderableWidget(customRollButton);

        customYawButton = new EnumButton<>(right, row + step * 2, w, 20,
                Component.translatable("screen.sbwcontrol.custom_yaw"),
                ClientConfig.CUSTOM_YAW_SOURCE.get(), AxisSource.values(),
                ClientConfig.CUSTOM_YAW_SOURCE::set, SbwConfigScreen::axisName);
        addRenderableWidget(customYawButton);

        addRenderableWidget(Button.builder(Component.translatable("screen.sbwcontrol.open_keys"), b -> {
            if (minecraft != null) {
                minecraft.setScreen(new KeyBindsScreen(this, minecraft.options));
            }
        }).bounds(right, row + step * 3, w, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.sbwcontrol.reset_keys"),
                b -> resetKeybinds()).bounds(right, row + step * 4, w, 20).build());

        addRenderableWidget(toggle(left, row + step * 2, w, "screen.sbwcontrol.invert_pitch",
                ClientConfig.INVERT_PITCH::get, ClientConfig.INVERT_PITCH::set));
        addRenderableWidget(toggle(left, row + step * 3, w, "screen.sbwcontrol.invert_roll",
                ClientConfig.INVERT_ROLL::get, ClientConfig.INVERT_ROLL::set));
        addRenderableWidget(toggle(left, row + step * 4, w, "screen.sbwcontrol.invert_yaw",
                ClientConfig.INVERT_YAW::get, ClientConfig.INVERT_YAW::set));
        addRenderableWidget(toggle(left, row + step * 5, w, "screen.sbwcontrol.invert_mouse_y",
                ClientConfig.INVERT_MOUSE_Y::get, ClientConfig.INVERT_MOUSE_Y::set));
    }

    private void buildCamera(int cx, int row) {
        switch (viewTab) {
            case MODE -> buildViewMode(cx, row);
            case DYNAMIC -> buildViewDynamic(cx, row);
            case DISTANCE -> buildViewDistance(cx, row);
        }
    }

    private void buildViewMode(int cx, int row) {
        int left = cx - 155;
        int right = cx + 5;
        int w = 150;
        int step = 20;

        addRenderableWidget(new EnumButton<>(left, row, w, 20,
                Component.translatable("screen.sbwcontrol.global_tps_view"),
                ClientConfig.CAMERA_GLOBAL_VIEW_MODE.get(), CameraViewMode.values(),
                ClientConfig.CAMERA_GLOBAL_VIEW_MODE::set, SbwConfigScreen::viewModeName));

        addRenderableWidget(new EnumButton<>(left, row + step, w, 20,
                Component.translatable("screen.sbwcontrol.airplane_tps_view"),
                ClientConfig.CAMERA_AIRPLANE_VIEW_MODE.get(), VehicleViewMode.values(),
                ClientConfig.CAMERA_AIRPLANE_VIEW_MODE::set, SbwConfigScreen::vehicleViewModeName));

        addRenderableWidget(new EnumButton<>(left, row + step * 2, w, 20,
                Component.translatable("screen.sbwcontrol.helicopter_tps_view"),
                ClientConfig.CAMERA_HELICOPTER_VIEW_MODE.get(), VehicleViewMode.values(),
                ClientConfig.CAMERA_HELICOPTER_VIEW_MODE::set, SbwConfigScreen::vehicleViewModeName));

        addRenderableWidget(new EnumButton<>(left, row + step * 3, w, 20,
                Component.translatable("screen.sbwcontrol.fixed_wing_backend"),
                ClientConfig.CAMERA_FIXED_WING_BACKEND.get(), FixedWingCameraBackend.values(),
                ClientConfig.CAMERA_FIXED_WING_BACKEND::set, SbwConfigScreen::fixedWingBackendName));

        addRenderableWidget(toggle(right, row, w, "screen.sbwcontrol.follow_hard_rotation",
                ClientConfig.CAMERA_FOLLOW_HARD_ROTATION::get,
                ClientConfig.CAMERA_FOLLOW_HARD_ROTATION::set));
        addRenderableWidget(slider(right, row + step, w, "screen.sbwcontrol.hard_follow_height",
                0.0D, 6.0D, ClientConfig.CAMERA_HARD_FOLLOW_HEIGHT.get(),
                ClientConfig.CAMERA_HARD_FOLLOW_HEIGHT::set, metric()));

        addRenderableWidget(new EnumButton<>(right, row + step * 2, w, 20,
                Component.translatable("screen.sbwcontrol.aim_mode"),
                ClientConfig.CAMERA_AIM_MODE.get(), CameraAimMode.values(),
                ClientConfig.CAMERA_AIM_MODE::set, SbwConfigScreen::aimModeName));

        addRenderableWidget(slider(right, row + step * 3, w, "screen.sbwcontrol.fixed_wing_distance",
                4.0D, 80.0D, ClientConfig.CAMERA_FIXED_WING_DISTANCE.get(),
                ClientConfig.CAMERA_FIXED_WING_DISTANCE::set, metric()));
        addRenderableWidget(slider(right, row + step * 4, w, "screen.sbwcontrol.fixed_wing_height",
                0.0D, 12.0D, ClientConfig.CAMERA_FIXED_WING_HEIGHT.get(),
                ClientConfig.CAMERA_FIXED_WING_HEIGHT::set, metric()));
    }

    private void buildViewDynamic(int cx, int row) {
        int left = cx - 155;
        int right = cx + 5;
        int w = 150;
        int step = 20;
        boolean enabled = hasDynamicView();

        addRenderableWidget(disableIf(slider(left, row, w, "screen.sbwcontrol.channel_yaw_gain",
                -8.0D, 8.0D, ClientConfig.CAMERA_CHANNEL_YAW_GAIN.get(),
                ClientConfig.CAMERA_CHANNEL_YAW_GAIN::set, deg()), !enabled));
        addRenderableWidget(disableIf(slider(left, row + step, w, "screen.sbwcontrol.channel_yaw_lag",
                0.0D, 0.5D, ClientConfig.CAMERA_CHANNEL_YAW_LAG.get(),
                ClientConfig.CAMERA_CHANNEL_YAW_LAG::set, sec()), !enabled));
        addRenderableWidget(disableIf(slider(left, row + step * 2, w, "screen.sbwcontrol.channel_pitch_gain",
                -8.0D, 8.0D, ClientConfig.CAMERA_CHANNEL_PITCH_GAIN.get(),
                ClientConfig.CAMERA_CHANNEL_PITCH_GAIN::set, deg()), !enabled));
        addRenderableWidget(disableIf(slider(left, row + step * 3, w, "screen.sbwcontrol.channel_pitch_lag",
                0.0D, 0.5D, ClientConfig.CAMERA_CHANNEL_PITCH_LAG.get(),
                ClientConfig.CAMERA_CHANNEL_PITCH_LAG::set, sec()), !enabled));
        addRenderableWidget(disableIf(slider(left, row + step * 4, w, "screen.sbwcontrol.spring_damping",
                0.5D, 1.5D, ClientConfig.CAMERA_SPRING_DAMPING.get(),
                ClientConfig.CAMERA_SPRING_DAMPING::set, num()), !enabled));

        addRenderableWidget(disableIf(slider(right, row, w, "screen.sbwcontrol.channel_roll_gain",
                -8.0D, 8.0D, ClientConfig.CAMERA_CHANNEL_ROLL_GAIN.get(),
                ClientConfig.CAMERA_CHANNEL_ROLL_GAIN::set, deg()), !enabled));
        addRenderableWidget(disableIf(slider(right, row + step, w, "screen.sbwcontrol.channel_roll_lag",
                0.0D, 0.5D, ClientConfig.CAMERA_CHANNEL_ROLL_LAG.get(),
                ClientConfig.CAMERA_CHANNEL_ROLL_LAG::set, sec()), !enabled));
        addRenderableWidget(disableIf(toggle(right, row + step * 2, w, "screen.sbwcontrol.camera_invert_yaw",
                ClientConfig.CAMERA_INVERT_YAW::get, ClientConfig.CAMERA_INVERT_YAW::set), !enabled));
        addRenderableWidget(disableIf(toggle(right, row + step * 3, w, "screen.sbwcontrol.camera_invert_roll",
                ClientConfig.CAMERA_INVERT_ROLL::get, ClientConfig.CAMERA_INVERT_ROLL::set), !enabled));
        addRenderableWidget(disableIf(toggle(right, row + step * 4, w, "screen.sbwcontrol.camera_invert_pitch",
                ClientConfig.CAMERA_INVERT_PITCH::get, ClientConfig.CAMERA_INVERT_PITCH::set), !enabled));
    }

    private void buildViewDistance(int cx, int row) {
        int left = cx - 155;
        int right = cx + 5;
        int w = 150;
        int step = 20;
        boolean enabled = hasCenteredView();
        boolean extra = ClientConfig.CAMERA_DISTANCE_MODE.get() == CameraDistanceMode.EXTRA;

        addRenderableWidget(disableIf(new EnumButton<>(left, row, w, 20,
                Component.translatable("screen.sbwcontrol.distance_mode"),
                ClientConfig.CAMERA_DISTANCE_MODE.get(), CameraDistanceMode.values(),
                value -> {
                    ClientConfig.CAMERA_DISTANCE_MODE.set(value);
                    rebuildWidgets();
                }, SbwConfigScreen::distanceModeName), !enabled));

        addRenderableWidget(disableIf(slider(left, row + step, w,
                extra ? "screen.sbwcontrol.airplane_distance_extra" : "screen.sbwcontrol.airplane_distance_total",
                extra ? 0.0D : 4.0D, extra ? 30.0D : 80.0D,
                extra ? ClientConfig.CAMERA_AIRPLANE_DISTANCE_EXTRA.get()
                        : ClientConfig.CAMERA_AIRPLANE_DISTANCE_TOTAL.get(),
                extra ? ClientConfig.CAMERA_AIRPLANE_DISTANCE_EXTRA::set
                        : ClientConfig.CAMERA_AIRPLANE_DISTANCE_TOTAL::set,
                metric()), !enabled));

        addRenderableWidget(disableIf(slider(left, row + step * 2, w,
                extra ? "screen.sbwcontrol.helicopter_distance_extra" : "screen.sbwcontrol.helicopter_distance_total",
                extra ? 0.0D : 4.0D, extra ? 30.0D : 80.0D,
                extra ? ClientConfig.CAMERA_HELICOPTER_DISTANCE_EXTRA.get()
                        : ClientConfig.CAMERA_HELICOPTER_DISTANCE_TOTAL.get(),
                extra ? ClientConfig.CAMERA_HELICOPTER_DISTANCE_EXTRA::set
                        : ClientConfig.CAMERA_HELICOPTER_DISTANCE_TOTAL::set,
                metric()), !enabled));

        addRenderableWidget(disableIf(toggle(left, row + step * 3, w, "screen.sbwcontrol.distance_collision",
                ClientConfig.CAMERA_DISTANCE_COLLISION::get, ClientConfig.CAMERA_DISTANCE_COLLISION::set), !enabled));

        addRenderableWidget(disableIf(slider(right, row, w, "screen.sbwcontrol.distance_smooth",
                0.0D, 0.5D, ClientConfig.CAMERA_DISTANCE_SMOOTH.get(),
                ClientConfig.CAMERA_DISTANCE_SMOOTH::set, sec()), !enabled));
    }

    private boolean hasDynamicView() {
        CameraViewMode global = ClientConfig.CAMERA_GLOBAL_VIEW_MODE.get();
        return ClientConfig.CAMERA_AIRPLANE_VIEW_MODE.get().resolve(global) == CameraViewMode.CENTERED_DYNAMIC
                || ClientConfig.CAMERA_HELICOPTER_VIEW_MODE.get().resolve(global) == CameraViewMode.CENTERED_DYNAMIC;
    }

    private boolean hasCenteredView() {
        CameraViewMode global = ClientConfig.CAMERA_GLOBAL_VIEW_MODE.get();
        return ClientConfig.CAMERA_AIRPLANE_VIEW_MODE.get().resolve(global) != CameraViewMode.NATIVE
                || ClientConfig.CAMERA_HELICOPTER_VIEW_MODE.get().resolve(global) != CameraViewMode.NATIVE;
    }

    private static <T extends net.minecraft.client.gui.components.AbstractWidget> T disableIf(T widget, boolean disabled) {
        widget.active = !disabled;
        return widget;
    }

    private void buildInput(int cx, int row) {
        int left = cx - 155;
        int right = cx + 5;
        int w = 150;
        int step = 20;

        addRenderableWidget(new EnumButton<>(left, row, w, 20,
                Component.translatable("screen.sbwcontrol.input_mode"),
                ClientConfig.INPUT_MODE.get(), InputMode.values(),
                ClientConfig.INPUT_MODE::set, mode -> Component.translatable("screen.sbwcontrol.mode." + mode.name())));
        addRenderableWidget(slider(left, row + step, w, "screen.sbwcontrol.sensitivity",
                10, 300, ClientConfig.MOUSE_SENSITIVITY.get(),
                value -> ClientConfig.MOUSE_SENSITIVITY.set((int) Math.round(value)),
                value -> String.format("%.0f%%", value)));
        addRenderableWidget(slider(left, row + step * 2, w, "screen.sbwcontrol.smoothing",
                0.0, 1.0, ClientConfig.SMOOTHING.get(), ClientConfig.SMOOTHING::set, sec()));
        addRenderableWidget(slider(left, row + step * 3, w, "screen.sbwcontrol.deadzone",
                0.0, 0.5, ClientConfig.DEADZONE.get(), ClientConfig.DEADZONE::set, num()));
        addRenderableWidget(slider(left, row + step * 4, w, "screen.sbwcontrol.pitch_gain",
                0.1, 5.0, ClientConfig.PITCH_GAIN.get(), ClientConfig.PITCH_GAIN::set, num()));
        addRenderableWidget(slider(left, row + step * 5, w, "screen.sbwcontrol.roll_gain",
                0.1, 5.0, ClientConfig.ROLL_GAIN.get(), ClientConfig.ROLL_GAIN::set, num()));

        addRenderableWidget(slider(right, row, w, "screen.sbwcontrol.yaw_gain",
                0.1, 5.0, ClientConfig.YAW_GAIN.get(), ClientConfig.YAW_GAIN::set, num()));
        addRenderableWidget(slider(right, row + step, w, "screen.sbwcontrol.yaw_sensitivity",
                0.0, 1.0, ClientConfig.YAW_SENSITIVITY.get(),
                ClientConfig.YAW_SENSITIVITY::set, value -> String.format("%.0f%%", value * 100.0D)));
        addRenderableWidget(slider(right, row + step * 2, w, "screen.sbwcontrol.hybrid_mouse_yaw",
                0.0, 1.0, ClientConfig.HYBRID_MOUSE_YAW.get(),
                ClientConfig.HYBRID_MOUSE_YAW::set, num()));
        addRenderableWidget(slider(right, row + step * 3, w, "screen.sbwcontrol.stick_center",
                0.0, 5.0, ClientConfig.STICK_CENTER_TIME.get(),
                ClientConfig.STICK_CENTER_TIME::set, sec()));
        addRenderableWidget(slider(right, row + step * 4, w, "screen.sbwcontrol.stick_gain",
                0.1, 5.0, ClientConfig.STICK_GAIN.get(), ClientConfig.STICK_GAIN::set, num()));
        addRenderableWidget(slider(right, row + step * 5, w, "screen.sbwcontrol.roll_smoothing",
                0.0, 0.3, ClientConfig.ROLL_SMOOTHING.get(), ClientConfig.ROLL_SMOOTHING::set, sec()));
    }

    private void buildServer(int cx, int row) {
        int left = cx - 155;
        int right = cx + 5;
        int w = 150;
        int step = 20;
        boolean available = minecraft != null && minecraft.hasSingleplayerServer();
        if (!available) {
            Button hint = Button.builder(Component.translatable("screen.sbwcontrol.server_only"), b -> {
            }).bounds(left, row, w * 2 + 10, 20).build();
            hint.active = false;
            addRenderableWidget(hint);
            return;
        }

        addRenderableWidget(slider(left, row, w, "screen.sbwcontrol.max_yaw_rate",
                0.0, 180.0, ServerConfig.MAX_YAW_RATE.get(),
                ServerConfig.MAX_YAW_RATE::set, value -> String.format("%.0f°/s", value)));
        addRenderableWidget(slider(left, row + step, w, "screen.sbwcontrol.pitch_scale",
                0.1, 64.0, ServerConfig.PITCH_SCALE.get(), ServerConfig.PITCH_SCALE::set, num()));
        addRenderableWidget(slider(left, row + step * 2, w, "screen.sbwcontrol.roll_scale",
                0.1, 20.0, ServerConfig.ROLL_SCALE.get(), ServerConfig.ROLL_SCALE::set, num()));
        addRenderableWidget(slider(left, row + step * 3, w, "screen.sbwcontrol.input_timeout",
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
                ClientConfig.INVERT_PITCH.set(false);
                ClientConfig.INVERT_ROLL.set(false);
                ClientConfig.INVERT_YAW.set(false);
                ClientConfig.INVERT_MOUSE_Y.set(false);
                ClientConfig.SPEC.save();
            }
            case CAMERA -> {
                switch (viewTab) {
                    case MODE -> {
                        ClientConfig.CAMERA_GLOBAL_VIEW_MODE.set(CameraViewMode.CENTERED_DYNAMIC);
                        ClientConfig.CAMERA_AIRPLANE_VIEW_MODE.set(VehicleViewMode.FOLLOW_GLOBAL);
                        ClientConfig.CAMERA_HELICOPTER_VIEW_MODE.set(VehicleViewMode.FOLLOW_GLOBAL);
                        ClientConfig.CAMERA_FOLLOW_HARD_ROTATION.set(true);
                        ClientConfig.CAMERA_HARD_FOLLOW_HEIGHT.set(1.0D);
                        ClientConfig.CAMERA_AIM_MODE.set(CameraAimMode.CENTER);
                        ClientConfig.CAMERA_FIXED_WING_BACKEND.set(FixedWingCameraBackend.INDEPENDENT);
                        ClientConfig.CAMERA_FIXED_WING_DISTANCE.set(18.0D);
                        ClientConfig.CAMERA_FIXED_WING_HEIGHT.set(2.0D);
                    }
                    case DYNAMIC -> {
                        ClientConfig.CAMERA_CHANNEL_YAW_GAIN.set(2.5D);
                        ClientConfig.CAMERA_CHANNEL_YAW_LAG.set(0.12D);
                        ClientConfig.CAMERA_CHANNEL_ROLL_GAIN.set(1.5D);
                        ClientConfig.CAMERA_CHANNEL_ROLL_LAG.set(0.12D);
                        ClientConfig.CAMERA_CHANNEL_PITCH_GAIN.set(1.0D);
                        ClientConfig.CAMERA_CHANNEL_PITCH_LAG.set(0.12D);
                        ClientConfig.CAMERA_INVERT_YAW.set(false);
                        ClientConfig.CAMERA_INVERT_ROLL.set(false);
                        ClientConfig.CAMERA_INVERT_PITCH.set(false);
                        ClientConfig.CAMERA_SPRING_DAMPING.set(0.90D);
                    }
                    case DISTANCE -> {
                        ClientConfig.CAMERA_DISTANCE_MODE.set(CameraDistanceMode.EXTRA);
                        ClientConfig.CAMERA_AIRPLANE_DISTANCE_EXTRA.set(9.0D);
                        ClientConfig.CAMERA_HELICOPTER_DISTANCE_EXTRA.set(3.0D);
                        ClientConfig.CAMERA_AIRPLANE_DISTANCE_TOTAL.set(20.0D);
                        ClientConfig.CAMERA_HELICOPTER_DISTANCE_TOTAL.set(12.0D);
                        ClientConfig.CAMERA_DISTANCE_COLLISION.set(true);
                        ClientConfig.CAMERA_DISTANCE_SMOOTH.set(0.12D);
                    }
                }
                ClientConfig.SPEC.save();
            }
            case OPERATION -> {
                ClientConfig.INPUT_MODE.set(InputMode.STICK);
                ClientConfig.STICK_CENTER_TIME.set(0.60D);
                ClientConfig.STICK_GAIN.set(1.0D);
                ClientConfig.MOUSE_SENSITIVITY.set(100);
                ClientConfig.SMOOTHING.set(0.04D);
        ClientConfig.ROLL_SMOOTHING.set(0.02D);
                ClientConfig.DEADZONE.set(0.02D);
                ClientConfig.PITCH_GAIN.set(1.0D);
                ClientConfig.ROLL_GAIN.set(1.0D);
                ClientConfig.YAW_GAIN.set(1.0D);
                ClientConfig.YAW_SENSITIVITY.set(0.7D);
                ClientConfig.HYBRID_MOUSE_YAW.set(0.25D);
                ClientConfig.SPEC.save();
            }
            case SERVER -> {
                if (minecraft == null || !minecraft.hasSingleplayerServer()) {
                    return;
                }
                ServerConfig.MAX_YAW_RATE.set(90.0D);
                ServerConfig.PITCH_SCALE.set(20.0D);
                ServerConfig.ROLL_SCALE.set(6.0D);
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

    private static DoubleFunction<String> deg() {
        return value -> String.format("%.1f°", value);
    }

    private static DoubleFunction<String> metric() {
        return value -> String.format("%.1f格", value);
    }

    static Component viewModeName(CameraViewMode mode) {
        return Component.translatable("screen.sbwcontrol.viewmode." + mode.name());
    }

    static Component vehicleViewModeName(VehicleViewMode mode) {
        return Component.translatable("screen.sbwcontrol.vehicleviewmode." + mode.name());
    }

    static Component distanceModeName(CameraDistanceMode mode) {
        return Component.translatable("screen.sbwcontrol.distancemode." + mode.name());
    }

    static Component aimModeName(CameraAimMode mode) {
        return Component.translatable("screen.sbwcontrol.aimmode." + mode.name());
    }

    static Component fixedWingBackendName(FixedWingCameraBackend backend) {
        return Component.translatable("screen.sbwcontrol.fixedwingbackend." + backend.name());
    }

    static Component schemeName(ControlScheme scheme) {
        return Component.translatable("screen.sbwcontrol.scheme." + scheme.name());
    }

    static Component axisName(AxisSource source) {
        return Component.translatable("screen.sbwcontrol.axis." + source.name());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
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
        } else if (tab == Tab.CAMERA) {
            CameraViewMode global = ClientConfig.CAMERA_GLOBAL_VIEW_MODE.get();
            CameraViewMode airplane = ClientConfig.CAMERA_AIRPLANE_VIEW_MODE.get().resolve(global);
            CameraViewMode helicopter = ClientConfig.CAMERA_HELICOPTER_VIEW_MODE.get().resolve(global);
            Component hint = switch (viewTab) {
                case MODE -> Component.translatable("screen.sbwcontrol.view_status",
                        viewModeName(airplane), viewModeName(helicopter));
                case DYNAMIC -> Component.translatable("screen.sbwcontrol.dynamic_hint");
                case DISTANCE -> Component.translatable("screen.sbwcontrol.distance_status",
                        distanceModeName(ClientConfig.CAMERA_DISTANCE_MODE.get()),
                        String.format("%.1f", CameraDistanceController.getLastBaseDistance()),
                        String.format("%.1f", CameraDistanceController.getLastEffectiveDistance()));
            };
            graphics.drawCenteredString(font, hint, width / 2, height - 44, 0xA0A0A0);
        } else if (tab == Tab.OPERATION) {
            graphics.drawCenteredString(font, Component.translatable("screen.sbwcontrol.input_hint"),
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
