## COMP4321 - Search Engine for Web data
### Project - Build a personal search engine

Spring 2019 - Group 25

AU, Ho Leong (hlauac@connect.ust.hk) 20260060

KIM, Hyun Gyu (hgkim@connect.ust.hk) 20375138

CHEUNG, Ka Ho (khcheungap@connect.ust.hk) 20465294

#### Phase 1 - Build Web Spider
| Submission files                      |
|---------------------------------------| 
| Database schema                       |
| Source code of spider and test program|
| readme.txt                            |
| Database file                         |
| spider_result.txt                     |

##### Project Setup
 | Setup Instructions |
 |--------------|
 | 1. Download and install IntelliJ IDEA from https://www.jetbrains.com/idea/ |
 | 2. Download and install Gradle Build Tool from https://gradle.org/ |
 | 3. Select open project: comp4321-Search-Engine-project as Gradle project |
 | 4. Build the project |

##### Run Spider

* Open and Run SpiderMain.kt in the projet

##### Phase 1 Test Program

* To retrieve the result from the .db, open and run DatabaseTest.kt in the project

##### Database Schema

* Please refer to readme.txt

#### Phase 2 - Build full Search Engine

##### web interface

* use spring boot for web interface because

* Kotlin works quite smoothly with Spring Boot and many of the steps found on the Spring Guides

* https://kotlinlang.org/docs/tutorials/spring-boot-restful.html kotlin guideline for spring boot

* https://spring.io/guides/gs/handling-form-submission/ spring boot handling submission tutorial (JAVA)

* src/main/kotlin/spring/web.kt and src/main/kotlin/resources/templates/result.html needed further editing

* starter program: src/main/kotlin/spring/application.kt

* default host: http://localhost:8080
