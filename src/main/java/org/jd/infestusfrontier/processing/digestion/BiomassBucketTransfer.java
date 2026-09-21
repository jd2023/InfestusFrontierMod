package org.jd.infestusfrontier.processing.digestion;

import java.util.function.BooleanSupplier;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jd.infestusfrontier.discovery.api.DiscoveryObserver;
import org.jd.infestusfrontier.storage.api.BiomassTransfer;
import org.jd.infestusfrontier.storage.api.PortableTransferAdmission;
import org.jd.infestusfrontier.storage.api.QuantityStore;

/** Shared hand exchange, portable admission and successful-transfer discovery. */
public final class BiomassBucketTransfer {
    enum Result { PASS, REFUSED, FILLED_BUCKET, EMPTIED_BUCKET }

    /** Calls the atomic 1,000 BU deposit only after admission; refusal must preserve its destination. */
    public static boolean empty(Player player, InteractionHand hand, Item biomassBucket,
            BooleanSupplier deposit, DiscoveryObserver discovery) {
        var held = player.getItemInHand(hand);
        return held.is(biomassBucket) && held.getComponentsPatch().isEmpty()
                && exchange(player, hand, Items.BUCKET, deposit, discovery);
    }

    static Result transfer(QuantityStore store, Player player, InteractionHand hand, Item biomassBucket,
            DiscoveryObserver discovery) {
        var held = player.getItemInHand(hand);
        if (!BiomassInteractions.isBucket(held, biomassBucket)) return Result.PASS;
        boolean filling = held.is(Items.BUCKET);
        boolean committed = exchange(player, hand, filling ? biomassBucket : Items.BUCKET, () -> {
            var result = filling ? BiomassTransfer.reserveBucketFill(store) : BiomassTransfer.reserveBucketEmpty(store);
            return result instanceof BiomassTransfer.Pending pending
                    && pending.commit() == BiomassTransfer.CommitResult.COMMITTED;
        }, discovery);
        return !committed ? Result.REFUSED : filling ? Result.FILLED_BUCKET : Result.EMPTIED_BUCKET;
    }

    private static boolean exchange(Player player, InteractionHand hand, Item returned,
            BooleanSupplier commit, DiscoveryObserver discovery) {
        if (player.getItemInHand(hand).getCount() != 1) {
            message(player, "refused");
            return false;
        }
        var admitted = TransferAdmission.take(player.getServer(), player.getUUID());
        if (admitted != PortableTransferAdmission.Result.ACCEPTED) {
            message(player, admitted == PortableTransferAdmission.Result.PLAYER_LIMIT ? "player_limited" : "server_limited");
            return false;
        }
        if (!commit.getAsBoolean()) {
            message(player, "refused");
            return false;
        }
        player.setItemInHand(hand, new ItemStack(returned));
        if (player instanceof ServerPlayer serverPlayer) {
            discovery.complete(serverPlayer, DiscoveryObserver.Milestone.BIOMASS_BUCKET);
        }
        message(player, "complete");
        return true;
    }

    private static void message(Player player, String suffix) {
        player.displayClientMessage(Component.translatable("message.infestusfrontier.biomass.transfer." + suffix), false);
    }

    private BiomassBucketTransfer() {}
}
