package org.codehaus.plexus.compiler.eclipse;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;

class EclipseJavaCompilerTest {
    @ParameterizedTest
    @MethodSource("sources")
    void testReorderedSources(List<String> expected, List<String> inputSources) {
        List<String> resorted = EclipseJavaCompiler.resortSourcesToPutModuleInfoFirst(inputSources);

        assertEquals(expected, resorted);
    }

    static Stream<Arguments> sources() {
        List<String> expectedOrder = asList("module-info.java", "plexus/A.java", "plexus/B.java", "eclipse/A.java");

        List<String> moduleInfoAlreadyFirst =
                asList("module-info.java", "plexus/A.java", "plexus/B.java", "eclipse/A.java");

        List<String> moduleInfoSomewhereInTheMiddle =
                asList("plexus/A.java", "module-info.java", "plexus/B.java", "eclipse/A.java");

        List<String> moduleInfoAsLast = asList("plexus/A.java", "plexus/B.java", "eclipse/A.java", "module-info.java");

        return Stream.of(
                Arguments.arguments(expectedOrder, moduleInfoAlreadyFirst),
                Arguments.arguments(expectedOrder, moduleInfoSomewhereInTheMiddle),
                Arguments.arguments(expectedOrder, moduleInfoAsLast));
    }
}
