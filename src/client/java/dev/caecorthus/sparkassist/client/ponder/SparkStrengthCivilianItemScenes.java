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
import java.util.List;
import java.util.function.Consumer;
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
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 * Hold-W item demos for SparkStrength's civilian-side items (the Toxicologist's Capsule and Blue Belladonna, the
 * Attendant's Flashlight, the Professor's Sedative and Truth Serum, the Engineer's Capture Device, the Timekeeper's
 * Dying Watch, the Perfumer's Aroma Orb and Zephyr Perfume) and for the Bartender's spirits and modifiers as they run
 * with SparkStrength's retuning. Effects only some players see, or only on a HUD, are told in captions; outlines are
 * boxes and the flashlight beam is drawn as lines, which the captions call illustrations.
 * SparkStrength 平民侧物品（毒理学家的胶囊与蓝颠茄、乘务员的手电筒、教授的镇静与吐真试剂、工程师的捕捉装置、计时员的濒毁怀表、
 * 调香师的香薰与晨风香水）以及酒保烈酒与修饰材料（按 SparkStrength 调整后的数值）的按住 W 物品演示。只有部分人看得到、
 * 或只显示在界面上的效果写在字幕里；描边用方框表示，手电光束用线条表示，字幕注明是示意。
 */
final class SparkStrengthCivilianItemScenes {
    /** Poison markers rise like Wathe's plate particles. 中毒标记像 Wathe 的盘子粒子一样上升。 */
    private static final Vec3d POISON_RISE = new Vec3d(0, 0.05, 0);
    /** A player's height: the top of the head above the feet. 玩家身高：头顶离脚底的高度。 */
    private static final double HEAD_TOP = 1.8;
    /** Blue-poisoned players in a Toxicologist's sight (SparkTraits ConsciencePoisonerService.BLUE_POISON_COLOR). 毒理学家视线内中蓝毒者的颜色。 */
    private static final int BLUE_POISON = 0x00BFFF;
    /** The Professor's outline of a sedated player (SparkStrength ProfessorSerumRules.SEDATIVE_HIGHLIGHT_COLOR). 教授看到的镇静目标描边颜色。 */
    private static final int SEDATIVE_YELLOW = 0xFFD84A;
    /** The illustrated flashlight beam. 示意手电光束的颜色。 */
    private static final int BEAM_YELLOW = 0xFFF1A8;
    /** Aroma Orb's burst (SparkStrength AromaService.LAVENDER_BURST). 香薰命中时的薰衣草色粒子。 */
    private static final DustParticleEffect LAVENDER_BURST =
            new DustParticleEffect(new Vector3f(0.72f, 0.55f, 0.95f), 1.1f);
    /** Aroma Orb's wisps (AromaService.LAVENDER_WISP). 香薰的淡紫色香气。 */
    private static final DustParticleEffect LAVENDER_WISP =
            new DustParticleEffect(new Vector3f(0.78f, 0.64f, 0.97f), 0.7f);
    /** Zephyr Perfume's mist (SparkStrength ZephyrService.ZEPHYR_MIST). 晨风香水的喷雾。 */
    private static final DustParticleEffect ZEPHYR_MIST =
            new DustParticleEffect(new Vector3f(0.86f, 0.94f, 1.0f), 0.9f);
    /**
     * Walking pace during a round, blocks a tick: Wathe sets the movement speed to 0.07 (PlayerEntityMixin), about
     * 0.151 blocks a tick. 对局中的步行速度，格每 tick：Wathe 把移动速度设为 0.07，约每 tick 0.151 格。
     */
    private static final double WALK = 0.151;
    /** Speed II: +40%. 速度 II：快 40%。 */
    private static final double SPEED_II = WALK * 1.4;
    /** Slowness II: -30%. 缓慢 II：慢 30%。 */
    private static final double SLOWNESS_II = WALK * 0.7;
    /** Sprinting pace during a round (Wathe's 0.1), about 0.216 blocks a tick. 对局中的疾跑速度，约每 tick 0.216 格。 */
    private static final double SPRINT = 0.216;
    /** Wathe's cocktail drink time, which base spirits keep. 基酒沿用的 Wathe 鸡尾酒饮用时长。 */
    private static final int DRINK_TICKS = 40;
    /** The gun of an actor standing at (5.8, 1, 1.2) and facing screen-right. 站在 (5.8, 1, 1.2) 朝屏幕右侧的演员手中枪口的位置。 */
    private static final Vec3d REVOLVER_MUZZLE = new Vec3d(5.2, 2.05, 1.8);
    /** A trickle runs until stopped; long enough for any scene. 持续到被停止；对任何场景都足够长。 */
    private static final int UNTIL_STOPPED = 20 * 60 * 10;
    private static final String[] SPIRITS = {"noellesroles:rum", "noellesroles:gin", "noellesroles:vodka",
            "noellesroles:tequila"};
    private static final String[] MODIFIERS = {"noellesroles:ice_cube", "noellesroles:special_liqueur",
            "noellesroles:special_spice", "sparkstrength:ghostflame_bitters", "sparkstrength:ember_sugar"};

    private SparkStrengthCivilianItemScenes() {
    }

    static void register() {
        List<String> noellesStrength = List.of("noellesroles", "sparkstrength");
        List<String> bluePoison = List.of("noellesroles", "sparkstrength", "sparktraits");
        List<String> perfumer = List.of("sparkwitch", "sparkstrength");
        item("sparkstrength:capsule", bluePoison, scene("wathe/bar", SparkStrengthCivilianItemScenes::capsule));
        item("sparkstrength:blue_belladonna", bluePoison,
                scene("wathe/aisle", SparkStrengthCivilianItemScenes::blueBelladonna));
        item("sparkstrength:flashlight", noellesStrength,
                scene("wathe/lit_carriage", SparkStrengthCivilianItemScenes::flashlight));
        item("sparkstrength:sedative", noellesStrength,
                scene("wathe/aisle", SparkStrengthCivilianItemScenes::professorSerums));
        item("sparkstrength:truth_serum", noellesStrength,
                scene("wathe/aisle", SparkStrengthCivilianItemScenes::professorSerums));
        item("sparkstrength:capture_device", noellesStrength,
                scene("wathe/aisle", SparkStrengthCivilianItemScenes::captureDevice));
        item("sparkstrength:dying_watch", noellesStrength,
                scene("wathe/aisle", SparkStrengthCivilianItemScenes::dyingWatch));
        item("sparkstrength:aroma_orb", perfumer, scene("wathe/aisle", SparkStrengthCivilianItemScenes::aromaOrb));
        item("sparkstrength:zephyr_perfume", perfumer,
                scene("wathe/aisle", SparkStrengthCivilianItemScenes::zephyrPerfume));
        for (String spirit : SPIRITS) {
            item(spirit, noellesStrength, scene("wathe/bar", NoellesCivilianScenes::bartender),
                    scene("wathe/lit_carriage", SparkStrengthCivilianItemScenes::spirits));
        }
        for (String modifier : MODIFIERS) {
            item(modifier, noellesStrength, scene("wathe/bar", NoellesCivilianScenes::bartender),
                    scene("wathe/aisle", SparkStrengthCivilianItemScenes::modifiers));
        }
    }

    /**
     * Capsule (SparkStrength CapsuleItem, CapsuleEntity, ToxicologistCapsuleShop; Wathe FoodPlatterBlock,
     * PoisonUtils.applyFoodPoison): 100 coins in the Toxicologist's shop. In the inventory, a food or drink on the
     * cursor right-clicked onto an empty capsule goes inside (one portion, poison flags kept). A right-click throws it
     * like a snowball (a swing); it is used up hit or miss. A hit player is made to eat or drink the contents, and
     * native poison takes hold as if they had eaten it (the kill still goes to the original poisoner); a miss just
     * ends on the first block. Only the Toxicologist's shop sells it (and a Coroner's Toxicologist disguise); it is not
     * hidden in hand. An empty-handed player who carries nothing from a plate takes one random serving; from a poisoned
     * plate the first such serving carries the poison off and clears the plate. The Toxicologist sees the plate's
     * skull and poisoned players in line of sight (boxes here); Wathe poison kills after 800-1400 ticks.
     * 胶囊：毒理学家商店 100 金币。在背包里把食物或饮品拿在鼠标上右键空胶囊，就装进去一份（毒的标记也保留）。右键像雪球一样
     * 扔出（会挥手），不管中没中都会用掉。被砸中的人被迫吃下或喝下里面的东西，普通毒照常生效；没砸中则碰到第一个方块就没了。
     * 胶囊只在毒理学家商店（以及验尸官的毒理学家伪装商店）出售；拿在手上不隐藏。身上没带这盘里东西的人空手能随机拿一份；
     * 从有毒的盘子上拿走的第一份带着毒，盘子随之变干净（击杀仍记在原下毒者名下）。毒理学家看得到盘子上的骷髅和视线内的
     * 中毒者（这里用方框表示）；Wathe 的毒在 800～1400 tick 后发作。
     */
    private static void capsule(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("ss_capsule", "胶囊：装进吃喝，砸中就得咽下");
        WatheItemScenes.setStage(scene, util);
        Vec3d plate = new Vec3d(3.5, 2.1, 3.5);
        Vec3d trayTop = new Vec3d(3.5, 3.0, 3.5);
        int toxicologistColor = RoleColors.of("noellesroles:toxicologist", 0xB8295A);
        ItemStack capsule = stack("sparkstrength:capsule");
        ItemStack drink = stack("wathe:mojito");
        Effects.Marker skull = Effects.mark(scene, WatheParticles.POISON, plate, POISON_RISE, 0.2f);
        Vec3d toxicologistStart = new Vec3d(6.0, 1, 1.0);
        Vec3d atTray = new Vec3d(4.4, 1, 2.6);
        ElementLink<ActorElement> toxicologist = Actors.enter(scene, toxicologistColor, Text.literal("毒理学家"),
                toxicologistStart, SCREEN_RIGHT, Direction.DOWN);
        scene.idle(10);
        Actors.walk(scene, toxicologist, atTray.subtract(toxicologistStart), 25);
        scene.idle(28);
        scene.overlay().showText(90)
                .text("托盘上冒着小红骷髅：被下了毒（毒理学家、杀手等少数人看得见）")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showControls(trayTop, Pointing.DOWN, 30).rightClick();
        scene.idle(10);
        Actors.swing(scene, toxicologist);
        Actors.hold(scene, toxicologist, drink);
        Effects.unmark(scene, skull);
        scene.overlay().showText(80)
                .text("空手拿走一杯（身上已带着这盘里的东西就拿不了）：毒跟着这杯走了，托盘也干净了")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        scene.overlay().showControls(atTray.add(0, 2.6, 0), Pointing.DOWN, 60).rightClick().withItem(drink);
        scene.overlay().showText(90)
                .text("【胶囊】100 金币：在背包里把食物或饮品拿在鼠标上，右键胶囊就装了进去")
                .independent()
                .attachKeyFrame();
        scene.idle(45);
        Actors.hold(scene, toxicologist, capsule);
        scene.idle(55);
        scene.overlay().showText(70)
                .text("一颗胶囊只装一份，里面的毒也原样留着")
                .independent();
        scene.idle(40);
        Vec3d killerStart = new Vec3d(1.0, 1, 6.0);
        Vec3d killerSpot = new Vec3d(1.4, 1, 2.0);
        ElementLink<ActorElement> killer = Actors.enter(scene, RoleColors.KILLER, Text.literal("杀手"), killerStart,
                NORTH, Direction.DOWN);
        scene.idle(10);
        Actors.walk(scene, killer, killerSpot.subtract(killerStart), 30);
        scene.idle(32);
        float atKiller = facing(atTray, killerSpot);
        Actors.turn(scene, killer, facing(killerSpot, plate));
        Actors.turn(scene, toxicologist, atKiller);
        scene.idle(10);
        scene.overlay().showControls(atTray.add(0, 2.6, 0), Pointing.DOWN, 30).rightClick().withItem(capsule);
        scene.idle(10);
        Actors.swing(scene, toxicologist);
        Actors.hold(scene, toxicologist, ItemStack.EMPTY);
        throwSprite(scene, capsule, hand(atTray, atKiller), towards(killerSpot, atTray, 1.3), 3);
        scene.idle(3);
        Actors.highlight(scene, killer, toxicologistColor, 170);
        scene.overlay().showText(90)
                .colored(PonderPalette.RED)
                .text("砸中的人会被迫吃下或喝下里面的东西：这杯酒里的毒照样生效")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showText(70)
                .text("他中了毒，在你视线里会高亮（方框示意）；不解毒的话约 40～70 秒后毒发")
                .independent();
        scene.idle(80);
        Actors.walk(scene, killer, new Vec3d(-0.4, 0, 3.6), 22);
        scene.idle(6);
        Actors.hold(scene, toxicologist, capsule);
        scene.idle(10);
        Actors.swing(scene, toxicologist);
        Actors.hold(scene, toxicologist, ItemStack.EMPTY);
        throwSprite(scene, capsule, hand(atTray, atKiller), new Vec3d(1.8, 1.05, 2.2), 3);
        scene.idle(3);
        scene.overlay().showText(80)
                .text("再扔一颗却没砸中人：胶囊一碰到方块就没了，里面的东西也一起浪费掉")
                .independent()
                .attachKeyFrame();
        scene.idle(30);
        Actors.leave(scene, killer, Direction.UP);
        scene.idle(60);
        scene.overlay().showText(80)
                .text("胶囊只有毒理学家能买（伪装成毒理学家的验尸官也能）；拿在手上别人看得见")
                .independent();
        scene.idle(90);
        scene.overlay().showText(100)
                .colored(PonderPalette.RED)
                .text("装的是普通毒，砸中好人一般也会把人毒死；不想毒死好人，就先用【蓝矾】把毒转成蓝毒，或者直接装【蓝颠茄】")
                .independent();
        scene.idle(110);
        scene.markAsFinished();
    }

