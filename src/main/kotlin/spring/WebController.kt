package spring

import util.Ranker
import model.Result
import org.springframework.stereotype.Controller
import org.springframework.ui.ModelMap
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import util.HTMLParser

@Controller
class WebController {
    private val ranker = Ranker()

    @RequestMapping("/")
    fun index(map: ModelMap): String {
        map.addAttribute("web", Web())
        return "index"
    }

    @PostMapping("/result")
    fun webSubmit(@ModelAttribute web: Web, result: BindingResult, map: ModelMap): String {
        if(result.hasErrors()) return "index"
        val query = web.content
        if(query.isNullOrEmpty())
            return "index"

        //This is where you get query
        //Parse it, modify it do whatever then return
        val startTime = System.currentTimeMillis()
        val rankedItems = ranker.rankDocs(HTMLParser.tokenize(query).toTypedArray())

        val resultList = mutableListOf<Result>()
        for(i in 0 until Math.min(50, rankedItems.size)) {
            resultList.add(Result(rankedItems[i]))
        }

        val timeDiff = (System.currentTimeMillis() - startTime) / 1000.0
        map.addAttribute("timeDiff", "${rankedItems.size} results found (%.2f seconds)".format(timeDiff))
        map.addAttribute("resultList", resultList)
        return "result"
    }
}