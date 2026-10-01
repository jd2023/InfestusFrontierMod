package org.jd.infestusfrontier.testmod.kit;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class SaveReload {
    private SaveReload() {}

    public static CompoundTag save(GameTestHelper helper, BlockPos relative) {
        return helper.getLevel().getBlockEntity(helper.absolutePos(relative))
                .saveWithFullMetadata(helper.getLevel().registryAccess());
    }

    public static BlockEntity reload(GameTestHelper helper, BlockPos relative) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(relative);
        var saved = save(helper, relative);
        level.removeBlockEntity(pos);
        var reloaded = BlockEntity.loadStatic(pos, level.getBlockState(pos), saved, level.registryAccess());
        helper.assertTrue(reloaded != null, "Reload must produce a block entity");
        level.setBlockEntity(reloaded);
        return reloaded;
    }

    public static void assertRetainsRejectedData(GameTestHelper helper, BlockPos relative, CompoundTag hostile) {
        var entity = helper.getLevel().getBlockEntity(helper.absolutePos(relative));
        entity.loadWithComponents(hostile, helper.getLevel().registryAccess());
        var saved = save(helper, relative);
        for (var key : hostile.getAllKeys()) {
            if (key.equals("id") || key.equals("x") || key.equals("y") || key.equals("z")) continue;
            helper.assertTrue(saved.contains(key) && hostile.get(key).equals(saved.get(key)),
                    "Rejected data must retain key " + key);
        }
    }
}
