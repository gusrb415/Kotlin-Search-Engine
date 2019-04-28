package main

import util.CSVParser
import util.RocksDB
import java.lang.Exception

class TfIdfMain {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val spiderDB = RocksDB(SpiderMain.SPIDER_DB_NAME)
            val urlWordCountDB = RocksDB(SpiderMain.URL_WORD_COUNT_DB_NAME)
            val tfIdfDB = RocksDB(SpiderMain.TF_IDF_DB_NAME)
            tfIdfDB.removeAll()
            val urls = urlWordCountDB.getAllKeys()
            val urlSize = urls.size
            urls.forEach {
                val wordCount = CSVParser.parseFrom(urlWordCountDB[it]!!)
                val tfIdfList = mutableListOf<String>()
                if(wordCount.size < 2) return@forEach
                for(i in 0 until wordCount.size step 2) {
                    val word = wordCount[i]
                    val count = wordCount[i + 1]

                    val docToCount = mutableMapOf<String, Int>()
                    val spiderList = spiderDB[word]!!.split("d").drop(0).map { tup -> tup.split(" ") }
                    for (j in 1 until spiderList.size)
                        docToCount[spiderList[j][0]] = (docToCount[spiderList[j][0]] ?: 0) + 1
                    val tfIdf = count.toDouble() * Math.log(urlSize.toDouble() / docToCount.size) / Math.log(2.0)
                    tfIdfList.add(word)
                    tfIdfList.add("%.6f".format(tfIdf))
                }
                tfIdfDB[it] = tfIdfList
            }

            spiderDB.close()
            urlWordCountDB.close()
            tfIdfDB.close()
        }
    }
}