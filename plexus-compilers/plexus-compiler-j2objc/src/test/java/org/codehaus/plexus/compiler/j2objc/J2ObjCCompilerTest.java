package org.codehaus.plexus.compiler.j2objc;

/**
 * The MIT License
 * <p>
 * Copyright (c) 2005, The Codehaus
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import org.hamcrest.io.FileMatchers;
import org.codehaus.plexus.compiler.CompilerConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * j2objc must be in the PATH
 * @author lmaitre
 */
public class J2ObjCCompilerTest
{

    @Test
    public void testJ2ObjCCompiler()
        throws Exception
    {
        J2ObjCCompiler comp = new J2ObjCCompiler();
        Map<String, String> customCompilerArguments = new HashMap<>();
        customCompilerArguments.put( "-use-arc", null );
        customCompilerArguments.put( "-sourcepath", "src/test/resources" );
        CompilerConfiguration cc = new CompilerConfiguration();
        cc.setOutputLocation( "target/generated/objective-c" );
        cc.setSourceLocations( Arrays.asList( new String[]{ "src/test/resources" } ) );
        cc.setWorkingDirectory( new File( "." ) );
        cc.setFork( true );
        cc.setVerbose( true );
        cc.setCustomCompilerArgumentsAsMap( customCompilerArguments );

        comp.performCompile( cc );
        File f = new File( "target/generated/objective-c/de/test/App.h" );
        assertThat("file not exists:" + f, f, FileMatchers.anExistingFile());
        f = new File( "target/generated/objective-c/de/test/App.m" );
        assertThat("file not exists:" + f, f, FileMatchers.anExistingFile());
    }

}
