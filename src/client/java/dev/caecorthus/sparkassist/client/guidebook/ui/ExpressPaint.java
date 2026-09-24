package dev.caecorthus.sparkassist.client.guidebook.ui;

import static dev.caecorthus.sparkassist.client.guidebook.ui.ExpressPalette.*;

import dev.caecorthus.sparkassist.SparkAssist;
import java.util.List;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * Harpy Express drawing primitives (spec-final §3.4), ported literally from the mockup renderer
 * {@code tools/final/mock/Ui.java}. The shared recipes (rounded fills, panel, header, gem, icons, tooltip) have the
 * same bodies as SparkWitch/SparkTraits {@code InventoryCardPaint}. Pure {@code DrawContext} fills, integer geometry,
 * no textures except the reused book icon; {@code fill(x1, y1, x2, y2)} has exclusive x2/y2.
 * 哈比特快绘制原语（spec-final §3.4），逐条移植自样稿渲染器 {@code Ui.java}；共享配方与 SparkWitch/SparkTraits 的
 * {@code InventoryCardPaint} 完全一致。只使用 {@code DrawContext} 填充与整数坐标，除复用的书本图标外不使用贴图。
 */
public final class ExpressPaint {
    /** Wathe-derived 16x16 book icon (see THIRD_PARTY_NOTICES.md). 源自 Wathe 的 16x16 书本图标。 */
    public static final Identifier BOOK_ICON = SparkAssist.id("textures/gui/guidebook/open_button.png");
    public static final int ICON_BUTTON_SIZE = 22;
    public static final int ICON_BOX = 14;
    public static final String ELLIPSIS = "…";

    private static final String[] GEM = {".BBB.", "BGIIB", "BIIIB", "BIISB", ".BBB."};
    private static final String[] STAR = {"...#...", "..###..", "#######", ".#####.", "..###..", ".##.##.", ".#...#."};
    private static final String[] DIAMOND = {"..#..", ".###.", "#####", ".###.", "..#.."};
    private static final String[] DIAMOND_OUTLINE = {"..#..", ".#.#.", "#...#", ".#.#.", "..#.."};
    private static final String[] MAGNIFIER = {".####....", "#....#...", "#....#...", "#....#...", "#....#...",
            ".######..", ".....###.", "......###", ".......##"};
    private static final String[] CLOSE = {"#.....#", "##...##", ".##.##.", "..###..", ".##.##.", "##...##", "#.....#"};
    private static final String[] FOLD_UP = {"..#..", ".#.#.", "#...#"};
    private static final String[] CLEAR = {"#...#", ".#.#.", "..#..", ".#.#.", "#...#"};
    private static final String[] CHEVRON_SMALL = {"#..", "##.", "###", "##.", "#.."};
    private static final String[] CHECK = {"....#", "...#.", "#.#..", ".#...", "....."};
    private static final String[] PLAY = {"#..", "##.", "###", "##.", "#.."};
    private static final String[] HOURGLASS = {"#####", ".#.#.", "..#..", ".###.", "#####"};
    private static final String[] PADLOCK = {".###.", ".#.#.", ".#.#.", "#####", "#####", "#####", "#####"};
    private static final String[] BULLET = {".#.", "###", ".#."};

    private ExpressPaint() {
    }

    // ================================================================ frame / 边框

    public static void roundedFill(DrawContext c, int x, int y, int w, int h, int col) {
        c.fill(x + 1, y, x + w - 1, y + 1, col);
        c.fill(x, y + 1, x + w, y + h - 1, col);
        c.fill(x + 1, y + h - 1, x + w - 1, y + h, col);
    }

    public static void roundedOutline(DrawContext c, int x, int y, int w, int h, int col) {
        c.fill(x + 1, y, x + w - 1, y + 1, col);
        c.fill(x + 1, y + h - 1, x + w - 1, y + h, col);
        c.fill(x, y + 1, x + 1, y + h - 1, col);
        c.fill(x + w - 1, y + 1, x + w, y + h - 1, col);
    }

    /** L-shaped shadow (right column + bottom row); the corner pixel stays empty so every corner cut survives.
     * L 形投影（右列与底行），角点留空以保留切角。 */
    public static void dropShadow(DrawContext c, int x, int y, int w, int h) {
        c.fill(x + w, y + 2, x + w + 1, y + h, SHADOW);
        c.fill(x + 2, y + h, x + w, y + h + 1, SHADOW);
    }

