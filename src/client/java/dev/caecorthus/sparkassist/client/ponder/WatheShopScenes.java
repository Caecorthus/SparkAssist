package dev.caecorthus.sparkassist.client.ponder;

import static dev.caecorthus.sparkassist.client.ponder.SparkPonderDemos.item;
import static dev.caecorthus.sparkassist.client.ponder.SparkPonderDemos.scene;
import static dev.caecorthus.sparkassist.client.ponder.WatheItemScenes.EAST;
import static dev.caecorthus.sparkassist.client.ponder.WatheItemScenes.NORTH;
import static dev.caecorthus.sparkassist.client.ponder.WatheItemScenes.SCREEN_LEFT;
import static dev.caecorthus.sparkassist.client.ponder.WatheItemScenes.SCREEN_RIGHT;
import static dev.caecorthus.sparkassist.client.ponder.WatheItemScenes.SOUTH;
import static dev.caecorthus.sparkassist.client.ponder.WatheItemScenes.WEST;
import static dev.caecorthus.sparkassist.client.ponder.WatheItemScenes.stack;

import dev.doctor4t.wathe.index.WatheParticles;
import dev.doctor4t.wathe.index.WatheProperties;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Demos for the killer shop's other wares (KillerShopBuilder). Effects the game plays only on the server (the
 * grenade blast, the firecracker's bang, a poisoning) or only for killers (the poison marker) are scripted here, and
 * the captions say who can see what. Times the game takes longer over are shortened and the captions say so.
 * 杀手商店其余商品的演示。游戏只在服务端播放（手雷爆炸、爆竹炸响、中毒）或只有杀手看得到（中毒标记）的效果由这里用
 * 脚本补上，并在字幕里说明谁能看到。游戏里耗时更长的过程做了缩短，字幕会注明。
 */
final class WatheShopScenes {
    private static final Vec3d POISON_RISE = new Vec3d(0, 0.05, 0);

    private WatheShopScenes() {
    }

    static void register() {
        item("wathe:grenade", scene("wathe/cabin", WatheShopScenes::grenade));
        item("wathe:firecracker", scene("wathe/aisle", WatheShopScenes::firecracker));
        item("wathe:poison_vial", scene("wathe/bar", WatheShopScenes::poisonVial));
        item("wathe:scorpion", scene("wathe/bedroom", WatheShopScenes::scorpion));
        item("wathe:body_bag", scene("wathe/aisle", WatheShopScenes::bodyBag));
        item("wathe:blackout", scene("wathe/lit_carriage", WatheShopScenes::blackout));
        item("wathe:psycho_mode", scene("wathe/aisle", WatheShopScenes::psychoMode));
    }

