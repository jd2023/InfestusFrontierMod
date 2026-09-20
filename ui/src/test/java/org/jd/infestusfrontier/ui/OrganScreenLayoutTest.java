package org.jd.infestusfrontier.ui;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import org.junit.jupiter.api.Test;

final class OrganScreenLayoutTest {
    @Test
    void guiScalesKeepEveryControlVisibleClickableAndNonOverlapping() {
        for (int scale : new int[] {2, 3, 4}) {
            int width = (int) Math.ceil(1280.0 / scale);
            int height = (int) Math.ceil(720.0 / scale);
            var layout = OrganScreenLayout.centered(width, height);
            var seen = new HashSet<OrganScreenLayout.Bounds>();
            for (var control : layout.focusOrder()) {
                assertTrue(control.bounds().width() >= 16 && control.bounds().height() >= 16, "click target at scale " + scale);
                assertTrue(layout.panel().contains(control.bounds()), "control outside panel at scale " + scale);
                assertTrue(seen.add(control.bounds()), "overlapping controls at scale " + scale);
                assertTrue(control.bounds().contains(control.bounds().centerX(), control.bounds().centerY()));
            }
            assertEquals(17, layout.focusOrder().size(), "nine slots, six recipes, start and cancel");
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
