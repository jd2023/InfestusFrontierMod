package org.jd.infestusfrontier.integration.modonomicon;

import com.klikli_dev.modonomicon.book.BookTextHolder;
import com.klikli_dev.modonomicon.client.gui.book.entry.BookEntryScreen;
import com.klikli_dev.modonomicon.client.render.page.BookRecipePageRenderer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jd.infestusfrontier.processing.api.OrganBatchRecipe;

/** Draws an {@link OrganBatchPage}: ingredients in rows of four, an arrow, the result, and one line of needs. */
public final class OrganBatchPageRenderer extends BookRecipePageRenderer<OrganBatchRecipe, OrganBatchPage> {
    private static final int SLOT = 18;
    private static final int PER_ROW = 4;
    /** Rows for {@link OrganBatchRecipe#MAX_INGREDIENTS} ingredients. */
    private static final int ROWS = (OrganBatchRecipe.MAX_INGREDIENTS + PER_ROW - 1) / PER_ROW;

    private static final int ITEMS_Y = 14;

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
        int arrowX = x + PER_ROW * SLOT + 2;
        graphics.drawString(font, "→", arrowX, y + ITEMS_Y + 4, ink, false);
        parentScreen.renderItemStack(graphics, arrowX + 10, y + ITEMS_Y, mouseX, mouseY, recipe.result());
        List<String> needs = new ArrayList<>();
        if (recipe.waterMb() > 0) needs.add(I18n.get("book.infestusfrontier.organ_batch.water", recipe.waterMb()));
        if (!recipe.biomass().isZero())
            needs.add(I18n.get(
                    "book.infestusfrontier.organ_batch.biomass",
                    recipe.biomass().units()));
        needs.add(I18n.get("book.infestusfrontier.organ_batch.seconds", recipe.ticks() / 20));
        int rows = (ingredients.size() + PER_ROW - 1) / PER_ROW;
        graphics.drawString(font, String.join(", ", needs), x, y + ITEMS_Y + rows * SLOT + 2, ink, false);
    }
}
