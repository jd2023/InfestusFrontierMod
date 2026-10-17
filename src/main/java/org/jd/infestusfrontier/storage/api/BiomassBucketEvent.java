package org.jd.infestusfrontier.storage.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;

/** Posted on the NeoForge bus after a bucket moved biomass into ({@code inserted}) or out of an organ block. */
public final class BiomassBucketEvent extends Event {
    private final Player player;
    private final ResourceLocation block;
    private final boolean inserted;

    public BiomassBucketEvent(Player player, ResourceLocation block, boolean inserted) {
        this.player = player;
        this.block = block;
        this.inserted = inserted;
    }

    public Player player() {
        return player;
    }

    public ResourceLocation block() {
        return block;
    }

    public boolean inserted() {
        return inserted;
    }
}
