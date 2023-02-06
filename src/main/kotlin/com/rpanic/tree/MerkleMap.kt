package com.rpanic.tree

import java.math.BigInteger

class MerkleMap<T : BigIntish>(store: MerkleTreeStore<T, T>, options: MerkleTreeOptions<T>) : MerkleTree<T>(store, options, 256) {

    override fun keyToIndex(key: T): T {

        // the bit map is reversed to make reconstructing the key during proving more convenient

        val keyBits = key
            .toBigInt()
            .toString(2)
            .let {  it.reversed() + (0 until (height - it.length)).joinToString(""){ "0" } }
            .slice(0 until height - 1)
            .reversed()
            .map { b -> if(b == '1') 1 else 0 }

        var n = BigInteger.ZERO;
        for (i in keyBits.indices) {
            n += BigInteger.valueOf(2).pow(i) * BigInteger.valueOf(keyBits[i].toLong())
        }

        return TField(n)
    }
}