package dev.caecorthus.sparkassist.client.ponder;

import static dev.caecorthus.sparkassist.client.ponder.SparkPonderDemos.item;
import static dev.caecorthus.sparkassist.client.ponder.SparkPonderDemos.role;
import static dev.caecorthus.sparkassist.client.ponder.SparkPonderDemos.scene;
import static dev.caecorthus.sparkassist.client.ponder.WatheItemScenes.SCREEN_LEFT;
import static dev.caecorthus.sparkassist.client.ponder.WatheItemScenes.SCREEN_RIGHT;
import static dev.caecorthus.sparkassist.client.ponder.WatheItemScenes.SOUTH;
import static dev.caecorthus.sparkassist.client.ponder.WatheItemScenes.WEST;
import static dev.caecorthus.sparkassist.client.ponder.WatheItemScenes.stack;

import dev.caecorthus.sparkassist.ponder.ActorMovement;
import java.lang.reflect.Field;
import java.util.List;
import java.util.function.Consumer;
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
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;

/**
 * Demos for the NoellesRoles Party Animal's SparkStrength kit: the Party Balloon and the summoned Partygoer, opened
 * from the role's guide page (the balloon scene also by holding W over the balloon); they need NoellesRoles and
 * SparkStrength, and the Partygoer scene also SparkWitch, whose Role Skill 2 key summons it. The Partygoer is SparkStrength's own entity drawn by its own renderer, and the player it carries rides
 * it, so SparkStrength's carry pose applies. Its brain runs only on the server, so the scenes walk it along the route
 * it would plan. It hunts and carries several times faster than shown; the captions say the demo is slowed.
 * NoellesRoles 派对狂的 SparkStrength 技能演示：派对气球与召唤出来的派对客，从该职业的指南页打开（气球场景也可在气球上
 * 按住 W 播放）；需要 NoellesRoles 与 SparkStrength，派对客场景还需要 SparkWitch（用它的“职业技能 2”键召唤）。派对客是 SparkStrength 自己的实体，由它自己的渲染器绘制，被扛的玩家
 * 真的骑在它身上，所以套用的是 SparkStrength 的扛人姿势。它的大脑只在服务端运行，所以场景按它会规划的路线移动它。它追人
 * 和扛人比演示快好几倍，字幕会注明演示放慢了。
 */
final class PartyAnimalScenes {
    private static final List<String> NOELLES_STRENGTH = List.of("noellesroles", "sparkstrength");
    private static final Random RANDOM = Random.create();
    private static final int PARTY_ANIMAL = RoleColors.of("noellesroles:party_animal", 0xFF5AC8);
    /** PartyBalloonRules.HIGHLIGHT_COLOR, also the room picker's destination accent. 气球描边色，也是选房界面的终点色。 */
    private static final int BALLOON_YELLOW = 0xFFD84A;
    /** PartygoerRoomScreen.PARTY: the spawn column's accent. 选房界面出生房间一栏的强调色。 */
    private static final int SPAWN_PINK = 0xFF5AC8;
    /** The near cabin's door (screen-right) and the far one's (stages_party.py). 近处客房（屏幕右侧）与远处客房的门。 */
    private static final BlockPos NEAR_DOOR = new BlockPos(2, 1, 3);
    private static final BlockPos FAR_DOOR = new BlockPos(6, 1, 3);
    private static final Box NEAR_CABIN = new Box(0.05, 1, 4.05, 3.95, 3, 8.95);
    private static final Box FAR_CABIN = new Box(5.05, 1, 4.05, 8.95, 3, 8.95);

    private PartyAnimalScenes() {
    }

    static void register() {
        // The Partygoer is summoned with SparkWitch's Role Skill 2 key; without SparkWitch the skill has no key
        // (PartygoerClientHooks), so its scene is added only when SparkWitch is loaded.
        // 派对客用 SparkWitch 的“职业技能 2”键召唤；没有 SparkWitch 时这个技能没有按键（PartygoerClientHooks），所以仅在
        // 加载 SparkWitch 时追加它的场景。
        if (FabricLoader.getInstance().isModLoaded("sparkwitch")) {
            role("sparkassist:roles/noellesroles/party_animal", NOELLES_STRENGTH,
                    scene("noellesroles/party_cabins", PartyAnimalScenes::balloon),
                    scene("noellesroles/party_cabins", PartyAnimalScenes::partygoer));
        } else {
            role("sparkassist:roles/noellesroles/party_animal", NOELLES_STRENGTH,
                    scene("noellesroles/party_cabins", PartyAnimalScenes::balloon));
        }
        item("sparkstrength:party_balloon", NOELLES_STRENGTH,
                scene("noellesroles/party_cabins", PartyAnimalScenes::balloon));
    }

