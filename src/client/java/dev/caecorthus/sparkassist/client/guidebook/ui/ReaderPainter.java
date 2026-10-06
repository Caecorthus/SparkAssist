package dev.caecorthus.sparkassist.client.guidebook.ui;

import static dev.caecorthus.sparkassist.client.guidebook.ui.ExpressPalette.BRASS;
import static dev.caecorthus.sparkassist.client.guidebook.ui.ExpressPalette.BRASS_HI;
import static dev.caecorthus.sparkassist.client.guidebook.ui.ExpressPalette.COIN;
import static dev.caecorthus.sparkassist.client.guidebook.ui.ExpressPalette.EDGE;
import static dev.caecorthus.sparkassist.client.guidebook.ui.ExpressPalette.FAINT;
import static dev.caecorthus.sparkassist.client.guidebook.ui.ExpressPalette.HEADING;
import static dev.caecorthus.sparkassist.client.guidebook.ui.ExpressPalette.ICON_HOVER;
import static dev.caecorthus.sparkassist.client.guidebook.ui.ExpressPalette.INK;
import static dev.caecorthus.sparkassist.client.guidebook.ui.ExpressPalette.INK_FAINT;
import static dev.caecorthus.sparkassist.client.guidebook.ui.ExpressPalette.INK_MUTED;
import static dev.caecorthus.sparkassist.client.guidebook.ui.ExpressPalette.INK_RULE;
import static dev.caecorthus.sparkassist.client.guidebook.ui.ExpressPalette.INK_RULE_SOFT;
import static dev.caecorthus.sparkassist.client.guidebook.ui.ExpressPalette.MUTED;
import static dev.caecorthus.sparkassist.client.guidebook.ui.ExpressPalette.PAPER;

import dev.caecorthus.sparkassist.client.guidebook.render.GuidebookContentRenderer;
import dev.caecorthus.sparkassist.client.guidebook.ui.decor.DrawContextSink;
import dev.caecorthus.sparkassist.guidebook.decor.Ornaments;
import dev.caecorthus.sparkassist.guidebook.decor.Plates;
import dev.caecorthus.sparkassist.guidebook.decor.PixelSink;
import dev.caecorthus.sparkassist.client.guidebook.render.GuidebookContentRenderer.Ornament;
import dev.caecorthus.sparkassist.client.guidebook.render.GuidebookContentRenderer.RenderedLine;
import dev.caecorthus.sparkassist.guidebook.GuidebookLayout.Region;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

/**
 * Stateless reader drawing on the parchment sheet (spec-final §5.3–5.5, §5.7), ported from the mockup's
 * {@code Scenes.reader/header/m6} and {@code Parts.drawLaid}. The band content (crumb, running head, source, close)
 * stays with the screen. Content coordinates: y 0 is the sheet interior top (P.y + 2) at scroll 0; the page header
 * occupies content y 0..{@link #HEADER_HEIGHT} and the laid body starts at {@link #HEADER_HEIGHT}; both scroll.
 * 羊皮纸正文的无状态绘制（spec-final §5.3–5.5、§5.7），移植自样稿。标题带内容（路径、页眉、来源、关闭）仍由界面负责。
 * 内容坐标：滚动为 0 时 y 0 位于纸张内侧顶边（P.y + 2）；页眉占据 0..HEADER_HEIGHT，正文从 HEADER_HEIGHT 开始，二者一起滚动。
 */
public final class ReaderPainter {
    /** Body start in content coordinates (gem, 2x title, chip and double rule above it). 正文在内容坐标中的起点。 */
    public static final int HEADER_HEIGHT = 37;
    /** Past this scroll the header rule has left the view and the band shows the running head. 超过后标题带显示页眉。 */
    public static final int RUNNING_HEAD_SCROLL = 29;
    /** Height the chapter plate adds above the header: the picture at content y 3 plus a gap. 扉画为页眉增加的高度。 */
    public static final int PLATE_BLOCK = Plates.HEIGHT + 8;
    /** Content y of the chapter plate's top edge. 扉画顶边的内容坐标。 */
    public static final int PLATE_TOP = 3;
    private static final int TITLE_X = 14;
    private static final String HEAD_SEPARATOR = " \u203A ";

