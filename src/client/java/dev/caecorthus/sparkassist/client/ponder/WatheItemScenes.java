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

    private WatheItemScenes() {
    }

    static void register() {
        item("wathe:knife", scene("wathe/aisle", WatheItemScenes::knife));
        item("wathe:revolver", scene("wathe/aisle", WatheItemScenes::revolver));
        item("wathe:lockpick", scene("wathe/cabin", WatheItemScenes::lockpick));
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
                .text("按住右键约半秒蓄力，附近的人都能听到举刀声")
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
                .text("松开右键出刀：对方当场死亡，原地脸朝下倒成一具尸体")
                .independent()
                .attachKeyFrame();
        scene.idle(80);
        scene.overlay().showText(70)
                .text("出刀后刀要冷却一段时间；左键只会把人推开，不会伤人")
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
        shoot(scene, shooter, new Vec3d(2.0, 1.9, 5.0));
        Actors.fall(scene, killer);
        scene.idle(25);
        scene.overlay().showText(70)
                .colored(PonderPalette.GREEN)
                .text("打死杀手没有任何惩罚")
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
        shoot(scene, shooter, new Vec3d(2.0, 1.9, 5.0));
        Actors.fall(scene, passenger);
        scene.idle(4);
        Actors.hold(scene, shooter, ItemStack.EMPTY);
        scene.world().createItemEntity(new Vec3d(5.4, 1.5, 1.6), new Vec3d(-0.04, 0.12, 0.04),
                stack("wathe:revolver"));
        scene.idle(16);
        scene.overlay().showText(70)
                .colored(PonderPalette.RED)
                .text("对方照样死亡，你会当场掉枪，本局不能再捡枪")
                .independent();
        scene.idle(80);
        Actors.fall(scene, shooter);
        scene.idle(15);
        scene.overlay().showText(70)
                .colored(PonderPalette.RED)
                .text("默认你也会跟着死（服务器可以改成只掉枪）")
                .independent();
        scene.idle(80);
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
                .text("包厢门平时是锁着的：只有拿着这间房钥匙的人能开")
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
                .text("开锁器用不坏，也没有冷却；门开着几秒后会自己关上")
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

    /** The aisle plate, brought closer. 走廊底板，并拉近视角。 */
    static void stage(SceneBuilder scene) {
        scene.configureBasePlate(0, 0, 7);
        scene.scaleSceneView(VIEW_SCALE);
        scene.showBasePlate();
        scene.idle(10);
    }

    /** The corridor plate, then the cabin wall and door dropping in. 先显示走廊底板，再落下包厢墙与门。 */
    static void cabinStage(SceneBuilder scene, SceneBuildingUtil util) {
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
    private static void shoot(SceneBuilder scene, ElementLink<ActorElement> shooter, Vec3d target) {
        Actors.lookPitch(scene, shooter, -4);
        scene.overlay().showLine(PonderPalette.RED, new Vec3d(5.2, 2.05, 1.8), target, 6);
        scene.idle(3);
        Actors.lookPitch(scene, shooter, 0);
    }

    static ItemStack stack(String id) {
        return new ItemStack(Registries.ITEM.get(Identifier.of(id)));
    }
}
