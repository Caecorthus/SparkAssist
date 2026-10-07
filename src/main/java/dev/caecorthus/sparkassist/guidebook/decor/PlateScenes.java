package dev.caecorthus.sparkassist.guidebook.decor;

import static dev.caecorthus.sparkassist.guidebook.decor.Plates.DISC;
import static dev.caecorthus.sparkassist.guidebook.decor.Plates.HEIGHT;
import static dev.caecorthus.sparkassist.guidebook.decor.Plates.focal;
import static dev.caecorthus.sparkassist.guidebook.decor.Plates.nightSky;
import static dev.caecorthus.sparkassist.guidebook.decor.Plates.ridge;
import static dev.caecorthus.sparkassist.guidebook.decor.Plates.stars;
import static dev.caecorthus.sparkassist.guidebook.decor.Plates.tree;

import dev.caecorthus.sparkassist.guidebook.decor.Plates.Focal;
import dev.caecorthus.sparkassist.guidebook.decor.Plates.Palette;

/**
 * The six procedural faction scenes, the fallback for pages without their own picture. Each paints its whole 56 px
 * picture in the plate's palette around a focal disc (moon, sun, clock tower, wheel hub, rune circle); positions
 * are proportional to the width so a narrow column keeps the composition.
 * 六幅程序化阵营景，供没有自己画作的页面使用。每幅用扉画调色板围绕一个焦点圆盘（月亮、太阳、钟楼、摩天轮轴心、
 * 法阵）画满 56 像素高的画面；位置按宽度比例计算，窄版心也保持构图。
 */
final class PlateScenes {
    private static final String[] WITCH = {".....##.......", "....####......", "....#.##......", "...##.#.......",
            "..#####.......", "#########.....", "...#...#.##...", "..#.....#.#..."};
    private static final String[] BAT = {"##...##", ".#####.", "..#.#.."};
    private static final String[] BIRD = {"#...#", ".#.#.", "..#.."};
    private static final String[] FLAG = {"###", ".#."};

    private PlateScenes() {
    }

    /** Whether (x, y) is within {@code r} of the focal centre. 是否位于焦点 r 范围内。 */
    private static boolean near(int x, int y, int cx, int cy, int r) {
        return (x - cx) * (x - cx) + (y - cy) * (y - cy) < r * r;
    }

    /** 沼泽月夜 — full moon, dead trees, mist, reeds and the moon in the water. */
    static void marsh(PixelSink s, int x0, int y0, int w, Palette p, DecorRandom rand, String[] emblem) {
        nightSky(s, x0, y0, w, p, 34, 16);
        stars(s, x0, y0, w, p, rand, 26, 26);
        int mx = x0 + w * 186 / 252;
        int my = y0 + 18;
        if (!focal(s, Focal.MOON, mx, my, p, emblem)) {
            Dither.disc(s, mx + 3, my - 3, 4, p.accent());
            Dither.bitmap(s, mx - 7, my - 4, p.dark(), WITCH);
        }
        ridge(s, x0, y0, w, 36, 7, rand.next() * 6, p.mid());
        int count = Math.max(3, w / 36);
        for (int i = 0; i < count; i++) {
            int tx = x0 + 12 + i * (w - 24) / count + rand.nextInt(14);
            double th = 12 + rand.next() * 10;
            if (Math.abs(tx - mx) < DISC + 8) {
                th = 7;
            }
            tree(s, tx, y0 + 44, th, -Math.PI / 2 + (rand.next() - 0.5) * 0.5, 4, p.dark(), rand);
        }
        Dither.rect(s, x0, y0 + 39, x0 + w, y0 + 44, p.light(), 6);
        s.fill(x0, y0 + 45, x0 + w, y0 + HEIGHT, p.mid());
        Dither.rect(s, x0, y0 + 45, x0 + w, y0 + HEIGHT, p.dark(), 9);
        for (int y = 46; y < HEIGHT; y += 2) {
            int wobble = (int) Math.round(Math.sin(y * 1.3) * 2);
            Dither.rect(s, mx - 6 + wobble, y0 + y, mx + 7 + wobble, y0 + y + 1, p.glow(), 10 - (y - 46));
        }
        int reeds = w * 44 / 252;
        for (int i = 0; i < reeds; i++) {
            int rx = x0 + rand.nextInt(w);
            int rh = 3 + rand.nextInt(5);
            int lean = rand.chance(0.5) ? -1 : 1;
            Dither.line(s, rx, y0 + 47, rx + lean * Math.round(rh / 4f), y0 + 47 - rh, p.dark());
        }
    }

