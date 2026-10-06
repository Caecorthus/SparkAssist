package dev.caecorthus.sparkassist.guidebook.decor;

import static dev.caecorthus.sparkassist.guidebook.decor.DecorPalette.BELL;
import static dev.caecorthus.sparkassist.guidebook.decor.DecorPalette.BELL_HI;
import static dev.caecorthus.sparkassist.guidebook.decor.DecorPalette.DEAD;
import static dev.caecorthus.sparkassist.guidebook.decor.DecorPalette.DEAD_HI;
import static dev.caecorthus.sparkassist.guidebook.decor.DecorPalette.LEAF;
import static dev.caecorthus.sparkassist.guidebook.decor.DecorPalette.LEAF_HI;
import static dev.caecorthus.sparkassist.guidebook.decor.DecorPalette.RIBBON_A;
import static dev.caecorthus.sparkassist.guidebook.decor.DecorPalette.RIBBON_B;
import static dev.caecorthus.sparkassist.guidebook.decor.DecorPalette.STEM;
import static dev.caecorthus.sparkassist.guidebook.decor.DecorPalette.STEM_DARK;
import static dev.caecorthus.sparkassist.guidebook.decor.DecorPalette.THORN;
import static dev.caecorthus.sparkassist.guidebook.decor.DecorPalette.WIRE;

import java.util.ArrayList;
import java.util.List;

/**
 * Faction foliage grown pixel by pixel along the frames: a stem wanders forward with a little wobble, is pulled
 * toward its rim's direction, puts out leaves on alternating sides at a fixed pitch and sometimes a branch. Each
 * motif has its own leaf, extras (thorns, barbed wire, bells, berries) and colours; the sink's {@link Bounds}
 * both clip the pixels and steer the stem back when it drifts out of its strip.
 * 沿边框逐像素生长的阵营枝叶：主茎带着轻微摆动前进，被拉向边条方向，按固定节距左右交替出叶，偶尔分叉。每种植物有
 * 自己的叶形、附件（刺、铁丝、铃铛、浆果）与颜色；落笔面的 Bounds 既裁剪像素，也在主茎漂出边条时把它拉回来。
 */
public final class Foliage {
    /** Where a stem may go; usually {@code ZonedSink::allowed}, so forbidden boxes hide pixels but do not stop
     * the stem. 主茎可去之处，通常是 ZonedSink::allowed：禁区只隐藏像素，不阻断主茎。 */
    @FunctionalInterface
    public interface Bounds {
        boolean inside(int x, int y);
    }

    private enum Leaf { NARROW, LOBED, HEART, NONE }

    private record Motif(Leaf leaf, int pitch, double wobble, double droop, double branch, boolean thorns,
                         boolean barbed, boolean bells, boolean ribbon, boolean deadWood, double berries,
                         int leafLength) {
    }

    private static final Motif BRIAR = new Motif(Leaf.LOBED, 6, 0.45, 0.0, 0.35, true, false, false, false, false,
            0.18, 0);
    private static final Motif WILLOW = new Motif(Leaf.NARROW, 4, 0.25, 0.05, 0.3, false, false, false, false,
            false, 0.0, 4);
    private static final Motif DEAD_BRANCH = new Motif(Leaf.NONE, 9, 0.6, 0.03, 0.5, false, true, false, false,
            true, 0.1, 0);
    private static final Motif RIBBON_VINE = new Motif(Leaf.HEART, 8, 0.5, 0.0, 0.25, false, false, true, true,
            false, 0.0, 0);
    private static final Motif IVY = new Motif(Leaf.HEART, 6, 0.4, 0.02, 0.3, false, false, false, false, false,
            0.06, 0);

