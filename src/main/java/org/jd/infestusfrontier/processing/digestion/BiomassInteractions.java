package org.jd.infestusfrontier.processing.digestion;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

final class BiomassInteractions {
    static String feed(ItemStack stack) {
        return stack.is(Items.WHEAT) ? "wheat" : stack.is(Items.ROTTEN_FLESH) ? "rotten_flesh" : "";
    }

    static boolean isBucket(ItemStack stack, Item biomassBucket) {
        return (stack.is(Items.BUCKET) || stack.is(biomassBucket)) && stack.getComponentsPatch().isEmpty();
    }

    static void status(Player player, int amount, int capacity) {
        player.displayClientMessage(Component.translatable(
                "message.infestusfrontier.biomass.status", amount, capacity), true);
    }

    private BiomassInteractions() {}
}
