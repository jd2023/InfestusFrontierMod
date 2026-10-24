package org.jd.infestusfrontier.ecology;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jd.infestusfrontier.InfestusFrontier;

/** Owner of substrate maturity, function, lining, reinforcement, dye, deliberate conversion and living walls. */
public final class EcologyModule {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(InfestusFrontier.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(InfestusFrontier.MOD_ID);

    public static final DeferredBlock<LivingSubstrateBlock> LIVING_SUBSTRATE = BLOCKS.registerBlock(
            "living_substrate",
            LivingSubstrateBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_PINK)
                    .strength(0.6F)
                    .sound(SoundType.GRAVEL)
                    .pushReaction(PushReaction.BLOCK));
    public static final DeferredItem<BlockItem> LIVING_SUBSTRATE_ITEM = ITEMS.registerSimpleBlockItem(LIVING_SUBSTRATE);
    public static final DeferredItem<Item> SPORE_CULTURE = ITEMS.registerItem("spore_culture", SporeCultureItem::new);
    /**
     * What a grown cell leaves when it is broken: Living Substrate and everything grown in it die and never move. The
     * Digestive Sac digests it back into a little biomass.
     */
    public static final DeferredItem<Item> DEAD_TISSUE = ITEMS.registerSimpleItem("dead_tissue");

    /** The three wall tiers: soft as grown, bone-ribbed, and calcified past any explosion. */
    public static final DeferredBlock<LivingWallBlock> LIVING_WALL =
            wall("living_wall", MapColor.TERRACOTTA_PINK, 1.5F, 3.0F, SoundType.GRAVEL);

    public static final DeferredBlock<LivingWallBlock> RIBBED_WALL =
            wall("ribbed_wall", MapColor.TERRACOTTA_WHITE, 4.0F, 9.0F, SoundType.BONE_BLOCK);
    public static final DeferredBlock<LivingWallBlock> CALCIFIED_WALL =
            wall("calcified_wall", MapColor.SAND, 25.0F, 1200.0F, SoundType.BONE_BLOCK);
    public static final DeferredItem<BlockItem> LIVING_WALL_ITEM = ITEMS.registerSimpleBlockItem(LIVING_WALL);
    public static final DeferredItem<BlockItem> RIBBED_WALL_ITEM = ITEMS.registerSimpleBlockItem(RIBBED_WALL);
    public static final DeferredItem<BlockItem> CALCIFIED_WALL_ITEM = ITEMS.registerSimpleBlockItem(CALCIFIED_WALL);

    /** Hardens a Living Wall into a Ribbed Wall (ITEM_CATALOG I050). */
    public static final DeferredItem<Item> SKELETAL_GRAFT =
            ITEMS.registerItem("skeletal_graft", properties -> new WallGraftItem(properties, LIVING_WALL, RIBBED_WALL));
    /** Hardens a Ribbed Wall into a Calcified Wall (ITEM_CATALOG I131). */
    public static final DeferredItem<Item> CALCIFYING_GRAFT = ITEMS.registerItem(
            "calcifying_graft", properties -> new WallGraftItem(properties, RIBBED_WALL, CALCIFIED_WALL));

    private EcologyModule() {}

    private static DeferredBlock<LivingWallBlock> wall(
            String id, MapColor colour, float hardness, float blastResistance, SoundType sound) {
        return BLOCKS.registerBlock(
                id,
                LivingWallBlock::new,
                BlockBehaviour.Properties.of()
                        .mapColor(colour)
                        .strength(hardness, blastResistance)
                        .sound(sound)
                        .pushReaction(PushReaction.BLOCK));
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        // vanilla skips a block's own use while its user sneaks with an item, so starting a wall listens here
        NeoForge.EVENT_BUS.addListener((PlayerInteractEvent.RightClickBlock event) -> {
            if (LivingWallBlock.start(
                    event.getLevel(), event.getPos(), event.getFace(), event.getEntity(), event.getItemStack())) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
            }
        });
    }
}
