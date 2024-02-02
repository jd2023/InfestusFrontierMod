package org.jd.infestusfrontier;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jd.infestusfrontier.block.*;

public class ZgBlocks {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, InfestusFrontier.MODID);
    public static final RegistryObject<Block> LIGHT_INFESTUS_POD = BLOCKS.register(LightInfestusPod.ID, LightInfestusPod::new);
    public static final RegistryObject<Block> CORRUPT_CHUNK = BLOCKS.register("corrupt_chunk", () -> new Block(BlockBehaviour.Properties.copy(Blocks.SHROOMLIGHT)));
    public static final RegistryObject<Block> INFESTUS_NETWORK = BLOCKS.register(InfestusNetwork.ID, InfestusNetwork::new);
    public static final RegistryObject<Block> corruption_core = BLOCKS.register(CorruptionCore.ID, CorruptionCore::new);
    public static final RegistryObject<Block> BIO_RESERVOIR = BLOCKS.register(BioReservoir.ID, BioReservoir::new);
    public static final RegistryObject<Block> INFESTUS_POD = BLOCKS.register(InfestusPod.ID, InfestusPod::new);

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
