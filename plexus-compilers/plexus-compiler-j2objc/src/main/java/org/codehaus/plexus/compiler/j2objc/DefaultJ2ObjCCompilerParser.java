package org.codehaus.plexus.compiler.j2objc;

import org.codehaus.plexus.compiler.CompilerMessage;
import org.codehaus.plexus.compiler.CompilerMessage.Kind;
import org.codehaus.plexus.util.StringUtils;

/**
 * Handle the output of J2ObjC
 * @author lmaitre
 */
public class DefaultJ2ObjCCompilerParser
{

    private static String ERROR_PREFIX = "error ";

    private static String CONVERT_PREFIX = "translating ";
    
    private static String TRANSLATION_PREFIX = "Translated ";

    private static String MAGIC_LINE_MARKER = ".cs(";

    private static String MAGIC_LINE_MARKER_2 = ")";

/**
Unknown output: translating /Users/lmaitre/Workspaces/IOSSamples/maven-quickstart-j2objc/src/main/java/de/test/App.java
Unknown output: Translated 1 file: 0 errors, 0 warnings
Unknown output: Translated 2 methods as functions
 * @param line
 * @return
 */
    public static CompilerMessage parseLine( String line )
    {
        CompilerMessage ce = null;

        if ( isOutputWithNoColumnNumber( line ) )
        {
            ce = parseLineWithNoColumnNumber( line );
        }
        else
        {
            ce = parseLineWithColumnNumberAndLineNumber( line );
        }

        return ce;
    }

    private static boolean isOutputWithNoColumnNumber( String line )
    {

        int i = line.indexOf( MAGIC_LINE_MARKER );

        if ( i == -1 )
        {
            return true;
        }

        String chunk1 = line.substring( i + MAGIC_LINE_MARKER.length() );

        int j = chunk1.indexOf( MAGIC_LINE_MARKER_2 );

        String chunk2 = chunk1.substring( 0, j );

        return ( chunk2.indexOf( "," ) == -1 );
    }

    private static CompilerMessage parseLineWithNoColumnNumber( String line )
    {

        String file = null;
        boolean error = true;
        int startline = -1;
        int startcolumn = -1;
        int endline = -1;
        int endcolumn = -1;
        String message;

        if ( line.startsWith( ERROR_PREFIX ) )
        {
            message = line.substring( ERROR_PREFIX.length() );
        }
        else if ( line.startsWith( CONVERT_PREFIX ) )
        {
        	message = line;
        } else if ( line.startsWith( TRANSLATION_PREFIX ) )
        {
        	message = line;
        }
        else if ( line.indexOf( MAGIC_LINE_MARKER ) != -1 )
        {
            int i = line.indexOf( MAGIC_LINE_MARKER );

            int j = line.indexOf( ' ', i );

            file = line.substring( 0, i + 3 );

            String num = line.substring( i + MAGIC_LINE_MARKER.length(), j - 1 );

            startline = Integer.parseInt( num );

            endline = startline;

            message = line.substring( j + 1 + ERROR_PREFIX.length() );

            error = line.indexOf( ") error" ) != -1;
        }
        else
        {
            System.err.println( "Unknown output: " + line );

            return null;
        }

        return new CompilerMessage( file, error ? Kind.ERROR : Kind.NOTE, startline, startcolumn, endline, endcolumn, message );

    }

    private static CompilerMessage parseLineWithColumnNumberAndLineNumber( String line )
    {

        String file = null;
        boolean error = true;
        int startline = -1;
        int startcolumn = -1;
        int endline = -1;
        int endcolumn = -1;
        String message;

        if ( line.startsWith( ERROR_PREFIX ) )
        {
            message = line.substring( ERROR_PREFIX.length() );
        }/*
        else if ( line.startsWith( COMPILATION_PREFIX ) )
        {
            return null;
        }*/
        else if ( line.indexOf( MAGIC_LINE_MARKER ) != -1 )
        {
            int i = line.indexOf( MAGIC_LINE_MARKER );

            int j = line.indexOf( ' ', i );

            file = line.substring( 0, i + 3 );

            String linecol = line.substring( i + MAGIC_LINE_MARKER.length(), j - 2 );

            String linenum = null;
            String colnum = null;

            if ( linecol.indexOf( "," ) > -1 && linecol.split( "," ).length == 2 )
            {
                linenum = linecol.split( "," )[0];
                colnum = linecol.split( "," )[1];
            }
            else if ( linecol.split( "," ).length == 1 )
            {
                linenum = linecol.split( "," )[0];
                colnum = "-1";
            }
            else
            {
                linenum = linecol.trim();
                colnum = "-1";
            }

            startline = StringUtils.isEmpty( linenum ) ? -1 : Integer.parseInt( linenum );

            startcolumn = StringUtils.isEmpty( colnum ) ? -1 : Integer.parseInt( colnum );

            endline = startline;

            endcolumn = startcolumn;

            message = line.substring( j + 1 + ERROR_PREFIX.length() );

            error = line.indexOf( "): error" ) != -1;
        }
        else
        {
            System.err.println( "Unknown output: " + line );

            return null;
        }

        return new CompilerMessage( file, error ? Kind.ERROR : Kind.NOTE, startline, startcolumn, endline, endcolumn, message );
    }

}
