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
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import java.util.Map;

import org.codehaus.plexus.compiler.Compiler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 */
@Named
public class DefaultCompilerManager implements CompilerManager {
    private static final String ERROR_MESSAGE = "Compiler '{}' could not be instantiated or injected properly. "
            + "If you spelled the compiler ID correctly and all necessary dependencies are on the classpath, "
            + "then next you can try running the build with -Dsisu.debug, looking for exceptions.";
    private static final String ERROR_MESSAGE_DETAIL = "TypeNotPresentException caused by UnsupportedClassVersionError "
            + "might indicate, that the compiler needs a more recent Java runtime. "
            + "IllegalArgumentException in ClassReader.<init> might mean, that you need to upgrade Maven.";

    @Inject
    private Map<String, Provider<Compiler>> compilers;

    private final Logger log = LoggerFactory.getLogger(getClass());

    // ----------------------------------------------------------------------
    // CompilerManager Implementation
    // ----------------------------------------------------------------------

    public Compiler getCompiler(String compilerId) throws NoSuchCompilerException {
        // Provider<Class> is lazy -> presence of provider means compiler is present, but not yet constructed
        Provider<Compiler> compilerProvider = compilers.get(compilerId);

        if (compilerProvider == null) {
            // Compiler could not be injected for some reason
            log.error(ERROR_MESSAGE + " " + ERROR_MESSAGE_DETAIL, compilerId);
            throw new NoSuchCompilerException(compilerId);
        }

        // Provider exists, but compiler was not created yet
        try {
            return compilerProvider.get();
        } catch (Exception e) {
            // DI could not construct compiler
            log.error(ERROR_MESSAGE, compilerId);
            throw new NoSuchCompilerException(compilerId, e);
        }
    }
}
