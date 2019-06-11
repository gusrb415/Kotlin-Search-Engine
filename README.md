### Project - Build a personal search engine

#### Project Setup
 | Setup Instructions |
 |--------------|
 | 1. Download and install IntelliJ IDEA from https://www.jetbrains.com/idea/ |
 | 2. Download and install Gradle Build Tool from https://gradle.org/ |
 | 3. Select open project: Kotlin-Search-Engine as Gradle project |
 | 4. Build the project |

#### Run Spider
Compile and Run SpiderMain.kt in the project (Requires Kotlin Compiler downloadable using Gradle)
Folder RocksDB is created with several databases.

#### Run Apache Tomcat Server
Compile and Run Application.kt in the project (Requires Kotlin Compiler downloadable using Gradle)
Folder RocksDB is created with several databases.

#### Alternative Method
Download pre-built jar from http://bit.ly/2WcaRsp
run java -jar path/to/jar <args>
If you want to run from crawler to server, pass “spider <max number of websites to fetch> server” as
arguments
If you want to run only the server, pass “server” as arguments

#### Programming Language Used
Kotlin - a cross-platform, statically typed, general-purpose programming language with type inference.
It is designed for full interoperation with Java, and the JVM version of its standard library
depends on the Java Class Library, but type inference allows its syntax to be more concise.
We used Kotlin since it is a JVM language which is highly readable.
