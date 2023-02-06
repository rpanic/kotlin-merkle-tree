package com.rpanic

import com.rpanic.tree.*
import org.junit.Test
import java.math.BigInteger
import kotlin.test.assertEquals

class MerkleTreeTest {

    fun treeOptions() = MerkleTreeOptions(
        fromBigInt = { b -> BigInt(b) },
        hash = { l -> BigInt(l.map { it.toBigInt() }.reduce { acc, bigInt -> BigInteger("1" + acc.toString() + bigInt.toString()).mod(2.toBigInteger().pow(256)) }) }
    )

    @Test
    fun testWitness(){

        val tree = MerkleTree<BigInt>(
            MemoryMerkleTreeStore(),
            treeOptions()
        )
        tree.set(BigInt("123"), BigInt("3573"))

        val root = tree.getRoot()

        val witness = tree.getWitness(BigInt("123"))
        val root2 = witness.computeRoot(BigInt("3573"), treeOptions())

        assertEquals(root.toBigInt(), root2.toBigInt())

    }

}

class BigInt(private val value: BigInteger) : BigIntish {

    override fun toBigInt() = value

    override fun toString(): String {
        return "$value"
    }

    constructor(s: String) : this(BigInteger(s))

}