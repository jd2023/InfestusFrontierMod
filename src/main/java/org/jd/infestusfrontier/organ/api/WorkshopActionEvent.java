package org.jd.infestusfrontier.organ.api;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.neoforged.bus.api.Event;

/**
 * Posted on the NeoForge bus once when a player, at a screen, does something that earns a workshop milestone and the
 * game has checked that it worked: a stack stored in a Capsule Tree, a Shaping Palm craft that used items its Mouth
 * fetched. A refused or empty action posts nothing. Actions an organ does for its steward are
 * {@link AutomaticWorkEvent}s.
 */
public final class WorkshopActionEvent extends Event {
    /** What the player did; {@link #getSerializedName} is the name advancements use. */
    public enum Action implements StringRepresentable {
        /** A stack went into a pod of a standing Capsule Stem tree, by hand. */
        TREE_STORED("tree_stored"),
        /** A Shaping Palm craft took the result with at least one ingredient the Palm's Mouth had fetched. */
        PALM_CRAFT_FROM_STORAGE("palm_craft_from_storage");

        private final String id;

        Action(String id) {
            this.id = id;
        }

        @Override
        public String getSerializedName() {
            return id;
        }
    }

    private final ServerPlayer player;
    private final Action action;

    public WorkshopActionEvent(ServerPlayer player, Action action) {
        this.player = player;
        this.action = action;
    }

    public ServerPlayer player() {
        return player;
    }

    public Action action() {
        return action;
    }
}
