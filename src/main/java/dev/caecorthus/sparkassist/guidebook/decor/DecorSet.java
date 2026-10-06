package dev.caecorthus.sparkassist.guidebook.decor;

import dev.caecorthus.sparkassist.guidebook.GuidebookEntryDecor;
import java.util.Locale;
import java.util.Optional;

/**
 * One page's (or one player's) decoration set: which chapter plate scene and emblem, watermark sigil and foliage
 * it gets, its accent colour, and the wax-seal mark and card charm. Factions have defaults; a page then gets its own
 * scene and emblem from {@link DecorSetResolver}, and an entry's JSON {@code decor} field overrides single members.
 * 一页（或一名玩家）的点缀套别：扉画的景与徽记、暗纹、植物、点睛色、火漆印面与吊坠。各阵营有默认值；页面再由
 * DecorSetResolver 得到自己的景与徽记，条目 JSON 的 decor 字段可以单独覆盖其中一项。
 *
 * @param colour 0xRRGGBB accent (ribbon, seal, plate highlight) / 点睛色
 * @param berry  ARGB berry colour for the foliage, 0 for none / 浆果色，0 表示没有
 * @param emblem entry id of the plate emblem (see {@link Emblems}), empty for none / 扉画徽记的条目 id，空串为无
 */
public record DecorSet(Plate plate, Sigil sigil, Foliage foliage, int colour, int berry, Mark mark, Charm charm,
                       String emblem) {
    /** Chapter plate scenes. 扉画的景。 */
    public enum Plate {
        MARSH, VIADUCT, RAIN, FAIR, FIELD, ARCANE,
        CARRIAGE, STATION, SEA, CHAPEL, GRAVEYARD, STUDY, ROOFTOPS, FOREST, STAGE, WORKSHOP
    }

    public enum Sigil { CIRCLE, COMPASS, WEB, HARLEQUIN, CONSTELLATION, RUNES }

    public enum Foliage { BRIAR, WILLOW, DEAD, RIBBON, IVY }

    public enum Mark { MOON, STAR, DAGGER, MASK }

    public enum Charm { MOON, KEY, DAGGER, BELL }

    public static final DecorSet WITCH = new DecorSet(Plate.MARSH, Sigil.CIRCLE, Foliage.BRIAR, 0xB567FF,
            0xFFB567FF, Mark.MOON, Charm.MOON, "");
    public static final DecorSet CIVILIAN = new DecorSet(Plate.VIADUCT, Sigil.COMPASS, Foliage.WILLOW, 0x5FA3C9,
            0, Mark.STAR, Charm.KEY, "");
    public static final DecorSet KILLER = new DecorSet(Plate.RAIN, Sigil.WEB, Foliage.DEAD, 0xC13838,
            0xFF8A1B29, Mark.DAGGER, Charm.DAGGER, "");
    public static final DecorSet NEUTRAL = new DecorSet(Plate.FAIR, Sigil.HARLEQUIN, Foliage.RIBBON, 0xDFA94F,
            0, Mark.MASK, Charm.BELL, "");
    public static final DecorSet TRAIT = new DecorSet(Plate.FIELD, Sigil.CONSTELLATION, Foliage.IVY, 0x36E51B,
            0xFF6B5A9A, Mark.STAR, Charm.KEY, "");
    public static final DecorSet SKILL = new DecorSet(Plate.ARCANE, Sigil.RUNES, Foliage.BRIAR, 0xD6B0FF,
            0xFFD6B0FF, Mark.MOON, Charm.MOON, "");

    /** This set with a page's own plate scene and emblem. 换上某页自己的景与徽记。 */
    public DecorSet withPlate(Plate scene, String emblemId) {
        return new DecorSet(scene, sigil, foliage, colour, berry, mark, charm, emblemId);
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
                colour, berry, mark, charm, emblem);
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
