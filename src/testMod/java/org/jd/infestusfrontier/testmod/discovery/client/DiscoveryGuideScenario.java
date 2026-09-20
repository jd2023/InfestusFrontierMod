package org.jd.infestusfrontier.testmod.discovery.client;

import com.klikli_dev.modonomicon.client.gui.BookGuiManager;
import com.klikli_dev.modonomicon.client.gui.book.entry.BookEntryScreen;
import com.klikli_dev.modonomicon.data.BookDataManager;
import java.util.ArrayDeque;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.jd.infestusfrontier.discovery.GuideAccess;
import org.jd.infestusfrontier.testmod.integration.ContentRequirements;
import org.jd.infestusfrontier.testmod.integration.client.ContentGuideScenario;
import org.jd.infestusfrontier.testmod.integration.client.ContentVisualScenario;

/** Opens every eligible owner-contributed entry from the actual synchronized guide book. */
public final class DiscoveryGuideScenario implements ContentGuideScenario, ContentVisualScenario {
    private static final ResourceLocation FIRST_ENTRY = ResourceLocation.fromNamespaceAndPath(
            "infestusfrontier", "discovery/waking_genome");
    private final ArrayDeque<ContentRequirements.GuideRequirement> pending = new ArrayDeque<>(
            ContentRequirements.read().guideRequirements());
    private ContentRequirements.GuideRequirement opened;
    private int page;
    private static final SurvivalDiscoveryScenario survival = new SurvivalDiscoveryScenario();

    @Override
    public String assertion() {
        return "infestusfrontier_client:discovery.waking_genome.guide";
    }

    @Override
    public List<String> assertions() {
        return pending.stream().map(ContentRequirements.GuideRequirement::assertion).toList();
    }

    @Override
    public boolean ready(Minecraft minecraft) {
        return BookDataManager.get().areBooksBuilt()
                && BookDataManager.get().getBook(GuideAccess.BOOK_ID) != null && survival.tick(minecraft);
    }

    @Override
    public List<UiView> uiViews() {
        return List.of(new UiView("waking-genome-guide.png", 3),
                new UiView("bowl-guide-controls.png", 3), new UiView("bowl-guide-recipes-1.png", 3),
                new UiView("bowl-guide-recipes-2.png", 3), new UiView("bowl-guide-recipes-3.png", 3),
                new UiView("bowl-recipe-browser.png", 3), new UiView("preparation-items.png", 3),
                new UiView("rack-guide-flesh.png", 3), new UiView("rack-guide-leather.png", 3),
                new UiView("loom-guide.png", 3), new UiView("rack-recipe-browser.png", 3),
                new UiView("loom-recipe-browser.png", 3));
    }

