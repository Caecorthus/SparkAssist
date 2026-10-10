package dev.caecorthus.sparkassist.client.ponder;

import static dev.caecorthus.sparkassist.client.ponder.SparkPonderDemos.item;
import static dev.caecorthus.sparkassist.client.ponder.SparkPonderDemos.scene;

import dev.doctor4t.wathe.block_entity.SmallDoorBlockEntity;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/**
 * Demos for Wathe's own items. Every mechanic shown was read from Wathe's source; numbers a server can change are
 * left out of the scenes (the guide states them).
 * Wathe 自带物品的演示。演示的每个机制都对照过 Wathe 源码；服务器可调的数值不放进演示（由指南书说明）。
 */
final class WatheItemScenes {
    static final float SOUTH = 0;
    static final float WEST = 90;
    static final float NORTH = 180;
    static final float EAST = -90;
    /**
     * Facing screen-right along the plate's diagonal. Ponder's default camera looks across that diagonal, so actors
     * walking along it are seen side-on with the right (item) hand towards the viewer.
     * 沿底板对角线朝屏幕右侧。Ponder 默认镜头横向看这条对角线，沿它走动的演员呈侧面，持物的右手朝向观看者。
     */
    static final float SCREEN_RIGHT = 45;
    static final float SCREEN_LEFT = -135;
    /** Ponder's default view fits a 5-wide plate; the 7-wide stages are brought closer. 默认视角适合 5 格底板，7 格舞台拉近一些。 */
    static final float VIEW_SCALE = 1.5f;
    /** The gun of an actor standing at (5.8, 1, 1.2) and facing screen-right. 站在 (5.8, 1, 1.2) 朝屏幕右侧的演员手中枪口的位置。 */
    private static final Vec3d REVOLVER_MUZZLE = new Vec3d(5.2, 2.05, 1.8);
    private static final String[] COCKTAILS = {"wathe:old_fashioned", "wathe:mojito", "wathe:martini",
            "wathe:cosmopolitan", "wathe:champagne"};

    private WatheItemScenes() {
    }

    static void register() {
        item("wathe:knife", scene("wathe/aisle", WatheItemScenes::knife));
        item("wathe:revolver", scene("wathe/aisle", WatheItemScenes::revolver));
        item("wathe:lockpick", scene("wathe/cabin", WatheItemScenes::lockpick));
        item("wathe:key", scene("wathe/cabin", WatheItemScenes::key));
        item("wathe:derringer", scene("wathe/aisle", WatheItemScenes::derringer));
        item("wathe:crowbar", scene("wathe/cabin_vent", WatheItemScenes::crowbar));
        for (String cocktail : COCKTAILS) {
            item(cocktail, scene("wathe/bar", WatheItemScenes::drinks));
        }
    }

    /**
     * Knife (KnifeItem / KnifeStabPayload): hold right-click for more than 10 ticks (the spear pose, with a sound
     * everyone nearby hears), release to stab the player within 3 blocks in front. The victim turns spectator and
     * leaves a body that falls face down where they faced; the attacker swings.
     * 刀：按住右键超过 10 tick（掷矛姿势，附近的人都能听到声音），松开刺中正前方 3 格内的玩家。被害者变为旁观者，
     * 留下一具朝其面向方向扑倒的尸体；出刀者挥一下手。
     */
    private static void knife(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("wathe_knife", "刀：蓄力后近身一刺");
        stage(scene);
        ElementLink<ActorElement> victim = Actors.enter(scene, RoleColors.CIVILIAN, Text.literal("平民"),
                new Vec3d(2.2, 1, 4.8), SCREEN_RIGHT, Direction.DOWN);
        scene.idle(5);
        ElementLink<ActorElement> killer = Actors.enter(scene, RoleColors.KILLER, Text.literal("杀手"),
                new Vec3d(5.8, 1, 1.2), SCREEN_RIGHT, Direction.DOWN);
        Actors.hold(scene, killer, stack("wathe:knife"));
        scene.idle(20);
        scene.overlay().showText(70)
                .text("刀只能刺中正前方 3 格以内的人")
                .independent()
                .attachKeyFrame();
        scene.idle(80);
        Actors.walk(scene, killer, new Vec3d(-2.2, 0, 2.2), 26);
        scene.idle(30);
        scene.overlay().showControls(new Vec3d(3.6, 3.6, 3.4), Pointing.DOWN, 30).rightClick()
                .withItem(stack("wathe:knife"));
        scene.overlay().showText(30)
                .text("按住右键约半秒蓄力，附近的人一般都能听到举刀声")
                .independent()
                .attachKeyFrame();
        Actors.charge(scene, killer, true);
        scene.idle(30);
        Actors.charge(scene, killer, false);
        Actors.swing(scene, killer);
        Actors.fall(scene, victim);
        scene.idle(20);
        scene.overlay().showText(70)
                .colored(PonderPalette.RED)
                .text("松开右键出刀：对方一般当场死亡，脸朝下倒成一具尸体")
                .independent()
                .attachKeyFrame();
        scene.idle(80);
        scene.overlay().showText(70)
                .text("杀人后刀要冷却一段时间；左键只会把人推开，不会伤人")
                .independent();
        scene.idle(80);
        scene.markAsFinished();
    }

