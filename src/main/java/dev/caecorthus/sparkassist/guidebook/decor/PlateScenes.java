package dev.caecorthus.sparkassist.guidebook.decor;

import static dev.caecorthus.sparkassist.guidebook.decor.Plates.DISC;
import static dev.caecorthus.sparkassist.guidebook.decor.Plates.HEIGHT;
import static dev.caecorthus.sparkassist.guidebook.decor.Plates.focal;
import static dev.caecorthus.sparkassist.guidebook.decor.Plates.gear;
import static dev.caecorthus.sparkassist.guidebook.decor.Plates.lancet;
import static dev.caecorthus.sparkassist.guidebook.decor.Plates.nightSky;
import static dev.caecorthus.sparkassist.guidebook.decor.Plates.pine;
import static dev.caecorthus.sparkassist.guidebook.decor.Plates.ridge;
import static dev.caecorthus.sparkassist.guidebook.decor.Plates.stars;
import static dev.caecorthus.sparkassist.guidebook.decor.Plates.tree;

import dev.caecorthus.sparkassist.guidebook.decor.Plates.Focal;
import dev.caecorthus.sparkassist.guidebook.decor.Plates.Palette;

/**
 * The sixteen plate scenes. Each paints its whole 56 px picture in the plate's palette and places the focal disc
 * with the page emblem; positions are proportional to the width so a narrow column keeps the composition. Things
 * that would cross the emblem (trees, rain, string lights) step around the disc.
 * 十六幅扉画景。每幅用扉画调色板画满 56 像素高的画面，并放置带页面徽记的焦点圆盘；位置按宽度比例计算，窄版心也保持
 * 构图。会穿过徽记的东西（树、雨、串灯）都绕开圆盘。
 */
final class PlateScenes {
    private static final String[] WITCH = {".....##.......", "....####......", "....#.##......", "...##.#.......",
            "..#####.......", "#########.....", "...#...#.##...", "..#.....#.#..."};
    private static final String[] BAT = {"##...##", ".#####.", "..#.#.."};
    private static final String[] BIRD = {"#...#", ".#.#.", "..#.."};
    private static final String[] FLAG = {"###", ".#."};
    private static final String[] CROW = {"..##...", ".####..", "######.", "..##.##", "..#...."};

    private PlateScenes() {
    }

    /** Whether (x, y) is within {@code r} of the focal centre. 是否位于焦点 r 范围内。 */
    private static boolean near(int x, int y, int cx, int cy, int r) {
        return (x - cx) * (x - cx) + (y - cy) * (y - cy) < r * r;
    }

    // ================================================================ faction defaults / 阵营默认

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

    // ================================================================ role scenes / 身份景

    /** 车厢 — inside a carriage at night: arched windows with the land streaming past, sconces, a luggage rack,
     * high-backed seats, and the emblem on the wall clock. */
    static void carriage(PixelSink s, int x0, int y0, int w, Palette p, DecorRandom rand, String[] emblem) {
        s.fill(x0, y0, x0 + w, y0 + HEIGHT, p.mid());
        s.fill(x0, y0, x0 + w, y0 + 4, p.dark());
        Dither.rect(s, x0, y0 + 4, x0 + w, y0 + 6, p.dark(), 8);
        s.fill(x0, y0 + 7, x0 + w, y0 + 8, p.light());
        for (int x = 1; x < w; x += 4) {
            s.set(x0 + x, y0 + 8, p.light());
        }
        int cx = x0 + w / 2;
        int cy = y0 + 23;
        int winW = 30;
        double hill = rand.next() * 6;
        for (int side = -1; side <= 1; side += 2) {
            int edge = side < 0 ? cx - 21 - winW : cx + 21;
            while (side < 0 ? edge + winW > x0 : edge < x0 + w) {
                window(s, edge, y0 + 11, winW, 23, p, rand, hill);
                int sconce = side < 0 ? edge - 6 : edge + winW + 6;
                if (Math.abs(sconce - cx) > 18) {
                    Dither.discDithered(s, sconce, y0 + 18, 5, p.glow(), 5);
                    s.fill(sconce - 1, y0 + 17, sconce + 2, y0 + 20, p.glow());
                    s.fill(sconce, y0 + 20, sconce + 1, y0 + 23, p.dark());
                }
                edge += side * (winW + 12);
            }
        }
        if (!focal(s, Focal.CLOCK, cx, cy, p, emblem)) {
            Dither.line(s, cx, cy, cx, cy - 8, p.dark());
            Dither.line(s, cx, cy, cx + 5, cy + 3, p.dark());
        }
        s.fill(x0, y0 + 40, x0 + w, y0 + 41, p.light());
        Dither.rect(s, x0, y0 + 41, x0 + w, y0 + HEIGHT, p.dark(), 6);
        for (int x = 8; x < w; x += 16) {
            s.fill(x0 + x, y0 + 42, x0 + x + 1, y0 + 52, p.dark());
        }
        int seat = x0 - rand.nextInt(10);
        while (seat < x0 + w) {
            Ornaments.roundedFill(s, seat, y0 + 44, 22, 12, p.dark());
            s.fill(seat + 2, y0 + 44, seat + 20, y0 + 45, p.accent());
            s.fill(seat + 3, y0 + 46, seat + 19, y0 + 47, p.mid());
            seat += 30;
        }
        s.fill(x0, y0 + 53, x0 + w, y0 + HEIGHT, p.dark());
    }

