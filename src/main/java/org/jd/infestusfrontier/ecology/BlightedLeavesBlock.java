package org.jd.infestusfrontier.ecology;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * The crown of an infected tree: vanilla's {@link LeavesBlock} with one more property, {@link #SOURCE}, the species it
 * was. Distance, persistence, waterlogging, support by logs, decay and the shears and Silk Touch rules are vanilla's,
 * unchanged. Decay and breaking roll {@code loot_table/blocks/blighted_leaves.json}, which nests the source leaf's own
 * table, so saplings, sticks, apples and the tool rules carry over. There is no item: pick block gives the living
 * leaves. No block entity, no production, no regrowth.
 */
public final class BlightedLeavesBlock extends LeavesBlock {
    public static final EnumProperty<TreeSource> SOURCE = EnumProperty.create("source", TreeSource.class);

    private static final MapCodec<BlightedLeavesBlock> CODEC = simpleCodec(BlightedLeavesBlock::new);

    BlightedLeavesBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(SOURCE, TreeSource.OAK));
    }

    @Override
    public MapCodec<BlightedLeavesBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(SOURCE);
    }

    /** Pick block gives the living leaves it was; there is no blighted leaf item. */
    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(state.getValue(SOURCE).leaves());
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 60;
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 30;
    }
}
