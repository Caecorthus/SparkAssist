package dev.caecorthus.sparkassist.guidebook;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
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
        JsonElement root = JsonParser.parseString(json);
        if (!root.isJsonObject() || !root.getAsJsonObject().has("entries")) {
            throw new IllegalArgumentException("Guidebook document must contain an entries array");
        }

        JsonArray jsonEntries = root.getAsJsonObject().getAsJsonArray("entries");
        List<GuidebookEntry> entries = new ArrayList<>(jsonEntries.size());
        for (JsonElement element : jsonEntries) {
            entries.add(parseEntry(element.getAsJsonObject()));
        }
        return new GuidebookCatalog(entries);
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

    private static GuidebookEntry parseEntry(JsonObject json) {
        String summaryKey = json.get("summaryKey").getAsString();
        List<String> pageKeys = json.has("page_keys")
                ? parseStringList(json.getAsJsonArray("page_keys"))
                : List.of(summaryKey);
        return new GuidebookEntry(
                json.get("id").getAsString(),
                GuidebookTab.valueOf(json.get("tab").getAsString().toUpperCase(Locale.ROOT)),
                json.get("sourceModId").getAsString(),
                json.get("nameKey").getAsString(),
                summaryKey,
                pageKeys,
                parseStringList(json.getAsJsonArray("ownerRoleIds")),
                parseStringList(json.getAsJsonArray("requiredModIds")),
                json.get("order").getAsInt()
        );
    }

    private static List<String> parseStringList(JsonArray values) {
        List<String> result = new ArrayList<>(values.size());
        for (JsonElement value : values) {
            result.add(value.getAsString());
        }
        return result;
    }
}
