package org.jd.infestusfrontier.testmod;

import java.util.List;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Four ingredients that each leave a different tool behind when crafted, so a recipe can leave three kinds of remainder
 * (the most a Shaping Palm's tray holds) or four, and a remainder that does not stack. The game has no vanilla recipe
 * with more than three kinds. Only the recipes in {@code data/infestusfrontier_tests/recipe} use them.
 */
final class TestItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("infestusfrontier_tests");

    static final List<DeferredItem<Item>> HUSKS = List.of(
            husk("husk_pickaxe", Items.WOODEN_PICKAXE),
            husk("husk_shovel", Items.WOODEN_SHOVEL),
            husk("husk_axe", Items.WOODEN_AXE),
            husk("husk_hoe", Items.WOODEN_HOE));

    private TestItems() {}

    private static DeferredItem<Item> husk(String name, Item remainder) {
        return ITEMS.registerItem(name, properties -> new Item(properties.craftRemainder(remainder)));
    }

    static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}
