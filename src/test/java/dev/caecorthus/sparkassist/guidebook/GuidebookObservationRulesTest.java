package dev.caecorthus.sparkassist.guidebook;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GuidebookObservationRulesTest {
    @Test
    void pollsOwnerTraitsOnlyWhileTheRoundPlayerIsAliveAndNotSpectating() {
        assertTrue(GuidebookObservationRules.shouldPollOwnerTraits(true, false, false, false));
        assertFalse(GuidebookObservationRules.shouldPollOwnerTraits(true, true, false, false));
        assertFalse(GuidebookObservationRules.shouldPollOwnerTraits(true, false, true, false));
        assertFalse(GuidebookObservationRules.shouldPollOwnerTraits(true, false, false, true));
        assertFalse(GuidebookObservationRules.shouldPollOwnerTraits(false, false, false, false));
    }
}
