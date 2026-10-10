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
import java.util.List;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.instruction.TickingInstruction;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

/**
 * Item demos for SparkWitch items that no role demo shows (hold W over the item): the Fire Poker, the Tofana Elixir,
 * the Gift Watch, Cologne, the Disruptor, the Angler's fish and bait, the USEC suppressor and the three non-lethal
 * potion shells. Effects only some players notice (sanity, coins, cooldowns, a private chime, an instinct view) are
 * told in captions or drawn as boxes the captions call illustrations; waits the game takes longer over are shortened,
 * which the captions say.
 * SparkWitch 物品演示（在物品上按住 W），覆盖职业演示没有展示的物品：烧火棍、托法娜仙液、赠时怀表、古龙水、干扰器、钓鱼
 * 佬的鱼与鱼饵、USEC 的消音器，以及三种不致命的药剂炮弹。只有部分玩家察觉得到的效果（理智、金币、冷却、私有钟声、本能视
 * 角）用字幕说明，或画成方框并在字幕中注明是示意；游戏中更长的等待做了缩短，字幕会注明。
 */
final class SparkWitchItemScenes {
    private static final List<String> SW = List.of("sparkwitch");
    private static final List<String> SW_TRAITS = List.of("sparkwitch", "sparktraits");
    /** A standing actor's eyes above the feet. 站立演员的眼睛离脚底的高度。 */
    private static final double EYES = 1.62;
    /** The stages' plate width. 舞台底板的边长。 */
    private static final double PLATE = 7;
    /** A player's half width: feet stop this far from a wall. 玩家碰撞箱半宽：脚离墙至少这么远。 */
    private static final double HALF_WIDTH = 0.3;
    /**
     * The gun of an actor at (5.8, 1, 1.2) facing screen-right, as in WatheItemScenes.
     * 站在 (5.8, 1, 1.2) 朝屏幕右侧的演员的枪口，与 WatheItemScenes 相同。
     */
    private static final Vec3d REVOLVER_MUZZLE = new Vec3d(5.2, 2.05, 1.8);
    /** What a killer holding instinct sees non-killers in (WatheClient.getInstinctHighlight). 杀手按住本能键时非杀手的颜色。 */
    private static final int INSTINCT_GREEN = 0x4EDD35;
    /** An illustrated "not affected" box. 示意“不受影响”的方框颜色。 */
    private static final int UNAFFECTED_GREEN = 0x7AE04F;
    /** Marks where an invisible player is: an illustration only. 标出隐身玩家位置的方框：仅为示意。 */
    private static final int WHERE_WHITE = 0xFFFFFF;
    /** Gold dust for an illustrated area edge. 用于示意范围边界的金色粉尘。 */
    private static final DustParticleEffect EDGE_DUST = new DustParticleEffect(new Vector3f(1.0f, 0.8f, 0.15f), 1.5f);
    private static final int EDGE_POINTS = 240;
    /** The Fire Poker's push, blocks a tick (FirePokerRules). 烧火棍的推力（每 tick 格数）。 */
    private static final double POKER_PUSH = 10.0;
    private static final double POKER_LIFT = 0.35;
    /**
     * Vanilla's velocity packet (EntityVelocityUpdateS2CPacket) clamps each axis to 3.9 blocks a tick, so that is what
     * the pushed player's client flies with.
     * 原版速度数据包把每轴限制在每 tick 3.9 格，被推者的客户端按此飞行。
     */
    private static final double VELOCITY_PACKET_CAP = 3.9;
    /** PotionShellType colours and cube edges. PotionShellType 的颜色与爆炸立方体边长。 */
    private static final int DK_COLOR = 0x4B3B8F;
    private static final int AC_COLOR = 0x2FB8C9;
    private static final int MR_COLOR = 0xE0B23A;
    private static final int DK_SIZE = 5;
    private static final int AC_SIZE = 7;
    private static final int MR_SIZE = 3;

    private SparkWitchItemScenes() {
    }

    static void register() {
        item("sparkwitch:fire_poker", SW, scene("noellesroles/axe_wall", SparkWitchItemScenes::firePoker));
        item("sparkwitch:tofana_elixir", SW, scene("wathe/aisle", SparkWitchItemScenes::tofanaElixir));
        item("sparkwitch:time_stealer_gift_watch", SW_TRAITS,
                scene("wathe/aisle", SparkWitchItemScenes::giftWatch));
        item("sparkwitch:cologne", SW, scene("wathe/aisle", SparkWitchItemScenes::cologne));
        item("sparkwitch:disruptor", SW, scene("wathe/cabin", SparkWitchItemScenes::disruptor));
        item("sparkwitch:clownfish", SW, scene("sparkwitch/civ_tray_cabin", SparkWitchItemScenes::edibleFish));
        for (String id : List.of("sparkwitch:cod", "sparkwitch:goldfish", "sparkwitch:fish_bait")) {
            item(id, SW, scene("sparkwitch/civ_tray_cabin", SparkWitchCivilianScenes::fisher),
                    scene("sparkwitch/civ_tray_cabin", SparkWitchItemScenes::edibleFish));
        }
        item("sparkwitch:glimmerfish", SW, scene("wathe/cabin", SparkWitchItemScenes::glimmerfish));
        item("sparkwitch:swordfish", SW, scene("wathe/aisle", SparkWitchItemScenes::swordfish));
        item("sparkwitch:usec_suppressor", SW, scene("wathe/aisle", SparkWitchItemScenes::usecSuppressor));
        for (String id : List.of("sparkwitch:gw_dk_shell", "sparkwitch:gw_ac_shell", "sparkwitch:gw_mr_shell")) {
            item(id, SW, scene("wathe/aisle", SparkWitchKillerScenes::potionGunner),
                    scene("wathe/aisle", SparkWitchItemScenes::potionShells));
        }
    }

    /**
     * Fire Poker (FirePokerCombatService, FirePokerRules, FirePokerFallAttributionService, MurderousWitchShopRules):
     * the Murderous Witch's shop sells one for 50; it is not hidden in hand. A left-click on a player (an ordinary
     * swing; the server cancels the attack) does no damage: the target is pushed 10 blocks a tick straight away from
     * the holder and 0.35 up, which in a carriage slams him into the next wall at once. A landed strike sets a 10 s
     * cooldown (a swing at the air sets none). With at least 20 mana it also spends 20: the target gets Blindness and
     * Slowness III, the holder Speed III, 3 s each, all without particles. A train-fall death within 10 s of the push
     * is credited to the pusher (the latest push wins). While her Death Ray window is open, a left-click fires the ray
     * instead. In the open the push carries on until friction stops it.
     * 烧火棍：杀意魔女商店 50 金币一根，手持不隐藏。左键打中玩家（普通挥手；服务端取消这次攻击）不造成伤害：对方被沿“持
     * 有者指向对方”的方向以每 tick 10 格水平推开并上抬 0.35，在车厢里会立刻撞上墙。推中才进入 10 秒冷却（挥空不算）。魔
     * 力不少于 20 时另扣 20：对方失明并获得缓慢 III，持有者获得速度 III，各 3 秒，都不显示粒子。被推者 10 秒内掉下火车，
     * 死亡记在推人者名下（以最近一次推人为准）。死亡射线窗口开着时，左键改为发射射线。在空旷处，人会一直飞到被摩擦力停下。
     */
    private static void firePoker(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("sparkwitch_fire_poker", "烧火棍：把人推飞");
        WatheItemScenes.setStage(scene, util);
        ItemStack poker = stack("sparkwitch:fire_poker");
        // The back wall's face is at z = 6. 后墙表面在 z = 6。
        Box room = new Box(HALF_WIDTH, 0, HALF_WIDTH, PLATE - HALF_WIDTH, 4, 6 - HALF_WIDTH - 0.01);
        Vec3d victimFeet = new Vec3d(3.0, 1, 2.4);
        ElementLink<ActorElement> victim = Actors.enter(scene, RoleColors.CIVILIAN, Text.literal("平民"), victimFeet,
                NORTH, Direction.DOWN);
        scene.idle(5);
        Vec3d feet = new Vec3d(3.0, 1, 0.9);
        Vec3d overHead = feet.add(0, 2.6, 0);
        ElementLink<ActorElement> witch = Actors.enter(scene, RoleColors.of("sparkwitch:murderous_witch", 0x7A3857),
                Text.literal("杀意魔女"), feet, SOUTH, Direction.DOWN);
        Actors.hold(scene, witch, poker);
        scene.idle(20);
        scene.overlay().showText(80)
                .text("杀意魔女能在商店花 50 金币买烧火棍（每局限 1 根），拿在手上别人看得见")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        scene.overlay().showControls(overHead, Pointing.DOWN, 30).leftClick().withItem(poker);
        scene.idle(10);
        Actors.swing(scene, witch);
        shove(scene, victim, pokerLaunch(feet, victimFeet), room, 20);
        scene.idle(25);
        scene.overlay().showText(80)
                .text("左键打中人不会伤到他，而是把他猛地推飞出去；在车厢里会直接撞到墙上")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        scene.overlay().showText(70)
                .text("推中后冷却 10 秒；挥空不进冷却")
                .independent();
        scene.idle(80);
        scene.overlay().showText(35)
                .text("……10 秒后（演示缩短了时间）")
                .independent();
        Actors.walk(scene, victim, new Vec3d(0, 0, victimFeet.z - room.maxZ), 40);
        scene.idle(45);
        scene.overlay().showText(55)
                .text("魔力不少于 20 时，推中还会自动扣 20 魔力……")
                .independent()
                .attachKeyFrame();
        scene.idle(30);
        scene.overlay().showControls(overHead, Pointing.DOWN, 30).leftClick().withItem(poker);
        scene.idle(10);
        Actors.swing(scene, witch);
        shove(scene, victim, pokerLaunch(feet, victimFeet), room, 20);
        scene.idle(25);
        // Slowness III takes 45% off his pace, Speed III adds 60% to hers.
        // 缓慢 III 让他步速降低 45%，速度 III 让她步速提高 60%。
        Actors.walk(scene, victim, new Vec3d(-1.2, 0, 0), 24);
        Actors.walk(scene, witch, new Vec3d(0, 0, 3.0), 20);
        scene.overlay().showText(90)
                .text("……让他失明并获得缓慢 III，你获得速度 III，各 3 秒（都不显示粒子）")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showText(90)
                .colored(PonderPalette.RED)
                .text("推中后 10 秒内他掉下火车，一般算作你的击杀")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showText(70)
                .text("所以最适合在火车边缘把人推下去")
                .independent();
        scene.idle(80);
        scene.markAsFinished();
    }

