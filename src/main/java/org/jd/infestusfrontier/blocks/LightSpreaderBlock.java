package org.jd.infestusfrontier.blocks;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import org.slf4j.Logger;

public class LightSpreaderBlock extends Block{

    private static final Logger LOGGER = LogUtils.getLogger();
    public LightSpreaderBlock() {
        super(Properties.of(Material.STONE)
                .strength(1.5f, 6.0f)
                .lightLevel(state -> 15)
        );
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(world, pos, state, placer, stack);

        LOGGER.info("Placed LightSpreaderBlock at {}", pos);

        if (!world.isClientSide) {
            // Coordinates of the block below
            BlockPos infectionPos = pos.below().east(5).south(5);
            for (int i = 0; i < 10; i++) {
                var nextPos = infectionPos.north();
                for (int j = 0; j < 10; j++) {
                    world.setBlockAndUpdate(infectionPos, Blocks.SLIME_BLOCK.defaultBlockState());
                    infectionPos = infectionPos.west();
                }
                infectionPos = nextPos;
            }

            var blockBelow = pos.below();
            // Example: Converting adjacent blocks
            BlockPos[] adjacentPositions = new BlockPos[]{
                    blockBelow, blockBelow.north(), blockBelow.east(), blockBelow.south(), blockBelow.west()
            };

            for (BlockPos adjacentPos : adjacentPositions) {
                // Check if conversion condition is met, then convert
                world.setBlockAndUpdate(adjacentPos, Blocks.OBSIDIAN.defaultBlockState());
            }
        }
    }
}
