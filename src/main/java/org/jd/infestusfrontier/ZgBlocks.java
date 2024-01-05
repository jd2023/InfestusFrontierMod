package org.jd.infestusfrontier;

import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jd.infestusfrontier.blocks.Creep;
import org.jd.infestusfrontier.blocks.CreepTwo;
import org.jd.infestusfrontier.blocks.LightSpreader;
import org.jd.infestusfrontier.blocks.Nest;

public class ZgBlocks {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, InfestusFrontier.MODID);
    public static final RegistryObject<Block> LIGHT_SPREADER = BLOCKS.register(LightSpreader.ID, LightSpreader::new);
    public static final RegistryObject<Block> CREEP = BLOCKS.register(Creep.ID, Creep::new);
    public static final RegistryObject<Block> CREEP_TWO = BLOCKS.register(CreepTwo.ID, CreepTwo::new);
    public static final RegistryObject<Block> NEST = BLOCKS.register(Nest.ID, Nest::new);

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}
