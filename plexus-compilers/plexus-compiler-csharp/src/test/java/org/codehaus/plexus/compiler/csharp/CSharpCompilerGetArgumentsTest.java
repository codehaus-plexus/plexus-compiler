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
import java.util.HashMap;
import java.util.Map;

import org.codehaus.plexus.compiler.CompilerConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link CSharpCompiler#getCompilerArguments(CompilerConfiguration)} method.
 * Tests the normalization of legacy colon-separated argument formats to key-value pairs.
 */
public class CSharpCompilerGetArgumentsTest {

    private CSharpCompiler compiler;
    private CompilerConfiguration config;

    @BeforeEach
    public void setUp() {
        compiler = new CSharpCompiler();
        config = new CompilerConfiguration();
    }

    @Test
    public void testEmptyArguments() {
        // Test with no custom arguments
        Map<String, String> result = compiler.getCompilerArguments(config);

        assertNotNull(result, "Result should not be null");
        assertTrue(result.isEmpty(), "Result should be empty when no custom arguments");
    }

    @Test
    public void testNormalArgumentsPassThrough() {
        // Test that normal key-value arguments pass through unchanged
        Map<String, String> customArgs = new HashMap<>();
        customArgs.put("-target", "library");
        customArgs.put("-debug", "full");
        customArgs.put("-nologo", "true");

        config.setCustomCompilerArgumentsAsMap(customArgs);
        Map<String, String> result = compiler.getCompilerArguments(config);

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("library", result.get("-target"));
        assertEquals("full", result.get("-debug"));
        assertEquals("true", result.get("-nologo"));
    }

    @Test
    public void testLegacyColonSeparatedFormat() {
        // Test that legacy format "-main:MyClass" is normalized to "-main" -> "MyClass"
        Map<String, String> customArgs = new HashMap<>();
        customArgs.put("-main:MyClass", null);

        config.setCustomCompilerArgumentsAsMap(customArgs);
        Map<String, String> result = compiler.getCompilerArguments(config);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("MyClass", result.get("-main"));
        assertFalse(result.containsKey("-main:MyClass"), "Original format key should not exist");
    }

    @Test
    public void testMultipleLegacyArguments() {
        // Test multiple legacy format arguments
        Map<String, String> customArgs = new HashMap<>();
        customArgs.put("-main:Program", null);
        customArgs.put("-doc:MyDoc.xml", null);
        customArgs.put("-out:MyApp.dll", null);

        config.setCustomCompilerArgumentsAsMap(customArgs);
        Map<String, String> result = compiler.getCompilerArguments(config);

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("Program", result.get("-main"));
        assertEquals("MyDoc.xml", result.get("-doc"));
        assertEquals("MyApp.dll", result.get("-out"));
    }

    @Test
    public void testMixedNormalAndLegacyArguments() {
        // Test combination of normal and legacy format arguments
        Map<String, String> customArgs = new HashMap<>();
        customArgs.put("-target", "library");
        customArgs.put("-main:MyApp", null);
        customArgs.put("-debug", "pdbonly");
        customArgs.put("-nowarn:CS0168,CS0219", null);
        customArgs.put("-unsafe", "true");

        config.setCustomCompilerArgumentsAsMap(customArgs);
        Map<String, String> result = compiler.getCompilerArguments(config);

        assertNotNull(result);
        assertEquals(5, result.size());
        assertEquals("library", result.get("-target"));
        assertEquals("MyApp", result.get("-main"));
        assertEquals("pdbonly", result.get("-debug"));
        assertEquals("CS0168,CS0219", result.get("-nowarn"));
        assertEquals("true", result.get("-unsafe"));
    }

    @Test
    public void testColonSeparatedWithValue() {
        // Test that colon-separated arguments with actual values are NOT treated as legacy
        Map<String, String> customArgs = new HashMap<>();
        customArgs.put("-pathmap:old=new", "somevalue");

        config.setCustomCompilerArgumentsAsMap(customArgs);
        Map<String, String> result = compiler.getCompilerArguments(config);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("somevalue", result.get("-pathmap:old=new"));
        assertNull(result.get("-pathmap"), "Should not split when value is present");
    }

    @Test
    public void testMultipleColonsInLegacyFormat() {
        // Test arguments with multiple colons (e.g., paths)
        Map<String, String> customArgs = new HashMap<>();
        customArgs.put("-keyfile:C:\\path\\to\\key.snk", null);

        config.setCustomCompilerArgumentsAsMap(customArgs);
        Map<String, String> result = compiler.getCompilerArguments(config);

        assertNotNull(result);
        assertEquals(1, result.size());
        // Should split at first colon only
        assertEquals("C:\\path\\to\\key.snk", result.get("-keyfile"));
    }

