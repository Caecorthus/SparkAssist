package dev.caecorthus.sparkassist.ponder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Tick-length actor movements. Only voluntary walks contribute to the stride; slides are external motion. */
public final class ActorMovement {
    private final List<Move> moves = new ArrayList<>();

    public void add(double x, double y, double z, int ticks, boolean walking) {
        requireDuration(ticks);
        moves.add(new Move(x / ticks, y / ticks, z / ticks, ticks, walking));
    }

    /**
     * Limb-swing speed for an actor walking {@code distance} blocks in one tick. Vanilla's min(1, 4 * distance) barely
     * swings the legs on a slow walk while the body glides on, so a planted foot slides along with most of each step.
     * Below a normal walk the swing follows 1.6 * sqrt(distance) instead, which makes the average stride (leg pitch
     * 1.4 * speed * cos(0.6662 * pos), pos advancing by speed each tick) cover the distance walked; from about 0.16
     * blocks a tick on it is vanilla's.
     */
    public static float strideSpeed(double distance) {
        if (!(distance > 0)) {
            return 0;
        }
        return (float) Math.min(1, Math.max(4 * distance, 1.6 * Math.sqrt(distance)));
    }

    public static void requireDuration(int ticks) {
        if (ticks < 1) {
            throw new IllegalArgumentException("Actor movement duration must be at least one tick");
        }
    }

    public Step tick() {
        double x = 0;
        double y = 0;
        double z = 0;
        double walkX = 0;
        double walkZ = 0;
        boolean sliding = false;
        for (Iterator<Move> it = moves.iterator(); it.hasNext(); ) {
            Move move = it.next();
            x += move.x;
            y += move.y;
            z += move.z;
            if (move.walking) {
                walkX += move.x;
                walkZ += move.z;
            } else {
                sliding = true;
            }
            if (--move.ticks == 0) {
                it.remove();
            }
        }
        return new Step(x, y, z, Math.hypot(walkX, walkZ), sliding);
    }

    /** Death and sleeping stop walking, while a shove or body throw may continue. */
    public void stopWalking() {
        moves.removeIf(move -> move.walking);
    }

    public void clear() {
        moves.clear();
    }

    public record Step(double x, double y, double z, double walkingDistance, boolean sliding) {
    }

    private static final class Move {
        private final double x;
        private final double y;
        private final double z;
        private final boolean walking;
        private int ticks;

        private Move(double x, double y, double z, int ticks, boolean walking) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.ticks = ticks;
            this.walking = walking;
        }
    }
}
