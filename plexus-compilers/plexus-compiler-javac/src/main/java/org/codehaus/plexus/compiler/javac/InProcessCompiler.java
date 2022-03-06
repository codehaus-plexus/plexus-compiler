package org.codehaus.plexus.compiler.javac;

import org.codehaus.plexus.compiler.CompilerConfiguration;
import org.codehaus.plexus.compiler.CompilerException;
import org.codehaus.plexus.compiler.CompilerResult;
import org.codehaus.plexus.logging.LogEnabled;

public interface InProcessCompiler extends LogEnabled {

	CompilerResult compileInProcess(String[] args, final CompilerConfiguration config, String[] sourceFiles)
			throws CompilerException;

}
