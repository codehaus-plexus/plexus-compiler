J2ObjC Plexus compiler
===================

 The J2ObjC Plexus compiler is a tool to compile java source files to objective-c by calling the j2objc tool.
 
 	```
	   <build>
	    <plugins>
	      <plugin>
	        <artifactId>maven-compiler-plugin</artifactId>
	        <version>3.3</version>
	        <configuration>
	          <fork>true</fork>	          
   		      <compilerId>j2objc</compilerId>  
		      <executable>${J2OBJC_DISTRIBUTION}/j2objc</executable>
              <compilerArguments>
			    <use-arc/>
                <x>objective-c</x>
                <sourcepath>src/main/java</sourcepath>
              </compilerArguments>
            </configuration>
            <dependencies>
              <dependency>
                <groupId>org.codehaus.plexus</groupId>
                <artifactId>plexus-compiler-j2objc</artifactId>
                <version>2.6-SNAPSHOT</version>
              </dependency>          
            </dependencies>
          </plugin>
          ...
 	```
  