// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.build

import org.objectweb.asm.ClassReader
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.extension
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if (args.size != 2) {
        System.err.println("Expected: INPUT OUTPUT")
        exitProcess(1)
    }

    val inputDir = Paths.get(args[0])
    val outputDir = Paths.get(args[1])

    val emitter = FileClassEmitter(outputDir)
    Files.find(inputDir, Int.MAX_VALUE, { path, _ -> path.extension == "class" }).use { files ->
        files.forEach { inputFile ->
            val reader = Files.newInputStream(inputFile).use { ClassReader(it) }
            emitter.generate(reader.className, flags = 0) { cw -> reader.accept(Unlambda(emitter, cw), 0) }
        }
    }
}
