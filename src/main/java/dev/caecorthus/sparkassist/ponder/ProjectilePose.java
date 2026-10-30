package dev.caecorthus.sparkassist.ponder;

import java.util.Set;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

/**
 * What the demos copy from SparkWitch's 3D projectile renderers (ProjectileModels, ProjectileModelPose,
 * NinjaGrapplingHeadPose, PotionShellModelPose, PotionShellEntityRenderer): the standalone model ids, the yaw and pitch
 * that turn a model's nose (authored pointing north, -Z) onto a direction, where the hook head points and where its
 * chain ties on, and the shell's spin. Copied by hand, so re-check them whenever SparkWitch changes its models.
 * 演示从 SparkWitch 的 3D 投射物渲染器照搬的内容：独立模型 id、把朝北（-Z）建模的模型头部转到某方向所需的偏航与俯仰、
 * 钩头朝向与铁链系点，以及炮弹的自旋。手工照搬，SparkWitch 修改模型时需复核。
 */
public final class ProjectilePose {
    /** The Ninja's hook head model, pivot at the crown tip. 忍者钩头模型，枢轴在钩冠尖端。 */
    public static final Identifier HOOK_MODEL = Identifier.of("sparkwitch", "item/projectile/ninja_grappling_hook");
    /**
     * The chain ties into the middle of the ring's back bar, this far behind the crown tip
     * (NinjaGrapplingHeadPose.RING_DISTANCE). 铁链系在圆环后横杆的中点，位于钩冠尖端后方这么远。
     */
    public static final double HOOK_RING_DISTANCE = 10.125 / 16;
    /**
     * A hand-to-hook vector shorter than this cannot aim a landed head (NinjaGrapplingHeadPose.MIN_AIM_LENGTH).
     * 手到钩爪的向量短于此值时无法用于钩住后的钩头朝向。
     */
    public static final double HOOK_MIN_AIM_LENGTH = 0.05;
    /** The shells' additive nozzle glow, one for every type. 炮弹加色的喷口尾焰，各种类共用。 */
    public static final Identifier SHELL_FLARE_MODEL = Identifier.of("sparkwitch", "item/projectile/potion_shell_flare");
    /** The 14-pixel shell drawn about 0.66 blocks long (PotionShellEntityRenderer.MODEL_SCALE). 14 像素的炮弹约 0.66 格长。 */
    public static final float SHELL_SCALE = 0.75f;
    /** Half the shell entity's 0.25 height: the model is drawn at its centre. 炮弹实体 0.25 高度的一半：模型画在其中心。 */
    public static final double SHELL_CENTRE = 0.125;
    /** PotionShellModelPose.SPIN_DEGREES_PER_TICK. 每 tick 自旋 30 度。 */
    public static final float SHELL_SPIN_DEGREES_PER_TICK = 30;
    /** PotionShellType keys. PotionShellType 的种类键。 */
    private static final Set<String> SHELL_TYPES = Set.of("dk", "ac", "mr", "tr");
    private static final double HORIZONTAL_EPSILON = 1.0e-6;
    private static final double MIN_FALLBACK_LENGTH_SQ = 1.0e-8;
    private static final Vec3d NORTH = new Vec3d(0, 0, -1);

    private ProjectilePose() {
    }

    /**
     * The flying model of the shell type {@code type} (dk, ac, mr or tr), {@code potion_shell_<type>}.
     * 种类为 type（dk、ac、mr 或 tr）的炮弹飞行模型 {@code potion_shell_<type>}。
     */
    public static Identifier shellModel(String type) {
        if (!SHELL_TYPES.contains(type)) {
            throw new IllegalArgumentException("Unknown potion shell type: " + type);
        }
        return Identifier.of("sparkwitch", "item/projectile/potion_shell_" + type);
    }

    /** Radians about +Y that turn north (0, 0, -1) toward {@code (dx, dz)}. 绕 +Y 把北方 (0, 0, -1) 转向 (dx, dz) 的弧度。 */
    public static float yaw(double dx, double dz) {
        if (Math.abs(dx) < HORIZONTAL_EPSILON && Math.abs(dz) < HORIZONTAL_EPSILON) {
            return 0;
        }
        return (float) Math.atan2(-dx, -dz);
    }

    /** Radians about +X that lift the nose to {@code dy}; positive is up. 绕 +X 把头部抬到 dy 的弧度，正值向上。 */
    public static float pitch(double dx, double dy, double dz) {
        return (float) Math.atan2(dy, Math.sqrt(dx * dx + dz * dz));
    }

    /**
     * Unit direction of a projectile's nose: along its {@code flight} while {@code flying}; once landed, away from the
     * hand holding its line ({@code handToHook}), so a latched hook head pivots as its holder moves. Each falls back to
     * the other, then north.
     * 投射物头部的单位方向：飞行中沿 flight；落定后背离握住其铁链的手（handToHook），因此钩住的钩头会随持有者移动而转动。
     * 两者互为后备，最后取北方。
     */
    public static Vec3d facing(boolean flying, Vec3d handToHook, Vec3d flight) {
        boolean flightUsable = flight.lengthSquared() > MIN_FALLBACK_LENGTH_SQ;
        if (flying && flightUsable) {
            return flight.normalize();
        }
        if (handToHook.lengthSquared() >= HOOK_MIN_AIM_LENGTH * HOOK_MIN_AIM_LENGTH) {
            return handToHook.normalize();
        }
        return flightUsable ? flight.normalize() : NORTH;
    }

    /** Where the hook's chain starts relative to its crown tip, for a unit {@code facing}. 给定单位朝向时铁链起点相对钩冠尖端的偏移。 */
    public static Vec3d ringOffset(Vec3d facing) {
        return facing.multiply(-HOOK_RING_DISTANCE);
    }

    /**
     * The shell's roll about its own axis {@code ticks} after launch, wrapped to (-360, 0]: clockwise seen from behind
     * (PotionShellModelPose.rollDegrees).
     * 炮弹出膛 ticks 后绕自身轴的横滚角，限制在 (-360, 0]：从后方看为顺时针。
     */
    public static float shellRollDegrees(float ticks) {
        float turned = (ticks * SHELL_SPIN_DEGREES_PER_TICK) % 360;
        return turned == 0 ? 0 : -turned;
    }
}
