package org.jd.infestusfrontier.integration.modonomicon;

import com.klikli_dev.modonomicon.book.BookTextHolder;
import com.klikli_dev.modonomicon.client.gui.book.entry.BookEntryScreen;
import com.klikli_dev.modonomicon.client.render.page.BookRecipePageRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jd.infestusfrontier.core.genetics.Genetics;
import org.jd.infestusfrontier.genetics.GeneticsModule;
import org.jd.infestusfrontier.genetics.api.DnaSampleRecipe;

/**
 * Draws a {@link DnaSamplePage}: the creature's spawn egg and the Sampling Needle, an arrow, the sample, an arrow and
 * the DNA Bank, then a line naming the species and what one extraction adds and costs.
 */
public final class DnaSamplePageRenderer extends BookRecipePageRenderer<DnaSampleRecipe, DnaSamplePage> {
    private static final int SLOT = 18;
    private static final int ITEMS_Y = 14;
    private static final int ARROW = 12;
    /** Lines the extraction's numbers may wrap to, and their pitch: the page's own text starts below them. */
    private static final int LINES = 3;

    private static final int LINE_HEIGHT = 10;
    /** The width a line may take: the page less its inner margin, so a long line wraps before the binding. */
    private static final int TEXT_WIDTH = BookEntryScreen.PAGE_WIDTH - 20;

    public DnaSamplePageRenderer(DnaSamplePage page) {
        super(page);
    }

    @Override
    protected int getRecipeHeight() {
        return ITEMS_Y + SLOT + 2 + LINES * LINE_HEIGHT + 4;
    }

    @Override
    protected void drawRecipe(
            GuiGraphics graphics,
            RecipeHolder<DnaSampleRecipe> holder,
            int x,
            int y,
            int mouseX,
            int mouseY,
            boolean second) {
        DnaSampleRecipe recipe = holder.value();
        BookTextHolder title = second ? page.getTitle2() : page.getTitle1();
        if (!title.isEmpty()) renderTitle(graphics, title, false, BookEntryScreen.PAGE_WIDTH / 2, y);
        int ink = page.getBook().getDefaultTextColor();
        int row = y + ITEMS_Y;
        int at = x;
        SpawnEggItem egg = SpawnEggItem.byId(recipe.creature());
        if (egg != null) {
            parentScreen.renderItemStack(graphics, at, row, mouseX, mouseY, new ItemStack(egg));
            at += SLOT;
        }
        parentScreen.renderItemStack(
                graphics, at, row, mouseX, mouseY, new ItemStack(GeneticsModule.SAMPLING_NEEDLE.get()));
        at += SLOT + 2;
        graphics.drawString(font, "→", at, row + 4, ink, false);
        at += ARROW;
        parentScreen.renderItemStack(graphics, at, row, mouseX, mouseY, recipe.sample());
        at += SLOT + 2;
        graphics.drawString(font, "→", at, row + 4, ink, false);
        at += ARROW;
        parentScreen.renderItemStack(
                graphics, at, row, mouseX, mouseY, new ItemStack(GeneticsModule.DNA_BANK_ITEM.get()));
        String line = I18n.get(
                "book.infestusfrontier.dna_sample.extract",
                recipe.creature().getDescription().getString(),
                recipe.points() * 100 / Genetics.COMPLETE,
                recipe.biomass().units(),
                recipe.ticks() / 20);
        int lineY = row + SLOT + 2;
        for (FormattedCharSequence part : font.split(FormattedText.of(line), TEXT_WIDTH)) {
            graphics.drawString(font, part, x, lineY, ink, false);
            lineY += LINE_HEIGHT;
        }
    }
}
