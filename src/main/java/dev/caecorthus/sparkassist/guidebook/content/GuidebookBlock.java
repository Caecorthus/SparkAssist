package dev.caecorthus.sparkassist.guidebook.content;

import java.util.List;
import java.util.Objects;

/**
 * A semantic content block rendered with consistent book typography.
 * 使用统一书本排版规则渲染的语义内容块。
 */
public record GuidebookBlock(
        GuidebookBlockType type,
        List<GuidebookRun> runs
) {
    public GuidebookBlock {
        Objects.requireNonNull(type, "type");
        runs = List.copyOf(Objects.requireNonNull(runs, "runs"));
        if (type != GuidebookBlockType.SPACER && runs.isEmpty()) {
            throw new IllegalArgumentException("Non-spacer GuideBook blocks must contain text");
        }
    }
}
