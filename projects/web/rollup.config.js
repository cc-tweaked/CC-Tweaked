// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

import path from "path";

import terser from "@rollup/plugin-terser";
import resolve from "@rollup/plugin-node-resolve";
import typescript from "@rollup/plugin-typescript";
import url from "@rollup/plugin-url";
import postcss from "rollup-plugin-postcss";

const input = "src/frontend";

const minify = args => !args.configDebug;

/** @type import("rollup").RollupOptionsFunction */
export default args => ({
    input: [`${input}/index.tsx`],
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

        postcss({
            namedExports: true,
            minimize: minify(args),
            extract: true,
        }),

        {
            name: "cc-tweaked",
            async transform(code, file) {
                // Allow loading files in /mount.
                const ext = path.extname(file);
                return ext != ".dfpwm" && path.dirname(file) === path.resolve(`${input}/mount`)
                    ? `export default ${JSON.stringify(code)};\n`
                    : null;
            },

            async resolveId(source) {
                if (source === "cct/classes") return path.resolve("build/teaVM/classes.js");
                if (source === "cct/resources") return path.resolve("build/teaVM/resources.js");
                return null;
            },
        },

        minify(args) && terser(),
    ],
});
