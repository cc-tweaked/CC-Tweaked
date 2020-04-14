local capture = require "test_helpers".capture_program

local gistout = [[{
  "url": "https://api.github.com/gists/aa5a315d61ae9438b18d",
  "forks_url": "https://api.github.com/gists/aa5a315d61ae9438b18d/forks",
  "commits_url": "https://api.github.com/gists/aa5a315d61ae9438b18d/commits",
  "id": "aa5a315d61ae9438b18d",
  "node_id": "MDQ6R2lzdGFhNWEzMTVkNjFhZTk0MzhiMThk",
  "git_pull_url": "https://gist.github.com/aa5a315d61ae9438b18d.git",
  "git_push_url": "https://gist.github.com/aa5a315d61ae9438b18d.git",
  "html_url": "https://gist.github.com/aa5a315d61ae9438b18d",
  "files": {
    "init.lua": {
      "filename": "init.lua",
      "type": "application/x-lua",
      "language": "Lua",
      "raw_url": "aaaaa",
      "size": 1,
      "truncated": false,
      "content": "print(\"Hello\", ...)"
    },
    "hello_world.rb": {
      "filename": "hello_world.rb",
      "type": "application/x-ruby",
      "language": "Ruby",
      "raw_url": "https://gist.githubusercontent.com/octocat/6cad326836d38bd3a7ae/raw/db9c55113504e46fa076e7df3a04ce592e2e86d8/hello_world.rb",
      "size": 167,
      "truncated": false,
      "content": "class HelloWorld\n   def initialize(name)\n      @name = name.capitalize\n   end\n   def sayHi\n      puts \"Hello !\"\n   end\nend\n\nhello = HelloWorld.new(\"World\")\nhello.sayHi"
    },
    "hello_world.py": {
      "filename": "hello_world.py",
      "type": "application/x-python",
      "language": "Python",
      "raw_url": "https://gist.githubusercontent.com/octocat/e29f3839074953e1cc2934867fa5f2d2/raw/99c1bf3a345505c2e6195198d5f8c36267de570b/hello_world.py",
      "size": 199,
      "truncated": false,
      "content": "class HelloWorld:\n\n    def __init__(self, name):\n        self.name = name.capitalize()\n       \n    def sayHi(self):\n        print \"Hello \" + self.name + \"!\"\n\nhello = HelloWorld(\"world\")\nhello.sayHi()"
    },
    "hello_world_ruby.txt": {
      "filename": "hello_world_ruby.txt",
      "type": "text/plain",
      "language": "Text",
      "raw_url": "https://gist.githubusercontent.com/octocat/e29f3839074953e1cc2934867fa5f2d2/raw/9e4544db60e01a261aac098592b11333704e9082/hello_world_ruby.txt",
      "size": 46,
      "truncated": false,
      "content": "Run `ruby hello_world.rb` to print Hello World"
    },
    "hello_world_python.txt": {
      "filename": "hello_world_python.txt",
      "type": "text/plain",
      "language": "Text",
      "raw_url": "https://gist.githubusercontent.com/octocat/e29f3839074953e1cc2934867fa5f2d2/raw/076b4b78c10c9b7e1e0b73ffb99631bfc948de3b/hello_world_python.txt",
      "size": 48,
      "truncated": false,
      "content": "Run `python hello_world.py` to print Hello World"
    }
  },
  "public": true,
  "created_at": "2010-04-14T02:15:15Z",
  "updated_at": "2011-06-20T11:34:15Z",
  "description": "Hello World Examples",
  "comments": 0,
  "user": null,
  "comments_url": "https://api.github.com/gists/aa5a315d61ae9438b18d/comments/",
  "owner": {
    "login": "octocat",
    "id": 1,
    "node_id": "MDQ6VXNlcjE=",
    "avatar_url": "https://github.com/images/error/octocat_happy.gif",
    "gravatar_id": "",
    "url": "https://api.github.com/users/octocat",
    "html_url": "https://github.com/octocat",
    "followers_url": "https://api.github.com/users/octocat/followers",
    "following_url": "https://api.github.com/users/octocat/following{/other_user}",
    "gists_url": "https://api.github.com/users/octocat/gists{/gist_id}",
    "starred_url": "https://api.github.com/users/octocat/starred{/owner}{/repo}",
    "subscriptions_url": "https://api.github.com/users/octocat/subscriptions",
    "organizations_url": "https://api.github.com/users/octocat/orgs",
    "repos_url": "https://api.github.com/users/octocat/repos",
    "events_url": "https://api.github.com/users/octocat/events{/privacy}",
    "received_events_url": "https://api.github.com/users/octocat/received_events",
    "type": "User",
    "site_admin": false
  },
  "truncated": false,
  "forks": [
    {
      "user": {
        "login": "octocat",
        "id": 1,
        "node_id": "MDQ6VXNlcjE=",
        "avatar_url": "https://github.com/images/error/octocat_happy.gif",
        "gravatar_id": "",
        "url": "https://api.github.com/users/octocat",
        "html_url": "https://github.com/octocat",
        "followers_url": "https://api.github.com/users/octocat/followers",
        "following_url": "https://api.github.com/users/octocat/following{/other_user}",
        "gists_url": "https://api.github.com/users/octocat/gists{/gist_id}",
        "starred_url": "https://api.github.com/users/octocat/starred{/owner}{/repo}",
        "subscriptions_url": "https://api.github.com/users/octocat/subscriptions",
        "organizations_url": "https://api.github.com/users/octocat/orgs",
        "repos_url": "https://api.github.com/users/octocat/repos",
        "events_url": "https://api.github.com/users/octocat/events{/privacy}",
        "received_events_url": "https://api.github.com/users/octocat/received_events",
        "type": "User",
        "site_admin": false
      },
      "url": "https://api.github.com/gists/dee9c42e4998ce2ea439",
      "id": "dee9c42e4998ce2ea439",
      "created_at": "2011-04-14T16:00:49Z",
      "updated_at": "2011-04-14T16:00:49Z"
    }
  ],
  "history": [
    {
      "url": "https://api.github.com/gists/aa5a315d61ae9438b18d/57a7f021a713b1c5a6a199b54cc514735d2d462f",
      "version": "57a7f021a713b1c5a6a199b54cc514735d2d462f",
      "user": {
        "login": "octocat",
        "id": 1,
        "node_id": "MDQ6VXNlcjE=",
        "avatar_url": "https://github.com/images/error/octocat_happy.gif",
        "gravatar_id": "",
        "url": "https://api.github.com/users/octocat",
        "html_url": "https://github.com/octocat",
        "followers_url": "https://api.github.com/users/octocat/followers",
        "following_url": "https://api.github.com/users/octocat/following{/other_user}",
        "gists_url": "https://api.github.com/users/octocat/gists{/gist_id}",
        "starred_url": "https://api.github.com/users/octocat/starred{/owner}{/repo}",
        "subscriptions_url": "https://api.github.com/users/octocat/subscriptions",
        "organizations_url": "https://api.github.com/users/octocat/orgs",
        "repos_url": "https://api.github.com/users/octocat/repos",
        "events_url": "https://api.github.com/users/octocat/events{/privacy}",
        "received_events_url": "https://api.github.com/users/octocat/received_events",
        "type": "User",
        "site_admin": false
      },
      "change_status": {
        "deletions": 0,
        "additions": 180,
        "total": 180
      },
      "committed_at": "2010-04-14T02:15:15Z"
    }
  ]
}]]

