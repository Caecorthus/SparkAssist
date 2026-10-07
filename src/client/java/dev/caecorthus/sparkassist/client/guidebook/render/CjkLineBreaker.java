package dev.caecorthus.sparkassist.client.guidebook.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.ToIntFunction;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;

/**
 * CJK-aware greedy line breaker over (code point, style) cells, so tone runs and render-time emphasis survive
 * wrapping (spec-final §2.4). A rule-for-rule port of the mockup's {@code tools/final/mock/CjkLineBreaker.java};
 * widths come from a caller-supplied advance function, so the class stays pure and unit-testable.
 * 基于（码点，样式）单元的中日韩感知贪心断行器，保证色调片段与渲染期强调在换行后保留（spec-final §2.4）。
 * 逐条移植自样稿 {@code CjkLineBreaker.java}；字宽由调用方提供，因此本类保持纯函数、可单元测试。
 *
 * <ol>
 *   <li>Break between any two CJK glyphs and at ASCII spaces (dropped at a line end); {@code \n} forces a break.
 *       任意两个汉字之间与 ASCII 空格处可断行（行尾空格丢弃）；{@code \n} 强制换行。</li>
 *   <li>Never break inside a word run of ASCII letters/digits and {@code + - . % / × ÷ _ ' #} ("+40%", "Enter").
 *       ASCII 字母数字等组成的单词不拆开。</li>
 *   <li>A word run containing a digit is glued to the next ideograph, optionally after one space ("15 秒").
 *       含数字的单词与其后的一个汉字（可隔一个空格）粘连。</li>
 *   <li>Closing punctuation sticks to the token before it; opening punctuation to the token after it.
 *       闭合标点附着前一记号，开启标点附着后一记号。</li>
 *   <li>{@code 【…】} with at most 8 inner glyphs is one token. 内含不超过 8 个字的【…】整体不拆。</li>
 *   <li>A token wider than the line falls back to glyph breaking. 超过行宽的记号退回逐字断行。</li>
 *   <li>Optionally, no paragraph ends on a lone glyph (widow control). 可选：段落末行不只剩一个字。</li>
 * </ol>
 */
public final class CjkLineBreaker {
    // Every OPEN mark has its CLOSE partner (“” ‘’ [] included), so a closing quote or bracket never starts a line.
    // 每个开启标点都有对应的闭合标点（含 “” ‘’ []），因此闭合引号或括号不会出现在行首。
    static final String CLOSE = "，。、；：？！）」』》】〉…—,.;:?!)％”’]";
    static final String OPEN = "（「『《【〈([“‘";
    private static final String WORD_SYMBOLS = "+-.%/×÷_'#";
    private static final int BRACKET_SPAN = 8;

    private CjkLineBreaker() {
    }

    /** One code point with its full style. 一个码点及其完整样式。 */
    public record Cell(int codePoint, Style style) {
    }

    /** Flattens styled text into cells, keeping every run's merged style. 把带样式文本展开为单元，保留每段合并后的样式。 */
    public static List<Cell> cells(StringVisitable text) {
        List<Cell> out = new ArrayList<>();
        text.visit((style, string) -> {
            string.codePoints().forEach(codePoint -> out.add(new Cell(codePoint, style)));
            return Optional.empty();
        }, Style.EMPTY);
        return out;
    }

    public static List<Cell> cells(String text, Style style) {
        List<Cell> out = new ArrayList<>(text.length());
        text.codePoints().forEach(codePoint -> out.add(new Cell(codePoint, style)));
        return out;
    }

    /**
     * Display normalisation (render time only; JSON and lang stay untouched). 7a: drop the ASCII space between a digit
     * and a following ideograph ("100 金币" -> "100金币"); the space before a number stays. 7b (when
     * {@code punctuation}): also drop ASCII spaces next to full-width punctuation ("反杀。 词条" -> "反杀。词条").
     * 显示归一化（仅渲染期）。7a：删除数字与其后汉字之间的 ASCII 空格，数字前的空格保留；7b：另删除紧邻全角标点的 ASCII 空格。
     */
    public static List<Cell> normalise(List<Cell> cells, boolean punctuation) {
        List<Cell> out = new ArrayList<>(cells.size());
        for (int i = 0; i < cells.size(); i++) {
            if (cells.get(i).codePoint() == ' ' && i > 0 && i + 1 < cells.size()) {
                int previous = cells.get(i - 1).codePoint();
                int next = cells.get(i + 1).codePoint();
                if (digit(previous) && ideograph(next)) {
                    continue;
                }
                if (punctuation && (fullWidthPunctuation(previous) || fullWidthPunctuation(next))) {
                    continue;
                }
            }
            out.add(cells.get(i));
        }
        return out;
    }

