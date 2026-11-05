package org.jd.infestusfrontier.foundation.screen;

import java.util.Objects;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.jd.infestusfrontier.ui.Bounds;
import org.jetbrains.annotations.Nullable;

/**
 * One drawn part of an organ screen. It is laid out in panel pixels; {@link OrganScreen#add} moves it into the
 * window. A part reads its values from suppliers every frame and never decides anything. Passive: it takes no clicks
 * and no focus, but it is hovered, so it can carry a tooltip and a narration.
 */
public abstract class Part extends AbstractWidget {
    private final Bounds panel;
    private @Nullable Component lastTip;

    Part(Bounds panel, Component narration) {
        super(panel.x(), panel.y(), panel.width(), panel.height(), narration);
        this.panel = panel;
        this.active = false;
    }

    /** Where the part sits on the panel. */
    final Bounds panel() {
        return panel;
    }

    /** Shows {@code text} while hovered, or nothing for null. Cheap to call every frame. */
    protected final void tip(@Nullable Component text) {
        if (Objects.equals(text, lastTip)) return;
        lastTip = text;
        setTooltip(text == null ? null : Tooltip.create(text));
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, getMessage());
    }
}