    @Test
    public void testEmptyValueAfterColon() {
        // Test legacy format with empty value after colon
        Map<String, String> customArgs = new HashMap<>();
        customArgs.put("-somearg:", null);

        config.setCustomCompilerArgumentsAsMap(customArgs);
        Map<String, String> result = compiler.getCompilerArguments(config);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("", result.get("-somearg"));
    }

    @Test
    public void testStatelessBehavior() {
        // Test that method is stateless - multiple calls with different configs
        // should not affect each other
        Map<String, String> customArgs1 = new HashMap<>();
        customArgs1.put("-main:App1", null);
        customArgs1.put("-target", "exe");

        CompilerConfiguration config1 = new CompilerConfiguration();
        config1.setCustomCompilerArgumentsAsMap(customArgs1);

        Map<String, String> result1 = compiler.getCompilerArguments(config1);

        // Second call with different arguments
        Map<String, String> customArgs2 = new HashMap<>();
        customArgs2.put("-main:App2", null);
        customArgs2.put("-target", "library");

        CompilerConfiguration config2 = new CompilerConfiguration();
        config2.setCustomCompilerArgumentsAsMap(customArgs2);

        Map<String, String> result2 = compiler.getCompilerArguments(config2);

        // Verify first result wasn't affected by second call
        assertEquals("App1", result1.get("-main"));
        assertEquals("exe", result1.get("-target"));

        // Verify second result is correct
        assertEquals("App2", result2.get("-main"));
        assertEquals("library", result2.get("-target"));
    }

    @Test
    public void testDoesNotModifyOriginalConfig() {
        // Test that original config map is not modified
        Map<String, String> customArgs = new HashMap<>();
        customArgs.put("-main:MyClass", null);
        customArgs.put("-target", "library");

        config.setCustomCompilerArgumentsAsMap(customArgs);

        // Store original state
        Map<String, String> originalArgs = new HashMap<>(config.getCustomCompilerArgumentsAsMap());

        // Call method
        compiler.getCompilerArguments(config);

        // Verify config wasn't modified
        Map<String, String> currentArgs = config.getCustomCompilerArgumentsAsMap();
        assertEquals(originalArgs.size(), currentArgs.size());
        assertTrue(currentArgs.containsKey("-main:MyClass"));
        assertEquals(originalArgs.get("-target"), currentArgs.get("-target"));
    }

    @Test
    public void testNullValueInNormalArgument() {
        // Test that null values in non-colon arguments are preserved
        Map<String, String> customArgs = new HashMap<>();
        customArgs.put("-someFlag", null);
        customArgs.put("-normalArg", "value");

        config.setCustomCompilerArgumentsAsMap(customArgs);
        Map<String, String> result = compiler.getCompilerArguments(config);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertNull(result.get("-someFlag"), "Null value should be preserved for non-colon args");
        assertEquals("value", result.get("-normalArg"));
    }

    @Test
    public void testRealWorldScenario() {
        // Test with realistic combination of arguments
        Map<String, String> customArgs = new HashMap<>();
        customArgs.put("-target", "library");
        customArgs.put("-out:MyLibrary.dll", null);
        customArgs.put("-debug:full", null);
        customArgs.put("-nowarn:CS0168", null);
        customArgs.put("-nologo", "true");
        customArgs.put("-unsafe", "false");
        customArgs.put("-doc:Documentation.xml", null);
        customArgs.put("-main:Program", null);

        config.setCustomCompilerArgumentsAsMap(customArgs);
        Map<String, String> result = compiler.getCompilerArguments(config);

        assertNotNull(result);
        assertEquals(8, result.size());

        // Verify normal arguments
        assertEquals("library", result.get("-target"));
        assertEquals("true", result.get("-nologo"));
        assertEquals("false", result.get("-unsafe"));

        // Verify normalized legacy arguments
        assertEquals("MyLibrary.dll", result.get("-out"));
        assertEquals("full", result.get("-debug"));
        assertEquals("CS0168", result.get("-nowarn"));
        assertEquals("Documentation.xml", result.get("-doc"));
        assertEquals("Program", result.get("-main"));
    }

    @Test
    public void testScenarioFromPR() throws Exception {
        Map<String, String> customArgs = new HashMap<>();
        customArgs.put("-nologo:true", null);
        customArgs.put("-nowarn:0414", null);
        customArgs.put("-utf8output:true", null);
        customArgs.put("-target:exe", null);
        customArgs.put("-out:${project.artifactId}.exe", null);

        config.setCustomCompilerArgumentsAsMap(customArgs);
        String[] sourceFiles = {"src/main/csharp/App.cs"};
        String[] result = compiler.buildCompilerArguments(config, sourceFiles);

        assertNotNull(result);
        assertEquals(6, result.length);

        // Verify
        assertEquals("/nowarn:0414", result[0]);
        assertTrue(result[1].startsWith("/out:"));
        assertEquals("/target:exe", result[2]);
        assertEquals("/nologo", result[3]);
        assertEquals("/utf8output", result[4]);
        assertTrue(result[5].endsWith("App.cs"));
    }
}
