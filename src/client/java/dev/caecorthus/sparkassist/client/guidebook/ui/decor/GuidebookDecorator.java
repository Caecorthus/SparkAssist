package dev.caecorthus.sparkassist.client.guidebook.ui.decor;

import dev.caecorthus.sparkassist.guidebook.GuidebookLayout.Region;
import dev.caecorthus.sparkassist.client.guidebook.ui.ReaderPainter;
import dev.caecorthus.sparkassist.guidebook.decor.DecorPalette;
import dev.caecorthus.sparkassist.guidebook.decor.DecorRandom;
import dev.caecorthus.sparkassist.guidebook.decor.DecorSet;
import dev.caecorthus.sparkassist.guidebook.decor.DecorZones;
import dev.caecorthus.sparkassist.guidebook.decor.Emblems;
import dev.caecorthus.sparkassist.guidebook.decor.Foliage;
import dev.caecorthus.sparkassist.guidebook.decor.OffsetSink;
import dev.caecorthus.sparkassist.guidebook.decor.Ornaments;
import dev.caecorthus.sparkassist.guidebook.decor.PixelBuffer;
import dev.caecorthus.sparkassist.guidebook.decor.PixelSink;
import dev.caecorthus.sparkassist.guidebook.decor.PlateArt;
import dev.caecorthus.sparkassist.guidebook.decor.Plates;
import dev.caecorthus.sparkassist.guidebook.decor.Sigils;
import dev.caecorthus.sparkassist.guidebook.decor.Steam;
import dev.caecorthus.sparkassist.guidebook.decor.ZonedSink;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.DrawContext;

/**
 * Owns the guide's cached decoration layers and draws them between the painters' batches: a layer is drawn after
 * the surface under it (panel body, paper) and before the content over it (rows, text), all at the same z, so
 * plain draw order settles what covers what. Layers regenerate only when their key changes.
 * 持有指南的缓存点缀层，并在绘制批次之间画出：每层画在其下的面（面板底、纸）之后、其上的内容（行、文字）之前，
 * 全部同一 z，纯靠绘制顺序决定遮盖。只有 key 变化时才重新生成。
 */
public final class GuidebookDecorator implements AutoCloseable {
    private static final int SIGIL_RADIUS = 40;
    /** Clear space kept between the directory sigil and the rows above / the footer below. 目录暗纹与上方各行、下方页脚之间的留白。 */
    private static final int SIGIL_GAP = 6;

    /** Largest paper watermark radius; smaller sheets shrink it, below 24 it is dropped. 纸面暗纹的最大半径。 */
    private static final int WATERMARK_RADIUS = 54;
    private static final int WATERMARK_MIN_RADIUS = 24;
    /** The watermark's bottom edge keeps this far from the sheet bottom. 暗纹底边与纸底的距离。 */
    private static final int WATERMARK_BOTTOM = 18;

    /** Overlay layers keep this much room around their panel for curls and vines. 覆盖层在面板四周留的余量。 */
    public static final int OVERLAY_MARGIN = 20;
    private static final int DEBUG_ZONE = 0x66E03030;

    private final DecorLayer directory = new DecorLayer("sparkassist_guide_directory");
    private final DecorLayer plate = new DecorLayer("sparkassist_guide_plate");
    private final DecorLayer frontPlate = new DecorLayer("sparkassist_guide_front_plate");
    private final DecorLayer watermark = new DecorLayer("sparkassist_guide_watermark");
    private final DecorLayer endSeal = new DecorLayer("sparkassist_guide_end_seal");
    private final DecorLayer readerOverlay = new DecorLayer("sparkassist_guide_reader_overlay");
    private final DecorLayer directoryOverlay = new DecorLayer("sparkassist_guide_directory_overlay");

    /**
     * Everything the reader overlay depends on; it doubles as the layer's cache key.
     * 正文覆盖层的全部输入，同时作为缓存键。
     *
     * @param ownPage       the page belongs to the player's own faction: gold ribbon / 自己阵营的页面，金线丝带
     * @param zones         no-go rectangles: the reader's own text boxes plus everything outside it / 禁区
     * @param bandTextWidth width of the crumb or running head drawn at x + 7 / 路径或页眉宽度
     * @param sourceWidth   width of the source label, 0 for none / 来源宽度
     * @param leftEdge      right edge of whatever sits left of the reader (directory, or screen margin) / 左侧邻居的右缘
     */
    public record ReaderOverlaySpec(Region reader, ReaderPainter.Sheet sheet, DecorSet set, boolean ownPage, long seed,
                                    List<Region> zones, int bandTextWidth, int sourceWidth, int leftEdge, int density,
                                    boolean foliage) {
    }

