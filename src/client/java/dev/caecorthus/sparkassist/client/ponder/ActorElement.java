package dev.caecorthus.sparkassist.client.ponder;

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
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
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
    /** Wathe's body falls over 20 ticks with a bounce (PlayerBodyEntityRenderer). Wathe 尸体在 20 tick 内回弹着倒下。 */
    private static final float FALL_TICKS = 20;
    private static final float PLAYER_SCALE = 0.9375f;
    private static final float NAME_SCALE = 0.025f;
    @Nullable
    private static PlayerEntityModel<LivingEntity> bodyModel;

    private final int color;
    @Nullable
    private final Text name;
    private final Vec3d startPosition;
    private final float startYaw;
    @Nullable
    private ActorPlayer player;
    @Nullable
    private ClientWorld playerWorld;

    private ItemStack held = ItemStack.EMPTY;
    private boolean charging;
    private boolean sneaking;
    private Vec3d position;
    private Vec3d previousPosition;
    private float yaw;
    private float previousYaw;
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
        this.color = color & 0xFFFFFF;
        this.name = name;
        this.startPosition = position;
        this.startYaw = yaw;
        reset(null);
    }

    /** Back to the starting state: Ponder reuses element objects when a scene replays or seeks. 回到初始状态：重播或跳转时 Ponder 会复用元素对象。 */
    @Override
    public void reset(@Nullable PonderScene scene) {
        position = startPosition;
        previousPosition = startPosition;
        yaw = startYaw;
        previousYaw = startYaw;
        pitch = 0;
        held = ItemStack.EMPTY;
        charging = false;
        sneaking = false;
        age = 0;
        swingTicks = -1;
        fall = -1;
        player = null;
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

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public void setPitch(float degrees) {
        pitch = degrees;
    }

    public void hold(ItemStack stack) {
        held = stack.copy();
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
        charging = false;
        sneaking = false;
        held = ItemStack.EMPTY;
        swingTicks = -1;
    }

    public boolean isDown() {
        return fall >= 0;
    }

    @Override
    public void tick(PonderScene scene) {
        // Runs before this tick's instructions move the actor, so the walk cycle follows last tick's movement.
        // 在本 tick 的指令移动演员之前运行；步态跟随上一 tick 的位移。
        ActorPlayer actor = player();
        double moved = position.subtract(previousPosition).horizontalLength();
        previousPosition = position;
        previousYaw = yaw;
        age++;
        if (fall >= 0 && fall < FALL_TICKS) {
            fall++;
        }
        if (actor == null) {
            return;
        }
        // LivingEntity's limb and swing bookkeeping, without ticking the entity in the real world.
        // 复刻 LivingEntity 的肢体与挥手计数，但不在真实世界中 tick 这个实体。
        actor.limbAnimator.updateLimbs(isDown() ? 0 : Math.min(1, (float) moved * 4), 0.4f);
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
        MatrixStack ms = graphics.getMatrices();
        Vec3d at = previousPosition.lerp(position, pt);
        float bodyYaw = MathHelper.lerpAngleDegrees(pt, previousYaw, yaw);
        int light = lightCoordsFromFade(fade);
        if (isDown()) {
            ms.push();
            ms.translate(at.x, at.y, at.z);
            body(ms, buffer, bodyYaw, pt, light);
            ms.pop();
            return;
        }
        ActorPlayer actor = player();
        if (actor == null) {
            return;
        }
        pose(actor);
        EntityRenderDispatcher dispatcher = MinecraftClient.getInstance().getEntityRenderDispatcher();
        dispatcher.setRenderShadows(false);
        dispatcher.render(actor, at.x, at.y, at.z, bodyYaw, pt, ms, buffer, light);
        dispatcher.setRenderShadows(MinecraftClient.getInstance().options.getEntityShadows().getValue());
        if (name != null) {
            ms.push();
            ms.translate(at.x, at.y + 2.15, at.z);
            nameTag(world, ms, buffer, pt);
            ms.pop();
        }
    }

    /** Copy this tick's state into the entity the vanilla renderer reads. 把本 tick 的状态写入原版渲染器读取的实体。 */
    private void pose(ActorPlayer actor) {
        actor.setStackInHand(Hand.MAIN_HAND, held);
        actor.charging = charging && !held.isEmpty();
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
     * Wathe's PlayerBodyEntityRenderer: tip forward about the feet with a bounce-out ease, lifted 0.15 so the body
     * lies on the floor, arms at the sides.
     * 与 Wathe 的 PlayerBodyEntityRenderer 相同：以脚为轴向前扑倒，回弹缓动，抬高 0.15 让尸体躺在地面上，双臂贴身。
     */
    private void body(MatrixStack ms, VertexConsumerProvider buffer, float bodyYaw, float pt, int light) {
        float progress = bounceOut(Math.min(FALL_TICKS, fall + pt) / FALL_TICKS);
        ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180 - bodyYaw));
        ms.translate(0, progress * 0.15f, 0);
        ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-progress * 90));
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
        font.drawWithOutline(text, -font.getWidth(text) / 2f, -font.fontHeight, 0xFF000000 | color, 0xFF000000,
                ms.peek().getPositionMatrix(), buffer, LightmapTextureManager.MAX_LIGHT_COORDINATE);
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
            player = new ActorPlayer(world, color);
            playerWorld = world;
        }
        return player;
    }

    private static PlayerEntityModel<LivingEntity> bodyModel() {
        if (bodyModel == null) {
            bodyModel = new PlayerEntityModel<>(MinecraftClient.getInstance().getEntityModelLoader()
                    .getModelPart(EntityModelLayers.PLAYER), false);
        }
        return bodyModel;
    }
}
