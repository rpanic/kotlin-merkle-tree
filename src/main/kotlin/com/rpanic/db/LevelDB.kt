package com.rpanic.db

import com.github.shyiko.levelkt.LevelDBBatch
import com.github.shyiko.levelkt.LevelDBCursor
import com.github.shyiko.levelkt.jna.JNALevelDB
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Path

class LevelDB(val path: Path, val tempkey: String? = null) {

    private val db: JNALevelDB = JNALevelDB(path)
    var closed = false

    init {
        Runtime.getRuntime().addShutdownHook(Thread {
            if(!closed){
                db.close()
            }
        })
    }

//    inline fun <reified T> put(key: String, o: T){
//        val s = Json.encodeToString(o)
//        put(key, s)
//    }

    fun put(key: ByteArray, value: ByteArray){
        db.put(key, value)
    }

    fun put(key: String, value: String){
        this.put(key.toByteArray(), value.toByteArray())
    }

    fun get(key: String) : String? {
        return db.get(key.toByteArray())?.let { String(it) }
    }

    fun get(key: ByteArray) : ByteArray? {
        return db.get(key)
    }

    inline fun <reified T> get(key: String) : T?{
        val s = get(key)
        return if(s != null){
            Json.decodeFromString<T>(s)
        } else null
    }

    fun delete(key: String) {
        db.del(key.toByteArray())
    }

    fun batch(f: ExtendedBatch.() -> Unit) {
        db.batch {
            f(ExtendedBatch(this))
        }
    }

    fun keyCursor() : LevelDBCursor<ByteArray> = db.keyCursor()

    fun keys() : List<ByteArray>{
        val cursor = keyCursor()
        val list = mutableListOf<ByteArray>()
        var line: ByteArray?
        while(run {
                line = cursor.next()
                line
            } != null){
            list.add(line!!)
        }
        return list
    }

    fun close(){
        if(!closed){
            db.close()
            closed = true
        }
    }

    class ExtendedBatch(private val batch: LevelDBBatch) : LevelDBBatch {

        override fun del(key: ByteArray) = batch.del(key)

        override fun put(key: ByteArray, value: ByteArray) = batch.put(key, value)

        fun delete(key: String) = batch.del(key.toByteArray())

        fun put(key: String, value: String) = batch.put(key.toByteArray(), value.toByteArray())

        inline fun <reified T> put(key: String, value: T): LevelDBBatch {
            val s = Json.encodeToString(value)
            return put(key, s)
        }

    }

}
