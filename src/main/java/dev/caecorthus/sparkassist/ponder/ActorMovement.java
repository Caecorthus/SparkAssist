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
