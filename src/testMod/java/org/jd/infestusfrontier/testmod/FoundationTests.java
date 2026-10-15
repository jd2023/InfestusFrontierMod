package org.jd.infestusfrontier.testmod;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jd.infestusfrontier.foundation.TickQuota;
import org.jd.infestusfrontier.ui.Bounds;

@Mod("infestusfrontier_tests")
@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class FoundationTests {
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void productionModCoreAndUiLoadTogether(GameTestHelper h) {
        h.assertTrue(ModList.get().isLoaded("infestusfrontier"), "Production mod must load");
        h.assertTrue(ModList.get().isLoaded("modonomicon"), "Guide library must load");
        h.assertTrue(ModList.get().isLoaded("geckolib"), "Model library must load");
        h.assertTrue(new Bounds(0, 0, 4, 4).contains(3, 3), "ui module is on the mod classpath");
        var quota = new TickQuota(2);
        long now = h.getLevel().getGameTime();
        h.assertTrue(quota.take(now) && quota.take(now) && !quota.take(now), "core policy runs in the real server");
        h.runAfterDelay(2, () -> {
            h.assertTrue(quota.take(h.getLevel().getGameTime()), "a later server tick gets fresh capacity");
            h.succeed();
        });
    }
}
