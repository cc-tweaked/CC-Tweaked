--[[
battleship,
 
by GopherAtl, 2013
 
Do whatever you want, just don't judge me by
what a mess this code is.
--]]
local args={...}
local action=args[1]
local opponentID=nil
local openedSide=nil
local opponent=nil
local myName=""
local opponentReady=false
local myTurn
local targetX,targetY
local shipsLeft=5
local oppShipsLeft=5
 
local originalTerm = term.current()
 
--bounding box of the target grid
local targetGridBounds={
    minX=16, maxX=25,
    minY=4, maxY=13
  }
 
 
local function doColor(text,background)
  term.setTextColor(text)
  term.setBackgroundColor(background)
end
 
local function doColor_mono(text,background)
  if text==colors.blue or text==colors.red or text==colors.black or text==colors.lime or background==colors.lightGray then
    term.setTextColor(colors.black)
    term.setBackgroundColor(colors.white)
  else
    term.setTextColor(colors.white)
    term.setBackgroundColor(colors.black)
  end  
end
 
local function doScreenColor()
  if term.isColor() then
    doColor(colors.white,colors.lightGray)
  else
    doColor(colors.black,colors.white)
  end
end
 
local function toGridRef(x,y)
  return string.sub("ABCDEFGHIJ",x,x)..string.sub("1234567890",y,y)
end
 
 
if not term.isColor() then
  doColor=doColor_mono
end
 
local function quit()
  if openedSide then
    rednet.close(openedSide)
  end
  term.redirect( originalTerm )
  term.setCursorPos(term.getSize())
  print()
  error()
end
 
local foundModem=false
--find modem
for k,v in pairs(redstone.getSides()) do
  if peripheral.getType(v)=="modem" then
    foundModem=true
    if not rednet.isOpen(v) then
      rednet.open(v)
      openedSide=v
    end
    break
  end
end
 
if not foundModem then
  print("You must have a modem to play!")
  return
end
 
if action==nil or (action~="join" and action~="host") then
  print("Invalid parameters. Usage:\n> battleship host\nHosts a game, waits for another computer to join\n> battleship join\nLooks for another game to join")
  quit()
end
 
--get player name
while true do
  doColor(colors.cyan,colors.black)
  write("player name: ")
  doColor(colors.gray,colors.black)
  myName=read()
  if myName=="" then
    doColor(colors.red,colors.black)
    print("You have to give a name!")
  elseif #myName>11 then
    doColor(colors.red,colors.black)
    print("Max name is 11 characters!")
  else
    break
  end
end
 
if action=="join" then
  print("Attempting to join a game...\n(press q to cancel)")
  while true do
    local retryTimer=os.startTimer(1);
    rednet.broadcast("bs join "..myName);
 
    while true do
      local event,p1,p2,p3=os.pullEvent();
      if event=="rednet_message" then
        opponent=string.match(p2,"bs accept %s*(.+)%s*")
        if opponent then
          opponentID=p1
          break
        end
      elseif event=="timer" and p1==retryTimer then
        break
      elseif event=="char" and (p1=="q" or p1=="Q") then
        print("Couldn't find an opponent; quitting")
        quit()
      end
    end
    local joined=false
   
    if opponentID then
      print("Joining game!")
      rednet.send(opponentID,"bs start")
      break
    end
  end
elseif action=="host" then
  print("Waiting for challenger...\n(Press q to cancel)")
  while true do
    while true do
      local event,p1,p2=os.pullEvent()  
      if event=="rednet_message" then
        opponent=string.match(p2,"bs join %s*(.+)%s*")        if opponent then
          print("found player, inviting..")
          opponentID=p1
          break
        end
      elseif event=="char" and (p1=="q" or p1=="Q") then
        print("Couldn't find opponent, quitting")
        quit()
      end
    end
   
    if opponentID then
      rednet.send(opponentID,"bs accept "..myName)
      local timeout=os.startTimer(1)
      while true do
        local event,p1,p2=os.pullEvent()
        if event=="rednet_message" and p2=="bs start" then
          print("player joined!")
          break
        elseif event=="timer" and p1==timeout then
          print("player joined another game. Waiting for another...")
          opponentID=nil
          break    
        end
      end
     
      if opponentID then
        break
      end
    end
  end
end
 
