package org.jd.infestusfrontier.testmod.integration;

import com.mojang.logging.LogUtils;
import java.util.regex.Pattern;
import net.minecraft.gametest.framework.GameTestHelper;
import org.slf4j.Logger;

/** Emits bounded success markers only after a development assertion completes. */
public final class ContentAssertion {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Pattern NAME = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");

    private ContentAssertion() {}

    public static void passGameTest(GameTestHelper helper, String name) {
        requireName(name);
        LOGGER.info("INFESTUS_CONTENT_ASSERTION name={}", name);
        helper.succeed();
    }

    public static void passGuide(String name) {
        requireName(name);
        LOGGER.info("INFESTUS_GUIDE_ASSERTION name={}", name);
    }

    private static void requireName(String name) {
        if (name == null || name.length() > 256 || !NAME.matcher(name).matches()) {
            throw new IllegalArgumentException("Invalid content assertion name");
        }
    }
}
