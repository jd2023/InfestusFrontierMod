package org.jd.infestusfrontier.storage.api;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.core.foundation.Outcome;
import org.jd.infestusfrontier.core.foundation.Refusal;
import org.jd.infestusfrontier.core.foundation.TickQuota;
import org.jd.infestusfrontier.core.foundation.WindowQuota;
import org.jd.infestusfrontier.core.storage.BiomassTank;
import org.jd.infestusfrontier.foundation.Refusals;
import org.jd.infestusfrontier.storage.StorageModule;
import org.jetbrains.annotations.Nullable;

/**
 * Whole-bucket transfers between a player's hand and a biomass block. An empty vanilla bucket takes 1000 BU out; a
 * biomass bucket puts 1000 BU in. Every organ block calls {@link #useOn} first from its {@code useItemOn}.
 *
 * <p>Admission is shared by every hand transfer on the server: at most 64 per server tick in total and 4 per player in
 * any 20 ticks. Excess is refused and nothing queues. The counters hold no world or item references.
 */
public final class BiomassBuckets {
    public static final Refusal LIMIT = new Refusal("storage.transfer_limit");
    public static final int PER_TICK = 64;
    public static final int PER_PLAYER = 4;
    public static final int PLAYER_WINDOW_TICKS = 20;
    private static final TickQuota PER_TICK_QUOTA = new TickQuota(PER_TICK);
    private static final Map<UUID, WindowQuota> PER_PLAYER_QUOTA = new HashMap<>();

    private BiomassBuckets() {}

    /** True when {@code stack} is a bucket this class handles. */
    public static boolean isBucket(ItemStack stack) {
        return stack.is(Items.BUCKET) || stack.is(StorageModule.BIOMASS_BUCKET.get());
    }

    /**
     * Moves one bucket between the hand and the block at {@code pos}. Passes to the default block interaction when the
     * stack is not a bucket or the block holds no biomass.
     */
    public static ItemInteractionResult useOn(
            ItemStack stack, Level level, BlockPos pos, @Nullable Direction face, Player player, InteractionHand hand) {
        if (!isBucket(stack)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        BiomassHandler handler = level.getCapability(BiomassCapabilities.BLOCK, pos, face);
        if (handler == null) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        if (!admit(player.getUUID(), level.getGameTime())) {
            Refusals.show(player, LIMIT);
            return ItemInteractionResult.FAIL;
        }
        boolean taking = stack.is(Items.BUCKET);
        Outcome<BiomassTank> moved = taking ? handler.extract(Biomass.BUCKET) : handler.insert(Biomass.BUCKET);
        ItemStack after = new ItemStack(taking ? StorageModule.BIOMASS_BUCKET.get() : Items.BUCKET);
        return switch (moved) {
            case Outcome.Done<BiomassTank> done -> {
                player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, after));
                yield ItemInteractionResult.CONSUME;
            }
            case Outcome.Refused<BiomassTank> refused -> {
                Refusals.show(player, refused.refusal());
                yield ItemInteractionResult.FAIL;
            }
        };
    }

    /** Admits one hand transfer, or refuses it. Also for equipment filling later. */
    public static boolean admit(UUID player, long now) {
        WindowQuota own =
                PER_PLAYER_QUOTA.computeIfAbsent(player, id -> new WindowQuota(PER_PLAYER, PLAYER_WINDOW_TICKS));
        return own.take(now) && PER_TICK_QUOTA.take(now);
    }

    /** Called when a player leaves, so the map stays bounded by the players online. */
    public static void forget(UUID player) {
        PER_PLAYER_QUOTA.remove(player);
    }
}
