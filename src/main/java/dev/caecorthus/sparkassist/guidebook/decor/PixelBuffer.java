package dev.caecorthus.sparkassist.guidebook.decor;

import java.util.Arrays;

/**
 * An ARGB pixel buffer with straight-alpha source-over compositing: the single implementation of {@link PixelSink},
 * uploaded to a dynamic texture in game and inspected directly in tests. All channels are rounded to nearest, so
 * the same generator produces the same bytes on every machine.
 * 直通 alpha 源在上合成的 ARGB 像素缓冲：PixelSink 的唯一实现，游戏里上传为动态纹理，测试里直接读取。
 * 各通道四舍五入，同一生成器在任何机器上得到完全相同的字节。
 */
public final class PixelBuffer implements PixelSink {
    private final int width;
    private final int height;
    private final int[] argb;

    public PixelBuffer(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Pixel buffer needs a positive size, got " + width + "x" + height);
        }
        this.width = width;
        this.height = height;
        this.argb = new int[width * height];
    }

    @Override
    public int width() {
        return width;
    }

    @Override
    public int height() {
        return height;
    }

    /** The stored ARGB value, 0 (transparent) outside the buffer. 存储的 ARGB 值，越界为 0。 */
    public int get(int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            return 0;
        }
        return argb[y * width + x];
    }

    @Override
    public void set(int x, int y, int color) {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            return;
        }
        int alpha = color >>> 24;
        if (alpha == 0) {
            return;
        }
        int index = y * width + x;
        if (alpha == 0xFF) {
            argb[index] = color;
            return;
        }
        argb[index] = over(color, argb[index]);
    }

    public void clear() {
        Arrays.fill(argb, 0);
    }

    /** Number of pixels in a rectangle whose alpha is non-zero. 矩形内不透明像素的数量。 */
    public int paintedCount(int x1, int y1, int x2, int y2) {
        int count = 0;
        for (int y = Math.max(0, y1); y < Math.min(height, y2); y++) {
            for (int x = Math.max(0, x1); x < Math.min(width, x2); x++) {
                if ((argb[y * width + x] >>> 24) != 0) {
                    count++;
                }
            }
        }
        return count;
    }

    /** The pixels as ABGR, the byte order {@code NativeImage.setColor} expects in 1.21.1. 以 1.21.1 原生图像的 ABGR 顺序导出。 */
    public int[] toAbgr() {
        int[] out = new int[argb.length];
        for (int i = 0; i < argb.length; i++) {
            out[i] = argbToAbgr(argb[i]);
        }
        return out;
    }

    public static int argbToAbgr(int argb) {
        return (argb & 0xFF00FF00) | ((argb >>> 16) & 0xFF) | ((argb & 0xFF) << 16);
    }

    /** Straight-alpha source-over of {@code src} on {@code dst}, rounded to nearest per channel. 源在上合成。 */
    static int over(int src, int dst) {
        int sa = src >>> 24;
        int da = dst >>> 24;
        if (da == 0) {
            return src;
        }
        // Alpha in 1/255 units: outA = sa + da * (255 - sa) / 255.
        int outA255 = sa * 255 + da * (255 - sa);
        int outA = (outA255 + 127) / 255;
        if (outA == 0) {
            return 0;
        }
        int r = channel(src >>> 16, dst >>> 16, sa, da, outA255);
        int g = channel(src >>> 8, dst >>> 8, sa, da, outA255);
        int b = channel(src, dst, sa, da, outA255);
        return (outA << 24) | (r << 16) | (g << 8) | b;
    }

    private static int channel(int src, int dst, int sa, int da, int outA255) {
        src &= 0xFF;
        dst &= 0xFF;
        // outC = (sc * sa + dc * da * (1 - sa)) / outA, everything scaled by 255.
        long numerator = (long) src * sa * 255 + (long) dst * da * (255 - sa);
        return (int) ((numerator + outA255 / 2) / outA255);
    }
}
