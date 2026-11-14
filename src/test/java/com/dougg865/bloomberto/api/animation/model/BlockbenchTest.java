package com.dougg865.bloomberto.api.animation.model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class BlockbenchTest {
    private static final float EPS = 1e-4f;

    private static final String GEO = """
            {
              "format_version": "1.12.0",
              "minecraft:geometry": [
                {
                  "description": {"identifier": "geometry.golem", "texture_width": 64, "texture_height": 32},
                  "bones": [
                    {"name": "root", "pivot": [0, 0, 0]},
                    {"name": "body", "parent": "root", "pivot": [0, 12, 0], "cubes": [
                      {"origin": [-4, 12, -2], "size": [8, 12, 4], "uv": [16, 16]}
                    ]},
                    {"name": "head", "parent": "body", "pivot": [0, 24, 0], "cubes": [
                      {"origin": [-4, 24, -4], "size": [8, 8, 8], "uv": [0, 0], "inflate": 0.5}
                    ]},
                    {"name": "arm", "parent": "body", "pivot": [5, 22, 0], "rotation": [0, 0, 0], "mirror": true, "cubes": [
                      {"origin": [4, 12, -2], "size": [4, 12, 4], "uv": {
                         "north": {"uv": [40, 16], "uv_size": [4, 12]},
                         "east": {"uv": [32, 16], "uv_size": [4, 12]}
                      }}
                    ]}
                  ]
                }
              ]
            }
            """;

    private static final String ANIM = """
            {
              "format_version": "1.8.0",
              "animations": {
                "animation.golem.idle": {
                  "loop": true,
                  "animation_length": 2.0,
                  "bones": {
                    "head": {"rotation": {"0.0": [0, 0, 0], "1.0": [0, 40, 0], "2.0": [0, 0, 0]}},
                    "body": {"position": [0, 1, 0]}
                  }
                },
                "animation.golem.attack": {
                  "animation_length": 1.0,
                  "bones": {
                    "arm": {"rotation": {"0.0": [0, 0, 0], "1.0": {"pre": [90, 0, 0], "post": [0, 0, 0]}}}
                  }
                },
                "animation.golem.hold": {
                  "loop": "hold_on_last_frame",
                  "animation_length": 1.0,
                  "bones": {"head": {"scale": {"0.0": 1, "1.0": 2}}}
                },
                "animation.golem.step": {
                  "loop": true,
                  "animation_length": 2.0,
                  "bones": {"head": {"position": {"0.0": {"post": [0, 0, 0]}, "1.0": {"post": [10, 0, 0], "lerp_mode": "step"}}}}
                },
                "animation.golem.spline": {
                  "loop": true,
                  "animation_length": 3.0,
                  "bones": {"head": {"position": {
                    "0.0": [0, 0, 0],
                    "1.0": {"post": [10, 0, 0], "lerp_mode": "catmullrom"},
                    "2.0": [10, 0, 0],
                    "3.0": [0, 0, 0]}}}
                }
              }
            }
            """;

    // ---------- geometry parsing ----------

    @Test
    void parsesAModernGeometryFile() throws ParseException {
        GeoModel model = GeoParser.parse(GEO);
        assertEquals("geometry.golem", model.identifier());
        assertEquals(64, model.textureWidth());
        assertEquals(32, model.textureHeight());
        assertEquals(4, model.bones().size());
        assertEquals("body", model.bone("head").parent());
        assertNull(model.bone("root").parent());
        assertEquals(List.of("body"), model.childrenOf("root").stream().map(GeoModel.Bone::name).toList());
        GeoModel.Cube head = model.bone("head").cubes().get(0);
        assertEquals(0.5f, head.inflate());
        assertArrayEquals(new float[] {8, 8, 8}, head.size());
        assertFalse(model.bone("arm").cubes().get(0).uv().isPerFace() && model.bone("body").cubes().get(0).uv().isPerFace());
        assertTrue(model.bone("arm").cubes().get(0).uv().isPerFace());
    }

    @Test
    void parsesTheOlderGeometryFormat() throws ParseException {
        GeoModel model = GeoParser.parse("""
                {"format_version": "1.8.0", "geometry.cube": {"texturewidth": 16, "textureheight": 16,
                  "bones": [{"name": "b", "cubes": [{"origin": [0,0,0], "size": [1,1,1], "uv": [0,0]}]}]}}
                """);
        assertEquals("geometry.cube", model.identifier());
        assertEquals(16, model.textureWidth());
    }

    @Test
    void selectsAGeometryById() throws ParseException {
        String two = """
                {"minecraft:geometry": [
                  {"description": {"identifier": "geometry.a"}, "bones": []},
                  {"description": {"identifier": "geometry.b", "texture_width": 32, "texture_height": 32}, "bones": []}]}
                """;
        assertEquals("geometry.b", GeoParser.parse(two, "geometry.b").identifier());
        assertEquals("geometry.a", GeoParser.parse(two).identifier());
        assertThrows(ParseException.class, () -> GeoParser.parse(two, "geometry.zzz"));
    }

    @Test
    void rejectsBrokenGeometry() {
        assertThrows(ParseException.class, () -> GeoParser.parse("not json"));
        assertThrows(ParseException.class, () -> GeoParser.parse("{}"));
        assertThrows(ParseException.class, () -> GeoParser.parse(
                "{\"minecraft:geometry\":[{\"bones\":[{\"name\":\"a\"},{\"name\":\"a\"}]}]}"));
        assertThrows(ParseException.class, () -> GeoParser.parse(
                "{\"minecraft:geometry\":[{\"bones\":[{\"name\":\"a\",\"parent\":\"ghost\"}]}]}"));
        assertThrows(ParseException.class, () -> GeoParser.parse(
                "{\"minecraft:geometry\":[{\"bones\":[{\"name\":\"a\",\"parent\":\"b\"},{\"name\":\"b\",\"parent\":\"a\"}]}]}"));
        assertThrows(ParseException.class, () -> GeoParser.parse(
                "{\"minecraft:geometry\":[{\"bones\":[{\"name\":\"a\",\"pivot\":[1,2]}]}]}"));
        assertThrows(ParseException.class, () -> GeoParser.parse(
                "{\"minecraft:geometry\":[{\"description\":{\"texture_width\":0},\"bones\":[]}]}"));
    }

    // ---------- animation parsing and sampling ----------

    private static Map<String, AnimationClip> clips() throws ParseException {
        return AnimationParser.parse(ANIM);
    }

    @Test
    void parsesAnimationsAndLoopModes() throws ParseException {
        Map<String, AnimationClip> clips = clips();
        assertEquals(5, clips.size());
        assertEquals(AnimationClip.Loop.LOOP, clips.get("animation.golem.idle").loop());
        assertEquals(AnimationClip.Loop.ONCE, clips.get("animation.golem.attack").loop());
        assertEquals(AnimationClip.Loop.HOLD, clips.get("animation.golem.hold").loop());
        assertEquals(2f, clips.get("animation.golem.idle").length());
    }

    @Test
    void samplesLinearKeyframesAndLoops() throws ParseException {
        AnimationClip idle = clips().get("animation.golem.idle");
        assertArrayEquals(new float[] {0, 20, 0}, idle.sample(0.5f).get("head").rotation, EPS);
        assertArrayEquals(new float[] {0, 40, 0}, idle.sample(1f).get("head").rotation, EPS);
        assertArrayEquals(new float[] {0, 20, 0}, idle.sample(2.5f).get("head").rotation, EPS, "wraps around");
        assertArrayEquals(new float[] {0, 1, 0}, idle.sample(0.3f).get("body").position, EPS, "a constant value");
    }

    @Test
    void oneShotAndHoldClipsStopAtTheEnd() throws ParseException {
        AnimationClip attack = clips().get("animation.golem.attack");
        assertFalse(attack.isFinished(0.5f));
        assertTrue(attack.isFinished(1f));
        assertTrue(attack.isFinished(5f));
        AnimationClip hold = clips().get("animation.golem.hold");
        assertArrayEquals(new float[] {2, 2, 2}, hold.sample(10f).get("head").scale, EPS);
        assertArrayEquals(new float[] {1.5f, 1.5f, 1.5f}, hold.sample(0.5f).get("head").scale, EPS, "a single number is used for all axes");
    }

    @Test
    void preAndPostValuesGiveAJump() throws ParseException {
        AnimationClip attack = clips().get("animation.golem.attack");
        assertArrayEquals(new float[] {45, 0, 0}, attack.sample(0.5f).get("arm").rotation, EPS);
        assertArrayEquals(new float[] {90, 0, 0}, attack.sample(0.999f).get("arm").rotation, 0.2f, "approaches 'pre' at the end");
        assertArrayEquals(new float[] {0, 0, 0}, attack.sample(1f).get("arm").rotation, EPS, "then 'post' from the key on");
    }

    @Test
    void stepKeyframesHoldTheValueUntilTheNextKey() throws ParseException {
        AnimationClip step = clips().get("animation.golem.step");
        assertArrayEquals(new float[] {0, 0, 0}, step.sample(0.9f).get("head").position, EPS);
        assertArrayEquals(new float[] {10, 0, 0}, step.sample(1.1f).get("head").position, EPS);
    }

    @Test
    void catmullRomPassesThroughKeysAndOvershootsLinear() throws ParseException {
        AnimationClip spline = clips().get("animation.golem.spline");
        assertArrayEquals(new float[] {10, 0, 0}, spline.sample(1f).get("head").position, EPS);
        float mid = spline.sample(1.5f).get("head").position[0];
        assertTrue(mid > 10f, "the curve bulges above the straight segment: " + mid);
        assertArrayEquals(new float[] {10, 0, 0}, spline.sample(2f).get("head").position, EPS);
    }

    @Test
    void rejectsBrokenAnimations() {
        assertThrows(ParseException.class, () -> AnimationParser.parse("{}"));
        assertThrows(ParseException.class, () -> AnimationParser.parse("nope"));
        assertThrows(ParseException.class, () -> AnimationParser.parse(
                "{\"animations\":{\"a\":{\"bones\":{\"x\":{\"rotation\":[\"1 +\",0,0]}}}}}"));
        assertThrows(ParseException.class, () -> AnimationParser.parse(
                "{\"animations\":{\"a\":{\"bones\":{\"x\":{\"rotation\":{\"abc\":[0,0,0]}}}}}}"));
        assertThrows(ParseException.class, () -> AnimationParser.parse(
                "{\"animations\":{\"a\":{\"bones\":{\"x\":{\"rotation\":[1,2]}}}}}"));
    }

    @Test
    void numericStringsAreAccepted() throws ParseException {
        Map<String, AnimationClip> c = AnimationParser.parse(
                "{\"animations\":{\"a\":{\"bones\":{\"x\":{\"position\":[\"1.5\",0,0]}}}}}");
        assertEquals(1.5f, c.get("a").sample(0).get("x").position[0], EPS);
    }

    // ---------- controller ----------

    @Test
    void controllerPlaysAndTicks() throws ParseException {
        AnimationController c = new AnimationController(clips());
        assertEquals(0, c.pose(0).boneNames().size(), "nothing playing");
        c.play("animation.golem.idle");
        for (int i = 0; i < 10; i++) {
            c.tick(); // 0.5 s
        }
        assertArrayEquals(new float[] {0, 20, 0}, c.pose(0).get("head").rotation, EPS);
        assertArrayEquals(new float[] {0, 20.2f, 0}, c.pose(0.1f).get("head").rotation, 0.01f, "0.1 of a tick is 0.005 s: +0.2 degrees at 40 degrees per second");
        assertTrue(c.isPlaying("animation.golem.idle"));
        assertThrows(IllegalArgumentException.class, () -> c.play("nope"));
    }

    @Test
    void playingTheCurrentClipDoesNotRestartIt() throws ParseException {
        AnimationController c = new AnimationController(clips());
        c.play("animation.golem.idle");
        for (int i = 0; i < 10; i++) {
            c.tick();
        }
        c.play("animation.golem.idle");
        assertArrayEquals(new float[] {0, 20, 0}, c.pose(0).get("head").rotation, EPS);
        c.restart();
        assertArrayEquals(new float[] {0, 0, 0}, c.pose(0).get("head").rotation, EPS);
    }

    @Test
    void transitionsBlendFromThePreviousPose() throws ParseException {
        AnimationController c = new AnimationController(clips());
        c.play("animation.golem.idle");
        for (int i = 0; i < 20; i++) {
            c.tick(); // 1 s: head yaw 40
        }
        c.play("animation.golem.attack").transition(10);
        // right after the switch the pose is still the old one
        assertEquals(40f, c.pose(0).get("head").rotation[1], 0.01f);
        for (int i = 0; i < 5; i++) {
            c.tick();
        }
        float yaw = c.pose(0).get("head").rotation[1];
        assertTrue(yaw > 15f && yaw < 25f, "halfway through the blend: " + yaw);
        for (int i = 0; i < 5; i++) {
            c.tick();
        }
        assertEquals(0f, c.pose(0).get("head").rotation[1], 0.01f, "blend finished: the new animation owns the pose");
        assertNotNull(c.pose(0).get("arm"));
    }

    @Test
    void thenQueuesTheNextClipWhenAOneShotEnds() throws ParseException {
        AnimationController c = new AnimationController(clips());
        c.play("animation.golem.attack").then("animation.golem.idle");
        for (int i = 0; i < 19; i++) {
            c.tick();
        }
        assertEquals("animation.golem.attack", c.currentName());
        c.tick(); // 1.0 s: finished
        c.tick();
        assertEquals("animation.golem.idle", c.currentName());
        assertThrows(IllegalArgumentException.class, () -> c.play("animation.golem.attack").then("ghost"));
    }

    @Test
    void speedScalesPlayback() throws ParseException {
        AnimationController c = new AnimationController(clips());
        c.play("animation.golem.idle").transition(0);
        c.speed(2f);
        for (int i = 0; i < 5; i++) {
            c.tick();
        }
        assertArrayEquals(new float[] {0, 20, 0}, c.pose(0).get("head").rotation, EPS, "0.25 s of ticks at 2x = 0.5 s of animation");
    }

    // ---------- geometry ----------

    private static boolean pointsOutward(ModelGeometry.Quad q, Vector3f center) {
        Vector3f faceCenter = new Vector3f();
        for (ModelGeometry.Vertex v : q.vertices()) {
            faceCenter.add(v.x(), v.y(), v.z());
        }
        faceCenter.div(4f).sub(center);
        return faceCenter.dot(q.nx(), q.ny(), q.nz()) > 0;
    }

    private static Vector3f cross(ModelGeometry.Quad q) {
        ModelGeometry.Vertex a = q.vertices()[0];
        ModelGeometry.Vertex b = q.vertices()[1];
        ModelGeometry.Vertex d = q.vertices()[3];
        Vector3f e1 = new Vector3f(b.x() - a.x(), b.y() - a.y(), b.z() - a.z());
        Vector3f e2 = new Vector3f(d.x() - a.x(), d.y() - a.y(), d.z() - a.z());
        return e1.cross(e2);
    }

    @Test
    void aCubeHasSixOutwardFacingQuadsWithConsistentWinding() throws ParseException {
        GeoModel model = GeoParser.parse("""
                {"minecraft:geometry":[{"description":{"texture_width":64,"texture_height":64},"bones":[
                  {"name":"b","pivot":[0,0,0],"cubes":[{"origin":[-2,0,-3],"size":[4,6,6],"uv":[0,0]}]}]}]}
                """);
        List<ModelGeometry.Quad> quads = ModelGeometry.build(model, new Pose());
        assertEquals(6, quads.size());
        Vector3f center = new Vector3f(0, 3, 0);
        for (ModelGeometry.Quad q : quads) {
            assertTrue(pointsOutward(q, center), "normal points away from the cube");
            Vector3f c = cross(q).normalize();
            assertEquals(q.nx(), c.x, EPS, "counter-clockwise winding matches the normal");
            assertEquals(q.ny(), c.y, EPS);
            assertEquals(q.nz(), c.z, EPS);
        }
    }

    @Test
    void xIsMirroredIntoRenderSpace() throws ParseException {
        GeoModel model = GeoParser.parse("""
                {"minecraft:geometry":[{"description":{"texture_width":16,"texture_height":16},"bones":[
                  {"name":"b","cubes":[{"origin":[1,0,0],"size":[2,2,2],"uv":[0,0]}]}]}]}
                """);
        float minX = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        for (ModelGeometry.Quad q : ModelGeometry.build(model, new Pose())) {
            for (ModelGeometry.Vertex v : q.vertices()) {
                minX = Math.min(minX, v.x());
                maxX = Math.max(maxX, v.x());
            }
        }
        assertEquals(-3f, minX, EPS);
        assertEquals(-1f, maxX, EPS);
    }

    @Test
    void boxUvLayoutOfTheFrontFace() throws ParseException {
        GeoModel model = GeoParser.parse("""
                {"minecraft:geometry":[{"description":{"texture_width":64,"texture_height":32},"bones":[
                  {"name":"b","cubes":[{"origin":[0,0,0],"size":[8,12,4],"uv":[16,16]}]}]}]}
                """);
        ModelGeometry.Quad south = ModelGeometry.build(model, new Pose()).stream()
                .filter(q -> q.nz() > 0.99f).findFirst().orElseThrow();
        // south region: u = 16 + d + w + d = 16 + 4 + 8 + 4 = 32 .. 40, v = 16 + 4 = 20 .. 32
        ModelGeometry.Vertex bl = south.vertices()[0];
        ModelGeometry.Vertex tr = south.vertices()[2];
        assertEquals(32f / 64f, bl.u(), EPS);
        assertEquals(32f / 32f, bl.v(), EPS);
        assertEquals(40f / 64f, tr.u(), EPS);
        assertEquals(20f / 32f, tr.v(), EPS);
    }

    @Test
    void perFaceUvAndMissingFaces() throws ParseException {
        GeoModel model = GeoParser.parse(GEO);
        List<ModelGeometry.Quad> quads = ModelGeometry.build(model, new Pose());
        // body (6) + head (6) + arm: only north and east are defined
        assertEquals(6 + 6 + 2, quads.size());
    }

    @Test
    void mirrorSwapsAndFlipsTheTextureHorizontally() throws ParseException {
        String json = """
                {"minecraft:geometry":[{"description":{"texture_width":64,"texture_height":64},"bones":[
                  {"name":"b","cubes":[{"origin":[0,0,0],"size":[4,4,4],"uv":[0,0],"mirror":%s}]}]}]}
                """;
        ModelGeometry.Quad normal = ModelGeometry.build(GeoParser.parse(json.formatted("false")), new Pose()).stream()
                .filter(q -> q.nz() > 0.99f).findFirst().orElseThrow();
        ModelGeometry.Quad mirrored = ModelGeometry.build(GeoParser.parse(json.formatted("true")), new Pose()).stream()
                .filter(q -> q.nz() > 0.99f).findFirst().orElseThrow();
        assertEquals(normal.vertices()[0].u(), mirrored.vertices()[1].u(), EPS);
        assertEquals(normal.vertices()[1].u(), mirrored.vertices()[0].u(), EPS);
    }

    @Test
    void boneRotationTurnsChildrenAroundThePivot() throws ParseException {
        GeoModel model = GeoParser.parse("""
                {"minecraft:geometry":[{"description":{"texture_width":16,"texture_height":16},"bones":[
                  {"name":"arm","pivot":[0,10,0],"cubes":[{"origin":[-1,0,-1],"size":[2,10,2],"uv":[0,0]}]}]}]}
                """);
        Pose pose = new Pose();
        pose.get("arm").rotation = new float[] {0, 0, 90}; // roll 90 degrees around the pivot
        float topBefore = 10f;
        float maxY = -Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        for (ModelGeometry.Quad q : ModelGeometry.build(model, new Pose())) {
            for (ModelGeometry.Vertex v : q.vertices()) {
                maxY = Math.max(maxY, v.y());
            }
        }
        assertEquals(topBefore, maxY, EPS);
        float maxX = -Float.MAX_VALUE;
        float minX = Float.MAX_VALUE;
        for (ModelGeometry.Quad q : ModelGeometry.build(model, pose)) {
            for (ModelGeometry.Vertex v : q.vertices()) {
                minY = Math.min(minY, v.y());
                maxY = Math.max(maxY, v.y());
                minX = Math.min(minX, v.x());
                maxX = Math.max(maxX, v.x());
            }
        }
        // A 10-long arm hanging from the pivot at y=10 now points sideways: the span moves from y to x.
        assertEquals(10f, Math.max(Math.abs(minX), Math.abs(maxX)), 0.01f);
        assertEquals(10f, minY + 1f, 0.01f, "the arm is level with the pivot, +-1 thick");
        assertEquals(11f, maxY, 0.01f);
    }

    @Test
    void childBonesInheritTheirParentsTransform() throws ParseException {
        GeoModel model = GeoParser.parse("""
                {"minecraft:geometry":[{"description":{"texture_width":16,"texture_height":16},"bones":[
                  {"name":"parent","pivot":[0,0,0]},
                  {"name":"child","parent":"parent","pivot":[0,0,0],"cubes":[{"origin":[0,0,0],"size":[1,1,1],"uv":[0,0]}]}]}]}
                """);
        Pose pose = new Pose();
        pose.get("parent").position = new float[] {0, 5, 0};
        float minY = Float.MAX_VALUE;
        for (ModelGeometry.Quad q : ModelGeometry.build(model, pose)) {
            for (ModelGeometry.Vertex v : q.vertices()) {
                minY = Math.min(minY, v.y());
            }
        }
        assertEquals(5f, minY, EPS);
    }

    @Test
    void scaleGrowsTheBoneAroundItsPivot() throws ParseException {
        GeoModel model = GeoParser.parse("""
                {"minecraft:geometry":[{"description":{"texture_width":16,"texture_height":16},"bones":[
                  {"name":"b","pivot":[0,0,0],"cubes":[{"origin":[0,0,0],"size":[2,2,2],"uv":[0,0]}]}]}]}
                """);
        Pose pose = new Pose();
        pose.get("b").scale = new float[] {2, 2, 2};
        float maxY = -Float.MAX_VALUE;
        for (ModelGeometry.Quad q : ModelGeometry.build(model, pose)) {
            for (ModelGeometry.Vertex v : q.vertices()) {
                maxY = Math.max(maxY, v.y());
            }
        }
        assertEquals(4f, maxY, EPS);
    }

    @Test
    void conventionsMatchGeckoLib() {
        assertArrayEquals(new float[] {-3, 4, 5}, Conventions.position(new float[] {3, 4, 5}), EPS);
        float[] r = Conventions.rotation(new float[] {90, 180, 45});
        assertEquals(-Math.PI / 2, r[0], EPS);
        assertEquals(-Math.PI, r[1], EPS);
        assertEquals(Math.PI / 4, r[2], EPS);
    }

    @Test
    void poseBlendTreatsMissingBonesAsNoChange() {
        Pose a = new Pose();
        a.get("x").position = new float[] {10, 0, 0};
        Pose b = new Pose();
        b.get("y").scale = new float[] {3, 3, 3};
        Pose half = Pose.blend(a, b, 0.5f);
        assertEquals(5f, half.get("x").position[0], EPS);
        assertEquals(2f, half.get("y").scale[0], EPS);
    }

    // ---------- the example files shipped with the testmod ----------

    private static java.nio.file.Path testmodAssets() {
        java.nio.file.Path dir = java.nio.file.Path.of("").toAbsolutePath();
        while (dir != null && !java.nio.file.Files.isDirectory(dir.resolve("src/testmod"))) {
            dir = dir.getParent();
        }
        assertNotNull(dir, "repository root not found");
        return dir.resolve("src/testmod/resources/assets/bloomberto_testmod");
    }

    @Test
    void theTestmodExampleFilesAreReadable() throws Exception {
        java.nio.file.Path assets = testmodAssets();
        GeoModel model = GeoParser.parse(java.nio.file.Files.readString(assets.resolve("geo/totem.geo.json")));
        assertEquals("geometry.totem", model.identifier());
        assertEquals(List.of("root", "base", "head"), model.bones().stream().map(GeoModel.Bone::name).toList());
        Map<String, AnimationClip> clips = AnimationParser.parse(
                java.nio.file.Files.readString(assets.resolve("animations/totem.animation.json")));
        assertEquals(3, clips.size());
        AnimationController controller = new AnimationController(clips);
        controller.play("animation.totem.idle").transition(5);
        for (int i = 0; i < 40; i++) {
            controller.tick(); // 2 s: the head has turned 180 degrees
        }
        assertEquals(180f, controller.pose(0).get("head").rotation[1], 0.01f);
        assertEquals(12, ModelGeometry.build(model, controller.pose(0)).size(), "two cubes, six faces each");
    }
}
