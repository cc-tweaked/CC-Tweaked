/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.ingame.mod;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

public final class ImageUtils
{
    private static final Logger LOG = LogManager.getLogger( ImageUtils.class );

    /**
     * Allow 0.3% of pixels to fail. This allows for slight differences at the edges.
     */
    private static final double PIXEL_THRESHOLD = 0.003;

    /**
     * Maximum possible distance between two colours. Floating point differences means we need some fuzziness here.
     */
    public static final int DISTANCE_THRESHOLD = 5;

    private ImageUtils()
    {
    }

    public static boolean areSame( BufferedImage left, BufferedImage right )
    {
        int width = left.getWidth(), height = left.getHeight();
        if( width != right.getWidth() || height != right.getHeight() ) return false;

        int failed = 0, threshold = (int) (width * height * PIXEL_THRESHOLD);
        for( int x = 0; x < width; x++ )
        {
            for( int y = 0; y < height; y++ )
            {
                int l = left.getRGB( x, y ), r = right.getRGB( x, y );
                if( (l & 0xFFFFFF) != (r & 0xFFFFFF) && distance( l, r, 0 ) + distance( l, r, 8 ) + distance( l, r, 16 ) >= DISTANCE_THRESHOLD )
                {
                    failed++;
                }
            }
        }

        if( failed > 0 ) LOG.warn( "{} pixels failed comparing (threshold is {})", failed, threshold );
        return failed <= threshold;
    }

    public static void writeDifference( Path path, BufferedImage left, BufferedImage right ) throws IOException
    {
        int width = left.getWidth(), height = left.getHeight();

        BufferedImage copy = new BufferedImage( width, height, left.getType() );
        for( int x = 0; x < width; x++ )
        {
            for( int y = 0; y < height; y++ )
            {
                int l = left.getRGB( x, y ), r = right.getRGB( x, y );
                copy.setRGB( x, y, difference( l, r, 0 ) | difference( l, r, 8 ) | difference( l, r, 16 ) | 0xFF000000 );
            }
        }

        ImageIO.write( copy, "png", path.toFile() );
    }

    private static int difference( int l, int r, int shift )
    {
        return Math.abs( ((l >> shift) & 0xFF) - ((r >> shift) & 0xFF) ) << shift;
    }

    private static int distance( int l, int r, int shift )
    {
        return Math.abs( ((l >> shift) & 0xFF) - ((r >> shift) & 0xFF) );
    }
}
