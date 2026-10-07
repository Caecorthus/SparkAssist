package dev.caecorthus.sparkassist.guidebook.decor;

/**
 * Shifts every pixel by a fixed offset before it reaches the inner sink, so generators can paint in screen
 * coordinates into a layer whose buffer starts elsewhere.
 * 把每个像素平移固定偏移后再交给内层落笔面，生成器因此可以用屏幕坐标画进起点不同的缓存层。
 */
public final class OffsetSink implements PixelSink {
    private final PixelSink inner;
    private final int dx;
    private final int dy;

    /** Screen (x, y) lands on inner (x + dx, y + dy). 屏幕 (x, y) 落到内层 (x + dx, y + dy)。 */
    public OffsetSink(PixelSink inner, int dx, int dy) {
        this.inner = inner;
        this.dx = dx;
        this.dy = dy;
    }

    @Override
    public int width() {
        return inner.width();
    }

    @Override
    public int height() {
        return inner.height();
    }

    @Override
    public void set(int x, int y, int argb) {
        inner.set(x + dx, y + dy, argb);
    }

    @Override
    public void fill(int x1, int y1, int x2, int y2, int argb) {
        inner.fill(x1 + dx, y1 + dy, x2 + dx, y2 + dy, argb);
    }
}
