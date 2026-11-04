package org.jd.infestusfrontier.organ.api;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;

/**
 * The one player an organ's automatic work is credited to. The steward is the last player who placed the organ or
 * loaded it by hand; an organ with none is claimed by the first player who opens its screen. Standing nearby changes
 * nothing, and opening the screen of an organ that has a steward changes nothing. Breaking the organ forgets the
 * steward: the saved state does not travel on the item. Fake players are never stewards.
 */
public final class Steward {
    private static final String TAG = "steward";

    private Optional<UUID> player = Optional.empty();

    public Optional<UUID> player() {
        return player;
    }

    /** Makes {@code who} the steward, replacing any other. True when this changed it. */
    public boolean assign(Player who) {
        if (who instanceof FakePlayer || player.equals(Optional.of(who.getUUID()))) return false;
        player = Optional.of(who.getUUID());
        return true;
    }

    /** Makes {@code who} the steward only when there is none. True when this changed it. */
    public boolean claim(Player who) {
        return player.isEmpty() && assign(who);
    }

    /** Tells the world that {@code organ} did a unit of work for the steward, when there is one. */
    public void worked(ServerLevel level, ResourceLocation organ) {
        player.ifPresent(steward -> NeoForge.EVENT_BUS.post(new AutomaticWorkEvent(level, organ, steward)));
    }

    public void save(CompoundTag tag) {
        player.ifPresent(steward -> tag.putUUID(TAG, steward));
    }

    public void load(CompoundTag tag) {
        player = tag.hasUUID(TAG) ? Optional.of(tag.getUUID(TAG)) : Optional.empty();
    }
}
