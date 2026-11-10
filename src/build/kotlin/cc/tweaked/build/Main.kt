// SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.build

import org.objectweb.asm.ClassReader
import java.io.FileOutputStream
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if (args.size != 2) {
        System.err.println("Expected: INPUT OUTPUT")
        exitProcess(1)
    }

    JarFile(args[0]).use { input ->
        val data = DowngradeData()
        for (file in input.entries()) {
            if (!file.name.endsWith(".class")) continue

            val klass = ClassReader(input.getInputStream(file).readBytes())
            data.addClass(klass)
        }

        JarOutputStream(FileOutputStream(args[1])).use { output ->
            val emitter = ZipClassEmitter(output)
            for (file in input.entries()) {
                if (file.name == "module-info.class") continue

                if (!file.name.endsWith(".class")) {
                    val newEntry = ZipEntry(file)
                    newEntry.time = file.time
                    output.putNextEntry(newEntry)
                    input.getInputStream(file).copyTo(output)
                } else {
                    val klass = ClassReader(input.getInputStream(file).readBytes())
                    emitter.generate(klass.className, 0) { cw ->
                        klass.accept(Downgrade(emitter, data, cw), ClassReader.EXPAND_FRAMES)
                    }
                }
            }
        }
    }
}
