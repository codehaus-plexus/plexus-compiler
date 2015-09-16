package org.codehaus.plexus.compiler.j2objc;

/*
 * Copyright 2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.codehaus.plexus.compiler.AbstractCompiler;
import org.codehaus.plexus.compiler.CompilerConfiguration;
import org.codehaus.plexus.compiler.CompilerException;
import org.codehaus.plexus.compiler.CompilerMessage;
import org.codehaus.plexus.compiler.CompilerMessage.Kind;
import org.codehaus.plexus.compiler.CompilerOutputStyle;
import org.codehaus.plexus.compiler.CompilerResult;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;
import org.codehaus.plexus.util.cli.WriterStreamConsumer;

/**
 * A plexus compiler which use J2ObjC . It is derived from the CSharpCompiler to
 * compile with J2ObjC.
 * 
 * @author <a href="mailto:ludovic.maitre@effervens.com">Ludovic
 *         Ma&icirc;tre</a>
 * @see CSharpCompiler
 * @plexus.component role="org.codehaus.plexus.compiler.Compiler"
 *                   role-hint="j2objc"
 */
public class J2ObjCCompiler extends AbstractCompiler {

	private static final String X_BOOTCLASSPATH = "Xbootclasspath";

	/**
	 * -J<flag> Pass Java <flag>, such as -Xmx1G, to the system runtime.
	 */
	private static final String J_FLAG = "J";

	/**
	 * --batch-translate-max=<n> The maximum number of source files that are
	 * translated. together. Batching speeds up translation, but requires more
	 * memory.
	 */
	private static final String BATCH_SIZE = "batch-translate-max";

	/**
	 * Put the arguments of j2objc who takes one dash inside an array, in order
	 * the check the command line.
	 */
	private static final List<String> ONE_DASH_ARGS = Arrays
			.asList(new String[] { "-pluginpath", "-pluginoptions", "-t",
					"-Xno-jsni-warnings", "-sourcepath", "-classpath", "-d",
					"-encoding", "-g", "-q", "-v", "-Werror", "-h", "-use-arc",
					"-use-reference-counting", "-x" });

	/**
	 * Put the command line arguments with 2 dashes inside an array, in order
	 * the check the command line and build it.
	 */
	private static final List<String> TWO_DASH_ARGS = Arrays
			.asList(new String[] { "--build-closure", "--dead-code-report",
					"--doc-comments", "--no-extract-unsequenced",
					"--generate-deprecated", "--mapping", "--no-class-methods",
					"--no-final-methods-functions",
					"--no-hide-private-members", "--no-package-directories",
					"--prefix", "--prefixes", "--preserve-full-paths",
					"--strip-gwt-incompatible", "--strip-reflection",
					"--segmented-headers", "--timing-info", "--quiet",
					"--verbose", "--help" });

	public J2ObjCCompiler() {
		super(CompilerOutputStyle.ONE_OUTPUT_FILE_PER_INPUT_FILE, ".java",
				null, null);
	}

	// ----------------------------------------------------------------------
	// Compiler Implementation
	// ----------------------------------------------------------------------

	public boolean canUpdateTarget(CompilerConfiguration configuration)
			throws CompilerException {
		return false;
	}

	public CompilerResult performCompile(CompilerConfiguration config)
			throws CompilerException {
		File destinationDir = new File(config.getOutputLocation());
		if (!destinationDir.exists()) {
			destinationDir.mkdirs();
		}

		config.setSourceFiles(null);

		String[] sourceFiles = J2ObjCCompiler.getSourceFiles(config);

		if (sourceFiles.length == 0) {
			return new CompilerResult().success(true);
		}

		System.out.println("Compiling " + sourceFiles.length + " "
				+ "source file" + (sourceFiles.length == 1 ? "" : "s") + " to "
				+ destinationDir.getAbsolutePath());

		String[] args = buildCompilerArguments(config, sourceFiles);

		List<CompilerMessage> messages;

		if (config.isFork()) {
			messages = compileOutOfProcess(config.getWorkingDirectory(),
					config.getBuildDirectory(), findExecutable(config), args);
		} else {
			throw new CompilerException(
					"This compiler doesn't support in-process compilation.");
		}

		return new CompilerResult().compilerMessages(messages);
	}

	public String[] createCommandLine(CompilerConfiguration config)
			throws CompilerException {
		return buildCompilerArguments(config,
				J2ObjCCompiler.getSourceFiles(config));
	}

