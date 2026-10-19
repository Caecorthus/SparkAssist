package dev.caecorthus.sparkassist.client.settings;

import dev.caecorthus.sparkassist.sound.EventSoundGroup;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * How the audio page lays out the event sound groups: train and round sounds first, then role moments by faction and
 * traits by category, each row named "role · sound" with the role's guide colour. A row shows only when the mod that
 * plays its sounds is installed. Role names come from the owning mod's own lang key (the guide's nameKey), with an
 * English fallback; colours copy the guide entries' {@code color}.
 * 音频页如何排列事件音效分组：先列车与回合，再按阵营排角色时刻、按分类排天赋；每行写成「角色 · 声音」并配上角色在
 * 指南里的颜色。只有播放这些声音的模组已安装时才显示该行。角色名取自所属模组自己的 lang 键（即指南的 nameKey），并带
 * 英文后备；颜色照抄指南条目的 {@code color}。
 */
final class EventVolumeRows {
    /** Audio page sections, in display order; ids key the remembered folded state. 音频页分节，按显示顺序。 */
    enum Section {
        TRAIN_AND_ROUND("train_round"),
        ROLE_ANY("role_any"),
        ROLE_KILLER("role_killer"),
        ROLE_NEUTRAL("role_neutral"),
        ROLE_CIVILIAN("role_civilian"),
        TRAIT_CIVILIAN("trait_civilian");

        private final String id;

        Section(String id) {
            this.id = id;
        }

        String id() {
            return id;
        }

        String titleKey() {
            return "screen.sparkassist.settings.group." + id;
        }
    }

    /**
     * One slider row.
     *
     * @param group       the volume group it controls / 控制的音量分组
     * @param section     where it sits / 所在分节
     * @param mod         the mod whose sounds it scales; the row hides without it / 声音所属模组，未安装时隐藏
     * @param whoKey      lang key of the role or trait, or null for a plain row / 角色或天赋名的 lang 键，普通行为 null
     * @param whoFallback English name used if that key is missing / 该键缺失时使用的英文名
     * @param colour      the role's guide colour as RGB, 0 for none / 角色在指南中的颜色，0 为无
     * @param preview     a representative sound and the category the game plays it on / 代表性声音及其在游戏中的分类
     */
    record Row(EventSoundGroup group, Section section, String mod, @Nullable String whoKey, String whoFallback,
               int colour, Sample preview) {
    }

    /** A sample: sound event id and its in-game category. 试听样本：声音事件 id 与游戏内分类。 */
    record Sample(String sound, SoundCategory category) {
        Identifier id() {
            return Identifier.of(sound);
        }
    }

    private static Sample sample(String sound, SoundCategory category) {
        return new Sample(sound, category);
    }