    /**
     * Revolver (RevolverItem / GunShootPayload): one right-click fires, no charge; guns are held levelled (Wathe's
     * BipedEntityModelMixin). Killing a killer is free. An innocent who kills an innocent drops the gun, may not pick
     * one up again this round, and by default (shootInnocentPunishment = killShooter) dies too.
     * 左轮手枪：右键一下即开枪，无需蓄力；持枪时手臂端平（Wathe 的 BipedEntityModelMixin）。打死杀手没有惩罚。
     * 好人打死好人会掉枪、本局不能再捡枪，默认（shootInnocentPunishment = killShooter）自己也会死。
     */
    private static void revolver(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("wathe_revolver", "左轮手枪：开枪前先看清目标");
        stage(scene);
        ElementLink<ActorElement> shooter = Actors.enter(scene, RoleColors.VIGILANTE, Text.literal("义警"),
                new Vec3d(5.8, 1, 1.2), SCREEN_RIGHT, Direction.DOWN);
        Actors.hold(scene, shooter, stack("wathe:revolver"));
        scene.idle(20);
        scene.overlay().showText(70)
                .text("义警开局就带着一把左轮手枪，射程约 30 格")
                .independent()
                .attachKeyFrame();
        scene.idle(80);
        ElementLink<ActorElement> killer = Actors.enter(scene, RoleColors.KILLER, Text.literal("杀手"),
                new Vec3d(1.2, 1, 5.8), SCREEN_LEFT, Direction.DOWN);
        Actors.hold(scene, killer, stack("wathe:knife"));
        scene.idle(10);
        Actors.walk(scene, killer, new Vec3d(0.8, 0, -0.8), 20);
        scene.idle(25);
        scene.overlay().showControls(new Vec3d(5.8, 3.6, 1.2), Pointing.DOWN, 30).rightClick()
                .withItem(stack("wathe:revolver"));
        scene.overlay().showText(40)
                .text("点一下右键就开枪，不用蓄力")
                .independent()
                .attachKeyFrame();
        scene.idle(15);
        shoot(scene, shooter, REVOLVER_MUZZLE, new Vec3d(2.0, 1.9, 5.0));
        Actors.fall(scene, killer);
        scene.idle(25);
        scene.overlay().showText(70)
                .colored(PonderPalette.GREEN)
                .text("打死杀手阵营的人没有惩罚")
                .independent();
        scene.idle(80);
        scene.overlay().showText(70)
                .text("每开一枪都要冷却一会儿，没打中也算一枪")
                .independent();
        scene.idle(80);
        Actors.leave(scene, killer, Direction.UP);
        scene.idle(20);
        ElementLink<ActorElement> passenger = Actors.enter(scene, RoleColors.CIVILIAN, Text.literal("平民"),
                new Vec3d(1.2, 1, 5.8), SCREEN_LEFT, Direction.DOWN);
        scene.idle(10);
        Actors.walk(scene, passenger, new Vec3d(0.8, 0, -0.8), 20);
        scene.overlay().showText(40)
                .text("可如果打中的是好人……")
                .independent()
                .attachKeyFrame();
        scene.idle(40);
        shoot(scene, shooter, REVOLVER_MUZZLE, new Vec3d(2.0, 1.9, 5.0));
        Actors.fall(scene, passenger);
        scene.idle(4);
        Actors.hold(scene, shooter, ItemStack.EMPTY);
        scene.world().createItemEntity(new Vec3d(5.4, 1.5, 1.6), new Vec3d(-0.04, 0.12, 0.04),
                stack("wathe:revolver"));
        Actors.fall(scene, shooter);
        scene.idle(16);
        scene.overlay().showText(80)
                .colored(PonderPalette.RED)
                .text("对方照样死亡；你当场掉枪、本局不能再捡枪，默认自己也会跟着死")
                .independent();
        scene.idle(90);
        scene.overlay().showText(60)
                .text("（服务器可以改成只掉枪、不赔命）")
                .independent();
        scene.idle(70);
        scene.markAsFinished();
    }

