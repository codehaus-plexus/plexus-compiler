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

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 */
@Named
public class DefaultCompilerManager implements CompilerManager {
    @Inject
    private Map<String, Provider<Compiler>> compilers;

    // ----------------------------------------------------------------------
    // CompilerManager Implementation
    // ----------------------------------------------------------------------

    public Compiler getCompiler(String compilerId) throws NoSuchCompilerException {
        // Provider<Class> is lazy
        // presence of provider means component is present (but not yet constructed)
        Provider<Compiler> compiler = compilers.get(compilerId);

        // it exists: as provider is found
        if (compiler == null) {
            throw new NoSuchCompilerException(compilerId);
        }
        // provider is lazy: if we are here, it exists but not yet created
        try {
            return compiler.get();
        } catch (Exception e) {
            // if we are here: DI could not construct it: so report proper error
            throw new RuntimeException(e);
        }
    }
}
