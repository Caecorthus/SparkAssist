package dev.caecorthus.sparkassist.guidebook.decor;

import static dev.caecorthus.sparkassist.guidebook.decor.DecorPalette.INK;
import static dev.caecorthus.sparkassist.guidebook.decor.DecorPalette.INK_FAINT;
import static dev.caecorthus.sparkassist.guidebook.decor.DecorPalette.INK_MUTED;
import static dev.caecorthus.sparkassist.guidebook.decor.DecorPalette.INK_RULE;
import static dev.caecorthus.sparkassist.guidebook.decor.DecorPalette.INK_RULE_SOFT;
import static dev.caecorthus.sparkassist.guidebook.decor.DecorPalette.PAPER;

/**
 * The chapter plates: six duotone scenes, 56 px tall and as wide as the text column, painted in paper, three inks
 * and one accent (the set's colour mixed into the paper) with Bayer dither for every gradient. Stars, trees,
 * buildings and rain come from the page seed; the composition does not. {@link #plate} also draws the thin plate
 * frame with its corner ticks, 3 px outside the picture.
 * 扉画：六幅双色景，高 56、宽同版心，只用纸色、三阶墨色与一个点睛色（套别色调进纸色），渐变全靠 Bayer 抖动。星、树、楼、
 * 雨随页种子变化，构图不变。plate() 同时在画外 3 像素处画细框与角标。
 */
public final class Plates {
    public static final int HEIGHT = 56;
    /** Frame and corner ticks reach this far outside the picture. 框与角标伸出画面的距离。 */
    public static final int MARGIN = 3;
    /** Narrowest text column that still gets a plate. 仍画扉画的最窄版心。 */
    public static final int MIN_WIDTH = 160;

    private static final String[] WITCH = {".....##.......", "....####......", "....#.##......", "...##.#.......",
            "..#####.......", "#########.....", "...#...#.##...", "..#.....#.#..."};
    private static final String[] BAT = {"##...##", ".#####.", "..#.#.."};
    private static final String[] BIRD = {"#...#", ".#.#.", "..#.."};
    private static final String[] FLAG = {"###", ".#."};

    /** The six colours of a plate. 一幅扉画的六种颜色。 */
    public record Palette(int paper, int light, int mid, int dark, int accent, int glow) {
        public static Palette of(int accentRgb) {
            int accent = DecorPalette.opaque(accentRgb);
            return new Palette(PAPER, INK_FAINT, INK_MUTED, INK, DecorPalette.mix(PAPER, accent, 0.55),
                    DecorPalette.mix(PAPER, accent, 0.22));
        }
    }

    private Plates() {
    }

    /**
     * Picture plus frame, with the picture's top-left at (x, y) and width {@code w}; the frame uses the 3 px
     * margin around it. The sink should extend {@link #MARGIN} beyond the picture on every side.
     * 画面左上角在 (x, y)、宽 w 的扉画连同画框；画框占用画面四周的 3 像素。落笔面应比画面四周各大 MARGIN。
     */
    public static void plate(PixelSink sink, DecorSet.Plate kind, int x, int y, int w, int accentRgb, long seed) {
        scene(sink, kind, x, y, w, Palette.of(accentRgb), new DecorRandom(seed));
        Ornaments.roundedOutline(sink, x - 1, y - 1, w + 2, HEIGHT + 2, INK_RULE);
        sink.fill(x, y + HEIGHT + 1, x + w + 1, y + HEIGHT + 2, INK_RULE_SOFT);
        sink.fill(x + w + 1, y, x + w + 2, y + HEIGHT + 1, INK_RULE_SOFT);
        tick(sink, x - 3, y - 3);
        tick(sink, x + w, y - 3);
        tick(sink, x - 3, y + HEIGHT);
        tick(sink, x + w, y + HEIGHT);
    }

    private static void tick(PixelSink sink, int cx, int cy) {
        // Two arms sharing the corner pixel once, so translucent ink does not stack there.
        // 两臂只共用角点一次，半透明墨色不会在角点叠加。
        sink.fill(cx, cy, cx + 3, cy + 1, INK_RULE_SOFT);
        sink.fill(cx, cy + 1, cx + 1, cy + 3, INK_RULE_SOFT);
    }

