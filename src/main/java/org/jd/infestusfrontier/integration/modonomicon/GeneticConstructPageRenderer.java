package org.jd.infestusfrontier.integration.modonomicon;

import com.klikli_dev.modonomicon.book.BookTextHolder;
import com.klikli_dev.modonomicon.client.gui.book.entry.BookEntryScreen;
import com.klikli_dev.modonomicon.client.render.page.BookRecipePageRenderer;
import java.util.StringJoiner;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import org.jd.infestusfrontier.genetics.api.GeneticConstructRecipe;

/**
 * Draws a {@link GeneticConstructPage}: the spawn egg of each genome it needs, a plus, its materials with their counts,
 * an arrow and the construct, then a line naming the genomes and the biomass.
 */
public final class GeneticConstructPageRenderer
        extends BookRecipePageRenderer<GeneticConstructRecipe, GeneticConstructPage> {
    private static final int SLOT = 18;
    private static final int ITEMS_Y = 14;
    private static final int SIGN = 10;
    /** Lines the genomes and biomass may wrap to, and their pitch: the page's own text starts below them. */
    private static final int LINES = 3;

    private static final int LINE_HEIGHT = 10;
    /** The width a line may take: the page less its inner margin, so a long line wraps before the binding. */
    private static final int TEXT_WIDTH = BookEntryScreen.PAGE_WIDTH - 20;

    public GeneticConstructPageRenderer(GeneticConstructPage page) {
        super(page);
    }

    @Override
    protected int getRecipeHeight() {
        return ITEMS_Y + SLOT + 2 + LINES * LINE_HEIGHT + 4;
    }

    @Override
    protected void drawRecipe(
            GuiGraphics graphics,
            RecipeHolder<GeneticConstructRecipe> holder,
            int x,
            int y,
            int mouseX,
            int mouseY,
            boolean second) {
        GeneticConstructRecipe recipe = holder.value();
        BookTextHolder title = second ? page.getTitle2() : page.getTitle1();
        if (!title.isEmpty()) renderTitle(graphics, title, false, BookEntryScreen.PAGE_WIDTH / 2, y);
        int ink = page.getBook().getDefaultTextColor();
        int row = y + ITEMS_Y;
        int at = x;
        for (EntityType<?> creature : recipe.creatures()) {
            SpawnEggItem egg = SpawnEggItem.byId(creature);
            if (egg == null) continue;
            parentScreen.renderItemStack(graphics, at, row, mouseX, mouseY, new ItemStack(egg));
            at += SLOT;
        }
        graphics.drawString(font, "+", at, row + 4, ink, false);
        at += SIGN - 2;
        for (SizedIngredient material : recipe.materials()) {
            parentScreen.renderIngredient(graphics, at, row, mouseX, mouseY, material.ingredient());
            ItemStack[] kinds = material.getItems();
            if (kinds.length > 0) graphics.renderItemDecorations(font, kinds[0], at, row);
            at += SLOT;
        }
        graphics.drawString(font, "→", at + 3, row + 4, ink, false); // clear of the last material's count
        at += SIGN + 2;
        parentScreen.renderItemStack(graphics, at, row, mouseX, mouseY, recipe.result());
        StringJoiner genomes = new StringJoiner(", ");
        for (EntityType<?> creature : recipe.creatures())
            genomes.add(creature.getDescription().getString());
        String line = I18n.get(
                "book.infestusfrontier.genetic_construct.needs",
                genomes.toString(),
                recipe.biomass().units());
        int lineY = row + SLOT + 2;
        for (FormattedCharSequence part : font.split(FormattedText.of(line), TEXT_WIDTH)) {
            graphics.drawString(font, part, x, lineY, ink, false);
            lineY += LINE_HEIGHT;
        }
    }
}
