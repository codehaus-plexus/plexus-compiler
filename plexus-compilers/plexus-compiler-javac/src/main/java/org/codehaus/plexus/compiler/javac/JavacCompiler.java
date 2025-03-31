package org.codehaus.plexus.compiler.javac;

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

/**
 *
 * Copyright 2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codehaus.plexus.compiler.AbstractCompiler;
import org.codehaus.plexus.compiler.CompilerConfiguration;
import org.codehaus.plexus.compiler.CompilerException;
import org.codehaus.plexus.compiler.CompilerMessage;
import org.codehaus.plexus.compiler.CompilerOutputStyle;
import org.codehaus.plexus.compiler.CompilerResult;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.Os;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;

import static org.codehaus.plexus.compiler.CompilerMessage.Kind.*;
import static org.codehaus.plexus.compiler.javac.JavacCompiler.Messages.*;

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @author <a href="mailto:matthew.pocock@ncl.ac.uk">Matthew Pocock</a>
 * @author <a href="mailto:joerg.wassmer@web.de">J&ouml;rg Wa&szlig;mer</a>
 * @author Alexander Kriegisch
 * @author Others
 *
 */
@Named("javac")
@Singleton
public class JavacCompiler extends AbstractCompiler {

    /**
     * Multi-language compiler messages to parse from forked javac output.
     * <ul>
     *   <li>OpenJDK 8+ is delivered with 3 locales (en, ja, zh_CN).</li>
     *   <li>OpenJDK 21+ is delivered with 4 locales (en, ja, zh_CN, de).</li>
     * </ul>
     * Instead of manually duplicating multi-language messages into this class, it would be preferable to fetch the
     * strings directly from the running JDK:
     * <pre>{@code
     * new JavacMessages("com.sun.tools.javac.resources.javac", Locale.getDefault())
     *   .getLocalizedString("javac.msg.proc.annotation.uncaught.exception")
     * }</pre>
     * Hoewever, due to JMS module protection, it would be necessary to run Plexus Compiler (and hence also Maven
     * Compiler and the whole Maven JVM) with {@code --add-exports jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED}
     * on more recent JDK versions. As this cannot be reliably expected and using internal APIs - even though stable
     * since at least JDK 8 - it is not a future-proof approach. So we refrain from doing so, even though during Plexus
     * Compiler development it might come in handy.
     * <p>
     * TODO: Check compiler.properties and javac.properties in OpenJDK javac source code for
     *       message changes, relevant new messages, new locales.
     */
    protected static class Messages {
        // compiler.properties -> compiler.err.error (en, ja, zh_CN, de)
        protected static final String[] ERROR_PREFIXES = {"error: ", "エラー: ", "错误: ", "Fehler: "};

        // compiler.properties -> compiler.warn.warning (en, ja, zh_CN, de)
        protected static final String[] WARNING_PREFIXES = {"warning: ", "警告: ", "警告: ", "Warnung: "};

        // compiler.properties -> compiler.note.note (en, ja, zh_CN, de)
        protected static final String[] NOTE_PREFIXES = {"Note: ", "ノート: ", "注: ", "Hinweis: "};

        // compiler.properties -> compiler.misc.verbose.*
        protected static final String[] MISC_PREFIXES = {"["};

        // Generic javac error prefix
        // TODO: In JDK 8, this generic prefix no longer seems to be in use for javac error messages, at least not in
        //       the Java part of javac. Maybe in C sources? Does javac even use any native classes?
        protected static final String[] JAVAC_GENERIC_ERROR_PREFIXES = {"javac:"};

        // Hard-coded, English-only error header in JVM native code, *not* followed by stack trace, but rather
        // by another text message
        protected static final String[] VM_INIT_ERROR_HEADERS = {"Error occurred during initialization of VM"};

        // Hard-coded, English-only error header in class System, followed by stack trace
        protected static final String[] BOOT_LAYER_INIT_ERROR_HEADERS = {
            "Error occurred during initialization of boot layer"
        };

        // javac.properties-> javac.msg.proc.annotation.uncaught.exception
        // (en JDK-8, ja JDK-8, zh_CN JDK-8, en JDK-21, ja JDK-21, zh_CN JDK-21, de JDK-21)
        protected static final String[] ANNOTATION_PROCESSING_ERROR_HEADERS = {
            "\n\nAn annotation processor threw an uncaught exception.\nConsult the following stack trace for details.\n\n",
            "\n\n注釈処理で捕捉されない例外がスローされました。\n詳細は次のスタック・トレースで調査してください。\n\n",
            "\n\n批注处理程序抛出未捕获的异常错误。\n有关详细信息, 请参阅以下堆栈跟踪。\n\n",
            "\n\nAn annotation processor threw an uncaught exception.\nConsult the following stack trace for details.\n\n",
            "\n\n注釈処理で捕捉されない例外がスローされました。\n詳細は次のスタックトレースで調査してください。\n\n",
            "\n\n批注处理程序抛出未捕获的异常错误。\n有关详细信息, 请参阅以下堆栈跟踪。\n\n",
            "\n\nEin Annotationsprozessor hat eine nicht abgefangene Ausnahme ausgelöst.\nDetails finden Sie im folgenden Stacktrace.\n\n"
        };

