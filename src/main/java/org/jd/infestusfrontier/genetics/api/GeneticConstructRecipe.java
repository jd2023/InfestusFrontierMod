package org.jd.infestusfrontier.genetics.api;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.core.genetics.Genetics;
import org.jd.infestusfrontier.core.genetics.Synthesis;
import org.jd.infestusfrontier.genetics.GeneticsModule;
import org.jd.infestusfrontier.storage.api.BiomassCodecs;

/**
 * One genetic construct: a DNA Bank whose research holds every genome in {@code genomes} complete turns
 * {@code materials} (flat sized ingredients, one construct slot each, in any order) and {@code biomass} into
 * {@code result}. The result names its own target: the item applied to a host is the mutation. Producing one keeps
 * the genomes. Data: {@code recipe/construct/*.json} with {@code "type": "infestusfrontier:genetic_construct"}. When the
 * same materials answer two recipes, the first by recipe id wins. Never a crafting recipe.
 */
public record GeneticConstructRecipe(
        List<ResourceLocation> genomes, List<SizedIngredient> materials, Biomass biomass, ItemStack result)
        implements Recipe<RecipeInput> {
    public static final MapCodec<GeneticConstructRecipe> CODEC = RecordCodecBuilder.<GeneticConstructRecipe>mapCodec(
                    instance -> instance.group(
                                    ResourceLocation.CODEC
                                            .listOf(1, Genetics.MAX_GENOMES)
                                            .fieldOf("genomes")
                                            .forGetter(GeneticConstructRecipe::genomes),
                                    SizedIngredient.FLAT_CODEC
                                            .listOf(1, Genetics.MATERIAL_SLOTS)
                                            .fieldOf("materials")
                                            .forGetter(GeneticConstructRecipe::materials),
                                    BiomassCodecs.BIOMASS.fieldOf("biomass").forGetter(GeneticConstructRecipe::biomass),
                                    ItemStack.CODEC.fieldOf("result").forGetter(GeneticConstructRecipe::result))
                            .apply(instance, GeneticConstructRecipe::new))
            .flatXmap(GeneticConstructRecipe::checked, DataResult::success);
    public static final StreamCodec<RegistryFriendlyByteBuf, GeneticConstructRecipe> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());

    public GeneticConstructRecipe {
        genomes = List.copyOf(genomes);
        materials = List.copyOf(materials);
        result = result.copy();
    }

    private static DataResult<GeneticConstructRecipe> checked(GeneticConstructRecipe recipe) {
        for (ResourceLocation species : recipe.genomes)
            if (!BuiltInRegistries.ENTITY_TYPE.containsKey(species))
                return DataResult.error(() -> "No entity type named " + species);
        if (new HashSet<>(recipe.genomes).size() != recipe.genomes.size())
            return DataResult.error(() -> "A genome named twice");
        for (SizedIngredient material : recipe.materials)
            if (material.count() > Item.DEFAULT_MAX_STACK_SIZE)
                return DataResult.error(() -> "A material slot holds one stack: " + material);
        if (recipe.biomass.units() > Genetics.BIOMASS_CAPACITY_BU)
            return DataResult.error(() -> "A bank holds at most " + Genetics.BIOMASS_CAPACITY_BU + " BU");
        if (recipe.result.getCount() > recipe.result.getMaxStackSize())
            return DataResult.error(() -> "The construct slot holds one stack");
        return DataResult.success(recipe);
    }

    /** Every construct the loaded recipes name, in a stable order: by recipe id. */
    public static List<RecipeHolder<GeneticConstructRecipe>> all(Level level) {
        return level.getRecipeManager().getAllRecipesFor(GeneticsModule.GENETIC_CONSTRUCT.get()).stream()
                .sorted(Comparator.comparing(RecipeHolder::id))
                .toList();
    }

    /** True when some construct recipe takes {@code stack} as a material, whatever its count. */
    public static boolean usesMaterial(Level level, ItemStack stack) {
        if (stack.isEmpty()) return false;
        return all(level).stream()
                .anyMatch(holder -> holder.value().materials.stream()
                        .anyMatch(material -> material.ingredient().test(stack)));
    }

    /** The first recipe {@code slots} answer, with the slot each of its materials is taken from, or empty. */
    public static Optional<Match> matching(Level level, List<ItemStack> slots) {
        for (RecipeHolder<GeneticConstructRecipe> holder : all(level)) {
            Optional<List<Integer>> taken = holder.value().assign(slots);
            if (taken.isPresent()) return Optional.of(new Match(holder, taken.get()));
        }
        return Optional.empty();
    }

    private Optional<List<Integer>> assign(List<ItemStack> slots) {
        return Synthesis.assign(
                materials.size(),
                slots.stream().map(stack -> !stack.isEmpty()).toList(),
                (material, slot) -> materials.get(material).test(slots.get(slot)));
    }

    /** A recipe that {@code slots} answer, and the slot each of its materials is taken from, by material. */
    public record Match(RecipeHolder<GeneticConstructRecipe> holder, List<Integer> slots) {
        public Match {
            slots = List.copyOf(slots);
        }

        public GeneticConstructRecipe recipe() {
            return holder.value();
        }
    }

    /** The genomes as species ids, as research names them. */
    public List<String> species() {
        List<String> species = new ArrayList<>(genomes.size());
        for (ResourceLocation genome : genomes) species.add(genome.toString());
        return species;
    }

    /** The creatures whose genomes it needs, for their names and spawn eggs. */
    public List<EntityType<?>> creatures() {
        return genomes.stream()
                .<EntityType<?>>map(BuiltInRegistries.ENTITY_TYPE::get)
                .toList();
    }

    /** Never matches a grid: a DNA Bank uses it. */
    @Override
    public boolean matches(RecipeInput input, Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(RecipeInput input, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return GeneticsModule.GENETIC_CONSTRUCT_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return GeneticsModule.GENETIC_CONSTRUCT.get();
    }

    /** Reads and writes the JSON and network forms. */
    public static final class Serializer implements RecipeSerializer<GeneticConstructRecipe> {
        @Override
        public MapCodec<GeneticConstructRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, GeneticConstructRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
