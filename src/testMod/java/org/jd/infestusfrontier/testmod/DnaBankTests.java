package org.jd.infestusfrontier.testmod;

import static org.jd.infestusfrontier.testmod.StarterAdvancementTests.done;
import static org.jd.infestusfrontier.testmod.StarterAdvancementTests.id;
import static org.jd.infestusfrontier.testmod.StarterAdvancementTests.player;

import com.klikli_dev.modonomicon.book.page.BookCraftingRecipePage;
import com.klikli_dev.modonomicon.book.page.BookRecipePage;
import com.klikli_dev.modonomicon.data.BookDataManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.core.foundation.Outcome;
import org.jd.infestusfrontier.core.foundation.Refusal;
import org.jd.infestusfrontier.core.genetics.Genetics;
import org.jd.infestusfrontier.discovery.PendingCredits;
import org.jd.infestusfrontier.genetics.DnaBankMenu;
import org.jd.infestusfrontier.genetics.GeneticsModule;
import org.jd.infestusfrontier.genetics.api.DnaSampleRecipe;
import org.jd.infestusfrontier.interaction.InteractionModule;
import org.jd.infestusfrontier.organ.OrganModule;
import org.jd.infestusfrontier.organ.api.Organs;
import org.jd.infestusfrontier.processing.ProcessingModule;
import org.jd.infestusfrontier.storage.api.BiomassCapabilities;

/**
 * GENE-1: a Sampling Needle takes pig DNA samples from pigs as the pig recipe says, and a DNA Bank extracts them into
 * the pig genome until it is complete. The research belongs to the bank: it survives breaking and placing, a save to
 * disk and a reload, and stays complete. Full research, invalid samples, missing biomass, a lost foundation and an
 * unloaded chunk refuse or pause with the reason and lose or double nothing. Recipes, guide, advancements and the demo
 * exhibit. Each bank stands in a yard of its own, in a chunk of its own, far from the test structures.
 */
