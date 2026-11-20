package org.jd.infestusfrontier.testmod;

import static org.jd.infestusfrontier.testmod.StarterAdvancementTests.done;
import static org.jd.infestusfrontier.testmod.StarterAdvancementTests.id;
import static org.jd.infestusfrontier.testmod.StarterAdvancementTests.player;

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
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.core.foundation.Refusal;
import org.jd.infestusfrontier.core.genetics.Genetics;
import org.jd.infestusfrontier.core.mining.RootMutation;
import org.jd.infestusfrontier.core.mining.Tunnelling;
import org.jd.infestusfrontier.genetics.DnaBankMenu;
import org.jd.infestusfrontier.genetics.GeneticsModule;
import org.jd.infestusfrontier.genetics.api.GeneticConstructRecipe;
import org.jd.infestusfrontier.interaction.InteractionModule;
import org.jd.infestusfrontier.mining.MiningModule;
import org.jd.infestusfrontier.mining.Stride;
import org.jd.infestusfrontier.mining.TunnelRootBlock;
import org.jd.infestusfrontier.mining.TunnelRootMenu;
import org.jd.infestusfrontier.organ.api.Organs;
import org.jd.infestusfrontier.processing.ProcessingModule;
import org.jd.infestusfrontier.storage.StorageModule;
import org.jd.infestusfrontier.storage.api.BiomassCapabilities;

/**
 * GENE-2: a DNA Bank with the complete pig genome turns supplied Overworld materials and biomass into a Stairs or Wider
 * Section Construct, as the construct recipes say, and keeps the genome. A Stairs Construct applied to a Tunnel Root
 * head makes it a Descending Rootstock, and a Wider Section Construct applied to a Rootstock widens it: each takes its
 * construct and biomass once, keeps the head's contents and history, and is permanent. Incomplete research,
 * incompatible targets and repeat installations refuse and take nothing. Constructs and mutations survive moving,
 * breaking, a save to disk and a reload. Recipes, guide, advancements and the demo exhibit. Each yard is a chunk of
 * its own, far from the test structures.
 */
