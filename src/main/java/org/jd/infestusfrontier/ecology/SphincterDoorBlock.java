package org.jd.infestusfrontier.ecology;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A two-high doorway mutated from a wall (BLOCK_CATALOG T1-29): a muscular rim whose membrane blades close like an
 * iris. It is a vanilla door, so its two halves stay one doorway; the lower half keeps the block entity that draws and
 * animates the whole iris, the upper half draws nothing of its own. Closed, a membrane four pixels thick fills the
 * doorway; open, nothing stops a walker. Grown, never placed: broken, it dies into Dead Tissue.
 *
 * <p>The Sphincter Door is a simple door, like a wooden one: hands and redstone open it, villagers open it (it is in
 * {@code minecraft:mob_interactable_doors}), and zombies on Hard break it down because it opens by hand. Grafts
 * harden it into the Clenched and then the Calcified Door, which follow the vanilla iron door: only redstone opens
 * them, so no mob opens or breaks them, and no piston moves them.
 */
public final class SphincterDoorBlock extends DoorBlock implements EntityBlock {
    /** The simple door's sounds and who may open it: any hand, redstone, never an arrow or a wind charge. */
    static final BlockSetType TYPE = BlockSetType.register(new BlockSetType(
            "infestusfrontier:sphincter",
            true,
            false,
            false,
            BlockSetType.PressurePlateSensitivity.EVERYTHING,
            SoundType.SLIME_BLOCK,
            SoundEvents.HONEY_BLOCK_SLIDE,
            SoundEvents.SLIME_SQUISH,
            SoundEvents.HONEY_BLOCK_SLIDE,
            SoundEvents.SLIME_SQUISH,
            SoundEvents.SLIME_BLOCK_STEP,
            SoundEvents.SLIME_BLOCK_STEP,
            SoundEvents.SLIME_BLOCK_STEP,
            SoundEvents.SLIME_BLOCK_STEP));

    /** The hardened doors: redstone only, as the vanilla iron door, and heavier sounds of bone closing on flesh. */
    static final BlockSetType CLENCHED = BlockSetType.register(new BlockSetType(
            "infestusfrontier:clenched",
            false,
            false,
            false,
            BlockSetType.PressurePlateSensitivity.MOBS,
            SoundType.BONE_BLOCK,
            SoundEvents.BONE_BLOCK_PLACE,
            SoundEvents.HONEY_BLOCK_SLIDE,
            SoundEvents.BONE_BLOCK_PLACE,
            SoundEvents.HONEY_BLOCK_SLIDE,
            SoundEvents.BONE_BLOCK_STEP,
            SoundEvents.BONE_BLOCK_STEP,
            SoundEvents.BONE_BLOCK_STEP,
            SoundEvents.BONE_BLOCK_STEP));

    public static final MapCodec<SphincterDoorBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                    BlockSetType.CODEC.fieldOf("block_set_type").forGetter(DoorBlock::type),
                    Codec.STRING.fieldOf("model").forGetter(door -> door.model),
                    propertiesCodec())
            .apply(instance, (type, model, properties) -> new SphincterDoorBlock(type, model, properties)));
    private static final VoxelShape ACROSS_X = Block.box(0, 0, 6, 16, 16, 10);
    private static final VoxelShape ACROSS_Z = Block.box(6, 0, 0, 10, 16, 16);
    private static final VoxelShape JAMBS_X = Shapes.or(Block.box(0, 0, 6, 3, 16, 10), Block.box(13, 0, 6, 16, 16, 10));
    private static final VoxelShape JAMBS_Z = Shapes.or(Block.box(6, 0, 0, 10, 16, 3), Block.box(6, 0, 13, 10, 16, 16));

    /** The block entity type whose model draws this door, by the name of that model. */
    private final String model;

    SphincterDoorBlock(BlockSetType type, String model, Properties properties) {
        super(type, properties);
        this.model = model;
    }

    @Override
    public MapCodec<SphincterDoorBlock> codec() {
        return CODEC;
    }

    /**
     * A graft that hardens this door, in hand, hardens it instead of opening it: vanilla's skip of the block's own use
     * hands the click to the item. Any other graft leaves the click to the door.
     */
    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        return stack.getItem() instanceof WallGraftItem graft && graft.hardens(state)
                ? ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION
                : super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    /** Closed, the membrane across the doorway; open, only the jambs, so a click passes through the doorway. */
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        boolean acrossX = state.getValue(FACING).getAxis() == Direction.Axis.Z;
        if (isOpen(state)) return acrossX ? JAMBS_X : JAMBS_Z;
        return acrossX ? ACROSS_X : ACROSS_Z;
    }

    /** Closed, the membrane stops a walker; open, nothing does. */
    @Override
    protected VoxelShape getCollisionShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return isOpen(state) ? Shapes.empty() : getShape(state, level, pos, context);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? RenderShape.ENTITYBLOCK_ANIMATED : RenderShape.INVISIBLE;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER
                ? new SphincterDoorBlockEntity(EcologyModule.doorEntity(model), pos, state)
                : null;
    }
}
