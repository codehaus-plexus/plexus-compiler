package org.codehaus.plexus.compiler.javac;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.stream.Stream;

import org.codehaus.plexus.compiler.CompilerMessage;
import org.codehaus.plexus.compiler.javac.JavacCompiler.JavaVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * @author Olivier Lamy
 * @author Alexander Kriegisch
 */
public class JavacCompilerTest extends AbstractJavacCompilerTest {
    private static final String EOL = System.getProperty("line.separator");

    @BeforeEach
    public void setUp() {
        super.setUp();
        setForceJavacCompilerUse(true);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("testParseModernStream_withAnnotationProcessingErrors_args")
    void testParseModernStream_withAnnotationProcessingErrors(String jdkAndLocale, String stackTraceHeader)
            throws IOException {
        String stackTraceWithHeader = stackTraceHeader + stackTraceAnnotationProcessingError;
        List<CompilerMessage> compilerMessages =
                JavacCompiler.parseModernStream(1, new BufferedReader(new StringReader(stackTraceWithHeader)));

        assertThat(compilerMessages, notNullValue());
        assertThat(compilerMessages, hasSize(1));

        String message = compilerMessages.get(0).getMessage().replaceAll(EOL, "\n");
        // Parser does not retain stack trace header, because it is hard to identify in a locale-independent way
        assertThat(message, not(startsWith(stackTraceHeader)));
        assertThat(message, endsWith(stackTraceAnnotationProcessingError));
    }

    private static final String stackTraceAnnotationProcessingError =
            "java.lang.IllegalAccessError: class lombok.javac.apt.LombokProcessor (in unnamed module @0x1da51a35) cannot access class com.sun.tools.javac.processing.JavacProcessingEnvironment (in module jdk.compiler) because module jdk.compiler does not export com.sun.tools.javac.processing to unnamed module @0x1da51a35\n"
                    + "\tat lombok.javac.apt.LombokProcessor.getJavacProcessingEnvironment(LombokProcessor.java:433)\n"
                    + "\tat lombok.javac.apt.LombokProcessor.init(LombokProcessor.java:92)\n"
                    + "\tat lombok.core.AnnotationProcessor$JavacDescriptor.want(AnnotationProcessor.java:160)\n"
                    + "\tat lombok.core.AnnotationProcessor.init(AnnotationProcessor.java:213)\n"
                    + "\tat lombok.launch.AnnotationProcessorHider$AnnotationProcessor.init(AnnotationProcessor.java:64)\n"
                    + "\tat jdk.compiler/com.sun.tools.javac.processing.JavacProcessingEnvironment$ProcessorState.<init>(JavacProcessingEnvironment.java:702)\n"
                    + "\tat jdk.compiler/com.sun.tools.javac.processing.JavacProcessingEnvironment$DiscoveredProcessors$ProcessorStateIterator.next(JavacProcessingEnvironment.java:829)\n"
                    + "\tat jdk.compiler/com.sun.tools.javac.processing.JavacProcessingEnvironment.discoverAndRunProcs(JavacProcessingEnvironment.java:925)\n"
                    + "\tat jdk.compiler/com.sun.tools.javac.processing.JavacProcessingEnvironment$Round.run(JavacProcessingEnvironment.java:1269)\n"
                    + "\tat jdk.compiler/com.sun.tools.javac.processing.JavacProcessingEnvironment.doProcessing(JavacProcessingEnvironment.java:1384)\n"
                    + "\tat jdk.compiler/com.sun.tools.javac.main.JavaCompiler.processAnnotations(JavaCompiler.java:1261)\n"
                    + "\tat jdk.compiler/com.sun.tools.javac.main.JavaCompiler.compile(JavaCompiler.java:935)\n"
                    + "\tat jdk.compiler/com.sun.tools.javac.main.Main.compile(Main.java:317)\n"
                    + "\tat jdk.compiler/com.sun.tools.javac.main.Main.compile(Main.java:176)\n"
                    + "\tat jdk.compiler/com.sun.tools.javac.Main.compile(Main.java:64)\n"
                    + "\tat jdk.compiler/com.sun.tools.javac.Main.main(Main.java:50)\n";

    private static Stream<Arguments> testParseModernStream_withAnnotationProcessingErrors_args() {
        return Stream.of(
                Arguments.of(
                        "JDK 8 English",
                        "\n\nAn annotation processor threw an uncaught exception.\nConsult the following stack trace for details.\n\n"),
                Arguments.of("JDK 8 Japanese", "\n\n注釈処理で捕捉されない例外がスローされました。\n詳細は次のスタック・トレースで調査してください。\n\n"),
                Arguments.of("JDK 8 Chinese", "\n\n注释处理程序抛出未捕获的异常错误。\n有关详细信息, 请参阅以下堆栈跟踪。\n\n"),
                Arguments.of(
                        "JDK 21 English",
                        "\n\nAn annotation processor threw an uncaught exception.\nConsult the following stack trace for details.\n\n"),
                Arguments.of("JDK 21 Japanese", "\n\n注釈処理で捕捉されない例外がスローされました。\n詳細は次のスタックトレースで調査してください。\n\n"),
                Arguments.of("JDK 21 Chinese", "\n\n批注处理程序抛出未捕获的异常错误。\n有关详细信息, 请参阅以下堆栈跟踪。\n\n"),
                Arguments.of(
                        "JDK 21 German",
                        "\n\nEin Annotationsprozessor hat eine nicht abgefangene Ausnahme ausgelöst.\nDetails finden Sie im folgenden Stacktrace.\n\n"));
    }

    @Test
    void testJavaVersionPrefixes() {
        assertFalse(JavaVersion.JAVA_1_4.isOlderOrEqualTo("1.3"));
        assertTrue(JavaVersion.JAVA_1_4.isOlderOrEqualTo("1.4"));
        assertTrue(JavaVersion.JAVA_1_4.isOlderOrEqualTo("1.4.0_something"));
        assertFalse(JavaVersion.JAVA_1_5.isOlderOrEqualTo("1.4"));
        assertTrue(JavaVersion.JAVA_1_8.isOlderOrEqualTo("1.8"));
        assertTrue(JavaVersion.JAVA_1_8.isOlderOrEqualTo("22.0.2-something"));
        assertTrue(JavaVersion.JAVA_1_8.isOlderOrEqualTo("unknown"));
    }

    @Test
    void testExtractMajorAndMinorVersion() {
        assertEquals("11.0", JavacCompiler.extractMajorAndMinorVersion("javac 11.0.22"));
        assertEquals("11.0", JavacCompiler.extractMajorAndMinorVersion("11.0.22"));
        assertEquals("21", JavacCompiler.extractMajorAndMinorVersion("javac 21"));
    }
}
