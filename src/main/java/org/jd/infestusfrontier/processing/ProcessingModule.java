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
import org.jd.infestusfrontier.discovery.api.DiscoveryObserver;

/** Registers only the Bowl and products with implemented Bowl producers. */
public final class ProcessingModule {
    private final DeferredRegister.Blocks blocks = DeferredRegister.createBlocks("infestusfrontier");
    private final DeferredRegister.Items items = DeferredRegister.createItems("infestusfrontier");
    private final DeferredRegister<BlockEntityType<?>> entities = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, "infestusfrontier");
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, "infestusfrontier");
    private final java.util.function.Supplier<CultureBowlBlock> bowl;
    private final java.util.function.Supplier<BlockEntityType<CultureBowlEntity>> bowlEntity;
    private final DiscoveryObserver discovery;
    public static final java.util.function.Supplier<MenuType<CultureBowlMenu>> BOWL_MENU = MENUS.register("processing/culture_bowl",
            () -> IMenuTypeExtension.create((id, inventory, buffer) -> new CultureBowlMenu(id, inventory,
                    buffer.readBlockPos(), org.jd.infestusfrontier.processing.menu.CultureBowlMenuSnapshot.CODEC.decode(buffer))));

    public ProcessingModule(DiscoveryObserver discovery) {
        this.discovery = discovery;
        bowl = blocks.register("processing/culture_bowl", this::createBowl);
        bowlEntity = entities.register("processing/culture_bowl", this::createBowlEntityType);
    }

    private CultureBowlBlock createBowl() {
        return new CultureBowlBlock(BlockBehaviour.Properties.of().strength(0.6F).noOcclusion()
                .sound(SoundType.WART_BLOCK).pushReaction(PushReaction.BLOCK), bowlEntity, discovery);
    }

    private BlockEntityType<CultureBowlEntity> createBowlEntityType() {
        return BlockEntityType.Builder.of(
                (pos, state) -> new CultureBowlEntity(pos, state, bowlEntity, discovery), bowl.get()).build(null);
    }

    public void register(IEventBus bus) {
        items.register("processing/culture_bowl", () -> new CultureBowlItem(bowl.get(), new Item.Properties().stacksTo(1)));
        for (String name : new String[] {"elastic_gel", "nutrient_mash", "honey_culture", "rooting_gel"}) {
            items.register("processing/" + name, () -> new Item(new Item.Properties()));
        }
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(this::allowBowlControls);
        bus.addListener(ProcessingModule::registerPayloads);
        blocks.register(bus);
        items.register(bus);
        entities.register(bus);
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
    private void allowBowlControls(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock event) {
        var level = event.getLevel();
        if (level.hasChunkAt(event.getPos()) && level.getBlockState(event.getPos()).is(bowl.get())) {
            event.setUseBlock(net.neoforged.neoforge.common.util.TriState.TRUE);
        }
    }
}
