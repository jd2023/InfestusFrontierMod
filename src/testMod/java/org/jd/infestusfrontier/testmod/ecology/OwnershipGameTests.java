package org.jd.infestusfrontier.testmod.ecology;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class OwnershipGameTests {
    private static final String NAME = "infestusfrontier_ecology_cells";
    private static final BlockPos CELL = new BlockPos(12, -60, 34);
    private static final UUID OWNER = UUID.fromString("34e0f374-dd87-482c-9d8e-57f71b3649c1");
    private static final UUID OTHER = UUID.fromString("bccdabed-899b-4d01-8ac5-c6a92e1f7b97");

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void ownershipSurvivesSaveAndReload(GameTestHelper helper) throws Exception {
        Path directory = Files.createTempDirectory("infestus-ownership-");
        try {
            SavedData original = load(storage(helper, directory), directory);
            helper.assertTrue(invoke(original, "claim", OWNER), "Fresh ownership must allow a claim");
            CompoundTag serialized = original.save(new CompoundTag(), helper.getLevel().registryAccess());
            write(directory, serialized);
            SavedData reloaded = load(storage(helper, directory), directory);
            helper.assertTrue(invoke(reloaded, "permits", OWNER) && !invoke(reloaded, "permits", OTHER),
                    "Reload must preserve the owner and refuse another player");
            helper.assertTrue(serialized.equals(reloaded.save(new CompoundTag(), helper.getLevel().registryAccess())),
                    "Round trip must retain the full ownership data");
            helper.succeed();
        } finally {
            Files.deleteIfExists(directory.resolve(NAME + ".dat"));
            Files.deleteIfExists(directory);
        }
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void unsupportedOwnershipCannotBecomeAnUnprotectedEmptyMap(GameTestHelper helper) throws Exception {
        CompoundTag unsupported = validData();
        unsupported.putInt("schema", 99);
        unsupported.putString("future-field", "preserve verbatim");
        assertRejected(helper, unsupported);
        helper.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void malformedOwnershipCannotDiscardProtectedCells(GameTestHelper helper) throws Exception {
        CompoundTag missingOwner = validData();
        missingOwner.getList("cells", 10).getCompound(0).remove("owner");
        assertRejected(helper, missingOwner);
        CompoundTag missingPosition = validData();
        missingPosition.getList("cells", 10).getCompound(0).remove("position");
        assertRejected(helper, missingPosition);
        CompoundTag duplicates = validData();
        var duplicateCells = duplicates.getList("cells", 10);
        duplicateCells.add(duplicateCells.getCompound(0).copy());
        assertRejected(helper, duplicates);
        CompoundTag wrongListType = validData();
        wrongListType.putString("cells", "not a cell list");
        assertRejected(helper, wrongListType);
        CompoundTag missingSchema = validData();
        missingSchema.remove("schema");
        assertRejected(helper, missingSchema);
        CompoundTag oversized = validData();
        var cells = oversized.getList("cells", 10);
        while (cells.size() <= 65_536) cells.add(cells.getCompound(0));
        assertRejected(helper, oversized);
        helper.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void truncatedOwnershipRefusesEditsAndPreservesDiskBytes(GameTestHelper helper) throws Exception {
        Path directory = Files.createTempDirectory("infestus-truncated-ownership-");
        try {
            write(directory, validData());
            Path file = directory.resolve(NAME + ".dat");
            byte[] bytes = Files.readAllBytes(file);
            Files.write(file, Arrays.copyOf(bytes, bytes.length / 2));
            assertRejectedFile(helper, directory);
            helper.succeed();
        } finally {
            Files.deleteIfExists(directory.resolve(NAME + ".dat"));
            Files.deleteIfExists(directory);
        }
    }

    private static void assertRejected(GameTestHelper helper, CompoundTag data) throws Exception {
        Path directory = Files.createTempDirectory("infestus-invalid-ownership-");
        try {
            write(directory, data);
            assertRejectedFile(helper, directory);
        } finally {
            Files.deleteIfExists(directory.resolve(NAME + ".dat"));
            Files.deleteIfExists(directory);
        }
    }

    private static void assertRejectedFile(GameTestHelper helper, Path directory) throws Exception {
        Path file = directory.resolve(NAME + ".dat");
        byte[] before = Files.readAllBytes(file);
        var storage = storage(helper, directory);
        SavedData rejected = load(storage, directory);
        helper.assertTrue(!invoke(rejected, "permits", OTHER) && !invoke(rejected, "claim", OTHER),
                "Rejected ownership data must block edits instead of creating unprotected cells");
        var remove = rejected.getClass().getDeclaredMethod("remove", BlockPos.class);
        remove.setAccessible(true);
        remove.invoke(rejected, CELL);
        helper.assertTrue(!rejected.isDirty(), "Rejected ownership data must never schedule replacement of the save");
        storage.save();
        helper.assertTrue(Arrays.equals(before, Files.readAllBytes(file)),
                "Rejected ownership data must remain byte-for-byte intact on disk");
        helper.assertTrue(load(storage, directory) == rejected, "Rejected load must remain cached");
    }

    private static CompoundTag validData() {
        CompoundTag cell = new CompoundTag();
        cell.putLong("position", CELL.asLong());
        cell.putUUID("owner", OWNER);
        ListTag cells = new ListTag();
        cells.add(cell);
        CompoundTag data = new CompoundTag();
        data.putInt("schema", 1);
        data.put("cells", cells);
        return data;
    }

    private static void write(Path directory, CompoundTag data) throws Exception {
        CompoundTag root = new CompoundTag();
        root.put("data", data);
        NbtUtils.addCurrentDataVersion(root);
        NbtIo.writeCompressed(root, directory.resolve(NAME + ".dat"));
    }

    private static DimensionDataStorage storage(GameTestHelper helper, Path directory) {
        return new DimensionDataStorage(directory.toFile(), DataFixers.getDataFixer(), helper.getLevel().registryAccess());
    }

    // Test modules cannot share production packages; reflection keeps this save adapter internal.
    private static SavedData load(DimensionDataStorage storage, Path directory) throws Exception {
        var method = Class.forName("org.jd.infestusfrontier.ecology.SubstrateOwnership")
                .getDeclaredMethod("get", DimensionDataStorage.class, Path.class);
        method.setAccessible(true);
        return (SavedData) method.invoke(null, storage, directory.resolve(NAME + ".dat"));
    }

    private static boolean invoke(SavedData data, String name, UUID player) throws Exception {
        var method = data.getClass().getDeclaredMethod(name, BlockPos.class, UUID.class);
        method.setAccessible(true);
        return (boolean) method.invoke(data, CELL, player);
    }
}
