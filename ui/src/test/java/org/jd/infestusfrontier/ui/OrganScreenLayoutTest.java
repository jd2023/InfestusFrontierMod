package org.jd.infestusfrontier.ui;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

final class OrganScreenLayoutTest {
    @Test
    void guiScalesKeepEveryControlVisibleClickableAndNonOverlapping() {
        for (int scale : new int[] {2, 3, 4}) {
            int width = (int) Math.ceil(1280.0 / scale);
            int height = (int) Math.ceil(720.0 / scale);
            var layout = OrganScreenLayout.centered(width, height);
            assertTrue(new OrganScreenLayout.Bounds(0, 0, width, height).contains(layout.panel()),
                    "panel outside viewport at scale " + scale);
            var seen = new java.util.ArrayList<OrganScreenLayout.Bounds>();
            for (var control : layout.focusOrder()) {
                assertTrue(control.bounds().width() >= 16 && control.bounds().height() >= 16, "click target at scale " + scale);
                assertTrue(layout.panel().contains(control.bounds()), "control outside panel at scale " + scale);
                var b = control.bounds();
                for (var a : seen) assertFalse(a.x() < b.x() + b.width() && b.x() < a.x() + a.width()
                        && a.y() < b.y() + b.height() && b.y() < a.y() + a.height(), "overlap at scale " + scale);
                seen.add(b);
                assertTrue(control.bounds().contains(control.bounds().centerX(), control.bounds().centerY()));
            }
            assertEquals(20, layout.focusOrder().size(), "nine slots, nine recipes, start and cancel");
        }
    }

    @Test
    void keyboardOrderIsStableAndTooltipPaletteHasStrongContrast() {
        var layout = OrganScreenLayout.centered(320, 180);
        assertEquals("slot-0", layout.focusOrder().getFirst().id());
        assertEquals("cancel", layout.focusOrder().getLast().id());
        assertTrue(UiTheme.contrastRatio(UiTheme.TOOLTIP_TEXT, UiTheme.TOOLTIP_BACKGROUND) >= 7.0);
        assertTrue(UiTheme.contrastRatio(UiTheme.FOCUSED_BORDER, UiTheme.PANEL_BACKGROUND) >= 4.5);
    }
}
