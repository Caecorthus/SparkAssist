package dev.caecorthus.sparkassist.guidebook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GuidebookWraithResourcesTest {
    private static final Path ROLE_ROOT = Path.of("src/client/resources/assets/sparkassist/guidebook/roles");
    private static final Map<String, String> PROMOTION_NAMES = Map.of(
            "wind_spirit", "风精灵",
            "guardian_angel", "守护天使",
            "vendetta", "仇杀客",
            "saboteur", "破坏者",
            "curser", "诅咒者"
    );

    @Test
    void wraithDocumentsThreeFactionsPromotionAndSharedRestrictions() throws IOException {
        GuidebookEntry wraith = allRoles().find("sparkwitch:wraith").orElseThrow();
        assertEquals(GuidebookTab.ROLE, wraith.tab());
        assertEquals("sparkwitch", wraith.sourceModId());
        assertEquals("announcement.role.wraith", wraith.nameKey());
        assertEquals(List.of(), wraith.ownerRoleIds());
        assertEquals(List.of("sparkwitch"), wraith.requiredModIds());
        assertEquals(0x79C7D4, wraith.color());
        assertEquals(606, wraith.order());

        String text = flattenedText(wraith);
        for (String required : List.of(
                "好人阵营、杀手阵营或魔女阵营",
                "完成第 3 个任务时晋升",
                "杀手阵营冤魂晋升为【破坏者】",
                "魔女阵营冤魂晋升为【诅咒者】",
                "除【守护天使】外的晋升职业不能发送文字聊天",
                "禁跳规则",
                "不会产生行走、落地或疾跑粒子",
                "无法捡起地面物品",
                "基础冤魂与【仇杀客】",
                "其他晋升职业恢复真实外观"
        )) {
            assertTrue(text.contains(required), required);
        }
        for (String prohibited : List.of("/sparkwitch", "/wathe", "gameSettings", "forcePromotion", "快照")) {
            assertTrue(!text.contains(prohibited), prohibited);
        }
    }

    @Test
    void promotionsUseApprovedColorsAlignmentsAndExpandedContracts() throws IOException {
        GuidebookCatalog roles = allRoles();
        assertPromotion(roles, "wind_spirit", 0x59D8E6, 280, "好人阵营冤魂");
        assertPromotion(roles, "guardian_angel", 0xF0D77A, 281, "好人阵营冤魂");
        assertPromotion(roles, "vendetta", 0xE34B5F, 282, "好人阵营");
        assertPromotion(roles, "saboteur", 0xE28743, 450, "杀手阵营冤魂");
        assertPromotion(roles, "curser", 0xA968D5, 520, "魔女阵营冤魂");

        String wind = flattenedText(roles.find("sparkwitch:wind_spirit").orElseThrow());
        assertTrue(wind.contains("普通存活玩家"));
        assertTrue(wind.contains("不会影响冤魂或晋升职业"));
        assertTrue(wind.contains("不会额外造成伤害"));

        String guardian = flattenedText(roles.find("sparkwitch:guardian_angel").orElseThrow());
        assertTrue(guardian.contains("可以使用文字聊天"));
        assertTrue(guardian.contains("死者语音频道"));

        String vendetta = flattenedText(roles.find("sparkwitch:vendetta").orElseThrow());
        assertTrue(vendetta.contains("红色描边"));
        assertTrue(vendetta.contains("无名 Steve"));
        assertTrue(vendetta.contains("兼容外部皮肤"));

        String saboteur = flattenedText(roles.find("sparkwitch:saboteur").orElseThrow());
        assertTrue(saboteur.contains("杀手阵营冤魂的晋升职业"));
        assertTrue(saboteur.contains("20 格内"));

        String curser = flattenedText(roles.find("sparkwitch:curser").orElseThrow());
        assertTrue(curser.contains("属于魔女阵营"));
        assertTrue(curser.contains("所有存活玩家均属于魔女阵营"));
    }

    @Test
    void wraithAndPromotionsKeepExpectedCatalogOrdering() throws IOException {
        List<String> ids = allRoles().entries().stream().map(GuidebookEntry::id).toList();
        assertBetween(ids, "sparkwitch:wind_spirit", "sparkwitch:prophet", "wathe:killer");
        assertBetween(ids, "sparkwitch:guardian_angel", "sparkwitch:prophet", "wathe:killer");
        assertBetween(ids, "sparkwitch:vendetta", "sparkwitch:prophet", "wathe:killer");
        assertBetween(ids, "sparkwitch:saboteur", "sparkwitch:witch_maiden", "sparkwitch:grand_witch");
        assertBetween(ids, "sparkwitch:curser", "sparkwitch:accomplice", "sparkwitch:murderous_witch");
        assertBetween(ids, "sparkwitch:wraith", "wathe:loose_end", "noellesroles:corrupt_cop");
    }

    private static void assertPromotion(GuidebookCatalog roles, String path, int color, int order, String contract) {
        GuidebookEntry entry = roles.find("sparkwitch:" + path).orElseThrow();
        assertEquals("sparkwitch", entry.sourceModId());
        assertEquals("announcement.role." + path, entry.nameKey());
        assertEquals(color, entry.color());
        assertEquals(order, entry.order());
        String text = flattenedText(entry);
        assertTrue(text.contains(PROMOTION_NAMES.get(path)));
        assertTrue(text.contains(contract), path);
    }

    private static void assertBetween(List<String> ids, String target, String before, String after) {
        assertTrue(ids.indexOf(before) < ids.indexOf(target), target);
        assertTrue(ids.indexOf(target) < ids.indexOf(after), target);
    }

    private static String flattenedText(GuidebookEntry entry) {
        Map<String, String> translations = translations("zh_cn");
        return entry.pages().stream()
                .flatMap(page -> page.blocks().stream())
                .flatMap(block -> block.runs().stream())
                .map(run -> run.translationKey() == null
                        ? run.text()
                        : translations.get(run.translationKey()))
                .reduce("", (left, right) -> left + "\n" + right);
    }

    private static Map<String, String> translations(String locale) {
        try {
            var json = com.google.gson.JsonParser.parseString(Files.readString(Path.of(
                    "src/client/resources/assets/sparkassist/lang/" + locale + ".json"
            ))).getAsJsonObject();
            return json.entrySet().stream().collect(java.util.stream.Collectors.toUnmodifiableMap(
                    Map.Entry::getKey,
                    entry -> entry.getValue().getAsString()
            ));
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read " + locale + " translations", exception);
        }
    }

    private static GuidebookCatalog allRoles() throws IOException {
        try (var paths = Files.walk(ROLE_ROOT)) {
            return GuidebookCatalog.merge(paths
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .map(GuidebookWraithResourcesTest::parse)
                    .toList());
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
