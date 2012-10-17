package org.codehaus.plexus.compiler.javac;
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.codehaus.plexus.compiler.CompilerConfiguration;
import org.codehaus.plexus.compiler.CompilerError;
import org.codehaus.plexus.compiler.CompilerException;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Olivier Lamy
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @since 2.0
 */
public class JavaxToolsCompiler
{
    static List<CompilerError> compileInProcess( String[] args, final CompilerConfiguration config,
                                                 String[] sourceFiles )
        throws CompilerException
    {
        try
        {
            final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if ( compiler == null )
            {
                return Collections.singletonList( new CompilerError(
                    "No compiler is provided in this environment.  Perhaps you are running on a JRE rather than a JDK?" ) );
            }
            final String sourceEncoding = config.getSourceEncoding();
            final Charset sourceCharset = sourceEncoding == null ? null : Charset.forName( sourceEncoding );
            final DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<JavaFileObject>();
            final StandardJavaFileManager standardFileManager =
                compiler.getStandardFileManager( collector, null, sourceCharset );

            final Iterable<? extends JavaFileObject> fileObjects =
                standardFileManager.getJavaFileObjectsFromStrings( Arrays.asList( sourceFiles ) );
            final JavaCompiler.CompilationTask task =

                                         /*(Writer out,
                                         JavaFileManager fileManager,
                                         DiagnosticListener<? super JavaFileObject> diagnosticListener,
                                         Iterable<String> options,
                                         Iterable<String> classes,
                                         Iterable<? extends JavaFileObject> compilationUnits)*/


                compiler.getTask( null, standardFileManager, collector, Arrays.asList( args ), null, fileObjects );
            final Boolean result = task.call();
            final ArrayList<CompilerError> compilerErrors = new ArrayList<CompilerError>();
            for ( Diagnostic<? extends JavaFileObject> diagnostic : collector.getDiagnostics() )
            {
                CompilerError.Kind kind;
                switch ( diagnostic.getKind() )
                {
                    case ERROR:
                        kind = CompilerError.Kind.ERROR;
                        break;
                    case WARNING:
                        kind = CompilerError.Kind.WARNING;
                        break;
                    case MANDATORY_WARNING:
                        kind = CompilerError.Kind.MANDATORY_WARNING;
                        break;
                    case NOTE:
                        kind = CompilerError.Kind.NOTE;
                        break;
                    default:
                        kind = CompilerError.Kind.OTHER;
                        break;
                }
                String baseMessage = diagnostic.getMessage( null );
                if ( baseMessage == null )
                {
                    continue;
                }
                JavaFileObject source = diagnostic.getSource();
                String longFileName = source == null ? null : source.toUri().getPath();
                String shortFileName = source == null ? null : source.getName();
                String formattedMessage = baseMessage;
                int lineNumber = Math.max( 0, (int) diagnostic.getLineNumber() );
                int columnNumber = Math.max( 0, (int) diagnostic.getColumnNumber() );
                if ( source != null && lineNumber > 0 )
                {
                    // Some compilers like to copy the file name into the message, which makes it appear twice.
                    String possibleTrimming = longFileName + ":" + lineNumber + ": ";
                    if ( formattedMessage.startsWith( possibleTrimming ) )
                    {
                        formattedMessage = formattedMessage.substring( possibleTrimming.length() );
                    }
                    else
                    {
                        possibleTrimming = shortFileName + ":" + lineNumber + ": ";
                        if ( formattedMessage.startsWith( possibleTrimming ) )
                        {
                            formattedMessage = formattedMessage.substring( possibleTrimming.length() );
                        }
                    }
                }
                compilerErrors.add(
                    new CompilerError( longFileName, kind, lineNumber, columnNumber, lineNumber, columnNumber,
                                       formattedMessage ) );
            }
            if ( result != Boolean.TRUE && compilerErrors.isEmpty() )
            {
                compilerErrors.add(
                    new CompilerError( "An unknown compilation problem occurred", CompilerError.Kind.ERROR ) );
            }
            return compilerErrors;
        }
        catch ( Exception e )
        {
            throw new CompilerException( e.getMessage(), e );
        }
    }
}