        // javac.properties-> javac.msg.bug
        // (en JDK-8, ja JDK-8, zh_CN JDK-8, en JDK-9, ja JDK-9, zh_CN JDK-9, en JDK-21, ja JDK-21, zh_CN JDK-21, de
        // JDK-21)
        protected static final String[] FILE_A_BUG_ERROR_HEADERS = {
            "An exception has occurred in the compiler ({0}). Please file a bug at the Java Developer Connection (http://java.sun.com/webapps/bugreport)  after checking the Bug Parade for duplicates. Include your program and the following diagnostic in your report.  Thank you.\n",
            "コンパイラで例外が発生しました({0})。Bug Paradeで重複がないかをご確認のうえ、Java Developer Connection (http://java.sun.com/webapps/bugreport)でbugの登録をお願いいたします。レポートには、そのプログラムと下記の診断内容を含めてください。ご協力ありがとうございます。\n",
            "编译器 ({0}) 中出现异常错误。 如果在 Bug Parade 中没有找到该错误, 请在 Java Developer Connection (http://java.sun.com/webapps/bugreport) 中建立 Bug。请在报告中附上您的程序和以下诊断信息。谢谢。\n",
            "An exception has occurred in the compiler ({0}). Please file a bug against the Java compiler via the Java bug reporting page (http://bugreport.java.com) after checking the Bug Database (http://bugs.java.com) for duplicates. Include your program and the following diagnostic in your report. Thank you.",
            "コンパイラで例外が発生しました({0})。Bug Database (http://bugs.java.com)で重複がないかをご確認のうえ、Java bugレポート・ページ(http://bugreport.java.com)でJavaコンパイラに対するbugの登録をお願いいたします。レポートには、そのプログラムと下記の診断内容を含めてください。ご協力ありがとうございます。",
            "编译器 ({0}) 中出现异常错误。如果在 Bug Database (http://bugs.java.com) 中没有找到该错误, 请通过 Java Bug 报告页 (http://bugreport.java.com) 建立该 Java 编译器 Bug。请在报告中附上您的程序和以下诊断信息。谢谢。",
            "An exception has occurred in the compiler ({0}). Please file a bug against the Java compiler via the Java bug reporting page (https://bugreport.java.com) after checking the Bug Database (https://bugs.java.com) for duplicates. Include your program, the following diagnostic, and the parameters passed to the Java compiler in your report. Thank you.\n",
            "コンパイラで例外が発生しました({0})。バグ・データベース(https://bugs.java.com)で重複がないかをご確認のうえ、Javaのバグ・レポート・ページ(https://bugreport.java.com)から、Javaコンパイラに対するバグの登録をお願いいたします。レポートには、該当のプログラム、次の診断内容、およびJavaコンパイラに渡されたパラメータをご入力ください。ご協力ありがとうございます。\n",
            "编译器 ({0}) 中出现异常错误。如果在 Bug Database (https://bugs.java.com) 中没有找到有关该错误的 Java 编译器 Bug，请通过 Java Bug 报告页 (https://bugreport.java.com) 提交 Java 编译器 Bug。请在报告中附上您的程序、以下诊断信息以及传递到 Java 编译器的参数。谢谢。\n",
            "Im Compiler ({0}) ist eine Ausnahme aufgetreten. Erstellen Sie auf der Java-Seite zum Melden von Bugs (https://bugreport.java.com) einen Bugbericht, nachdem Sie die Bugdatenbank (https://bugs.java.com) auf Duplikate geprüft haben. Geben Sie in Ihrem Bericht Ihr Programm, die folgende Diagnose und die Parameter an, die Sie dem Java-Compiler übergeben haben. Vielen Dank.\n"
        };

        // javac.properties-> javac.msg.resource
        // (en JDK-8, ja JDK-8, zh_CN JDK-8, en JDK-21, ja JDK-21, zh_CN JDK-21, de JDK-21)
        protected static final String[] SYSTEM_OUT_OF_RESOURCES_ERROR_HEADERS = {
            "\n\nThe system is out of resources.\nConsult the following stack trace for details.\n",
            "\n\nシステム・リソースが不足しています。\n詳細は次のスタック・トレースで調査してください。\n",
            "\n\n系统资源不足。\n有关详细信息, 请参阅以下堆栈跟踪。\n",
            "\n\nThe system is out of resources.\nConsult the following stack trace for details.\n",
            "\n\nシステム・リソースが不足しています。\n詳細は次のスタックトレースで調査してください。\n",
            "\n\n系统资源不足。\n有关详细信息, 请参阅以下堆栈跟踪。\n",
            "\n\nDas System hat keine Ressourcen mehr.\nDetails finden Sie im folgenden Stacktrace.\n"
        };

        // javac.properties-> javac.msg.io
        // (en JDK-8, ja JDK-8, zh_CN JDK-8, en JDK-21, ja JDK-21, zh_CN JDK-21, de JDK-21)
        protected static final String[] IO_ERROR_HEADERS = {
            "\n\nAn input/output error occurred.\nConsult the following stack trace for details.\n",
            "\n\n入出力エラーが発生しました。\n詳細は次のスタック・トレースで調査してください。\n",
            "\n\n发生输入/输出错误。\n有关详细信息, 请参阅以下堆栈跟踪。\n",
            "\n\nAn input/output error occurred.\nConsult the following stack trace for details.\n",
            "\n\n入出力エラーが発生しました。\n詳細は次のスタックトレースで調査してください。\n",
            "\n\n发生输入/输出错误。\n有关详细信息, 请参阅以下堆栈跟踪。\n",
            "\n\nEin Eingabe-/Ausgabefehler ist aufgetreten.\nDetails finden Sie im folgenden Stacktrace.\n"
        };

        // javac.properties-> javac.msg.plugin.uncaught.exception
        // (en JDK-8, ja JDK-8, zh_CN JDK-8, en JDK-21, ja JDK-21, zh_CN JDK-21, de JDK-21)
        protected static final String[] PLUGIN_ERROR_HEADERS = {
            "\n\nA plugin threw an uncaught exception.\nConsult the following stack trace for details.\n",
            "\n\nプラグインで捕捉されない例外がスローされました。\n詳細は次のスタック・トレースで調査してください。\n",
            "\n\n插件抛出未捕获的异常错误。\n有关详细信息, 请参阅以下堆栈跟踪。\n",
            "\n\nA plugin threw an uncaught exception.\nConsult the following stack trace for details.\n",
            "\n\nプラグインで捕捉されない例外がスローされました。\n詳細は次のスタック・トレースで調査してください。\n",
            "\n\n插件抛出未捕获的异常错误。\n有关详细信息, 请参阅以下堆栈跟踪。\n",
            "\n\nEin Plug-in hat eine nicht abgefangene Ausnahme ausgel\u00F6st.\nDetails finden Sie im folgenden Stacktrace.\n"
        };
    }

    private static final Object LOCK = new Object();
    private static final String JAVAC_CLASSNAME = "com.sun.tools.javac.Main";

