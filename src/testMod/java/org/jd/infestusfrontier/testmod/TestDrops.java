package org.jd.infestusfrontier.testmod;

import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/** Breaking blocks as a survival player and counting what falls, for tests of harvest conservation. */
final class TestDrops {
    private TestDrops() {}

    /** A survival player standing at the test's (3, 1, 1) with {@code tool} in the main hand. */
    static ServerPlayer miner(GameTestHelper h, ItemStack tool) {
        ServerPlayer player = new ServerPlayer(
                h.getLevel().getServer(),
                h.getLevel(),
                new GameProfile(UUID.randomUUID(), "gardener"),
                ClientInformation.createDefault());
        player.setGameMode(GameType.SURVIVAL);
        Vec3 feet = h.absolutePos(new BlockPos(3, 1, 1)).getCenter().add(0, -0.5, 0);
        player.moveTo(feet.x, feet.y, feet.z, 0F, 0F);
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        return player;
    }

    /** What lies on the ground inside the test's bounds, summed per item and ordered by registry key. */
    static Map<Item, Integer> dropped(GameTestHelper h) {
        Map<Item, Integer> sums = sorted();
        for (ItemEntity drop :
                h.getLevel().getEntitiesOfClass(ItemEntity.class, h.getBounds().inflate(1)))
            sums.merge(drop.getItem().getItem(), drop.getItem().getCount(), Integer::sum);
        return sums;
    }

    /** The non-empty stacks of {@code drops}, summed per item and ordered by registry key. */
    static Map<Item, Integer> describe(List<ItemStack> drops) {
        Map<Item, Integer> sums = sorted();
        for (ItemStack drop : drops) if (!drop.isEmpty()) sums.merge(drop.getItem(), drop.getCount(), Integer::sum);
        return sums;
    }

    /** Discards the items lying inside the test's bounds. */
    static void clearDrops(GameTestHelper h) {
        h.getLevel()
                .getEntitiesOfClass(ItemEntity.class, h.getBounds().inflate(1))
                .forEach(ItemEntity::discard);
    }

    private static Map<Item, Integer> sorted() {
        return new TreeMap<>((a, b) -> BuiltInRegistries.ITEM.getKey(a).compareTo(BuiltInRegistries.ITEM.getKey(b)));
    }
}
