package org.codehaus.plexus.compiler.csharp;

import org.codehaus.plexus.compiler.CompilerError;
import org.codehaus.plexus.util.StringUtils;

/**
 * Handles output from both mono with only the line number
 * <p/>
 * ex error = "/home/trygvis/dev/com.myrealbox/trunk/mcs/nunit20/core/./TestRunnerThread.cs(29) error CS0246: Cannot find type 'NameValueCollection'"
 * <p/>
 * and errors from mono & csc on windows which has column num also
 * <p/>
 * ex error = "src\\test\\csharp\\Hierarchy\\Logger.cs(98,4): warning CS0618: 'NUnit.Framework.Assertion' is obsolete: 'Use Assert class instead'";
 *
 * @author <a href="mailto:gdodinet@karmicsoft.com">Gilles Dodinet</a>
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @author <a href="mailto:matthew.pocock@ncl.ac.uk">Matthew Pocock</a>
 * @author <a href="mailto:chris.stevenson@gmail.com">Chris Stevenson</a>
 */
public class DefaultCSharpCompilerParser
{

    private static String ERROR_PREFIX = "error ";

    private static String COMPILATION_PREFIX = "Compilation ";

    private static String MAGIC_LINE_MARKER = ".cs(";

    private static String MAGIC_LINE_MARKER_2 = ")";


    public static CompilerError parseLine( String line )
    {
        CompilerError ce = null;

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

    private static CompilerError parseLineWithNoColumnNumber( String line )
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
        else if ( line.startsWith( COMPILATION_PREFIX ) )
        {
            // ignore

            return null;
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

        return new CompilerError( file, error, startline, startcolumn, endline, endcolumn, message );

    }

    private static CompilerError parseLineWithColumnNumberAndLineNumber( String line )
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
        else if ( line.startsWith( COMPILATION_PREFIX ) )
        {
            return null;
        }
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

        return new CompilerError( file, error, startline, startcolumn, endline, endcolumn, message );
    }

}