    private volatile Class<?> javacClass;
    private final Deque<Class<?>> javacClasses = new ConcurrentLinkedDeque<>();

    private static final Pattern JAVA_MAJOR_AND_MINOR_VERSION_PATTERN = Pattern.compile("\\d+(\\.\\d+)?");

    /** Cache of javac version per executable (never invalidated) */
    private static final Map<String, String> VERSION_PER_EXECUTABLE = new ConcurrentHashMap<>();

    @Inject
    private InProcessCompiler inProcessCompiler;

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public JavacCompiler() {
        super(CompilerOutputStyle.ONE_OUTPUT_FILE_PER_INPUT_FILE, ".java", ".class", null);
    }

    // ----------------------------------------------------------------------
    // Compiler Implementation
    // ----------------------------------------------------------------------

    @Override
    public String getCompilerId() {
        return "javac";
    }

    private String getInProcessJavacVersion() throws CompilerException {
        return System.getProperty("java.version");
    }

    private String getOutOfProcessJavacVersion(String executable) throws CompilerException {
        String version = VERSION_PER_EXECUTABLE.get(executable);
        if (version == null) {
            Commandline cli = new Commandline();
            cli.setExecutable(executable);
            /*
             * The option "-version" should be supported by javac since 1.6 (https://docs.oracle.com/javase/6/docs/technotes/tools/solaris/javac.html)
             * up to 21 (https://docs.oracle.com/en/java/javase/21/docs/specs/man/javac.html#standard-options)
             */
            cli.addArguments(new String[] {"-version"}); //
            CommandLineUtils.StringStreamConsumer out = new CommandLineUtils.StringStreamConsumer();
            try {
                int exitCode = CommandLineUtils.executeCommandLine(cli, out, out);
                if (exitCode != 0) {
                    throw new CompilerException("Could not retrieve version from " + executable + ". Exit code "
                            + exitCode + ", Output: " + out.getOutput());
                }
            } catch (CommandLineException e) {
                throw new CompilerException("Error while executing the external compiler " + executable, e);
            }
            version = extractMajorAndMinorVersion(out.getOutput());
            VERSION_PER_EXECUTABLE.put(executable, version);
        }
        return version;
    }

