package com.dougg865.bloomberto.api.ui;

//? if <1.21.11 {
import com.mojang.blaze3d.systems.RenderSystem;
//?}
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Drawing helpers for 9-slices, clipping and transitions. Client only. */
public final class UiGraphics {
    //? if >=1.21.11 {
    /*// 1.21.11 has no global shader color: transitions pass their alpha to the textures drawn here
    private static float alpha = 1f;
    *///?}

    private UiGraphics() {
    }

    /** Draws {@code texture} stretched over {@code target} with its corners intact. */
    public static void nineSlice(GuiGraphics graphics, ResourceLocation texture, NineSlice slice, Rect target) {
        for (NineSlice.Patch p : slice.patches(target)) {
            //? if >=1.21.11 {
            /*graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, texture, p.dstX(), p.dstY(),
                    (float) p.srcX(), (float) p.srcY(), p.dstW(), p.dstH(), p.srcW(), p.srcH(), slice.textureWidth(),
                    slice.textureHeight(), net.minecraft.util.ARGB.white(alpha));
            *///?} else {
            graphics.blit(texture, p.dstX(), p.dstY(), p.dstW(), p.dstH(),
                    (float) p.srcX(), (float) p.srcY(), p.srcW(), p.srcH(), slice.textureWidth(), slice.textureHeight());
            //?}
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
        float cx = pivot.x() + pivot.width() / 2f;
        float cy = pivot.y() + pivot.height() / 2f;
        //? if >=1.21.11 {
        /*pose.pushMatrix();
        pose.translate(transition.offsetX() + cx, transition.offsetY() + cy);
        pose.scale(transition.scale(), transition.scale());
        pose.translate(-cx, -cy);
        float previous = alpha;
        alpha = previous * transition.alpha();
        try {
            drawing.run();
        } finally {
            alpha = previous;
            pose.popMatrix();
        }
        *///?} else {
        pose.pushPose();
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
        //?}
    }
}
