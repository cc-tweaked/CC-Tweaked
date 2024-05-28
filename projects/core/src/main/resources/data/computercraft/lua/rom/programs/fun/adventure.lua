-- SPDX-FileCopyrightText: 2017 Daniel Ratcliffe
--
-- SPDX-License-Identifier: LicenseRef-CCPL

local tBiomes = {
    "in a forest",
    "in a pine forest",
    "knee deep in a swamp",
    "in a mountain range",
    "in a desert",
    "in a grassy plain",
    "in frozen tundra",
}

local function hasTrees(_nBiome)
    return _nBiome <= 3
end

local function hasStone(_nBiome)
    return _nBiome == 4
end

local function hasRivers(_nBiome)
    return _nBiome ~= 3 and _nBiome ~= 5
end

local items = {
    ["no tea"] = {
        droppable = false,
        desc = "Pull yourself together man.",
    },
    ["a pig"] = {
        heavy = true,
        creature = true,
        drops = { "some pork" },
        aliases = { "pig" },
        desc = "The pig has a square nose.",
    },
    ["a cow"] = {
        heavy = true,
        creature = true,
        aliases = { "cow" },
        desc = "The cow stares at you blankly.",
    },
    ["a sheep"] = {
        heavy = true,
        creature = true,
        hitDrops = { "some wool" },
        aliases = { "sheep" },
        desc = "The sheep is fluffy.",
    },
    ["a chicken"] = {
        heavy = true,
        creature = true,
        drops = { "some chicken" },
        aliases = { "chicken" },
        desc = "The chicken looks delicious.",
    },
    ["a creeper"] = {
        heavy = true,
        creature = true,
        monster = true,
        aliases = { "creeper" },
        desc = "The creeper needs a hug.",
    },
    ["a skeleton"] = {
        heavy = true,
        creature = true,
        monster = true,
        aliases = { "skeleton" },
        nocturnal = true,
        desc = "The head bone's connected to the neck bone, the neck bone's connected to the chest bone, the chest bone's connected to the arm bone, the arm bone's connected to the bow, and the bow is pointed at you.",
    },
    ["a zombie"] = {
        heavy = true,
        creature = true,
        monster = true,
        aliases = { "zombie" },
        nocturnal = true,
        desc = "All he wants to do is eat your brains.",
    },
    ["a spider"] = {
        heavy = true,
        creature = true,
        monster = true,
        aliases = { "spider" },
        desc = "Dozens of eyes stare back at you.",
    },
    ["a cave entrance"] = {
        heavy = true,
        aliases = { "cave entance", "cave", "entrance" },
        desc = "The entrance to the cave is dark, but it looks like you can climb down.",
    },
    ["an exit to the surface"] = {
        heavy = true,
        aliases = { "exit to the surface", "exit", "opening" },
        desc = "You can just see the sky through the opening.",
    },
    ["a river"] = {
        heavy = true,
        aliases = { "river" },
        desc = "The river flows majestically towards the horizon. It doesn't do anything else.",
    },
    ["some wood"] = {
        aliases = { "wood" },
        material = true,
        desc = "You could easily craft this wood into planks.",
    },
    ["some planks"] = {
        aliases = { "planks", "wooden planks", "wood planks" },
        material = true,
        desc = "You could easily craft these planks into sticks.",
    },
    ["some sticks"] = {
        aliases = { "sticks", "wooden sticks", "wood sticks" },
        desc = "A perfect handle for torches or a pickaxe.",
    },
    ["a crafting table"] = {
        aliases = { "crafting table", "craft table", "work bench", "workbench", "crafting bench", "table" },
        desc = "It's a crafting table. I shouldn't tell you this, but these don't actually do anything in this game, you can craft tools whenever you like.",
    },
    ["a furnace"] = {
        aliases = { "furnace" },
        desc = "It's a furnace. Between you and me, these don't actually do anything in this game.",
    },
    ["a wooden pickaxe"] = {
        aliases = { "pickaxe", "pick", "wooden pick", "wooden pickaxe", "wood pick", "wood pickaxe" },
        tool = true,
        toolLevel = 1,
        toolType = "pick",
        desc = "The pickaxe looks good for breaking stone and coal.",
    },
    ["a stone pickaxe"] = {
        aliases = { "pickaxe", "pick", "stone pick", "stone pickaxe" },
        tool = true,
        toolLevel = 2,
        toolType = "pick",
        desc = "The pickaxe looks good for breaking iron.",
    },
    ["an iron pickaxe"] = {
        aliases = { "pickaxe", "pick", "iron pick", "iron pickaxe" },
        tool = true,
        toolLevel = 3,
        toolType = "pick",
        desc = "The pickaxe looks strong enough to break diamond.",
    },
    ["a diamond pickaxe"] = {
        aliases = { "pickaxe", "pick", "diamond pick", "diamond pickaxe" },
        tool = true,
        toolLevel = 4,
        toolType = "pick",
        desc = "Best. Pickaxe. Ever.",
    },
    ["a wooden sword"] = {
        aliases = { "sword", "wooden sword", "wood sword" },
        tool = true,
        toolLevel = 1,
        toolType = "sword",
        desc = "Flimsy, but better than nothing.",
    },
    ["a stone sword"] = {
        aliases = { "sword", "stone sword" },
        tool = true,
        toolLevel = 2,
        toolType = "sword",
        desc = "A pretty good sword.",
    },
    ["an iron sword"] = {
        aliases = { "sword", "iron sword" },
        tool = true,
        toolLevel = 3,
        toolType = "sword",
        desc = "This sword can slay any enemy.",
    },
    ["a diamond sword"] = {
        aliases = { "sword", "diamond sword" },
        tool = true,
        toolLevel = 4,
        toolType = "sword",
        desc = "Best. Sword. Ever.",
    },
    ["a wooden shovel"] = {
        aliases = { "shovel", "wooden shovel", "wood shovel" },
        tool = true,
        toolLevel = 1,
        toolType = "shovel",
        desc = "Good for digging holes.",
    },
    ["a stone shovel"] = {
        aliases = { "shovel", "stone shovel" },
        tool = true,
        toolLevel = 2,
        toolType = "shovel",
        desc = "Good for digging holes.",
    },
    ["an iron shovel"] = {
        aliases = { "shovel", "iron shovel" },
        tool = true,
        toolLevel = 3,
        toolType = "shovel",
        desc = "Good for digging holes.",
    },
    ["a diamond shovel"] = {
        aliases = { "shovel", "diamond shovel" },
        tool = true,
        toolLevel = 4,
        toolType = "shovel",
        desc = "Good for digging holes.",
    },
    ["some coal"] = {
        aliases = { "coal" },
        ore = true,
        toolLevel = 1,
        toolType = "pick",
        desc = "That coal looks useful for building torches, if only you had a pickaxe to mine it.",
    },
    ["some dirt"] = {
        aliases = { "dirt" },
        material = true,
        desc = "Why not build a mud hut?",
    },
    ["some stone"] = {
        aliases = { "stone", "cobblestone" },
        material = true,
        ore = true,
        infinite = true,
        toolLevel = 1,
        toolType = "pick",
        desc = "Stone is useful for building things, and making stone pickaxes.",
    },
    ["some iron"] = {
        aliases = { "iron" },
        material = true,
        ore = true,
        toolLevel = 2,
        toolType = "pick",
        desc = "That iron looks mighty strong, you'll need a stone pickaxe to mine it.",
    },
    ["some diamond"] = {
        aliases = { "diamond", "diamonds" },
        material = true,
        ore = true,
        toolLevel = 3,
        toolType = "pick",
        desc = "Sparkly, rare, and impossible to mine without an iron pickaxe.",
    },
    ["some torches"] = {
        aliases = { "torches", "torch" },
        desc = "These won't run out for a while.",
    },
    ["a torch"] = {
        aliases = { "torch" },
        desc = "Fire, fire, burn so bright, won't you light my cave tonight?",
    },
    ["some wool"] = {
        aliases = { "wool" },
        material = true,
        desc = "Soft and good for building.",
    },
    ["some pork"] = {
        aliases = { "pork", "porkchops" },
        food = true,
        desc = "Delicious and nutritious.",
    },
    ["some chicken"] = {
        aliases = { "chicken" },
        food = true,
        desc = "Finger licking good.",
    },
}

