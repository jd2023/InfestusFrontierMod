package org.jd.infestusfrontier.ecology;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jd.infestusfrontier.ecology.api.LivingCell;

final class LivingSubstrateBlock extends Block {
    static final EnumProperty<Stage> STAGE = EnumProperty.create("stage", Stage.class);
    static final EnumProperty<Pigment> PIGMENT = EnumProperty.create("pigment", Pigment.class);
    static final EnumProperty<Function> FUNCTION = EnumProperty.create("function", Function.class);
    static final EnumProperty<Framework> FRAMEWORK = EnumProperty.create("framework", Framework.class);

    LivingSubstrateBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(STAGE, Stage.BASIC).setValue(PIGMENT, Pigment.UNDYED)
                .setValue(FUNCTION, Function.PLAIN).setValue(FRAMEWORK, Framework.UNREINFORCED));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return simpleCodec(LivingSubstrateBlock::new);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STAGE, PIGMENT, FUNCTION, FRAMEWORK);
    }

    @Override
    protected boolean skipRendering(BlockState state, BlockState adjacentState, Direction direction) {
        return adjacentState.is(this) || super.skipRendering(state, adjacentState, direction);
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult) {
        LivingCell.Treatment treatment = treatment(stack);
        if (treatment == null) return ItemInteractionResult.FAIL;
        if (level.isClientSide) {
            return mutation(state, treatment, player.getUUID()).applied()
                    ? ItemInteractionResult.SUCCESS
                    : ItemInteractionResult.FAIL;
        }
        SubstrateOwnership ownership = SubstrateOwnership.get((ServerLevel) level);
        if (!level.mayInteract(player, pos)
                || !player.mayUseItemAt(pos, hitResult.getDirection(), stack)
                || !ownership.permits(pos, player.getUUID())) {
            return ItemInteractionResult.FAIL;
        }
        var result = mutation(state, treatment, ownership.owner(pos).orElse(player.getUUID()));
        if (!result.applied()) return ItemInteractionResult.FAIL;
        BlockState changed = state
                .setValue(STAGE, Stage.from(result.cell().maturity()))
                .setValue(PIGMENT, Pigment.from(result.cell().pigment()))
                .setValue(FUNCTION, Function.from(result.cell().function()))
                .setValue(FRAMEWORK, Framework.from(result.cell().framework()));
        if (!level.setBlock(pos, changed, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE, 0)) {
            return ItemInteractionResult.FAIL;
        }
        stack.consume(1, player);
        return ItemInteractionResult.CONSUME;
    }

    private static LivingCell.MutationResult mutation(
            BlockState state, LivingCell.Treatment treatment, java.util.UUID owner) {
        LivingCell cell = new LivingCell(
                state.getValue(STAGE).maturity,
                LivingCell.Host.ORDINARY,
                state.getValue(FUNCTION).function,
                state.getValue(FRAMEWORK).framework,
                LivingCell.Lining.NONE,
                state.getValue(PIGMENT).pigment,
                owner);
        return cell.mutate(treatment);
    }

    private static LivingCell.Treatment treatment(ItemStack stack) {
        if (stack.is(Items.BONE_MEAL)) return new LivingCell.Treatment.Grow();
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id.equals(ResourceLocation.fromNamespaceAndPath("infestusfrontier", "processing/lumen_secretion"))) {
            return new LivingCell.Treatment.SetFunction(LivingCell.Function.LUMEN);
        }
        if (id.equals(ResourceLocation.fromNamespaceAndPath("infestusfrontier", "processing/skeletal_graft"))) {
            return new LivingCell.Treatment.SetFramework(LivingCell.Framework.BONE_RIBBED);
        }
        if (stack.getItem() instanceof DyeItem dye) {
            Pigment pigment = Pigment.fromName(dye.getDyeColor().getName());
            return pigment == null ? null : new LivingCell.Treatment.SetPigment(pigment.pigment);
        }
        return null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level instanceof ServerLevel serverLevel && placer instanceof Player player) {
            SubstrateOwnership.get(serverLevel).claim(pos, player.getUUID());
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel) {
            SubstrateOwnership.get(serverLevel).remove(pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return false;
    }

    static int tint(BlockState state) {
        return state.getValue(PIGMENT).color;
    }

    static int light(BlockState state) { return state.getValue(FUNCTION) == Function.LUMEN ? 12 : 0; }

    enum Function implements StringRepresentable {
        PLAIN("plain", LivingCell.Function.PLAIN), LUMEN("lumen", LivingCell.Function.LUMEN);
        private final String name; private final LivingCell.Function function;
        Function(String name, LivingCell.Function function) { this.name = name; this.function = function; }
        static Function from(LivingCell.Function function) {
            if (function == LivingCell.Function.LUMEN) return LUMEN;
            if (function == LivingCell.Function.PLAIN) return PLAIN;
            throw new IllegalArgumentException("Unregistered substrate function " + function);
        }
        @Override public String getSerializedName() { return name; }
    }

    enum Framework implements StringRepresentable {
        UNREINFORCED("unreinforced", LivingCell.Framework.UNREINFORCED),
        BONE_RIBBED("bone_ribbed", LivingCell.Framework.BONE_RIBBED);
        private final String name; private final LivingCell.Framework framework;
        Framework(String name, LivingCell.Framework framework) { this.name = name; this.framework = framework; }
        static Framework from(LivingCell.Framework framework) {
            if (framework == LivingCell.Framework.BONE_RIBBED) return BONE_RIBBED;
            if (framework == LivingCell.Framework.UNREINFORCED) return UNREINFORCED;
            throw new IllegalArgumentException("Unregistered substrate framework " + framework);
        }
        @Override public String getSerializedName() { return name; }
    }

    enum Stage implements StringRepresentable {
        BASIC("basic", LivingCell.Maturity.BASIC),
        YOUNG("young", LivingCell.Maturity.YOUNG),
        MATURE("mature", LivingCell.Maturity.MATURE);

        private final String name;
        private final LivingCell.Maturity maturity;

        Stage(String name, LivingCell.Maturity maturity) {
            this.name = name;
            this.maturity = maturity;
        }

        static Stage from(LivingCell.Maturity maturity) {
            return values()[maturity.ordinal()];
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    enum Pigment implements StringRepresentable {
        UNDYED("undyed", 0xB85D5D, LivingCell.Pigment.UNDYED),
        WHITE("white", 0xF9FFFE, LivingCell.Pigment.WHITE),
        ORANGE("orange", 0xF9801D, LivingCell.Pigment.ORANGE),
        MAGENTA("magenta", 0xC74EBD, LivingCell.Pigment.MAGENTA),
        LIGHT_BLUE("light_blue", 0x3AB3DA, LivingCell.Pigment.LIGHT_BLUE),
        YELLOW("yellow", 0xFED83D, LivingCell.Pigment.YELLOW),
        LIME("lime", 0x80C71F, LivingCell.Pigment.LIME),
        PINK("pink", 0xF38BAA, LivingCell.Pigment.PINK),
        GRAY("gray", 0x474F52, LivingCell.Pigment.GRAY),
        LIGHT_GRAY("light_gray", 0x9D9D97, LivingCell.Pigment.LIGHT_GRAY),
        CYAN("cyan", 0x169C9C, LivingCell.Pigment.CYAN),
        PURPLE("purple", 0x8932B8, LivingCell.Pigment.PURPLE),
        BLUE("blue", 0x3C44AA, LivingCell.Pigment.BLUE),
        BROWN("brown", 0x835432, LivingCell.Pigment.BROWN),
        GREEN("green", 0x5E7C16, LivingCell.Pigment.GREEN),
        RED("red", 0xB02E26, LivingCell.Pigment.RED),
        BLACK("black", 0x1D1D21, LivingCell.Pigment.BLACK);

        private final String name;
        private final int color;
        private final LivingCell.Pigment pigment;

        Pigment(String name, int color, LivingCell.Pigment pigment) {
            this.name = name;
            this.color = color;
            this.pigment = pigment;
        }

        static Pigment fromName(String name) {
            for (Pigment pigment : values()) if (pigment.name.equals(name)) return pigment;
            return null;
        }

        static Pigment from(LivingCell.Pigment pigment) {
            return values()[pigment.ordinal()];
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
