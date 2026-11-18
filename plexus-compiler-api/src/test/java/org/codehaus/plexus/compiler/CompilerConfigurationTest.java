package org.codehaus.plexus.compiler;

import java.util.Iterator;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CompilerConfigurationTest {
    private CompilerConfiguration configuration;

    @BeforeEach
    void setUp() {
        configuration = new CompilerConfiguration();
    }

    @Test
    @DisplayName("Duplicate custom arguments keep the last value in the map but all entries")
    void customArgumentsWithDuplicateKeys() {
        configuration.addCompilerCustomArgument("--add-exports", "FROM-MOD/package1=OTHER-MOD");
        configuration.addCompilerCustomArgument("--add-exports", "FROM-MOD/package2=OTHER-MOD");

        Map<String, String> arguments = configuration.getCustomCompilerArgumentsAsMap();
        assertEquals(1, arguments.size());
        assertEquals("FROM-MOD/package2=OTHER-MOD", arguments.get("--add-exports"));

        assertEquals(2, configuration.getCustomCompilerArgumentsEntries().size());
        Iterator<Map.Entry<String, String>> entries =
                configuration.getCustomCompilerArgumentsEntries().iterator();
        Map.Entry<String, String> first = entries.next();
        Map.Entry<String, String> second = entries.next();
        assertAll(
                () -> assertEquals("--add-exports", first.getKey()),
                () -> assertEquals("FROM-MOD/package1=OTHER-MOD", first.getValue()),
                () -> assertEquals("--add-exports", second.getKey()),
                () -> assertEquals("FROM-MOD/package2=OTHER-MOD", second.getValue()));
    }
}
