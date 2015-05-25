package org.codehaus.plexus.compiler.csharp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class JarUtil {
    public static void extract( File destDir, File jarFile ) throws IOException
    {
        JarFile jar = new JarFile( jarFile );
        Enumeration enumEntries = jar.entries();
        while ( enumEntries.hasMoreElements() ) {
            JarEntry file = ( JarEntry ) enumEntries.nextElement();
            File f = new File( destDir + File.separator + file.getName() );
            if ( file.isDirectory() )
            {
                f.mkdir();
                continue;
            }
            InputStream is = jar.getInputStream( file );
            FileOutputStream fos = new FileOutputStream( f );
            try
            {
                while ( is.available() > 0 )
                {
                    fos.write( is.read() );
                }
            }
            finally
            {
                is.close();
                fos.close();
            }
        }
    }
}
