package main

import org.apache.jdbm.DBMaker
import org.htmlparser.Parser
import org.rocksdb.Options
import org.rocksdb.RocksDB
import org.rocksdb.RocksDBException
import java.lang.IllegalArgumentException

class Main {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
//            val url = "https://hyungyu.me"
//            val parser = Parser(url)
//            val list = parser.parse(null)
//
//            println(list)
//            val database = DBMaker.openFile("test")
//                .closeOnExit()
////                .deleteFilesAfterClose()
//                .enableEncryption("password", true)
//                .make()
//
//            val map = try {
//                database.createTreeMap<Int, String>("testMap1")
//            } catch (e : IllegalArgumentException) {
//                database.getTreeMap<Int, String>("testMap1")
//            }
//
//            map[1] = "hello"
//            map[2] = "world"
//            database.commit()
//
//            println(database.collections)
//
//            database.close()

            RocksDB.loadLibrary()
            val options = try {
                Options().setCreateIfMissing(true)
            } catch (e: RocksDBException) {
                null
            }

            val rocksDB = try {
                RocksDB.open(options, "rockTest")
            } catch (e: RocksDBException) {
                null
            }

            if(rocksDB != null && options != null) {
                rocksDB.put("hello".toByteArray(), "world".toByteArray())
                println(String(rocksDB.get("hello".toByteArray())))
            }

            rocksDB?.close()
            options?.close()
        }
    }
}