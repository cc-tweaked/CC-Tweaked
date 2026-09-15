// SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.gradle

import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.attributes.*
import org.gradle.api.attributes.java.TargetJvmEnvironment
import org.gradle.api.file.FileCollection
import org.gradle.kotlin.dsl.named

/**
 * Support for extracting sources and build outputs from dependencies on external projects.
 *
 * See [org.gradle.testing.jacoco.plugins.JacocoReportAggregationPlugin] for the variant resolution logic this is based
 * on.
 */
object ExternalProjectArtifacts {
    /** Configure the attributes on a [Configuration] to use the standard dependency resolution rules. */
    fun configure(configuration: Configuration) {
        configuration.attributes {
            attribute(Category.CATEGORY_ATTRIBUTE, named(Category.LIBRARY))
            attribute(Usage.USAGE_ATTRIBUTE, named(Usage.JAVA_RUNTIME))
            attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, named(LibraryElements.JAR))
            attribute(Bundling.BUNDLING_ATTRIBUTE, named(Bundling.EXTERNAL))
            attribute(TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE, named(TargetJvmEnvironment.STANDARD_JVM))
        }
    }

    /** Get the classes directories from projects within a [Configuration]. */
    fun classes(configuration: Configuration): FileCollection = configuration.incoming.artifactView {
        componentFilter { c -> c is ProjectComponentIdentifier }
        attributes { attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, named(LibraryElements.CLASSES)) }
    }.files

    /** Get the resources directories from projects within a [Configuration]. */
    fun resources(configuration: Configuration): FileCollection = configuration.incoming.artifactView {
        componentFilter { c -> c is ProjectComponentIdentifier }
        attributes { attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, named(LibraryElements.RESOURCES)) }
    }.files

    /** Get the classes and resources directories from projects within a [Configuration]. */
    fun classesAndResources(configuration: Configuration): FileCollection =
        classes(configuration) + resources(configuration)

    /** Get the sources directory from projects within a [Configuration]. */
    fun sources(configuration: Configuration): FileCollection = configuration.incoming.artifactView {
        withVariantReselection()
        componentFilter { c -> c is ProjectComponentIdentifier }
        attributes {
            attribute(Category.CATEGORY_ATTRIBUTE, named(Category.VERIFICATION))
            attribute(VerificationType.VERIFICATION_TYPE_ATTRIBUTE, named(VerificationType.MAIN_SOURCES))
        }
    }.files
}
