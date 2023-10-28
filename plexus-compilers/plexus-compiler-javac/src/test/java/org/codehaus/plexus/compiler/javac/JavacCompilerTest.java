package org.codehaus.plexus.compiler.javac;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import org.codehaus.plexus.compiler.CompilerMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

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
 */
public class JavacCompilerTest extends AbstractJavacCompilerTest {
    @BeforeEach
    public void setUp() {
        super.setUp();
        setForceJavacCompilerUse(true);
    }

    @Test
    void parseModernStream_withAnnotationProcessingErrors() throws IOException {
        String input = "\n" + "\n"
                + "An annotation processor threw an uncaught exception.\n"
                + "Consult the following stack trace for details.\n"
                + "java.lang.IllegalAccessError: class lombok.javac.apt.LombokProcessor (in unnamed module @0x1da51a35) cannot access class com.sun.tools.javac.processing.JavacProcessingEnvironment (in module jdk.compiler) because module jdk.compiler does not export com.sun.tools.javac.processing to unnamed module @0x1da51a35\n"
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

        List<CompilerMessage> compilerMessages =
                JavacCompiler.parseModernStream(1, new BufferedReader(new StringReader(input)));

        assertThat(compilerMessages, hasSize(1));
    }
}
