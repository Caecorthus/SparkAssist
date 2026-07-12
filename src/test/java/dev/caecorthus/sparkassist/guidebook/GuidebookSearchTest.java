package dev.caecorthus.sparkassist.guidebook;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class GuidebookSearchTest {
    private static final GuidebookEntry GRAND_WITCH = new GuidebookEntry(
            "sparkwitch:grand_witch",
            GuidebookTab.ROLE,
            "sparkwitch",
            "announcement.role.grand_witch",
            "guidebook.sparkwitch.grand_witch.summary",
            List.of("guidebook.sparkwitch.grand_witch.summary"),
            List.of("sparkwitch:grand_witch", "sparkwitch:apprentice_witch"),
            List.of("sparkwitch"),
            10
    );

    @Test
    void matchesLocalizedDisplayNameCaseInsensitively() {
        assertTrue(GuidebookSearch.matches(GRAND_WITCH, "Grand Witch", "  wItCh  "));
    }

    @Test
    void matchesSourceModAndOwnerRoleIdsCaseInsensitively() {
        assertTrue(GuidebookSearch.matches(GRAND_WITCH, "Grand Witch", "SPARKWITCH"));
        assertTrue(GuidebookSearch.matches(GRAND_WITCH, "Grand Witch", "APPRENTICE_WITCH"));
    }

    @Test
    void matchesLocalizedOwnerRoleNames() {
        assertTrue(GuidebookSearch.matches(
                GRAND_WITCH,
                "Ceremonial Sword",
                List.of("大魔女"),
                "大魔女"
        ));
    }

    @Test
    void doesNotSearchUnspecifiedMetadata() {
        assertFalse(GuidebookSearch.matches(GRAND_WITCH, "Grand Witch", "grand_witch.summary"));
        assertFalse(GuidebookSearch.matches(GRAND_WITCH, "Grand Witch", "trait"));
    }

    @Test
    void emptyQueryMatchesEveryEntry() {
        assertTrue(GuidebookSearch.matches(GRAND_WITCH, "Grand Witch", "  "));
    }
}