    /**
     * Tofana Elixir (TofanaProtectionService, TofanaRetaliationQueue, TofanaRules, GameFunctionsTofanaProtectionMixin,
     * CeremonialSwordProtectionPolicy, WitchMaidenShopService; Wathe GunShootPayload, GameFunctions.killPlayer): the
     * Witch Maiden's shop sells one for 200; it works from anywhere in the inventory and is not hidden in hand. When
     * another living participant's non-forced kill of the holder gets past every earlier protection (shields, psycho
     * armour), one elixir is used up and the death is cancelled; at the end of the next server tick the killer is
     * normally killed (an ordinary kill with the elixir's own death reason, credited to the holder, so the killer's own
     * shield, armour or elixir can still stop it; skipped if the killer is dead or gone by then). Forced kills ignore
     * it; the Ceremonial Sword uses it up and is still avenged, but the holder dies too. A dead player's revolver
     * drops.
     * 托法娜仙液：巫女商店 200 金币一瓶；放在背包任何位置都生效，手持不隐藏。其他存活参与者对持有者的非强制击杀越过了所
     * 有更早的保护（护盾、疯魔护甲）时，用掉一瓶并取消这次死亡；下一个服务端 tick 结束时凶手一般被杀（普通击杀，死因是
     * 仙液反噬，记在持有者名下，所以凶手自己的护盾、护甲或仙液仍可能挡下；凶手届时已死或离开则不执行）。强制击杀不受影
     * 响；仪礼剑照样用掉仙液、照样被反杀，但持有者也会死。死者的左轮会掉落。
     */
    private static void tofanaElixir(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("sparkwitch_tofana_elixir", "托法娜仙液：挡下一死并反杀");
        WatheItemScenes.stage(scene);
        ItemStack elixir = stack("sparkwitch:tofana_elixir");
        ItemStack revolver = stack("wathe:revolver");
        ElementLink<ActorElement> maiden = Actors.enter(scene, RoleColors.of("sparkwitch:witch_maiden", 0xB04A8B),
                Text.literal("巫女"), new Vec3d(2.0, 1, 5.0), SCREEN_LEFT, Direction.DOWN);
        Actors.hold(scene, maiden, elixir);
        scene.idle(20);
        scene.overlay().showText(80)
                .text("巫女能在商店花 200 金币买托法娜仙液，每局限 1 瓶")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        scene.overlay().showText(70)
                .text("放在背包里就会生效，不用拿在手上")
                .independent();
        scene.idle(80);
        ElementLink<ActorElement> vigilante = Actors.enter(scene, RoleColors.VIGILANTE, Text.literal("义警"),
                new Vec3d(5.8, 1, 1.2), SCREEN_RIGHT, Direction.DOWN);
        Actors.hold(scene, vigilante, revolver);
        scene.idle(20);
        scene.overlay().showText(45)
                .text("有人要杀她，比如义警开枪……")
                .independent()
                .attachKeyFrame();
        scene.idle(20);
        scene.overlay().showControls(new Vec3d(5.8, 3.6, 1.2), Pointing.DOWN, 30).rightClick().withItem(revolver);
        scene.idle(10);
        gunshotFlash(scene, REVOLVER_MUZZLE);
        WatheItemScenes.shoot(scene, vigilante, REVOLVER_MUZZLE, new Vec3d(2.2, 1.9, 4.8));
        Actors.hold(scene, maiden, ItemStack.EMPTY);
        scene.idle(1);
        Actors.hold(scene, vigilante, ItemStack.EMPTY);
        scene.world().createItemEntity(new Vec3d(5.6, 1.5, 1.4), new Vec3d(0.05, 0.12, -0.03), revolver);
        Actors.fall(scene, vigilante);
        scene.idle(20);
        scene.overlay().showText(80)
                .colored(PonderPalette.GREEN)
                .text("仙液自动用掉一瓶，挡下了这次死亡：她还站着")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        scene.overlay().showText(80)
                .colored(PonderPalette.RED)
                .text("紧接着凶手一般当场毙命，算作她的击杀")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        scene.overlay().showText(90)
                .text("护盾等其他保护会先生效；只挡其他活着的玩家下的手，强制死亡挡不住")
                .independent();
        scene.idle(100);
        scene.overlay().showText(90)
                .text("大魔女的仪礼剑例外：仙液照样用掉、大魔女照样被反杀，但巫女自己也还是会死")
                .independent();
        scene.idle(100);
        scene.markAsFinished();
    }

