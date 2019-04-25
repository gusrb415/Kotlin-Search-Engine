package main

import util.HTMLParser
import util.RocksDB
import java.lang.NullPointerException
import java.net.URL

class SpiderMain {
    companion object {
        /**
         * URL_DB_NAME - Web url: String to Web id: Int database
         * URL_INFO_DB_NAME - Web id: Int to Triple(Title: String, Date-Modified: Long, Size: Int)
         * URL_CHILD_DB_NAME - Web id: Int to List(Child Web id: Int)
         * WORD_DB_NAME - Word: String to Word id: Int database
         * SPIDER_DB_NAME - Word id: Int to List(Web id: Int, Word Location: Int)) database
         */
        private const val BASE_URL = "RocksDB"
        const val URL_DB_NAME = "$BASE_URL/rockUrl"
        const val URL_INFO_DB_NAME = "$BASE_URL/rockUrlInfo"
        const val URL_CHILD_DB_NAME = "$BASE_URL/rockUrlChild"
        const val WORD_DB_NAME = "$BASE_URL/rockWord"
        const val SPIDER_DB_NAME = "$BASE_URL/rockSpider"

        private fun clearAllDB(vararg databases: RocksDB) {
            println("Clearing all databases")
            databases.forEach {
                it.removeAll()
            }
        }

        private fun closeAllDB(vararg databases: RocksDB) {
            println("Closing all databases")
            databases.forEach {
                it.close()
            }
        }

        private fun recursivelyCrawlLinks(
            url: URL,
            filter: String,
            urlSet: MutableSet<URL>,
            visitedUrls: MutableSet<URL>? = null
        ) {
            val visited = visitedUrls ?: mutableSetOf()
            if (url in visited || urlSet.size > 200)
                return

            val links = HTMLParser.extractLink(url.toExternalForm(), filter = filter)
            urlSet.addAll(links)
            visited.add(url)

            links.filter { it !in visited }.forEach {
                recursivelyCrawlLinks(it, filter, urlSet, visited)
            }
        }

        @JvmStatic
        fun main(args: Array<String>) {
            val startTime = System.currentTimeMillis()
            val rootLink = "https://www.cse.ust.hk/"
            // url -> urlID
            val urlDB = RocksDB(URL_DB_NAME)
            // urlID -> (title, date, size)
            val urlInfoDB = RocksDB(URL_INFO_DB_NAME)
            // urlID -> list(urlID)
            val urlChildDB = RocksDB(URL_CHILD_DB_NAME)
            // word -> wordID
            val wordDB = RocksDB(WORD_DB_NAME)
            // wordID -> list(urlID, position)
            val spiderDB = RocksDB(SPIDER_DB_NAME)
            val databases = arrayOf(urlDB, urlInfoDB, urlChildDB, spiderDB, wordDB)
            clearAllDB(*databases)

            // Retrieve all links under the root link recursively
            // Filter ensures only child links to be returned
            println("Recursively crawling URLs from $rootLink")
            val urlSet = mutableSetOf<URL>()
            recursivelyCrawlLinks(URL(rootLink), filter = rootLink, urlSet = urlSet)
            println("Total ${urlSet.size} websites found")
            println("Time Elapsed: ${(System.currentTimeMillis() - startTime).toDouble() / 1000} seconds")

            var counter = 0
            urlSet.toSortedSet(compareBy { it.toExternalForm() }).forEach {
                urlDB[it.toExternalForm()] = counter++
            }

            val cseLinks = urlDB.getAllKeys().filter { it.contains(rootLink) }
            val nonCseLinks = mutableSetOf<String>()
            println("Started getting child urls")
            cseLinks.parallelStream().forEach { url ->
                val childLinks = HTMLParser.extractLink(url, filter = null, self = false)
                nonCseLinks.addAll(childLinks.map { it.toExternalForm() }.filter { !it.contains(rootLink) })
            }
            println("Time Elapsed: ${(System.currentTimeMillis() - startTime).toDouble() / 1000} seconds")

            println("Started indexing and writing child urls")
            nonCseLinks.toSortedSet().forEach {
                urlDB[it] = counter++
            }

            cseLinks.parallelStream().forEach {
                val childLinks = HTMLParser.extractLink(it, filter = null, self = false)
                val childLinkIdList = mutableListOf<Int>()
                childLinks.forEach { childUrl ->
                    try {
                        childLinkIdList.add(urlDB[childUrl.toExternalForm()]!!.toInt())
                    } catch (ignored: Exception) {
                    }
                }
                urlChildDB[urlDB[it]!!.toInt()] = childLinkIdList
            }
            println("Time Elapsed: ${(System.currentTimeMillis() - startTime).toDouble() / 1000} seconds")

            println("Started crawling information")
            cseLinks.parallelStream().forEach { link ->
                val title = HTMLParser.getTitle(link)
                val date = HTMLParser.getDate(link).toString()
                val size = HTMLParser.getSize(link).toString()
                urlInfoDB[urlDB[link]!!] = Triple(title, date, size)
            }
            println("Time Elapsed: ${(System.currentTimeMillis() - startTime).toDouble() / 1000} seconds")

            val wordSet = mutableSetOf<String>()
            val wordList = mutableMapOf<Int, List<String>>()
            println("Started crawling words from websites")
            cseLinks.parallelStream().forEach { link ->
                wordList[urlDB[link]!!.toInt()] = HTMLParser.extractText(link)
            }

            wordList.values.forEach {
                wordSet.addAll(it)
            }
            println("Time Elapsed: ${(System.currentTimeMillis() - startTime).toDouble() / 1000} seconds")

            println("Started Indexing words")
            counter = 0
            wordSet.toSortedSet().forEach {
                wordDB[it] = counter++
            }
            println("Total number of ${counter - 1} unique words")
            println("Time Elapsed: ${(System.currentTimeMillis() - startTime).toDouble() / 1000} seconds")

            println("Started Writing word ID and position to database")
            counter = 0
            wordList.keys.parallelStream().forEach { id ->
                val words = wordList[id]!!
                var i = 0
                words.forEach { word ->
                    try {
                        spiderDB[wordDB[word]!!] = Pair(id, i++)
                    } catch (e: NullPointerException) {
                        println("\"$word\" failed being indexed")
                    }
                }
                if (++counter % 10 == 0) {
                    println("Currently wrote $counter websites to DB")
                    println("Time Elapsed: ${(System.currentTimeMillis() - startTime).toDouble() / 1000} seconds")
                }
            }
            println("Time Elapsed: ${(System.currentTimeMillis() - startTime).toDouble() / 1000} seconds")

            closeAllDB(*databases)
        }
    }
}