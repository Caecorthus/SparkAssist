package dev.caecorthus.sparkassist.sound;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

class EventSoundVolumeRulesTest {
    @Test
    void groupsRequestedEventAndResourceSounds() {
        assertEquals(EventSoundGroup.PSYCHO_MODE,
                EventSoundVolumeRules.groupFor(Identifier.of("wathe", "ambient.psycho_drone"), null));
        assertEquals(EventSoundGroup.PSYCHO_MODE,
                EventSoundVolumeRules.groupFor(null, Identifier.of("wathe", "ambient/psycho_drone")));
        assertEquals(EventSoundGroup.CORRUPT_COP_MOMENT,
                EventSoundVolumeRules.groupFor(Identifier.of("noellesroles", "music.corrupt_cop_moment_1"), null));
        assertEquals(EventSoundGroup.CORRUPT_COP_MOMENT,
                EventSoundVolumeRules.groupFor(null, Identifier.of("noellesroles", "ambient/boyi")));
        assertEquals(EventSoundGroup.JESTER_MOMENT,
                EventSoundVolumeRules.groupFor(Identifier.of("noellesroles", "music.jester_moment"), null));
        assertEquals(EventSoundGroup.JESTER_MOMENT,
                EventSoundVolumeRules.groupFor(null, Identifier.of("noellesroles", "ambient/dbd_jester")));
        assertEquals(EventSoundGroup.TRAIN_OUTSIDE,
                EventSoundVolumeRules.groupFor(Identifier.of("wathe", "ambient.train.outside"), null));
        assertEquals(EventSoundGroup.TRAIN_HORN,
                EventSoundVolumeRules.groupFor(Identifier.of("wathe", "ambient.train.horn"), null));
        assertEquals(EventSoundGroup.TRAIN_HORN,
                EventSoundVolumeRules.groupFor(null, Identifier.of("wathe", "ambient/ship_outside")));
        assertEquals(EventSoundGroup.PIG_CHASE,
                EventSoundVolumeRules.groupFor(Identifier.of("sparkwitch", "skill.pig_chase"), null));
        assertEquals(EventSoundGroup.PIG_CHASE,
                EventSoundVolumeRules.groupFor(null, Identifier.of("sparkwitch", "skill/pig_chase")));

        assertFalse(EventSoundVolumeRules.isEventSound(Identifier.of("minecraft", "entity.player.levelup")));
        assertFalse(EventSoundVolumeRules.isEventSound(Identifier.of("wathe", "item.revolver.shoot")));
    }

    @Test
    void scalesGroupedSoundOnly() {
        assertEquals(2.0F, EventSoundVolumeRules.scaledVolume(
                Identifier.of("wathe", "ambient.train.horn"), null, 10.0F, 0.2D));
        assertEquals(5.0F, EventSoundVolumeRules.scaledVolume(
                null, Identifier.of("sparkwitch", "skill/pig_chase"), 10.0F, 0.5D));
        assertEquals(10.0F, EventSoundVolumeRules.scaledVolume(
                Identifier.of("wathe", "item.revolver.shoot"), null, 10.0F, 0.2D));
    }
}
