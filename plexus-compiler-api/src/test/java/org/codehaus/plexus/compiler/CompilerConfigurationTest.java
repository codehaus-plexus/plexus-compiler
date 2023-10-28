package org.codehaus.plexus.compiler;

import java.util.Iterator;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class CompilerConfigurationTest {
    private CompilerConfiguration configuration;

    @BeforeEach
    protected void setUp() throws Exception {
        configuration = new CompilerConfiguration();
    }

    @Test
    public void testCustomArguments() {
        configuration.addCompilerCustomArgument("--add-exports", "FROM-MOD/package1=OTHER-MOD");
        configuration.addCompilerCustomArgument("--add-exports", "FROM-MOD/package2=OTHER-MOD");

        assertThat(configuration.getCustomCompilerArgumentsAsMap().size(), is(1));
        assertThat(
                configuration.getCustomCompilerArgumentsAsMap().get("--add-exports"),
                is("FROM-MOD/package2=OTHER-MOD"));

        assertThat(configuration.getCustomCompilerArgumentsEntries().size(), is(2));
        Iterator<Map.Entry<String, String>> entries =
                configuration.getCustomCompilerArgumentsEntries().iterator();
        Map.Entry<String, String> entry;

        entry = entries.next();
        assertThat(entry.getKey(), is("--add-exports"));
        assertThat(entry.getValue(), is("FROM-MOD/package1=OTHER-MOD"));
        entry = entries.next();
        assertThat(entry.getKey(), is("--add-exports"));
        assertThat(entry.getValue(), is("FROM-MOD/package2=OTHER-MOD"));
    }
}
