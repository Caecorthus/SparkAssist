package dev.caecorthus.sparkassist.guidebook.content;

import java.util.Objects;

/**
 * One styled text run inside a GuideBook block. {@code color} is an optional authored 0xRRGGBB colour that wins over
 * {@code tone}; null means "use the tone".
 * 指南书内容块中的一段带样式文字。{@code color} 是可选的作者自定颜色（0xRRGGBB），优先于 {@code tone}；null 表示按色调着色。
 */
public record GuidebookRun(
        String text,
        String translationKey,
        boolean bold,
        boolean italic,
        GuidebookTone tone,
        Integer color
) {
    public GuidebookRun {
        if ((text == null) == (translationKey == null)) {
            throw new IllegalArgumentException("GuideBook runs must contain exactly one of text or translationKey");
        }
        tone = tone == null ? GuidebookTone.DEFAULT : tone;
        if (color != null && (color < 0 || color > 0xFFFFFF)) {
            throw new IllegalArgumentException("GuideBook run color must be 0xRRGGBB");
        }
    }

    public GuidebookRun(String text, String translationKey, boolean bold, boolean italic, GuidebookTone tone) {
        this(text, translationKey, bold, italic, tone, null);
    }

    public GuidebookRun(String text, boolean bold, boolean italic, GuidebookTone tone) {
        this(text, null, bold, italic, tone, null);
    }

    public static GuidebookRun translated(
            String translationKey,
            boolean bold,
            boolean italic,
            GuidebookTone tone
    ) {
        return new GuidebookRun(null, Objects.requireNonNull(translationKey, "translationKey"), bold, italic, tone);
    }
}
