function initVariables()
	w,h =term.getSize()  -- 51,  19
	shipYPos = 10
	shipXPos = 24
	shipFacingRight=true
	pressedKey=0
	killedState = false
	lives=3
	score =0
	aliens=10
	killedDelay=0
	running=true
	moveLeft=false
	moveRight=true
	moveUp=false
	moveDown=false
	human1x = 3
	human1y = 18
	human2x = 15
	human2y = 18
	human3x = 40
	human3y = 18
	human4x = 60
	human4y = 18
	human5x = 70
	human5y = 18
	human6x = 85
	human6y = 18
	human1 = true
	human2 = true 
	human3 = true 
	human4 = true 
	human5 = true 
	human6 = true 
	human1Abducted=false
	human2Abducted=false
	human3Abducted=false
	human4Abducted=false
	human5Abducted=false
	human6Abducted=false
	humansLeft=6
	bulletXPos=0
	bulletYPos=0
	bulletState=false
	bulletGoingRight=true
	alien1 = false
	alien1y = 2
	alien1x =84
	alien1Abduct=false
	alien1Carry=false
	alien1Step=2
	stepValue=0.1 --0.1
end

function clear()
	term.clear()
	term.setCursorPos(1,1)
	term.setBackgroundColour(colours.black)
	term.setTextColour(colours.white)
end

function drawGrass()
	term.setCursorPos(1,h)
	term.setBackgroundColour(colours.green)
	write(string.rep(" ",w))
	term.setCursorPos(1,1)
	term.setBackgroundColour(colours.black)
end

function drawShip(yPos)
	if shipFacingRight==true then
		term.setCursorPos(24,yPos)
		term.setBackgroundColour(colours.orange)
		print(" ")
		term.setCursorPos(25,yPos)
		term.setBackgroundColour(colours.white)
		print("  ")
	else
		term.setCursorPos(26,yPos)
		term.setBackgroundColour(colours.orange)
		print(" ")
		term.setCursorPos(24,yPos)
		term.setBackgroundColour(colours.white)
		print("  ")
	end
	term.setBackgroundColour(colours.black)
end

function delShip(yPos)
	term.setCursorPos(24,yPos)
	term.setBackgroundColour(colours.black)
	print("   ")
end

function drawAliens()
	term.setBackgroundColour(colours.cyan)
	if alien1==true then
		term.setCursorPos(alien1x,alien1y)
		write("   ")
	end
	term.setBackgroundColour(colours.black)
end

function delAliens()
	term.setBackgroundColour(colours.black)
	if alien1==true then
		term.setCursorPos(alien1x,alien1y)
		write("   ")
	end
end

function drawHumans()
	term.setBackgroundColour(colours.pink)
	if human1==true then
		term.setCursorPos(human1x,human1y)
		write(" ")
	end
	if human2==true then
		term.setCursorPos(human2x,human2y)
		write(" ")
	end
	if human3==true then
		term.setCursorPos(human3x,human3y)
		write(" ")
	end
	if human4==true then
		term.setCursorPos(human4x,human4y)
		write(" ")
	end
	if human5==true then
		term.setCursorPos(human5x,human5y)
		write(" ")
	end
	if human6==true then
		term.setCursorPos(human6x,human6y)
		write(" ")
	end
	term.setBackgroundColour(colours.green)
	term.setCursorPos(1,19)
	write("   ")
	term.setBackgroundColour(colours.black)
end

function delHumans()
	term.setBackgroundColour(colours.black)
	if human1==true then
		term.setCursorPos(human1x,human1y)
		write(" ")
	end
	if human2==true then
		term.setCursorPos(human2x,human2y)
		write(" ")
	end
	if human3==true then
		term.setCursorPos(human3x,human3y)
		write(" ")
	end
	if human4==true then
		term.setCursorPos(human4x,human4y)
		write(" ")
	end
	if human5==true then
		term.setCursorPos(human5x,human5y)
		write(" ")
	end
	if human6==true then
		term.setCursorPos(human6x,human6y)
		write(" ")
	end
end

function drawBullet()
	term.setBackgroundColour(colours.yellow)
	term.setCursorPos(bulletXPos,bulletYPos)
	write(" ")
	term.setBackgroundColour(colours.black)
end

function delBullet()
	term.setBackgroundColour(colours.black)
	term.setCursorPos(bulletXPos,bulletYPos)
	write(" ")
end

function newHighScoreTable()
	name1="Dan" score1=1000
	name2="Fred" score2=800
	name3="Fred" score3=600
	name4="Fred" score4=400
	name5="Fred" score5=200
	local highScoreTable = {{name1, score1}, {name2,score2}, {name3,score3}, {name4,score4}, {name5,score5}}
	local newHighScoreStr = textutils.serialize(highScoreTable)
	--print("new table "..newHighScoreStr)-- debug
	fs.makeDir("protectordata")
	local handle = fs.open("protectordata/pdata","w")
	handle.write(newHighScoreStr)
	handle.close()
