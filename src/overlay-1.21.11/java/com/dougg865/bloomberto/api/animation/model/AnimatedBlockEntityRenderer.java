package com.dougg865.bloomberto.api.animation.model;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Draws a block entity with an animated Blockbench model, centered on the block. Register it with
 * {@code BlockEntityRenderers.register(TYPE, ctx -> new AnimatedBlockEntityRenderer<>(geo, texture))} from client setup.
 * The block entity must implement {@link Animatable}. Client only.
 *
 * <p>The block that owns the block entity must be created with {@code .noOcclusion()}: an opaque block has light level 0 inside
 * it, so the model would be drawn almost black. If the block should have no shape of its own, also make its
 * {@code getRenderShape} return {@code RenderShape.INVISIBLE}.
 */
public class AnimatedBlockEntityRenderer<T extends BlockEntity & Animatable>
        implements BlockEntityRenderer<T, AnimatedBlockEntityRenderer.State> {
    /** What is drawn this frame: the pose and transform are captured when the block entity is extracted. */
    public static class State extends BlockEntityRenderState {
        public Pose pose = new Pose();
        public final Matrix4f transform = new Matrix4f();
    }

    private final Identifier geometry;
    private final Identifier texture;

    public AnimatedBlockEntityRenderer(Identifier geometry, Identifier texture) {
        this.geometry = geometry;
        this.texture = texture;
    }

    /** Override to rotate or move the model (for example to follow the block's facing). The model is already centered. */
    protected void transform(T blockEntity, PoseStack poseStack) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(T blockEntity, State state, float partialTick, Vec3 cameraPosition,
                                   ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTick, cameraPosition, breakProgress);
        if (blockEntity.getLevel() != null) {
            // per controller, not per renderer: one renderer draws every block entity of its type
            blockEntity.animationController().tickAt(blockEntity.getLevel().getGameTime());
        }
        state.pose = blockEntity.animationController().pose(partialTick);
        PoseStack local = new PoseStack();
        transform(blockEntity, local);
        state.transform.set(local.last().pose());
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.5, 0.0, 0.5);
        poseStack.mulPose(state.transform);
        GeoModel model = ModelLibrary.model(geometry);
        Pose pose = state.pose;
        int light = state.lightCoords;
        collector.submitCustomGeometry(poseStack, RenderTypes.entityCutoutNoCull(texture), (last, consumer) -> {
            PoseStack local = new PoseStack();
            local.last().set(last);
            GeoRenderer.render(model, pose, local, consumer, light, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        });
        poseStack.popPose();
    }
}
