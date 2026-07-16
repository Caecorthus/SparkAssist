package dev.caecorthus.sparkassist.guidebook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class GuidebookBlackRavenResourcesTest {
    private static final Path GUIDEBOOK_ROOT = Path.of(
            "src/client/resources/assets/sparkassist/guidebook"
    );
    private static final Path BLACK_RAVEN = GUIDEBOOK_ROOT.resolve(
            "roles/sparkwitch/black_raven.json"
    );
    private static final Path LANG_ROOT = Path.of(
            "src/client/resources/assets/sparkassist/lang"
    );

    @Test
    void documentsBlackRavenBetweenKidnapperAndWitchMaiden() throws IOException {
        GuidebookEntry blackRaven = parse(BLACK_RAVEN)
                .find("sparkwitch:black_raven")
                .orElseThrow();

        assertEquals(GuidebookTab.ROLE, blackRaven.tab());
        assertEquals("sparkwitch", blackRaven.sourceModId());
        assertEquals("announcement.role.black_raven", blackRaven.nameKey());
        assertEquals("guidebook.sparkassist.content.role.overview", blackRaven.summaryKey());
        assertEquals(List.of(), blackRaven.ownerRoleIds());
        assertEquals(List.of("sparkwitch"), blackRaven.requiredModIds());
        assertEquals(0x51445F, blackRaven.color());
        assertEquals(430, blackRaven.order());

        List<String> text = blackRaven.pages().stream()
                .flatMap(page -> page.blocks().stream())
                .flatMap(block -> block.runs().stream())
                .map(run -> run.text())
                .toList();
        assertTrue(text.stream().anyMatch(value -> value.contains("60 秒")));
        assertTrue(text.stream().anyMatch(value -> value.contains("20 秒")));
        assertTrue(text.stream().anyMatch(value -> value.contains("保护") && value.contains("不返还")));
        assertTrue(text.stream().anyMatch(value -> value.contains("感知册")));
        assertTrue(text.stream().anyMatch(value -> value.contains("60 秒")
                && value.contains("15 秒") && value.contains("90 秒")));
        assertTrue(text.stream().anyMatch(value -> value.contains("8 格")
                && value.contains("每秒") && value.contains("10 点")));
        assertTrue(text.stream().anyMatch(value -> value.contains("50%")
                && value.contains("Steve")));
        assertTrue(text.stream().anyMatch(value -> value.contains("普通模式")
                && value.contains("感知模式")));

        GuidebookCatalog roles;
        try (var paths = Files.walk(GUIDEBOOK_ROOT.resolve("roles"))) {
            roles = GuidebookCatalog.merge(paths
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .map(GuidebookBlackRavenResourcesTest::parse)
                    .toList());
        }
        List<String> ids = roles.entries().stream().map(GuidebookEntry::id).toList();
        assertTrue(ids.indexOf("sparkwitch:kidnapper") < ids.indexOf("sparkwitch:black_raven"));
        assertTrue(ids.indexOf("sparkwitch:black_raven") < ids.indexOf("sparkwitch:witch_maiden"));
        assertEquals(1, roles.entries().stream()
                .filter(entry -> entry.tab() == GuidebookTab.ROLE)
                .filter(entry -> entry.order() == blackRaven.order())
                .count());
    }

    @Test
    void bothLocalesNameBlackRavenInChinese() throws IOException {
        for (String locale : List.of("zh_cn", "en_us")) {
            JsonObject translations = JsonParser.parseString(
                    Files.readString(LANG_ROOT.resolve(locale + ".json"))
            ).getAsJsonObject();
            assertEquals("黑羽鸦", translations.get("announcement.role.black_raven").getAsString());
        }
    }

    @Test
    void keepsPerceptionOnTheRolePageOnly() {
        assertFalse(GuidebookDiscoveryRules.includes(
                GuidebookTab.SKILL, "sparkwitch:perception"));
        assertTrue(GuidebookDiscoveryRules.includes(
                GuidebookTab.ROLE, "sparkwitch:perception"));
    }

    private static GuidebookCatalog parse(Path path) {
        try {
            return GuidebookCatalog.parse(Files.readString(path));
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read " + path, exception);
        }
    }
}
