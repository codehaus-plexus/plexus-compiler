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
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

public class EclipseJavaCompiler
    extends AbstractCompiler
{
    private static boolean target14;

    private static boolean source14;

    private boolean debug;

    private String sourceEncoding;

    // ----------------------------------------------------------------------
    // Static initializer
    // ----------------------------------------------------------------------

    static
    {
        // Detect JDK version we are running under
        String version = System.getProperty( "java.specification.version" );

        try
        {
            target14 = Float.parseFloat( version ) >= 1.4;

            source14 = target14;
        }
        catch ( NumberFormatException e )
        {
            target14 = false;

            source14 = target14;
        }
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public EclipseJavaCompiler()
    {
        debug = false;

        source14 = true;
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public List compile( CompilerConfiguration config )
        throws Exception
    {
        String[] sourceFiles = getSourceFiles( config );

        List errors = new LinkedList();

        for ( int i = 0; i < sourceFiles.length; i++ )
        {
            String sourceFile = sourceFiles[ i ];

            compile( sourceFile,
                     (String) config.getSourceLocations().get( 0 ),
                     config.getOutputLocation(),
                     errors );
        }

        return errors;
    }

    public void compile( String sourceFile,
                         String sourceDir,
                         String destinationDirectory,
                         List errors )
        throws Exception
    {
        String targetClassName = makeClassName( sourceFile, sourceDir );

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        String[] fileNames = new String[]{sourceFile};

        String[] classNames = new String[]{targetClassName};

        INameEnvironment env = new EclipseCompilerINameEnvironment( targetClassName, sourceFile, classLoader, errors );

        IErrorHandlingPolicy policy = DefaultErrorHandlingPolicies.proceedWithAllProblems();

        Map settings = new HashMap();

        settings.put( CompilerOptions.OPTION_LineNumberAttribute, CompilerOptions.GENERATE );

        settings.put( CompilerOptions.OPTION_SourceFileAttribute, CompilerOptions.GENERATE );

        settings.put( CompilerOptions.OPTION_ReportDeprecation, CompilerOptions.IGNORE );

        if ( sourceEncoding != null )
        {
            settings.put( CompilerOptions.OPTION_Encoding, sourceEncoding );
        }

        if ( debug )
        {
            settings.put( CompilerOptions.OPTION_LocalVariableAttribute, CompilerOptions.GENERATE );
        }

        if ( source14 )
        {
            settings.put( CompilerOptions.OPTION_Source, CompilerOptions.VERSION_1_4 );
        }

        if ( target14 )
        {
            settings.put( CompilerOptions.OPTION_TargetPlatform, CompilerOptions.VERSION_1_4 );
        }

        IProblemFactory problemFactory = new DefaultProblemFactory( Locale.getDefault() );

        ICompilerRequestor requestor = new EclipseCompilerICompilerRequestor( destinationDirectory, errors );

        ICompilationUnit[] compilationUnits = new ICompilationUnit[classNames.length];

        for ( int i = 0; i < compilationUnits.length; i++ )
        {
            String className = classNames[ i ];

            compilationUnits[ i ] = new CompilationUnit( fileNames[ i ], className, errors );
        }

        Compiler compiler = new Compiler( env, policy, settings, requestor, problemFactory );

        compiler.compile( compilationUnits );
    }

    private CompilerError handleError( String className, int line, int column, Object errorMessage )
    {
        String fileName = className.replace( '.', File.separatorChar ) + ".java";

        if ( column < 0 )
        {
            column = 0;
        }

        return new CompilerError( fileName,
                                  true,
                                  line,
                                  column,
                                  line,
                                  column,
                                  errorMessage.toString() );
    }

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
        private String targetClassName;

        private String sourceFile;

        private ClassLoader classLoader;

        private List errors;

        public EclipseCompilerINameEnvironment( String targetClassName,
                                                String sourceFile,
                                                ClassLoader classLoader,
                                                List errors )
        {
            this.targetClassName = targetClassName;
            this.sourceFile = sourceFile;
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
                if ( className.equals( targetClassName ) )
                {
                    ICompilationUnit compilationUnit = new CompilationUnit( sourceFile,
                                                                            className,
                                                                            errors );

                    return new NameEnvironmentAnswer( compilationUnit );
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

                return new NameEnvironmentAnswer( classFileReader );
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
            if ( result.equals( targetClassName ) )
            {
                return false;
            }
            String resourceName = result.replace( '.', '/' ) + ".class";
            InputStream is =
                classLoader.getResourceAsStream( resourceName );
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
            String str = new String( packageName );

            if ( Character.isUpperCase( str.charAt( 0 ) ) )
            {
                if ( !isPackage( result ) )
                {
                    return false;
                }
            }

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
        private final String destinationDirectory;

        private final List errors;

        public EclipseCompilerICompilerRequestor( String destinationDirectory, List errors )
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
                    errors.add( handleError( name, problem.getSourceLineNumber(), -1, problem.getMessage() ) );
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

                        BufferedOutputStream bos = new BufferedOutputStream( fout );

                        bos.write( bytes );
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
