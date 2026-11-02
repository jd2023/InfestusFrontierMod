package org.jd.infestusfrontier.processing;

import java.util.Optional;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.CommonHooks;
import org.jd.infestusfrontier.core.foundation.Refusal;

/**
 * The Palm's two touches on the crafting table's own rules. {@link #update} is the table's result lookup, which the
 * engine keeps in a protected static method of {@link CraftingMenu}; this class extends it only to call that method,
 * and is never instantiated. {@link #refusal} asks the table's own remainder rule, on a copy of the player's
 * inventory, whether a craft would leave something nowhere to go; the engine itself would drop it on the ground.
 */
final class PalmCrafting extends CraftingMenu {
    /** The craft would leave a result or a remainder with no room: a full inventory, or a grid cell it cannot join. */
    static final Refusal NO_ROOM = new Refusal("processing.palm_no_room");

    private PalmCrafting() {
        super(0, (Inventory) null);
    }

    /** Looks up the recipe for {@code grid} and puts its result in slot 0 of {@code menu}, as a crafting table does. */
    static void update(
            AbstractContainerMenu menu, Level level, Player player, CraftingContainer grid, ResultContainer results) {
        slotChangedCraftingGrid(menu, level, player, grid, results, null);
    }

    /**
     * Why taking the current result would leave something with nowhere to go, or empty when it fits. A remainder goes
     * to its emptied cell, joins an equal stack there, or goes to the player's inventory; {@code resultToInventory}
     * adds the result itself to the inventory, for a shift-click.
     */
    static Optional<Refusal> refusal(
            Player player, CraftingContainer grid, ItemStack result, boolean resultToInventory) {
        Level level = player.level();
        CraftingInput.Positioned positioned = grid.asPositionedCraftInput();
        CraftingInput input = positioned.input();
        CommonHooks.setCraftingPlayer(player);
        NonNullList<ItemStack> remainders =
                level.getRecipeManager().getRemainingItemsFor(RecipeType.CRAFTING, input, level);
        CommonHooks.setCraftingPlayer(null);
        Inventory scratch = new Inventory(player);
        for (int slot = 0; slot < scratch.getContainerSize(); slot++)
            scratch.setItem(slot, player.getInventory().getItem(slot).copy());
        if (resultToInventory && !fits(scratch, result)) return Optional.of(NO_ROOM);
        for (int row = 0; row < input.height(); row++) {
            for (int column = 0; column < input.width(); column++) {
                int cell = column + positioned.left() + (row + positioned.top()) * grid.getWidth();
                ItemStack remainder =
                        remainders.get(column + row * input.width()).copy();
                if (remainder.isEmpty()) continue;
                ItemStack left = grid.getItem(cell).copy();
                left.shrink(1);
                if (left.isEmpty()) continue;
                if (ItemStack.isSameItemSameComponents(left, remainder)) {
                    if (left.getCount() + remainder.getCount() > remainder.getMaxStackSize())
                        return Optional.of(NO_ROOM);
                } else if (!fits(scratch, remainder)) return Optional.of(NO_ROOM);
            }
        }
        return Optional.empty();
    }

    /** Whether the whole of {@code stack} goes into {@code inventory}: its {@code add} also answers true for a part. */
    private static boolean fits(Inventory inventory, ItemStack stack) {
        ItemStack rest = stack.copy();
        return inventory.add(rest) && rest.isEmpty();
    }
}
