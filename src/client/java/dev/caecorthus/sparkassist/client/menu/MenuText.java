package dev.caecorthus.sparkassist.client.menu;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

/**
 * Builds the lines the boot chrome draws. Nothing here pins a typeface: every string is plain {@link Text}, so it renders
 * with {@code minecraft:default} and a resource pack that replaces the default font also replaces these lines.
 * 构建启动装饰所绘制的文本行。此处不锁定任何字体：每个字符串都是普通 {@link Text}，因此使用 {@code minecraft:default}
 * 渲染，替换默认字体的资源包也会一并替换这些行。
 */
public final class MenuText {
    private MenuText() {
    }

    /** A translated line. 翻译文本行。 */
    public static MutableText of(String key, Object... args) {
        return Text.translatable(key, args);
    }

    /** A literal line. 字面文本行。 */
    public static MutableText literal(String value) {
        return Text.literal(value);
    }

    /**
     * A translated line that falls back to the given Chinese wording while the language manager is still empty. The
     * splash is drawn before languages load — vanilla's own splash draws no text for that reason — so a plain
     * {@link Text#translatable} there would render the raw key.
     * 带中文兜底的翻译文本：语言管理器尚未装载时使用给定的中文。启动画面在语言装载之前绘制——原版启动画面正因此不画
     * 任何文字——因此此处若用裸 {@link Text#translatable}，会渲染出原始键名。
     */
    public static MutableText localized(String key, String fallbackChinese) {
        MutableText text = Text.translatable(key);
        if (key.equals(text.getString())) {
            return Text.literal(fallbackChinese);
        }
        return text;
    }
}
