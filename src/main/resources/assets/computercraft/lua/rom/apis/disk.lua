
local function isDrive( name )
    if type( name ) ~= "string" then
        error( "bad argument #1 (expected string, got " .. type( name ) .. ")", 3 )
    end
    return peripheral.getType( name ) == "drive"
end

function isPresent( name )
    if isDrive( name ) then
        return peripheral.call( name, "isDiskPresent" )
    end
    return false
end

function getLabel( name )
    if isDrive( name ) then
        return peripheral.call( name, "getDiskLabel" )
    end
    return nil
end

function setLabel( name, label )
    if isDrive( name ) then
        peripheral.call( name, "setDiskLabel", label )
    end
end

function hasData( name )
    if isDrive( name ) then
        return peripheral.call( name, "hasData" )
    end
    return false
end

function getMountPath( name )
    if isDrive( name ) then
        return peripheral.call( name, "getMountPath" )
    end
    return nil
end

function hasAudio( name )
    if isDrive( name ) then
        return peripheral.call( name, "hasAudio" )
    end
    return false
end

function getAudioTitle( name )
    if isDrive( name ) then
        return peripheral.call( name, "getAudioTitle" )
    end
    return nil
end

function playAudio( name )
    if isDrive( name ) then
        peripheral.call( name, "playAudio" )
    end
end

function stopAudio( name )
    if not name then
        for _, sName in ipairs( peripheral.getNames() ) do
            stopAudio( sName )
        end
    else
        if isDrive( name ) then
            peripheral.call( name, "stopAudio" )
        end
    end
end

function eject( name )
    if isDrive( name ) then
        peripheral.call( name, "ejectDisk" )
    end
end

function getID( name )
    if isDrive( name ) then
        return peripheral.call( name, "getDiskID" )
    end
    return nil
end

