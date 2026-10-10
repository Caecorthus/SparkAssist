package dev.caecorthus.sparkassist.client.ponder;

import static dev.caecorthus.sparkassist.client.ponder.SparkPonderDemos.item;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import net.createmod.catnip.gui.element.ScreenElement;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.element.AnimatedSceneElementBase;
import net.createmod.ponder.foundation.element.ElementLinkImpl;
import net.createmod.ponder.foundation.instruction.TickingInstruction;
import net.fabricmc.fabric.api.client.model.loading.v1.FabricBakedModelManager;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.EntityEffectParticleEffect;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Demos for SparkWitch's civilian and police roles and the neutral Insider, opened only from the role's guide page;
 * each needs SparkWitch.
 * What onlookers cannot see (an outline only the role itself sees, the Taser's hit, a bullet's path) is drawn as boxes
 * or lines, and the captions call them illustrations.
 * SparkWitch 平民阵营、警职职业与中立内应的演示，只从该职业的指南页打开；都需要 SparkWitch。旁观者看不到的东西（只有本人看得到的
 * 轮廓、电击枪的命中、子弹的弹道）用方框或线条表示，字幕注明是示意。
 */
final class SparkWitchCivilianScenes {
    /** NoellesRoles' shared ability key, which SparkWitch skills reuse. SparkWitch 技能沿用的 NoellesRoles 技能键。 */
    private static final String ABILITY_KEY = "key.noellesroles.ability";
    /** SparkWitch's secondary skill key (default N). SparkWitch 的第二技能键（默认 N）。 */
    private static final String SECONDARY_KEY = "key.sparkwitch.secondary_skill";
    /** The driven Seeker car's speed, blocks a tick (SeekerRules.CAR_SPEED). 被驾驶的搜寻小车速度，格每 tick。 */
    private static final double CAR_SPEED = 0.375;
    /** The breaker mark's colour (SeekerRules.MARK_COLOR). 损坏者标记的颜色。 */
    private static final int MARK_RED = 0xFF4D4D;
    /** Bone Setting's potion swirl (BoneSettingEffect colour 0xB7E07A, particles shown). 正骨的药水漩涡粒子。 */
    private static final EntityEffectParticleEffect BONE_SETTING_SWIRL =
            EntityEffectParticleEffect.create(ParticleTypes.ENTITY_EFFECT, 0xFFB7E07A);
    /** Vanilla Speed's potion swirl (colour 0x33EBFF). 原版速度效果的药水漩涡粒子。 */
    private static final EntityEffectParticleEffect SPEED_SWIRL =
            EntityEffectParticleEffect.create(ParticleTypes.ENTITY_EFFECT, 0xFF33EBFF);
    /** Gold dust for an illustrated area edge. 用于示意范围边界的金色粉尘。 */
    private static final DustParticleEffect EDGE_DUST = new DustParticleEffect(new Vector3f(1.0f, 0.8f, 0.15f), 1.5f);
    /** A standing actor's eyes above the feet. 站立演员的眼睛离脚底的高度。 */
    private static final double EYES = 1.62;
    /** A crack or flight runs until the scene ends; long enough for any scene. 裂痕或飞行持续到场景结束；对任何场景都足够长。 */
    private static final int UNTIL_STOPPED = 20 * 60 * 10;
    /** The stages' plate width. 舞台底板的边长。 */
    private static final double PLATE = 7;
    /** Points around a full illustrated edge circle. 示意边界整圆上的点数。 */
    private static final int EDGE_POINTS = 240;
    /** A mature Witch Factor holder's outline (WitchFactorOutlineRules.FACTOR_COLOR). 成熟魔女因子持有者的描边颜色。 */
    private static final int FACTOR_PURPLE = 0x8B70DB;
    /** A marked player after a kill (PerfumerRules.BLOODY_OUTLINE_COLOR). 被标记者杀人后的描边颜色。 */
    private static final int BLOODY_RED = 0xC13838;
    /** A body the Perfumer smells (PerfumerRules.CORPSE_OUTLINE_COLOR). 调香师闻到的尸体的描边颜色。 */
    private static final int CORPSE_GREY = 0xD8D8D8;
    /** Cooling Oil's mist (SparkStrength CoolingOilService.MINT_MIST). 风油精的雾气。 */
    private static final DustParticleEffect MINT_MIST = new DustParticleEffect(new Vector3f(0.55f, 0.95f, 0.75f), 1.3f);
    /** Cooling Oil's mist core (CoolingOilService.MINT_CORE). 风油精雾气的核心。 */
    private static final DustParticleEffect MINT_CORE = new DustParticleEffect(new Vector3f(0.80f, 1.0f, 0.90f), 1.0f);
    /** A lying body's length from the feet, as ActorElement draws it. ActorElement 绘制的尸体从脚到头的长度。 */
    private static final double BODY_LENGTH = 1.75;
    /** The Blind's line art: white on black (sparkwitch_blind_echo.fsh). 盲人画面的线稿：黑底白线。 */
    private static final int ECHO_WHITE = 0xFFFFFF;
    /**
     * The faint band a sound front sweeps over surfaces (wave * 0.3 in the shader), lifted so it reads on the dimmed
     * floor. 声波前沿扫过表面的淡淡光带（着色器中的 wave * 0.3），提亮一些以便在调暗的地面上看清。
     */
    private static final int ECHO_WAVE_GREY = 0xA8A8A8;
    /** Blocks a sound lights up around its source (BlindRules.SOUND_REVEAL_RADIUS). 声音照亮声源周围的格数。 */
    private static final double SOUND_REVEAL_RADIUS = 4;
    /** A sound pulse and a sounding player last 1.5 s (BlindRules.SOUND_PULSE_TICKS). 声音脉冲与发声玩家持续 1.5 秒。 */
    private static final int SOUND_PULSE_TICKS = 30;
    /** A sound front reaches its radius in 0.25 s (BlindEchoUniforms.WAVE_EXPAND_SECONDS). 声波 0.25 秒扩张到半径。 */
    private static final int SOUND_EXPAND_TICKS = 5;
    /** At most one pulse per emitter in this window (BlindRules.EMITTER_THROTTLE_TICKS). 同一发声者在此窗口内至多一个脉冲。 */
    private static final int EMITTER_THROTTLE_TICKS = 10;
    /**
     * Blocks walked between two footsteps: vanilla adds 0.6 per block to the distance travelled and steps once per
     * unit (Entity.move). 两次脚步之间走过的格数：原版每走一格累计 0.6，每满 1 响一次脚步（Entity.move）。
     */
    private static final double STEP_DISTANCE = 1 / 0.6;
    /** The cane's lit environment radius (BlindRules.CANE_ENVIRONMENT_RADIUS). 盲杖照亮环境的半径。 */
    private static final double CANE_ENVIRONMENT_RADIUS = 15;
    /** Players within this many blocks show during the cane window (BlindRules.CANE_PLAYER_RADIUS). 盲杖显示的玩家半径。 */
    private static final double CANE_PLAYER_RADIUS = 5;
    /** The cane window, 5 s (BlindRules.CANE_ACTIVE_TICKS). 盲杖窗口 5 秒。 */
    private static final int CANE_ACTIVE_TICKS = 100;
    /** The cane sweep expands over 0.6 s (BlindEchoUniforms.CANE_EXPAND_SECONDS). 盲杖扫描 0.6 秒扩张完。 */
    private static final int CANE_EXPAND_TICKS = 12;
    /** Ponder's outliner fades an outline over 8 ticks once it is no longer kept. 不再保持后，描边在 8 tick 内淡出。 */
    private static final int OUTLINE_FADE_TICKS = 8;
    /** A standing player's box height, as Actors.highlight draws it. 站立玩家方框的高度，与 Actors.highlight 相同。 */
    private static final double STANDING_BOX_HEIGHT = 1.95;
    /** A crouching player is 1.5 tall instead of 1.8. 潜行的玩家高 1.5 而不是 1.8。 */
    private static final double CROUCHING_BOX_HEIGHT = 1.65;

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
        role("sparkassist:roles/sparkwitch/seeker", List.of("sparkwitch"),
                scene("wathe/cabin", SparkWitchCivilianScenes::seeker));
        role("sparkassist:roles/sparkwitch/insider", List.of("sparkwitch", "noellesroles"),
                scene("sparkwitch/civ_train_cabin", SparkWitchCivilianScenes::insider));
        role("sparkassist:roles/sparkwitch/saint", List.of("sparkwitch"),
                scene("wathe/aisle", SparkWitchCivilianScenes::saint));
        role("sparkassist:roles/sparkwitch/orthopedist", List.of("sparkwitch"),
                scene("wathe/aisle", SparkWitchCivilianScenes::orthopedist));
        role("sparkassist:roles/sparkwitch/apprentice_witch", List.of("sparkwitch"),
                scene("wathe/lit_carriage", SparkWitchCivilianScenes::apprenticeWitch));
        role("sparkassist:roles/sparkwitch/emma", List.of("sparkwitch"),
                scene("wathe/aisle", SparkWitchCivilianScenes::emma));
        role("sparkassist:roles/sparkwitch/perfumer", List.of("sparkwitch", "sparkstrength"),
                scene("wathe/aisle", SparkWitchCivilianScenes::perfumer));
        role("sparkassist:roles/sparkwitch/fiend", List.of("sparkwitch"),
                scene("wathe/cabin", SparkWitchCivilianScenes::fiend));
        role("sparkassist:roles/sparkwitch/blind", List.of("sparkwitch"),
                scene("sparkwitch/civ_blind_carriage", SparkWitchCivilianScenes::blind));

