package com.dougg865.bloomberto.api.animation.model;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Draws a block entity with an animated Blockbench model, centered on the block. Register it with
 * {@code BlockEntityRenderers.register(TYPE, ctx -> new AnimatedBlockEntityRenderer<>(geo, texture))} from client setup.
 * The block entity must implement {@link Animatable}. Client only.
 *
 * <p>The block that owns the block entity must be created with {@code .noOcclusion()}: an opaque block has light level 0 inside
 * it, so the model would be drawn almost black. If the block should have no shape of its own, also make its
 * {@code getRenderShape} return {@code RenderShape.INVISIBLE}.
 */
public class AnimatedBlockEntityRenderer<T extends BlockEntity & Animatable> implements BlockEntityRenderer<T> {
    private final ResourceLocation geometry;
    private final ResourceLocation texture;
    private long lastTick = Long.MIN_VALUE;

    public AnimatedBlockEntityRenderer(ResourceLocation geometry, ResourceLocation texture) {
        this.geometry = geometry;
        this.texture = texture;
    }

    /** Override to rotate or move the model (for example to follow the block's facing). The model is already centered. */
    protected void transform(T blockEntity, PoseStack poseStack) {
    }

    @Override
    public void render(T blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        if (blockEntity.getLevel() != null) {
            long now = blockEntity.getLevel().getGameTime();
            if (now != lastTick) {
                lastTick = now;
                blockEntity.animationController().tick();
            }
        }
        poseStack.pushPose();
        poseStack.translate(0.5, 0.0, 0.5);
        transform(blockEntity, poseStack);
        GeoRenderer.render(ModelLibrary.model(geometry), blockEntity.animationController().pose(partialTick), poseStack,
                buffers.getBuffer(RenderType.entityCutoutNoCull(texture)), light, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        poseStack.popPose();
    }
}
