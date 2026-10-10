package dev.caecorthus.sparkassist.client.ponder;

import java.util.Map;
import java.util.WeakHashMap;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.instruction.TickingInstruction;
import net.minecraft.util.math.MathHelper;

/**
 * Stage lighting for a blackout. Ponder lights its world at full brightness whatever the lamps do, but it dims a
 * section while that section fades in; holding every block section at a low fade therefore darkens the whole stage
 * evenly, and actors follow at a gentler level so they stay readable.
 * 停电时的舞台照明。无论灯怎么变，Ponder 都以满亮度照亮场景，但方块区域淡入时会变暗；把所有方块区域保持在较低的淡入值
 * 就能让整个舞台均匀变暗，演员则按较轻的程度一起变暗，保证仍然看得清。
 */
final class StageLights {
    private static final int FADE_TICKS = 8;
    /** Section fade while dark: Ponder maps fade 0..1 to light 5..15. 熄灯时方块区域的淡入值：Ponder 把 0..1 映射为亮度 5..15。 */
    private static final float DARK = 0;
    /** How far actors dim compared with the blocks. 演员相对方块变暗的程度。 */
    private static final float ACTOR_SHARE = 0.6f;
    private static final Map<PonderScene, Float> LEVELS = new WeakHashMap<>();

    private StageLights() {
    }

    /** Turn the stage lights off (dark) or back on over a few ticks. 在几 tick 内关掉（变暗）或重新打开舞台灯光。 */
    static void lights(SceneBuilder scene, boolean on) {
        scene.addInstruction(new Switch(on ? 1 : DARK));
    }

    /** The fade an actor should be lit as in {@code scene}: 1 with the lights on. 演员在该场景中应采用的亮度淡入值：灯亮时为 1。 */
    static float actorLevel(PonderScene scene) {
        float level = LEVELS.getOrDefault(scene, 1f);
        return 1 - (1 - level) * ACTOR_SHARE;
    }

    private static final class Switch extends TickingInstruction {
        private final float target;
        private float start;

        Switch(float target) {
            super(false, FADE_TICKS);
            this.target = target;
        }

        @Override
        public void reset(PonderScene scene) {
            super.reset(scene);
            LEVELS.remove(scene);
        }

        @Override
        protected void firstTick(PonderScene scene) {
            super.firstTick(scene);
            start = LEVELS.getOrDefault(scene, 1f);
            // A section translates by its fade vector while its fade is below 1; drop it so the stage stays put.
            // 方块区域淡入值低于 1 时会沿淡入方向偏移；清掉淡入方向，让舞台保持原位。
            scene.forEach(WorldSectionElement.class, section -> section.setFadeVec(null));
        }

        @Override
        public void tick(PonderScene scene) {
            super.tick(scene);
            float level = MathHelper.lerp(1 - remainingTicks / (float) totalTicks, start, target);
            LEVELS.put(scene, level);
            scene.forEach(WorldSectionElement.class, section -> section.forceApplyFade(level));
        }
    }
}
