import { readFileSync } from "fs";
import path from "path";

import typescript from "@rollup/plugin-typescript";
import url from '@rollup/plugin-url';
import { terser } from "rollup-plugin-terser";

const input = "src/web";
const requirejs = readFileSync("node_modules/requirejs/require.js");

export default {
    input: [`${input}/index.tsx`],
    output: {
        dir: "build/rollup/",
        // We bundle requirejs (and config) into the header. It's rather gross
        // but also works reasonably well.
        // Also suffix a ?v=${date} onto the end in the event we need to require a specific copy-cat version.
        banner: `
            ${requirejs}
            require.config({
                paths: { copycat: "https://copy-cat.squiddev.cc" },
                urlArgs: function(id) { return id == "copycat/embed" ? "?v=20211221" : ""; }
            });
        `,
        format: "amd",
        preferConst: true,
        amd: {
            define: "require",
        }
    },
    context: "window",
    external: ["copycat/embed"],

    plugins: [
        typescript(),

        url({
            include: "**/*.dfpwm",
            fileName: "[name]-[hash][extname]",
            publicPath: "/",
        }),

        {
            name: "cc-tweaked",
            async transform(code, file) {
                // Allow loading files in /mount.
                const ext = path.extname(file);
                return ext != '.dfpwm' && path.dirname(file) === path.resolve(`${input}/mount`)
                    ? `export default ${JSON.stringify(code)};\n`
                    : null;
            },
        },

        terser(),
    ],
};
