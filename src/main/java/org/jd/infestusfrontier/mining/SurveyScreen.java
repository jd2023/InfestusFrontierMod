package org.jd.infestusfrontier.mining;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jd.infestusfrontier.core.mining.MinePlan;
import org.jd.infestusfrontier.core.mining.PlanEdit;
import org.jd.infestusfrontier.core.mining.PlanShape;
import org.jd.infestusfrontier.core.mining.Survey;
import org.jd.infestusfrontier.core.mining.Surveying;
import org.jd.infestusfrontier.core.mining.Turn;
import org.jd.infestusfrontier.foundation.screen.OrganScreen;
import org.jd.infestusfrontier.foundation.screen.Tally;
import org.jd.infestusfrontier.ui.PartRow;
import org.jd.infestusfrontier.ui.Sheet;

/**
 * The marker's screen: four rows of buttons that shape the draft (shape and length, the first turn, the second turn,
 * lights, approval and a route's width), then lines for the plan, where it ends, what it needs and how far it is built. Shift asks for a
 * long stride. The plan itself is drawn in the world, from the marker. Nothing is decided here.
 */
final class SurveyScreen extends OrganScreen<SurveyMenu> {
    private static final String KEY = "gui.infestusfrontier.survey.";
    private static final int ROW_Y = 18;
    private static final int ROW_PITCH = Sheet.BUTTON.height() + 2;
    private static final int COLUMN_PITCH = Sheet.BUTTON.width() + 2;
    private static final int TALLY_Y = ROW_Y + 4 * ROW_PITCH + 4;
    private static final int LINE = 10;

    /** The edit each button asks for, in the order the buttons were made; the approve button is last. */
    private final List<PlanEdit> edits = new ArrayList<>();

    private final List<Button> editButtons = new ArrayList<>();
    private Button approve;

    SurveyScreen(SurveyMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, SurveyMenu.STRIPS);
    }

    @Override
    protected boolean showsBiomass() {
        return false;
    }

    @Override
    protected void addParts(PartRow row) {
        edits.clear();
        editButtons.clear();
        PlanEdit[][] rows = {
            {PlanEdit.SHAPE, PlanEdit.SHORTER, PlanEdit.LONGER},
            {PlanEdit.FIRST_WAY, PlanEdit.FIRST_EARLIER, PlanEdit.FIRST_LATER},
            {PlanEdit.SECOND_WAY, PlanEdit.SECOND_EARLIER, PlanEdit.SECOND_LATER},
            {PlanEdit.LIGHTS, null, PlanEdit.WIDTH}
        };
        for (int r = 0; r < rows.length; r++)
            for (int c = 0; c < rows[r].length; c++) {
                PlanEdit edit = rows[r][c];
                if (edit == null) continue; // the approve button's place
                Button button = button(
                        Sheet.LEFT_X + c * COLUMN_PITCH,
                        ROW_Y + r * ROW_PITCH,
                        Component.empty(),
                        b -> send(edit.ordinal() + (hasShiftDown() ? SurveyMenu.EDITS : 0)));
                button.setTooltip(Tooltip.create(Component.translatable(KEY + "edit." + name(edit) + ".story")));
                edits.add(edit);
                editButtons.add(button);
            }
        approve = button(
                Sheet.LEFT_X + COLUMN_PITCH,
                ROW_Y + 3 * ROW_PITCH,
                Component.translatable(KEY + "approve"),
                b -> send(SurveyMenu.APPROVE));
        approve.setTooltip(Tooltip.create(Component.translatable(KEY + "approve.story")));
        int width = Sheet.RIGHT_EDGE - Sheet.LEFT_X;
        add(new Tally(
                Sheet.LEFT_X,
                TALLY_Y,
                width,
                () -> menu.survey().map(s -> summary(s.plan())).orElseGet(() -> Component.translatable(KEY + "none")),
                () -> Component.translatable(KEY + "plan.story", Surveying.MAX_DEPTH, Surveying.MAX_ROUTE_LENGTH)));
        add(new Tally(
                Sheet.LEFT_X,
                TALLY_Y + LINE,
                width,
                () -> menu.end()
                        .<Component>map(end -> Component.translatable(KEY + "end", end.getX(), end.getY(), end.getZ()))
                        .orElse(Component.empty()),
                () -> Component.translatable(KEY + "end.story")));
        add(new Tally(
                Sheet.LEFT_X,
                TALLY_Y + 2 * LINE,
                width,
                () -> menu.survey()
                        .<Component>map(s -> Component.translatable(
                                KEY + "needs",
                                s.plan().needs().segments(),
                                s.plan().needs().treads(),
                                s.plan().needs().lights()))
                        .orElse(Component.empty()),
                () -> Component.translatable(KEY + "needs.story", Surveying.LIGHT_SPACING)));
        add(new Tally(
                Sheet.LEFT_X,
                TALLY_Y + 3 * LINE,
                width,
                () -> menu.survey().map(SurveyScreen::status).orElse(Component.empty()),
                () -> Component.translatable(KEY + "status.story")));
        label();
    }

    private static String name(PlanEdit edit) {
        return edit.name().toLowerCase(Locale.ROOT);
    }

    private static Component summary(MinePlan plan) {
        if (plan.shape() == PlanShape.STAIRCASE) return Component.translatable(KEY + "staircase", plan.length());
        List<String> turns = new ArrayList<>();
        for (Turn turn : List.of(plan.first(), plan.second()))
            if (turn.present())
                turns.add(
                        Component.translatable(KEY + "turn." + turn.way().name().toLowerCase(Locale.ROOT), turn.at())
                                .getString());
        String route = plan.wide() ? "wide_route" : "route";
        return turns.isEmpty()
                ? Component.translatable(KEY + route, plan.length())
                : Component.translatable(KEY + route + ".turns", plan.length(), String.join(", ", turns));
    }

    private static Component status(Survey survey) {
        int steps = survey.plan().steps().size();
        if (!survey.approved()) return Component.translatable(KEY + "draft");
        if (survey.finished()) return Component.translatable(KEY + "finished", steps);
        return Component.translatable(KEY + "approved", survey.built(), steps);
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

    /** Labels and enables the buttons from the plan as last synced. */
    private void label() {
        Optional<Survey> survey = menu.survey();
        boolean draft = survey.isPresent() && !survey.get().approved();
        approve.active = draft;
        for (int n = 0; n < edits.size(); n++) {
            PlanEdit edit = edits.get(n);
            Button button = editButtons.get(n);
            button.setMessage(survey.map(s -> label(edit, s.plan())).orElse(Component.empty()));
            button.active = draft
                    && !survey.get()
                            .plan()
                            .edit(edit, false)
                            .equals(survey.get().plan());
        }
    }

    private static Component label(PlanEdit edit, MinePlan plan) {
        return switch (edit) {
            case SHAPE ->
                Component.translatable(KEY + "shape." + plan.shape().name().toLowerCase(Locale.ROOT));
            case FIRST_WAY -> Component.translatable(KEY + "first." + way(plan.first()));
            case SECOND_WAY -> Component.translatable(KEY + "second." + way(plan.second()));
            case LIGHTS -> Component.translatable(KEY + (plan.lit() ? "lit" : "unlit"));
            case WIDTH -> Component.translatable(KEY + (plan.wide() ? "wide" : "narrow"));
            default -> Component.translatable(KEY + "edit." + name(edit));
        };
    }

    private static String way(Turn turn) {
        return turn.way().name().toLowerCase(Locale.ROOT);
    }
}