local ships={
  {pos=nil,dir="h",size=5,name="carrier",hits=0},
  {pos=nil,dir="h",size=4,name="battleship",hits=0},
  {pos=nil,dir="h",size=3,name="cruiser",hits=0},
  {pos=nil,dir="h",size=3,name="submarine",hits=0},
  {pos=nil,dir="h",size=2,name="destroyer",hits=0},
}
 
local myShotTable={ {1,1,true},{5,5,false} }
local oppShotTable={ }
 
local myGrid,oppGrid={title=myName},{title=opponent}
 
--setup grids
for i=1,10 do
  myGrid[i]={}
  oppGrid[i]={}
  for j=1,10 do
    myGrid[i][j]={hit=false,ship=false}
    oppGrid[i][j]={hit=false,ship=false}
  end
end
 
local function drawShipsToGrid(ships,grid)  
  for i=1,#ships do
    local x,y=table.unpack(ships[i].pos)
    local stepX=ships[i].dir=="h" and 1 or 0
    local stepY=stepX==1 and 0 or 1
    for j=1,ships[i].size do
      grid[x][y].ship=i
      x,y=x+stepX,y+stepY
    end
  end
end
 
local function drawShotToGrid(shot,grid)
  grid[shot[1]][shot[2]].shot=true
  grid[shot[1]][shot[2]].hit=shot[3]  
end
 
local function makeShot(x,y,grid)
  local tile=grid[x][y]
  if tile.shot==true then
    return nil --already shot here!
  end  
 
  local shot={x,y,tile.ship}
  drawShotToGrid(shot,grid)
  if tile.ship then
    ships[tile.ship].hits=ships[tile.ship].hits+1
    if ships[tile.ship].hits==ships[tile.ship].size then
      os.queueEvent("shipsunk",tile.ship)
    end
  end
  return shot
end  
   
 
local function drawTile(scrX,scrY,tile)
  term.setCursorPos(scrX,scrY)
 
  if tile.ship then
    if tile.shot then
      doColor(colors.red,colors.gray)
      term.write("@")
    else
      doColor(colors.white,colors.gray)
      term.write("O")
    end
  else
    if tile.hit then
      doColor(colors.red,colors.gray)
      term.write("x")
    elseif tile.shot then
      doColor(colors.white,colors.lightBlue)
      term.write(".")
    else
      doColor(colors.white,colors.lightBlue)
      term.write(" ")
    end
  end
end
 
local function drawGrid(scrX,scrY,grid)
  doColor(colors.white,colors.black)
  term.setCursorPos(scrX,scrY+1)
  term.write(" ")
  doColor(colors.white,colors.gray)  
  term.setCursorPos(scrX,scrY)
  local pad=11-#grid.title
  term.write(string.rep(" ",math.ceil(pad/2))..grid.title..string.rep(" ",math.floor(pad/2)))
 
  for gx=1,10 do
    term.setTextColor(colors.white)
    term.setBackgroundColor(colors.black)
    term.setCursorPos(scrX+gx,scrY+1)
    term.write(gx==10 and "0" or string.char(string.byte("0")+gx))
   
    term.setCursorPos(scrX,scrY+gx+1)
    term.write(string.char(string.byte("A")+gx-1))
    for gy=1,10 do
      drawTile(scrX+gx,scrY+gy+1,grid[gx][gy])      
    end
  end
  doColor(colors.white,colors.black)
end
 
function moveTargetIndicator(newX,newY)
  --if x has changed...
  if targetX and targetY then
    drawTile(targetX+targetGridBounds.minX-1,targetY+targetGridBounds.minY-1,oppGrid[targetX][targetY])
  end
  doColor(colors.yellow,colors.lightGray)
  if newX~=targetX then
    --space over old
    if targetX then
      term.setCursorPos(targetGridBounds.minX+targetX-1,targetGridBounds.maxY+1)
      term.write(" ")
      term.setCursorPos(targetGridBounds.minX+targetX-1,targetGridBounds.minY-3)
      term.write(" ")
    end
    --draw new
    term.setCursorPos(targetGridBounds.minX+newX-1,targetGridBounds.maxY+1)
    term.write("^")
    term.setCursorPos(targetGridBounds.minX+newX-1,targetGridBounds.minY-3)
    term.write("v")
   
    targetX=newX    
  end
  if newY~=targetY then
    --space over old
    if targetY then
      term.setCursorPos(targetGridBounds.maxX+1,targetGridBounds.minY+targetY-1)
      term.write(" ")
      term.setCursorPos(targetGridBounds.minX-2,targetGridBounds.minY+targetY-1)
      term.write(" ")
    end
    --draw new
    term.setCursorPos(targetGridBounds.maxX+1,targetGridBounds.minY+newY-1)
    term.write("<")
    term.setCursorPos(targetGridBounds.minX-2,targetGridBounds.minY+newY-1)
    term.write(">")
   
    targetY=newY
  end
  term.setCursorPos(15,15)
  term.write("Target : "..toGridRef(targetX,targetY))  
  --if the target tile is a valid target, draw a "+"
  if not oppGrid[targetX][targetY].shot then
    term.setCursorPos(targetX+targetGridBounds.minX-1,targetY+targetGridBounds.minY-1)
    doColor(colors.yellow,colors.lightBlue)
    term.write("+")
  end