    /**
     * Blue Belladonna (SparkStrength BlueBelladonnaItem, ToxicologistBlueRules, ToxicologistBluePassiveService,
     * CapsuleItem.forceConsume; SparkTraits ConsciencePoisonerService.triggerBlueTrap): 25 coins in the
     * Toxicologist's shop, hidden in hand. Eaten in 16 ticks; a Toxicologist (or a Coroner disguised as one) then has
     * 5 s of blue state: no blue sanity drain, +0.1 sanity a second and Speed II without particles. Anyone else who
     * eats it, by hand or from a Capsule, springs the buyer's blue trap: an effective civilian loses sanity for 3 s
     * (20 points a second), anyone else gets lethal blue poison credited to the buyer. The Toxicologist sees
     * blue-poisoned players in light blue (a box here). The antidote does not cure blue poison.
     * 蓝颠茄：毒理学家商店 25 金币，拿在手上别人看不见。吃下用 16 tick；毒理学家（或伪装成毒理学家的验尸官）随后进入 5 秒
     * 蓝毒状态：不掉蓝毒理智，每秒回 0.1 理智，并获得不冒粒子的速度 II。其他人吃下（亲手吃或被胶囊砸中）会触发购买者的蓝毒
     * 陷阱：实际阵营为好人的人 3 秒内每秒掉 20 点理智，其余的人中致命蓝毒并记在购买者名下。毒理学家看到中蓝毒的人为浅蓝色
     * （这里用方框表示）。解毒剂解不了蓝毒。
     */
    private static void blueBelladonna(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("ss_blue_belladonna", "蓝颠茄：自己吃了加速，砸人下蓝毒");
        WatheItemScenes.stage(scene);
        int toxicologistColor = RoleColors.of("noellesroles:toxicologist", 0xB8295A);
        ItemStack fruit = stack("sparkstrength:blue_belladonna");
        ItemStack capsule = stack("sparkstrength:capsule");
        Vec3d toxicologistStart = new Vec3d(5.6, 1, 0.8);
        Vec3d toxicologistEnd = new Vec3d(1.4, 1, 5.0);
        Vec3d walkerStart = new Vec3d(6.4, 1, 1.8);
        ElementLink<ActorElement> toxicologist = Actors.enter(scene, toxicologistColor, Text.literal("毒理学家"),
                toxicologistStart, SCREEN_RIGHT, Direction.DOWN);
        Actors.hold(scene, toxicologist, fruit);
        ElementLink<ActorElement> walker = Actors.enter(scene, RoleColors.CIVILIAN, Text.literal("平民"), walkerStart,
                SCREEN_RIGHT, Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(80)
                .text("【蓝颠茄】25 金币，拿在手上别的活人看不见")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        scene.overlay().showControls(toxicologistStart.add(0, 2.6, 0), Pointing.DOWN, 30).rightClick()
                .withItem(fruit);
        scene.overlay().showText(60)
                .text("按住右键吃下（约 0.8 秒），进入 5 秒蓝毒状态")
                .independent()
                .attachKeyFrame();
        Actors.charge(scene, toxicologist, true);
        scene.idle(16);
        Actors.charge(scene, toxicologist, false);
        Actors.hold(scene, toxicologist, ItemStack.EMPTY);
        scene.idle(5);
        Vec3d toxicologistDash = toxicologistEnd.subtract(toxicologistStart);
        int dash = (int) Math.round(toxicologistDash.length() / SPEED_II);
        Actors.walk(scene, toxicologist, toxicologistDash, dash);
        Actors.walk(scene, walker, toxicologistDash.normalize().multiply(WALK * dash), dash);
        scene.overlay().showText(100)
                .colored(PonderPalette.GREEN)
                .text("蓝毒状态下你不掉蓝毒的理智，反而每秒回 10%% 理智，还有速度 II（不冒粒子）")
                .independent()
                .attachKeyFrame();
        scene.idle(105);
        scene.overlay().showText(70)
                .text("效果只有 5 秒，过了就没了")
                .independent();
        scene.idle(20);
        Actors.leave(scene, walker, Direction.UP);
        scene.idle(60);
        Vec3d killerStart = new Vec3d(5.6, 1, 1.0);
        Vec3d killerSpot = new Vec3d(4.8, 1, 1.8);
        ElementLink<ActorElement> killer = Actors.enter(scene, RoleColors.KILLER, Text.literal("杀手"), killerStart,
                SCREEN_RIGHT, Direction.DOWN);
        Actors.hold(scene, killer, stack("wathe:knife"));
        scene.idle(10);
        Actors.walk(scene, killer, killerSpot.subtract(killerStart), 15);
        scene.idle(17);
        float atKiller = facing(toxicologistEnd, killerSpot);
        Actors.turn(scene, toxicologist, atKiller);
        scene.idle(8);
        scene.overlay().showControls(toxicologistEnd.add(0, 2.6, 0), Pointing.DOWN, 50).rightClick()
                .withItem(fruit);
        scene.overlay().showText(80)
                .text("蓝颠茄也能装进【胶囊】：在背包里把果实拿在鼠标上右键胶囊")
                .independent()
                .attachKeyFrame();
        scene.idle(45);
        Actors.hold(scene, toxicologist, capsule);
        scene.idle(45);
        scene.overlay().showControls(toxicologistEnd.add(0, 2.6, 0), Pointing.DOWN, 30).rightClick()
                .withItem(capsule);
        scene.idle(10);
        Actors.swing(scene, toxicologist);
        Actors.hold(scene, toxicologist, ItemStack.EMPTY);
        throwSprite(scene, capsule, hand(toxicologistEnd, atKiller), towards(killerSpot, toxicologistEnd, 1.3), 3);
        scene.idle(3);
        Actors.highlight(scene, killer, BLUE_POISON, 110);
        scene.overlay().showText(90)
                .colored(PonderPalette.RED)
                .text("砸中好人阵营以外的人：他中了你的致命蓝毒，你看得见他时显示为浅蓝色（方框示意）")
                .independent()
                .attachKeyFrame();
        scene.idle(110);
        Actors.fall(scene, killer);
        scene.overlay().showText(70)
                .text("约 40～70 秒后一般会毒发身亡（演示中缩短了时间），击杀记在买下这颗蓝颠茄的人名下")
                .independent();
        scene.idle(80);
        scene.overlay().showText(90)
                .text("好人被砸中或亲手吃下不会死，一般只是 3 秒内掉约 60%% 理智（带【内鬼】的好人照样会被毒死）")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showText(90)
                .colored(PonderPalette.RED)
                .text("解毒剂解不了蓝毒；理智已经掉到负数的好人再被砸，可能当场精神崩溃而死")
                .independent();
        scene.idle(100);
        scene.markAsFinished();
    }

    /**
     * Flashlight (SparkStrength FlashlightItem, FlashlightBlackoutService, AttendantFlashlightService,
     * FlashlightBeamRules and the client flashlight renderer; Wathe WorldBlackoutComponent): every Attendant gets one
     * at the start; it is not hidden in hand. A right-click toggles it (a swing, an action-bar note). While a lit
     * flashlight is held in either hand, the blackout's blindness, which Wathe refreshes every tick for non-killers,
     * is cancelled (killers and Veterans get Night Vision instead); switching it on mid-blackout clears the blindness
     * at once. Every client draws a lit flashlight as a real cone of light along the holder's look, about 28 blocks
     * long (not while an Iris shader pack is on; a blind viewer's fog hides it beyond a few blocks); Ponder cannot run
     * that shader, so the beam is drawn as lines. The stage is dimmed (StageLights) as in the blackout demo.
     * 手电筒：乘务员开局就有；拿在手上不隐藏。右键开关（会挥手，动作栏有提示）。主手或副手拿着开着的手电时，Wathe 每 tick
     * 给非杀手刷新的停电失明会被取消（杀手和老兵则获得夜视）；停电中途打开也会立刻解除失明。每个客户端都会把开着的手电
     * 沿持有者视线画成约 28 格长的真实光锥（开着 Iris 光影包时不画；失明者的迷雾会遮住几格外的光）；Ponder 跑不了那个
     * 着色器，所以光束用线条表示。舞台像停电演示一样调暗（StageLights）。
     */
    private static void flashlight(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("ss_flashlight", "手电筒：停电时照亮前方");
        WatheItemScenes.setStage(scene, util);
        Selection lamps = carriageLamps(util);
        ItemStack off = stack("sparkstrength:flashlight");
        ItemStack on = litFlashlight();
        Vec3d attendantSpot = new Vec3d(4.2, 1, 1.0);
        Vec3d civilianSpot = new Vec3d(2.2, 1, 2.0);
        ElementLink<ActorElement> attendant = Actors.enter(scene, RoleColors.of("noellesroles:attendant", 0x6495ED),
                Text.literal("乘务员"), attendantSpot, SCREEN_RIGHT, Direction.DOWN);
        Actors.hold(scene, attendant, off);
        ElementLink<ActorElement> civilian = Actors.enter(scene, RoleColors.CIVILIAN, Text.literal("平民"),
                civilianSpot, NORTH, Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(90)
                .text("乘务员开局带着一支【手电筒】：右键打开，再右键关上；拿在手上别人看得见")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        Vec3d killerStart = new Vec3d(2.2, 1, 4.6);
        ElementLink<ActorElement> killer = Actors.enter(scene, RoleColors.KILLER, Text.literal("杀手"), killerStart,
                NORTH, Direction.DOWN);
        Actors.hold(scene, killer, stack("wathe:knife"));
        scene.idle(15);
        scene.overlay().showControls(killerStart.add(0, 2.6, 0), Pointing.DOWN, 50)
                .withItem(stack("wathe:blackout"));
        scene.overlay().showText(80)
                .text("杀手开了停电：全车熄灯，杀手有夜视，其他大多数人都会失明")
                .independent()
                .attachKeyFrame();
        scene.idle(20);
        scene.world().modifyBlocks(lamps, state -> lamp(state, false), false);
        StageLights.lights(scene, false);
        scene.idle(30);
        Actors.turn(scene, civilian, EAST);
        scene.idle(40);
        scene.overlay().showControls(attendantSpot.add(0, 2.6, 0), Pointing.DOWN, 30).rightClick().withItem(off);
        scene.idle(10);
        Actors.swing(scene, attendant);
        Actors.hold(scene, attendant, on);
        Actors.lookPitch(scene, attendant, 10);
        Beam beam = beamOn(scene, attendant, 10, 6, 6);
        scene.overlay().showText(100)
                .colored(PonderPalette.GREEN)
                .text("打开手电并拿在手上（主手副手都行）就不会被停电致盲；停电中途打开也会立刻解除失明")
                .independent()
                .attachKeyFrame();
        scene.idle(110);
        scene.overlay().showText(90)
                .text("黄线是光束示意：游戏里手电会照亮视线前方一片锥形区域（最远约 28 格；开着 Iris 光影时不显示）")
                .independent();
        scene.idle(20);
        Actors.turn(scene, civilian, WEST);
        Vec3d killerSpot = new Vec3d(2.2, 1, 3.4);
        Actors.walk(scene, killer, killerSpot.subtract(killerStart), 25);
        scene.idle(80);
        scene.overlay().showText(90)
                .text("手电照到的人也会被照亮：你看得清是谁在摸黑靠近")
                .independent()
                .attachKeyFrame();
        scene.idle(60);
        Actors.walk(scene, killer, killerStart.subtract(killerSpot), 25);
        scene.idle(30);
        Actors.leave(scene, killer, Direction.UP);
        scene.idle(10);
        scene.overlay().showText(90)
                .colored(PonderPalette.RED)
                .text("停电时开着手电很显眼：有夜视的杀手远远就能看到这道光，失明的人一般只看得到身边几格")
                .independent();
        scene.idle(100);
        scene.overlay().showControls(attendantSpot.add(0, 2.6, 0), Pointing.DOWN, 30).rightClick().withItem(on);
        scene.idle(10);
        Actors.swing(scene, attendant);
        Actors.hold(scene, attendant, off);
        beamOff(scene, beam);
        Actors.lookPitch(scene, attendant, 0);
        scene.overlay().showText(80)
                .text("关掉手电或换成别的东西，下一刻又会被停电致盲")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        scene.world().modifyBlocks(lamps, state -> lamp(state, true), false);
        StageLights.lights(scene, true);
        Actors.turn(scene, civilian, NORTH);
        scene.overlay().showText(70)
                .text("停电结束，灯光恢复；不停电时开着手电也只是照亮前方")
                .independent();
        scene.idle(80);
        scene.markAsFinished();
    }

    /**
     * Professor's Sedative and Truth Serum (SparkStrength ProfessorSerumItem, ProfessorSerumService,
     * ProfessorSerumRules, ProfessorSedativeMoodMixin, ProfessorSerumClientHooks, HiddenEquipmentHelperMixin): bought
     * in the Professor's shop (50 and 125 coins), hidden in a Professor's hand. A right-click on a living player in the
     * crosshair feeds them (useOnEntity; use() also looks 1.5 blocks ahead); with nobody there the Professor drinks it.
     * The client
     * swings and a drink sound plays at the drinker; each serum is used up. Sedative: for 45 s Wathe's mood drain from
     * unfinished tasks stops (task rewards still count; other drains such as the Silencer's do not); Professors see
     * the drinker outlined yellow, through walls.
     * Truth Serum: every online player but the drinker gets an action-bar line naming the drinker's real role in its
     * colour; the drinker gets their own line; there is no duration. The inventory remote feed reaches any living
     * player and also uses up a serum: 90 s cooldown after a success, 40 s after a failure.
     * 教授的镇静试剂与吐真试剂：在教授商店购买（50 与 125 金币），教授拿在手上别人看不见。准心对着一名活人右键就喂给对方
     * （useOnEntity；use 也会向前找 1.5 格内的目标）；没对准人就自己喝。客户端挥手，喝的人身边响起喝东西的声音；试剂用一次就没。镇静：45 秒内
     * Wathe 因任务未完成而扣的理智停止（完成任务照样加；静语者等其他扣理智不受影响）；教授能隔墙看到喝的人黄色描边。吐真：除喝的人外的所有在线玩家
     * 动作栏都显示一行字，用身份颜色说出他的真实身份；喝的人自己收到另一行提示；没有持续时间。背包里的远程投喂可选任意
     * 存活玩家，同样用掉一瓶，成功后冷却 90 秒，失败冷却 40 秒。
     */
    private static void professorSerums(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("ss_professor_serums", "教授：镇静试剂与吐真试剂");
        WatheItemScenes.stage(scene);
        ItemStack sedative = stack("sparkstrength:sedative");
        ItemStack truth = stack("sparkstrength:truth_serum");
        Vec3d professorStart = new Vec3d(5.8, 1, 1.2);
        Vec3d professorSpot = new Vec3d(4.4, 1, 2.6);
        Vec3d civilianSpot = new Vec3d(3.4, 1, 3.6);
        ElementLink<ActorElement> civilian = Actors.enter(scene, RoleColors.CIVILIAN, Text.literal("平民"),
                civilianSpot, SCREEN_LEFT, Direction.DOWN);
        ElementLink<ActorElement> professor = Actors.enter(scene,
                RoleColors.of("noellesroles:professor", 0x4682B4), Text.literal("教授"), professorStart,
                SCREEN_RIGHT, Direction.DOWN);
        Actors.hold(scene, professor, sedative);
        scene.idle(15);
        scene.overlay().showText(90)
                .text("教授能在商店买【镇静试剂】和【吐真试剂】，拿在手上别的活人看不见")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        Actors.walk(scene, professor, professorSpot.subtract(professorStart), 20);
        scene.idle(25);
        scene.overlay().showControls(civilianSpot.add(0, 2.6, 0), Pointing.DOWN, 30).rightClick()
                .withItem(sedative);
        scene.idle(10);
        Actors.swing(scene, professor);
        Actors.hold(scene, professor, ItemStack.EMPTY);
        scene.overlay().showText(80)
                .text("准心对着身边的人右键就喂给对方；没对准人就是自己喝下")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        Actors.highlight(scene, civilian, SEDATIVE_YELLOW, 200);
        scene.overlay().showText(100)
                .colored(PonderPalette.GREEN)
                .text("【镇静试剂】：45 秒内他一般不会因为任务没做而掉理智（被静语者沉默时仍会掉），做完任务照样回理智")
                .independent()
                .attachKeyFrame();
        scene.idle(110);
        scene.overlay().showText(80)
                .text("效果期间，你能隔墙看到他的黄色描边（方框示意）")
                .independent();
        scene.idle(90);
        Actors.leave(scene, civilian, Direction.UP);
        scene.idle(20);
        Vec3d killerSpot = new Vec3d(2.4, 1, 4.6);
        Vec3d professorNear = new Vec3d(3.4, 1, 3.6);
        ElementLink<ActorElement> killer = Actors.enter(scene, RoleColors.KILLER, Text.literal("杀手"), killerSpot,
                SCREEN_LEFT, Direction.DOWN);
        Actors.hold(scene, professor, truth);
        scene.idle(15);
        Actors.walk(scene, professor, professorNear.subtract(professorSpot), 15);
        scene.idle(18);
        scene.overlay().showText(70)
                .text("怀疑一个人？给他喂一瓶【吐真试剂】")
                .independent()
                .attachKeyFrame();
        scene.idle(40);
        scene.overlay().showControls(killerSpot.add(0, 2.6, 0), Pointing.DOWN, 30).rightClick().withItem(truth);
        scene.idle(10);
        Actors.swing(scene, professor);
        Actors.hold(scene, professor, ItemStack.EMPTY);
        scene.overlay().showText(120)
                .colored(PonderPalette.RED)
                .text("除他本人外，所有在线玩家（含死者）的动作栏立刻显示他的具体身份：“……透露了自己的身份为:杀手”")
                .independent()
                .attachKeyFrame();
        scene.idle(130);
        scene.overlay().showText(80)
                .text("吐真试剂立刻生效、没有持续时间；喝下的人自己也会收到提示")
                .independent();
        scene.idle(30);
        Actors.walk(scene, killer, new Vec3d(-1.2, 0, 1.2), 20);
        scene.idle(25);
        Actors.leave(scene, killer, Direction.UP);
        scene.idle(35);
        scene.overlay().showText(100)
                .text("两种试剂都是一次性的；也能在背包界面远程喂给任意一名存活玩家（同样用掉一瓶；成功后冷却 90 秒，失败冷却 40 秒）")
                .independent();
        scene.idle(110);
        scene.markAsFinished();
    }

    /**
     * Capture Device (SparkStrength CaptureDeviceItem, CaptureDeviceEntity, CaptureDeviceEntityRenderer,
     * EngineerRules, EngineerStunnedPlayerComponent and its client input mixins, EngineerShopService,
     * HiddenEquipmentHelperMixin): 125 coins in the Engineer's shop, hidden in an Engineer's hand. A right-click on a
     * floor or ceiling places it (a private sound); it is drawn flat at 0.4 scale and only its owner and dead or
     * spectating players see it. The first tick any living player other than the owner is within 5 blocks of it (a
     * sphere measured from the feet, so through walls and floors, and at once if someone is already in range), every
     * such player is frozen in place for 5 s with input blocked, and the device is gone. The
     * owner gets a "capture report" paper naming them (replacing older ones), hidden in hand. Unused, it expires after
     * 2 minutes.
     * 捕捉装置：工程师商店 125 金币，工程师拿在手上别人看不见。右键地面或天花板放下（只有自己听得到提示音）；装置平放、
     * 缩小到 0.4 倍绘制，只有放置者本人和死亡或旁观的玩家看得到。只要有放置者以外的活人进入它 5 格内（从脚底算的球形范围，隔墙、上下层也算；放下时已在范围内也会立刻触发），当刻
     * 范围内的这些人都会被原地定住 5 秒、无法操作，装置随即消失。放置者收到一张写着他们名字的“捕捉检测报告”（替换旧报告），
     * 拿在手上别人看不见。2 分钟没人触发就自行消失。
     */
    private static void captureDevice(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("ss_capture_device", "捕捉装置：靠近就被定住");
        WatheItemScenes.stage(scene);
        ItemStack device = stack("sparkstrength:capture_device");
        Vec3d deviceAt = new Vec3d(1.0, 1, 1.0);
        Vec3d engineerStart = new Vec3d(2.8, 1, 2.8);
        Vec3d engineerSpot = new Vec3d(1.9, 1, 1.9);
        Vec3d engineerWatch = new Vec3d(0.8, 1, 4.2);
        ElementLink<ActorElement> engineer = Actors.enter(scene, RoleColors.of("noellesroles:engineer", 0xC8A03C),
                Text.literal("工程师"), engineerStart, SCREEN_RIGHT, Direction.DOWN);
        Actors.hold(scene, engineer, device);
        scene.idle(15);
        scene.overlay().showText(80)
                .text("工程师在商店买【捕捉装置】（125 金币），拿在手上别的活人看不见")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        Actors.walk(scene, engineer, engineerSpot.subtract(engineerStart), 12);
        scene.idle(15);
        Actors.lookPitch(scene, engineer, 50);
        scene.overlay().showControls(deviceAt.add(0, 0.6, 0), Pointing.DOWN, 30).rightClick().withItem(device);
        scene.idle(10);
        Actors.swing(scene, engineer);
        Actors.hold(scene, engineer, ItemStack.EMPTY);
        ElementLink<Placed> placed = place(scene, device, deviceAt, facing(engineerSpot, deviceAt));
        scene.overlay().showText(80)
                .text("右键地面（或天花板）放下：放好的装置只有你自己（和死者、旁观者）看得见")
                .independent()
                .attachKeyFrame();
        scene.idle(30);
        Actors.lookPitch(scene, engineer, 0);
        Actors.walk(scene, engineer, engineerWatch.subtract(engineerSpot), 15);
        scene.idle(60);
        Vec3d killerStart = new Vec3d(6.4, 1, 6.4);
        Vec3d killerStep = new Vec3d(-1.9, 0, -1.9);
        int approach = (int) Math.round(killerStep.length() / WALK);
        // The ring lasts until the device is gone. 范围线一直显示到装置消失。
        ring(scene, deviceAt.add(0, 0.05, 0), 5, 30 + 20 + 30 + approach);
        scene.overlay().showText(90)
                .text("以它为中心 5 格内（白线示意，隔墙、上下层也算），你以外的活人一进来就会触发")
                .independent()
                .attachKeyFrame();
        scene.idle(30);
        ElementLink<ActorElement> killer = Actors.enter(scene, RoleColors.KILLER, Text.literal("杀手"), killerStart,
                SCREEN_LEFT, Direction.DOWN);
        Actors.hold(scene, killer, stack("wathe:knife"));
        scene.idle(20);
        Actors.turn(scene, engineer, facing(engineerWatch, killerStart));
        scene.idle(30);
        Actors.walk(scene, killer, killerStep, approach);
        scene.idle(approach);
        // Frozen from here for the real 5 s (100 ticks). 从这里起被定住真实的 5 秒（100 tick）。
        removePlaced(scene, placed);
        scene.overlay().showText(90)
                .colored(PonderPalette.RED)
                .text("他一走进范围就被原地定住 5 秒，一般什么都做不了（他会听到一声铁砧响）；装置随即消失")
                .independent()
                .attachKeyFrame();
        scene.idle(20);
        ItemStack report = new ItemStack(Items.PAPER);
        report.set(DataComponentTypes.CUSTOM_NAME, Text.literal("捕捉检测报告").formatted(Formatting.GOLD));
        Actors.hold(scene, engineer, report);
        scene.idle(80);
        scene.overlay().showText(90)
                .text("你会收到一张【捕捉检测报告】，写着被定住的人的名字（放进背包，拿在手上别的活人看不见）")
                .independent()
                .attachKeyFrame();
        Actors.turn(scene, killer, SCREEN_RIGHT);
        scene.idle(5);
        Vec3d killerAway = new Vec3d(-2.0, 0, 2.0);
        int away = (int) Math.round(killerAway.length() / WALK);
        Actors.walk(scene, killer, killerAway, away);
        scene.idle(away);
        Actors.leave(scene, killer, Direction.UP);
        scene.idle(10);
        Actors.hold(scene, engineer, ItemStack.EMPTY);
        scene.idle(100 - 5 - away - 10);
        scene.overlay().showText(90)
                .text("装置只触发一次：当时范围内你以外的活人都会被定住，好人也一样；放下时 5 格内已经有人就会立刻触发")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showText(70)
                .text("放下后 2 分钟没人触发，它就会自己消失")
                .independent();
        scene.idle(80);
        scene.markAsFinished();
    }

    /**
     * Dying Watch (SparkStrength TimekeeperWatchItem, TimekeeperWatchService, TimekeeperConstants,
     * TimekeeperWatchClientHooks, TimekeeperWatchAttackMixin, SparkTraitsCompat; NoellesRoles IronManVialItem): the
     * Timekeeper's starting watch, hidden in hand; only a living Timekeeper (or a Coroner disguised as one) can use it.
     * A left-click switches the mode without attacking. A right-click (a swing) in item mode spends 100 coins and
     * clears the item cooldowns of the Timekeeper and of every other living innocent (Conscience killers included,
     * Impostors not); in ability mode it spends 75 coins and clears the targets' role-ability cooldowns, not the
     * Timekeeper's own (most NoellesRoles and SparkStrength ability cooldowns). Neutral and witch-faction players are
     * never targets. Targets get an action-bar note; coins are spent even with no target. The watch's own cooldown
     * is 40 s (item) or 30 s (ability), kept per mode. With the Impostor trait the targets flip to killers and other
     * Impostors. The demo uses the Professor's Iron Man vial, whose item cooldown is 5 minutes.
     * 濒毁怀表：计时员的开局物品，拿在手上别人看不见；只有存活的计时员（或伪装成计时员的验尸官）能用。左键切换模式，不会
     * 出手攻击。物品模式右键（会挥手）花 100 金币，清空计时员自己和其他所有存活好人（含善良杀手，不含内鬼）的物品冷却；技能
     * 模式花 75 金币，清空目标的职业技能冷却（多数 NoellesRoles 与 SparkStrength 技能），不含计时员自己。中立和魔女阵营
     * 永远不是目标。被刷新的人收到动作栏提示；没刷到人也照样扣钱。怀表自身冷却
     * 物品模式 40 秒、技能模式 30 秒，两种模式分开计算。带内鬼词条时目标反转为杀手和其他内鬼。演示用教授的铁人药剂，其
     * 物品冷却为 5 分钟。
     */
    private static void dyingWatch(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("ss_dying_watch", "濒毁怀表：替全体好人刷新冷却");
        WatheItemScenes.stage(scene);
        ItemStack watch = stack("sparkstrength:dying_watch");
        ItemStack vial = stack("noellesroles:iron_man_vial");
        Vec3d professorSpot = new Vec3d(3.4, 1, 3.0);
        Vec3d firstSpot = new Vec3d(1.6, 1, 5.0);
        Vec3d secondSpot = new Vec3d(1.6, 1, 2.6);
        Vec3d timekeeperSpot = new Vec3d(5.6, 1, 1.6);
        ElementLink<ActorElement> professor = Actors.enter(scene,
                RoleColors.of("noellesroles:professor", 0x4682B4), Text.literal("教授"), professorSpot,
                facing(professorSpot, firstSpot), Direction.DOWN);
        Actors.hold(scene, professor, vial);
        Actors.enter(scene, RoleColors.CIVILIAN, Text.literal("平民"), firstSpot, facing(firstSpot, professorSpot),
                Direction.DOWN);
        Actors.enter(scene, RoleColors.CIVILIAN, Text.literal("平民"), secondSpot, facing(secondSpot, professorSpot),
                Direction.DOWN);
        scene.idle(15);
        scene.overlay().showControls(firstSpot.add(0, 2.6, 0), Pointing.DOWN, 30).rightClick().withItem(vial);
        scene.idle(10);
        scene.overlay().showText(80)
                .text("教授刚给这位平民上了护盾：铁人药剂（拿在手上别人看不见）用一次要冷却 5 分钟")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        ElementLink<ActorElement> timekeeper = Actors.enter(scene,
                RoleColors.of("noellesroles:time_keeper", 0x0026FF), Text.literal("计时员"), timekeeperSpot,
                SCREEN_RIGHT, Direction.DOWN);
        Actors.hold(scene, timekeeper, watch);
        scene.idle(15);
        scene.overlay().showText(80)
                .text("计时员开局带着【濒毁怀表】，别的活人看不见你手里的它")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        scene.overlay().showControls(timekeeperSpot.add(0, 2.6, 0), Pointing.DOWN, 30).rightClick().withItem(watch);
        scene.idle(10);
        Actors.swing(scene, timekeeper);
        scene.overlay().showText(110)
                .colored(PonderPalette.GREEN)
                .text("怀表默认是物品模式。右键：花 100 金币，清空你自己和其他存活好人（含【善良】杀手，不含【内鬼】）的物品冷却")
                .independent()
                .attachKeyFrame();
        scene.idle(120);
        Actors.turn(scene, professor, facing(professorSpot, secondSpot));
        scene.idle(10);
        scene.overlay().showControls(secondSpot.add(0, 2.6, 0), Pointing.DOWN, 30).rightClick().withItem(vial);
        scene.idle(10);
        scene.overlay().showText(80)
                .colored(PonderPalette.GREEN)
                .text("被刷新的人会收到提示：教授的药剂马上又能用了，给第二个人也上了护盾")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        scene.overlay().showControls(timekeeperSpot.add(0, 2.6, 0), Pointing.DOWN, 40).leftClick().withItem(watch);
        scene.overlay().showText(80)
                .text("左键在物品模式和技能模式之间切换，不会出手打人")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        scene.overlay().showText(90)
                .text("技能模式右键花 75 金币，清空其他存活好人的大多数职业技能冷却（不含你自己）")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showText(90)
                .text("怀表自己也要冷却：物品模式 40 秒、技能模式 30 秒，分开计算；没刷到人也照样扣钱")
                .independent();
        scene.idle(100);
        scene.overlay().showText(90)
                .text("中立和魔女阵营刷不到；你自己带【内鬼】词条时，改为刷新杀手（不含【善良】）和其他内鬼")
                .independent();
        scene.idle(100);
        scene.markAsFinished();
    }

    /**
     * Aroma Orb (SparkStrength AromaOrbItem, AromaOrbEntity, AromaService, PerfumerKitService, PerfumerRules,
     * PerfumerScentComponent; client PerfumerAromaMouseMixin): 75 coins in the Perfumer's shop, hidden in a Perfumer's
     * hand, usable only by a living Perfumer. A right-click throws it fast and flat at 1.5 blocks a tick (a swing, no
     * cooldown, one used per throw); it never hits its thrower and spares no faction. It shatters on whatever it hits.
     * Only a direct player hit counts: the victim gets 3 s of random mouse
     * sensitivity and a drifting aim (their head visibly wanders; up to about 45 degrees a second sideways), a
     * lavender burst at the head and wisps over it every 5 ticks that everyone sees; only the thrower hears the hit
     * "ding" (everyone hears the break) and a lavender screen haze shows on the victim's screen only. A miss only
     * shatters (crumbs of the orb and a few wisps).
     * 香薰：调香师商店 75 金币，调香师拿在手上别人看不见，只有存活的调香师能用。右键以每 tick 1.5 格的速度平直扔出（会挥手，
     * 无冷却，每次用掉一个）；砸不到扔的人自己，也不分阵营。砸到什么都会碎。只有直接砸中玩家才算：被砸中的人 3 秒内鼠标灵敏度忽快忽慢、准心自己漂移（别人能看到
     * 他的头在乱转，横向每秒最多约 45 度），头顶炸开一团薰衣草色粒子，之后每 5 tick 冒出所有人都看得到的香气；命中的“叮”声
     * 只有扔的人听得到（碎瓶声人人听得到），被砸中者自己的屏幕上会蒙一层淡紫香雾。没砸中只会碎掉（香薰碎屑和少量香气）。
     */
    private static void aromaOrb(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("ss_aroma_orb", "香薰：砸中让人晕头转向");
        WatheItemScenes.stage(scene);
        ItemStack orb = stack("sparkstrength:aroma_orb");
        Vec3d perfumerSpot = new Vec3d(5.6, 1, 1.4);
        Vec3d killerSpot = new Vec3d(2.8, 1, 4.2);
        ElementLink<ActorElement> perfumer = Actors.enter(scene, RoleColors.of("sparkwitch:perfumer", 0xF2A4A4),
                Text.literal("调香师"), perfumerSpot, SCREEN_RIGHT, Direction.DOWN);
        Actors.hold(scene, perfumer, orb);
        ElementLink<ActorElement> killer = Actors.enter(scene, RoleColors.KILLER, Text.literal("杀手"), killerSpot,
                SCREEN_LEFT, Direction.DOWN);
        Actors.hold(scene, killer, stack("wathe:knife"));
        scene.idle(15);
        scene.overlay().showText(80)
                .text("调香师在商店买【香薰】（75 金币），拿在手上别的活人看不见")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        scene.overlay().showControls(perfumerSpot.add(0, 2.6, 0), Pointing.DOWN, 30).rightClick().withItem(orb);
        scene.idle(10);
        Actors.swing(scene, perfumer);
        throwSprite(scene, orb, hand(perfumerSpot, SCREEN_RIGHT), towards(killerSpot, perfumerSpot, 1.6), 3);
        scene.idle(3);
        Vec3d headTop = killerSpot.add(0, HEAD_TOP, 0);
        spawnParticles(scene, LAVENDER_BURST, headTop, 24, new Vec3d(0.35, 0.25, 0.35), 0);
        wisps(scene, headTop.add(0, 0.15, 0), 60);
        drift(scene, killer, SCREEN_LEFT, 60);
        scene.overlay().showText(100)
                .colored(PonderPalette.RED)
                .text("直接砸中人才生效（队友一般也会中）：他 3 秒内鼠标灵敏度忽快忽慢、视角自己乱飘，屏幕还蒙上一层香雾（只有他看得到）")
                .independent()
                .attachKeyFrame();
        scene.idle(110);
        scene.overlay().showText(90)
                .text("他头顶会冒淡紫色的香气，旁人都看得到；砸中时只有你听到“叮”的一声（碎瓶声大家都听得到）")
                .independent();
        scene.idle(100);
        Vec3d killerAway = new Vec3d(1.4, 1, 5.6);
        Actors.lookPitch(scene, killer, 0);
        Actors.walk(scene, killer, killerAway.subtract(killerSpot), 25);
        scene.idle(10);
        scene.overlay().showControls(perfumerSpot.add(0, 2.6, 0), Pointing.DOWN, 30).rightClick().withItem(orb);
        scene.idle(5);
        Actors.swing(scene, perfumer);
        Vec3d floor = new Vec3d(3.0, 1.1, 4.0);
        throwSprite(scene, orb, hand(perfumerSpot, SCREEN_RIGHT), floor, 3);
        scene.idle(3);
        Actors.hold(scene, perfumer, ItemStack.EMPTY);
        spawnParticles(scene, new ItemStackParticleEffect(ParticleTypes.ITEM, orb), floor, 6,
                new Vec3d(0.05, 0.05, 0.05), 0.1);
        spawnParticles(scene, LAVENDER_WISP, floor, 8, new Vec3d(0.25, 0.2, 0.25), 0);
        scene.overlay().showText(80)
                .text("没砸中人只会摔碎，什么效果都没有")
                .independent()
                .attachKeyFrame();
        scene.idle(30);
        Actors.leave(scene, killer, Direction.UP);
        scene.idle(60);
        scene.overlay().showText(90)
                .text("香薰没有冷却，扔一个少一个；只有调香师能用，也砸不到扔的人自己")
                .independent();
        scene.idle(100);
        scene.markAsFinished();
    }

    /**
     * Zephyr Perfume (SparkStrength ZephyrPerfumeItem, ZephyrService, PerfumerScentComponent, PerfumerShopService,
     * PerfumerZephyrMoodDrainMixin): 150 coins in the Perfumer's shop, one per round (none while it is active or a
     * bottle is carried), hidden in a Perfumer's hand, usable only by a living Perfumer. A right-click sprays it on
     * oneself (a swing): the mist and the sound reach only the user. For the rest of the round the Perfumer has an
     * infinite Speed II without particles (never replacing a stronger Speed) and half the mood drain; it ends on
     * death. A second spray while active is refused.
     * 晨风香水：调香师商店 150 金币，每局限购 1 瓶（生效中或身上已有一瓶时买不了），调香师拿在手上别人看不见，只有存活的
     * 调香师能用。右键喷在自己身上（会挥手）：喷雾和声音只有自己看得到、听得到。本局剩余时间里获得不冒粒子的无限速度 II（不会
     * 覆盖更强的速度效果），理智下降减半；死亡时结束。生效期间不能再喷。
     */
    private static void zephyrPerfume(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("ss_zephyr_perfume", "晨风香水：一整局的轻快脚步");
        WatheItemScenes.stage(scene);
        ItemStack perfume = stack("sparkstrength:zephyr_perfume");
        Vec3d perfumerStart = new Vec3d(5.6, 1, 0.8);
        Vec3d perfumerEnd = new Vec3d(1.4, 1, 5.0);
        Vec3d walkerStart = new Vec3d(6.4, 1, 1.8);
        ElementLink<ActorElement> perfumer = Actors.enter(scene, RoleColors.of("sparkwitch:perfumer", 0xF2A4A4),
                Text.literal("调香师"), perfumerStart, SCREEN_RIGHT, Direction.DOWN);
        Actors.hold(scene, perfumer, perfume);
        ElementLink<ActorElement> walker = Actors.enter(scene, RoleColors.CIVILIAN, Text.literal("平民"), walkerStart,
                SCREEN_RIGHT, Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(80)
                .text("【晨风香水】150 金币，每局只能买 1 瓶；拿在手上别的活人看不见")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        scene.overlay().showControls(perfumerStart.add(0, 2.6, 0), Pointing.DOWN, 30).rightClick()
                .withItem(perfume);
        scene.idle(10);
        Actors.swing(scene, perfumer);
        Actors.hold(scene, perfumer, ItemStack.EMPTY);
        spawnParticles(scene, ZEPHYR_MIST, perfumerStart.add(0, 1.1, 0), 10, new Vec3d(0.35, 0.45, 0.35), 0);
        scene.overlay().showText(80)
                .text("右键喷在自己身上：这团喷雾和声音只有你自己看得到、听得到")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        Vec3d perfumerDash = perfumerEnd.subtract(perfumerStart);
        int dash = (int) Math.round(perfumerDash.length() / SPEED_II);
        Actors.walk(scene, perfumer, perfumerDash, dash);
        Actors.walk(scene, walker, perfumerDash.normalize().multiply(WALK * dash), dash);
        scene.overlay().showText(100)
                .colored(PonderPalette.GREEN)
                .text("本局剩余时间里一直有速度 II，别人也看不到效果粒子")
                .independent()
                .attachKeyFrame();
        scene.idle(110);
        Actors.leave(scene, walker, Direction.UP);
        scene.overlay().showText(90)
                .text("任务带来的理智下降速度也减半（靠近尸体的额外下降同样减半），持续到本局结束；死了效果就没了")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showText(80)
                .text("效果还在时喷不了第二瓶；它不会顶掉更强的速度效果")
                .independent();
        scene.idle(90);
        scene.markAsFinished();
    }

    /**
     * Bartender spirits as they run with SparkStrength (NoellesRoles RumItem, GinItem, VodkaItem, TequilaItem,
     * BaseSpiritItem, StimulationEffect, StimulationCooldownMixin, the GIN_IMMUNITY blackout listener and the
     * collision mixins; SparkStrength BartenderRules, BartenderIngredientDurationMixin, BartenderVodkaMixin,
     * BartenderStimulationCrashMixin; SparkFactionAPI NoellesCollisionCompatibility; Wathe EntityMixin,
     * WorldBlackoutComponent): each takes effect when someone drinks a base spirit holding it, at once when the glass
     * is iced. Rum: Speed II for 10 s, no particles. Tequila: 12 s of No Collision, so the drinker and other players
     * pass through each other, though players are solid to each other once a round is 30 s old (SparkStrength
     * PlayerCollisionGraceRules). Gin: clears Blindness, gives Night Vision and Gin Immunity
     * for 20 s, which cancels the blackout's blindness. Vodka: 20 s of Stimulation (sprinting costs no stamina);
     * running item cooldowns drop to 60% at once and cooldowns set meanwhile are cut to 80%; at the end stamina
     * empties and the drinker is exhausted for about 15 s (SparkStrength drops NoellesRoles' Slowness II). The glass
     * adds 20% sanity and every spirit another 20%. Times shown are SparkStrength's; special liqueur doubles them.
     * 酒保的烈酒（按 SparkStrength 调整后）：有人喝下调了它的基酒时生效，加了冰块就立刻生效。朗姆酒：速度 II 10 秒，不冒粒子。
     * 龙舌兰：12 秒无碰撞，和别的玩家能互相穿过（开局 30 秒后玩家之间本是实心的）。金酒：解除失明，给夜视和金酒免疫 20 秒，免疫让停电不再
     * 致盲。伏特加：20 秒亢奋（疾跑不耗体力）；正在进行的物品冷却立刻剩 60%，期间新设的冷却打 8 折；结束时体力清零并进入
     * 疲惫约 15 秒（SparkStrength 去掉了 NoellesRoles 的缓慢 II）。这杯酒本身回 20% 理智，每种烈酒再多回 20%。时长为 SparkStrength 数值，特调利口酒
     * 会翻倍。
     */
    private static void spirits(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("ss_bartender_spirits", "酒保的四种烈酒：喝下去会怎样");
        WatheItemScenes.setStage(scene, util);
        Selection lamps = carriageLamps(util);
        scene.overlay().showText(100)
                .text("朗姆酒、金酒、伏特加、龙舌兰：调进基酒、喝下去才生效（这几杯都加了冰块，喝完立刻生效）")
                .independent()
                .attachKeyFrame();
        scene.idle(110);

        Vec3d rumStart = new Vec3d(4.4, 1, 0.6);
        Vec3d walkerStart = new Vec3d(4.6, 1, 1.8);
        ElementLink<ActorElement> rumDrinker = Actors.enter(scene, RoleColors.CIVILIAN, Text.literal("平民"),
                rumStart, SCREEN_RIGHT, Direction.DOWN);
        ItemStack rum = spirit("rum", "ice_cube");
        Actors.hold(scene, rumDrinker, rum);
        ElementLink<ActorElement> walker = Actors.enter(scene, RoleColors.CIVILIAN, Text.literal("平民"), walkerStart,
                SCREEN_RIGHT, Direction.DOWN);
        scene.idle(15);
        drink(scene, rumDrinker, rumStart, rum);
        scene.idle(10);
        int dash = 25;
        Vec3d diagonal = new Vec3d(-1, 0, 1).normalize();
        Actors.walk(scene, rumDrinker, diagonal.multiply(SPEED_II * dash), dash);
        Actors.walk(scene, walker, diagonal.multiply(WALK * dash), dash);
        scene.overlay().showText(90)
                .colored(PonderPalette.GREEN)
                .text("朗姆酒：速度 II，默认 10 秒（不冒效果粒子）")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        Actors.leave(scene, rumDrinker, Direction.UP);
        Actors.leave(scene, walker, Direction.UP);
        scene.idle(20);

        Vec3d tequilaStart = new Vec3d(4.4, 1, 0.8);
        Vec3d blockerSpot = new Vec3d(2.6, 1, 2.6);
        ElementLink<ActorElement> tequilaDrinker = Actors.enter(scene, RoleColors.CIVILIAN, Text.literal("平民"),
                tequilaStart, SCREEN_RIGHT, Direction.DOWN);
        ItemStack tequila = spirit("tequila", "ice_cube");
        Actors.hold(scene, tequilaDrinker, tequila);
        ElementLink<ActorElement> blocker = Actors.enter(scene, RoleColors.CIVILIAN, Text.literal("平民"),
                blockerSpot, SCREEN_LEFT, Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(80)
                .text("开局 30 秒后，玩家之间就是实心的，会互相挡住去路")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        drink(scene, tequilaDrinker, tequilaStart, tequila);
        scene.idle(10);
        double through = 3.6 * Math.sqrt(2);
        Actors.walk(scene, tequilaDrinker, diagonal.multiply(through), (int) Math.round(through / WALK));
        scene.overlay().showText(90)
                .colored(PonderPalette.GREEN)
                .text("龙舌兰：默认 12 秒内不和别人碰撞，能直接从人身上穿过去，别人也能穿过你")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        Actors.leave(scene, tequilaDrinker, Direction.UP);
        Actors.leave(scene, blocker, Direction.UP);
        scene.idle(20);

        Vec3d blindSpot = new Vec3d(2.0, 1, 2.0);
        Vec3d ginSpot = new Vec3d(4.0, 1, 1.0);
        Vec3d killerStart = new Vec3d(2.0, 1, 4.6);
        Vec3d killerSpot = new Vec3d(2.0, 1, 3.1);
        ElementLink<ActorElement> blind = Actors.enter(scene, RoleColors.CIVILIAN, Text.literal("平民"), blindSpot,
                NORTH, Direction.DOWN);
        ElementLink<ActorElement> ginDrinker = Actors.enter(scene, RoleColors.CIVILIAN, Text.literal("平民"),
                ginSpot, SCREEN_RIGHT, Direction.DOWN);
        ItemStack gin = spirit("gin", "ice_cube");
        Actors.hold(scene, ginDrinker, gin);
        scene.idle(10);
        ElementLink<ActorElement> killer = Actors.enter(scene, RoleColors.KILLER, Text.literal("杀手"), killerStart,
                NORTH, Direction.DOWN);
        Actors.hold(scene, killer, stack("wathe:knife"));
        scene.idle(15);
        scene.overlay().showControls(killerStart.add(0, 2.6, 0), Pointing.DOWN, 40)
                .withItem(stack("wathe:blackout"));
        scene.idle(10);
        scene.world().modifyBlocks(lamps, state -> lamp(state, false), false);
        StageLights.lights(scene, false);
        scene.overlay().showText(80)
                .text("停电了：大多数人都会失明")
                .independent()
                .attachKeyFrame();
        scene.idle(30);
        Actors.turn(scene, blind, SCREEN_LEFT);
        scene.idle(30);
        drink(scene, ginDrinker, ginSpot, gin);
        scene.overlay().showText(100)
                .colored(PonderPalette.GREEN)
                .text("金酒：立刻解除失明并获得夜视，默认 20 秒内停电也不会再让你失明")
                .independent()
                .attachKeyFrame();
        Actors.turn(scene, blind, NORTH);
        scene.idle(30);
        Actors.walk(scene, killer, killerSpot.subtract(killerStart), 20);
        scene.idle(25);
        Actors.charge(scene, killer, true);
        scene.idle(15);
        Actors.charge(scene, killer, false);
        Actors.swing(scene, killer);
        Actors.fall(scene, blind);
        scene.idle(40);
        scene.overlay().showText(90)
                .text("你看得清是谁摸黑下的手，失明的人却几乎什么也看不见")
                .independent()
                .attachKeyFrame();
        scene.idle(30);
        Actors.walk(scene, killer, killerStart.subtract(killerSpot), 20);
        scene.idle(22);
        Actors.leave(scene, killer, Direction.UP);
        scene.idle(48);
        scene.world().modifyBlocks(lamps, state -> lamp(state, true), false);
        StageLights.lights(scene, true);
        Actors.leave(scene, blind, Direction.UP);
        Actors.leave(scene, ginDrinker, Direction.UP);
        scene.idle(25);

        Vec3d vodkaStart = new Vec3d(4.4, 1, 0.8);
        ElementLink<ActorElement> vodkaDrinker = Actors.enter(scene, RoleColors.CIVILIAN, Text.literal("平民"),
                vodkaStart, SCREEN_RIGHT, Direction.DOWN);
        ItemStack vodka = spirit("vodka", "ice_cube");
        Actors.hold(scene, vodkaDrinker, vodka);
        scene.idle(15);
        drink(scene, vodkaDrinker, vodkaStart, vodka);
        scene.overlay().showText(90)
                .colored(PonderPalette.GREEN)
                .text("伏特加：默认亢奋 20 秒，这期间疾跑不消耗体力")
                .independent()
                .attachKeyFrame();
        Vec3d[] lap = {new Vec3d(-3.6, 0, 0), new Vec3d(0, 0, 3.6), new Vec3d(3.6, 0, 0), new Vec3d(0, 0, -3.6)};
        int leg = (int) Math.round(3.6 / SPRINT);
        for (Vec3d side : lap) {
            Actors.walk(scene, vodkaDrinker, side, leg);
            scene.idle(leg);
        }
        scene.idle(Math.max(0, 95 - 4 * leg));
        scene.overlay().showText(100)
                .text("喝下的一刻，所有物品的剩余冷却打 6 折；亢奋期间新产生的物品冷却只算 8 成")
                .independent()
                .attachKeyFrame();
        for (Vec3d side : lap) {
            Actors.walk(scene, vodkaDrinker, side, leg);
            scene.idle(leg);
        }
        scene.idle(Math.max(0, 105 - 4 * leg));
        Actors.walk(scene, vodkaDrinker, diagonal.multiply(WALK * 20), 20);
        scene.idle(20);
        scene.overlay().showText(90)
                .colored(PonderPalette.RED)
                .text("效果结束时体力清零并进入疲惫，默认约 15 秒跑不起来（演示中缩短了时间；不再附带缓慢）")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showText(80)
                .text("这杯酒本身回 20%% 理智，每加一种烈酒再多回 20%%；加了特调利口酒，这些时长都翻倍")
                .independent();
        scene.idle(90);
        scene.markAsFinished();
    }

    /**
     * Bartender modifiers (NoellesRoles BaseSpiritItem, IceCubeItem, SpecialLiqueurItem, SpecialSpiceItem; SparkStrength
     * GhostflameBittersItem, BartenderCooldownService, EmberSugarItem, BartenderCocktailUseTimeMixin, BartenderRules;
     * Wathe GameConstants revolver cooldown): they have no drink effect of their own and count toward a glass's three
     * ingredients. Without ice a base spirit first gives Slowness II and Blindness for 3 s (the effects and sanity come
     * after, if the drinker still lives); an ice cube skips that. Special liqueur doubles every duration in the glass,
     * the debuff included. Special spice adds 60% sanity when the glass takes effect (a HUD bar). Ghostflame bitters
     * clear the cooldown of every item the drinker carries when the glass takes effect (shop and ability cooldowns
     * stay; a few cooldowns that a running state holds up come back). Ember sugar makes the glass drink in one tick
     * instead of 40; it does not replace the ice. A glass with the bitters is named 冠死以冕, with the sugar 冠生以花,
     * with both 生死相依.
     * 酒保的修饰材料：本身没有酒效，也算在每杯最多 3 种材料里。没加冰的基酒喝完先缓慢 II 加失明 3 秒（之后若人还活着才回理智、
     * 触发效果）；冰块免掉这一步。特调利口酒让杯里所有时长翻倍，负面效果也一样。特调香料在酒生效时多回 60% 理智（只显示在
     * 界面上）。幽焰苦精在酒生效时清空饮用者身上所有物品的冷却（商店与技能冷却不变）。火种方糖让这杯酒 1 tick 喝完，而不是
     * 40 tick；它不能代替冰块。加苦精的酒叫冠死以冕，加方糖的叫冠生以花，两样都加叫生死相依。
     */
    private static void modifiers(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("ss_bartender_modifiers", "酒保的修饰材料：喝法和效果的变化");
        WatheItemScenes.stage(scene);
        scene.overlay().showText(100)
                .text("修饰材料不是烈酒：调进基酒后改变这杯酒的喝法和效果（也算在每杯最多 3 种材料里，同种不能加两次）")
                .independent()
                .attachKeyFrame();
        scene.idle(110);
        Vec3d drinkerStart = new Vec3d(5.6, 1, 1.0);
        Vec3d diagonal = new Vec3d(-1, 0, 1).normalize();
        ElementLink<ActorElement> plain = Actors.enter(scene, RoleColors.CIVILIAN, Text.literal("平民"), drinkerStart,
                SCREEN_RIGHT, Direction.DOWN);
        ItemStack plainRum = spirit("rum");
        Actors.hold(scene, plain, plainRum);
        scene.idle(15);
        scene.overlay().showText(60)
                .text("这杯只加了朗姆酒，没加冰块")
                .independent()
                .attachKeyFrame();
        scene.idle(30);
        drink(scene, plain, drinkerStart, plainRum);
        Actors.walk(scene, plain, diagonal.multiply(SLOWNESS_II * 24), 24);
        scene.overlay().showText(90)
                .colored(PonderPalette.RED)
                .text("没加【冰块】：喝完先缓慢 II 加失明 3 秒（失明的黑屏只有自己看得到）")
                .independent()
                .attachKeyFrame();
        scene.idle(60);
        Actors.walk(scene, plain, diagonal.multiply(SPEED_II * 10), 10);
        scene.overlay().showText(90)
                .text("之后（人还活着才算）才回酒的理智、触发材料效果：朗姆酒的加速这才开始（演示中缩短了时间）")
                .independent();
        scene.idle(100);
        Actors.leave(scene, plain, Direction.UP);
        scene.idle(15);
        ElementLink<ActorElement> iced = Actors.enter(scene, RoleColors.CIVILIAN, Text.literal("平民"), drinkerStart,
                SCREEN_RIGHT, Direction.DOWN);
        ItemStack icedRum = spirit("rum", "ice_cube");
        Actors.hold(scene, iced, icedRum);
        scene.idle(15);
        scene.overlay().showControls(drinkerStart.add(0, 2.6, 0), Pointing.DOWN, 40)
                .withItem(stack("noellesroles:ice_cube"));
        scene.idle(45);
        drink(scene, iced, drinkerStart, icedRum);
        Actors.walk(scene, iced, diagonal.multiply(SPEED_II * 20), 20);
        scene.overlay().showText(90)
                .colored(PonderPalette.GREEN)
                .text("加了【冰块】：没有这段负面效果，喝完立刻生效")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        Actors.leave(scene, iced, Direction.UP);
        scene.idle(15);
        Vec3d bartenderSpot = new Vec3d(3.5, 1, 3.5);
        ElementLink<ActorElement> bartender = Actors.enter(scene, RoleColors.of("noellesroles:bartender", 0xD9F1F0),
                Text.literal("酒保"), bartenderSpot, SCREEN_LEFT, Direction.DOWN);
        scene.idle(15);
        scene.overlay().showControls(bartenderSpot.add(0, 2.6, 0), Pointing.DOWN, 90)
                .withItem(stack("noellesroles:special_liqueur"));
        scene.overlay().showText(90)
                .text("【特调利口酒】：这杯所有效果的时长翻倍，没加冰时的负面效果也翻倍（3 秒变 6 秒）")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showControls(bartenderSpot.add(0, 2.6, 0), Pointing.DOWN, 90)
                .withItem(stack("noellesroles:special_spice"));
        scene.overlay().showText(90)
                .text("【特调香料】：酒生效时额外回复 60%% 理智（理智条只显示在自己的界面上）")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        Actors.leave(scene, bartender, Direction.UP);
        scene.idle(15);

        Vec3d vigilanteSpot = new Vec3d(5.8, 1, 1.2);
        ElementLink<ActorElement> vigilante = Actors.enter(scene, RoleColors.VIGILANTE, Text.literal("义警"),
                vigilanteSpot, SCREEN_RIGHT, Direction.DOWN);
        ItemStack revolver = stack("wathe:revolver");
        Actors.hold(scene, vigilante, revolver);
        Vec3d killerStart = new Vec3d(1.6, 1, 2.8);
        Vec3d killerSpot = new Vec3d(2.9, 1, 4.1);
        ElementLink<ActorElement> killer = Actors.enter(scene, RoleColors.KILLER, Text.literal("杀手"), killerStart,
                SOUTH, Direction.DOWN);
        Actors.hold(scene, killer, stack("wathe:knife"));
        scene.idle(15);
        Actors.walk(scene, killer, killerSpot.subtract(killerStart), 18);
        scene.idle(6);
        WatheItemScenes.shoot(scene, vigilante, REVOLVER_MUZZLE, new Vec3d(2.9, 1.9, 4.1));
        scene.idle(12);
        Actors.turn(scene, killer, SCREEN_LEFT);
        scene.overlay().showText(70)
                .text("义警一枪打空：左轮要冷却一会儿才能再开")
                .independent()
                .attachKeyFrame();
        scene.idle(30);
        ItemStack special = spirit("ice_cube", "ghostflame_bitters", "ember_sugar");
        Actors.hold(scene, vigilante, special);
        scene.idle(10);
        scene.overlay().showControls(vigilanteSpot.add(0, 2.6, 0), Pointing.DOWN, 40)
                .withItem(stack("sparkstrength:ember_sugar"));
        scene.overlay().showText(80)
                .text("他手里这杯加了冰块、【幽焰苦精】和【火种方糖】")
                .independent();
        scene.idle(50);
        scene.overlay().showControls(vigilanteSpot.add(0, 2.6, 0), Pointing.DOWN, 20).rightClick().withItem(special);
        scene.idle(5);
        Actors.charge(scene, vigilante, true);
        scene.idle(1);
        Actors.charge(scene, vigilante, false);
        Actors.hold(scene, vigilante, ItemStack.EMPTY);
        scene.overlay().showText(70)
                .text("【火种方糖】：点一下右键就喝完，不用按住 2 秒")
                .independent()
                .attachKeyFrame();
        scene.idle(30);
        Actors.hold(scene, vigilante, revolver);
        scene.idle(10);
        WatheItemScenes.shoot(scene, vigilante, REVOLVER_MUZZLE, new Vec3d(2.9, 1.9, 4.1));
        Actors.fall(scene, killer);
        scene.idle(20);
        scene.overlay().showText(90)
                .colored(PonderPalette.GREEN)
                .text("【幽焰苦精】：酒生效时清空你身上物品的冷却（少数持续状态维持的冷却除外），左轮马上又能开枪")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showText(80)
                .text("苦精只清物品冷却：商店冷却和按键技能的冷却不受影响（职业道具的冷却算物品冷却）")
                .independent();
        scene.idle(90);
        scene.overlay().showText(100)
                .text("方糖不能代替冰块。加了苦精的酒叫“冠死以冕”，加了方糖的叫“冠生以花”，两样都加叫“生死相依”")
                .independent();
        scene.idle(110);
        scene.markAsFinished();
    }

    // ------------------------------------------------------------------ helpers / 辅助

    /** Hold right-click for a base spirit's 2 s drink, then put the empty hand down. 按住右键 2 秒喝完基酒，然后空手。 */
    private static void drink(SceneBuilder scene, ElementLink<ActorElement> drinker, Vec3d feet, ItemStack glass) {
        scene.overlay().showControls(feet.add(0, 2.6, 0), Pointing.DOWN, DRINK_TICKS).rightClick().withItem(glass);
        Actors.charge(scene, drinker, true);
        scene.idle(DRINK_TICKS);
        Actors.charge(scene, drinker, false);
        Actors.hold(scene, drinker, ItemStack.EMPTY);
    }

    /** A base spirit with these ingredients, as BaseSpiritItem stores them. 按 BaseSpiritItem 的存法调好这些材料的基酒。 */
    private static ItemStack spirit(String... ingredients) {
        ItemStack glass = stack("noellesroles:base_spirit");
        NbtList list = new NbtList();
        for (String id : ingredients) {
            list.add(NbtString.of(id));
        }
        NbtCompound data = new NbtCompound();
        data.put("ingredients", list);
        glass.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(data));
        return glass;
    }

    /** A switched-on flashlight (FlashlightItem.setOn: custom model data 1). 打开的手电筒（自定义模型数据 1）。 */
    private static ItemStack litFlashlight() {
        ItemStack lit = stack("sparkstrength:flashlight");
        lit.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent(1));
        return lit;
    }

    /** The four wall lamps of the lit_carriage stage. lit_carriage 舞台上的四盏壁灯。 */
    private static Selection carriageLamps(SceneBuildingUtil util) {
        return util.select().position(1, 2, 5).add(util.select().position(4, 2, 5))
                .add(util.select().position(5, 2, 1)).add(util.select().position(5, 2, 4));
    }

    /** A Wathe lamp powered or cut, as WorldBlackoutComponent sets lit and active. 像 WorldBlackoutComponent 那样设置灯的 lit 与 active。 */
    private static BlockState lamp(BlockState state, boolean on) {
        if (state.contains(Properties.LIT)) {
            state = state.with(Properties.LIT, on);
        }
        return state.contains(WatheProperties.ACTIVE) ? state.with(WatheProperties.ACTIVE, on) : state;
    }

    /** The yaw that faces from {@code from} towards {@code to}. 从 from 面向 to 的朝向。 */
    private static float facing(Vec3d from, Vec3d to) {
        return (float) (MathHelper.atan2(from.x - to.x, to.z - from.z) * MathHelper.DEGREES_PER_RADIAN);
    }

    private static Vec3d forward(float yaw) {
        float radians = yaw * MathHelper.RADIANS_PER_DEGREE;
        return new Vec3d(-MathHelper.sin(radians), 0, MathHelper.cos(radians));
    }

    /** The right-hand side of an actor facing {@code yaw}. 朝 yaw 的演员的右手一侧。 */
    private static Vec3d right(float yaw) {
        float radians = yaw * MathHelper.RADIANS_PER_DEGREE;
        return new Vec3d(-MathHelper.cos(radians), 0, -MathHelper.sin(radians));
    }

    /** Roughly where a standing actor's right hand lets go of a throw. 站立演员右手出手的大致位置。 */
    private static Vec3d hand(Vec3d feet, float yaw) {
        return feet.add(0, 1.45, 0).add(forward(yaw).multiply(0.4)).add(right(yaw).multiply(0.3));
    }

    /**
     * A point on {@code target}'s body at {@code height}, on the side facing {@code from}: where a thrown item meets
     * their box. 目标身上朝向 from 一侧、高度 height 处的点：投掷物碰到其碰撞箱的位置。
     */
    private static Vec3d towards(Vec3d target, Vec3d from, double height) {
        Vec3d side = from.subtract(target).multiply(1, 0, 1).normalize().multiply(0.3);
        return target.add(side).add(0, height, 0);
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

    /**
     * Aroma wisps over a perfumed head for {@code ticks}: two every 5 ticks (AromaService.spawnHeadParticles).
     * 被香薰砸中者头顶的香气，持续 ticks：每 5 tick 两颗。
     */
    private static void wisps(SceneBuilder scene, Vec3d at, int ticks) {
        scene.addInstruction(new Every(5, ticks, ponder -> {
            PonderLevel world = ponder.getWorld();
            for (int i = 0; i < 2; i++) {
                world.addParticle(LAVENDER_WISP, at.x + world.random.nextGaussian() * 0.22,
                        at.y + world.random.nextGaussian() * 0.08, at.z + world.random.nextGaussian() * 0.22, 0, 0, 0);
            }
        }));
    }

    /**
     * The perfumed player's aim wandering for {@code ticks} around {@code yaw}, within Aroma's drift limits (45
     * degrees a second sideways, 20 up and down), fading in and out as AromaService's envelope does.
     * 被香薰砸中者的准心在 ticks 内绕 yaw 漂移，不超过香薰的漂移上限（横向每秒 45 度、纵向 20 度），像香薰的包络一样渐入渐出。
     */
    private static void drift(SceneBuilder scene, ElementLink<ActorElement> actor, float yaw, int ticks) {
        scene.addInstruction(new Wander(actor, yaw, ticks));
    }

    /**
     * An illustrated floor ring of {@code radius} around {@code centre}, drawn only over the 7-block plate.
     * 以 centre 为圆心、半径 radius 的示意地面圆圈，只画在 7 格底板范围内。
     */
    private static void ring(SceneBuilder scene, Vec3d centre, double radius, int ticks) {
        int segments = 64;
        for (int i = 0; i < segments; i++) {
            double a = MathHelper.TAU * i / segments;
            double b = MathHelper.TAU * (i + 1) / segments;
            Vec3d from = centre.add(Math.cos(a) * radius, 0, Math.sin(a) * radius);
            Vec3d to = centre.add(Math.cos(b) * radius, 0, Math.sin(b) * radius);
            if (onPlate(from) && onPlate(to)) {
                scene.overlay().showLine(PonderPalette.WHITE, from, to, ticks);
            }
        }
    }

    private static boolean onPlate(Vec3d point) {
        return point.x >= 0 && point.x <= 7 && point.z >= 0 && point.z <= 7;
    }

    /** A thrown item's flight as a camera-facing sprite, gone on arrival. 投掷物飞行（朝向镜头的物品图片），到达即消失。 */
    private static void throwSprite(SceneBuilder scene, ItemStack stack, Vec3d from, Vec3d to, int ticks) {
        Flight flight = new Flight(stack, from, to, ticks);
        scene.addInstruction(ponder -> {
            flight.setVisible(true);
            flight.setFade(1);
            ponder.addElement(flight);
        });
    }

    /** Set a placed Capture Device down on the floor. 把放好的捕捉装置放在地上。 */
    private static ElementLink<Placed> place(SceneBuilder scene, ItemStack stack, Vec3d at, float yaw) {
        Placed placed = new Placed(stack, at, yaw);
        ElementLink<Placed> link = new ElementLinkImpl<>(Placed.class);
        scene.addInstruction(ponder -> {
            placed.setVisible(true);
            placed.setFade(1);
            ponder.addElement(placed);
            ponder.linkElement(placed, link);
        });
        return link;
    }

    /** The device is gone (triggered). 装置消失（已触发）。 */
    private static void removePlaced(SceneBuilder scene, ElementLink<Placed> link) {
        scene.addInstruction(ponder -> {
            Placed placed = ponder.resolve(link);
            if (placed != null) {
                placed.setVisible(false);
            }
        });
    }

    /**
     * Start the illustrated beam of {@code holder}'s lit flashlight, aimed {@code pitch} degrees down as the holder
     * looks (set the same pitch on the actor), kept inside {@code maxX} by {@code maxZ} (the stage's walls) until
     * {@link #beamOff}.
     * 开始绘制 holder 手中开着的手电的示意光束，沿持有者的视线朝下 pitch 度（演员需设成同样的俯仰），限制在 maxX × maxZ 内
     * （舞台的墙），直到 beamOff。
     */
    private static Beam beamOn(SceneBuilder scene, ElementLink<ActorElement> holder, float pitch, double maxX,
                               double maxZ) {
        Beam beam = new Beam(holder, pitch, maxX, maxZ);
        scene.addInstruction(beam);
        return beam;
    }

    private static void beamOff(SceneBuilder scene, Beam beam) {
        scene.addInstruction(ponder -> beam.stopped = true);
    }

    /** Runs {@code action} every {@code period} ticks for {@code ticks}, alongside the script. 与脚本并行，在 ticks 内每 period tick 执行一次 action。 */
    private static final class Every extends TickingInstruction {
        private final int period;
        private final Consumer<PonderScene> action;

        private Every(int period, int ticks, Consumer<PonderScene> action) {
            super(false, ticks);
            this.period = period;
            this.action = action;
        }

        @Override
        public void tick(PonderScene scene) {
            super.tick(scene);
            if ((totalTicks - remainingTicks) % period == 0) {
                action.accept(scene);
            }
        }
    }

    /**
     * Aroma's wandering aim around {@code yaw}: a smooth sway within its drift limits, fading in over 3 ticks and out
     * over the last 10 (PerfumerRules.AROMA_FADE_IN_TICKS / AROMA_FADE_OUT_TICKS).
     * 香薰造成的准心绕 yaw 漂移：在漂移上限内平滑摆动，前 3 tick 渐入、最后 10 tick 渐出。
     */
    private static final class Wander extends TickingInstruction {
        /** Sideways sway: 25 degrees at 0.09 rad a tick turns at most 2.25 degrees a tick (45 a second). 横向摆动：每 tick 至多 2.25 度（每秒 45 度）。 */
        private static final float YAW_SWAY = 25;
        private static final float YAW_RATE = 0.09f;
        /** Up-and-down sway: 9 degrees at 0.1 rad a tick turns at most 0.9 degrees a tick (18 a second). 纵向摆动：每 tick 至多 0.9 度。 */
        private static final float PITCH_SWAY = 9;
        private static final float PITCH_RATE = 0.1f;
        private static final int FADE_IN = 3;
        private static final int FADE_OUT = 10;
        private final ElementLink<ActorElement> link;
        private final float yaw;
        private float yawOffset;
        private float pitchOffset;

        private Wander(ElementLink<ActorElement> link, float yaw, int ticks) {
            super(false, ticks);
            this.link = link;
            this.yaw = yaw;
        }

        @Override
        public void reset(PonderScene scene) {
            super.reset(scene);
            yawOffset = 0;
            pitchOffset = 0;
        }

        @Override
        public void tick(PonderScene scene) {
            super.tick(scene);
            ActorElement actor = scene.resolve(link);
            if (actor == null) {
                return;
            }
            // The envelope scales the drift speed, as the client's aim drift does, so the view stays where it
            // wandered once the effect ends. 包络缩放的是漂移速度（与客户端的准心漂移一致），效果结束时视角停在漂到的位置。
            int age = totalTicks - remainingTicks;
            float strength = MathHelper.clamp(Math.min(age / (float) FADE_IN, remainingTicks / (float) FADE_OUT), 0, 1);
            yawOffset += strength * YAW_SWAY * YAW_RATE * MathHelper.cos(age * YAW_RATE);
            pitchOffset += strength * PITCH_SWAY * PITCH_RATE * MathHelper.cos(age * PITCH_RATE);
            actor.turnTo(yaw + yawOffset);
            actor.setPitch(pitchOffset);
        }
    }

    /**
     * The illustrated beam of a lit flashlight: rays fanning 20 degrees (FlashlightBeamRules.OUTER_HALF_ANGLE) around
     * the holder's look, from the lens of the flashlight in the right hand (held at the hip, pointing along the look,
     * as FlashlightAim poses it) to where they meet the floor or the stage's walls, plus the rim joining their ends.
     * Ponder cannot run SparkStrength's flashlight shader, so lines stand in for it.
     * 开着的手电的示意光束：以持有者视线为轴、半角 20 度散开的射线，从右手里手电的镜片（FlashlightAim 让它在腰间顺着视线
     * 指向前方）画到碰上地面或舞台墙壁处，再把末端连成一圈。Ponder 跑不了 SparkStrength 的手电着色器，所以用线条代替。
     */
    private static final class Beam extends TickingInstruction {
        private static final int RAYS = 12;
        private static final double HALF_ANGLE = Math.toRadians(20);
        private static final double LONGEST = 6;
        private static final double FLOOR = 1.0;
        private final ElementLink<ActorElement> holder;
        private final double tilt;
        private final double maxX;
        private final double maxZ;
        private final Object[] raySlots = new Object[RAYS + 1];
        private final Object[] rimSlots = new Object[RAYS];
        private boolean stopped;

        private Beam(ElementLink<ActorElement> holder, float pitch, double maxX, double maxZ) {
            super(false, UNTIL_STOPPED);
            this.holder = holder;
            this.tilt = Math.toRadians(pitch);
            this.maxX = maxX;
            this.maxZ = maxZ;
            for (int i = 0; i < raySlots.length; i++) {
                raySlots[i] = new Object();
            }
            for (int i = 0; i < rimSlots.length; i++) {
                rimSlots[i] = new Object();
            }
        }

        @Override
        public void reset(PonderScene scene) {
            super.reset(scene);
            stopped = false;
        }

        @Override
        public void tick(PonderScene scene) {
            super.tick(scene);
            ActorElement actor = scene.resolve(holder);
            if (stopped || actor == null) {
                return;
            }
            float yaw = actor.yaw();
            Vec3d ahead = forward(yaw);
            Vec3d side = right(yaw);
            Vec3d lens = actor.position().add(0, 0.85, 0).add(ahead.multiply(0.5)).add(side.multiply(0.37));
            Vec3d axis = ahead.multiply(Math.cos(tilt)).add(0, -Math.sin(tilt), 0);
            Vec3d up = side.crossProduct(axis).normalize();
            if (up.y < 0) {
                up = up.multiply(-1);
            }
            Vec3d[] ends = new Vec3d[RAYS];
            for (int i = 0; i < RAYS; i++) {
                double around = MathHelper.TAU * i / RAYS;
                Vec3d ray = axis.multiply(Math.cos(HALF_ANGLE))
                        .add(side.multiply(Math.cos(around) * Math.sin(HALF_ANGLE)))
                        .add(up.multiply(Math.sin(around) * Math.sin(HALF_ANGLE)));
                ends[i] = lens.add(ray.multiply(reach(lens, ray)));
                scene.getOutliner().showLine(raySlots[i], lens, ends[i]).lineWidth(1 / 32f).colored(BEAM_YELLOW);
            }
            Vec3d centre = lens.add(axis.multiply(reach(lens, axis)));
            scene.getOutliner().showLine(raySlots[RAYS], lens, centre).lineWidth(1 / 32f).colored(BEAM_YELLOW);
            for (int i = 0; i < RAYS; i++) {
                scene.getOutliner().showLine(rimSlots[i], ends[i], ends[(i + 1) % RAYS]).lineWidth(1 / 32f)
                        .colored(BEAM_YELLOW);
            }
        }

        /** How far the ray runs before the floor, the plate's edge or a wall stops it. 射线碰到地面、底板边缘或墙之前的长度。 */
        private double reach(Vec3d from, Vec3d ray) {
            double length = LONGEST;
            if (ray.y < 0) {
                length = Math.min(length, (from.y - FLOOR) / -ray.y);
            }
            length = Math.min(length, bound(from.x, ray.x, maxX));
            length = Math.min(length, bound(from.z, ray.z, maxZ));
            return length;
        }

        private static double bound(double at, double step, double max) {
            if (step > 0) {
                return (max - at) / step;
            }
            if (step < 0) {
                return at / -step;
            }
            return Double.MAX_VALUE;
        }

        @Override
        public boolean isComplete() {
            return stopped || super.isComplete();
        }
    }

    /**
     * A placed Capture Device as CaptureDeviceEntityRenderer draws it: the item laid flat on the floor, 0.4 scale,
     * turned to the placer's facing. CaptureDeviceEntityRenderer 绘制的放置后的捕捉装置：物品平放在地上、缩小到 0.4 倍、
     * 转向放置者的朝向。
     */
    private static final class Placed extends AnimatedSceneElementBase {
        private final ItemStack stack;
        private final Vec3d at;
        private final float yaw;

        private Placed(ItemStack stack, Vec3d at, float yaw) {
            this.stack = stack.copy();
            this.at = at;
            this.yaw = yaw;
        }

        @Override
        protected void renderLast(PonderLevel world, VertexConsumerProvider buffer, DrawContext graphics, float fade,
                                  float pt) {
            if (fade <= 0.01f) {
                return;
            }
            MatrixStack ms = graphics.getMatrices();
            ms.push();
            ms.translate(at.x, at.y + 0.02, at.z);
            ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-yaw));
            ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90));
            ms.scale(0.4f, 0.4f, 0.4f);
            MinecraftClient.getInstance().getItemRenderer().renderItem(stack, ModelTransformationMode.FIXED,
                    lightCoordsFromFade(fade), OverlayTexture.DEFAULT_UV, ms, buffer, world, 0);
            ms.pop();
        }
    }

    /**
     * A thrown item in flight (a capsule, an aroma orb): a camera-facing item sprite, as vanilla's
     * FlyingItemEntityRenderer draws thrown items, on a straight scripted path, gone on arrival.
     * 飞行中的投掷物（胶囊、香薰）：像原版 FlyingItemEntityRenderer 那样始终朝向镜头的物品图片，沿脚本直线飞行，到达即消失。
     */
    private static final class Flight extends AnimatedSceneElementBase {
        private final ItemStack stack;
        private final Vec3d from;
        private final Vec3d to;
        private final int ticks;
        private int age;

        private Flight(ItemStack stack, Vec3d from, Vec3d to, int ticks) {
            this.stack = stack.copy();
            this.from = from;
            this.to = to;
            this.ticks = Math.max(1, ticks);
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
            if (fade <= 0.01f || time >= ticks) {
                return;
            }
            Vec3d at = from.lerp(to, time / ticks);
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
