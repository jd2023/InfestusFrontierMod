package org.jd.infestusfrontier.discovery;

import com.klikli_dev.modonomicon.registry.DataComponentRegistry;
import com.klikli_dev.modonomicon.registry.ItemRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Creates and identifies the one physical representation of The Waking Genome. */
public final class GuideAccess {
    public static final ResourceLocation BOOK_ID = ResourceLocation.fromNamespaceAndPath(
            "infestusfrontier", "waking_genome");

    public static ItemStack wakingGenome() {
        var stack = new ItemStack(ItemRegistry.MODONOMICON.get());
        stack.set(DataComponentRegistry.BOOK_ID.get(), BOOK_ID);
        return stack;
    }

    public static boolean isWakingGenome(ItemStack stack) {
        return stack.is(ItemRegistry.MODONOMICON.get())
                && BOOK_ID.equals(stack.get(DataComponentRegistry.BOOK_ID.get()));
    }

    private GuideAccess() {}
}
