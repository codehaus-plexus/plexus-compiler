package org.codehaus.plexus.compiler.manager;

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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Set;
import java.util.TreeSet;

import org.codehaus.plexus.compiler.AbstractCompiler;
import org.codehaus.plexus.compiler.Compiler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Runs with every compiler implementation module on the classpath.
 */
class ServiceLoaderCompilersTest {
    private static final String SERVICES = "META-INF/services/" + Compiler.class.getName();

    private static final String SISU_INDEX = "META-INF/sisu/javax.inject.Named";

    @Test
    void everyCompilerIsFoundById() throws Exception {
        CompilerManager manager =
                DefaultCompilerManager.fromServiceLoader(getClass().getClassLoader());

        for (String id : Arrays.asList("javac", "eclipse", "aspectj", "csharp", "javac-with-errorprone")) {
            Compiler compiler = manager.getCompiler(id);
            assertNotNull(compiler, id);
            assertEquals(id, ((AbstractCompiler) compiler).getCompilerId());
        }
        // the in-process compiler default must work without injection
        assertInstanceOf(org.codehaus.plexus.compiler.javac.JavacCompiler.class, manager.getCompiler("javac"));
    }

    /**
     * The Sisu index is generated from {@code @Named}, the services files are written by hand: per module,
     * both must name the same {@link Compiler} classes.
     */
    @Test
    void servicesFilesMatchTheSisuIndexPerModule() throws Exception {
        Enumeration<URL> indexes = getClass().getClassLoader().getResources(SISU_INDEX);
        int modules = 0;
        while (indexes.hasMoreElements()) {
            URL index = indexes.nextElement();
            String root = index.toString().substring(0, index.toString().length() - SISU_INDEX.length());
            Set<String> named = new TreeSet<>();
            for (String name : lines(index)) {
                Class<?> type = Class.forName(name, false, getClass().getClassLoader());
                if (Compiler.class.isAssignableFrom(type)) {
                    named.add(name);
                }
            }
            if (named.isEmpty()) {
                continue; // not a compiler module (e.g. the manager itself)
            }
            modules++;
            URL services = URI.create(root + SERVICES).toURL();
            assertEquals(named, lines(services), "services file in " + root);
        }
        assertEquals(5, modules, "compiler modules found with a Sisu index");
    }

    private static Set<String> lines(URL url) throws IOException {
        Set<String> result = new TreeSet<>();
        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                int hash = line.indexOf('#');
                String name = (hash >= 0 ? line.substring(0, hash) : line).trim();
                if (!name.isEmpty()) {
                    result.add(name);
                }
            }
        }
        return result;
    }
}
