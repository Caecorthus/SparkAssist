package dev.caecorthus.sparkassist.client.settings.screen;

import dev.caecorthus.sparkassist.client.guidebook.ui.ExpressPalette;

/**
 * Colours of the settings screen: Sodium-like translucent black rows with one accent, the guide's brass, on the
 * selected tab, slider fill and keyboard focus.
 * 设置界面的颜色：类似 Sodium 的半透明黑色行，只用一个强调色——指南的黄铜色——标出选中页签、滑块填充与键盘焦点。
 */
final class SettingsPalette {
    static final int ROW = 0x90000000;
    static final int ROW_HOVER = 0xB0101010;
    static final int ROW_HOVER_WASH = 0x18FFFFFF;
    static final int PANEL = 0x90000000;
    static final int SEPARATOR = 0x40FFFFFF;

    static final int TEXT = 0xFFFFFFFF;
    static final int TEXT_BODY = 0xFFD0D0D0;
    static final int TEXT_MUTED = 0xFFA0A0A0;
    static final int TEXT_FAINT = 0xFF8A8A8A;
    static final int TEXT_DISABLED = 0xFF6E6E6E;
    static final int TEXT_WARNING = 0xFFE3B55A;
    static final int ON = 0xFF9BD67E;
    static final int OFF = 0xFF9A9A9A;

    static final int ACCENT = ExpressPalette.BRASS_HI;
    static final int ACCENT_FILL = ExpressPalette.BRASS;
    static final int FOCUS = ExpressPalette.FOCUS;

    static final int TRACK = 0xFF3A3A3A;
    static final int TRACK_DISABLED = 0xFF2A2A2A;
    static final int THUMB = 0xFFE6E6E6;
    static final int THUMB_HOT = 0xFFFFFFFF;

    static final int BUTTON = 0xFF2F2F2F;
    static final int BUTTON_HOVER = 0xFF464646;
    static final int BUTTON_INACTIVE = 0xFF232323;
    static final int BUTTON_EDGE = 0xFF0E0E0E;

    static final int SCROLL_TRACK = 0x40000000;
    static final int SCROLL_THUMB = 0x90FFFFFF;

    private SettingsPalette() {
    }
}
