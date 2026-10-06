package dev.caecorthus.sparkassist.client.guidebook.ui.decor;

import dev.caecorthus.sparkassist.guidebook.decor.PixelSink;
import net.minecraft.client.gui.DrawContext;

/**
 * Lets the pure ornament generators paint straight into a {@link DrawContext}: every {@code fill} becomes one GUI
 * quad, so it suits the small fixed pieces (corner plates, nameplates, clasps, tags) that are drawn inside the
 * painters' batches. Heavy pixel work goes through {@link DecorLayer} instead.
 * 让纯 Java 的点缀生成器直接画进 DrawContext：每个 fill 就是一个 GUI 四边形，适合画在绘制批次里的小件
 * （角板、铭牌、书口扣、行李牌）。大量像素的内容走 DecorLayer。
 */
public final class DrawContextSink implements PixelSink {
    private final DrawContext context;

    public DrawContextSink(DrawContext context) {
        this.context = context;
    }

    @Override
    public int width() {
        return Integer.MAX_VALUE;
    }

    @Override
    public int height() {
        return Integer.MAX_VALUE;
    }

    @Override
    public void set(int x, int y, int argb) {
        if ((argb >>> 24) != 0) {
            context.fill(x, y, x + 1, y + 1, argb);
        }
    }

    @Override
    public void fill(int x1, int y1, int x2, int y2, int argb) {
        if ((argb >>> 24) != 0 && x2 > x1 && y2 > y1) {
            context.fill(x1, y1, x2, y2, argb);
        }
    }
}
