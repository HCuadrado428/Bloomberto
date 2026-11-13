package com.dougg865.bloomberto.api.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;

/**
 * What a {@link ClientEvents#WORLD_RENDER} listener gets. The pose stack already has the camera's rotation
 * applied, so positions must be given relative to the camera: use {@link #cameraPos()} or {@link #relative(Vec3)}.
 *
 * @param poseStack   the level's pose stack
 * @param camera      the camera
 * @param partialTick the fraction of the current tick that has passed (0-1)
 * @param buffers     the shared buffer source; Bloomberto flushes it after all listeners ran
 */
public record WorldRenderContext(PoseStack poseStack, Camera camera, float partialTick, MultiBufferSource.BufferSource buffers) {
    public Vec3 cameraPos() {
        return camera.getPosition();
    }

    /** {@code worldPos} expressed relative to the camera, as needed when drawing with {@link #poseStack()}. */
    public Vec3 relative(Vec3 worldPos) {
        return worldPos.subtract(camera.getPosition());
    }
}
