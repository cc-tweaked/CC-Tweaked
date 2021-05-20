# Lua VM tests

Unlike the rest of the test suites, the code in this folder doesn't test any
(well, much) CC code. Instead, it ensures that the Lua VM behaves in a way that
we expect.

The VM that CC uses (LuaJ and later Cobalt) does not really conform to any one
version of Lua, instead supporting a myriad of features from Lua 5.1 to 5.3 (and
not always accurately). These tests attempt to pin down what behaviour is
required for a well behaved emulator.


These tests are split into several folders:
 - `/` Tests for CC specific functionality, based on Cobalt/Lua quirks or needs
   of CC.
 - `puc`: Tests derived from the [PUC Lua test suite][puc-tests].


[puc-tests]: https://www.lua.org/tests/ "Lua: test suites"
