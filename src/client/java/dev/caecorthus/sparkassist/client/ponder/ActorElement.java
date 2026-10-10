package dev.caecorthus.sparkassist.client.ponder;

import dev.doctor4t.wathe.cca.PlayerPsychoComponent;
import dev.doctor4t.wathe.client.WatheClient;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.element.AnimatedSceneElementBase;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

/**
 * A demo actor: a player in one role colour (see {@link ActorSkins}) with an optional name tag in that colour above
 * its head. While standing it is drawn by the vanilla player renderer through an {@link ActorPlayer}, so held items
 * and Wathe's arm poses look exactly as in a round; once killed it is drawn as Wathe's body, face down where it faced.
 * Scene scripts drive it through {@link Actors}; state changes land in {@link #tick} order and rendering interpolates.
 * 演示演员：身份颜色的玩家（见 ActorSkins），头顶可显示同色名牌。站立时通过 ActorPlayer 交给原版玩家渲染器绘制，
 * 手持物品与 Wathe 的手臂姿势与对局中完全一致；被杀后按 Wathe 尸体绘制，朝其面向方向脸朝下。
 * 场景脚本通过 Actors 驱动它；状态在 tick 中推进，渲染时插值。
 */
public final class ActorElement extends AnimatedSceneElementBase {
    private static final int SWING_TICKS = 6;
    /** Degrees turned per tick towards a new heading: a half turn takes five ticks. 每 tick 转向新朝向的角度：转身半圈用五 tick。 */
    private static final float TURN_SPEED = 36;
    /** Moves longer than this in one tick are teleports: no stride for them. 单 tick 内超过此距离的移动视为传送，不迈步。 */
    private static final double TELEPORT = 1;
    /** Wathe's body falls over 20 ticks with a bounce (PlayerBodyEntityRenderer). Wathe 尸体在 20 tick 内回弹着倒下。 */
    private static final float FALL_TICKS = 20;
    private static final float PLAYER_SCALE = 0.9375f;
    private static final float NAME_SCALE = 0.025f;
    /** Vanilla puts a sleeper this high above the bed block (LivingEntity.setPositionInBed). 原版睡觉时人物离床方块底部的高度。 */
    private static final double BED_HEIGHT = 0.6875;
    /** Vanilla shifts a sleeper's feet this far from the head block (eye height - 0.1). 原版睡觉时脚离床头方块的距离。 */
    private static final double BED_REACH = 1.52;
    /** SparkStrength lifts a skateboard rider onto the deck by this much (SkateboardRenderer.DECK_TOP_HEIGHT). 滑板骑手被抬到板面上的高度。 */
    private static final double DECK_HEIGHT = 3 / 16.0;
    @Nullable
    private static PlayerEntityModel<LivingEntity> bodyModel;

    private final int startColor;
    @Nullable
    private final Text startName;
    private final Vec3d startPosition;
    private final float startYaw;
    @Nullable
    private ActorPlayer player;
    @Nullable
    private ClientWorld playerWorld;

    private int color;
    @Nullable
    private Text name;
    private ItemStack held = ItemStack.EMPTY;
    private ItemStack offHand = ItemStack.EMPTY;
    private ItemStack chest = ItemStack.EMPTY;
    /** The board ridden, drawn under the feet; empty when on foot. 脚下骑着的滑板；步行时为空。 */
    private ItemStack board = ItemStack.EMPTY;
    private boolean invisible;
    private boolean psycho;
    @Nullable
    private BlockPos bedHead;
    private Direction bedFacing = Direction.NORTH;
    private boolean charging;
    private boolean sneaking;
    private Vec3d position;
    private Vec3d previousPosition;
    /** Where the actor stood after its own step last tick; anything since is a move made by an instruction. 上一 tick 自身迈步后的位置；之后的位移来自指令。 */
    private Vec3d settled;
    /** Walks and slides under way; overlapping ones add up (a knockback with a hop). 进行中的行走与平移；重叠时相加（击退加上弹起）。 */
    private final List<Move> moves = new ArrayList<>();
    private float yaw;
    private float previousYaw;
    private float targetYaw;
    private float pitch;
    private int age;
    private int swingTicks = -1;
    private float fall = -1;

