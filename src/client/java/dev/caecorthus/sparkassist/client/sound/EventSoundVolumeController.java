package dev.caecorthus.sparkassist.client.sound;

import dev.caecorthus.sparkassist.client.SparkAssistClient;
import dev.caecorthus.sparkassist.config.SparkAssistConfig;
import dev.caecorthus.sparkassist.sound.EventSoundGroup;
import dev.caecorthus.sparkassist.sound.EventSoundVolumeRules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.Sound;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;

public final class EventSoundVolumeController {
    private EventSoundVolumeController() {
    }

    public static float adjustedVolume(SoundInstance soundInstance, float originalVolume) {
        Sound selectedSound = soundInstance == null ? null : soundInstance.getSound();
        return adjustedVolume(soundInstance, selectedSound, originalVolume);
    }

    public static float adjustedVolume(SoundInstance soundInstance, Sound selectedSound, float originalVolume) {
        if (SparkAssistClient.configManager() == null || soundInstance == null) {
            return originalVolume;
        }
        Identifier eventId = soundInstance.getId();
        Identifier selectedSoundId = selectedSound == null ? null : selectedSound.getIdentifier();
        EventSoundGroup group = EventSoundVolumeRules.groupFor(eventId, selectedSoundId);
        if (group == null) {
            return originalVolume;
        }
        SparkAssistConfig config = SparkAssistClient.configManager().config();
        return (float) (originalVolume * config.eventSoundVolume(group));
    }

    /**
     * Forces vanilla to recalculate currently playing source volumes.
     * 让原版重新计算当前正在播放声音的音量。
     */
    public static void refreshPlayingSounds() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getSoundManager() == null || client.options == null) {
            return;
        }
        refreshSoundCategory(client, SoundCategory.MUSIC);
        refreshSoundCategory(client, SoundCategory.PLAYERS);
    }

    private static void refreshSoundCategory(MinecraftClient client, SoundCategory category) {
        client.getSoundManager().updateSoundVolume(
                category,
                client.options.getSoundVolume(category)
        );
    }
}