        // Items whose use a demo above already shows; holding W over the item plays that demo.
        // 上面的演示已经演示过用法的物品；在物品上按住 W 即可播放对应演示。
        for (String id : List.of("sparkwitch:fishing_rod", "sparkwitch:salmon", "sparkwitch:key_fish")) {
            item(id, List.of("sparkwitch"), scene("sparkwitch/civ_tray_cabin", SparkWitchCivilianScenes::fisher));
        }
        for (String id : List.of("sparkwitch:usec_rifle", "sparkwitch:usec_338_fmj", "sparkwitch:usec_338_ap")) {
            item(id, List.of("sparkwitch"), scene("sparkwitch/civ_sniper_wall", SparkWitchCivilianScenes::usec));
        }
        for (String id : List.of("sparkwitch:taser", "sparkwitch:shock_device")) {
            item(id, List.of("sparkwitch"), scene("wathe/aisle", SparkWitchCivilianScenes::controlExpert));
        }
        for (String id : List.of("sparkwitch:seeker_car", "sparkwitch:seeker_camera")) {
            item(id, List.of("sparkwitch"), scene("wathe/cabin", SparkWitchCivilianScenes::seeker));
        }
        item("noellesroles:neutral_master_key", List.of("sparkwitch", "noellesroles"),
                scene("sparkwitch/civ_train_cabin", SparkWitchCivilianScenes::insider));
        item("sparkwitch:holy_flash", List.of("sparkwitch"), scene("wathe/aisle", SparkWitchCivilianScenes::saint));
        for (String id : List.of("sparkwitch:perfume_essence", "sparkstrength:cooling_oil")) {
            item(id, List.of("sparkwitch", "sparkstrength"), scene("wathe/aisle", SparkWitchCivilianScenes::perfumer));
        }
        for (String id : List.of("sparkwitch:white_cane", "sparkwitch:comtac_viii")) {
            item(id, List.of("sparkwitch"), scene("sparkwitch/civ_blind_carriage", SparkWitchCivilianScenes::blind));
        }
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
     * Seeker (SeekerRules, SeekerCameraItem, SeekerCarItem, SeekerDeviceService, SeekerPlacementRules,
     * SeekerQuickConnectHandler, SeekerCarUseRules, SeekerCameraEntity, SeekerCameraLookRules, SeekerDeviceHits,
     * SeekerMarkService; client SeekerCarEntityRenderer, SeekerCameraEntityRenderer, SeekerMarkClientHooks): a camera
     * from the shop is mounted with a right-click on a full block face within 4.5 blocks, never within a block of a
     * door, bed, seat or other interactive block; the car the role starts with is set down at the feet or up to 3
     * blocks ahead. Neither swings the arm (CONSUME), and the camera, the car and the tablet are hidden in hand from
     * other living players. The secondary skill key (N) or the tablet connects to a device and the body then stands
     * still. The car drives at 7.5 blocks a second; its own right-click toggles a door, gate, button or lever ahead
     * like an empty hand (key-locked and jammed doors refuse, and train doors during a round). Viewing a camera turns
     * its head with the Seeker's look and lights its LED, which everyone sees. One hit (any melee, a gun, a blast)
     * breaks a device, with a sound and no particles; the breaker then wears a red outline for 10 s that only the
     * Seeker sees, provided a tablet is in his inventory. The devices are drawn with their own placed models, as the
     * entity renderers draw them; the outline is a box.
     * 搜寻者：商店买的摄像头右键装在 4.5 格内的完整方块面上，不能装在门、床、座位等可交互方块 1 格之内；身份自带的小车放在
     * 脚下或前方 3 格内。两者都不挥手（CONSUME），摄像头、小车和平板拿在手上时其他活着的玩家都看不见。第二技能键（N）或
     * 平板连上设备后，本体站着不动。小车每秒跑 7.5 格；它自己的右键像空手一样开关前方的门、栅栏门、按钮或拉杆（要钥匙的门、
     * 被卡住的门打不开，对局中的列车门也打不开）。观看摄像头时，它的机头跟着搜寻者的视角转动、指示灯亮起，人人可见。
     * 设备挨一下（任何近战、枪、爆炸）就坏，有声音、没有粒子；损坏者随后被红色描边 10 秒，只有搜寻者本人看得到，前提是
     * 他背包里有平板。设备用它们自己的放置模型绘制，与实体渲染器相同；描边用方框表示。
     */
    private static void seeker(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("role_seeker", "搜寻者：遥控小车与摄像头");
        WatheItemScenes.cabinStage(scene, util);
        BlockPos door = util.grid().at(3, 1, 3);
        ItemStack cameraItem = stack("sparkwitch:seeker_camera");
        ItemStack carItem = stack("sparkwitch:seeker_car");
        ElementLink<ActorElement> seeker = Actors.enter(scene, RoleColors.of("sparkwitch:seeker", 0x5C9EFF),
                Text.literal("搜寻者"), new Vec3d(1.5, 1, 1.0), SOUTH, Direction.DOWN);
        Actors.hold(scene, seeker, cameraItem);
        scene.idle(15);
        // The mount spot; the caption points there, as the 0.3-block camera is small. 安装位置；摄像头只有 0.3 格，字幕指向那里。
        Vec3d mountSpot = new Vec3d(1.5, 2.5, 2.85);
        scene.overlay().showText(80)
                .text("搜寻者在商店买摄像头，对着 4.5 格内的墙面右键装上")
                .pointAt(mountSpot)
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(40);
        scene.overlay().showControls(new Vec3d(1.5, 3.1, 2.9), Pointing.DOWN, 30).rightClick().withItem(cameraItem);
        scene.idle(10);
        // SeekerPlacementRules.cameraEntityPos: the 0.3 cube against the wall's north face, centred on the aim.
        // 0.3 的立方体贴着墙的北面，以准星为中心。
        ElementLink<Gadget> camera = gadget(scene, new Gadget(cameraItem, new Vec3d(1.5, 2.35, 2.85), NORTH,
                Direction.NORTH));
        Actors.hold(scene, seeker, ItemStack.EMPTY);
        scene.idle(40);
        Actors.walk(scene, seeker, new Vec3d(4.0, 0, 0.3), 30);
        scene.idle(32);
        Actors.turn(scene, seeker, WEST);
        Actors.hold(scene, seeker, carItem);
        scene.idle(10);
        Vec3d overHead = new Vec3d(5.5, 3.6, 1.3);
        scene.overlay().showText(80)
                .text("自带的小车开局冷却 60 秒；右键放到脚下或前方 3 格内")
                .independent()
                .attachKeyFrame();
        scene.idle(30);
        scene.overlay().showControls(overHead, Pointing.DOWN, 30).rightClick().withItem(carItem);
        scene.idle(10);
        // SeekerPlacementRules.fallbackCarOrigin: 0.8 ahead of the feet, facing the owner's way.
        // 脚前 0.8 格处，朝向与放置者相同。
        ElementLink<Gadget> car = gadget(scene, new Gadget(carItem, new Vec3d(4.7, 1, 1.3), WEST, null));
        scene.idle(50);
        scene.overlay().showControls(overHead, Pointing.DOWN, 30).showing(key(SECONDARY_KEY, "N"));
        scene.overlay().showText(90)
                .text("按第二技能键（默认 N）或右键平板连上小车，本体站着不动")
                .independent()
                .attachKeyFrame();
        scene.idle(40);
        scene.idle(drive(scene, car, new Vec3d(-1.2, 0, 0)) + 2);
        steer(scene, car, SOUTH, 4);
        scene.idle(6);
        scene.idle(drive(scene, car, new Vec3d(0, 0, 0.9)) + 6);
        scene.overlay().showControls(new Vec3d(3.5, 1.7, 2.2), Pointing.DOWN, 25).rightClick();
        scene.idle(8);
        WatheItemScenes.openDoor(scene, door, true);
        scene.overlay().showText(90)
                .text("小车跑得快，右键能开关门（上锁、卡住的门和列车门开不了）")
                .independent()
                .attachKeyFrame();
        scene.idle(10);
        scene.idle(drive(scene, car, new Vec3d(0, 0, 2.6)) + 4);
        steer(scene, car, -60, 6);
        scene.idle(66);
        scene.overlay().showControls(overHead, Pointing.DOWN, 30).showing(key(SECONDARY_KEY, "N"));
        scene.idle(8);
        lightUp(scene, camera, true);
        scene.overlay().showText(90)
                .text("再按 N 切到摄像头：别人看得到它跟着转、指示灯变亮")
                .pointAt(mountSpot)
                .placeNearTarget()
                .attachKeyFrame();
        aim(scene, camera, 140, 10);
        // Doors close by themselves 5 s after opening. 门打开 5 秒后自动关上。
        scene.idle(5);
        WatheItemScenes.openDoor(scene, door, false);
        scene.idle(25);
        aim(scene, camera, 220, 5);
        scene.idle(30);
        ElementLink<ActorElement> killer = Actors.enter(scene, RoleColors.KILLER, Text.literal("杀手"),
                new Vec3d(0.5, 1, 0.4), -40, Direction.DOWN);
        Actors.hold(scene, killer, stack("wathe:knife"));
        // The head follows the Seeker's look onto the newcomer. 机头随搜寻者的视线转向来人。
        aim(scene, camera, 158, 5);
        scene.idle(25);
        Actors.walk(scene, killer, new Vec3d(1.0, 0, 1.2), 14);
        aim(scene, camera, 180, 10);
        scene.idle(16);
        Actors.turn(scene, killer, SOUTH);
        scene.idle(6);
        scene.overlay().showControls(new Vec3d(1.5, 3.4, 2.85), Pointing.DOWN, 25).leftClick();
        scene.idle(8);
        Actors.swing(scene, killer);
        removeGadget(scene, camera);
        Actors.highlight(scene, killer, MARK_RED, 200);
        scene.overlay().showText(90)
                .colored(PonderPalette.RED)
                .text("别人一般一下就能打坏设备，打坏的人被红框标记 10 秒")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showText(90)
                .text("红框只有你看得到（背包里要有平板）；手里的设备别人也看不见")
                .independent();
        scene.idle(100);
        scene.markAsFinished();
    }

    /**
     * Insider (InsiderEquipmentService, InsiderDoorService, InsiderDoorRules, InsiderRules; NoellesRoles
     * HiddenEquipmentHelper, Wathe SmallDoorBlock and TrainDoorBlock; client InsiderCohortRules): the role is dealt a
     * neutral master key, hidden in hand from other living players. Right-clicked on a closed train door or a
     * key-locked room door it opens the door (a swing) and then cools down 10 s; while it cools the door refuses
     * without a swing. Jammed doors refuse it. Doors close by themselves 5 s after opening. With the crosshair on the
     * other within 2 blocks, the Insider and the Corrupt Cop see a gold "嘉豪同伙" under Wathe's name display (hidden in
     * a blackout); once only Team Jiahao members are alive, the whole team wins, dead members too.
     * 内应：身份自带一把中立万能钥匙，拿在手上时其他活着的玩家看不见。右键关着的列车门或上锁的房门，门会打开（挥手），之后
     * 钥匙冷却 10 秒；冷却期间门没有反应，也不挥手。被卡住的门打不开。门打开 5 秒后自动关上。内应和黑警在 2 格内准星对着对方时，
     * Wathe 的名字显示下方出现金色的“嘉豪同伙”（停电时不显示）；只剩嘉豪阵营成员活着时全队获胜，已死的成员也算。
     */
    private static void insider(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("role_insider", "内应：中立万能钥匙");
        WatheItemScenes.setStage(scene, util);
        BlockPos trainDoor = util.grid().at(5, 1, 3);
        BlockPos cabinDoor = util.grid().at(2, 1, 3);
        ItemStack key = stack("noellesroles:neutral_master_key");
        ElementLink<ActorElement> insider = Actors.enter(scene, RoleColors.of("sparkwitch:insider", 0x00FFD0),
                Text.literal("内应"), new Vec3d(3.5, 1, 1.4), SOUTH, Direction.DOWN);
        Actors.hold(scene, insider, key);
        scene.idle(15);
        scene.overlay().showText(80)
                .text("内应开局拿到一把中立万能钥匙，别人看不见你手里的它")
                .independent()
                .attachKeyFrame();
        scene.idle(60);
        Actors.walk(scene, insider, new Vec3d(2.0, 0, 0.2), 20);
        scene.idle(22);
        Actors.turn(scene, insider, SOUTH);
        scene.idle(8);
        scene.overlay().showControls(new Vec3d(5.5, 3.6, 1.6), Pointing.DOWN, 30).rightClick().withItem(key);
        scene.idle(10);
        Actors.swing(scene, insider);
        WatheItemScenes.openDoor(scene, trainDoor, true);
        scene.overlay().showText(80)
                .text("右键列车门或上锁的房门就能打开（被卡住的门不行）")
                .independent()
                .attachKeyFrame();
        scene.idle(40);
        Actors.walk(scene, insider, new Vec3d(-3.0, 0, 0), 24);
        scene.idle(26);
        Actors.turn(scene, insider, SOUTH);
        scene.idle(8);
        scene.overlay().showControls(new Vec3d(2.5, 3.6, 1.6), Pointing.DOWN, 30).rightClick().withItem(key);
        scene.idle(10);
        // The key cools down: DoorInteraction answers DENY, so nothing happens and the arm stays still.
        // 钥匙冷却中：DoorInteraction 返回 DENY，什么都不发生，手也不挥。
        scene.overlay().showOutline(PonderPalette.RED, "door", util.select().fromTo(2, 1, 3, 2, 2, 3), 80);
        scene.overlay().showText(80)
                .colored(PonderPalette.RED)
                .text("开门后钥匙冷却 10 秒，这期间再开就没反应")
                .independent()
                .attachKeyFrame();
        // Doors close by themselves 5 s after opening. 门打开 5 秒后自动关上。
        scene.idle(16);
        WatheItemScenes.openDoor(scene, trainDoor, false);
        scene.idle(74);
        scene.overlay().showText(80)
                .text("10 秒冷却过后，上锁的房门也一样能开")
                .independent()
                .attachKeyFrame();
        scene.idle(20);
        scene.overlay().showControls(new Vec3d(2.5, 3.6, 1.6), Pointing.DOWN, 30).rightClick().withItem(key);
        scene.idle(10);
        Actors.swing(scene, insider);
        WatheItemScenes.openDoor(scene, cabinDoor, true);
        scene.idle(60);
        ElementLink<ActorElement> cop = Actors.enter(scene, RoleColors.of("noellesroles:corrupt_cop", 0x193264),
                Text.literal("黑警"), new Vec3d(0.4, 1, 0.9), EAST, Direction.DOWN);
        scene.idle(10);
        Actors.walk(scene, cop, new Vec3d(0.8, 0, 0.3), 12);
        scene.idle(10);
        Actors.turn(scene, insider, WEST);
        scene.idle(20);
        WatheItemScenes.openDoor(scene, cabinDoor, false);
        scene.overlay().showText(90)
                .text("你和黑警互为同伙：2 格内准星对着对方时显示金色“嘉豪同伙”")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showText(80)
                .text("只剩嘉豪阵营（你和黑警）的人活着时，全队一起获胜")
                .independent();
        scene.idle(90);
        scene.markAsFinished();
    }

