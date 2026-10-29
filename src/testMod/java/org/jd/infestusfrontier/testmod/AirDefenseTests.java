package org.jd.infestusfrontier.testmod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.defense.DefenseModule;
import org.jd.infestusfrontier.ecology.EcologyModule;
import org.jd.infestusfrontier.storage.api.BiomassCapabilities;
import org.jd.infestusfrontier.storage.api.BiomassHandler;

@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class AirDefenseTests {
    private static final BlockPos ORGAN = new BlockPos(3, 2, 4);

    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            template = "empty",
            batch = "aerocyte_kill",
            timeoutTicks = 100)
    public static void aBloomFiresARealPelletAndKeepsItsKill(GameTestHelper h) {
        var organ = plant(h, 100);
        Ghast ghast = ghast(h);
        ghast.setHealth(4);
        ghast.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STICK));
        ghast.setDropChance(EquipmentSlot.MAINHAND, 2.0F);
        h.runAfterDelay(
                2,
                () -> h.assertTrue(
                        !h.getEntities(pelletType(), ORGAN, 24).isEmpty(), "a visible pellet travels to the target"));
        h.runAfterDelay(35, () -> {
            h.assertFalse(ghast.isAlive(), "the pellet deals four damage");
            h.assertValueEqual(organ.tank().stored(), Biomass.of(95), "one shot costs five BU");
            int sticks = 0;
            for (int i = 0; i < output(h).getSlots(); i++) {
                var stack = output(h).getStackInSlot(i);
                if (stack.is(Items.STICK)) sticks += stack.getCount();
            }
            h.assertValueEqual(sticks, 1, "the kill's guaranteed drop is collected once");
            h.assertTrue(h.getEntities(pelletType(), ORGAN, 24).isEmpty(), "impact removes the pellet");
            h.setBlock(ORGAN, Blocks.AIR);
            h.succeed();
        });
    }

    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            template = "empty",
            batch = "aerocyte_wall",
            timeoutTicks = 150)
    public static void aDryBloomAndABlockedBloomDoNotSpendOrShoot(GameTestHelper h) {
        var organ = plant(h, 0);
        Ghast ghast = ghast(h);
        float health = ghast.getHealth();
        h.runAfterDelay(45, () -> {
            h.assertValueEqual(ghast.getHealth(), health, "dry organ cannot shoot");
            for (int y = 1; y <= 14; y++) for (int z = 0; z <= 8; z++) h.setBlock(new BlockPos(6, y, z), Blocks.STONE);
            organ.insert(Biomass.of(25), false);
        });
        h.runAfterDelay(90, () -> {
            h.assertValueEqual(ghast.getHealth(), health, "a wall blocks sight");
            h.assertValueEqual(organ.tank().stored(), Biomass.of(25), "blocked shots cost nothing");
            for (int y = 1; y <= 14; y++) for (int z = 0; z <= 8; z++) h.setBlock(new BlockPos(6, y, z), Blocks.AIR);
        });
        h.runAfterDelay(140, () -> {
            h.assertTrue(ghast.getHealth() < health, "opening the shot resumes fire");
            h.setBlock(ORGAN, Blocks.AIR);
            h.succeed();
        });
    }

    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            template = "empty",
            batch = "aerocyte_full",
            timeoutTicks = 80)
    public static void aFullBloomAndGroundOnlyTargetsDoNotConsumeFuel(GameTestHelper h) {
        var organ = plant(h, 100);
        var store = new net.neoforged.neoforge.items.ItemStackHandler(8);
        for (int i = 0; i < 8; i++) store.setStackInSlot(i, new ItemStack(Items.STONE, 64));
        var saved = h.getBlockEntity(ORGAN).saveWithoutMetadata(h.getLevel().registryAccess());
        saved.put("items", store.serializeNBT(h.getLevel().registryAccess()));
        h.getBlockEntity(ORGAN).loadWithComponents(saved, h.getLevel().registryAccess());
        Ghast ghast = ghast(h);
        float health = ghast.getHealth();
        h.runAfterDelay(30, () -> {
            h.assertValueEqual(ghast.getHealth(), health, "a full store prevents fire");
            ghast.discard();
            output(h).extractItem(0, 64, false);
            var ground = h.spawn(EntityType.ZOGLIN, new BlockPos(10, 2, 4));
            ground.setNoAi(true);
            ground.setNoGravity(true);
        });
        h.runAfterDelay(70, () -> {
            h.assertValueEqual(organ.tank().stored(), Biomass.of(100), "ground targets are never fired upon");
            h.setBlock(ORGAN, Blocks.AIR);
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = "aerocyte_life")
    public static void aReloadedPelletKeepsItsLifetimeLimit(GameTestHelper h) {
        var pellet = pelletType().create(h.getLevel());
        pellet.setPos(h.absolutePos(ORGAN.above(4)).getCenter());
        var tag = new net.minecraft.nbt.CompoundTag();
        pellet.saveWithoutId(tag);
        tag.putInt("life", 40);
        var restored = pelletType().create(h.getLevel());
        restored.load(tag);
        restored.tick();
        h.assertTrue(restored.isRemoved(), "reloading cannot renew a spent pellet's lifetime");
        h.succeed();
    }

    private static BiomassHandler plant(GameTestHelper h, int fuel) {
        h.setBlock(ORGAN.below(), EcologyModule.LIVING_SUBSTRATE.get());
        h.setBlock(ORGAN, DefenseModule.AEROCYTE_BLOOM.get());
        var organ = h.getLevel().getCapability(BiomassCapabilities.BLOCK, h.absolutePos(ORGAN), null);
        if (fuel > 0) organ.insert(Biomass.of(fuel), false);
        return organ;
    }

    private static Ghast ghast(GameTestHelper h) {
        Ghast ghast = h.spawn(EntityType.GHAST, new BlockPos(11, 6, 4));
        ghast.setNoAi(true);
        ghast.setNoGravity(true);
        return ghast;
    }

    private static net.neoforged.neoforge.items.IItemHandler output(GameTestHelper h) {
        return h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(ORGAN), null);
    }

    private static EntityType<?> pelletType() {
        return BuiltInRegistries.ENTITY_TYPE.get(
                ResourceLocation.fromNamespaceAndPath("infestusfrontier", "spore_pellet"));
    }
}