    /**
     * Party Balloon (PartyBalloonItem, PartyBalloonService, PartyBalloonRules, PartyBalloonClientHooks; NoellesRoles
     * hidden equipment via SparkStrength's HiddenEquipmentHelperMixin): 25 coins in the Party Animal's shop only,
     * stacks to 16, hidden from others while held. Right-click a player within 3 blocks (eye to hitbox) in sight: one
     * balloon is used and the target is marked for the round, with no message, sound or item (only the giver hears a
     * soft pop). Refused, keeping the balloon, for the killer side, an Undercover before stage 2, or someone already
     * marked. A living Party Animal usually sees marked players in an always-on yellow outline through walls (not an
     * invisible player, a Survival Master hidden behind cover or a stage-2 Undercover, nor through vetoes such as a
     * blackout); the mark lasts until the target dies or the round ends, and marked players are Partygoer prey.
     * 派对气球：只有派对狂的商店卖，25 金币，最多叠 16 个，拿在手里别人看不见。右键 3 格内（眼睛到碰撞箱）、在视线里的玩家：
     * 用掉一个气球，目标本局被标记，没有消息、声音或物品（只有塞气球的人听到一声轻响）。杀手一方、二阶段前的卧底或已被标记的人
     * 会被拒绝，气球不会用掉。存活的派对狂一般能常驻隔墙看到被标记玩家的黄色描边（隐身的人、躲在掩体后的生存大师、二阶段的
     * 卧底除外，熄灯等否决也会挡住）；标记持续到目标死亡或本局结束，被标记的人是派对客的猎物。
     */
    private static void balloon(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("role_party_animal_balloon", "派对狂：派对气球");
        stage(scene, util);
        ItemStack balloon = stack("sparkstrength:party_balloon");
        Vec3d guestFeet = new Vec3d(2.5, 1, 1.8);
        ElementLink<ActorElement> guest = Actors.enter(scene, RoleColors.CIVILIAN, Text.literal("平民"), guestFeet,
                SOUTH, Direction.DOWN);
        Actors.hold(scene, guest, stack("wathe:key"));
        scene.idle(10);
        ElementLink<ActorElement> host = Actors.enter(scene, PARTY_ANIMAL, Text.literal("派对狂"),
                new Vec3d(7.6, 1, 1.2), WEST, Direction.DOWN);
        Actors.hold(scene, host, balloon);
        scene.idle(15);
        scene.overlay().showText(90)
                .text("【派对气球】只有派对狂的商店卖，25 金币一个，可以多次购买")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showText(70)
                .text("气球拿在手里，其他人看不见（演示里画了出来）")
                .independent();
        Actors.walk(scene, host, new Vec3d(-2.6, 0, 0.2), 30);
        scene.idle(32);
        Actors.turn(scene, host, yawTowards(new Vec3d(5.0, 1, 1.4), guestFeet));
        scene.idle(48);
        scene.overlay().showControls(guestFeet.add(0, 2.6, 0), Pointing.DOWN, 30).rightClick().withItem(balloon);
        scene.overlay().showText(80)
                .text("右键 3 格内、在你视线里的玩家，把气球偷偷塞给对方")
                .independent()
                .attachKeyFrame();
        scene.idle(10);
        Actors.swing(scene, host);
        // The outline stays for the rest of the scene. 描边一直持续到场景结束。
        Actors.highlight(scene, guest, BALLOON_YELLOW, 600);
        scene.idle(80);
        scene.overlay().showText(90)
                .text("对方收不到任何提示，也听不到声音；塞成功才会用掉一个")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showText(100)
                .colored(PonderPalette.GREEN)
                .text("只要你活着，一般就能一直隔墙看到对方的黄色描边（只在你屏幕上）")
                .independent()
                .attachKeyFrame();
        scene.idle(40);
        Actors.swing(scene, guest);
        WatheItemScenes.openDoor(scene, NEAR_DOOR, true);
        scene.idle(5);
        Actors.walk(scene, guest, new Vec3d(0, 0, 3.6), 36);
        scene.idle(65);
        scene.overlay().showText(90)
                .text("进了房间也一样：描边一直亮到本局结束或对方死亡")
                .independent()
                .attachKeyFrame();
        scene.idle(30);
        WatheItemScenes.openDoor(scene, NEAR_DOOR, false);
        scene.idle(70);
        scene.overlay().showText(90)
                .text("被塞了气球的人，就成了派对客要抓的目标")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showText(100)
                .colored(PonderPalette.RED)
                .text("杀手阵营、还没戴过面具的卧底和已经被塞过的人都塞不了，气球不会用掉")
                .independent();
        scene.idle(110);
        scene.markAsFinished();
    }

