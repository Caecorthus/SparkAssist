package dev.caecorthus.sparkassist.guidebook.decor;

import java.util.Objects;

/**
 * A page's own hand-made chapter plate: a {@value #WIDTH}×{@value Plates#HEIGHT} picture painted in eight tones
 * (paper, three inks and four theme tones), plus where its story is centred and where the emblem is printed. The
 * game crops it to the text column around {@code focus}, inks the theme tones with the page's colour and prints the
 * emblem on the light carrier the picture leaves for it.
 * 页面自己手绘的扉画：一幅 WIDTH×HEIGHT 的画，只用八个色阶（纸色、三阶墨色与四阶主题色），并记下故事中心与徽记的
 * 位置。游戏按版心宽度围绕 focus 裁切，用页面主题色为主题色阶着色，再把徽记印在画里留出的浅色底上。
 *
 * @param tones  one tone index per pixel, row by row / 每像素一个色阶序号，逐行排列
 * @param focus  x of the story's centre / 故事中心的 x
 * @param emblemX centre of the emblem's carrier / 徽记底的中心
 */
public record PlateArt(int width, byte[] tones, int focus, int emblemX, int emblemY) {
    /** Painted width; the widest text column is 252. 画幅宽度；最宽的版心为 252。 */
    public static final int WIDTH = 256;

    public static final int PAPER = 0;
    public static final int LIGHT = 1;
    public static final int MID = 2;
    public static final int DARK = 3;
    public static final int GLOW = 4;
    public static final int ACCENT = 5;
    public static final int THEME = 6;
    public static final int DEEP = 7;

    /** The authoring colour of each tone; the theme tones are magenta placeholders. 各色阶的作画颜色；主题色阶为洋红占位。 */
    private static final int[] AUTHORING = {0xF3E6CC, 0x9C8667, 0x6B5641, 0x2F1B1B, 0xFFE0FF, 0xFF9CFF, 0xFF00FF,
            0x8C008C};

    public PlateArt {
        Objects.requireNonNull(tones, "tones");
        if (width <= 0 || tones.length != width * Plates.HEIGHT) {
            throw new IllegalArgumentException("plate art must be " + width + "x" + Plates.HEIGHT);
        }
    }

    /** From ARGB pixels; each colour maps to the nearest authoring tone. 由 ARGB 像素构造，每个颜色取最近的作画色阶。 */
    public static PlateArt of(int width, int height, int[] argb, int focus, int emblemX, int emblemY) {
        if (height != Plates.HEIGHT || argb.length != width * height) {
            throw new IllegalArgumentException("plate art must be " + width + "x" + Plates.HEIGHT);
        }
        byte[] tones = new byte[argb.length];
        for (int i = 0; i < argb.length; i++) {
            tones[i] = (byte) nearest(argb[i]);
        }
        return new PlateArt(width, tones, focus, emblemX, emblemY);
    }

    private static int nearest(int argb) {
        int best = 0;
        int bestDistance = Integer.MAX_VALUE;
        for (int t = 0; t < AUTHORING.length; t++) {
            int dr = ((argb >> 16) & 0xFF) - ((AUTHORING[t] >> 16) & 0xFF);
            int dg = ((argb >> 8) & 0xFF) - ((AUTHORING[t] >> 8) & 0xFF);
            int db = (argb & 0xFF) - (AUTHORING[t] & 0xFF);
            int distance = dr * dr + dg * dg + db * db;
            if (distance < bestDistance) {
                bestDistance = distance;
                best = t;
            }
        }
        return best;
    }

    public int tone(int x, int y) {
        return tones[y * width + x];
    }

    /** Left edge of the crop for a column {@code w} wide: centred on focus, kept inside the picture; negative when
     * the column is wider than the picture (the picture is centred and its edge columns continue).
     * 宽 w 的版心对应的裁切左缘：以 focus 为中心并限制在画内；版心比画宽时为负（画居中，边缘列向外延续）。 */
    public int cropLeft(int w) {
        if (w > width) {
            return -((w - width) / 2);
        }
        return Math.max(0, Math.min(width - w, focus - w / 2));
    }

    /** The colour of each tone for a plate palette. 某扉画调色板下各色阶的颜色。 */
    static int[] colours(Plates.Palette p) {
        return new int[] {p.paper(), p.light(), p.mid(), p.dark(), p.glow(), p.accent(), p.theme(), p.deep()};
    }
}
