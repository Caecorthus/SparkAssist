package dev.caecorthus.sparkassist.client.ponder;

import static dev.caecorthus.sparkassist.client.ponder.SparkPonderDemos.item;
import static dev.caecorthus.sparkassist.client.ponder.SparkPonderDemos.role;
import static dev.caecorthus.sparkassist.client.ponder.SparkPonderDemos.scene;
import static dev.caecorthus.sparkassist.client.ponder.WatheItemScenes.EAST;
import static dev.caecorthus.sparkassist.client.ponder.WatheItemScenes.SCREEN_LEFT;
import static dev.caecorthus.sparkassist.client.ponder.WatheItemScenes.SCREEN_RIGHT;
import static dev.caecorthus.sparkassist.client.ponder.WatheItemScenes.SOUTH;
import static dev.caecorthus.sparkassist.client.ponder.WatheItemScenes.WEST;
import static dev.caecorthus.sparkassist.client.ponder.WatheItemScenes.stack;

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
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.WindChargeEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

/**
 * Demos for SparkWitch's Wraith and the three good-side roles a Wraith is promoted to, opened only from the role's
 * guide page; each needs SparkWitch. A Wraith is invisible to the living (WraithPresence, WraithViewerRules), so the
 * demos draw it as an ordinary actor in its role colour and say once per demo that living players do not see it.
 * SparkWitch 冤魂及其三种好人晋升身份的演示，只从该职业的指南页打开；都需要 SparkWitch。活人看不见冤魂（WraithPresence、
 * WraithViewerRules），所以演示把它画成身份颜色的普通演员，并在每个演示里用一句字幕说明活人其实看不见它。
 */
final class SparkWitchWraithScenes {
    /** NoellesRoles' shared ability key, which SparkWitch skills reuse. SparkWitch 技能沿用的 NoellesRoles 技能键。 */
    private static final String ABILITY_KEY = "key.noellesroles.ability";
    /** A standing actor's eyes above the feet. 站立演员的眼睛离脚底的高度。 */
    private static final double EYES = 1.62;

    private SparkWitchWraithScenes() {
    }

    static void register() {
        role("sparkassist:roles/sparkwitch/wraith", List.of("sparkwitch"),
                scene("sparkwitch/wraith_cabin", SparkWitchWraithScenes::wraith));
        role("sparkassist:roles/sparkwitch/guardian_angel", List.of("sparkwitch"),
                scene("wathe/aisle", SparkWitchWraithScenes::guardianAngel));
        role("sparkassist:roles/sparkwitch/wind_spirit", List.of("sparkwitch"),
                scene("wathe/aisle", SparkWitchWraithScenes::windSpirit));
        role("sparkassist:roles/sparkwitch/vendetta", List.of("sparkwitch"),
                scene("wathe/aisle", SparkWitchWraithScenes::vendetta));

        // Items whose use a demo above already shows; holding W over the item plays that demo.
        // 上面的演示已经演示过用法的物品；在物品上按住 W 即可播放对应演示。
        item("sparkwitch:vendetta_knife", List.of("sparkwitch"),
                scene("wathe/aisle", SparkWitchWraithScenes::vendetta));
    }

