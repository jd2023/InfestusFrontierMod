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
import org.jd.infestusfrontier.processing.api.BatchOrganContents;

/**
 * What makes one batch organ differ from another: where its batches come from, how it grows, how many ingredient
 * slots and output slots it has, how much water, dirty water and biomass it holds, how tall its screen is, its shape and how it shows its work. The Bowl, the Rack, the
 * Loom, the Cradle, the Bio-Furnace and the Cyst are kinds; the block, block entity and menu are shared.
 */
record BatchOrganKind(
        String id,
        BatchBook book,
        Growth growth,
        int slots,
        int outputs,
        int waterMb,
        int dirtyMb,
        Biomass biomassCapacity,
        int strips,
        VoxelShape shape,
        OrganEffects effects,
        Supplier<Block> block,
        Supplier<BlockEntityType<BatchOrganBlockEntity>> entity,
        Supplier<MenuType<BatchOrganMenu>> menu) {
    BatchOrganKind {
        if (outputs < 1 || outputs > BatchOrganContents.MAX_OUTPUTS)
            throw new IllegalArgumentException(id + " has " + outputs + " output slots");
        if (dirtyMb < 0 || dirtyMb > BatchOrganContents.MAX_DIRTY)
            throw new IllegalArgumentException(id + " holds " + dirtyMb + " mB of dirty water");
    }

    /** A kind with several output slots and no dirty water. */
    BatchOrganKind(
            String id,
            BatchBook book,
            Growth growth,
            int slots,
            int outputs,
            int waterMb,
            Biomass biomassCapacity,
            int strips,
            VoxelShape shape,
            OrganEffects effects,
            Supplier<Block> block,
            Supplier<BlockEntityType<BatchOrganBlockEntity>> entity,
            Supplier<MenuType<BatchOrganMenu>> menu) {
        this(
                id,
                book,
                growth,
                slots,
                outputs,
                waterMb,
                0,
                biomassCapacity,
                strips,
                shape,
                effects,
                block,
                entity,
                menu);
    }

    /** A kind with one output slot. */
    BatchOrganKind(
            String id,
            BatchBook book,
            Growth growth,
            int slots,
            int waterMb,
            Biomass biomassCapacity,
            int strips,
            VoxelShape shape,
            OrganEffects effects,
            Supplier<Block> block,
            Supplier<BlockEntityType<BatchOrganBlockEntity>> entity,
            Supplier<MenuType<BatchOrganMenu>> menu) {
        this(id, book, growth, slots, 1, waterMb, biomassCapacity, strips, shape, effects, block, entity, menu);
    }

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
