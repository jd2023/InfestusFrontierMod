package org.jd.infestusfrontier.processing;

import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jd.infestusfrontier.InfestusFrontier;
import org.jd.infestusfrontier.core.foundation.Biomass;

/**
 * What makes one batch organ differ from another: its recipes' organ id, how many ingredient slots it has, how much
 * water and biomass it holds, how tall its screen is and its shape. The Bowl, the Rack and the Loom are kinds; the
 * block, block entity and menu are shared.
 */
record BatchOrganKind(
        String id,
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

    String nameKey() {
        return "block." + InfestusFrontier.MOD_ID + "." + id;
    }
}
