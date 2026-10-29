package org.jd.infestusfrontier.testmod;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.defense.DefenseModule;
import org.jd.infestusfrontier.ecology.EcologyModule;
import org.jd.infestusfrontier.storage.api.BiomassCapabilities;

@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class DefenseBudgetTests {
    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            template = "empty",
            batch = "defense_budget",
            timeoutTicks = 80)
    public static void exhaustedServerBudgetDelaysTheStrikeWithoutSpending(GameTestHelper h) {
        BlockPos pos = new BlockPos(4, 2, 4);
        h.setBlock(pos.below(), EcologyModule.LIVING_SUBSTRATE.get());
        h.setBlock(pos, DefenseModule.SPINE_SENTRY.get());
        var sentry = h.getLevel().getCapability(BiomassCapabilities.BLOCK, h.absolutePos(pos), null);
        sentry.insert(Biomass.of(100), false);
        var prey = h.spawn(EntityType.ZOGLIN, new BlockPos(7, 2, 4));
        prey.setNoAi(true);
        prey.setNoGravity(true);
        float health = prey.getHealth();
        var server = h.getLevel().getServer();
        var quota = server.overworld().getData(DefenseModule.SCANS);
        while (quota.take(server.getTickCount())) {}
        tick(h, pos, h.getBlockEntity(pos).getType());
        h.assertValueEqual(prey.getHealth(), health, "denied scan cannot strike");
        h.assertValueEqual(sentry.tank().stored(), Biomass.of(100), "denied scan cannot spend");
        h.runAfterDelay(40, () -> {
            h.assertTrue(prey.getHealth() < health, "normal ticks resume the strike after admission returns");
            h.succeed();
        });
    }

    private static <T extends net.minecraft.world.level.block.entity.BlockEntity> void tick(
            GameTestHelper h, BlockPos pos, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        var state = h.getBlockState(pos);
        var block = (net.minecraft.world.level.block.EntityBlock) state.getBlock();
        var ticker = block.getTicker(h.getLevel(), state, type);
        ticker.tick(h.getLevel(), h.absolutePos(pos), state, type.getBlockEntity(h.getLevel(), h.absolutePos(pos)));
    }
}
