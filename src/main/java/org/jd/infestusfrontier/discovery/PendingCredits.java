package org.jd.infestusfrontier.discovery;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Batch credits for players who were offline when their organ finished. Held on the overworld, at most
 * {@link #LIMIT} entries; the oldest is dropped first. A player's credits fire on login and are forgotten.
 */
public final class PendingCredits extends SavedData {
    public static final int LIMIT = 64;
    private static final String NAME = "infestusfrontier_pending_credits";
    private static final String CREDITS = "credits";
    private static final SavedData.Factory<PendingCredits> FACTORY =
            new SavedData.Factory<>(PendingCredits::new, PendingCredits::load);

    private record Credit(UUID player, ResourceLocation organ) {
        static final Codec<Credit> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                        UUIDUtil.CODEC.fieldOf("player").forGetter(Credit::player),
                        ResourceLocation.CODEC.fieldOf("organ").forGetter(Credit::organ))
                .apply(instance, Credit::new));
    }

    private final List<Credit> credits = new ArrayList<>();

    public static PendingCredits get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(FACTORY, NAME);
    }

    private static PendingCredits load(CompoundTag tag, HolderLookup.Provider registries) {
        PendingCredits loaded = new PendingCredits();
        Credit.CODEC
                .listOf()
                .parse(registries.createSerializationContext(NbtOps.INSTANCE), tag.get(CREDITS))
                .result()
                .ifPresent(read -> read.stream().limit(LIMIT).forEach(loaded.credits::add));
        return loaded;
    }

    public void remember(UUID player, ResourceLocation organ) {
        Credit credit = new Credit(player, organ);
        if (credits.contains(credit)) return;
        credits.add(credit);
        while (credits.size() > LIMIT) credits.remove(0);
        setDirty();
    }

    /** Fires every stored credit of {@code player} and forgets them. */
    public void award(ServerPlayer player) {
        boolean changed = false;
        for (Iterator<Credit> it = credits.iterator(); it.hasNext(); ) {
            Credit credit = it.next();
            if (!credit.player().equals(player.getUUID())) continue;
            DiscoveryModule.ORGAN_BATCH_COMPLETED.get().trigger(player, credit.organ());
            it.remove();
            changed = true;
        }
        if (changed) setDirty();
    }

    public int size() {
        return credits.size();
    }

    public boolean holds(UUID player, ResourceLocation organ) {
        return credits.contains(new Credit(player, organ));
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        Tag encoded = Credit.CODEC
                .listOf()
                .encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), credits)
                .getOrThrow();
        tag.put(CREDITS, encoded);
        return tag;
    }
}
