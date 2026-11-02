package org.jd.infestusfrontier.processing.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
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
 * {@code result}, {@code ticks}. A recipe may also name up to {@link #MAX_BYPRODUCTS} {@code byproducts}, each held in
 * the output slot after the result's, and the {@code experience} a smelting recipe earns. An organ with a dirty tank
 * (the Washing Kidney) reads {@code dirty_in}, the dirty water the batch takes from it at its start, and
 * {@code dirty_out}, the dirty water it adds there at its start; a recipe that takes dirty water may name no
 * ingredient. {@code slot} puts the first result in a later output slot.
 */
public record OrganBatchRecipe(
        String organ,
        List<Ingredient> ingredients,
        int waterMb,
        Biomass biomass,
        ItemStack result,
        int ticks,
        boolean keepsWear,
        List<DataComponentType<?>> keeps,
        List<ItemStack> byproducts,
        float experience,
        int dirtyIn,
        int dirtyOut,
        int slot)
        implements Recipe<RecipeInput> {
    public static final int MAX_INGREDIENTS = 9;
    /** The most results beyond the first: an organ's output tray has at most {@code 1 + MAX_BYPRODUCTS} slots. */
    public static final int MAX_BYPRODUCTS = 2;

    public static final MapCodec<OrganBatchRecipe> CODEC = RecordCodecBuilder.<OrganBatchRecipe>mapCodec(instance ->
                    instance.group(
                                    Codec.STRING.fieldOf("organ").forGetter(OrganBatchRecipe::organ),
                                    Ingredient.CODEC_NONEMPTY
                                            .listOf(0, MAX_INGREDIENTS)
                                            .fieldOf("ingredients")
                                            .forGetter(OrganBatchRecipe::ingredients),
                                    Codec.intRange(0, Integer.MAX_VALUE)
                                            .fieldOf("water")
                                            .forGetter(OrganBatchRecipe::waterMb),
                                    BiomassCodecs.BIOMASS.fieldOf("biomass").forGetter(OrganBatchRecipe::biomass),
                                    ItemStack.CODEC.fieldOf("result").forGetter(OrganBatchRecipe::result),
                                    Codec.intRange(1, Integer.MAX_VALUE)
                                            .fieldOf("ticks")
                                            .forGetter(OrganBatchRecipe::ticks),
                                    Codec.BOOL
                                            .optionalFieldOf("keeps_wear", false)
                                            .forGetter(OrganBatchRecipe::keepsWear),
                                    DataComponentType.CODEC
                                            .listOf()
                                            .optionalFieldOf("keeps", List.of())
                                            .forGetter(OrganBatchRecipe::keeps),
                                    ItemStack.CODEC
                                            .listOf(0, MAX_BYPRODUCTS)
                                            .optionalFieldOf("byproducts", List.of())
                                            .forGetter(OrganBatchRecipe::byproducts),
                                    Codec.floatRange(0, BatchOrganContents.MAX_EXPERIENCE)
                                            .optionalFieldOf("experience", 0f)
                                            .forGetter(OrganBatchRecipe::experience),
                                    Codec.intRange(0, BatchOrganContents.MAX_DIRTY)
                                            .optionalFieldOf("dirty_in", 0)
                                            .forGetter(OrganBatchRecipe::dirtyIn),
                                    Codec.intRange(0, BatchOrganContents.MAX_DIRTY)
                                            .optionalFieldOf("dirty_out", 0)
                                            .forGetter(OrganBatchRecipe::dirtyOut),
                                    Codec.intRange(0, BatchOrganContents.MAX_OUTPUTS - 1)
                                            .optionalFieldOf("slot", 0)
                                            .forGetter(OrganBatchRecipe::slot))
                            .apply(instance, OrganBatchRecipe::new))
            .flatXmap(OrganBatchRecipe::checked, DataResult::success);
    public static final StreamCodec<RegistryFriendlyByteBuf, OrganBatchRecipe> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());

    public OrganBatchRecipe {
        byproducts = byproducts.stream().map(ItemStack::copy).toList();
    }

    /** A recipe with no dirty water and its results from the first output slot. */
    public OrganBatchRecipe(
            String organ,
            List<Ingredient> ingredients,
            int waterMb,
            Biomass biomass,
            ItemStack result,
            int ticks,
            boolean keepsWear,
            List<DataComponentType<?>> keeps,
            List<ItemStack> byproducts,
            float experience) {
        this(organ, ingredients, waterMb, biomass, result, ticks, keepsWear, keeps, byproducts, experience, 0, 0, 0);
    }

    /** A recipe with nothing to take is no recipe: a batch needs an ingredient or dirty water, and room for its results. */
    private static DataResult<OrganBatchRecipe> checked(OrganBatchRecipe recipe) {
        if (recipe.ingredients.isEmpty() && recipe.dirtyIn == 0)
            return DataResult.error(() -> "A batch takes an ingredient or dirty water");
        if (recipe.slot + 1 + recipe.byproducts.size() > BatchOrganContents.MAX_OUTPUTS)
            return DataResult.error(() -> "The results do not fit the output slots after slot " + recipe.slot);
        return DataResult.success(recipe);
    }

    @Override
    public List<ItemStack> byproducts() {
        return byproducts.stream().map(ItemStack::copy).toList();
    }

    /** Every result, in output slot order: the result, then the byproducts. */
    public List<ItemStack> results() {
        List<ItemStack> all = new ArrayList<>();
        all.add(result.copy());
        all.addAll(byproducts());
        return all;
    }

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
        if (ingredients.isEmpty()) return Optional.of(new int[slots.size()]);
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
     * {@link #keepsWear}, so an awakened piece is as worn as the dormant one it came from, and each component the
     * recipe {@link #keeps} from the first input that has it, so a grafted tool keeps its reserve.
     */
    public ItemStack resultFor(List<ItemStack> inputs) {
        ItemStack made = result.copy();
        for (DataComponentType<?> type : keeps)
            for (ItemStack input : inputs)
                if (!input.isEmpty() && input.has(type)) {
                    keep(made, input, type);
                    break;
                }
        if (!keepsWear || !made.isDamageableItem()) return made;
        for (ItemStack input : inputs) {
            if (input.isEmpty() || !input.isDamageableItem()) continue;
            made.setDamageValue((int) ((long) input.getDamageValue() * made.getMaxDamage() / input.getMaxDamage()));
            return made;
        }
        return made;
    }

    private static <T> void keep(ItemStack to, ItemStack from, DataComponentType<T> type) {
        to.set(type, from.get(type));
    }

    /**
     * True when {@code kinds}, one stack per kind of item an organ holds, could all be ingredients of one batch of this
     * recipe at once, each kind its own ingredient. The organ asks before it admits a kind, so no slot ever holds an
     * item no batch can use with the others.
     */
    public boolean fits(List<ItemStack> kinds) {
        if (kinds.size() > ingredients.size()) return false;
        List<ItemStack> units = new ArrayList<>(kinds);
        while (units.size() < ingredients.size()) units.add(ANY_KIND);
        List<Predicate<ItemStack>> tests = new ArrayList<>();
        for (Ingredient ingredient : ingredients) tests.add(stack -> stack == ANY_KIND || ingredient.test(stack));
        return RecipeMatcher.findMatches(units, tests) != null;
    }

    /** A unit that stands for "whatever else the batch needs"; every ingredient accepts it, no kind is ever empty. */
    private static final ItemStack ANY_KIND = ItemStack.EMPTY;

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
