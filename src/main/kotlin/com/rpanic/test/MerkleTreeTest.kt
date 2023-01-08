package com.rpanic.test

import com.rpanic.tree.*
import java.math.BigInteger

class MerkleTreeTest {

    fun treeOptions() = MerkleTreeOptions(
        fromBigInt = { b -> BigInt(b) },
        hash = { l ->
            val new = l.map { it.toBigInt() }.reduce { acc, bigInt -> BigInteger("1" + acc.toString() + bigInt.toString()) }
            BigInt(new.mod(2.toBigInteger().pow(255)))
        }
    )

    fun testWitness(){

        val tree = MerkleTree<BigInt>(
            MemoryMerkleTreeStore(),
            treeOptions(),
            4
        )
        tree.set(BigInt("123"), BigInt("3573"))

        val root = tree.getRoot()

        val witness = tree.getWitness(BigInt("123"))
        val root2 = witness.computeRoot(BigInt("3573"), treeOptions())

        require(root.toBigInt() == root2.toBigInt())

        require(root.toBigInt() != tree.defaultNodes.last().toBigInt())

        println(root.toBigInt())

    }

}

class BigInt(private val value: BigInteger) : BigIntish {

    override fun toBigInt() = value

    override fun toString(): String {
        return "$value"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BigInt

        if (value != other.value) return false

        return true
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    constructor(s: String) : this(BigInteger(s))



}

fun main(){
    println("123")
    MerkleTreeTest().testWitness()
}