    /**
     * Gift Watch (TimeGiftWatchItem, TimeGiftWatchService, TimeGiftWatchLoadout, TimeGiftRuntime, TimeStealerRules,
     * TimeStealerTargeting; SparkWitch's NoellesHiddenEquipment list): only a Time Stealer whose effective faction is
     * not the killers' (SparkTraits Conscience) holds it, next to the curse watch; it is hidden in hand and its use
     * never swings. Aimed at a player within 7 blocks and in sight (any faction, never oneself, not one currently being
     * gifted) it starts a gift: nothing for 15 s, then Speed I-IV with a chime only the target hears at 15, 20, 25 and
     * 30 s, and at 35 s Speed V for 5 s and full sanity. No particles. 45 s cooldown after a gift and at round start; a
     * miss is free. The Timekeeper never lifts a gift. The notes over the head stand in for the private chimes.
     * 赠时怀表：只有有效阵营不是杀手的窃时者（SparkTraits 善良）才有，和诅咒用的时间怀表放在一起；手持隐藏，使用不挥手。
     * 对准 7 格内看得见的玩家（不分阵营，不能对自己，正在受赠的不行）即开始赠时：前 15 秒毫无动静，之后在 15、20、25、
     * 30 秒依次获得速度 I～IV 并听到只有自己听得见的钟声，35 秒时速度 V 持续 5 秒并恢复全部理智。没有粒子。赠时成功后冷
     * 却 45 秒，开局亦然；没对准不进冷却。计时员解除不了赠时。头顶的音符代表私有钟声。
     */
    private static void giftWatch(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("sparkwitch_time_stealer_gift_watch", "赠时怀表：把时间送给别人");
        WatheItemScenes.stage(scene);
        ItemStack watch = stack("sparkwitch:time_stealer_gift_watch");
        ElementLink<ActorElement> civilian = Actors.enter(scene, RoleColors.CIVILIAN, Text.literal("平民"),
                new Vec3d(1.8, 1, 5.2), SCREEN_LEFT, Direction.DOWN);
        scene.idle(5);
        Vec3d feet = new Vec3d(5.8, 1, 1.2);
        ElementLink<ActorElement> stealer = Actors.enter(scene, RoleColors.of("sparkwitch:time_stealer", 0x2E8B7A),
                Text.literal("窃时者").append(Text.literal("（善良）").formatted(Formatting.GRAY)), feet,
                SCREEN_RIGHT, Direction.DOWN);
        Actors.hold(scene, stealer, watch);
        scene.idle(20);
        scene.overlay().showText(90)
                .text("只有带【善良】词条（SparkTraits）的窃时者，才会在时间怀表之外多一块赠时怀表")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showControls(feet.add(0, 2.6, 0), Pointing.DOWN, 30).rightClick().withItem(watch);
        scene.overlay().showText(90)
                .text("其他活人看不见它；对准 7 格内看得见的人右键，把时间送给他，没有挥手动作")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showText(80)
                .text("不分阵营，不能给自己用；赠出后冷却 45 秒，开局也要等 45 秒")
                .independent();
        scene.idle(30);
        Actors.walk(scene, civilian, new Vec3d(1.5, 0, 0), 30);
        scene.idle(60);
        scene.overlay().showText(45)
                .text("前 15 秒对方毫无变化……")
                .independent()
                .attachKeyFrame();
        scene.idle(55);
        scene.overlay().showText(35)
                .text("……15 秒后（演示缩短了时间）")
                .independent();
        scene.idle(45);
        // Each chime starts the next, faster leg of 30 ticks: Speed I-IV add 20, 40, 60 and 80% to the pace.
        // 每声钟响开始下一段 30 tick 的更快路程：速度 I～IV 分别让步速提高 20%、40%、60%、80%。
        chime(scene, new Vec3d(3.3, 3.2, 5.2), 1);
        scene.overlay().showText(110)
                .text("之后每 5 秒响一次钟（音符示意，只有他自己听得见），一次比一次走得快，没有粒子")
                .independent()
                .attachKeyFrame();
        Actors.walk(scene, civilian, new Vec3d(0, 0, -1.8), 30);
        scene.idle(30);
        chime(scene, new Vec3d(3.3, 3.2, 3.4), 2);
        Actors.walk(scene, civilian, new Vec3d(-2.1, 0, 0), 30);
        scene.idle(30);
        chime(scene, new Vec3d(1.2, 3.2, 3.4), 3);
        Actors.walk(scene, civilian, new Vec3d(0, 0, 2.4), 30);
        scene.idle(30);
        chime(scene, new Vec3d(1.2, 3.2, 5.8), 4);
        Actors.walk(scene, civilian, new Vec3d(2.7, 0, 0), 30);
        scene.idle(30);
        chime(scene, new Vec3d(3.9, 3.2, 5.8), 5);
        Actors.walk(scene, civilian, new Vec3d(0, 0, -3.0), 30);
        scene.overlay().showText(90)
                .colored(PonderPalette.GREEN)
                .text("第 35 秒：速度 V 约 5 秒，理智回满（只显示在他自己的界面上）")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showText(80)
                .text("计时员解除不了赠时；正在受赠的人不能再被赠，35 秒结束后才行")
                .independent();
        scene.idle(90);
        scene.markAsFinished();
    }

    /**
     * Cologne (CologneItem, PerfumerRules, PerfumerState, PerfumerRuntime, PerfumerShopService; SparkWitch's
     * NoellesHiddenEquipment list): 50 coins in the Perfumer's shop, no stock limit, hidden in hand from other living
     * players, usable only by an active Perfumer. A right-click on a living player within 3 blocks and in sight, or any
     * right-click that reaches no player (the air) for oneself, swings (the client returns success), uses one up and
     * starts (or restarts, never stacks) a 10 s timer: +5% sanity every second, capped at 100%. The user gets an
     * action-bar line, and so does the target when it is someone else; there are no particles and no sound, and sanity
     * shows only in the target's own HUD.
     * 古龙水：调香师商店 50 金币，不限购，手持对其他活人隐藏，只有在场的调香师能用。右键 3 格内看得见的活人，或没对准人
     * （对着空气）右键给自己用，会挥手（客户端返回成功），用掉一瓶，开始（或重新开始，不叠加）10 秒计时：每秒回复 5% 理
     * 智，最多到 100%。使用者收到一行动作栏提示，给别人用时目标也会收到；没有粒子和声音，理智只显示在目标自己的界面上。
     */
    private static void cologne(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("sparkwitch_cologne", "古龙水：帮人稳住理智");
        WatheItemScenes.stage(scene);
        ItemStack cologne = stack("sparkwitch:cologne");
        ElementLink<ActorElement> civilian = Actors.enter(scene, RoleColors.CIVILIAN, Text.literal("平民"),
                new Vec3d(3.4, 1, 3.6), SCREEN_LEFT, Direction.DOWN);
        scene.idle(5);
        ElementLink<ActorElement> perfumer = Actors.enter(scene, RoleColors.of("sparkwitch:perfumer", 0xF2A4A4),
                Text.literal("调香师"), new Vec3d(5.6, 1, 1.4), SCREEN_RIGHT, Direction.DOWN);
        Actors.hold(scene, perfumer, cologne);
        scene.idle(20);
        scene.overlay().showText(90)
                .text("调香师能在商店花 50 金币买古龙水，不限购；拿在手上其他活人看不见")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        Actors.walk(scene, perfumer, new Vec3d(-0.8, 0, 0.8), 14);
        scene.idle(16);
        Vec3d overHead = new Vec3d(4.8, 3.6, 2.2);
        scene.overlay().showControls(overHead, Pointing.DOWN, 30).rightClick().withItem(cologne);
        scene.idle(10);
        Actors.swing(scene, perfumer);
        Actors.hold(scene, perfumer, ItemStack.EMPTY);
        scene.overlay().showText(80)
                .text("对 3 格内看得见的人右键（会挥手），用掉一瓶")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        scene.overlay().showText(90)
                .colored(PonderPalette.GREEN)
                .text("他 10 秒内每秒回复 5%% 理智（最多回 50%%），理智只显示在他自己的界面上")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showText(80)
                .text("你和他屏幕上各会出现一行提示；旁人只看得到挥手，看不出效果：没有粒子，也没有声音")
                .independent();
        scene.idle(90);
        Actors.hold(scene, perfumer, cologne);
        Actors.turn(scene, perfumer, NORTH);
        Actors.lookPitch(scene, perfumer, -30);
        scene.idle(15);
        scene.overlay().showControls(overHead, Pointing.DOWN, 30).rightClick().withItem(cologne);
        scene.idle(10);
        Actors.swing(scene, perfumer);
        Actors.hold(scene, perfumer, ItemStack.EMPTY);
        scene.overlay().showText(70)
                .text("没对准 3 格内的人（比如对着空气）右键，就是给自己用，同样用掉一瓶")
                .independent()
                .attachKeyFrame();
        scene.idle(80);
        Actors.lookPitch(scene, perfumer, 0);
        scene.overlay().showText(80)
                .text("再用一次只会把 10 秒重新计时，不会叠加；只有调香师能用")
                .independent();
        scene.idle(90);
        scene.markAsFinished();
    }

