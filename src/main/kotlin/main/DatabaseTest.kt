package main

import util.HTMLParser
import util.RocksDB
import java.io.PrintStream
import java.net.URL

class DatabaseTest {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val urlDB = RocksDB(SpiderMain.URL_DB_NAME)
            val spiderDB = RocksDB(SpiderMain.SPIDER_DB_NAME)
            val wordDB = RocksDB(SpiderMain.WORD_DB_NAME)
            val linkList = urlDB.getAllKeys()

            val printMap = mutableMapOf<String, String>().toSortedMap()
            linkList.parallelStream().forEach {
                val keywords = spiderDB.findFrequency(urlDB[it]!!.toInt(), wordDB)
                val title = HTMLParser.getTitle(it)
                val date = HTMLParser.getDate(it)
                val size = HTMLParser.getSize(it)
                val childLinks = HTMLParser.extractLink(it)
                printMap[it] = phaseOnePrint(title, it, date, size, keywords, childLinks)
            }

            val temp = System.out
            System.setOut(PrintStream("spider_result.txt"))
            printMap.forEach {
                print(it.value)
                if(it.key != printMap.lastKey())
                    println("-----------------------------------------------------------------------------------------")
            }
            System.setOut(temp)

            urlDB.close()
            spiderDB.close()
            wordDB.close()
        }

        private fun phaseOnePrint(title: String,
                                  url: String,
                                  date: String, size: Int,
                                  keywordCounts: Map<String, Int>,
                                  childLinks: List<URL>): String {
            val sb = StringBuilder()
            sb.append("$title\n$url\n$date, $size\n")
                .append(keywordCounts.toString()
                    .replace(",", ";")
                    .replace("=", " ")
                    .replace("{", "")
                    .replace("}", "")
                )
                .append('\n')
                .append(childLinks.toString()
                    .replace("[", "")
                    .replace("]", "")
                    .replace(", ", "\n")
                )
                .append('\n')
            return sb.toString()
        }
    }
}