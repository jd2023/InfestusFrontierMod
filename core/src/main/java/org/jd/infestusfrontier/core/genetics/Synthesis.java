package org.jd.infestusfrontier.core.genetics;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;

/**
 * How the materials in a DNA Bank's construct slots answer a construct recipe: each of the recipe's materials takes a
 * slot of its own, and every slot that holds something is taken by one of them, so a stray item makes no construct.
 * The slots and materials are at most {@value Genetics#MATERIAL_SLOTS}, so trying every pairing is bounded.
 */
public final class Synthesis {
    private Synthesis() {}

    /**
     * The slot each material takes, by the material's index, or empty when no pairing fits. {@code fits.test(material,
     * slot)} says whether the slot holds enough of that material.
     */
    public static Optional<List<Integer>> assign(
            int materials, List<Boolean> occupied, BiPredicate<Integer, Integer> fits) {
        if (materials < 0 || materials > Genetics.MATERIAL_SLOTS || occupied.size() > Genetics.MATERIAL_SLOTS)
            throw new IllegalArgumentException("materials " + materials + ", slots " + occupied.size());
        int filled = 0;
        for (boolean holds : occupied) if (holds) filled++;
        if (filled != materials) return Optional.empty();
        List<Integer> taken = new ArrayList<>(materials);
        return pair(0, materials, occupied, fits, taken) ? Optional.of(List.copyOf(taken)) : Optional.empty();
    }

    private static boolean pair(
            int material,
            int materials,
            List<Boolean> occupied,
            BiPredicate<Integer, Integer> fits,
            List<Integer> taken) {
        if (material == materials) return true;
        for (int slot = 0; slot < occupied.size(); slot++) {
            if (!occupied.get(slot) || taken.contains(slot) || !fits.test(material, slot)) continue;
            taken.add(slot);
            if (pair(material + 1, materials, occupied, fits, taken)) return true;
            taken.remove(taken.size() - 1);
        }
        return false;
    }
}
