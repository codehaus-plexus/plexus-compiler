/*

 ============================================================================
                   The Apache Software License, Version 1.1
 ============================================================================

 Copyright (C) 1999-2003 The Apache Software Foundation. All rights reserved.

 Redistribution and use in source and binary forms, with or without modifica-
 tion, are permitted provided that the following conditions are met:

 1. Redistributions of  source code must  retain the above copyright  notice,
    this list of conditions and the following disclaimer.

 2. Redistributions in binary form must reproduce the above copyright notice,
    this list of conditions and the following disclaimer in the documentation
    and/or other materials provided with the distribution.

 3. The end-user documentation included with the redistribution, if any, must
    include  the following  acknowledgment:  "This product includes  software
    developed  by the  Apache Software Foundation  (http://www.apache.org/)."
    Alternately, this  acknowledgment may  appear in the software itself,  if
    and wherever such third-party acknowledgments normally appear.

 4. The names "Apache Cocoon" and  "Apache Software Foundation" must  not  be
    used to  endorse or promote  products derived from  this software without
    prior written permission. For written permission, please contact
    apache@apache.org.

 5. Products  derived from this software may not  be called "Apache", nor may
    "Apache" appear  in their name,  without prior written permission  of the
    Apache Software Foundation.

 THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 FITNESS  FOR A PARTICULAR  PURPOSE ARE  DISCLAIMED.  IN NO  EVENT SHALL  THE
 APACHE SOFTWARE  FOUNDATION  OR ITS CONTRIBUTORS  BE LIABLE FOR  ANY DIRECT,
 INDIRECT, INCIDENTAL, SPECIAL,  EXEMPLARY, OR CONSEQUENTIAL  DAMAGES (INCLU-
 DING, BUT NOT LIMITED TO, PROCUREMENT  OF SUBSTITUTE GOODS OR SERVICES; LOSS
 OF USE, DATA, OR  PROFITS; OR BUSINESS  INTERRUPTION)  HOWEVER CAUSED AND ON
 ANY  THEORY OF LIABILITY,  WHETHER  IN CONTRACT,  STRICT LIABILITY,  OR TORT
 (INCLUDING  NEGLIGENCE OR  OTHERWISE) ARISING IN  ANY WAY OUT OF THE  USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 This software  consists of voluntary contributions made  by many individuals
 on  behalf of the Apache Software  Foundation and was  originally created by
 Stefano Mazzocchi  <stefano@apache.org>. For more  information on the Apache
 Software Foundation, please see <http://www.apache.org/>.

*/
package org.codehaus.plexus.compiler;

/**
 * This class encapsulates an error message produced by a programming language
 * processor (whether interpreted or compiled)
 * @author <a href="mailto:stefano@apache.org">Stefano Mazzocchi</a>
 * @version CVS $Id$
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
     * @param error The actual error text produced by the language processor
     * @param startline The start line number of the offending program text
     * @param startcolumn The start column number of the offending program text
     * @param endline The end line number of the offending program text
     * @param endcolumn The end column number of the offending program text
     * @param message The actual error text produced by the language processor
     */
    public CompilerError(
        String file,
        boolean error,
        int startline,
        int startcolumn,
        int endline,
        int endcolumn,
        String message
        )
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
     * The error message constructor.
     *
     * @param message The actual error text produced by the language processor
     */
    public CompilerError( String message )
    {
        this.message = message;
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
        return file + ":" + "[" + startline + "," + startcolumn + "] " + message;
    }
}
