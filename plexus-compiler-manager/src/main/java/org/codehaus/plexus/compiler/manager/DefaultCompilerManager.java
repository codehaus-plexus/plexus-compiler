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
import javax.inject.Singleton;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import org.codehaus.plexus.compiler.AbstractCompiler;
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

    private static final int MAX_LOOKUP_FAILURES = 100;

    @Inject
    private Map<String, Provider<Compiler>> compilers;

    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Creates a manager populated through {@link ServiceLoader} instead of a dependency injection container.
     * Intended for callers that cannot use Sisu/Plexus, for example Maven 4 API plugins that bind the result
     * with a plugin-local {@code @Provides} method.
     * <p>
     * Every {@link Compiler} registered in {@code META-INF/services/org.codehaus.plexus.compiler.Compiler} of the
     * given class loader is registered under its {@code @Named} value, the key Sisu uses (equal to
     * {@link AbstractCompiler#getCompilerId()}), or under the compiler id if it has no {@code @Named}. Instance
     * scope follows the Sisu-based manager: a compiler class annotated {@code @Singleton} (javac, eclipse) is
     * shared, any other one gets a new instance on every {@link #getCompiler(String)} call, so it needs a public
     * no-argument constructor.
     * If two providers share an id, the first one found wins.
     * <p>
     * A provider that cannot be loaded or instantiated, for example because an optional dependency such as
     * ecj, AspectJ or Error Prone is missing from the class loader, is skipped with a warning and the other
     * providers are still registered. Asking for the skipped id throws {@link NoSuchCompilerException}.
     *
     * @param classLoader the class loader to look providers up in
     * @return a new manager holding every compiler that could be loaded
     * @since 2.18.0
     */
    public static DefaultCompilerManager fromServiceLoader(ClassLoader classLoader) {
        Objects.requireNonNull(classLoader, "classLoader");
        DefaultCompilerManager manager = new DefaultCompilerManager();
        Map<String, Provider<Compiler>> providers = new HashMap<>();
        Iterator<Compiler> iterator =
                ServiceLoader.load(Compiler.class, classLoader).iterator();
        int failures = 0;
        while (failures < MAX_LOOKUP_FAILURES) {
            Compiler compiler;
            try {
                if (!iterator.hasNext()) {
                    break;
                }
                compiler = iterator.next();
            } catch (ServiceConfigurationError e) {
                // the iterator has already moved past the offending entry, so carry on with the next one;
                // the cap only guards against an iterator that would fail forever
                failures++;
                manager.log.warn("Skipping a compiler that cannot be loaded: {}", e.getMessage(), e);
                continue;
            }
            String id = idOf(compiler);
            if (id == null) {
                manager.log.warn("Skipping compiler {}: it has neither @Named nor a compiler id", compiler.getClass());
                continue;
            }
            Class<? extends Compiler> type = compiler.getClass();
            providers.putIfAbsent(
                    id,
                    type.isAnnotationPresent(Singleton.class)
                            ? new SingletonProvider(compiler)
                            : new NewInstanceProvider(type));
        }
        manager.compilers = providers;
        return manager;
    }

    /**
     * The key Sisu registers the compiler under: its {@code @Named} value, which equals
     * {@link AbstractCompiler#getCompilerId()} for every compiler in this project.
     */
    private static String idOf(Compiler compiler) {
        Named named = compiler.getClass().getAnnotation(Named.class);
        if (named != null && !named.value().isEmpty()) {
            return named.value();
        }
        return compiler instanceof AbstractCompiler ? ((AbstractCompiler) compiler).getCompilerId() : null;
    }

    /** Hands out the one instance, like a Sisu binding of a {@code @Singleton} class. */
    private static final class SingletonProvider implements Provider<Compiler> {
        private final Compiler instance;

        SingletonProvider(Compiler instance) {
            this.instance = instance;
        }

        @Override
        public Compiler get() {
            return instance;
        }
    }

    /** Creates a fresh compiler per call, like an unscoped Sisu binding. */
    private static final class NewInstanceProvider implements Provider<Compiler> {
        private final Class<? extends Compiler> type;

        NewInstanceProvider(Class<? extends Compiler> type) {
            this.type = type;
        }

        @Override
        public Compiler get() {
            try {
                return type.getConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Cannot instantiate " + type.getName(), e);
            }
        }
    }

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
