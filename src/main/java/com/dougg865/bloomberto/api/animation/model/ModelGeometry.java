package com.dougg865.bloomberto.api.animation.model;

import java.util.ArrayList;
import java.util.List;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Turns a {@link GeoModel} and a {@link Pose} into quads in Minecraft's render space (1/16 block units, Y up). Pure math: no
 * rendering API is involved, so the result is the same on every version and can be tested.
 */
public final class ModelGeometry {
    /** A corner: position and texture coordinates (0-1). */
    public record Vertex(float x, float y, float z, float u, float v) {
    }

    /** A flat face: four corners in counter-clockwise order seen from the front, and its outward normal. */
    public record Quad(Vertex[] vertices, float nx, float ny, float nz) {
    }

    /**
     * A model prepared for drawing: every face is stored once in its bone's space, so drawing a frame only computes one
     * matrix per bone and transforms the stored corners, without creating objects. Built once per {@link GeoModel} and
     * kept until the model is no longer used.
     */
    public static final class Compiled {
        private final String[] names;
        private final float[][] pivots;
        private final float[][] baseRotations;
        private final int[][] children;
        private final int[] roots;
        /** Per bone: 4 corners x (x, y, z, u, v) per quad. */
        private final float[][] corners;
        /** Per bone: (nx, ny, nz) per quad. */
        private final float[][] normals;

        private Compiled(GeoModel model) {
            List<GeoModel.Bone> bones = model.bones();
            int n = bones.size();
            names = new String[n];
            pivots = new float[n][];
            baseRotations = new float[n][];
            children = new int[n][];
            corners = new float[n][];
            normals = new float[n][];
            java.util.Map<String, Integer> index = new java.util.HashMap<>();
            for (int i = 0; i < n; i++) {
                index.put(bones.get(i).name(), i);
            }
            List<Integer> rootList = new ArrayList<>();
            List<List<Integer>> kids = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                kids.add(new ArrayList<>());
            }
            for (int i = 0; i < n; i++) {
                GeoModel.Bone b = bones.get(i);
                names[i] = b.name();
                pivots[i] = Conventions.position(b.pivot());
                baseRotations[i] = Conventions.rotation(b.rotation());
                Integer parent = b.parent() == null ? null : index.get(b.parent());
                (parent == null ? rootList : kids.get(parent)).add(i);
                List<Quad> quads = new ArrayList<>();
                for (GeoModel.Cube cube : b.cubes()) {
                    addCube(model, b, cube, quads);
                }
                float[] c = new float[quads.size() * 20];
                float[] nn = new float[quads.size() * 3];
                for (int q = 0; q < quads.size(); q++) {
                    Quad quad = quads.get(q);
                    for (int v = 0; v < 4; v++) {
                        Vertex vx = quad.vertices()[v];
                        int o = q * 20 + v * 5;
                        c[o] = vx.x();
                        c[o + 1] = vx.y();
                        c[o + 2] = vx.z();
                        c[o + 3] = vx.u();
                        c[o + 4] = vx.v();
                    }
                    nn[q * 3] = quad.nx();
                    nn[q * 3 + 1] = quad.ny();
                    nn[q * 3 + 2] = quad.nz();
                }
                corners[i] = c;
                normals[i] = nn;
            }
            roots = rootList.stream().mapToInt(Integer::intValue).toArray();
            for (int i = 0; i < n; i++) {
                children[i] = kids.get(i).stream().mapToInt(Integer::intValue).toArray();
            }
        }

        /** Receives the corners of each quad, already posed, in order. */
        @FunctionalInterface
        public interface Sink {
            void vertex(float x, float y, float z, float u, float v, float nx, float ny, float nz);
        }

        /** Sends every posed corner to {@code sink}: four per quad, in the same order as {@link #build}. */
        public void emit(Pose pose, Sink sink) {
            Matrix4f identity = new Matrix4f();
            Vector3f p = new Vector3f();
            Vector3f nrm = new Vector3f();
            Matrix3f normalMatrix = new Matrix3f();
            for (int root : roots) {
                emitBone(root, pose, identity, sink, p, nrm, normalMatrix);
            }
        }

