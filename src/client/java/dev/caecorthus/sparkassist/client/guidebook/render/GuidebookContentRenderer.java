package dev.caecorthus.sparkassist.client.guidebook.render;

import dev.caecorthus.sparkassist.guidebook.content.GuidebookBlock;
import dev.caecorthus.sparkassist.guidebook.content.GuidebookBlockType;
import dev.caecorthus.sparkassist.guidebook.content.GuidebookPage;
import dev.caecorthus.sparkassist.guidebook.content.GuidebookRun;
import dev.caecorthus.sparkassist.guidebook.content.GuidebookTone;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

/**
 * Lays out structured GuideBook blocks without tying content to one GUI scale.
 * 在不绑定特定 GUI 缩放的前提下排版结构化指南书正文。
 */
public final class GuidebookContentRenderer {
    private static final int DEFAULT_COLOR = 0x3B2A1A;
    private static final int SECTION_COLOR = 0x71442D;
    private static final int MUTED_COLOR = 0x806B58;
    private static final int ITEM_COLOR = 0x8A5A16;
    private static final int MONEY_COLOR = 0x98710E;
    private static final int GOOD_COLOR = 0xA13E68;
    private static final int DANGER_COLOR = 0xA33932;
    private static final int INFO_COLOR = 0x3F668F;
    private static final int LINE_GAP = 1;
    private static final String BULLET_GLYPH = "•";
    private static final int BULLET_TEXT_GAP = 3;

    private GuidebookContentRenderer() {
    }

    public static Layout layout(GuidebookPage page, TextRenderer textRenderer, int width) {
        List<RenderedLine> renderedLines = new ArrayList<>();
        int y = 0;
        boolean firstBlock = true;
        for (GuidebookBlock block : page.blocks()) {
            if (block.type() == GuidebookBlockType.SPACER) {
                y += Math.max(4, textRenderer.fontHeight / 2);
                firstBlock = false;
                continue;
            }

            if (!firstBlock) {
                y += gapBefore(block.type());
            }
            int indent = indent(block.type());
            MutableText text = styledText(block);
            if (block.type() == GuidebookBlockType.BULLET) {
                int contentIndent = indent + textRenderer.getWidth(BULLET_GLYPH) + BULLET_TEXT_GAP;
                List<OrderedText> wrapped = textRenderer.wrapLines(text, Math.max(20, width - contentIndent));
                renderedLines.add(new RenderedLine(styledBullet().asOrderedText(), indent, y));
                if (wrapped.isEmpty()) {
                    y += textRenderer.fontHeight + LINE_GAP;
                } else {
                    for (OrderedText line : wrapped) {
                        renderedLines.add(new RenderedLine(line, contentIndent, y));
                        y += textRenderer.fontHeight + LINE_GAP;
                    }
                }
            } else {
                List<OrderedText> wrapped = textRenderer.wrapLines(text, Math.max(20, width - indent));
                for (OrderedText line : wrapped) {
                    renderedLines.add(new RenderedLine(line, indent, y));
                    y += textRenderer.fontHeight + LINE_GAP;
                }
            }
            y += gapAfter(block.type());
            firstBlock = false;
        }
        return new Layout(renderedLines, Math.max(0, y - LINE_GAP));
    }

    private static MutableText styledText(GuidebookBlock block) {
        MutableText text = Text.empty();
        for (GuidebookRun run : block.runs()) {
            text.append(styledRun(run, block.type()));
        }
        return text;
    }

    private static MutableText styledBullet() {
        return styledRun(new GuidebookRun(BULLET_GLYPH, true, false, GuidebookTone.MUTED), GuidebookBlockType.BULLET);
    }

    private static MutableText styledRun(GuidebookRun run, GuidebookBlockType blockType) {
        int color = color(run.tone(), blockType);
        boolean bold = run.bold() || blockType == GuidebookBlockType.SECTION;
        boolean italic = run.italic() || blockType == GuidebookBlockType.QUOTE;
        return Text.literal(run.text()).styled(style -> style
                .withColor(color)
                .withBold(bold)
                .withItalic(italic));
    }

    private static int color(GuidebookTone tone, GuidebookBlockType blockType) {
        if (tone == GuidebookTone.DEFAULT) {
            return switch (blockType) {
                case SECTION -> SECTION_COLOR;
                case QUOTE -> MUTED_COLOR;
                default -> DEFAULT_COLOR;
            };
        }
        return switch (tone) {
            case DEFAULT -> DEFAULT_COLOR;
            case MUTED -> MUTED_COLOR;
            case ITEM -> ITEM_COLOR;
            case MONEY -> MONEY_COLOR;
            case GOOD -> GOOD_COLOR;
            case DANGER -> DANGER_COLOR;
            case INFO -> INFO_COLOR;
        };
    }

    private static int indent(GuidebookBlockType type) {
        return switch (type) {
            case QUOTE, BULLET -> 4;
            default -> 0;
        };
    }

    private static int gapBefore(GuidebookBlockType type) {
        return type == GuidebookBlockType.SECTION ? 3 : 1;
    }

    private static int gapAfter(GuidebookBlockType type) {
        return switch (type) {
            case SECTION, QUOTE -> 2;
            case PARAGRAPH -> 1;
            case BULLET -> 1;
            case SPACER -> 0;
        };
    }

    public record RenderedLine(OrderedText text, int indent, int y) {
    }

    public record Layout(List<RenderedLine> lines, int height) {
        public Layout {
            lines = List.copyOf(lines);
        }
    }
}
