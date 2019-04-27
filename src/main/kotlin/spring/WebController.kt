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
    private val ranker = Ranker()
    private val urlDB = RocksDB(SpiderMain.URL_DB_NAME)
    private val urlInfo = RocksDB(SpiderMain.URL_INFO_DB_NAME)
    private val urlChildInfo = RocksDB(SpiderMain.URL_CHILD_DB_NAME)
    private val urlParentInfo = RocksDB(SpiderMain.URL_PARENT_DB_NAME)
    private val pageRank = RocksDB(SpiderMain.PAGE_RANK_DB_NAME)
    private val urlWordsDB = RocksDB(SpiderMain.URL_WORDS_DB_NAME)
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
        if(result.hasErrors()) return "redirect:"
        val query = web.content
        if(query.isNullOrEmpty())
            return "redirect:"

        val startTime = System.currentTimeMillis()
        val rankedItems = ranker.rankDocs(HTMLParser.tokenize(query).toTypedArray(), urlWordsDB, urlDB)
        val resultList = mutableListOf<Pair<String, Double>>()
        rankedItems.forEach { t, u -> resultList.add(Pair(t, u)) }
        val sortedList = resultList.sortedBy { -it.second }
        
        val sb = StringBuilder()
        var count = 0
        sb.append("""
            <table class="table table-striped">
                <thead>
                    <tr>
                        <th scope="col">#</th>
                        <th scope="col">Score</th>
                        <th scope="col">Information</th>
                        <th scope="col">Keyword Frequency</th>
                        <th scope="col">Parent Link</th>
                        <th scope="col">Child Link</th>
                    </tr>
                </thead>
                <tbody>
        """.trimIndent())
        for (rankedItem in sortedList) {
            val urlId = rankedItem.first
            val score = "%.4f".format(rankedItem.second)
            val pageRank = "%.4f".format(pageRank[urlId]!!.toDouble())
            val childLinks = CSVParser.parseFrom(urlChildInfo[urlId]!!)
            val childUrlSb = StringBuilder()
            childLinks.forEach {
                val url = urlDB.getKey(it)
                childUrlSb.append("""
                    <a href="$url" rel="nofollow" target="_blank">$url</a><br>
                """.trimMargin())
            }
            val parentLinks = CSVParser.parseFrom(urlParentInfo[urlId]!!)
            val parentUrlSb = StringBuilder()
            parentLinks.forEach {
                val url = urlDB.getKey(it)
                parentUrlSb.append("""
                    <a href="$url" rel="nofollow" target="_blank">$url</a><br>
                """.trimMargin())
            }
            val url = urlDB.getKey(urlId)
            val info = CSVParser.parseFrom(urlInfo[urlId]!!)
            sb.append("""
                <tr>
                    <th scope="row">${++count}</th>
                    <td>Cos sim: $score<br>
                    PageRank: $pageRank<br>
                    </td>
                    <td>
                    <a href="$url" rel="nofollow" target="_blank">${info[0]}</a><br>
                    <a href="$url" rel="nofollow" target="_blank">$url</a><br>
                    <b>Last-Modified:</b> ${buildDateFromLong(info[1].toLong())}<br>
                    <b>Size:</b> ${info[2]} Bytes
                    </td>
                    <td>
                    Keywords
                    </td>
                    <td>
                    $parentUrlSb
                    </td>
                    <td>
                    $childUrlSb
                    </td>
                </tr>
            """.trimIndent())
            if(count == 50) break
        }
        sb.append("</tbody></table>")

        val timeDiff = (System.currentTimeMillis() - startTime) / 1000.0
        map.addAttribute("timeDiff", "${rankedItems.size} results found (%.2f seconds)".format(timeDiff))
        map.addAttribute("result", if(rankedItems.isEmpty()) "" else sb.toString())
        return "result"
    }


    private fun buildDateFromLong(longNumber: Long): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date(longNumber))
    }
}