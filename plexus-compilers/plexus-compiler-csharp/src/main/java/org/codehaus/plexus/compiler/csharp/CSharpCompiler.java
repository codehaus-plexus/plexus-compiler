package org.codehaus.plexus.compiler.csharp;

/*
 * Copyright 2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import javax.inject.Named;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.plexus.compiler.AbstractCompiler;
import org.codehaus.plexus.compiler.CompilerConfiguration;
import org.codehaus.plexus.compiler.CompilerException;
import org.codehaus.plexus.compiler.CompilerMessage;
import org.codehaus.plexus.compiler.CompilerOutputStyle;
import org.codehaus.plexus.compiler.CompilerResult;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.Os;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;
import org.codehaus.plexus.util.cli.WriterStreamConsumer;

/**
 * @author <a href="mailto:gdodinet@karmicsoft.com">Gilles Dodinet</a>
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @author <a href="mailto:matthew.pocock@ncl.ac.uk">Matthew Pocock</a>
 * @author <a href="mailto:chris.stevenson@gmail.com">Chris Stevenson</a>
 * @author <a href="mailto:mazas.marc@gmail.com">Marc Mazas</a>
 */
@Named("csharp")
public class CSharpCompiler extends AbstractCompiler {
    private static final String JAR_SUFFIX = ".jar";
    private static final String DLL_SUFFIX = ".dll";
    private static final String NET_SUFFIX = ".net";

    private static final String ARGUMENTS_FILE_NAME = "csharp-arguments";

    private static final String[] DEFAULT_INCLUDES = {"**/**"};

    private Map<String, String> compilerArguments;

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public CSharpCompiler() {
        super(CompilerOutputStyle.ONE_OUTPUT_FILE_FOR_ALL_INPUT_FILES, ".cs", null, null);
    }

    // ----------------------------------------------------------------------
    // Compiler Implementation
    // ----------------------------------------------------------------------

    @Override
    public String getCompilerId() {
        return "csharp";
    }

    public boolean canUpdateTarget(CompilerConfiguration configuration) throws CompilerException {
        return false;
    }

    public String getOutputFile(CompilerConfiguration configuration) throws CompilerException {
        return configuration.getOutputFileName() + "." + getTypeExtension(configuration);
    }

    public CompilerResult performCompile(CompilerConfiguration config) throws CompilerException {
        File destinationDir = new File(config.getOutputLocation());

        if (!destinationDir.exists()) {
            destinationDir.mkdirs();
        }

        config.setSourceFiles(null);

        String[] sourceFiles = CSharpCompiler.getSourceFiles(config);

        if (sourceFiles.length == 0) {
            return new CompilerResult().success(true);
        }

        logCompiling(sourceFiles, config);

        String[] args = buildCompilerArguments(config, sourceFiles);

        List<CompilerMessage> messages;

        if (config.isFork()) {
            messages = compileOutOfProcess(
                    config.getWorkingDirectory(), config.getBuildDirectory(), findExecutable(config), args);
        } else {
            throw new CompilerException("This compiler doesn't support in-process compilation.");
        }

        return new CompilerResult().compilerMessages(messages);
    }

