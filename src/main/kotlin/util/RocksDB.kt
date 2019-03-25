package util

import org.rocksdb.Options
import org.rocksdb.RocksDB
import java.lang.NullPointerException

class RocksDB(path: String) {
    private var options: Options
    private var rocksDB: RocksDB

    init {
        RocksDB.loadLibrary()
        options = Options().setCreateIfMissing(true)
        options.setAllowMmapReads(true)
        options.setAllowMmapWrites(true)
        rocksDB = RocksDB.open(options, path)
    }

    fun close() {
        rocksDB.close()
        options.close()
    }

    private fun addEntry(word: String, x: Int, y: Int) {
        // Add a "docX Y" entry for the key "word" into hashtable
        synchronized(this) {
            var content = get(word)
            content = if (content == null) {
                "doc$x $y"
            } else {
                "$content doc$x $y"
            }
            put(word, content)
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
        while (iter.isValid) {
            println(String(iter.key()) + "=" + String(iter.value()))
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
                .split("doc")
                .flatMap { it.split(" ") }
                .filter { it != "" }
                .map{ it.toInt() }
            for(i in 0 until values.size step 2)
                if(values[i] == docID) ++count
            if(count != 0)
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

    fun getKey(value: String): String? {
        val iter = rocksDB.newIterator()
        iter.seekToFirst()
        while (iter.isValid) {
            if(String(iter.value()) == value)
                return String(iter.key())
            iter.next()
        }
        return null
    }

    private fun put(key: String, value: String) {
        rocksDB.put(key.toByteArray(), value.toByteArray())
    }

    operator fun set(word: String, docID: Int, pos: Int) {
        addEntry(word, docID, pos)
    }

    operator fun set(url: String, docID: Int) {
        put(url, docID.toString())
    }

    operator fun get(key: String?): String?{
        return try {
            String(rocksDB.get(key?.toByteArray() ?: throw NullPointerException()))
        } catch (e: Exception) {
            null
        }
    }

    operator fun get(key: ByteArray?): ByteArray?{
        return try {
            rocksDB.get(key ?: throw NullPointerException())
        } catch (e: Exception) {
            null
        }
    }
}