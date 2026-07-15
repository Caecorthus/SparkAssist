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

        assertEquals(92, resources.size());
        GuidebookCatalog catalog = GuidebookCatalog.merge(resources.stream()
                .map(GuidebookAuthoredResourcesTest::parse)
                .toList());
        assertEquals(92, catalog.entries().size());
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

    private static GuidebookCatalog parse(Path path) {
        try {
            return GuidebookCatalog.parse(Files.readString(path));
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read " + path, exception);
        }
    }
}