    public String[] createCommandLine(CompilerConfiguration config) throws CompilerException {
        return buildCompilerArguments(config, CSharpCompiler.getSourceFiles(config));
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    private Map<String, String> getCompilerArguments(CompilerConfiguration config) {
        if (compilerArguments != null) {
            return compilerArguments;
        }

        compilerArguments = config.getCustomCompilerArgumentsAsMap();
        Map<String, String> ca2 = new HashMap<String, String>();

        Iterator<String> i = compilerArguments.keySet().iterator();

        while (i.hasNext()) {
            String orig = i.next();
            String v = compilerArguments.get(orig);
            if (orig.contains(":") && v == null) {
                String[] arr = orig.split(":");
                i.remove();
                String k = arr[0];
                v = arr[1];
                //                compilerArguments.put(k, v);
                ca2.put(k, v);
                if (config.isDebug()) {
                    System.out.println(
                            "internal splitting of argument '" + orig + "' to key '" + k + "', value '" + v + "'");
                }
            }
        }
        compilerArguments.putAll(ca2);

        config.setCustomCompilerArgumentsAsMap(compilerArguments);

        return compilerArguments;
    }

    private String findExecutable(CompilerConfiguration config) {
        String executable = config.getExecutable();

        if (!StringUtils.isEmpty(executable)) {
            return executable;
        }

        if (Os.isFamily("windows")) {
            return "csc";
        }

        return "mcs";
    }

    /*
    $ mcs --help
    Mono C# compiler, (C) 2001 - 2003 Ximian, Inc.
    mcs [options] source-files
       --about            About the Mono C# compiler
       -addmodule:MODULE  Adds the module to the generated assembly
       -checked[+|-]      Set default context to checked
       -codepage:ID       Sets code page to the one in ID (number, utf8, reset)
       -clscheck[+|-]     Disables CLS Compliance verifications
       -define:S1[;S2]    Defines one or more symbols (short: /d:)
       -debug[+|-], -g    Generate debugging information
       -delaysign[+|-]    Only insert the public key into the assembly (no signing)
       -doc:FILE          XML Documentation file to generate
       -keycontainer:NAME The key pair container used to strongname the assembly
       -keyfile:FILE      The strongname key file used to strongname the assembly
       -langversion:TEXT  Specifies language version modes: ISO-1 or Default
       -lib:PATH1,PATH2   Adds the paths to the assembly link path
       -main:class        Specified the class that contains the entry point
       -noconfig[+|-]     Disables implicit references to assemblies
       -nostdlib[+|-]     Does not load core libraries
       -nowarn:W1[,W2]    Disables one or more warnings
       -optimize[+|-]     Enables code optimalizations
       -out:FNAME         Specifies output file
       -pkg:P1[,Pn]       References packages P1..Pn
       -recurse:SPEC      Recursively compiles the files in SPEC ([dir]/file)
       -reference:ASS     References the specified assembly (-r:ASS)
       -target:KIND       Specifies the target (KIND is one of: exe, winexe,
                          library, module), (short: /t:)
       -unsafe[+|-]       Allows unsafe code
       -warnaserror[+|-]  Treat warnings as errors
       -warn:LEVEL        Sets warning level (the highest is 4, the default is 2)
       -help2             Show other help flags

    Resources:
       -linkresource:FILE[,ID] Links FILE as a resource
       -resource:FILE[,ID]     Embed FILE as a resource
       -win32res:FILE          Specifies Win32 resource file (.res)
       -win32icon:FILE         Use this icon for the output
       @file                   Read response file for more options

    Options can be of the form -option or /option
        */

    /*
    C:\Program Files\Microsoft Visual Studio\2022\Professional\MSBuild\Current\Bin\Roslyn>csc -help -preferreduilang:en
    Microsoft (R) Visual C# Compiler version 4.11.0-3.24460.3 (5649376e)
    Copyright (C) Microsoft Corporation. All rights reserved.


                                 Visual C# Compiler Options

                           - OUTPUT FILES -
    -out:<file>                   Specify output file name (default: base name of
                                  file with main class or first file)
    -target:exe                   Build a console executable (default) (Short
                                  form: -t:exe)
    -target:winexe                Build a Windows executable (Short form:
                                  -t:winexe)
    -target:library               Build a library (Short form: -t:library)
    -target:module                Build a module that can be added to another
                                  assembly (Short form: -t:module)
    -target:appcontainerexe       Build an Appcontainer executable (Short form:
                                  -t:appcontainerexe)
    -target:winmdobj              Build a Windows Runtime intermediate file that
                                  is consumed by WinMDExp (Short form: -t:winmdobj)
    -doc:<file>                   XML Documentation file to generate
    -refout:<file>                Reference assembly output to generate
    -platform:<string>            Limit which platforms this code can run on: x86,
                                  Itanium, x64, arm, arm64, anycpu32bitpreferred, or
                                  anycpu. The default is anycpu.

                           - INPUT FILES -
    -recurse:<wildcard>           Include all files in the current directory and
                                  subdirectories according to the wildcard
                                  specifications
    -reference:<alias>=<file>     Reference metadata from the specified assembly
                                  file using the given alias (Short form: -r)
    -reference:<file list>        Reference metadata from the specified assembly
                                  files (Short form: -r)
    -addmodule:<file list>        Link the specified modules into this assembly
    -link:<file list>             Embed metadata from the specified interop
                                  assembly files (Short form: -l)
    -analyzer:<file list>         Run the analyzers from this assembly
                                  (Short form: -a)
    -additionalfile:<file list>   Additional files that don't directly affect code
                                  generation but may be used by analyzers for producing
                                  errors or warnings.
    -embed                        Embed all source files in the PDB.
    -embed:<file list>            Embed specific files in the PDB.

                           - RESOURCES -
    -win32res:<file>              Specify a Win32 resource file (.res)
    -win32icon:<file>             Use this icon for the output
    -win32manifest:<file>         Specify a Win32 manifest file (.xml)
    -nowin32manifest              Do not include the default Win32 manifest
    -resource:<resinfo>           Embed the specified resource (Short form: -res)
    -linkresource:<resinfo>       Link the specified resource to this assembly
                                  (Short form: -linkres) Where the resinfo format
                                  is <file>[,<string name>[,public|private]]

                           - CODE GENERATION -
    -debug[+|-]                   Emit debugging information
    -debug:{full|pdbonly|portable|embedded}
                                  Specify debugging type ('full' is default,
                                  'portable' is a cross-platform format,
                                  'embedded' is a cross-platform format embedded into
                                  the target .dll or .exe)
    -optimize[+|-]                Enable optimizations (Short form: -o)
    -deterministic                Produce a deterministic assembly
                                  (including module version GUID and timestamp)
    -refonly                      Produce a reference assembly in place of the main output
    -instrument:TestCoverage      Produce an assembly instrumented to collect
                                  coverage information
    -sourcelink:<file>            Source link info to embed into PDB.

                           - ERRORS AND WARNINGS -
    -warnaserror[+|-]             Report all warnings as errors
    -warnaserror[+|-]:<warn list> Report specific warnings as errors
                                  (use "nullable" for all nullability warnings)
    -warn:<n>                     Set warning level (0 or higher) (Short form: -w)
    -nowarn:<warn list>           Disable specific warning messages
                                  (use "nullable" for all nullability warnings)
    -ruleset:<file>               Specify a ruleset file that disables specific
                                  diagnostics.
    -errorlog:<file>[,version=<sarif_version>]
                                  Specify a file to log all compiler and analyzer
                                  diagnostics.
                                  sarif_version:{1|2|2.1} Default is 1. 2 and 2.1
                                  both mean SARIF version 2.1.0.
    -reportanalyzer               Report additional analyzer information, such as
                                  execution time.
    -skipanalyzers[+|-]           Skip execution of diagnostic analyzers.

                           - LANGUAGE -
    -checked[+|-]                 Generate overflow checks
    -unsafe[+|-]                  Allow 'unsafe' code
    -define:<symbol list>         Define conditional compilation symbol(s) (Short
                                  form: -d)
    -langversion:?                Display the allowed values for language version
    -langversion:<string>         Specify language version such as
                                  `latest` (latest version, including minor versions),
                                  `default` (same as `latest`),
                                  `latestmajor` (latest version, excluding minor versions),
                                  `preview` (latest version, including features in unsupported preview),
                                  or specific versions like `6` or `7.1`
    -nullable[+|-]                Specify nullable context option enable|disable.
    -nullable:{enable|disable|warnings|annotations}
                                  Specify nullable context option enable|disable|warnings|annotations.

                           - SECURITY -
    -delaysign[+|-]               Delay-sign the assembly using only the public
                                  portion of the strong name key
    -publicsign[+|-]              Public-sign the assembly using only the public
                                  portion of the strong name key
    -keyfile:<file>               Specify a strong name key file
    -keycontainer:<string>        Specify a strong name key container
    -highentropyva[+|-]           Enable high-entropy ASLR

                           - MISCELLANEOUS -
    @<file>                       Read response file for more options
    -help                         Display this usage message (Short form: -?)
    -nologo                       Suppress compiler copyright message
    -noconfig                     Do not auto include CSC.RSP file
    -parallel[+|-]                Concurrent build.
    -version                      Display the compiler version number and exit.

                           - ADVANCED -
    -baseaddress:<address>        Base address for the library to be built
    -checksumalgorithm:<alg>      Specify algorithm for calculating source file
                                  checksum stored in PDB. Supported values are:
                                  SHA1 or SHA256 (default).
    -codepage:<n>                 Specify the codepage to use when opening source
                                  files
    -utf8output                   Output compiler messages in UTF-8 encoding
    -main:<type>                  Specify the type that contains the entry point
                                  (ignore all other possible entry points) (Short
                                  form: -m)
    -fullpaths                    Compiler generates fully qualified paths
    -filealign:<n>                Specify the alignment used for output file
                                  sections
    -pathmap:<K1>=<V1>,<K2>=<V2>,...
                                  Specify a mapping for source path names output by
                                  the compiler.
    -pdb:<file>                   Specify debug information file name (default:
                                  output file name with .pdb extension)
    -errorendlocation             Output line and column of the end location of
                                  each error
    -preferreduilang              Specify the preferred output language name.
    -nosdkpath                    Disable searching the default SDK path for standard library assemblies.
    -nostdlib[+|-]                Do not reference standard library (mscorlib.dll)
    -subsystemversion:<string>    Specify subsystem version of this assembly
    -lib:<file list>              Specify additional directories to search in for
                                  references
    -errorreport:<string>         Specify how to handle internal compiler errors:
                                  prompt, send, queue, or none. The default is
                                  queue.
    -appconfig:<file>             Specify an application configuration file
                                  containing assembly binding settings
    -moduleassemblyname:<string>  Name of the assembly which this module will be
                                  a part of
    -modulename:<string>          Specify the name of the source module
    -generatedfilesout:<dir>      Place files generated during compilation in the
                                  specified directory.
    -reportivts[+|-]                    Output information on all IVTs granted to this
                                  assembly by all dependencies, and annotate foreign assembly
                                  accessibility errors with what assembly they came from.
     */

    private String[] buildCompilerArguments(CompilerConfiguration config, String[] sourceFiles)
            throws CompilerException {
        List<String> args = new ArrayList<>();

        // plugin parameter is no the same as the compiler option! so replaced
        //        if (config.isDebug()) {
        //            args.add("/debug+");
        //        } else {
        //            args.add("/debug-");
        //        }

        // config.isShowWarnings()
        // config.getSourceVersion()
        // config.getTargetVersion()
        // config.getSourceEncoding()

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        for (String element : config.getClasspathEntries()) {
            File f = new File(element);

            if (!f.isFile()) {
                continue;
            }

            if (element.endsWith(JAR_SUFFIX)) {
                try {
                    File dllDir = new File(element + NET_SUFFIX);
                    if (!dllDir.exists()) {
                        dllDir.mkdir();
                    }
                    JarUtil.extract(dllDir.toPath(), new File(element));
                    for (String tmpfile : dllDir.list()) {
                        if (tmpfile.endsWith(DLL_SUFFIX)) {
                            String dll =
                                    Paths.get(dllDir.getAbsolutePath(), tmpfile).toString();
                            args.add("/reference:\"" + dll + "\"");
                        }
                    }
                } catch (IOException e) {
                    throw new CompilerException(e.toString(), e);
                }
            } else {
                args.add("/reference:\"" + element + "\"");
            }
        }

        // TODO: include all user compiler arguments and not only some!

        Map<String, String> compilerArguments = getCompilerArguments(config);

        // ----------------------------------------------------------------------
        // Main class
        // ----------------------------------------------------------------------

        String mainClass = compilerArguments.get("-main");

        if (!StringUtils.isEmpty(mainClass)) {
            args.add("/main:" + mainClass);
        }

        // ----------------------------------------------------------------------
        // Xml Doc output
        // ----------------------------------------------------------------------

        String doc = compilerArguments.get("-doc");

        if (!StringUtils.isEmpty(doc)) {
            args.add("/doc:"
                    + new File(config.getOutputLocation(), config.getOutputFileName() + ".xml").getAbsolutePath());
        }

        // ----------------------------------------------------------------------
        // Debug option (full, pdbonly...)
        // ----------------------------------------------------------------------

        String debug = compilerArguments.get("-debug");

        if (!StringUtils.isEmpty(debug)) {
            args.add("/debug:" + debug);
        }

        // ----------------------------------------------------------------------
        // Nowarn option (w#1,w#2...)
        // ----------------------------------------------------------------------

        String nowarn = compilerArguments.get("-nowarn");

        if (!StringUtils.isEmpty(nowarn)) {
            args.add("/nowarn:" + nowarn);
        }

        // ----------------------------------------------------------------------
        // Out - Override output name, this is required for generating the unit test dll
        // ----------------------------------------------------------------------

        String out = compilerArguments.get("-out");

        if (!StringUtils.isEmpty(out)) {
            args.add("/out:" + new File(config.getOutputLocation(), out).getAbsolutePath());
        } else {
            args.add("/out:" + new File(config.getOutputLocation(), getOutputFile(config)).getAbsolutePath());
        }

        // ----------------------------------------------------------------------
        // Resource File - compile in a resource file into the assembly being created
        // ----------------------------------------------------------------------
        String resourcefile = compilerArguments.get("-resourcefile");

        if (!StringUtils.isEmpty(resourcefile)) {
            String resourceTarget = compilerArguments.get("-resourcetarget");
            args.add("/res:" + new File(resourcefile).getAbsolutePath() + "," + resourceTarget);
        }

        // ----------------------------------------------------------------------
        // Target - type of assembly to produce: library,exe,winexe...
        // ----------------------------------------------------------------------

        String target = compilerArguments.get("-target");

        if (StringUtils.isEmpty(target)) {
            args.add("/target:library");
        } else {
            args.add("/target:" + target);
        }

        // ----------------------------------------------------------------------
        // remove MS logo from output (not applicable for mono)
        // ----------------------------------------------------------------------
        String nologo = compilerArguments.get("-nologo");

        if (!StringUtils.isEmpty(nologo) && !"false".equals(nologo.toLowerCase())) {
            args.add("/nologo");
        }

        // ----------------------------------------------------------------------
        // Unsafe option
        // ----------------------------------------------------------------------
        String unsafe = compilerArguments.get("-unsafe");

        if (!StringUtils.isEmpty(unsafe) && "true".equals(unsafe.toLowerCase())) {
            args.add("/unsafe");
        }

        // ----------------------------------------------------------------------
        // PreferredUILang option
        // ----------------------------------------------------------------------
        String preferreduilang = compilerArguments.get("-preferreduilang");

        if (!StringUtils.isEmpty(preferreduilang)) {
            args.add("/preferreduilang:" + preferreduilang);
        }

        // ----------------------------------------------------------------------
        // Utf8Output option
        // ----------------------------------------------------------------------
        String utf8output = compilerArguments.get("-utf8output");

        if (!StringUtils.isEmpty(utf8output) && !"false".equals(utf8output)) {
            args.add("/utf8output");
        }

        // ----------------------------------------------------------------------
        // add any resource files
        // ----------------------------------------------------------------------
        this.addResourceArgs(config, args);

        // ----------------------------------------------------------------------
        // add source files
        // ----------------------------------------------------------------------
        for (String sourceFile : sourceFiles) {
            args.add(sourceFile);
        }

        if (config.isDebug()) {
            System.out.println("built compiler arguments:" + args);
        }

        return args.toArray(new String[args.size()]);
    }

    private void addResourceArgs(CompilerConfiguration config, List<String> args) {
        File filteredResourceDir = this.findResourceDir(config);
        if ((filteredResourceDir != null) && filteredResourceDir.exists()) {
            DirectoryScanner scanner = new DirectoryScanner();
            scanner.setBasedir(filteredResourceDir);
            scanner.setIncludes(DEFAULT_INCLUDES);
            scanner.addDefaultExcludes();
            scanner.scan();

            List<String> includedFiles = Arrays.asList(scanner.getIncludedFiles());
            for (String name : includedFiles) {
                File filteredResource = new File(filteredResourceDir, name);
                String assemblyResourceName = this.convertNameToAssemblyResourceName(name);
                String argLine = "/resource:\"" + filteredResource + "\",\"" + assemblyResourceName + "\"";
                if (config.isDebug()) {
                    System.out.println("adding resource arg line:" + argLine);
                }
                args.add(argLine);
            }
        }
    }

    private File findResourceDir(CompilerConfiguration config) {
        if (config.isDebug()) {
            System.out.println("Looking for resourcesDir");
        }

        Map<String, String> compilerArguments = getCompilerArguments(config);

        String tempResourcesDirAsString = compilerArguments.get("-resourceDir");
        File filteredResourceDir = null;
        if (tempResourcesDirAsString != null) {
            filteredResourceDir = new File(tempResourcesDirAsString);
            if (config.isDebug()) {
                System.out.println("Found resourceDir at: " + filteredResourceDir.toString());
            }
        } else {
            if (config.isDebug()) {
                System.out.println("No resourceDir was available.");
            }
        }
        return filteredResourceDir;
    }

    private String convertNameToAssemblyResourceName(String name) {
        return name.replace(File.separatorChar, '.');
    }

    @SuppressWarnings("deprecation")
    private List<CompilerMessage> compileOutOfProcess(
            File workingDirectory, File target, String executable, String[] args) throws CompilerException {
        // ----------------------------------------------------------------------
        // Build the @arguments file
        // ----------------------------------------------------------------------

        File file;

        PrintWriter output = null;

        try {
            file = new File(target, ARGUMENTS_FILE_NAME);

            output = new PrintWriter(new FileWriter(file));

            for (String arg : args) {
                output.println(arg);
            }
        } catch (IOException e) {
            throw new CompilerException("Error writing arguments file.", e);
        } finally {
            IOUtil.close(output);
        }

        // ----------------------------------------------------------------------
        // Execute!
        // ----------------------------------------------------------------------

        Commandline cli = new Commandline();

        cli.setWorkingDirectory(workingDirectory.getAbsolutePath());

        cli.setExecutable(executable);

        cli.createArgument().setValue("@" + file.getAbsolutePath());

        Writer stringWriter = new StringWriter();

        StreamConsumer out = new WriterStreamConsumer(stringWriter);

        StreamConsumer err = new WriterStreamConsumer(stringWriter);

        int returnCode;

        List<CompilerMessage> messages;

        try {
            returnCode = CommandLineUtils.executeCommandLine(cli, out, err);

            messages = parseCompilerOutput(new BufferedReader(new StringReader(stringWriter.toString())));
        } catch (CommandLineException | IOException e) {
            throw new CompilerException("Error while executing the external compiler.", e);
        }

        if (returnCode != 0 && messages.isEmpty()) {
            // TODO: exception?
            messages.add(new CompilerMessage(
                    "Failure executing the compiler, but could not parse the error:" + EOL + stringWriter.toString(),
                    true));
        }

        return messages;
    }

    public static List<CompilerMessage> parseCompilerOutput(BufferedReader bufferedReader) throws IOException {
        List<CompilerMessage> messages = new ArrayList<>();

        String line = bufferedReader.readLine();

        while (line != null) {
            CompilerMessage compilerError = DefaultCSharpCompilerParser.parseLine(line);

            if (compilerError != null) {
                messages.add(compilerError);
            }

            line = bufferedReader.readLine();
        }

        return messages;
    }

    private String getType(Map<String, String> compilerArguments) {
        String type = compilerArguments.get("-target");

        if (StringUtils.isEmpty(type)) {
            return "library";
        }

        return type;
    }

    private String getTypeExtension(CompilerConfiguration configuration) throws CompilerException {
        String type = getType(configuration.getCustomCompilerArgumentsAsMap());

        if ("exe".equals(type) || "winexe".equals(type)) {
            return "exe";
        }

        if ("library".equals(type) || "module".equals(type)) {
            return "dll";
        }

        throw new CompilerException("Unrecognized type '" + type + "'.");
    }

    // added for debug purposes....
    protected static String[] getSourceFiles(CompilerConfiguration config) {
        Set<String> sources = new HashSet<>();

        // Set sourceFiles = null;
        // was:
        Set<File> sourceFiles = config.getSourceFiles();

        if (sourceFiles != null && !sourceFiles.isEmpty()) {
            for (File sourceFile : sourceFiles) {
                sources.add(sourceFile.getAbsolutePath());
            }
        } else {
            for (String sourceLocation : config.getSourceLocations()) {
                if (!new File(sourceLocation).exists()) {
                    if (config.isDebug()) {
                        System.out.println("Ignoring not found sourceLocation at: " + sourceLocation);
                    }
                    continue;
                }
                sources.addAll(getSourceFilesForSourceRoot(config, sourceLocation));
            }
        }

        String[] result;

        if (sources.isEmpty()) {
            result = new String[0];
        } else {
            result = sources.toArray(new String[sources.size()]);
        }

        return result;
    }

    protected static Set<String> getSourceFilesForSourceRoot(CompilerConfiguration config, String sourceLocation) {
        DirectoryScanner scanner = new DirectoryScanner();

        scanner.setBasedir(sourceLocation);

        Set<String> includes = config.getIncludes();

        if (includes != null && !includes.isEmpty()) {
            String[] inclStrs = includes.toArray(new String[includes.size()]);
            scanner.setIncludes(inclStrs);
        } else {
            scanner.setIncludes(new String[] {"**/*.cs"});
        }

        Set<String> excludes = config.getExcludes();

        if (excludes != null && !excludes.isEmpty()) {
            String[] exclStrs = excludes.toArray(new String[excludes.size()]);
            scanner.setIncludes(exclStrs);
        }

        scanner.scan();

        String[] sourceDirectorySources = scanner.getIncludedFiles();

        Set<String> sources = new HashSet<>();

        for (String source : sourceDirectorySources) {
            File f = new File(sourceLocation, source);

            sources.add(f.getPath());
        }

        return sources;
    }
}
