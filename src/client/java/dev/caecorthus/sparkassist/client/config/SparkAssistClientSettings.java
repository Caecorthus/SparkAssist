package dev.caecorthus.sparkassist.client.config;

import dev.caecorthus.sparkassist.client.SparkAssistClient;
import dev.caecorthus.sparkassist.client.input.InstinctKeyController;
import dev.caecorthus.sparkassist.client.sound.EventSoundVolumeController;
import dev.caecorthus.sparkassist.config.SparkAssistConfig;
import dev.caecorthus.sparkassist.config.SparkAssistConfig.InstinctKeyMode;
import dev.caecorthus.sparkassist.sound.EventSoundGroup;

/**
 * Client settings Adapter that keeps screen Modules shallow.
 * 客户端设置适配器，用于保持界面 Module 的浅层职责。
 */
public final class SparkAssistClientSettings {
    private SparkAssistClientSettings() {
    }

    public static InstinctKeyMode instinctKeyMode() {
        return config().instinctKeyMode();
    }

    public static void setInstinctKeyMode(InstinctKeyMode mode) {
        SparkAssistConfigManager manager = SparkAssistClient.configManager();
        manager.config().setInstinctKeyMode(mode);
        manager.save();
        InstinctKeyController.reset();
    }

    public static double eventSoundVolume(EventSoundGroup group) {
        return config().eventSoundVolume(group);
    }

    public static void setEventSoundVolume(EventSoundGroup group, double volume) {
        SparkAssistConfigManager manager = SparkAssistClient.configManager();
        manager.config().setEventSoundVolume(group, volume);
        manager.save();
        EventSoundVolumeController.refreshPlayingSounds();
    }

    private static SparkAssistConfig config() {
        return SparkAssistClient.configManager().config();
    }
}
