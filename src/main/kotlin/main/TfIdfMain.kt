package main

import util.CSVParser
import util.RocksDB

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
            val logTwo = Math.log(2.0)
            val urlToLengthMap = mutableMapOf<String, Double>()
            urls.parallelStream().forEach { urlId ->
                val wordCount = CSVParser.parseFrom(urlWordCountDB[urlId]!!)
                val tfIdfList = mutableListOf<Pair<String, Double>>()
                if (wordCount.size < 2) return@forEach
                for (i in 0 until wordCount.size step 2) {
                    val word = wordCount[i]
                    val count = wordCount[i + 1]

                    val docToCount = mutableMapOf<String, Int>()
                    val spiderList = CSVParser.parseFrom(spiderDB[word]!!)
                    for (docId in spiderList)
                        docToCount[docId] = (docToCount[docId] ?: 0) + 1
                    val tfIdf = count.toDouble() * Math.log(urlSize.toDouble() / docToCount.size) / logTwo
                    tfIdfList.add(Pair(word, tfIdf))
                }
                urlToLengthMap[urlId] = tfIdfList.map { it.second * it.second }.sum()
                tfIdfDB[urlId] = tfIdfList.flatMap { listOf(it.first, "%.6f".format(it.second)) }
            }
            urlToLengthMap.forEach { urlId, length ->
                urlLengthDB[urlId] = Math.sqrt(length)
            }

            urlLengthDB.close()
            spiderDB.close()
            urlWordCountDB.close()
            tfIdfDB.close()
        }
    }
}