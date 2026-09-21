package org.jd.infestusfrontier.construction;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jd.infestusfrontier.InfestusFrontier;
import org.jd.infestusfrontier.construction.api.CultureUse;

final class ConstructionContent {
    static final String SPORE_CULTURE = "construction/spore_culture";
    static final String ORGAN_BUD = "construction/organ_bud";
    static final String SEED_POUCH = "construction/seed_pouch";
    static final String LIVING_SKIN = "construction/living_skin";
    static final String LIVING_SKIN_SLAB = "construction/living_skin_slab";
    static final String LIVING_SKIN_STAIRS = "construction/living_skin_stairs";
    static final String LIVING_SKIN_COVERING = "construction/living_skin_covering";
    static final String RIB_FRAME = "construction/rib_frame";
    static final String MEMBRANE_WINDOW = "construction/membrane_window";

    private final DeferredRegister.Blocks blocks = DeferredRegister.createBlocks(InfestusFrontier.MOD_ID);
    private final DeferredRegister.Items items = DeferredRegister.createItems(InfestusFrontier.MOD_ID);
    private final DeferredRegister<BlockEntityType<?>> entities = DeferredRegister.create(
            Registries.BLOCK_ENTITY_TYPE, InfestusFrontier.MOD_ID);
    private final DeferredBlock<OrganBudBlock> organBud;
    private final DeferredBlock<SeedPouchBlock> seedPouch;
    private final java.util.function.Supplier<BlockEntityType<SeedPouchEntity>> seedPouchEntity;
    @SuppressWarnings("unused")
    private final DeferredItem<Item> sporeCulture;
    @SuppressWarnings("unused")
    private final DeferredItem<BlockItem> organBudItem;

    ConstructionContent(BudRecipeRegistry budRecipes, CultureUse cultureUse) {
        organBud = blocks.registerBlock(
                ORGAN_BUD,
                properties -> new OrganBudBlock(properties, budRecipes),
                BlockBehaviour.Properties.of()
                        .mapColor(MapColor.COLOR_RED)
                        .strength(0.4F)
                        .sound(SoundType.WART_BLOCK)
                        .noOcclusion());
        seedPouch = blocks.register(SEED_POUCH, this::createSeedPouch);
        seedPouchEntity = entities.register(SEED_POUCH, this::createSeedPouchEntity);
        var skin = blocks.registerSimpleBlock(LIVING_SKIN, BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_RED).strength(0.8F).sound(SoundType.WART_BLOCK));
        var skinSlab = blocks.register(LIVING_SKIN_SLAB, () -> new SlabBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_RED).strength(0.8F).sound(SoundType.WART_BLOCK)));
        var skinStairs = blocks.register(LIVING_SKIN_STAIRS, () -> new StairBlock(skin.get().defaultBlockState(),
                BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(0.8F).sound(SoundType.WART_BLOCK)));
        var skinCovering = blocks.register(LIVING_SKIN_COVERING, () -> new LivingSkinCoveringBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_RED).strength(0.2F).sound(SoundType.WART_BLOCK).noOcclusion()));
        var ribs = blocks.register(RIB_FRAME, () -> new RibFrameBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.SAND).strength(1.5F).sound(SoundType.BONE_BLOCK).noOcclusion()));
        var window = blocks.register(MEMBRANE_WINDOW, () -> new MembraneWindowBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_LIGHT_BLUE).strength(0.5F).sound(SoundType.GLASS)
                .noOcclusion().isViewBlocking((state, level, pos) -> false)));
        sporeCulture = items.register(SPORE_CULTURE,
                () -> new SporeCultureItem(new Item.Properties(), cultureUse));
        organBudItem = items.registerSimpleBlockItem(organBud, new Item.Properties());
        items.register(SEED_POUCH, () -> new BlockItem(seedPouch.get(), new Item.Properties().stacksTo(1)));
        items.register(LIVING_SKIN, () -> new LoadedAreaBlockItem(skin.get(), new Item.Properties()));
        items.register(LIVING_SKIN_SLAB, () -> new LoadedAreaBlockItem(skinSlab.get(), new Item.Properties()));
        items.register(LIVING_SKIN_STAIRS, () -> new LoadedAreaBlockItem(skinStairs.get(), new Item.Properties()));
        items.register(LIVING_SKIN_COVERING, () -> new LoadedAreaBlockItem(skinCovering.get(), new Item.Properties()));
        items.register(RIB_FRAME, () -> new LoadedAreaBlockItem(ribs.get(), new Item.Properties()));
        items.register(MEMBRANE_WINDOW, () -> new MembraneWindowItem(window.get(), new Item.Properties()));
    }

    private SeedPouchBlock createSeedPouch() {
        return new SeedPouchBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).strength(0.6F)
                .sound(SoundType.WOOL).noOcclusion(), seedPouchEntity);
    }

    private BlockEntityType<SeedPouchEntity> createSeedPouchEntity() {
        return BlockEntityType.Builder.of(
                (pos, state) -> new SeedPouchEntity(pos, state, seedPouchEntity), seedPouch.get()).build(null);
    }

    private void allowPouchReserveControl(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock event) {
        var level = event.getLevel();
        if (event.getItemStack().is(net.minecraft.world.item.Items.STICK) && level.hasChunkAt(event.getPos())
                && level.getBlockState(event.getPos()).is(seedPouch.get())) {
            event.setUseBlock(net.neoforged.neoforge.common.util.TriState.TRUE);
        }
    }

    void register(IEventBus modBus) {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(this::allowPouchReserveControl);
        blocks.register(modBus);
        items.register(modBus);
        entities.register(modBus);
    }
}
