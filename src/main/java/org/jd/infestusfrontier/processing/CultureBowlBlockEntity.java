package org.jd.infestusfrontier.processing;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.core.foundation.Outcome;
import org.jd.infestusfrontier.core.foundation.Refusal;
import org.jd.infestusfrontier.core.organ.OrganHistory;
import org.jd.infestusfrontier.core.storage.BiomassTank;
import org.jd.infestusfrontier.ecology.EcologyModule;
import org.jd.infestusfrontier.foundation.Persistence;
import org.jd.infestusfrontier.organ.api.OrganCodecs;
import org.jd.infestusfrontier.storage.api.BiomassCodecs;
import org.jd.infestusfrontier.storage.api.BiomassHandler;

/**
 * What the Culture Bowl holds: nine ingredient slots of 64, one output slot, 2000 mB of water and 2000 BU of biomass.
 * Everything is loaded by hand and travels with the block when it is broken and placed again.
 */
public final class CultureBowlBlockEntity extends BlockEntity implements BiomassHandler {
    public static final int INGREDIENT_SLOTS = 9;
    static final Biomass CAPACITY = Biomass.of(2000);
    static final int WATER_CAPACITY = 2000;
    static final Refusal NO_SLOT = new Refusal("processing.no_slot");
    /** Items the bowl takes for now. IF-015 replaces this set with the culturing recipes' ingredients. */
    private static final Set<Item> ACCEPTED =
            Set.of(Items.RED_MUSHROOM, Items.WHEAT_SEEDS, Items.ROTTEN_FLESH, Items.BONE_MEAL);

    /** The saved and carried form of a bowl. */
    public record Contents(
            List<ItemStack> ingredients, ItemStack output, int waterMb, BiomassTank biomass, OrganHistory history) {
        public static final Codec<Contents> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                        ItemStack.OPTIONAL_CODEC.listOf().fieldOf("ingredients").forGetter(Contents::ingredients),
                        ItemStack.OPTIONAL_CODEC.fieldOf("output").forGetter(Contents::output),
                        Codec.intRange(0, WATER_CAPACITY).fieldOf("water").forGetter(Contents::waterMb),
                        BiomassCodecs.TANK.fieldOf("biomass").forGetter(Contents::biomass),
                        OrganCodecs.HISTORY.fieldOf("history").forGetter(Contents::history))
                .apply(instance, Contents::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, Contents> STREAM_CODEC =
                ByteBufCodecs.fromCodecWithRegistries(CODEC);
    }

    private static final MapCodec<Contents> SAVED = Contents.CODEC.fieldOf("contents");

