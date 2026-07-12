package dev.caecorthus.sparkassist.guidebook;

import java.util.Collection;
import java.util.Locale;
import java.util.List;

public final class GuidebookSearch {
    private GuidebookSearch() {
    }

    public static boolean matches(GuidebookEntry entry, String localizedDisplayName, String query) {
        return matches(entry, localizedDisplayName, List.of(), query);
    }

    public static boolean matches(
            GuidebookEntry entry,
            String localizedDisplayName,
            Collection<String> localizedOwnerRoleNames,
            String query
    ) {
        String needle = normalize(query).trim();
        if (needle.isEmpty()) {
            return true;
        }
        if (normalize(localizedDisplayName).contains(needle)
                || normalize(entry.sourceModId()).contains(needle)) {
            return true;
        }
        return entry.ownerRoleIds().stream()
                .map(GuidebookSearch::normalize)
                .anyMatch(roleId -> roleId.contains(needle))
                || localizedOwnerRoleNames.stream()
                .map(GuidebookSearch::normalize)
                .anyMatch(roleName -> roleName.contains(needle));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
