package org.jd.infestusfrontier.mining;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jd.infestusfrontier.core.foundation.Refusal;
import org.jd.infestusfrontier.core.mining.Surveying;
import org.jd.infestusfrontier.foundation.screen.OrganScreen;
import org.jd.infestusfrontier.foundation.screen.Tally;
import org.jd.infestusfrontier.foundation.screen.Well;
import org.jd.infestusfrontier.ui.PartRow;
import org.jd.infestusfrontier.ui.Sheet;

/**
 * The grower's screen: the segment, tread and light wells on the first row, then lines for what it is doing (or why it
 * stopped), how far the plan under it is built and how many steps it has built in all.
 */
final class StructureGrowerScreen extends OrganScreen<StructureGrowerMenu> {
    private static final String KEY = "gui.infestusfrontier.structure_grower.";
    private static final int STATUS_Y = Sheet.PARTS_Y + Sheet.SLOT.height() + 6;
    private static final int LINE = 11;

    StructureGrowerScreen(StructureGrowerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, StructureGrowerMenu.STRIPS);
    }

    @Override
    protected boolean showsBiomass() {
        return false;
    }

    @Override
    protected void addParts(PartRow row) {
        add(new Well(row, menu.getSlot(0), Component.translatable(KEY + "stock"))
                .story(Component.translatable(KEY + "segments.story", Surveying.MAX_STOCK)));
        add(new Well(row, menu.getSlot(1), Component.empty())
                .story(Component.translatable(KEY + "treads.story", Surveying.MAX_STOCK)));
        add(new Well(row, menu.getSlot(2), Component.empty())
                .story(Component.translatable(KEY + "lights.story", Surveying.LIGHT_SPACING)));
        int width = Sheet.RIGHT_EDGE - Sheet.FIRST_PART_X;
        add(new Tally(
                Sheet.FIRST_PART_X,
                STATUS_Y,
                width,
                () -> menu.stop()
                        .map(stop -> Component.translatable(
                                KEY + "stopped", Component.translatable(KEY + "stop." + condition(stop))))
                        .orElseGet(() -> Component.translatable(KEY + "next", menu.seconds())),
                () -> menu.stopKey()
                        .<Component>map(Component::translatable)
                        .orElseGet(() -> Component.translatable(KEY + "status.story"))));
        add(new Tally(
                Sheet.FIRST_PART_X,
                STATUS_Y + LINE,
                width,
                () -> Component.translatable(KEY + "plan", menu.built(), menu.steps()),
                () -> Component.translatable(KEY + "plan.story")));
        add(new Tally(
                Sheet.FIRST_PART_X,
                STATUS_Y + 2 * LINE,
                width,
                () -> Component.translatable(KEY + "grown", menu.grown()),
                () -> Component.translatable(KEY + "grown.story")));
    }

    /** The condition part of a stop's key, {@code structure_grower.<condition>}, for its short label. */
    private static String condition(Refusal stop) {
        return stop.key().substring(stop.key().indexOf('.') + 1);
    }
}
