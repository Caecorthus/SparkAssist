package dev.caecorthus.sparkassist.guidebook.content;

import java.util.Objects;

/**
 * One styled text run inside a GuideBook block.
 * 指南书内容块中的一段带样式文字。
 */
public record GuidebookRun(
        String text,
        String translationKey,
        boolean bold,
        boolean italic,
        GuidebookTone tone
) {
    public GuidebookRun {
        if ((text == null) == (translationKey == null)) {
            throw new IllegalArgumentException("GuideBook runs must contain exactly one of text or translationKey");
        }
        tone = tone == null ? GuidebookTone.DEFAULT : tone;
    }

    public GuidebookRun(String text, boolean bold, boolean italic, GuidebookTone tone) {
        this(text, null, bold, italic, tone);
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
