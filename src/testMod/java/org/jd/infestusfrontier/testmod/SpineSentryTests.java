package org.jd.infestusfrontier.testmod;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import org.jd.infestusfrontier.core.defense.Sentry;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.defense.DefenseModule;
import org.jd.infestusfrontier.defense.SpineSentryBlock;
import org.jd.infestusfrontier.ecology.EcologyModule;
import org.jd.infestusfrontier.storage.StorageModule;
import org.jd.infestusfrontier.storage.api.BiomassCapabilities;
import org.jd.infestusfrontier.storage.api.BiomassHandler;

/**
 * The Spine Sentry (BLOCK_CATALOG T2-30): it lashes hostiles in sight for biomass, keeps what they drop, and rests
 * dry, full or blind. Zoglins stand in for hostiles: they never burn in the sun and always drop rotten flesh.
 */
@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class SpineSentryTests {
    private static final BlockPos GROUND = new BlockPos(4, 1, 4);
    private static final BlockPos ORGAN = GROUND.above();

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 200)
    public static void aFedSentryLashesTheHostileInReachAndKeepsWhatItDrops(GameTestHelper h) {
        plant(h, 1000);
        Zoglin prey = h.spawn(EntityType.ZOGLIN, new BlockPos(7, 2, 4));
        prey.setNoAi(true);
        prey.setHealth(Sentry.DAMAGE); // one lash
        Sheep bystander = h.spawn(EntityType.SHEEP, new BlockPos(1, 2, 4));
        bystander.setNoAi(true);
        Player visitor = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
        visitor.setPos(h.absolutePos(new BlockPos(4, 2, 7)).getCenter());
        h.runAfterDelay(
                5,
                () -> h.assertTrue(
                        h.getBlockState(ORGAN).getValue(SpineSentryBlock.WORKING), "shown working after the strike"));
        h.runAfterDelay(Sentry.PULSE_TICKS + 5, () -> {
            h.assertFalse(prey.isAlive(), "the zoglin is lashed dead");
            h.assertValueEqual(tank(h).stored(), Biomass.of(1000 - Sentry.COST), "10 BU a strike");
            h.assertFalse(
                    h.getBlockState(ORGAN).getValue(SpineSentryBlock.WORKING), "and resting once nothing is left");
            int kept = 0;
            for (ItemStack stack : stored(h)) if (stack.is(Items.ROTTEN_FLESH)) kept += stack.getCount();
            h.assertTrue(kept >= 1, "its rotten flesh is in the store, not on the ground");
            h.assertTrue(h.getEntities(EntityType.ITEM, ORGAN, 8.0).isEmpty(), "nothing fell");
            h.assertTrue(
                    bystander.isAlive() && bystander.getHealth() == bystander.getMaxHealth(), "the sheep is untouched");
            h.assertTrue(visitor.getHealth() == visitor.getMaxHealth(), "the player is untouched");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 200)
    public static void aDrySentryAndAFullOneRest(GameTestHelper h) {
        plant(h, Sentry.COST - 1);
        Zoglin prey = h.spawn(EntityType.ZOGLIN, new BlockPos(7, 2, 4));
        prey.setNoAi(true);
        float health = prey.getHealth();
        h.runAfterDelay(Sentry.RETRY_TICKS + 5, () -> {
            h.assertValueEqual(prey.getHealth(), health, "nine BU buys no lash");
            h.assertFalse(h.getBlockState(ORGAN).getValue(SpineSentryBlock.WORKING), "shown resting");
            feed(h, 500);
            ItemStack[] full = new ItemStack[Sentry.STORE_SLOTS];
            java.util.Arrays.fill(full, new ItemStack(Items.COBBLESTONE));
            setStore(h, full);
        });
        h.runAfterDelay(Sentry.RETRY_TICKS * 2 + 10, () -> {
            h.assertValueEqual(prey.getHealth(), health, "a full store stops the lash");
            ItemStack[] freed = new ItemStack[Sentry.STORE_SLOTS];
            java.util.Arrays.fill(freed, new ItemStack(Items.COBBLESTONE));
            freed[0] = ItemStack.EMPTY;
            setStore(h, freed);
        });
        h.runAfterDelay(Sentry.RETRY_TICKS * 3 + 15, () -> {
            h.assertTrue(prey.getHealth() < health, "one well freed, it lashes again");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 200)
    public static void aSentryLashesOnlyWhatItSeesAndWithinReach(GameTestHelper h) {
        plant(h, 1000);
        for (int y = 1; y <= 4; y++) for (int z = 2; z <= 6; z++) h.setBlock(new BlockPos(6, y, z), Blocks.STONE);
        Zoglin hidden = h.spawn(EntityType.ZOGLIN, new BlockPos(8, 2, 4)); // behind the wall
        hidden.setNoAi(true);
        Zoglin far = h.spawn(EntityType.ZOGLIN, new BlockPos(4, 2, 12)); // eight blocks off
        far.setNoAi(true);
        float hiddenHealth = hidden.getHealth(), farHealth = far.getHealth();
        h.runAfterDelay(Sentry.RETRY_TICKS + 5, () -> {
            h.assertValueEqual(hidden.getHealth(), hiddenHealth, "a wall hides the zoglin");
            h.assertValueEqual(far.getHealth(), farHealth, "eight blocks is out of reach");
            h.assertValueEqual(tank(h).stored(), Biomass.of(1000), "nothing paid");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void theStoreIsTakenFromAndNeverFilled(GameTestHelper h) {
        plant(h, 0);
        ItemStack[] some = new ItemStack[Sentry.STORE_SLOTS];
        java.util.Arrays.fill(some, ItemStack.EMPTY);
        some[2] = new ItemStack(Items.ROTTEN_FLESH, 5);
        setStore(h, some);
        IItemHandler seen =
                h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(ORGAN), Direction.UP);
        h.assertTrue(seen != null, "the store is reachable");
        h.assertValueEqual(seen.getSlots(), Sentry.STORE_SLOTS, "eight wells");
        h.assertValueEqual(seen.insertItem(0, new ItemStack(Items.STONE), false).getCount(), 1, "nothing goes in");
        h.assertValueEqual(seen.extractItem(2, 3, false).getCount(), 3, "three of the flesh come out");
        h.assertValueEqual(stored(h).get(2).getCount(), 2, "two stay");
        h.succeed();
    }

    /**
     * Seven wells blocked, one empty: the zoglin's flesh takes the empty well and the stick it carried (a drop chance
     * over one makes vanilla drop it every time) finds no well and falls, once.
     */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 200)
    public static void whatDoesNotFitFallsOnceAndNothingIsDoubled(GameTestHelper h) {
        plant(h, 1000);
        ItemStack[] wells = new ItemStack[Sentry.STORE_SLOTS];
        java.util.Arrays.fill(wells, new ItemStack(Items.COBBLESTONE));
        wells[5] = ItemStack.EMPTY;
        setStore(h, wells);
        Zoglin prey = h.spawn(EntityType.ZOGLIN, new BlockPos(7, 2, 4));
        prey.setNoAi(true);
        prey.setHealth(Sentry.DAMAGE);
        prey.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(Items.STICK));
        prey.setDropChance(net.minecraft.world.entity.EquipmentSlot.MAINHAND, 2.0F);
        h.runAfterDelay(Sentry.PULSE_TICKS + 5, () -> {
            h.assertFalse(prey.isAlive(), "the zoglin is lashed dead");
            h.assertTrue(stored(h).get(5).is(Items.ROTTEN_FLESH), "its flesh took the empty well");
            int sticksKept = stored(h).stream()
                    .mapToInt(s -> s.is(Items.STICK) ? s.getCount() : 0)
                    .sum();
            var fallen = h.getEntities(EntityType.ITEM, ORGAN, 8.0);
            int sticksFallen = fallen.stream()
                    .mapToInt(d -> d.getItem().is(Items.STICK) ? d.getItem().getCount() : 0)
                    .sum();
            int fleshFallen = fallen.stream()
                    .mapToInt(d ->
                            d.getItem().is(Items.ROTTEN_FLESH) ? d.getItem().getCount() : 0)
                    .sum();
            h.assertValueEqual(sticksKept, 0, "no well for the stick");
            h.assertValueEqual(sticksFallen, 1, "the stick fell, once");
            h.assertValueEqual(fleshFallen, 0, "the flesh did not");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void breakingASentryDropsItsStoreAndKeepsItsTankOnTheItem(GameTestHelper h) {
        plant(h, 500);
        ItemStack[] wells = new ItemStack[Sentry.STORE_SLOTS];
        java.util.Arrays.fill(wells, ItemStack.EMPTY);
        wells[1] = new ItemStack(Items.ROTTEN_FLESH, 9);
        setStore(h, wells);
        h.getLevel().destroyBlock(h.absolutePos(ORGAN), true);
        var drops = h.getEntities(EntityType.ITEM, ORGAN, 3.0);
        int flesh = drops.stream()
                .mapToInt(drop ->
                        drop.getItem().is(Items.ROTTEN_FLESH) ? drop.getItem().getCount() : 0)
                .sum();
        h.assertValueEqual(flesh, 9, "the store falls out");
        ItemStack organ = drops.stream()
                .map(drop -> drop.getItem())
                .filter(stack -> stack.is(DefenseModule.SPINE_SENTRY_ITEM.get()))
                .findFirst()
                .orElseThrow(() -> new net.minecraft.gametest.framework.GameTestAssertException("no sentry item"));
        var tank = organ.get(StorageModule.TANK.get());
        h.assertTrue(tank != null && tank.stored().equals(Biomass.of(500)), "the item carries its 500 BU");
        h.succeed();
    }

    private static void plant(GameTestHelper h, int biomass) {
        h.setBlock(GROUND, EcologyModule.LIVING_SUBSTRATE.get());
        h.setBlock(ORGAN, DefenseModule.SPINE_SENTRY.get());
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
        if (biomass > 0) feed(h, biomass);
    }

    private static void feed(GameTestHelper h, int biomass) {
        handler(h).insert(Biomass.of(biomass), false);
    }

    private static BiomassHandler handler(GameTestHelper h) {
        BiomassHandler handler = h.getLevel().getCapability(BiomassCapabilities.BLOCK, h.absolutePos(ORGAN), null);
        if (handler == null) throw new IllegalStateException("no biomass handler on the sentry");
        return handler;
    }

    private static org.jd.infestusfrontier.core.storage.BiomassTank tank(GameTestHelper h) {
        return handler(h).tank();
    }

    /** Writes the store's wells through the saved form, as a world load would. */
    private static void setStore(GameTestHelper h, ItemStack[] wells) {
        var registries = h.getLevel().registryAccess();
        var sentry = h.getBlockEntity(ORGAN);
        var handler = new net.neoforged.neoforge.items.ItemStackHandler(Sentry.STORE_SLOTS);
        for (int slot = 0; slot < wells.length; slot++) handler.setStackInSlot(slot, wells[slot].copy());
        var tag = sentry.saveWithoutMetadata(registries);
        tag.put("items", handler.serializeNBT(registries));
        sentry.loadWithComponents(tag, registries);
    }

    /** The store's wells as the world sees them. */
    private static List<ItemStack> stored(GameTestHelper h) {
        IItemHandler seen =
                h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(ORGAN), Direction.UP);
        if (seen == null) throw new IllegalStateException("no item handler on the sentry");
        List<ItemStack> out = new java.util.ArrayList<>();
        for (int slot = 0; slot < seen.getSlots(); slot++) out.add(seen.getStackInSlot(slot));
        return out;
    }
}
