package dev.caecorthus.sparkassist.client.ponder;

import static dev.caecorthus.sparkassist.client.ponder.SparkPonderDemos.role;
import static dev.caecorthus.sparkassist.client.ponder.SparkPonderDemos.scene;
import static dev.caecorthus.sparkassist.client.ponder.WatheItemScenes.EAST;
import static dev.caecorthus.sparkassist.client.ponder.WatheItemScenes.NORTH;
import static dev.caecorthus.sparkassist.client.ponder.WatheItemScenes.SOUTH;
import static dev.caecorthus.sparkassist.client.ponder.WatheItemScenes.WEST;
import static dev.caecorthus.sparkassist.client.ponder.WatheItemScenes.stack;

import java.util.List;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/**
 * Demos for NoellesRoles' civilian roles, opened only from the role's guide page; each needs NoellesRoles.
 * NoellesRoles 平民阵营职业的演示，只从该职业的指南页打开；都需要 NoellesRoles。
 */
final class NoellesCivilianScenes {
    private NoellesCivilianScenes() {
    }

    static void register() {
        role("sparkassist:roles/noellesroles/conductor", List.of("noellesroles"),
                scene("wathe/cabin", NoellesCivilianScenes::conductor));
    }

    /**
     * Conductor (NoellesRoles): starts with a master key that opens train doors and locked room doors with no
     * cooldown, but not jammed ones; it drops on death and anyone can use it.
     * 列车长（NoellesRoles）：开局带万能钥匙，可无冷却打开列车门和锁着的房门，但打不开被卡住的门；死后钥匙掉落，
     * 任何人捡到都能用。
     */
    private static void conductor(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("role_conductor", "列车长：万能钥匙");
        WatheItemScenes.cabinStage(scene, util);
        BlockPos door = util.grid().at(3, 1, 3);
        ElementLink<ActorElement> passenger = Actors.enter(scene, RoleColors.CIVILIAN, Text.literal("平民"),
                new Vec3d(2.2, 1, 5.2), EAST, Direction.DOWN);
        ElementLink<ActorElement> killer = Actors.enter(scene, RoleColors.KILLER, Text.literal("杀手"),
                new Vec3d(4.6, 1, 5.2), WEST, Direction.DOWN);
        Actors.hold(scene, killer, stack("wathe:knife"));
        scene.idle(10);
        ElementLink<ActorElement> conductor = Actors.enter(scene,
                RoleColors.of("noellesroles:conductor", 0xFFCD54), Text.literal("列车长"),
                new Vec3d(0.5, 1, 1.5), EAST, Direction.DOWN);
        Actors.hold(scene, conductor, stack("noellesroles:master_key"));
        scene.idle(15);
        scene.overlay().showText(70)
                .text("列车长开局就带着一把万能钥匙")
                .independent()
                .attachKeyFrame();
        scene.idle(40);
        Actors.charge(scene, killer, true);
        Actors.walk(scene, conductor, new Vec3d(3, 0, 0), 30);
        scene.idle(32);
        Actors.turn(scene, conductor, SOUTH);
        scene.idle(10);
        scene.overlay().showControls(new Vec3d(3.5, 3.8, 1.5), Pointing.DOWN, 30).rightClick()
                .withItem(stack("noellesroles:master_key"));
        scene.idle(10);
        Actors.swing(scene, conductor);
        WatheItemScenes.openDoor(scene, door, true);
        scene.overlay().showText(70)
                .text("万能钥匙能打开车上锁着的门，包括别人的房间和车厢门，没有冷却")
                .independent()
                .attachKeyFrame();
        scene.idle(10);
        // Switching items cancels a charge; releasing it would stab the passenger in reach.
        // 换手会取消蓄力；直接松开会刺中身边的乘客。
        Actors.charge(scene, killer, false);
        Actors.hold(scene, killer, ItemStack.EMPTY);
        Actors.turn(scene, killer, NORTH);
        scene.idle(70);
        scene.overlay().showText(70)
                .text("听到动静就开门进去：抓个现行，或者把人救出来")
                .independent()
                .attachKeyFrame();
        scene.idle(80);
        scene.overlay().showText(70)
                .text("被卡住的门打不开；你死后钥匙会掉在地上，谁捡到都能用")
                .independent();
        scene.idle(80);
        scene.markAsFinished();
    }
}
