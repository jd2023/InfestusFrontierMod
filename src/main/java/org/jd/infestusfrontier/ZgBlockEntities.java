package org.jd.infestusfrontier;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jd.infestusfrontier.block.Nest;
import org.jd.infestusfrontier.block.NestBlockEntity;

public class ZgBlockEntities {
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, InfestusFrontier.MODID);

    public static final RegistryObject<BlockEntityType<NestBlockEntity>> NEST_BLOCK_ENTITY_TYPE = BLOCK_ENTITY_TYPES.register(Nest.ID, () -> BlockEntityType.Builder.of(NestBlockEntity::new, ZgBlocks.NEST.get()).build(null));
    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITY_TYPES.register(modEventBus);
    }
}