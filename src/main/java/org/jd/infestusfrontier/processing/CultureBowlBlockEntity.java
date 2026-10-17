package org.jd.infestusfrontier.processing;

import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.core.foundation.Outcome;
import org.jd.infestusfrontier.core.foundation.Refusal;
import org.jd.infestusfrontier.core.organ.OrganHistory;
import org.jd.infestusfrontier.core.processing.Batch;
import org.jd.infestusfrontier.core.storage.BiomassTank;
import org.jd.infestusfrontier.foundation.Persistence;
import org.jd.infestusfrontier.organ.api.Organs;
import org.jd.infestusfrontier.processing.api.CultureBowlContents;
import org.jd.infestusfrontier.processing.api.OrganBatchRecipe;
import org.jd.infestusfrontier.storage.api.BiomassHandler;

/**
 * What the Culture Bowl holds: nine ingredient slots of 64, one output slot, 2000 mB of water and 2000 BU of biomass.
 * Loaded by hand, it runs one batch at a time. Starting consumes the inputs at once and the output slot is checked
 * first, so completion can never be refused. Work advances only while loaded and rooted on a foundation.
 */
final class CultureBowlBlockEntity extends BlockEntity implements BiomassHandler {
    static final int INGREDIENT_SLOTS = 9;
    static final Biomass CAPACITY = Biomass.of(2000);
    static final int WATER_CAPACITY = CultureBowlContents.WATER_CAPACITY;
    static final String ORGAN = "culture_bowl";
    static final Refusal NO_SLOT = new Refusal("processing.no_slot");
    static final Refusal BUSY = new Refusal("processing.busy");
    static final Refusal NO_RECIPE = new Refusal("processing.no_recipe");
    static final Refusal OUTPUT_FULL = new Refusal("processing.output_full");
    static final Refusal COMMITTED = new Refusal("processing.committed");

    private static final MapCodec<CultureBowlContents> SAVED = CultureBowlContents.CODEC.fieldOf("contents");

