package dev.caecorthus.sparkassist.guidebook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class GuidebookHunterOrthopedistResourcesTest {
    private static final Path GUIDEBOOK_ROOT = Path.of(
            "src/client/resources/assets/sparkassist/guidebook"
    );
    private static final Path LANG_ROOT = Path.of(
            "src/client/resources/assets/sparkassist/lang"
    );

    @Test
    void documentsOrthopedistBetweenMermaidAndSaint() throws IOException {
        GuidebookEntry orthopedist = parse(GUIDEBOOK_ROOT.resolve(
                "roles/sparkwitch/orthopedist.json"
        )).find("sparkwitch:orthopedist").orElseThrow();

        assertEquals(GuidebookTab.ROLE, orthopedist.tab());
        assertEquals("sparkwitch", orthopedist.sourceModId());
        assertEquals("announcement.role.orthopedist", orthopedist.nameKey());
        assertEquals("guidebook.sparkassist.content.role.overview", orthopedist.summaryKey());
        assertEquals(List.of(), orthopedist.ownerRoleIds());
        assertEquals(List.of("sparkwitch"), orthopedist.requiredModIds());
        assertEquals(0x90B358, orthopedist.color());
        assertEquals(225, orthopedist.order());
        List<String> orthopedistText = flattenedText(orthopedist);
        assertTrue(orthopedistText.contains("身份与任务"));
        assertTrue(orthopedistText.contains("骨科大夫属于好人阵营，可以完成任务并与好人阵营共同获胜。"));
        assertTrue(orthopedistText.contains("骨科大夫自身的实际体力消耗降低 25%。"));
        assertTrue(orthopedistText.contains("按 G 对 3 格内瞄准的玩家使用【正骨】；开局冷却 30 秒，成功使用后冷却 60 秒。"));
        assertTrue(orthopedistText.stream().noneMatch(text -> text.contains("刷新")));

        List<String> ids = roleIds();
        int mermaid = ids.indexOf("noellesroles:mermaid");
        int orthopedistIndex = ids.indexOf("sparkwitch:orthopedist");
        int saint = ids.indexOf("sparkwitch:saint");
        assertTrue(mermaid < orthopedistIndex);
        assertTrue(orthopedistIndex < saint);
    }

    @Test
    void documentsHunterBetweenKillerAndPhantom() throws IOException {
        GuidebookEntry hunter = parse(GUIDEBOOK_ROOT.resolve(
                "roles/sparkwitch/hunter.json"
        )).find("sparkwitch:hunter").orElseThrow();

        assertEquals(GuidebookTab.ROLE, hunter.tab());
        assertEquals("sparkwitch", hunter.sourceModId());
        assertEquals("announcement.role.hunter", hunter.nameKey());
        assertEquals("guidebook.sparkassist.content.role.overview", hunter.summaryKey());
        assertEquals(List.of(), hunter.ownerRoleIds());
        assertEquals(List.of("sparkwitch"), hunter.requiredModIds());
        assertEquals(0x5C4C34, hunter.color());
        assertEquals(305, hunter.order());
        List<String> hunterText = flattenedText(hunter);
        assertTrue(hunterText.contains("猎人是杀手阵营身份，拥有无限体力、杀手本能和本局计时器。"));
        assertTrue(hunterText.contains("射击后若枪内仍有弹药，冷却 0.2 秒；打空后冷却 30 秒。"));
        assertTrue(hunterText.contains("夹子放置 0.5 秒后激活，10 分钟后失效；放置者可以潜行交互回收自己的夹子。"));
        assertTrue(hunterText.stream().noneMatch(text -> text.contains("刷新")));
        assertTrue(hunterText.stream().noneMatch(text -> text.contains("tick")));

        List<String> ids = roleIds();
        int killer = ids.indexOf("wathe:killer");
        int hunterIndex = ids.indexOf("sparkwitch:hunter");
        int phantom = ids.indexOf("noellesroles:phantom");
        assertTrue(killer < hunterIndex);
        assertTrue(hunterIndex < phantom);
    }

    @Test
    void bothLocalesNameHunterAndOrthopedistInChinese() throws IOException {
        for (String locale : List.of("zh_cn", "en_us")) {
            JsonObject translations = JsonParser.parseString(
                    Files.readString(LANG_ROOT.resolve(locale + ".json"))
            ).getAsJsonObject();
            assertEquals("猎人", translations.get("announcement.role.hunter").getAsString());
            assertEquals("骨科大夫", translations.get("announcement.role.orthopedist").getAsString());
        }
    }

    private static List<String> roleIds() throws IOException {
        try (var paths = Files.walk(GUIDEBOOK_ROOT.resolve("roles"))) {
            return GuidebookCatalog.merge(paths
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .map(GuidebookHunterOrthopedistResourcesTest::parse)
                    .toList())
                    .entries().stream()
                    .map(GuidebookEntry::id)
                    .toList();
        }
    }

    private static List<String> flattenedText(GuidebookEntry entry) {
        return entry.pages().stream()
                .flatMap(page -> page.blocks().stream())
                .flatMap(block -> block.runs().stream())
                .map(run -> run.text())
                .toList();
    }

    private static GuidebookCatalog parse(Path path) {
        try {
            return GuidebookCatalog.parse(Files.readString(path));
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read " + path, exception);
        }
    }
}