    /** 晨光高架 — dawn sky, the sun over the hills, a viaduct and the Harpy Express heading left. */
    static void viaduct(PixelSink s, int x0, int y0, int w, Palette p, DecorRandom rand, String[] emblem) {
        s.fill(x0, y0, x0 + w, y0 + HEIGHT, p.paper());
        for (int y = 0; y < 30; y++) {
            int level = (int) Math.round(10 * (1 - y / 30.0));
            Dither.rect(s, x0, y0 + y, x0 + w, y0 + y + 1, p.light(), level);
        }
        for (int i = 0; i < 5; i++) {
            Dither.rect(s, x0, y0 + 24 + i * 2, x0 + w, y0 + 25 + i * 2, p.glow(), 4 + i);
        }
        ridge(s, x0, y0, w, 34, 9, 1.3 + rand.next(), p.light());
        ridge(s, x0, y0, w, 42, 8, 4.1 + rand.next(), p.mid());
        int sx = x0 + w * 58 / 252;
        focal(s, Focal.SUN, sx, y0 + 21, p, emblem);
        s.fill(x0, y0 + 35, x0 + w, y0 + 52, p.dark());
        for (int ax = x0 + 8; ax < x0 + w + 18; ax += 36) {
            for (int y = 39; y < 52; y++) {
                double t = (52 - y) / 13.0;
                int half = (int) Math.round(14 * Math.sqrt(Math.max(0, 1 - t * t)));
                s.fill(ax - half, y0 + y, ax + half, y0 + y + 1, p.light());
            }
        }
        for (int x = 0; x < w; x += 6) {
            s.set(x0 + x, y0 + 37, p.mid());
        }
        s.fill(x0, y0 + 52, x0 + w, y0 + HEIGHT, p.mid());
        Dither.rect(s, x0, y0 + 52, x0 + w, y0 + HEIGHT, p.dark(), 6);
        int trainX = x0 + Math.min(w - 72, w * 120 / 252 + rand.nextInt(40));
        int ty = y0 + 27;
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
        for (int puff = 0; puff < 5; puff++) {
            int px = trainX + 16 + puff * 7 + rand.nextInt(3);
            int py = ty - 2 - puff * 3;
            Dither.discDithered(s, px, py, 2 + puff / 2, p.mid(), 10 - puff);
        }
        for (int b = 0; b < 4; b++) {
            int bx = x0 + w * 150 / 252 + rand.nextInt(Math.max(1, w * 80 / 252));
            int by = y0 + 5 + rand.nextInt(12);
            Dither.bitmap(s, bx, by, p.mid(), BIRD);
        }
    }

