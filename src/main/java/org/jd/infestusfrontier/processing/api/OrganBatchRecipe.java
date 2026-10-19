package org.jd.infestusfrontier.processing.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.RecipeMatcher;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.processing.ProcessingModule;
import org.jd.infestusfrontier.storage.api.BiomassCodecs;

/**
 * One batch of an organ: up to nine ingredients, each consumed once, plus {@code waterMb} and {@code biomass}, become
 * {@code result} after {@code ticks} loaded ticks. Data: {@code recipe/culturing/*.json} with
 * {@code "type": "infestusfrontier:organ_batch"}, {@code organ}, {@code ingredients}, {@code water}, {@code biomass},
 * {@code result}, {@code ticks}.
 */
public record OrganBatchRecipe(
        String organ, List<Ingredient> ingredients, int waterMb, Biomass biomass, ItemStack result, int ticks)
        implements Recipe<RecipeInput> {
    public static final int MAX_INGREDIENTS = 9;
    public static final MapCodec<OrganBatchRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.STRING.fieldOf("organ").forGetter(OrganBatchRecipe::organ),
                    Ingredient.CODEC_NONEMPTY
                            .listOf(1, MAX_INGREDIENTS)
                            .fieldOf("ingredients")
                            .forGetter(OrganBatchRecipe::ingredients),
                    Codec.intRange(0, Integer.MAX_VALUE).fieldOf("water").forGetter(OrganBatchRecipe::waterMb),
                    BiomassCodecs.BIOMASS.fieldOf("biomass").forGetter(OrganBatchRecipe::biomass),
                    ItemStack.CODEC.fieldOf("result").forGetter(OrganBatchRecipe::result),
                    Codec.intRange(1, Integer.MAX_VALUE).fieldOf("ticks").forGetter(OrganBatchRecipe::ticks))
            .apply(instance, OrganBatchRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, OrganBatchRecipe> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            OrganBatchRecipe::organ,
            Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()),
            OrganBatchRecipe::ingredients,
            ByteBufCodecs.VAR_INT,
            OrganBatchRecipe::waterMb,
            ByteBufCodecs.VAR_INT.map(Biomass::of, Biomass::units),
            OrganBatchRecipe::biomass,
            ItemStack.STREAM_CODEC,
            OrganBatchRecipe::result,
            ByteBufCodecs.VAR_INT,
            OrganBatchRecipe::ticks,
            OrganBatchRecipe::new);

    /** True when the input holds exactly one item per ingredient and every ingredient has its own item. */
    @Override
    public boolean matches(RecipeInput input, Level level) {
        List<ItemStack> items = new ArrayList<>();
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (items.size() + stack.getCount() > ingredients.size()) return false;
            for (int i = 0; i < stack.getCount(); i++) items.add(stack);
        }
        if (items.size() != ingredients.size()) return false;
        return RecipeMatcher.findMatches(items, ingredients) != null;
    }

    /** True when some ingredient of this recipe takes {@code stack}. */
    public boolean uses(ItemStack stack) {
        return ingredients.stream().anyMatch(ingredient -> ingredient.test(stack));
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
        return result;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ProcessingModule.ORGAN_BATCH_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ProcessingModule.ORGAN_BATCH.get();
    }

    /** Reads and writes the JSON and network forms. */
    public static final class Serializer implements RecipeSerializer<OrganBatchRecipe> {
        @Override
        public MapCodec<OrganBatchRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, OrganBatchRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
