package spring

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import util.Ranker

@SpringBootApplication
class Application {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            Ranker.initialize()
            val app = SpringApplication(Application::class.java)
            app.setDefaultProperties(mapOf("server.port" to "80"))
            app.run(*args)
        }
    }
}