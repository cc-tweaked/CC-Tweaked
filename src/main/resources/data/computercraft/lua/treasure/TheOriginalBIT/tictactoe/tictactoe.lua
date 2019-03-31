--[[
Author: TheOriginalBIT
Version: 1.1.2
Created: 26 APR 2013
Last Update: 30 APR 2013

License:

COPYRIGHT NOTICE
Copyright © 2013 Joshua Asbury a.k.a TheOriginalBIT [theoriginalbit@gmail.com]

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
associated documentation files (the "Software"), to deal in the Software without restriction,
including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
copies of the Software, and to permit persons to whom the Software is furnished to do so,
subject to the following conditions:

-The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
-Visible credit is given to the original author.
-The software is distributed in a non-profit way.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
]]--

-- make sure that its only a computer terminal that is displaying
local sw, sh = term.getSize()

if sw ~= 51 and sh ~= 19 then
  error("Sorry this game can only run on computers", 0)
end

-- the wining directions
local winCombos = {
  -- horizontal
  {1,2,3}, {4,5,6}, {7,8,9},
  -- vertical
  {1,4,7}, {2,5,8}, {3,6,9},
  -- diagonal
  {1,5,9}, {3,5,7}
}

local players = {x = 'Player', o = 'The Computer'}
-- whether an AI is active, could be used later to allow SP
local activeAI = true
local currentPlayer
local opposites = { x = 'o', o = 'x' }
local board
local winner
local move
local allowedBgColors = { colors.orange, colors.lightBlue, colors.gray, colors.cyan, colors.purple, colors.blue, colors.brown, colors.green, colors.red, colors.black }
local bg

local function clear(col)
  term.setBackgroundColor(col or colors.black)
  term.clear()
  term.setCursorPos(1,1)
end

-- function thanks to Mads... found here: http://www.computercraft.info/forums2/index.php?/topic/11771-print-coloured-text-easily/page__p__105389#entry105389
local function writeWithFormat(...)
  local s = "&0"
  for k, v in ipairs(arg) do
    s = s .. v
  end
  s = s .. "&0"
  local fields = {}
  local lastcolor, lastpos = "0", 0
  for pos, clr in s:gmatch"()&(%x)" do
    table.insert(fields, {s:sub(lastpos + 2, pos - 1), lastcolor})
    lastcolor, lastpos = clr , pos
  end
  for i = 2, #fields do
    term.setTextColor(2 ^ (tonumber(fields[i][2], 16)))
    write(fields[i][1])
  end
end

-- modification of Mads' function to get the length of the string without the color modifiers
local function countFormatters(text)
  return #(text:gsub("()&(%x)", ''))
end

-- print a color formatted string in the center of the screen
local function cwriteWithFormat(text, y)
  local sw,sh = term.getSize()
  local _,cy = term.getCursorPos()
  term.setCursorPos(math.floor((sw-countFormatters(text))/2)+(countFormatters(text) % 2 == 0 and 1 or 0), y or cy)
  writeWithFormat(text)
end

-- writes the text at the give location
local function writeAt(text, x, y)
  local _,cy = term.getCursorPos()
  term.setCursorPos(x or 1, y or cy)
  write(text)
end

