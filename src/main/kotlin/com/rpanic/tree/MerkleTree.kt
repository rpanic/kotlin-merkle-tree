package com.rpanic.tree

import java.lang.IllegalArgumentException
import java.math.BigInteger

data class MerkleTreeOptions<V>(
    val fromBigInt: (BigInteger) -> V,
    val hash: (List<V>) -> V
)

open class MerkleTree<T : BigIntish>(val store: MerkleTreeStore<T, T>, val options: MerkleTreeOptions<T>, val height: Int = 256) {

    val defaultNodes = generateDefaultNodes();

    val maxIndex = BigInteger.valueOf(2).pow(height - 1)

    private fun generateDefaultNodes() : List<T>{

        val nodes = mutableListOf(TField(0))
        for (i in 1 until height) {
            nodes.add(options.hash(listOf(nodes[i - 1], nodes[i - 1])))
        }
        return nodes

    }

    private fun setNode(level: Int, index: T, value: T) {
        store.setNode(index, level, value)
    }

    fun getNode(level: Int, index: T): T {
        return store.getNode(index, level) ?: defaultNodes[level]
    }

    fun getRoot(): T {
        return this.getNode(this.height - 1, TField(0));
    }

    private fun setLeaf(index: T, value: T){

        if(index.toBigInt() >= maxIndex){
            throw IllegalArgumentException("Index out of range")
        }
        setNode(0, index, value)
        //Recalculate tree
        var c = index.toBigInt()
        var last = index.toBigInt() to value
        for(level in 1 until this.height){

            c /= BigInteger.valueOf(2)

            val mod2Base = c * 2.toBigInteger()
            val left = if(last.first == mod2Base) last.second else getNode(level - 1, TField(mod2Base))
            val right = if(last.first != mod2Base) last.second else getNode(level - 1, TField(mod2Base + BigInteger.ONE))

            val hash = options.hash(listOf(left, right))
            setNode(level, TField(c), hash)

            last = c to hash
        }

        store.commit()

    }

    open fun keyToIndex(key: T) : T {
        return key
    }

    fun set(key: T, value: T) {
        val index = this.keyToIndex(key);
        this.setLeaf(index, value);
    }

    fun get(key: T) : T {
        val index = this.keyToIndex(key);
        return this.getNode(0, index);
    }

    fun getWitness(_index: T): Witness<T> {

        var index = keyToIndex(_index).toBigInt()
        if (index >= this.maxIndex) {
            throw IllegalArgumentException("index $index is out of range for ${this.maxIndex} leaves.")
        }
        val witness = mutableListOf<WitnessEntry<T>>();
        for (level in 0 until this.height - 1) {
            val isLeft = index.mod(2.toBigInteger()) == 0.toBigInteger()
            val sibling = this.getNode(level, TField(
                if(isLeft) index + 1.toBigInteger()
                else index - 1.toBigInteger()
            ));
            witness.add(WitnessEntry(isLeft, sibling))
            index /= 2.toBigInteger()
        }
        return witness
    }

    fun validate(index: T) = validate(index, this.getWitness(index))

    fun validate(_index: T, path: Witness<T>): Boolean {
        val index = keyToIndex(_index)
        val value = this.getNode(0, index)
        val hash = path.computeRoot(value, options)

        return hash.toString() == this.getRoot().toString();
    }

//    fun getDetached(key: String) : MerkleTree<T>{
//        return VirtualizedMerkleTree(height, options, store.virtualize(key), this)
//    }

    protected fun TField(i: Int): T {
        return TField(i.toBigInteger())
    }

    protected fun TField(it: BigInteger): T {
        return options.fromBigInt(it)
    }

}

//data class Witness(val data: List<WitnessStep>)
typealias Witness<T> = List<WitnessEntry<T>>

fun <T : BigIntish> Witness<T>.computeRoot(value: T, options: MerkleTreeOptions<T>) : T{

    var hash = value;

    val isLeft = this.map { it.isLeft };
    val siblings = this.map { it.sibling };

    val Field = { i: Int -> options.fromBigInt(i.toBigInteger())}

    var key = Field(0);

    for (i in isLeft.indices) {
        val left = if(isLeft[i]) hash else siblings[i]
        val right = if(isLeft[i]) siblings[i] else hash
        hash = options.hash(listOf(left, right));

        val bit = if(isLeft[i]) Field(0) else Field(1)

        key = options.fromBigInt(key.toBigInt() * 2.toBigInteger() + bit.toBigInt())
    }

    return hash

}

data class WitnessEntry<T : BigIntish>(val isLeft: Boolean, val sibling: T)

