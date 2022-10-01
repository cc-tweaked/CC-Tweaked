package cc.tweaked.gradle

import org.gradle.api.provider.Property
import org.gradle.api.tasks.AbstractExecTask
import org.gradle.api.tasks.OutputDirectory
import java.io.File

abstract class ExecToDir : AbstractExecTask<ExecToDir>(ExecToDir::class.java) {
    @get:OutputDirectory
    abstract val output: Property<File>
}
