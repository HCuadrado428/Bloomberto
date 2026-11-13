package com.dougg865.bloomberto.impl.render;

import com.dougg865.bloomberto.api.client.ClientEvents;
import com.dougg865.bloomberto.api.client.WorldRenderContext;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Matrix4f;

/**
 * The one place that fires {@link ClientEvents#WORLD_RENDER}, so every loader gives listeners the same thing.
 *
 * <p>Since Minecraft 1.20.5 the camera rotation is no longer inside the level's {@code PoseStack}; it lives in the
 * model-view matrix of {@code RenderSystem}, and loaders fire their events at slightly different moments (Fabric after that
 * matrix has already been removed, Forge and NeoForge while it is still set). To behave the same everywhere on 1.21 the bridge
 * <em>sets</em> that matrix to the camera's view matrix for the duration of the event, so the listener can draw with an
 * identity pose stack and camera-relative coordinates.
 */
public final class WorldRenderBridge {
    private WorldRenderBridge() {
    }

    /**
     * @param poseStack the loader's pose stack (1.20.1: already contains the camera rotation; 1.21: an identity stack)
     * @param view      1.21 only: the camera's view matrix; ignored on 1.20.1
     */
    public static void fire(PoseStack poseStack, Camera camera, float partialTick, Matrix4f view) {
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        WorldRenderContext context = new WorldRenderContext(poseStack, camera, partialTick, buffers);
        //? if >=1.21 {
        org.joml.Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.set(view);
        RenderSystem.applyModelViewMatrix();
        try {
            ClientEvents.WORLD_RENDER.invoke(l -> l.render(context));
            buffers.endBatch();
        } finally {
            modelView.popMatrix();
            RenderSystem.applyModelViewMatrix();
        }
        //?} else {
        /*ClientEvents.WORLD_RENDER.invoke(l -> l.render(context));
        buffers.endBatch();
        *///?}
    }
}
