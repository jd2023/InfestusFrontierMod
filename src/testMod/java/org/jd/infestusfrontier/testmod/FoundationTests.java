package org.jd.infestusfrontier.testmod;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jd.infestusfrontier.foundation.TickQuota;

@Mod("infestusfrontier_tests")
@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class FoundationTests {
    @GameTest(templateNamespace="infestusfrontier_tests", template="empty")
    public static void productionModAndPureCoreLoadTogether(GameTestHelper h) {
        h.assertTrue(ModList.get().isLoaded("infestusfrontier"), "Production mod must actually load");
        var quota = new TickQuota(2);
        long now = h.getLevel().getGameTime();
        h.assertTrue(quota.take(now) && quota.take(now) && !quota.take(now), "Core policy is available in the real server");
        h.runAfterDelay(2, () -> {
            h.assertTrue(quota.take(h.getLevel().getGameTime()), "A later real server tick gets fresh capacity");
            h.succeed();
        });
    }
}
