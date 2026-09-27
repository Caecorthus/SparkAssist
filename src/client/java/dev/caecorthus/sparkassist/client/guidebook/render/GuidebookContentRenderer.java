package dev.caecorthus.sparkassist.client.guidebook.render;

import static dev.caecorthus.sparkassist.client.guidebook.ui.ExpressPalette.BRASS_LO;
import static dev.caecorthus.sparkassist.client.guidebook.ui.ExpressPalette.CALLOUT;
import static dev.caecorthus.sparkassist.client.guidebook.ui.ExpressPalette.INK;
import static dev.caecorthus.sparkassist.client.guidebook.ui.ExpressPalette.INK_FAINT;
import static dev.caecorthus.sparkassist.client.guidebook.ui.ExpressPalette.INK_MUTED;
import static dev.caecorthus.sparkassist.client.guidebook.ui.ExpressPalette.INK_RULE;
import static dev.caecorthus.sparkassist.client.guidebook.ui.ExpressPalette.INK_RULE_SOFT;
import static dev.caecorthus.sparkassist.client.guidebook.ui.ExpressPalette.P_DANGER;
import static dev.caecorthus.sparkassist.client.guidebook.ui.ExpressPalette.P_GOOD;
import static dev.caecorthus.sparkassist.client.guidebook.ui.ExpressPalette.P_INFO;
import static dev.caecorthus.sparkassist.client.guidebook.ui.ExpressPalette.P_ITEM;
import static dev.caecorthus.sparkassist.client.guidebook.ui.ExpressPalette.P_MONEY;
import static dev.caecorthus.sparkassist.client.guidebook.ui.ExpressPalette.P_MUTED;

import dev.caecorthus.sparkassist.client.guidebook.render.CjkLineBreaker.Cell;
import dev.caecorthus.sparkassist.guidebook.content.GuidebookBlock;
import dev.caecorthus.sparkassist.guidebook.content.GuidebookBlockType;
import dev.caecorthus.sparkassist.guidebook.content.GuidebookPage;
import dev.caecorthus.sparkassist.guidebook.content.GuidebookRun;
import dev.caecorthus.sparkassist.guidebook.content.GuidebookTone;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

/**
 * Typesets structured GuideBook blocks on the reader's paper (spec-final §5.5): 12 px lines with glyphs at +2,
 * CJK-aware wrapping, tone colours, render-time emphasis and ornaments. JSON {@code bold}/{@code italic} are ignored
 * (no bold or italic CJK). All coordinates are relative to the body origin (column x 0, body y 0); the page header
 * above the body belongs to {@code ReaderPainter}.
 * 在正文纸面上排版结构化指南内容（spec-final §5.5）：行高 12、文字下移 2、中日韩感知换行、色调、渲染期强调与装饰。
 * 忽略 JSON 的粗体与斜体（不绘制粗体或斜体汉字）。所有坐标相对正文原点（栏内 x 0、正文 y 0），正文上方的页眉由 ReaderPainter 负责。
 */
public final class GuidebookContentRenderer {
    public static final int LINE_HEIGHT = 12;
    /** Glyph top inside a 12 px line. 文字在 12 像素行内的下移量。 */
    public static final int TEXT_OFFSET = 2;
    /** SECTION_MARK sits 4 px below its row top. 分节菱形位于行顶下方 4 像素。 */
    public static final int SECTION_MARK_OFFSET = 4;
    private static final int SECTION_GAP = 10;
    private static final int SECTION_AFTER = 3;
    private static final int SECTION_LABEL_X = 9;
    private static final int SECTION_RULE_GAP = 5;
    private static final int SECTION_RULE_MIN = 16;
    private static final int QUOTE_GAP = 4;
    private static final int QUOTE_INSET = 36;
    private static final int QUOTE_AFTER = 3;
    private static final int BULLET_GAP = 4;
    private static final int BULLET_INDENT = 10;
    private static final int PARAGRAPH_GAP = 6;
    private static final int CALLOUT_TEXT_X = 8;
    private static final int CALLOUT_WRAP_INSET = 12;
    private static final int DIVIDER_GAP = 10;
    private static final int ORNAMENT_HEIGHT = 5;
    private static final int TAIL_GAP = 14;
    private static final int TAIL_AFTER = 6;
    private static final Pattern EMPHASIS = Pattern.compile("【[^】]*】|\\d+ ?金币");
    /** Preferred split points for a balanced two-line epigraph. 两行引言优先的断点。 */
    private static final String BALANCE_BREAKS = "，。；：！？、…」";

