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
    void documentsOrthopedistBetweenMermaidAndPigGod() throws IOException {
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
        assertEquals(List.of(
                "身份与刷新",
                "骨科大夫是好人身份，可以完成任务并与好人阵营共同获胜。",
                "本局刷新猎人时，一定会同时刷新一名骨科大夫；骨科大夫也可以在没有猎人的对局中单独刷新。",
                "体力专长",
                "骨科大夫自身的实际体力消耗降低 25%。",
                "正骨",
                "按 G 对 3 格内瞄准的玩家使用【正骨】；开局冷却 30 秒，成功使用后冷却 60 秒。",
                "目标没有骨折时，获得 20 秒【正骨】和 5 秒速度 I；【正骨】使实际体力消耗降低 25%。",
                "【正骨】持续期间若目标将获得一层骨折，会消耗【正骨】并抵消这一层骨折。",
                "骨科大夫可以高亮看见直接视线内处于【正骨】状态的玩家。",
                "已有【正骨】的目标不能被重复施放；本次不会进入冷却，界面会提示效果已经生效。",
                "骨折处理",
                "目标已有骨折时，立即移除一层骨折并获得 5 秒速度 I，不会保留【正骨】效果；本次仍视为成功使用。",
                "受到大魔女的【恐惧】时无法使用【正骨】。"
        ), flattenedText(orthopedist));

        List<String> ids = roleIds();
        int mermaid = ids.indexOf("noellesroles:mermaid");
        int orthopedistIndex = ids.indexOf("sparkwitch:orthopedist");
        int pigGod = ids.indexOf("sparkwitch:pig_god");
        assertTrue(mermaid < orthopedistIndex);
        assertTrue(orthopedistIndex < pigGod);
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
        assertEquals(List.of(
                "身份与商店",
                "猎人是杀手阵营身份，拥有无限体力、杀手本能和本局计时器。",
                "猎人刷新时，本局一定会同时刷新一名骨科大夫。",
                "商店保留小刀、裹尸袋、撬棍和蝎子；另可购买双管猎枪 100 金币（库存 1）、猎枪弹药 125 金币、捕兽夹 75 金币。",
                "双管猎枪",
                "双管猎枪与猎枪弹药可由任何持有者使用。枪内最多装填 2 发，射程 8 格，命中玩家会立即造成枪杀死亡。",
                "射击后若枪内仍有弹药，冷却 4 tick；打空后冷却 30 秒。",
                "装入第一发后的 10 秒内才可装入第二发；窗口结束后必须先发射现有弹药才能再次装填。创造模式装填不会消耗猎枪弹药。",
                "捕兽夹",
                "捕兽夹可由任何持有者放置。每名放置者最多保留 2 个夹子，放置第三个时会移除最早放置的夹子。",
                "夹子放置 10 tick 后激活，10 分钟后失效；放置者可以潜行交互回收自己的夹子。",
                "任何存活玩家都能触发夹子，包括放置者本人和杀手队友。触发后定身 3 秒并增加一层骨折。",
                "骨折",
                "骨折最多叠加 5 层，每层独立持续 60 秒；每层使移动速度降低 20%。骨折期间无法疾跑或恢复体力。",
                "使用毒药瓶可为尚未中毒的夹子涂毒；带毒夹触发后会施加 40 秒中毒。",
                "受害者确认死于带毒夹时，下毒者获得 75 金币，放置者获得 50 金币；两者为同一人时合计获得 125 金币。",
                "观察与拆除",
                "猎人、原生杀手阵营玩家和本局死亡旁观者会持续隔墙看见夹子。",
                "义警、老兵、黑警和工程师只能在直接视线内看见夹子；老兵、黑警和工程师可以拆除，义警不能拆除。",
                "成功拆除夹子后，携带的所有可冷却物品至少进入 15 秒共享冷却；已有更长冷却不会缩短。",
                "大魔女、杀意魔女和共犯可以在直接视线内看见夹子；开启本能时，夹子会按各自的本能颜色隔墙描边。学徒魔女和猪神看不见夹子。",
                "死亡掉落",
                "猎人确认死亡后，携带的双管猎枪与猎枪弹药会消失，未放置的捕兽夹正常掉落；死亡被取消时不会提前清理这些物品。"
        ), flattenedText(hunter));

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
