; -*- mode: Lisp;-*-

(sources
  /doc/stub/
  /src/main/resources/data/computercraft/lua/bios.lua
  /src/main/resources/data/computercraft/lua/rom/
  /src/test/resources/test-rom)

(doc
  (title "CC: Tweaked")
  (index doc/index.md)
  (source-link https://github.com/SquidDev-CC/CC-Tweaked/blob/${commit}/${path}#L${line})

  (library-path
    /doc/stub/

    /src/main/resources/data/computercraft/lua/rom/apis
    /src/main/resources/data/computercraft/lua/rom/apis/command
    /src/main/resources/data/computercraft/lua/rom/apis/turtle

    /src/main/resources/data/computercraft/lua/rom/modules/main
    /src/main/resources/data/computercraft/lua/rom/modules/command
    /src/main/resources/data/computercraft/lua/rom/modules/turtle))

(at /
  (linters
    syntax:string-index

    ;; It'd be nice to avoid this, but right now there's a lot of instances of
    ;; it.
    -var:set-loop

    ;; It's useful to name arguments for documentation, so we allow this. It'd
    ;; be good to find a compromise in the future, but this works for now.
    -var:unused-arg

    ;; Suppress a couple of documentation comments warnings for now. We'll
    ;; hopefully be able to remove them in the future.
    -doc:undocumented -doc:undocumented-arg -doc:unresolved-reference
    -var:unresolved-member)
  (lint
    (bracket-spaces
      (call no-space)
      (function-args no-space)
      (parens no-space)
      (table space)
      (index no-space))))

;; We disable the unused global linter in bios.lua and the APIs. In the future
;; hopefully we'll get illuaminate to handle this.
(at
  (/src/main/resources/data/computercraft/lua/bios.lua
   /src/main/resources/data/computercraft/lua/rom/apis/)
  (linters -var:unused-global)
  (lint (allow-toplevel-global true)))

;; Silence some variable warnings in documentation stubs.
(at /doc/stub
  (linters -var:unused-global)
  (lint (allow-toplevel-global true)))

;; Ensure any fully documented modules stay fully documented.
(at
  (/src/main/resources/data/computercraft/lua/rom/apis/colors.lua
   /src/main/resources/data/computercraft/lua/rom/apis/colours.lua
   /src/main/resources/data/computercraft/lua/rom/apis/disk.lua
   /src/main/resources/data/computercraft/lua/rom/apis/gps.lua
   /src/main/resources/data/computercraft/lua/rom/apis/help.lua
   /src/main/resources/data/computercraft/lua/rom/apis/keys.lua
   /src/main/resources/data/computercraft/lua/rom/apis/paintutils.lua
   /src/main/resources/data/computercraft/lua/rom/apis/parallel.lua
   /src/main/resources/data/computercraft/lua/rom/apis/peripheral.lua
   /src/main/resources/data/computercraft/lua/rom/apis/rednet.lua
   /src/main/resources/data/computercraft/lua/rom/apis/settings.lua
   /src/main/resources/data/computercraft/lua/rom/apis/texutils.lua
   /src/main/resources/data/computercraft/lua/rom/apis/vector.lua)
  (linters doc:undocumented doc:undocumented-arg))
