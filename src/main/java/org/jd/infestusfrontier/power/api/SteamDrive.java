package org.jd.infestusfrontier.power.api;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.jd.infestusfrontier.core.foundation.Outcome;

/** A physical driver pays for one iron-grade cut. Simulation never changes the store. */
public interface SteamDrive {
    BlockCapability<SteamDrive, Direction> BLOCK = BlockCapability.createSided(
            ResourceLocation.fromNamespaceAndPath("infestusfrontier", "steam_drive"), SteamDrive.class);
    int CUT_STEAM = 40;
    org.jd.infestusfrontier.core.foundation.Refusal UNROOTED =
            new org.jd.infestusfrontier.core.foundation.Refusal("power.unrooted");
    org.jd.infestusfrontier.core.foundation.Refusal NO_STEAM =
            new org.jd.infestusfrontier.core.foundation.Refusal("power.no_steam");

    Outcome<Integer> pay(boolean simulate);
}
