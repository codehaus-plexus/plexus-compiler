package org.codehaus.plexus.compiler.csharp;

/*
 * Copyright 2026 The Apache Software Foundation.
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.plexus.compiler.CompilerConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link CSharpCompiler#getCompilerArguments(CompilerConfiguration)} method.
 * Tests the normalization of legacy colon-separated argument formats to key-value pairs.
 */
class CSharpCompilerGetArgumentsTest {

    private CSharpCompiler compiler;
    private CompilerConfiguration config;

    @BeforeEach
    void setUp() {
        compiler = new CSharpCompiler();
        config = new CompilerConfiguration();
    }

    @Nested
    @DisplayName("Legacy colon-separated arguments")
    class LegacyArguments {
        @Test
        void singleLegacyArgumentIsNormalized() {
            config.setCustomCompilerArgumentsAsMap(args("-main:MyClass", null));

            Map<String, String> result = compiler.getCompilerArguments(config);

            assertArguments(result, "-main", "MyClass");
            assertFalse(result.containsKey("-main:MyClass"), "Original format key should not exist");
        }

        @Test
        void multipleLegacyArgumentsAreNormalized() {
            config.setCustomCompilerArgumentsAsMap(
                    args("-main:Program", null, "-doc:MyDoc.xml", null, "-out:MyApp.dll", null));

            assertArguments(
                    compiler.getCompilerArguments(config),
                    "-main",
                    "Program",
                    "-doc",
                    "MyDoc.xml",
                    "-out",
                    "MyApp.dll");
        }

        @Test
        void normalAndLegacyArgumentsAreCombined() {
            config.setCustomCompilerArgumentsAsMap(args(
                    "-target",
                    "library",
                    "-main:MyApp",
                    null,
                    "-debug",
                    "pdbonly",
                    "-nowarn:CS0168,CS0219",
                    null,
                    "-unsafe",
                    "true"));

            assertArguments(
                    compiler.getCompilerArguments(config),
                    "-target",
                    "library",
                    "-main",
                    "MyApp",
                    "-debug",
                    "pdbonly",
                    "-nowarn",
                    "CS0168,CS0219",
                    "-unsafe",
                    "true");
        }

        @Test
        void splitsOnlyAtTheFirstColon() {
            config.setCustomCompilerArgumentsAsMap(args("-keyfile:C:\\path\\to\\key.snk", null));

            assertArguments(compiler.getCompilerArguments(config), "-keyfile", "C:\\path\\to\\key.snk");
        }

        @Test
        void emptyValueAfterColonIsPreserved() {
            config.setCustomCompilerArgumentsAsMap(args("-somearg:", null));

            assertArguments(compiler.getCompilerArguments(config), "-somearg", "");
        }

        @Test
        void realisticCombinationOfArguments() {
            config.setCustomCompilerArgumentsAsMap(args(
                    "-target",
                    "library",
                    "-out:MyLibrary.dll",
                    null,
                    "-debug:full",
                    null,
                    "-nowarn:CS0168",
                    null,
                    "-nologo",
                    "true",
                    "-unsafe",
                    "false",
                    "-doc:Documentation.xml",
                    null,
                    "-main:Program",
                    null));

            assertArguments(
                    compiler.getCompilerArguments(config),
                    "-target",
                    "library",
                    "-out",
                    "MyLibrary.dll",
                    "-debug",
                    "full",
                    "-nowarn",
                    "CS0168",
                    "-nologo",
                    "true",
                    "-unsafe",
                    "false",
                    "-doc",
                    "Documentation.xml",
                    "-main",
                    "Program");
        }
    }

    @Nested
    @DisplayName("Normal arguments")
    class NormalArguments {
        @Test
        void passThroughUnchanged() {
            config.setCustomCompilerArgumentsAsMap(args("-target", "library", "-debug", "full", "-nologo", "true"));

            assertArguments(
                    compiler.getCompilerArguments(config), "-target", "library", "-debug", "full", "-nologo", "true");
        }

        @Test
        void colonSeparatedKeyWithAValueIsNotTreatedAsLegacy() {
            config.setCustomCompilerArgumentsAsMap(args("-pathmap:old=new", "somevalue"));

            Map<String, String> result = compiler.getCompilerArguments(config);

            assertArguments(result, "-pathmap:old=new", "somevalue");
            assertNull(result.get("-pathmap"), "Should not split when value is present");
        }

        @Test
        void nullValueIsPreserved() {
            config.setCustomCompilerArgumentsAsMap(args("-someFlag", null, "-normalArg", "value"));

            assertArguments(compiler.getCompilerArguments(config), "-someFlag", null, "-normalArg", "value");
        }
    }

    @Nested
    @DisplayName("Compiler state")
    class CompilerState {
        @Test
        void noCustomArgumentsYieldsEmptyMap() {
            Map<String, String> result = compiler.getCompilerArguments(config);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        void callsAreStatelessAcrossConfigurations() {
            CompilerConfiguration first = new CompilerConfiguration();
            first.setCustomCompilerArgumentsAsMap(args("-main:App1", null, "-target", "exe"));
            CompilerConfiguration second = new CompilerConfiguration();
            second.setCustomCompilerArgumentsAsMap(args("-main:App2", null, "-target", "library"));

            Map<String, String> firstResult = compiler.getCompilerArguments(first);
            Map<String, String> secondResult = compiler.getCompilerArguments(second);

            assertAll(
                    () -> assertArguments(firstResult, "-main", "App1", "-target", "exe"),
                    () -> assertArguments(secondResult, "-main", "App2", "-target", "library"));
        }

        @Test
        void originalConfigurationIsNotModified() {
            config.setCustomCompilerArgumentsAsMap(args("-main:MyClass", null, "-target", "library"));
            Map<String, String> original = new HashMap<>(config.getCustomCompilerArgumentsAsMap());

            compiler.getCompilerArguments(config);

            Map<String, String> current = config.getCustomCompilerArgumentsAsMap();
            assertAll(() -> assertEquals(original, current), () -> assertTrue(current.containsKey("-main:MyClass")));
        }
    }

    @Test
    void scenarioFromPullRequest() throws Exception {
        config.setCustomCompilerArgumentsAsMap(args(
                "-nologo:true",
                null,
                "-nowarn:0414",
                null,
                "-utf8output:true",
                null,
                "-target:exe",
                null,
                "-out:${project.artifactId}.exe",
                null));

        String[] result = compiler.buildCompilerArguments(config, new String[] {"src/main/csharp/App.cs"});

        assertNotNull(result);
        assertEquals(6, result.length);
        assertAll(
                () -> assertEquals("/nowarn:0414", result[0]),
                () -> assertTrue(result[1].startsWith("/out:")),
                () -> assertEquals("/target:exe", result[2]),
                () -> assertEquals("/nologo", result[3]),
                () -> assertEquals("/utf8output", result[4]),
                () -> assertTrue(result[5].endsWith("App.cs")));
    }

    /** Builds a map from alternating key/value arguments; a {@code null} value is allowed. */
    private static Map<String, String> args(String... keysAndValues) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            map.put(keysAndValues[i], keysAndValues[i + 1]);
        }
        return map;
    }

    private static void assertArguments(Map<String, String> result, String... expectedPairs) {
        Map<String, String> expected = args(expectedPairs);
        List<Executable> checks = new ArrayList<>();
        checks.add(() -> assertEquals(expected.size(), result.size(), "argument count"));
        expected.forEach((key, value) -> checks.add(() -> assertEquals(value, result.get(key), "argument " + key)));
        assertAll(checks);
    }
}
