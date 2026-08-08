# Plexus Compiler

[![Maven Central](https://img.shields.io/maven-central/v/org.codehaus.plexus/plexus-compiler-api.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/org.codehaus.plexus/plexus-compiler-api)
[![GitHub CI](https://github.com/codehaus-plexus/plexus-compiler/actions/workflows/maven.yml/badge.svg)](https://github.com/codehaus-plexus/plexus-compiler/actions/workflows/maven.yml)
[![Reproducible Builds](https://img.shields.io/endpoint?url=https://raw.githubusercontent.com/jvm-repo-rebuild/reproducible-central/master/content/org/codehaus/plexus/plexus-compiler/badge.json)](https://github.com/jvm-repo-rebuild/reproducible-central/blob/master/content/org/codehaus/plexus/plexus-compiler/README.md)

One API over several Java compilers, so a build tool can switch between them without knowing how each is
invoked. This is what `maven-compiler-plugin` uses: its `<compilerId>` selects one of the implementations
below.

## Modules

|              Artifact              |                                       What it is                                        |
|------------------------------------|-----------------------------------------------------------------------------------------|
| `plexus-compiler-api`              | The `Compiler` interface and its configuration. Depend on this to write against the API |
| `plexus-compiler-manager`          | Looks up an implementation by id                                                        |
| `plexus-compiler-javac`            | The JDK compiler — the default                                                          |
| `plexus-compiler-eclipse`          | The Eclipse batch compiler (ECJ)                                                        |
| `plexus-compiler-javac-errorprone` | javac with [Error Prone](https://errorprone.info/)                                      |
| `plexus-compiler-aspectj`          | AspectJ                                                                                 |
| `plexus-compiler-csharp`           | C#                                                                                      |

## Status

Maintained. `maven-compiler-plugin` depends on this, so public API is kept compatible.

## Using it

To write against the API:

```xml
<dependency>
  <groupId>org.codehaus.plexus</groupId>
  <artifactId>plexus-compiler-api</artifactId>
  <version>2.16.2</version>
</dependency>
```

Check the badge above for the current version.

To *use* a non-default compiler in a build, you normally do not depend on these directly — you set
`<compilerId>` on `maven-compiler-plugin` and add the matching implementation to that plugin's
dependencies. See the
[Maven documentation](https://maven.apache.org/plugins/maven-compiler-plugin/non-javac-compilers.html).

### Error Prone

See the [Error Prone installation guide](https://errorprone.info/docs/installation#maven), or the
[integration test](plexus-compiler-its/src/main/it/error-prone-compiler/pom.xml) in this repository for a
working example.

## Requirements

Java 8 or later.

## Documentation

- [Project site](https://codehaus-plexus.github.io/plexus-compiler/)
- [Javadoc](https://javadoc.io/doc/org.codehaus.plexus/plexus-compiler-api)
- [Release notes](https://github.com/codehaus-plexus/plexus-compiler/releases)

## Contributing

See [CONTRIBUTING.md](https://github.com/codehaus-plexus/.github/blob/master/CONTRIBUTING.md). In short:
`mvn verify` builds, and run `mvn spotless:apply` before pushing or CI will fail on formatting.

Please report security vulnerabilities privately — see
[SECURITY.md](https://github.com/codehaus-plexus/.github/blob/master/SECURITY.md), not a public issue.
