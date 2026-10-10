package dev.caecorthus.sparkassist.client.guidebook.ui;

/**
 * Harpy Express tokens (spec-final §3.1). Names and values are identical to the constants block of SparkWitch and
 * SparkTraits {@code InventoryCardPaint}; the card only uses the dark subset. All values are ARGB.
 * 哈比特快设计令牌（spec-final §3.1）。名称与数值与 SparkWitch、SparkTraits 的 {@code InventoryCardPaint} 常量完全一致；
 * 信息卡只使用暗色子集。所有数值均为 ARGB。
 */
public final class ExpressPalette {
    // ---- mahogany frame / 桃花心木边框
    public static final int SHADOW = 0x66000000;
    public static final int EDGE = 0xFF0B0402;
    public static final int RIM_HI = 0xFF5A2D19;
    public static final int RIM_LO = 0xFF2C1204;
    public static final int RIM_MITER = 0xFF43200F;
    public static final int BRASS_LO = 0xFF815A15;
    public static final int BRASS = 0xFFA58224;
    public static final int BRASS_HI = 0xFFC5A244;
    public static final int POLISHED = 0xFFD4AF37;
    public static final int COIN = 0xFFFFBF49;
    public static final int BODY = 0xFF1C0C05;
    public static final int BAND = 0xFF381406;
    public static final int WELL = 0xFF0C0502;
    public static final int WELL_SHADE = 0xFF050200;
    public static final int WELL_LIP = 0xFF3D1E0E;
    public static final int SELECT = 0xFF4E2614;
    public static final int HOVER = 0x16FFBF49;
    public static final int ICON_HOVER = 0x30FFBF49;
    public static final int ICON_ACTIVE = 0x18FFBF49;
    public static final int FOCUS = 0xC0C5A244;
    public static final int SCRIM = 0xC4070302;
    public static final int SCRIM_SOFT = 0x60070302;
    public static final int TIP_BG = 0xFF160902;
    /** Lower line of the etched footer separator. 页脚刻线的下沿。 */
    public static final int ETCH_LIGHT = 0x80542818;
    /** Shade of the idle dark scrollbar thumb. 暗色滚动条静止滑块的阴影。 */
    public static final int THUMB_SHADE = 0xFF5E3F0C;
    /** Body of a hovered 22 px icon button. 悬停时 22 像素图标按钮的底色。 */
    public static final int BUTTON_HOVER = 0xFF3A1A0B;
    /** Top glint on the paper sheet. 纸张顶部高光。 */
    public static final int PAPER_GLINT = 0x40FFFFFF;

    // ---- text on dark / 暗底文字
    public static final int TEXT = 0xFFEFE2C8;
    public static final int TEXT_HI = 0xFFFFF7E6;
    public static final int MUTED = 0xFFB4A080;
    public static final int FAINT = 0xFF9A8565;
    public static final int TIP_DESC = 0xFFCDBB9C;
    public static final int HEADING = BRASS_HI;
    public static final int TITLE = POLISHED;
    /** Mana numbers (= SparkWitch HUD); the mana glyph itself is drawn with {@link #GLYPH}. 魔力数字；魔力图标用 GLYPH 绘制。 */
    public static final int MANA = 0xFFD6B0FF;
    /** Untinted colour for font icons (coin, mana) so their native pixels show. 字体图标不着色，保留原生像素。 */
    public static final int GLYPH = 0xFFFFFFFF;

    // ---- status (card) / 状态（信息卡）
    public static final int READY_TEXT = 0xFFA9E98C;
    public static final int ACTIVE_TEXT = 0xFFFFD68A;
    public static final int COOL_TEXT = 0xFFE3CC94;
    public static final int ALERT_TEXT = 0xFFF2838B;
    public static final int VELVET = 0xFFDC001E;
    public static final int VELVET_LO = 0xFF8A1B29;

    // ---- paper / 纸张
    public static final int PAPER = 0xFFF3E6CC;
    public static final int PAPER_EDGE = 0xFFE6CEAC;
    public static final int PAPER_DEEP = 0xFFD2B68C;
    public static final int INK = 0xFF2F1B1B;
    public static final int INK_MUTED = 0xFF6B5641;
    /** Decoration only, never text (2.8:1). 仅用于装饰，不用于文字。 */
    public static final int INK_FAINT = 0xFF9C8667;
    public static final int INK_RULE = 0x803B2A1A;
    public static final int INK_RULE_SOFT = 0x383B2A1A;
    public static final int CALLOUT = 0x143B2A1A;
    /** Idle demo button on the paper: a faint brass wash. 纸面上未悬停的演示按钮：淡黄铜底。 */
    public static final int DEMO_WASH = 0x24A58224;
    public static final int PAPER_THUMB = 0xFF8C7659;
    public static final int PAPER_THUMB_HI = 0xFFA8926F;
    public static final int GILT_LEAF = 0xFFF6DE9C;
    public static final int GILT_INK = 0xFF5A3E08;
    public static final int GILT_STAR = 0xFF9A6B0C;

    // ---- tones on paper (>= 4.5:1 on PAPER and on the callout) / 纸面色调
    public static final int P_GOOD = 0xFF2F6B34;
    public static final int P_MONEY = 0xFF735410;
    public static final int P_DANGER = 0xFFA3141C;
    public static final int P_INFO = 0xFF25477A;
    public static final int P_ITEM = 0xFF5E2A6E;
    public static final int P_MUTED = INK_MUTED;

    private ExpressPalette() {
    }
}
