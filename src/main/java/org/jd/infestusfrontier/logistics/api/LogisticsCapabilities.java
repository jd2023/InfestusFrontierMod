package org.jd.infestusfrontier.logistics.api;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.jd.infestusfrontier.InfestusFrontier;
import org.jetbrains.annotations.Nullable;

/** How the probe reaches a block's ports and a Mouth's screen, and how an organ reaches the network through its Mouth. */
public final class LogisticsCapabilities {
    public static final BlockCapability<PortSwitch, @Nullable Direction> PORT_SWITCH = BlockCapability.createSided(
            ResourceLocation.fromNamespaceAndPath(InfestusFrontier.MOD_ID, "port_switch"), PortSwitch.class);
    /** A Mouth's settings screen; the probe opens it from the store standing on the Mouth. */
    public static final BlockCapability<MenuProvider, @Nullable Void> MOUTH_SCREEN = BlockCapability.createVoid(
            ResourceLocation.fromNamespaceAndPath(InfestusFrontier.MOD_ID, "mouth_screen"), MenuProvider.class);

    /** A Mouth as the organ standing on it reaches the network through it. Found on the Mouth itself. */
    public static final BlockCapability<ItemAccess, @Nullable Void> ITEM_ACCESS = BlockCapability.createVoid(
            ResourceLocation.fromNamespaceAndPath(InfestusFrontier.MOD_ID, "item_access"), ItemAccess.class);

    public static final BlockCapability<NetworkStatus, @Nullable Direction> NETWORK_STATUS =
            BlockCapability.createSided(
                    ResourceLocation.fromNamespaceAndPath(InfestusFrontier.MOD_ID, "network_status"),
                    NetworkStatus.class);

    private LogisticsCapabilities() {}
}
