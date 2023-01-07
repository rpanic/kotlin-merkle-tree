package com.rpanic.tree

interface VirtualizedStore<T> {
    fun virtualize(prefix: String) : T

    fun merge()

    fun close()

    /**
     * Destroys the Datastore (i.e. deletes it)
     */
    fun destroy()
}

interface ObjectStore<K, V> : VirtualizedStore<ObjectStore<K, V>>{
    fun put(key: K, state: V)
    fun get(key: K) : V?
}

interface MerkleTreeStore<K, V> : VirtualizedStore<MerkleTreeStore<K, V>>{

    fun setNode(key: K, level: Int, value: V)

    fun getNode(key: K, level: Int) : V?

    fun commit()

}