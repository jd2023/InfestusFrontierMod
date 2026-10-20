package org.jd.infestusfrontier.cultivation;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.util.TriState;
import org.jd.infestusfrontier.storage.api.BiomassFluids;

/**
 * A crop bed grown out of a mature substrate cell with Rooting Gel. Vanilla crops grow on it as on farmland, and as
 * on wet farmland while it holds water: it drinks {@value CultivationTissueBlockEntity#SIP_MB} mB on each of its own
 * random ticks that finds a plant above. Water comes by bucket or by a Fluid Vein. It plants and harvests nothing.
 */
public final class CultivationTissueBlock extends BaseEntityBlock {
    private static final MapCodec<CultivationTissueBlock> CODEC = simpleCodec(CultivationTissueBlock::new);

    CultivationTissueBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<CultivationTissueBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CultivationTissueBlockEntity(pos, state);
    }

    /** Any crop may root in it from above; nothing else asks. */
    @Override
    public TriState canSustainPlant(
            BlockState state, BlockGetter level, BlockPos pos, Direction facing, BlockState plant) {
        return facing == Direction.UP && plant.is(BlockTags.CROPS) ? TriState.TRUE : TriState.DEFAULT;
    }

    /** Wet farmland for the crop's growth roll, while there is water in the bed. */
    @Override
    public boolean isFertile(BlockState state, BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof CultivationTissueBlockEntity bed && bed.isWet();
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof CultivationTissueBlockEntity bed
                && level.getBlockState(pos.above()).is(BlockTags.CROPS)) bed.sip();
    }

    /** A water bucket waters the bed; other items build against it, it has no screen. */
    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        return BiomassFluids.useOn(stack, level, pos, hit.getDirection(), player, hand);
    }
}
