package spring

import main.SpiderMain
import util.Ranker
import org.springframework.stereotype.Controller
import org.springframework.ui.ModelMap
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import util.CSVParser
import util.HTMLParser
import util.RocksDB
import java.text.SimpleDateFormat
import java.util.*

@Controller
class WebController {
    private val urlInfo = RocksDB(SpiderMain.URL_INFO_DB_NAME)
    private val urlChildInfo = RocksDB(SpiderMain.URL_CHILD_DB_NAME)
    private val urlParentInfo = RocksDB(SpiderMain.URL_PARENT_DB_NAME)
    private val pageRank = RocksDB(SpiderMain.PAGE_RANK_DB_NAME)
    private val urlWordCountDB = RocksDB(SpiderMain.URL_WORD_COUNT_DB_NAME)
    private val reverseUrlDB = RocksDB(SpiderMain.REVERSE_URL_DB_NAME)
    private val reverseWordDB = RocksDB(SpiderMain.REVERSE_WORD_DB_NAME)
    private val maxPR = (pageRank.getAllValues().map { it.toDouble() }.max() ?: 1.0) * 3

    @RequestMapping("/")
    fun index(map: ModelMap): String {
        map.addAttribute("web", Web())
        return "index"
    }

    @GetMapping("/result")
    fun invalidGet(map: ModelMap): String {
        map.addAttribute("web", Web())
        return "redirect:"
    }

    @PostMapping("/result")
    fun webSubmit(@ModelAttribute web: Web, result: BindingResult, map: ModelMap): String {
        if (result.hasErrors()) return "redirect:"
        val query = web.content
        if (query.isNullOrEmpty())
            return "redirect:"

        val startTime = System.currentTimeMillis()
        val rawQuery = StringTokenizer(query.replace("\"", ""))
        val queryList = HTMLParser.tokenizeQuery(query)
        val rankedItems = Ranker.rankDocs(queryList)
        print("Ranking took ${(System.currentTimeMillis() - startTime) / 1000.0} seconds, ")

        val meanScore = rankedItems.values.sum() / (rankedItems.size * 3)
        val resultList = mutableListOf<Pair<String, List<Double>>>()

        rankedItems.forEach { urlId, score ->
            val termCount = CSVParser.parseFrom(urlWordCountDB[urlId]!!)[1]
            val normCosScore = score / termCount.toDouble()
            var titleScore = 0.0
            val title = CSVParser.parseFrom(urlInfo[urlId]!!)[0]
            while(rawQuery.hasMoreTokens()) {
                if (title.contains(rawQuery.nextToken(), true))
                    titleScore += meanScore
            }
            val pageRankScore = (pageRank[urlId]?.toDouble() ?: 0.0) / maxPR
            val totalScore = normCosScore + pageRankScore + titleScore
            resultList.add(Pair(urlId, listOf(totalScore, normCosScore, pageRankScore, titleScore)))
        }

        var counter = 0
        val sortedList =
            resultList
            .sortedByDescending { it.second[0] }
            .map { ++counter to it }
            .filter{it.first <= 50}

        val sb = StringBuilder()
        sb.append(
            """
            <table class="table table-striped">
                <thead>
                    <tr>
                        <th scope="col" style="width: 2%">#</th>
                        <th scope="col" style="width: 8%">Score<br>(Cos+PR+Title)</th>
                        <th scope="col" style="width: 18%">Information</th>
                        <th scope="col" style="width: 10%">Top-5 Frequency</th>
                        <th scope="col" style="width: 31%">Parent Link</th>
                        <th scope="col" style="width: 31%">Child Link</th>
                    </tr>
                </thead>
                <tbody>
        """.trimIndent()
        )

        val resultMap = mutableMapOf<Int, String>()
        sortedList.parallelStream().forEach { rankAndItem ->
            val rank = rankAndItem.first
            val urlId = rankAndItem.second.first
            val score = "%.6f".format(rankAndItem.second.second[0])
            val cosScore = "%.4f".format(rankAndItem.second.second[1])
            val pageRankScore = "%.4f".format(rankAndItem.second.second[2])
            val titleScore = "%.4f".format(rankAndItem.second.second[3])

            val childLinks = CSVParser.parseFrom(urlChildInfo[urlId]!!)
            val childUrlSb = StringBuilder()
            childLinks.forEach {
                val url = reverseUrlDB[it]
                if (url != null)
                    childUrlSb.append(
                        """
                        <a href="$url" rel="nofollow" target="_blank">$url</a><br>
                    """.trimMargin()
                    )
            }
            val parentLinks = CSVParser.parseFrom(urlParentInfo[urlId]!!)
            val parentUrlSb = StringBuilder()
            parentLinks.forEach {
                val url = reverseUrlDB[it]
                if (url != null)
                    parentUrlSb.append(
                        """
                        <a href="$url" rel="nofollow" target="_blank">$url</a><br>
                    """.trimMargin()
                    )
            }
            val termCount = CSVParser.parseFrom(urlWordCountDB[urlId]!!)
            var termCountString = ""
            if (termCount.size > 1) {
                val until = Math.min(10, termCount.size)
                for (i in 0 until until step 2)
                    termCountString += "${reverseWordDB[termCount[i]]}: ${termCount[i + 1]}<br>"
            }
            val url = reverseUrlDB[urlId]
            val info = CSVParser.parseFrom(urlInfo[urlId]!!)
            resultMap[rank] = ("""
                <tr>
                    <th scope="row">$rank</th>
                    <td>$score<br>
                    ($cosScore<br>
                    $pageRankScore<br>
                    $titleScore)
                    </td>
                    <td>
                    <a href="$url" rel="nofollow" target="_blank">${info[0]}</a><br>
                    <a href="$url" rel="nofollow" target="_blank">$url</a><br>
                    <b>Last-Modified:</b> ${buildDateFromLong(info[1].toLong())}<br>
                    <b>Size:</b> ${info[2]} Bytes
                    </td>
                    <td>
                    $termCountString
                    </td>
                    <td>
                    <button class="btn btn-info" type="button" data-toggle="collapse"
                    onclick="change(this.id)" id="parColBut$rank"
                    data-target="#parCol$rank" aria-expanded="false" aria-controls="parCol$rank">
                    Show Parent Urls (${parentLinks.size} urls)</button>
                    <div class="collapse" id="parCol$rank">
                    $parentUrlSb
                    </div>
                    </td>
                    <td>
                    <button class="btn btn-info" type="button" data-toggle="collapse"
                    onclick="change(this.id)"id="chiColBut$rank"
                    data-target="#chiCol$rank" aria-expanded="false" aria-controls="chiCol$rank">
                    Show Child Urls (${childLinks.size} urls)</button>
                    <div class="collapse" id="chiCol$rank">
                    $childUrlSb
                    </div>
                    </td>
                </tr>
            """.trimIndent())
        }
        resultMap.toSortedMap().forEach { _, str -> sb.append(str) }
        sb.append("</tbody></table>")
        println("Overall took ${(System.currentTimeMillis() - startTime) / 1000.0} seconds")

        val timeDiff = (System.currentTimeMillis() - startTime) / 1000.0
        map.addAttribute("timeDiff", "${rankedItems.size} results found (%.2f seconds)".format(timeDiff))
        map.addAttribute("result", if (rankedItems.isEmpty()) "" else sb.toString().replace("\n", ""))
        return "result"
    }


    private fun buildDateFromLong(longNumber: Long): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date(longNumber))
    }
}