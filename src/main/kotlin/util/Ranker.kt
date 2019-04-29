package util

import org.apache.commons.lang3.StringUtils
import main.SpiderMain
import java.lang.StringBuilder

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

    fun rankDocs(queryTerms: List<List<String>>, spiderDB: RocksDB, wordDB: RocksDB): Map<String, Double> {
        val queryTermIds = findWordId(queryTerms, wordDB)
        val resultMap = mutableMapOf<String, Double>()
        queryTermIds.forEach { queryTermId ->
            if (queryTermId.size > 1) {  //Phrase detected
                val urls = urlLengthDB.getAllKeys()
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
                        val tfIdf = CSVParser.parseFrom(tfIdfDB[urlId] ?: "")
                        var score = 0.0
                        val size = queryTermId.size
                        var found = 0
                        //Get tfidf score for each word in phrase
                        for (i in 0 until tfIdf.size step 2) {
                            for (query in queryTermId) {
                                if (tfIdf[i] == query) {
                                    score += tfIdf[i + 1].toDouble()
                                    ++found
                                    break
                                }
                            }
                            if (found == size)
                                break
                        }

                        //Multiply score by number of times the phrase appear in doc
                        resultMap[urlId] = (resultMap[urlId] ?: 0.0) +
                                score * StringUtils.countMatches(wordsInDoc, queryStr)
                    }
                }
            } else {            //Single word
                queryTermId.forEach {
                    val spiderList = CSVParser.parseFrom(spiderDB[it]!!)
                    spiderList.parallelStream().forEach { docId ->
                        val tfIdf = CSVParser.parseFrom(tfIdfDB[docId] ?: "")
                        var score = 0.0
                        for (i in 0 until tfIdf.size step 2) {
                            if (tfIdf[i] == it) {
                                score = tfIdf[i + 1].toDouble()
                                break
                            }
                        }
                        resultMap[docId] = (resultMap[docId] ?: 0.0) + score
                    }
                }
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