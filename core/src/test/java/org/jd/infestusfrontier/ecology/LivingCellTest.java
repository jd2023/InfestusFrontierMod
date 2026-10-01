package org.jd.infestusfrontier.ecology;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.jd.infestusfrontier.ecology.api.LivingCell;
import org.jd.infestusfrontier.ecology.api.LivingCell.Framework;
import org.jd.infestusfrontier.ecology.api.LivingCell.Function;
import org.jd.infestusfrontier.ecology.api.LivingCell.Host;
import org.jd.infestusfrontier.ecology.api.LivingCell.Lining;
import org.jd.infestusfrontier.ecology.api.LivingCell.Maturity;
import org.jd.infestusfrontier.ecology.api.LivingCell.MutationStatus;
import org.jd.infestusfrontier.ecology.api.LivingCell.Pigment;
import org.jd.infestusfrontier.ecology.api.LivingCell.Treatment;
import org.junit.jupiter.api.Test;

final class LivingCellTest {
    private static final UUID OWNER = UUID.fromString("2e6ca606-c3bb-4f9a-a70b-0a30130c0c65");

    @Test
    void deliberateGrowthStopsAtMature() {
        LivingCell basic = LivingCell.basic(OWNER);
        LivingCell young = basic.mutate(new Treatment.Grow()).cell();
        LivingCell mature = young.mutate(new Treatment.Grow()).cell();

        assertEquals(Maturity.YOUNG, young.maturity());
        assertEquals(Maturity.MATURE, mature.maturity());
        var refused = mature.mutate(new Treatment.Grow());
        assertEquals(MutationStatus.ALREADY_APPLIED, refused.status());
        assertEquals(mature, refused.cell());
    }

    @Test
    void compatibleFunctionMutationPreservesIndependentAnatomy() {
        LivingCell reinforced = matureCell()
                .mutate(new Treatment.SetFramework(Framework.BONE_RIBBED)).cell()
                .mutate(new Treatment.SetLining(Lining.SEALED_MEMBRANE)).cell()
                .mutate(new Treatment.SetPigment(Pigment.CYAN)).cell();

        var result = reinforced.mutate(new Treatment.SetFunction(Function.BIOMASS_VEIN));

        assertTrue(result.applied());
        assertEquals(Function.BIOMASS_VEIN, result.cell().function());
        assertEquals(Framework.BONE_RIBBED, result.cell().framework());
        assertEquals(Lining.SEALED_MEMBRANE, result.cell().lining());
        assertEquals(Pigment.CYAN, result.cell().pigment());
        assertEquals(OWNER, result.cell().owner());
        assertEquals(Maturity.MATURE, result.cell().maturity());
    }

    @Test
    void refusedMutationReturnsTheUnchangedCell() {
        LivingCell young = LivingCell.basic(OWNER).mutate(new Treatment.Grow()).cell();
        var tooYoung = young.mutate(new Treatment.SetFunction(Function.LUMEN));
        assertEquals(MutationStatus.REQUIRES_MATURE, tooYoung.status());
        assertEquals(young, tooYoung.cell());

        LivingCell sealed = matureCell().mutate(new Treatment.SetLining(Lining.SEALED_MEMBRANE)).cell();
        var incompatible = sealed.mutate(new Treatment.SetFunction(Function.CULTIVATION));
        assertFalse(incompatible.applied());
        assertEquals(MutationStatus.INCOMPATIBLE, incompatible.status());
        assertEquals(sealed, incompatible.cell());

        var skippedRibGrade = matureCell().mutate(new Treatment.SetFramework(Framework.TEMPERED_RIBBED));
        assertEquals(MutationStatus.INCOMPATIBLE, skippedRibGrade.status());
        assertEquals(Framework.UNREINFORCED, skippedRibGrade.cell().framework());
    }

    @Test
    void incompatibleNativeHostMutationPreservesEveryProperty() {
        LivingCell anchored = matureCell()
                .mutate(new Treatment.SetHost(Host.ANCHORED)).cell()
                .mutate(new Treatment.Grow(Host.ANCHORED)).cell()
                .mutate(new Treatment.SetLining(Lining.SPATIAL_WEAVE)).cell()
                .mutate(new Treatment.SetPigment(Pigment.PURPLE)).cell();

        var result = anchored.mutate(new Treatment.SetHost(Host.THERMAL));

        assertEquals(MutationStatus.INCOMPATIBLE, result.status());
        assertEquals(anchored, result.cell());
    }

    @Test
    void changingNativeHostRequiresLocalMaturationAndPreservesIndependentAnatomy() {
        for (Host host : new Host[] {Host.THERMAL, Host.ANCHORED}) {
            LivingCell original = matureCell()
                    .mutate(new Treatment.SetFramework(Framework.BONE_RIBBED)).cell()
                    .mutate(new Treatment.SetLining(Lining.SEALED_MEMBRANE)).cell()
                    .mutate(new Treatment.SetPigment(Pigment.CYAN)).cell();
            var mutation = original.mutate(new Treatment.SetHost(host));
            assertTrue(mutation.applied());
            LivingCell pending = mutation.cell();
            assertEquals(Maturity.YOUNG, pending.maturity());
            assertEquals(host, pending.host());
            assertEquals(original.framework(), pending.framework());
            assertEquals(original.lining(), pending.lining());
            assertEquals(original.pigment(), pending.pigment());
            assertEquals(original.owner(), pending.owner());
            assertEquals(MutationStatus.REQUIRES_MATURE,
                    pending.mutate(new Treatment.SetFunction(Function.LUMEN)).status());
            var importedGrowth = pending.mutate(new Treatment.Grow());
            assertEquals(MutationStatus.INCOMPATIBLE, importedGrowth.status());
            assertEquals(pending, importedGrowth.cell());
            Host wrongNative = host == Host.THERMAL ? Host.ANCHORED : Host.THERMAL;
            assertEquals(pending, pending.mutate(new Treatment.Grow(wrongNative)).cell());
            var localGrowth = pending.mutate(new Treatment.Grow(host));
            assertTrue(localGrowth.applied());
            assertEquals(new LivingCell(Maturity.MATURE, host, Function.PLAIN,
                    original.framework(), original.lining(), original.pigment(), OWNER), localGrowth.cell());
            assertTrue(localGrowth.cell().mutate(new Treatment.SetFunction(Function.LUMEN)).applied());
        }
    }

    private static LivingCell matureCell() {
        return LivingCell.basic(OWNER)
                .mutate(new Treatment.Grow()).cell()
                .mutate(new Treatment.Grow()).cell();
    }
}
