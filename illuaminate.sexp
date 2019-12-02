; -*- mode: Lisp;-*-

(sources
  /src/main/resources/assets/computercraft/lua/bios.lua
  /src/main/resources/assets/computercraft/lua/rom/)

(at /
  (linters
    ;; We disable the two global linters for now, as Illuaminate really doesn't
    ;; like CC's (mis)use of them.
    -var:set-global -var:unused-global -var:unused-arg
    ;; This warning is broken right now
    -doc:malformed-type
    ))
