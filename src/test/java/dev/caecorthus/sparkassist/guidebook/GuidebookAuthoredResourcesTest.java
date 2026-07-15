package dev.caecorthus.sparkassist.guidebook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class GuidebookAuthoredResourcesTest {
    private static final Path GUIDEBOOK_ROOT = Path.of(
            "src/client/resources/assets/sparkassist/guidebook"
    );

    @Test
    void includesSpiritSleuthWithTheUniversalTraits() throws IOException {
        List<Path> resources;
        try (var paths = Files.walk(GUIDEBOOK_ROOT)) {
            resources = paths
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted()
                    .toList();
        }

        assertEquals(91, resources.size());
        GuidebookCatalog catalog = GuidebookCatalog.merge(resources.stream()
                .map(GuidebookAuthoredResourcesTest::parse)
                .toList());
        assertEquals(91, catalog.entries().size());
        assertEquals(31, catalog.entries().stream()
                .filter(entry -> entry.tab() == GuidebookTab.TRAIT)
                .count());

        GuidebookEntry spiritSleuth = catalog.find("sparktraits:spirit_sleuth").orElseThrow();
        assertEquals("trait.sparktraits.spirit_sleuth.name", spiritSleuth.nameKey());
        assertEquals("trait.sparktraits.spirit_sleuth.description", spiritSleuth.summaryKey());
        assertEquals(List.of(), spiritSleuth.ownerRoleIds());
        assertEquals(List.of("sparktraits"), spiritSleuth.requiredModIds());
        assertEquals(0xB8A7FF, spiritSleuth.color());
        assertEquals(780, spiritSleuth.order());
        assertEquals("灵体视野", spiritSleuth.pages().getFirst().blocks().get(0).runs().getFirst().text());
        assertEquals(
                "始终可以看到旁观者漂浮的头。",
                spiritSleuth.pages().getFirst().blocks().get(1).runs().getFirst().text()
        );

        List<String> ids = catalog.entries().stream().map(GuidebookEntry::id).toList();
        int excellentPhysique = ids.indexOf("sparktraits:excellent_physique");
        int spiritSleuthIndex = ids.indexOf("sparktraits:spirit_sleuth");
        int lastStand = ids.indexOf("sparktraits:last_stand");
        assertTrue(excellentPhysique < spiritSleuthIndex);
        assertTrue(spiritSleuthIndex < lastStand);
    }

    private static GuidebookCatalog parse(Path path) {
        try {
            return GuidebookCatalog.parse(Files.readString(path));
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read " + path, exception);
        }
    }
}
