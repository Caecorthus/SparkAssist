package dev.caecorthus.sparkassist.guidebook;

import java.util.List;
import java.util.Objects;

public record GuidebookEntry(
        String id,
        GuidebookTab tab,
        String sourceModId,
        String nameKey,
        String summaryKey,
        List<String> pageKeys,
        List<String> ownerRoleIds,
        List<String> requiredModIds,
        int order
) {
    public GuidebookEntry {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(tab, "tab");
        Objects.requireNonNull(sourceModId, "sourceModId");
        Objects.requireNonNull(nameKey, "nameKey");
        Objects.requireNonNull(summaryKey, "summaryKey");
        pageKeys = List.copyOf(Objects.requireNonNull(pageKeys, "pageKeys"));
        ownerRoleIds = List.copyOf(Objects.requireNonNull(ownerRoleIds, "ownerRoleIds"));
        requiredModIds = List.copyOf(Objects.requireNonNull(requiredModIds, "requiredModIds"));
    }
}
