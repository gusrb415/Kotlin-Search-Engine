package util

import org.htmlparser.Parser
import org.htmlparser.beans.LinkBean
import org.htmlparser.beans.StringBean
import org.htmlparser.filters.*
import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException
import java.net.URL
import java.util.*


object HTMLParser {
    private val stopWords = mutableSetOf<String>()
    private val filePath = this::class.java.classLoader.getResource("stopwords.txt").toExternalForm().split("file:/").last()
    init {
        try {
            val reader = BufferedReader(FileReader(filePath))
            var line = reader.readLine()
            while (line != null) {
                stopWords.add(line)
                line = reader.readLine()
            }
            reader.close()
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

    private fun processText(str: String): String {
        return str
            .replace(Regex("[\n\t]"), " ")
            .filter { it.isLetter() || it == ' ' }
            .toLowerCase()
    }

    fun extractText(url: String): List<String> {
        val bean = StringBean()
        bean.url = url
        bean.links = false
        val contents = bean.strings
        val processedText = processText(contents)
        val words = StringTokenizer(processedText)
        val strList = mutableListOf<String>()
        while (words.hasMoreTokens()) {
            val word = words.nextToken()
            if(!isStopWord(word))
//                strList.add(stem(word))
                strList.add(word)
        }
        return strList
    }

    fun extractLink(url: String, filter: String? = null): List<URL> {
        val bean = LinkBean()
        bean.url = url
        return bean.links
            .map { it.toExternalForm() }
            .filter { if(filter != null) it.contains(filter) else true }
            .map { if(it.contains("#")) it.split("#")[0] else it }
            .toSortedSet()
            .map { URL(it) }
    }

    fun getSize(link: String): Int {
        val connection = URL(link).openConnection()
        val contentLength = connection.contentLength
        val newContentLength = if(contentLength == -1)
            connection.getInputStream().readBytes().size
            else -1
        return if(contentLength != -1) contentLength else newContentLength
    }

    fun getTitle(link: String): String {
        val parser = Parser()
        parser.url = link
        val nodeList = parser.extractAllNodesThatMatch(TagNameFilter("title"))
        return nodeList?.elementAt(0)?.lastChild?.toPlainTextString() ?: ""
    }

    fun getDate(link: String): String {
        val parser = Parser()
        parser.url = link
        val connection = URL(link).openConnection()
        val lastModifiedHeader = connection.lastModified
        val textExtraction = if(lastModifiedHeader == 0.toLong())
            parser.extractAllNodesThatMatch(
                AndFilter(TagNameFilter("p"),
                    HasAttributeFilter("class", "copyright")))
                ?.elementAt(0)?.toPlainTextString()?.replace("\\s".toRegex(), "")!!
                .split("on")[1]
        else ""
        return if(lastModifiedHeader != 0.toLong()) Date(lastModifiedHeader).toString() else textExtraction
    }
}