package org.jd.infestusfrontier.mining;

import java.util.Optional;
import java.util.function.IntSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import org.jd.infestusfrontier.core.mining.MinePlan;
import org.jd.infestusfrontier.core.mining.PlanEdit;
import org.jd.infestusfrontier.core.mining.PlanShape;
import org.jd.infestusfrontier.core.mining.Steer;
import org.jd.infestusfrontier.core.mining.Survey;
import org.jd.infestusfrontier.core.mining.Turn;
import org.jd.infestusfrontier.foundation.menu.OrganMenu;

/**
 * The Surveyed Tissue screen's server half. It holds no slots of its own: as synced ints it shows the plan's numbers
 * and width,
 * whether it is approved, how many steps are built and which way the marker faces. Button {@code n} below
 * {@link #EDITS} asks for edit {@code n}; the next {@link #EDITS} ask for the same edits by a long stride; then
 * {@link #APPROVE}. The marker checks and applies each.
 */
public final class SurveyMenu extends OrganMenu {
    public static final int EDITS = PlanEdit.values().length;
    public static final int APPROVE = 2 * EDITS;
    static final int STRIPS = 4;

    private static final int SHAPE = 0;
    private static final int LENGTH = 1;
    private static final int FIRST_WAY = 2;
    private static final int FIRST_AT = 3;
    private static final int SECOND_WAY = 4;
    private static final int SECOND_AT = 5;
    private static final int LIT = 6;
    private static final int APPROVED = 7;
    private static final int BUILT = 8;
    private static final int FACING = 9;
    /** 1 while the marker's saved state is readable. */
    private static final int READABLE = 10;

    private static final int WIDE = 11;
    private static final int SYNCED = 12;

    private final Optional<SurveyedTissueBlockEntity> marker;

    /** Server side. */
    SurveyMenu(int id, Inventory inventory, SurveyedTissueBlockEntity marker) {
        this(id, inventory, reading(reads(marker)), at(marker), Optional.of(marker));
    }

    /** Client side. The position arrives as extra data. */
    public SurveyMenu(int id, Inventory inventory, RegistryFriendlyByteBuf extra) {
        this(id, inventory, new SimpleContainerData(SYNCED), at(inventory, extra), Optional.empty());
    }

    private SurveyMenu(
            int id,
            Inventory inventory,
            ContainerData data,
            ContainerLevelAccess access,
            Optional<SurveyedTissueBlockEntity> marker) {
        super(MiningModule.SURVEY_MENU.get(), id, access, MiningModule.SURVEYED_TISSUE.get(), data);
        this.marker = marker;
        addPlayer(inventory, STRIPS);
    }

    private static IntSupplier[] reads(SurveyedTissueBlockEntity marker) {
        IntSupplier[] reads = new IntSupplier[SYNCED];
        reads[SHAPE] = () -> plan(marker).shape().ordinal();
        reads[LENGTH] = () -> plan(marker).length();
        reads[FIRST_WAY] = () -> plan(marker).first().way().ordinal();
        reads[FIRST_AT] = () -> plan(marker).first().at();
        reads[SECOND_WAY] = () -> plan(marker).second().way().ordinal();
        reads[SECOND_AT] = () -> plan(marker).second().at();
        reads[LIT] = () -> plan(marker).lit() ? 1 : 0;
        reads[APPROVED] =
                () -> marker.survey().map(survey -> survey.approved() ? 1 : 0).orElse(0);
        reads[BUILT] = () -> marker.survey().map(Survey::built).orElse(0);
        reads[FACING] = () -> marker.facing().get2DDataValue();
        reads[READABLE] = () -> marker.survey().isPresent() ? 1 : 0;
        reads[WIDE] = () -> plan(marker).wide() ? 1 : 0;
        return reads;
    }

    private static MinePlan plan(SurveyedTissueBlockEntity marker) {
        return marker.survey().orElse(Survey.START).plan();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == APPROVE) {
            marker.ifPresent(m -> m.approve(player));
            return true;
        }
        Optional<PlanEdit> edit = PlanEdit.at(id % EDITS);
        if (id < 0 || id >= APPROVE || edit.isEmpty()) return false;
        marker.ifPresent(m -> m.edit(player, edit.get(), id >= EDITS));
        return true;
    }

    @Override
    public int stored() {
        return 0;
    }

    @Override
    public int capacity() {
        return 1;
    }

    /**
     * The plan and its progress as last synced, or empty while the marker's state is unreadable or the synced numbers
     * have not all arrived yet.
     */
    public Optional<Survey> survey() {
        if (synced(READABLE) != 1) return Optional.empty();
        Optional<PlanShape> shape = PlanShape.at(synced(SHAPE));
        Optional<Steer> first = Steer.at(synced(FIRST_WAY));
        Optional<Steer> second = Steer.at(synced(SECOND_WAY));
        if (shape.isEmpty() || first.isEmpty() || second.isEmpty()) return Optional.empty();
        try {
            return Optional.of(new Survey(
                    new MinePlan(
                            shape.get(),
                            synced(LENGTH),
                            new Turn(first.get(), synced(FIRST_AT)),
                            new Turn(second.get(), synced(SECOND_AT)),
                            synced(LIT) == 1,
                            synced(WIDE) == 1),
                    synced(APPROVED) == 1,
                    synced(BUILT)));
        } catch (IllegalArgumentException partlySynced) {
            return Optional.empty();
        }
    }

    /** The way the marker faces, as last synced. */
    public Direction facing() {
        return Direction.from2DDataValue(synced(FACING));
    }

    /** Where the plan as last synced ends in the world: its landing or its last cell. */
    public Optional<BlockPos> end() {
        return survey().map(survey -> {
            Direction forward = facing();
            var end = survey.plan().end();
            return pos().relative(forward, end.forward())
                    .relative(forward.getClockWise(), end.right())
                    .above(end.up());
        });
    }

    /** The marker's position. */
    public BlockPos pos() {
        return access().evaluate((level, at) -> at, BlockPos.ZERO);
    }
}
