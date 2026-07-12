package dev.caecorthus.sparkassist.guidebook;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class GuidebookSessionState {
    private final Set<String> observedRoleIds = new LinkedHashSet<>();
    private final Set<String> observedTraitIds = new LinkedHashSet<>();

    private boolean roundActive;
    private String currentRoleId;
    private String pendingRoleAutoSelection;
    private GuidebookTab selectedTab;
    private String selectedEntryId;
    private int selectedPage;
    private int leftScroll;
    private int rightScroll;

    public void startRound() {
        clear();
        roundActive = true;
    }

    public void endRound() {
        clear();
    }

    public void disconnect() {
        clear();
    }

    public boolean roundActive() {
        return roundActive;
    }

    public void observe(String roleId, Collection<String> ownerVisibleOrRevealedTraitIds) {
        if (!roundActive) {
            return;
        }

        // Empty death/sync observations never erase discoveries from this round.
        // 死亡或同步空档中的空观察不会抹去本局已经发现的内容。
        if (isPresent(roleId)) {
            observedRoleIds.add(roleId);
            if (!roleId.equals(currentRoleId)) {
                currentRoleId = roleId;
                pendingRoleAutoSelection = roleId;
            }
        }
        if (ownerVisibleOrRevealedTraitIds != null) {
            ownerVisibleOrRevealedTraitIds.stream()
                    .filter(GuidebookSessionState::isPresent)
                    .forEach(observedTraitIds::add);
        }
    }

    public Set<String> observedRoleIds() {
        return immutableSnapshot(observedRoleIds);
    }

    public Set<String> observedTraitIds() {
        return immutableSnapshot(observedTraitIds);
    }

    public Optional<String> currentRoleId() {
        return Optional.ofNullable(currentRoleId);
    }

    public Optional<String> consumeRoleAutoSelection() {
        String roleId = pendingRoleAutoSelection;
        pendingRoleAutoSelection = null;
        return Optional.ofNullable(roleId);
    }

    public void rememberSelection(GuidebookTab tab, String entryId) {
        if (!roundActive) {
            return;
        }
        selectedTab = Objects.requireNonNull(tab, "tab");
        String nextEntryId = Objects.requireNonNull(entryId, "entryId");
        if (!nextEntryId.equals(selectedEntryId)) {
            selectedPage = 0;
            rightScroll = 0;
        }
        selectedEntryId = nextEntryId;
    }

    public void rememberViewPosition(int page, int left, int right) {
        if (!roundActive) {
            return;
        }
        selectedPage = Math.max(0, page);
        leftScroll = Math.max(0, left);
        rightScroll = Math.max(0, right);
    }

    public Optional<GuidebookTab> selectedTab() {
        return Optional.ofNullable(selectedTab);
    }

    public Optional<String> selectedEntryId() {
        return Optional.ofNullable(selectedEntryId);
    }

    public int selectedPage() {
        return selectedPage;
    }

    public int leftScroll() {
        return leftScroll;
    }

    public int rightScroll() {
        return rightScroll;
    }

    private void clear() {
        roundActive = false;
        observedRoleIds.clear();
        observedTraitIds.clear();
        currentRoleId = null;
        pendingRoleAutoSelection = null;
        selectedTab = null;
        selectedEntryId = null;
        selectedPage = 0;
        leftScroll = 0;
        rightScroll = 0;
    }

    private static boolean isPresent(String id) {
        return id != null && !id.isBlank();
    }

    private static Set<String> immutableSnapshot(Set<String> values) {
        return Collections.unmodifiableSet(new LinkedHashSet<>(values));
    }
}
