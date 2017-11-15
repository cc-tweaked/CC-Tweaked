-- Definition for the IO API

local g_defaultInput = {
	bFileHandle = true,
	bClosed = false,
	close = function( self )
	end,
	read = function( self, _sFormat )
		if _sFormat and _sFormat ~= "*l" then
			error( "Unsupported format" )
		end
		return _G.read()
	end,
	lines = function( self )
		return function()
			return _G.read()
		end
	end,
}

local g_defaultOutput = {
	bFileHandle = true,
	bClosed = false,
	close = function( self )
	end,
	write = function( self, ... )
        local nLimit = select("#", ... )
        for n = 1, nLimit do
            _G.write( select( n, ... ) )
        end
	end,
	flush = function( self )
	end,
}

local g_currentInput = g_defaultInput
local g_currentOutput = g_defaultOutput

function close( _file )
	(_file or g_currentOutput):close()
end

function flush()
	g_currentOutput:flush()
end

function input( _arg )
	if _G.type( _arg ) == "string" then
		g_currentInput = open( _arg, "r" )
	elseif _G.type( _arg ) == "table" then
		g_currentInput = _arg
	elseif _G.type( _arg ) == "nil" then
		return g_currentInput
	else
		error( "bad argument #1 (expected string/table/nil, got " .. _G.type( _arg ) .. ")", 2 )
	end
end

function lines( _sFileName )
    if _G.type( _sFileName ) ~= "string" then
        error( "bad argument #1 (expected string, got " .. _G.type( _sFileName ) .. ")", 2 )
    end
	if _sFileName then
		return open( _sFileName, "r" ):lines()
	else
		return g_currentInput:lines()
	end
end

function open( _sPath, _sMode )
    if _G.type( _sPath ) ~= "string" then
        error( "bad argument #1 (expected string, got " .. _G.type( _sPath ) .. ")", 2 )
    end
    if _sMode ~= nil and _G.type( _sMode ) ~= "string" then
        error( "bad argument #2 (expected string, got " .. _G.type( _sMode ) .. ")", 2 )
    end
	local sMode = _sMode or "r"
	local file, err = fs.open( _sPath, sMode )
	if not file then
		return nil, err
	end
	
	if sMode == "r"then
		return {
			bFileHandle = true,
			bClosed = false,				
			close = function( self )
				file.close()
				self.bClosed = true
			end,
			read = function( self, _sFormat )
				local sFormat = _sFormat or "*l"
				if sFormat == "*l" then
					return file.readLine()
				elseif sFormat == "*a" then
					return file.readAll()
                elseif _G.type( sFormat ) == "number" then
                    return file.read( sFormat )
				else
					error( "Unsupported format", 2 )
				end
				return nil
			end,
			lines = function( self )
				return function()
					local sLine = file.readLine()
					if sLine == nil then
						file.close()
						self.bClosed = true
					end
					return sLine
				end
			end,
		}
	elseif sMode == "w" or sMode == "a" then
		return {
			bFileHandle = true,
			bClosed = false,				
			close = function( self )
				file.close()
				self.bClosed = true
			end,
			write = function( self, ... )
                local nLimit = select("#", ... )
                for n = 1, nLimit do
				    file.write( select( n, ... ) )
                end
			end,
			flush = function( self )
				file.flush()
			end,
		}
	
	elseif sMode == "rb" then
		return {
			bFileHandle = true,
			bClosed = false,				
			close = function( self )
				file.close()
				self.bClosed = true
			end,
			read = function( self )
				return file.read()
			end,
		}
		
	elseif sMode == "wb" or sMode == "ab" then
		return {
			bFileHandle = true,
			bClosed = false,				
			close = function( self )
				file.close()
				self.bClosed = true
			end,
			write = function( self, ... )
                local nLimit = select("#", ... )
                for n = 1, nLimit do
				    file.write( select( n, ... ) )
                end
			end,
			flush = function( self )
				file.flush()
			end,
		}
	
	else
		file.close()
		error( "Unsupported mode", 2 )
		
	end
end

function output( _arg )
	if _G.type( _arg ) == "string" then
		g_currentOutput = open( _arg, "w" )
	elseif _G.type( _arg ) == "table" then
		g_currentOutput = _arg
	elseif _G.type( _arg ) == "nil" then
		return g_currentOutput
	else
		error( "bad argument #1 (expected string/table/nil, got " .. _G.type( _arg ) .. ")", 2 )
	end
end

function read( ... )
	return input():read( ... )
end

function type( _handle )
	if _G.type( _handle ) == "table" and _handle.bFileHandle == true then
		if _handle.bClosed then
			return "closed file"
		else
			return "file"
		end
	end
	return nil
end

function write( ... )
	return output():write( ... )
end