@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class GeneticConstructTests {
    private static final int TOP = 100;
    private static final ResourceLocation PIG = ResourceLocation.withDefaultNamespace("pig");
    /** The shipped Stairs recipe: two porkchops, two Bone Plates and four cobblestone stairs for 250 BU. */
    private static final int STAIRS_BIOMASS = 250;

    private static final int WIDER_BIOMASS = 500;
    private static final int STAIRS_COST = RootMutation.STAIRS.biomassUnits();
    private static final int WIDER_COST = RootMutation.WIDER_SECTION.biomassUnits();
    /** Ticks a due step may take: one try, a stop, and one more look after the retry delay, with a margin. */
    private static final int STEP_WAIT = 4 * Tunnelling.RETRY_TICKS;

    private static final BlockPos DEMO_MIN = new BlockPos(148, -62, -24);
    private static final BlockPos DEMO_MAX = new BlockPos(162, -56, -18);

    /**
     * A yard: rock from the floor layer to the layer over a tunnel, a DNA Bank on substrate at {@code bank()} and a
     * Tunnel Root head on substrate at {@code start}, facing north, in the middle of a chunk the yard forces.
     */
    private record Site(ServerLevel level, BlockPos start, ChunkPos chunk) {
        private static final int REACH = 6;

        static Site of(GameTestHelper h, int slot) {
            BlockPos origin = h.absolutePos(BlockPos.ZERO);
            BlockPos start =
                    new BlockPos((origin.getX() & ~15) + 320008, TOP, (origin.getZ() & ~15) + 320008 + slot * 64);
            Site site = new Site(h.getLevel(), start, new ChunkPos(start));
            TestChunks.force(site.level, site.chunk);
            site.clear();
            for (BlockPos cell : BlockPos.betweenClosed(
                    start.offset(-REACH + 1, -1, -REACH + 1), start.offset(REACH - 1, 2, REACH - 1)))
                site.level.setBlock(cell, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
            for (BlockPos organ : List.of(start, site.bankAt())) {
                site.level.setBlockAndUpdate(organ.below(), EcologyTests.mature());
                site.level.setBlock(organ, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                site.level.setBlock(organ.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
            site.level.setBlockAndUpdate(
                    start,
                    MiningModule.TUNNEL_ROOT
                            .get()
                            .defaultBlockState()
                            .setValue(TunnelRootBlock.FACING, Direction.NORTH));
            site.level.setBlockAndUpdate(
                    site.bankAt(), GeneticsModule.DNA_BANK.get().defaultBlockState());
            return site;
        }

        void clear() {
            for (BlockPos cell :
                    BlockPos.betweenClosed(start.offset(-REACH, -2, -REACH), start.offset(REACH, 4, REACH))) {
                level.removeBlockEntity(cell);
                level.setBlock(cell, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
            for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, yard())) item.discard();
        }

        void release() {
            clear();
            level.setChunkForced(chunk.x, chunk.z, false);
        }

        AABB yard() {
            return AABB.encapsulatingFullBlocks(start.offset(-REACH, -2, -REACH), start.offset(REACH, 4, REACH));
        }

        int loose() {
            return level.getEntitiesOfClass(ItemEntity.class, yard()).size();
        }

        BlockPos bankAt() {
            return start.offset(3, 0, 3);
        }

        /** Where the head is now; it moves. */
        BlockPos head() {
            for (BlockPos cell :
                    BlockPos.betweenClosed(start.offset(-REACH, -1, -REACH), start.offset(REACH, 2, REACH)))
                if (level.getBlockState(cell).is(MiningModule.TUNNEL_ROOT.get())) return cell.immutable();
            throw new IllegalStateException("No head in the yard");
        }

        CompoundTag headSaved() {
            return level.getBlockEntity(head()).saveWithFullMetadata(level.registryAccess());
        }

        CompoundTag bankSaved() {
            return level.getBlockEntity(bankAt()).saveWithFullMetadata(level.registryAccess());
        }

        /** Loads the organ's own saved state at {@code pos} changed by {@code change}, as a load from disk does. */
        void load(BlockPos pos, Consumer<CompoundTag> change) {
            BlockEntity organ = level.getBlockEntity(pos);
            CompoundTag tag = organ.saveWithFullMetadata(level.registryAccess());
            change.accept(tag);
            organ.loadWithComponents(tag, level.registryAccess());
        }

        DnaBankMenu bankMenu(Player player) {
            return (DnaBankMenu)
                    ((MenuProvider) level.getBlockEntity(bankAt())).createMenu(0, player.getInventory(), player);
        }

        TunnelRootMenu headMenu(Player player) {
            return (TunnelRootMenu)
                    ((MenuProvider) level.getBlockEntity(head())).createMenu(0, player.getInventory(), player);
        }

        /** {@code player} right-clicks {@code pos} holding {@code stack}, as the block answers it. */
        void use(Player player, BlockPos pos, ItemStack stack) {
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            level.getBlockState(pos)
                    .useItemOn(
                            stack,
                            level,
                            player,
                            InteractionHand.MAIN_HAND,
                            new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
        }

        void feedHead(int units) {
            load(head(), tag -> tag.getCompound("tank").putInt("stored", units));
        }

        void feedBank(int units) {
            var handler = level.getCapability(BiomassCapabilities.BLOCK, bankAt(), null);
            if (handler == null || !handler.insert(Biomass.of(units), false).isDone())
                throw new IllegalStateException("could not feed the bank");
        }

        /** The bank keeps the pig genome at {@code points}. */
        void pigGenome(int points) {
            load(bankAt(), tag -> {
                ListTag genomes = new ListTag();
                CompoundTag genome = new CompoundTag();
                genome.putString("species", PIG.toString());
                genome.putInt("points", points);
                genomes.add(genome);
                tag.put("genomes", genomes);
            });
        }

        /** The bank's material slots hold {@code stacks}, in order, as saved state. */
        void materials(ItemStack... stacks) {
            load(bankAt(), tag -> {
                ListTag slots = new ListTag();
                for (int slot = 0; slot < Genetics.MATERIAL_SLOTS; slot++)
                    slots.add(
                            slot < stacks.length && !stacks[slot].isEmpty()
                                    ? stacks[slot].save(level.registryAccess())
                                    : new CompoundTag());
                tag.put("materials", slots);
            });
        }
    }

    private static ItemStack stairs() {
        return new ItemStack(MiningModule.STAIRS_CONSTRUCT.get());
    }

    private static ItemStack wider() {
        return new ItemStack(MiningModule.WIDER_SECTION_CONSTRUCT.get());
    }

    private static ItemStack[] stairsMaterials() {
        return new ItemStack[] {
            new ItemStack(Items.COBBLESTONE_STAIRS, 4),
            new ItemStack(Items.PORKCHOP, 2),
            new ItemStack(ProcessingModule.BONE_PLATE.get(), 2)
        };
    }

    private static int stored(CompoundTag tag) {
        return tag.getCompound("tank").getInt("stored");
    }

    private static int pigPoints(CompoundTag tag) {
        for (Tag element : tag.getList("genomes", Tag.TAG_COMPOUND)) {
            CompoundTag genome = (CompoundTag) element;
            if (genome.getString("species").equals(PIG.toString())) return genome.getInt("points");
        }
        return 0;
    }

    /** The material slots as {@code item count} lines, empty slots as "-"; all empty is saved as no list at all. */
    private static List<String> materials(CompoundTag tag) {
        if (!tag.contains("materials")) return List.of("-", "-", "-");
        List<String> slots = new ArrayList<>();
        for (Tag element : tag.getList("materials", Tag.TAG_COMPOUND)) {
            CompoundTag slot = (CompoundTag) element;
            slots.add(slot.contains("id") ? slot.getString("id") + " " + slot.getInt("count") : "-");
        }
        return slots;
    }

    private static String construct(CompoundTag tag) {
        CompoundTag slot = tag.getCompound("construct");
        return slot.contains("id") ? slot.getString("id") + " " + slot.getInt("count") : "-";
    }

    private static List<String> mutations(CompoundTag tag) {
        return tag.getList("mutations", Tag.TAG_STRING).stream()
                .map(Tag::getAsString)
                .toList();
    }

    /** A translatable line as its key and arguments, so the check does not depend on the server's language. */
    private static String spelled(Component line) {
        if (!(line.getContents() instanceof TranslatableContents words)) return line.getString();
        List<String> args = new ArrayList<>();
        for (Object arg : words.getArgs())
            args.add(arg instanceof Component part ? spelled(part) : String.valueOf(arg));
        return args.isEmpty() ? words.getKey() : words.getKey() + "(" + String.join(", ", args) + ")";
    }

    private static int count(Player player, Item item) {
        return player.getInventory().countItem(item);
    }

    // ---- production ----

    /**
     * The steward puts the Stairs materials into the bank from the inventory, in any order, and presses Produce: one
     * Stairs Construct in the construct slot, each material and 250 BU used once, the pig genome still complete. A
     * porkchop too many stays. The same genome makes a second construct from new materials, and the milestones follow.
     */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aCompletePigGenomeProducesStairsConstructsAndStaysComplete(GameTestHelper h) {
        Site site = Site.of(h, 0);
        ServerPlayer player = player(h);
        site.pigGenome(Genetics.COMPLETE);
        site.feedBank(2 * Biomass.BUCKET.units());
        DnaBankMenu menu = site.bankMenu(player);
        int inventory = DnaBankMenu.CONSTRUCT_SLOT + 1;
        List<ItemStack> supplied = List.of(
                new ItemStack(Items.COBBLESTONE_STAIRS, 4),
                new ItemStack(Items.PORKCHOP, 3),
                new ItemStack(ProcessingModule.BONE_PLATE.get(), 2));
        for (int n = 0; n < supplied.size(); n++) {
            menu.getSlot(inventory + n).set(supplied.get(n).copy());
            menu.quickMoveStack(player, inventory + n);
        }
        h.assertValueEqual(
                materials(site.bankSaved()),
                List.of("minecraft:cobblestone_stairs 4", "minecraft:porkchop 3", "infestusfrontier:bone_plate 2"),
                "shift-clicked into the material slots, in the order given");
        h.assertValueEqual(menu.constructStop(), Optional.empty(), "the materials make a construct");
        h.assertValueEqual(
                menu.constructItem(), Optional.of(MiningModule.STAIRS_CONSTRUCT.get()), "a Stairs Construct");
        h.assertValueEqual(menu.constructBiomass(), STAIRS_BIOMASS, "for 250 BU");

        h.assertTrue(menu.clickMenuButton(player, DnaBankMenu.PRODUCE_BUTTON), "the screen asks");
        CompoundTag after = site.bankSaved();
        h.assertValueEqual(construct(after), "infestusfrontier:stairs_construct 1", "one construct");
        h.assertValueEqual(materials(after), List.of("-", "minecraft:porkchop 1", "-"), "each material once");
        h.assertValueEqual(stored(after), 2000 - STAIRS_BIOMASS, "and its biomass once");
        h.assertValueEqual(pigPoints(after), Genetics.COMPLETE, "the genome stays complete");
        h.assertValueEqual(menu.constructStop(), Optional.of(Genetics.NO_CONSTRUCT), "a lone porkchop makes none");
        h.assertTrue(menu.clickMenuButton(player, DnaBankMenu.PRODUCE_BUTTON), "asked again");
        h.assertValueEqual(site.bankSaved(), after, "a refused press changes nothing");
        h.assertTrue(done(h, player, "contact/genetic_construct"), "Written in Flesh is earned");

        site.materials(stairsMaterials());
        menu.clickMenuButton(player, DnaBankMenu.PRODUCE_BUTTON);
        CompoundTag second = site.bankSaved();
        h.assertValueEqual(construct(second), "infestusfrontier:stairs_construct 2", "the genome makes another");
        h.assertValueEqual(stored(second), 2000 - 2 * STAIRS_BIOMASS, "paid again, once");
        h.assertValueEqual(pigPoints(second), Genetics.COMPLETE, "and is still complete");

        ItemStack taken = menu.quickMoveStack(player, DnaBankMenu.CONSTRUCT_SLOT);
        h.assertValueEqual(taken.getCount(), 2, "both taken out");
        h.assertValueEqual(count(player, MiningModule.STAIRS_CONSTRUCT.get()), 2, "into the inventory");
        CriteriaTriggers.INVENTORY_CHANGED.trigger(player, player.getInventory(), taken);
        h.assertTrue(done(h, player, "acquired/stairs_construct"), "the guide's flag");
        h.assertValueEqual(construct(site.bankSaved()), "-", "the slot is empty");
        h.assertValueEqual(site.loose(), 0, "nothing fell loose");
        site.release();
        h.succeed();
    }

    /**
     * Nothing is used when the construct cannot be made: an incomplete pig genome, a stray item among the materials,
     * too little biomass, a construct slot holding another construct, or a bank off its substrate. The screen and the
     * press both name the reason. The material slots take only construct materials, by hand and by shift-click.
     */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void productionRefusesAndTakesNothing(GameTestHelper h) {
        Site site = Site.of(h, 1);
        ServerPlayer player = player(h);
        site.feedBank(Biomass.BUCKET.units());
        site.pigGenome(Genetics.COMPLETE - 10);
        site.materials(stairsMaterials());
        DnaBankMenu menu = site.bankMenu(player);
        List<Refusal> seen = new ArrayList<>();
        Runnable press = () -> {
            CompoundTag before = site.bankSaved();
            seen.add(menu.constructStop().orElseThrow());
            menu.clickMenuButton(player, DnaBankMenu.PRODUCE_BUTTON);
            h.assertValueEqual(site.bankSaved(), before, "refused, it changes nothing: " + seen);
        };
        press.run();

        site.pigGenome(Genetics.COMPLETE);
        site.materials(
                new ItemStack(Items.COBBLESTONE_STAIRS, 4),
                new ItemStack(Items.PORKCHOP, 2),
                new ItemStack(Items.STICK));
        press.run();

        site.materials(stairsMaterials());
        site.load(site.bankAt(), tag -> tag.getCompound("tank").putInt("stored", STAIRS_BIOMASS - 1));
        press.run();

        site.feedBank(1);
        site.load(site.bankAt(), tag -> tag.put("construct", wider().save(site.level.registryAccess())));
        press.run();

        site.load(site.bankAt(), tag -> tag.put("construct", new CompoundTag()));
        site.level.setBlock(site.bankAt().below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
        press.run();
        h.assertValueEqual(
                seen,
                List.of(
                        Genetics.GENOME_INCOMPLETE,
                        Genetics.NO_CONSTRUCT,
                        Genetics.NO_BIOMASS,
                        Genetics.CONSTRUCT_FULL,
                        Genetics.UNROOTED),
                "each reason, in turn");

        site.level.setBlockAndUpdate(site.bankAt().below(), EcologyTests.mature());
        site.materials();
        int stick = DnaBankMenu.CONSTRUCT_SLOT + 1;
        menu.getSlot(stick).set(new ItemStack(Items.STICK, 5));
        menu.quickMoveStack(player, stick);
        h.assertValueEqual(materials(site.bankSaved()), List.of("-", "-", "-"), "a stick does not go in");
        h.assertFalse(
                menu.getSlot(DnaBankMenu.FIRST_MATERIAL_SLOT).mayPlace(new ItemStack(Items.STICK)), "not by hand");
        h.assertTrue(
                menu.getSlot(DnaBankMenu.FIRST_MATERIAL_SLOT).mayPlace(new ItemStack(Items.IRON_INGOT)),
                "an ingot a construct uses does");
        h.assertFalse(menu.getSlot(DnaBankMenu.CONSTRUCT_SLOT).mayPlace(stairs()), "nothing goes into the output");
        h.assertValueEqual(site.loose(), 0, "nothing fell loose");
        site.release();
        h.succeed();
    }

    // ---- application ----

    /**
     * A head with supplies, drops, cells and a steward. Wider Section is refused on a plain head and Stairs while the
     * tank is short; a bucket later, Stairs makes it a Rootstock for one construct and 500 BU, and its supplies,
     * drops, cells, steering and steward stay. Stairs again is refused. Wider Section then waits for biomass, and
     * widens the Rootstock for one construct and 1000 BU. Every refusal keeps the construct and the biomass. A
     * construct on stone takes nothing. The mutations add no segment, tread, jaw or grower, and the milestones follow.
     */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void stairsMakesARootstockAndWiderSectionWidensIt(GameTestHelper h) {
        Site site = Site.of(h, 2);
        ServerPlayer player = player(h);
        Organs.placedBy(site.level, site.start, player);
        site.load(site.start, tag -> {
            tag.put("segments", new ItemStack(MiningModule.ROOT_SEGMENT.get(), 5).save(site.level.registryAccess()));
            ListTag output = tag.getList("output", Tag.TAG_COMPOUND);
            output.set(0, new ItemStack(Items.COBBLESTONE, 7).save(site.level.registryAccess()));
            tag.putInt("cells", 9);
            tag.getCompound("tank").putInt("stored", STAIRS_COST - 100);
        });
        site.level.setBlockAndUpdate(
                site.start,
                site.level.getBlockState(site.start).setValue(TunnelRootBlock.STEER, Stride.LEFT)); // steered left
        CompoundTag before = site.headSaved();
        Consumer<ItemStack> refused = construct -> {
            CompoundTag held = site.headSaved();
            site.use(player, site.start, construct);
            h.assertValueEqual(player.getMainHandItem().getCount(), 1, "the construct is kept");
            h.assertValueEqual(site.headSaved(), held, "nothing is paid");
        };
        refused.accept(wider()); // a plain head is no Rootstock
        refused.accept(stairs()); // 400 BU is short of 500

        site.feedHead(STAIRS_COST + 400);
        site.use(player, site.start, stairs());
        h.assertTrue(player.getMainHandItem().isEmpty(), "the construct is used");
        CompoundTag rootstock = site.headSaved();
        h.assertValueEqual(mutations(rootstock), List.of("stairs"), "a Rootstock");
        h.assertValueEqual(stored(rootstock), 400, "500 BU, once");
        for (String key : List.of("segments", "output", "cells", "ticks", "steward"))
            h.assertValueEqual(rootstock.get(key), before.get(key), key + " stays as it was");
        h.assertValueEqual(
                site.level.getBlockState(site.start).getValue(TunnelRootBlock.STEER), Stride.LEFT, "steering stays");
        h.assertValueEqual(site.head(), site.start, "it did not move");
        h.assertValueEqual(site.headMenu(player).mutations(), List.of(RootMutation.STAIRS), "the screen names it");
        h.assertTrue(done(h, player, "contact/rootstock"), "The Root Learns to Descend is earned");

        refused.accept(stairs());
        refused.accept(wider()); // 400 BU is short of 1000
        site.feedHead(WIDER_COST + 50);
        site.use(player, site.start, wider());
        h.assertTrue(player.getMainHandItem().isEmpty(), "the construct is used");
        CompoundTag wide = site.headSaved();
        h.assertValueEqual(mutations(wide), List.of("stairs", "wider_section"), "a widened Rootstock");
        h.assertValueEqual(stored(wide), 50, "1000 BU, once");
        for (String key : List.of("segments", "output", "cells", "steward"))
            h.assertValueEqual(wide.get(key), before.get(key), key + " stays as it was");
        h.assertValueEqual(
                site.level.getBlockState(site.start).getValue(TunnelRootBlock.STEER), Stride.LEFT, "steering stays");
        h.assertTrue(done(h, player, "contact/wider_section"), "Room to Breathe is earned");
        refused.accept(wider());
        refused.accept(stairs());

        ItemStack offStone = stairs();
        player.setItemInHand(InteractionHand.MAIN_HAND, offStone);
        BlockPos stone = site.start.offset(-2, 0, 0);
        offStone.useOn(new UseOnContext(
                player,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(stone), Direction.UP, stone, false)));
        h.assertValueEqual(player.getMainHandItem().getCount(), 1, "stone takes nothing");
        h.assertTrue(site.level.getBlockState(stone).is(Blocks.STONE), "and stays stone");

        h.assertValueEqual(
                site.headMenu(player).mutations(),
                List.of(RootMutation.STAIRS, RootMutation.WIDER_SECTION),
                "the screen names both");
        int organs = 0;
        for (BlockPos cell : BlockPos.betweenClosed(
                site.start.offset(-Site.REACH, -2, -Site.REACH), site.start.offset(Site.REACH, 4, Site.REACH)))
            if (site.level.getBlockState(cell).is(MiningModule.FRACTURE_JAW.get())
                    || site.level.getBlockState(cell).is(MiningModule.STRUCTURE_GROWER.get())
                    || site.level.getBlockState(cell).getBlock() instanceof StairBlock) organs++;
        h.assertValueEqual(organs, 0, "no jaw, grower or tread appeared");
        h.assertValueEqual(site.loose(), 0, "nothing fell loose");
        site.release();
        h.succeed();
    }

    /**
     * A widened Rootstock still cuts by hand the level 1x2 step it always did: the mutations bring no descent, width or
     * stock of their own. It moves with its mutations; broken with the Surgical Blade it comes out as one item that
     * carries and names them, and placed again it has them.
     */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = STEP_WAIT)
    public static void theMutationsMoveWithTheHeadAndAddNoStep(GameTestHelper h) {
        Site site = Site.of(h, 3);
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
        site.load(site.start, tag -> {
            ListTag installed = new ListTag();
            installed.add(StringTag.valueOf("stairs"));
            installed.add(StringTag.valueOf("wider_section"));
            tag.put("mutations", installed);
            tag.put("segments", new ItemStack(MiningModule.ROOT_SEGMENT.get(), 3).save(site.level.registryAccess()));
            tag.getCompound("tank").putInt("stored", Biomass.BUCKET.units());
            tag.putInt("ticks", Tunnelling.STEP_TICKS);
        });
        h.startSequence()
                .thenWaitUntil(() -> h.assertValueEqual(site.head(), site.start.north(), "one step north"))
                .thenExecute(() -> {
                    CompoundTag moved = site.headSaved();
                    h.assertValueEqual(mutations(moved), List.of("stairs", "wider_section"), "it moved with them");
                    h.assertTrue(
                            site.level
                                    .getBlockState(site.start.north().above(2))
                                    .is(Blocks.STONE),
                            "the roof over the step stays: 1x2");
                    for (BlockPos side : List.of(
                            site.start.north().west(), site.start.north().east()))
                        h.assertTrue(site.level.getBlockState(side).is(Blocks.STONE), "and the walls: " + side);
                    h.assertTrue(
                            site.level.getBlockState(site.start.north().below()).is(MiningModule.ROOT_CELL.get()),
                            "a level root cell, no descent");
                    h.assertValueEqual(moved.getCompound("segments").getInt("count"), 2, "one segment, as always");
                    h.assertValueEqual(
                            stored(moved), Biomass.BUCKET.units() - Tunnelling.STEP_BIOMASS_BU, "the same step cost");

                    BlockPos at = site.head();
                    List<ItemStack> drops = Block.getDrops(
                            site.level.getBlockState(at),
                            site.level,
                            at,
                            site.level.getBlockEntity(at),
                            player,
                            new ItemStack(InteractionModule.SURGICAL_BLADE.get()));
                    h.assertValueEqual(drops.size(), 1, "one item, nothing spilled: " + drops);
                    ItemStack carried = drops.get(0);
                    List<Component> tooltip = new ArrayList<>();
                    carried.getItem()
                            .appendHoverText(
                                    carried, Item.TooltipContext.of(site.level), tooltip, TooltipFlag.Default.NORMAL);
                    h.assertValueEqual(
                            tooltip.stream().map(GeneticConstructTests::spelled).toList(),
                            List.of(
                                    "gui.infestusfrontier.tunnel_root.mutation.stairs",
                                    "gui.infestusfrontier.tunnel_root.mutation.wider_section"),
                            "the tooltip names its mutations");
                    site.level.removeBlockEntity(at);
                    site.level.setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                    BlockPos elsewhere = site.start.offset(-3, 0, 0);
                    site.level.setBlockAndUpdate(elsewhere.below(), EcologyTests.mature());
                    site.level.setBlock(elsewhere, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                    player.setItemInHand(InteractionHand.MAIN_HAND, carried);
                    carried.useOn(new UseOnContext(
                            player,
                            InteractionHand.MAIN_HAND,
                            new BlockHitResult(
                                    Vec3.atCenterOf(elsewhere.below()).add(0, 0.5, 0),
                                    Direction.UP,
                                    elsewhere.below(),
                                    false)));
                    h.assertTrue(
                            site.level.getBlockState(elsewhere).is(MiningModule.TUNNEL_ROOT.get()), "planted again");
                    CompoundTag again = site.headSaved();
                    for (String key : List.of("mutations", "segments", "tank", "cells"))
                        h.assertValueEqual(again.get(key), moved.get(key), key + " travels on the item");
                    h.assertValueEqual(site.loose(), 0, "nothing spilled");
                    site.release();
                })
                .thenSucceed();
    }

    /**
     * The yard's chunk is written to disk, unloaded and read back with a widened Rootstock in it and a bank holding a
     * construct, its materials and the complete genome: new block entities with the same mutations, constructs,
     * materials and research. The bank then produces from the same materials, and the Rootstock still refuses a
     * second Stairs.
     */
    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            template = "empty",
            batch = "construct_unload",
            timeoutTicks = 1200)
    public static void constructsAndMutationsSurviveASaveToDiskAndAReload(GameTestHelper h) {
        Site site = Site.of(h, 4);
        ServerPlayer player = player(h);
        site.pigGenome(Genetics.COMPLETE);
        site.feedBank(Biomass.BUCKET.units());
        site.materials(stairsMaterials());
        site.load(site.bankAt(), tag -> tag.put("construct", stairs().save(site.level.registryAccess())));
        site.feedHead(Biomass.BUCKET.units());
        site.use(player, site.start, stairs());
        site.feedHead(Biomass.BUCKET.units());
        site.use(player, site.start, wider());
        CompoundTag[] saved = new CompoundTag[2];
        BlockEntity[] entities = new BlockEntity[2];
        h.startSequence()
                .thenExecute(() -> {
                    saved[0] = site.headSaved();
                    saved[1] = site.bankSaved();
                    h.assertValueEqual(mutations(saved[0]), List.of("stairs", "wider_section"), "both applied");
                    entities[0] = site.level.getBlockEntity(site.start);
                    entities[1] = site.level.getBlockEntity(site.bankAt());
                    site.level.save(null, true, false);
                    site.level.setChunkForced(site.chunk.x, site.chunk.z, false);
                })
                .thenWaitUntil(() -> h.assertFalse(site.level.hasChunkAt(site.start), "the chunk is gone"))
                .thenExecute(() -> site.level.save(null, true, false))
                .thenIdle(2 * Tunnelling.RETRY_TICKS)
                .thenExecute(() -> TestChunks.force(site.level, site.chunk))
                .thenWaitUntil(() -> h.assertTrue(
                        site.level.hasChunkAt(site.start)
                                && site.level.getBlockEntity(site.start) != null
                                && site.level.getBlockEntity(site.bankAt()) != null,
                        "the chunk, the head and the bank are back"))
                .thenExecute(() -> {
                    h.assertTrue(
                            site.level.getBlockEntity(site.start) != entities[0]
                                    && site.level.getBlockEntity(site.bankAt()) != entities[1],
                            "new block entities, read from the saved chunk");
                    CompoundTag head = site.headSaved();
                    for (String key : List.of("mutations", "segments", "output", "tank", "cells", "steward"))
                        h.assertValueEqual(head.get(key), saved[0].get(key), "the head keeps " + key);
                    CompoundTag bank = site.bankSaved();
                    for (String key : List.of("genomes", "materials", "construct", "tank"))
                        h.assertValueEqual(bank.get(key), saved[1].get(key), "the bank keeps " + key);
                    h.assertValueEqual(construct(bank), "infestusfrontier:stairs_construct 1", "its construct");
                    site.bankMenu(player).clickMenuButton(player, DnaBankMenu.PRODUCE_BUTTON);
                    h.assertValueEqual(
                            construct(site.bankSaved()), "infestusfrontier:stairs_construct 2", "and produces on");
                    site.use(player, site.start, stairs());
                    h.assertValueEqual(player.getMainHandItem().getCount(), 1, "a second Stairs is still refused");
                    h.assertValueEqual(site.headSaved().get("tank"), saved[0].get("tank"), "for nothing");
                    site.release();
                })
                .thenSucceed();
    }

    /**
     * A bank holding the complete genome, construct materials and a construct comes out of a Surgical Blade break as
     * one item carrying all of it. Placed on substrate elsewhere it has them again and produces from them.
     */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aBrokenBankCarriesItsConstructsAndMaterials(GameTestHelper h) {
        Site site = Site.of(h, 6);
        ServerPlayer player = player(h);
        site.pigGenome(Genetics.COMPLETE);
        site.feedBank(Biomass.BUCKET.units());
        site.materials(stairsMaterials());
        site.load(site.bankAt(), tag -> tag.put("construct", wider().save(site.level.registryAccess())));
        CompoundTag held = site.bankSaved();
        BlockPos at = site.bankAt();
        List<ItemStack> drops = Block.getDrops(
                site.level.getBlockState(at),
                site.level,
                at,
                site.level.getBlockEntity(at),
                player,
                new ItemStack(InteractionModule.SURGICAL_BLADE.get()));
        h.assertValueEqual(drops.size(), 1, "one item, nothing spilled: " + drops);
        ItemStack carried = drops.get(0);
        h.assertTrue(carried.is(GeneticsModule.DNA_BANK_ITEM.get()), "the bank itself");
        site.level.removeBlockEntity(at);
        site.level.setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        BlockPos elsewhere = site.start.offset(-3, 0, 3);
        site.level.setBlockAndUpdate(elsewhere.below(), EcologyTests.mature());
        site.level.setBlock(elsewhere, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        player.setItemInHand(InteractionHand.MAIN_HAND, carried);
        carried.useOn(new UseOnContext(
                player,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(
                        Vec3.atCenterOf(elsewhere.below()).add(0, 0.5, 0), Direction.UP, elsewhere.below(), false)));
        h.assertTrue(site.level.getBlockState(elsewhere).is(GeneticsModule.DNA_BANK.get()), "placed again");
        BlockEntity placed = site.level.getBlockEntity(elsewhere);
        CompoundTag again = placed.saveWithFullMetadata(site.level.registryAccess());
        for (String key : List.of("genomes", "materials", "construct", "tank"))
            h.assertValueEqual(again.get(key), held.get(key), key + " travels on the item");
        DnaBankMenu menu = (DnaBankMenu) ((MenuProvider) placed).createMenu(0, player.getInventory(), player);
        h.assertValueEqual(
                menu.constructStop(), Optional.of(Genetics.CONSTRUCT_FULL), "its Wider Section blocks a Stairs");
        menu.quickMoveStack(player, DnaBankMenu.CONSTRUCT_SLOT);
        menu.clickMenuButton(player, DnaBankMenu.PRODUCE_BUTTON);
        h.assertValueEqual(
                construct(placed.saveWithFullMetadata(site.level.registryAccess())),
                "infestusfrontier:stairs_construct 1",
                "emptied, it produces from the carried materials");
        h.assertValueEqual(site.loose(), 0, "nothing spilled");
        site.release();
        h.succeed();
    }

    /**
     * Saved mutations that cannot be, Wider Section without Stairs or a name no mutation has, are kept byte for byte;
     * the head then refuses every construct and takes nothing.
     */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void impossibleSavedMutationsAreKeptAndRefuseEveryConstruct(GameTestHelper h) {
        Site site = Site.of(h, 5);
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
        for (String wrong : List.of("wider_section", "ore_sense")) {
            site.load(site.start, tag -> {
                ListTag installed = new ListTag();
                installed.add(StringTag.valueOf(wrong));
                tag.put("mutations", installed);
                tag.getCompound("tank").putInt("stored", Biomass.BUCKET.units());
            });
            site.use(player, site.start, stairs());
            h.assertValueEqual(player.getMainHandItem().getCount(), 1, wrong + ": the construct is kept");
            CompoundTag kept = site.headSaved();
            h.assertValueEqual(mutations(kept), List.of(wrong), wrong + ": its bytes stay");
            h.assertValueEqual(stored(kept), Biomass.BUCKET.units(), wrong + ": nothing paid");
        }
        site.release();
        h.succeed();
    }

    // ---- discovery ----

    /**
     * The Stairs and Wider Section recipes are data: each needs only the complete pig genome and Overworld materials,
     * with no silverfish. The guide's entry renders both from recipe data, and the flags and milestones exist.
     */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void constructRecipesAreDataAndTheGuideTeachesThem(GameTestHelper h) {
        List<ResourceLocation> recipes = GeneticConstructRecipe.all(h.getLevel()).stream()
                .map(holder -> holder.id())
                .toList();
        h.assertValueEqual(
                recipes, List.of(id("construct/stairs"), id("construct/wider_section")), "two construct recipes");
        Set<Item> overworld = Set.of(
                Items.PORKCHOP,
                Items.COBBLESTONE_STAIRS,
                Items.IRON_INGOT,
                ProcessingModule.BONE_PLATE.get(),
                ProcessingModule.MEMBRANE_SHEET.get());
        for (var holder : GeneticConstructRecipe.all(h.getLevel())) {
            GeneticConstructRecipe recipe = holder.value();
            h.assertValueEqual(recipe.genomes(), List.of(PIG), holder.id() + " needs the pig genome only");
            h.assertFalse(recipe.creatures().contains(EntityType.SILVERFISH), "no silverfish genome");
            for (SizedIngredient material : recipe.materials())
                for (ItemStack option : material.getItems()) {
                    String name =
                            BuiltInRegistries.ITEM.getKey(option.getItem()).toString();
                    h.assertTrue(overworld.contains(option.getItem()), holder.id() + " uses Overworld " + name);
                    h.assertFalse(name.contains("silverfish") || name.contains("infested"), "no silverfish: " + name);
                }
        }
        GeneticConstructRecipe stairs =
                GeneticConstructRecipe.all(h.getLevel()).get(0).value();
        h.assertTrue(stairs.result().is(MiningModule.STAIRS_CONSTRUCT.get()), "the first is the Stairs Construct");
        h.assertValueEqual(stairs.biomass(), Biomass.of(STAIRS_BIOMASS), "for 250 BU");
        GeneticConstructRecipe widerRecipe =
                GeneticConstructRecipe.all(h.getLevel()).get(1).value();
        h.assertTrue(widerRecipe.result().is(MiningModule.WIDER_SECTION_CONSTRUCT.get()), "then Wider Section");
        h.assertValueEqual(widerRecipe.biomass(), Biomass.of(WIDER_BIOMASS), "for 500 BU");
        var advancements = h.getLevel().getServer().getAdvancements();
        for (String path : List.of(
                "acquired/stairs_construct",
                "acquired/wider_section_construct",
                "contact/genetic_construct",
                "contact/rootstock",
                "contact/wider_section")) h.assertTrue(advancements.get(id(path)) != null, path + " exists");
        var entry = BookDataManager.get()
                .getBook(id("the_waking_genome"))
                .getCategory(id("contact"))
                .getEntries()
                .get(id("contact/genetic_constructs"));
        h.assertTrue(entry != null, "the guide entry loaded");
        List<ResourceLocation> shown = new ArrayList<>();
        entry.getPages().stream()
                .filter(page -> page instanceof BookRecipePage<?>)
                .map(page -> (BookRecipePage<?>) page)
                .forEach(page -> shown.add(page.getRecipeId1()));
        h.assertValueEqual(shown, recipes, "the guide renders both constructs from recipe data");
        h.succeed();
    }

    // ---- the demo ----

    /**
     * The demo's exhibit: a bank with the complete pig genome and the Stairs materials loaded, a plain head, a
     * Rootstock and a chest of constructs and materials. Produce makes a Stairs Construct; the chest's Stairs makes
     * the plain head a Rootstock and its Wider Section widens the Rootstock. Nothing falls loose.
     */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 200)
    public static void theDemoExhibitProducesAndAppliesConstructs(GameTestHelper h) {
        DemoExhibit yard = new DemoExhibit(DEMO_MIN, DEMO_MAX, 66);
        yard.force(h);
        ServerPlayer player = player(h);
        h.startSequence()
                .thenIdle(DemoExhibit.ENTITIES_LOAD_TICKS)
                .thenExecute(() -> yard.build(h))
                .thenExecute(() -> {
                    ServerLevel level = h.getLevel();
                    BlockPos bank = yard.at(new BlockPos(150, -60, -22));
                    DnaBankMenu menu = (DnaBankMenu)
                            ((MenuProvider) level.getBlockEntity(bank)).createMenu(0, player.getInventory(), player);
                    h.assertValueEqual(menu.constructStop(), Optional.empty(), "the bank is ready");
                    menu.clickMenuButton(player, DnaBankMenu.PRODUCE_BUTTON);
                    h.assertValueEqual(
                            construct(level.getBlockEntity(bank).saveWithFullMetadata(level.registryAccess())),
                            "infestusfrontier:stairs_construct 1",
                            "Produce makes a Stairs Construct");
                    var chest = (ChestBlockEntity) level.getBlockEntity(yard.at(new BlockPos(161, -60, -22)));
                    for (Item supply : List.of(
                            MiningModule.STAIRS_CONSTRUCT.get(),
                            MiningModule.WIDER_SECTION_CONSTRUCT.get(),
                            Items.PORKCHOP,
                            ProcessingModule.BONE_PLATE.get(),
                            Items.COBBLESTONE_STAIRS,
                            ProcessingModule.MEMBRANE_SHEET.get(),
                            Items.IRON_INGOT,
                            StorageModule.BIOMASS_BUCKET.get()))
                        h.assertTrue(chest != null && chest.countItem(supply) > 0, "the chest holds " + supply);
                    for (BlockPos head : List.of(new BlockPos(154, -60, -22), new BlockPos(158, -60, -22))) {
                        BlockPos at = yard.at(head);
                        ItemStack construct = head.getX() == 154 ? stairs() : wider();
                        player.setItemInHand(InteractionHand.MAIN_HAND, construct);
                        level.getBlockState(at)
                                .useItemOn(
                                        construct,
                                        level,
                                        player,
                                        InteractionHand.MAIN_HAND,
                                        new BlockHitResult(Vec3.atCenterOf(at), Direction.UP, at, false));
                        h.assertTrue(player.getMainHandItem().isEmpty(), "applied at " + head);
                    }
                    h.assertValueEqual(
                            mutations(level.getBlockEntity(yard.at(new BlockPos(154, -60, -22)))
                                    .saveWithFullMetadata(level.registryAccess())),
                            List.of("stairs"),
                            "the plain head is a Rootstock");
                    h.assertValueEqual(
                            mutations(level.getBlockEntity(yard.at(new BlockPos(158, -60, -22)))
                                    .saveWithFullMetadata(level.registryAccess())),
                            List.of("stairs", "wider_section"),
                            "the Rootstock is widened");
                    h.assertValueEqual(
                            level.getEntitiesOfClass(ItemEntity.class, new AABB(bank).inflate(12))
                                    .size(),
                            0,
                            "nothing fell loose");
                    yard.tidy(h);
                    yard.forget(h);
                })
                .thenSucceed();
    }
}