    /**
     * Partygoer (PartyAnimalRules, PartygoerSkillService, PartygoerRoomScreen, PartygoerService, PartygoerBrain,
     * PartygoerWeaponHits, PartygoerCorpseService): unlocked by 2 tasks; the second skill key opens the room picker
     * (rooms = keyed cabin doors), and summoning costs 50 coins, undiscounted. It appears at the spawn room's centre
     * with 20 poof particles and a loud clip, hunts the prey nearest by path at about Speed II, pushing closed room
     * doors 2-3 cells ahead open, and grabs within 1.1 blocks: the prey rides on its arms while it runs for the
     * destination room at about Speed IV. Anything that can kill a player kills it in one hit; its passenger is set
     * down at the nearest standable cell, the killer (not its summoner) gets 25 coins and 20% sanity, no gun is
     * dropped, and it leaves a Wathe body in its own skin where it stood. 60 s cooldown once it is gone.
     * Here the near cabin is the spawn room, the far one the destination; a Vigilante shoots it in the back, so it is
     * nearer than the player it carries in front (nearest wins).
     * 派对客：做完 2 个任务解锁；第二技能键打开选房界面（房间 = 带钥匙的客房门），召唤花 50 金币、不打折。它在出生房间中心
     * 登场，冒出 20 个烟雾粒子并发出响亮的音效，以约速度 II 追按路程最近的猎物，前方 2~3 格内关着的房门会被推开，1.1 格内
     * 抓住：猎物骑在它伸出的双臂上，它以约速度 IV 跑向终点房间。任何能杀死玩家的东西都能一击打死它；被扛的人被放到最近的可
     * 站立格，击杀者（召唤者除外）得 25 金币和 20% 理智，不会掉枪，原地留下一具派对客皮肤的 Wathe 尸体。离场后冷却 60 秒。
     * 演示中近处客房是出生房间，远处是终点房间；义警从背后开枪，所以派对客比它身前扛着的人更近（最近者命中）。
     */
    private static void partygoer(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("role_party_animal_partygoer", "派对狂：召唤派对客");
        stage(scene, util);
        Vec3d preyFeet = new Vec3d(1.2, 1, 1.0);
        ElementLink<ActorElement> prey = Actors.enter(scene, RoleColors.CIVILIAN, Text.literal("平民"), preyFeet,
                SCREEN_RIGHT, Direction.DOWN);
        Vec3d vigilanteFeet = new Vec3d(0.5, 1, 2.4);
        ElementLink<ActorElement> vigilante = Actors.enter(scene, RoleColors.VIGILANTE, Text.literal("义警"),
                vigilanteFeet, SCREEN_LEFT, Direction.DOWN);
        Actors.hold(scene, vigilante, stack("wathe:revolver"));
        scene.idle(10);
        ElementLink<ActorElement> host = Actors.enter(scene, PARTY_ANIMAL, Text.literal("派对狂"),
                new Vec3d(7.6, 1, 1.0), SCREEN_RIGHT, Direction.DOWN);
        scene.idle(15);
        // Balloon-marked: yellow on the Party Animal's screen for the rest of the scene, carried or not.
        // 被塞了气球：此后整个场景在派对狂屏幕上都是黄色，被扛着时也一样。
        ElementLink<Partygoer> goer = new ElementLinkImpl<>(Partygoer.class);
        // 1289 ticks: to the end of the scene. 1289 tick：直到场景结束。
        scene.addInstruction(new MarkOutline(prey, goer, BALLOON_YELLOW, 1289));
        scene.overlay().showText(90)
                .text("做完 2 个任务，派对狂就能召唤派对客")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showText(90)
                .text("派对客只抓目标：正在变声、或被塞了【派对气球】的人")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        frame(scene, NEAR_CABIN, SPAWN_PINK, 220);
        frame(scene, FAR_CABIN, BALLOON_YELLOW, 220);
        scene.overlay().showText(100)
                .text("按第二技能键（默认 N）选出生房间（粉框）和终点房间（黄框）")
                .independent()
                .attachKeyFrame();
        scene.idle(110);
        scene.overlay().showText(100)
                .text("召唤花 50 金币，不打折；目标不用你选，它去追离出生房间最近的那个")
                .independent();
        scene.idle(110);

        // S: the summon. Hunting is slowed to 0.05 blocks a tick (Speed II is about 0.3).
        // S：召唤。追人放慢到每 tick 0.05 格（速度 II 约为 0.3）。
        Vec3d spawn = new Vec3d(2.5, 1, 6.5);
        poof(scene, spawn);
        summon(scene, goer, spawn, yawTowards(spawn, preyFeet));
        // The door is within its three-cell lookahead from the start, so it opens at once.
        // 门从一开始就在它前方三格的探查范围内，所以立刻被推开。
        WatheItemScenes.openDoor(scene, NEAR_DOOR, true);
        scene.overlay().showText(46)
                .text("点“召唤”：派对客在出生房间登场，声音很响")
                .independent()
                .attachKeyFrame();
        scene.idle(8);
        walk(scene, goer, new Vec3d(0, 0, -1.0), 20);
        scene.idle(7);
        Actors.leave(scene, host, Direction.UP);
        scene.idle(13);
        walk(scene, goer, new Vec3d(0, 0, -2.0), 40);
        scene.idle(28);
        scene.overlay().showText(40)
                .text("挡路的房门它直接推开（演示放慢）")
                .independent()
                .attachKeyFrame();
        scene.idle(12);
        walk(scene, goer, new Vec3d(-0.6, 0, -1.8), 38);
        scene.idle(38);

        // S+106: within 1.1 blocks of the prey: the grab. S+106：离猎物 1.1 格以内，抓住。
        Actors.vanish(scene, prey);
        carry(scene, goer, RoleColors.CIVILIAN);
        scene.overlay().showText(50)
                .colored(PonderPalette.RED)
                .text("贴到目标身边就把人扛起来，扛向终点房间（演示放慢）")
                .independent()
                .attachKeyFrame();
        Vec3d fall = new Vec3d(4.8, 1, 2.2);
        float away = yawTowards(new Vec3d(1.9, 1, 1.7), fall);
        turn(scene, goer, away);
        scene.idle(4);
        // Carrying is slowed to about 0.06 blocks a tick (Speed IV is about 0.39); it reaches `fall` as the shot lands.
        // 扛人放慢到每 tick 约 0.06 格（速度 IV 约 0.39）；中枪时正好走到 fall。
        walk(scene, goer, fall.subtract(1.9, 1, 1.7), 49);
        scene.idle(10);
        WatheItemScenes.openDoor(scene, NEAR_DOOR, false);
        scene.idle(5);
        float aim = yawTowards(vigilanteFeet, fall);
        Actors.turn(scene, vigilante, aim);
        scene.idle(25);
        // The far door is now within three cells of its path. 远处的门进入它前方三格之内。
        WatheItemScenes.openDoor(scene, FAR_DOOR, true);
        scene.idle(9);
        // The shot (WatheItemScenes.shoot) reaches it from behind, before the player it carries in front, and kills it
        // that tick; the player is set down at the centre of its cell, (4, 2).
        // 开枪（同 WatheItemScenes.shoot）从背后打中它，先于它身前扛着的人，当 tick 打死；被扛的人落在它所在格 (4, 2) 的中心。
        Actors.lookPitch(scene, vigilante, -4);
        scene.overlay().showLine(PonderPalette.RED, vigilanteFeet.add(forward(aim).multiply(0.85)).add(0, 1.05, 0),
                fall.add(0, 1.3, 0), 6);
        die(scene, goer, prey, new Vec3d(4.5, 1, 2.5), away);
        scene.idle(3);
        Actors.lookPitch(scene, vigilante, 0);
        scene.idle(7);
        scene.overlay().showText(90)
                .colored(PonderPalette.GREEN)
                .text("派对客一击就死：刀、左轮这类能杀死玩家的攻击都行")
                .independent()
                .attachKeyFrame();
        scene.idle(87);
        WatheItemScenes.openDoor(scene, FAR_DOOR, false);
        scene.idle(13);
        scene.overlay().showText(80)
                .text("它扛着的人被放了下来，毫发无伤")
                .independent()
                .attachKeyFrame();
        scene.idle(20);
        Actors.walk(scene, prey, new Vec3d(-0.4, 0, -1.4), 30);
        scene.idle(70);
        scene.overlay().showText(90)
                .colored(PonderPalette.GREEN)
                .text("打死它的人得到 25 金币，理智回升 20%%（召唤它的派对狂除外）")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showText(90)
                .text("打死它不算杀人：开枪打它不会掉枪，也不算误杀")
                .independent();
        scene.idle(100);
        scene.overlay().showText(90)
                .text("被扛着的人能转头、说话，但动不了也下不来，只能喊人来救")
                .independent()
                .attachKeyFrame();
        scene.idle(100);
        scene.overlay().showText(90)
                .colored(PonderPalette.RED)
                .text("没人拦的话，它会扛进终点房间吃掉：5 秒起，每层护盾多 5 秒")
                .independent();
        scene.idle(100);
        scene.overlay().showText(100)
                .text("它留下一具派对客尸体；召唤花的 50 金币不退，60 秒后才能再召唤")
                .independent();
        scene.idle(110);
        scene.markAsFinished();
    }

