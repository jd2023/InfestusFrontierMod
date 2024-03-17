package org.jd.infestusfrontier.block.entity.util;

import net.minecraft.world.Container;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChangeOverTimeBlock;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public interface InfestusLootLeech extends Container {
    VoxelShape SUCK = Block.box(-48.0D, 96.0D, -48.0D, 104.0D, 96.0D, 104.0D);

    default VoxelShape getSuckShape() {
        return SUCK;
    }

    double getLevelX();

    double getLevelY();

    double getLevelZ();
}
