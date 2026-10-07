package dev.caecorthus.sparkassist.guidebook.decor;

/**
 * Faint procedural watermarks, one per decoration set: the witches' magic circle, the civilians' compass rose,
 * the killers' spider web, the neutrals' harlequin lattice, the traits' constellation and the skills' rune ring.
 * Drawn in one translucent colour around a centre with radius {@code r}; nothing lands outside r + 1.
 * 淡淡的程序暗纹，每套一种：魔女法阵、好人罗盘、杀手蛛网、中立菱格、词条星座、技能符环。以单一半透明色绕圆心 r
 * 绘制，不会落在 r + 1 之外。
 */
public final class Sigils {
    private static final String[] PLUS5 = {"..#..", "..#..", "#####", "..#..", "..#.."};

    private Sigils() {
    }

    public static void draw(PixelSink s, DecorSet.Sigil kind, int cx, int cy, int r, int col, DecorRandom rand) {
        switch (kind) {
            case CIRCLE -> circle(s, cx, cy, r, col, rand);
            case COMPASS -> compass(s, cx, cy, r, col);
            case WEB -> web(s, cx, cy, r, col, rand);
            case HARLEQUIN -> harlequin(s, cx, cy, r, col);
            case CONSTELLATION -> constellation(s, cx, cy, r, col, rand);
            case RUNES -> runes(s, cx, cy, r, col, rand);
        }
    }

    private static void circle(PixelSink s, int cx, int cy, int r, int col, DecorRandom rand) {
        Dither.ring(s, cx, cy, r, col);
        Dither.ring(s, cx, cy, r - 7, col);
        for (int k = 0; k < 24; k++) {
            double a = k * Math.PI / 12;
            Dither.line(s, cx + Math.cos(a) * r, cy + Math.sin(a) * r, cx + Math.cos(a) * (r - 3),
                    cy + Math.sin(a) * (r - 3), col);
        }
        double[][] points = new double[5][];
        for (int k = 0; k < 5; k++) {
            double a = -Math.PI / 2 + k * 2 * Math.PI / 5;
            points[k] = new double[] {cx + Math.cos(a) * (r - 8), cy + Math.sin(a) * (r - 8)};
        }
        for (int k = 0; k < 5; k++) {
            double[] from = points[k];
            double[] to = points[(k + 2) % 5];
            Dither.line(s, from[0], from[1], to[0], to[1], col);
        }
        for (int k = 0; k < 10; k++) {
            double a = k * Math.PI / 5 + 0.3;
            int rx = (int) Math.round(cx + Math.cos(a) * (r - 3.5));
            int ry = (int) Math.round(cy + Math.sin(a) * (r - 3.5));
            rune(s, rx, ry, 3, 3, col, rand);
        }
    }

    private static void compass(PixelSink s, int cx, int cy, int r, int col) {
        Dither.ring(s, cx, cy, r, col);
        Dither.ring(s, cx, cy, r - 3, col);
        for (int k = 0; k < 32; k++) {
            double a = k * Math.PI / 16;
            int inner = r - (k % 4 == 0 ? 8 : 5);
            Dither.line(s, cx + Math.cos(a) * (r - 3), cy + Math.sin(a) * (r - 3), cx + Math.cos(a) * inner,
                    cy + Math.sin(a) * inner, col);
        }
        for (int k = 0; k < 8; k++) {
            double a = k * Math.PI / 4;
            double length = k % 2 == 1 ? r * 0.45 : r - 10;
            int width = k % 2 == 1 ? 3 : 5;
            double tx = cx + Math.cos(a) * length;
            double ty = cy + Math.sin(a) * length;
            double lx = cx + Math.cos(a + Math.PI / 2) * width;
            double ly = cy + Math.sin(a + Math.PI / 2) * width;
            double rx = cx + Math.cos(a - Math.PI / 2) * width;
            double ry = cy + Math.sin(a - Math.PI / 2) * width;
            Dither.line(s, tx, ty, lx, ly, col);
            Dither.line(s, tx, ty, rx, ry, col);
            Dither.line(s, (double) cx, (double) cy, tx, ty, col);
        }
        Dither.ring(s, cx, cy, 4, col);
    }