    /** The picture alone, filling exactly (x, y)..(x + w, y + HEIGHT); strokes that wander out are clipped.
     * 只画画面本身，恰好填满 (x, y) 到 (x + w, y + HEIGHT)；越界的笔画被裁掉。 */
    public static void scene(PixelSink sink, DecorSet.Plate kind, int x, int y, int w, Palette p, DecorRandom rand) {
        PixelSink s = new ClipSink(sink, x, y, w, HEIGHT);
        switch (kind) {
            case MARSH -> marsh(s, x, y, w, p, rand);
            case VIADUCT -> viaduct(s, x, y, w, p, rand);
            case RAIN -> rain(s, x, y, w, p, rand);
            case FAIR -> fair(s, x, y, w, p, rand);
            case FIELD -> field(s, x, y, w, p, rand);
            case ARCANE -> arcane(s, x, y, w, p, rand);
        }
    }

    // ================================================================ scenes / 六幅景

    /** 魔女：沼泽月夜 — full moon, a witch crossing it, dead trees, mist, reeds and the moon in the water. */
    private static void marsh(PixelSink s, int x0, int y0, int w, Palette p, DecorRandom rand) {
        nightSky(s, x0, y0, w, p, 34, 16);
        stars(s, x0, y0, w, p, rand, 26, 26);
        int mx = x0 + w * 186 / 252;
        int my = y0 + 16;
        Dither.disc(s, mx, my, 9, p.glow());
        Dither.disc(s, mx + 1, my - 1, 8, p.accent());
        Dither.disc(s, mx + 3, my - 3, 4, p.glow());
        s.set(mx - 3, my + 2, p.accent());
        s.set(mx + 2, my + 4, p.accent());
        Dither.bitmap(s, mx - 7, my - 4, p.dark(), WITCH);
        ridge(s, x0, y0, w, 36, 7, rand.next() * 6, p.mid());
        int count = Math.max(3, w / 36);
        for (int i = 0; i < count; i++) {
            int tx = x0 + 12 + i * (w - 24) / count + rand.nextInt(14);
            double th = 12 + rand.next() * 10;
            tree(s, tx, y0 + 44, th, -Math.PI / 2 + (rand.next() - 0.5) * 0.5, 4, p.dark(), rand);
        }
        Dither.rect(s, x0, y0 + 39, x0 + w, y0 + 44, p.light(), 6);
        s.fill(x0, y0 + 45, x0 + w, y0 + HEIGHT, p.mid());
        Dither.rect(s, x0, y0 + 45, x0 + w, y0 + HEIGHT, p.dark(), 9);
        for (int y = 46; y < HEIGHT; y += 2) {
            int wobble = (int) Math.round(Math.sin(y * 1.3) * 2);
            Dither.rect(s, mx - 5 + wobble, y0 + y, mx + 6 + wobble, y0 + y + 1, p.glow(), 10 - (y - 46));
        }
        int reeds = w * 44 / 252;
        for (int i = 0; i < reeds; i++) {
            int rx = x0 + rand.nextInt(w);
            int rh = 3 + rand.nextInt(5);
            int lean = rand.chance(0.5) ? -1 : 1;
            Dither.line(s, rx, y0 + 47, rx + lean * Math.round(rh / 4f), y0 + 47 - rh, p.dark());
        }
    }

    /** 好人：晨光高架 — dawn sky, the sun on the horizon, a viaduct and the Harpy Express heading left. */
    private static void viaduct(PixelSink s, int x0, int y0, int w, Palette p, DecorRandom rand) {
        s.fill(x0, y0, x0 + w, y0 + HEIGHT, p.paper());
        for (int y = 0; y < 30; y++) {
            int level = (int) Math.round(10 * (1 - y / 30.0));
            Dither.rect(s, x0, y0 + y, x0 + w, y0 + y + 1, p.light(), level);
        }
        int sx = x0 + w * 58 / 252;
        int sy = y0 + 31;
        Dither.disc(s, sx, sy, 10, p.glow());
        Dither.disc(s, sx, sy, 8, p.accent());
        for (int i = 0; i < 5; i++) {
            Dither.rect(s, x0, y0 + 24 + i * 2, x0 + w, y0 + 25 + i * 2, p.glow(), 4 + i);
        }
        ridge(s, x0, y0, w, 34, 9, 1.3 + rand.next(), p.light());
        ridge(s, x0, y0, w, 42, 8, 4.1 + rand.next(), p.mid());
        s.fill(x0, y0 + 34, x0 + w, y0 + 52, p.dark());
        for (int ax = x0 + 8; ax < x0 + w + 18; ax += 36) {
            for (int y = 38; y < 52; y++) {
                double t = (52 - y) / 14.0;
                int half = (int) Math.round(14 * Math.sqrt(Math.max(0, 1 - t * t)));
                s.fill(ax - half, y0 + y, ax + half, y0 + y + 1, p.light());
            }
        }
        for (int x = 0; x < w; x += 6) {
            s.set(x0 + x, y0 + 36, p.mid());
        }
        s.fill(x0, y0 + 52, x0 + w, y0 + HEIGHT, p.mid());
        Dither.rect(s, x0, y0 + 52, x0 + w, y0 + HEIGHT, p.dark(), 6);
        int trainX = x0 + Math.min(w - 72, w * 120 / 252 + rand.nextInt(40));
        int ty = y0 + 26;
        s.fill(trainX, ty + 2, trainX + 7, ty + 8, p.dark());
        s.fill(trainX + 7, ty + 4, trainX + 19, ty + 8, p.dark());
        s.fill(trainX + 15, ty + 1, trainX + 17, ty + 4, p.dark());
        s.fill(trainX + 1, ty + 3, trainX + 3, ty + 5, p.glow());
        for (int c = 0; c < 3; c++) {
            int cx = trainX + 21 + c * 16;
            s.fill(cx, ty + 2, cx + 14, ty + 8, p.dark());
            for (int k = 0; k < 4; k++) {
                s.fill(cx + 2 + k * 3, ty + 4, cx + 3 + k * 3, ty + 6, p.glow());
            }
        }
        for (int puff = 0; puff < 6; puff++) {
            int px = trainX + 14 - puff * 7 - rand.nextInt(3);
            int py = ty - 2 - puff * 3;
            int r = 2 + puff / 2;
            Dither.discDithered(s, px, py, r, p.mid(), 10 - puff);
        }
        for (int b = 0; b < 4; b++) {
            int bx = x0 + w * 150 / 252 + rand.nextInt(Math.max(1, w * 80 / 252));
            int by = y0 + 6 + rand.nextInt(12);
            Dither.bitmap(s, bx, by, p.mid(), BIRD);
        }
    }

