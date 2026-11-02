package org.jd.infestusfrontier.integration.modonomicon;

import com.klikli_dev.modonomicon.book.BookTextHolder;
import com.klikli_dev.modonomicon.client.gui.book.entry.BookEntryScreen;
import com.klikli_dev.modonomicon.client.render.page.BookRecipePageRenderer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jd.infestusfrontier.processing.api.OrganBatchRecipe;

/**
 * Draws an {@link OrganBatchPage}: ingredients in rows of four, an arrow just after the first row, the results, and
 * one line of needs.
 */
public final class OrganBatchPageRenderer extends BookRecipePageRenderer<OrganBatchRecipe, OrganBatchPage> {
    private static final int SLOT = 18;
    private static final int PER_ROW = 4;
    /** Rows for {@link OrganBatchRecipe#MAX_INGREDIENTS} ingredients. */
    private static final int ROWS = (OrganBatchRecipe.MAX_INGREDIENTS + PER_ROW - 1) / PER_ROW;

    private static final int ITEMS_Y = 14;
    /** The results of a recipe with byproducts stand in one row after the arrow. */
    private static final int RESULTS_PER_ROW = 3;
    /** The width a line of needs may take: the page less its inner margin, so a long line wraps before the binding. */
    private static final int TEXT_WIDTH = BookEntryScreen.PAGE_WIDTH - 20;

    public OrganBatchPageRenderer(OrganBatchPage page) {
        super(page);
    }

    @Override
    protected int getRecipeHeight() {
        return ITEMS_Y + ROWS * SLOT + 12;
    }

    @Override
    protected void drawRecipe(
            GuiGraphics graphics,
            RecipeHolder<OrganBatchRecipe> holder,
            int x,
            int y,
            int mouseX,
            int mouseY,
            boolean second) {
        OrganBatchRecipe recipe = holder.value();
        BookTextHolder title = second ? page.getTitle2() : page.getTitle1();
        if (!title.isEmpty()) renderTitle(graphics, title, false, BookEntryScreen.PAGE_WIDTH / 2, y);
        List<Ingredient> ingredients = recipe.ingredients();
        for (int i = 0; i < ingredients.size(); i++)
            parentScreen.renderIngredient(
                    graphics,
                    x + (i % PER_ROW) * SLOT,
                    y + ITEMS_Y + (i / PER_ROW) * SLOT,
                    mouseX,
                    mouseY,
                    ingredients.get(i));
        int ink = page.getBook().getDefaultTextColor();
        int arrowX = x + Math.min(ingredients.size(), PER_ROW) * SLOT + 2;
        graphics.drawString(font, "→", arrowX, y + ITEMS_Y + 4, ink, false);
        List<ItemStack> results = recipe.results();
        for (int i = 0; i < results.size(); i++)
            parentScreen.renderItemStack(
                    graphics,
                    arrowX + 10 + (i % RESULTS_PER_ROW) * SLOT,
                    y + ITEMS_Y + (i / RESULTS_PER_ROW) * SLOT,
                    mouseX,
                    mouseY,
                    results.get(i));
        List<String> needs = new ArrayList<>();
        if (recipe.waterMb() > 0) needs.add(I18n.get("book.infestusfrontier.organ_batch.water", recipe.waterMb()));
        if (!recipe.biomass().isZero())
            needs.add(I18n.get(
                    "book.infestusfrontier.organ_batch.biomass",
                    recipe.biomass().units()));
        needs.add(I18n.get("book.infestusfrontier.organ_batch.seconds", recipe.ticks() / 20));
        if (recipe.dirtyIn() > 0) needs.add(I18n.get("book.infestusfrontier.organ_batch.dirty_in", recipe.dirtyIn()));
        if (recipe.dirtyOut() > 0)
            needs.add(I18n.get("book.infestusfrontier.organ_batch.dirty_out", recipe.dirtyOut()));
        int rows = Math.max(
                Math.max(1, (ingredients.size() + PER_ROW - 1) / PER_ROW),
                (results.size() + RESULTS_PER_ROW - 1) / RESULTS_PER_ROW);
        drawWrapped(graphics, String.join(", ", needs), x, y + ITEMS_Y + rows * SLOT + 2, ink);
    }

    /** Draws {@code text} in lines no wider than {@link #TEXT_WIDTH}, from {@code y}. */
    private void drawWrapped(GuiGraphics graphics, String text, int x, int y, int ink) {
        for (FormattedCharSequence part : font.split(FormattedText.of(text), TEXT_WIDTH)) {
            graphics.drawString(font, part, x, y, ink, false);
            y += font.lineHeight + 1;
        }
    }
}
