package org.jd.infestusfrontier.testmod.discovery.client;

import com.klikli_dev.modonomicon.book.page.BookRecipePage;
import com.klikli_dev.modonomicon.client.gui.BookGuiManager;
import com.klikli_dev.modonomicon.data.BookDataManager;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jd.infestusfrontier.discovery.GuideAccess;

/** Real inventory clicks and block-use packets; setup supplies only ordinary starter materials. */
final class SurvivalDiscoveryScenario {
    private static final BlockPos SUPPORT = new BlockPos(2, -61, 3);
    private int stage;
    private int ticks;
    private int culturesCrafted;
    private int readingTicks;
    private Item expectedCraft;
    private int expectedCount;
    private boolean announced;
    private boolean keyClicked;
    private boolean collecting;
    private final java.util.Set<ResourceLocation> earned = new java.util.HashSet<>();
    private final net.minecraft.client.multiplayer.ClientAdvancements.Listener progress = new net.minecraft.client.multiplayer.ClientAdvancements.Listener() {
        public void onUpdateAdvancementProgress(net.minecraft.advancements.AdvancementNode node, net.minecraft.advancements.AdvancementProgress value) {
            if (value.isDone()) earned.add(node.holder().id()); else earned.remove(node.holder().id());
        }
        public void onSelectedTabChanged(net.minecraft.advancements.AdvancementHolder value) {}
        public void onAddAdvancementRoot(net.minecraft.advancements.AdvancementNode value) {}
        public void onRemoveAdvancementRoot(net.minecraft.advancements.AdvancementNode value) { earned.remove(value.holder().id()); }
        public void onAddAdvancementTask(net.minecraft.advancements.AdvancementNode value) {}
        public void onRemoveAdvancementTask(net.minecraft.advancements.AdvancementNode value) { earned.remove(value.holder().id()); }
        public void onAdvancementsCleared() { earned.clear(); }
    };

