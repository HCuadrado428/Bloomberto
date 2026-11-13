package com.dougg865.bloomberto.api.effects;

import java.util.List;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Builds particle effects from a pattern and spawns them on either side: on the client it adds the particles directly, on
 * the server it sends them to the players who are near.
 *
 * <pre>{@code
 * Particles.of(ParticleTypes.FLAME).ring(pos, 1.5, 24).speed(0.05).spawn(level);
 * Particles.of(ParticleTypes.END_ROD).spiral(pos, 1.0, 3.0, 2, 40).spawn(level);
 * }</pre>
 */
public final class Particles {
    private final ParticleOptions options;
    private List<ParticlePatterns.Point> points = List.of();
    private double speed = 0.02;
    private double inheritedSpeed;
    private Vec3 push = Vec3.ZERO;

    private Particles(ParticleOptions options) {
        this.options = options;
    }

    public static Particles of(ParticleOptions options) {
        return new Particles(options);
    }

    public Particles ring(Vec3 center, double radius, int count) {
        this.points = ParticlePatterns.ring(center, radius, count, new Vec3(0, 1, 0));
        return this;
    }

    /** A ring in the plane perpendicular to {@code normal}. */
    public Particles ring(Vec3 center, double radius, int count, Vec3 normal) {
        this.points = ParticlePatterns.ring(center, radius, count, normal);
        return this;
    }

    public Particles sphere(Vec3 center, double radius, int count) {
        this.points = ParticlePatterns.sphere(center, radius, count);
        return this;
    }

    public Particles line(Vec3 from, Vec3 to, int count) {
        this.points = ParticlePatterns.line(from, to, count);
        return this;
    }

    public Particles spiral(Vec3 base, double radius, double height, double turns, int count) {
        this.points = ParticlePatterns.spiral(base, radius, height, turns, count);
        return this;
    }

    /** Uses an explicit list of points. */
    public Particles points(List<ParticlePatterns.Point> points) {
        this.points = List.copyOf(points);
        return this;
    }

    /** How fast particles move along their pattern direction, in blocks per tick. */
    public Particles speed(double speed) {
        this.speed = speed;
        return this;
    }

    /** A constant velocity added to every particle (e.g. upward drift). */
    public Particles push(Vec3 velocity) {
        this.push = velocity;
        return this;
    }

    /** The velocity of the particle at pattern point {@code index}. */
    public Vec3 velocityOf(int index) {
        return points.get(index).direction().scale(speed).add(push);
    }

    public List<ParticlePatterns.Point> points() {
        return points;
    }

    /** Spawns the particles. Works on client levels and server levels. */
    public void spawn(Level level) {
        for (int i = 0; i < points.size(); i++) {
            Vec3 p = points.get(i).position();
            Vec3 v = velocityOf(i);
            if (level instanceof ServerLevel server) {
                // With count 0 the "offset" is the particle's velocity.
                server.sendParticles(options, p.x, p.y, p.z, 0, v.x, v.y, v.z, 1.0);
            } else {
                level.addParticle(options, p.x, p.y, p.z, v.x, v.y, v.z);
            }
        }
    }
}
