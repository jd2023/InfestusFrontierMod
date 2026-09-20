package org.jd.infestusfrontier;

import net.neoforged.fml.common.Mod;
import org.jd.infestusfrontier.integration.RuntimeIdentity;

/** Production composition root. No prototype content is registered here. */
@Mod(InfestusFrontier.MOD_ID)
public final class InfestusFrontier {
    public static final String MOD_ID = "infestusfrontier";

    public InfestusFrontier() {
        RuntimeIdentity.install();
    }
}
