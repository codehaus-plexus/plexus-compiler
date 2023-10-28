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
import java.net.URL;
import java.net.URLClassLoader;

public class IsolatedClassLoader extends URLClassLoader {
    private ClassLoader parentClassLoader = ClassLoader.getSystemClassLoader();

    public IsolatedClassLoader() {
        super(new URL[0], null);
    }

    public void addURL(URL url) {
        super.addURL(url);
    }

    public synchronized Class<?> loadClass(String className) throws ClassNotFoundException {
        Class<?> c = findLoadedClass(className);

        ClassNotFoundException ex = null;

        if (c == null) {
            try {
                c = findClass(className);
            } catch (ClassNotFoundException e) {
                ex = e;

                if (parentClassLoader != null) {
                    c = parentClassLoader.loadClass(className);
                }
            }
        }

        if (c == null) {
            throw ex;
        }

        return c;
    }
}