    /** Everything the directory overlay depends on; also its cache key. 目录覆盖层的全部输入，同时作为缓存键。 */
    public record DirectoryOverlaySpec(Region nav, DecorSet set, long seed, List<Region> zones, int titleWidth,
                                       int searchToggleX, int density, boolean foliage) {
    }

    private record PlateKey(DecorSet.Plate kind, String page, boolean art, int width, int theme, long seed) {
    }

    private record SigilKey(DecorSet.Sigil kind, int radius, long seed) {
    }

    private record SealKey(int width, int colour, DecorSet.Mark mark) {
    }

    /** Whether a page gets a chapter plate: decorations on and a text column at least 160 px wide.
     * 页面是否有扉画：点缀开启且版心不窄于 160 像素。 */
    public boolean plateShown(int measure) {
        return DecorSettings.enabled() && measure >= Plates.MIN_WIDTH;
    }

    /** The frontispiece only takes a plate when the whole block still fits under it. 扉页只在整块放得下时才有扉画。 */
    public boolean frontPlateShown(ReaderPainter.Sheet sheet) {
        return plateShown(sheet.measure())
                && sheet.height() >= ReaderPainter.PLATE_BLOCK + ReaderPainter.FRONTISPIECE_HEIGHT + 8;
    }

    /** The page's chapter plate at content y 3, scrolling with the page and clipped to the viewport. Call after
     * the paper and before the content. 页面扉画，位于内容 y 3，随页滚动并裁剪到视口；在纸张之后、内容之前调用。 */
    public void drawPagePlate(DrawContext context, ReaderPainter.Sheet sheet, DecorSet set, long seed, int scroll) {
        if (!plateShown(sheet.measure())) {
            return;
        }
        drawPlate(plate, context, sheet, set, seed, sheet.viewport().y() + ReaderPainter.PLATE_TOP - scroll, true);
    }

    /** The player's own plate above the frontispiece. 扉页上方的玩家扉画。 */
    public void drawFrontispiecePlate(DrawContext context, ReaderPainter.Sheet sheet, DecorSet set, long seed) {
        if (!frontPlateShown(sheet)) {
            return;
        }
        drawPlate(frontPlate, context, sheet, set, seed, sheet.viewport().y() + ReaderPainter.PLATE_TOP, false);
    }

    private static void drawPlate(DecorLayer layer, DrawContext context, ReaderPainter.Sheet sheet, DecorSet set,
                                  long seed, int top, boolean clipToViewport) {
        int width = sheet.measure();
        Optional<PlateArt> art = PlateArtLoader.forEntry(set.page());
        PlateKey key = new PlateKey(set.plate(), set.page(), art.isPresent(), width, set.theme(), seed);
        if (!layer.isCurrent(key)) {
            PixelBuffer buffer = new PixelBuffer(width + 2 * Plates.MARGIN, Plates.HEIGHT + 2 * Plates.MARGIN);
            String[] emblem = Emblems.forEntry(set.page()).orElse(null);
            if (art.isPresent()) {
                Plates.art(buffer, art.get(), emblem, Plates.MARGIN, Plates.MARGIN, width, set.theme());
            } else {
                Plates.plate(buffer, set.plate(), emblem, Plates.MARGIN, Plates.MARGIN, width, set.theme(), seed);
            }
            layer.update(key, buffer);
        }
        Region view = sheet.viewport();
        if (clipToViewport) {
            context.enableScissor(view.x(), view.y(), view.right(), view.bottom());
        }
        layer.draw(context, sheet.textLeft() - Plates.MARGIN, top - Plates.MARGIN);
        if (clipToViewport) {
            context.disableScissor();
        }
    }

