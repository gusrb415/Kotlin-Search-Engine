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

    private fun countPhrase(termIds: String, urlId: String) : Int {

        return StringUtils.countMatches(urlWordsDB[urlId].toString(), termIds)
    }

    fun rankDocs(queryTerms: List<List<String>>, spiderDB: RocksDB, wordDB: RocksDB): Map<String, Double> {
        System.out.println(queryTerms)
        val queryTermIds = findWordId(queryTerms, wordDB)
        System.out.println(queryTermIds.toString())

        val resultMap = mutableMapOf<String, Double>()

        queryTermIds.forEach {queryTermIds ->

            if (queryTermIds.size > 1){  //Phrase detected
                System.out.println("Phrase Mode")
                val urls = urlLengthDB.getAllKeys()
                urls.forEach {urlId ->
                    var score = 0.0
                    //Phrase is in document

                    val sb = StringBuilder()
                    for(i in 0 until queryTermIds.size) {
                        sb.append("\"${queryTermIds[i]}\"")
                        if(i != queryTermIds.size - 1) {
                            sb.append(",")
                        }
                    }

//                    print(urlId)
//                    print(": ")
//                    println(urlWordsDB[urlId])
//                    println(sb.toString())

                    if (urlWordsDB[urlId]!!.contains(sb.toString())){

                        //tfIdf: wordId, score, wordId, score...
                        val tfIdf = CSVParser.parseFrom(tfIdfDB[urlId] ?: "")

                        //Get tfidf score for each word in phrase
                        for (i in 0 until tfIdf.size step 2) {
                            if (tfIdf[i] == queryTermIds[0]){ //Score of first wordId found
                                score += tfIdf[i+1].toDouble()

                                //Get the tfidf score of the remaining phrase
                                var counter = 2
                                for (j in i+1 until queryTermIds.size step 2) {
                                    if (counter == queryTermIds.size)
                                        break
                                    score += tfIdf[j].toDouble()
                                    counter++
                                }
                            }
                        }
                    }

//                    print(urlId)
//                    print(": ")
//                    print(score)
//                    print(", ")
//                    println(countPhrase(sb.toString(), urlId))

                    //Multiply score by number of times the phrase appear in doc
                    resultMap[urlId] = (resultMap[urlId] ?: 0.0) + score * countPhrase(sb.toString(), urlId)


                }

            } else {            //Single word
                System.out.println("Word Mode")
                queryTermIds.forEach {
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

        resultMap.forEach { key, value ->
            resultMap[key] = value / (urlLengthDB[key]!!.toDouble() * Math.sqrt(queryTermIds.size.toDouble()))
            if (resultMap[key] != 0.0) {
                print(key)
                print(": ")
                println(resultMap[key])
            }
        }


        return resultMap
    }

    private fun findWordId(inputWordsList: List<List<String>>, wordDB: RocksDB): List<List<String>> {

        val result = mutableListOf<List<String>>()

        for (inputWords in inputWordsList) {
            val phraseList = mutableListOf<String>()
            for(inputWord in inputWords) {
                val wordId = wordDB[inputWord]
                if (wordId != null) phraseList.add(wordId)

                print(inputWord)
                print(":")
                println(wordId)

            }

            result.add(phraseList)
        }

        print("findWordId:")
        println(result)

        return result
    }

}