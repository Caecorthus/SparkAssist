package dev.caecorthus.sparkassist.guidebook.content;

import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.Nullable;

/**
 * A semantic content block rendered with consistent book typography. {@code target} is the demo id of a
 * {@link GuidebookBlockType#DEMO} block and null for every other type.
 * 使用统一书本排版规则渲染的语义内容块。target 是 DEMO 块的演示 id，其他类型为 null。
 */
public record GuidebookBlock(
        GuidebookBlockType type,
        List<GuidebookRun> runs,
        @Nullable String target
) {
    public GuidebookBlock {
        Objects.requireNonNull(type, "type");
        runs = List.copyOf(Objects.requireNonNull(runs, "runs"));
        if (type != GuidebookBlockType.SPACER && runs.isEmpty()) {
            throw new IllegalArgumentException("Non-spacer GuideBook blocks must contain text");
        }
        if ((type == GuidebookBlockType.DEMO) != (target != null)) {
            throw new IllegalArgumentException("Only GuideBook demo blocks carry a target, and they must");
        }
        if (target != null && !target.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
            throw new IllegalArgumentException("GuideBook demo target must be a namespaced id: " + target);
        }
    }

    public GuidebookBlock(GuidebookBlockType type, List<GuidebookRun> runs) {
        this(type, runs, null);
    }
}
