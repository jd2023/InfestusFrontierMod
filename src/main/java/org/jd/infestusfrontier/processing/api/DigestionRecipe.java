package org.jd.infestusfrontier.processing.api;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.processing.ProcessingModule;
import org.jd.infestusfrontier.storage.api.BiomassCodecs;

/**
 * One feed item becomes {@code biomass} BU after {@code ticks} loaded ticks. Data: {@code recipe/digestion/*.json} with
 * {@code "type": "infestusfrontier:digestion"}, {@code feed} (ingredient), {@code biomass} (BU), {@code ticks}.
 */
public record DigestionRecipe(Ingredient feed, Biomass biomass, int ticks) implements Recipe<SingleRecipeInput> {
    public static final MapCodec<DigestionRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Ingredient.CODEC_NONEMPTY.fieldOf("feed").forGetter(DigestionRecipe::feed),
                    BiomassCodecs.BIOMASS.fieldOf("biomass").forGetter(DigestionRecipe::biomass),
                    com.mojang.serialization.Codec.intRange(1, Integer.MAX_VALUE)
                            .fieldOf("ticks")
                            .forGetter(DigestionRecipe::ticks))
            .apply(instance, DigestionRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, DigestionRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC,
            DigestionRecipe::feed,
            ByteBufCodecs.VAR_INT.map(Biomass::of, Biomass::units),
            DigestionRecipe::biomass,
            ByteBufCodecs.VAR_INT,
            DigestionRecipe::ticks,
            DigestionRecipe::new);

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return feed.test(input.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ProcessingModule.DIGESTION_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ProcessingModule.DIGESTION.get();
    }

    /** Reads and writes the JSON and network forms. */
    public static final class Serializer implements RecipeSerializer<DigestionRecipe> {
        @Override
        public MapCodec<DigestionRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, DigestionRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
