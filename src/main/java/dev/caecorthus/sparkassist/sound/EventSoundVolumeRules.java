package dev.caecorthus.sparkassist.sound;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.util.Identifier;

/**
 * Whitelist for game-event sounds controlled by SparkAssist's event volume.
 * SparkAssist 只缩放这里列出的游戏事件音效，避免影响普通方块、玩家、语音或其他角色音效。
 */
public final class EventSoundVolumeRules {
    private static final Map<EventSoundGroup, Set<Identifier>> GROUP_SOUND_IDS = createGroupSoundIds();
    private static final Map<Identifier, EventSoundGroup> SOUND_ID_GROUPS = createSoundIdGroups();

    private EventSoundVolumeRules() {
    }

    public static boolean isEventSound(Identifier id) {
        return id != null && SOUND_ID_GROUPS.containsKey(id);
    }

    public static EventSoundGroup groupFor(Identifier eventId, Identifier selectedSoundId) {
        EventSoundGroup eventGroup = groupFor(eventId);
        if (eventGroup != null) {
            return eventGroup;
        }
        return groupFor(selectedSoundId);
    }

    public static float scaledVolume(Identifier eventId, Identifier selectedSoundId, float originalVolume, double eventVolume) {
        if (groupFor(eventId, selectedSoundId) == null) {
            return originalVolume;
        }
        return (float) (originalVolume * clampVolume(eventVolume));
    }

    public static double clampVolume(double volume) {
        if (Double.isNaN(volume)) {
            return 1.0D;
        }
        return Math.max(0.0D, Math.min(1.0D, volume));
    }

    private static EventSoundGroup groupFor(Identifier id) {
        return id == null ? null : SOUND_ID_GROUPS.get(id);
    }

    private static Map<EventSoundGroup, Set<Identifier>> createGroupSoundIds() {
        EnumMap<EventSoundGroup, Set<Identifier>> ids = new EnumMap<>(EventSoundGroup.class);
        ids.put(EventSoundGroup.PSYCHO_MODE, Set.of(
                Identifier.of("wathe", "ambient.psycho_drone"),
                Identifier.of("wathe", "ambient/psycho_drone")
        ));
        ids.put(EventSoundGroup.CORRUPT_COP_MOMENT, Set.of(
                Identifier.of("noellesroles", "ambient.corrupt_cop_execution"),
                Identifier.of("noellesroles", "music.corrupt_cop_moment_1"),
                Identifier.of("noellesroles", "music.corrupt_cop_moment_2"),
                Identifier.of("noellesroles", "ambient/manba_out"),
                Identifier.of("noellesroles", "ambient/kemiao"),
                Identifier.of("noellesroles", "ambient/boyi"),
                Identifier.of("noellesroles", "ambient/donk")
        ));
        ids.put(EventSoundGroup.JESTER_MOMENT, Set.of(
                Identifier.of("noellesroles", "ambient.jester_laugh"),
                Identifier.of("noellesroles", "music.jester_moment"),
                Identifier.of("noellesroles", "ambient/jester_laugh"),
                Identifier.of("noellesroles", "ambient/dbd_jester")
        ));
        ids.put(EventSoundGroup.TRAIN_OUTSIDE, Set.of(
                Identifier.of("wathe", "ambient.train.outside"),
                Identifier.of("wathe", "ambient/train_outside")
        ));
        ids.put(EventSoundGroup.TRAIN_HORN, Set.of(
                Identifier.of("wathe", "ambient.train.horn"),
                Identifier.of("wathe", "ambient.ship.outside"),
                Identifier.of("wathe", "ambient/train_horn"),
                Identifier.of("wathe", "ambient/ship_outside")
        ));
        ids.put(EventSoundGroup.PIG_CHASE, Set.of(
                Identifier.of("sparkwitch", "skill.pig_chase"),
                Identifier.of("sparkwitch", "skill/pig_chase")
        ));
        ids.put(EventSoundGroup.ARROGANT_ASF_MUSIC, Set.of(
                Identifier.of("sparktraits", "music.takediskrush"),
                Identifier.of("sparktraits", "music/takediskrush")
        ));
        ids.put(EventSoundGroup.GRAND_WITCH_CEREMONIAL_SWORD_BGM, Set.of(
                Identifier.of("sparkwitch", "ambient.grand_witch_ceremonial_sword_bgm"),
                Identifier.of("sparkwitch", "ambient/grand_witch_ceremonial_sword_bgm")
        ));
        ids.put(EventSoundGroup.DEPRESSION_PSYCHO_RANGE, Set.of(
                Identifier.of("sparktraits", "depression.docile_to_rage"),
                Identifier.of("sparktraits", "depression/docile_to_rage"),
                Identifier.of("sparktraits", "depression.rage_loop"),
                Identifier.of("sparktraits", "depression/rage_loop"),
                Identifier.of("sparktraits", "depression.melee_kill_1"),
                Identifier.of("sparktraits", "depression/melee_kill_1"),
                Identifier.of("sparktraits", "depression.melee_kill_2"),
                Identifier.of("sparktraits", "depression/melee_kill_2"),
                Identifier.of("sparktraits", "depression.rage_to_docile"),
                Identifier.of("sparktraits", "depression/rage_to_docile"),
                Identifier.of("sparktraits", "depression.shyguy_killed"),
                Identifier.of("sparktraits", "depression/shyguy_killed")
        ));
        ids.put(EventSoundGroup.DEPRESSION_PSYCHO_MUSIC, Set.of(
                Identifier.of("sparktraits", "depression.blind_rage_enrage"),
                Identifier.of("sparktraits", "depression/blind_rage_enrage"),
                Identifier.of("sparktraits", "depression.blind_rage_chase"),
                Identifier.of("sparktraits", "depression/blind_rage_chase")
        ));
        ids.put(EventSoundGroup.DEPRESSION_PSYCHO_ALERT, Set.of(
                Identifier.of("sparktraits", "depression.player_was_seen"),
                Identifier.of("sparktraits", "depression/player_was_seen")
        ));
        return Map.copyOf(ids);
    }

    private static Map<Identifier, EventSoundGroup> createSoundIdGroups() {
        Map<Identifier, EventSoundGroup> groups = new HashMap<>();
        for (Map.Entry<EventSoundGroup, Set<Identifier>> entry : GROUP_SOUND_IDS.entrySet()) {
            for (Identifier id : entry.getValue()) {
                groups.put(id, entry.getKey());
            }
        }
        return Map.copyOf(groups);
    }
}
