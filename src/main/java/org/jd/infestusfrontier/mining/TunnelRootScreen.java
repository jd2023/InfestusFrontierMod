package org.jd.infestusfrontier.mining;

import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Inventory;
import org.jd.infestusfrontier.core.foundation.Refusal;
import org.jd.infestusfrontier.core.mining.RootMutation;
import org.jd.infestusfrontier.core.mining.Tunnelling;
import org.jd.infestusfrontier.foundation.screen.OrganScreen;
import org.jd.infestusfrontier.foundation.screen.Tally;
import org.jd.infestusfrontier.foundation.screen.Well;
import org.jd.infestusfrontier.ui.PartRow;
import org.jd.infestusfrontier.ui.Sheet;

/**
 * The head's screen: the segment well and the steering button on the first row, the output wells on the second, then
 * a line for what the head is doing (or why it stopped), a line for the length of its root and a line naming its
 * mutations. The tank shows its biomass.
 */
final class TunnelRootScreen extends OrganScreen<TunnelRootMenu> {
    private static final String KEY = "gui.infestusfrontier.tunnel_root.";
    private static final int STATUS_Y = Sheet.ROW2_Y + Sheet.SLOT.height() + 4;
    private static final int LINE = 11;

    private Button steer;

    TunnelRootScreen(TunnelRootMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, TunnelRootMenu.STRIPS);
    }

    @Override
    protected void addParts(PartRow row) {
        add(new Well(row, menu.getSlot(0), Component.translatable(KEY + "segments"))
                .story(Component.translatable(KEY + "segments.story", Tunnelling.MAX_SEGMENTS)));
        steer = button(row, Component.empty(), b -> send(menu.steer().next().ordinal()));
        steer.setTooltip(Tooltip.create(Component.translatable(KEY + "steer.story")));
        for (int slot = 0; slot < Tunnelling.OUTPUT_SLOTS; slot++)
            add(new Well(
                            Sheet.FIRST_PART_X + slot * TunnelRootMenu.OUTPUT_PITCH,
                            Sheet.ROW2_LABEL_Y,
                            Sheet.ROW2_Y,
                            menu.getSlot(1 + slot),
                            slot == 0 ? Component.translatable(KEY + "output") : Component.empty())
                    .story(Component.translatable(KEY + "output.story", Tunnelling.OUTPUT_ITEMS)));
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
                () -> Component.translatable(KEY + "cells", menu.cells(), Tunnelling.MAX_CELLS),
                () -> Component.translatable(KEY + "cells.story")));
        add(new Tally(
                Sheet.FIRST_PART_X,
                STATUS_Y + 2 * LINE,
                width,
                this::mutations,
                () -> Component.translatable(KEY + "mutations.story")));
        label();
    }

    /** "Mutations: none", or the installed mutations in their fixed order. */
    private Component mutations() {
        List<RootMutation> installed = menu.mutations();
        if (installed.isEmpty()) return Component.translatable(KEY + "mutations.none");
        MutableComponent names = Component.empty();
        for (int n = 0; n < installed.size(); n++) {
            if (n > 0) names.append(", ");
            names.append(
                    Component.translatable(KEY + "mutation." + installed.get(n).id()));
        }
        return Component.translatable(KEY + "mutations", names);
    }

    /** The condition part of a stop's key, {@code tunnel_root.<condition>}, for its short label. */
    private static String condition(Refusal stop) {
        return stop.key().substring(stop.key().indexOf('.') + 1);
    }

    private void send(int id) {
        if (minecraft != null && minecraft.gameMode != null)
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        label();
    }

    private void label() {
        steer.setMessage(
                Component.translatable(KEY + "steer." + menu.steer().name().toLowerCase(Locale.ROOT)));
    }
}
