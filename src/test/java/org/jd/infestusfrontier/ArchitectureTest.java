package org.jd.infestusfrontier;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameEndingWith;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.library.dependencies.SliceAssignment;
import com.tngtech.archunit.library.dependencies.SliceIdentifier;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** The module rules of ENGINEERING.md "Architecture", checked on the compiled mod. */
class ArchitectureTest {
    private static final String ROOT = "org.jd.infestusfrontier";
    private static JavaClasses mod;

    /** Feature packages are slices; each integration adapter is a slice of its own. */
    private static final SliceAssignment FEATURES = new SliceAssignment() {
        @Override
        public SliceIdentifier getIdentifierOf(JavaClass javaClass) {
            String pkg = javaClass.getPackageName();
            if (!pkg.startsWith(ROOT + ".")) return SliceIdentifier.ignore();
            String[] parts = pkg.substring(ROOT.length() + 1).split("\\.");
            if (parts[0].equals("integration") && parts.length > 1)
                return SliceIdentifier.of("integration." + parts[1]);
            return SliceIdentifier.of(parts[0]);
        }

        @Override
        public String getDescription() {
            return "feature packages";
        }
    };

    @BeforeAll
    static void importMod() {
        mod = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(ROOT);
    }

    /** A rule over nothing passes; this proves the mod was imported. */
    @Test
    void theModWasImported() {
        assertTrue(mod.contain(ROOT + ".processing.ProcessingModule"), "processing imported");
        assertTrue(mod.contain(ROOT + ".integration.modonomicon.GuidePages"), "integration imported");
        assertTrue(mod.size() > 50, "more than a handful of classes: " + mod.size());
    }

    @Test
    void featurePackagesHaveNoCycles() {
        slices().assignedFrom(FEATURES).should().beFreeOfCycles().check(mod);
    }

    @Test
    void apiPackagesReachTheirImplementationOnlyThroughTheModule() {
        noClasses()
                .that()
                .resideInAPackage("..api..")
                .should()
                .dependOnClassesThat(resideInAPackage(ROOT + "..")
                        .and(not(resideInAnyPackage("..api..", "..foundation..", ROOT + ".core..", ROOT + ".ui..")))
                        .and(not(simpleNameEndingWith("Module"))))
                .check(mod);
    }

    /** True for a class whose top-level class has a name ending with {@code suffix}; nested and anonymous ones count. */
    private static DescribedPredicate<JavaClass> declaredIn(String suffix) {
        return new DescribedPredicate<>("declared in a *" + suffix) {
            @Override
            public boolean test(JavaClass javaClass) {
                JavaClass top = javaClass;
                while (top.getEnclosingClass().isPresent())
                    top = top.getEnclosingClass().get();
                return top.getSimpleName().endsWith(suffix);
            }
        };
    }

    @Test
    void clientCodeStaysInItsPlaces() {
        noClasses()
                .that(not(resideInAnyPackage("..foundation.screen..", "..integration.."))
                        .and(not(declaredIn("Screen")))
                        .and(not(declaredIn("Module"))))
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("net.minecraft.client..", "net.neoforged.neoforge.client..", "com.mojang.blaze3d..")
                .check(mod);
    }

    @Test
    void blockEntitiesArePackagePrivate() {
        classes()
                .that()
                .haveSimpleNameEndingWith("BlockEntity")
                .should()
                .notBePublic()
                .check(mod);
    }
}
