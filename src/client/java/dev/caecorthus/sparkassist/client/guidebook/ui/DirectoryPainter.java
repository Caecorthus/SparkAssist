package dev.caecorthus.sparkassist.client.guidebook.ui;

import static dev.caecorthus.sparkassist.client.guidebook.ui.ExpressPalette.BODY;
import static dev.caecorthus.sparkassist.client.guidebook.ui.ExpressPalette.BRASS;
import static dev.caecorthus.sparkassist.client.guidebook.ui.ExpressPalette.BRASS_HI;
import static dev.caecorthus.sparkassist.client.guidebook.ui.ExpressPalette.BRASS_LO;
import static dev.caecorthus.sparkassist.client.guidebook.ui.ExpressPalette.COIN;
import static dev.caecorthus.sparkassist.client.guidebook.ui.ExpressPalette.EDGE;
import static dev.caecorthus.sparkassist.client.guidebook.ui.ExpressPalette.FAINT;
import static dev.caecorthus.sparkassist.client.guidebook.ui.ExpressPalette.HEADING;
import static dev.caecorthus.sparkassist.client.guidebook.ui.ExpressPalette.ICON_ACTIVE;
import static dev.caecorthus.sparkassist.client.guidebook.ui.ExpressPalette.ICON_HOVER;
import static dev.caecorthus.sparkassist.client.guidebook.ui.ExpressPalette.MUTED;
import static dev.caecorthus.sparkassist.client.guidebook.ui.ExpressPalette.POLISHED;
import static dev.caecorthus.sparkassist.client.guidebook.ui.ExpressPalette.TEXT;
import static dev.caecorthus.sparkassist.client.guidebook.ui.ExpressPalette.TEXT_HI;
import static dev.caecorthus.sparkassist.client.guidebook.ui.ExpressPalette.TITLE;
import static dev.caecorthus.sparkassist.client.guidebook.ui.ExpressPalette.WELL;
import static dev.caecorthus.sparkassist.client.guidebook.ui.ExpressPalette.WELL_LIP;
import static dev.caecorthus.sparkassist.client.guidebook.ui.ExpressPalette.WELL_SHADE;

import dev.caecorthus.sparkassist.client.guidebook.ui.decor.DrawContextSink;
import dev.caecorthus.sparkassist.guidebook.GuidebookLayout.Region;
import dev.caecorthus.sparkassist.guidebook.decor.Ornaments;
import dev.caecorthus.sparkassist.guidebook.decor.PixelSink;
import java.util.List;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import org.jetbrains.annotations.Nullable;

/**
 * Stateless directory drawing (spec-final §4.2–4.5), ported from the mockup's {@code Parts.directory}. The screen owns
 * all state and hit testing; the static geometry helpers below are the single source of every hit box, so what is
 * clicked is exactly what is painted. Row coordinates are content offsets: y 0 is the viewport top at scroll 0.
 * 无状态的目录绘制（spec-final §4.2–4.5），移植自样稿 {@code Parts.directory}。状态与命中判定由界面负责；下方的静态几何
 * 方法是所有热区的唯一来源，保证点击区域与绘制位置一致。行坐标为内容偏移：滚动为 0 时 y 0 位于视口顶边。
 */
public final class DirectoryPainter {
    public static final int ROOT_HEIGHT = 14;
    /** Margin above every root row except the first; excluded from wash and hit. 非首个根行上方的间隔，不计入底色与热区。 */
    public static final int ROOT_GAP = 3;
    public static final int ROW_HEIGHT = 12;
    private static final int BAND_BOTTOM = 23;
    private static final int SEARCH_BOTTOM = 42;
    private static final int FOOTER = 22;
    private static final int EMPTY_BODY = 44;
    private static final int ICON_PITCH = 14;

    private DirectoryPainter() {
    }

    // ================================================================ geometry / 几何

