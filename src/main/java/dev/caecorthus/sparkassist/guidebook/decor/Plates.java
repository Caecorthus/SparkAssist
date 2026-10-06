package dev.caecorthus.sparkassist.guidebook.decor;

import static dev.caecorthus.sparkassist.guidebook.decor.DecorPalette.INK;
import static dev.caecorthus.sparkassist.guidebook.decor.DecorPalette.INK_FAINT;
import static dev.caecorthus.sparkassist.guidebook.decor.DecorPalette.INK_MUTED;
import static dev.caecorthus.sparkassist.guidebook.decor.DecorPalette.INK_RULE;
import static dev.caecorthus.sparkassist.guidebook.decor.DecorPalette.INK_RULE_SOFT;
import static dev.caecorthus.sparkassist.guidebook.decor.DecorPalette.PAPER;

/**
 * The chapter plates, 56 px tall and as wide as the text column. Every guide page has its own hand-made picture
 * ({@link PlateArt}, drawn by {@link #art}); pages without one (the credits, the lobby frontispiece, entries added
 * by other mods) fall back to their faction's procedural scene ({@link #plate}): paper, three inks and the theme
 * colour, with Bayer dither for every gradient, stars and trees from the page seed. Both draw the thin plate frame
 * with its corner ticks, 3 px outside the picture.
 * 扉画，高 56、宽同版心。每个指南页面都有自己手绘的画（PlateArt，由 art() 绘制）；没有的页面（鸣谢页、大厅扉页、
 * 其他模组新增的条目）退回阵营的程序化景（plate()）：纸色、三阶墨色与主题色，渐变全靠 Bayer 抖动，星与树随页种子
 * 变化。两者都会在画外 3 像素处画细框与角标。
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
    enum Focal { MOON, SUN, PLAIN }

    /**
     * The colours of a plate, all from the page's theme colour: paper, three inks, the accent and glow mixed into the
     * paper, {@code deep} (the theme darkened) for scene details, and the emblem's own ramp ({@code mark*}), so the
     * emblem is drawn in the theme colour itself.
     * 一幅扉画的颜色，全部取自页面主题色：纸色、三阶墨色、调进纸色的点睛色与辉光、压暗的 deep（景的细节），以及徽记
     * 自己的色阶（mark*），让徽记本身就是主题色。
     */
    public record Palette(int paper, int light, int mid, int dark, int accent, int glow, int deep, int markDark,
                          int markMid, int markLight, int markBright, int theme) {
        public static Palette of(int themeRgb) {
            int theme = DecorPalette.opaque(themeRgb);
            // A pale theme (a near-white witch, a pink pig) would vanish on the glowing disc: darken its bright tone.
            // 很浅的主题色（近白的魔女、粉色的猪）在发光圆盘上会看不见，压暗其亮色。
            double luma = (0.299 * ((theme >> 16) & 0xFF) + 0.587 * ((theme >> 8) & 0xFF) + 0.114 * (theme & 0xFF)) / 255;
            int bright = luma > 0.62 ? DecorPalette.mix(theme, INK, Math.min(0.55, (luma - 0.62) * 1.6 + 0.2)) : theme;
            return new Palette(PAPER, INK_FAINT, INK_MUTED, INK, DecorPalette.mix(PAPER, theme, 0.55),
                    DecorPalette.mix(PAPER, theme, 0.22), DecorPalette.mix(theme, INK, 0.3),
                    DecorPalette.mix(theme, INK, 0.66), DecorPalette.mix(theme, INK, 0.38),
                    DecorPalette.mix(theme, PAPER, 0.4), bright, theme);
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
        frame(sink, x, y, w);
    }

    /**
     * A page's own hand-made plate with its frame: the picture cropped to {@code w} around its focus, the theme
     * tones inked with {@code themeRgb}, and the emblem printed on the carrier the picture leaves for it (when the
     * crop shows it).
     * 页面自己的手绘扉画连同画框：围绕 focus 裁成 w 宽，主题色阶用 themeRgb 着色，徽记印在画里留出的底上（裁切后可见时）。
     */
    public static void art(PixelSink sink, PlateArt art, String[] emblem, int x, int y, int w, int themeRgb) {
        Palette p = Palette.of(themeRgb);
        int[] colours = PlateArt.colours(p);
        int left = art.cropLeft(w);
        for (int row = 0; row < HEIGHT; row++) {
            for (int col = 0; col < w; col++) {
                int ax = Math.max(0, Math.min(art.width() - 1, left + col));
                sink.set(x + col, y + row, colours[art.tone(ax, row)]);
            }
        }
        if (emblem != null) {
            int ex = x + art.emblemX() - left;
            if (ex - Emblems.SIZE / 2 >= x && ex + Emblems.SIZE / 2 < x + w) {
                Emblems.draw(new ClipSink(sink, x, y, w, HEIGHT), emblem, ex, y + art.emblemY(), p.markDark(),
                        p.markMid(), p.markLight(), p.markBright(), p.paper());
            }
        }
        frame(sink, x, y, w);
    }

    /** The thin frame and corner ticks in the 3 px margin around a picture. 画外 3 像素内的细框与角标。 */
    private static void frame(PixelSink sink, int x, int y, int w) {
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
        switch (kind) {
            case MARSH -> PlateScenes.marsh(s, x, y, w, p, rand, emblem);
            case VIADUCT -> PlateScenes.viaduct(s, x, y, w, p, rand, emblem);
            case RAIN -> PlateScenes.rain(s, x, y, w, p, rand, emblem);
            case FAIR -> PlateScenes.fair(s, x, y, w, p, rand, emblem);
            case FIELD -> PlateScenes.field(s, x, y, w, p, rand, emblem);
            case ARCANE -> PlateScenes.arcane(s, x, y, w, p, rand, emblem);
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
            case PLAIN -> {
            }
        }
        Dither.disc(s, cx, cy, DISC + 1, p.accent());
        Dither.disc(s, cx, cy, DISC, p.glow());
        if (emblem == null) {
            return false;
        }
        Emblems.draw(s, emblem, cx, cy, p.markDark(), p.markMid(), p.markLight(), p.markBright(), p.paper());
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
}
