package dev.caecorthus.sparkassist.ponder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ActorMovementTest {
    private static final double EPSILON = 1.0e-9;

    @Test
    void walkCoversItsFullDistanceInExactlyItsDuration() {
        ActorMovement movement = new ActorMovement();
        movement.add(3, 1.5, -4, 10, true);
        double x = 0;
        double y = 0;
        double z = 0;
        for (int tick = 0; tick < 10; tick++) {
            ActorMovement.Step step = movement.tick();
            x += step.x();
            y += step.y();
            z += step.z();
            assertEquals(0.5, step.walkingDistance(), EPSILON);
            assertFalse(step.sliding());
        }
        assertEquals(3, x, EPSILON);
        assertEquals(1.5, y, EPSILON);
        assertEquals(-4, z, EPSILON);
        assertIdle(movement.tick());
    }

    @Test
    void overlappingKnockbackAndHopBothReachTheirEndpoints() {
        ActorMovement movement = new ActorMovement();
        movement.add(2.1, 0, -2.1, 12, false);
        movement.add(0, 0.7, 0, 5, false);
        double x = 0;
        double y = 0;
        double z = 0;
        for (int tick = 0; tick < 12; tick++) {
            ActorMovement.Step step = movement.tick();
            x += step.x();
            y += step.y();
            z += step.z();
            assertEquals(0, step.walkingDistance(), EPSILON);
            assertTrue(step.sliding());
            if (tick == 4) {
                movement.add(0, -0.7, 0, 7, false);
            }
        }
        assertEquals(2.1, x, EPSILON);
        assertEquals(0, y, EPSILON);
        assertEquals(-2.1, z, EPSILON);
        assertIdle(movement.tick());
    }

    @Test
    void externalForceDoesNotAddToWalkingSpeed() {
        ActorMovement movement = new ActorMovement();
        movement.add(0.5, 0, 0, 2, true);
        movement.add(2, 0, 0, 2, false);
        ActorMovement.Step step = movement.tick();
        assertEquals(1.25, step.x(), EPSILON);
        assertEquals(0.25, step.walkingDistance(), EPSILON);
        assertTrue(step.sliding());
    }

    @Test
    void verticalMotionDoesNotProduceAStride() {
        ActorMovement movement = new ActorMovement();
        movement.add(0, 2, 0, 4, true);
        ActorMovement.Step step = movement.tick();
        assertEquals(0.5, step.y(), EPSILON);
        assertEquals(0, step.walkingDistance(), EPSILON);
    }

    @Test
    void opposingWalksDoNotAnimateMovementInPlace() {
        ActorMovement movement = new ActorMovement();
        movement.add(2, 0, 0, 4, true);
        movement.add(-2, 0, 0, 4, true);
        assertIdle(movement.tick());
    }

    @Test
    void deathOrSleepStopsWalkingButKeepsBodyMotion() {
        ActorMovement movement = new ActorMovement();
        movement.add(4, 0, 0, 4, true);
        movement.add(0, 2, 0, 4, false);
        movement.tick();
        movement.stopWalking();
        for (int tick = 0; tick < 3; tick++) {
            ActorMovement.Step step = movement.tick();
            assertEquals(0, step.x(), EPSILON);
            assertEquals(0.5, step.y(), EPSILON);
            assertEquals(0, step.walkingDistance(), EPSILON);
            assertTrue(step.sliding());
        }
        assertIdle(movement.tick());
    }

    @Test
    void teleportOrReplayCancelsEveryPendingMove() {
        ActorMovement movement = new ActorMovement();
        movement.add(4, 0, 0, 4, true);
        movement.add(0, 2, 0, 4, false);
        movement.tick();
        movement.clear();
        assertIdle(movement.tick());
    }

    @Test
    void nonPositiveDurationsCannotCreateAnEndlessMove() {
        ActorMovement movement = new ActorMovement();
        assertThrows(IllegalArgumentException.class, () -> movement.add(1, 0, 0, 0, true));
        assertThrows(IllegalArgumentException.class, () -> movement.add(1, 0, 0, -1, false));
        assertIdle(movement.tick());
    }

    private static void assertIdle(ActorMovement.Step step) {
        assertEquals(0, step.x(), EPSILON);
        assertEquals(0, step.y(), EPSILON);
        assertEquals(0, step.z(), EPSILON);
        assertEquals(0, step.walkingDistance(), EPSILON);
        assertFalse(step.sliding());
    }
}
