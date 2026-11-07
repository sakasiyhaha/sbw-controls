package com.sakasiyhaha.sbwcontrol.client.widget;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.DoubleFunction;

/** 把一个 double 配置项放进原版滑块（AbstractSliderButton）里。 */
public class ConfigSlider extends AbstractSliderButton {

    private final String label;
    private final double min;
    private final double max;
    private final Consumer<Double> setter;
    private final DoubleFunction<String> formatter;

    public ConfigSlider(int x, int y, int width, int height, String label,
                        double min, double max, double current,
                        Consumer<Double> setter, DoubleFunction<String> formatter) {
        super(x, y, width, height, Component.empty(), toFraction(current, min, max));
        this.label = label;
        this.min = min;
        this.max = max;
        this.setter = setter;
        this.formatter = formatter;
        updateMessage();
    }

    private static double toFraction(double value, double min, double max) {
        if (max <= min) {
            return 0.0D;
        }
        return (value - min) / (max - min);
    }

    private double currentValue() {
        return min + (max - min) * value;
    }

    @Override
    protected void updateMessage() {
        setMessage(Component.literal(label + ": " + formatter.apply(currentValue())));
    }

    @Override
    protected void applyValue() {
        setter.accept(currentValue());
    }
}
