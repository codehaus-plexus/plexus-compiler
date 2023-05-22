package org.codehaus.plexus.compiler.javac;

import org.codehaus.plexus.compiler.CompilerConfiguration;
import org.codehaus.plexus.compiler.CompilerException;
import org.codehaus.plexus.compiler.CompilerResult;

public interface InProcessCompiler {

	CompilerResult compileInProcess(String[] args, final CompilerConfiguration config, String[] sourceFiles)
			throws CompilerException;

}