    /**
     * Lockpick (LockpickItem, SmallDoorBlock): room doors stay locked to everyone but the holder of that room's key;
     * a lockpick opens a locked door at once, never wears out and has no cooldown; sneak-use jams the door so keys
     * and lockpicks fail. Doors close by themselves after a few seconds. The door slides via its block-entity
     * renderer, so the scene drives the block entity's synced event like the server does.
     * 开锁器：包厢门只对持有该房钥匙的人开放；开锁器能立刻打开锁着的门，用不坏、没有冷却；潜行使用会把门卡住，
     * 钥匙和开锁器都打不开。门几秒后会自动关上。门由方块实体渲染器推拉，所以演示像服务端一样触发方块实体的同步事件。
     */
    private static void lockpick(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("wathe_lockpick", "开锁器：打开锁着的门");
        cabinStage(scene, util);
        BlockPos door = util.grid().at(3, 1, 3);
        ElementLink<ActorElement> owner = Actors.enter(scene, RoleColors.CIVILIAN, Text.literal("平民"),
                new Vec3d(3.5, 1, 5.2), NORTH, Direction.DOWN);
        scene.idle(15);
        scene.overlay().showOutline(PonderPalette.WHITE, "door", util.select().fromTo(3, 1, 3, 3, 2, 3), 70);
        scene.overlay().showText(70)
                .text("包厢门平时是锁着的：一般只有拿着这间房钥匙的人能开")
                .independent()
                .attachKeyFrame();
        scene.idle(80);
        ElementLink<ActorElement> killer = Actors.enter(scene, RoleColors.KILLER, Text.literal("杀手"),
                new Vec3d(0.5, 1, 1.5), EAST, Direction.DOWN);
        Actors.hold(scene, killer, stack("wathe:lockpick"));
        scene.idle(10);
        Actors.walk(scene, killer, new Vec3d(3, 0, 0), 30);
        scene.idle(32);
        Actors.turn(scene, killer, SOUTH);
        scene.idle(10);
        scene.overlay().showControls(new Vec3d(3.5, 3.8, 1.5), Pointing.DOWN, 30).rightClick()
                .withItem(stack("wathe:lockpick"));
        scene.idle(10);
        Actors.swing(scene, killer);
        openDoor(scene, door, true);
        scene.overlay().showText(70)
                .text("杀手用开锁器右键锁着的门，门会立刻打开")
                .independent()
                .attachKeyFrame();
        scene.idle(80);
        scene.overlay().showText(60)
                .text("开锁器用不坏，开门也没有冷却；门开着几秒后会自己关上")
                .independent();
        scene.idle(40);
        openDoor(scene, door, false);
        scene.idle(40);
        Actors.sneak(scene, killer, true);
        scene.idle(5);
        scene.overlay().showControls(new Vec3d(3.5, 3.8, 1.5), Pointing.DOWN, 30).rightClick().whileSneaking()
                .withItem(stack("wathe:lockpick"));
        scene.idle(10);
        Actors.swing(scene, killer);
        scene.overlay().showOutline(PonderPalette.RED, "door", util.select().fromTo(3, 1, 3, 3, 2, 3), 70);
        scene.overlay().showText(70)
                .colored(PonderPalette.RED)
                .text("潜行时右键：把门卡住一段时间，期间钥匙和开锁器都打不开")
                .independent()
                .attachKeyFrame();
        scene.idle(80);
        Actors.sneak(scene, killer, false);
        scene.markAsFinished();
    }

