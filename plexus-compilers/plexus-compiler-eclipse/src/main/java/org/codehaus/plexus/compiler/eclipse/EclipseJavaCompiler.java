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
import org.codehaus.plexus.compiler.CompilerException;
import org.codehaus.plexus.compiler.CompilerMessage;
import org.codehaus.plexus.compiler.CompilerOutputStyle;
import org.codehaus.plexus.compiler.CompilerResult;
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
import org.eclipse.jdt.internal.compiler.batch.FileSystem;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFormatException;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.jdt.internal.compiler.env.INameEnvironment;
import org.eclipse.jdt.internal.compiler.env.NameEnvironmentAnswer;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblemFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.function.Predicate;

/**
 * @plexus.component role="org.codehaus.plexus.compiler.Compiler" role-hint="eclipse"
 */
public class EclipseJavaCompiler
    extends AbstractCompiler
{
    public EclipseJavaCompiler()
    {
        super( CompilerOutputStyle.ONE_OUTPUT_FILE_PER_INPUT_FILE, ".java", ".class", null );
    }

    // ----------------------------------------------------------------------
    // Compiler Implementation
    // ----------------------------------------------------------------------

    public CompilerResult performCompile( CompilerConfiguration config )
        throws CompilerException
    {
        List<CompilerMessage> errors = new LinkedList<CompilerMessage>();

        File outFile = new File(config.getOutputLocation());

        if(!outFile.isDirectory() && !outFile.mkdirs()) {
            throw new CompilerException( "Error creating output directory: " + outFile.getAbsolutePath());
        }

        List<String> cpe = new ArrayList<String>();
        cpe.addAll(Arrays.asList(System.getProperty("sun.boot.class.path").split(":")));
        cpe.addAll(config.getSourceLocations());
        cpe.addAll(config.getClasspathEntries());

        cpe.removeIf(new Predicate<String>() {
            @Override public boolean test(String e) {
                File file = new File(e);
                return !file.exists();
            }
        });

        INameEnvironment env = new FileSystem(
            cpe.toArray(new String[cpe.size()]),
            getSourceFiles(config),
            config.getSourceEncoding());

        IErrorHandlingPolicy policy = DefaultErrorHandlingPolicies.proceedWithAllProblems();

        // ----------------------------------------------------------------------
        // Build settings from configuration
        // ----------------------------------------------------------------------

        Map<String, String> settings = new HashMap<String, String>();

        if ( config.isDebug() )
        {
            settings.put( CompilerOptions.OPTION_LocalVariableAttribute, CompilerOptions.GENERATE );
            settings.put( CompilerOptions.OPTION_LineNumberAttribute, CompilerOptions.GENERATE );
            settings.put( CompilerOptions.OPTION_SourceFileAttribute, CompilerOptions.GENERATE );
        }

        if ( !config.isShowWarnings() )
        {
            Map opts = new CompilerOptions().getMap();
            for (Object optKey : opts.keySet()) {
                if (opts.get(optKey).equals(CompilerOptions.WARNING)) {
                    settings.put((String) optKey, CompilerOptions.IGNORE);
                }
            }
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
            settings.put( CompilerOptions.OPTION_Compliance, targetVersion );
        }

        if ( StringUtils.isNotEmpty( config.getSourceEncoding() ) )
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

        // compiler-specific extra options override anything else in the config object...
        Map<String, String> extras = cleanKeyNames( config.getCustomCompilerArgumentsAsMap() );

        settings.putAll( extras );

        if ( settings.containsKey( "properties" ) )
        {
            initializeWarnings( settings.get( "properties" ), settings );
            settings.remove( "properties" );
        }

        IProblemFactory problemFactory = new DefaultProblemFactory( Locale.getDefault() );

        ICompilerRequestor requestor = new EclipseCompilerICompilerRequestor( config.getOutputLocation(), errors );

        List<CompilationUnit> compilationUnits = new ArrayList<CompilationUnit>();

        for ( String sourceRoot : config.getSourceLocations() )
        {
            // annotations directory does not always exist and the below scanner fails on non existing directories
            File potentialSourceDirectory = new File( sourceRoot );
            if ( potentialSourceDirectory.exists() )
            {
                Set<String> sources = getSourceFilesForSourceRoot( config, sourceRoot );

                for ( String source : sources )
                {
                    CompilationUnit unit = new CompilationUnit( source, makeClassName( source, sourceRoot ), errors,
                                                                config.getSourceEncoding() );

                    compilationUnits.add( unit );
                }
            }
        }

        // ----------------------------------------------------------------------
        // Compile!
        // ----------------------------------------------------------------------

        CompilerOptions options = new CompilerOptions( settings );
        Compiler compiler = new Compiler( env, policy, options, requestor, problemFactory );

        ICompilationUnit[] units = compilationUnits.toArray( new ICompilationUnit[compilationUnits.size()] );

        compiler.compile( units );

        CompilerResult compilerResult = new CompilerResult().compilerMessages( errors );

        for ( CompilerMessage compilerMessage : errors )
        {
            if ( compilerMessage.isError() )
            {
                compilerResult.setSuccess( false );
                continue;
            }
        }

        return compilerResult;
    }

    // The compiler mojo adds a dash to all keys which does not make sense for the eclipse compiler
    Map<String, String> cleanKeyNames( Map<String, String> customCompilerArgumentsAsMap )
    {
        LinkedHashMap<String, String> cleanedMap = new LinkedHashMap<String, String>();

        for ( Map.Entry<String, String> entry : customCompilerArgumentsAsMap.entrySet() )
        {
            String key = entry.getKey();
            if ( key.startsWith( "-" ) )
            {
                key = key.substring( 1 );
            }
            cleanedMap.put( key, entry.getValue() );
        }

        return cleanedMap;
    }

    public String[] createCommandLine( CompilerConfiguration config )
        throws CompilerException
    {
        return null;
    }

    private CompilerMessage handleError( String className, int line, int column, Object errorMessage )
    {
        if ( className.endsWith( ".java" ) )
        {
            className = className.substring( 0, className.lastIndexOf( '.' ) );
        }
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

        return new CompilerMessage( fileName, CompilerMessage.Kind.ERROR, line, column, line, column, message );

    }

    private CompilerMessage handleWarning( String fileName, IProblem warning )
    {
        return new CompilerMessage( fileName, CompilerMessage.Kind.WARNING,
                                    warning.getSourceLineNumber(), warning.getSourceStart(),
                                    warning.getSourceLineNumber(), warning.getSourceEnd(), warning.getMessage() );
    }

    private String decodeVersion( String versionSpec )
    {
        if ( StringUtils.isEmpty( versionSpec ) )
        {
            return null;
        }
        else if ( "1.1".equals( versionSpec ) )
        {
            return CompilerOptions.VERSION_1_1;
        }
        else if ( "1.2".equals( versionSpec ) )
        {
            return CompilerOptions.VERSION_1_2;
        }
        else if ( "1.3".equals( versionSpec ) )
        {
            return CompilerOptions.VERSION_1_3;
        }
        else if ( "1.4".equals( versionSpec ) )
        {
            return CompilerOptions.VERSION_1_4;
        }
        else if ( "1.5".equals( versionSpec ) )
        {
            return CompilerOptions.VERSION_1_5;
        }
        else if ( "1.6".equals( versionSpec ) )
        {
            return CompilerOptions.VERSION_1_6;
        }
        else if ( "1.7".equals( versionSpec ) )
        {
            return CompilerOptions.VERSION_1_7;
        }
        else if ( "1.8".equals( versionSpec ) )
        {
            return CompilerOptions.VERSION_1_8;
        }
        else
        {
            getLogger().warn(
                "Unknown version '" + versionSpec + "', no version setting will be given to the compiler." );

            return null;
        }
    }

    // ----------------------------------------------------------------------
    // Classes
    // ----------------------------------------------------------------------

    private class CompilationUnit
        implements ICompilationUnit
    {
        private final String className;

        private final String sourceFile;

        private final String sourceEncoding;

        private final List<CompilerMessage> errors;

        CompilationUnit( String sourceFile, String className, List<CompilerMessage> errors )
        {
            this( sourceFile, className, errors, null );
        }

        CompilationUnit( String sourceFile, String className, List<CompilerMessage> errors, String sourceEncoding )
        {
            this.className = className;
            this.sourceFile = sourceFile;
            this.errors = errors;
            this.sourceEncoding = sourceEncoding;
        }

        public char[] getFileName()
        {
            String fileName = sourceFile;

            int lastSeparator = fileName.lastIndexOf( File.separatorChar );

            if ( lastSeparator > 0 )
            {
                fileName = fileName.substring( lastSeparator + 1 );
            }

            return fileName.toCharArray();
        }

        String getAbsolutePath()
        {
            return sourceFile;
        }

        public char[] getContents()
        {
            try
            {
                return FileUtils.fileRead( sourceFile, sourceEncoding ).toCharArray();
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

                result[i] = tok.toCharArray();
            }

            return result;
        }

        public boolean ignoreOptionalProblems()
        {
            return false;
        }
    }

    private class EclipseCompilerICompilerRequestor
        implements ICompilerRequestor
    {
        private String destinationDirectory;

        private List<CompilerMessage> errors;

        public EclipseCompilerICompilerRequestor( String destinationDirectory, List<CompilerMessage> errors )
        {
            this.destinationDirectory = destinationDirectory;
            this.errors = errors;
        }

        public void acceptResult( CompilationResult result )
        {
            boolean hasErrors = false;

            if ( result.hasProblems() )
            {
                IProblem[] problems = result.getProblems();

                for ( IProblem problem : problems )
                {
                    String name = getFileName( result.getCompilationUnit(), problem.getOriginatingFileName() );

                    if ( problem.isWarning() )
                    {
                        errors.add( handleWarning( name, problem ) );
                    }
                    else
                    {
                        hasErrors = true;
                        errors.add( handleError( name, problem.getSourceLineNumber(), -1, problem.getMessage() ) );
                    }
                }
            }

            if ( !hasErrors )
            {
                ClassFile[] classFiles = result.getClassFiles();

                for ( ClassFile classFile : classFiles )
                {
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

        private String getFileName( ICompilationUnit compilationUnit, char[] originalFileName )
        {
            if ( compilationUnit instanceof CompilationUnit )
            {
                return ( (CompilationUnit) compilationUnit ).getAbsolutePath();
            }
            else
            {
                return String.valueOf( originalFileName );
            }
        }
    }

    private void initializeWarnings( String propertiesFile, Map<String, String> setting )
    {
        File file = new File( propertiesFile );
        if ( !file.exists() )
        {
            throw new IllegalArgumentException( "Properties file not exist" );
        }
        BufferedInputStream stream = null;
        Properties properties = null;
        try
        {
            stream = new BufferedInputStream( new FileInputStream( propertiesFile ) );
            properties = new Properties();
            properties.load( stream );
        }
        catch ( IOException e )
        {
            throw new IllegalArgumentException( "Properties file load error" );
        }
        finally
        {
            if ( stream != null )
            {
                try
                {
                    stream.close();
                }
                catch ( IOException e )
                {
                    // ignore
                }
            }
        }
        for ( Iterator iterator = properties.entrySet().iterator(); iterator.hasNext(); )
        {
            Map.Entry entry = (Map.Entry) iterator.next();
            final String key = (String) entry.getKey();
            setting.put( key, entry.getValue().toString() );
        }
    }
}
