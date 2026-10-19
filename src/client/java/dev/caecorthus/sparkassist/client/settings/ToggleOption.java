package dev.caecorthus.sparkassist.client.settings;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

/** An on/off row. 开关行。 */
public final class ToggleOption extends SettingsOption {
    private final BooleanSupplier getter;
    private final BooleanConsumer setter;
    private final boolean defaultValue;

    public ToggleOption(String id, Text label, Text description, Text source, SettingsStore store,
                        BooleanSupplier getter, BooleanConsumer setter, boolean defaultValue) {
        super(id, label, description, source, store);
        this.getter = Objects.requireNonNull(getter);
        this.setter = Objects.requireNonNull(setter);
        this.defaultValue = defaultValue;
    }

    public boolean value() {
        return getter.getAsBoolean();
    }

    public void set(boolean value) {
        setter.accept(value);
    }

    public void toggle() {
        set(!value());
    }

    @Override
    public Object capture() {
        return value();
    }

    @Override
    public void restore(Object captured) {
        if (captured instanceof Boolean value) {
            set(value);
        }
    }

    @Override
    public void resetToDefault() {
        set(defaultValue);
    }

    @Override
    public boolean isDefault() {
        return value() == defaultValue;
    }

    @Override
    public Text valueText() {
        return ScreenTexts.onOrOff(value());
    }

    @Override
    public Text defaultValueText() {
        return ScreenTexts.onOrOff(defaultValue);
    }
}
