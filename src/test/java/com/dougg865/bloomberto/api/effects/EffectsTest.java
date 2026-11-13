package com.dougg865.bloomberto.api.effects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class EffectsTest {
    private static final double EPS = 1e-9;

    @Test
    void ringPointsAreOnTheCircleAndMoveOutward() {
        Vec3 center = new Vec3(10, 64, -3);
        List<ParticlePatterns.Point> ring = ParticlePatterns.ring(center, 2.0, 12, new Vec3(0, 1, 0));
        assertEquals(12, ring.size());
        for (ParticlePatterns.Point p : ring) {
            Vec3 offset = p.position().subtract(center);
            assertEquals(2.0, offset.length(), EPS);
            assertEquals(0.0, offset.y, EPS, "a horizontal ring stays level");
            assertEquals(1.0, p.direction().length(), EPS);
            assertEquals(0.0, p.direction().subtract(offset.normalize()).length(), EPS);
        }
        assertNotEquals(ring.get(0).position(), ring.get(1).position());
    }

    @Test
    void ringFollowsItsNormal() {
        Vec3 center = Vec3.ZERO;
        for (ParticlePatterns.Point p : ParticlePatterns.ring(center, 1.0, 8, new Vec3(1, 0, 0))) {
            assertEquals(0.0, p.position().x, EPS, "a ring around the X axis lies in the YZ plane");
        }
        // a normal that is parallel to the helper axis must still work
        assertEquals(6, ParticlePatterns.ring(center, 1.0, 6, new Vec3(0, 1, 0)).size());
        assertEquals(6, ParticlePatterns.ring(center, 1.0, 6, new Vec3(0, 0, 1)).size());
    }

    @Test
    void spherePointsLieOnTheSphere() {
        Vec3 center = new Vec3(1, 2, 3);
        List<ParticlePatterns.Point> sphere = ParticlePatterns.sphere(center, 3.0, 50);
        assertEquals(50, sphere.size());
        for (ParticlePatterns.Point p : sphere) {
            assertEquals(3.0, p.position().subtract(center).length(), 1e-9);
            assertEquals(1.0, p.direction().length(), 1e-9);
        }
        assertEquals(1, ParticlePatterns.sphere(center, 1.0, 1).size());
    }

    @Test
    void lineIncludesBothEnds() {
        List<ParticlePatterns.Point> line = ParticlePatterns.line(new Vec3(0, 0, 0), new Vec3(10, 0, 0), 5);
        assertEquals(new Vec3(0, 0, 0), line.get(0).position());
        assertEquals(new Vec3(10, 0, 0), line.get(4).position());
        assertEquals(new Vec3(5, 0, 0), line.get(2).position());
        assertEquals(new Vec3(1, 0, 0), line.get(0).direction());
        assertEquals(Vec3.ZERO, ParticlePatterns.line(Vec3.ZERO, Vec3.ZERO, 3).get(1).direction());
    }

    @Test
    void spiralClimbsAndTurns() {
        List<ParticlePatterns.Point> spiral = ParticlePatterns.spiral(new Vec3(0, 0, 0), 1.0, 4.0, 2, 41);
        assertEquals(4.0, spiral.get(40).position().y, EPS);
        assertEquals(0.0, spiral.get(0).position().y, EPS);
        assertEquals(1.0, spiral.get(0).position().x, EPS);
        assertEquals(1.0, spiral.get(20).position().x, 1e-9, "two turns in 40 steps: back at the start angle every 20");
    }

    @Test
    void particleVelocityCombinesSpeedAndPush() {
        Particles particles = Particles.of(null).line(Vec3.ZERO, new Vec3(0, 0, 4), 3).speed(0.5).push(new Vec3(0, 0.1, 0));
        assertEquals(3, particles.points().size());
        Vec3 v = particles.velocityOf(1);
        assertEquals(0.0, v.x, EPS);
        assertEquals(0.1, v.y, EPS);
        assertEquals(0.5, v.z, EPS);
    }

    @Test
    void trailKeepsAtMostMaxSamplesAndExpires() {
        Trail trail = new Trail(5, 4);
        for (int i = 0; i < 10; i++) {
            trail.add(new Vec3(i, 0, 0));
        }
        assertEquals(4, trail.size());
        assertEquals(new Vec3(9, 0, 0), trail.positions().get(0), "newest first");
        for (int i = 0; i < 5; i++) {
            trail.tick();
        }
        assertEquals(0, trail.size(), "everything is older than the max age");
    }

    @Test
    void trailOnlyDropsExpiredSamples() {
        Trail trail = new Trail(3, 10);
        trail.add(Vec3.ZERO);
        trail.tick();
        trail.add(new Vec3(1, 0, 0));
        trail.tick();
        assertEquals(2, trail.size());
        trail.tick();
        assertEquals(1, trail.size(), "the first sample expired, the second has not");
    }

    @Test
    void segmentsFaceTheCameraAndNarrowTowardsTheTail() {
        Trail trail = new Trail(10, 10);
        trail.add(new Vec3(0, 0, 0));
        trail.tick();
        trail.add(new Vec3(1, 0, 0));
        trail.tick();
        trail.add(new Vec3(2, 0, 0));
        List<Trail.Segment> segments = trail.segments(new Vec3(1, 0, 10), 1.0f);
        assertEquals(2, segments.size());
        Trail.Segment first = segments.get(0); // between the newest (x=2) and the middle sample
        // the camera is on +Z, the trail runs along X, so the ribbon is offset along Y
        assertEquals(0.0, first.a1().z, 1e-9);
        assertTrue(Math.abs(first.a1().y - first.a2().y) > 0.4, "head of the segment is about one block wide");
        double headWidth = first.a1().subtract(first.a2()).length();
        double tailWidth = first.b1().subtract(first.b2()).length();
        assertTrue(tailWidth < headWidth, "narrower towards the tail");
        assertTrue(first.ageStart() < first.ageEnd());
    }

    @Test
    void stationaryOrStraightOnTrailsDoNotProduceNaN() {
        Trail still = new Trail(5, 5);
        still.add(Vec3.ZERO);
        still.add(Vec3.ZERO);
        assertEquals(0, still.segments(new Vec3(0, 0, 5), 1f).size(), "no direction, no ribbon");
        Trail inline = new Trail(5, 5);
        inline.add(new Vec3(0, 0, 0));
        inline.add(new Vec3(0, 0, 1));
        List<Trail.Segment> seg = inline.segments(new Vec3(0, 0, 10), 1f);
        assertEquals(1, seg.size());
        assertTrue(Double.isFinite(seg.get(0).a1().x));
    }

    @Test
    void trailValidatesItsLimits() {
        assertThrows(IllegalArgumentException.class, () -> new Trail(0, 5));
        assertThrows(IllegalArgumentException.class, () -> new Trail(5, 1));
    }
}
