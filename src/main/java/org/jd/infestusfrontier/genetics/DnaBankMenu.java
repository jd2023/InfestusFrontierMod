package org.jd.infestusfrontier.genetics;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.IntSupplier;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jd.infestusfrontier.core.foundation.Outcome;
import org.jd.infestusfrontier.core.foundation.Refusal;
import org.jd.infestusfrontier.core.genetics.Genetics;
import org.jd.infestusfrontier.core.genetics.Genome;
import org.jd.infestusfrontier.foundation.menu.OrganMenu;
import org.jd.infestusfrontier.genetics.api.GeneticConstructRecipe;
import org.jd.infestusfrontier.ui.Sheet;

/**
 * The DNA Bank screen's server half: the sample slot, the three construct material slots and the take-only construct
 * slot, and as synced ints the biomass, the stop, the extraction under way (its species and seconds left), each
 * genome's species and points, and what the materials make now or why they make nothing. The sample slot takes only
 * samples the bank accepts, the material slots only items some construct recipe takes; taking out is always allowed.
 * Button {@value #PRODUCE_BUTTON} asks for one construct; the block entity checks and decides.
 */
public final class DnaBankMenu extends OrganMenu {
    public static final int PRODUCE_BUTTON = 0;
    /** Menu slot of the first construct material; the construct slot follows the materials. */
    public static final int FIRST_MATERIAL_SLOT = 1;

    public static final int CONSTRUCT_SLOT = FIRST_MATERIAL_SLOT + Genetics.MATERIAL_SLOTS;
    static final int STRIPS = 4;
    /** The construct wells sit on the second row, this far apart. */
    static final int WELL_PITCH = Sheet.SLOT.width() + Sheet.GAP;

    private static final int STORED = 0;
    private static final int CAPACITY = 1;
    private static final int STOP = 2;
    private static final int JOB_SPECIES = 3;
    private static final int JOB_SECONDS = 4;
    private static final int GENOMES = 5;
    private static final int CONSTRUCT_STOP = GENOMES + 2 * Genetics.MAX_GENOMES;
    private static final int CONSTRUCT_ITEM = CONSTRUCT_STOP + 1;
    private static final int CONSTRUCT_BIOMASS = CONSTRUCT_ITEM + 1;
    private static final int SYNCED = CONSTRUCT_BIOMASS + 1;
    private static final int TICKS_PER_SECOND = 20;

    private final Optional<DnaBankBlockEntity> bank;

    /** Server side. */
    DnaBankMenu(int id, Inventory inventory, DnaBankBlockEntity bank) {
        this(
                id,
                inventory,
                bank.samples(),
                bank.materials(),
                bank.construct(),
                reading(reads(bank)),
                at(bank),
                Optional.of(bank));
    }

    /** Client side. The position arrives as extra data. */
    public DnaBankMenu(int id, Inventory inventory, RegistryFriendlyByteBuf extra) {
        this(
                id,
                inventory,
                new ItemStackHandler(1),
                new ItemStackHandler(Genetics.MATERIAL_SLOTS),
                new ItemStackHandler(1),
                new SimpleContainerData(SYNCED),
                at(inventory, extra),
                Optional.empty());
    }

