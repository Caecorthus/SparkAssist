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

import dev.doctor4t.wathe.index.WatheParticles;
import dev.doctor4t.wathe.index.WatheProperties;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.createmod.catnip.gui.element.ScreenElement;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.element.AnimatedSceneElementBase;
import net.createmod.ponder.foundation.element.ElementLinkImpl;
import net.createmod.ponder.foundation.instruction.TickingInstruction;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SculkShriekerBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.EntityEffectParticleEffect;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.SculkChargeParticleEffect;
import net.minecraft.particle.ShriekParticleEffect;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
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
    /**
     * The armed Poison Apple plate's particle (BeveragePlateBlockEntityPoisonAppleParticleMixin): red dust, scale 1.0,
     * rising from 0.2 above the plate on about 4 of every 21 ticks.
     * 布下毒苹果的盘子的粒子：红色粉尘，大小 1.0，从盘子上方 0.2 处升起，约每 21 tick 出现 4 次。
     */
    private static final DustParticleEffect APPLE_DUST = new DustParticleEffect(new Vector3f(1.0f, 0.0f, 0.0f), 1.0f);
    /** Fracture's potion swirl (FractureEffect colour 0x8F6B52, particles shown). 骨折的药水漩涡粒子（FractureEffect 颜色 0x8F6B52，显示粒子）。 */
    private static final EntityEffectParticleEffect FRACTURE_SWIRL =
            EntityEffectParticleEffect.create(ParticleTypes.ENTITY_EFFECT, 0xFF8F6B52);
    /** The grappling hook chain's dark steel link colour (NinjaGrapplingHookEntityRenderer.DARK_STEEL). 钩爪铁链的深色钢链节颜色。 */
    private static final int CHAIN_STEEL = 0x4A4C54;
    /**
     * Where vanilla puts the hand holding a line in third person (FishingBobberEntityRenderer.getHandPos, which
     * NinjaGrapplingHookEntityRenderer ports): 0.45 below the eyes, 0.8 ahead and 0.35 to the side.
     * 原版第三人称下持线手的位置（FishingBobberEntityRenderer.getHandPos，NinjaGrapplingHookEntityRenderer 照搬）：眼睛下方
     * 0.45、前方 0.8、侧向 0.35。
     */
    private static final double HAND_HEIGHT = EYES - 0.45;
    private static final double HAND_REACH = 0.8;
    private static final double HAND_SIDE = 0.35;
    /** A sneaking player's height: a released passenger is set on top of its carrier (Entity.updatePassengerForDismount). 潜行玩家的高度：被放下的乘客会被放到载具的头顶。 */
    private static final double SNEAKING_HEIGHT = 1.5;
    /**
     * Wathe's body is drawn with its feet one block behind the body entity (PlayerBodyEntityRenderer.setupTransforms),
     * and the dragged body entity rides one block behind the Kidnapper (KidnapperPassengerPositioning): its feet
     * trail two blocks behind him, head towards his heels.
     * Wathe 尸体绘制时脚在尸体实体后方一格，被拖的尸体实体又在绑架者身后一格：尸体的脚落后他两格，头朝着他的脚跟。
     */
    private static final double BODY_PIVOT = 1.0;
    private static final double DRAGGED_FEET = 1.0 + BODY_PIVOT;
    /** A tether or chain runs until stopped; long enough for any scene. 拴绳或铁链运行到被停止为止；对任何场景都足够长。 */
    private static final int UNTIL_STOPPED = 20 * 60 * 10;
    /** The TR shell's trail: its colour 0xD8432F as dust of scale 1 (PotionShellEntity.spawnTrail). TR 弹的烟迹：本色 0xD8432F 的粉尘，大小 1。 */
    private static final DustParticleEffect SHELL_TRAIL =
            new DustParticleEffect(new Vector3f(0xD8 / 255f, 0x43 / 255f, 0x2F / 255f), 1.0f);
    /** Half the TR shell's 5-block blast cube (PotionShellType.TR, PotionBlastRings.half). TR 弹 5 格爆炸立方体的一半。 */
    private static final double TR_HALF = 2.5;
    /** The backblast lane and its particle spacing (PotionGunnerRules, PotionBackblastService). 尾焰通道长度与粒子间距。 */
    private static final double BACKBLAST_LENGTH = 4.0;
    private static final double BACKBLAST_STEP = 0.25;
    /** The Shriek Gun's push on an enemy, blocks a tick (AbyssListenerRules.GUN_ENEMY_KNOCKBACK / LIFT). 啸音铳对敌人的推力（每 tick 格数）。 */
    private static final double GUN_PUSH = 2.0;
    private static final double GUN_LIFT = 0.45;
    /** The spore_cabin stage: plate size, the wall's z and the door's x. spore_cabin 舞台：底板大小、墙的 z 与门的 x。 */
    private static final int SPORE_STAGE_SIZE = 7;
    private static final int SPORE_WALL_Z = 3;
    private static final int SPORE_DOOR_X = 3;
    /** Sculk charge events played as a zone appears (DeepDarkZoneCues.SPREAD_EVENTS_PER_TICK). 领域出现时的幽匿充能事件数。 */
    private static final int ZONE_CHARGE_SAMPLES = 12;

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
        role("sparkassist:roles/sparkwitch/witch_maiden", List.of("sparkwitch"),
                scene("sparkwitch/food_platter", SparkWitchKillerScenes::witchMaiden));
        role("sparkassist:roles/sparkwitch/ninja", List.of("sparkwitch"),
                scene("sparkwitch/cargo_ledge", SparkWitchKillerScenes::ninja));
        role("sparkassist:roles/sparkwitch/hunter", List.of("sparkwitch"),
                scene("wathe/aisle", SparkWitchKillerScenes::hunter));
        role("sparkassist:roles/sparkwitch/kidnapper", List.of("sparkwitch"),
                scene("wathe/aisle", SparkWitchKillerScenes::kidnapper));
        role("sparkassist:roles/sparkwitch/grand_witch", List.of("sparkwitch"),
                scene("wathe/aisle", SparkWitchKillerScenes::grandWitch));
        role("sparkassist:roles/sparkwitch/potion_gunner", List.of("sparkwitch"),
                scene("wathe/aisle", SparkWitchKillerScenes::potionGunner));
        role("sparkassist:roles/sparkwitch/abyss_listener", List.of("sparkwitch"),
                scene("sparkwitch/spore_cabin", SparkWitchKillerScenes::abyssListener));
        role("sparkassist:roles/sparkwitch/riftwalker", List.of("sparkwitch"),
                scene("wathe/aisle", SparkWitchKillerScenes::riftwalker));
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
     * Witch Maiden's Poison Apple (FoodPlatterBlockPoisonAppleMixin, PoisonApplePlateService, PoisonApplePlateState,
     * BeveragePlateBlockEntityPoisonAppleParticleMixin, PoisonAppleParticleClientHooks, ItemStackPoisonAppleDrinkMixin):
     * a right-click on a food platter or drink tray with the apple in the main hand uses it up and arms the plate (one
     * apple per plate at a time; the client returns success, so the arm swings). The first portion taken afterwards
     * carries none of the apple's poison, the second carries the Maiden's, which works like a poison vial's: only once
     * eaten or drunk (eating shows crumbs to everyone), death 40-70 s later unless something blocks it (Wathe
     * PoisonUtils, PlayerPoisonComponent). While armed the plate gives off rising red dust that killer roles,
     * spectators and the poison-seeing roles see (Wathe's red-skull set). A vial can poison the same plate; a
     * Toxicologist's ready antidote clears the apple.
     * 巫女的毒苹果：主手拿着毒苹果右键餐盘或饮品托盘，毒苹果用掉，盘子被布下（同一个盘子同时只能有一颗；客户端返回成功，所以
     * 会挥手）。之后第一份被拿走的不带苹果的毒，第二份带着巫女的毒，与毒药瓶的毒相同：吃下或喝下才中毒（吃东西时人人都能
     * 看到碎屑），没被挡下的话 40～70 秒后死亡。布下期间盘子冒出上升的红色粉尘，杀手阵营、旁观者和识毒身份看得到（与 Wathe
     * 的小红骷髅相同）。同一个盘子也能再下毒药瓶；毒理学家用可用的解毒剂能清掉毒苹果。
     */
    private static void witchMaiden(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("role_witch_maiden", "巫女：毒苹果");
        WatheItemScenes.setStage(scene, util);
        Vec3d plate = new Vec3d(3.5, 2.2, 3.5);
        Vec3d overPlate = new Vec3d(3.5, 3.1, 3.5);
        ElementLink<ActorElement> maiden = Actors.enter(scene, RoleColors.of("sparkwitch:witch_maiden", 0xB04A8B),
                Text.literal("巫女"), new Vec3d(6.0, 1, 1.0), SCREEN_RIGHT, Direction.DOWN);
        Actors.hold(scene, maiden, stack("sparkwitch:poison_apple"));
        scene.idle(20);
        scene.overlay().showText(80)
                .text("巫女的毒苹果：拿在手上右键餐盘（饮品托盘也行）")
                .independent()
                .attachKeyFrame();
        scene.idle(46);
        Actors.walk(scene, maiden, new Vec3d(-1.6, 0, 1.6), 25);
        scene.idle(28);
        Actors.lookPitch(scene, maiden, 20);
        scene.overlay().showControls(overPlate, Pointing.DOWN, 30).rightClick()
                .withItem(stack("sparkwitch:poison_apple"));
        scene.idle(10);
        Actors.swing(scene, maiden);
        Actors.hold(scene, maiden, ItemStack.EMPTY);
        Effects.Marker dust = Effects.mark(scene, APPLE_DUST, plate, new Vec3d(0, 0.15, 0), 0.19f);
        scene.overlay().showText(90)
                .colored(PonderPalette.RED)
                .text("毒苹果用掉，盘子冒出红色粉尘：杀手、旁观者和识毒身份看得到")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        Actors.lookPitch(scene, maiden, 0);
        Actors.walk(scene, maiden, new Vec3d(1.6, 0, -1.6), 25);
        scene.idle(25);
        Actors.leave(scene, maiden, Direction.UP);
        Text civilian = Text.literal("平民");
        Vec3d entrance = new Vec3d(1.0, 1, 6.0);
        Vec3d toPlate = new Vec3d(1.6, 0, -1.6);
        ElementLink<ActorElement> first = Actors.enter(scene, RoleColors.CIVILIAN, civilian, entrance, SCREEN_LEFT,
                Direction.DOWN);
        scene.idle(10);
        Actors.walk(scene, first, toPlate, 25);
        scene.idle(28);
        scene.overlay().showControls(overPlate, Pointing.DOWN, 30).rightClick();
        scene.idle(10);
        Actors.swing(scene, first);
        Actors.hold(scene, first, stack("minecraft:bread"));
        scene.overlay().showText(80)
                .colored(PonderPalette.GREEN)
                .text("第一份被拿走的不带苹果的毒，盘子照样冒着红尘")
                .independent()
                .attachKeyFrame();
        scene.idle(50);
        Actors.walk(scene, first, toPlate.multiply(-1), 25);
        scene.idle(25);
        Actors.leave(scene, first, Direction.UP);
        ElementLink<ActorElement> second = Actors.enter(scene, RoleColors.CIVILIAN, civilian, entrance, SCREEN_LEFT,
                Direction.DOWN);
        scene.idle(10);
        Actors.walk(scene, second, toPlate, 25);
        scene.idle(28);
        scene.overlay().showControls(overPlate, Pointing.DOWN, 30).rightClick();
        scene.idle(10);
        Actors.swing(scene, second);
        Actors.hold(scene, second, stack("minecraft:cookie"));
        Effects.unmark(scene, dust);
        scene.overlay().showText(90)
                .colored(PonderPalette.RED)
                .text("第二份才带着毒；它被拿走后，红尘随即消失")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showText(70)
                .text("吃下或喝下才会中毒；这份看起来和别的一样")
                .independent()
                .attachKeyFrame();
        Actors.charge(scene, second, true);
        scene.idle(7);
        crumbs(scene, stack("minecraft:cookie"), new Vec3d(2.6, 1, 4.4), SCREEN_LEFT, 25);
        scene.idle(25);
        Actors.charge(scene, second, false);
        Actors.hold(scene, second, ItemStack.EMPTY);
        scene.idle(45);
        scene.overlay().showText(50)
                .text("……一般 40～70 秒后（演示缩短了时间）")
                .independent();
        Actors.walk(scene, second, new Vec3d(-1.0, 0, -1.0), 30);
        scene.idle(55);
        Actors.fall(scene, second);
        scene.overlay().showText(80)
                .colored(PonderPalette.RED)
                .text("没被解毒或保护效果挡下，就会毒发身亡")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        scene.overlay().showText(90)
                .text("毒苹果能和毒药瓶下在同一个盘子；毒理学家的解毒剂能清掉它")
                .independent();
        scene.idle(100);
        scene.markAsFinished();
    }

    /**
     * Ninja (NinjaKnifeItem, NinjaShurikenItem / NinjaShurikenEntity, NinjaGrapplingHookItem / NinjaGrappleService /
     * NinjaGrapplingHookEntity, NinjaRules, NinjaShopService): every weapon is bought, none is hidden in hand. The Kunai
     * kills the player on the crosshair within 4 blocks at a right-click, with no charge and no swing (the client
     * passes, the server consumes); 30 s cooldown after a kill, never used up. The Shuriken is held (the spear pose)
     * and thrown on release after at least 4 ticks: it flies straight at 1.3 blocks a tick without gravity, a hit
     * player is killed amid a burst of crits; one is used per throw, 1 s cooldown. The Grappling Hook is thrown at a
     * right-click (swing) from the eyes along the aim at 2.5 blocks a tick, latches onto the first block within 24;
     * another right-click within 5 s pulls its holder there at up to 1.4 blocks a tick (a top face puts the feet on
     * the hook point). Its head and chain draw nothing without a real owner, so the scene draws the head sprite
     * itself and a steel line stands in for the chain.
     * 忍者：所有武器都要购买，手持都不隐藏。苦无右键立即击杀准星上 4 格内的玩家，不用蓄力、不挥手（客户端放行、服务端
     * 消耗）；杀人后冷却 30 秒，不会用掉。手里剑按住（掷矛姿势）至少 4 tick 后松手掷出：无重力、每 tick 1.3 格直线飞行，
     * 命中的玩家在暴击粒子中死亡；每次用掉一枚，冷却 1 秒。钩爪右键（挥手）从眼睛沿准星以每 tick 2.5 格抛出，钩住 24 格内
     * 遇到的第一个方块；5 秒内再右键，把持有者以至多每 tick 1.4 格拉过去（钩在顶面时双脚落在钩点）。钩头和铁链没有真实
     * 主人时不绘制，所以由场景自己画出钩头贴图，并用钢色线代替铁链。
     */
    private static void ninja(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("role_ninja", "忍者：苦无、手里剑与钩爪");
        WatheItemScenes.setStage(scene, util);
        Text civilian = Text.literal("平民");
        ElementLink<ActorElement> first = Actors.enter(scene, RoleColors.CIVILIAN, civilian,
                new Vec3d(3.9, 1, 3.1), SCREEN_RIGHT, Direction.DOWN);
        scene.idle(5);
        Vec3d feet = new Vec3d(5.8, 1, 1.2);
        Vec3d overHead = feet.add(0, 2.6, 0);
        ElementLink<ActorElement> ninja = Actors.enter(scene, RoleColors.of("sparkwitch:ninja", 0x2C2C2C),
                Text.literal("忍者"), feet, SCREEN_RIGHT, Direction.DOWN);
        Actors.hold(scene, ninja, stack("sparkwitch:ninja_knife"));
        scene.idle(20);
        scene.overlay().showText(80)
                .text("苦无、手里剑和钩爪都在商店买。苦无：对准 4 格内的人右键")
                .independent()
                .attachKeyFrame();
        scene.idle(50);
        scene.overlay().showControls(overHead, Pointing.DOWN, 30).rightClick()
                .withItem(stack("sparkwitch:ninja_knife"));
        scene.idle(10);
        Actors.fall(scene, first);
        scene.idle(30);
        scene.overlay().showText(80)
                .colored(PonderPalette.RED)
                .text("对方一般当场倒下：不用蓄力，也没有挥手动作")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        scene.overlay().showText(70)
                .text("苦无不会用掉，杀人后一般冷却 30 秒")
                .independent();
        scene.idle(30);
        Actors.leave(scene, first, Direction.UP);
        scene.idle(50);
        Actors.hold(scene, ninja, stack("sparkwitch:ninja_shuriken"));
        ElementLink<ActorElement> second = Actors.enter(scene, RoleColors.CIVILIAN, civilian,
                new Vec3d(2.4, 1, 4.6), SCREEN_LEFT, Direction.DOWN);
        scene.idle(20);
        scene.overlay().showText(80)
                .text("手里剑：按住右键蓄力片刻，松开就直线掷出")
                .independent()
                .attachKeyFrame();
        scene.idle(40);
        scene.overlay().showControls(overHead, Pointing.DOWN, 30).rightClick()
                .withItem(stack("sparkwitch:ninja_shuriken"));
        Actors.charge(scene, ninja, true);
        scene.idle(20);
        Actors.charge(scene, ninja, false);
        Actors.hold(scene, ninja, ItemStack.EMPTY);
        // Spawned 0.1 below the eyes (PersistentProjectileEntity), it meets the target's box after about 4.4 blocks.
        // 在眼睛下方 0.1 处生成（PersistentProjectileEntity），飞约 4.4 格碰到目标的碰撞箱。
        sprite(scene, Sprite.item(stack("sparkwitch:ninja_shuriken"), feet.add(0, EYES - 0.1, 0),
                new Vec3d(2.7, 1 + EYES - 0.1, 4.3), 4, false));
        scene.idle(4);
        Effects.burst(scene, ParticleTypes.CRIT, new Vec3d(2.4, 2.25, 4.6), 10, 0.15);
        Actors.fall(scene, second);
        scene.idle(30);
        scene.overlay().showText(80)
                .colored(PonderPalette.RED)
                .text("命中一般当场死亡；每扔一次用掉一枚，冷却约 1 秒")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        Actors.leave(scene, second, Direction.UP);
        Actors.hold(scene, ninja, stack("sparkwitch:ninja_grappling_hook"));
        scene.idle(20);
        scene.overlay().showText(80)
                .text("钩爪：右键朝准星抛出，能钩住 24 格内的方块")
                .independent()
                .attachKeyFrame();
        scene.idle(30);
        // Aimed 5 degrees down along the diagonal, the hook clears the boxes' corner and lands on their top.
        // 沿对角线向下 5 度瞄准，钩爪越过货箱的棱角，落在货箱顶面。
        Vec3d latch = new Vec3d(1.2, 2.0, 5.8);
        Actors.lookPitch(scene, ninja, 5);
        scene.overlay().showControls(overHead, Pointing.DOWN, 30).rightClick()
                .withItem(stack("sparkwitch:ninja_grappling_hook"));
        scene.idle(10);
        Actors.swing(scene, ninja);
        ElementLink<Sprite> hook = sprite(scene, Sprite.hookHead(feet.add(0, EYES, 0), latch, 3));
        Chain chain = chain(scene, ninja, feet.add(0, EYES, 0), latch, 3);
        scene.idle(45);
        scene.overlay().showText(90)
                .text("钩住后 5 秒内再右键，铁链就把你拉过去（钩头和铁链为示意）")
                .independent()
                .attachKeyFrame();
        scene.idle(40);
        scene.overlay().showControls(overHead, Pointing.DOWN, 30).rightClick()
                .withItem(stack("sparkwitch:ninja_grappling_hook"));
        scene.idle(10);
        Actors.swing(scene, ninja);
        Actors.slide(scene, ninja, latch.subtract(feet), 5);
        scene.idle(5);
        unchain(scene, chain);
        unsprite(scene, hook);
        Actors.lookPitch(scene, ninja, 0);
        scene.idle(45);
        scene.overlay().showText(80)
                .text("铁链声附近都听得到，铁链一般人人看得见")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        scene.overlay().showText(80)
                .text("钩爪每次收回后一般冷却 10 秒；开局约 90 秒内不能用")
                .independent();
        scene.idle(90);
        scene.markAsFinished();
    }

    /**
     * Hunter (HunterTrapItem, HunterTrapEntity, HunterPlayerComponent, FractureEffect, DoubleBarrelShotgunItem,
     * HunterRules): a right-click aimed at the floor sets a trap on the block's top centre (swing); it arms after 0.5 s
     * and lasts 10 min, at most 4 per Hunter. The first living player to touch it is rooted 3 s and usually gains a
     * fracture layer (-20% speed for 60 s, brown swirls everyone sees) amid 8 crits; the trap is used up. The placed trap renders
     * only for killer-side and a few other roles in a live game, so the item lies in for it. The shotgun (a Wathe gun,
     * held levelled, no swing) kills the nearest player on the aim within 8 blocks; 2 shells, 0.2 s between them,
     * 30 s once empty. The shot leaves no trace, so a short red line marks it.
     * 猎人：对着地面右键，在方块顶面中央放下夹子（挥手）；0.5 秒后生效，持续 10 分钟，每人最多 4 个。第一个碰到它的存活玩家
     * 被定身 3 秒，一般还叠一层骨折（减速 20%，持续 60 秒，人人可见的棕色漩涡粒子），伴随 8 颗暴击粒子；夹子随即消失。放下的夹子
     * 只在进行中的对局里对杀手阵营等少数身份绘制，所以用物品代替。双管猎枪（Wathe 枪械，端平持握，不挥手）击杀准星上 8 格内
     * 最近的玩家；2 发子弹，两发间隔 0.2 秒，打空后冷却 30 秒。开枪没有弹道，所以用一道短红线标出。
     */
    private static void hunter(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("role_hunter", "猎人：捕兽夹与双管猎枪");
        WatheItemScenes.stage(scene);
        ElementLink<ActorElement> hunter = Actors.enter(scene, RoleColors.of("sparkwitch:hunter", 0x5C4C34),
                Text.literal("猎人"), new Vec3d(2.5, 1, 1.4), SOUTH, Direction.DOWN);
        Actors.hold(scene, hunter, stack("sparkwitch:hunter_trap"));
        scene.idle(20);
        scene.overlay().showText(80)
                .text("猎人拿着捕兽夹，对着脚前的地面右键放下")
                .independent()
                .attachKeyFrame();
        scene.idle(30);
        Actors.walk(scene, hunter, new Vec3d(0, 0, 1.2), 20);
        scene.idle(22);
        // Looking 40 degrees down from (2.5, 2.6) the aim meets the floor 1.9 blocks ahead: the trap sits on that
        // block's top centre. 从 (2.5, 2.6) 向下 40 度瞄准，准星落在前方 1.9 格的地面：夹子放在那格方块顶面的中央。
        Vec3d trapAt = new Vec3d(2.5, 1.0, 4.5);
        Actors.lookPitch(scene, hunter, 40);
        scene.overlay().showControls(new Vec3d(2.5, 3.6, 2.6), Pointing.DOWN, 30).rightClick()
                .withItem(stack("sparkwitch:hunter_trap"));
        scene.idle(10);
        Actors.swing(scene, hunter);
        ElementLink<ThrownElement> trap = Actors.toss(scene, stack("sparkwitch:hunter_trap"), trapAt.add(0, 0.3, 0),
                trapAt, 2, 0, true, ThrownElement.Flight.LIES_FLAT);
        scene.idle(25);
        scene.overlay().showText(80)
                .text("0.5 秒后生效，能留 10 分钟；每人最多同时放 4 个")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        Actors.lookPitch(scene, hunter, 0);
        scene.overlay().showText(90)
                .text("夹子一般只有杀手阵营等少数人看得见（演示用物品图标代替）")
                .independent()
                .attachKeyFrame();
        scene.idle(30);
        Vec3d shooterFeet = new Vec3d(5.6, 1, 1.6);
        Actors.walk(scene, hunter, shooterFeet.subtract(2.5, 1, 2.6), 30);
        scene.idle(32);
        Actors.hold(scene, hunter, stack("sparkwitch:double_barrel_shotgun"));
        scene.idle(38);
        Vec3d caught = new Vec3d(2.5, 1, 5.4);
        ElementLink<ActorElement> victim = Actors.enter(scene, RoleColors.CIVILIAN, Text.literal("平民"),
                caught.add(0, 0, 1.2), NORTH, Direction.DOWN);
        scene.idle(10);
        // Walking straight at the trap, the player's box meets its trigger box (the trap's 0.5 box grown 0.35 a
        // side) with the feet 0.9 short of its centre. 径直走向夹子时，玩家碰撞箱在脚离夹子中心 0.9 格处碰到触发箱
        // （夹子 0.5 的碰撞箱每边再扩 0.35）。
        Actors.walk(scene, victim, new Vec3d(0, 0, -1.2), 24);
        scene.idle(24);
        Effects.burst(scene, ParticleTypes.CRIT, trapAt.add(0, 0.05, 0), 8, 0.1);
        Actors.remove(scene, trap);
        fractureSwirls(scene, caught, 50);
        float aim = yawTowards(shooterFeet, caught);
        Actors.turn(scene, hunter, aim);
        scene.overlay().showText(70)
                .colored(PonderPalette.RED)
                .text("踩中的人被夹住：定身 3 秒，一般还叠上一层骨折")
                .independent()
                .attachKeyFrame();
        scene.idle(40);
        scene.overlay().showControls(shooterFeet.add(0, 2.6, 0), Pointing.DOWN, 25).rightClick()
                .withItem(stack("sparkwitch:double_barrel_shotgun"));
        scene.idle(10);
        // A levelled gun's muzzle, as WatheItemScenes places the revolver's. 端平的枪口位置，与 WatheItemScenes 的左轮相同。
        WatheItemScenes.shoot(scene, hunter, shooterFeet.add(forward(aim)).add(0, 1.05, 0), caught.add(0, 0.9, 0));
        Actors.fall(scene, victim);
        scene.idle(25);
        scene.overlay().showText(90)
                .text("趁机开枪：射程 8 格，命中一般当场死亡（红线为示意）")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showText(90)
                .text("最多装 2 发，打完一发很快能再打；打空一般冷却 30 秒")
                .independent();
        scene.idle(100);
        scene.overlay().showText(80)
                .text("骨折每层减速两成、持续 60 秒；夹子不分敌我")
                .independent();
        scene.idle(90);
        scene.markAsFinished();
    }

    /**
     * Kidnapper (KnockoutDrugItem, KidnapperControlComponent, KidnapperKnockoutService, KidnapperDragService,
     * KidnapperTargeting, KidnapperPassengerPositionMixin, KidnapperBodyPoseMixin, KidnapperThrowService,
     * KidnapperThrowUseMixin): one knockout drug comes with the role (40 s start lock). Used on a player at hand (swing),
     * it is spent and the target is teleported onto the Kidnapper's position and facing every tick for 30 s: their
     * screen goes black and attack, use and hotbar keys are locked; sneaking releases them. Killing the held target pays
     * 100 coins, but the knife and guns cannot pick a target whose box holds the shooter's eyes (ProjectileUtil
     * .getCollision), so no kill is shown: he lets go and a body from an earlier kill is dragged instead. The ability
     * key mounts the aimed body within 2 blocks: it lies one block behind him (its feet two) facing his way, speed
     * x0.8; pressed again it drops. Sneak plus use throws it (no swing): released on top of his head, it flies with his
     * look x0.8 (at least 0.2 up) under LivingEntity gravity and drag.
     * 绑架者：身份自带一瓶迷药（开局锁 40 秒）。对身边的玩家使用（挥手）后迷药用掉，目标在 30 秒内每 tick 被传送到绑架者的
     * 位置与朝向：黑屏，攻击、使用和快捷栏按键被锁；绑架者潜行即放人。杀死被劫持的人得 100 金币，但刀和枪选不中碰撞箱包住
     * 自己眼睛的目标（ProjectileUtil.getCollision），所以演示不画击杀：他放了人，改拖一具早先留下的尸体。技能键把 2 格内瞄准
     * 的尸体挂到身上：尸体在他身后一格（脚在两格外），朝向跟随他，移速 ×0.8；再按一次放下。潜行加右键把尸体扔出（不挥手）：
     * 尸体先被放到他的头顶，再以视线方向 ×0.8（向上至少 0.2）的速度飞出，受 LivingEntity 的重力与阻力影响。
     */
    private static void kidnapper(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("role_kidnapper", "绑架者：迷药、拖尸与扔尸");
        WatheItemScenes.stage(scene);
        // A body from an earlier kill along the screen-left edge; the drag aims at its entity, one block ahead of the
        // feet. 屏幕左侧边缘躺着早先一次击杀留下的尸体；拖动瞄准的是它的实体，在脚前一格。
        ElementLink<ActorElement> body = Actors.enter(scene, RoleColors.CIVILIAN, null, new Vec3d(5.9, 1, 3.4), NORTH,
                Direction.DOWN);
        Actors.fall(scene, body);
        Text civilian = Text.literal("平民");
        ElementLink<ActorElement> victim = Actors.enter(scene, RoleColors.CIVILIAN, civilian,
                new Vec3d(3.0, 1, 4.0), SCREEN_RIGHT, Direction.DOWN);
        scene.idle(5);
        ElementLink<ActorElement> kidnapper = Actors.enter(scene, RoleColors.of("sparkwitch:kidnapper", 0x9B59B6),
                Text.literal("绑架者"), new Vec3d(1.0, 1, 6.0), SCREEN_LEFT, Direction.DOWN);
        Actors.hold(scene, kidnapper, stack("sparkwitch:knockout_drug"));
        scene.idle(20);
        scene.overlay().showText(80)
                .text("绑架者开局自带 1 瓶迷药（开局约 40 秒内不能用）")
                .independent()
                .attachKeyFrame();
        scene.idle(58);
        Actors.walk(scene, kidnapper, new Vec3d(0.4, 0, -0.4), 10);
        scene.idle(12);
        scene.overlay().showControls(new Vec3d(1.4, 3.6, 5.6), Pointing.DOWN, 30).rightClick()
                .withItem(stack("sparkwitch:knockout_drug"));
        scene.idle(10);
        Actors.swing(scene, kidnapper);
        Actors.hold(scene, kidnapper, ItemStack.EMPTY);
        // The two stand in one spot from now on; one name tag is enough. 此后两人重叠在一处，只留一个名牌。
        Actors.retint(scene, victim, RoleColors.CIVILIAN, null);
        Actors.turn(scene, victim, SCREEN_LEFT);
        Actors.slide(scene, victim, new Vec3d(-1.6, 0, 1.6), 3);
        scene.idle(4);
        Tether held = tether(scene, kidnapper, victim, 0);
        scene.overlay().showText(80)
                .text("对准身边的人右键：用迷药劫持他 30 秒")
                .independent()
                .attachKeyFrame();
        scene.idle(45);
        Actors.walk(scene, kidnapper, new Vec3d(3.0, 0, -3.0), 50);
        scene.idle(40);
        scene.overlay().showText(90)
                .text("他被一直拉到你身上：眼前一片黑，不能攻击或用东西")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showText(80)
                .colored(PonderPalette.RED)
                .text("劫持期间亲手杀了他，额外得 100 金币")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        Actors.sneak(scene, kidnapper, true);
        release(scene, held);
        Actors.retint(scene, victim, RoleColors.CIVILIAN, civilian);
        scene.overlay().showText(80)
                .text("你一按潜行就会放人；30 秒到了也会结束")
                .independent()
                .attachKeyFrame();
        scene.idle(10);
        Actors.walk(scene, victim, new Vec3d(-0.2, 0, 2.0), 30);
        scene.idle(15);
        Actors.sneak(scene, kidnapper, false);
        scene.idle(15);
        Actors.leave(scene, victim, Direction.UP);
        scene.idle(50);
        Actors.turn(scene, kidnapper, EAST);
        Actors.lookPitch(scene, kidnapper, 50);
        scene.idle(10);
        scene.overlay().showControls(new Vec3d(4.4, 3.6, 2.6), Pointing.DOWN, 30).showing(key(ABILITY_KEY, "G"));
        scene.idle(10);
        Tether dragged = tether(scene, kidnapper, body, DRAGGED_FEET);
        scene.overlay().showText(90)
                .text("按技能键（默认 G）拖起 2 格内的尸体，再按一次放下")
                .independent()
                .attachKeyFrame();
        scene.idle(30);
        Actors.lookPitch(scene, kidnapper, 0);
        Actors.turn(scene, kidnapper, SCREEN_RIGHT);
        scene.idle(64);
        Actors.walk(scene, kidnapper, new Vec3d(-0.6, 0, 0.6), 30);
        scene.overlay().showText(70)
                .text("尸体拖在身后，走得慢一些（只剩八成速度）")
                .independent();
        scene.idle(80);
        // Released on top of his sneaking head, the body flies along his look 45 degrees down: 0.57 ahead and the
        // minimum 0.2 up a tick. 尸体先被放到他潜行时的头顶，再沿向下 45 度的视线飞出：每 tick 前进 0.57、上升最低的 0.2。
        Actors.sneak(scene, kidnapper, true);
        Actors.lookPitch(scene, kidnapper, 45);
        scene.idle(10);
        scene.overlay().showControls(new Vec3d(3.8, 3.4, 3.2), Pointing.DOWN, 30).rightClick().whileSneaking();
        scene.idle(10);
        release(scene, dragged);
        throwBody(scene, body, new Vec3d(3.8, 1 + SNEAKING_HEIGHT, 3.2), SCREEN_RIGHT, 45, 20);
        scene.idle(20);
        scene.overlay().showText(90)
                .text("拖着尸体时潜行并右键：把尸体朝你看的方向扔出去")
                .independent()
                .attachKeyFrame();
        scene.idle(30);
        Actors.sneak(scene, kidnapper, false);
        Actors.lookPitch(scene, kidnapper, 0);
        scene.idle(70);
        scene.markAsFinished();
    }

    /**
     * Grand Witch's Ceremonial Sword (GrandWitchActiveSkillService, CeremonialSwordItem, CeremonialSwordCombatService,
     * CeremonialSwordDashService, GrandWitchRules): handed out free on the second completed task and not hidden in hand;
     * held in either hand it adds 40% move speed. A left-click swings (a vanilla attack) and, with the 0.5 s attack
     * charge full and the target inside the sword's +2 reach, kills through most shields. A right-click swings (the
     * client returns success) and dashes 6 blocks along the level look at 1.5 blocks a tick; the first player the swept
     * box touches is killed, ally or not, and the dash stops there or before a wall; 5 s item cooldown. Both kills share
     * a 30 s kill cooldown, during which the dash only moves.
     * 大魔女的仪礼剑：完成第 2 个任务时免费发放，手持不隐藏；主手或副手拿着时移速提高 40%。左键挥剑（原版攻击），攻击蓄力
     * 0.5 秒满、目标在 +2 格的攻击距离内即击杀，能穿透大部分护盾。右键挥手（客户端返回成功）并沿水平视线以每 tick 1.5 格
     * 冲刺 6 格；扫过的碰撞箱碰到的第一名玩家被击杀，不分敌我，冲刺在那里或撞墙前停下；物品冷却 5 秒。两种击杀共用 30 秒
     * 击杀冷却，冷却期间冲刺只能位移。
     */
    private static void grandWitch(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("role_grand_witch", "大魔女：仪礼剑");
        WatheItemScenes.stage(scene);
        Text civilian = Text.literal("平民");
        ElementLink<ActorElement> slashed = Actors.enter(scene, RoleColors.CIVILIAN, civilian,
                new Vec3d(3.9, 1, 3.1), SCREEN_RIGHT, Direction.DOWN);
        scene.idle(5);
        Vec3d feet = new Vec3d(6.0, 1, 1.0);
        ElementLink<ActorElement> witch = Actors.enter(scene, RoleColors.of("sparkwitch:grand_witch", 0xF2DFF7),
                Text.literal("大魔女"), feet, SCREEN_RIGHT, Direction.DOWN);
        Actors.hold(scene, witch, stack("sparkwitch:ceremonial_sword"));
        scene.idle(20);
        scene.overlay().showText(80)
                .text("完成 2 个任务后，大魔女免费获得仪礼剑，别人看得见")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        scene.overlay().showText(80)
                .text("左键斩击：比平时远 2 格，蓄力约 0.5 秒再出手")
                .independent()
                .attachKeyFrame();
        scene.idle(30);
        scene.overlay().showControls(feet.add(0, 2.6, 0), Pointing.DOWN, 30).leftClick()
                .withItem(stack("sparkwitch:ceremonial_sword"));
        scene.idle(10);
        Actors.swing(scene, witch);
        Actors.fall(scene, slashed);
        scene.idle(50);
        scene.overlay().showText(80)
                .colored(PonderPalette.RED)
                .text("命中一般当场倒下，还能穿透大部分护盾")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        // The dash runs along the diagonal behind the first body, so the body lying in front hides no one.
        // 冲刺沿第一具尸体后方的对角线进行，躺在前面的尸体不会挡住任何人。
        Vec3d dashFrom = new Vec3d(6.4, 1, 2.4);
        Actors.walk(scene, witch, dashFrom.subtract(feet), 20);
        ElementLink<ActorElement> fleeing = Actors.enter(scene, RoleColors.CIVILIAN, civilian,
                new Vec3d(3.8, 1, 5.0), SCREEN_RIGHT, Direction.DOWN);
        scene.overlay().showText(90)
                .text("拿着剑移速提高 40%%；斩击和冲刺击杀共用 30 秒冷却")
                .independent();
        scene.idle(22);
        Actors.turn(scene, witch, SCREEN_RIGHT);
        Actors.walk(scene, fleeing, new Vec3d(-0.6, 0, 0.6), 30);
        scene.idle(78);
        scene.overlay().showText(40)
                .text("……30 秒后（演示缩短了时间）")
                .independent();
        scene.idle(45);
        scene.overlay().showControls(dashFrom.add(0, 2.6, 0), Pointing.DOWN, 30).rightClick()
                .withItem(stack("sparkwitch:ceremonial_sword"));
        scene.idle(10);
        Actors.swing(scene, witch);
        // The dash moves in 0.25-block steps, 1.5 blocks a tick, each step first sweeping the box (0.15 wider on every
        // side) over the next step: after 13 steps (3.25 blocks, early in the third tick) the sweep touches the fleeing
        // player and the dash stops there. 冲刺以 0.25 格为一步、每 tick 1.5 格前进，每一步先把碰撞箱（每边宽 0.15）扫过
        // 下一步：走完 13 步（3.25 格，第三个 tick 刚开始）时扫到逃跑的人，冲刺就此停下。
        Actors.slide(scene, witch, new Vec3d(-2.3, 0, 2.3), 3);
        scene.idle(3);
        Actors.fall(scene, fleeing);
        scene.idle(30);
        scene.overlay().showText(90)
                .colored(PonderPalette.RED)
                .text("右键向前冲刺约 6 格：碰到的第一个人一般当场倒下")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showText(90)
                .text("冲刺不分敌我：撞上的是共犯，共犯也会死；撞墙就停")
                .independent();
        scene.idle(100);
        scene.overlay().showText(90)
                .text("冲刺本身冷却 5 秒；击杀冷却中冲刺只能赶路，杀不了人")
                .independent();
        scene.idle(100);
        scene.markAsFinished();
    }

    /**
     * Potion Gunner's Anti-Tank Launcher (PotionLauncherItem, PotionLauncherArmPoseMixin, PotionLauncherFireService,
     * PotionShellEntity, PotionBlastService, PotionBlastResolver, PotionBackblastService, PotionShellType): bound and
     * not hidden in hand, carried two-handed (CROSSBOW_HOLD) whether scoped or not. One shell is loaded in the inventory
     * screen. Holding right-click only scopes (the zoom is the gunner's own screen). A left-click fires with no swing: the
     * shell leaves 0.1 below the eyes at 2.5 blocks a tick, straight for 50 blocks, trailing dust in its colour and
     * smoke, and bursts on the first block or living player (allies too) with Wathe's big flash, smoke and shell
     * debris. TR kills everyone whose feet lie in the 5 x 5 x 5 cube with a line of sight (ordinary kills: shields
     * hold), the gunner and allies included; DK, AC and MR only touch non-allies. Every shot vents flame and smoke
     * 4 blocks straight back from the eyes (cut at the first block) and makes one ordinary kill on the nearest player
     * there, any faction.
     * 药炮手的反坦克炮筒：绑定，手持不隐藏，开不开镜都是双手扛着（CROSSBOW_HOLD）。在背包界面装填一发炮弹。按住右键只是开镜
     * （放大只发生在药炮手自己的画面里）。左键开火，不挥手：炮弹从眼睛下方 0.1 处以每 tick 2.5 格射出，50 格内笔直飞行，拖着
     * 本色粉尘与烟，撞上第一个方块或存活玩家（含队友）就爆炸，伴随 Wathe 的大闪光、烟雾和炮弹碎片。TR 弹击杀脚下位于
     * 5 × 5 × 5 立方体内且视线可达的所有人（普通击杀，护盾能挡），药炮手本人与队友也不例外；DK、AC、MR 弹只影响非队友。每次
     * 开火都会从眼睛向正后方喷出 4 格火焰与烟（遇方块截断），对其中最近的一名玩家进行一次普通击杀，不分阵营。
     */
    private static void potionGunner(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("role_potion_gunner", "药炮手：反坦克炮筒");
        WatheItemScenes.stage(scene);
        Text civilian = Text.literal("平民");
        ElementLink<ActorElement> struck = Actors.enter(scene, RoleColors.CIVILIAN, civilian,
                new Vec3d(2.6, 1, 4.4), SCREEN_LEFT, Direction.DOWN);
        ElementLink<ActorElement> beside = Actors.enter(scene, RoleColors.CIVILIAN, civilian,
                new Vec3d(1.6, 1, 5.4), EAST, Direction.DOWN);
        ElementLink<ActorElement> behind = Actors.enter(scene, RoleColors.CIVILIAN, civilian,
                new Vec3d(4.6, 1, 4.6), EAST, Direction.DOWN);
        ElementLink<ActorElement> ally = Actors.enter(scene, RoleColors.of("sparkwitch:accomplice", 0x6B338A),
                Text.literal("共犯"), new Vec3d(6.6, 1, 0.4), WEST, Direction.DOWN);
        scene.idle(10);
        Vec3d feet = new Vec3d(5.6, 1, 1.4);
        Vec3d overHead = feet.add(0, 2.6, 0);
        ElementLink<ActorElement> gunner = Actors.enter(scene, RoleColors.of("sparkwitch:potion_gunner", 0xB45CFF),
                Text.literal("药炮手"), feet, SCREEN_RIGHT, Direction.DOWN);
        Actors.hold(scene, gunner, stack("sparkwitch:anti_tank_launcher"));
        scene.idle(20);
        scene.overlay().showText(80)
                .text("药炮手双手扛着反坦克炮筒，拿在手上别人看得见")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        scene.overlay().showControls(overHead, Pointing.DOWN, 70).withItem(stack("sparkwitch:tr_shell"));
        scene.overlay().showText(80)
                .text("炮弹要在背包里装进炮筒，一次只能装一发")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        scene.overlay().showControls(overHead, Pointing.DOWN, 40).rightClick()
                .withItem(stack("sparkwitch:anti_tank_launcher"));
        Actors.charge(scene, gunner, true);
        scene.overlay().showText(70)
                .text("按住右键开镜：只有你自己的画面会放大")
                .independent()
                .attachKeyFrame();
        scene.idle(80);
        scene.overlay().showControls(overHead, Pointing.DOWN, 30).leftClick()
                .withItem(stack("sparkwitch:anti_tank_launcher"));
        scene.idle(10);
        // Level aim along the diagonal: the shell meets the first passenger's box (grown 0.3, as vanilla collision does)
        // after about 3.8 blocks and bursts on his body. 沿对角线水平瞄准：炮弹飞约 3.8 格碰到第一个乘客的碰撞箱（与原版碰撞一样
        // 扩大 0.3），在他身上爆炸。
        Vec3d muzzle = feet.add(0, EYES - 0.1, 0);
        Vec3d impact = new Vec3d(2.9, muzzle.y, 4.1);
        ItemStack shell = stack("sparkwitch:tr_shell");
        sprite(scene, Sprite.item(shell, muzzle, impact, 2, false));
        trail(scene, SHELL_TRAIL, muzzle, impact, 2, 0, 4, 1);
        trail(scene, ParticleTypes.SMOKE, muzzle, impact, 2, 0, 1, 0.5f);
        backblast(scene, feet.add(0, EYES, 0), SCREEN_RIGHT);
        Actors.fall(scene, ally);
        scene.idle(2);
        shellBlast(scene, impact, shell);
        Actors.fall(scene, struck);
        Actors.fall(scene, beside);
        Actors.fall(scene, behind);
        blastSquare(scene, impact, TR_HALF, 110);
        scene.idle(20);
        scene.overlay().showText(70)
                .text("左键发射：炮弹笔直飞出，撞上人或墙就爆炸")
                .independent()
                .attachKeyFrame();
        scene.idle(80);
        scene.overlay().showText(80)
                .colored(PonderPalette.RED)
                .text("TR 弹：5×5 范围内的人一般都会被炸死（红框示意）")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        Actors.charge(scene, gunner, false);
        scene.overlay().showText(90)
                .colored(PonderPalette.RED)
                .text("开火时正后方 4 格内最近的人一般会被尾焰烧死，不分敌我")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showText(80)
                .text("爆炸和尾焰都会被墙挡住，开火前最好背靠墙")
                .independent();
        scene.idle(90);
        scene.overlay().showText(80)
                .text("TR 弹连你和队友也炸；另外三种只影响魔女阵营以外的人")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        scene.overlay().showText(90)
                .text("GW-DK 致盲减速，GW-AC 拖长冷却，GW-MR 扣金币")
                .independent();
        scene.idle(100);
        scene.markAsFinished();
    }

    /**
     * Abyss Listener (WardensShriekService, AbyssSuppression, ShriekGunItem, ShriekGunService, ShriekGunRules,
     * DeepDarkSporeFlaskItem / DeepDarkSporeFlaskEntity, DeepDarkZoneShape, DeepDarkZoneEligibility, DeepDarkZoneCues,
     * DeepDarkZoneSchedule, DeepDarkZoneStandingService). The ability key spends 75 mana on the Warden's Shriek: every
     * non-witch-faction participant within 8 blocks (feet to feet, through walls) gets hidden Slowness II for 5 s,
     * cooldowns floored at 5 s and, with real sanity, -40 sanity; all anyone sees is vanilla's shrieker rings at the
     * caster's block. The Shriek Gun (bound, hidden in hand; the client returns success, so the arm swings) hits the
     * first eligible player within 12 blocks (blocks cut it) with a sonic boom ring every block: an enemy is pushed
     * 2.0 blocks a tick with 0.45 lift (with real sanity also -60 sanity, Blindness and Slowness II 2 s, cooldowns
     * floored at 2 s), an ally 3.0 with 0.2 lift and Speed III; 30 s cooldown, misses included. The Spore Flask (shop,
     * visible) is tossed at 0.5 blocks a tick with sculk charge pops; where it lands, shriek rings and sculk souls rise
     * and every solid block next to the air it reaches within 10 blocks turns into a fake sculk (floors) or deepslate
     * mix at once, closed doors and walls stopping it; enemies standing on it get Slowness III and the x15 sanity
     * drain, witch-faction allies and the thrower Speed III. It holds 15 s, then restores outermost first over 5 s.
     * 聆渊者。技能键消耗 75 魔力施放监守之啸：8 格内（脚到脚，可隔墙）所有魔女阵营以外的参与者获得隐藏粒子的缓慢 II 5 秒，
     * 冷却抬高到至少 5 秒，有真实理智的再掉 40 理智；旁人只看到施放者所在方块上原版尖啸体的波纹。啸音铳（绑定，手持隐藏；
     * 客户端返回成功，所以会挥手）命中 12 格内（方块会截断）第一个符合条件的玩家，每格一个音爆圈：敌人以每 tick 2.0 格、
     * 0.45 抬升被推开（有真实理智的另掉 60 理智、失明与缓慢 II 2 秒、冷却至少 2 秒），队友 3.0 格、0.2 抬升并获得速度 III；
     * 冷却 30 秒，打空也算。深暗孢瓶（商店购买，可见）以每 tick 0.5 格抛出，伴随幽匿充能爆裂；落地处升起尖啸波纹与幽匿之魂，
     * 它在 10 格内触及的空气旁的每个实心方块立即变成假的幽匿块（地面）或深板岩混合外观，墙与关着的门会挡住它；站在上面的敌人
     * 获得缓慢 III 与 15 倍理智下降，魔女阵营队友和投掷者获得速度 III。领域保持 15 秒，再用 5 秒由外向内恢复。
     */
    private static void abyssListener(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("role_abyss_listener", "聆渊者：尖啸、啸音铳与深暗孢瓶");
        WatheItemScenes.setStage(scene, util);
        Text civilian = Text.literal("平民");
        ElementLink<ActorElement> corridor = Actors.enter(scene, RoleColors.CIVILIAN, civilian,
                new Vec3d(2.9, 1, 1.5), EAST, Direction.DOWN);
        ElementLink<ActorElement> cabin = Actors.enter(scene, RoleColors.CIVILIAN, civilian,
                new Vec3d(4.5, 1, 5.0), NORTH, Direction.DOWN);
        scene.idle(5);
        int listenerColor = RoleColors.of("sparkwitch:abyss_listener", 0x0B5E78);
        Vec3d feet = new Vec3d(6.6, 1, 1.5);
        Vec3d overHead = feet.add(0, 2.6, 0);
        ElementLink<ActorElement> listener = Actors.enter(scene, listenerColor, Text.literal("聆渊者"), feet, WEST,
                Direction.DOWN);
        scene.idle(20);
        scene.overlay().showControls(overHead, Pointing.DOWN, 40).showing(key(ABILITY_KEY, "G"));
        scene.overlay().showText(70)
                .text("监守之啸：按技能键（默认 G），消耗 75 魔力")
                .independent()
                .attachKeyFrame();
        scene.idle(15);
        shriek(scene, BlockPos.ofFloored(feet));
        scene.idle(60);
        Actors.highlight(scene, corridor, listenerColor, 80);
        Actors.highlight(scene, cabin, listenerColor, 80);
        scene.overlay().showText(80)
                .text("8 格内魔女阵营以外的人一般都会中招，隔墙也一样（方框示意）")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        scene.overlay().showText(80)
                .text("缓慢 II 5 秒、道具和技能至少冷却 5 秒，有理智的还掉 40 理智")
                .independent();
        scene.idle(90);
        Actors.hold(scene, listener, stack("sparkwitch:shriek_gun"));
        scene.idle(10);
        scene.overlay().showText(80)
                .text("啸音铳（其他活人看不见）：右键击中 12 格内的第一个人")
                .independent()
                .attachKeyFrame();
        scene.idle(30);
        scene.overlay().showControls(overHead, Pointing.DOWN, 30).rightClick()
                .withItem(stack("sparkwitch:shriek_gun"));
        scene.idle(10);
        Actors.swing(scene, listener);
        // The beam meets the passenger's box (grown 0.2) 3.2 blocks from the eyes: three rings, at 1, 2 and 3 blocks.
        // 射线在离眼睛 3.2 格处碰到乘客的碰撞箱（扩大 0.2）：三个音爆圈，分别在 1、2、3 格处。
        sonicBeam(scene, feet.add(0, EYES, 0), new Vec3d(-1, 0, 0), 3.2);
        fling(scene, corridor, new Vec3d(-GUN_PUSH, GUN_LIFT, 0), 1, 3);
        scene.idle(3);
        Actors.leave(scene, corridor, Direction.WEST);
        scene.idle(22);
        scene.overlay().showText(80)
                .text("被打中的人被猛地击飞；有理智的敌人还会失明、缓慢 2 秒")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        scene.overlay().showText(70)
                .text("推下火车算你的击杀；冷却 30 秒，打空也算")
                .independent();
        scene.idle(80);
        Actors.hold(scene, listener, stack("sparkwitch:deep_dark_spore_flask"));
        Actors.lookPitch(scene, listener, 20);
        scene.overlay().showText(80)
                .text("深暗孢瓶：右键扔出，落点周围瞬间变成深暗领域")
                .independent()
                .attachKeyFrame();
        scene.idle(20);
        scene.overlay().showControls(overHead, Pointing.DOWN, 30).rightClick()
                .withItem(stack("sparkwitch:deep_dark_spore_flask"));
        scene.idle(10);
        Actors.swing(scene, listener);
        Actors.hold(scene, listener, ItemStack.EMPTY);
        // Thrown 20 degrees down at 0.5 blocks a tick (drag 0.99, gravity 0.03), the flask drops 1.5 blocks in about 6
        // ticks and breaks on the floor 2.85 blocks ahead; the zone grows from the air cell above. 向下 20 度、每 tick
        // 0.5 格抛出（阻力 0.99、重力 0.03），孢瓶约 6 tick 下落 1.5 格，碎在前方 2.85 格的地面上；领域从其上方的空气格开始。
        Vec3d flaskFrom = feet.add(0, EYES - 0.1, 0);
        Vec3d landing = new Vec3d(3.75, 1.0, 1.5);
        Actors.toss(scene, stack("sparkwitch:deep_dark_spore_flask"), flaskFrom, landing, 6, 0.1, false,
                ThrownElement.Flight.UPRIGHT);
        trail(scene, ParticleTypes.SCULK_CHARGE_POP, flaskFrom, landing, 6, 0.1, 1, 0.3f);
        scene.idle(6);
        BlockPos landingCell = BlockPos.ofFloored(landing);
        sporeLanding(scene, landingCell);
        sporeZone(scene, landingCell, true, 0, Double.MAX_VALUE);
        Actors.lookPitch(scene, listener, 0);
        scene.idle(50);
        Actors.highlight(scene, cabin, 0x7AE04F, 80);
        scene.overlay().showText(80)
                .colored(PonderPalette.GREEN)
                .text("墙和关着的门会挡住它，门后的房间不受影响")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        ElementLink<ActorElement> walker = Actors.enter(scene, RoleColors.CIVILIAN, civilian,
                new Vec3d(0.5, 1, 0.7), EAST, Direction.DOWN);
        scene.idle(10);
        // Slowness III takes 45% off an enemy's pace, Speed III adds 60% to the listener's.
        // 缓慢 III 让敌人步速降低 45%，速度 III 让聆渊者步速提高 60%。
        Actors.walk(scene, walker, new Vec3d(2.0, 0, 0), 45);
        Actors.walk(scene, listener, new Vec3d(-2.4, 0, 0), 18);
        scene.overlay().showText(80)
                .text("站上去的敌人缓慢 III，有理智的狂掉理智；你和队友反而加速")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        scene.overlay().showText(70)
                .text("……约 15 秒后，领域由外向内退去（演示缩短了时间）")
                .independent()
                .attachKeyFrame();
        scene.idle(30);
        // Restored outermost first, as DeepDarkZoneSchedule.restoreOffset orders them. 按 restoreOffset 由外向内恢复。
        sporeZone(scene, landingCell, false, 3.0, Double.MAX_VALUE);
        scene.idle(6);
        sporeZone(scene, landingCell, false, 2.0, 3.0);
        scene.idle(6);
        sporeZone(scene, landingCell, false, 0, 2.0);
        scene.idle(50);
        scene.markAsFinished();
    }

    /**
     * Riftwalker's Rift Gates (RiftGateItem, RiftGatePlacementService / RiftGatePlacementRules, RiftGateEntityRenderer,
     * RiftGateClientEffects, RiftSessionService, RiftSessionBody, RiftExitSearch, RiftSessionClient, RiftwalkerRules):
     * the gate item is bought for 50 mana and hidden in hand; a right-click places a gate at the feet (no swing: both
     * sides answer CONSUME), its front facing the look, its width centred on the block, with a burst of portal and witch
     * particles; everyone sees the gate. A right-click on a gate within 3 blocks takes a witch-faction user inside: a
     * reverse-portal puff and the body turns spectator (gone for everyone else). A/D hop silently between gates; Shift
     * leaves one block in front of the current gate, keeping the look, with another puff. Stay: 30 s for the
     * Riftwalker, 20 s for the witch faction, 15 s / 10 s for the Murderous / Apprentice Witch; re-entry waits 30 s /
     * 45 s.
     * 隙行者的裂隙门：门以 50 魔力购买，手持隐藏；右键把门放在脚下（不挥手：两端都返回 CONSUME），正面朝视线方向，宽度方向
     * 对齐方块中心，伴随传送门与女巫粒子；所有人都看得见门。对 3 格内的门右键，魔女阵营的使用者即进入门内：一团反向传送门
     * 粒子，本体变为旁观模式（其他人都看不见）。A/D 无声地在门之间跳转；Shift 从当前这扇门正前方一格出来，保持视线方向，
     * 同样有一团粒子。停留上限：隙行者 30 秒，魔女阵营 20 秒，杀意 / 预备魔女 15 / 10 秒；再次进门要等 30 / 45 秒。
     */
    private static void riftwalker(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("role_riftwalker", "隙行者：裂隙门");
        WatheItemScenes.stage(scene);
        int walkerColor = RoleColors.of("sparkwitch:riftwalker", 0x5B6CFF);
        Vec3d firstGate = new Vec3d(5.6, 1, 1.5);
        Vec3d secondGate = new Vec3d(2.6, 1, 4.5);
        ElementLink<ActorElement> walker = Actors.enter(scene, walkerColor, Text.literal("隙行者"), firstGate, WEST,
                Direction.DOWN);
        Actors.hold(scene, walker, stack("sparkwitch:rift_gate"));
        scene.idle(20);
        scene.overlay().showText(80)
                .text("裂隙门在商店用 50 魔力买；其他活人看不见你手里的门")
                .independent()
                .attachKeyFrame();
        scene.idle(60);
        scene.overlay().showControls(firstGate.add(0, 2.6, 0), Pointing.DOWN, 30).rightClick()
                .withItem(stack("sparkwitch:rift_gate"));
        scene.idle(10);
        riftGate(scene, firstGate, Direction.WEST, walkerColor);
        scene.idle(20);
        scene.overlay().showText(90)
                .text("右键把门放在脚下，正面朝你看的方向；旁人也看得见门")
                .independent()
                .attachKeyFrame();
        scene.idle(50);
        Actors.walk(scene, walker, secondGate.subtract(firstGate), 40);
        scene.idle(42);
        Actors.turn(scene, walker, WEST);
        scene.idle(8);
        scene.overlay().showControls(secondGate.add(0, 2.6, 0), Pointing.DOWN, 30).rightClick()
                .withItem(stack("sparkwitch:rift_gate"));
        scene.idle(10);
        riftGate(scene, secondGate, Direction.WEST, walkerColor);
        Actors.hold(scene, walker, ItemStack.EMPTY);
        scene.overlay().showText(80)
                .text("走开几步再放一扇：门与门至少相隔 3 格")
                .independent()
                .attachKeyFrame();
        scene.idle(40);
        Actors.walk(scene, walker, new Vec3d(-1.0, 0, 0), 15);
        scene.idle(17);
        Actors.turn(scene, walker, EAST);
        scene.idle(10);
        scene.overlay().showControls(secondGate.add(0, 2.4, 0), Pointing.DOWN, 30).rightClick();
        scene.idle(10);
        Actors.vanish(scene, walker);
        gatePuff(scene, secondGate);
        scene.overlay().showText(90)
                .text("右键 3 格内的门进门：你从众人眼前消失，刀枪伤不到你")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showControls(secondGate.add(0, 2.6, 0), Pointing.DOWN, 40)
                .showing(keyCaps("key.left", "A", "key.right", "D"));
        gateOutline(scene, secondGate, Direction.WEST, walkerColor, 40);
        scene.idle(40);
        gateOutline(scene, firstGate, Direction.WEST, walkerColor, 80);
        scene.overlay().showText(90)
                .text("门内按 A/D 跳到另一扇门，没有任何动静（方框示意）")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showControls(firstGate.add(0, 2.6, 0), Pointing.DOWN, 30)
                .showing(keyCaps("key.sneak", "Shift"));
        scene.idle(10);
        // The first exit candidate: one block out in front of the gate, keeping the gate-facing look the hop set.
        // 第一个出门候选：门正前方一格，保持跳门时设定的、与门同向的视线。
        reappear(scene, walker, firstGate.add(-1.0, 0, 0), WEST);
        gatePuff(scene, firstGate);
        scene.overlay().showText(80)
                .text("按 Shift 从这扇门的正前方出来")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        scene.overlay().showText(90)
                .text("最长停留：你 30 秒，魔女阵营队友 20 秒，时间到会被弹出")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showText(80)
                .text("出门后再进门要等：你 30 秒，其他人 45 秒")
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

    /**
     * The crumbs everyone sees while a player eats, for {@code ticks}: five item particles every 4 ticks (they start
     * about 7 ticks into eating), 0.6 ahead of and 0.3-0.9 below the eyes (LivingEntity.spawnConsumptionEffects /
     * spawnItemParticles).
     * 玩家吃东西时人人看得到的碎屑，持续 ticks：每 4 tick 五颗物品粒子（开吃约 7 tick 后开始），位于眼睛前方 0.6、下方
     * 0.3～0.9 处。
     */
    private static void crumbs(SceneBuilder scene, ItemStack food, Vec3d feet, float yaw, int ticks) {
        ItemStackParticleEffect crumb = new ItemStackParticleEffect(ParticleTypes.ITEM, food);
        Vec3d mouth = feet.add(forward(yaw).multiply(0.6)).add(0, EYES - 0.6, 0);
        scene.effects().emitParticles(mouth, (world, x, y, z) -> world.addParticle(crumb,
                x + (world.random.nextDouble() - 0.5) * 0.3, y + (world.random.nextDouble() - 0.5) * 0.6, z,
                (world.random.nextDouble() - 0.5) * 0.1, world.random.nextDouble() * 0.1 + 0.15,
                (world.random.nextDouble() - 0.5) * 0.1), 1.25f, ticks);
    }

    /** The level direction a player facing {@code yaw} looks along (0 = south, 90 = west). 朝向 yaw 时的水平前方（0 为南，90 为西）。 */
    private static Vec3d forward(float yaw) {
        float radians = yaw * MathHelper.RADIANS_PER_DEGREE;
        return new Vec3d(-MathHelper.sin(radians), 0, MathHelper.cos(radians));
    }

    /** The yaw that faces from {@code from} towards {@code to}. 从 from 看向 to 的朝向角。 */
    private static float yawTowards(Vec3d from, Vec3d to) {
        Vec3d delta = to.subtract(from);
        return (float) (MathHelper.atan2(-delta.x, delta.z) * MathHelper.DEGREES_PER_RADIAN);
    }

    /**
     * Fracture's potion swirls on a standing player for {@code ticks}: about one every fourth tick somewhere on the
     * body, as LivingEntity.tickStatusEffects spawns a visible effect's particles for everyone.
     * 站立玩家身上 ticks 内的骨折药水漩涡：约每 4 tick 在身上某处冒出一颗，与 LivingEntity.tickStatusEffects 为可见效果
     * 生成、人人可见的粒子相同。
     */
    private static void fractureSwirls(SceneBuilder scene, Vec3d feet, int ticks) {
        scene.effects().emitParticles(feet, (world, x, y, z) -> world.addParticle(FRACTURE_SWIRL,
                x + (world.random.nextDouble() - 0.5) * 0.6, y + world.random.nextDouble() * 1.8,
                z + (world.random.nextDouble() - 0.5) * 0.6, 1, 1, 1), 0.25f, ticks);
    }

    /**
     * Keeps {@code follower} {@code behind} blocks behind {@code leader}, facing the same way, until {@link #release}d:
     * a hijacked player teleported onto the Kidnapper every tick (0, KidnapperControlComponent.teleportToController) or
     * a dragged body riding behind him ({@link #DRAGGED_FEET}).
     * 让 follower 保持在 leader 身后 behind 格、朝向相同，直到 release：被劫持的人每 tick 被传送到绑架者身上（0），或被拖的
     * 尸体挂在他身后（DRAGGED_FEET）。
     */
    private static Tether tether(SceneBuilder scene, ElementLink<ActorElement> leader,
                                 ElementLink<ActorElement> follower, double behind) {
        Tether tether = new Tether(leader, follower, behind);
        scene.addInstruction(tether);
        return tether;
    }

    private static void release(SceneBuilder scene, Tether tether) {
        scene.addInstruction(ponder -> tether.stopped = true);
    }

    /**
     * A steel line from {@code holder}'s hand to a grappling hook thrown from {@code from} that reaches {@code to}
     * after {@code flight} ticks, until {@link #unchain}ed: the hook's own chain draws nothing without a real owner.
     * 从 holder 的手连到钩爪的钢色线，钩爪从 from 抛出、flight tick 后到达 to，直到 unchain：钩爪自己的铁链没有真实主人时不绘制。
     */
    private static Chain chain(SceneBuilder scene, ElementLink<ActorElement> holder, Vec3d from, Vec3d to,
                               int flight) {
        Chain chain = new Chain(holder, from, to, flight);
        scene.addInstruction(chain);
        return chain;
    }

    private static void unchain(SceneBuilder scene, Chain chain) {
        scene.addInstruction(ponder -> chain.stopped = true);
    }

    /**
     * Throw a dragged body (KidnapperThrowService): set on top of the sneaking thrower at {@code release}, it leaves
     * with his look ({@code yaw}, {@code pitch} degrees down) times 0.8, at least 0.2 up, and flies for {@code ticks}.
     * 扔出被拖的尸体：先放到潜行投掷者头顶的 release 处，再以视线（yaw、向下 pitch 度）×0.8、向上至少 0.2 的速度飞出 ticks。
     */
    private static void throwBody(SceneBuilder scene, ElementLink<ActorElement> body, Vec3d release, float yaw,
                                  float pitch, int ticks) {
        float radians = pitch * MathHelper.RADIANS_PER_DEGREE;
        Vec3d ahead = forward(yaw);
        Vec3d look = ahead.multiply(MathHelper.cos(radians)).add(0, -MathHelper.sin(radians), 0)
                .multiply(BodyFlight.THROW_SPEED);
        Vec3d launch = new Vec3d(look.x, Math.max(look.y, BodyFlight.THROW_MIN_UP), look.z);
        scene.addInstruction(new BodyFlight(body, release, launch, yaw, release.y - SNEAKING_HEIGHT, ticks));
    }

    private static final class Tether extends TickingInstruction {
        private final ElementLink<ActorElement> leader;
        private final ElementLink<ActorElement> follower;
        private final double behind;
        private boolean stopped;

        Tether(ElementLink<ActorElement> leader, ElementLink<ActorElement> follower, double behind) {
            super(false, UNTIL_STOPPED);
            this.leader = leader;
            this.follower = follower;
            this.behind = behind;
        }

        @Override
        public void reset(PonderScene scene) {
            super.reset(scene);
            stopped = false;
        }

        @Override
        public void tick(PonderScene scene) {
            super.tick(scene);
            ActorElement lead = scene.resolve(leader);
            ActorElement follow = scene.resolve(follower);
            if (stopped || lead == null || follow == null) {
                return;
            }
            follow.setYaw(lead.yaw());
            follow.moveTo(lead.position().subtract(forward(lead.yaw()).multiply(behind)));
        }

        @Override
        public boolean isComplete() {
            return stopped || super.isComplete();
        }
    }

    private static final class Chain extends TickingInstruction {
        private final ElementLink<ActorElement> holder;
        private final Vec3d from;
        private final Vec3d to;
        private final int flight;
        private final Object slot = new Object();
        private int age;
        private boolean stopped;

        Chain(ElementLink<ActorElement> holder, Vec3d from, Vec3d to, int flight) {
            super(false, UNTIL_STOPPED);
            this.holder = holder;
            this.from = from;
            this.to = to;
            this.flight = flight;
        }

        @Override
        public void reset(PonderScene scene) {
            super.reset(scene);
            age = 0;
            stopped = false;
        }

        @Override
        public void tick(PonderScene scene) {
            super.tick(scene);
            ActorElement actor = scene.resolve(holder);
            if (stopped || actor == null) {
                return;
            }
            age++;
            Vec3d hook = from.lerp(to, Math.min(1, age / (double) flight));
            Vec3d ahead = forward(actor.yaw());
            Vec3d hand = actor.position().add(0, HAND_HEIGHT, 0).add(ahead.multiply(HAND_REACH))
                    .add(new Vec3d(-ahead.z, 0, ahead.x).multiply(HAND_SIDE));
            scene.getOutliner().showLine(slot, hand, hook).lineWidth(1 / 20f).colored(CHAIN_STEEL);
        }

        @Override
        public boolean isComplete() {
            return stopped || super.isComplete();
        }
    }

    /**
     * A thrown body flying as a LivingEntity does with no input: it moves by its velocity, then gravity 0.08 and drag
     * 0.98 act vertically and 0.91 horizontally in the air, 0.6 x 0.91 sliding on a floor (LivingEntity.travel).
     * 被扔出的尸体按无输入的 LivingEntity 飞行：先按速度移动，再在竖直方向受 0.08 重力与 0.98 阻力，空中水平阻力 0.91，
     * 落地后按 0.6 × 0.91 滑行。
     */
    private static final class BodyFlight extends TickingInstruction {
        static final double THROW_SPEED = 0.8;
        static final double THROW_MIN_UP = 0.2;
        private static final double GRAVITY = 0.08;
        private static final double VERTICAL_DRAG = 0.98;
        private static final double AIR_DRAG = 0.91;
        private static final double FLOOR_DRAG = 0.6 * 0.91;
        private final ElementLink<ActorElement> link;
        private final Vec3d release;
        private final Vec3d launch;
        private final float yaw;
        private final Vec3d pivot;
        private final double floor;
        private Vec3d at = Vec3d.ZERO;
        private Vec3d velocity = Vec3d.ZERO;
        private boolean grounded;
        private boolean placed;

        BodyFlight(ElementLink<ActorElement> link, Vec3d release, Vec3d launch, float yaw, double floor, int ticks) {
            super(false, ticks);
            this.link = link;
            this.release = release;
            this.launch = launch;
            this.yaw = yaw;
            this.pivot = forward(yaw).multiply(BODY_PIVOT);
            this.floor = floor;
        }

        @Override
        protected void firstTick(PonderScene scene) {
            super.firstTick(scene);
            at = release;
            velocity = launch;
            grounded = false;
            placed = false;
        }

        @Override
        public void tick(PonderScene scene) {
            super.tick(scene);
            ActorElement body = scene.resolve(link);
            if (body == null) {
                return;
            }
            if (placed) {
                at = at.add(velocity);
                if (at.y <= floor) {
                    at = new Vec3d(at.x, floor, at.z);
                    grounded = true;
                }
                double drag = grounded ? FLOOR_DRAG : AIR_DRAG;
                velocity = new Vec3d(velocity.x * drag, grounded ? 0 : (velocity.y - GRAVITY) * VERTICAL_DRAG,
                        velocity.z * drag);
            }
            placed = true;
            body.setYaw(yaw);
            // The body entity flies; Wathe draws its feet one block behind it. 飞行的是尸体实体；Wathe 把脚画在它身后一格。
            body.moveTo(at.subtract(pivot));
        }
    }

    /** Show {@code sprite} in the scene. 在场景中显示 sprite。 */
    private static ElementLink<Sprite> sprite(SceneBuilder scene, Sprite sprite) {
        ElementLink<Sprite> link = new ElementLinkImpl<>(Sprite.class);
        scene.addInstruction(ponder -> {
            sprite.setVisible(true);
            sprite.setFade(1);
            ponder.addElement(sprite);
            ponder.linkElement(sprite, link);
        });
        return link;
    }

    private static void unsprite(SceneBuilder scene, ElementLink<Sprite> link) {
        scene.addInstruction(ponder -> {
            Sprite sprite = ponder.resolve(link);
            if (sprite != null) {
                sprite.setVisible(false);
            }
        });
    }

    /**
     * A picture that always faces the viewer, moving from {@code from} to {@code to} over {@code ticks}, then held there
     * or gone: a thrown item as vanilla's FlyingItemEntityRenderer draws the Shuriken, or the hook head's own texture as
     * NinjaGrapplingHookEntityRenderer.renderHead draws it (a 0.5 square around the hook's centre).
     * 始终朝向观看者的图片，在 ticks 内从 from 移到 to，之后停在那里或消失：像原版 FlyingItemEntityRenderer 绘制手里剑那样的
     * 投掷物，或像 NinjaGrapplingHookEntityRenderer.renderHead 那样用钩头自己的贴图绘制（以钩爪中心为中心、边长 0.5 的方块）。
     */
    private static final class Sprite extends AnimatedSceneElementBase {
        private static final Identifier HOOK_HEAD = Identifier.of("sparkwitch", "textures/entity/ninja_grappling_hook.png");
        private static final float HOOK_HEAD_SIZE = 0.5f;
        /** Half the hook entity's 0.25 height: the head is drawn at its centre. 钩爪实体 0.25 高度的一半：钩头画在其中心。 */
        private static final double HOOK_CENTRE = 0.125;
        @Nullable
        private final ItemStack stack;
        private final Vec3d from;
        private final Vec3d to;
        private final int ticks;
        private final boolean stays;
        private int age;

        private Sprite(@Nullable ItemStack stack, Vec3d from, Vec3d to, int ticks, boolean stays) {
            this.stack = stack == null ? null : stack.copy();
            this.from = from;
            this.to = to;
            this.ticks = Math.max(1, ticks);
            this.stays = stays;
        }

        static Sprite item(ItemStack stack, Vec3d from, Vec3d to, int ticks, boolean stays) {
            return new Sprite(stack, from, to, ticks, stays);
        }

        /** The hook head flying from {@code from} to the latch point {@code to}, where it stays. 从 from 飞到钩点 to 并停在那里的钩头。 */
        static Sprite hookHead(Vec3d from, Vec3d to, int ticks) {
            return new Sprite(null, from.add(0, HOOK_CENTRE, 0), to.add(0, HOOK_CENTRE, 0), ticks, true);
        }

        @Override
        public void reset(@Nullable PonderScene scene) {
            age = 0;
        }

        @Override
        public void tick(PonderScene scene) {
            age++;
        }

        @Override
        protected void renderLast(PonderLevel world, VertexConsumerProvider buffer, DrawContext graphics, float fade,
                                  float pt) {
            float time = age + pt;
            if (fade <= 0.01f || time >= ticks && !stays) {
                return;
            }
            Vec3d at = from.lerp(to, Math.min(1, time / ticks));
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
            int light = lightCoordsFromFade(fade);
            if (stack != null) {
                MinecraftClient.getInstance().getItemRenderer().renderItem(stack, ModelTransformationMode.GROUND, light,
                        OverlayTexture.DEFAULT_UV, ms, buffer, world, 0);
            } else {
                ms.scale(HOOK_HEAD_SIZE, HOOK_HEAD_SIZE, HOOK_HEAD_SIZE);
                MatrixStack.Entry entry = ms.peek();
                VertexConsumer consumer = buffer.getBuffer(RenderLayer.getEntityCutoutNoCull(HOOK_HEAD));
                corner(consumer, entry, light, 0, 0, 0, 1);
                corner(consumer, entry, light, 1, 0, 1, 1);
                corner(consumer, entry, light, 1, 1, 1, 0);
                corner(consumer, entry, light, 0, 1, 0, 0);
            }
            ms.pop();
        }

        private static void corner(VertexConsumer consumer, MatrixStack.Entry entry, int light, float x, float y,
                                   float u, float v) {
            consumer.vertex(entry, x - 0.5f, y - 0.5f, 0)
                    .color(0xFFFFFFFF)
                    .texture(u, v)
                    .overlay(OverlayTexture.DEFAULT_UV)
                    .light(light)
                    .normal(entry, 0, 1, 0);
        }
    }

    /**
     * Particles as ServerWorld.spawnParticles sends them and the client spreads them (ClientPlayNetworkHandler
     * .onParticle): each of {@code count} at a gaussian offset scaled by {@code dx}/{@code dy}/{@code dz}, moving at a
     * gaussian speed scaled by {@code speed} on every axis.
     * 与 ServerWorld.spawnParticles 发出、客户端展开的粒子相同：count 个粒子各自按 dx/dy/dz 缩放的高斯偏移放置，每轴按 speed
     * 缩放的高斯速度运动。
     */
    private static void serverParticles(SceneBuilder scene, ParticleEffect particle, Vec3d at, int count, double dx,
                                        double dy, double dz, double speed) {
        scene.effects().emitParticles(at, (world, x, y, z) -> world.addParticle(particle,
                x + world.random.nextGaussian() * dx, y + world.random.nextGaussian() * dy,
                z + world.random.nextGaussian() * dz, world.random.nextGaussian() * speed,
                world.random.nextGaussian() * speed, world.random.nextGaussian() * speed), count, 1);
    }

    /**
     * A trail left by something flying from {@code from} to {@code to} over {@code ticks} on the same arc as
     * {@link ThrownElement}: each tick, {@code perTick} particles spread along that tick's stretch, each kept with
     * {@code chance}.
     * 从 from 飞到 to、用时 ticks、弧线与 ThrownElement 相同的物体留下的轨迹：每 tick 沿这一段均匀放出 perTick 个粒子，每个按
     * chance 的概率保留。
     */
    private static void trail(SceneBuilder scene, ParticleEffect particle, Vec3d from, Vec3d to, int ticks, double arc,
                              int perTick, float chance) {
        scene.addInstruction(new Trail(particle, from, to, ticks, arc, perTick, chance));
    }

    /**
     * The launcher's backblast as PotionBackblastService.present lays it out: from the eyes straight back (yaw only)
     * every 0.25 block for 4 blocks, two flames and one large smoke at each point.
     * 炮筒尾焰，排布与 PotionBackblastService.present 相同：从眼睛沿正后方（只看偏航角）每 0.25 格一点，共 4 格，每点两团火焰、
     * 一团大烟。
     */
    private static void backblast(SceneBuilder scene, Vec3d eyes, float yaw) {
        Vec3d backwards = forward(yaw).multiply(-1);
        scene.effects().emitParticles(eyes, (world, x, y, z) -> {
            for (double travelled = BACKBLAST_STEP; travelled <= BACKBLAST_LENGTH; travelled += BACKBLAST_STEP) {
                Vec3d point = eyes.add(backwards.multiply(travelled));
                for (int flame = 0; flame < 2; flame++) {
                    world.addParticle(ParticleTypes.FLAME, point.x + world.random.nextGaussian() * 0.08,
                            point.y + world.random.nextGaussian() * 0.08, point.z + world.random.nextGaussian() * 0.08,
                            world.random.nextGaussian() * 0.02, world.random.nextGaussian() * 0.02,
                            world.random.nextGaussian() * 0.02);
                }
                world.addParticle(ParticleTypes.LARGE_SMOKE, point.x + world.random.nextGaussian() * 0.12,
                        point.y + world.random.nextGaussian() * 0.12, point.z + world.random.nextGaussian() * 0.12,
                        world.random.nextGaussian() * 0.01, world.random.nextGaussian() * 0.01,
                        world.random.nextGaussian() * 0.01);
            }
        }, 1, 1);
    }

    /**
     * A potion shell bursting, as PotionBlastService.present shows it to everyone: Wathe's big explosion flash, 100
     * smoke puffs and 100 shards of the shell item, 0.1 above the centre.
     * 药剂炮弹爆炸，与 PotionBlastService.present 展示给所有人的相同：Wathe 的大爆炸闪光、100 团烟和 100 片炮弹碎片，位于爆心
     * 上方 0.1 处。
     */
    private static void shellBlast(SceneBuilder scene, Vec3d center, ItemStack shell) {
        Vec3d at = center.add(0, 0.1, 0);
        serverParticles(scene, WatheParticles.BIG_EXPLOSION, at, 1, 0, 0, 0, 0);
        serverParticles(scene, ParticleTypes.SMOKE, at, 100, 0, 0, 0, 0.2);
        serverParticles(scene, new ItemStackParticleEffect(ParticleTypes.ITEM, shell), at, 100, 0, 0, 0, 1.0);
    }

    /** The blast cube's footprint as a red square on the floor. 爆炸立方体在地面上的范围，画成红色方框。 */
    private static void blastSquare(SceneBuilder scene, Vec3d center, double half, int ticks) {
        double y = 1.02;
        Vec3d a = new Vec3d(center.x - half, y, center.z - half);
        Vec3d b = new Vec3d(center.x + half, y, center.z - half);
        Vec3d c = new Vec3d(center.x + half, y, center.z + half);
        Vec3d d = new Vec3d(center.x - half, y, center.z + half);
        scene.overlay().showLine(PonderPalette.RED, a, b, ticks);
        scene.overlay().showLine(PonderPalette.RED, b, c, ticks);
        scene.overlay().showLine(PonderPalette.RED, c, d, ticks);
        scene.overlay().showLine(PonderPalette.RED, d, a, ticks);
    }

    /**
     * Vanilla's sculk shrieker event (WorldEvents.SCULK_SHRIEKS, as WorldRenderer plays it): ten shriek rings, each 5
     * ticks after the last, rising from the top of a shrieker standing in {@code block}.
     * 原版幽匿尖啸体事件（WorldEvents.SCULK_SHRIEKS，与 WorldRenderer 的播放相同）：十个尖啸波纹，依次间隔 5 tick，从位于 block
     * 的尖啸体顶部升起。
     */
    private static void shriek(SceneBuilder scene, BlockPos block) {
        Vec3d at = new Vec3d(block.getX() + 0.5, block.getY() + SculkShriekerBlock.TOP, block.getZ() + 0.5);
        scene.effects().emitParticles(at, (world, x, y, z) -> {
            for (int ring = 0; ring < 10; ring++) {
                world.addParticle(new ShriekParticleEffect(ring * 5), x, y, z, 0, 0, 0);
            }
        }, 1, 1);
    }

    /**
     * The Shriek Gun's beam (ShriekGunService.spawnBeam, ShriekGunRules.particleDistances): one sonic boom ring every
     * block from the eyes up to where it hit.
     * 啸音铳的射线：从眼睛起每格一个音爆圈，直到命中处。
     */
    private static void sonicBeam(SceneBuilder scene, Vec3d eyes, Vec3d direction, double cut) {
        Vec3d unit = direction.normalize();
        for (int step = 1; step <= Math.floor(cut); step++) {
            serverParticles(scene, ParticleTypes.SONIC_BOOM, eyes.add(unit.multiply(step)), 1, 0, 0, 0, 0);
        }
    }

    /**
     * Knock a standing actor back with {@code launch} (blocks a tick), flying as LivingEntity.travel moves a player
     * with no input, its feet stopping at {@code floor}, for {@code ticks}.
     * 以 launch（每 tick 格数）把站立的演员击飞，按 LivingEntity.travel 对无输入玩家的移动方式飞行 ticks，脚落在 floor 高度。
     */
    private static void fling(SceneBuilder scene, ElementLink<ActorElement> actor, Vec3d launch, double floor,
                              int ticks) {
        scene.addInstruction(new Fling(actor, launch, floor, ticks));
    }

    /**
     * A spore flask breaking in {@code cell} (DeepDarkZoneCues.landing): the shrieker rings and six sculk souls.
     * 孢瓶在 cell 碎裂（DeepDarkZoneCues.landing）：尖啸波纹与六个幽匿之魂。
     */
    private static void sporeLanding(SceneBuilder scene, BlockPos cell) {
        shriek(scene, cell);
        serverParticles(scene, ParticleTypes.SCULK_SOUL, new Vec3d(cell.getX() + 0.5, cell.getY() + 0.15,
                cell.getZ() + 0.5), 6, 0.3, 0.1, 0.3, 0.02);
    }

    /**
     * Turn the spore_cabin corridor into the Deep Dark Zone ({@code convert}) or back, for the cells whose distance
     * from {@code landing} lies in [{@code nearest}, {@code farthest}). The zone floods the corridor's air, so it
     * converts the corridor floor (sculk, as an upward-open floor always is) and the corridor face of the wall
     * (DeepDarkZoneEligibility.look); the closed door and everything behind it stay. The 2-high wall stands for a
     * carriage wall up to the ceiling, so the zone neither climbs over it nor counts its top as a floor. Converting
     * plays DeepDarkZoneCues.spread's twelve sculk charges, one in each equal slice of the cells nearest first;
     * restoring pops sculk charges on a few of the blocks, as DeepDarkZoneCues.restore samples them.
     * 把 spore_cabin 的走廊变成深暗领域（convert）或恢复原样，只处理到 landing 的距离位于 [nearest, farthest) 的格子。领域填满
     * 走廊的空气，因此转换走廊地面（朝上外露的地面总是幽匿块）和墙朝走廊的一面（DeepDarkZoneEligibility.look）；关着的门及其
     * 后面的一切保持原样。2 格高的墙代表一直顶到车顶的车厢墙，所以领域不会翻过墙，墙顶也不算地面。转换时播放
     * DeepDarkZoneCues.spread 的十二次幽匿充能（按由近及远的顺序等分，每段一次）；恢复时在少数方块上爆出幽匿充能，与
     * DeepDarkZoneCues.restore 的抽样相同。
     */
    private static void sporeZone(SceneBuilder scene, BlockPos landing, boolean convert, double nearest,
                                  double farthest) {
        BlockState floorTile = block("wathe:marble_tiles");
        BlockState wall = block("wathe:anthracite_steel");
        List<BlockPos> cells = new ArrayList<>();
        for (int x = 0; x < SPORE_STAGE_SIZE; x++) {
            for (int y = 0; y <= 2; y++) {
                for (int z = 0; z <= SPORE_WALL_Z; z++) {
                    boolean floor = y == 0 && z < SPORE_WALL_Z;
                    boolean wallFace = y > 0 && z == SPORE_WALL_Z && x != SPORE_DOOR_X;
                    BlockPos pos = new BlockPos(x, y, z);
                    double distance = Math.sqrt(pos.getSquaredDistance(landing));
                    if ((floor || wallFace) && distance >= nearest && distance < farthest) {
                        cells.add(pos);
                    }
                }
            }
        }
        cells.sort(Comparator.comparingDouble(pos -> pos.getSquaredDistance(landing)));
        for (int i = 0; i < cells.size(); i++) {
            BlockPos pos = cells.get(i);
            boolean floor = pos.getY() == 0;
            scene.world().setBlock(pos, convert ? deepDarkLook(pos, floor) : floor ? floorTile : wall, false);
            if (!convert && i % 4 == 0) {
                chargePop(scene, pos);
            }
        }
        int samples = Math.min(ZONE_CHARGE_SAMPLES, cells.size());
        for (int sample = 0; convert && sample < samples; sample++) {
            BlockPos pos = cells.get((int) ((sample + 0.5) * cells.size() / samples));
            zoneCharge(scene, pos, pos.getY() == 0 ? Direction.UP : Direction.NORTH);
        }
    }

    /**
     * Vanilla's sculk charge event of charge 2 on one open face of a converted block (WorldRenderer,
     * WorldEvents.SCULK_CHARGE, ParticleUtil.spawnParticle): up to two charges 0.35 from the block centre towards that
     * face, scattered across it, drifting at most 0.005 a tick; turned upside down on an upward face.
     * 原版在已转换方块的一个外露面上的 2 级幽匿充能事件：至多两个充能粒子，位于方块中心朝该面 0.35 处、散布在面上，以至多每 tick
     * 0.005 漂移；朝上的面上粒子倒转。
     */
    private static void zoneCharge(SceneBuilder scene, BlockPos pos, Direction face) {
        SculkChargeParticleEffect charge = new SculkChargeParticleEffect(face == Direction.UP ? MathHelper.PI : 0);
        scene.effects().emitParticles(Vec3d.ofCenter(pos), (world, x, y, z) -> {
            int count = world.random.nextInt(3);
            for (int i = 0; i < count; i++) {
                world.addParticle(charge,
                        x + (face.getOffsetX() == 0 ? world.random.nextDouble() - 0.5 : face.getOffsetX() * 0.35),
                        y + (face.getOffsetY() == 0 ? world.random.nextDouble() - 0.5 : face.getOffsetY() * 0.35),
                        z + (face.getOffsetZ() == 0 ? world.random.nextDouble() - 0.5 : face.getOffsetZ() * 0.35),
                        (world.random.nextDouble() - 0.5) * 0.01, (world.random.nextDouble() - 0.5) * 0.01,
                        (world.random.nextDouble() - 0.5) * 0.01);
            }
        }, 1, 1);
    }

    /**
     * DeepDarkZoneEligibility.look: an upward-open floor is sculk; any other face hashes its position (SplitMix64) into
     * 15 buckets, 9 sculk and 2 each of deepslate tiles, deepslate bricks and cobbled deepslate.
     * DeepDarkZoneEligibility.look：朝上外露的地面是幽匿块；其他面按位置哈希（SplitMix64）分进 15 个桶，9 个幽匿块，深板岩瓦、
     * 深板岩砖、深板岩圆石各 2 个。
     */
    private static BlockState deepDarkLook(BlockPos pos, boolean floor) {
        if (floor) {
            return Blocks.SCULK.getDefaultState();
        }
        long z = pos.asLong() + 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        int bucket = (int) Long.remainderUnsigned(z ^ (z >>> 31), 15);
        if (bucket < 9) {
            return Blocks.SCULK.getDefaultState();
        }
        if (bucket < 11) {
            return Blocks.DEEPSLATE_TILES.getDefaultState();
        }
        return bucket < 13 ? Blocks.DEEPSLATE_BRICKS.getDefaultState() : Blocks.COBBLED_DEEPSLATE.getDefaultState();
    }

    /**
     * Vanilla's charge-0 sculk charge event on a full block (WorldRenderer, WorldEvents.SCULK_CHARGE): 40 charge pops
     * inside the block drifting out at up to 0.07 a tick.
     * 原版在完整方块上的 0 级幽匿充能事件：方块内 40 个充能爆裂粒子，以至多每 tick 0.07 向外飘出。
     */
    private static void chargePop(SceneBuilder scene, BlockPos pos) {
        scene.effects().emitParticles(Vec3d.ofCenter(pos), (world, x, y, z) -> {
            float ox = 2 * world.random.nextFloat() - 1;
            float oy = 2 * world.random.nextFloat() - 1;
            float oz = 2 * world.random.nextFloat() - 1;
            world.addParticle(ParticleTypes.SCULK_CHARGE_POP, x + ox * 0.45, y + oy * 0.45, z + oz * 0.45,
                    ox * 0.07, oy * 0.07, oz * 0.07);
        }, 40, 1);
    }

    private static BlockState block(String id) {
        return Registries.BLOCK.get(Identifier.of(id)).getDefaultState();
    }

    /**
     * Place a Rift Gate at {@code pos} facing {@code facing}, with RiftGatePlacementService.playPlacementCue's burst:
     * 24 portal and 6 witch particles around its centre, one block up.
     * 在 pos 放下朝向 facing 的裂隙门，并放出 RiftGatePlacementService.playPlacementCue 的粒子：门中心（上方一格）周围 24 个
     * 传送门粒子与 6 个女巫粒子。
     */
    private static void riftGate(SceneBuilder scene, Vec3d pos, Direction facing, int color) {
        RiftGate gate = new RiftGate(pos, facing, color);
        scene.addInstruction(ponder -> {
            gate.setVisible(true);
            gate.setFade(1);
            ponder.addElement(gate);
        });
        Vec3d centre = pos.add(0, 1, 0);
        serverParticles(scene, ParticleTypes.PORTAL, centre, 24, 0.3, 0.7, 0.3, 0.4);
        serverParticles(scene, ParticleTypes.WITCH, centre, 6, 0.3, 0.6, 0.3, 0);
    }

    /**
     * The puff at a gate when someone goes in or comes out (RiftSessionBody.cue): 24 reverse-portal particles around
     * its centre.
     * 有人进门或出门时门上的一团粒子（RiftSessionBody.cue）：门中心周围 24 个反向传送门粒子。
     */
    private static void gatePuff(SceneBuilder scene, Vec3d pos) {
        serverParticles(scene, ParticleTypes.REVERSE_PORTAL, pos.add(0, 1, 0), 24, 0.3, 0.7, 0.3, 0.02);
    }

    /** A box around a gate for {@code ticks}: which gate the occupant is at. 门周围 ticks 的方框：门内的人在哪扇门。 */
    private static void gateOutline(SceneBuilder scene, Vec3d pos, Direction facing, int color, int ticks) {
        double halfX = facing.getAxis() == Direction.Axis.X ? 0.3 : 0.6;
        double halfZ = facing.getAxis() == Direction.Axis.Z ? 0.3 : 0.6;
        Box box = new Box(pos.x - halfX, pos.y, pos.z - halfZ, pos.x + halfX, pos.y + 2.3, pos.z + halfZ);
        Object slot = new Object();
        scene.addInstruction(new TickingInstruction(false, ticks) {
            @Override
            public void tick(PonderScene ponder) {
                super.tick(ponder);
                ponder.getOutliner().chaseAABB(slot, box).lineWidth(1 / 16f).colored(color);
            }
        });
    }

    /**
     * A vanished actor shows again at {@code feet} facing {@code yaw}, with no fade, as a player leaving spectator
     * mode does; it is moved a tick before it shows so it never streaks across the stage.
     * 消失的演员在 feet 处、朝向 yaw 重新出现，没有淡入，与玩家离开旁观模式相同；先移动、下一 tick 再显示，免得一路拖影。
     */
    private static void reappear(SceneBuilder scene, ElementLink<ActorElement> actor, Vec3d feet, float yaw) {
        scene.addInstruction(ponder -> {
            ActorElement element = ponder.resolve(actor);
            if (element != null) {
                element.moveTo(feet);
                element.setYaw(yaw);
            }
        });
        scene.idle(1);
        scene.addInstruction(ponder -> {
            ActorElement element = ponder.resolve(actor);
            if (element != null) {
                element.setVisible(true);
            }
        });
    }

    /**
     * Key caps side by side for pairs of (binding id, fallback label), shrunk to fit the 24-pixel icon slot of a
     * control hint; drawn like {@link #key}.
     * 若干键帽并排显示，参数为（按键 id，备用标签）成对给出，缩小到控制提示 24 像素宽的图标位里；画法与 key 相同。
     */
    private static ScreenElement keyCaps(String... idsAndFallbacks) {
        return (graphics, x, y) -> {
            TextRenderer font = MinecraftClient.getInstance().textRenderer;
            int caps = idsAndFallbacks.length / 2;
            Text[] labels = new Text[caps];
            int[] widths = new int[caps];
            int total = 0;
            for (int cap = 0; cap < caps; cap++) {
                labels[cap] = boundKey(idsAndFallbacks[2 * cap], idsAndFallbacks[2 * cap + 1]);
                widths[cap] = Math.max(14, font.getWidth(labels[cap]) + 6);
                total += widths[cap] + (cap > 0 ? 2 : 0);
            }
            float scale = Math.min(1, 22f / total);
            MatrixStack ms = graphics.getMatrices();
            ms.push();
            ms.translate(x + 1, y + 1 + 14 * (1 - scale) / 2, 0);
            ms.scale(scale, scale, 1);
            int left = 0;
            for (int cap = 0; cap < caps; cap++) {
                graphics.fill(left, 0, left + widths[cap], 14, 0xFFA0A0A0);
                graphics.fill(left + 1, 1, left + widths[cap] - 1, 12, 0xFF3A3A3A);
                graphics.drawText(font, labels[cap], left + (widths[cap] - font.getWidth(labels[cap])) / 2, 3,
                        0xFFFFFFFF, false);
                left += widths[cap] + 2;
            }
            ms.pop();
        };
    }

    private static final class Trail extends TickingInstruction {
        private final ParticleEffect particle;
        private final Vec3d from;
        private final Vec3d to;
        private final int flight;
        private final double arc;
        private final int perTick;
        private final float chance;
        private int age;

        Trail(ParticleEffect particle, Vec3d from, Vec3d to, int flight, double arc, int perTick, float chance) {
            super(false, flight);
            this.particle = particle;
            this.from = from;
            this.to = to;
            this.flight = flight;
            this.arc = arc;
            this.perTick = perTick;
            this.chance = chance;
        }

        @Override
        protected void firstTick(PonderScene scene) {
            super.firstTick(scene);
            age = 0;
        }

        @Override
        public void tick(PonderScene scene) {
            super.tick(scene);
            PonderLevel world = scene.getWorld();
            for (int step = 0; step < perTick; step++) {
                if (world.random.nextFloat() >= chance) {
                    continue;
                }
                double t = (age + step / (double) perTick) / flight;
                Vec3d point = from.lerp(to, t).add(0, arc * 4 * t * (1 - t), 0);
                world.addParticle(particle, point.x, point.y, point.z, 0, 0, 0);
            }
            age++;
        }
    }

    /**
     * A standing player knocked back with no input (LivingEntity.travel): it moves by its velocity, then the
     * horizontal speed keeps 0.6 x 0.91 if it stood on a floor before that move (so the first tick too), 0.91 if it was
     * in the air, and the vertical one loses 0.08 and keeps 0.98.
     * 无输入的站立玩家被击退（LivingEntity.travel）：先按速度移动；若这一步之前站在地面上（第一 tick 也是），水平速度保留
     * 0.6 × 0.91，之前在空中则保留 0.91；竖直速度减去 0.08 后保留 0.98。
     */
    private static final class Fling extends TickingInstruction {
        private final ElementLink<ActorElement> link;
        private final Vec3d launch;
        private final double floor;
        private Vec3d at = Vec3d.ZERO;
        private Vec3d velocity = Vec3d.ZERO;
        private boolean grounded;

        Fling(ElementLink<ActorElement> link, Vec3d launch, double floor, int ticks) {
            super(false, ticks);
            this.link = link;
            this.launch = launch;
            this.floor = floor;
        }

        @Override
        protected void firstTick(PonderScene scene) {
            super.firstTick(scene);
            ActorElement actor = scene.resolve(link);
            at = actor == null ? Vec3d.ZERO : actor.position();
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
            at = at.add(velocity);
            grounded = at.y <= floor;
            if (grounded) {
                at = new Vec3d(at.x, floor, at.z);
            }
            velocity = new Vec3d(velocity.x * drag, grounded ? 0 : (velocity.y - 0.08) * 0.98, velocity.z * drag);
            actor.moveTo(at);
        }
    }

    /**
     * A placed Rift Gate as RiftGateEntityRenderer draws it: the standalone model sparkwitch:item/rift_gate_placed
     * through the item renderer's FIXED transform, full-bright, lifted 0.75 and turned so its front faces
     * {@code facing}; each tick it adds RiftGateClientEffects' sparse ambient particles (portal drawn into the swirl,
     * role-colour dust and rare witch sparks on the rune ring).
     * 已放置的裂隙门，画法与 RiftGateEntityRenderer 相同：独立模型 sparkwitch:item/rift_gate_placed 经物品渲染器 FIXED 变换、
     * 全亮度绘制，上移 0.75 并转到正面朝向 facing；每 tick 加上 RiftGateClientEffects 的少量环境粒子（吸入旋涡的传送门粒子、
     * 符文环上的职业色尘与偶尔的女巫火花）。
     */
    private static final class RiftGate extends AnimatedSceneElementBase {
        private static final Identifier MODEL = Identifier.of("sparkwitch", "item/rift_gate_placed");
        /** Model y -4 lifted onto the gate base (RiftGatePresentationRules.MODEL_LIFT). 把模型 y -4 抬到门底。 */
        private static final double MODEL_LIFT = 0.75;
        /** RiftGatePresentationRules: swirl centre, rune ring and swirl target sizes. 旋涡中心、符文环与旋涡内区的尺寸。 */
        private static final double SWIRL_UP = 18 / 16.0;
        private static final double RING_HALF_WIDTH = 6.5 / 16.0;
        private static final double RING_HALF_HEIGHT = 13.25 / 16.0;
        private static final double RING_HALF_DEPTH = 1.5 / 16.0;
        private static final double SWIRL_HALF_WIDTH = 0.25;
        private static final double SWIRL_HALF_HEIGHT = 0.7;
        private final Vec3d pos;
        private final Direction facing;
        private final DustParticleEffect dust;
        private final ItemStack stack = stack("sparkwitch:rift_gate");

        RiftGate(Vec3d pos, Direction facing, int color) {
            this.pos = pos;
            this.facing = facing;
            this.dust = new DustParticleEffect(new Vector3f((color >> 16 & 0xFF) / 255f, (color >> 8 & 0xFF) / 255f,
                    (color & 0xFF) / 255f), 0.9f);
        }

        @Override
        public void tick(PonderScene scene) {
            if (!isVisible()) {
                return;
            }
            PonderLevel world = scene.getWorld();
            if (world.random.nextDouble() < 0.5) {
                double[] start = ringPoint(world.random.nextFloat() * MathHelper.TAU);
                double depth = (world.random.nextBoolean() ? 1 : -1) * (0.25 + world.random.nextDouble() * 0.35);
                Vec3d from = local(start[0] * 1.1, start[1], depth);
                Vec3d to = local(SWIRL_HALF_WIDTH * (world.random.nextDouble() * 2 - 1),
                        SWIRL_UP + SWIRL_HALF_HEIGHT * (world.random.nextDouble() * 2 - 1), 0);
                world.addParticle(ParticleTypes.PORTAL, to.x, to.y, to.z, from.x - to.x, from.y - to.y - 1,
                        from.z - to.z);
            }
            if (world.random.nextDouble() < 0.25) {
                ring(world, dust, 0);
            }
            if (world.random.nextDouble() < 1 / 40.0) {
                ring(world, ParticleTypes.WITCH, 0.02);
            }
        }

        private void ring(PonderLevel world, ParticleEffect particle, double upwards) {
            double[] point = ringPoint(world.random.nextFloat() * MathHelper.TAU);
            Vec3d at = local(point[0], point[1], (world.random.nextDouble() * 2 - 1) * RING_HALF_DEPTH * 2);
            world.addParticle(particle, at.x, at.y, at.z, 0, upwards, 0);
        }

        /** A point on the rune ring, {side, up} (a superellipse of exponent 4). 符文环上的点 {横向, 高度}（指数 4 的超椭圆）。 */
        private static double[] ringPoint(double t) {
            double cos = Math.cos(t);
            double sin = Math.sin(t);
            return new double[]{RING_HALF_WIDTH * Math.signum(cos) * Math.sqrt(Math.abs(cos)),
                    SWIRL_UP + RING_HALF_HEIGHT * Math.signum(sin) * Math.sqrt(Math.abs(sin))};
        }

        /** Gate-local (side, up, forward) to the scene. 门局部坐标（横向、高度、前方）转为场景坐标。 */
        private Vec3d local(double side, double up, double ahead) {
            double forwardX = facing.getOffsetX();
            double forwardZ = facing.getOffsetZ();
            return new Vec3d(pos.x - forwardZ * side + forwardX * ahead, pos.y + up,
                    pos.z + forwardX * side + forwardZ * ahead);
        }

        @Override
        protected void renderLast(PonderLevel world, VertexConsumerProvider buffer, DrawContext graphics, float fade,
                                  float pt) {
            if (fade <= 0.01f) {
                return;
            }
            MinecraftClient client = MinecraftClient.getInstance();
            MatrixStack ms = graphics.getMatrices();
            ms.push();
            ms.translate(pos.x, pos.y + MODEL_LIFT, pos.z);
            ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180 - facing.asRotation()));
            client.getItemRenderer().renderItem(stack, ModelTransformationMode.FIXED, false, ms, buffer,
                    LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV,
                    client.getBakedModelManager().getModel(MODEL));
            ms.pop();
        }
    }
}