    /**
     * Room key (KeyItem, SmallDoorBlock.onUse): a key opens only the door whose lock matches the room on its label;
     * the door closes by itself after 5 s and is still locked, so leaving the room needs the key too. Closing needs
     * none. Another room's key fails (locked sound, a hint only its holder sees).
     * 房间钥匙：钥匙只能打开锁与说明上的房间相符的门；门 5 秒后自动关上且仍然锁着，所以出门也要钥匙。关门不需要钥匙。
     * 别的房间的钥匙打不开（播放上锁声，只有本人能看到提示）。
     */
    private static void key(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("wathe_key", "房间钥匙：只开自己的房间");
        cabinStage(scene, util);
        BlockPos door = util.grid().at(3, 1, 3);
        ElementLink<ActorElement> owner = Actors.enter(scene, RoleColors.CIVILIAN, Text.literal("平民"),
                new Vec3d(0.8, 1, 1.5), EAST, Direction.DOWN);
        Actors.hold(scene, owner, stack("wathe:key"));
        scene.idle(15);
        scene.overlay().showText(70)
                .text("开局每人发一把房间钥匙，钥匙的说明里写着是哪一间")
                .independent()
                .attachKeyFrame();
        scene.idle(80);
        Actors.walk(scene, owner, new Vec3d(2.7, 0, 0.3), 30);
        scene.idle(32);
        Actors.turn(scene, owner, SOUTH);
        scene.idle(10);
        scene.overlay().showControls(new Vec3d(3.5, 3.8, 1.8), Pointing.DOWN, 30).rightClick()
                .withItem(stack("wathe:key"));
        scene.idle(10);
        Actors.swing(scene, owner);
        openDoor(scene, door, true);
        scene.overlay().showText(60)
                .text("拿着这间房的钥匙右键房门，门就会滑开")
                .independent()
                .attachKeyFrame();
        scene.idle(20);
        Actors.walk(scene, owner, new Vec3d(0, 0, 3.2), 30);
        scene.idle(80);
        openDoor(scene, door, false);
        scene.overlay().showText(70)
                .text("门开着 5 秒后会自己关上，关上后仍然是锁着的")
                .independent();
        scene.idle(80);
        ElementLink<ActorElement> stranger = Actors.enter(scene, RoleColors.CIVILIAN, Text.literal("乘客"),
                new Vec3d(6.2, 1, 1.5), WEST, Direction.DOWN);
        Actors.hold(scene, stranger, stack("wathe:key"));
        scene.idle(10);
        Actors.walk(scene, stranger, new Vec3d(-2.7, 0, 0.3), 30);
        scene.idle(32);
        Actors.turn(scene, stranger, SOUTH);
        scene.idle(10);
        scene.overlay().showControls(new Vec3d(3.5, 3.8, 1.8), Pointing.DOWN, 30).rightClick()
                .withItem(stack("wathe:key"));
        scene.idle(10);
        Actors.swing(scene, stranger);
        scene.overlay().showOutline(PonderPalette.RED, "door", util.select().fromTo(3, 1, 3, 3, 2, 3), 70);
        scene.overlay().showText(70)
                .colored(PonderPalette.RED)
                .text("别的房间的钥匙打不开，只会听到一声上锁的响动")
                .independent()
                .attachKeyFrame();
        scene.idle(80);
        Actors.leave(scene, stranger, Direction.UP);
        scene.idle(10);
        Actors.turn(scene, owner, NORTH);
        scene.idle(10);
        scene.overlay().showControls(new Vec3d(3.5, 3.8, 4.7), Pointing.DOWN, 30).rightClick()
                .withItem(stack("wathe:key"));
        scene.idle(10);
        Actors.swing(scene, owner);
        openDoor(scene, door, true);
        scene.overlay().showText(70)
                .text("从房间里出门也要用钥匙；关门不需要钥匙")
                .independent()
                .attachKeyFrame();
        scene.idle(60);
        Actors.walk(scene, owner, new Vec3d(0, 0, -3.2), 30);
        scene.idle(40);
        openDoor(scene, door, false);
        scene.idle(20);
        scene.markAsFinished();
    }

