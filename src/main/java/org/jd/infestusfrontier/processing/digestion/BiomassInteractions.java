package org.jd.infestusfrontier.processing.digestion;

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
    static boolean isBucket(ItemStack stack, Item biomassBucket) {
        return (stack.is(Items.BUCKET) || stack.is(biomassBucket)) && stack.getComponentsPatch().isEmpty();
    }

    static boolean transfer(QuantityStore store, Player player, InteractionHand hand, Item biomassBucket) {
        var held = player.getItemInHand(hand);
        if (!isBucket(held, biomassBucket)) return false;
        if (held.is(Items.BUCKET) && held.getCount() != 1) {
            message(player, "refused");
            return true;
        }
        var admitted = TransferAdmission.take(player.getServer(), player.getUUID());
        if (admitted != PortableTransferAdmission.Result.ACCEPTED) {
            message(player, admitted == PortableTransferAdmission.Result.PLAYER_LIMIT
                    ? "player_limited" : "server_limited");
            return true;
        }
        var result = held.is(Items.BUCKET)
                ? BiomassTransfer.reserveBucketFill(store)
                : BiomassTransfer.reserveBucketEmpty(store);
        if (!(result instanceof BiomassTransfer.Pending pending)
                || pending.commit() != BiomassTransfer.CommitResult.COMMITTED) {
            message(player, "refused");
            return true;
        }
        player.setItemInHand(hand, new ItemStack(held.is(Items.BUCKET) ? biomassBucket : Items.BUCKET));
        message(player, "complete");
        return true;
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