    /**
     * @param position feet position in scene coordinates / 场景坐标中的脚底位置
     * @param yaw      facing in degrees, Minecraft convention (0 = south, 90 = west) / 朝向角度，与 Minecraft 相同
     * @param name     name tag text, or null for none / 名牌文字，null 表示不显示
     */
    public ActorElement(int color, @Nullable Text name, Vec3d position, float yaw) {
        this.startColor = color & 0xFFFFFF;
        this.startName = name;
        this.startPosition = position;
        this.startYaw = yaw;
        reset(null);
    }

    /** Back to the starting state: Ponder reuses element objects when a scene replays or seeks. 回到初始状态：重播或跳转时 Ponder 会复用元素对象。 */
    @Override
    public void reset(@Nullable PonderScene scene) {
        color = startColor;
        name = startName;
        position = startPosition;
        previousPosition = startPosition;
        settled = startPosition;
        moves.clear();
        yaw = startYaw;
        previousYaw = startYaw;
        targetYaw = startYaw;
        pitch = 0;
        held = ItemStack.EMPTY;
        offHand = ItemStack.EMPTY;
        chest = ItemStack.EMPTY;
        board = ItemStack.EMPTY;
        invisible = false;
        psycho = false;
        bedHead = null;
        charging = false;
        sneaking = false;
        age = 0;
        swingTicks = -1;
        fall = -1;
        dropPlayer();
    }

    // ------------------------------------------------------------------ state, set by instructions / 状态（由指令设置）

    public Vec3d position() {
        return position;
    }

    public void moveTo(Vec3d target) {
        position = target;
    }

    public float yaw() {
        return yaw;
    }

    /** Face {@code yaw} at once (a teleport). 立刻朝向 yaw（如传送）。 */
    public void setYaw(float yaw) {
        this.yaw = yaw;
        targetYaw = yaw;
    }

    /** Turn towards {@code yaw} over the next few ticks. 在接下来几 tick 内转向 yaw。 */
    public void turnTo(float yaw) {
        targetYaw = yaw;
    }

    /**
     * Cover {@code delta} in even steps over the next {@code ticks} ticks, turning to {@code heading} on the way
     * (NaN keeps the current facing), on top of any other walk or slide under way. The actor takes the steps itself
     * so its stride matches them tick for tick.
     * 在接下来 ticks 内匀速走完 delta，途中转向 heading（NaN 表示保持朝向），与进行中的其他行走或平移叠加。由演员自己
     * 迈步，步伐与位移逐 tick 同步。
     */
    public void walk(Vec3d delta, int ticks, float heading) {
        moves.add(new Move(delta.multiply(1.0 / ticks), ticks));
        if (!Float.isNaN(heading)) {
            turnTo(heading);
        }
    }

    public void setPitch(float degrees) {
        pitch = degrees;
    }

    public void hold(ItemStack stack) {
        held = stack.copy();
    }

    /** Hold {@code stack} in the off hand (a second pistol), or empty it. 副手拿着 stack（如第二把手枪），空物品即放下。 */
    public void holdOffHand(ItemStack stack) {
        offHand = stack.copy();
    }

    /**
     * Stand on {@code board} (SparkStrength's skateboard), or step off with an empty stack. As SkateboardRenderer draws
     * a rider for every viewer: the board under the feet, nose along the body, the body lifted onto its deck, the legs
     * still while it rolls along.
     * 站上 board（SparkStrength 的滑板），空物品即下板。与 SkateboardRenderer 在所有观察者眼中绘制骑手的方式一致：滑板在
     * 脚下、板头朝向身体朝向，身体抬到板面上，滑行时双腿不动。
     */
    public void ride(ItemStack board) {
        this.board = board.copy();
    }

    /** Wear {@code stack} in the chest slot (a vest), or take it off with an empty stack. 胸甲槽穿上 stack（如背心），空物品即脱下。 */
    public void wear(ItemStack stack) {
        chest = stack.copy();
    }

