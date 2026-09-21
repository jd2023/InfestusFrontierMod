package org.jd.infestusfrontier.ecology.api;

import java.util.Objects;
import java.util.UUID;

/** Immutable anatomy for one substrate cell. */
public record LivingCell(
        Maturity maturity,
        Host host,
        Function function,
        Framework framework,
        Lining lining,
        Pigment pigment,
        UUID owner) {

    public LivingCell {
        Objects.requireNonNull(maturity, "maturity");
        Objects.requireNonNull(host, "host");
        Objects.requireNonNull(function, "function");
        Objects.requireNonNull(framework, "framework");
        Objects.requireNonNull(lining, "lining");
        Objects.requireNonNull(pigment, "pigment");
        Objects.requireNonNull(owner, "owner");
    }

    public static LivingCell basic(UUID owner) {
        return new LivingCell(Maturity.BASIC, Host.ORDINARY, Function.PLAIN,
                Framework.UNREINFORCED, Lining.NONE, Pigment.UNDYED, owner);
    }

    public MutationResult mutate(Treatment treatment) {
        Objects.requireNonNull(treatment, "treatment");
        if (treatment instanceof Treatment.Grow growth) {
            if (host != Host.ORDINARY && host != growth.environment()) return refused(MutationStatus.INCOMPATIBLE);
            return switch (maturity) {
                case BASIC -> applied(new LivingCell(Maturity.YOUNG, host, function, framework, lining, pigment, owner));
                case YOUNG -> applied(new LivingCell(Maturity.MATURE, host, function, framework, lining, pigment, owner));
                case MATURE -> refused(MutationStatus.ALREADY_APPLIED);
            };
        }
        if (treatment instanceof Treatment.SetPigment change) {
            Pigment requested = Objects.requireNonNull(change.pigment(), "pigment");
            return requested == pigment
                    ? refused(MutationStatus.ALREADY_APPLIED)
                    : applied(new LivingCell(maturity, host, function, framework, lining, requested, owner));
        }
        if (treatment instanceof Treatment.SetFramework change) {
            Framework requested = Objects.requireNonNull(change.framework(), "framework");
            if (requested == framework) return refused(MutationStatus.ALREADY_APPLIED);
            if (requested.ordinal() != framework.ordinal() + 1) return refused(MutationStatus.INCOMPATIBLE);
            return applied(new LivingCell(maturity, host, function, requested, lining, pigment, owner));
        }
        if (treatment instanceof Treatment.SetLining change) {
            Lining requested = Objects.requireNonNull(change.lining(), "lining");
            if (requested == lining) return refused(MutationStatus.ALREADY_APPLIED);
            if (!compatible(host, function, requested)) return refused(MutationStatus.INCOMPATIBLE);
            return applied(new LivingCell(maturity, host, function, framework, requested, pigment, owner));
        }
        if (treatment instanceof Treatment.SetFunction change) {
            Function requested = Objects.requireNonNull(change.function(), "function");
            if (requested == function) return refused(MutationStatus.ALREADY_APPLIED);
            if (maturity != Maturity.MATURE) return refused(MutationStatus.REQUIRES_MATURE);
            if (!compatible(host, requested, lining)) return refused(MutationStatus.INCOMPATIBLE);
            return applied(new LivingCell(maturity, host, requested, framework, lining, pigment, owner));
        }
        if (treatment instanceof Treatment.SetHost change) {
            Host requested = Objects.requireNonNull(change.host(), "host");
            if (requested == host) return refused(MutationStatus.ALREADY_APPLIED);
            if (maturity != Maturity.MATURE) return refused(MutationStatus.REQUIRES_MATURE);
            if (function != Function.PLAIN || !compatible(requested, function, lining)) {
                return refused(MutationStatus.INCOMPATIBLE);
            }
            Maturity nextMaturity = requested == Host.ORDINARY ? maturity : Maturity.YOUNG;
            return applied(new LivingCell(nextMaturity, requested, function, framework, lining, pigment, owner));
        }
        throw new IllegalArgumentException("Unknown treatment " + treatment.getClass().getName());
    }

    private MutationResult applied(LivingCell changed) {
        return new MutationResult(MutationStatus.APPLIED, changed);
    }

    private MutationResult refused(MutationStatus status) {
        return new MutationResult(status, this);
    }

    private static boolean compatible(Host host, Function function, Lining lining) {
        if (host == Host.THERMAL && lining == Lining.SPATIAL_WEAVE) return false;
        if (host == Host.ANCHORED && lining == Lining.HEAT_LINING) return false;
        return switch (function) {
            case CULTIVATION, AQUACULTURE -> lining == Lining.NONE;
            case THERMAL_CULTIVATION -> host == Host.THERMAL && lining == Lining.NONE;
            case CHORUS_ORCHARD -> host == Host.ANCHORED && lining == Lining.NONE;
            case STEAM_VEIN -> lining == Lining.HEAT_LINING;
            default -> true;
        };
    }

    public enum Maturity { BASIC, YOUNG, MATURE }

    public enum Host { ORDINARY, THERMAL, ANCHORED }

    public enum Function {
        PLAIN,
        LUMEN,
        BIOMASS_VEIN,
        FLUID_VEIN,
        ITEM_VEIN,
        NERVE,
        STEAM_VEIN,
        CONDUCTIVE,
        CULTIVATION,
        WORK_BED,
        AQUACULTURE,
        THERMAL_CULTIVATION,
        CHORUS_ORCHARD,
        DIGESTIVE,
        RESTRAINING,
        TRAVEL,
        RESTORATIVE,
        FUEL_PAPILLA,
        SURVEYED,
        WORKER_WAYPOINT
    }

    public enum Framework { UNREINFORCED, BONE_RIBBED, TEMPERED_RIBBED }

    public enum Lining { NONE, SEALED_MEMBRANE, HEAT_LINING, SPATIAL_WEAVE }

    public enum Pigment {
        UNDYED,
        WHITE,
        ORANGE,
        MAGENTA,
        LIGHT_BLUE,
        YELLOW,
        LIME,
        PINK,
        GRAY,
        LIGHT_GRAY,
        CYAN,
        PURPLE,
        BLUE,
        BROWN,
        GREEN,
        RED,
        BLACK
    }

    public sealed interface Treatment {
        /** Environment is resolved by the server adapter, never supplied by a client intent. */
        record Grow(Host environment) implements Treatment {
            public Grow {
                Objects.requireNonNull(environment, "environment");
            }

            public Grow() {
                this(Host.ORDINARY);
            }
        }
        record SetHost(Host host) implements Treatment {}
        record SetFunction(Function function) implements Treatment {}
        record SetFramework(Framework framework) implements Treatment {}
        record SetLining(Lining lining) implements Treatment {}
        record SetPigment(Pigment pigment) implements Treatment {}
    }

    public enum MutationStatus { APPLIED, ALREADY_APPLIED, REQUIRES_MATURE, INCOMPATIBLE }

    public record MutationResult(MutationStatus status, LivingCell cell) {
        public MutationResult {
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(cell, "cell");
        }

        public boolean applied() {
            return status == MutationStatus.APPLIED;
        }
    }
}
