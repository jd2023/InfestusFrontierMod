package org.jd.infestusfrontier.testmod.discovery;

import com.mojang.authlib.GameProfile;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jd.infestusfrontier.discovery.GuideAccess;
import org.jd.infestusfrontier.testmod.integration.AdvancementRecordingPlayer;
import org.jd.infestusfrontier.testmod.integration.ContentAssertion;

@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class DiscoveryGameTests {
    private static final ResourceLocation GUIDE_RECIPE = id("discovery/waking_genome");
    private static final ResourceLocation BOWL = id("processing/culture_bowl");
    private static final ResourceLocation BOWL_MILESTONE = id("discovery/culture_bowl_batch");

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void replacementGuideCraftRetainsProgressIdentity(GameTestHelper helper) {
        var recipe = (CraftingRecipe) helper.getLevel().getRecipeManager().byKey(GUIDE_RECIPE).orElseThrow().value();
        var ingredients = new ArrayList<>(List.of(new ItemStack(Items.BOOK), stack("construction/spore_culture")));
        while (ingredients.size() < 4) ingredients.add(ItemStack.EMPTY);
        var input = CraftingInput.of(2, 2, ingredients);
        helper.assertTrue(recipe.matches(input, helper.getLevel()), "Guide recipe uses one ordinary book and one Culture");
        var crafted = recipe.assemble(input, helper.getLevel().registryAccess());
        helper.assertTrue(crafted.getCount() == 1 && GuideAccess.isWakingGenome(crafted),
                "Replacement recipe creates exactly one bound Waking Genome");
        ContentAssertion.passGameTest(helper, "infestusfrontier_tests:discovery.waking_genome.obtain");
        ContentAssertion.passGameTest(helper, "infestusfrontier_tests:discovery.waking_genome.use");
        helper.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void fullInventoryRetainsOneClaimAndRepeatJoinDoesNotDuplicate(GameTestHelper helper) {
        var player = player(helper, "PendingGuide");
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            player.getInventory().setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
        }
        NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedInEvent(player));
        helper.assertTrue(countGuides(player) == 0, "Full inventory must not drop or force a guide");

        player.getInventory().setItem(0, ItemStack.EMPTY);
        NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(player));
        helper.assertTrue(countGuides(player) == 1, "Pending claim delivers once when one slot becomes available");

        NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedInEvent(player));
        NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(player));
        helper.assertTrue(countGuides(player) == 1, "Repeated joins and ticks cannot grant a second guide");
        helper.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void creativeFullInventoryRetainsClaim(GameTestHelper helper) {
        var player = player(helper, "CreativePendingGuide");
        player.setGameMode(GameType.CREATIVE);
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            player.getInventory().setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
        }
        NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedInEvent(player));
        helper.assertTrue(countGuides(player) == 0, "Full Creative inventory receives no item");
        player.getInventory().setItem(0, ItemStack.EMPTY);
        NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(player));
        helper.assertTrue(countGuides(player) == 1, "Creative overflow must retain the undelivered claim");
        NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedInEvent(player));
        helper.assertTrue(countGuides(player) == 1, "Creative claim is once only");
        helper.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void pendingAndDeliveredClaimsSurvivePlayerClone(GameTestHelper helper) {
        var original = player(helper, "CloneGuide");
        for (int slot = 0; slot < 36; slot++) original.getInventory().setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
        NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedInEvent(original));
        var replacement = player(helper, "CloneGuide");
        NeoForge.EVENT_BUS.post(new PlayerEvent.Clone(replacement, original, true));
        NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedInEvent(replacement));
        helper.assertTrue(countGuides(replacement) == 1, "Pending claim survives respawn");
        var third = player(helper, "CloneGuide");
        NeoForge.EVENT_BUS.post(new PlayerEvent.Clone(third, replacement, true));
        NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedInEvent(third));
        helper.assertTrue(countGuides(third) == 0, "Delivered claim cannot be repeated after death");
        helper.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void creativeGiveDoesNotCompleteBowlOperation(GameTestHelper helper) {
        var player = player(helper, "CreativeBowl");
        player.setGameMode(GameType.CREATIVE);
        player.getInventory().add(new ItemStack(BuiltInRegistries.ITEM.get(BOWL)));
        var advancement = helper.getLevel().getServer().getAdvancements().get(BOWL_MILESTONE);
        helper.assertTrue(advancement != null, "Bowl completion advancement must exist");
        helper.assertTrue(!player.wasAwarded(advancement.id()),
                "Possessing a creative Bowl is not a completed rooted batch");
        helper.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void craftingCultureCompletesOnlyTheObtainMilestone(GameTestHelper helper) {
        var player = player(helper, "CraftedCulture");
        var culture = stack("construction/spore_culture");
        NeoForge.EVENT_BUS.post(new PlayerEvent.ItemCraftedEvent(player, culture, new SimpleContainer(0)));
        var cultureMilestone = helper.getLevel().getServer().getAdvancements().get(id("discovery/spore_culture"));
        var bowlMilestone = helper.getLevel().getServer().getAdvancements().get(BOWL_MILESTONE);
        helper.assertTrue(player.wasAwarded(cultureMilestone.id()),
                "A real Culture craft completes the obtain milestone");
        helper.assertTrue(!player.wasAwarded(bowlMilestone.id()),
                "Crafting an ingredient cannot complete the Bowl operation milestone");
        helper.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void receivingCultureAndCreativeBowlRevealWithoutOperating(GameTestHelper helper) {
        var player = player(helper, "ReceivedCulture");
        var culture = stack("construction/spore_culture");
        player.getInventory().setItem(0, culture);
        net.minecraft.advancements.CriteriaTriggers.INVENTORY_CHANGED.trigger(player, player.getInventory(), culture);
        helper.assertTrue(player.wasAwarded(id("discovery/spore_culture")),
                "Received Culture earns the obtain milestone without a craft");
        player.setGameMode(GameType.CREATIVE);
        var bowl = stack("processing/culture_bowl");
        player.getInventory().setItem(1, bowl);
        net.minecraft.advancements.CriteriaTriggers.INVENTORY_CHANGED.trigger(player, player.getInventory(), bowl);
        helper.assertTrue(player.wasAwarded(id("discovery/acquired/culture_bowl")),
                "Possession reveals Bowl safety instructions");
        helper.assertTrue(!player.wasAwarded(BOWL_MILESTONE), "Possession is not operation completion");
        helper.succeed();
    }

    private static AdvancementRecordingPlayer player(GameTestHelper helper, String name) {
        var profile = new GameProfile(UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)), name);
        var player = new AdvancementRecordingPlayer(helper.getLevel(), profile);
        player.setGameMode(GameType.SURVIVAL);
        player.getInventory().clearContent();
        return player;
    }

    private static int countGuides(ServerPlayer player) {
        int count = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (GuideAccess.isWakingGenome(player.getInventory().getItem(slot))) count++;
        }
        return count;
    }

    private static ItemStack stack(String path) {
        return new ItemStack(BuiltInRegistries.ITEM.get(id(path)));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("infestusfrontier", path);
    }
}
