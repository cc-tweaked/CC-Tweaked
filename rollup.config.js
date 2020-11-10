import { readFileSync } from "fs";

import typescript from "@rollup/plugin-typescript";

const requirejs = readFileSync("node_modules/requirejs/require.js");

export default {
  input: ["src/web/index.tsx"],
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
  ],
};
