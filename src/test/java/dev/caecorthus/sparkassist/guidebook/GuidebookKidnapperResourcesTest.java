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

class GuidebookKidnapperResourcesTest {
    private static final Path GUIDEBOOK_ROOT = Path.of(
            "src/client/resources/assets/sparkassist/guidebook"
    );
    private static final Path LANG_ROOT = Path.of(
            "src/client/resources/assets/sparkassist/lang"
    );

    @Test
    void documentsKidnapperBetweenNinjaAndGrandWitch() throws IOException {
        GuidebookCatalog roles;
        try (var paths = Files.walk(GUIDEBOOK_ROOT.resolve("roles"))) {
            roles = GuidebookCatalog.merge(paths
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .map(GuidebookKidnapperResourcesTest::parse)
                    .toList());
        }

        GuidebookEntry kidnapper = roles.find("sparkwitch:kidnapper").orElseThrow();
        assertEquals(GuidebookTab.ROLE, kidnapper.tab());
        assertEquals("sparkwitch", kidnapper.sourceModId());
        assertEquals("announcement.role.kidnapper", kidnapper.nameKey());
        assertEquals("guidebook.sparkassist.content.role.overview", kidnapper.summaryKey());
        assertEquals(List.of(), kidnapper.ownerRoleIds());
        assertEquals(List.of("sparkwitch"), kidnapper.requiredModIds());
        assertEquals(0x9B59B6, kidnapper.color());
        assertEquals(420, kidnapper.order());
        assertEquals(List.of(
                "身份",
                "绑架者是杀手阵营身份，继承杀手的基础能力。",
                "拖动",
                "直接瞄准 2 格内且视线无遮挡的尸体，按身份技能键开始拖动。",
                "拖动中可在任意位置再次按身份技能键放下尸体；开始与放下都没有冷却，也不消耗任何资源。",
                "每名绑架者同时只能拖动一具尸体，不能抢走其他绑架者正在拖动的尸体。",
                "跟随与速度",
                "尸体会跟在绑架者身后约 1 格处。",
                "拖动期间，绑架者的最终行走与疾跑速度倍率均变为 0.8；放下尸体或尸体被移除后恢复。",
                "尸体限制",
                "假死尸体无法被拖动；尝试拖动时会提示：TA 真的死了吗...？",
                "裹尸袋、秃鹫与既有尸体清理流程仍正常处理拖动中的尸体。"
        ), flattenedText(kidnapper));

        List<String> ids = roles.entries().stream().map(GuidebookEntry::id).toList();
        int ninja = ids.indexOf("sparkwitch:ninja");
        int kidnapperIndex = ids.indexOf("sparkwitch:kidnapper");
        int grandWitch = ids.indexOf("sparkwitch:grand_witch");
        assertTrue(ninja < kidnapperIndex);
        assertTrue(kidnapperIndex < grandWitch);
    }

    @Test
    void bothLocalesNameKidnapperInChinese() throws IOException {
        for (String locale : List.of("zh_cn", "en_us")) {
            JsonObject translations = JsonParser.parseString(
                    Files.readString(LANG_ROOT.resolve(locale + ".json"))
            ).getAsJsonObject();
            assertEquals("绑架者", translations.get("announcement.role.kidnapper").getAsString());
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
