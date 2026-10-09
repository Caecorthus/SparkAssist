package dev.caecorthus.sparkassist.client.ponder;

import static dev.caecorthus.sparkassist.client.ponder.SparkPonderDemos.role;
import static dev.caecorthus.sparkassist.client.ponder.SparkPonderDemos.scene;
import static dev.caecorthus.sparkassist.client.ponder.WatheItemScenes.NORTH;
import static dev.caecorthus.sparkassist.client.ponder.WatheItemScenes.SCREEN_LEFT;
import static dev.caecorthus.sparkassist.client.ponder.WatheItemScenes.SCREEN_RIGHT;
import static dev.caecorthus.sparkassist.client.ponder.WatheItemScenes.SOUTH;
import static dev.caecorthus.sparkassist.client.ponder.WatheItemScenes.WEST;
import static dev.caecorthus.sparkassist.client.ponder.WatheItemScenes.stack;

import dev.doctor4t.wathe.index.WatheParticles;
import java.util.List;
import net.createmod.catnip.gui.element.ScreenElement;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.element.AnimatedSceneElementBase;
import net.createmod.ponder.foundation.instruction.TickingInstruction;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

/**
 * Demos for SparkWitch's civilian and police roles, opened only from the role's guide page; each needs SparkWitch.
 * What onlookers cannot see (an outline only the role itself sees, the Taser's hit, a bullet's path) is drawn as boxes
 * or lines, and the captions call them illustrations.
 * SparkWitch 平民阵营与警职职业的演示，只从该职业的指南页打开；都需要 SparkWitch。旁观者看不到的东西（只有本人看得到的
 * 轮廓、电击枪的命中、子弹的弹道）用方框或线条表示，字幕注明是示意。
 */
final class SparkWitchCivilianScenes {
    /** NoellesRoles' shared ability key, which SparkWitch skills reuse. SparkWitch 技能沿用的 NoellesRoles 技能键。 */
    private static final String ABILITY_KEY = "key.noellesroles.ability";
    /** A standing actor's eyes above the feet. 站立演员的眼睛离脚底的高度。 */
    private static final double EYES = 1.62;
    /** A crack or flight runs until the scene ends; long enough for any scene. 裂痕或飞行持续到场景结束；对任何场景都足够长。 */
    private static final int UNTIL_STOPPED = 20 * 60 * 10;

    private SparkWitchCivilianScenes() {
    }

    static void register() {
        role("sparkassist:roles/sparkwitch/pig_god", List.of("sparkwitch"),
                scene("wathe/cabin", SparkWitchCivilianScenes::pigGod));
        role("sparkassist:roles/sparkwitch/fisher", List.of("sparkwitch"),
                scene("sparkwitch/civ_tray_cabin", SparkWitchCivilianScenes::fisher));
        role("sparkassist:roles/sparkwitch/usec", List.of("sparkwitch"),
                scene("sparkwitch/civ_sniper_wall", SparkWitchCivilianScenes::usec));
        role("sparkassist:roles/sparkwitch/control_expert", List.of("sparkwitch"),
                scene("wathe/aisle", SparkWitchCivilianScenes::controlExpert));
    }