    /** 杀手：雨夜城 — a skyline with a few red windows, a lamp post, a wet street and slanting rain. */
    private static void rain(PixelSink s, int x0, int y0, int w, Palette p, DecorRandom rand) {
        nightSky(s, x0, y0, w, p, 40, 14);
        int bx = x0 - 4;
        while (bx < x0 + w) {
            int bw = 9 + rand.nextInt(13);
            int bh = 12 + rand.nextInt(22);
            s.fill(bx, y0 + 50 - bh, bx + bw, y0 + 50, p.dark());
            for (int wy = y0 + 50 - bh + 2; wy < y0 + 48; wy += 4) {
                for (int wx = bx + 2; wx < bx + bw - 1; wx += 3) {
                    if (rand.chance(0.5)) {
                        s.set(wx, wy, rand.chance(0.1) ? p.accent() : p.light());
                    }
                }
            }
            bx += bw + 1 + rand.nextInt(3);
        }
        int lx = x0 + w * 44 / 252;
        s.fill(lx, y0 + 20, lx + 2, y0 + 50, p.dark());
        s.fill(lx - 2, y0 + 18, lx + 4, y0 + 21, p.dark());
        s.set(lx + 1, y0 + 19, p.glow());
        Dither.discDithered(s, lx + 1, y0 + 20, 6, p.glow(), 5);
        s.fill(x0, y0 + 50, x0 + w, y0 + HEIGHT, p.mid());
        Dither.rect(s, x0, y0 + 50, x0 + w, y0 + HEIGHT, p.dark(), 8);
        for (int y = 51; y < HEIGHT; y++) {
            Dither.rect(s, lx - 3 + (y & 1), y0 + y, lx + 6 - (y & 1), y0 + y + 1, p.glow(), 7 - (y - 51));
        }
        int drops = w * 70 / 252;
        for (int i = 0; i < drops; i++) {
            int rx = x0 + rand.nextInt(w);
            int ry = y0 + rand.nextInt(50);
            Dither.line(s, rx, ry, rx - 1, ry + 3, p.light());
        }
    }

