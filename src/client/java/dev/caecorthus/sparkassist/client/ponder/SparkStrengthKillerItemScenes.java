package dev.caecorthus.sparkassist.client.ponder;

import static dev.caecorthus.sparkassist.client.ponder.SparkPonderDemos.item;
import static dev.caecorthus.sparkassist.client.ponder.SparkPonderDemos.scene;
import static dev.caecorthus.sparkassist.client.ponder.WatheItemScenes.NORTH;
import static dev.caecorthus.sparkassist.client.ponder.WatheItemScenes.SCREEN_LEFT;
import static dev.caecorthus.sparkassist.client.ponder.WatheItemScenes.SCREEN_RIGHT;
import static dev.caecorthus.sparkassist.client.ponder.WatheItemScenes.WEST;
import static dev.caecorthus.sparkassist.client.ponder.WatheItemScenes.stack;

import dev.doctor4t.wathe.index.WatheParticles;
import java.util.List;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Demos for SparkStrength's killer and neutral items (Morphling, Bomber, Vulture, Pathogen, the Serial Killer's
 * psycho pistols, the M67), opened by holding W over the item and from the role's guide page; each needs NoellesRoles
 * and SparkStrength. Effects the server plays (a blast, a sneeze, a revive) or only some players see (a mark, a
 * carrier's outline, the trap's smoke) are scripted here and the captions say who sees what; times the game takes
 * longer over are shortened and the captions say so.
 * SparkStrength 杀手与中立阵营物品的演示（变形者、炸弹客、秃鹫、病原体、连环杀手的疯魔手枪、M67），在物品上按住 W
 * 或从职业指南页打开；都需要 NoellesRoles 与 SparkStrength。服务端播放的效果（爆炸、喷嚏、复活）或只有部分人看得到的
 * 效果（标记、带毒者轮廓、陷阱冒烟）由这里用脚本补上，并在字幕里说明谁能看到；游戏里耗时更长的过程做了缩短，字幕会注明。
 */
final class SparkStrengthKillerItemScenes {
    private static final List<String> NOELLES_STRENGTH = List.of("noellesroles", "sparkstrength");
    /** PathogenRules.CARRIER_COLOR_FOR_PATHOGEN: a carrier as the Pathogen sees it. 病原体眼中带毒者的颜色。 */
    private static final int CARRIER_DARK_GREEN = 0x1E8C1E;
    /** PathogenRules.TEAMMATE_COLOR: a fellow Pathogen, through walls. 病原体同伴（隔墙可见）的颜色。 */
    private static final int PATHOGEN_TEAMMATE = 0x00E5A0;

    private SparkStrengthKillerItemScenes() {
    }

    static void register() {
        item("sparkstrength:morph_reagent", NOELLES_STRENGTH,
                scene("wathe/aisle", SparkStrengthKillerItemScenes::morphReagent));
        item("sparkstrength:morph_device", NOELLES_STRENGTH,
                scene("wathe/aisle", SparkStrengthKillerItemScenes::morphReagent));
        item("sparkstrength:m67", NOELLES_STRENGTH,
                scene("noellesroles/wide_aisle", SparkStrengthKillerItemScenes::m67));
        item("sparkstrength:bomb_drone", NOELLES_STRENGTH,
                scene("noellesroles/wide_aisle", SparkStrengthKillerItemScenes::bombDrone));
        item("sparkstrength:skateboard", NOELLES_STRENGTH,
                scene("noellesroles/wide_aisle", SparkStrengthKillerItemScenes::skateboard));
        item("sparkstrength:virus", NOELLES_STRENGTH,
                scene("wathe/aisle", SparkStrengthKillerItemScenes::virus));
        item("sparkstrength:t_virus", NOELLES_STRENGTH,
                scene("wathe/aisle", SparkStrengthKillerItemScenes::tVirus));
        item("sparkstrength:serial_pistol", NOELLES_STRENGTH,
                scene("wathe/aisle", SparkStrengthKillerItemScenes::serialPistols));
        item("sparkstrength:serial_left_pistol", NOELLES_STRENGTH,
                scene("wathe/aisle", SparkStrengthKillerItemScenes::serialPistols));
        // timedBombTrap and serialPistols also join the noellesroles:timed_bomb and wathe:psycho_mode demos
        // (NoellesKillerScenes / WatheShopScenes.register). 两者也分别加入定时炸弹与疯魔模式的演示。
    }

