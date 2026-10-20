package org.jd.infestusfrontier.logistics.api;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;

/** A block whose faces the probe configures. Exposed as {@link LogisticsCapabilities#PORT_SWITCH}. */
public interface PortSwitch {
    /** Moves {@code face} to its next setting and says what it is now. Runs on the server. */
    Component cycle(Direction face);
}
