package dev.caecorthus.sparkassist.client.input;

import dev.caecorthus.sparkassist.client.SparkAssistClient;
import dev.caecorthus.sparkassist.config.SparkAssistConfig.InstinctKeyMode;
import dev.caecorthus.sparkassist.input.InstinctToggleState;

public final class InstinctKeyController {
    private static final String WATHE_INSTINCT_KEY = "key.wathe.instinct";
    private static final InstinctToggleState STATE = new InstinctToggleState();

    private InstinctKeyController() {
    }

    public static boolean shouldHandle(String translationKey) {
        return WATHE_INSTINCT_KEY.equals(translationKey) && SparkAssistClient.configManager() != null;
    }

    public static boolean effectivePressed(boolean physicalDown) {
        InstinctKeyMode mode = SparkAssistClient.configManager().config().instinctKeyMode();
        return STATE.effectivePressed(mode, physicalDown);
    }

    public static void reset() {
        STATE.reset();
    }
}
