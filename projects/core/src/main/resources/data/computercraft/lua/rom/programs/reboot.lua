if term.isColour() then
    term.setTextColour(colours.yellow)
end
print("Goodbye")
term.setTextColour(colours.white)

sleep(1)
os.reboot()
