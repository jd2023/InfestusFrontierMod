package org.jd.infestusfrontier;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import org.jd.infestusfrontier.block.InfestusBlocks;
import org.jd.infestusfrontier.block.InfestusBlockEntities;

import org.jd.infestusfrontier.item.InfestusItems;
import org.jd.infestusfrontier.item.InfestusTabs;
import org.jd.infestusfrontier.recipe.InfestusRecipes;
import org.jd.infestusfrontier.screen.CollectorScreen;
import org.jd.infestusfrontier.screen.InfestusMenuTypes;
import org.jd.infestusfrontier.screen.MutationPoolMenu;
import org.jd.infestusfrontier.screen.MutationPoolScreen;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(InfestusFrontier.MODID)
public class InfestusFrontier
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "infestusfrontier";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold Blocks which will all be registered under the "examplemod" namespace


    public InfestusFrontier()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register the Deferred Register to the mod event bus so blocks get registered
        InfestusBlocks.register(modEventBus);
        // Register the Deferred Register to the mod event bus so items get registered
        InfestusItems.register(modEventBus);
        // Register the Deferred Register to the mod event bus so tabs get registered
        InfestusTabs.register(modEventBus);
        InfestusBlockEntities.register(modEventBus);
        InfestusMenuTypes.register(modEventBus);
        InfestusRecipes.register(modEventBus);



        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        // Register the item to a creative tab
        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
    public static ResourceLocation createResource(String s){
        return new ResourceLocation(MODID, s);
    }


    private void commonSetup(final FMLCommonSetupEvent event)
    {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");

        if (Config.logDirtBlock)
            LOGGER.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));

        LOGGER.info(Config.magicNumberIntroduction + Config.magicNumber);

        Config.items.forEach((item) -> LOGGER.info("ITEM >> {}", item.toString()));
    }

    // Add the example block item to the building blocks tab


    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            // Some client setup code
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
            ItemBlockRenderTypes.setRenderLayer(InfestusBlocks.BIOMASS_RESERVOIR.get(), RenderType.translucent());

            MenuScreens.register(InfestusMenuTypes.COLLECTOR_MENU.get(), CollectorScreen::new);

            MenuScreens.register(InfestusMenuTypes.MUTATION_POOL_MENU.get(), MutationPoolScreen::new);


        }
    }
}