    public static int rowHeight(int depth, boolean first) {
        return depth == 0 ? ROOT_HEIGHT + (first ? 0 : ROOT_GAP) : ROW_HEIGHT;
    }

    public static int bodyHeight(int depth) {
        return depth == 0 ? ROOT_HEIGHT : ROW_HEIGHT;
    }

    /** Content-fit height: band 23 (+19 with search) + rows (44 for the empty state) + 3 + footer 22.
     * 按内容取高：标题带 23（搜索展开再加 19）+ 行高（无结果时为 44）+ 3 + 页脚 22。 */
    public static int need(boolean searchOpen, int rowsHeight, boolean empty) {
        return (searchOpen ? SEARCH_BOTTOM : BAND_BOTTOM) + Math.max(rowsHeight, empty ? EMPTY_BODY : 0) + 3 + FOOTER;
    }

    /** Scissored row viewport (x+3, vy)..(x+w-3, y+h-22). 裁剪后的行视口。 */
    public static Region viewport(Region nav, boolean searchOpen) {
        int top = nav.y() + (searchOpen ? SEARCH_BOTTOM : BAND_BOTTOM);
        return new Region(nav.x() + 3, top, Math.max(0, nav.width() - 6), Math.max(0, nav.bottom() - FOOTER - top));
    }

    public static int rowLeft(Region nav) {
        return nav.x() + 4;
    }

    public static int rowRight(Region nav, boolean scrollable) {
        return nav.right() - 4 - (scrollable ? 5 : 0);
    }

    /** Scrollbar drag hit: the rightmost 5 px of the viewport, i.e. the 3 px gutter at x+w-8 and its margin.
     * 滚动条拖动热区：视口最右 5 像素，即 x+w-8 处的 3 像素槽及其边距。 */
    public static Region scrollHit(Region nav, boolean searchOpen) {
        Region view = viewport(nav, searchOpen);
        return new Region(view.right() - 5, view.y(), 5, view.height());
    }

    /** Rightmost 14x14 band icon box (the close × when present). 标题带最右侧的 14x14 图标热区。 */
    public static Region closeBox(Region nav) {
        return new Region(nav.right() - 18, nav.y() + 4, ICON_PITCH, ICON_PITCH);
    }

    public static Region searchToggle(Region nav, boolean closeInBand) {
        return new Region(nav.right() - 18 - (closeInBand ? ICON_PITCH : 0), nav.y() + 4, ICON_PITCH, ICON_PITCH);
    }

    /** Search well; clicking anywhere in it focuses the field. 搜索凹槽；点击其中任意位置都会聚焦输入框。 */
    public static Region well(Region nav) {
        return new Region(nav.x() + 6, nav.y() + 24, Math.max(0, nav.width() - 12), 14);
    }

    /** Clear × hit, tested before the text field. 清除按钮热区，先于输入框判定。 */
    public static Region clearBox(Region nav) {
        return new Region(nav.right() - 18, nav.y() + 24, 11, 14);
    }

    public static int fieldX(Region nav) {
        return nav.x() + 21;
    }

    public static int fieldY(Region nav) {
        return nav.y() + 27;
    }

    public static int fieldWidth(Region nav) {
        return Math.max(1, nav.width() - 39);
    }

    public static Region foldBox(Region nav) {
        return new Region(nav.right() - 18, nav.bottom() - 18, ICON_PITCH, ICON_PITCH);
    }

    /** Bottom 20 px of the panel minus the fold box. 面板底部 20 像素，扣除收起按钮。 */
    public static Region creditsZone(Region nav, boolean foldable) {
        return new Region(nav.x(), nav.bottom() - 20, linkRight(nav, foldable) - nav.x(), 20);
    }

    private static int linkRight(Region nav, boolean foldable) {
        return foldable ? nav.right() - 19 : nav.right() - 3;
    }

    /** Space the title may use before the leftmost band icon box - 4. 标题可用宽度（止于最左图标热区前 4 像素）。 */
    public static int titleRoom(Region nav, boolean closeInBand) {
        return searchToggle(nav, closeInBand).x() - 4 - (nav.x() + 7);
    }