    /** The wide plate, then the cabin walls and doors dropping in. 先显示宽底板，再落下客房墙与门。 */
    private static void stage(SceneBuilder scene, SceneBuildingUtil util) {
        NoellesKillerScenes.wideStage(scene);
        scene.world().showSection(util.select().layersFrom(1), Direction.DOWN);
        scene.idle(15);
    }

    /**
     * The summon's 20 poof particles a block above the spawn (PartygoerService.summon: spread 0.3, 0.6, 0.3; speed
     * 0.02). 召唤时在出生点上方一格冒出的 20 个烟雾粒子（扩散 0.3、0.6、0.3，速度 0.02）。
     */
    private static void poof(SceneBuilder scene, Vec3d feet) {
        scene.addInstruction(ponder -> {
            for (int i = 0; i < 20; i++) {
                ponder.getWorld().addParticle(ParticleTypes.POOF, feet.x + RANDOM.nextGaussian() * 0.3,
                        feet.y + 1 + RANDOM.nextGaussian() * 0.6, feet.z + RANDOM.nextGaussian() * 0.3,
                        RANDOM.nextGaussian() * 0.02, RANDOM.nextGaussian() * 0.02, RANDOM.nextGaussian() * 0.02);
            }
        });
    }

    /**
     * A {@code color} box around a room for {@code ticks}: marks the rooms picked in the summon screen.
     * 在 ticks 内用 color 色方框框住一个房间：标出召唤界面里选中的房间。
     */
    private static void frame(SceneBuilder scene, Box room, int color, int ticks) {
        Object slot = new Object();
        scene.addInstruction(new TickingInstruction(false, ticks) {
            @Override
            public void tick(PonderScene ponder) {
                super.tick(ponder);
                ponder.getOutliner().chaseAABB(slot, room).lineWidth(1 / 16f).colored(color);
            }
        });
    }

