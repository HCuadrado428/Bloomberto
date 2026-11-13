package com.dougg865.bloomberto.api.client;

import com.dougg865.bloomberto.api.config.Config;
import com.dougg865.bloomberto.api.config.ConfigValue;
import com.dougg865.bloomberto.api.config.ValueType;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * A settings screen generated from a {@link Config}: a toggle for booleans, a slider for ranged numbers, a cycle button for
 * enums and a text box for strings and string lists. Changes are written to disk when the screen is closed.
 * Client only.
 *
 * <pre>{@code
 * Minecraft.getInstance().setScreen(ConfigScreen.create(currentScreen, config));
 * }</pre>
 *
 * Options marked {@code synced()} show a note when the server overrides them.
 */
public final class ConfigScreen extends Screen {
    private static final int ROW_HEIGHT = 24;
    private static final int WIDGET_WIDTH = 150;

    private final Screen parent;
    private final Config config;
    private final List<ConfigValue<?>> values;
    private int page;
    private int perPage = 6;
    private Button prev;
    private Button next;

    private ConfigScreen(Screen parent, Config config) {
        super(Component.literal(config.modId() + " settings"));
        this.parent = parent;
        this.config = config;
        this.values = new ArrayList<>(config.values());
    }

    public static ConfigScreen create(Screen parent, Config config) {
        return new ConfigScreen(parent, config);
    }

    @Override
    protected void init() {
        perPage = Math.max(1, (height - 90) / ROW_HEIGHT);
        int pages = Math.max(1, (values.size() + perPage - 1) / perPage);
        page = Math.min(page, pages - 1);
        int left = width / 2 - 155;
        int y = 36;
        for (int i = page * perPage; i < Math.min(values.size(), (page + 1) * perPage); i++) {
            addRow(values.get(i), left, y);
            y += ROW_HEIGHT;
        }
        int bottom = height - 28;
        prev = addRenderableWidget(Button.builder(Component.literal("<"), b -> turnPage(-1))
                .bounds(width / 2 - 155, bottom, 20, 20).build());
        next = addRenderableWidget(Button.builder(Component.literal(">"), b -> turnPage(1))
                .bounds(width / 2 + 135, bottom, 20, 20).build());
        prev.active = page > 0;
        next.active = page < pages - 1;
        addRenderableWidget(Button.builder(Component.literal("Reset all"), b -> {
            values.forEach(ConfigValue::reset);
            rebuildWidgets();
        }).bounds(width / 2 - 130, bottom, 80, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(width / 2 - 40, bottom, 80, 20).build());
    }

    private void turnPage(int delta) {
        page += delta;
        rebuildWidgets();
    }

    private <T> void addRow(ConfigValue<T> value, int left, int y) {
        int widgetX = left + 310 - WIDGET_WIDTH;
        Component label = Component.literal(value.path().substring(value.path().lastIndexOf('.') + 1));
        Component name = value.isSynced() ? Component.literal(label.getString() + " *") : label;
        // The label is drawn in render(); the widget sits on the right.
        labels.add(new Label(name, left, y + 6, value.comment()));
        T current = value.localValue();
        ValueType<T> type = value.type();
        if (current instanceof Boolean b) {
            @SuppressWarnings("unchecked")
            ConfigValue<Boolean> flag = (ConfigValue<Boolean>) (ConfigValue<?>) value;
            addRenderableWidget(CycleButton.onOffBuilder(b).create(widgetX, y, WIDGET_WIDTH, 20, Component.empty(),
                    (button, v) -> flag.set(v)));
        } else if (current instanceof Enum<?> e) {
            addEnumButton(value, e, widgetX, y);
        } else if ((current instanceof Integer || current instanceof Double) && type instanceof ValueType.Ranged ranged) {
            addRenderableWidget(new RangeSlider<>(widgetX, y, WIDGET_WIDTH, value, ranged));
        } else {
            addTextBox(value, widgetX, y);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void addEnumButton(ConfigValue value, Enum<?> current, int x, int y) {
        Enum<?>[] constants = current.getDeclaringClass().getEnumConstants();
        addRenderableWidget(CycleButton.builder((Enum<?> e) -> Component.literal(e.name().toLowerCase(Locale.ROOT)))
                .withValues(constants)
                .withInitialValue(current)
                .create(x, y, WIDGET_WIDTH, 20, Component.empty(), (button, v) -> setValue(value, v)));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> void addTextBox(ConfigValue<T> value, int x, int y) {
        EditBox box = new EditBox(font, x, y, WIDGET_WIDTH, 20, Component.empty());
        box.setMaxLength(256);
        T current = value.localValue();
        box.setValue(current instanceof List<?> list ? String.join(", ", (List<String>) list) : String.valueOf(current));
        Consumer<String> onChange = text -> {
            if (current instanceof List) {
                List<String> items = new ArrayList<>();
                for (String part : text.split(",")) {
                    if (!part.isBlank()) {
                        items.add(part.strip());
                    }
                }
                setValue((ConfigValue) value, items);
            } else if (current instanceof String) {
                setValue((ConfigValue) value, text);
            } else {
                setValue((ConfigValue) value, parseNumber(current, text));
            }
        };
        box.setResponder(onChange);
        addRenderableWidget(box);
    }

    private static Object parseNumber(Object like, String text) {
        try {
            return like instanceof Integer ? (Object) Integer.parseInt(text.strip()) : Double.parseDouble(text.strip());
        } catch (NumberFormatException e) {
            return like;
        }
    }

    private static <T> void setValue(ConfigValue<T> value, T newValue) {
        value.set(newValue);
    }

    /** A slider for a ranged int or double option. */
    private static final class RangeSlider<T> extends AbstractSliderButton {
        private final ConfigValue<T> option;
        private final double min;
        private final double max;
        private final boolean integer;

        RangeSlider(int x, int y, int width, ConfigValue<T> option, ValueType.Ranged range) {
            super(x, y, width, 20, Component.empty(),
                    (((Number) option.localValue()).doubleValue() - range.min()) / (range.max() - range.min()));
            this.option = option;
            this.min = range.min();
            this.max = range.max();
            this.integer = option.localValue() instanceof Integer;
            updateMessage();
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void applyValue() {
            double raw = min + (max - min) * this.value;
            option.set((T) (integer ? (Object) (int) Math.round(raw) : (Object) raw));
        }

        @Override
        protected void updateMessage() {
            double shown = min + (max - min) * this.value;
            setMessage(Component.literal(integer ? String.valueOf((int) Math.round(shown)) : String.format(Locale.ROOT, "%.2f", shown)));
        }
    }

    private record Label(Component text, int x, int y, String tooltip) { }

    private final List<Label> labels = new ArrayList<>();

    @Override
    protected void rebuildWidgets() {
        labels.clear();
        super.rebuildWidgets();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        //? if <1.21 {
        /*this.renderBackground(graphics);
        *///?}
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 14, 0xFFFFFF);
        int pages = Math.max(1, (values.size() + perPage - 1) / perPage);
        graphics.drawCenteredString(font, Component.literal((page + 1) + " / " + pages), width / 2, height - 22, 0xAAAAAA);
        for (Label label : labels) {
            graphics.drawString(font, label.text(), label.x(), label.y(), 0xFFFFFF);
            if (!label.tooltip().isEmpty() && mouseX >= label.x() && mouseX < label.x() + 150
                    && mouseY >= label.y() - 2 && mouseY < label.y() + 10) {
                graphics.renderTooltip(font, Component.literal(label.tooltip()), mouseX, mouseY);
            }
        }
    }

    @Override
    public void onClose() {
        config.save();
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }
}