    /**
     * Grenade (GrenadeItem, GrenadeEntity; Spark: SparkStrength GrenadeExplosionMixin): one right-click throws it,
     * it blows up on the first thing it touches and kills everyone within 3 blocks, the thrower included. On Spark
     * walls and closed doors shield; plain Wathe uses a cube with no cover. 3 min cooldown, single use.
     * 手雷：右键一下扔出，撞到任何东西即爆炸，炸死 3 格内所有人（包括扔的人）。Spark 上墙和关着的门能挡住爆炸；
     * 原版 Wathe 是没有掩体的立方体范围。冷却 3 分钟，用一次就没。
     */
    private static void grenade(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("wathe_grenade", "手雷：撞到东西就爆炸");
        WatheItemScenes.cabinStage(scene, util);
        Text civilian = Text.literal("平民");
        ElementLink<ActorElement> near = Actors.enter(scene, RoleColors.CIVILIAN, civilian,
                new Vec3d(2.2, 1, 0.8), EAST, Direction.DOWN);
        ElementLink<ActorElement> nearer = Actors.enter(scene, RoleColors.CIVILIAN, civilian,
                new Vec3d(0.8, 1, 2.2), EAST, Direction.DOWN);
        ElementLink<ActorElement> sheltered = Actors.enter(scene, RoleColors.CIVILIAN, civilian,
                new Vec3d(1.6, 1, 4.4), NORTH, Direction.DOWN);
        scene.idle(10);
        ElementLink<ActorElement> killer = Actors.enter(scene, RoleColors.KILLER, Text.literal("杀手"),
                new Vec3d(6.4, 1, 1.5), WEST, Direction.DOWN);
        Actors.hold(scene, killer, stack("wathe:grenade"));
        scene.idle(20);
        scene.overlay().showText(80)
                .text("手雷：右键扔出去，一碰到墙、地面或人就立刻爆炸，没有引信")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        scene.overlay().showControls(new Vec3d(6.4, 3.6, 1.5), Pointing.DOWN, 30).rightClick()
                .withItem(stack("wathe:grenade"));
        scene.idle(10);
        Actors.swing(scene, killer);
        Actors.hold(scene, killer, ItemStack.EMPTY);
        Vec3d impact = new Vec3d(1.5, 1.05, 1.5);
        Actors.toss(scene, stack("wathe:thrown_grenade"), new Vec3d(6.4, 2.52, 1.5), impact, 11, 0.4, false,
                ThrownElement.Flight.UPRIGHT);
        scene.idle(11);
        Effects.burst(scene, WatheParticles.BIG_EXPLOSION, impact.add(0, 0.3, 0), 1, 0);
        Effects.burst(scene, ParticleTypes.SMOKE, impact.add(0, 0.2, 0), 60, 0.2);
        Effects.burst(scene, new ItemStackParticleEffect(ParticleTypes.ITEM, stack("wathe:thrown_grenade")),
                impact.add(0, 0.2, 0), 40, 0.5);
        Actors.fall(scene, near);
        Actors.fall(scene, nearer);
        blastRing(scene, impact, 3, 3, 90);
        scene.idle(5);
        scene.overlay().showText(90)
                .colored(PonderPalette.RED)
                .text("3 格内的人一般当场被炸死；扔的人自己站得太近也一样")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        Actors.highlight(scene, sheltered, 0x7AE04F, 90);
        scene.overlay().showText(90)
                .colored(PonderPalette.GREEN)
                .text("墙后的人没事：本服的手雷会被墙和关着的门挡住")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showText(90)
                .text("平着扔大约落在 5 格外；爆炸声很响，约 80 格内都听得到")
                .independent();
        scene.idle(100);
        scene.overlay().showText(80)
                .text("手雷用一次就没了，扔完默认要等 3 分钟才能再扔")
                .independent();
        scene.idle(90);
        scene.markAsFinished();
    }

    /**
     * The blast radius as a ring on the floor, kept to the corridor ({@code z <= wallZ}) because the wall shields
     * what lies behind it. 地面上的爆炸半径圆圈，只画在走廊一侧（z <= wallZ），因为墙后的范围被墙挡住了。
     */
    private static void blastRing(SceneBuilder scene, Vec3d center, double radius, double wallZ, int ticks) {
        int segments = 48;
        for (int i = 0; i < segments; i++) {
            double a = MathHelper.TAU * i / segments;
            double b = MathHelper.TAU * (i + 1) / segments;
            Vec3d from = center.add(Math.cos(a) * radius, 0, Math.sin(a) * radius);
            Vec3d to = center.add(Math.cos(b) * radius, 0, Math.sin(b) * radius);
            if (onPlate(from, wallZ) && onPlate(to, wallZ)) {
                scene.overlay().showLine(PonderPalette.RED, from, to, ticks);
            }
        }
    }

    private static boolean onPlate(Vec3d point, double maxZ) {
        return point.x >= 0 && point.x <= 7 && point.z >= 0 && point.z <= maxZ;
    }

