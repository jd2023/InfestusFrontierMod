package org.jd.infestusfrontier.cultivation.api;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jd.infestusfrontier.InfestusFrontier;

/** What counts as planting stock: the item tag {@code infestusfrontier:planting_stock}, seeds, saplings and tubers. */
public final class PlantingStock {
    public static final TagKey<Item> TAG = TagKey.create(
            Registries.ITEM, ResourceLocation.fromNamespaceAndPath(InfestusFrontier.MOD_ID, "planting_stock"));

    private PlantingStock() {}

    public static boolean is(ItemStack stack) {
        return stack.is(TAG) && stack.getItem() instanceof BlockItem;
    }

    /** The plant one item of {@code stack} puts in the world. */
    public static BlockState plantOf(ItemStack stack) {
        return ((BlockItem) stack.getItem()).getBlock().defaultBlockState();
    }
}
