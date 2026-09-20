package org.jd.infestusfrontier.processing;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Registers only the Bowl and products with implemented Bowl producers. */
public final class ProcessingModule {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks("infestusfrontier");
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("infestusfrontier");
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, "infestusfrontier");
    static final java.util.function.Supplier<CultureBowlBlock> BOWL = BLOCKS.register("processing/culture_bowl",
            () -> new CultureBowlBlock(BlockBehaviour.Properties.of().strength(0.6F).noOcclusion()
                    .sound(SoundType.WART_BLOCK).pushReaction(PushReaction.BLOCK)));
    static final java.util.function.Supplier<BlockEntityType<CultureBowlEntity>> BOWL_ENTITY = ENTITIES.register("processing/culture_bowl",
            () -> BlockEntityType.Builder.of(CultureBowlEntity::new, BOWL.get()).build(null));

    public void register(IEventBus bus) {
        ITEMS.register("processing/culture_bowl", () -> new CultureBowlItem(BOWL.get(), new Item.Properties().stacksTo(1)));
        for (String name : new String[] {"elastic_gel", "nutrient_mash", "honey_culture", "rooting_gel"}) {
            ITEMS.register("processing/" + name, () -> new Item(new Item.Properties()));
        }
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(ProcessingModule::allowBowlControls);
        BLOCKS.register(bus);
        ITEMS.register(bus);
        ENTITIES.register(bus);
    }
    private static void allowBowlControls(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock event) {
        var level = event.getLevel();
        if (level.hasChunkAt(event.getPos()) && level.getBlockState(event.getPos()).is(BOWL.get())) {
            event.setUseBlock(net.neoforged.neoforge.common.util.TriState.TRUE);
        }
    }
}
