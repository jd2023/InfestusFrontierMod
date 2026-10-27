package org.jd.infestusfrontier.testmod;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jd.infestusfrontier.ecology.EcologyModule;
import org.jd.infestusfrontier.interaction.InteractionModule;
import org.jd.infestusfrontier.logistics.LogisticsModule;
import org.jd.infestusfrontier.organ.OrganModule;
import org.jd.infestusfrontier.organ.api.Organs;
import org.jd.infestusfrontier.processing.ProcessingModule;
import org.jd.infestusfrontier.storage.StorageModule;

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

    /** Every organ takes about 18 seconds with a hand and 1.5 with the Surgical Blade, and comes out whole. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void organsAreHardToBreakAndTheBladeCutsThemFast(GameTestHelper h) {
        BlockPos at = h.absolutePos(BUD);
        Player hand = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
        Player surgeon = EcologyTests.holding(h, GameType.SURVIVAL, InteractionModule.SURGICAL_BLADE.get(), 1);
        hand.setOnGround(true);
        surgeon.setOnGround(true);
        List<Block> organs = new ArrayList<>();
        BuiltInRegistries.BLOCK.getTagOrEmpty(Organs.ORGANS).forEach(holder -> organs.add(holder.value()));
        h.assertValueEqual(organs.size(), 21, "twenty-one organs");
        for (String id : List.of(
                "digestive_sac",
                "biomass_bladder",
                "organ_bud",
                "culture_bowl",
                "membrane_rack",
                "bone_loom",
                "awakening_cradle",
                "bio_furnace",
                "activation_cyst",
                "compost_gland",
                "vascular_junction",
                "reservoir_cell",
                "seed_pouch",
                "planting_proboscis",
                "harvest_corolla",
                "sensor_polyp",
                "feeding_lobe",
                "yield_lobe",
                "filter_valve",
                "spine_sentry"))
            h.assertTrue(
                    organs.contains(
                            BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("infestusfrontier", id))),
                    id + " is an organ");
        for (Block organ : organs) {
            BlockState state = organ.defaultBlockState();
            h.assertValueEqual(state.getDestroySpeed(h.getLevel(), at), Organs.HARDNESS, organ + " hardness");
            int byHand = (int) Math.ceil(1 / state.getDestroyProgress(hand, h.getLevel(), at));
            int byBlade = (int) Math.ceil(1 / state.getDestroyProgress(surgeon, h.getLevel(), at));
            h.assertTrue(byHand >= 360, organ + " takes at least 18 s by hand, not " + byHand + " ticks");
            h.assertTrue(byBlade <= 30, organ + " takes at most 1.5 s with the blade, not " + byBlade + " ticks");
            List<ItemStack> drops = Block.getDrops(state, h.getLevel(), at, null, null, ItemStack.EMPTY);
            h.assertTrue(
                    drops.stream().anyMatch(drop -> drop.is(organ.asItem()))
                            && drops.stream().noneMatch(drop -> drop.is(EcologyModule.DEAD_TISSUE.get())),
                    organ + " comes out whole, not " + drops);
        }
        for (Block outside : List.of(
                StorageModule.CAPSULE_STEM.get(),
                EcologyModule.LIVING_SUBSTRATE.get(),
                LogisticsModule.MOUTH.get(),
                LogisticsModule.BIOMASS_VEIN.get()))
            h.assertFalse(outside.defaultBlockState().is(Organs.ORGANS), outside + " is no organ");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void ironABoneAndASheetMakeTheBlade(GameTestHelper h) {
        ItemStack blade = M1RouteTests.craft(
                h,
                List.of(
                        new ItemStack(Items.IRON_INGOT),
                        new ItemStack(Items.BONE),
                        new ItemStack(ProcessingModule.MEMBRANE_SHEET.get())),
                InteractionModule.SURGICAL_BLADE.get());
        h.assertValueEqual(blade.getMaxDamage(), 64, "64 uses");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void budHasNoBlockEntity(GameTestHelper h) {
        h.setBlock(BUD, OrganModule.ORGAN_BUD.get());
        h.assertTrue(h.getLevel().getBlockEntity(h.absolutePos(BUD)) == null, "a bud keeps no block entity");
        h.succeed();
    }
}
