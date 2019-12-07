; -*- mode: Lisp;-*-

(sources
  /src/main/resources/assets/computercraft/lua/bios.lua
  /src/main/resources/assets/computercraft/lua/rom/
  /src/test/resources/test-rom)

(at /
  (linters
    ;; It'd be nice to avoid this, but right now there's a lot of instances of it.
    -var:set-loop

    ;; It's useful to name arguments for documentation, so we allow this. It'd
    ;; be good to find a compromise in the future, but this works for now.
    -var:unused-arg))

;; We disable the two global linters in bios.lua and the APIs. In the future
;; hopefully we'll get illuaminate to handle this.
(at
  (/src/main/resources/assets/computercraft/lua/bios.lua
   /src/main/resources/assets/computercraft/lua/rom/apis/)
  (linters -var:set-global -var:unused-global))

;; These warnings are broken right now
(at completion.lua (linters -doc:malformed-type))
(at (bios.lua worm.lua) (linters -control:unreachable))
