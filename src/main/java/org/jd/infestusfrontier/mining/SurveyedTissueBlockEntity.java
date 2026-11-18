package org.jd.infestusfrontier.mining;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jd.infestusfrontier.core.foundation.Outcome;
import org.jd.infestusfrontier.core.foundation.Refusal;
import org.jd.infestusfrontier.core.mining.Offset;
import org.jd.infestusfrontier.core.mining.PlanEdit;
import org.jd.infestusfrontier.core.mining.PlanStep;
import org.jd.infestusfrontier.core.mining.Survey;
import org.jd.infestusfrontier.core.mining.Surveying;
import org.jd.infestusfrontier.foundation.Persistence;
import org.jd.infestusfrontier.foundation.Refusals;

/**
 * The plan of a Surveyed Tissue marker and how far it is built. It never ticks and never reads the ground: a plan is
 * geometry the player chose. Clients get the plan to draw its preview, the steps still to build, from these numbers
 * alone. Unreadable saved state is kept as it was; the marker then refuses every edit and offers no plan.
 */
final class SurveyedTissueBlockEntity extends BlockEntity implements MenuProvider {
    private Survey survey = Survey.START;
    private Optional<CompoundTag> unreadable = Optional.empty();

    SurveyedTissueBlockEntity(BlockPos pos, BlockState state) {
        super(MiningModule.SURVEYED_TISSUE_ENTITY.get(), pos, state);
    }

    /** The plan and its progress, or empty while the saved state is unreadable. */
    Optional<Survey> survey() {
        return unreadable.isPresent() ? Optional.empty() : Optional.of(survey);
    }

    Direction facing() {
        return getBlockState().getValue(SurveyedTissueBlock.FACING);
    }

    /** Where {@code offset} of the plan lies in the world. */
    BlockPos at(Offset offset) {
        Direction forward = facing();
        return worldPosition
                .relative(forward, offset.forward())
                .relative(forward.getClockWise(), offset.right())
                .above(offset.up());
    }

    /** Changes the draft as {@code player} asked, or shows them why not. */
    void edit(Player player, PlanEdit edit, boolean far) {
        apply(player, unreadable.isPresent() ? Outcome.refused(Persistence.CORRUPT) : survey.edit(edit, far));
    }

    /** Approves the draft for building, or shows {@code player} why not: every block of it must lie in the world. */
    void approve(Player player) {
        if (unreadable.isPresent()) {
            apply(player, Outcome.refused(Persistence.CORRUPT));
            return;
        }
        if (level != null && !inWorld()) {
            apply(player, Outcome.refused(Surveying.OUTSIDE_WORLD));
            return;
        }
        apply(player, survey.approve());
    }

    private boolean inWorld() {
        for (PlanStep step : survey.plan().steps()) {
            for (Offset root : step.roots()) if (level.isOutsideBuildHeight(at(root).below())) return false;
            for (Offset cell : step.cleared()) if (level.isOutsideBuildHeight(at(cell))) return false;
        }
        return true;
    }

    private void apply(Player player, Outcome<Survey> outcome) {
        if (outcome instanceof Outcome.Refused<Survey>(Refusal refusal)) {
            Refusals.show(player, refusal);
            return;
        }
        survey = ((Outcome.Done<Survey>) outcome).value();
        changed();
    }

    /** The grower built the next step. Only after it has checked that there is one. */
    void grown() {
        survey = survey.grown();
        changed();
    }

    /** Saved, and sent to the clients that draw the preview. */
    private void changed() {
        setChanged();
        if (level != null)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    // ---- saved state ----

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        Optional<Survey> read = Persistence.read(
                SurveyContents.CODEC, tag, registries, MiningModule.LOGGER, "Surveyed Tissue at " + worldPosition);
        unreadable = read.isPresent() ? Optional.empty() : Optional.of(tag.copy());
        survey = read.orElse(Survey.START);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (unreadable.isPresent()) tag.merge(unreadable.get());
        else Persistence.write(SurveyContents.CODEC, survey, registries, tag);
    }

    /** Clients get the plan and its progress to draw; an unreadable marker sends nothing, so they draw nothing. */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        if (unreadable.isEmpty()) Persistence.write(SurveyContents.CODEC, survey, registries, tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new SurveyMenu(id, inventory, this);
    }
}
