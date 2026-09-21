package org.jd.infestusfrontier.discovery.api;

import net.minecraft.server.level.ServerPlayer;

/** Narrow server-side port used by implemented features to report real milestones. */
@FunctionalInterface
public interface DiscoveryObserver {
    void complete(ServerPlayer player, Milestone milestone);

    enum Milestone {
        CONTACT("root"),
        FIRST_COPY("first_copy"),
        SPORE_CULTURE("spore_culture"),
        SYNAPTIC_PROBE("synaptic_probe"),
        LIVING_SUBSTRATE("living_substrate"),
        CULTURE_BOWL_BATCH("culture_bowl_batch"),
        ORGAN_BUD("organ_bud"),
        DIGESTIVE_SAC("digestive_sac"),
        BIOMASS_BLADDER("biomass_bladder"),
        BIOMASS_BUCKET("biomass_bucket");

        private final String path;

        Milestone(String path) {
            this.path = path;
        }

        public String path() {
            return path;
        }
    }
}
