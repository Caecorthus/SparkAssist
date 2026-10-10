package dev.caecorthus.sparkassist.client.ponder;

import static dev.caecorthus.sparkassist.client.ponder.SparkPonderDemos.role;
import static dev.caecorthus.sparkassist.client.ponder.SparkPonderDemos.scene;
import static dev.caecorthus.sparkassist.client.ponder.WatheItemScenes.SCREEN_LEFT;
import static dev.caecorthus.sparkassist.client.ponder.WatheItemScenes.SCREEN_RIGHT;
import static dev.caecorthus.sparkassist.client.ponder.WatheItemScenes.stack;

import java.util.List;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/**
 * Demos for Wathe's own roles beyond what the item demos cover. They follow the Spark server stack, as the guide
 * does (SparkStrength reworks the Veteran).
 * Wathe 自带身份中物品演示没有覆盖到的部分。与指南书一致，按 Spark 服务器的实际规则演示（SparkStrength 改写了老兵）。
 */
final class WatheRoleScenes {
    private WatheRoleScenes() {
    }

    static void register() {
        role("sparkassist:roles/wathe/veteran", List.of(), scene("wathe/aisle", WatheRoleScenes::veteran));
        role("sparkassist:roles/wathe/loose_end", List.of(), scene("wathe/aisle", WatheItemScenes::derringer));
    }

    /**
     * Veteran on Spark (SparkStrength VeteranKnifeService, VeteranRules, InnocentKnifeKillRules,
     * VeteranBlackoutService): right-click stabs at once within 3 blocks, silent, no cooldown; 2 stabs per knife,
     * then the knife is gone; the shop sells another for 300. Stabbing an innocent kills the Veteran too. During a
     * blackout every player within 10 blocks is outlined through walls.
     * Spark 上的老兵：右键立即刺杀 3 格内的人，无声、无冷却；每把刀刺 2 次后消失，商店 300 金币可再买一把。刺死好人
     * 自己也会死。停电时能隔墙看到 10 格内所有玩家的轮廓。
     */
    private static void veteran(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("role_veteran", "老兵：两次无声刺杀");
        WatheItemScenes.stage(scene);
        Text killerName = Text.literal("杀手");
        ElementLink<ActorElement> veteran = Actors.enter(scene, RoleColors.VETERAN, Text.literal("老兵"),
                new Vec3d(5.8, 1, 1.2), SCREEN_RIGHT, Direction.DOWN);
        Actors.hold(scene, veteran, stack("wathe:knife"));
        scene.idle(15);
        scene.overlay().showText(70)
                .text("老兵开局带一把刀，这把刀能刺 2 次")
                .independent()
                .attachKeyFrame();
        scene.idle(80);
        ElementLink<ActorElement> first = Actors.enter(scene, RoleColors.KILLER, killerName,
                new Vec3d(2.2, 1, 4.8), SCREEN_RIGHT, Direction.DOWN);
        Actors.hold(scene, first, stack("wathe:knife"));
        scene.idle(10);
        Actors.walk(scene, veteran, new Vec3d(-1.8, 0, 1.8), 24);
        scene.idle(28);
        scene.overlay().showControls(new Vec3d(4.0, 3.6, 3.0), Pointing.DOWN, 30).rightClick()
                .withItem(stack("wathe:knife"));
        scene.idle(8);
        Actors.swing(scene, veteran);
        Actors.fall(scene, first);
        scene.overlay().showText(70)
                .text("右键直接刺杀 3 格内的人：不用蓄力、没有声音，刺完也没有冷却")
                .independent()
                .attachKeyFrame();
        scene.idle(80);
        scene.overlay().showText(60)
                .colored(PonderPalette.GREEN)
                .text("刺死好人阵营以外的人，奖励 100 金币")
                .independent();
        scene.idle(60);
        Actors.leave(scene, first, Direction.UP);
        scene.idle(15);
        ElementLink<ActorElement> second = Actors.enter(scene, RoleColors.KILLER, killerName,
                new Vec3d(2.2, 1, 4.8), SCREEN_RIGHT, Direction.DOWN);
        scene.idle(20);
        Actors.swing(scene, veteran);
        Actors.fall(scene, second);
        scene.idle(4);
        Actors.hold(scene, veteran, ItemStack.EMPTY);
        scene.overlay().showText(70)
                .text("第 2 次刺完，刀就消失了；商店里 300 金币能再买一把，再刺 2 次")
                .independent()
                .attachKeyFrame();
        scene.idle(80);
        Actors.leave(scene, second, Direction.UP);
        scene.idle(15);
        ElementLink<ActorElement> civilian = Actors.enter(scene, RoleColors.CIVILIAN, Text.literal("平民"),
                new Vec3d(2.2, 1, 4.8), SCREEN_RIGHT, Direction.DOWN);
        ElementLink<ActorElement> lurker = Actors.enter(scene, RoleColors.KILLER, killerName,
                new Vec3d(1.2, 1, 1.4), SCREEN_LEFT, Direction.DOWN);
        scene.idle(15);
        StageLights.lights(scene, false);
        Actors.highlight(scene, civilian, RoleColors.VETERAN, 80);
        Actors.highlight(scene, lurker, RoleColors.VETERAN, 80);
        scene.overlay().showText(80)
                .text("停电时老兵有夜视，还能隔墙看到 10 格内大多数人的轮廓（方框示意）")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        StageLights.lights(scene, true);
        Actors.leave(scene, lurker, Direction.UP);
        scene.overlay().showControls(new Vec3d(4.0, 3.6, 3.0), Pointing.DOWN, 30).withItem(stack("wathe:knife"));
        scene.overlay().showText(40)
                .text("花 300 金币再买一把刀……")
                .independent();
        scene.idle(20);
        Actors.hold(scene, veteran, stack("wathe:knife"));
        scene.idle(25);
        scene.overlay().showControls(new Vec3d(4.0, 3.6, 3.0), Pointing.DOWN, 30).rightClick()
                .withItem(stack("wathe:knife"));
        scene.idle(8);
        Actors.swing(scene, veteran);
        Actors.fall(scene, civilian);
        scene.idle(10);
        Actors.fall(scene, veteran);
        scene.overlay().showText(80)
                .colored(PonderPalette.RED)
                .text("可要是刺死了好人阵营的人，老兵自己也会当场死亡")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        scene.markAsFinished();
    }
}