    /** Walnut bevel: lit top/left, shaded bottom/right, mitred TR/BL corners. 胡桃木斜面：左上亮、右下暗、斜接转角。 */
    public static void bevel(DrawContext c, int x, int y, int w, int h) {
        c.fill(x + 1, y + 1, x + w - 1, y + 2, RIM_HI);
        c.fill(x + 1, y + 2, x + 2, y + h - 1, RIM_HI);
        c.fill(x + 2, y + h - 2, x + w - 1, y + h - 1, RIM_LO);
        c.fill(x + w - 2, y + 2, x + w - 1, y + h - 2, RIM_LO);
        c.fill(x + w - 2, y + 1, x + w - 1, y + 2, RIM_MITER);
        c.fill(x + 1, y + h - 2, x + 2, y + h - 1, RIM_MITER);
    }

    /** 1 px ring at inset 2, top colour fading to the bottom colour on the sides. 内缩 2 像素的 1 像素环。 */
    public static void ring(DrawContext c, int x, int y, int w, int h, int top, int bottom) {
        c.fill(x + 2, y + 2, x + w - 2, y + 3, top);
        c.fill(x + 2, y + h - 3, x + w - 2, y + h - 2, bottom);
        c.fillGradient(x + 2, y + 3, x + 3, y + h - 3, top, bottom);
        c.fillGradient(x + w - 3, y + 3, x + w - 2, y + h - 3, top, bottom);
    }

    /** Mahogany + brass panel; content area is (x+3, y+3)..(x+w-3, y+h-3). 桃花心木黄铜面板；内容区为内缩 3 像素。 */
    public static void panel(DrawContext c, int x, int y, int w, int h) {
        dropShadow(c, x, y, w, h);
        roundedOutline(c, x, y, w, h, EDGE);
        c.fill(x + 1, y + 1, x + w - 1, y + h - 1, BODY);
        bevel(c, x, y, w, h);
        ring(c, x, y, w, h, BRASS_HI, BRASS_LO);
    }

    /** Title band (x+3, y+3, w-6, 16) with the engraved brass rule at y+19/y+20. 标题带及其下方的黄铜刻线。 */
    public static void titleBand(DrawContext c, int x, int y, int w) {
        c.fill(x + 3, y + 3, x + w - 3, y + 19, BAND);
        c.fill(x + 3, y + 19, x + w - 3, y + 20, BRASS_LO);
        c.fill(x + 3, y + 20, x + w - 3, y + 21, EDGE);
    }

    public static void etched(DrawContext c, int x1, int x2, int y) {
        c.fill(x1, y, x2, y + 1, EDGE);
        c.fill(x1, y + 1, x2, y + 2, ETCH_LIGHT);
    }

    /** 14x14 icon hit-box wash with cut corners. 14x14 图标热区底色（切角）。 */
    public static void iconWash(DrawContext c, int bx, int by, int col) {
        roundedFill(c, bx, by, ICON_BOX, ICON_BOX, col);
    }

    /**
     * Section header on dark (row 11): brass label without shadow, optional right tail in its own styles, and an
     * engraved rule drawn only after a non-empty label and only when it is at least 12 px long.
     * 暗底分节标题（行高 11）：无阴影黄铜标签、可选的右侧尾注（保留自身样式），刻线仅在标签非空且长度不少于 12 像素时绘制。
     *
     * @return the end x of the label / 标签结束的横坐标
     */
    public static int sectionHeader(DrawContext c, TextRenderer f, Text label, int x, int y, int x2,
                                    @Nullable Text tail, boolean tailShadow) {
        String plain = label.getString();
        int tailWidth = tail == null ? 0 : f.getWidth(tail);
        boolean showTail = tail != null && f.getWidth(plain) + 4 + tailWidth <= x2 - x;
        int end = x - 4;
        if (!plain.isEmpty()) {
            String shown = ellipsize(f, plain, x2 - x);
            c.drawText(f, shown, x, y + 1, HEADING, false);
            end = x + f.getWidth(shown);
        }
        int ruleEnd = x2;
        if (showTail) {
            c.drawText(f, tail, x2 - tailWidth, y + 1, GLYPH, tailShadow);
            ruleEnd = x2 - tailWidth - 4;
        }
        if (!plain.isEmpty() && ruleEnd - (end + 4) >= 12) {
            c.fill(end + 4, y + 5, ruleEnd, y + 6, BRASS_LO);
            c.fill(end + 4, y + 6, ruleEnd, y + 7, EDGE);
        }
        return end;
    }

