package org.jd.infestusfrontier.genetics;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.core.genetics.Extraction;
import org.jd.infestusfrontier.core.genetics.Genetics;
import org.jd.infestusfrontier.core.genetics.Genome;
import org.jd.infestusfrontier.core.genetics.Research;
import org.jd.infestusfrontier.core.storage.BiomassTank;
import org.jd.infestusfrontier.storage.api.BiomassCodecs;

/**
 * Everything a DNA Bank owns: the samples waiting in its slot, its biomass, its research (the genomes it keeps), the
 * extraction under way, paid for, the materials in its construct slots and the constructs it has made and not yet
 * given out. Saved on the block entity and carried on the item when the bank is broken, so research and constructs
 * belong to the bank wherever it goes. A bank saved before it had construct slots reads with them empty. The stacks
 * are copies.
 */
record DnaBankContents(
        ItemStack samples,
        BiomassTank tank,
        Research research,
        Optional<Extraction> job,
        List<ItemStack> materials,
        ItemStack construct) {
    static final List<ItemStack> NO_MATERIALS = List.of(ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY);

    static final DnaBankContents EMPTY = new DnaBankContents(
            ItemStack.EMPTY,
            BiomassTank.empty(Biomass.of(Genetics.BIOMASS_CAPACITY_BU)),
            Research.NONE,
            Optional.empty(),
            NO_MATERIALS,
            ItemStack.EMPTY);

    DnaBankContents {
        samples = samples.copy();
        List<ItemStack> copies = new ArrayList<>(materials.size());
        for (ItemStack stack : materials) copies.add(stack.copy());
        materials = List.copyOf(copies);
        construct = construct.copy();
    }

    /** A genome as saved: a species id and its points, judged when read. */
    private record RawGenome(ResourceLocation species, int points) {
        static final Codec<RawGenome> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                        ResourceLocation.CODEC.fieldOf("species").forGetter(RawGenome::species),
                        Codec.INT.fieldOf("points").forGetter(RawGenome::points))
                .apply(instance, RawGenome::new));
    }

    /** An extraction as saved, judged when read. */
    private record RawJob(ResourceLocation species, int points, int ticks, int elapsed) {
        static final Codec<RawJob> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                        ResourceLocation.CODEC.fieldOf("species").forGetter(RawJob::species),
                        Codec.INT.fieldOf("points").forGetter(RawJob::points),
                        Codec.INT.fieldOf("ticks").forGetter(RawJob::ticks),
                        Codec.INT.fieldOf("elapsed").forGetter(RawJob::elapsed))
                .apply(instance, RawJob::new));
    }

    /** The saved values before they are judged; impossible ones are a decode error, never an exception. */
    private record Raw(
            ItemStack samples,
            BiomassTank tank,
            List<RawGenome> genomes,
            Optional<RawJob> job,
            List<ItemStack> materials,
            ItemStack construct) {}

    static final MapCodec<DnaBankContents> CODEC = RecordCodecBuilder.<Raw>mapCodec(instance -> instance.group(
                            ItemStack.OPTIONAL_CODEC.fieldOf("samples").forGetter(Raw::samples),
                            BiomassCodecs.TANK.fieldOf("tank").forGetter(Raw::tank),
                            RawGenome.CODEC.listOf().fieldOf("genomes").forGetter(Raw::genomes),
                            RawJob.CODEC.optionalFieldOf("job").forGetter(Raw::job),
                            ItemStack.OPTIONAL_CODEC
                                    .listOf()
                                    .optionalFieldOf("materials", NO_MATERIALS)
                                    .forGetter(Raw::materials),
                            ItemStack.OPTIONAL_CODEC
                                    .optionalFieldOf("construct", ItemStack.EMPTY)
                                    .forGetter(Raw::construct))
                    .apply(instance, Raw::new))
            .flatXmap(DnaBankContents::read, contents -> DataResult.success(contents.raw()));

    private Raw raw() {
        return new Raw(
                samples,
                tank,
                research.genomes().stream()
                        .map(genome -> new RawGenome(ResourceLocation.parse(genome.species()), genome.points()))
                        .toList(),
                job.map(extraction -> new RawJob(
                        ResourceLocation.parse(extraction.species()),
                        extraction.points(),
                        extraction.ticks(),
                        extraction.elapsed())),
                materials,
                construct);
    }

    private static DataResult<DnaBankContents> read(Raw raw) {
        if (raw.genomes().size() > Genetics.MAX_GENOMES)
            return DataResult.error(() -> "genomes " + raw.genomes().size());
        for (RawGenome genome : raw.genomes())
            if (genome.points() < 1 || genome.points() > Genetics.COMPLETE)
                return DataResult.error(() -> "points " + genome);
        List<Genome> genomes = raw.genomes().stream()
                .map(genome -> new Genome(genome.species().toString(), genome.points()))
                .toList();
        Optional<String> wrong = Research.problem(genomes);
        if (wrong.isPresent()) return DataResult.error(wrong::get);
        if (raw.job().isPresent()) {
            RawJob job = raw.job().get();
            if (job.points() < 1
                    || job.points() > Genetics.COMPLETE
                    || job.ticks() < 1
                    || job.ticks() > Genetics.MAX_EXTRACTION_TICKS
                    || job.elapsed() < 0
                    || job.elapsed() > job.ticks()) return DataResult.error(() -> "job " + job);
        }
        DnaBankContents contents = new DnaBankContents(
                raw.samples(),
                raw.tank(),
                new Research(genomes),
                raw.job()
                        .map(job -> new Extraction(job.species().toString(), job.points(), job.ticks(), job.elapsed())),
                raw.materials(),
                raw.construct());
        Optional<String> problem = contents.problem();
        return problem.isPresent() ? DataResult.error(problem::get) : DataResult.success(contents);
    }

    /** Why these contents cannot be a bank's, or empty when they can. */
    Optional<String> problem() {
        if (samples.getCount() > Genetics.MAX_SAMPLES || samples.getCount() > samples.getMaxStackSize())
            return Optional.of("samples " + samples);
        if (tank.capacity().units() != Genetics.BIOMASS_CAPACITY_BU
                || !tank.reserved().isZero()) return Optional.of("tank " + tank);
        if (job.isPresent() && research.refusal(job.get().species()).isPresent())
            return Optional.of("job " + job.get() + " that the research refuses");
        if (materials.size() != Genetics.MATERIAL_SLOTS) return Optional.of("material slots " + materials.size());
        for (ItemStack stack : materials)
            if (stack.getCount() > stack.getMaxStackSize()) return Optional.of("material stack " + stack);
        if (construct.getCount() > construct.getMaxStackSize()) return Optional.of("construct stack " + construct);
        return Optional.empty();
    }
}