    /** Label x of a row at depth 0/1/2. 各层级行标签的横坐标。 */
    public static int labelX(int depth, int rowLeft) {
        return rowLeft + (depth == 0 ? 11 : depth == 1 ? 18 : 25);
    }

    /**
     * Widest label that still clears the count, star and rule gap (roots and groups) or the trailing star (entries,
     * and roots or groups whose count was dropped: {@code countWidth} 0). Pass the count's fixed reserve, not its live
     * width, so a label never changes while search counts shrink. The mockups never hit this limit; it only matters in
     * narrow columns (72 px at 320×240), where the screen drops the count before it shortens the name.
     * 标签的最大宽度：根与分组行需避开计数、星标与刻线间隔；条目行（以及计数被省略、countWidth 为 0 的根与分组行）需避开末尾
     * 星标。传入计数的固定预留宽度而非实时宽度，搜索时计数变短也不会改变标签。样稿尺寸下不会触发，只在窄目录（320×240 下 72
     * 像素）中起作用，此时界面会先省略计数，再考虑截断名称。
     * <p>Before a star the gap is 3 px, not the rule's 4: the star has no left bearing and a hanzi keeps 1 px of right
     * bearing, so the visible gap is still 4 px, and "身份与阵营" (45) keeps its count and star in the 98 px column at
     * 427×240. 星标前的间隔为 3 像素而非刻线的 4 像素：星标没有左侧留白，汉字自带 1 像素右侧留白，视觉间隔仍为 4 像素；
     * 427×240 下 98 像素目录中的“身份与阵营”（45）因此可同时保留计数与星标。
     */
    public static int labelLimit(int depth, int rowLeft, int rowRight, int countWidth, boolean star, boolean marked) {
        int x = labelX(depth, rowLeft);
        if (depth == 2 || countWidth == 0) {
            return rowRight - x - ((depth == 2 ? marked : star) ? 12 : 3);
        }
        int countX = rowRight - 3 - countWidth;
        return (star ? countX - 11 - 3 : countX - 4) - x;
    }

    /**
     * Content box of a row tooltip (the full name, "本局已获得"), anchored to the row rather than the pointer so it
     * never covers the row's own star: beside the panel on the label line (mock m1b), else under the row (above it
     * at the screen bottom), always inside the screen with the 4 px tooltip border.
     * 行提示框（完整名称、“本局已获得”）的内容区：锚定在行上而非指针处，不会遮住该行自己的星标。优先放在面板右侧、与标签同一
     * 行（样稿 m1b）；右侧放不下时放在行下方（靠近屏幕底部时放在行上方），并连同 4 像素边框始终留在屏幕内。
     *
     * <p>Beside the panel it must also clear every obstacle (shop items and their price tags, target heads): at
     * 640×360 the first price tag sits right there. When the box, border included, would touch one, it goes under
     * the row, where it can only cover the guide's own neighbouring rows.
     * 面板右侧的位置还必须避开所有障碍物（商品及其价签、目标头像）：640×360 下第一个价签恰好在那里。提示框连同边框会
     * 碰到障碍物时改放在行下方，此时只会遮住指南自己的相邻行。
     *
     * @param rowY      screen y of the row body / 行体在屏幕上的纵坐标
     * @param labelX    screen x of the row label / 行标签在屏幕上的横坐标
     * @param obstacles regions the box must not cover beside the panel (empty when nothing live is visible, e.g.
     *                  standalone or under the modal scrim) / 面板右侧不可遮挡的区域（独立界面或模态遮罩下为空）
     */
    public static Region rowTipBox(Region nav, int depth, int rowY, int labelX, int tipWidth, int tipHeight,
                                   int screenWidth, int screenHeight, List<Region> obstacles) {
        int x = nav.right() + 6;
        int y = rowY + (depth == 0 ? 3 : 2);
        if (x + tipWidth + 4 > screenWidth || touches(new Region(x - 4, y - 4, tipWidth + 8, tipHeight + 8),
                obstacles)) {
            x = Math.max(4, Math.min(labelX, screenWidth - 4 - tipWidth));
            y = rowY + bodyHeight(depth) + 6;
            if (y + tipHeight + 4 > screenHeight) {
                y = rowY - 6 - tipHeight;
            }
        }
        y = Math.max(4, Math.min(y, screenHeight - 4 - tipHeight));
        return new Region(x, y, tipWidth, tipHeight);
    }

