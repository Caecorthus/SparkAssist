package dev.caecorthus.sparkassist.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonObject;
import dev.caecorthus.sparkassist.config.SparkAssistConfig.InstinctKeyMode;
import org.junit.jupiter.api.Test;

class SparkAssistConfigTest {
    @Test
    void defaultsUseHoldInstinctAndFullEventVolume() {
        SparkAssistConfig config = SparkAssistConfig.defaults();

        assertEquals(InstinctKeyMode.HOLD, config.instinctKeyMode());
        assertEquals(1.0D, config.eventSoundVolume());
    }

    @Test
    void jsonRoundTripKeepsSupportedOptions() {
        SparkAssistConfig config = SparkAssistConfig.defaults();
        config.setInstinctKeyMode(InstinctKeyMode.TOGGLE);
        config.setEventSoundVolume(0.35D);

        SparkAssistConfig roundTrip = SparkAssistConfig.fromJson(config.toJson());

        assertEquals(InstinctKeyMode.TOGGLE, roundTrip.instinctKeyMode());
        assertEquals(0.35D, roundTrip.eventSoundVolume());
    }

    @Test
    void invalidJsonFallsBackSafely() {
        JsonObject json = new JsonObject();
        json.addProperty("instinctKeyMode", "unknown");
        json.addProperty("eventSoundVolume", 8.0D);

        SparkAssistConfig config = SparkAssistConfig.fromJson(json);

        assertEquals(InstinctKeyMode.HOLD, config.instinctKeyMode());
        assertEquals(1.0D, config.eventSoundVolume());
    }
}
