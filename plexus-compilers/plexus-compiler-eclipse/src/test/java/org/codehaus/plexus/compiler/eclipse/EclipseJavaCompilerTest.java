package org.codehaus.plexus.compiler.eclipse;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;

class EclipseJavaCompilerTest
{
    @ParameterizedTest
    @MethodSource( "sources" )
    void testReorderedSources( Set<String> expected, Set<String> inputSources )
    {
        Set<String> resorted = EclipseJavaCompiler.resortSourcesToPutModuleInfoFirst( inputSources );

        assertEquals( expected, resorted );
    }

    static Stream<Arguments> sources()
    {
        Set<String> expectedOrder = new LinkedHashSet<>( asList(
                "module-info.java",
                "plexus/A.java",
                "plexus/B.java",
                "eclipse/A.java"
        ) );

        Set<String> moduleInfoAlreadyFirst = new LinkedHashSet<>( asList(
                "module-info.java",
                "plexus/A.java",
                "plexus/B.java",
                "eclipse/A.java"
        ) );

        Set<String> moduleInfoSomewhereInTheMiddle = new LinkedHashSet<>( asList(
                "plexus/A.java",
                "module-info.java",
                "plexus/B.java",
                "eclipse/A.java"
        ) );

        Set<String> moduleInfoAsLast = new LinkedHashSet<>( asList(
                "plexus/A.java",
                "plexus/B.java",
                "eclipse/A.java",
                "module-info.java"
        ) );

        return Stream.of(
                Arguments.arguments( expectedOrder, moduleInfoAlreadyFirst ),
                Arguments.arguments( expectedOrder, moduleInfoSomewhereInTheMiddle ),
                Arguments.arguments( expectedOrder, moduleInfoAsLast )
        );
    }
}
