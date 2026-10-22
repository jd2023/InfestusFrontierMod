package org.jd.infestusfrontier.storage.api;

import net.minecraft.world.item.ItemStack;

/**
 * An item store the colony knows natively, beside its standard item handler: whole counts and one insertion for the
 * whole store, so a reader never walks its slots. A Mouth reads an Item Capsule's counts through it.
 */
public interface ItemStore {
    /** How many items of {@code like}'s kind, the same item with the same components, the store holds. */
    int count(ItemStack like);

    /** Puts {@code stack} where the store's own rules put it and returns what did not fit. {@code stack} is unchanged. */
    ItemStack insert(ItemStack stack, boolean simulate);

    /** Items held, for a fullness reading. */
    long stored();

    /** Items the store could hold with the kinds it holds now, for a fullness reading; 0 while it has no slots. */
    long capacity();
}
