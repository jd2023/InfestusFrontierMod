package org.jd.infestusfrontier.processing.client;

import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jd.infestusfrontier.processing.menu.BowlIntentPayload;
import org.jd.infestusfrontier.processing.menu.CultureBowlMenu;
import org.jd.infestusfrontier.ui.UiTheme;
import org.jd.infestusfrontier.ui.client.OrganScreen;

/** Thin rendering adapter over the internal UI layout and immutable processing snapshot. */
public final class CultureBowlScreen extends OrganScreen<CultureBowlMenu> {
    private static final List<String> RECIPES = List.copyOf(
            org.jd.infestusfrontier.processing.api.CultureBowlRecipes.all().keySet());
    private String selectedRecipe;

    public CultureBowlScreen(CultureBowlMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        selectedRecipe = menu.snapshot().selectedRecipe();
    }

    @Override protected void init() {
        super.init();
        for (int i = 0; i < 9; i++) {
            final int index = i;
            slot("slot-" + i, Component.translatable("screen.infestusfrontier.bowl.slot", i + 1),
                    Component.translatable("screen.infestusfrontier.bowl.slot.tooltip", i + 1), () -> {
                        var stored = menu.snapshot().slots().get(index);
                        return stored.resource().isEmpty() ? ItemStack.EMPTY : new ItemStack(
                                BuiltInRegistries.ITEM.get(ResourceLocation.parse(stored.resource())), stored.count());
                    }, () -> menu.submit(BowlIntentPayload.Intent.EXTRACT_SLOT, index, ""));
        }
        for (int i = 0; i < RECIPES.size(); i++) {
            String recipe = RECIPES.get(i);
            action("recipe-" + i, Component.translatable("screen.infestusfrontier.bowl.recipe." + recipe),
                    Component.translatable("screen.infestusfrontier.bowl.recipe.tooltip." + recipe), () -> selectedRecipe = recipe);
        }
        action("start", Component.translatable("screen.infestusfrontier.bowl.start"),
                Component.translatable("screen.infestusfrontier.bowl.start.tooltip"),
                () -> menu.submit(BowlIntentPayload.Intent.START, -1, selectedRecipe));
        action("cancel", Component.translatable("screen.infestusfrontier.bowl.cancel"),
                Component.translatable("screen.infestusfrontier.bowl.cancel.tooltip"),
                () -> menu.submit(BowlIntentPayload.Intent.CANCEL, -1, ""));
    }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        panel(graphics);
        bar(graphics, 130, menu.snapshot().water(), menu.snapshot().waterCapacity(), 0xFF3A9FC4);
        bar(graphics, 143, menu.snapshot().biomass(), menu.snapshot().biomassCapacity(), 0xFF8D4463);
        bar(graphics, 156, menu.snapshot().completedWork(), menu.snapshot().requiredWork(), 0xFF82B94B);
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 12, 9, UiTheme.TEXT, false);
        graphics.drawString(font, Component.translatable("screen.infestusfrontier.bowl.water",
                menu.snapshot().water(), menu.snapshot().waterCapacity()), 12, 120, UiTheme.MUTED_TEXT, false);
        graphics.drawString(font, Component.translatable("screen.infestusfrontier.bowl.biomass",
                menu.snapshot().biomass(), menu.snapshot().biomassCapacity()), 126, 120, UiTheme.MUTED_TEXT, false);
        graphics.drawString(font, Component.translatable("screen.infestusfrontier.bowl.state." +
                menu.snapshot().state().name().toLowerCase(java.util.Locale.ROOT)), 12, 91, UiTheme.TEXT, false);
        graphics.drawString(font, Component.translatable("screen.infestusfrontier.bowl.completed",
                menu.snapshot().completedBatches()), 12, 102, UiTheme.MUTED_TEXT, false);
        if (menu.snapshot().refusal() != org.jd.infestusfrontier.processing.menu.BowlRefusal.NONE) {
            graphics.drawString(font, Component.translatable("screen.infestusfrontier.bowl.refusal." +
                    menu.snapshot().refusal().name().toLowerCase(java.util.Locale.ROOT)), 12, 168, 0xFFFFA0A0, false);
        }
    }

}