    private ReaderPainter() {
    }

    /**
     * Sheet geometry for a reader frame r: P = (r.x+5, r.y+22, r.w-10, r.h-27).
     * 由阅读器外框 r 推出的纸张几何：P = (r.x+5, r.y+22, r.w-10, r.h-27)。
     */
    public record Sheet(int x, int y, int width, int height) {
        public static Sheet of(Region reader) {
            return new Sheet(reader.x() + 5, reader.y() + 22, reader.width() - 10, reader.height() - 27);
        }

        public int right() {
            return x + width;
        }

        public int bottom() {
            return y + height;
        }

        /** Text column left edge tx0 = P.x + 11. 文字栏左缘。 */
        public int textLeft() {
            return x + 11;
        }

        /** Text column right edge tx1 = P.right - 11. 文字栏右缘。 */
        public int textRight() {
            return right() - 11;
        }

        /** Measure W = tx1 - tx0 (252 for a 284 px reader); pass it to GuidebookContentRenderer.layout. 版心宽度。 */
        public int measure() {
            return textRight() - textLeft();
        }

        /** Scissored scroll viewport = sheet interior (P.x+2, P.y+2)..(P.r-2, P.b-2). 滚动视口即纸张内侧。 */
        public Region viewport() {
            return new Region(x + 2, y + 2, width - 4, height - 4);
        }

        /** Scrollbar gutter x = tx1 + 5. 滚动条槽横坐标。 */
        public int gutterX() {
            return textRight() + 5;
        }

        /** Thumb drag hit, separate from the text area. 滑块拖动热区，与文字区分开。 */
        public Region scrollHit() {
            return new Region(gutterX() - 1, y + 2, 5, height - 4);
        }
    }

    /**
     * Header fit decided once per layout (not per frame): 2x title + full chip, then 2x title + star-only chip, then
     * the 1x title; the title never shrinks below 1x.
     * 页眉排布在排版时一次决定：先 2 倍标题 + 完整徽记，其次 2 倍标题 + 仅星标徽记，最后 1 倍标题；标题不会小于 1 倍。
     */
    public record PageHeader(OrderedText title, boolean doubleTitle, int gemRgb, boolean marked,
                             @Nullable OrderedText chipLabel) {
        /**
         * @param gemRgb     identity colour 0xRRGGBB (credits: POLISHED) / 身份色
         * @param chipLabel  "本局已获得", used only when {@code marked} / 仅在已获得时使用
         * @param measure    text column width ({@link Sheet#measure()}) / 版心宽度
         */
        public static PageHeader fit(TextRenderer f, String title, int gemRgb, boolean marked, String chipLabel,
                                     int measure) {
            int titleWidth = f.getWidth(title);
            int chipWidth = marked ? f.getWidth(chipLabel) + 15 : 0;
            boolean chipFull = marked;
            int available = measure - TITLE_X;
            if (marked && 2 * titleWidth > available - chipWidth - 6) {
                chipFull = false;
                chipWidth = 11;
            }
            int titleRoom = available - (marked ? chipWidth + 6 : 0);
            boolean doubleTitle = 2 * titleWidth <= titleRoom;
            String shown = doubleTitle ? title : ExpressPaint.ellipsize(f, title, titleRoom);
            return new PageHeader(OrderedText.styledForwardsVisitedString(shown, Style.EMPTY), doubleTitle,
                    gemRgb & 0xFFFFFF, marked,
                    chipFull ? OrderedText.styledForwardsVisitedString(chipLabel, Style.EMPTY) : null);
        }
    }

    /** Close × hit box on the reader band (x+w-18, y+4, 14, 14). 阅读器标题带上的关闭热区。 */
    public static Region closeBox(Region reader) {
        return new Region(reader.right() - 18, reader.y() + 4, 14, 14);
    }