    // ================================================================ rows / scroll / 行与滚动

    /** One row grammar for the directory and the card; the coin bar means "selected" and nothing else.
     * 目录与信息卡共用的行样式；金币色竖条只表示“已选中”。 */
    public static void rowState(DrawContext c, int x1, int y, int x2, int h,
                                boolean hovered, boolean selected, boolean focused) {
        if (selected) {
            c.fill(x1, y, x2, y + h, SELECT);
            c.fill(x1, y, x1 + 2, y + h, COIN);
        } else if (hovered) {
            c.fill(x1, y, x2, y + h, HOVER);
        }
        if (focused) {
            roundedOutline(c, x1, y, x2 - x1, h, FOCUS);
        }
    }

    /** Brass rod in a 3 px gutter at gx; idle is quiet, hot = hovered or dragged. 3 像素槽中的黄铜滑杆。 */
    public static void scrollbarDark(DrawContext c, int gx, int vy, int vh, int ty, int th, boolean hot) {
        c.fill(gx + 1, vy, gx + 2, vy + vh, EDGE);
        int body = hot ? BRASS : BRASS_LO;
        int light = hot ? BRASS_HI : BRASS;
        int shade = hot ? BRASS_LO : THUMB_SHADE;
        c.fill(gx, ty, gx + 3, ty + th, body);
        c.fill(gx, ty, gx + 1, ty + th, light);
        c.fill(gx, ty, gx + 3, ty + 1, light);
        c.fill(gx + 2, ty + 1, gx + 3, ty + th, shade);
        c.fill(gx + 1, ty + th - 1, gx + 3, ty + th, shade);
    }

    public static void scrollbarPaper(DrawContext c, int gx, int vy, int vh, int ty, int th, boolean hot) {
        c.fill(gx + 1, vy, gx + 2, vy + vh, INK_RULE_SOFT);
        c.fill(gx, ty, gx + 3, ty + th, hot ? INK_MUTED : PAPER_THUMB);
        c.fill(gx, ty, gx + 3, ty + 1, hot ? PAPER_THUMB : PAPER_THUMB_HI);
    }

    /** Thumb height for a track: max(12, track * viewport / total). 滑块高度。 */
    public static int thumbSize(int track, int viewport, int total) {
        return Math.max(12, track * viewport / Math.max(1, total));
    }

    /** Thumb top for a scroll offset within [0, total - viewport]. 给定滚动偏移时滑块的顶边。 */
    public static int thumbOffset(int trackTop, int track, int thumb, int scroll, int viewport, int total) {
        return trackTop + scroll * (track - thumb) / Math.max(1, total - viewport);
    }

    /**
     * 8 px fade to the same RGB at alpha 0, depth-tested at local z+1: it covers glyphs (+0.03) yet stays under
     * z=400 tooltips. {@code rgb} is 0xRRGGBB.
     * 8 像素渐隐到同色透明，位于局部 z+1 并参与深度测试：盖住文字（+0.03）但仍低于 z=400 的提示框。
     */
    public static void fadeTop(DrawContext c, int x1, int y, int x2, int rgb) {
        c.fillGradient(x1, y, x2, y + 8, 1, 0xFF000000 | rgb, rgb & 0xFFFFFF);
    }

    public static void fadeBottom(DrawContext c, int x1, int y2, int x2, int rgb) {
        c.fillGradient(x1, y2 - 8, x2, y2, 1, rgb & 0xFFFFFF, 0xFF000000 | rgb);
    }

    // ================================================================ tooltip / buttons / paper / 提示框、按钮、纸张

    /** Vanilla TooltipBackgroundRenderer geometry in Wathe colours; (x, y, w, h) is the content box.
     * 原版提示框几何、Wathe 配色；参数为内容区。 */
    public static void brassTooltip(DrawContext c, int x, int y, int w, int h) {
        c.fill(x - 3, y - 4, x + w + 3, y - 3, TIP_BG);
        c.fill(x - 3, y + h + 3, x + w + 3, y + h + 4, TIP_BG);
        c.fill(x - 3, y - 3, x + w + 3, y + h + 3, TIP_BG);
        c.fill(x - 4, y - 3, x - 3, y + h + 3, TIP_BG);
        c.fill(x + w + 3, y - 3, x + w + 4, y + h + 3, TIP_BG);
        c.fillGradient(x - 3, y - 2, x - 2, y + h + 2, BRASS_HI, BRASS_LO);
        c.fillGradient(x + w + 2, y - 2, x + w + 3, y + h + 2, BRASS_HI, BRASS_LO);
        c.fill(x - 3, y - 3, x + w + 3, y - 2, BRASS_HI);
        c.fill(x - 3, y + h + 2, x + w + 3, y + h + 3, BRASS_LO);
    }

