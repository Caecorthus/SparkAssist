package dev.caecorthus.sparkassist.guidebook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class GuidebookWitchMaidenResourcesTest {
    private static final Path GUIDEBOOK_ROOT = Path.of(
            "src/client/resources/assets/sparkassist/guidebook"
    );
    private static final Path WITCH_MAIDEN = GUIDEBOOK_ROOT.resolve(
            "roles/sparkwitch/witch_maiden.json"
    );
    private static final Path LANG_ROOT = Path.of(
            "src/client/resources/assets/sparkassist/lang"
    );

    @Test
    void documentsWitchMaidenBetweenKidnapperAndGrandWitch() throws IOException {
        GuidebookEntry witchMaiden = parse(WITCH_MAIDEN)
                .find("sparkwitch:witch_maiden")
                .orElseThrow();

        assertEquals(GuidebookTab.ROLE, witchMaiden.tab());
        assertEquals("sparkwitch", witchMaiden.sourceModId());
        assertEquals("announcement.role.witch_maiden", witchMaiden.nameKey());
        assertEquals("guidebook.sparkassist.content.role.overview", witchMaiden.summaryKey());
        assertEquals(List.of(), witchMaiden.ownerRoleIds());
        assertEquals(List.of("sparkwitch"), witchMaiden.requiredModIds());
        assertEquals(0xB04A8B, witchMaiden.color());
        assertEquals(440, witchMaiden.order());
        assertEquals(List.of(
                "曾经我也相信爱与魔法…",
                "身份与商店",
                "巫女是杀手阵营身份，继承杀手的基础能力。",
                "刀售价 100 金币，开锁器售价 50 金币，托法娜仙液售价 200 金币，三者各限购 1 件。",
                "毒药、蝎子与毒苹果均售价 75 金币且不限购；另保留杀手的停电。",
                "聚焦步伐",
                "打开背包，点击玩家头像释放；每页最多显示 10 个目标，可用上一页和下一页翻页。技能不使用身份技能键。",
                "可选择本局任意一名存活的其他玩家，不受距离、阵营、墙体或可见性限制；杀手队友与隐身目标也能选择。",
                "开局即可释放。效果持续 30 秒，成功释放时立即进入 90 秒冷却；这 90 秒包含效果持续时间。",
                "目标只能向当前视角的前方移动，不能停下、后退、横移或蹲伏减速；仍可转向、跳跃、攻击、使用物品和交互。",
                "目标有体力时会被迫疾跑；体力耗尽后改为步行且不会恢复疾跑。期间持续消耗心情。",
                "硬定身仍可阻止移动，但计时和心情消耗照常继续；巫女死亡不会解除效果，目标死亡时效果结束。",
                "巫毒诅咒免疫",
                "巫毒师仍可绑定巫女；巫毒师死亡后，5 秒倒计时和提示照常出现，但倒计时结束时的巫毒死亡无效。",
                "只免疫由绑定产生的最终巫毒死亡；其他伤害与击杀照常生效。该次取消不会触发或消耗托法娜仙液。",
                "毒苹果",
                "毒苹果售价 75 金币，可右键布置在餐盘上；任意持有者都能布置，已布置毒苹果的餐盘不能重复布置。",
                "布置后，第一次成功拿取不会触发毒苹果（若餐盘另有普通毒药，仍会正常中毒）；第二次成功拿取会触发毒苹果，随后陷阱解除。空盘也能布置；如果第一次拿走最后一份，补餐后的下一次成功拿取仍会触发。",
                "毒苹果可与普通餐盘毒药共存；毒理学家的解毒会一并清除。",
                "托法娜仙液",
                "托法娜仙液售价 200 金币且限购 1 瓶，是没有主动右键效果的被动物品。",
                "巫女被另一名仍存活且在本局的玩家直接击杀时，若主背包或快捷栏携带仙液，会消耗 1 瓶并立即反杀实际击杀者。",
                "环境死亡、自杀、没有击杀者或击杀者已死亡时不触发也不消耗；副手、护甲槽、容器与掉落物中的仙液不算携带。",
                "反杀仍可被正常保护挡下；仙液已经消耗且不会重试。巫毒免疫取消的死亡不会触发它。"
        ), witchMaiden.pages().stream()
                .flatMap(page -> page.blocks().stream())
                .flatMap(block -> block.runs().stream())
                .map(run -> run.text())
                .toList());

        GuidebookCatalog roles;
        try (var paths = Files.walk(GUIDEBOOK_ROOT.resolve("roles"))) {
            roles = GuidebookCatalog.merge(paths
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .map(GuidebookWitchMaidenResourcesTest::parse)
                    .toList());
        }
        List<String> ids = roles.entries().stream().map(GuidebookEntry::id).toList();
        assertTrue(ids.indexOf("sparkwitch:kidnapper") < ids.indexOf("sparkwitch:witch_maiden"));
        assertTrue(ids.indexOf("sparkwitch:witch_maiden") < ids.indexOf("sparkwitch:grand_witch"));
        assertEquals(1, roles.entries().stream()
                .filter(entry -> entry.tab() == GuidebookTab.ROLE)
                .filter(entry -> entry.order() == witchMaiden.order())
                .count());
    }

    @Test
    void bothLocalesNameWitchMaidenInChinese() throws IOException {
        for (String locale : List.of("zh_cn", "en_us")) {
            JsonObject translations = JsonParser.parseString(
                    Files.readString(LANG_ROOT.resolve(locale + ".json"))
            ).getAsJsonObject();
            assertEquals("巫女", translations.get("announcement.role.witch_maiden").getAsString());
        }
    }

    @Test
    void keepsFocusedFootstepsOnTheRolePageOnly() {
        assertFalse(GuidebookDiscoveryRules.includes(
                GuidebookTab.SKILL, "sparkwitch:focused_footsteps"));
        assertTrue(GuidebookDiscoveryRules.includes(
                GuidebookTab.ROLE, "sparkwitch:focused_footsteps"));
    }

    private static GuidebookCatalog parse(Path path) {
        try {
            return GuidebookCatalog.parse(Files.readString(path));
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read " + path, exception);
        }
    }
}
