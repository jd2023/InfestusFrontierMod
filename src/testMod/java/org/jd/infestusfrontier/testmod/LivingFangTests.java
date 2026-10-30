package org.jd.infestusfrontier.testmod;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jd.infestusfrontier.core.equipment.LivingTool;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.core.foundation.Outcome;
import org.jd.infestusfrontier.core.storage.BiomassTank;
import org.jd.infestusfrontier.ecology.EcologyModule;
import org.jd.infestusfrontier.equipment.EquipmentModule;
import org.jd.infestusfrontier.equipment.ToolMutation;
import org.jd.infestusfrontier.equipment.api.Reserves;
import org.jd.infestusfrontier.logistics.LogisticsModule;
import org.jd.infestusfrontier.processing.ProcessingModule;
import org.jd.infestusfrontier.processing.api.BatchOrganContents;
import org.jd.infestusfrontier.storage.StorageModule;

/** The Dormant Fang from plates (ITEM_CATALOG I017), the Living Fang (I018) and its Grasping graft. */
@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class LivingFangTests {
    private static final BlockPos GROUND = new BlockPos(1, 1, 1);
    private static final BlockPos ORGAN = GROUND.above();
    private static final int AWAKENING_TICKS = 600;

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void twoPlatesOnAStickMakeAWeakDormantFang(GameTestHelper h) {
        List<ItemStack> grid = new java.util.ArrayList<>();
        for (char c : ("#  " + "#  " + "|  ").toCharArray())
            grid.add(
                    c == '#'
                            ? new ItemStack(ProcessingModule.BONE_PLATE.get())
                            : c == '|' ? new ItemStack(Items.STICK) : ItemStack.EMPTY);
        CraftingInput input = CraftingInput.of(3, 3, grid.subList(0, 9));
        var recipe = h.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, h.getLevel());
        h.assertTrue(recipe.isPresent(), "two plates over a stick craft");
        ItemStack made = recipe.get().value().assemble(input, h.getLevel().registryAccess());
        h.assertTrue(made.is(EquipmentModule.DORMANT_FANG.get()), "a Dormant Fang");
        h.assertValueEqual(made.getMaxDamage(), LivingTool.DORMANT_USES, "12 strikes");
        h.assertValueEqual(attackDamage(made), 2.0, "less than a wooden sword's bite of three");
        h.assertTrue(Reserves.of(made).isEmpty(), "a dormant fang holds nothing");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 700)
    public static void theCradleWakesAFangAndTheGraftMakesItGrasping(GameTestHelper h) {
        h.setBlock(GROUND, EcologyModule.LIVING_SUBSTRATE.get());
        h.setBlock(ORGAN, ProcessingModule.AWAKENING_CRADLE.get());
        ItemStack worn = new ItemStack(EquipmentModule.DORMANT_FANG.get());
        worn.setDamageValue(3);
        Player loader = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
        loader.setItemInHand(InteractionHand.MAIN_HAND, worn);
        EcologyTests.use(h, loader, ORGAN, Direction.UP);
        load(h, EcologyModule.SPORE_CULTURE.get(), 1);
        load(h, StorageModule.BIOMASS_BUCKET.get(), 1);
        h.runAtTickTime(AWAKENING_TICKS + 2, () -> {
            ItemStack woken = contents(h).output();
            h.assertTrue(woken.is(EquipmentModule.LIVING_FANG.get()), "a Living Fang");
            h.assertValueEqual(woken.getMaxDamage(), LivingTool.LIVING_USES, "256 strikes");
            h.assertValueEqual(woken.getDamageValue(), 64, "as worn as it went in");
            h.assertValueEqual(attackDamage(woken), 3.5, "a little more bite");
            h.assertValueEqual(Reserves.of(woken).orElseThrow().stored(), Biomass.ZERO, "wakes empty");
            var graft = h.getLevel()
                    .getRecipeManager()
                    .byKey(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                            "infestusfrontier", "awakening/grasping_fang"))
                    .orElseThrow();
            var batch = (org.jd.infestusfrontier.processing.api.OrganBatchRecipe) graft.value();
            fill(woken, 12);
            ItemStack grafted = batch.resultFor(List.of(woken, new ItemStack(LogisticsModule.CONTRACTILE_FIBER.get())));
            h.assertValueEqual(grafted.get(EquipmentModule.TOOL_MUTATION.get()), ToolMutation.GRASPING, "Grasping");
            h.assertValueEqual(grafted.getDamageValue(), 64, "the graft keeps the wear");
            h.assertValueEqual(Reserves.of(grafted).orElseThrow().stored(), Biomass.of(12), "and the reserve");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aGraspingFangSlowsWhatItHitsForTwoBUUntilItIsDry(GameTestHelper h) {
        Zombie prey = h.spawn(EntityType.ZOMBIE, new BlockPos(2, 1, 2));
        prey.setNoAi(true);
        ItemStack fang = new ItemStack(EquipmentModule.LIVING_FANG.get());
        fang.set(EquipmentModule.TOOL_MUTATION.get(), ToolMutation.GRASPING);
        fill(fang, 3);
        Player hunter = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
        hunter.setItemInHand(InteractionHand.MAIN_HAND, fang);
        float health = prey.getHealth();
        fang.getItem().postHurtEnemy(fang, prey, hunter); // what vanilla calls after the blade has cut
        var slowed = prey.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
        h.assertTrue(slowed != null, "the prey is gripped");
        h.assertValueEqual(slowed.getAmplifier(), LivingTool.GRIP_LEVEL, "Slowness II");
        h.assertValueEqual(slowed.getDuration(), LivingTool.GRIP_TICKS, "for three seconds");
        h.assertValueEqual(Reserves.of(fang).orElseThrow().stored(), Biomass.of(1), "2 BU paid");
        h.assertValueEqual(fang.getDamageValue(), 1, "one strike of wear, as vanilla charges");
        prey.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        fang.getItem().postHurtEnemy(fang, prey, hunter);
        h.assertTrue(prey.getEffect(MobEffects.MOVEMENT_SLOWDOWN) == null, "one BU grips nothing");
        h.assertValueEqual(Reserves.of(fang).orElseThrow().stored(), Biomass.of(1), "and costs nothing");
        ItemStack plain = new ItemStack(EquipmentModule.LIVING_FANG.get());
        fill(plain, 40);
        plain.getItem().postHurtEnemy(plain, prey, hunter);
        h.assertTrue(prey.getEffect(MobEffects.MOVEMENT_SLOWDOWN) == null, "an ungrafted fang only cuts");
        h.assertValueEqual(Reserves.of(plain).orElseThrow().stored(), Biomass.of(40), "for free");
        h.assertValueEqual(prey.getHealth(), health, "no hook of ours hurts; the blade's damage is vanilla's");
        h.succeed();
    }

    /** The attack damage modifier the item adds to a hand: the blade's figure plus the tier's bonus. */
    private static double attackDamage(ItemStack stack) {
        double[] sum = {0};
        stack.forEachModifier(net.minecraft.world.entity.EquipmentSlot.MAINHAND, (attribute, modifier) -> {
            if (attribute.is(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE))
                sum[0] += modifier.amount();
        });
        return sum[0];
    }

    private static void fill(ItemStack tool, int units) {
        BiomassTank reserve = Reserves.of(tool).orElseThrow();
        tool.set(EquipmentModule.RESERVE.get(), ((Outcome.Done<BiomassTank>) reserve.fill(Biomass.of(units))).value());
    }

    private static void load(GameTestHelper h, Item item, int count) {
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, item, count);
        for (int i = 0; i < count; i++) EcologyTests.use(h, player, ORGAN, Direction.UP);
    }

    private static BatchOrganContents contents(GameTestHelper h) {
        var registries = h.getLevel().registryAccess();
        var tag = h.getBlockEntity(ORGAN).saveWithoutMetadata(registries);
        return BatchOrganContents.CODEC
                .parse(registries.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), tag.get("contents"))
                .getOrThrow();
    }
}