end

function printCent(xpos,text)
	local ypos = w/2 - (string.len(text)/2)
	term.setCursorPos(ypos,xpos)
	write(text)
end

function introHighScoreTable()
	term.clear()
	term.setCursorPos(35,1) write("SPACE WHEN READY")
	if fs.exists("protectordata")==false then
		newHighScoreTable()
	end
	local handle = fs.open("protectordata/pdata","r")
	local dataStr = handle.readAll()
	handle.close()
	--print("dataStr "..dataStr)
	highScoreData = textutils.unserialize(dataStr)
	--print(highScoreData[2])
	name1 = highScoreData[1][1] score1 = highScoreData[1][2]
	name2 = highScoreData[2][1] score2 = highScoreData[2][2]
	name3 = highScoreData[3][1] score3 = highScoreData[3][2]
	name4 = highScoreData[4][1] score4 = highScoreData[4][2]
	name5 = highScoreData[5][1] score5 = highScoreData[5][2]
	term.setTextColour(colours.yellow)
	printCent(5,"HIGH SCORES")
	term.setTextColour(colours.white)
	printCent(7,name1.." "..score1)
	printCent(8,name2.." "..score2)
	printCent(9,name3.." "..score3)
	printCent(10,name4.." "..score4)
	printCent(11,name5.." "..score5)
end


function printScore()
	term.clear()
	term.setTextColour(colours.yellow)
	printCent(3,"HIGH SCORES")
	term.setTextColour(colours.white)
	printCent(5,name1.." "..score1)
	printCent(6,name2.." "..score2)
	printCent(7,name3.." "..score3)
	printCent(8,name4.." "..score4)
	printCent(9,name5.." "..score5)
	playAgain()
end

function rewriteScores()
	if newScore > score1 then 
		name5=name4 score5=score4
		name4=name3 score4=score3
		name3=name2 score3=score2
		name2=name1 score2=score1
		name1= newName score1=newScore
	elseif newScore > score2 then
		name5=name4 score5=score4
		name4=name3 score4=score3
		name3=name2 score3=score2
		name2=newName score2=newScore
	elseif newScore > score3 then
		name5=name4 score5=score4
		name4=name3 score4=score3
		name3=newName score3=newScore
	elseif newScore > score4 then
		name5=name4 score5=score4
		name4=newName score4=newScore
	elseif newScore > score5 then
		name5=newName score5=newScore	
	end
	local highScoreTable = {{name1, score1}, {name2,score2}, {name3,score3}, {name4,score4}, {name5,score5}}
	local newHighScoreStr = textutils.serialize(highScoreTable)
	local handle = fs.open("protectordata/pdata","w")
	handle.write(newHighScoreStr)
	handle.close()
end

function newHighScoreObtained()
	clear()
	term.setTextColour(colours.yellow)
	printCent(8,"CONGRATULATIONS")
	term.setTextColour(colours.white)
	printCent(10,"You have a new high score!")
	printCent(12,"Enter your name: ")
	printCent(14,"  ")
	local newNameStr = ""
	local newNameStrLen = 0
	while true do
		local event,p1,p2,p3 = os.pullEvent()
		if event=="char" and newNameStrLen < 9 then
			newNameStr=newNameStr..p1
			newNameStrLen=newNameStrLen+1
			printCent(14,newNameStr.." ")
			
		elseif event=="key" and p1 == 14 and newNameStrLen>0 then
			newNameStr=string.sub(newNameStr,1,string.len(newNameStr)-1)
			newNameStrLen=newNameStrLen-1
			term.setCursorPos(1,14)
			write(string.rep(" ",w))
			printCent(14,newNameStr.." ")
		elseif event=="key" and p1== 28 then
			newName = newNameStr
			newScore = score
			rewriteScores()
			printScore()
		end
	end
	

	
end

function highScore()
	if fs.exists("protectordata")==false then
		newHighScoreTable()
	end
	local handle = fs.open("protectordata/pdata","r")
	local dataStr = handle.readAll()
	handle.close()
	--print("dataStr "..dataStr)
	highScoreData = textutils.unserialize(dataStr)
	--print(highScoreData[2])
	name1 = highScoreData[1][1] score1 = highScoreData[1][2]
	name2 = highScoreData[2][1] score2 = highScoreData[2][2]
	name3 = highScoreData[3][1] score3 = highScoreData[3][2]
	name4 = highScoreData[4][1] score4 = highScoreData[4][2]
	name5 = highScoreData[5][1] score5 = highScoreData[5][2]
	if score>score5 then
		newHighScoreObtained()
	end
	printScore()
	
	
end