    /**
     * Pig God (PigGodSkillService, PigGodChaseRuntime, PigGodFeatureService, PigGodRules; Wathe PlayerPsychoComponent,
     * PlayerEntityMixin.attack, PlayerEntityRendererMixin): the ability key spends 150 coins, no cooldown at round
     * start, and starts a 17 s psycho of type VISIBLE_QUIET (the psycho skin everyone sees, no psycho music) with its
     * shield stripped, Speed V (no particles) and a Wathe bat, held in the CROSSBOW_CHARGE pose. A left-click with the
     * bat at full attack charge kills (not forced, so a shield still blocks). From activation, other living players are
     * usually outlined in his colour through walls, to him only (exceptions: a hidden Survival Master, an Obscured or
     * glimmering player, a Fiend in its own colour). A right-click on a small or train
     * door blasts it open for good (DoorBlockEntity.blast, the crowbar's loud pry sound). Killing an effective
     * civilian-faction player during the chase kills him in the same tick, forced (SHOT_INNOCENT). 60 s cooldown after
     * the chase.
     * 皮革噶的：技能键花 150 金币，开局没有冷却，开启 17 秒 VISIBLE_QUIET 类型的疯魔（人人看得到疯魔皮肤，没有疯魔音乐），
     * 去掉疯魔护盾，获得速度 V（无粒子）和一根 Wathe 球棒，以 CROSSBOW_CHARGE 姿势端着。球棒蓄满力左键击杀（非强制，
     * 护盾仍能挡下）。发动起，其他活着的玩家一般都会以他的颜色隔墙高亮，只有他自己看得到（例外：隐藏的生存大师、被遮蔽或
     * 灵光鱼生效的玩家、以自身颜色显示的魔人）。右键小门或车门会把门
     * 轰开且不再关上（DoorBlockEntity.blast，与撬棍相同的响亮撬门声）。追杀中杀死实际阵营为好人的人，自己在同一 tick 被强制
     * 处死（SHOT_INNOCENT）。
     * 追杀结束后冷却 60 秒。
     */
    private static void pigGod(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("role_pig_god", "皮革噶的：皮革追杀");
        WatheItemScenes.cabinStage(scene, util);
        BlockPos door = util.grid().at(3, 1, 3);
        int pink = RoleColors.of("sparkwitch:pig_god", 0xF2A4FC);
        ItemStack bat = stack("wathe:bat");
        ElementLink<ActorElement> civilian = Actors.enter(scene, RoleColors.CIVILIAN, Text.literal("平民"),
                new Vec3d(3.5, 1, 5.0), NORTH, Direction.DOWN);
        ElementLink<ActorElement> killer = Actors.enter(scene, RoleColors.KILLER, Text.literal("杀手"),
                new Vec3d(1.6, 1, 1.4), SCREEN_RIGHT, Direction.DOWN);
        Actors.hold(scene, killer, stack("wathe:knife"));
        scene.idle(10);
        Vec3d feet = new Vec3d(5.8, 1, 1.0);
        ElementLink<ActorElement> pigGod = Actors.enter(scene, pink, Text.literal("皮革噶的"), feet, WEST,
                Direction.DOWN);
        scene.idle(15);
        scene.overlay().showControls(feet.add(0, 2.6, 0), Pointing.DOWN, 40).showing(key(ABILITY_KEY, "G"));
        scene.overlay().showText(80)
                .text("认准凶手后，花 150 金币按技能键（默认 G）发动皮革追杀")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        Actors.psycho(scene, pigGod, true);
        Actors.hold(scene, pigGod, bat);
        // Outlines start with the chase and end with the outlined player's death. 轮廓随追杀开始，随被高亮者死亡而消失。
        Actors.highlight(scene, killer, pink, 218);
        Actors.highlight(scene, civilian, pink, 425);
        scene.overlay().showText(90)
                .text("立刻进入 17 秒疯魔：拿到球棒、获得速度 V，但没有疯魔护盾")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showText(90)
                .text("追杀期间，其他活人一般都以同一颜色隔墙对你高亮（粉框示意）")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        // Speed V: the dash takes a few ticks. 速度 V：几 tick 就冲到。
        Actors.walk(scene, pigGod, new Vec3d(-2.4, 0, 0.3), 8);
        scene.idle(10);
        scene.overlay().showControls(new Vec3d(3.4, 3.6, 1.3), Pointing.DOWN, 30).leftClick().withItem(bat);
        scene.idle(8);
        Actors.swing(scene, pigGod);
        Actors.fall(scene, killer);
        scene.overlay().showText(80)
                .text("球棒蓄满力后左键一击，对方一般当场倒下")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        Actors.leave(scene, killer, Direction.UP);
        scene.idle(15);
        Actors.walk(scene, pigGod, new Vec3d(1.3, 0, 0.6), 6);
        scene.idle(8);
        Actors.turn(scene, pigGod, SCREEN_RIGHT);
        scene.idle(8);
        scene.overlay().showControls(new Vec3d(3.5, 3.6, 3.0), Pointing.DOWN, 30).rightClick().withItem(bat);
        scene.idle(10);
        Actors.swing(scene, pigGod);
        WatheItemScenes.openDoor(scene, door, true);
        scene.overlay().showText(66)
                .text("追杀中右键小门或车门，门会被直接轰开，响声很大")
                .independent()
                .attachKeyFrame();
        scene.idle(30);
        Actors.walk(scene, civilian, new Vec3d(0, 0, -2.6), 26);
        scene.idle(28);
        Actors.walk(scene, civilian, new Vec3d(-1.2, 0, -0.4), 10);
        Actors.turn(scene, pigGod, WEST);
        scene.idle(12);
        scene.overlay().showControls(new Vec3d(4.7, 3.6, 1.9), Pointing.DOWN, 30).leftClick().withItem(bat);
        scene.idle(6);
        Actors.swing(scene, pigGod);
        Actors.fall(scene, civilian);
        // The forced kill in the same tick ends his psycho, so his body wears his own skin. 同一 tick 的强制击杀会结束疯魔，所以尸体是他原本的皮肤。
        Actors.psycho(scene, pigGod, false);
        Actors.fall(scene, pigGod);
        scene.overlay().showText(90)
                .colored(PonderPalette.RED)
                .text("可他是好人：追杀中打死好人阵营的人，你也会当场死亡")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showText(80)
                .text("所以只在确认凶手后再发动；追杀结束后冷却 60 秒")
                .independent();
        scene.idle(90);
        scene.markAsFinished();
    }

