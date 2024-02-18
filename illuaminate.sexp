; -*- mode: Lisp;-*-

; SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
;
; SPDX-License-Identifier: MPL-2.0

(sources
  /doc/
  /projects/common/build/docs/luaJavadoc/
  /projects/core/src/main/resources/data/computercraft/lua/bios.lua
  /projects/core/src/main/resources/data/computercraft/lua/rom/
  /projects/core/src/test/resources/test-rom
  /projects/web/src/frontend/mount)

(doc
  ; Also defined in projects/web/build.gradle.kts
  (destination /projects/web/build/illuaminate)
  (index doc/index.md)

  (site
    (title "CC: Tweaked")
    (logo projects/common/src/main/resources/pack.png)
    (url https://tweaked.cc/)
    (source-link https://github.com/cc-tweaked/CC-Tweaked/blob/${commit}/${path}#L${line})

    (styles  /projects/web/build/rollup/index.css)
    (scripts /projects/web/build/rollup/index.js)
    (head doc/head.html))

  (module-kinds
    (peripheral Peripherals)
    (generic_peripheral "Generic Peripherals")
    (event Events)
    (guide Guides)
    (reference Reference))

  (library-path
    /doc/stub/
    /projects/common/build/docs/luaJavadoc/

    /projects/core/src/main/resources/data/computercraft/lua/rom/apis/
    /projects/core/src/main/resources/data/computercraft/lua/rom/apis/command/
    /projects/core/src/main/resources/data/computercraft/lua/rom/apis/turtle/

    /projects/core/src/main/resources/data/computercraft/lua/rom/modules/main/
    /projects/core/src/main/resources/data/computercraft/lua/rom/modules/command/
    /projects/core/src/main/resources/data/computercraft/lua/rom/modules/turtle/))

(at /
  (linters
    syntax:string-index
    doc:docusaurus-admonition
    doc:ldoc-reference

    ;; It'd be nice to avoid this, but right now there's a lot of instances of
    ;; it.
    -var:set-loop

    ;; It's useful to name arguments for documentation, so we allow this. It'd
    ;; be good to find a compromise in the future, but this works for now.
    -var:unused-arg)

  (lint
    (bracket-spaces
      (call no-space)
      (function-args no-space)
      (parens no-space)
      (table space)
      (index no-space))

    (allow-clarifying-parens true)

    ;; colours imports from colors, and we don't handle that right now.
    ;; keys is entirely dynamic, so we skip it.
    (dynamic-modules colours keys _G)

    (globals
      :max
      _CC_DEFAULT_SETTINGS
      _HOST
      ;; Ideally we'd pick these up from bios.lua, but illuaminate currently
      ;; isn't smart enough.
      sleep write printError read rs)))

;; We disable the unused global linter in bios.lua, APIs and our documentation
;; stubs docs. In the future hopefully we'll get illuaminate to handle this.
(at
  (/doc/stub/
   /projects/core/src/main/resources/data/computercraft/lua/bios.lua
   /projects/core/src/main/resources/data/computercraft/lua/rom/apis/
   /projects/common/build/docs/luaJavadoc/)
  (linters -var:unused-global)
  (lint (allow-toplevel-global true)))

;; Suppress warnings for currently undocumented modules.
(at
  (; Lua APIs
   /projects/core/src/main/resources/data/computercraft/lua/rom/apis/window.lua)

  (linters -doc:undocumented -doc:undocumented-arg -doc:undocumented-return))

;; Suppress warnings for various APIs using its own deprecated members.
(at
  (/projects/core/src/main/resources/data/computercraft/lua/bios.lua
   /projects/core/src/main/resources/data/computercraft/lua/rom/apis/turtle/turtle.lua)
  (linters -var:deprecated))

;; Suppress unused variable warnings in the parser.
(at /projects/core/src/main/resources/data/computercraft/lua/rom/modules/main/cc/internal/syntax/parser.lua
  (linters -var:unused))

(at /projects/core/src/test/resources/test-rom
  ; We should still be able to test deprecated members.
  (linters -var:deprecated)

  (lint
    (globals
      :max sleep write
      cct_test describe expect howlci fail it pending stub before_each)))

(at /projects/web/src/frontend/mount/expr_template.lua (lint (globals :max __expr__)))
