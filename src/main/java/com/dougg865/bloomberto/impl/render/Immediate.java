package com.dougg865.bloomberto.impl.render;

import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Matrix4f;

/**
 * Immediate-mode drawing with the current shader. Minecraft 1.21 replaced {@code Tesselator.getBuilder()} /
 * {@code BufferBuilder.end()} with {@code Tesselator.begin()} / {@code buildOrThrow()}; this class hides that.
 */
public final class Immediate {
    private Immediate() {
    }

    /** Draws a filled rectangle in the plane z = 0 with the shader that is currently set; the vertex format is POSITION_COLOR. */
    public static void quadPositionColor(Matrix4f pose, float x, float y, float width, float height, int argb) {
        float[][] corners = {{x, y + height}, {x + width, y + height}, {x + width, y}, {x, y}};
        //? if >=1.21 {
        var builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (float[] c : corners) {
            builder.addVertex(pose, c[0], c[1], 0f).setColor(argb);
        }
        BufferUploader.drawWithShader(builder.buildOrThrow());
        //?} else {
        /*var builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (float[] c : corners) {
            builder.vertex(pose, c[0], c[1], 0f).color(Vtx.r(argb), Vtx.g(argb), Vtx.b(argb), Vtx.a(argb)).endVertex();
        }
        BufferUploader.drawWithShader(builder.end());
        *///?}
    }
}
