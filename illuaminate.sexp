; -*- mode: Lisp;-*-

(sources
  /src/main/resources/assets/computercraft/lua/bios.lua
  /src/main/resources/assets/computercraft/lua/rom/)

(at /
  (linters
    ;; We disable the two global linters for now, as Illuaminate really doesn't
    ;; like CC's (mis)use of them.
    -var:set-global -var:unused-global -var:unused-arg
    ;; It'd be nice to avoid this, but right now there's a lot of instances of it.
    -var:set-loop
    ))


;; These warning is broken right now
(at "completion.lua" (linters -doc:malformed-type))
(at "bios.lua" (linters -control:unreachable))
(at "worm.lua" (linters -control:unreachable))