    private GuidebookContentRenderer() {
    }

    public static Layout layout(
            GuidebookPage page,
            TextRenderer textRenderer,
            int width,
            Function<String, String> translationResolver
    ) {
        // Widths are cached per (code point, style) for this layout only; a resource reload rebuilds the article.
        // 字宽按（码点，样式）缓存，仅在本次排版内有效；资源重载会重新排版。
        Map<Cell, Integer> widths = new HashMap<>();
        return typeset(page, cell -> widths.computeIfAbsent(cell,
                key -> textRenderer.getWidth(OrderedText.styled(key.codePoint(), key.style()))), width, translationResolver);
    }

    /** Font-free core (tests pass a fake width): {@code advance} measures one cell. 无字体的核心排版，测试可传入假字宽。 */
    static Layout typeset(
            GuidebookPage page,
            ToIntFunction<Cell> advance,
            int width,
            Function<String, String> translationResolver
    ) {
        Typesetter typesetter = new Typesetter(advance, width, translationResolver);
        for (GuidebookBlock block : page.blocks()) {
            typesetter.block(block);
        }
        return typesetter.finish();
    }

    static String resolveText(GuidebookRun run, Function<String, String> translationResolver) {
        return run.translationKey() == null ? run.text() : translationResolver.apply(run.translationKey());
    }

    /**
     * Credits body split on blank lines into PARAGRAPH blocks; a paragraph without CJK (U+3000–9FFF, i.e. the
     * required English Wathe notice) is toned MUTED so it typesets as a callout. The legal text is unchanged.
     * 鸣谢正文按空行拆成段落；不含汉字的段落（即必需的 Wathe 英文声明）设为 MUTED，排版为注释框。法律文本保持不变。
     */
    public static GuidebookPage creditsPage(String body) {
        List<GuidebookBlock> blocks = new ArrayList<>();
        for (String paragraph : body.split("\n\n")) {
            if (paragraph.isBlank()) {
                continue;
            }
            boolean cjk = paragraph.codePoints().anyMatch(codePoint -> codePoint >= 0x3000 && codePoint <= 0x9FFF);
            blocks.add(new GuidebookBlock(GuidebookBlockType.PARAGRAPH, List.of(new GuidebookRun(paragraph, false,
                    false, cjk ? GuidebookTone.DEFAULT : GuidebookTone.MUTED))));
        }
        if (blocks.isEmpty()) {
            blocks.add(new GuidebookBlock(GuidebookBlockType.PARAGRAPH,
                    List.of(new GuidebookRun(body, false, false, GuidebookTone.DEFAULT))));
        }
        return new GuidebookPage(blocks);
    }

    private static int toneColor(GuidebookTone tone, int base) {
        return switch (tone) {
            case DEFAULT -> base;
            case MUTED -> P_MUTED;
            case ITEM -> P_ITEM;
            case MONEY -> P_MONEY;
            case GOOD -> P_GOOD;
            case DANGER -> P_DANGER;
            case INFO -> P_INFO;
        };
    }

    private static Style ink(int argb) {
        return Style.EMPTY.withColor(argb & 0xFFFFFF);
    }

    /** Stateful single-pass typesetter; "first" is true at the body start and after an epigraph or a divider.
     * Paragraphs, bullets and callouts wrap with widow control (避免末行只剩一个字 / no lone last glyph); epigraphs
     * use the balanced split instead and section labels are too short to need it.
     * 单遍排版状态机；正文开头、引言或分隔线之后 first 为真。段落、列表项与注释框换行时避免末行只剩一个字；引言改用平衡断行，
     * 分节标题很短，无需处理。 */
    private static final class Typesetter {
        private final ToIntFunction<Cell> advance;
        private final int width;
        private final Function<String, String> resolver;
        private final List<RenderedLine> lines = new ArrayList<>();
        private final List<Ornament> ornaments = new ArrayList<>();
        private int y;
        private boolean first = true;

