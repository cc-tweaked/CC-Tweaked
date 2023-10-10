// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

import path from "path";

import resolve from "@rollup/plugin-node-resolve";
import typescript from "@rollup/plugin-typescript";
import url from "@rollup/plugin-url";
import { minify as minifyJavascript } from "@swc/core";
import { transform as transformCss } from "lightningcss";

const inputDir = "src/frontend";

/**
 * Generate an ESM module from a list of {@code ligntingcss} exports.
 *
 * @param {string} filename The input file name.
 * @param {string} input The input code.
 * @param {boolean} minify Whether to minify the css.
 * @returns {string} The ESM module, containing the list of exports.
 */
const compileCss = (filename, input, minify) => {
    const { code, exports } = transformCss({
        filename,
        cssModules: filename.endsWith(".module.css"),
        pattern: "[local]-[hash]",
        code: Buffer.from(input),
        minify,
    });

    let output = "";
    let importId = 0;
    for (const [name, symbol] of Object.entries(exports ?? {})) {
        let className = JSON.stringify(symbol.name);
        for (const dep of symbol.composes) {
            if (dep.type == "dependency") {
                output += `import { ${dep.name} as import_${++importId}$ } from ${JSON.stringify(dep.specifier)};\n`;
                className += `+ " " + import_${importId}$`;
            } else {
                className += `+ " " + ${JSON.stringify(dep.name)}`;
            }
        }
        output += `export const ${name} = ${className};\n`;
    }

    return { js: output, css: new TextDecoder().decode(code) };
}

/**
 * Custom plugin for CC: Tweaked specific build logic.
 *
 * This handles:
 *  - Minifying JS using swc, which is faster than using terser.
 *  - Importing CSS files.
 *  - Importing plain text files from our "mount/" directory.
 *  - Resoving our TeaVM compiled classes and resources files.
 *
 * @param {boolean} minify Whether to minify our sources.
 * @returns {@type import("rollup").Plugin} Our plugin.
 */
const ccTweaked = minify => {
    let cssChunks = [];
    return {
        name: "cc-tweaked",

        buildStart() {
            cssChunks = [];
        },

        async renderChunk(code) {
            // Use swc to minify our Javascript.
            return minify ? (await minifyJavascript(code, { module: true })).code : code;
        },

        async transform(code, file) {
            const ext = path.extname(file);
            if (ext === ".css") {
                // Compile our CSS file, emitting a JS module, and saving the CSS for later.
                const { js, css } = compileCss(file, code, minify);
                cssChunks.push(css);
                return js;
            } else if (ext != ".dfpwm" && path.dirname(file) === path.resolve(`${inputDir}/mount`)) {
                // Bundle all mount files aside from our dfpwm.
                return `export default ${JSON.stringify(code)};\n`
            } else {
                return null;
            }
        },

        async resolveId(source) {
            if (source === "cct/classes") return path.resolve("build/teaVM/classes.js");
            if (source === "cct/resources") return path.resolve("build/teaVM/resources.js");
            return null;
        },

        generateBundle() {
            this.emitFile({
                type: 'asset',
                fileName: "index.css",
                source: cssChunks.join(),
            });
        }
    }
};

/** @type import("rollup").RollupOptionsFunction */
export default args => ({
    input: [`${inputDir}/index.tsx`],
    output: {
        // Also defined in build.gradle.kts
        dir: "build/rollup/",

        format: "esm",
        generatedCode: {
            preset: "es2015",
            constBindings: true,
        },
    },
    context: "window",

    plugins: [
        typescript(),
        resolve({ browser: true }),

        url({
            include: ["**/*.dfpwm", "**/*.worker.js", "**/*.png"],
            fileName: "[name]-[hash][extname]",
            publicPath: "/",
            limit: 0,
        }),

        ccTweaked(!args.configDebug),
    ],
});
