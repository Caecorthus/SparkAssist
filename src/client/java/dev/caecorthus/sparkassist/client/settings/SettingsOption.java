package dev.caecorthus.sparkassist.client.settings;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

/**
 * One row of the SparkDLC settings screen: a value the player can change, its default, the store that persists it and
 * the words that explain it. Values are applied live; the store writes them to disk when the session flushes.
 * SparkDLC 设置界面中的一行：玩家可调的值、默认值、负责保存它的存储，以及说明文字。改动立即生效，会话刷新时由存储写盘。
 */
public abstract class SettingsOption {
    private final String id;
    private final Text label;
    private final Text description;
    private final Text source;
    private final SettingsStore store;
    private BooleanSupplier enabled = () -> true;
    private Supplier<Text> disabledReason = () -> null;

    protected SettingsOption(String id, Text label, Text description, Text source, SettingsStore store) {
        this.id = Objects.requireNonNull(id);
        this.label = Objects.requireNonNull(label);
        this.description = Objects.requireNonNull(description);
        this.source = Objects.requireNonNull(source);
        this.store = Objects.requireNonNull(store);
    }

    public String id() {
        return id;
    }

    public Text label() {
        return label;
    }

    public Text description() {
        return description;
    }

    /** The mod that owns the value, shown under the description. 拥有该值的模组，显示在说明下方。 */
    public Text source() {
        return source;
    }

    public SettingsStore store() {
        return store;
    }

    /** Greys the row out while {@code enabled} is false; {@code reason} explains why. 条件不满足时置灰并说明原因。 */
    public SettingsOption enabledWhen(BooleanSupplier enabled, Supplier<Text> reason) {
        this.enabled = Objects.requireNonNull(enabled);
        this.disabledReason = Objects.requireNonNull(reason);
        return this;
    }

    public boolean enabled() {
        return enabled.getAsBoolean();
    }

    @Nullable
    public Text disabledReason() {
        return enabled() ? null : disabledReason.get();
    }

    /** The current value, as an opaque token for undo. 当前值的不透明快照，用于撤销。 */
    public abstract Object capture();

    /** Puts a value captured earlier back. 恢复先前捕获的值。 */
    public abstract void restore(Object captured);

    public abstract void resetToDefault();

    public abstract boolean isDefault();

    public abstract Text valueText();

    public abstract Text defaultValueText();

    /** Extra words about the value that is selected now, or null. 当前所选值的补充说明，可为 null。 */
    @Nullable
    public Text valueDescription() {
        return null;
    }
}
