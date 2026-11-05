package org.jd.infestusfrontier.foundation.screen;

import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jd.infestusfrontier.ui.Bounds;
import org.jetbrains.annotations.Nullable;

/** One sheet sprite on the panel, chosen every frame by a supplier that returns null to draw nothing, with a tooltip. */
public final class Glyph extends Part {
    private final Supplier<@Nullable Bounds> sprite;
    private final Supplier<Component> story;

    /** A sprite {@code width x height} whose top left corner is at panel position {@code x, y}. */
    public Glyph(int x, int y, int width, int height, Supplier<@Nullable Bounds> sprite, Supplier<Component> story) {
        super(new Bounds(x, y, width, height), Component.empty());
        this.sprite = sprite;
        this.story = story;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Bounds shown = sprite.get();
        tip(shown == null ? null : story.get());
        if (shown != null) Draw.sprite(graphics, shown, getX(), getY());
    }
}