    static String extractMajorAndMinorVersion(String text) {
        Matcher matcher = JAVA_MAJOR_AND_MINOR_VERSION_PATTERN.matcher(text);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Could not extract version from \"" + text + "\"");
        }
        return matcher.group();
    }

    @Override
    public CompilerResult performCompile(CompilerConfiguration config) throws CompilerException {
        File destinationDir = new File(config.getOutputLocation());
        if (!destinationDir.exists()) {
            destinationDir.mkdirs();
        }

        String[] sourceFiles = getSourceFiles(config);
        if ((sourceFiles == null) || (sourceFiles.length == 0)) {
            return new CompilerResult();
        }

        logCompiling(sourceFiles, config);

        final String javacVersion;
        final String executable;
        if (config.isFork()) {
            executable = getJavacExecutable(config);
            javacVersion = getOutOfProcessJavacVersion(executable);
        } else {
            javacVersion = getInProcessJavacVersion();
            executable = null;
        }

        String[] args = buildCompilerArguments(config, sourceFiles, javacVersion);
        CompilerResult result;

        if (config.isFork()) {
            result = compileOutOfProcess(config, executable, args);
        } else {
            if (hasJavaxToolProvider() && !config.isForceJavacCompilerUse()) {
                // use fqcn to prevent loading of the class on 1.5 environment !
                result = inProcessCompiler().compileInProcess(args, config, sourceFiles);
            } else {
                result = compileInProcess(args, config);
            }
        }

        return result;
    }

    protected InProcessCompiler inProcessCompiler() {
        return inProcessCompiler;
    }

    /**
     *
     * @return {@code true} if the current context class loader has access to {@code javax.tools.ToolProvider}
     */
    protected static boolean hasJavaxToolProvider() {
        try {
            Thread.currentThread().getContextClassLoader().loadClass("javax.tools.ToolProvider");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String[] createCommandLine(CompilerConfiguration config) throws CompilerException {
        final String javacVersion;
        if (config.isFork()) {
            String executable = getJavacExecutable(config);
            javacVersion = getOutOfProcessJavacVersion(executable);
        } else {
            javacVersion = getInProcessJavacVersion();
        }
        return buildCompilerArguments(config, getSourceFiles(config), javacVersion);
    }

    public static String[] buildCompilerArguments(
            CompilerConfiguration config, String[] sourceFiles, String javacVersion) {
        List<String> args = new ArrayList<>();

        // ----------------------------------------------------------------------
        // Set output
        // ----------------------------------------------------------------------

        File destinationDir = new File(config.getOutputLocation());
        args.add("-d");
        args.add(destinationDir.getAbsolutePath());

        // ----------------------------------------------------------------------
        // Set the class and source paths
        // ----------------------------------------------------------------------

        List<String> classpathEntries = config.getClasspathEntries();
        if (classpathEntries != null && !classpathEntries.isEmpty()) {
            args.add("-classpath");
            args.add(getPathString(classpathEntries));
        }

        List<String> modulepathEntries = config.getModulepathEntries();
        if (modulepathEntries != null && !modulepathEntries.isEmpty()) {
            args.add("--module-path");
            args.add(getPathString(modulepathEntries));
        }

        List<String> sourceLocations = config.getSourceLocations();
        if (sourceLocations != null && !sourceLocations.isEmpty()) {
            // always pass source path, even if sourceFiles are declared,
            // needed for jsr269 annotation processing, see MCOMPILER-98
            args.add("-sourcepath");
            args.add(getPathString(sourceLocations));
        }
        if (!hasJavaxToolProvider() || config.isForceJavacCompilerUse() || config.isFork()) {
            args.addAll(Arrays.asList(sourceFiles));
        }

        if (JavaVersion.JAVA_1_6.isOlderOrEqualTo(javacVersion)) {
            // now add jdk 1.6 annotation processing related parameters

            if (config.getGeneratedSourcesDirectory() != null) {
                config.getGeneratedSourcesDirectory().mkdirs();
                args.add("-s");
                args.add(config.getGeneratedSourcesDirectory().getAbsolutePath());
            }
            if (config.getProc() != null) {
                args.add("-proc:" + config.getProc());
            }
            if (config.getAnnotationProcessors() != null) {
                args.add("-processor");
                String[] procs = config.getAnnotationProcessors();
                StringBuilder buffer = new StringBuilder();
                for (int i = 0; i < procs.length; i++) {
                    if (i > 0) {
                        buffer.append(",");
                    }
                    buffer.append(procs[i]);
                }
                args.add(buffer.toString());
            }
            if (config.getProcessorPathEntries() != null
                    && !config.getProcessorPathEntries().isEmpty()) {
                args.add("-processorpath");
                args.add(getPathString(config.getProcessorPathEntries()));
            }
            if (config.getProcessorModulePathEntries() != null
                    && !config.getProcessorModulePathEntries().isEmpty()) {
                args.add("--processor-module-path");
                args.add(getPathString(config.getProcessorModulePathEntries()));
            }
        }

        if (config.isOptimize()) {
            args.add("-O");
        }

        if (config.isDebug()) {
            if (StringUtils.isNotEmpty(config.getDebugLevel())) {
                args.add("-g:" + config.getDebugLevel());
            } else {
                args.add("-g");
            }
        }

        if (config.isVerbose()) {
            args.add("-verbose");
        }

        if (JavaVersion.JAVA_1_8.isOlderOrEqualTo(javacVersion) && config.isParameters()) {
            args.add("-parameters");
        }

        if (config.isEnablePreview()) {
            args.add("--enable-preview");
        }

        if (config.getImplicitOption() != null) {
            args.add("-implicit:" + config.getImplicitOption());
        }

        if (config.isShowDeprecation()) {
            args.add("-deprecation");

            // This is required to actually display the deprecation messages
            config.setShowWarnings(true);
        }

        if (!config.isShowWarnings()) {
            args.add("-nowarn");
        } else {
            String warnings = config.getWarnings();
            if (config.isShowLint()) {
                if (config.isShowWarnings() && StringUtils.isNotEmpty(warnings)) {
                    args.add("-Xlint:" + warnings);
                } else {
                    args.add("-Xlint");
                }
            }
        }

        if (config.isFailOnWarning()) {
            args.add("-Werror");
        }

        if (JavaVersion.JAVA_9.isOlderOrEqualTo(javacVersion) && !StringUtils.isEmpty(config.getReleaseVersion())) {
            args.add("--release");
            args.add(config.getReleaseVersion());
        } else {
            // TODO: this could be much improved
            if (StringUtils.isEmpty(config.getTargetVersion())) {
                // Required, or it defaults to the target of your JDK (eg 1.5)
                args.add("-target");
                args.add("1.1");
            } else {
                args.add("-target");
                args.add(config.getTargetVersion());
            }

            if (JavaVersion.JAVA_1_4.isOlderOrEqualTo(javacVersion) && StringUtils.isEmpty(config.getSourceVersion())) {
                // If omitted, later JDKs complain about a 1.1 target
                args.add("-source");
                args.add("1.3");
            } else if (JavaVersion.JAVA_1_4.isOlderOrEqualTo(javacVersion)) {
                args.add("-source");
                args.add(config.getSourceVersion());
            }
        }

        if (JavaVersion.JAVA_1_4.isOlderOrEqualTo(javacVersion) && !StringUtils.isEmpty(config.getSourceEncoding())) {
            args.add("-encoding");
            args.add(config.getSourceEncoding());
        }

        if (!StringUtils.isEmpty(config.getModuleVersion())) {
            args.add("--module-version");
            args.add(config.getModuleVersion());
        }

        for (Map.Entry<String, String> entry : config.getCustomCompilerArgumentsEntries()) {
            String key = entry.getKey();

            if (StringUtils.isEmpty(key) || key.startsWith("-J")) {
                continue;
            }

            args.add(key);
            String value = entry.getValue();
            if (StringUtils.isEmpty(value)) {
                continue;
            }
            args.add(value);
        }

        if (!config.isFork() && !args.contains("-XDuseUnsharedTable=false")) {
            args.add("-XDuseUnsharedTable=true");
        }

        return args.toArray(new String[0]);
    }

    /**
     * Represents a particular Java version (through their according version prefixes)
     */
    enum JavaVersion {
        JAVA_1_3_OR_OLDER("1.3", "1.2", "1.1", "1.0"),
        JAVA_1_4("1.4"),
        JAVA_1_5("1.5"),
        JAVA_1_6("1.6"),
        JAVA_1_7("1.7"),
        JAVA_1_8("1.8"),
        JAVA_9("9"); // since Java 9 a different versioning scheme was used (https://openjdk.org/jeps/223)
        final Set<String> versionPrefixes;

        JavaVersion(String... versionPrefixes) {
            this.versionPrefixes = new HashSet<>(Arrays.asList(versionPrefixes));
        }

        /**
         * The internal logic checks if the given version starts with the prefix of one of the enums preceding the current one.
         *
         * @param version the version to check
         * @return {@code true} if the version represented by this enum is older than or equal (in its minor and major version) to a given version
         */
        boolean isOlderOrEqualTo(String version) {
            // go through all previous enums
            JavaVersion[] allJavaVersionPrefixes = JavaVersion.values();
            for (int n = ordinal() - 1; n > -1; n--) {
                if (allJavaVersionPrefixes[n].versionPrefixes.stream().anyMatch(version::startsWith)) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Compile the java sources in a external process, calling an external executable,
     * like javac.
     *
     * @param config     compiler configuration
     * @param executable name of the executable to launch
     * @param args       arguments for the executable launched
     * @return a CompilerResult object encapsulating the result of the compilation and any compiler messages
     * @throws CompilerException
     */
    protected CompilerResult compileOutOfProcess(CompilerConfiguration config, String executable, String[] args)
            throws CompilerException {
        Commandline cli = new Commandline();

        cli.setWorkingDirectory(config.getWorkingDirectory().getAbsolutePath());
        cli.setExecutable(executable);

        try {
            File argumentsFile =
                    createFileWithArguments(args, config.getBuildDirectory().getAbsolutePath());
            cli.addArguments(
                    new String[] {"@" + argumentsFile.getCanonicalPath().replace(File.separatorChar, '/')});

            if (!StringUtils.isEmpty(config.getMaxmem())) {
                cli.addArguments(new String[] {"-J-Xmx" + config.getMaxmem()});
            }
            if (!StringUtils.isEmpty(config.getMeminitial())) {
                cli.addArguments(new String[] {"-J-Xms" + config.getMeminitial()});
            }

            for (String key : config.getCustomCompilerArgumentsAsMap().keySet()) {
                if (StringUtils.isNotEmpty(key) && key.startsWith("-J")) {
                    cli.addArguments(new String[] {key});
                }
            }
        } catch (IOException e) {
            throw new CompilerException("Error creating file with javac arguments", e);
        }

        CommandLineUtils.StringStreamConsumer out = new CommandLineUtils.StringStreamConsumer();
        int returnCode;
        List<CompilerMessage> messages;

        if (getLog().isDebugEnabled()) {
            String debugFileName = StringUtils.isEmpty(config.getDebugFileName()) ? "javac" : config.getDebugFileName();

            File commandLineFile = new File(
                    config.getBuildDirectory(),
                    StringUtils.trim(debugFileName) + "." + (Os.isFamily(Os.FAMILY_WINDOWS) ? "bat" : "sh"));
            try {
                FileUtils.fileWrite(
                        commandLineFile.getAbsolutePath(), cli.toString().replaceAll("'", ""));

                if (!Os.isFamily(Os.FAMILY_WINDOWS)) {
                    Runtime.getRuntime().exec(new String[] {"chmod", "a+x", commandLineFile.getAbsolutePath()});
                }
            } catch (IOException e) {
                if (getLog().isWarnEnabled()) {
                    getLog().warn("Unable to write '" + commandLineFile.getName() + "' debug script file", e);
                }
            }
        }

        try {
            // TODO:
            //   Is it really helpful to parse stdOut and stdErr as a single stream, instead of taking the chance to
            //   draw extra information from the fact that normal javac output is written to stdOut, while warnings and
            //   errors are written to stdErr? Of course, chronological correlation of messages would be more difficult
            //   then, but basically, we are throwing away information here.
            returnCode = CommandLineUtils.executeCommandLine(cli, out, out);

            if (getLog().isDebugEnabled()) {
                getLog().debug("Compiler output:{}{}", EOL, out.getOutput());
            }

            Path logsDir = config.getBuildDirectory().toPath().resolve("compiler-logs");
            if (!Files.exists(logsDir)) {
                Files.createDirectories(logsDir);
            }
            SimpleDateFormat dateFormat = new SimpleDateFormat("'javac'_yyyy-MM-dd'T'HH_mm_ss.'log'");
            String logFileName = dateFormat.format(new Date());
            Files.write(logsDir.resolve(logFileName), out.getOutput().getBytes(StandardCharsets.UTF_8));

            messages = parseModernStream(returnCode, new BufferedReader(new StringReader(out.getOutput())));
        } catch (CommandLineException | IOException e) {
            throw new CompilerException("Error while executing the external compiler.", e);
        }

        boolean success = returnCode == 0;
        return new CompilerResult(success, messages);
    }

    /**
     * Compile the java sources in the current JVM, without calling an external executable,
     * using <code>com.sun.tools.javac.Main</code> class
     *
     * @param args   arguments for the compiler as they would be used in the command line javac
     * @param config compiler configuration
     * @return a CompilerResult object encapsulating the result of the compilation and any compiler messages
     * @throws CompilerException
     */
    CompilerResult compileInProcess(String[] args, CompilerConfiguration config) throws CompilerException {
        final Class<?> javacClass = getJavacClass(config);
        final Thread thread = Thread.currentThread();
        final ClassLoader contextClassLoader = thread.getContextClassLoader();
        thread.setContextClassLoader(javacClass.getClassLoader());
        if (getLog().isDebugEnabled()) {
            getLog().debug("ttcl changed run compileInProcessWithProperClassloader");
        }
        try {
            return compileInProcessWithProperClassloader(javacClass, args);
        } finally {
            releaseJavaccClass(javacClass, config);
            thread.setContextClassLoader(contextClassLoader);
        }
    }

    protected CompilerResult compileInProcessWithProperClassloader(Class<?> javacClass, String[] args)
            throws CompilerException {
        return compileInProcess0(javacClass, args);
    }

    /**
     * Helper method for compileInProcess()
     */
    private CompilerResult compileInProcess0(Class<?> javacClass, String[] args) throws CompilerException {
        StringWriter out = new StringWriter();
        Integer ok;
        List<CompilerMessage> messages;

        try {
            Method compile = javacClass.getMethod("compile", new Class[] {String[].class, PrintWriter.class});
            ok = (Integer) compile.invoke(null, new Object[] {args, new PrintWriter(out)});

            if (getLog().isDebugEnabled()) {
                getLog().debug("Compiler output:{}{}", EOL, out.toString());
            }

            messages = parseModernStream(ok, new BufferedReader(new StringReader(out.toString())));
        } catch (NoSuchMethodException | IOException | InvocationTargetException | IllegalAccessException e) {
            throw new CompilerException("Error while executing the compiler.", e);
        }

        boolean success = ok == 0;
        return new CompilerResult(success, messages);
    }

    // Match ~95% of existing JDK exception name patterns (last checked for JDK 21)
    private static final Pattern STACK_TRACE_FIRST_LINE = Pattern.compile("^(?:[\\w+.-]+\\.)[\\w$]*?(?:"
            + "Exception|Error|Throwable|Failure|Result|Abort|Fault|ThreadDeath|Overflow|Warning|"
            + "NotSupported|NotFound|BadArgs|BadClassFile|Illegal|Invalid|Unexpected|Unchecked|Unmatched\\w+"
            + ").*$");

    // Match exception causes, existing and omitted stack trace elements
    private static final Pattern STACK_TRACE_OTHER_LINE =
            Pattern.compile("^(?:Caused by:\\s.*|\\s*at .*|\\s*\\.\\.\\.\\s\\d+\\smore)$");

    /**
     * Parse the compiler output into a list of compiler messages
     *
     * @param exitCode javac exit code (0 on success, non-zero otherwise)
     * @param input    compiler output (stdOut and stdErr merged into input stream)
     * @return list of {@link CompilerMessage} objects
     * @throws IOException if there is a problem reading from the input reader
     */
    static List<CompilerMessage> parseModernStream(int exitCode, BufferedReader input) throws IOException {
        List<CompilerMessage> errors = new ArrayList<>();
        String line;
        StringBuilder buffer = new StringBuilder();
        boolean hasPointer = false;
        int stackTraceLineCount = 0;

        while ((line = input.readLine()) != null) {
            if (stackTraceLineCount == 0 && STACK_TRACE_FIRST_LINE.matcher(line).matches()
                    || STACK_TRACE_OTHER_LINE.matcher(line).matches()) {
                stackTraceLineCount++;
            } else {
                stackTraceLineCount = 0;
            }

            // new error block?
            if (!line.startsWith(" ") && hasPointer) {
                // add the error bean
                errors.add(parseModernError(exitCode, buffer.toString()));
                // reset for next error block
                buffer = new StringBuilder(); // this is quicker than clearing it
                hasPointer = false;
            }

            if (buffer.length() == 0) {
                // try to classify output line by type (error, warning etc.)
                // TODO: there should be a better way to parse these
                if (isError(line)) {
                    errors.add(new CompilerMessage(line, ERROR));
                } else if (isWarning(line)) {
                    errors.add(new CompilerMessage(line, WARNING));
                } else if (isNote(line)) {
                    // skip, JDK telling us deprecated APIs are used but -Xlint:deprecation isn't set
                } else if (isMisc(line)) {
                    // verbose output was set
                    errors.add(new CompilerMessage(line, CompilerMessage.Kind.OTHER));
                } else {
                    // add first unclassified line to buffer
                    buffer.append(line).append(EOL);
                }
            } else {
                // add next unclassified line to buffer
                buffer.append(line).append(EOL);
            }

            if (line.endsWith("^")) {
                hasPointer = true;
            }
        }

        String bufferContent = buffer.toString();
        if (bufferContent.isEmpty()) {
            return errors;
        }

        // javac output not detected by other parsing
        // maybe better to ignore only the summary and mark the rest as error
        String cleanedUpMessage;
        if ((cleanedUpMessage = getJavacGenericError(bufferContent)) != null
                || (cleanedUpMessage = getBootLayerInitError(bufferContent)) != null
                || (cleanedUpMessage = getVMInitError(bufferContent)) != null
                || (cleanedUpMessage = getFileABugError(bufferContent)) != null
                || (cleanedUpMessage = getAnnotationProcessingError(bufferContent)) != null
                || (cleanedUpMessage = getSystemOutOfResourcesError(bufferContent)) != null
                || (cleanedUpMessage = getIOError(bufferContent)) != null
                || (cleanedUpMessage = getPluginError(bufferContent)) != null) {
            errors.add(new CompilerMessage(cleanedUpMessage, ERROR));
        } else if (hasPointer) {
            // A compiler message remains in buffer at end of parse stream
            errors.add(parseModernError(exitCode, bufferContent));
        } else if (stackTraceLineCount > 0) {
            // Extract stack trace from end of buffer
            String[] lines = bufferContent.split("\\R");
            int linesTotal = lines.length;
            buffer = new StringBuilder();
            int firstLine = linesTotal - stackTraceLineCount;
            for (int i = firstLine; i < linesTotal; i++) {
                buffer.append(lines[i]).append(EOL);
            }
            errors.add(new CompilerMessage(buffer.toString(), ERROR));
        }
        // TODO: Add something like this? Check if it creates more value or more unnecessary log output in general.
        // else {
        //     // Fall-back, if still no error or stack trace was recognised
        //     errors.add(new CompilerMessage(bufferContent, exitCode == 0 ? OTHER : ERROR));
        // }

        return errors;
    }

    private static boolean isMisc(String message) {
        return startsWithPrefix(message, MISC_PREFIXES);
    }

    private static boolean isNote(String message) {
        return startsWithPrefix(message, NOTE_PREFIXES);
    }

    private static boolean isWarning(String message) {
        return startsWithPrefix(message, WARNING_PREFIXES);
    }

    private static boolean isError(String message) {
        return startsWithPrefix(message, ERROR_PREFIXES);
    }

    private static String getJavacGenericError(String message) {
        return getTextStartingWithPrefix(message, JAVAC_GENERIC_ERROR_PREFIXES);
    }

    private static String getVMInitError(String message) {
        return getTextStartingWithPrefix(message, VM_INIT_ERROR_HEADERS);
    }

    private static String getBootLayerInitError(String message) {
        return getTextStartingWithPrefix(message, BOOT_LAYER_INIT_ERROR_HEADERS);
    }

    private static String getFileABugError(String message) {
        return getTextStartingWithPrefix(message, FILE_A_BUG_ERROR_HEADERS);
    }

    private static String getAnnotationProcessingError(String message) {
        return getTextStartingWithPrefix(message, ANNOTATION_PROCESSING_ERROR_HEADERS);
    }

    private static String getSystemOutOfResourcesError(String message) {
        return getTextStartingWithPrefix(message, SYSTEM_OUT_OF_RESOURCES_ERROR_HEADERS);
    }

    private static String getIOError(String message) {
        return getTextStartingWithPrefix(message, IO_ERROR_HEADERS);
    }

    private static String getPluginError(String message) {
        return getTextStartingWithPrefix(message, PLUGIN_ERROR_HEADERS);
    }

    private static boolean startsWithPrefix(String text, String[] prefixes) {
        for (String prefix : prefixes) {
            if (text.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Identify and return a known javac error message prefix and all subsequent text - usually a stack trace - from a
     * javac log output buffer.
     *
     * @param text     log buffer to search for a javac error message stack trace
     * @param prefixes array of strings in Java properties format, e.g. {@code "some error with line feed\nand parameter
     *                 placeholders {0} and {1}"} in multiple locales (hence the array). For the search, the
     *                 placeholders may be represented by any text in the log buffer.
     * @return if found, the error message + all subsequent text, otherwise {@code null}
     */
    static String getTextStartingWithPrefix(String text, String[] prefixes) {
        // Implementation note: The properties format with placeholders  makes it easy to just copy & paste values from
        // the JDK compared to having to convert them to regular expressions with ".*" instead of "{0}" and quote
        // special regex characters. This makes the implementation of this method more complex and potentially a bit
        // slower, but hopefully is worth the effort for the convenience of future developers maintaining this class.

        // Normalise line feeds to the UNIX format found in JDK multi-line messages in properties files
        text = text.replaceAll("\\R", "\n");

        // Search text for given error message prefixes/headers, until the first match is found
        for (String prefix : prefixes) {
            // Split properties message along placeholders like "{0}", "{1}" etc.
            String[] prefixParts = prefix.split("\\{\\d+\\}");
            for (int i = 0; i < prefixParts.length; i++) {
                // Make sure to treat split sections as literal text in search regex by enclosing them in "\Q" and "\E".
                // See https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html, search for "Quotation".
                prefixParts[i] = "\\Q" + prefixParts[i] + "\\E";
            }
            // Join message parts, replacing properties placeholders by ".*" regex ones
            prefix = String.join(".*?", prefixParts);
            // Find prefix + subsequent text in Pattern.DOTALL mode, represented in regex as "(?s)".
            // This matches across line break boundaries.
            Matcher matcher = Pattern.compile("(?s).*(" + prefix + ".*)").matcher(text);
            if (matcher.matches()) {
                // Match -> cut off text before header and replace UNIX line breaks by platform ones again
                return matcher.replaceFirst("$1").replaceAll("\n", EOL);
            }
        }

        // No match
        return null;
    }

    /**
     * Construct a compiler message object from a compiler output line
     *
     * @param exitCode javac exit code
     * @param error    compiler output line
     * @return compiler message object
     */
    static CompilerMessage parseModernError(int exitCode, String error) {
        final StringTokenizer tokens = new StringTokenizer(error, ":");
        CompilerMessage.Kind messageKind = exitCode == 0 ? WARNING : ERROR;

        try {
            // With Java 6 error output lines from the compiler got longer. For backward compatibility
            // and the time being, we eat up all (if any) tokens up to the erroneous file and source
            // line indicator tokens.

            boolean tokenIsAnInteger;
            StringBuilder file = null;
            String currentToken = null;

            do {
                if (currentToken != null) {
                    if (file == null) {
                        file = new StringBuilder(currentToken);
                    } else {
                        file.append(':').append(currentToken);
                    }
                }

                currentToken = tokens.nextToken();
                // Probably the only backward compatible means of checking if a string is an integer.
                tokenIsAnInteger = true;

                try {
                    Integer.parseInt(currentToken);
                } catch (NumberFormatException e) {
                    tokenIsAnInteger = false;
                }
            } while (!tokenIsAnInteger);

            final String lineIndicator = currentToken;
            final int startOfFileName = Objects.requireNonNull(file).toString().lastIndexOf(']');
            if (startOfFileName > -1) {
                file = new StringBuilder(file.substring(startOfFileName + 1 + EOL.length()));
            }

            final int line = Integer.parseInt(lineIndicator);
            final StringBuilder msgBuffer = new StringBuilder();
            String msg = tokens.nextToken(EOL).substring(2);

            // Remove "error: " and "warning: " prefixes
            String prefix;
            if ((prefix = getErrorPrefix(msg)) != null) {
                messageKind = ERROR;
                msg = msg.substring(prefix.length());
            } else if ((prefix = getWarningPrefix(msg)) != null) {
                messageKind = WARNING;
                msg = msg.substring(prefix.length());
            }
            msgBuffer.append(msg).append(EOL);

            String context = tokens.nextToken(EOL);
            String pointer = null;

            do {
                final String msgLine = tokens.nextToken(EOL);
                if (pointer != null) {
                    msgBuffer.append(msgLine);
                    msgBuffer.append(EOL);
                } else if (msgLine.endsWith("^")) {
                    pointer = msgLine;
                } else {
                    msgBuffer.append(context);
                    msgBuffer.append(EOL);
                    context = msgLine;
                }
            } while (tokens.hasMoreTokens());

            msgBuffer.append(EOL);

            final String message = msgBuffer.toString();
            final int startcolumn = Objects.requireNonNull(pointer).indexOf("^");
            int endcolumn = (context == null) ? startcolumn : context.indexOf(" ", startcolumn);
            if (endcolumn == -1) {
                endcolumn = Objects.requireNonNull(context).length();
            }

            return new CompilerMessage(
                    file.toString(), messageKind, line, startcolumn, line, endcolumn, message.trim());
        } catch (NoSuchElementException e) {
            return new CompilerMessage("no more tokens - could not parse error message: " + error, messageKind);
        } catch (Exception e) {
            return new CompilerMessage("could not parse error message: " + error, messageKind);
        }
    }

    private static String getMessagePrefix(String message, String[] prefixes) {
        for (String prefix : prefixes) {
            if (message.startsWith(prefix)) {
                return prefix;
            }
        }
        return null;
    }

    private static String getWarningPrefix(String message) {
        return getMessagePrefix(message, WARNING_PREFIXES);
    }

    private static String getErrorPrefix(String message) {
        return getMessagePrefix(message, ERROR_PREFIXES);
    }

    /**
     * put args into a temp file to be referenced using the @ option in javac command line
     *
     * @param args
     * @return the temporary file wth the arguments
     * @throws IOException
     */
    private File createFileWithArguments(String[] args, String outputDirectory) throws IOException {
        PrintWriter writer = null;
        try {
            File tempFile;
            if (getLog().isDebugEnabled()) {
                tempFile = File.createTempFile(JavacCompiler.class.getName(), "arguments", new File(outputDirectory));
            } else {
                tempFile = File.createTempFile(JavacCompiler.class.getName(), "arguments");
                tempFile.deleteOnExit();
            }

            writer = new PrintWriter(new FileWriter(tempFile));
            for (String arg : args) {
                String argValue = arg.replace(File.separatorChar, '/');
                writer.write("\"" + argValue + "\"");
                writer.println();
            }
            writer.flush();

            return tempFile;

        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    /**
     * Get the path of the javac tool executable to use.
     * Either given through explicit configuration or via {@link #getJavacExecutable()}.
     * @param config the configuration
     * @return the path of the javac tool
     */
    protected String getJavacExecutable(CompilerConfiguration config) {
        String executable = config.getExecutable();

        if (StringUtils.isEmpty(executable)) {
            try {
                executable = getJavacExecutable();
            } catch (IOException e) {
                if (getLog().isWarnEnabled()) {
                    getLog().warn("Unable to autodetect 'javac' path, using 'javac' from the environment.");
                }
                executable = "javac";
            }
        }
        return executable;
    }

    /**
     * Get the path of the javac tool executable: try to find it depending the OS or the <code>java.home</code>
     * system property or the <code>JAVA_HOME</code> environment variable.
     *
     * @return the path of the javac tool
     * @throws IOException if not found
     */
    private static String getJavacExecutable() throws IOException {
        String javacCommand = "javac" + (Os.isFamily(Os.FAMILY_WINDOWS) ? ".exe" : "");
        String javaHome = System.getProperty("java.home");
        File javacExe;

        if (Os.isName("AIX")) {
            javacExe = new File(javaHome + File.separator + ".." + File.separator + "sh", javacCommand);
        } else if (Os.isName("Mac OS X")) {
            javacExe = new File(javaHome + File.separator + "bin", javacCommand);
        } else {
            javacExe = new File(javaHome + File.separator + ".." + File.separator + "bin", javacCommand);
        }

        // ----------------------------------------------------------------------
        // Try to find javacExe from JAVA_HOME environment variable
        // ----------------------------------------------------------------------
        if (!javacExe.isFile()) {
            Properties env = CommandLineUtils.getSystemEnvVars();
            javaHome = env.getProperty("JAVA_HOME");
            if (StringUtils.isEmpty(javaHome)) {
                throw new IOException("The environment variable JAVA_HOME is not correctly set.");
            }
            if (!new File(javaHome).isDirectory()) {
                throw new IOException("The environment variable JAVA_HOME=" + javaHome
                        + " doesn't exist or is not a valid directory.");
            }
            javacExe = new File(env.getProperty("JAVA_HOME") + File.separator + "bin", javacCommand);
        }

        if (!javacExe.isFile()) {
            throw new IOException("The javadoc executable '" + javacExe
                    + "' doesn't exist or is not a file. Verify the JAVA_HOME environment variable.");
        }

        return javacExe.getAbsolutePath();
    }

    private void releaseJavaccClass(Class<?> javaccClass, CompilerConfiguration compilerConfiguration) {
        if (compilerConfiguration.getCompilerReuseStrategy()
                == CompilerConfiguration.CompilerReuseStrategy.ReuseCreated) {
            javacClasses.add(javaccClass);
        }
    }

    /**
     * Find the main class of JavaC. Return the same class for subsequent calls.
     *
     * @return the non-null class.
     * @throws CompilerException if the class has not been found.
     */
    private Class<?> getJavacClass(CompilerConfiguration compilerConfiguration) throws CompilerException {
        Class<?> c;
        switch (compilerConfiguration.getCompilerReuseStrategy()) {
            case AlwaysNew:
                return createJavacClass();
            case ReuseCreated:
                c = javacClasses.poll();
                if (c == null) {
                    c = createJavacClass();
                }
                return c;
            case ReuseSame:
            default:
                c = javacClass;
                if (c == null) {
                    synchronized (this) {
                        c = javacClass;
                        if (c == null) {
                            javacClass = c = createJavacClass();
                        }
                    }
                }
                return c;
        }
    }

    /**
     * Helper method for create Javac class
     */
    protected Class<?> createJavacClass() throws CompilerException {
        try {
            // look whether JavaC is on Maven's classpath
            // return Class.forName( JavacCompiler.JAVAC_CLASSNAME, true, JavacCompiler.class.getClassLoader() );
            return JavacCompiler.class.getClassLoader().loadClass(JavacCompiler.JAVAC_CLASSNAME);
        } catch (ClassNotFoundException ex) {
            // ok
        }

        final File toolsJar = new File(System.getProperty("java.home"), "../lib/tools.jar");
        if (!toolsJar.exists()) {
            throw new CompilerException("tools.jar not found: " + toolsJar);
        }

        try {
            // Combined classloader with no parent/child relationship, so classes in our classloader
            // can reference classes in tools.jar
            URL[] originalUrls = ((URLClassLoader) JavacCompiler.class.getClassLoader()).getURLs();
            URL[] urls = new URL[originalUrls.length + 1];
            urls[0] = toolsJar.toURI().toURL();
            System.arraycopy(originalUrls, 0, urls, 1, originalUrls.length);
            ClassLoader javacClassLoader = new URLClassLoader(urls);

            final Thread thread = Thread.currentThread();
            final ClassLoader contextClassLoader = thread.getContextClassLoader();
            thread.setContextClassLoader(javacClassLoader);
            try {
                // return Class.forName( JavacCompiler.JAVAC_CLASSNAME, true, javacClassLoader );
                return javacClassLoader.loadClass(JavacCompiler.JAVAC_CLASSNAME);
            } finally {
                thread.setContextClassLoader(contextClassLoader);
            }
        } catch (MalformedURLException ex) {
            throw new CompilerException(
                    "Could not convert the file reference to tools.jar to a URL, path to tools.jar: '"
                            + toolsJar.getAbsolutePath() + "'.",
                    ex);
        } catch (ClassNotFoundException ex) {
            throw new CompilerException(
                    "Unable to locate the Javac Compiler in:" + EOL + "  " + toolsJar + EOL
                            + "Please ensure you are using JDK 1.4 or above and" + EOL
                            + "not a JRE (the com.sun.tools.javac.Main class is required)." + EOL
                            + "In most cases you can change the location of your Java" + EOL
                            + "installation by setting the JAVA_HOME environment variable.",
                    ex);
        }
    }
}