    /**
     * Wraith (WraithConversion, WraithRules, WraithSettings, WraithLifecycle, WraithPresence, WraithDoorPassingMixin,
     * WraithParticipation, WraithProgression, WraithPromotionQueue, WraithPromotionRoles): a good, killer or witch
     * player who dies (not by leaving, nor by falling off the train unaided) may, by default 75% within the round's
     * quota, rise as a Wraith on the spot, the body staying behind with the pre-death role. The Wraith is invisible to
     * living players (the SparkStrength Spiritualist excepted), has no collision and passes closed doors, which stay
     * shut. Until promotion it may use only food platters, drink trays and empty beds and only eat or drink; 3 tasks
     * done as a Wraith promote it at once, waking it if asleep: good to the Wind Spirit, Guardian Angel or Vendetta,
     * killer to the Saboteur, witch to the Curser. The camera swings round once the Wraith is inside the cabin; the
     * demo shows two of the three tasks and a short sleep.
     * 冤魂：好人、杀手、魔女阵营的玩家死亡（中途退出、自己掉下火车除外）时，在本局名额内默认有 75% 概率在原地化为冤魂，
     * 尸体留在原地并显示生前身份。活人看不见冤魂（SparkStrength 的灵界行者除外），冤魂没有碰撞，能穿过关着的门，门保持
     * 关闭。晋升前只能用餐盘、饮品托盘和空床，只能吃喝；以冤魂身份完成 3 个任务立刻晋升（睡着也会被叫醒）：好人成为风精灵、
     * 守护天使或仇杀客，杀手成为破坏者，魔女成为诅咒者。冤魂进入包厢后镜头转到另一侧；演示只做了三个任务中的两个，睡觉也缩短了。
     */
    private static void wraith(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("role_wraith", "冤魂：死后继续帮阵营");
        WatheItemScenes.cabinStage(scene, util);
        Vec3d feet = new Vec3d(3.5, 1, 1.5);
        ElementLink<ActorElement> civilian = Actors.enter(scene, RoleColors.CIVILIAN, Text.literal("平民"), feet,
                WEST, Direction.DOWN);
        ElementLink<ActorElement> killer = Actors.enter(scene, RoleColors.KILLER, Text.literal("杀手"),
                new Vec3d(6.3, 1, 1.5), WEST, Direction.DOWN);
        Actors.hold(scene, killer, stack("wathe:knife"));
        scene.idle(15);
        Actors.walk(scene, killer, new Vec3d(-0.7, 0, 0), 12);
        scene.idle(14);
        Actors.charge(scene, killer, true);
        scene.idle(14);
        Actors.charge(scene, killer, false);
        Actors.swing(scene, killer);
        Actors.fall(scene, civilian);
        scene.idle(25);
        ElementLink<ActorElement> wraith = Actors.enter(scene, RoleColors.of("sparkwitch:wraith", 0x79C7D4),
                Text.literal("冤魂"), feet, WEST, Direction.UP);
        scene.idle(5);
        scene.overlay().showText(74)
                .text("好人、杀手、魔女阵营的人死后，有机会化为冤魂（默认 75%%）")
                .independent()
                .attachKeyFrame();
        scene.idle(85);
        Actors.turn(scene, killer, EAST);
        scene.overlay().showText(77)
                .text("一般就在原地化为冤魂；尸体留下，仍显示你生前的身份")
                .independent();
        scene.idle(10);
        Actors.leave(scene, killer, Direction.UP);
        scene.idle(75);
        scene.overlay().showText(80)
                .text("演示里看得到你，活人其实看不见你（灵界行者除外）")
                .independent()
                .attachKeyFrame();
        scene.idle(85);
        Actors.turn(scene, wraith, SOUTH);
        scene.idle(8);
        Actors.walk(scene, wraith, new Vec3d(0, 0, 3.3), 50);
        scene.overlay().showText(65)
                .text("你不会和人相撞，还能直接穿过关着的门（门不会打开）")
                .independent()
                .attachKeyFrame();
        scene.idle(55);
        scene.rotateCameraY(180);
        scene.idle(20);
        Actors.walk(scene, wraith, new Vec3d(1.8, 0, -0.3), 25);
        scene.overlay().showText(80)
                .text("做任务就能晋升；晋升前只能用餐盘、饮品托盘和空床")
                .independent()
                .attachKeyFrame();
        scene.idle(27);
        Actors.turn(scene, wraith, EAST);
        scene.idle(8);
        scene.overlay().showControls(new Vec3d(6.5, 2.4, 4.5), Pointing.DOWN, 30).rightClick();
        scene.idle(10);
        Actors.swing(scene, wraith);
        Actors.hold(scene, wraith, stack("wathe:mojito"));
        scene.idle(10);
        Actors.charge(scene, wraith, true);
        scene.idle(30);
        Actors.charge(scene, wraith, false);
        Actors.hold(scene, wraith, ItemStack.EMPTY);
        scene.idle(10);
        Actors.walk(scene, wraith, new Vec3d(-2.7, 0, -0.1), 35);
        scene.idle(37);
        Actors.lieDown(scene, wraith, util.grid().at(1, 1, 5), Direction.WEST);
        scene.overlay().showText(77)
                .text("喝饮品、吃东西、睡觉都能完成对应任务（演示省略了一个、缩短了时间）")
                .independent()
                .attachKeyFrame();
        scene.idle(85);
        // Promotion wakes a sleeping Wraith (WraithLifecycle.promotePlayer). 晋升会叫醒正在睡觉的冤魂。
        Actors.getUp(scene, wraith);
        Actors.retint(scene, wraith, RoleColors.of("sparkwitch:guardian_angel", 0xF0D77A), Text.literal("守护天使"));
        scene.overlay().showText(97)
                .colored(PonderPalette.GREEN)
                .text("做满 3 个任务立刻晋升：好人阵营随机成为风精灵、守护天使或仇杀客")
                .independent()
                .attachKeyFrame();
        scene.idle(105);
        scene.overlay().showText(80)
                .text("杀手阵营晋升为破坏者，魔女阵营晋升为诅咒者；晋升后一般仍然隐身")
                .independent();
        scene.idle(85);
        scene.markAsFinished();
    }

