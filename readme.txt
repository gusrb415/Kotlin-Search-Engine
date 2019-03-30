Project Setup

Install IntelliJ IDEA in https://www.jetbrains.com/idea/
Install Gradle Build Tool in https://gradle.org/
(Or use local gradle distribution from Intellij)
Select import project using Gradle: Comp4321-Search-Engine-project
Build Project Using Gradle

Run Spider
Compile and Run SpiderMain.kt in the project (Requires Kotlin Compiler downloadable using Gradle)
Folder RocksDB is created with several databases.

Phase 1 Test Program
To retrieve the result, open and run DatabaseTest.kt in the project.
The program will read data from the RocksDB and outputs a plain-text file named spider_result.txt.

Database Schema
Please refer to Database_Schema.docx

Programming Language Used
Kotlin - a cross-platform, statically typed, general-purpose programming language with type inference.
It is designed for full interoperation with Java, and the JVM version of its standard library
depends on the Java Class Library, but type inference allows its syntax to be more concise.
We used Kotlin since it is a JVM language which is highly readable.
