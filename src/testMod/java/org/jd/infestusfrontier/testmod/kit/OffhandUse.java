package org.jd.infestusfrontier.testmod.kit;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

public final class OffhandUse {
    private OffhandUse() {}

    public static InteractionResult useWithOffhand(GameTestHelper helper, BlockPos relative, ItemStack offhand) {
        var player = FakePlayerFactory.getMinecraft(helper.getLevel());
        player.setGameMode(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.setItemInHand(InteractionHand.OFF_HAND, offhand);
        var pos = helper.absolutePos(relative);
        var hit = new BlockHitResult(Vec3.atCenterOf(pos).add(0.5, 0, 0), Direction.EAST, pos, false);
        return player.gameMode.useItemOn(player, helper.getLevel(), player.getOffhandItem(), InteractionHand.OFF_HAND, hit);
    }

    public static void assertPlacedBeside(GameTestHelper helper, BlockPos relative, Block expected) {
        var pos = helper.absolutePos(relative);
        for (var direction : Direction.values()) {
            if (helper.getLevel().getBlockState(pos.relative(direction)).is(expected)) return;
        }
        helper.fail("Offhand block was not placed beside the target");
    }
}
