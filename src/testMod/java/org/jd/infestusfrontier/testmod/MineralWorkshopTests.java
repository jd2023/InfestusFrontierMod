package org.jd.infestusfrontier.testmod;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.core.foundation.Outcome;
import org.jd.infestusfrontier.core.mineral.Grinding;
import org.jd.infestusfrontier.core.mineral.Metal;
import org.jd.infestusfrontier.core.mineral.Parcel;
import org.jd.infestusfrontier.core.organ.OrganHistory;
import org.jd.infestusfrontier.core.processing.Batch;
import org.jd.infestusfrontier.core.storage.BiomassTank;
import org.jd.infestusfrontier.logistics.LogisticsModule;
import org.jd.infestusfrontier.logistics.MouthBlock;
import org.jd.infestusfrontier.logistics.MouthMode;
import org.jd.infestusfrontier.mineral.MineralModule;
import org.jd.infestusfrontier.organ.OrganModule;
import org.jd.infestusfrontier.organ.api.Organs;
import org.jd.infestusfrontier.processing.BatchOrganMenu;
import org.jd.infestusfrontier.processing.ProcessingModule;
import org.jd.infestusfrontier.processing.api.BatchOrganContents;
import org.jd.infestusfrontier.processing.api.OrganBatch;
import org.jd.infestusfrontier.processing.api.OrganBatchRecipe;
import org.jd.infestusfrontier.storage.api.BiomassCapabilities;
import org.slf4j.Logger;

/**
 * The automated mineral workshop's first release: the Mineral Gizzard turns a raw iron, gold or copper item into four
 * dust, one Tailings of four residual units and one Rock Residue; the Bio-Furnace smelts four dust of one metal into an
 * ingot. Tests count every product to the item, refuse every other input, pause on every full tray, and keep one copy of
 * everything through saves, breaking and hand, hopper and Mouth access. Organs stand at y=2 on substrate at y=1.
 */
