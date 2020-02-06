; -*- mode: Lisp;-*-

(sources
  /doc/stub/
  /src/main/resources/assets/computercraft/lua/bios.lua
  /src/main/resources/assets/computercraft/lua/rom/
  /src/test/resources/test-rom)


(doc
  (title "CC: Tweaked")
  (index doc/index.md)
  (source-link https://github.com/SquidDev-CC/CC-Tweaked/blob/${commit}/${path}#L${line})

  (library-path
    /doc/stub/

    /src/main/resources/assets/computercraft/lua/rom/apis
    /src/main/resources/assets/computercraft/lua/rom/apis/command
    /src/main/resources/assets/computercraft/lua/rom/apis/turtle

    /src/main/resources/assets/computercraft/lua/rom/modules/main
    /src/main/resources/assets/computercraft/lua/rom/modules/command
    /src/main/resources/assets/computercraft/lua/rom/modules/turtle))

(at /
  (linters
    ;; It'd be nice to avoid this, but right now there's a lot of instances of
    ;; it.
    -var:set-loop

    ;; It's useful to name arguments for documentation, so we allow this. It'd
    ;; be good to find a compromise in the future, but this works for now.
    -var:unused-arg

    ;; Suppress a couple of documentation comments warnings for now. We'll
    ;; hopefully be able to remove them in the coming weeks.
    -doc:undocumented -doc:undocumented-arg -doc:unresolved-reference))

;; We disable the unused global linter in bios.lua and the APIs. In the future
;; hopefully we'll get illuaminate to handle this.
(at
  (/src/main/resources/assets/computercraft/lua/bios.lua
   /src/main/resources/assets/computercraft/lua/rom/apis/)
  (linters -var:unused-global)
  (lint
    (allow-toplevel-global true)))

;; Shut up some variable warnings in documentation stubs. It's not like this is
;; actual code after all.
(at /doc/stub
  (linters -var:unused-global -var:unused -var:set-global))