    private static boolean touches(Region box, List<Region> obstacles) {
        for (Region obstacle : obstacles) {
            if (obstacle.intersects(box)) {
                return true;
            }
        }
        return false;
    }

    // ================================================================ model / 模型

    /**
     * One laid-out directory row. {@code top} includes the root margin, {@code bodyTop} excludes it; both are content
     * offsets. {@code count} is null when there is none or the column dropped it; {@code countReserve} is the fixed
     * width the count may take ("88"), used for every fit decision so nothing flickers while search counts change.
     * {@code color} is the identity 0xRRGGBB (entries only). {@code containsMarked} and {@code marked} only drive the
     * star, so the screen clears them when the column is too narrow for it (the tooltip still says "本局已获得").
     * 一行目录的排版结果：top 含根行间隔，bodyTop 不含；二者均为内容偏移。count 在没有计数或目录过窄而省略时为 null；
     * countReserve 为计数的固定预留宽度（“88”），所有排布判断都以它为准，搜索时计数变化也不会闪烁。color 为条目身份色。
     * containsMarked 与 marked 只控制星标，目录过窄放不下星标时界面会将其置为 false（行提示仍显示“本局已获得”）。
     */
    public record Row(int depth, int top, int bodyTop, OrderedText label, int labelWidth,
                      @Nullable OrderedText count, int countWidth, int countReserve, boolean expanded,
                      boolean containsMarked, boolean marked, boolean overview, int color, @Nullable String entryId) {
        public int bodyBottom() {
            return bodyTop + bodyHeight(depth);
        }
    }

    /** Chrome strings, laid out once per geometry change (never per frame). The placeholder is one wording for both
     * search states, chosen to fit the focused (7 px narrower) room. 界面文字，仅在几何变化时排版一次；占位文字按聚焦时
     * （窄 7 像素）的宽度选定，聚焦与否使用同一措辞。 */
    public record Chrome(OrderedText title, OrderedText placeholder, OrderedText credits, int creditsWidth,
                         OrderedText noResults, int noResultsWidth) {
    }

    /**
     * Everything one frame needs. Indices refer to {@code rows}; -1 means none. {@code selectedId} is null while the
     * credits page is open.
     * 单帧绘制所需的全部信息；索引对应 rows，-1 表示无。鸣谢页打开时 selectedId 为 null。
     */
    public record Model(Region nav, Chrome chrome, List<Row> rows, int contentHeight, int scroll,
                        boolean searchOpen, boolean searchFocused, boolean queryEmpty, boolean closeInBand,
                        boolean foldable, boolean creditsOpen, int hoveredRow, int focusedRow,
                        @Nullable String selectedId, boolean closeHover, boolean searchHover, boolean creditsHover,
                        boolean foldHover, boolean thumbHot, boolean trim, int titleWidth, int accent) {
        /**
         * @param trim       draw the frame ornaments (corner plates, inner line, nameplate, tag) / 是否画边框点缀
         * @param titleWidth width of the shown band title, for the nameplate and the tag / 标题带标题的宽度
         * @param accent     the player's faction colour 0xRRGGBB for the luggage tag / 行李牌的阵营色
         */
        public Model {
        }
    }

    // ================================================================ drawing / 绘制

