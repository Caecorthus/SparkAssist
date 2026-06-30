package dev.caecorthus.sparkassist.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonObject;
import dev.caecorthus.sparkassist.config.SparkAssistConfig.InstinctKeyMode;
import dev.caecorthus.sparkassist.sound.EventSoundGroup;
import org.junit.jupiter.api.Test;

class SparkAssistConfigTest {
    @Test
    void defaultsUseHoldInstinctAndFullPerEventVolumes() {
        SparkAssistConfig config = SparkAssistConfig.defaults();

        assertEquals(InstinctKeyMode.HOLD, config.instinctKeyMode());
        for (EventSoundGroup group : EventSoundGroup.values()) {
            assertEquals(1.0D, config.eventSoundVolume(group));
        }
    }

    @Test
    void jsonRoundTripKeepsSupportedOptions() {
        SparkAssistConfig config = SparkAssistConfig.defaults();
        config.setInstinctKeyMode(InstinctKeyMode.TOGGLE);
        config.setEventSoundVolume(EventSoundGroup.PIG_CHASE, 0.35D);
        config.setEventSoundVolume(EventSoundGroup.TRAIN_HORN, 0.6D);

        SparkAssistConfig roundTrip = SparkAssistConfig.fromJson(config.toJson());

        assertEquals(InstinctKeyMode.TOGGLE, roundTrip.instinctKeyMode());
        assertEquals(0.35D, roundTrip.eventSoundVolume(EventSoundGroup.PIG_CHASE));
        assertEquals(0.6D, roundTrip.eventSoundVolume(EventSoundGroup.TRAIN_HORN));
        assertEquals(1.0D, roundTrip.eventSoundVolume(EventSoundGroup.PSYCHO_MODE));
        assertEquals(1.0D, roundTrip.eventSoundVolume(EventSoundGroup.ARROGANT_ASF_MUSIC));
    }

    @Test
    void legacyGlobalEventVolumeMigratesToAllEventGroups() {
        JsonObject json = new JsonObject();
        json.addProperty("eventSoundVolume", 0.25D);

        SparkAssistConfig config = SparkAssistConfig.fromJson(json);

        for (EventSoundGroup group : EventSoundGroup.values()) {
            assertEquals(0.25D, config.eventSoundVolume(group));
        }
    }

    @Test
    void missingNewEventVolumeFallsBackToFullVolume() {
        JsonObject eventVolumes = new JsonObject();
        eventVolumes.addProperty(EventSoundGroup.PIG_CHASE.serializedName(), 0.35D);
        JsonObject json = new JsonObject();
        json.add("eventSoundVolumes", eventVolumes);

        SparkAssistConfig config = SparkAssistConfig.fromJson(json);

        assertEquals(0.35D, config.eventSoundVolume(EventSoundGroup.PIG_CHASE));
        assertEquals(1.0D, config.eventSoundVolume(EventSoundGroup.ARROGANT_ASF_MUSIC));
    }

    @Test
    void invalidJsonFallsBackSafely() {
        JsonObject json = new JsonObject();
        json.addProperty("instinctKeyMode", "unknown");
        json.addProperty("eventSoundVolume", 8.0D);

        SparkAssistConfig config = SparkAssistConfig.fromJson(json);

        assertEquals(InstinctKeyMode.HOLD, config.instinctKeyMode());
        for (EventSoundGroup group : EventSoundGroup.values()) {
            assertEquals(1.0D, config.eventSoundVolume(group));
        }
    }
}
