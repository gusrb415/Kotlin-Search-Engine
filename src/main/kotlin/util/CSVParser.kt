package util

import au.com.bytecode.opencsv.CSVParser
import au.com.bytecode.opencsv.CSVWriter
import java.io.StringWriter

object CSVParser {
    private val parser = CSVParser()

    fun parseFrom(csvString: String): List<String> {
        return parser.parseLine(csvString).toList()
    }

    fun parseTo(list: List<String>): String {
        val stringWriter = StringWriter()
        val writer = CSVWriter(stringWriter)

        writer.writeNext(list.toTypedArray())
        val returnString = stringWriter.toString()
        writer.close()
        stringWriter.close()
        return returnString.substring(0, returnString.length - 1)
    }

    fun parseTo(triple: Triple<String, String, String>): String {
        val stringWriter = StringWriter()
        val writer = CSVWriter(stringWriter)

        writer.writeNext(arrayOf(triple.first, triple.second, triple.third))
        val returnString = stringWriter.toString()
        writer.close()
        stringWriter.close()
        return returnString.substring(0, returnString.length - 1)
    }
}