    /**
     * Angler (FisherFishingService, FisherInventory, FisherFishUse, FisherFishItem, FisherKeyFishDoors, SparkWitch's
     * NoellesHiddenEquipment list): right-clicking a Wathe drink tray with the rod spends one bait and rolls a catch:
     * a fish into a free hotbar slot, nothing (a skunk) or a pufferfish on the tray; others see one arm swing, the
     * splash sounds are the Angler's own. The demo puts each catch straight in his hand. Edible fish are eaten
     * at once on right-click, with no swing or animation. The rod, bait and edible fish are hidden in hand from other
     * living players; the Key Fish and Swordfish are not. A Key Fish opens a locked small door (or a train door during
     * a round), not a jammed, blasted or open one, and is used up once the doorway opens; the door still closes by
     * itself after 5 s.
     * 钓鱼佬：拿着鱼竿右键 Wathe 饮料托盘，消耗一个鱼饵随机钓一次：鱼放进快捷栏空位，也可能空军或在托盘上钓出河豚；别人只
     * 看到挥一下手，水花声只有自己听得到。演示里渔获直接拿在手上。能吃的鱼右键立刻吃掉，不挥手也没有动画。鱼竿、鱼饵和能吃的鱼对其他活着的玩家隐藏，钥匙鱼和剑鱼不隐藏。
     * 钥匙鱼能打开锁着的小门（对局中也能开车门），打不开被卡住、被撬开或开着的门；门整道打开后钥匙鱼用掉，门照样 5 秒后
     * 自动关上。
     */
    private static void fisher(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("role_fisher", "钓鱼佬：托盘钓鱼与钥匙鱼");
        WatheItemScenes.setStage(scene, util);
        BlockPos door = util.grid().at(3, 1, 3);
        Vec3d tray = new Vec3d(0.5, 3.0, 1.5);
        ItemStack rod = stack("sparkwitch:fishing_rod");
        ItemStack salmon = stack("sparkwitch:salmon");
        ItemStack keyFish = stack("sparkwitch:key_fish");
        ElementLink<ActorElement> fisher = Actors.enter(scene, RoleColors.of("sparkwitch:fisher", 0x2A9D8F),
                Text.literal("钓鱼佬"), new Vec3d(2.9, 1, 1.5), WEST, Direction.DOWN);
        Actors.hold(scene, fisher, rod);
        scene.idle(15);
        scene.overlay().showText(80)
                .text("钓鱼佬开局有一根鱼竿，能在饮料托盘上钓鱼")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        Actors.walk(scene, fisher, new Vec3d(-1.4, 0, 0), 20);
        scene.idle(22);
        scene.overlay().showControls(tray, Pointing.DOWN, 30).rightClick().withItem(rod);
        scene.idle(10);
        Actors.swing(scene, fisher);
        scene.overlay().showText(80)
                .text("拿着鱼竿右键饮料托盘，消耗 1 个鱼饵钓一次")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        scene.overlay().showText(80)
                .text("其他活人看不见你手里的鱼竿，只看到你挥了一下手")
                .independent();
        scene.idle(90);
        Actors.hold(scene, fisher, salmon);
        scene.overlay().showText(90)
                .text("钓到的鱼放进快捷栏：这条鲑鱼右键就吃掉，回复理智")
                .independent()
                .attachKeyFrame();
        scene.idle(40);
        scene.overlay().showControls(new Vec3d(1.5, 3.6, 1.5), Pointing.DOWN, 30).rightClick().withItem(salmon);
        scene.idle(10);
        Actors.hold(scene, fisher, ItemStack.EMPTY);
        scene.idle(50);
        scene.overlay().showText(80)
                .text("鱼竿、鱼饵和能吃的鱼拿在手上，其他活人都看不见")
                .independent();
        scene.idle(90);
        Actors.hold(scene, fisher, rod);
        scene.idle(10);
        scene.overlay().showControls(tray, Pointing.DOWN, 30).rightClick().withItem(rod);
        scene.idle(10);
        Actors.swing(scene, fisher);
        scene.idle(10);
        Actors.hold(scene, fisher, keyFish);
        scene.overlay().showText(80)
                .text("再钓一次：钓到了钥匙鱼，这条别人看得见")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        Actors.walk(scene, fisher, new Vec3d(2.8, 0, 0.6), 28);
        scene.idle(30);
        Actors.turn(scene, fisher, SCREEN_RIGHT);
        scene.idle(8);
        scene.overlay().showControls(new Vec3d(3.5, 3.6, 3.0), Pointing.DOWN, 30).rightClick().withItem(keyFish);
        scene.idle(10);
        Actors.swing(scene, fisher);
        WatheItemScenes.openDoor(scene, door, true);
        Actors.hold(scene, fisher, ItemStack.EMPTY);
        scene.overlay().showText(90)
                .text("右键锁着的门或火车门就能打开，钥匙鱼随即用掉")
                .independent()
                .attachKeyFrame();
        scene.idle(20);
        Actors.walk(scene, fisher, new Vec3d(-0.8, 0, 0.4), 8);
        scene.idle(8);
        Actors.walk(scene, fisher, new Vec3d(0, 0, 2.4), 24);
        scene.idle(72);
        WatheItemScenes.openDoor(scene, door, false);
        scene.overlay().showText(80)
                .text("门照样 5 秒后自动关上；被卡住的门它打不开")
                .independent();
        scene.idle(90);
        scene.overlay().showText(80)
                .text("钓到什么全凭运气：也可能空军，或钓到剑鱼、河豚等")
                .independent();
        scene.idle(90);
        scene.markAsFinished();
    }

