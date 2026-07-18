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
    private static final String GUARDIAN_ANGEL_DESCRIPTION = "帮助好人阵营。你拥有无限体力、免疫熄灯，"
            + "并能识别被下毒的食物、饮品和床；你只能与死者进行语音交流。晋升60秒后，瞄准三格内一名"
            + "存活玩家并按下技能键，可秘密施加一层持续10秒、判定与铁人药剂相同的守护护盾；成功施放后冷却90秒。"
            + "护盾生效期间，目标会以你的身份色隔墙高亮且仅你可见；目标不会知道自己受到保护。";
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
    void wraithBelongsToSparkWitchAndBlocksTextAndOutgoingVoice() throws IOException {
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
        assertTrue(text.contains("不能发送文字聊天或主动发送语音"));
        assertTrue(text.contains("可以正常接收语音"));
        assertTrue(text.contains("所有冤魂都不能捡起地面物品"));
        assertTrue(text.contains("不会修改任何玩家的真实皮肤"));
        assertTrue(text.contains("若安装 SparkTraits"));

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

        GuidebookEntry windSpirit = roles.find("sparkwitch:wind_spirit").orElseThrow();
        assertEquals(GuidebookTab.ROLE, windSpirit.tab());
        assertEquals("sparkwitch", windSpirit.sourceModId());
        assertEquals("announcement.role.wind_spirit", windSpirit.nameKey());
        assertEquals(List.of(), windSpirit.ownerRoleIds());
        assertEquals(List.of("sparkwitch"), windSpirit.requiredModIds());
        assertEquals(0x36E51B, windSpirit.color());
        assertEquals(280, windSpirit.order());
        assertTrue(flattenedText(windSpirit).contains("风精灵"));
        assertTrue(flattenedText(windSpirit).contains("好人阵营"));

        assertPromotion(roles, "guardian_angel", 0x36E51B, 281, "好人阵营");
        assertPromotion(roles, "vendetta", 0x36E51B, 282, "好人阵营");
        assertSaboteur(roles);
        assertPromotion(roles, "curser", 0xC13838, 520, "魔女阵营");

        List<String> ids = roles.entries().stream().map(GuidebookEntry::id).toList();
        assertBetween(ids, "sparkwitch:wind_spirit", "sparkwitch:prophet", "wathe:killer");
        assertEquals(-1, ids.indexOf("sparktraits:wind_spirit"));
        assertBetween(ids, "sparkwitch:guardian_angel", "sparkwitch:prophet", "wathe:killer");
        assertBetween(ids, "sparkwitch:vendetta", "sparkwitch:prophet", "wathe:killer");
        assertBetween(ids, "sparkwitch:saboteur", "sparkwitch:witch_maiden", "sparkwitch:grand_witch");
        assertBetween(ids, "sparkwitch:curser", "sparkwitch:accomplice", "sparkwitch:murderous_witch");
    }

    @Test
    void windSpiritListsItsExactPostPromotionAbilities() throws IOException {
        GuidebookEntry windSpirit = allRoles().find("sparkwitch:wind_spirit").orElseThrow();
        String text = flattenedText(windSpirit);

        assertTrue(text.contains("永久拥有速度 II；若获得更高等级的速度效果，则保留更高等级，效果结束后恢复速度 II。"));
        assertTrue(text.contains("商店中可以用 50 金币购买 1 个原版【风弹】，不限购买次数且没有商店冷却。"));
        assertTrue(text.contains("拥有 Wathe 原生本能透视，但仍遵守已有的本能隐藏规则。"));
        assertTrue(text.contains("熄灯期间免疫失明并获得夜视。"));
        assertTrue(text.contains("拥有无限体力。"));
        assertTrue(text.contains("晋升后每完成 1 个任务获得 50 金币。"));
    }

    @Test
    void saboteurListsItsExactShopSabotageAndIncomeContracts() throws IOException {
        String text = flattenedText(allRoles().find("sparkwitch:saboteur").orElseThrow());

        assertTrue(text.contains("1 个【开锁器】，售价 50 金币"));
        assertTrue(text.contains("Wathe 原版【熄灯】条目"));
        assertTrue(text.contains("20 格球形范围"));
        assertTrue(text.contains("晋升时先进入 60 秒冷却"));
        assertTrue(text.contains("随后进入 120 秒冷却"));
        assertTrue(text.contains("范围内没有可用灯时仍视为成功使用"));
        assertTrue(text.contains("灯必须等两个效果都结束后才会恢复"));
        assertTrue(text.contains("不会触发全局倒计时、失明、夜视或 Wathe 熄灯的共享冷却"));
        assertTrue(text.contains("不影响火把、红石灯、手持物动态光源或其他模组的光源"));
        assertTrue(text.contains("触发晋升的那项任务不会补发奖励"));
    }

    private static void assertSaboteur(GuidebookCatalog roles) {
        GuidebookEntry entry = roles.find("sparkwitch:saboteur").orElseThrow();
        assertEquals(GuidebookTab.ROLE, entry.tab());
        assertEquals("sparkwitch", entry.sourceModId());
        assertEquals("announcement.role.saboteur", entry.nameKey());
        assertEquals(List.of(), entry.ownerRoleIds());
        assertEquals(List.of("sparkwitch"), entry.requiredModIds());
        assertEquals(0xC13838, entry.color());
        assertEquals(450, entry.order());
        assertTrue(flattenedText(entry).contains("破坏者"));
        assertTrue(flattenedText(entry).contains("杀手阵营"));
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
        if (path.equals("guardian_angel")) {
            assertTrue(text.contains(GUARDIAN_ANGEL_DESCRIPTION));
            return;
        }
        if (path.equals("vendetta")) {
            assertTrue(text.contains("首次死亡时真正对你负责的凶手"));
            assertTrue(text.contains("无限体力、免疫熄灯"));
            assertTrue(text.contains("15格内"));
            assertTrue(text.contains("1格内达到100%"));
            assertTrue(text.contains("4格内会持续以红色隔墙高亮"));
            assertTrue(text.contains("每次30秒倒计时结束后"));
            assertTrue(text.contains("完整循环为35秒"));
            assertTrue(text.contains("5秒红色隔墙透视"));
            assertTrue(text.contains("绑定凶手在30秒内重连"));
            assertTrue(text.contains("超时则按确认逃脱结算"));
            assertTrue(text.contains("按住右键至少0.5秒"));
            assertTrue(text.contains("视线内3格以内"));
            assertTrue(text.contains("只有你与绑定凶手能够互相影响"));
            assertTrue(text.contains("真正的旁观者能看见灰白史蒂夫"));
            assertTrue(text.contains("但看不见复仇刀"));
            assertTrue(text.contains("各获得200点对应资源"));
            assertTrue(text.contains("直接转为旁观者且不留下尸体"));
            assertTrue(text.contains("不留下仇杀客尸体，也不触发秃鹫粒子或200点奖励"));
            assertTrue(text.contains("不产生奖励、仇杀客尸体或粒子"));
            assertTrue(text.contains("不会更换目标"));
            return;
        }
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