@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class DnaBankTests {
    private static final int TOP = 100;
    private static final ResourceLocation PIG = ResourceLocation.withDefaultNamespace("pig");
    /** The pig recipe's numbers, as shipped: ten samples complete the genome. */
    private static final int PIG_POINTS = 10;

    private static final int PIG_BIOMASS = 100;
    private static final int PIG_TICKS = 100;
    private static final int SAMPLES_TO_COMPLETE = Genetics.COMPLETE / PIG_POINTS;
    private static final BlockPos DEMO_MIN = new BlockPos(132, -62, -24);
    private static final BlockPos DEMO_MAX = new BlockPos(146, -56, -18);
    /** The exhibit's bank with the pig genome under way; the one beside it waits for the player's samples. */
    private static final BlockPos DEMO_BANK = new BlockPos(139, -60, -22);

    /** A bank's yard: the bank at {@code start} on Living Substrate, in the middle of a chunk the yard forces. */
    private record Site(ServerLevel level, BlockPos start, ChunkPos chunk) {
        private static final int REACH = 4;

        static Site of(GameTestHelper h, int slot) {
            BlockPos origin = h.absolutePos(BlockPos.ZERO);
            BlockPos start =
                    new BlockPos((origin.getX() & ~15) + 300008, TOP, (origin.getZ() & ~15) + 300008 + slot * 64);
            Site site = new Site(h.getLevel(), start, new ChunkPos(start));
            TestChunks.force(site.level, site.chunk);
            site.clear();
            site.level.setBlockAndUpdate(start.below(), EcologyTests.mature());
            site.level.setBlockAndUpdate(start, GeneticsModule.DNA_BANK.get().defaultBlockState());
            return site;
        }

        void clear() {
            for (BlockPos cell :
                    BlockPos.betweenClosed(start.offset(-REACH, -2, -REACH), start.offset(REACH, 3, REACH))) {
                level.removeBlockEntity(cell);
                level.setBlock(cell, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
            for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, yard())) item.discard();
            for (Mob mob : level.getEntitiesOfClass(Mob.class, yard())) mob.discard();
        }

        void release() {
            clear();
            level.setChunkForced(chunk.x, chunk.z, false);
        }

        AABB yard() {
            return AABB.encapsulatingFullBlocks(start.offset(-REACH, -2, -REACH), start.offset(REACH, 3, REACH));
        }

        int loose() {
            return level.getEntitiesOfClass(ItemEntity.class, yard()).size();
        }

        BlockEntity bank() {
            return level.getBlockEntity(start);
        }

        CompoundTag saved() {
            return bank().saveWithFullMetadata(level.registryAccess());
        }

        /** Loads the bank's own saved state changed by {@code change}, as a load from disk does. */
        void load(Consumer<CompoundTag> change) {
            CompoundTag tag = saved();
            change.accept(tag);
            bank().loadWithComponents(tag, level.registryAccess());
        }

        void feed(int units) {
            var handler = level.getCapability(BiomassCapabilities.BLOCK, start, null);
            if (handler == null || !(handler.insert(Biomass.of(units), false) instanceof Outcome.Done<?>))
                throw new IllegalStateException("could not feed the bank");
        }

        IItemHandler intake() {
            IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, start, Direction.UP);
            if (handler == null) throw new IllegalStateException("the bank has no item handler");
            return handler;
        }

        DnaBankMenu menu(Player player) {
            return (DnaBankMenu) ((MenuProvider) bank()).createMenu(0, player.getInventory(), player);
        }

        /** The player right-clicks the bank's top with what they hold, as the block answers it. */
        void use(Player player) {
            level.getBlockState(start)
                    .useItemOn(
                            player.getMainHandItem(),
                            level,
                            player,
                            InteractionHand.MAIN_HAND,
                            new BlockHitResult(Vec3.atCenterOf(start).add(0, 0.5, 0), Direction.UP, start, false));
        }

        /** A pig with no will of its own beside the bank. */
        Mob pig() {
            Mob pig = EntityType.PIG.create(level);
            if (pig == null) throw new IllegalStateException("no pig");
            pig.setNoAi(true);
            pig.moveTo(Vec3.atBottomCenterOf(start.offset(2, 0, 0)));
            level.addFreshEntity(pig);
            return pig;
        }
    }

    private static ItemStack samples(int count) {
        return new ItemStack(GeneticsModule.PIG_DNA_SAMPLE.get(), count);
    }

    private static String key(Refusal refusal) {
        return "infestusfrontier.refusal." + refusal.key();
    }

    private static int stored(CompoundTag tag) {
        return tag.getCompound("tank").getInt("stored");
    }

    private static int sampleCount(CompoundTag tag) {
        return tag.getCompound("samples").getInt("count");
    }

    private static int pigPoints(CompoundTag tag) {
        for (var element : tag.getList("genomes", 10)) {
            CompoundTag genome = (CompoundTag) element;
            if (genome.getString("species").equals(PIG.toString())) return genome.getInt("points");
        }
        return 0;
    }

    private static CompoundTag genome(String species, int points) {
        CompoundTag genome = new CompoundTag();
        genome.putString("species", species);
        genome.putInt("points", points);
        return genome;
    }

    private static CompoundTag job(int points, int ticks, int elapsed) {
        CompoundTag job = new CompoundTag();
        job.putString("species", PIG.toString());
        job.putInt("points", points);
        job.putInt("ticks", ticks);
        job.putInt("elapsed", elapsed);
        return job;
    }

    /** A translatable line as its key and arguments, so the check does not depend on the server's language. */
    private static String spelled(Component line) {
        if (!(line.getContents() instanceof TranslatableContents words)) return line.getString();
        List<String> args = new ArrayList<>();
        for (Object arg : words.getArgs())
            args.add(arg instanceof Component part ? spelled(part) : String.valueOf(arg));
        return args.isEmpty() ? words.getKey() : words.getKey() + "(" + String.join(", ", args) + ")";
    }

    /** Uses the needle in {@code player}'s main hand on {@code target} as the server does, after its rest is over. */
    private static void sample(Player player, LivingEntity target) {
        for (int tick = 0; tick < Genetics.SAMPLE_COOLDOWN_TICKS; tick++)
            player.getCooldowns().tick();
        player.interactOn(target, InteractionHand.MAIN_HAND);
    }

    // ---- the needle ----

    /**
     * The needle used on a pig puts one Pig DNA Sample in the inventory, as the pig recipe names it, wears by one and
     * rests; the pig is not hurt. Used again at once it does nothing. Used on a cow, which no recipe names, or with no
     * room in the inventory, it takes nothing and keeps its wear.
     */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void theNeedleTakesAPigSampleAsTheRecipeSaysAndRefusesWithoutLoss(GameTestHelper h) {
        Optional<DnaSampleRecipe> recipe = DnaSampleRecipe.forCreature(h.getLevel(), EntityType.PIG);
        h.assertTrue(recipe.isPresent(), "the pig recipe names the pig");
        h.assertTrue(recipe.get().sample().is(GeneticsModule.PIG_DNA_SAMPLE.get()), "and its sample");
        h.assertTrue(DnaSampleRecipe.forCreature(h.getLevel(), EntityType.COW).isEmpty(), "no recipe names a cow");
        Mob pig = h.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(1, 2, 1));
        Mob cow = h.spawnWithNoFreeWill(EntityType.COW, new BlockPos(3, 2, 1));
        ServerPlayer player = player(h);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(GeneticsModule.SAMPLING_NEEDLE.get()));
        ItemStack needle = player.getMainHandItem();
        h.assertValueEqual(needle.getMaxDamage(), Genetics.NEEDLE_USES, "the needle's uses");

        sample(player, pig);
        h.assertValueEqual(player.getInventory().countItem(GeneticsModule.PIG_DNA_SAMPLE.get()), 1, "one sample");
        h.assertValueEqual(needle.getDamageValue(), 1, "worn by one");
        h.assertValueEqual(pig.getHealth(), pig.getMaxHealth(), "the pig is not hurt");
        player.interactOn(pig, InteractionHand.MAIN_HAND);
        h.assertValueEqual(
                player.getInventory().countItem(GeneticsModule.PIG_DNA_SAMPLE.get()), 1, "resting, it takes nothing");

        sample(player, cow);
        h.assertValueEqual(player.getInventory().countItem(GeneticsModule.PIG_DNA_SAMPLE.get()), 1, "a cow: none");
        h.assertValueEqual(needle.getDamageValue(), 1, "and no wear");

        for (int slot = 0; slot < player.getInventory().items.size(); slot++)
            if (slot != player.getInventory().selected)
                player.getInventory().items.set(slot, new ItemStack(Items.DIRT, 64));
        sample(player, pig);
        h.assertValueEqual(player.getInventory().countItem(GeneticsModule.PIG_DNA_SAMPLE.get()), 0, "no room: none");
        h.assertValueEqual(needle.getDamageValue(), 1, "and no wear");
        h.assertValueEqual(player.getInventory().countItem(Items.DIRT), 64 * 35, "nothing displaced");
        h.killAllEntitiesOfClass(Mob.class);
        h.succeed();
    }

    // ---- the route ----

    /**
     * Ten pig samples taken with the needle complete the pig genome in a bank. Loaded by hand, they are extracted one
     * at a time: the screen shows each extraction and the genome rising by ten points each, every sample and its
     * biomass paid once. The completed genome earns the steward the milestone; the bank then takes no more pig samples,
     * by hand or by hopper, and the genome stays complete.
     */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 1600)
    public static void pigSamplesGatheredFromAPigCompleteThePigGenome(GameTestHelper h) {
        Site site = Site.of(h, 0);
        ServerPlayer player = player(h);
        Organs.placedBy(site.level, site.start, player);
        site.feed(2 * Biomass.BUCKET.units());
        Mob pig = site.pig();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(GeneticsModule.SAMPLING_NEEDLE.get()));
        for (int n = 0; n < SAMPLES_TO_COMPLETE; n++) sample(player, pig);
        ItemStack gathered = samples(0);
        for (ItemStack stack : player.getInventory().items)
            if (stack.is(GeneticsModule.PIG_DNA_SAMPLE.get())) gathered = stack;
        h.assertValueEqual(gathered.getCount(), SAMPLES_TO_COMPLETE, "ten samples from the pig");
        CriteriaTriggers.INVENTORY_CHANGED.trigger(player, player.getInventory(), gathered);
        h.assertTrue(done(h, player, "contact/dna_sample"), "the first sample's milestone");
        h.assertFalse(done(h, player, "contact/pig_genome"), "not yet the genome");
        player.setItemInHand(InteractionHand.MAIN_HAND, gathered.copy());
        gathered.setCount(0);
        site.use(player);
        h.assertTrue(player.getMainHandItem().isEmpty(), "the bank took them all");
        DnaBankMenu menu = site.menu(player);
        List<Integer> seen = new ArrayList<>();
        PendingCredits credits = PendingCredits.get(site.level);
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(menu.extracting().isPresent(), "the first extraction starts"))
                .thenExecute(() -> {
                    h.assertValueEqual(menu.extracting().get(), EntityType.PIG, "of a pig sample");
                    h.assertValueEqual(menu.seconds(), PIG_TICKS / 20, "five seconds");
                    h.assertValueEqual(sampleCount(site.saved()), SAMPLES_TO_COMPLETE - 1, "one sample taken");
                    h.assertValueEqual(stored(site.saved()), 2000 - PIG_BIOMASS, "and its biomass");
                })
                .thenWaitUntil(() -> {
                    int points = menu.genomes() > 0 ? menu.points(0) : 0;
                    if (seen.isEmpty() || seen.get(seen.size() - 1) != points) seen.add(points);
                    h.assertValueEqual(points, Genetics.COMPLETE, "the genome completes");
                })
                .thenExecute(() -> {
                    List<Integer> steps = new ArrayList<>();
                    for (int n = 0; n <= SAMPLES_TO_COMPLETE; n++) steps.add(n * PIG_POINTS);
                    h.assertValueEqual(seen, steps, "the screen shows each sample's ten points, once");
                    h.assertValueEqual(menu.species(0), Optional.of(EntityType.PIG), "for the pig");
                    CompoundTag tag = site.saved();
                    h.assertValueEqual(sampleCount(tag), 0, "every sample used once");
                    h.assertValueEqual(stored(tag), 2000 - SAMPLES_TO_COMPLETE * PIG_BIOMASS, "and paid once");
                    h.assertFalse(tag.contains("job"), "nothing under way");
                    h.assertTrue(credits.holdsGenome(player.getUUID(), PIG), "the offline steward's credit waits");
                    credits.award(player);
                    h.assertTrue(done(h, player, "contact/pig_genome"), "the genome's milestone");

                    player.setItemInHand(InteractionHand.MAIN_HAND, samples(3));
                    site.use(player);
                    h.assertValueEqual(player.getMainHandItem().getCount(), 3, "a complete genome takes no more");
                    ItemStack left = site.intake().insertItem(0, samples(2), false);
                    h.assertValueEqual(left.getCount(), 2, "nor through a hopper");
                    h.assertValueEqual(sampleCount(site.saved()), 0, "the slot stays empty");
                    h.assertValueEqual(site.loose(), 0, "nothing fell loose");
                })
                .thenIdle(2 * Genetics.RETRY_TICKS)
                .thenExecute(() -> {
                    h.assertValueEqual(pigPoints(site.saved()), Genetics.COMPLETE, "it stays complete");
                    site.release();
                })
                .thenSucceed();
    }

    // ---- the research belongs to the bank ----

    /**
     * A bank with a complete pig genome, samples, biomass and an extraction under way comes out of a Surgical Blade
     * break as one item carrying all of it, which its tooltip shows. Placed on substrate elsewhere it has the same
     * research and carries on with the same extraction.
     */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void theResearchMovesWithTheBank(GameTestHelper h) {
        Site site = Site.of(h, 1);
        site.feed(500);
        site.load(tag -> {
            ListTag genomes = new ListTag();
            genomes.add(genome(PIG.toString(), 70));
            genomes.add(genome("minecraft:cow", 30));
            tag.put("genomes", genomes);
            tag.put("samples", samples(4).save(site.level.registryAccess()));
            tag.put("job", job(PIG_POINTS, PIG_TICKS, 40));
        });
        CompoundTag held = site.saved();
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
        List<ItemStack> drops = Block.getDrops(
                site.level.getBlockState(site.start),
                site.level,
                site.start,
                site.bank(),
                player,
                new ItemStack(InteractionModule.SURGICAL_BLADE.get()));
        h.assertValueEqual(drops.size(), 1, "one item, nothing spilled: " + drops);
        ItemStack carried = drops.get(0);
        h.assertTrue(carried.is(GeneticsModule.DNA_BANK_ITEM.get()), "the bank itself");
        List<Component> tooltip = new ArrayList<>();
        carried.getItem()
                .appendHoverText(carried, Item.TooltipContext.of(site.level), tooltip, TooltipFlag.Default.NORMAL);
        h.assertValueEqual(
                tooltip.stream().map(DnaBankTests::spelled).toList(),
                List.of(
                        "gui.infestusfrontier.dna_bank.genome(entity.minecraft.pig, 70)",
                        "gui.infestusfrontier.dna_bank.genome(entity.minecraft.cow, 30)"),
                "the tooltip lists its genomes");
        site.level.removeBlockEntity(site.start);
        site.level.setBlock(site.start, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        BlockPos elsewhere = site.start.offset(2, 0, 2);
        site.level.setBlockAndUpdate(elsewhere.below(), EcologyTests.mature());
        player.setItemInHand(InteractionHand.MAIN_HAND, carried);
        carried.useOn(new UseOnContext(
                player,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(
                        Vec3.atCenterOf(elsewhere.below()).add(0, 0.5, 0), Direction.UP, elsewhere.below(), false)));
        h.assertTrue(site.level.getBlockState(elsewhere).is(GeneticsModule.DNA_BANK.get()), "placed again");
        CompoundTag again = site.level.getBlockEntity(elsewhere).saveWithFullMetadata(site.level.registryAccess());
        for (String key : List.of("genomes", "samples", "tank", "job"))
            h.assertValueEqual(again.get(key), held.get(key), key + " travels on the item");
        h.assertValueEqual(site.loose(), 0, "nothing spilled");
        h.startSequence()
                .thenWaitUntil(() -> h.assertValueEqual(
                        pigPoints(
                                site.level.getBlockEntity(elsewhere).saveWithFullMetadata(site.level.registryAccess())),
                        70 + PIG_POINTS,
                        "the carried extraction finishes where it was placed"))
                .thenExecute(site::release)
                .thenSucceed();
    }

    /**
     * The bank's chunk is written to disk, unloaded and read back in the middle of an extraction: a new block entity
     * with the same samples, biomass, research, extraction and steward, no work while it was gone, and the extraction
     * then finishes once: ten points, no sample or biomass paid twice.
     */
    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            template = "empty",
            batch = "dna_bank_unload",
            timeoutTicks = 1200)
    public static void theBankKeepsItsResearchAndExtractionThroughASaveToDiskAndAReload(GameTestHelper h) {
        Site site = Site.of(h, 2);
        ServerPlayer player = player(h);
        Organs.placedBy(site.level, site.start, player);
        site.feed(PIG_BIOMASS);
        site.load(tag -> {
            ListTag genomes = new ListTag();
            genomes.add(genome(PIG.toString(), 40));
            tag.put("genomes", genomes);
            tag.put("samples", samples(1).save(site.level.registryAccess()));
        });
        List<CompoundTag> before = new ArrayList<>();
        List<BlockEntity> entity = new ArrayList<>();
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(
                        site.saved().getCompound("job").getInt("elapsed") >= 20, "an extraction is under way"))
                .thenExecute(() -> {
                    before.add(site.saved());
                    entity.add(site.bank());
                    site.level.save(null, true, false);
                    site.level.setChunkForced(site.chunk.x, site.chunk.z, false);
                })
                .thenWaitUntil(() -> h.assertFalse(site.level.hasChunkAt(site.start), "the chunk is gone"))
                .thenExecute(() -> site.level.save(null, true, false))
                .thenIdle(PIG_TICKS)
                .thenExecute(() -> TestChunks.force(site.level, site.chunk))
                .thenWaitUntil(() -> h.assertTrue(
                        site.level.hasChunkAt(site.start) && site.bank() != null, "the chunk and the bank are back"))
                .thenExecute(() -> {
                    h.assertTrue(site.bank() != entity.get(0), "read from the saved chunk");
                    CompoundTag back = site.saved();
                    for (String key : List.of("genomes", "samples", "tank", "steward"))
                        h.assertValueEqual(back.get(key), before.get(0).get(key), "keeps " + key);
                    int waited = back.getCompound("job").getInt("elapsed")
                            - before.get(0).getCompound("job").getInt("elapsed");
                    h.assertTrue(waited >= 0 && waited <= 5, "no extraction while gone: " + waited);
                    h.assertValueEqual(pigPoints(back), 40, "no points while gone");
                })
                .thenWaitUntil(() -> h.assertFalse(site.saved().contains("job"), "the extraction finishes"))
                .thenExecute(() -> {
                    CompoundTag after = site.saved();
                    h.assertValueEqual(pigPoints(after), 40 + PIG_POINTS, "ten points, once");
                    h.assertValueEqual(sampleCount(after), 0, "the one sample, used once");
                    h.assertValueEqual(stored(after), 0, "its biomass, paid once");
                })
                .thenIdle(2 * Genetics.RETRY_TICKS)
                .thenExecute(() -> {
                    h.assertValueEqual(pigPoints(site.saved()), 40 + PIG_POINTS, "and nothing more");
                    site.release();
                })
                .thenSucceed();
    }

    // ---- refusals ----

    /**
     * A bank that researches four other species refuses a pig sample by hand and by hopper, keeping it; an item no
     * recipe reads is refused by the hopper and, already in the slot, stops the bank and stays to be taken out; too
     * little biomass stops it with the sample kept; a full slot takes nothing more from a hand.
     */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 400)
    public static void fullResearchInvalidSamplesAndMissingSuppliesLoseNothing(GameTestHelper h) {
        Site site = Site.of(h, 3);
        ServerPlayer player = player(h);
        site.feed(1000);
        site.load(tag -> {
            ListTag genomes = new ListTag();
            for (int n = 0; n < Genetics.MAX_GENOMES; n++) genomes.add(genome("minecraft:species" + n, 20));
            tag.put("genomes", genomes);
        });
        player.setItemInHand(InteractionHand.MAIN_HAND, samples(5));
        site.use(player);
        h.assertValueEqual(player.getMainHandItem().getCount(), 5, "a full bank takes no pig sample by hand");
        h.assertValueEqual(site.intake().insertItem(0, samples(5), false).getCount(), 5, "nor by hopper");
        h.assertValueEqual(
                site.intake().insertItem(0, new ItemStack(Items.STICK), false).getCount(), 1, "nor a stick");
        h.assertTrue(site.intake().extractItem(0, 1, false).isEmpty(), "the hopper takes nothing out");
        site.load(tag -> {
            tag.put("genomes", new ListTag());
            tag.put("samples", new ItemStack(Items.STICK, 3).save(site.level.registryAccess()));
        });
        DnaBankMenu menu = site.menu(player);
        h.startSequence()
                .thenWaitUntil(() -> h.assertValueEqual(
                        menu.stopKey(), Optional.of(key(Genetics.UNKNOWN_SAMPLE)), "an unknown sample stops it"))
                .thenExecute(() -> {
                    h.assertValueEqual(sampleCount(site.saved()), 3, "and stays");
                    h.assertValueEqual(stored(site.saved()), 1000, "nothing paid");
                    ItemStack taken = menu.quickMoveStack(player, 0);
                    h.assertValueEqual(taken.getCount(), 3, "the player takes it out");
                    h.assertValueEqual(player.getInventory().countItem(Items.STICK), 3, "into the inventory");
                    site.load(tag -> {
                        tag.getCompound("tank").putInt("stored", PIG_BIOMASS - 1);
                        tag.put("samples", samples(2).save(site.level.registryAccess()));
                    });
                })
                .thenWaitUntil(() -> h.assertValueEqual(
                        menu.stopKey(), Optional.of(key(Genetics.NO_BIOMASS)), "too little biomass stops it"))
                .thenExecute(() -> {
                    h.assertValueEqual(sampleCount(site.saved()), 2, "the samples stay");
                    h.assertValueEqual(stored(site.saved()), PIG_BIOMASS - 1, "and the biomass");
                    site.load(tag ->
                            tag.put("samples", samples(Genetics.MAX_SAMPLES).save(site.level.registryAccess())));
                    player.setItemInHand(InteractionHand.MAIN_HAND, samples(4));
                    site.use(player);
                    h.assertValueEqual(player.getMainHandItem().getCount(), 4, "a full slot takes none");
                    h.assertValueEqual(sampleCount(site.saved()), Genetics.MAX_SAMPLES, "and holds its limit");
                    site.feed(1);
                })
                .thenWaitUntil(() -> h.assertTrue(site.saved().contains("job"), "with biomass it starts"))
                .thenExecute(() -> {
                    h.assertValueEqual(sampleCount(site.saved()), Genetics.MAX_SAMPLES - 1, "one sample");
                    h.assertValueEqual(stored(site.saved()), 0, "and its biomass");
                    h.assertValueEqual(site.loose(), 0, "nothing fell loose");
                    site.release();
                })
                .thenSucceed();
    }

    /**
     * Without its foundation an extraction pauses where it is and names the reason; with the substrate back it goes on
     * from there. No points are added while it waits.
     */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 400)
    public static void anExtractionPausesWithoutItsFoundationAndResumes(GameTestHelper h) {
        Site site = Site.of(h, 4);
        site.load(tag -> tag.put("job", job(PIG_POINTS, PIG_TICKS, 50)));
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
        DnaBankMenu menu = site.menu(player);
        List<Integer> paused = new ArrayList<>();
        h.startSequence()
                .thenExecute(() ->
                        site.level.setBlock(site.start.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS))
                .thenWaitUntil(() -> h.assertValueEqual(
                        menu.stopKey(), Optional.of(key(Genetics.UNROOTED)), "it names the missing substrate"))
                .thenExecute(() -> paused.add(site.saved().getCompound("job").getInt("elapsed")))
                .thenIdle(PIG_TICKS)
                .thenExecute(() -> {
                    h.assertValueEqual(
                            site.saved().getCompound("job").getInt("elapsed"), paused.get(0), "paused where it was");
                    h.assertValueEqual(pigPoints(site.saved()), 0, "nothing added");
                    site.level.setBlockAndUpdate(site.start.below(), EcologyTests.mature());
                })
                .thenWaitUntil(() -> h.assertValueEqual(pigPoints(site.saved()), PIG_POINTS, "it goes on and adds"))
                .thenExecute(site::release)
                .thenSucceed();
    }

    /** Impossible saved research is kept as it was; the bank then extracts nothing and takes no sample. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 200)
    public static void unreadableResearchKeepsItsBytesAndTheBankDoesNothing(GameTestHelper h) {
        Site site = Site.of(h, 5);
        site.feed(1000);
        site.load(tag -> {
            ListTag genomes = new ListTag();
            genomes.add(genome(PIG.toString(), Genetics.COMPLETE + 1));
            tag.put("genomes", genomes);
            tag.put("samples", samples(2).save(site.level.registryAccess()));
        });
        h.startSequence()
                .thenIdle(2 * Genetics.RETRY_TICKS + PIG_TICKS)
                .thenExecute(() -> {
                    CompoundTag tag = site.saved();
                    h.assertValueEqual(pigPoints(tag), Genetics.COMPLETE + 1, "its bytes stay");
                    h.assertValueEqual(sampleCount(tag), 2, "no sample taken");
                    h.assertFalse(tag.contains("job"), "nothing extracted");
                    h.assertValueEqual(
                            site.intake().insertItem(0, samples(1), false).getCount(), 1, "it takes none");
                    site.release();
                })
                .thenSucceed();
    }

    // ---- discovery ----

    /**
     * The needle and the bank are crafted from Overworld materials only: no silverfish, Nether material or mining
     * organ. The pig recipe is data. The guide's entry teaches them and renders the sample and both crafts from recipe
     * data; the flags and the milestones exist.
     */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void recipesMakeTheNeedleAndTheBankAndTheGuideTeachesThem(GameTestHelper h) {
        M1RouteTests.craft(
                h,
                List.of(new ItemStack(Items.IRON_NUGGET), new ItemStack(Items.BONE), new ItemStack(Items.GLASS_BOTTLE)),
                GeneticsModule.SAMPLING_NEEDLE.get());
        M1RouteTests.craft(
                h,
                List.of(
                        new ItemStack(OrganModule.ORGAN_BUD_ITEM.get()),
                        new ItemStack(Items.GLASS_BOTTLE),
                        new ItemStack(Items.GLASS_BOTTLE),
                        new ItemStack(ProcessingModule.MEMBRANE_SHEET.get()),
                        new ItemStack(ProcessingModule.BONE_PLATE.get())),
                GeneticsModule.DNA_BANK_ITEM.get());
        DnaSampleRecipe pig =
                DnaSampleRecipe.forSample(h.getLevel(), samples(1)).orElseThrow();
        h.assertValueEqual(pig.species(), PIG, "the pig sample researches the pig");
        h.assertValueEqual(pig.points(), PIG_POINTS, "ten points a sample");
        h.assertValueEqual(pig.biomass(), Biomass.of(PIG_BIOMASS), "for 100 BU");
        h.assertValueEqual(pig.ticks(), PIG_TICKS, "in five seconds");
        h.assertValueEqual(DnaSampleRecipe.all(h.getLevel()).size(), 1, "pig DNA is the only DNA so far");
        var advancements = h.getLevel().getServer().getAdvancements();
        for (String path : List.of(
                "acquired/sampling_needle",
                "acquired/pig_dna_sample",
                "acquired/dna_bank",
                "recipes/sampling_needle",
                "recipes/dna_bank",
                "contact/dna_sample",
                "contact/pig_genome")) h.assertTrue(advancements.get(id(path)) != null, path + " exists");
        var entry = BookDataManager.get()
                .getBook(id("the_waking_genome"))
                .getCategory(id("contact"))
                .getEntries()
                .get(id("contact/dna_bank"));
        h.assertTrue(entry != null, "the guide entry loaded");
        List<ResourceLocation> shown = new ArrayList<>();
        entry.getPages().stream()
                .filter(page -> page instanceof BookRecipePage<?>)
                .map(page -> (BookRecipePage<?>) page)
                .forEach(page -> {
                    shown.add(page.getRecipeId1());
                    if (page.getRecipeId2() != null) shown.add(page.getRecipeId2());
                });
        h.assertValueEqual(
                shown,
                List.of(id("dna/pig"), id("sampling_needle"), id("dna_bank")),
                "the guide renders the sample and both crafts from recipe data");
        h.assertTrue(
                entry.getPages().stream().anyMatch(page -> page instanceof BookCraftingRecipePage),
                "the crafts as crafting pages");
        Set<Item> overworld = Set.of(
                Items.IRON_NUGGET,
                Items.BONE,
                Items.GLASS_BOTTLE,
                OrganModule.ORGAN_BUD_ITEM.get(),
                ProcessingModule.MEMBRANE_SHEET.get(),
                ProcessingModule.BONE_PLATE.get());
        for (String recipe : List.of("sampling_needle", "dna_bank"))
            for (Ingredient ingredient : h.getLevel()
                    .getRecipeManager()
                    .byKey(id(recipe))
                    .orElseThrow()
                    .value()
                    .getIngredients())
                for (ItemStack option : ingredient.getItems())
                    h.assertTrue(
                            overworld.contains(option.getItem()),
                            recipe + " needs only Overworld starter materials, not "
                                    + BuiltInRegistries.ITEM.getKey(option.getItem()));
        h.succeed();
    }

    // ---- the demo ----

    /**
     * The demo's exhibit: a pig in a pen, a needle in a chest and a supplied DNA Bank on substrate with pig samples
     * loaded and the genome under way. It extracts on its own and the genome completes; nothing falls loose.
     */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 1200)
    public static void theDemoExhibitCompletesThePigGenome(GameTestHelper h) {
        DemoExhibit yard = new DemoExhibit(DEMO_MIN, DEMO_MAX, 65);
        yard.force(h);
        BlockPos bank = yard.at(DEMO_BANK);
        h.startSequence()
                .thenIdle(DemoExhibit.ENTITIES_LOAD_TICKS)
                .thenExecute(() -> yard.build(h))
                .thenExecute(() -> {
                    CompoundTag tag = h.getLevel()
                            .getBlockEntity(bank)
                            .saveWithFullMetadata(h.getLevel().registryAccess());
                    h.assertTrue(pigPoints(tag) > 0 && pigPoints(tag) < Genetics.COMPLETE, "the genome is under way");
                    h.assertTrue(sampleCount(tag) > 0, "with samples loaded");
                })
                .thenWaitUntil(() -> h.assertValueEqual(
                        pigPoints(h.getLevel()
                                .getBlockEntity(bank)
                                .saveWithFullMetadata(h.getLevel().registryAccess())),
                        Genetics.COMPLETE,
                        "it completes the pig genome on its own"))
                .thenExecute(() -> {
                    h.assertValueEqual(
                            h.getLevel()
                                    .getEntitiesOfClass(ItemEntity.class, new AABB(bank).inflate(8))
                                    .size(),
                            0,
                            "nothing fell loose");
                    yard.tidy(h);
                    yard.forget(h);
                })
                .thenSucceed();
    }
}
