package dev.caecorthus.sparkassist.guidebook;

import dev.caecorthus.sparkassist.guidebook.data.GuidebookJsonParser;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class GuidebookCatalog {
    private static final Comparator<GuidebookEntry> ENTRY_ORDER = Comparator
            .comparingInt(GuidebookEntry::order)
            .thenComparing(GuidebookEntry::id);

    private final List<GuidebookEntry> entries;
    private final Map<String, GuidebookEntry> entriesById;

    private GuidebookCatalog(List<GuidebookEntry> entries) {
        List<GuidebookEntry> sortedEntries = entries.stream().sorted(ENTRY_ORDER).toList();
        Map<String, GuidebookEntry> index = new LinkedHashMap<>();
        for (GuidebookEntry entry : sortedEntries) {
            if (index.putIfAbsent(entry.id(), entry) != null) {
                throw new IllegalArgumentException("Duplicate guidebook entry id: " + entry.id());
            }
        }
        this.entries = sortedEntries;
        this.entriesById = Map.copyOf(index);
    }

    public static GuidebookCatalog parse(String json) {
        return new GuidebookCatalog(GuidebookJsonParser.parseEntries(json));
    }

    public static GuidebookCatalog of(Collection<GuidebookEntry> entries) {
        return new GuidebookCatalog(new ArrayList<>(entries));
    }

    public static GuidebookCatalog merge(Collection<GuidebookCatalog> catalogs) {
        List<GuidebookEntry> entries = new ArrayList<>();
        for (GuidebookCatalog catalog : catalogs) {
            entries.addAll(catalog.entries);
        }
        return new GuidebookCatalog(entries);
    }

    /**
     * Keeps authored reference pages browsable while applying mod availability to runtime discoveries.
     * 保留所有人工编写的参考页面，并只按模组可用性筛选运行时发现的补充条目。
     */
    public static GuidebookCatalog mergeAuthoredWithAvailableDiscoveries(
            GuidebookCatalog authored,
            GuidebookCatalog discoveries,
            Set<String> loadedModIds
    ) {
        return merge(List.of(authored, discoveries.availableFor(loadedModIds)));
    }

    public GuidebookCatalog availableFor(Set<String> loadedModIds) {
        return new GuidebookCatalog(entries.stream()
                .filter(entry -> loadedModIds.containsAll(entry.requiredModIds()))
                .toList());
    }

    public List<GuidebookEntry> entries() {
        return entries;
    }

    public Optional<GuidebookEntry> find(String id) {
        return Optional.ofNullable(entriesById.get(id));
    }
}
