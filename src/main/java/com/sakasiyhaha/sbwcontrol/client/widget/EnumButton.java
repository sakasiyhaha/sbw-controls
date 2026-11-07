package com.sakasiyhaha.sbwcontrol.client.widget;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.Function;

/** 枚举循环按钮：点击后切到下一个枚举值。 */
public class EnumButton<T extends Enum<T>> extends Button {

    private final Component prefix;
    private final T[] values;
    private final Consumer<T> setter;
    private final Function<T, Component> display;
    private T current;

    public EnumButton(int x, int y, int width, int height, Component prefix,
                      T current, T[] values, Consumer<T> setter, Function<T, Component> display) {
        super(x, y, width, height, Component.empty(), button -> {
        }, button -> Component.empty());
        this.prefix = prefix;
        this.current = current;
        this.values = values;
        this.setter = setter;
        this.display = display;
        updateLabel();
    }

    private void updateLabel() {
        setMessage(Component.literal(prefix.getString() + ": ").append(display.apply(current)));
    }

    @Override
    public void onPress() {
        current = values[(current.ordinal() + 1) % values.length];
        setter.accept(current);
        updateLabel();
    }

    public T getCurrent() {
        return current;
    }

    /** 外部状态变化（B 键/命令/TOML 重载）时同步显示，不触发 setter。 */
    public void setCurrent(T value) {
        if (value == null || value == current) {
            return;
        }
        this.current = value;
        updateLabel();
    }
}
