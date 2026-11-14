package com.dougg865.bloomberto.api.animation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dougg865.bloomberto.api.animation.model.AnimationClip;
import com.dougg865.bloomberto.api.animation.model.AnimationParser;
import com.dougg865.bloomberto.api.animation.molang.MolangContext;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MolangAnimationTest {
    private static final String JSON = """
            {"format_version":"1.8.0","animations":{"animation.t.wave":{"loop":true,"animation_length":2.0,
              "bones":{"arm":{"rotation":["math.sin(query.anim_time * 90) * 10", 0, "variable.tilt"],
                              "position":{"0":[0,0,0],"1":[0,"query.anim_time * 2",0]}}}}}}
            """;

    @Test
    void expressionsAreEvaluatedAtSampleTime() throws Exception {
        Map<String, AnimationClip> clips = AnimationParser.parse(JSON);
        AnimationClip wave = clips.get("animation.t.wave");
        MolangContext ctx = new MolangContext().set("variable.tilt", 7);
        var pose = wave.sample(1f, ctx);
        assertEquals(10f, pose.get("arm").rotation[0], 1e-4f);
        assertEquals(7f, pose.get("arm").rotation[2], 1e-4f);
        // life_time is the unwrapped playback time; anim_time wraps with the loop
        pose = wave.sample(2.5f, ctx);
        assertEquals((float) (Math.sin(Math.toRadians(0.5 * 90)) * 10), pose.get("arm").rotation[0], 1e-3f);
    }

    @Test
    void withoutContextExpressionsFallBackToZero() throws Exception {
        AnimationClip wave = AnimationParser.parse(JSON).get("animation.t.wave");
        var pose = wave.sample(1f);
        assertEquals(0f, pose.get("arm").rotation[0], 1e-6f);
    }

    @Test
    void numbersStillWorkAsStrings() throws Exception {
        var clips = AnimationParser.parse("{\"animations\":{\"a\":{\"bones\":{\"b\":{\"position\":[\"1.5\",0,0]}}}}}");
        assertEquals(1.5f, clips.get("a").sample(0f).get("b").position[0], 1e-6f);
    }

    @Test
    void badExpressionIsAClearError() {
        var ex = org.junit.jupiter.api.Assertions.assertThrows(com.dougg865.bloomberto.api.animation.model.ParseException.class,
                () -> AnimationParser.parse("{\"animations\":{\"a\":{\"bones\":{\"b\":{\"position\":[\"math.nope(1)\",0,0]}}}}}"));
        org.junit.jupiter.api.Assertions.assertTrue(ex.getMessage().contains("math.nope"));
    }
}
