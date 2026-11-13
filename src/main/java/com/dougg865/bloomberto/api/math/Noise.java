package com.dougg865.bloomberto.api.math;

import java.util.Random;

/** Deterministic 2D gradient (Perlin) noise. Output is in [-1, 1] and is 0 at integer lattice points. */
public final class Noise {
    private final int[] perm = new int[512];

    public Noise(long seed) {
        int[] p = new int[256];
        for (int i = 0; i < 256; i++) {
            p[i] = i;
        }
        Random random = new Random(seed);
        for (int i = 255; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int tmp = p[i];
            p[i] = p[j];
            p[j] = tmp;
        }
        for (int i = 0; i < 512; i++) {
            perm[i] = p[i & 255];
        }
    }

    public float sample(float x, float y) {
        int xi = (int) Math.floor(x);
        int yi = (int) Math.floor(y);
        float xf = x - xi;
        float yf = y - yi;
        float u = Interpolation.smootherstep(xf);
        float v = Interpolation.smootherstep(yf);
        int x0 = xi & 255;
        int y0 = yi & 255;
        int aa = perm[perm[x0] + y0];
        int ab = perm[perm[x0] + y0 + 1];
        int ba = perm[perm[x0 + 1] + y0];
        int bb = perm[perm[x0 + 1] + y0 + 1];
        float x1 = Interpolation.lerp(grad(aa, xf, yf), grad(ba, xf - 1f, yf), u);
        float x2 = Interpolation.lerp(grad(ab, xf, yf - 1f), grad(bb, xf - 1f, yf - 1f), u);
        // 2D gradient noise with these gradients stays within +-sqrt(2)/2; scale to [-1, 1].
        return Interpolation.clamp(Interpolation.lerp(x1, x2, v) * 1.4142135f, -1f, 1f);
    }

    private static float grad(int hash, float x, float y) {
        switch (hash & 7) {
            case 0: return x + y;
            case 1: return x - y;
            case 2: return -x + y;
            case 3: return -x - y;
            case 4: return x;
            case 5: return -x;
            case 6: return y;
            default: return -y;
        }
    }
}
