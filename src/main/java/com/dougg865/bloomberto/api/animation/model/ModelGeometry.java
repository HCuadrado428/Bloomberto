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

    private ModelGeometry() {
    }

    /** All quads of the model with the pose applied. */
    public static List<Quad> build(GeoModel model, Pose pose) {
        List<Quad> quads = new ArrayList<>();
        for (GeoModel.Bone root : model.childrenOf(null)) {
            addBone(model, root, pose, new Matrix4f(), quads);
        }
        return quads;
    }

    private static void addBone(GeoModel model, GeoModel.Bone bone, Pose pose, Matrix4f parent, List<Quad> out) {
        BoneTransform anim = pose.has(bone.name()) ? pose.get(bone.name()) : new BoneTransform();
        float[] pivot = Conventions.position(bone.pivot());
        float[] baseRot = Conventions.rotation(bone.rotation());
        float[] animRot = Conventions.rotation(anim.rotation);
        float[] animPos = Conventions.position(anim.position);

        Matrix4f local = new Matrix4f(parent)
                .translate(pivot[0], pivot[1], pivot[2])
                .translate(animPos[0], animPos[1], animPos[2])
                .rotateZYX(baseRot[2] + animRot[2], baseRot[1] + animRot[1], baseRot[0] + animRot[0])
                .scale(anim.scale[0], anim.scale[1], anim.scale[2])
                .translate(-pivot[0], -pivot[1], -pivot[2]);

        for (GeoModel.Cube cube : bone.cubes()) {
            addCube(model, bone, cube, local, out);
        }
        for (GeoModel.Bone child : model.childrenOf(bone.name())) {
            addBone(model, child, pose, local, out);
        }
    }

    private static void addCube(GeoModel model, GeoModel.Bone bone, GeoModel.Cube cube, Matrix4f bonePose, List<Quad> out) {
        Matrix4f m = new Matrix4f(bonePose);
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