function gameOver(gameOverMsg)
	clear()
	delShip(shipYPos)
	term.setCursorPos(40,1)
	write("Lives: "..lives.."  ")
	term.setCursorPos(5,1)
	if score<0 then score=0 end
	write("Score: "..score.."  ")
	term.setTextColour(colours.red)
	term.setCursorPos( (w/2)-5 , h/2 -1)
	print("GAME OVER")
	term.setCursorPos( (w/2)-(string.len(gameOverMsg)/2) , (h/2)+1)
	print(gameOverMsg)
	term.setTextColour(colours.white)
	running=false
	sleep(1.5)
	highScore()-- new
	--playAgain 
end

function playAgain()
	sleep(1)
	printCent(12,"Play again (Y or N)")
	
	while true do
		local event,p1,p2,p3 = os.pullEvent()
		if event=="char" then
			if string.lower(p1)=="y" then 
				runGame()
			elseif string.lower(p1)=="n" then 
				os.shutdown()
			end
		end
	end
	
end

function killPlayer()
	moveLeft=false
	moveRight=false
	moveUp=false
	moveDown=false
	delShip(shipYPos)
	lives=lives-1
	if lives==0 then 
		gameOver("OUT OF LIVES")
	end
	killedState=true
	--killedStr="true"
end

function left()
	delHumans()
	delAliens()
	human1x=human1x+1
	human2x=human2x+1
	human3x=human3x+1
	human4x=human4x+1
	human5x=human5x+1
	human6x=human6x+1
	alien1x=alien1x+1
	if human1x>100 then human1x=0 end
	if human2x>100 then human2x=0 end
	if human3x>100 then human3x=0 end
	if human4x>100 then human4x=0 end
	if human5x>100 then human5x=0 end
	if human6x>100 then human6x=0 end
	if alien1x>100 then alien1x=0 end
	shipFacingRight=false
	checkShipCollision()
	drawShip(shipYPos)
	drawHumans()
	drawAliens()
	drawBorder()
	moveRight=false
end

function right()
	delHumans()
	delAliens()
	human1x=human1x-1
	human2x=human2x-1
	human3x=human3x-1
	human4x=human4x-1
	human5x=human5x-1
	human6x=human6x-1
	alien1x=alien1x-1
	if human1x<1 then human1x=100 end
	if human2x<1 then human2x=100 end
	if human3x<1 then human3x=100 end
	if human4x<1 then human4x=100 end
	if human5x<1 then human5x=100 end
	if human6x<1 then human6x=100 end
	if alien1x<1 then alien1x=100 end
	shipFacingRight=true
	checkShipCollision()
	drawShip(shipYPos)
	drawHumans()
	drawAliens()
	drawBorder()
	moveLeft=false
end

function up()
	if shipYPos > 2 then 
		delShip(shipYPos)
		shipYPos=shipYPos-1
		checkShipCollision()
		drawShip(shipYPos)
	end
	moveUp=false
	moveDown=false
end

function down()
	if shipYPos<17 then 
		delShip(shipYPos)
		shipYPos=shipYPos+1
		checkShipCollision()
		drawShip(shipYPos)
	end
	moveDown=false
	moveUp=false
end

function checkShipCollision()
	if killedState==false then
		if shipYPos == alien1y and alien1== true then
			if alien1x >= 22 and alien1x <= 26 then
				alien1Hit()
				killPlayer()
			end
		elseif human1==true and human1y==shipYPos then
			if human1x >=24 and human1x <= 26 then
				human1=false
				humanHitRoutine()
			end
		elseif human2==true and human2y==shipYPos then
			if human2x >=24 and human2x <= 26 then
				human2=false
				humanHitRoutine()
			end
		elseif human3==true and human3y==shipYPos then
			if human3x >=24 and human3x <= 26 then
				human3=false
				humanHitRoutine()
			end	
		elseif human4==true and human4y==shipYPos then
			if human4x >=24 and human4x <= 26 then
				human4=false
				humanHitRoutine()
			end
		elseif human5==true and human5y==shipYPos then
			if human5x >=24 and human5x <= 26 then
				human5=false
				humanHitRoutine()
			end
		elseif human6==true and human6y==shipYPos then
			if human6x >=24 and human6x <= 26 then
				human6=false
				humanHitRoutine()
			end	
		end
	end
end

function alienGen()
	if alien1==false then
		local alienChance= math.random(1,10)
		if alienChance==1 then
			if human1==true then
				alien1 = true
				alien1y = 2
				alien1x = human1x - 1
			end
		elseif alienChance == 2 then
			if human2==true then
				alien1 = true
				alien1y=2
				alien1x = human2x-1
			end
		elseif alienChance == 3 then
			if human3==true then
				alien1 = true
				alien1y=2
				alien1x = human3x-1
			end
		elseif alienChance == 4 then
			if human4==true then
				alien1 = true
				alien1y=2
				alien1x = human4x-1
			end
		elseif alienChance == 5 then
			if human5==true then
				alien1 = true
				alien1y=2
				alien1x = human5x-1
			end
		elseif alienChance == 6 then
			if human6==true then
				alien1 = true
				alien1y=2
				alien1x = human6x-1
			end
		end
	end