    /**
     * Band text laid out once per article (spec §5.3): the crumb, the right-aligned source ending at x+w-22, and the
     * running-head title (ellipsized to crumbMax - 8, drawn after the 5x5 gem). {@code crumbMax} is the room left of
     * the source - 8, or left of the close box - 4.
     * 每篇正文只排版一次的标题带文字：路径、右对齐止于 x+w-22 的来源、页眉标题（截断到 crumbMax - 8，绘于 5x5 宝石之后）。
     * crumbMax 为来源左侧减 8（无来源时为关闭热区左侧减 4）的可用宽度。
     */
    public record Band(@Nullable OrderedText crumb, @Nullable OrderedText source, int sourceWidth,
                       OrderedText headTitle, int headTitleWidth, String title, int gemRgb, int crumbMax) {
        public static Band fit(TextRenderer f, Region reader, String crumb, @Nullable String source, String title,
                               int gemRgb) {
            int box = reader.right() - 18;
            String shownSource = source == null ? "" : source;
            int sourceWidth = f.getWidth(shownSource);
            int crumbMax = (shownSource.isEmpty() ? box - 4 : box - 4 - sourceWidth - 8) - (reader.x() + 7);
            String head = ExpressPaint.ellipsize(f, title, crumbMax - 8);
            return new Band(crumb.isEmpty() ? null : plain(ExpressPaint.ellipsize(f, crumb, crumbMax)),
                    shownSource.isEmpty() ? null : plain(shownSource), sourceWidth, plain(head), f.getWidth(head),
                    title, gemRgb & 0xFFFFFF, crumbMax);
        }

        /** Hover box of the source label (tooltip "来源：%s"). 来源文字的悬停区域。 */
        public Region sourceHit(Region reader) {
            return new Region(reader.right() - 22 - sourceWidth, reader.y() + 3, source == null ? 0 : sourceWidth, 16);
        }

        /** The running head's section label, ellipsized to the room after " › "; null when it does not fit or only
         * repeats the title ({@link #runningLabel}). 页眉中分节标题按 “ › ” 之后的剩余宽度截断；放不下或与标题重复时为 null。 */
        public @Nullable OrderedText section(TextRenderer f, @Nullable Text section) {
            String label = runningLabel(section, title);
            if (label == null || headTitleWidth + 20 >= crumbMax) {
                return null;
            }
            int room = crumbMax - 8 - headTitleWidth - f.getWidth(HEAD_SEPARATOR);
            return plain(ExpressPaint.ellipsize(f, label, room));
        }
    }

    /**
     * Section part of the running head, or null. Skill pages open with a section named after the page itself
     * (魔女因子, 死亡射线), so a label equal to the title is dropped: the band reads "◆ 魔女因子" and then
     * "◆ 魔女因子 › 持有者", never "◆ 魔女因子 › 魔女因子".
     * 页眉的分节部分，可能为 null。技能页以与页面同名的分节开头（魔女因子、死亡射线），分节名与标题相同时页眉只显示标题，
     * 即先显示“◆ 魔女因子”，再显示“◆ 魔女因子 › 持有者”，避免“魔女因子 › 魔女因子”。
     */
    static @Nullable String runningLabel(@Nullable Text section, String title) {
        if (section == null) {
            return null;
        }
        String label = section.getString();
        return label.strip().equals(title.strip()) ? null : label;
    }

    /**
     * Reader band content: close × (ICON_HOVER wash + COIN when hovered), source in FAINT, then either the crumb in
     * HEADING or, once the header rule has scrolled away, the running head "◆ title › section".
     * 阅读器标题带内容：关闭按钮（悬停时底色与金币色）、FAINT 来源，以及 HEADING 路径；页眉线滚出后改为页眉 “◆ 标题 › 分节”。
     */
    public static void band(DrawContext c, TextRenderer f, Region reader, Band band, boolean running,
                            @Nullable OrderedText section, boolean closeHover) {
        band(c, f, reader, band, running, section, closeHover, false);
    }

