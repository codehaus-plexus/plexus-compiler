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

import org.codehaus.plexus.compiler.AbstractCompilerTest;
import org.codehaus.plexus.compiler.CompilerConfiguration;
import org.codehaus.plexus.util.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="mailto:jason@plexus.org">Jason van Zyl</a>
 */
public abstract class AbstractJavacCompilerTest
    extends AbstractCompilerTest
{
    private static final String PS = File.pathSeparator;

    @BeforeEach
    public void setUp()
        throws Exception
    {
        setCompilerDebug( true );
        setCompilerDeprecationWarnings( true );

    }

    @Override
    protected String getRoleHint()
    {
        return "javac";
    }

    @Override
    protected int expectedErrors()
    {
        String javaVersion = getJavaVersion();
        if (javaVersion.contains("9.0")||javaVersion.contains("11")||javaVersion.contains("14")||
            javaVersion.contains("15")||javaVersion.contains("16")||javaVersion.contains("17")){
            // lots of new warnings about obsoletions for future releases
            return 5;
        }
        // javac output changed for misspelled modifiers starting in 1.6...they now generate 2 errors per occurrence, not one.
        if ( "1.5".compareTo( javaVersion ) < 0 )
        {
            return 4;
        }
        else
        {
            return 3;
        }
    }

    @Override
    protected int expectedWarnings()
    {
        String javaVersion = getJavaVersion();
        if (javaVersion.contains("9.0")||javaVersion.contains("11")||javaVersion.contains("14")||
            javaVersion.contains("15")||javaVersion.contains("16")||javaVersion.contains("17")){
            return 1;
        }
        if (javaVersion.contains("9.0")){
            // lots of new warnings about obsoletions for future releases
            return 8;
        }

        if (javaVersion.contains("1.8")){
            // lots of new warnings about obsoletions for future releases
            return 30;
        }

        if ( "1.6".compareTo( javaVersion ) < 0 )
        {
            // with 1.7 some warning with bootstrap class path not set in conjunction with -source 1.3
            return 9;
        }

        return 2;
    }

    @Override
    public String getTargetVersion()
    {
        String javaVersion = getJavaVersion();
        if (javaVersion.contains("9.0")){
            return "1.7";
        }
        if (javaVersion.contains("11")){
            return "11";
        }
        if (javaVersion.contains("14")){
            return "14";
        }
        if (javaVersion.contains("15")){
            return "15";
        }
        if (javaVersion.contains("16")){
            return "16";
        }
        if (javaVersion.contains("17")){
            return "17";
        }
        return super.getTargetVersion();
    }

    @Override
    public String getSourceVersion()
    {
        String javaVersion = getJavaVersion();
        if (javaVersion.contains("9.0"))
        {
            return "1.7";
        }
        if (javaVersion.contains("11"))
        {
            return "11";
        }
        if (javaVersion.contains("14"))
        {
            return "14";
        }
        if (javaVersion.contains("15"))
        {
            return "15";
        }
        if (javaVersion.contains("16")){
            return "16";
        }
        if (javaVersion.contains("17")){
            return "17";
        }
        return super.getTargetVersion();
    }

    @Override
    protected Collection<String> expectedOutputFiles()
    {
        String javaVersion = getJavaVersion();
        if (javaVersion.contains("9.0")||javaVersion.contains("11")||javaVersion.contains("14")||
            javaVersion.contains("15")||javaVersion.contains("16")||javaVersion.contains("17")){
            return Arrays.asList( new String[]{ "org/codehaus/foo/Deprecation.class", "org/codehaus/foo/ExternalDeps.class",
                "org/codehaus/foo/Person.class"} );
        }
        return Arrays.asList( new String[]{ "org/codehaus/foo/Deprecation.class", "org/codehaus/foo/ExternalDeps.class",
            "org/codehaus/foo/Person.class", "org/codehaus/foo/ReservedWord.class" } );
    }

    public void internalTest(CompilerConfiguration compilerConfiguration, List<String> expectedArguments) {
        internalTest(compilerConfiguration, expectedArguments, new String[0]);
    }

    public void internalTest(CompilerConfiguration compilerConfiguration, List<String> expectedArguments, String[] sources)
    {
        String[] actualArguments = JavacCompiler.buildCompilerArguments( compilerConfiguration, sources );

        assertThat( actualArguments ).as( "The expected and actual argument list sizes differ." ).hasSize( expectedArguments.size() );

        for ( int i = 0; i < actualArguments.length; i++ )
        {
            assertThat( actualArguments[i] ).as( "Unexpected argument").isEqualTo( expectedArguments.get( i ) );
        }
    }

    @Test
    public void testBuildCompilerArgs13()
    {
        List<String> expectedArguments = new ArrayList<>();

        CompilerConfiguration compilerConfiguration = new CompilerConfiguration();

        compilerConfiguration.setCompilerVersion( "1.3" );

        populateArguments( compilerConfiguration, expectedArguments, true, true, false );

        internalTest( compilerConfiguration, expectedArguments );
    }

    @Test
    public void testBuildCompilerArgs14()
    {
        List<String> expectedArguments = new ArrayList<>();

        CompilerConfiguration compilerConfiguration = new CompilerConfiguration();

        compilerConfiguration.setCompilerVersion( "1.4" );

        populateArguments( compilerConfiguration, expectedArguments, false, false, false );

        internalTest( compilerConfiguration, expectedArguments );
    }

    @Test
    public void testBuildCompilerArgs15()
    {
        List<String> expectedArguments = new ArrayList<>();

        CompilerConfiguration compilerConfiguration = new CompilerConfiguration();

        compilerConfiguration.setCompilerVersion( "1.5" );

        populateArguments( compilerConfiguration, expectedArguments, false, false, false );

        internalTest( compilerConfiguration, expectedArguments );
    }

    @Test
    public void testBuildCompilerArgs18()
    {
        List<String> expectedArguments = new ArrayList<>();

        CompilerConfiguration compilerConfiguration = new CompilerConfiguration();

        compilerConfiguration.setCompilerVersion( "1.8" );

        populateArguments( compilerConfiguration, expectedArguments, false, false, true );

        internalTest( compilerConfiguration, expectedArguments );
    }

    @Test
    public void testBuildCompilerArgsUnspecifiedVersion()
    {
        List<String> expectedArguments = new ArrayList<>();

        CompilerConfiguration compilerConfiguration = new CompilerConfiguration();

        populateArguments( compilerConfiguration, expectedArguments, false, false, false );

        internalTest( compilerConfiguration, expectedArguments );
    }

    @Test
    public void testBuildCompilerDebugLevel()
    {
        List<String> expectedArguments = new ArrayList<>();

        CompilerConfiguration compilerConfiguration = new CompilerConfiguration();

        compilerConfiguration.setDebug( true );

        compilerConfiguration.setDebugLevel( "none" );

        populateArguments( compilerConfiguration, expectedArguments, false, false, false );

        internalTest( compilerConfiguration, expectedArguments );
    }

    // PLXCOMP-190
    @Test
    public void testJRuntimeArguments()
    {
        List<String> expectedArguments = new ArrayList<>();

        CompilerConfiguration compilerConfiguration = new CompilerConfiguration();

        // outputLocation
        compilerConfiguration.setOutputLocation( "/output" );
        expectedArguments.add( "-d" );
        expectedArguments.add( new File( "/output" ).getAbsolutePath() );

        // targetVersion
        compilerConfiguration.setTargetVersion( "1.3" );
        expectedArguments.add( "-target" );
        expectedArguments.add( "1.3" );

        // sourceVersion
        compilerConfiguration.setSourceVersion( "1.3" );
        expectedArguments.add( "-source" );
        expectedArguments.add( "1.3" );

        // customCompilerArguments
        Map<String, String> customCompilerArguments = new LinkedHashMap<>();
        customCompilerArguments.put( "-J-Duser.language=en_us", null );
        compilerConfiguration.setCustomCompilerArgumentsAsMap( customCompilerArguments );
        // don't expect this argument!!

        internalTest( compilerConfiguration, expectedArguments );
    }

    @Test
    public void testModulePathAnnotations() throws Exception
    {
        List<String> expectedArguments = new ArrayList<>();

        CompilerConfiguration compilerConfiguration = new CompilerConfiguration();

        final String[] source = {"module-info.java"};

        // outputLocation
        compilerConfiguration.setOutputLocation( "/output" );
        expectedArguments.add( "-d" );
        expectedArguments.add( new File( "/output" ).getAbsolutePath() );

        // failOnWarning
        compilerConfiguration.setModulepathEntries( Arrays.asList( "/repo/a/b/1.0/b-1.0.jar",
                "/repo/c/d/1.0/d-1.0.jar" ) );
        expectedArguments.add( "--module-path" );
        expectedArguments.add( "/repo/a/b/1.0/b-1.0.jar" + File.pathSeparator +
                "/repo/c/d/1.0/d-1.0.jar" + File.pathSeparator );

        compilerConfiguration.setProcessorModulePathEntries(Arrays.asList("/repo/a/b/1.0/annotations-1.0.jar",
                "/repo/f/a/1.0/annotations-4.0.jar"));
        expectedArguments.add( "--processor-module-path" );
        expectedArguments.add("/repo/a/b/1.0/annotations-1.0.jar" + File.pathSeparator +
                "/repo/f/a/1.0/annotations-4.0.jar" + File.pathSeparator );

        // releaseVersion
        compilerConfiguration.setReleaseVersion( "9" );
        expectedArguments.add( "--release" );
        expectedArguments.add( "9" );

        internalTest( compilerConfiguration, expectedArguments, source);
    }

    @Test
    public void testModulePath() throws Exception
    {
        List<String> expectedArguments = new ArrayList<>();

        CompilerConfiguration compilerConfiguration = new CompilerConfiguration();

        // outputLocation
        compilerConfiguration.setOutputLocation( "/output" );
        expectedArguments.add( "-d" );
        expectedArguments.add( new File( "/output" ).getAbsolutePath() );

        // failOnWarning
        compilerConfiguration.setModulepathEntries( Arrays.asList( "/repo/a/b/1.0/b-1.0.jar",
                                                                   "/repo/c/d/1.0/d-1.0.jar" ) );
        expectedArguments.add( "--module-path" );
        expectedArguments.add( "/repo/a/b/1.0/b-1.0.jar" + File.pathSeparator + 
                               "/repo/c/d/1.0/d-1.0.jar" + File.pathSeparator );

        // default source + target
        expectedArguments.add( "-target" );
        expectedArguments.add( "1.1" );
        expectedArguments.add( "-source" );
        expectedArguments.add( "1.3" );

        internalTest( compilerConfiguration, expectedArguments );
    }

    @Test
    public void testModuleVersion()
    {
        List<String> expectedArguments = new ArrayList<>();

        CompilerConfiguration compilerConfiguration = new CompilerConfiguration();

        // outputLocation
        compilerConfiguration.setOutputLocation( "/output" );
        expectedArguments.add( "-d" );
        expectedArguments.add( new File( "/output" ).getAbsolutePath() );

        // default source + target
        expectedArguments.add( "-target" );
        expectedArguments.add( "1.1" );
        expectedArguments.add( "-source" );
        expectedArguments.add( "1.3" );

        // module version
        compilerConfiguration.setModuleVersion( "1.2.0-SNAPSHOT" );
        expectedArguments.add( "--module-version" );
        expectedArguments.add( "1.2.0-SNAPSHOT" );

        internalTest( compilerConfiguration, expectedArguments );
    }

    @Test
    public void testReleaseVersion()
    {
        List<String> expectedArguments = new ArrayList<>();

        CompilerConfiguration compilerConfiguration = new CompilerConfiguration();

        // outputLocation
        compilerConfiguration.setOutputLocation( "/output" );
        expectedArguments.add( "-d" );
        expectedArguments.add( new File( "/output" ).getAbsolutePath() );

        // releaseVersion
        compilerConfiguration.setReleaseVersion( "6" );
        expectedArguments.add( "--release" );
        expectedArguments.add( "6" );
        
        internalTest( compilerConfiguration, expectedArguments );
    }

    @Test
    public void testFailOnWarning()
    {
        List<String> expectedArguments = new ArrayList<>();

        CompilerConfiguration compilerConfiguration = new CompilerConfiguration();

        // outputLocation
        compilerConfiguration.setOutputLocation( "/output" );
        expectedArguments.add( "-d" );
        expectedArguments.add( new File( "/output" ).getAbsolutePath() );

        // failOnWarning
        compilerConfiguration.setFailOnWarning( true );
        expectedArguments.add( "-Werror" );
        
        // default source + target
        expectedArguments.add( "-target" );
        expectedArguments.add( "1.1" );
        expectedArguments.add( "-source" );
        expectedArguments.add( "1.3" );
        
        internalTest( compilerConfiguration, expectedArguments );
    }

    @Test
    public void testMultipleAddExports()
    {
        List<String> expectedArguments = new ArrayList<>();

        CompilerConfiguration compilerConfiguration = new CompilerConfiguration();

        // outputLocation
        compilerConfiguration.setOutputLocation( "/output" );
        expectedArguments.add( "-d" );
        expectedArguments.add( new File( "/output" ).getAbsolutePath() );

        // default source + target
        expectedArguments.add( "-target" );
        expectedArguments.add( "1.1" );
        expectedArguments.add( "-source" );
        expectedArguments.add( "1.3" );

        // add multiple --add-exports
        compilerConfiguration.addCompilerCustomArgument( "--add-exports", "FROM-MOD/package1=OTHER-MOD" );
        expectedArguments.add( "--add-exports" );
        expectedArguments.add( "FROM-MOD/package1=OTHER-MOD" );
        compilerConfiguration.addCompilerCustomArgument( "--add-exports", "FROM-MOD/package2=OTHER-MOD" );
        expectedArguments.add( "--add-exports" );
        expectedArguments.add( "FROM-MOD/package2=OTHER-MOD" );
        
        internalTest( compilerConfiguration, expectedArguments );
    }

    /* This test fails on Java 1.4. The multiple parameters of the same source file cause an error, as it is interpreted as a DuplicateClass
     * Setting the size of the array to 3 is fine, but does not exactly test what it is supposed to - disabling the test for now
    public void testCommandLineTooLongWhenForking()
        throws Exception
    {
        JavacCompiler compiler = (JavacCompiler) lookup( org.codehaus.plexus.compiler.Compiler.ROLE, getRoleHint() );

        File destDir = new File( "target/test-classes-cmd" );
        destDir.mkdirs();

        // fill the cmd line arguments, 300 is enough to make it break
        String[] args = new String[400];
        args[0] = "-d";
        args[1] = destDir.getAbsolutePath();
        for ( int i = 2; i < args.length; i++ )
        {
            args[i] = "org/codehaus/foo/Person.java";
        }

        CompilerConfiguration config = new CompilerConfiguration();
        config.setWorkingDirectory( new File( getBasedir() + "/src/test-input/src/main" ) );
        config.setFork( true );

        List messages = compiler.compileOutOfProcess( config, "javac", args );

        assertEquals( "There were errors launching the external compiler: " + messages, 0, messages.size() );
    }
    */

    private void populateArguments( CompilerConfiguration compilerConfiguration, List<String> expectedArguments,
                                    boolean suppressSourceVersion, boolean suppressEncoding, boolean parameters )
    {
        // outputLocation

        compilerConfiguration.setOutputLocation( "/output" );

        expectedArguments.add( "-d" );

        expectedArguments.add( new File( "/output" ).getAbsolutePath() );

        // classpathEntires

        List<String> classpathEntries = new ArrayList<>();

        classpathEntries.add( "/myjar1.jar" );

        classpathEntries.add( "/myjar2.jar" );

        compilerConfiguration.setClasspathEntries( classpathEntries );

        expectedArguments.add( "-classpath" );

        expectedArguments.add( "/myjar1.jar" + PS + "/myjar2.jar" + PS );

        // sourceRoots

        List<String> compileSourceRoots = new ArrayList<>();

        compileSourceRoots.add( "/src/main/one" );

        compileSourceRoots.add( "/src/main/two" );

        compilerConfiguration.setSourceLocations( compileSourceRoots );

        expectedArguments.add( "-sourcepath" );

        expectedArguments.add( "/src/main/one" + PS + "/src/main/two" + PS );

        // debug

        compilerConfiguration.setDebug( true );

        if ( StringUtils.isNotEmpty( compilerConfiguration.getDebugLevel() ) )
        {
            expectedArguments.add( "-g:" + compilerConfiguration.getDebugLevel() );
        }
        else
        {
            expectedArguments.add( "-g" );
        }

        // parameters

        compilerConfiguration.setParameters( true );

        if (parameters)
        {
            expectedArguments.add( "-parameters" );
        }
        
        // showDeprecation

        compilerConfiguration.setShowDeprecation( true );

        expectedArguments.add( "-deprecation" );

        // targetVersion

        compilerConfiguration.setTargetVersion( "1.3" );

        expectedArguments.add( "-target" );

        expectedArguments.add( "1.3" );

        // sourceVersion

        compilerConfiguration.setSourceVersion( "1.3" );

        if ( !suppressSourceVersion )
        {
            expectedArguments.add( "-source" );

            expectedArguments.add( "1.3" );
        }

        // sourceEncoding

        compilerConfiguration.setSourceEncoding( "iso-8859-1" );

        if ( !suppressEncoding )
        {
            expectedArguments.add( "-encoding" );

            expectedArguments.add( "iso-8859-1" );
        }

        // customerCompilerArguments

        Map<String, String> customerCompilerArguments = new LinkedHashMap<>();

        customerCompilerArguments.put( "arg1", null );

        customerCompilerArguments.put( "foo", "bar" );

        compilerConfiguration.setCustomCompilerArgumentsAsMap( customerCompilerArguments );

        expectedArguments.add( "arg1" );

        expectedArguments.add( "foo" );

        expectedArguments.add( "bar" );
    }
}
