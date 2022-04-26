/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.terminal;

import dan200.computercraft.ContramapMatcher;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import java.util.Arrays;

public class TerminalMatchers
{
    public static Matcher<Terminal> textColourMatches( String[] x )
    {
        return linesMatch( "text colour", Terminal::getTextColourLine, x );
    }

    public static Matcher<Terminal> backgroundColourMatches( String[] x )
    {
        return linesMatch( "background colour", Terminal::getBackgroundColourLine, x );
    }

    public static Matcher<Terminal> textMatches( String[] x )
    {
        return linesMatch( "text", Terminal::getLine, x );
    }

    @SuppressWarnings( "unchecked" )
    public static Matcher<Terminal> linesMatch( String kind, LineProvider getLine, String[] lines )
    {
        return linesMatchWith( kind, getLine, Arrays.stream( lines ).map( Matchers::equalTo ).toArray( Matcher[]::new ) );
    }

    public static Matcher<Terminal> linesMatchWith( String kind, LineProvider getLine, Matcher<String>[] lines )
    {
        return new ContramapMatcher<>( kind, terminal -> {
            String[] termLines = new String[terminal.getHeight()];
            for( int i = 0; i < termLines.length; i++ ) termLines[i] = getLine.getLine( terminal, i ).toString();
            return termLines;
        }, Matchers.array( lines ) );
    }

    @FunctionalInterface
    public interface LineProvider
    {
        TextBuffer getLine( Terminal terminal, int line );
    }

}
