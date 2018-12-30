local ok, err = pcall( pocket.unequipBack )
if not ok then
    printError( "Nothing to unequip" )
else
    print( "Item unequipped" )
end