    /** One carriage window: brass frame, night outside, a hill line and motion streaks. 一扇车窗。 */
    private static void window(PixelSink s, int x, int y, int w, int h, Palette p, DecorRandom rand, double hill) {
        Ornaments.roundedFill(s, x - 1, y - 1, w + 2, h + 2, p.light());
        Ornaments.roundedFill(s, x, y, w, h, p.dark());
        for (int i = 0; i < w / 6; i++) {
            s.set(x + 1 + rand.nextInt(w - 2), y + 1 + rand.nextInt(h / 2), p.light());
        }
        for (int dx = 0; dx < w; dx++) {
            int top = y + h - 7 + (int) Math.round(2.5 * Math.sin((x + dx) * 0.13 + hill));
            s.fill(x + dx, top, x + dx + 1, y + h, p.mid());
        }
        for (int k = 0; k < 3; k++) {
            int sy = y + h - 3 - k * 2;
            int sx = x + 2 + rand.nextInt(Math.max(1, w - 12));
            Dither.dashes(s, sx, Math.min(x + w - 1, sx + 6 + rand.nextInt(6)), sy, 3, 1, p.light(), k);
        }
        s.fill(x + w / 2, y, x + w / 2 + 1, y + h, p.light());
    }

    /** 月台 — under an iron train shed: truss roof, a hanging clock bearing the emblem, a waiting train, lamps,
     * a bench and the platform edge. */
    static void station(PixelSink s, int x0, int y0, int w, Palette p, DecorRandom rand, String[] emblem) {
        nightSky(s, x0, y0, w, p, 40, 12);
        stars(s, x0, y0 + 12, w, p, rand, 10, 14);
        s.fill(x0, y0, x0 + w, y0 + 5, p.dark());
        for (int x = -12; x < w; x += 12) {
            Dither.line(s, x0 + x, y0 + 5, x0 + x + 6, y0 + 11, p.dark());
            Dither.line(s, x0 + x + 6, y0 + 11, x0 + x + 12, y0 + 5, p.dark());
        }
        s.fill(x0, y0 + 11, x0 + w, y0 + 12, p.dark());
        int cx = x0 + w * 150 / 252;
        int cy = y0 + 26;
        for (int x = x0 + 14; x < x0 + w; x += 58) {
            if (Math.abs(x - cx) > 20) {
                s.fill(x, y0 + 12, x + 2, y0 + 45, p.dark());
                s.fill(x - 2, y0 + 12, x + 4, y0 + 14, p.dark());
            }
        }
        int trainEnd = x0 + w * (56 + rand.nextInt(24)) / 252;
        s.fill(x0, y0 + 24, trainEnd, y0 + 45, p.dark());
        Ornaments.roundedFill(s, trainEnd - 6, y0 + 24, 8, 21, p.dark());
        s.fill(x0, y0 + 26, trainEnd, y0 + 27, p.mid());
        for (int x = x0 + 3 - rand.nextInt(4); x < trainEnd - 5; x += 8) {
            boolean lit = rand.chance(0.75);
            s.fill(x, y0 + 30, x + 5, y0 + 36, lit ? p.glow() : p.mid());
            s.fill(x, y0 + 30, x + 5, y0 + 31, lit ? p.accent() : p.dark());
        }
        s.fill(cx, y0 + 12, cx + 1, cy - 15, p.dark());
        if (!focal(s, Focal.CLOCK, cx, cy, p, emblem)) {
            Dither.line(s, cx, cy, cx, cy - 9, p.dark());
            Dither.line(s, cx, cy, cx + 6, cy, p.dark());
        }
        for (int lamp : new int[] {x0 + w * 96 / 252, x0 + w * 226 / 252}) {
            if (Math.abs(lamp - cx) < 22) {
                continue;
            }
            s.fill(lamp, y0 + 22, lamp + 1, y0 + 45, p.dark());
            Dither.discDithered(s, lamp, y0 + 21, 6, p.glow(), 5);
            s.fill(lamp - 1, y0 + 19, lamp + 2, y0 + 22, p.glow());
            s.fill(lamp - 2, y0 + 18, lamp + 3, y0 + 19, p.dark());
        }
        int bench = x0 + w * 196 / 252;
        if (Math.abs(bench - cx) > 24) {
            s.fill(bench, y0 + 40, bench + 14, y0 + 41, p.dark());
            s.fill(bench, y0 + 36, bench + 14, y0 + 37, p.dark());
            s.fill(bench + 1, y0 + 41, bench + 2, y0 + 45, p.dark());
            s.fill(bench + 12, y0 + 41, bench + 13, y0 + 45, p.dark());
        }
        s.fill(x0, y0 + 45, x0 + w, y0 + 46, p.light());
        s.fill(x0, y0 + 46, x0 + w, y0 + HEIGHT, p.mid());
        Dither.dashes(s, x0, x0 + w, y0 + 47, 2, 2, p.accent(), 0);
        Dither.rect(s, x0, y0 + 48, x0 + w, y0 + HEIGHT, p.dark(), 6);
    }

