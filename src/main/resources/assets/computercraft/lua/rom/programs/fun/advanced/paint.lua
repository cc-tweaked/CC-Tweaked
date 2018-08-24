-- Paint created by nitrogenfingers (edited by dan200)
-- Rewritten & optimized by magiczocker
-- http://www.youtube.com/user/NitrogenFingers
-- variables
local w,h=term.getSize()
local black_white=(term.isColor and not term.isColor()) or false
local can_edit=true
local menu_message="Press Ctrl to access menu"
local menu=false
local menu_selected=1
local running=true
local save_image
-- tables
local arg={...}
local color={1,black_white and 1 or nil}
local cursor={x=1,y=1}
local get_code={}
local get_color={}
local image={}
local menu_items={{txt="Save",func=function() if save_image() then menu_message="Saved to "..arg[1] else menu_message="Error saving to "..arg[1] end end},{txt="Exit",func=function() running=false end}}
local order={
    {1,32768},
    {256,128},
    {128,256},
    {32768,1}
}
-- functions
for i=1,16 do
    get_code[2^(i-1)]=string.sub("0123456789abcdef",i,i)
end
for i=1,16 do
    get_color[string.sub("0123456789abcdef",i,i)]=2^(i-1)
end
local function draw_colors(_)
    if black_white then
        if not _ then
            for i=1,4 do
                term.setCursorPos(w-1,i)
                term.setBackgroundColor(order[i][1])
                term.setTextColor(order[i][2])
                term.write(i.." ")
            end
            term.setCursorPos(w-1,5)
            term.setTextColor(1)
            term.write"5\127"
        end
        term.setCursorPos(w-1,6)
        term.setTextColor(128)
        term.setBackgroundColor(color[1] or 32768)
        term.write(color[1] and " " or "\127")
        term.setBackgroundColor(color[2] or 32768)
        term.write(color[2] and " " or "\127")
    else
        term.setTextColor(128)
        if not _ then
            for i=1,16 do
                term.setBackgroundColor(2^(i-1))
                term.setCursorPos(w-1,i)
                term.write"  "
            end
            term.setCursorPos(w-1,17)
            term.write"\127\127"
        end
        term.setCursorPos(w-1,18)
        term.setBackgroundColor(color[1] or 32768)
        term.write(color[1] and " " or "\127")
        term.setBackgroundColor(color[2] or 32768)
        term.write(color[2] and " " or "\127")
        if not _ then
            term.setBackgroundColor(32768)
            for i=19,h-1 do
                term.setCursorPos(w-1,i)
                term.write"  "
            end
        end
    end
end
local function draw_image()
    for i=1,h-1 do
        image[i]=image[i] or {}
        term.setCursorPos(1,i)
        for j=1,w-2 do
            term.setBackgroundColor(image[i][j] or 32768)
            if black_white and image[i][j]==128 then
                term.setTextColor(256)
            else
                term.setTextColor(128)
            end
            if black_white and i==cursor.y and j==cursor.x then
                term.write"x"
            else
                term.write(image[i][j] and " " or "\127")
            end
        end
    end
end
local function draw_menu()
    term.setBackgroundColor(32768)
    term.setCursorPos(1,h)
    if menu then
        for i=1,#menu_items do
            if i==menu_selected then
                term.setTextColor(black_white and 1 or 16)
                term.write"["
                term.setTextColor(1)
                term.write(menu_items[i].txt)
                term.setTextColor(black_white and 1 or 16)
                term.write"]"
            else
                term.setTextColor(1)
                term.write(" "..menu_items[i].txt.." ")
            end
        end
    else
        term.setTextColor(black_white and 1 or 16)
        term.write(menu_message..string.rep(" ",w))
    end
    term.write(string.rep(" ",w))
    term.setTextColor(128)
end
local function load_image()
    if fs.exists(arg[1]) then
        local count=1
        local file=fs.open(arg[1],"r")
        image={}
        local line=file.readLine()
        while line do
            image[count]=image[count]or{}
            for i=1,w-2 do
                image[count][i]=get_color[string.sub(line,i,i)]
                if black_white and (image[count][i]==1 or image[count][i]==128 or image[count][i]==256 or image[count][i]==32768 or image[count][i]==nil) then
                elseif not black_white then
                else
                    file.close()
                    error("Image includes color.",0)
                    --return
                end
            end
            line=file.readLine()
            count=count+1
        end
        file.close()
    end
