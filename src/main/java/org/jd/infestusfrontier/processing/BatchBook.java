package org.jd.infestusfrontier.processing;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.core.organ.Growth;
import org.jd.infestusfrontier.core.organ.OrganHistory;
import org.jd.infestusfrontier.processing.api.BatchOrganContents;
import org.jd.infestusfrontier.processing.api.OrganBatchRecipe;

/**
 * Where a batch organ's batches come from: the mod's own {@code organ_batch} recipes for most kinds, the vanilla
 * smelting book for the Bio-Furnace. A book is read on a change, never in a quiet tick.
 */
interface BatchBook {
    /**
     * Whether a slot may take {@code stack} while the other slots hold {@code others}: a kind of item no other slot
     * holds, and one that some batch can use together with them. A hopper that offers one ingredient fills one slot
     * and is then refused, so it offers the next; no slot ever holds what no batch can use.
     */
    boolean takes(Level level, List<ItemStack> others, ItemStack stack);

    /** The batches the slots could start now, the preferred one first. Water, biomass and output room are judged by the organ. */
    List<Plan> plans(Level level, List<ItemStack> slots, OrganHistory history);

    /**
     * One batch about to start: what each slot gives, what the batch costs, what it makes after how many ticks, and the
     * smelting experience it earns for whoever takes the result.
     */
    record Plan(String id, int[] take, int waterMb, Biomass biomass, ItemStack result, int ticks, float experience) {}

    /** The items {@code take} says to take from {@code slots}, as stacks of that count. */
    static List<ItemStack> taken(List<ItemStack> slots, int[] take) {
        List<ItemStack> taken = new ArrayList<>();
        for (int slot = 0; slot < slots.size(); slot++)
            if (take[slot] > 0) taken.add(slots.get(slot).copyWithCount(take[slot]));
        return taken;
    }

    /** The organ's own {@code organ_batch} recipes; a batch of the recipe with most ingredients wins when two fit. */
    record Data(String organ) implements BatchBook {
        private Stream<RecipeHolder<OrganBatchRecipe>> recipes(Level level) {
            return level.getRecipeManager().getAllRecipesFor(ProcessingModule.ORGAN_BATCH.get()).stream()
                    .filter(recipe -> recipe.value().organ().equals(organ));
        }

        private List<RecipeHolder<OrganBatchRecipe>> orderedRecipes(Level level) {
            return recipes(level)
                    .sorted(Comparator.<RecipeHolder<OrganBatchRecipe>>comparingInt(
                                    recipe -> -recipe.value().ingredients().size())
                            .thenComparing(recipe -> recipe.id().toString()))
                    .toList();
        }

        @Override
        public boolean takes(Level level, List<ItemStack> others, ItemStack stack) {
            for (ItemStack other : others) if (ItemStack.isSameItemSameComponents(other, stack)) return false;
            List<ItemStack> kinds = new ArrayList<>(others);
            kinds.add(stack);
            return recipes(level).anyMatch(recipe -> recipe.value().fits(kinds));
        }

        @Override
        public List<Plan> plans(Level level, List<ItemStack> slots, OrganHistory history) {
            List<Plan> plans = new ArrayList<>();
            for (RecipeHolder<OrganBatchRecipe> holder : orderedRecipes(level)) {
                OrganBatchRecipe recipe = holder.value();
                Optional<int[]> take = recipe.plan(slots);
                if (take.isEmpty()) continue;
                plans.add(new Plan(
                        holder.id().toString(),
                        take.get(),
                        recipe.waterMb(),
                        recipe.biomass(),
                        recipe.resultFor(taken(slots, take.get())),
                        recipe.ticks(),
                        0));
            }
            return plans;
        }
    }

    /**
     * The vanilla smelting book: one item in the first slot becomes its furnace result, for {@code biomass} and the
     * ticks of the organ's growth stage, earning the recipe's experience.
     */
    record Smelting(Growth growth, List<Integer> ticksByStage, Biomass biomass) implements BatchBook {
        public Smelting {
            if (ticksByStage.size() != growth.stages())
                throw new IllegalArgumentException(
                        ticksByStage.size() + " tick counts for " + growth.stages() + " stages");
        }

        private Optional<RecipeHolder<SmeltingRecipe>> recipe(Level level, ItemStack stack) {
            if (stack.isEmpty()) return Optional.empty();
            return level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(stack), level);
        }

        @Override
        public boolean takes(Level level, List<ItemStack> others, ItemStack stack) {
            return others.isEmpty() && recipe(level, stack).isPresent();
        }

        @Override
        public List<Plan> plans(Level level, List<ItemStack> slots, OrganHistory history) {
            ItemStack stack = slots.get(0);
            Optional<RecipeHolder<SmeltingRecipe>> found = recipe(level, stack);
            if (found.isEmpty()) return List.of();
            int[] take = new int[slots.size()];
            take[0] = 1;
            return List.of(new Plan(
                    found.get().id().toString(),
                    take,
                    0,
                    biomass,
                    found.get().value().assemble(new SingleRecipeInput(stack), level.registryAccess()),
                    ticksByStage.get(growth.stage(history)),
                    // a data pack may write any number; the organ keeps at most MAX_EXPERIENCE and never a debt
                    Math.clamp(found.get().value().getExperience(), 0, BatchOrganContents.MAX_EXPERIENCE)));
        }
    }
}
