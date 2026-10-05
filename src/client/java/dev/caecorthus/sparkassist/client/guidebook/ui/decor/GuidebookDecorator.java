package dev.caecorthus.sparkassist.client.guidebook.ui.decor;

import dev.caecorthus.sparkassist.guidebook.GuidebookLayout.Region;
import dev.caecorthus.sparkassist.guidebook.decor.DecorPalette;
import dev.caecorthus.sparkassist.guidebook.decor.DecorRandom;
import dev.caecorthus.sparkassist.guidebook.decor.DecorSet;
import dev.caecorthus.sparkassist.guidebook.decor.Ornaments;
import dev.caecorthus.sparkassist.guidebook.decor.PixelBuffer;
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

    private final DecorLayer directory = new DecorLayer("sparkassist_guide_directory");

    private record DirectoryKey(int width, int height, DecorSet set, long seed, int density) {
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
    }
}
