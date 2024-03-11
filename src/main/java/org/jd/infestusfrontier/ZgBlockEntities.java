package org.jd.infestusfrontier;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jd.infestusfrontier.block.BioReservoir;
import org.jd.infestusfrontier.block.CorruptionCore;
import org.jd.infestusfrontier.block.InfestusPod;
import org.jd.infestusfrontier.block.entity.BioReservoirBlockEntity;
import org.jd.infestusfrontier.block.entity.CorruptionCoreBlockEntity;
import org.jd.infestusfrontier.block.entity.InfestusPodBlockEntity;

public class ZgBlockEntities {
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, InfestusFrontier.MODID);

    public static final RegistryObject<BlockEntityType<InfestusPodBlockEntity>> INFESTUS_POD_BLOCK_ENTITY_TYPE = BLOCK_ENTITY_TYPES.register(InfestusPod.ID,
            () -> BlockEntityType.Builder.of(InfestusPodBlockEntity::new, ZgBlocks.INFESTUS_POD.get()).build(null));

    public static final RegistryObject<BlockEntityType<CorruptionCoreBlockEntity>> CORRUPTION_CORE_ENTITY_TYPE = BLOCK_ENTITY_TYPES.register(CorruptionCore.ID,
            () -> BlockEntityType.Builder.of(CorruptionCoreBlockEntity::new, ZgBlocks.CORRUPTION_CORE.get()).build(null));

    public static final RegistryObject<BlockEntityType<BioReservoirBlockEntity>> BIOMASS_RESERVOIR = BLOCK_ENTITY_TYPES.register(BioReservoir.ID,
            () -> BlockEntityType.Builder.of(BioReservoirBlockEntity::new, ZgBlocks.BIO_RESERVOIR.get()).build(null));

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITY_TYPES.register(modEventBus);
    }
}
