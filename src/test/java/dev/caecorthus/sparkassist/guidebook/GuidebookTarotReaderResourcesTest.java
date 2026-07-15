package dev.caecorthus.sparkassist.guidebook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.caecorthus.sparkassist.guidebook.content.GuidebookBlockType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class GuidebookTarotReaderResourcesTest {
    private static final Path GUIDEBOOK_ROOT = Path.of(
            "src/client/resources/assets/sparkassist/guidebook"
    );
    private static final Path TAROT_READER_RESOURCE = GUIDEBOOK_ROOT.resolve(
            "roles/sparkwitch/tarot_reader.json"
    );
    private static final Path LANG_ROOT = Path.of(
            "src/client/resources/assets/sparkassist/lang"
    );

    @Test
    void documentsTarotReaderDivinationsBetweenApprenticeWitchAndKiller() throws IOException {
        GuidebookEntry tarotReader = parse(TAROT_READER_RESOURCE)
                .find("sparkwitch:tarot_reader")
                .orElseThrow();

        assertEquals(GuidebookTab.ROLE, tarotReader.tab());
        assertEquals("sparkwitch", tarotReader.sourceModId());
        assertEquals("announcement.role.tarot_reader", tarotReader.nameKey());
        assertEquals("guidebook.sparkassist.content.role.overview", tarotReader.summaryKey());
        assertEquals(List.of(), tarotReader.ownerRoleIds());
        assertEquals(List.of("sparkwitch"), tarotReader.requiredModIds());
        assertEquals(0xAEE1CF, tarotReader.color());
        assertEquals(250, tarotReader.order());
        assertFalse(tarotReader.pages().stream()
                .flatMap(page -> page.blocks().stream())
                .anyMatch(block -> block.type() == GuidebookBlockType.QUOTE));
        assertEquals(List.of(
                "身份与经济",
                "塔罗牌师是好人身份；初始金币为 0，每完成一个任务获得 50 金币。",
                "常规占卜",
                "花费 200 金币，记录购买时本局存活玩家的阵营人数。死亡、断线玩家不计入统计；被吞下但尚未消化的玩家仍会计入。",
                "占卜结果持续显示至本局结束，不会实时更新；再次购买会刷新为新的购买时快照。",
                "右上角以粗体【占卜结果】为标题；好人、杀手、中立与魔女四行人数分别使用对应阵营颜色。",
                "身份占卜",
                "花费 50 金币，选择一个身份，得知该身份本局是否被分配过；已经死亡的该身份玩家仍算本局出现过。",
                "存活占卜",
                "花费 50 金币，选择一名本局玩家，得知其是否仍然存活。",
                "被吞下但尚未消化的玩家仍算存活；消化或死亡后不算存活。"
        ), tarotReader.pages().stream()
                .flatMap(page -> page.blocks().stream())
                .flatMap(block -> block.runs().stream())
                .map(run -> run.text())
                .toList());

        GuidebookCatalog roles;
        try (var paths = Files.walk(GUIDEBOOK_ROOT.resolve("roles"))) {
            roles = GuidebookCatalog.merge(paths
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .map(GuidebookTarotReaderResourcesTest::parse)
                    .toList());
        }
        List<String> ids = roles.entries().stream().map(GuidebookEntry::id).toList();
        int apprenticeWitch = ids.indexOf("sparkwitch:apprentice_witch");
        int tarotReaderIndex = ids.indexOf("sparkwitch:tarot_reader");
        int killer = ids.indexOf("wathe:killer");
        assertTrue(apprenticeWitch < tarotReaderIndex);
        assertTrue(tarotReaderIndex < killer);
    }

    @Test
    void bothLocalesNameTarotReaderInChinese() throws IOException {
        for (String locale : List.of("zh_cn", "en_us")) {
            JsonObject translations = JsonParser.parseString(
                    Files.readString(LANG_ROOT.resolve(locale + ".json"))
            ).getAsJsonObject();
            assertEquals("塔罗牌师", translations.get("announcement.role.tarot_reader").getAsString());
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
