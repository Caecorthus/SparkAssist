package dev.caecorthus.sparkassist.client.ponder;

import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.instruction.TickingInstruction;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

/**
 * Scripted particles for effects the game only plays server-side or only for some viewers (an explosion, the poison
 * marker killers see): Ponder's world never runs that code, so scenes add the particles themselves.
 * 脚本化的粒子效果，用于游戏只在服务端播放、或只有部分人看得到的效果（爆炸、杀手才看得到的中毒标记）：
 * Ponder 的世界不会运行那些代码，所以由场景自己添加粒子。
 */
final class Effects {
    private static final Random RANDOM = Random.create();

    private Effects() {
    }

    /** {@code count} particles at once, flying out in random directions at up to {@code speed}. 一次放出 count 个粒子，以至多 speed 的速度向随机方向飞散。 */
    static void burst(SceneBuilder scene, ParticleEffect particle, Vec3d at, int count, double speed) {
        scene.effects().emitParticles(at, (world, x, y, z) -> {
            double yaw = RANDOM.nextDouble() * MathHelper.TAU;
            double pitch = (RANDOM.nextDouble() - 0.5) * Math.PI;
            double v = speed * (0.3 + 0.7 * RANDOM.nextDouble());
            world.addParticle(particle, x, y, z, Math.cos(yaw) * Math.cos(pitch) * v, Math.sin(pitch) * v,
                    Math.sin(yaw) * Math.cos(pitch) * v);
        }, count, 1);
    }

    /** A steady trickle: {@code perTick} particles (fractions allowed) drifting with {@code motion} for {@code ticks}. 持续冒出：ticks 内每 tick 放出 perTick 个（可为小数）随 motion 漂移的粒子。 */
    static void trickle(SceneBuilder scene, ParticleEffect particle, Vec3d at, Vec3d motion, float perTick, int ticks) {
        scene.effects().emitParticles(at, (world, x, y, z) -> world.addParticle(particle,
                x + (RANDOM.nextDouble() - 0.5) * 0.1, y, z + (RANDOM.nextDouble() - 0.5) * 0.1,
                motion.x + (RANDOM.nextDouble() - 0.5) * 0.01, motion.y, motion.z + (RANDOM.nextDouble() - 0.5) * 0.01),
                perTick, ticks);
    }

    /**
     * Start a trickle that runs until {@link #unmark}ed, like the poison marker over a plate or bed that lasts
     * until someone takes the poisoned portion.
     * 开始持续冒出粒子，直到 unmark，例如托盘或床上的中毒标记会一直持续到有人拿走有毒的那份。
     */
    static Marker mark(SceneBuilder scene, ParticleEffect particle, Vec3d at, Vec3d motion, float perTick) {
        Marker marker = new Marker(particle, at, motion, perTick);
        scene.addInstruction(marker);
        return marker;
    }

    static void unmark(SceneBuilder scene, Marker marker) {
        scene.addInstruction(ponder -> marker.stopped = true);
    }

    static final class Marker extends TickingInstruction {
        private static final int LONGEST = 20 * 60 * 10;
        private final ParticleEffect particle;
        private final Vec3d at;
        private final Vec3d motion;
        private final float perTick;
        private boolean stopped;

        private Marker(ParticleEffect particle, Vec3d at, Vec3d motion, float perTick) {
            super(false, LONGEST);
            this.particle = particle;
            this.at = at;
            this.motion = motion;
            this.perTick = perTick;
        }

        @Override
        public void reset(PonderScene scene) {
            super.reset(scene);
            stopped = false;
        }

        @Override
        public void tick(PonderScene scene) {
            super.tick(scene);
            if (stopped || RANDOM.nextFloat() >= perTick) {
                return;
            }
            scene.getWorld().addParticle(particle, at.x + (RANDOM.nextDouble() - 0.5) * 0.1, at.y,
                    at.z + (RANDOM.nextDouble() - 0.5) * 0.1, (RANDOM.nextDouble() - 0.5) * 0.01, motion.y,
                    (RANDOM.nextDouble() - 0.5) * 0.01);
        }

        @Override
        public boolean isComplete() {
            return stopped || super.isComplete();
        }
    }
}