    /**
     * Morph reagent and remote (SparkStrength MorphReagentItem, MorphDeviceItem, MorphlingService, MorphlingRules,
     * MorphMarkPlayerComponent, MorphlingClientHooks, MorphlingAppearanceClientHelper, HiddenEquipmentHelperMixin):
     * the Morphling's shop sells the reagent for 10; the remote is a starting item. The reagent's first right-click
     * samples the living player or body in the crosshair (vanilla's ~3-block reach; a 2.5-block raycast when no entity
     * is targeted; oneself if none) and does not swing; after letting go, the second marks the living player in the
     * crosshair the same way (oneself if none; not the sample) and uses the reagent up. Marks show only on the
     * Morphling's screen, outlined in its colour unless instinct is held. The remote's right-click starts every mark of
     * this Morphling at once and is not used up: for 60 s each marked player usually looks like the sample to others
     * (skin and the name under the crosshair; not to a converted Pathogen or during the Jester Moment), the disguised
     * player's voice chat is muted and a living sample's voice also plays from them, and a body left meanwhile looks
     * like the sample too (killers holding instinct and the Coroner's body info show the real one). Each kill or death
     * of a marked player during it pays the living Morphling 50; an innocent shot while disguised usually costs the
     * shooter no punishment. Both items are hidden in hand from other living players.
     * 变形试剂与变形遥控器：变形者商店卖试剂，10 金币；遥控器开局自带。试剂第一次右键对准准星里的活人或尸体采样（原版约 3 格
     * 交互距离；没对准实体时按 2.5 格射线找；都没有就采样自己），不挥手；松开后第二次右键同样对准活人标记（没对准就标记自己；
     * 不能是采样对象），试剂随即用掉。标记只在变形者屏幕上以其颜色显示轮廓（按住本能键时除外）。右键遥控器同时触发自己的所有
     * 标记，遥控器不会用掉：60 秒内，被标记者在其他人眼里一般是采样对象的样子（皮肤和准星下的名字；转化病原体眼中与小丑时刻
     * 除外），语音被静音、活着的采样对象说话时声音也从他身上传出，期间留下的尸体也是伪装后的样子（杀手按住本能、验尸官查看
     * 尸体时能看到原样）。被标记者在此期间每杀一人或死亡，活着的变形者得 50 金币；伪装中的好人中枪，开枪的人一般不受误杀
     * 惩罚。两件物品拿在手里时其他活人看不见。
     */
    private static void morphReagent(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("sparkstrength_morph_reagent", "变形试剂：把别人变成另一个人");
        WatheItemScenes.stage(scene);
        int color = RoleColors.of("noellesroles:morphling", 0xAA023D);
        Text morphlingName = Text.literal("变形者");
        ItemStack reagent = stack("sparkstrength:morph_reagent");
        ItemStack remote = stack("sparkstrength:morph_device");
        Text ming = Text.literal("阿明");
        Text jie = Text.literal("小杰");
        Vec3d mingSpot = new Vec3d(1.2, 1, 4.4);
        Vec3d jieSpot = new Vec3d(4.4, 1, 6.2);
        ElementLink<ActorElement> real = Actors.enter(scene, RoleColors.CIVILIAN, ming, mingSpot, -45,
                Direction.DOWN);
        ElementLink<ActorElement> marked = Actors.enter(scene, RoleColors.VIGILANTE, jie, jieSpot, NORTH,
                Direction.DOWN);
        scene.idle(10);
        Vec3d start = new Vec3d(6.2, 1, 0.8);
        ElementLink<ActorElement> morphling = Actors.enter(scene, color, morphlingName, start, SCREEN_RIGHT,
                Direction.DOWN);
        Actors.hold(scene, morphling, reagent);
        scene.idle(15);
        scene.overlay().showText(90)
                .text("变形者可在商店买【变形试剂】（默认 10 金币），开局还自带一个【变形遥控器】")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showText(70)
                .text("这两样拿在手里时，其他活人一般看不见")
                .independent();
        Vec3d nearMing = new Vec3d(2.6, 1, 3.0);
        Actors.walk(scene, morphling, nearMing.subtract(start), 40);
        scene.idle(42);
        Actors.turn(scene, morphling, facing(nearMing, mingSpot));
        scene.idle(38);
        scene.overlay().showControls(nearMing.add(0, 2.6, 0), Pointing.DOWN, 30).rightClick().withItem(reagent);
        scene.idle(10);
        scene.overlay().showText(90)
                .text("第一次右键：对准身边（约 3 格内）的活人或尸体采样（没对准人就采你自己）")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        Vec3d nearJie = new Vec3d(5.6, 1, 5.0);
        Actors.walk(scene, morphling, nearJie.subtract(nearMing), 36);
        scene.idle(38);
        Actors.turn(scene, morphling, facing(nearJie, jieSpot));
        scene.idle(8);
        scene.overlay().showControls(nearJie.add(0, 2.6, 0), Pointing.DOWN, 30).rightClick().withItem(reagent);
        scene.idle(10);
        // With a sample stored the client's use swings the arm; the mark uses the reagent up.
        // 已采样时客户端使用会挥手；标记后试剂用掉。
        Actors.swing(scene, morphling);
        Actors.hold(scene, morphling, ItemStack.EMPTY);
        // The mark's outline lasts until the disguise ends. 标记轮廓持续到伪装结束。
        Actors.highlight(scene, marked, color, 800);
        scene.overlay().showText(90)
                .text("松开后再右键：对准身边另一个活人标记（没对准人就标记你自己；不能是采样对象），试剂随即用掉")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showText(80)
                .text("被你标记的人会带着你颜色的轮廓，直到伪装结束（只在你屏幕上；按住本能键时显示普通本能颜色）")
                .independent();
        Vec3d hideout = new Vec3d(6.2, 1, 1.4);
        Actors.walk(scene, morphling, hideout.subtract(nearJie), 36);
        scene.idle(38);
        Actors.turn(scene, morphling, SCREEN_RIGHT);
        Actors.hold(scene, morphling, remote);
        scene.idle(52);
        scene.overlay().showControls(hideout.add(0, 2.6, 0), Pointing.DOWN, 30).rightClick().withItem(remote);
        scene.idle(10);
        Actors.swing(scene, morphling);
        Actors.retint(scene, marked, RoleColors.CIVILIAN, ming);
        scene.overlay().showText(90)
                .text("在哪都行，右键遥控器：你标记的人同时开始伪装，持续 60 秒")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        Actors.walk(scene, marked, new Vec3d(-1.2, 0, -0.8), 24);
        scene.overlay().showText(90)
                .text("其他人眼里，小杰现在一般就是“阿明”：样子和准星下的名字都变了")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showText(90)
                .text("开着语音时：伪装期间他说不出话；阿明若还活着，说话声也会从他身上传出来")
                .independent();
        scene.idle(100);
        scene.overlay().showText(90)
                .colored(PonderPalette.GREEN)
                .text("伪装期间他每杀一个人、或者他死了，只要你还活着，都能得 50 金币")
                .independent();
        scene.idle(100);
        scene.overlay().showText(90)
                .text("伪装中的好人中枪时，开枪的人一般不受误杀惩罚")
                .independent();
        scene.idle(100);
        scene.overlay().showText(90)
                .text("死在伪装期间，尸体一般也是伪装后的样子（杀手按住本能键能看到原样，验尸官查看尸体也能看到真名）")
                .independent();
        scene.idle(100);
        Actors.retint(scene, marked, RoleColors.VIGILANTE, jie);
        scene.overlay().showText(80)
                .text("60 秒后（演示缩短了时间）变回原样；遥控器不会用掉")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        scene.markAsFinished();
    }

