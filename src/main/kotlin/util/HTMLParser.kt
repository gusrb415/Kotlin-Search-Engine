package util

import org.htmlparser.Parser
import org.htmlparser.beans.LinkBean
import org.htmlparser.beans.StringBean
import org.htmlparser.filters.*
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.net.URL
import java.sql.Timestamp
import java.util.*


object HTMLParser {
    private val stopWords = mutableSetOf<String>()
    private val resourceStream = this::class.java.classLoader.getResourceAsStream("stopwords.txt")

    init {
        try {
            var data = resourceStream.read()
            val words = StringBuilder()
            while (data != -1) {
                if(data.toChar() == '\n') {
                    stopWords.add(words.toString())
                    words.clear()
                }
                words.append(data.toChar())
                data = resourceStream.read()
            }
            resourceStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun isStopWord(str: String): Boolean {
        return stopWords.contains(str)
    }

    private fun stem(str: String): String {
        return Porter.stripAffixes(str)
    }

    private fun processText(str: String, query: Boolean): String {
        return str
            .map { if(it in 'a'..'z' || it in 'A'..'Z') it.toLowerCase() else ' '}
            .joinToString("")
            .replace("\\s".toRegex(), " ")
    }

    fun tokenize(str: String, query: Boolean=false): List<String> {
        val processedText = processText(str, query)
        val words = StringTokenizer(processedText)
        val strList = mutableListOf<String>()
        while (words.hasMoreTokens()) {
            val word = words.nextToken()
//            if(query && word.contains('"'))
//                strList.add(word)
//            else
            if(!isStopWord(word))
                strList.add(stem(word))
        }

        return strList.filter { it.length > 2 }
    }

    fun extractText(url: String): List<String> {
        val bean = StringBean()
        bean.url = url
        bean.links = false
        return tokenize(bean.strings)
    }

    fun extractLink(url: String, filter: String? = null, self: Boolean = true): List<URL> {
        val bean = LinkBean()
        bean.url = url
        return bean.links
            .map { it.toExternalForm() }
            .filter { if(filter != null) it.contains(filter, ignoreCase=true) else true }
            .map { if(it.contains("#")) it.split("#")[0] else it }
            .filter { if(!self) it != url else true }
            .toSet()
            .map { URL(it) }
    }

    fun getSize(link: String): Int {
        return try {
            val connection = URL(link).openConnection()
            val contentLength = connection.contentLength
            val newContentLength = if(contentLength == -1)
                connection.getInputStream().readBytes().size
            else -1
            if(contentLength != -1) contentLength else newContentLength
        } catch (e: Exception) {
            0
        }
    }

    fun getTitle(link: String): String {
        return try {
            val parser = Parser()
            parser.url = link
            val nodeList = parser.extractAllNodesThatMatch(TagNameFilter("title"))
            nodeList?.elementAt(0)?.lastChild?.toPlainTextString() ?: ""
        } catch (e: Exception) {
            "Unauthorized"
        }
    }

    fun getDate(link: String): Long {
        val parser = Parser()
        val default = "1990-01-01 00:00:00"
        try {
            parser.url = link
        } catch (e: Exception) { return Timestamp.valueOf(default).time }
        val connection = parser.connection
        var lastModifiedHeader = connection.lastModified
        val dateExtraction = if(lastModifiedHeader == 0.toLong()) {
            try {
                parser.extractAllNodesThatMatch(
                    AndFilter(
                        TagNameFilter("p"),
                        HasAttributeFilter("class", "copyright")
                    )
                )
                    ?.elementAt(0)?.toPlainTextString()?.replace("\\s".toRegex(), "")!!
                    .split("on")[1] + " 00:00:00"
            } catch (e: Exception) {
                lastModifiedHeader = connection.date
                default
            }
        } else default
        return if(lastModifiedHeader != 0.toLong()) lastModifiedHeader else Timestamp.valueOf(dateExtraction).time
    }
}