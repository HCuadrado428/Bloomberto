package com.dougg865.bloomberto.api.animation.model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dougg865.bloomberto.api.animation.molang.MolangContext;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BbModelTest {
    private static final String PROJECT = """
            {"meta":{"format_version":"4.9","model_format":"bedrock"},"name":"golem","resolution":{"width":64,"height":32},
             "elements":[
               {"type":"cube","uuid":"c1","name":"body","from":[-4,0,-2],"to":[4,8,2],"origin":[0,0,0],"rotation":[0,0,0],
                "box_uv":true,"uv_offset":[16,16],"inflate":0.25,"mirror_uv":true},
               {"type":"cube","uuid":"c2","name":"arm","from":[4,6,-1],"to":[6,10,1],"origin":[5,10,0],"rotation":[15,25,35],
                "box_uv":false,"faces":{"north":{"uv":[0,0,2,4]},"south":{"uv":[2,0,4,4]},"up":{"uv":null}}},
               {"type":"cube","uuid":"c3","name":"loose","from":[0,0,0],"to":[1,1,1],"box_uv":true,"uv_offset":[0,0]},
               {"type":"locator","uuid":"l1","name":"hand"}
             ],
             "outliner":[
               {"name":"root","uuid":"g1","origin":[8,0,0],"rotation":[10,20,30],"children":[
                  "c1",
                  {"name":"arm_bone","uuid":"g2","origin":[5,10,0],"rotation":[0,0,0],"children":["c2"]}
               ]},
               "c3"
             ],
             "animations":[
               {"uuid":"a1","name":"animation.golem.wave","loop":"loop","length":2,"animators":{
                 "g2":{"name":"arm_bone","type":"bone","keyframes":[
                    {"channel":"rotation","time":1,"interpolation":"catmullrom","data_points":[{"x":10,"y":20,"z":30}]},
                    {"channel":"rotation","time":0,"data_points":[{"x":0,"y":0,"z":0},{"x":1,"y":2,"z":3}]},
                    {"channel":"position","time":0,"data_points":[{"x":4,"y":5,"z":6}]},
                    {"channel":"scale","time":0,"data_points":[{"x":2,"y":2,"z":2}]},
                    {"channel":"rotation","time":0.5,"data_points":[{"x":"math.sin(query.anim_time * 90)","y":0,"z":0}]}
                 ]},
                 "fx":{"name":"effects","type":"effect","keyframes":[]}
               }},
               {"uuid":"a2","name":"animation.golem.once","loop":"once","animators":{
                 "g1":{"name":"root","keyframes":[{"channel":"position","time":3,"data_points":[{"x":0,"y":1,"z":0}]}]}
               }}
             ],
             "textures":[{"name":"t.png","source":"data:image/png;base64,iVBORw0KGgo="},{"name":"ext.png","path":"x.png"}]}
            """;

    @Test
    void groupsBecomeBonesWithBedrockConversions() throws Exception {
        GeoModel model = BbModelParser.parseModel(PROJECT);
        assertEquals("geometry.golem", model.identifier());
        assertEquals(64, model.textureWidth());
        assertEquals(32, model.textureHeight());
        assertEquals(List.of("root", "arm_bone", "bb_main"), model.bones().stream().map(GeoModel.Bone::name).toList());
        GeoModel.Bone root = model.bone("root");
        assertNull(root.parent());
        assertArrayEquals(new float[] {-8, 0, 0}, root.pivot());
        assertArrayEquals(new float[] {-10, -20, 30}, root.rotation());
        assertEquals("root", model.bone("arm_bone").parent());
    }

    @Test
    void boxUvCubeIsMirroredOnX() throws Exception {
        GeoModel.Cube body = BbModelParser.parseModel(PROJECT).bone("root").cubes().get(0);
        assertArrayEquals(new float[] {-4, 0, -2}, body.origin()); // -(from.x + size.x) = -(-4 + 8)
        assertArrayEquals(new float[] {8, 8, 4}, body.size());
        assertNull(body.pivot());
        assertEquals(0.25f, body.inflate());
        assertTrue(body.mirror());
        assertEquals(16f, body.uv().boxU());
        assertEquals(16f, body.uv().boxV());
    }

    @Test
    void rotatedCubeWithPerFaceUv() throws Exception {
        GeoModel.Cube arm = BbModelParser.parseModel(PROJECT).bone("arm_bone").cubes().get(0);
        assertArrayEquals(new float[] {-6, 6, -1}, arm.origin());
        assertArrayEquals(new float[] {-5, 10, 0}, arm.pivot());
        assertArrayEquals(new float[] {-15, -25, 35}, arm.rotation());
        assertTrue(arm.uv().isPerFace());
        assertEquals(2, arm.uv().faces().size());
        GeoModel.UvRegion south = arm.uv().faces().get("south");
        assertEquals(2f, south.u());
        assertEquals(2f, south.width());
        assertEquals(4f, south.height());
    }

    @Test
    void looseCubesGoToTheMainBone() throws Exception {
        GeoModel.Bone main = BbModelParser.parseModel(PROJECT).bone("bb_main");
        assertEquals(1, main.cubes().size());
        assertEquals(0, BbModelParser.parseModel(PROJECT).bone("root").cubes().size() - 1);
    }

    @Test
    void animationsUseBedrockConversions() throws Exception {
        Map<String, AnimationClip> clips = BbModelParser.parseAnimations(PROJECT);
        assertEquals(List.of("animation.golem.wave", "animation.golem.once"), List.copyOf(clips.keySet()));
        AnimationClip wave = clips.get("animation.golem.wave");
        assertEquals(AnimationClip.Loop.LOOP, wave.loop());
        assertEquals(2f, wave.length());
        AnimationClip.BoneChannels arm = wave.bones().get("arm_bone");
        assertEquals(3, arm.rotation().size());
        // sorted by time; the dual keyframe has a pre (0,0,0) and a post (-1,-2,3)
        assertEquals(0f, arm.rotation().get(0).time());
        assertArrayEquals(new float[] {0, 0, 0}, arm.rotation().get(0).pre());
        assertArrayEquals(new float[] {-1, -2, 3}, arm.rotation().get(0).post());
        assertArrayEquals(new float[] {-10, -20, 30}, arm.rotation().get(2).post());
        assertEquals(AnimationClip.Interp.CATMULLROM, arm.rotation().get(2).interp());
        assertArrayEquals(new float[] {-4, 5, 6}, arm.position().get(0).post());
        assertArrayEquals(new float[] {2, 2, 2}, arm.scale().get(0).post());
        assertEquals(false, wave.bones().containsKey("effects"));
    }

    @Test
    void molangValuesAreNegatedToo() throws Exception {
        AnimationClip wave = BbModelParser.parseAnimations(PROJECT).get("animation.golem.wave");
        // at anim_time 1 the expression is sin(90) = 1, mirrored to -1 on the x rotation of the keyframe at 0.5
        var pose = wave.sample(0.5f, new MolangContext());
        assertEquals((float) -Math.sin(Math.toRadians(0.5 * 90)), pose.get("arm_bone").rotation[0], 1e-4f);
    }

    @Test
    void lengthFallsBackToTheLastKeyframe() throws Exception {
        AnimationClip once = BbModelParser.parseAnimations(PROJECT).get("animation.golem.once");
        assertEquals(AnimationClip.Loop.ONCE, once.loop());
        assertEquals(3f, once.length());
    }

    @Test
    void embeddedTexturesAreDecoded() throws Exception {
        List<byte[]> textures = BbModelParser.embeddedTextures(PROJECT);
        assertEquals(2, textures.size());
        assertEquals(8, textures.get(0).length); // the PNG signature
        assertNull(textures.get(1));
    }

    @Test
    void notAProjectIsRejected() {
        assertThrows(ParseException.class, () -> BbModelParser.parseModel("{}"));
        assertThrows(ParseException.class, () -> BbModelParser.parseModel("nope"));
        assertThrows(ParseException.class, () -> BbModelParser.parseModel(
                "{\"elements\":[{\"type\":\"cube\",\"uuid\":\"a\",\"from\":[1,2],\"to\":[1,2,3]}],\"outliner\":[\"a\"]}"));
    }

    @Test
    void theTestmodBbmodelMatchesItsGeoJson() throws Exception {
        java.nio.file.Path dir = java.nio.file.Path.of("").toAbsolutePath();
        while (dir != null && !java.nio.file.Files.isDirectory(dir.resolve("src/testmod"))) {
            dir = dir.getParent();
        }
        java.nio.file.Path assets = dir.resolve("src/testmod/resources/assets/bloomberto_testmod");
        GeoModel fromGeo = GeoParser.parse(java.nio.file.Files.readString(assets.resolve("geo/totem.geo.json")));
        String project = java.nio.file.Files.readString(assets.resolve("models/totem.bbmodel"));
        GeoModel fromBb = BbModelParser.parseModel(project);
        assertEquals(fromGeo.bones().stream().map(GeoModel.Bone::name).toList(),
                fromBb.bones().stream().map(GeoModel.Bone::name).toList());
        for (GeoModel.Bone geo : fromGeo.bones()) {
            GeoModel.Bone bb = fromBb.bone(geo.name());
            assertEquals(geo.parent(), bb.parent());
            assertArrayEquals(geo.pivot(), bb.pivot());
            assertEquals(geo.cubes().size(), bb.cubes().size());
            for (int i = 0; i < geo.cubes().size(); i++) {
                assertArrayEquals(geo.cubes().get(i).origin(), bb.cubes().get(i).origin());
                assertArrayEquals(geo.cubes().get(i).size(), bb.cubes().get(i).size());
                assertEquals(geo.cubes().get(i).uv().boxU(), bb.cubes().get(i).uv().boxU());
                assertEquals(geo.cubes().get(i).uv().boxV(), bb.cubes().get(i).uv().boxV());
            }
        }
        assertTrue(BbModelParser.parseAnimations(project).containsKey("animation.totem.sway"));
    }
}