    /**
     * Saint (SaintRules, SaintFeatureService, GameFunctionsSaintProtectionMixin, SaintAbilityService,
     * SaintKarmaService, HolyFlashItem, HolyFlashEntity, HolyFlashRules, HolyFlashTargeting; Wathe GunShootPayload): a
     * kill by a player of the effective civilian faction is cancelled at Wathe's kill entry (a few forced kills and the
     * Ceremonial Sword pierce it, and an Impostor is no civilian). The shot itself still happens: muzzle flash, and
     * Wathe's innocent-shot punishment runs 4 ticks later (default: the revolver drops and the shooter dies). Hellfire
     * costs 100 coins on the ability key and runs 15 s, shown only on the Saint's HUD; a non-civilian (not the Grand
     * Witch) who kills him then gains Karma: every later kill puts all their items on a 5 s cooldown (20 s for the
     * Bomber). The Holy Flash (visible in hand) is thrown like Wathe's grenade with a swing; it bursts on its first
     * block or player (big explosion, 40 end rods, 24 firework sparks) and blacks out the screen of every affectable
     * player whose body is within 6 blocks and who has a line of sight to it, the thrower too: 10 s at the burst down
     * to 3 s at the edge, x0.75 facing away. Only the flashed players see it; nobody else sees a change. All Holy
     * Flashes share a 15 s cooldown. The gold arc marks the 6-block edge.
     * 圣徒：实际阵营为好人的玩家造成的击杀会在 Wathe 的击杀入口被取消（少数强制击杀与仪礼剑例外，内鬼也不算好人）。开枪本身
     * 照常发生：有枪口火光，Wathe 的误杀惩罚在 4 tick 后执行（默认丢枪并处死开枪者）。业火：按技能键花 100 金币，持续 15 秒，
     * 只显示在圣徒自己的屏幕上；此时杀死他的非好人（大魔女除外）背上业障：之后每次杀人，身上所有物品冷却 5 秒（炸弹客 20 秒）。
     * 圣光弹（拿在手上别人看得到）像 Wathe 手雷一样挥手扔出，碰到第一个方块或玩家就爆开（大爆炸、40 颗末地烛光点、24 颗烟花
     * 火花），让身体在 6 格内、与爆点之间没有遮挡的可影响玩家黑屏，投掷者本人也算：爆点处 10 秒，边缘 3 秒，背对 ×0.75。
     * 只有被闪的人自己看得到，旁人看不出变化。所有圣光弹共用 15 秒冷却。金色弧线示意 6 格边界。
     */
    private static void saint(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("role_saint", "圣徒：好人一般杀不死你");
        WatheItemScenes.stage(scene);
        ItemStack revolver = stack("wathe:revolver");
        ItemStack flash = stack("sparkwitch:holy_flash");
        ElementLink<ActorElement> saint = Actors.enter(scene, RoleColors.of("sparkwitch:saint", 0xEEBC78),
                Text.literal("圣徒"), new Vec3d(3.2, 1, 3.8), SCREEN_LEFT, Direction.DOWN);
        Vec3d shooterFeet = new Vec3d(5.8, 1, 1.2);
        ElementLink<ActorElement> vigilante = Actors.enter(scene, RoleColors.VIGILANTE, Text.literal("义警"),
                shooterFeet, SCREEN_RIGHT, Direction.DOWN);
        Actors.hold(scene, vigilante, revolver);
        scene.idle(15);
        scene.overlay().showText(80)
                .text("好人阵营的人一般杀不死圣徒")
                .independent()
                .attachKeyFrame();
        scene.idle(50);
        scene.overlay().showControls(shooterFeet.add(0, 2.6, 0), Pointing.DOWN, 30).rightClick().withItem(revolver);
        scene.idle(10);
        // The muzzle of a levelled revolver held by an actor at (5.8, 1, 1.2) facing screen-right, as in
        // controlExpert; RevolverItem kicks the view up 4 degrees. 与 controlExpert 相同的端平左轮枪口；开枪时视角上抬 4 度。
        gunshotFlash(scene, new Vec3d(5.2, 2.05, 1.8));
        Actors.lookPitch(scene, vigilante, -4);
        scene.idle(3);
        Actors.lookPitch(scene, vigilante, 0);
        scene.idle(1);
        // GunShootPayload: 4 ticks later the revolver drops ahead and the shooter dies (SHOT_INNOCENT).
        // 4 tick 后左轮掉在前方，开枪者死亡。
        Actors.hold(scene, vigilante, ItemStack.EMPTY);
        ElementLink<ThrownElement> dropped = Actors.toss(scene, revolver, new Vec3d(5.4, 2.0, 1.6),
                new Vec3d(4.6, 1.05, 2.4), 8, 0.3, true, ThrownElement.Flight.SPIN);
        Actors.fall(scene, vigilante);
        scene.overlay().showText(100)
                .colored(PonderPalette.RED)
                .text("好人朝你开枪你不会死，开枪的人照样受罚（默认丢枪并死亡）")
                .independent()
                .attachKeyFrame();
        scene.idle(110);
        Actors.leave(scene, vigilante, Direction.UP);
        Actors.remove(scene, dropped);
        scene.idle(10);
        Actors.walk(scene, saint, new Vec3d(3.3, 0, -3.3), 30);
        scene.idle(32);
        Vec3d overHead = new Vec3d(6.5, 3.6, 0.5);
        scene.overlay().showControls(overHead, Pointing.DOWN, 40).showing(key(ABILITY_KEY, "G"));
        scene.overlay().showText(90)
                .text("花 100 金币按技能键点燃业火：持续 15 秒，别人看不出来")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showText(90)
                .text("业火中杀你的人一般会背上业障，此后每次行凶都会被缴械几秒")
                .independent();
        scene.idle(60);
        Actors.turn(scene, saint, SCREEN_RIGHT);
        Actors.hold(scene, saint, flash);
        scene.idle(30);
        ElementLink<ActorElement> killer = Actors.enter(scene, RoleColors.KILLER, Text.literal("杀手"),
                new Vec3d(0.4, 1, 6.6), SCREEN_LEFT, Direction.DOWN);
        Actors.hold(scene, killer, stack("wathe:knife"));
        scene.idle(10);
        Actors.walk(scene, killer, new Vec3d(0.8, 0, -0.8), 16);
        scene.overlay().showText(70)
                .text("被人追时，把圣光弹扔到追兵面前")
                .independent()
                .attachKeyFrame();
        // Thrown 10 degrees up at 0.5 blocks a tick (gravity 0.03, drag 0.99), it comes down about 6.5 blocks away,
        // just ahead of him, after about 15 ticks. 以每 tick 0.5 格向上 10 度扔出（重力 0.03、阻力 0.99），约 15 tick 后
        // 落在约 6.5 格外、他的面前。
        Actors.lookPitch(scene, saint, -10);
        scene.overlay().showControls(overHead, Pointing.DOWN, 25).rightClick().withItem(flash);
        scene.idle(8);
        Actors.swing(scene, saint);
        Actors.hold(scene, saint, ItemStack.EMPTY);
        Vec3d burst = new Vec3d(1.8, 1.1, 5.2);
        throwSprite(scene, new SpriteFlight(flash, new Vec3d(6.2, 2.5, 0.8), burst.add(0, -0.1, 0), 15, 0.8, null));
        scene.idle(15);
        // HolyFlashEntity.burst, 0.1 above the floor it hit. 在被击中的地面上方 0.1 处爆开。
        spawnParticles(scene, WatheParticles.BIG_EXPLOSION, burst, 1, Vec3d.ZERO, 0);
        spawnParticles(scene, ParticleTypes.END_ROD, burst, 40, new Vec3d(0.4, 0.4, 0.4), 0.25);
        spawnParticles(scene, ParticleTypes.FIREWORK, burst, 24, new Vec3d(0.4, 0.4, 0.4), 0.25);
        edge(scene, burst.add(0, -0.05, 0), 6, 90);
        Actors.lookPitch(scene, saint, 0);
        scene.idle(10);
        scene.overlay().showText(90)
                .text("6 格内的人一般都会眼前一黑、耳鸣（金色弧线示意 6 格）")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showText(90)
                .text("你自己在 6 格内也会被闪；所有圣光弹共用 15 秒冷却")
                .independent();
        scene.idle(100);
        scene.markAsFinished();
    }

