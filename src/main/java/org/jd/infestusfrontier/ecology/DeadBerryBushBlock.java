package org.jd.infestusfrontier.ecology;

import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A sweet berry bush killed by substrate growth under it. Its one property is {@link #AGE}, the age the bush had,
 * and breaking it rolls the living bush's loot for that age once ({@code loot_table/blocks/dead_berry_bush.json}).
 * It cannot be picked, takes no random ticks, never grows, hurts nothing that walks through it and answers no bone
 * meal; a break is its only harvest.
 */
public final class DeadBerryBushBlock extends DeadShrubBlock {
    public static final IntegerProperty AGE = SweetBerryBushBlock.AGE;
    /** Ash, bone-brown, brown and plum: the tints a Dead Berry Bush is drawn in. */
    static final int[] TINTS = {0x9C9890, 0x6E5A48, 0x805E3E, 0x764C62};

    private static final MapCodec<DeadBerryBushBlock> CODEC = simpleCodec(DeadBerryBushBlock::new);
    private static final VoxelShape SMALL = Block.box(3.0, 0.0, 3.0, 13.0, 8.0, 13.0);
    private static final VoxelShape LARGE = Block.box(1.0, 0.0, 1.0, 15.0, 14.0, 15.0);

    DeadBerryBushBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(AGE, 0));
    }

    /** The dead form of a sweet berry bush at its present age; empty for every other block. */
    static Optional<BlockState> wither(BlockState plant) {
        if (!plant.is(Blocks.SWEET_BERRY_BUSH)) return Optional.empty();
        return Optional.of(EcologyModule.DEAD_BERRY_BUSH
                .get()
                .defaultBlockState()
                .setValue(AGE, plant.getValue(SweetBerryBushBlock.AGE)));
    }

    /** The tint a bush at {@code pos} is drawn in: one of {@link #TINTS}. */
    public static int tintAt(BlockPos pos) {
        return TINTS[tintIndexAt(pos, TINTS.length)];
    }

    @Override
    protected MapCodec<DeadBerryBushBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(AGE) == 0 ? SMALL : LARGE;
    }

    /** Pick block gives berries, as the living bush does. */
    @Override
    ItemStack plantStack(BlockState state) {
        return new ItemStack(Items.SWEET_BERRIES);
    }
}
