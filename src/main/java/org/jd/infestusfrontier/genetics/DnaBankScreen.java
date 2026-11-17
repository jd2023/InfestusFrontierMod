package org.jd.infestusfrontier.genetics;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import org.jd.infestusfrontier.core.foundation.Refusal;
import org.jd.infestusfrontier.core.genetics.Genetics;
import org.jd.infestusfrontier.foundation.screen.OrganScreen;
import org.jd.infestusfrontier.foundation.screen.Tally;
import org.jd.infestusfrontier.foundation.screen.Well;
import org.jd.infestusfrontier.ui.PartRow;
import org.jd.infestusfrontier.ui.Sheet;

/**
 * The bank's screen: the sample well on the parts row, a line for what the bank is doing (or why it stopped), and one
 * line for each genome it keeps with its progress. The tank shows its biomass.
 */
final class DnaBankScreen extends OrganScreen<DnaBankMenu> {
    private static final String KEY = "gui.infestusfrontier.dna_bank.";
    private static final int STATUS_Y = Sheet.PARTS_Y + Sheet.SLOT.height() + 4;
    private static final int GENOMES_Y = STATUS_Y + 18;
    private static final int LINE = 11;

    DnaBankScreen(DnaBankMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, DnaBankMenu.STRIPS);
    }

    @Override
    protected void addParts(PartRow row) {
        add(new Well(row, menu.getSlot(0), Component.translatable(KEY + "samples"))
                .story(Component.translatable(KEY + "samples.story", Genetics.MAX_SAMPLES)));
        add(new Tally(
                Sheet.LEFT_X,
                STATUS_Y,
                Sheet.PARTS_END - Sheet.LEFT_X,
                this::status,
                () -> menu.stopKey()
                        .<Component>map(Component::translatable)
                        .orElseGet(() -> Component.translatable(KEY + "status.story"))));
        for (int n = 0; n < Genetics.MAX_GENOMES; n++) {
            int index = n;
            add(new Tally(
                    Sheet.LEFT_X,
                    GENOMES_Y + n * LINE,
                    Sheet.RIGHT_EDGE - Sheet.LEFT_X,
                    () -> genome(index),
                    () -> Component.translatable(KEY + "genome.story", Genetics.MAX_GENOMES)));
        }
    }

    private Component status() {
        if (menu.extracting().isPresent())
            return Component.translatable(
                    KEY + "extracting", menu.extracting().get().getDescription(), menu.seconds());
        return menu.stop()
                .<Component>map(stop -> Component.translatable(
                        KEY + "stopped", Component.translatable(KEY + "stop." + condition(stop))))
                .orElseGet(() -> Component.translatable(KEY + "waiting"));
    }

    private Component genome(int index) {
        if (index >= menu.genomes()) return index == 0 ? Component.translatable(KEY + "no_genome") : Component.empty();
        Component name = menu.species(index)
                .map(EntityType::getDescription)
                .orElseGet(() -> Component.translatable(KEY + "unknown_species"));
        return Species.progress(name, menu.points(index));
    }

    /** The condition part of a stop's key, {@code dna_bank.<condition>}, for its short label. */
    private static String condition(Refusal stop) {
        return stop.key().substring(stop.key().indexOf('.') + 1);
    }
}
