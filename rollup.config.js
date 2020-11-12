import { readFileSync, promises as fs } from "fs";
import path from "path";

import typescript from "@rollup/plugin-typescript";

const input = "src/web";
const requirejs = readFileSync("node_modules/requirejs/require.js");

export default {
    input: [`${input}/index.tsx`],
    output: {
        file: "build/rollup/index.js",
        // We bundle requirejs (and config) into the header. It's rather gross
        // but also works reasonably well.
        banner: `${requirejs}\nrequire.config({ paths: { copycat: "https://copy-cat.squiddev.cc" } });`,
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

        {
            name: "cc-tweaked",
            async options(options) {
                // Generate .d.ts files for all /mount files. This is the worst way to do it,
                // but we need to run before the TS pass.
                const template = "declare const contents : string;\nexport default contents;\n";
                const files = await fs.readdir(`${input}/mount`);

                await Promise.all(files
                    .filter(x => path.extname(x) !== ".ts")
                    .map(file => fs.writeFile(`${input}/mount/${file}.d.ts`, template))
                );
                return options;
            },
            async transform(code, file) {
                // Allow loading files in /mount.
                if (path.extname(file) != ".lua" && path.basename(file) != ".settings") return null;
                return `export default ${JSON.stringify(code)};\n`;
            },
        }
    ],
};
