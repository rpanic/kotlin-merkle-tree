package com.rpanic.tree

import com.rpanic.db.LevelDB
import java.nio.file.Path

//fun <K: Fieldable, V : Struct> levelObjectStore(path: Path, encoder: ObjectStoreEncoder<K, V>) = LevelObjectStore(path, encoder, null)

interface ObjectStoreEncoder<K, V>{

    fun decodeKey(bytes: ByteArray) : K
    fun encodeK(v: K) : ByteArray
    fun encodeV(v: V) : ByteArray
    fun decodeValue(bytes: ByteArray) : V

}

open class LevelObjectStore<K : Any, V: Any>(
    private val path: Path,
//    private val keyClazz: KClass<K>,
//    private val valueClazz: KClass<V>,
    private val encoder: ObjectStoreEncoder<K, V>,
    private val parent: ObjectStore<K, V>? = null
) : ObjectStore<K, V> {

    private val db = LevelDB(path)

    override fun put(key: K, state: V) {
        db.put(encoder.encodeK(key), encoder.encodeV(state))
    }

    override fun get(key: K): V? {
        return getInternal(encoder.encodeK(key))
    }

    private fun getInternal(key: ByteArray) : V?{
        return db.get(key)?.let { encoder.decodeValue(it) }
    }

    override fun virtualize(prefix: String): ObjectStore<K, V> {
        return LevelObjectStore(path.resolve(prefix), encoder, this)
    }

    override fun merge() {

        if(parent == null) throw IllegalAccessException("Can´t merge the highest level")

        val keys = db.keyCursor()
        var curr = keys.next()
        while(curr != null){

            parent.put(encoder.decodeKey(curr), getInternal(curr)!!)

            curr = keys.next()

        }
    }

//    private fun encodeValue(v: Fieldable) : ByteArray{
//
//        return v.toFields().map {
//            it.value.toByteArray().padToLength(32)
//        }.reduce { acc, bytes -> acc + bytes }
//    }

//    private fun valuesToFields(v: ByteArray) : List<Field>{
//        return v.toList().chunked(32).map { Field(BigInteger(it.toByteArray())) }
//    }
//
//    private fun decodeValue(v: ByteArray) : V {
//        return Struct.fromFields(this.valueClazz, valuesToFields(v))
//    }
//
//    private fun decodeKey(v: ByteArray) : K {
//        if(keyClazz.isSubclassOf(Field::class)){
//            return keyClazz.primaryConstructor!!.call(BigInteger(v))
//        }else{
//            throw IllegalAccessException("Cant parse Value type")
//        }
//    }

    override fun close(){
        db.close()
    }

    override fun destroy() {
        this.close()
        path.toFile().deleteRecursively()
    }

}

open class LevelTreeStore<K : Any, V : Any>(
    private val path: Path,
    private val encoder: ObjectStoreEncoder<K, V>,
    private val parent: MerkleTreeStore<K, V>? = null) : MerkleTreeStore<K, V> {

    data class Operation(val mode: String, val key: ByteArray, val value: ByteArray)

    private val db = LevelDB(path)

    private val queue = mutableListOf<Operation>()

    private fun getLevelKey(key: K, level: Int) : ByteArray{
        return encoder.encodeK(key) + level.toByte()
    }

    override fun setNode(key: K, level: Int, value: V) {
        queue += Operation("put", getLevelKey(key, level), encoder.encodeV(value))
    }

    override fun getNode(key: K, level: Int): V? {
        val value = db.get(getLevelKey(key, level))
        return if(value != null) encoder.decodeValue(value) else parent?.getNode(key, level)
    }

    override fun commit() {
        db.batch {
            queue.forEach {
                this.put(it.key, it.value)
            }
        }
    }

    override fun close(){
        db.close()
    }

    override fun destroy() {
        this.close()
        path.toFile().deleteRecursively()
    }

    override fun virtualize(prefix: String): MerkleTreeStore<K, V> {
        return LevelTreeStore(path.resolve(prefix), encoder, this)
    }

    override fun merge() {

        if(parent == null) throw IllegalAccessException("Can´t merge the highest MerkleTree")

        val keys = db.keyCursor()
        var curr = keys.next()
        while(curr != null){

            val level = curr.last().toInt()
            val key = encoder.decodeKey(curr.sliceArray(0 until curr.lastIndex))

            parent.setNode(key, level, getNode(key, level)!!)

            curr = keys.next()

        }
    }


}