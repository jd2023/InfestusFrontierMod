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
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

final class SubstrateOwnership extends SavedData {
    private static final String DATA_NAME = "infestusfrontier_ecology_cells";
    private static final int MAX_CELLS_PER_DIMENSION = 65_536;
    private static final Factory<SubstrateOwnership> FACTORY = new Factory<>(
            SubstrateOwnership::new,
            SubstrateOwnership::load,
            DataFixTypes.LEVEL);

    private final Map<Long, UUID> owners = new HashMap<>();

    static SubstrateOwnership get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    boolean canClaim(BlockPos pos) {
        return owners.containsKey(pos.asLong()) || owners.size() < MAX_CELLS_PER_DIMENSION;
    }

    boolean claim(BlockPos pos, UUID owner) {
        long key = pos.asLong();
        if (!owners.containsKey(key) && owners.size() >= MAX_CELLS_PER_DIMENSION) return false;
        if (!owner.equals(owners.put(key, owner))) setDirty();
        return true;
    }

    boolean permits(BlockPos pos, UUID player) {
        UUID owner = owners.get(pos.asLong());
        return owner == null || owner.equals(player);
    }

    Optional<UUID> owner(BlockPos pos) {
        return Optional.ofNullable(owners.get(pos.asLong()));
    }

    void remove(BlockPos pos) {
        if (owners.remove(pos.asLong()) != null) setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
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
        if (tag.getInt("schema") != 1) return data;
        ListTag cells = tag.getList("cells", Tag.TAG_COMPOUND);
        int count = Math.min(cells.size(), MAX_CELLS_PER_DIMENSION);
        for (int index = 0; index < count; index++) {
            CompoundTag cell = cells.getCompound(index);
            if (cell.hasUUID("owner")) data.owners.put(cell.getLong("position"), cell.getUUID("owner"));
        }
        return data;
    }
}
