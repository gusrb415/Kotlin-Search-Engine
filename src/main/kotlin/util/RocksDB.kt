package util

import org.rocksdb.Options
import org.rocksdb.RocksDB
import org.rocksdb.RocksDBException
import java.io.File
import java.lang.NullPointerException

class RocksDB(path: String) {
    private var options: Options
    private var rocksDB: RocksDB

    init {
        RocksDB.loadLibrary()
        options = Options().setCreateIfMissing(true)
        options.setAllowMmapReads(true)
        options.setAllowMmapWrites(true)
        rocksDB = try {
            RocksDB.open(options, path)
        } catch (e: RocksDBException) {
            val dir = path.split("/")[0]
            File(dir).mkdir()
            RocksDB.open(options, path)
        }
    }

    fun close() {
        try {
            rocksDB.close()
            options.close()
        } catch (ignored: Exception) {
        }
    }

    fun remove(word: String) {
        rocksDB.delete(word.toByteArray())
    }

    fun remove(word: ByteArray) {
        rocksDB.delete(word)
    }

    fun removeAll() {
        val iter = rocksDB.newIterator()

        iter.seekToFirst()
        while (iter.isValid) {
            remove(iter.key())
            iter.next()
        }
    }

    fun printAll() {
        val iter = rocksDB.newIterator()

        iter.seekToFirst()
        var counter = 0
        val sb = StringBuilder()
        while (iter.isValid) {
            sb.append(String(iter.key())).append(": ").append(String(iter.value()))
            if (++counter % 10 == 0) {
                println(sb.toString())
                sb.clear()
            } else sb.append(", ")
            iter.next()
        }
    }

    fun findFrequency(docID: Int, wordDB: util.RocksDB): Map<String, Int> {
        val iter = rocksDB.newIterator()

        iter.seekToFirst()
        val map = mutableMapOf<String, Int>()
        while (iter.isValid) {
            val key = wordDB.getKey(String(iter.key())) ?: throw NullPointerException()
            var count = 0
            val values = String(iter.value())
                .split("d")
                .flatMap { it.split(" ") }
                .filter { it != "" }
                .map { it.toInt() }
            for (i in 0 until values.size step 2)
                if (values[i] == docID) ++count
            if (count != 0)
                map[key] = count
            iter.next()
        }

        return map
    }

    fun getAllKeys(): List<String> {
        val iter = rocksDB.newIterator()
        val mutableList = mutableListOf<String>()
        iter.seekToFirst()
        while (iter.isValid) {
            mutableList.add(String(iter.key()))
            iter.next()
        }
        return mutableList
    }

    fun getAllValues(): List<String> {
        val iter = rocksDB.newIterator()
        val mutableList = mutableListOf<String>()
        iter.seekToFirst()
        while (iter.isValid) {
            mutableList.add(String(iter.value()))
            iter.next()
        }
        return mutableList
    }

    fun getKey(value: String): String? {
        val iter = rocksDB.newIterator()
        iter.seekToFirst()
        while (iter.isValid) {
            if (String(iter.value()) == value)
                return String(iter.key())
            iter.next()
        }
        return null
    }

    private fun put(key: String, value: String) {
        rocksDB.put(key.toByteArray(), value.toByteArray())
    }

    operator fun set(word: String, pair: Pair<Int, Int>) {
        // Add a "docX Y" entry for the key "word" into hashtable
        synchronized(this) {
            var content = get(word)
            content = if (content == null) {
                "d${pair.first} ${pair.second}"
            } else {
                "$content d${pair.first} ${pair.second}"
            }
            put(word, content)
        }
    }

    operator fun set(url: String, docID: Int) {
        put(url, docID.toString())
    }

    operator fun set(docID: String, url: String) {
        put(docID, url)
    }

    operator fun set(docId: String, rank: Double) {
        put(docId, rank.toString())
    }

    operator fun set(url: String, triple: Triple<String, String, String>) {
        val temp = CSVParser.parseTo(triple)
        put(url, temp)
    }

    operator fun set(urlId: Int, linkList: List<Int>) {
        val stringList = mutableListOf<String>()
        linkList.forEach { stringList.add(it.toString()) }
        put(urlId.toString(), CSVParser.parseTo(stringList))
    }

    operator fun set(urlId: String, linkList: List<String>) {
        put(urlId, CSVParser.parseTo(linkList))
    }

    operator fun get(key: String?): String? {
        return try {
            String(rocksDB.get(key?.toByteArray() ?: throw NullPointerException()))
        } catch (e: Exception) {
            null
        }
    }

    operator fun get(key: ByteArray?): ByteArray? {
        return try {
            rocksDB.get(key ?: throw NullPointerException())
        } catch (e: Exception) {
            null
        }
    }
}