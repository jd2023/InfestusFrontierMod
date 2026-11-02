package org.jd.infestusfrontier.storage;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import org.jd.infestusfrontier.integration.geckolib.OrganItem;
import org.jd.infestusfrontier.storage.api.CapsuleStock;

/**
 * A pod as an item, bulk, Mixed Craw or Larder. It carries the compartments, grade, labels and reserves it was broken
 * with, one to a stack, lists them in its tooltip, fits in no container item, and spills its items in whole stacks when the item itself is destroyed, as a
 * shulker box does.
 */
final class ItemCapsuleItem extends OrganItem {
    ItemCapsuleItem(Block block, Properties properties) {
        super(block, properties, "idle", "grade_0");
    }

    @Override
    public boolean canFitInsideContainerItems() {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        CapsuleStock carried = stack.get(StorageModule.CAPSULE_STOCK.get());
        if (carried == null) return;
        for (ItemStack held : carried.held())
            if (!held.isEmpty())
                tooltip.add(Component.translatable(
                                "tooltip.infestusfrontier.item_capsule.held", held.getCount(), held.getHoverName())
                        .withStyle(ChatFormatting.GRAY));
        for (int slot = 0; slot < carried.marks().size(); slot++) {
            CapsuleStock.Mark mark = carried.mark(slot);
            if (mark.isNone()) continue;
            tooltip.add((mark.label().isEmpty()
                            ? Component.translatable("tooltip.infestusfrontier.larder.unlabelled", mark.reserve())
                            : Component.translatable(
                                    "tooltip.infestusfrontier.larder.label",
                                    mark.label().getHoverName(),
                                    mark.reserve()))
                    .withStyle(ChatFormatting.GRAY));
        }
        if (carried.grade() > 0)
            tooltip.add(Component.translatable(
                            switch (carried.kind()) {
                                case BULK -> "tooltip.infestusfrontier.item_capsule.grade";
                                case CRAW -> "tooltip.infestusfrontier.mixed_craw.grade";
                                case LARDER -> "tooltip.infestusfrontier.larder.grade";
                            },
                            carried.kind().stacks(carried.grade()))
                    .withStyle(ChatFormatting.GRAY));
    }

    @Override
    public void onDestroyed(ItemEntity entity) {
        CapsuleStock carried = entity.getItem().set(StorageModule.CAPSULE_STOCK.get(), CapsuleStock.EMPTY);
        if (carried == null) return;
        List<ItemStack> stacks = new ArrayList<>();
        for (int item = 0; item < entity.getItem().getCount(); item++)
            for (ItemStack held : carried.held())
                while (!held.isEmpty()) stacks.add(held.split(held.getMaxStackSize()));
        ItemUtils.onContainerDestroyed(entity, stacks);
    }
}