    /** 中立：游园夜 — a ferris wheel, striped tents, bunting and a string of lights. */
    private static void fair(PixelSink s, int x0, int y0, int w, Palette p, DecorRandom rand) {
        nightSky(s, x0, y0, w, p, 44, 12);
        int wx = x0 + w - 54;
        int wy = y0 + 30;
        int r = 17;
        Dither.ring(s, wx, wy, r, p.light());
        for (int k = 0; k < 8; k++) {
            double a = k * Math.PI / 4 + 0.3;
            Dither.line(s, (double) wx, (double) wy, wx + Math.cos(a) * r, wy + Math.sin(a) * r, p.light());
            int cx = (int) Math.round(wx + Math.cos(a) * r);
            int cy = (int) Math.round(wy + Math.sin(a) * r);
            s.fill(cx - 1, cy, cx + 2, cy + 2, (k & 1) == 1 ? p.accent() : p.glow());
        }
        Dither.line(s, wx, wy, wx - 10, y0 + 52, p.dark());
        Dither.line(s, wx, wy, wx + 10, y0 + 52, p.dark());
        Dither.disc(s, wx, wy, 2, p.dark());
        int tents = w < 200 ? 2 : 3;
        for (int t = 0; t < tents; t++) {
            int tx = x0 + (30 + t * 58) * w / 252 + rand.nextInt(8);
            int tw = 26 + t * 4;
            int th = 18 + t * 2;
            int top = y0 + 52 - th;
            for (int dx = -tw / 2; dx <= tw / 2; dx++) {
                int h = (int) Math.round(th * (1 - Math.abs(dx) / (tw / 2.0)));
                int col = ((((dx + tw / 2) / 3) & 1) == 1) ? p.light() : p.dark();
                s.fill(tx + dx, y0 + 52 - h, tx + dx + 1, y0 + 52, col);
            }
            s.fill(tx - 1, top - 4, tx + 1, top, p.accent());
            s.set(tx, top - 4, p.accent());
            s.fill(tx - 3, y0 + 46, tx + 3, y0 + 52, p.dark());
        }
        for (int x = 0; x < w; x += 24) {
            boolean odd = ((x / 24) & 1) == 1;
            int ya = odd ? 3 : 8;
            int yb = odd ? 8 : 3;
            Dither.line(s, x0 + x, y0 + ya, x0 + x + 24, y0 + yb, p.light());
            for (int k = 4; k < 24; k += 6) {
                int fx = x0 + x + k;
                int fy = y0 + (int) Math.round(ya + (yb - ya) * k / 24.0);
                Dither.bitmap(s, fx - 1, fy + 1, ((k / 6) & 1) == 1 ? p.accent() : p.glow(), FLAG);
            }
        }
        for (int x = 6; x < w; x += 5) {
            s.set(x0 + x, y0 + 14 + ((x / 5) & 1), (x / 5) % 3 == 0 ? p.accent() : p.glow());
        }
        s.fill(x0, y0 + 52, x0 + w, y0 + HEIGHT, p.mid());
        Dither.rect(s, x0, y0 + 52, x0 + w, y0 + HEIGHT, p.dark(), 6);
    }

    /** 词条：夜原 — crescent moon, two ridges, a lone tree, bats and fireflies. */
    private static void field(PixelSink s, int x0, int y0, int w, Palette p, DecorRandom rand) {
        nightSky(s, x0, y0, w, p, 38, 13);
        stars(s, x0, y0, w, p, rand, 30, 30);
        int mx = x0 + w * 64 / 252;
        int my = y0 + 14;
        Dither.disc(s, mx, my, 8, p.glow());
        Dither.disc(s, mx + 4, my - 2, 7, p.mid());
        Dither.discDithered(s, mx + 4, my - 2, 7, p.dark(), 8);
        ridge(s, x0, y0, w, 40, 8, 2.2 + rand.next(), p.mid());
        ridge(s, x0, y0, w, 47, 7, 5.5 + rand.next(), p.dark());
        int tx = x0 + Math.min(w - 12, w * 170 / 252 + rand.nextInt(30));
        tree(s, tx, y0 + 44, 17, -Math.PI / 2 + 0.1, 5, p.dark(), rand);
        for (int b = 0; b < 4; b++) {
            int bx = x0 + w * 90 / 252 + rand.nextInt(Math.max(1, w * 120 / 252));
            int by = y0 + 8 + rand.nextInt(20);
            Dither.bitmap(s, bx, by, p.dark(), BAT);
        }
        for (int f = 0; f < 18; f++) {
            int fx = x0 + rand.nextInt(w);
            int fy = y0 + 36 + rand.nextInt(16);
            s.set(fx, fy, p.accent());
            if (rand.chance(0.4)) {
                s.set(fx + 1, fy, p.glow());
            }
        }
    }