    /** The Partygoer appears at {@code feet}, facing {@code yaw}, as {@code link}. 派对客在 feet 处朝 yaw 出现，链接为 link。 */
    private static void summon(SceneBuilder scene, ElementLink<Partygoer> link, Vec3d feet, float yaw) {
        Partygoer goer = new Partygoer(feet, yaw);
        scene.addInstruction(ponder -> {
            goer.reset(ponder);
            Actors.placeAtOnce(ponder, goer);
            ponder.linkElement(goer, link);
        });
    }

    /** Walk by {@code delta} over {@code ticks}, turning towards the way. 在 ticks 内走过 delta，并转向前进方向。 */
    private static void walk(SceneBuilder scene, ElementLink<Partygoer> goer, Vec3d delta, int ticks) {
        float heading = (float) (MathHelper.atan2(-delta.x, delta.z) * MathHelper.DEGREES_PER_RADIAN);
        apply(scene, goer, element -> element.walk(delta, ticks, heading));
    }

    private static void turn(SceneBuilder scene, ElementLink<Partygoer> goer, float yaw) {
        apply(scene, goer, element -> element.turnTo(yaw));
    }

    /** Pick up a player in {@code color}: it now rides the Partygoer's arms. 扛起 color 色的玩家：此后骑在派对客双臂上。 */
    private static void carry(SceneBuilder scene, ElementLink<Partygoer> goer, int color) {
        apply(scene, goer, element -> element.carry(color));
    }

    /**
     * Killed: the body falls, and the player it carried stands at once at {@code setDown} facing {@code yaw}
     * (PartygoerService.releasePassenger: the centre of the Partygoer's cell).
     * 被打死：尸体倒下，被扛的人立刻站到 setDown、朝 yaw（releasePassenger：派对客所在格的中心）。
     */
    private static void die(SceneBuilder scene, ElementLink<Partygoer> goer, ElementLink<ActorElement> carried,
                            Vec3d setDown, float yaw) {
        apply(scene, goer, Partygoer::die);
        scene.addInstruction(ponder -> {
            ActorElement actor = ponder.resolve(carried);
            if (actor != null) {
                actor.moveTo(setDown);
                actor.setYaw(yaw);
                actor.setVisible(true);
            }
        });
    }

    private static void apply(SceneBuilder scene, ElementLink<Partygoer> goer, Consumer<Partygoer> change) {
        scene.addInstruction(ponder -> {
            Partygoer element = ponder.resolve(goer);
            if (element != null) {
                change.accept(element);
            }
        });
    }

