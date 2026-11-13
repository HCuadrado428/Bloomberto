package com.dougg865.bloomberto.api.effects;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.phys.Vec3;

/** Geometric layouts for particle effects. Pure math; every point has a unit direction particles can move along. */
public final class ParticlePatterns {
    /** A spawn position and the direction a particle should move in (unit length, or zero for none). */
    public record Point(Vec3 position, Vec3 direction) {
    }

    private ParticlePatterns() {
    }

    /** {@code count} points evenly spaced on a circle around {@code center}, in the plane perpendicular to {@code normal}; they move outward. */
    public static List<Point> ring(Vec3 center, double radius, int count, Vec3 normal) {
        Vec3 n = normal.normalize();
        Vec3 u = perpendicular(n);
        Vec3 v = n.cross(u);
        List<Point> points = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            double angle = 2.0 * Math.PI * i / count;
            Vec3 dir = u.scale(Math.cos(angle)).add(v.scale(Math.sin(angle)));
            points.add(new Point(center.add(dir.scale(radius)), dir));
        }
        return points;
    }

    /** {@code count} points evenly spread over a sphere (Fibonacci lattice); they move outward. */
    public static List<Point> sphere(Vec3 center, double radius, int count) {
        List<Point> points = new ArrayList<>(count);
        double golden = Math.PI * (3.0 - Math.sqrt(5.0));
        for (int i = 0; i < count; i++) {
            double y = count == 1 ? 0 : 1.0 - 2.0 * i / (count - 1);
            double r = Math.sqrt(Math.max(0, 1.0 - y * y));
            double theta = golden * i;
            Vec3 dir = new Vec3(Math.cos(theta) * r, y, Math.sin(theta) * r);
            points.add(new Point(center.add(dir.scale(radius)), dir));
        }
        return points;
    }

    /** {@code count} points evenly spaced from {@code from} to {@code to} (both included for count >= 2); they move along the line. */
    public static List<Point> line(Vec3 from, Vec3 to, int count) {
        Vec3 dir = to.subtract(from);
        Vec3 unit = dir.lengthSqr() == 0 ? Vec3.ZERO : dir.normalize();
        List<Point> points = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            double t = count == 1 ? 0 : (double) i / (count - 1);
            points.add(new Point(from.add(dir.scale(t)), unit));
        }
        return points;
    }

    /** A rising spiral: {@code turns} turns around the vertical axis from {@code base} up by {@code height}; points move upward. */
    public static List<Point> spiral(Vec3 base, double radius, double height, double turns, int count) {
        List<Point> points = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            double t = count == 1 ? 0 : (double) i / (count - 1);
            double angle = 2.0 * Math.PI * turns * t;
            points.add(new Point(base.add(Math.cos(angle) * radius, height * t, Math.sin(angle) * radius), new Vec3(0, 1, 0)));
        }
        return points;
    }

    private static Vec3 perpendicular(Vec3 n) {
        Vec3 axis = Math.abs(n.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        return n.cross(axis).normalize();
    }
}