    /**
     * Guardian Angel (GuardianAngelRules, GuardianAngelFeatureService, GuardianAngelTargeting, GuardianAngelState,
     * GuardianAngelClientHooks; Wathe KnifeStabPayload): the ability key, 60 s after promotion, puts a 10 s Guardian
     * Shield on the living player aimed at within 3 blocks in sight; its effect shows no particles or icon and the
     * target is told nothing; while it lasts the target is outlined to the angel through walls. The shield cancels the
     * next lethal kill except a few death reasons, plays Wathe's psycho-armour clang to everyone nearby and is gone.
     * The stab still swings. 90 s cooldown from the cast.
     * 守护天使：晋升 60 秒后，按技能键给 3 格内准星对准、看得见的活人套上 10 秒守护护盾；该效果不显示粒子和图标，目标
     * 也收不到提示；护盾期间目标隔墙对天使高亮。护盾抵消下一次致命击杀（少数死因除外），向附近所有人播放 Wathe 疯魔护甲的
     * 碰撞声，随即消失。出刀照样挥手。冷却 90 秒，从施放时算起。
     */
    private static void guardianAngel(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("role_guardian_angel", "守护天使：替队友挡下一刀");
        WatheItemScenes.stage(scene);
        int gold = RoleColors.of("sparkwitch:guardian_angel", 0xF0D77A);
        ElementLink<ActorElement> civilian = Actors.enter(scene, RoleColors.CIVILIAN, Text.literal("平民"),
                new Vec3d(2.2, 1, 4.8), SCREEN_RIGHT, Direction.DOWN);
        scene.idle(5);
        Vec3d feet = new Vec3d(0.7, 1, 6.3);
        Actors.enter(scene, gold, Text.literal("守护天使"), feet, SCREEN_LEFT, Direction.UP);
        scene.idle(20);
        scene.overlay().showText(80)
                .text("好人冤魂可能晋升为守护天使，能悄悄给活人套上护盾")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        scene.overlay().showText(80)
                .text("演示里看得到你，活人其实看不见你（灵界行者除外）")
                .independent();
        scene.idle(90);
        scene.overlay().showControls(feet.add(0, 2.6, 0), Pointing.DOWN, 40).showing(key(ABILITY_KEY, "G"));
        scene.overlay().showText(67)
                .text("对准 3 格内看得见的活人，按技能键（默认 G）")
                .independent()
                .attachKeyFrame();
        scene.idle(20);
        // The shield (10 s) and its outline last until the stab below, 141 ticks on, pops them.
        // 护盾（10 秒）与轮廓一直持续到 141 tick 后的那一刀，随之消失。
        Actors.highlight(scene, civilian, gold, 141);
        scene.idle(55);
        scene.overlay().showText(80)
                .text("他得到 10 秒护盾，期间隔墙对你高亮（金框示意）")
                .independent()
                .attachKeyFrame();
        ElementLink<ActorElement> killer = Actors.enter(scene, RoleColors.KILLER, Text.literal("杀手"),
                new Vec3d(5.8, 1, 1.2), SCREEN_RIGHT, Direction.DOWN);
        Actors.hold(scene, killer, stack("wathe:knife"));
        scene.idle(40);
        Actors.walk(scene, killer, new Vec3d(-2.2, 0, 2.2), 26);
        scene.idle(30);
        Actors.charge(scene, killer, true);
        scene.idle(16);
        Actors.charge(scene, killer, false);
        Actors.swing(scene, killer);
        scene.idle(10);
        scene.overlay().showText(88)
                .colored(PonderPalette.GREEN)
                .text("这一刀被护盾挡下：他没死，附近会响起一声盔甲碰撞声")
                .independent()
                .attachKeyFrame();
        Actors.turn(scene, civilian, SOUTH);
        scene.idle(6);
        Actors.walk(scene, civilian, new Vec3d(-0.4, 0, 1.8), 14);
        scene.idle(20);
        Actors.leave(scene, civilian, Direction.UP);
        scene.idle(70);
        scene.overlay().showText(80)
                .text("护盾本身没有任何特效，他自己事先也收不到提示")
                .independent();
        scene.idle(90);
        scene.overlay().showText(90)
                .text("护盾挡一次就没了；冷却 90 秒，有几种死法挡不住（见指南）")
                .independent();
        scene.idle(100);
        scene.markAsFinished();
    }