end

function alienDown()
	if alien1==true and alien1Abduct==false and alien1y<19 then
		alien1Step=alien1Step+stepValue
		alien1y=math.floor(alien1Step)
		if alien1==true and alien1Abduct==false and alien1y==17 then
			alien1Abduct=true
			alien1Carry=true
			alien1Step=17
		end	
	end
end

function alienRoutine()
	alien1=false
	alien1Step=2
	if alien1Carry==true then
		score= score -50
		humansLeft=humansLeft-1
	end
	alien1Abduct=false
	alien1Carry=false
	if humansLeft==0 then 
		gameOver("NO HUMANS LEFT")
	end
	
end

function alienUp()
	if alien1==true and alien1Abduct==true then
		if alien1x+1 == human1x then 
			human1Abducted=true
			alien1Step=alien1Step-stepValue
			alien1y=math.floor(alien1Step)
			human1y=math.floor(alien1Step)+1
			human1x=alien1x+1
			if human1y<=2 then 
				alienRoutine()
				human1=false
			end
		elseif alien1x+1 == human2x then 
			human2Abducted=true
			alien1Step=alien1Step-stepValue
			alien1y=math.floor(alien1Step)
			human2y=math.floor(alien1Step)+1
			human2x=alien1x+1
			if human2y<=2 then 
				alienRoutine()
				human2=false
			end
		elseif alien1x+1 == human3x then 
			human3Abducted=true
			alien1Step=alien1Step-stepValue
			alien1y=math.floor(alien1Step)
			human3y=math.floor(alien1Step)+1
			human3x=alien1x+1
			if human3y<=2 then 
				alienRoutine()
				human3=false
			end
		elseif alien1x+1 == human4x then 
			human4Abducted=true
			alien1Step=alien1Step-stepValue
			alien1y=math.floor(alien1Step)
			human4y=math.floor(alien1Step)+1
			human4x=alien1x+1
			if human4y<=2 then 
				alienRoutine()
				human4=false
			end
		elseif alien1x+1 == human5x then 
			human5Abducted=true
			alien1Step=alien1Step-stepValue
			alien1y=math.floor(alien1Step)
			human5y=math.floor(alien1Step)+1
			human5x=alien1x+1
			if human5y<=2 then 
				alienRoutine()
				human5=false
			end
		elseif alien1x+1 == human6x then 
			human6Abducted=true
			alien1Step=alien1Step-stepValue
			alien1y=math.floor(alien1Step)
			human6y=math.floor(alien1Step)+1
			human6x=alien1x+1
			if human6y<=2 then 
				alienRoutine()
				human6=false
			end	
		end
	end
	if alien1==false then alienGen() end
end

function keyPress()  -- 200 UP, 208 DOWN, 203 LEFT,  205 RIGHT,  57 SPACE, 16 Q
		if pressedKey==200 or pressedKey == 17 then -- up
			moveUp=true
			moveDown=false
		elseif pressedKey==208 or pressedKey == 31 then -- DOWN
			moveDown=true
			moveUp=false
		elseif pressedKey==203 or pressedKey == 30 then -- left
			moveLeft=true	
			moveRight=false
		elseif pressedKey==205 or pressedKey == 32 then -- right
			moveRight=true
			moveLeft=false
		elseif pressedKey==57 then -- space
				if bulletState==false then
					bulletYPos=shipYPos
					bulletState=true
					if shipFacingRight==true then 
						bulletXPos=shipXPos+3
						bulletGoingRight=true
					else
						bulletXPos=shipXPos-1
						bulletGoingRight=false
					end
				end
		elseif pressedKey==25 then -- q  (use 25 if p for quit)
				gameOver("YOU QUIT")
		end
		
		--term.setCursorPos(30,1)
		--write(pressedKey.."  ")
end

function removeBullet()
	if bulletGoingRight==true then 
		bulletXPos = 60
	else
		bulletXPos = -10
	end
end

function alien1Hit()
	delAliens()
	alien1=false
	score=score+20
	alien1Step=2
	alien1Abduct=false
	removeBullet()
	drawAliens()
end

function humanHitRoutine()
	score=score-50
	humansLeft=humansLeft-1
	if humansLeft==0 then 
		gameOver("NO HUMANS LEFT")
	end
	if alien1Carry==true then alien1Carry=false end
end