    /** @param trim also draw the recessed nameplate under the crumb or running head / 是否在文字下画凹铭牌 */
    public static void band(DrawContext c, TextRenderer f, Region reader, Band band, boolean running,
                            @Nullable OrderedText section, boolean closeHover, boolean trim) {
        int x = reader.x();
        int y = reader.y();
        Region close = closeBox(reader);
        if (closeHover) {
            ExpressPaint.iconWash(c, close.x(), close.y(), ICON_HOVER);
        }
        ExpressPaint.close7(c, reader.right() - 14, y + 8, closeHover ? COIN : BRASS_HI);
        if (band.source() != null) {
            c.drawText(f, band.source(), reader.right() - 22 - band.sourceWidth(), y + 7, FAINT, true);
        }
        if (trim) {
            int textEnd;
            if (running) {
                textEnd = x + 15 + band.headTitleWidth()
                        + (section == null ? 0 : f.getWidth(HEAD_SEPARATOR) + f.getWidth(section));
            } else if (band.crumb() != null) {
                textEnd = x + 7 + f.getWidth(band.crumb());
            } else {
                textEnd = -1;
            }
            if (textEnd > 0) {
                Ornaments.nameplate(new DrawContextSink(c), x + 4, y + 5, textEnd + 4 - (x + 4));
            }
        }
        if (running) {
            ExpressPaint.gem(c, x + 7, y + 9, band.gemRgb(), false);
            c.drawText(f, band.headTitle(), x + 15, y + 7, HEADING, false);
            if (section != null) {
                int end = x + 15 + band.headTitleWidth();
                c.drawText(f, HEAD_SEPARATOR, end, y + 7, FAINT, true);
                c.drawText(f, section, end + f.getWidth(HEAD_SEPARATOR), y + 7, MUTED, true);
            }
        } else if (band.crumb() != null) {
            c.drawText(f, band.crumb(), x + 7, y + 7, HEADING, false);
        }
    }

    private static OrderedText plain(String text) {
        return OrderedText.styledForwardsVisitedString(text, Style.EMPTY);
    }

    /** Header height with or without the chapter plate above it. 有无扉画时的页眉高度。 */
    public static int headerHeight(boolean plate) {
        return (plate ? PLATE_BLOCK : 0) + HEADER_HEIGHT;
    }

    /** Scroll past which the band shows the running head. 超过后标题带显示页眉的滚动量。 */
    public static int runningHeadScroll(boolean plate) {
        return (plate ? PLATE_BLOCK : 0) + RUNNING_HEAD_SCROLL;
    }

    /** Total scroll content height: header + laid body. 滚动内容总高度。 */
    public static int contentHeight(GuidebookContentRenderer.Layout body) {
        return contentHeight(body, false);
    }

    public static int contentHeight(GuidebookContentRenderer.Layout body, boolean plate) {
        return headerHeight(plate) + body.height();
    }

    public static int maxScroll(Sheet sheet, GuidebookContentRenderer.Layout body) {
        return maxScroll(sheet, body, false);
    }

    public static int maxScroll(Sheet sheet, GuidebookContentRenderer.Layout body, boolean plate) {
        return Math.max(0, contentHeight(body, plate) - sheet.viewport().height());
    }

    /** Running-head section: null until the header rule has scrolled away, then the last section with its row top
     * at or above scroll + 6. 页眉的当前分节：标题线滚出前为 null，之后为行顶不低于 scroll + 6 的最后一个分节。 */
    public static @Nullable Text currentSection(GuidebookContentRenderer.Layout body, int scroll) {
        return currentSection(body, scroll, false);
    }

    public static @Nullable Text currentSection(GuidebookContentRenderer.Layout body, int scroll, boolean plate) {
        return scroll > runningHeadScroll(plate) ? body.sectionAt(scroll + 6 - headerHeight(plate)) : null;
    }