    /**
     * M67 (SparkStrength M67Item, M67UseService, M67Rules, M67Physics, M67GrenadeEntity, GrenadeBlastService,
     * M67ShopService, M67Client and its outline mixins): every shop that sells Wathe's grenade also sells it, 25 coins
     * or 10 for a Bomber; in a match it is locked for the first 90 s. Holding right-click (the bow pose) pulls the pin;
     * after 20 charge ticks letting go throws it, earlier is a cancel with a 2 s cooldown (SparkTraits can shorten
     * both). Thrown hard (2.25 blocks a tick: level, it first lands about 13 blocks out), it bounces off floors and walls
     * and rolls; the scene aims 77 degrees down and follows M67Physics tick by tick. 5 s after the throw it blows up
     * (one big explosion, 100 smoke and 100 fragments) and
     * usually kills every living player within 5 blocks by path (walls and closed doors shield), the thrower included,
     * breaking drones too. Each viewer sees the grenade outlined red within 5 blocks of it and yellow within 7, by
     * distance only. 10 s cooldown after a throw. Not hidden in hand.
     * M67 手雷：凡是卖 Wathe 手雷的商店都会多卖它，25 金币，炸弹客 10 金币；对局中开局 90 秒内不能用。按住右键（拉弓姿势）
     * 拉环，蓄力 20 tick 后松手扔出，更早松手算取消，冷却 2 秒（SparkTraits 可缩短两者）。出手很猛（每 tick 2.25 格，平着扔
     * 约 13 格外才第一次落地），会在地面和墙上弹跳、滚动；演示朝下 77 度扔，逐 tick 按 M67Physics 走位。扔出 5 秒后爆炸
     * （1 个大爆炸、100 个烟雾和 100 个碎片），沿路径一般炸死 5 格内所有活人（墙和关着的门能挡），扔的人也不例外，范围内的
     * 无人机同样被炸坏。每名观察者离它 5 格内看到红色描边、7 格内黄色，只按距离判断。投掷后冷却 10 秒。拿在手里别人看得见。
     */
    private static void m67(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("sparkstrength_m67", "M67 手雷：拉环、扔出、5 秒后爆炸");
        NoellesKillerScenes.wideStage(scene);
        Text civilian = Text.literal("平民");
        ItemStack m67 = stack("sparkstrength:m67");
        Vec3d staysSpot = new Vec3d(2.6, 1, 6.9);
        Vec3d runsSpot = new Vec3d(3.8, 1, 7.2);
        ElementLink<ActorElement> stays = Actors.enter(scene, RoleColors.CIVILIAN, civilian, staysSpot,
                facing(staysSpot, runsSpot), Direction.DOWN);
        ElementLink<ActorElement> runs = Actors.enter(scene, RoleColors.CIVILIAN, civilian, runsSpot,
                facing(runsSpot, staysSpot), Direction.DOWN);
        scene.idle(10);
        Vec3d killerSpot = new Vec3d(8.0, 1, 1.2);
        ElementLink<ActorElement> killer = Actors.enter(scene, RoleColors.KILLER, Text.literal("杀手"), killerSpot,
                SCREEN_RIGHT, Direction.DOWN);
        Actors.hold(scene, killer, m67);
        scene.idle(15);
        scene.overlay().showText(90)
                .text("能买【手雷】的商店都会多卖【M67 手雷】：一般 25 金币，炸弹客 10 金币")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showText(70)
                .text("开局 90 秒后才能扔；拿在手里别人看得见")
                .independent();
        scene.idle(80);
        scene.overlay().showControls(killerSpot.add(0, 2.6, 0), Pointing.DOWN, 40).rightClick().withItem(m67);
        // Aimed steeply down: thrown level it would first land about 13 blocks out (M67Physics, 2.25 a tick).
        // 朝脚下猛扔：平着扔要飞出约 13 格才第一次落地（M67Physics，每 tick 2.25 格）。
        Actors.lookPitch(scene, killer, 77);
        Actors.charge(scene, killer, true);
        scene.overlay().showText(58)
                .text("按住右键拉环，一般约 1 秒拉满再松手；没拉满就松手算取消，冷却约 2 秒")
                .independent()
                .attachKeyFrame();
        scene.idle(40);
        Actors.charge(scene, killer, false);
        Actors.hold(scene, killer, ItemStack.EMPTY);
        // M67Physics at pitch 77, tick by tick: it hits the floor half a block out, bounces high (a quarter of the
        // speed back up), lands about 5.2 blocks out, hops once and rolls to rest about 6.4 blocks out at tick 28.
        // 按 M67Physics 以 77 度俯角逐 tick 推算：半格外砸到地面，高高弹起（反弹四分之一速度），约 5.2 格外落地，
        // 再小跳一下，第 28 tick 滚到约 6.4 格外停住。
        Vec3d floorHit = along(killerSpot, SCREEN_RIGHT, 0.51);
        Vec3d landing = along(killerSpot, SCREEN_RIGHT, 5.16);
        Vec3d hop = along(killerSpot, SCREEN_RIGHT, 5.8);
        Vec3d rest = along(killerSpot, SCREEN_RIGHT, 6.41);
        Actors.toss(scene, NoellesKillerScenes.armedM67(), killerSpot.add(0, 1.52, 0), floorHit, 1, 0, false,
                ThrownElement.Flight.UPRIGHT);
        scene.idle(1);
        Actors.toss(scene, NoellesKillerScenes.armedM67(), floorHit, landing, 14, 1.74, false,
                ThrownElement.Flight.UPRIGHT);
        scene.idle(14);
        Actors.toss(scene, NoellesKillerScenes.armedM67(), landing, hop, 3, 0.06, false,
                ThrownElement.Flight.UPRIGHT);
        scene.idle(3);
        ElementLink<ThrownElement> grenade = Actors.toss(scene, NoellesKillerScenes.armedM67(), hop, rest, 10, 0,
                true, ThrownElement.Flight.UPRIGHT);
        scene.idle(10);
        // The outline lasts the rest of the fuse. 描边持续到引信结束。
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, grenade,
                new Box(rest.x - 0.2, rest.y, rest.z - 0.2, rest.x + 0.2, rest.y + 0.3, rest.z + 0.2), 72);
        scene.overlay().showText(80)
                .text("出手很猛：落地后还会弹起、滚动，碰到墙也会弹开；扔出 5 秒后爆炸")
                .independent()
                .attachKeyFrame();
        scene.idle(20);
        Actors.lookPitch(scene, killer, 0);
        Actors.walk(scene, runs, new Vec3d(4.8, 0, 1.2), 24);
        scene.idle(10);
        scene.overlay().showText(80)
                .text("每个人按自己离它的距离看到描边：5 格内红色，7 格内黄色；不管掩体（盲人看不到）")
                .independent(40)
                .attachKeyFrame();
        scene.idle(42);
        // The M67 went off 100 ticks after the throw. M67 在扔出 100 tick 后爆炸。
        Actors.remove(scene, grenade);
        Vec3d burst = rest.add(0, 0.1, 0);
        Effects.burst(scene, WatheParticles.BIG_EXPLOSION, burst, 1, 0);
        NoellesKillerScenes.spray(scene, ParticleTypes.SMOKE, burst, 100, 0, 0.2);
        NoellesKillerScenes.spray(scene, new ItemStackParticleEffect(ParticleTypes.ITEM,
                NoellesKillerScenes.armedM67()), burst, 100, 0, 1.0);
        Actors.fall(scene, stays);
        NoellesKillerScenes.blastRing(scene, rest, 5, 9, 160);
        scene.idle(30);
        scene.overlay().showText(100)
                .colored(PonderPalette.RED)
                .text("红圈内（5 格）的人一般被炸死；爆炸沿路径算，墙和关着的门能挡")
                .independent()
                .attachKeyFrame();
        scene.idle(110);
        scene.overlay().showText(90)
                .text("扔的人自己离得太近一般也会被炸死，范围内的无人机也会被炸坏；扔出后一般冷却 10 秒")
                .independent();
        scene.idle(100);
        scene.markAsFinished();
    }

    /**
     * Bomb drone (SparkStrength DroneItem, DroneEntity, DroneCombatService, DroneRules, DroneShopService,
     * HiddenEquipmentHelperMixin): only the real Bomber's shop sells it, 400 by default; single use, and each Bomber may
     * have one undetonated bomb drone at a time. Placed and piloted with the tablet like the grenade drone (90 s
     * opening lock), it flies faster (0.7 blocks a tick) and drains faster (5% a second flying, 2% hovering). The
     * pilot's left click detonates it: one big explosion, 100 smoke and 100 fragments, and every living player within
     * 4 blocks by path of the hull's centre (about 3.8 at foot level here) usually dies (the Bomber too), credited to
     * the Bomber; drones in the blast break. Broken by a weapon or
     * run flat, it detonates in place; if its owner dies it fizzles with a puff of smoke and no blast. The held drone is
     * hidden from other living players; the flying one is seen by all.
     * 炸弹无人机：只有真正的炸弹客商店有卖，默认 400 金币；一次性，每名炸弹客场上同时只能有一架没炸掉的。放置和用平板操控与
     * 投弹无人机相同（开局 90 秒锁），飞得更快（每 tick 0.7 格），耗电也更快（飞行每秒 5%，悬停 2%）。驾驶者左键引爆：1 个
     * 大爆炸、100 个烟雾和 100 个碎片，以机身中心沿路径 4 格内（此处脚的高度约 3.8 格）的活人一般都会死亡（炸弹客自己也算），
     * 算炸弹客的击杀；范围内的无人机被炸坏。
     * 被武器打坏或电量耗尽时原地爆炸；主人死亡时冒一阵烟失效，不会爆炸。手里的无人机对其他活人隐藏；飞着的人人可见。
     */
    private static void bombDrone(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("sparkstrength_bomb_drone", "炸弹无人机：飞过去自爆");
        NoellesKillerScenes.wideStage(scene);
        int color = RoleColors.of("noellesroles:bomber", 0x323232);
        Text civilian = Text.literal("平民");
        ItemStack bombDrone = stack("sparkstrength:bomb_drone");
        ElementLink<ActorElement> first = Actors.enter(scene, RoleColors.CIVILIAN, civilian,
                new Vec3d(2.6, 1, 7.2), SCREEN_RIGHT, Direction.DOWN);
        ElementLink<ActorElement> second = Actors.enter(scene, RoleColors.CIVILIAN, civilian,
                new Vec3d(4.2, 1, 7.0), WEST, Direction.DOWN);
        ElementLink<ActorElement> third = Actors.enter(scene, RoleColors.CIVILIAN, civilian,
                new Vec3d(1.6, 1, 6.4), SCREEN_LEFT, Direction.DOWN);
        Actors.enter(scene, RoleColors.CIVILIAN, civilian, new Vec3d(8.2, 1, 7.6), WEST, Direction.DOWN);
        scene.idle(10);
        Vec3d bomberSpot = new Vec3d(8.0, 1, 1.2);
        ElementLink<ActorElement> bomber = Actors.enter(scene, color, Text.literal("炸弹客"), bomberSpot,
                SCREEN_RIGHT, Direction.DOWN);
        Actors.hold(scene, bomber, bombDrone);
        scene.idle(15);
        scene.overlay().showText(90)
                .text("炸弹客可在商店买【炸弹无人机】（一般 400 金币，善良炸弹客 200），开局 90 秒后才能放下和操控")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        Actors.lookPitch(scene, bomber, 55);
        scene.overlay().showControls(new Vec3d(7.3, 1.3, 1.9), Pointing.DOWN, 30).rightClick().withItem(bombDrone);
        scene.idle(10);
        Actors.swing(scene, bomber);
        Actors.hold(scene, bomber, ItemStack.EMPTY);
        ElementLink<EntityElement> drone = NoellesKillerScenes.placeDrone(scene, "bomb_drone",
                new Vec3d(7.3, 1, 1.9), SCREEN_RIGHT);
        scene.idle(15);
        Actors.lookPitch(scene, bomber, 0);
        scene.overlay().showText(90)
                .text("右键地面放下（拿在手里时其他活人看不见）；一次性，场上同时只能有一架没炸掉的")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        Actors.hold(scene, bomber, stack("sparkstrength:tablet"));
        scene.overlay().showText(90)
                .text("和投弹无人机一样：主手拿【平板电脑】连接，以无人机视角操控（平板拿在手里其他活人看不见）")
                .independent()
                .attachKeyFrame();
        scene.idle(30);
        NoellesKillerScenes.flyDrone(scene, drone, new Vec3d(0, 1.0, 0), 3);
        scene.idle(70);
        scene.overlay().showText(90)
                .text("它横向飞得更快，耗电也更快：飞行每秒 5%%，悬停每秒 2%%")
                .independent()
                .attachKeyFrame();
        NoellesKillerScenes.flyDrone(scene, drone, new Vec3d(-4.3, 0, 4.3), 9);
        scene.idle(100);
        Vec3d hover = new Vec3d(3.0, 2.0, 6.2);
        scene.overlay().showControls(hover.add(0, 1.2, 0), Pointing.DOWN, 20).leftClick();
        scene.idle(10);
        scene.world().modifyEntity(drone, Entity::discard);
        Vec3d burst = hover.add(0, 0.15, 0);
        Effects.burst(scene, WatheParticles.BIG_EXPLOSION, burst, 1, 0);
        NoellesKillerScenes.spray(scene, ParticleTypes.SMOKE, burst, 100, 0, 0.2);
        NoellesKillerScenes.spray(scene, new ItemStackParticleEffect(ParticleTypes.ITEM, bombDrone), burst, 100, 0,
                1.0);
        Actors.fall(scene, first);
        Actors.fall(scene, second);
        Actors.fall(scene, third);
        // A 4-block sphere around the hull's centre, 1.15 above the floor: about 3.8 at foot level.
        // 以机身中心（离地 1.15 格）为球心的 4 格球体：在脚的高度约 3.8 格。
        NoellesKillerScenes.blastRing(scene, new Vec3d(hover.x, 1, hover.z), 3.8, 9, 160);
        scene.idle(30);
        scene.overlay().showText(100)
                .colored(PonderPalette.RED)
                .text("左键引爆：以无人机为中心约 4 格内（红圈）的人一般都被炸死；墙和关着的门能挡")
                .independent()
                .attachKeyFrame();
        scene.idle(110);
        scene.overlay().showText(90)
                .text("被打坏或电量耗尽时也会立刻爆炸；你的本体在 4 格内一般也会被炸死")
                .independent();
        scene.idle(100);
        scene.overlay().showText(80)
                .text("你死亡或掉线后，无人机一般冒一阵烟就失效，不会爆炸")
                .independent();
        scene.idle(90);
        scene.markAsFinished();
    }

    /**
     * Skateboard (SparkStrength SkateboardItem, VultureSkateboardService, VultureSkateboardRules,
     * SkateboardRideComponent, SkateboardRenderer, SkateboardLiftMixin, SkateboardLimbsMixin, SkateboardStepSoundMixin,
     * HiddenEquipmentHelperMixin): every real Vulture gets one at round start (if a hotbar slot is free); it is hidden
     * in hand. Right-click rides it for 10 s at three times the walk and sprint speed; it is not used up. Everyone sees
     * the board under the rider's feet and the rider standing on its deck, legs still; footsteps are muted and a rolling
     * sound plays. 60 s opening lock; the item cooldown covers the ride and 10 s after it. A stun (the Engineer's
     * capture device) or a Taotie swallow ends the ride at once.
     * 滑板：真正的秃鹫开局都会拿到一块（快捷栏有空位时），拿在手里别人看不见。右键上板滑行 10 秒，走路和疾跑都变成 3 倍速；
     * 不会用掉。所有人都能看到骑手脚下的滑板，骑手站在板面上、双腿不动；脚步声被屏蔽，改为滚轮声。开局 60 秒内不能用；物品
     * 冷却覆盖滑行和之后的 10 秒。被定身（工程师的捕捉装置）或被饕餮吞下时立刻结束。
     */
    private static void skateboard(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("sparkstrength_skateboard", "滑板：秃鹫的三倍速");
        NoellesKillerScenes.wideStage(scene);
        int color = RoleColors.of("noellesroles:vulture", 0xB56700);
        ItemStack board = stack("sparkstrength:skateboard");
        Vec3d vultureSpot = new Vec3d(7.0, 1, 1.0);
        ElementLink<ActorElement> walker = Actors.enter(scene, RoleColors.CIVILIAN, Text.literal("平民"),
                new Vec3d(8.5, 1, 2.5), SCREEN_RIGHT, Direction.DOWN);
        ElementLink<ActorElement> vulture = Actors.enter(scene, color, Text.literal("秃鹫"), vultureSpot,
                SCREEN_RIGHT, Direction.DOWN);
        Actors.hold(scene, vulture, board);
        scene.idle(15);
        scene.overlay().showText(90)
                .text("秃鹫开局自带一块【滑板】，拿在手里别人一般看不见；开局 60 秒后才能用")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showControls(vultureSpot.add(0, 2.6, 0), Pointing.DOWN, 30).rightClick().withItem(board);
        scene.idle(10);
        Actors.swing(scene, vulture);
        Actors.hold(scene, vulture, ItemStack.EMPTY);
        Actors.ride(scene, vulture, board);
        scene.overlay().showText(80)
                .text("右键踩上滑板：10 秒内移动速度变成 3 倍（走路和疾跑都算）")
                .independent()
                .attachKeyFrame();
        scene.idle(30);
        // Same 30 ticks, three times the distance. 同样 30 tick，距离是三倍。
        Actors.walk(scene, walker, new Vec3d(-5 / 3.0, 0, 5 / 3.0), 30);
        Actors.walk(scene, vulture, new Vec3d(-5.0, 0, 5.0), 30);
        scene.idle(60);
        scene.overlay().showText(90)
                .text("脚下的滑板一般人人都看得见；滑行时没有脚步声，移动时只有轮子滚动的声音")
                .independent()
                .attachKeyFrame();
        // The ride ends 200 ticks (10 s) after getting on. 上板 200 tick（10 秒）后结束。
        scene.idle(110);
        Actors.ride(scene, vulture, ItemStack.EMPTY);
        scene.overlay().showText(90)
                .text("10 秒后自动下板，中途不能自己下来；一般从上板算起 20 秒后才能再用，滑板不会用掉")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showText(90)
                .text("被定身（比如工程师的捕捉装置）、被饕餮吞下或死亡时会立刻停下")
                .independent();
        scene.idle(100);
        scene.markAsFinished();
    }

    /**
     * Virus (SparkStrength VirusItem, VirusService, PathogenRules, VirusCarrierComponent, PathogenShopService,
     * HiddenEquipmentHelperMixin; NoellesRoles InfectedPlayerComponent): the Pathogen's shop sells it for 50, it stacks
     * and is hidden in hand. Right-clicking a living player within 3 blocks in sight makes them infected and a carrier
     * and they sneeze at once (the panda sneeze, volume 2); Pathogens and carriers are refused and the virus is kept.
     * The Pathogen sees a carrier dark green while in sight. Every 30 s a carrier infects the nearest uninfected
     * non-Pathogen within 3 blocks in sight and sneezes again; those it infects do not spread it. A Toxicologist sees
     * carriers in sight and the antidote cures them.
     * 病毒：病原体商店卖，50 金币，能叠加，拿在手里别人看不见。右键 3 格内视线可及的活人：对方被感染并成为带毒者，立刻打个
     * 喷嚏（熊猫喷嚏声，音量 2）；对病原体和已带毒的人无效，病毒保留。病原体在视线内看到带毒者的深绿色轮廓。带毒者每 30 秒
     * 传染一次 3 格内、视线可及、离他最近的未感染非病原体，并再打一个喷嚏；被他传染的人不会再往外传。毒理学家能看到视线内的
     * 带毒者，解毒剂能把他治好。
     */
    private static void virus(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("sparkstrength_virus", "病毒：让别人替你传播");
        WatheItemScenes.stage(scene);
        int color = RoleColors.of("noellesroles:pathogen", 0x7FFF00);
        ItemStack virus = stack("sparkstrength:virus");
        Text civilian = Text.literal("平民");
        Vec3d carrierSpot = new Vec3d(2.8, 1, 3.4);
        ElementLink<ActorElement> carrier = Actors.enter(scene, RoleColors.CIVILIAN, civilian, carrierSpot,
                SCREEN_LEFT, Direction.DOWN);
        Vec3d bystanderStart = new Vec3d(1.0, 1, 6.0);
        ElementLink<ActorElement> bystander = Actors.enter(scene, RoleColors.CIVILIAN, civilian, bystanderStart,
                SCREEN_LEFT, Direction.DOWN);
        scene.idle(10);
        Vec3d start = new Vec3d(6.2, 1, 0.8);
        ElementLink<ActorElement> pathogen = Actors.enter(scene, color, Text.literal("病原体"), start, SCREEN_RIGHT,
                Direction.DOWN);
        Actors.hold(scene, pathogen, virus);
        scene.idle(15);
        scene.overlay().showText(90)
                .text("病原体可在商店买【病毒】（默认 50 金币，能叠加），拿在手里别人一般看不见")
                .independent()
                .attachKeyFrame();
        Vec3d near = new Vec3d(4.2, 1, 2.2);
        Actors.walk(scene, pathogen, near.subtract(start), 26);
        scene.idle(28);
        Actors.turn(scene, pathogen, facing(near, carrierSpot));
        scene.idle(72);
        scene.overlay().showControls(near.add(0, 2.6, 0), Pointing.DOWN, 30).rightClick().withItem(virus);
        scene.idle(10);
        Actors.swing(scene, pathogen);
        Actors.hold(scene, pathogen, ItemStack.EMPTY);
        // The outline shows as long as the Pathogen has the carrier in sight. 只要病原体看得见带毒者，轮廓就一直在。
        Actors.highlight(scene, carrier, CARRIER_DARK_GREEN, 700);
        scene.overlay().showText(40)
                .text("阿嚏！")
                .pointAt(carrierSpot.add(0, 1.9, 0))
                .placeNearTarget();
        scene.overlay().showText(90)
                .text("右键 3 格内视线可及的活人：他一般会立刻打个喷嚏（附近都听得到），成为带毒者")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showText(90)
                .text("你一般能在视线内看到带毒者的深绿色轮廓（只在你屏幕上，隔墙不显示，方框为示意）")
                .independent();
        Vec3d back = new Vec3d(5.6, 1, 1.0);
        Actors.walk(scene, pathogen, back.subtract(near), 20);
        scene.idle(22);
        Actors.turn(scene, pathogen, facing(back, carrierSpot));
        scene.idle(78);
        scene.overlay().showText(80)
                .text("对病原体或已经带毒的人无效，这时病毒不会用掉")
                .independent();
        Vec3d closer = new Vec3d(1.8, 1, 5.0);
        Actors.walk(scene, bystander, closer.subtract(bystanderStart), 20);
        scene.idle(22);
        Actors.turn(scene, bystander, facing(closer, carrierSpot));
        Actors.turn(scene, carrier, facing(carrierSpot, closer));
        scene.idle(68);
        scene.overlay().showText(40)
                .text("阿嚏！")
                .pointAt(carrierSpot.add(0, 1.9, 0))
                .placeNearTarget();
        Actors.highlight(scene, bystander, color, 410);
        scene.overlay().showText(100)
                .text("带毒者每 30 秒传染一次：身边 3 格内、视线可及、最近的一名未感染者（演示缩短了时间）")
                .independent()
                .attachKeyFrame();
        scene.idle(110);
        scene.overlay().showText(90)
                .text("传染时他又打一个喷嚏；被传染的人在你眼里是亮绿色轮廓，只算感染，一般不会再往外传")
                .independent();
        scene.idle(100);
        scene.overlay().showText(90)
                .text("毒理学家也能看见视线内的带毒者，还能用【解毒剂】把他治好")
                .independent();
        scene.idle(100);
        scene.overlay().showText(90)
                .colored(PonderPalette.GREEN)
                .text("感染本身不伤人：其他活着的人全被感染时，病原体获胜")
                .independent();
        scene.idle(100);
        scene.markAsFinished();
    }

    /**
     * T-Virus (SparkStrength TVirusItem, PathogenReviveService, PathogenRules, PathogenShopService,
     * PathogenTeamService, PathogenGrayCrowdClient, HiddenEquipmentHelperMixin): only an original Pathogen's shop sells
     * it, 150, one per round; hidden in hand. Right-clicking the newest real body of a dead, spectating player (not a
     * fake or hidden body, not oneself) revives that player at the body's position (Wathe spawns bodies a block ahead
     * of the death spot) and yaw, as a converted
     * Pathogen: inventory cleared, balance 0, NoellesRoles' Pathogen kit (a neutral master key), a shop with only the
     * Virus; the body goes and a zombie villager's "converted" sound plays. A failed try keeps the item. A living
     * converted Pathogen cannot speak and sees every non-Pathogen as a gray Steve; Pathogens see each other through
     * walls at any distance and all win together.
     * T病毒：只有开局就是病原体的人商店有卖，150 金币，每局 1 支；拿在手里别人看不见。右键一名已死亡、处于旁观的玩家最新的
     * 真实尸体（不能是假尸体、被藏起的尸体或自己的），死者在尸体实体处（Wathe 把尸体生成在死亡位置前方一格）按其朝向复活为
     * 转化病原体：背包清空、金币归零，拿到 NoellesRoles
     * 给病原体的物品（中立万能钥匙），商店只卖病毒；尸体消失，响起僵尸村民的“转化”声。没成功时物品保留。活着的转化病原体不能
     * 说话，眼中除病原体以外的人都是灰色史蒂夫；病原体之间不论多远都能隔墙看到彼此，并且一起获胜。
     */
    private static void tVirus(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("sparkstrength_t_virus", "T病毒：把死人拉进你的队伍");
        WatheItemScenes.stage(scene);
        int color = RoleColors.of("noellesroles:pathogen", 0x7FFF00);
        ItemStack tVirus = stack("sparkstrength:t_virus");
        Vec3d bodySpot = new Vec3d(2.8, 1, 3.4);
        ElementLink<ActorElement> victim = Actors.enter(scene, RoleColors.CIVILIAN, Text.literal("平民"), bodySpot,
                SCREEN_RIGHT, Direction.DOWN);
        scene.idle(15);
        Actors.fall(scene, victim);
        scene.idle(25);
        Vec3d start = new Vec3d(6.2, 1, 0.8);
        ElementLink<ActorElement> pathogen = Actors.enter(scene, color, Text.literal("病原体"), start, SCREEN_RIGHT,
                Direction.DOWN);
        Actors.hold(scene, pathogen, tVirus);
        scene.idle(15);
        scene.overlay().showText(90)
                .text("【T病毒】：默认 150 金币，每局限购 1 支，拿在手里别人一般看不见；被 T病毒复活的病原体买不到")
                .independent()
                .attachKeyFrame();
        Vec3d near = new Vec3d(4.2, 1, 2.0);
        Actors.walk(scene, pathogen, near.subtract(start), 26);
        scene.idle(28);
        Actors.turn(scene, pathogen, SCREEN_RIGHT);
        scene.idle(10);
        Actors.lookPitch(scene, pathogen, 40);
        scene.idle(62);
        scene.overlay().showControls(new Vec3d(2.2, 2.0, 4.0), Pointing.DOWN, 30).rightClick().withItem(tVirus);
        scene.idle(10);
        Actors.swing(scene, pathogen);
        Actors.hold(scene, pathogen, ItemStack.EMPTY);
        Actors.vanish(scene, victim);
        // Wathe spawns a body a block ahead of where its player stood (and draws it a block back), and the revive
        // puts the player at the body's position and yaw. Wathe 把尸体生成在死者站位前方一格处（绘制时再后移一格），
        // 复活把玩家放到尸体的位置与朝向。
        ElementLink<ActorElement> revived = Actors.enter(scene, color, Text.literal("病原体（复活）"),
                along(bodySpot, SCREEN_RIGHT, 1), SCREEN_RIGHT, Direction.DOWN);
        scene.overlay().showText(100)
                .text("右键死者的尸体：他在尸体处、按尸体的朝向复活，成为你的病原体同伴（附近都听得到僵尸村民的转化声）")
                .independent()
                .attachKeyFrame();
        scene.idle(20);
        Actors.lookPitch(scene, pathogen, 0);
        // Pathogens see each other through walls at any distance. 病原体之间不论多远都能隔墙看到彼此。
        Actors.highlight(scene, revived, PATHOGEN_TEAMMATE, 490);
        scene.idle(90);
        scene.overlay().showText(90)
                .text("活着的病原体之间一般不论多远都能隔墙看到彼此（方框为示意）；病原体获胜时，所有在线的病原体一起算赢")
                .independent();
        scene.idle(100);
        scene.overlay().showText(90)
                .text("他的背包清空、金币归零，拿到中立万能钥匙和感染技能；他的商店只卖【病毒】")
                .independent()
                .attachKeyFrame();
        Actors.walk(scene, revived, new Vec3d(-1.2, 0, 1.2), 24);
        scene.idle(100);
        scene.overlay().showText(90)
                .text("复活的同伴活着时说不了话；在他眼里，除病原体以外的人都是灰色的史蒂夫")
                .independent();
        scene.idle(100);
        scene.overlay().showText(90)
                .text("每名死者只有最新的那具尸体有效；假尸体、被藏起来的尸体、冤魂等都不行；没复活成功不会用掉")
                .independent();
        scene.idle(100);
        scene.markAsFinished();
    }

    /**
     * Serial Killer's psycho pistols (SparkStrength SerialKillerPsychoMixin, SerialPistolItem,
     * SerialPistolShotService, SerialKillerConstants, SerialKillerClientUseMixin, SerialKillerBipedEntityModelMixin,
     * SerialPistolInventoryRules and the slot/drop guards; Wathe PlayerPsychoComponent): when a NoellesRoles Serial
     * Killer starts psycho mode (bought in the killer shop), there is no bat: the serial pistol goes into a free hotbar
     * slot and the left pistol into the off hand (refused if the off hand is full or no hotbar slot is free); the
     * psycho skin, scrambled name and shield are Wathe's as usual. A right-click shoots the player in the crosshair up
     * to 30 blocks, line of sight required, killing with the revolver's sound and flash; each pistol cools down 2 s on
     * its own, and while the main one cools a right-click fires the left one, so two shots every 2 s. The pistols are
     * locked in their slots and taken away when psycho ends. Both are visible in hand, both arms levelled.
     * 连环杀手的疯魔手枪：NoellesRoles 连环杀手开启疯魔模式（在杀手商店购买）时不再拿球棒：连环手枪放进快捷栏空位，左持手枪放进
     * 副手（副手有东西或快捷栏没空位时开不了）；疯魔皮肤、乱码名字和护盾照旧由 Wathe 提供。右键射击准星里 30 格内的玩家，需要
     * 视线，命中即死，有左轮的枪声和枪口火光；两把枪各自冷却 2 秒，主手枪冷却时右键改由左手枪开火，所以每 2 秒能开两枪。两把枪
     * 锁在槽位里，疯魔结束时被收走。两把枪拿在手里都看得见，两只手臂都端平。
     */
    static void serialPistols(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("sparkstrength_serial_pistol", "连环杀手：疯魔双枪");
        WatheItemScenes.stage(scene);
        int color = RoleColors.of("noellesroles:serial_killer", 0x662222);
        Text serialName = Text.literal("连环杀手");
        ItemStack pistol = stack("sparkstrength:serial_pistol");
        ItemStack leftPistol = stack("sparkstrength:serial_left_pistol");
        Text civilian = Text.literal("平民");
        Vec3d firstSpot = new Vec3d(1.6, 1, 5.4);
        Vec3d secondSpot = new Vec3d(4.4, 1, 6.0);
        ElementLink<ActorElement> first = Actors.enter(scene, RoleColors.CIVILIAN, civilian, firstSpot, SCREEN_LEFT,
                Direction.DOWN);
        ElementLink<ActorElement> second = Actors.enter(scene, RoleColors.CIVILIAN, civilian, secondSpot, NORTH,
                Direction.DOWN);
        Vec3d spot = new Vec3d(5.8, 1, 1.2);
        ElementLink<ActorElement> killer = Actors.enter(scene, color, serialName, spot, SCREEN_RIGHT,
                Direction.DOWN);
        scene.idle(15);
        scene.overlay().showControls(spot.add(0, 2.6, 0), Pointing.DOWN, 60).withItem(stack("wathe:psycho_mode"));
        scene.overlay().showText(90)
                .text("装了 SparkStrength 时，连环杀手开疯魔模式不拿球棒，改拿两把手枪")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        Actors.psycho(scene, killer, true);
        Actors.hold(scene, killer, pistol);
        Actors.holdOffHand(scene, killer, leftPistol);
        Actors.retint(scene, killer, color,
                Text.literal("urscrewed").formatted(Formatting.OBFUSCATED, Formatting.DARK_RED));
        scene.overlay().showText(90)
                .text("主手【连环手枪】、副手【连环左持手枪】；疯魔皮肤、乱码名字和护盾照旧")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showControls(spot.add(0, 2.6, 0), Pointing.DOWN, 20).rightClick().withItem(pistol);
        scene.idle(5);
        WatheItemScenes.shoot(scene, killer, muzzle(spot, SCREEN_RIGHT, -0.12), firstSpot.add(0, 0.9, 0));
        Actors.fall(scene, first);
        scene.overlay().showText(75)
                .text("右键开枪，射程 30 格，打中一般当场死亡；隔墙打不到人")
                .independent()
                .attachKeyFrame();
        scene.idle(10);
        float towardSecond = facing(spot, secondSpot);
        Actors.turn(scene, killer, towardSecond);
        scene.idle(5);
        scene.overlay().showControls(spot.add(0, 2.6, 0), Pointing.DOWN, 20).rightClick().withItem(leftPistol);
        scene.idle(5);
        WatheItemScenes.shoot(scene, killer, muzzle(spot, towardSecond, 0.12), secondSpot.add(0, 0.9, 0));
        Actors.fall(scene, second);
        scene.idle(60);
        scene.overlay().showText(90)
                .text("主手枪冷却时再按右键，自动改用副手枪开火；两把枪各自冷却，一般 2 秒")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showText(90)
                .text("疯魔期间两把枪锁在槽位里：丢不掉、移不走，也切换不了别的物品")
                .independent();
        scene.idle(100);
        scene.overlay().showText(90)
                .text("开疯魔时副手必须空着、快捷栏要留一个空位，否则开不了（不扣金币）")
                .independent();
        scene.idle(100);
        Actors.psycho(scene, killer, false);
        Actors.hold(scene, killer, ItemStack.EMPTY);
        Actors.holdOffHand(scene, killer, ItemStack.EMPTY);
        Actors.retint(scene, killer, color, serialName);
        scene.overlay().showText(80)
                .text("疯魔结束（默认 30 秒）时两把枪被收走")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        scene.markAsFinished();
    }

    /**
     * Timed bomb trap (SparkStrength TimedBombTrayUseMixin, TimedBombBedUseMixin, BomberTrapService,
     * TimedBombTrayViewMixin, TimedBombBedViewMixin; NoellesRoles BomberPlayerComponent): a Bomber (or a Coroner
     * disguised as one) right-clicks a food platter or drink tray that holds something and is not poisoned, or a Wathe
     * bed without a scorpion, with the timed bomb; the bomb is used up and hidden there (one per tray or bed; not while
     * the bomb is cooling down, the 45 s opening included, or while carrying a bomb). Clients that can use killer
     * features see smoke over it. The next player who takes from that tray with an empty hand, or right-clicks that bed
     * or a bed joined to it to lie down (not holding a knife or gun), gets the bomb attached: 10 quiet seconds, then 15
     * s of beeping, passable while it beeps. Someone already carrying a bomb does not set it off; it waits for the next.
     * It does not tell friend from foe, and it skips NoellesRoles' placeBomb, so a SparkTraits Conscience Bomber's trap
     * bomb stays lethal until passed (as the guide says).
     * 定时炸弹陷阱：炸弹客（或伪装成炸弹客的验尸官）拿着定时炸弹右键放着东西、没下毒的餐盘或饮品托盘，或没有蝎子的 Wathe 床，
     * 炸弹随即用掉、藏进里面（每个托盘或床一颗；炸弹冷却中，包括开局 45 秒，或自己身上带着炸弹时放不了）。能用杀手阵营特性的
     * 客户端会看到上方冒烟。下一个空手从这个托盘取东西的人，或右键这张床（及与它相连的床）想躺下的人（手持刀枪时除外），会带上
     * 炸弹：先安静 10 秒，再滴滴响 15 秒，滴滴响时可以传走。已经带着炸弹的人不会触发，陷阱留给下一个人。陷阱不分敌我；它绕过
     * NoellesRoles 的 placeBomb，所以 SparkTraits 善良炸弹客藏的炸弹在被传走前仍然致命（与指南书一致）。
     */
    static void timedBombTrap(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("sparkstrength_timed_bomb_trap", "定时炸弹：藏进托盘或床里");
        WatheItemScenes.setStage(scene, util);
        int color = RoleColors.of("noellesroles:bomber", 0x323232);
        ItemStack bomb = stack("noellesroles:timed_bomb");
        Vec3d tray = new Vec3d(3.5, 3.0, 3.5);
        Vec3d start = new Vec3d(6.2, 1, 0.8);
        ElementLink<ActorElement> bomber = Actors.enter(scene, color, Text.literal("炸弹客"), start, SCREEN_RIGHT,
                Direction.DOWN);
        Actors.hold(scene, bomber, bomb);
        scene.idle(15);
        scene.overlay().showText(90)
                .text("装了 SparkStrength 时，定时炸弹还能藏进餐盘、饮品托盘或床里当陷阱")
                .independent()
                .attachKeyFrame();
        Vec3d atTray = new Vec3d(4.4, 1, 2.6);
        Actors.walk(scene, bomber, atTray.subtract(start), 25);
        scene.idle(27);
        Actors.turn(scene, bomber, SCREEN_RIGHT);
        scene.idle(73);
        scene.overlay().showControls(tray, Pointing.DOWN, 30).rightClick().withItem(bomb);
        scene.idle(10);
        // Using a block swings the arm on the client. 对方块使用时客户端会挥手。
        Actors.swing(scene, bomber);
        Actors.hold(scene, bomber, ItemStack.EMPTY);
        // TimedBombTrayViewMixin: smoke at the tray block's centre, rising, on about 4 ticks in 21.
        // 托盘方块中心冒烟、向上飘，约每 21 tick 中有 4 tick 冒出。
        Effects.Marker smoke = Effects.mark(scene, ParticleTypes.SMOKE, new Vec3d(3.5, 2.5, 3.5),
                new Vec3d(0, 0.04, 0), 4 / 21f);
        scene.overlay().showText(90)
                .text("拿着炸弹右键放着东西、没下毒的托盘：炸弹藏了进去，你手里这颗用掉")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showText(80)
                .text("藏了炸弹的托盘会冒烟，一般只有杀手阵营看得见")
                .independent();
        Actors.walk(scene, bomber, start.subtract(atTray), 25);
        scene.idle(27);
        Actors.turn(scene, bomber, SCREEN_RIGHT);
        scene.idle(3);
        Vec3d passengerStart = new Vec3d(1.0, 1, 6.0);
        Vec3d passengerAtTray = new Vec3d(2.6, 1, 4.4);
        ElementLink<ActorElement> passenger = Actors.enter(scene, RoleColors.CIVILIAN, Text.literal("平民"),
                passengerStart, SCREEN_LEFT, Direction.DOWN);
        scene.idle(10);
        Actors.walk(scene, passenger, passengerAtTray.subtract(passengerStart), 25);
        scene.idle(40);
        scene.overlay().showControls(tray, Pointing.DOWN, 30).rightClick();
        scene.idle(10);
        Actors.swing(scene, passenger);
        Actors.hold(scene, passenger, stack("wathe:mojito"));
        Effects.unmark(scene, smoke);
        // The Bomber sees bomb carriers through walls. 炸弹客能隔墙看到带炸弹的人。
        Actors.highlight(scene, passenger, color, 590);
        scene.overlay().showText(90)
                .text("下一个空手来拿东西的人照常拿到，同时悄悄带上了炸弹")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showText(80)
                .text("带着炸弹的人你一般能隔墙看到（只在你屏幕上）")
                .independent();
        scene.idle(90);
        scene.overlay().showText(90)
                .text("之后照常计时：先安静 10 秒，再滴滴响 15 秒后爆炸；滴滴响时也能传给别人")
                .independent();
        scene.idle(100);
        scene.overlay().showText(90)
                .text("床也一样：下一个右键这张床（或与它相连的床）想躺下的人会带上炸弹")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showText(90)
                .text("已经带着炸弹的人不会触发，陷阱会留给下一个人；陷阱不分敌我，杀手也会中招")
                .independent();
        scene.idle(100);
        scene.overlay().showText(90)
                .text("下了毒的托盘、藏着蝎子的床、已藏有炸弹的地方都放不了；你身上带着炸弹、或炸弹还在冷却（一般开局 45 秒）时也放不了")
                .independent();
        scene.idle(100);
        scene.markAsFinished();
    }

    /** The point {@code distance} blocks from {@code from} along {@code yaw}. 从 from 沿 yaw 方向 distance 格处的点。 */
    private static Vec3d along(Vec3d from, float yaw, double distance) {
        float radians = yaw * MathHelper.RADIANS_PER_DEGREE;
        return from.add(-MathHelper.sin(radians) * distance, 0, MathHelper.cos(radians) * distance);
    }

    /** The yaw that faces from {@code from} towards {@code to}. 从 from 朝向 to 的朝向角。 */
    private static float facing(Vec3d from, Vec3d to) {
        return (float) (MathHelper.atan2(-(to.x - from.x), to.z - from.z) * MathHelper.DEGREES_PER_RADIAN);
    }

    /**
     * Where a levelled pistol's muzzle is for an actor at {@code feet} facing {@code yaw}: at shoulder height, 0.85
     * ahead, {@code side} to the actor's left (negative: right).
     * 站在 feet、朝向 yaw 的演员端平的手枪枪口位置：肩高，前方 0.85 格，向演员左侧偏 side（负数为右侧）。
     */
    private static Vec3d muzzle(Vec3d feet, float yaw, double side) {
        float radians = yaw * MathHelper.RADIANS_PER_DEGREE;
        double ahead = 0.85;
        return feet.add(-MathHelper.sin(radians) * ahead + MathHelper.cos(radians) * side, 1.05,
                MathHelper.cos(radians) * ahead + MathHelper.sin(radians) * side);
    }
}