function checkBulletCollision()
	if alien1 == true and bulletYPos == alien1y then 
		if bulletXPos >= alien1x  and bulletXPos <= alien1x + 3 then 
			alien1Hit() 
		end
	end
	if human1 == true and bulletYPos == human1y and bulletXPos == human1x then human1=false humanHitRoutine()  end
	if human2 == true and bulletYPos == human2y and bulletXPos == human2x then human2=false humanHitRoutine()  end
	if human3 == true and bulletYPos == human3y and bulletXPos == human3x then human3=false humanHitRoutine()  end
	if human4 == true and bulletYPos == human4y and bulletXPos == human4x then human4=false humanHitRoutine()  end
	if human5 == true and bulletYPos == human5y and bulletXPos == human5x then human5=false humanHitRoutine()  end
	if human6 == true and bulletYPos == human6y and bulletXPos == human6x then human6=false humanHitRoutine()  end
end

function drawBorder()
	term.setBackgroundColour(colours.black)
	for i=1,h-2 do
		term.setCursorPos(1,i+1)
		write(" ")
		term.setCursorPos(w,i+1)
		write(" ")
	end
end

function dropHumans()
	if alien1Abduct==false then
		if human1y<18 then human1y = human1y+1 end
		if human2y<18 then human2y = human2y+1 end
		if human3y<18 then human3y = human3y+1 end
		if human4y<18 then human4y = human4y+1 end
		if human5y<18 then human5y = human5y+1 end
		if human6y<18 then human6y = human6y+1 end
	end
end


function gameControl()

	gameTimer=os.startTimer(0.1)
		
	while running do
		local event,p1,p2,p3 = os.pullEvent()
		if score<0 then score=0 end
		term.setCursorPos(1,1)
		term.setBackgroundColour(colours.yellow)
		write(string.rep(" ",w))
	
	
		term.setTextColour(colours.red)
		term.setCursorPos(5,1)
		write("Score: "..score.."  ")
		term.setCursorPos(20,1)
		write("Humans Left: "..humansLeft.."  ")
		term.setCursorPos(40,1)
		write("Lives: "..lives.."  ")
	
		term.setBackgroundColour(colours.black)
		term.setTextColour(colours.white)
		
		local newStepValue = (score+0.1)/1000
		if newStepValue > stepValue then stepValue= newStepValue end
		if stepValue>0.4 then stepValue=0.4 end
		
		
		--[[DEBUG
		term.setCursorPos(2,2)
		write("human1x "..human1x.."  ")
		term.setCursorPos(2,3)
		write("human2x "..human2x.."  ")
		term.setCursorPos(2,4)
		write("human3x "..human3x.."  ")
		term.setCursorPos(2,5)
		write("human4x "..human4x.."  ")
		term.setCursorPos(2,6)
		write("human5x "..human5x.."  ")
		term.setCursorPos(2,7)
		write("human6x "..human6x.."  ")
		]]--
		
				
		if event=="timer" and gameTimer == p1 then
			if killedState==true then 
				delShip(shipYPos)
				delHumans()
				dropHumans()
				drawHumans()
				killedDelay = killedDelay + 1
				if killedDelay>20 then
					shipYPos = 10
					killedState = false
					term.setBackgroundColour(colours.black)
					for i = 2, h-2 do
						term.setCursorPos(1,i)
						write(string.rep(" ",w))
					end
					drawGrass()
					if shipFacingRight==true then
						moveRight=true
						moveLeft=false
					else
						moveLeft=true
						moveRight=false
					end 
					killedDelay=0
				end
			else
				
				--alienGen()
				drawShip(shipYPos)
				delAliens()
				
				delHumans()
				dropHumans()
				alienDown()
				alienUp()
				drawAliens()
				drawHumans()
				drawBorder()
			end
			
			if bulletState==true then
				if bulletGoingRight==true then
					delBullet()
					bulletXPos=bulletXPos+1
					checkBulletCollision()
					if bulletXPos>45 then
						bulletState=false
					else
						if killedState==false then drawBullet() end
					end
				else
					delBullet()
					bulletXPos=bulletXPos-1
					checkBulletCollision()
					if bulletXPos<6 then
						bulletState=false
					else
						if killedState==false then drawBullet() end
					end
				end
			end
			
			if moveLeft==true then
				left()
			end
			if moveRight==true then
				right()
			end
			if moveUp==true then
				up()
			end
			if moveDown==true then
				down()
			end
			
			gameTimer=os.startTimer(0.1)
		
		elseif event=="key" and killedState==false then 
			pressedKey=p1
			keyPress()
		end
		
	end 
	
end

function runGame()
	initVariables()
	clear()
	drawGrass()
	drawHumans()
	alienGen()
	gameControl()
end


function pix(xCo,yCo,text,col)
	if col== nil then term.setBackgroundColour(colours.black) 
	elseif col =="white" then term.setBackgroundColour(colours.white)
	elseif col =="green" then term.setBackgroundColour(colours.green)
	elseif col =="pink" then term.setBackgroundColour(colours.pink)
	elseif col =="orange" then term.setBackgroundColour(colours.orange)
	elseif col =="cyan" then term.setBackgroundColour(colours.cyan)
	elseif col =="yellow" then term.setBackgroundColour(colours.yellow)
	end
	term.setCursorPos(xCo,yCo)
	write(text)
