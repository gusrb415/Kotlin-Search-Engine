package main

import spring.Application
import util.CSVParser
import util.HTMLParser
import util.RocksDB
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
        const val URL_WORDS_DB_NAME = "$BASE_URL/rockUrlWords"
        const val PAGE_RANK_DB_NAME = "$BASE_URL/rockPageRank"
        const val URL_PARENT_DB_NAME = "$BASE_URL/rockUrlParent"
        const val URL_WORD_COUNT_DB_NAME = "$BASE_URL/rockUrlWordCount"
        const val TF_IDF_DB_NAME = "$BASE_URL/rockTfIdf"
        const val URL_LENGTH_DB_NAME = "$BASE_URL/rockUrlLength"
        const val REVERSE_URL_DB_NAME = "$BASE_URL/rockReverseUrl"
        const val REVERSE_WORD_DB_NAME = "$BASE_URL/rockReverseWord"

        private fun closeAllDB(vararg databases: RocksDB) {
            println("Closing all databases")
            databases.toList().forEach {
                it.close()
            }
        }

        private fun clearAllDB(vararg databases: RocksDB) {
            println("Clearing all databases")
            databases.toList().parallelStream().forEach {
                it.removeAll()
            }
        }

        private fun recursivelyCrawlLinks(
            url: URL, filter: String,
            urlSet: MutableSet<URL>, minSize: Int, visitedUrls: MutableSet<URL>? = null
        ) {
            val visited = visitedUrls ?: mutableSetOf()
            if (url in visited || urlSet.size > minSize)
                return
            val links = HTMLParser.extractLink(url.toExternalForm(), filter = filter)
            urlSet.addAll(links)
            visited.add(url)

            links.filter { it !in visited }.parallelStream().forEach {
                recursivelyCrawlLinks(it, filter, urlSet, minSize, visited)
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
            // urlID -> list(urlID)
            val urlParentDB = RocksDB(URL_PARENT_DB_NAME)
            // word -> wordID
            val wordDB = RocksDB(WORD_DB_NAME)
            // wordID -> list(urlID, position)
            val spiderDB = RocksDB(SPIDER_DB_NAME)
            // urlID -> list(wordID)
            val urlWordsDB = RocksDB(URL_WORDS_DB_NAME)
            // urlID -> PageRank
            val pageRankDB = RocksDB(PAGE_RANK_DB_NAME)
            // urlID -> list(wordID, count)
            val urlWordCountDB = RocksDB(URL_WORD_COUNT_DB_NAME)
            // urlID -> list(wordID, tfidf)
            // TFIDF DB NOT USED HERE
            //  val urlLengthDB = RocksDB(URL_LENGTH_DB_NAME)
            val reverseUrlDB = RocksDB(REVERSE_URL_DB_NAME)
            val reverseWordDB = RocksDB(REVERSE_WORD_DB_NAME)
            val databases = arrayOf(
                urlDB, urlInfoDB, urlChildDB, urlParentDB, reverseUrlDB, reverseWordDB,
                spiderDB, wordDB, urlWordsDB, pageRankDB, urlWordCountDB
            )
            clearAllDB(*databases)

            // Retrieve all links under the root link recursively
            // Filter ensures only child links to be returned
            println("Recursively crawling URLs from $rootLink")
            val urlSet = mutableSetOf<URL>()
            val minSize = if (args.isNotEmpty()) args[0].toInt() else 100000
            recursivelyCrawlLinks(URL(rootLink), filter = rootLink, urlSet = urlSet, minSize = minSize)
            println("Total ${urlSet.size} websites found")
            println("Time Elapsed: ${(System.currentTimeMillis() - startTime).toDouble() / 1000} seconds")

            var counter = -1
            urlSet.toSortedSet(compareBy { it.toExternalForm() }).map { it.toExternalForm() }.forEach {
                urlDB[it] = ++counter
                reverseUrlDB[counter.toString()] = it
            }

            println("Started getting child urls")
            val cseLinks = urlSet.map { it.toExternalForm() }.toList()
            cseLinks.parallelStream().forEach {
                val childLinks = HTMLParser.extractLink(it, filter = rootLink, self = false)
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

            println("Started calculating page rank and writing parent url DB")
            val linkMatrix = getMatrix(urlChildDB)
            val keys = urlChildDB.getAllKeys().map { it.toInt() }.sorted().map { it.toString() }
            writeUrlParentDB(keys, linkMatrix, urlParentDB)
            val ranks = getPageRank(keys, linkMatrix)
            ranks.forEach { pageRankDB[it.key] = it.value }
            println("Time Elapsed: ${(System.currentTimeMillis() - startTime).toDouble() / 1000} seconds")

            println("Started crawling information")
            cseLinks.parallelStream().forEach { link ->
                val title = HTMLParser.getTitle(link)
                val date = HTMLParser.getDate(link).toString()
                val size = HTMLParser.getSize(link).toString()
                urlInfoDB[urlDB[link]!!] = Triple(title, date, size)
//                urlInfoDB[urlDB[link]!!] = HTMLParser.getAllInfo(link)
            }
            println("Time Elapsed: ${(System.currentTimeMillis() - startTime).toDouble() / 1000} seconds")

            println("Started Writing word ID and position to database")
            counter = 0
            var wordCounter = -1
            val wordToDocIdMap = mutableMapOf<String, MutableList<String>>()

            cseLinks.forEach { link ->
                val id = urlDB[link]!!
                val wordList = HTMLParser.extractText(link)

                val wordsToIds = wordList.map {
                    wordDB[it] ?: {
                        wordDB[it] = ++wordCounter
                        reverseWordDB[wordCounter.toString()] = it
                        wordCounter.toString()
                    }.invoke()
                }
                urlWordsDB[id] = wordsToIds
                val wordCountMap = mutableMapOf<String, Int>()
                wordsToIds.forEach { wordId ->
                    wordCountMap[wordId] = (wordCountMap[wordId] ?: 0) + 1
                    wordToDocIdMap[wordId]?.add(id) ?: {
                        wordToDocIdMap[wordId] = mutableListOf(id)
                    }.invoke()
                }
                val countList = mutableListOf<Pair<String, Int>>()
                wordCountMap.forEach { t, u ->
                    countList.add(Pair(t, u))
                }
                urlWordCountDB[id] = countList
                    .sortedByDescending { it.second }
                    .map { listOf(it.first, it.second.toString()) }
                    .flatten()
                if (++counter % 100 == 0) {
                    System.gc()
                    println("Currently wrote $counter websites to DB")
                    println("Time Elapsed: ${(System.currentTimeMillis() - startTime).toDouble() / 1000} seconds")
                }
            }
            wordToDocIdMap.forEach { wordId, docIdList ->
                spiderDB[wordId] = docIdList
            }
            println("Total ${wordToDocIdMap.size} unique words found")
            closeAllDB(*databases)

            TfIdfMain.main(args)
            println("Time Elapsed: ${(System.currentTimeMillis() - startTime).toDouble() / 1000} seconds")

            Application.main(args)
        }

        private fun writeUrlParentDB(keys: List<String>, Linkmatrix: List<List<Double>>, urlParentDB: RocksDB) {
            val linksList = mutableListOf<String>()
            for (i in 0 until keys.size) {
                linksList.clear()
                for (j in 0 until Linkmatrix.size)
                    if (Linkmatrix[j][i] > 0.0) linksList.add(keys[j])
                urlParentDB[keys[i]] = linksList
            }
        }

        private fun getMatrix(urlChildDB: RocksDB): List<List<Double>> {
            val keys = urlChildDB.getAllKeys().map { it.toInt() }.sorted()
            val size = keys.size
            val finalMatrix = mutableListOf<MutableList<Double>>()
            keys.forEach {
                val linkList =
                    CSVParser.parseFrom(urlChildDB[it.toString()]!!)
                        .filter { link -> link != "" }
                        .map { link -> link.toInt() }
                        .filter { link -> link < size }
                val row = MutableList(size) { 0.0 }
                var sum = 0
                for (each in linkList) {
                    row[each] += 1.0
                    sum += 1
                }
                for (each in linkList) {
                    row[each] /= sum.toDouble()
                }
                finalMatrix.add(row)
            }
            return finalMatrix
        }

        private fun getPageRank(keys: List<String>, linkMatrix: List<List<Double>>): Map<String, Double> {
            val size = keys.size
            val finalMatrix = transpose(linkMatrix)
            var rank = MutableList(size) { 1.0 }
            for (i in 0 until 40) {
                // PR Formula = 0.15 + 0.85(Sum of incoming weight)
                rank = addition(0.15, multiply(0.85, dot(finalMatrix, rank)))
            }

            val map = mutableMapOf<String, Double>()
            for (i in 0 until keys.size) {
                map[keys[i]] = rank[i]
            }
            return map
        }

        private fun addition(index: Double, secondMatrix: MutableList<Double>): MutableList<Double> {
            for (i in 0 until secondMatrix.size)
                secondMatrix[i] += index
            return secondMatrix
        }

        private fun multiply(index: Double, secondMatrix: MutableList<Double>): MutableList<Double> {
            for (i in 0 until secondMatrix.size)
                secondMatrix[i] *= index
            return secondMatrix
        }

        private fun dot(firstMatrix: List<List<Double>>, secondMatrix: List<Double>): MutableList<Double> {
            val result = mutableListOf<Double>()
            firstMatrix.forEach {
                var sum = 0.0
                for (i in 0 until it.size) {
                    sum += it[i] * secondMatrix[i]
                }
                result.add(sum)
            }
            return result
        }

        private fun <E> transpose(matrix: List<List<E>>): MutableList<MutableList<E>> {
            val newMatrix = mutableListOf<MutableList<E>>()
            for (i in 0 until matrix.size) {
                newMatrix.add(mutableListOf())
                for (j in 0 until matrix[i].size) {
                    newMatrix[i].add(matrix[j][i])
                }
            }
            return newMatrix
        }
    }
}