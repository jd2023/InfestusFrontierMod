package org.jd.infestusfrontier.processing;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jd.infestusfrontier.core.foundation.Refusal;
import org.jd.infestusfrontier.processing.api.PalmContents;
import org.jd.infestusfrontier.processing.api.PalmRote;

/**
 * What a Palm's pattern makes, asked of the vanilla crafting recipes (BLOCK_CATALOG T1-41, stage 3), and where those
 * products go in its tray. One recipe lookup per call of {@link #resolve}; the Palm makes it at Select and when a cycle
 * is about to start, never in a quiet tick.
 */
final class PalmPlan {
    static final Refusal NO_RECIPE = new Refusal("processing.palm_select_no_recipe");
    static final Refusal SPECIAL = new Refusal("processing.palm_select_special");
    static final Refusal NO_RESULT = new Refusal("processing.palm_select_no_result");
    static final Refusal TOO_MANY_REMAINDERS = new Refusal("processing.palm_select_remainders");
    static final Refusal BIG_REMAINDER = new Refusal("processing.palm_select_big_remainder");

    private PalmPlan() {}

    /** What the recipe makes for one pattern, or why none is made. */
    sealed interface Plan {
        /** {@code remainders} are merged by kind, at most {@link PalmRote#REMAINDER_SLOTS} of them. */
        record Found(ResourceLocation recipe, ItemStack result, List<ItemStack> remainders) implements Plan {}

        record Refused(Refusal why) implements Plan {}
    }

    /**
     * The recipe the nine {@code kinds} (one item each, or empty) resolve to, the result it assembles from them and the
     * remainders it leaves, merged by kind. Refused for an empty or unmatched grid, a recipe whose result depends on its
     * inputs ({@code isSpecial}), an empty result, more than three kinds of remainder, and a remainder of one kind that
     * would not fit one stack.
     */
    static Plan resolve(Level level, List<ItemStack> kinds) {
        CraftingInput input = CraftingInput.ofPositioned(3, 3, kinds).input();
        if (input.isEmpty()) return new Plan.Refused(NO_RECIPE);
        Optional<RecipeHolder<CraftingRecipe>> found =
                level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level);
        if (found.isEmpty()) return new Plan.Refused(NO_RECIPE);
        CraftingRecipe recipe = found.get().value();
        if (recipe.isSpecial()) return new Plan.Refused(SPECIAL);
        ItemStack result = recipe.assemble(input, level.registryAccess());
        if (result.isEmpty() || result.getCount() > result.getMaxStackSize()) return new Plan.Refused(NO_RESULT);
        List<ItemStack> remainders = new ArrayList<>();
        for (ItemStack left : recipe.getRemainingItems(input)) {
            if (left.isEmpty()) continue;
            ItemStack same = null;
            for (ItemStack kept : remainders) if (ItemStack.isSameItemSameComponents(kept, left)) same = kept;
            if (same != null) same.grow(left.getCount());
            else remainders.add(left.copy());
        }
        if (remainders.size() > PalmRote.REMAINDER_SLOTS) return new Plan.Refused(TOO_MANY_REMAINDERS);
        for (ItemStack kept : remainders)
            if (kept.getCount() > kept.getMaxStackSize()) return new Plan.Refused(BIG_REMAINDER);
        return new Plan.Found(found.get().id(), result, remainders);
    }

    /**
     * The tray after {@code result} and {@code remainders} are put in it, or empty when any of them has no room. The
     * result joins slot 0; each remainder kind joins a remainder slot that holds its kind and has room for all of it,
     * else the first empty remainder slot. {@code tray} is not changed.
     */
    static Optional<NonNullList<ItemStack>> place(List<ItemStack> tray, ItemStack result, List<ItemStack> remainders) {
        NonNullList<ItemStack> after = NonNullList.withSize(PalmRote.TRAY, ItemStack.EMPTY);
        for (int slot = 0; slot < PalmRote.TRAY; slot++)
            after.set(slot, tray.get(slot).copy());
        if (!join(after, 0, 1, result)) return Optional.empty();
        for (ItemStack remainder : remainders) if (!join(after, 1, PalmRote.TRAY, remainder)) return Optional.empty();
        return Optional.of(after);
    }

    /** Puts all of {@code stack} in the first slot of [{@code from}, {@code to}) that holds its kind with room, else the first empty one. */
    private static boolean join(NonNullList<ItemStack> tray, int from, int to, ItemStack stack) {
        if (stack.isEmpty()) return true;
        for (int slot = from; slot < to; slot++) {
            ItemStack held = tray.get(slot);
            if (!held.isEmpty()
                    && ItemStack.isSameItemSameComponents(held, stack)
                    && held.getCount() + stack.getCount() <= held.getMaxStackSize()) {
                held.grow(stack.getCount());
                return true;
            }
        }
        for (int slot = from; slot < to; slot++) {
            if (!tray.get(slot).isEmpty()) continue;
            tray.set(slot, stack.copy());
            return true;
        }
        return false;
    }

    /** True when every cell of {@code grid} holds its pattern kind, and the cells the pattern leaves empty are empty. */
    static boolean matches(List<ItemStack> kinds, List<ItemStack> grid) {
        for (int cell = 0; cell < PalmContents.CELLS; cell++) {
            ItemStack kind = kinds.get(cell);
            ItemStack held = grid.get(cell);
            if (kind.isEmpty() ? !held.isEmpty() : !ItemStack.isSameItemSameComponents(kind, held)) return false;
        }
        return true;
    }
}