end

function drawHumanPix()
	drawGrass()
	pix(23,humanPixY," ","pink")
	pix(27,humanPixY," ","pink")
	pix(24,humanPixY-1," ","pink")
	pix(26,humanPixY-1," ","pink")
	pix(25,humanPixY-2," ","pink")
	pix(23,humanPixY-3,"     ","pink")
	pix(25,humanPixY-4," ","pink")
	pix(24,humanPixY-5,"   ","pink")
	pix(24,humanPixY-6,"   ","pink")
end

function delHumanPix()
	pix(23,humanPixY," ")
	pix(27,humanPixY," ")
	pix(24,humanPixY-1," ")
	pix(26,humanPixY-1," ")
	pix(25,humanPixY-2," ")
	pix(23,humanPixY-3,"     ")
	pix(25,humanPixY-4," ")
	pix(24,humanPixY-5,"   ")
	pix(24,humanPixY-6,"   ")
end

function drawAlienPix()
	pix(19,alienPixY,"             ","cyan")
	pix(17,alienPixY+1,"                 ","cyan")
	pix(19,alienPixY+2,"             ","cyan")
end

function delAlienPix()
	pix(19,alienPixY,"             ")
	pix(17,alienPixY+1,"                 ")
	pix(19,alienPixY+2,"             ")
end

function drawShipPix()
	pix(shipPixX+3,3,"  ","white")
	pix(shipPixX+3,4,"   ","white")
	pix(shipPixX+3,5,"           ","white")
	pix(shipPixX+3,6,"               ","white")
	pix(shipPixX+3,7,"                 ","white")
	pix(shipPixX+2,5," ","orange")
	pix(shipPixX+2,6," ","yellow")
	pix(shipPixX+2,7," ","orange")
	pix(shipPixX,6,"  ","orange")
	pix(shipPixX+14,5,"  ","cyan")
end

function delShipPix()
	pix(shipPixX+3,3,"  ")
	pix(shipPixX+3,4,"   ")
	pix(shipPixX+3,5,"           ")
	pix(shipPixX+3,6,"               ")
	pix(shipPixX+3,7,"                 ")
	pix(shipPixX+2,5," ")
	pix(shipPixX+2,6," ")
	pix(shipPixX+2,7," ")
	pix(shipPixX,6,"  ")
	pix(shipPixX+14,5,"  ")
end

function line1()
	pix(8,4,"   ","white")
	pix(12,4,"  ","white")
	pix(16,4,"   ","white")
	pix(20,4,"   ","white")
	pix(24,4,"   ","white")
	pix(28,4,"   ","white")
	pix(32,4,"   ","white")
	pix(36,4,"   ","white")
	pix(40,4,"  ","white")
end

function line2()
	pix(8,5," ","white")
	pix(10,5," ","white")
	pix(12,5," ","white")
	pix(14,5," ","white")
	pix(16,5," ","white")
	pix(18,5," ","white")
	pix(21,5," ","white")
	pix(24,5," ","white")
	pix(28,5," ","white")
	pix(33,5," ","white")
	pix(36,5," ","white")
	pix(38,5," ","white")
	pix(40,5," ","white")
	pix(42,5," ","white")
	
end

function line3()
	pix(8,6,"   ","white")
	pix(12,6,"   ","white")
	pix(16,6," ","white")
	pix(18,6," ","white")
	pix(21,6," ","white")
	pix(24,6,"   ","white")
	pix(28,6," ","white")
	pix(33,6," ","white")
	pix(36,6," ","white")
	pix(38,6," ","white")
	pix(40,6,"   ","white")
end

function line4()
	pix(8,7," ","white")
	pix(12,7,"  ","white")
	pix(16,7," ","white")
	pix(18,7," ","white")
	pix(21,7," ","white")
	pix(24,7," ","white")
	pix(28,7," ","white")
	pix(33,7," ","white")
	pix(36,7," ","white")
	pix(38,7," ","white")
	pix(40,7,"  ","white")
end

function line5()
	pix(8,8," ","white")
	pix(12,8," ","white")
	pix(14,8," ","white")
	pix(16,8,"   ","white")
	pix(21,8," ","white")
	pix(24,8,"   ","white")
	pix(28,8,"   ","white")
	pix(33,8," ","white")
	pix(36,8,"   ","white")
	pix(40,8," ","white")
	pix(42,8," ","white")
end