    /**
     * Firecracker (FirecrackerItem, FirecrackerEntity): right-click the top of a block to set it down; it smokes,
     * then after 15 s plays the revolver's own gunshot sound (about 80 blocks) with a flash and smoke on the floor.
     * It hurts nobody and cannot be picked up. A real revolver shot usually also clicks close by.
     * 爆竹：右键方块顶面放下；它会冒烟，15 秒后发出与左轮完全相同的枪声（约 80 格内听得到），地上有闪光和烟。
     * 不伤人，也捡不回来。左轮真开枪时近处一般还有一声咔哒。
     */
    private static void firecracker(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("wathe_firecracker", "爆竹：一声假枪响");
        WatheItemScenes.stage(scene);
        ElementLink<ActorElement> killer = Actors.enter(scene, RoleColors.KILLER, Text.literal("杀手"),
                new Vec3d(5.2, 1, 0.8), SCREEN_RIGHT, Direction.DOWN);
        Actors.hold(scene, killer, stack("wathe:firecracker"));
        scene.idle(15);
        scene.overlay().showText(70)
                .text("爆竹：对着地面（方块的顶面）右键放下")
                .independent()
                .attachKeyFrame();
        scene.idle(40);
        Actors.lookPitch(scene, killer, 50);
        scene.overlay().showControls(new Vec3d(4.4, 1.8, 1.6), Pointing.DOWN, 30).rightClick()
                .withItem(stack("wathe:firecracker"));
        scene.idle(10);
        Actors.swing(scene, killer);
        Actors.hold(scene, killer, ItemStack.EMPTY);
        Vec3d spot = new Vec3d(4.4, 1.02, 1.6);
        ElementLink<ThrownElement> cracker = Actors.toss(scene, stack("wathe:firecracker"), spot, spot, 1, 0, true,
                ThrownElement.Flight.LIES_FLAT);
        Effects.Marker smoke = Effects.mark(scene, ParticleTypes.SMOKE, spot.add(0, 0.06, 0),
                new Vec3d(0, 0.03, 0), 0.3f);
        scene.idle(5);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.WHITE, cracker,
                new Box(spot.x - 0.3, spot.y - 0.02, spot.z - 0.3, spot.x + 0.3, spot.y + 0.15, spot.z + 0.3), 70);
        scene.idle(25);
        Actors.lookPitch(scene, killer, 0);
        scene.overlay().showText(70)
                .text("放下后会一直冒烟，过 15 秒才炸响")
                .independent()
                .attachKeyFrame();
        Actors.walk(scene, killer, new Vec3d(1.3, 0, 4.4), 40);
        scene.idle(45);
        Actors.leave(scene, killer, Direction.UP);
        scene.idle(35);
        scene.overlay().showText(35)
                .text("……15 秒后（演示缩短了等待时间）")
                .independent();
        scene.idle(45);
        Effects.unmark(scene, smoke);
        Actors.remove(scene, cracker);
        Effects.burst(scene, WatheParticles.EXPLOSION, spot.add(0, 0.2, 0), 1, 0);
        Effects.burst(scene, ParticleTypes.SMOKE, spot.add(0, 0.1, 0), 25, 0.05);
        scene.overlay().showText(80)
                .colored(PonderPalette.RED)
                .text("砰！听起来和左轮开枪一模一样，远处也听得到")
                .independent()
                .attachKeyFrame();
        ElementLink<ActorElement> first = Actors.enter(scene, RoleColors.CIVILIAN, Text.literal("平民"),
                new Vec3d(0.9, 1, 5.6), SCREEN_LEFT, Direction.DOWN);
        ElementLink<ActorElement> second = Actors.enter(scene, RoleColors.VIGILANTE, Text.literal("义警"),
                new Vec3d(6.2, 1, 5.8), NORTH, Direction.DOWN);
        Actors.hold(scene, second, stack("wathe:revolver"));
        scene.idle(15);
        Actors.walk(scene, first, new Vec3d(2.0, 0, -2.6), 30);
        Actors.walk(scene, second, new Vec3d(-1.2, 0, -2.8), 30);
        scene.idle(75);
        scene.overlay().showText(80)
                .text("它不会伤人：用来制造假枪声，把人引过去或分散注意力")
                .independent();
        scene.idle(90);
        scene.overlay().showText(80)
                .text("左轮真开枪时近处一般还有一声“咔哒”；爆竹没有，闪光和烟出现在地上")
                .independent();
        scene.idle(90);
        scene.overlay().showText(60)
                .text("放下就拿不回来，也没人能拆掉")
                .independent();
        scene.idle(70);
        scene.markAsFinished();
    }

    /**
     * Poison vial (FoodPlatterBlock.onUse, PoisonUtils, PlayerPoisonComponent): used on a stocked drink tray or
     * food platter, it poisons the next single portion taken; the plate is clean again after that. The portion
     * looks like any other and poisons only once eaten or drunk; death follows 40-70 s later with a body. Killer
     * roles, spectators and the poison-seeing roles (the Toxicologist, Grand Witch, Murderous Witch, Guardian Angel,
     * the accomplices; WatheClient, CanSeePoison) see the marker over a poisoned plate; only the victim hears the
     * heartbeat.
     * 毒药瓶：对有东西的饮品托盘或餐盘使用，下一份被拿走的就是毒的，之后盘子恢复干净。这份看起来与别的一样，吃下或喝下才
     * 中毒，40～70 秒后死亡并留下尸体。杀手阵营、旁观者和识毒身份（毒理学家、大魔女、杀意魔女、守护天使、各共犯）看得到盘子上的中毒标记；只有中毒的人听得到心跳。
     */
    private static void poisonVial(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("wathe_poison_vial", "毒药瓶：给托盘下毒");
        WatheItemScenes.setStage(scene, util);
        Vec3d tray = new Vec3d(3.5, 2.1, 3.5);
        scene.overlay().showText(70)
                .text("车厢里的饮品托盘和餐盘，空手右键就能拿一份")
                .independent()
                .attachKeyFrame();
        scene.idle(80);
        ElementLink<ActorElement> killer = Actors.enter(scene, RoleColors.KILLER, Text.literal("杀手"),
                new Vec3d(6.0, 1, 1.0), SCREEN_RIGHT, Direction.DOWN);
        Actors.hold(scene, killer, stack("wathe:poison_vial"));
        scene.idle(10);
        Actors.walk(scene, killer, new Vec3d(-1.6, 0, 1.6), 25);
        scene.idle(28);
        scene.overlay().showControls(tray.add(0, 0.9, 0), Pointing.DOWN, 30).rightClick()
                .withItem(stack("wathe:poison_vial"));
        scene.idle(10);
        Actors.swing(scene, killer);
        Actors.hold(scene, killer, ItemStack.EMPTY);
        Effects.Marker poison = Effects.mark(scene, WatheParticles.POISON, tray, POISON_RISE, 0.2f);
        scene.overlay().showText(80)
                .text("杀手拿毒药瓶右键托盘（餐盘也行），药瓶就用掉了")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        scene.overlay().showText(80)
                .colored(PonderPalette.RED)
                .text("下了毒的盘子会冒出小红骷髅：杀手、旁观者和识毒身份看得到")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        Actors.walk(scene, killer, new Vec3d(1.6, 0, -1.6), 25);
        scene.idle(25);
        Actors.leave(scene, killer, Direction.UP);
        scene.idle(10);
        ElementLink<ActorElement> victim = Actors.enter(scene, RoleColors.CIVILIAN, Text.literal("平民"),
                new Vec3d(1.0, 1, 6.0), SCREEN_LEFT, Direction.DOWN);
        scene.idle(10);
        Actors.walk(scene, victim, new Vec3d(1.6, 0, -1.6), 25);
        scene.idle(28);
        scene.overlay().showControls(tray.add(0, 0.9, 0), Pointing.DOWN, 30).rightClick();
        scene.idle(10);
        Actors.swing(scene, victim);
        Actors.hold(scene, victim, stack("wathe:mojito"));
        Effects.unmark(scene, poison);
        scene.overlay().showText(90)
                .text("下一个空手拿的人拿到的就是毒酒，托盘随即恢复干净")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showText(60)
                .text("喝下去才会中毒，光拿着没事；这杯看起来和别的一模一样")
                .independent()
                .attachKeyFrame();
        Actors.charge(scene, victim, true);
        scene.idle(40);
        Actors.charge(scene, victim, false);
        Actors.hold(scene, victim, ItemStack.EMPTY);
        scene.idle(30);
        scene.overlay().showText(50)
                .text("……一般 40～70 秒后（演示缩短了时间）")
                .independent();
        Actors.walk(scene, victim, new Vec3d(1.2, 0, 0.8), 30);
        scene.idle(60);
        Actors.fall(scene, victim);
        scene.overlay().showText(80)
                .colored(PonderPalette.RED)
                .text("没被解毒或护盾挡下，就会毒发身亡：死因“中毒”，留下尸体")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        scene.overlay().showText(80)
                .text("只有中毒的人自己听得到越来越快的心跳；毒理学家和毒师能看出谁中了毒")
                .independent();
        scene.idle(90);
        scene.markAsFinished();
    }

    /**
     * Scorpion (TrimmedBedBlock.onUse, PoisonUtils.bedPoison): right-click a Wathe bed to hide it there, silently.
     * Whoever lies down in that bed (or one next to it) is stung 2 s later unless up again by then, gets a message,
     * and dies 40-70 s later. One scorpion stings once. The marker shows to the same players as the vial's.
     * 蝎子：右键 Wathe 的床悄悄藏进去。之后谁躺进这张床（或紧挨着的床），2 秒后就会被蜇（除非已经起身），收到一条提示，
     * 40～70 秒后死亡。一只蝎子只蜇一次。中毒标记的可见范围与毒药瓶相同。
     */
    private static void scorpion(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("wathe_scorpion", "蝎子：藏进床里");
        WatheItemScenes.setStage(scene, util);
        BlockPos head = util.grid().at(3, 1, 3);
        Vec3d pillow = new Vec3d(3.5, 1.6, 3.5);
        ElementLink<ActorElement> killer = Actors.enter(scene, RoleColors.KILLER, Text.literal("杀手"),
                new Vec3d(6.0, 1, 1.0), SCREEN_RIGHT, Direction.DOWN);
        Actors.hold(scene, killer, stack("wathe:scorpion"));
        scene.idle(10);
        Actors.walk(scene, killer, new Vec3d(-2.8, 0, 0.8), 30);
        scene.idle(32);
        Actors.turn(scene, killer, SOUTH);
        scene.idle(10);
        scene.overlay().showControls(pillow.add(-0.5, 1, 0), Pointing.DOWN, 30).rightClick()
                .withItem(stack("wathe:scorpion"));
        scene.idle(10);
        Actors.swing(scene, killer);
        Actors.hold(scene, killer, ItemStack.EMPTY);
        Effects.Marker poison = Effects.mark(scene, WatheParticles.POISON, pillow, POISON_RISE, 0.2f);
        scene.overlay().showText(80)
                .text("蝎子：拿着它右键一张床，悄悄藏进去，没有任何声音")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        scene.overlay().showText(80)
                .colored(PonderPalette.RED)
                .text("藏了蝎子的床会冒出小红骷髅：杀手、旁观者和识毒身份看得到")
                .independent();
        scene.idle(90);
        Actors.walk(scene, killer, new Vec3d(2.8, 0, -0.8), 30);
        scene.idle(30);
        Actors.leave(scene, killer, Direction.UP);
        scene.idle(10);
        ElementLink<ActorElement> sleeper = Actors.enter(scene, RoleColors.CIVILIAN, Text.literal("平民"),
                new Vec3d(1.2, 1, 6.0), SCREEN_LEFT, Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(70)
                .text("空手右键空床就能躺下睡觉，白天黑夜都能睡")
                .independent()
                .attachKeyFrame();
        Actors.walk(scene, sleeper, new Vec3d(1.8, 0, -0.9), 25);
        scene.idle(28);
        Actors.turn(scene, sleeper, NORTH);
        scene.idle(8);
        scene.overlay().showControls(pillow.add(-0.5, 1, 0), Pointing.DOWN, 30).rightClick();
        scene.idle(10);
        Actors.lieDown(scene, sleeper, head, Direction.EAST);
        scene.idle(40);
        Effects.unmark(scene, poison);
        scene.overlay().showText(90)
                .colored(PonderPalette.RED)
                .text("躺下 2 秒就被蜇：屏幕下方提示“你在睡梦中感到什么东西在蜇你”")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showText(70)
                .text("要是 2 秒内就起身，就能躲过；一只蝎子只蜇一个人")
                .independent();
        scene.idle(80);
        Actors.getUp(scene, sleeper);
        scene.overlay().showText(50)
                .text("……一般 40～70 秒后（演示缩短了时间）")
                .independent();
        Actors.walk(scene, sleeper, new Vec3d(1.4, 0, 0.6), 30);
        scene.idle(60);
        Actors.fall(scene, sleeper);
        scene.overlay().showText(80)
                .colored(PonderPalette.RED)
                .text("没被解毒或护盾挡下，就会毒发身亡：死因“中毒”，留下尸体")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        scene.markAsFinished();
    }

    /**
     * Body bag (BodyBagItem.useOnEntity): right-click a body to make it vanish for everyone; a bagging sound fades
     * out over about 16 blocks. Things the victim dropped (a revolver) stay. Single use, 5 min default cooldown.
     * 裹尸袋：右键尸体，尸体对所有人立刻消失；装袋声约 16 格内渐弱可闻。死者掉落的东西（左轮）还在原地。一次性，
     * 默认冷却 5 分钟。
     */
    private static void bodyBag(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("wathe_body_bag", "裹尸袋：收走尸体");
        WatheItemScenes.stage(scene);
        ElementLink<ActorElement> victim = Actors.enter(scene, RoleColors.CIVILIAN, Text.literal("平民"),
                new Vec3d(2.2, 1, 4.8), SCREEN_RIGHT, Direction.DOWN);
        scene.idle(15);
        Actors.fall(scene, victim);
        scene.idle(25);
        scene.overlay().showText(60)
                .text("走廊里躺着一具尸体")
                .independent()
                .attachKeyFrame();
        scene.idle(70);
        ElementLink<ActorElement> killer = Actors.enter(scene, RoleColors.KILLER, Text.literal("杀手"),
                new Vec3d(6.0, 1, 1.0), SCREEN_RIGHT, Direction.DOWN);
        Actors.hold(scene, killer, stack("wathe:body_bag"));
        scene.idle(10);
        Actors.walk(scene, killer, new Vec3d(-2.6, 0, 2.6), 30);
        scene.idle(32);
        Actors.lookPitch(scene, killer, 30);
        scene.overlay().showControls(new Vec3d(1.6, 2.2, 5.4), Pointing.DOWN, 30).rightClick()
                .withItem(stack("wathe:body_bag"));
        scene.idle(10);
        Actors.swing(scene, killer);
        Actors.vanish(scene, victim);
        Actors.hold(scene, killer, ItemStack.EMPTY);
        scene.overlay().showText(80)
                .text("裹尸袋：对着尸体右键，尸体立刻消失")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        Actors.lookPitch(scene, killer, 0);
        scene.overlay().showText(80)
                .text("附近十几格内的人能听到一声装袋的声音，越远越轻")
                .independent();
        scene.idle(90);
        scene.overlay().showText(80)
                .text("尸体没了，路过的人就看不到；但死者掉的左轮等东西可能还在地上")
                .independent();
        scene.idle(90);
        scene.overlay().showText(90)
                .text("裹尸袋用一次就没了；就算再买一个，默认也要等 5 分钟才能再用")
                .independent();
        scene.idle(100);
        scene.markAsFinished();
    }

    /**
     * Blackout (PlayerShopComponent.useBlackout, WorldBlackoutComponent): bought in the shop, it fires at once with
     * nothing in the inventory. Every lamp goes out, by default for 30-40 s, with a countdown; most players go blind,
     * while killers, the witch faction and Veterans get night vision (some roles are spared). An Engineer can restore
     * power early. Killers usually share a 5 min cooldown. Ponder keeps its world fully
     * lit, so the scene dims its stage (StageLights) to stand in for the darkness.
     * 停电：在商店购买后立刻生效，不占物品栏。所有灯熄灭，默认 30～40 秒，屏幕显示倒计时；大多数人失明，杀手、魔女
     * 阵营和老兵获得夜视（部分身份不受影响）。工程师能提前恢复供电。杀手一般共用 5 分钟冷却。Ponder 场景始终满亮度，所以由场景调暗舞台（StageLights）来表示黑暗。
     */
    private static void blackout(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("wathe_blackout", "停电：全车熄灯");
        WatheItemScenes.setStage(scene, util);
        Selection lamps = util.select().position(1, 2, 5).add(util.select().position(4, 2, 5))
                .add(util.select().position(5, 2, 1)).add(util.select().position(5, 2, 4));
        ElementLink<ActorElement> victim = Actors.enter(scene, RoleColors.CIVILIAN, Text.literal("平民"),
                new Vec3d(2.0, 1, 2.4), SCREEN_RIGHT, Direction.DOWN);
        ElementLink<ActorElement> other = Actors.enter(scene, RoleColors.CIVILIAN, Text.literal("平民"),
                new Vec3d(2.8, 1, 4.6), SCREEN_LEFT, Direction.DOWN);
        scene.idle(10);
        ElementLink<ActorElement> killer = Actors.enter(scene, RoleColors.KILLER, Text.literal("杀手"),
                new Vec3d(5.0, 1, 0.9), SCREEN_RIGHT, Direction.DOWN);
        Actors.hold(scene, killer, stack("wathe:knife"));
        scene.idle(15);
        scene.overlay().showControls(new Vec3d(5.0, 3.6, 0.9), Pointing.DOWN, 60)
                .withItem(stack("wathe:blackout"));
        scene.overlay().showText(80)
                .text("杀手在商店（按 E 打开背包）买下【停电器】：立刻生效，不占物品栏")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        scene.world().modifyBlocks(lamps, state -> lamp(state, false), false);
        StageLights.lights(scene, false);
        scene.overlay().showText(90)
                .colored(PonderPalette.RED)
                .text("全车的灯一起熄灭，一般持续 30～40 秒，屏幕下方会显示倒计时")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        Actors.turn(scene, other, NORTH);
        scene.overlay().showText(90)
                .text("大多数人会失明，只看得清身边一点、不能疾跑；杀手、魔女阵营和老兵有夜视")
                .independent()
                .attachKeyFrame();
        scene.idle(50);
        Actors.turn(scene, other, SCREEN_LEFT);
        scene.idle(50);
        Actors.walk(scene, killer, new Vec3d(-1.8, 0, 0.3), 25);
        scene.idle(27);
        Actors.turn(scene, killer, SCREEN_RIGHT);
        scene.idle(6);
        Actors.charge(scene, killer, true);
        scene.idle(15);
        Actors.charge(scene, killer, false);
        Actors.swing(scene, killer);
        Actors.fall(scene, victim);
        scene.overlay().showText(80)
                .text("趁黑动手，好人很难看清是谁干的")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        scene.world().modifyBlocks(lamps, state -> lamp(state, true), false);
        StageLights.lights(scene, true);
        scene.overlay().showText(90)
                .text("时间一到灯会自动恢复（工程师能提前修好）；杀手一般共用 5 分钟冷却")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showText(70)
                .text("停电期间按开关也没用：灯没电")
                .independent();
        scene.idle(80);
        scene.markAsFinished();
    }

    /** A Wathe lamp powered or cut, as WorldBlackoutComponent sets lit and active. 像 WorldBlackoutComponent 那样设置灯的 lit 与 active。 */
    private static BlockState lamp(BlockState state, boolean on) {
        if (state.contains(Properties.LIT)) {
            state = state.with(Properties.LIT, on);
        }
        return state.contains(WatheProperties.ACTIVE) ? state.with(WatheProperties.ACTIVE, on) : state;
    }

    /**
     * Psycho mode (PlayerPsychoComponent): bought in the shop, by default 30 s with the bat locked in hand and the
     * psycho skin and garbled name everyone can see; a public psycho plays music for the whole train (the Silencer's
     * is silent). A fully charged left-click kills; by default one shield absorbs the first ordinary lethal hit (only
     * the psycho hears it). The shop entry cools down 5 min from the purchase.
     * 疯魔模式：在商店购买，默认持续 30 秒，球棒锁在手上，所有人都能看到疯魔皮肤和乱码名字；公开的疯魔会让全车响起
     * 音乐（静语者的没有）。蓄满力的左键一击必杀；默认 1 层护盾挡下第一次普通致命攻击（只有疯魔自己听得到）。
     * 商店购买后冷却 5 分钟。
     */
    private static void psychoMode(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("wathe_psycho_mode", "疯魔模式：30 秒的球棒狂徒");
        WatheItemScenes.stage(scene);
        Text killerName = Text.literal("杀手");
        ElementLink<ActorElement> victim = Actors.enter(scene, RoleColors.CIVILIAN, Text.literal("平民"),
                new Vec3d(3.4, 1, 3.6), SCREEN_RIGHT, Direction.DOWN);
        ElementLink<ActorElement> vigilante = Actors.enter(scene, RoleColors.VIGILANTE, Text.literal("义警"),
                new Vec3d(0.5, 1, 6.5), SCREEN_LEFT, Direction.DOWN);
        Actors.hold(scene, vigilante, stack("wathe:revolver"));
        ElementLink<ActorElement> killer = Actors.enter(scene, RoleColors.KILLER, killerName,
                new Vec3d(5.8, 1, 1.2), SCREEN_RIGHT, Direction.DOWN);
        scene.idle(15);
        scene.overlay().showControls(new Vec3d(5.8, 3.6, 1.2), Pointing.DOWN, 60)
                .withItem(stack("wathe:psycho_mode"));
        scene.overlay().showText(80)
                .text("杀手在商店买下【疯魔模式】：立刻生效，默认持续 30 秒")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        Actors.psycho(scene, killer, true);
        Actors.hold(scene, killer, stack("wathe:bat"));
        Actors.retint(scene, killer, RoleColors.KILLER,
                Text.literal("urscrewed").formatted(Formatting.OBFUSCATED, Formatting.DARK_RED));
        scene.overlay().showText(90)
                .text("换上疯魔皮肤，名字变成乱码；手里一般只能拿球棒，换不了别的")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showText(60)
                .text("全车一般都会响起疯魔音乐（静语者的疯魔没有）")
                .independent();
        scene.idle(70);
        Actors.walk(scene, killer, new Vec3d(-1.6, 0, 1.6), 25);
        scene.idle(28);
        scene.overlay().showControls(new Vec3d(4.2, 3.6, 2.8), Pointing.DOWN, 30).leftClick()
                .withItem(stack("wathe:bat"));
        scene.overlay().showText(80)
                .text("球棒蓄满力（约 1 秒）后左键，一击必杀")
                .independent()
                .attachKeyFrame();
        scene.idle(10);
        Actors.swing(scene, killer);
        Actors.fall(scene, victim);
        scene.idle(80);
        WatheItemScenes.shoot(scene, vigilante, new Vec3d(1.1, 2.05, 5.9), new Vec3d(4.2, 1.9, 2.8));
        scene.overlay().showText(90)
                .text("疯魔默认带 1 层护盾，一般能挡下第一次致命攻击（只有疯魔自己听到盔甲声）")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showText(60)
                .text("护盾没了，再挨一下就会死")
                .independent();
        scene.idle(70);
        scene.overlay().showText(70)
                .text("好人听到疯魔音乐，赶紧躲进房间把门关好")
                .independent();
        scene.idle(80);
        Actors.psycho(scene, killer, false);
        Actors.hold(scene, killer, ItemStack.EMPTY);
        Actors.retint(scene, killer, RoleColors.KILLER, killerName);
        scene.overlay().showText(80)
                .text("疯魔结束（默认 30 秒）后球棒自动收走；买下后 5 分钟内不能再买")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        scene.markAsFinished();
    }
}
