package main

import util.HTMLParser
import util.RocksDB

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
        const val URL_DB_NAME  = "$BASE_URL/rockUrl"
        const val URL_INFO_DB_NAME = "$BASE_URL/rockUrlInfo"
        const val URL_CHILD_DB_NAME = "$BASE_URL/rockUrlChild"
        const val WORD_DB_NAME = "$BASE_URL/rockWord"
        const val SPIDER_DB_NAME = "$BASE_URL/rockSpider"

        private fun clearAllDB(vararg databases: RocksDB) {
            databases.forEach {
                it.removeAll()
            }
        }

        private fun closeAllDB(vararg databases: RocksDB) {
            databases.forEach {
                it.close()
            }
        }

        @JvmStatic
        fun main(args: Array<String>) {
            val rootLink = "https://www.cse.ust.hk/"
            val urlDB = RocksDB(URL_DB_NAME)
            val urlInfoDB = RocksDB(URL_INFO_DB_NAME)
            val urlChildDB = RocksDB(URL_CHILD_DB_NAME)
            val wordDB = RocksDB(WORD_DB_NAME)
            val spiderDB = RocksDB(SPIDER_DB_NAME)
            val databases = arrayOf(urlDB, urlInfoDB, urlChildDB, spiderDB, wordDB)
            clearAllDB(*databases)

            // Filter ensures only child links to be returned
            var linkList = HTMLParser.extractLink(rootLink, filter=rootLink)
            // PHASE 1 requires only 30 links
            linkList = linkList.subList(0, 30)

            val urlSet = mutableSetOf<String>()
            linkList.parallelStream().forEach {
                val link = it.toExternalForm()
                urlSet.add(link)
                val childLinks = HTMLParser.extractLink(link)
                childLinks.forEach { childUrl ->
                    val childLink = childUrl.toExternalForm()
                    urlSet.add(childLink)
                }
            }

            var counter = 0
            urlSet.toSortedSet().forEach {
                urlDB[it] = counter++
            }

            linkList.parallelStream().forEach {
                val link = it.toExternalForm()
                val childLinks = HTMLParser.extractLink(link, filter=null, self=false)
                val childLinkIdList = mutableListOf<Int>()
                childLinks.forEach { childUrl ->
                    childLinkIdList.add(urlDB[childUrl.toExternalForm()]!!.toInt())
                }
                urlChildDB[urlDB[link]!!.toInt()] = childLinkIdList
            }

            linkList.parallelStream().forEach {
                val link = it.toExternalForm()
                val title = HTMLParser.getTitle(link)
                val date = HTMLParser.getDate(link).toString()
                val size = HTMLParser.getSize(link).toString()
                urlInfoDB[urlDB[link]!!] = Triple(title, date, size)
            }

            val wordSet = mutableSetOf<String>()
            linkList.parallelStream().forEach {
                val wordList = HTMLParser.extractText(it.toExternalForm())
                for(word in wordList)
                    wordSet.add(word)
            }

            counter = 0
            wordSet.toSortedSet().forEach {
                wordDB[it] = counter++
            }

            linkList.parallelStream().forEach {
                val wordList = HTMLParser.extractText(it.toExternalForm())
                for(i in 0 until wordList.size) {
                    spiderDB[wordDB[wordList[i]]!!] = Pair(urlDB[it.toExternalForm()]!!.toInt(), i)
                }
            }

            closeAllDB(*databases)
        }
    }
}