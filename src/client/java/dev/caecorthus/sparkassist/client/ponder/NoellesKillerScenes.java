package dev.caecorthus.sparkassist.client.ponder;

import static dev.caecorthus.sparkassist.client.ponder.SparkPonderDemos.item;
import static dev.caecorthus.sparkassist.client.ponder.SparkPonderDemos.role;
import static dev.caecorthus.sparkassist.client.ponder.SparkPonderDemos.scene;
import static dev.caecorthus.sparkassist.client.ponder.WatheItemScenes.NORTH;
import static dev.caecorthus.sparkassist.client.ponder.WatheItemScenes.SCREEN_LEFT;
import static dev.caecorthus.sparkassist.client.ponder.WatheItemScenes.SCREEN_RIGHT;
import static dev.caecorthus.sparkassist.client.ponder.WatheItemScenes.WEST;
import static dev.caecorthus.sparkassist.client.ponder.WatheItemScenes.stack;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.element.AnimatedSceneElementBase;
import net.createmod.ponder.foundation.instruction.TickingInstruction;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 * Demos for NoellesRoles' killer and neutral roles, opened from the role's guide page (and, for their shop items, by
 * holding W over the item); each needs NoellesRoles. Effects the server triggers (the swap's portal particles, a body
 * eaten, the poison gas) never run in Ponder's world, so they are scripted with the game's own particles and counts,
 * and the captions say who sees what. The axe's flight, faster than the eye can follow, is slowed and captioned so.
 * NoellesRoles 杀手与中立阵营职业的演示，从该职业的指南页打开（其商店物品也可在物品上按住 W 播放）；都需要
 * NoellesRoles。由服务端触发的效果（交换的传送粒子、尸体被吃、毒气）不会在 Ponder 的世界里运行，所以按游戏自己的粒子
 * 与数量用脚本补上，并在字幕里说明谁能看到。飞斧的飞行快得看不清，做了放慢，字幕会注明。
 */
final class NoellesKillerScenes {
    private static final List<String> NOELLES = List.of("noellesroles");
    private static final Random RANDOM = Random.create();
    /** Facing the camera: towards the plate's (0, 0) corner. 面向镜头：朝底板的 (0, 0) 角。 */
    private static final float FACING_CAMERA = 135;
    /** PoisonGasCloudEntity.GAS_PARTICLE. 毒气云的粒子。 */
    private static final DustParticleEffect GAS = new DustParticleEffect(new Vector3f(0.3f, 0.8f, 0.2f), 1.5f);

    private NoellesKillerScenes() {
    }

    static void register() {
        role("sparkassist:roles/noellesroles/swapper", NOELLES,
                scene("wathe/aisle", NoellesKillerScenes::swapper));
        role("sparkassist:roles/noellesroles/vulture", NOELLES,
                scene("wathe/aisle", NoellesKillerScenes::vulture));
        role("sparkassist:roles/noellesroles/poisoner", NOELLES,
                scene("wathe/aisle", NoellesKillerScenes::poisonNeedle));
        item("noellesroles:poison_needle", NOELLES, scene("wathe/aisle", NoellesKillerScenes::poisonNeedle));
        item("noellesroles:catalyst", NOELLES, scene("wathe/aisle", NoellesKillerScenes::poisonNeedle));
        item("noellesroles:poison_gas_bomb", NOELLES, scene("wathe/cabin", NoellesKillerScenes::poisonGasBomb));
        role("sparkassist:roles/noellesroles/bandit", NOELLES,
                scene("noellesroles/axe_wall", NoellesKillerScenes::throwingAxe));
        item("noellesroles:throwing_axe", NOELLES, scene("noellesroles/axe_wall", NoellesKillerScenes::throwingAxe));
    }

