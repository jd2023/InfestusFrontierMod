package org.jd.infestusfrontier.genetics;

import java.util.List;
import java.util.Optional;
import java.util.function.IntSupplier;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jd.infestusfrontier.core.foundation.Refusal;
import org.jd.infestusfrontier.core.genetics.Genetics;
import org.jd.infestusfrontier.core.genetics.Genome;
import org.jd.infestusfrontier.foundation.menu.OrganMenu;
import org.jd.infestusfrontier.ui.Sheet;

/**
 * The DNA Bank screen's server half: the sample slot, and as synced ints the biomass, the stop, the extraction under
 * way (its species and seconds left) and each genome's species and points. The slot takes only samples the
 * bank accepts; taking them out is always allowed.
 */
public final class DnaBankMenu extends OrganMenu {
    static final int STRIPS = 3;

    private static final int STORED = 0;
    private static final int CAPACITY = 1;
    private static final int STOP = 2;
    private static final int JOB_SPECIES = 3;
    private static final int JOB_SECONDS = 4;
    private static final int GENOMES = 5;
    private static final int SYNCED = GENOMES + 2 * Genetics.MAX_GENOMES;
    private static final int TICKS_PER_SECOND = 20;

    /** Server side. */
    DnaBankMenu(int id, Inventory inventory, DnaBankBlockEntity bank) {
        this(id, inventory, bank.samples(), reading(reads(bank)), at(bank));
    }

    /** Client side. The position arrives as extra data. */
    public DnaBankMenu(int id, Inventory inventory, RegistryFriendlyByteBuf extra) {
        this(id, inventory, new ItemStackHandler(1), new SimpleContainerData(SYNCED), at(inventory, extra));
    }

    private DnaBankMenu(
            int id, Inventory inventory, IItemHandler samples, ContainerData data, ContainerLevelAccess access) {
        super(GeneticsModule.DNA_BANK_MENU.get(), id, access, GeneticsModule.DNA_BANK.get(), data);
        addSlot(new SlotItemHandler(samples, 0, Sheet.SLOT_X, Sheet.SLOT_Y));
        addPlayer(inventory, STRIPS);
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
        return reads;
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
}
