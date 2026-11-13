package com.dougg865.bloomberto.api.animation.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/**
 * Draws an entity with an animated Blockbench model. Register it for your entity type with your loader's renderer
 * registration, e.g. {@code EntityRenderers.register(TYPE, ctx -> new AnimatedEntityRenderer<>(ctx, geo, texture))}
 * from client setup. The entity must implement {@link Animatable}. Client only.
 */
public class AnimatedEntityRenderer<T extends Entity & Animatable> extends EntityRenderer<T> {
    private final ResourceLocation geometry;
    private final ResourceLocation texture;
    private final Map<Entity, Integer> lastTick = new WeakHashMap<>();

    public AnimatedEntityRenderer(EntityRendererProvider.Context context, ResourceLocation geometry, ResourceLocation texture) {
        super(context);
        this.geometry = geometry;
        this.texture = texture;
    }

    @Override
    public void render(T entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
        Integer seen = lastTick.put(entity, entity.tickCount);
        if (seen == null || seen != entity.tickCount) {
            entity.animationController().tick();
        }
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180f - yaw));
        GeoRenderer.render(ModelLibrary.model(geometry), entity.animationController().pose(partialTick), poseStack,
                buffers.getBuffer(RenderType.entityCutoutNoCull(texture)), light, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        poseStack.popPose();
        super.render(entity, yaw, partialTick, poseStack, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return texture;
    }
}
