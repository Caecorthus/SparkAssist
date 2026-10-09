package dev.caecorthus.sparkassist.client.ponder;

import static dev.caecorthus.sparkassist.client.ponder.SparkPonderDemos.role;
import static dev.caecorthus.sparkassist.client.ponder.SparkPonderDemos.scene;
import static dev.caecorthus.sparkassist.client.ponder.WatheItemScenes.NORTH;
import static dev.caecorthus.sparkassist.client.ponder.WatheItemScenes.SCREEN_LEFT;
import static dev.caecorthus.sparkassist.client.ponder.WatheItemScenes.SCREEN_RIGHT;
import static dev.caecorthus.sparkassist.client.ponder.WatheItemScenes.stack;

import dev.doctor4t.wathe.index.WatheProperties;
import java.util.List;
import net.createmod.catnip.gui.element.ScreenElement;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

/**
 * Demos for SparkWitch's killer, witch and neutral roles, opened only from the role's guide page; each needs
 * SparkWitch. Casts the game shows to nobody (a mark, a theft) are captioned with who can see what, and waits the
 * game takes longer over are shortened, which the captions say.
 * SparkWitch 杀手、魔女与中立阵营职业的演示，只从该职业的指南页打开；都需要 SparkWitch。游戏里谁也看不到的施放
 * （标记、窃取）由字幕说明谁能看到什么；游戏里耗时更长的等待做了缩短，字幕会注明。
 */
final class SparkWitchKillerScenes {
    /** NoellesRoles' shared ability key, which SparkWitch skills reuse. SparkWitch 技能沿用的 NoellesRoles 技能键。 */
    private static final String ABILITY_KEY = "key.noellesroles.ability";
    /** Facing the camera corner (towards small x and small z). 朝向镜头所在的角（x 与 z 都变小的方向）。 */
    private static final float TO_CAMERA = 135;
    /** A standing actor's eyes above the feet. 站立演员的眼睛离脚底的高度。 */
    private static final double EYES = 1.62;
    /** MurderousWitchDeathRayService's particle: bright red dust, scale 1.2, every 0.35 blocks. 死亡射线的粒子：亮红色粉尘，大小 1.2，每 0.35 格一颗。 */
    private static final DustParticleEffect RAY_DUST = new DustParticleEffect(new Vector3f(1.0f, 0.02f, 0.02f), 1.2f);
    private static final double RAY_STEP = 0.35;

    private SparkWitchKillerScenes() {
    }

    static void register() {
        role("sparkassist:roles/sparkwitch/murderous_witch", List.of("sparkwitch"),
                scene("sparkwitch/ray_wall", SparkWitchKillerScenes::murderousWitch));
        role("sparkassist:roles/sparkwitch/saboteur", List.of("sparkwitch"),
                scene("wathe/lit_carriage", SparkWitchKillerScenes::saboteur));
        role("sparkassist:roles/sparkwitch/black_raven", List.of("sparkwitch", "noellesroles"),
                scene("wathe/aisle", SparkWitchKillerScenes::blackRaven));
        role("sparkassist:roles/sparkwitch/time_stealer", List.of("sparkwitch"),
                scene("wathe/aisle", SparkWitchKillerScenes::timeStealer));
    }

