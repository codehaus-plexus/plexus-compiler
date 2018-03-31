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
import org.eclipse.jdt.core.compiler.CompilationProgress;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.compiler.batch.BatchCompiler;
import org.eclipse.jdt.internal.compiler.ClassFile;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.ICompilerRequestor;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFormatException;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.jdt.internal.compiler.env.INameEnvironment;
import org.eclipse.jdt.internal.compiler.env.NameEnvironmentAnswer;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

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
    boolean errorsAsWarnings = false;

    public CompilerResult performCompile( CompilerConfiguration config )
        throws CompilerException
    {

        //URL[] urls = new URL[1 + classpathEntries.size()];
		//
        //int i = 0;
		//
        //try
        //{
        //    urls[i++] = new File( config.getOutputLocation() ).toURL();
		//
        //    for ( String entry : classpathEntries )
        //    {
        //        urls[i++] = new File( entry ).toURL();
        //    }
        //}
        //catch ( MalformedURLException e )
        //{
        //    throw new CompilerException( "Error while converting the classpath entries to URLs.", e );
        //}

        //ClassLoader classLoader = new URLClassLoader( urls );
		//
        //SourceCodeLocator sourceCodeLocator = new SourceCodeLocator( config.getSourceLocations() );

        // ----------------------------------------------------------------------
        // Build settings from configuration
        // ----------------------------------------------------------------------

		List<String> args = new ArrayList<>();
        if ( config.isDebug() )
        {
        	args.add("-preserveAllLocals");
        	args.add("-g:lines,vars,source");
        } else {
			args.add("-g:lines,source");
		}

        String sourceVersion = decodeVersion( config.getSourceVersion() );

        if ( sourceVersion != null )
        {
        	args.add("-source");
        	args.add(sourceVersion);
        }

        String targetVersion = decodeVersion( config.getTargetVersion() );

        if ( targetVersion != null )
        {
        	args.add("-target");
        	args.add(targetVersion);
        }

        if ( StringUtils.isNotEmpty( config.getSourceEncoding() ) )
        {
        	args.add("-encoding");
        	args.add(config.getSourceEncoding());
        }

		if ( !config.isShowWarnings() )
		{
			args.add("-warn:none");
		}
		else
		{
			StringBuilder warns = new StringBuilder();

			if(config.isShowDeprecation()) {
				append(warns, "+deprecation");
			} else {
				append(warns, "-deprecation");
			}

			//-- Make room for more warnings to be enabled/disabled
			args.add("-warn:" + warns);
		}

		if(config.isParameters())
		{
			args.add("-parameters");
		}

        // ----------------------------------------------------------------------
        // Set Eclipse-specific options
        // ----------------------------------------------------------------------

        // compiler-specific extra options override anything else in the config object...
        Map<String, String> extras = cleanKeyNames( config.getCustomCompilerArgumentsAsMap() );
        if( extras.containsKey( "errorsAsWarnings" ) )
        {
        	extras.remove( "errorsAsWarnings" );
        	this.errorsAsWarnings = true;
        } else if(extras.containsKey("-errorsAsWarnings")) {
			extras.remove( "-errorsAsWarnings" );
			this.errorsAsWarnings = true;
		}

		//-- Handle the properties silliness
		String props = extras.get("properties");
        if(null == props)
        	props = extras.get("properties");
        if(null != props) {
        	File propFile = new File(props);
        	if(! propFile.exists() || ! propFile.isFile())
				throw new IllegalArgumentException("Properties file " + propFile + " does not exist");
		}
        //settings.putAll( extras );

        //if ( settings.containsKey( "properties" ) )
        //{
        //    initializeWarnings( settings.get( "properties" ), settings );
        //    settings.remove( "properties" );
        //}

        //IProblemFactory problemFactory = new DefaultProblemFactory( Locale.getDefault() );
		//
        //ICompilerRequestor requestor = new EclipseCompilerICompilerRequestor( config.getOutputLocation(), errors );
		//
        //for ( String sourceRoot : config.getSourceLocations() )
        //{
        //    // annotations directory does not always exist and the below scanner fails on non existing directories
        //    File potentialSourceDirectory = new File( sourceRoot );
        //    if ( potentialSourceDirectory.exists() )
        //    {
        //        Set<String> sources = getSourceFilesForSourceRoot( config, sourceRoot );
		//
        //        for ( String source : sources )
        //        {
        //            CompilationUnit unit = new CompilationUnit( source, makeClassName( source, sourceRoot ), errors,
        //                                                        config.getSourceEncoding() );
		//
        //            compilationUnits.add( unit );
        //        }
        //    }
        //}

		// Annotation processors defined?
		List<String> extraSourceDirs = new ArrayList<>();

		if(!isPreJava16(config)) {
			//now add jdk 1.6 annotation processing related parameters
			String[] annotationProcessors = config.getAnnotationProcessors();
			List<String> processorPathEntries = config.getProcessorPathEntries();
			if((annotationProcessors != null && annotationProcessors.length > 0) || (processorPathEntries != null && processorPathEntries.size() > 0)) {
				if(annotationProcessors != null && annotationProcessors.length > 0) {
					args.add("-processor");
					StringBuilder sb = new StringBuilder();
					for(String ap : annotationProcessors) {
						if(sb.length() > 0)
							sb.append(',');
						sb.append(ap);
					}
					args.add(sb.toString());
				}

				if(processorPathEntries != null && processorPathEntries.size() > 0) {
					args.add("-processorpath");
					args.add(getPathString(processorPathEntries));
				}

				File generatedSourcesDir = config.getGeneratedSourcesDirectory();
				if(generatedSourcesDir != null) {
					generatedSourcesDir.mkdirs();
				}
				if(config.getProc() != null) {
					args.add("-proc:" + config.getProc());
				}
				extraSourceDirs.add(generatedSourcesDir.getAbsolutePath());
			}
		}

		// Output path
		args.add("-d");
		args.add(config.getOutputLocation());

		//-- Write .class files even when error occur, but make sure methods with compile errors do abort when called
		args.add("-proceedOnError:Fatal");

		//-- classpath
		List<String> classpathEntries = config.getClasspathEntries();
		if(classpathEntries.size() != 0) {
			args.add("-classpath");
			args.add(getPathString(classpathEntries));
		}

		// ----------------------------------------------------------------------
        // Compile!
        // ----------------------------------------------------------------------

		// Send all errors to xml temp file
		File errorF = null;
		try {
			errorF = File.createTempFile("ecjerr-", ".xml");

			args.add("-log");
			args.add(errorF.toString());

			for(String source : config.getSourceLocations()) {
				File srcFile = new File(source);
				if(srcFile.exists()) {
					Set<String> ss = getSourceFilesForSourceRoot( config, source );
					args.addAll(ss);
				}
			}


			System.out.println(">>>> ECJ: " + args);

			StringWriter sw = new StringWriter();
			PrintWriter devNull = new PrintWriter(sw);

			//BatchCompiler.compile(args.toArray(new String[args.size()]), new PrintWriter(System.err), new PrintWriter(System.out), new CompilationProgress() {
			boolean worked = BatchCompiler.compile(args.toArray(new String[args.size()]), devNull, devNull, new CompilationProgress() {
				@Override public void begin(int i) {

				}

				@Override public void done() {

				}

				@Override public boolean isCanceled() {
					return false;
				}

				@Override public void setTaskName(String s) {

				}

				@Override public void worked(int i, int i1) {

				}
			});

			List<CompilerMessage> messageList;
			boolean hasError = false;
			if(errorF.length() < 80) {
				messageList = new ArrayList<>();
				messageList.add(new CompilerMessage("Internal compiler error"));
				System.err.println(">> " + sw.toString());
				return new CompilerResult(false, messageList);
			}
			messageList = new EcjResponseParser().parse(errorF, errorsAsWarnings);

			for(CompilerMessage compilerMessage : messageList) {
				if(compilerMessage.isError()) {
					hasError = true;
					break;
				}
			}
			return new CompilerResult(! hasError, messageList);

		} catch(Exception x) {
        	throw new RuntimeException(x);				// sigh
		} finally {
			//if(null != errorF) {
        		//try {
        		//	errorF.delete();
			//	} catch(Exception x) {}
			//}
		}
    }

	static private void append(StringBuilder warns, String s) {
		if(warns.length() > 0)
			warns.append(',');
		warns.append(s);

	}

	private boolean isPreJava16(CompilerConfiguration config) {
        String s = config.getSourceVersion();
        if ( s == null )
        {
            //now return true, as the 1.6 version is not the default - 1.4 is.
            return true;
        }
        return s.startsWith( "1.5" ) || s.startsWith( "1.4" ) || s.startsWith( "1.3" ) || s.startsWith( "1.2" )
            || s.startsWith( "1.1" ) || s.startsWith( "1.0" );
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
        else if ( "9".equals( versionSpec ) )
        {
        	return CompilerOptions.VERSION_9;
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

    private class EclipseCompilerINameEnvironment
        implements INameEnvironment
    {
        private SourceCodeLocator sourceCodeLocator;

        private ClassLoader classLoader;

        private List<CompilerMessage> errors;

        public EclipseCompilerINameEnvironment( SourceCodeLocator sourceCodeLocator, ClassLoader classLoader,
                                                List<CompilerMessage> errors )
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
                File f = sourceCodeLocator.findSourceCodeForClass( className );

                if ( f != null )
                {
                    ICompilationUnit compilationUnit = new CompilationUnit( f.getAbsolutePath(), className, errors );

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

        public boolean isPackage( char[][] parentPackageName, char[] packageName )
        {
            String result = "";

            String sep = "";

            if ( parentPackageName != null )
            {
                for ( int i = 0; i < parentPackageName.length; i++ )
                {
                    result += sep;
                    result += new String( parentPackageName[i] );
                    sep = ".";
                }
            }

            if ( Character.isUpperCase( packageName[0] ) )
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
            // nothing to do
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
                    	if( errorsAsWarnings )
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

    private class NullWriter extends Writer {
		@Override public void write(char[] cbuf, int off, int len) throws IOException {
		}

		@Override public void close() {
		}

		@Override public void flush() {
		}
	}
}
