package dev.caecorthus.sparkassist.sound;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

class EventSoundVolumeRulesTest {
    @Test
    void includesOnlyRequestedEventSounds() {
        assertTrue(EventSoundVolumeRules.isEventSound(Identifier.of("wathe", "ambient.psycho_drone")));
        assertTrue(EventSoundVolumeRules.isEventSound(Identifier.of("wathe", "ambient.train.outside")));
        assertTrue(EventSoundVolumeRules.isEventSound(Identifier.of("wathe", "ambient.train.horn")));
        assertTrue(EventSoundVolumeRules.isEventSound(Identifier.of("noellesroles", "music.corrupt_cop_moment_1")));
        assertTrue(EventSoundVolumeRules.isEventSound(Identifier.of("noellesroles", "music.jester_moment")));
        assertTrue(EventSoundVolumeRules.isEventSound(Identifier.of("sparkwitch", "skill.pig_chase")));

        assertFalse(EventSoundVolumeRules.isEventSound(Identifier.of("minecraft", "entity.player.levelup")));
        assertFalse(EventSoundVolumeRules.isEventSound(Identifier.of("wathe", "item.revolver.shoot")));
    }

    @Test
    void scalesWhitelistedSoundOnly() {
        assertEquals(2.0F, EventSoundVolumeRules.scaledVolume(
                Identifier.of("wathe", "ambient.train.horn"), 10.0F, 0.2D));
        assertEquals(10.0F, EventSoundVolumeRules.scaledVolume(
                Identifier.of("wathe", "item.revolver.shoot"), 10.0F, 0.2D));
    }
}
