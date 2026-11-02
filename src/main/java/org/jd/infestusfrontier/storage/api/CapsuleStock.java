package org.jd.infestusfrontier.storage.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import org.jd.infestusfrontier.core.storage.PodKind;

/**
 * What one Item Capsule carries, as saved and as carried on its item: its compartments, each empty or holding one kind
 * with its count, which may pass a stack, its grade, the volume it has been grown to, and the kind of pod that carries
 * them, and for a Larder the label and the reserve of each compartment. A value: stacks are copied in and out and
 * compared with {@link ItemStack#matches}. Saved as one entry per held compartment and one per compartment with a label
 * or a reserve; a slot out of range, a slot named twice, an unknown kind or a grade past the last does not decode,
 * though any value encodes, so bad state can be kept as it came. Stock saved before pods had kinds is a bulk pod's.
 */
public record CapsuleStock(List<ItemStack> held, int grade, PodKind kind, List<Mark> marks) {
    /** What a Larder compartment carries beside its items: its label, one of the kind or empty, and its reserve. */
    public record Mark(ItemStack label, int reserve) {
        public static final Mark NONE = new Mark(ItemStack.EMPTY, 0);
        public static final StreamCodec<RegistryFriendlyByteBuf, Mark> STREAM_CODEC = StreamCodec.composite(
                ItemStack.OPTIONAL_STREAM_CODEC, Mark::label, ByteBufCodecs.VAR_INT, Mark::reserve, Mark::new);

        public Mark {
            label = label.isEmpty() ? ItemStack.EMPTY : label.copyWithCount(1);
            if (reserve < 0) throw new IllegalArgumentException("reserve " + reserve);
        }

        @Override
        public ItemStack label() {
            return label.copy();
        }

        public boolean isNone() {
            return label.isEmpty() && reserve == 0;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Mark that && that.reserve == reserve && ItemStack.matches(that.label, label);
        }

        @Override
        public int hashCode() {
            return ItemStack.hashItemAndComponents(label) * 31 + reserve;
        }
    }

    /** The most compartments any capsule carries: a Mixed Craw's. */
    public static final int COMPARTMENTS = PodKind.CRAW_COMPARTMENTS;
    /** Grades 0 to this: a young capsule holds {@link PodKind#stacks} stacks of its kind, each grade doubles it. */
    public static final int LAST_GRADE = 2;

    public static final CapsuleStock EMPTY = new CapsuleStock(List.of(), 0, PodKind.BULK, List.of());

    private static final Codec<PodKind> KIND = Codec.STRING.comapFlatMap(
            id -> PodKind.byId(id)
                    .map(DataResult::success)
                    .orElseGet(() -> DataResult.error(() -> "unknown pod kind " + id)),
            PodKind::id);

    private record Entry(int slot, ItemStack kind, int count) {}

    private record MarkEntry(int slot, ItemStack label, int reserve) {}

    private static final Codec<Entry> ENTRY = RecordCodecBuilder.<Entry>create(instance -> instance.group(
                    Codec.intRange(0, COMPARTMENTS - 1).fieldOf("slot").forGetter(Entry::slot),
                    ItemStack.SINGLE_ITEM_CODEC.fieldOf("item").forGetter(Entry::kind),
                    Codec.intRange(1, Integer.MAX_VALUE).fieldOf("count").forGetter(Entry::count))
            .apply(instance, Entry::new));

    private static final Codec<List<ItemStack>> HELD =
            ENTRY.listOf(0, COMPARTMENTS).comapFlatMap(CapsuleStock::decode, CapsuleStock::entries);

    private static final Codec<MarkEntry> MARK_ENTRY = RecordCodecBuilder.<MarkEntry>create(instance -> instance.group(
                    Codec.intRange(0, COMPARTMENTS - 1).fieldOf("slot").forGetter(MarkEntry::slot),
                    ItemStack.SINGLE_ITEM_CODEC
                            .optionalFieldOf("label", ItemStack.EMPTY)
                            .forGetter(MarkEntry::label),
                    Codec.intRange(0, Integer.MAX_VALUE)
                            .optionalFieldOf("reserve", 0)
                            .forGetter(MarkEntry::reserve))
            .apply(instance, MarkEntry::new));

    private static final Codec<List<Mark>> MARKS =
            MARK_ENTRY.listOf(0, COMPARTMENTS).comapFlatMap(CapsuleStock::decodeMarks, CapsuleStock::markEntries);