    /**
     * The closing wax seal: centred under the last paragraph between two hairlines, scrolling with the page and
     * clipped to the viewport, so it only shows once the reader reaches the end. Call after the paper and before
     * the content. {@code bodyEnd} is the content y where the body ends ({@link ReaderPainter#bodyEnd}).
     * 落款火漆：在最后一段下方居中、夹在两道细线之间，随页滚动并裁剪到视口，只有读到末尾才看得见。在纸张之后、内容之前
     * 调用；bodyEnd 为正文结束处的内容坐标。
     */
    public void drawEndSeal(DrawContext context, ReaderPainter.Sheet sheet, DecorSet set, int bodyEnd, int scroll) {
        if (!plateShown(sheet.measure())) {
            return;
        }
        int width = sheet.measure();
        SealKey key = new SealKey(width, set.colour(), set.mark());
        if (!endSeal.isCurrent(key)) {
            PixelBuffer buffer = new PixelBuffer(width, ReaderPainter.SEAL_BLOCK);
            int cx = width / 2;
            int cy = 13;
            int reach = Math.min(64, width / 2 - 4);
            buffer.fill(cx - reach, cy, cx - 11, cy + 1, DecorPalette.INK_RULE_SOFT);
            buffer.fill(cx + 12, cy, cx + reach + 1, cy + 1, DecorPalette.INK_RULE_SOFT);
            for (int end : new int[] {cx - reach - 2, cx + reach + 2}) {
                buffer.set(end, cy - 1, DecorPalette.INK_RULE);
                buffer.set(end - 1, cy, DecorPalette.INK_RULE);
                buffer.set(end + 1, cy, DecorPalette.INK_RULE);
                buffer.set(end, cy + 1, DecorPalette.INK_RULE);
            }
            Ornaments.waxSeal(buffer, cx, cy, DecorPalette.opaque(set.colour()), set.mark());
            endSeal.update(key, buffer);
        }
        Region view = sheet.viewport();
        int top = view.y() - scroll + bodyEnd;
        if (top >= view.bottom() || top + ReaderPainter.SEAL_BLOCK <= view.y()) {
            return;
        }
        context.enableScissor(view.x(), view.y(), view.right(), view.bottom());
        endSeal.draw(context, sheet.textLeft(), top);
        context.disableScissor();
    }

    /** Faint sigil on the sheet's lower half, under the body text; shrinks with the sheet and disappears below
     * radius 24. 纸页下半幅文字之下的淡暗纹；随纸页缩小，半径不足 24 时不画。 */
    public void drawPaperWatermark(DrawContext context, ReaderPainter.Sheet sheet, DecorSet set, long seed) {
        if (DecorSettings.density() < 1) {
            return;
        }
        int radius = Math.min(WATERMARK_RADIUS, Math.min(sheet.measure() / 2 - 4, (sheet.height() - 20) / 2));
        if (radius < WATERMARK_MIN_RADIUS) {
            return;
        }
        SigilKey key = new SigilKey(set.sigil(), radius, seed);
        if (!watermark.isCurrent(key)) {
            int size = 2 * radius + 3;
            PixelBuffer buffer = new PixelBuffer(size, size);
            Sigils.draw(buffer, set.sigil(), radius + 1, radius + 1, radius, DecorPalette.WATERMARK_INK,
                    new DecorRandom(seed ^ 77));
            watermark.update(key, buffer);
        }
        int cx = sheet.textLeft() + sheet.measure() / 2;
        int cy = sheet.bottom() - WATERMARK_BOTTOM - radius;
        watermark.draw(context, cx - radius - 1, cy - radius - 1);
    }

    /**
     * The player's faction sigil, faintly engraved in the empty part of the directory under the last row. It is only
     * drawn while the rows end above it, so it never sits behind text. Call between the chrome and the rows.
     * 玩家阵营暗纹，淡淡刻在目录最后一行下方的空白处。只在各行结束于它上方时绘制，绝不垫在文字下。在边框与行之间调用。
     *
     * @param rowsBottom screen y where the (scrolled) rows end / 各行（含滚动）结束处的屏幕 y
     */
    public void drawDirectoryBackground(DrawContext context, Region nav, int viewportTop, int rowsBottom, DecorSet set,
                                        long seed) {
        if (DecorSettings.density() < 1) {
            return;
        }
        int floor = nav.bottom() - 21 - SIGIL_GAP;
        int top = floor - 2 * SIGIL_RADIUS - 1;
        if (top < viewportTop || rowsBottom + SIGIL_GAP > top || nav.width() - 6 < 2 * SIGIL_RADIUS + 3) {
            return;
        }
        SigilKey key = new SigilKey(set.sigil(), SIGIL_RADIUS, DecorRandom.ownerSeed("directory", seed));
        if (!directory.isCurrent(key)) {
            int size = 2 * SIGIL_RADIUS + 3;
            PixelBuffer buffer = new PixelBuffer(size, size);
            Sigils.draw(buffer, set.sigil(), SIGIL_RADIUS + 1, SIGIL_RADIUS + 1, SIGIL_RADIUS,
                    DecorPalette.WATERMARK_BRASS, new DecorRandom(key.seed()));
            directory.update(key, buffer);
        }
        directory.draw(context, nav.x() + nav.width() / 2 - SIGIL_RADIUS - 1, top - 1);
    }