        Typesetter(ToIntFunction<Cell> advance, int width, Function<String, String> resolver) {
            this.advance = advance;
            this.width = Math.max(1, width);
            this.resolver = resolver;
        }

        void block(GuidebookBlock block) {
            switch (block.type()) {
                case SPACER -> {
                    y += DIVIDER_GAP;
                    ornaments.add(new Ornament(Ornament.Kind.DIVIDER, 0, y, width, ORNAMENT_HEIGHT, INK_RULE, null));
                    y += ORNAMENT_HEIGHT + DIVIDER_GAP;
                    first = true;
                }
                case SECTION -> {
                    section(block);
                    first = false;
                }
                case QUOTE -> {
                    quote(block);
                    first = true;
                }
                case BULLET -> {
                    bullet(block);
                    first = false;
                }
                case PARAGRAPH -> {
                    paragraph(block);
                    first = false;
                }
            }
        }

        private void section(GuidebookBlock block) {
            if (!first) {
                y += SECTION_GAP;
            }
            int tone = toneColor(block.runs().get(0).tone(), INK);
            List<Cell> cells = CjkLineBreaker.normalise(plainCells(block, INK), true);
            List<List<Cell>> wrapped = CjkLineBreaker.wrap(cells, width - SECTION_LABEL_X, advance);
            ornaments.add(new Ornament(Ornament.Kind.SECTION_MARK, 0, y + SECTION_MARK_OFFSET, 5, 5, tone,
                    Text.literal(CjkLineBreaker.string(cells))));
            for (int i = 0; i < wrapped.size(); i++) {
                lines.add(new RenderedLine(CjkLineBreaker.toOrderedText(wrapped.get(i)), SECTION_LABEL_X,
                        y + TEXT_OFFSET + i * LINE_HEIGHT));
            }
            if (wrapped.size() == 1) {
                int ruleX = SECTION_LABEL_X + CjkLineBreaker.width(wrapped.get(0), advance) + SECTION_RULE_GAP;
                if (width - ruleX >= SECTION_RULE_MIN) {
                    ornaments.add(new Ornament(Ornament.Kind.SECTION_RULE, ruleX, y + 6, width - ruleX, 1, INK_RULE,
                            null));
                }
            }
            y += wrapped.size() * LINE_HEIGHT + SECTION_AFTER;
        }

        private void quote(GuidebookBlock block) {
            if (!first) {
                y += QUOTE_GAP;
            }
            Style bracket = ink(INK_MUTED);
            List<Cell> cells = new ArrayList<>(CjkLineBreaker.cells("「", bracket));
            cells.addAll(plainCells(block, INK_MUTED));
            cells.addAll(CjkLineBreaker.cells("」", bracket));
            cells = CjkLineBreaker.normalise(cells, true);
            for (List<Cell> line : balanced(cells, width - QUOTE_INSET)) {
                // Hanging brackets: centre the text between the brackets, not the whole line.
                // 悬挂括号：居中的是括号内的文字而不是整行。
                int lead = !line.isEmpty() && line.get(0).codePoint() == '「' ? advance.applyAsInt(line.get(0)) : 0;
                int trail = !line.isEmpty() && line.get(line.size() - 1).codePoint() == '」'
                        ? advance.applyAsInt(line.get(line.size() - 1)) : 0;
                int x = (width - (CjkLineBreaker.width(line, advance) - lead - trail)) / 2 - lead;
                lines.add(new RenderedLine(CjkLineBreaker.toOrderedText(line), x, y + TEXT_OFFSET));
                y += LINE_HEIGHT;
            }
            y += QUOTE_AFTER;
            ornaments.add(new Ornament(Ornament.Kind.QUOTE_RULE, 0, y, width, 1, INK_RULE_SOFT, null));
            y += 1 + 10;
        }

