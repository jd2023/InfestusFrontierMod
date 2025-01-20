package org.jd.infestusfrontier.block;



import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jd.infestusfrontier.InfestusFrontier;
import org.jd.infestusfrontier.block.custom.CorruptionCoreBlock;
import org.jd.infestusfrontier.block.custom.MutationPoolBlock;
import org.jd.infestusfrontier.block.entity.CorruptionCoreBlockEntity;
import org.jd.infestusfrontier.block.entity.MutationPoolBlockEntity;

public class InfestusBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, InfestusFrontier.MODID);

    public static final RegistryObject<BlockEntityType<CorruptionCoreBlockEntity>> CORRUPTION_CORE_ENTITY = BLOCK_ENTITIES.register(
            CorruptionCoreBlock.ID,
            () -> BlockEntityType.Builder.of(CorruptionCoreBlockEntity::new, InfestusBlocks.CORRUPTION_CORE.get()).build(null));
//    public static final RegistryObject<BlockEntityType<MutationPoolBlockEntity>> MUTATION_POOL_ENTITY=BLOCK_ENTITIES.register(MutationPoolBlock.ID
//            ,()-> BlockEntityType.Builder.of(MutationPoolBlockEntity::new, InfestusBlocks.MUTATION_POOL.get()).build(null));

    public static void register(IEventBus bus){
        BLOCK_ENTITIES.register(bus);
    }
}