        private void emitBone(int i, Pose pose, Matrix4f parent, Sink sink, Vector3f p, Vector3f nrm, Matrix3f normalMatrix) {
            float[] animRot;
            float[] animPos;
            float[] scale;
            if (pose.has(names[i])) {
                BoneTransform anim = pose.get(names[i]);
                animRot = Conventions.rotation(anim.rotation);
                animPos = Conventions.position(anim.position);
                scale = anim.scale;
            } else {
                animRot = ZERO;
                animPos = ZERO;
                scale = ONE;
            }
            float[] pivot = pivots[i];
            float[] base = baseRotations[i];
            Matrix4f local = new Matrix4f(parent)
                    .translate(pivot[0], pivot[1], pivot[2])
                    .translate(animPos[0], animPos[1], animPos[2])
                    .rotateZYX(base[2] + animRot[2], base[1] + animRot[1], base[0] + animRot[0])
                    .scale(scale[0], scale[1], scale[2])
                    .translate(-pivot[0], -pivot[1], -pivot[2]);
            float[] c = corners[i];
            float[] nn = normals[i];
            if (c.length > 0) {
                local.normal(normalMatrix);
                for (int q = 0; q < nn.length / 3; q++) {
                    normalMatrix.transform(nn[q * 3], nn[q * 3 + 1], nn[q * 3 + 2], nrm);
                    if (nrm.lengthSquared() > 0f) {
                        nrm.normalize();
                    }
                    for (int v = 0; v < 4; v++) {
                        int o = q * 20 + v * 5;
                        p.set(c[o], c[o + 1], c[o + 2]);
                        local.transformPosition(p);
                        sink.vertex(p.x, p.y, p.z, c[o + 3], c[o + 4], nrm.x, nrm.y, nrm.z);
                    }
                }
            }
            for (int child : children[i]) {
                emitBone(child, pose, local, sink, p, nrm, normalMatrix);
            }
        }
    }

    private static final float[] ZERO = {0f, 0f, 0f};
    private static final float[] ONE = {1f, 1f, 1f};

    /** Compiled models, by identity; an entry goes away when its model is no longer referenced (e.g. after F3+T). */
    private static final java.util.Map<GeoModel, Compiled> CACHE =
            java.util.Collections.synchronizedMap(new IdentityWeakMap<>());

    /** The compiled form of {@code model}, built on first use. */
    public static Compiled compile(GeoModel model) {
        Compiled compiled = CACHE.get(model);
        if (compiled == null) {
            compiled = new Compiled(model);
            CACHE.put(model, compiled);
        }
        return compiled;
    }

    private ModelGeometry() {
    }

    /** All quads of the model with the pose applied. Allocates; renderers use {@link #compile}{@code .emit} instead. */
    public static List<Quad> build(GeoModel model, Pose pose) {
        List<Quad> quads = new ArrayList<>();
        float[][] pending = new float[4][];
        int[] count = {0};
        float[] normal = new float[3];
        compile(model).emit(pose, (x, y, z, u, v, nx, ny, nz) -> {
            pending[count[0]++] = new float[] {x, y, z, u, v};
            normal[0] = nx;
            normal[1] = ny;
            normal[2] = nz;
            if (count[0] == 4) {
                Vertex[] vs = new Vertex[4];
                for (int i = 0; i < 4; i++) {
                    vs[i] = new Vertex(pending[i][0], pending[i][1], pending[i][2], pending[i][3], pending[i][4]);
                }
                quads.add(new Quad(vs, normal[0], normal[1], normal[2]));
                count[0] = 0;
            }
        });
        return quads;
    }

    private static void addCube(GeoModel model, GeoModel.Bone bone, GeoModel.Cube cube, List<Quad> out) {
        Matrix4f m = new Matrix4f(); // the cube's own (static) rotation; the bone pose is applied per frame
        if (cube.pivot() != null) {
            float[] cp = Conventions.position(cube.pivot());
            float[] cr = Conventions.rotation(cube.rotation());
            m.translate(cp[0], cp[1], cp[2]).rotateZYX(cr[2], cr[1], cr[0]).translate(-cp[0], -cp[1], -cp[2]);
        }
        float w = cube.size()[0];
        float h = cube.size()[1];
        float d = cube.size()[2];
        float inf = cube.inflate();
        // Bedrock x -> render x is mirrored, so the cube spans [-(ox + w), -ox] on x.
        float x0 = -(cube.origin()[0] + w) - inf;
        float x1 = -cube.origin()[0] + inf;
        float y0 = cube.origin()[1] - inf;
        float y1 = cube.origin()[1] + h + inf;
        float z0 = cube.origin()[2] - inf;
        float z1 = cube.origin()[2] + d + inf;

        boolean mirror = cube.mirror() || (bone.mirror() && !cube.mirror());
        GeoModel.UvRegion north = region(cube, "north", cube.uv().boxU() + d, cube.uv().boxV() + d, w, h);
        GeoModel.UvRegion east = region(cube, "east", cube.uv().boxU(), cube.uv().boxV() + d, d, h);
        GeoModel.UvRegion south = region(cube, "south", cube.uv().boxU() + d + w + d, cube.uv().boxV() + d, w, h);
        GeoModel.UvRegion west = region(cube, "west", cube.uv().boxU() + d + w, cube.uv().boxV() + d, d, h);
        GeoModel.UvRegion up = region(cube, "up", cube.uv().boxU() + d, cube.uv().boxV(), w, d);
        GeoModel.UvRegion down = region(cube, "down", cube.uv().boxU() + d + w, cube.uv().boxV(), w, d);
        if (mirror) {
            GeoModel.UvRegion tmp = east;
            east = west;
            west = tmp;
        }
        float tw = model.textureWidth();
        float th = model.textureHeight();
        Matrix3f normalMatrix = new Matrix3f();
        m.normal(normalMatrix);

        // front (+z, "south"): bottom-left, bottom-right, top-right, top-left
        face(out, m, normalMatrix, 0, 0, 1, mirror, tw, th, south, false,
                x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
        // back (-z, "north")
        face(out, m, normalMatrix, 0, 0, -1, mirror, tw, th, north, false,
                x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0);
        // +x side shows Bedrock's "west" region (x is mirrored)
        face(out, m, normalMatrix, 1, 0, 0, mirror, tw, th, west, false,
                x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1);
        // -x side shows Bedrock's "east" region
        face(out, m, normalMatrix, -1, 0, 0, mirror, tw, th, east, false,
                x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
        // top
        face(out, m, normalMatrix, 0, 1, 0, mirror, tw, th, up, false,
                x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0);
        // bottom (its texture is flipped vertically in Bedrock)
        face(out, m, normalMatrix, 0, -1, 0, mirror, tw, th, down, true,
                x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
    }

    private static GeoModel.UvRegion region(GeoModel.Cube cube, String face, float u, float v, float w, float h) {
        if (cube.uv().isPerFace()) {
            GeoModel.UvRegion r = cube.uv().faces().get(face);
            if (r == null) {
                return new GeoModel.UvRegion(0, 0, 0, 0);
            }
            return new GeoModel.UvRegion(r.u(), r.v(), r.width() == 0 ? w : r.width(), r.height() == 0 ? h : r.height());
        }
        return new GeoModel.UvRegion(u, v, w, h);
    }

    private static void face(List<Quad> out, Matrix4f m, Matrix3f normalMatrix, float nx, float ny, float nz, boolean mirror,
                             float tw, float th, GeoModel.UvRegion r, boolean flipV, float... c) {
        if (r.width() == 0 && r.height() == 0) {
            return; // an explicit-UV cube without this face: nothing is drawn
        }
        float u0 = r.u() / tw;
        float u1 = (r.u() + r.width()) / tw;
        float v0 = r.v() / th;
        float v1 = (r.v() + r.height()) / th;
        if (mirror) {
            float t = u0;
            u0 = u1;
            u1 = t;
        }
        if (flipV) {
            float t = v0;
            v0 = v1;
            v1 = t;
        }
        // bottom-left, bottom-right, top-right, top-left
        float[][] uv = {{u0, v1}, {u1, v1}, {u1, v0}, {u0, v0}};
        Vertex[] vertices = new Vertex[4];
        Vector3f p = new Vector3f();
        for (int i = 0; i < 4; i++) {
            p.set(c[i * 3], c[i * 3 + 1], c[i * 3 + 2]);
            m.transformPosition(p);
            vertices[i] = new Vertex(p.x, p.y, p.z, uv[i][0], uv[i][1]);
        }
        Vector3f n = normalMatrix.transform(new Vector3f(nx, ny, nz));
        if (n.lengthSquared() > 0f) {
            n.normalize();
        }
        out.add(new Quad(vertices, n.x, n.y, n.z));
    }
}
