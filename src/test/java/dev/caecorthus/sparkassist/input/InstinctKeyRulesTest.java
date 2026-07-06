package dev.caecorthus.sparkassist.input;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class InstinctKeyRulesTest {
    @Test
    void handlesOnlyExactWatheInstinctTranslationKeyWhenConfigExists() {
        assertTrue(InstinctKeyRules.shouldHandle("key.wathe.instinct", true));

        assertFalse(InstinctKeyRules.shouldHandle("key.wathe.instinct.extra", true));
        assertFalse(InstinctKeyRules.shouldHandle("key.wathe", true));
        assertFalse(InstinctKeyRules.shouldHandle("key.noellesroles.instinct", true));
        assertFalse(InstinctKeyRules.shouldHandle("KEY.WATHE.INSTINCT", true));
        assertFalse(InstinctKeyRules.shouldHandle(null, true));
    }

    @Test
    void leavesVanillaKeyBehaviorAloneWhenConfigIsUnavailable() {
        assertFalse(InstinctKeyRules.shouldHandle("key.wathe.instinct", false));
    }
}