    /** Greedy fill over tokens; always returns at least one (possibly empty) line. 按记号贪心填充，至少返回一行。 */
    public static List<List<Cell>> wrap(List<Cell> cells, int maxWidth, ToIntFunction<Cell> advance) {
        return wrap(cells, maxWidth, advance, false);
    }

    /**
     * Greedy fill over tokens, as {@link #wrap(List, int, ToIntFunction)}. With {@code avoidWidow}, a paragraph (the
     * text up to a forced break or the end) that wraps to two or more lines never ends on a lone glyph (closing
     * punctuation does not count): the previous line's last token moves down ("…共同" | "获胜。"), provided the last
     * line still fits and the previous line keeps at least two glyphs. Tokens move whole, so every rule above still
     * holds, and the line count never changes (block heights stay put).
     * 按记号贪心填充，同上。avoidWidow 为真时，换成两行及以上的一段文字（至强制换行或结尾）不会以只剩一个字的末行结束
     * （闭合标点不计）：把上一行的最后一个记号移到末行（“…共同” | “获胜。”），前提是末行仍放得下、上一行至少保留两个字。
     * 记号整体移动，上述规则依然成立，行数不变（块高度不变）。
     */
    public static List<List<Cell>> wrap(List<Cell> cells, int maxWidth, ToIntFunction<Cell> advance,
                                        boolean avoidWidow) {
        // Lines are kept as token lists until the end so the widow pass can move whole tokens.
        // 行在结束前保持为记号列表，便于末行处理整体移动记号。
        List<List<List<Cell>>> lines = new ArrayList<>();
        List<List<Cell>> current = new ArrayList<>();
        int currentWidth = 0;
        int paragraphStart = 0;
        for (List<Cell> token : tokens(cells)) {
            int first = token.get(0).codePoint();
            if (first == '\n') {
                lines.add(current);
                if (avoidWidow) {
                    avoidWidow(lines, paragraphStart, maxWidth, advance);
                }
                paragraphStart = lines.size();
                current = new ArrayList<>();
                currentWidth = 0;
                continue;
            }
            int tokenWidth = width(token, advance);
            if (first == ' ') {
                if (!current.isEmpty()) {
                    current.add(token);
                    currentWidth += tokenWidth;
                }
                continue;
            }
            if (currentWidth + tokenWidth > maxWidth && !current.isEmpty()) {
                lines.add(current);
                current = new ArrayList<>();
                currentWidth = 0;
            }
            if (tokenWidth > maxWidth) {
                // Rule 6: an over-long token breaks between glyphs. 规则 6：超长记号逐字断行。
                for (Cell cell : token) {
                    int glyph = advance.applyAsInt(cell);
                    if (currentWidth + glyph > maxWidth && !current.isEmpty()) {
                        lines.add(current);
                        current = new ArrayList<>();
                        currentWidth = 0;
                    }
                    current.add(List.of(cell));
                    currentWidth += glyph;
                }
                continue;
            }
            current.add(token);
            currentWidth += tokenWidth;
        }
        lines.add(current);
        if (avoidWidow) {
            avoidWidow(lines, paragraphStart, maxWidth, advance);
        }
        List<List<Cell>> out = new ArrayList<>(lines.size());
        for (List<List<Cell>> line : lines) {
            List<Cell> flat = new ArrayList<>();
            line.forEach(flat::addAll);
            out.add(trim(flat));
        }
        return out;
    }

    /**
     * Widow pass over the paragraph that ends at the last line: 避免末行只剩一个字 / avoid a lone last glyph.
     * @param from index of the paragraph's first line / 本段第一行的下标
     */
    private static void avoidWidow(List<List<List<Cell>>> lines, int from, int maxWidth, ToIntFunction<Cell> advance) {
        int last = lines.size() - 1;
        if (last - 1 < from) {
            return;
        }
        List<List<Cell>> tail = lines.get(last);
        if (glyphs(tail) != 1) {
            return;
        }
        List<List<Cell>> previous = lines.get(last - 1);
        int end = previous.size();
        while (end > 0 && previous.get(end - 1).get(0).codePoint() == ' ') {
            end--;
        }
        if (end == 0) {
            return;
        }
        // The last token plus the spaces after it, so "word x。" keeps its space. 连同其后的空格一起移动。
        List<List<Cell>> moved = new ArrayList<>(previous.subList(end - 1, previous.size()));
        List<List<Cell>> kept = new ArrayList<>(previous.subList(0, end - 1));
        int movedWidth = 0;
        for (List<Cell> token : moved) {
            movedWidth += width(token, advance);
        }
        for (List<Cell> token : tail) {
            movedWidth += width(token, advance);
        }
        if (glyphs(kept) < 2 || movedWidth > maxWidth) {
            return;
        }
        moved.addAll(tail);
        lines.set(last - 1, kept);
        lines.set(last, moved);
    }

