package org.jd.infestusfrontier.control;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jd.infestusfrontier.InfestusFrontier;
import org.slf4j.Logger;

/** Owner of signals and interlocks. Ships the Sensor Polyp. */
public final class ControlModule {
    static final Logger LOGGER = LogUtils.getLogger();
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(InfestusFrontier.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(InfestusFrontier.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, InfestusFrontier.MOD_ID);

    public static final DeferredBlock<SensorPolypBlock> SENSOR_POLYP = BLOCKS.registerBlock(
            "sensor_polyp",
            SensorPolypBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(0.8F)
                    .sound(SoundType.SLIME_BLOCK)
                    .noOcclusion());
    public static final DeferredItem<BlockItem> SENSOR_POLYP_ITEM = ITEMS.registerSimpleBlockItem(SENSOR_POLYP);
    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SensorPolypBlockEntity>> SENSOR_POLYP_ENTITY =
            BLOCK_ENTITIES.register(
                    "sensor_polyp",
                    () -> BlockEntityType.Builder.of(SensorPolypBlockEntity::new, SENSOR_POLYP.get())
                            .build(null));

    private ControlModule() {}

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
    }
}
