package dan200.computercraft.core.lua.waluaigi;

class UnreachableException extends IllegalStateException
{
    public UnreachableException()
    {
        super( "Unreachable code" );
    }
}