function startScreen()

	clear()
	term.setBackgroundColour(colours.green)
	term.setCursorPos(1,h)
	write(string.rep(" ",w))
	local screenStage=0
	
	screenTimer=os.startTimer(0.1)
	while true do
		local event,p1,p2,p3=os.pullEvent()
		if event=="key"and p1==57 then
			term.setBackgroundColour(colours.black)
			clear()
			runGame()
		elseif event=="timer" and screenTimer == p1 then
		
			--term.setCursorPos(1,1) write("screenStage: "..screenStage.."  ")
			
			term.setBackgroundColour(colours.black)
			term.setCursorPos(35,1) write("SPACE WHEN READY")
			
			if screenStage>0 and screenStage<0.5 then
				humanPixY = 18
				drawHumanPix()
			elseif screenStage>2 and screenStage<2.9 then
				alienPixY = -2
				drawAlienPix()
			elseif screenStage>3 and screenStage<3.9 then
				alienPixY = -2
				delAlienPix()
				alienPixY = -1
				drawAlienPix() 
			elseif screenStage>4 and screenStage<4.9 then
				alienPixY = -1
				delAlienPix()
				alienPixY = 0
				drawAlienPix() 	
			elseif screenStage>5 and screenStage<5.9 then
				alienPixY = 0
				delAlienPix()
				alienPixY = 1
				drawAlienPix() 
			elseif screenStage>6 and screenStage<6.9 then
				alienPixY = 1
				delAlienPix()
				alienPixY = 2
				drawAlienPix()
			elseif screenStage>7 and screenStage<7.9 then
				alienPixY = 2
				delAlienPix()
				alienPixY = 3
				drawAlienPix()
			elseif screenStage>8 and screenStage<8.9 then
				alienPixY = 3
				delAlienPix()
				alienPixY = 4
				drawAlienPix()	
			elseif screenStage>8 and screenStage<9.9 then
				alienPixY = 4
				delAlienPix()
				alienPixY = 5
				drawAlienPix()
			elseif screenStage>10 and screenStage<10.4 then
				pix(25,8," ","yellow")
				pix(25,9," ","yellow")
				pix(25,10," ","yellow")
				pix(25,11," ","yellow")
				pix(25,17," ","yellow")
				pix(25,18," ","yellow")
			elseif screenStage>10.4 and screenStage<10.6 then
				pix(25,8," ","yellow")
				pix(25,9," ","yellow")
				pix(25,10," ","yellow")
				pix(24,11,"   ","yellow")
				pix(24,12,"   ","yellow")
				pix(24,13,"   ","yellow")
				pix(24,14,"   ","yellow")
				pix(23,15,"     ","yellow")
				pix(23,16,"     ","yellow")
				pix(23,17,"     ","yellow")
				pix(23,18,"     ","yellow")
				humanPixY = 18
				drawHumanPix()
			elseif screenStage>10.6 and screenStage<10.8 then
				pix(25,8," ","yellow")
				pix(25,9," ","yellow")
				pix(24,10,"   ","yellow")
				pix(24,11,"   ","yellow")
				pix(24,12,"   ","yellow")
				pix(23,13,"     ","yellow")
				pix(23,14,"     ","yellow")
				pix(23,15,"     ","yellow")
				pix(22,16,"       ","yellow")
				pix(22,17,"       ","yellow")
				pix(22,18,"       ","yellow")
				humanPixY = 18
				drawHumanPix()	
			elseif screenStage>10.8 and screenStage<11 then
				pix(25,8," ","yellow")
				pix(24,9,"   ","yellow")
				pix(24,10,"   ","yellow")
				pix(23,11,"     ","yellow")
				pix(23,12,"     ","yellow")
				pix(22,13,"       ","yellow")
				pix(22,14,"       ","yellow")
				pix(21,15,"         ","yellow")
				pix(21,16,"         ","yellow")
				pix(20,17,"           ","yellow")
				pix(20,18,"           ","yellow")
				humanPixY = 18
				drawHumanPix()		
			elseif screenStage>11.9 and screenStage<12 then
				pix(1,6,"  ","yellow")
			elseif screenStage>12 and screenStage<12.1 then
				pix(1,6,"  ")
				pix(3,6,"  ","yellow")
			elseif screenStage>12.1 and screenStage<12.2 then
				pix(3,6,"  ")
				pix(5,6,"  ","yellow")	
			elseif screenStage>12.2 and screenStage<12.3 then
				pix(5,6,"  ")
				pix(7,6,"  ","yellow")
			elseif screenStage>12.3 and screenStage<12.4 then
				pix(7,6,"  ")
				pix(9,6,"  ","yellow")
			elseif screenStage>12.4 and screenStage<12.5 then
				pix(9,6,"  ")
				pix(11,6,"  ","yellow")
			elseif screenStage>12.5 and screenStage<12.6 then
				pix(11,6,"  ")
				pix(13,6,"  ","yellow")
			elseif screenStage>12.6 and screenStage<12.7 then
				pix(13,6,"  ")
				pix(15,6,"  ","yellow")
			elseif screenStage>12.7 and screenStage<12.8 then
				term.setBackgroundColour(colours.black)
				for i= 5, h-1 do
					term.setCursorPos(15,i)
					write("                    ")
				end
				humanPixY=18
				drawHumanPix()
			elseif screenStage>13 and screenStage<13.1 then	
				shipPixX= -16
				drawShipPix()
			elseif screenStage>13 and screenStage<13.1 then	
				delShipPix()
				shipPixX= -15
				drawShipPix()	
			elseif screenStage>13.1 and screenStage<13.2 then	
				delShipPix()
				shipPixX= -12
				drawShipPix()	
			elseif screenStage>13.2 and screenStage<13.3 then	
				delShipPix()
				shipPixX= -9
				drawShipPix()
			elseif screenStage>13.2 and screenStage<13.3 then	
				delShipPix()
				shipPixX= -6
				drawShipPix()
			elseif screenStage>13.3 and screenStage<13.4 then	
				delShipPix()
				shipPixX= -3
				drawShipPix()
			elseif screenStage>13.4 and screenStage<13.5 then	
				delShipPix()
				shipPixX= 0
				drawShipPix()
			elseif screenStage>13.6 and screenStage<13.7 then	
				delShipPix()
				shipPixX= 3
				drawShipPix()
			elseif screenStage>13.8 and screenStage<13.9 then	
				delShipPix()
				shipPixX= 6
				drawShipPix()
			elseif screenStage>13.9 and screenStage<14 then	
				delShipPix()
				shipPixX= 9
				drawShipPix()
			elseif screenStage>14.1 and screenStage<14.2 then	
				delShipPix()
				shipPixX= 12
				drawShipPix()
			elseif screenStage>14.2 and screenStage<14.3 then	
				delShipPix()
				shipPixX= 15
				drawShipPix()
			elseif screenStage>14.3 and screenStage<14.4 then	
				delShipPix()
				shipPixX= 18
				drawShipPix()
			elseif screenStage>14.4 and screenStage<14.5 then	
				delShipPix()
				shipPixX= 21
				drawShipPix()
			elseif screenStage>14.5 and screenStage<14.6 then	
				delShipPix()
				shipPixX= 24
				drawShipPix()
			elseif screenStage>14.6 and screenStage<14.7 then	
				delShipPix()
				shipPixX= 27
				drawShipPix()
			elseif screenStage>14.7 and screenStage<14.8 then	
				delShipPix()
				shipPixX= 30
				drawShipPix()
			elseif screenStage>14.8 and screenStage<14.9 then	
				delShipPix()
				shipPixX= 33
				drawShipPix()
			elseif screenStage>14.9 and screenStage<15 then	
				delShipPix()
				shipPixX= 36
				drawShipPix()
			elseif screenStage>15 and screenStage<15.1 then	
				delShipPix()
				shipPixX= 39
				drawShipPix()
			elseif screenStage>15.1 and screenStage<15.2 then	
				delShipPix()
				shipPixX= 41
				drawShipPix()
			elseif screenStage>15.2 and screenStage<15.3 then	
				delShipPix()
				shipPixX= 44
				drawShipPix()
			elseif screenStage>15.3 and screenStage<15.4 then	
				delShipPix()
				shipPixX= 47
				drawShipPix()
			elseif screenStage>15.4 and screenStage<15.5 then	
				delShipPix()
				shipPixX= 50
				drawShipPix()
			elseif screenStage>15.5 and screenStage<15.6 then	
				delShipPix()
			elseif screenStage>16 and screenStage<16.9 then
				humanPixY=18
				delHumanPix()
				line1()
				line2()
				line3()
				line4()
				line5()
			elseif screenStage>17 and screenStage<22 then
				term.setCursorPos((w/2)-6,10)
				write("by FredTHead")
				term.setCursorPos((w/2)-13,12)
				write("WSAD or arrow keys to move")
				term.setCursorPos((w/2)-6,13)
				write("SPACE to fire")
				term.setCursorPos((w/2)-4,14)
				write("P to quit")
				term.setCursorPos((w/2)-8,16)
				write("Fire when ready")
			elseif screenStage>22.1 and screenStage <27 then
				introHighScoreTable()
			elseif screenStage>27 then	
				term.setBackgroundColour(colours.black)
				for i = 2,h-1 do
					term.setCursorPos(1,i)
					write(string.rep(" ",w-1))
				end
				screenStage=0
			end
			
			screenStage=screenStage+0.1
			screenTimer=os.startTimer(0.025)
		end
	end
end

w,h =term.getSize()
if term.isColour() and w==51 then
	initVariables()
	startScreen()
else
	term.clear()
	term.setCursorPos(1,1)
	print("I'm sorry, Protector requires an Advanced Computer to run")
	print(" ")
end