package org.jd.infestusfrontier.construction.api;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;

/** Platform composition port for I000's ecology-owned world interaction. */
@FunctionalInterface
public interface CultureUse {
    InteractionResult apply(UseOnContext context);
}
