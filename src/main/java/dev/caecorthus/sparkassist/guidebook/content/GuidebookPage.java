package dev.caecorthus.sparkassist.guidebook.content;

import java.util.List;
import java.util.Objects;

/**
 * One editorially paginated page of authored GuideBook content.
 * 一页由编辑明确分页的指南书正文。
 */
public record GuidebookPage(List<GuidebookBlock> blocks) {
    public GuidebookPage {
        blocks = List.copyOf(Objects.requireNonNull(blocks, "blocks"));
        if (blocks.isEmpty()) {
            throw new IllegalArgumentException("GuideBook pages must contain at least one block");
        }
    }
}
