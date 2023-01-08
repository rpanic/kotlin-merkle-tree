package com.rpanic.tree

import com.rpanic.utils.Cloneable
import java.math.BigInteger

open class MemoryObjectStore<K, V : Cloneable<V>>(
    private val _parent: ObjectStore<K, V>? = null
) : ObjectStore<K, V>
{

    private val store = mutableMapOf<K, V>()

    override fun virtualize(prefix: String): ObjectStore<K, V> {
        return MemoryObjectStore(this)
    }

    override fun merge() {
        if(_parent == null) throw IllegalAccessException("Can´t merge the highest level")

        store.forEach { (k, v) ->
            _parent.put(k, v)
        }
    }

    override fun put(key: K, value: V) {
        store[key] = value
    }

    override fun get(key: K): V? {
        return (store[key] ?: _parent?.get(key))?.clone()
    }

    override fun close() {
    }

    override fun destroy() {
        this.store.clear()
    }
}

open class MemoryMerkleTreeStore<T : BigIntish>(val parent: MerkleTreeStore<T, T>? = null) : MerkleTreeStore<T, T> {

    val store = mutableMapOf<Int, MutableMap<T, T>>()

//    fun setNodeRaw(key: BigInteger, level: Int, value: T) {
//        store.getOrPut(level) { mutableMapOf() }[key] = value
//    }

    override fun setNode(key: T, level: Int, value: T) {
        store.getOrPut(level) { mutableMapOf() }[key] = value
    }

    override fun getNode(key: T, level: Int): T? {
        return store[level]?.get(key) ?: parent?.getNode(key, level)
    }

    override fun commit() {
    }

    override fun close() {
    }

    override fun destroy() {
        store.clear()
    }

    override fun virtualize(prefix: String): MerkleTreeStore<T, T> {
        return MemoryMerkleTreeStore(this)
    }

    override fun merge() {

        if(parent == null) throw IllegalAccessException("Can´t merge the highest MerkleTree")

        store.forEach { (level, values) ->
            values.forEach { (index, value) ->
                parent.setNode(index, level, value)
            }
        }
        parent.commit()
    }
}