    /** Content height of a guide tooltip in vanilla geometry: 8 for one line, else 10 per line (+2 after the first
     * line, the last line counting 8). 提示框内容高度（原版几何）：单行为 8，多行每行 10（首行后 +2，末行计 8）。 */
    public static int tooltipHeight(int lines) {
        return lines <= 1 ? 8 : 10 * lines;
    }

    /**
     * A guide-owned tooltip whose content box starts at (x, y): the brass recipe at z+400 in every context (lobby,
     * title screen or round), so it never falls back to vanilla purple next to the brass chrome. The first line is
     * {@code TEXT_HI}, later lines {@code TIP_DESC}; style colours win. The background is flushed before the text, as
     * vanilla does, so the glyphs always sit on top.
     * 指南自有的提示框，内容区左上角为 (x, y)：无论在大厅、标题界面还是对局中都以 z+400 绘制黄铜配方，不会在黄铜界面旁退回
     * 原版紫色。首行为 TEXT_HI，其余行为 TIP_DESC，文本自带颜色优先。与原版一样先提交背景再绘制文字，保证文字位于其上。
     */
    public static void tooltip(DrawContext c, TextRenderer f, List<Text> lines, int x, int y, int w) {
        int h = tooltipHeight(lines.size());
        c.getMatrices().push();
        c.getMatrices().translate(0, 0, 400);
        c.draw(() -> brassTooltip(c, x, y, w, h));
        int lineY = y;
        for (int i = 0; i < lines.size(); i++) {
            c.drawText(f, lines.get(i), x, lineY, i == 0 ? TEXT_HI : TIP_DESC, true);
            lineY += i == 0 ? 12 : 10;
        }
        c.getMatrices().pop();
    }

    /** Widest line of a tooltip. 提示框最宽一行的宽度。 */
    public static int tooltipWidth(TextRenderer f, List<Text> lines) {
        int width = 0;
        for (Text line : lines) {
            width = Math.max(width, f.getWidth(line));
        }
        return width;
    }

    /** 22x22 icon button = the panel recipe at 22 px (3 + 16 + 3); hover warms the body and coins the ring, no raise.
     * Hit area equals the visual. 22x22 图标按钮即 22 像素的面板配方；悬停只改底色与金币色环，不抬升；热区等于外观。 */
    public static void iconButton(DrawContext c, int x, int y, boolean hover) {
        int s = ICON_BUTTON_SIZE;
        dropShadow(c, x, y, s, s);
        roundedOutline(c, x, y, s, s, EDGE);
        c.fill(x + 1, y + 1, x + s - 1, y + s - 1, hover ? BUTTON_HOVER : BODY);
        bevel(c, x, y, s, s);
        ring(c, x, y, s, s, hover ? COIN : BRASS_HI, hover ? BRASS : BRASS_LO);
        c.drawTexture(BOOK_ICON, x + 3, y + 3, 0, 0, 16, 16, 16, 16);
    }

    /** The book icon at exactly 2x (32x32), for the frontispiece. 扉页用的 2 倍书本图标。 */
    public static void bookIcon2x(DrawContext c, int x, int y) {
        c.getMatrices().push();
        c.getMatrices().translate(x, y, 0);
        c.getMatrices().scale(2, 2, 1); // exact 2x only / 仅允许精确 2 倍
        c.drawTexture(BOOK_ICON, 0, 0, 0, 0, 16, 16, 16, 16);
        c.getMatrices().pop();
    }

