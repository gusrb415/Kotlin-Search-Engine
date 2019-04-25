package spring

import org.springframework.stereotype.Controller
import org.springframework.ui.ModelMap
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping

@Controller
class WebController {
    @RequestMapping("/")//default mapping
    fun index(map: ModelMap): String {
        map.addAttribute("web", Web())
        return "index" // return src/main/resources/templates/index.html
    }

    @PostMapping("/web")
    fun webSubmit(@ModelAttribute web: Web, result: BindingResult, map: ModelMap): String {
        if(result.hasErrors()) return "index"
        val query = web.content
        //This is where you get query
        //Parse it, modify it do whatever then return
        map.addAttribute("result", "this is the result string with $query")
        return "result" // return src/main/resources/templates/result.html
    }
}