    /** 1 px EDGE recess ring outside P, then the paper. 纸张外圈 1 像素凹槽，然后绘制纸张。 */
    public static void sheet(DrawContext c, Sheet s) {
        c.fill(s.x() - 1, s.y() - 1, s.right() + 1, s.y(), EDGE);
        c.fill(s.x() - 1, s.bottom(), s.right() + 1, s.bottom() + 1, EDGE);
        c.fill(s.x() - 1, s.y(), s.x(), s.bottom(), EDGE);
        c.fill(s.right(), s.y(), s.right() + 1, s.bottom(), EDGE);
        ExpressPaint.paper(c, s.x(), s.y(), s.width(), s.height());
    }

    /** Reader frame: panel + title band + recessed sheet; the band text is drawn by the caller.
     * 阅读器外框：面板、标题带与嵌入的纸张；标题带文字由调用方绘制。 */
    public static void frame(DrawContext c, Region reader) {
        frame(c, reader, false);
    }

    /** @param trim add the double-ruled frame, corner plates and two book clasps on the left rim
     *             / 是否加双线框、角板与左沿的两枚书口扣 */
    public static void frame(DrawContext c, Region reader, boolean trim) {
        ExpressPaint.panel(c, reader.x(), reader.y(), reader.width(), reader.height());
        ExpressPaint.titleBand(c, reader.x(), reader.y(), reader.width());
        sheet(c, Sheet.of(reader));
        if (trim) {
            PixelSink sink = new DrawContextSink(c);
            Ornaments.innerLine(sink, reader.x(), reader.y(), reader.width(), reader.height());
            Ornaments.cornerPlates(sink, reader.x(), reader.y(), reader.width(), reader.height());
            Ornaments.clasp(sink, reader.x(), reader.y() + Math.round(reader.height() / 3f));
            Ornaments.clasp(sink, reader.x(), reader.y() + Math.round(reader.height() * 2 / 3f));
        }
    }

    /**
     * Scrolled content: header + body inside the viewport scissor, then 8 px PAPER fades (z+1) and the paper
     * scrollbar (z+2, because the fades write depth) when the content overflows.
     * 滚动内容：在视口裁剪内绘制页眉与正文；内容溢出时再绘制 8 像素纸色渐隐（z+1）与纸面滚动条（z+2，因为渐隐会写入深度）。
     */
    public static void content(DrawContext c, TextRenderer f, Sheet s, PageHeader header,
                               GuidebookContentRenderer.Layout body, int scroll, boolean thumbHot) {
        content(c, f, s, header, body, scroll, thumbHot, false);
    }

    /** @param plate a chapter plate occupies the first {@link #PLATE_BLOCK} content pixels (drawn by the caller
     *              before this), so the header and body start lower / 扉画占据内容最上方 PLATE_BLOCK 像素，页眉与正文下移 */
    public static void content(DrawContext c, TextRenderer f, Sheet s, PageHeader header,
                               GuidebookContentRenderer.Layout body, int scroll, boolean thumbHot, boolean plate) {
        Region view = s.viewport();
        int originY = view.y() - scroll;
        c.enableScissor(view.x(), view.y(), view.right(), view.bottom());
        pageHeader(c, f, header, s.textLeft(), s.textRight(), originY + (plate ? PLATE_BLOCK : 0));
        body(c, f, body, s.textLeft(), originY + headerHeight(plate), view.y(), view.bottom());
        c.disableScissor();
        int total = contentHeight(body, plate);
        if (total <= view.height()) {
            return;
        }
        if (scroll > 0) {
            ExpressPaint.fadeTop(c, view.x(), view.y(), view.right(), PAPER & 0xFFFFFF);
        }
        if (scroll < total - view.height()) {
            ExpressPaint.fadeBottom(c, view.x(), view.bottom(), view.right(), PAPER & 0xFFFFFF);
        }
        int track = view.height() - 4;
        int thumb = ExpressPaint.thumbSize(track, view.height(), total);
        int thumbY = ExpressPaint.thumbOffset(view.y() + 2, track, thumb, scroll, view.height(), total);
        c.getMatrices().push();
        c.getMatrices().translate(0, 0, 2);
        ExpressPaint.scrollbarPaper(c, s.gutterX(), view.y() + 2, track, thumbY, thumb, thumbHot);
        c.getMatrices().pop();
    }