    private Optional<CompoundTag> unreadable = Optional.empty();
    private final ItemStackHandler ingredients = new ItemStackHandler(INGREDIENT_SLOTS) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return !stack.isEmpty()
                    && recipes().anyMatch(recipe -> recipe.value().uses(stack));
        }

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
    private final FluidTank water =
            new FluidTank(WATER_CAPACITY, stack -> stack.is(Fluids.WATER) && unreadable.isEmpty()) {
                @Override
                protected void onContentsChanged() {
                    setChanged();
                }
            };
    private BiomassTank biomass = BiomassTank.empty(CAPACITY);
    private OrganHistory history = OrganHistory.NONE;
    private Optional<OrganBatch> batch = Optional.empty();

    CultureBowlBlockEntity(BlockPos pos, BlockState state) {
        super(ProcessingModule.CULTURE_BOWL_ENTITY.get(), pos, state);
    }

    FluidTank water() {
        return water;
    }

    ItemStackHandler ingredients() {
        return ingredients;
    }

    /** The bowl's recipes. Called only on a player's action, never in a tick. */
    private Stream<RecipeHolder<OrganBatchRecipe>> recipes() {
        if (level == null) return Stream.empty();
        return level.getRecipeManager().getAllRecipesFor(ProcessingModule.ORGAN_BATCH.get()).stream()
                .filter(recipe -> recipe.value().organ().equals(ORGAN));
    }

    int percent() {
        return batch.map(b -> b.batch().percent()).orElse(0);
    }

    boolean isEmpty() {
        if (batch.isPresent()
                || water.getFluidAmount() > 0
                || !biomass.stored().isZero()
                || !output.getStackInSlot(0).isEmpty()) return false;
        for (int slot = 0; slot < INGREDIENT_SLOTS; slot++)
            if (!ingredients.getStackInSlot(slot).isEmpty()) return false;
        return true;
    }

    static void serverTick(Level level, BlockPos pos, BlockState state, CultureBowlBlockEntity bowl) {
        if (bowl.batch.isEmpty() || bowl.unreadable.isPresent() || !Organs.hasFoundation(level, pos)) return;
        OrganBatch advanced = bowl.batch.get().advance();
        if (advanced.isDone()) {
            if (!bowl.output.insertItem(0, advanced.result().copy(), false).isEmpty()) return;
            bowl.history = bowl.history.completed();
            bowl.batch = Optional.empty();
        } else {
            bowl.batch = Optional.of(advanced);
        }
        bowl.setChanged();
    }

    /**
     * Starts the first matching batch: consumes every ingredient, the water and the biomass now, and reserves the
     * result in the output slot. A refusal changes nothing.
     */
    Optional<Refusal> start(Player player) {
        if (unreadable.isPresent()) return Optional.of(Persistence.CORRUPT);
        if (batch.isPresent()) return Optional.of(BUSY);
        List<ItemStack> held = new ArrayList<>();
        for (int slot = 0; slot < INGREDIENT_SLOTS; slot++) held.add(ingredients.getStackInSlot(slot));
        RecipeInput input = new RecipeInput() {
            @Override
            public ItemStack getItem(int slot) {
                return held.get(slot);
            }

            @Override
            public int size() {
                return held.size();
            }
        };
        Optional<RecipeHolder<OrganBatchRecipe>> found = recipes()
                .filter(recipe -> recipe.value().matches(input, level))
                .filter(recipe -> water.getFluidAmount() >= recipe.value().waterMb())
                .filter(recipe -> biomass.stored().atLeast(recipe.value().biomass()))
                .findFirst();
        if (found.isEmpty()) return Optional.of(NO_RECIPE);
        OrganBatchRecipe recipe = found.get().value();
        if (!output.insertItem(0, recipe.result().copy(), true).isEmpty()) return Optional.of(OUTPUT_FULL);
        for (int slot = 0; slot < INGREDIENT_SLOTS; slot++) ingredients.setStackInSlot(slot, ItemStack.EMPTY);
        water.drain(recipe.waterMb(), IFluidHandler.FluidAction.EXECUTE);
        biomass = ((Outcome.Done<BiomassTank>) biomass.drain(recipe.biomass())).value();
        batch = Optional.of(new OrganBatch(
                Batch.start(found.get().id().toString(), recipe.ticks()),
                recipe.result().copy(),
                Optional.of(player.getUUID())));
        setChanged();
        return Optional.empty();
    }

    /** A running batch cannot be cancelled: its inputs are gone. */
    Optional<Refusal> cancel(Player player) {
        return batch.isPresent() ? Optional.of(COMMITTED) : Optional.empty();
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
        if (batch.isPresent()) {
            String state = Organs.hasFoundation(level, worldPosition) ? "working" : "paused";
            player.displayClientMessage(
                    Component.translatable("infestusfrontier.status.culture_bowl." + state, percent()), true);
            return;
        }
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

    private CultureBowlContents contents() {
        List<ItemStack> held = new ArrayList<>();
        for (int slot = 0; slot < INGREDIENT_SLOTS; slot++)
            held.add(ingredients.getStackInSlot(slot).copy());
        return new CultureBowlContents(
                held, output.getStackInSlot(0).copy(), water.getFluidAmount(), biomass, history, batch);
    }

    private void restore(CultureBowlContents saved, boolean takeTank) {
        for (int slot = 0; slot < INGREDIENT_SLOTS; slot++)
            ingredients.setStackInSlot(
                    slot,
                    slot < saved.ingredients().size()
                            ? saved.ingredients().get(slot).copy()
                            : ItemStack.EMPTY);
        output.setStackInSlot(0, saved.output().copy());
        water.setFluid(saved.waterMb() > 0 ? new FluidStack(Fluids.WATER, saved.waterMb()) : FluidStack.EMPTY);
        if (takeTank) biomass = saved.biomass();
        history = saved.history();
        batch = saved.batch();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        Optional<CultureBowlContents> read =
                Persistence.read(SAVED, tag, registries, ProcessingModule.LOGGER, "Culture Bowl");
        unreadable = read.isPresent() ? Optional.empty() : Optional.of(tag.copy());
        read.ifPresent(saved -> restore(saved, true));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (unreadable.isPresent()) tag.merge(unreadable.get());
        else Persistence.write(SAVED, contents(), registries, tag);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(ProcessingModule.BOWL_CONTENTS.get(), contents());
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        CultureBowlContents carried = input.get(ProcessingModule.BOWL_CONTENTS.get());
        if (carried != null) restore(carried, carried.biomass().capacity().equals(CAPACITY));
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        tag.remove("contents");
    }
}
