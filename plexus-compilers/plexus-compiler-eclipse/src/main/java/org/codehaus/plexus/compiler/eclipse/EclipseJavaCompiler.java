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
import org.codehaus.plexus.compiler.CompilerMessage;
import org.codehaus.plexus.compiler.CompilerOutputStyle;
import org.codehaus.plexus.compiler.CompilerResult;
import org.codehaus.plexus.util.StringUtils;
import org.eclipse.jdt.core.compiler.CompilationProgress;
import org.eclipse.jdt.core.compiler.batch.BatchCompiler;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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

    public CompilerResult performCompile(CompilerConfiguration config )
    {
        List<String> args = new ArrayList<>();
        args.add("-noExit");                            // Make sure ecj does not System.exit on us 8-/

        // Build settings from configuration
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

            if(config.isShowDeprecation())
            {
                append(warns, "+deprecation");
            }
            else
            {
                append(warns, "-deprecation");
            }

            //-- Make room for more warnings to be enabled/disabled
            args.add("-warn:" + warns);
        }

        if(config.isParameters())
        {
            args.add("-parameters");
        }

        // Set Eclipse-specific options
        // compiler-specific extra options override anything else in the config object...
        Map<String, String> extras = config.getCustomCompilerArgumentsAsMap();
        if( extras.containsKey( "errorsAsWarnings" ) )
        {
            extras.remove( "errorsAsWarnings" );
            this.errorsAsWarnings = true;
        }
        else if(extras.containsKey("-errorsAsWarnings"))
        {
            extras.remove( "-errorsAsWarnings" );
            this.errorsAsWarnings = true;
        }

        //-- check for existence of the properties file manually
        String props = extras.get("-properties");
        if(null != props) {
            File propFile = new File(props);
            if(! propFile.exists() || ! propFile.isFile())
                throw new IllegalArgumentException("Properties file specified by -properties " + propFile + " does not exist");
        }

        for(Entry<String, String> entry : extras.entrySet())
        {
            /*
             * The compiler mojo makes quite a mess of passing arguments, depending on exactly WHICH
             * way is used to pass them. The method method using <compilerArguments> uses the tag names
             * of its contents to denote option names, and so the compiler mojo happily adds a '-' to
             * all of the names there and adds them to the "custom compiler arguments" map as a
             * name, value pair where the name always contains a single '-'. The Eclipse compiler (and
             * javac too, btw) has options with two dashes (like --add-modules for java 9). These cannot
             * be passed using a <compilerArguments> tag.
             *
             * The other method is to use <compilerArgs>, where each SINGLE argument needs to be passed
             * using an <arg>xxxx</arg> tag. In there the xxx is not manipulated by the compiler mojo, so
             * if it starts with a dash or more dashes these are perfectly preserved. But of course these
             * single <arg> entries are not a pair. So the compiler mojo adds them as pairs of (xxxx, null).
             *
             * We use that knowledge here: if a pair has a null value then do not mess up the key but
             * render it as a single value. This should ensure that something like:
             * <compilerArgs>
             *     <arg>--add-modules</arg>
             *     <arg>java.se.ee</arg>
             * </compilerArgs>
             *
             * is actually added to the command like as such.
             *
             * (btw: the above example will still give an error when using ecj <= 4.8M6:
             *      invalid module name: java.se.ee
             * but that seems to be a bug in ecj).
             */
            String opt = entry.getKey();
            String optionValue = entry.getValue();
            if(null == optionValue) {
                //-- We have an option from compilerArgs: use the key as-is as a single option value
                args.add(opt);
            } else {
                if(!opt.startsWith("-"))
                    opt = "-" + opt;
                args.add(opt);
                args.add(optionValue);
            }
        }

        // Output path
        args.add("-d");
        args.add(config.getOutputLocation());

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
                    extraSourceDirs.add(generatedSourcesDir.getAbsolutePath());

                    //-- option to specify where annotation processor is to generate its output
                    args.add("-s");
                    args.add(generatedSourcesDir.getAbsolutePath());
                }
                if(config.getProc() != null) {
                    args.add("-proc:" + config.getProc());
                }
            }
        }

        //-- Write .class files even when error occur, but make sure methods with compile errors do abort when called
        if(extras.containsKey("-proceedOnError"))
            args.add("-proceedOnError:Fatal");      // Generate a class file even with errors, but make methods with errors fail when called

        //-- classpath
        List<String> classpathEntries = new ArrayList<>(config.getClasspathEntries());
        classpathEntries.add(config.getOutputLocation());
        args.add("-classpath");
        args.add(getPathString(classpathEntries));

        // Compile! Send all errors to xml temp file.
        File errorF = null;
        try
        {
            errorF = File.createTempFile("ecjerr-", ".xml");

            args.add("-log");
            args.add(errorF.toString());

            // Add all sources.
            int argCount = args.size();
            for(String source : config.getSourceLocations())
            {
                File srcFile = new File(source);
                if(srcFile.exists())
                {
                    Set<String> ss = getSourceFilesForSourceRoot(config, source);
                    args.addAll(ss);
                }
            }
            args.addAll(extraSourceDirs);
            if(args.size() == argCount)
            {
                //-- Nothing to do -> bail out
                return new CompilerResult(true, Collections.EMPTY_LIST);
            }

            getLogger().debug("ecj command line: " + args);

            StringWriter sw = new StringWriter();
            PrintWriter devNull = new PrintWriter(sw);

            //BatchCompiler.compile(args.toArray(new String[args.size()]), new PrintWriter(System.err), new PrintWriter(System.out), new CompilationProgress() {
            boolean success = BatchCompiler.compile(args.toArray( new String[0] ), devNull, devNull, new CompilationProgress() {
                @Override
                public void begin(int i)
                {
                }

                @Override
                public void done()
                {
                }

                @Override
                public boolean isCanceled()
                {
                    return false;
                }

                @Override
                public void setTaskName(String s)
                {
                }

                @Override
                public void worked(int i, int i1)
                {
                }
            });
            getLogger().debug(sw.toString());

            List<CompilerMessage> messageList;
            boolean hasError = false;
            if(errorF.length() < 80)
            {
                throw new EcjFailureException(sw.toString());
            }
            messageList = new EcjResponseParser().parse(errorF, errorsAsWarnings);

            for(CompilerMessage compilerMessage : messageList)
            {
                if(compilerMessage.isError())
                {
                    hasError = true;
                    break;
                }
            }
            if(!hasError && !success && !errorsAsWarnings)
            {
                CompilerMessage.Kind kind = errorsAsWarnings ? CompilerMessage.Kind.WARNING : CompilerMessage.Kind.ERROR;

                //-- Compiler reported failure but we do not seem to have one -> probable exception
                CompilerMessage cm = new CompilerMessage("[ecj] The compiler reported an error but has not written it to its logging", kind);
                messageList.add(cm);
                hasError = true;

                //-- Try to find the actual message by reporting the last 5 lines as a message
                String stdout = getLastLines(sw.toString(), 5);
                if(stdout.length() > 0)
                {
                    cm = new CompilerMessage("[ecj] The following line(s) might indicate the issue:\n" + stdout, kind);
                    messageList.add(cm);
                }
            }
            return new CompilerResult(!hasError || errorsAsWarnings, messageList);
        } catch(EcjFailureException x) {
            throw x;
        } catch(Exception x) {
            throw new RuntimeException(x);				// sigh
        } finally {
            if(null != errorF) {
                try {
                    errorF.delete();
                } catch(Exception x) {}
            }
        }
    }

    private String getLastLines(String text, int lines)
    {
        List<String> lineList = new ArrayList<>();
        text = text.replace("\r\n", "\n");
        text = text.replace("\r", "\n");            // make sure eoln is \n

        int index = text.length();
        while(index > 0) {
            int before = text.lastIndexOf('\n', index - 1);

            if(before + 1 < index) {                        // Non empty line?
                lineList.add(text.substring(before + 1, index));
                lines--;
                if(lines <= 0)
                    break;
            }

            index = before;
        }

        StringBuilder sb = new StringBuilder();
        for(int i = lineList.size() - 1; i >= 0; i--)
        {
            String s = lineList.get(i);
            sb.append(s);
            sb.append(System.getProperty("line.separator"));        // 8-/
        }
        return sb.toString();
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

    public String[] createCommandLine( CompilerConfiguration config )
    {
        return null;
    }


    /**
     * Change any Maven Java version number to ECJ's version number. Do not check the validity
     * of the version: the compiler does that nicely, and this allows compiler updates without
     * changing the compiler plugin. This is important with the half year release cycle for Java.
     */
    private String decodeVersion( String versionSpec )
    {
        if ( StringUtils.isEmpty( versionSpec ) )
        {
            return null;
        }

        if(versionSpec.equals("1.9")) {
            getLogger().warn("Version 9 should be specified as 9, not 1.9");
            return "9";
        }
        return versionSpec;
    }
}