    /**
     * Orthopedist (OrthopedistSkillService, OrthopedistTargeting, OrthopedistRules, BoneSettingEffect; client
     * OrthopedistClientHooks; Hunter HunterTrapEntity, HunterPlayerComponent, HunterInjuryState, HunterRules): the
     * ability key, aimed at a visible living player within 3 blocks (no swing), gives 20 s of Bone Setting and 5 s of
     * Speed I, both with particles everyone sees (one swirl every fourth tick from a random running effect), and starts
     * a 60 s cooldown (30 s at round start). The Orthopedist alone sees Bone Set players in his colour while in sight.
     * A Hunter trap, drawn for only a few roles (the item lies in for it), still roots its victim 3 s amid 8 crits
     * and is used up, but Bone Setting is consumed instead of a fracture layer. On a fractured player the skill heals
     * one layer instead.
     * 骨科大夫：对准 3 格内看得见的活人按技能键（不挥手），给对方 20 秒正骨和 5 秒速度 I，两者的粒子人人可见（约每 4 tick
     * 从正在生效的效果中随机冒出一颗），随后冷却 60 秒（开局 30 秒）。只有骨科大夫本人能看到视线内正骨中的人以他的颜色高亮。
     * 猎人的捕兽夹只对少数身份绘制（用物品代替），照样把人定身 3 秒、冒出 8 颗暴击粒子并消失，但消耗的是正骨，而不是叠一层
     * 骨折。对已骨折的人使用则改为治好一层骨折。
     */
    private static void orthopedist(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("role_orthopedist", "骨科大夫：正骨抵掉捕兽夹骨折");
        WatheItemScenes.stage(scene);
        int olive = RoleColors.of("sparkwitch:orthopedist", 0x90B358);
        Vec3d feet = new Vec3d(5.2, 1, 1.6);
        Actors.enter(scene, olive, Text.literal("骨科大夫"), feet, SCREEN_RIGHT, Direction.DOWN);
        // A plain civilian: Vigilantes, Veterans, Corrupt Cops and Engineers see a trap in sight (HunterRules).
        // 普通平民：义警、老兵、黑警和工程师看得见视线内的捕兽夹。
        ElementLink<ActorElement> passenger = Actors.enter(scene, RoleColors.CIVILIAN, Text.literal("平民"),
                new Vec3d(3.3, 1, 3.5), SCREEN_LEFT, Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(80)
                .text("骨科大夫对准 3 格内的人按技能键（默认 G）正骨")
                .independent()
                .attachKeyFrame();
        scene.idle(40);
        scene.overlay().showControls(feet.add(0, 2.6, 0), Pointing.DOWN, 30).showing(key(ABILITY_KEY, "G"));
        scene.idle(10);
        Swirls swirls = swirls(scene, passenger, new Swirl(BONE_SETTING_SWIRL, 20 * 20),
                new Swirl(SPEED_SWIRL, 5 * 20));
        scene.idle(30);
        scene.overlay().showText(90)
                .text("对方得到 20 秒正骨和 5 秒速度，冒出人人可见的药水粒子")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        // The Orthopedist's own Bone Setting outline, shown with its caption, lasts until the trap consumes the effect.
        // 骨科大夫自己看到的正骨高亮随字幕出现，持续到捕兽夹消耗掉效果为止。
        Actors.highlight(scene, passenger, olive, 174);
        scene.overlay().showText(80)
                .text("正骨中的人在你眼里高亮（绿框示意，只有你看得到）")
                .independent();
        scene.idle(90);
        Vec3d trapAt = new Vec3d(3.3, 1.0, 5.8);
        ElementLink<ThrownElement> trap = Actors.toss(scene, stack("sparkwitch:hunter_trap"), trapAt.add(0, 0.3, 0),
                trapAt, 2, 0, true, ThrownElement.Flight.LIES_FLAT);
        scene.overlay().showText(90)
                .text("猎人的捕兽夹一般只有杀手等少数人看得见（这里用物品代替）")
                .independent()
                .attachKeyFrame();
        scene.idle(60);
        Actors.turn(scene, passenger, SOUTH);
        scene.idle(10);
        // Walking straight at the trap, his box meets its trigger box (the trap's 0.5 box grown 0.35 a side) with the
        // feet 0.9 short of its centre. 径直走向夹子时，脚离夹子中心 0.9 格处他的碰撞箱就碰到触发箱（夹子 0.5 的碰撞箱每边扩 0.35）。
        Actors.walk(scene, passenger, new Vec3d(0, 0, 1.4), 14);
        scene.idle(14);
        spawnParticles(scene, ParticleTypes.CRIT, trapAt.add(0, 0.05, 0), 8, new Vec3d(0.2, 0.05, 0.2), 0.05);
        Actors.remove(scene, trap);
        unswirl(scene, swirls);
        scene.overlay().showText(100)
                .colored(PonderPalette.GREEN)
                .text("正骨抵掉了这次骨折，但 3 秒的禁锢照样发生")
                .independent()
                .attachKeyFrame();
        scene.idle(60);
        Actors.walk(scene, passenger, new Vec3d(0, 0, 0.9), 10);
        scene.idle(50);
        scene.overlay().showText(90)
                .text("对已骨折的人用则治好 1 层骨折；用后冷却 60 秒")
                .independent();
        scene.idle(100);
        scene.markAsFinished();
    }

    /**
     * Apprentice Witch (WitchSkillAssignmentService, ApprenticeAbilityCatalog, MightyForceAbility,
     * MightyForceCombatService, ApprenticeRules, ApprenticeFeatureService, PurifyAbility; Wathe GameFunctions.killPlayer,
     * PlayerBodyEntity): at role assignment she draws one of five skills at random (Mighty Force, Swift Step, Murder
     * Sense, Healing, Clairvoyance), used on the ability key. Mighty Force costs 60 mana and opens a 10 s window in
     * which an empty-hand left-click on a player (the swing everyone sees) kills them (not forced, so a shield still
     * blocks) and launches the body: an impulse of 10 blocks a tick away from her plus 0.35 up, so in a carriage it
     * flies until a wall stops it, here within the tick; knockback sound. Killing an effective civilian follows Wathe's
     * innocent-shot punishment (default: she dies too). 45 s cooldown from the punch (or from the window running out).
     * After two tasks she graduates and the secondary key (N) purifies a Witch Factor (the aimed player within 6 blocks,
     * else herself if she holds one). The killer faces her, so his body lies face down towards her; in game its hitbox
     * stops at the wall and the drawn feet sink half a block into it, here they rest against it.
     * 预备魔女：分配身份时从五种技能中随机得到一种（巨力、滑步、杀意感知、疗愈、千里眼），用技能键发动。巨力花 60 魔力，
     * 打开 10 秒窗口，期间空手左键一名玩家（人人看得到挥手）即可击杀（非强制，护盾仍能挡下）并把尸体击飞：沿远离她的方向
     * 每 tick 10 格、向上 0.35 的冲量，所以在车厢里会一直飞到被墙挡住，这里在同一 tick 内就撞到墙；有击退音效。打死实际
     * 阵营为好人的人按 Wathe 的误杀惩罚处理（默认她也会死）。冷却 45 秒，从出拳（或窗口用完）时算起。做完 2 个任务出师后，
     * 第二技能键（N）可净化魔女因子（准心 6 格内的玩家，否则是持有因子的她自己）。杀手面朝她，所以尸体脸朝她扑倒；游戏里
     * 尸体的碰撞箱停在墙前，画出来的脚会陷进墙里半格，这里让脚抵着墙。
     */
    private static void apprenticeWitch(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("role_apprentice_witch", "预备魔女：巨力一拳击飞凶手");
        WatheItemScenes.setStage(scene, util);
        Vec3d feet = new Vec3d(1.2, 1, 2.8);
        Vec3d overHead = feet.add(0, 2.6, 0);
        ElementLink<ActorElement> apprentice = Actors.enter(scene,
                RoleColors.of("sparkwitch:apprentice_witch", 0x75EDFA), Text.literal("预备魔女"), feet, EAST,
                Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(80)
                .text("预备魔女开局一般随机得到五种技能之一，用技能键（默认 G）发动")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        ElementLink<ActorElement> killer = Actors.enter(scene, RoleColors.KILLER, Text.literal("杀手"),
                new Vec3d(5.2, 1, 2.8), WEST, Direction.DOWN);
        Actors.hold(scene, killer, stack("wathe:knife"));
        scene.idle(10);
        scene.overlay().showControls(overHead, Pointing.DOWN, 40).showing(key(ABILITY_KEY, "G"));
        scene.overlay().showText(80)
                .text("这局抽到巨力：花 60 魔力，之后 10 秒内空手左键一名玩家")
                .independent()
                .attachKeyFrame();
        scene.idle(20);
        Actors.walk(scene, killer, new Vec3d(-2.4, 0, 0), 36);
        scene.idle(38);
        Actors.charge(scene, killer, true);
        scene.idle(16);
        scene.overlay().showControls(overHead, Pointing.DOWN, 30).leftClick();
        scene.idle(8);
        Actors.swing(scene, apprentice);
        Actors.fall(scene, killer);
        // The body flies at 10 blocks a tick until the wall at x = 6 stops it; its feet end against the wall.
        // 尸体以每 tick 10 格的速度飞出，直到 x = 6 的墙挡住它；脚抵着墙。
        Actors.slide(scene, killer, new Vec3d(3.15, 0, 0), 2);
        scene.overlay().showText(90)
                .colored(PonderPalette.GREEN)
                .text("对方一般当场死亡，尸体被一拳打飞出去")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showText(90)
                .colored(PonderPalette.RED)
                .text("打死好人阵营的人会受罚，默认你也会死；巨力冷却 45 秒")
                .independent();
        scene.idle(100);
        scene.overlay().showText(90)
                .text("另外四种技能是滑步、杀意感知、疗愈和千里眼，详见指南")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showControls(overHead, Pointing.DOWN, 40).showing(key(SECONDARY_KEY, "N"));
        scene.overlay().showText(90)
                .text("做完 2 个任务出师后，第二技能键（默认 N）能净化魔女因子")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.markAsFinished();
    }

    /**
     * Emma (EmmaRules, EmmaSkillService, EmmaGunService, EmmaLifecycle; WitchFactorService, WitchFactorState,
     * WitchFactorWorldComponent, GrandWitchTargeting; client WitchFactorClientHooks, WitchFactorOutlineRules): the
     * ability key spends 100 mana to plant a Witch Factor on the player aimed at within 8 blocks (20 s cooldown), with
     * nothing to see. 20 s later the holder joins the network: Emma sees him outlined in blue-purple through walls (as do
     * the Grand Witch's side and other mature holders), and he in turn sees Emma's pink outline. When a holder is
     * killed, the factor passes to an eligible killer, who joins the network 20 s later. A revolver kill of a holder
     * cuts that shot's cooldown by 90% and gives 2 s of Speed III. Planting one on the Grand Witch, a Bewitched, an
     * Accomplice or the Witch Maiden kills Emma 20 s later. The outlines are boxes; the factor's 20 s waits are
     * shortened, and Wathe draws no bullet line, only the muzzle flash.
     * 樱羽艾玛：技能键花 100 魔力，给准心对准的 8 格内玩家种下魔女因子（冷却 20 秒），没有任何可见效果。20 秒后持有者
     * 接入网络：艾玛能隔墙看到他的蓝紫色描边（大魔女一方和其他成熟持有者也能），他也能看到艾玛的粉色描边。持有者被杀时，
     * 因子转到符合条件的凶手身上，凶手 20 秒后接入网络。用左轮打死持有者，这一枪冷却缩短 90%，并获得 2 秒速度 III。
     * 种给大魔女、魔化使、共犯或巫女，艾玛 20 秒后死亡。描边用方框表示；因子的 20 秒等待被缩短，Wathe 不画弹道线，
     * 只有枪口火光。
     */
    private static void emma(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("role_emma", "樱羽艾玛：魔女因子追踪凶手");
        WatheItemScenes.stage(scene);
        int pink = RoleColors.of("sparkwitch:emma", 0xF29BC3);
        Vec3d feet = new Vec3d(5.8, 1, 1.2);
        Vec3d overHead = feet.add(0, 2.6, 0);
        ItemStack revolver = stack("wathe:revolver");
        ElementLink<ActorElement> emma = Actors.enter(scene, pink, Text.literal("樱羽艾玛"), feet, SCREEN_RIGHT,
                Direction.DOWN);
        ElementLink<ActorElement> passenger = Actors.enter(scene, RoleColors.CIVILIAN, Text.literal("平民"),
                new Vec3d(3.3, 1, 3.7), SCREEN_LEFT, Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(80)
                .text("准心对准 8 格内的人按技能键（默认 G），花 100 魔力种下魔女因子")
                .independent()
                .attachKeyFrame();
        scene.idle(40);
        scene.overlay().showControls(overHead, Pointing.DOWN, 30).showing(key(ABILITY_KEY, "G"));
        scene.idle(50);
        // His outline lasts until he is stabbed. 他的描边持续到他被刺为止。
        Actors.highlight(scene, passenger, FACTOR_PURPLE, 148);
        scene.overlay().showText(90)
                .text("20 秒后（演示缩短），他一般会隔墙对你显示蓝紫色描边（框为示意）")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        ElementLink<ActorElement> killer = Actors.enter(scene, RoleColors.KILLER, Text.literal("杀手"),
                new Vec3d(1.0, 1, 6.0), SCREEN_LEFT, Direction.DOWN);
        Actors.hold(scene, killer, stack("wathe:knife"));
        scene.idle(10);
        Actors.walk(scene, killer, new Vec3d(1.2, 0, -1.2), 24);
        scene.idle(26);
        Actors.charge(scene, killer, true);
        scene.idle(12);
        Actors.charge(scene, killer, false);
        Actors.swing(scene, killer);
        Actors.fall(scene, passenger);
        scene.overlay().showText(80)
                .text("持有者被人杀死时，因子一般会转到凶手身上")
                .independent()
                .attachKeyFrame();
        scene.idle(20);
        Actors.walk(scene, killer, new Vec3d(-0.6, 0, 0.6), 12);
        scene.idle(70);
        // Both outlines last until the killer is shot: then no mature holder is left. 两个描边都持续到杀手中枪：此后没有成熟持有者了。
        Actors.highlight(scene, killer, FACTOR_PURPLE, 123);
        Actors.highlight(scene, emma, pink, 123);
        scene.overlay().showText(90)
                .text("再过 20 秒（演示缩短），凶手也会显形；他同样能看到你（粉框）")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        Actors.hold(scene, emma, revolver);
        scene.idle(10);
        scene.overlay().showControls(overHead, Pointing.DOWN, 30).rightClick().withItem(revolver);
        scene.idle(10);
        // The levelled revolver's muzzle, as in controlExpert; RevolverItem kicks the view up 4 degrees.
        // 与 controlExpert 相同的端平左轮枪口；开枪时视角上抬 4 度。
        gunshotFlash(scene, new Vec3d(5.2, 2.05, 1.8));
        Actors.lookPitch(scene, emma, -4);
        scene.idle(3);
        Actors.lookPitch(scene, emma, 0);
        Actors.fall(scene, killer);
        scene.idle(20);
        scene.overlay().showText(90)
                .colored(PonderPalette.GREEN)
                .text("用左轮打死持有者：这一枪冷却缩短 90%%，还能加速 2 秒")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showText(90)
                .colored(PonderPalette.RED)
                .text("别种给大魔女、魔化使、共犯或巫女：20 秒后你会死于反噬")
                .independent();
        scene.idle(100);
        scene.markAsFinished();
    }

    /**
     * Perfumer (PerfumeEssenceItem, PerfumerRules, PerfumerState, PerfumerRuntime, PerfumerFeatureService, CologneItem;
     * SparkStrength CoolingOilItem, CoolingOilEntity, CoolingOilService, PerfumerKitService, HiddenEquipmentHelperMixin):
     * right-clicking another living player with Perfume Essence (a swing; the essence is used up and hidden in hand from
     * other living players) marks them for the Perfumer alone: pink while in sight within 12 blocks. After the marked
     * player kills someone (an attributed kill), the outline turns red and also shows through walls within 4 blocks.
     * Bodies show grey (4 blocks through walls, 12 in sight; hidden ones excepted). Cooling Oil is thrown like a
     * splash potion (a swing, the vial visible in flight) and shatters on the first block or player: mint mist and
     * glass bits; every player but the thrower and Perfumers within 2.5 blocks (cover permitting) gets 5 s of blurred
     * vision and no instinct. Any kill credited to the marked player counts (poison and bombs too). The outlines are
     * boxes; the corpse outline shows from the kill.
     * 调香师：用香精右键另一名活人（会挥手；香精用掉，手持时其他活人看不见）只给调香师自己做记号：12 格内看得见时显示粉色。
     * 被标记者之后杀了人（有记名的击杀），描边变红，4 格内隔墙也能看到。尸体显示灰色（4 格内隔墙、12 格内需视线；被藏起的
     * 除外）。风油精像喷溅药水一样扔出（会挥手，飞行中的药瓶看得见），碰到第一个方块或玩家就碎：薄荷色雾气和玻璃碎片；
     * 2.5 格内（受遮挡判定）除投掷者和调香师外的人 5 秒内视野模糊、不能用本能。记在被标记者名下的击杀都算（下毒、炸弹也算）。
     * 描边用方框表示；尸体描边从击杀起就出现。
     */
    private static void perfumer(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("role_perfumer", "调香师：香精标记与血腥气味");
        WatheItemScenes.stage(scene);
        ItemStack essence = stack("sparkwitch:perfume_essence");
        ItemStack oil = stack("sparkstrength:cooling_oil");
        int rose = RoleColors.of("sparkwitch:perfumer", 0xF2A4A4);
        ElementLink<ActorElement> perfumer = Actors.enter(scene, rose, Text.literal("调香师"), new Vec3d(5.6, 1, 1.4),
                SCREEN_RIGHT, Direction.DOWN);
        Actors.hold(scene, perfumer, essence);
        ElementLink<ActorElement> killer = Actors.enter(scene, RoleColors.KILLER, Text.literal("杀手"),
                new Vec3d(3.8, 1, 3.2), SCREEN_LEFT, Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(80)
                .text("用香精右键一名活人做记号；其他活人看不见你手里的香精")
                .independent()
                .attachKeyFrame();
        scene.idle(20);
        Actors.walk(scene, perfumer, new Vec3d(-0.8, 0, 0.8), 14);
        scene.idle(16);
        Vec3d overHead = new Vec3d(4.8, 3.6, 2.2);
        scene.overlay().showControls(overHead, Pointing.DOWN, 30).rightClick().withItem(essence);
        scene.idle(10);
        Actors.swing(scene, perfumer);
        Actors.hold(scene, perfumer, ItemStack.EMPTY);
        // Pink until his kill turns it red. 粉色持续到他杀人后变红。
        Actors.highlight(scene, killer, rose, 182);
        scene.idle(40);
        scene.overlay().showText(90)
                .text("粉框示意：他在 12 格内、你看得见他时，只有你能看到这层描边")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        Vec3d victimFeet = new Vec3d(1.0, 1, 4.6);
        ElementLink<ActorElement> victim = Actors.enter(scene, RoleColors.CIVILIAN, Text.literal("平民"), victimFeet,
                SOUTH, Direction.DOWN);
        Actors.hold(scene, killer, stack("wathe:knife"));
        scene.idle(10);
        Actors.walk(scene, killer, new Vec3d(-1.4, 0, 0.7), 18);
        scene.idle(20);
        Actors.charge(scene, killer, true);
        scene.idle(12);
        Actors.charge(scene, killer, false);
        Actors.swing(scene, killer);
        Actors.fall(scene, victim);
        Actors.highlight(scene, killer, BLOODY_RED, 406);
        bodyOutline(scene, victimFeet, SOUTH, CORPSE_GREY, 406);
        scene.overlay().showText(90)
                .colored(PonderPalette.RED)
                .text("他之后杀了人（下毒、炸死也算），描边变红，4 格内隔墙也看得到")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showText(80)
                .text("尸体一般也会对你高亮（灰框示意）")
                .independent();
        scene.idle(90);
        Actors.hold(scene, perfumer, oil);
        Actors.walk(scene, killer, new Vec3d(1.0, 0, -0.7), 14);
        scene.idle(16);
        scene.overlay().showControls(overHead, Pointing.DOWN, 25).rightClick().withItem(oil);
        scene.idle(8);
        Actors.swing(scene, perfumer);
        Actors.hold(scene, perfumer, ItemStack.EMPTY);
        // Thrown at 1.5 blocks a tick, the vial meets him within a tick: it breaks where its path enters his box grown
        // 0.3 a side, pulled 0.2 back (PerfumerKitService.impactPoint), and the mist settles onto the floor below.
        // 以每 tick 1.5 格扔出，药瓶一 tick 内就砸中他：在路径进入他每边扩 0.3 的碰撞箱处、再后退 0.2 碎裂，雾气落到下方地面。
        Vec3d impact = new Vec3d(4.15, 2.25, 2.7);
        throwSprite(scene, new SpriteFlight(oil, new Vec3d(4.5, 2.4, 2.45), impact, 2, 0, null));
        scene.idle(2);
        coolingOilBurst(scene, oil, impact, new Vec3d(impact.x, 1.2, impact.z));
        scene.overlay().showText(90)
                .text("风油精一碰就碎：溅到的人 5 秒内视野模糊、不能用本能")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showText(80)
                .text("调香师自己不受风油精影响；古龙水等其他香料见指南")
                .independent();
        scene.idle(90);
        scene.markAsFinished();
    }

    /**
     * Fiend (FiendRules, FiendImmunityService, FiendReactionService, FiendCooldownAura, FiendShopService,
     * FiendMomentService, FiendMomentEffects, FiendMomentCrowbar, FiendDashService, FiendDashRules; client
     * FiendMomentHighlightRules, WatheClientFiendHighlightMixin; Wathe CrowbarItem, DoorBlockEntity.blast): every kill
     * on the dormant Fiend is cancelled except falling off the train (and a disconnect or /kill): the attacker's stab or
     * shot happens, the Fiend stays standing. A knife stab pays 50 coins and 4 notes, a gunshot 50 coins and 5 s of
     * Speed III (no particles) and raises the items of everyone else within 8 blocks to a 20 s cooldown. The 200-coin
     * Fiend Moment (one per round) ends the immunity and gives Speed II and a crowbar; for its 2 minutes the Fiend sees
     * every living player outlined in his colour through walls and everyone sees him (not while Cooling Oil blinds the
     * viewer, nor hidden Wraiths). The door's own right-click runs first, so the crowbar reaches a locked or jammed
     * door (any door on a sneaking right-click) and blasts it open for good unless an Engineer repairs it (a swing and a
     * loud pry), then cools down 5 s. Dash (ability key, moment only): 10 s of
     * Speed IV, 30 s cooldown. Surviving the moment wins alone. Wathe draws no bullet line, only the muzzle flash; the
     * outlines are boxes.
     * 魔人：休眠时针对他的击杀全部被取消，只有掉下火车（以及断线、/kill）例外：对方照常出刀或开枪，魔人站着不倒。挨刀得
     * 50 金币和 4 张便条，中枪得 50 金币和 5 秒速度 III（无粒子），并让 8 格内其他人的物品冷却提升到 20 秒。花 200 金币
     * 买下魔人时刻（每局一次）会失去免疫，获得速度 II 和一把撬棍；时刻持续 2 分钟，期间魔人隔墙看到所有活人以他的颜色
     * 描边，所有人也看得到他（被风油精糊眼的人和隐藏的冤魂除外）。门自己的右键先生效，所以撬棍只作用于上锁或卡住的门（潜行
     * 右键时任何门都行），把门撬开且不再关上，除非工程师修好它（挥手，撬门声很响），之后冷却 5 秒。疾驰（技能键，仅限时刻中）：
     * 10 秒速度 IV，冷却 30 秒。撑过时刻即独自获胜。Wathe 不画弹道线，只有枪口火光；描边用方框表示。
     */
    private static void fiend(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("role_fiend", "魔人：挨打攒钱，买下魔人时刻");
        WatheItemScenes.cabinStage(scene, util);
        BlockPos door = util.grid().at(3, 1, 3);
        int crimson = RoleColors.of("sparkwitch:fiend", 0x8E1B3A);
        ItemStack knife = stack("wathe:knife");
        ItemStack revolver = stack("wathe:revolver");
        ItemStack crowbar = stack("wathe:crowbar");
        ElementLink<ActorElement> fiend = Actors.enter(scene, crimson, Text.literal("魔人"), new Vec3d(3.0, 1, 1.5),
                EAST, Direction.DOWN);
        Vec3d shooterFeet = new Vec3d(0.6, 1, 1.5);
        ElementLink<ActorElement> vigilante = Actors.enter(scene, RoleColors.VIGILANTE, Text.literal("义警"),
                shooterFeet, EAST, Direction.DOWN);
        Actors.hold(scene, vigilante, revolver);
        scene.idle(5);
        ElementLink<ActorElement> killer = Actors.enter(scene, RoleColors.KILLER, Text.literal("杀手"),
                new Vec3d(6.2, 1, 1.5), WEST, Direction.DOWN);
        Actors.hold(scene, killer, knife);
        scene.idle(15);
        scene.overlay().showText(80)
                .text("休眠的魔人几乎杀不死：除了掉下火车，刀枪一般都要不了命")
                .independent()
                .attachKeyFrame();
        scene.idle(10);
        Actors.walk(scene, killer, new Vec3d(-1.8, 0, 0), 20);
        scene.idle(22);
        scene.overlay().showControls(new Vec3d(4.4, 3.6, 1.5), Pointing.DOWN, 30).rightClick().withItem(knife);
        Actors.charge(scene, killer, true);
        scene.idle(14);
        Actors.charge(scene, killer, false);
        Actors.swing(scene, killer);
        scene.idle(30);
        scene.overlay().showControls(shooterFeet.add(0, 2.6, 0), Pointing.DOWN, 30).rightClick().withItem(revolver);
        scene.idle(10);
        // A levelled revolver's muzzle 0.85 ahead of an actor facing east, as REVOLVER_MUZZLE sits for one facing
        // screen-right. 面朝东的演员手中端平左轮的枪口，在身前 0.85 格，与朝屏幕右侧时 REVOLVER_MUZZLE 的位置相同。
        gunshotFlash(scene, new Vec3d(1.45, 2.05, 1.6));
        Actors.lookPitch(scene, vigilante, -4);
        scene.idle(3);
        Actors.lookPitch(scene, vigilante, 0);
        scene.idle(17);
        scene.overlay().showText(90)
                .colored(PonderPalette.GREEN)
                .text("挨一刀 +50 金币和 4 张便条；中一枪 +50 金币并加速 5 秒")
                .independent()
                .attachKeyFrame();
        scene.idle(30);
        // The killer backs off, clearing the door for the moment. 杀手退开，给时刻让出门前的位置。
        Actors.walk(scene, killer, new Vec3d(1.5, 0, -0.3), 18);
        scene.idle(20);
        Actors.turn(scene, killer, WEST);
        scene.idle(50);
        Actors.hold(scene, fiend, crowbar);
        // The moment's two-way outline lasts to the end. 时刻的双向描边一直持续到结束。
        Actors.highlight(scene, fiend, crimson, 436);
        Actors.highlight(scene, killer, crimson, 436);
        Actors.highlight(scene, vigilante, crimson, 436);
        scene.overlay().showText(90)
                .text("攒够 200 金币买下魔人时刻：你和活人一般互相隔墙可见（框为示意）")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        Actors.walk(scene, fiend, new Vec3d(0.5, 0, 0.2), 6);
        scene.idle(8);
        Actors.turn(scene, fiend, SOUTH);
        scene.idle(8);
        Vec3d overHead = new Vec3d(3.5, 3.6, 1.7);
        scene.overlay().showControls(overHead, Pointing.DOWN, 30).rightClick().withItem(crowbar);
        scene.idle(10);
        Actors.swing(scene, fiend);
        WatheItemScenes.openDoor(scene, door, true);
        scene.overlay().showText(90)
                .text("撬棍右键锁着的门就能撬开，门一般不会再关上；冷却 5 秒")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showControls(overHead, Pointing.DOWN, 30).showing(key(ABILITY_KEY, "G"));
        scene.idle(10);
        // Speed IV: about twice the walking pace. 速度 IV：大约是步行速度的两倍。
        Actors.walk(scene, fiend, new Vec3d(0, 0, 3.6), 9);
        scene.overlay().showText(90)
                .text("疾驰（默认 G）：10 秒速度 IV，每次用后冷却 30 秒")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showText(90)
                .colored(PonderPalette.RED)
                .text("撑满 2 分钟你就独自获胜；可此时没有免疫，被杀就结束")
                .independent();
        scene.idle(100);
        scene.markAsFinished();
    }

    /**
     * Blind (BlindRules, BlindSoundRules, BlindSoundPerception, BlindSoundAttribution, BlindPulseFanout,
     * BlindCaneService, BlindKitRules, BlindAttuneService, WhiteCaneItem, ComTacItem; client BlindView, BlindEchoView,
     * BlindEchoUniforms, BlindPerceptionClientState, BlindGateRules, BlindComTacHeadVisibility,
     * sparkwitch_blind_echo.fsh; vanilla PlayerEntity.getMoveEffect; Wathe PlayerEntityMixin; NoellesRoles
     * HiddenEquipmentHelper): the Blind's screen is black. A public sound within the Blind's range (10 blocks, x3 with a
     * worn ComTac, x5 while Attuned; a whisper counts half, the USEC rifle double or half) sends a pulse: as its front
     * sweeps out over 0.25 s, the depth edges the Blind can see within 4 blocks of the source light up as white lines,
     * fading from 0.6 s to 1.5 s. A player who made it is perceived for 1.5 s and drawn as white line art (a silhouette
     * and ripple through walls) with no name, held item or armour; object sounds (doors, non-player entities) light only
     * the environment. Unperceived players are not drawn at all; a sneaking player on the ground makes no footsteps (nor
     * does a Cautious one). The White Cane's right-click (CONSUME: no swing) sweeps 15 blocks over 0.6 s, its lines
     * fading from 2 s to 5 s, and shows every player within 5 blocks for the 5 s window, sneaking or invisible (not
     * active Wraiths or swallowed players); its tap is a public sound. Attune (ability key) multiplies the range by 5 for
     * 10 s. A bought ComTac goes straight onto an empty head and is drawn on no head during a round; the cane and the
     * ComTac are hidden in hand from other living players, so onlookers see an empty hand. Wathe walks at about
     * 3 blocks a second and sneaks at about 0.9. The stage is dimmed to stand for the black screen; the white boxes,
     * block outlines and the grey ring stand in for the line art.
     * 盲人：屏幕一片漆黑。盲人感知范围内（10 格，戴 ComTac ×3，凝神时 ×5；悄悄话按一半算，USEC 步枪按两倍或一半）的公开
     * 声音会发来一个脉冲：波前在 0.25 秒内扩散开，声源 4 格内盲人能看到的深度边缘随之亮成白线，从 0.6 秒淡出到 1.5 秒。发出
     * 声音的玩家被感知 1.5 秒，画成白色线稿（隔墙为人形轮廓和涟漪），没有名字、手持物和护甲；物体声（门、非玩家实体）只照亮
     * 环境。未被感知的玩家完全不画；在地面潜行的玩家没有脚步声（“小心翼翼”词条也一样）。盲杖右键（CONSUME：不挥手）在 0.6 秒
     * 内扫开 15 格，线条从 2 秒淡出到 5 秒，并在 5 秒窗口内显示 5 格内的所有玩家，潜行或隐身的也算（激活的冤魂与被吞者
     * 除外）；敲击声是公开的。凝神（技能键）10 秒内感知距离 ×5。买到的 ComTac 在头部槽空着时直接戴上，对局中不会画在任何人
     * 头上；盲杖和 ComTac 拿在手上时其他活着的玩家看不见，旁人只看到空手。Wathe 中步行约每秒 3 格，潜行约每秒 0.9 格。
     * 舞台调暗代表黑屏；白框、方块白色轮廓线和灰色圆圈代表线稿。
     */
    private static void blind(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("role_blind", "盲人：听声辨位与盲杖");
        WatheItemScenes.setStage(scene, util);
        ItemStack cane = stack("sparkwitch:white_cane");
        Vec3d feet = new Vec3d(4.6, 1, 1.4);
        Vec3d overHead = feet.add(0, 2.6, 0);
        ElementLink<ActorElement> blind = Actors.enter(scene, RoleColors.of("sparkwitch:blind", 0xB8C7D9),
                Text.literal("盲人"), feet, SCREEN_RIGHT, Direction.DOWN);
        Actors.hold(scene, blind, cane);
        ElementLink<ActorElement> civilian = Actors.enter(scene, RoleColors.CIVILIAN, Text.literal("平民"),
                new Vec3d(0.9, 1, 4.3), EAST, Direction.DOWN);
        ElementLink<ActorElement> killer = Actors.enter(scene, RoleColors.KILLER, Text.literal("杀手"),
                new Vec3d(0.7, 1, 0.9), EAST, Direction.DOWN);
        Actors.hold(scene, killer, stack("wathe:knife"));
        scene.idle(15);
        scene.overlay().showText(70)
                .text("盲人看不见：屏幕一片漆黑，主要靠声音感知周围")
                .independent()
                .attachKeyFrame();
        scene.idle(75);
        StageLights.lights(scene, false);
        scene.overlay().showText(80)
                .text("切到盲人眼中（示意）：舞台调暗代表黑屏，安静时一片漆黑")
                .independent()
                .attachKeyFrame();
        scene.idle(85);
        // Wathe's walking pace, about 3 blocks a second. Wathe 的步行速度，约每秒 3 格。
        loudWalk(scene, civilian, new Vec3d(3.4, 0, 0), 24);
        scene.overlay().showText(90)
                .text("一般 10 格内的脚步、说话或枪声，会让声源周围 4 格亮起白线")
                .independent()
                .attachKeyFrame();
        scene.idle(24);
        loudWalk(scene, civilian, new Vec3d(0, 0, -1.3), 9);
        scene.idle(71);
        loudWalk(scene, civilian, new Vec3d(-3.3, 0, 0), 23);
        scene.overlay().showText(90)
                .text("出声的人短暂显示成白色人影（白框示意），没有名字、手持物和护甲")
                .independent()
                .attachKeyFrame();
        scene.idle(95);
        Actors.sneak(scene, killer, true);
        // Wathe's sneaking pace, about 0.9 blocks a second. Wathe 的潜行速度，约每秒 0.9 格。
        Actors.walk(scene, killer, new Vec3d(2.2, 0, 0.3), 48);
        scene.overlay().showText(90)
                .text("潜行走路没有脚步声：他不出声摸到你身边，你也感知不到")
                .independent()
                .attachKeyFrame();
        scene.idle(95);
        scene.overlay().showControls(overHead, Pointing.DOWN, 30).rightClick().withItem(cane);
        scene.idle(10);
        // CONSUME: no arm swing. 返回 CONSUME：不挥手。
        caneSweep(scene, feet, List.of(new Revealed(killer, CROUCHING_BOX_HEIGHT),
                new Revealed(civilian, STANDING_BOX_HEIGHT)));
        scene.overlay().showText(100)
                .colored(PonderPalette.GREEN)
                .text("右键盲杖：5 秒内照亮周围 15 格，5 格内的人一般都会显形")
                .independent()
                .attachKeyFrame();
        scene.idle(105);
        scene.overlay().showControls(overHead, Pointing.DOWN, 40).showing(key(ABILITY_KEY, "G"));
        scene.overlay().showText(90)
                .text("凝神（默认 G）：10 秒内感知范围 ×5；商店的耳机戴上 ×3")
                .independent()
                .attachKeyFrame();
        scene.idle(45);
        scene.overlay().showControls(overHead, Pointing.DOWN, 40).withItem(stack("sparkwitch:comtac_viii"));
        scene.idle(50);
        StageLights.lights(scene, true);
        // Back to the onlookers' view: the cane is hidden in hand. 回到旁人视角：手里的盲杖被隐藏。
        Actors.hold(scene, blind, ItemStack.EMPTY);
        scene.overlay().showText(90)
                .text("其他活人看不见你的盲杖和耳机，敲杖不会挥手，但附近的人听得到")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
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
        throwSprite(scene,
                new SpriteFlight(device, from, to, ticks, (from.y - to.y) / 4, ParticleTypes.ELECTRIC_SPARK));
    }

    /**
     * A thrown item's flight as a camera-facing sprite (vanilla's FlyingItemEntityRenderer), gone on landing.
     * 投掷物的飞行：始终朝向镜头的物品图片（原版 FlyingItemEntityRenderer），落地即消失。
     */
    private static void throwSprite(SceneBuilder scene, SpriteFlight flight) {
        scene.addInstruction(ponder -> {
            flight.setVisible(true);
            flight.setFade(1);
            ponder.addElement(flight);
        });
    }

    /** Put a Seeker device into the scene. 把一台搜寻者设备放进场景。 */
    private static ElementLink<Gadget> gadget(SceneBuilder scene, Gadget gadget) {
        ElementLink<Gadget> link = new ElementLinkImpl<>(Gadget.class);
        scene.addInstruction(ponder -> {
            gadget.setVisible(true);
            gadget.setFade(1);
            ponder.addElement(gadget);
            ponder.linkElement(gadget, link);
        });
        return link;
    }

    /** A broken device: gone at once, with no particles (SeekerDeviceService.breakDevice). 设备被打坏：立刻消失，没有粒子。 */
    private static void removeGadget(SceneBuilder scene, ElementLink<Gadget> device) {
        changeGadget(scene, device, gadget -> gadget.setVisible(false));
    }

    /**
     * Drive the car by {@code delta} at its real speed, facing the way; returns the ticks it takes.
     * 以真实速度把小车开过 delta，并朝向前进方向；返回所需 tick 数。
     */
    private static int drive(SceneBuilder scene, ElementLink<Gadget> car, Vec3d delta) {
        int ticks = Math.max(1, (int) Math.ceil(delta.length() / CAR_SPEED));
        float heading = (float) (MathHelper.atan2(-delta.x, delta.z) * MathHelper.DEGREES_PER_RADIAN);
        scene.addInstruction(new Drive(car, delta, heading, ticks, true));
        return ticks;
    }

    /** Turn the car on the spot to {@code yaw} over {@code ticks}. 在 ticks 内原地把小车转向 yaw。 */
    private static void steer(SceneBuilder scene, ElementLink<Gadget> car, float yaw, int ticks) {
        scene.addInstruction(new Drive(car, Vec3d.ZERO, yaw, ticks, false));
    }

    /**
     * The look the Seeker views a camera with; its head eases towards it. 搜寻者观看摄像头时的视角；机头逐渐转向它。
     */
    private static void aim(SceneBuilder scene, ElementLink<Gadget> camera, float yaw, float pitch) {
        changeGadget(scene, camera, gadget -> gadget.aim(yaw, pitch));
    }

    /** The camera's LED: bright while the Seeker views it, dim otherwise. 摄像头指示灯：搜寻者观看时变亮，否则变暗。 */
    private static void lightUp(SceneBuilder scene, ElementLink<Gadget> camera, boolean viewed) {
        changeGadget(scene, camera, gadget -> gadget.lit = viewed);
    }

    private static void changeGadget(SceneBuilder scene, ElementLink<Gadget> device, Consumer<Gadget> change) {
        scene.addInstruction(ponder -> {
            Gadget gadget = ponder.resolve(device);
            if (gadget != null) {
                change.accept(gadget);
            }
        });
    }

    /**
     * Start potion swirls on {@code actor}, each effect for its own ticks, until they run out or {@link #unswirl}.
     * 在 actor 身上开始药水漩涡粒子，每种效果各自持续其 tick 数，直到用完或 unswirl。
     */
    private static Swirls swirls(SceneBuilder scene, ElementLink<ActorElement> actor, Swirl... effects) {
        Swirls swirls = new Swirls(actor, effects);
        scene.addInstruction(swirls);
        return swirls;
    }

    /** The effects are removed: no more swirls. 效果被移除：不再冒漩涡粒子。 */
    private static void unswirl(SceneBuilder scene, Swirls swirls) {
        scene.addInstruction(ponder -> swirls.stopped = true);
    }

    /**
     * An illustrated edge: an arc of gold dust on the floor {@code radius} blocks around {@code centre}, drawn only
     * over the plate, for {@code ticks}. 示意边界：以 centre 为圆心、半径 radius 格的金色粉尘弧线，只画在底板范围内，持续
     * ticks。
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
     * An illustrated outline over a whole body lying face down from {@code feet} towards {@code yaw}, for
     * {@code ticks}; Actors.highlight centres a body's box on its feet.
     * 示意的尸体描边：覆盖从 feet 朝 yaw 方向脸朝下躺着的整具尸体，持续 ticks；Actors.highlight 的尸体方框以脚为中心。
     */
    private static void bodyOutline(SceneBuilder scene, Vec3d feet, float yaw, int color, int ticks) {
        float radians = yaw * MathHelper.RADIANS_PER_DEGREE;
        Vec3d head = feet.add(-MathHelper.sin(radians) * BODY_LENGTH, 0, MathHelper.cos(radians) * BODY_LENGTH);
        scene.addInstruction(new Outline(new Box(feet.x, feet.y, feet.z, head.x, feet.y + 0.4, head.z)
                .expand(0.35, 0, 0.35), color, ticks));
    }

    /**
     * A Cooling Oil vial shattering (SparkStrength CoolingOilService.playBurst): 48 mint and 16 pale mist puffs filling
     * the body-height space 0.9 above the floor point, and 10 glass bits where it broke.
     * 风油精药瓶碎裂：薄荷色雾气 48 团、浅色核心 16 团，充满地面点上方 0.9 处一人高的空间；碎裂处飞出 10 片玻璃碎片。
     */
    private static void coolingOilBurst(SceneBuilder scene, ItemStack vial, Vec3d impact, Vec3d floor) {
        Vec3d mist = floor.add(0, 0.9, 0);
        spawnParticles(scene, MINT_MIST, mist, 48, new Vec3d(1.0, 0.6, 1.0), 0);
        spawnParticles(scene, MINT_CORE, mist, 16, new Vec3d(0.4, 0.3, 0.4), 0);
        spawnParticles(scene, new ItemStackParticleEffect(ParticleTypes.ITEM, vial), impact, 10,
                new Vec3d(0.05, 0.05, 0.05), 0.12);
    }

    /**
     * Walk by {@code delta} over {@code ticks} with footsteps the Blind perceives: a sound pulse at the feet on every
     * step (a vanilla step every {@link #STEP_DISTANCE} blocks, at most one pulse per throttle window), and the walker
     * drawn as a white box while perceived.
     * 在 ticks 内走过 delta，并发出盲人能感知的脚步：每一步在脚下产生一个声音脉冲（原版每走 STEP_DISTANCE 格响一步，节流窗口
     * 内至多一个脉冲），被感知期间行走者显示为白框。
     */
    private static void loudWalk(SceneBuilder scene, ElementLink<ActorElement> walker, Vec3d delta, int ticks) {
        Actors.walk(scene, walker, delta, ticks);
        int stepTicks = Math.max(EMITTER_THROTTLE_TICKS, (int) Math.round(STEP_DISTANCE * ticks / delta.length()));
        scene.addInstruction(new Footsteps(walker, ticks, stepTicks));
    }

    /**
     * A White Cane tap at the Blind's {@code feet} (BlindCaneService.use): the 15-block sweep held for the 5 s window,
     * and each of {@code players} shown as a white box from the moment it is within 5 blocks to the window's end (the
     * server re-scans for entrants every 10 ticks).
     * 盲人在 feet 处敲击盲杖：15 格扫描保持 5 秒窗口；players 中每个人从进入 5 格内起显示为白框，直到窗口结束（服务端每
     * 10 tick 补扫新进入者）。
     */
    private static void caneSweep(SceneBuilder scene, Vec3d feet, List<Revealed> players) {
        scene.addInstruction(new CaneSweep(feet, players));
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
     * A thrown item in flight (a Shock Device, a Holy Flash): the item drawn facing the camera on a fixed arc, with an
     * optional client trail (ShockDeviceEntity's electric spark on about 30% of ticks); scripted, so every replay lands
     * in the same spot.
     * 飞行中的投掷物（电击装置、圣光弹）：沿固定弧线、始终朝向镜头绘制的物品，可带客户端拖尾（ShockDeviceEntity 约 30% 的
     * tick 冒一颗电火花）；轨迹由脚本决定，每次重播都落在同一处。
     */
    private static final class SpriteFlight extends AnimatedSceneElementBase {
        private static final float TRAIL_CHANCE = 0.3f;
        private final ItemStack stack;
        private final Vec3d from;
        private final Vec3d to;
        private final int ticks;
        /** The arc's rise over the straight line at mid-flight. 弧线中点高出直线的高度。 */
        private final double arc;
        @Nullable
        private final ParticleEffect trail;
        private int age;

        /** @param trail the trail particle, or null for none / 拖尾粒子，没有则为 null */
        private SpriteFlight(ItemStack stack, Vec3d from, Vec3d to, int ticks, double arc,
                             @Nullable ParticleEffect trail) {
            this.stack = stack.copy();
            this.from = from;
            this.to = to;
            this.ticks = Math.max(1, ticks);
            this.arc = arc;
            this.trail = trail;
        }

        @Override
        public void reset(@Nullable PonderScene scene) {
            age = 0;
        }

        @Override
        public void tick(PonderScene scene) {
            age++;
            if (trail != null && age < ticks && scene.getWorld().random.nextFloat() < TRAIL_CHANCE) {
                Vec3d at = at(age);
                scene.getWorld().addParticle(trail, at.x, at.y, at.z, 0, 0, 0);
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

    /**
     * A placed Seeker device drawn with its own baked models, as SeekerCarEntityRenderer and
     * SeekerCameraEntityRenderer draw the entities. The car (no mount): the placed car model, front at model north,
     * turned to its yaw and resting on the floor. A wall camera: the static mount against the wall face, the head
     * pivoting at its housing centre towards the look (closing half the remaining angle a tick,
     * SeekerCameraLookRules.RENDER_SMOOTHING) and the LED on the head, full-bright while viewed and dimmed to 0.3
     * otherwise. Scripted, so every replay is the same.
     * 用搜寻者设备自己的烘焙模型绘制放置后的设备，与 SeekerCarEntityRenderer、SeekerCameraEntityRenderer 绘制实体的方式相同。
     * 小车（没有安装面）：放置的小车模型，模型北侧为车头，转到其朝向并贴着地面。墙面摄像头：静态底座贴着墙面，机头以外壳中心
     * 为轴转向视角（每 tick 追上剩余角度的一半），指示灯在机头上，被观看时全亮，否则变暗到 0.3。由脚本驱动，每次重播都相同。
     */
    private static final class Gadget extends AnimatedSceneElementBase {
        private static final Identifier CAR_MODEL = Identifier.of("sparkwitch", "item/seeker_car_placed");
        private static final Identifier MOUNT_MODEL = Identifier.of("sparkwitch", "item/seeker_camera_placed");
        private static final Identifier HEAD_MODEL = Identifier.of("sparkwitch", "item/seeker_camera_placed_head");
        private static final Identifier LED_MODEL = Identifier.of("sparkwitch", "item/seeker_camera_placed_led");
        /** Lifts the centred car model onto its feet (SeekerCarEntityRenderer). 把居中的小车模型抬到脚底。 */
        private static final double CAR_MODEL_LIFT = 0.5;
        /** The camera cube's size (SeekerRules.CAMERA_SIZE). 摄像头立方体的边长。 */
        private static final double CAMERA_SIZE = 0.3;
        /** The head's pivot from the model centre (SeekerCameraEntityRenderer.HEAD_PIVOT). 机头转轴相对模型中心的位置。 */
        private static final Vector3f HEAD_PIVOT = new Vector3f(0.0f, 0.125f / 16.0f, -0.5f / 16.0f);
        private static final float LED_DIM = 0.3f;
        private static final float LOOK_SMOOTHING = 0.5f;
        private static final Direction[] QUAD_SIDES = {Direction.DOWN, Direction.UP, Direction.NORTH,
                Direction.SOUTH, Direction.WEST, Direction.EAST, null};
        private final ItemStack stack;
        /** The wall face a camera hangs on; null for the car. 摄像头所依附的墙面；小车为 null。 */
        @Nullable
        private final Direction mount;
        private final Vec3d startPosition;
        private final float startYaw;
        private final Random quadRandom = Random.create();
        private Vec3d position;
        private Vec3d previousPosition;
        /** The car's heading, or the camera's look yaw. 小车的朝向，或摄像头视角的偏航。 */
        private float yaw;
        private float previousYaw;
        private float pitch;
        private float headYaw;
        private float previousHeadYaw;
        private float headPitch;
        private float previousHeadPitch;
        private boolean lit;

        /** @param mount the wall face of a camera, or null for the car / 摄像头的依附墙面，小车为 null */
        private Gadget(ItemStack stack, Vec3d position, float yaw, @Nullable Direction mount) {
            this.stack = stack.copy();
            this.startPosition = position;
            this.startYaw = yaw;
            this.mount = mount;
            reset(null);
        }

        @Override
        public void reset(@Nullable PonderScene scene) {
            position = startPosition;
            previousPosition = startPosition;
            yaw = startYaw;
            previousYaw = startYaw;
            pitch = 0;
            headYaw = startYaw;
            previousHeadYaw = startYaw;
            headPitch = 0;
            previousHeadPitch = 0;
            lit = false;
        }

        private void aim(float yaw, float pitch) {
            this.yaw = yaw;
            this.pitch = pitch;
        }

        @Override
        public void tick(PonderScene scene) {
            previousPosition = position;
            previousYaw = yaw;
            previousHeadYaw = headYaw;
            previousHeadPitch = headPitch;
            headYaw += MathHelper.wrapDegrees(yaw - headYaw) * LOOK_SMOOTHING;
            headPitch += (pitch - headPitch) * LOOK_SMOOTHING;
        }

        @Override
        protected void renderLast(PonderLevel world, VertexConsumerProvider buffer, DrawContext graphics, float fade,
                                  float pt) {
            if (fade <= 0.01f) {
                return;
            }
            MatrixStack ms = graphics.getMatrices();
            Vec3d at = previousPosition.lerp(position, pt);
            int light = lightCoordsFromFade(fade);
            ms.push();
            ms.translate(at.x, at.y, at.z);
            if (mount == null) {
                ms.translate(0, CAR_MODEL_LIFT, 0);
                ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(
                        180 - MathHelper.lerpAngleDegrees(pt, previousYaw, yaw)));
                renderPart(ms, buffer, light, CAR_MODEL);
            } else {
                ms.translate(0, CAMERA_SIZE / 2, 0);
                Quaternionf wall = RotationAxis.POSITIVE_Y.rotationDegrees(180 - mount.asRotation());
                ms.push();
                ms.multiply(wall);
                renderPart(ms, buffer, light, MOUNT_MODEL);
                ms.pop();
                Vector3f pivot = wall.transform(new Vector3f(HEAD_PIVOT));
                ms.translate(pivot.x, pivot.y, pivot.z);
                ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(
                        180 - MathHelper.lerpAngleDegrees(pt, previousHeadYaw, headYaw)));
                ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(
                        -MathHelper.lerp(pt, previousHeadPitch, headPitch)));
                ms.translate(-HEAD_PIVOT.x, -HEAD_PIVOT.y, -HEAD_PIVOT.z);
                renderPart(ms, buffer, light, HEAD_MODEL);
                renderLed(ms, buffer, light);
            }
            ms.pop();
        }

        private void renderPart(MatrixStack ms, VertexConsumerProvider buffer, int light, Identifier model) {
            MinecraftClient.getInstance().getItemRenderer().renderItem(stack, ModelTransformationMode.FIXED, false, ms,
                    buffer, light, OverlayTexture.DEFAULT_UV, model(model));
        }

        /** SeekerCameraEntityRenderer.renderLed. */
        private void renderLed(MatrixStack ms, VertexConsumerProvider buffer, int light) {
            float shade = lit ? 1 : LED_DIM;
            int ledLight = lit ? LightmapTextureManager.MAX_LIGHT_COORDINATE : light;
            VertexConsumer consumer = buffer.getBuffer(TexturedRenderLayers.getEntityCutout());
            BakedModel model = model(LED_MODEL);
            ms.push();
            ms.translate(-0.5f, -0.5f, -0.5f);
            MatrixStack.Entry entry = ms.peek();
            for (Direction side : QUAD_SIDES) {
                quadRandom.setSeed(42L);
                for (BakedQuad quad : model.getQuads(null, side, quadRandom)) {
                    consumer.quad(entry, quad, shade, shade, shade, 1, ledLight, OverlayTexture.DEFAULT_UV);
                }
            }
            ms.pop();
        }

        /** A model SparkWitch loads through ModelLoadingPlugin (SeekerModels). SparkWitch 通过插件加载的模型。 */
        private static BakedModel model(Identifier id) {
            return ((FabricBakedModelManager) MinecraftClient.getInstance().getBakedModelManager()).getModel(id);
        }
    }

    /**
     * Moves a device by {@code delta} over {@code ticks}, turning it to {@code yaw}: at once when driving (the car
     * faces the way it is driven), step by step when steering on the spot.
     * 在 ticks 内把设备移动 delta，并转向 yaw：行驶时立刻朝向前进方向，原地转向时逐步转过去。
     */
    private static final class Drive extends TickingInstruction {
        private final ElementLink<Gadget> link;
        private final Vec3d step;
        private final float targetYaw;
        private final boolean faceAtOnce;
        @Nullable
        private Gadget gadget;
        private float startYaw;

        private Drive(ElementLink<Gadget> link, Vec3d delta, float yaw, int ticks, boolean faceAtOnce) {
            super(false, ticks);
            this.link = link;
            this.step = delta.multiply(1.0 / ticks);
            this.targetYaw = yaw;
            this.faceAtOnce = faceAtOnce;
        }

        @Override
        protected void firstTick(PonderScene scene) {
            super.firstTick(scene);
            gadget = scene.resolve(link);
            if (gadget != null) {
                startYaw = gadget.yaw;
                if (faceAtOnce) {
                    gadget.yaw = targetYaw;
                }
            }
        }

        @Override
        public void tick(PonderScene scene) {
            super.tick(scene);
            if (gadget == null) {
                return;
            }
            gadget.position = gadget.position.add(step);
            if (!faceAtOnce) {
                float progress = 1 - remainingTicks / (float) totalTicks;
                gadget.yaw = startYaw + MathHelper.wrapDegrees(targetYaw - startYaw) * progress;
            }
        }
    }

    /** One status effect's swirls and how long the effect runs. 一种状态效果的漩涡粒子及其持续时间。 */
    private record Swirl(ParticleEffect particle, int ticks) {
    }

    /**
     * Potion swirls on an actor while their effects run, as LivingEntity.tickStatusEffects shows a visible effect to
     * everyone: on about one tick in four, one swirl of a random running effect somewhere on the body.
     * 演员身上效果生效期间的药水漩涡粒子，与 LivingEntity.tickStatusEffects 向所有人显示可见效果相同：约每 4 tick 一次，
     * 在身上某处冒出一颗正在生效的效果中随机一种的漩涡。
     */
    private static final class Swirls extends TickingInstruction {
        private static final int ONE_IN = 4;
        private static final double HALF_WIDTH = 0.3;
        private static final double HEIGHT = 1.8;
        private final ElementLink<ActorElement> link;
        private final Swirl[] effects;
        private final List<ParticleEffect> running = new ArrayList<>();
        private int age;
        private boolean stopped;

        private Swirls(ElementLink<ActorElement> link, Swirl[] effects) {
            super(false, UNTIL_STOPPED);
            this.link = link;
            this.effects = effects.clone();
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
            age++;
            running.clear();
            for (Swirl effect : effects) {
                if (age <= effect.ticks()) {
                    running.add(effect.particle());
                }
            }
            if (running.isEmpty()) {
                stopped = true;
            }
            ActorElement actor = scene.resolve(link);
            PonderLevel world = scene.getWorld();
            if (stopped || actor == null || world.random.nextInt(ONE_IN) != 0) {
                return;
            }
            Vec3d at = actor.position();
            world.addParticle(running.get(world.random.nextInt(running.size())),
                    at.x + (world.random.nextDouble() * 2 - 1) * HALF_WIDTH, at.y + world.random.nextDouble() * HEIGHT,
                    at.z + (world.random.nextDouble() * 2 - 1) * HALF_WIDTH, 1, 1, 1);
        }

        @Override
        public boolean isComplete() {
            return stopped || super.isComplete();
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

        private Outline(Box box, int color, int ticks) {
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

    /**
     * One pulse of the Blind's echo view, as sparkwitch_blind_echo.fsh draws it: as the front grows to {@code radius}
     * over {@code expandTicks} (the shader's ease-out), every solid block above the floor it reaches joins one white
     * cluster outline (its creases and corners are the depth edges the shader lights; a flat floor has none), which
     * fades from max(expand, 40%) of {@code ticks} to the end; a faint ring on the floor marks the front and fades
     * over 0.3 s after it stops.
     * 盲人回声视图的一个脉冲，与 sparkwitch_blind_echo.fsh 的画法对应：波前在 expandTicks 内按着色器的缓出曲线扩张到 radius，
     * 它扫到的地面以上实心方块并入同一个白色整体描边（折角与棱角就是着色器点亮的深度边缘；平坦地面没有边缘），描边从
     * ticks 的 max(扩张, 40%) 处淡出到结束；地面上一圈淡淡的环标出波前，停止扩张后 0.3 秒内淡出。
     */
    private static final class EchoPulse {
        private static final int RING_SEGMENTS = 64;
        /** The front's sweep fades over 0.3 s after the expansion (the shader's sweep). 波前在扩张后 0.3 秒内淡出。 */
        private static final int SWEEP_FADE_TICKS = 6;
        /** The lines start fading at this share of the pulse (the shader's duration * 0.4). 线条从脉冲时长的该比例处开始淡出。 */
        private static final float FADE_FROM_SHARE = 0.4f;
        private static final int TOP = 3;
        private final Vec3d centre;
        private final double radius;
        private final int expandTicks;
        private final int ticks;
        private final Object cluster = new Object();
        private final Object[] ring = new Object[RING_SEGMENTS];
        private final List<BlockPos> lit = new ArrayList<>();
        private int age;

        private EchoPulse(Vec3d centre, double radius, int expandTicks, int ticks) {
            this.centre = centre;
            this.radius = radius;
            this.expandTicks = expandTicks;
            this.ticks = ticks;
            for (int i = 0; i < RING_SEGMENTS; i++) {
                ring[i] = new Object();
            }
        }

        /** Returns whether the pulse still draws anything. 返回脉冲是否仍在绘制。 */
        private boolean tick(PonderScene scene) {
            age++;
            float grow = Math.min(1, age / (float) expandTicks);
            double front = radius * (1 - (1 - grow) * (1 - grow));
            float fade = 1 - smoothstep(Math.max(expandTicks, ticks * FADE_FROM_SHARE), ticks, age);
            if (age <= ticks) {
                outlineWithin(scene, front, grey(ECHO_WHITE, fade));
            }
            float sweep = 1 - smoothstep(expandTicks, expandTicks + SWEEP_FADE_TICKS, age);
            if (sweep > 0) {
                ring(scene, front, grey(ECHO_WAVE_GREY, sweep * fade));
            }
            return age < ticks;
        }

        /** The cluster of solid blocks whose centre the front has reached. 波前已扫到其中心的实心方块整体。 */
        private void outlineWithin(PonderScene scene, double front, int color) {
            int before = lit.size();
            lit.clear();
            PonderLevel world = scene.getWorld();
            for (int x = 0; x < PLATE; x++) {
                for (int y = 1; y <= TOP; y++) {
                    for (int z = 0; z < PLATE; z++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        if (!world.getBlockState(pos).isAir() && Vec3d.ofCenter(pos).isInRange(centre, front)) {
                            lit.add(pos);
                        }
                    }
                }
            }
            if (lit.isEmpty()) {
                return;
            }
            if (lit.size() != before) {
                scene.getOutliner().showCluster(cluster, List.copyOf(lit)).lineWidth(1 / 16f).colored(color);
            } else {
                scene.getOutliner().edit(cluster).ifPresent(params -> params.colored(color));
            }
        }

        /** The front on the open floor: segments over the plate and outside blocks only. 开阔地面上的波前：只画底板上方、方块外的线段。 */
        private void ring(PonderScene scene, double front, int color) {
            // Just above the floor under the source. 声源下方地面的稍上方。
            double y = Math.floor(centre.y) + 0.02;
            for (int i = 0; i < RING_SEGMENTS; i++) {
                double a0 = i * MathHelper.TAU / RING_SEGMENTS;
                double a1 = (i + 1) * MathHelper.TAU / RING_SEGMENTS;
                Vec3d from = new Vec3d(centre.x + Math.cos(a0) * front, y, centre.z + Math.sin(a0) * front);
                Vec3d to = new Vec3d(centre.x + Math.cos(a1) * front, y, centre.z + Math.sin(a1) * front);
                if (!onFloor(scene, from) || !onFloor(scene, to)) {
                    continue;
                }
                scene.getOutliner().showLine(ring[i], from, to).lineWidth(1 / 32f).colored(color);
            }
        }

        private static boolean onFloor(PonderScene scene, Vec3d at) {
            return at.x >= 0 && at.x <= PLATE && at.z >= 0 && at.z <= PLATE
                    && scene.getWorld().getBlockState(BlockPos.ofFloored(at.x, at.y, at.z)).isAir();
        }

        /** GLSL smoothstep. GLSL 的 smoothstep。 */
        private static float smoothstep(float edge0, float edge1, float x) {
            float t = MathHelper.clamp((x - edge0) / (edge1 - edge0), 0, 1);
            return t * t * (3 - 2 * t);
        }

        /** {@code rgb} dimmed to {@code level} (0..1) on black. 把 rgb 在黑底上调暗到 level（0..1）。 */
        private static int grey(int rgb, float level) {
            return ActorSkins.scale(rgb, MathHelper.clamp(level, 0, 1));
        }
    }

    /**
     * A walker's footsteps as the Blind perceives them (BlindPulseFanout, BlindPerceptionClientState): a 4-block
     * pulse at the feet every {@code stepTicks} while walking, and the walker drawn as a white box while perceived,
     * 1.5 s from the last step (the outliner's own fade included).
     * 盲人感知到的行走者脚步：行走期间每 stepTicks 在脚下产生一个 4 格脉冲；被感知期间（最后一步后 1.5 秒，含描边自身的
     * 淡出）行走者显示为白框。
     */
    private static final class Footsteps extends TickingInstruction {
        /** The first step lands a little after setting off. 起步后稍等片刻才响第一步。 */
        private static final int FIRST_STEP = 2;
        private static final int NEVER = Integer.MIN_VALUE / 2;
        private final ElementLink<ActorElement> link;
        private final int walkTicks;
        private final int stepTicks;
        private final List<EchoPulse> pulses = new ArrayList<>();
        private final Object body = new Object();
        private int age;
        private int lastStep = NEVER;

        private Footsteps(ElementLink<ActorElement> link, int walkTicks, int stepTicks) {
            super(false, walkTicks + SOUND_PULSE_TICKS);
            this.link = link;
            this.walkTicks = walkTicks;
            this.stepTicks = stepTicks;
        }

        @Override
        public void reset(PonderScene scene) {
            super.reset(scene);
            pulses.clear();
            age = 0;
            lastStep = NEVER;
        }

        @Override
        public void tick(PonderScene scene) {
            super.tick(scene);
            ActorElement actor = scene.resolve(link);
            if (actor != null && age < walkTicks && age % stepTicks == FIRST_STEP) {
                pulses.add(new EchoPulse(actor.position(), SOUND_REVEAL_RADIUS, SOUND_EXPAND_TICKS,
                        SOUND_PULSE_TICKS));
                lastStep = age;
            }
            pulses.removeIf(pulse -> !pulse.tick(scene));
            if (actor != null && age - lastStep < SOUND_PULSE_TICKS - OUTLINE_FADE_TICKS) {
                scene.getOutliner().chaseAABB(body, actorBox(actor.position(), STANDING_BOX_HEIGHT))
                        .lineWidth(1 / 16f).colored(ECHO_WHITE);
            }
            age++;
        }
    }

    /** A player the cane may reveal, with the height of the box drawn for them. 盲杖可能显示的玩家及其方框高度。 */
    private record Revealed(ElementLink<ActorElement> actor, double boxHeight) {
    }

    /**
     * A White Cane window (BlindCaneService, BlindKitRules.isCaneTarget): one 15-block pulse from the Blind's body
     * centre for the 5 s window, and each player drawn as a white box once within 5 blocks of the Blind, to the
     * window's end (the outliner's own fade included).
     * 盲杖窗口：从盲人身体中心发出一个 15 格脉冲，持续 5 秒窗口；每名玩家一旦进入盲人 5 格内就显示为白框，直到窗口结束
     * （含描边自身的淡出）。
     */
    private static final class CaneSweep extends TickingInstruction {
        private final Vec3d feet;
        private final List<Revealed> players;
        private final List<Object> boxes = new ArrayList<>();
        private final boolean[] revealed;
        @Nullable
        private EchoPulse pulse;

        private CaneSweep(Vec3d feet, List<Revealed> players) {
            super(false, CANE_ACTIVE_TICKS);
            this.feet = feet;
            this.players = List.copyOf(players);
            this.revealed = new boolean[players.size()];
            for (int i = 0; i < players.size(); i++) {
                boxes.add(new Object());
            }
        }

        @Override
        public void reset(PonderScene scene) {
            super.reset(scene);
            pulse = null;
            Arrays.fill(revealed, false);
        }

        @Override
        protected void firstTick(PonderScene scene) {
            super.firstTick(scene);
            pulse = new EchoPulse(feet.add(0, 0.9, 0), CANE_ENVIRONMENT_RADIUS, CANE_EXPAND_TICKS, CANE_ACTIVE_TICKS);
        }

        @Override
        public void tick(PonderScene scene) {
            super.tick(scene);
            if (pulse != null) {
                pulse.tick(scene);
            }
            if (remainingTicks <= OUTLINE_FADE_TICKS) {
                return;
            }
            for (int i = 0; i < players.size(); i++) {
                Revealed player = players.get(i);
                ActorElement actor = scene.resolve(player.actor());
                if (actor == null || actor.isDown()) {
                    continue;
                }
                revealed[i] |= actor.position().isInRange(feet, CANE_PLAYER_RADIUS);
                if (revealed[i]) {
                    scene.getOutliner().chaseAABB(boxes.get(i), actorBox(actor.position(), player.boxHeight()))
                            .lineWidth(1 / 16f).colored(ECHO_WHITE);
                }
            }
        }
    }

    /** An actor's box of {@code height}, as wide as Actors.highlight draws it. 高 height、宽度与 Actors.highlight 相同的演员方框。 */
    private static Box actorBox(Vec3d feet, double height) {
        return new Box(feet.x - 0.4, feet.y, feet.z - 0.4, feet.x + 0.4, feet.y + height, feet.z + 0.4);
    }
}