    private static void web(PixelSink s, int cx, int cy, int r, int col, DecorRandom rand) {
        int n = 11;
        int tear = rand.nextInt(n);
        for (int k = 0; k < n; k++) {
            double a = k * 2 * Math.PI / n - 0.2;
            Dither.line(s, (double) cx, (double) cy, cx + Math.cos(a) * r, cy + Math.sin(a) * r, col);
        }
        for (int ring = 1; ring <= 6; ring++) {
            double rr = r * ring / 6.2;
            for (int k = 0; k < n; k++) {
                if (ring > 3 && k == tear) {
                    continue;
                }
                double a1 = k * 2 * Math.PI / n - 0.2;
                double a2 = (k + 1) * 2 * Math.PI / n - 0.2;
                double am = (a1 + a2) / 2;
                double sag = rr * 0.92;
                Dither.line(s, cx + Math.cos(a1) * rr, cy + Math.sin(a1) * rr, cx + Math.cos(am) * sag,
                        cy + Math.sin(am) * sag, col);
                Dither.line(s, cx + Math.cos(am) * sag, cy + Math.sin(am) * sag, cx + Math.cos(a2) * rr,
                        cy + Math.sin(a2) * rr, col);
            }
        }
    }

    private static void harlequin(PixelSink s, int cx, int cy, int r, int col) {
        for (int y = -r; y <= r; y++) {
            for (int x = -r; x <= r; x++) {
                if (x * x + y * y > r * r) {
                    continue;
                }
                int u = (x + y + 1000) % 12;
                int v = (x - y + 1000) % 12;
                if (u == 0 || v == 0) {
                    s.set(cx + x, cy + y, col);
                } else if (((((x + y + 1000) / 12) + ((x - y + 1000) / 12)) & 1) == 1 && Dither.bayer(x, y) < 3) {
                    s.set(cx + x, cy + y, col);
                }
            }
        }
        Dither.ring(s, cx, cy, r, col);
    }

    private static void constellation(PixelSink s, int cx, int cy, int r, int col, DecorRandom rand) {
        int[][] points = new int[9][];
        for (int k = 0; k < points.length; k++) {
            points[k] = inDisc(cx, cy, r - 3, rand);
        }
        for (int k = 1; k < points.length; k++) {
            Dither.line(s, points[k - 1][0], points[k - 1][1], points[k][0], points[k][1], col);
        }
        for (int[] point : points) {
            Dither.bitmap(s, point[0] - 2, point[1] - 2, col, PLUS5);
        }
        for (int k = 0; k < 30; k++) {
            int[] dot = inDisc(cx, cy, r - 1, rand);
            s.set(dot[0], dot[1], col);
        }
    }

    /** A uniformly random pixel inside the disc of radius {@code radius}. 圆盘内均匀随机的一个像素。 */
    private static int[] inDisc(int cx, int cy, int radius, DecorRandom rand) {
        double angle = rand.next() * 2 * Math.PI;
        double distance = Math.sqrt(rand.next()) * radius;
        return new int[] {cx + (int) Math.round(Math.cos(angle) * distance),
                cy + (int) Math.round(Math.sin(angle) * distance)};
    }

    private static void runes(PixelSink s, int cx, int cy, int r, int col, DecorRandom rand) {
        Dither.ring(s, cx, cy, r, col);
        Dither.ring(s, cx, cy, r - 9, col);
        for (int k = 0; k < 16; k++) {
            double a = k * Math.PI / 8;
            int rx = (int) Math.round(cx + Math.cos(a) * (r - 4.5));
            int ry = (int) Math.round(cy + Math.sin(a) * (r - 4.5));
            rune(s, rx, ry, 3, 5, col, rand);
        }
        for (int k = 0; k < 6; k++) {
            double a = k * Math.PI / 3;
            double b = ((k + 2) % 6) * Math.PI / 3;
            Dither.line(s, cx + Math.cos(a) * (r - 12), cy + Math.sin(a) * (r - 12), cx + Math.cos(b) * (r - 12),
                    cy + Math.sin(b) * (r - 12), col);
        }
    }

    /** A random w x h glyph centred on (cx, cy), about half its cells lit. 以 (cx, cy) 为中心的随机小符。 */
    private static void rune(PixelSink s, int cx, int cy, int w, int h, int col, DecorRandom rand) {
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                if (rand.chance(0.55)) {
                    s.set(cx + i - w / 2, cy + j - h / 2, col);
                }
            }
        }
    }
}
