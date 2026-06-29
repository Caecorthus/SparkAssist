package dev.caecorthus.sparkassist.sound;

/**
 * Event-level sound groups exposed in SparkAssist's client settings.
 * SparkAssist 设置里暴露的事件级音量分组。
 */
public enum EventSoundGroup {
    PSYCHO_MODE("psycho_mode", "option.sparkassist.event_sound_volume.psycho_mode"),
    CORRUPT_COP_MOMENT("corrupt_cop_moment", "option.sparkassist.event_sound_volume.corrupt_cop_moment"),
    JESTER_MOMENT("jester_moment", "option.sparkassist.event_sound_volume.jester_moment"),
    TRAIN_OUTSIDE("train_outside", "option.sparkassist.event_sound_volume.train_outside"),
    TRAIN_HORN("train_horn", "option.sparkassist.event_sound_volume.train_horn"),
    PIG_CHASE("pig_chase", "option.sparkassist.event_sound_volume.pig_chase");

    private final String serializedName;
    private final String translationKey;

    EventSoundGroup(String serializedName, String translationKey) {
        this.serializedName = serializedName;
        this.translationKey = translationKey;
    }

    public static EventSoundGroup fromSerialized(String serializedName) {
        for (EventSoundGroup group : values()) {
            if (group.serializedName.equals(serializedName)) {
                return group;
            }
        }
        return null;
    }

    public String serializedName() {
        return serializedName;
    }

    public String translationKey() {
        return translationKey;
    }
}