    /** 魔女技能：法阵 — rune rings, a pentagram, two candles and drifting sparks. */
    private static void arcane(PixelSink s, int x0, int y0, int w, Palette p, DecorRandom rand) {
        s.fill(x0, y0, x0 + w, y0 + HEIGHT, p.dark());
        Dither.rect(s, x0, y0, x0 + w, y0 + HEIGHT, p.mid(), 5);
        int cx = x0 + w / 2;
        int cy = y0 + 30;
        Dither.ring(s, cx, cy, 24, p.light());
        Dither.ring(s, cx, cy, 20, p.light());
        Dither.ring(s, cx, cy, 11, p.glow());
        for (int k = 0; k < 24; k++) {
            double a = k * Math.PI / 12;
            int inner = k % 6 == 0 ? 18 : 22;
            Dither.line(s, cx + Math.cos(a) * 24, cy + Math.sin(a) * 24, cx + Math.cos(a) * inner,
                    cy + Math.sin(a) * inner, p.light());
        }
        double[][] pts = new double[5][];
        for (int k = 0; k < 5; k++) {
            double a = -Math.PI / 2 + k * 2 * Math.PI / 5;
            pts[k] = new double[] {cx + Math.cos(a) * 19, cy + Math.sin(a) * 19};
        }
        for (int k = 0; k < 5; k++) {
            Dither.line(s, pts[k][0], pts[k][1], pts[(k + 2) % 5][0], pts[(k + 2) % 5][1], p.accent());
        }
        for (int k = 0; k < 12; k++) {
            double a = k * Math.PI / 6 + 0.26;
            int rx = (int) Math.round(cx + Math.cos(a) * 30);
            int ry = (int) Math.round(cy + Math.sin(a) * 30);
            for (int r = 0; r < 4; r++) {
                for (int c = 0; c < 3; c++) {
                    if (rand.chance(0.55)) {
                        s.set(rx + c - 1, ry + r - 2, p.light());
                    }
                }
            }
        }
        for (int k = 0; k < 40; k++) {
            s.set(x0 + rand.nextInt(w), y0 + rand.nextInt(HEIGHT), rand.chance(0.5) ? p.accent() : p.glow());
        }
        for (int lx : new int[] {x0 + 22, x0 + w - 30}) {
            s.fill(lx - 1, y0 + 38, lx + 2, y0 + 52, p.light());
            Dither.disc(s, lx, y0 + 35, 2, p.accent());
            s.set(lx, y0 + 33, p.glow());
            Dither.rect(s, lx - 5, y0 + 30, lx + 6, y0 + 41, p.glow(), 4);
        }
    }

    // ================================================================ parts / 部件

    /** Mid ground with the top {@code horizon} rows dithered towards dark. 中灰底，上方 horizon 行向暗色抖动。 */
    private static void nightSky(PixelSink s, int x0, int y0, int w, Palette p, int horizon, int topLevel) {
        s.fill(x0, y0, x0 + w, y0 + HEIGHT, p.mid());
        for (int y = 0; y < horizon; y++) {
            int level = (int) Math.round(topLevel * (1 - (double) y / horizon));
            Dither.rect(s, x0, y0 + y, x0 + w, y0 + y + 1, p.dark(), level);
        }
    }

    private static void stars(PixelSink s, int x0, int y0, int w, Palette p, DecorRandom rand, int n, int maxY) {
        int count = n * w / 252;
        for (int i = 0; i < count; i++) {
            int sx = x0 + rand.nextInt(w);
            int sy = y0 + rand.nextInt(maxY);
            s.set(sx, sy, p.light());
            if (rand.chance(0.2)) {
                s.set(sx + 1, sy, p.mid());
                s.set(sx, sy + 1, p.mid());
            }
        }
    }

    /** A rolling silhouette from {@code baseY} down to the plate bottom. 从 baseY 向下填满的起伏剪影。 */
    private static void ridge(PixelSink s, int x0, int y0, int w, int baseY, int amp, double phase, int col) {
        for (int x = 0; x < w; x++) {
            double v = 0.55 * Math.sin(x * 0.045 + phase) + 0.3 * Math.sin(x * 0.11 + phase * 1.7)
                    + 0.15 * Math.sin(x * 0.27 + phase * 2.3);
            int top = (int) Math.round(baseY - amp * (0.5 + 0.5 * v));
            s.fill(x0 + x, y0 + top, x0 + x + 1, y0 + HEIGHT, col);
        }
    }

    /** A bare, twisted tree: trunk then two branches, recursively. 枯树：主干与两根分枝，递归。 */
    private static void tree(PixelSink s, double x, double y, double len, double angle, int depth, int col,
                             DecorRandom rand) {
        double x2 = x + Math.cos(angle) * len;
        double y2 = y + Math.sin(angle) * len;
        Dither.line(s, x, y, x2, y2, col);
        if (depth > 1 && len > 2) {
            Dither.line(s, x + 1, y, x2 + 1, y2, col);
        }
        if (depth == 0 || len < 2) {
            return;
        }
        double spread = 0.45 + rand.next() * 0.35;
        tree(s, x2, y2, len * 0.68, angle - spread, depth - 1, col, rand);
        tree(s, x2, y2, len * 0.68, angle + spread * (0.6 + rand.next() * 0.6), depth - 1, col, rand);
    }
}
