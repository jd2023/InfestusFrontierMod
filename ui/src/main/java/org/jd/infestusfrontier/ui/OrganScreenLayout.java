package org.jd.infestusfrontier.ui;

import java.util.ArrayList;
import java.util.List;

/** Fixed logical layout centered in the GUI-scaled viewport. */
public final class OrganScreenLayout {
    public static final int WIDTH = 248;
    public static final int HEIGHT = 166;
    private final Bounds panel;
    private final List<Control> focusOrder;

    public static OrganScreenLayout centered(int viewportWidth, int viewportHeight) {
        if (viewportWidth < WIDTH || viewportHeight < HEIGHT) {
            throw new IllegalArgumentException("Organ screen viewport is smaller than its bounded layout");
        }
        int left = (viewportWidth - WIDTH) / 2;
        int top = (viewportHeight - HEIGHT) / 2;
        var controls = new ArrayList<Control>(17);
        for (int slot = 0; slot < 9; slot++) {
            controls.add(new Control("slot-" + slot,
                    new Bounds(left + 12 + slot % 3 * 20, top + 28 + slot / 3 * 20, 18, 18)));
        }
        for (int recipe = 0; recipe < 6; recipe++) {
            controls.add(new Control("recipe-" + recipe,
                    new Bounds(left + 84 + recipe % 2 * 74, top + 28 + recipe / 2 * 22, 70, 18)));
        }
        controls.add(new Control("start", new Bounds(left + 84, top + 100, 70, 20)));
        controls.add(new Control("cancel", new Bounds(left + 158, top + 100, 70, 20)));
        return new OrganScreenLayout(new Bounds(left, top, WIDTH, HEIGHT), List.copyOf(controls));
    }

    private OrganScreenLayout(Bounds panel, List<Control> focusOrder) {
        this.panel = panel;
        this.focusOrder = focusOrder;
    }

    public Bounds panel() { return panel; }
    public List<Control> focusOrder() { return focusOrder; }
    public Control control(String id) {
        return focusOrder.stream().filter(control -> control.id().equals(id)).findFirst().orElseThrow();
    }

    public record Control(String id, Bounds bounds) {}

    public record Bounds(int x, int y, int width, int height) {
        public Bounds {
            if (width < 1 || height < 1) throw new IllegalArgumentException("Bounds need positive dimensions");
        }
        public int centerX() { return x + width / 2; }
        public int centerY() { return y + height / 2; }
        public boolean contains(int pointX, int pointY) {
            return pointX >= x && pointX < x + width && pointY >= y && pointY < y + height;
        }
        public boolean contains(Bounds child) {
            return contains(child.x, child.y)
                    && child.x + child.width <= x + width
                    && child.y + child.height <= y + height;
        }
    }
}
