package org.jd.infestusfrontier.ecology;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.jd.infestusfrontier.InfestusFrontier;

/**
 * Living ground the player grows on purpose. No block entity, no random ticks, no spreading on its own.
 *
 * <p>Bone meal gives a cell {@value #BONE_MEAL_VIGOR} vigor and starts one bounded wave of scheduled ticks, one every
 * {@value #GROWTH_DELAY_TICKS} ticks. Young tissue is the growing edge: a basic cell becomes young and converts up to
 * {@value #SPREAD_PER_CELL} exposed host blocks beside it into basic cells with one vigor less; a young cell with two
 * or more vigor becomes mature; a mature cell that was fed converts hosts the same way. Then the cell goes dormant.
 * Vigor 0 cells do nothing. One bone meal on basic ground therefore reaches at most 15 cells, 3 mature in the core,
 * 4 young around them and 8 basic at the edge, schedules at most 10 ticks and is quiet after 90 ticks.
 *
 * <p>Each stage is drawn from sixteen 64x64 tiles cut out of one 4x4-block macro texture. {@link #TILE} follows the
 * cell's position ({@link #tileAt}), so a patch shows the macro pattern without seams or repeats.
 */
public final class LivingSubstrateBlock extends Block implements BonemealableBlock {
    public static final EnumProperty<SubstrateStage> STAGE = EnumProperty.create("stage", SubstrateStage.class);
    /** Growth left in this cell. 0 is dormant. Bone meal sets {@value #BONE_MEAL_VIGOR}; each spread hands on one less. */
    public static final IntegerProperty VIGOR = IntegerProperty.create("vigor", 0, 3);
    /** Which of the sixteen macro tiles this cell draws. Always {@link #tileAt} of its position. */
    public static final IntegerProperty TILE = IntegerProperty.create("tile", 0, 15);
    /** Ground that Spore Culture converts and a mature cell can spread onto. */
    public static final TagKey<Block> HOSTS = TagKey.create(
            Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(InfestusFrontier.MOD_ID, "substrate_hosts"));

    public static final int BONE_MEAL_VIGOR = 3;
    public static final int GROWTH_DELAY_TICKS = 30;
    public static final int SPREAD_PER_CELL = 2;
    private static final int STEP_PARTICLES = 5;

    LivingSubstrateBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition
                .any()
                .setValue(STAGE, SubstrateStage.BASIC)
                .setValue(VIGOR, 0)
                .setValue(TILE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STAGE, VIGOR, TILE);
    }

    /** Column of the macro texture from x, row from z: tile 0 at (0, 0), tile 15 at (3, 3). */
    public static int tileAt(BlockPos pos) {
        return (pos.getX() & 3) + 4 * (pos.getZ() & 3);
    }

    /** A new basic, dormant cell drawing the tile for {@code pos}. */
    public BlockState cellAt(BlockPos pos) {
        return defaultBlockState().setValue(TILE, tileAt(pos));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return cellAt(context.getClickedPos());
    }

    /**
     * Commands and other mods may place any tile; one extra setBlock puts the right one. The tile changes only the
     * picture, so the neighbours' shapes are left alone: updating them would read, and so load, a neighbour's chunk.
     */
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        int tile = tileAt(pos);
        if (state.getValue(TILE) != tile)
            level.setBlock(pos, state.setValue(TILE, tile), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return state.getValue(STAGE).next().isPresent()
                || !spreadTargets(level, pos).isEmpty();
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        level.setBlock(pos, state.setValue(VIGOR, BONE_MEAL_VIGOR), Block.UPDATE_CLIENTS);
        level.scheduleTick(pos, this, GROWTH_DELAY_TICKS);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int vigor = state.getValue(VIGOR);
        if (vigor == 0) return;
        switch (state.getValue(STAGE)) {
            case BASIC -> {
                boolean goesOn = vigor >= 2;
                level.setBlock(
                        pos,
                        state.setValue(STAGE, SubstrateStage.YOUNG).setValue(VIGOR, goesOn ? vigor : 0),
                        Block.UPDATE_ALL);
                level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_PLANT_GROWTH, pos, STEP_PARTICLES);
                spread(level, pos, vigor - 1, random);
                if (goesOn) level.scheduleTick(pos, this, GROWTH_DELAY_TICKS);
            }
            case YOUNG -> {
                level.setBlock(pos, state.setValue(STAGE, SubstrateStage.MATURE).setValue(VIGOR, 0), Block.UPDATE_ALL);
                level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_PLANT_GROWTH, pos, STEP_PARTICLES);
            }
            case MATURE -> {
                spread(level, pos, vigor - 1, random);
                level.setBlock(pos, state.setValue(VIGOR, 0), Block.UPDATE_CLIENTS);
            }
        }
    }

    /** Converts up to {@value #SPREAD_PER_CELL} exposed hosts beside {@code pos} into basic cells with {@code vigor}. */
    private void spread(ServerLevel level, BlockPos pos, int vigor, RandomSource random) {
        List<BlockPos> targets = spreadTargets(level, pos);
        for (int n = 0; n < SPREAD_PER_CELL && !targets.isEmpty(); n++) {
            BlockPos target = targets.remove(random.nextInt(targets.size()));
            level.setBlock(target, cellAt(target).setValue(VIGOR, vigor), Block.UPDATE_ALL);
            level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_PLANT_GROWTH, target, STEP_PARTICLES);
            if (vigor > 0) level.scheduleTick(target, this, GROWTH_DELAY_TICKS);
        }
    }

    /**
     * The first exposed host in each horizontal direction, level with the cell or one step up or down. At most twelve
     * block reads, all inside loaded chunks.
     */
    static List<BlockPos> spreadTargets(LevelReader level, BlockPos pos) {
        List<BlockPos> targets = new ArrayList<>(4);
        for (Direction side : Direction.Plane.HORIZONTAL) {
            for (int dy : new int[] {0, 1, -1}) {
                BlockPos host = pos.relative(side).above(dy);
                if (!level.hasChunkAt(host) || !level.hasChunkAt(host.above())) continue;
                if (isExposedHost(level, host)) {
                    targets.add(host);
                    break;
                }
            }
        }
        return targets;
    }

    /** A host block with nothing solid on top of it. */
    static boolean isExposedHost(LevelReader level, BlockPos host) {
        return level.getBlockState(host).is(HOSTS)
                && level.getBlockState(host.above())
                        .getCollisionShape(level, host.above())
                        .isEmpty();
    }
}