    private final ItemStackHandler ingredients = new ItemStackHandler(INGREDIENT_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final ItemStackHandler output = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final FluidTank water = new FluidTank(WATER_CAPACITY, stack -> stack.is(Fluids.WATER)) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private BiomassTank biomass = BiomassTank.empty(CAPACITY);
    private OrganHistory history = OrganHistory.NONE;
    private Optional<CompoundTag> unreadable = Optional.empty();

    CultureBowlBlockEntity(BlockPos pos, BlockState state) {
        super(ProcessingModule.CULTURE_BOWL_ENTITY.get(), pos, state);
    }

    FluidTank water() {
        return water;
    }

    ItemStackHandler ingredients() {
        return ingredients;
    }

    /** True when the bowl takes this item at all. A refused item is not the bowl's business: the player builds. */
    boolean accepts(ItemStack stack) {
        return !stack.isEmpty() && (ACCEPTED.contains(stack.getItem()) || stack.is(EcologyModule.SPORE_CULTURE.get()));
    }

    /** Moves one item from {@code stack} into the first slot that holds the same item and has room, else the first empty slot. */
    Optional<Refusal> insert(ItemStack stack) {
        if (unreadable.isPresent()) return Optional.of(Persistence.CORRUPT);
        ItemStack one = stack.copyWithCount(1);
        for (int pass = 0; pass < 2; pass++) {
            for (int slot = 0; slot < INGREDIENT_SLOTS; slot++) {
                ItemStack held = ingredients.getStackInSlot(slot);
                if ((pass == 0) == held.isEmpty()) continue;
                if (ingredients.insertItem(slot, one, false).isEmpty()) {
                    stack.shrink(1);
                    return Optional.empty();
                }
            }
        }
        return Optional.of(NO_SLOT);
    }

    /** Gives the player the output stack, else the first non-empty ingredient stack. Returns whether anything moved. */
    boolean collect(Player player) {
        ItemStack taken = output.getStackInSlot(0);
        if (!taken.isEmpty()) {
            output.setStackInSlot(0, ItemStack.EMPTY);
        } else {
            taken = ItemStack.EMPTY;
            for (int slot = 0; slot < INGREDIENT_SLOTS && taken.isEmpty(); slot++) {
                taken = ingredients.getStackInSlot(slot);
                if (!taken.isEmpty()) ingredients.setStackInSlot(slot, ItemStack.EMPTY);
            }
        }
        if (taken.isEmpty()) return false;
        player.getInventory().placeItemBackInInventory(taken);
        return true;
    }

    /** Tells the player what the bowl holds. */
    void report(Player player) {
        int items = 0;
        for (int slot = 0; slot < INGREDIENT_SLOTS; slot++)
            items += ingredients.getStackInSlot(slot).getCount();
        player.displayClientMessage(
                Component.translatable(
                        "infestusfrontier.status.culture_bowl.idle",
                        items,
                        water.getFluidAmount(),
                        biomass.stored().units(),
                        biomass.capacity().units()),
                true);
    }

    @Override
    public BiomassTank tank() {
        return biomass;
    }

    @Override
    public Outcome<BiomassTank> insert(Biomass amount, boolean simulate) {
        return apply(biomass.fill(amount), simulate);
    }

    @Override
    public Outcome<BiomassTank> extract(Biomass amount, boolean simulate) {
        return apply(biomass.drain(amount), simulate);
    }

    private Outcome<BiomassTank> apply(Outcome<BiomassTank> change, boolean simulate) {
        if (unreadable.isPresent()) return Outcome.refused(Persistence.CORRUPT);
        if (!simulate && change instanceof Outcome.Done<BiomassTank> done) {
            biomass = done.value();
            setChanged();
        }
        return change;
    }

    private Contents contents() {
        List<ItemStack> held = new ArrayList<>();
        for (int slot = 0; slot < INGREDIENT_SLOTS; slot++)
            held.add(ingredients.getStackInSlot(slot).copy());
        return new Contents(held, output.getStackInSlot(0).copy(), water.getFluidAmount(), biomass, history);
    }

    private void restore(Contents saved) {
        for (int slot = 0; slot < INGREDIENT_SLOTS; slot++)
            ingredients.setStackInSlot(
                    slot,
                    slot < saved.ingredients().size()
                            ? saved.ingredients().get(slot).copy()
                            : ItemStack.EMPTY);
        output.setStackInSlot(0, saved.output().copy());
        water.setFluid(saved.waterMb() > 0 ? new FluidStack(Fluids.WATER, saved.waterMb()) : FluidStack.EMPTY);
        if (saved.biomass().capacity().equals(CAPACITY)) biomass = saved.biomass();
        history = saved.history();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        Optional<Contents> read = Persistence.read(SAVED, tag, ProcessingModule.LOGGER, "Culture Bowl");
        unreadable = read.isPresent() ? Optional.empty() : Optional.of(tag.copy());
        read.ifPresent(this::restore);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (unreadable.isPresent()) tag.merge(unreadable.get());
        else Persistence.write(SAVED, contents(), tag);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(ProcessingModule.BOWL_CONTENTS.get(), contents());
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        Contents carried = input.get(ProcessingModule.BOWL_CONTENTS.get());
        if (carried != null) restore(carried);
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        tag.remove("contents");
    }
}
