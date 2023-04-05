-- SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
--
-- SPDX-License-Identifier: MPL-2.0

describe("The gps library", function()
    describe("gps.locate", function()
        it("validates arguments", function()
            stub(_G, "commands", { getBlockPosition = function()
            end, })

            gps.locate()
            gps.locate(1)
            gps.locate(1, true)

            expect.error(gps.locate, ""):eq("bad argument #1 (number expected, got string)")
            expect.error(gps.locate, 1, ""):eq("bad argument #2 (boolean expected, got string)")
        end)
    end)

    describe("on fake computers", function()
        local fake_computer = require "support.fake_computer"

        local function gps_reciever(x, y, z, fn)
            local computer = fake_computer.make_computer(1, fn)
            computer.position = vector.new(x, y, z)
            fake_computer.add_api(computer, "rom/apis/gps.lua")
            fake_computer.add_api(computer, "rom/apis/vector.lua")

            local modem = fake_computer.add_modem(computer, "back")
            return computer, modem
        end

        local function gps_hosts(computer, modem, positions)
            local computers = {}
            for _, position in pairs(positions) do
                local position = vector.new(position[1], position[2], position[3])
                local host = fake_computer.make_computer(99, function(env)
                    env.loadfile("/rom/programs/gps.lua")("host", position.x, position.y, position.z)
                end)
                host.position = position
                fake_computer.add_api(host, "rom/apis/gps.lua")
                fake_computer.add_api(host, "rom/apis/vector.lua")

                local host_modem = fake_computer.add_modem(host, "back")
                fake_computer.add_modem_edge(modem, host_modem)

                computers[#computers + 1] = host
            end

            computers[#computers + 1] = computer -- Run the computer after hosts have started
            return computers
        end

        it("locates a computer", function()
            local computer, modem = gps_reciever(12, 23, 52, function(env)
                local x, y, z = env.gps.locate()
                expect({ x, y, z }):same { 12, 23, 52 }
            end)
            local computers = gps_hosts(computer, modem, {
                { 5, 5, 5 },
                { 10, 5, 5 },
                { 5, 10, 5 },
                { 5, 5, 10 },
            })

            fake_computer.run_all(computers, false)
            fake_computer.advance_all(computers, 2)
            fake_computer.run_all(computers, { computer })
        end)

        it("fails to locate a computer with insufficient hosts", function()
            local computer, modem = gps_reciever(12, 23, 52, function(env)
                local x, y, z = env.gps.locate()
                expect({ x, y, z }):same { nil, nil, nil }
            end)
            local computers = gps_hosts(computer, modem, {
                { 5, 5, 5 },
                { 10, 5, 5 },
                { 5, 10, 5 },
            })

            fake_computer.run_all(computers, false)
            fake_computer.advance_all(computers, 2)
            fake_computer.run_all(computers, { computer })
        end)

        it("doesn't fail on duplicate hosts", function()
            local computer, modem = gps_reciever(12, 23, 52, function(env)
                local x, y, z = env.gps.locate()
                expect({ x, y, z }):same { 12, 23, 52 }
            end)
            local computers = gps_hosts(computer, modem, {
                { 5, 5, 5 },
                { 5, 5, 5 },
                { 10, 5, 5 },
                { 5, 10, 5 },
                { 5, 5, 10 },
            })

            fake_computer.run_all(computers, false)
            fake_computer.advance_all(computers, 2)
            fake_computer.run_all(computers, { computer })
        end)
    end)
end)
