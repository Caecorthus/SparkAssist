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

    @Test
    void strideMatchesVanillaFromANormalWalkUp() {
        for (double distance = 0.16; distance <= 0.6; distance += 0.01) {
            assertEquals(Math.min(1, 4 * distance), ActorMovement.strideSpeed(distance), 1.0e-6);
        }
    }

    @Test
    void slowWalksSwingTheLegsFarEnoughToCoverTheGround() {
        // BipedEntityModel: leg pitch = cos(0.6662 * pos) * 1.4 * speed, pos += speed per tick; legs 12/16 of a block
        // at the player scale 0.9375. Half a gait cycle sweeps the foot 2 * leg * sin(1.4 * speed).
        double leg = 0.75 * 0.9375;
        for (double distance : new double[]{0.028, 0.05, 0.08, 0.1, 0.13}) {
            double speed = ActorMovement.strideSpeed(distance);
            double sweep = 2 * leg * Math.sin(1.4 * speed);
            double travel = Math.PI / 0.6662 / speed * distance;
            assertEquals(1, sweep / travel, 0.1, "distance " + distance);
            double vanilla = Math.min(1, 4 * distance);
            assertTrue(2 * leg * Math.sin(1.4 * vanilla) < Math.PI / 0.6662 / vanilla * distance * 0.8,
                    "vanilla stride already covers " + distance);
        }
    }

    @Test
    void strideIsBoundedAndRisesSmoothly() {
        float previous = 0;
        for (int i = 1; i <= 2000; i++) {
            float speed = ActorMovement.strideSpeed(i / 1000.0);
            assertTrue(speed >= previous && speed <= 1, "at " + i / 1000.0);
            assertTrue(speed - previous < 0.06, "jump at " + i / 1000.0);
            previous = speed;
        }
        assertEquals(0, ActorMovement.strideSpeed(0));
        assertEquals(0, ActorMovement.strideSpeed(-0.1));
        assertEquals(0, ActorMovement.strideSpeed(Double.NaN));
    }

    private static void assertIdle(ActorMovement.Step step) {
        assertEquals(0, step.x(), EPSILON);
        assertEquals(0, step.y(), EPSILON);
        assertEquals(0, step.z(), EPSILON);
        assertEquals(0, step.walkingDistance(), EPSILON);
        assertFalse(step.sliding());
    }
}
