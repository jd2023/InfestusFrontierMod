package org.jd.infestusfrontier.storage.api;

import net.minecraft.world.item.ItemStack;

/**
 * An item handler that tells what a Mouth puts in from what a hopper or a pipe does. {@link ItemStores#insert}, the
 * rule every Mouth fills a partner by, calls {@link #deliver}; the handler's own {@code insertItem} stays the
 * hopper's. A Shaping Palm counts what arrives here as items fetched from the network.
 */
public interface MouthFed {
    /** Puts {@code stack} where the handler's rules put it and returns what did not fit. {@code stack} is unchanged. */
    ItemStack deliver(ItemStack stack, boolean simulate);
}
