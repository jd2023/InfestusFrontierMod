package org.jd.infestusfrontier.processing.digestion;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;

/** One unstackable recovered core carries the organ's block-entity data. */
final class StoredOrganItem extends BlockItem {
    StoredOrganItem(Block block, Properties properties) { super(block, properties.stacksTo(1)); }
}
