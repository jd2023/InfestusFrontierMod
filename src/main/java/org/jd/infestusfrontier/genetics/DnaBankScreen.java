package org.jd.infestusfrontier.genetics;

import java.util.Optional;
import net.minecraft.client.gui.components.Tooltip;
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
 * The bank's screen: the sample well and the produce button on the parts row, the three construct material wells and
 * the construct well on the second, a line for what the bank is doing (or why it stopped), a line for what the
 * materials make (or why they make nothing), and one line for each genome it keeps with its progress. The tank shows
 * its biomass.
 */
final class DnaBankScreen extends OrganScreen<DnaBankMenu> {
    private static final String KEY = "gui.infestusfrontier.dna_bank.";
    /** Lines sit closer than on other screens so the four genomes clear the inventory label on a four-strip panel. */
    private static final int LINE = 10;

    private static final int STATUS_Y = Sheet.ROW2_Y + Sheet.SLOT.height() + 2;
    private static final int CONSTRUCT_Y = STATUS_Y + LINE;
    private static final int GENOMES_Y = CONSTRUCT_Y + LINE + 1;

    DnaBankScreen(DnaBankMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, DnaBankMenu.STRIPS);
    }

    @Override
    protected void addParts(PartRow row) {
        add(new Well(row, menu.getSlot(0), Component.translatable(KEY + "samples"))
                .story(Component.translatable(KEY + "samples.story", Genetics.MAX_SAMPLES)));
        button(row, Component.translatable(KEY + "produce"), b -> send(DnaBankMenu.PRODUCE_BUTTON))
                .setTooltip(Tooltip.create(Component.translatable(KEY + "produce.story")));
        for (int slot = 0; slot <= Genetics.MATERIAL_SLOTS; slot++) {
            boolean material = slot < Genetics.MATERIAL_SLOTS;
            Well well = new Well(
                    Sheet.FIRST_PART_X + slot * DnaBankMenu.WELL_PITCH,
                    Sheet.ROW2_LABEL_Y,
                    Sheet.ROW2_Y,
                    menu.getSlot(DnaBankMenu.FIRST_MATERIAL_SLOT + slot),
                    slot == 0 ? Component.translatable(KEY + "materials") : Component.empty());
            add(
                    material
                            ? well.story(Component.translatable(KEY + "materials.story"))
                            : well.mark(Sheet.trayMark(0)).story(Component.translatable(KEY + "construct.story")));
        }
        add(new Tally(
                Sheet.LEFT_X,
                STATUS_Y,
                Sheet.PARTS_END - Sheet.LEFT_X,
                this::status,
                () -> menu.stopKey()
                        .<Component>map(Component::translatable)
                        .orElseGet(() -> Component.translatable(KEY + "status.story"))));
        add(new Tally(
                Sheet.LEFT_X,
                CONSTRUCT_Y,
                Sheet.RIGHT_EDGE - Sheet.LEFT_X,
                this::construct,
                () -> menu.constructStop()
                        .<Component>map(stop -> Component.translatable("infestusfrontier.refusal." + stop.key()))
                        .orElseGet(
                                () -> Component.translatable(KEY + "construct.ready.story", menu.constructBiomass()))));
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

    /** "Ready: Stairs Construct", or why the materials make nothing now. Its tooltip names the biomass. */
    private Component construct() {
        Optional<Refusal> stop = menu.constructStop();
        if (stop.isEmpty() && menu.constructItem().isPresent())
            return Component.translatable(
                    KEY + "construct.ready", menu.constructItem().get().getDescription());
        return Component.translatable(
                KEY + "construct.stopped",
                Component.translatable(KEY + "stop." + condition(stop.orElse(Genetics.NO_CONSTRUCT))));
    }

    private void send(int id) {
        if (minecraft != null && minecraft.gameMode != null)
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
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
