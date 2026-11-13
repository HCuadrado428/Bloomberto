package com.dougg865.bloomberto.api.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Drawing helpers for 9-slices, clipping and transitions. Client only. */
public final class UiGraphics {
    private UiGraphics() {
    }

    /** Draws {@code texture} stretched over {@code target} with its corners intact. */
    public static void nineSlice(GuiGraphics graphics, ResourceLocation texture, NineSlice slice, Rect target) {
        for (NineSlice.Patch p : slice.patches(target)) {
            graphics.blit(texture, p.dstX(), p.dstY(), p.dstW(), p.dstH(),
                    (float) p.srcX(), (float) p.srcY(), p.srcW(), p.srcH(), slice.textureWidth(), slice.textureHeight());
        }
    }

    /** Runs {@code drawing} with everything outside {@code area} clipped away. */
    public static void scissor(GuiGraphics graphics, Rect area, Runnable drawing) {
        graphics.enableScissor(area.x(), area.y(), area.right(), area.bottom());
        try {
            drawing.run();
        } finally {
            graphics.disableScissor();
        }
    }

    /**
     * Draws with a {@link Transition} applied: the slide and scale move and resize what {@code drawing} draws (scaling is
     * around {@code pivot}'s center) and the alpha fades textures. Does nothing while the transition is hidden.
     */
    public static void transition(GuiGraphics graphics, Transition transition, Rect pivot, Runnable drawing) {
        if (!transition.isVisible()) {
            return;
        }
        var pose = graphics.pose();
        pose.pushPose();
        float cx = pivot.x() + pivot.width() / 2f;
        float cy = pivot.y() + pivot.height() / 2f;
        pose.translate(transition.offsetX() + cx, transition.offsetY() + cy, 0f);
        pose.scale(transition.scale(), transition.scale(), 1f);
        pose.translate(-cx, -cy, 0f);
        RenderSystem.enableBlend();
        graphics.setColor(1f, 1f, 1f, transition.alpha());
        try {
            drawing.run();
        } finally {
            graphics.setColor(1f, 1f, 1f, 1f);
            pose.popPose();
        }
    }
}
