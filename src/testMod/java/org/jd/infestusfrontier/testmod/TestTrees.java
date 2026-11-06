package org.jd.infestusfrontier.testmod;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * A tree of another mod, registered the way tree mods register theirs, and its look-alikes. The tags under
 * {@code src/testMod/resources/data} put them where such mods put their blocks: the log and leaves in vanilla's
 * {@code overworld_natural_logs}, {@code logs_that_burn} and {@code leaves}; the wood only in {@code logs_that_burn};
 * the stripped log in {@code c:stripped_logs} too; one log and one leaf in the pack exclusion tags and one log only in
 * the pack inclusion tag. The branch is thin and the entity log has a block entity. No species list names any of them.
 */
final class TestTrees {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks("infestusfrontier_tests");
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, "infestusfrontier_tests");

    static final DeferredBlock<RotatedPillarBlock> LOG = log("fixture_log");
    static final DeferredBlock<LeavesBlock> LEAVES = leaves("fixture_leaves");
    static final DeferredBlock<RotatedPillarBlock> WOOD = log("fixture_wood");
    static final DeferredBlock<RotatedPillarBlock> STRIPPED_LOG = log("fixture_stripped_log");
    static final DeferredBlock<RotatedPillarBlock> EXCLUDED_LOG = log("fixture_excluded_log");
    static final DeferredBlock<RotatedPillarBlock> INCLUDED_LOG = log("fixture_included_log");
    static final DeferredBlock<LeavesBlock> EXCLUDED_LEAVES = leaves("fixture_excluded_leaves");
    static final DeferredBlock<BranchBlock> BRANCH = BLOCKS.registerBlock(
            "fixture_branch", BranchBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LOG));
    static final DeferredBlock<EntityLogBlock> ENTITY_LOG = BLOCKS.registerBlock(
            "fixture_entity_log", EntityLogBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LOG));
    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LogEntity>> LOG_ENTITY = BLOCK_ENTITIES.register(
            "fixture_entity_log",
            () -> BlockEntityType.Builder.of(LogEntity::new, ENTITY_LOG.get()).build(null));

    private TestTrees() {}

    static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
    }

    private static DeferredBlock<RotatedPillarBlock> log(String name) {
        return BLOCKS.registerBlock(
                name, RotatedPillarBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LOG));
    }

    private static DeferredBlock<LeavesBlock> leaves(String name) {
        return BLOCKS.registerBlock(name, LeavesBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LEAVES));
    }

    /** A thin branch of a special branch system: an {@code axis} pillar that is not a full block. */
    static final class BranchBlock extends RotatedPillarBlock {
        private static final VoxelShape SHAPE = Block.box(5, 0, 5, 11, 16, 11);
        private static final MapCodec<BranchBlock> CODEC = simpleCodec(BranchBlock::new);

        BranchBlock(Properties properties) {
            super(properties);
        }

        @Override
        public MapCodec<BranchBlock> codec() {
            return CODEC;
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }
    }

    /** A log with a block entity, as a tapped or hollow log of another mod has. */
    static final class EntityLogBlock extends RotatedPillarBlock implements EntityBlock {
        private static final MapCodec<EntityLogBlock> CODEC = simpleCodec(EntityLogBlock::new);

        EntityLogBlock(Properties properties) {
            super(properties);
        }

        @Override
        public MapCodec<EntityLogBlock> codec() {
            return CODEC;
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new LogEntity(pos, state);
        }
    }

    static final class LogEntity extends BlockEntity {
        LogEntity(BlockPos pos, BlockState state) {
            super(LOG_ENTITY.get(), pos, state);
        }
    }
}
