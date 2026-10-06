package dev.caecorthus.sparkassist.guidebook.decor;

/**
 * Colours the decoration generators paint with. The Harpy Express tokens are value-identical copies of the
 * client's {@code ExpressPalette} (the generators live in the pure source set and cannot see it); the botanical
 * and translucent tokens are new and exist only here. All values are ARGB.
 * 点缀生成器使用的颜色。哈比特快令牌与客户端的 ExpressPalette 同值（生成器位于纯 Java 源集，看不到它）；植物色与
 * 半透明墨色是新增令牌，只存在于此。所有数值均为 ARGB。
 */
public final class DecorPalette {
    // ---- Harpy Express, copied from ExpressPalette / 与 ExpressPalette 同值
    public static final int SHADOW = 0x66000000;
    public static final int EDGE = 0xFF0B0402;
    public static final int RIM_HI = 0xFF5A2D19;
    public static final int RIM_LO = 0xFF2C1204;
    public static final int BRASS_LO = 0xFF815A15;
    public static final int BRASS = 0xFFA58224;
    public static final int BRASS_HI = 0xFFC5A244;
    public static final int POLISHED = 0xFFD4AF37;
    public static final int COIN = 0xFFFFBF49;
    public static final int BODY = 0xFF1C0C05;
    public static final int BAND = 0xFF381406;
    public static final int WELL = 0xFF0C0502;
    public static final int WELL_SHADE = 0xFF050200;
    public static final int VELVET = 0xFFDC001E;
    public static final int VELVET_LO = 0xFF8A1B29;
    public static final int PAPER = 0xFFF3E6CC;
    public static final int PAPER_EDGE = 0xFFE6CEAC;
    public static final int PAPER_DEEP = 0xFFD2B68C;
    public static final int INK = 0xFF2F1B1B;
    public static final int INK_MUTED = 0xFF6B5641;
    public static final int INK_FAINT = 0xFF9C8667;
    public static final int INK_RULE = 0x803B2A1A;
    public static final int INK_RULE_SOFT = 0x383B2A1A;
    public static final int GILT_STAR = 0xFF9A6B0C;
    public static final int P_DANGER = 0xFFA3141C;

    // ---- botanical, on dark wood / 植物色，暗底
    public static final int STEM = 0xFF6B4A1E;
    public static final int STEM_DARK = 0xFF4A3212;
    public static final int LEAF = 0xFF5F7030;
    public static final int LEAF_HI = 0xFF8DA047;
    public static final int DEAD = 0xFF5E4E40;
    public static final int DEAD_HI = 0xFF8A7A68;
    public static final int WIRE = 0xFF9A8E80;
    public static final int THORN = BRASS_HI;
    public static final int BELL = BRASS;
    public static final int BELL_HI = COIN;
    public static final int RIBBON_A = VELVET;
    public static final int RIBBON_B = COIN;

    // ---- botanical, on paper / 植物色，纸面
    public static final int PAPER_STEM = INK_MUTED;
    public static final int PAPER_STEM_DARK = 0xFF4F3F30;
    public static final int PAPER_LEAF = 0xFF566A2C;
    public static final int PAPER_LEAF_HI = 0xFF7A8E3C;
    public static final int PAPER_DEAD = INK_MUTED;
    public static final int PAPER_DEAD_HI = 0xFF8C7659;
    public static final int PAPER_WIRE = 0xFF7A6E60;
    public static final int PAPER_THORN = GILT_STAR;
    public static final int PAPER_BELL = GILT_STAR;
    public static final int PAPER_BELL_HI = BRASS_HI;
    public static final int PAPER_RIBBON_A = P_DANGER;
    public static final int PAPER_RIBBON_B = GILT_STAR;

    // ---- translucent layers / 半透明层
    /** Sigil watermark under the body text: 9.4 % ink. 文字下的暗纹：9.4% 墨色。 */
    public static final int WATERMARK_INK = 0x182F1B1B;
    /** Sigil engraved into the empty part of a dark body: 8 % brass. 暗底空白处的暗纹：8% 黄铜。 */
    public static final int WATERMARK_BRASS = 0x14C5A244;
    /** Steam curls, two tones. 蒸汽卷云的两阶。 */
    public static final int STEAM = 0x40FFF7E6;
    public static final int STEAM_SOFT = 0x22FFF7E6;
    /** Second hairline inside the brass ring. 黄铜环内的第二道细线。 */
    public static final int INNER_LINE = 0x66815A15;
    /** Recessed plate behind a band title. 标题带文字下的凹铭牌。 */
    public static final int NAMEPLATE = 0xFF120804;

    private DecorPalette() {
    }

    /** Opaque ARGB from 0xRRGGBB. 由 0xRRGGBB 得到不透明 ARGB。 */
    public static int opaque(int rgb) {
        return 0xFF000000 | (rgb & 0xFFFFFF);
    }

    /** ARGB with the given alpha (0..255) and 0xRRGGBB. 指定 alpha 的 ARGB。 */
    public static int withAlpha(int alpha, int rgb) {
        return (alpha << 24) | (rgb & 0xFFFFFF);
    }

    /** Per-channel ARGB lerp rounded to nearest, the same rule as {@code ExpressPaint.mix}. 按通道插值并四舍五入。 */
    public static int mix(int a, int b, double t) {
        int alpha = (int) Math.round((a >>> 24) * (1 - t) + (b >>> 24) * t);
        int red = (int) Math.round(((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
        int green = (int) Math.round(((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
        int blue = (int) Math.round((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }
}