    /** Page header at content origin y {@code originY}: 2x gem, title (2x or 1x), chip, double rule.
     * 在内容原点 originY 处绘制页眉：2 倍宝石、标题（2 倍或 1 倍）、徽记与双线。 */
    public static void pageHeader(DrawContext c, TextRenderer f, PageHeader header, int tx0, int tx1, int originY) {
        ExpressPaint.gem2x(c, tx0, originY + 11, header.gemRgb());
        if (header.doubleTitle()) {
            c.getMatrices().push();
            c.getMatrices().translate(tx0 + TITLE_X, originY + 8, 0);
            c.getMatrices().scale(2, 2, 1); // exact 2x only / 仅允许精确 2 倍
            c.drawText(f, header.title(), 0, 0, INK, false);
            c.getMatrices().pop();
        } else {
            c.drawText(f, header.title(), tx0 + TITLE_X, originY + 12, INK, false);
        }
        if (header.marked()) {
            ExpressPaint.chip(c, f, header.chipLabel(), tx1, originY + 11);
        }
        c.fill(tx0, originY + 29, tx1, originY + 30, INK_RULE);
        c.fill(tx0, originY + 31, tx1, originY + 32, INK_RULE_SOFT);
    }

    /**
     * Laid body lines and ornaments with body origin (tx0, bodyTop); rows entirely outside [clipTop, clipBottom)
     * are skipped (the caller's scissor still clips partial rows). Text is INK without shadow; runs carry their tones.
     * 以 (tx0, bodyTop) 为正文原点绘制排版行与装饰；完全位于 [clipTop, clipBottom) 之外的行被跳过（部分可见的行由调用方裁剪）。
     */
    public static void body(DrawContext c, TextRenderer f, GuidebookContentRenderer.Layout body, int tx0, int bodyTop,
                            int clipTop, int clipBottom) {
        for (Ornament ornament : body.ornaments()) {
            int top = bodyTop + ornament.y() - 2;
            if (top + ornament.height() + 4 <= clipTop || top >= clipBottom) {
                continue;
            }
            ornament(c, ornament, tx0, bodyTop);
        }
        for (RenderedLine line : body.lines()) {
            int y = bodyTop + line.y();
            if (y + f.fontHeight <= clipTop || y >= clipBottom) {
                continue;
            }
            c.drawText(f, line.text(), tx0 + line.indent(), y, INK, false);
        }
    }

    private static void ornament(DrawContext c, Ornament o, int tx0, int bodyTop) {
        int x = tx0 + o.x();
        int y = bodyTop + o.y();
        int cx = x + o.width() / 2;
        switch (o.kind()) {
            case SECTION_MARK -> ExpressPaint.diamond(c, x, y, o.color());
            case SECTION_RULE -> c.fill(x, y, x + o.width(), y + o.height(), o.color());
            case BULLET -> ExpressPaint.bullet(c, x, y, o.color());
            case QUOTE_RULE -> {
                c.fill(cx - 14, y, cx - 3, y + 1, o.color());
                ExpressPaint.diamond(c, cx - 2, y - 2, INK_FAINT);
                c.fill(cx + 4, y, cx + 15, y + 1, o.color());
            }
            case CALLOUT -> {
                c.fill(x, y, x + o.width(), y + o.height(), o.color());
                c.fill(x, y, x + 2, y + o.height(), BRASS);
            }
            case DIVIDER -> {
                c.fill(cx - 48, y + 2, cx - 6, y + 3, o.color());
                c.fill(cx + 7, y + 2, cx + 49, y + 3, o.color());
                ExpressPaint.diamondOutline(c, cx - 2, y, BRASS);
                c.fill(cx - 10, y + 2, cx - 8, y + 3, BRASS);
                c.fill(cx + 9, y + 2, cx + 11, y + 3, BRASS);
            }
            case TAILPIECE -> {
                ExpressPaint.diamond(c, cx - 2, y, o.color());
                c.fill(cx - 8, y + 2, cx - 6, y + 3, o.color());
                c.fill(cx + 7, y + 2, cx + 9, y + 3, o.color());
            }
        }
    }

