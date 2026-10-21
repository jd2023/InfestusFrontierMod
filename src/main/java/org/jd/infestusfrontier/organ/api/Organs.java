package org.jd.infestusfrontier.organ.api;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.jd.infestusfrontier.InfestusFrontier;

/** Rules every organ block shares. */
public final class Organs {
    /** Blocks an organ may stand on. Living Substrate in every stage. */
    public static final TagKey<Block> FOUNDATIONS = TagKey.create(
            Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(InfestusFrontier.MOD_ID, "organ_foundations"));

    /** Items that read an organ instead of opening its screen; the organ steps aside and the item's own use runs. */
    public static final TagKey<Item> READERS = TagKey.create(
            Registries.ITEM, ResourceLocation.fromNamespaceAndPath(InfestusFrontier.MOD_ID, "organ_readers"));

    /** Organs a Feeding Lobe or a Yield Lobe can serve. */
    public static final TagKey<Block> PORT_HOSTS = TagKey.create(
            Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(InfestusFrontier.MOD_ID, "port_hosts"));

    /** The last growth stage an organ's state can name. */
    public static final int LAST_STAGE = 2;
    /** The growth stage of an organ that grows: 0 young, up to {@link #LAST_STAGE}. */
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, LAST_STAGE);

    private Organs() {}

    /** The growth stage {@code state} names, or 0 for an organ that does not grow. */
    public static int stage(BlockState state) {
        return state.hasProperty(STAGE) ? state.getValue(STAGE) : 0;
    }

    /**
     * Opens the screen of the organ at {@code pos} for any hand, the way a chest opens. Blocks call it after their
     * special items (fluid containers, feed, ingredients) had their chance. Sneaking skips the block, so building
     * against an organ still works.
     */
    public static ItemInteractionResult openScreen(Level level, BlockPos pos, Player player) {
        if (player.getMainHandItem().is(READERS)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!(level.getBlockEntity(pos) instanceof MenuProvider provider))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        if (player instanceof ServerPlayer server) server.openMenu(provider, pos);
        return ItemInteractionResult.CONSUME;
    }

    /**
     * For {@code useItemOn}: an empty hand falls through to {@code useWithoutItem}, a {@link #READERS reader} falls
     * through to its own use, any other item opens the screen.
     */
    public static ItemInteractionResult openScreen(ItemStack held, Level level, BlockPos pos, Player player) {
        if (held.isEmpty() || held.is(READERS)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        return openScreen(level, pos, player);
    }

    /** True when the block below {@code pos} is a loaded foundation. An organ places and works only then. */
    public static boolean hasFoundation(LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.hasChunkAt(below) && level.getBlockState(below).is(FOUNDATIONS);
    }
}
