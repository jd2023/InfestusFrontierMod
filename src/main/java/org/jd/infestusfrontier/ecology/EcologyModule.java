package org.jd.infestusfrontier.ecology;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jd.infestusfrontier.InfestusFrontier;

/** Owner of substrate maturity, function, lining, reinforcement, dye and deliberate conversion. */
public final class EcologyModule {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(InfestusFrontier.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(InfestusFrontier.MOD_ID);

    public static final DeferredBlock<LivingSubstrateBlock> LIVING_SUBSTRATE = BLOCKS.registerBlock(
            "living_substrate",
            LivingSubstrateBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_PINK)
                    .strength(0.6F)
                    .sound(SoundType.GRAVEL)
                    .pushReaction(PushReaction.BLOCK));
    public static final DeferredItem<BlockItem> LIVING_SUBSTRATE_ITEM = ITEMS.registerSimpleBlockItem(LIVING_SUBSTRATE);
    public static final DeferredItem<Item> SPORE_CULTURE = ITEMS.registerItem("spore_culture", SporeCultureItem::new);
    /**
     * What a grown cell leaves when it is broken: Living Substrate and everything grown in it die and never move. The
     * Digestive Sac digests it back into a little biomass.
     */
    public static final DeferredItem<Item> DEAD_TISSUE = ITEMS.registerSimpleItem("dead_tissue");

    private EcologyModule() {}

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
    }
}
