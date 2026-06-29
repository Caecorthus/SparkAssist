package dev.caecorthus.sparkassist.sound;

import java.util.Set;
import net.minecraft.util.Identifier;

/**
 * Whitelist for game-event sounds controlled by SparkAssist's event volume.
 * SparkAssist 只缩放这里列出的游戏事件音效，避免影响普通方块、玩家、语音或其他角色音效。
 */
public final class EventSoundVolumeRules {
    private static final Set<Identifier> EVENT_SOUND_IDS = Set.of(
            Identifier.of("wathe", "ambient.psycho_drone"),
            Identifier.of("wathe", "ambient.train.outside"),
            Identifier.of("wathe", "ambient.train.horn"),
            Identifier.of("wathe", "ambient.ship.outside"),
            Identifier.of("noellesroles", "ambient.jester_laugh"),
            Identifier.of("noellesroles", "ambient.corrupt_cop_execution"),
            Identifier.of("noellesroles", "music.corrupt_cop_moment_1"),
            Identifier.of("noellesroles", "music.corrupt_cop_moment_2"),
            Identifier.of("noellesroles", "music.jester_moment"),
            Identifier.of("sparkwitch", "skill.pig_chase")
    );

    private EventSoundVolumeRules() {
    }

    public static boolean isEventSound(Identifier id) {
        return id != null && EVENT_SOUND_IDS.contains(id);
    }

    public static float scaledVolume(Identifier id, float originalVolume, double eventVolume) {
        if (!isEventSound(id)) {
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
}
