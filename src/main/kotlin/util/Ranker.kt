package util

import main.SpiderMain

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

    fun rankDocs(queryTerms: List<String>, spiderDB: RocksDB, wordDB: RocksDB): Map<String, Double> {
        val queryTermIds = findWordId(queryTerms, wordDB)
        val querySize = queryTermIds.size
        val resultMap = mutableMapOf<String, Double>()
        val docLength = mutableMapOf<String, Double>()
        queryTermIds.forEach {
            val docToCount = mutableMapOf<String, Int>()
            val spiderList = CSVParser.parseFrom(spiderDB[it]!!)
            spiderList.forEach {docId ->
                docToCount[docId] = (docToCount[docId] ?: 0) + 1
            }
            val docs = docToCount.keys

            docs.parallelStream().forEach { docId ->
                val tfIdf = CSVParser.parseFrom(tfIdfDB[docId] ?: "")
                var score = 0.0
                val checkNotCalculated = docLength[docId] == null
                for (i in 0 until tfIdf.size step 2) {
                    if (tfIdf[i] == it) {
                        score = tfIdf[i + 1].toDouble()
                    }
                    if (checkNotCalculated) {
                        docLength[docId] = (docLength[docId] ?: 0.0) + Math.pow(tfIdf[i + 1].toDouble(), 2.0)
                    }
                }
                resultMap[docId] = (resultMap[docId] ?: 0.0) + score
            }
        }

        resultMap.forEach { key, value ->
            resultMap[key] = value / (Math.sqrt(docLength[key]!!) * Math.sqrt(querySize.toDouble()))
        }

        return resultMap
    }

    private fun findWordId(inputWords: List<String>, wordDB: RocksDB): List<String> {
        val result = mutableListOf<String>()
        for (inputWord in inputWords) {
            if (inputWord.contains('"')) {
                val word = wordDB[inputWord.replace("\"", "")]
                if (inputWord[0] == '"') {
                    result.add("\"$word")
                } else {
                    result.add("$word\"")
                }
            } else {
                val word = wordDB[inputWord]
                if (word != null) result.add(word)
            }
        }
        return result
    }
}