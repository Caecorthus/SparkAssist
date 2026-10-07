package dev.caecorthus.sparkassist.guidebook.decor;

/**
 * Integer drawing helpers for the decoration generators: 4x4 Bayer ordered dither, Bresenham lines, discs, rings
 * and '#' bitmaps. Everything lands on whole pixels; nothing here knows about Minecraft.
 * 点缀生成器的整数绘图工具：4×4 Bayer 有序抖动、Bresenham 直线、圆盘、圆环与 '#' 位图。全部落在整像素上，不依赖 Minecraft。
 */
public final class Dither {
    private static final int[] BAYER = {0, 8, 2, 10, 12, 4, 14, 6, 3, 11, 1, 9, 15, 7, 13, 5};

    private Dither() {
    }

    /** Threshold 0..15 of the Bayer cell at (x, y). (x, y) 处的 Bayer 阈值，0 到 15。 */
    public static int bayer(int x, int y) {
        return BAYER[(x & 3) | ((y & 3) << 2)];
    }

    /** Paint {@code argb} on the pixels of a rectangle whose Bayer threshold is below {@code level} (0 = none,
     * 16 = all). 在矩形内 Bayer 阈值低于 level 的像素上落笔（0 为无，16 为全部）。 */
    public static void rect(PixelSink sink, int x1, int y1, int x2, int y2, int argb, int level) {
        for (int y = y1; y < y2; y++) {
            for (int x = x1; x < x2; x++) {
                if (bayer(x, y) < level) {
                    sink.set(x, y, argb);
                }
            }
        }
    }

    /** Dithered disc: the rectangle dither clipped to a circle. 抖动圆盘：矩形抖动裁剪到圆内。 */
    public static void discDithered(PixelSink sink, int cx, int cy, int r, int argb, int level) {
        for (int y = -r; y <= r; y++) {
            for (int x = -r; x <= r; x++) {
                if (x * x + y * y <= r * r + r && bayer(cx + x, cy + y) < level) {
                    sink.set(cx + x, cy + y, argb);
                }
            }
        }
    }

    public static void line(PixelSink sink, int x1, int y1, int x2, int y2, int argb) {
        int dx = Math.abs(x2 - x1);
        int dy = -Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;
        int err = dx + dy;
        for (;;) {
            sink.set(x1, y1, argb);
            if (x1 == x2 && y1 == y2) {
                return;
            }
            int e2 = 2 * err;
            if (e2 >= dy) {
                err += dy;
                x1 += sx;
            }
            if (e2 <= dx) {
                err += dx;
                y1 += sy;
            }
        }
    }

    /** Line between real endpoints, rounded to pixels first. 实数端点的直线，先四舍五入到像素。 */
    public static void line(PixelSink sink, double x1, double y1, double x2, double y2, int argb) {
        line(sink, (int) Math.round(x1), (int) Math.round(y1), (int) Math.round(x2), (int) Math.round(y2), argb);
    }

    public static void disc(PixelSink sink, int cx, int cy, int r, int argb) {
        for (int y = -r; y <= r; y++) {
            for (int x = -r; x <= r; x++) {
                if (x * x + y * y <= r * r + r) {
                    sink.set(cx + x, cy + y, argb);
                }
            }
        }
    }

    /** 1 px ring of radius r. 半径 r 的 1 像素圆环。 */
    public static void ring(PixelSink sink, int cx, int cy, int r, int argb) {
        int outer = r * r + r;
        int inner = (r - 1) * (r - 1) + (r - 1);
        for (int y = -r; y <= r; y++) {
            for (int x = -r; x <= r; x++) {
                int d = x * x + y * y;
                if (d <= outer && d >= inner) {
                    sink.set(cx + x, cy + y, argb);
                }
            }
        }
    }

    /** '#' = one pixel of {@code argb}, drawn at scale {@code scale} (1 = pixel per character). '#' 为一像素。 */
    public static void bitmap(PixelSink sink, int x, int y, int argb, int scale, String... rows) {
        for (int row = 0; row < rows.length; row++) {
            String line = rows[row];
            for (int i = 0; i < line.length(); i++) {
                if (line.charAt(i) == '#') {
                    sink.fill(x + i * scale, y + row * scale, x + (i + 1) * scale, y + (row + 1) * scale, argb);
                }
            }
        }
    }

    public static void bitmap(PixelSink sink, int x, int y, int argb, String... rows) {
        bitmap(sink, x, y, argb, 1, rows);
    }

    /** Multi-colour bitmap: each character maps to a colour in {@code keys}/{@code colours}; '.' and unknown
     * characters are skipped. 多色位图：字符按 keys/colours 取色，'.' 与未知字符跳过。 */
    public static void glyph(PixelSink sink, int x, int y, String keys, int[] colours, int scale, String... rows) {
        for (int row = 0; row < rows.length; row++) {
            String line = rows[row];
            for (int i = 0; i < line.length(); i++) {
                int slot = keys.indexOf(line.charAt(i));
                if (slot >= 0) {
                    sink.fill(x + i * scale, y + row * scale, x + (i + 1) * scale, y + (row + 1) * scale,
                            colours[slot]);
                }
            }
        }
    }

    /** Dashes along a horizontal run: {@code on} pixels painted, {@code off} skipped. 水平虚线。 */
    public static void dashes(PixelSink sink, int x1, int x2, int y, int on, int off, int argb, int phase) {
        for (int x = x1, k = phase; x < x2; x++, k++) {
            if (k % (on + off) < on) {
                sink.set(x, y, argb);
            }
        }
    }
}