    /**
     * Wind Spirit (WindSpiritFeatureService, WindSpiritRules, WindSpiritWindChargeMixin, WraithParticipationRules;
     * vanilla WindChargeItem, WindChargeEntity, GustEmitterParticle; Wathe PlayerEntityMixin.applyDamage): permanent
     * Speed II and endless stamina; the shop sells vanilla wind charges at 50 coins, no stock limit. A right-click
     * throws one from the eyes at 1.5 blocks a tick; on hitting a living player it bursts where it was at the start of
     * that tick (a wind burst of power 1.2: the small gust emitter's GUST particles, seen by everyone), blowing back
     * everyone near, who may be blown off the train; Wathe cancels the damage. A shove does not end a knife charge.
     * The thrower stays unseen; the charge triggers no blocks.
     * 风精灵：永久速度 II、体力无限；商店以 50 金币出售原版风弹，不限购。右键从眼睛处以每 tick 1.5 格扔出；命中活人后在该
     * tick 开始时的位置爆开（威力 1.2 的风爆：小型阵风发射器放出的 GUST 粒子，人人可见），把附近的人都吹开，可能吹下火车；
     * 伤害被 Wathe 取消。被吹开不会打断刀的蓄力。扔的人仍然看不见；风弹不触发任何方块。
     */
    private static void windSpirit(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("role_wind_spirit", "风精灵：用风弹击退杀手");
        WatheItemScenes.stage(scene);
        ItemStack windCharge = stack("minecraft:wind_charge");
        float towardsFarCorner = -45;
        float towardsCamera = 135;
        ElementLink<ActorElement> civilian = Actors.enter(scene, RoleColors.CIVILIAN, Text.literal("平民"),
                new Vec3d(2.0, 1, 2.0), towardsFarCorner, Direction.DOWN);
        scene.idle(5);
        Vec3d feet = new Vec3d(6.2, 1, 2.2);
        ElementLink<ActorElement> spirit = Actors.enter(scene, RoleColors.of("sparkwitch:wind_spirit", 0x59D8E6),
                Text.literal("风精灵"), feet, SCREEN_RIGHT, Direction.UP);
        scene.idle(20);
        scene.overlay().showText(80)
                .text("好人冤魂可能晋升为风精灵：永久速度 II，体力无限")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        scene.overlay().showText(80)
                .text("演示里看得到你，活人其实看不见你（灵界行者除外）")
                .independent();
        scene.idle(90);
        Actors.hold(scene, spirit, windCharge);
        scene.overlay().showText(80)
                .text("在商店花 50 金币买风弹，不限购")
                .independent()
                .attachKeyFrame();
        ElementLink<ActorElement> killer = Actors.enter(scene, RoleColors.KILLER, Text.literal("杀手"),
                new Vec3d(5.8, 1, 5.8), towardsCamera, Direction.DOWN);
        Actors.hold(scene, killer, stack("wathe:knife"));
        scene.idle(30);
        Actors.charge(scene, killer, true);
        Actors.walk(scene, killer, new Vec3d(-1.6, 0, -1.6), 40);
        scene.idle(30);
        scene.overlay().showControls(feet.add(0, 2.6, 0), Pointing.DOWN, 30).rightClick().withItem(windCharge);
        scene.idle(10);
        Actors.swing(scene, spirit);
        Vec3d eyes = feet.add(0, EYES, 0);
        // It bursts where it was at the start of the hitting tick, short of the killer. 在命中那一 tick 开始时所在处爆开，离杀手还差一点。
        Vec3d burst = new Vec3d(4.9, eyes.y, 3.5);
        throwWindCharge(scene, eyes, burst, 2);
        scene.idle(2);
        gust(scene, burst);
        Actors.slide(scene, killer, new Vec3d(-2.1, 0, 2.1), 12);
        Actors.slide(scene, killer, new Vec3d(0, 0.7, 0), 5);
        scene.idle(5);
        Actors.slide(scene, killer, new Vec3d(0, -0.7, 0), 7);
        scene.idle(10);
        scene.overlay().showText(80)
                .colored(PonderPalette.GREEN)
                .text("右键扔出：风弹打中就爆开，把附近的人吹开；不伤人，但可能把人吹下车")
                .independent()
                .attachKeyFrame();
        scene.idle(20);
        Actors.turn(scene, civilian, SCREEN_LEFT);
        scene.idle(5);
        Actors.walk(scene, civilian, new Vec3d(2.4, 0, -1.2), 20);
        scene.idle(25);
        Actors.leave(scene, civilian, Direction.UP);
        // Out of reach, he lets go without a target. 够不着了，松开也没有目标。
        Actors.charge(scene, killer, false);
        scene.idle(40);
        scene.overlay().showText(80)
                .text("旁人看得到风弹和气流，却一般看不出是谁扔的")
                .independent();
        scene.idle(90);
        scene.overlay().showText(80)
                .text("趁杀手贴近队友之前出手，给队友争取逃跑的时间")
                .independent();
        scene.idle(90);
        scene.markAsFinished();
    }

