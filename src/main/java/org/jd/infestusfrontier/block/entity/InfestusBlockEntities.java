package org.jd.infestusfrontier.block.entity;



import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import org.jd.infestusfrontier.InfestusFrontier;

public class InfestusBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, InfestusFrontier.MODID);



    public static void register(IEventBus bus){
        BLOCK_ENTITIES.register(bus);
    }
}
