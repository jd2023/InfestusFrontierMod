package org.jd.infestusfrontier.testmod;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.SharedConstants;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.core.foundation.Outcome;
import org.jd.infestusfrontier.core.organ.OrganHistory;
import org.jd.infestusfrontier.core.processing.Batch;
import org.jd.infestusfrontier.core.storage.BiomassTank;
import org.jd.infestusfrontier.discovery.PendingCredits;
import org.jd.infestusfrontier.ecology.EcologyModule;
import org.jd.infestusfrontier.processing.ProcessingModule;
import org.jd.infestusfrontier.processing.api.BatchOrganContents;
import org.jd.infestusfrontier.processing.api.OrganBatch;

/**
 * The Infestus Frontier advancement tab: it opens when a player obtains a log, keeps what a player earned before,
 * and its starter milestones come from batches that finish, never from owning the organ.
 */
@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class StarterAdvancementTests {
    private static final String ROOT = "contact/root";
    private static final List<String> CONTACT = List.of(
            "a_culture_in_your_hand",
            "living_substrate",
            "organ_bud",
            "digestive_sac",
            "biomass_bladder",
            "awakening",
            "membrane_sheet",
            "bone_plate",
            "bio_furnace",
            "planting_proboscis",
            "harvest_corolla",
            "supplied_sac",
            "automatic_supply",
            "tree_storage",
            "palm_from_storage",
            "raw_to_dust",
            "washed_tailings",
            "dust_ingot",
            "ore_workshop");
    private static final BlockPos RACK = new BlockPos(1, 2, 1);
    private static final BlockPos LOOM = new BlockPos(3, 2, 1);
    private static final BlockPos FURNACE = new BlockPos(5, 2, 1);
    private static final int RACK_TICKS = 400;
    private static final int LOOM_TICKS = 400;
    private static final int FURNACE_TICKS = 320;
    private static final String RAW_IRON_RECIPE = "minecraft:iron_ingot_from_smelting_raw_iron";

    static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("infestusfrontier", path);
    }

    static AdvancementHolder holder(GameTestHelper h, String path) {
        AdvancementHolder holder = h.getLevel().getServer().getAdvancements().get(id(path));
        h.assertTrue(holder != null, "advancement " + path + " loaded");
        return holder;
    }

    static boolean done(GameTestHelper h, ServerPlayer player, String path) {
        return player.getAdvancements().getOrStartProgress(holder(h, path)).isDone();
    }

    /**
     * A player the server does not list. Placing a player in the GameTest server makes the guide build its books,
     * and from then on every advancement an unplaced player earns tries to send them a packet; so the tests use
     * unplaced players, whose batches leave a credit, and the real client capture covers the online player.
     */
    static ServerPlayer player(GameTestHelper h) {
        GameProfile profile = new GameProfile(UUID.randomUUID(), "starter-tester");
        ServerPlayer player =
                new ServerPlayer(h.getLevel().getServer(), h.getLevel(), profile, ClientInformation.createDefault());
        // A recipe an advancement unlocks is sent to the player: give the unplaced player a wire that goes nowhere.
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        new ServerGamePacketListenerImpl(
                h.getLevel().getServer(), connection, player, CommonListenerCookie.createInitial(profile, false));
        return player;
    }

    /** Puts {@code item} in the inventory and runs the trigger a pickup runs: the menu listener is a client's. */
    private static void obtain(ServerPlayer player, Item item) {
        ItemStack stack = new ItemStack(item);
        player.getInventory().add(stack.copy()); // the inventory takes the stack it is given
        CriteriaTriggers.INVENTORY_CHANGED.trigger(player, player.getInventory(), stack);
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void theTabIsHiddenAndOnlyALogOrOldProgressOpensIt(GameTestHelper h) {
        AdvancementHolder root = holder(h, ROOT);
        DisplayInfo display = root.value().display().orElseThrow();
        h.assertTrue(display.isHidden(), "the root hides until it is earned");
        h.assertTrue(root.value().parent().isEmpty(), "it is a tab root");
        h.assertValueEqual(
                root.value().criteria().keySet(),
                Set.of("never", "has_log"),
                "the old criterion stays so earned progress counts; the log is new");
        h.assertValueEqual(
                root.value().criteria().get("has_log").trigger(),
                CriteriaTriggers.INVENTORY_CHANGED,
                "a log is an inventory change, no command");
        AdvancementRequirements requirements = root.value().requirements();
        h.assertValueEqual(requirements.size(), 1, "one group");
        h.assertValueEqual(requirements.names(), Set.of("never", "has_log"), "either criterion earns the root");
        h.assertTrue(
                root.value().criteria().get("has_log").triggerInstance()
                        instanceof InventoryChangeTrigger.TriggerInstance,
                "an inventory condition");
        h.assertTrue(
                root.value().rewards().recipes().isEmpty()
                        && root.value().rewards().loot().isEmpty()
                        && root.value().rewards().experience() == 0,
                "no reward");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void joiningGrantsNothingAndOnlyALogRevealsTheTab(GameTestHelper h) {
        ServerPlayer player = player(h);
        h.assertFalse(done(h, player, ROOT), "a player who has just joined has no tab");
        for (String path : CONTACT)
            h.assertFalse(done(h, player, "contact/" + path), path + " is not granted by joining");
        obtain(player, Items.OAK_PLANKS);
        obtain(player, Items.STICK);
        obtain(player, Items.OAK_SAPLING);
        h.assertFalse(done(h, player, ROOT), "planks, sticks and saplings are not logs");
        obtain(player, Items.OAK_LOG);
        h.assertTrue(done(h, player, ROOT), "an oak log opens the tab");
        for (String path : CONTACT) h.assertFalse(done(h, player, "contact/" + path), path + " is still to do");
        ServerPlayer nether = player(h);
        obtain(nether, Items.CRIMSON_STEM);
        h.assertTrue(done(h, nether, ROOT), "any block of the logs tag opens it, a stem too");
        h.assertFalse(done(h, player, "contact/a_culture_in_your_hand"), "the log earns only the root");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void everyLogOfTheTagOpensTheTab(GameTestHelper h) {
        var tag = net.minecraft.tags.ItemTags.LOGS;
        int checked = 0;
        for (Item item : BuiltInRegistries.ITEM) {
            if (!item.getDefaultInstance().is(tag)) continue;
            ServerPlayer player = player(h);
            ItemStack log = new ItemStack(item);
            player.getInventory().add(log.copy());
            // an unplaced player has no menu listener: run the trigger a pickup would
            CriteriaTriggers.INVENTORY_CHANGED.trigger(player, player.getInventory(), log);
            h.assertTrue(done(h, player, ROOT), BuiltInRegistries.ITEM.getKey(item) + " opens the tab");
            checked++;
        }
        h.assertTrue(checked > 10, "the logs tag has its members");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aPlayerWhoAlreadyHadTheRootKeepsItAndTheirProgress(GameTestHelper h) throws IOException {
        ServerPlayer player = player(h);
        Path file = h.getLevel()
                .getServer()
                .getWorldPath(LevelResource.PLAYER_ADVANCEMENTS_DIR)
                .resolve(player.getUUID() + ".json");
        Files.createDirectories(file.getParent());
        String when = "2026-01-01 00:00:00 +0000";
        // What the shipped build wrote when an operator granted the old root and a player then made a culture.
        Files.writeString(
                file,
                "{\"DataVersion\":"
                        + SharedConstants.getCurrentVersion().getDataVersion().getVersion()
                        + ",\"infestusfrontier:contact/root\":{\"criteria\":{\"never\":\"" + when
                        + "\"},\"done\":true},\"infestusfrontier:contact/a_culture_in_your_hand\":{\"criteria\":"
                        + "{\"has_culture\":\"" + when + "\"},\"done\":true},"
                        + "\"infestusfrontier:contact/digestive_sac\":{\"criteria\":{\"digested\":\"" + when
                        + "\"},\"done\":true}}",
                StandardCharsets.UTF_8);
        try {
            player.getAdvancements().reload(h.getLevel().getServer().getAdvancements());
            h.assertTrue(done(h, player, ROOT), "the root a player already had is still earned");
            h.assertTrue(done(h, player, "contact/a_culture_in_your_hand"), "so is the culture");
            h.assertTrue(done(h, player, "contact/digestive_sac"), "so is the first biomass");
            h.assertFalse(done(h, player, "contact/living_substrate"), "and nothing else appeared");
            h.assertFalse(done(h, player, "contact/membrane_sheet"), "the new milestones wait for work");
        } finally {
            Files.deleteIfExists(file);
        }
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void everyMilestoneHangsUnderTheRootAndShowsAnActionNotAReward(GameTestHelper h) {
        var tree = h.getLevel().getServer().getAdvancements().tree();
        JsonObject lang = lang();
        List<String> titled = new ArrayList<>(CONTACT);
        titled.add("root");
        for (String path : titled) {
            AdvancementHolder holder = holder(h, "contact/" + path);
            DisplayInfo display = holder.value().display().orElseThrow();
            h.assertFalse(
                    display.isHidden() && !path.equals("root"), path + " is shown as an upcoming step once reachable");
            AdvancementNode node = tree.get(holder);
            while (node.parent() != null) node = node.parent();
            h.assertValueEqual(node.holder().id(), id(ROOT), path + " leads back to the root");
            h.assertTrue(
                    holder.value().rewards().recipes().isEmpty()
                            && holder.value().rewards().loot().isEmpty()
                            && holder.value().rewards().experience() == 0,
                    path + " grants no material");
            for (String part : List.of("title", "description")) {
                String key = "advancement.infestusfrontier.contact." + path + "." + part;
                h.assertTrue(lang.has(key) && !lang.get(key).getAsString().isBlank(), key + " is translated");
            }
        }
        String root = lang.get("advancement.infestusfrontier.contact.root.description")
                .getAsString();
        for (String ingredient : List.of("rotten flesh", "red mushroom", "wheat seeds"))
            h.assertTrue(root.contains(ingredient), "the root names " + ingredient);
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void theNewMilestonesCompleteOnTheirOrgansBatchesAndShowTheirItems(GameTestHelper h) {
        record Step(String path, String parent, String organ, Item icon) {}
        for (Step step : List.of(
                new Step("membrane_sheet", "living_substrate", "membrane_rack", ProcessingModule.MEMBRANE_SHEET.get()),
                new Step("bone_plate", "digestive_sac", "bone_loom", ProcessingModule.BONE_PLATE.get()),
                new Step("bio_furnace", "membrane_sheet", "bio_furnace", ProcessingModule.BIO_FURNACE_ITEM.get()))) {
            var advancement = holder(h, "contact/" + step.path()).value();
            h.assertValueEqual(advancement.parent().orElseThrow(), id("contact/" + step.parent()), "parent");
            h.assertValueEqual(
                    advancement.display().orElseThrow().getIcon().getItem(), step.icon(), step.path() + " icon");
            h.assertValueEqual(advancement.criteria().size(), 1, "one criterion");
            var criterion = advancement.criteria().values().iterator().next();
            h.assertValueEqual(
                    BuiltInRegistries.TRIGGER_TYPES.getKey(criterion.trigger()).toString(),
                    "infestusfrontier:organ_batch_completed",
                    step.path() + " uses the batch trigger");
            h.assertTrue(
                    criterion.triggerInstance().toString().contains(step.organ()),
                    step.path() + " names " + step.organ());
        }
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void recipeAndAcquiredFlagsStayInvisible(GameTestHelper h) {
        int flags = 0;
        for (AdvancementHolder holder :
                h.getLevel().getServer().getAdvancements().getAllAdvancements()) {
            String path = holder.id().getPath();
            if (!holder.id().getNamespace().equals("infestusfrontier")) continue;
            if (path.startsWith("acquired/") || path.startsWith("recipes/")) {
                h.assertTrue(holder.value().display().isEmpty(), path + " has no display");
                flags++;
            }
        }
        h.assertTrue(flags > 100, "the discovery flags are all still there: " + flags);
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void holdingTheOrgansOrTheirProductsIsNotOperatingThem(GameTestHelper h) {
        ServerPlayer player = player(h);
        obtain(player, ProcessingModule.MEMBRANE_RACK_ITEM.get());
        obtain(player, ProcessingModule.MEMBRANE_SHEET.get());
        obtain(player, ProcessingModule.BONE_LOOM_ITEM.get());
        obtain(player, ProcessingModule.BONE_PLATE.get());
        obtain(player, ProcessingModule.BIO_FURNACE_ITEM.get());
        h.assertTrue(done(h, player, "acquired/membrane_sheet"), "the guide still opens on the sheet");
        h.assertTrue(done(h, player, "acquired/bone_plate"), "and on the plate");
        h.assertTrue(done(h, player, "acquired/bio_furnace"), "and on the furnace");
        for (String path : List.of("membrane_sheet", "bone_plate", "bio_furnace"))
            h.assertFalse(done(h, player, "contact/" + path), path + " waits for a batch");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 500)
    public static void eachFinishedBatchCreditsItsFeederAndTheCreditWaitsForLogin(GameTestHelper h) {
        ServerPlayer player = player(h);
        ServerPlayer bystander = player(h);
        PendingCredits credits = PendingCredits.get(h.getLevel());
        placeWorkshop(h, 1000, 800);
        feed(h, player, RACK, Items.ROTTEN_FLESH);
        feed(h, player, RACK, Items.STRING);
        feed(h, player, LOOM, Items.BONE);
        feed(h, player, FURNACE, Items.RAW_IRON);
        h.runAtTickTime(FURNACE_TICKS - 10, () -> {
            for (String organ : List.of("membrane_rack", "bone_loom", "bio_furnace"))
                h.assertFalse(credits.holds(player.getUUID(), id(organ)), organ + " has finished nothing yet");
        });
        h.runAtTickTime(FURNACE_TICKS + 12, () -> {
            h.assertTrue(credits.holds(player.getUUID(), id("bio_furnace")), "the furnace batch ended");
            h.assertFalse(credits.holds(player.getUUID(), id("membrane_rack")), "the rack batch has not");
            h.assertFalse(credits.holds(player.getUUID(), id("bone_loom")), "the loom batch has not");
        });
        h.runAtTickTime(RACK_TICKS + 12, () -> {
            for (String organ : List.of("membrane_rack", "bone_loom", "bio_furnace")) {
                h.assertTrue(credits.holds(player.getUUID(), id(organ)), organ + " credit waits for the feeder");
                h.assertFalse(credits.holds(bystander.getUUID(), id(organ)), organ + " credits the feeder only");
            }
            for (String path : List.of("membrane_sheet", "bone_plate", "bio_furnace"))
                h.assertFalse(done(h, player, "contact/" + path), path + " is not earned while away");
            credits.award(bystander);
            for (String path : List.of("membrane_sheet", "bone_plate", "bio_furnace"))
                h.assertFalse(done(h, bystander, "contact/" + path), path + " is not the bystander's");
            credits.award(player);
            for (String path : List.of("membrane_sheet", "bone_plate", "bio_furnace"))
                h.assertTrue(done(h, player, "contact/" + path), path + " is earned on login");
            for (String organ : List.of("membrane_rack", "bone_loom", "bio_furnace"))
                h.assertFalse(credits.holds(player.getUUID(), id(organ)), organ + " credit is spent");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 200)
    public static void batchesThatCannotStartEarnNothing(GameTestHelper h) {
        ServerPlayer player = player(h);
        PendingCredits credits = PendingCredits.get(h.getLevel());
        placeWorkshop(h, 0, 0); // no water for the Rack, no biomass for the Loom and the Furnace
        feed(h, player, RACK, Items.ROTTEN_FLESH);
        feed(h, player, RACK, Items.STRING);
        feed(h, player, LOOM, Items.BONE);
        feed(h, player, FURNACE, Items.RAW_IRON);
        h.runAtTickTime(120, () -> {
            for (BlockPos organ : List.of(RACK, LOOM, FURNACE))
                h.assertTrue(contents(h, organ).batch().isEmpty(), "no batch runs at " + organ);
            credits.award(player);
            for (String path : List.of("membrane_sheet", "bone_plate", "bio_furnace"))
                h.assertFalse(done(h, player, "contact/" + path), path + " is not earned by a batch that never ran");
            for (String organ : List.of("membrane_rack", "bone_loom", "bio_furnace"))
                h.assertFalse(credits.holds(player.getUUID(), id(organ)), organ + " left no credit");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 200)
    public static void aFinishedBatchWithNoRoomEarnsNothingUntilItIsDelivered(GameTestHelper h) {
        ServerPlayer player = player(h);
        PendingCredits credits = PendingCredits.get(h.getLevel());
        placeWorkshop(h, 0, 800);
        OrganBatch almostDone = new OrganBatch(
                new Batch("infestusfrontier:preparing/bone_plate", LOOM_TICKS, LOOM_TICKS - 1),
                new ItemStack(ProcessingModule.BONE_PLATE.get()),
                Optional.of(player.getUUID()),
                0f);
        preset(h, LOOM, 1, new ItemStack(ProcessingModule.BONE_PLATE.get(), 64), Optional.of(almostDone), 800);
        h.runAtTickTime(30, () -> {
            h.assertTrue(contents(h, LOOM).batch().isPresent(), "the finished batch waits for room");
            h.assertFalse(
                    credits.holds(player.getUUID(), id("bone_loom")), "a plate that cannot come out earns nothing");
            preset(h, LOOM, 1, ItemStack.EMPTY, Optional.of(almostDone.advance()), 800);
        });
        h.runAtTickTime(45, () -> {
            h.assertTrue(contents(h, LOOM).batch().isEmpty(), "the batch is delivered once there is room");
            h.assertTrue(credits.holds(player.getUUID(), id("bone_loom")), "and then it counts");
            credits.award(player);
            h.assertTrue(done(h, player, "contact/bone_plate"), "once the feeder is back");
            h.succeed();
        });
    }

    private static void placeWorkshop(GameTestHelper h, int waterMb, int biomassUnits) {
        for (BlockPos organ : List.of(RACK, LOOM, FURNACE))
            h.setBlock(organ.below(), EcologyModule.LIVING_SUBSTRATE.get());
        h.setBlock(RACK, ProcessingModule.MEMBRANE_RACK.get());
        h.setBlock(LOOM, ProcessingModule.BONE_LOOM.get());
        h.setBlock(FURNACE, ProcessingModule.BIO_FURNACE.get());
        preset(h, RACK, 2, ItemStack.EMPTY, Optional.empty(), 0, waterMb);
        preset(h, LOOM, 1, ItemStack.EMPTY, Optional.empty(), biomassUnits);
        preset(h, FURNACE, 1, ItemStack.EMPTY, Optional.empty(), biomassUnits);
    }

    private static void preset(
            GameTestHelper h, BlockPos organ, int slots, ItemStack output, Optional<OrganBatch> batch, int biomass) {
        preset(h, organ, slots, output, batch, biomass, 0);
    }

    /** An organ holding no ingredients, {@code waterMb} of water and {@code biomass} units, ready to be fed by hand. */
    private static void preset(
            GameTestHelper h,
            BlockPos organ,
            int slots,
            ItemStack output,
            Optional<OrganBatch> batch,
            int biomass,
            int waterMb) {
        Biomass capacity = organ.equals(RACK) ? Biomass.ZERO : Biomass.of(organ.equals(LOOM) ? 1000 : 2000);
        BiomassTank tank = BiomassTank.empty(capacity);
        if (biomass > 0) tank = ((Outcome.Done<BiomassTank>) tank.fill(Biomass.of(biomass))).value();
        List<ItemStack> held = new ArrayList<>();
        for (int n = 0; n < slots; n++) held.add(ItemStack.EMPTY);
        h.getBlockEntity(organ)
                .applyComponents(
                        DataComponentMap.builder()
                                .set(
                                        ProcessingModule.BATCH_CONTENTS.get(),
                                        new BatchOrganContents(held, output, waterMb, tank, OrganHistory.NONE, batch))
                                .build(),
                        DataComponentPatch.EMPTY);
    }

    private static BatchOrganContents contents(GameTestHelper h, BlockPos organ) {
        return h.getBlockEntity(organ).collectComponents().get(ProcessingModule.BATCH_CONTENTS.get());
    }

    /** The player puts one {@code item} into the organ by hand, as a right-click does. */
    private static void feed(GameTestHelper h, ServerPlayer player, BlockPos organ, Item item) {
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
        EcologyTests.use(h, player, organ, Direction.UP);
    }

    /** The shipped English text, read from the mod's own files; the server's resource manager holds data only. */
    private static JsonObject lang() {
        Path file = ModList.get()
                .getModFileById("infestusfrontier")
                .getFile()
                .findResource("assets", "infestusfrontier", "lang", "en_us.json");
        try (var in = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(in).getAsJsonObject();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
