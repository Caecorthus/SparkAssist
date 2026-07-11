package dev.caecorthus.sparkassist.input;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.caecorthus.sparkassist.config.SparkAssistConfig.InstinctKeyMode;
import org.junit.jupiter.api.Test;

class InstinctToggleStateTest {
    @Test
    void holdModeMirrorsPhysicalKeyAndClearsToggle() {
        InstinctToggleState state = new InstinctToggleState();
        assertTrue(state.effectivePressed(InstinctKeyMode.TOGGLE, true));
        assertTrue(state.toggled());

        assertFalse(state.effectivePressed(InstinctKeyMode.HOLD, false));

        assertFalse(state.toggled());
        assertTrue(state.effectivePressed(InstinctKeyMode.HOLD, true));
    }

    @Test
    void toggleModeChangesOnlyOnRisingEdge() {
        InstinctToggleState state = new InstinctToggleState();

        assertFalse(state.effectivePressed(InstinctKeyMode.TOGGLE, false));
        assertTrue(state.effectivePressed(InstinctKeyMode.TOGGLE, true));
        assertTrue(state.effectivePressed(InstinctKeyMode.TOGGLE, true));
        assertTrue(state.effectivePressed(InstinctKeyMode.TOGGLE, false));
        assertFalse(state.effectivePressed(InstinctKeyMode.TOGGLE, true));
    }
}
