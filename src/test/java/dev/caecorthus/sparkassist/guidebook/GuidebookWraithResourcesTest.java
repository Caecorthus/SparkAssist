package dev.caecorthus.sparkassist.guidebook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GuidebookWraithResourcesTest {
    private static final Path ROLE_ROOT = Path.of(
            "src/client/resources/assets/sparkassist/guidebook/roles"
    );
    private static final Map<String, String> PROMOTION_NAMES = Map.of(
            "wind_spirit", "风精灵",
            "guardian_angel", "守护天使",
            "vendetta", "仇杀客",
            "saboteur", "破坏者",
            "curser", "诅咒者"
    );

    @Test
    void wraithBelongsToSparkWitchAndKeepsNormalTextAndIncomingVoice() throws IOException {
        GuidebookEntry wraith = parse(ROLE_ROOT.resolve("sparkwitch/wraith.json"))
                .find("sparkwitch:wraith")
                .orElseThrow();

        assertEquals(GuidebookTab.ROLE, wraith.tab());
        assertEquals("sparkwitch", wraith.sourceModId());
        assertEquals("announcement.role.wraith", wraith.nameKey());
        assertEquals("guidebook.sparkassist.content.role.overview", wraith.summaryKey());
        assertEquals(List.of(), wraith.ownerRoleIds());
        assertEquals(List.of("sparkwitch"), wraith.requiredModIds());
        assertEquals(0x79C7D4, wraith.color());
        assertEquals(606, wraith.order());

        String text = flattenedText(wraith);
        assertTrue(text.contains("文字聊天与接收语音保持正常"));
        assertTrue(text.contains("仅禁止冤魂主动发送语音"));
        assertFalse(text.contains("不能在对局聊天"));
        assertFalse(text.contains("对局聊天仍不可用"));

        GuidebookCatalog roles = allRoles();
        List<String> ids = roles.entries().stream().map(GuidebookEntry::id).toList();
        int looseEnd = ids.indexOf("wathe:loose_end");
        int wraithIndex = ids.indexOf("sparkwitch:wraith");
        int corruptCop = ids.indexOf("noellesroles:corrupt_cop");
        assertTrue(looseEnd < wraithIndex);
        assertTrue(wraithIndex < corruptCop);
        assertEquals(-1, ids.indexOf("sparktraits:wraith"));
    }

    @Test
    void promotionsUseCanonicalIdsExactNamesAndTheirActualFactionSections() throws IOException {
        GuidebookCatalog roles = allRoles();

        assertPromotion(roles, "wind_spirit", 0x36E51B, 280, "好人阵营");
        assertPromotion(roles, "guardian_angel", 0x36E51B, 281, "好人阵营");
        assertPromotion(roles, "vendetta", 0x36E51B, 282, "好人阵营");
        assertPromotion(roles, "saboteur", 0xC13838, 450, "杀手阵营");
        assertPromotion(roles, "curser", 0xC13838, 520, "魔女阵营");

        List<String> ids = roles.entries().stream().map(GuidebookEntry::id).toList();
        assertBetween(ids, "sparkwitch:wind_spirit", "sparkwitch:prophet", "wathe:killer");
        assertBetween(ids, "sparkwitch:guardian_angel", "sparkwitch:prophet", "wathe:killer");
        assertBetween(ids, "sparkwitch:vendetta", "sparkwitch:prophet", "wathe:killer");
        assertBetween(ids, "sparkwitch:saboteur", "sparkwitch:witch_maiden", "sparkwitch:grand_witch");
        assertBetween(ids, "sparkwitch:curser", "sparkwitch:accomplice", "sparkwitch:murderous_witch");
    }

    private static void assertPromotion(
            GuidebookCatalog roles,
            String path,
            int color,
            int order,
            String factionName
    ) {
        GuidebookEntry entry = roles.find("sparkwitch:" + path).orElseThrow();
        assertEquals(GuidebookTab.ROLE, entry.tab());
        assertEquals("sparkwitch", entry.sourceModId());
        assertEquals("announcement.role." + path, entry.nameKey());
        assertEquals(List.of(), entry.ownerRoleIds());
        assertEquals(List.of("sparkwitch"), entry.requiredModIds());
        assertEquals(color, entry.color());
        assertEquals(order, entry.order());

        String text = flattenedText(entry);
        assertTrue(text.contains(PROMOTION_NAMES.get(path)));
        assertTrue(text.contains(factionName));
        for (String forbidden : List.of("主动技能", "魔女技能", "物品", "商店", "魔力", "装备")) {
            assertFalse(text.contains(forbidden), path + " must remain identity-only: " + forbidden);
        }
        String lowerText = text.toLowerCase(Locale.ROOT);
        for (String forbidden : List.of("active ability", "skill", "item", "shop", "mana", "loadout")) {
            assertFalse(lowerText.contains(forbidden), path + " must remain identity-only: " + forbidden);
        }
    }

    private static void assertBetween(List<String> ids, String target, String before, String after) {
        assertTrue(ids.indexOf(before) < ids.indexOf(target), target);
        assertTrue(ids.indexOf(target) < ids.indexOf(after), target);
    }

    private static String flattenedText(GuidebookEntry entry) {
        return entry.pages().stream()
                .flatMap(page -> page.blocks().stream())
                .flatMap(block -> block.runs().stream())
                .map(run -> run.text())
                .reduce("", (left, right) -> left + "\n" + right);
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
