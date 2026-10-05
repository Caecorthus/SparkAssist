package dev.caecorthus.sparkassist.guidebook.decor;

/**
 * Where decoration generators paint: one ARGB pixel at a time, composited source-over onto what is already there.
 * Pixels outside the sink are ignored, so generators never need their own bounds checks. The guide renders a sink
 * as one textured quad; tests read it back as an int array.
 * 点缀生成器的落笔面：逐像素以 ARGB 源在上合成到已有像素上。越界像素直接忽略，生成器不必自行判界。
 * 游戏里一块落笔面就是一个贴图四边形；测试里则读回整数数组。
 */
public interface PixelSink {
    int width();

    int height();

    /** Composite {@code argb} over the pixel at (x, y); fully transparent colours and outside pixels are no-ops.
     * 把 argb 合成到 (x, y) 上；完全透明的颜色与越界像素不做任何事。 */
    void set(int x, int y, int argb);

    /** Composite a rectangle; x2/y2 are exclusive like {@code DrawContext.fill}. 合成一个矩形，x2/y2 为开区间。 */
    default void fill(int x1, int y1, int x2, int y2, int argb) {
        for (int y = y1; y < y2; y++) {
            for (int x = x1; x < x2; x++) {
                set(x, y, argb);
            }
        }
    }
}
