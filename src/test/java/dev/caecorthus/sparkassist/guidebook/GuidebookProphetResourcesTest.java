package dev.caecorthus.sparkassist.guidebook;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.caecorthus.sparkassist.guidebook.content.GuidebookBlockType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuidebookProphetResourcesTest {
    private static final Path ROOT = Path.of(
            "src/client/resources/assets/sparkassist/guidebook");
    private static final Path PROPHET = ROOT.resolve("roles/sparkwitch/prophet.json");
    private static final Path APPRENTICE = ROOT.resolve("roles/sparkwitch/apprentice_witch.json");
    private static final Path KILLER = ROOT.resolve("roles/wathe/killer.json");
    private static final Path LANG = Path.of("src/client/resources/assets/sparkassist/lang");

    @Test
    void documentsProphetInsideTheCivilianRoleBlock() throws IOException {
        GuidebookEntry prophet = parse(PROPHET).find("sparkwitch:prophet").orElseThrow();
        GuidebookEntry apprentice = parse(APPRENTICE)
                .find("sparkwitch:apprentice_witch").orElseThrow();
        GuidebookEntry killer = parse(KILLER).find("wathe:killer").orElseThrow();

        assertEquals(GuidebookTab.ROLE, prophet.tab());
        assertEquals("sparkwitch", prophet.sourceModId());
        assertEquals("announcement.role.prophet", prophet.nameKey());
        assertEquals("guidebook.sparkassist.content.role.overview", prophet.summaryKey());
        assertEquals(List.of(), prophet.ownerRoleIds());
        assertEquals(List.of("sparkwitch"), prophet.requiredModIds());
        assertEquals(0xD4AF37, prophet.color());
        assertTrue(apprentice.order() < prophet.order());
        assertTrue(prophet.order() < killer.order());
        assertFalse(prophet.pages().stream()
                .flatMap(page -> page.blocks().stream())
                .anyMatch(block -> block.type() == GuidebookBlockType.QUOTE));
        assertEquals(List.of(
                "死亡预兆",
                "开局进入 60 秒冷却。按下技能键后，死亡预兆持续 20 秒。",
                "只会红色高亮生效期间新产生的尸体；技能开启前已有的尸体不会被标记。",
                "高亮不受墙体阻挡，但只能看见 128 格内的尸体。先知死亡时，效果立即终止。",
                "效果结束后进入 90 秒冷却。"
        ), prophet.pages().stream()
                .flatMap(page -> page.blocks().stream())
                .flatMap(block -> block.runs().stream())
                .map(run -> run.text())
                .toList());

        long entriesAtProphetOrder;
        try (var paths = Files.walk(ROOT.resolve("roles"))) {
            entriesAtProphetOrder = paths
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .flatMap(path -> parse(path).entries().stream())
                    .filter(entry -> entry.tab() == GuidebookTab.ROLE)
                    .filter(entry -> entry.order() == prophet.order())
                    .count();
        }
        assertEquals(1, entriesAtProphetOrder);
    }

    @Test
    void bothLocalesNameProphetInChinese() throws IOException {
        for (String locale : List.of("zh_cn", "en_us")) {
            JsonObject translations = JsonParser.parseString(
                    Files.readString(LANG.resolve(locale + ".json")))
                    .getAsJsonObject();
            assertEquals("先知", translations.get("announcement.role.prophet").getAsString());
        }
    }

    private static GuidebookCatalog parse(Path path) {
        try {
            return GuidebookCatalog.parse(Files.readString(path));
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read " + path, exception);
        }
    }
}
