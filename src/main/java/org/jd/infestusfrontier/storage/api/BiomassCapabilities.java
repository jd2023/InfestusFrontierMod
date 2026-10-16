package org.jd.infestusfrontier.storage.api;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.jd.infestusfrontier.InfestusFrontier;
import org.jetbrains.annotations.Nullable;

/** How other modules and items reach a block's biomass. */
public final class BiomassCapabilities {
    public static final BlockCapability<BiomassHandler, @Nullable Direction> BLOCK = BlockCapability.createSided(
            ResourceLocation.fromNamespaceAndPath(InfestusFrontier.MOD_ID, "biomass"), BiomassHandler.class);

    private BiomassCapabilities() {}
}
