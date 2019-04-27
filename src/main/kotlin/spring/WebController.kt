package spring

import util.Ranker
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

    @RequestMapping("/")//default mapping
    fun index(map: ModelMap): String {
        map.addAttribute("web", Web())
        return "index" // return src/main/resources/templates/index.html
    }

    @PostMapping("/web")
    fun webSubmit(@ModelAttribute web: Web, result: BindingResult, map: ModelMap): String {
        if(result.hasErrors()) return "index"
        val query = web.content
        if(query.isNullOrEmpty())
            return "index"

        //This is where you get query
        //Parse it, modify it do whatever then return
        val rankedItems = ranker.rankDocs(HTMLParser.tokenize(query).toTypedArray())
        val sb = StringBuilder()
        rankedItems.forEach {
            sb.append(sb).append('\n')
        }
        map.addAttribute("result", sb.toString())
        return "result" // return src/main/resources/templates/result.html
    }
}