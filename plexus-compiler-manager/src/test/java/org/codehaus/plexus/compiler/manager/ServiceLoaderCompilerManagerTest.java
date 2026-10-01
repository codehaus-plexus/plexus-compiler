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
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.codehaus.plexus.compiler.AbstractCompiler;
import org.codehaus.plexus.compiler.Compiler;
import org.codehaus.plexus.compiler.CompilerConfiguration;
import org.codehaus.plexus.compiler.CompilerException;
import org.codehaus.plexus.compiler.CompilerOutputStyle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Builds a {@link DefaultCompilerManager} with no Sisu or Plexus container.
 */
class ServiceLoaderCompilerManagerTest {

    @javax.inject.Named("per-call")
    public static class PerCallCompiler extends AbstractCompiler {
        public PerCallCompiler() {
            super(CompilerOutputStyle.ONE_OUTPUT_FILE_PER_INPUT_FILE, ".x", ".y", null);
        }

        @Override
        public String getCompilerId() {
            return "per-call";
        }

        @Override
        public String[] createCommandLine(CompilerConfiguration config) throws CompilerException {
            return new String[0];
        }
    }

    @javax.inject.Named("shared")
    @javax.inject.Singleton
    public static class SharedCompiler extends PerCallCompiler {
        @Override
        public String getCompilerId() {
            return "shared";
        }
    }

    private static URLClassLoader loaderWith(Path dir, String... lines) throws Exception {
        Path services = dir.resolve("META-INF/services/" + Compiler.class.getName());
        Files.createDirectories(services.getParent());
        Files.write(services, String.join("\n", lines).getBytes(StandardCharsets.UTF_8));
        return new URLClassLoader(
                new URL[] {dir.toUri().toURL()}, ServiceLoaderCompilerManagerTest.class.getClassLoader());
    }

    @Test
    void compilersAreFoundById(@TempDir Path dir) throws Exception {
        try (URLClassLoader loader = loaderWith(dir, PerCallCompiler.class.getName(), SharedCompiler.class.getName())) {
            CompilerManager manager = DefaultCompilerManager.fromServiceLoader(loader);

            assertInstanceOf(PerCallCompiler.class, manager.getCompiler("per-call"));
            assertInstanceOf(SharedCompiler.class, manager.getCompiler("shared"));
            assertThrows(NoSuchCompilerException.class, () -> manager.getCompiler("foo"));
        }
    }

    @Test
    void instanceScopeFollowsSisu(@TempDir Path dir) throws Exception {
        try (URLClassLoader loader = loaderWith(dir, PerCallCompiler.class.getName(), SharedCompiler.class.getName())) {
            CompilerManager manager = DefaultCompilerManager.fromServiceLoader(loader);

            assertNotSame(manager.getCompiler("per-call"), manager.getCompiler("per-call"));
            assertSame(manager.getCompiler("shared"), manager.getCompiler("shared"));
        }
    }

    @Test
    void providerThatCannotBeLoadedIsSkipped(@TempDir Path dir) throws Exception {
        try (URLClassLoader loader = loaderWith(
                dir,
                "org.example.MissingCompiler",
                PerCallCompiler.class.getName(),
                "org.example.OtherMissingCompiler",
                SharedCompiler.class.getName())) {
            CompilerManager manager = DefaultCompilerManager.fromServiceLoader(loader);

            assertInstanceOf(PerCallCompiler.class, manager.getCompiler("per-call"));
            assertInstanceOf(SharedCompiler.class, manager.getCompiler("shared"));
            assertThrows(NoSuchCompilerException.class, () -> manager.getCompiler("missing"));
        }
    }
}
