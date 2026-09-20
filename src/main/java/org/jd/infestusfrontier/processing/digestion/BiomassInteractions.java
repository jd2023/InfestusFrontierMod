package org.jd.infestusfrontier.processing.digestion;

import net.minecraft.server.level.ServerPlayer;
import org.jd.infestusfrontier.discovery.api.DiscoveryObserver;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jd.infestusfrontier.storage.api.BiomassTransfer;
import org.jd.infestusfrontier.storage.api.PortableTransferAdmission;
import org.jd.infestusfrontier.storage.api.QuantityStore;

final class BiomassInteractions {
    enum TransferResult { PASS, REFUSED, FILLED_BUCKET, EMPTIED_BUCKET }

    static String feed(ItemStack stack) {
        return stack.is(Items.WHEAT) ? "wheat" : stack.is(Items.ROTTEN_FLESH) ? "rotten_flesh" : "";
    }

    static boolean isBucket(ItemStack stack, Item biomassBucket) {
        return (stack.is(Items.BUCKET) || stack.is(biomassBucket)) && stack.getComponentsPatch().isEmpty();
    }

    static TransferResult transfer(QuantityStore store, Player player, InteractionHand hand, Item biomassBucket,
            DiscoveryObserver discovery) {
        var held = player.getItemInHand(hand);
        if (!isBucket(held, biomassBucket)) return TransferResult.PASS;
        if (held.is(Items.BUCKET) && held.getCount() != 1) {
            message(player, "refused");
            return TransferResult.REFUSED;
        }
        var admitted = TransferAdmission.take(player.getServer(), player.getUUID());
        if (admitted != PortableTransferAdmission.Result.ACCEPTED) {
            message(player, admitted == PortableTransferAdmission.Result.PLAYER_LIMIT
                    ? "player_limited" : "server_limited");
            return TransferResult.REFUSED;
        }
        var result = held.is(Items.BUCKET)
                ? BiomassTransfer.reserveBucketFill(store)
                : BiomassTransfer.reserveBucketEmpty(store);
        if (!(result instanceof BiomassTransfer.Pending pending)
                || pending.commit() != BiomassTransfer.CommitResult.COMMITTED) {
            message(player, "refused");
            return TransferResult.REFUSED;
        }
        player.setItemInHand(hand, new ItemStack(held.is(Items.BUCKET) ? biomassBucket : Items.BUCKET));
        if (player instanceof ServerPlayer serverPlayer) {
            discovery.complete(serverPlayer,
                    DiscoveryObserver.Milestone.BIOMASS_BUCKET);
        }
        message(player, "complete");
        return held.is(Items.BUCKET) ? TransferResult.FILLED_BUCKET : TransferResult.EMPTIED_BUCKET;
    }

    static void status(Player player, int amount, int capacity) {
        player.displayClientMessage(Component.translatable(
                "message.infestusfrontier.biomass.status", amount, capacity), true);
    }

    private static void message(Player player, String suffix) {
        player.displayClientMessage(Component.translatable(
                "message.infestusfrontier.biomass.transfer." + suffix), false);
    }

    private BiomassInteractions() {}
}
