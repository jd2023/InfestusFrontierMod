package org.jd.infestusfrontier.processing.api;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import org.jd.infestusfrontier.core.foundation.Refusal;

/**
 * A Shaping Palm's supply link as the probe reaches it. Exposed as {@link PalmLinking#PALM_LINK}. Runs on the server.
 */
public interface PalmLink {
    /** True when the Palm has been given Reach and may hold a link. */
    boolean hasReach();

    /**
     * Points the Palm's one supply link at {@code target}. Empty when the link is made; otherwise the refusal says
     * which condition failed and nothing changed. The target must be within {@link PalmLinking#RANGE} blocks, in a
     * loaded chunk, and have an item handler; nothing is loaded or searched to find out.
     */
    Optional<Refusal> link(BlockPos target);
}
