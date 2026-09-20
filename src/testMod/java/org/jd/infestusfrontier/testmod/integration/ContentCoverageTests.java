package org.jd.infestusfrontier.testmod.integration;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class ContentCoverageTests {
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void noGameplayRegistrationsAtBootstrap(GameTestHelper helper) {
        var items = BuiltInRegistries.ITEM.keySet().stream()
                .filter(key -> key.getNamespace().equals("infestusfrontier"))
                .toList();
        var blocks = BuiltInRegistries.BLOCK.keySet().stream()
                .filter(key -> key.getNamespace().equals("infestusfrontier"))
                .toList();
        helper.assertTrue(items.isEmpty(), "IF-108 must not register gameplay items: " + items);
        helper.assertTrue(blocks.isEmpty(), "IF-108 must not register gameplay blocks: " + blocks);
        helper.succeed();
    }
}