local tAnimals = {
    "a pig", "a cow", "a sheep", "a chicken",
}

local tMonsters = {
    "a creeper", "a skeleton", "a zombie", "a spider",
}

local tRecipes = {
    ["some planks"] = { "some wood" },
    ["some sticks"] = { "some planks" },
    ["a crafting table"] = { "some planks" },
    ["a furnace"] = { "some stone" },
    ["some torches"] = { "some sticks", "some coal" },

    ["a wooden pickaxe"] = { "some planks", "some sticks" },
    ["a stone pickaxe"] = { "some stone", "some sticks" },
    ["an iron pickaxe"] = { "some iron", "some sticks" },
    ["a diamond pickaxe"] = { "some diamond", "some sticks" },

    ["a wooden sword"] = { "some planks", "some sticks" },
    ["a stone sword"] = { "some stone", "some sticks" },
    ["an iron sword"] = { "some iron", "some sticks" },
    ["a diamond sword"] = { "some diamond", "some sticks" },

    ["a wooden shovel"] = { "some planks", "some sticks" },
    ["a stone shovel"] = { "some stone", "some sticks" },
    ["an iron shovel"] = { "some iron", "some sticks" },
    ["a diamond shovel"] = { "some diamond", "some sticks" },
}

local tGoWest = {
    "(life is peaceful there)",
    "(lots of open air)",
    "(to begin life anew)",
    "(this is what we'll do)",
    "(sun in winter time)",
    "(we will do just fine)",
    "(where the skies are blue)",
    "(this and more we'll do)",
}
local nGoWest = 0