    /** 雨夜城 — a skyline with a few red windows, a clock tower, a lamp post, a wet street and slanting rain. */
    static void rain(PixelSink s, int x0, int y0, int w, Palette p, DecorRandom rand, String[] emblem) {
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
        int cx = x0 + w * 168 / 252;
        int cy = y0 + 20;
        s.fill(cx - 15, cy - 13, cx + 16, y0 + 50, p.dark());
        for (int y = 0; y < 8; y++) {
            s.fill(cx - 13 + y * 2, cy - 14 - y, cx + 14 - y * 2, cy - 13 - y, p.dark());
        }
        s.fill(cx, cy - 26, cx + 1, cy - 21, p.dark());
        for (int wy = cy + 18; wy < y0 + 47; wy += 5) {
            s.fill(cx - 9, wy, cx - 7, wy + 2, p.light());
            s.fill(cx + 8, wy, cx + 10, wy + 2, p.light());
        }
        if (!focal(s, Focal.PLAIN, cx, cy, p, emblem)) {
            Dither.line(s, cx, cy, cx, cy - 8, p.dark());
            Dither.line(s, cx, cy, cx + 6, cy + 2, p.dark());
        }
        for (int k = 0; k < 4; k++) {
            double a = k * Math.PI / 2;
            s.set((int) Math.round(cx + Math.cos(a) * 13), (int) Math.round(cy + Math.sin(a) * 13), p.dark());
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
            if (!near(rx, ry, cx, cy, DISC + 4)) {
                Dither.line(s, rx, ry, rx - 1, ry + 3, p.light());
            }
        }
    }

    /** 游园夜 — a ferris wheel with the emblem at its hub, striped tents, bunting and a string of lights. */
    static void fair(PixelSink s, int x0, int y0, int w, Palette p, DecorRandom rand, String[] emblem) {
        nightSky(s, x0, y0, w, p, 44, 12);
        int wx = x0 + w - 56;
        int wy = y0 + 26;
        int r = 22;
        Dither.line(s, wx, wy, wx - 13, y0 + 52, p.dark());
        Dither.line(s, wx + 1, wy, wx - 12, y0 + 52, p.dark());
        Dither.line(s, wx, wy, wx + 13, y0 + 52, p.dark());
        Dither.line(s, wx + 1, wy, wx + 14, y0 + 52, p.dark());
        Dither.ring(s, wx, wy, r, p.light());
        Dither.ring(s, wx, wy, r - 4, p.light());
        for (int k = 0; k < 10; k++) {
            double a = k * Math.PI / 5 + 0.3;
            Dither.line(s, wx + Math.cos(a) * DISC, wy + Math.sin(a) * DISC, wx + Math.cos(a) * r,
                    wy + Math.sin(a) * r, p.light());
            int gx = (int) Math.round(wx + Math.cos(a) * r);
            int gy = (int) Math.round(wy + Math.sin(a) * r);
            s.fill(gx - 1, gy, gx + 2, gy + 3, (k & 1) == 1 ? p.accent() : p.glow());
            s.set(gx, gy - 1, p.light());
        }
        if (!focal(s, Focal.PLAIN, wx, wy, p, emblem)) {
            Dither.disc(s, wx, wy, 3, p.dark());
        }
        int tents = w < 200 ? 2 : 3;
        for (int t = 0; t < tents; t++) {
            int tx = x0 + (26 + t * 52) * w / 252 + rand.nextInt(8);
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
            if (Math.abs(x0 + x + 12 - wx) < r + 10) {
                continue;
            }
            Dither.line(s, x0 + x, y0 + ya, x0 + x + 24, y0 + yb, p.light());
            for (int k = 4; k < 24; k += 6) {
                int fx = x0 + x + k;
                int fy = y0 + (int) Math.round(ya + (yb - ya) * k / 24.0);
                Dither.bitmap(s, fx - 1, fy + 1, ((k / 6) & 1) == 1 ? p.accent() : p.glow(), FLAG);
            }
        }
        for (int x = 6; x < w; x += 5) {
            if (Math.abs(x0 + x - wx) > r + 3) {
                s.set(x0 + x, y0 + 15 + ((x / 5) & 1), (x / 5) % 3 == 0 ? p.accent() : p.glow());
            }
        }
        s.fill(x0, y0 + 52, x0 + w, y0 + HEIGHT, p.mid());
        Dither.rect(s, x0, y0 + 52, x0 + w, y0 + HEIGHT, p.dark(), 6);
    }

