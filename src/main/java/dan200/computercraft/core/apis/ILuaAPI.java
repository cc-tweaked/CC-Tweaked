package dan200.computercraft.core.apis;

/**
 * This exists purely to ensure binary compatibility.
 *
 * @see dan200.computercraft.api.lua.ILuaAPI
 */
@Deprecated
public interface ILuaAPI extends dan200.computercraft.api.lua.ILuaAPI
{
    void advance( double v );

    default void update()
    {
        advance( 0.05 );
    }
}
