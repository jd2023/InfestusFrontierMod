package org.jd.infestusfrontier.processing;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jd.infestusfrontier.processing.menu.BowlIntentPayload;
import org.jd.infestusfrontier.processing.menu.BowlSnapshotPayload;
import org.jd.infestusfrontier.processing.menu.CultureBowlMenu;

/** Registers only the Bowl and products with implemented Bowl producers. */
public final class ProcessingModule {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks("infestusfrontier");
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("infestusfrontier");
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, "infestusfrontier");
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, "infestusfrontier");
    static final java.util.function.Supplier<CultureBowlBlock> BOWL = BLOCKS.register("processing/culture_bowl",
            () -> new CultureBowlBlock(BlockBehaviour.Properties.of().strength(0.6F).noOcclusion()
                    .sound(SoundType.WART_BLOCK).pushReaction(PushReaction.BLOCK)));
    static final java.util.function.Supplier<BlockEntityType<CultureBowlEntity>> BOWL_ENTITY = ENTITIES.register("processing/culture_bowl",
            () -> BlockEntityType.Builder.of(CultureBowlEntity::new, BOWL.get()).build(null));
    public static final java.util.function.Supplier<MenuType<CultureBowlMenu>> BOWL_MENU = MENUS.register("processing/culture_bowl",
            () -> IMenuTypeExtension.create((id, inventory, buffer) -> new CultureBowlMenu(id, inventory,
                    buffer.readBlockPos(), org.jd.infestusfrontier.processing.menu.CultureBowlMenuSnapshot.CODEC.decode(buffer))));

    public void register(IEventBus bus) {
        ITEMS.register("processing/culture_bowl", () -> new CultureBowlItem(BOWL.get(), new Item.Properties().stacksTo(1)));
        for (String name : new String[] {"elastic_gel", "nutrient_mash", "honey_culture", "rooting_gel"}) {
            ITEMS.register("processing/" + name, () -> new Item(new Item.Properties()));
        }
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(ProcessingModule::allowBowlControls);
        bus.addListener(ProcessingModule::registerPayloads);
        BLOCKS.register(bus);
        ITEMS.register(bus);
        ENTITIES.register(bus);
        MENUS.register(bus);
    }
    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToServer(BowlIntentPayload.TYPE, BowlIntentPayload.CODEC, (payload, context) -> {
            if (context.player() instanceof net.minecraft.server.level.ServerPlayer player
                    && player.containerMenu instanceof CultureBowlMenu menu
                    && menu.containerId == payload.menuId()) menu.handleIntent(player, payload);
        });
        registrar.playToClient(BowlSnapshotPayload.TYPE, BowlSnapshotPayload.CODEC, (payload, context) -> {
            if (context.player().containerMenu instanceof CultureBowlMenu menu && menu.containerId == payload.menuId()) {
                menu.acceptSnapshot(payload.snapshot());
            }
        });
    }
    private static void allowBowlControls(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock event) {
        var level = event.getLevel();
        if (level.hasChunkAt(event.getPos()) && level.getBlockState(event.getPos()).is(BOWL.get())) {
            event.setUseBlock(net.neoforged.neoforge.common.util.TriState.TRUE);
        }
    }
}
