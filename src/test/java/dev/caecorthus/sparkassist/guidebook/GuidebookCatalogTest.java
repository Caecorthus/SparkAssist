package dev.caecorthus.sparkassist.guidebook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GuidebookCatalogTest {
    @Test
    void parsesOneGuidebookDocument() {
        GuidebookCatalog catalog = GuidebookCatalog.parse("""
                {
                  "entries": [
                    {
                      "id": "sparktraits:impostor",
                      "tab": "TRAIT",
                      "sourceModId": "sparktraits",
                      "nameKey": "trait.sparktraits.impostor.name",
                      "summaryKey": "trait.sparktraits.impostor.description",
                      "ownerRoleIds": ["wathe:civilian"],
                      "requiredModIds": ["sparktraits"],
                      "order": 20
                    }
                  ]
                }
                """);

        assertEquals(List.of(new GuidebookEntry(
                "sparktraits:impostor",
                GuidebookTab.TRAIT,
                "sparktraits",
                "trait.sparktraits.impostor.name",
                "trait.sparktraits.impostor.description",
                List.of("trait.sparktraits.impostor.description"),
                List.of("wathe:civilian"),
                List.of("sparktraits"),
                20
        )), catalog.entries());
    }

    @Test
    void omittedPageKeysDefaultToTheSummaryKey() {
        GuidebookCatalog catalog = GuidebookCatalog.parse("""
                {
                  "entries": [
                    {
                      "id": "sparktraits:impostor", "tab": "TRAIT", "sourceModId": "sparktraits",
                      "nameKey": "trait.sparktraits.impostor.name",
                      "summaryKey": "trait.sparktraits.impostor.description",
                      "ownerRoleIds": [], "requiredModIds": [], "order": 20
                    }
                  ]
                }
                """);

        assertEquals(
                List.of("trait.sparktraits.impostor.description"),
                catalog.entries().getFirst().pageKeys()
        );
    }

    @Test
    void parsesExplicitPageKeysAsAnImmutableList() {
        GuidebookCatalog catalog = GuidebookCatalog.parse("""
                {
                  "entries": [
                    {
                      "id": "sparkwitch:grand_witch", "tab": "ROLE", "sourceModId": "sparkwitch",
                      "nameKey": "announcement.role.grand_witch", "summaryKey": "guide.grand_witch.summary",
                      "page_keys": ["guide.grand_witch.overview", "guide.grand_witch.abilities"],
                      "ownerRoleIds": [], "requiredModIds": ["sparkwitch"], "order": 10
                    }
                  ]
                }
                """);

        List<String> pageKeys = catalog.entries().getFirst().pageKeys();
        assertEquals(List.of(
                "guide.grand_witch.overview",
                "guide.grand_witch.abilities"
        ), pageKeys);
        assertThrows(UnsupportedOperationException.class, () -> pageKeys.add("guide.grand_witch.items"));
    }

    @Test
    void entryCollectionsAreImmutableSnapshots() {
        List<String> owners = new ArrayList<>(List.of("wathe:civilian"));
        GuidebookEntry entry = entry("sparktraits:impostor", 20, owners, List.of("sparktraits"));

        owners.add("wathe:veteran");

        assertEquals(List.of("wathe:civilian"), entry.ownerRoleIds());
        assertThrows(UnsupportedOperationException.class, () -> entry.requiredModIds().add("wathe"));
    }

    @Test
    void mergeSortsByOrderThenIdRegardlessOfDocumentOrder() {
        GuidebookCatalog later = catalog(entry("sparktraits:conscience", 20));
        GuidebookCatalog earlierB = catalog(entry("sparkwitch:grand_witch", 10));
        GuidebookCatalog earlierA = catalog(entry("noellesroles:assassin", 10));

        GuidebookCatalog merged = GuidebookCatalog.merge(List.of(later, earlierB, earlierA));

        assertEquals(List.of(
                "noellesroles:assassin",
                "sparkwitch:grand_witch",
                "sparktraits:conscience"
        ), merged.entries().stream().map(GuidebookEntry::id).toList());
    }

    @Test
    void ofDefensivelyCopiesSortsAndRejectsDuplicateIds() {
        GuidebookEntry later = entry("sparktraits:conscience", 20);
        GuidebookEntry earlier = entry("wathe:civilian", 10);
        List<GuidebookEntry> source = new ArrayList<>(List.of(later, earlier));

        GuidebookCatalog catalog = GuidebookCatalog.of(source);
        source.clear();

        assertEquals(List.of(earlier, later), catalog.entries());
        assertThrows(UnsupportedOperationException.class, () -> catalog.entries().add(later));
        assertThrows(IllegalArgumentException.class, () -> GuidebookCatalog.of(List.of(later, later)));
    }

    @Test
    void rejectsDuplicateIdsWithinOrAcrossDocuments() {
        assertThrows(IllegalArgumentException.class, () -> GuidebookCatalog.parse("""
                {
                  "entries": [
                    {
                      "id": "sparktraits:impostor", "tab": "TRAIT", "sourceModId": "sparktraits",
                      "nameKey": "name.one", "summaryKey": "summary.one",
                      "ownerRoleIds": [], "requiredModIds": [], "order": 1
                    },
                    {
                      "id": "sparktraits:impostor", "tab": "TRAIT", "sourceModId": "sparktraits",
                      "nameKey": "name.two", "summaryKey": "summary.two",
                      "ownerRoleIds": [], "requiredModIds": [], "order": 2
                    }
                  ]
                }
                """));

        GuidebookCatalog left = catalog(entry("sparktraits:impostor", 1));
        GuidebookCatalog right = catalog(entry("sparktraits:impostor", 2));
        assertThrows(IllegalArgumentException.class, () -> GuidebookCatalog.merge(List.of(left, right)));
    }

    @Test
    void filtersEntriesWhoseRequiredOptionalModsAreAbsent() {
        GuidebookEntry base = entry("wathe:civilian", 1, List.of(), List.of("wathe"));
        GuidebookEntry witch = entry(
                "sparkwitch:grand_witch",
                2,
                List.of(),
                List.of("wathe", "sparkwitch")
        );

        GuidebookCatalog available = catalog(base, witch).availableFor(Set.of("wathe"));

        assertEquals(List.of(base), available.entries());
    }

    @Test
    void findsEntriesByExactId() {
        GuidebookEntry entry = entry("sparktraits:impostor", 1);
        GuidebookCatalog catalog = catalog(entry);

        assertEquals(entry, catalog.find("sparktraits:impostor").orElseThrow());
        assertTrue(catalog.find("sparktraits:missing").isEmpty());
    }

    private static GuidebookCatalog catalog(GuidebookEntry... entries) {
        StringBuilder json = new StringBuilder("{\"entries\":[");
        for (int index = 0; index < entries.length; index++) {
            if (index > 0) {
                json.append(',');
            }
            GuidebookEntry entry = entries[index];
            json.append("{\"id\":\"").append(entry.id()).append("\"")
                    .append(",\"tab\":\"").append(entry.tab()).append("\"")
                    .append(",\"sourceModId\":\"").append(entry.sourceModId()).append("\"")
                    .append(",\"nameKey\":\"").append(entry.nameKey()).append("\"")
                    .append(",\"summaryKey\":\"").append(entry.summaryKey()).append("\"")
                    .append(",\"ownerRoleIds\":");
            appendJsonArray(json, entry.ownerRoleIds());
            json.append(",\"requiredModIds\":");
            appendJsonArray(json, entry.requiredModIds());
            json
                    .append(",\"order\":").append(entry.order()).append('}');
        }
        return GuidebookCatalog.parse(json.append("]}").toString());
    }

    private static void appendJsonArray(StringBuilder json, List<String> values) {
        json.append('[');
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                json.append(',');
            }
            json.append('"').append(values.get(index)).append('"');
        }
        json.append(']');
    }

    private static GuidebookEntry entry(String id, int order) {
        return entry(id, order, List.of(), List.of());
    }

    private static GuidebookEntry entry(
            String id,
            int order,
            List<String> ownerRoleIds,
            List<String> requiredModIds
    ) {
        String nameKey = "guidebook." + id + ".name";
        String summaryKey = "guidebook." + id + ".summary";
        return new GuidebookEntry(
                id,
                GuidebookTab.ROLE,
                id.substring(0, id.indexOf(':')),
                nameKey,
                summaryKey,
                List.of(summaryKey),
                ownerRoleIds,
                requiredModIds,
                order
        );
    }
}
