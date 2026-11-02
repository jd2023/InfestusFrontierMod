package org.jd.infestusfrontier.storage;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jd.infestusfrontier.core.storage.PodKind;
import org.jd.infestusfrontier.foundation.screen.OrganScreen;
import org.jd.infestusfrontier.foundation.screen.Tally;
import org.jd.infestusfrontier.foundation.screen.Well;
import org.jd.infestusfrontier.ui.PartRow;
import org.jd.infestusfrontier.ui.Sheet;

/**
 * One pod on the plain panel: its compartment wells, the treatment well, and its grade and fill on the strip below. A
 * Larder is three rows instead: the compartment's well, its label well, a reserve button that steps through the
 * settings (shift steps back) and Withdraw reserve, with the line of held, kept and free under each.
 */
final class ItemCapsulePodScreen extends OrganScreen<ItemCapsulePodMenu> {
    private static final String KEY = "gui.infestusfrontier.item_capsule.pod.";
    private static final int LINE = 9;
    private static final int BUTTON_GAP = 2;

    private Button[] reserve = new Button[0];
    private Button[] withdraw = new Button[0];

    ItemCapsulePodScreen(ItemCapsulePodMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, ItemCapsulePodMenu.strips(menu.kind()));
    }

    @Override
    protected boolean showsBiomass() {
        return false;
    }

    @Override
    protected void addParts(PartRow row) {
        if (menu.kind().reserves()) {
            addLarder();
            return;
        }
        boolean craw = menu.kind() == PodKind.CRAW;
        for (int compartment = 0; compartment < menu.compartments(); compartment++)
            add(new Well(
                    row,
                    menu.getSlot(ItemCapsulePodMenu.COMPARTMENT + compartment),
                    craw
                            ? Component.literal(Integer.toString(compartment + 1))
                            : Component.translatable(KEY + "holds")));
        row.place(ItemCapsulePodMenu.spacer(menu.compartments())); // room for the label over the compartment
        add(new Well(
                row,
                menu.getSlot(ItemCapsulePodMenu.TREATMENT),
                Component.translatable(KEY + (craw ? "treatment.short" : "treatment"))));
        int width = Sheet.RIGHT_EDGE - Sheet.LEFT_X;
        add(new Tally(
                Sheet.LEFT_X,
                Sheet.ROW2_Y,
                width,
                () -> Component.translatable(
                        KEY + (craw ? "craw.grade" : "grade"),
                        menu.grade() + 1,
                        menu.kind().stacks(menu.grade())),
                () -> Component.translatable(KEY + (craw ? "craw.treatment.story" : "treatment.story"))));
        add(new Tally(
                Sheet.LEFT_X,
                Sheet.ROW2_Y + LINE + 1,
                width,
                () -> craw
                        ? Component.translatable(KEY + "craw.fill", menu.used(), menu.compartments(), menu.count())
                        : Component.translatable(KEY + "fill", menu.count(0), menu.limit(0)),
                () -> Component.translatable(KEY + (craw ? "craw.fill.story" : "fill.story"))));
    }

    private void addLarder() {
        int n = menu.compartments();
        reserve = new Button[n];
        withdraw = new Button[n];
        for (int c = 0; c < n; c++) {
            int compartment = c;
            int y = ItemCapsulePodMenu.rowY(c);
            add(new Well(Sheet.LEFT_X, y, y, menu.getSlot(ItemCapsulePodMenu.COMPARTMENT + c), Component.empty())
                    .story(Component.translatable(KEY + "larder.holds.story")));
            add(new Well(ItemCapsulePodMenu.LABEL_X, y, y, menu.getSlot(menu.labelSlot(c)), Component.empty())
                    .story(Component.translatable(KEY + "larder.label.story")));
            reserve[c] = button(
                    ItemCapsulePodMenu.RESERVE_X,
                    y + 1,
                    Component.empty(),
                    b -> send((Screen.hasShiftDown() ? ItemCapsulePodMenu.RESERVE_DOWN : ItemCapsulePodMenu.RESERVE_UP)
                                    * ItemCapsulePodMenu.BUTTON_STEP
                            + compartment));
            reserve[c].setTooltip(Tooltip.create(Component.translatable(KEY + "larder.reserve.story")));
            withdraw[c] = button(
                    ItemCapsulePodMenu.WITHDRAW_X,
                    y + 1,
                    Component.translatable(KEY + "larder.withdraw"),
                    b -> send(ItemCapsulePodMenu.WITHDRAW * ItemCapsulePodMenu.BUTTON_STEP + compartment));
            withdraw[c].setTooltip(Tooltip.create(Component.translatable(KEY + "larder.withdraw.story")));
            add(new Tally(
                    Sheet.LEFT_X,
                    y + Sheet.SLOT.height() + BUTTON_GAP - 1,
                    Sheet.RIGHT_EDGE - Sheet.LEFT_X,
                    () -> Component.translatable(
                            KEY + "larder.line",
                            menu.count(compartment),
                            menu.kept(compartment),
                            menu.free(compartment)),
                    () -> Component.translatable(KEY + "larder.line.story")));
        }
        int treatmentY = ItemCapsulePodMenu.LARDER_TREATMENT_Y;
        add(new Well(
                        Sheet.LEFT_X,
                        treatmentY,
                        treatmentY,
                        menu.getSlot(ItemCapsulePodMenu.TREATMENT),
                        Component.empty())
                .story(Component.translatable(KEY + "larder.treatment.story")));
        int textX = Sheet.LEFT_X + Sheet.SLOT.width() + 6;
        // the story's hover area is the start of each line, so a cursor resting mid-panel never covers the text
        int width = 50;
        add(new Tally(
                textX,
                treatmentY,
                width,
                () -> Component.translatable(
                        KEY + "larder.grade", menu.grade() + 1, menu.kind().stacks(menu.grade())),
                () -> Component.translatable(KEY + "larder.treatment.story")));
        add(new Tally(
                textX,
                treatmentY + LINE + 1,
                width,
                () -> Component.translatable(KEY + "larder.fill", menu.used(), menu.compartments(), menu.count()),
                () -> Component.translatable(KEY + "larder.fill.story")));
        label();
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

    /** The buttons name the settings the server last sent. */
    private void label() {
        for (int c = 0; c < reserve.length; c++) {
            int setting = menu.reserve(c);
            reserve[c].setMessage(
                    setting == 0
                            ? Component.translatable(KEY + "larder.keep.none")
                            : Component.translatable(KEY + "larder.keep", setting));
            withdraw[c].active = menu.kept(c) > 0;
        }
    }
}
