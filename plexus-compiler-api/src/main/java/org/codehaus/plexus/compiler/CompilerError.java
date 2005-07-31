package org.codehaus.plexus.compiler;

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

/**
 *
 * Copyright 2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/**
 * This class encapsulates an error message produced by a programming language
 * processor (whether interpreted or compiled)
 * @author <a href="mailto:stefano@apache.org">Stefano Mazzocchi</a>
 * @version $Id$
 * @since 2.0
 */
public class CompilerError
{
    /**
     * Is this a severe error or a warning?
     */
    private boolean error;

    /**
     * The start line number of the offending program text
     */
    private int startline;

    /**
     * The start column number of the offending program text
     */
    private int startcolumn;

    /**
     * The end line number of the offending program text
     */
    private int endline;

    /**
     * The end column number of the offending program text
     */
    private int endcolumn;

    /**
     * The name of the file containing the offending program text
     */
    private String file;

    /**
     * The actual error text produced by the language processor
     */
    private String message;

    /**
     * The error message constructor.
     *
     * @param file The name of the file containing the offending program text
     * @param error Is this a severe error or a warning?
     * @param startline The start line number of the offending program text
     * @param startcolumn The start column number of the offending program text
     * @param endline The end line number of the offending program text
     * @param endcolumn The end column number of the offending program text
     * @param message The actual error text produced by the language processor
     */
    public CompilerError( String file,
                          boolean error,
                          int startline,
                          int startcolumn,
                          int endline,
                          int endcolumn,
                          String message )
    {
        this.file = file;
        this.error = error;
        this.startline = startline;
        this.startcolumn = startcolumn;
        this.endline = endline;
        this.endcolumn = endcolumn;
        this.message = message;
    }

    /**
     * The warning message constructor.
     *
     * @param message The actual error text produced by the language processor
     */
    public CompilerError( String message )
    {
        this.message = message;
    }

    /**
     * The error message constructor.
     *
     * @param message The actual error text produced by the language processor
     * @param error whether it was an error or informational
     */
    public CompilerError( String message, boolean error )
    {
        this.message = message;
        this.error = error;
    }

    /**
     * Return the filename associated with this compiler error.
     *
     * @return The filename associated with this compiler error
     */
    public String getFile()
    {
        return file;
    }

    /**
     * Assert whether this is a severe error or a warning
     *
     * @return Whether the error is severe
     */
    public boolean isError()
    {
        return error;
    }

    /**
     * Return the starting line number of the program text originating this error
     *
     * @return The starting line number of the program text originating this error
     */
    public int getStartLine()
    {
        return startline;
    }

    /**
     * Return the starting column number of the program text originating this
     * error
     *
     * @return The starting column number of the program text originating this
     * error
     */
    public int getStartColumn()
    {
        return startcolumn;
    }

    /**
     * Return the ending line number of the program text originating this error
     *
     * @return The ending line number of the program text originating this error
     */
    public int getEndLine()
    {
        return endline;
    }

    /**
     * Return the ending column number of the program text originating this
     * error
     *
     * @return The ending column number of the program text originating this
     * error
     */
    public int getEndColumn()
    {
        return endcolumn;
    }

    /**
     * Return the message produced by the language processor
     *
     * @return The message produced by the language processor
     */
    public String getMessage()
    {
        return message;
    }

    public String toString()
    {
        if ( file != null )
        {
            return file + ":" + "[" + startline + "," + startcolumn + "] " + message;
        }
        else
        {
            return message;
        }
    }
}
