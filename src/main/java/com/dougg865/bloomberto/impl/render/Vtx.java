package com.dougg865.bloomberto.impl.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * The only place that knows how to write a vertex. Minecraft 1.21 renamed the whole {@code VertexConsumer} API
 * ({@code vertex(...).color(...).endVertex()} became {@code addVertex(...).setColor(...)}); everything else in Bloomberto
 * goes through here.
 */
public final class Vtx {
    private Vtx() {
    }

    /** Position and color (for {@code POSITION_COLOR} formats). */
    public static void posColor(VertexConsumer c, PoseStack.Pose pose, float x, float y, float z, int argb) {
        //? if >=1.21 {
        c.addVertex(pose, x, y, z).setColor(argb);
        //?} else {
        /*c.vertex(pose.pose(), x, y, z).color(r(argb), g(argb), b(argb), a(argb)).endVertex();
        *///?}
    }

    /** Position, color and normal (for {@code POSITION_COLOR_NORMAL} formats, i.e. lines). */
    public static void posColorNormal(VertexConsumer c, PoseStack.Pose pose, float x, float y, float z, int argb,
                                      float nx, float ny, float nz) {
        //? if >=1.21 {
        c.addVertex(pose, x, y, z).setColor(argb).setNormal(pose, nx, ny, nz);
        //?} else {
        /*c.vertex(pose.pose(), x, y, z).color(r(argb), g(argb), b(argb), a(argb)).normal(pose.normal(), nx, ny, nz).endVertex();
        *///?}
    }

    /** Position, color, uv and light (for {@code POSITION_COLOR_TEX_LIGHTMAP} formats, e.g. {@code RenderType.text}). */
    public static void posColorUvLight(VertexConsumer c, PoseStack.Pose pose, float x, float y, float z, int argb,
                                       float u, float v, int light) {
        //? if >=1.21 {
        c.addVertex(pose, x, y, z).setColor(argb).setUv(u, v).setLight(light);
        //?} else {
        /*c.vertex(pose.pose(), x, y, z).color(r(argb), g(argb), b(argb), a(argb)).uv(u, v).uv2(light).endVertex();
        *///?}
    }

    /** Position, color and uv (for {@code POSITION_COLOR_TEX} formats, e.g. {@code RenderTypes.additive}). */
    public static void posColorUv(VertexConsumer c, PoseStack.Pose pose, float x, float y, float z, int argb, float u, float v) {
        //? if >=1.21 {
        c.addVertex(pose, x, y, z).setColor(argb).setUv(u, v);
        //?} else {
        /*c.vertex(pose.pose(), x, y, z).color(r(argb), g(argb), b(argb), a(argb)).uv(u, v).endVertex();
        *///?}
    }

    /** The entity vertex format: position, color, uv, overlay, light and normal (e.g. {@code RenderType.entityCutoutNoCull}). */
    public static void entity(VertexConsumer c, PoseStack.Pose pose, float x, float y, float z, int argb, float u, float v,
                              int overlay, int light, float nx, float ny, float nz) {
        //? if >=1.21 {
        c.addVertex(pose, x, y, z).setColor(argb).setUv(u, v).setOverlay(overlay).setLight(light).setNormal(pose, nx, ny, nz);
        //?} else {
        /*c.vertex(pose.pose(), x, y, z).color(r(argb), g(argb), b(argb), a(argb)).uv(u, v).overlayCoords(overlay).uv2(light)
                .normal(pose.normal(), nx, ny, nz).endVertex();
        *///?}
    }

    public static int a(int argb) {
        return argb >>> 24;
    }

    public static int r(int argb) {
        return argb >> 16 & 0xFF;
    }

    public static int g(int argb) {
        return argb >> 8 & 0xFF;
    }

    public static int b(int argb) {
        return argb & 0xFF;
    }
}