    /** The level direction a player facing {@code yaw} looks along (0 = south, 90 = west). 朝向 yaw 时的水平前方。 */
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
     * The balloon mark's yellow outline on the Party Animal's screen (a box, as {@link Actors#highlight} draws one):
     * around the marked player while they stand, and around them on the Partygoer's arms while it carries them.
     * 派对狂屏幕上气球标记的黄色描边（与 Actors.highlight 一样画成方框）：站着时框住被标记的人，被派对客扛着时框住它双臂上的人。
     */
    private static final class MarkOutline extends TickingInstruction {
        private final ElementLink<ActorElement> marked;
        private final ElementLink<Partygoer> goer;
        private final int color;
        // One outline each, so switching does not slide a box across the stage.
        // 两种状态各用一个描边，切换时方框不会滑过舞台。
        private final Object standing = new Object();
        private final Object carried = new Object();

        MarkOutline(ElementLink<ActorElement> marked, ElementLink<Partygoer> goer, int color, int ticks) {
            super(false, ticks);
            this.marked = marked;
            this.goer = goer;
            this.color = color;
        }

        @Override
        public void tick(PonderScene scene) {
            super.tick(scene);
            Partygoer carrier = scene.resolve(goer);
            ActorElement actor = scene.resolve(marked);
            if (carrier != null && carrier.isCarrying()) {
                scene.getOutliner().chaseAABB(carried, carrier.carriedBox()).lineWidth(1 / 16f).colored(color);
            } else if (actor != null && actor.isVisible()) {
                Vec3d at = actor.position();
                scene.getOutliner().chaseAABB(standing,
                        new Box(at.x - 0.4, at.y, at.z - 0.4, at.x + 0.4, at.y + 1.95, at.z + 0.4))
                        .lineWidth(1 / 16f).colored(color);
            }
        }
    }

    /**
     * SparkStrength's Partygoer, a PartygoerEntity drawn by PartygoerEntityRenderer: the wide player model in the
     * Partygoer skin, arms out like a zombie, legs swinging with its steps. It never joins a world: like
     * {@link ActorPlayer} it is posed here each tick. A carried player is an {@link ActorPlayer} riding it, drawn where
     * PartygoerEntity seats a passenger, so SparkStrength's PartygoerCarriedPoseMixin lays it across the arms. Killed,
     * it becomes the Wathe body PartygoerCorpseService spawns where it stood: Partygoer skin, falling face down along
     * its facing with the feet a block behind (Wathe's body renderer), as the Jester's fake body does. SparkAssist does
     * not build against SparkStrength, so the entity comes from the registry and its state field is found by name;
     * without them nothing is drawn but the body.
     * SparkStrength 的派对客：由 PartygoerEntityRenderer 绘制的 PartygoerEntity，宽手臂玩家模型配派对客皮肤，像僵尸那样
     * 伸着双臂，走路时双腿摆动。它从不加入世界：与 ActorPlayer 一样每 tick 在这里摆好姿态。被扛的人是骑在它身上的
     * ActorPlayer，画在 PartygoerEntity 安放乘客的位置，于是 SparkStrength 的 PartygoerCarriedPoseMixin 会让他横躺在双臂上。
     * 被打死后变成 PartygoerCorpseService 在原地生成的 Wathe 尸体：派对客皮肤，沿朝向脸朝下倒下，脚在后方一格（Wathe 尸体
     * 渲染器），与小丑的假尸体相同。SparkAssist 不依赖 SparkStrength 编译，所以实体从注册表取得、状态字段按名字查找；找不到
     * 时只画尸体。
     */
    private static final class Partygoer extends AnimatedSceneElementBase {
        private static final Identifier TYPE = Identifier.of("sparkstrength", "partygoer");
        private static final Identifier SKIN = Identifier.of("sparkstrength", "textures/entity/partygoer.png");
        /** PartygoerState wire ids. PartygoerState 的 wire id。 */
        private static final byte HUNT = 1;
        private static final byte CARRY = 2;
        /** PartygoerBrain.TURN_DEGREES_PER_TICK. 每 tick 转向角度。 */
        private static final float TURN_SPEED = 30;
        /**
         * Where a carried player's feet ride: PartygoerEntity.CARRY_ATTACHMENT (1.05 up, 0.55 ahead) less a player's
         * own vehicle attachment (0.6 up). 被扛玩家的脚所在处：CARRY_ATTACHMENT（上 1.05、前 0.55）减去玩家自身的
         * 骑乘挂点（上 0.6）。
         */
        private static final double SEAT_HEIGHT = 1.05 - 0.6;
        private static final double SEAT_AHEAD = 0.55;
        private static final float FALL_TICKS = 20;
        @Nullable
        private static Field stateField;
        private static boolean stateFieldLooked;

        private final Vec3d startPosition;
        private final float startYaw;
        private final ActorMovement movement = new ActorMovement();
        private Vec3d position;
        private Vec3d previousPosition;
        private float yaw;
        private float previousYaw;
        private float targetYaw;
        private byte state;
        /** The carried player's colour, or -1 for none. 被扛玩家的颜色，-1 表示没有。 */
        private int carried;
        private float fall;
        private int age;
        @Nullable
        private LivingEntity entity;
        @Nullable
        private ActorPlayer rider;
        @Nullable
        private ClientWorld entityWorld;

        private Partygoer(Vec3d position, float yaw) {
            this.startPosition = position;
            this.startYaw = yaw;
            reset(null);
        }

        @Override
        public void reset(@Nullable PonderScene scene) {
            position = startPosition;
            previousPosition = startPosition;
            yaw = startYaw;
            previousYaw = startYaw;
            targetYaw = startYaw;
            movement.clear();
            state = HUNT;
            carried = -1;
            fall = -1;
            age = 0;
            dropRider();
        }

        void walk(Vec3d delta, int ticks, float heading) {
            movement.add(delta.x, delta.y, delta.z, ticks, true);
            targetYaw = heading;
        }

        void turnTo(float yaw) {
            targetYaw = yaw;
        }

        void carry(int color) {
            carried = color & 0xFFFFFF;
            state = CARRY;
            dropRider();
        }

        boolean isCarrying() {
            return carried >= 0 && fall < 0;
        }

        /**
         * Around the carried player, lying across the arms (PartygoerCarriedPoseMixin: centred 0.55 above the seat,
         * along the Partygoer's left-right axis). 框住横躺在双臂上的被扛者（以座位上方 0.55 为中心，沿派对客的左右方向）。
         */
        Box carriedBox() {
            Vec3d ahead = forward(yaw);
            Vec3d centre = position.add(ahead.multiply(SEAT_AHEAD)).add(0, SEAT_HEIGHT + 0.55, 0);
            double x = 0.95 * Math.abs(ahead.z) + 0.35 * Math.abs(ahead.x);
            double z = 0.95 * Math.abs(ahead.x) + 0.35 * Math.abs(ahead.z);
            return new Box(centre.x - x, centre.y - 0.35, centre.z - z, centre.x + x, centre.y + 0.35, centre.z + z);
        }

        void die() {
            fall = 0;
            movement.clear();
            carried = -1;
            dropRider();
        }

        @Override
        public void tick(PonderScene scene) {
            previousPosition = position;
            previousYaw = yaw;
            age++;
            if (fall >= 0) {
                fall = Math.min(FALL_TICKS, fall + 1);
                return;
            }
            ActorMovement.Step step = movement.tick();
            position = position.add(step.x(), step.y(), step.z());
            yaw += MathHelper.clamp(MathHelper.wrapDegrees(targetYaw - yaw), -TURN_SPEED, TURN_SPEED);
            LivingEntity goer = entity();
            if (goer != null) {
                goer.limbAnimator.updateLimbs(ActorMovement.strideSpeed(step.walkingDistance()), 0.4f);
                goer.age = age;
            }
        }

        @Override
        protected void renderLast(PonderLevel world, VertexConsumerProvider buffer, DrawContext graphics, float fade,
                                  float pt) {
            if (fade <= 0.01f) {
                return;
            }
            pt = MathHelper.clamp(pt, 0, 1);
            MatrixStack ms = graphics.getMatrices();
            int light = lightCoordsFromFade(world.scene == null ? fade
                    : Math.min(fade, StageLights.actorLevel(world.scene)));
            if (fall >= 0) {
                float radians = yaw * MathHelper.RADIANS_PER_DEGREE;
                ms.push();
                ms.translate(position.x + MathHelper.sin(radians), position.y, position.z - MathHelper.cos(radians));
                ActorElement.lying(ms, buffer, SKIN, yaw,
                        -90 * ActorElement.bounceOut(Math.min(FALL_TICKS, fall + pt) / FALL_TICKS), light);
                ms.pop();
                return;
            }
            LivingEntity goer = entity();
            if (goer == null) {
                return;
            }
            pose(goer);
            Vec3d at = previousPosition.lerp(position, pt);
            float bodyYaw = MathHelper.lerpAngleDegrees(pt, previousYaw, yaw);
            EntityRenderDispatcher dispatcher = MinecraftClient.getInstance().getEntityRenderDispatcher();
            dispatcher.setRenderShadows(false);
            try {
                dispatcher.render(goer, at.x, at.y, at.z, bodyYaw, pt, ms, buffer, light);
                ActorPlayer passenger = rider(goer);
                if (passenger != null) {
                    Vec3d seat = at.add(forward(bodyYaw).multiply(SEAT_AHEAD)).add(0, SEAT_HEIGHT, 0);
                    seat(passenger, seat);
                    ActorPlayer.setDrawing(true);
                    try {
                        dispatcher.render(passenger, seat.x, seat.y, seat.z, bodyYaw, pt, ms, buffer, light);
                    } finally {
                        ActorPlayer.setDrawing(false);
                    }
                }
            } finally {
                dispatcher.setRenderShadows(MinecraftClient.getInstance().options.getEntityShadows().getValue());
            }
        }

        /** Copy this tick's state into the entity its renderer reads. 把本 tick 的状态写入渲染器读取的实体。 */
        private void pose(LivingEntity goer) {
            goer.setPosition(position);
            goer.prevX = previousPosition.x;
            goer.prevY = previousPosition.y;
            goer.prevZ = previousPosition.z;
            goer.lastRenderX = previousPosition.x;
            goer.lastRenderY = previousPosition.y;
            goer.lastRenderZ = previousPosition.z;
            goer.prevBodyYaw = previousYaw;
            goer.bodyYaw = yaw;
            goer.prevHeadYaw = previousYaw;
            goer.headYaw = yaw;
            goer.prevYaw = previousYaw;
            goer.setYaw(yaw);
            goer.prevPitch = 0;
            goer.setPitch(0);
            setState(goer, state);
        }

        /** The carried player, facing the way the Partygoer does, legs still. 被扛的人，与派对客朝向相同，双腿不动。 */
        private void seat(ActorPlayer passenger, Vec3d seat) {
            passenger.setPosition(seat);
            passenger.prevX = seat.x;
            passenger.prevY = seat.y;
            passenger.prevZ = seat.z;
            passenger.lastRenderX = seat.x;
            passenger.lastRenderY = seat.y;
            passenger.lastRenderZ = seat.z;
            passenger.prevBodyYaw = previousYaw;
            passenger.bodyYaw = yaw;
            passenger.prevHeadYaw = previousYaw;
            passenger.headYaw = yaw;
            passenger.prevYaw = previousYaw;
            passenger.setYaw(yaw);
            passenger.preferredHand = Hand.MAIN_HAND;
            passenger.age = age;
        }

        /**
         * Set PartygoerEntity.STATE as the brain does, so the model poses for it. 像大脑那样设置 STATE，让模型摆出对应姿势。
         */
        @SuppressWarnings("unchecked")
        private static void setState(Entity goer, byte wire) {
            if (!stateFieldLooked) {
                stateFieldLooked = true;
                try {
                    stateField = goer.getClass().getDeclaredField("STATE");
                    stateField.setAccessible(true);
                } catch (ReflectiveOperationException e) {
                    stateField = null;
                }
            }
            if (stateField == null) {
                return;
            }
            try {
                goer.getDataTracker().set((TrackedData<Byte>) stateField.get(null), wire);
            } catch (ReflectiveOperationException | ClassCastException e) {
                // Another SparkStrength version: keep the hunting pose. 其他版本的 SparkStrength：保持追人姿势。
            }
        }

        /** The entity, rebuilt when the client world changes. 绘制用的实体，客户端世界变化时重建。 */
        @Nullable
        private LivingEntity entity() {
            ClientWorld world = MinecraftClient.getInstance().world;
            if (world == null) {
                return null;
            }
            if (entity == null || entityWorld != world) {
                dropRider();
                entity = Registries.ENTITY_TYPE.getOrEmpty(TYPE)
                        .map(type -> type.create(world))
                        .filter(LivingEntity.class::isInstance)
                        .map(LivingEntity.class::cast)
                        .orElse(null);
                entityWorld = world;
            }
            return entity;
        }

        /** The carried player, mounted on first use. 被扛的人，首次使用时骑上去。 */
        @Nullable
        private ActorPlayer rider(LivingEntity goer) {
            if (carried < 0 || entityWorld == null) {
                return null;
            }
            if (rider == null) {
                rider = new ActorPlayer(entityWorld, carried);
                rider.startRiding(goer, true);
            }
            return rider;
        }

        private void dropRider() {
            if (rider != null) {
                rider.stopRiding();
                rider = null;
            }
        }
    }
}
