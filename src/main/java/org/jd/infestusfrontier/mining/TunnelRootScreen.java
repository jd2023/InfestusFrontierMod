package org.jd.infestusfrontier.mining;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Inventory;
import org.jd.infestusfrontier.core.foundation.Refusal;
import org.jd.infestusfrontier.core.mining.RootMutation;
import org.jd.infestusfrontier.core.mining.Steer;
import org.jd.infestusfrontier.core.mining.Tunnelling;
import org.jd.infestusfrontier.foundation.screen.Note;
import org.jd.infestusfrontier.foundation.screen.OrganScreen;
import org.jd.infestusfrontier.foundation.screen.Tally;
import org.jd.infestusfrontier.foundation.screen.Well;
import org.jd.infestusfrontier.ui.PartRow;
import org.jd.infestusfrontier.ui.Sheet;

/**
 * The head's screen, top down: where it heads and where its next step goes, with a button for each way, left and right
 * relative to the head, never to the player; then the seconds to its next step; then the segment well and the output
 * wells; then, in full, why it stopped; then the length of its root and its mutations. The tank shows its biomass. A
 * Rootstock working as a minehead takes no steps, so its buttons rest; it shows instead the step its section is at and
 * the steps it has advanced, and with galleries a line that counts its faces at work and paused, its tooltip naming
 * each face's step or what holds it up.
 */
final class TunnelRootScreen extends OrganScreen<TunnelRootMenu> {
    private static final String KEY = "gui.infestusfrontier.tunnel_root.";
    /** The widths of the three steering buttons, left, ahead and right: side by side left of the tank. */
    private static final List<Integer> STEER_WIDTHS = List.of(31, 38, 31);

    private static final int STEER_GAP = 1;
    private static final int PROGRESS_Y = Sheet.PARTS_Y + Sheet.BUTTON.height() + 3;
    private static final int WELL_Y = TunnelRootMenu.WELL_SLOT_Y - 1;
    private static final int WELL_LABEL_Y = WELL_Y - 8;
    private static final int STOP_Y = WELL_Y + Sheet.SLOT.height() + 3;
    private static final int STOP_LINES = 2;
    private static final int STATUS_Y = STOP_Y + 21;
    private static final int LINE = 11;
    private static final List<Steer> WAYS = List.of(Steer.LEFT, Steer.STRAIGHT, Steer.RIGHT);

    private final List<Button> ways = new ArrayList<>();