    static final List<Row> ROWS = List.of(
            plain(EventSoundGroup.TRAIN_INSIDE, Section.TRAIN_AND_ROUND, "wathe",
                    sample("wathe:ambient.train.inside", SoundCategory.AMBIENT)),
            plain(EventSoundGroup.TRAIN_OUTSIDE, Section.TRAIN_AND_ROUND, "wathe",
                    sample("wathe:ambient.train.outside", SoundCategory.AMBIENT)),
            plain(EventSoundGroup.TRAIN_HORN, Section.TRAIN_AND_ROUND, "wathe",
                    sample("wathe:ambient.train.horn", SoundCategory.AMBIENT)),
            plain(EventSoundGroup.BLACKOUT, Section.TRAIN_AND_ROUND, "wathe",
                    sample("wathe:ambient.blackout", SoundCategory.PLAYERS)),
            plain(EventSoundGroup.ROUND_MUSIC, Section.TRAIN_AND_ROUND, "wathe",
                    sample("wathe:ui.riser", SoundCategory.MASTER)),

            plain(EventSoundGroup.PSYCHO_MODE, Section.ROLE_ANY, "wathe",
                    sample("wathe:ambient.psycho_drone", SoundCategory.AMBIENT)),
            plain(EventSoundGroup.SHIELD_CLANG, Section.ROLE_ANY, "wathe",
                    sample("wathe:item.psycho.armour", SoundCategory.MASTER)),

            role(EventSoundGroup.BELL_RINGER_TOLL, Section.ROLE_KILLER, "sparkwitch",
                    "announcement.role.bell_ringer", "Bell Ringer", 0x8C6D3F,
                    sample("sparkwitch:ambient.bell_ringer_toll", SoundCategory.PLAYERS)),
            role(EventSoundGroup.TIME_STEALER_CHIME, Section.ROLE_KILLER, "sparkwitch",
                    "announcement.role.time_stealer", "Time Stealer", 0x2E8B7A,
                    sample("sparkwitch:ambient.time_stealer_chime", SoundCategory.PLAYERS)),

            role(EventSoundGroup.CORRUPT_COP_MOMENT, Section.ROLE_NEUTRAL, "noellesroles",
                    "announcement.role.corrupt_cop", "Corrupt Cop", 0x193264,
                    sample("noellesroles:music.corrupt_cop_moment_1", SoundCategory.AMBIENT)),
            role(EventSoundGroup.ARROGANT_ASF_MUSIC, Section.ROLE_NEUTRAL, "sparkstrength",
                    "announcement.role.corrupt_cop", "Corrupt Cop", 0x193264,
                    sample("sparkstrength:music.takediskrush", SoundCategory.AMBIENT)),
            role(EventSoundGroup.JESTER_MOMENT, Section.ROLE_NEUTRAL, "noellesroles",
                    "announcement.role.jester", "Jester", 0xF8C8DC,
                    sample("noellesroles:ambient.jester_laugh", SoundCategory.MASTER)),
            role(EventSoundGroup.SHADOW_JESTER_FINALE, Section.ROLE_NEUTRAL, "sparkstrength",
                    "announcement.role.shadow_jester", "Shadow Jester", 0xBE1E5A,
                    sample("sparkstrength:ambient.shadow_jester", SoundCategory.AMBIENT)),
            role(EventSoundGroup.VULTURE_CURSE, Section.ROLE_NEUTRAL, "sparkstrength",
                    "announcement.role.vulture", "Vulture", 0xB56700,
                    sample("sparkstrength:vulture.super_curse", SoundCategory.PLAYERS)),

            role(EventSoundGroup.PIG_CHASE, Section.ROLE_CIVILIAN, "sparkwitch",
                    "announcement.role.pig_god", "Pig God", 0xF2A4FC,
                    sample("sparkwitch:skill.pig_chase", SoundCategory.PLAYERS)),
            role(EventSoundGroup.SAINT_BELL, Section.ROLE_CIVILIAN, "sparkwitch",
                    "announcement.role.saint", "Saint", 0xEEBC78,
                    sample("sparkwitch:ambient.saint_bell", SoundCategory.PLAYERS)),
            role(EventSoundGroup.HOLY_FLASH_TINNITUS, Section.ROLE_CIVILIAN, "sparkwitch",
                    "announcement.role.saint", "Saint", 0xEEBC78,
                    sample("sparkwitch:skill.holy_flash_tinnitus", SoundCategory.PLAYERS)),

            role(EventSoundGroup.DEPRESSION_PSYCHO_RANGE, Section.TRAIT_CIVILIAN, "sparktraits",
                    "trait.sparktraits.depression.name", "Depression", 0x6B7280,
                    sample("sparktraits:depression.docile_to_rage", SoundCategory.AMBIENT)),
            role(EventSoundGroup.DEPRESSION_PSYCHO_MUSIC, Section.TRAIT_CIVILIAN, "sparktraits",
                    "trait.sparktraits.depression.name", "Depression", 0x6B7280,
                    sample("sparktraits:depression.blind_rage_chase", SoundCategory.AMBIENT)),
            role(EventSoundGroup.DEPRESSION_PSYCHO_ALERT, Section.TRAIT_CIVILIAN, "sparktraits",
                    "trait.sparktraits.depression.name", "Depression", 0x6B7280,
                    sample("sparktraits:depression.player_was_seen", SoundCategory.AMBIENT))
    );

    /**
     * Groups kept in the catalog and the config but given no row, because nothing in the shipped mods plays them:
     * SparkWitch stopped starting the Grand Witch's ceremonial sword BGM when the sword became a permanent reward
     * (f29ea44d, 2026-09-14). The catalog still scales the sound if it ever comes back.
     * 保留在目录和配置中、但不显示行的分组，因为当前随附的模组都不会播放它们：仪礼剑改为常驻奖励后
     * （f29ea44d，2026-09-14），SparkWitch 不再启动大魔女仪礼剑 BGM。若以后恢复播放，目录仍会按设置缩放它。
     */
    static final Set<EventSoundGroup> NOT_PLAYED = EnumSet.of(EventSoundGroup.GRAND_WITCH_CEREMONIAL_SWORD_BGM);

    private EventVolumeRows() {
    }

    private static Row plain(EventSoundGroup group, Section section, String mod, Sample preview) {
        return new Row(group, section, mod, null, "", 0, preview);
    }

    private static Row role(EventSoundGroup group, Section section, String mod, String whoKey, String whoFallback,
                            int colour, Sample preview) {
        return new Row(group, section, mod, whoKey, whoFallback, colour, preview);
    }

    /**
     * Rows that pass {@code shown} (in game: the mod is installed and the sound manager knows the sample), grouped by
     * section in display order.
     * 通过 {@code shown} 的行（游戏中：模组已安装且声音管理器认识该样本），按分节与显示顺序排列。
     */
    static List<List<Row>> visibleBySection(Predicate<Row> shown) {
        List<List<Row>> sections = new ArrayList<>();
        for (Section section : Section.values()) {
            List<Row> rows = new ArrayList<>();
            for (Row row : ROWS) {
                if (row.section() == section && shown.test(row)) {
                    rows.add(row);
                }
            }
            sections.add(rows);
        }
        return sections;
    }

    /** Groups that no row places (a programming error the tests catch). 没有被任何行放置的分组（由测试发现）。 */
    static Set<EventSoundGroup> unplaced() {
        Set<EventSoundGroup> missing = EnumSet.allOf(EventSoundGroup.class);
        ROWS.forEach(row -> missing.remove(row.group()));
        missing.removeAll(NOT_PLAYED);
        return missing;
    }
}