    /**
     * Disruptor (DisruptorItem, ControlExpertRules, ControlExpertTargeting, ControlExpertShopService,
     * ControlExpertInventoryRules; client ControlExpertInstinctGateMixin; Wathe WatheClient instinct): 100 coins in the
     * Control Expert's shop, one per round, 30 s locked at round start, usable only by an unstunned Control Expert and
     * never droppable; not hidden in hand. A right-click (no swing: the client returns success without a swing) gives
     * every other affectable participant whose feet are within 6 blocks of the user's, walls ignored and allies
     * included (Wraiths, SparkTraits Last Escape, Vendetta isolation and SparkFactionAPI vetoes excepted), 10 s without
     * keyed instinct: no instinct highlights that need the key and no instinct light; keyless outlines stay. Each one
     * hears a private sound and sees an on-screen countdown (the action-bar line is best effort); the user alone hears
     * a fixed use sound. Not used up; a plain 90 s cooldown that SparkTraits Fast Hands may shorten. Outlines are
     * boxes, the 6-block edge gold dust.
     * 干扰器：控场专家商店 100 金币，每局限 1 个，开局锁 30 秒，只有未被电住的控场专家能用，丢不出去；手持不隐藏。右键
     * （不挥手：客户端返回成功但不挥手）让脚下距使用者 6 格内的其他所有可影响参与者 10 秒内失去按键本能：需要按键的本能
     * 高亮和本能夜视都失效，不需按键的描边不受影响；墙挡不住，队友也算（冤魂、SparkTraits 最后逃脱、复仇者隔离与
     * SparkFactionAPI 否决除外）。每人听到一声私密音效，屏幕上显示倒计时（动作栏提示只是尽力而为）；使用声固定、只有使
     * 用者听得到。不会用掉，普通原版冷却 90 秒，SparkTraits 快手可缩短。描边用方框表示，6 格边界用金色粉尘表示。
     */
    private static void disruptor(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("sparkwitch_disruptor", "干扰器：关掉附近的本能");
        WatheItemScenes.cabinStage(scene, util);
        ItemStack disruptor = stack("sparkwitch:disruptor");
        ElementLink<ActorElement> killer = Actors.enter(scene, RoleColors.KILLER, Text.literal("杀手"),
                new Vec3d(4.6, 1, 5.0), NORTH, Direction.DOWN);
        Actors.hold(scene, killer, stack("wathe:knife"));
        ElementLink<ActorElement> civilian = Actors.enter(scene, RoleColors.CIVILIAN, Text.literal("平民"),
                new Vec3d(1.6, 1, 1.6), EAST, Direction.DOWN);
        scene.idle(5);
        Vec3d feet = new Vec3d(4.4, 1, 1.2);
        ElementLink<ActorElement> expert = Actors.enter(scene, RoleColors.of("sparkwitch:control_expert", 0x4DD0E1),
                Text.literal("控场专家"), feet, SCREEN_RIGHT, Direction.DOWN);
        Actors.hold(scene, expert, disruptor);
        scene.idle(20);
        scene.overlay().showText(90)
                .text("控场专家能在商店花 100 金币买干扰器（限购 1 个），开局要等 30 秒才能用")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        // What the killer's screen shows while he holds the instinct key. 杀手按住本能键时他屏幕上的样子。
        Actors.highlight(scene, expert, INSTINCT_GREEN, 110);
        Actors.highlight(scene, civilian, INSTINCT_GREEN, 110);
        scene.overlay().showText(90)
                .text("墙后的杀手按住本能键，能隔墙看到附近的人（绿框示意，只在他的屏幕上）")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showControls(feet.add(0, 2.6, 0), Pointing.DOWN, 30).rightClick().withItem(disruptor);
        scene.idle(10);
        edge(scene, feet.add(0, 0.05, 0), 6, 100);
        scene.overlay().showText(90)
                .text("右键使用，不挥手：6 格内除你以外的参赛者一般 10 秒内都用不了按键本能（金色示意范围）")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showText(90)
                .text("隔着墙也算：杀手的本能透视和本能夜视都失效，绿框就看不到了")
                .independent();
        scene.idle(100);
        scene.overlay().showText(80)
                .text("被干扰的人会听到一声音效，屏幕上显示本能受干扰的倒计时；使用声只有你自己听得到")
                .independent();
        scene.idle(90);
        scene.overlay().showText(80)
                .colored(PonderPalette.RED)
                .text("它不分敌我：范围内队友的按键本能（如果有）也会一起失效")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        scene.overlay().showText(80)
                .text("干扰器不会用掉，默认冷却 90 秒；只有控场专家能用，也丢不出去")
                .independent();
        scene.idle(90);
        scene.markAsFinished();
    }

    /**
     * The Angler's bait and edible fish (FisherFishingService, FisherInventory, FisherRules, FisherFishItem,
     * FisherFishUse, FisherParticipants; SparkWitch's NoellesHiddenEquipment list): bait costs 25 in the Angler's shop.
     * Each cast (the rod in the main hand, a right-click on a drink tray, one swing) needs a free hotbar slot and uses
     * one bait from anywhere in the inventory; the catch is a weighted roll (nothing at all 10% of the time; this demo
     * happens to land a cod, a goldfish and a clownfish) and goes into the hotbar (here straight into the hand). Any
     * living participant can eat a fish: a right-click eats it at once, no swing and no animation, the eating sound
     * private. Cod +20% sanity; Goldfish +20% sanity and 50 coins; Clownfish +30% sanity and 10 s of Speed I without
     * particles. All fish share a 1 s cooldown and never complete the eat task. Rod, bait and edible fish are hidden in
     * hand from other living players.
     * 钓鱼佬的鱼饵与能吃的鱼：鱼饵在钓鱼佬商店 25 金币一个。每钓一次（主手拿鱼竿右键饮料托盘，挥一下手）需要快捷栏有空
     * 位，并从背包任意位置用掉一个鱼饵；渔获按权重随机（10% 什么也没有；演示里恰好依次钓到鳕鱼、金鱼、小丑鱼），放进快
     * 捷栏（演示中直接拿在手上）。任何存活参与者都能吃鱼：右键立刻吃掉，不挥手、没有动画，吃鱼声只有自己听得到。鳕鱼理
     * 智 +20%；金鱼理智 +20% 并得 50 金币；小丑鱼理智 +30%，并获得 10 秒无粒子的速度 I。所有鱼共用 1 秒冷却，吃鱼不算完
     * 成进食任务。鱼竿、鱼饵和能吃的鱼手持时对其他活人隐藏。
     */
    private static void edibleFish(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("sparkwitch_edible_fish", "鱼饵与能吃的鱼");
        WatheItemScenes.setStage(scene, util);
        Vec3d tray = new Vec3d(0.5, 3.0, 1.5);
        Vec3d overHead = new Vec3d(1.5, 3.6, 1.5);
        ItemStack bait = stack("sparkwitch:fish_bait");
        ItemStack rod = stack("sparkwitch:fishing_rod");
        ItemStack cod = stack("sparkwitch:cod");
        ItemStack goldfish = stack("sparkwitch:goldfish");
        ItemStack clownfish = stack("sparkwitch:clownfish");
        ElementLink<ActorElement> fisher = Actors.enter(scene, RoleColors.of("sparkwitch:fisher", 0x2A9D8F),
                Text.literal("钓鱼佬"), new Vec3d(2.9, 1, 1.5), WEST, Direction.DOWN);
        Actors.hold(scene, fisher, bait);
        scene.idle(15);
        scene.overlay().showText(80)
                .text("鱼饵在钓鱼佬的商店 25 金币一个；拿在手上其他活人看不见")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        Actors.hold(scene, fisher, rod);
        Actors.walk(scene, fisher, new Vec3d(-1.4, 0, 0), 20);
        scene.idle(22);
        scene.overlay().showControls(tray, Pointing.DOWN, 30).rightClick().withItem(rod);
        scene.idle(10);
        Actors.swing(scene, fisher);
        scene.overlay().showText(90)
                .text("拿鱼竿右键饮料托盘，每钓一次用掉 1 个鱼饵（放在背包哪里都行），快捷栏一般要留个空位")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        Actors.hold(scene, fisher, cod);
        scene.overlay().showText(80)
                .text("钓到什么全凭运气，这次是鳕鱼：右键立刻吃掉，不挥手，理智回复 20%%")
                .independent()
                .attachKeyFrame();
        scene.idle(40);
        scene.overlay().showControls(overHead, Pointing.DOWN, 30).rightClick().withItem(cod);
        scene.idle(10);
        Actors.hold(scene, fisher, ItemStack.EMPTY);
        scene.idle(45);
        Actors.hold(scene, fisher, rod);
        scene.idle(10);
        scene.overlay().showControls(tray, Pointing.DOWN, 30).rightClick().withItem(rod);
        scene.idle(10);
        Actors.swing(scene, fisher);
        scene.idle(10);
        Actors.hold(scene, fisher, goldfish);
        scene.overlay().showText(80)
                .text("再钓一次，钓到金鱼：理智回复 20%%，另得 50 金币")
                .independent()
                .attachKeyFrame();
        scene.idle(40);
        scene.overlay().showControls(overHead, Pointing.DOWN, 30).rightClick().withItem(goldfish);
        scene.idle(10);
        Actors.hold(scene, fisher, ItemStack.EMPTY);
        scene.idle(45);
        Actors.hold(scene, fisher, rod);
        scene.idle(10);
        scene.overlay().showControls(tray, Pointing.DOWN, 30).rightClick().withItem(rod);
        scene.idle(10);
        Actors.swing(scene, fisher);
        scene.idle(10);
        Actors.hold(scene, fisher, clownfish);
        scene.overlay().showText(90)
                .text("小丑鱼：理智回复 30%%，还能获得 10 秒速度（没有粒子）")
                .independent()
                .attachKeyFrame();
        scene.idle(40);
        scene.overlay().showControls(overHead, Pointing.DOWN, 30).rightClick().withItem(clownfish);
        scene.idle(10);
        Actors.hold(scene, fisher, ItemStack.EMPTY);
        scene.idle(45);
        Actors.walk(scene, fisher, new Vec3d(0.1, 0, -0.9), 12);
        ElementLink<ActorElement> civilian = Actors.enter(scene, RoleColors.CIVILIAN, Text.literal("平民"),
                new Vec3d(1.6, 1, 2.3), EAST, Direction.DOWN);
        scene.idle(15);
        Actors.turn(scene, fisher, EAST);
        scene.idle(8);
        // Speed I adds 20% to the pace: the same 4.5 blocks take him 38 ticks and the passenger 46.
        // 速度 I 让步速提高 20%：同样 4.5 格，他用 38 tick，乘客用 46 tick。
        Actors.walk(scene, fisher, new Vec3d(4.5, 0, 0), 38);
        Actors.walk(scene, civilian, new Vec3d(4.5, 0, 0), 46);
        scene.overlay().showText(80)
                .text("同样一段路，吃了小丑鱼的他先走到")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        scene.overlay().showText(90)
                .text("理智和金币只显示在自己的界面上；所有鱼共用 1 秒冷却")
                .independent();
        scene.idle(100);
        scene.overlay().showText(90)
                .text("这些鱼拿在手上其他活人看不见；不是钓鱼佬也能吃，但吃鱼不算完成进食任务")
                .independent();
        scene.idle(100);
        scene.markAsFinished();
    }