    /**
     * Vendetta (VendettaKnifeItem, VendettaKnifeService, VendettaKnifeRules, VendettaPresentationRules,
     * VendettaClientPresentation, VendettaTerminalService, WraithViewerRules): promoted only when the player's killer is
     * known, alive and not themself. Living players do not see the Vendetta, but the bound killer sees it and its
     * knife and can kill it. The killer is outlined red to the Vendetta within 8 blocks through walls, and fully for
     * 5 s every 30 s; the screen greys more the closer the killer is. The Vendetta Knife (Wathe's knife, SPEAR pose)
     * held at least 10 ticks and released kills only the bound killer within 3 blocks in sight; a confirmed kill swings
     * and ends the Vendetta, who becomes a spectator in the same tick.
     * 仇杀客：只在杀死你的人已知、仍存活且不是你自己时晋升而来。活人看不见仇杀客，但绑定的凶手看得见它和它的刀，也能杀死
     * 它。凶手在 8 格内隔墙对仇杀客显示红色描边，每 30 秒还会完整透视 5 秒；离凶手越近屏幕越灰。复仇刀（Wathe 刀的模型，
     * 掷矛姿势）按住至少 10 tick 后松开，只能杀死 3 格内看得见的绑定凶手；确认击杀后挥手，仇杀客结束，同一 tick 变为旁观者。
     */
    private static void vendetta(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("role_vendetta", "仇杀客：向凶手复仇");
        WatheItemScenes.stage(scene);
        ItemStack knife = stack("sparkwitch:vendetta_knife");
        ElementLink<ActorElement> killer = Actors.enter(scene, RoleColors.KILLER, Text.literal("杀手"),
                new Vec3d(2.2, 1, 4.8), SCREEN_RIGHT, Direction.DOWN);
        scene.idle(5);
        ElementLink<ActorElement> vendetta = Actors.enter(scene, RoleColors.of("sparkwitch:vendetta", 0xE34B5F),
                Text.literal("仇杀客"), new Vec3d(5.8, 1, 1.2), SCREEN_RIGHT, Direction.UP);
        Actors.hold(scene, vendetta, knife);
        scene.idle(20);
        scene.overlay().showText(80)
                .text("被杀的好人化为冤魂后，可能晋升为仇杀客，拿到复仇刀")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        scene.overlay().showText(90)
                .text("演示里看得到你；活人一般看不见你，但凶手看得见你和刀，也能杀死你")
                .independent();
        scene.idle(100);
        // Shown until the killer dies below. 一直显示到下面凶手死亡。
        Actors.highlight(scene, killer, 0xFF0000, 237);
        scene.overlay().showText(80)
                .text("凶手在 8 格内会隔墙显示红色描边（方框示意）")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        scene.overlay().showText(80)
                .text("每隔 30 秒还会透视凶手 5 秒；离凶手越近，你眼中的世界越灰")
                .independent();
        scene.idle(90);
        Actors.walk(scene, vendetta, new Vec3d(-1.8, 0, 1.8), 30);
        scene.idle(32);
        scene.overlay().showControls(new Vec3d(4.0, 3.6, 3.0), Pointing.DOWN, 30).rightClick().withItem(knife);
        scene.overlay().showText(30)
                .text("从背后靠近，按住右键至少 0.5 秒……")
                .independent()
                .attachKeyFrame();
        Actors.charge(scene, vendetta, true);
        scene.idle(25);
        Actors.charge(scene, vendetta, false);
        Actors.swing(scene, vendetta);
        Actors.fall(scene, killer);
        // The Vendetta turns spectator in the same tick (VendettaTerminalService). 仇杀客在同一 tick 变为旁观者。
        Actors.leave(scene, vendetta, Direction.UP);
        scene.idle(15);
        scene.overlay().showText(80)
                .colored(PonderPalette.GREEN)
                .text("……松开刺中 3 格内的凶手；复仇刀只对凶手有效")
                .independent()
                .attachKeyFrame();
        scene.idle(90);
        scene.overlay().showText(80)
                .text("复仇成功后，你就变回旁观者")
                .independent();
        scene.idle(90);
        scene.markAsFinished();
    }