    /** 夜海 — moonlit sea, the moon's road on the water, a lighthouse on its rock throwing its beam. */
    static void sea(PixelSink s, int x0, int y0, int w, Palette p, DecorRandom rand, String[] emblem) {
        nightSky(s, x0, y0, w, p, 34, 14);
        stars(s, x0, y0, w, p, rand, 24, 26);
        int mx = x0 + w * 176 / 252;
        int my = y0 + 18;
        int lx = x0 + w * 40 / 252;
        for (int y = 8; y < 26; y++) {
            int spread = (y - 8) / 2;
            int reach = lx - 4 - x0;
            Dither.rect(s, x0, y0 + 16 - spread / 2 + (y - 8) / 3, x0 + reach, y0 + 17 + (y - 8) / 3, p.glow(), 2);
        }
        focal(s, Focal.MOON, mx, my, p, emblem);
        s.fill(x0, y0 + 34, x0 + w, y0 + HEIGHT, p.mid());
        Dither.rect(s, x0, y0 + 34, x0 + w, y0 + HEIGHT, p.dark(), 7);
        for (int y = 36; y < HEIGHT; y += 3) {
            int on = 2 + (y - 36) / 5;
            Dither.dashes(s, x0, x0 + w, y0 + y, on, 5 + rand.nextInt(4), p.light(), rand.nextInt(9));
        }
        for (int y = 35; y < HEIGHT; y += 2) {
            int half = 3 + (y - 35) / 3;
            int wobble = (int) Math.round(Math.sin(y * 1.7) * 2);
            Dither.rect(s, mx - half + wobble, y0 + y, mx + half + wobble, y0 + y + 1, p.glow(), 11 - (y - 35) / 3);
        }
        for (int dx = -14; dx <= 16; dx++) {
            int top = 41 + (int) Math.round(Math.abs(dx) * 0.45 + Math.sin(dx * 0.9) * 1.2);
            s.fill(lx + dx, y0 + top, lx + dx + 1, y0 + HEIGHT, p.dark());
        }
        for (int y = 16; y < 42; y++) {
            int half = 2 + (y - 16) / 8;
            boolean band = ((y - 16) / 5 & 1) == 1;
            s.fill(lx - half, y0 + y, lx + half + 1, y0 + y + 1, band ? p.light() : p.dark());
        }
        s.fill(lx - 3, y0 + 15, lx + 4, y0 + 16, p.dark());
        s.fill(lx - 2, y0 + 11, lx + 3, y0 + 15, p.glow());
        s.set(lx, y0 + 12, p.accent());
        s.fill(lx - 3, y0 + 10, lx + 4, y0 + 11, p.dark());
        s.fill(lx - 2, y0 + 8, lx + 3, y0 + 10, p.dark());
        s.set(lx, y0 + 7, p.dark());
    }

