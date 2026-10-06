package dev.caecorthus.sparkassist.guidebook.decor;

import static dev.caecorthus.sparkassist.guidebook.decor.DecorPalette.INK;
import static dev.caecorthus.sparkassist.guidebook.decor.DecorPalette.INK_FAINT;
import static dev.caecorthus.sparkassist.guidebook.decor.DecorPalette.INK_MUTED;
import static dev.caecorthus.sparkassist.guidebook.decor.DecorPalette.INK_RULE;
import static dev.caecorthus.sparkassist.guidebook.decor.DecorPalette.INK_RULE_SOFT;
import static dev.caecorthus.sparkassist.guidebook.decor.DecorPalette.PAPER;

/**
 * The chapter plates: duotone scenes 56 px tall and as wide as the text column, painted in paper, three inks and
 * one accent (the set's colour mixed into the paper) with Bayer dither for every gradient. Each scene has a focal
 * disc — a moon, a sun, a clock, a rose window — and the page's emblem sits on it, so every role and trait gets its
 * own picture on a scene that suits it. Stars, trees, buildings and rain come from the page seed; the composition
 * does not. {@link #plate} also draws the thin plate frame with its corner ticks, 3 px outside the picture.
 * 扉画：高 56、宽同版心的双色景，只用纸色、三阶墨色与一个点睛色（套别色调进纸色），渐变全靠 Bayer 抖动。每幅景都有
 * 一个焦点圆盘（月亮、太阳、钟面、玫瑰窗……），页面徽记就画在上面，因此每个身份与词条都在合适的景里有自己的画。星、
 * 树、楼、雨随页种子变化，构图不变。plate() 同时在画外 3 像素处画细框与角标。
 */
public final class Plates {
    public static final int HEIGHT = 56;
    /** Frame and corner ticks reach this far outside the picture. 框与角标伸出画面的距离。 */
    public static final int MARGIN = 3;
    /** Narrowest text column that still gets a plate. 仍画扉画的最窄版心。 */
    public static final int MIN_WIDTH = 160;
    /** Radius of the clean disc the emblem sits on. 徽记所在净色圆盘的半径。 */
    public static final int DISC = 12;

    /** How a scene frames its focal disc. 景对焦点圆盘的装框方式。 */
    enum Focal { MOON, SUN, CLOCK, ROSE, GEAR, SPOT, WINDOW, PLAIN }

    /** The seven colours of a plate; {@code deep} is the accent darkened, for emblem details.
     * 一幅扉画的七种颜色；deep 是压暗的点睛色，用于徽记细节。 */
    public record Palette(int paper, int light, int mid, int dark, int accent, int glow, int deep) {
        public static Palette of(int accentRgb) {
            int accent = DecorPalette.opaque(accentRgb);
            return new Palette(PAPER, INK_FAINT, INK_MUTED, INK, DecorPalette.mix(PAPER, accent, 0.55),
                    DecorPalette.mix(PAPER, accent, 0.22), DecorPalette.mix(accent, INK, 0.3));
        }
    }

    private Plates() {
    }

