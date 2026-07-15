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

        assertEquals(96, resources.size());
        GuidebookCatalog catalog = GuidebookCatalog.merge(resources.stream()
                .map(GuidebookAuthoredResourcesTest::parse)
                .toList());
        assertEquals(96, catalog.entries().size());
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

    @Test
    void includesSaintBetweenMermaidAndPigGod() throws IOException {
        List<Path> resources;
        try (var paths = Files.walk(GUIDEBOOK_ROOT)) {
            resources = paths
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted()
                    .toList();
        }

        GuidebookCatalog catalog = GuidebookCatalog.merge(resources.stream()
                .map(GuidebookAuthoredResourcesTest::parse)
                .toList());
        assertEquals(54, catalog.entries().stream()
                .filter(entry -> entry.tab() == GuidebookTab.ROLE)
                .count());

        GuidebookEntry saint = catalog.find("sparkwitch:saint").orElseThrow();
        assertEquals(GuidebookTab.ROLE, saint.tab());
        assertEquals("sparkwitch", saint.sourceModId());
        assertEquals("announcement.role.saint", saint.nameKey());
        assertEquals("guidebook.sparkassist.content.role.overview", saint.summaryKey());
        assertEquals(List.of(), saint.ownerRoleIds());
        assertEquals(List.of("sparkwitch"), saint.requiredModIds());
        assertEquals(0xEEBC78, saint.color());
        assertEquals(226, saint.order());
        assertEquals(List.of(
                "身份规则",
                "圣徒是平民好人身份；没有理智值，不会获得任务，只能使用受限的乘客冲刺，且无法拾取枪械。",
                "不会获得内鬼词条。",
                "有效阵营为平民阵营的玩家无法击杀圣徒；该次击杀会被取消。",
                "巫毒师仍可绑定圣徒，但由绑定触发的最终连锁死亡会被取消。",
                "业火",
                "开局冷却 60 秒；激活后持续 15 秒。若圣徒存活至效果结束，则进入 60 秒冷却。",
                "效果期间，除大魔女外，成功杀死圣徒的非平民阵营玩家会获得永久【业障】，持续到本局结束。",
                "业障",
                "【业障】只会在三种动作成功后触发：成功击杀、成功转移定时炸弹、成功命中毒针。",
                "大魔女不会获得或触发【业障】。已背负【业障】的玩家成为大魔女后，标记仍会保留，但在保持大魔女身份期间不会继续施加物品冷却；离开该身份后恢复正常规则。",
                "物品冷却",
                "触发时，快捷栏、主背包与副手中所有可以冷却的物品统一进入冷却：当前身份为炸弹客则为 20 秒，否则为 5 秒。",
                "已有更长冷却时保留更长值；再次触发只刷新而不叠加，之后获得的新物品也会继承当前冷却。",
                "【业障】跨越死亡、复活、重连和身份变化保留，直到本局结束。"
        ), saint.pages().stream()
                .flatMap(page -> page.blocks().stream())
                .flatMap(block -> block.runs().stream())
                .map(run -> run.text())
                .toList());

        List<String> ids = catalog.entries().stream().map(GuidebookEntry::id).toList();
        int mermaid = ids.indexOf("noellesroles:mermaid");
        int saintIndex = ids.indexOf("sparkwitch:saint");
        int pigGod = ids.indexOf("sparkwitch:pig_god");
        assertTrue(mermaid < saintIndex);
        assertTrue(saintIndex < pigGod);
    }

    @Test
    void includesPerfumerAfterApprenticeWitchInTheCivilianBlock() throws IOException {
        Path resource = GUIDEBOOK_ROOT.resolve("roles/sparkwitch/perfumer.json");
        GuidebookEntry perfumer = parse(resource).find("sparkwitch:perfumer").orElseThrow();

        assertEquals(GuidebookTab.ROLE, perfumer.tab());
        assertEquals("sparkwitch", perfumer.sourceModId());
        assertEquals("announcement.role.perfumer", perfumer.nameKey());
        assertEquals("guidebook.sparkassist.content.role.overview", perfumer.summaryKey());
        assertEquals(List.of(), perfumer.ownerRoleIds());
        assertEquals(List.of("sparkwitch"), perfumer.requiredModIds());
        assertEquals(0xF2A4A4, perfumer.color());
        assertEquals(250, perfumer.order());
        assertEquals(List.of(
                "身份与经济",
                "调香师是好人身份；每完成一个任务获得 50 金币。",
                "香精",
                "香精售价 100 金币。右键另一名存活玩家，为其施加仅自己可见的标记；每名调香师的标记彼此独立，持续至目标死亡或本局结束。",
                "被标记的玩家成功击杀其他玩家后会沾上【血腥气味】，持续至目标死亡或本局结束。",
                "调香师会隔墙高亮 4 格内带有【血腥气味】的玩家；4 格外至 12 格内仅在视线无遮挡时高亮。",
                "尸体感知",
                "调香师会隔墙高亮 4 格内未被清道夫隐藏的新鲜尸体与腐烂尸体；4 格外至 12 格内仅在视线无遮挡时高亮。",
                "距离未被隐藏的新鲜尸体或腐烂尸体 4 格内且有未完成任务时，任务造成的理智下降速度变为 2 倍；多个尸体不会叠加该效果。",
                "古龙水",
                "古龙水售价 50 金币。可对自己使用，或对 3 格内视线无遮挡的一名存活玩家使用。",
                "目标在 10 秒内每秒恢复 5 点理智；再次使用只会刷新持续时间，不会叠加理智恢复速度。",
                "香精与古龙水均可不限次数重复购买。",
                "香精与古龙水无法丢弃；持有者自己可见，其他存活且仍在活动的玩家不可见，死亡或旁观状态的玩家可见。"
        ), perfumer.pages().stream()
                .flatMap(page -> page.blocks().stream())
                .flatMap(block -> block.runs().stream())
                .map(run -> run.text())
                .toList());

        GuidebookCatalog roles;
        try (var paths = Files.walk(GUIDEBOOK_ROOT.resolve("roles"))) {
            roles = GuidebookCatalog.merge(paths
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .map(GuidebookAuthoredResourcesTest::parse)
                    .toList());
        }
        List<String> ids = roles.entries().stream().map(GuidebookEntry::id).toList();
        int apprenticeWitch = ids.indexOf("sparkwitch:apprentice_witch");
        int perfumerIndex = ids.indexOf("sparkwitch:perfumer");
        int killer = ids.indexOf("wathe:killer");
        assertTrue(apprenticeWitch < perfumerIndex);
        assertTrue(perfumerIndex < killer);
    }

    @Test
    void includesNinjaBetweenBanditAndGrandWitchInTheKillerBlock() throws IOException {
        Path resource = GUIDEBOOK_ROOT.resolve("roles/sparkwitch/ninja.json");
        GuidebookEntry ninja = parse(resource).find("sparkwitch:ninja").orElseThrow();

        assertEquals(GuidebookTab.ROLE, ninja.tab());
        assertEquals("sparkwitch", ninja.sourceModId());
        assertEquals("announcement.role.ninja", ninja.nameKey());
        assertEquals("guidebook.sparkassist.content.role.overview", ninja.summaryKey());
        assertEquals(List.of(), ninja.ownerRoleIds());
        assertEquals(List.of("sparkwitch"), ninja.requiredModIds());
        assertEquals(0x2C2C2C, ninja.color());
        assertEquals(410, ninja.order());
        assertEquals(List.of(
                "身份与经济",
                "忍者是杀手阵营身份，继承杀手的基础能力；开局携带一把开锁器。",
                "忍者在自身位置的综合亮度不高于 5，或全局停电期间成功击杀玩家时，额外获得 100 金币。",
                "专属商店",
                "苦无：130 金币；手里剑：275 金币；开锁器：75 金币。",
                "停电沿用杀手的规则：杀手不超过 3 人时售价 400 金币；每多 1 名杀手，价格增加 100 金币。停电持续 30 至 40 秒，并进入全体杀手共享的 5 分钟冷却。",
                "格挡",
                "开局冷却 60 秒；激活后开启 2.5 秒格挡窗口。成功挡下一次由其他玩家造成的致命击杀，或窗口自然结束后，进入 180 秒冷却。",
                "普通伤害不会消耗格挡；环境伤害、自杀、管理员与脚本强制死亡无法被格挡。",
                "苦无与手里剑",
                "苦无可立即击杀视线内 4 格内的一名玩家；未命中时不会消耗，成功使用后同类物品冷却 30 秒。",
                "手里剑至少蓄力 0.2 秒后投出；命中其他玩家时将其击杀，投掷后同类物品冷却 1 秒。",
                "苦无的使用与手里剑的投掷本身无声；手里剑命中声与正常的尸体、死亡反馈仍会出现。",
                "两种武器都可由任意持有者使用，也可通过容器或既有转移方式流通；存活玩家无法主动丢出，持有者死亡时会直接移除。"
        ), ninja.pages().stream()
                .flatMap(page -> page.blocks().stream())
                .flatMap(block -> block.runs().stream())
                .map(run -> run.text())
                .toList());

        GuidebookCatalog roles;
        try (var paths = Files.walk(GUIDEBOOK_ROOT.resolve("roles"))) {
            roles = GuidebookCatalog.merge(paths
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .map(GuidebookAuthoredResourcesTest::parse)
                    .toList());
        }
        List<String> ids = roles.entries().stream().map(GuidebookEntry::id).toList();
        int bandit = ids.indexOf("noellesroles:bandit");
        int ninjaIndex = ids.indexOf("sparkwitch:ninja");
        int grandWitch = ids.indexOf("sparkwitch:grand_witch");
        assertTrue(bandit < ninjaIndex);
        assertTrue(ninjaIndex < grandWitch);
    }

    private static GuidebookCatalog parse(Path path) {
        try {
            return GuidebookCatalog.parse(Files.readString(path));
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read " + path, exception);
        }
    }
}
