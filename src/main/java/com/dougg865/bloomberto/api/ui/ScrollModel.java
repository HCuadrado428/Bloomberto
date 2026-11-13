package com.dougg865.bloomberto.api.ui;

/**
 * The state of a vertically scrolling area: how far it is scrolled, the scroll bar size and position, and dragging.
 * Pure math, used by {@code ScrollList}.
 */
public final class ScrollModel {
    private int viewportHeight;
    private int contentHeight;
    private double offset;
    private double dragGrabY = Double.NaN;

    public ScrollModel(int viewportHeight, int contentHeight) {
        resize(viewportHeight, contentHeight);
    }

    public void resize(int viewportHeight, int contentHeight) {
        this.viewportHeight = Math.max(0, viewportHeight);
        this.contentHeight = Math.max(0, contentHeight);
        this.offset = clamp(offset);
    }

    public int maxScroll() {
        return Math.max(0, contentHeight - viewportHeight);
    }

    private double clamp(double value) {
        return Math.max(0, Math.min(maxScroll(), value));
    }

    public double offset() {
        return offset;
    }

    public void scrollTo(double newOffset) {
        offset = clamp(newOffset);
    }

    /** Scrolls by {@code pixels} (positive = down) and returns whether the position changed. */
    public boolean scrollBy(double pixels) {
        double before = offset;
        offset = clamp(offset + pixels);
        return offset != before;
    }

    public boolean canScroll() {
        return maxScroll() > 0;
    }

    /** The scroll bar thumb height, at least {@code minThumb} and at most the viewport. */
    public int thumbHeight(int minThumb) {
        if (!canScroll() || contentHeight == 0) {
            return viewportHeight;
        }
        int thumb = Math.round((float) viewportHeight * viewportHeight / contentHeight);
        return Math.max(Math.min(minThumb, viewportHeight), Math.min(viewportHeight, thumb));
    }

    /** The distance of the thumb's top from the top of the track. */
    public int thumbOffset(int minThumb) {
        int travel = viewportHeight - thumbHeight(minThumb);
        if (!canScroll() || travel <= 0) {
            return 0;
        }
        return (int) Math.round(travel * (offset / maxScroll()));
    }

    /** Starts dragging the thumb; {@code localY} is the pointer position inside the track. */
    public void beginDrag(double localY, int minThumb) {
        dragGrabY = localY - thumbOffset(minThumb);
    }

    /** Moves the content so the thumb follows the pointer. */
    public void dragTo(double localY, int minThumb) {
        if (Double.isNaN(dragGrabY) || !canScroll()) {
            return;
        }
        int travel = viewportHeight - thumbHeight(minThumb);
        if (travel <= 0) {
            return;
        }
        double fraction = (localY - dragGrabY) / travel;
        offset = clamp(fraction * maxScroll());
    }

    public void endDrag() {
        dragGrabY = Double.NaN;
    }

    public boolean isDragging() {
        return !Double.isNaN(dragGrabY);
    }
}
