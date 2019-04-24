package spring

import org.springframework.stereotype.Controller
import org.springframework.ui.ModelMap
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping

@Controller
class WebController {

    @RequestMapping("/")//default mapping
    fun index(map: ModelMap): String {
        map.addAttribute("web",Web())
        return "index" // return src/main/resources/templates/index.html
    }

    @PostMapping("/web")
    fun webSubmit(@ModelAttribute web: Web): String {
        return "result" // return src/main/resources/templates/result.html
}

}