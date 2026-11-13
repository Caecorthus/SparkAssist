package dev.caecorthus.sparkassist.client.ponder;

import dev.doctor4t.wathe.index.WatheParticles;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.instruction.TickingInstruction;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import org.joml.Vector3f;

/**
 * Scripted particles for effects the game only plays server-side or only for some viewers (an explosion, the poison
 * marker killers see): Ponder's world never runs that code, so scenes add the particles themselves.
 * 脚本化的粒子效果，用于游戏只在服务端播放、或只有部分人看得到的效果（爆炸、杀手才看得到的中毒标记）：
 * Ponder 的世界不会运行那些代码，所以由场景自己添加粒子。
 */
final class Effects {
    private static final Random RANDOM = Random.create();
    /** Gold dust for an illustrated area edge. 用于示意范围边界的金色粉尘。 */
    private static final DustParticleEffect EDGE_DUST = new DustParticleEffect(new Vector3f(1.0f, 0.8f, 0.15f), 1.5f);
    /** Points around a full illustrated edge circle. 示意边界整圆上的点数。 */
    private static final int EDGE_POINTS = 240;
    /** The stages' plate width an illustrated edge is clipped to. 示意边界裁剪到的舞台底板边长。 */
    private static final double EDGE_PLATE = 7;

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

    /**
     * {@code count} particles as the client plays a server's spawnParticles: each at a gaussian offset of
     * {@code spread} per axis, moving at a gaussian {@code speed} per axis.
     * 像客户端播放服务端 spawnParticles 那样放出 count 个粒子：每轴按 spread 的高斯偏移放置，每轴以高斯 speed 运动。
     */
    static void spawnParticles(SceneBuilder scene, ParticleEffect particle, Vec3d at, int count, Vec3d spread,
                               double speed) {
        scene.effects().emitParticles(at, (world, x, y, z) -> {
            for (int i = 0; i < count; i++) {
                world.addParticle(particle, x + world.random.nextGaussian() * spread.x,
                        y + world.random.nextGaussian() * spread.y, z + world.random.nextGaussian() * spread.z,
                        world.random.nextGaussian() * speed, world.random.nextGaussian() * speed,
                        world.random.nextGaussian() * speed);
            }
        }, 1, 1);
    }

    /**
     * Wathe's gunshot flash, which ShootMuzzleS2CPayload shows everyone at the shooter's muzzle.
     * Wathe 的枪口火光，ShootMuzzleS2CPayload 让所有人在射手枪口处看到。
     */
    static void gunshotFlash(SceneBuilder scene, Vec3d muzzle) {
        scene.effects().emitParticles(muzzle,
                (world, x, y, z) -> world.addParticle(WatheParticles.GUNSHOT, x, y, z, 0, 0, 0), 1, 1);
    }

    /**
     * An illustrated edge: an arc of gold dust on the floor {@code radius} blocks around {@code centre}, drawn only
     * over the plate, for {@code ticks}. 示意边界：以 centre 为圆心、半径 radius 格的金色粉尘弧线，只画在底板范围内，持续
     * ticks。
     */
    static void edge(SceneBuilder scene, Vec3d centre, double radius, int ticks) {
        scene.effects().emitParticles(centre, (world, x, y, z) -> {
            for (int i = 0; i < EDGE_POINTS; i++) {
                double angle = i * MathHelper.TAU / EDGE_POINTS;
                double px = x + Math.cos(angle) * radius;
                double pz = z + Math.sin(angle) * radius;
                if (px >= 0 && px <= EDGE_PLATE && pz >= 0 && pz <= EDGE_PLATE && world.random.nextInt(3) == 0) {
                    world.addParticle(EDGE_DUST, px, y, pz, 0, 0, 0);
                }
            }
        }, 1, ticks);
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
