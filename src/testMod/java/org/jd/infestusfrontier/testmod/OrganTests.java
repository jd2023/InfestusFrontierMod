package org.jd.infestusfrontier.testmod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jd.infestusfrontier.ecology.EcologyModule;
import org.jd.infestusfrontier.organ.OrganModule;

/** The Organ Bud: placement on a foundation, its drop and its lack of a block entity. */
@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class OrganTests {
    private static final BlockPos GROUND = new BlockPos(1, 1, 1);
    private static final BlockPos BUD = GROUND.above();

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void budPlacesOnlyOnLivingSubstrate(GameTestHelper h) {
        h.setBlock(GROUND, Blocks.STONE);
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, OrganModule.ORGAN_BUD_ITEM.get(), 1);
        h.placeAt(player, player.getMainHandItem(), GROUND, Direction.UP);
        h.assertBlockPresent(Blocks.AIR, BUD);
        h.assertValueEqual(player.getMainHandItem().getCount(), 1, "bud still in hand");
        h.setBlock(GROUND, EcologyModule.LIVING_SUBSTRATE.get());
        h.placeAt(player, player.getMainHandItem(), GROUND, Direction.UP);
        h.assertBlockPresent(OrganModule.ORGAN_BUD.get(), BUD);
        h.assertValueEqual(player.getMainHandItem().getCount(), 0, "bud placed");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void budDropsItself(GameTestHelper h) {
        h.setBlock(BUD, OrganModule.ORGAN_BUD.get());
        h.getLevel().destroyBlock(h.absolutePos(BUD), true);
        h.assertItemEntityPresent(OrganModule.ORGAN_BUD_ITEM.get(), BUD, 2.0);
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void budHasNoBlockEntity(GameTestHelper h) {
        h.setBlock(BUD, OrganModule.ORGAN_BUD.get());
        h.assertTrue(h.getLevel().getBlockEntity(h.absolutePos(BUD)) == null, "a bud keeps no block entity");
        h.succeed();
    }
}