end
function save_image()
    local file=fs.open(arg[1],"w")
    if file then
        local content={}
        for i=1,h-1 do
            content[i]=""
            for j=1,w-2 do
                content[i]=content[i]..(get_code[image[i][j]] or " ")
            end
            while string.sub(content[i],#content[i],#content[i])==" " do
                content[i]=string.sub(content[i],1,#content[i]-1)
            end
        end
        for i=#content,1,-1 do
            if content[i]=="" then
                content[i]=nil
            else
                break
            end
        end
        for i=1,#content do
            file.write(content[i])
            if i<#content then
                file.write("\n")
            end
        end
        file.close()
        return true
    else
        return false
    end
end
-- start
if #arg==0 then
    print("Usage: paint <path>")
    return
elseif fs.exists(arg[1]) and fs.isDir(arg[1]) then
    print("Cannot edit a directory.")
    return
elseif settings and not fs.exists(arg[1]) and not string.find(arg[1],"%.") then
    local extension=settings.get("paint.default_extension","")
    if extension~="" and type(extension)=="string" then
        arg[1]=arg[1].."."..extension
    end
end
if fs.isReadOnly(arg[1]) then
    can_edit=false
    menu_items={{txt="Exit",func=function() running=false end}}
end
load_image()
draw_image()
draw_colors()
draw_menu()
-- events
repeat
    local e,d,x,y=coroutine.yield()
    if (e=="mouse_click" or e=="mouse_drag") and y<h and not menu and can_edit then
        if not black_white and x>w-2 and y<18 and e=="mouse_click" then -- color palette
            color[d]=(y<17 and 2^(y-1)) or nil
            draw_colors(true)
        elseif black_white and x>w-2 and y<6 and e=="mouse_click" then -- color palette
            color[d]=(y<5 and order[y][1] or nil)
            draw_colors(true)
        elseif x<w-1 then -- canvas
            image[y][x]=color[d]
            term.setCursorPos(x,y)
            term.setBackgroundColor(image[y][x] or 32768)
            term.write(image[y][x] and " " or "\127")
        end
    elseif e=="key" then
        if d>1 and d<7 and black_white and not menu then
            color[1]=(d<6 and order[d-1][1]) or nil
            color[2]=(d<6 and order[d-1][1]) or nil
            draw_colors(true)
        elseif d==28 then -- enter
            if menu then
                menu_items[menu_selected].func()
                menu=false
                draw_menu()
            elseif black_white then
                image[cursor.y][cursor.x]=color[1]
                term.setCursorPos(cursor.x,cursor.y)
                term.setBackgroundColor(image[cursor.y][cursor.x] or 32768)
                if color[1]==128 then
                    term.setTextColor(256)
                end
                term.write("x")
            end
        elseif d==29 then -- left ctrl
            menu=not menu
            draw_menu()
        elseif d==200 and black_white and not menu then -- up
            if cursor.y>1 then
                cursor.y=cursor.y-1
                draw_image()
            end
        elseif d==203 then -- left
            if menu then
                menu_selected=menu_selected>1 and menu_selected-1 or #menu_items
                draw_menu()
            elseif black_white and cursor.x>1 then
                cursor.x=cursor.x-1
                draw_image()
            end
        elseif d==205 then -- right
            if menu then
                menu_selected=menu_selected<#menu_items and menu_selected+1 or 1
                draw_menu()
            elseif black_white and cursor.x<w-2 then
                cursor.x=cursor.x+1
                draw_image()
            end
        elseif d==208 and black_white and not menu then -- down
            if cursor.y<h-1 then
                cursor.y=cursor.y+1
                draw_image()
            end
        end
    elseif e=="term_resize" then
        w,h=term.getSize()
        if cursor.x>w-2 then
            cursor.x=w-2
        end
        if cursor.y>h-1 then
            cursor.y=h-1
        end
        draw_image()
        draw_colors()
        draw_menu()
    end
until not running
term.setBackgroundColor(32768)
term.setTextColor(1)
term.clear()
term.setCursorPos(1,1)
