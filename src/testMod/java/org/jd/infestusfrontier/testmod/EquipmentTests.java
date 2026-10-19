package org.jd.infestusfrontier.testmod;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.ecology.EcologyModule;
import org.jd.infestusfrontier.equipment.EquipmentModule;
import org.jd.infestusfrontier.equipment.api.Reserves;
import org.jd.infestusfrontier.processing.BatchOrganMenu;
import org.jd.infestusfrontier.processing.ProcessingModule;
import org.jd.infestusfrontier.processing.api.BatchOrganContents;
import org.jd.infestusfrontier.storage.BiomassBladderMenu;
import org.jd.infestusfrontier.storage.StorageModule;

/** Dormant pieces from flesh, the Cradle that wakes them, and the Bladder that feeds them. */
@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class EquipmentTests {
    private static final BlockPos GROUND = new BlockPos(1, 1, 1);
    private static final BlockPos ORGAN = GROUND.above();
    private static final int AWAKENING_TICKS = 600;

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void fleshShapedLikeArmorMakesADormantPiece(GameTestHelper h) {
        List<ItemStack> grid = new java.util.ArrayList<>();
        for (char c : "# ####### ".toCharArray())
            grid.add(c == '#' ? new ItemStack(Items.ROTTEN_FLESH) : ItemStack.EMPTY);
        CraftingInput input = CraftingInput.of(3, 3, grid.subList(0, 9));
        var recipe = h.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, h.getLevel());
        h.assertTrue(recipe.isPresent(), "eight flesh in a chestplate shape craft");
        ItemStack made = recipe.get().value().assemble(input, h.getLevel().registryAccess());
        h.assertTrue(made.is(EquipmentModule.DORMANT_THORAX.get()), "a Dormant Thorax");
        h.assertValueEqual(((ArmorItem) made.getItem()).getDefense(), 1, "G0 chest protection");
        h.assertValueEqual(made.getMaxDamage(), 48, "G0 chest durability");
        h.assertTrue(Reserves.of(made).isEmpty(), "a dormant piece holds nothing");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 700)
    public static void theCradleWakesAPieceAsWornAsItWentIn(GameTestHelper h) {
        h.setBlock(GROUND, EcologyModule.LIVING_SUBSTRATE.get());
        h.setBlock(ORGAN, ProcessingModule.AWAKENING_CRADLE.get());
        ItemStack worn = new ItemStack(EquipmentModule.DORMANT_THORAX.get());
        worn.setDamageValue(24); // half worn
        Player loader = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
        loader.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, worn);
        EcologyTests.use(h, loader, ORGAN, Direction.UP);
        load(h, EcologyModule.SPORE_CULTURE.get(), 1);
        load(h, StorageModule.BIOMASS_BUCKET.get(), 1);
        h.runAfterDelay(1, () -> {
            BatchOrganContents started = contents(h);
            h.assertTrue(started.batch().isPresent(), "a batch runs on its own");
            h.assertValueEqual(started.biomass().stored().units(), 900, "100 BU spent on waking");
        });
        h.runAtTickTime(AWAKENING_TICKS + 2, () -> {
            ItemStack woken = contents(h).output();
            h.assertTrue(woken.is(EquipmentModule.AWAKENED_THORAX.get()), "an Awakened Thorax");
            h.assertValueEqual(woken.getMaxDamage(), 128, "G1 chest durability");
            h.assertValueEqual(woken.getDamageValue(), 64, "still half worn");
            h.assertValueEqual(((ArmorItem) woken.getItem()).getDefense(), 2, "G1 chest protection");
            var reserve = Reserves.of(woken).orElseThrow();
            h.assertValueEqual(reserve.stored(), Biomass.ZERO, "wakes empty");
            h.assertValueEqual(reserve.capacity(), EquipmentModule.RESERVE_CAPACITY, "with a 40 BU reserve");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void anAwakenedPieceInHandOpensTheScreenInsteadOfFilling(GameTestHelper h) {
        StorageTests.placeBladder(h);
        Player carrier = EcologyTests.holding(h, GameType.SURVIVAL, StorageModule.BIOMASS_BUCKET.get(), 1);
        EcologyTests.use(h, carrier, ORGAN, Direction.UP);
        Player wearer = EcologyTests.holding(h, GameType.SURVIVAL, EquipmentModule.AWAKENED_THORAX.get(), 1);
        h.assertValueEqual(
                EcologyTests.useHeld(h, wearer, ORGAN, Direction.UP),
                ItemInteractionResult.CONSUME,
                "the Bladder answers the piece itself");
        h.assertValueEqual(
                Reserves.of(wearer.getMainHandItem()).orElseThrow().stored(), Biomass.ZERO, "the piece stays empty");
        h.assertValueEqual(StorageTests.tank(h).stored(), Biomass.of(1000), "the Bladder keeps its biomass");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void theBladderFillsAnAwakenedPiece(GameTestHelper h) {
        StorageTests.placeBladder(h);
        Player carrier = EcologyTests.holding(h, GameType.SURVIVAL, StorageModule.BIOMASS_BUCKET.get(), 1);
        EcologyTests.use(h, carrier, ORGAN, Direction.UP);
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
        MenuProvider provider = (MenuProvider) h.getBlockEntity(ORGAN);
        BiomassBladderMenu menu = (BiomassBladderMenu) provider.createMenu(1, player.getInventory(), player);
        h.assertFalse(
                menu.slots.get(0).mayPlace(new ItemStack(EquipmentModule.DORMANT_THORAX.get())), "dormant refused");
        ItemStack piece = new ItemStack(EquipmentModule.AWAKENED_THORAX.get());
        h.assertTrue(menu.slots.get(0).mayPlace(piece), "awakened accepted");
        menu.slots.get(0).set(piece);
        h.assertTrue(menu.clickMenuButton(player, BiomassBladderMenu.FILL_BUTTON), "fill handled");
        h.assertValueEqual(
                Reserves.of(menu.slots.get(0).getItem()).orElseThrow().stored(), Biomass.of(40), "40 BU in the piece");
        h.assertValueEqual(StorageTests.tank(h).stored(), Biomass.of(960), "40 BU out of the Bladder");
        menu.clickMenuButton(player, BiomassBladderMenu.FILL_BUTTON);
        h.assertValueEqual(StorageTests.tank(h).stored(), Biomass.of(960), "a full piece takes no more");
        h.succeed();
    }

    private static void load(GameTestHelper h, Item item, int count) {
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, item, count);
        for (int i = 0; i < count; i++) EcologyTests.use(h, player, ORGAN, Direction.UP);
    }

    private static BatchOrganMenu menu(GameTestHelper h, Player player) {
        MenuProvider provider = (MenuProvider) h.getBlockEntity(ORGAN);
        return (BatchOrganMenu) provider.createMenu(1, player.getInventory(), player);
    }

    private static BatchOrganContents contents(GameTestHelper h) {
        var registries = h.getLevel().registryAccess();
        var tag = h.getBlockEntity(ORGAN).saveWithoutMetadata(registries);
        return BatchOrganContents.CODEC
                .parse(registries.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), tag.get("contents"))
                .getOrThrow();
    }
}