    /**
     * Glimmerfish (FisherFishUse, FisherSpiritService, FisherInvisibility, FisherDoorPassingMixin,
     * FisherDoorPassingRules, FisherSpiritExit; client FisherGlimmerInstinctHooks, FisherGlimmerClientRules,
     * FisherHeldItemFeatureRendererMixin): any living participant may eat it (no swing), which sets sanity to full and
     * starts (or restarts) a 156-tick window: vanilla invisibility without particles and, for every non-spectator
     * viewer, no held items either; doors (not trapdoors or gates) have no collision for the eater, nor do other
     * players; the eater usually sees every other living player outlined in the Angler's colour through walls with no
     * key (not other glimmering or SparkTraits-hidden players, and nobody under a Fear or Obscure suppression), while
     * living viewers usually lose the eater's instinct outline (dead spectators and a few role views keep it). When the
     * window ends inside a door, the eater is moved to a safe spot out of it if one exists. The demo stretches the
     * window; the outlines are boxes; the white box only marks where the invisible eater is.
     * 灵光鱼：任何存活参与者都能吃（不挥手），理智回满并开始（或重新开始）156 tick 的窗口：原版隐身且没有粒子，所有非旁
     * 观者也看不到其手持物；门（不含活板门和栅栏门）对他没有碰撞，其他玩家也没有；他一般不用按键就能隔墙看到其他活人，
     * 颜色统一为钓鱼佬色（同在灵光中或被 SparkTraits 隐藏的人除外，被恐惧或障眼压制时什么都看不到），活着的旁人一般看不
     * 到他的本能描边（已死亡的旁观者和少数职业视角除外）。窗口结束时若人在门里，有安全位置的话会被挪到门外。演示拉长了
     * 窗口；描边用方框表示；白框只标出隐身者的位置。
     */
    private static void glimmerfish(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("sparkwitch_glimmerfish", "灵光鱼：隐身穿门");
        WatheItemScenes.cabinStage(scene, util);
        ItemStack glimmerfish = stack("sparkwitch:glimmerfish");
        int anglerColor = RoleColors.of("sparkwitch:fisher", 0x2A9D8F);
        ElementLink<ActorElement> killer = Actors.enter(scene, RoleColors.KILLER, Text.literal("杀手"),
                new Vec3d(5.0, 1, 5.4), NORTH, Direction.DOWN);
        Actors.hold(scene, killer, stack("wathe:knife"));
        ElementLink<ActorElement> civilian = Actors.enter(scene, RoleColors.CIVILIAN, Text.literal("平民"),
                new Vec3d(5.8, 1, 1.0), WEST, Direction.DOWN);
        scene.idle(5);
        Vec3d feet = new Vec3d(1.0, 1, 1.5);
        ElementLink<ActorElement> fisher = Actors.enter(scene, anglerColor, Text.literal("钓鱼佬"), feet, EAST,
                Direction.DOWN);
        Actors.hold(scene, fisher, glimmerfish);
        scene.idle(20);
        scene.overlay().showText(80)
                .text("灵光鱼是钓鱼佬偶尔钓到的鱼；拿在手上其他活人看不见，任何活着的玩家都能吃")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        scene.overlay().showControls(feet.add(0, 2.6, 0), Pointing.DOWN, 30).rightClick().withItem(glimmerfish);
        scene.idle(10);
        Actors.hold(scene, fisher, ItemStack.EMPTY);
        Actors.invisible(scene, fisher, true);
        Actors.highlight(scene, fisher, WHERE_WHITE, 310);
        scene.overlay().showText(90)
                .text("右键吃下：理智回满，约 7.8 秒内隐身（没有粒子），手里的东西别人也看不见（白框只是示意位置）")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        Actors.highlight(scene, killer, anglerColor, 210);
        Actors.highlight(scene, civilian, anglerColor, 210);
        scene.overlay().showText(80)
                .text("他一般不用按键就能隔墙看到其他活人（青框示意，只有他看得到）；其他活人的本能一般也看不到他")
                .independent()
                .attachKeyFrame();
        scene.idle(60);
        Actors.walk(scene, fisher, new Vec3d(2.5, 0, 0), 25);
        scene.idle(30);
        Actors.walk(scene, fisher, new Vec3d(0, 0, 3.0), 30);
        scene.overlay().showText(90)
                .text("关着的门也能直接穿过去（活板门、栅栏门不行），也不会和其他玩家相撞")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showText(50)
                .text("……约 7.8 秒后现身（演示拉长了时间）")
                .independent();
        scene.idle(20);
        Actors.invisible(scene, fisher, false);
        scene.idle(40);
        scene.overlay().showText(80)
                .text("结束时如果人还卡在门里，一般会被挪到门外安全的地方")
                .independent();
        scene.idle(90);
        scene.overlay().showText(80)
                .text("再吃一条会重新计时；它和别的鱼共用 1 秒冷却")
                .independent();
        scene.idle(90);
        scene.markAsFinished();
    }

    /**
     * Swordfish (SwordfishItem, SwordfishRules, SwordfishStabService, FisherRules): usable by any living participant of
     * an active round except an active Wraith, a stunned player, one in a Seeker session or one SparkTraits locks; not
     * hidden in hand. Held like the knife (the spear pose, the knife's raise sound everyone near hears), it stabs on
     * release after at least 11 ticks: the player under the aim within 3 blocks, in sight, is killed (an ordinary kill,
     * so protections may stop it) and the attacker swings. Every accepted hit uses the fish up, even one a shield or
     * Tofana stops; a miss does not. A civilian-faction attacker who kills a civilian-faction player usually dies too
     * (an ordinary kill, so a shield can stop it).
     * 剑鱼：进行中的对局里任何存活参与者都能用（激活的冤魂、被电住、处于搜寻者遥控或被 SparkTraits 锁住的人除外）；手持
     * 不隐藏。像刀一样按住（掷矛姿势，附近的人都听得到举刀声），至少 11 tick 后松手刺出：准心对着的 3 格内、看得见的玩
     * 家被杀（普通击杀，保护效果可能挡下），出手者挥一下手。每次被接纳的命中都会用掉剑鱼，哪怕被护盾或托法娜挡下；没刺
     * 中不算。好人阵营的人刺死好人阵营的人，自己一般也会死（普通击杀，护盾能挡）。
     */
    private static void swordfish(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("sparkwitch_swordfish", "剑鱼：一次性的刺杀");
        WatheItemScenes.stage(scene);
        ItemStack swordfish = stack("sparkwitch:swordfish");
        ElementLink<ActorElement> killer = Actors.enter(scene, RoleColors.KILLER, Text.literal("杀手"),
                new Vec3d(2.2, 1, 4.8), SCREEN_LEFT, Direction.DOWN);
        Actors.hold(scene, killer, stack("wathe:knife"));
        scene.idle(5);
        ElementLink<ActorElement> fisher = Actors.enter(scene, RoleColors.of("sparkwitch:fisher", 0x2A9D8F),
                Text.literal("钓鱼佬"), new Vec3d(5.8, 1, 1.2), SCREEN_RIGHT, Direction.DOWN);
        Actors.hold(scene, fisher, swordfish);
        scene.idle(20);
        scene.overlay().showText(90)
                .text("剑鱼是钓鱼佬偶尔钓到的鱼；一般任何活着的玩家都能用（冤魂等除外），拿在手上别人看得见")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        Actors.walk(scene, killer, new Vec3d(0.8, 0, -0.8), 20);
        Actors.walk(scene, fisher, new Vec3d(-0.8, 0, 0.8), 20);
        scene.idle(22);
        Vec3d overHead = new Vec3d(5.0, 3.6, 2.0);
        scene.overlay().showControls(overHead, Pointing.DOWN, 30).rightClick().withItem(swordfish);
        Actors.charge(scene, fisher, true);
        scene.overlay().showText(45)
                .text("像刀一样按住右键蓄力约半秒，附近的人听得到举刀声")
                .independent()
                .attachKeyFrame();
        scene.idle(30);
        Actors.charge(scene, fisher, false);
        Actors.swing(scene, fisher);
        Actors.hold(scene, fisher, ItemStack.EMPTY);
        Actors.fall(scene, killer);
        scene.idle(25);
        scene.overlay().showText(80)
                .colored(PonderPalette.RED)
                .text("松开就刺中准心对着的 3 格内的人：对方一般当场死亡")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        scene.overlay().showText(80)
                .text("只能用一次：刺中就用掉（没刺中不算），哪怕被护盾挡下或被对方的托法娜反杀")
                .independent();
        scene.idle(90);
        Actors.leave(scene, killer, Direction.UP);
        scene.idle(15);
        ElementLink<ActorElement> civilian = Actors.enter(scene, RoleColors.CIVILIAN, Text.literal("平民"),
                new Vec3d(3.0, 1, 4.0), SCREEN_RIGHT, Direction.DOWN);
        Actors.hold(scene, fisher, swordfish);
        scene.idle(15);
        scene.overlay().showText(40)
                .text("可如果刺中的是好人……")
                .independent()
                .attachKeyFrame();
        Actors.charge(scene, fisher, true);
        scene.idle(30);
        Actors.charge(scene, fisher, false);
        Actors.swing(scene, fisher);
        Actors.hold(scene, fisher, ItemStack.EMPTY);
        Actors.fall(scene, civilian);
        scene.idle(1);
        Actors.fall(scene, fisher);
        scene.idle(20);
        scene.overlay().showText(90)
                .colored(PonderPalette.RED)
                .text("……对方照样倒下，你自己一般也会跟着死：好人阵营刺死好人阵营的人就会这样")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.markAsFinished();
    }

