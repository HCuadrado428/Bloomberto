package com.dougg865.bloomberto.api.ui.widget;

import com.dougg865.bloomberto.api.ui.Rect;
import com.dougg865.bloomberto.api.ui.UiGraphics;
import com.dougg865.bloomberto.api.ui.UiTheme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** A push button drawn with a {@link UiTheme}. Client only. */
public class ThemedButton extends AbstractWidget {
    private final UiTheme theme;
    private final Runnable onPress;

    public ThemedButton(int x, int y, int width, int height, Component label, UiTheme theme, Runnable onPress) {
        super(x, y, width, height, label);
        this.theme = theme;
        this.onPress = onPress;
    }

    public ThemedButton(int x, int y, int width, int height, Component label, Runnable onPress) {
        this(x, y, width, height, label, UiTheme.DEFAULT, onPress);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        var texture = !active ? theme.buttonDisabled() : isHoveredOrFocused() ? theme.buttonHover() : theme.button();
        UiGraphics.nineSlice(graphics, texture, theme.slice(), new Rect(getX(), getY(), getWidth(), getHeight()));
        Font font = Minecraft.getInstance().font;
        int color = active ? theme.textColor() : theme.disabledTextColor();
        graphics.drawCenteredString(font, getMessage(), getX() + getWidth() / 2, getY() + (getHeight() - 8) / 2, color);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        onPress.run();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