    public static void draw(DrawContext c, TextRenderer f, Model m) {
        drawChrome(c, f, m);
        drawRows(c, f, m);
    }

    /**
     * Panel, band, search well and footer: everything under the rows. The screen draws the decoration layer
     * (the sigil under the last row) after this and {@link #drawRows} after that, so the layer sits between body and
     * rows.
     * 面板、标题带、搜索槽与页脚，即行之下的一切。界面在其后画点缀层（末行下方的暗纹），再调用 drawRows，点缀层因此夹在
     * 面板底与行之间。
     */
    public static void drawChrome(DrawContext c, TextRenderer f, Model m) {
        Region nav = m.nav();
        int x = nav.x();
        int y = nav.y();
        int w = nav.width();
        int h = nav.height();
        ExpressPaint.panel(c, x, y, w, h);
        ExpressPaint.titleBand(c, x, y, w);
        if (m.trim()) {
            PixelSink sink = new DrawContextSink(c);
            Ornaments.innerLine(sink, x, y, w, h);
            Ornaments.cornerPlates(sink, x, y, w, h);
        }
        band(c, f, m);
        if (m.searchOpen()) {
            searchWell(c, f, m);
        }
        footer(c, f, m);
    }

    /** The scissored rows with their fades and scrollbar. 裁剪视口内的行、渐隐与滚动条。 */
    public static void drawRows(DrawContext c, TextRenderer f, Model m) {
        Region nav = m.nav();
        int x = nav.x();
        int w = nav.width();
        Region view = viewport(nav, m.searchOpen());
        int total = m.contentHeight();
        boolean scrollable = total > view.height();
        int rx0 = rowLeft(nav);
        int rx1 = rowRight(nav, scrollable);
        if (view.height() > 0) {
            c.enableScissor(view.x(), view.y(), view.right(), view.bottom());
            if (m.rows().isEmpty()) {
                noResults(c, f, m, view);
            }
            int origin = view.y() - m.scroll();
            for (int i = 0; i < m.rows().size(); i++) {
                Row row = m.rows().get(i);
                int ry = origin + row.bodyTop();
                if (ry + bodyHeight(row.depth()) <= view.y() || ry >= view.bottom()) {
                    continue;
                }
                row(c, f, m, row, i, rx0, rx1, ry);
            }
            c.disableScissor();
        }
        if (scrollable && view.height() > 0) {
            int body = BODY & 0xFFFFFF;
            if (m.scroll() > 0) {
                ExpressPaint.fadeTop(c, x + 3, view.y(), x + w - 8, body);
            }
            if (m.scroll() < total - view.height()) {
                ExpressPaint.fadeBottom(c, x + 3, view.bottom(), x + w - 8, body);
            }
            int thumb = ExpressPaint.thumbSize(view.height(), view.height(), total);
            int thumbY = ExpressPaint.thumbOffset(view.y(), view.height(), thumb, m.scroll(), view.height(), total);
            // The fades write depth at z+1, so the rod sits at z+2 to stay visible above them.
            // 渐隐在 z+1 写入深度，滑杆置于 z+2 才不会被遮住。
            c.getMatrices().push();
            c.getMatrices().translate(0, 0, 2);
            ExpressPaint.scrollbarDark(c, x + w - 8, view.y(), view.height(), thumbY, thumb, m.thumbHot());
            c.getMatrices().pop();
        }
    }

    /**
     * "No results" inside the scissored viewport, so it never reaches the footer: the magnifier and text at +12/+26
     * when the viewport has the room (36 px), else the text alone, centred (a short persistent directory above target
     * heads can leave only ~20 px).
     * “没有匹配的条目”绘制在裁剪视口内，永远不会压到页脚：视口高度足够（36 像素）时放大镜与文字位于 +12/+26，否则只绘制
     * 居中的文字（位于目标头像上方的矮常驻目录可能只剩约 20 像素）。
     */
    private static void noResults(DrawContext c, TextRenderer f, Model m, Region view) {
        int cx = m.nav().x() + m.nav().width() / 2;
        int textX = cx - m.chrome().noResultsWidth() / 2;
        if (view.height() >= 36) {
            ExpressPaint.magnifier(c, cx - 4, view.y() + 12, FAINT);
            c.drawText(f, m.chrome().noResults(), textX, view.y() + 26, MUTED, true);
        } else {
            c.drawText(f, m.chrome().noResults(), textX, view.y() + Math.max(0, (view.height() - 9) / 2), MUTED, true);
        }
    }

