package dev.caecorthus.sparkassist.guidebook.decor;

import dev.caecorthus.sparkassist.guidebook.GuidebookEntryDecor;
import java.util.Locale;
import java.util.Optional;

/**
 * One page's (or one player's) decoration set: its page (whose hand-made plate and emblem it shows) and theme
 * colour, the fallback plate scene, watermark sigil and foliage, its faction accent, and the wax-seal mark and card
 * charm. Factions have defaults; {@link DecorSetResolver} fills in the page and theme, and an entry's JSON
 * {@code decor} field overrides single members.
 * 一页（或一名玩家）的点缀套别：所属页面（显示它的手绘扉画与徽记）与主题色、后备扉画景、暗纹、植物、阵营点睛色、
 * 火漆印面与吊坠。各阵营有默认值；DecorSetResolver 填入页面与主题色，条目 JSON 的 decor 字段可以单独覆盖其中一项。
 *
 * @param colour 0xRRGGBB accent (ribbon, seal, plate highlight) / 点睛色
 * @param berry  ARGB berry colour for the foliage, 0 for none / 浆果色，0 表示没有
 * @param page   entry id of the page, whose plate art and emblem are drawn; empty for none / 页面的条目 id，取其扉画与
 *               徽记；空串为无
 * @param theme  0xRRGGBB the page's own theme colour (the role's or trait's colour), which inks the plate and its
 *               emblem; the faction sets use their accent / 页面自己的主题色（身份或词条色），扉画与徽记用它着色
 */
public record DecorSet(Plate plate, Sigil sigil, Foliage foliage, int colour, int berry, Mark mark, Charm charm,
                       String page, int theme) {
    /** The procedural fallback scenes, one per set. 程序化后备景，每个套别一幅。 */
    public enum Plate { MARSH, VIADUCT, RAIN, FAIR, FIELD, ARCANE }

    public enum Sigil { CIRCLE, COMPASS, WEB, HARLEQUIN, CONSTELLATION, RUNES }

    public enum Foliage { BRIAR, WILLOW, DEAD, RIBBON, IVY }

    public enum Mark { MOON, STAR, DAGGER, MASK }

    public enum Charm { MOON, KEY, DAGGER, BELL }

    public static final DecorSet WITCH = new DecorSet(Plate.MARSH, Sigil.CIRCLE, Foliage.BRIAR, 0xB567FF,
            0xFFB567FF, Mark.MOON, Charm.MOON, "", 0xB567FF);
    public static final DecorSet CIVILIAN = new DecorSet(Plate.VIADUCT, Sigil.COMPASS, Foliage.WILLOW, 0x5FA3C9,
            0, Mark.STAR, Charm.KEY, "", 0x5FA3C9);
    public static final DecorSet KILLER = new DecorSet(Plate.RAIN, Sigil.WEB, Foliage.DEAD, 0xC13838,
            0xFF8A1B29, Mark.DAGGER, Charm.DAGGER, "", 0xC13838);
    public static final DecorSet NEUTRAL = new DecorSet(Plate.FAIR, Sigil.HARLEQUIN, Foliage.RIBBON, 0xDFA94F,
            0, Mark.MASK, Charm.BELL, "", 0xDFA94F);
    public static final DecorSet TRAIT = new DecorSet(Plate.FIELD, Sigil.CONSTELLATION, Foliage.IVY, 0x36E51B,
            0xFF6B5A9A, Mark.STAR, Charm.KEY, "", 0x36E51B);
    public static final DecorSet SKILL = new DecorSet(Plate.ARCANE, Sigil.RUNES, Foliage.BRIAR, 0xD6B0FF,
            0xFFD6B0FF, Mark.MOON, Charm.MOON, "", 0xD6B0FF);

    /** This set for one page: its entry id and theme colour. 用于某一页：条目 id 与主题色。 */
    public DecorSet withPage(String pageId, int themeRgb) {
        return new DecorSet(plate, sigil, foliage, colour, berry, mark, charm, pageId, themeRgb & 0xFFFFFF);
    }

    /** Same faction look, whatever the plate: used for "is this my own faction's page". 不论扉画，阵营外观相同。 */
    public boolean sameFaction(DecorSet other) {
        return other != null && sigil == other.sigil && foliage == other.foliage && colour == other.colour
                && berry == other.berry && mark == other.mark && charm == other.charm;
    }

    /** The entry's own choices replace single members; unknown names are ignored. 条目自定义覆盖单项，未知名称忽略。 */
    public DecorSet withOverrides(GuidebookEntryDecor decor) {
        if (decor == null || decor.isEmpty()) {
            return this;
        }
        return new DecorSet(
                parse(Plate.class, decor.plate()).orElse(plate),
                parse(Sigil.class, decor.sigil()).orElse(sigil),
                parse(Foliage.class, decor.foliage()).orElse(foliage),
                colour, berry, mark, charm, page, theme);
    }

    private static <E extends Enum<E>> Optional<E> parse(Class<E> type, Optional<String> name) {
        if (name.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Enum.valueOf(type, name.get().trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException invalid) {
            return Optional.empty();
        }
    }
}