    /**
     * Picture plus frame, with the picture's top-left at (x, y) and width {@code w}; the frame uses the 3 px
     * margin around it. The sink should extend {@link #MARGIN} beyond the picture on every side.
     * 画面左上角在 (x, y)、宽 w 的扉画连同画框；画框占用画面四周的 3 像素。落笔面应比画面四周各大 MARGIN。
     *
     * @param emblem the page emblem (see {@link Emblems}), or null for the scene's own centrepiece
     *               / 页面徽记（见 Emblems），null 时画景自带的主体
     */
    public static void plate(PixelSink sink, DecorSet.Plate kind, String[] emblem, int x, int y, int w,
                             int accentRgb, long seed) {
        scene(sink, kind, emblem, x, y, w, Palette.of(accentRgb), new DecorRandom(seed));
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
    public static void scene(PixelSink sink, DecorSet.Plate kind, String[] emblem, int x, int y, int w, Palette p,
                             DecorRandom rand) {
        PixelSink s = new ClipSink(sink, x, y, w, HEIGHT);
        // Half the pages see their scene from the other side, so pages sharing a scene still differ.
        // 一半页面的景左右翻转，同景的页面也各不相同。
        if (rand.chance(0.5)) {
            s = new MirrorSink(s, x, w);
        }
        switch (kind) {
            case MARSH -> PlateScenes.marsh(s, x, y, w, p, rand, emblem);
            case VIADUCT -> PlateScenes.viaduct(s, x, y, w, p, rand, emblem);
            case RAIN -> PlateScenes.rain(s, x, y, w, p, rand, emblem);
            case FAIR -> PlateScenes.fair(s, x, y, w, p, rand, emblem);
            case FIELD -> PlateScenes.field(s, x, y, w, p, rand, emblem);
            case ARCANE -> PlateScenes.arcane(s, x, y, w, p, rand, emblem);
            case CARRIAGE -> PlateScenes.carriage(s, x, y, w, p, rand, emblem);
            case STATION -> PlateScenes.station(s, x, y, w, p, rand, emblem);
            case SEA -> PlateScenes.sea(s, x, y, w, p, rand, emblem);
            case CHAPEL -> PlateScenes.chapel(s, x, y, w, p, rand, emblem);
            case GRAVEYARD -> PlateScenes.graveyard(s, x, y, w, p, rand, emblem);
            case STUDY -> PlateScenes.study(s, x, y, w, p, rand, emblem);
            case ROOFTOPS -> PlateScenes.rooftops(s, x, y, w, p, rand, emblem);
            case FOREST -> PlateScenes.forest(s, x, y, w, p, rand, emblem);
            case STAGE -> PlateScenes.stage(s, x, y, w, p, rand, emblem);
            case WORKSHOP -> PlateScenes.workshop(s, x, y, w, p, rand, emblem);
        }
    }

    // ================================================================ focal disc / 焦点圆盘

    /**
     * The focal disc at (cx, cy): the scene's frame for it, the clean disc of radius {@link #DISC}, then the emblem.
     * Returns false when there is no emblem, so the scene can draw its own centrepiece on the disc instead.
     * 位于 (cx, cy) 的焦点圆盘：先画景给它的框，再画半径 DISC 的净色圆盘，最后画徽记。没有徽记时返回 false，
     * 由景自己在圆盘上画主体。
     */
    static boolean focal(PixelSink s, Focal style, int cx, int cy, Palette p, String[] emblem) {
        switch (style) {
            case MOON -> {
                Dither.discDithered(s, cx, cy, 17, p.glow(), 3);
                Dither.discDithered(s, cx, cy, 15, p.glow(), 7);
            }
            case SUN -> {
                for (int k = 0; k < 16; k++) {
                    double a = k * Math.PI / 8 + Math.PI / 16;
                    int outer = (k & 1) == 0 ? 19 : 16;
                    Dither.line(s, cx + Math.cos(a) * 14, cy + Math.sin(a) * 14, cx + Math.cos(a) * outer,
                            cy + Math.sin(a) * outer, p.accent());
                }
                Dither.discDithered(s, cx, cy, 15, p.glow(), 8);
            }
            case CLOCK -> {
                Dither.disc(s, cx, cy, 16, p.dark());
                Dither.ring(s, cx, cy, 16, p.mid());
                for (int k = 0; k < 12; k++) {
                    double a = k * Math.PI / 6;
                    int tx = (int) Math.round(cx + Math.cos(a) * 14);
                    int ty = (int) Math.round(cy + Math.sin(a) * 14);
                    s.set(tx, ty, p.light());
                    if (k % 3 == 0) {
                        s.set((int) Math.round(cx + Math.cos(a) * 15), (int) Math.round(cy + Math.sin(a) * 15),
                                p.light());
                    }
                }
            }
            case ROSE -> {
                Dither.disc(s, cx, cy, 17, p.dark());
                Dither.ring(s, cx, cy, 17, p.mid());
                for (int k = 0; k < 8; k++) {
                    double a = k * Math.PI / 4 + Math.PI / 8;
                    Dither.disc(s, (int) Math.round(cx + Math.cos(a) * 14.5), (int) Math.round(cy + Math.sin(a) * 14.5),
                            1, (k & 1) == 0 ? p.accent() : p.glow());
                }
            }
            case GEAR -> {
                for (int k = 0; k < 14; k++) {
                    double a = k * Math.PI / 7;
                    int tx = (int) Math.round(cx + Math.cos(a) * 16);
                    int ty = (int) Math.round(cy + Math.sin(a) * 16);
                    s.fill(tx - 1, ty - 1, tx + 2, ty + 2, p.mid());
                }
                Dither.disc(s, cx, cy, 15, p.mid());
                Dither.ring(s, cx, cy, 15, p.light());
                Dither.ring(s, cx, cy, 14, p.dark());
            }
            case SPOT -> {
                Dither.discDithered(s, cx, cy, 19, p.glow(), 3);
                Dither.discDithered(s, cx, cy, 16, p.glow(), 6);
            }
            case WINDOW -> {
                Dither.disc(s, cx, cy, 16, p.dark());
                Dither.ring(s, cx, cy, 16, p.mid());
                Dither.ring(s, cx, cy, 14, p.mid());
                for (int k = 0; k < 4; k++) {
                    double a = k * Math.PI / 2;
                    Dither.line(s, cx + Math.cos(a) * 13, cy + Math.sin(a) * 13, cx + Math.cos(a) * 15,
                            cy + Math.sin(a) * 15, p.mid());
                }
            }
            case PLAIN -> {
            }
        }
        Dither.disc(s, cx, cy, DISC + 1, p.accent());
        Dither.disc(s, cx, cy, DISC, p.glow());
        if (emblem == null) {
            return false;
        }
        // The emblem keeps its handedness on a mirrored scene. 镜像的景上，徽记保持原本朝向。
        PixelSink target = s instanceof MirrorSink mirror ? mirror.inner() : s;
        int ex = s instanceof MirrorSink mirror ? mirror.flip(cx) : cx;
        Emblems.draw(target, emblem, ex, cy, p.dark(), p.mid(), p.light(), p.deep(), p.paper());
        return true;
    }

    // ================================================================ parts / 部件

    /** Mid ground with the top {@code horizon} rows dithered towards dark. 中灰底，上方 horizon 行向暗色抖动。 */
    static void nightSky(PixelSink s, int x0, int y0, int w, Palette p, int horizon, int topLevel) {
        s.fill(x0, y0, x0 + w, y0 + HEIGHT, p.mid());
        for (int y = 0; y < horizon; y++) {
            int level = (int) Math.round(topLevel * (1 - (double) y / horizon));
            Dither.rect(s, x0, y0 + y, x0 + w, y0 + y + 1, p.dark(), level);
        }
    }

    static void stars(PixelSink s, int x0, int y0, int w, Palette p, DecorRandom rand, int n, int maxY) {
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
    static void ridge(PixelSink s, int x0, int y0, int w, int baseY, int amp, double phase, int col) {
        for (int x = 0; x < w; x++) {
            double v = 0.55 * Math.sin(x * 0.045 + phase) + 0.3 * Math.sin(x * 0.11 + phase * 1.7)
                    + 0.15 * Math.sin(x * 0.27 + phase * 2.3);
            int top = (int) Math.round(baseY - amp * (0.5 + 0.5 * v));
            s.fill(x0 + x, y0 + top, x0 + x + 1, y0 + HEIGHT, col);
        }
    }

    /** A bare, twisted tree: trunk then two branches, recursively. 枯树：主干与两根分枝，递归。 */
    static void tree(PixelSink s, double x, double y, double len, double angle, int depth, int col,
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

    /** A tiered pine with its trunk foot at (x, baseY). 底部在 (x, baseY) 的分层松树。 */
    static void pine(PixelSink s, int x, int baseY, int height, int col) {
        int top = baseY - height;
        int tiers = Math.max(2, height / 7);
        int tierHeight = Math.max(4, (height - 2) / tiers + 2);
        for (int t = 0; t < tiers; t++) {
            int ty = top + t * (height - 2) / tiers;
            int maxHalf = 2 + (t + 1) * height / (tiers * 4);
            for (int r = 0; r < tierHeight; r++) {
                int half = Math.max(0, r * maxHalf / tierHeight);
                s.fill(x - half, ty + r, x + half + 1, ty + r + 1, col);
            }
        }
        s.fill(x, baseY - 2, x + 1, baseY, col);
    }

    /** A pointed (lancet) arch outline filled with {@code fill}. 尖拱，内填 fill。 */
    static void lancet(PixelSink s, int x, int top, int w, int bottom, int fill) {
        int half = w / 2;
        for (int y = top; y < bottom; y++) {
            int dy = y - top;
            int span = dy < half * 2 ? (int) Math.round(half * Math.sqrt(Math.min(1.0, dy / (half * 2.0)))) : half;
            s.fill(x + half - span, y, x + half + span + (w & 1), y + 1, fill);
        }
    }

    /** A gear outline with {@code teeth} teeth. 有 teeth 个齿的齿轮。 */
    static void gear(PixelSink s, int cx, int cy, int r, int teeth, int col, int hole) {
        for (int k = 0; k < teeth; k++) {
            double a = k * 2 * Math.PI / teeth;
            int tx = (int) Math.round(cx + Math.cos(a) * (r + 1));
            int ty = (int) Math.round(cy + Math.sin(a) * (r + 1));
            s.fill(tx - 1, ty - 1, tx + 2, ty + 2, col);
        }
        Dither.disc(s, cx, cy, r, col);
        Dither.disc(s, cx, cy, Math.max(1, r / 3), hole);
    }
}
