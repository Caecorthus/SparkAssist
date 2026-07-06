package dev.caecorthus.sparkassist.sound;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import net.minecraft.sound.SoundCategory;
import org.junit.jupiter.api.Test;

class EventSoundRefreshCategoriesTest {
    @Test
    void refreshesOnlyCategoriesThatCanContainEventSounds() {
        assertEquals(List.of(
                SoundCategory.AMBIENT,
                SoundCategory.MUSIC,
                SoundCategory.PLAYERS
        ), EventSoundRefreshCategories.refreshCategories());

        assertFalse(EventSoundRefreshCategories.refreshCategories().contains(SoundCategory.BLOCKS));
        assertFalse(EventSoundRefreshCategories.refreshCategories().contains(SoundCategory.MASTER));
        assertFalse(EventSoundRefreshCategories.refreshCategories().contains(SoundCategory.VOICE));
    }

    @Test
    void refreshCategoryMetadataIsImmutable() {
        assertThrows(UnsupportedOperationException.class, () ->
                EventSoundRefreshCategories.refreshCategories().add(SoundCategory.BLOCKS));
    }
}
