local ok, err = pocket.equipBack()
if not ok then
    printError( err )
else
    print( "Item equipped" )
end
