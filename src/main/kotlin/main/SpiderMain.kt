package main

import util.HTMLParser
import util.RocksDB
import java.io.PrintStream
import java.net.URL

class SpiderMain {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val rootLink = "http://www.cse.ust.hk/"
            var linkList = HTMLParser.extractLink(rootLink)
            val urlDB = RocksDB("rockUrls")
            val spiderDB = RocksDB("rockTest")
            var counter = 0
            for(linkUrl in linkList) {
                val link = linkUrl.toExternalForm()
                if(urlDB[link] == null) urlDB[link] = counter++
            }

            spiderDB.removeAll()

            linkList = linkList.subList(0, 30)
            linkList.parallelStream().forEach {
                val wordList = HTMLParser.extractText(it.toExternalForm())
                val index = urlDB[it.toExternalForm()]!!.toInt()
                for(i in 0 until wordList.size) {
                    spiderDB[wordList[i], index] = i
                }
            }

            val temp = System.out
            System.setOut(PrintStream("spider_result.txt"))
            linkList.forEach {
                val keywords = spiderDB.findFrequency(urlDB[it.toExternalForm()]!!.toInt())
                val link = it.toExternalForm()!!
                val title = HTMLParser.getTitle(link)
                val date = HTMLParser.getDate(link)
                val size = HTMLParser.getSize(link)
                phaseOnePrint(title, link, date, size, keywords, HTMLParser.extractLink(link))
                if(it != linkList.last())
                    println("-----------------------------------------------------------------------------------------")
            }
            System.setOut(temp)

            urlDB.close()
            spiderDB.close()
        }

        private fun phaseOnePrint(title: String,
                                  url: String,
                                  date: String, size: Int,
                                  keywordCounts: Map<String, Int>,
                                  childLinks: List<URL>) {
            println("$title,\n$url,\n$date, $size\n${keywordCounts.entries},\n$childLinks")
        }
    }
}