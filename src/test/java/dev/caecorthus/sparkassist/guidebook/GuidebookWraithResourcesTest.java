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
    void wraithDocumentsThreeAlignmentsSettingsSnapshotAndSharedRestrictions() throws IOException {
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
                "GOOD（好人）、KILLER（杀手）或 WITCH（魔女）",
                "/sparkwitch:ghostChance <0..100>",
                "/sparkwitch:ghostMinRequirement <非负人数>",
                "/wathe:gameSettings set roleDividend ghost <n>",
                "floor(starting players / n)",
                "局中修改仅从下一局生效",
                "完成第 3 个任务时晋升",
                "KILLER 冤魂只会晋升为【破坏者】",
                "WITCH 冤魂只会晋升为【诅咒者】",
                "除【守护天使】外的所有晋升身份都不能发送文字聊天",
                "守护天使可以打开并使用文字聊天",
                "地图的 no-jump 设置",
                "不会产生走路、落地或疾跑地面粒子",
                "风精灵风弹可以影响普通存活参赛玩家",
                "绝不会影响基础冤魂或任何晋升冤魂身份",
                "只有基础（restricted）冤魂和晋升后的【仇杀客】",
                "【风精灵】【守护天使】【破坏者】【诅咒者】恢复真实皮肤"
        )) {
            assertTrue(text.contains(required), required);
        }
    }

    @Test
    void promotionsUseApprovedColorsAlignmentsAndExpandedContracts() throws IOException {
        GuidebookCatalog roles = allRoles();
        assertPromotion(roles, "wind_spirit", 0x59D8E6, 280, "GOOD 冤魂");
        assertPromotion(roles, "guardian_angel", 0xF0D77A, 281, "GOOD 冤魂");
        assertPromotion(roles, "vendetta", 0xE34B5F, 282, "GOOD 冤魂");
        assertPromotion(roles, "saboteur", 0xE28743, 450, "KILLER 冤魂唯一");
        assertPromotion(roles, "curser", 0xA968D5, 520, "WITCH 冤魂唯一");

        String wind = flattenedText(roles.find("sparkwitch:wind_spirit").orElseThrow());
        assertTrue(wind.contains("普通的存活参赛玩家"));
        assertTrue(wind.contains("绝不会影响基础冤魂或任何晋升冤魂身份"));
        assertTrue(wind.contains("不额外造成伤害"));

        String guardian = flattenedText(roles.find("sparkwitch:guardian_angel").orElseThrow());
        assertTrue(guardian.contains("可以打开并使用文字聊天"));
        assertTrue(guardian.contains("隐藏死者/旁观者语音组"));

        String vendetta = flattenedText(roles.find("sparkwitch:vendetta").orElseThrow());
        assertTrue(vendetta.contains("纯红色 #FF0000"));
        assertTrue(vendetta.contains("默认宽臂 Steve"));
        assertTrue(vendetta.contains("动态皮肤模型包装"));

        String saboteur = flattenedText(roles.find("sparkwitch:saboteur").orElseThrow());
        assertTrue(saboteur.contains("GOOD 与 WITCH 冤魂不会晋升为破坏者"));
        assertTrue(saboteur.contains("20 格球形范围"));

        String curser = flattenedText(roles.find("sparkwitch:curser").orElseThrow());
        assertTrue(curser.contains("属于魔女阵营"));
        assertTrue(curser.contains("不再出现在 KILLER 冤魂晋升池中"));
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
