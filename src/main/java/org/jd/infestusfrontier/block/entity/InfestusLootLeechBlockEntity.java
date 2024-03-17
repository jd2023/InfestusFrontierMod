package org.jd.infestusfrontier.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jd.infestusfrontier.ZgBlockEntities;
import org.jd.infestusfrontier.block.InfesusLootLeechBlock;
import org.jd.infestusfrontier.block.entity.util.InfestusLootLeech;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class InfestusLootLeechBlockEntity extends BlockEntity implements InfestusLootLeech {
    private NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);

    public InfestusLootLeechBlockEntity( BlockPos pos, BlockState state) {
        super(ZgBlockEntities.LOOT_LEECH_ENTITY.get(), pos, state);
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
    public static void pushItemsTick(Level level, BlockPos pos, BlockState state, InfestusLootLeechBlockEntity leech) {
        tryMoveItems(level, pos, state, leech, () -> {
            return suckInItems(level, leech);
        });
    }
    private static boolean tryMoveItems(Level level, BlockPos pos, BlockState state, InfestusLootLeechBlockEntity leech, BooleanSupplier bs) {
        if (level.isClientSide) {
            return false;
        } else {

            boolean flag = false;
            if (!leech.isEmpty()) {
                flag = ejectItems(level, pos, state, leech);
            }

            if (!leech.inventoryFull()) {
                flag |= bs.getAsBoolean();
            }

            if (flag) {
                setChanged(level, pos, state);
                return true;
            }


            return false;
        }
    }
    private static boolean ejectItems(Level level, BlockPos pos, BlockState state, InfestusLootLeechBlockEntity entity) {
        Container container = getDepositContainer(level, pos, state);
        if (container == null) {
            return false;
        } else {
            Direction direction = Direction.UP;
            System.out.println("Ejecting");
            if (isFullContainer(container, direction)) {
                return false;
            } else {
                for(int i = 0; i < entity.getContainerSize(); ++i) {
                    if (!entity.getItem(i).isEmpty()) {
                        ItemStack itemstack = entity.getItem(i).copy();
                        ItemStack itemstack1 = addItem(entity, container, entity.removeItem(i, 1), direction);
                        if (itemstack1.isEmpty()) {
                            container.setChanged();
                            return true;
                        }

                        entity.setItem(i, itemstack);
                    }
                }

                return false;
            }
        }
    }
    private static boolean isFullContainer(Container container, Direction direction) {
        return getSlots(container, direction).allMatch((p_59379_) -> {
            ItemStack itemstack = container.getItem(p_59379_);
            return itemstack.getCount() >= itemstack.getMaxStackSize();
        });
    }

    @Override
    public void setItem(int index, ItemStack itemStack) {
        if (canMergeItems(items.get(index), itemStack) || isEmpty()) {
            items.set(index, itemStack);
        }
    }
    private static boolean canMergeItems(ItemStack stack, ItemStack stack2) {
        if (!stack.is(stack2.getItem())) {
            return false;
        } else if (stack.getDamageValue() != stack2.getDamageValue()) {
            return false;
        } else if (stack.getCount() > stack.getMaxStackSize()) {
            return false;
        } else {
            return ItemStack.tagMatches(stack, stack2);
        }
    }
    private static IntStream getSlots(Container container, Direction direction) {
        return container instanceof WorldlyContainer ? IntStream.of(((WorldlyContainer)container).getSlotsForFace(direction)) : IntStream.range(0, container.getContainerSize());
    }

    public static boolean suckInItems(Level level, InfestusLootLeech leech) {

        for(ItemEntity itementity : getItemsAtAndAbove(level, leech)) {
            if (addItem(leech, itementity)) {
                System.out.println("Sucking");
                return true;
            }
        }

        return false;


    }
    public static boolean addItem(Container container, ItemEntity entity) {
        System.out.println("Adding Item");
        boolean flag = false;
        ItemStack itemstack = entity.getItem().copy();
        ItemStack itemstack1 = addItem((Container)null, container, itemstack, (Direction)null);
        if (itemstack1.isEmpty()) {
            flag = true;
            entity.discard();
        } else {
            entity.setItem(itemstack1);
        }

        return flag;
    }
    public static ItemStack addItem(@Nullable Container container, Container container2, ItemStack stack, @Nullable Direction direction) {
        System.out.println("Adding Item");

        if (container2 instanceof WorldlyContainer worldlycontainer && direction != null) {
            int[] aint = worldlycontainer.getSlotsForFace(direction);

            for(int k = 0; k < aint.length && !stack.isEmpty(); ++k) {
                stack = tryMoveInItem(container, container2, stack, aint[k], direction);
            }
        } else {
            int i = container2.getContainerSize();

            for(int j = 0; j < i && !stack.isEmpty(); ++j) {
                stack = tryMoveInItem(container, container2, stack, j, direction);
            }
        }

        return stack;
    }
    private static boolean canPlaceItemInContainer(Container container, ItemStack stack, int index, @Nullable Direction direction) {
        if (!container.canPlaceItem(index, stack)) {
            return false;
        } else {
            return !(container instanceof WorldlyContainer) || ((WorldlyContainer)container).canPlaceItemThroughFace(index, stack, direction);
        }
    }
    private static ItemStack tryMoveInItem(@Nullable Container container, Container container2, ItemStack stack, int index, @Nullable Direction direction) {
        ItemStack itemstack = container2.getItem(index);
        if (canPlaceItemInContainer(container2, stack, index, direction)) {
            boolean flag = false;
            boolean flag1 = container2.isEmpty();
            if (itemstack.isEmpty()) {
                container2.setItem(index, stack);
                stack = ItemStack.EMPTY;
                flag = true;
            } else if (canMergeItems(itemstack, stack)) {
                int i = stack.getMaxStackSize() - itemstack.getCount();
                int j = Math.min(stack.getCount(), i);
                stack.shrink(j);
                itemstack.grow(j);
                flag = j > 0;
            }

            if (flag) {
                container2.setChanged();
            }
        }

        return stack;
    }
    @Override
    public int getContainerSize() {
        return items.size();
    }



    public static List<ItemEntity> getItemsAtAndAbove(Level level, InfestusLootLeech leech) {
        return leech.getSuckShape().toAabbs().stream().flatMap((p_155558_) -> {
            return level.getEntitiesOfClass(ItemEntity.class, p_155558_.move(leech.getLevelX() - 0.5D, leech.getLevelY() - 0.5D, leech.getLevelZ() - 0.5D), EntitySelector.ENTITY_STILL_ALIVE).stream();
        }).collect(Collectors.toList());
    }

    @Nullable
    private static Container getDepositContainer(Level p_155593_, BlockPos p_155594_, BlockState p_155595_) {
        Direction direction = Direction.UP;
        return getContainerAt(p_155593_, p_155594_.relative(direction));
    }
    @Nullable
    public static Container getContainerAt(Level level, BlockPos pos) {
        return getContainerAt(level, (double)pos.getX() + 0.5D, (double)pos.getY() + 0.5D, (double)pos.getZ() + 0.5D);
    }

    @Nullable
    private static Container getContainerAt(Level level, double x, double y, double z) {
        Container container = null;
        BlockPos blockpos = new BlockPos(x, y, z);
        BlockState blockstate = level.getBlockState(blockpos);
        Block block = blockstate.getBlock();
        if (block instanceof WorldlyContainerHolder) {
            container = ((WorldlyContainerHolder) block).getContainer(blockstate, level, blockpos);
        } else if (blockstate.hasBlockEntity()) {
            BlockEntity blockentity = level.getBlockEntity(blockpos);
            if (blockentity instanceof Container) {
                container = (Container) blockentity;
                if (container instanceof ChestBlockEntity && block instanceof ChestBlock) {
                    container = ChestBlock.getContainer((ChestBlock) block, blockstate, level, blockpos, true);
                }
            }
        }
        if (container == null) {
            List<Entity> list = level.getEntities((Entity)null, new AABB(x - 0.5D, y - 0.5D, z - 0.5D, x + 0.5D, y + 0.5D, z + 0.5D), EntitySelector.CONTAINER_ENTITY_SELECTOR);
            if (!list.isEmpty()) {
                container = (Container)list.get(level.random.nextInt(list.size()));
            }
        }

        return container;
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
    public void drops() {
        SimpleContainer inventory = new SimpleContainer(items.size());
        for (int i = 0; i < items.size(); i++) {
            inventory.setItem(i, items.get(i));
        }
        Containers.dropContents(this.level, this.worldPosition, inventory);
    }
    private boolean inventoryFull() {
        for(ItemStack itemstack : this.items) {
            if (itemstack.isEmpty() || itemstack.getCount() != itemstack.getMaxStackSize()) {
                return false;
            }
        }

        return true;
    }
}
