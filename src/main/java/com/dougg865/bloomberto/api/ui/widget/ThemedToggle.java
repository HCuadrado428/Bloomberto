package com.dougg865.bloomberto.api.ui.widget;

import com.dougg865.bloomberto.api.ui.UiTheme;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;

/** A button that switches between on and off and shows its state in the label. Client only. */
public class ThemedToggle extends ThemedButton {
    private boolean value;
    private final Component name;
    private final Consumer<Boolean> onChange;

    public ThemedToggle(int x, int y, int width, int height, Component name, boolean initial, UiTheme theme,
                        Consumer<Boolean> onChange) {
        super(x, y, width, height, Component.empty(), theme, () -> { });
        this.name = name;
        this.value = initial;
        this.onChange = onChange;
        refreshLabel();
    }

    public boolean value() {
        return value;
    }

    public void setValue(boolean value) {
        this.value = value;
        refreshLabel();
    }

    private void refreshLabel() {
        setMessage(Component.empty().append(name).append(": ").append(value ? "ON" : "OFF"));
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        value = !value;
        refreshLabel();
        onChange.accept(value);
    }
}
