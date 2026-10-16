package org.jd.infestusfrontier.ecology;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
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
 * <p>Bone meal gives a cell {@value #BONE_MEAL_VIGOR} vigor and starts one bounded wave of scheduled ticks: every
 * {@value #GROWTH_DELAY_TICKS} ticks the cell matures one stage; once mature it converts up to {@value #SPREAD_PER_CELL}
 * exposed host blocks beside it into basic cells with one vigor less, then goes dormant. Vigor 0 cells do nothing.
 * One bone meal therefore touches at most 15 cells and schedules at most 22 ticks, then everything is quiet again.
 */
public final class LivingSubstrateBlock extends Block implements BonemealableBlock {
    public static final EnumProperty<SubstrateStage> STAGE = EnumProperty.create("stage", SubstrateStage.class);
    /** Growth left in this cell. 0 is dormant. Bone meal sets {@value #BONE_MEAL_VIGOR}; each spread hands on one less. */
    public static final IntegerProperty VIGOR = IntegerProperty.create("vigor", 0, 3);
    /** Ground that Spore Culture converts and a mature cell can spread onto. */
    public static final TagKey<Block> HOSTS = TagKey.create(
            Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(InfestusFrontier.MOD_ID, "substrate_hosts"));

    public static final int BONE_MEAL_VIGOR = 3;
    public static final int GROWTH_DELAY_TICKS = 30;
    public static final int SPREAD_PER_CELL = 2;
    private static final int STEP_PARTICLES = 5;

    LivingSubstrateBlock(Properties properties) {
        super(properties);
        registerDefaultState(
                stateDefinition.any().setValue(STAGE, SubstrateStage.BASIC).setValue(VIGOR, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STAGE, VIGOR);
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
        Optional<SubstrateStage> next = state.getValue(STAGE).next();
        if (next.isPresent()) {
            level.setBlock(pos, state.setValue(STAGE, next.get()), Block.UPDATE_ALL);
            level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_PLANT_GROWTH, pos, STEP_PARTICLES);
            level.scheduleTick(pos, this, GROWTH_DELAY_TICKS);
            return;
        }
        List<BlockPos> targets = spreadTargets(level, pos);
        for (int n = 0; n < SPREAD_PER_CELL && !targets.isEmpty(); n++) {
            BlockPos target = targets.remove(random.nextInt(targets.size()));
            level.setBlock(target, defaultBlockState().setValue(VIGOR, vigor - 1), Block.UPDATE_ALL);
            level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_PLANT_GROWTH, target, STEP_PARTICLES);
            if (vigor - 1 > 0) level.scheduleTick(target, this, GROWTH_DELAY_TICKS);
        }
        level.setBlock(pos, state.setValue(VIGOR, 0), Block.UPDATE_CLIENTS);
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
