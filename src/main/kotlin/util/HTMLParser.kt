package util

import com.sun.javafx.robot.impl.FXRobotHelper.getChildren
import org.htmlparser.Node
import org.htmlparser.Parser
import org.htmlparser.nodes.TagNode
import org.htmlparser.nodes.RemarkNode
import org.htmlparser.nodes.TextNode



object HTMLParser {
    val htmlParser = Parser()
    fun processMyNodes(node: Node) {
        if (node is TextNode) {
            // downcast to TextNode
            val text = node
            // do whatever processing you want with the text
            println(text.text)
        }
        if (node is RemarkNode) {
            // downcast to RemarkNode
            val remark = node
            // do whatever processing you want with the comment
        } else if (node is TagNode) {
            // downcast to TagNode
            val tag = node
            // do whatever processing you want with the tag itself
            // ...
            // process recursively (nodes within nodes) via getChildren()
            val nl = tag.children
            if (null != nl) {
                val i = nl.elements()
                while (i.hasMoreNodes())
                    processMyNodes(i.nextNode())
            }
        }
    }

}