    /**
     * Derringer (DerringerItem, GunShootPayload, GameFunctions.killPlayer), as dealt to every Loose End: aims 7
     * blocks, one shot, then only a click; any kill credited to the holder reloads it (only they hear it). It never
     * drops on death and has no wrong-target punishment. Loose Ends is last-alive-wins.
     * 德林加手枪（每个亡命徒开局都有）：射程 7 格，只有一发，打空后只会咔哒一声；持有者每杀一个人（任何方式）都会自动
     * 重新装弹（只有自己听得到）。死后不会掉落，也没有误杀惩罚。生还模式中最后活下来的人获胜。
     */
    static void derringer(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("wathe_derringer", "德林加手枪：只有一发");
        stage(scene);
        Text looseEnd = Text.literal("亡命徒");
        ElementLink<ActorElement> shooter = Actors.enter(scene, RoleColors.LOOSE_END, looseEnd,
                new Vec3d(5.8, 1, 1.2), SCREEN_RIGHT, Direction.DOWN);
        Actors.hold(scene, shooter, stack("wathe:derringer"));
        scene.idle(10);
        ElementLink<ActorElement> first = Actors.enter(scene, RoleColors.LOOSE_END, looseEnd,
                new Vec3d(1.6, 1, 5.4), SCREEN_LEFT, Direction.DOWN);
        Actors.hold(scene, first, stack("wathe:knife"));
        scene.idle(15);
        scene.overlay().showText(80)
                .text("生还模式里人人都是亡命徒：每人一把德林加、一把刀和一根撬棍，最后活下来的人获胜")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        scene.overlay().showControls(new Vec3d(5.8, 3.6, 1.2), Pointing.DOWN, 30).rightClick()
                .withItem(stack("wathe:derringer"));
        scene.overlay().showText(50)
                .text("右键开枪，但一般只能打中 7 格以内的人")
                .independent()
                .attachKeyFrame();
        scene.idle(15);
        shoot(scene, shooter, REVOLVER_MUZZLE, new Vec3d(2.2, 1.9, 4.8));
        Actors.fall(scene, first);
        scene.idle(45);
        Actors.leave(scene, first, Direction.UP);
        scene.idle(15);
        ElementLink<ActorElement> second = Actors.enter(scene, RoleColors.LOOSE_END, looseEnd,
                new Vec3d(1.6, 1, 5.4), SCREEN_LEFT, Direction.DOWN);
        Actors.hold(scene, second, stack("wathe:knife"));
        scene.idle(20);
        scene.overlay().showControls(new Vec3d(5.8, 3.6, 1.2), Pointing.DOWN, 30).rightClick()
                .withItem(stack("wathe:derringer"));
        scene.overlay().showText(70)
                .colored(PonderPalette.RED)
                .text("一把德林加只有一发子弹：打空后再开枪只会“咔哒”一声")
                .independent()
                .attachKeyFrame();
        scene.idle(80);
        Actors.hold(scene, shooter, stack("wathe:knife"));
        scene.idle(10);
        Actors.walk(scene, shooter, new Vec3d(-2.4, 0, 2.4), 30);
        scene.idle(32);
        Actors.charge(scene, shooter, true);
        scene.idle(16);
        Actors.charge(scene, shooter, false);
        Actors.swing(scene, shooter);
        Actors.fall(scene, second);
        scene.overlay().showText(50)
                .text("换成刀蓄力出手，再干掉一个……")
                .independent();
        scene.idle(55);
        Actors.hold(scene, shooter, stack("wathe:derringer"));
        scene.overlay().showText(70)
                .colored(PonderPalette.GREEN)
                .text("……德林加就自动装好了下一发：用任何方式杀死一个人都会重新装弹")
                .independent()
                .attachKeyFrame();
        scene.idle(80);
        scene.overlay().showText(70)
                .text("上弹声只有自己听得到；德林加死后也不会掉落")
                .independent();
        scene.idle(80);
        scene.markAsFinished();
    }

