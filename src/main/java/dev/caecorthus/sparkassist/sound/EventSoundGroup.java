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
    PIG_CHASE("pig_chase", "option.sparkassist.event_sound_volume.pig_chase"),
    ARROGANT_ASF_MUSIC("arrogant_asf_music", "option.sparkassist.event_sound_volume.arrogant_asf_music"),
    DEPRESSION_PSYCHO_RANGE("depression_psycho_range", "option.sparkassist.event_sound_volume.depression_psycho_range"),
    DEPRESSION_PSYCHO_MUSIC("depression_psycho_music", "option.sparkassist.event_sound_volume.depression_psycho_music"),
    DEPRESSION_PSYCHO_ALERT("depression_psycho_alert", "option.sparkassist.event_sound_volume.depression_psycho_alert");

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