    @Override
    public boolean prepareUi(Minecraft minecraft, UiView view) {
        if (view.filename().equals("preparation-items.png")) {
            for (var slot : java.util.Map.of(10, "membrane_sheet", 12, "bone_plate").entrySet()) {
                var stack = minecraft.player.getInventory().getItem(slot.getKey());
                if (!stack.is(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ResourceLocation.parse(
                        "infestusfrontier:processing/" + slot.getValue())))) {
                    throw new IllegalStateException("Missing server-supplied preparation inventory item " + slot.getValue());
                }
            }
            if (minecraft.screen instanceof net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen screen) {
                if (screen.isInventoryOpen()) return true;
                var tab = screen.getCurrentPage().getVisibleTabs().stream()
                        .filter(value -> value.getType() == net.minecraft.world.item.CreativeModeTab.Type.INVENTORY)
                        .findFirst().orElseThrow();
                int column = screen.getCurrentPage().getColumn(tab);
                int x = screen.getGuiLeft() + 195 - 27 * (7 - column) + 14;
                int y = screen.getGuiTop() + 148;
                screen.mouseClicked(x, y, 0);
                screen.mouseReleased(x, y, 0);
                return false;
            }
            if (minecraft.screen instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen) return true;
            minecraft.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(minecraft.player));
            return false;
        }
        String browser = switch (view.filename()) {
            case "bowl-recipe-browser.png" -> "culture_bowl";
            case "rack-recipe-browser.png" -> "membrane_rack";
            case "loom-recipe-browser.png" -> "bone_loom";
            default -> "";
        };
        if (!browser.isEmpty() && net.neoforged.fml.ModList.get().isLoaded("jei")) {
            return JeiDiscoveryScenario.prepare(minecraft, browser);
        }
        var entry = switch (view.filename()) {
            case "waking-genome-guide.png" -> FIRST_ENTRY;
            case "rack-guide-flesh.png", "rack-guide-leather.png", "rack-recipe-browser.png" ->
                    ResourceLocation.parse("infestusfrontier:processing/membrane_rack");
            case "loom-guide.png", "loom-recipe-browser.png" -> ResourceLocation.parse("infestusfrontier:processing/bone_loom");
            default -> ResourceLocation.parse("infestusfrontier:processing/culture_bowl");
        };
        int target = switch (view.filename()) {
            case "bowl-guide-recipes-1.png", "bowl-recipe-browser.png", "rack-guide-leather.png" -> 2;
            case "bowl-guide-recipes-2.png" -> 4;
            case "bowl-guide-recipes-3.png" -> 6;
            default -> 0;
        };
        if (minecraft.screen instanceof BookEntryScreen screen && screen.getEntry().getId().equals(entry)
                && screen.getCurrentPageNumber() == target) return true;
        BookGuiManager.get().openEntry(GuideAccess.BOOK_ID, entry, target);
        return false;
    }

    @Override
    public boolean tick(Minecraft minecraft) {
        if (minecraft.level == null || minecraft.player == null) return false;
        if (!survival.tick(minecraft)) return false;
        if (opened != null) {
            if (!(minecraft.screen instanceof BookEntryScreen screen)
                    || !screen.getEntry().getId().equals(ResourceLocation.parse(opened.entry()))) {
                throw new IllegalStateException("Guide did not render contributed entry " + opened.entry());
            }
            if (screen.getCurrentPageNumber() != page) throw new IllegalStateException("Wrong guide spread");
            page += 2;
            if (page < screen.getEntry().getPages().size()) {
                screen.goToPage(page, false);
                return false;
            }
            pending.remove();
            opened = null;
            page = 0;
        }
        var next = pending.peek();
        if (next == null) return true;
        var book = BookDataManager.get().getBook(GuideAccess.BOOK_ID);
        if (book == null || !BookDataManager.get().areBooksBuilt()) return false;
        var entryId = ResourceLocation.parse(next.entry());
        var entry = book.getEntry(entryId);
        if (entry == null || entry.getPages().isEmpty()) {
            throw new IllegalStateException("Guide is missing contributed entry pages " + next.entry());
        }
        verifyRecipes(entry);
        BookGuiManager.get().openEntry(GuideAccess.BOOK_ID, entryId, 0);
        opened = next;
        return false;
    }
    private static void verifyRecipes(com.klikli_dev.modonomicon.book.entries.BookEntry entry) {
        if (!entry.getId().getPath().equals("processing/culture_bowl")) {
            verifyPreparationRecipes(entry);
            return;
        }
        int page = 2;
        for (var recipe : org.jd.infestusfrontier.processing.api.CultureBowlRecipes.all().values()) {
            var text = ((com.klikli_dev.modonomicon.book.page.BookTextPage) entry.getPages().get(page++)).getText().getString();
            var quantities = new java.util.ArrayList<java.util.Map<String, Integer>>(recipe.itemInputAlternatives());
            quantities.add(recipe.outputs());
            quantities.add(recipe.returnedContainers());
            for (var map : quantities) for (var item : map.entrySet()) {
                String expected = item.getValue() + " × " + org.jd.infestusfrontier.processing.BowlResources.item(item.getKey()).getDescription().getString();
                if (!text.contains(expected)) throw new IllegalStateException("Guide lacks " + expected);
            }
            if (!text.contains(recipe.fluidInputs().getOrDefault("water", 0) + " mB")
                    || !text.contains(recipe.baseWorkUnits() / 20 + " loaded seconds") || !text.contains("0 BU"))
                throw new IllegalStateException("Guide recipe costs differ from processing " + recipe.catalogId());
        }
    }

    private static void verifyPreparationRecipes(com.klikli_dev.modonomicon.book.entries.BookEntry entry) {
        String catalogId = switch (entry.getId().getPath()) {
            case "processing/membrane_rack", "processing/membrane_sheet" -> "I002";
            case "processing/bone_loom", "processing/bone_plate" -> "I003";
            default -> "";
        };
        if (catalogId.isEmpty()) return;
        var recipe = org.jd.infestusfrontier.processing.api.PreparationRecipes.recipe(catalogId);
        String text = entry.getPages().stream()
                .filter(com.klikli_dev.modonomicon.book.page.BookTextPage.class::isInstance)
                .map(com.klikli_dev.modonomicon.book.page.BookTextPage.class::cast)
                .map(page -> page.getText().getString()).collect(java.util.stream.Collectors.joining("\n"));
        for (var route : recipe.routes()) {
            for (var item : route.itemInputs().entrySet()) {
                String expected = item.getValue() + " × "
                        + org.jd.infestusfrontier.processing.PreparationResources.item(item.getKey()).getDescription().getString();
                if (!text.contains(expected)) throw new IllegalStateException("Guide lacks " + expected);
            }
            String duration = route.workUnits() / org.jd.infestusfrontier.processing.api.PreparationRecipes.TICKS_PER_SECOND
                    + " loaded seconds";
            if (!text.contains(duration)) throw new IllegalStateException("Guide lacks " + duration);
        }
        for (var output : recipe.outputs().entrySet()) {
            String expected = output.getValue() + " × "
                    + org.jd.infestusfrontier.processing.PreparationResources.item(output.getKey()).getDescription().getString();
            if (!text.contains(expected)) throw new IllegalStateException("Guide lacks " + expected);
        }
        int fluid = recipe.fluidInputs().values().stream().findFirst().orElse(0);
        if (!text.contains(fluid + (catalogId.equals("I002") ? " mB" : " BU"))) {
            throw new IllegalStateException("Guide fluid cost differs from processing " + catalogId);
        }
    }

}
