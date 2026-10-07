package dev.caecorthus.sparkassist.guidebook.decor;

/**
 * A sink that only lets pixels inside one rectangle through, for generators whose strokes may wander past their
 * box (a scene painted into a plate, a vine kept to a rim strip).
 * 只放行一个矩形内像素的落笔面，供可能越过自身边界的生成器使用（画进扉画框的景、限在边条里的枝条）。
 */
public final class ClipSink implements PixelSink {
    private final PixelSink inner;
    private final int x1;
    private final int y1;
    private final int x2;
    private final int y2;

    /** Clip to (x, y)..(x + w, y + h), exclusive on the far edges. 裁剪到左闭右开的矩形。 */
    public ClipSink(PixelSink inner, int x, int y, int w, int h) {
        this.inner = inner;
        this.x1 = x;
        this.y1 = y;
        this.x2 = x + w;
        this.y2 = y + h;
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
        if (x >= x1 && y >= y1 && x < x2 && y < y2) {
            inner.set(x, y, argb);
        }
    }

    @Override
    public void fill(int ax1, int ay1, int ax2, int ay2, int argb) {
        int cx1 = Math.max(ax1, x1);
        int cy1 = Math.max(ay1, y1);
        int cx2 = Math.min(ax2, x2);
        int cy2 = Math.min(ay2, y2);
        if (cx2 > cx1 && cy2 > cy1) {
            inner.fill(cx1, cy1, cx2, cy2, argb);
        }
    }
}