    /** Take on another colour and name tag in place (a disguise, a new role). 原地换成另一种颜色与名牌（伪装、转职）。 */
    public void retint(int color, @Nullable Text name) {
        this.color = color & 0xFFFFFF;
        this.name = name;
        if (player != null) {
            player.recolor(this.color);
        }
    }

    /**
     * Vanilla invisibility: the body and name tag vanish, held items and armour still show, as a real invisible
     * player looks to others.
     * 原版隐身：身体与名牌消失，手持物品和盔甲仍可见，与真实隐身玩家在别人眼中的样子一致。
     */
    public void setInvisible(boolean invisible) {
        this.invisible = invisible;
    }

    /**
     * Wathe's psycho mode look: the psycho skin, no armour or other layers (Wathe's own renderer mixins read the
     * psycho component, which this sets on the drawn entity).
     * Wathe 疯魔模式的外观：疯魔皮肤，不显示盔甲等图层（由 Wathe 自己的渲染 mixin 读取疯魔组件，这里设置在绘制用的实体上）。
     */
    public void setPsycho(boolean psycho) {
        this.psycho = psycho;
    }

    /**
     * Lie in the bed whose head block is {@code head} and whose {@code facing} points from foot to head, as a sleeping
     * player does: on the back, head on the pillow.
     * 躺进床头方块为 head、朝向（床尾指向床头）为 facing 的床，与睡觉的玩家相同：仰面，头枕在枕头上。
     */
    public void lieDown(BlockPos head, Direction facing) {
        bedHead = head;
        bedFacing = facing;
        charging = false;
        sneaking = false;
    }

    public void getUp() {
        bedHead = null;
    }

    public boolean isAsleep() {
        return bedHead != null;
    }

    /** Hold right-click with the held item (the knife's charge). 按住右键使用手持物品（如刀的蓄力）。 */
    public void setCharging(boolean charging) {
        this.charging = charging;
    }

    public void setSneaking(boolean sneaking) {
        this.sneaking = sneaking;
    }

    /** One main-hand swing, as Wathe plays after a stab. 一次主手挥动，如 Wathe 出刀后的动作。 */
    public void swing() {
        swingTicks = 0;
    }

    /**
     * Become a body, as a Wathe kill does: the player turns spectator and a body falls face down in the direction they
     * faced, its feet where they stood. Held items stay with the player, so the body's hands are empty.
     * 变成尸体，与 Wathe 的击杀一致：玩家变为旁观者，尸体以站立处为脚、朝其面向的方向脸朝下扑倒。物品留在玩家身上，
     * 所以尸体手里是空的。
     */
    public void fall() {
        fall = 0;
        targetYaw = yaw;
        charging = false;
        sneaking = false;
        held = ItemStack.EMPTY;
        offHand = ItemStack.EMPTY;
        board = ItemStack.EMPTY;
        swingTicks = -1;
    }

    public boolean isDown() {
        return fall >= 0;
    }

    @Override
    public void tick(PonderScene scene) {
        // Runs before this tick's instructions. The actor's own step and turn happen here, so rendering eases from
        // previous to current and the stride matches the step of the same tick; moves made by instructions since the
        // last tick (a scripted circle) count towards the stride one tick late, teleports not at all.
        // 在本 tick 的指令之前运行。演员自己的迈步与转身在这里完成，渲染从上一位置平滑过渡到当前位置，步伐与同一 tick
        // 的位移同步；上一 tick 以来由指令造成的移动（脚本绕圈）晚一 tick 计入步伐，传送不计入。
        ActorPlayer actor = player();
        double pushed = position.subtract(settled).horizontalLength();
        previousPosition = position;
        previousYaw = yaw;
        for (Iterator<Move> it = moves.iterator(); it.hasNext(); ) {
            Move move = it.next();
            position = position.add(move.step);
            if (--move.ticks <= 0) {
                it.remove();
            }
        }
        settled = position;
        if (!isDown() && bedHead == null) {
            yaw += MathHelper.clamp(MathHelper.wrapDegrees(targetYaw - yaw), -TURN_SPEED, TURN_SPEED);
        }
        double moved = position.subtract(previousPosition).horizontalLength() + (pushed < TELEPORT ? pushed : 0);
        age++;
        if (fall >= 0 && fall < FALL_TICKS) {
            fall++;
        }
        if (actor == null) {
            return;
        }
        // LivingEntity's limb and swing bookkeeping, without ticking the entity in the real world.
        // 复刻 LivingEntity 的肢体与挥手计数，但不在真实世界中 tick 这个实体。
        // A rider rolls instead of walking (SparkStrength SkateboardLimbsMixin). 骑手是滚动而非行走。
        actor.limbAnimator.updateLimbs(isDown() || !board.isEmpty() ? 0 : Math.min(1, (float) moved * 4), 0.4f);
        actor.lastHandSwingProgress = actor.handSwingProgress;
        if (swingTicks >= 0) {
            actor.handSwinging = true;
            actor.handSwingTicks = swingTicks;
            actor.handSwingProgress = swingTicks / (float) SWING_TICKS;
            swingTicks = swingTicks + 1 > SWING_TICKS ? -1 : swingTicks + 1;
        } else {
            actor.handSwinging = false;
            actor.handSwingTicks = 0;
            actor.handSwingProgress = 0;
        }
        actor.age = age;
    }