    TunnelRootScreen(TunnelRootMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, TunnelRootMenu.STRIPS);
    }

    @Override
    protected void addParts(PartRow row) {
        int width = Sheet.RIGHT_EDGE - Sheet.FIRST_PART_X;
        add(new Tally(
                Sheet.FIRST_PART_X,
                Sheet.LABEL_Y,
                Sheet.PARTS_END - Sheet.FIRST_PART_X,
                this::heading,
                () -> Component.translatable(KEY + "steer.story")));
        ways.clear();
        int x = Sheet.FIRST_PART_X;
        for (int n = 0; n < WAYS.size(); n++) {
            Steer way = WAYS.get(n);
            Button button = button(
                    x,
                    Sheet.PARTS_Y,
                    STEER_WIDTHS.get(n),
                    Component.translatable(KEY + "steer." + name(way)),
                    b -> send(way.ordinal()));
            button.setTooltip(Tooltip.create(Component.translatable(KEY + "steer." + name(way) + ".story")));
            ways.add(button);
            x += STEER_WIDTHS.get(n) + STEER_GAP;
        }
        add(new Tally(
                Sheet.FIRST_PART_X,
                PROGRESS_Y,
                Sheet.PARTS_END - Sheet.FIRST_PART_X,
                this::progress,
                () -> Component.translatable(KEY + (menu.minehead() ? "minehead.story" : "status.story"))));
        add(new Well(
                        Sheet.FIRST_PART_X,
                        WELL_LABEL_Y,
                        WELL_Y,
                        menu.getSlot(0),
                        Component.translatable(KEY + "segments"))
                .story(Component.translatable(KEY + "segments.story", Tunnelling.MAX_SEGMENTS)));
        for (int slot = 0; slot < Tunnelling.OUTPUT_SLOTS; slot++)
            add(new Well(
                            TunnelRootMenu.OUTPUT_X - 1 + slot * TunnelRootMenu.OUTPUT_PITCH,
                            WELL_LABEL_Y,
                            WELL_Y,
                            menu.getSlot(1 + slot),
                            slot == 0 ? Component.translatable(KEY + "output") : Component.empty())
                    .story(Component.translatable(KEY + "output.story", Tunnelling.OUTPUT_ITEMS)));
        add(new Note(Sheet.FIRST_PART_X, STOP_Y, width, STOP_LINES, this::reason));
        add(new Tally(
                Sheet.FIRST_PART_X,
                STATUS_Y,
                width,
                () -> galleries()
                        ? faces()
                        : menu.minehead()
                                ? Component.translatable(KEY + "descended", menu.descended())
                                : Component.translatable(KEY + "cells", menu.cells(), Tunnelling.MAX_CELLS),
                () -> galleries()
                        ? faceStory()
                        : Component.translatable(KEY + (menu.minehead() ? "descended.story" : "cells.story"))));
        add(new Tally(
                Sheet.FIRST_PART_X,
                STATUS_Y + LINE,
                width,
                this::mutations,
                () -> Component.translatable(KEY + "mutations.story")));
        label();
    }

    private static String name(Steer way) {
        return way.name().toLowerCase(Locale.ROOT);
    }

    /** "Heading north, next step west". */
    private Component heading() {
        return Component.translatable(
                KEY + "heading",
                Component.translatable(KEY + "dir." + menu.heading().getSerializedName()),
                Component.translatable(KEY + "dir." + menu.next().getSerializedName()));
    }

    /** The full reason the head or its minehead stopped, or nothing while it works. */
    private Component reason() {
        return menu.stop()
                .<Component>map(stop -> Component.translatable("infestusfrontier.refusal." + stop.key()))
                .orElse(Component.empty());
    }

    /** True for a minehead with gallery faces: show face counts and mutations below its stop reason. */
    private boolean galleries() {
        return menu.minehead() && !menu.faces().isEmpty();
    }

    /** "Faces: 2 at work, 1 paused". */
    private Component faces() {
        List<TunnelRootMenu.Face> faces = menu.faces();
        long paused = faces.stream().filter(face -> face.stop().isPresent()).count();
        return Component.translatable(KEY + "faces", faces.size() - paused, paused);
    }

    /** One line per face: its width and step, or the part that holds it up and why. */
    private Component faceStory() {
        MutableComponent story = Component.translatable(KEY + "faces.story");
        story.append("\n").append(Component.translatable(KEY + "descended", menu.descended()));
        List<TunnelRootMenu.Face> faces = menu.faces();
        for (int n = 0; n < faces.size(); n++) {
            TunnelRootMenu.Face face = faces.get(n);
            Component width = Component.translatable(KEY + (face.wide() ? "face.wide" : "face.narrow"));
            Component state = face.stop()
                    .<Component>map(stop -> Component.translatable(
                            KEY + "face.paused",
                            Component.translatable(KEY + "part." + owner(stop)),
                            Component.translatable("infestusfrontier.refusal." + stop.key())))
                    .orElseGet(() -> Component.translatable(
                            KEY + "face.working", Math.min(face.built() + 1, face.steps()), face.steps()));
            story.append("\n").append(Component.translatable(KEY + "face", n + 1, width, state));
        }
        return story;
    }

    /** The seconds to the next step, or what stopped it; for a minehead, its section's step or the part that holds it up. */
    private Component progress() {
        if (menu.minehead())
            return menu.stop()
                    .map(stop ->
                            Component.translatable(KEY + "paused", Component.translatable(KEY + "part." + owner(stop))))
                    .orElseGet(() -> menu.faces().isEmpty()
                            ? Component.translatable(
                                    KEY + "mining", Math.min(menu.built() + 1, menu.steps()), menu.steps())
                            : Component.translatable(
                                    KEY + "galleries", menu.faces().size()));
        return menu.stop()
                .map(stop -> Component.translatable(
                        KEY + "stopped", Component.translatable(KEY + "stop." + condition(stop))))
                .orElseGet(() -> Component.translatable(KEY + "next", menu.seconds()));
    }

    /** The owner part of a stop's key, {@code <owner>.<condition>}: which part of the minehead holds it up. */
    private static String owner(Refusal stop) {
        return stop.key().substring(0, stop.key().indexOf('.'));
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
        return names;
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

    /** The chosen way rests pressed; a minehead takes no steps, so none is offered. */
    private void label() {
        for (int n = 0; n < WAYS.size(); n++) ways.get(n).active = !menu.minehead() && menu.steer() != WAYS.get(n);
    }
}
