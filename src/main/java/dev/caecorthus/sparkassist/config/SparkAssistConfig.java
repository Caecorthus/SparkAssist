package dev.caecorthus.sparkassist.config;

import com.google.gson.JsonObject;
import dev.caecorthus.sparkassist.sound.EventSoundGroup;
import java.util.EnumMap;
import net.minecraft.util.TranslatableOption;

/**
 * Small client config model for SparkAssist.
 * SparkAssist 的本地客户端配置模型。
 */
public final class SparkAssistConfig {
    private static final String INSTINCT_KEY_MODE = "instinctKeyMode";
    private static final String EVENT_SOUND_VOLUME = "eventSoundVolume";
    private static final String EVENT_SOUND_VOLUMES = "eventSoundVolumes";
    private static final String GUIDEBOOK_DECOR = "guidebookDecor";
    private static final String GUIDEBOOK_FOLIAGE = "guidebookFoliage";

    private InstinctKeyMode instinctKeyMode = InstinctKeyMode.HOLD;
    private GuidebookDecor guidebookDecor = GuidebookDecor.MEDIUM;
    private boolean guidebookFoliage = true;
    private final EnumMap<EventSoundGroup, Double> eventSoundVolumes = new EnumMap<>(EventSoundGroup.class);

    private SparkAssistConfig() {
        setAllEventSoundVolumes(1.0D);
    }

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
            config.setAllEventSoundVolumes(json.get(EVENT_SOUND_VOLUME).getAsDouble());
        }
        if (json.has(GUIDEBOOK_DECOR)) {
            config.setGuidebookDecor(GuidebookDecor.fromSerialized(json.get(GUIDEBOOK_DECOR).getAsString()));
        }
        if (json.has(GUIDEBOOK_FOLIAGE) && json.get(GUIDEBOOK_FOLIAGE).isJsonPrimitive()
                && json.get(GUIDEBOOK_FOLIAGE).getAsJsonPrimitive().isBoolean()) {
            config.setGuidebookFoliage(json.get(GUIDEBOOK_FOLIAGE).getAsBoolean());
        }
        if (json.has(EVENT_SOUND_VOLUMES) && json.get(EVENT_SOUND_VOLUMES).isJsonObject()) {
            JsonObject eventVolumes = json.getAsJsonObject(EVENT_SOUND_VOLUMES);
            for (String key : eventVolumes.keySet()) {
                EventSoundGroup group = EventSoundGroup.fromSerialized(key);
                if (group != null) {
                    config.setEventSoundVolume(group, eventVolumes.get(key).getAsDouble());
                }
            }
        }
        return config;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty(INSTINCT_KEY_MODE, this.instinctKeyMode.serializedName());
        JsonObject eventVolumes = new JsonObject();
        for (EventSoundGroup group : EventSoundGroup.values()) {
            eventVolumes.addProperty(group.serializedName(), eventSoundVolume(group));
        }
        json.add(EVENT_SOUND_VOLUMES, eventVolumes);
        json.addProperty(GUIDEBOOK_DECOR, guidebookDecor.serializedName());
        json.addProperty(GUIDEBOOK_FOLIAGE, guidebookFoliage);
        return json;
    }

    public GuidebookDecor guidebookDecor() {
        return guidebookDecor;
    }

    public void setGuidebookDecor(GuidebookDecor guidebookDecor) {
        this.guidebookDecor = guidebookDecor == null ? GuidebookDecor.MEDIUM : guidebookDecor;
    }

    public boolean guidebookFoliage() {
        return guidebookFoliage;
    }

    public void setGuidebookFoliage(boolean guidebookFoliage) {
        this.guidebookFoliage = guidebookFoliage;
    }

    public InstinctKeyMode instinctKeyMode() {
        return instinctKeyMode;
    }

    public void setInstinctKeyMode(InstinctKeyMode instinctKeyMode) {
        this.instinctKeyMode = instinctKeyMode == null ? InstinctKeyMode.HOLD : instinctKeyMode;
    }

    public double eventSoundVolume(EventSoundGroup group) {
        if (group == null) {
            return 1.0D;
        }
        return eventSoundVolumes.getOrDefault(group, 1.0D);
    }

    public void setEventSoundVolume(EventSoundGroup group, double eventSoundVolume) {
        if (group == null) {
            return;
        }
        eventSoundVolumes.put(group, clampVolume(eventSoundVolume));
    }

    private void setAllEventSoundVolumes(double eventSoundVolume) {
        double clampedVolume = clampVolume(eventSoundVolume);
        for (EventSoundGroup group : EventSoundGroup.values()) {
            eventSoundVolumes.put(group, clampedVolume);
        }
    }

    public static double clampVolume(double value) {
        if (Double.isNaN(value)) {
            return 1.0D;
        }
        return Math.max(0.0D, Math.min(1.0D, value));
    }

    /** How much guidebook decoration to draw; {@link #density()} is the generators' 0..2 scale, -1 for off.
     * 指南点缀的多少；density() 为生成器使用的 0 到 2 档，关为 -1。 */
    public enum GuidebookDecor implements TranslatableOption {
        OFF(0, "off", "option.sparkassist.guidebook_decor.off", -1),
        LIGHT(1, "light", "option.sparkassist.guidebook_decor.light", 0),
        MEDIUM(2, "medium", "option.sparkassist.guidebook_decor.medium", 1),
        FULL(3, "full", "option.sparkassist.guidebook_decor.full", 2);

        private final int id;
        private final String serializedName;
        private final String translationKey;
        private final int density;

        GuidebookDecor(int id, String serializedName, String translationKey, int density) {
            this.id = id;
            this.serializedName = serializedName;
            this.translationKey = translationKey;
            this.density = density;
        }

        public static GuidebookDecor fromSerialized(String serializedName) {
            for (GuidebookDecor value : values()) {
                if (value.serializedName.equals(serializedName)) {
                    return value;
                }
            }
            return MEDIUM;
        }

        public String serializedName() {
            return serializedName;
        }

        public int density() {
            return density;
        }

        @Override
        public int getId() {
            return id;
        }

        @Override
        public String getTranslationKey() {
            return translationKey;
        }
    }

    public enum InstinctKeyMode implements TranslatableOption {
        HOLD(0, "hold", "option.sparkassist.instinct_key_mode.hold"),
        TOGGLE(1, "toggle", "option.sparkassist.instinct_key_mode.toggle");

        private final int id;
        private final String serializedName;
        private final String translationKey;

        InstinctKeyMode(int id, String serializedName, String translationKey) {
            this.id = id;
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

        @Override
        public int getId() {
            return id;
        }

        @Override
        public String getTranslationKey() {
            return translationKey;
        }
    }
}
