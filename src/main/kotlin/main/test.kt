package main

import util.CSVParser
import util.HTMLParser
import util.RocksDB
import java.net.URL
import javax.swing.text.html.HTML

fun main(){
    val url = "https://www.cse.ust.hk/News/AlumniDay2015/"
    val urlChildDB = RocksDB(SpiderMain.URL_CHILD_DB_NAME)
    val urlDB = RocksDB(SpiderMain.URL_DB_NAME)
    val linkMatrix = SpiderMain.getMatrix(urlChildDB)

    val id = urlDB[url]!!.toInt()
    var count = 0
    val list = mutableListOf<Int>()
    linkMatrix[id].forEach {
        if(it > 0.0)
            list.add(count)
        ++count
    }

    val a = CSVParser.parseFrom(urlChildDB[id.toString()]!!)
    val new_list = linkMatrix[id].map{ if(it > 0.0) 1 else 0}
    val parentList = list.map { urlDB.getKey(it.toString()) }

//    parentList.forEach {
//        if(!HTMLParser.extractLink(it!!, self=false).contains(URL(url)))
//            println("$it Do not have")
//        else
//            println("$it Have")
//    }
}