    /** 礼拜堂 — stone nave, lancet windows, light falling from the rose window that carries the emblem, pews
     * and candle stands. */
    static void chapel(PixelSink s, int x0, int y0, int w, Palette p, DecorRandom rand, String[] emblem) {
        s.fill(x0, y0, x0 + w, y0 + HEIGHT, p.dark());
        Dither.rect(s, x0, y0, x0 + w, y0 + HEIGHT, p.mid(), 4);
        int cx = x0 + w / 2;
        int cy = y0 + 20;
        for (int y = cy; y < y0 + 52; y++) {
            int half = 8 + (y - cy) * 2 / 3;
            Dither.rect(s, cx - half, y, cx + half + 1, y + 1, p.glow(), 3);
        }
        for (int side = -1; side <= 1; side += 2) {
            for (int k = 0; ; k++) {
                int colX = cx + side * (28 + k * 34);
                if (colX < x0 - 4 || colX > x0 + w + 4) {
                    break;
                }
                s.fill(colX - 1, y0 + 4, colX + 2, y0 + 50, p.mid());
                s.set(colX - 1, y0 + 4, p.light());
                s.fill(colX - 1, y0 + 5, colX, y0 + 50, p.light());
                s.fill(colX - 3, y0 + 3, colX + 4, y0 + 5, p.mid());
                int winX = colX + side * 11 - 4;
                if (winX > x0 - 8 && winX < x0 + w) {
                    lancet(s, winX - 1, y0 + 9, 11, y0 + 37, p.mid());
                    lancet(s, winX, y0 + 10, 9, y0 + 36, p.glow());
                    for (int y = y0 + 14; y < y0 + 36; y += 4) {
                        s.fill(winX, y, winX + 9, y + 1, p.dark());
                    }
                    s.fill(winX + 4, y0 + 12, winX + 5, y0 + 36, p.dark());
                    for (int pane = 0; pane < 2; pane++) {
                        int px = winX + (rand.chance(0.5) ? 1 : 5);
                        int py = y0 + 15 + rand.nextInt(5) * 4;
                        s.fill(px, py, px + 3, py + 3, p.accent());
                    }
                }
            }
        }
        for (int k = 0; k < 14; k++) {
            int dy = 16 + rand.nextInt(30);
            int half = 8 + dy * 2 / 3;
            int dx = rand.nextInt(2 * half + 1) - half;
            if (!near(cx + dx, cy + dy, cx, cy, 18)) {
                s.set(cx + dx, cy + dy, p.glow());
            }
        }
        if (!focal(s, Focal.ROSE, cx, cy, p, emblem)) {
            for (int k = 0; k < 8; k++) {
                double a = k * Math.PI / 4;
                Dither.line(s, cx, cy, cx + Math.cos(a) * DISC, cy + Math.sin(a) * DISC, p.dark());
            }
            Dither.disc(s, cx, cy, 3, p.accent());
        }
        for (int row = 0; row < 3; row++) {
            int py = y0 + 42 + row * 4;
            s.fill(x0, py, cx - 8, py + 2, p.dark());
            s.fill(cx + 9, py, x0 + w, py + 2, p.dark());
            s.fill(x0, py, cx - 8, py + 1, p.mid());
            s.fill(cx + 9, py, x0 + w, py + 1, p.mid());
        }
        for (int side = -1; side <= 1; side += 2) {
            int sx = cx + side * 21;
            s.fill(sx, y0 + 34, sx + 1, y0 + 42, p.light());
            s.fill(sx - 3, y0 + 34, sx + 4, y0 + 35, p.light());
            for (int c = -3; c <= 3; c += 3) {
                s.fill(sx + c, y0 + 32, sx + c + 1, y0 + 34, p.paper());
                s.set(sx + c, y0 + 31, p.accent());
            }
            Dither.discDithered(s, sx, y0 + 31, 5, p.glow(), 3);
        }
        s.fill(x0, y0 + 54, x0 + w, y0 + HEIGHT, p.dark());
    }

