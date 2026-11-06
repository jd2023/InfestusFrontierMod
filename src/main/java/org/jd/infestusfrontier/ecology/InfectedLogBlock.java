package org.jd.infestusfrontier.ecology;

import com.mojang.serialization.MapCodec;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The trunk of every infected tree, whatever tree it was: a vanilla pillar with only its {@code axis}. Breaking it
 * drops one Infected Log ({@code loot_table/blocks/infected_log.json}), and the item carries nothing, so cutting and
 * replacing a log never multiplies wood. It sits in the {@code logs} block tag, which is how vanilla's
 * {@code LeavesBlock} counts a log as distance 0 and keeps Blighted Leaves alive. It has no block entity, tick, resin or
 * production. A state saved with the {@code source} property of older versions reads as this block with its axis.
 *
 * <p>Its collision and outline are the full block. Its models ({@code art/gen_infected_timber.py}) cut bark plates,
 * clefts and a rib into the sides, so it hides a neighbour's face only where its section reaches the block's face:
 * {@link #OCCLUSION}, the same section. It still stops all light, like any log.
 */
public final class InfectedLogBlock extends RotatedPillarBlock {
    /**
     * How deep each column of a side is cut, in pixels, read along the north side from west to east: the generator's
     * {@code PROFILE}. The other sides are the same profile turned a quarter clockwise, seen from above.
     */
    static final int[] PROFILE = {2, 2, 1, 1, 0, 0, 1, 2, 4, 4, 4, 2, 1, 1, 2, 2};

    /** The section the models draw, extruded along each axis: what a neighbour's face is hidden by. */
    public static final Map<Direction.Axis, VoxelShape> OCCLUSION = occlusion();

    private static final MapCodec<InfectedLogBlock> CODEC = simpleCodec(InfectedLogBlock::new);

    InfectedLogBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<InfectedLogBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return OCCLUSION.get(state.getValue(AXIS));
    }

    /** All light stops, as at any log: the cut section is not a full occluder, which alone would let light through. */
    @Override
    protected int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return level.getMaxLightLevel();
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 5;
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 5;
    }

    /** Whether the section holds wood at column {@code x}, row {@code z} of the upright log, seen from above. */
    static boolean wood(int x, int z) {
        for (int side = 0; side < 4; side++) {
            int column = x;
            int depth = z;
            for (int turn = 0; turn < side; turn++) { // turn the cell back onto the north side
                int next = depth;
                depth = 15 - column;
                column = next;
            }
            if (depth < PROFILE[column]) return false;
        }
        return true;
    }

    private static Map<Direction.Axis, VoxelShape> occlusion() {
        Map<Direction.Axis, VoxelShape> shapes = new EnumMap<>(Direction.Axis.class);
        for (Direction.Axis axis : Direction.Axis.values()) shapes.put(axis, Shapes.empty());
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                if (!wood(x, z)) continue;
                // The blockstate lays the upright model down by x 90 (axis z) or x 90 then y 90 (axis x).
                shapes.merge(Direction.Axis.Y, Block.box(x, 0, z, x + 1, 16, z + 1), Shapes::or);
                shapes.merge(Direction.Axis.Z, Block.box(x, z, 0, x + 1, z + 1, 16), Shapes::or);
                shapes.merge(Direction.Axis.X, Block.box(0, z, x, 16, z + 1, x + 1), Shapes::or);
            }
        }
        shapes.replaceAll((axis, shape) -> shape.optimize());
        return Map.copyOf(shapes);
    }
}
