package org.jd.infestusfrontier.integration.jei;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import org.jd.infestusfrontier.processing.BowlResources;
import org.jd.infestusfrontier.processing.PreparationResources;
import org.jd.infestusfrontier.processing.api.CultureBowlRecipes;
import org.jd.infestusfrontier.processing.api.PreparationRecipes;

/** Optional JEI projection of the processing-owned finite recipe catalog. */
@JeiPlugin
public final class CultureBowlJeiPlugin implements IModPlugin {
    static final RecipeType<BowlDisplayRecipe> TYPE = RecipeType.create(
            "infestusfrontier", "culture_bowl", BowlDisplayRecipe.class);
    static final RecipeType<PreparationDisplayRecipe> RACK_TYPE = RecipeType.create(
            "infestusfrontier", "membrane_rack", PreparationDisplayRecipe.class);
    static final RecipeType<PreparationDisplayRecipe> LOOM_TYPE = RecipeType.create(
            "infestusfrontier", "bone_loom", PreparationDisplayRecipe.class);
    private static final ResourceLocation UID = id("jei/culture_bowl");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        var gui = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new BowlCategory(gui),
                new PreparationCategory(gui, RACK_TYPE, "membrane_rack"),
                new PreparationCategory(gui, LOOM_TYPE, "bone_loom"));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(TYPE, BowlDisplayRecipe.all());
        registration.addRecipes(RACK_TYPE, PreparationDisplayRecipe.all("I002"));
        registration.addRecipes(LOOM_TYPE, PreparationDisplayRecipe.all("I003"));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(BuiltInRegistries.ITEM.get(id("processing/culture_bowl")), TYPE);
        registration.addRecipeCatalyst(BuiltInRegistries.ITEM.get(id("processing/membrane_rack")), RACK_TYPE);
        registration.addRecipeCatalyst(BuiltInRegistries.ITEM.get(id("processing/bone_loom")), LOOM_TYPE);
    }

    static final class PreparationCategory implements IRecipeCategory<PreparationDisplayRecipe> {
        private final RecipeType<PreparationDisplayRecipe> type;
        private final String organ;
        private final IDrawable icon;

        PreparationCategory(IGuiHelper gui, RecipeType<PreparationDisplayRecipe> type, String organ) {
            this.type = type;
            this.organ = organ;
            icon = gui.createDrawableItemStack(new ItemStack(BuiltInRegistries.ITEM.get(id("processing/" + organ))));
        }
        @Override public RecipeType<PreparationDisplayRecipe> getRecipeType() { return type; }
        @Override public Component getTitle() { return Component.translatable("jei.infestusfrontier." + organ); }
        @Override public int getWidth() { return 150; }
        @Override public int getHeight() { return 58; }
        @Override public IDrawable getIcon() { return icon; }
        @Override public void setRecipe(IRecipeLayoutBuilder builder, PreparationDisplayRecipe recipe, IFocusGroup focuses) {
            int slot = 0;
            for (var input : recipe.itemInputs().entrySet()) {
                builder.addInputSlot(slot++ * 20, 0).addItemStack(
                        new ItemStack(PreparationResources.item(input.getKey()), input.getValue()));
            }
            if (recipe.waterMb() > 0) builder.addInputSlot(62, 0).setFluidRenderer(1000, false, 16, 16)
                    .addFluidStack(Fluids.WATER, recipe.waterMb());
            var output = recipe.outputs().entrySet().iterator().next();
            builder.addOutputSlot(112, 0).addItemStack(
                    new ItemStack(PreparationResources.item(output.getKey()), output.getValue()));
        }
        @Override public void draw(PreparationDisplayRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics,
                double mouseX, double mouseY) {
            var font = Minecraft.getInstance().font;
            graphics.drawString(font, Component.translatable("jei.infestusfrontier.duration", recipe.durationSeconds()),
                    0, 36, 0x3B342A, false);
            graphics.drawString(font, Component.translatable("jei.infestusfrontier.biomass", recipe.biomassBu()),
                    0, 47, 0x3B342A, false);
        }
        @Override public ResourceLocation getRegistryName(PreparationDisplayRecipe recipe) {
            return id("jei/" + organ + "/" + recipe.catalogId().toLowerCase(java.util.Locale.ROOT)
                    + "_" + recipe.route());
        }
    }

    static final class BowlCategory implements IRecipeCategory<BowlDisplayRecipe> {
        private final IDrawable icon;

        BowlCategory(IGuiHelper gui) {
            icon = gui.createDrawableItemStack(new ItemStack(
                    BuiltInRegistries.ITEM.get(id("processing/culture_bowl"))));
        }

        @Override public RecipeType<BowlDisplayRecipe> getRecipeType() { return TYPE; }
        @Override public Component getTitle() { return Component.translatable("jei.infestusfrontier.culture_bowl"); }
        @Override public int getWidth() { return 160; }
        @Override public int getHeight() { return 64; }
        @Override public IDrawable getIcon() { return icon; }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, BowlDisplayRecipe recipe, IFocusGroup focuses) {
            int slot = 0;
            for (var input : recipe.itemInputs().entrySet()) {
                builder.addInputSlot((slot % 4) * 20, (slot / 4) * 20)
                        .addItemStack(new ItemStack(BowlResources.item(input.getKey()), input.getValue()));
                slot++;
            }
            if (recipe.waterMb() > 0) {
                builder.addInputSlot(82, 0).setFluidRenderer(1000, false, 16, 16)
                        .addFluidStack(Fluids.WATER, recipe.waterMb());
            }
            int output = 0;
            for (var entry : recipe.outputs().entrySet()) {
                builder.addOutputSlot(112 + output * 20, 0)
                        .addItemStack(new ItemStack(BowlResources.item(entry.getKey()), entry.getValue()));
                output++;
            }
            for (var entry : recipe.returnedContainers().entrySet()) {
                builder.addOutputSlot(112 + output * 20, 0)
                        .addItemStack(new ItemStack(BowlResources.item(entry.getKey()), entry.getValue()));
                output++;
            }
        }

        @Override
        public void draw(BowlDisplayRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics,
                double mouseX, double mouseY) {
            var font = Minecraft.getInstance().font;
            graphics.drawString(font, Component.translatable("jei.infestusfrontier.duration", recipe.durationSeconds()),
                    0, 43, 0x3B342A, false);
            graphics.drawString(font, Component.translatable("jei.infestusfrontier.biomass", recipe.biomassBu()),
                    0, 54, 0x3B342A, false);
        }

        @Override
        public ResourceLocation getRegistryName(BowlDisplayRecipe recipe) {
            return id("jei/culture_bowl/" + recipe.catalogId().toLowerCase(java.util.Locale.ROOT)
                    + "_" + recipe.alternative());
        }
    }

    record BowlDisplayRecipe(String catalogId, int alternative, Map<String, Integer> itemInputs,
            int waterMb, Map<String, Integer> outputs, Map<String, Integer> returnedContainers,
            int durationSeconds, int biomassBu) {
        static List<BowlDisplayRecipe> all() {
            var result = new ArrayList<BowlDisplayRecipe>();
            for (var recipe : CultureBowlRecipes.all().values()) {
                int alternative = 0;
                for (var inputs : recipe.itemInputAlternatives()) {
                    result.add(new BowlDisplayRecipe(recipe.catalogId(), alternative++, sorted(inputs),
                            recipe.fluidInputs().getOrDefault("water", 0), sorted(recipe.outputs()),
                            sorted(recipe.returnedContainers()), recipe.baseWorkUnits() / 20, CultureBowlRecipes.BIOMASS_BU));
                }
            }
            return List.copyOf(result);
        }

        private static Map<String, Integer> sorted(Map<String, Integer> values) {
            var result = new LinkedHashMap<String, Integer>();
            values.entrySet().stream().sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                    .forEach(entry -> result.put(entry.getKey(), entry.getValue()));
            return java.util.Collections.unmodifiableMap(result);
        }
    }

    record PreparationDisplayRecipe(String catalogId, int route, Map<String, Integer> itemInputs,
            int waterMb, int biomassBu, Map<String, Integer> outputs, int durationSeconds) {
        static List<PreparationDisplayRecipe> all(String catalogId) {
            var recipe = PreparationRecipes.recipe(catalogId);
            var result = new ArrayList<PreparationDisplayRecipe>();
            for (int route = 0; route < recipe.routes().size(); route++) {
                var selected = recipe.routes().get(route);
                result.add(new PreparationDisplayRecipe(catalogId, route, BowlDisplayRecipe.sorted(selected.itemInputs()),
                        recipe.fluidInputs().getOrDefault("water", 0),
                        recipe.fluidInputs().getOrDefault("biomass", 0), BowlDisplayRecipe.sorted(recipe.outputs()),
                        selected.workUnits() / PreparationRecipes.TICKS_PER_SECOND));
            }
            return List.copyOf(result);
        }
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("infestusfrontier", path);
    }
}
