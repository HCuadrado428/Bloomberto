package com.dougg865.bloomberto.api.client;

import com.dougg865.bloomberto.Bloomberto;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.ItemStack;

/**
 * Adds lines to item tooltips, with helpers for colors and "hold Shift for more" sections. Client only.
 *
 * <pre>{@code
 * Tooltips.shiftInfo(stack -> stack.is(MyItems.RUBY.get()),
 *         List.of(Component.literal("Found deep underground.")));
 * Tooltips.add(stack -> stack.isEnchanted(), (stack, lines, advanced) ->
 *         lines.add(Component.literal("Shiny").withStyle(ChatFormatting.AQUA)));
 * }</pre>
 */
public final class Tooltips {
    /** Adds lines to a tooltip. {@code advanced} is true with F3+H. */
    @FunctionalInterface
    public interface Provider {
        void addLines(ItemStack stack, List<Component> lines, boolean advanced);
    }

    private record Rule(Predicate<ItemStack> filter, Provider provider) { }

    private static final List<Rule> RULES = new ArrayList<>();

    private Tooltips() {
    }

    /** Adds {@code provider}'s lines to every tooltip whose stack matches {@code filter}. */
    public static synchronized void add(Predicate<ItemStack> filter, Provider provider) {
        RULES.add(new Rule(filter, provider));
    }

    /**
     * Shows {@code details} while Shift is held, otherwise a gray "Hold Shift for more info" hint.
     */
    public static void shiftInfo(Predicate<ItemStack> filter, List<Component> details) {
        add(filter, (stack, lines, advanced) -> {
            if (Screen.hasShiftDown()) {
                lines.addAll(details);
            } else {
                lines.add(Component.literal("Hold [Shift] for more info").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            }
        });
    }

    /** Called by the loader code for every tooltip. */
    public static void apply(ItemStack stack, List<Component> lines, boolean advanced) {
        List<Rule> snapshot;
        synchronized (Tooltips.class) {
            snapshot = new ArrayList<>(RULES);
        }
        for (Rule rule : snapshot) {
            try {
                if (rule.filter().test(stack)) {
                    rule.provider().addLines(stack, lines, advanced);
                }
            } catch (RuntimeException e) {
                Bloomberto.LOGGER.error("A tooltip provider failed", e);
            }
        }
    }
}
