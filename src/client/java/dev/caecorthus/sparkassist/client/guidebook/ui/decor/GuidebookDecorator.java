package dev.caecorthus.sparkassist.client.guidebook.ui.decor;

import dev.caecorthus.sparkassist.guidebook.GuidebookLayout.Region;
import dev.caecorthus.sparkassist.guidebook.decor.DecorPalette;
import dev.caecorthus.sparkassist.guidebook.decor.DecorRandom;
import dev.caecorthus.sparkassist.guidebook.decor.DecorSet;
import dev.caecorthus.sparkassist.guidebook.decor.Ornaments;
import dev.caecorthus.sparkassist.client.guidebook.ui.ReaderPainter;
import dev.caecorthus.sparkassist.guidebook.decor.PixelBuffer;
import dev.caecorthus.sparkassist.guidebook.decor.Plates;
import dev.caecorthus.sparkassist.guidebook.decor.Sigils;
import net.minecraft.client.gui.DrawContext;

/**
 * Owns the guide's cached decoration layers and draws them between the painters' batches: a layer is drawn after
 * the surface under it (panel body, paper) and before the content over it (rows, text), all at the same z, so
 * plain draw order settles what covers what. Layers regenerate only when their key changes.
 * 持有指南的缓存点缀层，并在绘制批次之间画出：每层画在其下的面（面板底、纸）之后、其上的内容（行、文字）之前，
 * 全部同一 z，纯靠绘制顺序决定遮盖。只有 key 变化时才重新生成。
 */
public final class GuidebookDecorator implements AutoCloseable {
    /** Rows area must be at least this tall for the directory sigil. 目录行区至少这么高才画暗纹。 */
    private static final int SIGIL_MIN_HEIGHT = 100;
    private static final int SIGIL_RADIUS = 40;

    /** Largest paper watermark radius; smaller sheets shrink it, below 24 it is dropped. 纸面暗纹的最大半径。 */
    private static final int WATERMARK_RADIUS = 54;
    private static final int WATERMARK_MIN_RADIUS = 24;
    /** The watermark's bottom edge keeps this far from the sheet bottom. 暗纹底边与纸底的距离。 */
    private static final int WATERMARK_BOTTOM = 18;

    private final DecorLayer directory = new DecorLayer("sparkassist_guide_directory");
    private final DecorLayer plate = new DecorLayer("sparkassist_guide_plate");
    private final DecorLayer frontPlate = new DecorLayer("sparkassist_guide_front_plate");
    private final DecorLayer watermark = new DecorLayer("sparkassist_guide_watermark");

    private record DirectoryKey(int width, int height, DecorSet set, long seed, int density) {
    }

    private record PlateKey(DecorSet.Plate kind, int width, int colour, long seed) {
    }

    private record SigilKey(DecorSet.Sigil kind, int radius, long seed) {
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
        PlateKey key = new PlateKey(set.plate(), width, set.colour(), seed);
        if (!layer.isCurrent(key)) {
            PixelBuffer buffer = new PixelBuffer(width + 2 * Plates.MARGIN, Plates.HEIGHT + 2 * Plates.MARGIN);
            Plates.plate(buffer, set.plate(), Plates.MARGIN, Plates.MARGIN, width, set.colour(), seed);
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
     * Wallpaper and the player's faction sigil behind the directory rows: the area from the row viewport's top
     * to the footer rule, inside the panel's 3 px frame. Call between the chrome and the rows.
     * 目录行区身后的墙纸与玩家阵营暗纹：从行视口顶边到页脚刻线、面板 3 像素边框之内。在边框与行之间调用。
     */
    public void drawDirectoryBackground(DrawContext context, Region nav, int viewportTop, DecorSet set, long seed) {
        int density = DecorSettings.density();
        if (density < 0) {
            return;
        }
        int x = nav.x() + 3;
        int width = nav.width() - 6;
        int height = nav.bottom() - 21 - viewportTop;
        if (width < 8 || height < 8) {
            return;
        }
        DirectoryKey key = new DirectoryKey(width, height, set, seed, density);
        if (!directory.isCurrent(key)) {
            PixelBuffer buffer = new PixelBuffer(width, height);
            Ornaments.wallpaper(buffer, 0, 0, width, height);
            if (density >= 1 && height >= SIGIL_MIN_HEIGHT) {
                Sigils.draw(buffer, set.sigil(), width / 2, height - 51, SIGIL_RADIUS, DecorPalette.WATERMARK_BRASS,
                        new DecorRandom(DecorRandom.ownerSeed("directory", seed)));
            }
            directory.update(key, buffer);
        }
        directory.draw(context, x, viewportTop);
    }

    /** Release every texture; layers come back lazily on the next draw. 释放全部纹理，下次绘制时惰性重建。 */
    @Override
    public void close() {
        directory.close();
        plate.close();
        frontPlate.close();
        watermark.close();
    }
}
