package dev.caecorthus.sparkassist.guidebook.content;

import java.util.Objects;

/**
 * One styled text run inside a GuideBook block.
 * 指南书内容块中的一段带样式文字。
 */
public record GuidebookRun(
        String text,
        boolean bold,
        boolean italic,
        GuidebookTone tone
) {
    public GuidebookRun {
        Objects.requireNonNull(text, "text");
        tone = tone == null ? GuidebookTone.DEFAULT : tone;
    }
}
