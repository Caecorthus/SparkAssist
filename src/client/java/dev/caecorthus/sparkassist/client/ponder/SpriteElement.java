package dev.caecorthus.sparkassist.client.ponder;

import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.element.AnimatedSceneElementBase;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

/**
 * A picture that always faces the viewer, moving from {@code from} to {@code to} over {@code ticks}, then held there
 * or gone: a thrown item as vanilla's FlyingItemEntityRenderer draws one (a Shuriken, a Shock Device, a capsule), or
 * a square of an entity's own texture (the Ninja's old hook head, drawn when SparkWitch has no 3D head). The path may
 * rise in an arc and leave a client trail; it is scripted, so every replay lands in the same spot.
 * 始终朝向观看者的图片，在 ticks 内从 from 移到 to，之后停在那里或消失：像原版 FlyingItemEntityRenderer 那样绘制的投掷物
 * （手里剑、电击装置、胶囊），或实体自己贴图的一块方形（忍者的旧钩头，SparkWitch 没有 3D 钩头时绘制）。路径可以拱成弧线
 * 并带客户端拖尾；轨迹由脚本决定，每次重播都落在同一处。
 */
public final class SpriteElement extends AnimatedSceneElementBase {
    /** A trail particle shows on about 30% of ticks (ShockDeviceEntity's electric spark). 约 30% 的 tick 冒一颗拖尾粒子。 */
    private static final float TRAIL_CHANCE = 0.3f;
    @Nullable
    private final ItemStack stack;
    @Nullable
    private final Identifier texture;
    private final float size;
    private final Vec3d from;
    private final Vec3d to;
    private final int ticks;
    /** The arc's rise over the straight line at mid-flight. 弧线中点高出直线的高度。 */
    private final double arc;
    private final boolean stays;
    @Nullable
    private final ParticleEffect trail;
    private int age;

    private SpriteElement(@Nullable ItemStack stack, @Nullable Identifier texture, float size, Vec3d from, Vec3d to,
                          int ticks, double arc, boolean stays, @Nullable ParticleEffect trail) {
        this.stack = stack == null ? null : stack.copy();
        this.texture = texture;
        this.size = size;
        this.from = from;
        this.to = to;
        this.ticks = Math.max(1, ticks);
        this.arc = arc;
        this.stays = stays;
        this.trail = trail;
    }

    /** {@code stack} on a straight path, held at {@code to} if it {@code stays}. stack 沿直线飞行，stays 为 true 时停在 to。 */
    static SpriteElement item(ItemStack stack, Vec3d from, Vec3d to, int ticks, boolean stays) {
        return new SpriteElement(stack, null, 0, from, to, ticks, 0, stays, null);
    }

    /**
     * {@code stack} thrown on an arc {@code arc} above the straight line at mid-flight, gone on landing, with an
     * optional trail particle (null for none).
     * stack 沿飞行中点高出直线 arc 的弧线抛出，落地即消失，可带拖尾粒子（没有则为 null）。
     */
    static SpriteElement thrown(ItemStack stack, Vec3d from, Vec3d to, int ticks, double arc,
                                @Nullable ParticleEffect trail) {
        return new SpriteElement(stack, null, 0, from, to, ticks, arc, false, trail);
    }

    /**
     * A {@code size} square of {@code texture} centred on the path, held at {@code to} if it {@code stays}.
     * 以路径为中心、边长 size 的 texture 方形贴图，stays 为 true 时停在 to。
     */
    static SpriteElement picture(Identifier texture, float size, Vec3d from, Vec3d to, int ticks, boolean stays) {
        return new SpriteElement(null, texture, size, from, to, ticks, 0, stays, null);
    }

    /**
     * Turn {@code ms} to face the viewer. Ponder maps the scene to the screen with its two camera rotations and a y
     * flip, so undoing those leaves plain screen axes (y down), as text wants; a picture flips y back to stand upright.
     * 让 ms 朝向观看者。Ponder 用两次镜头旋转加一次 y 翻转把场景映射到屏幕，抵消后就是普通屏幕坐标（y 朝下），文字直接
     * 使用；图片再把 y 翻回来才是正立的。
     */
    static void faceCamera(PonderLevel world, MatrixStack ms, float pt) {
        ms.scale(1, -1, 1);
        if (world.scene != null) {
            PonderScene.SceneTransform transform = world.scene.getTransform();
            ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-transform.yRotation.getValue(pt)));
            ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-transform.xRotation.getValue(pt)));
        }
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
        if (fade <= 0.01f || time >= ticks && !stays) {
            return;
        }
        Vec3d at = at(time);
        MatrixStack ms = graphics.getMatrices();
        ms.push();
        ms.translate(at.x, at.y, at.z);
        faceCamera(world, ms, pt);
        ms.scale(1, -1, 1);
        int light = lightCoordsFromFade(fade);
        if (stack != null) {
            MinecraftClient.getInstance().getItemRenderer().renderItem(stack, ModelTransformationMode.GROUND, light,
                    OverlayTexture.DEFAULT_UV, ms, buffer, world, 0);
        } else if (texture != null) {
            ms.scale(size, size, size);
            MatrixStack.Entry entry = ms.peek();
            VertexConsumer consumer = buffer.getBuffer(RenderLayer.getEntityCutoutNoCull(texture));
            corner(consumer, entry, light, 0, 0, 0, 1);
            corner(consumer, entry, light, 1, 0, 1, 1);
            corner(consumer, entry, light, 1, 1, 1, 0);
            corner(consumer, entry, light, 0, 1, 0, 0);
        }
        ms.pop();
    }

    private static void corner(VertexConsumer consumer, MatrixStack.Entry entry, int light, float x, float y, float u,
                               float v) {
        consumer.vertex(entry, x - 0.5f, y - 0.5f, 0)
                .color(0xFFFFFFFF)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(entry, 0, 1, 0);
    }
}