    /** 墓园 — headstones and crosses in the mist, an iron fence, a crooked tree with a crow, the moon. */
    static void graveyard(PixelSink s, int x0, int y0, int w, Palette p, DecorRandom rand, String[] emblem) {
        nightSky(s, x0, y0, w, p, 40, 15);
        stars(s, x0, y0, w, p, rand, 20, 24);
        int mx = x0 + w * 74 / 252;
        int my = y0 + 19;
        focal(s, Focal.MOON, mx, my, p, emblem);
        ridge(s, x0, y0, w, 42, 5, rand.next() * 6, p.mid());
        int tx = x0 + w * 216 / 252;
        tree(s, tx, y0 + 46, 15, -Math.PI / 2 - 0.25, 4, p.dark(), rand);
        Dither.bitmap(s, tx - 9, y0 + 24, p.dark(), CROW);
        int gx = x0 + 4 + rand.nextInt(6);
        int row = 0;
        while (gx < x0 + w - 4) {
            boolean back = (row++ & 1) == 1;
            int base = y0 + (back ? 43 : 48);
            int col = back ? p.mid() : p.dark();
            if (back) {
                Dither.rect(s, gx - 1, base - 9, gx + 7, base, p.dark(), 10);
            }
            switch (rand.nextInt(3)) {
                case 0 -> {
                    Ornaments.roundedFill(s, gx, base - 10, 7, 10, col);
                    s.fill(gx + 2, base - 7, gx + 5, base - 6, p.light());
                }
                case 1 -> {
                    s.fill(gx + 3, base - 12, gx + 5, base, col);
                    s.fill(gx, base - 9, gx + 8, base - 7, col);
                }
                default -> {
                    s.fill(gx + 1, base - 9, gx + 6, base, col);
                    s.fill(gx + 2, base - 11, gx + 5, base - 9, col);
                    s.set(gx + 3, base - 12, col);
                }
            }
            gx += 12 + rand.nextInt(14);
        }
        Dither.rect(s, x0, y0 + 40, x0 + w, y0 + 48, p.light(), 4);
        s.fill(x0, y0 + 48, x0 + w, y0 + HEIGHT, p.dark());
        for (int x = 1; x < w; x += 4) {
            s.fill(x0 + x, y0 + 46, x0 + x + 1, y0 + HEIGHT, p.dark());
            s.set(x0 + x, y0 + 45, p.dark());
            s.set(x0 + x - 1, y0 + 46, p.dark());
            s.set(x0 + x + 1, y0 + 46, p.dark());
        }
        s.fill(x0, y0 + 49, x0 + w, y0 + 50, p.dark());
    }

    /** 书房 — bookshelves with flasks among the books, a round window bearing the emblem, a desk with a candle,
     * an inkwell and papers. */
    static void study(PixelSink s, int x0, int y0, int w, Palette p, DecorRandom rand, String[] emblem) {
        s.fill(x0, y0, x0 + w, y0 + HEIGHT, p.mid());
        for (int x = 0; x < w; x += 6) {
            Dither.rect(s, x0 + x, y0, x0 + x + 2, y0 + HEIGHT, p.dark(), 3);
        }
        int cx = x0 + w / 2;
        int cy = y0 + 21;
        int[] spines = {p.dark(), p.light(), p.paper(), p.accent(), p.dark(), p.glow()};
        for (int side = -1; side <= 1; side += 2) {
            int from = side < 0 ? x0 : cx + 24;
            int to = side < 0 ? cx - 24 : x0 + w;
            if (to - from < 8) {
                continue;
            }
            s.fill(from, y0, to, y0 + 42, p.dark());
            for (int shelf = 0; shelf < 3; shelf++) {
                int floor = y0 + 13 + shelf * 13;
                int bx = from + 2;
                while (bx < to - 3) {
                    if (rand.chance(0.12)) {
                        Dither.disc(s, bx + 2, floor - 3, 2, p.glow());
                        s.fill(bx + 1, floor - 3, bx + 4, floor - 1, p.accent());
                        s.fill(bx + 2, floor - 7, bx + 3, floor - 4, p.light());
                        bx += 6;
                        continue;
                    }
                    int bw = 2 + rand.nextInt(2);
                    int bh = 7 + rand.nextInt(4);
                    s.fill(bx, floor - bh, bx + bw, floor, spines[rand.nextInt(spines.length)]);
                    bx += bw + (rand.chance(0.15) ? 2 : 0);
                }
                s.fill(from, floor, to, floor + 2, p.mid());
            }
        }
        if (!focal(s, Focal.WINDOW, cx, cy, p, emblem)) {
            s.fill(cx - DISC, cy, cx + DISC + 1, cy + 1, p.mid());
            s.fill(cx, cy - DISC, cx + 1, cy + DISC + 1, p.mid());
        }
        s.fill(x0, y0 + 42, x0 + w, y0 + 44, p.light());
        s.fill(x0, y0 + 44, x0 + w, y0 + HEIGHT, p.dark());
        s.fill(cx - 22, y0 + 39, cx - 12, y0 + 42, p.paper());
        s.fill(cx - 21, y0 + 38, cx - 13, y0 + 39, p.paper());
        s.fill(cx + 13, y0 + 38, cx + 17, y0 + 42, p.dark());
        Dither.line(s, cx + 15, y0 + 38, cx + 20, y0 + 31, p.light());
        s.fill(cx - 8, y0 + 35, cx - 6, y0 + 42, p.paper());
        s.set(cx - 7, y0 + 34, p.accent());
        Dither.discDithered(s, cx - 7, y0 + 34, 6, p.glow(), 4);
    }

