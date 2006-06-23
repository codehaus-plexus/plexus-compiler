package org.codehaus.plexus.compiler.eclipse;

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

import org.codehaus.plexus.compiler.AbstractCompiler;
import org.codehaus.plexus.compiler.CompilerConfiguration;
import org.codehaus.plexus.compiler.CompilerError;
import org.codehaus.plexus.compiler.CompilerException;
import org.codehaus.plexus.compiler.CompilerOutputStyle;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.internal.compiler.ClassFile;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.Compiler;
import org.eclipse.jdt.internal.compiler.DefaultErrorHandlingPolicies;
import org.eclipse.jdt.internal.compiler.ICompilerRequestor;
import org.eclipse.jdt.internal.compiler.IErrorHandlingPolicy;
import org.eclipse.jdt.internal.compiler.IProblemFactory;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFormatException;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.jdt.internal.compiler.env.INameEnvironment;
import org.eclipse.jdt.internal.compiler.env.NameEnvironmentAnswer;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblemFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * @plexus.component
 *   role="org.codehaus.plexus.compiler.Compiler"
 *   role-hint="eclipse"
 */
public class EclipseJavaCompiler
    extends AbstractCompiler
{
    public EclipseJavaCompiler()
    {
        super( CompilerOutputStyle.ONE_OUTPUT_FILE_PER_INPUT_FILE,
               ".java",
               ".class",
               null );
    }

    // ----------------------------------------------------------------------
    // Compiler Implementation
    // ----------------------------------------------------------------------

    public List compile( CompilerConfiguration config )
        throws CompilerException
    {
        List errors = new LinkedList();

        List classpathEntries = config.getClasspathEntries();

        URL[] urls = new URL[ 1 + classpathEntries.size() ];

        int i = 0;

        try
        {
            urls[ i++ ] = new File( config.getOutputLocation() ).toURL();

            for ( Iterator it = classpathEntries.iterator(); it.hasNext(); )
            {
                urls[ i++ ] = new File( (String) it.next() ).toURL();
            }
        }
        catch ( MalformedURLException e )
        {
            throw new CompilerException( "Error while converting the classpath entries to URLs.", e );
        }

        ClassLoader classLoader = new URLClassLoader( urls );

        SourceCodeLocator sourceCodeLocator = new SourceCodeLocator( config.getSourceLocations() );

        INameEnvironment env = new EclipseCompilerINameEnvironment( sourceCodeLocator,
                                                                    classLoader,
                                                                    errors );

        IErrorHandlingPolicy policy = DefaultErrorHandlingPolicies.proceedWithAllProblems();

        // ----------------------------------------------------------------------
        // Build settings from configuration
        // ----------------------------------------------------------------------

        Map settings = new HashMap();

        if ( config.isDebug() )
        {
            settings.put( CompilerOptions.OPTION_LocalVariableAttribute, CompilerOptions.GENERATE );
        }

        if ( config.isShowWarnings() )
        {
            // TODO: Implement. I'm not sure what value to pass - trygve
//            settings.put( CompilerOptions.OPTION_SuppressWarnings,  );
        }

        String sourceVersion = decodeVersion( config.getSourceVersion() );

        if ( sourceVersion != null )
        {
            settings.put( CompilerOptions.OPTION_Source, sourceVersion );
        }

        String targetVersion = decodeVersion( config.getTargetVersion() );

        if ( targetVersion != null )
        {
            settings.put( CompilerOptions.OPTION_TargetPlatform, targetVersion );
        }

        if ( !StringUtils.isEmpty( config.getSourceEncoding() ) )
        {
            settings.put( CompilerOptions.OPTION_Encoding, config.getSourceEncoding() );
        }

        if ( config.isShowDeprecation() )
        {
            settings.put( CompilerOptions.OPTION_ReportDeprecation, CompilerOptions.WARNING );
        }
        else
        {
            settings.put( CompilerOptions.OPTION_ReportDeprecation, CompilerOptions.IGNORE );
        }

        // ----------------------------------------------------------------------
        // Set Eclipse-specific options
        // ----------------------------------------------------------------------

        settings.put( CompilerOptions.OPTION_LineNumberAttribute, CompilerOptions.GENERATE );

        settings.put( CompilerOptions.OPTION_SourceFileAttribute, CompilerOptions.GENERATE );

        IProblemFactory problemFactory = new DefaultProblemFactory( Locale.getDefault() );

        ICompilerRequestor requestor = new EclipseCompilerICompilerRequestor( config.getOutputLocation(),
                                                                              errors );

        List compilationUnits = new ArrayList();

        for ( Iterator it = config.getSourceLocations().iterator(); it.hasNext(); )
        {
            String sourceRoot = (String) it.next();

            Set sources = getSourceFilesForSourceRoot( config, sourceRoot );

            for ( Iterator it2 = sources.iterator(); it2.hasNext(); )
            {
                String source = (String) it2.next();

                CompilationUnit unit = new CompilationUnit( source,
                                                            makeClassName( source, sourceRoot ),
                                                            errors );

                compilationUnits.add( unit );
            }
        }

        // ----------------------------------------------------------------------
        // Compile!
        // ----------------------------------------------------------------------

        Compiler compiler = new Compiler( env, policy, settings, requestor, problemFactory );

        ICompilationUnit[] units = (ICompilationUnit[])
            compilationUnits.toArray( new ICompilationUnit[ compilationUnits.size() ] );

        compiler.compile( units );

        return errors;
    }

    public String[] createCommandLine( CompilerConfiguration config )
            throws CompilerException
    {
        return null;
    }

    private CompilerError handleError( String className, int line, int column, Object errorMessage )
    {
        String fileName = className.replace( '.', File.separatorChar ) + ".java";

        if ( column < 0 )
        {
            column = 0;
        }

        String message;

        if ( errorMessage != null )
        {
            message = errorMessage.toString();
        }
        else
        {
            message = "No message";
        }

        return new CompilerError( fileName,
                                  true,
                                  line,
                                  column,
                                  line,
                                  column,
                                  message );
    }

    private CompilerError handleWarning( IProblem warning )
    {
        return new CompilerError( new String( warning.getOriginatingFileName() ),
                                  false,
                                  warning.getSourceLineNumber(),
                                  warning.getSourceStart(),
                                  warning.getSourceLineNumber(),
                                  warning.getSourceEnd(),
                                  warning.getMessage() );
    }

    private String decodeVersion( String versionSpec )
    {
        if ( StringUtils.isEmpty( versionSpec ) )
        {
            return null;
        }
        else if ( versionSpec.equals( "1.1" ) )
        {
            return CompilerOptions.VERSION_1_1;
        }
        else if ( versionSpec.equals( "1.2" ) )
        {
            return CompilerOptions.VERSION_1_2;
        }
        else if ( versionSpec.equals( "1.3" ) )
        {
            return CompilerOptions.VERSION_1_3;
        }
        else if ( versionSpec.equals( "1.4" ) )
        {
            return CompilerOptions.VERSION_1_4;
        }
        else if ( versionSpec.equals( "1.5" ) )
        {
            return CompilerOptions.VERSION_1_5;
        }
        else
        {
            getLogger().warn( "Unknown version '" + versionSpec + "', no version setting will be given to the compiler." );

            return null;
        }
    }

    // ----------------------------------------------------------------------
    // Classes
    // ----------------------------------------------------------------------

    private class CompilationUnit
        implements ICompilationUnit
    {
        private String className;

        private String sourceFile;

        private List errors;

        CompilationUnit( String sourceFile,
                         String className,
                         List errors )
        {
            this.className = className;
            this.sourceFile = sourceFile;
            this.errors = errors;
        }

        public char[] getFileName()
        {
            return className.toCharArray();
        }

        public char[] getContents()
        {
            try
            {
                return FileUtils.fileRead( sourceFile ).toCharArray();
            }
            catch ( FileNotFoundException e )
            {
                errors.add( handleError( className, -1, -1, e.getMessage() ) );

                return null;
            }
            catch ( IOException e )
            {
                errors.add( handleError( className, -1, -1, e.getMessage() ) );

                return null;
            }
        }

        public char[] getMainTypeName()
        {
            int dot = className.lastIndexOf( '.' );

            if ( dot > 0 )
            {
                return className.substring( dot + 1 ).toCharArray();
            }

            return className.toCharArray();
        }

        public char[][] getPackageName()
        {
            StringTokenizer izer = new StringTokenizer( className, "." );

            char[][] result = new char[izer.countTokens() - 1][];

            for ( int i = 0; i < result.length; i++ )
            {
                String tok = izer.nextToken();

                result[ i ] = tok.toCharArray();
            }

            return result;
        }
    }

    private class EclipseCompilerINameEnvironment
        implements INameEnvironment
    {
        private SourceCodeLocator sourceCodeLocator;

        private ClassLoader classLoader;

        private List errors;

        public EclipseCompilerINameEnvironment( SourceCodeLocator sourceCodeLocator,
                                                ClassLoader classLoader,
                                                List errors )
        {
            this.sourceCodeLocator = sourceCodeLocator;
            this.classLoader = classLoader;
            this.errors = errors;
        }

        public NameEnvironmentAnswer findType( char[][] compoundTypeName )
        {
            String result = "";

            String sep = "";

            for ( int i = 0; i < compoundTypeName.length; i++ )
            {
                result += sep;
                result += new String( compoundTypeName[ i ] );
                sep = ".";
            }

            return findType( result );
        }

        public NameEnvironmentAnswer findType( char[] typeName, char[][] packageName )
        {
            String result = "";

            String sep = "";

            for ( int i = 0; i < packageName.length; i++ )
            {
                result += sep;
                result += new String( packageName[ i ] );
                sep = ".";
            }

            result += sep;
            result += new String( typeName );
            return findType( result );
        }

        private NameEnvironmentAnswer findType( String className )
        {
            try
            {
                File f = sourceCodeLocator.findSourceCodeForClass( className );

                if ( f != null )
                {
                    ICompilationUnit compilationUnit = new CompilationUnit( f.getAbsolutePath(),
                                                                            className,
                                                                            errors );

                    return new NameEnvironmentAnswer( compilationUnit, null );
                }

                String resourceName = className.replace( '.', '/' ) + ".class";

                InputStream is = classLoader.getResourceAsStream( resourceName );

                if ( is == null )
                {
                    return null;
                }

                byte[] classBytes = IOUtil.toByteArray( is );

                char[] fileName = className.toCharArray();

                ClassFileReader classFileReader = new ClassFileReader( classBytes, fileName, true );

                return new NameEnvironmentAnswer( classFileReader, null );
            }
            catch ( IOException e )
            {
                errors.add( handleError( className, -1, -1, e.getMessage() ) );

                return null;
            }
            catch ( ClassFormatException e )
            {
                errors.add( handleError( className, -1, -1, e.getMessage() ) );

                return null;
            }
        }

        private boolean isPackage( String result )
        {
            if ( sourceCodeLocator.findSourceCodeForClass( result ) != null )
            {
                return false;
            }

            String resourceName = "/" + result.replace( '.', '/' ) + ".class";

            InputStream is = classLoader.getResourceAsStream( resourceName );

            return is == null;
        }

        public boolean isPackage( char[][] parentPackageName,
                                  char[] packageName )
        {
            String result = "";

            String sep = "";

            if ( parentPackageName != null )
            {
                for ( int i = 0; i < parentPackageName.length; i++ )
                {
                    result += sep;
                    result += new String( parentPackageName[ i ] );
                    sep = ".";
                }
            }

            if ( Character.isUpperCase( packageName[ 0 ] ) )
            {
                return false;
            }

            String str = new String( packageName );

            result += sep;

            result += str;

            return isPackage( result );
        }

        public void cleanup()
        {
        }
    }

    private class EclipseCompilerICompilerRequestor
        implements ICompilerRequestor
    {
        private String destinationDirectory;

        private List errors;

        public EclipseCompilerICompilerRequestor( String destinationDirectory,
                                                  List errors )
        {
            this.destinationDirectory = destinationDirectory;
            this.errors = errors;
        }

        public void acceptResult( CompilationResult result )
        {
            if ( result.hasProblems() )
            {
                IProblem[] problems = result.getProblems();

                for ( int i = 0; i < problems.length; i++ )
                {
                    IProblem problem = problems[ i ];

                    String name = new String( problems[ i ].getOriginatingFileName() );

                    if ( problem.isWarning() )
                    {
                        errors.add( handleWarning( problem ) );
                    }
                    else
                    {
                        errors.add( handleError( name,
                                                 problem.getSourceLineNumber(),
                                                 -1,
                                                 problem.getMessage() ) );
                    }
                }
            }
            else
            {
                ClassFile[] classFiles = result.getClassFiles();

                for ( int i = 0; i < classFiles.length; i++ )
                {
                    ClassFile classFile = classFiles[ i ];
                    char[][] compoundName = classFile.getCompoundName();
                    String className = "";
                    String sep = "";

                    for ( int j = 0; j < compoundName.length; j++ )
                    {
                        className += sep;
                        className += new String( compoundName[ j ] );
                        sep = ".";
                    }

                    byte[] bytes = classFile.getBytes();

                    File outFile = new File( destinationDirectory, className.replace( '.', '/' ) + ".class" );

                    if ( !outFile.getParentFile().exists() )
                    {
                        outFile.getParentFile().mkdirs();
                    }

                    FileOutputStream fout = null;

                    try
                    {
                        fout = new FileOutputStream( outFile );

                        fout.write( bytes );
                    }
                    catch ( FileNotFoundException e )
                    {
                        errors.add( handleError( className, -1, -1, e.getMessage() ) );
                    }
                    catch ( IOException e )
                    {
                        errors.add( handleError( className, -1, -1, e.getMessage() ) );
                    }
                    finally
                    {
                        IOUtil.close( fout );
                    }
                }
            }
        }
    }
}
