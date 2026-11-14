package org.jd.infestusfrontier.organ;

import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import org.jd.infestusfrontier.core.foundation.Refusal;
import org.jd.infestusfrontier.foundation.Persistence;
import org.jd.infestusfrontier.foundation.Refusals;
import org.jd.infestusfrontier.organ.api.OrganGrowthRecipe;
import org.jd.infestusfrontier.organ.api.Organs;
import org.jetbrains.annotations.Nullable;

/**
 * A dormant organ blank. It grows only on an organ foundation. A bud is told which organ to grow (sneak with an empty
 * hand to change the choice, until something is supplied), is handed the materials the growth needs by right click, and
 * the moment the last one arrives grows into the organ in place. Every growth is an
 * {@link OrganGrowthRecipe}. A refusal changes nothing; breaking the bud gives every supplied material back.
 */
final class OrganBudBlock extends BaseEntityBlock {
    static final Refusal NO_FOUNDATION = new Refusal("organ.bud_no_foundation");
    static final Refusal NO_GROWTH = new Refusal("organ.bud_no_growth");
    static final Refusal LOST_GROWTH = new Refusal("organ.bud_lost_growth");
    static final Refusal NOT_GROWN = new Refusal("organ.bud_not_grown");

    private static final MapCodec<OrganBudBlock> CODEC = simpleCodec(OrganBudBlock::new);
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 10, 14);

    OrganBudBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<OrganBudBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new OrganBudBlockEntity(pos, state);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return Organs.hasFoundation(context.getLevel(), context.getClickedPos()) ? defaultBlockState() : null;
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (stack.isEmpty() || stack.is(Organs.READERS)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        InteractionResult result = interact(level, pos, player, stack);
        if (result == InteractionResult.PASS) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        return result == InteractionResult.SUCCESS ? ItemInteractionResult.SUCCESS : ItemInteractionResult.CONSUME;
    }

    /** An empty main hand reads the bud, and supplies from the off hand when that holds something. */
    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.getMainHandItem().isEmpty()) return InteractionResult.PASS;
        return interact(level, pos, player, player.getOffhandItem());
    }

    /**
     * One use of the bud with {@code held} (possibly empty). While nothing is supplied the choice follows the hand:
     * sneaking with an empty hand takes the next organ, and an item the chosen organ does not use picks the first
     * organ that does. Then the item is supplied and, when the growth is complete, the organ grows.
     */
    private InteractionResult interact(Level level, BlockPos pos, Player player, ItemStack held) {
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof OrganBudBlockEntity bud)) return InteractionResult.PASS;
        if (bud.unreadable()) {
            Refusals.show(player, Persistence.CORRUPT);
            return InteractionResult.CONSUME;
        }
        List<RecipeHolder<OrganGrowthRecipe>> growths = OrganGrowthRecipe.all(level);
        if (growths.isEmpty()) {
            Refusals.show(player, NO_GROWTH);
            return InteractionResult.CONSUME;
        }
        Optional<RecipeHolder<OrganGrowthRecipe>> chosen = bud.chosen(level);
        if (!bud.started()) {
            Optional<RecipeHolder<OrganGrowthRecipe>> next = chooseFor(growths, chosen, held, player.isShiftKeyDown());
            if (next.isPresent() && !next.equals(chosen)) {
                bud.choose(next.get().value().organ());
                chosen = next;
            }
        }
        if (chosen.isEmpty()) {
            Refusals.show(player, LOST_GROWTH);
            return InteractionResult.CONSUME;
        }
        OrganGrowthRecipe recipe = chosen.get().value();
        if (!held.isEmpty() && bud.supply(recipe, held) > 0)
            level.playSound(null, pos, SoundEvents.SLIME_BLOCK_PLACE, SoundSource.BLOCKS, 0.8F, 1.1F);
        if (bud.complete(recipe)) grow(server, pos, bud, recipe, player);
        else player.displayClientMessage(status(recipe, bud), true);
        return InteractionResult.CONSUME;
    }

    /** The growth a bud with nothing supplied should have for this use of {@code held}. */
    private static Optional<RecipeHolder<OrganGrowthRecipe>> chooseFor(
            List<RecipeHolder<OrganGrowthRecipe>> growths,
            Optional<RecipeHolder<OrganGrowthRecipe>> chosen,
            ItemStack held,
            boolean sneaking) {
        if (chosen.isEmpty())
            return growths.stream()
                    .filter(growth -> growth.value().uses(held))
                    .findFirst()
                    .or(() -> Optional.of(growths.get(0)));
        if (held.isEmpty()) {
            if (!sneaking) return chosen;
            return Optional.of(growths.get((growths.indexOf(chosen.get()) + 1) % growths.size()));
        }
        if (chosen.get().value().uses(held)) return chosen;
        return growths.stream()
                .filter(growth -> growth.value().uses(held))
                .findFirst()
                .or(() -> chosen);
    }

    /** The grown organ replaces the bud in place, spending the materials once; any surplus is given back. */
    private static void grow(
            ServerLevel level, BlockPos pos, OrganBudBlockEntity bud, OrganGrowthRecipe recipe, Player player) {
        if (!Organs.hasFoundation(level, pos)) {
            Refusals.show(player, NO_FOUNDATION);
            return;
        }
        Block organ = recipe.block();
        OrganBudBlockEntity.Taken taken = bud.takeForGrowth(recipe);
        BlockState grown = organ.defaultBlockState();
        if (!level.setBlock(pos, grown, Block.UPDATE_ALL)
                || !level.getBlockState(pos).is(organ)) {
            bud.restore(taken);
            Refusals.show(player, NOT_GROWN);
            return;
        }
        for (ItemStack unused : taken.unused())
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, unused);
        organ.setPlacedBy(level, pos, grown, player, new ItemStack(organ));
        level.playSound(null, pos, SoundEvents.SLIME_BLOCK_BREAK, SoundSource.BLOCKS, 1.0F, 0.8F);
        level.sendParticles(
                ParticleTypes.HAPPY_VILLAGER,
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5,
                8,
                0.3,
                0.3,
                0.3,
                0.0);
        player.displayClientMessage(Component.translatable("infestusfrontier.organ_bud.grown", organ.getName()), true);
    }

    /** What the bud is growing and what it still needs, as {@code supplied/needed Material}. */
    private static Component status(OrganGrowthRecipe recipe, OrganBudBlockEntity bud) {
        int[] missing = bud.missing(recipe);
        MutableComponent needs = Component.empty();
        for (int i = 0; i < missing.length; i++) {
            SizedIngredient material = recipe.materials().get(i);
            if (i > 0) needs.append(", ");
            needs.append(Component.translatable(
                    "infestusfrontier.organ_bud.material",
                    material.count() - missing[i],
                    material.count(),
                    materialName(material)));
        }
        return Component.translatable(
                bud.started() ? "infestusfrontier.organ_bud.growing" : "infestusfrontier.organ_bud.choice",
                recipe.block().getName(),
                needs);
    }

    private static Component materialName(SizedIngredient material) {
        ItemStack[] kinds = material.getItems();
        return kinds.length == 0 ? Component.empty() : kinds[0].getHoverName();
    }

    /** Breaking the bud, or anything else that replaces it, gives back what it was supplied. */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide
                && !state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof OrganBudBlockEntity bud)
            for (ItemStack supplied : bud.emptied())
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, supplied);
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
