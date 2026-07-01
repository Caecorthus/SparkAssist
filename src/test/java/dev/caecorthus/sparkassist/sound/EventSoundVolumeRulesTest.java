package dev.caecorthus.sparkassist.sound;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
        assertEquals(EventSoundGroup.ARROGANT_ASF_MUSIC,
                EventSoundVolumeRules.groupFor(Identifier.of("sparktraits", "music.takediskrush"), null));
        assertEquals(EventSoundGroup.ARROGANT_ASF_MUSIC,
                EventSoundVolumeRules.groupFor(null, Identifier.of("sparktraits", "music/takediskrush")));
        assertMatches(EventSoundGroup.DEPRESSION_PSYCHO_RANGE, "depression.melee_kill_1", "depression/melee_kill_1");
        assertMatches(EventSoundGroup.DEPRESSION_PSYCHO_RANGE, "depression.melee_kill_2", "depression/melee_kill_2");
        assertMatches(EventSoundGroup.DEPRESSION_PSYCHO_RANGE, "depression.docile_to_rage", "depression/docile_to_rage");
        assertMatches(EventSoundGroup.DEPRESSION_PSYCHO_RANGE, "depression.rage_loop", "depression/rage_loop");
        assertMatches(EventSoundGroup.DEPRESSION_PSYCHO_RANGE, "depression.rage_to_docile", "depression/rage_to_docile");
        assertMatches(EventSoundGroup.DEPRESSION_PSYCHO_RANGE, "depression.shyguy_killed", "depression/shyguy_killed");
        assertMatches(EventSoundGroup.DEPRESSION_PSYCHO_MUSIC, "depression.blind_rage_enrage", "depression/blind_rage_enrage");
        assertMatches(EventSoundGroup.DEPRESSION_PSYCHO_MUSIC, "depression.blind_rage_chase", "depression/blind_rage_chase");
        assertMatches(EventSoundGroup.DEPRESSION_PSYCHO_ALERT, "depression.player_was_seen", "depression/player_was_seen");

        assertFalse(EventSoundVolumeRules.isEventSound(Identifier.of("minecraft", "entity.player.levelup")));
        assertFalse(EventSoundVolumeRules.isEventSound(Identifier.of("wathe", "item.revolver.shoot")));
        assertFalse(EventSoundVolumeRules.isEventSound(Identifier.of("sparktraits", "item.flashlight.toggle")));
        assertTrue(EventSoundVolumeRules.isEventSound(Identifier.of("sparktraits", "depression.docile_to_rage")));
        assertTrue(EventSoundVolumeRules.isEventSound(Identifier.of("sparktraits", "depression/rage_loop")));
    }

    @Test
    void scalesGroupedSoundOnly() {
        assertEquals(2.0F, EventSoundVolumeRules.scaledVolume(
                Identifier.of("wathe", "ambient.train.horn"), null, 10.0F, 0.2D));
        assertEquals(5.0F, EventSoundVolumeRules.scaledVolume(
                null, Identifier.of("sparkwitch", "skill/pig_chase"), 10.0F, 0.5D));
        assertEquals(3.0F, EventSoundVolumeRules.scaledVolume(
                Identifier.of("sparktraits", "music.takediskrush"), null, 10.0F, 0.3D));
        assertEquals(1.5F, EventSoundVolumeRules.scaledVolume(
                Identifier.of("sparktraits", "depression.blind_rage_chase"), null, 10.0F, 0.15D));
        assertEquals(4.0F, EventSoundVolumeRules.scaledVolume(
                Identifier.of("sparktraits", "depression.player_was_seen"), null, 10.0F, 0.4D));
        assertEquals(7.5F, EventSoundVolumeRules.scaledVolume(
                null, Identifier.of("sparktraits", "depression/melee_kill_2"), 10.0F, 0.75D));
        assertEquals(2.0F, EventSoundVolumeRules.scaledVolume(
                Identifier.of("sparktraits", "depression.rage_loop"), null, 10.0F, 0.2D));
        assertEquals(10.0F, EventSoundVolumeRules.scaledVolume(
                Identifier.of("wathe", "item.revolver.shoot"), null, 10.0F, 0.2D));
    }

    @Test
    void refreshesAllCategoriesUsedByEventSounds() throws IOException {
        String source = Files.readString(Path.of(
                "src/client/java/dev/caecorthus/sparkassist/client/sound/EventSoundVolumeController.java"
        ));

        assertTrue(source.contains("refreshSoundCategory(client, SoundCategory.AMBIENT);"));
        assertTrue(source.contains("refreshSoundCategory(client, SoundCategory.MUSIC);"));
        assertTrue(source.contains("refreshSoundCategory(client, SoundCategory.PLAYERS);"));
    }

    private static void assertMatches(EventSoundGroup group, String eventPath, String resourcePath) {
        assertEquals(group, EventSoundVolumeRules.groupFor(Identifier.of("sparktraits", eventPath), null));
        assertEquals(group, EventSoundVolumeRules.groupFor(null, Identifier.of("sparktraits", resourcePath)));
    }
}