    /**
     * A wind charge in flight from {@code from} to {@code to} over {@code ticks}, drawn by the vanilla wind charge
     * renderer; gone on arrival.
     * 从 from 飞到 to、历时 ticks 的风弹，由原版风弹渲染器绘制；到达即消失。
     */
    private static void throwWindCharge(SceneBuilder scene, Vec3d from, Vec3d to, int ticks) {
        WindChargeFlight flight = new WindChargeFlight(from, to, ticks);
        scene.addInstruction(ponder -> {
            flight.setVisible(true);
            flight.setFade(1);
            ponder.addElement(flight);
        });
    }

    /**
     * The burst everyone sees where a wind charge pops: the small gust emitter (GustEmitterParticle: deviation 1,
     * lifetime 3, interval 2) puts out 3 gusts on its first tick, each up to a block off, and is gone before its next.
     * 风弹爆开处人人看得到的气流：小型阵风发射器（偏移 1、寿命 3、间隔 2）在第一个 tick 放出 3 团阵风，各自至多偏离 1 格，
     * 下一次放出之前就已消失。
     */
    private static void gust(SceneBuilder scene, Vec3d at) {
        scene.effects().emitParticles(at, (world, x, y, z) -> {
            for (int i = 0; i < 3; i++) {
                world.addParticle(ParticleTypes.GUST, x + world.random.nextDouble() - world.random.nextDouble(),
                        y + world.random.nextDouble() - world.random.nextDouble(),
                        z + world.random.nextDouble() - world.random.nextDouble(), 0, 0, 0);
            }
        }, 1, 1);
    }

