package dev.caecorthus.sparkassist.client.ponder;

import dev.caecorthus.sparkassist.ponder.ProjectilePose;
import java.util.List;
import net.createmod.ponder.api.level.PonderLevel;
import net.fabricmc.fabric.api.client.model.loading.v1.FabricBakedModelManager;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.element.AnimatedSceneElementBase;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;

/**
 * One of SparkWitch's standalone 3D projectile models (the Ninja's hook head, a Potion Gunner shell) moving from
 * {@code from} to {@code to} over {@code ticks} with its nose along the path, then held there or gone, drawn as
 * SparkWitch's own renderer draws it. SparkWitch bakes these models through Fabric's ModelLoadingPlugin; while one is
 * not baked (SparkWitch missing or older than its 3D projectiles, a broken resource pack) the element draws its
 * {@code fallback} sprite instead, so the demo still plays.
 * SparkWitch 的独立 3D 投射物模型之一（忍者钩头、药炮手炮弹），在 ticks 内从 from 移到 to、头部沿路径方向，之后停在那里
 * 或消失，画法与 SparkWitch 自己的渲染器相同。这些模型由 SparkWitch 经 Fabric ModelLoadingPlugin 烘焙；某个模型未烘焙时
 * （没装 SparkWitch、版本早于 3D 投射物、资源包损坏），改画 fallback 贴图，演示照常播放。
 */
public final class ProjectileModelElement extends AnimatedSceneElementBase {
    private static final long QUAD_SEED = 42;
    // Render thread only; reseeded before every getQuads call. 仅渲染线程使用；每次 getQuads 前重新设种子。
    private static final Random QUAD_RANDOM = Random.create();
    private static final Direction[] DIRECTIONS = Direction.values();
    private final Identifier model;
    private final RenderLayer layer;
    /** An additive full-bright glow drawn over the model, or null. 叠加在模型上的加色全亮光效，没有则为 null。 */
    @Nullable
    private final Identifier glow;
    private final float scale;
    /** How far above the path the model's pivot sits. 模型枢轴高出路径多少。 */
    private final double lift;
    private final boolean spins;
    /** How far behind the pivot a line ties on, 0 for none. 铁链系点在枢轴后方多远，没有则为 0。 */
    private final double tie;
    private final Vec3d from;
    private final Vec3d to;
    private final int ticks;
    private final boolean stays;
    private final SpriteElement fallback;
    /** Where the hand holding this projectile's line was last tick, or null. 上一 tick 握住其铁链的手的位置，没有则为 null。 */
    @Nullable
    private Vec3d hand;
    private int age;

    private ProjectileModelElement(Identifier model, RenderLayer layer, @Nullable Identifier glow, float scale,
                                   double lift, boolean spins, double tie, Vec3d from, Vec3d to, int ticks,
                                   boolean stays, SpriteElement fallback) {
        this.model = model;
        this.layer = layer;
        this.glow = glow;
        this.scale = scale;
        this.lift = lift;
        this.spins = spins;
        this.tie = tie;
        this.from = from;
        this.to = to;
        this.ticks = Math.max(1, ticks);
        this.stays = stays;
        this.fallback = fallback;
    }

    /**
     * The Ninja's hook head flying to the latch point {@code to} and staying there, as
     * NinjaGrapplingHookEntityRenderer.renderHead draws it: the crown tip on the path (the block hit point once
     * latched), the claws along the flight, then away from the hand holding its chain ({@link #lineEnd}); culled
     * cutout on the block atlas, at full size, no spin.
     * 飞到钩点 to 并停在那里的忍者钩头，画法与 NinjaGrapplingHookEntityRenderer.renderHead 相同：钩冠尖端在路径上（钩住后
     * 即方块命中点），爪尖先沿飞行方向，钩住后背离握着铁链的手（lineEnd）；方块图集上的剔除背面镂空层，原大，不自旋。
     */
    static ProjectileModelElement hookHead(Vec3d from, Vec3d to, int ticks, SpriteElement fallback) {
        return new ProjectileModelElement(ProjectilePose.HOOK_MODEL,
                RenderLayer.getEntityCutout(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE), null, 1, 0, false,
                ProjectilePose.HOOK_RING_DISTANCE, from, to, ticks, true, fallback);
    }

