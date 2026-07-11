package dev.caecorthus.sparkassist.sound;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

class EventSoundVolumeRulesTest {
    @Test
    void mapsEveryEventGroupAndBothIdentifierFamilies() {
        assertMatches(EventSoundGroup.PSYCHO_MODE, "wathe", "ambient.psycho_drone", "ambient/psycho_drone");
        assertMatches(EventSoundGroup.CORRUPT_COP_MOMENT, "noellesroles", "music.corrupt_cop_moment_1", "ambient/boyi");
        assertMatches(EventSoundGroup.JESTER_MOMENT, "noellesroles", "music.jester_moment", "ambient/dbd_jester");
        assertMatches(EventSoundGroup.TRAIN_OUTSIDE, "wathe", "ambient.train.outside", "ambient/train_outside");
        assertMatches(EventSoundGroup.TRAIN_HORN, "wathe", "ambient.train.horn", "ambient/train_horn");
        assertMatches(EventSoundGroup.PIG_CHASE, "sparkwitch", "skill.pig_chase", "skill/pig_chase");
        assertMatches(EventSoundGroup.ARROGANT_ASF_MUSIC, "sparkstrength", "music.takediskrush", "music/takediskrush");
        assertMatches(
                EventSoundGroup.GRAND_WITCH_CEREMONIAL_SWORD_BGM,
                "sparkwitch",
                "ambient.grand_witch_ceremonial_sword_bgm",
                "ambient/grand_witch_ceremonial_sword_bgm"
        );
        assertMatches(EventSoundGroup.DEPRESSION_PSYCHO_RANGE, "sparktraits", "depression.rage_loop", "depression/rage_loop");
        assertMatches(EventSoundGroup.DEPRESSION_PSYCHO_MUSIC, "sparktraits", "depression.blind_rage_chase", "depression/blind_rage_chase");
        assertMatches(EventSoundGroup.DEPRESSION_PSYCHO_ALERT, "sparktraits", "depression.player_was_seen", "depression/player_was_seen");
    }

    @Test
    void leavesUnrelatedSoundsUnchanged() {
        assertFalse(EventSoundVolumeRules.isEventSound(Identifier.of("minecraft", "entity.player.levelup")));
        assertFalse(EventSoundVolumeRules.isEventSound(Identifier.of("wathe", "item.revolver.shoot")));
        assertFalse(EventSoundVolumeRules.isEventSound(Identifier.of("sparktraits", "item.flashlight.toggle")));
        assertFalse(EventSoundVolumeRules.isEventSound(Identifier.of("sparkwitch", "ambient.random_witch_sound")));
        assertEquals(10.0F, EventSoundVolumeRules.scaledVolume(
                Identifier.of("wathe", "item.revolver.shoot"), null, 10.0F, 0.2D));
    }

    @Test
    void scalesOnlyCataloguedSoundsWithClampedVolume() {
        assertEquals(2.0F, EventSoundVolumeRules.scaledVolume(
                Identifier.of("wathe", "ambient.train.horn"), null, 10.0F, 0.2D));
        assertEquals(0.0F, EventSoundVolumeRules.scaledVolume(
                Identifier.of("sparktraits", "depression.rage_loop"), null, 10.0F, -1.0D));
        assertEquals(10.0F, EventSoundVolumeRules.scaledVolume(
                Identifier.of("sparkwitch", "skill.pig_chase"), null, 10.0F, Double.NaN));
        assertTrue(EventSoundVolumeRules.isEventSound(
                Identifier.of("sparkwitch", "ambient.grand_witch_ceremonial_sword_bgm")));
    }

    private static void assertMatches(
            EventSoundGroup group,
            String namespace,
            String eventPath,
            String resourcePath
    ) {
        assertEquals(group, EventSoundVolumeRules.groupFor(Identifier.of(namespace, eventPath), null));
        assertEquals(group, EventSoundVolumeRules.groupFor(null, Identifier.of(namespace, resourcePath)));
    }
}