    /**
     * Crowbar (CrowbarItem, DoorBlockEntity.blast, VentHatchBlock): right-click a locked or jammed door (or a train
     * door) and it is blasted open: it no longer closes by itself, by hand or by key, and cannot be jammed, until a
     * NoellesRoles Engineer repairs it. Loud (about 40 blocks). Floor vent hatches pry open too and resist hands for
     * 30 s. Never wears out, 10 s default cooldown, one per round from the shop.
     * 撬棍：右键锁着或被卡住的门（或列车门），门被撬开：不会自己关上，徒手和钥匙都关不上，也卡不住，直到 NoellesRoles
     * 的工程师修好。声音很响（约 40 格）。地面的通风口盖板也能撬开，之后 30 秒内徒手开关不了。用不坏，默认冷却 10 秒，
     * 商店每局限购 1 根。
     */
    private static void crowbar(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("wathe_crowbar", "撬棍：强行撬开房门");
        cabinStage(scene, util);
        BlockPos door = util.grid().at(3, 1, 3);
        BlockPos vent = util.grid().at(5, 1, 1);
        Actors.enter(scene, RoleColors.CIVILIAN, Text.literal("平民"), new Vec3d(3.5, 1, 5.2), NORTH,
                Direction.DOWN);
        scene.idle(10);
        scene.overlay().showOutline(PonderPalette.WHITE, "door", util.select().fromTo(3, 1, 3, 3, 2, 3), 70);
        scene.overlay().showText(70)
                .text("包厢门锁着，平常没有钥匙进不去")
                .independent()
                .attachKeyFrame();
        scene.idle(80);
        ElementLink<ActorElement> killer = Actors.enter(scene, RoleColors.KILLER, Text.literal("杀手"),
                new Vec3d(0.5, 1, 1.5), EAST, Direction.DOWN);
        Actors.hold(scene, killer, stack("wathe:crowbar"));
        scene.idle(10);
        Actors.walk(scene, killer, new Vec3d(3, 0, 0), 30);
        scene.idle(32);
        Actors.turn(scene, killer, SOUTH);
        scene.idle(10);
        scene.overlay().showControls(new Vec3d(3.5, 3.8, 1.5), Pointing.DOWN, 30).rightClick()
                .withItem(stack("wathe:crowbar"));
        scene.idle(10);
        Actors.swing(scene, killer);
        openDoor(scene, door, true);
        scene.overlay().showText(80)
                .text("杀手用撬棍右键锁着的门（被卡住的门也行），门被撬开")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        scene.overlay().showOutline(PonderPalette.RED, "door", util.select().fromTo(3, 1, 3, 3, 2, 3), 110);
        scene.overlay().showText(110)
                .colored(PonderPalette.RED)
                .text("撬开的门会一直开着：不会自己关上，也卡不住，只有工程师能修好")
                .independent()
                .attachKeyFrame();
        scene.idle(120);
        scene.overlay().showText(70)
                .text("撬门声很响，约 40 格内都听得到")
                .independent();
        scene.idle(80);
        Actors.turn(scene, killer, EAST);
        scene.idle(5);
        Actors.walk(scene, killer, new Vec3d(1.0, 0, 0), 12);
        scene.idle(15);
        Actors.lookPitch(scene, killer, 45);
        scene.overlay().showControls(new Vec3d(5.5, 2.4, 1.5), Pointing.DOWN, 30).rightClick()
                .withItem(stack("wathe:crowbar"));
        scene.idle(10);
        Actors.swing(scene, killer);
        scene.world().modifyBlock(vent, state -> state.with(Properties.OPEN, true), false);
        scene.overlay().showText(80)
                .text("地上的通风口盖板也能撬开，之后 30 秒内别人徒手开关不了")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        Actors.lookPitch(scene, killer, 0);
        scene.overlay().showText(70)
                .text("撬棍用不坏，默认冷却 10 秒；商店每局只能买 1 根")
                .independent();
        scene.idle(80);
        scene.markAsFinished();
    }

