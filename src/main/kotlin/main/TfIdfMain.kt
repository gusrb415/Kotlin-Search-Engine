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
            val urlLengthDB = RocksDB(SpiderMain.URL_LENGTH_DB_NAME)

            urlLengthDB.removeAll()
            tfIdfDB.removeAll()
            println("Calculating tfIdf for all docs and terms")
            val urls = urlWordCountDB.getAllKeys()
            val urlSize = urls.size
            urls.forEach {urlId ->
                val wordCount = CSVParser.parseFrom(urlWordCountDB[urlId]!!)
                val tfIdfList = mutableListOf<String>()
                if(wordCount.size < 2) return@forEach
                for(i in 0 until wordCount.size step 2) {
                    val word = wordCount[i]
                    val count = wordCount[i + 1]

                    val docToCount = mutableMapOf<String, Int>()
                    val spiderList = CSVParser.parseFrom(spiderDB[word]!!)
                    for (j in 1 until spiderList.size)
                        docToCount[spiderList[j]] = (docToCount[spiderList[j]] ?: 0) + 1
                    val tfIdf = count.toDouble() * Math.log(urlSize.toDouble() / docToCount.size) / Math.log(2.0)
                    tfIdfList.add(word)
                    tfIdfList.add("%.6f".format(tfIdf))
                }
                urlLengthDB[urlId] = Math.sqrt(tfIdfList.map { it.toDouble() * it.toDouble() }.sum())
                tfIdfDB[urlId] = tfIdfList
            }

            urlLengthDB.close()
            spiderDB.close()
            urlWordCountDB.close()
            tfIdfDB.close()
        }
    }
}