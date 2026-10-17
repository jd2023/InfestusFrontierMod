package org.jd.infestusfrontier.testmod;

import com.klikli_dev.modonomicon.data.BookDataManager;
import com.klikli_dev.modonomicon.registry.DataComponentRegistry;
import java.util.List;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jd.infestusfrontier.ecology.EcologyModule;

/** The Waking Genome guide: its advancements, its recipe and its book data. */
@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class DiscoveryTests {
    private static final ResourceLocation BOOK = id("the_waking_genome");

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("infestusfrontier", path);
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void advancementsLoad(GameTestHelper h) {
        for (String path : List.of("contact/root", "contact/a_culture_in_your_hand", "contact/living_substrate")) {
            AdvancementHolder holder =
                    h.getLevel().getServer().getAdvancements().get(id(path));
            h.assertTrue(holder != null, "advancement " + path + " loaded");
        }
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void cultureAdvancementHasInventoryCriterion(GameTestHelper h) {
        AdvancementHolder holder = h.getLevel().getServer().getAdvancements().get(id("contact/a_culture_in_your_hand"));
        h.assertTrue(holder != null, "advancement loaded");
        h.assertTrue(holder.value().criteria().containsKey("has_culture"), "criterion has_culture present");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void bookRecipeResolves(GameTestHelper h) {
        List<ItemStack> inputs = List.of(new ItemStack(Items.BOOK), new ItemStack(EcologyModule.SPORE_CULTURE.get()));
        var recipe = h.getLevel()
                .getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, CraftingInput.of(2, 1, inputs), h.getLevel());
        h.assertTrue(recipe.isPresent(), "recipe present");
        ItemStack result = recipe.get()
                .value()
                .assemble(CraftingInput.of(2, 1, inputs), h.getLevel().registryAccess());
        h.assertTrue(
                BuiltInRegistries.ITEM.getKey(result.getItem()).toString().equals("modonomicon:modonomicon"),
                "result is modonomicon:modonomicon");
        h.assertTrue(BOOK.equals(result.get(DataComponentRegistry.BOOK_ID.get())), "book_id is the_waking_genome");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void bookLoads(GameTestHelper h) {
        h.assertTrue(BookDataManager.get().getBooks().containsKey(BOOK), "book known to BookDataManager");
        h.succeed();
    }
}
