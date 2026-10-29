package org.jd.infestusfrontier.equipment;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jd.infestusfrontier.core.equipment.LivingTool;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.core.foundation.Outcome;
import org.jd.infestusfrontier.core.storage.BiomassTank;
import org.jd.infestusfrontier.equipment.api.Reserves;

/**
 * What every living tool does besides its vanilla work: it shows its reserve and its branch, mends itself from the
 * reserve once a second while a player carries it, and, grafted Contractile, breaks the blocks around the one it
 * mines through the player's own block breaking, each paid from the reserve; a fang grafted Grasping grips what it
 * hits.
 */
final class LivingTools {
    /** A carried tool mends on the ticks that are a multiple of this. */
    static final int MEND_TICKS = 20;
    /** True while a spread is breaking its neighbours, so their breaking does not spread again. */
    private static final ThreadLocal<Boolean> SPREADING = ThreadLocal.withInitial(() -> false);

    private LivingTools() {}

    static void describe(ItemStack stack, List<Component> tooltip) {
        Reserves.of(stack)
                .ifPresent(reserve -> tooltip.add(Component.translatable(
                        "item.infestusfrontier.awakened.reserve",
                        reserve.stored().units(),
                        reserve.capacity().units())));
        ToolMutation mutation = stack.get(EquipmentModule.TOOL_MUTATION.get());
        if (mutation != null) tooltip.add(Component.translatable(mutation.translationKey()));
    }

    /** Once a second, a worn tool in a player's inventory mends up to {@link LivingTool#MEND_PER_SECOND} points. */
    static void mend(ItemStack stack, Level level, Entity carrier) {
        if (level.isClientSide
                || !(carrier instanceof Player)
                || level.getGameTime() % MEND_TICKS != 0
                || !stack.isDamaged()) return;
        Optional<BiomassTank> reserve = Reserves.of(stack);
        if (reserve.isEmpty()) return;
        int points =
                LivingTool.mend(stack.getDamageValue(), reserve.get().stored().units());
        if (points == 0 || !spend(stack, Biomass.of(points))) return;
        stack.setDamageValue(stack.getDamageValue() - points);
    }

    /**
     * A Contractile tool, having mined {@code pos}, breaks the blocks of the 3x3 face round it that it is the tool
     * for, as its player would break them one by one (drops, wear, events and protections included), while its
     * reserve pays {@link LivingTool#SPREAD_COST} BU for each. Nothing spreads from those breaks.
     */
    static void spread(ItemStack stack, Level level, BlockPos pos, BlockState mined, LivingEntity miner) {
        if (level.isClientSide
                || SPREADING.get()
                || !stack.isCorrectToolForDrops(mined)
                || stack.getDestroySpeed(mined) <= 1.0F
                || !(miner instanceof ServerPlayer player)
                || stack.get(EquipmentModule.TOOL_MUTATION.get()) != ToolMutation.CONTRACTILE) return;
        Optional<BiomassTank> reserve = Reserves.of(stack);
        if (reserve.isEmpty()) return;
        int left = LivingTool.spreads(reserve.get().stored().units());
        if (left == 0) return;
        SPREADING.set(true);
        try {
            for (BlockPos next : around(pos, facing(player, pos))) {
                if (left == 0 || stack.isEmpty() || player.getMainHandItem() != stack) return;
                if (!level.isLoaded(next)) continue;
                BlockState state = level.getBlockState(next);
                if (state.isAir()
                        || state.getDestroySpeed(level, next) < 0
                        || !stack.isCorrectToolForDrops(state)
                        || stack.getDestroySpeed(state) <= 1.0F) continue;
                if (!player.gameMode.destroyBlock(next)) continue;
                if (!spend(stack, Biomass.of(LivingTool.SPREAD_COST))) return;
                left--;
            }
        } finally {
            SPREADING.set(false);
        }
    }

    /**
     * A Grasping fang that has hurt {@code target} grips it: Slowness {@link LivingTool#GRIP_LEVEL} for
     * {@link LivingTool#GRIP_TICKS} ticks, {@link LivingTool#GRIP_COST} BU from the reserve; a dry fang only cuts.
     */
    static void grip(ItemStack stack, LivingEntity target) {
        if (target.level().isClientSide || stack.get(EquipmentModule.TOOL_MUTATION.get()) != ToolMutation.GRASPING)
            return;
        Optional<BiomassTank> reserve = Reserves.of(stack);
        if (reserve.isEmpty() || !LivingTool.grips(reserve.get().stored().units())) return;
        if (!spend(stack, Biomass.of(LivingTool.GRIP_COST))) return;
        target.addEffect(
                new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, LivingTool.GRIP_TICKS, LivingTool.GRIP_LEVEL));
    }

    /** Takes {@code amount} from the tool's reserve; false, taking nothing, when it has less. */
    private static boolean spend(ItemStack stack, Biomass amount) {
        Optional<BiomassTank> reserve = Reserves.of(stack);
        if (reserve.isEmpty() || reserve.get().stored().units() < amount.units()) return false;
        if (!(reserve.get().drain(amount) instanceof Outcome.Done<BiomassTank> drained)) return false;
        stack.set(EquipmentModule.RESERVE.get(), drained.value());
        return true;
    }

    /** The face of {@code pos} the player struck: what they look at, or the side facing them when the look misses. */
    private static Direction facing(ServerPlayer player, BlockPos pos) {
        HitResult hit = player.pick(player.blockInteractionRange() + 1, 0, false);
        if (hit instanceof BlockHitResult block && block.getBlockPos().equals(pos)) return block.getDirection();
        return Direction.getNearest(player.getViewVector(1)).getOpposite();
    }

    /** The eight positions round {@code pos} in the plane of {@code face}. */
    private static List<BlockPos> around(BlockPos pos, Direction face) {
        List<BlockPos> out = new ArrayList<>(LivingTool.SPREAD);
        for (int a = -1; a <= 1; a++)
            for (int b = -1; b <= 1; b++) {
                if (a == 0 && b == 0) continue;
                out.add(
                        switch (face.getAxis()) {
                            case Y -> pos.offset(a, 0, b);
                            case X -> pos.offset(0, a, b);
                            case Z -> pos.offset(a, b, 0);
                        });
            }
        return out;
    }
}