    public static final Codec<CapsuleStock> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    HELD.optionalFieldOf("held", List.of()).forGetter(CapsuleStock::held),
                    Codec.intRange(0, LAST_GRADE).optionalFieldOf("grade", 0).forGetter(CapsuleStock::grade),
                    KIND.optionalFieldOf("kind", PodKind.BULK).forGetter(CapsuleStock::kind),
                    MARKS.optionalFieldOf("marks", List.of()).forGetter(CapsuleStock::marks))
            .apply(instance, CapsuleStock::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CapsuleStock> STREAM_CODEC = StreamCodec.composite(
            ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list(COMPARTMENTS)),
            CapsuleStock::held,
            ByteBufCodecs.VAR_INT,
            CapsuleStock::grade,
            ByteBufCodecs.STRING_UTF8.map(id -> PodKind.byId(id).orElse(PodKind.BULK), PodKind::id),
            CapsuleStock::kind,
            Mark.STREAM_CODEC.apply(ByteBufCodecs.list(COMPARTMENTS)),
            CapsuleStock::marks,
            CapsuleStock::new);

    /** Stock of a bulk pod. */
    public CapsuleStock(List<ItemStack> held, int grade) {
        this(held, grade, PodKind.BULK, List.of());
    }

    /** Stock of a pod with no labels or reserves. */
    public CapsuleStock(List<ItemStack> held, int grade, PodKind kind) {
        this(held, grade, kind, List.of());
    }

    /** Keeps copies, so the value never changes. */
    public CapsuleStock {
        if (held.size() > COMPARTMENTS) throw new IllegalArgumentException(held.size() + " compartments");
        if (grade < 0 || grade > LAST_GRADE) throw new IllegalArgumentException("grade " + grade);
        List<ItemStack> copies = new ArrayList<>(held.size());
        for (ItemStack stack : held) copies.add(stack.copy());
        held = List.copyOf(copies);
        if (marks.size() > COMPARTMENTS) throw new IllegalArgumentException(marks.size() + " marks");
        marks = List.copyOf(marks);
    }

    /** Copies of the compartments, each with its whole count. */
    @Override
    public List<ItemStack> held() {
        List<ItemStack> copies = new ArrayList<>(held.size());
        for (ItemStack stack : held) copies.add(stack.copy());
        return copies;
    }

    /** True while no compartment holds anything; the grade may still be worth carrying. */
    public boolean isEmpty() {
        for (ItemStack stack : held) if (!stack.isEmpty()) return false;
        return true;
    }

    /** True when there is nothing to carry: empty, ungrown, no label and no reserve. */
    public boolean isBare() {
        return isEmpty() && grade == 0 && marks.stream().allMatch(Mark::isNone);
    }

    /** The label and reserve of compartment {@code slot}; none for a pod without them. */
    public Mark mark(int slot) {
        return slot >= 0 && slot < marks.size() ? marks.get(slot) : Mark.NONE;
    }

    /** The same stock at {@code grade}. */
    public CapsuleStock graded(int grade) {
        return new CapsuleStock(held, grade, kind, marks);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof CapsuleStock that)
                || that.held.size() != held.size()
                || that.grade != grade
                || that.kind != kind) return false;
        for (int i = 0; i < held.size(); i++) if (!ItemStack.matches(held.get(i), that.held.get(i))) return false;
        for (int i = 0; i < COMPARTMENTS; i++) if (!mark(i).equals(that.mark(i))) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int hash = grade * 7 + kind.ordinal();
        for (ItemStack stack : held) hash = 31 * hash + ItemStack.hashItemAndComponents(stack) * 31 + stack.getCount();
        for (int i = 0; i < COMPARTMENTS; i++) hash = 31 * hash + mark(i).hashCode();
        return hash;
    }

    private static DataResult<List<ItemStack>> decode(List<Entry> entries) {
        List<ItemStack> held = new ArrayList<>();
        for (int i = 0; i < COMPARTMENTS; i++) held.add(ItemStack.EMPTY);
        int last = -1;
        for (Entry entry : entries) {
            if (!held.get(entry.slot()).isEmpty()) return DataResult.error(() -> "slot " + entry.slot() + " twice");
            held.set(entry.slot(), entry.kind().copyWithCount(entry.count()));
            last = Math.max(last, entry.slot());
        }
        return DataResult.success(List.copyOf(held.subList(0, last + 1)));
    }

    private static List<Entry> entries(List<ItemStack> held) {
        List<Entry> entries = new ArrayList<>();
        for (int slot = 0; slot < held.size(); slot++) {
            ItemStack stack = held.get(slot);
            if (!stack.isEmpty()) entries.add(new Entry(slot, stack.copyWithCount(1), stack.getCount()));
        }
        return entries;
    }

    private static DataResult<List<Mark>> decodeMarks(List<MarkEntry> entries) {
        List<Mark> marks = new ArrayList<>();
        for (int i = 0; i < COMPARTMENTS; i++) marks.add(Mark.NONE);
        boolean[] seen = new boolean[COMPARTMENTS];
        int last = -1;
        for (MarkEntry entry : entries) {
            if (seen[entry.slot()]) return DataResult.error(() -> "mark " + entry.slot() + " twice");
            seen[entry.slot()] = true;
            marks.set(entry.slot(), new Mark(entry.label(), entry.reserve()));
            last = Math.max(last, entry.slot());
        }
        return DataResult.success(List.copyOf(marks.subList(0, last + 1)));
    }

    private static List<MarkEntry> markEntries(List<Mark> marks) {
        List<MarkEntry> entries = new ArrayList<>();
        for (int slot = 0; slot < marks.size(); slot++)
            if (!marks.get(slot).isNone())
                entries.add(new MarkEntry(
                        slot, marks.get(slot).label(), marks.get(slot).reserve()));
        return entries;
    }
}
