package dev.caecorthus.sparkassist.client.ponder;

import java.util.function.Consumer;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.instruction.FadeIntoSceneInstruction;
import net.createmod.ponder.foundation.element.ElementLinkImpl;
import net.createmod.ponder.foundation.instruction.TickingInstruction;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
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

    /** Turn to {@code yaw} over a few ticks (a half turn takes five). 在几 tick 内转向 yaw（转身半圈用五 tick）。 */
    public static void turn(SceneBuilder scene, ElementLink<ActorElement> actor, float yaw) {
        apply(scene, actor, element -> element.turnTo(yaw));
    }

    /**
     * Move by {@code delta} over {@code ticks} without turning: a step back, a shove, a body dragged along.
     * 在 ticks 内平移 delta 而不转身：后退、被推开、尸体被拖动。
     */
    public static void slide(SceneBuilder scene, ElementLink<ActorElement> actor, Vec3d delta, int ticks) {
        scene.addInstruction(new Walk(actor, delta, Float.NaN, ticks));
    }

    public static void hold(SceneBuilder scene, ElementLink<ActorElement> actor, ItemStack stack) {
        apply(scene, actor, element -> element.hold(stack));
    }

    /** Put on (or, with an empty stack, take off) chest armour. 穿上（空物品则脱下）胸甲。 */
    public static void wear(SceneBuilder scene, ElementLink<ActorElement> actor, ItemStack stack) {
        apply(scene, actor, element -> element.wear(stack));
    }

    /** Change colour and name tag in place. 原地改换颜色与名牌。 */
    public static void retint(SceneBuilder scene, ElementLink<ActorElement> actor, int color, @Nullable Text name) {
        apply(scene, actor, element -> element.retint(color, name));
    }

    /** Wathe's psycho mode look on or off. 开启或关闭 Wathe 疯魔模式外观。 */
    public static void psycho(SceneBuilder scene, ElementLink<ActorElement> actor, boolean psycho) {
        apply(scene, actor, element -> element.setPsycho(psycho));
    }

    /** Gone at once, no fade (a body bagged). 立刻消失，不淡出（如尸体被装袋）。 */
    public static void vanish(SceneBuilder scene, ElementLink<ActorElement> actor) {
        apply(scene, actor, element -> element.setVisible(false));
    }

    /** Vanilla invisibility on or off. 开启或关闭原版隐身。 */
    public static void invisible(SceneBuilder scene, ElementLink<ActorElement> actor, boolean invisible) {
        apply(scene, actor, element -> element.setInvisible(invisible));
    }

    /** Lie down in a bed (head block, foot-to-head facing). 躺进床里（床头方块、床尾指向床头的朝向）。 */
    public static void lieDown(SceneBuilder scene, ElementLink<ActorElement> actor, BlockPos head, Direction facing) {
        apply(scene, actor, element -> element.lieDown(head, facing));
    }

    public static void getUp(SceneBuilder scene, ElementLink<ActorElement> actor) {
        apply(scene, actor, ActorElement::getUp);
    }

    /**
     * A box in {@code color} that follows the actor for {@code ticks}: stands in for the glowing outline a role or
     * instinct shows through walls (the vanilla outline shader does not run inside Ponder).
     * 跟随演员 ticks 的 color 色方框：代替职业能力或本能隔墙显示的发光轮廓（Ponder 中无法运行原版轮廓着色器）。
     */
    public static void highlight(SceneBuilder scene, ElementLink<ActorElement> actor, int color, int ticks) {
        scene.addInstruction(new Highlight(actor, color, ticks));
    }

    /**
     * Throw {@code stack} from {@code from} to {@code to} over {@code ticks}, {@code arc} blocks above the straight
     * line at mid-flight; it stays where it lands until {@link #remove}d if {@code stays}.
     * 在 ticks 内把 stack 从 from 抛到 to，飞行中点高出直线 arc 格；stays 为 true 时落地后留在原处，直到 remove。
     */
    public static ElementLink<ThrownElement> toss(SceneBuilder scene, ItemStack stack, Vec3d from, Vec3d to, int ticks,
                                                  double arc, boolean stays, ThrownElement.Flight flight) {
        ThrownElement element = new ThrownElement(stack, from, to, ticks, arc, stays, flight);
        ElementLink<ThrownElement> link = new ElementLinkImpl<>(ThrownElement.class);
        scene.addInstruction(ponder -> {
            element.setVisible(true);
            element.setFade(1);
            ponder.addElement(element);
            ponder.linkElement(element, link);
        });
        return link;
    }

    public static void remove(SceneBuilder scene, ElementLink<ThrownElement> thrown) {
        scene.addInstruction(ponder -> {
            ThrownElement element = ponder.resolve(thrown);
            if (element != null) {
                element.setVisible(false);
            }
        });
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

    /**
     * Hands the walk to the actor, which takes the steps itself from the next tick on; the instruction only keeps the
     * walk's length on the timeline. 把行走交给演员，演员从下一 tick 起自己迈步；本指令只在时间线上占住行走的时长。
     */
    private static final class Walk extends TickingInstruction {
        private final ElementLink<ActorElement> link;
        private final Vec3d delta;
        private final float heading;

        /** @param heading the yaw to face while walking, or NaN to keep the current one / 行走时的朝向，NaN 表示保持当前朝向 */
        Walk(ElementLink<ActorElement> link, Vec3d delta, float heading, int ticks) {
            super(false, ticks);
            this.link = link;
            this.delta = delta;
            this.heading = heading;
        }

        @Override
        protected void firstTick(PonderScene scene) {
            super.firstTick(scene);
            ActorElement actor = scene.resolve(link);
            if (actor != null) {
                actor.walk(delta, totalTicks, heading);
            }
        }
    }

    private static final class Highlight extends TickingInstruction {
        private final ElementLink<ActorElement> link;
        private final int color;
        private final Object slot = new Object();

        Highlight(ElementLink<ActorElement> link, int color, int ticks) {
            super(false, ticks);
            this.link = link;
            this.color = color;
        }

        @Override
        public void tick(PonderScene scene) {
            super.tick(scene);
            ActorElement actor = scene.resolve(link);
            if (actor == null) {
                return;
            }
            Vec3d at = actor.position();
            Box box = actor.isDown()
                    ? new Box(at.x - 1, at.y, at.z - 1, at.x + 1, at.y + 0.4, at.z + 1)
                    : new Box(at.x - 0.4, at.y, at.z - 0.4, at.x + 0.4, at.y + 1.95, at.z + 0.4);
            scene.getOutliner().chaseAABB(slot, box).lineWidth(1 / 16f).colored(color);
        }
    }
}