    /** 夜原 — a full moon, two ridges, a lone tree, bats and fireflies. */
    static void field(PixelSink s, int x0, int y0, int w, Palette p, DecorRandom rand, String[] emblem) {
        nightSky(s, x0, y0, w, p, 38, 13);
        stars(s, x0, y0, w, p, rand, 30, 30);
        int mx = x0 + w * 64 / 252;
        int my = y0 + 18;
        if (!focal(s, Focal.MOON, mx, my, p, emblem)) {
            Dither.disc(s, mx + 5, my - 3, 9, p.mid());
            Dither.discDithered(s, mx + 5, my - 3, 9, p.dark(), 8);
        }
        ridge(s, x0, y0, w, 40, 8, 2.2 + rand.next(), p.mid());
        ridge(s, x0, y0, w, 47, 7, 5.5 + rand.next(), p.dark());
        int tx = x0 + Math.min(w - 12, w * 170 / 252 + rand.nextInt(30));
        tree(s, tx, y0 + 44, 17, -Math.PI / 2 + 0.1, 5, p.dark(), rand);
        for (int b = 0; b < 4; b++) {
            int bx = x0 + w * 100 / 252 + rand.nextInt(Math.max(1, w * 110 / 252));
            int by = y0 + 6 + rand.nextInt(20);
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

    /** 法阵 — rune rings and a pentagram around the emblem, two candles and drifting sparks. */
    static void arcane(PixelSink s, int x0, int y0, int w, Palette p, DecorRandom rand, String[] emblem) {
        s.fill(x0, y0, x0 + w, y0 + HEIGHT, p.dark());
        Dither.rect(s, x0, y0, x0 + w, y0 + HEIGHT, p.mid(), 5);
        int cx = x0 + w / 2;
        int cy = y0 + 28;
        Dither.ring(s, cx, cy, 25, p.light());
        Dither.ring(s, cx, cy, 21, p.light());
        for (int k = 0; k < 24; k++) {
            double a = k * Math.PI / 12;
            int inner = k % 6 == 0 ? 19 : 23;
            Dither.line(s, cx + Math.cos(a) * 25, cy + Math.sin(a) * 25, cx + Math.cos(a) * inner,
                    cy + Math.sin(a) * inner, p.light());
        }
        double[][] pts = new double[5][];
        for (int k = 0; k < 5; k++) {
            double a = -Math.PI / 2 + k * 2 * Math.PI / 5;
            pts[k] = new double[] {cx + Math.cos(a) * 20, cy + Math.sin(a) * 20};
        }
        for (int k = 0; k < 5; k++) {
            Dither.line(s, pts[k][0], pts[k][1], pts[(k + 2) % 5][0], pts[(k + 2) % 5][1], p.accent());
        }
        for (int k = 0; k < 12; k++) {
            double a = k * Math.PI / 6 + 0.26;
            int rx = (int) Math.round(cx + Math.cos(a) * 31);
            int ry = (int) Math.round(cy + Math.sin(a) * 31);
            for (int r = 0; r < 4; r++) {
                for (int c = 0; c < 3; c++) {
                    if (rand.chance(0.55)) {
                        s.set(rx + c - 1, ry + r - 2, p.light());
                    }
                }
            }
        }
        for (int k = 0; k < 40; k++) {
            int sx = x0 + rand.nextInt(w);
            int sy = y0 + rand.nextInt(HEIGHT);
            if (!near(sx, sy, cx, cy, DISC + 2)) {
                s.set(sx, sy, rand.chance(0.5) ? p.accent() : p.glow());
            }
        }
        for (int lx : new int[] {x0 + 22, x0 + w - 30}) {
            s.fill(lx - 1, y0 + 38, lx + 2, y0 + 52, p.light());
            Dither.disc(s, lx, y0 + 35, 2, p.accent());
            s.set(lx, y0 + 33, p.glow());
            Dither.rect(s, lx - 5, y0 + 30, lx + 6, y0 + 41, p.glow(), 4);
        }
        if (!focal(s, Focal.PLAIN, cx, cy, p, emblem)) {
            Dither.ring(s, cx, cy, 9, p.deep());
            Dither.ring(s, cx, cy, 5, p.deep());
        }
    }
}
