package org.codehaus.plexus.compiler.util;

/**
 * The MIT License
 *
 * Copyright (c) 2005, The Codehaus
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class StreamPumper
    extends Thread
{
    private static final int BUFFER_SIZE = 512;

    private BufferedInputStream stream;

    private boolean endOfStream = false;

    private int SLEEP_TIME = 5;

    private OutputStream out;

    public StreamPumper( BufferedInputStream is, OutputStream out )
    {
        this.stream = is;
        this.out = out;
    }

    public void pumpStream()
        throws IOException
    {
        byte[] buf = new byte[BUFFER_SIZE];
        if ( !endOfStream )
        {
            int bytesRead = stream.read( buf, 0, BUFFER_SIZE );

            if ( bytesRead > 0 )
            {
                out.write( buf, 0, bytesRead );
            }
            else if ( bytesRead == -1 )
            {
                endOfStream = true;
            }
        }
    }

    public void run()
    {
        try
        {
            while ( !endOfStream )
            {
                pumpStream();
                sleep( SLEEP_TIME );
            }
        }
        catch ( Exception e )
        {
            // getLogger().warn("Jikes.run()", e);
        }
    }
}
