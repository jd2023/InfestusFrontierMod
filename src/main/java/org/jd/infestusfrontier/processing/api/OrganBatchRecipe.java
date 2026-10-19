package org.jd.infestusfrontier.processing.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
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
 * One batch of an organ: up to nine ingredients, one item each, plus {@code waterMb} and {@code biomass}, become
 * {@code result} after {@code ticks} loaded ticks. The slots may hold more; a batch takes one item per ingredient and
 * leaves the rest. Data: {@code recipe/culturing/*.json} with
 * {@code "type": "infestusfrontier:organ_batch"}, {@code organ}, {@code ingredients}, {@code water}, {@code biomass},
 * {@code result}, {@code ticks}.
 */
public record OrganBatchRecipe(
        String organ,
        List<Ingredient> ingredients,
        int waterMb,
        Biomass biomass,
        ItemStack result,
        int ticks,
        boolean keepsWear)
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
                    Codec.intRange(1, Integer.MAX_VALUE).fieldOf("ticks").forGetter(OrganBatchRecipe::ticks),
                    Codec.BOOL.optionalFieldOf("keeps_wear", false).forGetter(OrganBatchRecipe::keepsWear))
            .apply(instance, OrganBatchRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, OrganBatchRecipe> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());

    /** True when {@link #plan} finds a batch in the input's slots. */
    @Override
    public boolean matches(RecipeInput input, Level level) {
        List<ItemStack> slots = new ArrayList<>();
        for (int slot = 0; slot < input.size(); slot++) slots.add(input.getItem(slot));
        return plan(slots).isPresent();
    }

    /**
     * How many items each slot gives to one batch, or empty when the slots cannot fill every ingredient. Each
     * ingredient takes one item; a slot gives at most as many items as ingredients accept its stack; whatever the slots
     * hold beyond that stays for the next batch.
     */
    public Optional<int[]> plan(List<ItemStack> slots) {
        List<ItemStack> units = new ArrayList<>();
        List<Integer> from = new ArrayList<>();
        for (int slot = 0; slot < slots.size(); slot++) {
            ItemStack stack = slots.get(slot);
            if (stack.isEmpty()) continue;
            int accepting = (int) ingredients.stream()
                    .filter(ingredient -> ingredient.test(stack))
                    .count();
            for (int i = 0; i < Math.min(stack.getCount(), accepting); i++) {
                units.add(stack);
                from.add(slot);
            }
        }
        if (units.size() < ingredients.size()) return Optional.empty();
        List<Predicate<ItemStack>> tests = new ArrayList<>(ingredients);
        while (tests.size() < units.size()) tests.add(any -> true); // the spare units match nothing in particular
        int[] matched = RecipeMatcher.findMatches(units, tests);
        if (matched == null) return Optional.empty();
        int[] take = new int[slots.size()];
        for (int unit = 0; unit < units.size(); unit++) if (matched[unit] < ingredients.size()) take[from.get(unit)]++;
        return Optional.of(take);
    }

    /**
     * The result for these inputs: a copy of {@link #result}, carrying the first damaged input's wear when the recipe
     * {@link #keepsWear}, so an awakened piece is as worn as the dormant one it came from.
     */
    public ItemStack resultFor(List<ItemStack> inputs) {
        ItemStack made = result.copy();
        if (!keepsWear || !made.isDamageableItem()) return made;
        for (ItemStack input : inputs) {
            if (input.isEmpty() || !input.isDamageableItem()) continue;
            made.setDamageValue((int) ((long) input.getDamageValue() * made.getMaxDamage() / input.getMaxDamage()));
            return made;
        }
        return made;
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
