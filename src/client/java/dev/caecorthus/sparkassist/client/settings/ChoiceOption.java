package dev.caecorthus.sparkassist.client.settings;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

/** A row that cycles through a fixed list of values. 在固定取值列表中循环切换的行。 */
public final class ChoiceOption<T> extends SettingsOption {
    private final List<T> values;
    private final Supplier<T> getter;
    private final Consumer<T> setter;
    private final T defaultValue;
    private final Function<T, Text> names;
    private Function<T, Text> valueDescriptions = value -> null;

    public ChoiceOption(String id, Text label, Text description, Text source, SettingsStore store, List<T> values,
                        Supplier<T> getter, Consumer<T> setter, T defaultValue, Function<T, Text> names) {
        super(id, label, description, source, store);
        if (values.isEmpty() || !values.contains(defaultValue)) {
            throw new IllegalArgumentException("The default must be one of the values / 默认值必须在取值列表中");
        }
        this.values = List.copyOf(values);
        this.getter = Objects.requireNonNull(getter);
        this.setter = Objects.requireNonNull(setter);
        this.defaultValue = defaultValue;
        this.names = Objects.requireNonNull(names);
    }

    /** Adds per-value words, shown in the description panel for the selected value. 为每个取值附加说明。 */
    public ChoiceOption<T> describeValues(Function<T, Text> valueDescriptions) {
        this.valueDescriptions = Objects.requireNonNull(valueDescriptions);
        return this;
    }

    public List<T> values() {
        return values;
    }

    public T value() {
        return getter.get();
    }

    public void set(T value) {
        if (values.contains(value)) {
            setter.accept(value);
        }
    }

    /** Steps forward ({@code +1}) or back ({@code -1}), wrapping around. 前进或后退一个取值，首尾相接。 */
    public void cycle(int direction) {
        int index = values.indexOf(value());
        int size = values.size();
        int next = index < 0 ? 0 : Math.floorMod(index + Integer.signum(direction == 0 ? 1 : direction), size);
        set(values.get(next));
    }

    @Override
    public Object capture() {
        return value();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void restore(Object captured) {
        if (values.contains(captured)) {
            set((T) captured);
        }
    }

    @Override
    public void resetToDefault() {
        set(defaultValue);
    }

    @Override
    public boolean isDefault() {
        return Objects.equals(value(), defaultValue);
    }

    @Override
    public Text valueText() {
        return names.apply(value());
    }

    @Override
    public Text defaultValueText() {
        return names.apply(defaultValue);
    }

    @Override
    @Nullable
    public Text valueDescription() {
        return valueDescriptions.apply(value());
    }
}