    /** Parchment sheet: lit 2 px top/left, shaded outer bottom/right, top glint and foxed corners.
     * 羊皮纸：左上 2 像素受光、右下外沿阴影、顶部高光与角部霉斑。 */
    public static void paper(DrawContext c, int x, int y, int w, int h) {
        c.fill(x, y, x + w, y + h, PAPER_EDGE);
        c.fill(x + 2, y + 2, x + w - 2, y + h - 2, PAPER);
        c.fill(x + 1, y + h - 1, x + w, y + h, PAPER_DEEP);
        c.fill(x + w - 1, y + 1, x + w, y + h, PAPER_DEEP);
        c.fill(x + 1, y + 1, x + w - 1, y + 2, PAPER_GLINT);
        foxCorner(c, x, y, 1, 1);
        foxCorner(c, x + w - 1, y, -1, 1);
        foxCorner(c, x, y + h - 1, 1, -1);
        foxCorner(c, x + w - 1, y + h - 1, -1, -1);
    }

    private static void foxCorner(DrawContext c, int cx, int cy, int dx, int dy) {
        c.fill(cx, cy, cx + 1, cy + 1, PAPER_DEEP);
        c.fill(cx + dx, cy, cx + dx + 1, cy + 1, PAPER_DEEP);
        c.fill(cx, cy + dy, cx + 1, cy + dy + 1, PAPER_DEEP);
    }

    /** Width of the "本局已获得" chip: label + 15, or 11 for the star-only chip. 徽记宽度：完整为标签宽 + 15，仅星标为 11。 */
    public static int chipWidth(TextRenderer f, @Nullable OrderedText label) {
        return label == null ? 11 : f.getWidth(label) + 15;
    }

    /** Gilt "marked this round" chip, right-aligned at {@code right}; a null label draws the star-only chip.
     * 金箔“本局已获得”徽记，右对齐于 right；标签为 null 时只绘制星标。 */
    public static void chip(DrawContext c, TextRenderer f, @Nullable OrderedText label, int right, int y) {
        int w = chipWidth(f, label);
        int x = right - w;
        roundedFill(c, x, y, w, 11, GILT_LEAF);
        roundedOutline(c, x, y, w, 11, BRASS);
        star(c, x + 2, y + 2, GILT_STAR);
        if (label != null) {
            c.drawText(f, label, x + 12, y + 2, GILT_INK, false);
        }
    }

    // ================================================================ icons (1 px stroke family) / 图标

    public static void chevron(DrawContext c, int x, int y, boolean expanded, int col) {
        if (expanded) {
            c.fill(x, y + 1, x + 5, y + 2, col);
            c.fill(x + 1, y + 2, x + 4, y + 3, col);
            c.fill(x + 2, y + 3, x + 3, y + 4, col);
        } else {
            c.fill(x + 1, y, x + 2, y + 5, col);
            c.fill(x + 2, y + 1, x + 3, y + 4, col);
            c.fill(x + 3, y + 2, x + 4, y + 3, col);
        }
    }

    /** Identity gem 5x5: brass bezel (corners cut), 3x3 identity, glint TL, shade BR. {@code rgb} is 0xRRGGBB.
     * 5x5 身份宝石：黄铜镶边（切角）、3x3 身份色、左上高光、右下阴影。 */
    public static void gem(DrawContext c, int x, int y, int rgb, boolean bright) {
        int identity = 0xFF000000 | rgb;
        int bezel = bright ? BRASS_HI : BRASS_LO;
        int glint = mix(identity, 0xFFFFFFFF, 0.45);
        int shade = mix(identity, 0xFF000000, 0.35);
        for (int row = 0; row < GEM.length; row++) {
            String line = GEM[row];
            int start = 0;
            while (start < line.length()) {
                char key = line.charAt(start);
                int end = start + 1;
                while (end < line.length() && line.charAt(end) == key) {
                    end++;
                }
                int col = switch (key) {
                    case 'B' -> bezel;
                    case 'G' -> glint;
                    case 'S' -> shade;
                    case 'I' -> identity;
                    default -> 0;
                };
                if (col != 0) {
                    c.fill(x + start, y + row, x + end, y + row + 1, col);
                }
                start = end;
            }
        }
    }

    /** The same gem at exactly 2x (10x10), bezel BRASS_LO. 精确 2 倍的同一宝石（10x10）。 */
    public static void gem2x(DrawContext c, int x, int y, int rgb) {
        c.getMatrices().push();
        c.getMatrices().translate(x, y, 0);
        c.getMatrices().scale(2, 2, 1); // exact 2x only / 仅允许精确 2 倍
        gem(c, 0, 0, rgb, false);
        c.getMatrices().pop();
    }

    public static void star(DrawContext c, int x, int y, int col) {
        bitmap(c, x, y, col, STAR);
    }

