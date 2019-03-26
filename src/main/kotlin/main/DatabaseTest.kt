package main

import util.CSVParser
import util.RocksDB
import java.io.PrintStream
import java.text.SimpleDateFormat
import java.util.*

class DatabaseTest {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val urlDB = RocksDB(SpiderMain.URL_DB_NAME)
            val urlInfoDB = RocksDB(SpiderMain.URL_INFO_DB_NAME)
            val urlChildDB = RocksDB(SpiderMain.URL_CHILD_DB_NAME)
            val spiderDB = RocksDB(SpiderMain.SPIDER_DB_NAME)
            val wordDB = RocksDB(SpiderMain.WORD_DB_NAME)
            val linkList = urlInfoDB.getAllKeys()

            val printMap = mutableMapOf<String, String>().toSortedMap()
            linkList.parallelStream().forEach {
                val keywords = spiderDB.findFrequency(it.toInt(), wordDB)
                val infoList = CSVParser.parseFrom(urlInfoDB[it]!!)
                val title = infoList[0]
                val date = infoList[1].toLong()
                val size = infoList[2].toInt()
                val childLinkIds = CSVParser.parseFrom(urlChildDB[it]!!)
                val childLinkStrings = mutableListOf<String>()
                for (id in childLinkIds) {
                    if(id.isEmpty())
                        continue
                    childLinkStrings.add(urlDB.getKey(id)!!)
                }
                printMap[it] = phaseOnePrint(title, urlDB.getKey(it)!!, date, size, keywords, childLinkStrings)
            }

            val temp = System.out
            System.setOut(PrintStream("spider_result.txt"))
            printMap.forEach {
                print(it.value)
                if(it.key != printMap.lastKey())
                    println("-----------------------------------------------------------------------------------------")
            }
            System.setOut(temp)

            urlInfoDB.close()
            urlDB.close()
            urlChildDB.close()
            wordDB.close()
            spiderDB.close()
        }

        private fun buildDateFromLong(longNumber: Long): String {
            return SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date(longNumber))
        }

        private fun phaseOnePrint(title: String,
                                  url: String,
                                  date: Long, size: Int,
                                  keywordCounts: Map<String, Int>,
                                  childLinks: List<String>): String {
            val sb = StringBuilder()
            sb.append("$title\n$url\n${buildDateFromLong(date)}, $size\n")
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
                .append(if(childLinks.isNotEmpty()) '\n' else "")
            return sb.toString()
        }
    }
}