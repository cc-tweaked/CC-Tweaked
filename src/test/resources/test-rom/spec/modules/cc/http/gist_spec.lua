describe("cc.http.gist", function()
    local function setup_request()
        stub(_G, "http", {
            checkURL = function()
                return true
            end,
            get = function()
                local file = fs.open("test-rom/data/gist-data.json", "r")
                file.getResponseHeaders = function()
                    local tHeader = {}
                    tHeader["Content-Type"] = "application/json"
                    return tHeader
                end
                file.getResponseCode = function()
                    return 200
                end
                return file
            end,
            post = function(tab)
                local file = fs.open("test-rom/data/gist-data.json", "r")
                if type(tab) == "table" and tab.method == "PATCH" then
                    local oldReadAll = file.readAll
                    file.readAll = function()
                        return oldReadAll()
                            :gsub('"hello_world.rb": %b{},\n', "")
                            :gsub('"Hello World Examples"', '"Hello World Examples (Updated)"'), nil
                    end
                end
                file.getResponseCode = function()
                    if type(tab) ~= "table" then return 201
                    elseif tab.method == "PATCH" then return 200
                    else return 204 end
                end
                return file
            end,
        })
        stub(_G, "settings", {
            get = function()
                return "0123456789abcdef"
            end,
        })
    end

    local gist = require("cc.http.gist")

    describe("get", function()
        it("downloads one file", function()
            setup_request()
            expect(gist.get("aa5a315d61ae9438b18d")):eq "print(\"Hello\", ...) return 7, 4"
        end)

        it("downloads the requested file", function()
            setup_request()
            expect(gist.get("aa5a315d61ae9438b18d/hello_world_ruby.txt")):eq "Run `ruby hello_world.rb` to print Hello World"
        end)
    end)

    describe("getAll", function()
        it("downloads all files", function()
            setup_request()
            expect(gist.getAll("aa5a315d61ae9438b18d")):same {
                ["init.lua"] = "print(\"Hello\", ...) return 7, 4",
                ["hello_world.rb"] = "class HelloWorld\n   def initialize(name)\n      @name = name.capitalize\n   end\n   def sayHi\n      puts \"Hello !\"\n   end\nend\n\nhello = HelloWorld.new(\"World\")\nhello.sayHi",
                ["hello_world.py"] = "class HelloWorld:\n\n    def __init__(self, name):\n        self.name = name.capitalize()\n       \n    def sayHi(self):\n        print \"Hello \" + self.name + \"!\"\n\nhello = HelloWorld(\"world\")\nhello.sayHi()",
                ["hello_world_ruby.txt"] = "Run `ruby hello_world.rb` to print Hello World",
                ["hello_world_python.txt"] = "Run `python hello_world.py` to print Hello World"
            }
        end)
    end)

    describe("run", function()
        it("runs a program from the internet", function()
            setup_request()

            expect(gist.run("aa5a315d61ae9438b18d", "a", "b", "c")):eq(7, 4)
        end)
    end)

    describe("info", function()
        it("gets info about a file", function()
            setup_request()
            expect(gist.info("aa5a315d61ae9438b18d")):same {
                description = "Hello World Examples",
                author = "octocat",
                revisionCount = 1,
                files = {"hello_world.py", "hello_world.rb", "hello_world_python.txt", "hello_world_ruby.txt", "init.lua"}
            }
        end)
    end)

    describe("put", function()
        it("upload a program to Gist", function()
            setup_request()

            expect(gist.put({["test.txt"] = "Hello"})):eq("aa5a315d61ae9438b18d", "https://gist.github.com/aa5a315d61ae9438b18d")
        end)

        it("edit a Gist", function()
            setup_request()

            expect(gist.put({["hello_world.rb"] = textutils.json_null}, "Hello World Examples (Updated)", "aa5a315d61ae9438b18d")):eq("aa5a315d61ae9438b18d", "https://gist.github.com/aa5a315d61ae9438b18d")
        end)
    end)

    describe("delete", function()
        it("delete a Gist", function()
            setup_request()

            expect(gist.delete("aa5a315d61ae9438b18d")):eq(true)
        end)
    end)
end)
