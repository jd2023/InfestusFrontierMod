package org.jd.infestusfrontier.foundation.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jd.infestusfrontier.InfestusFrontier;
import org.jd.infestusfrontier.ui.Bounds;
import org.jd.infestusfrontier.ui.Sheet;

/** The only place that blits the organ sheet or draws ink. Window coordinates. */
final class Draw {
    static final ResourceLocation SHEET =
            ResourceLocation.fromNamespaceAndPath(InfestusFrontier.MOD_ID, "textures/gui/organ.png");

    private Draw() {}

    static Font font() {
        return Minecraft.getInstance().font;
    }

    /** One sheet sprite with its top left corner at {@code x, y}. */
    static void sprite(GuiGraphics graphics, Bounds sprite, int x, int y) {
        graphics.blit(SHEET, x, y, sprite.x(), sprite.y(), sprite.width(), sprite.height());
    }

    /** The part of {@code sprite} that starts {@code skipX, skipY} inside it and is {@code width x height}. */
    static void sprite(GuiGraphics graphics, Bounds sprite, int x, int y, int skipX, int skipY, int width, int height) {
        graphics.blit(SHEET, x, y, sprite.x() + skipX, sprite.y() + skipY, width, height);
    }

    static void ink(GuiGraphics graphics, Component text, int x, int y) {
        graphics.drawString(font(), text, x, y, Sheet.INK, false);
    }

    static void centered(GuiGraphics graphics, Component text, int centerX, int y) {
        ink(graphics, text, centerX - font().width(text) / 2, y);
    }

    /** A slot count at half size, white with its shadow as vanilla draws counts, ending at {@code rightX, bottomY}. */
    static void smallCount(GuiGraphics graphics, String count, int rightX, int bottomY) {
        graphics.pose().pushPose();
        graphics.pose().translate(rightX, bottomY, 200);
        graphics.pose().scale(0.5F, 0.5F, 1);
        graphics.drawString(font(), count, -font().width(count), 1 - font().lineHeight, 0xFFFFFF, true);
        graphics.pose().popPose();
    }

    static void rightAligned(GuiGraphics graphics, Component text, int rightX, int y) {
        ink(graphics, text, rightX - font().width(text), y);
    }
}
