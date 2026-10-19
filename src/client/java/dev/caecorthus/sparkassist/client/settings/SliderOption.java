package dev.caecorthus.sparkassist.client.settings;

import java.util.Objects;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import net.minecraft.text.Text;

/** An integer row on a slider, snapped to {@code step} between {@code min} and {@code max}. 按步长取整的整数滑块行。 */
public final class SliderOption extends SettingsOption {
    private final int min;
    private final int max;
    private final int step;
    private final IntSupplier getter;
    private final IntConsumer setter;
    private final int defaultValue;
    private final IntFunction<Text> format;

    public SliderOption(String id, Text label, Text description, Text source, SettingsStore store, int min, int max,
                        int step, IntSupplier getter, IntConsumer setter, int defaultValue, IntFunction<Text> format) {
        super(id, label, description, source, store);
        if (min >= max || step <= 0) {
            throw new IllegalArgumentException("Bad slider range / 滑块范围无效");
        }
        this.min = min;
        this.max = max;
        this.step = step;
        this.getter = Objects.requireNonNull(getter);
        this.setter = Objects.requireNonNull(setter);
        this.defaultValue = snap(defaultValue);
        this.format = Objects.requireNonNull(format);
    }

    public int min() {
        return min;
    }

    public int max() {
        return max;
    }

    public int step() {
        return step;
    }

    public int value() {
        return getter.getAsInt();
    }

    public void set(int value) {
        int snapped = snap(value);
        if (snapped != value()) {
            setter.accept(snapped);
        }
    }

    /** Moves by whole steps (keyboard arrows). 按整步移动（键盘方向键）。 */
    public void nudge(int steps) {
        set(value() + steps * step);
    }

    /** Where the value sits on the track, 0..1. 值在滑轨上的位置，0 到 1。 */
    public double fraction() {
        return (snap(value()) - min) / (double) (max - min);
    }

    /** Sets the value from a track position, 0..1. 由滑轨位置（0 到 1）设定值。 */
    public void setFraction(double fraction) {
        double clamped = Double.isNaN(fraction) ? 0.0D : Math.max(0.0D, Math.min(1.0D, fraction));
        set((int) Math.round(min + clamped * (max - min)));
    }

    /** Clamps into range and rounds to the nearest step counted from {@code min}. 限制到范围内并按步长取最近值。 */
    public int snap(int value) {
        int clamped = Math.max(min, Math.min(max, value));
        int steps = Math.round((clamped - min) / (float) step);
        return Math.min(max, min + steps * step);
    }

    @Override
    public Object capture() {
        return value();
    }

    @Override
    public void restore(Object captured) {
        if (captured instanceof Integer value) {
            set(value);
        }
    }

    @Override
    public void resetToDefault() {
        set(defaultValue);
    }

    @Override
    public boolean isDefault() {
        return snap(value()) == defaultValue;
    }

    @Override
    public Text valueText() {
        return format.apply(snap(value()));
    }

    @Override
    public Text defaultValueText() {
        return format.apply(defaultValue);
    }
}
