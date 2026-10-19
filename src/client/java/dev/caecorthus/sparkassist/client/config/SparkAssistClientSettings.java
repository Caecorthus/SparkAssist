package dev.caecorthus.sparkassist.client.config;

import dev.caecorthus.sparkassist.client.SparkAssistClient;
import dev.caecorthus.sparkassist.client.input.InstinctKeyController;
import dev.caecorthus.sparkassist.client.settings.SettingsStore;
import dev.caecorthus.sparkassist.client.sound.EventSoundVolumeController;
import dev.caecorthus.sparkassist.config.SparkAssistConfig;
import dev.caecorthus.sparkassist.config.SparkAssistConfig.GuidebookDecor;
import dev.caecorthus.sparkassist.config.SparkAssistConfig.InstinctKeyMode;
import dev.caecorthus.sparkassist.sound.EventSoundGroup;

/**
 * Client settings Adapter that keeps screen Modules shallow. Setters apply live and leave the file alone; the settings
 * screen writes {@link #STORE} once per change it commits (a slider writes when released, not on every step).
 * 客户端设置适配器，用于保持界面 Module 的浅层职责。setter 立即生效但不写盘；设置界面每次提交改动时写入 {@link #STORE}
 * 一次（滑块在松手时写，而不是每动一格写一次）。
 */
public final class SparkAssistClientSettings {
    /** {@code config/sparkassist.json}. */
    public static final SettingsStore STORE = new SettingsStore("sparkassist", SparkAssistClientSettings::save);

    private SparkAssistClientSettings() {
    }

    public static void save() {
        SparkAssistClient.configManager().save();
    }

    public static InstinctKeyMode instinctKeyMode() {
        return config().instinctKeyMode();
    }

    public static void setInstinctKeyMode(InstinctKeyMode mode) {
        config().setInstinctKeyMode(mode);
        InstinctKeyController.reset();
    }

    public static double eventSoundVolume(EventSoundGroup group) {
        return config().eventSoundVolume(group);
    }

    public static void setEventSoundVolume(EventSoundGroup group, double volume) {
        config().setEventSoundVolume(group, volume);
        EventSoundVolumeController.refreshPlayingSounds();
    }

    public static GuidebookDecor guidebookDecor() {
        return config().guidebookDecor();
    }

    public static void setGuidebookDecor(GuidebookDecor decor) {
        config().setGuidebookDecor(decor);
    }

    public static boolean guidebookFoliage() {
        return config().guidebookFoliage();
    }

    public static void setGuidebookFoliage(boolean foliage) {
        config().setGuidebookFoliage(foliage);
    }

    private static SparkAssistConfig config() {
        return SparkAssistClient.configManager().config();
    }
}