	/**
	 * Find the executable given in the configuration or use j2objc from the
	 * PATH.
	 * 
	 * @param config
	 * @return the List<String> of args
	 */
	private String findExecutable(CompilerConfiguration config) {
		String executable = config.getExecutable();

		if (!StringUtils.isEmpty(executable)) {
			return executable;
		}

		return "j2objc";
	}

	/**
	 * Build the compiler arguments : 
	 * <li>the output location is used for -d of j2objc) 
	 * <li>the classpath entries are added to -classpath 
	 * <li>the sourcefiles are listed at the end of the command line 
	 * <li>the configuration can contain any of the arguments
	 * 
	 * @param config
	 * @param sourceFiles
	 * @return The List<String> to give to the command line tool
	 * @throws CompilerException
	 */
	private String[] buildCompilerArguments(CompilerConfiguration config,
			String[] sourceFiles) throws CompilerException {
		/*
		 * j2objc --help Usage: j2objc <options> <source files>
		 */
		List<String> args = new ArrayList<String>();
		Map<String, String> compilerArguments = config
				.getCustomCompilerArgumentsAsMap();

		// Verbose
		if (config.isVerbose()) {
			args.add("-v");
		}

		// warningsAreErrors
		if (config.isWarningsAreErrors()) {
			args.add("-Werror");
		}

		// Destination/output directory
		args.add("-d");
		args.add(config.getOutputLocation());

		if (!config.getClasspathEntries().isEmpty()) {
			List<String> classpath = new ArrayList<String>();
			for (String element : config.getClasspathEntries()) {
				File f = new File(element);
				classpath.add(f.getAbsolutePath());

				classpath.add(element);
			}
			args.add("-classpath");
			args.add(StringUtils.join(classpath.toArray(), File.pathSeparator));
		}

		if (config.isVerbose()) {
			System.out.println("Args: ");
		}

		for (String k : compilerArguments.keySet()) {
			if (config.isVerbose()) {
				System.out.println(k + "=" + compilerArguments.get(k));
			}
			String v = compilerArguments.get(k);
			if (J_FLAG.equals(k)) {
				args.add(J_FLAG + v);
			} else if (X_BOOTCLASSPATH.equals(k)) {
				args.add(X_BOOTCLASSPATH + ":" + v);
			} else if (BATCH_SIZE.equals(k)) {
				args.add("-" + BATCH_SIZE + "=" + v);
			} else {
				if (TWO_DASH_ARGS.contains(k)) {
					args.add("-" + k);
				} else if (ONE_DASH_ARGS.contains(k)) {
					args.add(k);
				} else {
					throw new IllegalArgumentException("The argument " + k
							+ " isnt't a flag recognized by J2ObjC.");
				}
				if (v != null) {
					args.add(v);
				}
			}
		}

		for (String sourceFile : sourceFiles) {
			args.add(sourceFile);
		}

		return args.toArray(new String[args.size()]);
	}

	private List<CompilerMessage> compileOutOfProcess(File workingDirectory,
			File target, String executable, String[] args)
			throws CompilerException {

		Commandline cli = new Commandline();

		cli.setWorkingDirectory(workingDirectory.getAbsolutePath());

		cli.setExecutable(executable);

		cli.addArguments(args);

		Writer stringWriter = new StringWriter();

		StreamConsumer out = new WriterStreamConsumer(stringWriter);

		StreamConsumer err = new WriterStreamConsumer(stringWriter);

		int returnCode;

		List<CompilerMessage> messages;

		try {
			returnCode = CommandLineUtils.executeCommandLine(cli, out, err);

			messages = parseCompilerOutput(new BufferedReader(new StringReader(
					stringWriter.toString())));
		} catch (CommandLineException e) {
			throw new CompilerException(
					"Error while executing the external compiler.", e);
		} catch (IOException e) {
			throw new CompilerException(
					"Error while executing the external compiler.", e);
		}

		if (returnCode != 0 && messages.isEmpty()) {
			// TODO: exception?
			messages.add(new CompilerMessage(
					"Failure executing the compiler, but could not parse the error:"
							+ EOL + stringWriter.toString(), Kind.ERROR));
		}

		return messages;
	}

	public static List<CompilerMessage> parseCompilerOutput(
			BufferedReader bufferedReader) throws IOException {
		List<CompilerMessage> messages = new ArrayList<CompilerMessage>();

		String line = bufferedReader.readLine();

		while (line != null) {
			CompilerMessage compilerError = DefaultJ2ObjCCompilerParser
					.parseLine(line);

			if (compilerError != null) {
				messages.add(compilerError);
			}

			line = bufferedReader.readLine();
		}

		return messages;
	}

}
