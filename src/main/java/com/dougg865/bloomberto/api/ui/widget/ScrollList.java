package com.dougg865.bloomberto.api.ui.widget;

import com.dougg865.bloomberto.api.ui.Rect;
import com.dougg865.bloomberto.api.ui.ScrollModel;
import com.dougg865.bloomberto.api.ui.UiGraphics;
import com.dougg865.bloomberto.api.ui.UiTheme;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * A vertically scrolling list of rows with a scroll bar, mouse wheel and thumb dragging. Rows are clipped to the list's
 * area. Client only.
 */
public class ScrollList extends AbstractWidget {
    /** One row of the list. */
    public interface Row {
        int height();

        /** Draws the row inside {@code area} (already clipped to the list). */
        void render(GuiGraphics graphics, Rect area, int mouseX, int mouseY, boolean hovered);

        /** Called when the row is clicked; return {@code true} if it was handled. */
        default boolean onClick(double mouseX, double mouseY) {
            return false;
        }
    }

    private static final int BAR_WIDTH = 6;
    private static final int MIN_THUMB = 10;

    private final UiTheme theme;
    private final List<Row> rows = new ArrayList<>();
    private final ScrollModel scroll;

    public ScrollList(int x, int y, int width, int height, UiTheme theme) {
        super(x, y, width, height, Component.empty());
        this.theme = theme;
        this.scroll = new ScrollModel(height, 0);
    }

    public ScrollList(int x, int y, int width, int height) {
        this(x, y, width, height, UiTheme.DEFAULT);
    }

    public void add(Row row) {
        rows.add(row);
        refresh();
    }

    public void clear() {
        rows.clear();
        refresh();
    }

    private void refresh() {
        int total = 0;
        for (Row row : rows) {
            total += row.height();
        }
        scroll.resize(getHeight(), total);
    }

    private Rect area() {
        return new Rect(getX(), getY(), getWidth() - (scroll.canScroll() ? BAR_WIDTH + 2 : 0), getHeight());
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Rect box = new Rect(getX(), getY(), getWidth(), getHeight());
        UiGraphics.nineSlice(graphics, theme.panel(), theme.slice(), box);
        Rect inner = area().inset(1);
        UiGraphics.scissor(graphics, inner, () -> {
            int y = inner.y() - (int) Math.round(scroll.offset());
            for (Row row : rows) {
                Rect rowArea = new Rect(inner.x(), y, inner.width(), row.height());
                if (rowArea.bottom() > inner.y() && rowArea.y() < inner.bottom()) {
                    row.render(graphics, rowArea, mouseX, mouseY, rowArea.contains(mouseX, mouseY) && inner.contains(mouseX, mouseY));
                }
                y += row.height();
            }
        });
        if (scroll.canScroll()) {
            int barX = getX() + getWidth() - BAR_WIDTH - 1;
            UiGraphics.nineSlice(graphics, theme.sliderTrack(), theme.slice(), new Rect(barX, getY() + 1, BAR_WIDTH, getHeight() - 2));
            UiGraphics.nineSlice(graphics, theme.sliderKnob(), theme.slice(),
                    new Rect(barX, getY() + 1 + scroll.thumbOffset(MIN_THUMB), BAR_WIDTH, scroll.thumbHeight(MIN_THUMB)));
        }
    }

    private boolean onBar(double mouseX) {
        return scroll.canScroll() && mouseX >= getX() + getWidth() - BAR_WIDTH - 1 && mouseX < getX() + getWidth();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isMouseOver(mouseX, mouseY)) {
            return false;
        }
        if (onBar(mouseX)) {
            scroll.beginDrag(mouseY - getY(), MIN_THUMB);
            return true;
        }
        Rect inner = area().inset(1);
        int y = inner.y() - (int) Math.round(scroll.offset());
        for (Row row : rows) {
            if (mouseY >= y && mouseY < y + row.height() && inner.contains(mouseX, mouseY)) {
                return row.onClick(mouseX, mouseY - y);
            }
            y += row.height();
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (scroll.isDragging()) {
            scroll.dragTo(mouseY - getY(), MIN_THUMB);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean wasDragging = scroll.isDragging();
        scroll.endDrag();
        return wasDragging;
    }

    //? if >=1.21 {
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return isMouseOver(mouseX, mouseY) && scroll.scrollBy(-scrollY * 12);
    }
    //?} else {
    /*@Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return isMouseOver(mouseX, mouseY) && scroll.scrollBy(-delta * 12);
    }
    *///?}

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