    public static void diamond(DrawContext c, int x, int y, int col) {
        bitmap(c, x, y, col, DIAMOND);
    }

    public static void diamondOutline(DrawContext c, int x, int y, int col) {
        bitmap(c, x, y, col, DIAMOND_OUTLINE);
    }

    public static void magnifier(DrawContext c, int x, int y, int col) {
        bitmap(c, x, y, col, MAGNIFIER);
    }

    public static void close7(DrawContext c, int x, int y, int col) {
        bitmap(c, x, y, col, CLOSE);
    }

    /** 5x3 roll-up chevron (directory fold). 5x3 收起箭头。 */
    public static void foldUp(DrawContext c, int x, int y, int col) {
        bitmap(c, x, y, col, FOLD_UP);
    }

    public static void clear5(DrawContext c, int x, int y, int col) {
        bitmap(c, x, y, col, CLEAR);
    }

    public static void chevronSmall(DrawContext c, int x, int y, int col) {
        bitmap(c, x, y, col, CHEVRON_SMALL);
    }

    public static void check(DrawContext c, int x, int y, int col) {
        bitmap(c, x, y, col, CHECK);
    }

    public static void play(DrawContext c, int x, int y, int col) {
        bitmap(c, x, y, col, PLAY);
    }

    public static void hourglass(DrawContext c, int x, int y, int col) {
        bitmap(c, x, y, col, HOURGLASS);
    }

    /** 5x7 padlock; the keyhole (x+2, y+4..y+6) is filled with {@code hole}, the surface behind it. 挂锁，钥匙孔填底色。 */
    public static void padlock(DrawContext c, int x, int y, int col, int hole) {
        bitmap(c, x, y, col, PADLOCK);
        c.fill(x + 2, y + 4, x + 3, y + 6, hole);
    }

    public static void bullet(DrawContext c, int x, int y, int col) {
        bitmap(c, x, y, col, BULLET);
    }

    /** '#' = 1 px of {@code col}; horizontal runs are merged into one fill (pixel-identical).
     * '#' 表示 1 像素；同一行的连续像素合并为一次填充（像素结果不变）。 */
    public static void bitmap(DrawContext c, int x, int y, int col, String... rows) {
        for (int row = 0; row < rows.length; row++) {
            String line = rows[row];
            int start = line.indexOf('#');
            while (start >= 0) {
                int end = start + 1;
                while (end < line.length() && line.charAt(end) == '#') {
                    end++;
                }
                c.fill(x + start, y + row, x + end, y + row + 1, col);
                start = line.indexOf('#', end);
            }
        }
    }

    // ================================================================ helpers / 工具

    /** Trim to {@code maxWidth} with a trailing "…" (mock rule: the ellipsis width counts); a space before the "…"
     * is dropped ("Spark…", never "Spark …"). 超宽时截断并加省略号（省略号宽度计入）；省略号前的空格会被去掉。 */
    public static String ellipsize(TextRenderer f, String text, int maxWidth) {
        if (f.getWidth(text) <= maxWidth) {
            return text;
        }
        StringBuilder kept = new StringBuilder();
        int[] codePoints = text.codePoints().toArray();
        for (int codePoint : codePoints) {
            int before = kept.length();
            kept.appendCodePoint(codePoint);
            if (f.getWidth(kept + ELLIPSIS) > maxWidth) {
                kept.setLength(before);
                break;
            }
        }
        return kept.toString().stripTrailing() + ELLIPSIS;
    }

    /** The first candidate that fits {@code maxWidth} (longest wording first), else the last one ellipsized.
     * 返回第一个放得下的候选文字（按从长到短排列），都放不下时截断最后一个。 */
    public static String firstFitting(TextRenderer f, int maxWidth, String... candidates) {
        for (String candidate : candidates) {
            if (f.getWidth(candidate) <= maxWidth) {
                return candidate;
            }
        }
        return ellipsize(f, candidates[candidates.length - 1], maxWidth);
    }

    /** Per-channel ARGB lerp rounded to nearest, matching the mockup renderer (not MathHelper's floor).
     * 按通道线性插值并四舍五入，与样稿渲染器一致。 */
    public static int mix(int a, int b, double t) {
        int alpha = (int) Math.round((a >>> 24) * (1 - t) + (b >>> 24) * t);
        int red = (int) Math.round(((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
        int green = (int) Math.round(((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
        int blue = (int) Math.round((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }
}
