; -*- mode: Lisp;-*-

(sources
  /doc/stub/
  /src/main/resources/*/computercraft/lua/bios.lua
  /src/main/resources/*/computercraft/lua/rom/
  /src/test/resources/test-rom)


(doc
  (title "CC: Tweaked")
  (index doc/index.md)
  (source-link https://github.com/SquidDev-CC/CC-Tweaked/blob/${commit}/${path}#L${line})

  (module-kinds
    (peripheral Peripherals))

  (library-path
    /doc/stub/

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
    (dynamic-modules colours keys)

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
(at /doc/stub
  (linters -var:unused-global)
  (lint (allow-toplevel-global true)))

;; Suppress warnings for currently undocumented modules.
(at
  (; Java APIs
   /doc/stub/fs.lua
   /doc/stub/http.lua
   /doc/stub/os.lua
   /doc/stub/term.lua
   /doc/stub/turtle.lua
   ; Peripherals
   /doc/stub/drive.lua
   /doc/stub/modem.lua
   /doc/stub/printer.lua
   ; Lua APIs
   /src/main/resources/*/computercraft/lua/rom/apis/io.lua
   /src/main/resources/*/computercraft/lua/rom/apis/window.lua)

  (linters -doc:undocumented -doc:undocumented-arg))

;; These currently rely on unknown references.
(at
  (/src/main/resources/*/computercraft/lua/rom/apis/textutils.lua
   /src/main/resources/*/computercraft/lua/rom/modules/main/cc/completion.lua
   /src/main/resources/*/computercraft/lua/rom/modules/main/cc/shell/completion.lua
   /src/main/resources/*/computercraft/lua/rom/programs/shell.lua)
  (linters -doc:unresolved-reference))

(at /src/test/resources/test-rom
  (lint
    (globals
      :max sleep write
      cct_test describe expect howlci fail it pending stub)))