    /**
     * USEC (UsecRules, UsecRifleItem, UsecRifleFireService, UsecTracer, UsecImpactRules, UsecShieldPierce,
     * UsecFirePunishment, UsecCooldowns; client UsecRifleArmPoseMixin, UsecImpactClient, UsecCrackTracker): the AXMC
     * starts empty. Onlookers (Blind viewers excepted) see it held two-handed (CROSSBOW_HOLD), hip and scoped alike. Right-click held
     * scopes, left-click fires; locked for 60 s at round start, then a 2 s bolt after each shot. Every shot puts smoke
     * (and a flame without a suppressor) one block ahead of the eyes; there is no visible tracer. FMJ flies straight up
     * to 50 blocks and stops at the first block; AP pierces up to 2 blocks. Debris flies at every entry and exit point
     * and each hit block shows a stage-9 crack (within 25 blocks) for 5 s that then heals a stage a second; no block
     * ever breaks. A hit pierces up to 2 (FMJ) or 5 (AP) shield layers, psycho armour included (Heavy Artillery adds
     * one). Loud: about 192 blocks, 64 with a suppressor. Hitting an innocent is punished (default: the shooter dies too).
     * The rifle carries an FMJ in the chamber and an AP over an FMJ in the magazine, so it fires FMJ, AP, FMJ.
     * USEC：AXMC 开局是空的。手持时旁人（盲人除外）看到的是双手端枪（CROSSBOW_HOLD），腰射与开镜一样。按住右键开镜，左键开火；
     * 开局锁 60 秒，之后每枪拉栓 2 秒。每次开火在眼前一格处冒烟（未装消音器时还有火光）；没有可见的弹道。FMJ 直线飞行
     * 最远 50 格，遇到第一个方块即停；AP 最多穿透 2 个方块。每个入口和出口都飞出碎屑，被打中的方块出现 9 级裂痕（25 格内），
     * 保持 5 秒后每秒恢复一级；方块永远不会坏。命中时可击穿 2 层（FMJ）或 5 层（AP）护盾，含疯魔护甲（重炮手再加 1 层）。
     * 枪声约 192 格，装消音器约 64 格。打中好人会受罚（默认射手一起死）。演示的步枪膛内一发 FMJ，弹匣里 FMJ 上压着 AP，
     * 所以依次打出 FMJ、AP、FMJ。
     */
    private static void usec(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("role_usec", "USEC：狙击、穿墙与破盾");
        WatheItemScenes.setStage(scene, util);
        Crack crack = crack(scene, util.grid().at(3, 2, 1));
        ItemStack rifle = loadedRifle();
        Vec3d feet = new Vec3d(6.0, 1, 1.5);
        Vec3d eyes = feet.add(0, EYES, 0);
        // UsecRifleFireService.spawnMuzzle: one block ahead of the eyes along the aim. 眼前沿瞄准方向一格。
        Vec3d muzzle = eyes.add(-1, 0, 0);
        Vec3d wallFront = new Vec3d(4.0, eyes.y, eyes.z);
        Vec3d wallBack = new Vec3d(3.0, eyes.y, eyes.z);
        ElementLink<ActorElement> hider = Actors.enter(scene, RoleColors.KILLER, Text.literal("杀手"),
                new Vec3d(1.0, 1, 1.5), SOUTH, Direction.DOWN);
        Actors.hold(scene, hider, stack("wathe:knife"));
        scene.idle(10);
        ElementLink<ActorElement> usec = Actors.enter(scene, RoleColors.of("sparkwitch:usec", 0xC8A96A),
                Text.literal("USEC"), feet, WEST, Direction.DOWN);
        Actors.hold(scene, usec, rifle);
        scene.idle(15);
        scene.overlay().showText(80)
                .text("USEC 的狙击步枪开局是空的：弹匣和子弹都要自己买、自己装")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        scene.overlay().showText(60)
                .text("狙击步枪不会被藏起来，拿在手上别人一般都看得到")
                .independent();
        scene.idle(70);
        Vec3d overHead = feet.add(0, 2.6, 0);
        scene.overlay().showControls(overHead, Pointing.DOWN, 30).rightClick().withItem(rifle);
        Actors.charge(scene, usec, true);
        scene.overlay().showText(60)
                .text("按住右键开镜，左键开火；开局 60 秒内不能开火")
                .independent()
                .attachKeyFrame();
        scene.idle(40);
        scene.overlay().showControls(overHead, Pointing.DOWN, 30).leftClick().withItem(rifle);
        scene.idle(10);
        muzzleFlash(scene, muzzle);
        scene.overlay().showLine(PonderPalette.RED, muzzle, wallFront, 25);
        debris(scene, crack.pos, wallFront, 10);
        hit(scene, crack);
        scene.idle(20);
        scene.overlay().showText(80)
                .text("FMJ 弹直线飞行，一般最远 50 格，打不穿方块（红线示意）")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        scene.overlay().showText(80)
                .text("被打中的方块会裂开，5 秒后慢慢消退，方块不会坏")
                .independent();
        scene.idle(90);
        scene.overlay().showText(60)
                .text("每开一枪都要拉栓，一般 2 秒后才能再开……")
                .independent()
                .attachKeyFrame();
        scene.idle(60);
        scene.overlay().showControls(overHead, Pointing.DOWN, 30).leftClick().withItem(rifle);
        scene.idle(10);
        muzzleFlash(scene, muzzle);
        scene.overlay().showLine(PonderPalette.RED, muzzle, new Vec3d(1.0, eyes.y, eyes.z), 15);
        debris(scene, crack.pos, wallFront, 10);
        debris(scene, crack.pos, wallBack, 6);
        hit(scene, crack);
        Actors.fall(scene, hider);
        scene.idle(20);
        scene.overlay().showText(80)
                .colored(PonderPalette.GREEN)
                .text("……这发是 AP 弹：最多能穿透 2 个方块，打中墙后的人")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        ElementLink<ActorElement> psycho = Actors.enter(scene, RoleColors.KILLER, Text.literal("杀手"),
                new Vec3d(1.63, 1, 6.72), SCREEN_LEFT, Direction.DOWN);
        Actors.psycho(scene, psycho, true);
        Actors.hold(scene, psycho, stack("wathe:bat"));
        scene.idle(15);
        // He charges the USEC from the open far side, where the wall hides neither him nor his body.
        // 他从开阔的远侧冲向 USEC，墙既挡不住他，也挡不住他的尸体。
        Actors.walk(scene, psycho, new Vec3d(0.77, 0, -0.92), 16);
        Actors.turn(scene, usec, 40);
        scene.idle(18);
        scene.overlay().showControls(overHead, Pointing.DOWN, 30).leftClick().withItem(rifle);
        scene.idle(6);
        Vec3d aimed = eyes.add(-0.642, 0, 0.767);
        muzzleFlash(scene, aimed);
        scene.overlay().showLine(PonderPalette.RED, aimed, new Vec3d(2.4, eyes.y, 5.8), 15);
        Actors.psycho(scene, psycho, false);
        Actors.fall(scene, psycho);
        scene.idle(20);
        scene.overlay().showText(90)
                .text("子弹一般能击穿 FMJ 2 层、AP 5 层护盾，疯魔护盾也算")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        Actors.charge(scene, usec, false);
        scene.overlay().showText(80)
                .colored(PonderPalette.RED)
                .text("没装消音器时，枪声约 190 格内都听得到；打中好人默认你也会死")
                .independent();
        scene.idle(90);
        scene.markAsFinished();
    }

