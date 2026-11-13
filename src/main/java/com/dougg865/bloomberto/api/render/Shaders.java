package com.dougg865.bloomberto.api.render;

import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

/** Creates {@link ShaderProgram}s. Client only. */
public final class Shaders {
    private static final List<ShaderProgram> PROGRAMS = new ArrayList<>();
    private static boolean listening;

    private Shaders() {
    }

    /**
     * Declares a core shader. Nothing is loaded until the program is first used on the render thread, so this
     * is safe to call while the mod initializes.
     *
     * @param id     the shader name; files live at {@code assets/<namespace>/shaders/core/<path>.json}
     * @param format the vertex format your shader's vertex stage expects
     */
    public static synchronized ShaderProgram create(ResourceLocation id, VertexFormat format) {
        ShaderProgram program = new ShaderProgram(id, format);
        PROGRAMS.add(program);
        return program;
    }

    /** Hooks into vanilla's reload so programs are rebuilt after F3+T; called when the client starts. */
    public static synchronized void listenForReloads() {
        if (listening) {
            return;
        }
        listening = true;
        ((ReloadableResourceManager) Minecraft.getInstance().getResourceManager()).registerReloadListener(
                (ResourceManagerReloadListener) manager -> {
                    synchronized (Shaders.class) {
                        PROGRAMS.forEach(ShaderProgram::invalidate);
                        com.dougg865.bloomberto.api.render.post.PostEffects.invalidate();
                    }
                });
    }
}