    /**
     * One vine to grow. {@code pull} is the heading the stem is drawn toward (NaN for none) with strength
     * {@code pullStrength}; {@code wobble} and {@code branch} override the motif's own when non-negative.
     * 一根要生长的藤：pull 为主茎被拉向的方向（NaN 为无），强度 pullStrength；wobble 与 branch 非负时覆盖植物默认值。
     *
     * @param berry ARGB berry colour, 0 for none / 浆果色，0 为无
     */
    public record Vine(DecorSet.Foliage motif, double x, double y, double angle, int length, double pull,
                       double pullStrength, double wobble, double branch, int berry, boolean rail) {
        /**
         * A calm vine hugging a rim: low wobble, few branches, and a rail through its start point along
         * {@code along} that the stem keeps steering back to, so it never leaves its strip.
         * 贴边的安静藤：小摆动、少分叉，并以起点沿 along 方向为轨，主茎不断被拉回轨上，不会离开边条。
         */
        public static Vine alongRim(DecorSet.Foliage motif, double x, double y, double along, int length, int berry) {
            return new Vine(motif, x, y, along, length, along, 0.3, 0.2, 0.12, berry, true);
        }

        /** A short free twig with a gentle wobble and few branches, e.g. in a title band. 平缓少分叉的自由小枝。 */
        public static Vine twig(DecorSet.Foliage motif, double x, double y, double angle, int length, int berry) {
            return new Vine(motif, x, y, angle, length, Double.NaN, 0.03, 0.35, 0.25, berry, false);
        }
    }

    private Foliage() {
    }

    public static void grow(PixelSink sink, Bounds bounds, Vine vine, DecorRandom rand) {
        grow(sink, bounds, vine, rand, 0, 1);
    }

    private static void grow(PixelSink s, Bounds bounds, Vine v, DecorRandom rand, int depth, int branchSide) {
        grow(s, bounds, v, rand, depth, branchSide, v.x(), v.y());
    }

