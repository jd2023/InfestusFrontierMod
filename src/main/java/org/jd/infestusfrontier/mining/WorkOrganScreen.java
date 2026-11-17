package org.jd.infestusfrontier.mining;

import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jd.infestusfrontier.core.foundation.Refusal;
import org.jd.infestusfrontier.foundation.screen.OrganScreen;
import org.jd.infestusfrontier.foundation.screen.Tally;
import org.jd.infestusfrontier.ui.PartRow;
import org.jd.infestusfrontier.ui.Sheet;

/**
 * The Fracture Jaw's and the Collection Cilia's screen: a line for what the organ is doing or why it stopped, and a
 * line for its history. The tank shows its biomass.
 */
final class WorkOrganScreen extends OrganScreen<WorkOrganMenu> {
    private static final int LINE = 11;

    WorkOrganScreen(WorkOrganMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    private String key() {
        return "gui.infestusfrontier." + (menu.kind() == WorkOrganMenu.Kind.JAW ? "fracture_jaw" : "collection_cilia")
                + ".";
    }

    @Override
    protected void addParts(PartRow row) {
        String key = key();
        int width = Sheet.PARTS_END - Sheet.FIRST_PART_X;
        add(new Tally(
                Sheet.FIRST_PART_X,
                Sheet.LABEL_Y,
                width,
                () -> menu.stop()
                        .map(stop -> Component.translatable(
                                key + "stopped", Component.translatable(key + "stop." + condition(stop))))
                        .orElseGet(() -> Component.translatable(key + "next", menu.seconds())),
                () -> menu.stopKey()
                        .<Component>map(Component::translatable)
                        .orElseGet(() -> Component.translatable(key + "status.story"))));
        add(new Tally(
                Sheet.FIRST_PART_X,
                Sheet.LABEL_Y + LINE,
                width,
                () -> Component.translatable(key + "count", menu.count()),
                () -> Component.translatable(key + "count.story")));
    }

    /** The condition part of a stop's key, {@code <owner>.<condition>}, for its short label. */
    private static String condition(Refusal stop) {
        return stop.key().substring(stop.key().indexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}
