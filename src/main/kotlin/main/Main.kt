package main

import spring.Application
import java.lang.Exception

class Main {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) = if(args.isEmpty()) {
            SpiderMain.main(arrayOf("500"))
            Application.main(arrayOf())
        } else {
            var i = 0
            if(args.size > 3) {
                println("""Please input only up to 3 arguments. e.g. "spider 300 server" """)
            }
            while(i < args.size) {
                when(args[i].toLowerCase()) {
                     "spider" -> {
                        val minPages = try {
                            args[i + 1].toInt()
                        } catch (e : Exception) {
                            0
                        }
                        if(minPages != 0) ++i
                        SpiderMain.main(if(minPages == 0)arrayOf("100000") else arrayOf(minPages.toString()))
                    }
                    "server" -> Application.main(arrayOf())
                }
                ++i
            }
        }
    }
}