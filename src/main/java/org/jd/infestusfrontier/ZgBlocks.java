package org.jd.infestusfrontier;

import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jd.infestusfrontier.block.*;

public class ZgBlocks {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, InfestusFrontier.MODID);
    public static final RegistryObject<Block> LIGHT_SPREADER = BLOCKS.register(LightSpreader.ID, LightSpreader::new);
    public static final RegistryObject<Block> FLESH_BLOCK = BLOCKS.register(FleshBlock.ID, FleshBlock::new);
    public static final RegistryObject<Block> CREEP = BLOCKS.register(Creep.ID, Creep::new);
    public static final RegistryObject<Block> CREEP_TWO = BLOCKS.register(CreepTwo.ID, CreepTwo::new);
    public static final RegistryObject<Block> NEST = BLOCKS.register(Nest.ID, Nest::new);
    public static final RegistryObject<Block> BIO_RESERVOIR = BLOCKS.register(BioReservoir.ID, BioReservoir::new);
    public static final RegistryObject<Block> TUMOR = BLOCKS.register(Tumor.ID, Tumor::new);

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}
