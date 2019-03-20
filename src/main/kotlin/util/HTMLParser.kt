package util

import org.htmlparser.Node
import org.htmlparser.Parser
import org.htmlparser.beans.LinkBean
import org.htmlparser.beans.StringBean
import org.htmlparser.filters.TagNameFilter
import org.htmlparser.nodes.TagNode
import org.htmlparser.nodes.RemarkNode
import org.htmlparser.nodes.TextNode
import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.*


object HTMLParser {
    private val stopWords = mutableSetOf<String>()
    private val parser = Parser()
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

    fun isStopWord(str: String): Boolean {
        return stopWords.contains(str)
    }

    fun stem(str: String): String {
        return Porter.stripAffixes(str)
    }

    fun processMyNodes(node: Node) {
        if (node is TextNode) {
            // do whatever processing you want with the text
            println(node.text)
        }
        if (node is RemarkNode) {
            // do whatever processing you want with the comment
        } else if (node is TagNode) {
            // do whatever processing you want with the tag itself
            // process recursively (nodes within nodes) via getChildren()
            if (node.children != null) {
                val i = node.children.elements()
                while (i.hasMoreNodes())
                    processMyNodes(i.nextNode())
            }
        }
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
                strList.add(stem(word))
        }
        return strList
    }

    fun extractLink(url: String): List<URL> {
        val bean = LinkBean()
        bean.url = url
        val filter = url.split("www.").last().substring(0, 3)
        return bean.links.filter { it.toExternalForm().contains(filter) }.toSet().toList()
    }

    fun getSize(link: String): Int {
        val urlConnection = URL(link).openConnection() as HttpURLConnection
        return urlConnection.contentLength
    }

    fun getTitle(link: String): String {
        parser.url = link
        val nodeList = parser.extractAllNodesThatMatch(TagNameFilter("title"))
        return nodeList?.elementAt(0)?.lastChild?.toPlainTextString() ?: ""
    }

    fun getDate(link: String): String {
        val urlConnection = URL(link).openConnection()
        return Date(urlConnection.date).toString()
    }
}