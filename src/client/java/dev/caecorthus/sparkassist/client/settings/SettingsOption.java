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
    @Nullable
    private Text detail;
    private int accent;
    @Nullable
    private SoundPreview preview;

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

    /** The row name; for a role row, the role (the sound is in {@link #detail()}). 行名；角色行时为角色名。 */
    public Text label() {
        return label;
    }

    /** What the row controls, drawn after the label in grey, or null. 标签后灰色显示的具体内容，可为 null。 */
    @Nullable
    public Text detail() {
        return detail;
    }

    /** The label and detail together, for the description panel and narration. 标签与具体内容合在一起，用于说明面板与旁白。 */
    public Text title() {
        return detail == null ? label : Text.translatable("screen.sparkassist.settings.title_with_detail", label, detail);
    }

    /** Opaque RGB of the dot drawn before the label (the role's guide colour), or 0 for none. 标签前圆点的颜色，0 为无。 */
    public int accent() {
        return accent;
    }

    /** Splits the row into "who · what" and marks it with a colour dot. 把行拆成「谁 · 什么」并加上颜色圆点。 */
    public SettingsOption withDetail(@Nullable Text detail, int accentRgb) {
        this.detail = detail;
        this.accent = accentRgb == 0 ? 0 : 0xFF000000 | accentRgb;
        return this;
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

    /** A sample the row can play, or null. 该行可试听的声音，可为 null。 */
    @Nullable
    public SoundPreview preview() {
        return preview;
    }

    public SettingsOption withPreview(@Nullable SoundPreview preview) {
        this.preview = preview;
        return this;
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
