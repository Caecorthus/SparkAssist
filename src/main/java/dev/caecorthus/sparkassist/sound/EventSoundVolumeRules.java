package dev.caecorthus.sparkassist.sound;

import net.minecraft.util.Identifier;

/**
 * Whitelist for game-event sounds controlled by SparkAssist's event volume.
 * SparkAssist 只缩放这里列出的游戏事件音效，避免影响普通方块、玩家、语音或其他角色音效。
 */
public final class EventSoundVolumeRules {
    private static final EventSoundCatalog CATALOG = EventSoundCatalog.createDefault();

    private EventSoundVolumeRules() {
    }

    public static boolean isEventSound(Identifier id) {
        return CATALOG.contains(id);
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
        return CATALOG.groupFor(id);
    }
}