    /**
     * A Potion Gunner shell of {@code type} flying from {@code from} to {@code to} and gone on impact, as
     * PotionShellEntityRenderer draws it: centred 0.125 above the path, nose along the flight, spinning, at 0.75 scale,
     * with the additive nozzle glow on the eyes layer. The body uses the immediate no-cull cutout layer, which is
     * flushed before the glow; the fixed entity-cutout buffer would be drawn last and paint over it. Vanilla's launch
     * hide does not apply: the demo's camera is never the shooter.
     * 种类为 type、从 from 飞到 to、命中即消失的药炮手炮弹，画法与 PotionShellEntityRenderer 相同：中心在路径上方 0.125，
     * 弹头沿飞行方向，自旋，缩放 0.75，并在眼睛渲染层叠加加色喷口尾焰。弹体用即时的不剔除镂空层，会在尾焰之前提交；固定的
     * 实体镂空缓冲最后才绘制，会盖掉尾焰。原版的出膛隐藏不适用：演示镜头从来不是射手。
     */
    static ProjectileModelElement shell(String type, Vec3d from, Vec3d to, int ticks, SpriteElement fallback) {
        return new ProjectileModelElement(ProjectilePose.shellModel(type),
                RenderLayer.getEntityCutoutNoCull(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE),
                ProjectilePose.SHELL_FLARE_MODEL, ProjectilePose.SHELL_SCALE, ProjectilePose.SHELL_CENTRE, true, 0,
                from, to, ticks, false, fallback);
    }

    /**
     * Where a line held at {@code hand} meets this projectile this tick: the hook's ring behind the crown tip, or the
     * hook position itself while the fallback sprite is drawn. A landed head turns away from {@code hand}.
     * 握在 hand 的铁链这一 tick 连到投射物的哪一点：钩冠尖端后方的圆环；画后备贴图时就是钩爪位置本身。钩住的钩头会背离 hand。
     */
    Vec3d lineEnd(Vec3d hand) {
        this.hand = hand;
        Vec3d at = at(age);
        if (tie == 0 || baked(model) == null) {
            return at;
        }
        return at.add(facing(age).multiply(-tie));
    }

    @Override
    public void reset(@Nullable PonderScene scene) {
        age = 0;
        hand = null;
        fallback.reset(scene);
    }

    @Override
    public void tick(PonderScene scene) {
        age++;
        fallback.tick(scene);
    }

    private Vec3d at(float time) {
        return from.lerp(to, Math.min(1, time / ticks));
    }

    private Vec3d facing(float time) {
        Vec3d handToHook = hand == null ? Vec3d.ZERO : at(time).subtract(hand);
        return ProjectilePose.facing(time < ticks, handToHook, to.subtract(from));
    }

    @Override
    protected void renderLast(PonderLevel world, VertexConsumerProvider buffer, DrawContext graphics, float fade,
                              float pt) {
        float time = age + pt;
        if (fade <= 0.01f || time >= ticks && !stays) {
            return;
        }
        BakedModel body = baked(model);
        if (body == null) {
            fallback.renderLast(world, buffer, graphics, fade, pt);
            return;
        }
        Vec3d at = at(time);
        Vec3d facing = facing(time);
        MatrixStack ms = graphics.getMatrices();
        ms.push();
        ms.translate(at.x, at.y + lift, at.z);
        ms.multiply(RotationAxis.POSITIVE_Y.rotation(ProjectilePose.yaw(facing.x, facing.z)));
        ms.multiply(RotationAxis.POSITIVE_X.rotation(ProjectilePose.pitch(facing.x, facing.y, facing.z)));
        if (spins) {
            ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(ProjectilePose.shellRollDegrees(time)));
        }
        ms.scale(scale, scale, scale);
        draw(body, ms, buffer.getBuffer(layer), lightCoordsFromFade(fade));
        BakedModel glowModel = glow == null ? null : baked(glow);
        if (glowModel != null) {
            draw(glowModel, ms, buffer.getBuffer(RenderLayer.getEyes(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE)),
                    LightmapTextureManager.MAX_LIGHT_COORDINATE);
        }
        ms.pop();
    }

    /**
     * The baked standalone model {@code id}, or null when SparkWitch did not add it (Fabric keeps no entry then).
     * SparkWitch 加入的独立模型 id 的烘焙结果；SparkWitch 没有加入时为 null（此时 Fabric 没有对应条目）。
     */
    @Nullable
    private static BakedModel baked(Identifier id) {
        return ((FabricBakedModelManager) MinecraftClient.getInstance().getBakedModelManager()).getModel(id);
    }

    /** Every quad of {@code model} with its pivot (8, 8, 8) at the origin, untinted (ProjectileModels.render). 以原点为枢轴 (8, 8, 8) 绘制模型的全部 quad，不着色。 */
    private static void draw(BakedModel model, MatrixStack ms, VertexConsumer consumer, int light) {
        ms.push();
        ms.translate(-0.5f, -0.5f, -0.5f);
        MatrixStack.Entry entry = ms.peek();
        for (Direction direction : DIRECTIONS) {
            QUAD_RANDOM.setSeed(QUAD_SEED);
            emit(model.getQuads(null, direction, QUAD_RANDOM), entry, consumer, light);
        }
        QUAD_RANDOM.setSeed(QUAD_SEED);
        emit(model.getQuads(null, null, QUAD_RANDOM), entry, consumer, light);
        ms.pop();
    }

    private static void emit(List<BakedQuad> quads, MatrixStack.Entry entry, VertexConsumer consumer, int light) {
        for (BakedQuad quad : quads) {
            consumer.quad(entry, quad, 1, 1, 1, 1, light, OverlayTexture.DEFAULT_UV);
        }
    }
}