    /**
     * Swapper (Noellesroles SWAP_PACKET receiver, SwapperScreenMixin, SwapperPlayerWidget): the inventory lists the
     * living players' heads; clicking a first and then a second head swaps those two at once, position, yaw and pitch
     * alike, with portal particles (entity status 46) and the enderman teleport sound at both spots. The Swapper may
     * pick themself; 60 s cooldown. Some players cannot be swapped (SparkTraits Last Stand, SparkWitch Wraiths and Rift
     * gates via SparkFactionAPI). Here the Swapper waits in front of a partner's knife and swaps themself with the
     * target.
     * 交换者：背包里列出存活玩家的头像；先后点两个头像，这两人立刻互换位置，连朝向一起互换，两处都会冒出传送粒子
     * （实体状态 46）并响起末影人传送声。可以选自己；冷却 60 秒。少数人换不了（SparkTraits 背水一战、经 SparkFactionAPI
     * 的 SparkWitch 冤魂与裂隙门）。演示中交换者站在同伴刀前，把自己和目标互换。
     */
    private static void swapper(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("role_swapper", "交换者：让两个人互换位置");
        WatheItemScenes.stage(scene);
        ItemStack head = stack("minecraft:player_head");
        ElementLink<ActorElement> partner = Actors.enter(scene, RoleColors.KILLER, Text.literal("杀手"),
                new Vec3d(5.6, 1, 1.0), SCREEN_RIGHT, Direction.DOWN);
        Actors.hold(scene, partner, stack("wathe:knife"));
        ElementLink<ActorElement> swapper = Actors.enter(scene,
                RoleColors.of("noellesroles:swapper", 0x3904AA), Text.literal("交换者"),
                new Vec3d(4.4, 1, 2.2), SCREEN_RIGHT, Direction.DOWN);
        scene.idle(10);
        ElementLink<ActorElement> target = Actors.enter(scene, RoleColors.CIVILIAN, Text.literal("平民"),
                new Vec3d(1.6, 1, 5.2), SCREEN_LEFT, Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(80)
                .text("交换者能让两名存活玩家瞬间互换位置")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        scene.overlay().showText(70)
                .text("你先站到杀手同伴身前，目标还在远处")
                .independent();
        scene.idle(80);
        scene.overlay().showText(80)
                .text("按 E 打开背包，里面列着存活玩家的头像")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        scene.overlay().showControls(new Vec3d(4.4, 3.6, 2.2), Pointing.DOWN, 40).leftClick().withItem(head);
        scene.overlay().showText(50)
                .text("先点一个人的头像（可以选你自己）……")
                .independent();
        scene.idle(55);
        scene.overlay().showControls(new Vec3d(1.6, 3.6, 5.2), Pointing.DOWN, 30).leftClick().withItem(head);
        scene.overlay().showText(70)
                .text("……再点第二个人：选中的一瞬间，两人一般就互换了位置")
                .independent()
                .attachKeyFrame();
        scene.idle(15);
        swap(scene, swapper, target);
        scene.idle(65);
        scene.overlay().showText(90)
                .text("两人连朝向一起互换；两处都会冒出传送粒子、响起传送声")
                .independent()
                .attachKeyFrame();
        scene.idle(60);
        Actors.charge(scene, partner, true);
        scene.idle(20);
        Actors.charge(scene, partner, false);
        Actors.swing(scene, partner);
        Actors.fall(scene, target);
        scene.idle(20);
        scene.overlay().showText(80)
                .colored(PonderPalette.RED)
                .text("目标被送到了杀手同伴的刀下")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        scene.overlay().showText(70)
                .text("你自己则站到了目标原来的位置，离现场远远的")
                .independent();
        scene.idle(80);
        scene.overlay().showText(80)
                .text("每次交换冷却 60 秒；被换走的两个人自己会立刻发现")
                .independent();
        scene.idle(90);
        scene.markAsFinished();
    }

    /**
     * Vulture (Noellesroles VULTURE_PACKET receiver, NoellesrolesClient body raycast and highlights): holding the
     * instinct key outlines bodies, through walls, on the Vulture's screen. The ability key eats the body under the
     * crosshair within 2 blocks: it is removed with 30 smoke and 10 soul particles everyone near sees, and the Vulture
     * gets 10 s of Speed III and outlines on the other living players for 10 s. 30 s opening cooldown by default,
     * then 5 s; a living Vulture that has eaten half the starting players' bodies (rounded down) wins at once
     * (CheckWinCondition).
     * 秃鹫：按住本能透视键时，秃鹫屏幕上的尸体会显出轮廓，隔墙也行。准星对准 2 格内的尸体按技能键即可吃掉：尸体消失，
     * 原地冒出 30 个烟雾和 10 个灵魂粒子，附近的人都看得见；秃鹫获得 10 秒速度 III，10 秒内能看到其他活人的轮廓。
     * 开局冷却默认 30 秒，之后 5 秒；活着的秃鹫吃够开局人数一半（向下取整）的尸体立即获胜。
     */
    private static void vulture(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("role_vulture", "秃鹫：吃掉尸体");
        WatheItemScenes.stage(scene);
        int color = RoleColors.of("noellesroles:vulture", 0xB56700);
        Vec3d feet = new Vec3d(2.6, 1, 3.0);
        ElementLink<ActorElement> victim = Actors.enter(scene, RoleColors.CIVILIAN, Text.literal("平民"), feet,
                SCREEN_RIGHT, Direction.DOWN);
        scene.idle(15);
        Actors.fall(scene, victim);
        scene.idle(25);
        ElementLink<ActorElement> passenger = Actors.enter(scene, RoleColors.CIVILIAN, Text.literal("平民"),
                new Vec3d(1.4, 1, 5.4), SCREEN_LEFT, Direction.DOWN);
        scene.idle(10);
        ElementLink<ActorElement> vulture = Actors.enter(scene, color, Text.literal("秃鹫"),
                new Vec3d(6.2, 1, 0.6), SCREEN_RIGHT, Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(80)
                .text("秃鹫是中立阵营：吃够尸体就能直接获胜")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        outlineBody(scene, feet, SCREEN_RIGHT, color, 90);
        scene.overlay().showText(90)
                .text("按住本能透视键能隔墙看到尸体（轮廓只在你屏幕上）")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        Actors.walk(scene, vulture, new Vec3d(-2.6, 0, 1.4), 28);
        scene.idle(30);
        Actors.turn(scene, vulture, SCREEN_RIGHT);
        scene.idle(6);
        Actors.lookPitch(scene, vulture, 45);
        scene.overlay().showText(70)
                .text("走到 2 格内，准星对准尸体，按技能键（默认 G）")
                .independent()
                .attachKeyFrame();
        scene.idle(60);
        Vec3d smoke = feet.add(0, 0.5, 0);
        spray(scene, ParticleTypes.SMOKE, smoke, 30, 0.3, 0.02);
        spray(scene, ParticleTypes.SOUL, smoke, 10, 0.2, 0.01);
        Actors.vanish(scene, victim);
        // Speed III and the outlines on the living both last 10 s from the meal.
        // 速度 III 与活人轮廓都从吃完起持续 10 秒。
        Actors.highlight(scene, passenger, color, 200);
        scene.idle(10);
        scene.overlay().showText(80)
                .text("尸体当场被吃掉，原地冒出烟雾和灵魂粒子，旁人也看得见")
                .independent()
                .attachKeyFrame();
        scene.idle(85);
        Actors.lookPitch(scene, vulture, 0);
        scene.overlay().showText(95)
                .colored(PonderPalette.GREEN)
                .text("吃完 10 秒内：速度 III，还能隔墙看到活人（只在你屏幕上）")
                .independent()
                .attachKeyFrame();
        scene.idle(10);
        Actors.walk(scene, vulture, new Vec3d(2.4, 0, 2.4), 11);
        scene.idle(95);
        scene.overlay().showText(80)
                .text("吃够开局人数一半的尸体就赢；每吃一具冷却 5 秒")
                .independent();
        scene.idle(90);
        scene.overlay().showText(80)
                .text("吃尸体等于帮杀手毁掉证据，被人撞见容易被当成帮凶")
                .independent();
        scene.idle(90);
        scene.markAsFinished();
    }

    /**
     * Poison needle and catalyst (PoisonNeedleItem, CatalystItem, PoisonerShopHandler; SparkStrength
     * PoisonerEconomyService): the Poisoner's shop has no knife or revolver. The needle, right-clicked on a player in
     * reach, poisons for 40 s (or takes 40 s off a poison already running); reusable, 60 s opening and 45 s cooldown.
     * The catalyst, used on a poisoned player, sets the poison to 5 s; single use, it always clears the needle's
     * cooldown. Poisoned players usually carry an outline through walls on the Poisoner's screen (NoellesrolesClient;
     * not on invisible players, a hidden Survival Master or SparkTraits' Covert players); each new poisoning pays 75
     * coins.
     * 毒针与催化剂：毒师的商店没有刀和左轮。毒针右键够得着的玩家即可下毒 40 秒（已中毒则缩短 40 秒）；可反复使用，
     * 开局冷却 60 秒，之后每次 45 秒。催化剂用在中毒的人身上，毒发时间设为 5 秒；一次性，总会刷新毒针冷却。
     * 中毒的人在毒师屏幕上一般隔墙带轮廓（隐身的人、隐藏的生存大师和 SparkTraits 隐蔽行动的人除外）；每让一个人新中毒
     * 得 75 金币。
     */
    private static void poisonNeedle(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("role_poisoner", "毒师：毒针加催化剂");
        WatheItemScenes.stage(scene);
        int color = RoleColors.of("noellesroles:poisoner", 0x1E5014);
        ElementLink<ActorElement> victim = Actors.enter(scene, RoleColors.CIVILIAN, Text.literal("平民"),
                new Vec3d(2.4, 1, 4.2), SCREEN_RIGHT, Direction.DOWN);
        scene.idle(5);
        ElementLink<ActorElement> poisoner = Actors.enter(scene, color, Text.literal("毒师"),
                new Vec3d(6.0, 1, 0.6), SCREEN_RIGHT, Direction.DOWN);
        Actors.hold(scene, poisoner, stack("noellesroles:poison_needle"));
        scene.idle(15);
        scene.overlay().showText(80)
                .text("毒师的商店不卖刀和枪，靠毒针、毒气和下毒杀人")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        Actors.walk(scene, poisoner, new Vec3d(-2.4, 0, 2.4), 30);
        scene.idle(32);
        scene.overlay().showControls(new Vec3d(2.4, 3.6, 4.2), Pointing.DOWN, 30).rightClick()
                .withItem(stack("noellesroles:poison_needle"));
        scene.idle(10);
        Actors.swing(scene, poisoner);
        // The outline lasts until the victim drops: dead players are spectators and lose it.
        // 轮廓一直持续到被害者倒下：死者成为旁观者，轮廓随之消失。
        Actors.highlight(scene, victim, color, 400);
        scene.overlay().showText(80)
                .text("毒针：右键身边的人注射毒素，对方 40 秒后毒发")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        scene.overlay().showText(80)
                .colored(PonderPalette.GREEN)
                .text("每让一个没中毒的人中毒，你就得到 75 金币")
                .independent();
        scene.idle(90);
        scene.overlay().showText(90)
                .text("中了毒的人你一般不用按键就能隔墙看到（轮廓只在你屏幕上）")
                .independent();
        scene.idle(100);
        Actors.hold(scene, poisoner, stack("noellesroles:catalyst"));
        scene.idle(10);
        scene.overlay().showControls(new Vec3d(2.4, 3.6, 4.2), Pointing.DOWN, 30).rightClick()
                .withItem(stack("noellesroles:catalyst"));
        scene.idle(10);
        Actors.swing(scene, poisoner);
        Actors.hold(scene, poisoner, ItemStack.EMPTY);
        scene.overlay().showText(90)
                .text("想快点：用催化剂右键中毒的人，对方 5 秒后毒发")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        Actors.fall(scene, victim);
        scene.overlay().showText(80)
                .colored(PonderPalette.RED)
                .text("没被解毒或护盾挡下，就会毒发身亡：死因“中毒”，留下尸体")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        scene.overlay().showText(80)
                .text("催化剂用一次就没了，但会立刻刷新毒针的冷却")
                .independent();
        scene.idle(90);
        scene.overlay().showText(80)
                .text("毒针能反复用：开局冷却 60 秒，之后每扎一次冷却 45 秒")
                .independent();
        scene.idle(90);
        scene.markAsFinished();
    }

    /**
     * Poison gas bomb (PoisonGasBombItem, PoisonGasBombEntity, PoisonGasCloudEntity): a right-click throws it; on
     * hitting a block or a player it releases a gas cloud that floods one more block outwards every 8 ticks (up to
     * 500 blocks within 20 of its start; solid blocks and closed doors stop it, but a cabin door's thin panel lets it
     * into the doorway, so a player pressed against the door is gassed) and lasts 30 s. Every 5 s in a row spent in it
     * poisons for 20 s, or takes 20 s off a running poison, so a second dose on a fresh 20 s poison kills at once;
     * inside it a player with a sprint limit cannot sprint. The Poisoner is immune; killers are not, though
     * SparkFactionAPI policies spare Wraiths and Rift gate occupants and a Conscience Poisoner's blue gas spares
     * civilians.
     * 毒气弹：右键扔出，碰到方块或玩家就放出毒气，每 8 tick 向外多扩散一格（起点 20 格内最多 500 格；实心方块与关着的门
     * 挡得住，但包厢门的薄门板会让毒气进到门洞里，贴着门站的人也会中毒），持续 30 秒。在毒气里每连续待满 5 秒就中毒 20 秒，
     * 已中毒则缩短 20 秒，所以刚中 20 秒毒的人再中一次会当场毒发；有疾跑上限的人身处毒气中不能疾跑。毒师免疫；杀手也会
     * 中毒，但 SparkFactionAPI 的规则放过冤魂与裂隙门里的人，善良毒师的蓝色毒气放过平民。
     */
    private static void poisonGasBomb(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("noelles_poison_gas_bomb", "毒气弹：把毒气放进人堆");
        WatheItemScenes.cabinStage(scene, util);
        int color = RoleColors.of("noellesroles:poisoner", 0x1E5014);
        Text civilian = Text.literal("平民");
        ElementLink<ActorElement> first = Actors.enter(scene, RoleColors.CIVILIAN, civilian,
                new Vec3d(2.4, 1, 0.8), WEST, Direction.DOWN);
        ElementLink<ActorElement> second = Actors.enter(scene, RoleColors.CIVILIAN, civilian,
                new Vec3d(2.4, 1, 2.1), WEST, Direction.DOWN);
        ElementLink<ActorElement> sheltered = Actors.enter(scene, RoleColors.CIVILIAN, civilian,
                new Vec3d(1.5, 1, 4.6), NORTH, Direction.DOWN);
        scene.idle(10);
        ElementLink<ActorElement> poisoner = Actors.enter(scene, color, Text.literal("毒师"),
                new Vec3d(6.3, 1, 1.5), WEST, Direction.DOWN);
        Actors.hold(scene, poisoner, stack("noellesroles:poison_gas_bomb"));
        scene.idle(20);
        scene.overlay().showText(80)
                .text("毒气弹：右键扔出，碰到方块或人就会放出毒气")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        Actors.lookPitch(scene, poisoner, 25);
        scene.overlay().showControls(new Vec3d(6.3, 3.6, 1.5), Pointing.DOWN, 30).rightClick()
                .withItem(stack("noellesroles:poison_gas_bomb"));
        scene.idle(10);
        Actors.swing(scene, poisoner);
        Actors.hold(scene, poisoner, ItemStack.EMPTY);
        // PoisonGasBombItem throws at 1.6875 blocks a tick: aimed 25 degrees down it reaches the floor in 3 ticks.
        // 毒气弹以每 tick 1.6875 格的速度扔出：向下 25 度瞄准时 3 tick 落地。
        Actors.toss(scene, stack("noellesroles:poison_gas_bomb"), new Vec3d(6.3, 2.52, 1.5),
                new Vec3d(3.3, 1.05, 1.5), 3, 0, false, ThrownElement.Flight.UPRIGHT);
        scene.idle(3);
        GasCloud gas = gas(scene, util.grid().at(3, 1, 1), corridorAir());
        Actors.lookPitch(scene, poisoner, 0);
        scene.overlay().showText(100)
                .text("毒气会一格一格往外扩散，漫进附近能去的地方")
                .independent()
                .attachKeyFrame();
        // Both passengers stand in gas from the cloud's first spread (8 ticks) and are poisoned once 100 ticks of
        // exposure add up; the second dose 100 ticks later leaves 1 tick of their 20 s poison.
        // 两名乘客从毒气第一次扩散（第 8 tick）起就身处其中，累计 100 tick 后中毒；100 tick 后的第二次中毒只给 20 秒的毒
        // 留下 1 tick。
        scene.idle(107);
        Actors.highlight(scene, first, color, 101);
        Actors.highlight(scene, second, color, 101);
        scene.overlay().showText(90)
                .colored(PonderPalette.RED)
                .text("待满 5 秒就中毒（20 秒后毒发），毒师能看到中毒者的轮廓")
                .independent()
                .attachKeyFrame();
        scene.idle(101);
        Actors.fall(scene, first);
        Actors.fall(scene, second);
        scene.overlay().showText(90)
                .colored(PonderPalette.RED)
                .text("刚中毒的人再待满 5 秒，毒发提前 20 秒：当场倒下")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showOutline(PonderPalette.WHITE, "door", util.select().fromTo(3, 1, 3, 3, 2, 3), 80);
        Actors.highlight(scene, sheltered, 0x7AE04F, 80);
        scene.overlay().showText(80)
                .colored(PonderPalette.GREEN)
                .text("毒气穿不过墙和关着的门：门后的人没事（别贴着门站）")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        scene.overlay().showText(80)
                .text("身处毒气中一般不能疾跑，想逃也跑不快")
                .independent();
        scene.idle(90);
        scene.overlay().showText(80)
                .colored(PonderPalette.GREEN)
                .text("毒师自己不会被毒气毒倒；杀手同伴待在里面一样会中毒")
                .independent();
        scene.idle(90);
        scene.overlay().showText(70)
                .text("毒气放出 30 秒后散去")
                .independent();
        scene.idle(22);
        stopGas(scene, gas);
        scene.idle(58);
        scene.markAsFinished();
    }

    /**
     * Throwing axe (ThrowingAxeItem, ThrowingAxeEntity, ThrowingAxeEntityRenderer, BanditShopHandler): the Bandit's
     * shop sells it for 200, single use. Holding right-click shows the bow pose; power peaks after 20 ticks, and under
     * a quarter of it (6 ticks or fewer) nothing is thrown. The axe flies at up to 2.4 blocks a tick, kills every
     * player whose hitbox it crosses except its thrower (unless a protection stops the kill) and keeps 90% of its
     * speed after each, then sticks in the block it hits and cannot be picked up. Throw, hit and stick play trident
     * sounds. The flight lasts about 3 game ticks here, so the demo slows it down.
     * 飞斧：强盗商店 200 金币，一次性。按住右键为拉弓姿势；20 tick 蓄满，不到四分之一（6 tick 及以内）就扔不出去。飞斧每 tick
     * 最多飞 2.4 格，杀死所有被它穿过碰撞箱的玩家（扔的人除外），每穿过一人保留 90% 的速度，最后插在撞到的方块上，
     * 捡不回来。投掷、命中、插墙都有三叉戟音效。这里的飞行在游戏中只有约 3 tick，演示做了放慢。
     */
    private static void throwingAxe(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("noelles_throwing_axe", "飞斧：一斧穿一串");
        WatheItemScenes.setStage(scene, util);
        Text civilian = Text.literal("平民");
        ElementLink<ActorElement> first = Actors.enter(scene, RoleColors.CIVILIAN, civilian,
                new Vec3d(4.99, 1, 1.91), FACING_CAMERA, Direction.DOWN);
        ElementLink<ActorElement> second = Actors.enter(scene, RoleColors.CIVILIAN, civilian,
                new Vec3d(3.64, 1, 3.26), FACING_CAMERA, Direction.DOWN);
        ElementLink<ActorElement> third = Actors.enter(scene, RoleColors.CIVILIAN, civilian,
                new Vec3d(2.30, 1, 4.60), FACING_CAMERA, Direction.DOWN);
        scene.idle(10);
        ElementLink<ActorElement> bandit = Actors.enter(scene,
                RoleColors.of("noellesroles:bandit", 0x5A6428), Text.literal("强盗"),
                new Vec3d(6.4, 1, 0.5), SCREEN_RIGHT, Direction.DOWN);
        Actors.hold(scene, bandit, stack("noellesroles:throwing_axe"));
        scene.idle(20);
        scene.overlay().showText(80)
                .text("强盗能在商店花 200 金币买【飞斧】，用一次就没了")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        scene.overlay().showText(70)
                .text("按住右键蓄力：约 1 秒蓄满，蓄得越久飞得越快越远")
                .independent()
                .attachKeyFrame();
        scene.idle(30);
        scene.overlay().showControls(new Vec3d(6.4, 3.6, 0.5), Pointing.DOWN, 40).rightClick()
                .withItem(stack("noellesroles:throwing_axe"));
        Actors.charge(scene, bandit, true);
        scene.idle(45);
        Actors.charge(scene, bandit, false);
        Actors.hold(scene, bandit, ItemStack.EMPTY);
        // From the eyes (feet + 1.62 - 0.1) along the facing, sagging under gravity until the back wall's north face.
        // 从眼睛高度（脚 + 1.62 - 0.1）沿朝向飞出，受重力略微下沉，直到后墙的北面。
        throwAxe(scene, new Vec3d(6.4, 2.52, 0.5), new Vec3d(0.9, 2.27, 6.0), 16, 3.3f);
        scene.overlay().showText(70)
                .text("松开右键扔出：飞斧穿过一个人还会接着飞（演示放慢了）")
                .independent()
                .attachKeyFrame();
        scene.idle(3);
        Actors.fall(scene, first);
        scene.idle(4);
        Actors.fall(scene, second);
        scene.idle(4);
        Actors.fall(scene, third);
        scene.idle(64);
        scene.overlay().showText(80)
                .colored(PonderPalette.RED)
                .text("被穿过的人一般当场死亡，每穿过一人只慢一点点")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.WHITE, "axe",
                new Box(0.5, 1.8, 5.3, 1.3, 2.7, 6.0), 70);
        scene.overlay().showText(70)
                .text("撞上方块就插在上面，捡不回来")
                .independent()
                .attachKeyFrame();
        scene.idle(80);
        scene.overlay().showText(70)
                .text("蓄力太短（0.3 秒以内）扔不出去")
                .independent();
        scene.idle(80);
        scene.overlay().showText(80)
                .text("扔出、命中和插墙都有声响；飞斧不分敌我，杀手同伴也会被杀")
                .independent();
        scene.idle(90);
        scene.markAsFinished();
    }

    /**
     * Swap two actors at once, position and yaw alike, with the portal particles entity status 46 plays at each spot.
     * They are hidden for the one tick in which they move, so neither streaks across the stage.
     * 立即互换两名演员的位置与朝向，并在两处播放实体状态 46 的传送粒子。移动的那一 tick 先隐藏，避免拖影划过舞台。
     */
    private static void swap(SceneBuilder scene, ElementLink<ActorElement> first, ElementLink<ActorElement> second) {
        scene.addInstruction(ponder -> {
            ActorElement a = ponder.resolve(first);
            ActorElement b = ponder.resolve(second);
            if (a == null || b == null) {
                return;
            }
            portalParticles(ponder.getWorld(), a.position());
            portalParticles(ponder.getWorld(), b.position());
            Vec3d at = a.position();
            float yaw = a.yaw();
            a.moveTo(b.position());
            a.setYaw(b.yaw());
            b.moveTo(at);
            b.setYaw(yaw);
            a.setVisible(false);
            b.setVisible(false);
        });
        scene.idle(1);
        scene.addInstruction(ponder -> {
            ActorElement a = ponder.resolve(first);
            ActorElement b = ponder.resolve(second);
            if (a != null && b != null) {
                a.setVisible(true);
                b.setVisible(true);
            }
        });
    }

    /** LivingEntity.handleStatus(46): 128 portal particles around a player-sized box. 实体状态 46：玩家大小范围内 128 个传送粒子。 */
    private static void portalParticles(PonderLevel world, Vec3d feet) {
        for (int i = 0; i < 128; i++) {
            world.addParticle(ParticleTypes.PORTAL, feet.x + (RANDOM.nextDouble() - 0.5) * 1.2,
                    feet.y + RANDOM.nextDouble() * 1.8, feet.z + (RANDOM.nextDouble() - 0.5) * 1.2,
                    (RANDOM.nextFloat() - 0.5) * 0.2, (RANDOM.nextFloat() - 0.5) * 0.2,
                    (RANDOM.nextFloat() - 0.5) * 0.2);
        }
    }

    /**
     * Particles as ServerWorld.spawnParticles sends them: {@code count} at {@code at} plus a gaussian {@code spread},
     * each moving at a gaussian {@code speed}.
     * 与 ServerWorld.spawnParticles 发出的一致：在 at 处按高斯分布 spread 散开 count 个粒子，速度按高斯分布 speed。
     */
    private static void spray(SceneBuilder scene, ParticleEffect particle, Vec3d at, int count, double spread,
                              double speed) {
        scene.addInstruction(ponder -> {
            for (int i = 0; i < count; i++) {
                ponder.getWorld().addParticle(particle, at.x + RANDOM.nextGaussian() * spread,
                        at.y + RANDOM.nextGaussian() * spread, at.z + RANDOM.nextGaussian() * spread,
                        RANDOM.nextGaussian() * speed, RANDOM.nextGaussian() * speed, RANDOM.nextGaussian() * speed);
            }
        });
    }

    /**
     * A box in {@code color} around a body lying from {@code feet} towards {@code yaw}, for {@code ticks}: stands in
     * for the glowing outline an instinct shows (Actors.highlight boxes a body around its feet only).
     * 在 ticks 内用 color 色方框框住从 feet 朝 yaw 方向躺倒的尸体：代替本能显示的发光轮廓（Actors.highlight 只框住尸体的脚边）。
     */
    private static void outlineBody(SceneBuilder scene, Vec3d feet, float yaw, int color, int ticks) {
        float radians = yaw * MathHelper.RADIANS_PER_DEGREE;
        Vec3d head = feet.add(-MathHelper.sin(radians) * 1.8, 0, MathHelper.cos(radians) * 1.8);
        Box box = new Box(Math.min(feet.x, head.x) - 0.3, feet.y, Math.min(feet.z, head.z) - 0.3,
                Math.max(feet.x, head.x) + 0.3, feet.y + 0.4, Math.max(feet.z, head.z) + 0.3);
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
     * The cabin stage's corridor air plus the doorway the door panel sits in. The stage is a cut-away: a train corridor
     * has a ceiling at y = 3, left out so the camera can see in, so the gas stays below it.
     * 包厢舞台走廊里的空气，加上门板所在的门洞。舞台是剖面图：列车走廊在 y = 3 处有天花板，为了让镜头看进去而省略，
     * 所以毒气留在它下面。
     */
    private static Set<BlockPos> corridorAir() {
        Set<BlockPos> air = new HashSet<>();
        for (int x = 0; x < 7; x++) {
            for (int y = 1; y <= 2; y++) {
                for (int z = 0; z < 3; z++) {
                    air.add(new BlockPos(x, y, z));
                }
            }
        }
        air.add(new BlockPos(3, 1, 3));
        air.add(new BlockPos(3, 2, 3));
        return air;
    }

    /** Start a gas cloud at {@code start}, spreading through {@code air}. 在 start 处放出毒气，在 air 中扩散。 */
    private static GasCloud gas(SceneBuilder scene, BlockPos start, Set<BlockPos> air) {
        GasCloud cloud = new GasCloud(start, air);
        scene.addInstruction(cloud);
        return cloud;
    }

    private static void stopGas(SceneBuilder scene, GasCloud cloud) {
        scene.addInstruction(ponder -> cloud.stopped = true);
    }

    /**
     * PoisonGasCloudEntity's flood fill and particles: the start block at once, then one more block outwards every
     * 8 ticks; each tick 4-6 green dust particles at random gassed blocks, scattered by a gaussian 0.3.
     * 复刻毒气云的扩散与粒子：先占起点方块，之后每 8 tick 向外多扩散一格；每 tick 在随机的毒气方块处放出 4～6 个绿色
     * 粒子，按高斯分布 0.3 散开。
     */
    private static final class GasCloud extends TickingInstruction {
        private static final int LONGEST = 20 * 60 * 10;
        private static final int SPREAD_INTERVAL = 8;
        private final Map<BlockPos, Integer> steps = new HashMap<>();
        private int age;
        private boolean stopped;

        private GasCloud(BlockPos start, Set<BlockPos> air) {
            super(false, LONGEST);
            Deque<BlockPos> open = new ArrayDeque<>();
            steps.put(start, 0);
            open.add(start);
            while (!open.isEmpty()) {
                BlockPos pos = open.poll();
                for (Direction direction : Direction.values()) {
                    BlockPos next = pos.offset(direction);
                    if (air.contains(next) && !steps.containsKey(next)) {
                        steps.put(next, steps.get(pos) + 1);
                        open.add(next);
                    }
                }
            }
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
            if (stopped) {
                return;
            }
            List<BlockPos> gassed = new ArrayList<>();
            steps.forEach((pos, step) -> {
                if (step * SPREAD_INTERVAL <= age) {
                    gassed.add(pos);
                }
            });
            int count = 4 + RANDOM.nextInt(3);
            for (int i = 0; i < count && !gassed.isEmpty(); i++) {
                BlockPos pos = gassed.get(RANDOM.nextInt(gassed.size()));
                scene.getWorld().addParticle(GAS, pos.getX() + 0.5 + RANDOM.nextGaussian() * 0.3,
                        pos.getY() + 0.5 + RANDOM.nextGaussian() * 0.3, pos.getZ() + 0.5 + RANDOM.nextGaussian() * 0.3,
                        0, 0, 0);
            }
        }

        @Override
        public boolean isComplete() {
            return stopped || super.isComplete();
        }
    }

    /** Throw the axe from {@code from} into the north face at {@code to}. 把飞斧从 from 扔进 to 处的北面。 */
    private static void throwAxe(SceneBuilder scene, Vec3d from, Vec3d to, int ticks, float gameTicks) {
        FlyingAxe axe = new FlyingAxe(from, to, ticks, gameTicks);
        scene.addInstruction(ponder -> {
            axe.reset(ponder);
            axe.setVisible(true);
            axe.setFade(1);
            ponder.addElement(axe);
        });
    }

    /**
     * The throwing axe as ThrowingAxeEntityRenderer draws it: the item's ground model at 1.6x and full brightness;
     * in flight it turns 8 degrees per game tick about y and 0.7 times that about z; stuck in a north face it is
     * turned round, set 0.35 out from the face and tipped 50 degrees. {@code ticks} demo ticks stand for
     * {@code gameTicks} game ticks of flight, and the spin is slowed with them.
     * 按 ThrowingAxeEntityRenderer 的方式绘制飞斧：物品掉落物模型放大 1.6 倍、满亮度；飞行中每游戏 tick 绕 y 轴转 8 度，
     * 绕 z 轴转其 0.7 倍；插在北面时转身、离墙面 0.35 并倾斜 50 度。演示用 ticks 个 tick 表示游戏中 gameTicks 个 tick
     * 的飞行，旋转也同步放慢。
     */
    private static final class FlyingAxe extends AnimatedSceneElementBase {
        private static final float SPIN_PER_GAME_TICK = 8;
        private static final float SCALE = 1.6f;
        private final ItemStack stack = stack("noellesroles:throwing_axe");
        private final Vec3d from;
        private final Vec3d to;
        private final int ticks;
        private final float gameTicks;
        private int age;

        private FlyingAxe(Vec3d from, Vec3d to, int ticks, float gameTicks) {
            this.from = from;
            this.to = to;
            this.ticks = ticks;
            this.gameTicks = gameTicks;
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
            if (fade <= 0.01f) {
                return;
            }
            float time = Math.min(age + pt, ticks);
            Vec3d at = from.lerp(to, time / ticks);
            MatrixStack ms = graphics.getMatrices();
            ms.push();
            ms.translate(at.x, at.y, at.z);
            if (time < ticks) {
                float spin = time / ticks * gameTicks * SPIN_PER_GAME_TICK;
                ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(spin));
                ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(spin * 0.7f));
            } else {
                ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180));
                ms.translate(0, 0, 0.35);
                ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-50));
            }
            ms.scale(SCALE, SCALE, SCALE);
            MinecraftClient.getInstance().getItemRenderer().renderItem(stack, ModelTransformationMode.GROUND,
                    lightCoordsFromFade(fade), OverlayTexture.DEFAULT_UV, ms, buffer, world, 0);
            ms.pop();
        }
    }
}
