package com.github.xjcyan1de.modellangide

class TokenReader(val charReader: CharReader) {
    var current: SimpleExpression? = null
    var next: SimpleExpression? = null

    fun readWhile(predicate: (Char) -> Boolean): String {
        val sb = StringBuilder()
        while (charReader.hasNext() && predicate(charReader.peek())) {
            sb.append(charReader.next())
        }
        return sb.toString()
    }

    fun readInt(): IntegerExpression {
        val string = readWhile { it.isDigit() }
        return IntegerExpression(string)
    }

    fun readId(): Identifier {
        val id = readWhile { it.isLetter() }
        return Identifier(id)
    }

    fun readEscaped(endChar: Char): String {
        var escaped = false
        val sb = StringBuilder()
        while (charReader.hasNext()) {
            val ch = charReader.next()
            if (escaped) {
                sb.append(ch)
                escaped = false
            } else if (ch == '\\') {
                escaped = true
            } else if (ch == endChar) {
                break
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun skipComment() {
        readWhile { it != '\n' }
        charReader.next()
    }

    fun readNext(): SimpleExpression? {
        readWhile { it.isWhiteSpace() }
        if (!charReader.hasNext()) return null
        val ch = charReader.peek()
        return when {
            ch.isDigit() -> readInt()
            ch.isPunctuation() -> Punctuation(charReader.next().toString())
            ch.isOperator() -> Operator(readWhile { it.isOperator() })
            ch.isLetter() -> {
                val value = readWhile { it.isLetter() }
                if (value.isKeyWord()) KeyWord(value) else Identifier(value)
            }
            else -> charReader.error("Can't handle character: '$ch'")
        }
    }

    fun peek(): SimpleExpression? = current ?: readNext()?.also { current = it }

    fun next(): SimpleExpression? {
        val expression = current ?: readNext()
        current = null
        next = peek()
        return expression
    }

    fun hasNext(): Boolean = peek() != null

    fun error(message: String): Nothing = charReader.error(message)
}