    private static void band(DrawContext c, TextRenderer f, Model m) {
        Region nav = m.nav();
        Region close = closeBox(nav);
        Region search = searchToggle(nav, m.closeInBand());
        if (m.closeInBand()) {
            if (m.closeHover()) {
                ExpressPaint.iconWash(c, close.x(), close.y(), ICON_HOVER);
            }
            ExpressPaint.close7(c, nav.right() - 14, nav.y() + 8, m.closeHover() ? COIN : BRASS_HI);
        }
        if (m.searchHover() || m.searchOpen()) {
            ExpressPaint.iconWash(c, search.x(), search.y(), m.searchHover() ? ICON_HOVER : ICON_ACTIVE);
        }
        ExpressPaint.magnifier(c, search.x() + 2, nav.y() + 6,
                m.searchOpen() ? COIN : m.searchHover() ? BRASS_HI : BRASS);
        if (m.trim()) {
            // Nameplate under the title; the luggage tag only when the band keeps 6 px between title and string.
            // 标题下的铭牌；行李牌只在标题与绳子之间还剩 6 像素时出现。
            PixelSink sink = new DrawContextSink(c);
            Ornaments.nameplate(sink, nav.x() + 4, nav.y() + 5, m.titleWidth() + Ornaments.NAMEPLATE_PADDING);
            int tagX = search.x() - 6 - Ornaments.TAG_WIDTH;
            if (tagX - Ornaments.TAG_STRING - 6 >= nav.x() + 7 + m.titleWidth()) {
                Ornaments.luggageTag(sink, tagX, nav.y() + 7, m.accent());
            }
        }
        c.drawText(f, m.chrome().title(), nav.x() + 7, nav.y() + 7, TITLE, false);
    }

    private static void searchWell(DrawContext c, TextRenderer f, Model m) {
        Region nav = m.nav();
        Region well = well(nav);
        if (m.searchFocused()) {
            ExpressPaint.roundedOutline(c, well.x() - 1, well.y() - 1, well.width() + 2, well.height() + 2, BRASS_HI);
        }
        c.fill(well.x(), well.y(), well.right(), well.bottom(), WELL);
        c.fill(well.x(), well.y(), well.right(), well.y() + 1, WELL_SHADE);
        c.fill(well.x(), well.bottom() - 1, well.right(), well.bottom(), WELL_LIP);
        ExpressPaint.magnifier(c, well.x() + 3, well.y() + 3, FAINT);
        if (m.queryEmpty()) {
            // Drawn by us, focused or not; 7 px further right while focused, clear of vanilla's "_" caret.
            // 占位文字由本界面绘制；聚焦时右移 7 像素，避开原版的 “_” 光标。
            c.drawText(f, m.chrome().placeholder(), fieldX(nav) + (m.searchFocused() ? 7 : 0), fieldY(nav), FAINT, true);
        } else {
            ExpressPaint.clear5(c, nav.right() - 15, nav.y() + 28, FAINT);
        }
    }

