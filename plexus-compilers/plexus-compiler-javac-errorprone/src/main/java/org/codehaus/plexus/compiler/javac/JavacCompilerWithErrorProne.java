/*
 * Copyright 2011 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.codehaus.plexus.compiler.javac;

import com.google.errorprone.ErrorProneCompiler;
import org.codehaus.plexus.compiler.CompilerConfiguration;
import org.codehaus.plexus.compiler.CompilerException;
import org.codehaus.plexus.compiler.CompilerMessage;
import org.codehaus.plexus.compiler.CompilerResult;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * This class overrides JavacCompiler with modifications to use the error-prone
 * entry point into Javac.
 *
 * @author <a href="mailto:alexeagle@google.com">Alex Eagle</a>
 * @plexus.component role="org.codehaus.plexus.compiler.Compiler"
 * role-hint="javac-with-errorprone"
 */
public class JavacCompilerWithErrorProne extends JavacCompiler {

  @Override
  CompilerResult compileOutOfProcess(CompilerConfiguration config, String executable, String[] args)
      throws CompilerException {
    throw new UnsupportedOperationException("Cannot compile out-of-process with error-prone");
  }

  @Override
  CompilerResult compileInProcessWithProperClassloader(Class<?> javacClass, String[] args)
      throws CompilerException {
    // TODO(alexeagle): perhaps error-prone can conform to the 1.6 JavaCompiler API.
    // Then we could use the JavaxToolsCompiler approach instead, which would reuse more code.

    final List<CompilerMessage> messages = new ArrayList<CompilerMessage>();
    DiagnosticListener<? super JavaFileObject> listener = new DiagnosticListener<JavaFileObject>() {
      public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
        messages.add(new CompilerMessage(
            diagnostic.getSource().getName(),
            JavaxToolsCompiler.convertKind(diagnostic),
            (int)diagnostic.getLineNumber(),
            (int)diagnostic.getColumnNumber(),
            -1, -1, // end pos line:column is hard to calculate
            diagnostic.getMessage(Locale.getDefault())));
      }
    };
    int result = new ErrorProneCompiler.Builder()
        .listenToDiagnostics(listener)
        .build()
        .compile(args);

    return new CompilerResult(result == 0, messages);
  }
}
