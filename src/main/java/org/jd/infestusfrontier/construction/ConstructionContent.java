package org.jd.infestusfrontier.construction;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jd.infestusfrontier.InfestusFrontier;
import org.jd.infestusfrontier.construction.api.CultureUse;

final class ConstructionContent {
    static final String SPORE_CULTURE = "construction/spore_culture";
    static final String ORGAN_BUD = "construction/organ_bud";

    private final DeferredRegister.Blocks blocks = DeferredRegister.createBlocks(InfestusFrontier.MOD_ID);
    private final DeferredRegister.Items items = DeferredRegister.createItems(InfestusFrontier.MOD_ID);
    private final DeferredBlock<OrganBudBlock> organBud;
    @SuppressWarnings("unused")
    private final DeferredItem<Item> sporeCulture;
    @SuppressWarnings("unused")
    private final DeferredItem<BlockItem> organBudItem;

    ConstructionContent(BudRecipeRegistry budRecipes, CultureUse cultureUse) {
        organBud = blocks.registerBlock(
                ORGAN_BUD,
                properties -> new OrganBudBlock(properties, budRecipes),
                BlockBehaviour.Properties.of()
                        .mapColor(MapColor.COLOR_RED)
                        .strength(0.4F)
                        .sound(SoundType.WART_BLOCK)
                        .noOcclusion());
        sporeCulture = items.register(SPORE_CULTURE,
                () -> new SporeCultureItem(new Item.Properties(), cultureUse));
        organBudItem = items.registerSimpleBlockItem(organBud, new Item.Properties());
    }

    void register(IEventBus modBus) {
        blocks.register(modBus);
        items.register(modBus);
    }
}
