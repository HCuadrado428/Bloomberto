package com.dougg865.bloomberto.api.ui;

import java.util.ArrayList;
import java.util.List;

/**
 * A one-dimensional flexible layout, like CSS flexbox with a single line: items are placed in a row or a column, may have a
 * fixed size or share the remaining space by weight, and are justified and aligned inside the container.
 *
 * <pre>{@code
 * List<Rect> cells = FlexLayout.row().gap(4).padding(6).alignItems(FlexLayout.Align.CENTER)
 *         .add(FlexLayout.Item.fixed(20, 20))          // an icon
 *         .add(FlexLayout.Item.grow(1, 0, 12))         // a label that takes the free space
 *         .add(FlexLayout.Item.fixed(60, 20))          // a button
 *         .compute(new Rect(10, 10, 200, 30));
 * }</pre>
 *
 * Pure math: it only computes rectangles; place your widgets with them.
 */
public final class FlexLayout {
    public enum Direction { ROW, COLUMN }

    /** How leftover space on the main axis is distributed when no item grows. */
    public enum Justify { START, CENTER, END, SPACE_BETWEEN }

    /** Placement on the cross axis. {@code STRETCH} fills it (items with a cross size of 0). */
    public enum Align { START, CENTER, END, STRETCH }

    /**
     * One item.
     *
     * @param main  preferred size on the main axis
     * @param cross preferred size on the cross axis (0 with {@link Align#STRETCH} means "fill")
     * @param grow  share of the free space it takes (0 = none)
     */
    public record Item(int main, int cross, float grow, int minMain, int maxMain) {
        public static Item fixed(int main, int cross) {
            return new Item(main, cross, 0f, 0, Integer.MAX_VALUE);
        }

        /** An item that takes {@code weight} parts of the free space on top of its {@code main} size. */
        public static Item grow(float weight, int main, int cross) {
            return new Item(main, cross, weight, 0, Integer.MAX_VALUE);
        }

        public Item min(int minMain) {
            return new Item(main, cross, grow, minMain, maxMain);
        }

        public Item max(int maxMain) {
            return new Item(main, cross, grow, minMain, maxMain);
        }
    }

    private final Direction direction;
    private final List<Item> items = new ArrayList<>();
    private int gap;
    private int padding;
    private Justify justify = Justify.START;
    private Align align = Align.START;

    private FlexLayout(Direction direction) {
        this.direction = direction;
    }

    public static FlexLayout row() {
        return new FlexLayout(Direction.ROW);
    }

    public static FlexLayout column() {
        return new FlexLayout(Direction.COLUMN);
    }

    public FlexLayout gap(int gap) {
        this.gap = Math.max(0, gap);
        return this;
    }

    public FlexLayout padding(int padding) {
        this.padding = Math.max(0, padding);
        return this;
    }

    public FlexLayout justify(Justify justify) {
        this.justify = justify;
        return this;
    }

    public FlexLayout alignItems(Align align) {
        this.align = align;
        return this;
    }

    public FlexLayout add(Item item) {
        items.add(item);
        return this;
    }

    /** Computes one rectangle per item, in the order they were added, inside {@code container}. */
    public List<Rect> compute(Rect container) {
        Rect inner = container.inset(padding);
        boolean row = direction == Direction.ROW;
        int mainSpace = row ? inner.width() : inner.height();
        int crossSpace = row ? inner.height() : inner.width();
        int n = items.size();
        if (n == 0) {
            return List.of();
        }

        int[] sizes = new int[n];
        int used = gap * (n - 1);
        float totalGrow = 0f;
        for (int i = 0; i < n; i++) {
            Item item = items.get(i);
            sizes[i] = clamp(item.main(), item);
            used += sizes[i];
            totalGrow += item.grow();
        }
        int free = mainSpace - used;
        if (free > 0 && totalGrow > 0f) {
            distributeGrowth(sizes, free, totalGrow);
            free = mainSpace - gap * (n - 1) - sum(sizes);
        } else if (free < 0) {
            shrink(sizes, -free);
            free = Math.max(0, mainSpace - gap * (n - 1) - sum(sizes));
        }
        free = Math.max(0, free);

        int cursor = row ? inner.x() : inner.y();
        int extraGap = 0;
        switch (justify) {
            case CENTER -> cursor += free / 2;
            case END -> cursor += free;
            case SPACE_BETWEEN -> extraGap = n > 1 ? free / (n - 1) : 0;
            default -> { }
        }
        List<Rect> result = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            Item item = items.get(i);
            int crossSize = align == Align.STRETCH && item.cross() == 0 ? crossSpace : Math.min(item.cross(), crossSpace);
            int crossOffset = switch (align) {
                case CENTER -> (crossSpace - crossSize) / 2;
                case END -> crossSpace - crossSize;
                default -> 0;
            };
            if (row) {
                result.add(new Rect(cursor, inner.y() + crossOffset, sizes[i], crossSize));
            } else {
                result.add(new Rect(inner.x() + crossOffset, cursor, crossSize, sizes[i]));
            }
            cursor += sizes[i] + gap + extraGap;
        }
        return result;
    }

    /** Hands out free space by weight, honouring each item's maximum; leftover pixels go to the first growing items. */
    private void distributeGrowth(int[] sizes, int free, float totalGrow) {
        int remaining = free;
        float weightLeft = totalGrow;
        boolean[] done = new boolean[sizes.length];
        boolean changed = true;
        while (changed && remaining > 0 && weightLeft > 0f) {
            changed = false;
            for (int i = 0; i < sizes.length; i++) {
                Item item = items.get(i);
                if (done[i] || item.grow() <= 0f) {
                    continue;
                }
                int share = Math.round(remaining * item.grow() / weightLeft);
                int room = item.maxMain() - sizes[i];
                if (share >= room) {
                    sizes[i] += room;
                    remaining -= room;
                    weightLeft -= item.grow();
                    done[i] = true;
                    changed = true;
                }
            }
        }
        int pixels = remaining;
        float weights = weightLeft;
        for (int i = 0; i < sizes.length && weights > 0f; i++) {
            Item item = items.get(i);
            if (done[i] || item.grow() <= 0f) {
                continue;
            }
            int share = Math.min(pixels, Math.round(remaining * item.grow() / weightLeft));
            sizes[i] += share;
            pixels -= share;
        }
        // rounding leftovers
        for (int i = 0; i < sizes.length && pixels > 0; i++) {
            Item item = items.get(i);
            if (!done[i] && item.grow() > 0f && sizes[i] < item.maxMain()) {
                sizes[i]++;
                pixels--;
            }
        }
    }

    /** Takes {@code excess} pixels away from the items, largest first, never below their minimum. */
    private void shrink(int[] sizes, int excess) {
        int left = excess;
        while (left > 0) {
            int target = -1;
            for (int i = 0; i < sizes.length; i++) {
                if (sizes[i] > items.get(i).minMain() && (target < 0 || sizes[i] > sizes[target])) {
                    target = i;
                }
            }
            if (target < 0) {
                return;
            }
            sizes[target]--;
            left--;
        }
    }

    private static int clamp(int size, Item item) {
        return Math.max(item.minMain(), Math.min(item.maxMain(), size));
    }

    private static int sum(int[] values) {
        int total = 0;
        for (int v : values) {
            total += v;
        }
        return total;
    }
}