    // ------------------------------------------------------------------ rendering / 渲染

    @Override
    protected void renderLast(PonderLevel world, VertexConsumerProvider buffer, DrawContext graphics, float fade,
                              float pt) {
        if (fade <= 0.01f) {
            return;
        }
        // Ponder's slow-reading mode can hand out partial ticks outside 0..1 as a caption closes; never extrapolate.
        // Ponder 的慢速阅读模式在字幕关闭时可能给出 0..1 之外的插值；不外推。
        pt = MathHelper.clamp(pt, 0, 1);
        MatrixStack ms = graphics.getMatrices();
        Vec3d at = previousPosition.lerp(position, pt);
        float bodyYaw = MathHelper.lerpAngleDegrees(pt, previousYaw, yaw);
        int light = lightCoordsFromFade(world.scene == null ? fade : Math.min(fade, StageLights.actorLevel(world.scene)));
        if (isDown()) {
            ms.push();
            ms.translate(at.x, at.y, at.z);
            lying(ms, buffer, bodyYaw, -90 * bounceOut(Math.min(FALL_TICKS, fall + pt) / FALL_TICKS), light);
            ms.pop();
            return;
        }
        if (bedHead != null) {
            ms.push();
            ms.translate(bedHead.getX() + 0.5 - bedFacing.getOffsetX() * BED_REACH, bedHead.getY() + BED_HEIGHT,
                    bedHead.getZ() + 0.5 - bedFacing.getOffsetZ() * BED_REACH);
            lying(ms, buffer, bedFacing.getOpposite().asRotation(), 90, light);
            ms.pop();
            return;
        }
        ActorPlayer actor = player();
        if (actor == null) {
            return;
        }
        pose(actor);
        double lift = board.isEmpty() ? 0 : DECK_HEIGHT;
        if (!board.isEmpty() && !invisible) {
            board(world, ms, buffer, at, bodyYaw, light);
        }
        EntityRenderDispatcher dispatcher = MinecraftClient.getInstance().getEntityRenderDispatcher();
        dispatcher.setRenderShadows(false);
        ActorPlayer.setDrawing(true);
        try {
            dispatcher.render(actor, at.x, at.y + lift, at.z, bodyYaw, pt, ms, buffer, light);
        } finally {
            ActorPlayer.setDrawing(false);
            dispatcher.setRenderShadows(MinecraftClient.getInstance().options.getEntityShadows().getValue());
        }
        if (name != null && !invisible) {
            ms.push();
            ms.translate(at.x, at.y + lift + 2.15, at.z);
            nameTag(world, ms, buffer, pt);
            ms.pop();
        }
    }

