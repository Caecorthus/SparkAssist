package dev.caecorthus.sparkassist.guidebook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.caecorthus.sparkassist.guidebook.content.GuidebookRun;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class GuidebookAuthoredResourcesTest {
    private static final Path GUIDEBOOK_ROOT = Path.of(
            "src/client/resources/assets/sparkassist/guidebook"
    );
    private static final Path LANG_ROOT = Path.of(
            "src/client/resources/assets/sparkassist/lang"
    );
    private static final Map<GuidebookTab, Long> EXPECTED_ENTRY_COUNTS = Map.of(
            GuidebookTab.ROLE, 65L,
            GuidebookTab.TRAIT, 31L,
            GuidebookTab.SKILL, 7L,
            GuidebookTab.FACTION, 4L
    );
    private static final Map<String, List<String>> EXPECTED_SKILL_OWNERS = Map.of(
            "sparkwitch:ceremonial_sword", List.of("sparkwitch:grand_witch"),
            "sparkwitch:death_ray", List.of("sparkwitch:murderous_witch"),
            "sparkwitch:mighty_force", List.of("sparkwitch:apprentice_witch"),
            "sparkwitch:swift_step", List.of("sparkwitch:apprentice_witch"),
            "sparkwitch:murder_sense", List.of("sparkwitch:apprentice_witch"),
            "sparkwitch:healing", List.of("sparkwitch:apprentice_witch"),
            "sparkwitch:clairvoyance", List.of("sparkwitch:apprentice_witch")
    );
    private static final Set<String> ALLOWED_FACTION_LABELS = Set.of(
            "好人阵营", "杀手阵营", "中立阵营", "魔女阵营"
    );
    private static final Pattern FACTION_LABEL = Pattern.compile(
            "(?:非好人|非平民|原生杀手|好人|杀手|中立|魔女|普通|独立|平民)阵营"
    );
    private static final Pattern SLASH_COMMAND = Pattern.compile(
            "(?<!\\S)/[a-z][a-z0-9_-]*(?::[a-z][a-z0-9_-]*)?",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern RAW_TICKS = Pattern.compile("(?i)(?<![a-z])\\d+\\s*ticks?(?![a-z])");
    private static final List<String> PROHIBITED_POLICY_TERMS = List.of(
            "管理员", "脚本", "配置", "职业刷新", "身份刷新", "角色刷新",
            "刷新职业", "刷新身份", "刷新角色", "身份与刷新", "本局刷新", "同时刷新"
    );

    @Test
    void authoredAndLocalizedContentConformsToGuidebookPolicy() throws IOException {
        List<Path> resources = authoredResources();
        assertEquals(107, resources.size());

        List<GuidebookCatalog> resourceCatalogs = resources.stream()
                .map(path -> {
                    GuidebookCatalog parsed = parse(path);
                    assertEquals(1, parsed.entries().size(), () -> path + " must contain exactly one entry");
                    return parsed;
                })
                .toList();
        GuidebookCatalog catalog = GuidebookCatalog.merge(resourceCatalogs);
        assertEquals(107, catalog.entries().size());

        Map<GuidebookTab, Long> counts = new EnumMap<>(GuidebookTab.class);
        for (GuidebookTab tab : GuidebookTab.values()) {
            counts.put(tab, catalog.entries().stream().filter(entry -> entry.tab() == tab).count());
        }
        assertEquals(EXPECTED_ENTRY_COUNTS, counts);

        Map<String, List<String>> skillOwners = catalog.entries().stream()
                .filter(entry -> entry.tab() == GuidebookTab.SKILL)
                .collect(java.util.stream.Collectors.toMap(
                        GuidebookEntry::id,
                        GuidebookEntry::ownerRoleIds
                ));
        assertEquals(EXPECTED_SKILL_OWNERS, skillOwners);

        Map<String, JsonObject> translations = Map.of(
                "zh_cn", translations("zh_cn"),
                "en_us", translations("en_us")
        );
        List<String> chineseProse = new ArrayList<>();
        List<String> allLocalizedProse = new ArrayList<>();
        for (GuidebookEntry entry : catalog.entries()) {
            for (GuidebookRun run : entry.pages().stream()
                    .flatMap(page -> page.blocks().stream())
                    .flatMap(block -> block.runs().stream())
                    .toList()) {
                if (run.text() != null) {
                    String line = entry.id() + ": " + run.text();
                    chineseProse.add(line);
                    allLocalizedProse.add(line);
                } else {
                    for (Map.Entry<String, JsonObject> locale : translations.entrySet()) {
                        assertTrue(locale.getValue().has(run.translationKey()),
                                () -> entry.id() + " references missing " + locale.getKey()
                                        + " key " + run.translationKey());
                        String line = entry.id() + " [" + locale.getKey() + "]: "
                                + locale.getValue().get(run.translationKey()).getAsString();
                        allLocalizedProse.add(line);
                        if (locale.getKey().equals("zh_cn")) {
                            chineseProse.add(line);
                        }
                    }
                }
            }
        }

        for (String line : chineseProse) {
            var factions = FACTION_LABEL.matcher(line);
            while (factions.find()) {
                assertTrue(ALLOWED_FACTION_LABELS.contains(factions.group()),
                        () -> "Nonstandard faction label in " + line);
            }
            for (String term : PROHIBITED_POLICY_TERMS) {
                assertFalse(line.contains(term), () -> "Prohibited policy prose '" + term + "' in " + line);
            }
        }
        for (String line : allLocalizedProse) {
            assertFalse(SLASH_COMMAND.matcher(line).find(), () -> "Slash command in " + line);
            assertFalse(RAW_TICKS.matcher(line).find(), () -> "Raw tick duration in " + line);
        }

        GuidebookEntry witch = catalog.find("sparkassist:faction/sparkwitch/witch").orElseThrow();
        assertTrue(witch.ownerRoleIds().contains("sparkwitch:curser"));
        String witchProse = chineseProse.stream()
                .filter(line -> line.startsWith(witch.id() + ": "))
                .collect(java.util.stream.Collectors.joining("\n"));
        assertTrue(witchProse.contains("诅咒者"));
        assertTrue(witchProse.contains("所有存活玩家均属于魔女阵营"));
    }

    @Test
    void includesSpiritSleuthWithTheUniversalTraits() throws IOException {
        List<Path> resources;
        try (var paths = Files.walk(GUIDEBOOK_ROOT)) {
            resources = paths
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted()
                    .toList();
        }

        assertEquals(107, resources.size());
        GuidebookCatalog catalog = GuidebookCatalog.merge(resources.stream()
                .map(GuidebookAuthoredResourcesTest::parse)
                .toList());
        assertEquals(107, catalog.entries().size());
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
        assertEquals(65, catalog.entries().stream()
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
                "圣徒属于好人阵营；没有理智值，不会获得任务，只能使用受限冲刺，且无法拾取枪械。",
                "不会获得内鬼词条。",
                "好人阵营玩家无法击杀圣徒；该次击杀会被取消。",
                "巫毒师仍可绑定圣徒，但由绑定触发的最终连锁死亡会被取消。",
                "业火",
                "开局冷却 60 秒；激活后持续 15 秒。若圣徒存活至效果结束，则进入 60 秒冷却。",
                "效果期间，成功杀死圣徒且不属于好人阵营的玩家会获得【业障】，但大魔女除外；除非之后成为大魔女，否则持续到本局结束。",
                "业障",
                "【业障】只会在三种动作成功后触发：成功击杀、成功转移定时炸弹、成功命中毒针。",
                "大魔女不会获得【业障】。已背负【业障】的玩家成为大魔女时，标记会立即清除；之后离开大魔女身份也不会恢复。",
                "物品冷却",
                "触发时，快捷栏、主背包与副手中所有可以冷却的物品统一进入冷却：当前身份为炸弹客则为 20 秒，否则为 5 秒。",
                "已有更长冷却时保留更长值；再次触发只刷新而不叠加，之后获得的新物品也会继承当前冷却。成为大魔女前已生效的物品冷却不会随标记一起清除，而会按原剩余时间自然结束。",
                "【业障】跨越死亡、复活、重连和其他身份变化保留；成为大魔女时立即清除，否则持续到本局结束。"
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
                "调香师属于好人阵营；每完成一个任务获得 50 金币。",
                "香精",
                "香精售价 100 金币。右键另一名存活玩家，为其施加仅自己可见的标记；每名调香师的标记彼此独立，持续至目标死亡或本局结束。",
                "标记成功后，调香师会在 12 格内且视线无遮挡时，以自己的身份颜色高亮目标。被标记的玩家成功击杀其他玩家后会沾上【血腥气味】，持续至目标死亡或本局结束。",
                "目标沾上【血腥气味】后，高亮边框会切换为红色；调香师可隔墙高亮 4 格内的目标，4 格外至 12 格内仅在视线无遮挡时高亮。",
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
                "忍者属于杀手阵营，继承杀手的基础能力。",
                "忍者在自身位置的综合亮度不高于 5，或全局停电期间成功击杀玩家时，额外获得 100 金币。",
                "专属商店",
                "苦无：100 金币，每局限购 1 次；手里剑：275 金币；开锁器：75 金币。",
                "停电沿用杀手的规则：杀手不超过 3 人时售价 400 金币；每多 1 名杀手，价格增加 100 金币。停电持续 30 至 40 秒，并进入全体杀手共享的 5 分钟冷却。",
                "格挡",
                "开局冷却 60 秒；激活后开启 2.5 秒格挡窗口。成功挡下一次由其他玩家造成的致命击杀，或窗口自然结束后，进入 180 秒冷却。",
                "普通伤害不会消耗格挡；环境伤害与自杀无法被格挡。",
                "苦无与手里剑",
                "苦无可立即击杀视线内 4 格内的一名玩家；无论是否命中都不会消耗，成功使用后同类物品冷却 30 秒。",
                "左键苦无可击退玩家。安装 SparkTraits 时，嗜血可缩短苦无冷却，突刺可增强左键击退。",
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

    @Test
    void documentsKunaiSupportOnBloodthirstyAndThrust() throws IOException {
        GuidebookEntry bloodthirsty = parse(GUIDEBOOK_ROOT.resolve(
                "traits/sparktraits/bloodthirsty.json"
        )).find("sparktraits:bloodthirsty").orElseThrow();
        GuidebookEntry thrust = parse(GUIDEBOOK_ROOT.resolve(
                "traits/sparktraits/thrust.json"
        )).find("sparktraits:thrust").orElseThrow();

        assertEquals(List.of(
                "嗜血层数",
                "每次真实击杀都会使受支持武器的使用冷却缩短 5%；苦无也属于受支持武器。",
                "层数上限为“本局开局人数 ÷ 3”向下取整。"
        ), bloodthirsty.pages().stream()
                .flatMap(page -> page.blocks().stream())
                .flatMap(block -> block.runs().stream())
                .map(run -> run.text())
                .toList());
        assertEquals(List.of(
                "突刺",
                "使用受支持武器攻击时，攻击击退属性额外提高 0.25；苦无也属于受支持武器。"
        ), thrust.pages().stream()
                .flatMap(page -> page.blocks().stream())
                .flatMap(block -> block.runs().stream())
                .map(run -> run.text())
                .toList());
    }

    private static JsonObject translations(String locale) throws IOException {
        return JsonParser.parseString(Files.readString(LANG_ROOT.resolve(locale + ".json")))
                .getAsJsonObject();
    }

    private static List<Path> authoredResources() throws IOException {
        try (var paths = Files.walk(GUIDEBOOK_ROOT)) {
            return paths
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted()
                    .toList();
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
