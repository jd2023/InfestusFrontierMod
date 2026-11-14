package org.jd.infestusfrontier.organ;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import org.jd.infestusfrontier.foundation.Persistence;
import org.jd.infestusfrontier.organ.api.OrganGrowthRecipe;
import org.slf4j.Logger;

/**
 * What an Organ Bud has been given: the organ it was told to grow and the materials supplied so far, as the stacks the
 * player handed over. Nothing ticks. The materials leave only by growth, which takes what the growth needs and gives
 * back the rest, or by breaking, which gives back all of it.
 */
final class OrganBudBlockEntity extends BlockEntity {
    private static final Logger LOGGER = LogUtils.getLogger();

    private record Saved(Optional<ResourceLocation> choice, List<ItemStack> supplied) {}

    private static final MapCodec<Saved> SAVED = RecordCodecBuilder.mapCodec(instance -> instance.group(
                    ResourceLocation.CODEC.optionalFieldOf("organ").forGetter(Saved::choice),
                    ItemStack.CODEC
                            .listOf(0, OrganGrowthRecipe.MAX_ITEMS)
                            .optionalFieldOf("supplied", List.of())
                            .forGetter(Saved::supplied))
            .apply(instance, Saved::new));

    private Optional<ResourceLocation> choice = Optional.empty();
    private final List<ItemStack> supplied = new ArrayList<>();
    private Optional<CompoundTag> unreadable = Optional.empty();

    OrganBudBlockEntity(BlockPos pos, BlockState state) {
        super(OrganModule.ORGAN_BUD_ENTITY.get(), pos, state);
    }

    /** True when saved data could not be read; the bud keeps it unchanged and refuses every command. */
    boolean unreadable() {
        return unreadable.isPresent();
    }

    Optional<ResourceLocation> choice() {
        return choice;
    }

    /** True once anything has been supplied: the choice is then fixed until the bud grows or breaks. */
    boolean started() {
        return !supplied.isEmpty();
    }

    /** The growth this bud was told to grow, if the loaded recipes still have it. */
    Optional<RecipeHolder<OrganGrowthRecipe>> chosen(Level level) {
        if (choice.isEmpty()) return Optional.empty();
        return OrganGrowthRecipe.all(level).stream()
                .filter(holder -> holder.value().organ().equals(choice.get()))
                .findFirst();
    }

    /** Chooses {@code organ}; refused once materials were supplied. */
    boolean choose(ResourceLocation organ) {
        if (unreadable() || started()) return false;
        choice = Optional.of(organ);
        setChanged();
        return true;
    }

    /** How much of each material of {@code recipe} is still missing, in the recipe's order. */
    int[] missing(OrganGrowthRecipe recipe) {
        return allocate(recipe, copies());
    }

    /**
     * The one matching rule, used both to judge a growth complete and to decide what it consumes. Takes from
     * {@code pool}, material by material in the recipe's order, the stacks each material's ingredient accepts, so
     * what is left in {@code pool} is what the growth does not use. Returns how much of each material was not found.
     */
    private static int[] allocate(OrganGrowthRecipe recipe, List<ItemStack> pool) {
        int[] left = new int[recipe.materials().size()];
        for (int i = 0; i < left.length; i++) {
            SizedIngredient material = recipe.materials().get(i);
            left[i] = material.count();
            for (ItemStack stack : pool) {
                if (left[i] == 0) break;
                if (stack.isEmpty() || !material.ingredient().test(stack)) continue;
                int take = Math.min(left[i], stack.getCount());
                stack.shrink(take);
                left[i] -= take;
            }
        }
        return left;
    }

    /** True when every material of {@code recipe} has been supplied. */
    boolean complete(OrganGrowthRecipe recipe) {
        for (int left : missing(recipe)) if (left > 0) return false;
        return true;
    }

    /**
     * Moves what {@code recipe} still lacks of {@code held}'s kind from {@code held} into the bud, and returns how many
     * items moved. Nothing moves when no material still missing takes {@code held}.
     */
    int supply(OrganGrowthRecipe recipe, ItemStack held) {
        if (unreadable() || held.isEmpty()) return 0;
        int moved = 0;
        for (int i = 0; i < recipe.materials().size() && !held.isEmpty(); i++) {
            SizedIngredient material = recipe.materials().get(i);
            if (!material.ingredient().test(held)) continue;
            int take = Math.min(missing(recipe)[i], held.getCount());
            if (take == 0) continue;
            add(held.copyWithCount(take));
            held.shrink(take);
            moved += take;
        }
        if (moved > 0) setChanged();
        return moved;
    }

    /** Merges {@code stack} into the supplied stacks without exceeding any stack's size. */
    private void add(ItemStack stack) {
        for (ItemStack have : supplied) {
            if (!ItemStack.isSameItemSameComponents(have, stack)) continue;
            int room = have.getMaxStackSize() - have.getCount();
            int moved = Math.min(room, stack.getCount());
            have.grow(moved);
            stack.shrink(moved);
            if (stack.isEmpty()) return;
        }
        supplied.add(stack.copy());
    }

    /**
     * Empties the bud of its materials and returns what {@code recipe} did not use, for the caller to give back. The
     * caller grows the organ in the same step; {@link #restore} undoes this if it cannot.
     */
    Taken takeForGrowth(OrganGrowthRecipe recipe) {
        List<ItemStack> before = copies();
        List<ItemStack> pool = copies();
        allocate(recipe, pool);
        supplied.clear();
        setChanged();
        return new Taken(before, pool.stream().filter(stack -> !stack.isEmpty()).toList());
    }

    /** The materials {@link #takeForGrowth} took, and the part of them the growth did not use. */
    record Taken(List<ItemStack> all, List<ItemStack> unused) {}

    /** Puts back what {@link #takeForGrowth} took, when the organ could not grow after all. */
    void restore(Taken taken) {
        supplied.clear();
        supplied.addAll(taken.all());
        setChanged();
    }

    /** Empties the bud and returns everything it held, for the caller to give back. */
    List<ItemStack> emptied() {
        List<ItemStack> all = copies();
        supplied.clear();
        setChanged();
        return all;
    }

    private List<ItemStack> copies() {
        return new ArrayList<>(supplied.stream().map(ItemStack::copy).toList());
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (unreadable.isPresent()) {
            tag.merge(unreadable.get());
            return;
        }
        Persistence.write(SAVED, new Saved(choice, copies()), registries, tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        choice = Optional.empty();
        supplied.clear();
        unreadable = Optional.empty();
        Optional<Saved> saved = Persistence.read(SAVED, tag, registries, LOGGER, "Organ Bud at " + worldPosition);
        if (saved.isPresent()) {
            choice = saved.get().choice();
            supplied.addAll(saved.get().supplied().stream().map(ItemStack::copy).toList());
        } else {
            unreadable = Optional.of(tag.copy());
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        unreadable.ifPresent(tag -> components.set(OrganModule.UNREADABLE_SAVE.get(), tag.copy()));
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        CompoundTag kept = input.get(OrganModule.UNREADABLE_SAVE.get());
        if (kept != null) unreadable = Optional.of(kept.copy());
    }
}
