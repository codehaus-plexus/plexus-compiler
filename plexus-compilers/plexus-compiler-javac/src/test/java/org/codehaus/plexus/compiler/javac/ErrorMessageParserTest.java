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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.codehaus.plexus.compiler.CompilerMessage;
import org.codehaus.plexus.util.Os;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.codehaus.plexus.compiler.javac.JavacCompiler.Messages.*;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @author Alexander Kriegisch
 */
public class ErrorMessageParserTest {
    private static final String EOL = System.getProperty("line.separator");
    private static final String UNIDENTIFIED_LOG_LINES =
            "These log lines should be cut off\n" + "when preceding known error message headers\n";

    @Test
    public void testDeprecationMessage() throws Exception {
        String error =
                "target/compiler-src/testDeprecation/Foo.java:1: warning: Date(java.lang.String) in java.util.Date has been deprecated"
                        + EOL + "import java.util.Date;public class Foo{    private Date date = new Date( \"foo\");}"
                        + EOL + "                                                               ^"
                        + EOL;

        CompilerMessage compilerError = JavacCompiler.parseModernError(0, error);

        assertThat(compilerError, notNullValue());

        assertThat(compilerError.isError(), is(false));

        assertThat(compilerError.getMessage(), is("Date(java.lang.String) in java.util.Date has been deprecated"));

        assertThat(compilerError.getStartColumn(), is(63));

        assertThat(compilerError.getEndColumn(), is(66));

        assertThat(compilerError.getStartLine(), is(1));

        assertThat(compilerError.getEndLine(), is(1));
    }

    @Test
    public void testWarningMessage() {
        String error = "target/compiler-src/testWarning/Foo.java:8: warning: finally clause cannot complete normally"
                + EOL + "        finally { return; }"
                + EOL + "                          ^"
                + EOL;

        CompilerMessage compilerError = JavacCompiler.parseModernError(0, error);

        assertThat(compilerError, notNullValue());

        assertThat(compilerError.isError(), is(false));

        assertThat(compilerError.getMessage(), is("finally clause cannot complete normally"));

        assertThat(compilerError.getStartColumn(), is(26));

        assertThat(compilerError.getEndColumn(), is(27));

        assertThat(compilerError.getStartLine(), is(8));

        assertThat(compilerError.getEndLine(), is(8));
    }

    @Test
    public void testErrorMessage() {
        String error = "Foo.java:7: not a statement" + EOL + "         i;" + EOL + "         ^" + EOL;

        CompilerMessage compilerError = JavacCompiler.parseModernError(1, error);

        assertThat(compilerError, notNullValue());

        assertThat(compilerError.isError(), is(true));

        assertThat(compilerError.getMessage(), is("not a statement"));

        assertThat(compilerError.getStartColumn(), is(9));

        assertThat(compilerError.getEndColumn(), is(11));

        assertThat(compilerError.getStartLine(), is(7));

        assertThat(compilerError.getEndLine(), is(7));
    }

    @Test
    public void testUnknownSymbolError() {
        String error = "./org/codehaus/foo/UnknownSymbol.java:7: cannot find symbol" + EOL + "symbol  : method foo()"
                + EOL + "location: class org.codehaus.foo.UnknownSymbol"
                + EOL + "        foo();"
                + EOL + "        ^"
                + EOL;

        CompilerMessage compilerError = JavacCompiler.parseModernError(1, error);

        assertThat(compilerError, notNullValue());

        assertThat(compilerError.isError(), is(true));

        assertThat(
                compilerError.getMessage(),
                is("cannot find symbol" + EOL + "symbol  : method foo()" + EOL
                        + "location: class org.codehaus.foo.UnknownSymbol"));

        assertThat(compilerError.getStartColumn(), is(8));

        assertThat(compilerError.getEndColumn(), is(14));

        assertThat(compilerError.getStartLine(), is(7));

        assertThat(compilerError.getEndLine(), is(7));
    }

    @Test
    public void testTwoErrors() throws IOException {
        String errors = "./org/codehaus/foo/ExternalDeps.java:4: package org.apache.commons.lang does not exist" + EOL
                + "import org.apache.commons.lang.StringUtils;"
                + EOL + "                               ^"
                + EOL + "./org/codehaus/foo/ExternalDeps.java:12: cannot find symbol"
                + EOL + "symbol  : variable StringUtils"
                + EOL + "location: class org.codehaus.foo.ExternalDeps"
                + EOL + "          System.out.println( StringUtils.upperCase( str)  );"
                + EOL + "                              ^"
                + EOL + "2 errors"
                + EOL;

        List<CompilerMessage> messages =
                JavacCompiler.parseModernStream(1, new BufferedReader(new StringReader(errors)));

        assertThat(messages.size(), is(2));
    }

    @Test
    public void testAnotherTwoErrors() throws IOException {
        String errors = "./org/codehaus/foo/ExternalDeps.java:4: package org.apache.commons.lang does not exist" + EOL
                + "import org.apache.commons.lang.StringUtils;"
                + EOL + "                               ^"
                + EOL + "./org/codehaus/foo/ExternalDeps.java:12: cannot find symbol"
                + EOL + "symbol  : variable StringUtils"
                + EOL + "location: class org.codehaus.foo.ExternalDeps"
                + EOL + "          System.out.println( StringUtils.upperCase( str)  );"
                + EOL + "                              ^"
                + EOL + "2 errors"
                + EOL;

        List<CompilerMessage> messages =
                JavacCompiler.parseModernStream(1, new BufferedReader(new StringReader(errors)));

        assertThat(messages.size(), is(2));
    }

    @Test
    public void testAssertError() throws IOException {
        String errors =
                "./org/codehaus/foo/ReservedWord.java:5: as of release 1.4, 'assert' is a keyword, and may not be used as an identifier"
                        + EOL + "(try -source 1.3 or lower to use 'assert' as an identifier)"
                        + EOL + "        String assert;"
                        + EOL + "               ^"
                        + EOL + "1 error"
                        + EOL;

        List<CompilerMessage> messages =
                JavacCompiler.parseModernStream(1, new BufferedReader(new StringReader(errors)));

        assertThat(messages.size(), is(1));
    }

    @Test
    public void testLocalizedWarningNotTreatedAsError() throws IOException {
        String errors =
                "./src/main/java/Main.java:9: \u8b66\u544a:[deprecation] java.io.File \u306e toURL() \u306f\u63a8\u5968\u3055\u308c\u307e\u305b\u3093\u3002"
                        + EOL + "    new File( path ).toURL()"
                        + EOL + "                    ^"
                        + EOL + "\u8b66\u544a 1 \u500b"
                        + EOL;

        List<CompilerMessage> messages =
                JavacCompiler.parseModernStream(0, new BufferedReader(new StringReader(errors)));

        assertThat(messages.size(), is(1));
        assertThat(messages.get(0).isError(), is(false));
    }

    @Test
    public void testUnixFileNames() {
        String error = "/my/prj/src/main/java/test/prj/App.java:11: not a statement" + EOL
                + "        System.out.println( \"Hello World!\" );x"
                + EOL + "                                             ^"
                + EOL;

        CompilerMessage compilerError = JavacCompiler.parseModernError(1, error);

        assertThat(
                String.valueOf(compilerError), is("/my/prj/src/main/java/test/prj/App.java:[11,45] not a statement"));
    }

    @Test
    public void testWindowsDriveLettersMCOMPILER140() {
        String error =
                "c:\\Documents and Settings\\My Self\\Documents\\prj\\src\\main\\java\\test\\prj\\App.java:11: not a statement"
                        + EOL + "        System.out.println( \"Hello World!\" );x"
                        + EOL + "                                             ^"
                        + EOL;

        CompilerMessage compilerError = JavacCompiler.parseModernError(1, error);

        assertThat(
                String.valueOf(compilerError),
                is(
                        "c:\\Documents and Settings\\My Self\\Documents\\prj\\src\\main\\java\\test\\prj\\App.java:[11,45] not a statement"));
    }