    /** Height of the frontispiece block, book top to diamond bottom. 扉页内容块高度（书本顶边到菱形底边）。 */
    public static final int FRONTISPIECE_HEIGHT = 109;

    /**
     * Frontispiece for the standalone screen with nothing selected (spec §5.7, m6): 2x book, title (2x, or 1x when
     * it does not fit), double rule, select hint, key hint and a brass diamond. The block starts at 28 % of the sheet,
     * raised when needed so it ends 4 px inside the paper; on sheets too short even for that (forced GUI scale,
     * height below ~160) the diamond and then the key hint are dropped, and the rest is clipped to the paper.
     * 独立界面未选中条目时的扉页：2 倍书本、标题（放不下时 1 倍）、双线、选择提示、按键提示与黄铜菱形。内容块从纸张 28%
     * 处开始，必要时上移，使其止于纸张内 4 像素；纸张矮到仍放不下时（强制 GUI 缩放、高度低于约 160）依次省略菱形与按键提示，
     * 其余内容裁剪在纸张内。
     */
    public static void frontispiece(DrawContext c, TextRenderer f, Sheet s, OrderedText title, OrderedText selectEntry,
                                    OrderedText keysHint) {
        frontispiece(c, f, s, title, selectEntry, keysHint, false);
    }

    /** @param plate the player's chapter plate sits at the top of the sheet, so the block starts below it
     *              / 纸页顶部有玩家的扉画时，内容块从其下方开始 */
    public static void frontispiece(DrawContext c, TextRenderer f, Sheet s, OrderedText title, OrderedText selectEntry,
                                    OrderedText keysHint, boolean plate) {
        int cx = (s.x() + s.right()) / 2;
        int top = s.y() + Math.max(plate ? PLATE_BLOCK + 2 : 4,
                Math.min(Math.round(s.height() * 0.28f), s.height() - FRONTISPIECE_HEIGHT - 4));
        int limit = s.bottom() - 2;
        Region view = s.viewport();
        c.enableScissor(view.x(), view.y(), view.right(), view.bottom());
        ExpressPaint.bookIcon2x(c, cx - 16, top);
        int titleWidth = f.getWidth(title);
        if (2 * titleWidth <= s.width() - 32) {
            c.getMatrices().push();
            c.getMatrices().translate(cx - titleWidth, top + 40, 0);
            c.getMatrices().scale(2, 2, 1); // exact 2x only / 仅允许精确 2 倍
            c.drawText(f, title, 0, 0, INK, false);
            c.getMatrices().pop();
        } else {
            c.drawText(f, title, cx - titleWidth / 2, top + 44, INK, false);
        }
        c.fill(cx - 60, top + 62, cx + 60, top + 63, INK_RULE);
        c.fill(cx - 60, top + 64, cx + 60, top + 65, INK_RULE_SOFT);
        c.drawText(f, selectEntry, cx - f.getWidth(selectEntry) / 2, top + 72, INK, false);
        if (top + 96 <= limit) {
            c.drawText(f, keysHint, cx - f.getWidth(keysHint) / 2, top + 88, INK_MUTED, false);
        }
        if (top + FRONTISPIECE_HEIGHT <= limit) {
            ExpressPaint.diamondOutline(c, cx - 2, top + 104, BRASS);
        }
        c.disableScissor();
    }
}
