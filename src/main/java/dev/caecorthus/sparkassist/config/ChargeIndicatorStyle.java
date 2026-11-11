package dev.caecorthus.sparkassist.config;

/**
 * Where and how the charge indicator is drawn. Every style shows the same reading; they differ only in where the
 * player has to look and how much of the screen they take.
 * 蓄力指示画在哪里、画成什么样。各样式显示的读数相同，区别只在玩家要看向哪里、占多少画面。
 */
public enum ChargeIndicatorStyle {
    /** A ring around the crosshair, open at the bottom to leave Wathe's knife glyph alone. 绕准星的环，底部开口让出 Wathe 的刀图标。 */
    RING_OPEN("ring_open"),
    /** A full ring small enough to end above that glyph. 完整的小环，小到停在该图标上方。 */
    RING("ring"),
    /** A short bar under the crosshair, between the knife glyph and a looked-at player's nametag. 准星下的短条，夹在刀图标与所看玩家的名牌之间。 */
    CROSSHAIR_BAR("crosshair_bar"),
    /** A thin line just above the hotbar. 紧贴快捷栏上方的细线。 */
    HOTBAR_LINE("hotbar_line"),
    /** Nothing is drawn. 不绘制。 */
    OFF("off");

    private final String id;

    ChargeIndicatorStyle(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public String translationKey() {
        return "option.sparkassist.charge_indicator.value." + id;
    }

    /** Stable lookup for config deserialisation; an unknown id falls back to {@link #RING_OPEN}. 配置反序列化用的稳定查找，未知 id 回落到默认。 */
    public static ChargeIndicatorStyle fromId(String value) {
        for (ChargeIndicatorStyle style : values()) {
            if (style.id.equals(value)) {
                return style;
            }
        }
        return RING_OPEN;
    }
}
