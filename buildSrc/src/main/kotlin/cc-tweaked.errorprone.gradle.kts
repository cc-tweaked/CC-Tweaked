import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone

plugins {
    java
    id("net.ltgt.errorprone")
}


dependencies {
    val libs = project.extensions.getByType<VersionCatalogsExtension>().named("libs")
    errorprone(libs.findLibrary("errorProne-core").get())
    errorprone(libs.findLibrary("nullAway").get())
}

// Configure default JavaCompile tasks with our arguments.
sourceSets.all {
    tasks.named(compileJavaTaskName, JavaCompile::class.java) {
        options.errorprone {
            check("InvalidBlockTag", CheckSeverity.OFF) // Broken by @cc.xyz
            check("InvalidParam", CheckSeverity.OFF) // Broken by records.
            check("InlineMeSuggester", CheckSeverity.OFF) // Minecraft uses @Deprecated liberally
            // Too many false positives right now. Maybe we need an indirection for it later on.
            check("ReferenceEquality",CheckSeverity.OFF)
            check("UnusedVariable", CheckSeverity.OFF) // Too many false positives with records.
            check("OperatorPrecedence", CheckSeverity.OFF) // For now.
            check("AlreadyChecked", CheckSeverity.OFF) // Seems to be broken?
            check("NonOverridingEquals", CheckSeverity.OFF) // Peripheral.equals makes this hard to avoid
            check("FutureReturnValueIgnored", CheckSeverity.OFF) // Too many false positives with Netty

            check("NullAway", CheckSeverity.ERROR)
            option("NullAway:AnnotatedPackages", listOf("dan200.computercraft").joinToString(","))
            option("NullAway:ExcludedFieldAnnotations", listOf("org.spongepowered.asm.mixin.Shadow").joinToString(","))
            option("NullAway:CastToNonNullMethod", "dan200.computercraft.core.util.Nullability.assertNonNull")
            option("NullAway:CheckOptionalEmptiness")
            option("NullAway:AcknowledgeRestrictiveAnnotations")
        }
    }
}

tasks.compileTestJava {
    options.errorprone {
        check("NullAway", CheckSeverity.OFF)
    }
}
