package dev.caecorthus.sparkassist.client.ponder;

import java.util.function.Consumer;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.instruction.FadeIntoSceneInstruction;
import net.createmod.ponder.foundation.instruction.TickingInstruction;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

/**
 * Scene-script verbs for {@link ActorElement}s. Movements run alongside the following instructions (add an
 * {@code idle} to wait for them); pose changes take effect on the next tick.
 * 场景脚本中驱动演员的动作。移动与后续指令并行（需要等待时加 idle）；姿势变化在下一 tick 生效。
 */
public final class Actors {
    private Actors() {
    }

    /** Fade an actor in at {@code feet}, dropping from {@code from}. 在 feet 处淡入演员，从 from 方向落入。 */
    public static ElementLink<ActorElement> enter(SceneBuilder scene, int color, @Nullable Text name, Vec3d feet,
                                                  float yaw, Direction from) {
        Enter instruction = new Enter(new ActorElement(color, name, feet, yaw), from);
        scene.addInstruction(instruction);
        return instruction.createLink(scene.getScene());
    }

    /** Walk by {@code delta} over {@code ticks}, turning to face the way. 在 ticks 内走过 delta，并转向前进方向。 */
    public static void walk(SceneBuilder scene, ElementLink<ActorElement> actor, Vec3d delta, int ticks) {
        float heading = (float) (MathHelper.atan2(-delta.x, delta.z) * MathHelper.DEGREES_PER_RADIAN);
        scene.addInstruction(new Walk(actor, delta, heading, ticks));
    }

    /** Turn to {@code yaw} over a few ticks. 在几 tick 内转向 yaw。 */
    public static void turn(SceneBuilder scene, ElementLink<ActorElement> actor, float yaw) {
        scene.addInstruction(new Turn(actor, yaw, 5));
    }

    public static void hold(SceneBuilder scene, ElementLink<ActorElement> actor, ItemStack stack) {
        apply(scene, actor, element -> element.hold(stack));
    }

    /** Start or stop holding right-click with the held item. 开始或停止按住右键使用手持物品。 */
    public static void charge(SceneBuilder scene, ElementLink<ActorElement> actor, boolean charging) {
        apply(scene, actor, element -> element.setCharging(charging));
    }

    public static void sneak(SceneBuilder scene, ElementLink<ActorElement> actor, boolean sneaking) {
        apply(scene, actor, element -> element.setSneaking(sneaking));
    }

    public static void lookPitch(SceneBuilder scene, ElementLink<ActorElement> actor, float degrees) {
        apply(scene, actor, element -> element.setPitch(degrees));
    }

    public static void swing(SceneBuilder scene, ElementLink<ActorElement> actor) {
        apply(scene, actor, ActorElement::swing);
    }

    public static void fall(SceneBuilder scene, ElementLink<ActorElement> actor) {
        apply(scene, actor, ActorElement::fall);
    }

    /** Fade the actor out towards {@code to}. 演员朝 to 方向淡出。 */
    public static void leave(SceneBuilder scene, ElementLink<ActorElement> actor, Direction to) {
        scene.special().hideElement(actor, to);
    }

    private static void apply(SceneBuilder scene, ElementLink<ActorElement> actor, Consumer<ActorElement> change) {
        scene.addInstruction(ponder -> {
            ActorElement element = ponder.resolve(actor);
            if (element != null) {
                change.accept(element);
            }
        });
    }

    private static final class Enter extends FadeIntoSceneInstruction<ActorElement> {
        Enter(ActorElement element, Direction from) {
            super(15, from, element);
        }

        @Override
        protected Class<ActorElement> getElementClass() {
            return ActorElement.class;
        }
    }

    private static final class Walk extends TickingInstruction {
        private final ElementLink<ActorElement> link;
        private final Vec3d step;
        private final float heading;
        @Nullable
        private ActorElement actor;

        Walk(ElementLink<ActorElement> link, Vec3d delta, float heading, int ticks) {
            super(false, ticks);
            this.link = link;
            this.step = delta.multiply(1.0 / ticks);
            this.heading = heading;
        }

        @Override
        protected void firstTick(PonderScene scene) {
            super.firstTick(scene);
            actor = scene.resolve(link);
            if (actor != null) {
                actor.setYaw(heading);
            }
        }

        @Override
        public void tick(PonderScene scene) {
            super.tick(scene);
            if (actor != null) {
                actor.moveTo(actor.position().add(step));
            }
        }
    }

    private static final class Turn extends TickingInstruction {
        private final ElementLink<ActorElement> link;
        private final float target;
        @Nullable
        private ActorElement actor;
        private float start;

        Turn(ElementLink<ActorElement> link, float target, int ticks) {
            super(false, ticks);
            this.link = link;
            this.target = target;
        }

        @Override
        protected void firstTick(PonderScene scene) {
            super.firstTick(scene);
            actor = scene.resolve(link);
            if (actor != null) {
                start = actor.yaw();
            }
        }

        @Override
        public void tick(PonderScene scene) {
            super.tick(scene);
            if (actor != null) {
                float progress = 1 - remainingTicks / (float) totalTicks;
                actor.setYaw(start + MathHelper.wrapDegrees(target - start) * progress);
            }
        }
    }
}