    /**
     * Control Expert (ControlExpertRules, TaserItem, ControlExpertTaserTargeting, ControlExpertStun,
     * ControlExpertStunGuards, ShockDeviceItem, ShockDeviceEntity, ControlExpertFeatureService): starts with a
     * revolver (not with Impostor); the stun items come from the shop. The Taser hits the nearest affectable player
     * along the aim within 6 blocks (7.8 with Marksman): no swing and no beam, only sounds and the user's own 2-degree
     * recoil of the view; 30 s cooldown, a miss included. A stun lasts 5 s (no moving, attacking,
     * using items or skills; hidden Blindness and Slowness) and cancels a raised knife without a stab. The Shock Device
     * is thrown like a grenade (the hand swings) with a sparse electric-spark trail, and on landing or hitting a player
     * bursts into 40 sparks and stuns every affectable player but the thrower in a 3x3 area, allies too; consumed,
     * 60 s cooldown. Wathe draws no bullet line for the revolver, only its gunshot flash; the demo adds a line and says
     * so.
     * 控场专家：开局带左轮（内鬼除外），电击道具在商店买。电击枪命中准心方向 6 格内（精确枪手 7.8 格）最近的可影响玩家：
     * 不挥手、没有光束，只有声音和使用者自己视角上抬 2 度；冷却 30 秒，打空也算。电击持续 5 秒（不能移动、攻击、使用物品和技能；失明与缓慢不显示粒子），并取消举起的刀而不触发刺击。
     * 电击装置像手雷一样投出（会挥手），沿途零星冒出电火花，落地或砸中玩家时爆出 40 颗电火花，电住 3×3 范围内除投掷者外的
     * 所有可影响的人，队友也算；用一次消耗一个，冷却 60 秒。Wathe 的左轮没有弹道线，只有枪口火光；演示另画了一条线并注明示意。
     */
    private static void controlExpert(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("role_control_expert", "控场专家：先电住，再开枪");
        WatheItemScenes.stage(scene);
        ElementLink<ActorElement> civilian = Actors.enter(scene, RoleColors.CIVILIAN, Text.literal("平民"),
                new Vec3d(1.4, 1, 5.6), SCREEN_LEFT, Direction.DOWN);
        ElementLink<ActorElement> killer = Actors.enter(scene, RoleColors.KILLER, Text.literal("杀手"),
                new Vec3d(3.4, 1, 3.6), SCREEN_RIGHT, Direction.DOWN);
        Actors.hold(scene, killer, stack("wathe:knife"));
        scene.idle(10);
        Vec3d feet = new Vec3d(5.8, 1, 1.2);
        Vec3d overHead = feet.add(0, 2.6, 0);
        ItemStack taser = stack("sparkwitch:taser");
        ItemStack revolver = stack("wathe:revolver");
        ItemStack device = stack("sparkwitch:shock_device");
        ElementLink<ActorElement> expert = Actors.enter(scene, RoleColors.of("sparkwitch:control_expert", 0x4DD0E1),
                Text.literal("控场专家"), feet, SCREEN_RIGHT, Direction.DOWN);
        Actors.hold(scene, expert, taser);
        scene.idle(15);
        scene.overlay().showText(80)
                .text("控场专家开局一般带把左轮，电击道具要在商店买")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        Actors.charge(scene, killer, true);
        scene.overlay().showText(40)
                .text("有人举刀对着你的队友……")
                .independent();
        scene.idle(40);
        scene.overlay().showControls(overHead, Pointing.DOWN, 30).rightClick().withItem(taser);
        scene.idle(8);
        // The Taser shows no beam and no swing, only the user's 2-degree recoil; the line stands in for the hit.
        // 电击枪没有光束也不挥手，只有使用者视角上抬 2 度；线条代表命中。
        Actors.lookPitch(scene, expert, -2);
        scene.overlay().showLine(PonderPalette.INPUT, new Vec3d(5.3, 1.9, 1.7), new Vec3d(3.5, 2.2, 3.5), 8);
        Actors.charge(scene, killer, false);
        scene.overlay().showText(90)
                .text("电击枪电住准心对着的人（一般 6 格内）、打断蓄力（线条示意）")
                .independent()
                .attachKeyFrame();
        scene.idle(50);
        Actors.hold(scene, expert, revolver);
        scene.idle(15);
        scene.overlay().showControls(overHead, Pointing.DOWN, 30).rightClick().withItem(revolver);
        scene.idle(10);
        gunshotFlash(scene, new Vec3d(5.2, 2.05, 1.8));
        WatheItemScenes.shoot(scene, expert, new Vec3d(5.2, 2.05, 1.8), new Vec3d(3.6, 2.0, 3.4));
        Actors.fall(scene, killer);
        scene.idle(22);
        scene.overlay().showText(90)
                .colored(PonderPalette.GREEN)
                .text("被电的人 5 秒内动不了，也不能攻击和用道具，正好补枪")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        Actors.leave(scene, killer, Direction.UP);
        scene.idle(15);
        ElementLink<ActorElement> second = Actors.enter(scene, RoleColors.KILLER, Text.literal("杀手"),
                new Vec3d(3.6, 1, 5.6), NORTH, Direction.DOWN);
        Actors.hold(scene, second, stack("wathe:knife"));
        Actors.hold(scene, expert, device);
        scene.idle(20);
        // Both walk on until the device lands and stuns them. 两人一直走到装置落地把他们电住。
        Actors.walk(scene, civilian, new Vec3d(0.3, 0, -0.7), 20);
        Actors.walk(scene, second, new Vec3d(-0.4, 0, -0.8), 20);
        scene.idle(6);
        scene.overlay().showControls(overHead, Pointing.DOWN, 30).rightClick().withItem(device);
        scene.overlay().showText(90)
                .text("电击装置像手雷一样扔出：落点 3×3 内除你以外一般都会被电")
                .independent()
                .attachKeyFrame();
        scene.idle(4);
        Actors.swing(scene, expert);
        Actors.hold(scene, expert, ItemStack.EMPTY);
        Vec3d landing = new Vec3d(2.45, 1.05, 4.3);
        throwShockDevice(scene, device, new Vec3d(5.5, 2.5, 1.5), landing, 10);
        scene.idle(10);
        // ShockDeviceEntity.shock: 40 sparks, spread 0.75 / 0.5 / 0.75 around 0.5 above the landing, speed 0.1.
        // 40 颗电火花，以落点上方 0.5 处为中心，散布 0.75 / 0.5 / 0.75，速度 0.1。
        spawnParticles(scene, ParticleTypes.ELECTRIC_SPARK, landing.add(0, 0.5, 0), 40, new Vec3d(0.75, 0.5, 0.75),
                0.1);
        scene.idle(90);
        scene.overlay().showText(80)
                .colored(PonderPalette.RED)
                .text("它不分敌我：站在附近的队友也会一起被电")
                .independent();
        scene.idle(90);
        scene.overlay().showText(80)
                .text("电击枪一般冷却 30 秒（打空也算），电击装置 60 秒")
                .independent();
        scene.idle(90);
        scene.markAsFinished();
    }

