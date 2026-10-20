package org.jd.infestusfrontier.processing;

import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jd.infestusfrontier.InfestusFrontier;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.core.organ.Growth;

/**
 * What makes one batch organ differ from another: where its batches come from, how it grows, how many ingredient
 * slots it has, how much water and biomass it holds, how tall its screen is and its shape. The Bowl, the Rack, the
 * Loom, the Cradle and the Bio-Furnace are kinds; the block, block entity and menu are shared.
 */
record BatchOrganKind(
        String id,
        BatchBook book,
        Growth growth,
        int slots,
        int waterMb,
        Biomass biomassCapacity,
        int strips,
        VoxelShape shape,
        Supplier<Block> block,
        Supplier<BlockEntityType<BatchOrganBlockEntity>> entity,
        Supplier<MenuType<BatchOrganMenu>> menu) {
    ResourceLocation blockId() {
        return ResourceLocation.fromNamespaceAndPath(InfestusFrontier.MOD_ID, id);
    }

    /** Whether a screen draws the biomass tank, which shortens the parts row. */
    boolean holdsBiomass() {
        return !biomassCapacity.isZero();
    }

    /** Whether the organ has more than one stage, and so a {@code stage} block state and a grown look. */
    boolean grows() {
        return growth.stages() > 1;
    }

    String nameKey() {
        return "block." + InfestusFrontier.MOD_ID + "." + id;
    }
}
