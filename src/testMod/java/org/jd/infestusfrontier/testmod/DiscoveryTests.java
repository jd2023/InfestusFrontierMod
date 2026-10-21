package org.jd.infestusfrontier.testmod;

import com.klikli_dev.modonomicon.book.conditions.BookOrCondition;
import com.klikli_dev.modonomicon.data.BookDataManager;
import com.klikli_dev.modonomicon.registry.DataComponentRegistry;
import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jd.infestusfrontier.discovery.PendingCredits;
import org.jd.infestusfrontier.ecology.EcologyModule;
import org.jd.infestusfrontier.integration.modonomicon.OrganBatchPage;
import org.jd.infestusfrontier.processing.api.OrganBatchCompletedEvent;
import org.jd.infestusfrontier.storage.StorageModule;
import org.jd.infestusfrontier.storage.api.BiomassBucketEvent;

/** The Waking Genome guide: its advancements, its recipe and its book data. */
@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class DiscoveryTests {
    private static final ResourceLocation BOOK = id("the_waking_genome");

    private static final BlockPos SAC = new BlockPos(1, 2, 1);

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("infestusfrontier", path);
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void advancementsLoad(GameTestHelper h) {
        for (String path : List.of("contact/root", "contact/a_culture_in_your_hand", "contact/living_substrate")) {
            AdvancementHolder holder =
                    h.getLevel().getServer().getAdvancements().get(id(path));
            h.assertTrue(holder != null, "advancement " + path + " loaded");
        }
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void cultureAdvancementHasInventoryCriterion(GameTestHelper h) {
        AdvancementHolder holder = h.getLevel().getServer().getAdvancements().get(id("contact/a_culture_in_your_hand"));
        h.assertTrue(holder != null, "advancement loaded");
        h.assertTrue(holder.value().criteria().containsKey("has_culture"), "criterion has_culture present");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void bookRecipeResolves(GameTestHelper h) {
        List<ItemStack> inputs = List.of(new ItemStack(Items.BOOK), new ItemStack(EcologyModule.SPORE_CULTURE.get()));
        var recipe = h.getLevel()
                .getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, CraftingInput.of(2, 1, inputs), h.getLevel());
        h.assertTrue(recipe.isPresent(), "recipe present");
        ItemStack result = recipe.get()
                .value()
                .assemble(CraftingInput.of(2, 1, inputs), h.getLevel().registryAccess());
        h.assertTrue(
                BuiltInRegistries.ITEM.getKey(result.getItem()).toString().equals("modonomicon:modonomicon"),
                "result is modonomicon:modonomicon");
        h.assertTrue(BOOK.equals(result.get(DataComponentRegistry.BOOK_ID.get())), "book_id is the_waking_genome");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void bookLoads(GameTestHelper h) {
        h.assertTrue(BookDataManager.get().getBooks().containsKey(BOOK), "book known to BookDataManager");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void workshopEntriesLoad(GameTestHelper h) {
        var book = BookDataManager.get().getBook(BOOK);
        h.assertTrue(book != null, "book known to BookDataManager");
        var category = book.getCategory(id("contact"));
        h.assertTrue(category != null, "category contact loaded");
        for (String entry : List.of(
                "organ_bud",
                "digestive_sac",
                "biomass_bladder",
                "biomass_bucket",
                "culture_bowl",
                "membrane_rack",
                "bone_loom",
                "awakening_cradle",
                "bio_furnace",
                "dormant_pieces",
                "speak_to_an_organ")) {
            h.assertTrue(category.getEntries().containsKey(id("contact/" + entry)), "entry " + entry + " loaded");
        }
        var circulation = book.getCategory(id("circulation"));
        h.assertTrue(circulation != null, "category circulation loaded");
        for (String entry : List.of(
                "activation_cyst",
                "biomass_vein",
                "item_vein",
                "fluid_vein",
                "vascular_junction",
                "mouth",
                "cultivation_tissue",
                "seed_pouch",
                "planting_proboscis",
                "harvest_corolla",
                "compost_gland",
                "feeding_lobe",
                "yield_lobe",
                "sensor_polyp",
                "fuel_papilla",
                "filter_valve",
                "reservoir_cell"))
            h.assertTrue(
                    circulation.getEntries().containsKey(id("circulation/" + entry)), "entry " + entry + " loaded");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void bowlEntryShowsItsBatchRecipes(GameTestHelper h) {
        var entry = BookDataManager.get()
                .getBook(BOOK)
                .getCategory(id("contact"))
                .getEntries()
                .get(id("contact/culture_bowl"));
        h.assertTrue(entry != null, "entry loaded");
        List<ResourceLocation> recipes = entry.getPages().stream()
                .filter(page -> page instanceof OrganBatchPage)
                .map(page -> ((OrganBatchPage) page).getRecipeId1())
                .toList();
        h.assertValueEqual(
                recipes,
                List.of(id("culturing/spore_culture"), id("culturing/organ_bud")),
                "one page per batch recipe");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void entriesUnlockByStepOrByItem(GameTestHelper h) {
        var contact =
                BookDataManager.get().getBook(BOOK).getCategory(id("contact")).getEntries();
        for (String name : List.of(
                "living_substrate",
                "organ_bud",
                "digestive_sac",
                "biomass_bladder",
                "biomass_bucket",
                "culture_bowl",
                "membrane_rack",
                "bone_loom",
                "awakening_cradle",
                "bio_furnace",
                "dormant_pieces",
                "speak_to_an_organ")) {
            var condition = contact.get(id("contact/" + name)).getCondition();
            h.assertTrue(condition instanceof BookOrCondition, name + " unlocks by step or by item");
        }
        var advancements = h.getLevel().getServer().getAdvancements();
        for (String item : List.of(
                "spore_culture",
                "living_substrate",
                "organ_bud",
                "digestive_sac",
                "biomass_bladder",
                "biomass_bucket",
                "culture_bowl",
                "synaptic_probe",
                "membrane_rack",
                "membrane_sheet",
                "bone_loom",
                "bone_plate",
                "awakening_cradle",
                "bio_furnace",
                "dormant_thorax",
                "awakened_thorax"))
            h.assertTrue(advancements.get(id("acquired/" + item)) != null, "acquired/" + item + " exists");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void substrateAdvancementGrantedOnConversion(GameTestHelper h) {
        BlockPos ground = new BlockPos(1, 1, 1);
        h.setBlock(ground, Blocks.GRASS_BLOCK);
        ServerPlayer player = new ServerPlayer(
                h.getLevel().getServer(),
                h.getLevel(),
                new GameProfile(UUID.randomUUID(), "advancement-tester"),
                ClientInformation.createDefault());
        ItemStack stack = new ItemStack(EcologyModule.SPORE_CULTURE.get(), 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockHitResult hit = new BlockHitResult(
                Vec3.atCenterOf(h.absolutePos(ground)).add(0, 0.5, 0), Direction.UP, h.absolutePos(ground), false);
        player.gameMode.useItemOn(player, h.getLevel(), stack, InteractionHand.MAIN_HAND, hit);
        AdvancementHolder holder = h.getLevel().getServer().getAdvancements().get(id("contact/living_substrate"));
        h.assertTrue(holder != null, "advancement loaded");
        h.assertTrue(h.getBlockState(ground).is(EcologyModule.LIVING_SUBSTRATE.get()), "cell converted");
        h.assertTrue(
                player.getAdvancements().getOrStartProgress(holder).isDone(), "living_substrate advancement is done");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void advancementsLoadWithTriggers(GameTestHelper h) {
        Map<String, String> expected = Map.of(
                "contact/organ_bud", "minecraft:inventory_changed",
                "contact/digestive_sac", "infestusfrontier:organ_batch_completed",
                "contact/biomass_bladder", "infestusfrontier:biomass_bucket");
        expected.forEach((path, trigger) -> {
            AdvancementHolder holder =
                    h.getLevel().getServer().getAdvancements().get(id(path));
            h.assertTrue(holder != null, "advancement " + path + " loaded");
            h.assertTrue(
                    holder.value().criteria().values().stream()
                            .map(c -> BuiltInRegistries.TRIGGER_TYPES
                                    .getKey(c.trigger())
                                    .toString())
                            .toList()
                            .equals(List.of(trigger)),
                    path + " uses " + trigger);
        });
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 100)
    public static void sacPostsCompletionForItsFeeder(GameTestHelper h) {
        ProcessingTests.placeSac(h);
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, Items.WHEAT, 1);
        List<OrganBatchCompletedEvent> seen = new ArrayList<>();
        // The bus is shared with every test running in the same ticks; only this player's events count.
        Consumer<OrganBatchCompletedEvent> listener = event -> {
            if (event.feeder().equals(player.getUUID())) seen.add(event);
        };
        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, OrganBatchCompletedEvent.class, listener);
        EcologyTests.use(h, player, SAC, Direction.UP);
        h.runAfterDelay(42, () -> {
            NeoForge.EVENT_BUS.unregister(listener);
            h.assertValueEqual(seen.size(), 1, "one completion event");
            h.assertValueEqual(seen.get(0).organ(), id("digestive_sac"), "organ");
            h.assertValueEqual(seen.get(0).feeder(), player.getUUID(), "feeder");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void bucketPostsTransfer(GameTestHelper h) {
        StorageTests.placeBladder(h);
        List<BiomassBucketEvent> seen = new ArrayList<>();
        Consumer<BiomassBucketEvent> listener = seen::add;
        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, BiomassBucketEvent.class, listener);
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, StorageModule.BIOMASS_BUCKET.get(), 1);
        EcologyTests.use(h, player, SAC, Direction.UP);
        NeoForge.EVENT_BUS.unregister(listener);
        h.assertValueEqual(seen.size(), 1, "one bucket event");
        h.assertValueEqual(seen.get(0).block(), id("biomass_bladder"), "block");
        h.assertTrue(seen.get(0).inserted(), "inserted");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void creditsNeverEvictAnotherPlayer(GameTestHelper h) {
        PendingCredits credits = PendingCredits.get(h.getLevel());
        List<UUID> players = new ArrayList<>();
        for (int n = 0; n < 65; n++) {
            UUID player = UUID.randomUUID();
            players.add(player);
            credits.remember(player, id("digestive_sac"));
        }
        for (UUID player : players) h.assertTrue(credits.holds(player, id("digestive_sac")), "credit kept: " + player);
        for (UUID player : players)
            credits.award(new ServerPlayer(
                    h.getLevel().getServer(),
                    h.getLevel(),
                    new GameProfile(player, "credited"),
                    ClientInformation.createDefault()));
        for (UUID player : players) h.assertFalse(credits.holds(player, id("digestive_sac")), "credit spent");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aFullCreditListSavesAndLoadsIntact(GameTestHelper h) {
        var registries = h.getLevel().registryAccess();
        PendingCredits fresh = PendingCredits.load(new CompoundTag(), registries);
        UUID last = null;
        for (int n = 0; n < PendingCredits.LIMIT; n++) {
            last = UUID.randomUUID();
            fresh.remember(last, id("digestive_sac"));
        }
        UUID refused = UUID.randomUUID();
        fresh.remember(refused, id("digestive_sac"));
        h.assertValueEqual(fresh.size(), PendingCredits.LIMIT, "the writer stops at the limit");
        h.assertFalse(fresh.holds(refused, id("digestive_sac")), "the credit past the limit is refused, not evicting");
        PendingCredits reloaded = PendingCredits.load(fresh.save(new CompoundTag(), registries), registries);
        h.assertValueEqual(reloaded.size(), PendingCredits.LIMIT, "a full list reads back whole");
        h.assertTrue(reloaded.holds(last, id("digestive_sac")), "the last credit survives");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void unreadableCreditsAreKeptNotDropped(GameTestHelper h) {
        var registries = h.getLevel().registryAccess();
        CompoundTag bad = new CompoundTag();
        bad.putString("player", "not a uuid");
        bad.putString("organ", "infestusfrontier:digestive_sac");
        ListTag list = new ListTag();
        list.add(bad);
        CompoundTag tag = new CompoundTag();
        tag.put("credits", list);
        PendingCredits loaded = PendingCredits.load(tag, registries);
        h.assertValueEqual(loaded.size(), 0, "nothing readable");
        UUID player = UUID.randomUUID();
        loaded.remember(player, id("biomass_bladder"));
        CompoundTag saved = loaded.save(new CompoundTag(), registries);
        h.assertTrue(saved.getList("unreadable_credits", Tag.TAG_COMPOUND).equals(list), "the bad list is kept");
        h.assertValueEqual(saved.getList("credits", Tag.TAG_COMPOUND).size(), 1, "new credits still save");
        h.assertTrue(PendingCredits.load(saved, registries).holds(player, id("biomass_bladder")), "and reload");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void offlineCreditIsAwardedOnLogin(GameTestHelper h) {
        UUID uuid = UUID.randomUUID();
        PendingCredits credits = PendingCredits.get(h.getLevel());
        credits.remember(uuid, id("digestive_sac"));
        h.assertTrue(credits.holds(uuid, id("digestive_sac")), "credit remembered");
        ServerPlayer player = new ServerPlayer(
                h.getLevel().getServer(),
                h.getLevel(),
                new GameProfile(uuid, "credit-tester"),
                ClientInformation.createDefault());
        credits.award(player);
        AdvancementHolder holder = h.getLevel().getServer().getAdvancements().get(id("contact/digestive_sac"));
        h.assertTrue(holder != null, "advancement loaded");
        h.assertTrue(player.getAdvancements().getOrStartProgress(holder).isDone(), "digestive_sac advancement is done");
        h.assertTrue(!credits.holds(uuid, id("digestive_sac")), "credit forgotten");
        h.succeed();
    }
}