    /**
     * The ridden board as SparkStrength's SkateboardRenderer draws it: the item model at real size, nose (+Z) along
     * the body yaw, wheels (model y = 0) on the floor. An invisible rider's board is not drawn, as there.
     * 按 SparkStrength 的 SkateboardRenderer 绘制脚下的滑板：真实尺寸的物品模型，板头（+Z）朝身体朝向，轮子（模型 y = 0）
     * 贴地。与原版一致，隐身骑手的滑板不绘制。
     */
    private void board(PonderLevel world, MatrixStack ms, VertexConsumerProvider buffer, Vec3d feet, float bodyYaw,
                       int light) {
        ms.push();
        ms.translate(feet.x, feet.y, feet.z);
        ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-bodyYaw));
        // ItemRenderer recentres by -0.5 after the NONE transform; undo the Y part. 抵消 ItemRenderer 的 -0.5 居中。
        ms.translate(0, 0.5, 0);
        MinecraftClient.getInstance().getItemRenderer().renderItem(board, ModelTransformationMode.NONE, light,
                OverlayTexture.DEFAULT_UV, ms, buffer, world, 0);
        ms.pop();
    }

    /** Copy this tick's state into the entity the vanilla renderer reads. 把本 tick 的状态写入原版渲染器读取的实体。 */
    private void pose(ActorPlayer actor) {
        actor.setStackInHand(Hand.MAIN_HAND, held);
        actor.setStackInHand(Hand.OFF_HAND, offHand);
        actor.equipStack(EquipmentSlot.CHEST, chest);
        actor.setInvisible(invisible);
        PlayerPsychoComponent.KEY.get(actor).psychoTicks = psycho ? 1 : 0;
        actor.charging = charging && !held.isEmpty();
        // Swings use the arm of preferredHand; left null (never swung through swingHand) it is the off arm.
        // 挥手动画使用 preferredHand 对应的手臂；为 null（从未经 swingHand 挥过）时会变成副手。
        actor.preferredHand = Hand.MAIN_HAND;
        actor.setSneaking(sneaking);
        actor.setPose(sneaking ? EntityPose.CROUCHING : EntityPose.STANDING);
        actor.prevBodyYaw = previousYaw;
        actor.bodyYaw = yaw;
        actor.prevHeadYaw = previousYaw;
        actor.headYaw = yaw;
        actor.prevYaw = previousYaw;
        actor.setYaw(yaw);
        actor.prevPitch = pitch;
        actor.setPitch(pitch);
    }

    /**
     * A player lying flat, feet at the origin, tipped {@code angle} degrees about the feet away from standing: -90 is
     * Wathe's body (PlayerBodyEntityRenderer: face down towards {@code yaw}, arms at the sides), +90 a sleeper on the
     * back with the head away from {@code yaw}. Lifted in step with the tip so the model rests on the surface.
     * 平躺的玩家，脚在原点，绕脚从站立倾倒 angle 度：-90 是 Wathe 的尸体（PlayerBodyEntityRenderer：朝 yaw 脸朝下扑倒，
     * 双臂贴身），+90 是仰面睡觉、头朝 yaw 反方向的人。随倾倒同步抬高，让模型落在表面上。
     */
    private void lying(MatrixStack ms, VertexConsumerProvider buffer, float yaw, float angle, int light) {
        ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180 - yaw));
        ms.translate(0, Math.abs(angle) / 90 * 0.15f, 0);
        ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(angle));
        ms.scale(-PLAYER_SCALE, -PLAYER_SCALE, PLAYER_SCALE);
        ms.translate(0, -1.501, 0);
        PlayerEntityModel<LivingEntity> model = bodyModel();
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
        model.render(ms, buffer.getBuffer(RenderLayer.getEntityCutoutNoCull(ActorSkins.of(color))), light,
                OverlayTexture.DEFAULT_UV, 0xFFFFFFFF);
    }

    /** Penner's bounce-out, as Ratatouille's Easing.BOUNCE_OUT. 与 Ratatouille 的 BOUNCE_OUT 相同的回弹缓动。 */
    static float bounceOut(float t) {
        if (t < 1 / 2.75f) {
            return 7.5625f * t * t;
        }
        if (t < 2 / 2.75f) {
            t -= 1.5f / 2.75f;
            return 7.5625f * t * t + 0.75f;
        }
        if (t < 2.5f / 2.75f) {
            t -= 2.25f / 2.75f;
            return 7.5625f * t * t + 0.9375f;
        }
        t -= 2.625f / 2.75f;
        return 7.5625f * t * t + 0.984375f;
    }

    /**
     * Role-coloured name with a black outline above the head, facing the viewer. Ponder maps the scene to the screen with its two camera
     * rotations and a y flip, so undoing those leaves plain screen axes (y down) for the text.
     * 头顶带黑色描边的身份色名牌，始终朝向观看者。Ponder 用两次镜头旋转加一次 y 翻转把场景映射到屏幕，抵消后文字就落在
     * 普通屏幕坐标（y 朝下）中。
     */
    private void nameTag(PonderLevel world, MatrixStack ms, VertexConsumerProvider buffer, float pt) {
        TextRenderer font = MinecraftClient.getInstance().textRenderer;
        ms.scale(1, -1, 1);
        if (world.scene != null) {
            PonderScene.SceneTransform transform = world.scene.getTransform();
            ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-transform.yRotation.getValue(pt)));
            ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-transform.xRotation.getValue(pt)));
        }
        ms.scale(NAME_SCALE, NAME_SCALE, NAME_SCALE);
        OrderedText text = name.asOrderedText();
        font.drawWithOutline(text, -font.getWidth(text) / 2f, -font.fontHeight, 0xFF000000 | readable(color), 0xFF000000,
                ms.peek().getPositionMatrix(), buffer, LightmapTextureManager.MAX_LIGHT_COORDINATE);
    }

    /**
     * The role colour, lifted towards white until it stands out from the tag's black outline: dark roles (the
     * Veteran's olive, deep reds) would otherwise read as a dark smudge. Bright colours pass unchanged.
     * 身份颜色，必要时向白色提亮，直到与名牌的黑色描边拉开对比：深色身份（老兵的橄榄绿、深红）否则会糊成一团。
     * 亮色保持不变。
     */
    static int readable(int rgb) {
        int r = rgb >> 16 & 0xFF;
        int g = rgb >> 8 & 0xFF;
        int b = rgb & 0xFF;
        for (float t = 0; t <= 1; t += 0.05f) {
            int lr = Math.round(r + (255 - r) * t);
            int lg = Math.round(g + (255 - g) * t);
            int lb = Math.round(b + (255 - b) * t);
            if (luminance(lr, lg, lb) >= 0.2) {
                return lr << 16 | lg << 8 | lb;
            }
        }
        return 0xFFFFFF;
    }

    /** WCAG relative luminance of an sRGB colour. sRGB 颜色的 WCAG 相对亮度。 */
    static double luminance(int r, int g, int b) {
        return 0.2126 * linear(r) + 0.7152 * linear(g) + 0.0722 * linear(b);
    }

    private static double linear(int channel) {
        double c = channel / 255.0;
        return c <= 0.04045 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    }

    /** The entity, rebuilt when the client world changes; null without a world (Ponder needs one anyway).
     * 绘制用的实体，客户端世界变化时重建；没有世界时为 null（Ponder 本身也需要世界）。 */
    @Nullable
    private ActorPlayer player() {
        ClientWorld world = MinecraftClient.getInstance().world;
        if (world == null) {
            return null;
        }
        if (player == null || playerWorld != world) {
            dropPlayer();
            player = new ActorPlayer(world, color);
            playerWorld = world;
        }
        return player;
    }

    /**
     * Let go of the drawn entity. Wathe keeps a muzzle position per gun-holding player in a static map, which would
     * otherwise hold every replaced actor and its world.
     * 释放绘制用的实体。Wathe 在静态表中为每个持枪玩家记录枪口位置，不移除的话会一直持有被替换的演员及其世界。
     */
    private void dropPlayer() {
        if (player != null && WatheClient.particleMap != null) {
            WatheClient.particleMap.remove(player);
        }
        player = null;
    }

    private static final class Move {
        private final Vec3d step;
        private int ticks;

        private Move(Vec3d step, int ticks) {
            this.step = step;
            this.ticks = ticks;
        }
    }

    private static PlayerEntityModel<LivingEntity> bodyModel() {
        if (bodyModel == null) {
            bodyModel = new PlayerEntityModel<>(MinecraftClient.getInstance().getEntityModelLoader()
                    .getModelPart(EntityModelLayers.PLAYER), false);
        }
        return bodyModel;
    }
}
