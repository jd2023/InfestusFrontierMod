package org.jd.infestusfrontier.testmod;

import java.util.Collection;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.items.IItemHandler;
import org.jd.infestusfrontier.logistics.LogisticsModule;

/** Runs only in a profile with the real optional storage mod on the runtime classpath. */
@GameTestHolder("infestusfrontier_tests")
public final class DrawerCompatibilityTests {
    private DrawerCompatibilityTests() {}

    @GameTestGenerator
    public static Collection<TestFunction> storageDrawers() {
        if (!ModList.get().isLoaded("storagedrawers")) return List.of();
        return List.of(new TestFunction(
                "drawers_compat",
                "mouthsTransferThroughARealDrawerController",
                "infestusfrontier_tests:empty",
                160,
                0,
                true,
                DrawerCompatibilityTests::transfer));
    }

    private static void transfer(GameTestHelper h) {
        BlockPos drawer = new BlockPos(2, 2, 2);
        BlockPos controller = drawer.east();
        BlockPos source = controller.east(2);
        BlockPos target = drawer.west(2);
        h.setBlock(drawer, drawerBlock("oak_full_drawers_1"));
        h.setBlock(controller, drawerBlock("controller"));
        h.setBlock(source, Blocks.CHEST);
        ((ChestBlockEntity) h.getBlockEntity(source)).setItem(0, new ItemStack(Items.BONE, 24));
        h.setBlock(
                controller.east(),
                LogisticsModule.INTAKE_MOUTH
                        .get()
                        .defaultBlockState()
                        .setValue(BlockStateProperties.FACING, Direction.EAST));
        h.runAfterDelay(80, () -> {
            var inventory =
                    h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(drawer), Direction.WEST);
            h.assertTrue(inventory != null, "real drawer exposes its standard item handler");
            h.assertValueEqual(count(inventory), 24, "controller routes all items into its drawer");
            h.assertTrue(((ChestBlockEntity) h.getBlockEntity(source)).isEmpty(), "source extracted once");
            h.setBlock(target, Blocks.CHEST);
            h.setBlock(
                    drawer.west(),
                    LogisticsModule.OUTPUT_MOUTH
                            .get()
                            .defaultBlockState()
                            .setValue(BlockStateProperties.FACING, Direction.WEST));
            h.runAfterDelay(40, () -> {
                h.assertValueEqual(
                        ((ChestBlockEntity) h.getBlockEntity(target)).getItem(0).getCount(),
                        24,
                        "output mouth retrieves the drawer contents");
                h.assertTrue(count(inventory) == 0, "drawer emptied without a duplicate");
                h.succeed();
            });
        });
    }

    private static int count(IItemHandler inventory) {
        int total = 0;
        for (int slot = 0; slot < inventory.getSlots(); slot++)
            total += inventory.getStackInSlot(slot).getCount();
        return total;
    }

    private static Block drawerBlock(String path) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("storagedrawers", path));
    }
}
