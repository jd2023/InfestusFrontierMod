package org.jd.infestusfrontier.testmod;

import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jd.infestusfrontier.core.equipment.LivingTool;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.core.foundation.Outcome;
import org.jd.infestusfrontier.core.storage.BiomassTank;
import org.jd.infestusfrontier.ecology.EcologyModule;
import org.jd.infestusfrontier.equipment.EquipmentModule;
import org.jd.infestusfrontier.equipment.ToolMutation;
import org.jd.infestusfrontier.equipment.api.Reserves;
import org.jd.infestusfrontier.logistics.LogisticsModule;
import org.jd.infestusfrontier.processing.ProcessingModule;
import org.jd.infestusfrontier.processing.api.BatchOrganContents;
import org.jd.infestusfrontier.storage.BiomassBladderMenu;
import org.jd.infestusfrontier.storage.StorageModule;

/**
 * Bone tools from plates (ITEM_CATALOG I015), the Cradle that wakes them (I016), the reserve they mend from, and the
 * Contractile graft that breaks the blocks round the one they mine.
 */
@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class LivingToolTests {
    private static final BlockPos GROUND = new BlockPos(1, 1, 1);
    private static final BlockPos ORGAN = GROUND.above();
    private static final int AWAKENING_TICKS = 600;

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void platesOnASticksMakeADormantPickThatDigsWorseThanWood(GameTestHelper h) {
        List<ItemStack> grid = new java.util.ArrayList<>();
        for (char c : ("###" + " | " + " | ").toCharArray())
            grid.add(
                    c == '#'
                            ? new ItemStack(ProcessingModule.BONE_PLATE.get())
                            : c == '|' ? new ItemStack(Items.STICK) : ItemStack.EMPTY);
        CraftingInput input = CraftingInput.of(3, 3, grid.subList(0, 9));
        var recipe = h.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, h.getLevel());
        h.assertTrue(recipe.isPresent(), "three plates over a stick craft");
        ItemStack made = recipe.get().value().assemble(input, h.getLevel().registryAccess());
        h.assertTrue(made.is(EquipmentModule.DORMANT_PICK.get()), "a Dormant Pick");
        h.assertValueEqual(made.getMaxDamage(), LivingTool.DORMANT_USES, "12 blocks");
        h.assertTrue(made.isCorrectToolForDrops(Blocks.STONE.defaultBlockState()), "digs stone as wood does");
        h.assertFalse(made.isCorrectToolForDrops(Blocks.IRON_ORE.defaultBlockState()), "not iron ore");
        h.assertTrue(made.getDestroySpeed(Blocks.STONE.defaultBlockState()) < 2.0F, "slower than a wooden pick");
        h.assertTrue(
                made.getMaxDamage() < new ItemStack(Items.WOODEN_PICKAXE).getMaxDamage(), "wears sooner than wood");
        h.assertFalse(made.isCorrectToolForDrops(Blocks.DIRT.defaultBlockState()), "a pick is no spade");
        h.assertTrue(Reserves.of(made).isEmpty(), "a dormant tool holds nothing");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void everyDormantToolIsWeakerAndShorterLivedThanItsWoodenKin(GameTestHelper h) {
        List<Item> wooden = List.of(
                Items.WOODEN_PICKAXE, Items.WOODEN_AXE, Items.WOODEN_SHOVEL, Items.WOODEN_HOE, Items.WOODEN_SWORD);
        for (int i = 0; i < wooden.size(); i++) {
            ItemStack dormant =
                    new ItemStack(EquipmentModule.DORMANT_TOOLS.get(i).get());
            ItemStack wood = new ItemStack(wooden.get(i));
            String name = dormant.getItem().getDescriptionId();
            h.assertTrue(dormant.getMaxDamage() < wood.getMaxDamage() / 2, name + " breaks in under half the uses");
            h.assertTrue(
                    modifier(dormant, Attributes.ATTACK_DAMAGE) < modifier(wood, Attributes.ATTACK_DAMAGE),
                    name + " hits softer");
            h.assertTrue(
                    modifier(dormant, Attributes.ATTACK_SPEED) <= modifier(wood, Attributes.ATTACK_SPEED),
                    name + " swings no faster");
            h.assertTrue(
                    dormant.getEnchantmentValue() < wood.getEnchantmentValue(), name + " takes worse enchantments");
        }
        h.assertTrue(
                new ItemStack(EquipmentModule.DORMANT_PICK.get()).getDestroySpeed(Blocks.STONE.defaultBlockState())
                        < new ItemStack(Items.WOODEN_PICKAXE).getDestroySpeed(Blocks.STONE.defaultBlockState()),
                "a dormant pick digs slower than wood");
        h.succeed();
    }

    private static double modifier(
            ItemStack stack, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute) {
        double[] sum = {0};
        stack.forEachModifier(net.minecraft.world.entity.EquipmentSlot.MAINHAND, (a, m) -> {
            if (a.equals(attribute)) sum[0] += m.amount();
        });
        return sum[0];
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 700)
    public static void theCradleWakesAToolAsWornAsItWentIn(GameTestHelper h) {
        h.setBlock(GROUND, EcologyModule.LIVING_SUBSTRATE.get());
        h.setBlock(ORGAN, ProcessingModule.AWAKENING_CRADLE.get());
        ItemStack worn = new ItemStack(EquipmentModule.DORMANT_HATCHET.get());
        worn.setDamageValue(6); // half worn
        Player loader = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
        loader.setItemInHand(InteractionHand.MAIN_HAND, worn);
        EcologyTests.use(h, loader, ORGAN, Direction.UP);
        load(h, EcologyModule.SPORE_CULTURE.get(), 1);
        load(h, StorageModule.BIOMASS_BUCKET.get(), 1);
        h.runAfterDelay(
                1, () -> h.assertValueEqual(contents(h).biomass().stored().units(), 900, "100 BU spent on waking"));
        h.runAtTickTime(AWAKENING_TICKS + 2, () -> {
            ItemStack woken = contents(h).output();
            h.assertTrue(woken.is(EquipmentModule.LIVING_HATCHET.get()), "a Living Hatchet");
            h.assertValueEqual(woken.getMaxDamage(), LivingTool.LIVING_USES, "256 blocks");
            h.assertValueEqual(woken.getDamageValue(), 128, "still half worn");
            var reserve = Reserves.of(woken).orElseThrow();
            h.assertValueEqual(reserve.stored(), Biomass.ZERO, "wakes empty");
            h.assertValueEqual(reserve.capacity(), EquipmentModule.RESERVE_CAPACITY, "with a 40 BU reserve");
            h.assertTrue(woken.get(EquipmentModule.TOOL_MUTATION.get()) == null, "and no branch yet");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 60)
    public static void aCarriedLivingToolMendsFourPointsASecondFromItsReserve(GameTestHelper h) {
        ItemStack tool = new ItemStack(EquipmentModule.LIVING_SPADE.get());
        tool.setDamageValue(10);
        fill(tool, 6);
        Player carrier = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
        carrier.setItemInHand(InteractionHand.MAIN_HAND, tool);
        ItemStack idle = new ItemStack(EquipmentModule.LIVING_HOE.get());
        idle.setDamageValue(10);
        carrier.setItemInHand(InteractionHand.OFF_HAND, idle); // dry, so it cannot mend
        for (int tick = 1; tick <= 41; tick++)
            h.runAfterDelay(tick, () -> carrier.getInventory().tick());
        h.runAfterDelay(42, () -> {
            ItemStack carried = carrier.getMainHandItem();
            h.assertValueEqual(
                    carried.getDamageValue(), 4, "6 BU mend six points, four the first second, two the next");
            h.assertValueEqual(Reserves.of(carried).orElseThrow().stored(), Biomass.ZERO, "one BU a point");
            h.assertValueEqual(carrier.getOffhandItem().getDamageValue(), 10, "a dry tool stays worn");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void theBladderFillsALivingTool(GameTestHelper h) {
        StorageTests.placeBladder(h);
        Player carrier = EcologyTests.holding(h, GameType.SURVIVAL, StorageModule.BIOMASS_BUCKET.get(), 1);
        EcologyTests.use(h, carrier, ORGAN, Direction.UP);
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
        MenuProvider provider = (MenuProvider) h.getBlockEntity(ORGAN);
        BiomassBladderMenu menu = (BiomassBladderMenu) provider.createMenu(1, player.getInventory(), player);
        h.assertFalse(menu.slots.get(0).mayPlace(new ItemStack(EquipmentModule.DORMANT_PICK.get())), "dormant refused");
        ItemStack tool = new ItemStack(EquipmentModule.LIVING_PICK.get());
        h.assertTrue(menu.slots.get(0).mayPlace(tool), "living accepted");
        menu.slots.get(0).set(tool);
        h.assertTrue(menu.clickMenuButton(player, BiomassBladderMenu.FILL_BUTTON), "fill handled");
        h.assertValueEqual(
                Reserves.of(menu.slots.get(0).getItem()).orElseThrow().stored(), Biomass.of(40), "40 BU in the tool");
        h.assertValueEqual(StorageTests.tank(h).stored(), Biomass.of(960), "40 BU out of the Bladder");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 700)
    public static void aContractileGraftKeepsTheToolsWearAndReserve(GameTestHelper h) {
        h.setBlock(GROUND, EcologyModule.LIVING_SUBSTRATE.get());
        h.setBlock(ORGAN, ProcessingModule.AWAKENING_CRADLE.get());
        ItemStack tool = new ItemStack(EquipmentModule.LIVING_PICK.get());
        tool.setDamageValue(64);
        fill(tool, 25);
        Player loader = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
        loader.setItemInHand(InteractionHand.MAIN_HAND, tool);
        EcologyTests.use(h, loader, ORGAN, Direction.UP);
        load(h, LogisticsModule.CONTRACTILE_FIBER.get(), 1);
        load(h, StorageModule.BIOMASS_BUCKET.get(), 1);
        h.runAfterDelay(
                1, () -> h.assertValueEqual(contents(h).biomass().stored().units(), 800, "200 BU spent on the graft"));
        h.runAtTickTime(AWAKENING_TICKS + 2, () -> {
            ItemStack grafted = contents(h).output();
            h.assertTrue(grafted.is(EquipmentModule.LIVING_PICK.get()), "the same pick");
            h.assertValueEqual(
                    grafted.get(EquipmentModule.TOOL_MUTATION.get()), ToolMutation.CONTRACTILE, "now Contractile");
            h.assertValueEqual(grafted.getDamageValue(), 64, "as worn as it went in");
            h.assertValueEqual(Reserves.of(grafted).orElseThrow().stored(), Biomass.of(25), "with its reserve");
            h.succeed();
        });
    }

    /**
     * A wall of stone three wide and three high at z 3, with obsidian in one corner and dirt in another; the player
     * stands north of it looking at its centre and breaks that block as a survival player does.
     */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aContractilePickBreaksTheBlocksItIsTheToolForWhileItsReservePays(GameTestHelper h) {
        BlockPos centre = new BlockPos(2, 2, 3);
        for (int x = 1; x <= 3; x++) for (int y = 1; y <= 3; y++) h.setBlock(new BlockPos(x, y, 3), Blocks.STONE);
        h.setBlock(new BlockPos(1, 3, 3), Blocks.OBSIDIAN);
        h.setBlock(new BlockPos(3, 1, 3), Blocks.DIRT);
        h.setBlock(new BlockPos(2, 2, 4), Blocks.STONE); // behind the centre: not in the face, stays
        ItemStack pick = new ItemStack(EquipmentModule.LIVING_PICK.get());
        pick.set(EquipmentModule.TOOL_MUTATION.get(), ToolMutation.CONTRACTILE);
        fill(pick, 9); // pays for four neighbours, with one BU to spare
        ServerPlayer miner = miner(h, pick, new BlockPos(2, 1, 0), centre);
        h.assertTrue(miner.gameMode.destroyBlock(h.absolutePos(centre)), "the centre breaks");
        h.assertBlockPresent(Blocks.AIR, centre);
        int stoneLeft = 0;
        for (int x = 1; x <= 3; x++)
            for (int y = 1; y <= 3; y++)
                if (h.getBlockState(new BlockPos(x, y, 3)).is(Blocks.STONE)) stoneLeft++;
        h.assertValueEqual(stoneLeft, 2, "four of the six stone neighbours broke, 2 BU each");
        h.assertBlockPresent(Blocks.OBSIDIAN, new BlockPos(1, 3, 3));
        h.assertBlockPresent(Blocks.DIRT, new BlockPos(3, 1, 3));
        h.assertBlockPresent(Blocks.STONE, new BlockPos(2, 2, 4));
        ItemStack held = miner.getMainHandItem();
        h.assertValueEqual(Reserves.of(held).orElseThrow().stored(), Biomass.of(1), "8 BU paid");
        h.assertValueEqual(held.getDamageValue(), 5, "each block wears the pick as it would by hand");
        h.assertValueEqual(
                h.getEntities(EntityType.ITEM, centre, 3.0).stream()
                        .mapToInt(drop -> drop.getItem().is(Items.COBBLESTONE)
                                ? drop.getItem().getCount()
                                : 0)
                        .sum(),
                5,
                "five cobblestone dropped");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aDryContractilePickAndAPlainOneMineOneBlock(GameTestHelper h) {
        BlockPos centre = new BlockPos(2, 2, 3);
        for (int x = 1; x <= 3; x++) for (int y = 1; y <= 3; y++) h.setBlock(new BlockPos(x, y, 3), Blocks.STONE);
        ItemStack dry = new ItemStack(EquipmentModule.LIVING_PICK.get());
        dry.set(EquipmentModule.TOOL_MUTATION.get(), ToolMutation.CONTRACTILE);
        fill(dry, 1);
        ServerPlayer miner = miner(h, dry, new BlockPos(2, 1, 0), centre);
        miner.gameMode.destroyBlock(h.absolutePos(centre));
        h.assertValueEqual(stone(h), 8, "one BU pays for nothing beside the centre");
        h.setBlock(centre, Blocks.STONE);
        ItemStack plain = new ItemStack(EquipmentModule.LIVING_PICK.get());
        fill(plain, 40);
        ServerPlayer other = miner(h, plain, new BlockPos(2, 1, 0), centre);
        other.gameMode.destroyBlock(h.absolutePos(centre));
        h.assertValueEqual(stone(h), 8, "an ungrafted pick mines one block");
        h.assertValueEqual(Reserves.of(other.getMainHandItem()).orElseThrow().stored(), Biomass.of(40), "for free");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aContractilePickThatBreaksMidSpreadStopsThere(GameTestHelper h) {
        BlockPos centre = new BlockPos(2, 2, 3);
        for (int x = 1; x <= 3; x++) for (int y = 1; y <= 3; y++) h.setBlock(new BlockPos(x, y, 3), Blocks.STONE);
        ItemStack worn = new ItemStack(EquipmentModule.LIVING_PICK.get());
        worn.set(EquipmentModule.TOOL_MUTATION.get(), ToolMutation.CONTRACTILE);
        worn.setDamageValue(LivingTool.LIVING_USES - 2); // the centre and one neighbour are all it has left
        fill(worn, 40);
        ServerPlayer miner = miner(h, worn, new BlockPos(2, 1, 0), centre);
        miner.gameMode.destroyBlock(h.absolutePos(centre));
        h.assertValueEqual(stone(h), 7, "the centre and one neighbour broke, then the pick was gone");
        h.assertTrue(miner.getMainHandItem().isEmpty(), "the hand is empty");
        h.succeed();
    }

    private static int stone(GameTestHelper h) {
        int count = 0;
        for (int x = 1; x <= 3; x++)
            for (int y = 1; y <= 3; y++)
                if (h.getBlockState(new BlockPos(x, y, 3)).is(Blocks.STONE)) count++;
        return count;
    }

    /** A survival player at {@code at} holding {@code tool}, looking at the centre of {@code target}. */
    private static ServerPlayer miner(GameTestHelper h, ItemStack tool, BlockPos at, BlockPos target) {
        ServerPlayer player = new ServerPlayer(
                h.getLevel().getServer(),
                h.getLevel(),
                new GameProfile(UUID.randomUUID(), "miner"),
                ClientInformation.createDefault());
        player.setGameMode(GameType.SURVIVAL);
        var eye = h.absolutePos(at).getCenter().add(0, -0.5 + player.getEyeHeight(), 0);
        var aim = h.absolutePos(target).getCenter().subtract(eye);
        double yaw = Math.toDegrees(Math.atan2(-aim.x, aim.z));
        double pitch = Math.toDegrees(-Math.atan2(aim.y, Math.hypot(aim.x, aim.z)));
        var feet = h.absolutePos(at).getCenter().add(0, -0.5, 0);
        player.moveTo(feet.x, feet.y, feet.z, (float) yaw, (float) pitch);
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        return player;
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aContractilePickDoesNotSpreadFromDirt(GameTestHelper h) {
        BlockPos centre = new BlockPos(2, 2, 3);
        h.setBlock(centre, Blocks.DIRT);
        h.setBlock(centre.above(), Blocks.STONE);
        ItemStack pick = new ItemStack(EquipmentModule.LIVING_PICK.get());
        pick.set(EquipmentModule.TOOL_MUTATION.get(), ToolMutation.CONTRACTILE);
        fill(pick, 40);
        ServerPlayer player = miner(h, pick, new BlockPos(2, 1, 0), centre);
        h.assertTrue(player.gameMode.destroyBlock(h.absolutePos(centre)), "dirt breaks normally");
        h.assertBlockPresent(Blocks.STONE, centre.above());
        h.assertValueEqual(Reserves.of(pick).orElseThrow().stored(), Biomass.of(40), "no spread paid");
        h.succeed();
    }

    private static void fill(ItemStack tool, int units) {
        BiomassTank reserve = Reserves.of(tool).orElseThrow();
        tool.set(EquipmentModule.RESERVE.get(), ((Outcome.Done<BiomassTank>) reserve.fill(Biomass.of(units))).value());
    }

    private static void load(GameTestHelper h, Item item, int count) {
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, item, count);
        for (int i = 0; i < count; i++) EcologyTests.use(h, player, ORGAN, Direction.UP);
    }

    private static BatchOrganContents contents(GameTestHelper h) {
        var registries = h.getLevel().registryAccess();
        var tag = h.getBlockEntity(ORGAN).saveWithoutMetadata(registries);
        return BatchOrganContents.CODEC
                .parse(registries.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), tag.get("contents"))
                .getOrThrow();
    }
}
