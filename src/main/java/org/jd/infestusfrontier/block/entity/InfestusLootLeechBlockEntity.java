package org.jd.infestusfrontier.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jd.infestusfrontier.ZgBlockEntities;
import org.jd.infestusfrontier.block.entity.util.InfestusLootLeech;

public class InfestusLootLeechBlockEntity extends BlockEntity implements InfestusLootLeech {
    private NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);

    public InfestusLootLeechBlockEntity( BlockPos pos, BlockState state) {
        super(ZgBlockEntities.LOOT_LEECH_ENTITY.get(), pos, state);
    }

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {

        return items.isEmpty();
    }

    @Override
    public ItemStack getItem(int index) {
        return items.get(index);
    }

    @Override
    public ItemStack removeItem(int index, int amount) {
        return ContainerHelper.removeItem(this.getItems(), index, amount);
    }
    public NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        return items.remove(index);
    }

    @Override
    public void setItem(int index, ItemStack itemStack) {
        if (canMergeItems(index, itemStack) || isEmpty()) {
            items.set(index, itemStack);
        }
    }
    public boolean canMergeItems(int index,ItemStack stack) {
        return items.get(index)==stack;
    }


    @Override
    public boolean stillValid(Player p_18946_) {
        return true;
    }

    @Override
    public void clearContent() {

    }

    @Override
    public double getLevelX() {
        return this.worldPosition.getX();
    }

    @Override
    public double getLevelY() {
        return this.worldPosition.getY();
    }

    @Override
    public double getLevelZ() {
        return this.worldPosition.getZ();
    }
}
