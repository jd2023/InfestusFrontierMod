package org.jd.infestusfrontier.testmod.integration;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.slf4j.Logger;

@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class IntegrationBootstrapTests {
    private static final Logger LOGGER = LogUtils.getLogger();

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void reportsRealLoadedServerState(GameTestHelper helper) {
        long gameTime = helper.getLevel().getGameTime();
        helper.assertTrue(helper.getLevel().hasChunkAt(helper.absolutePos(BlockPos.ZERO)), "GameTest chunk must be loaded");
        LOGGER.info("INFESTUS_INTEGRATION_GAME_STATE dimension={} gameTime={} loaded=true",
                helper.getLevel().dimension().location(), gameTime);
        ContentAssertion.passGameTest(helper, "infestusfrontier_tests:integration.server_state");
    }
}
