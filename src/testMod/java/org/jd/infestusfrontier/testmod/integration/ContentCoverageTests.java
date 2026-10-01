package org.jd.infestusfrontier.testmod.integration;

import java.util.List;
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
        checkRegistry(helper, ContentRequirements.read().emptyRegistry(), items, blocks);
        helper.succeed();
    }

    private static void checkRegistry(GameTestHelper helper, boolean empty, List<?> items, List<?> blocks) {
        if (!empty) return;
        helper.assertTrue(items.isEmpty(), "IF-108 must not register gameplay items: " + items);
        helper.assertTrue(blocks.isEmpty(), "IF-108 must not register gameplay blocks: " + blocks);
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void contributedContentReplacesEmptyRegistryExpectation(GameTestHelper helper) {
        checkRegistry(helper, false, List.of("infestusfrontier:contributed_item"), List.of("infestusfrontier:contributed_block"));
        helper.succeed();
    }
}
