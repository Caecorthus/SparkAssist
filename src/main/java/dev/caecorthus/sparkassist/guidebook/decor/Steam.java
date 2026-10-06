package dev.caecorthus.sparkassist.guidebook.decor;

import static dev.caecorthus.sparkassist.guidebook.decor.DecorPalette.STEAM;
import static dev.caecorthus.sparkassist.guidebook.decor.DecorPalette.STEAM_SOFT;

/**
 * Steam curls rising from a panel's outer corner: thin 1 px paths whose curvature grows along the way, drawn in
 * two faint warm tones. The sink decides where they may land, so curls never cross a panel's content.
 * 从面板外角升起的蒸汽卷云：1 像素细线，曲率沿途增大，两阶淡暖色。落点由落笔面裁决，卷云不会越进面板内容。
 */
public final class Steam {
    private Steam() {
    }

    /**
     * {@code count} curls from around (x, y) heading {@code direction} (radians, -π/2 is up), each 14..25 px.
     * 从 (x, y) 附近向 direction 方向升起 count 条卷云，每条 14 到 25 像素。
     */
    public static void wisps(PixelSink s, int x, int y, double direction, int count, DecorRandom rand) {
        for (int k = 0; k < count; k++) {
            double sx = x + (rand.next() - 0.5) * 4;
            double sy = y + (rand.next() - 0.5) * 3;
            wisp(s, sx, sy, direction + (rand.next() - 0.5) * 0.6, 14 + rand.nextInt(12), rand);
        }
    }

    private static void wisp(PixelSink s, double x, double y, double angle, int length, DecorRandom rand) {
        double a = angle;
        int turn = rand.chance(0.5) ? 1 : -1;
        for (int i = 0; i < length; i++) {
            a += turn * (0.05 + i * 0.012) + (rand.next() - 0.5) * 0.1;
            x += Math.cos(a);
            y += Math.sin(a);
            int rx = (int) Math.round(x);
            int ry = (int) Math.round(y);
            s.set(rx, ry, (i & 1) == 1 ? STEAM : STEAM_SOFT);
            if (i > length * 0.3 && i % 3 == 0) {
                s.set(rx + 1, ry, STEAM_SOFT);
            }
        }
    }
}
