package dev.caecorthus.sparkassist.client.ponder;

import static dev.caecorthus.sparkassist.client.ponder.SparkPonderDemos.item;
import static dev.caecorthus.sparkassist.client.ponder.SparkPonderDemos.role;
import static dev.caecorthus.sparkassist.client.ponder.SparkPonderDemos.scene;
import static dev.caecorthus.sparkassist.client.ponder.WatheItemScenes.EAST;
import static dev.caecorthus.sparkassist.client.ponder.WatheItemScenes.NORTH;
import static dev.caecorthus.sparkassist.client.ponder.WatheItemScenes.SCREEN_LEFT;
import static dev.caecorthus.sparkassist.client.ponder.WatheItemScenes.SCREEN_RIGHT;
import static dev.caecorthus.sparkassist.client.ponder.WatheItemScenes.WEST;
import static dev.caecorthus.sparkassist.client.ponder.WatheItemScenes.stack;

import dev.doctor4t.wathe.index.WatheParticles;
import java.lang.reflect.Field;
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
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.element.AnimatedSceneElementBase;
import net.createmod.ponder.foundation.element.ElementLinkImpl;
import net.createmod.ponder.foundation.instruction.TickingInstruction;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.model.SkullEntityModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
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
    /** Scenes that also use SparkStrength's items or entities. 还用到 SparkStrength 物品或实体的场景。 */
    private static final List<String> NOELLES_STRENGTH = List.of("noellesroles", "sparkstrength");
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
        role("sparkassist:roles/noellesroles/bomber", NOELLES,
                scene("wathe/aisle", NoellesKillerScenes::timedBomb));
        item("noellesroles:timed_bomb", NOELLES, scene("wathe/aisle", NoellesKillerScenes::timedBomb));
        item("sparkstrength:grenade_drone", NOELLES_STRENGTH,
                scene("noellesroles/wide_aisle", NoellesKillerScenes::grenadeDrone));
        role("sparkassist:roles/noellesroles/jester", NOELLES_STRENGTH,
                scene("wathe/aisle", NoellesKillerScenes::jesterFakeDeath),
                scene("wathe/aisle", NoellesKillerScenes::jesterMoment));
        role("sparkassist:roles/noellesroles/taotie", NOELLES_STRENGTH,
                scene("wathe/aisle", NoellesKillerScenes::taotie));
        role("sparkassist:roles/noellesroles/morphling", NOELLES,
                scene("wathe/aisle", NoellesKillerScenes::morph),
                scene("wathe/aisle", NoellesKillerScenes::corpseMode));
        role("sparkassist:roles/noellesroles/phantom", NOELLES,
                scene("wathe/aisle", NoellesKillerScenes::phantom));
        role("sparkassist:roles/noellesroles/scavenger", NOELLES,
                scene("wathe/aisle", NoellesKillerScenes::scavenger));
        role("sparkassist:roles/noellesroles/corrupt_cop", NOELLES,
                scene("wathe/aisle", NoellesKillerScenes::corruptCop));
        role("sparkassist:roles/noellesroles/shadow_jester", NOELLES_STRENGTH,
                scene("wathe/aisle", NoellesKillerScenes::shadowJester));
        role("sparkassist:roles/noellesroles/pathogen", NOELLES,
                scene("wathe/aisle", NoellesKillerScenes::pathogen));
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
        scene.overlay().showText(45)
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
        scene.overlay().showText(65)
                .text("走到 2 格内，准星对准尸体，按技能键（默认 G）")
                .independent()
                .attachKeyFrame();
        scene.idle(65);
        Vec3d smoke = feet.add(0, 0.5, 0);
        spray(scene, ParticleTypes.SMOKE, smoke, 30, 0.3, 0.02);
        spray(scene, ParticleTypes.SOUL, smoke, 10, 0.2, 0.01);
        Actors.vanish(scene, victim);
        // Speed III and the outlines on the living both last 10 s from the meal.
        // 速度 III 与活人轮廓都从吃完起持续 10 秒。
        Actors.highlight(scene, passenger, color, 200);
        scene.idle(10);
        scene.overlay().showText(75)
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
        scene.overlay().showText(97)
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
        scene.overlay().showText(65)
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
        scene.overlay().showText(65)
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
     * Timed bomb (TimedBombItem, BomberPlayerComponent, BomberShopHandler, HiddenEquipmentHelper; NoellesrolesClient
     * instinct highlight): the Bomber's shop sells it for 100; it cannot be planted in the first 45 s. Right-clicking
     * a living player plants it on them and uses it up; the Bomber sees bomb carriers outlined through walls. For
     * 10 s nothing happens; then the bomb item lands in a free slot of the carrier's, it beeps every 6 ticks for 15 s
     * (everyone near hears it) and the carrier sees a countdown. A beeping carrier holding it may pass it on by
     * right-clicking someone (the receiver cannot pass for 3 s, a little less under cooldown-cutting effects; nobody
     * can take a second bomb). Planting and passing succeed on the server only, so no arm swing shows. At zero it
     * blows up with a big explosion, smoke and bomb fragments and usually kills only its carrier (a swallowed carrier's
     * Taotie instead; a SparkTraits Conscience Bomber's planted or passed bomb does not kill). The Bomber's held bomb
     * is hidden from other living players while he carries none himself; a carrier's is not.
     * 定时炸弹：炸弹客商店 100 金币，开局 45 秒内放不了。右键一名活人即把炸弹放到他身上，炸弹随即用掉；炸弹客能隔墙
     * 看到带炸弹的人的轮廓。前 10 秒毫无动静；之后炸弹进到携带者背包的空槽位，每 6 tick 滴一声，持续 15 秒（附近的人都听得到），
     * 携带者还能看到倒计时。滴滴响的携带者拿着炸弹右键别人即可传过去（接到的人 3 秒内传不出去，有缩短冷却的效果时略短；已带
     * 炸弹的人接不了）。放置与传递只在服务端成功，所以看不到挥手。归零时发生大爆炸，冒烟并飞出炸弹碎片，一般只炸死携带者
     * （携带者被饕餮吞下时炸死饕餮；SparkTraits 善良炸弹客直接放出或传出的炸弹不致命）。炸弹客自己没带炸弹时，手里的炸弹对
     * 其他活人隐藏；携带者手里的不隐藏。
     */
    private static void timedBomb(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("role_bomber", "炸弹客：定时炸弹");
        WatheItemScenes.stage(scene);
        int color = RoleColors.of("noellesroles:bomber", 0x323232);
        ItemStack bomb = stack("noellesroles:timed_bomb");
        Text civilian = Text.literal("平民");
        ElementLink<ActorElement> first = Actors.enter(scene, RoleColors.CIVILIAN, civilian,
                new Vec3d(3.0, 1, 3.4), SCREEN_RIGHT, Direction.DOWN);
        ElementLink<ActorElement> second = Actors.enter(scene, RoleColors.CIVILIAN, civilian,
                new Vec3d(1.0, 1, 5.6), SCREEN_LEFT, Direction.DOWN);
        scene.idle(10);
        ElementLink<ActorElement> bomber = Actors.enter(scene, color, Text.literal("炸弹客"),
                new Vec3d(6.2, 1, 0.6), SCREEN_RIGHT, Direction.DOWN);
        Actors.hold(scene, bomber, bomb);
        scene.idle(15);
        scene.overlay().showText(80)
                .text("炸弹客：把【定时炸弹】悄悄放到别人身上")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        scene.overlay().showText(55)
                .text("商店一般 100 金币一颗；拿在手里时别的活人一般看不见")
                .independent();
        scene.idle(20);
        Actors.walk(scene, bomber, new Vec3d(-2.3, 0, 1.9), 30);
        scene.idle(32);
        Actors.turn(scene, bomber, SCREEN_RIGHT);
        scene.idle(6);
        scene.overlay().showControls(new Vec3d(3.0, 3.6, 3.4), Pointing.DOWN, 30).rightClick().withItem(bomb);
        scene.idle(10);
        // Planting and passing succeed on the server only, so no arm swing shows. 放置与传递只在服务端成功，所以不挥手。
        Actors.hold(scene, bomber, ItemStack.EMPTY);
        // The outline lasts until the bomb is passed on. 轮廓持续到炸弹被传走。
        Actors.highlight(scene, first, color, 296);
        scene.overlay().showText(90)
                .text("右键身边的人：炸弹悄悄放到他身上，你手里这颗用掉")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showText(80)
                .text("带着炸弹的人你一般能隔墙看到（只在你屏幕上）")
                .independent();
        Actors.walk(scene, bomber, new Vec3d(2.6, 0, -1.9), 30);
        scene.idle(32);
        Actors.turn(scene, bomber, SCREEN_RIGHT);
        scene.idle(68);
        // 10 s after the plant, in real time. 放置 10 秒后（真实时间）。
        scene.overlay().showText(86)
                .text("10 秒后开始滴滴作响，炸弹出现在他背包里：15 秒后爆炸")
                .independent()
                .attachKeyFrame();
        scene.idle(50);
        // The bomb lands in a free slot; he picks it before passing it. 炸弹进到空槽位；他先选中再传。
        Actors.hold(scene, first, bomb);
        scene.idle(10);
        Actors.walk(scene, first, new Vec3d(-1.2, 0, 1.2), 24);
        scene.idle(26);
        scene.overlay().showControls(new Vec3d(1.0, 3.6, 5.6), Pointing.DOWN, 30).rightClick().withItem(bomb);
        scene.idle(10);
        Actors.hold(scene, first, ItemStack.EMPTY);
        Actors.highlight(scene, second, color, 204);
        scene.overlay().showText(90)
                .text("滴滴声附近都听得到；持有者右键别人就能把炸弹传过去")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showText(80)
                .text("刚接到的人一般 3 秒内传不出去；带着炸弹的人接不了")
                .independent();
        Actors.walk(scene, first, new Vec3d(2.0, 0, -2.0), 30);
        scene.idle(30);
        Actors.walk(scene, second, new Vec3d(0.6, 0, -0.6), 12);
        scene.idle(74);
        Vec3d blast = new Vec3d(1.6, 1.5, 5.0);
        // BomberPlayerComponent.explode: one big explosion, 100 smoke and 100 bomb fragments at the carrier.
        // 与 explode 一致：在携带者处 1 个大爆炸、100 个烟雾和 100 个炸弹碎片粒子。
        Effects.burst(scene, WatheParticles.BIG_EXPLOSION, blast, 1, 0);
        spray(scene, ParticleTypes.SMOKE, blast, 100, 0, 0.2);
        spray(scene, new ItemStackParticleEffect(ParticleTypes.ITEM, bomb), blast, 100, 0, 1.0);
        Actors.fall(scene, second);
        scene.overlay().showText(90)
                .colored(PonderPalette.RED)
                .text("时间到就爆炸：一般只炸死当时带着炸弹的人")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showText(90)
                .text("炸死谁都算你的，传回你身上也会炸死你；开局 45 秒内放不了")
                .independent();
        scene.idle(100);
        scene.markAsFinished();
    }

    /**
     * Grenade drone (SparkStrength DroneItem, DroneEntity, DronePilotService, DroneCombatService, DroneRules,
     * M67GrenadeEntity, M67Client, HiddenEquipmentHelperMixin): the Bomber starts with one; an M67 is bound onto it in
     * the inventory. Both placing and piloting wait out the 90 s opening lock. Right-clicking the floor sets it down;
     * with the tablet in the main hand the Bomber connects from the tablet's drone section and flies it (0.5 blocks a
     * tick sideways, 0.4 up) from its own view while the body stands still. Held drones and tablets are hidden from
     * other living players; the drone itself is seen by all. A left click drops the M67 (straight down while
     * hovering): 5 s later it blows up and usually kills within 5 blocks by path (walls and closed doors shield),
     * breaking drones caught in it too; meanwhile each viewer sees the grenade outlined red within 5 blocks of it and
     * yellow within 7. A right click disconnects and leaves the drone hovering; the owner right-clicks it to put it
     * back in a free hotbar slot. Flying drains 2% a second, hovering 1%.
     * 投弹无人机：炸弹客开局自带一架，在背包里把 M67 挂到它上面。放置和驾驶都要等开局 90 秒。右键地面放下；主手拿平板在
     * “无人机”分区连接，即以无人机视角飞行（横向每 tick 0.5 格、上升 0.4 格），本体站着不动。手里的无人机和平板对其他活人
     * 隐藏；无人机本身人人可见。左键投下 M67（悬停时竖直落下）：5 秒后爆炸，沿路径一般炸死 5 格内的人（墙与关着的门能挡），
     * 范围内的无人机也会被炸坏；其间每名观察者离手雷 5 格内看到红色描边，7 格内黄色。右键断开连接，无人机原地悬停；主人右键
     * 它即可收回到快捷栏空位。飞行每秒耗电 2%，悬停 1%。
     */
    private static void grenadeDrone(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("sparkstrength_grenade_drone", "投弹无人机：从空中投弹");
        wideStage(scene);
        int color = RoleColors.of("noellesroles:bomber", 0x323232);
        Text civilian = Text.literal("平民");
        ElementLink<ActorElement> stays = Actors.enter(scene, RoleColors.CIVILIAN, civilian,
                new Vec3d(2.6, 1, 7.2), SCREEN_RIGHT, Direction.DOWN);
        ElementLink<ActorElement> runs = Actors.enter(scene, RoleColors.CIVILIAN, civilian,
                new Vec3d(4.2, 1, 7.0), WEST, Direction.DOWN);
        ElementLink<ActorElement> flees = Actors.enter(scene, RoleColors.CIVILIAN, civilian,
                new Vec3d(1.6, 1, 6.4), SCREEN_LEFT, Direction.DOWN);
        scene.idle(10);
        ElementLink<ActorElement> bomber = Actors.enter(scene, color, Text.literal("炸弹客"),
                new Vec3d(8.0, 1, 1.2), SCREEN_RIGHT, Direction.DOWN);
        Actors.hold(scene, bomber, stack("sparkstrength:grenade_drone"));
        scene.idle(15);
        scene.overlay().showText(90)
                .text("炸弹客开局自带【投弹无人机】，开局 90 秒后才能用")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        Actors.lookPitch(scene, bomber, 55);
        scene.overlay().showControls(new Vec3d(7.3, 1.3, 1.9), Pointing.DOWN, 30).rightClick()
                .withItem(stack("sparkstrength:grenade_drone"));
        scene.idle(10);
        Actors.swing(scene, bomber);
        Actors.hold(scene, bomber, ItemStack.EMPTY);
        ElementLink<EntityElement> drone = placeDrone(scene, new Vec3d(7.3, 1, 1.9), SCREEN_RIGHT);
        scene.idle(15);
        Actors.lookPitch(scene, bomber, 0);
        scene.overlay().showText(80)
                .text("背包里给它挂一颗 M67，右键地面放下（手里的其他活人看不见）")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        Actors.hold(scene, bomber, stack("sparkstrength:tablet"));
        scene.overlay().showText(90)
                .text("主手拿【平板电脑】，在“无人机”分区里连接")
                .independent()
                .attachKeyFrame();
        scene.idle(30);
        flyDrone(scene, drone, new Vec3d(0, 1.6, 0), 4);
        scene.idle(70);
        scene.overlay().showText(90)
                .text("画面切到无人机；本体站着不动，手里的平板其他活人看不见")
                .independent()
                .attachKeyFrame();
        flyDrone(scene, drone, new Vec3d(-4.3, 0, 4.3), 12);
        scene.idle(100);
        Vec3d drop = new Vec3d(3.0, 1.0, 6.2);
        scene.overlay().showControls(new Vec3d(3.0, 3.3, 6.2), Pointing.DOWN, 20).leftClick();
        scene.idle(10);
        // DroneCombatService.fire: the armed M67 leaves from under the hull and falls straight down.
        // fire：拉了环的 M67 从机腹下方离开，竖直落下。
        scene.world().modifyEntity(drone, entity -> droneData(entity, "PAYLOAD", false));
        ElementLink<ThrownElement> grenade = Actors.toss(scene, armedM67(), new Vec3d(3.0, 2.33, 6.2), drop, 7, 0,
                true, ThrownElement.Flight.UPRIGHT);
        scene.overlay().showText(40)
                .text("左键投下 M67：5 秒后爆炸")
                .independent()
                .attachKeyFrame();
        scene.idle(7);
        // The warning outline lasts the whole fuse. 警示描边持续整个引信时间。
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, grenade,
                new Box(2.8, 1.0, 6.0, 3.2, 1.3, 6.4), 93);
        scene.idle(5);
        flyDrone(scene, drone, new Vec3d(4.0, 0, -4.0), 12);
        scene.idle(38);
        scene.overlay().showText(80)
                .text("附近的人看得到它的描边：5 格内红色，7 格内黄色")
                .independent()
                .attachKeyFrame();
        Actors.walk(scene, runs, new Vec3d(4.4, 0, 1.6), 24);
        Actors.walk(scene, flees, new Vec3d(4.6, 0, -5.8), 28);
        scene.idle(50);
        // The M67 went off 100 ticks after the drop. M67 在投下 100 tick 后爆炸。
        Actors.remove(scene, grenade);
        Vec3d burst = drop.add(0, 0.1, 0);
        Effects.burst(scene, WatheParticles.BIG_EXPLOSION, burst, 1, 0);
        spray(scene, ParticleTypes.SMOKE, burst, 100, 0, 0.2);
        spray(scene, new ItemStackParticleEffect(ParticleTypes.ITEM, armedM67()), burst, 100, 0, 1.0);
        Actors.fall(scene, stays);
        blastRing(scene, drop, 5, 9, 150);
        scene.idle(40);
        scene.overlay().showText(100)
                .colored(PonderPalette.RED)
                .text("红圈内（5 格）的人一般被炸死；无人机离太近也会被炸坏")
                .independent()
                .attachKeyFrame();
        scene.idle(110);
        scene.overlay().showControls(new Vec3d(8.0, 3.4, 1.2), Pointing.DOWN, 25).rightClick();
        scene.overlay().showText(90)
                .text("右键断开连接，无人机原地悬停；右键它就能收回")
                .independent()
                .attachKeyFrame();
        scene.idle(45);
        scene.overlay().showControls(new Vec3d(7.0, 3.2, 2.2), Pointing.DOWN, 25).rightClick();
        scene.idle(10);
        Actors.swing(scene, bomber);
        // Back into a free hotbar slot; the tablet stays in hand. 回到快捷栏空位；平板仍在手上。
        scene.world().modifyEntity(drone, Entity::discard);
        scene.idle(45);
        scene.overlay().showText(80)
                .text("飞行每秒耗电 2%，悬停 1%；收回后会慢慢充电")
                .independent();
        scene.idle(90);
        scene.markAsFinished();
    }

    /**
     * Jester, fake death (NoellesRoles Jester trigger, JesterPlayerComponent; SparkStrength JesterMomentService,
     * JesterMomentRules, JesterMomentClientStasisMixin): shot by an innocent role's gun (not the Demon Hunter's), the
     * Jester does not die. A body falls where it stood (spawned there, so it lies a block further back than a kill's)
     * and everyone else alive is frozen for 5 s: no moving, turning or using anything, with nothing to see. The Jester
     * then revives at its spot: every living player not swallowed by a Taotie, the Jester included, is moved to
     * another's spot and facing (no one keeps their own), and in the same tick a laugh plays to every player. The
     * others are freed and told to run; the Jester is held for 3 s, giving off glow particles. From then on, to every other living player, everyone wears the Jester's
     * psycho look and holds a bat, and names read as scrambled text.
     * 小丑假死：被好人阵营的枪（猎魔人的除外）打中时，小丑不会死。原地倒下一具尸体（尸体就生成在他站的地方，所以比击杀
     * 留下的尸体靠后一格），其余活人被定住 5 秒：不能动、不能转头、不能用任何东西，外表看不出来。随后小丑在原地复活，
     * 没被饕餮吞下的活人（含小丑）都被换到别人的位置与朝向（没人留在原位），同一 tick 里所有玩家都听到笑声。其他人解除定身并
     * 收到逃跑提示；小丑被定住 3 秒，身上冒出发光粒子。此后在其他活人眼里，人人都是小丑的疯魔模样、拿着球棒，名字显示为乱码。
     */
    private static void jesterFakeDeath(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("role_jester", "小丑：骗好人开枪");
        WatheItemScenes.stage(scene);
        int color = RoleColors.of("noellesroles:jester", 0xF8C8DC);
        Text civilian = Text.literal("平民");
        ElementLink<ActorElement> jester = Actors.enter(scene, color, Text.literal("小丑"),
                new Vec3d(2.6, 1, 4.4), SCREEN_LEFT, Direction.DOWN);
        ElementLink<ActorElement> first = Actors.enter(scene, RoleColors.CIVILIAN, civilian,
                new Vec3d(0.8, 1, 2.6), EAST, Direction.DOWN);
        ElementLink<ActorElement> second = Actors.enter(scene, RoleColors.CIVILIAN, civilian,
                new Vec3d(5.4, 1, 5.6), NORTH, Direction.DOWN);
        scene.idle(10);
        ElementLink<ActorElement> vigilante = Actors.enter(scene, RoleColors.VIGILANTE, Text.literal("义警"),
                new Vec3d(5.8, 1, 1.2), SCREEN_RIGHT, Direction.DOWN);
        Actors.hold(scene, vigilante, stack("wathe:revolver"));
        // The Jester after its revival, waiting unseen on the spot the shuffle sends it to.
        // 复活后的小丑，先隐身等在打乱后要去的位置。
        ElementLink<ActorElement> revived = Actors.enter(scene, color, scrambled(), new Vec3d(5.8, 1, 1.2),
                SCREEN_RIGHT, Direction.DOWN);
        Actors.vanish(scene, revived);
        scene.idle(15);
        scene.overlay().showText(80)
                .text("小丑是中立阵营：想办法让好人开枪打你")
                .independent()
                .attachKeyFrame();
        scene.idle(40);
        Actors.walk(scene, jester, new Vec3d(0.6, 0, -0.6), 14);
        scene.idle(50);
        WatheItemScenes.shoot(scene, vigilante, new Vec3d(5.2, 2.05, 1.8), new Vec3d(3.2, 2.2, 3.8));
        fakeBody(scene, jester);
        scene.idle(10);
        scene.overlay().showText(78)
                .colored(PonderPalette.RED)
                .text("被好人开枪打中时不会死，只留下假尸体；其他活人被定住 5 秒")
                .independent()
                .attachKeyFrame();
        scene.idle(88);
        Actors.vanish(scene, jester);
        // A single cycle, as JesterMomentRules.shuffleSpots draws it: each takes the next one's spot and facing.
        // 与 shuffleSpots 一样的单一循环：每人拿到下一个人的位置与朝向。
        relocate(scene, List.of(vigilante, first, second), List.of(new Vec3d(0.8, 1, 2.6), new Vec3d(5.4, 1, 5.6),
                new Vec3d(3.2, 1, 3.8)), new float[] {EAST, NORTH, SCREEN_LEFT});
        reveal(scene, revived);
        for (ElementLink<ActorElement> other : List.of(vigilante, first, second)) {
            Actors.retint(scene, other, 0xFFFFFF, scrambled());
            Actors.psycho(scene, other, true);
            Actors.hold(scene, other, stack("wathe:bat"));
        }
        Actors.psycho(scene, revived, true);
        glow(scene, new Vec3d(5.8, 2, 1.2), 200);
        // The others are free at once and told to run. 其他人立刻解除定身，并被提示快跑。
        Actors.leave(scene, vigilante, Direction.WEST);
        Actors.walk(scene, first, new Vec3d(-1.6, 0, 0.6), 20);
        Actors.walk(scene, second, new Vec3d(-1.2, 0, 1.6), 20);
        scene.overlay().showText(90)
                .text("5 秒后你复活：活人（被吞的除外）被随机换到别人的位置")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showText(90)
                .text("你在新位置定住 3 秒、冒着光（演示放慢）；全车都听到笑声")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        Actors.hold(scene, revived, stack("wathe:bat"));
        scene.overlay().showText(100)
                .text("此后在其他活人眼里，人人都是你的疯魔模样、拿着球棒")
                .independent()
                .attachKeyFrame();
        scene.idle(110);
        Actors.highlight(scene, revived, color, 190);
        scene.overlay().showText(90)
                .text("名字也都成了乱码；方框标出的才是真小丑（仅为示意）")
                .independent();
        scene.idle(100);
        scene.overlay().showText(80)
                .colored(PonderPalette.RED)
                .text("注意：只有好人的枪才触发假死，被杀手或猎魔人打中就是真死")
                .independent();
        scene.idle(90);
        scene.markAsFinished();
    }

    /**
     * Jester moment (JesterPlayerComponent.startJesterPsychoMode, registerKill; Wathe PlayerPsychoComponent; SparkStrength
     * JesterMomentRules), seen by a living non-Jester: everyone wears the Jester's psycho look with a bat, so the box
     * marking the real Jester is only a guide. The Jester is in psycho mode for 60 s, plus 30 s and its shield topped
     * up per kill; a fully charged bat swing usually kills; sprint never runs out and any door that is not jammed opens.
     * Killing everyone else alive wins; running out of time kills the Jester. Each kill greys the Jester's own screen
     * by 10%, up to 50%, and it can neither speak nor hear voice chat.
     * 小丑时刻（从一名非小丑活人的视角看）：人人都是小丑的疯魔模样、拿着球棒，所以标出真小丑的方框只是示意。小丑进入
     * 疯魔 60 秒，每杀一人加 30 秒并补回护盾；蓄满力的一棒一般能打死人；体力无限，没被堵住的门都能打开。杀光其他活人即获胜；
     * 时间耗尽小丑会死。每杀一人小丑自己的画面变灰 10%（最多 50%），也既不能说话也听不到语音。
     */
    private static void jesterMoment(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("role_jester_moment", "小丑时刻：疯魔追杀");
        WatheItemScenes.stage(scene);
        int color = RoleColors.of("noellesroles:jester", 0xF8C8DC);
        ItemStack bat = stack("wathe:bat");
        ElementLink<ActorElement> jester = Actors.enter(scene, color, scrambled(), new Vec3d(5.6, 1, 1.4),
                SCREEN_RIGHT, Direction.DOWN);
        ElementLink<ActorElement> victim = Actors.enter(scene, 0xFFFFFF, scrambled(), new Vec3d(3.4, 1, 3.2),
                SCREEN_RIGHT, Direction.DOWN);
        ElementLink<ActorElement> first = Actors.enter(scene, 0xFFFFFF, scrambled(), new Vec3d(1.2, 1, 5.0),
                SCREEN_LEFT, Direction.DOWN);
        ElementLink<ActorElement> second = Actors.enter(scene, 0xFFFFFF, scrambled(), new Vec3d(4.2, 1, 5.6),
                NORTH, Direction.DOWN);
        for (ElementLink<ActorElement> actor : List.of(jester, victim, first, second)) {
            Actors.psycho(scene, actor, true);
            Actors.hold(scene, actor, bat);
        }
        Actors.highlight(scene, jester, color, 640);
        scene.idle(20);
        scene.overlay().showText(90)
                .text("小丑时刻：你拿着球棒追杀所有人（方框为示意）")
                .independent()
                .attachKeyFrame();
        scene.idle(60);
        Actors.walk(scene, jester, new Vec3d(-1.2, 0, 1.0), 16);
        scene.idle(30);
        Actors.swing(scene, jester);
        // The body is drawn with the victim's own skin. 尸体按死者本人的皮肤绘制。
        Actors.retint(scene, victim, RoleColors.CIVILIAN, null);
        Actors.fall(scene, victim);
        scene.idle(10);
        scene.overlay().showText(90)
                .colored(PonderPalette.RED)
                .text("蓄满力的一棒一般能打死人；每杀一人加 30 秒、补回护盾")
                .independent()
                .attachKeyFrame();
        Actors.walk(scene, first, new Vec3d(-0.4, 0, 1.0), 18);
        Actors.walk(scene, second, new Vec3d(1.6, 0, 0.6), 18);
        scene.idle(100);
        scene.overlay().showText(90)
                .text("疯魔限时 60 秒：体力无限，没被堵住的门都能打开")
                .independent();
        scene.idle(100);
        scene.overlay().showText(90)
                .text("杀光其他所有人就赢；倒计时耗尽你就会死")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showText(90)
                .text("每杀一人画面变灰 10%（最多 50%），也听不到任何人说话")
                .independent();
        scene.idle(100);
        scene.markAsFinished();
    }

    /**
     * Taotie (NoellesRoles swallow packet, TaotiePlayerComponent, SwallowedPlayerComponent; SparkStrength TaotieHeadService,
     * TaotieHeadRules, TaotieHeadEntity, TaotieHeadEntityRenderer): the ability key swallows the player under the
     * crosshair within 3 blocks and in sight, with no sound or effect and no body; they live on inside, seeing through
     * the Taotie's eyes and heard only by it and the others inside. With anyone alive inside, the second ability key
     * spits one head per living swallowed player, each wearing that player's face; it homes on someone ahead within
     * 8 blocks at 1 block a tick, trailing brown dust and smoke. A hit blinds and slows (Slowness V) for 3 s and locks
     * items and skills, but never kills. A dead Taotie releases everyone inside where it fell.
     * 饕餮：技能键吞下准星对着、3 格内且看得见的人，没有声音和特效，也不留尸体；被吞的人在肚子里活着，只能透过饕餮的眼睛看，
     * 说话只有饕餮和肚子里的人听得到。肚子里有活人时，第二技能键为每个被吞的活人吐出一颗长着他的脸的头颅，追向前方 8 格内
     * 的人，每 tick 飞 1 格，拖着棕色粉尘和烟。砸中的人失明、缓慢 V 3 秒，用不了物品和技能，但不会被砸死。饕餮死亡时，
     * 肚子里的人会在原地全部放出来。
     */
    private static void taotie(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("role_taotie", "饕餮：吞人与吐头颅");
        WatheItemScenes.stage(scene);
        int color = RoleColors.of("noellesroles:taotie", 0x8B4513);
        ElementLink<ActorElement> meal = Actors.enter(scene, RoleColors.CIVILIAN, Text.literal("平民"),
                new Vec3d(3.6, 1, 3.2), SCREEN_RIGHT, Direction.DOWN);
        scene.idle(10);
        ElementLink<ActorElement> taotie = Actors.enter(scene, color, Text.literal("饕餮"),
                new Vec3d(5.8, 1, 1.0), SCREEN_RIGHT, Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(80)
                .text("饕餮是中立阵营：把人一个个吞进肚子")
                .independent()
                .attachKeyFrame();
        scene.idle(30);
        Actors.walk(scene, taotie, new Vec3d(-1.0, 0, 1.0), 16);
        scene.idle(60);
        scene.overlay().showText(70)
                .text("准星对准 3 格内看得见的人，按技能键（默认 G）")
                .independent()
                .attachKeyFrame();
        scene.idle(50);
        Actors.vanish(scene, meal);
        scene.idle(30);
        scene.overlay().showText(90)
                .colored(PonderPalette.RED)
                .text("他当场消失：没有声音和特效，也不留尸体")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showText(90)
                .text("他没死，只能透过你的眼睛看；说话只有你和肚子里的人听得到")
                .independent();
        scene.idle(100);
        scene.overlay().showText(80)
                .text("吞完要冷却 20～50 秒（开局人越多越短）；演示省略了等待")
                .independent();
        ElementLink<ActorElement> vigilante = Actors.enter(scene, RoleColors.VIGILANTE, Text.literal("义警"),
                new Vec3d(0.8, 1, 6.0), SCREEN_LEFT, Direction.DOWN);
        Actors.hold(scene, vigilante, stack("wathe:revolver"));
        scene.idle(40);
        Actors.walk(scene, vigilante, new Vec3d(1.2, 0, -1.2), 24);
        scene.idle(60);
        scene.overlay().showText(90)
                .text("第二技能键（默认 N）吐出头颅：肚子里每个活人一颗")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showText(85)
                .text("头颅长着被吞者的脸，追向前方 8 格内的人（演示放慢）")
                .independent()
                .attachKeyFrame();
        scene.idle(40);
        // From 0.6 ahead of the eyes to the vigilante's face: about 3 game ticks, slowed down here.
        // 从眼前 0.6 格飞到义警脸上：游戏中约 3 tick，这里放慢了。
        Vec3d hit = new Vec3d(2.4, 2.62, 4.4);
        spitHead(scene, ActorSkins.of(RoleColors.CIVILIAN), new Vec3d(4.38, 2.62, 2.42), hit, 16);
        scene.idle(16);
        spray(scene, ParticleTypes.POOF, hit, 12, 0.2, 0.02);
        // The swallow lands well inside the 3 s daze. 吞噬发生在 3 秒眩晕之内。
        scene.idle(20);
        Actors.walk(scene, taotie, new Vec3d(-1.8, 0, 1.8), 18);
        scene.idle(19);
        scene.overlay().showText(90)
                .colored(PonderPalette.RED)
                .text("砸中的人 3 秒内失明、走不快，也用不了物品和技能")
                .independent()
                .attachKeyFrame();
        scene.idle(11);
        Actors.vanish(scene, vigilante);
        scene.idle(89);
        scene.overlay().showText(80)
                .text("趁他还不了手，上前把他也吞掉")
                .independent();
        scene.idle(90);
        scene.overlay().showText(90)
                .text("头颅砸不死人，还会亮出你吞了谁；你一死，肚子里的人全放出来")
                .independent();
        scene.idle(100);
        scene.markAsFinished();
    }

    /**
     * Morph (NoellesRoles MorphlingScreenMixin, MorphlingPlayerWidget, morph packet, MorphlingPlayerComponent and the
     * morphling renderer mixins): the inventory lists every other player's head, the dead included; clicking one
     * makes the Morphling look like that player to everyone for 35 s (skin, and the name shown under the crosshair
     * within 2 blocks), with no particle or sound; the voice stays its own and fellow killers' instinct still shows it
     * as a killer. 20 s cooldown after it ends; nothing for the first 30 s of a round by default.
     * 变形：背包里列出其他所有玩家的头像（死人也在内）；点一个，35 秒内在所有人眼里都是那名玩家的样子（皮肤，以及准星对着
     * 2 格内时显示的名字），没有粒子和声音；声音仍是自己的，杀手同伴的本能仍显示他是杀手。结束后冷却 20 秒；开局默认 30 秒
     * 内不能用。
     */
    private static void morph(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("role_morphling", "变形者：变成别人的样子");
        WatheItemScenes.stage(scene);
        int color = RoleColors.of("noellesroles:morphling", 0xAA023D);
        ItemStack head = stack("minecraft:player_head");
        ItemStack knife = stack("wathe:knife");
        Text ming = Text.literal("阿明");
        ElementLink<ActorElement> real = Actors.enter(scene, RoleColors.CIVILIAN, ming, new Vec3d(1.4, 1, 6.2),
                SCREEN_LEFT, Direction.DOWN);
        ElementLink<ActorElement> victim = Actors.enter(scene, RoleColors.CIVILIAN, Text.literal("小红"),
                new Vec3d(2.4, 1, 3.4), SCREEN_RIGHT, Direction.DOWN);
        ElementLink<ActorElement> witness = Actors.enter(scene, RoleColors.CIVILIAN, Text.literal("阿杰"),
                new Vec3d(6.2, 1, 3.4), WEST, Direction.DOWN);
        scene.idle(10);
        ElementLink<ActorElement> morphling = Actors.enter(scene, color, Text.literal("变形者"),
                new Vec3d(5.8, 1, 0.8), SCREEN_RIGHT, Direction.DOWN);
        Actors.hold(scene, morphling, knife);
        scene.idle(15);
        scene.overlay().showText(80)
                .text("变形者（杀手）：能变成本局其他玩家的样子")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        scene.overlay().showControls(new Vec3d(1.4, 3.6, 6.2), Pointing.DOWN, 40).leftClick().withItem(head);
        scene.overlay().showText(80)
                .text("按 E 打开背包，点一名玩家的头像（死人也能选）")
                .independent()
                .attachKeyFrame();
        scene.idle(50);
        Actors.retint(scene, morphling, RoleColors.CIVILIAN, ming);
        scene.idle(40);
        scene.overlay().showText(90)
                .text("35 秒内，别人看到的你就是阿明：样子和名字，没有任何特效")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        Actors.walk(scene, morphling, new Vec3d(-2.4, 0, 1.8), 30);
        scene.idle(32);
        Actors.charge(scene, morphling, true);
        scene.idle(20);
        Actors.charge(scene, morphling, false);
        Actors.swing(scene, morphling);
        Actors.fall(scene, victim);
        scene.idle(10);
        scene.overlay().showText(90)
                .colored(PonderPalette.RED)
                .text("在阿杰看来，动手的就是“阿明”")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        Actors.highlight(scene, real, 0xFFFFFF, 100);
        scene.overlay().showText(90)
                .text("破绽：真阿明还在别处（方框为示意）；声音也还是你的")
                .independent();
        scene.idle(100);
        scene.overlay().showText(80)
                .text("杀手同伴开本能时，仍能认出你是自己人")
                .independent();
        Actors.walk(scene, morphling, new Vec3d(2.4, 0, -1.8), 30);
        scene.idle(90);
        Actors.retint(scene, morphling, color, Text.literal("变形者"));
        scene.overlay().showText(70)
                .text("35 秒后（演示缩短了）变回原样")
                .independent()
                .attachKeyFrame();
        scene.idle(80);
        scene.overlay().showText(80)
                .text("之后冷却 20 秒；开局默认 30 秒内不能变形")
                .independent();
        scene.idle(90);
        scene.markAsFinished();
    }

    /**
     * Corpse mode (NoellesRoles corpse toggle packet, MorphlingPlayerComponent, MorphlingCorpseRendererMixin and the
     * collision mixins): the ability key toggles it, with no cooldown. The Morphling is drawn at once as Wathe draws a
     * body (no fall, a block back from where it stands, face down) with no name, at Slowness III, and can still act;
     * like a body it can be walked through. A held item still shows in its hand, and fellow killers holding the
     * instinct key see it outlined.
     * 尸体伪装：技能键开关，没有冷却。变形者立刻按 Wathe 尸体的样子绘制（没有倒下动画，比站立处靠后一格，脸朝下），不显示
     * 名字，带缓慢 III，仍能行动；和尸体一样能被人穿过。手里拿着的东西仍会显示，杀手同伴按住本能键能看到他的轮廓。
     */
    private static void corpseMode(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("role_morphling_corpse", "变形者：装成尸体");
        WatheItemScenes.stage(scene);
        int color = RoleColors.of("noellesroles:morphling", 0xAA023D);
        ItemStack knife = stack("wathe:knife");
        Vec3d spot = new Vec3d(3.6, 1, 3.0);
        ElementLink<ActorElement> morphling = Actors.enter(scene, color, Text.literal("变形者"), spot,
                SCREEN_RIGHT, Direction.DOWN);
        Actors.hold(scene, morphling, knife);
        scene.idle(15);
        scene.overlay().showText(90)
                .text("技能键（默认 G）开关尸体伪装，没有冷却")
                .independent()
                .attachKeyFrame();
        scene.idle(60);
        Actors.hold(scene, morphling, ItemStack.EMPTY);
        scene.idle(10);
        ElementLink<Corpse> corpse = playDead(scene, morphling, color, spot, SCREEN_RIGHT);
        scene.idle(30);
        scene.overlay().showText(90)
                .text("你立刻躺倒、不显示名字，看起来就像一具尸体")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showText(80)
                .text("躺着时移动变慢，但其他动作照常")
                .independent();
        ElementLink<ActorElement> passenger = Actors.enter(scene, RoleColors.CIVILIAN, Text.literal("平民"),
                new Vec3d(0.4, 1, 6.0), SCREEN_LEFT, Direction.DOWN);
        scene.idle(30);
        Actors.walk(scene, passenger, new Vec3d(1.0, 0, -1.2), 30);
        scene.idle(60);
        scene.overlay().showText(70)
                .text("有人走近查看时……")
                .independent()
                .attachKeyFrame();
        scene.idle(30);
        getUp(scene, morphling, corpse);
        Actors.hold(scene, morphling, knife);
        scene.idle(10);
        Actors.charge(scene, morphling, true);
        scene.idle(15);
        Actors.charge(scene, morphling, false);
        Actors.swing(scene, morphling);
        Actors.fall(scene, passenger);
        scene.idle(35);
        scene.overlay().showText(80)
                .colored(PonderPalette.RED)
                .text("……再按一次 G 起身出刀")
                .independent();
        scene.idle(90);
        scene.overlay().showText(90)
                .text("注意：手里拿着东西会露馅；杀手同伴按住本能键看得到你")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showText(90)
                .text("可以先变成已死的人再躺下，装得更像")
                .independent();
        scene.idle(100);
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

    /**
     * The 9-wide aisle, slightly further out than the 7-wide one so the M67's 5-block reach fits on it.
     * 9 格宽的走廊，比 7 格的稍远一些，让 M67 的 5 格范围放得下。
     */
    private static void wideStage(SceneBuilder scene) {
        scene.configureBasePlate(0, 0, 9);
        scene.scaleSceneView(1.2f);
        scene.showBasePlate();
        scene.idle(10);
    }

    /**
     * SparkStrength's grenade drone entity, drawn by its own renderer, set down at {@code at} facing {@code yaw} with
     * an M67 bound (as DroneItem.useOnBlock places it). 由 SparkStrength 自己的渲染器绘制的投弹无人机实体，像
     * DroneItem.useOnBlock 那样挂着 M67、朝 yaw 放在 at 处。
     */
    private static ElementLink<EntityElement> placeDrone(SceneBuilder scene, Vec3d at, float yaw) {
        return scene.world().createEntity(world -> {
            Entity drone = Registries.ENTITY_TYPE.get(Identifier.of("sparkstrength", "grenade_drone")).create(world);
            drone.refreshPositionAndAngles(at.x, at.y, at.z, yaw, 0);
            droneData(drone, "PAYLOAD", true);
            return drone;
        });
    }

    /**
     * Set one of DroneEntity's synced fields (STATE: 0 grounded, 1 hovering, 2 flying; PAYLOAD) as the server would.
     * SparkAssist does not build against SparkStrength, so the field is looked up by name; if it is missing the drone
     * simply keeps its default look.
     * 像服务端那样设置 DroneEntity 的同步字段（STATE：0 停放、1 悬停、2 飞行；PAYLOAD 挂载）。SparkAssist 不依赖
     * SparkStrength 编译，所以按名字查找字段；找不到时无人机保持默认外观。
     */
    @SuppressWarnings("unchecked")
    private static <T> void droneData(Entity drone, String field, T value) {
        try {
            Field data = drone.getClass().getDeclaredField(field);
            data.setAccessible(true);
            drone.getDataTracker().set((TrackedData<T>) data.get(null), value);
        } catch (ReflectiveOperationException | ClassCastException e) {
            // Another SparkStrength version: keep the default look. 其他版本的 SparkStrength：保持默认外观。
        }
    }

    /** Fly the drone by {@code delta} over {@code ticks}; it hovers once there. 在 ticks 内让无人机飞过 delta，到达后悬停。 */
    private static void flyDrone(SceneBuilder scene, ElementLink<EntityElement> drone, Vec3d delta, int ticks) {
        scene.addInstruction(new DroneFlight(drone, delta, ticks));
    }

    /** The M67 as a dropped one looks: the armed model (custom model data 1). 投下的 M67 的样子：已拉环的模型。 */
    private static ItemStack armedM67() {
        ItemStack m67 = stack("sparkstrength:m67");
        m67.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent(1));
        return m67;
    }

    /** A {@code radius} ring on the floor around {@code center}, kept on a {@code size} plate. 地面上以 center 为圆心、半径 radius 的圆圈，只画在 size 大小的底板内。 */
    private static void blastRing(SceneBuilder scene, Vec3d center, double radius, int size, int ticks) {
        int segments = 64;
        for (int i = 0; i < segments; i++) {
            double a = MathHelper.TAU * i / segments;
            double b = MathHelper.TAU * (i + 1) / segments;
            Vec3d from = center.add(Math.cos(a) * radius, 0.02, Math.sin(a) * radius);
            Vec3d to = center.add(Math.cos(b) * radius, 0.02, Math.sin(b) * radius);
            if (onPlate(from, size) && onPlate(to, size)) {
                scene.overlay().showLine(PonderPalette.RED, from, to, ticks);
            }
        }
    }

    private static boolean onPlate(Vec3d point, int size) {
        return point.x >= 0 && point.x <= size && point.z >= 0 && point.z <= size;
    }

    /**
     * A piloted drone's flight: an even step every tick, turning to face the way when it moves sideways; its rotors run
     * (flying while it moves sideways, hovering otherwise) and it hovers once there. Ponder's world does not reset an
     * entity's previous position, so it is set here for the renderer's banking.
     * 驾驶中的无人机飞行：每 tick 匀速前进，横向移动时转向前进方向；旋翼转动（横移时为飞行，否则为悬停），到达后悬停。
     * Ponder 的世界不会重置实体的上一位置，所以在这里设置，供渲染器计算倾斜。
     */
    private static final class DroneFlight extends TickingInstruction {
        private static final byte HOVERING = 1;
        private static final byte FLYING = 2;
        private final ElementLink<EntityElement> link;
        private final Vec3d step;
        private final boolean sideways;
        private final float heading;

        private DroneFlight(ElementLink<EntityElement> link, Vec3d delta, int ticks) {
            super(false, ticks);
            this.link = link;
            this.step = delta.multiply(1.0 / ticks);
            this.sideways = delta.horizontalLengthSquared() > 1.0E-6;
            this.heading = (float) (MathHelper.atan2(-delta.x, delta.z) * MathHelper.DEGREES_PER_RADIAN);
        }

        @Override
        public void tick(PonderScene scene) {
            super.tick(scene);
            EntityElement element = scene.resolve(link);
            if (element == null) {
                return;
            }
            element.ifPresent(drone -> {
                droneData(drone, "STATE", remainingTicks > 0 ? FLYING : HOVERING);
                drone.prevX = drone.getX();
                drone.prevY = drone.getY();
                drone.prevZ = drone.getZ();
                if (sideways) {
                    drone.setYaw(heading);
                }
                drone.setPosition(drone.getPos().add(step));
            });
        }
    }

    /**
     * The name under the crosshair during the Jester moment (NoellesRoles MorphlingRoleNameRendererMixin): scrambled.
     * 小丑时刻里准星下显示的名字（MorphlingRoleNameRendererMixin）：乱码。
     */
    private static Text scrambled() {
        return Text.literal("??!?!").formatted(Formatting.OBFUSCATED);
    }

    /**
     * The Jester's fake body: spawned where the Jester stands (JesterPlayerComponent.beginFakeDeath), not a block
     * ahead as a kill's is (GameFunctions.killPlayer), so with Wathe's body renderer it lies a block further back.
     * 小丑的假尸体：生成在小丑站立处（beginFakeDeath），而不是像击杀那样生成在前方一格（killPlayer），所以按 Wathe 的
     * 尸体渲染会比击杀的尸体靠后一格。
     */
    private static void fakeBody(SceneBuilder scene, ElementLink<ActorElement> jester) {
        scene.addInstruction(ponder -> {
            ActorElement actor = ponder.resolve(jester);
            if (actor != null) {
                float radians = actor.yaw() * MathHelper.RADIANS_PER_DEGREE;
                actor.moveTo(actor.position().add(MathHelper.sin(radians), 0, -MathHelper.cos(radians)));
                actor.setVisible(false);
            }
        });
        scene.idle(1);
        reveal(scene, jester);
        Actors.fall(scene, jester);
    }

    /**
     * Move each actor at once to its spot and facing, hidden for the tick in which they move so none streaks across
     * the stage (a server teleport). 立即把每名演员移到各自的位置与朝向；移动的那一 tick 先隐藏，避免拖影（服务端传送）。
     */
    private static void relocate(SceneBuilder scene, List<ElementLink<ActorElement>> actors, List<Vec3d> spots,
                                 float[] yaws) {
        scene.addInstruction(ponder -> {
            for (int i = 0; i < actors.size(); i++) {
                ActorElement actor = ponder.resolve(actors.get(i));
                if (actor != null) {
                    actor.moveTo(spots.get(i));
                    actor.setYaw(yaws[i]);
                    actor.setVisible(false);
                }
            }
        });
        scene.idle(1);
        for (ElementLink<ActorElement> actor : actors) {
            reveal(scene, actor);
        }
    }

    /** Show a hidden actor again where it is. 让隐藏的演员在原处重新出现。 */
    private static void reveal(SceneBuilder scene, ElementLink<ActorElement> link) {
        scene.addInstruction(ponder -> {
            ActorElement actor = ponder.resolve(link);
            if (actor != null) {
                actor.setVisible(true);
            }
        });
    }

    /**
     * The Jester's stasis: 5 glow particles a tick around its middle (JesterPlayerComponent.serverTick: offset 0.5,
     * speed 0.02), seen by everyone near. 小丑的禁锢：每 tick 在身体中部放出 5 个发光粒子（偏移 0.5、速度 0.02），附近的人都看得见。
     */
    private static void glow(SceneBuilder scene, Vec3d at, int ticks) {
        scene.addInstruction(new TickingInstruction(false, ticks) {
            @Override
            public void tick(PonderScene ponder) {
                super.tick(ponder);
                for (int i = 0; i < 5; i++) {
                    ponder.getWorld().addParticle(ParticleTypes.GLOW, at.x + RANDOM.nextGaussian() * 0.5,
                            at.y + RANDOM.nextGaussian() * 0.5, at.z + RANDOM.nextGaussian() * 0.5,
                            RANDOM.nextGaussian() * 0.02, RANDOM.nextGaussian() * 0.02, RANDOM.nextGaussian() * 0.02);
                }
            }
        });
    }

    /** Spit one Taotie head from {@code from} to {@code to}, box centres. 把一颗饕餮头颅从 from 吐到 to（碰撞箱中心）。 */
    private static void spitHead(SceneBuilder scene, Identifier skin, Vec3d from, Vec3d to, int ticks) {
        TaotieHead head = new TaotieHead(skin, from, to, ticks);
        scene.addInstruction(ponder -> {
            head.reset(ponder);
            head.setVisible(true);
            head.setFade(1);
            ponder.addElement(head);
        });
    }

    /**
     * Turn corpse mode on: the standing actor is hidden and a lying one takes its place.
     * 开启尸体伪装：隐藏站立的演员，换成躺着的样子。
     */
    private static ElementLink<Corpse> playDead(SceneBuilder scene, ElementLink<ActorElement> actor, int color,
                                                Vec3d feet, float yaw) {
        Corpse corpse = new Corpse(color, feet, yaw);
        ElementLink<Corpse> link = new ElementLinkImpl<>(Corpse.class);
        Actors.vanish(scene, actor);
        scene.addInstruction(ponder -> {
            corpse.setVisible(true);
            corpse.setFade(1);
            ponder.addElement(corpse);
            ponder.linkElement(corpse, link);
        });
        return link;
    }

    /** Turn corpse mode off: standing again at once. 关闭尸体伪装：立刻重新站起。 */
    private static void getUp(SceneBuilder scene, ElementLink<ActorElement> actor, ElementLink<Corpse> corpse) {
        scene.addInstruction(ponder -> {
            Corpse lying = ponder.resolve(corpse);
            if (lying != null) {
                lying.setVisible(false);
            }
        });
        reveal(scene, actor);
    }

    /**
     * A Taotie head as TaotieHeadEntityRenderer draws it: a vanilla player skull at its natural size wearing the
     * swallowed player's skin, its face leading along the flight; each tick it leaves 2 brown dust particles and
     * 1 smoke (TaotieHeadEntity.spawnTrail). It is gone once it reaches {@code to} (the hit).
     * 按 TaotieHeadEntityRenderer 绘制的饕餮头颅：原版大小的玩家头颅，穿着被吞者的皮肤，脸朝飞行方向；每 tick 留下 2 个棕色粉尘
     * 和 1 个烟雾粒子（spawnTrail）。到达 to（命中）后消失。
     */
    private static final class TaotieHead extends AnimatedSceneElementBase {
        private static final DustParticleEffect TRAIL = new DustParticleEffect(new Vector3f(0x8B / 255f,
                0x45 / 255f, 0x13 / 255f), 1.0f);
        @Nullable
        private static SkullEntityModel model;
        private final Identifier skin;
        private final Vec3d from;
        private final Vec3d to;
        private final int ticks;
        private final float yaw;
        private final float pitch;
        private int age;

        private TaotieHead(Identifier skin, Vec3d from, Vec3d to, int ticks) {
            this.skin = skin;
            this.from = from;
            this.to = to;
            this.ticks = ticks;
            Vec3d heading = to.subtract(from);
            this.yaw = (float) (MathHelper.atan2(heading.x, heading.z) * MathHelper.DEGREES_PER_RADIAN);
            this.pitch = (float) (MathHelper.atan2(heading.y, heading.horizontalLength())
                    * MathHelper.DEGREES_PER_RADIAN);
        }

        @Override
        public void reset(@Nullable PonderScene scene) {
            age = 0;
        }

        @Override
        public void tick(PonderScene scene) {
            age++;
            if (age > ticks) {
                return;
            }
            Vec3d at = from.lerp(to, age / (float) ticks);
            for (int i = 0; i < 2; i++) {
                scene.getWorld().addParticle(TRAIL, at.x + (RANDOM.nextDouble() - 0.5) * 0.2,
                        at.y + (RANDOM.nextDouble() - 0.5) * 0.2, at.z + (RANDOM.nextDouble() - 0.5) * 0.2, 0, 0, 0);
            }
            scene.getWorld().addParticle(ParticleTypes.SMOKE, at.x + (RANDOM.nextDouble() - 0.5) * 0.15,
                    at.y + (RANDOM.nextDouble() - 0.5) * 0.15, at.z + (RANDOM.nextDouble() - 0.5) * 0.15, 0, 0, 0);
        }

        @Override
        protected void renderLast(PonderLevel world, VertexConsumerProvider buffer, DrawContext graphics, float fade,
                                  float pt) {
            float time = age + pt;
            if (fade <= 0.01f || time >= ticks) {
                return;
            }
            if (model == null) {
                model = new SkullEntityModel(MinecraftClient.getInstance().getEntityModelLoader()
                        .getModelPart(EntityModelLayers.PLAYER_HEAD));
            }
            Vec3d at = from.lerp(to, time / ticks);
            MatrixStack ms = graphics.getMatrices();
            ms.push();
            ms.translate(at.x, at.y, at.z);
            ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yaw + 180));
            ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));
            ms.translate(0, -0.25, 0);
            ms.scale(-1, -1, 1);
            model.setHeadRotation(0, 0, 0);
            model.render(ms, buffer.getBuffer(RenderLayer.getEntityTranslucent(skin)), lightCoordsFromFade(fade),
                    OverlayTexture.DEFAULT_UV, 0xFFFFFFFF);
            ms.pop();
        }
    }

    /**
     * A Morphling in corpse mode as MorphlingCorpseRendererMixin draws it: Wathe's body transforms at full tilt (no
     * fall), so face down towards {@code yaw} with the feet a block behind where it stands, and no name.
     * 尸体伪装中的变形者，按 MorphlingCorpseRendererMixin 绘制：直接套用 Wathe 尸体的变换（没有倒下动画），朝 yaw 脸朝下，
     * 脚在站立处后方一格，不显示名字。
     */
    private static final class Corpse extends AnimatedSceneElementBase {
        @Nullable
        private static PlayerEntityModel<LivingEntity> model;
        private final int color;
        private final Vec3d feet;
        private final float yaw;

        private Corpse(int color, Vec3d standing, float yaw) {
            this.color = color;
            this.yaw = yaw;
            float radians = yaw * MathHelper.RADIANS_PER_DEGREE;
            this.feet = standing.add(MathHelper.sin(radians), 0, -MathHelper.cos(radians));
        }

        @Override
        protected void renderLast(PonderLevel world, VertexConsumerProvider buffer, DrawContext graphics, float fade,
                                  float pt) {
            if (fade <= 0.01f) {
                return;
            }
            if (model == null) {
                model = new PlayerEntityModel<>(MinecraftClient.getInstance().getEntityModelLoader()
                        .getModelPart(EntityModelLayers.PLAYER), false);
            }
            MatrixStack ms = graphics.getMatrices();
            ms.push();
            ms.translate(feet.x, feet.y, feet.z);
            ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180 - yaw));
            ms.translate(0, 0.15, 0);
            ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90));
            ms.scale(-0.9375f, -0.9375f, 0.9375f);
            ms.translate(0, -1.501, 0);
            model.setVisible(true);
            model.child = false;
            model.riding = false;
            model.sneaking = false;
            model.handSwingProgress = 0;
            model.leftArmPose = BipedEntityModel.ArmPose.EMPTY;
            model.rightArmPose = BipedEntityModel.ArmPose.EMPTY;
            model.getHead().resetTransform();
            model.body.resetTransform();
            model.rightArm.resetTransform();
            model.leftArm.resetTransform();
            model.rightLeg.resetTransform();
            model.leftLeg.resetTransform();
            model.hat.copyTransform(model.head);
            model.render(ms, buffer.getBuffer(RenderLayer.getEntityCutoutNoCull(ActorSkins.of(color))),
                    lightCoordsFromFade(fade), OverlayTexture.DEFAULT_UV, 0xFFFFFFFF);
            ms.pop();
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

    /**
     * Phantom (NoellesRoles ability packet, PhantomHudMixin, MorphlingRoleNameRendererMixin; Wathe KnifeItem,
     * WatheClient.getInstinctHighlight): the ability key gives the Phantom 30 s of vanilla Invisibility without
     * particles and starts a 90 s cooldown at once (30 s opening cooldown by default). Others no longer see the body,
     * nor the name under the crosshair, but held items still render, sprinting still kicks up the floor's particles and
     * the hitbox stays, so the Phantom can be bumped into, stabbed and shot. The knife still needs its charge, with the
     * raise sound nearby players hear; nothing ends the invisibility early. Fellow killers holding the instinct key
     * usually still see the Phantom outlined.
     * 幽灵：技能键给自己 30 秒原版隐身（无粒子），90 秒冷却同时开始计算（开局默认冷却 30 秒）。别人看不见他的身体，准星
     * 对着也不显示名字，但手持物品照常显示，疾跑仍会踢起地面粒子，碰撞箱也还在，所以会被撞到、刺中和枪击。用刀仍要蓄力，
     * 附近的人听得到举刀声；隐身不会提前结束。杀手同伴按住本能键一般仍能看到他的轮廓。
     */
    private static void phantom(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("role_phantom", "幽灵：隐身摸到人身后");
        WatheItemScenes.stage(scene);
        int color = RoleColors.of("noellesroles:phantom", 0x500505);
        ItemStack knife = stack("wathe:knife");
        ElementLink<ActorElement> victim = Actors.enter(scene, RoleColors.CIVILIAN, Text.literal("平民"),
                new Vec3d(2.2, 1, 4.8), SCREEN_RIGHT, Direction.DOWN);
        scene.idle(5);
        ElementLink<ActorElement> phantom = Actors.enter(scene, color, Text.literal("幽灵"),
                new Vec3d(6.4, 1, 0.6), SCREEN_RIGHT, Direction.DOWN);
        Actors.hold(scene, phantom, knife);
        scene.idle(15);
        scene.overlay().showText(80)
                .text("幽灵（杀手）：按技能键（默认 G）隐身 30 秒")
                .independent()
                .attachKeyFrame();
        scene.idle(50);
        Actors.invisible(scene, phantom, true);
        scene.idle(40);
        scene.overlay().showText(90)
                .text("别人看不见你的身体和名字，手里的刀却照样看得见")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        Actors.walk(scene, phantom, new Vec3d(-2.2, 0, 2.2), 13);
        sprintDust(scene, phantom, 13);
        scene.overlay().showText(80)
                .text("疾跑照样会踢起脚下的粒子；靠近目标时最好慢慢走")
                .independent()
                .attachKeyFrame();
        scene.idle(15);
        Actors.walk(scene, phantom, new Vec3d(-0.8, 0, 0.8), 20);
        scene.idle(75);
        scene.overlay().showControls(new Vec3d(3.4, 3.6, 3.6), Pointing.DOWN, 30).rightClick().withItem(knife);
        Actors.charge(scene, phantom, true);
        scene.idle(20);
        Actors.charge(scene, phantom, false);
        Actors.swing(scene, phantom);
        Actors.fall(scene, victim);
        scene.idle(10);
        scene.overlay().showText(80)
                .colored(PonderPalette.RED)
                .text("出刀照样要蓄力、有举刀声；得手后你还隐着身")
                .independent()
                .attachKeyFrame();
        Actors.walk(scene, phantom, new Vec3d(2.0, 0, -2.0), 30);
        scene.idle(32);
        Actors.turn(scene, phantom, SCREEN_RIGHT);
        scene.idle(58);
        scene.overlay().showText(80)
                .text("身体还在：照样会被人撞到、被刀刺中、被枪打中")
                .independent();
        scene.idle(90);
        // A killer holding the instinct key sees fellow killers in hsvToRgb(0, 1, 0.6) (WatheClient).
        // 杀手按住本能键时，杀手同伴显示为 hsvToRgb(0, 1, 0.6) 的颜色。
        Actors.highlight(scene, phantom, 0x990000, 80);
        scene.overlay().showText(80)
                .text("杀手同伴按住本能键一般仍看得到你（方框为示意）")
                .independent();
        scene.idle(90);
        Actors.invisible(scene, phantom, false);
        scene.overlay().showText(80)
                .text("30 秒后现形（演示缩短了）；冷却 90 秒从隐身时算起")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        scene.markAsFinished();
    }

    /**
     * Scavenger (NoellesRoles ScavengerInstantKnifeMixin, ScavengerKnifeSoundMixin, ScavengerKnifeStabSoundMixin,
     * ScavengerBodyHideMixin, ScavengerBodyCanHitMixin, HiddenBodiesWorldComponent, ScavengerShopHandler; Wathe
     * KnifeStabPayload): a single right-click with the knife stabs the player in front within 3 blocks at once; on a
     * hit there is no charge pose and neither the raise nor the stab sound; the Scavenger swings as after any stab, and
     * the knife cooldown still applies (the shop sells a reset, 150 coins by default). The body of anyone it kills with
     * the knife, a Noisemaker excepted, is not drawn for living players outside the killer and neutral factions, so to
     * them the victim simply drops out of sight; the dead still see it.
     * 清道夫：拿刀右键一下即刺中正前方 3 格内的人；刺中时没有蓄力姿势，也没有举刀声和刺杀声；出刀后照常挥一下手，刀的冷
     * 却照常（商店可买重置，默认 150 金币）。被他用刀杀死的人（大嗓门除外），尸体不会绘制给杀手与中立阵营以外的活人，所
     * 以在他们眼里被害者就这么不见了；死者仍看得到尸体。
     */
    private static void scavenger(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("role_scavenger", "清道夫：无声瞬刺，尸体藏起来");
        WatheItemScenes.stage(scene);
        int color = RoleColors.of("noellesroles:scavenger", 0x654321);
        ItemStack knife = stack("wathe:knife");
        ElementLink<ActorElement> victim = Actors.enter(scene, RoleColors.CIVILIAN, Text.literal("平民"),
                new Vec3d(2.2, 1, 4.6), SCREEN_RIGHT, Direction.DOWN);
        scene.idle(5);
        ElementLink<ActorElement> scavenger = Actors.enter(scene, color, Text.literal("清道夫"),
                new Vec3d(5.8, 1, 1.0), SCREEN_RIGHT, Direction.DOWN);
        Actors.hold(scene, scavenger, knife);
        scene.idle(15);
        scene.overlay().showText(80)
                .text("清道夫（杀手）：拿着刀右键点一下就出刀")
                .independent()
                .attachKeyFrame();
        scene.idle(40);
        Actors.walk(scene, scavenger, new Vec3d(-2.2, 0, 2.2), 26);
        scene.idle(50);
        scene.overlay().showControls(new Vec3d(3.6, 3.6, 3.2), Pointing.DOWN, 20).rightClick().withItem(knife);
        scene.idle(10);
        Actors.swing(scene, scavenger);
        Actors.fall(scene, victim);
        scene.idle(10);
        scene.overlay().showText(90)
                .colored(PonderPalette.RED)
                .text("不用按住蓄力，也没有举刀声和刺杀声")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showText(90)
                .text("刀杀的人，尸体一般只有杀手、中立和死者看得见")
                .independent()
                .attachKeyFrame();
        Actors.walk(scene, scavenger, new Vec3d(2.0, 0, -2.0), 28);
        scene.idle(30);
        Actors.turn(scene, scavenger, SCREEN_RIGHT);
        scene.idle(70);
        // From here on the stage shows what a civilian sees. 此后舞台展示的是平民看到的画面。
        Actors.vanish(scene, victim);
        ElementLink<ActorElement> passenger = Actors.enter(scene, RoleColors.CIVILIAN, Text.literal("平民"),
                new Vec3d(0.4, 1, 6.4), SCREEN_LEFT, Direction.DOWN);
        scene.overlay().showText(90)
                .text("而在好人眼里（示意），这里什么都没有")
                .independent()
                .attachKeyFrame();
        scene.idle(20);
        Actors.walk(scene, passenger, new Vec3d(2.4, 0, -2.4), 40);
        scene.idle(80);
        scene.overlay().showText(80)
                .text("大嗓门的尸体藏不住；不是用刀杀的也照常留尸体")
                .independent();
        scene.idle(90);
        scene.overlay().showText(80)
                .text("商店默认 150 金币就能立刻清空刀的冷却")
                .independent();
        scene.idle(90);
        scene.markAsFinished();
    }

    /**
     * Corrupt Cop (NoellesRoles start items, ShouldPunishGunShooter listener, win check; Wathe GunShootPayload,
     * GameConstants): the Corrupt Cop starts with a revolver (and the neutral master key, hidden in hand). Its shots
     * kill whoever they hit, unless a shield or psycho armour stops them, without any of an innocent shooter's
     * penalties: no gun drop, no ban on picking guns up, no death, no backfire; the revolver's 10 s cooldown applies (2
     * s in the Corrupt Cop moment). While it lives neither the passengers nor the killers can win early; it usually
     * wins as the last one alive and not swallowed (SparkWitch's Insider team and the Shadow Jester finale aside).
     * 黑警：开局自带左轮手枪（还有手持时别人看不见的中立万能钥匙）。他开枪打中谁就打死谁（护盾或疯魔护甲挡下的除外），
     * 没有好人误杀的惩罚：不掉枪、不禁止捡枪、不赔命、不走火；左轮冷却 10 秒（处决时刻 2 秒）。他活着时好人和杀手都无法
     * 提前获胜；他一般在成为最后一名没被吞下的活人时获胜（SparkWitch 内应同伙与双影谢幕除外）。
     */
    private static void corruptCop(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("role_corrupt_cop", "黑警：打谁都不受罚的左轮");
        WatheItemScenes.stage(scene);
        int color = RoleColors.of("noellesroles:corrupt_cop", 0x193264);
        ItemStack revolver = stack("wathe:revolver");
        ElementLink<ActorElement> cop = Actors.enter(scene, color, Text.literal("黑警"), new Vec3d(5.8, 1, 1.2),
                SCREEN_RIGHT, Direction.DOWN);
        Actors.hold(scene, cop, revolver);
        scene.idle(20);
        scene.overlay().showText(80)
                .text("黑警（中立）：开局自带一把左轮手枪")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        ElementLink<ActorElement> passenger = Actors.enter(scene, RoleColors.CIVILIAN, Text.literal("平民"),
                new Vec3d(1.2, 1, 5.8), SCREEN_LEFT, Direction.DOWN);
        scene.idle(10);
        Actors.walk(scene, passenger, new Vec3d(0.8, 0, -0.8), 20);
        scene.idle(25);
        scene.overlay().showControls(new Vec3d(5.8, 3.6, 1.2), Pointing.DOWN, 30).rightClick().withItem(revolver);
        scene.idle(15);
        WatheItemScenes.shoot(scene, cop, new Vec3d(5.2, 2.05, 1.8), new Vec3d(2.0, 1.9, 5.0));
        Actors.fall(scene, passenger);
        scene.idle(20);
        scene.overlay().showText(90)
                .colored(PonderPalette.GREEN)
                .text("打死好人也不受罚：不掉枪，也不会赔命")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        ElementLink<ActorElement> killer = Actors.enter(scene, RoleColors.KILLER, Text.literal("杀手"),
                new Vec3d(4.6, 1, 6.4), NORTH, Direction.DOWN);
        Actors.hold(scene, killer, stack("wathe:knife"));
        scene.overlay().showText(80)
                .text("每开一枪冷却 10 秒（处决时刻 2 秒，演示缩短了）")
                .independent();
        scene.idle(30);
        Actors.walk(scene, killer, new Vec3d(0.4, 0, -1.8), 30);
        // Facing the killer's stop at (5.0, 1, 4.6). 面向杀手停下的位置 (5.0, 1, 4.6)。
        Actors.turn(scene, cop, 13);
        scene.idle(30);
        Actors.charge(scene, killer, true);
        scene.idle(10);
        WatheItemScenes.shoot(scene, cop, new Vec3d(5.6, 2.05, 2.03), new Vec3d(5.0, 1.9, 4.6));
        Actors.fall(scene, killer);
        scene.idle(20);
        scene.overlay().showText(90)
                .colored(PonderPalette.GREEN)
                .text("杀手也照打不误：好人、杀手都可以是你的目标")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showText(90)
                .text("一般要杀到车上只剩你一人才赢，所以两边都会来找你")
                .independent();
        scene.idle(100);
        scene.markAsFinished();
    }

    /**
     * Shadow Jester pact (NoellesRoles SHADOW_ALLY receiver and client ability key, TaskComplete listener,
     * ShadowJesterPlayerComponent, ShadowJesterKnifeMixin, death listener; SparkStrength ShadowJesterAllianceMixin,
     * ShadowJesterShowdownService.enhanceAlliance): the two Shadow Jesters always see each other outlined. Each gets a
     * knife on its 4th task, which kills only the partner. Once both have their knife, one aims at the partner within 3
     * blocks and presses the ability key; the partner presses it back: the proposer becomes the Seer (knife taken, a
     * lockpick added to the inventory), the accepter the Blade (the knife now kills anyone, a derringer added), both
     * usually see the living through walls with the instinct key, and from then on when one dies the other dies too. No
     * particle or sound; onlookers only see the Seer's knife go. Unallied, a Shadow Jester whose partner dies becomes a
     * Jester.
     * 影子小丑结盟：两名影子小丑始终能看到彼此的轮廓。各自做完第 4 个任务得到一把刀，只能杀搭档。两人都拿到刀后，一方准
     * 星对准 3 格内的搭档按技能键，搭档回按一次：发起者成为影瞳（刀被收走，背包里多一把开锁器），同意者成为影刃（刀能杀
     * 任何人，背包里多一把德林加手枪），两人按住本能键一般都能隔墙看见活人，此后一人死另一人也死。没有粒子和声音，旁人
     * 只看得到影瞳手里的刀没了。未结盟时搭档一死，影子小丑就变成小丑。
     */
    private static void shadowJester(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("role_shadow_jester", "影子小丑：结下影誓");
        WatheItemScenes.stage(scene);
        int color = RoleColors.of("noellesroles:shadow_jester", 0xBE1E5A);
        ItemStack knife = stack("wathe:knife");
        Text shadowJester = Text.literal("影子小丑");
        ElementLink<ActorElement> partner = Actors.enter(scene, color, shadowJester, new Vec3d(2.0, 1, 5.0),
                SCREEN_RIGHT, Direction.DOWN);
        scene.idle(5);
        ElementLink<ActorElement> proposer = Actors.enter(scene, color, shadowJester, new Vec3d(5.8, 1, 1.2),
                SCREEN_RIGHT, Direction.DOWN);
        scene.idle(15);
        Actors.highlight(scene, partner, color, 80);
        Actors.highlight(scene, proposer, color, 80);
        scene.overlay().showText(75)
                .text("影子小丑两人一对，能隔墙看见彼此（只在你们屏幕上）")
                .independent()
                .attachKeyFrame();
        scene.idle(85);
        Actors.hold(scene, partner, knife);
        Actors.hold(scene, proposer, knife);
        scene.overlay().showText(75)
                .text("各自做满 4 个任务得到一把刀：这把刀只能杀搭档")
                .independent()
                .attachKeyFrame();
        scene.idle(80);
        Actors.walk(scene, proposer, new Vec3d(-2.0, 0, 2.0), 26);
        scene.idle(28);
        scene.overlay().showText(70)
                .text("准星对准 3 格内的搭档按技能键（默认 G），发起影誓")
                .independent()
                .attachKeyFrame();
        scene.idle(55);
        Actors.turn(scene, partner, SCREEN_LEFT);
        scene.idle(25);
        scene.overlay().showText(50)
                .text("搭档也对你按一次技能键，同盟就结成了")
                .independent()
                .attachKeyFrame();
        scene.idle(45);
        // The pact lands: the proposer's knife is taken and the accepter's turns real.
        // 结盟生效：发起者的刀被收走，同意者的刀变成真刀。
        Actors.hold(scene, proposer, ItemStack.EMPTY);
        Actors.retint(scene, proposer, color, Text.literal("影瞳"));
        Actors.retint(scene, partner, color, Text.literal("影刃"));
        scene.idle(15);
        scene.overlay().showText(80)
                .text("发起的你成为影瞳：刀被收走，背包里多了把开锁器")
                .independent()
                .attachKeyFrame();
        scene.idle(45);
        // He takes out the lockpick, which landed in a free slot. 他拿出放进空槽位的开锁器。
        Actors.hold(scene, proposer, stack("wathe:lockpick"));
        scene.idle(45);
        scene.overlay().showText(80)
                .text("同意的搭档成为影刃：刀能杀任何人，背包里多一把德林加")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        scene.overlay().showText(80)
                .text("两人都能按住本能键隔墙看见活人；此后一人死，另一人也会死")
                .independent();
        scene.idle(90);
        scene.overlay().showText(85)
                .colored(PonderPalette.RED)
                .text("没结盟时搭档一死（比如被你杀了），你就变成小丑")
                .independent();
        scene.idle(90);
        scene.markAsFinished();
    }

    /**
     * Pathogen infection (NoellesRoles ability packet, PathogenPlayerComponent, InfectedPlayerComponent,
     * NoellesrolesClient highlight; SparkStrength PathogenClientHooks): the ability key needs no aim; it infects the
     * nearest living, uninfected player within 3 blocks in line of sight, with nothing to see or hear. The Pathogen
     * sees infected players it has in sight outlined in its own colour (Virus carriers darker), never through walls. 10
     * s opening cooldown, then 20, 15, 10 or 7 s by starting player count. 10-30 s after the infection the victim
     * sneezes, a sound only (panda sneeze, volume 2) with no particles, and is told they feel unwell. Killers count
     * too: the Pathogen wins once every other living player is infected (unless the Shadow Jester finale is running).
     * 病原体感染：技能键不用瞄准，感染 3 格内、视线可及、离得最近的一名未感染活人，看不到也听不到任何动静。病原体能看到
     * 视线内已感染者身上自己颜色的轮廓（带毒者颜色更深），隔墙不显示。开局冷却 10 秒，之后按开局人数为 20、15、10 或 7
     * 秒。感染后 10～30 秒被感染者打一个喷嚏，只有声音（熊猫喷嚏声，音量 2），没有粒子，并收到身体不适的提示。杀手也要
     * 感染：其他活人全部感染时病原体获胜（双影谢幕期间除外）。
     */
    private static void pathogen(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("role_pathogen", "病原体：悄悄感染");
        WatheItemScenes.stage(scene);
        int color = RoleColors.of("noellesroles:pathogen", 0x7FFF00);
        ElementLink<ActorElement> vigilante = Actors.enter(scene, RoleColors.VIGILANTE, Text.literal("义警"),
                new Vec3d(3.0, 1, 2.0), SCREEN_LEFT, Direction.DOWN);
        ElementLink<ActorElement> killer = Actors.enter(scene, RoleColors.KILLER, Text.literal("杀手"),
                new Vec3d(1.4, 1, 4.6), FACING_CAMERA, Direction.DOWN);
        scene.idle(10);
        ElementLink<ActorElement> pathogen = Actors.enter(scene, color, Text.literal("病原体"),
                new Vec3d(6.2, 1, 0.8), SCREEN_RIGHT, Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(80)
                .text("病原体（中立）：一般把其他活人全部感染就能获胜")
                .independent()
                .attachKeyFrame();
        scene.idle(40);
        Actors.walk(scene, pathogen, new Vec3d(-1.8, 0, 0.8), 26);
        scene.idle(50);
        scene.overlay().showText(70)
                .text("走到 3 格内按技能键（默认 G），自动感染最近的未感染者")
                .independent()
                .attachKeyFrame();
        scene.idle(50);
        // The outline lasts as long as the Pathogen has the vigilante in sight. 只要病原体看得见义警，轮廓就一直在。
        Actors.highlight(scene, vigilante, color, 480);
        scene.idle(30);
        scene.overlay().showText(90)
                .text("不用瞄准，对方毫无察觉；你看得到他的绿色轮廓（隔墙不显示）")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showText(80)
                .text("冷却 7～20 秒（开局人越多越短，演示缩短了）；杀手也得感染")
                .independent();
        Actors.walk(scene, pathogen, new Vec3d(-1.8, 0, 2.0), 30);
        scene.idle(60);
        Actors.highlight(scene, killer, color, 250);
        scene.idle(30);
        scene.overlay().showText(40)
                .text("阿嚏！")
                .pointAt(new Vec3d(3.0, 2.9, 2.0))
                .placeNearTarget();
        scene.overlay().showText(90)
                .text("10～30 秒后他会打个喷嚏，附近都听得到（演示缩短了）")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showText(90)
                .text("喷嚏只有声音；他还会收到“身体不太舒服”的提示")
                .independent();
        scene.idle(100);
        scene.markAsFinished();
    }

    /**
     * Sprinting's floor particles as Entity.spawnSprintingParticles adds them every tick, which still runs for an
     * invisible player: one particle of the block under the feet, kicked back against the movement.
     * 疾跑的地面粒子，与 Entity.spawnSprintingParticles 每 tick 添加的一致（隐身的玩家照样有）：脚下方块的一个粒子，
     * 朝移动的反方向踢起。
     */
    private static void sprintDust(SceneBuilder scene, ElementLink<ActorElement> link, int ticks) {
        scene.addInstruction(new TickingInstruction(false, ticks) {
            @Nullable
            private Vec3d last;

            @Override
            protected void firstTick(PonderScene ponder) {
                super.firstTick(ponder);
                last = null;
            }

            @Override
            public void tick(PonderScene ponder) {
                super.tick(ponder);
                ActorElement actor = ponder.resolve(link);
                if (actor == null) {
                    return;
                }
                Vec3d at = actor.position();
                Vec3d velocity = last == null ? Vec3d.ZERO : at.subtract(last);
                last = at;
                BlockState floor = ponder.getWorld().getBlockState(BlockPos.ofFloored(at).down());
                if (floor.isAir()) {
                    return;
                }
                ponder.getWorld().addParticle(new BlockStateParticleEffect(ParticleTypes.BLOCK, floor),
                        at.x + (RANDOM.nextDouble() - 0.5) * 0.6, at.y + 0.1, at.z + (RANDOM.nextDouble() - 0.5) * 0.6,
                        velocity.x * -4, 1.5, velocity.z * -4);
            }
        });
    }
}
