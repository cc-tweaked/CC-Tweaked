describe("cc.http.pastebin", function()
  local paste = require("cc.http.pastebin")
  
  describe("get", function()
    it("gets a string from pastebin", function()
      local pasteString = paste.get("GW6SRgk8")
      expect(pasteString):eq("This is a test.")
    end)
  end)
  
  describe("put", function()
    it("puts a paste onto pastebin", function()
      local pasteString = "test"
      local pasteID = paste.put(pasteString)
      expect(type(pasteID)):eq("string")
    end)
  end)
end)
