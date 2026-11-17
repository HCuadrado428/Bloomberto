package com.dougg865.bloomberto.api.animation.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/**
 * Draws an entity with an animated Blockbench model. Register it for your entity type with your loader's renderer
 * registration, e.g. {@code EntityRenderers.register(TYPE, ctx -> new AnimatedEntityRenderer<>(ctx, geo, texture))}
 * from client setup. The entity must implement {@link Animatable}. Client only.
 */
public class AnimatedEntityRenderer<T extends Entity & Animatable>
        extends EntityRenderer<T, AnimatedEntityRenderer.State> {
    /** What is drawn this frame: the pose is sampled when the entity is extracted. */
    public static class State extends EntityRenderState {
        public Pose pose = new Pose();
        public float yaw;
    }

    private final Identifier geometry;
    private final Identifier texture;

    public AnimatedEntityRenderer(EntityRendererProvider.Context context, Identifier geometry, Identifier texture) {
        super(context);
        this.geometry = geometry;
        this.texture = texture;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(T entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        // catches up the ticks that passed since the last frame, so low frame rates do not slow the animation down
        entity.animationController().tickAt(entity.tickCount);
        state.pose = entity.animationController().pose(partialTick);
        state.yaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180f - state.yaw));
        GeoModel model = ModelLibrary.model(geometry);
        Pose pose = state.pose;
        int light = state.lightCoords;
        collector.submitCustomGeometry(poseStack, RenderTypes.entityCutoutNoCull(texture), (last, consumer) -> {
            PoseStack local = new PoseStack();
            local.last().set(last);
            GeoRenderer.render(model, pose, local, consumer, light, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        });
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    /** The model's texture. */
    public Identifier texture() {
        return texture;
    }
}
