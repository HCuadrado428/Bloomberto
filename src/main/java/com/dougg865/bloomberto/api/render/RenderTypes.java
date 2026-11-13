package com.dougg865.bloomberto.api.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

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
public final class RenderTypes extends RenderType {
    private static final Map<ResourceLocation, RenderType> ADDITIVE = new ConcurrentHashMap<>();

    // Only extends RenderType to reach its protected render state constants; never instantiated.
    private RenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean crumbling,
                        boolean sort, Runnable setup, Runnable clear) {
        super(name, format, mode, bufferSize, crumbling, sort, setup, clear);
        throw new AssertionError("not instantiable");
    }

    /** Vertex format: position, uv, color ({@code POSITION_TEX_COLOR}); write vertices in any order through the builder. */
    public static RenderType additive(ResourceLocation texture) {
        return ADDITIVE.computeIfAbsent(texture, tex -> RenderType.create("bloomberto_additive",
                DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS, 256, false, true,
                RenderType.CompositeState.builder()
                        .setShaderState(new ShaderStateShard(net.minecraft.client.renderer.GameRenderer::getPositionTexColorShader))
                        .setTextureState(new TextureStateShard(tex, false, false))
                        .setTransparencyState(ADDITIVE_TRANSPARENCY)
                        .setWriteMaskState(COLOR_WRITE)
                        .setCullState(NO_CULL)
                        .createCompositeState(false)));
    }

    /** Translucent, fullbright. Vertex format: the entity format (position, color, uv, overlay, light, normal). */
    public static RenderType emissive(ResourceLocation texture) {
        return RenderType.entityTranslucentEmissive(texture);
    }

    /** Minecraft's glowing-eyes render type. */
    public static RenderType glow(ResourceLocation texture) {
        return RenderType.eyes(texture);
    }
}
