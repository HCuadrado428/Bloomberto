package com.dougg865.bloomberto.api.ui.widget;

import com.dougg865.bloomberto.api.ui.Rect;
import com.dougg865.bloomberto.api.ui.SliderModel;
import com.dougg865.bloomberto.api.ui.UiGraphics;
import com.dougg865.bloomberto.api.ui.UiTheme;
import java.util.Locale;
import java.util.function.DoubleConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/** A horizontal slider drawn with a {@link UiTheme}. Client only. */
public class ThemedSlider extends AbstractWidget {
    private static final int KNOB_WIDTH = 8;

    private final UiTheme theme;
    private final SliderModel model;
    private final Component name;
    private final DoubleConsumer onChange;

    public ThemedSlider(int x, int y, int width, int height, Component name, SliderModel model, UiTheme theme,
                        DoubleConsumer onChange) {
        super(x, y, width, height, name);
        this.theme = theme;
        this.model = model;
        this.name = name;
        this.onChange = onChange;
    }

    public SliderModel model() {
        return model;
    }

    private void update(double mouseX) {
        model.setFromPointer(mouseX - getX() - KNOB_WIDTH / 2.0, getWidth() - KNOB_WIDTH);
        onChange.accept(model.value());
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        UiGraphics.nineSlice(graphics, theme.sliderTrack(), theme.slice(), new Rect(getX(), getY(), getWidth(), getHeight()));
        int knobX = getX() + (int) Math.round(model.fraction() * (getWidth() - KNOB_WIDTH));
        UiGraphics.nineSlice(graphics, theme.sliderKnob(), theme.slice(), new Rect(knobX, getY(), KNOB_WIDTH, getHeight()));
        Component label = Component.empty().append(name).append(": ").append(String.format(Locale.ROOT, "%.2f", model.value()));
        graphics.drawCenteredString(Minecraft.getInstance().font, label, getX() + getWidth() / 2,
                getY() + (getHeight() - 8) / 2, active ? theme.textColor() : theme.disabledTextColor());
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        update(mouseX);
    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
        update(mouseX);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT) {
            model.nudge(keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT ? -1 : 1);
            onChange.accept(model.value());
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