        private void bullet(GuidebookBlock block) {
            if (!first) {
                y += BULLET_GAP;
            }
            ornaments.add(new Ornament(Ornament.Kind.BULLET, 3, y + 5, 3, 3, BRASS_LO, null));
            List<Cell> cells = CjkLineBreaker.normalise(emphasised(block), true);
            for (List<Cell> line : CjkLineBreaker.wrap(cells, width - BULLET_INDENT, advance, true)) {
                lines.add(new RenderedLine(CjkLineBreaker.toOrderedText(line), BULLET_INDENT, y + TEXT_OFFSET));
                y += LINE_HEIGHT;
            }
        }

        private void paragraph(GuidebookBlock block) {
            if (!first) {
                y += PARAGRAPH_GAP;
            }
            if (block.runs().stream().allMatch(run -> run.tone() == GuidebookTone.MUTED)) {
                // Callout: MUTED paragraphs are notes; no render-time emphasis inside.
                // 注释框：MUTED 段落是备注，框内不做渲染期强调。
                List<Cell> cells = CjkLineBreaker.normalise(plainCells(block, INK_MUTED), true);
                List<List<Cell>> wrapped = CjkLineBreaker.wrap(cells, width - CALLOUT_WRAP_INSET, advance, true);
                int height = wrapped.size() * LINE_HEIGHT + 4;
                ornaments.add(new Ornament(Ornament.Kind.CALLOUT, 0, y, width, height, CALLOUT, null));
                int rowY = y + 2;
                for (List<Cell> line : wrapped) {
                    lines.add(new RenderedLine(CjkLineBreaker.toOrderedText(line), CALLOUT_TEXT_X, rowY + TEXT_OFFSET));
                    rowY += LINE_HEIGHT;
                }
                y += height + 2;
                return;
            }
            List<Cell> cells = CjkLineBreaker.normalise(emphasised(block), true);
            for (List<Cell> line : CjkLineBreaker.wrap(cells, width, advance, true)) {
                lines.add(new RenderedLine(CjkLineBreaker.toOrderedText(line), 0, y + TEXT_OFFSET));
                y += LINE_HEIGHT;
            }
        }

        Layout finish() {
            y += TAIL_GAP;
            ornaments.add(new Ornament(Ornament.Kind.TAILPIECE, 0, y, width, ORNAMENT_HEIGHT, INK_FAINT, null));
            y += ORNAMENT_HEIGHT + TAIL_AFTER;
            return new Layout(lines, ornaments, y);
        }

        /** Runs in their tone colours (DEFAULT -> base); bold and italic are ignored. 各段按色调着色，忽略粗斜体。 */
        private List<Cell> plainCells(GuidebookBlock block, int base) {
            List<Cell> cells = new ArrayList<>();
            for (GuidebookRun run : block.runs()) {
                cells.addAll(CjkLineBreaker.cells(resolveText(run, resolver), ink(toneColor(run.tone(), base))));
            }
            return cells;
        }

        /** Render-time emphasis on DEFAULT runs only: 【…】 -> ITEM, "N 金币" -> MONEY; authored tones win.
         * 仅对 DEFAULT 片段做渲染期强调：【…】为物品色、“N 金币”为金币色；已标注的色调优先。 */
        private List<Cell> emphasised(GuidebookBlock block) {
            List<Cell> cells = new ArrayList<>();
            Style base = ink(INK);
            for (GuidebookRun run : block.runs()) {
                String text = resolveText(run, resolver);
                if (run.tone() != GuidebookTone.DEFAULT) {
                    cells.addAll(CjkLineBreaker.cells(text, ink(toneColor(run.tone(), INK))));
                    continue;
                }
                Matcher matcher = EMPHASIS.matcher(text);
                int last = 0;
                while (matcher.find()) {
                    cells.addAll(CjkLineBreaker.cells(text.substring(last, matcher.start()), base));
                    cells.addAll(CjkLineBreaker.cells(matcher.group(),
                            ink(matcher.group().startsWith("【") ? P_ITEM : P_MONEY)));
                    last = matcher.end();
                }
                cells.addAll(CjkLineBreaker.cells(text.substring(last), base));
            }
            return cells;
        }

