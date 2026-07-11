package dev.caecorthus.sparkassist.sound;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

class EventSoundCatalogTest {
    @Test
    void rejectsDuplicateIdentifierOwnership() {
        Map<Identifier, EventSoundGroup> index = new HashMap<>();
        Identifier id = Identifier.of("sparktraits", "music.shared");

        EventSoundCatalog.putUnique(index, id, EventSoundGroup.ARROGANT_ASF_MUSIC, "event");

        assertThrows(IllegalArgumentException.class, () ->
                EventSoundCatalog.putUnique(index, id, EventSoundGroup.DEPRESSION_PSYCHO_MUSIC, "event"));
    }

    @Test
    void rejectsOwnershipConflictsAcrossIdentifierFamilies() {
        Identifier id = Identifier.of("sparktraits", "music.shared");
        Map<Identifier, EventSoundGroup> eventIds = Map.of(
                id,
                EventSoundGroup.ARROGANT_ASF_MUSIC
        );
        Map<Identifier, EventSoundGroup> resourceIds = Map.of(
                id,
                EventSoundGroup.DEPRESSION_PSYCHO_MUSIC
        );

        assertThrows(IllegalArgumentException.class, () ->
                EventSoundCatalog.validateCrossFamilyOwnership(eventIds, resourceIds));
    }
}
