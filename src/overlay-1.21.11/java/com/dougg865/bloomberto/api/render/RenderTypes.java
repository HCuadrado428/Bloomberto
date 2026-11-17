package com.dougg865.bloomberto.api.render;

import com.dougg865.bloomberto.Bloomberto;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

/**
 * Ready-made render types for glowing visuals. Client only. Pass them to a {@code MultiBufferSource}, e.g.
 * {@code ctx.buffers().getBuffer(RenderTypes.additive(texture))}.
 *
 * <ul>
 *   <li>{@link #additive}: the texture is added to what is behind it (light, fire, energy); no depth write.</li>
 *   <li>{@link #emissive}: translucent and not affected by light, like glowing mob layers.</li>
 *   <li>{@link #glow}: Minecraft's additive "eyes" look.</li>
 * </ul>
 */
public final class RenderTypes {
    private static final RenderPipeline ADDITIVE_PIPELINE = RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
            .withLocation(Bloomberto.id("bloomberto", "pipeline/additive"))
            .withVertexShader("core/position_tex_color")
            .withFragmentShader("core/position_tex_color")
            .withSampler("Sampler0")
            .withBlend(BlendFunction.ADDITIVE)
            .withDepthWrite(false)
            .withCull(false)
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
            .build();
    private static final Map<Identifier, RenderType> ADDITIVE = new ConcurrentHashMap<>();

    private RenderTypes() {
    }

    /** Vertex format: position, uv, color ({@code POSITION_TEX_COLOR}); write vertices in any order through the builder. */
    public static RenderType additive(Identifier texture) {
        return ADDITIVE.computeIfAbsent(texture, tex -> RenderType.create("bloomberto_additive",
                RenderSetup.builder(ADDITIVE_PIPELINE).withTexture("Sampler0", tex).sortOnUpload().createRenderSetup()));
    }

    /** Translucent, fullbright. Vertex format: the entity format (position, color, uv, overlay, light, normal). */
    public static RenderType emissive(Identifier texture) {
        return net.minecraft.client.renderer.rendertype.RenderTypes.entityTranslucentEmissive(texture);
    }

    /** Minecraft's glowing-eyes render type. */
    public static RenderType glow(Identifier texture) {
        return net.minecraft.client.renderer.rendertype.RenderTypes.eyes(texture);
    }
}