    boolean tick(Minecraft game) {
        if (stage == 20) return true;
        if (++ticks > (stage == 12 ? 1500 : 400)) throw new IllegalStateException("Survival discovery timed out at " + stage);
        switch (stage) {
            case 0 -> {
                if (!announced) {
                    if (game.player.getInventory().items.stream().noneMatch(GuideAccess::isWakingGenome)) return false;
                    game.player.closeContainer();
                    game.getConnection().getAdvancements().setListener(progress);
                    LogUtils.getLogger().info("INFESTUS_DISCOVERY_READY");
                    announced = true;
                }
                var phase = Path.of(System.getenv("INFESTUS_CAPTURE_DIR"), "discovery-stage");
                try {
                    if (!Files.exists(phase)) return false;
                    if (Files.size(phase) > 8) throw new IllegalStateException("Oversized discovery phase");
                    if (!Files.readString(phase).equals("run")) return false;
                } catch (java.io.IOException exception) { throw new IllegalStateException(exception); }
                if (!game.gameMode.getPlayerMode().isSurvival() || count(game, Items.ROTTEN_FLESH) != 2
                        || count(game, Items.RED_MUSHROOM) != 3 || count(game, Items.WHEAT_SEEDS) != 4
                        || count(game, Items.BOOK) != 1 || count(game, Items.WATER_BUCKET) != 1) return false;
                require(!completed(game, "spore_culture") && !completed(game, "living_substrate")
                        && !completed(game, "culture_bowl_batch"), "Fresh Survival progress baseline");
                next();
            }
            case 1 -> {
                if (!keyClicked) {
                    var key = java.util.Arrays.stream(game.options.keyMappings)
                            .filter(k -> k.getName().equals("key.infestusfrontier.open_guide")).findFirst().orElseThrow();
                    var original = key.getKey();
                    var rebound = InputConstants.getKey(org.lwjgl.glfw.GLFW.GLFW_KEY_H, 0);
                    key.setKey(rebound);
                    KeyMapping.resetMapping();
                    KeyMapping.click(rebound);
                    key.setKey(original);
                    KeyMapping.resetMapping();
                    keyClicked = true;
                    return false;
                }
                if (game.screen == null || !game.screen.getClass().getName().startsWith("com.klikli_dev.modonomicon")) return false;
                LogUtils.getLogger().info("INFESTUS_DISCOVERY_KEY_WITHOUT_BOOK");
                next();
            }
            case 2 -> {
                if (!readPage(game, "construction/spore_culture")) return false;
                craftFromPage(game, "construction/spore_culture");
                next();
            }
            case 3 -> {
                if (!takeCraft(game)) return false;
                culturesCrafted++;
                if (culturesCrafted < 2) jump(2); else next();
            }
            case 4 -> {
                if (!completed(game, "spore_culture")) return false;
                if (!readPage(game, "ecology/living_substrate")) return false;
                use(game, item("construction/spore_culture"), SUPPORT);
                next();
            }
            case 5 -> {
                if (!game.level.getBlockState(SUPPORT).is(BuiltInRegistries.BLOCK.get(id("ecology/living_substrate")))) return false;
                if (!completed(game, "living_substrate")) return false;
                if (!readPage(game, "processing/culture_bowl")) return false;
                craftFromPage(game, "processing/culture_bowl");
                next();
            }
            case 6 -> { if (takeCraft(game)) next(); }
            case 7 -> { use(game, item("processing/culture_bowl"), SUPPORT); next(); }
            case 8 -> {
                if (!game.level.getBlockState(SUPPORT.above()).is(BuiltInRegistries.BLOCK.get(id("processing/culture_bowl")))) return false;
                // Read the synchronized renewal page before supplying its inputs.
                BookGuiManager.get().openEntry(GuideAccess.BOOK_ID, id("processing/culture_bowl"), 2);
                next();
            }
            case 9 -> { use(game, Items.RED_MUSHROOM, SUPPORT.above()); next(); }
            case 10 -> { use(game, Items.WHEAT_SEEDS, SUPPORT.above()); next(); }
            case 11 -> { use(game, Items.WATER_BUCKET, SUPPORT.above()); next(); }
            case 12 -> {
                if (ticks == 1) use(game, Items.AIR, SUPPORT.above());
                if (!completed(game, "culture_bowl_batch")) return false;
                require(count(game, item("construction/spore_culture")) == 0, "Both starting Cultures were spent");
                collecting = true;
                game.options.keyShift.setDown(true);
                game.getConnection().send(new net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket(game.player,
                        net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action.PRESS_SHIFT_KEY));
                use(game, Items.AIR, SUPPORT.above());
                next();
            }
            case 13 -> {
                if (count(game, item("construction/spore_culture")) != 1) return false;
                if (collecting) {
                    collecting = false;
                    game.options.keyShift.setDown(false);
                    game.getConnection().send(new net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket(game.player,
                            net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action.RELEASE_SHIFT_KEY));
                    LogUtils.getLogger().info("INFESTUS_DISCOVERY_SURVIVAL_RENEWED");
                }
                if (!readPage(game, "discovery/waking_genome")) return false;
                craftFromPage(game, "discovery/waking_genome");
                next();
            }
            case 14 -> { if (takeCraft(game)) next(); }
            case 15 -> {
                require(game.player.getInventory().items.stream().anyMatch(GuideAccess::isWakingGenome), "Replacement bound guide received");
                require(completed(game, "culture_bowl_batch") && completed(game, "living_substrate"), "Replacement retained server progress");
                game.player.closeContainer();
                LogUtils.getLogger().info("INFESTUS_DISCOVERY_SURVIVAL_COMPLETE");
                jump(20);
            }
            default -> throw new IllegalStateException("Unknown Survival stage " + stage);
        }
        return false;
    }

