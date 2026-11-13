package com.dougg865.bloomberto.api.render;

import com.dougg865.bloomberto.api.client.WorldRenderContext;
import com.dougg865.bloomberto.api.color.Color;
import com.dougg865.bloomberto.impl.render.Vtx;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Draws boxes, lines, quads, billboards and 3D text in the world. Create one inside a
 * {@code ClientEvents.WORLD_RENDER} listener; every position is a normal world position.
 *
 * <pre>{@code
 * ClientEvents.WORLD_RENDER.register(ctx -> {
 *     WorldDraw draw = WorldDraw.of(ctx);
 *     draw.box(new AABB(pos), Color.fromHex("#ff0000"));
 *     draw.text(Component.literal("Here"), pos.getCenter().add(0, 1, 0), 0.03f, Color.WHITE);
 * });
 * }</pre>
 *
 * <p>By default everything is hidden behind blocks; {@link #throughWalls(boolean)} draws over everything.
 * All drawing happens on the render thread.
 */
public final class WorldDraw {
    private final WorldRenderContext ctx;
    private boolean throughWalls;

    private WorldDraw(WorldRenderContext ctx) {
        this.ctx = ctx;
    }

    public static WorldDraw of(WorldRenderContext context) {
        return new WorldDraw(context);
    }

    /** When {@code true}, what is drawn next ignores the depth buffer and shows through blocks. */
    public WorldDraw throughWalls(boolean throughWalls) {
        this.throughWalls = throughWalls;
        return this;
    }

    private void draw(RenderType type, java.util.function.Consumer<VertexConsumer> body) {
        if (throughWalls) {
            RenderSystem.disableDepthTest();
        }
        body.accept(ctx.buffers().getBuffer(type));
        ctx.buffers().endBatch(type);
        if (throughWalls) {
            RenderSystem.enableDepthTest();
        }
    }

    /** A line between two world positions. */
    public WorldDraw line(Vec3 from, Vec3 to, Color color) {
        Vec3 a = ctx.relative(from);
        Vec3 b = ctx.relative(to);
        Vec3 d = b.subtract(a);
        float len = (float) d.length();
        float nx = len == 0 ? 0 : (float) d.x / len;
        float ny = len == 0 ? 1 : (float) d.y / len;
        float nz = len == 0 ? 0 : (float) d.z / len;
        draw(RenderType.lines(), vc -> {
            PoseStack.Pose pose = ctx.poseStack().last();
            Vtx.posColorNormal(vc, pose, (float) a.x, (float) a.y, (float) a.z, color.argb(), nx, ny, nz);
            Vtx.posColorNormal(vc, pose, (float) b.x, (float) b.y, (float) b.z, color.argb(), nx, ny, nz);
        });
        return this;
    }

    /** The 12 edges of a box. */
    public WorldDraw box(AABB box, Color color) {
        double x0 = box.minX, y0 = box.minY, z0 = box.minZ, x1 = box.maxX, y1 = box.maxY, z1 = box.maxZ;
        Vec3[] c = {
            new Vec3(x0, y0, z0), new Vec3(x1, y0, z0), new Vec3(x1, y0, z1), new Vec3(x0, y0, z1),
            new Vec3(x0, y1, z0), new Vec3(x1, y1, z0), new Vec3(x1, y1, z1), new Vec3(x0, y1, z1)
        };
        int[][] edges = {{0, 1}, {1, 2}, {2, 3}, {3, 0}, {4, 5}, {5, 6}, {6, 7}, {7, 4}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};
        for (int[] e : edges) {
            line(c[e[0]], c[e[1]], color);
        }
        return this;
    }

    /** A solid (translucent if the color has alpha) quad; corners in counter-clockwise order as seen from the front. */
    public WorldDraw quad(Vec3 p1, Vec3 p2, Vec3 p3, Vec3 p4, Color color) {
        Vec3[] p = {ctx.relative(p1), ctx.relative(p2), ctx.relative(p3), ctx.relative(p4)};
        draw(RenderType.debugQuads(), vc -> {
            PoseStack.Pose pose = ctx.poseStack().last();
            for (Vec3 v : p) {
                Vtx.posColor(vc, pose, (float) v.x, (float) v.y, (float) v.z, color.argb());
            }
            // Draw the back side too, so the quad is visible from both sides.
            for (int i = 3; i >= 0; i--) {
                Vtx.posColor(vc, pose, (float) p[i].x, (float) p[i].y, (float) p[i].z, color.argb());
            }
        });
        return this;
    }

    /** The six faces of a box, filled. */
    public WorldDraw filledBox(AABB b, Color color) {
        Vec3 a = new Vec3(b.minX, b.minY, b.minZ), bb = new Vec3(b.maxX, b.minY, b.minZ);
        Vec3 c = new Vec3(b.maxX, b.minY, b.maxZ), d = new Vec3(b.minX, b.minY, b.maxZ);
        Vec3 e = new Vec3(b.minX, b.maxY, b.minZ), f = new Vec3(b.maxX, b.maxY, b.minZ);
        Vec3 g = new Vec3(b.maxX, b.maxY, b.maxZ), h = new Vec3(b.minX, b.maxY, b.maxZ);
        quad(a, bb, c, d, color);
        quad(e, f, g, h, color);
        quad(a, bb, f, e, color);
        quad(d, c, g, h, color);
        quad(a, d, h, e, color);
        quad(bb, c, g, f, color);
        return this;
    }

    /**
     * A textured quad centered on {@code center}. {@code right} and {@code up} are the half-extent vectors of the quad
     * (so the quad is {@code 2*|right|} wide and {@code 2*|up|} tall).
     */
    public WorldDraw texturedQuad(ResourceLocation texture, Vec3 center, Vec3 right, Vec3 up, Color tint) {
        Vec3 c = ctx.relative(center);
        Vec3 bl = c.subtract(right).subtract(up);
        Vec3 br = c.add(right).subtract(up);
        Vec3 tr = c.add(right).add(up);
        Vec3 tl = c.subtract(right).add(up);
        draw(RenderType.text(texture), vc -> {
            PoseStack.Pose pose = ctx.poseStack().last();
            int light = LightTexture.FULL_BRIGHT;
            Vtx.posColorUvLight(vc, pose, (float) bl.x, (float) bl.y, (float) bl.z, tint.argb(), 0f, 1f, light);
            Vtx.posColorUvLight(vc, pose, (float) br.x, (float) br.y, (float) br.z, tint.argb(), 1f, 1f, light);
            Vtx.posColorUvLight(vc, pose, (float) tr.x, (float) tr.y, (float) tr.z, tint.argb(), 1f, 0f, light);
            Vtx.posColorUvLight(vc, pose, (float) tl.x, (float) tl.y, (float) tl.z, tint.argb(), 0f, 0f, light);
        });
        return this;
    }

    /** A textured square of {@code size} blocks that always faces the camera. */
    public WorldDraw billboard(ResourceLocation texture, Vec3 center, float size, Color tint) {
        Vector3f left = ctx.camera().getLeftVector();
        Vector3f up = ctx.camera().getUpVector();
        float half = size / 2f;
        Vec3 right = new Vec3(-left.x() * half, -left.y() * half, -left.z() * half);
        Vec3 upV = new Vec3(up.x() * half, up.y() * half, up.z() * half);
        return texturedQuad(texture, center, right, upV, tint);
    }

    /**
     * Text that faces the camera, centered on {@code pos}.
     *
     * @param scale size of one font pixel in blocks; {@code 0.025f} gives name-tag sized text
     */
    public WorldDraw text(Component text, Vec3 pos, float scale, Color color) {
        Vec3 rel = ctx.relative(pos);
        Font font = Minecraft.getInstance().font;
        PoseStack stack = ctx.poseStack();
        stack.pushPose();
        stack.translate(rel.x, rel.y, rel.z);
        stack.mulPose(ctx.camera().rotation());
        //? if >=1.21 {
        // On 1.21 the camera's rotation leaves the text facing away from the viewer (seen in-game), so turn it around.
        stack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180f));
        //?}
        stack.scale(-scale, -scale, scale);
        float x = -font.width(text) / 2f;
        if (throughWalls) {
            RenderSystem.disableDepthTest();
        }
        font.drawInBatch(text, x, 0f, color.argb(), false, stack.last().pose(), ctx.buffers(),
                throughWalls ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
        ctx.buffers().endBatch();
        if (throughWalls) {
            RenderSystem.enableDepthTest();
        }
        stack.popPose();
        return this;
    }

    /**
     * Draws a {@link com.dougg865.bloomberto.api.effects.Trail}: a ribbon that fades from {@code head} to {@code tail} color
     * (including alpha) and narrows towards its end.
     */
    public WorldDraw trail(com.dougg865.bloomberto.api.effects.Trail trail, float width, Color head, Color tail) {
        for (var segment : trail.segments(ctx.cameraPos(), width)) {
            float mid = (segment.ageStart() + segment.ageEnd()) / 2f;
            quad(segment.a1(), segment.a2(), segment.b2(), segment.b1(), head.lerp(tail, mid));
        }
        return this;
    }
}
