package util

import org.apache.commons.lang3.StringUtils
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
                } else {
                    words.append(data.toChar())
                }
                data = resourceStream.read()
            }
            resourceStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun isStopWord(str: String): Boolean {
        return stopWords.contains(str)
    }

    private fun stem(str: String): String {
        return Porter.stripAffixes(str)
    }

    private fun processText(str: String, query: Boolean): String {
        return str
            .map { if(query && it == '"') '"' else if(it in 'a'..'z' || it in 'A'..'Z') it.toLowerCase() else ' '}
            .joinToString("")
            .replace("\\s".toRegex(), " ")
    }

    fun tokenizeQuery(str: String): List<List<String>> {
        val processedText = processText(str, true)
        val words = StringTokenizer(processedText)
        val result = mutableListOf<List<String>>()
        while (words.hasMoreTokens()) {
            val strList = mutableListOf<String>()

            var word = words.nextToken()
            if (StringUtils.countMatches(word, '"') > 1) {
                strList.add(word.replace("\"", ""))
            } else if (word.contains('"')) {
                var checkEven = false
                strList.add(word.replace("\"", ""))
                while (words.hasMoreTokens()) {
                    word = words.nextToken()
                    if (word.contains('"')) {
                        strList.add(word.replace("\"", ""))
                        checkEven = true
                        break
                    }
                    strList.add(word)
                }

                if (!checkEven && strList.size > 1) {
                    strList.forEach {
                        if (!isStopWord(it))
                            result.add(listOf(stem(it)))
                    }
                    strList.clear()
                }
            } else
                strList.add(word)
            result.add(strList)
        }
        return result.map { list -> list.filter { !isStopWord(it) }.map { stem(it) } }.filter { it.isNotEmpty() }
    }

    fun tokenize(str: String): List<String> {
        val processedText = processText(str, false)
        val words = StringTokenizer(processedText)
        val strList = mutableListOf<String>()
        while (words.hasMoreTokens()) {
            val word = words.nextToken()
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
    fun getAllInfo(link: String): Triple<String, String, String> {
        val parser = Parser()
        parser.url = link

        val connection = URL(link).openConnection()
        val contentLength = connection.contentLength

        val size = try {
            val newContentLength = if (contentLength == -1)
                connection.getInputStream().readBytes().size
            else -1
            if (contentLength != -1) contentLength else newContentLength
        } catch (e: Exception) {
            0
        }

        val title = try {
            val nodeList = parser.extractAllNodesThatMatch(TagNameFilter("title"))
            nodeList?.elementAt(0)?.lastChild?.toPlainTextString() ?: ""
        } catch (e: Exception) {
            "Unauthorized"
        }

        val default = "1990-01-01 00:00:00"
        var lastModifiedHeader = connection.lastModified
        val dateExtraction = if (lastModifiedHeader == 0.toLong()) {
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
        val date = if (lastModifiedHeader != 0.toLong()) lastModifiedHeader else Timestamp.valueOf(dateExtraction).time

        return Triple(title, date.toString(), size.toString())
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