    /**
     * A key cap for a control hint, labelled with the key bound to {@code keyId}, or {@code fallback} without one:
     * Ponder's own hints draw only mouse buttons.
     * 控制提示里的键帽，标着 keyId 绑定的按键，没有绑定时显示 fallback：Ponder 自带的提示只会画鼠标按键。
     */
    private static ScreenElement key(String keyId, String fallback) {
        return (graphics, x, y) -> {
            TextRenderer font = MinecraftClient.getInstance().textRenderer;
            Text label = Text.literal(fallback);
            for (KeyBinding binding : MinecraftClient.getInstance().options.allKeys) {
                if (binding.getTranslationKey().equals(keyId)) {
                    label = binding.getBoundKeyLocalizedText();
                }
            }
            int width = Math.max(14, font.getWidth(label) + 6);
            graphics.fill(x + 1, y + 1, x + 1 + width, y + 15, 0xFFA0A0A0);
            graphics.fill(x + 2, y + 2, x + width, y + 13, 0xFF3A3A3A);
            graphics.drawText(font, label, x + 1 + (width - font.getWidth(label)) / 2, y + 4, 0xFFFFFFFF, false);
        };
    }

    /**
     * A wind charge on a straight, gravity-free line, as the vanilla projectile flies, drawn through the entity
     * renderer with a client-side WindChargeEntity so its swirling model matches the game.
     * 沿直线、无重力飞行的风弹（与原版投射物一致），借助客户端 WindChargeEntity 交给实体渲染器绘制，旋转的模型与游戏一致。
     */
    private static final class WindChargeFlight extends AnimatedSceneElementBase {
        private final Vec3d from;
        private final Vec3d to;
        private final int ticks;
        private int age;
        @Nullable
        private WindChargeEntity charge;
        @Nullable
        private ClientWorld chargeWorld;

        private WindChargeFlight(Vec3d from, Vec3d to, int ticks) {
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
            WindChargeEntity entity = entity();
            if (fade <= 0.01f || time >= ticks || entity == null) {
                return;
            }
            Vec3d at = from.lerp(to, time / ticks);
            entity.age = age;
            EntityRenderDispatcher dispatcher = MinecraftClient.getInstance().getEntityRenderDispatcher();
            dispatcher.setRenderShadows(false);
            dispatcher.render(entity, at.x, at.y, at.z, 0, pt, graphics.getMatrices(), buffer,
                    lightCoordsFromFade(fade));
            dispatcher.setRenderShadows(MinecraftClient.getInstance().options.getEntityShadows().getValue());
        }

        /** The entity drawn, rebuilt when the client world changes. 绘制用的实体，客户端世界变化时重建。 */
        @Nullable
        private WindChargeEntity entity() {
            ClientWorld world = MinecraftClient.getInstance().world;
            if (world == null) {
                return null;
            }
            if (charge == null || chargeWorld != world) {
                charge = new WindChargeEntity(EntityType.WIND_CHARGE, world);
                chargeWorld = world;
            }
            return charge;
        }
    }
}
