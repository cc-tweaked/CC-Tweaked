describe("The rednet library", function()
    describe("rednet.open", function()
        it("validates arguments", function()
            expect.error(rednet.open, nil):eq("bad argument #1 (expected string, got nil)")
        end)

        it("requires a modem to be present", function()
            expect.error(rednet.open, "not_there"):eq("No such modem: not_there")
        end)
    end)

    describe("rednet.close", function()
        it("validates arguments", function()
            rednet.close()
            expect.error(rednet.close, 1):eq("bad argument #1 (expected string, got number)")
            expect.error(rednet.close, false):eq("bad argument #1 (expected string, got boolean)")
        end)

        it("requires a modem to be present", function()
            expect.error(rednet.close, "not_there"):eq("No such modem: not_there")
        end)
    end)

    describe("rednet.isOpen", function()
        it("validates arguments", function()
            rednet.isOpen()
            rednet.isOpen("")
            expect.error(rednet.isOpen, 1):eq("bad argument #1 (expected string, got number)")
            expect.error(rednet.isOpen, false):eq("bad argument #1 (expected string, got boolean)")
        end)
    end)

    describe("rednet.send", function()
        it("validates arguments", function()
            rednet.send(1)
            rednet.send(1, nil, "")
            expect.error(rednet.send, nil):eq("bad argument #1 (expected number, got nil)")
            expect.error(rednet.send, 1, nil, false):eq("bad argument #3 (expected string, got boolean)")
        end)
    end)

    describe("rednet.broadcast", function()
        it("validates arguments", function()
            rednet.broadcast(nil)
            rednet.broadcast(nil, "")
            expect.error(rednet.broadcast, nil, false):eq("bad argument #2 (expected string, got boolean)")
        end)
    end)

    describe("rednet.receive", function()
        it("validates arguments", function()
            expect.error(rednet.receive, false):eq("bad argument #1 (expected string, got boolean)")
            expect.error(rednet.receive, "", false):eq("bad argument #2 (expected number, got boolean)")
        end)
    end)

    describe("rednet.host", function()
        it("validates arguments", function()
            expect.error(rednet.host, "", "localhost"):eq("Reserved hostname")
            expect.error(rednet.host, nil):eq("bad argument #1 (expected string, got nil)")
            expect.error(rednet.host, "", nil):eq("bad argument #2 (expected string, got nil)")
        end)
    end)

    describe("rednet.unhost", function()
        it("validates arguments", function()
            rednet.unhost("")
            expect.error(rednet.unhost, nil):eq("bad argument #1 (expected string, got nil)")
        end)
    end)

    describe("rednet.lookup", function()
        it("validates arguments", function()
            expect.error(rednet.lookup, nil):eq("bad argument #1 (expected string, got nil)")
            expect.error(rednet.lookup, "", false):eq("bad argument #2 (expected string, got boolean)")
        end)

        it("gets a locally hosted protocol", function()
            rednet.host("a_protocol", "a_hostname")

            expect(rednet.lookup("a_protocol")):eq(os.getComputerID())
            expect(rednet.lookup("a_protocol", "localhost")):eq(os.getComputerID())
            expect(rednet.lookup("a_protocol", "a_hostname")):eq(os.getComputerID())
        end)
    end)

    describe("on fake computers", function()
        local fake_computer = require "support.fake_computer"
        local debugx = require "support.debug_ext"

        local function dawdle() while true do coroutine.yield() end end
        local function computer_with_rednet(id, fn, options)
            local computer = fake_computer.make_computer(id, function(env)
                local fns = { env.rednet.run }
                if options and options.rep then
                    fns[#fns + 1] = function() env.dofile("rom/programs/rednet/repeat.lua") end
                end

                if fn then
                    fns[#fns + 1] = function()
                        if options and options.open then
                            env.rednet.open("back")
                            env.os.queueEvent("x") env.os.pullEvent("x")
                        end
                        return fn(env.rednet, env)
                    end
                end

                if options and options.host then
                    env.rednet.host("some_protocol", "host_" .. id)
                end

                return parallel.waitForAny(table.unpack(fns))
            end)
            local modem = fake_computer.add_modem(computer, "back")
            fake_computer.add_api(computer, "rom/apis/rednet.lua")
            return computer, modem
        end

        it("opens and closes channels", function()
            local id = math.random(256)
            local computer = computer_with_rednet(id, function(rednet)
                expect(rednet.isOpen()):eq(false)

                rednet.open("back")
                rednet.open("front")

                expect(rednet.isOpen()):eq(true)
                expect(rednet.isOpen("back")):eq(true)
                expect(rednet.isOpen("front")):eq(true)

                rednet.close("back")
                expect(rednet.isOpen("back")):eq(false)
                expect(rednet.isOpen("front")):eq(true)
                expect(rednet.isOpen()):eq(true)

                rednet.close()

                expect(rednet.isOpen("back")):eq(false)
                expect(rednet.isOpen("front")):eq(false)
                expect(rednet.isOpen()):eq(false)
            end)
            fake_computer.add_modem(computer, "front")

            fake_computer.run_all { computer }
        end)

        it("sends and receives rednet messages", function()
            local computer_1, modem_1 = computer_with_rednet(1, function(rednet)
                rednet.send(2, "Hello")
            end, { open = true })
            local computer_2, modem_2 = computer_with_rednet(2, function(rednet)
                local id, message = rednet.receive()
                expect(id):eq(1)
                expect(message):eq("Hello")
            end, { open = true })
            fake_computer.add_modem_edge(modem_1, modem_2)

            fake_computer.run_all { computer_1, computer_2 }
        end)

        it("repeats messages between computers", function()
            local computer_1, modem_1 = computer_with_rednet(1, function(rednet)
                rednet.send(3, "Hello")
            end, { open = true })
            local computer_2, modem_2 = computer_with_rednet(2, nil, { open = true, rep = true })
            local computer_3, modem_3 = computer_with_rednet(3, function(rednet)
                local id, message = rednet.receive()
                expect(id):eq(1)
                expect(message):eq("Hello")
            end, { open = true })
            fake_computer.add_modem_edge(modem_1, modem_2)
            fake_computer.add_modem_edge(modem_2, modem_3)

            fake_computer.run_all({ computer_1, computer_2, computer_3 }, { computer_1, computer_3 })
        end)

        it("repeats messages between computers with massive ids", function()
            local id_1, id_3 = 24283947, 93428798
            local computer_1, modem_1 = computer_with_rednet(id_1, function(rednet)
                rednet.send(id_3, "Hello")
                local id, message = rednet.receive()
                expect { id, message }:same { id_3, "World" }
            end, { open = true })
            local computer_2, modem_2 = computer_with_rednet(2, nil, { open = true, rep = true })
            local computer_3, modem_3 = computer_with_rednet(id_3, function(rednet)
                rednet.send(id_1, "World")
                local id, message = rednet.receive()
                expect { id, message }:same { id_1, "Hello" }
            end, { open = true })
            fake_computer.add_modem_edge(modem_1, modem_2)
            fake_computer.add_modem_edge(modem_2, modem_3)

            fake_computer.run_all({ computer_1, computer_2, computer_3 }, { computer_1, computer_3 })
        end)

        it("ignores duplicate messages", function()
            local computer_1, modem_1 = computer_with_rednet(1, function(rednet)
                rednet.send(2, "Hello")
            end, { open = true })
            local computer_2, modem_2 = computer_with_rednet(2, function(rednet, env)
                local id, message = rednet.receive()
                expect { id, message }:same { 1, "Hello" }

                local id = rednet.receive(nil, 1)
                expect(id):eq(nil)

                env.sleep(10)

                -- Ensure our pending message store is empty. Bit ugly to prod internals, but there's no other way.
                expect(debugx.getupvalue(rednet.run, "received_messages")):same({})
                expect(debugx.getupvalue(rednet.run, "prune_received_timer")):eq(nil)
            end, { open = true })

            local computer_3, modem_3 = computer_with_rednet(3, nil, { open = true, rep = true })
            fake_computer.add_modem_edge(modem_1, modem_3)
            fake_computer.add_modem_edge(modem_3, modem_2)

            local computer_4, modem_4 = computer_with_rednet(4, nil, { open = true, rep = true })
            fake_computer.add_modem_edge(modem_1, modem_4)
            fake_computer.add_modem_edge(modem_4, modem_2)

            local computers = { computer_1, computer_2, computer_3, computer_4 }
            fake_computer.run_all(computers, false)
            fake_computer.advance_all(computers, 1)
            fake_computer.run_all(computers, { computer_1 })
            fake_computer.advance_all(computers, 10)
            fake_computer.run_all(computers, { computer_1, computer_2 })
        end)

        it("handles lookups between computers with massive IDs", function()
            local id_1, id_3 = 24283947, 93428798
            local computer_1, modem_1 = computer_with_rednet(id_1, function(rednet)
                local ids = { rednet.lookup("some_protocol") }
                expect(ids):same { id_3 }
            end, { open = true })
            local computer_2, modem_2 = computer_with_rednet(2, nil, { open = true, rep = true })
            local computer_3, modem_3 = computer_with_rednet(id_3, dawdle, { open = true, host = true })
            fake_computer.add_modem_edge(modem_1, modem_2)
            fake_computer.add_modem_edge(modem_2, modem_3)

            local computers = { computer_1, computer_2, computer_3 }
            fake_computer.run_all(computers, false)
            fake_computer.advance_all(computers, 3)
            fake_computer.run_all(computers, { computer_1 })
        end)
    end)
end)
