package org.jd.infestusfrontier.logistics.api;

import java.util.List;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;
import org.jd.infestusfrontier.core.foundation.Refusal;

/**
 * A Mouth as an organ standing on it uses the Item Vein network: one transaction draws items from the output Mouths
 * of the Mouth's network, or sends items to its intake Mouths. Exposed as {@link LogisticsCapabilities#ITEM_ACCESS}
 * on the Mouth, found by the organ at the position under it. Logistics owns every rule: the Mouth's direction, filter,
 * amount and priority, each far Mouth's own, the store's sided inventory and reserves, loaded chunks only, and the
 * shared network work admission. A call is one admission and looks at no more than {@link #CANDIDATES} far Mouths;
 * what it moves is really taken from one store and really put in another before it returns, so the caller holds
 * every item it was given and nothing else. Runs on the server.
 */
public interface ItemAccess {
    /** Far Mouths one call looks at. */
    int CANDIDATES = 16;

    /** The Mouth does not take from the network: it is Output or closed. */
    Refusal NOT_TAKING = new Refusal("logistics.access_not_taking");
    /** The Mouth does not give to the network: it is Intake or closed. */
    Refusal NOT_GIVING = new Refusal("logistics.access_not_giving");
    /** The Mouth's network is rebuilding or too large to be read. */
    Refusal NETWORK = new Refusal("logistics.access_network");
    /** The shared network work of this tick is spent. */
    Refusal BUSY = new Refusal("logistics.access_busy");
    /** The Mouth's filter or amount turns every request away. */
    Refusal TURNED_AWAY = new Refusal("logistics.access_turned_away");

    /** One kind wanted: at most {@code max} of it, while the asker already holds {@code held} (for the Mouth's amount). */
    record Want(ItemStack kind, int max, int held) {}

    /** One stack offered, while the asker holds {@code held} of its kind in all (for the Mouth's amount). */
    record Offer(ItemStack stack, int held) {}

    /**
     * What a pull gave, one stack per {@link Want} in order (empty where nothing came). {@code why} is present when the
     * call could not consult the network, or when the Mouth turned every request away; {@code consulted} says whether
     * the network was looked at, so an empty answer from a looked-at network is told from a failed call.
     */
    record Drawn(List<ItemStack> stacks, boolean consulted, Optional<Refusal> why) {
        public int count() {
            return stacks.stream().mapToInt(ItemStack::getCount).sum();
        }
    }

    /** What a push left, one stack per {@link Offer} in order: the part no Mouth took. Same {@code why} as {@link Drawn}. */
    record Sent(List<ItemStack> rests, boolean consulted, Optional<Refusal> why) {}

    /** The Mouth lets the network feed what stands on it. */
    boolean takes();

    /** The Mouth lets the network take from what stands on it. */
    boolean gives();

    /** Takes the wanted kinds from the output Mouths of the network. Needs a Mouth that {@link #takes}. */
    Drawn pull(List<Want> wants);

    /**
     * Sends the offered stacks to the intake Mouths of the network, in their priority order; the stacks are not changed
     * and the rests say what stayed. Needs a Mouth that {@link #gives}. With {@code simulate} nothing moves.
     */
    Sent push(List<Offer> offers, boolean simulate);
}
