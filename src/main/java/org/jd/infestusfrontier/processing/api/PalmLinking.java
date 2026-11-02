package org.jd.infestusfrontier.processing.api;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.jd.infestusfrontier.InfestusFrontier;
import org.jd.infestusfrontier.core.foundation.Refusal;
import org.jetbrains.annotations.Nullable;

/** How the probe reaches a Palm to give it a supply link (BLOCK_CATALOG T1-41, stage 2). */
public final class PalmLinking {
    /** The farthest a linked store may stand from its Palm, centre to centre, in blocks. */
    public static final int RANGE = 8;

    /** The Palm to link is not at its position, or is in a chunk that is not loaded. */
    public static final Refusal GONE = new Refusal("processing.palm_link_gone");

    public static final BlockCapability<PalmLink, @Nullable Void> PALM_LINK = BlockCapability.createVoid(
            ResourceLocation.fromNamespaceAndPath(InfestusFrontier.MOD_ID, "palm_link"), PalmLink.class);

    private PalmLinking() {}

    /** Links the Palm at {@code palm} to {@code target} in {@code level}, loading no chunk to find the Palm. */
    public static Optional<Refusal> link(Level level, BlockPos palm, BlockPos target) {
        if (!level.hasChunkAt(palm)) return Optional.of(GONE);
        PalmLink found = level.getCapability(PALM_LINK, palm, null);
        return found == null ? Optional.of(GONE) : found.link(target);
    }
}
