package org.jd.infestusfrontier.genetics.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.core.genetics.Genetics;
import org.jd.infestusfrontier.genetics.GeneticsModule;
import org.jd.infestusfrontier.storage.api.BiomassCodecs;

/**
 * One species' DNA sample: a Sampling Needle used on a living creature of entity type {@code species} gives one
 * {@code sample}; a DNA Bank extracts one such sample in {@code ticks} loaded ticks for {@code biomass} and adds
 * {@code points} to the species' genome, which is complete at {@value Genetics#COMPLETE}. Data:
 * {@code recipe/dna/*.json} with {@code "type": "infestusfrontier:dna_sample"}. The species is the identity: one recipe
 * per species and one per sample item; when two collide, the first by recipe id wins. Never a crafting recipe.
 */
public record DnaSampleRecipe(ResourceLocation species, ItemStack sample, int points, Biomass biomass, int ticks)
        implements Recipe<RecipeInput> {
    public static final MapCodec<DnaSampleRecipe> CODEC = RecordCodecBuilder.<DnaSampleRecipe>mapCodec(instance ->
                    instance.group(
                                    ResourceLocation.CODEC.fieldOf("species").forGetter(DnaSampleRecipe::species),
                                    ItemStack.CODEC.fieldOf("sample").forGetter(DnaSampleRecipe::sample),
                                    Codec.intRange(1, Genetics.COMPLETE)
                                            .fieldOf("points")
                                            .forGetter(DnaSampleRecipe::points),
                                    BiomassCodecs.BIOMASS.fieldOf("biomass").forGetter(DnaSampleRecipe::biomass),
                                    Codec.intRange(1, Genetics.MAX_EXTRACTION_TICKS)
                                            .fieldOf("ticks")
                                            .forGetter(DnaSampleRecipe::ticks))
                            .apply(instance, DnaSampleRecipe::new))
            .flatXmap(DnaSampleRecipe::checked, DataResult::success);
    public static final StreamCodec<RegistryFriendlyByteBuf, DnaSampleRecipe> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());

    public DnaSampleRecipe {
        sample = sample.copy();
    }

    private static DataResult<DnaSampleRecipe> checked(DnaSampleRecipe recipe) {
        if (!BuiltInRegistries.ENTITY_TYPE.containsKey(recipe.species))
            return DataResult.error(() -> "No entity type named " + recipe.species);
        if (recipe.sample.getCount() != 1) return DataResult.error(() -> "A needle takes one sample at a time");
        if (recipe.biomass.units() > Genetics.BIOMASS_CAPACITY_BU)
            return DataResult.error(() -> "A bank holds at most " + Genetics.BIOMASS_CAPACITY_BU + " BU");
        return DataResult.success(recipe);
    }

    /** Every DNA sample the loaded recipes name, in a stable order: by recipe id. */
    public static List<RecipeHolder<DnaSampleRecipe>> all(Level level) {
        return level.getRecipeManager().getAllRecipesFor(GeneticsModule.DNA_SAMPLE.get()).stream()
                .sorted(Comparator.comparing(RecipeHolder::id))
                .toList();
    }

    /** The sample a needle takes from a creature of {@code type}, or empty when no recipe names it. */
    public static Optional<DnaSampleRecipe> forCreature(Level level, EntityType<?> type) {
        ResourceLocation species = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        return all(level).stream()
                .map(RecipeHolder::value)
                .filter(recipe -> recipe.species.equals(species))
                .findFirst();
    }

    /** The recipe whose sample {@code stack} is, whatever its count, or empty when none is. */
    public static Optional<DnaSampleRecipe> forSample(Level level, ItemStack stack) {
        if (stack.isEmpty()) return Optional.empty();
        return all(level).stream()
                .map(RecipeHolder::value)
                .filter(recipe -> ItemStack.isSameItemSameComponents(recipe.sample, stack))
                .findFirst();
    }

    /** The creature this species is, for its name and its spawn egg. */
    public EntityType<?> creature() {
        return BuiltInRegistries.ENTITY_TYPE.get(species);
    }

    /** Never matches a grid: a needle and a bank use it. */
    @Override
    public boolean matches(RecipeInput input, Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(RecipeInput input, HolderLookup.Provider registries) {
        return sample.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return sample.copy();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return GeneticsModule.DNA_SAMPLE_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return GeneticsModule.DNA_SAMPLE.get();
    }

    /** Reads and writes the JSON and network forms. */
    public static final class Serializer implements RecipeSerializer<DnaSampleRecipe> {
        @Override
        public MapCodec<DnaSampleRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, DnaSampleRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