@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class MineralWorkshopTests {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int Y = 1;
    private static final int GRIND_TICKS = 160;
    private static final int FIRST_STAGE_SMELT_TICKS = 320;
    private static final int THIRD_STAGE_SMELT_TICKS = 160;
    private static final int THIRD_STAGE = 512;
    private static final String FURNACE_DUST_RECIPE = "infestusfrontier:dust_smelting/";

    private MineralWorkshopTests() {}

    // --- grinding ---

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 260)
    public static void everyMetalIsGroundToFourDustOneTailingsAndOneResidue(GameTestHelper h) {
        List<Metal> metals = List.of(Metal.values());
        for (int i = 0; i < metals.size(); i++) {
            BlockPos at = gizzard(h, 1 + 2 * i);
            h.getLevel()
                    .getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(at), Direction.UP)
                    .insertItem(0, new ItemStack(raw(metals.get(i))), false);
        }
        h.runAtTickTime(GRIND_TICKS + 6, () -> {
            for (int i = 0; i < metals.size(); i++) {
                Metal metal = metals.get(i);
                BatchOrganContents done = contents(h, new BlockPos(1 + 2 * i, Y + 1, 1));
                h.assertTrue(done.batch().isEmpty(), metal.id() + ": the grind is done");
                h.assertValueEqual(done.history().completedBatches(), 1, metal.id() + ": counted once");
                h.assertTrue(done.ingredients().get(0).isEmpty(), metal.id() + ": the raw item is taken");
                List<ItemStack> trays = done.outputs();
                h.assertTrue(trays.get(0).is(MineralModule.dust(metal)), metal.id() + ": its own dust");
                h.assertValueEqual(trays.get(0).getCount(), Grinding.DUST_PER_RAW, metal.id() + ": four dust");
                h.assertTrue(trays.get(1).is(MineralModule.MINERAL_TAILINGS.get()), metal.id() + ": a Tailings");
                h.assertValueEqual(trays.get(1).getCount(), 1, metal.id() + ": one Tailings");
                h.assertValueEqual(
                        trays.get(1).get(MineralModule.PARCEL.get()),
                        Parcel.ground(metal),
                        metal.id() + ": four residual units of the same metal");
                h.assertTrue(trays.get(2).is(MineralModule.ROCK_RESIDUE.get()), metal.id() + ": Rock Residue");
                h.assertValueEqual(trays.get(2).getCount(), 1, metal.id() + ": one Rock Residue");
                h.assertValueEqual(done.biomass().stored().units(), 980, metal.id() + ": 20 BU burnt");
            }
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aGrindStartsAtOnceAndTakesExactlyWhatItNeeds(GameTestHelper h) {
        BlockPos at = gizzard(h, 1);
        IItemHandler ports = ports(h, at);
        h.assertTrue(
                ports.insertItem(0, new ItemStack(Items.RAW_IRON, 64), false).isEmpty(), "64 raw items fit one slot");
        h.runAfterDelay(2, () -> {
            BatchOrganContents started = contents(h, at);
            h.assertTrue(started.batch().isPresent(), "a batch runs on its own");
            h.assertValueEqual(
                    started.batch().orElseThrow().batch().recipe(), "infestusfrontier:grinding/iron", "the recipe");
            h.assertValueEqual(started.batch().orElseThrow().batch().ticksRequired(), GRIND_TICKS, "8 seconds");
            h.assertValueEqual(started.ingredients().get(0).getCount(), 63, "one raw item taken, the rest waits");
            h.assertValueEqual(started.biomass().stored().units(), 980, "20 BU spent at the start");
            h.assertValueEqual(started.batch().orElseThrow().results().size(), 3, "three results promised");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void withoutEnoughBiomassNothingStarts(GameTestHelper h) {
        BlockPos at = gizzard(h, 1, 19);
        ports(h, at).insertItem(0, new ItemStack(Items.RAW_COPPER), false);
        h.runAfterDelay(10, () -> {
            BatchOrganContents waiting = contents(h, at);
            h.assertTrue(waiting.batch().isEmpty(), "19 BU cannot pay for a grind");
            h.assertValueEqual(waiting.ingredients().get(0).getCount(), 1, "the raw copper stays");
            h.assertValueEqual(waiting.biomass().stored().units(), 19, "and the biomass");
            h.succeed();
        });
    }

    // --- refusal ---

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void theGizzardRefusesEverythingButRawIronGoldAndCopper(GameTestHelper h) {
        BlockPos at = gizzard(h, 1);
        IItemHandler ports = ports(h, at);
        List<Item> refused = List.of(
                Items.IRON_INGOT,
                Items.GOLD_INGOT,
                Items.COPPER_INGOT,
                Items.IRON_NUGGET,
                Items.GOLD_NUGGET,
                Items.RAW_IRON_BLOCK,
                Items.RAW_GOLD_BLOCK,
                Items.RAW_COPPER_BLOCK,
                Items.IRON_BLOCK,
                Items.GOLD_BLOCK,
                Items.COPPER_BLOCK,
                Items.IRON_ORE,
                Items.DEEPSLATE_GOLD_ORE,
                Items.COPPER_ORE,
                Items.NETHER_GOLD_ORE,
                Items.COAL_ORE,
                Items.LAPIS_ORE,
                Items.DIAMOND_ORE,
                Items.ANCIENT_DEBRIS,
                Items.RAW_IRON_BLOCK,
                Items.IRON_PICKAXE,
                Items.IRON_CHESTPLATE,
                Items.DIAMOND,
                Items.COAL,
                Items.COBBLESTONE,
                MineralModule.IRON_DUST.get(),
                MineralModule.GOLD_DUST.get(),
                MineralModule.COPPER_DUST.get(),
                MineralModule.ROCK_RESIDUE.get(),
                MineralModule.MINERAL_TAILINGS.get());
        for (Item item : refused)
            h.assertTrue(
                    ports.insertItem(0, new ItemStack(item, 3), true).getCount() == 3
                            && !ports.isItemValid(0, new ItemStack(item)),
                    "the Gizzard refuses " + item);
        ItemStack tailings = MineralModule.tailings(Parcel.ground(Metal.IRON));
        h.assertFalse(ports.isItemValid(0, tailings), "a real Tailings is no raw item");
        h.assertFalse(ports.isItemValid(0, MineralModule.tailings(new Parcel(Metal.GOLD, 1))), "nor a thin one");
        for (Metal metal : Metal.values())
            h.assertTrue(ports.isItemValid(0, new ItemStack(raw(metal))), "raw " + metal.id() + " goes in");
        // by hand: a refused item opens the screen and stays in the hand
        Player hand = EcologyTests.holding(h, GameType.SURVIVAL, Items.IRON_INGOT, 5);
        EcologyTests.use(h, hand, at, Direction.UP);
        h.assertValueEqual(hand.getMainHandItem().getCount(), 5, "an ingot stays in the hand");
        h.assertTrue(contents(h, at).ingredients().get(0).isEmpty(), "and out of the Gizzard");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void theSlotHoldsOneKindOfRawItemAtATime(GameTestHelper h) {
        BlockPos at = gizzard(h, 1, 0);
        IItemHandler ports = ports(h, at);
        h.assertTrue(
                ports.insertItem(0, new ItemStack(Items.RAW_IRON, 5), false).isEmpty(), "iron goes in");
        h.assertValueEqual(
                ports.insertItem(0, new ItemStack(Items.RAW_GOLD, 5), false).getCount(),
                5,
                "gold waits while the slot holds iron");
        h.assertValueEqual(ports.getStackInSlot(0).getCount(), 5, "nothing mixed");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void theFurnaceSmeltsDustAndRefusesTheWastesAndOtherMetalsMixed(GameTestHelper h) {
        BlockPos at = furnace(h, 1, 0, 0);
        IItemHandler ports = ports(h, at);
        for (Metal metal : Metal.values())
            h.assertTrue(
                    ports.isItemValid(0, new ItemStack(MineralModule.dust(metal))), metal.id() + " dust is accepted");
        h.assertFalse(ports.isItemValid(0, new ItemStack(MineralModule.ROCK_RESIDUE.get())), "residue is no fuel");
        h.assertFalse(
                ports.isItemValid(0, MineralModule.tailings(Parcel.ground(Metal.IRON))),
                "Tailings are not smelted, whatever they hold");
        h.assertFalse(ports.isItemValid(0, new ItemStack(Items.IRON_INGOT)), "an ingot is no dust and no ore");
        h.assertTrue(ports.isItemValid(0, new ItemStack(Items.RAW_IRON)), "vanilla smelting stays available");
        h.assertTrue(
                ports.insertItem(0, new ItemStack(MineralModule.IRON_DUST.get(), 2), false)
                        .isEmpty(),
                "iron dust");
        h.assertValueEqual(
                ports.insertItem(0, new ItemStack(MineralModule.GOLD_DUST.get(), 2), false)
                        .getCount(),
                2,
                "gold dust waits for the slot");
        h.assertValueEqual(
                ports.insertItem(0, new ItemStack(Items.RAW_IRON), true).getCount(),
                1,
                "and raw iron cannot join dust in the one slot");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void dustHasOneSourceAndTailingsFeedNothing(GameTestHelper h) {
        var registries = h.getLevel().registryAccess();
        List<Item> dusts =
                List.of(MineralModule.IRON_DUST.get(), MineralModule.GOLD_DUST.get(), MineralModule.COPPER_DUST.get());
        Set<Item> raws = Set.of(Items.RAW_IRON, Items.RAW_GOLD, Items.RAW_COPPER);
        ItemStack tailings = MineralModule.tailings(Parcel.ground(Metal.IRON));
        int grinds = 0;
        int dustSmelts = 0;
        int discards = 0;
        for (RecipeHolder<?> holder : h.getLevel().getRecipeManager().getRecipes()) {
            var recipe = holder.value();
            List<ItemStack> results = new ArrayList<>();
            List<net.minecraft.world.item.crafting.Ingredient> ingredients = new ArrayList<>(recipe.getIngredients());
            if (recipe instanceof OrganBatchRecipe batch) {
                results.addAll(batch.results());
                ingredients.addAll(batch.ingredients());
            } else results.add(recipe.getResultItem(registries));
            for (ItemStack made : results) {
                if (dusts.contains(made.getItem()) || made.is(MineralModule.MINERAL_TAILINGS.get())) {
                    h.assertTrue(
                            recipe instanceof OrganBatchRecipe batch
                                    && batch.organ().equals("mineral_gizzard"),
                            holder.id() + " makes dust or Tailings outside the Gizzard");
                    grinds++;
                }
                if (made.is(MineralModule.ROCK_RESIDUE.get()))
                    h.assertTrue(
                            (recipe instanceof OrganBatchRecipe batch
                                            && batch.organ().equals("mineral_gizzard"))
                                    || holder.id().getPath().equals("tailings_to_rock_residue"),
                            holder.id() + " makes Rock Residue from something else");
            }
            if (recipe instanceof OrganBatchRecipe batch && batch.organ().equals("mineral_gizzard")) {
                h.assertValueEqual(batch.ingredients().size(), 1, holder.id() + " takes one item");
                h.assertTrue(
                        raws.stream().anyMatch(raw -> batch.ingredients().get(0).test(new ItemStack(raw)))
                                && Set.of(Items.IRON_INGOT, Items.GOLD_INGOT, Items.COPPER_INGOT, Items.RAW_IRON_BLOCK)
                                        .stream()
                                        .noneMatch(other ->
                                                batch.ingredients().get(0).test(new ItemStack(other))),
                        holder.id() + " grinds a raw item and nothing else");
            }
            for (Item dust : dusts) {
                if (ingredients.stream().noneMatch(i -> i.test(new ItemStack(dust)))) continue;
                h.assertTrue(
                        recipe instanceof OrganBatchRecipe batch
                                && batch.organ().equals("bio_furnace")
                                && holder.id().toString().startsWith(FURNACE_DUST_RECIPE),
                        holder.id() + " uses dust outside the furnace");
                dustSmelts++;
            }
            if (ingredients.stream().anyMatch(i -> i.test(tailings))) {
                h.assertTrue(
                        holder.id().getPath().equals("tailings_to_rock_residue"),
                        holder.id() + " does more with Tailings than discard them");
                discards++;
            }
        }
        h.assertValueEqual(grinds, 6, "three metals, each with a dust and a Tailings result");
        h.assertValueEqual(dustSmelts, 3 * 1, "one dust smelt per metal, counted once per dust it takes");
        h.assertValueEqual(discards, 1, "one craft takes Tailings");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void theGizzardAndTheWasteCraftsResolveAndNothingTurnsWasteIntoDust(GameTestHelper h) {
        Item bud = OrganModule.ORGAN_BUD_ITEM.get();
        Item plate = ProcessingModule.BONE_PLATE.get();
        craft(
                h,
                List.of(
                        new ItemStack(bud),
                        new ItemStack(Items.IRON_PICKAXE),
                        new ItemStack(plate),
                        new ItemStack(plate),
                        new ItemStack(Items.FLINT),
                        new ItemStack(Items.FLINT)),
                Optional.of(new ItemStack(ProcessingModule.MINERAL_GIZZARD_ITEM.get())));
        craft(
                h,
                List.of(
                        new ItemStack(MineralModule.ROCK_RESIDUE.get()),
                        new ItemStack(MineralModule.ROCK_RESIDUE.get())),
                Optional.of(new ItemStack(Items.COBBLESTONE)));
        craft(h, List.of(new ItemStack(MineralModule.ROCK_RESIDUE.get())), Optional.empty());
        for (Metal metal : Metal.values()) {
            craft(
                    h,
                    List.of(MineralModule.tailings(Parcel.ground(metal))),
                    Optional.of(new ItemStack(MineralModule.ROCK_RESIDUE.get())));
            craft(
                    h,
                    List.of(MineralModule.tailings(new Parcel(metal, 1))),
                    Optional.of(new ItemStack(MineralModule.ROCK_RESIDUE.get())));
        }
        // a Tailings with no parcel holds no metal, and the craft that clears waste still takes it
        craft(
                h,
                List.of(new ItemStack(MineralModule.MINERAL_TAILINGS.get())),
                Optional.of(new ItemStack(MineralModule.ROCK_RESIDUE.get())));
        craft(
                h,
                List.of(
                        MineralModule.tailings(Parcel.ground(Metal.IRON)),
                        MineralModule.tailings(Parcel.ground(Metal.IRON))),
                Optional.empty());
        craft(h, List.of(new ItemStack(MineralModule.IRON_DUST.get(), 1)), Optional.empty());
        craft(
                h,
                List.of(
                        new ItemStack(Items.IRON_INGOT),
                        new ItemStack(Items.IRON_INGOT),
                        new ItemStack(Items.IRON_INGOT),
                        new ItemStack(Items.IRON_INGOT)),
                Optional.empty());
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void theTailingsTooltipSaysWhatTheStackHoldsAndWhatItCanStillDo(GameTestHelper h) {
        h.assertValueEqual(
                tooltipKeys(h, new ItemStack(MineralModule.MINERAL_TAILINGS.get())),
                List.of("item.infestusfrontier.mineral_tailings.unparcelled"),
                "a Tailings without a parcel says it holds no recoverable metal");
        h.assertValueEqual(
                tooltipKeys(h, MineralModule.tailings(new Parcel(Metal.GOLD, 3))),
                List.of("item.infestusfrontier.mineral_tailings.units", "item.infestusfrontier.mineral_tailings.inert"),
                "a parcelled Tailings says its units and that it is inert");
        try (var lang = MineralModule.class.getResourceAsStream("/assets/infestusfrontier/lang/en_us.json")) {
            var words = com.google.gson.JsonParser.parseString(
                            new String(lang.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8))
                    .getAsJsonObject()
                    .get("item.infestusfrontier.mineral_tailings.unparcelled")
                    .getAsString();
            h.assertTrue(
                    words.contains("Rock Residue"), "the bare Tailings' text names what it can still become: " + words);
        } catch (IOException e) {
            h.fail("cannot read the lang file: " + e.getMessage());
        }
        h.succeed();
    }

    private static List<String> tooltipKeys(GameTestHelper h, ItemStack stack) {
        List<net.minecraft.network.chat.Component> lines = new ArrayList<>();
        stack.getItem()
                .appendHoverText(
                        stack,
                        Item.TooltipContext.of(h.getLevel()),
                        lines,
                        net.minecraft.world.item.TooltipFlag.NORMAL);
        return lines.stream()
                .map(line -> ((net.minecraft.network.chat.contents.TranslatableContents) line.getContents()).getKey())
                .toList();
    }

    /** The crafting grid's answer to {@code inputs} laid out in a row: {@code expected}, or no recipe when empty. */
    private static void craft(GameTestHelper h, List<ItemStack> inputs, Optional<ItemStack> expected) {
        List<ItemStack> grid = new ArrayList<>(inputs);
        while (grid.size() < 9) grid.add(ItemStack.EMPTY);
        net.minecraft.world.item.crafting.CraftingInput input =
                net.minecraft.world.item.crafting.CraftingInput.of(3, 3, grid);
        var recipe = h.getLevel()
                .getRecipeManager()
                .getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, h.getLevel());
        if (expected.isEmpty()) {
            h.assertTrue(recipe.isEmpty(), "no craft for " + inputs);
            return;
        }
        h.assertTrue(recipe.isPresent(), "a craft for " + inputs);
        ItemStack made = recipe.get().value().assemble(input, h.getLevel().registryAccess());
        h.assertTrue(
                made.is(expected.get().getItem())
                        && made.getCount() == expected.get().getCount(),
                inputs + " make " + made + ", not " + expected.get());
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aParcelIsBoundedAndNoOneCanReplayAnOutOfRangeState(GameTestHelper h) {
        var codec = ItemStack.CODEC;
        for (String bad : List.of(
                "{id:'infestusfrontier:mineral_tailings',count:1,components:{'infestusfrontier:parcel':{metal:'iron',units:5}}}",
                "{id:'infestusfrontier:mineral_tailings',count:1,components:{'infestusfrontier:parcel':{metal:'iron',units:0}}}",
                "{id:'infestusfrontier:mineral_tailings',count:1,components:{'infestusfrontier:parcel':{metal:'iron',units:-3}}}",
                "{id:'infestusfrontier:mineral_tailings',count:1,components:{'infestusfrontier:parcel':{metal:'diamond',units:4}}}",
                "{id:'infestusfrontier:mineral_tailings',count:1,components:{'infestusfrontier:parcel':{units:4}}}")) {
            var parsed = codec.parse(
                    h.getLevel().registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE),
                    parseTag(h, bad));
            h.assertTrue(parsed.result().isEmpty(), "refused: " + bad);
        }
        ItemStack ok = MineralModule.tailings(new Parcel(Metal.COPPER, 3));
        var ops = h.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);
        var json = codec.encodeStart(ops, ok).getOrThrow();
        h.assertTrue(json.toString().contains("\"units\":3"), "a legal parcel round-trips: " + json);
        ItemStack back = codec.parse(ops, json).getOrThrow();
        h.assertValueEqual(back.get(MineralModule.PARCEL.get()), new Parcel(Metal.COPPER, 3), "same parcel");
        // a Tailings, however it is put, never makes dust: no slot of the workshop takes it
        BlockPos gizzard = gizzard(h, 1);
        BlockPos furnace = furnace(h, 3, 0, 0);
        h.assertFalse(ports(h, gizzard).isItemValid(0, back), "the Gizzard does not grind Tailings again");
        h.assertFalse(ports(h, furnace).isItemValid(0, back), "the furnace does not smelt them");
        h.succeed();
    }

    private static net.minecraft.nbt.Tag parseTag(GameTestHelper h, String snbt) {
        try {
            return net.minecraft.nbt.TagParser.parseTag(snbt.replace('\'', '"'));
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            h.fail("bad test tag " + snbt + ": " + e.getMessage());
            throw new IllegalStateException(e);
        }
    }

    // --- smelting dust ---

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 500)
    public static void thePartialBatchWaitsAndTheSurplusDustStaysUsable(GameTestHelper h) {
        BlockPos at = furnace(h, 1, THIRD_STAGE, 1000);
        IItemHandler ports = ports(h, at);
        ports.insertItem(0, new ItemStack(MineralModule.IRON_DUST.get(), 3), false);
        h.runAfterDelay(10, () -> {
            BatchOrganContents waiting = contents(h, at);
            h.assertTrue(waiting.batch().isEmpty(), "three dust are no batch");
            h.assertValueEqual(waiting.ingredients().get(0).getCount(), 3, "they wait");
            h.assertValueEqual(waiting.biomass().stored().units(), 1000, "and nothing is spent");
            ports.insertItem(0, new ItemStack(MineralModule.IRON_DUST.get(), 2), false);
        });
        h.runAfterDelay(14, () -> {
            BatchOrganContents running = contents(h, at);
            h.assertTrue(running.batch().isPresent(), "the fourth dust starts a batch");
            h.assertValueEqual(
                    running.batch().orElseThrow().batch().recipe(),
                    FURNACE_DUST_RECIPE + "iron",
                    "the dust recipe, not a vanilla one");
            h.assertValueEqual(
                    running.batch().orElseThrow().batch().ticksRequired(),
                    THIRD_STAGE_SMELT_TICKS,
                    "the third stage's 8 seconds");
            h.assertValueEqual(running.ingredients().get(0).getCount(), 1, "exactly four were taken from five");
            h.assertValueEqual(running.biomass().stored().units(), 960, "40 BU");
        });
        h.runAfterDelay(14 + THIRD_STAGE_SMELT_TICKS + 4, () -> {
            BatchOrganContents first = contents(h, at);
            h.assertValueEqual(first.outputs().get(0).getCount(), 1, "one ingot");
            h.assertTrue(first.outputs().get(0).is(Items.IRON_INGOT), "an iron ingot");
            h.assertTrue(
                    Math.abs(first.experience() - 0.7f) < 0.001f,
                    "the raw item's 0.7 experience, once: " + first.experience());
            h.assertValueEqual(first.ingredients().get(0).getCount(), 1, "the surplus dust is still there");
            ports.insertItem(0, new ItemStack(MineralModule.IRON_DUST.get(), 3), false);
        });
        h.runAfterDelay(14 + THIRD_STAGE_SMELT_TICKS + 8, () -> {
            BatchOrganContents second = contents(h, at);
            h.assertTrue(second.batch().isPresent(), "the surplus counted toward the next ingot");
            h.assertValueEqual(second.ingredients().get(0).getCount(), 0, "four taken");
        });
        h.runAfterDelay(14 + 2 * THIRD_STAGE_SMELT_TICKS + 12, () -> {
            BatchOrganContents done = contents(h, at);
            h.assertValueEqual(done.outputs().get(0).getCount(), 2, "two ingots from eight dust");
            h.assertValueEqual(done.biomass().stored().units(), 920, "80 BU for two ingots");
            h.assertValueEqual(done.history().completedBatches(), THIRD_STAGE + 2, "counted per ingot");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aFullIngotTrayHoldsTheDustAndTheBiomassBack(GameTestHelper h) {
        BlockPos at = furnace(h, 1, THIRD_STAGE, 1000);
        preset(
                h,
                at,
                contents(h, at),
                List.of(new ItemStack(MineralModule.GOLD_DUST.get(), 4)),
                List.of(new ItemStack(Items.GOLD_INGOT, 64)),
                Optional.empty());
        h.runAfterDelay(10, () -> {
            BatchOrganContents held = contents(h, at);
            h.assertTrue(held.batch().isEmpty(), "no ingot fits, so no batch starts");
            h.assertValueEqual(held.ingredients().get(0).getCount(), 4, "the dust stays");
            h.assertValueEqual(held.biomass().stored().units(), 1000, "and the biomass");
            ports(h, at).extractItem(1, 64, false);
        });
        h.runAfterDelay(14, () -> {
            h.assertTrue(contents(h, at).batch().isPresent(), "emptying the tray lets it start");
            h.succeed();
        });
    }

    // --- full trays ---

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 400)
    public static void everyFullTrayPausesTheGizzardWithNothingSpentAndNamesIt(GameTestHelper h) {
        // dust full, Tailings full, residue full, and a tray holding another metal's dust
        List<List<ItemStack>> trays = List.of(
                List.of(new ItemStack(MineralModule.IRON_DUST.get(), 64), ItemStack.EMPTY, ItemStack.EMPTY),
                List.of(
                        ItemStack.EMPTY,
                        withCount(MineralModule.tailings(Parcel.ground(Metal.IRON)), 64),
                        ItemStack.EMPTY),
                List.of(ItemStack.EMPTY, ItemStack.EMPTY, new ItemStack(MineralModule.ROCK_RESIDUE.get(), 64)),
                List.of(new ItemStack(MineralModule.GOLD_DUST.get(), 1), ItemStack.EMPTY, ItemStack.EMPTY));
        int[] masks = {1, 2, 4, 1};
        for (int i = 0; i < trays.size(); i++) {
            BlockPos at = gizzard(h, 1 + 2 * i);
            preset(h, at, contents(h, at), List.of(new ItemStack(Items.RAW_IRON)), trays.get(i), Optional.empty());
        }
        h.runAfterDelay(20, () -> {
            for (int i = 0; i < trays.size(); i++) {
                BlockPos at = new BlockPos(1 + 2 * i, Y + 1, 1);
                BatchOrganContents held = contents(h, at);
                h.assertTrue(held.batch().isEmpty(), "tray case " + i + " pauses");
                h.assertValueEqual(held.ingredients().get(0).getCount(), 1, i + ": the raw item stays");
                h.assertValueEqual(held.biomass().stored().units(), 1000, i + ": no biomass spent");
                h.assertValueEqual(menu(h, at).blocked(), masks[i], i + ": the screen names the full tray");
            }
            for (int i = 0; i < trays.size(); i++) {
                BlockPos at = new BlockPos(1 + 2 * i, Y + 1, 1);
                IItemHandler ports = ports(h, at);
                for (int slot = 1; slot <= 3; slot++) ports.extractItem(slot, 64, false);
            }
        });
        h.runAfterDelay(26, () -> {
            for (int i = 0; i < trays.size(); i++) {
                BatchOrganContents resumed = contents(h, new BlockPos(1 + 2 * i, Y + 1, 1));
                h.assertTrue(resumed.batch().isPresent(), i + ": emptying the tray resumes the grind");
                h.assertValueEqual(resumed.biomass().stored().units(), 980, i + ": 20 BU, now");
                h.assertValueEqual(menu(h, new BlockPos(1 + 2 * i, Y + 1, 1)).blocked(), 0, i + ": nothing waits");
            }
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 200)
    public static void aFinishedBatchWaitsForRoomAndDeliversEveryProductOnce(GameTestHelper h) {
        BlockPos at = gizzard(h, 1);
        OrganBatch nearlyDone = new OrganBatch(
                new Batch("infestusfrontier:grinding/copper", GRIND_TICKS, GRIND_TICKS - 2),
                new ItemStack(MineralModule.COPPER_DUST.get(), 4),
                List.of(
                        MineralModule.tailings(Parcel.ground(Metal.COPPER)),
                        new ItemStack(MineralModule.ROCK_RESIDUE.get())),
                Optional.empty(),
                0f);
        // 62 dust in the tray: the four more do not fit
        preset(
                h,
                at,
                contents(h, at),
                List.of(ItemStack.EMPTY),
                List.of(new ItemStack(MineralModule.COPPER_DUST.get(), 62), ItemStack.EMPTY, ItemStack.EMPTY),
                Optional.of(nearlyDone));
        h.runAfterDelay(8, () -> {
            BatchOrganContents waiting = contents(h, at);
            h.assertTrue(waiting.batch().isPresent(), "the finished batch is kept, not dropped");
            h.assertValueEqual(waiting.outputs().get(0).getCount(), 62, "no partial delivery to the dust tray");
            h.assertTrue(waiting.outputs().get(1).isEmpty(), "nor the Tailings");
            h.assertTrue(waiting.outputs().get(2).isEmpty(), "nor the residue");
            h.assertValueEqual(waiting.history().completedBatches(), 0, "and it is not counted yet");
            h.assertValueEqual(menu(h, at).blocked(), 1, "the screen says the dust tray is full");
            ports(h, at).extractItem(1, 10, false);
        });
        h.runAfterDelay(14, () -> {
            BatchOrganContents delivered = contents(h, at);
            h.assertTrue(delivered.batch().isEmpty(), "delivered once room opened");
            h.assertValueEqual(delivered.outputs().get(0).getCount(), 56, "52 left in the tray plus four");
            h.assertValueEqual(delivered.outputs().get(1).getCount(), 1, "one Tailings");
            h.assertValueEqual(delivered.outputs().get(2).getCount(), 1, "one residue");
            h.assertValueEqual(delivered.history().completedBatches(), 1, "counted exactly once");
            h.succeed();
        });
    }

    // --- saves ---

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 260)
    public static void aSaveInTheMiddleOfAGrindGivesTheProductsOnce(GameTestHelper h) {
        BlockPos at = gizzard(h, 1);
        ports(h, at).insertItem(0, new ItemStack(Items.RAW_GOLD, 2), false);
        h.runAfterDelay(100, () -> {
            BatchOrganContents mid = contents(h, at);
            h.assertTrue(mid.batch().isPresent(), "a grind runs");
            h.assertValueEqual(mid.biomass().stored().units(), 980, "its 20 BU are spent");
            reload(h, at); // through real NBT bytes, as a chunk writes and reads it
            BatchOrganContents again = contents(h, at);
            h.assertValueEqual(again, mid, "saved and read back, nothing changed");
        });
        h.runAtTickTime(GRIND_TICKS + 8, () -> {
            BatchOrganContents first = contents(h, at);
            h.assertValueEqual(first.history().completedBatches(), 1, "one grind finished after the reload");
            h.assertValueEqual(first.outputs().get(0).getCount(), 4, "four dust, not eight");
            h.assertValueEqual(first.outputs().get(1).getCount(), 1, "one Tailings");
            h.assertValueEqual(first.outputs().get(2).getCount(), 1, "one residue");
            reload(h, at);
            h.assertValueEqual(contents(h, at), first, "a second reload keeps every tray as it was");
            h.assertTrue(
                    contents(h, at).batch().isPresent(),
                    "the second raw item's batch is part of the saved state, started once");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aSavedTrayBeyondTheKindOrAFurnaceTrayOfTwoIsUnreadable(GameTestHelper h) {
        var registries = h.getLevel().registryAccess();
        BlockPos gizzard = gizzard(h, 1);
        BlockEntity organ = h.getBlockEntity(gizzard);
        CompoundTag tag = organ.saveWithoutMetadata(registries);
        var outputs = tag.getCompound("contents").getList("outputs", 10);
        outputs.add(new CompoundTag()); // a fourth tray
        tag.getCompound("contents").put("outputs", outputs);
        organ.loadWithComponents(tag, registries);
        h.assertTrue(
                h.getBlockEntity(gizzard).collectComponents().get(OrganModule.UNREADABLE_SAVE.get()) != null,
                "four trays cannot be a Gizzard's contents");
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, Items.RAW_IRON, 1);
        EcologyTests.use(h, player, gizzard, Direction.UP);
        h.assertValueEqual(player.getMainHandItem().getCount(), 1, "an unreadable Gizzard takes nothing");

        BlockPos furnace = furnace(h, 3, 0, 0);
        BlockEntity stove = h.getBlockEntity(furnace);
        CompoundTag saved = stove.saveWithoutMetadata(registries);
        var two = new net.minecraft.nbt.ListTag();
        two.add(new CompoundTag());
        two.add(new CompoundTag());
        saved.getCompound("contents").put("outputs", two);
        stove.loadWithComponents(saved, registries);
        h.assertTrue(
                h.getBlockEntity(furnace).collectComponents().get(OrganModule.UNREADABLE_SAVE.get()) != null,
                "two trays cannot be a furnace's contents");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aSaveWithOneOutputFieldStillLoadsIntoTheFirstTray(GameTestHelper h) {
        var registries = h.getLevel().registryAccess();
        BlockPos at = furnace(h, 1, 70, 800);
        BlockEntity organ = h.getBlockEntity(at);
        CompoundTag tag = organ.saveWithoutMetadata(registries);
        CompoundTag contents = tag.getCompound("contents");
        contents.remove("outputs");
        CompoundTag ingots = new CompoundTag();
        ingots.putString("id", "minecraft:iron_ingot");
        ingots.putInt("count", 3);
        contents.put("output", ingots);
        organ.loadWithComponents(tag, registries);
        h.assertTrue(
                h.getBlockEntity(at).collectComponents().get(OrganModule.UNREADABLE_SAVE.get()) == null,
                "a save from before the trays were listed is not unreadable");
        BatchOrganContents loaded = contents(h, at);
        h.assertValueEqual(loaded.outputs().size(), 1, "one tray");
        h.assertTrue(loaded.outputs().get(0).is(Items.IRON_INGOT), "the ingots are in it");
        h.assertValueEqual(loaded.outputs().get(0).getCount(), 3, "all three");
        h.assertValueEqual(loaded.history().completedBatches(), 70, "and the history is kept");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 260)
    public static void breakingAGizzardCarriesEveryIntermediateOnceAndPlacingItResumes(GameTestHelper h) {
        BlockPos at = gizzard(h, 1);
        ports(h, at).insertItem(0, new ItemStack(Items.RAW_IRON, 5), false);
        h.runAfterDelay(60, () -> {
            BatchOrganContents before = contents(h, at);
            h.assertTrue(before.batch().isPresent(), "a grind runs");
            h.getLevel().destroyBlock(h.absolutePos(at), true);
            List<ItemEntity> drops = h.getEntities(EntityType.ITEM, at, 3.0);
            h.assertValueEqual(drops.size(), 1, "one drop, the organ itself");
            ItemStack dropped = drops.get(0).getItem();
            h.assertTrue(dropped.is(ProcessingModule.MINERAL_GIZZARD_ITEM.get()), "it is the Gizzard");
            BatchOrganContents carried = dropped.get(ProcessingModule.BATCH_CONTENTS.get());
            h.assertTrue(carried != null, "carrying its contents");
            h.assertValueEqual(carried, before, "the batch, the four raw items and the biomass, as they were");
            h.assertValueEqual(
                    carried.ingredients().get(0).getCount(), 4, "one raw item is in the batch, not the slot");
            drops.get(0).discard();
            Player placer = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
            placer.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, dropped.copy());
            h.placeAt(placer, placer.getMainHandItem(), at.below(), Direction.UP);
            h.assertBlockPresent(ProcessingModule.MINERAL_GIZZARD.get(), at);
            h.assertValueEqual(contents(h, at), before, "placed back with everything it carried");
        });
        h.runAtTickTime(GRIND_TICKS + 20, () -> {
            BatchOrganContents done = contents(h, at);
            h.assertValueEqual(done.history().completedBatches(), 1, "one grind, finished once");
            h.assertValueEqual(done.outputs().get(0).getCount(), 4, "four dust");
            h.assertTrue(
                    h.getEntities(EntityType.ITEM, at, 3.0).isEmpty(), "nothing else was left lying on the ground");
            h.succeed();
        });
    }

    // --- access ---

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 400)
    public static void handAndHopperFeedTheGizzardAndSneakingTakesAStackBack(GameTestHelper h) {
        BlockPos at = gizzard(h, 1);
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, Items.RAW_COPPER, 3);
        EcologyTests.use(h, player, at, Direction.UP);
        h.assertValueEqual(player.getMainHandItem().getCount(), 2, "one raw item a click, by hand");
        h.assertValueEqual(contents(h, at).ingredients().get(0).getCount(), 1, "it is in the slot");
        BlockPos hopperAt = at.above();
        h.setBlock(hopperAt, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        ((HopperBlockEntity) h.getBlockEntity(hopperAt)).setItem(0, new ItemStack(Items.RAW_COPPER, 6));
        ((HopperBlockEntity) h.getBlockEntity(hopperAt)).setItem(1, new ItemStack(Items.RAW_IRON, 6));
        h.runAfterDelay(40, () -> {
            BatchOrganContents fed = contents(h, at);
            h.assertTrue(fed.batch().isPresent(), "the grind started");
            h.assertTrue(
                    fed.ingredients().get(0).is(Items.RAW_COPPER)
                            || fed.ingredients().get(0).isEmpty(),
                    "only the kind the slot holds went in: " + fed.ingredients().get(0));
            HopperBlockEntity hopper = (HopperBlockEntity) h.getBlockEntity(hopperAt);
            h.assertValueEqual(
                    fed.ingredients().get(0).getCount() + hopper.countItem(Items.RAW_COPPER),
                    6,
                    "the hopper's six copper are in the slot or still in the hopper");
            h.assertValueEqual(hopper.countItem(Items.RAW_IRON), 6, "and its iron has not gone in beside the copper");
        });
        h.runAtTickTime(GRIND_TICKS + 30, () -> {
            IItemHandler ports = ports(h, at);
            h.assertTrue(ports.extractItem(0, 1, true).isEmpty(), "the raw slot gives nothing back to a hopper");
            h.assertTrue(
                    ports.insertItem(1, new ItemStack(MineralModule.IRON_DUST.get()), true)
                            .is(MineralModule.IRON_DUST.get()),
                    "the trays take nothing in");
            ItemStack dust = ports.extractItem(1, 64, false);
            h.assertTrue(dust.is(MineralModule.COPPER_DUST.get()), "a hopper below would take the dust");
            h.assertValueEqual(dust.getCount(), 4, "four");
            Player sneaker = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
            sneaker.setShiftKeyDown(true);
            EcologyTests.use(h, sneaker, at, Direction.UP);
            h.assertTrue(
                    sneaker.getInventory().countItem(MineralModule.MINERAL_TAILINGS.get()) == 1,
                    "sneaking with empty hands takes a stack: the Tailings is next");
            h.succeed();
        });
    }

    /**
     * The demo's line with no hand in it: a chest of raw iron, a hopper, the Gizzard, a Yield Lobe on an output Mouth,
     * veins, an intake Mouth that takes only iron dust under a third-stage Bio-Furnace, its Yield Lobe on an output
     * Mouth and an intake Mouth that takes only ingots under a chest.
     */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 1000)
    public static void aChestOfRawIronBecomesIngotsThroughTheWholeLineWithNoHand(GameTestHelper h) {
        int raw = 2;
        BlockPos gizzard = gizzard(h, 2);
        h.setBlock(gizzard.above(), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        h.setBlock(gizzard.above(2), Blocks.CHEST);
        ChestBlockEntity ore = (ChestBlockEntity) h.getBlockEntity(gizzard.above(2));
        ore.setItem(0, new ItemStack(Items.RAW_IRON, raw));
        BlockPos lobeOut = gizzard.east();
        mouth(h, lobeOut.below(), MouthMode.OUTPUT);
        lobe(h, lobeOut, Direction.WEST);
        h.setBlock(lobeOut.below().east(), LogisticsModule.ITEM_VEIN.get());
        h.setBlock(lobeOut.below().east(2), LogisticsModule.ITEM_VEIN.get());
        BlockPos stove = lobeOut.east(3);
        h.setBlock(stove.below(), EcologyTests.mature());
        mouth(h, stove.below(), MouthMode.INTAKE);
        filter(h, stove.below(), MineralModule.IRON_DUST.get());
        furnaceAt(h, stove, THIRD_STAGE, 1000);
        BlockPos lobeIn = stove.east();
        mouth(h, lobeIn.below(), MouthMode.OUTPUT);
        lobe(h, lobeIn, Direction.WEST);
        h.setBlock(lobeIn.below().east(), LogisticsModule.ITEM_VEIN.get());
        BlockPos sink = lobeIn.east(2);
        mouth(h, sink.below(), MouthMode.INTAKE);
        filter(h, sink.below(), Items.IRON_INGOT);
        h.setBlock(sink, Blocks.CHEST);
        ChestBlockEntity ingots = (ChestBlockEntity) h.getBlockEntity(sink);
        h.runAfterDelay(900, () -> {
            h.assertValueEqual(ingots.countItem(Items.IRON_INGOT), raw, "every raw item became exactly one ingot");
            h.assertValueEqual(ore.countItem(Items.RAW_IRON), 0, "the chest is empty");
            BatchOrganContents ground = contents(h, gizzard);
            h.assertValueEqual(ground.outputs().get(0).getCount(), 0, "all the dust left the Gizzard");
            h.assertValueEqual(
                    ground.outputs().get(1).getCount(),
                    raw,
                    "the Tailings stay: no filter took them, the line does not clog on them");
            h.assertValueEqual(ground.outputs().get(2).getCount(), raw, "nor the residue");
            h.assertValueEqual(
                    ground.outputs().get(1).get(MineralModule.PARCEL.get()), Parcel.ground(Metal.IRON), "4 units each");
            BatchOrganContents smelted = contents(h, stove);
            h.assertTrue(smelted.ingredients().get(0).isEmpty(), "no dust left in the furnace");
            h.assertValueEqual(smelted.history().completedBatches(), THIRD_STAGE + raw, "two ingots smelted");
            h.assertValueEqual(smelted.biomass().stored().units(), 1000 - 40 * raw, "40 BU an ingot");
            h.assertValueEqual(ground.biomass().stored().units(), 1000 - 20 * raw, "20 BU a raw item");
            h.succeed();
        });
    }

    // --- per-metal conservation and the cost against direct smelting ---

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 760)
    public static void everyMetalIsConservedToTheItemThroughAGizzardAndAFurnace(GameTestHelper h) {
        int raw = 2;
        List<Metal> metals = List.of(Metal.values());
        for (int i = 0; i < metals.size(); i++) {
            BlockPos at = gizzard(h, 1 + 4 * i);
            furnace(h, 3 + 4 * i, THIRD_STAGE, 1000);
            ports(h, at).insertItem(0, new ItemStack(raw(metals.get(i)), raw), false);
        }
        int movedAt = 2 * GRIND_TICKS + 12;
        h.runAtTickTime(movedAt, () -> {
            for (int i = 0; i < metals.size(); i++) {
                Metal metal = metals.get(i);
                BlockPos at = new BlockPos(1 + 4 * i, Y + 1, 1);
                BlockPos stove = new BlockPos(3 + 4 * i, Y + 1, 1);
                IItemHandler gizzard = ports(h, at);
                ItemStack dust = gizzard.extractItem(1, 64, false);
                Grinding.Ground ground = Grinding.grind(raw);
                h.assertValueEqual((long) dust.getCount(), ground.dust(), metal.id() + ": four dust a raw item");
                h.assertTrue(dust.is(MineralModule.dust(metal)), metal.id() + ": the right dust");
                BatchOrganContents held = contents(h, at);
                h.assertValueEqual(
                        (long) held.outputs().get(1).getCount(), ground.tailings(), metal.id() + ": a Tailings each");
                long units = (long) held.outputs().get(1).getCount()
                        * held.outputs().get(1).get(MineralModule.PARCEL.get()).units();
                h.assertValueEqual(units, ground.residualUnits(), metal.id() + ": residual units");
                h.assertValueEqual((long) held.outputs().get(2).getCount(), ground.residue(), metal.id() + ": residue");
                h.assertValueEqual(
                        (long) dust.getCount() + units,
                        (long) raw * (Grinding.DUST_PER_RAW + Grinding.TAILINGS_UNITS),
                        metal.id() + ": every unit of the raw metal is dust or residual");
                h.assertTrue(
                        ports(h, stove).insertItem(0, dust, false).isEmpty(), metal.id() + ": the furnace takes it");
            }
        });
        h.runAtTickTime(movedAt + raw * THIRD_STAGE_SMELT_TICKS + 12, () -> {
            for (int i = 0; i < metals.size(); i++) {
                Metal metal = metals.get(i);
                BatchOrganContents smelted = contents(h, new BlockPos(3 + 4 * i, Y + 1, 1));
                Item ingot = ingot(metal);
                h.assertTrue(smelted.outputs().get(0).is(ingot), metal.id() + ": its own ingot");
                h.assertValueEqual(
                        (long) smelted.outputs().get(0).getCount(),
                        Grinding.smelt(Grinding.grind(raw).dust()).ingots(),
                        metal.id() + ": one ingot a raw item, to the item");
                h.assertTrue(smelted.ingredients().get(0).isEmpty(), metal.id() + ": no dust left over");
                float experience = metal == Metal.GOLD ? 2.0f : 1.4f;
                h.assertTrue(
                        Math.abs(smelted.experience() - experience) < 0.001f,
                        metal.id() + ": the raw item's own smelting experience once an ingot: " + smelted.experience());
            }
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 640)
    public static void theDryRouteCostsMoreBiomassAndTimeThanSmeltingTheRawItemDirectly(GameTestHelper h) {
        BlockPos direct = furnace(h, 1, 0, 1000);
        ports(h, direct).insertItem(0, new ItemStack(Items.RAW_IRON), false);
        BlockPos gizzard = gizzard(h, 3);
        BlockPos stove = furnace(h, 5, 0, 1000);
        ports(h, gizzard).insertItem(0, new ItemStack(Items.RAW_IRON), false);
        int[] directDone = {-1};
        int[] routeDone = {-1};
        for (int tick = 1; tick <= 520; tick++) {
            int at = tick;
            h.runAtTickTime(at, () -> {
                if (directDone[0] < 0 && contents(h, direct).outputs().get(0).is(Items.IRON_INGOT)) directDone[0] = at;
                if (routeDone[0] < 0 && contents(h, stove).outputs().get(0).is(Items.IRON_INGOT)) routeDone[0] = at;
                BatchOrganContents ground = contents(h, gizzard);
                if (ground.batch().isEmpty() && !ground.outputs().get(0).isEmpty())
                    ports(h, stove).insertItem(0, ports(h, gizzard).extractItem(1, 64, false), false);
            });
        }
        h.runAtTickTime(521, () -> {
            BatchOrganContents directly = contents(h, direct);
            BatchOrganContents route = contents(h, stove);
            BatchOrganContents ground = contents(h, gizzard);
            int directBiomass = 1000 - directly.biomass().stored().units();
            int routeBiomass = (1000 - ground.biomass().stored().units())
                    + (1000 - route.biomass().stored().units());
            h.assertValueEqual(directly.outputs().get(0).getCount(), 1, "direct smelting: one ingot");
            h.assertValueEqual(route.outputs().get(0).getCount(), 1, "the dry route: the same one ingot");
            h.assertValueEqual(directBiomass, 40, "direct smelting costs 40 BU");
            h.assertValueEqual(routeBiomass, 60, "the dry route costs 60 BU: 20 to grind and 40 to smelt");
            h.assertTrue(directDone[0] > 0 && routeDone[0] > directDone[0], "and takes longer");
            LOGGER.info(
                    "MINERAL LEDGER per raw iron at a first-stage furnace: direct {} BU, ingot at tick {}; dry route {} BU "
                            + "(20 grind + 40 smelt), ingot at tick {}; the route also leaves 1 Tailings (4 units) and 1 Rock Residue",
                    directBiomass,
                    directDone[0],
                    routeBiomass,
                    routeDone[0]);
            h.succeed();
        });
    }

    // --- fixtures ---

    private static Item raw(Metal metal) {
        return switch (metal) {
            case IRON -> Items.RAW_IRON;
            case GOLD -> Items.RAW_GOLD;
            case COPPER -> Items.RAW_COPPER;
        };
    }

    private static Item ingot(Metal metal) {
        return switch (metal) {
            case IRON -> Items.IRON_INGOT;
            case GOLD -> Items.GOLD_INGOT;
            case COPPER -> Items.COPPER_INGOT;
        };
    }

    private static ItemStack withCount(ItemStack stack, int count) {
        stack.setCount(count);
        return stack;
    }

    /** A Gizzard at (x, 2, 1) on substrate with 1000 BU. */
    private static BlockPos gizzard(GameTestHelper h, int x) {
        return gizzard(h, x, 1000);
    }

    private static BlockPos gizzard(GameTestHelper h, int x, int biomass) {
        BlockPos at = new BlockPos(x, Y + 1, 1);
        h.setBlock(at.below(), EcologyTests.mature());
        h.setBlock(at, ProcessingModule.MINERAL_GIZZARD.get());
        h.getLevel()
                .getCapability(BiomassCapabilities.BLOCK, h.absolutePos(at), null)
                .insert(Biomass.of(biomass), false);
        return at;
    }

    /** A Bio-Furnace at (x, 2, 1) with {@code history} completed batches and {@code biomass} BU. */
    private static BlockPos furnace(GameTestHelper h, int x, int history, int biomass) {
        BlockPos at = new BlockPos(x, Y + 1, 1);
        h.setBlock(at.below(), EcologyTests.mature());
        furnaceAt(h, at, history, biomass);
        return at;
    }

    private static void furnaceAt(GameTestHelper h, BlockPos at, int history, int biomass) {
        h.setBlock(at, ProcessingModule.BIO_FURNACE.get());
        BiomassTank tank =
                ((Outcome.Done<BiomassTank>) BiomassTank.empty(Biomass.of(2000)).fill(Biomass.of(biomass))).value();
        h.getBlockEntity(at)
                .applyComponents(
                        DataComponentMap.builder()
                                .set(
                                        ProcessingModule.BATCH_CONTENTS.get(),
                                        new BatchOrganContents(
                                                List.of(ItemStack.EMPTY),
                                                ItemStack.EMPTY,
                                                0,
                                                tank,
                                                new OrganHistory(history),
                                                Optional.empty()))
                                .build(),
                        DataComponentPatch.EMPTY);
    }

    /** Replaces what the organ at {@code at} holds. */
    private static void preset(
            GameTestHelper h,
            BlockPos at,
            BatchOrganContents like,
            List<ItemStack> ingredients,
            List<ItemStack> outputs,
            Optional<OrganBatch> batch) {
        h.getBlockEntity(at)
                .applyComponents(
                        DataComponentMap.builder()
                                .set(
                                        ProcessingModule.BATCH_CONTENTS.get(),
                                        new BatchOrganContents(
                                                ingredients,
                                                outputs,
                                                like.waterMb(),
                                                like.biomass(),
                                                like.history(),
                                                batch,
                                                Optional.empty(),
                                                like.experience()))
                                .build(),
                        DataComponentPatch.EMPTY);
    }

    /** Saves the organ to bytes, reads them into a fresh block entity of the same kind and puts that in its place. */
    private static void reload(GameTestHelper h, BlockPos at) {
        var registries = h.getLevel().registryAccess();
        BlockEntity live = h.getBlockEntity(at);
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            NbtIo.writeCompressed(live.saveWithoutMetadata(registries), bytes);
            CompoundTag read =
                    NbtIo.readCompressed(new ByteArrayInputStream(bytes.toByteArray()), NbtAccounter.unlimitedHeap());
            BlockPos absolute = h.absolutePos(at);
            BlockEntity fresh =
                    ((EntityBlock) h.getBlockState(at).getBlock()).newBlockEntity(absolute, h.getBlockState(at));
            fresh.loadWithComponents(read, registries);
            h.getLevel().removeBlockEntity(absolute);
            h.getLevel().setBlockEntity(fresh);
        } catch (IOException e) {
            h.fail("could not save and read the organ: " + e.getMessage());
        }
    }

    private static IItemHandler ports(GameTestHelper h, BlockPos at) {
        return h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(at), Direction.UP);
    }

    private static BatchOrganContents contents(GameTestHelper h, BlockPos at) {
        BatchOrganContents contents =
                h.getBlockEntity(at).collectComponents().get(ProcessingModule.BATCH_CONTENTS.get());
        if (contents == null) h.fail("no organ contents", at);
        return contents;
    }

    private static BatchOrganMenu menu(GameTestHelper h, BlockPos at) {
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
        return (BatchOrganMenu) ((MenuProvider) h.getBlockEntity(at)).createMenu(1, player.getInventory(), player);
    }

    private static void mouth(GameTestHelper h, BlockPos at, MouthMode mode) {
        h.setBlock(at, LogisticsModule.MOUTH.get().defaultBlockState().setValue(MouthBlock.MODE, mode));
    }

    /** Lets the intake Mouth at {@code mouth} take only {@code item}: its saved filter, as its screen would set it. */
    private static void filter(GameTestHelper h, BlockPos mouth, Item item) {
        var registries = h.getLevel().registryAccess();
        BlockEntity entity = h.getBlockEntity(mouth);
        CompoundTag tag = entity.saveWithoutMetadata(registries);
        CompoundTag entry = new CompoundTag();
        entry.putString(
                "id",
                net.minecraft.core.registries.BuiltInRegistries.ITEM
                        .getKey(item)
                        .toString());
        entry.putInt("count", 1);
        var list = new net.minecraft.nbt.ListTag();
        list.add(entry);
        tag.put("filter", list);
        entity.loadWithComponents(tag, registries);
    }

    private static void lobe(GameTestHelper h, BlockPos at, Direction toOrgan) {
        BlockState state = LogisticsModule.YIELD_LOBE
                .get()
                .defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, toOrgan);
        if (!h.getBlockState(at.below()).is(Organs.FOUNDATIONS)) h.setBlock(at.below(), EcologyTests.mature());
        h.setBlock(at, state);
    }
}
