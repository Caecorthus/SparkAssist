package dev.caecorthus.sparkassist.guidebook.decor;

/**
 * A sink that flips everything horizontally inside one column band, so a plate scene can be painted mirror-wise.
 * Things that must keep their handedness (the page emblem) are drawn straight into {@link #inner()} at
 * {@link #flip}ped coordinates.
 * 在一段列范围内把所有像素左右翻转的落笔面，用于镜像绘制扉画景。需要保持朝向的东西（页面徽记）直接画进 inner()，
 * 坐标用 flip() 换算。
 */
final class MirrorSink implements PixelSink {
    private final PixelSink inner;
    private final int axis;

    /** Mirror the columns x .. x + w - 1 onto each other. 把 x 到 x + w - 1 的各列对调。 */
    MirrorSink(PixelSink inner, int x, int w) {
        this.inner = inner;
        this.axis = 2 * x + w - 1;
    }

    PixelSink inner() {
        return inner;
    }

    /** Where column {@code x} lands. 第 x 列被翻到的位置。 */
    int flip(int x) {
        return axis - x;
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
        inner.set(axis - x, y, argb);
    }

    @Override
    public void fill(int x1, int y1, int x2, int y2, int argb) {
        inner.fill(axis - x2 + 1, y1, axis - x1 + 1, y2, argb);
    }
}