    /**
     * Trays, platters and drinks (FoodPlatterBlock.onUse, CocktailItem, PlayerMoodComponent): an empty-hand
     * right-click takes one random serving straight into the hand; the plate never runs out, but nobody carrying
     * something from that plate gets another. Holding right-click 2 s drinks a cocktail, which counts for the
     * "drink" task only while that task is showing; food from platters works the same for "eat".
     * 托盘、餐盘与饮品：空手右键随机拿一份，直接放到手上；盘子永远拿不完，但身上带着这盘里东西的人拿不了第二份。
     * 按住右键 2 秒喝完一杯，只有任务栏里正好有“喝点东西”时才算完成；餐盘里的食物对“吃点东西”同理。
     */
    private static void drinks(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("wathe_drinks", "托盘与饮品：喝点东西");
        setStage(scene, util);
        Vec3d tray = new Vec3d(3.5, 3.0, 3.5);
        scene.overlay().showText(70)
                .text("车厢里摆着饮品托盘和餐盘，盘里原有的东西拿不完")
                .independent()
                .attachKeyFrame();
        scene.idle(60);
        ElementLink<ActorElement> passenger = Actors.enter(scene, RoleColors.CIVILIAN, Text.literal("平民"),
                new Vec3d(1.0, 1, 6.0), SCREEN_LEFT, Direction.DOWN);
        scene.idle(10);
        Actors.walk(scene, passenger, new Vec3d(1.6, 0, -1.6), 25);
        scene.idle(28);
        scene.overlay().showControls(tray, Pointing.DOWN, 30).rightClick();
        scene.idle(10);
        Actors.swing(scene, passenger);
        Actors.hold(scene, passenger, stack("wathe:mojito"));
        scene.overlay().showText(80)
                .text("空手右键：随机拿到一份，直接放在手上")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        Actors.hold(scene, passenger, ItemStack.EMPTY);
        scene.idle(10);
        scene.overlay().showControls(tray, Pointing.DOWN, 30).rightClick();
        scene.idle(10);
        Actors.swing(scene, passenger);
        scene.overlay().showText(80)
                .text("收进背包也没用：身上有这盘里的东西就拿不了第二份（服务员能拿两份）")
                .independent();
        scene.idle(90);
        Actors.hold(scene, passenger, stack("wathe:mojito"));
        scene.idle(10);
        scene.overlay().showText(40)
                .text("按住右键 2 秒喝完")
                .independent()
                .attachKeyFrame();
        Actors.charge(scene, passenger, true);
        scene.idle(40);
        Actors.charge(scene, passenger, false);
        Actors.hold(scene, passenger, ItemStack.EMPTY);
        scene.idle(5);
        scene.overlay().showText(90)
                .colored(PonderPalette.GREEN)
                .text("喝完能完成“去喝点东西”任务，理智回升；餐盘里的食物对“去吃点东西”同理")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showText(80)
                .text("只有任务栏里正好出现这项任务时，吃喝才算完成任务")
                .independent();
        scene.idle(90);
        scene.markAsFinished();
    }

    /** The aisle plate, brought closer. 走廊底板，并拉近视角。 */
    static void stage(SceneBuilder scene) {
        scene.configureBasePlate(0, 0, 7);
        scene.scaleSceneView(VIEW_SCALE);
        scene.showBasePlate();
        scene.idle(10);
    }

    /** The corridor plate, then the cabin wall and door dropping in. 先显示走廊底板，再落下包厢墙与门。 */
    static void cabinStage(SceneBuilder scene, SceneBuildingUtil util) {
        setStage(scene, util);
    }

    /** The plate, then everything standing on it (walls, tables, beds, lamps) dropping in. 先显示底板，再落下其上的一切（墙、桌、床、灯）。 */
    static void setStage(SceneBuilder scene, SceneBuildingUtil util) {
        stage(scene);
        scene.world().showSection(util.select().layersFrom(1), Direction.DOWN);
        scene.idle(15);
    }

    /** Slide a cabin door like the server's synced block event (type 1, data = open). 像服务端同步方块事件一样推拉包厢门。 */
    static void openDoor(SceneBuilder scene, BlockPos lower, boolean open) {
        scene.world().modifyBlockEntity(lower, SmallDoorBlockEntity.class,
                door -> door.onSyncedBlockEvent(1, open ? 1 : 0));
    }

    /** A shot: the shooter's head kicks up 4 degrees as RevolverItem.use does, and a short tracer marks the line.
     * 开枪：射手视角像 RevolverItem.use 那样上抬 4 度，并用一道短线标出弹道。 */
    static void shoot(SceneBuilder scene, ElementLink<ActorElement> shooter, Vec3d muzzle, Vec3d target) {
        Actors.lookPitch(scene, shooter, -4);
        scene.overlay().showLine(PonderPalette.RED, muzzle, target, 6);
        scene.idle(3);
        Actors.lookPitch(scene, shooter, 0);
    }

    static ItemStack stack(String id) {
        return new ItemStack(Registries.ITEM.get(Identifier.of(id)));
    }
}
