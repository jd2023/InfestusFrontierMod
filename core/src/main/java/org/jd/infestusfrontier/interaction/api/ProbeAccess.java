package org.jd.infestusfrontier.interaction.api;

import java.util.UUID;

/** Deterministic authorization for a single server-observed probe target. */
public final class ProbeAccess {
    public static final double MAX_REACH = 8.0;

    public static Refusal check(
            UUID owner,
            UUID player,
            boolean loaded,
            boolean sameDimension,
            double distanceSquared,
            double playerReach) {
        if (player == null || !Double.isFinite(distanceSquared) || distanceSquared < 0
                || !Double.isFinite(playerReach) || playerReach < 0) return Refusal.MALFORMED;
        if (!loaded) return Refusal.UNLOADED_TARGET;
        if (!sameDimension) return Refusal.REMOTE_TARGET;
        if (owner == null || !owner.equals(player)) return Refusal.WRONG_OWNER;
        double reach = Math.min(MAX_REACH, playerReach);
        return distanceSquared <= reach * reach ? Refusal.NONE : Refusal.OUT_OF_REACH;
    }

    public enum Refusal {
        NONE,
        MALFORMED,
        UNLOADED_TARGET,
        REMOTE_TARGET,
        WRONG_OWNER,
        OUT_OF_REACH
    }

    private ProbeAccess() {}
}
