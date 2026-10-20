package org.jd.infestusfrontier.logistics.api;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.jd.infestusfrontier.InfestusFrontier;
import org.jetbrains.annotations.Nullable;

/** How the probe reaches a block's ports. */
public final class LogisticsCapabilities {
    public static final BlockCapability<PortSwitch, @Nullable Direction> PORT_SWITCH = BlockCapability.createSided(
            ResourceLocation.fromNamespaceAndPath(InfestusFrontier.MOD_ID, "port_switch"), PortSwitch.class);

    private LogisticsCapabilities() {}
}
