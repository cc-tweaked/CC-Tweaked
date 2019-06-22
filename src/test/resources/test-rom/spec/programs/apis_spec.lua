local capture = require "test_helpers".capture_program

describe("The apis program", function()

    it("run the program", function()       
        expect(capture(stub, "apis"))
            :matches { ok = true, output = "bit\nbit32\ncct_test\ncolors\ncolours\ncoroutine\ndebug\ndisk\nfs\ngps\nhelp\nhttp\nio\nkeys\nmath\nmultishell\nos\npackage\npaintutils\nparallel\nperipheral\nrednet\nredstone\nrs\nsettings\nshell\nstring\ntable\nterm\ntextutils\nvector\nwindow\n", error = "" }
    end)
end)
