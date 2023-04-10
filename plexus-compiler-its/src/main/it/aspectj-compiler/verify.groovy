/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
def logFile = new File( basedir, 'build.log' )
assert logFile.exists()
content = logFile.text.normalize()

assert content.contains( "Tests run: 2, Failures: 0, Errors: 0, Skipped: 0" )

def junitLog = """Running org.acme.ApplicationTest
call(String org.acme.Application.greet(String))
execution(String org.acme.Application.greet(String))
call(void org.junit.Assert.assertEquals(Object, Object))
call(void org.acme.Application.main(String[]))
execution(void org.acme.Application.main(String[]))
Running application
execution(String org.acme.Application.greet(String))
call(void org.junit.Assert.assertTrue(boolean))""".normalize()

assert content.contains( junitLog )
