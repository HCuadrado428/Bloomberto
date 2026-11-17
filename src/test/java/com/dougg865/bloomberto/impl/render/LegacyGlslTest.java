package com.dougg865.bloomberto.impl.render;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LegacyGlslTest {
    private static final String BLUR = """
            #version 150

            uniform sampler2D DiffuseSampler;

            uniform vec2 InSize;
            uniform vec2 BlurDir;
            uniform float Radius; // pixels

            in vec2 texCoord;
            out vec4 fragColor;

            void main() {
                fragColor = texture(DiffuseSampler, texCoord + BlurDir / InSize * Radius);
            }
            """;

    @Test
    void movesPlainUniformsIntoTheBlock() {
        LegacyGlsl.Result r = LegacyGlsl.translate(BLUR, List.of("InSize", "OutSize"));
        assertTrue(r.source().startsWith("#version 330\n"));
        assertFalse(r.source().contains("#version 150"));
        assertFalse(r.source().contains("uniform float Radius"));
        assertTrue(r.source().contains("uniform sampler2D DiffuseSampler;"));
        assertTrue(r.source().contains("#define Radius (_bb_Radius.x)"));
        assertTrue(r.source().contains("#define BlurDir (_bb_BlurDir.xy)"));
        assertEquals(List.of("DiffuseSampler"), r.samplers());
        assertEquals(4, r.slots().size()); // InSize, BlurDir, Radius + reserved OutSize
        assertEquals(64, r.blockSize());
        assertEquals(0, r.slots().get("InSize").index());
        assertEquals(3, r.slots().get("OutSize").index());
    }

    @Test
    void stagesShareOneBlockAndMatricesTakeFourRows() {
        String vsh = "#version 150\nin vec3 Position;\nuniform mat4 ModelViewMat;\nuniform mat4 ProjMat;\nvoid main() {}\n";
        String fsh = "#version 150\nuniform float Time;\nout vec4 fragColor;\nvoid main() {}\n";
        LegacyGlsl.Program p = LegacyGlsl.translateAll(List.of(vsh, fsh), List.of("ModelViewMat", "ProjMat"));
        assertEquals(3, p.slots().size());
        assertEquals((4 + 4 + 1) * 16, p.blockSize());
        String header = p.sources().get(0).substring(0, p.sources().get(0).indexOf("#define"));
        assertTrue(p.sources().get(1).startsWith(header), "both stages must declare the same block");
        assertTrue(header.contains("mat4 _bb_ProjMat;"));
    }

    @Test
    void packsValuesInBlockOrder() {
        LegacyGlsl.Program p = LegacyGlsl.translateAll(List.of("uniform mat4 M;\nuniform float A;\nuniform vec3 B;\n"), List.of());
        float[] m = new float[16];
        for (int i = 0; i < 16; i++) {
            m[i] = i;
        }
        float[] packed = LegacyGlsl.pack(p.slots(), Map.of("M", m, "B", new float[] {7, 8, 9}));
        assertEquals(24, packed.length);
        assertArrayEquals(m, java.util.Arrays.copyOf(packed, 16));
        assertEquals(0f, packed[16]); // A missing: zeros
        assertArrayEquals(new float[] {7, 8, 9, 0}, java.util.Arrays.copyOfRange(packed, 20, 24));
    }

    @Test
    void rejectsArraysAndUnknownTypes() {
        assertThrows(IllegalArgumentException.class, () -> LegacyGlsl.translate("uniform float Weights[4];\n", List.of()));
        assertThrows(IllegalArgumentException.class, () -> LegacyGlsl.translate("uniform mat3 N;\n", List.of()));
    }

    @Test
    void emptyShaderStillHasAValidBlock() {
        LegacyGlsl.Result r = LegacyGlsl.translate("#version 150\nvoid main() {}\n", List.of());
        assertTrue(r.source().contains("vec4 _bb_unused;"));
        assertEquals(16, r.blockSize());
    }
}
