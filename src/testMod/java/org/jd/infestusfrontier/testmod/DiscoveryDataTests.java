package org.jd.infestusfrontier.testmod;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jd.infestusfrontier.control.ControlModule;
import org.jd.infestusfrontier.cultivation.CultivationModule;
import org.jd.infestusfrontier.ecology.EcologyModule;
import org.jd.infestusfrontier.equipment.EquipmentModule;
import org.jd.infestusfrontier.logistics.LogisticsModule;
import org.jd.infestusfrontier.organ.OrganModule;
import org.jd.infestusfrontier.processing.ProcessingModule;
import org.jd.infestusfrontier.storage.StorageModule;

/** The workshop recipes and the advancements that reveal them in the recipe book. */
@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class DiscoveryDataTests {
    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("infestusfrontier", path);
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void mouthRecipeResolves(GameTestHelper h) {
        resolves(
                h,
                List.of(OrganModule.ORGAN_BUD_ITEM.get(), Items.HOPPER, ProcessingModule.MEMBRANE_SHEET.get()),
                LogisticsModule.MOUTH_ITEM.get());
        h.succeed();
    }

    private static void resolves(GameTestHelper h, List<Item> inputs, Item expected) {
        List<ItemStack> stacks =
                new ArrayList<>(inputs.stream().map(ItemStack::new).toList());
        while (stacks.size() < 4) {
            stacks.add(ItemStack.EMPTY);
        }
        CraftingInput input = CraftingInput.of(2, 2, stacks);
        var recipe = h.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, h.getLevel());
        h.assertTrue(recipe.isPresent(), "recipe present for " + expected);
        ItemStack result = recipe.get().value().assemble(input, h.getLevel().registryAccess());
        h.assertTrue(result.is(expected), "result is " + expected);
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void workshopRecipesResolve(GameTestHelper h) {
        Item culture = EcologyModule.SPORE_CULTURE.get();
        Item bud = OrganModule.ORGAN_BUD_ITEM.get();
        resolves(h, List.of(culture, Items.ROTTEN_FLESH, Items.ROTTEN_FLESH, Items.BONE_MEAL), bud);
        resolves(
                h,
                List.of(bud, Items.ROTTEN_FLESH, Items.ROTTEN_FLESH, Items.BOWL),
                ProcessingModule.DIGESTIVE_SAC_ITEM.get());
        resolves(h, List.of(bud, Items.GLASS, Items.GLASS, Items.SLIME_BALL), StorageModule.BIOMASS_BLADDER_ITEM.get());
        Item sheet = ProcessingModule.MEMBRANE_SHEET.get();
        resolves(
                h,
                List.of(bud, sheet, sheet, ProcessingModule.BONE_PLATE.get()),
                ProcessingModule.ACTIVATION_CYST_ITEM.get());
        resolves(h, List.of(bud, sheet, sheet, Items.SLIME_BALL), LogisticsModule.VASCULAR_JUNCTION_ITEM.get());
        resolves(h, List.of(Items.LEATHER, sheet, culture), CultivationModule.SEED_POUCH_ITEM.get());
        resolves(
                h,
                List.of(bud, Items.WOODEN_HOE, CultivationModule.ROOTING_GEL.get(), sheet),
                CultivationModule.PLANTING_PROBOSCIS_ITEM.get());
        resolves(h, List.of(bud, Items.IRON_HOE, sheet, sheet), CultivationModule.HARVEST_COROLLA_ITEM.get());
        resolves(h, List.of(Items.COMPOSTER, bud, sheet), ProcessingModule.COMPOST_GLAND_ITEM.get());
        resolves(h, List.of(bud, sheet, Items.SLIME_BALL), LogisticsModule.FEEDING_LOBE_ITEM.get());
        resolves(h, List.of(bud, sheet, ProcessingModule.BONE_PLATE.get()), LogisticsModule.YIELD_LOBE_ITEM.get());
        resolves(h, List.of(bud, Items.COMPARATOR, Items.SPIDER_EYE), ControlModule.SENSOR_POLYP_ITEM.get());
        resolves(h, List.of(LogisticsModule.CAPILLARY_GEL.get(), sheet), EquipmentModule.FUEL_PAPILLA_ITEM.get());
        resolves(
                h,
                List.of(ProcessingModule.BONE_PLATE.get(), Items.SLIME_BALL, Items.PAPER),
                LogisticsModule.FILTER_VALVE_ITEM.get());
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void cultureBowlRecipeResolves(GameTestHelper h) {
        resolves(
                h,
                List.of(Items.FLOWER_POT, EcologyModule.SPORE_CULTURE.get(), Items.WHEAT_SEEDS),
                ProcessingModule.CULTURE_BOWL_ITEM.get());
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void recipeUnlockAdvancementsExist(GameTestHelper h) {
        for (String path : List.of(
                "spore_culture",
                "organ_bud",
                "digestive_sac",
                "biomass_bladder",
                "culture_bowl",
                "activation_cyst",
                "vascular_junction",
                "mouth",
                "seed_pouch",
                "planting_proboscis",
                "harvest_corolla",
                "compost_gland",
                "feeding_lobe",
                "yield_lobe",
                "sensor_polyp",
                "fuel_papilla",
                "filter_valve",
                "reservoir_cell",
                "capsule_stem",
                "surgical_blade",
                "membrane_window",
                "sphincter_door",
                "tread_receptor",
                "septum_crossing",
                "vascular_stitch")) {
            ResourceLocation recipe = id(path);
            AdvancementHolder holder =
                    h.getLevel().getServer().getAdvancements().get(id("recipes/" + path));
            h.assertTrue(holder != null, "advancement recipes/" + path + " loaded");
            var criteria = holder.value().criteria();
            h.assertTrue(criteria.containsKey("has_the_recipe"), path + " has has_the_recipe");
            long inputs = criteria.keySet().stream()
                    .filter(k -> k.startsWith("has_") && !k.equals("has_the_recipe"))
                    .count();
            h.assertTrue(inputs == 1, path + " has one input criterion");
            h.assertTrue(holder.value().rewards().recipes().contains(recipe), path + " rewards its recipe");
        }
        h.succeed();
    }
}
