local ok, err = pcall( pocket.equipBack )
if not ok then
    printError( "Nothing to equip" )
else
    print( "Item equipped" )
end