    /**
     * USEC suppressor (UsecSuppressorItem, UsecRules, UsecShopRules, UsecAttachmentRules, UsecRifleFireService,
     * UsecModelPredicates; BlindSoundRules): 50 coins in the USEC's shop, one per round, fitted or removed in the
     * rifle's attachment screen (a right-click on the rifle in the inventory). Every shot puts smoke one block ahead of
     * the eyes: without a suppressor 5 puffs and 3 small flames and the loud shot (about 192 blocks), with one 2 puffs,
     * no flame and the suppressed shot (a sharper recording, about 64 blocks, a fixed pitch); the Blind perceive the
     * loud shot at twice their usual perception radius and the suppressed one at half. The rifle model and the
     * shooter's HUD show it fitted. Nothing else reads the suppressor. The demo rifle holds an FMJ in the chamber and
     * one in the magazine.
     * USEC 消音器：USEC 商店 50 金币，每局限 1 个，在步枪的配件界面里装上或拆下（背包里右键步枪打开）。每次开火在眼前一
     * 格处冒烟：没装消音器时 5 团烟和 3 朵小火焰、枪声响亮（约 192 格），装上后 2 团烟、没有火焰、消音枪声（约 64 格，
     * 音调固定）；盲人对响亮枪声的感知半径加倍，对消音枪声减半。步枪模型和射手的弹药界面会显示装着的消音器。除此之外没
     * 有别的东西读取消音器。演示用的步枪膛内和弹匣里各有一发 FMJ。
     */
    private static void usecSuppressor(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("sparkwitch_usec_suppressor", "消音器：压低枪声");
        WatheItemScenes.stage(scene);
        ItemStack loud = rifle(false);
        ItemStack quiet = rifle(true);
        Vec3d feet = new Vec3d(5.4, 1, 1.2);
        Vec3d overHead = feet.add(0, 2.6, 0);
        // UsecRifleFireService.spawnMuzzle: one block ahead of the eyes along the aim. 眼前沿瞄准方向一格。
        Vec3d muzzle = feet.add(0, EYES, 0).add(forward(SCREEN_RIGHT));
        ElementLink<ActorElement> usec = Actors.enter(scene, RoleColors.of("sparkwitch:usec", 0xC8A96A),
                Text.literal("USEC"), feet, SCREEN_RIGHT, Direction.DOWN);
        Actors.hold(scene, usec, loud);
        scene.idle(20);
        scene.overlay().showText(80)
                .text("USEC 能在商店花 50 金币买消音器，每局限 1 个")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        scene.overlay().showControls(overHead, Pointing.DOWN, 30).leftClick().withItem(loud);
        scene.idle(10);
        muzzle(scene, muzzle, false);
        scene.idle(20);
        scene.overlay().showText(90)
                .colored(PonderPalette.RED)
                .text("没装消音器：枪口冒烟还有火光，枪声约 190 格内都听得到")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showControls(overHead, Pointing.DOWN, 60).withItem(stack("sparkwitch:usec_suppressor"));
        scene.overlay().showText(90)
                .text("在背包里右键步枪打开配件界面，就能装上或拆下消音器")
                .independent()
                .attachKeyFrame();
        scene.idle(60);
        Actors.hold(scene, usec, quiet);
        scene.idle(40);
        scene.overlay().showControls(overHead, Pointing.DOWN, 30).leftClick().withItem(quiet);
        scene.idle(10);
        muzzle(scene, muzzle, true);
        scene.idle(20);
        scene.overlay().showText(90)
                .colored(PonderPalette.GREEN)
                .text("装上后：只冒两小团烟、没有火光，枪声只传约 64 格（换成一声更尖的枪声）")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showText(80)
                .text("盲人对没装消音器的枪声感知范围是平时的 2 倍；装了消音器只有平时的一半")
                .independent();
        scene.idle(90);
        scene.overlay().showText(80)
                .text("消音器只改变枪声、枪口烟火和枪的样子，子弹本身不受影响")
                .independent();
        scene.idle(90);
        scene.markAsFinished();
    }

