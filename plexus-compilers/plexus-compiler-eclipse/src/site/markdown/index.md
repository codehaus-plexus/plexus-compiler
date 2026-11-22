Plexus Eclipse Compiler
-----------------------

Eclipse Compiler support for Plexus Compiler component.

**Requires** `JDK 17+` and `Maven 3.9.6+`

The configured source locations are passed to the compiler as `-sourcepath`, also when the source files
to compile are listed explicitly, so that sibling sources and annotation processors can find the other
sources in the source roots. The `proc` setting is passed as `-proc:<value>` even when the annotation
processors are discovered from the class path.
