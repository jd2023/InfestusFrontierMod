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
    public static final RegistryObject<Block> CORRUPT_CHUNK = BLOCKS.register("corrupt_chunk", () -> new Block(BlockBehaviour.Properties.copy(Blocks.SHROOMLIGHT)));
    public static final RegistryObject<Block> INFESTUS_NETWORK = BLOCKS.register(InfestusNetwork.ID, InfestusNetwork::new);
    public static final RegistryObject<Block> INFESTUS_NETWORK_DEAD = BLOCKS.register(InfestusNetworkDead.ID, InfestusNetworkDead::new);
    public static final RegistryObject<Block> INFESTED_VEIN_VAPORIZER = BLOCKS.register(InfestedVeinVaporizer.ID, InfestedVeinVaporizer::new);
    public static final RegistryObject<Block> INFESTUS_NETWORK_DENSE = BLOCKS.register(InfestusNetworkDense.ID, InfestusNetworkDense::new);
    public static final RegistryObject<Block> INFESTUS_NETWORK_ADVANCED = BLOCKS.register(InfestusNetworkAdvanced.ID, InfestusNetworkAdvanced::new);
    public static final RegistryObject<Block> INFESTUS_NETWORK_FINAL = BLOCKS.register(InfestusNetworkFinal.ID, InfestusNetworkFinal::new);
    public static final RegistryObject<Block> CORRUPTION_CORE = BLOCKS.register(CorruptionCore.ID, CorruptionCore::new);
    public static final RegistryObject<Block> BIO_RESERVOIR = BLOCKS.register(BioReservoir.ID, BioReservoir::new);
    public static final RegistryObject<Block> INFESTUS_POD = BLOCKS.register(InfestusPod.ID, InfestusPod::new);
    public static final RegistryObject<Block> EXCAVATED = BLOCKS.register(ExcavatedBlock.ID, ExcavatedBlock::new);
    public static final RegistryObject<Block> LOOT_LEECH = BLOCKS.register(InfesusLootLeech.ID, InfesusLootLeech::new);

    public static final RegistryObject<Block> LUMINESCENT_INFESTUS_POD = BLOCKS.register(LuminescentInfestusPod.ID, LuminescentInfestusPod::new);

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