    /**
     * Murderous Witch (MurderousWitchDeathRayRules / MurderousWitchDeathRayService, DeathRayAttackMixin, WitchManaRules):
     * mana starts at 0 and regenerates 1 every 2 s. The ability key spends 100 mana and opens a 10 s window with 3
     * shots; each left-click (any held item, no arm swing: the attack is cancelled at its head) fires a red dust ray
     * from the eyes along the aim, up to 12 blocks or the first block. Every player on it is killed (not forced, so a
     * psycho shield still blocks). 60 s cooldown after the window, and at round start.
     * 杀意魔女：魔力开局为 0，每 2 秒恢复 1 点。技能键消耗 100 魔力，开启 10 秒、3 发的窗口；每次左键（手持任何物品，
     * 没有挥手动作：攻击在开头就被取消）从眼睛沿准星射出一道红色粉尘射线，最远 12 格或到第一个方块为止。线上的玩家都会被
     * 击杀（非强制，疯魔护盾仍能挡下）。窗口结束后冷却 60 秒，开局也是。
     */
    private static void murderousWitch(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("role_murderous_witch", "杀意魔女：死亡射线");
        WatheItemScenes.setStage(scene, util);
        Text civilian = Text.literal("平民");
        ElementLink<ActorElement> first = Actors.enter(scene, RoleColors.CIVILIAN, civilian,
                new Vec3d(4.0, 1, 1.4), TO_CAMERA, Direction.DOWN);
        ElementLink<ActorElement> second = Actors.enter(scene, RoleColors.KILLER, Text.literal("杀手"),
                new Vec3d(3.2, 1, 2.2), TO_CAMERA, Direction.DOWN);
        ElementLink<ActorElement> third = Actors.enter(scene, RoleColors.CIVILIAN, civilian,
                new Vec3d(2.5, 1, 2.9), TO_CAMERA, Direction.DOWN);
        ElementLink<ActorElement> sheltered = Actors.enter(scene, RoleColors.CIVILIAN, civilian,
                new Vec3d(0.5, 1, 4.9), NORTH, Direction.DOWN);
        scene.idle(10);
        Vec3d feet = new Vec3d(5.0, 1, 0.4);
        Actors.enter(scene, RoleColors.of("sparkwitch:murderous_witch", 0x7A3857), Text.literal("杀意魔女"), feet,
                SCREEN_RIGHT, Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(80)
                .text("杀意魔女开局 0 魔力，每 2 秒恢复 1 点，杀人还能回魔")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        Vec3d overHead = feet.add(0, 2.6, 0);
        scene.overlay().showControls(overHead, Pointing.DOWN, 40).showing(key(ABILITY_KEY, "G"));
        scene.overlay().showText(80)
                .text("攒够 100 魔力后，按技能键（默认 G）开启死亡射线")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        scene.overlay().showControls(overHead, Pointing.DOWN, 30).leftClick();
        scene.overlay().showText(70)
                .text("开启后 10 秒内能射 3 发：左键开火，手里拿什么都行")
                .independent()
                .attachKeyFrame();
        scene.idle(30);
        // Level aim along the diagonal; the partition's face at x = 2 stops the ray.
        // 沿对角线水平瞄准；隔断墙在 x = 2 处的表面挡住射线。
        Vec3d eyes = feet.add(0, EYES, 0);
        deathRay(scene, eyes, new Vec3d(2.0, eyes.y, 3.4));
        Actors.fall(scene, first);
        Actors.fall(scene, second);
        Actors.fall(scene, third);
        scene.idle(45);
        scene.overlay().showText(80)
                .colored(PonderPalette.RED)
                .text("射线沿准星笔直穿出最远 12 格，线上的人一般当场倒下")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        Actors.highlight(scene, sheltered, 0x7AE04F, 80);
        scene.overlay().showText(80)
                .colored(PonderPalette.GREEN)
                .text("墙会挡住射线，墙后的人没事（绿框示意）")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        scene.overlay().showText(80)
                .text("3 发打完或 10 秒到期后冷却 60 秒；开局也要等 60 秒")
                .independent();
        scene.idle(90);
        scene.overlay().showText(70)
                .text("附近的人都看得见这道红线，也听得到响声")
                .independent();
        scene.idle(80);
        scene.markAsFinished();
    }

    /**
     * Saboteur (SaboteurRules, SaboteurAbilityService, SaboteurLightOutageService, WraithPresence): a killer-side Wraith
     * promoted to Saboteur stays invisible and invulnerable. The ability key darkens every Wathe lamp (a block with
     * both lit and active) whose centre is within 20 blocks for 20 s, then restores it; no blindness, other light
     * sources untouched. 120 s cooldown (60 s right after promotion), used up even with no lamp near. Living killers
     * holding instinct see it outlined orange. Ponder keeps its world fully lit, so the stage dims (StageLights).
     * 破坏者：杀手阵营的冤魂晋升后成为破坏者，仍然隐身、无敌。技能键让中心在 20 格内的所有 Wathe 灯（同时带 lit 与 active
     * 的方块）熄灭 20 秒后恢复；不会致盲，其他光源不受影响。冷却 120 秒（刚晋升时 60 秒），附近没灯也照样冷却。活着的杀手
     * 按住本能键能看到它的橙色轮廓。Ponder 场景始终满亮度，所以由舞台变暗（StageLights）表示黑暗。
     */
    private static void saboteur(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("role_saboteur", "破坏者：熄灭列车灯");
        WatheItemScenes.setStage(scene, util);
        Selection lamps = util.select().position(1, 2, 5).add(util.select().position(4, 2, 5))
                .add(util.select().position(5, 2, 1)).add(util.select().position(5, 2, 4));
        ElementLink<ActorElement> victim = Actors.enter(scene, RoleColors.CIVILIAN, Text.literal("平民"),
                new Vec3d(2.0, 1, 2.4), SCREEN_RIGHT, Direction.DOWN);
        ElementLink<ActorElement> killer = Actors.enter(scene, RoleColors.KILLER, Text.literal("杀手"),
                new Vec3d(5.0, 1, 0.9), SCREEN_RIGHT, Direction.DOWN);
        Actors.hold(scene, killer, stack("wathe:knife"));
        scene.idle(10);
        Vec3d feet = new Vec3d(2.4, 1, 4.6);
        ElementLink<ActorElement> saboteur = Actors.enter(scene, RoleColors.of("sparkwitch:saboteur", 0xE28743),
                Text.literal("破坏者"), feet, SCREEN_RIGHT, Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(80)
                .text("杀手阵营的冤魂晋升后成为破坏者：仍然隐身、无敌")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        scene.overlay().showText(80)
                .text("活着的玩家看不见你（灵界行者除外；演示里把你画出来）")
                .independent();
        scene.idle(90);
        scene.overlay().showControls(feet.add(0, 2.6, 0), Pointing.DOWN, 40).showing(key(ABILITY_KEY, "G"));
        scene.overlay().showText(80)
                .text("按技能键（默认 G）：身边 20 格内的列车灯一起熄灭")
                .independent()
                .attachKeyFrame();
        scene.idle(20);
        scene.world().modifyBlocks(lamps, state -> lamp(state, false), false);
        StageLights.lights(scene, false);
        scene.idle(70);
        scene.overlay().showText(80)
                .text("只是关灯：不会让人失明，火把等其他光源照样亮")
                .independent()
                .attachKeyFrame();
        scene.idle(40);
        Actors.walk(scene, killer, new Vec3d(-1.8, 0, 0.3), 25);
        scene.idle(27);
        Actors.turn(scene, killer, SCREEN_RIGHT);
        scene.idle(6);
        Actors.charge(scene, killer, true);
        scene.idle(15);
        Actors.charge(scene, killer, false);
        Actors.swing(scene, killer);
        Actors.fall(scene, victim);
        scene.overlay().showText(70)
                .text("杀手队友趁黑动手，旁人很难看清")
                .independent()
                .attachKeyFrame();
        scene.idle(80);
        scene.world().modifyBlocks(lamps, state -> lamp(state, true), false);
        StageLights.lights(scene, true);
        scene.overlay().showText(80)
                .text("一般 20 秒后灯自动亮回（演示缩短了时间）")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        scene.overlay().showText(90)
                .text("按下后冷却 120 秒（刚晋升时先等 60 秒）；附近没灯也照样冷却")
                .independent();
        scene.idle(100);
        Actors.highlight(scene, saboteur, RoleColors.of("sparkwitch:saboteur", 0xE28743), 90);
        scene.overlay().showText(90)
                .text("活着的杀手按住本能键，能看到你的橙色轮廓（方框示意）")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.markAsFinished();
    }

    /**
     * Black Raven (FeatherBladeItem, BlackRavenRules, BlackRavenMarkRuntime, BlackRavenInstinctClientHooks,
     * BlackRavenMaskItem, BlackRavenDisguiseRules, ConductorDisguiseAdapter): right-click with the Feather Blade (not
     * hidden; the client returns success, so the arm swings) marks the aimed player within 3 blocks; only the marking
     * Raven sees the outline and the victim is told nothing; 20 s later the victim is killed (knife death, not forced),
     * 60 s cooldown. The Raven Mask (hidden in hand) opens the list of absent good roles; a disguise keeps the look,
     * stashes the blade and hands out that role's kit (the Conductor's master key). A mark already placed still kills.
     * 黑羽鸦：手持羽刃（不隐藏；客户端返回成功，所以会挥手）右键，标记 3 格内准星对准的玩家；只有标记者能看到描边，被标记者
     * 不会收到任何提示；20 秒后被标记者死亡（刀杀，非强制），冷却 60 秒。鸦羽假面（手持隐藏）打开未登场好人身份名单；伪装
     * 不改外观，收起羽刃，发放该身份的道具（列车长的万能钥匙）。已经打出的标记照样致死。
     */
    private static void blackRaven(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("role_black_raven", "黑羽鸦：羽刃标记与伪装");
        WatheItemScenes.stage(scene);
        ElementLink<ActorElement> victim = Actors.enter(scene, RoleColors.CIVILIAN, Text.literal("平民"),
                new Vec3d(2.6, 1, 4.4), SCREEN_RIGHT, Direction.DOWN);
        scene.idle(5);
        int ravenColor = RoleColors.of("sparkwitch:black_raven", 0x51445F);
        ElementLink<ActorElement> raven = Actors.enter(scene, ravenColor, Text.literal("黑羽鸦"),
                new Vec3d(5.8, 1, 1.2), SCREEN_RIGHT, Direction.DOWN);
        Actors.hold(scene, raven, stack("sparkwitch:feather_blade"));
        scene.idle(20);
        scene.overlay().showText(70)
                .text("黑羽鸦开局带着羽刃，拿在手里别人看得见")
                .independent()
                .attachKeyFrame();
        scene.idle(80);
        Actors.walk(scene, raven, new Vec3d(-1.4, 0, 1.4), 22);
        scene.idle(26);
        scene.overlay().showControls(new Vec3d(4.4, 3.6, 2.6), Pointing.DOWN, 30).rightClick()
                .withItem(stack("sparkwitch:feather_blade"));
        scene.idle(10);
        Actors.swing(scene, raven);
        // The outline lasts until the mark settles: the idles until the fall add up to 525 ticks, and the box ends a
        // few ticks early so its fade-out does not take the lying shape.
        // 描边持续到标记结算：直到倒下前的 idle 共 525 tick，方框提前几 tick 结束，免得淡出时变成躺倒的形状。
        Actors.highlight(scene, victim, ravenColor, 520);
        scene.overlay().showText(80)
                .text("准星对准 3 格内的人右键，挥一下手就打上了标记")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        scene.overlay().showText(80)
                .text("描边只有你看得见（方框示意），对方毫无察觉")
                .independent();
        scene.idle(30);
        Actors.walk(scene, raven, new Vec3d(1.4, 0, -1.4), 22);
        scene.idle(60);
        Actors.turn(scene, raven, SCREEN_RIGHT);
        Actors.walk(scene, victim, new Vec3d(-0.8, 0, 0.8), 40);
        scene.overlay().showText(70)
                .text("标记后羽刃一般冷却 60 秒，先离开现场")
                .independent();
        scene.idle(80);
        Actors.hold(scene, raven, stack("sparkwitch:black_raven_mask"));
        scene.idle(10);
        scene.overlay().showControls(new Vec3d(5.8, 3.6, 1.2), Pointing.DOWN, 30).rightClick()
                .withItem(stack("sparkwitch:black_raven_mask"));
        scene.idle(10);
        Actors.swing(scene, raven);
        scene.overlay().showText(90)
                .text("鸦羽假面（其他活人看不见）：右键选一个本局没人拿到的好人身份")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        Actors.hold(scene, raven, stack("noellesroles:master_key"));
        scene.overlay().showText(90)
                .text("外观不变；羽刃收起，换上那个身份的道具，比如列车长的万能钥匙")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showText(40)
                .text("……标记 20 秒后（演示缩短了时间）")
                .independent();
        scene.idle(45);
        Actors.fall(scene, victim);
        scene.overlay().showText(80)
                .colored(PonderPalette.RED)
                .text("目标一般当场倒下，而你已经换了一副身份")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        scene.overlay().showText(80)
                .text("疯魔护盾等保护能挡下这一刀；羽刃不分敌我，别标到队友")
                .independent();
        scene.idle(90);
        scene.markAsFinished();
    }

    /**
     * Time Stealer (TimeStealerClockItem / TimeStealerClockService, TimeStealerRules, TimeTheftRuntime,
     * TimeTheftSlowness, TimeStealerKillService, TimekeeperCounter): the pocket watch is hidden in hand and its use
     * never swings the arm. Aimed at a visible player within 7 blocks it starts a theft (45 s cooldown, a miss is
     * free). Nothing happens for 15 s; then every 5 s a chime only the victim hears and Slowness I-IV without
     * particles; at 35 s a forced kill that shields and psycho mode cannot stop. The round loses 30 s and a living
     * Time Stealer gets a time stamp. A real Timekeeper buying the time reduction lifts every theft.
     * 窃时者：时间怀表手持隐藏，使用时绝不挥手。对准 7 格内看得见的玩家即开始窃取（冷却 45 秒，没打中不算）。前 15 秒毫无
     * 动静；之后每 5 秒一声只有被窃者听得到的钟声，并获得无粒子的缓慢 I～IV；35 秒时强制击杀，护盾和疯魔都挡不住。对局时间
     * 扣 30 秒，活着的窃时者得到 1 枚时光邮票。真正的计时员购买减少时间会解除所有窃取。
     */
    private static void timeStealer(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("role_time_stealer", "窃时者：窃走对方的时间");
        WatheItemScenes.stage(scene);
        ElementLink<ActorElement> victim = Actors.enter(scene, RoleColors.CIVILIAN, Text.literal("平民"),
                new Vec3d(1.8, 1, 5.2), SCREEN_LEFT, Direction.DOWN);
        scene.idle(5);
        Vec3d feet = new Vec3d(5.8, 1, 1.2);
        ElementLink<ActorElement> stealer = Actors.enter(scene, RoleColors.of("sparkwitch:time_stealer", 0x2E8B7A),
                Text.literal("窃时者"), feet, SCREEN_RIGHT, Direction.DOWN);
        Actors.hold(scene, stealer, stack("sparkwitch:time_stealer_pocket_watch"));
        scene.idle(20);
        scene.overlay().showText(80)
                .text("窃时者开局带着时间怀表：其他活人看不见你手里的怀表")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        scene.overlay().showControls(feet.add(0, 2.6, 0), Pointing.DOWN, 30).rightClick()
                .withItem(stack("sparkwitch:time_stealer_pocket_watch"));
        scene.overlay().showText(80)
                .text("对准 7 格内看得见的人右键：窃走他的时间，没有挥手动作")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        scene.overlay().showText(70)
                .text("窃取成功后冷却 45 秒；没对准不进冷却")
                .independent();
        scene.idle(30);
        Actors.walk(scene, stealer, new Vec3d(0.6, 0, -0.6), 15);
        scene.idle(17);
        Actors.turn(scene, stealer, SCREEN_LEFT);
        scene.idle(33);
        Actors.walk(scene, victim, new Vec3d(2.7, 0, 0), 30);
        scene.overlay().showText(60)
                .text("前 15 秒对方毫无察觉，照常走动")
                .independent()
                .attachKeyFrame();
        scene.idle(70);
        scene.overlay().showText(40)
                .text("……15 秒后（演示缩短了时间）")
                .independent();
        scene.idle(45);
        // Each chime starts the next, slower leg of 30 ticks: Slowness I-IV take 15, 30, 45 and 60% off the pace.
        // 每声钟响开始下一段 30 tick 的更慢路程：缓慢 I～IV 分别让步速降低 15%、30%、45%、60%。
        chime(scene, new Vec3d(4.5, 3.2, 5.2), 1);
        scene.overlay().showText(110)
                .text("之后每 5 秒响一次钟（音符示意，只有他自己听得见），一次比一次走得慢")
                .independent()
                .attachKeyFrame();
        Actors.walk(scene, victim, new Vec3d(0, 0, -2.3), 30);
        scene.idle(30);
        chime(scene, new Vec3d(4.5, 3.2, 2.9), 2);
        Actors.walk(scene, victim, new Vec3d(-1.9, 0, 0), 30);
        scene.idle(30);
        chime(scene, new Vec3d(2.6, 3.2, 2.9), 3);
        Actors.walk(scene, victim, new Vec3d(0, 0, 1.5), 30);
        scene.idle(30);
        chime(scene, new Vec3d(2.6, 3.2, 4.4), 4);
        Actors.walk(scene, victim, new Vec3d(1.1, 0, 0), 30);
        scene.idle(30);
        chime(scene, new Vec3d(3.7, 3.2, 4.4), 5);
        Actors.fall(scene, victim);
        scene.overlay().showText(80)
                .colored(PonderPalette.RED)
                .text("第 35 秒时光耗尽，一般当场死亡：护盾和疯魔都挡不住")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        scene.overlay().showControls(new Vec3d(6.4, 3.6, 0.6), Pointing.DOWN, 60)
                .withItem(stack("sparkwitch:time_stamp"));
        scene.overlay().showText(90)
                .text("每次怀表击杀扣 30 秒对局时间；你还活着就得到 1 枚时光邮票")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showText(80)
                .text("但真正的计时员买一次“减少时间”，所有被窃的人都会得救")
                .independent();
        scene.idle(90);
        scene.markAsFinished();
    }

    /**
     * The Death Ray's red dust line from {@code eyes} to where a block stops it, laid out as
     * MurderousWitchDeathRayService.spawnRayParticles does. 死亡射线从 eyes 到被方块挡住处的红色粉尘线，排布与
     * MurderousWitchDeathRayService.spawnRayParticles 相同。
     */
    private static void deathRay(SceneBuilder scene, Vec3d eyes, Vec3d end) {
        Vec3d direction = end.subtract(eyes).normalize();
        double length = eyes.distanceTo(end);
        scene.effects().emitParticles(eyes, (world, x, y, z) -> {
            for (double distance = 0; distance <= length; distance += RAY_STEP) {
                Vec3d point = eyes.add(direction.multiply(distance));
                world.addParticle(RAY_DUST, point.x, point.y, point.z, 0, 0, 0);
            }
            world.addParticle(RAY_DUST, end.x, end.y, end.z, 0, 0, 0);
        }, 1, 1);
    }

    /**
     * Notes over the victim's head standing in for a private chime (the game shows nothing); a note particle's x
     * motion picks its colour. 用受害者头顶的音符示意只有他听得到的钟声（游戏里没有画面）；音符粒子的 x 速度决定颜色。
     */
    private static void chime(SceneBuilder scene, Vec3d at, int stage) {
        Effects.trickle(scene, ParticleTypes.NOTE, at, new Vec3d(stage / 24.0, 0, 0), 0.75f, 8);
    }

    /** A Wathe lamp powered or cut, as SaboteurLightOutageService sets lit and active. 像 SaboteurLightOutageService 那样设置灯的 lit 与 active。 */
    private static BlockState lamp(BlockState state, boolean on) {
        if (!state.contains(Properties.LIT) || !state.contains(WatheProperties.ACTIVE)) {
            return state;
        }
        return state.with(Properties.LIT, on).with(WatheProperties.ACTIVE, on);
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
}
