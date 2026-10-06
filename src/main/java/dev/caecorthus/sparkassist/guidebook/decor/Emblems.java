package dev.caecorthus.sparkassist.guidebook.decor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The page emblems: one small woodcut-style picture per guide entry (role, skill, trait, faction), keyed by entry
 * id and drawn on the chapter plate's focal disc. Each is at most {@value #SIZE}×{@value #SIZE} characters:
 * {@code #} dark ink, {@code +} mid ink, {@code -} light ink, {@code *} the deep accent (the page's identity colour
 * darkened), {@code o} paper; {@code .} is clear. The pictures live in the {@code Emblems*} tables by faction.
 * 页面徽记：每个指南条目（身份、技能、词条、阵营）一幅木刻风小图，按条目 id 取用，画在扉画的焦点圆盘上。每幅最大
 * SIZE×SIZE 个字符：# 深墨，+ 中墨，- 浅墨，* 深点睛色（页面身份色压暗），o 纸色，. 透明。图案按阵营分放在各个
 * Emblems* 表里。
 */
public final class Emblems {
    /** Largest emblem side, in pixels. 徽记最大边长（像素）。 */
    public static final int SIZE = 17;
    /** Characters an emblem row may use. 徽记行允许的字符。 */
    public static final String KEYS = "#+-*o";

    private static final Map<String, String[]> TABLE;

    static {
        Map<String, String[]> table = new HashMap<>();
        EmblemsCivilian.register(table);
        EmblemsCrew.register(table);
        EmblemsKiller.register(table);
        EmblemsCoven.register(table);
        EmblemsKillerTraits.register(table);
        EmblemsTraits.register(table);
        TABLE = Collections.unmodifiableMap(table);
    }

    private Emblems() {
    }

    public static Optional<String[]> forEntry(String entryId) {
        return Optional.ofNullable(entryId == null ? null : TABLE.get(entryId));
    }

    public static Set<String> ids() {
        return TABLE.keySet();
    }

    /** The emblem centred on (cx, cy) in the given inks. 以 (cx, cy) 为中心、用给定墨色画出徽记。 */
    public static void draw(PixelSink sink, String[] rows, int cx, int cy, int dark, int mid, int light, int accent,
                            int paper) {
        int width = 0;
        for (String row : rows) {
            width = Math.max(width, row.length());
        }
        Dither.glyph(sink, cx - width / 2, cy - rows.length / 2, KEYS, new int[] {dark, mid, light, accent, paper},
                1, rows);
    }
}
