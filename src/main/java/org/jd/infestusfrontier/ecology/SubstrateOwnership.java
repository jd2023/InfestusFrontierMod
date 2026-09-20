package org.jd.infestusfrontier.ecology;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

final class SubstrateOwnership extends SavedData {
    private static final String DATA_NAME = "infestusfrontier_ecology_cells";
    private static final int MAX_CELLS_PER_DIMENSION = 65_536;
    private static final Factory<SubstrateOwnership> FACTORY = new Factory<>(
            SubstrateOwnership::new,
            SubstrateOwnership::load);

    private final Map<Long, UUID> owners = new HashMap<>();
    private boolean rejected;

    static SubstrateOwnership get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    boolean canClaim(BlockPos pos) {
        return !rejected && (owners.containsKey(pos.asLong()) || owners.size() < MAX_CELLS_PER_DIMENSION);
    }

    boolean claim(BlockPos pos, UUID owner) {
        if (!canClaim(pos)) return false;
        long key = pos.asLong();
        if (!owner.equals(owners.put(key, owner))) setDirty();
        return true;
    }

    boolean permits(BlockPos pos, UUID player) {
        UUID owner = owners.get(pos.asLong());
        return !rejected && (owner == null || owner.equals(player));
    }

    Optional<UUID> owner(BlockPos pos) {
        return Optional.ofNullable(owners.get(pos.asLong()));
    }

    void remove(BlockPos pos) {
        if (!rejected && owners.remove(pos.asLong()) != null) setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        if (rejected) throw new IllegalStateException("Cannot overwrite rejected substrate ownership data");
        ListTag cells = new ListTag();
        owners.forEach((position, owner) -> {
            CompoundTag cell = new CompoundTag();
            cell.putLong("position", position);
            cell.putUUID("owner", owner);
            cells.add(cell);
        });
        tag.putInt("schema", 1);
        tag.put("cells", cells);
        return tag;
    }

    private static SubstrateOwnership load(CompoundTag tag, HolderLookup.Provider registries) {
        SubstrateOwnership data = new SubstrateOwnership();
        if (!tag.contains("schema", Tag.TAG_INT) || tag.getInt("schema") != 1) {
            return data.reject("unsupported schema " + tag.getInt("schema"));
        }
        if (!(tag.get("cells") instanceof ListTag cells)
                || (!cells.isEmpty() && cells.getElementType() != Tag.TAG_COMPOUND)
                || cells.size() > MAX_CELLS_PER_DIMENSION) {
            return data.reject("invalid cells list or exceeded cell limit");
        }
        for (int index = 0; index < cells.size(); index++) {
            CompoundTag cell = cells.getCompound(index);
            if (!cell.contains("position", Tag.TAG_LONG) || !cell.hasUUID("owner")
                    || data.owners.putIfAbsent(cell.getLong("position"), cell.getUUID("owner")) != null) {
                return data.reject("invalid or duplicate cell at index " + index);
            }
        }
        return data;
    }

    private SubstrateOwnership reject(String reason) {
        // Throwing here lets DimensionDataStorage replace a failed load with empty ownership.
        // Keep a non-dirty sentinel instead: refuse edits and leave the original file untouched.
        owners.clear();
        rejected = true;
        com.mojang.logging.LogUtils.getLogger().error("Rejected substrate ownership data: {}; edits disabled", reason);
        return this;
    }
}