    /** 屋顶 — a far row of town houses and a spire, then a near slate roof with chimney pots breathing smoke, a lit
     * dormer and a washing line, all under a big moon. */
    static void rooftops(PixelSink s, int x0, int y0, int w, Palette p, DecorRandom rand, String[] emblem) {
        nightSky(s, x0, y0, w, p, 42, 15);
        stars(s, x0, y0, w, p, rand, 28, 30);
        int mx = x0 + w * 170 / 252;
        int my = y0 + 18;
        focal(s, Focal.MOON, mx, my, p, emblem);
        int spire = x0 + w * 96 / 252 + rand.nextInt(10);
        int hx = x0 - 4;
        while (hx < x0 + w) {
            int hw = 10 + rand.nextInt(10);
            int top = y0 + 30 + rand.nextInt(8);
            if (Math.abs(hx + hw / 2 - mx) < DISC + 4) {
                top = Math.max(top, my + DISC + 3);
            }
            s.fill(hx, top, hx + hw, y0 + HEIGHT, p.mid());
            for (int k = 0; k <= hw / 2; k++) {
                s.fill(hx + k, top - k / 2 - 1, hx + hw - k, top - k / 2, p.mid());
            }
            if (rand.chance(0.6)) {
                s.set(hx + 2 + rand.nextInt(Math.max(1, hw - 4)), top + 3, p.glow());
            }
            hx += hw;
        }
        if (Math.abs(spire - mx) > DISC + 6) {
            for (int y = 0; y < 22; y++) {
                int half = y / 7;
                s.fill(spire - half, y0 + 12 + y, spire + half + 1, y0 + 13 + y, p.mid());
            }
            s.fill(spire - 3, y0 + 34, spire + 4, y0 + HEIGHT, p.mid());
        }
        int ridgeY = y0 + 40;
        for (int x = 0; x < w; x++) {
            int top = ridgeY + (int) Math.round(Math.sin((x0 + x) * 0.02) * 2);
            s.fill(x0 + x, top, x0 + x + 1, y0 + HEIGHT, p.dark());
            s.set(x0 + x, top, p.mid());
        }
        for (int y = 44; y < HEIGHT; y += 3) {
            Dither.dashes(s, x0, x0 + w, y0 + y, 5, 1, p.mid(), y * 3);
        }
        int[] stacks = {x0 + w * 24 / 252 + rand.nextInt(6), x0 + w * 92 / 252 + rand.nextInt(6),
                x0 + w * 226 / 252 + rand.nextInt(6)};
        for (int chx : stacks) {
            if (Math.abs(chx - mx) < DISC + 8 || chx > x0 + w - 8) {
                continue;
            }
            s.fill(chx, ridgeY - 12, chx + 7, ridgeY + 2, p.dark());
            s.fill(chx - 1, ridgeY - 13, chx + 8, ridgeY - 11, p.dark());
            s.fill(chx + 1, ridgeY - 15, chx + 3, ridgeY - 13, p.dark());
            s.fill(chx + 4, ridgeY - 15, chx + 6, ridgeY - 13, p.dark());
            for (int puff = 0; puff < 4; puff++) {
                int px = chx + 3 + puff * 4 + puff * puff;
                int py = ridgeY - 18 - puff * 4;
                if (!near(px, py, mx, my, DISC + 6)) {
                    Dither.discDithered(s, px, py, 2 + puff, p.light(), 9 - puff * 2);
                }
            }
        }
        int dormer = x0 + w * 132 / 252;
        if (Math.abs(dormer + 5 - mx) > DISC + 8) {
            s.fill(dormer, ridgeY - 7, dormer + 11, ridgeY + 3, p.dark());
            for (int k = 0; k < 6; k++) {
                s.fill(dormer - 1 + k, ridgeY - 8 - k, dormer + 12 - k, ridgeY - 7 - k, p.dark());
            }
            s.fill(dormer + 3, ridgeY - 5, dormer + 8, ridgeY + 1, p.glow());
            s.fill(dormer + 5, ridgeY - 5, dormer + 6, ridgeY + 1, p.dark());
            s.fill(dormer + 3, ridgeY - 3, dormer + 8, ridgeY - 2, p.dark());
        }
        int lineA = stacks[0] + 7;
        int lineB = stacks[1];
        if (lineB - lineA > 16) {
            for (int x = lineA; x <= lineB; x++) {
                double t = (x - lineA) / (double) (lineB - lineA);
                int ly = ridgeY - 9 + (int) Math.round(Math.sin(t * Math.PI) * 4);
                s.set(x, ly, p.light());
                if ((x - lineA) % 8 == 3 && x + 4 < lineB) {
                    s.fill(x, ly + 1, x + 4, ly + 5 + ((x - lineA) / 8 & 1), ((x - lineA) / 8 % 3 == 1) ? p.accent()
                            : p.paper());
                }
            }
        }
    }

