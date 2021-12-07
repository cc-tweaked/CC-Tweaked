; -*- mode: Lisp;-*-

(sources
  /doc/events/
  /doc/guides/
  /doc/stub/
  /build/docs/luaJavadoc/
  /src/main/resources/*/computercraft/lua/bios.lua
  /src/main/resources/*/computercraft/lua/rom/
  /src/test/resources/test-rom
  /src/web/mount)


(doc
  (destination build/docs/lua)
  (index doc/index.md)

  (site
    (title "CC: Tweaked")
    (logo src/main/resources/pack.png)
    (url https://tweaked.cc/)
    (source-link https://github.com/cc-tweaked/CC-Tweaked/blob/${commit}/${path}#L${line})

    (styles src/web/styles.css)
    (scripts build/rollup/index.js)
    (head doc/head.html))

  (module-kinds
    (peripheral Peripherals)
    (generic_peripheral "Generic Peripherals")
    (event Events)
    (guide Guides))

  (library-path
    /doc/stub/
    /build/docs/luaJavadoc/

    /src/main/resources/*/computercraft/lua/rom/apis/
    /src/main/resources/*/computercraft/lua/rom/apis/command/
    /src/main/resources/*/computercraft/lua/rom/apis/turtle/

    /src/main/resources/*/computercraft/lua/rom/modules/main/
    /src/main/resources/*/computercraft/lua/rom/modules/command/
    /src/main/resources/*/computercraft/lua/rom/modules/turtle/))

(at /
  (linters
    syntax:string-index

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
      _CC_DISABLE_LUA51_FEATURES
      _HOST
      ;; Ideally we'd pick these up from bios.lua, but illuaminate currently
      ;; isn't smart enough.
      sleep write printError read rs)))

;; We disable the unused global linter in bios.lua and the APIs. In the future
;; hopefully we'll get illuaminate to handle this.
(at
  (/src/main/resources/*/computercraft/lua/bios.lua
   /src/main/resources/*/computercraft/lua/rom/apis/)
  (linters -var:unused-global)
  (lint (allow-toplevel-global true)))

;; Silence some variable warnings in documentation stubs.
(at (/doc/stub/ /build/docs/luaJavadoc/)
  (linters -var:unused-global)
  (lint (allow-toplevel-global true)))

;; Suppress warnings for currently undocumented modules.
(at
  (; Lua APIs
   /src/main/resources/*/computercraft/lua/rom/apis/io.lua
   /src/main/resources/*/computercraft/lua/rom/apis/window.lua)

  (linters -doc:undocumented -doc:undocumented-arg -doc:undocumented-return))

;; Suppress warnings for various APIs using its own deprecated members.
(at
  (/src/main/resources/*/computercraft/lua/bios.lua
   /src/main/resources/*/computercraft/lua/rom/apis/turtle/turtle.lua)
  (linters -var:deprecated))

(at /src/test/resources/test-rom
  ; We should still be able to test deprecated members.
  (linters -var:deprecated)

  (lint
    (globals
      :max sleep write
      cct_test describe expect howlci fail it pending stub)))

(at /src/web/mount/expr_template.lua (lint (globals :max __expr__)))