    /**
     * GW-DK, GW-AC and GW-MR shells (PotionShellType, PotionBlastService, PotionBlastResolver, PotionBlastRings,
     * PotionBlastRewards, DkShellEffect, AcShellEffect, MrShellEffect, PotionShellDebuff, PotionShellEntity,
     * PotionBackblastService, PotionGunnerRules): bought for 50, 100 and 125, one loaded at a time in the inventory; a
     * left-click fires with no swing and the shell flies straight at 2.5 blocks a tick, trailing dust in its colour,
     * and bursts on the first player or block with the TR shell's flash, smoke and debris. Only non-witch-faction
     * players whose feet are within the N x N square, whose body overlaps the cube's height and whom a clear line from
     * the burst reaches (feet, body or eyes) are caught; witch allies and the gunner never. Rings by the horizontal
     * Chebyshev distance scale the effect. DK (5): Blindness and Slowness II without particles, 7 s at the centre ring,
     * then about 4.7 s and 2.3 s (at least 1 s). AC (7): each skill and item cooldown gains 20% of its normal length at
     * the centre, then 15, 10 and 5%; a ready one starts a cooldown of that penalty. MR (3): 150 coins at the centre,
     * 75 outside, never below zero, destroyed; the loss is shown to the target alone. The gunner earns 15 per caught
     * enemy. Every shot vents the backblast behind, an ordinary kill of the nearest player within 4 blocks, any
     * faction. The cubes' footprints are drawn on the floor in the shell's colour (clipped to the plate); the boxes on
     * players are illustrations.
     * GW-DK、GW-AC、GW-MR 弹：分别 50、100、125 金币，在背包里一次装一发；左键发射，不挥手，炮弹以每 tick 2.5 格笔直飞
     * 行，拖着本色粉尘，碰到第一个玩家或方块就爆炸，闪光、烟与碎片和 TR 弹相同。只有脚下位于 N × N 正方形内、身体与立方
     * 体高度重叠、且爆心到其脚部、身体或眼睛至少一条线无遮挡的魔女阵营以外玩家被波及，魔女队友和药炮手本人永不。按水平
     * 切比雪夫距离分环衰减。DK（5）：失明与缓慢 II，无粒子，爆心 7 秒，往外约 4.7、2.3 秒（至少 1 秒）。AC（7）：每项技
     * 能和道具冷却追加其正常时长的 20%（爆心），往外 15%、10%、5%；已就绪的进入这段惩罚时长的冷却。MR（3）：爆心扣 150
     * 金币，外圈 75，不会扣成负数，扣掉的直接消失，只有本人看得到。每波及一名敌人，药炮手得 15 金币。每次开火都会向后喷
     * 出尾焰，对身后 4 格内最近的一人进行普通击杀，不分阵营。立方体的地面范围用炮弹本色画在地上（只画底板以内）；玩家身
     * 上的方框是示意。
     */
    private static void potionShells(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("sparkwitch_potion_shells", "药剂炮弹：GW-DK、GW-AC 与 GW-MR");
        WatheItemScenes.stage(scene);
        Text civilian = Text.literal("平民");
        ElementLink<ActorElement> struck = Actors.enter(scene, RoleColors.KILLER, Text.literal("杀手"),
                new Vec3d(2.6, 1, 4.4), SCREEN_LEFT, Direction.DOWN);
        ElementLink<ActorElement> beside = Actors.enter(scene, RoleColors.CIVILIAN, civilian,
                new Vec3d(1.5, 1, 5.4), EAST, Direction.DOWN);
        ElementLink<ActorElement> ally = Actors.enter(scene, RoleColors.of("sparkwitch:accomplice", 0x6B338A),
                Text.literal("共犯"), new Vec3d(4.0, 1, 4.8), WEST, Direction.DOWN);
        ElementLink<ActorElement> far = Actors.enter(scene, RoleColors.CIVILIAN, civilian,
                new Vec3d(5.6, 1, 5.8), NORTH, Direction.DOWN);
        scene.idle(10);
        Vec3d feet = new Vec3d(5.6, 1, 1.4);
        Vec3d overHead = feet.add(0, 2.6, 0);
        ItemStack launcher = stack("sparkwitch:anti_tank_launcher");
        ElementLink<ActorElement> gunner = Actors.enter(scene, RoleColors.of("sparkwitch:potion_gunner", 0xB45CFF),
                Text.literal("药炮手"), feet, SCREEN_RIGHT, Direction.DOWN);
        Actors.hold(scene, gunner, launcher);
        scene.idle(20);
        scene.overlay().showText(90)
                .text("药炮手的另外三种炮弹本身不会炸死人，只影响魔女阵营以外的人")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        // Level aim along the diagonal: the shell bursts where its path enters the killer's box.
        // 沿对角线水平瞄准：炮弹在路径进入杀手碰撞箱处爆炸。
        Vec3d muzzle = feet.add(0, EYES - 0.1, 0);
        Vec3d crowd = new Vec3d(2.9, muzzle.y, 4.1);
        ItemStack dk = stack("sparkwitch:gw_dk_shell");
        scene.overlay().showControls(overHead, Pointing.DOWN, 50).withItem(dk);
        scene.overlay().showText(70)
                .text("GW-DK 弹 50 金币：先在背包里装进炮筒，一次一发")
                .independent()
                .attachKeyFrame();
        scene.idle(80);
        fireShell(scene, gunner, dk, DK_COLOR, muzzle, crowd, SCREEN_RIGHT);
        footprint(scene, crowd, DK_SIZE, DK_COLOR, 200);
        Actors.highlight(scene, struck, DK_COLOR, 200);
        Actors.highlight(scene, beside, DK_COLOR, 200);
        scene.idle(20);
        scene.overlay().showText(90)
                .text("5×5 范围内的敌人失明并获得缓慢 II：爆心 7 秒，往外约 4.7 秒、2.3 秒，没有粒子（框为示意）")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        Actors.highlight(scene, ally, UNAFFECTED_GREEN, 80);
        scene.overlay().showText(80)
                .colored(PonderPalette.GREEN)
                .text("魔女阵营的队友和你自己都不受影响")
                .independent();
        scene.idle(90);
        ItemStack ac = stack("sparkwitch:gw_ac_shell");
        scene.overlay().showControls(overHead, Pointing.DOWN, 40).withItem(ac);
        scene.overlay().showText(50)
                .text("GW-AC 弹 100 金币，范围 7×7")
                .independent()
                .attachKeyFrame();
        scene.idle(60);
        fireShell(scene, gunner, ac, AC_COLOR, muzzle, crowd, SCREEN_RIGHT);
        footprint(scene, crowd, AC_SIZE, AC_COLOR, 200);
        Actors.highlight(scene, struck, AC_COLOR, 200);
        Actors.highlight(scene, beside, AC_COLOR, 200);
        Actors.highlight(scene, far, AC_COLOR, 200);
        scene.idle(20);
        scene.overlay().showText(100)
                .text("范围内敌人的技能和道具冷却被拉长：爆心加正常冷却时长的 20%%，往外依次 15%%、10%%、5%%；没在冷却的也会进入这段冷却")
                .independent()
                .attachKeyFrame();
        scene.idle(110);
        scene.overlay().showText(70)
                .text("冷却只在他们自己的界面上看得出来")
                .independent();
        scene.idle(80);
        ItemStack mr = stack("sparkwitch:gw_mr_shell");
        Actors.turn(scene, gunner, SOUTH);
        scene.overlay().showControls(overHead, Pointing.DOWN, 40).withItem(mr);
        scene.overlay().showText(50)
                .text("GW-MR 弹 125 金币，范围只有 3×3")
                .independent()
                .attachKeyFrame();
        scene.idle(60);
        // Straight south, the shell enters the far passenger's box at z = 5.5.
        // 向正南发射，炮弹在 z = 5.5 处进入远处乘客的碰撞箱。
        Vec3d target = new Vec3d(5.6, muzzle.y, 5.5);
        fireShell(scene, gunner, mr, MR_COLOR, muzzle, target, SOUTH);
        footprint(scene, target, MR_SIZE, MR_COLOR, 160);
        Actors.highlight(scene, far, MR_COLOR, 160);
        scene.idle(20);
        scene.overlay().showText(100)
                .text("范围内的敌人被扣金币：爆心最多 150，外圈 75，不会扣成负数；钱直接消失，只有他们自己看得到")
                .independent()
                .attachKeyFrame();
        scene.idle(110);
        scene.overlay().showText(90)
                .text("每波及一名敌人，你得 15 金币；但开火时身后 4 格内最近的人一般会被尾焰烧死，队友也一样")
                .independent();
        scene.idle(100);
        scene.markAsFinished();
    }

    // ------------------------------------------------------------------ helpers / 辅助

    /**
     * The Fire Poker's push from the holder's feet towards the target's, as the target's client flies it.
     * 烧火棍从持有者脚下指向对方脚下的推力，即被推者客户端实际飞行的速度。
     */
    private static Vec3d pokerLaunch(Vec3d holder, Vec3d target) {
        Vec3d away = new Vec3d(target.x - holder.x, 0, target.z - holder.z).normalize().multiply(POKER_PUSH);
        return new Vec3d(MathHelper.clamp(away.x, -VELOCITY_PACKET_CAP, VELOCITY_PACKET_CAP), POKER_LIFT,
                MathHelper.clamp(away.z, -VELOCITY_PACKET_CAP, VELOCITY_PACKET_CAP));
    }

    /**
     * Knock a standing actor back with {@code launch} (blocks a tick), flying as LivingEntity.travel moves a player
     * with no input, its feet kept inside {@code room} as walls stop a player, for {@code ticks}.
     * 以 launch（每 tick 格数）把站立的演员击退，按 LivingEntity.travel 对无输入玩家的移动方式飞行 ticks，脚像被墙挡住
     * 一样留在 room 之内。
     */
    private static void shove(SceneBuilder scene, ElementLink<ActorElement> actor, Vec3d launch, Box room, int ticks) {
        scene.addInstruction(new Shove(actor, launch, room, ticks));
    }

    /** Wathe's gunshot flash at the muzzle (ShootMuzzleS2CPayload). Wathe 的枪口火光。 */
    private static void gunshotFlash(SceneBuilder scene, Vec3d muzzle) {
        scene.effects().emitParticles(muzzle,
                (world, x, y, z) -> world.addParticle(WatheParticles.GUNSHOT, x, y, z, 0, 0, 0), 1, 1);
    }

    /**
     * Notes over the target's head standing in for a private gift chime (the game shows nothing); a note particle's x
     * motion picks its colour. 用目标头顶的音符示意只有他听得到的赠时钟声（游戏里没有画面）；音符粒子的 x 速度决定颜色。
     */
    private static void chime(SceneBuilder scene, Vec3d at, int stage) {
        Effects.trickle(scene, ParticleTypes.NOTE, at, new Vec3d((stage + 8) / 24.0, 0, 0), 0.75f, 8);
    }

    /**
     * An illustrated edge: an arc of gold dust on the floor {@code radius} blocks around {@code centre}, drawn only
     * over
     * the plate, for {@code ticks}. 示意边界：以 centre 为圆心、半径 radius 格的金色粉尘弧线，只画在底板范围内，持续 ticks。
     */
    private static void edge(SceneBuilder scene, Vec3d centre, double radius, int ticks) {
        scene.effects().emitParticles(centre, (world, x, y, z) -> {
            for (int i = 0; i < EDGE_POINTS; i++) {
                double angle = i * MathHelper.TAU / EDGE_POINTS;
                double px = x + Math.cos(angle) * radius;
                double pz = z + Math.sin(angle) * radius;
                if (px >= 0 && px <= PLATE && pz >= 0 && pz <= PLATE && world.random.nextInt(3) == 0) {
                    world.addParticle(EDGE_DUST, px, y, pz, 0, 0, 0);
                }
            }
        }, 1, ticks);
    }

