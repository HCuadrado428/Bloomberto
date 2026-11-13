package com.dougg865.bloomberto.api.effects;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import net.minecraft.world.phys.Vec3;

/**
 * A ribbon that follows something that moves (a projectile, a sword swing, a player). Call {@link #add} every tick with the
 * current position and {@link #tick} to age the points; draw it with {@code WorldDraw.trail}.
 * The geometry is computed here so it can be tested without a game.
 */
public final class Trail {
    /** One flat piece of the ribbon, between two samples. {@code age} is 0 at the head and 1 at the tail. */
    public record Segment(Vec3 a1, Vec3 a2, Vec3 b2, Vec3 b1, float ageStart, float ageEnd) {
    }

    private record Sample(Vec3 position, int born) { }

    private final Deque<Sample> samples = new ArrayDeque<>();
    private final int maxAge;
    private final int maxSamples;
    private int clock;

    /**
     * @param maxAgeTicks samples older than this disappear
     * @param maxSamples  at most this many samples are kept
     */
    public Trail(int maxAgeTicks, int maxSamples) {
        if (maxAgeTicks < 1 || maxSamples < 2) {
            throw new IllegalArgumentException("A trail needs a max age of at least 1 tick and 2 samples");
        }
        this.maxAge = maxAgeTicks;
        this.maxSamples = maxSamples;
    }

    /** Adds the newest position (the head of the ribbon). */
    public void add(Vec3 position) {
        samples.addFirst(new Sample(position, clock));
        while (samples.size() > maxSamples) {
            samples.removeLast();
        }
    }

    /** Ages the samples by one tick and drops the ones that expired. */
    public void tick() {
        clock++;
        samples.removeIf(s -> clock - s.born() >= maxAge);
    }

    public int size() {
        return samples.size();
    }

    public void clear() {
        samples.clear();
    }

    /** Positions from newest to oldest. */
    public List<Vec3> positions() {
        List<Vec3> list = new ArrayList<>(samples.size());
        samples.forEach(s -> list.add(s.position()));
        return list;
    }

    /**
     * The ribbon as flat segments that face the camera.
     *
     * @param camera the camera position, so each piece is turned towards it
     * @param width  the width at the head; it narrows to 0 at the tail
     */
    public List<Segment> segments(Vec3 camera, float width) {
        List<Segment> result = new ArrayList<>();
        List<Sample> list = new ArrayList<>(samples);
        for (int i = 0; i + 1 < list.size(); i++) {
            Sample s1 = list.get(i);
            Sample s2 = list.get(i + 1);
            float age1 = ageOf(s1);
            float age2 = ageOf(s2);
            Vec3 dir = s2.position().subtract(s1.position());
            if (dir.lengthSqr() < 1e-12) {
                continue;
            }
            Vec3 toCamera = camera.subtract(s1.position().add(s2.position()).scale(0.5));
            Vec3 side = dir.cross(toCamera);
            if (side.lengthSqr() < 1e-12) {
                side = dir.cross(new Vec3(0, 1, 0));
                if (side.lengthSqr() < 1e-12) {
                    side = dir.cross(new Vec3(1, 0, 0));
                }
            }
            side = side.normalize();
            Vec3 o1 = side.scale(width * 0.5 * (1.0 - age1));
            Vec3 o2 = side.scale(width * 0.5 * (1.0 - age2));
            result.add(new Segment(s1.position().add(o1), s1.position().subtract(o1),
                    s2.position().subtract(o2), s2.position().add(o2), age1, age2));
        }
        return result;
    }

    private float ageOf(Sample s) {
        return Math.min(1f, (clock - s.born()) / (float) maxAge);
    }

    /** Iterates positions newest to oldest. */
    public Iterator<Vec3> iterator() {
        return positions().iterator();
    }
}