    /** 林地 — layered pines in the mist, fireflies, the moon low over the trees. */
    static void forest(PixelSink s, int x0, int y0, int w, Palette p, DecorRandom rand, String[] emblem) {
        nightSky(s, x0, y0, w, p, 36, 14);
        stars(s, x0, y0, w, p, rand, 24, 24);
        int mx = x0 + w * 62 / 252;
        int my = y0 + 18;
        focal(s, Focal.MOON, mx, my, p, emblem);
        for (int x = x0 + rand.nextInt(6); x < x0 + w + 6; x += 6 + rand.nextInt(6)) {
            int h = 12 + rand.nextInt(10);
            if (Math.abs(x - mx) < DISC + 4) {
                h = Math.min(h, 44 - (my + DISC + 2 - y0));
            }
            pine(s, x, y0 + 44, h, p.light());
        }
        Dither.rect(s, x0, y0 + 34, x0 + w, y0 + 46, p.light(), 5);
        for (int x = x0 + rand.nextInt(10); x < x0 + w + 10; x += 13 + rand.nextInt(12)) {
            int h = 22 + rand.nextInt(16);
            if (Math.abs(x - mx) < DISC + 10) {
                h = Math.min(h, HEIGHT - (my + DISC + 3 - y0));
            }
            pine(s, x, y0 + HEIGHT, h, p.dark());
        }
        for (int f = 0; f < 16; f++) {
            int fx = x0 + rand.nextInt(w);
            int fy = y0 + 34 + rand.nextInt(18);
            s.set(fx, fy, p.accent());
            if (rand.chance(0.4)) {
                s.set(fx, fy - 1, p.glow());
            }
        }
    }

    /** 剧场 — velvet curtains tied back, a scalloped valance, footlights and the spotlight on the emblem. */
    static void stage(PixelSink s, int x0, int y0, int w, Palette p, DecorRandom rand, String[] emblem) {
        s.fill(x0, y0, x0 + w, y0 + HEIGHT, p.dark());
        Dither.rect(s, x0, y0, x0 + w, y0 + 46, p.mid(), 3);
        int cx = x0 + w / 2;
        int cy = y0 + 25;
        for (int y = 0; y < 46; y++) {
            int half = 4 + y * 2 / 5;
            Dither.rect(s, cx - half, y0 + y, cx + half + 1, y0 + y + 1, p.glow(), 2);
        }
        if (!focal(s, Focal.SPOT, cx, cy, p, emblem)) {
            Dither.disc(s, cx, cy, 4, p.accent());
        }
        int curtain = Math.max(26, w * 44 / 252);
        for (int side = -1; side <= 1; side += 2) {
            for (int y = 5; y < 47; y++) {
                // Full width under the valance, gathered at the tie-back, flaring again to the floor.
                // 帷幔下全宽，系带处收拢，落地处再散开。
                double t = y < 30 ? Math.pow((30 - y) / 25.0, 1.4) : Math.pow((y - 30) / 17.0, 1.2) * 0.7;
                int reach = (int) Math.round(curtain * (0.42 + 0.58 * t));
                for (int k = 0; k < reach; k++) {
                    int x = side < 0 ? x0 + k : x0 + w - 1 - k;
                    // Four folds squeezed with the curtain: lit face, shadow, lit, shadow; a highlight on each crest.
                    // 四道随帘收拢的褶：亮面、暗面交替，每道褶脊一条高光。
                    int fold = k * 4 / Math.max(1, reach);
                    int within = k * 4 % Math.max(1, reach);
                    int col = (fold & 1) == 0 ? p.mid() : p.dark();
                    if ((fold & 1) == 0 && within < 4) {
                        col = p.accent();
                    }
                    s.set(x, y0 + y, col);
                }
            }
            int tieReach = (int) Math.round(curtain * 0.42);
            int tie = side < 0 ? x0 + tieReach - 3 : x0 + w - tieReach;
            s.fill(tie, y0 + 29, tie + 3, y0 + 32, p.light());
            s.set(tie + 1, y0 + 32, p.light());
        }
        s.fill(x0, y0, x0 + w, y0 + 5, p.dark());
        for (int x = 0; x < w; x += 12) {
            for (int dx = 0; dx < 12; dx++) {
                double t = (dx - 5.5) / 6.0;
                int depth = (int) Math.round(4 * Math.sqrt(Math.max(0, 1 - t * t)));
                s.fill(x0 + x + dx, y0 + 5, x0 + x + dx + 1, y0 + 5 + depth, p.dark());
                s.set(x0 + x + dx, y0 + 4 + depth, p.deep());
            }
            s.fill(x0 + x + 5, y0 + 9, x0 + x + 7, y0 + 11, p.accent());
        }
        s.fill(x0, y0 + 2, x0 + w, y0 + 3, p.accent());
        s.fill(x0, y0 + 46, x0 + w, y0 + 47, p.light());
        s.fill(x0, y0 + 47, x0 + w, y0 + HEIGHT, p.mid());
        for (int y = 49; y < HEIGHT; y += 3) {
            s.fill(x0, y0 + y, x0 + w, y0 + y + 1, p.dark());
        }
        for (int x = 5 + rand.nextInt(4); x < w; x += 10) {
            s.fill(x0 + x, y0 + 45, x0 + x + 2, y0 + 46, p.glow());
            Dither.rect(s, x0 + x - 1, y0 + 41, x0 + x + 3, y0 + 45, p.glow(), 4);
        }
    }