    /**
     * Test that CRLF is parsed correctly wrt. the filename in warnings.
     *
     * @throws Exception
     */
    @Test
    public void testCRLF_windows() throws Exception {
        // This test is only relevant on windows (test hardcodes EOL)
        if (!Os.isFamily("windows")) {
            return;
        }

        String CRLF = new String(new byte[] {(byte) 0x0D, (byte) 0x0A});
        String errors = "warning: [options] bootstrap class path not set in conjunction with -source 1.6" + CRLF
                + "[parsing started RegularFileObject[C:\\commander\\pre\\ec\\ec-http\\src\\main\\java\\com\\electriccloud\\http\\HttpServerImpl.java]]"
                + CRLF + "[parsing completed 19ms]"
                + CRLF
                + "[parsing started RegularFileObject[C:\\commander\\pre\\ec\\ec-http\\src\\main\\java\\com\\electriccloud\\http\\HttpServer.java]]"
                + CRLF + "[parsing completed 1ms]"
                + CRLF
                + "[parsing started RegularFileObject[C:\\commander\\pre\\ec\\ec-http\\src\\main\\java\\com\\electriccloud\\http\\HttpServerAware.java]]"
                + CRLF + "[parsing completed 1ms]"
                + CRLF
                + "[parsing started RegularFileObject[C:\\commander\\pre\\ec\\ec-http\\src\\main\\java\\com\\electriccloud\\http\\HttpUtil.java]]"
                + CRLF + "[parsing completed 3ms]"
                + CRLF
                + "[parsing started RegularFileObject[C:\\commander\\pre\\ec\\ec-http\\src\\main\\java\\com\\electriccloud\\http\\HttpThreadPool.java]]"
                + CRLF + "[parsing completed 3ms]"
                + CRLF
                + "[parsing started RegularFileObject[C:\\commander\\pre\\ec\\ec-http\\src\\main\\java\\com\\electriccloud\\http\\HttpQueueAware.java]]"
                + CRLF + "[parsing completed 0ms]"
                + CRLF
                + "[parsing started RegularFileObject[C:\\commander\\pre\\ec\\ec-http\\src\\main\\java\\com\\electriccloud\\http\\HttpThreadPoolAware.java]]"
                + CRLF + "[parsing completed 1ms]"
                + CRLF + "[search path for source files: C:\\commander\\pre\\ec\\ec-http\\src\\main\\java]"
                + CRLF
                + "[search path for class files: C:\\Program Files\\Java\\jdk1.7.0_04\\jre\\lib\\resources.jar,C:\\Program Files\\Java\\jdk1.7.0_04\\jre\\lib\\rt.jar,C:\\Program Files\\Java\\jdk1.7.0_04\\jre\\lib\\sunrsasign.jar,C:\\Program Files\\Java\\jdk1.7.0_04\\jre\\lib\\jsse.jar,C:\\Program Files\\Java\\jdk1.7.0_04\\jre\\lib\\jce.jar,C:\\Program Files\\Java\\jdk1.7.0_04\\jre\\lib\\charsets.jar,C:\\Program Files\\Java\\jdk1.7.0_04\\jre\\lib\\jfr.jar,C:\\Program Files\\Java\\jdk1.7.0_04\\jre\\classes,C:\\Program Files\\Java\\jdk1.7.0_04\\jre\\lib\\ext\\dnsns.jar,C:\\Program Files\\Java\\jdk1.7.0_04\\jre\\lib\\ext\\localedata.jar,C:\\Program Files\\Java\\jdk1.7.0_04\\jre\\lib\\ext\\sunec.jar,C:\\Program Files\\Java\\jdk1.7.0_04\\jre\\lib\\ext\\sunjce_provider.jar,C:\\Program Files\\Java\\jdk1.7.0_04\\jre\\lib\\ext\\sunmscapi.jar,C:\\Program Files\\Java\\jdk1.7.0_04\\jre\\lib\\ext\\zipfs.jar,C:\\commander\\pre\\ec\\ec-http\\target\\classes,C:\\Users\\anders\\.m2\\repository\\com\\electriccloud\\ec-core\\1.0.0-SNAPSHOT\\ec-core-1.0.0-SNAPSHOT.jar,C:\\Users\\anders\\.m2\\repository\\com\\electriccloud\\ec-lock\\1.0.0-SNAPSHOT\\ec-lock-1.0.0-SNAPSHOT.jar,C:\\Users\\anders\\.m2\\repository\\com\\electriccloud\\ec-timer\\1.0.0-SNAPSHOT\\ec-timer-1.0.0-SNAPSHOT.jar,C:\\Users\\anders\\.m2\\repository\\org\\apache\\commons\\commons-math\\2.2\\commons-math-2.2.jar,C:\\Users\\anders\\.m2\\repository\\com\\electriccloud\\ec-validation\\1.0.0-SNAPSHOT\\ec-validation-1.0.0-SNAPSHOT.jar,C:\\Users\\anders\\.m2\\repository\\com\\electriccloud\\ec-xml\\1.0.0-SNAPSHOT\\ec-xml-1.0.0-SNAPSHOT.jar,C:\\Users\\anders\\.m2\\repository\\commons-beanutils\\commons-beanutils\\1.8.3-PATCH1\\commons-beanutils-1.8.3-PATCH1.jar,C:\\Users\\anders\\.m2\\repository\\commons-collections\\commons-collections\\3.2.1\\commons-collections-3.2.1.jar,C:\\Users\\anders\\.m2\\repository\\dom4j\\dom4j\\1.6.1-PATCH1\\dom4j-1.6.1-PATCH1.jar,C:\\Users\\anders\\.m2\\repository\\javax\\validation\\validation-api\\1.0.0.GA\\validation-api-1.0.0.GA.jar,C:\\Users\\anders\\.m2\\repository\\org\\codehaus\\jackson\\jackson-core-asl\\1.9.7\\jackson-core-asl-1.9.7.jar,C:\\Users\\anders\\.m2\\repository\\org\\codehaus\\jackson\\jackson-mapper-asl\\1.9.7\\jackson-mapper-asl-1.9.7.jar,C:\\Users\\anders\\.m2\\repository\\org\\hibernate\\hibernate-core\\3.6.7-PATCH14\\hibernate-core-3.6.7-PATCH14.jar,C:\\Users\\anders\\.m2\\repository\\antlr\\antlr\\2.7.6\\antlr-2.7.6.jar,C:\\Users\\anders\\.m2\\repository\\org\\hibernate\\hibernate-commons-annotations\\3.2.0.Final\\hibernate-commons-annotations-3.2.0.Final.jar,C:\\Users\\anders\\.m2\\repository\\javax\\transaction\\jta\\1.1\\jta-1.1.jar,C:\\Users\\anders\\.m2\\repository\\org\\hibernate\\javax\\persistence\\hibernate-jpa-2.0-api\\1.0.1.Final\\hibernate-jpa-2.0-api-1.0.1.Final.jar,C:\\Users\\anders\\.m2\\repository\\org\\hyperic\\sigar\\1.6.5.132\\sigar-1.6.5.132.jar,C:\\Users\\anders\\.m2\\repository\\org\\springframework\\spring-context\\3.1.1.RELEASE-PATCH1\\spring-context-3.1.1.RELEASE-PATCH1.jar,C:\\Users\\anders\\.m2\\repository\\org\\springframework\\spring-expression\\3.1.1.RELEASE-PATCH1\\spring-expression-3.1.1.RELEASE-PATCH1.jar,C:\\Users\\anders\\.m2\\repository\\org\\springframework\\spring-core\\3.1.1.RELEASE-PATCH1\\spring-core-3.1.1.RELEASE-PATCH1.jar,C:\\Users\\anders\\.m2\\repository\\tanukisoft\\wrapper\\3.5.14\\wrapper-3.5.14.jar,C:\\Users\\anders\\.m2\\repository\\com\\electriccloud\\ec-log\\1.0.0-SNAPSHOT\\ec-log-1.0.0-SNAPSHOT.jar,C:\\Users\\anders\\.m2\\repository\\ch\\qos\\logback\\logback-classic\\1.0.3-PATCH4\\logback-classic-1.0.3-PATCH4.jar,C:\\Users\\anders\\.m2\\repository\\ch\\qos\\logback\\logback-core\\1.0.3-PATCH4\\logback-core-1.0.3-PATCH4.jar,C:\\Users\\anders\\.m2\\repository\\org\\slf4j\\slf4j-api\\1.6.4\\slf4j-api-1.6.4.jar,C:\\Users\\anders\\.m2\\repository\\org\\slf4j\\jul-to-slf4j\\1.6.4\\jul-to-slf4j-1.6.4.jar,C:\\Users\\anders\\.m2\\repository\\com\\electriccloud\\ec-queue\\1.0.0-SNAPSHOT\\ec-queue-1.0.0-SNAPSHOT.jar,C:\\Users\\anders\\.m2\\repository\\com\\electriccloud\\ec-security\\1.0.0-SNAPSHOT\\ec-security-1.0.0-SNAPSHOT.jar,C:\\Users\\anders\\.m2\\repository\\com\\electriccloud\\ec-acl\\1.0.0-SNAPSHOT\\ec-acl-1.0.0-SNAPSHOT.jar,C:\\Users\\anders\\.m2\\repository\\com\\electriccloud\\ec-transaction\\1.0.0-SNAPSHOT\\ec-transaction-1.0.0-SNAPSHOT.jar,C:\\Users\\anders\\.m2\\repository\\org\\aspectj\\aspectjrt\\1.7.0.M1-PATCH1\\aspectjrt-1.7.0.M1-PATCH1.jar,C:\\Users\\anders\\.m2\\repository\\com\\electriccloud\\ec-crypto\\1.0.0-SNAPSHOT\\ec-crypto-1.0.0-SNAPSHOT.jar,C:\\Users\\anders\\.m2\\repository\\org\\bouncycastle\\bcprov-jdk16\\1.46\\bcprov-jdk16-1.46.jar,C:\\Users\\anders\\.m2\\repository\\com\\electriccloud\\ec-property\\1.0.0-SNAPSHOT\\ec-property-1.0.0-SNAPSHOT.jar,C:\\Users\\anders\\.m2\\repository\\org\\apache\\commons\\commons-lang3\\3.1\\commons-lang3-3.1.jar,C:\\Users\\anders\\.m2\\repository\\org\\springframework\\spring-tx\\3.1.1.RELEASE-PATCH1\\spring-tx-3.1.1.RELEASE-PATCH1.jar,C:\\Users\\anders\\.m2\\repository\\org\\aopalliance\\com.springsource.org.aopalliance\\1.0.0\\com.springsource.org.aopalliance-1.0.0.jar,C:\\Users\\anders\\.m2\\repository\\org\\springframework\\ldap\\spring-ldap-core\\1.3.1.RELEASE\\spring-ldap-core-1.3.1.RELEASE.jar,C:\\Users\\anders\\.m2\\repository\\commons-lang\\commons-lang\\2.5\\commons-lang-2.5.jar,C:\\Users\\anders\\.m2\\repository\\org\\springframework\\security\\spring-security-core\\2.0.6.PATCH1\\spring-security-core-2.0.6.PATCH1.jar,C:\\Users\\anders\\.m2\\repository\\com\\electriccloud\\ec-util\\1.0.0-SNAPSHOT\\ec-util-1.0.0-SNAPSHOT.jar,C:\\Users\\anders\\.m2\\repository\\cglib\\cglib-nodep\\2.2.2\\cglib-nodep-2.2.2.jar,C:\\Users\\anders\\.m2\\repository\\org\\apache\\commons\\commons-digester3\\3.2-PATCH5\\commons-digester3-3.2-PATCH5.jar,C:\\Users\\anders\\.m2\\repository\\cglib\\cglib\\2.2.2\\cglib-2.2.2.jar,C:\\Users\\anders\\.m2\\repository\\asm\\asm\\3.3.1\\asm-3.3.1.jar,C:\\Users\\anders\\.m2\\repository\\org\\springframework\\spring-aop\\3.1.1.RELEASE-PATCH1\\spring-aop-3.1.1.RELEASE-PATCH1.jar,C:\\Users\\anders\\.m2\\repository\\com\\google\\guava\\guava\\12.0\\guava-12.0.jar,C:\\Users\\anders\\.m2\\repository\\com\\google\\code\\findbugs\\jsr305\\2.0.0\\jsr305-2.0.0.jar,C:\\Users\\anders\\.m2\\repository\\com\\intellij\\annotations\\116.108\\annotations-116.108.jar,C:\\Users\\anders\\.m2\\repository\\commons-io\\commons-io\\2.3\\commons-io-2.3.jar,C:\\Users\\anders\\.m2\\repository\\net\\jcip\\jcip-annotations\\1.0\\jcip-annotations-1.0.jar,C:\\Users\\anders\\.m2\\repository\\org\\apache\\httpcomponents\\httpclient\\4.2\\httpclient-4.2.jar,C:\\Users\\anders\\.m2\\repository\\commons-codec\\commons-codec\\1.6\\commons-codec-1.6.jar,C:\\Users\\anders\\.m2\\repository\\org\\apache\\httpcomponents\\httpcore\\4.2\\httpcore-4.2.jar,C:\\Users\\anders\\.m2\\repository\\org\\eclipse\\jetty\\jetty-server\\8.1.4.v20120524\\jetty-server-8.1.4.v20120524.jar,C:\\Users\\anders\\.m2\\repository\\org\\eclipse\\jetty\\orbit\\javax.servlet\\3.0.0.v201112011016\\javax.servlet-3.0.0.v201112011016.jar,C:\\Users\\anders\\.m2\\repository\\org\\eclipse\\jetty\\jetty-continuation\\8.1.4.v20120524\\jetty-continuation-8.1.4.v20120524.jar,C:\\Users\\anders\\.m2\\repository\\org\\eclipse\\jetty\\jetty-http\\8.1.4.v20120524\\jetty-http-8.1.4.v20120524.jar,C:\\Users\\anders\\.m2\\repository\\org\\eclipse\\jetty\\jetty-io\\8.1.4.v20120524\\jetty-io-8.1.4.v20120524.jar,C:\\Users\\anders\\.m2\\repository\\org\\eclipse\\jetty\\jetty-util\\8.1.4.v20120524\\jetty-util-8.1.4.v20120524.jar,C:\\Users\\anders\\.m2\\repository\\org\\mortbay\\jetty\\servlet-api\\3.0.20100224\\servlet-api-3.0.20100224.jar,C:\\Users\\anders\\.m2\\repository\\org\\springframework\\spring-beans\\3.1.1.RELEASE-PATCH1\\spring-beans-3.1.1.RELEASE-PATCH1.jar,C:\\Users\\anders\\.m2\\repository\\org\\springframework\\spring-asm\\3.1.1.RELEASE-PATCH1\\spring-asm-3.1.1.RELEASE-PATCH1.jar,.]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Program Files\\Java\\jdk1.7.0_04\\lib\\ct.sym(META-INF/sym/rt.jar/java/net/BindException.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Program Files\\Java\\jdk1.7.0_04\\lib\\ct.sym(META-INF/sym/rt.jar/java/util/ArrayList.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Program Files\\Java\\jdk1.7.0_04\\lib\\ct.sym(META-INF/sym/rt.jar/java/util/Collection.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Program Files\\Java\\jdk1.7.0_04\\lib\\ct.sym(META-INF/sym/rt.jar/java/util/Collections.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Program Files\\Java\\jdk1.7.0_04\\lib\\ct.sym(META-INF/sym/rt.jar/java/util/HashSet.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Program Files\\Java\\jdk1.7.0_04\\lib\\ct.sym(META-INF/sym/rt.jar/java/util/concurrent/TimeUnit.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Users\\anders\\.m2\\repository\\org\\eclipse\\jetty\\jetty-server\\8.1.4.v20120524\\jetty-server-8.1.4.v20120524.jar(org/eclipse/jetty/server/Handler.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Users\\anders\\.m2\\repository\\org\\eclipse\\jetty\\jetty-server\\8.1.4.v20120524\\jetty-server-8.1.4.v20120524.jar(org/eclipse/jetty/server/Server.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Users\\anders\\.m2\\repository\\org\\eclipse\\jetty\\jetty-server\\8.1.4.v20120524\\jetty-server-8.1.4.v20120524.jar(org/eclipse/jetty/server/nio/SelectChannelConnector.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Users\\anders\\.m2\\repository\\org\\eclipse\\jetty\\jetty-server\\8.1.4.v20120524\\jetty-server-8.1.4.v20120524.jar(org/eclipse/jetty/server/ssl/SslSelectChannelConnector.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Users\\anders\\.m2\\repository\\org\\eclipse\\jetty\\jetty-util\\8.1.4.v20120524\\jetty-util-8.1.4.v20120524.jar(org/eclipse/jetty/util/ssl/SslContextFactory.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Users\\anders\\.m2\\repository\\com\\intellij\\annotations\\116.108\\annotations-116.108.jar(org/jetbrains/annotations/NonNls.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Users\\anders\\.m2\\repository\\com\\intellij\\annotations\\116.108\\annotations-116.108.jar(org/jetbrains/annotations/NotNull.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Users\\anders\\.m2\\repository\\com\\intellij\\annotations\\116.108\\annotations-116.108.jar(org/jetbrains/annotations/TestOnly.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Users\\anders\\.m2\\repository\\org\\springframework\\spring-beans\\3.1.1.RELEASE-PATCH1\\spring-beans-3.1.1.RELEASE-PATCH1.jar(org/springframework/beans/factory/BeanNameAware.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Users\\anders\\.m2\\repository\\org\\springframework\\spring-beans\\3.1.1.RELEASE-PATCH1\\spring-beans-3.1.1.RELEASE-PATCH1.jar(org/springframework/beans/factory/annotation/Autowired.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Users\\anders\\.m2\\repository\\com\\google\\guava\\guava\\12.0\\guava-12.0.jar(com/google/common/collect/Iterables.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Users\\anders\\.m2\\repository\\com\\electriccloud\\ec-log\\1.0.0-SNAPSHOT\\ec-log-1.0.0-SNAPSHOT.jar(com/electriccloud/log/Log.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Users\\anders\\.m2\\repository\\com\\electriccloud\\ec-log\\1.0.0-SNAPSHOT\\ec-log-1.0.0-SNAPSHOT.jar(com/electriccloud/log/LogFactory.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Users\\anders\\.m2\\repository\\com\\electriccloud\\ec-core\\1.0.0-SNAPSHOT\\ec-core-1.0.0-SNAPSHOT.jar(com/electriccloud/service/ServiceManager.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Users\\anders\\.m2\\repository\\com\\electriccloud\\ec-core\\1.0.0-SNAPSHOT\\ec-core-1.0.0-SNAPSHOT.jar(com/electriccloud/service/ServiceState.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Users\\anders\\.m2\\repository\\com\\electriccloud\\ec-util\\1.0.0-SNAPSHOT\\ec-util-1.0.0-SNAPSHOT.jar(com/electriccloud/util/ExceptionUtil.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Users\\anders\\.m2\\repository\\com\\electriccloud\\ec-util\\1.0.0-SNAPSHOT\\ec-util-1.0.0-SNAPSHOT.jar(com/electriccloud/util/SystemUtil.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Users\\anders\\.m2\\repository\\com\\electriccloud\\ec-util\\1.0.0-SNAPSHOT\\ec-util-1.0.0-SNAPSHOT.jar(com/electriccloud/util/ToString.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Users\\anders\\.m2\\repository\\com\\electriccloud\\ec-util\\1.0.0-SNAPSHOT\\ec-util-1.0.0-SNAPSHOT.jar(com/electriccloud/util/ToStringSupport.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Program Files\\Java\\jdk1.7.0_04\\lib\\ct.sym(META-INF/sym/rt.jar/java/lang/String.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Program Files\\Java\\jdk1.7.0_04\\lib\\ct.sym(META-INF/sym/rt.jar/java/lang/Object.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Program Files\\Java\\jdk1.7.0_04\\lib\\ct.sym(META-INF/sym/rt.jar/java/io/Serializable.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Program Files\\Java\\jdk1.7.0_04\\lib\\ct.sym(META-INF/sym/rt.jar/java/lang/Comparable.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Program Files\\Java\\jdk1.7.0_04\\lib\\ct.sym(META-INF/sym/rt.jar/java/lang/CharSequence.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Program Files\\Java\\jdk1.7.0_04\\lib\\ct.sym(META-INF/sym/rt.jar/java/lang/Enum.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Users\\anders\\.m2\\repository\\com\\electriccloud\\ec-util\\1.0.0-SNAPSHOT\\ec-util-1.0.0-SNAPSHOT.jar(com/electriccloud/util/ToStringAware.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Users\\anders\\.m2\\repository\\org\\springframework\\spring-beans\\3.1.1.RELEASE-PATCH1\\spring-beans-3.1.1.RELEASE-PATCH1.jar(org/springframework/beans/factory/Aware.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Users\\anders\\.m2\\repository\\com\\electriccloud\\ec-core\\1.0.0-SNAPSHOT\\ec-core-1.0.0-SNAPSHOT.jar(com/electriccloud/service/Service.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Program Files\\Java\\jdk1.7.0_04\\lib\\ct.sym(META-INF/sym/rt.jar/java/lang/Integer.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Program Files\\Java\\jdk1.7.0_04\\lib\\ct.sym(META-INF/sym/rt.jar/java/util/concurrent/RejectedExecutionException.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Users\\anders\\.m2\\repository\\org\\eclipse\\jetty\\jetty-util\\8.1.4.v20120524\\jetty-util-8.1.4.v20120524.jar(org/eclipse/jetty/util/component/AbstractLifeCycle.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Users\\anders\\.m2\\repository\\org\\eclipse\\jetty\\jetty-util\\8.1.4.v20120524\\jetty-util-8.1.4.v20120524.jar(org/eclipse/jetty/util/thread/ThreadPool.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Users\\anders\\.m2\\repository\\com\\electriccloud\\ec-queue\\1.0.0-SNAPSHOT\\ec-queue-1.0.0-SNAPSHOT.jar(com/electriccloud/queue/ExecuteQueue.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Users\\anders\\.m2\\repository\\com\\electriccloud\\ec-core\\1.0.0-SNAPSHOT\\ec-core-1.0.0-SNAPSHOT.jar(com/electriccloud/service/ServiceManagerAware.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Users\\anders\\.m2\\repository\\com\\electriccloud\\ec-util\\1.0.0-SNAPSHOT\\ec-util-1.0.0-SNAPSHOT.jar(com/electriccloud/util/ToStringImpl.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Users\\anders\\.m2\\repository\\org\\eclipse\\jetty\\jetty-util\\8.1.4.v20120524\\jetty-util-8.1.4.v20120524.jar(org/eclipse/jetty/util/component/LifeCycle.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Program Files\\Java\\jdk1.7.0_04\\lib\\ct.sym(META-INF/sym/rt.jar/java/lang/InterruptedException.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Program Files\\Java\\jdk1.7.0_04\\lib\\ct.sym(META-INF/sym/rt.jar/java/lang/Runnable.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Program Files\\Java\\jdk1.7.0_04\\lib\\ct.sym(META-INF/sym/rt.jar/java/lang/Exception.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Program Files\\Java\\jdk1.7.0_04\\lib\\ct.sym(META-INF/sym/rt.jar/java/io/IOException.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Program Files\\Java\\jdk1.7.0_04\\lib\\ct.sym(META-INF/sym/rt.jar/java/security/KeyManagementException.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Program Files\\Java\\jdk1.7.0_04\\lib\\ct.sym(META-INF/sym/rt.jar/java/security/NoSuchAlgorithmException.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Program Files\\Java\\jdk1.7.0_04\\lib\\ct.sym(META-INF/sym/rt.jar/java/security/SecureRandom.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Program Files\\Java\\jdk1.7.0_04\\lib\\ct.sym(META-INF/sym/rt.jar/javax/net/ssl/SSLContext.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Program Files\\Java\\jdk1.7.0_04\\lib\\ct.sym(META-INF/sym/rt.jar/javax/net/ssl/TrustManager.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Users\\anders\\.m2\\repository\\org\\apache\\httpcomponents\\httpcore\\4.2\\httpcore-4.2.jar(org/apache/http/HttpResponse.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Users\\anders\\.m2\\repository\\org\\apache\\httpcomponents\\httpclient\\4.2\\httpclient-4.2.jar(org/apache/http/client/HttpClient.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Users\\anders\\.m2\\repository\\org\\apache\\httpcomponents\\httpclient\\4.2\\httpclient-4.2.jar(org/apache/http/client/methods/HttpGet.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Users\\anders\\.m2\\repository\\org\\apache\\httpcomponents\\httpclient\\4.2\\httpclient-4.2.jar(org/apache/http/client/methods/HttpPost.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Users\\anders\\.m2\\repository\\org\\apache\\httpcomponents\\httpclient\\4.2\\httpclient-4.2.jar(org/apache/http/client/methods/HttpUriRequest.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Users\\anders\\.m2\\repository\\org\\apache\\httpcomponents\\httpclient\\4.2\\httpclient-4.2.jar(org/apache/http/conn/scheme/Scheme.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Users\\anders\\.m2\\repository\\org\\apache\\httpcomponents\\httpclient\\4.2\\httpclient-4.2.jar(org/apache/http/conn/ssl/SSLSocketFactory.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Users\\anders\\.m2\\repository\\org\\apache\\httpcomponents\\httpcore\\4.2\\httpcore-4.2.jar(org/apache/http/entity/StringEntity.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Users\\anders\\.m2\\repository\\org\\apache\\httpcomponents\\httpclient\\4.2\\httpclient-4.2.jar(org/apache/http/impl/client/DefaultConnectionKeepAliveStrategy.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Users\\anders\\.m2\\repository\\org\\apache\\httpcomponents\\httpclient\\4.2\\httpclient-4.2.jar(org/apache/http/impl/client/DefaultHttpClient.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Users\\anders\\.m2\\repository\\org\\apache\\httpcomponents\\httpclient\\4.2\\httpclient-4.2.jar(org/apache/http/impl/client/DefaultHttpRequestRetryHandler.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Users\\anders\\.m2\\repository\\org\\apache\\httpcomponents\\httpclient\\4.2\\httpclient-4.2.jar(org/apache/http/impl/conn/tsccm/ThreadSafeClientConnManager.class)]]"
                + CRLF
                + "C:\\commander\\pre\\ec\\ec-http\\src\\main\\java\\com\\electriccloud\\http\\HttpUtil.java:31: warning: [deprecation] ThreadSafeClientConnManager in org.apache.http.impl.conn.tsccm has been deprecated"
                + CRLF + "import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;"
                + CRLF + "                                      ^"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Users\\anders\\.m2\\repository\\org\\apache\\httpcomponents\\httpcore\\4.2\\httpcore-4.2.jar(org/apache/http/params/HttpParams.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Users\\anders\\.m2\\repository\\org\\apache\\httpcomponents\\httpcore\\4.2\\httpcore-4.2.jar(org/apache/http/protocol/HttpContext.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Users\\anders\\.m2\\repository\\org\\apache\\httpcomponents\\httpcore\\4.2\\httpcore-4.2.jar(org/apache/http/util/EntityUtils.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Users\\anders\\.m2\\repository\\com\\electriccloud\\ec-security\\1.0.0-SNAPSHOT\\ec-security-1.0.0-SNAPSHOT.jar(com/electriccloud/security/DummyX509TrustManager.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Users\\anders\\.m2\\repository\\org\\apache\\httpcomponents\\httpclient\\4.2\\httpclient-4.2.jar(org/apache/http/conn/scheme/SchemeLayeredSocketFactory.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Users\\anders\\.m2\\repository\\org\\apache\\httpcomponents\\httpclient\\4.2\\httpclient-4.2.jar(org/apache/http/conn/scheme/SchemeSocketFactory.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Users\\anders\\.m2\\repository\\org\\apache\\httpcomponents\\httpclient\\4.2\\httpclient-4.2.jar(org/apache/http/conn/scheme/LayeredSchemeSocketFactory.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Users\\anders\\.m2\\repository\\org\\apache\\httpcomponents\\httpclient\\4.2\\httpclient-4.2.jar(org/apache/http/conn/scheme/LayeredSocketFactory.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Users\\anders\\.m2\\repository\\org\\apache\\httpcomponents\\httpclient\\4.2\\httpclient-4.2.jar(org/apache/http/conn/scheme/SocketFactory.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Users\\anders\\.m2\\repository\\org\\apache\\httpcomponents\\httpcore\\4.2\\httpcore-4.2.jar(org/apache/http/params/CoreConnectionPNames.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Program Files\\Java\\jdk1.7.0_04\\lib\\ct.sym(META-INF/sym/rt.jar/java/lang/SuppressWarnings.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Program Files\\Java\\jdk1.7.0_04\\lib\\ct.sym(META-INF/sym/rt.jar/java/lang/annotation/Retention.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Program Files\\Java\\jdk1.7.0_04\\lib\\ct.sym(META-INF/sym/rt.jar/java/lang/annotation/RetentionPolicy.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Program Files\\Java\\jdk1.7.0_04\\lib\\ct.sym(META-INF/sym/rt.jar/java/lang/annotation/Target.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Program Files\\Java\\jdk1.7.0_04\\lib\\ct.sym(META-INF/sym/rt.jar/java/lang/annotation/ElementType.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Users\\anders\\.m2\\repository\\com\\google\\guava\\guava\\12.0\\guava-12.0.jar(com/google/common/annotations/GwtCompatible.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Users\\anders\\.m2\\repository\\com\\google\\guava\\guava\\12.0\\guava-12.0.jar(com/google/common/annotations/GwtIncompatible.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Users\\anders\\.m2\\repository\\com\\electriccloud\\ec-core\\1.0.0-SNAPSHOT\\ec-core-1.0.0-SNAPSHOT.jar(com/electriccloud/infoset/InfosetType.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Program Files\\Java\\jdk1.7.0_04\\lib\\ct.sym(META-INF/sym/rt.jar/java/lang/annotation/Annotation.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Program Files\\Java\\jdk1.7.0_04\\lib\\ct.sym(META-INF/sym/rt.jar/java/lang/Override.class)]]"
                + CRLF + "[checking com.electriccloud.http.HttpServerImpl]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Program Files\\Java\\jdk1.7.0_04\\lib\\ct.sym(META-INF/sym/rt.jar/java/lang/Error.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Program Files\\Java\\jdk1.7.0_04\\lib\\ct.sym(META-INF/sym/rt.jar/java/lang/Throwable.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Program Files\\Java\\jdk1.7.0_04\\lib\\ct.sym(META-INF/sym/rt.jar/java/lang/RuntimeException.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Program Files\\Java\\jdk1.7.0_04\\lib\\ct.sym(META-INF/sym/rt.jar/java/lang/AutoCloseable.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Program Files\\Java\\jdk1.7.0_04\\lib\\ct.sym(META-INF/sym/rt.jar/java/lang/Class.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Program Files\\Java\\jdk1.7.0_04\\lib\\ct.sym(META-INF/sym/rt.jar/java/lang/Number.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Program Files\\Java\\jdk1.7.0_04\\lib\\ct.sym(META-INF/sym/rt.jar/java/util/AbstractList.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Program Files\\Java\\jdk1.7.0_04\\lib\\ct.sym(META-INF/sym/rt.jar/java/util/AbstractCollection.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Program Files\\Java\\jdk1.7.0_04\\lib\\ct.sym(META-INF/sym/rt.jar/java/lang/Iterable.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Program Files\\Java\\jdk1.7.0_04\\lib\\ct.sym(META-INF/sym/rt.jar/java/lang/Byte.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Program Files\\Java\\jdk1.7.0_04\\lib\\ct.sym(META-INF/sym/rt.jar/java/lang/Character.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Program Files\\Java\\jdk1.7.0_04\\lib\\ct.sym(META-INF/sym/rt.jar/java/lang/Short.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Users\\anders\\.m2\\repository\\org\\eclipse\\jetty\\jetty-server\\8.1.4.v20120524\\jetty-server-8.1.4.v20120524.jar(org/eclipse/jetty/server/nio/AbstractNIOConnector.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Users\\anders\\.m2\\repository\\org\\eclipse\\jetty\\jetty-server\\8.1.4.v20120524\\jetty-server-8.1.4.v20120524.jar(org/eclipse/jetty/server/AbstractConnector.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Users\\anders\\.m2\\repository\\org\\eclipse\\jetty\\jetty-util\\8.1.4.v20120524\\jetty-util-8.1.4.v20120524.jar(org/eclipse/jetty/util/component/AggregateLifeCycle.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Users\\anders\\.m2\\repository\\org\\eclipse\\jetty\\jetty-server\\8.1.4.v20120524\\jetty-server-8.1.4.v20120524.jar(org/eclipse/jetty/server/Connector.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Users\\anders\\.m2\\repository\\org\\eclipse\\jetty\\jetty-util\\8.1.4.v20120524\\jetty-util-8.1.4.v20120524.jar(org/eclipse/jetty/util/component/Destroyable.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Users\\anders\\.m2\\repository\\org\\eclipse\\jetty\\jetty-util\\8.1.4.v20120524\\jetty-util-8.1.4.v20120524.jar(org/eclipse/jetty/util/component/Dumpable.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Users\\anders\\.m2\\repository\\org\\eclipse\\jetty\\jetty-http\\8.1.4.v20120524\\jetty-http-8.1.4.v20120524.jar(org/eclipse/jetty/http/HttpBuffers.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Users\\anders\\.m2\\repository\\org\\eclipse\\jetty\\jetty-server\\8.1.4.v20120524\\jetty-server-8.1.4.v20120524.jar(org/eclipse/jetty/server/handler/HandlerWrapper.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Users\\anders\\.m2\\repository\\org\\eclipse\\jetty\\jetty-server\\8.1.4.v20120524\\jetty-server-8.1.4.v20120524.jar(org/eclipse/jetty/server/handler/AbstractHandlerContainer.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Users\\anders\\.m2\\repository\\org\\eclipse\\jetty\\jetty-server\\8.1.4.v20120524\\jetty-server-8.1.4.v20120524.jar(org/eclipse/jetty/server/handler/AbstractHandler.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Program Files\\Java\\jdk1.7.0_04\\lib\\ct.sym(META-INF/sym/rt.jar/java/net/SocketException.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Program Files\\Java\\jdk1.7.0_04\\lib\\ct.sym(META-INF/sym/rt.jar/java/lang/Thread.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Program Files\\Java\\jdk1.7.0_04\\lib\\ct.sym(META-INF/sym/rt.jar/java/lang/IllegalStateException.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Program Files\\Java\\jdk1.7.0_04\\lib\\ct.sym(META-INF/sym/rt.jar/java/util/AbstractSet.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Program Files\\Java\\jdk1.7.0_04\\lib\\ct.sym(META-INF/sym/rt.jar/java/util/Iterator.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Program Files\\Java\\jdk1.7.0_04\\lib\\ct.sym(META-INF/sym/rt.jar/java/lang/IllegalArgumentException.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Program Files\\Java\\jdk1.7.0_04\\lib\\ct.sym(META-INF/sym/rt.jar/java/util/Locale.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Program Files\\Java\\jdk1.7.0_04\\lib\\ct.sym(META-INF/sym/rt.jar/java/lang/Long.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Program Files\\Java\\jdk1.7.0_04\\lib\\ct.sym(META-INF/sym/rt.jar/java/lang/Float.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Program Files\\Java\\jdk1.7.0_04\\lib\\ct.sym(META-INF/sym/rt.jar/java/lang/Double.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Program Files\\Java\\jdk1.7.0_04\\lib\\ct.sym(META-INF/sym/rt.jar/java/lang/Boolean.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Program Files\\Java\\jdk1.7.0_04\\lib\\ct.sym(META-INF/sym/rt.jar/java/lang/Void.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Program Files\\Java\\jdk1.7.0_04\\lib\\ct.sym(META-INF/sym/rt.jar/java/lang/AssertionError.class)]]"
                + CRLF
                + "[wrote RegularFileObject[C:\\commander\\pre\\ec\\ec-http\\target\\classes\\com\\electriccloud\\http\\HttpServerImpl.class]]"
                + CRLF + "[checking com.electriccloud.http.HttpServer]"
                + CRLF
                + "[wrote RegularFileObject[C:\\commander\\pre\\ec\\ec-http\\target\\classes\\com\\electriccloud\\http\\HttpServer.class]]"
                + CRLF + "[checking com.electriccloud.http.HttpThreadPoolAware]"
                + CRLF
                + "[wrote RegularFileObject[C:\\commander\\pre\\ec\\ec-http\\target\\classes\\com\\electriccloud\\http\\HttpThreadPoolAware.class]]"
                + CRLF + "[checking com.electriccloud.http.HttpThreadPool]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Program Files\\Java\\jdk1.7.0_04\\lib\\ct.sym(META-INF/sym/rt.jar/java/util/concurrent/Future.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Program Files\\Java\\jdk1.7.0_04\\lib\\ct.sym(META-INF/sym/rt.jar/java/util/concurrent/Callable.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Program Files\\Java\\jdk1.7.0_04\\lib\\ct.sym(META-INF/sym/rt.jar/java/util/Date.class)]]"
                + CRLF
                + "[wrote RegularFileObject[C:\\commander\\pre\\ec\\ec-http\\target\\classes\\com\\electriccloud\\http\\HttpThreadPool.class]]"
                + CRLF + "[checking com.electriccloud.http.HttpQueueAware]"
                + CRLF
                + "[wrote RegularFileObject[C:\\commander\\pre\\ec\\ec-http\\target\\classes\\com\\electriccloud\\http\\HttpQueueAware.class]]"
                + CRLF + "[checking com.electriccloud.http.HttpServerAware]"
                + CRLF
                + "[wrote RegularFileObject[C:\\commander\\pre\\ec\\ec-http\\target\\classes\\com\\electriccloud\\http\\HttpServerAware.class]]"
                + CRLF + "[checking com.electriccloud.http.HttpUtil]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Program Files\\Java\\jdk1.7.0_04\\lib\\ct.sym(META-INF/sym/rt.jar/java/net/URI.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Users\\anders\\.m2\\repository\\org\\apache\\httpcomponents\\httpclient\\4.2\\httpclient-4.2.jar(org/apache/http/client/methods/HttpRequestBase.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Users\\anders\\.m2\\repository\\org\\apache\\httpcomponents\\httpcore\\4.2\\httpcore-4.2.jar(org/apache/http/message/AbstractHttpMessage.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Users\\anders\\.m2\\repository\\org\\apache\\httpcomponents\\httpcore\\4.2\\httpcore-4.2.jar(org/apache/http/HttpMessage.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Users\\anders\\.m2\\repository\\org\\apache\\httpcomponents\\httpclient\\4.2\\httpclient-4.2.jar(org/apache/http/impl/client/AbstractHttpClient.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Users\\anders\\.m2\\repository\\org\\apache\\httpcomponents\\httpcore\\4.2\\httpcore-4.2.jar(org/apache/http/annotation/GuardedBy.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Users\\anders\\.m2\\repository\\org\\apache\\httpcomponents\\httpclient\\4.2\\httpclient-4.2.jar(org/apache/http/client/ResponseHandler.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Users\\anders\\.m2\\repository\\org\\apache\\httpcomponents\\httpclient\\4.2\\httpclient-4.2.jar(org/apache/http/client/ClientProtocolException.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Users\\anders\\.m2\\repository\\org\\apache\\httpcomponents\\httpcore\\4.2\\httpcore-4.2.jar(org/apache/http/HttpEntity.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Users\\anders\\.m2\\repository\\org\\apache\\httpcomponents\\httpclient\\4.2\\httpclient-4.2.jar(org/apache/http/client/methods/HttpEntityEnclosingRequestBase.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Users\\anders\\.m2\\repository\\org\\apache\\httpcomponents\\httpcore\\4.2\\httpcore-4.2.jar(org/apache/http/entity/AbstractHttpEntity.class)]]"
                + CRLF
                + "C:\\commander\\pre\\ec\\ec-http\\src\\main\\java\\com\\electriccloud\\http\\HttpUtil.java:151: warning: [deprecation] ThreadSafeClientConnManager in org.apache.http.impl.conn.tsccm has been deprecated"
                + CRLF + "        ThreadSafeClientConnManager connectionManager ="
                + CRLF + "        ^"
                + CRLF
                + "C:\\commander\\pre\\ec\\ec-http\\src\\main\\java\\com\\electriccloud\\http\\HttpUtil.java:152: warning: [deprecation] ThreadSafeClientConnManager in org.apache.http.impl.conn.tsccm has been deprecated"
                + CRLF + "            new ThreadSafeClientConnManager();"
                + CRLF + "                ^"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Program Files\\Java\\jdk1.7.0_04\\lib\\ct.sym(META-INF/sym/rt.jar/java/security/GeneralSecurityException.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Program Files\\Java\\jdk1.7.0_04\\lib\\ct.sym(META-INF/sym/rt.jar/javax/net/ssl/X509TrustManager.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Program Files\\Java\\jdk1.7.0_04\\lib\\ct.sym(META-INF/sym/rt.jar/java/security/KeyException.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Users\\anders\\.m2\\repository\\org\\apache\\httpcomponents\\httpclient\\4.2\\httpclient-4.2.jar(org/apache/http/conn/ssl/X509HostnameVerifier.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Program Files\\Java\\jdk1.7.0_04\\lib\\ct.sym(META-INF/sym/rt.jar/javax/net/ssl/SSLSocketFactory.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Users\\anders\\.m2\\repository\\org\\apache\\httpcomponents\\httpclient\\4.2\\httpclient-4.2.jar(org/apache/http/conn/scheme/HostNameResolver.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Program Files\\Java\\jdk1.7.0_04\\lib\\ct.sym(META-INF/sym/rt.jar/javax/net/ssl/HostnameVerifier.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Users\\anders\\.m2\\repository\\org\\apache\\httpcomponents\\httpclient\\4.2\\httpclient-4.2.jar(org/apache/http/conn/ssl/TrustStrategy.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Program Files\\Java\\jdk1.7.0_04\\lib\\ct.sym(META-INF/sym/rt.jar/java/security/KeyStore.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Users\\anders\\.m2\\repository\\org\\apache\\httpcomponents\\httpclient\\4.2\\httpclient-4.2.jar(org/apache/http/conn/scheme/SchemeRegistry.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Users\\anders\\.m2\\repository\\org\\apache\\httpcomponents\\httpclient\\4.2\\httpclient-4.2.jar(org/apache/http/conn/ClientConnectionManager.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Users\\anders\\.m2\\repository\\org\\apache\\httpcomponents\\httpclient\\4.2\\httpclient-4.2.jar(org/apache/http/client/HttpRequestRetryHandler.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Users\\anders\\.m2\\repository\\org\\apache\\httpcomponents\\httpclient\\4.2\\httpclient-4.2.jar(org/apache/http/conn/ConnectionKeepAliveStrategy.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Users\\anders\\.m2\\repository\\org\\apache\\httpcomponents\\httpcore\\4.2\\httpcore-4.2.jar(org/apache/http/ParseException.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Program Files\\Java\\jdk1.7.0_04\\lib\\ct.sym(META-INF/sym/rt.jar/java/io/UnsupportedEncodingException.class)]]"
                + CRLF
                + "[wrote RegularFileObject[C:\\commander\\pre\\ec\\ec-http\\target\\classes\\com\\electriccloud\\http\\HttpUtil$1.class]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Program Files\\Java\\jdk1.7.0_04\\lib\\ct.sym(META-INF/sym/rt.jar/java/lang/StringBuilder.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Program Files\\Java\\jdk1.7.0_04\\lib\\ct.sym(META-INF/sym/rt.jar/java/lang/AbstractStringBuilder.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Program Files\\Java\\jdk1.7.0_04\\lib\\ct.sym(META-INF/sym/rt.jar/java/lang/StringBuffer.class)]]"
                + CRLF
                + "[loading ZipFileIndexFileObject[C:\\Program Files\\Java\\jdk1.7.0_04\\lib\\ct.sym(META-INF/sym/rt.jar/javax/net/ssl/KeyManager.class)]]"
                + CRLF
                + "[wrote RegularFileObject[C:\\commander\\pre\\ec\\ec-http\\target\\classes\\com\\electriccloud\\http\\HttpUtil.class]]"
                + CRLF + "[total 654ms]"
                + CRLF + "4 warnings"
                + CRLF;
        List<CompilerMessage> compilerMessages =
                JavacCompiler.parseModernStream(0, new BufferedReader(new StringReader(errors)));
        assertThat("count", compilerMessages.size(), is(187));
        List<CompilerMessage> compilerErrors = new ArrayList<>(3);
        for (CompilerMessage message : compilerMessages) {
            if (message.getKind() != CompilerMessage.Kind.OTHER) {
                compilerErrors.add(message);
            }
        }

        assertEquivalent(
                new CompilerMessage(
                        "[options] bootstrap class path not set in conjunction with -source " + "1.6",
                        CompilerMessage.Kind.WARNING),
                compilerErrors.get(0));
        CompilerMessage error1 = compilerErrors.get(1);
        assertThat(
                "file",
                error1.getFile(),
                is("C:\\commander\\pre\\ec\\ec-http\\src\\main\\java\\com\\electriccloud\\http\\HttpUtil.java"));
        assertThat(
                "message",
                error1.getMessage(),
                is("[deprecation] ThreadSafeClientConnManager in org.apache.http.impl.conn.tsccm has been deprecated"));
        assertThat("line", error1.getStartLine(), is(31));
        assertThat("column", error1.getStartColumn(), is(38));
        CompilerMessage error2 = compilerErrors.get(2);
        assertThat(
                "file",
                error2.getFile(),
                is("C:\\commander\\pre\\ec\\ec-http\\src\\main\\java\\com\\electriccloud\\http\\HttpUtil.java"));
        assertThat(
                "message",
                error2.getMessage(),
                is("[deprecation] ThreadSafeClientConnManager in org.apache.http.impl.conn.tsccm has been deprecated"));
        assertThat("line", error2.getStartLine(), is(151));
        assertThat("column", error2.getStartColumn(), is(8));
        CompilerMessage error3 = compilerErrors.get(3);
        assertThat(
                "file",
                error3.getFile(),
                is("C:\\commander\\pre\\ec\\ec-http\\src\\main\\java\\com\\electriccloud\\http\\HttpUtil.java"));
        assertThat(
                "message",
                error3.getMessage(),
                is("[deprecation] ThreadSafeClientConnManager in org.apache.http.impl.conn.tsccm has been deprecated"));
        assertThat("line", error3.getStartLine(), is(152));
        assertThat("column", error3.getStartColumn(), is(16));
    }

    @Test
    public void testJava6Error() throws Exception {
        String out = "Error.java:3: cannot find symbol" + EOL + "symbol  : class Properties"
                + EOL + "location: class Error"
                + EOL + "                Properties p = new Properties();"
                + EOL + "                ^"
                + EOL + "Error.java:3: cannot find symbol"
                + EOL + "symbol  : class Properties"
                + EOL + "location: class Error"
                + EOL + "                Properties p = new Properties();"
                + EOL + "                                   ^"
                + EOL + "2 errors";

        List<CompilerMessage> compilerErrors =
                JavacCompiler.parseModernStream(1, new BufferedReader(new StringReader(out)));

        assertThat(compilerErrors, notNullValue());

        CompilerMessage message1 = compilerErrors.get(0);

        assertThat(message1.isError(), is(true));

        assertThat(
                message1.getMessage(),
                is("cannot find symbol" + EOL + "symbol  : class Properties" + EOL + "location: class Error"));

        assertThat(message1.getStartColumn(), is(16));

        assertThat(message1.getEndColumn(), is(26));

        assertThat(message1.getStartLine(), is(3));

        assertThat(message1.getEndLine(), is(3));

        CompilerMessage message2 = compilerErrors.get(1);

        assertThat(message2.isError(), is(true));

        assertThat(
                message2.getMessage(),
                is("cannot find symbol" + EOL + "symbol  : class Properties" + EOL + "location: class Error"));

        assertThat(message2.getStartColumn(), is(35));

        assertThat(message2.getEndColumn(), is(48));

        assertThat(message2.getStartLine(), is(3));

        assertThat(message2.getEndLine(), is(3));
    }

    @Test
    public void testJava7Error() throws Exception {
        String out =
                "Error.java:3: error: cannot find symbol" + EOL + "                Properties p = new Properties();"
                        + EOL + "                ^"
                        + EOL + "  symbol:   class Properties"
                        + EOL + "  location: class Error"
                        + EOL + "Error.java:3: error: cannot find symbol"
                        + EOL + "                Properties p = new Properties();"
                        + EOL + "                                   ^"
                        + EOL + "  symbol:   class Properties"
                        + EOL + "  location: class Error"
                        + EOL + "2 errors";

        List<CompilerMessage> compilerErrors =
                JavacCompiler.parseModernStream(1, new BufferedReader(new StringReader(out)));

        assertThat(compilerErrors, notNullValue());

        CompilerMessage message1 = compilerErrors.get(0);

        assertThat(message1.isError(), is(true));

        assertThat(
                message1.getMessage(),
                is("cannot find symbol" + EOL + "  symbol:   class Properties" + EOL + "  location: class Error"));

        assertThat(message1.getStartColumn(), is(16));

        assertThat(message1.getEndColumn(), is(26));

        assertThat(message1.getStartLine(), is(3));

        assertThat(message1.getEndLine(), is(3));

        CompilerMessage message2 = compilerErrors.get(1);

        assertThat(message2.isError(), is(true));

        assertThat(
                message2.getMessage(),
                is("cannot find symbol" + EOL + "  symbol:   class Properties" + EOL + "  location: class Error"));

        assertThat(message2.getStartColumn(), is(35));

        assertThat(message2.getEndColumn(), is(48));

        assertThat(message2.getStartLine(), is(3));

        assertThat(message2.getEndLine(), is(3));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("testStackTraceWithUnknownHeader_args")
    public void testStackTraceWithUnknownHeader(String scenario, String stackTraceHeader) throws Exception {
        String stackTraceWithHeader = UNIDENTIFIED_LOG_LINES + stackTraceHeader + stackTraceInternalCompilerError;

        List<CompilerMessage> compilerMessages =
                JavacCompiler.parseModernStream(4, new BufferedReader(new StringReader(stackTraceWithHeader)));

        assertThat(compilerMessages, notNullValue());
        assertThat(compilerMessages, hasSize(1));

        String message = compilerMessages.get(0).getMessage().replaceAll(EOL, "\n");
        // Parser retains neither unidentified log lines nor slightly modified stack trace header
        assertThat(message, not(containsString(UNIDENTIFIED_LOG_LINES)));
        assertThat(message, not(containsString(stackTraceHeader)));
        // Parser returns stack strace without any preceding lines
        assertThat(message, startsWith(stackTraceInternalCompilerError));
    }

    private static Stream<Arguments> testStackTraceWithUnknownHeader_args() {
        return Stream.of(
                Arguments.of(
                        "modified compiler error header",
                        FILE_A_BUG_ERROR_HEADERS[0].replaceAll("\\{0\\}", "21").replaceAll("bug", "beetle")),
                Arguments.of(
                        "modified annotation processor error header",
                        ANNOTATION_PROCESSING_ERROR_HEADERS[0].replaceAll("uncaught", "undandled")),
                Arguments.of(
                        "modified out of resources error header",
                        SYSTEM_OUT_OF_RESOURCES_ERROR_HEADERS[0].replaceAll("resources", "memory")),
                Arguments.of("modified I/O error header", IO_ERROR_HEADERS[0].replaceAll("input/output", "I/O")),
                Arguments.of(
                        "modified plugin error header", PLUGIN_ERROR_HEADERS[0].replaceAll("uncaught", "unhandled")));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("testBugParade_args")
    public void testBugParade(String jdkAndLocale, String stackTraceHeader) throws Exception {
        String stackTraceWithHeader = UNIDENTIFIED_LOG_LINES + stackTraceHeader + stackTraceInternalCompilerError;

        List<CompilerMessage> compilerMessages =
                JavacCompiler.parseModernStream(4, new BufferedReader(new StringReader(stackTraceWithHeader)));

        assertThat(compilerMessages, notNullValue());
        assertThat(compilerMessages, hasSize(1));

        String message = compilerMessages.get(0).getMessage().replaceAll(EOL, "\n");
        // Parser retains stack trace header
        assertThat(message, startsWith(stackTraceHeader));
        assertThat(message, endsWith(stackTraceInternalCompilerError));
    }

    private static final String stackTraceInternalCompilerError =
            "com.sun.tools.javac.code.Symbol$CompletionFailure: class file for java.util.Optional not found\n"
                    + "\tat com.sun.tools.javac.comp.MemberEnter.baseEnv(MemberEnter.java:1388)\n"
                    + "\tat com.sun.tools.javac.comp.MemberEnter.complete(MemberEnter.java:1046)\n"
                    + "\tat com.sun.tools.javac.code.Symbol.complete(Symbol.java:574)\n"
                    + "\tat com.sun.tools.javac.code.Symbol$ClassSymbol.complete(Symbol.java:1037)\n"
                    + "\tat com.sun.tools.javac.code.Symbol$ClassSymbol.flags(Symbol.java:973)\n"
                    + "\tat com.sun.tools.javac.code.Symbol$ClassSymbol.getKind(Symbol.java:1101)\n"
                    + "\tat com.sun.tools.javac.code.Kinds.kindName(Kinds.java:162)\n"
                    + "\tat com.sun.tools.javac.comp.Check.duplicateError(Check.java:329)\n"
                    + "\tat com.sun.tools.javac.comp.Check.checkUnique(Check.java:3435)\n"
                    + "\tat com.sun.tools.javac.comp.Enter.visitTypeParameter(Enter.java:454)\n"
                    + "\tat com.sun.tools.javac.tree.JCTree$JCTypeParameter.accept(JCTree.java:2224)\n"
                    + "\tat com.sun.tools.javac.comp.Enter.classEnter(Enter.java:258)\n"
                    + "\tat com.sun.tools.javac.comp.Enter.classEnter(Enter.java:272)\n"
                    + "\tat com.sun.tools.javac.comp.Enter.visitClassDef(Enter.java:418)\n"
                    + "\tat com.sun.tools.javac.tree.JCTree$JCClassDecl.accept(JCTree.java:693)\n"
                    + "\tat com.sun.tools.javac.comp.Enter.classEnter(Enter.java:258)\n"
                    + "\tat com.sun.tools.javac.comp.Enter.classEnter(Enter.java:272)\n"
                    + "\tat com.sun.tools.javac.comp.Enter.visitTopLevel(Enter.java:334)\n"
                    + "\tat com.sun.tools.javac.tree.JCTree$JCCompilationUnit.accept(JCTree.java:518)\n"
                    + "\tat com.sun.tools.javac.comp.Enter.classEnter(Enter.java:258)\n"
                    + "\tat com.sun.tools.javac.comp.Enter.classEnter(Enter.java:272)\n"
                    + "\tat com.sun.tools.javac.comp.Enter.complete(Enter.java:486)\n"
                    + "\tat com.sun.tools.javac.comp.Enter.main(Enter.java:471)\n"
                    + "\tat com.sun.tools.javac.main.JavaCompiler.enterTrees(JavaCompiler.java:982)\n"
                    + "\tat com.sun.tools.javac.main.JavaCompiler.compile(JavaCompiler.java:857)\n"
                    + "\tat com.sun.tools.javac.main.Main.compile(Main.java:523)\n"
                    + "\tat com.sun.tools.javac.main.Main.compile(Main.java:381)\n"
                    + "\tat com.sun.tools.javac.main.Main.compile(Main.java:370)\n"
                    + "\tat com.sun.tools.javac.main.Main.compile(Main.java:361)\n"
                    + "\tat com.sun.tools.javac.Main.compile(Main.java:56)\n"
                    + "\tat com.sun.tools.javac.Main.main(Main.java:42)\n";

    private static Stream<Arguments> testBugParade_args() {
        return Stream.of(
                Arguments.of("JDK 8 English", FILE_A_BUG_ERROR_HEADERS[0].replaceFirst("\\{0\\}", "21")),
                Arguments.of("JDK 8 Japanese", FILE_A_BUG_ERROR_HEADERS[1].replaceFirst("\\{0\\}", "21")),
                Arguments.of("JDK 8 Chinese", FILE_A_BUG_ERROR_HEADERS[2].replaceFirst("\\{0\\}", "21")),
                Arguments.of("JDK 9 English", FILE_A_BUG_ERROR_HEADERS[3].replaceFirst("\\{0\\}", "21")),
                Arguments.of("JDK 9 Japanese", FILE_A_BUG_ERROR_HEADERS[4].replaceFirst("\\{0\\}", "21")),
                Arguments.of("JDK 9 Chinese", FILE_A_BUG_ERROR_HEADERS[5].replaceFirst("\\{0\\}", "21")),
                Arguments.of("JDK 21 English", FILE_A_BUG_ERROR_HEADERS[6].replaceFirst("\\{0\\}", "21")),
                Arguments.of("JDK 21 Japanese", FILE_A_BUG_ERROR_HEADERS[7].replaceFirst("\\{0\\}", "21")),
                Arguments.of("JDK 21 Chinese", FILE_A_BUG_ERROR_HEADERS[8].replaceFirst("\\{0\\}", "21")),
                Arguments.of("JDK 21 German", FILE_A_BUG_ERROR_HEADERS[9].replaceFirst("\\{0\\}", "21")));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("testSystemOutOfResourcesError_args")
    public void testSystemOutOfResourcesError(String jdkAndLocale, String stackTraceHeader) throws Exception {
        String stackTraceWithHeader = UNIDENTIFIED_LOG_LINES + stackTraceHeader + stackTraceSystemOutOfResourcesError;

        List<CompilerMessage> compilerMessages =
                JavacCompiler.parseModernStream(4, new BufferedReader(new StringReader(stackTraceWithHeader)));

        assertThat(compilerMessages, notNullValue());
        assertThat(compilerMessages, hasSize(1));

        String message = compilerMessages.get(0).getMessage().replaceAll(EOL, "\n");
        // Parser retains stack trace header
        assertThat(message, startsWith(stackTraceHeader));
        assertThat(message, endsWith(stackTraceSystemOutOfResourcesError));
    }

    private static final String stackTraceSystemOutOfResourcesError =
            "java.lang.OutOfMemoryError: GC overhead limit exceeded\n"
                    + "\tat com.sun.tools.javac.util.List.of(List.java:135)\n"
                    + "\tat com.sun.tools.javac.util.ListBuffer.append(ListBuffer.java:129)\n"
                    + "\tat com.sun.tools.javac.parser.JavacParser.variableDeclaratorsRest(JavacParser.java:3006)\n"
                    + "\tat com.sun.tools.javac.parser.JavacParser.classOrInterfaceBodyDeclaration(JavacParser.java:3537)\n"
                    + "\tat com.sun.tools.javac.parser.JavacParser.classOrInterfaceBody(JavacParser.java:3436)\n"
                    + "\tat com.sun.tools.javac.parser.JavacParser.classDeclaration(JavacParser.java:3285)\n"
                    + "\tat com.sun.tools.javac.parser.JavacParser.classOrInterfaceOrEnumDeclaration(JavacParser.java:3226)\n"
                    + "\tat com.sun.tools.javac.parser.JavacParser.typeDeclaration(JavacParser.java:3215)\n"
                    + "\tat com.sun.tools.javac.parser.JavacParser.parseCompilationUnit(JavacParser.java:3155)\n"
                    + "\tat com.sun.tools.javac.main.JavaCompiler.parse(JavaCompiler.java:628)\n"
                    + "\tat com.sun.tools.javac.main.JavaCompiler.parse(JavaCompiler.java:665)\n"
                    + "\tat com.sun.tools.javac.main.JavaCompiler.parseFiles(JavaCompiler.java:950)\n"
                    + "\tat com.sun.tools.javac.main.JavaCompiler.compile(JavaCompiler.java:857)\n"
                    + "\tat com.sun.tools.javac.main.Main.compile(Main.java:523)\n"
                    + "\tat com.sun.tools.javac.main.Main.compile(Main.java:381)\n"
                    + "\tat com.sun.tools.javac.main.Main.compile(Main.java:370)\n"
                    + "\tat com.sun.tools.javac.main.Main.compile(Main.java:361)\n"
                    + "\tat com.sun.tools.javac.Main.compile(Main.java:56)\n"
                    + "\tat com.sun.tools.javac.Main.main(Main.java:42)\n";

    private static Stream<Arguments> testSystemOutOfResourcesError_args() {
        return Stream.of(
                Arguments.of("JDK 8 English", SYSTEM_OUT_OF_RESOURCES_ERROR_HEADERS[0]),
                Arguments.of("JDK 8 Japanese", SYSTEM_OUT_OF_RESOURCES_ERROR_HEADERS[1]),
                Arguments.of("JDK 8 Chinese", SYSTEM_OUT_OF_RESOURCES_ERROR_HEADERS[2]),
                Arguments.of("JDK 21 English", SYSTEM_OUT_OF_RESOURCES_ERROR_HEADERS[3]),
                Arguments.of("JDK 21 Japanese", SYSTEM_OUT_OF_RESOURCES_ERROR_HEADERS[4]),
                Arguments.of("JDK 21 Chinese", SYSTEM_OUT_OF_RESOURCES_ERROR_HEADERS[5]),
                Arguments.of("JDK 21 German", SYSTEM_OUT_OF_RESOURCES_ERROR_HEADERS[6]));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("testIOError_args")
    public void testIOError(String jdkAndLocale, String stackTraceHeader) throws Exception {
        String stackTraceWithHeader = UNIDENTIFIED_LOG_LINES + stackTraceHeader + stackTraceIOError;

        List<CompilerMessage> compilerMessages =
                JavacCompiler.parseModernStream(4, new BufferedReader(new StringReader(stackTraceWithHeader)));

        assertThat(compilerMessages, notNullValue());
        assertThat(compilerMessages, hasSize(1));

        String message = compilerMessages.get(0).getMessage().replaceAll(EOL, "\n");
        // Parser retains stack trace header
        assertThat(message, startsWith(stackTraceHeader));
        assertThat(message, endsWith(stackTraceIOError));
    }

    private static final String stackTraceIOError =
            "An input/output error occurred.\n" + "Consult the following stack trace for details.\n"
                    + "java.nio.charset.MalformedInputException: Input length = 1\n"
                    + "\tat java.base/java.nio.charset.CoderResult.throwException(CoderResult.java:274)\n"
                    + "\tat java.base/sun.nio.cs.StreamDecoder.implRead(StreamDecoder.java:339)\n"
                    + "\tat java.base/sun.nio.cs.StreamDecoder.read(StreamDecoder.java:178)\n"
                    + "\tat java.base/java.io.InputStreamReader.read(InputStreamReader.java:185)\n"
                    + "\tat java.base/java.io.BufferedReader.fill(BufferedReader.java:161)\n"
                    + "\tat java.base/java.io.BufferedReader.read(BufferedReader.java:182)\n"
                    + "\tat jdk.compiler/com.sun.tools.javac.main.CommandLine$Tokenizer.<init>(CommandLine.java:143)\n"
                    + "\tat jdk.compiler/com.sun.tools.javac.main.CommandLine.loadCmdFile(CommandLine.java:129)\n"
                    + "\tat jdk.compiler/com.sun.tools.javac.main.CommandLine.appendParsedCommandArgs(CommandLine.java:71)\n"
                    + "\tat jdk.compiler/com.sun.tools.javac.main.CommandLine.parse(CommandLine.java:102)\n"
                    + "\tat jdk.compiler/com.sun.tools.javac.main.CommandLine.parse(CommandLine.java:123)\n"
                    + "\tat jdk.compiler/com.sun.tools.javac.main.Main.compile(Main.java:215)\n"
                    + "\tat jdk.compiler/com.sun.tools.javac.main.Main.compile(Main.java:170)\n"
                    + "\tat jdk.compiler/com.sun.tools.javac.Main.compile(Main.java:57)\n"
                    + "\tat jdk.compiler/com.sun.tools.javac.Main.main(Main.java:43)\n";

    private static Stream<Arguments> testIOError_args() {
        return Stream.of(
                Arguments.of("JDK 8 English", IO_ERROR_HEADERS[0]),
                Arguments.of("JDK 8 Japanese", IO_ERROR_HEADERS[1]),
                Arguments.of("JDK 8 Chinese", IO_ERROR_HEADERS[2]),
                Arguments.of("JDK 21 English", IO_ERROR_HEADERS[3]),
                Arguments.of("JDK 21 Japanese", IO_ERROR_HEADERS[4]),
                Arguments.of("JDK 21 Chinese", IO_ERROR_HEADERS[5]),
                Arguments.of("JDK 21 German", IO_ERROR_HEADERS[6]));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("testPluginError_args")
    public void testPluginError(String jdkAndLocale, String stackTraceHeader) throws Exception {
        String stackTraceWithHeader = UNIDENTIFIED_LOG_LINES + stackTraceHeader + stackTracePluginError;

        List<CompilerMessage> compilerMessages =
                JavacCompiler.parseModernStream(4, new BufferedReader(new StringReader(stackTraceWithHeader)));

        assertThat(compilerMessages, notNullValue());
        assertThat(compilerMessages, hasSize(1));

        String message = compilerMessages.get(0).getMessage().replaceAll(EOL, "\n");
        // Parser retains stack trace header
        assertThat(message, startsWith(stackTraceHeader));
        assertThat(message, endsWith(stackTracePluginError));
    }

    private static final String stackTracePluginError =
            "A plugin threw an uncaught exception.\n" + "Consult the following stack trace for details.\n"
                    + "java.lang.NoSuchMethodError: com.sun.tools.javac.util.JavacMessages.add(Lcom/sun/tools/javac/util/JavacMessages$ResourceBundleHelper;)V\n"
                    + "\tat com.google.errorprone.BaseErrorProneJavaCompiler.setupMessageBundle(BaseErrorProneJavaCompiler.java:202)\n"
                    + "\tat com.google.errorprone.ErrorProneJavacPlugin.init(ErrorProneJavacPlugin.java:40)\n"
                    + "\tat com.sun.tools.javac.main.Main.compile(Main.java:470)\n"
                    + "\tat com.sun.tools.javac.main.Main.compile(Main.java:381)\n"
                    + "\tat com.sun.tools.javac.main.Main.compile(Main.java:370)\n"
                    + "\tat com.sun.tools.javac.main.Main.compile(Main.java:361)\n"
                    + "\tat com.sun.tools.javac.Main.compile(Main.java:56)\n"
                    + "\tat com.sun.tools.javac.Main.main(Main.java:42)\n";

    private static Stream<Arguments> testPluginError_args() {
        return Stream.of(
                Arguments.of("JDK 8 English", PLUGIN_ERROR_HEADERS[0]),
                Arguments.of("JDK 8 Japanese", PLUGIN_ERROR_HEADERS[1]),
                Arguments.of("JDK 8 Chinese", PLUGIN_ERROR_HEADERS[2]),
                Arguments.of("JDK 21 English", PLUGIN_ERROR_HEADERS[3]),
                Arguments.of("JDK 21 Japanese", PLUGIN_ERROR_HEADERS[4]),
                Arguments.of("JDK 21 Chinese", PLUGIN_ERROR_HEADERS[5]),
                Arguments.of("JDK 21 German", PLUGIN_ERROR_HEADERS[6]));
    }

    @Test
    public void testNonAnchoredWarning() throws IOException {
        final String error = "warning: [options] bootstrap class path not set in conjunction with -source 1.6" + EOL
                + "1 warning" + EOL;

        final List<CompilerMessage> compilerErrors =
                JavacCompiler.parseModernStream(0, new BufferedReader(new StringReader(error)));

        assertThat(compilerErrors, notNullValue());
        assertThat(compilerErrors.size(), is(1));
        assertEquivalent(
                new CompilerMessage(
                        "[options] bootstrap class path not set in conjunction with -source 1.6",
                        CompilerMessage.Kind.WARNING),
                compilerErrors.get(0));
    }

    @Test
    public void testAnchoredWarning() throws IOException {
        final String error = "C:\\repo\\src\\it\\includes-output-when-compiler-forked\\src\\main"
                + "\\java\\MyClass.java:23: warning: [divzero] division by zero"
                + EOL + "      System.out.println(1/0);"
                + EOL + "                           ^"
                + EOL + "1 warnings"
                + EOL;

        final List<CompilerMessage> compilerErrors =
                JavacCompiler.parseModernStream(0, new BufferedReader(new StringReader(error)));

        assertThat(compilerErrors, notNullValue());
        assertThat(compilerErrors.size(), is(1));
        assertEquivalent(
                new CompilerMessage(
                        "C:\\repo\\src\\it\\includes-output-when-compiler-forked\\src\\main\\java\\MyClass" + ".java",
                        CompilerMessage.Kind.WARNING,
                        23,
                        27,
                        23,
                        30,
                        "[divzero] division by zero"),
                compilerErrors.get(0));
    }

    @Test
    public void testMixedWarnings() throws IOException {
        final String error = "warning: [options] bootstrap class path not set in conjunction with -source 1.6" + EOL
                + "C:\\repo\\src\\it\\includes-output-when-compiler-forked\\src\\main\\java"
                + "\\MyClass.java:23: warning: [divzero] division by zero"
                + EOL + "      System.out.println(1/0);"
                + EOL + "                           ^"
                + EOL + "2 warnings";

        final List<CompilerMessage> compilerErrors =
                JavacCompiler.parseModernStream(0, new BufferedReader(new StringReader(error)));

        assertThat(compilerErrors, notNullValue());
        assertThat(compilerErrors.size(), is(2));
        assertEquivalent(
                new CompilerMessage(
                        "[options] bootstrap class path not set in conjunction with -source 1.6",
                        CompilerMessage.Kind.WARNING),
                compilerErrors.get(0));
        assertEquivalent(
                new CompilerMessage(
                        "C:\\repo\\src\\it\\includes-output-when-compiler-forked\\src\\main\\java\\MyClass" + ".java",
                        CompilerMessage.Kind.WARNING,
                        23,
                        27,
                        23,
                        30,
                        "[divzero] division by zero"),
                compilerErrors.get(1));
    }

    @Test
    public void testIssue37() throws IOException {
        String error =
                "warning: [path] bad path element \"d:\\maven_repo\\.m2\\repository\\org\\ow2\\asm\\asm-xml\\5.0.3\\asm-5.0.3.jar\": no such file or directory"
                        + EOL
                        + "warning: [path] bad path element \"d:\\maven_repo\\.m2\\repository\\org\\ow2\\asm\\asm-xml\\5.0.3\\asm-util-5.0.3.jar\": no such file or directory"
                        + EOL + "warning: [options] bootstrap class path not set in conjunction with -source 1.7"
                        + EOL + "3 warnings"
                        + EOL
                        + "An exception has occurred in the compiler (9). Please file a bug against the Java compiler via the Java bug reporting page (http://bugreport.java.com) after checking the Bug Database (http://bugs.java.com) for duplicates. Include your program and the following diagnostic in your report. Thank you."
                        + EOL + "java.lang.NullPointerException"
                        + EOL + "\tat jdk.zipfs/jdk.nio.zipfs.JarFileSystem.getVersionMap(JarFileSystem.java:137)"
                        + EOL
                        + "\tat jdk.zipfs/jdk.nio.zipfs.JarFileSystem.createVersionedLinks(JarFileSystem.java:112)"
                        + EOL + "\tat jdk.zipfs/jdk.nio.zipfs.JarFileSystem.<init>(JarFileSystem.java:85)"
                        + EOL
                        + "\tat jdk.zipfs/jdk.nio.zipfs.ZipFileSystemProvider.newFileSystem(ZipFileSystemProvider.java:134)"
                        + EOL
                        + "\tat jdk.compiler/com.sun.tools.javac.file.JavacFileManager$ArchiveContainer.<init>(JavacFileManager.java:517)"
                        + EOL
                        + "\tat jdk.compiler/com.sun.tools.javac.file.JavacFileManager.getContainer(JavacFileManager.java:319)"
                        + EOL
                        + "\tat jdk.compiler/com.sun.tools.javac.file.JavacFileManager.list(JavacFileManager.java:715)"
                        + EOL + "\tat jdk.compiler/com.sun.tools.javac.code.ClassFinder.list(ClassFinder.java:722)"
                        + EOL
                        + "\tat jdk.compiler/com.sun.tools.javac.code.ClassFinder.scanUserPaths(ClassFinder.java:655)"
                        + EOL + "\tat jdk.compiler/com.sun.tools.javac.code.ClassFinder.fillIn(ClassFinder.java:526)"
                        + EOL + "\tat jdk.compiler/com.sun.tools.javac.code.ClassFinder.complete(ClassFinder.java:293)"
                        + EOL + "\tat jdk.compiler/com.sun.tools.javac.code.Symbol.complete(Symbol.java:633)"
                        + EOL
                        + "\tat jdk.compiler/com.sun.tools.javac.code.Symbol$PackageSymbol.members(Symbol.java:1120)"
                        + EOL + "\tat jdk.compiler/com.sun.tools.javac.code.Symtab.listPackageModules(Symtab.java:810)"
                        + EOL + "\tat jdk.compiler/com.sun.tools.javac.comp.Enter.visitTopLevel(Enter.java:344)"
                        + EOL
                        + "\tat jdk.compiler/com.sun.tools.javac.tree.JCTree$JCCompilationUnit.accept(JCTree.java:529)"
                        + EOL + "\tat jdk.compiler/com.sun.tools.javac.comp.Enter.classEnter(Enter.java:285)"
                        + EOL + "\tat jdk.compiler/com.sun.tools.javac.comp.Enter.classEnter(Enter.java:300)"
                        + EOL + "\tat jdk.compiler/com.sun.tools.javac.comp.Enter.complete(Enter.java:570)"
                        + EOL + "\tat jdk.compiler/com.sun.tools.javac.comp.Enter.main(Enter.java:554)"
                        + EOL
                        + "\tat jdk.compiler/com.sun.tools.javac.main.JavaCompiler.enterTrees(JavaCompiler.java:1052)"
                        + EOL + "\tat jdk.compiler/com.sun.tools.javac.main.JavaCompiler.compile(JavaCompiler.java:923)"
                        + EOL + "\tat jdk.compiler/com.sun.tools.javac.main.Main.compile(Main.java:302)"
                        + EOL + "\tat jdk.compiler/com.sun.tools.javac.main.Main.compile(Main.java:162)"
                        + EOL + "\tat jdk.compiler/com.sun.tools.javac.Main.compile(Main.java:57)"
                        + EOL + "\tat jdk.compiler/com.sun.tools.javac.Main.main(Main.java:43)";

        List<CompilerMessage> compilerErrors =
                JavacCompiler.parseModernStream(0, new BufferedReader(new StringReader(error)));

        assertThat(compilerErrors, notNullValue());
        assertThat(compilerErrors.size(), is(4));

        assertEquivalent(
                new CompilerMessage(
                        "[path] bad path element \"d:\\maven_repo\\"
                                + ".m2\\repository\\org\\ow2\\asm\\asm-xml\\5.0.3\\asm-5.0.3.jar\": no such file or directory",
                        CompilerMessage.Kind.WARNING),
                compilerErrors.get(0));
        assertEquivalent(
                new CompilerMessage(
                        "warning: [path] bad path element \"d:\\maven_repo\\.m2\\repository\\org\\ow2\\asm\\asm-xml\\5.0.3\\asm-util-5.0.3.jar\": no such file or directory",
                        CompilerMessage.Kind.WARNING),
                compilerErrors.get(1));
        assertEquivalent(
                new CompilerMessage(
                        "[options] bootstrap class path not set in conjunction with -source 1.7",
                        CompilerMessage.Kind.WARNING),
                compilerErrors.get(2));

        CompilerMessage finalMessage = compilerErrors.get(3);
        assertThat(finalMessage.getKind(), is(CompilerMessage.Kind.ERROR));
        assertThat(
                "Starts correctly", finalMessage.getMessage(), startsWith("An exception has occurred in the compiler"));
        assertThat(
                "continues through end of output",
                finalMessage.getMessage(),
                endsWith("\tat jdk.compiler/com.sun" + ".tools.javac.Main.main(Main.java:43)" + EOL));
    }

    @Test
    public void testJvmBootLayerInitializationError() throws Exception {
        String out = "Error occurred during initialization of boot layer\n"
                + "java.lang.module.FindException: Module java.xml.bind not found";

        List<CompilerMessage> compilerErrors =
                JavacCompiler.parseModernStream(1, new BufferedReader(new StringReader(UNIDENTIFIED_LOG_LINES + out)));

        assertThat(compilerErrors, notNullValue());
        assertThat(compilerErrors.size(), is(1));
        assertThat(compilerErrors.get(0).getKind(), is(CompilerMessage.Kind.ERROR));
        assertThat(compilerErrors.get(0).getMessage().replaceAll(EOL, "\n"), startsWith(out));
    }

    @Test
    public void testJvmInitializationError() throws Exception {
        String out = "Error occurred during initialization of VM\n"
                + "Initial heap size set to a larger value than the maximum heap size";

        List<CompilerMessage> compilerErrors =
                JavacCompiler.parseModernStream(1, new BufferedReader(new StringReader(UNIDENTIFIED_LOG_LINES + out)));

        assertThat(compilerErrors, notNullValue());
        assertThat(compilerErrors.size(), is(1));
        assertThat(compilerErrors.get(0).getKind(), is(CompilerMessage.Kind.ERROR));
        assertThat(compilerErrors.get(0).getMessage().replaceAll(EOL, "\n"), startsWith(out));
    }

    @Test
    public void testBadSourceFileError() throws Exception {
        String out = "/MTOOLCHAINS-19/src/main/java/ch/pecunifex/x/Cls1.java:12: error: cannot access Cls2\n"
                + "    Cls2 tvar;\n"
                + "    ^\n"
                + "  bad source file: /MTOOLCHAINS-19/src/main/java/ch/pecunifex/x/Cls2.java\n"
                + "    file does not contain class ch.pecunifex.x.Cls2\n"
                + "    Please remove or make sure it appears in the correct subdirectory of the sourcepath.";

        List<CompilerMessage> compilerErrors =
                JavacCompiler.parseModernStream(1, new BufferedReader(new StringReader(out)));

        assertThat(compilerErrors, notNullValue());

        assertThat(compilerErrors.size(), is(1));

        CompilerMessage message = compilerErrors.get(0);
        validateBadSourceFile(message);
    }

    @Test
    public void testWarningFollowedByBadSourceFileError() throws Exception {
        String out =
                "/MTOOLCHAINS-19/src/main/java/ch/pecunifex/x/Cls1.java:3: warning: FontDesignMetrics is internal proprietary API and may be removed in a future release\n"
                        + "import sun.font.FontDesignMetrics;\n"
                        + "               ^\n"
                        + "/MTOOLCHAINS-19/src/main/java/ch/pecunifex/x/Cls1.java:12: error: cannot access Cls2\n"
                        + "    Cls2 tvar;\n"
                        + "    ^\n"
                        + "  bad source file: /MTOOLCHAINS-19/src/main/java/ch/pecunifex/x/Cls2.java\n"
                        + "    file does not contain class ch.pecunifex.x.Cls2\n"
                        + "    Please remove or make sure it appears in the correct subdirectory of the sourcepath.";

        List<CompilerMessage> compilerErrors =
                JavacCompiler.parseModernStream(1, new BufferedReader(new StringReader(out)));

        assertThat(compilerErrors, notNullValue());

        assertThat(compilerErrors, hasSize(2));

        CompilerMessage firstMessage = compilerErrors.get(0);
        assertThat("Is a Warning", firstMessage.getKind(), is(CompilerMessage.Kind.WARNING));
        assertThat(
                "On Correct File",
                firstMessage.getFile(),
                is("/MTOOLCHAINS-19/src/main/java/ch/pecunifex/x/Cls1.java"));
        assertThat(
                "Internal API Warning",
                firstMessage.getMessage(),
                is("FontDesignMetrics is internal proprietary API and may be removed in a future release"));

        CompilerMessage secondMessage = compilerErrors.get(1);
        validateBadSourceFile(secondMessage);
    }

    private void validateBadSourceFile(CompilerMessage message) {
        assertThat("Is an Error", message.getKind(), is(CompilerMessage.Kind.ERROR));
        assertThat("On Correct File", message.getFile(), is("/MTOOLCHAINS-19/src/main/java/ch/pecunifex/x/Cls1.java"));
        assertThat("Message starts with access Error", message.getMessage(), startsWith("cannot access Cls2"));
    }

    private static void assertEquivalent(CompilerMessage expected, CompilerMessage actual) {
        assertThat("Message did not match", actual.getMessage(), is(expected.getMessage()));
        assertThat("Kind did not match", actual.getKind(), is(expected.getKind()));
        assertThat("File did not match", actual.getFile(), is(expected.getFile()));
        assertThat("Start line did not match", actual.getStartLine(), is(expected.getStartLine()));
        assertThat("Start column did not match", actual.getStartColumn(), is(expected.getStartColumn()));
        assertThat("End line did not match", actual.getEndLine(), is(expected.getEndLine()));
        assertThat("End column did not match", actual.getEndColumn(), is(expected.getEndColumn()));
    }
}