    /**
     * The reader's overlay, drawn last: the ribbon bookmark down the left paper margin, steam curls rising from the
     * outer corners and a little faction foliage on the rims. Curls and vines go through the no-go zones pixel by
     * pixel. (The wax seal is no longer here: it closes the article, see {@link #drawEndSeal}.)
     * 正文的覆盖层，最后绘制：垂在纸页左边距的丝带书签、从外角升起的蒸汽卷云，以及边框上的少量阵营枝叶。卷云与枝条
     * 逐像素受禁区约束。（火漆印不在这里：它落在正文末尾，见 drawEndSeal。）
     */
    public void drawReaderOverlay(DrawContext context, ReaderOverlaySpec spec) {
        Region reader = spec.reader();
        int density = spec.density();
        if (density < 0 || reader.width() <= 0 || reader.height() <= 0) {
            return;
        }
        int ox = reader.x() - OVERLAY_MARGIN;
        int oy = reader.y() - OVERLAY_MARGIN;
        if (!readerOverlay.isCurrent(spec)) {
            PixelBuffer buffer = new PixelBuffer(reader.width() + 2 * OVERLAY_MARGIN,
                    reader.height() + 2 * OVERLAY_MARGIN);
            PixelSink sink = new OffsetSink(buffer, -ox, -oy);
            DecorRandom rand = new DecorRandom(spec.seed() ^ 0xA11L);
            DecorSet set = spec.set();
            int colour = DecorPalette.opaque(set.colour());
            ReaderPainter.Sheet sheet = spec.sheet();
            int ribbonEnd = reader.y() + 21 + Math.round(reader.height() * 0.42f) + rand.nextInt(10);
            Ornaments.ribbon(sink, sheet.x() + 3, reader.y() + 21, Math.min(ribbonEnd, sheet.bottom() - 12),
                    spec.ownPage() ? DecorPalette.POLISHED : DecorPalette.mix(colour, DecorPalette.INK, 0.3));
            if (density >= 1) {
                List<Region> forbidden = new ArrayList<>(spec.zones());
                forbidden.add(DecorZones.panelInterior(reader));
                PixelSink curls = new ZonedSink(sink, List.of(), forbidden);
                Steam.wisps(curls, reader.right() + 1, reader.bottom() - 8, -Math.PI / 2 + 0.2, 3, rand);
                if (density >= 2) {
                    Steam.wisps(curls, reader.x() - 1, reader.bottom() - 8, -Math.PI / 2 - 0.2, 2, rand);
                }
            }
            if (spec.foliage()) {
                readerFoliage(sink, spec, rand);
            }
            readerOverlay.update(spec, buffer);
        }
        readerOverlay.draw(context, ox, oy);
    }

    /** Longest band twig: a sprig, not a garland. 标题带小枝的最大长度：一小枝，而不是一整条花环。 */
    private static final int TWIG_LENGTH = 34;

    /**
     * A little foliage at a few anchors, kept in 10–14 px strips along the rims: a sprig in the band gap and one
     * climbing the bottom-right corner; at full density a vine down the left gap and one along the bottom rim.
     * 少量枝叶，只在几个锚点、贴着边框的 10 到 14 像素边条里：标题带空档一小枝，右下角向上一小枝；满密度时左侧缝隙
     * 与底沿各加一根。
     */
    private static void readerFoliage(PixelSink sink, ReaderOverlaySpec spec, DecorRandom rand) {
        Region r = spec.reader();
        DecorSet set = spec.set();
        int density = spec.density();
        int f1 = r.x() + 7 + spec.bandTextWidth() + 10;
        int f2 = (spec.sourceWidth() > 0 ? r.right() - 22 - spec.sourceWidth() : r.right() - 18) - 6;
        if (f2 - f1 >= 16) {
            int length = Math.min(TWIG_LENGTH, f2 - f1 + 6);
            grow(sink, spec.zones(), new Region(f1 - 3, r.y() + 4, length + 2, 14),
                    Foliage.Vine.twig(set.foliage(), f1 - 2, r.y() + 11, 0, length, set.berry()), rand);
        }
        grow(sink, spec.zones(), new Region(r.right() - 4, r.y() + 4, 10, r.height() - 8),
                Foliage.Vine.alongRim(set.foliage(), r.right() - 1, r.bottom() - 10, -Math.PI / 2,
                        new int[] {26, 38, 60}[density], set.berry()), rand);
        if (density >= 2) {
            int left = spec.leftEdge() + 1;
            grow(sink, spec.zones(), new Region(left, r.y() + 22, Math.max(6, r.x() + 7 - left), r.height() - 30),
                    Foliage.Vine.alongRim(set.foliage(), r.x() + 2, r.y() + 26, Math.PI / 2, 70, set.berry()), rand);
            grow(sink, spec.zones(), new Region(r.x() + 24, r.bottom() - 4, r.width() - 32, 10),
                    Foliage.Vine.alongRim(set.foliage(), r.right() - 10, r.bottom() - 1, Math.PI, 50, set.berry()),
                    rand);
        }
    }

