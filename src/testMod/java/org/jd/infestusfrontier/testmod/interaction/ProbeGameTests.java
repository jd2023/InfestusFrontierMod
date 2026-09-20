package org.jd.infestusfrontier.testmod.interaction;

import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.connection.ConnectionType;
import org.jd.infestusfrontier.processing.menu.BowlIntentPayload;
import org.jd.infestusfrontier.processing.menu.BowlMenuTarget;
import org.jd.infestusfrontier.processing.menu.BowlRefusal;
import org.jd.infestusfrontier.processing.menu.CultureBowlMenu;
import org.jd.infestusfrontier.processing.menu.CultureBowlMenuSnapshot;
import org.jd.infestusfrontier.testmod.integration.ContentAssertion;

@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class ProbeGameTests {
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void probeCraftsOpensAndLeavesPlacementAvailable(GameTestHelper helper) {
        var recipe = (CraftingRecipe) helper.getLevel().getRecipeManager().byKey(id("interaction/synaptic_probe")).orElseThrow().value();
        var ingredients = new ArrayList<>(List.of(new ItemStack(Items.BONE), new ItemStack(Items.SLIME_BALL), stack("construction/spore_culture")));
        while (ingredients.size() < 9) ingredients.add(ItemStack.EMPTY);
        var input = CraftingInput.of(3, 3, ingredients);
        helper.assertTrue(recipe.matches(input, helper.getLevel()), "Probe recipe uses bone, slime and Culture");
        var probe = recipe.assemble(input, helper.getLevel().registryAccess());
        helper.assertTrue(probe.is(BuiltInRegistries.ITEM.get(id("interaction/synaptic_probe"))) && probe.getCount() == 1,
                "Craft one reusable Synaptic Probe");

        var owner = FakePlayerFactory.getMinecraft(helper.getLevel());
        owner.setGameMode(GameType.SURVIVAL);
        var pos = placeOwned(helper, owner);
        helper.assertTrue(use(owner, pos, probe).consumesAction(), "Probe recognizes the Bowl target");
        openForTest(helper, owner, pos);
        helper.assertTrue(owner.containerMenu instanceof CultureBowlMenu, "Bowl creates its server menu");
        helper.assertTrue(owner.getMainHandItem().is(probe.getItem()) && owner.getMainHandItem().getCount() == 1,
                "Probe remains reusable");
        owner.closeContainer();

        use(owner, pos, new ItemStack(Items.DIRT), Direction.EAST);
        helper.assertTrue(helper.getLevel().getBlockState(pos.east()).is(net.minecraft.world.level.block.Blocks.DIRT),
                "Unsupported held blocks proceed to placement beside the Bowl");
        ContentAssertion.passGameTest(helper, "infestusfrontier_tests:interaction.synaptic_probe.obtain");
        ContentAssertion.passGameTest(helper, "infestusfrontier_tests:interaction.synaptic_probe.use");
        helper.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void staleWrongRemoteMalformedAndSpamIntentsCannotMutate(GameTestHelper helper) {
        var owner = FakePlayerFactory.getMinecraft(helper.getLevel());
        owner.setGameMode(GameType.SURVIVAL);
        var pos = placeOwned(helper, owner);
        use(owner, pos, new ItemStack(Items.RED_MUSHROOM));
        use(owner, pos, new ItemStack(Items.WHEAT_SEEDS));
        use(owner, pos, new ItemStack(Items.WATER_BUCKET));
        use(owner, pos, stack("interaction/synaptic_probe"));
        openForTest(helper, owner, pos);
        var menu = (CultureBowlMenu) owner.containerMenu;
        long revision = menu.snapshot().revision();
        var start = new BowlIntentPayload(menu.containerId, pos, revision, BowlIntentPayload.Intent.START, -1, "I000");
        menu.handleIntent(owner, start);
        var started = data(helper, pos);
        helper.assertTrue(started.getCompound("bowl").contains("active"), "Valid start mutates only through processing");

        menu.handleIntent(owner, start);
        helper.assertTrue(started.equals(data(helper, pos)) && menu.snapshot().refusal() == BowlRefusal.STALE_REVISION,
                "Stale revision cannot mutate the Bowl");
        menu.handleIntent(owner, new BowlIntentPayload(menu.containerId, pos.east(), revision,
                BowlIntentPayload.Intent.CANCEL, -1, ""));
        helper.assertTrue(started.equals(data(helper, pos)) && menu.snapshot().refusal() == BowlRefusal.REMOTE_TARGET,
                "Remote target cannot mutate the Bowl");
        menu.handleIntent(owner, new BowlIntentPayload(menu.containerId, pos, revision,
                BowlIntentPayload.Intent.EXTRACT_SLOT, 99, ""));
        helper.assertTrue(started.equals(data(helper, pos)), "Malformed slot cannot mutate the Bowl");
        menu.handleIntent(owner, start);
        helper.assertTrue(started.equals(data(helper, pos)) && menu.snapshot().refusal() == BowlRefusal.RATE_LIMITED,
                "A fifth intent in twenty ticks is refused without a queue");

        var observer = FakePlayerFactory.get(helper.getLevel(), new GameProfile(
                UUID.fromString("22222222-2222-2222-2222-222222222222"), "Observer"));
        observer.setGameMode(GameType.SURVIVAL);
        observer.setShiftKeyDown(true);
        use(observer, pos, new ItemStack(Items.STICK));
        observer.setShiftKeyDown(false);
        helper.assertTrue(started.equals(data(helper, pos)), "Wrong owner cannot mutate through direct Bowl controls");
        use(observer, pos, stack("interaction/synaptic_probe"));
        openForTest(helper, observer, pos);
        helper.assertTrue(observer.containerMenu instanceof CultureBowlMenu, "A second client may inspect current progress");
        var observerMenu = (CultureBowlMenu) observer.containerMenu;
        helper.assertTrue(observerMenu.snapshot().revision() == menu.snapshot().revision()
                        && observerMenu.snapshot().completedWork() == menu.snapshot().completedWork()
                        && observerMenu.snapshot().state() == menu.snapshot().state(),
                "Both clients observe the same authoritative progress");
        observerMenu.handleIntent(observer, new BowlIntentPayload(observerMenu.containerId, pos,
                observerMenu.snapshot().revision(), BowlIntentPayload.Intent.CANCEL, -1, ""));
        helper.assertTrue(started.equals(data(helper, pos)) && observerMenu.snapshot().refusal() == BowlRefusal.WRONG_OWNER,
                "Wrong owner can inspect but cannot mutate");

        tick(helper.getLevel().getBlockEntity(pos));
        int progress = data(helper, pos).getCompound("bowl").getCompound("active").getInt("work");
        owner.closeContainer();
        use(owner, pos, stack("interaction/synaptic_probe"));
        openForTest(helper, owner, pos);
        helper.assertTrue(((CultureBowlMenu) owner.containerMenu).snapshot().completedWork() == progress,
                "Close and reopen retains authoritative progress");
        helper.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void progressDoesNotInvalidateCancelButCommandsDo(GameTestHelper helper) {
        var owner = player(helper, "CancelOwner");
        var pos = placeOwned(helper, owner);
        use(owner, pos, new ItemStack(Items.RED_MUSHROOM));
        use(owner, pos, new ItemStack(Items.WHEAT_SEEDS));
        use(owner, pos, new ItemStack(Items.WATER_BUCKET));
        openForTest(helper, owner, pos);
        var menu = (CultureBowlMenu) owner.containerMenu;
        menu.handleIntent(owner, new BowlIntentPayload(menu.containerId, pos, menu.snapshot().revision(),
                BowlIntentPayload.Intent.START, -1, "I000"));
        long receivedRevision = menu.snapshot().revision();
        tick(helper.getLevel().getBlockEntity(pos));
        menu.handleIntent(owner, new BowlIntentPayload(menu.containerId, pos, receivedRevision,
                BowlIntentPayload.Intent.CANCEL, -1, ""));
        helper.assertTrue(!data(helper, pos).getCompound("bowl").contains("active")
                && menu.snapshot().refusal() == BowlRefusal.NONE, "Cancel survives progress between snapshots");
        var cancelled = data(helper, pos);
        menu.handleIntent(owner, new BowlIntentPayload(menu.containerId, pos, receivedRevision,
                BowlIntentPayload.Intent.START, -1, "I000"));
        helper.assertTrue(cancelled.equals(data(helper, pos)) && menu.snapshot().refusal() == BowlRefusal.STALE_REVISION,
                "A successful command still invalidates the old revision");
        helper.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void reopenCannotResetPlayerAdmission(GameTestHelper helper) {
        var owner = player(helper, "ReopenOwner");
        var first = placeOwned(helper, owner);
        var second = first.south(2);
        helper.getLevel().setBlock(second.below(), BuiltInRegistries.BLOCK.get(id("ecology/living_substrate")).defaultBlockState(), 2);
        helper.getLevel().setBlock(second, BuiltInRegistries.BLOCK.get(id("processing/culture_bowl")).defaultBlockState(), 2);
        ((org.jd.infestusfrontier.interaction.api.ProbeTarget) helper.getLevel().getBlockEntity(second)).claimProbeOwner(owner.getUUID());
        for (int i = 0; i < 5; i++) {
            var pos = i % 2 == 0 ? first : second;
            owner.setPos(Vec3.atCenterOf(pos).add(0, 0, -2));
            openForTest(helper, owner, pos);
            var menu = (CultureBowlMenu) owner.containerMenu;
            menu.handleIntent(owner, new BowlIntentPayload(menu.containerId, pos, menu.snapshot().revision(),
                    BowlIntentPayload.Intent.CANCEL, -1, ""));
            helper.assertTrue(menu.snapshot().refusal() == (i == 4 ? BowlRefusal.RATE_LIMITED : BowlRefusal.IDLE),
                    "Four intents per player survives replacing the menu, intent " + i);
            owner.closeContainer();
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void emptyMainHandAllowsOffhandPlacement(GameTestHelper helper) {
        var owner = player(helper, "OffhandOwner");
        var pos = placeOwned(helper, owner);
        use(owner, pos, new ItemStack(Items.RED_MUSHROOM));
        use(owner, pos, new ItemStack(Items.WHEAT_SEEDS));
        use(owner, pos, new ItemStack(Items.WATER_BUCKET));
        var before = data(helper, pos);
        owner.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.DIRT));
        var main = use(owner, pos, ItemStack.EMPTY, Direction.EAST);
        helper.assertTrue(!main.consumesAction() && before.equals(data(helper, pos)),
                "Empty main hand passes without starting the Bowl when offhand is occupied");
        var hit = new BlockHitResult(Vec3.atCenterOf(pos).add(0.5, 0, 0), Direction.EAST, pos, false);
        owner.gameMode.useItemOn(owner, helper.getLevel(), owner.getOffhandItem(), InteractionHand.OFF_HAND, hit);
        helper.assertTrue(helper.getLevel().getBlockState(pos.east()).is(net.minecraft.world.level.block.Blocks.DIRT),
                "Offhand places beside the Bowl");
        helper.assertTrue(before.equals(data(helper, pos)), "Placement leaves the Bowl unchanged");
        helper.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = "probe_quota")
    public static void allMenusShare64IntentsPerServerTick(GameTestHelper helper) {
        var owner = player(helper, "QuotaOwner");
        var pos = placeOwned(helper, owner);
        var before = data(helper, pos);
        for (int i = 0; i < 65; i++) {
            var observer = player(helper, "QuotaViewer" + i);
            observer.setPos(Vec3.atCenterOf(pos).add(0, 0, -2));
            openForTest(helper, observer, pos);
            var menu = (CultureBowlMenu) observer.containerMenu;
            menu.handleIntent(observer, new BowlIntentPayload(menu.containerId, pos, menu.snapshot().revision(),
                    BowlIntentPayload.Intent.CANCEL, -1, ""));
            helper.assertTrue(menu.snapshot().refusal() == (i < 64 ? BowlRefusal.WRONG_OWNER : BowlRefusal.SERVER_BUSY),
                    "Shared server limit at request " + i);
            observer.closeContainer();
        }
        helper.assertTrue(before.equals(data(helper, pos)), "Refused traffic never mutates the Bowl");
        helper.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void malformedRecipeAndSlotRefuseWithoutMutation(GameTestHelper helper) {
        var owner = player(helper, "MalformedOwner");
        var pos = placeOwned(helper, owner);
        var before = data(helper, pos);
        openForTest(helper, owner, pos);
        var menu = (CultureBowlMenu) owner.containerMenu;
        menu.handleIntent(owner, new BowlIntentPayload(menu.containerId, pos, menu.snapshot().revision(),
                BowlIntentPayload.Intent.START, -1, ""));
        helper.assertTrue(menu.snapshot().refusal() == BowlRefusal.UNKNOWN_RECIPE, "Blank recipe refuses without throwing");
        menu.handleIntent(owner, new BowlIntentPayload(menu.containerId, pos, menu.snapshot().revision(),
                BowlIntentPayload.Intent.EXTRACT_SLOT, 99, ""));
        helper.assertTrue(menu.snapshot().refusal() == BowlRefusal.MALFORMED_SLOT, "Malformed slot has a clear refusal");
        helper.assertTrue(before.equals(data(helper, pos)), "Malformed payloads leave the Bowl unchanged");
        helper.succeed();
    }

    private static ServerPlayer player(GameTestHelper helper, String name) {
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.nameUUIDFromBytes(name.getBytes(java.nio.charset.StandardCharsets.UTF_8)), name));
        player.setGameMode(GameType.SURVIVAL);
        return player;
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void maximumSnapshotRoundTripsBelowFourKiB(GameTestHelper helper) {
        var slots = java.util.stream.IntStream.range(0, 9)
                .mapToObj(i -> new CultureBowlMenuSnapshot.Slot("infestusfrontier:processing/nutrient_mash", 64)).toList();
        var snapshot = new CultureBowlMenuSnapshot(Long.MAX_VALUE - 1, slots, 2000, 2000,
                CultureBowlMenuSnapshot.State.WORKING, 1199, 1200, Long.MAX_VALUE - 1, "I033", BowlRefusal.RATE_LIMITED);
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess(), ConnectionType.NEOFORGE);
        CultureBowlMenuSnapshot.CODEC.encode(buffer, snapshot);
        helper.assertTrue(buffer.readableBytes() < CultureBowlMenuSnapshot.MAX_ENCODED_BYTES, "Snapshot stays below 4 KiB");
        helper.assertTrue(snapshot.equals(CultureBowlMenuSnapshot.CODEC.decode(buffer)), "Snapshot codec round trip");
        buffer.release();
        helper.succeed();
    }

    private static BlockPos placeOwned(GameTestHelper helper, ServerPlayer player) {
        var support = helper.absolutePos(BlockPos.ZERO);
        helper.getLevel().setBlock(support, BuiltInRegistries.BLOCK.get(id("ecology/living_substrate")).defaultBlockState(), 2);
        player.setPos(Vec3.atCenterOf(support).add(0, 1, -2));
        player.setItemInHand(InteractionHand.MAIN_HAND, stack("processing/culture_bowl"));
        var hit = new BlockHitResult(Vec3.atCenterOf(support), Direction.UP, support, false);
        player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
        return support.above();
    }

    private static net.minecraft.world.InteractionResult use(ServerPlayer player, BlockPos pos, ItemStack stack) {
        return use(player, pos, stack, Direction.UP);
    }
    private static net.minecraft.world.InteractionResult use(ServerPlayer player, BlockPos pos, ItemStack stack, Direction face) {
        player.setPos(Vec3.atCenterOf(pos).add(0, 0, -2));
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var hit = new BlockHitResult(Vec3.atCenterOf(pos).add(Vec3.atLowerCornerOf(face.getNormal()).scale(0.5)), face, pos, false);
        return player.gameMode.useItemOn(player, player.serverLevel(), stack, InteractionHand.MAIN_HAND, hit);
    }

    private static void openForTest(GameTestHelper helper, ServerPlayer player, BlockPos pos) {
        var target = (BowlMenuTarget) helper.getLevel().getBlockEntity(pos);
        player.containerMenu = new CultureBowlMenu(player.containerMenu.containerId + 1, player.getInventory(), target);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void tick(BlockEntity bowl) {
        var ticker = bowl.getBlockState().getTicker(bowl.getLevel(), bowl.getType());
        ((net.minecraft.world.level.block.entity.BlockEntityTicker) ticker).tick(bowl.getLevel(), bowl.getBlockPos(), bowl.getBlockState(), bowl);
    }
    private static net.minecraft.nbt.CompoundTag data(GameTestHelper helper, BlockPos pos) {
        return helper.getLevel().getBlockEntity(pos).saveWithFullMetadata(helper.getLevel().registryAccess());
    }
    private static ItemStack stack(String path) { return new ItemStack(BuiltInRegistries.ITEM.get(id(path))); }
    private static ResourceLocation id(String path) { return ResourceLocation.parse("infestusfrontier:" + path); }
}
