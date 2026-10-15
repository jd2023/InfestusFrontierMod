package org.jd.infestusfrontier.ecology;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/** Living ground the player grows on purpose. No block entity, no ticking, no spreading. */
public final class LivingSubstrateBlock extends Block {
    public static final EnumProperty<SubstrateStage> STAGE = EnumProperty.create("stage", SubstrateStage.class);

    LivingSubstrateBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(STAGE, SubstrateStage.BASIC));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STAGE);
    }
}
