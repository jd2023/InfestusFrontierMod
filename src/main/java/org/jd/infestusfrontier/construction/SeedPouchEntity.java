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

    SeedPouchEntity(BlockPos pos, BlockState state, Supplier<BlockEntityType<SeedPouchEntity>> type) {
        super(type.get(), pos, state);
    }

    int insert(ItemStack stack) {
        String resource = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        int accepted = pouch.insert(resource, stack.getCount()).accepted();
        if (accepted > 0) setChanged();
        return accepted;
    }

    ItemStack take(boolean includeReserve) {
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
        tag.putInt("schema", 1);
        var stocks = new ListTag();
        pouch.snapshot().forEach((resource, count) -> {
            var stock = new CompoundTag();
            stock.putString("resource", resource); stock.putInt("count", count); stocks.add(stock);
        });
        tag.put("stocks", stocks);
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        var restored = new SeedPouch();
        if (tag.getInt("schema") == 1 && tag.contains("stocks", Tag.TAG_LIST)) {
            var stocks = tag.getList("stocks", Tag.TAG_COMPOUND);
            if (stocks.size() <= SeedPouch.TYPE_CAPACITY) for (int i = 0; i < stocks.size(); i++) {
                var stock = stocks.getCompound(i);
                String resource = stock.getString("resource"); int count = stock.getInt("count");
                ResourceLocation id = ResourceLocation.tryParse(resource);
                if (id != null && BuiltInRegistries.ITEM.containsKey(id) && count > 0
                        && count <= SeedPouch.COUNT_CAPACITY
                        && new ItemStack(BuiltInRegistries.ITEM.get(id)).is(ConstructionTags.SEED_STOCK)) {
                    restored.insert(resource, count);
                }
            }
        }
        pouch = restored;
    }
}
