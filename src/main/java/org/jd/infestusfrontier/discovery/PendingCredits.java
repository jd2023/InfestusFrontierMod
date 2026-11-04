package org.jd.infestusfrontier.discovery;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
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
import org.slf4j.Logger;

/**
 * Batch credits and automatic-work credits for players who were offline when their organ finished or worked. Held on
 * the overworld, one credit per player, organ and kind and at most {@link #LIMIT} in all: past that a new credit is refused and logged, so no player's
 * credit ever evicts another's and the list we save is always one we can read back. A player's credits fire on
 * login and are forgotten. A credit list that cannot be read is kept as it was.
 */
public final class PendingCredits extends SavedData {
    /** The most credits held at once; the reader and the writer share it. */
    public static final int LIMIT = 4096;

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String NAME = "infestusfrontier_pending_credits";
    private static final String CREDITS = "credits";
    private static final String KEPT = "unreadable_credits";
    private static final SavedData.Factory<PendingCredits> FACTORY =
            new SavedData.Factory<>(PendingCredits::new, PendingCredits::load);

    /**
     * {@code automatic} tells work an organ did on its own for its steward apart from a batch a player started;
     * {@code recipe} is the recipe a batch ran, so a credit for a grinding does not stand for a washing.
     */
    private record Credit(UUID player, ResourceLocation organ, boolean automatic, Optional<ResourceLocation> recipe) {
        static final Codec<Credit> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                        UUIDUtil.CODEC.fieldOf("player").forGetter(Credit::player),
                        ResourceLocation.CODEC.fieldOf("organ").forGetter(Credit::organ),
                        Codec.BOOL.optionalFieldOf("automatic", false).forGetter(Credit::automatic),
                        ResourceLocation.CODEC.optionalFieldOf("recipe").forGetter(Credit::recipe))
                .apply(instance, Credit::new));
    }

    private final List<Credit> credits = new ArrayList<>();
    private Optional<Tag> unreadable = Optional.empty();

    public static PendingCredits get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(FACTORY, NAME);
    }

    public static PendingCredits load(CompoundTag tag, HolderLookup.Provider registries) {
        PendingCredits loaded = new PendingCredits();
        if (tag.contains(KEPT)) loaded.unreadable = Optional.of(tag.get(KEPT).copy());
        if (!tag.contains(CREDITS)) return loaded;
        DataResult<List<Credit>> read = Credit.CODEC
                .listOf(0, LIMIT)
                .parse(registries.createSerializationContext(NbtOps.INSTANCE), tag.get(CREDITS));
        read.ifSuccess(loaded.credits::addAll);
        read.ifError(error -> {
            LOGGER.warn("Pending credits keep an unreadable list: {}", error.message());
            loaded.unreadable = Optional.of(tag.get(CREDITS).copy());
        });
        return loaded;
    }

    public void remember(UUID player, ResourceLocation organ) {
        remember(player, organ, Optional.empty());
    }

    /** Remembers that a batch of {@code recipe}, when it names one, finished in {@code organ} for {@code player}. */
    public void remember(UUID player, ResourceLocation organ, Optional<ResourceLocation> recipe) {
        remember(new Credit(player, organ, false, recipe));
    }

    /** Remembers that {@code organ} worked on its own for {@code player}. */
    public void rememberAutomatic(UUID player, ResourceLocation organ) {
        remember(new Credit(player, organ, true, Optional.empty()));
    }

    private void remember(Credit credit) {
        if (credits.contains(credit)) return;
        if (credits.size() >= LIMIT) {
            LOGGER.warn(
                    "Pending credits full ({}); {} loses its credit for {}", LIMIT, credit.player(), credit.organ());
            return;
        }
        credits.add(credit);
        setDirty();
    }

    /** Fires every stored credit of {@code player} and forgets them. */
    public void award(ServerPlayer player) {
        boolean changed = false;
        for (Iterator<Credit> it = credits.iterator(); it.hasNext(); ) {
            Credit credit = it.next();
            if (!credit.player().equals(player.getUUID())) continue;
            if (credit.automatic()) DiscoveryModule.AUTOMATIC_WORK.get().trigger(player, credit.organ());
            else DiscoveryModule.ORGAN_BATCH_COMPLETED.get().trigger(player, credit.organ(), credit.recipe());
            it.remove();
            changed = true;
        }
        if (changed) setDirty();
    }

    public int size() {
        return credits.size();
    }

    /** True when {@code player} has a batch credit for {@code organ}, whatever recipe it ran. */
    public boolean holds(UUID player, ResourceLocation organ) {
        return credits.stream()
                .anyMatch(credit ->
                        credit.player().equals(player) && credit.organ().equals(organ) && !credit.automatic());
    }

    /** True when {@code player} has a batch credit for a batch of {@code recipe} finished in {@code organ}. */
    public boolean holds(UUID player, ResourceLocation organ, ResourceLocation recipe) {
        return credits.contains(new Credit(player, organ, false, Optional.of(recipe)));
    }

    public boolean holdsAutomatic(UUID player, ResourceLocation organ) {
        return credits.contains(new Credit(player, organ, true, Optional.empty()));
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        Tag encoded = Credit.CODEC
                .listOf()
                .encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), credits)
                .getOrThrow();
        tag.put(CREDITS, encoded);
        unreadable.ifPresent(kept -> tag.put(KEPT, kept.copy()));
        return tag;
    }
}