        /**
         * Balanced epigraph: when greedy wrapping gives two lines, split after CJK punctuation where both halves fit and
         * the longer half is at most 2/3 of the total (minimising it); otherwise use the smallest width that keeps two
         * lines. 平衡引言：贪心为两行时，优先在中文标点后断开（两半都放得下且较长一半不超过总宽 2/3，取最短者）；否则取仍为两行的最小宽度。
         */
        private List<List<Cell>> balanced(List<Cell> cells, int measure) {
            List<List<Cell>> greedy = CjkLineBreaker.wrap(cells, measure, advance);
            if (greedy.size() != 2) {
                return greedy;
            }
            int best = -1;
            int bestMax = Integer.MAX_VALUE;
            for (int i = 1; i < cells.size() - 1; i++) {
                if (BALANCE_BREAKS.indexOf(cells.get(i - 1).codePoint()) < 0
                        || BALANCE_BREAKS.indexOf(cells.get(i).codePoint()) >= 0) {
                    continue;
                }
                int head = CjkLineBreaker.width(cells.subList(0, i), advance);
                int tail = CjkLineBreaker.width(cells.subList(i, cells.size()), advance);
                if (head > measure || tail > measure) {
                    continue;
                }
                int longer = Math.max(head, tail);
                if (longer < bestMax) {
                    bestMax = longer;
                    best = i;
                }
            }
            int total = CjkLineBreaker.width(cells, advance);
            if (best > 0 && bestMax <= total * 2 / 3) {
                return List.of(new ArrayList<>(cells.subList(0, best)), new ArrayList<>(cells.subList(best, cells.size())));
            }
            for (int tryWidth = total / 2; tryWidth <= measure; tryWidth++) {
                List<List<Cell>> wrapped = CjkLineBreaker.wrap(cells, tryWidth, advance);
                if (wrapped.size() == 2) {
                    return wrapped;
                }
            }
            return greedy;
        }
    }

    /** One laid-out text line: glyph top at (indent, y) in body coordinates. 一行排版结果：文字左上角位于正文坐标 (indent, y)。 */
    public record RenderedLine(OrderedText text, int indent, int y) {
    }

    /**
     * A drawn decoration in body coordinates; {@code color} is ARGB. Per kind:
     * SECTION_MARK = the 5x5 diamond box (label = section title, for the running head);
     * SECTION_RULE = the 1 px rule rect; BULLET = the 3x3 cross box;
     * QUOTE_RULE / DIVIDER / TAILPIECE = full-column box, centred on x + width / 2 (y is the rule row or ornament top);
     * CALLOUT = the tinted box, 2 px BRASS bar at its left.
     * 正文坐标中的装饰；各类型的几何含义见上。
     */
    public record Ornament(Kind kind, int x, int y, int width, int height, int color, @Nullable Text label) {
        public enum Kind {
            SECTION_MARK,
            SECTION_RULE,
            BULLET,
            QUOTE_RULE,
            CALLOUT,
            DIVIDER,
            TAILPIECE
        }
    }

    public record Layout(List<RenderedLine> lines, List<Ornament> ornaments, int height) {
        public static final Layout EMPTY = new Layout(List.of(), List.of(), 0);

        public Layout {
            lines = List.copyOf(lines);
            ornaments = List.copyOf(ornaments);
        }

        /** Title of the last section whose row top is at or above body y {@code bodyY}, or null.
         * 行顶不低于正文 y 的最后一个分节标题；没有则为 null。 */
        public @Nullable Text sectionAt(int bodyY) {
            Text current = null;
            for (Ornament ornament : ornaments) {
                if (ornament.kind() == Ornament.Kind.SECTION_MARK && ornament.y() - SECTION_MARK_OFFSET <= bodyY) {
                    current = ornament.label();
                }
            }
            return current;
        }
    }
}
