# Agent guide for plexus-compiler

`org.codehaus.plexus:plexus-compiler` (`pom` packaging, inheriting the `plexus` parent) is a uniform
API over several Java compilers that `maven-compiler-plugin` selects with its `<compilerId>`. The
reactor contains `plexus-compiler-api`, `plexus-compiler-manager`, `plexus-compiler-test`,
`plexus-compilers` (the implementations) and `plexus-compiler-its`.

## Build and test

- Building the reactor needs JDK 17 or newer (an enforcer rule in the root POM). Main code targets
  Java 8 (`javaVersion` 8 from the parent), except the modules that set their own `javaVersion`: the
  Eclipse and AspectJ compilers use 17, and the Error Prone compiler and `plexus-compiler-its` use 21.
  Keep the requirements in `README.md` and the module site pages in step with those values.
- `mvn verify` builds the reactor. CI calls the shared `codehaus-plexus/.github` build workflow with
  `verify javadoc:javadoc`, and `maven.yml` here excludes JDK 8 and 11 from its matrix and adds
  several JDK distributions.
- Run `mvn spotless:apply` on JDK 17 to 21 before pushing, or the formatting check fails.
- `plexus-compiler-its` runs the projects in `src/main/it` through `maven-invoker-plugin` (`install`,
  `run` and `verify` goals, a `verify.groovy` post-build hook and its own settings file); each
  project runs `clean test-compile`. The projects are test fixtures: keep them as they are unless
  you are changing what they cover.
- The parent POM pins the AspectJ, Error Prone and Eclipse Sisu versions used by the compiler
  implementations in properties; change them there.

## Documentation updates

A change that users or contributors can notice ships with the documentation it affects, in the
same pull request. A refactor with no visible effect needs none.

- A compiler implementation added, removed or given a new JDK or Maven requirement: its
  `src/site/markdown/index.md`, the module list in the root `src/site/markdown/index.md`, and the
  Modules and Requirements sections of `README.md`.
- API changes in `plexus-compiler-api` or `plexus-compiler-manager`: the Javadoc, which the site
  publishes. A `CompilerConfiguration` option, or a change in how a compiler maps it to command-line
  arguments, is described on the option itself and on that compiler's module page.
- A page added or removed: the `site.xml` of that module.
- How to build, test or release: `README.md`.
- A new convention, build gate or IT fixture: this file.