end
 
local log={}
 
local termWidth,termHeight=term.getSize()
 
local logHeight=termHeight-3
local logWidth=termWidth-28
 
for i=1,logHeight do
  log[i]=""
end
 
local function printLog()
  doColor(colors.white,colors.black)
  for i=1,logHeight do
    term.setCursorPos(28,1+i)
    local name,line=string.match(log[i],"(<[^>]+> )(.*)")
    if name then
      doColor(colors.lightBlue,colors.black)
      write(name)
      doColor(colors.white,colors.black)
      write(line..string.rep(" ",logWidth-#log[i]))
    else
      write(log[i]..string.rep(" ",logWidth-#log[i]))
    end
  end
end
 
 
 
--shipX/Y are the position of ship on grid; gridX/Y are the offset of the top-left of grid
local function drawShip(size,align,x,y,char)
  local stepX=align=="h" and 1 or 0
  local stepY=stepX==1 and 0 or 1
  for j=1,size do
    term.setCursorPos(x,y)
    term.write(char)
    x,y=x+stepX,y+stepY
  end
end
 
local function setStatusLine(lineNum,text)
  doScreenColor()
  local pad=math.floor((termWidth-#text)/2)
  term.setCursorPos(1,16+lineNum)
  term.write((" "):rep(pad)..text..(" "):rep(termWidth-#text-pad))
end
 
 
doScreenColor()
term.clear()
 
drawGrid(2,2,myGrid)
 
setStatusLine(1,"Started game with "..opponent.." at computer #"..(opponentID or "nil"))
 
local function getShipBounds(ship)
  return {
     minX=ship.pos[1],
     minY=ship.pos[2],
     maxX=ship.pos[1]+(ship.dir=="h" and ship.size-1 or 0),
     maxY=ship.pos[2]+(ship.dir=="v" and ship.size-1 or 0)
   }    
end
 
local function getPointBounds(x,y)
  return {
    minX=x,
    minY=y,
    maxX=x,
    maxY=y,
  }
end
 
local function boundsIntersect(boundsA,boundsB)
  return not (
      boundsA.minX>boundsB.maxX or
      boundsA.maxX<boundsB.minX or
      boundsA.minY>boundsB.maxY or
      boundsA.maxY<boundsB.minY
    )    
end
 
 
local function checkShipCollision(shipIndex)
  local myBounds=getShipBounds(ships[shipIndex])
  for i=1,#ships do        
    if i~=shipIndex and ships[i].pos then
      if boundsIntersect(myBounds,getShipBounds(ships[i])) then
        return i
      end
    end
  end
  return 0
end
 
 
 
local function randomizeShips()
  for i=1,5 do
    ships[i].pos=nil
  end
  for i=1,5 do
    local ship=ships[i]
    local dir
    local x,y
    repeat
      --random orientation
      dir=math.random(2)==1 and "v" or "h"
      --random position
      x = math.random(dir=="v" and 10 or (10-ship.size))
      y = math.random(dir=="h" and 10 or (10-ship.size))
      ship.pos={x,y}
      ship.dir=dir
    until checkShipCollision(i)==0
  end
end    
 
 
 
local function shipPlacement()
  local selection=1
  local collidesWith=0
  local dragging=false
  local moveShip=nil
  local clickedOn=nil
  local clickedAt=nil
 
  doScreenColor()
  term.setCursorPos(28,3)
  write("use arrows to move ship")
  term.setCursorPos(28,4)
  write("press space to rotate")
  term.setCursorPos(28,5)
  write("tab selects next ship")
  if term.isColor() then
    term.setCursorPos(28,6)
    write("click and drag ships")
    term.setCursorPos(28,7)
    write("right-click ship to")
    term.setCursorPos(28,8)
    write("  rotate")
  end
  term.setCursorPos(28,9)
  write('"r" to randomize ships')
  term.setCursorPos(28,10)
  write('"f" when finished')
  randomizeShips()
  setStatusLine(1,"Arrange your ships on the grid")
 
  while true do
    --local placed=0
    --draw sea
    doColor(colors.white,colors.lightBlue)
    for i=1,10 do
      term.setCursorPos(3,3+i)
      term.write("          ")
    end
    --draw ships
    for i=1,#ships do
     --draw ship at sea if it's placed
      if ships[i].pos then
        if collidesWith~=0 and (collidesWith==i or selection==i) then
          doColor(selection==i and colors.red or colors.pink,colors.gray)
          drawShip(ships[i].size,ships[i].dir,2+ships[i].pos[1],3+ships[i].pos[2],"@")
        else
          doColor(selection==i and colors.lime or colors.white,colors.gray)
          drawShip(ships[i].size,ships[i].dir,2+ships[i].pos[1],3+ships[i].pos[2],"O")
        end
      end
    end
 
    local event,p1,p2,p3=os.pullEvent()
    if event=="key" then
      if not dragging then
        if p1==keys.tab then
          if collidesWith==0 then
            selection=(selection%5)+1
          else
            local t=selection
            selection=collidesWith
            collidesWith=t
          end
        elseif p1==keys.up then
          moveShip={0,-1}
        elseif p1==keys.down then
          moveShip={0,1}
        elseif p1==keys.left then
          moveShip={-1,0}
        elseif p1==keys.right then
          moveShip={1,0}
        elseif p1==keys.space then
          moveShip={0,0}
          ships[selection].dir=ships[selection].dir=="h" and "v" or "h"
        elseif p1==keys.f then
          if collidesWith~=0 then
            setStatusLine(2,"You can't finalize with ships overlapping!")
          else
            break
          end
        elseif p1==keys.r then
          randomizeShips();
        end          
      end
    elseif event=="mouse_click" then
      clickedOn=nil
      --click event! figure out what we clicked on
      local clickBounds=getPointBounds(p2,p3)
      local clickGridBounds=getPointBounds(p2-2,p3-3)
 
      for i=1,#ships do
        if ships[i].pos and boundsIntersect(clickGridBounds,getShipBounds(ships[i])) and
           (collidesWith==0 or collidesWith==i or i==selection) then
          --select it
          --if we're switching between the colliding ships, swap selection
          if collidesWith~=0 and i~=selection then
            collidesWith=selection
          end
          --mode="place"
          clickedOn=ships[i]
          clickedOffset={p2-2-ships[i].pos[1],p3-3-ships[i].pos[2]}
          selection=i
          break
        --[[else        
          local labelBounds={minX=15,maxX=24,minY=2*i,maxY=1+2*i}
          if boundsIntersect(clickBounds,labelBounds) and
             (collidesWith==0 or collidesWith==i or i==selection) then
            if collidesWith~=0 then
              if i~=selection then
                collidesWith=selection
              end
            else
              mode="select"
            end
            clickedOn=ships[i]
            clickedOffset={0,0}
            selection=i
            if ships[i].pos==nil then
              ships[i].pos={1,1}
              collidesWith=checkShipCollision(selection)
              break
            end
          end--]]
        end
      end
      if not clickedOn and collidesWith==0 and
          boundsIntersect(clickBounds,{minX=15,maxX=22,minY=13,maxY=13}) then
        break
      elseif clickedOn and p1==2 then
        --can't drag from a right-click!
        clickedOn=nil
        if ships[selection].dir=="h" then
          ships[selection].dir="v"          
          moveShip={p2-2-ships[selection].pos[1],-(p2-2-ships[selection].pos[1])}
        else          
          ships[selection].dir="h"          
          moveShip={p3-3-(ships[selection].pos[2]+ships[selection].size-1),p3-3-(ships[selection].pos[2])}
        end
      end
    elseif event=="mouse_drag" and clickedOn~=nil then      
      --mode="place"
      moveShip={
         p2-2-clickedOffset[1]-ships[selection].pos[1],
         p3-3-clickedOffset[2]-ships[selection].pos[2]}      
    end
 
    if moveShip then
      local curShip=ships[selection]
      --calc position limits based on ship size and alignment
      local maxX=curShip.dir=="h" and (11-curShip.size) or 10
      local maxY=curShip.dir=="v" and (11-curShip.size) or 10
      --apply move and clamp to limits
      local newPos={
        math.min(math.max(curShip.pos[1]+moveShip[1],1),maxX),
        math.min(math.max(curShip.pos[2]+moveShip[2],1),maxY)
      }
      --place the ship
      ships[selection].pos=newPos
      --check for collisions with other ships
 
      collidesWith=checkShipCollision(selection)
      moveShip=nil
    end
  end
end
 
local function displayGameHelp()
  doScreenColor()  
  term.setCursorPos(28,3)
  write("arrows to move cursor")
  term.setCursorPos(28,4)
  write("space to fire")
  if term.isColor() then
    term.setCursorPos(28,6)
    write("click on grid to fire")
  end  
end
 
local function hideHelpArea()
  doScreenColor()  
  for y=3,13 do
    term.setCursorPos(28,y)
    write(string.rep(" ",32))
  end
end
 
 
local function runGame()
 
  --first, ship placement phase!!
  shipPlacement()
 
  hideHelpArea()
 
  --hide the old help, draw the new
 
  --tell the other guy we're done
  rednet.send(opponentID,"bs ready")
  if not opponentReady then
    setStatusLine(1,"Waiting for opponent to finish placing ships")
    while not opponentReady do
      os.pullEvent()
    end
  end
 
  --now, play the game
  --draw my final ship positions intto the grid
  drawShipsToGrid(ships,myGrid)
 
 
  --if I'm host, flip a coin
  if action=="host" then
    math.randomseed(os.time())
    myTurn=math.floor(100*math.random())%2==0
    rednet.send(opponentID,"bs cointoss "..tostring(not myTurn))      
    if myTurn then
      setStatusLine(2,"Your turn, take your shot!")
    else
      setStatusLine(2,"Opponent's turn, waiting...")
    end
  else
    --I joined, wait for coin toss
    setStatusLine(2,"waiting for coin toss...")
    while myTurn==nil do
      os.pullEvent()
    end
  end
 
 
  setStatusLine(1,"")
  if myTurn then
    --I won, I go first
    displayGameHelp()
  end
 
  --draw a target grid  
  drawGrid(2,2,myGrid)
  drawGrid(15,2,oppGrid)
  --initialize target indicators
  moveTargetIndicator(5,5)
  --game turn loop
  while true do
    --wait for my turn
    while not myTurn do
      os.pullEvent()
    end
    --my turn!
    while true do
      local e,p1,p2,p3,p4,p5=os.pullEvent()
      if e=="mouse_click" then
        local clickBounds=getPointBounds(p2,p3)
        if boundsIntersect(clickBounds,targetGridBounds) then
          moveTargetIndicator(p2-15,p3-3)
          local shot=makeShot(targetX,targetY,oppGrid)
          if shot then
            --valid shot, tell the other guy
            rednet.send(opponentID,"bs shot "..targetX.." "..targetY)
            break
          end
        end
      elseif e=="char" then
        p1=string.lower(p1)
        if p1>="a" and p1<="j" then
          --row selected
          moveTargetIndicator(targetX,string.byte(p1)-string.byte("a")+1)
        elseif p1>="0" and p1<="9" then
          local t=string.byte(p1)-string.byte("0")
          if t==0 then t=10 end
          moveTargetIndicator(t,targetY)          
        end
      elseif e=="key" then
        if p1==keys.enter or p1==keys.space and targetX and targetY then
          local shot=makeShot(targetX,targetY,oppGrid)
          if shot then
            rednet.send(opponentID,"bs shot "..targetX.." "..targetY)
            break
          end
        elseif p1==keys.up then
          moveTargetIndicator(targetX,math.max(targetY-1,1))
        elseif p1==keys.down then
          moveTargetIndicator(targetX,math.min(targetY+1,10))
        elseif p1==keys.left then
          moveTargetIndicator(math.max(targetX-1,1),targetY)
        elseif p1==keys.right then
          moveTargetIndicator(math.min(targetX+1,10),targetY)
        end        
      end
    end
    --shot sent, wait for my turn to resolve (top coroutine will switch turns and draw the hit to the grid)
    setStatusLine(2,"Waiting for opponent...")
    while myTurn do
      os.pullEvent()
    end
  end
end
 
local gameRoutine=coroutine.create(runGame)
--if advanced terminal, default focus to chat, can play with mouse
local inChat=term.isColor()
local savedCursorPos={7,19}
 
--redirect just to block scroll
local redir={}
for k,v in pairs(originalTerm) do
  if k~="scroll" then
    redir[k]=v
  else
    redir[k]=function() end
  end
end
originalTerm = term.redirect(redir)
 
--run the game routine once
coroutine.resume(gameRoutine)
--hide cursor
term.setCursorBlink(false)
 
while true do
  local e,p1,p2,p3,p4,p5=os.pullEventRaw()
  if e=="terminate" then
    quit()
  elseif e=="shipsunk" then
    setStatusLine(1,opponent.." sank your "..ships[p1].name.."!")
    rednet.send(opponentID,"bs sink")
    shipsLeft=shipsLeft-1
    if shipsLeft==1 then
      setStatusLine(3,"You only have 1 ship left!")
    elseif shipsLeft>1 then
      setStatusLine(3,"You have "..shipsLeft.." ships left!")
    else
      rednet.send(opponentID,"bs win")
      setStatusLine(3,"You lost the game!")
      break
    end
  elseif e=="rednet_message" then
    local cmd,args=string.match(p2,"^bs (%S+)%s?(.*)")
    if cmd=="ready" then
      opponentReady=true
      os.queueEvent("kickcoroutine")
    elseif cmd=="cointoss" then
      myTurn=args=="true"      
      if myTurn then
        setStatusLine(2,"Your turn, take your shot!")
      else
        setStatusLine(2,"Opponent's turn, waiting...")
      end
      os.queueEvent("kickcoroutine")
    elseif cmd=="shot" then
      if myTurn then
        setStatusLine(3,"What the?! Got a shot but not their turn! Ignoring")
      else
        local tx, ty=string.match(args,"(%d+) (%d+)")
        tx,ty=tonumber(tx),tonumber(ty)
        local tile=myGrid[tx][ty]
        local shot=makeShot(tx,ty,myGrid)
        rednet.send(opponentID,"bs result "..(shot[3] and "hit" or "miss"))
        drawTile(2+tx,3+ty,tile)        
        myTurn=true
        os.queueEvent("kickcoroutine")
        displayGameHelp()
        setStatusLine(1,opponent.." fired at "..toGridRef(tx,ty).." and "..(shot[3] and "hit" or "missed"))        
        setStatusLine(2,"Your turn, take your shot!")
      end
    elseif cmd=="sink" then
      setStatusLine(1,"You sank one of "..opponent.."'s ships!")
      oppShipsLeft=oppShipsLeft-1
      if oppShipsLeft==0 then
        setStatusLine(2,opponent.." has no ships left!")
      elseif oppShipsLeft==1 then
        setStatusLine(2,"Sink 1 more to win!")
      else
        setStatusLine(2,"They have "..oppShipsLeft.." ships left.")
      end
    elseif cmd=="result" then
      if not myTurn then
        setStatusLine(3,"What the?! Got a shot result but not my turn! Ignoring")
      else
        local tile=oppGrid[targetX][targetY]
        tile.hit=args=="hit"
        drawTile(targetX+15,targetY+3,tile)
        myTurn=false
        doColor(tile.hit and colors.red or colors.white,colors.lightGray)
        term.setCursorPos(17,16)
        term.write(tile.hit and "HIT!" or "MISS")
        setStatusLine(2,"Waiting for opponent...")
        os.queueEvent("kickcoroutine")
      end
     
    elseif cmd=="win" then
      --we won!
      setStatusLine(3,"You won the game! Congratulations!")
      break
    end
  --everything else goes to gameRoutine
  else
    --all other events go to this routine
    local succ,err=coroutine.resume(gameRoutine,e,p1,p2,p3,p4,p5)
    if not succ then
      print("game coroutine crashed with the following error: "..err)
      quit()
    end
     
    if coroutine.status(gameRoutine)=="dead" then
      --game over
      break
    end
  end
 
end
 
term.setCursorPos(1,19)
term.clearLine()
term.write("  Press any key to continue...")
os.pullEvent("key")
--if a char event was queued following the key event, this will eat it
os.sleep(0)
 
term.setTextColor(colors.white)
term.setBackgroundColor(colors.black)
term.clear()
quit()
--