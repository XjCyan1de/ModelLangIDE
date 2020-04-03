package com.github.xjcyan1de.modellangide

class CharReader(val charArray: CharArray) {
    var position: Int = 0
    var line = 1
    var col = 0

    fun next(): Char {
        val c = charArray[position++]
        if (c == '\n') {
            line++
            col = 0
        } else {
            col++
        }
        return c
    }

    fun peek(): Char = charArray[position]

    fun hasNext(): Boolean = position < charArray.size

    fun error(message: String): Nothing = throw ReaderException(message)

    inner class ReaderException(message: String) : Exception("$message ($line:$col)")
}