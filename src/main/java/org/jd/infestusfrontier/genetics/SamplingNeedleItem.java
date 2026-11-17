package org.jd.infestusfrontier.genetics;

import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jd.infestusfrontier.core.foundation.Refusal;
import org.jd.infestusfrontier.core.genetics.Genetics;
import org.jd.infestusfrontier.foundation.Refusals;
import org.jd.infestusfrontier.genetics.api.DnaSampleRecipe;

/**
 * A hand tool for DNA. Used on a living creature that a {@link DnaSampleRecipe} names, it puts one sample of that
 * species in the player's inventory, wears by one and rests for {@value Genetics#SAMPLE_COOLDOWN_TICKS} ticks. The
 * creature is not harmed. A creature no recipe names, or an inventory with no room for the sample, is refused with the
 * reason, and the needle takes nothing and keeps its wear.
 */
public final class SamplingNeedleItem extends Item {
    SamplingNeedleItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(
            ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!target.isAlive() || player.getCooldowns().isOnCooldown(this)) return InteractionResult.PASS;
        Level level = player.level();
        if (level.isClientSide) return InteractionResult.SUCCESS;
        Optional<Refusal> refused = sample(level, stack, player, target, hand);
        refused.ifPresent(why -> Refusals.show(player, why));
        return InteractionResult.CONSUME;
    }

    /** Takes one sample from {@code target} into {@code player}'s inventory, or names why it took nothing. */
    private Optional<Refusal> sample(
            Level level, ItemStack needle, Player player, LivingEntity target, InteractionHand hand) {
        Optional<DnaSampleRecipe> recipe = DnaSampleRecipe.forCreature(level, target.getType());
        if (recipe.isEmpty()) return Optional.of(Genetics.NO_DNA);
        ItemStack sample = recipe.get().sample();
        Inventory inventory = player.getInventory();
        if (inventory.getSlotWithRemainingSpace(sample) < 0 && inventory.getFreeSlot() < 0)
            return Optional.of(Genetics.NO_ROOM);
        inventory.add(sample.copy()); // one item, and there is room for it
        needle.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
        player.getCooldowns().addCooldown(this, Genetics.SAMPLE_COOLDOWN_TICKS);
        level.playSound(null, target.blockPosition(), SoundEvents.BEEHIVE_SHEAR, SoundSource.PLAYERS, 0.8F, 1.4F);
        player.displayClientMessage(
                Component.translatable("infestusfrontier.sampling_needle.sampled", sample.getHoverName()), true);
        return Optional.empty();
    }
}
