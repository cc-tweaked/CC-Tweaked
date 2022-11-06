package cc.tweaked.gradle

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.attributes.Bundling
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.Usage
import org.gradle.api.attributes.java.TargetJvmVersion
import org.gradle.api.capabilities.Capability
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.named

/**
 * This sets up a separate client-only source set, and extends that and the main/common source set with additional
 * metadata, to make it easier to consume jars downstream.
 */
class MinecraftConfigurations private constructor(private val project: Project) {
    private val java = project.extensions.getByType(JavaPluginExtension::class.java)
    private val sourceSets = java.sourceSets
    private val configurations = project.configurations
    private val objects = project.objects

    private val main = sourceSets[SourceSet.MAIN_SOURCE_SET_NAME]
    private val test = sourceSets[SourceSet.TEST_SOURCE_SET_NAME]

    /**
     * Performs the initial setup of our configurations.
     */
    private fun setup() {
        // Define a client source set.
        val client = sourceSets.maybeCreate("client")

        // Ensure the client classpaths behave the same as the main ones.
        configurations.named(client.compileClasspathConfigurationName) {
            shouldResolveConsistentlyWith(configurations[main.compileClasspathConfigurationName])
        }

        configurations.named(client.runtimeClasspathConfigurationName) {
            shouldResolveConsistentlyWith(configurations[main.runtimeClasspathConfigurationName])
        }

        // Set up an API configuration for clients (to ensure it's consistent with the main source set).
        val clientApi = configurations.maybeCreate(client.apiConfigurationName).apply {
            isVisible = false
            isCanBeConsumed = false
            isCanBeResolved = false
        }
        configurations.named(client.implementationConfigurationName) { extendsFrom(clientApi) }

        /*
          Now add outgoing variants for the main and common source sets that we can consume downstream. This is possibly
          the worst way to do things, but unfortunately the alternatives don't actually work very well:

           - Just using source set outputs: This means dependencies don't propagate, which means when :fabric depends
             on :fabric-api, we don't inherit the fake :common-api in IDEA.

           - Having separate common/main jars: Nice in principle, but unfortunately Forge needs a separate deobf jar
             task (as the original jar is obfuscated), and IDEA is not able to map its output back to a source set.

          This works for now, but is incredibly brittle. It's part of the reason we can't use testFixtures inside our
          MC projects, as that adds a project(self) -> test dependency, which would pull in the jar instead.

          Note we register a fake client jar here. It's not actually needed, but is there to make sure IDEA has
          a way to tell that client classes are needed at runtime.

          I'm so sorry, deeply aware how cursed this is.
        */
        setupOutgoing(main, "CommonOnly")
        project.tasks.register(client.jarTaskName, Jar::class.java) {
            description = "An empty jar standing in for the client classes."
            group = BasePlugin.BUILD_GROUP
            archiveClassifier.set("client")
        }
        setupOutgoing(client)

        // Reset the client classpath (Loom configures it slightly differently to this) and add a main -> client
        // dependency. Here we /can/ use source set outputs as we add transitive deps by patching the classpath. Nasty,
        // but avoids accidentally pulling in Forge's obfuscated jar.
        client.compileClasspath = client.compileClasspath + main.compileClasspath
        client.runtimeClasspath = client.runtimeClasspath + main.runtimeClasspath
        project.dependencies.add(client.apiConfigurationName, main.output)

        // Also add client classes to the test classpath. We do the same nasty tricks as needed for main -> client.
        test.compileClasspath += client.compileClasspath
        test.runtimeClasspath += client.runtimeClasspath
        project.dependencies.add(test.implementationConfigurationName, client.output)

        // Configure some tasks to include our additional files.
        project.tasks.named("javadoc", Javadoc::class.java) {
            source += client.allJava
            classpath = main.compileClasspath + main.output + client.compileClasspath + client.output
        }
        // This are already done by Fabric, but we need it for Forge and vanilla. It shouldn't conflict at all.
        project.tasks.named("jar", Jar::class.java) { from(client.output) }
        project.tasks.named("sourcesJar", Jar::class.java) { from(client.allSource) }

        project.extensions.configure(CCTweakedExtension::class.java) {
            sourceDirectories.add(SourceSetReference.internal(client))
        }
    }

    private fun setupOutgoing(sourceSet: SourceSet, suffix: String = "") {
        setupOutgoing("${sourceSet.apiElementsConfigurationName}$suffix", sourceSet, objects.named(Usage.JAVA_API)) {
            description = "API elements for ${sourceSet.name}"
            extendsFrom(configurations[sourceSet.apiConfigurationName])
        }

        setupOutgoing("${sourceSet.runtimeElementsConfigurationName}$suffix", sourceSet, objects.named(Usage.JAVA_RUNTIME)) {
            description = "Runtime elements for ${sourceSet.name}"
            extendsFrom(configurations[sourceSet.implementationConfigurationName], configurations[sourceSet.runtimeOnlyConfigurationName])
        }
    }

    /**
     * Set up an outgoing configuration for a specific source set. We set an additional "main" or "client" capability
     * (depending on the source set name) which allows downstream projects to consume them separately (see
     * [DependencyHandler.commonClasses] and [DependencyHandler.clientClasses]).
     */
    private fun setupOutgoing(name: String, sourceSet: SourceSet, usage: Usage, configure: Configuration.() -> Unit) {
        configurations.register(name) {
            isVisible = false
            isCanBeConsumed = true
            isCanBeResolved = false

            configure(this)

            attributes {
                attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
                attribute(Usage.USAGE_ATTRIBUTE, usage)
                attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
                attributeProvider(
                    TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE,
                    java.toolchain.languageVersion.map { it.asInt() },
                )
                attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
            }

            outgoing {
                capability(BasicOutgoingCapability(project, sourceSet.name))

                // We have two outgoing variants here: the original jar and the classes.
                artifact(project.tasks.named(sourceSet.jarTaskName))

                variants.create("classes") {
                    attributes.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.CLASSES))
                    sourceSet.output.classesDirs.forEach { artifact(it) { builtBy(sourceSet.output) } }
                }
            }
        }
    }

    companion object {
        fun setup(project: Project) {
            MinecraftConfigurations(project).setup()
        }
    }
}

private class BasicIncomingCapability(private val module: ModuleDependency, private val name: String) : Capability {
    override fun getGroup(): String = module.group!!
    override fun getName(): String = "${module.name}-$name"
    override fun getVersion(): String? = null
}

private class BasicOutgoingCapability(private val project: Project, private val name: String) : Capability {
    override fun getGroup(): String = project.group.toString()
    override fun getName(): String = "${project.name}-$name"
    override fun getVersion(): String = project.version.toString()
}

fun DependencyHandler.clientClasses(notation: Any): ModuleDependency {
    val dep = create(notation) as ModuleDependency
    dep.capabilities { requireCapability(BasicIncomingCapability(dep, "client")) }
    return dep
}

fun DependencyHandler.commonClasses(notation: Any): ModuleDependency {
    val dep = create(notation) as ModuleDependency
    dep.capabilities { requireCapability(BasicIncomingCapability(dep, "main")) }
    return dep
}
