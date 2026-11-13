package com.dougg865.bloomberto.testmod;

import com.dougg865.bloomberto.api.ui.FlexLayout;
import com.dougg865.bloomberto.api.ui.Rect;
import com.dougg865.bloomberto.api.ui.SliderModel;
import com.dougg865.bloomberto.api.ui.Transition;
import com.dougg865.bloomberto.api.ui.UiGraphics;
import com.dougg865.bloomberto.api.ui.UiTheme;
import com.dougg865.bloomberto.api.ui.widget.ScrollList;
import com.dougg865.bloomberto.api.ui.widget.ThemedButton;
import com.dougg865.bloomberto.api.ui.widget.ThemedSlider;
import com.dougg865.bloomberto.api.ui.widget.ThemedToggle;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Client-only example of the ui module: a flex layout of themed widgets and a scroll list, faded in with a Transition. */
public final class TestScreen extends Screen {
    private final Transition appear = Transition.create().fade().slide(0, 16).scale(0.95f).duration(8);
    private Rect panel = new Rect(0, 0, 0, 0);

    public TestScreen() {
        super(Component.literal("Bloomberto UI example"));
    }

    @Override
    protected void init() {
        panel = new Rect(width / 2 - 110, height / 2 - 80, 220, 160);
        List<Rect> row = FlexLayout.row().gap(6).padding(8).alignItems(FlexLayout.Align.CENTER)
                .add(FlexLayout.Item.grow(1, 0, 20))
                .add(FlexLayout.Item.grow(1, 0, 20))
                .compute(new Rect(panel.x(), panel.y() + 8, panel.width(), 36));
        addRenderableWidget(new ThemedToggle(row.get(0).x(), row.get(0).y(), row.get(0).width(), row.get(0).height(),
                Component.literal("Glow"), true, UiTheme.DEFAULT, on -> { }));
        addRenderableWidget(new ThemedSlider(row.get(1).x(), row.get(1).y(), row.get(1).width(), row.get(1).height(),
                Component.literal("Size"), new SliderModel(0, 10, 0.5, 5), UiTheme.DEFAULT, v -> { }));

        ScrollList list = new ScrollList(panel.x() + 8, panel.y() + 52, panel.width() - 16, 70);
        for (int i = 1; i <= 12; i++) {
            int n = i;
            list.add(new ScrollList.Row() {
                @Override
                public int height() {
                    return 14;
                }

                @Override
                public void render(GuiGraphics graphics, Rect area, int mouseX, int mouseY, boolean hovered) {
                    graphics.drawString(font, "Row " + n, area.x() + 4, area.y() + 3, hovered ? 0xFFFFAA : 0xFFFFFF);
                }
            });
        }
        addRenderableWidget(list);
        addRenderableWidget(new ThemedButton(panel.x() + panel.width() / 2 - 40, panel.bottom() - 28, 80, 20,
                Component.literal("Close"), this::onClose));
        appear.show();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        //? if <1.21 {
        /*renderBackground(graphics);
        *///?}
        UiGraphics.transition(graphics, appear, panel, () -> {
            UiGraphics.nineSlice(graphics, UiTheme.DEFAULT.panel(), UiTheme.DEFAULT.slice(), panel);
            super.render(graphics, mouseX, mouseY, partialTick);
        });
    }
}