local bRunning = true
local tMap = { { {} } }
local x, y, z = 0, 0, 0
local inventory = {
    ["no tea"] = items["no tea"],
}

local nTurn = 0
local nTimeInRoom = 0
local bInjured = false

local tDayCycle = {
    "It is daytime.",
    "It is daytime.",
    "It is daytime.",
    "It is daytime.",
    "It is daytime.",
    "It is daytime.",
    "It is daytime.",
    "It is daytime.",
    "The sun is setting.",
    "It is night.",
    "It is night.",
    "It is night.",
    "It is night.",
    "It is night.",
    "The sun is rising.",
}

local function getTimeOfDay()
    return math.fmod(math.floor(nTurn / 3), #tDayCycle) + 1
end

local function isSunny()
    return getTimeOfDay() < 10
end

local function getRoom(x, y, z, dontCreate)
    tMap[x] = tMap[x] or {}
    tMap[x][y] = tMap[x][y] or {}
    if not tMap[x][y][z] and dontCreate ~= true then
         local room = {
             items = {},
             exits = {},
             nMonsters = 0,
         }
        tMap[x][y][z] = room

        if y == 0 then
            -- Room is above ground

            -- Pick biome
            room.nBiome = math.random(1, #tBiomes)
            room.trees = hasTrees(room.nBiome)

            -- Add animals
            if math.random(1, 3) == 1 then
                for _ = 1, math.random(1, 2) do
                    local sAnimal = tAnimals[math.random(1, #tAnimals)]
                    room.items[sAnimal] = items[sAnimal]
                end
            end

            -- Add surface ore
            if math.random(1, 5) == 1 or hasStone(room.nBiome) then
                room.items["some stone"] = items["some stone"]
            end
            if math.random(1, 8) == 1 then
                room.items["some coal"] = items["some coal"]
            end
            if math.random(1, 8) == 1 and hasRivers(room.nBiome) then
                room.items["a river"] = items["a river"]
            end

            -- Add exits
            room.exits = {
                ["north"] = true,
                ["south"] = true,
                ["east"] = true,
                ["west"] = true,
            }
            if math.random(1, 8) == 1 then
                room.exits.down = true
                room.items["a cave entrance"] = items["a cave entrance"]
            end

        else
            -- Room is underground
            -- Add exits
            local function tryExit(sDir, sOpp, x, y, z)
                local adj = getRoom(x, y, z, true)
                if adj then
                    if adj.exits[sOpp] then
                        room.exits[sDir] = true
                    end
                else
                    if math.random(1, 3) == 1 then
                        room.exits[sDir] = true
                    end
                end
            end

            if y == -1 then
                local above = getRoom(x, y + 1, z)
                if above.exits.down then
                    room.exits.up = true
                    room.items["an exit to the surface"] = items["an exit to the surface"]
                end
            else
                tryExit("up", "down", x, y + 1, z)
            end

            if y > -3 then
                tryExit("down", "up", x, y - 1, z)
            end

            tryExit("east", "west", x - 1, y, z)
            tryExit("west", "east", x + 1, y, z)
            tryExit("north", "south", x, y, z + 1)
            tryExit("south", "north", x, y, z - 1)

            -- Add ores
            room.items["some stone"] = items["some stone"]
            if math.random(1, 3) == 1 then
                room.items["some coal"] = items["some coal"]
            end
            if math.random(1, 8) == 1 then
                room.items["some iron"] = items["some iron"]
            end
            if y == -3 and math.random(1, 15) == 1 then
                room.items["some diamond"] = items["some diamond"]
            end

            -- Turn out the lights
            room.dark = true
        end
    end
    return tMap[x][y][z]
end

local function itemize(t)
    local item = next(t)
    if item == nil then
        return "nothing"
    end

    local text = ""
    while item do
        text = text .. item

        local nextItem = next(t, item)
        if nextItem ~= nil then
            local nextNextItem = next(t, nextItem)
            if nextNextItem == nil then
                text = text .. " and "
            else
                text = text .. ", "
            end
        end
        item = nextItem
    end
    return text
end

local function findItem(_tList, _sQuery)
    for sItem, tItem in pairs(_tList) do
        if sItem == _sQuery then
            return sItem
        end
        if tItem.aliases ~= nil then
            for _, sAlias in pairs(tItem.aliases) do
                if sAlias == _sQuery then
                    return sItem
                end
            end
        end
    end
    return nil
end

local tMatches = {
    ["wait"] = {
        "wait",
    },
    ["look"] = {
        "look at the ([%a ]+)",
        "look at ([%a ]+)",
        "look",
        "inspect ([%a ]+)",
        "inspect the ([%a ]+)",
        "inspect",
    },
    ["inventory"] = {
        "check self",
        "check inventory",
        "inventory",
        "i",
    },
    ["go"] = {
        "go (%a+)",
        "travel (%a+)",
        "walk (%a+)",
        "run (%a+)",
        "go",
    },
    ["dig"] = {
        "dig (%a+) using ([%a ]+)",
        "dig (%a+) with ([%a ]+)",
        "dig (%a+)",
        "dig",
    },
    ["take"] = {
        "pick up the ([%a ]+)",
        "pick up ([%a ]+)",
        "pickup ([%a ]+)",
        "take the ([%a ]+)",
        "take ([%a ]+)",
        "take",
    },
    ["drop"] = {
        "put down the ([%a ]+)",
        "put down ([%a ]+)",
        "drop the ([%a ]+)",
        "drop ([%a ]+)",
        "drop",
    },
    ["place"] = {
        "place the ([%a ]+)",
        "place ([%a ]+)",
        "place",
    },
    ["cbreak"] = {
        "punch the ([%a ]+)",
        "punch ([%a ]+)",
        "punch",
        "break the ([%a ]+) with the ([%a ]+)",
        "break ([%a ]+) with ([%a ]+) ",
        "break the ([%a ]+)",
        "break ([%a ]+)",
        "break",
    },
    ["mine"] = {
        "mine the ([%a ]+) with the ([%a ]+)",
        "mine ([%a ]+) with ([%a ]+)",
        "mine ([%a ]+)",
        "mine",
    },
    ["attack"] = {
        "attack the ([%a ]+) with the ([%a ]+)",
        "attack ([%a ]+) with ([%a ]+)",
        "attack ([%a ]+)",
        "attack",
        "kill the ([%a ]+) with the ([%a ]+)",
        "kill ([%a ]+) with ([%a ]+)",
        "kill ([%a ]+)",
        "kill",
        "hit the ([%a ]+) with the ([%a ]+)",
        "hit ([%a ]+) with ([%a ]+)",
        "hit ([%a ]+)",
        "hit",
    },
    ["craft"] = {
        "craft a ([%a ]+)",
        "craft some ([%a ]+)",
        "craft ([%a ]+)",
        "craft",
        "make a ([%a ]+)",
        "make some ([%a ]+)",
        "make ([%a ]+)",
        "make",
    },
    ["build"] = {
        "build ([%a ]+) out of ([%a ]+)",
        "build ([%a ]+) from ([%a ]+)",
        "build ([%a ]+)",
        "build",
    },
    ["eat"] = {
        "eat a ([%a ]+)",
        "eat the ([%a ]+)",
        "eat ([%a ]+)",
        "eat",
    },
    ["help"] = {
        "help me",
        "help",
    },
    ["exit"] = {
        "exit",
        "quit",
        "goodbye",
        "good bye",
        "bye",
        "farewell",
    },
}

local commands = {}
local function doCommand(text)
    if text == "" then
        commands.noinput()
        return
    end

    for sCommand, t in pairs(tMatches) do
        for _, sMatch in pairs(t) do
            local tCaptures = { string.match(text, "^" .. sMatch .. "$") }
            if #tCaptures ~= 0 then
                local fnCommand = commands[sCommand]
                if #tCaptures == 1 and tCaptures[1] == sMatch then
                    fnCommand()
                else
                    fnCommand(table.unpack(tCaptures))
                end
                return
            end
        end
    end
    commands.badinput()
end

function commands.wait()
    print("Time passes...")
end

function commands.look(_sTarget)
    local room = getRoom(x, y, z)
    if room.dark then
        print("It is pitch dark.")
        return
    end

    if _sTarget == nil then
        -- Look at the world
        if y == 0 then
            io.write("You are standing " .. tBiomes[room.nBiome] .. ". ")
            print(tDayCycle[getTimeOfDay()])
        else
            io.write("You are underground. ")
            if next(room.exits) ~= nil then
                print("You can travel " .. itemize(room.exits) .. ".")
            else
                print()
            end
        end
        if next(room.items) ~= nil then
            print("There is " .. itemize(room.items) .. " here.")
        end
        if room.trees then
            print("There are trees here.")
        end

    else
        -- Look at stuff
        if room.trees and (_sTarget == "tree" or _sTarget == "trees") then
            print("The trees look easy to break.")
        elseif _sTarget == "self" or _sTarget == "myself" then
            print("Very handsome.")
        else
            local tItem = nil
            local sItem = findItem(room.items, _sTarget)
            if sItem then
                tItem = room.items[sItem]
            else
                sItem = findItem(inventory, _sTarget)
                if sItem then
                    tItem = inventory[sItem]
                end
            end

            if tItem then
                print(tItem.desc or "You see nothing special about " .. sItem .. ".")
            else
                print("You don't see any " .. _sTarget .. " here.")
            end
        end
    end
end

function commands.go(_sDir)
    local room = getRoom(x, y, z)
    if _sDir == nil then
        print("Go where?")
        return
    end

    if nGoWest ~= nil then
        if _sDir == "west" then
            nGoWest = nGoWest + 1
            if nGoWest > #tGoWest then
                nGoWest = 1
            end
            print(tGoWest[nGoWest])
        else
            if nGoWest > 0 or nTurn > 6 then
                nGoWest = nil
            end
        end
    end

    if room.exits[_sDir] == nil then
        print("You can't go that way.")
        return
    end

    if _sDir == "north" then
        z = z + 1
    elseif _sDir == "south" then
        z = z - 1
    elseif _sDir == "east" then
        x = x - 1
    elseif _sDir == "west" then
        x = x + 1
    elseif _sDir == "up" then
        y = y + 1
    elseif _sDir == "down" then
        y = y - 1
    else
        print("I don't understand that direction.")
        return
    end

    nTimeInRoom = 0
    doCommand("look")
end

function commands.dig(_sDir, _sTool)
    local room = getRoom(x, y, z)
    if _sDir == nil then
        print("Dig where?")
        return
    end

    local sTool = nil
    local tTool = nil
    if _sTool ~= nil then
        sTool = findItem(inventory, _sTool)
        if not sTool then
            print("You're not carrying a " .. _sTool .. ".")
            return
        end
        tTool = inventory[sTool]
    end

    local bActuallyDigging = room.exits[_sDir] ~= true
    if bActuallyDigging then
        if sTool == nil or tTool.toolType ~= "pick" then
            print("You need to use a pickaxe to dig through stone.")
            return
        end
    end

    if _sDir == "north" then
        room.exits.north = true
        z = z + 1
        getRoom(x, y, z).exits.south = true

    elseif _sDir == "south" then
        room.exits.south = true
        z = z - 1
        getRoom(x, y, z).exits.north = true

    elseif _sDir == "east" then
        room.exits.east = true
        x = x - 1
        getRoom(x, y, z).exits.west = true

    elseif _sDir == "west" then
        room.exits.west = true
        x = x + 1
        getRoom(x, y, z).exits.east = true

    elseif _sDir == "up" then
        if y == 0 then
            print("You can't dig that way.")
            return
        end

        room.exits.up = true
        if y == -1 then
            room.items["an exit to the surface"] = items["an exit to the surface"]
        end
        y = y + 1

        room = getRoom(x, y, z)
        room.exits.down = true
        if y == 0 then
            room.items["a cave entrance"] = items["a cave entrance"]
        end

    elseif _sDir == "down" then
        if y <= -3 then
            print("You hit bedrock.")
            return
        end

        room.exits.down = true
        if y == 0 then
            room.items["a cave entrance"] = items["a cave entrance"]
        end
        y = y - 1

        room = getRoom(x, y, z)
        room.exits.up = true
        if y == -1 then
            room.items["an exit to the surface"] = items["an exit to the surface"]
        end

    else
        print("I don't understand that direction.")
        return
    end

    --
    if bActuallyDigging then
        if _sDir == "down" and y == -1 or
           _sDir == "up" and y == 0 then
            inventory["some dirt"] = items["some dirt"]
            inventory["some stone"] = items["some stone"]
            print("You dig " .. _sDir .. " using " .. sTool .. " and collect some dirt and stone.")
        else
            inventory["some stone"] = items["some stone"]
            print("You dig " .. _sDir .. " using " .. sTool .. " and collect some stone.")
        end
    end

    nTimeInRoom = 0
    doCommand("look")
end

function commands.inventory()
    print("You are carrying " .. itemize(inventory) .. ".")
end

function commands.drop(_sItem)
    if _sItem == nil then
        print("Drop what?")
        return
    end

    local room = getRoom(x, y, z)
    local sItem = findItem(inventory, _sItem)
    if sItem then
        local tItem = inventory[sItem]
        if tItem.droppable == false then
            print("You can't drop that.")
        else
            room.items[sItem] = tItem
            inventory[sItem] = nil
            print("Dropped.")
        end
    else
        print("You don't have a " .. _sItem .. ".")
    end
end

function commands.place(_sItem)
    if _sItem == nil then
        print("Place what?")
        return
    end

    if _sItem == "torch" or _sItem == "a torch" then
        local room = getRoom(x, y, z)
        if inventory["some torches"] or inventory["a torch"] then
            inventory["a torch"] = nil
            room.items["a torch"] = items["a torch"]
            if room.dark then
                print("The cave lights up under the torchflame.")
                room.dark = false
            elseif y == 0 and not isSunny() then
                print("The night gets a little brighter.")
            else
                print("Placed.")
            end
        else
            print("You don't have torches.")
        end
        return
    end

    commands.drop(_sItem)
end

function commands.take(_sItem)
    if _sItem == nil then
        print("Take what?")
        return
    end

    local room = getRoom(x, y, z)
    local sItem = findItem(room.items, _sItem)
    if sItem then
        local tItem = room.items[sItem]
        if tItem.heavy == true then
            print("You can't carry " .. sItem .. ".")
        elseif tItem.ore == true then
            print("You need to mine this ore.")
        else
            if tItem.infinite ~= true then
                room.items[sItem] = nil
            end
            inventory[sItem] = tItem

            if inventory["some torches"] and inventory["a torch"] then
                inventory["a torch"] = nil
            end
            if sItem == "a torch" and y < 0 then
                room.dark = true
                print("The cave plunges into darkness.")
            else
                print("Taken.")
            end
        end
    else
        print("You don't see a " .. _sItem .. " here.")
    end
end

function commands.mine(_sItem, _sTool)
    if _sItem == nil then
        print("Mine what?")
        return
    end
    if _sTool == nil then
        print("Mine " .. _sItem .. " with what?")
        return
    end
    commands.cbreak(_sItem, _sTool)
end

function commands.attack(_sItem, _sTool)
    if _sItem == nil then
        print("Attack what?")
        return
    end
    commands.cbreak(_sItem, _sTool)
end

function commands.cbreak(_sItem, _sTool)
    if _sItem == nil then
        print("Break what?")
        return
    end

    local sTool = nil
    if _sTool ~= nil then
        sTool = findItem(inventory, _sTool)
        if sTool == nil then
            print("You're not carrying a " .. _sTool .. ".")
            return
        end
    end

    local room = getRoom(x, y, z)
    if _sItem == "tree" or _sItem == "trees" or _sItem == "a tree" then
        print("The tree breaks into blocks of wood, which you pick up.")
        inventory["some wood"] = items["some wood"]
        return
    elseif _sItem == "self" or _sItem == "myself" then
        if term.isColour() then
            term.setTextColour(colours.red)
        end
        print("You have died.")
        print("Score: &e0")
        term.setTextColour(colours.white)
        bRunning = false
        return
    end

    local sItem = findItem(room.items, _sItem)
    if sItem then
        local tItem = room.items[sItem]
        if tItem.ore == true then
            -- Breaking ore
            if not sTool then
                print("You need a tool to break this ore.")
                return
            end
            local tTool = inventory[sTool]
            if tTool.tool then
                if tTool.toolLevel < tItem.toolLevel then
                    print(sTool .. " is not strong enough to break this ore.")
                elseif tTool.toolType ~= tItem.toolType then
                    print("You need a different kind of tool to break this ore.")
                else
                    print("The ore breaks, dropping " .. sItem .. ", which you pick up.")
                    inventory[sItem] = items[sItem]
                    if tItem.infinite ~= true then
                        room.items[sItem] = nil
                    end
                end
            else
                print("You can't break " .. sItem .. " with " .. sTool .. ".")
            end

        elseif tItem.creature == true then
            -- Fighting monsters (or pigs)
            local toolLevel = 0
            local tTool = nil
            if sTool then
                tTool = inventory[sTool]
                if tTool.toolType == "sword" then
                    toolLevel = tTool.toolLevel
                end
            end

            local tChances = { 0.2, 0.4, 0.55, 0.8, 1 }
            if math.random() <= tChances[toolLevel + 1] then
                room.items[sItem] = nil
                print("The " .. tItem.aliases[1] .. " dies.")

                if tItem.drops then
                    for _, sDrop in pairs(tItem.drops) do
                        if not room.items[sDrop] then
                            print("The " .. tItem.aliases[1] .. " dropped " .. sDrop .. ".")
                            room.items[sDrop] = items[sDrop]
                        end
                    end
                end

                if tItem.monster then
                    room.nMonsters = room.nMonsters - 1
                end
            else
                print("The " .. tItem.aliases[1] .. " is injured by your blow.")
            end

            if tItem.hitDrops then
                for _, sDrop in pairs(tItem.hitDrops) do
                    if not room.items[sDrop] then
                        print("The " .. tItem.aliases[1] .. " dropped " .. sDrop .. ".")
                        room.items[sDrop] = items[sDrop]
                    end
                end
            end

        else
            print("You can't break " .. sItem .. ".")
        end
    else
        print("You don't see a " .. _sItem .. " here.")
    end
end

function commands.craft(_sItem)
    if _sItem == nil then
        print("Craft what?")
        return
    end

    if _sItem == "computer" or _sItem == "a computer" then
        print("By creating a computer in a computer in a computer, you tear a hole in the spacetime continuum from which no mortal being can escape.")
        if term.isColour() then
            term.setTextColour(colours.red)
        end
        print("You have died.")
        print("Score: &e0")
        term.setTextColour(colours.white)
        bRunning = false
        return
    end

    local sItem = findItem(items, _sItem)
    local tRecipe = sItem and tRecipes[sItem] or nil
    if tRecipe then
        for _, sReq in ipairs(tRecipe) do
            if inventory[sReq] == nil then
                print("You don't have the items you need to craft " .. sItem .. ".")
                return
            end
        end

        for _, sReq in ipairs(tRecipe) do
            inventory[sReq] = nil
        end
        inventory[sItem] = items[sItem]
        if inventory["some torches"] and inventory["a torch"] then
            inventory["a torch"] = nil
        end
        print("Crafted.")
    else
        print("You don't know how to make " .. (sItem or _sItem) .. ".")
    end
end

function commands.build(_sThing, _sMaterial)
    if _sThing == nil then
        print("Build what?")
        return
    end

    local sMaterial = nil
    if _sMaterial == nil then
        for sItem, tItem in pairs(inventory) do
            if tItem.material then
                sMaterial = sItem
                break
            end
        end
        if sMaterial == nil then
            print("You don't have any building materials.")
            return
        end
    else
        sMaterial = findItem(inventory, _sMaterial)
        if not sMaterial then
            print("You don't have any " .. _sMaterial)
            return
        end

        if inventory[sMaterial].material ~= true then
            print(sMaterial .. " is not a good building material.")
            return
        end
    end

    local alias = nil
    if string.sub(_sThing, 1, 1) == "a" then
        alias = string.match(_sThing, "a ([%a ]+)")
    end

    local room = getRoom(x, y, z)
    inventory[sMaterial] = nil
    room.items[_sThing] = {
        heavy = true,
        aliases = { alias },
        desc = "As you look at your creation (made from " .. sMaterial .. "), you feel a swelling sense of pride.",
    }

    print("Your construction is complete.")
end

function commands.help()
    local sText =
        "Welcome to adventure, the greatest text adventure game on CraftOS. " ..
        "To get around the world, type actions, and the adventure will " ..
        "be read back to you. The actions available to you are go, look, inspect, inventory, " ..
        "take, drop, place, punch, attack, mine, dig, craft, build, eat and exit."
    print(sText)
end

function commands.eat(_sItem)
    if _sItem == nil then
        print("Eat what?")
        return
    end

    local sItem = findItem(inventory, _sItem)
    if not sItem then
        print("You don't have any " .. _sItem .. ".")
        return
    end

    local tItem = inventory[sItem]
    if tItem.food then
        print("That was delicious!")
        inventory[sItem] = nil

        if bInjured then
            print("You are no longer injured.")
            bInjured = false
        end
    else
        print("You can't eat " .. sItem .. ".")
    end
end

function commands.exit()
    bRunning = false
end

function commands.badinput()
    local tResponses = {
        "I don't understand.",
        "I don't understand you.",
        "You can't do that.",
        "Nope.",
        "Huh?",
        "Say again?",
        "That's crazy talk.",
        "Speak clearly.",
        "I'll think about it.",
        "Let me get back to you on that one.",
        "That doesn't make any sense.",
        "What?",
    }
    print(tResponses[math.random(1, #tResponses)])
end

function commands.noinput()
    local tResponses = {
        "Speak up.",
        "Enunciate.",
        "Project your voice.",
        "Don't be shy.",
        "Use your words.",
    }
    print(tResponses[math.random(1, #tResponses)])
end

local function simulate()
    local bNewMonstersThisRoom = false

    -- Spawn monsters in nearby rooms
    for sx = -2, 2 do
        for sy = -1, 1 do
            for sz = -2, 2 do
                local h = y + sy
                if h >= -3 and h <= 0 then
                    local room = getRoom(x + sx, h, z + sz)

                    -- Spawn monsters
                    if room.nMonsters < 2 and
                       (h == 0 and not isSunny() and not room.items["a torch"] or room.dark) and
                       math.random(1, 6) == 1 then

                        local sMonster = tMonsters[math.random(1, #tMonsters)]
                        if room.items[sMonster] == nil then
                               room.items[sMonster] = items[sMonster]
                               room.nMonsters = room.nMonsters + 1

                               if sx == 0 and sy == 0 and sz == 0 and not room.dark then
                                   print("From the shadows, " .. sMonster .. " appears.")
                                   bNewMonstersThisRoom = true
                               end
                        end
                    end

                    -- Burn monsters
                    if h == 0 and isSunny() then
                        for _, sMonster in ipairs(tMonsters) do
                            if room.items[sMonster] and items[sMonster].nocturnal then
                                room.items[sMonster] = nil
                                   if sx == 0 and sy == 0 and sz == 0 and not room.dark then
                                       print("With the sun high in the sky, the " .. items[sMonster].aliases[1] .. " bursts into flame and dies.")
                                   end
                                   room.nMonsters = room.nMonsters - 1
                               end
                        end
                    end
                end
            end
        end
    end

    -- Make monsters attack
    local room = getRoom(x, y, z)
    if nTimeInRoom >= 2 and not bNewMonstersThisRoom then
        for _, sMonster in ipairs(tMonsters) do
            if room.items[sMonster] then
                if math.random(1, 4) == 1 and
                   not (y == 0 and isSunny() and sMonster == "a spider") then
                    if sMonster == "a creeper" then
                        if room.dark then
                            print("A creeper explodes.")
                        else
                            print("The creeper explodes.")
                        end
                        room.items[sMonster] = nil
                        room.nMonsters = room.nMonsters - 1
                    else
                        if room.dark then
                            print("A " .. items[sMonster].aliases[1] .. " attacks you.")
                        else
                            print("The " .. items[sMonster].aliases[1] .. " attacks you.")
                        end
                    end

                    if bInjured then
                        if term.isColour() then
                            term.setTextColour(colours.red)
                        end
                        print("You have died.")
                        print("Score: &e0")
                        term.setTextColour(colours.white)
                        bRunning = false
                        return
                    else
                        bInjured = true
                    end

                    break
                end
            end
        end
    end

    -- Always print this
    if bInjured then
        if term.isColour() then
            term.setTextColour(colours.red)
        end
        print("You are injured.")
        term.setTextColour(colours.white)
    end

    -- Advance time
    nTurn = nTurn + 1
    nTimeInRoom = nTimeInRoom + 1
end

doCommand("look")
simulate()

local tCommandHistory = {}
while bRunning do
    if term.isColour() then
        term.setTextColour(colours.yellow)
    end
    write("? ")
    term.setTextColour(colours.white)

    local sRawLine = read(nil, tCommandHistory)
    table.insert(tCommandHistory, sRawLine)

    local sLine = nil
    for match in string.gmatch(sRawLine, "%a+") do
        if sLine then
            sLine = sLine .. " " .. string.lower(match)
        else
            sLine = string.lower(match)
        end
    end

    doCommand(sLine or "")
    if bRunning then
        simulate()
    end
end
