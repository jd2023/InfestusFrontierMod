package org.jd.infestusfrontier.storage.api;

import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.core.foundation.Outcome;
import org.jd.infestusfrontier.core.storage.BiomassTank;

/**
 * A block that holds biomass. Transfers are whole or nothing and run on the server. {@code simulate} answers without
 * changing anything. Buckets, pipes and other mods reach the same balance through {@link BiomassFluids#handler}.
 */
public interface BiomassHandler {
    /** Current balance, an immutable snapshot. */
    BiomassTank tank();

    /** Adds exactly {@code amount} or refuses. The result is the balance after the change. */
    Outcome<BiomassTank> insert(Biomass amount, boolean simulate);

    /** Removes exactly {@code amount} or refuses. The result is the balance after the change. */
    Outcome<BiomassTank> extract(Biomass amount, boolean simulate);
}
