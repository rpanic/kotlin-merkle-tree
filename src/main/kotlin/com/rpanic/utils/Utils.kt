package com.rpanic.utils

fun ByteArray.padToLength(length: Int): ByteArray {
    val arr = ByteArray(length)
    val bytes = this
    val diff = length - bytes.size
    bytes.forEachIndexed { index, byte ->
        arr[diff + index] = byte
    }
    return arr
}

interface Cloneable<T>{
    fun clone() : T
}