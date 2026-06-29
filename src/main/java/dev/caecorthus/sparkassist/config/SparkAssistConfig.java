package dev.caecorthus.sparkassist.config;

import com.google.gson.JsonObject;

/**
 * Small client config model for SparkAssist.
 * SparkAssist 的本地客户端配置模型。
 */
public final class SparkAssistConfig {
    private static final String INSTINCT_KEY_MODE = "instinctKeyMode";
    private static final String EVENT_SOUND_VOLUME = "eventSoundVolume";

    private InstinctKeyMode instinctKeyMode = InstinctKeyMode.HOLD;
    private double eventSoundVolume = 1.0D;

    public static SparkAssistConfig defaults() {
        return new SparkAssistConfig();
    }

    public static SparkAssistConfig fromJson(JsonObject json) {
        SparkAssistConfig config = defaults();
        if (json == null) {
            return config;
        }
        if (json.has(INSTINCT_KEY_MODE)) {
            config.setInstinctKeyMode(InstinctKeyMode.fromSerialized(json.get(INSTINCT_KEY_MODE).getAsString()));
        }
        if (json.has(EVENT_SOUND_VOLUME)) {
            config.setEventSoundVolume(json.get(EVENT_SOUND_VOLUME).getAsDouble());
        }
        return config;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty(INSTINCT_KEY_MODE, this.instinctKeyMode.serializedName());
        json.addProperty(EVENT_SOUND_VOLUME, this.eventSoundVolume);
        return json;
    }

    public InstinctKeyMode instinctKeyMode() {
        return instinctKeyMode;
    }

    public void setInstinctKeyMode(InstinctKeyMode instinctKeyMode) {
        this.instinctKeyMode = instinctKeyMode == null ? InstinctKeyMode.HOLD : instinctKeyMode;
    }

    public double eventSoundVolume() {
        return eventSoundVolume;
    }

    public void setEventSoundVolume(double eventSoundVolume) {
        this.eventSoundVolume = clampVolume(eventSoundVolume);
    }

    public static double clampVolume(double value) {
        if (Double.isNaN(value)) {
            return 1.0D;
        }
        return Math.max(0.0D, Math.min(1.0D, value));
    }

    public enum InstinctKeyMode {
        HOLD("hold", "option.sparkassist.instinct_key_mode.hold"),
        TOGGLE("toggle", "option.sparkassist.instinct_key_mode.toggle");

        private final String serializedName;
        private final String translationKey;

        InstinctKeyMode(String serializedName, String translationKey) {
            this.serializedName = serializedName;
            this.translationKey = translationKey;
        }

        public static InstinctKeyMode fromSerialized(String serializedName) {
            for (InstinctKeyMode mode : values()) {
                if (mode.serializedName.equals(serializedName)) {
                    return mode;
                }
            }
            return HOLD;
        }

        public InstinctKeyMode next() {
            return this == HOLD ? TOGGLE : HOLD;
        }

        public String serializedName() {
            return serializedName;
        }

        public String translationKey() {
            return translationKey;
        }
    }
}
