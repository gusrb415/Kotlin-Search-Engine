package util

import org.apache.commons.lang3.StringUtils
import main.SpiderMain
import java.lang.StringBuilder
import java.util.*

/**
 *  Weight of query term can be determined by tf * idf
 *
 *  Given:
 *  A query (q) with k query terms  (Let qk denote the weight of term k in the query)
 *  Doc i (di) with k terms         (Let dik denote the frequency of term k in doc i)
 *
 *  Variables needed:
 *  Frequency of query term in the doc  (dik)
 *  Weight of query term                (qk)
 *  Formula:
 *  Sum of all query terms (dik * qk) / sqrt ( (Sum of all query terms in doc (dik))^2 * (Sum of all query terms (qk))^2 )
 */
object Ranker {
    private val tfIdfDB = RocksDB(SpiderMain.TF_IDF_DB_NAME)
    private val urlLengthDB = RocksDB(SpiderMain.URL_LENGTH_DB_NAME)
    private val urlWordsDB = RocksDB(SpiderMain.URL_WORDS_DB_NAME)
    private val spiderDB = RocksDB(SpiderMain.SPIDER_DB_NAME)
    private val wordDB = RocksDB(SpiderMain.WORD_DB_NAME)
    private val urls = urlLengthDB.getAllKeys()
    private val tfIdfMap = mutableMapOf<String, SortedMap<String, Double>>()

    init {
        val keys = tfIdfDB.getAllKeys()
        keys.forEach {
            val tfIdfList = CSVParser.parseFrom(tfIdfDB[it] ?: "")
            val sortedMap = sortedMapOf<String, Double>()
            for (i in 0 until tfIdfList.size step 2) {
                sortedMap[tfIdfList[i]] = tfIdfList[i + 1].toDouble()
            }
            tfIdfMap[it] = sortedMap
        }
    }

    fun initialize() {
        println("Total ${urls.size} URLs found in Database")
    }

    fun rankDocs(queryTerms: List<List<String>>): MutableMap<String, Double> {
        val queryTermIds = findWordId(queryTerms, wordDB)
        val resultMap = mutableMapOf<String, Double>()
        queryTermIds.forEach { queryTermId ->
            if (queryTermId.size > 1) {  //Phrase detected
                urls.forEach { urlId ->
                    //Phrase is in document
                    val sb = StringBuilder()
                    for (i in 0 until queryTermId.size) {
                        sb.append("\"${queryTermId[i]}\"")
                        if (i != queryTermId.size - 1) {
                            sb.append(",")
                        }
                    }

                    val queryStr = sb.toString()
                    val wordsInDoc = urlWordsDB[urlId]!!
                    if (wordsInDoc.contains(queryStr)) {
                        //tfIdf: wordId, score, wordId, score...
                        var score = 0.0

                        //Get tfidf score for each word in phrase
                        for (term in queryTermId) {
                            score += tfIdfMap[urlId]!![term]!!
                        }

                        //Multiply score by number of times the phrase appear in doc
                        resultMap[urlId] = (resultMap[urlId] ?: 0.0) +
                                score * StringUtils.countMatches(wordsInDoc, queryStr)
                    }
                }
            } else {            //Single word
                val startTime = System.currentTimeMillis()
                val spiderList = CSVParser.parseFrom(spiderDB[queryTermId[0]]!!)
                println("Term ${queryTermId[0]} parsing took with ${spiderList.size} urls found ${(System.currentTimeMillis() - startTime) / 1000.0} seconds")
                spiderList.parallelStream().forEach { docId ->
                    resultMap[docId] = (resultMap[docId] ?: 0.0) + tfIdfMap[docId]!![queryTermId[0]]!!
                }
                println("Term ${queryTermId[0]} took ${(System.currentTimeMillis() - startTime) / 1000.0} seconds")
            }
        }

        val queryLength = Math.sqrt(queryTermIds.flatten().size.toDouble())
        resultMap.forEach { key, value ->
            resultMap[key] = value / (urlLengthDB[key]!!.toDouble() * queryLength)
        }

        return resultMap
    }

    private fun findWordId(inputWordsList: List<List<String>>, wordDB: RocksDB): List<List<String>> {
        val result = mutableListOf<List<String>>()

        for (inputWords in inputWordsList) {
            val phraseList = mutableListOf<String>()
            for (inputWord in inputWords) {
                val wordId = wordDB[inputWord]
                if (wordId != null) phraseList.add(wordId)
            }
            result.add(phraseList)
        }

        return result
    }
}