    /**
     * {@code count} particles as the client plays a server's spawnParticles: a gaussian offset of {@code spread} per
     * axis, a gaussian {@code speed} per axis. 像客户端播放服务端 spawnParticles 那样放出 count 个粒子。
     */
    private static void spawnParticles(SceneBuilder scene, ParticleEffect particle, Vec3d at, int count, double spread,
                                       double speed) {
        scene.effects().emitParticles(at, (world, x, y, z) -> {
            for (int i = 0; i < count; i++) {
                world.addParticle(particle, x + world.random.nextGaussian() * spread,
                        y + world.random.nextGaussian() * spread, z + world.random.nextGaussian() * spread,
                        world.random.nextGaussian() * speed, world.random.nextGaussian() * speed,
                        world.random.nextGaussian() * speed);
            }
        }, 1, 1);
    }

    /**
     * The AXMC's muzzle as UsecRifleFireService.spawnMuzzle shows it to everyone: 5 smoke puffs and 3 small flames, or
     * 2
     * puffs and no flame with a suppressor. 人人看得到的 AXMC 枪口效果：5 团烟和 3 朵小火焰；装消音器时 2 团烟、没有火焰。
     */
    private static void muzzle(SceneBuilder scene, Vec3d at, boolean suppressed) {
        spawnParticles(scene, ParticleTypes.SMOKE, at, suppressed ? 2 : 5, 0.04, 0.01);
        if (!suppressed) {
            spawnParticles(scene, ParticleTypes.SMALL_FLAME, at, 3, 0.03, 0.02);
        }
    }

    /**
     * The demo's AXMC: an FMJ chambered over a magazine holding one more, with or without the suppressor, which the
     * held
     * model shows (UsecModelPredicates). 演示用的 AXMC：膛内一发 FMJ，弹匣里再一发，装或不装消音器，手持模型会显示出来。
     */
    private static ItemStack rifle(boolean suppressor) {
        ItemStack rifle = stack("sparkwitch:usec_rifle");
        NbtList magazine = new NbtList();
        magazine.add(NbtString.of("fmj"));
        NbtCompound state = new NbtCompound();
        state.putString("Chamber", "fmj");
        state.put("Magazine", magazine);
        if (suppressor) {
            state.putBoolean("Suppressor", true);
        }
        NbtCompound data = new NbtCompound();
        data.put("UsecRifle", state);
        rifle.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(data));
        return rifle;
    }

    /**
     * One launcher shot as the Potion Gunner scene shows it: a left-click, the shell flying from {@code muzzle} to
     * {@code impact} at 2.5 blocks a tick with its colour's dust and some smoke, the backblast behind the gunner facing
     * {@code yaw}, and the burst. 一次炮筒射击，与药炮手场景相同：左键，炮弹以每 tick 2.5 格从 muzzle 飞到 impact，拖着
     * 本色粉尘和少许烟，朝向 yaw 的药炮手身后喷出尾焰，然后爆炸。
     */
    private static void fireShell(SceneBuilder scene, ElementLink<ActorElement> gunner, ItemStack shell, int color,
                                  Vec3d muzzle, Vec3d impact, float yaw) {
        Vec3d overHead = new Vec3d(muzzle.x, muzzle.y + 1.1, muzzle.z);
        scene.overlay().showControls(overHead, Pointing.DOWN, 30).leftClick().withItem(
                stack("sparkwitch:anti_tank_launcher"));
        scene.idle(10);
        int flight = Math.max(1, (int) Math.ceil(muzzle.distanceTo(impact) / 2.5));
        DustParticleEffect dust = new DustParticleEffect(
                new Vector3f((color >> 16 & 0xFF) / 255f, (color >> 8 & 0xFF) / 255f, (color & 0xFF) / 255f), 1.0f);
        SparkWitchKillerScenes.sprite(scene, SparkWitchKillerScenes.Sprite.item(shell, muzzle, impact, flight, false));
        SparkWitchKillerScenes.trail(scene, dust, muzzle, impact, flight, 0, 4, 1);
        SparkWitchKillerScenes.trail(scene, ParticleTypes.SMOKE, muzzle, impact, flight, 0, 1, 0.5f);
        SparkWitchKillerScenes.backblast(scene, new Vec3d(muzzle.x, muzzle.y + 0.1, muzzle.z), yaw);
        scene.idle(flight);
        SparkWitchKillerScenes.shellBlast(scene, impact, shell);
    }

    /**
     * A blast cube's footprint as a flat box on the floor in {@code color}, clipped to the plate, for {@code ticks}.
     * 爆炸立方体在地面上的范围，画成 color 色的扁平方框，只画底板以内，持续 ticks。
     */
    private static void footprint(SceneBuilder scene, Vec3d centre, int size, int color, int ticks) {
        double half = size / 2.0;
        Box box = new Box(Math.max(0, centre.x - half), 1.0, Math.max(0, centre.z - half),
                Math.min(PLATE, centre.x + half), 1.05, Math.min(PLATE, centre.z + half));
        scene.addInstruction(new Outline(box, color, ticks));
    }

    /** The horizontal look of {@code yaw}. yaw 的水平朝向。 */
    private static Vec3d forward(float yaw) {
        float radians = yaw * MathHelper.RADIANS_PER_DEGREE;
        return new Vec3d(-MathHelper.sin(radians), 0, MathHelper.cos(radians));
    }

    /**
     * A standing player knocked back with no input (LivingEntity.travel): it moves by its velocity, a wall stopping the
     * blocked axis dead (Entity.move); then the horizontal speed keeps 0.6 x 0.91 if it stood on the floor before that
     * move (so the first tick too), 0.91 if it was in the air, and the vertical one loses 0.08 and keeps 0.98.
     * 无输入的站立玩家被击退（LivingEntity.travel）：先按速度移动，撞墙的那一轴速度归零（Entity.move）；若这一步之前站
     * 在地面上（第一 tick 也是），水平速度保留 0.6 × 0.91，之前在空中则保留 0.91；竖直速度减去 0.08 后保留 0.98。
     */
    private static final class Shove extends TickingInstruction {
        private final ElementLink<ActorElement> link;
        private final Vec3d launch;
        private final Box room;
        private Vec3d at = Vec3d.ZERO;
        private Vec3d velocity = Vec3d.ZERO;
        private double floor;
        private boolean grounded;

        Shove(ElementLink<ActorElement> link, Vec3d launch, Box room, int ticks) {
            super(false, ticks);
            this.link = link;
            this.launch = launch;
            this.room = room;
        }

        @Override
        protected void firstTick(PonderScene scene) {
            super.firstTick(scene);
            ActorElement actor = scene.resolve(link);
            at = actor == null ? Vec3d.ZERO : actor.position();
            floor = at.y;
            velocity = launch;
            grounded = true;
        }

        @Override
        public void tick(PonderScene scene) {
            super.tick(scene);
            ActorElement actor = scene.resolve(link);
            if (actor == null) {
                return;
            }
            double drag = grounded ? 0.6 * 0.91 : 0.91;
            Vec3d next = at.add(velocity);
            double x = MathHelper.clamp(next.x, room.minX, room.maxX);
            double z = MathHelper.clamp(next.z, room.minZ, room.maxZ);
            double vx = x == next.x ? velocity.x : 0;
            double vz = z == next.z ? velocity.z : 0;
            grounded = next.y <= floor;
            at = new Vec3d(x, grounded ? floor : next.y, z);
            velocity = new Vec3d(vx * drag, grounded ? 0 : (velocity.y - 0.08) * 0.98, vz * drag);
            actor.moveTo(at);
        }
    }

    /**
     * A fixed illustrated outline box in {@code color} for {@code ticks}, drawn like Actors.highlight's.
     * 固定位置的示意描边方框，颜色为 color，持续 ticks，画法与 Actors.highlight 相同。
     */
    private static final class Outline extends TickingInstruction {
        private final Box box;
        private final int color;
        private final Object slot = new Object();

        Outline(Box box, int color, int ticks) {
            super(false, ticks);
            this.box = box;
            this.color = color;
        }

        @Override
        public void tick(PonderScene scene) {
            super.tick(scene);
            scene.getOutliner().chaseAABB(slot, box).lineWidth(1 / 16f).colored(color);
        }
    }
}