    private DnaBankMenu(
            int id,
            Inventory inventory,
            IItemHandler samples,
            IItemHandler materials,
            IItemHandler construct,
            ContainerData data,
            ContainerLevelAccess access,
            Optional<DnaBankBlockEntity> bank) {
        super(GeneticsModule.DNA_BANK_MENU.get(), id, access, GeneticsModule.DNA_BANK.get(), data);
        this.bank = bank;
        addSlot(new SlotItemHandler(samples, 0, Sheet.SLOT_X, Sheet.SLOT_Y));
        Level level = inventory.player.level();
        for (int slot = 0; slot < Genetics.MATERIAL_SLOTS; slot++)
            addSlot(new SlotItemHandler(materials, slot, Sheet.SLOT_X + slot * WELL_PITCH, Sheet.ROW2_SLOT_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return GeneticConstructRecipe.usesMaterial(level, stack) && super.mayPlace(stack);
                }
            });
        addSlot(
                new SlotItemHandler(
                        construct, 0, Sheet.SLOT_X + Genetics.MATERIAL_SLOTS * WELL_PITCH, Sheet.ROW2_SLOT_Y) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }
                });
        addPlayer(inventory, STRIPS);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != PRODUCE_BUTTON) return false;
        if (player instanceof ServerPlayer server) bank.ifPresent(b -> b.produce(server));
        return true;
    }

    private static IntSupplier[] reads(DnaBankBlockEntity bank) {
        IntSupplier[] reads = new IntSupplier[SYNCED];
        reads[STORED] = () -> bank.tank().stored().units();
        reads[CAPACITY] = () -> bank.tank().capacity().units();
        reads[STOP] =
                () -> bank.stop().map(stop -> Genetics.STOPS.indexOf(stop) + 1).orElse(0);
        reads[JOB_SPECIES] =
                () -> bank.job().map(job -> Species.synced(job.species())).orElse(0);
        reads[JOB_SECONDS] = () -> bank.job()
                .map(job -> (job.ticksLeft() + TICKS_PER_SECOND - 1) / TICKS_PER_SECOND)
                .orElse(0);
        for (int n = 0; n < Genetics.MAX_GENOMES; n++) {
            int index = n;
            reads[GENOMES + 2 * n] = () -> genome(bank, index)
                    .map(genome -> Species.synced(genome.species()))
                    .orElse(0);
            reads[GENOMES + 2 * n + 1] =
                    () -> genome(bank, index).map(Genome::points).orElse(0);
        }
        reads[CONSTRUCT_STOP] = () -> bank.production() instanceof Outcome.Refused<?> refused
                ? Genetics.CONSTRUCT_STOPS.indexOf(refused.refusal()) + 1
                : 0;
        reads[CONSTRUCT_ITEM] = () -> matched(bank)
                .map(match ->
                        BuiltInRegistries.ITEM.getId(match.recipe().result().getItem()) + 1)
                .orElse(0);
        reads[CONSTRUCT_BIOMASS] = () ->
                matched(bank).map(match -> match.recipe().biomass().units()).orElse(0);
        return reads;
    }

    /** The recipe the bank's materials answer, whether or not it can be made now. */
    private static Optional<GeneticConstructRecipe.Match> matched(DnaBankBlockEntity bank) {
        Level level = bank.getLevel();
        if (level == null) return Optional.empty();
        List<ItemStack> slots = new ArrayList<>(Genetics.MATERIAL_SLOTS);
        for (int slot = 0; slot < Genetics.MATERIAL_SLOTS; slot++)
            slots.add(bank.materials().getStackInSlot(slot));
        return GeneticConstructRecipe.matching(level, slots);
    }

    private static Optional<Genome> genome(DnaBankBlockEntity bank, int index) {
        List<Genome> genomes = bank.research().genomes();
        return index < genomes.size() ? Optional.of(genomes.get(index)) : Optional.empty();
    }

    @Override
    public int stored() {
        return synced(STORED);
    }

    @Override
    public int capacity() {
        return Math.max(1, synced(CAPACITY));
    }

    /** Why the bank stopped, as last synced, or empty while it works or waits for samples. */
    public Optional<Refusal> stop() {
        int index = synced(STOP) - 1;
        return index >= 0 && index < Genetics.STOPS.size() ? Optional.of(Genetics.STOPS.get(index)) : Optional.empty();
    }

    /** The translation key of the reason the bank stopped, or empty while it works. */
    public Optional<String> stopKey() {
        return stop().map(stop -> "infestusfrontier.refusal." + stop.key());
    }

    /** The creature whose sample is being extracted, or empty when none is. */
    public Optional<EntityType<?>> extracting() {
        return Species.fromSynced(synced(JOB_SPECIES));
    }

    /** Seconds until the extraction under way is done. */
    public int seconds() {
        return synced(JOB_SECONDS);
    }

    /** How many genomes the bank keeps, as last synced. */
    public int genomes() {
        int count = 0;
        while (count < Genetics.MAX_GENOMES && synced(GENOMES + 2 * count + 1) > 0) count++;
        return count;
    }

    /** The creature of genome {@code index}, or empty when its creature is not registered. */
    public Optional<EntityType<?>> species(int index) {
        return Species.fromSynced(synced(GENOMES + 2 * index));
    }

    /** The points of genome {@code index}; {@value Genetics#COMPLETE} is complete. */
    public int points(int index) {
        return synced(GENOMES + 2 * index + 1);
    }

    /**
     * Why the materials make no construct now, as last synced, or empty when one can be produced. A refusal that is
     * not one of {@link Genetics#CONSTRUCT_STOPS}, such as unreadable saved state, shows as no construct.
     */
    public Optional<Refusal> constructStop() {
        int index = synced(CONSTRUCT_STOP) - 1;
        if (index < 0) return Optional.empty();
        return Optional.of(
                index < Genetics.CONSTRUCT_STOPS.size() ? Genetics.CONSTRUCT_STOPS.get(index) : Genetics.NO_CONSTRUCT);
    }

    /** The construct the materials answer, made now or not, or empty when they answer none. */
    public Optional<Item> constructItem() {
        int id = synced(CONSTRUCT_ITEM) - 1;
        return id < 0 ? Optional.empty() : BuiltInRegistries.ITEM.getHolder(id).map(Holder::value);
    }

    /** The biomass that construct costs, in BU. */
    public int constructBiomass() {
        return synced(CONSTRUCT_BIOMASS);
    }
}