    private void craftFromPage(Minecraft game, String path) {
        var page = (BookRecipePage<?>) BookDataManager.get().getBook(GuideAccess.BOOK_ID).getEntry(id(path))
                .getPages().stream().filter(p -> p instanceof BookRecipePage<?>).findFirst().orElseThrow();
        var recipe = page.getRecipe1().value();
        var output = recipe.getResultItem(game.level.registryAccess());
        expectedCraft = output.getItem();
        expectedCount = count(game, expectedCraft) + output.getCount();
        game.setScreen(new InventoryScreen(game.player));
        int grid = 1;
        for (var ingredient : recipe.getIngredients()) {
            if (ingredient.isEmpty()) continue;
            final var choice = ingredient;
            int source = -1;
            for (int slot = 9; slot < 45; slot++) if (choice.test(game.player.inventoryMenu.getSlot(slot).getItem())) { source = slot; break; }
            require(source >= 0, "Starter inventory supplies guide recipe " + path);
            click(game, source, 0, ClickType.PICKUP);
            click(game, grid++, 1, ClickType.PICKUP);
            click(game, source, 0, ClickType.PICKUP);
        }
    }

    private boolean takeCraft(Minecraft game) {
        if (count(game, expectedCraft) >= expectedCount) return true;
        if (game.player.inventoryMenu.getSlot(0).getItem().is(expectedCraft)) click(game, 0, 0, ClickType.QUICK_MOVE);
        return false;
    }

    private boolean readPage(Minecraft game, String path) {
        var book = BookDataManager.get().getBook(GuideAccess.BOOK_ID);
        var entry = book.getEntry(id(path));
        if (!com.klikli_dev.modonomicon.bookstate.BookUnlockStateManager.get().isUnlockedFor(game.player, entry)) return false;
        if (!(game.screen instanceof com.klikli_dev.modonomicon.client.gui.book.entry.BookEntryScreen screen)
                || !screen.getEntry().getId().equals(id(path))) {
            BookGuiManager.get().openEntry(GuideAccess.BOOK_ID, id(path), 0);
            return false;
        }
        return ++readingTicks >= 3;
    }

    private boolean completed(Minecraft game, String path) {
        return earned.contains(id("discovery/" + path));
    }

    private static void use(Minecraft game, Item item, BlockPos pos) {
        game.player.closeContainer();
        int index = -1;
        for (int i = 0; i < 36; i++) {
            var stack = game.player.getInventory().getItem(i);
            if (item == Items.AIR ? stack.isEmpty() : stack.is(item)) { index = i; break; }
        }
        require(index >= 0, "Missing held ingredient " + item);
        if (index >= 9) { click(game, index, 8, ClickType.SWAP); index = 8; }
        game.player.getInventory().selected = index;
        var hit = Vec3.atCenterOf(pos).add(0, 0.5, 0);
        var delta = hit.subtract(game.player.getEyePosition());
        float yaw = (float) Math.toDegrees(Math.atan2(-delta.x, delta.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(delta.y, Math.sqrt(delta.x * delta.x + delta.z * delta.z)));
        game.player.setYRot(yaw); game.player.setXRot(pitch);
        game.getConnection().send(new ServerboundMovePlayerPacket.Rot(yaw, pitch, game.player.onGround()));
        game.gameMode.useItemOn(game.player, InteractionHand.MAIN_HAND, new BlockHitResult(hit, Direction.UP, pos, false));
    }

    private static void click(Minecraft game, int slot, int button, ClickType type) {
        game.gameMode.handleInventoryMouseClick(0, slot, button, type, game.player);
    }
    private static int count(Minecraft game, Item item) { return game.player.getInventory().countItem(item); }
    private static Item item(String path) { return BuiltInRegistries.ITEM.get(id(path)); }
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("infestusfrontier", path); }
    private static void require(boolean value, String message) { if (!value) throw new IllegalStateException(message); }
    private void next() { jump(stage + 1); }
    private void jump(int value) { stage = value; ticks = 0; readingTicks = 0; }
}