local function reset()
  bg = allowedBgColors[math.random(1, #allowedBgColors)]
  currentPlayer = 'x'
  board = {}
  for i = 1, 9 do
    board[i] = ' '
  end
  winner = nil
  move = nil
end

local function search(match)
  for _, check in ipairs(winCombos) do
    if board[check[1]] == board[check[2]] and board[check[1]] == match and board[check[3]] == ' ' then
      return check[3]
    elseif board[check[1]] == board[check[3]] and board[check[1]] == match and board[check[2]] == ' ' then
      return check[2]
    elseif board[check[2]] == board[check[3]] and board[check[2]] == match and board[check[1]] == ' ' then
      return check[1]
    end
  end
end

local function getAIMove()
  -- make it seem like the computer actually has to think about its move
  sleep(0.8)
  
  -- check if AI can win and return the 3rd tile to create a win, if it cannot, check for a human attempt at winning and stop it, if there is none, return a random
  return (search(currentPlayer) or search(opposites[currentPlayer])) or math.random(1,9)
end

local function modread( _mask, _history, _limit )
  term.setCursorBlink(true)

  local input = ""
  local pos = 0
  if _mask then
    _mask = _mask:sub(1,1)
  end
  local historyPos = nil

  local sw, sh = term.getSize()
  local sx, sy = term.getCursorPos()

  local function redraw( _special )
    local scroll = (sx + pos >= sw and (sx + pos) - sw or 0)
    local replace = _special or _mask
    term.setCursorPos( sx, sy )
    term.write( replace and string.rep(replace, #input - scroll) or input:sub(scroll + 1) )
    term.setCursorPos( sx + pos - scroll, sy )
  end

  while true do
    local event = {os.pullEvent()}
    if event[1] == 'char' and (not _limit or #input < _limit) then
      input = input:sub(1, pos)..event[2]..input:sub(pos + 1)
      pos = pos + 1
    elseif event[1] == 'key' then
      if event[2] == keys.enter then
        break
      elseif event[2] == keys.backspace and pos > 0 then
        redraw(' ')
        input = input:sub(1, pos - 1)..input:sub(pos + 1)
        pos = pos - 1
      elseif event[2] == keys.delete and pos < #input then
        redraw(' ')
        input = input:sub(1, pos)..input:sub(pos + 2)
      elseif event[2] == keys.home then
        pos = 0
      elseif event[2] == keys['end'] then
        pos = #input
      elseif event[2] == keys.left and pos > 0 then
        pos = pos - 1
      elseif event[2] == keys.right and pos < #input then
        pos = pos + 1
      elseif _history and event[2] == keys.up or event[2] == keys.down then
        redraw(' ')
        if event[2] == keys.up then
          if not historyPos then
            historyPos = #_history 
          elseif historyPos > 1 then
            historyPos = historyPos - 1
          end
        else
          if historyPos ~= nil and historyPos < #_history then
            historyPos = historyPos + 1
          elseif historyPos == #_history then
            historyPos = nil
          end
        end

        if historyPos then
          input = string.sub(_history[historyPos], 1, _limit) or ""
          pos = #input
        else
          input = ""
          pos = 0
        end
      end
    elseif event[1] == 'mouse_click' then
      local xPos, yPos = event[3], event[4]
      if xPos == sw and yPos == 1 then
        -- exit and make sure to fool the catch-all
        error('Terminated', 0)
      end
      local row = (xPos >= 16 and xPos <= 21) and 1 or (xPos >= 23 and xPos <= 28) and 2 or (xPos >= 30 and xPos <= 35) and 3 or 10
      local col = (yPos >= 4 and yPos <= 6) and 1 or (yPos >= 8 and yPos <= 10) and 2 or (yPos >= 12 and yPos <= 16) and 3 or 10
      local ret = (col - 1) * 3 + row
      if ret >= 1 and ret <= 9 then
        return ret
      end
    end

    redraw(_mask)
  end

  term.setCursorBlink(false)
  term.setCursorPos(1, sy + 1)

  return input
end

local function getHumanMove()
  writeWithFormat('&b[1-9] >>&f ')
  return modread()
end

local function processInput()
  -- set the cursor pos ready for the input
  term.setCursorPos(3, sh-1)
  move = (currentPlayer == 'x' and getHumanMove or getAIMove)()
end

local function output(msg)
  -- if the player is not an AI, print the error
  if not (activeAI and currentPlayer == 'o') then
    term.setCursorPos(3, sh-1)
    writeWithFormat('&eERROR >> '..msg)
    sleep(2)
  end
end

local function checkMove()
  -- if the user typed exit
  if not tonumber(move) and move:lower() == 'exit' then
    -- exit and make sure to fool the catch-all
    error('Terminated', 0)
  end

  -- attempt to convert the move to a number
  local nmove = tonumber(move)
  -- if it wasn't a number
  if not nmove then
    output(tostring(move)..' is not a number between 1 and 9!')
    return false
  end
  -- if it is not within range of the board
  if nmove > 9 or nmove < 1 then
    output('Must be a number between 1 and 9!')
    return false
  end
  -- if the space is already taken
  if board[nmove] ~= ' ' then
    output('Position already taken!')
    return false
  end
  -- keep the conversion
  move = tonumber(move)
  return true
end

local function checkWin()
  for _, check in ipairs(winCombos) do
    if board[check[1]] ~= ' ' and board[check[1]] == board[check[2]] and board[check[1]] == board[check[3]] then
      return board[check[1]]
    end
  end

  for _, tile in ipairs(board) do
    if tile == ' ' then
      return nil
    end
  end

  return 'tie'
end

local function update()
  if checkMove() then
    board[move] = currentPlayer
    winner = checkWin()

    currentPlayer = currentPlayer == 'x' and 'o' or 'x'
  end
end

local function render()
  -- clear the screen light blue
  clear(bg)

  -- draw the ascii borders
  term.setTextColor(colors.white)
  for i = 2, sh-1 do
    writeAt('|', 1, i)
    writeAt('|', sw, i)
  end
  writeAt('+'..string.rep('-', sw-2)..'+', 1, 1)
  writeAt('+'..string.rep('-', sw-2)..'+', 1, 3)
  writeAt('+'..string.rep('-', sw-2)..'+', 1, sh-2)
  writeAt('+'..string.rep('-', sw-2)..'+', 1, sh)
  
  if term.isColor and term.isColor() then
    term.setCursorPos(sw, 1)
    term.setBackgroundColor(colors.red)
    term.setTextColor(colors.black)
    writeWithFormat('X')
  end
  
  -- set our colours
  term.setBackgroundColor(colors.white)
  term.setTextColor(colors.black)

  -- clear an area for the title
  writeAt(string.rep(' ', sw-2), 2, 2)
  writeAt('Tic-Tac-Toe!', sw/2-5, 2)

  -- clear an area for the input
  writeAt(string.rep(' ', sw-2), 2, sh-1)

  -- clear the area for the board
  local h = sh - 6
  for i = 0, h - 1 do
    writeAt(string.rep(' ', sw - 2), 2, 4+i)
  end

  -- draw the grid
  for i = 0, 10 do
    writeAt(((i == 3 or i == 7) and '------+------+------' or '      |      |      '), 16, i + 4)
  end

  -- draw the first line moves
  for i = 1, 3 do
    if board[i] ~= ' ' then
      writeAt((board[i] == 'x' and '\\/' or '/\\'), 18+((i-1)*7), 5)
      writeAt((board[i] == 'x' and '/\\' or '\\/'), 18+((i-1)*7), 6)
    end
  end
  -- draw the second line moves
  for i = 1, 3 do
    if board[i + 3] ~= ' ' then
      writeAt((board[i + 3] == 'x' and '\\/' or '/\\'), 18+((i-1)*7), 9)
      writeAt((board[i + 3] == 'x' and '/\\' or '\\/'), 18+((i-1)*7), 10)
    end
  end
  -- draw the third line moves
  for i = 1, 3 do
    if board[i + 6] ~= ' ' then
      writeAt((board[i + 6] == 'x' and '\\/' or '/\\'), 18+((i-1)*7), 13)
      writeAt((board[i + 6] == 'x' and '/\\' or '\\/'), 18+((i-1)*7), 14)
    end
  end
  
  -- draw the current player
  term.setCursorPos(3, sh - 3)
  if not winner then
    writeWithFormat('&bCurrent Player: &f'..players[currentPlayer])
  end
end

local function main(arc, argv)
  clear()
  writeWithFormat('&0Welcome to CCTicTacToe by &8TheOriginal&3BIT&0\n\nPlease enter your name\n\n&4>>&0 ')
  players.x = read() or 'Player'

  -- setup the game, will later be used to
  reset()

  -- initial render
  render()

  -- game loop
  while not winner do
    processInput()
    update()
    render()

    -- highly unorthodox having something that isn't in input, update, render!
    -- print the winner info
    if winner then
      writeWithFormat('&f'..(winner == 'tie' and 'There was no winner :(&f' or players[winner]..'&f is the winner!'))
      -- allow the player to start a new game or quit
      writeAt("Press 'R' to play again, 'Q' to quit...", 3, sh - 1)
      while true do
        local _, k = os.pullEvent('key')
        if k == 16 then
          break
        elseif k == 19 then
          reset() -- reset the game
          render() -- render the new game ready to wait for input
          break
        end
      end
      os.pullEvent() -- remove the char event that would be waiting
    end
  end

  return true
end

-- create a terminal object with a non-advanced computer safe version of setting colors
local oldTermObj = term.current()
local termObj = {
  setTextColor = function(n) if term.isColor and term.isColor() then local ok, err = pcall(oldTermObj.setTextColor , n) if not ok then error(err, 2) end end end,
  setBackgroundColor = function(n) if term.isColor and term.isColor() then local ok, err = pcall(oldTermObj.setBackgroundColor , n) if not ok then error(err, 2) end end end
}
-- also override the English spelling of the colour functions
termObj.setTextColour = termObj.setTextColor
termObj.setBackgroundColour = termObj.setBackgroundColor

-- make the terminal object refer to the native terminal for every other function
termObj.__index = oldTermObj
setmetatable(termObj, termObj)

-- redirect the terminal to the new object
term.redirect(termObj)

-- run the program
local ok, err = pcall(main, #{...}, {...})

-- catch-all
if not ok and err ~= 'Terminated' then
  clear()
  print('Error in runtime!')
  print(err)
  sleep(5)
end

-- print thank you message
clear()
cwriteWithFormat('&4Thank you for playing CCTicTacToe v1.0', 1)
cwriteWithFormat('&4By &8TheOriginal&3BIT\n', 2)

-- restore the default terminal object
term.redirect( oldTermObj )