    private static void grow(PixelSink s, Bounds bounds, Vine v, DecorRandom rand, int depth, int branchSide,
                             double startX, double startY) {
        Motif m = motif(v.motif());
        double wobble = v.wobble() >= 0 ? v.wobble() : m.wobble();
        double branchChance = v.branch() >= 0 ? v.branch() : m.branch();
        double bend = v.rail() ? 0 : (rand.next() - 0.5) * 0.08;
        double x = startX;
        double y = startY;
        double a = v.angle();
        double phase = rand.next() * 6.28;
        int side = rand.chance(0.5) ? 1 : -1;
        int outside = 0;
        // Rail: the line through the start point along the pull heading; the stem steers toward it.
        // 轨：过起点、沿拉力方向的直线；主茎向它靠拢。
        double railNx = -Math.sin(v.pull());
        double railNy = Math.cos(v.pull());
        List<Vine> branches = new ArrayList<>();
        List<Double> forkX = new ArrayList<>();
        List<Double> forkY = new ArrayList<>();
        for (int i = 0; i < v.length(); i++) {
            a += bend + wobble * (Math.sin(phase + i * 0.22) * 0.22 + (rand.next() - 0.5) * 0.25)
                    + m.droop() * Math.cos(a);
            if (v.rail()) {
                double offset = (v.x() - x) * railNx + (v.y() - y) * railNy;
                double target = v.pull() + Math.max(-0.7, Math.min(0.7, offset * 0.2));
                a += norm(target - a) * v.pullStrength();
            } else if (!Double.isNaN(v.pull())) {
                a += norm(v.pull() - a) * v.pullStrength();
            }
            int rx = (int) Math.round(x);
            int ry = (int) Math.round(y);
            if (!bounds.inside(rx, ry)) {
                outside++;
                if (outside > 14) {
                    break;
                }
                a += 0.35 * side;
                x += Math.cos(a);
                y += Math.sin(a);
                continue;
            }
            outside = 0;
            int stemColour = m.deadWood() ? DEAD : STEM;
            int stemShade = m.deadWood() ? DEAD_HI : STEM_DARK;
            int nx = rx + (int) Math.round(-Math.sin(a));
            int ny = ry + (int) Math.round(Math.cos(a));
            if (m.ribbon()) {
                int ribbon = ((i / 3) & 1) == 1 ? RIBBON_A : RIBBON_B;
                s.set(rx, ry, ribbon);
                s.set(nx, ny, ribbon);
            } else {
                s.set(rx, ry, stemColour);
                if (depth == 0 && i < v.length() * 0.45) {
                    s.set(nx, ny, stemShade);
                }
            }
            double n = a + side * Math.PI / 2;
            if (i > 3 && i % m.pitch() == 0) {
                side = -side;
                double la = m.leaf() == Leaf.NARROW ? n + norm(Math.PI / 2 - n) * 0.45 : n;
                double ux = Math.cos(la);
                double uy = Math.sin(la);
                switch (m.leaf()) {
                    case NARROW -> {
                        for (int d = 1; d <= m.leafLength(); d++) {
                            dot(s, rx + ux * d, ry + uy * d, d == m.leafLength() ? LEAF_HI : LEAF);
                        }
                    }
                    case LOBED, HEART -> {
                        double cx = rx + ux * 2.2;
                        double cy = ry + uy * 2.2;
                        String[] rows = m.leaf() == Leaf.HEART ? new String[] {"#.#", "###", ".#."}
                                : new String[] {".#.", "###", "##."};
                        for (int r = 0; r < 3; r++) {
                            for (int k = 0; k < 3; k++) {
                                if (rows[r].charAt(k) == '#') {
                                    boolean lit = (k == 1 && r == 0) || (k == 0 && r == 1);
                                    dot(s, cx + k - 1, cy + r - 1, lit ? LEAF_HI : LEAF);
                                }
                            }
                        }
                        dot(s, rx + ux, ry + uy, STEM_DARK);
                        if (v.berry() != 0 && rand.chance(m.berries())) {
                            berry(s, (int) Math.round(rx - ux * 2), (int) Math.round(ry - uy * 2), v.berry(), 0.4);
                        }
                    }
                    case NONE -> {
                        if (v.berry() != 0 && rand.chance(m.berries())) {
                            berry(s, (int) Math.round(rx + ux * 2), (int) Math.round(ry + uy * 2), v.berry(), 0.3);
                        }
                    }
                }
                if (m.bells() && i % 16 == 0 && i > 6) {
                    double bx = rx + ux * 2;
                    double by = ry + uy * 2;
                    dot(s, rx + ux, ry + uy, STEM_DARK);
                    for (int dx = -1; dx <= 1; dx++) {
                        dot(s, bx + dx, by, BELL);
                        dot(s, bx + dx, by + 1, BELL);
                    }
                    dot(s, bx, by - 1, BELL);
                    dot(s, bx - 1, by, BELL_HI);
                    dot(s, bx, by + 2, STEM_DARK);
                }
            }
            if (m.thorns() && i % 8 == 2) {
                double t = a + (i % 16 < 8 ? 1 : -1) * Math.PI / 2;
                dot(s, rx + Math.cos(t), ry + Math.sin(t), THORN);
            }
            if (m.barbed() && i % 11 == 3) {
                s.set(rx + 1, ry + 1, WIRE);
                s.set(rx - 1, ry - 1, WIRE);
                s.set(rx + 1, ry - 1, WIRE);
                s.set(rx - 1, ry + 1, WIRE);
            }
            boolean firstFork = i == (int) Math.floor(v.length() * 0.4);
            boolean secondFork = i == (int) Math.floor(v.length() * 0.68);
            if (depth < 2 && (firstFork || secondFork) && rand.chance(branchChance)) {
                int sign = (firstFork ? 1 : -1) * branchSide;
                // A branch of a rail vine keeps the parent's rail (its own start would be off the strip's centre).
                // 轨藤的分枝沿用母藤的轨（分枝起点并不在边条中线上）。
                branches.add(new Vine(v.motif(), v.rail() ? v.x() : x, v.rail() ? v.y() : y,
                        a + sign * (0.55 + rand.next() * 0.5),
                        (int) Math.floor(v.length() * (0.45 + rand.next() * 0.15)), v.pull(), v.pullStrength(),
                        v.wobble(), v.branch(), v.berry(), v.rail()));
                forkX.add(x);
                forkY.add(y);
            }
            x += Math.cos(a);
            y += Math.sin(a);
        }
        for (int k = 0; k < branches.size(); k++) {
            grow(s, bounds, branches.get(k), rand, depth + 1, branchSide, forkX.get(k), forkY.get(k));
        }
    }

    private static void berry(PixelSink s, int bx, int by, int colour, double glint) {
        s.fill(bx, by, bx + 2, by + 2, colour);
        s.set(bx, by, DecorPalette.mix(colour, 0xFFFFFFFF, glint));
    }

    private static void dot(PixelSink s, double x, double y, int colour) {
        s.set((int) Math.round(x), (int) Math.round(y), colour);
    }

    private static Motif motif(DecorSet.Foliage kind) {
        return switch (kind) {
            case BRIAR -> BRIAR;
            case WILLOW -> WILLOW;
            case DEAD -> DEAD_BRANCH;
            case RIBBON -> RIBBON_VINE;
            case IVY -> IVY;
        };
    }

    private static double norm(double angle) {
        while (angle > Math.PI) {
            angle -= 2 * Math.PI;
        }
        while (angle < -Math.PI) {
            angle += 2 * Math.PI;
        }
        return angle;
    }
}
