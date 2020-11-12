; -*- mode: Lisp;-*-

(sources
  /doc/stub/
  /build/docs/luaJavadoc/
  /src/main/resources/*/computercraft/lua/bios.lua
  /src/main/resources/*/computercraft/lua/rom/
  /src/test/resources/test-rom
  /src/web/mount)


(doc
  (title "CC: Tweaked")
  (destination build/docs/lua)
  (logo src/main/resources/pack.png)
  (index doc/index.md)
  (styles src/web/styles.css)
  (scripts build/rollup/index.js)
  (source-link https://github.com/SquidDev-CC/CC-Tweaked/blob/${commit}/${path}#L${line})

  (module-kinds
    (peripheral Peripherals))

  (library-path
    /doc/stub/
    /build/docs/luaJavadoc/

    /src/main/resources/*/computercraft/lua/rom/apis
    /src/main/resources/*/computercraft/lua/rom/apis/command
    /src/main/resources/*/computercraft/lua/rom/apis/turtle

    /src/main/resources/*/computercraft/lua/rom/modules/main
    /src/main/resources/*/computercraft/lua/rom/modules/command
    /src/main/resources/*/computercraft/lua/rom/modules/turtle))

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

    ;; colours imports from colors, and we don't handle that right now.
    ;; keys is entirely dynamic, so we skip it.
    (dynamic-modules colours keys _G)

    (globals
      :max
      _CC_DEFAULT_SETTINGS
      _CC_DISABLE_LUA51_FEATURES
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
  (; Java APIs
   /doc/stub/http.lua
   /doc/stub/os.lua
   /doc/stub/turtle.lua
   /doc/stub/global.lua
   ; Java generated APIs
   /build/docs/luaJavadoc/turtle.lua
   ; Peripherals
   /build/docs/luaJavadoc/drive.lua
   /build/docs/luaJavadoc/speaker.lua
   /build/docs/luaJavadoc/printer.lua
   ; Lua APIs
   /src/main/resources/*/computercraft/lua/rom/apis/io.lua
   /src/main/resources/*/computercraft/lua/rom/apis/window.lua)

  (linters -doc:undocumented -doc:undocumented-arg -doc:undocumented-return))

;; These currently rely on unknown references.
(at
  (/src/main/resources/*/computercraft/lua/rom/apis/textutils.lua
   /src/main/resources/*/computercraft/lua/rom/modules/main/cc/completion.lua
   /src/main/resources/*/computercraft/lua/rom/modules/main/cc/shell/completion.lua
   /src/main/resources/*/computercraft/lua/rom/programs/shell.lua
   /doc/stub/fs.lua)
  (linters -doc:unresolved-reference))

;; Suppress warnings for the BIOS using its own deprecated members for now.
(at /src/main/resources/*/computercraft/lua/bios.lua
  (linters -var:deprecated))

(at /src/test/resources/test-rom
  ; We should still be able to test deprecated members.
  (linters -var:deprecated)

  (lint
    (globals
      :max sleep write
      cct_test describe expect howlci fail it pending stub)))

(at /src/web/mount/expr_template.lua (lint (globals :max __expr__)))