    /** Steam curls at the directory's outer corners and, with foliage on, a twig in the band gap (and at full
     * density a vine up the outer left edge), drawn last. 目录外角的蒸汽卷云；开枝叶时标题带空档一根横枝，满密度时左外沿
     * 再长一根；最后绘制。 */
    public void drawDirectoryOverlay(DrawContext context, DirectoryOverlaySpec spec) {
        Region nav = spec.nav();
        int density = spec.density();
        if (density < 0 || nav.width() <= 0 || nav.height() <= 0) {
            return;
        }
        if (density < 1 && !spec.foliage()) {
            return;
        }
        int ox = nav.x() - OVERLAY_MARGIN;
        int oy = nav.y() - OVERLAY_MARGIN;
        if (!directoryOverlay.isCurrent(spec)) {
            PixelBuffer buffer = new PixelBuffer(nav.width() + 2 * OVERLAY_MARGIN, nav.height() + 2 * OVERLAY_MARGIN);
            PixelSink sink = new OffsetSink(buffer, -ox, -oy);
            DecorRandom rand = new DecorRandom(spec.seed() ^ 0xD1FL);
            if (density >= 1) {
                List<Region> forbidden = new ArrayList<>(spec.zones());
                forbidden.add(DecorZones.panelInterior(nav));
                PixelSink curls = new ZonedSink(sink, List.of(), forbidden);
                Steam.wisps(curls, nav.x() - 1, nav.bottom() - 6, -Math.PI / 2, 3, rand);
                if (density >= 2) {
                    Steam.wisps(curls, nav.right() + 1, nav.y() + 24, Math.PI / 2 + 0.3, 2, rand);
                }
            }
            if (spec.foliage()) {
                DecorSet set = spec.set();
                int f1 = nav.x() + 7 + spec.titleWidth() + 10;
                int tagX = spec.searchToggleX() - 6 - Ornaments.TAG_WIDTH;
                boolean tagShown = tagX - Ornaments.TAG_STRING - 6 >= nav.x() + 7 + spec.titleWidth();
                int f2 = (tagShown ? tagX - Ornaments.TAG_STRING - 4 : spec.searchToggleX() - 6);
                if (f2 - f1 >= 16) {
                    int length = Math.min(TWIG_LENGTH, f2 - f1 + 6);
                    grow(sink, spec.zones(), new Region(f1 - 3, nav.y() + 4, length + 2, 14),
                            Foliage.Vine.twig(set.foliage(), f1 - 2, nav.y() + 11, 0, length, set.berry()), rand);
                }
                if (density >= 2) {
                    grow(sink, spec.zones(), new Region(Math.max(0, nav.x() - 12), nav.y() + 40, Math.min(nav.x(), 12) + 4,
                            nav.height() - 44), Foliage.Vine.alongRim(set.foliage(), nav.x() - 1, nav.bottom() - 10,
                            -Math.PI / 2, 50, set.berry()), rand);
                }
            }
            directoryOverlay.update(spec, buffer);
        }
        directoryOverlay.draw(context, ox, oy);
    }

    private static void grow(PixelSink sink, List<Region> zones, Region strip, Foliage.Vine vine, DecorRandom rand) {
        ZonedSink zoned = new ZonedSink(sink, List.of(strip), zones);
        Foliage.grow(zoned, zoned::allowed, vine, rand);
    }

    /** Developer overlay: every no-go rectangle in translucent red. 开发用叠加：所有禁区矩形涂半透明红。 */
    public static void drawDebugZones(DrawContext context, List<Region> zones) {
        for (Region zone : zones) {
            context.fill(zone.x(), zone.y(), zone.right(), zone.bottom(), DEBUG_ZONE);
        }
    }

    /** Release every texture; layers come back lazily on the next draw. 释放全部纹理，下次绘制时惰性重建。 */
    @Override
    public void close() {
        directory.close();
        plate.close();
        frontPlate.close();
        watermark.close();
        endSeal.close();
        PlateArtLoader.clear();
        readerOverlay.close();
        directoryOverlay.close();
    }
}
