package dev.caecorthus.sparkassist.client.ponder;

import static dev.caecorthus.sparkassist.client.ponder.SparkPonderDemos.role;
import static dev.caecorthus.sparkassist.client.ponder.SparkPonderDemos.scene;
import static dev.caecorthus.sparkassist.client.ponder.WatheItemScenes.EAST;
import static dev.caecorthus.sparkassist.client.ponder.WatheItemScenes.NORTH;
import static dev.caecorthus.sparkassist.client.ponder.WatheItemScenes.SCREEN_LEFT;
import static dev.caecorthus.sparkassist.client.ponder.WatheItemScenes.SCREEN_RIGHT;
import static dev.caecorthus.sparkassist.client.ponder.WatheItemScenes.SOUTH;
import static dev.caecorthus.sparkassist.client.ponder.WatheItemScenes.WEST;
import static dev.caecorthus.sparkassist.client.ponder.WatheItemScenes.stack;

import dev.doctor4t.wathe.block_entity.BeveragePlateBlockEntity;
import dev.doctor4t.wathe.index.WatheParticles;
import dev.doctor4t.wathe.index.WatheProperties;
import java.util.List;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Demos for NoellesRoles' civilian roles, opened only from the role's guide page; each needs NoellesRoles. They
 * follow the Spark server stack, as the guide does: SparkStrength retunes the Bartender's drinks and adds the
 * Engineer's power restoration and the Toxicologist's Blue Vitriol (with SparkTraits' blue poison).
 * NoellesRoles 平民阵营职业的演示，只从该职业的指南页打开；都需要 NoellesRoles。与指南书一致，按 Spark 服务器的实际规则
 * 演示：SparkStrength 调整了酒保的酒效，并加入了工程师的电力恢复系统和毒理学家的蓝矾（配合 SparkTraits 的蓝毒）。
 */
final class NoellesCivilianScenes {
    /** The gun of an actor standing at (5.8, 1, 1.2) and facing screen-right. 站在 (5.8, 1, 1.2) 朝屏幕右侧的演员手中枪口的位置。 */
    private static final Vec3d PISTOL_MUZZLE = new Vec3d(5.2, 2.05, 1.8);
    /** Poison markers rise like Wathe's and SparkTraits' plate particles. 中毒标记像 Wathe 与 SparkTraits 的盘子粒子一样上升。 */
    private static final Vec3d POISON_RISE = new Vec3d(0, 0.05, 0);

    private NoellesCivilianScenes() {
    }

    static void register() {
        role("sparkassist:roles/noellesroles/conductor", List.of("noellesroles"),
                scene("wathe/cabin", NoellesCivilianScenes::conductor));
        role("sparkassist:roles/noellesroles/demon_hunter", List.of("noellesroles"),
                scene("wathe/aisle", NoellesCivilianScenes::demonHunter));
        role("sparkassist:roles/noellesroles/engineer", List.of("noellesroles", "sparkstrength"),
                scene("wathe/cabin", NoellesCivilianScenes::engineer),
                scene("wathe/lit_carriage", NoellesCivilianScenes::engineerPower));
        role("sparkassist:roles/noellesroles/bartender", List.of("noellesroles"),
                scene("wathe/bar", NoellesCivilianScenes::bartender));
        role("sparkassist:roles/noellesroles/toxicologist", List.of("noellesroles", "sparkstrength", "sparktraits"),
                scene("wathe/bar", NoellesCivilianScenes::toxicologist));
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

    /**
     * Demon Hunter (NoellesRoles Noellesroles ON_PSYCHO_START / ON_PSYCHO_END, DemonHunterPistolItem,
     * DemonHunterShootC2SPacket; client highlight in NoellesrolesClient): whenever anyone starts psycho mode, each
     * living Demon Hunter gets a Demon Hunter pistol with 2 bullets (2 more if they already have one; none while
     * carrying another gun), first shot after 5 s. It is held levelled (Wathe's gun tag) and not hidden. Right-click
     * hits the first player within 5 blocks; every pull spends a bullet and cools down 10 s. A psycho target loses one
     * armour, then killPlayer runs, so the default single armour does not save them (extra armour, shields or a
     * bodyguard can); a Jester (outside stasis) dies and returns a bullet; other players are unharmed (SparkStrength
     * and SparkWitch let it break drones, devices and puppets). The pistol goes when its bullets run out or no psycho
     * is left. The hunter sees psychos through walls in the hunter's colour, except the Silencer and invisible players.
     * 猎魔人：每当有人开启疯魔，每个活着的猎魔人都会拿到一把装 2 发子弹的猎魔枪（已有则加 2 发；身上带着别的枪则不发），
     * 5 秒后才能开第一枪。它按 Wathe 的枪标签端平，且不会被隐藏。右键命中 5 格内的第一个玩家；每扣一次扳机消耗 1 发、
     * 冷却 10 秒。命中疯魔者会先打掉 1 层护盾再执行击杀，所以默认的 1 层护盾救不了对方（额外护甲、护盾或保镖可以）；
     * 命中小丑（非静止状态）直接击杀并返还 1 发；打其他玩家没有效果（SparkStrength 与 SparkWitch 让它能打坏无人机、
     * 装置和傀儡）。子弹打光或没有人在疯魔时枪就会消失。猎魔人能隔墙看到疯魔者（猎魔人颜色），静语者和隐身者除外。
     */
    private static void demonHunter(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("role_demon_hunter", "猎魔人：猎魔枪专打疯魔者");
        WatheItemScenes.stage(scene);
        int hunterColor = RoleColors.of("noellesroles:demon_hunter", 0x8C9BB4);
        ElementLink<ActorElement> hunter = Actors.enter(scene, hunterColor, Text.literal("猎魔人"),
                new Vec3d(5.8, 1, 1.2), SCREEN_RIGHT, Direction.DOWN);
        scene.idle(5);
        ElementLink<ActorElement> killer = Actors.enter(scene, RoleColors.KILLER, Text.literal("杀手"),
                new Vec3d(1.2, 1, 5.8), SCREEN_LEFT, Direction.DOWN);
        Actors.hold(scene, killer, stack("wathe:knife"));
        scene.idle(15);
        scene.overlay().showText(70)
                .text("猎魔人是专门对付疯魔的好人，平时没有猎魔枪")
                .independent()
                .attachKeyFrame();
        scene.idle(80);
        scene.overlay().showControls(new Vec3d(1.2, 3.6, 5.8), Pointing.DOWN, 60)
                .withItem(stack("wathe:psycho_mode"));
        scene.overlay().showText(60)
                .text("一旦有人开启疯魔模式……")
                .independent()
                .attachKeyFrame();
        scene.idle(30);
        Actors.psycho(scene, killer, true);
        Actors.hold(scene, killer, stack("wathe:bat"));
        Actors.retint(scene, killer, RoleColors.KILLER,
                Text.literal("urscrewed").formatted(Formatting.OBFUSCATED, Formatting.DARK_RED));
        Actors.hold(scene, hunter, stack("noellesroles:demon_hunter_pistol"));
        scene.idle(40);
        scene.overlay().showText(80)
                .text("……猎魔人一般会马上拿到猎魔枪：2 发子弹，5 秒后能开枪")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        Actors.highlight(scene, killer, hunterColor, 80);
        scene.overlay().showText(80)
                .text("你还能隔墙看到疯魔中的人（方框示意；静语者和隐身者除外）")
                .independent();
        scene.idle(90);
        scene.overlay().showControls(new Vec3d(5.8, 3.6, 1.2), Pointing.DOWN, 30).rightClick()
                .withItem(stack("noellesroles:demon_hunter_pistol"));
        scene.idle(10);
        // The line stops where the pistol's 5-block reach ends. 弹道线停在猎魔枪 5 格射程的尽头。
        WatheItemScenes.shoot(scene, hunter, PISTOL_MUZZLE, new Vec3d(2.4, 2.0, 4.6));
        scene.overlay().showText(80)
                .colored(PonderPalette.RED)
                .text("射程只有 5 格：离得太远打不中，白白浪费 1 发")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        Actors.walk(scene, killer, new Vec3d(1.4, 0, -1.4), 40);
        scene.overlay().showText(70)
                .text("每开一枪默认冷却 10 秒；等疯魔者走进 5 格再开枪")
                .independent();
        scene.idle(80);
        scene.overlay().showControls(new Vec3d(5.8, 3.6, 1.2), Pointing.DOWN, 30).rightClick()
                .withItem(stack("noellesroles:demon_hunter_pistol"));
        scene.idle(30);
        WatheItemScenes.shoot(scene, hunter, PISTOL_MUZZLE, new Vec3d(2.9, 1.9, 4.1));
        Actors.fall(scene, killer);
        scene.idle(4);
        Actors.hold(scene, hunter, ItemStack.EMPTY);
        scene.overlay().showText(80)
                .colored(PonderPalette.GREEN)
                .text("命中会多打掉 1 层护盾，所以疯魔者一般一枪毙命")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        scene.overlay().showText(70)
                .text("子弹打光、或所有疯魔都结束后，猎魔枪就会消失")
                .independent();
        scene.idle(80);
        scene.overlay().showText(80)
                .text("它只能击杀疯魔中的人和小丑，打其他人没有任何效果")
                .independent();
        scene.idle(90);
        scene.markAsFinished();
    }

    /**
     * Engineer's repair tool (NoellesRoles Noellesroles DoorInteraction handler, RepairToolItem,
     * EngineerDoorHighlightRenderer; Wathe SmallDoorBlock, DoorBlockEntity, CrowbarItem): right-click a door with it:
     * a blasted door is repaired and shut; else a jammed door is unjammed; else the door is shut and jammed for
     * 1 minute, so keys and lockpicks fail (a crowbar still blasts it). Every use cools down 3 min (60 s at the
     * start); the server answers a handled use with SUCCESS and swings the arm for everyone nearby, though living
     * onlookers see an empty hand. When a door is blasted (crowbar, Pig God) or jammed by a sneaking lockpick (not by
     * the Engineer's own lock), the Engineer gets a message and sees that door outlined red through walls for 5 s.
     * 工程师的维修工具：拿着它右键门：被撬坏的门会被修好并关上；否则被堵住的门会被解开；否则把门关上并堵住 1 分钟，
     * 期间钥匙和开锁器都打不开（撬棍仍能撬开）。每次使用默认冷却 3 分钟（开局 60 秒）；服务端处理成功后会让手臂挥一下，
     * 附近的人都看得到，但别的活人看到的是空手。门被撬开（撬棍、猪神）或被潜行开锁器堵住时（工程师自己锁门不算），
     * 工程师会收到提示，并隔墙看到那扇门红色高亮 5 秒。
     */
    private static void engineer(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("role_engineer", "工程师：维修工具锁门、修门");
        WatheItemScenes.cabinStage(scene, util);
        BlockPos door = util.grid().at(3, 1, 3);
        Selection doorBlocks = util.select().fromTo(3, 1, 3, 3, 2, 3);
        Vec3d doorFace = new Vec3d(3.5, 1, 3.0);
        Vec3d atDoor = new Vec3d(1.6, 1, 2.4);
        Actors.enter(scene, RoleColors.CIVILIAN, Text.literal("平民"), new Vec3d(3.5, 1, 5.2), NORTH,
                Direction.DOWN);
        ElementLink<ActorElement> engineer = Actors.enter(scene,
                RoleColors.of("noellesroles:engineer", 0xC8A03C), Text.literal("工程师"),
                new Vec3d(0.6, 1, 1.5), EAST, Direction.DOWN);
        Actors.hold(scene, engineer, stack("noellesroles:repair_tool"));
        scene.idle(15);
        scene.overlay().showText(70)
                .text("工程师开局带着【维修工具】，别的活人看不见你手里的它")
                .independent()
                .attachKeyFrame();
        scene.idle(80);
        Actors.walk(scene, engineer, atDoor.subtract(0.6, 1, 1.5), 25);
        scene.idle(28);
        Actors.turn(scene, engineer, facing(atDoor, doorFace));
        scene.idle(8);
        scene.overlay().showControls(new Vec3d(3.5, 3.4, 3.0), Pointing.DOWN, 30).rightClick()
                .withItem(stack("noellesroles:repair_tool"));
        scene.idle(10);
        Actors.swing(scene, engineer);
        scene.overlay().showOutline(PonderPalette.WHITE, "door", doorBlocks, 70);
        scene.overlay().showText(70)
                .text("对着正常的门右键：把门关上并锁住 1 分钟")
                .independent()
                .attachKeyFrame();
        scene.idle(50);
        Actors.walk(scene, engineer, new Vec3d(-0.8, 0, -1.2), 25);
        scene.idle(30);
        ElementLink<ActorElement> killer = Actors.enter(scene, RoleColors.KILLER, Text.literal("杀手"),
                new Vec3d(6.4, 1, 1.5), WEST, Direction.DOWN);
        Actors.hold(scene, killer, stack("wathe:lockpick"));
        scene.idle(10);
        Vec3d killerSpot = new Vec3d(4.6, 1, 1.5);
        Actors.walk(scene, killer, killerSpot.subtract(6.4, 1, 1.5), 25);
        scene.idle(28);
        Actors.turn(scene, killer, facing(killerSpot, doorFace));
        scene.idle(8);
        scene.overlay().showControls(new Vec3d(3.5, 3.4, 3.0), Pointing.DOWN, 30).rightClick()
                .withItem(stack("wathe:lockpick"));
        scene.idle(10);
        scene.overlay().showOutline(PonderPalette.WHITE, "door", doorBlocks, 70);
        scene.overlay().showText(80)
                .colored(PonderPalette.RED)
                .text("锁住期间，钥匙和开锁器都打不开，只会听到上锁声")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        Actors.hold(scene, killer, stack("wathe:crowbar"));
        scene.idle(10);
        scene.overlay().showControls(new Vec3d(3.5, 3.4, 3.0), Pointing.DOWN, 30).rightClick()
                .withItem(stack("wathe:crowbar"));
        scene.idle(10);
        Actors.swing(scene, killer);
        WatheItemScenes.openDoor(scene, door, true);
        scene.overlay().showText(32)
                .text("但撬棍还是能强行撬开")
                .independent()
                .attachKeyFrame();
        scene.idle(40);
        scene.overlay().showOutline(PonderPalette.RED, "door", doorBlocks, 100);
        scene.overlay().showText(90)
                .text("有门被撬或被堵时你会收到提示，那扇门在你眼里隔墙红色高亮 5 秒")
                .independent()
                .attachKeyFrame();
        scene.idle(50);
        Actors.leave(scene, killer, Direction.UP);
        scene.idle(50);
        scene.overlay().showText(42)
                .text("……3 分钟冷却后（演示缩短了时间）")
                .independent();
        Actors.walk(scene, engineer, new Vec3d(0.8, 0, 1.2), 25);
        scene.idle(28);
        Actors.turn(scene, engineer, facing(atDoor, doorFace));
        scene.idle(12);
        scene.overlay().showControls(new Vec3d(3.5, 3.4, 3.0), Pointing.DOWN, 30).rightClick()
                .withItem(stack("noellesroles:repair_tool"));
        scene.idle(10);
        Actors.swing(scene, engineer);
        WatheItemScenes.openDoor(scene, door, false);
        scene.overlay().showText(80)
                .colored(PonderPalette.GREEN)
                .text("对撬坏的门右键：把门修好并关上，不再一直敞着")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        scene.overlay().showText(80)
                .text("对被堵住的门右键则是解开；每次使用默认冷却 3 分钟")
                .independent();
        scene.idle(90);
        scene.markAsFinished();
    }

    /**
     * Engineer's power restoration (SparkStrength EngineerShopService, EngineerPowerRestorationService,
     * EngineerRules): a 300-coin entry in the Engineer's shop that can only be bought during a blackout. Buying it ends
     * the blackout at once: lamps return to their state before it, night vision and blindness are cleared for everyone
     * (whatever their source), and the price is split
     * among the living killers, whose blackout cooldown becomes 90 s. Ponder keeps its world fully lit, so the stage is
     * dimmed (StageLights) as in the blackout demo.
     * 工程师的电力恢复系统：工程师商店里 300 金币的条目，只能在停电时购买（否则买不成）。买下立刻结束停电：灯恢复停电前
     * 的状态，所有人的夜视和失明被清除；这笔钱平分给存活的杀手，杀手的停电冷却变为 90 秒。Ponder 场景始终满亮度，所以和停电演示一样调暗舞台。
     */
    private static void engineerPower(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("role_engineer_power", "工程师：电力恢复系统");
        WatheItemScenes.setStage(scene, util);
        Selection lamps = util.select().position(1, 2, 5).add(util.select().position(4, 2, 5))
                .add(util.select().position(5, 2, 1)).add(util.select().position(5, 2, 4));
        Vec3d civilianSpot = new Vec3d(2.2, 1, 2.5);
        Vec3d engineerSpot = new Vec3d(0.8, 1, 4.2);
        ElementLink<ActorElement> civilian = Actors.enter(scene, RoleColors.CIVILIAN, Text.literal("平民"),
                civilianSpot, SCREEN_RIGHT, Direction.DOWN);
        ElementLink<ActorElement> engineer = Actors.enter(scene,
                RoleColors.of("noellesroles:engineer", 0xC8A03C), Text.literal("工程师"),
                engineerSpot, SCREEN_LEFT, Direction.DOWN);
        scene.idle(10);
        ElementLink<ActorElement> killer = Actors.enter(scene, RoleColors.KILLER, Text.literal("杀手"),
                new Vec3d(4.4, 1, 0.8), SCREEN_RIGHT, Direction.DOWN);
        Actors.hold(scene, killer, stack("wathe:knife"));
        scene.idle(15);
        scene.overlay().showControls(new Vec3d(4.4, 3.6, 0.8), Pointing.DOWN, 50)
                .withItem(stack("wathe:blackout"));
        scene.overlay().showText(80)
                .text("杀手开了停电：全车熄灯，大多数人看不清")
                .independent()
                .attachKeyFrame();
        scene.idle(20);
        scene.world().modifyBlocks(lamps, state -> lamp(state, false), false);
        StageLights.lights(scene, false);
        scene.idle(70);
        Vec3d killerSpot = new Vec3d(3.4, 1, 1.3);
        Actors.walk(scene, killer, killerSpot.subtract(4.4, 1, 0.8), 30);
        scene.idle(32);
        Actors.turn(scene, killer, facing(killerSpot, civilianSpot));
        Actors.charge(scene, killer, true);
        scene.overlay().showControls(engineerSpot.add(0, 2.6, 0), Pointing.DOWN, 70)
                .withItem(stack("sparkstrength:power_restoration"));
        scene.overlay().showText(72)
                .text("停电时，工程师能在商店花 300 金币买【电力恢复系统】")
                .independent()
                .attachKeyFrame();
        scene.idle(80);
        scene.world().modifyBlocks(lamps, state -> lamp(state, true), false);
        StageLights.lights(scene, true);
        // Switching items cancels a charge; releasing it would stab. 换手会取消蓄力；直接松开就会出刀。
        Actors.charge(scene, killer, false);
        Actors.hold(scene, killer, ItemStack.EMPTY);
        scene.overlay().showText(80)
                .colored(PonderPalette.GREEN)
                .text("停电立刻结束：灯恢复原状，所有人的失明和夜视都被解除")
                .independent()
                .attachKeyFrame();
        scene.idle(15);
        Actors.turn(scene, civilian, facing(civilianSpot, killerSpot));
        Actors.turn(scene, engineer, facing(engineerSpot, killerSpot));
        scene.idle(30);
        Actors.walk(scene, killer, new Vec3d(1.8, 0, -0.6), 25);
        scene.idle(25);
        Actors.leave(scene, killer, Direction.UP);
        scene.idle(20);
        scene.overlay().showText(90)
                .text("代价：这 300 金币会平分给存活的杀手，杀手的停电冷却变成 90 秒")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showText(70)
                .text("只能在停电时买；买一次就等于送杀手 300 金币，别乱用")
                .independent();
        scene.idle(80);
        scene.markAsFinished();
    }

    /**
     * Bartender (NoellesRoles BaseSpiritItem, DefenseVialApplyMixin, WhiskeyShieldEffect, Noellesroles KillPlayer
     * handler, HiddenEquipmentHelper, CocktailItemMixin; SparkStrength BartenderRules): buy a base spirit and
     * ingredients, mix them in the inventory (ingredient on the cursor, right-click the spirit; at most 3, no repeats).
     * Right-clicking a drink tray or food platter with the spirit adds it to the plate, where everyone sees it; the
     * next empty-handed taker usually gets it first (a SparkStrength timed-bomb tray hands out something else) and it
     * leaves the plate. Holding right-click 2 s drinks it (ember sugar: at once): without ice, Slowness II and
     * Blindness for 3 s, then +20% sanity and the ingredient effects; whiskey gives a 30 s shield (special liqueur
     * doubles these times) that cancels one lethal kill (not a shot-innocent penalty, an assassination, voodoo, a fall
     * out of the train or a forced kill) and plays the psycho armour sound around the victim. Base spirit and
     * ingredients are hidden in a Bartender's hand. Drinkers of the train's own cocktails glow green for the Bartender
     * for 40 s.
     * 酒保：买基酒和材料，在背包里调（材料拿在鼠标上右键基酒；最多 3 种，不能重复）。拿着基酒右键饮品托盘或餐盘就把它
     * 放上去，所有人都看得到；下一个空手来拿的人一般会先拿到它（SparkStrength 的定时炸弹托盘例外），它随即离开盘子。
     * 按住右键 2 秒喝下（加火种方糖则瞬间喝完）：没加冰块时先缓慢 II 加失明 3 秒，然后回复 20% 理智并触发材料效果；
     * 威士忌给 30 秒护盾（特调利口酒让这些时间翻倍），挡下一次致命击杀（误杀惩罚、刺客猜中、巫毒、掉出列车与强制击杀
     * 除外），并在受害者周围响起疯魔护甲声。基酒与材料在酒保手里别人看不见。喝过列车自带鸡尾酒的人会在酒保眼里亮绿框 40 秒。
     */
    private static void bartender(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("role_bartender", "酒保：调一杯酒请人喝");
        WatheItemScenes.setStage(scene, util);
        BlockPos tray = util.grid().at(3, 2, 3);
        Vec3d trayTop = new Vec3d(3.5, 3.0, 3.5);
        ItemStack spirit = stack("noellesroles:base_spirit");
        ElementLink<ActorElement> bartender = Actors.enter(scene,
                RoleColors.of("noellesroles:bartender", 0xD9F1F0), Text.literal("酒保"),
                new Vec3d(6.0, 1, 1.0), SCREEN_RIGHT, Direction.DOWN);
        scene.idle(10);
        Actors.walk(scene, bartender, new Vec3d(-1.6, 0, 1.6), 25);
        scene.idle(28);
        scene.overlay().showControls(new Vec3d(4.4, 3.6, 2.6), Pointing.DOWN, 60).withItem(spirit);
        scene.overlay().showText(70)
                .text("酒保在商店买【基酒】和材料，比如【威士忌】")
                .independent()
                .attachKeyFrame();
        scene.idle(80);
        scene.overlay().showControls(new Vec3d(4.4, 3.6, 2.6), Pointing.DOWN, 60).rightClick()
                .withItem(stack("noellesroles:whiskey"));
        scene.overlay().showText(80)
                .text("在背包里把材料拿在鼠标上右键基酒，就调了进去（每杯最多 3 种）")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        Actors.hold(scene, bartender, spirit);
        scene.overlay().showText(70)
                .text("别的活人看不见你手里的基酒和材料")
                .independent();
        scene.idle(80);
        scene.overlay().showControls(trayTop, Pointing.DOWN, 30).rightClick().withItem(spirit);
        scene.idle(10);
        Actors.swing(scene, bartender);
        Actors.hold(scene, bartender, ItemStack.EMPTY);
        serve(scene, tray, spirit);
        scene.overlay().showText(80)
                .text("拿着调好的基酒右键饮品托盘（餐盘也行），酒就摆了上去")
                .independent()
                .attachKeyFrame();
        scene.idle(60);
        ElementLink<ActorElement> vigilante = Actors.enter(scene, RoleColors.VIGILANTE, Text.literal("义警"),
                new Vec3d(1.0, 1, 6.0), SCREEN_LEFT, Direction.DOWN);
        Actors.hold(scene, vigilante, stack("wathe:revolver"));
        scene.idle(10);
        Actors.walk(scene, vigilante, new Vec3d(1.6, 0, -1.6), 25);
        scene.idle(25);
        Actors.hold(scene, vigilante, ItemStack.EMPTY);
        scene.idle(5);
        scene.overlay().showControls(trayTop, Pointing.DOWN, 30).rightClick();
        scene.idle(10);
        Actors.swing(scene, vigilante);
        Actors.hold(scene, vigilante, spirit);
        takeServed(scene, tray, spirit.getItem());
        scene.overlay().showText(80)
                .text("下一个空手来拿的人一般会先拿到这杯酒，不一定是你想请的人")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        scene.overlay().showText(90)
                .text("一般按住右键 2 秒喝下；没加冰块会先缓慢加失明几秒，之后才生效")
                .independent()
                .attachKeyFrame();
        Actors.charge(scene, vigilante, true);
        scene.idle(40);
        Actors.charge(scene, vigilante, false);
        Actors.hold(scene, vigilante, ItemStack.EMPTY);
        scene.idle(60);
        scene.overlay().showText(72)
                .colored(PonderPalette.GREEN)
                .text("威士忌：默认 30 秒护盾，一般能挡下一次致命攻击")
                .independent()
                .attachKeyFrame();
        scene.idle(26);
        Vec3d killerSpot = new Vec3d(1.3, 1, 5.5);
        Vec3d vigilanteSpot = new Vec3d(2.6, 1, 4.4);
        ElementLink<ActorElement> killer = Actors.enter(scene, RoleColors.KILLER, Text.literal("杀手"),
                new Vec3d(0.6, 1, 6.4), SCREEN_LEFT, Direction.DOWN);
        Actors.hold(scene, killer, stack("wathe:knife"));
        scene.idle(10);
        Actors.walk(scene, killer, killerSpot.subtract(0.6, 1, 6.4), 20);
        scene.idle(22);
        Actors.turn(scene, killer, facing(killerSpot, vigilanteSpot));
        scene.idle(6);
        Actors.charge(scene, killer, true);
        scene.idle(16);
        Actors.charge(scene, killer, false);
        Actors.swing(scene, killer);
        scene.overlay().showText(80)
                .text("被刀刺中也没倒下：护盾挡住了这一刀，附近响起一声盔甲声")
                .independent()
                .attachKeyFrame();
        scene.idle(20);
        Actors.turn(scene, vigilante, facing(vigilanteSpot, killerSpot));
        Actors.hold(scene, vigilante, stack("wathe:revolver"));
        scene.idle(70);
        scene.overlay().showText(80)
                .text("另外：喝过车上普通饮品的人，在你视线里会亮绿色描边 40 秒")
                .independent();
        scene.idle(90);
        scene.markAsFinished();
    }

    /**
     * Toxicologist (Wathe BeveragePlateBlockEntityMixin + NoellesRoles CanSeePoison listener; NoellesRoles
     * AntidoteItem, NoellesrolesClient highlight, HiddenEquipmentHelper; SparkStrength ToxicologistBlueVitriolService,
     * HiddenEquipmentHelperMixin; SparkTraits BeveragePlateBluePoisonParticleMixin, ConsciencePoisonerService): the
     * Toxicologist sees the red skull over poisoned plates and beds, like killers and spectators. Poisoned players in
     * sight glow in the Toxicologist's colour (blue-poisoned ones light blue). Blue Vitriol on a poisoned plate turns
     * its native poison into the Toxicologist's blue poison: the red poisoner is cleared, a blue skull shows only to
     * the Toxicologist, Conscience Poisoners and spectators; blue poison kills only non-civilians and drains other
     * civilians' sanity (the Toxicologist gains sanity and speed instead). Holding right-click on a poisoned player
     * within 3 blocks for 1.5 s (spear pose) cures Wathe's poison, not blue poison; a right-click clears a plate's red
     * poison; 3 min default cooldown. Antidote and vitriol are hidden in hand from other living players.
     * 毒理学家：和杀手、旁观者一样看得到下了毒的盘子和床上的小红骷髅；视线内中毒的玩家显示为毒理学家的颜色（中蓝毒的
     * 为浅蓝色）。对有毒的盘子用蓝矾，原生的毒就变成毒理学家的蓝毒：红色下毒者被清除，蓝骷髅只有毒理学家、善良毒师和
     * 旁观者看得到；蓝毒只毒死好人阵营以外的人，别的好人只会掉理智（毒理学家自己反而回理智、加速）。对 3 格内中毒的
     * 玩家按住右键 1.5 秒（掷矛姿势）可解 Wathe 的毒，解不了蓝毒；右键盘子可清掉红毒；默认冷却 3 分钟。解毒剂与蓝矾
     * 拿在手上别的活人看不见。
     */
    private static void toxicologist(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("role_toxicologist", "毒理学家：识毒、解毒与蓝矾");
        WatheItemScenes.setStage(scene, util);
        Vec3d plate = new Vec3d(3.5, 2.1, 3.5);
        Vec3d trayTop = new Vec3d(3.5, 3.0, 3.5);
        int toxicologistColor = RoleColors.of("noellesroles:toxicologist", 0xB8295A);
        ParticleEffect bluePoison = (ParticleEffect) Registries.PARTICLE_TYPE.get(
                Identifier.of("sparktraits", "blue_poison"));
        Effects.Marker redSkull = Effects.mark(scene, WatheParticles.POISON, plate, POISON_RISE, 0.2f);
        ElementLink<ActorElement> toxicologist = Actors.enter(scene, toxicologistColor, Text.literal("毒理学家"),
                new Vec3d(6.0, 1, 1.0), SCREEN_RIGHT, Direction.DOWN);
        scene.idle(10);
        Vec3d atTray = new Vec3d(4.4, 1, 2.6);
        Actors.walk(scene, toxicologist, atTray.subtract(6.0, 1, 1.0), 25);
        scene.idle(28);
        scene.overlay().showText(90)
                .text("下了毒的盘子和床会冒小红骷髅：你、杀手、旁观者等少数人看得见")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        Vec3d civilianSpot = new Vec3d(1.4, 1, 3.2);
        Vec3d curerSpot = new Vec3d(3.6, 1, 1.8);
        ElementLink<ActorElement> civilian = Actors.enter(scene, RoleColors.CIVILIAN, Text.literal("平民"),
                new Vec3d(1.0, 1, 6.0), SCREEN_LEFT, Direction.DOWN);
        scene.idle(10);
        Actors.walk(scene, civilian, civilianSpot.subtract(1.0, 1, 6.0), 35);
        // Lasts until the cure below completes. 一直持续到下面解毒完成。
        Actors.highlight(scene, civilian, toxicologistColor, 153);
        scene.idle(10);
        scene.overlay().showText(80)
                .text("视线内中毒的人会显示成你的身份颜色，中蓝毒的显示为蓝色（方框示意）")
                .independent()
                .attachKeyFrame();
        scene.idle(30);
        Actors.turn(scene, civilian, facing(civilianSpot, curerSpot));
        scene.idle(60);
        Actors.hold(scene, toxicologist, stack("noellesroles:antidote"));
        Actors.walk(scene, toxicologist, curerSpot.subtract(atTray), 15);
        scene.idle(17);
        Actors.turn(scene, toxicologist, facing(curerSpot, civilianSpot));
        scene.idle(6);
        scene.overlay().showControls(curerSpot.add(0, 2.6, 0), Pointing.DOWN, 30).rightClick()
                .withItem(stack("noellesroles:antidote"));
        Actors.charge(scene, toxicologist, true);
        scene.overlay().showText(80)
                .colored(PonderPalette.GREEN)
                .text("拿【解毒剂】对 3 格内中毒的人按住右键 1.5 秒解毒（蓝毒解不了）")
                .independent()
                .attachKeyFrame();
        scene.idle(30);
        Actors.charge(scene, toxicologist, false);
        scene.idle(60);
        scene.overlay().showText(80)
                .text("别的活人看不见你手里的解毒剂和蓝矾；对方走出 3 格会中断")
                .independent();
        scene.idle(40);
        Actors.leave(scene, civilian, Direction.UP);
        scene.idle(50);
        Actors.hold(scene, toxicologist, stack("sparkstrength:blue_vitriol"));
        Actors.walk(scene, toxicologist, atTray.subtract(curerSpot), 15);
        scene.idle(17);
        Actors.turn(scene, toxicologist, SCREEN_RIGHT);
        scene.idle(8);
        scene.overlay().showControls(trayTop, Pointing.DOWN, 30).rightClick()
                .withItem(stack("sparkstrength:blue_vitriol"));
        scene.idle(10);
        Actors.swing(scene, toxicologist);
        Actors.hold(scene, toxicologist, ItemStack.EMPTY);
        Effects.unmark(scene, redSkull);
        Effects.Marker blueSkull = Effects.mark(scene, bluePoison, plate, POISON_RISE, 0.2f);
        scene.overlay().showText(80)
                .text("用【蓝矾】右键它：盘子上的红毒变成你的蓝毒，骷髅变成蓝色")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        scene.overlay().showText(80)
                .text("蓝毒只会毒死好人阵营以外的人；别的好人吃到只会掉一截理智")
                .independent();
        scene.idle(90);
        scene.overlay().showText(80)
                .text("蓝骷髅只有你、善良毒师和旁观者看得到，普通杀手看不见了")
                .independent();
        scene.idle(90);
        scene.overlay().showText(90)
                .text("也能按住右键给自己解毒、右键清掉盘子上的红毒；用完默认冷却 3 分钟")
                .independent();
        scene.idle(100);
        Effects.unmark(scene, blueSkull);
        scene.markAsFinished();
    }

    /** The yaw that faces from {@code from} towards {@code to}. 从 from 面向 to 的朝向。 */
    private static float facing(Vec3d from, Vec3d to) {
        return (float) (MathHelper.atan2(from.x - to.x, to.z - from.z) * MathHelper.DEGREES_PER_RADIAN);
    }

    /** Put a glass on a plate the way DefenseVialApplyMixin adds it. 像 DefenseVialApplyMixin 那样把一杯酒放到盘子上。 */
    private static void serve(SceneBuilder scene, BlockPos plate, ItemStack glass) {
        scene.world().modifyBlockEntity(plate, BeveragePlateBlockEntity.class,
                entity -> entity.getStoredItems().add(glass.copy()));
    }

    /** Take a served glass off a plate, as an empty-handed taker does. 像空手取用的人那样从盘子上拿走那杯酒。 */
    private static void takeServed(SceneBuilder scene, BlockPos plate, Item glass) {
        scene.world().modifyBlockEntity(plate, BeveragePlateBlockEntity.class,
                entity -> entity.getStoredItems().removeIf(stored -> stored.isOf(glass)));
    }

    /** A Wathe lamp powered or cut, as WorldBlackoutComponent sets lit and active. 像 WorldBlackoutComponent 那样设置灯的 lit 与 active。 */
    private static BlockState lamp(BlockState state, boolean on) {
        if (state.contains(Properties.LIT)) {
            state = state.with(Properties.LIT, on);
        }
        return state.contains(WatheProperties.ACTIVE) ? state.with(WatheProperties.ACTIVE, on) : state;
    }
}