    /** Glyphs that count for widow control: everything except spaces and closing punctuation. 计入末行判断的字数。 */
    private static int glyphs(List<List<Cell>> tokens) {
        int count = 0;
        for (List<Cell> token : tokens) {
            for (Cell cell : token) {
                if (cell.codePoint() != ' ' && CLOSE.indexOf(cell.codePoint()) < 0) {
                    count++;
                }
            }
        }
        return count;
    }

    /** One OrderedText per line: consecutive cells with an equal style become one forwards-visited run.
     * 每行一个 OrderedText：样式相同的相邻单元合并为一段。 */
    public static OrderedText toOrderedText(List<Cell> line) {
        if (line.isEmpty()) {
            return OrderedText.EMPTY;
        }
        List<OrderedText> runs = new ArrayList<>();
        StringBuilder run = new StringBuilder();
        Style style = line.get(0).style();
        for (Cell cell : line) {
            if (!cell.style().equals(style)) {
                runs.add(OrderedText.styledForwardsVisitedString(run.toString(), style));
                run.setLength(0);
                style = cell.style();
            }
            run.appendCodePoint(cell.codePoint());
        }
        runs.add(OrderedText.styledForwardsVisitedString(run.toString(), style));
        return OrderedText.concat(runs);
    }

    public static int width(List<Cell> cells, ToIntFunction<Cell> advance) {
        int width = 0;
        for (Cell cell : cells) {
            width += advance.applyAsInt(cell);
        }
        return width;
    }

    public static String string(List<Cell> cells) {
        StringBuilder out = new StringBuilder(cells.size());
        cells.forEach(cell -> out.appendCodePoint(cell.codePoint()));
        return out.toString();
    }

    static List<List<Cell>> tokens(List<Cell> cells) {
        List<List<Cell>> tokens = new ArrayList<>();
        int i = 0;
        int n = cells.size();
        while (i < n) {
            int codePoint = cells.get(i).codePoint();
            if (codePoint == ' ' || codePoint == '\n') {
                tokens.add(List.of(cells.get(i++)));
                continue;
            }
            List<Cell> token = new ArrayList<>();
            while (i < n && OPEN.indexOf(cells.get(i).codePoint()) >= 0) {
                token.add(cells.get(i++));
            }
            boolean bracket = !token.isEmpty() && token.get(token.size() - 1).codePoint() == '【';
            int close = -1;
            if (bracket) {
                for (int j = i; j < n && j <= i + BRACKET_SPAN; j++) {
                    if (cells.get(j).codePoint() == '】') {
                        close = j;
                        break;
                    }
                }
            }
            if (close >= 0) {
                while (i <= close) {
                    token.add(cells.get(i++));
                }
            } else if (i < n && word(cells.get(i).codePoint())) {
                boolean number = false;
                while (i < n && word(cells.get(i).codePoint())) {
                    number |= digit(cells.get(i).codePoint());
                    token.add(cells.get(i++));
                }
                if (number) {
                    int j = i;
                    if (j < n && cells.get(j).codePoint() == ' ') {
                        j++;
                    }
                    if (j < n && ideograph(cells.get(j).codePoint())) {
                        while (i <= j) {
                            token.add(cells.get(i++));
                        }
                    }
                }
            } else if (i < n && cells.get(i).codePoint() != ' ' && cells.get(i).codePoint() != '\n') {
                token.add(cells.get(i++));
            }
            while (i < n && CLOSE.indexOf(cells.get(i).codePoint()) >= 0) {
                token.add(cells.get(i++));
            }
            if (token.isEmpty()) {
                token.add(cells.get(i++));
            }
            tokens.add(token);
        }
        return tokens;
    }

    static boolean word(int codePoint) {
        return codePoint >= '0' && codePoint <= '9' || codePoint >= 'A' && codePoint <= 'Z'
                || codePoint >= 'a' && codePoint <= 'z' || WORD_SYMBOLS.indexOf(codePoint) >= 0;
    }

    static boolean digit(int codePoint) {
        return codePoint >= '0' && codePoint <= '9';
    }

    static boolean ideograph(int codePoint) {
        return codePoint >= 0x3400 && codePoint <= 0x9FFF || codePoint >= 0xF900 && codePoint <= 0xFAFF;
    }

    static boolean fullWidthPunctuation(int codePoint) {
        return codePoint >= 0x3000 && codePoint <= 0x303F || codePoint >= 0xFF01 && codePoint <= 0xFF0F
                || codePoint >= 0xFF1A && codePoint <= 0xFF20 || codePoint >= 0xFF3B && codePoint <= 0xFF40
                || codePoint >= 0xFF5B && codePoint <= 0xFF65;
    }

    private static List<Cell> trim(List<Cell> line) {
        int start = 0;
        int end = line.size();
        while (start < end && line.get(start).codePoint() == ' ') {
            start++;
        }
        while (end > start && line.get(end - 1).codePoint() == ' ') {
            end--;
        }
        return new ArrayList<>(line.subList(start, end));
    }
}