describe("The gist program", function()
    local function setup_request()
        stub(_G, "http", {
            checkURL = function()
                return true
            end,
            get = function()
                return {
                    readAll = function()
                        return gistout
                    end,
                    close = function()
                    end,
                    getResponseHeaders = function()
                        local tHeader = {}
                        tHeader["Content-Type"] = "application/json"
                        return tHeader
                    end,
                    getResponseCode = function()
                        return 200
                    end,
                }
            end,
            post = function(tab)
                return {
                    readAll = function()
                        if type(tab) == "table" and tab.method == "PATCH" then 
                            return gistout
                                :gsub('"hello_world.rb": %b{},\n', "")
                                :gsub('"Hello World Examples"', '"Hello World Examples (Updated)"')
                        else return gistout end
                    end,
                    close = function()
                    end,
                    getResponseCode = function()
                        if type(tab) ~= "table" then return 201
                        elseif tab.method == "PATCH" then return 200
                        else return 204 end
                    end,
                }
            end,
        })
        stub(_G, "settings", {
            get = function()
                return "0123456789abcdef"
            end,
        })
    end

    it("downloads one file", function()
        setup_request()
        capture(stub, "gist", "get", "aa5a315d61ae9438b18d", "testdown")

        expect(fs.exists("/testdown")):eq(true)
    end)

    it("runs a program from the internet", function()
        setup_request()

        expect(capture(stub, "gist", "run", "aa5a315d61ae9438b18d", "a", "b", "c"))
            :matches { ok = true, output = "Connecting to api.github.com... Success.\nHello a b c\n", error = "" }
    end)

    it("gets info about a file", function()
        setup_request()
        expect(capture(stub, "gist", "info", "aa5a315d61ae9438b18d"))
            :matches { ok = true, output = "Connecting to api.github.com... Success.\nDescription: Hello World Examples\nAuthor: octocat\nRevisions: 1\nFiles in this Gist:\n\n\n", error = "" } -- No file info because it uses positioning functions
    end)

    it("upload a program to gist", function()
        setup_request()

        local file = fs.open( "testup", "w" )
        file.close()

        expect(capture(stub, "gist", "put", "testup"))
            :matches { ok = true, output = "Connecting to api.github.com... Success.\nUploaded as https://gist.github.com/aa5a315d61ae9438b18d\nRun 'gist get aa5a315d61ae9438b18d' to download anywhere\n", error = "" }
    end)

    it("upload a not existing program to gist", function()
        setup_request()

        expect(capture(stub, "gist", "put", "nothing"))
            :matches { ok = true, output = "Could not read nothing.\n", error = "" }
    end)

    it("edit a Gist", function()
        setup_request()

        expect(capture(stub, "gist", "edit", "aa5a315d61ae9438b18d", "hello_world.rb", "--", "Hello", "World", "Examples", "(Updated)"))
            :matches { ok = true, output = "Connecting to api.github.com... Success.\nUploaded as https://gist.github.com/aa5a315d61ae9438b18d\nRun 'gist get aa5a315d61ae9438b18d' to download anywhere\n", error = "" }
    end)

    it("delete a Gist", function()
        setup_request()

        expect(capture(stub, "gist", "delete", "aa5a315d61ae9438b18d"))
            :matches { ok = true, output = "Connecting to api.github.com... Success.\nThe requested Gist has been deleted.\n", error = "" }
    end)

    it("displays its usage when given no arguments", function()
        setup_request()

        expect(capture(stub, "gist"))
            :matches { ok = true, output = "Usages:\ngist put <filenames...> [-- description...]\ngist edit <id> <filenames...> [-- description]\ngist delete <id>\ngist get <id> <filename>\ngist run <id> [arguments...]\ngist info <id>\n", error = "" }
    end)

    it("can be completed", function()
        local complete = shell.getCompletionInfo()["rom/programs/http/gist.lua"].fnComplete
        expect(complete(shell, 1, "", {})):same { "put ", "edit ", "delete ", "get ", "run ", "info " }
    end)
end)
