package org.jd.infestusfrontier.construction;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jd.infestusfrontier.construction.api.SeedPouch;

final class SeedPouchEntity extends BlockEntity {
    private SeedPouch pouch = new SeedPouch();
    private CompoundTag rejected;

    boolean rejected() { return rejected != null; }
    int reserve() { return pouch.reserve(); }
    void adjustReserve(boolean decrease) {
        if (rejected != null) return;
        pouch.setReserve(Math.floorMod(pouch.reserve() + (decrease ? -1 : 1), SeedPouch.COUNT_CAPACITY + 1));
        setChanged();
    }

    SeedPouchEntity(BlockPos pos, BlockState state, Supplier<BlockEntityType<SeedPouchEntity>> type) {
        super(type.get(), pos, state);
    }

    int insert(ItemStack stack) {
        if (rejected != null || !stack.is(ConstructionTags.SEED_STOCK)
                || !stack.getComponentsPatch().isEmpty()) return 0;
        String resource = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        int accepted = pouch.insert(resource, stack.getCount()).accepted();
        if (accepted > 0) setChanged();
        return accepted;
    }

    ItemStack take(boolean includeReserve) {
        if (rejected != null) return ItemStack.EMPTY;
        for (var entry : pouch.snapshot().entrySet()) {
            int amount = includeReserve ? pouch.takeAll(entry.getKey()) : pouch.takeSurplus(entry.getKey(), 64);
            if (amount > 0) {
                setChanged();
                var item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(entry.getKey())).orElseThrow();
                return new ItemStack(item, amount);
            }
        }
        return ItemStack.EMPTY;
    }

    void dropContents(Level level, BlockPos pos) {
        if (rejected != null) return;
        for (var entry : pouch.snapshot().entrySet()) {
            int amount = pouch.takeAll(entry.getKey());
            var id = ResourceLocation.tryParse(entry.getKey());
            var item = id == null ? null : BuiltInRegistries.ITEM.getOptional(id).orElse(null);
            if (item != null && amount > 0) level.addFreshEntity(new ItemEntity(level,
                    pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, new ItemStack(item, amount)));
        }
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (rejected != null) { tag.merge(rejected); return; }
        tag.putInt("schema", 1);
        tag.putInt("reserve", pouch.reserve());
        var stocks = new ListTag();
        pouch.snapshot().forEach((resource, count) -> {
            var stock = new CompoundTag();
            stock.putString("resource", resource); stock.putInt("count", count); stocks.add(stock);
        });
        tag.put("stocks", stocks);
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        try {
            pouch = restore(tag);
            rejected = null;
        } catch (IllegalArgumentException invalid) {
            rejected = tag.copy();
            for (String metadata : java.util.List.of("id", "x", "y", "z")) rejected.remove(metadata);
            pouch = new SeedPouch();
        }
    }

    private static SeedPouch restore(CompoundTag tag) {
        if (!tag.contains("schema", Tag.TAG_INT) || tag.getInt("schema") != 1
                || !(tag.get("stocks") instanceof ListTag stocks)
                || stocks.size() > SeedPouch.TYPE_CAPACITY
                || !stocks.isEmpty() && stocks.getElementType() != Tag.TAG_COMPOUND) {
            throw new IllegalArgumentException("Invalid Seed Pouch schema or stocks");
        }
        var restored = new SeedPouch();
        if (tag.contains("reserve") && (!tag.contains("reserve", Tag.TAG_INT)
                || !restored.setReserve(tag.getInt("reserve")))) {
            throw new IllegalArgumentException("Invalid planting reserve");
        }
        for (int i = 0; i < stocks.size(); i++) {
            var stock = stocks.getCompound(i);
            String resource = stock.getString("resource");
            int count = stock.getInt("count");
            ResourceLocation id = ResourceLocation.tryParse(resource);
            if (!stock.contains("resource", Tag.TAG_STRING) || !stock.contains("count", Tag.TAG_INT)
                    || id == null || !id.toString().equals(resource) || !BuiltInRegistries.ITEM.containsKey(id) || count < 1
                    || count > SeedPouch.COUNT_CAPACITY || restored.count(resource) != 0
                    || !new ItemStack(BuiltInRegistries.ITEM.get(id)).is(ConstructionTags.SEED_STOCK)) {
                throw new IllegalArgumentException("Invalid or duplicate planting stock");
            }
            if (restored.insert(resource, count).accepted() != count) {
                throw new IllegalArgumentException("Planting stock exceeds store bounds");
            }
        }
        return restored;
    }
}
