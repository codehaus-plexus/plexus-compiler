package org.codehaus.plexus.compiler.eclipse;

import org.codehaus.plexus.compiler.AbstractCompiler;
import org.codehaus.plexus.compiler.CompilerConfiguration;
import org.codehaus.plexus.compiler.CompilerError;

import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.internal.compiler.ClassFile;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.Compiler;
import org.eclipse.jdt.internal.compiler.DefaultErrorHandlingPolicies;
import org.eclipse.jdt.internal.compiler.ICompilerRequestor;
import org.eclipse.jdt.internal.compiler.IErrorHandlingPolicy;
import org.eclipse.jdt.internal.compiler.IProblemFactory;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.jdt.internal.compiler.env.INameEnvironment;
import org.eclipse.jdt.internal.compiler.env.NameEnvironmentAnswer;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblemFactory;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
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

    private List errors = new LinkedList();

    static
    {
        // Detect JDK version we are running under
        String version = System.getProperty( "java.specification.version" );

        try
        {
            source14 = target14 = Float.parseFloat( version ) >= 1.4;
        }
        catch ( NumberFormatException e )
        {
            source14 = target14 = false;
        }
    }

    public EclipseJavaCompiler()
    {
        this.debug = false;

        source14 = true;
    }

    public List compile( CompilerConfiguration config )
        throws Exception
    {
        String[] sourceFiles = getSourceFiles( config );

        for ( int i = 0; i < sourceFiles.length; i++ )
        {
            String sourceFile = sourceFiles[i];

            compile( sourceFile, (String) config.getSourceLocations().get( 0 ), config.getOutputLocation() );
        }

        return errors;
    }

    public void setEncoding( String encoding )
    {
        this.sourceEncoding = encoding;
    }

    public boolean compile( final String sourceFile, String sourceDir, final String destinationDirectory )
        throws IOException
    {
        final String targetClassName = makeClassName( sourceFile, sourceDir );

        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        String[] fileNames = new String[]{sourceFile};

        String[] classNames = new String[]{targetClassName};


        final INameEnvironment env = new INameEnvironment()
        {

            public NameEnvironmentAnswer findType( char[][] compoundTypeName )
            {
                String result = "";

                String sep = "";

                for ( int i = 0; i < compoundTypeName.length; i++ )
                {
                    result += sep;
                    result += new String( compoundTypeName[i] );
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
                    result += new String( packageName[i] );
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
                        ICompilationUnit compilationUnit = new CompilationUnit( sourceFile, className );
                        return new NameEnvironmentAnswer( compilationUnit );
                    }

                    String resourceName = className.replace( '.', '/' ) + ".class";

                    InputStream is = classLoader.getResourceAsStream( resourceName );

                    if ( is != null )
                    {
                        byte[] classBytes;
                        byte[] buf = new byte[8192];
                        ByteArrayOutputStream baos = new ByteArrayOutputStream( buf.length );

                        int count;

                        while ( ( count = is.read( buf, 0, buf.length ) ) > 0 )
                        {
                            baos.write( buf, 0, count );
                        }

                        baos.flush();

                        classBytes = baos.toByteArray();

                        char[] fileName = className.toCharArray();

                        ClassFileReader classFileReader = new ClassFileReader( classBytes, fileName, true );

                        return new NameEnvironmentAnswer( classFileReader );
                    }
                }
                catch ( IOException exc )
                {
                    handleError( className, -1, -1, exc.getMessage() );
                }
                catch ( org.eclipse.jdt.internal.compiler.classfmt.ClassFormatException exc )
                {
                    handleError( className, -1, -1, exc.getMessage() );
                }

                return null;
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
                        String str = new String( parentPackageName[i] );
                        result += str;
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

        };

        final IErrorHandlingPolicy policy = DefaultErrorHandlingPolicies.proceedWithAllProblems();

        final Map settings = new HashMap();

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

        final IProblemFactory problemFactory = new DefaultProblemFactory( Locale.getDefault() );

        final ICompilerRequestor requestor = new ICompilerRequestor()
        {
            public void acceptResult( CompilationResult result )
            {
                try
                {
                    if ( result.hasProblems() )
                    {
                        IProblem[] problems = result.getProblems();

                        for ( int i = 0; i < problems.length; i++ )
                        {
                            IProblem problem = problems[i];
                            String name = new String( problems[i].getOriginatingFileName() );
                            handleError( name, problem.getSourceLineNumber(), -1, problem.getMessage() );
                        }
                    }
                    else
                    {
                        ClassFile[] classFiles = result.getClassFiles();

                        for ( int i = 0; i < classFiles.length; i++ )
                        {
                            ClassFile classFile = classFiles[i];
                            char[][] compoundName = classFile.getCompoundName();
                            String className = "";
                            String sep = "";

                            for ( int j = 0; j < compoundName.length; j++ )
                            {
                                className += sep;
                                className += new String( compoundName[j] );
                                sep = ".";
                            }

                            byte[] bytes = classFile.getBytes();

                            File outFile = new File( destinationDirectory, className.replace( '.', '/' ) + ".class" );

                            if ( !outFile.getParentFile().exists() )
                            {
                                outFile.getParentFile().mkdirs();
                            }

                            FileOutputStream fout = new FileOutputStream( outFile );

                            BufferedOutputStream bos = new BufferedOutputStream( fout );

                            bos.write( bytes );

                            bos.close();
                        }
                    }
                }
                catch ( IOException exc )
                {
                    exc.printStackTrace();
                }
            }
        };

        ICompilationUnit[] compilationUnits = new ICompilationUnit[classNames.length];

        for ( int i = 0; i < compilationUnits.length; i++ )
        {
            String className = classNames[i];

            compilationUnits[i] = new CompilationUnit( fileNames[i], className );
        }

        Compiler compiler = new Compiler( env, policy, settings, requestor, problemFactory );

        compiler.compile( compilationUnits );

        return errors.size() == 0;
    }

    void handleError( String className, int line, int column, Object errorMessage )
    {
        String fileName = className.replace( '.', File.separatorChar ) + ".java";

        if ( column < 0 )
        {
            column = 0;
        }

        CompilerError err = new CompilerError( fileName,
                                               true,
                                               line,
                                               column,
                                               line,
                                               column,
                                               errorMessage.toString() );

        System.out.println( "err = " + err );

        errors.add( err );
    }

    public List getErrors()
        throws IOException
    {
        return errors;
    }

    class CompilationUnit implements ICompilationUnit
    {
        String className;
        String sourceFile;

        CompilationUnit( String sourceFile, String className )
        {
            this.className = className;
            this.sourceFile = sourceFile;
        }

        public char[] getFileName()
        {
            return className.toCharArray();
        }

        public char[] getContents()
        {
            char[] result = null;
            try
            {
                Reader reader = new BufferedReader( new FileReader( sourceFile ) );

                if ( reader != null )
                {
                    char[] chars = new char[8192];

                    StringBuffer buf = new StringBuffer();

                    int count;

                    while ( ( count = reader.read( chars, 0, chars.length ) ) > 0 )
                    {
                        buf.append( chars, 0, count );
                    }

                    result = new char[buf.length()];

                    buf.getChars( 0, result.length, result, 0 );
                }
            }
            catch ( IOException e )
            {
                handleError( className, -1, -1, e.getMessage() );
            }

            return result;
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

                result[i] = tok.toCharArray();
            }

            return result;
        }
    }
}
