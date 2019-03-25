package main

import util.HTMLParser
import util.RocksDB

class SpiderMain {
    companion object {
        const val URL_DB_NAME  = "rockUrl"
        const val WORD_DB_NAME = "rockWord"
        const val SPIDER_DB_NAME = "rockSpider"

        @JvmStatic
        fun main(args: Array<String>) {
            val rootLink = "http://www.cse.ust.hk/"
            val urlDB = RocksDB(URL_DB_NAME)
            val wordDB = RocksDB(WORD_DB_NAME)
            val spiderDB = RocksDB(SPIDER_DB_NAME)
            urlDB.removeAll()
            spiderDB.removeAll()
            wordDB.removeAll()

            var linkList = HTMLParser.extractLink(rootLink
                , rootLink.split("www.").last().substring(0, 10))
            linkList = linkList.subList(0, 30)

            var counter = 0
            for(linkUrl in linkList) {
                val link = linkUrl.toExternalForm()
                if(urlDB[link] == null) urlDB[link] = counter++
            }

            counter = 0
            linkList.parallelStream().forEach {
                val wordList = HTMLParser.extractText(it.toExternalForm())
                for(word in wordList) {
                    if (wordDB[word] == null) wordDB[word] = counter++
                }
                val urlID = urlDB[it.toExternalForm()]!!.toInt()
                for(i in 0 until wordList.size) {
                    spiderDB[wordDB[wordList[i]]!!, urlID] = i
                }
            }

            wordDB.close()
            urlDB.close()
            spiderDB.close()
        }
    }
}