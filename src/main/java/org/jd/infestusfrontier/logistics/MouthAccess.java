package org.jd.infestusfrontier.logistics;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.jd.infestusfrontier.core.foundation.Refusal;
import org.jd.infestusfrontier.core.logistics.Candidates;
import org.jd.infestusfrontier.core.logistics.Delivery;
import org.jd.infestusfrontier.foundation.Persistence;
import org.jd.infestusfrontier.logistics.api.ItemAccess;

/**
 * The transactions an organ makes through the Mouth under it. The far Mouths come from the shared topology and are
 * read live; a far Mouth in an unloaded chunk gives and takes nothing, and no chunk is loaded to find out. One call
 * takes one network work admission and looks at no more than {@link ItemAccess#CANDIDATES} far Mouths, however many
 * kinds it moves. The window of far Mouths a call looks at follows {@link Candidates}: a call that moved nothing, or a
 * simulated push that would move nothing, passes the Mouths it asked, so a later call reaches the ones behind.
 */
final class MouthAccess implements ItemAccess {
    private final MouthBlockEntity mouth;

    MouthAccess(MouthBlockEntity mouth) {
        this.mouth = mouth;
    }

    @Override
    public boolean takes() {
        return mouth.mode().takes();
    }

    @Override
    public boolean gives() {
        return mouth.mode().gives();
    }

    /** Whether a call may use the network now. */
    private sealed interface Admission {
        record Open(List<BlockPos> peers) implements Admission {}

        record Shut(Refusal why) implements Admission {}
    }

    /** The far Mouths of the network, or the reason the network cannot be used now; spends the admission. */
    private Admission admit(boolean direction, Refusal wrongWay) {
        if (!(mouth.getLevel() instanceof ServerLevel level)) return new Admission.Shut(BUSY);
        if (mouth.isUnreadable()) return new Admission.Shut(Persistence.CORRUPT);
        if (!direction) return new Admission.Shut(wrongWay);
        Optional<List<BlockPos>> peers = mouth.peers();
        if (peers.isEmpty()) return new Admission.Shut(NETWORK);
        if (!NetworkWork.take(level)) return new Admission.Shut(BUSY);
        return new Admission.Open(peers.get());
    }

    /** The loaded far Mouths that pass {@code use}, in the order they are asked: highest priority first, equals by turns. */
    private List<MouthBlockEntity> ordered(List<BlockPos> peers, Predicate<MouthBlockEntity> use) {
        List<Delivery.Destination<MouthBlockEntity>> found = new ArrayList<>();
        for (BlockPos peer : peers) {
            Optional<MouthBlockEntity> far = mouth.destination(peer);
            if (far.isPresent() && use.test(far.get()))
                found.add(new Delivery.Destination<>(far.get(), far.get().priority()));
        }
        return Delivery.order(found, mouth.turn());
    }

    @Override
    public Drawn pull(List<Want> wants) {
        List<ItemStack> none = wants.stream().map(w -> ItemStack.EMPTY).toList();
        Admission admission = admit(takes(), NOT_TAKING);
        if (admission instanceof Admission.Shut shut) return new Drawn(none, false, Optional.of(shut.why()));
        List<MouthBlockEntity> sources = mouth.pulls()
                .window(
                        ordered(
                                ((Admission.Open) admission).peers(),
                                far -> far.mode().gives()),
                        CANDIDATES);
        List<ItemStack> drawn = new ArrayList<>();
        boolean asked = false;
        boolean moved = false;
        for (Want want : wants) {
            ItemStack kind = want.kind().copyWithCount(1);
            int room = kind.isEmpty() || !mouth.wants(kind)
                    ? 0
                    : Delivery.intakeRoom(mouth.amount(), mouth.amount() == 0 ? 0 : want.held(), want.max());
            if (room <= 0) {
                drawn.add(ItemStack.EMPTY);
                continue;
            }
            asked = true;
            int got = 0;
            for (MouthBlockEntity far : sources) {
                if (got >= room) break;
                got += far.release(kind, room - got).getCount();
            }
            drawn.add(got == 0 ? ItemStack.EMPTY : kind.copyWithCount(got));
            if (got > 0) {
                moved = true;
                mouth.turned();
            }
        }
        if (moved) mouth.pulls().moved();
        else if (asked) mouth.pulls().refused(sources.size());
        return new Drawn(drawn, true, asked || wants.isEmpty() ? Optional.empty() : Optional.of(TURNED_AWAY));
    }

    @Override
    public Sent push(List<Offer> offers, boolean simulate) {
        List<ItemStack> kept = offers.stream().map(o -> o.stack().copy()).toList();
        Admission admission = admit(gives(), NOT_GIVING);
        if (admission instanceof Admission.Shut shut) return new Sent(kept, false, Optional.of(shut.why()));
        List<MouthBlockEntity> takers = mouth.pushes()
                .window(
                        ordered(
                                ((Admission.Open) admission).peers(),
                                far -> far.mode().takes()),
                        CANDIDATES);
        List<ItemStack> rests = new ArrayList<>();
        boolean asked = false;
        boolean moved = false;
        for (Offer offer : offers) {
            ItemStack stack = offer.stack();
            int count = stack.getCount();
            int allowed = stack.isEmpty() || !mouth.permits(stack)
                    ? 0
                    : mouth.amount() == 0 ? count : Delivery.outputAllowance(mouth.amount(), offer.held(), count);
            int left = allowed;
            if (allowed > 0) asked = true;
            for (MouthBlockEntity far : takers) {
                if (left <= 0) break;
                ItemStack part = stack.copyWithCount(left);
                if (!far.wants(part)) continue;
                int room = Math.min(far.room(part), left);
                if (room <= 0) continue;
                left -= simulate
                        ? room
                        : room - far.accept(stack.copyWithCount(room)).getCount();
            }
            rests.add(stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(count - (allowed - left)));
            if (left < allowed) {
                moved = true;
                if (!simulate) mouth.turned();
            }
        }
        if (moved) {
            if (!simulate) mouth.pushes().moved();
        } else if (asked) mouth.pushes().refused(takers.size());
        return new Sent(rests, true, asked || offers.isEmpty() ? Optional.empty() : Optional.of(TURNED_AWAY));
    }
}
