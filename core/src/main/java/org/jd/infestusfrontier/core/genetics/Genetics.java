package org.jd.infestusfrontier.core.genetics;

import java.util.List;
import org.jd.infestusfrontier.core.foundation.Refusal;

/**
 * The fixed limits of DNA research and the reasons it stops. A Sampling Needle takes a DNA sample from a living
 * creature; a DNA Bank extracts its samples one at a time and keeps each species' genome research. Every number is a
 * starting value, not measured balance.
 */
public final class Genetics {
    /** Research points that make a genome complete. A sample adds the points its recipe names. */
    public static final int COMPLETE = 100;
    /** Species one DNA Bank researches; a sample of another species waits in the bank once it holds this many. */
    public static final int MAX_GENOMES = 4;
    /** Samples the bank's sample slot holds. */
    public static final int MAX_SAMPLES = 16;
    /** Biomass the bank holds, in BU: two buckets. */
    public static final int BIOMASS_CAPACITY_BU = 2000;
    /** The longest extraction a recipe may name, in loaded ticks: five minutes. */
    public static final int MAX_EXTRACTION_TICKS = 6000;
    /** Loaded ticks a stopped bank waits before it looks again. */
    public static final int RETRY_TICKS = 20;
    /** Samples one Sampling Needle takes before it breaks. */
    public static final int NEEDLE_USES = 64;
    /** Ticks a player waits between two samples with a needle. */
    public static final int SAMPLE_COOLDOWN_TICKS = 20;

    /** The bank does not stand on a loaded organ foundation. */
    public static final Refusal UNROOTED = new Refusal("dna_bank.unrooted");
    /** The sample's genome is already complete in this bank; the sample stays. */
    public static final Refusal GENOME_COMPLETE = new Refusal("dna_bank.genome_complete");
    /** The bank already researches {@value #MAX_GENOMES} other species; the sample stays. */
    public static final Refusal RESEARCH_FULL = new Refusal("dna_bank.research_full");
    /** No recipe reads the item in the sample slot, for instance after a data pack removed it; it stays. */
    public static final Refusal UNKNOWN_SAMPLE = new Refusal("dna_bank.unknown_sample");
    /** The bank holds less biomass than the next extraction costs. */
    public static final Refusal NO_BIOMASS = new Refusal("dna_bank.no_biomass");
    /** The sample slot cannot take any of the samples in the player's hand. */
    public static final Refusal SAMPLES_FULL = new Refusal("dna_bank.samples_full");

    /** No recipe takes DNA from this creature. */
    public static final Refusal NO_DNA = new Refusal("sampling_needle.no_dna");
    /** The player's inventory has no room for the sample; the needle takes nothing and keeps its wear. */
    public static final Refusal NO_ROOM = new Refusal("sampling_needle.no_room");

    /** Every stop of a bank, in a fixed order: a screen names one by its index. */
    public static final List<Refusal> STOPS =
            List.of(UNROOTED, UNKNOWN_SAMPLE, GENOME_COMPLETE, RESEARCH_FULL, NO_BIOMASS);

    private Genetics() {}
}
