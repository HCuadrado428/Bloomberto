package com.dougg865.bloomberto.api.animation.model;

import com.dougg865.bloomberto.impl.render.Vtx;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

/** Draws a {@link GeoModel} with a {@link Pose} into a vertex consumer using the entity vertex format. Client only. */
public final class GeoRenderer {
    private GeoRenderer() {
    }

    /**
     * @param consumer from a render type that uses the entity format, e.g. {@code RenderType.entityCutoutNoCull(texture)}
     * @param light    packed light, e.g. from the renderer's arguments
     * @param overlay  packed overlay, usually {@code OverlayTexture.NO_OVERLAY}
     * @param argb     tint color
     */
    public static void render(GeoModel model, Pose pose, PoseStack poseStack, VertexConsumer consumer, int light, int overlay,
                              int argb) {
        poseStack.pushPose();
        poseStack.scale(1f / 16f, 1f / 16f, 1f / 16f);
        PoseStack.Pose last = poseStack.last();
        for (ModelGeometry.Quad quad : ModelGeometry.build(model, pose)) {
            for (ModelGeometry.Vertex v : quad.vertices()) {
                Vtx.entity(consumer, last, v.x(), v.y(), v.z(), argb, v.u(), v.v(), overlay, light, quad.nx(), quad.ny(), quad.nz());
            }
        }
        poseStack.popPose();
    }
}
