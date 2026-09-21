package org.jd.infestusfrontier.ecology;

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

final class EcologyContent {
    static final String LIVING_SUBSTRATE = "ecology/living_substrate";

    private final DeferredRegister.Blocks blocks = DeferredRegister.createBlocks(InfestusFrontier.MOD_ID);
    private final DeferredRegister.Items items = DeferredRegister.createItems(InfestusFrontier.MOD_ID);
    private final DeferredBlock<LivingSubstrateBlock> livingSubstrate;
    @SuppressWarnings("unused")
    private final DeferredItem<LivingSubstrateItem> livingSubstrateItem;

    EcologyContent() {
        livingSubstrate = blocks.registerBlock(
                LIVING_SUBSTRATE,
                LivingSubstrateBlock::new,
                BlockBehaviour.Properties.of()
                        .mapColor(MapColor.COLOR_RED)
                        .strength(0.8F)
                        .lightLevel(LivingSubstrateBlock::light)
                        .speedFactor(0.8F)
                        .pushReaction(PushReaction.BLOCK)
                        .sound(SoundType.NETHER_WART));
        livingSubstrateItem = items.register(LIVING_SUBSTRATE,
                () -> new LivingSubstrateItem(livingSubstrate.get(), new Item.Properties()));
    }

    LivingSubstrateBlock livingSubstrate() {
        return livingSubstrate.get();
    }

    void register(IEventBus modBus) {
        blocks.register(modBus);
        items.register(modBus);
    }
}
