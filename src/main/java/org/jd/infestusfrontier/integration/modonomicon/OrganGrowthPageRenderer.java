package org.jd.infestusfrontier.integration.modonomicon;

import com.klikli_dev.modonomicon.book.BookTextHolder;
import com.klikli_dev.modonomicon.client.gui.book.entry.BookEntryScreen;
import com.klikli_dev.modonomicon.client.render.page.BookRecipePageRenderer;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import org.jd.infestusfrontier.organ.api.OrganGrowthRecipe;

/** Draws an {@link OrganGrowthPage}: the materials in rows of four, each with its count, an arrow and the organ. */
public final class OrganGrowthPageRenderer extends BookRecipePageRenderer<OrganGrowthRecipe, OrganGrowthPage> {
    private static final int SLOT = 18;
    private static final int PER_ROW = 4;
    private static final int ROWS = (OrganGrowthRecipe.MAX_MATERIALS + PER_ROW - 1) / PER_ROW;
    private static final int ITEMS_Y = 14;

    public OrganGrowthPageRenderer(OrganGrowthPage page) {
        super(page);
    }

    @Override
    protected int getRecipeHeight() {
        return ITEMS_Y + ROWS * SLOT + 4;
    }

    @Override
    protected void drawRecipe(
            GuiGraphics graphics,
            RecipeHolder<OrganGrowthRecipe> holder,
            int x,
            int y,
            int mouseX,
            int mouseY,
            boolean second) {
        OrganGrowthRecipe recipe = holder.value();
        BookTextHolder title = second ? page.getTitle2() : page.getTitle1();
        if (!title.isEmpty()) renderTitle(graphics, title, false, BookEntryScreen.PAGE_WIDTH / 2, y);
        List<SizedIngredient> materials = recipe.materials();
        for (int i = 0; i < materials.size(); i++) {
            SizedIngredient material = materials.get(i);
            int slotX = x + (i % PER_ROW) * SLOT;
            int slotY = y + ITEMS_Y + (i / PER_ROW) * SLOT;
            parentScreen.renderIngredient(graphics, slotX, slotY, mouseX, mouseY, material.ingredient());
            ItemStack[] kinds = material.getItems();
            if (kinds.length > 0) graphics.renderItemDecorations(font, kinds[0], slotX, slotY);
        }
        int arrowX = x + Math.min(materials.size(), PER_ROW) * SLOT + 2;
        graphics.drawString(font, "→", arrowX, y + ITEMS_Y + 4, page.getBook().getDefaultTextColor(), false);
        parentScreen.renderItemStack(
                graphics,
                arrowX + 10,
                y + ITEMS_Y,
                mouseX,
                mouseY,
                recipe.getResultItem(Minecraft.getInstance().level.registryAccess()));
    }
}
