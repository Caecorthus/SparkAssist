package dev.caecorthus.sparkassist.client.ponder;

import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.element.AnimatedSceneElementBase;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

/**
 * A thrown item on a fixed arc (a grenade, an axe, a capsule), drawn with the item's ground model. Scripted rather
 * than simulated, so every replay lands in the same spot; once it lands it either stays or vanishes.
 * 沿固定弧线飞行的投掷物（手雷、飞斧、胶囊），使用物品的掉落物模型绘制。轨迹由脚本决定而非物理模拟，
 * 每次重播都落在同一处；落地后留在原处或消失。
 */
public final class ThrownElement extends AnimatedSceneElementBase {
    private static final float SPIN_PER_TICK = 40;

    private final ItemStack stack;
    private final Vec3d from;
    private final Vec3d to;
    private final int ticks;
    private final double arc;
    private final boolean stays;
    private final Flight flight;
    private int age;

    /** How the item looks on its way and once down. 物品飞行中与落地后的样子。 */
    public enum Flight {
        /** Tumbles end over end and stays as it landed (an axe, a knife). 翻滚飞行，落地后保持落地姿态（飞斧、飞刀）。 */
        SPIN,
        /** Flies upright without turning, like a thrown-item sprite (a grenade). 竖直不转地飞行，如投掷物贴图（手雷）。 */
        UPRIGHT,
        /** Flies upright, then lies flat on the floor (a firecracker set down). 竖直飞行，落地后平躺在地上（放下的爆竹）。 */
        LIES_FLAT
    }

    ThrownElement(ItemStack stack, Vec3d from, Vec3d to, int ticks, double arc, boolean stays, Flight flight) {
        this.stack = stack.copy();
        this.from = from;
        this.to = to;
        this.ticks = Math.max(1, ticks);
        this.arc = arc;
        this.stays = stays;
        this.flight = flight;
    }

    @Override
    public void reset(@Nullable PonderScene scene) {
        age = 0;
    }

    @Override
    public void tick(PonderScene scene) {
        age++;
    }

    private boolean landed(float time) {
        return time >= ticks;
    }

    private Vec3d at(float time) {
        float t = Math.min(1, time / ticks);
        return from.lerp(to, t).add(0, arc * 4 * t * (1 - t), 0);
    }

    @Override
    protected void renderLast(PonderLevel world, VertexConsumerProvider buffer, DrawContext graphics, float fade,
                              float pt) {
        float time = age + pt;
        if (fade <= 0.01f || landed(time) && !stays) {
            return;
        }
        Vec3d at = at(time);
        MatrixStack ms = graphics.getMatrices();
        ms.push();
        ms.translate(at.x, at.y, at.z);
        Vec3d heading = to.subtract(from);
        ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(
                (float) (MathHelper.atan2(heading.x, heading.z) * MathHelper.DEGREES_PER_RADIAN)));
        if (flight == Flight.SPIN) {
            ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(Math.min(time, ticks) * SPIN_PER_TICK));
        } else if (flight == Flight.LIES_FLAT && landed(time)) {
            ms.translate(0, 0.02, 0);
            ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90));
        }
        MinecraftClient.getInstance().getItemRenderer().renderItem(stack, ModelTransformationMode.GROUND,
                lightCoordsFromFade(fade), OverlayTexture.DEFAULT_UV, ms, buffer, world, 0);
        ms.pop();
    }
}
