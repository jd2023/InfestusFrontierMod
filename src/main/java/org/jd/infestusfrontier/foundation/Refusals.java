package org.jd.infestusfrontier.foundation;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.jd.infestusfrontier.core.foundation.Refusal;

/** Shows a refusal to the player who caused it, above the hotbar. */
public final class Refusals {
    private Refusals() {}

    public static Component text(Refusal refusal) {
        return Component.translatable("infestusfrontier.refusal." + refusal.key());
    }

    public static void show(Player player, Refusal refusal) {
        player.displayClientMessage(text(refusal), true);
    }
}
