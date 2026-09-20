package org.jd.infestusfrontier.construction;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jd.infestusfrontier.construction.api.BudConstructionPort;
import org.jd.infestusfrontier.construction.api.BudConstructionRecipe;
import org.jd.infestusfrontier.construction.api.BudConstructionResult;
import org.jd.infestusfrontier.construction.api.BudRecipeRegistrar;
import org.jd.infestusfrontier.foundation.TickQuota;

final class BudRecipeRegistry implements BudRecipeRegistrar {
    private static final int PLACEMENTS_PER_SERVER_TICK = 16;

    private final List<BudConstructionRecipe> recipes = new ArrayList<>();
    private final TickQuota placementQuota = new TickQuota(PLACEMENTS_PER_SERVER_TICK);
    private BudConstructionPort port = new BudConstructionPort(List.of());
    private boolean sealed;

    @Override
    public synchronized void register(BudConstructionRecipe recipe) {
        if (sealed) throw new IllegalStateException("Bud construction recipes are sealed after first use");
        if (recipe.output().equals("infestusfrontier:" + ConstructionContent.ORGAN_BUD)) {
            throw new IllegalArgumentException("A bud construction recipe must replace the bud");
        }
        var candidate = new ArrayList<>(recipes);
        candidate.add(recipe);
        port = new BudConstructionPort(candidate);
        recipes.add(recipe);
    }

    boolean supports(String heldIngredient) {
        return sealedPort().supports(heldIngredient);
    }

    BudConstructionResult apply(Level level, BlockPos pos, Block bud, Player player, ItemStack held) {
        if (!(level instanceof ServerLevel serverLevel)) return BudConstructionResult.UNAVAILABLE;
        String heldIngredient = BuiltInRegistries.ITEM.getKey(held.getItem()).toString();
        var stock = new PlayerStock(player);
        var site = new LevelSite(serverLevel, pos, bud);
        return sealedPort().apply(
                heldIngredient,
                stock,
                site,
                () -> placementQuota.take(level.getServer().getTickCount()));
    }

    private synchronized BudConstructionPort sealedPort() {
        sealed = true;
        return port;
    }

    private static final class PlayerStock implements BudConstructionPort.Stock {
        private final Player player;
        private final Inventory inventory;

        private PlayerStock(Player player) {
            this.player = player;
            inventory = player.getInventory();
        }

        @Override
        public int count(String ingredient) {
            ResourceLocation id = ResourceLocation.tryParse(ingredient);
            if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) return 0;
            var item = BuiltInRegistries.ITEM.getOptional(id).orElseThrow();
            int count = 0;
            for (ItemStack stack : inventory.items) {
                if (stack.is(item)) count += stack.getCount();
            }
            for (ItemStack stack : inventory.offhand) {
                if (stack.is(item)) count += stack.getCount();
            }
            return count;
        }

        @Override
        public void consume(java.util.Map<String, Integer> costs) {
            costs.forEach((ingredient, required) -> consume(ingredient, required));
        }

        private void consume(String ingredient, int required) {
            var item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(ingredient)).orElseThrow();
            int remaining = consumeFrom(inventory.items, item, required);
            consumeFrom(inventory.offhand, item, remaining);
        }

        private int consumeFrom(List<ItemStack> stacks, net.minecraft.world.item.Item item, int required) {
            int remaining = required;
            for (ItemStack stack : stacks) {
                if (remaining == 0) break;
                if (!stack.is(item)) continue;
                int amount = Math.min(remaining, stack.getCount());
                stack.consume(amount, player);
                remaining -= amount;
            }
            return remaining;
        }
    }

    private static final class LevelSite implements BudConstructionPort.Site {
        private final ServerLevel level;
        private final BlockPos pos;
        private final Block bud;

        private LevelSite(ServerLevel level, BlockPos pos, Block bud) {
            this.level = level;
            this.pos = pos;
            this.bud = bud;
        }

        @Override
        public boolean isBud() {
            var chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
            return chunk != null && chunk.getBlockState(pos).is(bud);
        }

        @Override
        public boolean replace(String output) {
            ResourceLocation id = ResourceLocation.tryParse(output);
            if (id == null || !BuiltInRegistries.BLOCK.containsKey(id)) return false;
            Block replacement = BuiltInRegistries.BLOCK.getOptional(id).orElseThrow();
            if (replacement == bud || !isBud() || !updateAreaLoaded()) return false;
            var replacementState = replacement.defaultBlockState();
            if (!replacementState.canSurvive(level, pos)) return false;
            // Construction installs one body, without vanilla neighbor/shape cascades. Output owners
            // must keep their own onPlace/onBlockStateChange callbacks bounded and loaded-only.
            return level.setBlock(pos, replacementState,
                    Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE, 0);
        }

        private boolean updateAreaLoaded() {
            // A one-block halo intersects at most four chunks, including diagonal boundaries.
            for (int x = (pos.getX() - 1) >> 4; x <= (pos.getX() + 1) >> 4; x++) {
                for (int z = (pos.getZ() - 1) >> 4; z <= (pos.getZ() + 1) >> 4; z++) {
                    if (level.getChunkSource().getChunkNow(x, z) == null) return false;
                }
            }
            return true;
        }
    }
}
