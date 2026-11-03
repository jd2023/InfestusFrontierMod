package org.jd.infestusfrontier.ecology;

import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A small flower killed by substrate growth under it. Inert: no block entity, no ticks, no growth, no bone meal
 * response. Its one property is {@link #SOURCE}, the flower it was, and breaking it rolls that flower's own loot
 * table once ({@code loot_table/blocks/withered_flower.json}).
 *
 * <p>Appearance is never saved. The blockstate holds the family's 48 looks, three silhouettes turned four ways in
 * four tints, and vanilla picks one by the position's seed, so a flower looks the same after every reload and the
 * same on both sides of a chunk border. The tint is {@link #tintAt}, from the same seed, applied by the module's
 * block colour.
 */
public final class WitheredFlowerBlock extends BushBlock {
    public static final EnumProperty<FlowerSource> SOURCE = EnumProperty.create("source", FlowerSource.class);
    /** Muted ash, bone, brown and plum: the tints a Withered Flower is drawn in. */
    static final int[] TINTS = {0x9C9890, 0xD6CCB2, 0x805E3E, 0x764C62};

    private static final MapCodec<WitheredFlowerBlock> CODEC = simpleCodec(WitheredFlowerBlock::new);
    private static final VoxelShape SHAPE = Block.box(5.0, 0.0, 5.0, 11.0, 10.0, 11.0);
    private static final int FLAMMABILITY = 100;
    private static final int FIRE_SPREAD_SPEED = 60;

    WitheredFlowerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(SOURCE, FlowerSource.DANDELION));
    }

    /** The dead form of {@code plant}; empty for every block that is not a supported flower. */
    public static Optional<BlockState> wither(BlockState plant) {
        return FlowerSource.of(plant.getBlock())
                .map(source ->
                        EcologyModule.WITHERED_FLOWER.get().defaultBlockState().setValue(SOURCE, source));
    }

    /** The tint a flower at {@code pos} is drawn in: one of {@link #TINTS}, chosen by the position's seed. */
    public static int tintAt(BlockPos pos) {
        return TINTS[(int) ((Mth.getSeed(pos) >>> 23) & 3)];
    }

    @Override
    protected MapCodec<WitheredFlowerBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SOURCE);
    }

    /** Stands where its flower stood and on the cell that replaced that ground. */
    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return super.mayPlaceOn(state, level, pos) || state.is(EcologyModule.LIVING_SUBSTRATE.get());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Vec3 offset = state.getOffset(level, pos);
        return SHAPE.move(offset.x, offset.y, offset.z);
    }

    /** Pick block gives the flower it was; there is no Withered Flower item. */
    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(state.getValue(SOURCE).flower());
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return FLAMMABILITY;
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return FIRE_SPREAD_SPEED;
    }
}