    private static void row(DrawContext c, TextRenderer f, Model m, Row row, int index, int rx0, int rx1, int ry) {
        boolean hovered = index == m.hoveredRow();
        boolean selected = row.entryId() != null && row.entryId().equals(m.selectedId());
        ExpressPaint.rowState(c, rx0, ry, rx1, bodyHeight(row.depth()), hovered, selected, index == m.focusedRow());
        int labelX = labelX(row.depth(), rx0);
        boolean star = row.containsMarked() && !row.expanded();
        if (row.depth() == 0) {
            ExpressPaint.chevron(c, rx0 + 3, ry + 4, row.expanded(), hovered ? POLISHED : BRASS_HI);
            c.drawText(f, row.label(), labelX, ry + 3, hovered ? POLISHED : HEADING, false);
            if (row.count() == null) {
                // Narrow column: the count was dropped, the star takes its slot. 窄目录：计数已省略，星标占据其位置。
                if (star) {
                    ExpressPaint.star(c, rx1 - 10, ry + 3, COIN);
                }
                return;
            }
            int countX = rx1 - 3 - row.countWidth();
            c.drawText(f, row.count(), countX, ry + 3, FAINT, true);
            int ruleEnd = countX - 4;
            // Whether the rule fits is decided on the count reserve, never the live count, so it cannot appear and
            // vanish while typing a search; once drawn it runs to the live count.
            // 刻线是否绘制按计数预留宽度判断而非实时计数，输入搜索时不会时隐时现；绘制时仍延伸到实时计数前。
            int fitEnd = rx1 - 3 - row.countReserve() - 4;
            if (star) {
                ExpressPaint.star(c, countX - 11, ry + 3, COIN);
                ruleEnd = countX - 15;
                fitEnd -= 11;
            }
            int end = labelX + row.labelWidth();
            if (fitEnd - (end + 4) >= 12) {
                c.fill(end + 4, ry + 7, ruleEnd, ry + 8, BRASS_LO);
                c.fill(end + 4, ry + 8, ruleEnd, ry + 9, EDGE);
            }
        } else if (row.depth() == 1) {
            ExpressPaint.chevron(c, rx0 + 10, ry + 3, row.expanded(), hovered ? TEXT_HI : MUTED);
            c.drawText(f, row.label(), labelX, ry + 2, hovered ? TEXT_HI : TEXT, true);
            if (row.count() == null) {
                if (star) {
                    ExpressPaint.star(c, rx1 - 10, ry + 2, COIN);
                }
                return;
            }
            int countX = rx1 - 3 - row.countWidth();
            c.drawText(f, row.count(), countX, ry + 2, FAINT, true);
            if (star) {
                ExpressPaint.star(c, countX - 11, ry + 2, COIN);
            }
        } else {
            ExpressPaint.gem(c, rx0 + 17, ry + 3, row.color(), selected || hovered);
            int color = selected || hovered ? TEXT_HI : row.overview() ? MUTED : TEXT;
            c.drawText(f, row.label(), labelX, ry + 2, color, true);
            if (row.marked()) {
                ExpressPaint.star(c, rx1 - 10, ry + 2, COIN);
            }
        }
    }

    private static void footer(DrawContext c, TextRenderer f, Model m) {
        Region nav = m.nav();
        int x = nav.x();
        int bottom = nav.bottom();
        ExpressPaint.etched(c, x + 3, nav.right() - 3, bottom - 21);
        int textY = bottom - 14;
        int color = m.creditsHover() || m.creditsOpen() ? COIN : MUTED;
        if (m.creditsHover()) {
            c.fill(x + 3, bottom - 19, linkRight(nav, m.foldable()), bottom - 3, ICON_ACTIVE);
        }
        c.drawText(f, m.chrome().credits(), x + 7, textY, color, true);
        ExpressPaint.chevronSmall(c, x + 7 + m.chrome().creditsWidth() + 3 + (m.creditsHover() ? 1 : 0), textY + 2,
                color);
        if (m.foldable()) {
            Region fold = foldBox(nav);
            if (m.foldHover()) {
                ExpressPaint.iconWash(c, fold.x(), fold.y(), ICON_HOVER);
            }
            ExpressPaint.foldUp(c, nav.right() - 13, bottom - 12, m.foldHover() ? BRASS_HI : BRASS);
        }
    }
}
