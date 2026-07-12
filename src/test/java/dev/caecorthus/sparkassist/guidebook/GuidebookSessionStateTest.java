package dev.caecorthus.sparkassist.guidebook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GuidebookSessionStateTest {
    @Test
    void remembersObservedRolesAndOwnerVisibleTraitsThroughEmptyDeathObservations() {
        GuidebookSessionState state = new GuidebookSessionState();
        state.startRound();

        state.observe(
                "wathe:civilian",
                List.of("sparktraits:cautious", "sparktraits:last_stand")
        );
        state.observe(null, List.of());

        assertTrue(state.roundActive());
        assertEquals(Set.of("wathe:civilian"), state.observedRoleIds());
        assertEquals(Set.of(
                "sparktraits:cautious",
                "sparktraits:last_stand"
        ), state.observedTraitIds());
        assertEquals("wathe:civilian", state.currentRoleId().orElseThrow());
        assertThrows(UnsupportedOperationException.class, () ->
                state.observedRoleIds().add("wathe:killer"));
    }

    @Test
    void flagsEachCurrentRoleTransitionOnlyOnceForAutoSelection() {
        GuidebookSessionState state = new GuidebookSessionState();
        state.startRound();

        state.observe("wathe:civilian", List.of());
        assertEquals("wathe:civilian", state.consumeRoleAutoSelection().orElseThrow());
        assertTrue(state.consumeRoleAutoSelection().isEmpty());

        state.observe("wathe:civilian", List.of("sparktraits:cautious"));
        assertTrue(state.consumeRoleAutoSelection().isEmpty());

        state.observe("wathe:loose_end", List.of());
        assertEquals("wathe:loose_end", state.consumeRoleAutoSelection().orElseThrow());
        assertTrue(state.consumeRoleAutoSelection().isEmpty());
        assertEquals(Set.of("wathe:civilian", "wathe:loose_end"), state.observedRoleIds());
    }

    @Test
    void preservesChosenTabAndEntryForLaterOpensInTheSameRound() {
        GuidebookSessionState state = new GuidebookSessionState();
        state.startRound();
        state.rememberSelection(GuidebookTab.TRAIT, "sparktraits:cautious");

        state.observe(null, List.of());

        assertEquals(GuidebookTab.TRAIT, state.selectedTab().orElseThrow());
        assertEquals("sparktraits:cautious", state.selectedEntryId().orElseThrow());
    }

    @Test
    void preservesPageAndScrollPositionForLaterOpensInTheSameRound() {
        GuidebookSessionState state = new GuidebookSessionState();
        state.startRound();
        state.rememberSelection(GuidebookTab.ROLE, "wathe:veteran");
        state.rememberViewPosition(2, 39, 17);

        assertEquals(2, state.selectedPage());
        assertEquals(39, state.leftScroll());
        assertEquals(17, state.rightScroll());
    }

    @Test
    void roundEndAndDisconnectClearDiscoverySelectionAndPendingRole() {
        GuidebookSessionState state = populatedState();

        state.endRound();

        assertCleared(state);

        state.startRound();
        state.observe("wathe:killer", List.of("sparktraits:conscience"));
        state.rememberSelection(GuidebookTab.ROLE, "wathe:killer");
        state.disconnect();

        assertCleared(state);
    }

    @Test
    void observationsOutsideAnActiveRoundDoNotLeakIntoTheNextRound() {
        GuidebookSessionState state = new GuidebookSessionState();

        state.observe("wathe:killer", List.of("sparktraits:conscience"));
        state.rememberSelection(GuidebookTab.ROLE, "wathe:killer");
        state.startRound();

        assertTrue(state.observedRoleIds().isEmpty());
        assertTrue(state.observedTraitIds().isEmpty());
        assertTrue(state.selectedTab().isEmpty());
        assertTrue(state.consumeRoleAutoSelection().isEmpty());
    }

    private static GuidebookSessionState populatedState() {
        GuidebookSessionState state = new GuidebookSessionState();
        state.startRound();
        state.observe("wathe:civilian", List.of("sparktraits:cautious"));
        state.rememberSelection(GuidebookTab.TRAIT, "sparktraits:cautious");
        return state;
    }

    private static void assertCleared(GuidebookSessionState state) {
        assertFalse(state.roundActive());
        assertTrue(state.observedRoleIds().isEmpty());
        assertTrue(state.observedTraitIds().isEmpty());
        assertTrue(state.currentRoleId().isEmpty());
        assertTrue(state.consumeRoleAutoSelection().isEmpty());
        assertTrue(state.selectedTab().isEmpty());
        assertTrue(state.selectedEntryId().isEmpty());
        assertEquals(0, state.selectedPage());
        assertEquals(0, state.leftScroll());
        assertEquals(0, state.rightScroll());
    }
}