    /**
     * The demo's AXMC: an FMJ chambered and a magazine of FMJ under AP, so it fires FMJ, AP, FMJ; the fitted magazine
     * also shows on the held model (UsecModelPredicates). 演示用的 AXMC：膛内一发 FMJ，弹匣里 FMJ 上压着 AP，所以依次打出
     * FMJ、AP、FMJ；装上的弹匣也会显示在手持模型上（UsecModelPredicates）。
     */
    private static ItemStack loadedRifle() {
        ItemStack rifle = stack("sparkwitch:usec_rifle");
        // Bottom to top; the top round chambers after each shot (UsecMagazineContents). 自下而上；每次开火后顶部那发上膛。
        NbtList magazine = new NbtList();
        magazine.add(NbtString.of("fmj"));
        magazine.add(NbtString.of("ap"));
        NbtCompound state = new NbtCompound();
        state.putString("Chamber", "fmj");
        state.put("Magazine", magazine);
        NbtCompound data = new NbtCompound();
        data.put("UsecRifle", state);
        rifle.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(data));
        return rifle;
    }

    /**
     * The shot everyone sees at the muzzle (UsecRifleFireService.spawnMuzzle, no suppressor): 5 smoke puffs and 3 small
     * flames. 人人看得到的枪口效果（未装消音器）：5 团烟与 3 朵小火焰。
     */
    private static void muzzleFlash(SceneBuilder scene, Vec3d muzzle) {
        spawnParticles(scene, ParticleTypes.SMOKE, muzzle, 5, new Vec3d(0.04, 0.04, 0.04), 0.01);
        spawnParticles(scene, ParticleTypes.SMALL_FLAME, muzzle, 3, new Vec3d(0.03, 0.03, 0.03), 0.02);
    }

    /**
     * Wathe's gunshot flash, which ShootMuzzleS2CPayload shows everyone at the shooter's muzzle.
     * Wathe 的枪口火光，ShootMuzzleS2CPayload 让所有人在射手枪口处看到。
     */
    private static void gunshotFlash(SceneBuilder scene, Vec3d muzzle) {
        scene.effects().emitParticles(muzzle,
                (world, x, y, z) -> world.addParticle(WatheParticles.GUNSHOT, x, y, z, 0, 0, 0), 1, 1);
    }

    /**
     * Debris of the block at {@code pos} where a bullet meets or leaves it (UsecRifleFireService.spawnDebris: 10 at an
     * entry, 6 at an exit). 子弹进入或离开 pos 处方块时的碎屑（入口 10 颗，出口 6 颗）。
     */
    private static void debris(SceneBuilder scene, BlockPos pos, Vec3d at, int count) {
        scene.effects().emitParticles(at, (world, x, y, z) -> {
            BlockState state = world.getBlockState(pos);
            BlockStateParticleEffect particle = new BlockStateParticleEffect(ParticleTypes.BLOCK, state);
            for (int i = 0; i < count; i++) {
                world.addParticle(particle, x + world.random.nextGaussian() * 0.08,
                        y + world.random.nextGaussian() * 0.08, z + world.random.nextGaussian() * 0.08,
                        world.random.nextGaussian() * 0.15, world.random.nextGaussian() * 0.15,
                        world.random.nextGaussian() * 0.15);
            }
        }, 1, 1);
    }

    /**
     * {@code count} particles as the client plays a server's spawnParticles: each at a gaussian offset of
     * {@code spread} per axis, moving at a gaussian {@code speed} per axis.
     * 像客户端播放服务端 spawnParticles 那样放出 count 个粒子：每轴按 spread 的高斯偏移放置，每轴以高斯 speed 运动。
     */
    private static void spawnParticles(SceneBuilder scene, ParticleEffect particle, Vec3d at, int count, Vec3d spread,
                                       double speed) {
        scene.effects().emitParticles(at, (world, x, y, z) -> {
            for (int i = 0; i < count; i++) {
                world.addParticle(particle, x + world.random.nextGaussian() * spread.x,
                        y + world.random.nextGaussian() * spread.y, z + world.random.nextGaussian() * spread.z,
                        world.random.nextGaussian() * speed, world.random.nextGaussian() * speed,
                        world.random.nextGaussian() * speed);
            }
        }, 1, 1);
    }

    /** Start tracking bullet cracks on the block at {@code pos}. 开始跟踪 pos 处方块上的弹痕。 */
    private static Crack crack(SceneBuilder scene, BlockPos pos) {
        Crack crack = new Crack(pos);
        scene.addInstruction(crack);
        return crack;
    }

    /** A bullet hit within 25 blocks: the deepest crack, stage 9 (UsecImpactRules.crackStage). 25 格内的命中：最深的 9 级裂痕。 */
    private static void hit(SceneBuilder scene, Crack crack) {
        scene.addInstruction(ponder -> crack.hit(Crack.DEEPEST));
    }

    /**
     * The Shock Device's flight: a camera-facing item sprite, as vanilla's FlyingItemEntityRenderer draws a thrown
     * item, on a level throw's arc, gone on landing.
     * 电击装置的飞行：像原版 FlyingItemEntityRenderer 绘制投掷物那样始终朝向镜头的物品图片，沿平抛弧线飞行，落地即消失。
     */
    private static void throwShockDevice(SceneBuilder scene, ItemStack device, Vec3d from, Vec3d to, int ticks) {
        ShockDeviceFlight flight = new ShockDeviceFlight(device, from, to, ticks);
        scene.addInstruction(ponder -> {
            flight.setVisible(true);
            flight.setFade(1);
            ponder.addElement(flight);
        });
    }

    /**
     * A key cap for a control hint, labelled with the key the player has bound to {@code keyId} ({@code fallback} when
     * that binding is missing): Ponder's own hints only draw mouse buttons.
     * 控制提示用的键帽图标，标着玩家给 keyId 绑定的按键（找不到该按键时用 fallback）：Ponder 自带的提示只画鼠标按键。
     */
    private static ScreenElement key(String keyId, String fallback) {
        return (graphics, x, y) -> {
            TextRenderer font = MinecraftClient.getInstance().textRenderer;
            Text label = boundKey(keyId, fallback);
            int width = Math.max(14, font.getWidth(label) + 6);
            graphics.fill(x + 1, y + 1, x + 1 + width, y + 15, 0xFFA0A0A0);
            graphics.fill(x + 2, y + 2, x + width, y + 13, 0xFF3A3A3A);
            graphics.drawText(font, label, x + 1 + (width - font.getWidth(label)) / 2, y + 4, 0xFFFFFFFF, false);
        };
    }

    private static Text boundKey(String keyId, String fallback) {
        for (KeyBinding binding : MinecraftClient.getInstance().options.allKeys) {
            if (binding.getTranslationKey().equals(keyId)) {
                return binding.getBoundKeyLocalizedText();
            }
        }
        return Text.literal(fallback);
    }

    /**
     * One block's visual bullet cracks as UsecCrackTracker keeps them, drawn with Ponder's block-breaking overlay: a
     * hit shows its stage (or the current one when higher) and restarts the hold; the stage holds 5 s, then heals one
     * stage a second and clears after stage 0. Never changes the block.
     * 像 UsecCrackTracker 那样跟踪一个方块上的纯视觉弹痕，用 Ponder 的方块破坏覆盖层绘制：命中时显示其级别（当前级别更高
     * 则保留）并重新开始保持计时；保持 5 秒，之后每秒恢复一级，0 级之后清除。从不改动方块。
     */
    private static final class Crack extends TickingInstruction {
        static final int DEEPEST = 9;
        private static final int CLEAR = -1;
        private static final int HOLD_TICKS = 100;
        private static final int HEAL_STEP_TICKS = 20;
        private final BlockPos pos;
        private int hitStage = CLEAR;
        private int shownStage = CLEAR;
        private int age;

        private Crack(BlockPos pos) {
            super(false, UNTIL_STOPPED);
            this.pos = pos;
        }

        @Override
        public void reset(PonderScene scene) {
            super.reset(scene);
            hitStage = CLEAR;
            shownStage = CLEAR;
            age = 0;
        }

        void hit(int stage) {
            hitStage = Math.max(shownStage, stage);
            age = 0;
        }

        @Override
        public void tick(PonderScene scene) {
            super.tick(scene);
            int stage = hitStage == CLEAR ? CLEAR : stageAt(hitStage, age);
            age++;
            if (stage != shownStage) {
                shownStage = stage;
                // Ponder stores damage - 1 and drops the entry at 0. Ponder 存储 damage - 1，为 0 时移除。
                scene.getWorld().setBlockBreakingProgress(pos, stage + 1);
            }
        }

        /** UsecCrackTracker.stageAt. */
        private static int stageAt(int hitStage, int age) {
            if (age < HOLD_TICKS) {
                return hitStage;
            }
            return Math.max(CLEAR, hitStage - 1 - (age - HOLD_TICKS) / HEAL_STEP_TICKS);
        }
    }

    /**
     * A thrown Shock Device in flight: the item drawn facing the camera on a fixed arc, with ShockDeviceEntity's
     * client trail (an electric spark on about 30% of ticks); scripted, so every replay lands in the same spot.
     * 飞行中的电击装置：沿固定弧线、始终朝向镜头绘制的物品，带有 ShockDeviceEntity 的客户端拖尾（约 30% 的 tick 冒一颗
     * 电火花）；轨迹由脚本决定，每次重播都落在同一处。
     */
    private static final class ShockDeviceFlight extends AnimatedSceneElementBase {
        private static final float TRAIL_CHANCE = 0.3f;
        private final ItemStack stack;
        private final Vec3d from;
        private final Vec3d to;
        private final int ticks;
        /** The arc's rise over the straight line at mid-flight: a level throw. 弧线中点高出直线的高度：平抛。 */
        private final double arc;
        private int age;

        private ShockDeviceFlight(ItemStack stack, Vec3d from, Vec3d to, int ticks) {
            this.stack = stack.copy();
            this.from = from;
            this.to = to;
            this.ticks = Math.max(1, ticks);
            this.arc = (from.y - to.y) / 4;
        }

        @Override
        public void reset(@Nullable PonderScene scene) {
            age = 0;
        }

        @Override
        public void tick(PonderScene scene) {
            age++;
            if (age < ticks && scene.getWorld().random.nextFloat() < TRAIL_CHANCE) {
                Vec3d at = at(age);
                scene.getWorld().addParticle(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 0, 0, 0);
            }
        }

        private Vec3d at(float time) {
            float t = Math.min(1, time / ticks);
            return from.lerp(to, t).add(0, arc * 4 * t * (1 - t), 0);
        }

        @Override
        protected void renderLast(PonderLevel world, VertexConsumerProvider buffer, DrawContext graphics, float fade,
                                  float pt) {
            float time = age + pt;
            if (fade <= 0.01f || time >= ticks) {
                return;
            }
            Vec3d at = at(time);
            MatrixStack ms = graphics.getMatrices();
            ms.push();
            ms.translate(at.x, at.y, at.z);
            // Undo Ponder's camera (its two rotations and the GUI y flip), then flip back so y points up on screen.
            // 抵消 Ponder 的镜头（两次旋转与 GUI 的 y 翻转），再翻转回来，使 y 在屏幕上朝上。
            ms.scale(1, -1, 1);
            if (world.scene != null) {
                PonderScene.SceneTransform transform = world.scene.getTransform();
                ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-transform.yRotation.getValue(pt)));
                ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-transform.xRotation.getValue(pt)));
            }
            ms.scale(1, -1, 1);
            MinecraftClient.getInstance().getItemRenderer().renderItem(stack, ModelTransformationMode.GROUND,
                    lightCoordsFromFade(fade), OverlayTexture.DEFAULT_UV, ms, buffer, world, 0);
            ms.pop();
        }
    }
}
