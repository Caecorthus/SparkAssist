package dev.caecorthus.sparkassist.sound;

/**
 * Event-level sound groups exposed in SparkAssist's client settings. Serialized names are config keys and must not
 * change; new groups are appended and default to full volume.
 * SparkAssist 设置里暴露的事件级音量分组。序列化名称即配置键，不得更改；新分组追加在末尾，默认满音量。
 */
public enum EventSoundGroup {
    PSYCHO_MODE("psycho_mode", "option.sparkassist.event_sound_volume.psycho_mode"),
    CORRUPT_COP_MOMENT("corrupt_cop_moment", "option.sparkassist.event_sound_volume.corrupt_cop_moment"),
    JESTER_MOMENT("jester_moment", "option.sparkassist.event_sound_volume.jester_moment"),
    TRAIN_OUTSIDE("train_outside", "option.sparkassist.event_sound_volume.train_outside"),
    TRAIN_HORN("train_horn", "option.sparkassist.event_sound_volume.train_horn"),
    PIG_CHASE("pig_chase", "option.sparkassist.event_sound_volume.pig_chase"),
    ARROGANT_ASF_MUSIC("arrogant_asf_music", "option.sparkassist.event_sound_volume.arrogant_asf_music"),
    GRAND_WITCH_CEREMONIAL_SWORD_BGM(
            "grand_witch_ceremonial_sword_bgm",
            "option.sparkassist.event_sound_volume.grand_witch_ceremonial_sword_bgm"
    ),
    BELL_RINGER_TOLL("bell_ringer_toll", "option.sparkassist.event_sound_volume.bell_ringer_toll"),
    DEPRESSION_PSYCHO_RANGE("depression_psycho_range", "option.sparkassist.event_sound_volume.depression_psycho_range"),
    DEPRESSION_PSYCHO_MUSIC("depression_psycho_music", "option.sparkassist.event_sound_volume.depression_psycho_music"),
    DEPRESSION_PSYCHO_ALERT("depression_psycho_alert", "option.sparkassist.event_sound_volume.depression_psycho_alert"),
    TRAIN_INSIDE("train_inside", "option.sparkassist.event_sound_volume.train_inside"),
    BLACKOUT("blackout", "option.sparkassist.event_sound_volume.blackout"),
    ROUND_MUSIC("round_music", "option.sparkassist.event_sound_volume.round_music"),
    SHADOW_JESTER_FINALE("shadow_jester_finale", "option.sparkassist.event_sound_volume.shadow_jester_finale"),
    VULTURE_CURSE("vulture_curse", "option.sparkassist.event_sound_volume.vulture_curse"),
    TIME_STEALER_CHIME("time_stealer_chime", "option.sparkassist.event_sound_volume.time_stealer_chime"),
    SAINT_BELL("saint_bell", "option.sparkassist.event_sound_volume.saint_bell"),
    HOLY_FLASH_TINNITUS("holy_flash_tinnitus", "option.sparkassist.event_sound_volume.holy_flash_tinnitus"),
    SHIELD_CLANG("shield_clang", "option.sparkassist.event_sound_volume.shield_clang"),
    FIEND_MOMENT("fiend_moment", "option.sparkassist.event_sound_volume.fiend_moment");

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
