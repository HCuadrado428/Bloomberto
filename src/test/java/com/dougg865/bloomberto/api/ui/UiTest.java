package com.dougg865.bloomberto.api.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dougg865.bloomberto.api.animation.Easing;
import com.dougg865.bloomberto.api.animation.TweenManager;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class UiTest {
    // ---------- Rect ----------

    @Test
    void rectBasics() {
        Rect r = new Rect(10, 20, 30, 40);
        assertEquals(40, r.right());
        assertEquals(60, r.bottom());
        assertTrue(r.contains(10, 20));
        assertFalse(r.contains(40, 20), "right edge is exclusive");
        assertEquals(new Rect(12, 22, 26, 36), r.inset(2));
        assertEquals(new Rect(20, 30, 0, 0).width(), r.inset(100).width());
        assertEquals(new Rect(15, 25, 20, 10), r.intersect(new Rect(15, 25, 20, 10)));
        assertEquals(0, r.intersect(new Rect(100, 100, 5, 5)).width());
    }

    // ---------- FlexLayout ----------

    @Test
    void rowWithFixedItemsAndGap() {
        List<Rect> cells = FlexLayout.row().gap(5)
                .add(FlexLayout.Item.fixed(20, 10)).add(FlexLayout.Item.fixed(30, 10))
                .compute(new Rect(0, 0, 100, 10));
        assertEquals(new Rect(0, 0, 20, 10), cells.get(0));
        assertEquals(new Rect(25, 0, 30, 10), cells.get(1));
    }

    @Test
    void growSharesFreeSpaceByWeight() {
        List<Rect> cells = FlexLayout.row().gap(0)
                .add(FlexLayout.Item.fixed(40, 10))
                .add(FlexLayout.Item.grow(1, 0, 10))
                .add(FlexLayout.Item.grow(3, 0, 10))
                .compute(new Rect(0, 0, 200, 10));
        assertEquals(40, cells.get(0).width());
        assertEquals(40, cells.get(1).width());
        assertEquals(120, cells.get(2).width());
        assertEquals(200, cells.get(2).right(), "the row is filled exactly");
    }

    @Test
    void growRespectsMaximumsAndPassesTheRestOn() {
        List<Rect> cells = FlexLayout.row()
                .add(FlexLayout.Item.grow(1, 0, 10).max(20))
                .add(FlexLayout.Item.grow(1, 0, 10))
                .compute(new Rect(0, 0, 100, 10));
        assertEquals(20, cells.get(0).width());
        assertEquals(80, cells.get(1).width());
    }

    @Test
    void roundingLeavesNoGap() {
        List<Rect> cells = FlexLayout.row()
                .add(FlexLayout.Item.grow(1, 0, 10)).add(FlexLayout.Item.grow(1, 0, 10)).add(FlexLayout.Item.grow(1, 0, 10))
                .compute(new Rect(0, 0, 100, 10));
        int total = cells.stream().mapToInt(Rect::width).sum();
        assertEquals(100, total);
        assertEquals(100, cells.get(2).right());
    }

    @Test
    void justifyCenterEndAndSpaceBetween() {
        FlexLayout base = FlexLayout.row().add(FlexLayout.Item.fixed(20, 5)).add(FlexLayout.Item.fixed(20, 5));
        Rect box = new Rect(0, 0, 100, 5);
        assertEquals(30, base.justify(FlexLayout.Justify.CENTER).compute(box).get(0).x());
        assertEquals(60, base.justify(FlexLayout.Justify.END).compute(box).get(0).x());
        List<Rect> between = base.justify(FlexLayout.Justify.SPACE_BETWEEN).compute(box);
        assertEquals(0, between.get(0).x());
        assertEquals(80, between.get(1).x());
    }

    @Test
    void crossAxisAlignment() {
        Rect box = new Rect(0, 0, 100, 40);
        assertEquals(15, FlexLayout.row().alignItems(FlexLayout.Align.CENTER).add(FlexLayout.Item.fixed(10, 10)).compute(box).get(0).y());
        assertEquals(30, FlexLayout.row().alignItems(FlexLayout.Align.END).add(FlexLayout.Item.fixed(10, 10)).compute(box).get(0).y());
        Rect stretched = FlexLayout.row().alignItems(FlexLayout.Align.STRETCH).add(FlexLayout.Item.fixed(10, 0)).compute(box).get(0);
        assertEquals(40, stretched.height());
    }

    @Test
    void columnsAndPadding() {
        List<Rect> cells = FlexLayout.column().padding(4).gap(2)
                .add(FlexLayout.Item.fixed(10, 20)).add(FlexLayout.Item.fixed(10, 20))
                .compute(new Rect(0, 0, 50, 100));
        assertEquals(new Rect(4, 4, 20, 10), cells.get(0));
        assertEquals(new Rect(4, 16, 20, 10), cells.get(1));
    }

    @Test
    void shrinksWhenItemsDoNotFitButKeepsMinimums() {
        List<Rect> cells = FlexLayout.row()
                .add(FlexLayout.Item.fixed(80, 10).min(60)).add(FlexLayout.Item.fixed(80, 10))
                .compute(new Rect(0, 0, 100, 10));
        assertTrue(cells.get(0).width() >= 60);
        assertEquals(100, cells.get(0).width() + cells.get(1).width());
        List<Rect> tight = FlexLayout.row()
                .add(FlexLayout.Item.fixed(80, 10).min(80)).add(FlexLayout.Item.fixed(80, 10).min(80))
                .compute(new Rect(0, 0, 100, 10));
        assertEquals(80, tight.get(0).width(), "minimums win over fitting");
    }

    @Test
    void emptyLayoutIsEmpty() {
        assertTrue(FlexLayout.row().compute(new Rect(0, 0, 10, 10)).isEmpty());
    }

    // ---------- NineSlice ----------

    @Test
    void nineSliceKeepsCornersAndStretchesTheRest() {
        NineSlice slice = new NineSlice(16, 16, 3);
        List<NineSlice.Patch> patches = slice.patches(new Rect(100, 50, 40, 30));
        assertEquals(9, patches.size());
        NineSlice.Patch topLeft = patches.get(0);
        assertEquals(new NineSlice.Patch(0, 0, 3, 3, 100, 50, 3, 3), topLeft);
        NineSlice.Patch center = patches.get(4);
        assertEquals(new NineSlice.Patch(3, 3, 10, 10, 103, 53, 34, 24), center);
        NineSlice.Patch bottomRight = patches.get(8);
        assertEquals(new NineSlice.Patch(13, 13, 3, 3, 137, 77, 3, 3), bottomRight);
        // the patches tile the target exactly
        long area = patches.stream().mapToLong(p -> (long) p.dstW() * p.dstH()).sum();
        assertEquals(40L * 30L, area);
    }

    @Test
    void nineSliceFitsTargetsSmallerThanItsBorders() {
        NineSlice slice = new NineSlice(16, 16, 3);
        List<NineSlice.Patch> patches = slice.patches(new Rect(0, 0, 4, 10));
        long area = patches.stream().mapToLong(p -> (long) p.dstW() * p.dstH()).sum();
        assertEquals(40L, area, "no overlap and no gap even when squeezed");
        for (NineSlice.Patch p : patches) {
            assertTrue(p.dstX() >= 0 && p.dstX() + p.dstW() <= 4);
        }
        assertTrue(slice.patches(new Rect(0, 0, 0, 5)).isEmpty());
    }

    @Test
    void nineSliceRejectsBordersWithoutACenter() {
        assertThrows(IllegalArgumentException.class, () -> new NineSlice(16, 16, 8));
        assertThrows(IllegalArgumentException.class, () -> new NineSlice(16, 16, -1, 0, 0, 0));
    }

    // ---------- ScrollModel ----------

    @Test
    void scrollClampsAndReportsChanges() {
        ScrollModel s = new ScrollModel(100, 300);
        assertEquals(200, s.maxScroll());
        assertTrue(s.scrollBy(50));
        assertEquals(50, s.offset());
        assertTrue(s.scrollBy(1000));
        assertEquals(200, s.offset());
        assertFalse(s.scrollBy(10), "already at the bottom");
        s.scrollBy(-1000);
        assertEquals(0, s.offset());
    }

    @Test
    void shortContentDoesNotScroll() {
        ScrollModel s = new ScrollModel(100, 50);
        assertFalse(s.canScroll());
        assertFalse(s.scrollBy(10));
        assertEquals(100, s.thumbHeight(10));
    }

    @Test
    void resizeKeepsTheOffsetInRange() {
        ScrollModel s = new ScrollModel(100, 300);
        s.scrollTo(200);
        s.resize(100, 150);
        assertEquals(50, s.offset());
    }

    @Test
    void thumbSizeAndPosition() {
        ScrollModel s = new ScrollModel(100, 400);
        assertEquals(25, s.thumbHeight(10));
        assertEquals(0, s.thumbOffset(10));
        s.scrollTo(300);
        assertEquals(75, s.thumbOffset(10), "at the bottom the thumb touches the end of the track");
        assertEquals(10, new ScrollModel(100, 100000).thumbHeight(10), "never smaller than the minimum");
    }

    @Test
    void draggingTheThumbScrollsTheContent() {
        ScrollModel s = new ScrollModel(100, 400);
        s.beginDrag(10, 10);
        assertTrue(s.isDragging());
        s.dragTo(10 + 75, 10); // move the thumb to the bottom of the 75px of travel
        assertEquals(300, s.offset(), 1e-6);
        s.dragTo(-500, 10);
        assertEquals(0, s.offset());
        s.endDrag();
        assertFalse(s.isDragging());
        s.dragTo(50, 10);
        assertEquals(0, s.offset(), "no drag in progress, nothing moves");
    }

    // ---------- SliderModel ----------

    @Test
    void sliderMapsPointerToValue() {
        SliderModel m = new SliderModel(0, 10, 0, 5);
        assertEquals(0.5, m.fraction(), 1e-9);
        m.setFromPointer(25, 100);
        assertEquals(2.5, m.value(), 1e-9);
        m.setFromPointer(-20, 100);
        assertEquals(0, m.value());
        m.setFromPointer(500, 100);
        assertEquals(10, m.value());
        m.setFromPointer(10, 0);
        assertEquals(0, m.value(), "a zero-width track does not divide by zero");
    }

    @Test
    void sliderSteps() {
        SliderModel m = new SliderModel(0, 10, 2, 3.4);
        assertEquals(4, m.value());
        m.setFromPointer(51, 100);
        assertEquals(6, m.value());
        m.nudge(-2);
        assertEquals(2, m.value());
        m.nudge(100);
        assertEquals(10, m.value());
        assertThrows(IllegalArgumentException.class, () -> new SliderModel(5, 5, 0, 5));
    }

    // ---------- Transition ----------

    @Test
    void transitionShowsAndHides() {
        TweenManager manager = new TweenManager();
        Transition t = Transition.create(manager).fade().slide(0, 20).scale(0.5f).duration(4).ease(Easing.LINEAR);
        assertFalse(t.isVisible());
        assertEquals(0f, t.alpha());
        assertEquals(20f, t.offsetY());
        assertEquals(0.5f, t.scale());
        t.show();
        assertTrue(t.isAnimating());
        manager.tick();
        manager.tick();
        assertEquals(0.5f, t.progress(), 1e-6f);
        assertEquals(0.5f, t.alpha(), 1e-6f);
        assertEquals(10f, t.offsetY(), 1e-6f);
        assertEquals(0.75f, t.scale(), 1e-6f);
        manager.tick();
        manager.tick();
        assertEquals(1f, t.progress(), 1e-6f);
        assertEquals(0f, t.offsetY(), 1e-6f);
        assertEquals(1f, t.scale(), 1e-6f);
        assertFalse(t.isAnimating());
        t.hide();
        for (int i = 0; i < 4; i++) {
            manager.tick();
        }
        assertFalse(t.isVisible());
    }

    @Test
    void reversingHalfwayTakesLessTime() {
        TweenManager manager = new TweenManager();
        Transition t = Transition.create(manager).duration(10).ease(Easing.LINEAR);
        t.show();
        for (int i = 0; i < 5; i++) {
            manager.tick();
        }
        assertEquals(0.5f, t.progress(), 1e-6f);
        t.hide();
        for (int i = 0; i < 5; i++) {
            manager.tick();
        }
        assertEquals(0f, t.progress(), 1e-6f, "back to hidden in about half the duration");
    }

    @Test
    void withoutFadeTheAlphaStaysOpaque() {
        Transition t = Transition.create(new TweenManager());
        assertEquals(1f, t.alpha());
        t.showNow();
        assertEquals(1f, t.progress());
    }

    // ---------- default theme assets ----------

    @Test
    void defaultThemeTexturesAreValidNineSliceReadyPngs() throws IOException {
        for (String name : List.of("button", "button_hover", "button_disabled", "panel", "slider_track", "slider_knob")) {
            try (InputStream in = UiTest.class.getResourceAsStream("/assets/bloomberto/textures/gui/" + name + ".png")) {
                assertNotNull(in, "missing texture " + name);
                BufferedImage image = ImageIO.read(in);
                assertEquals(16, image.getWidth());
                assertEquals(16, image.getHeight());
                assertEquals(0, image.getRGB(0, 0) >>> 24, name + " has a transparent corner");
                assertEquals(255, image.getRGB(8, 8) >>> 24, name + " has an opaque center");
            }
        }
    }
}
