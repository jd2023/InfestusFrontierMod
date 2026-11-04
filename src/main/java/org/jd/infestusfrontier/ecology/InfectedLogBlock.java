package org.jd.infestusfrontier.ecology;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * The trunk of an infected tree. A vanilla pillar with one more property, {@link #SOURCE}, the species it was: it only
 * picks the bark tone. Breaking it drops one Infected Log whatever the species ({@code loot_table/blocks/infected_log.json}),
 * and the item carries nothing, so cutting and replacing a log never multiplies wood. It sits in the {@code logs} block
 * tag, which is how vanilla's {@code LeavesBlock} counts a log as distance 0 and keeps Blighted Leaves alive. It has no
 * block entity, tick, resin or production.
 */
public final class InfectedLogBlock extends RotatedPillarBlock {
    public static final EnumProperty<TreeSource> SOURCE = EnumProperty.create("source", TreeSource.class);

    private static final MapCodec<InfectedLogBlock> CODEC = simpleCodec(InfectedLogBlock::new);

    InfectedLogBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(SOURCE, TreeSource.OAK));
    }

    @Override
    public MapCodec<InfectedLogBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(SOURCE);
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 5;
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 5;
    }
}