    /** 机械间 — gears, pipes with a valve wheel, a pressure gauge and a hiss of steam around a great gear. */
    static void workshop(PixelSink s, int x0, int y0, int w, Palette p, DecorRandom rand, String[] emblem) {
        s.fill(x0, y0, x0 + w, y0 + HEIGHT, p.dark());
        Dither.rect(s, x0, y0, x0 + w, y0 + HEIGHT, p.mid(), 3);
        int cx = x0 + w * 140 / 252;
        int cy = y0 + 26;
        gear(s, x0 + w * 30 / 252 + rand.nextInt(6), y0 + 38 + rand.nextInt(5), 15, 12, p.mid(), p.dark());
        gear(s, x0 + w * 76 / 252 + rand.nextInt(6), y0 + 15 + rand.nextInt(5), 9, 9, p.mid(), p.dark());
        gear(s, x0 + w * 222 / 252 - rand.nextInt(6), y0 + 10 + rand.nextInt(5), 12, 10, p.mid(), p.dark());
        gear(s, x0 + w * 200 / 252 - rand.nextInt(6), y0 + 45 + rand.nextInt(3), 8, 8, p.light(), p.dark());
        s.fill(x0, y0 + 5, x0 + w, y0 + 8, p.light());
        s.fill(x0, y0 + 6, x0 + w, y0 + 7, p.mid());
        for (int x = 18; x < w; x += 34) {
            s.fill(x0 + x, y0 + 4, x0 + x + 2, y0 + 9, p.light());
        }
        int pipeX = x0 + w * 104 / 252;
        s.fill(pipeX, y0 + 8, pipeX + 3, y0 + 50, p.light());
        s.fill(pipeX + 1, y0 + 8, pipeX + 2, y0 + 50, p.mid());
        Dither.ring(s, pipeX + 1, y0 + 30, 4, p.accent());
        s.fill(pipeX - 3, y0 + 30, pipeX + 6, y0 + 31, p.accent());
        int gaugeX = x0 + w * 176 / 252;
        if (gaugeX - cx > 20) {
            Dither.disc(s, gaugeX, y0 + 34, 4, p.paper());
            Dither.ring(s, gaugeX, y0 + 34, 4, p.light());
            Dither.line(s, gaugeX, y0 + 34, gaugeX + 2, y0 + 32, p.deep());
            s.fill(gaugeX, y0 + 38, gaugeX + 1, y0 + 50, p.light());
        }
        for (int puff = 0; puff < 4; puff++) {
            Dither.discDithered(s, pipeX + 6 + puff * 4, y0 + 18 - puff * 3, 2 + puff / 2, p.light(), 7 - puff);
        }
        if (!focal(s, Focal.GEAR, cx, cy, p, emblem)) {
            Dither.disc(s, cx, cy, 4, p.dark());
        }
        s.fill(x0, y0 + 50, x0 + w, y0 + HEIGHT, p.dark());
        for (int x = 1; x < w; x += 3) {
            s.set(x0 + x, y0 + 52, p.mid());
            s.set(x0 + x + 1, y0 + 54, p.mid());
        }
    }
}
