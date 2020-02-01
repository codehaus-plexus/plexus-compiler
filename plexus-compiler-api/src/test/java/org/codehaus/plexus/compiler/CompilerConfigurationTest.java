package org.codehaus.plexus.compiler;

import java.util.Iterator;
import java.util.Map;

import junit.framework.TestCase;

public class CompilerConfigurationTest
    extends TestCase
{
    private CompilerConfiguration configuration;
    
    @Override
    protected void setUp()
    {
        configuration = new CompilerConfiguration();
    }
    
    public void testCustomArguments() 
    {
        configuration.addCompilerCustomArgument( "--add-exports", "FROM-MOD/package1=OTHER-MOD" );
        configuration.addCompilerCustomArgument( "--add-exports", "FROM-MOD/package2=OTHER-MOD" );
        
        assertEquals( 1, configuration.getCustomCompilerArgumentsAsMap().size() );
        assertEquals( "FROM-MOD/package2=OTHER-MOD", configuration.getCustomCompilerArgumentsAsMap().get( "--add-exports" ) );
        
        assertEquals( 2, configuration.getCustomCompilerArgumentsEntries().size() );
        Iterator<Map.Entry<String,String>> entries = configuration.getCustomCompilerArgumentsEntries().iterator();
        Map.Entry<String,String> entry;
        
        entry = entries.next();
        assertEquals( "--add-exports", entry.getKey() );
        assertEquals( "FROM-MOD/package1=OTHER-MOD", entry.getValue() );
        entry = entries.next();
        assertEquals( "--add-exports", entry.getKey() );
        assertEquals( "FROM-MOD/package2=OTHER-MOD", entry.getValue() );
    }
}
