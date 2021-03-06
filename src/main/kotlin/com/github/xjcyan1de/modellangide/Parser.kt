package com.github.xjcyan1de.modellangide

import kotlin.reflect.KClass

class Parser(private val reader: TokenReader) {
    private inline fun <reified T : Token<*>> skipToken(value: String) = skipToken(T::class, value)
    private fun <T : Token<*>> skipToken(tokenClass: KClass<T>, value: String) {
        if (isToken(tokenClass, value)) reader.next()
        else reader.error("Expecting ${tokenClass.simpleName}: '$value'")
    }

    private inline fun <reified T : Token<*>> isToken(value: String? = null): Boolean = isToken(T::class, value)
    private fun <T : Token<*>> isToken(tokenClass: KClass<T>, value: String? = null): Boolean {
        val expression = reader.peek()
        return expression != null && expression::class == tokenClass && if (value != null) expression.value == value else true
    }

    private inline fun <reified T : Token<*>> expecting(): Nothing = expecting(T::class)
    private fun <T : Token<*>> expecting(tokenClass: KClass<T>): Nothing = reader.error("Expecting ${tokenClass.simpleName}")

    private inline fun <reified T : Token<*>> checkToken() = checkToken(T::class)
    private fun <T : Token<*>> checkToken(tokenClass: KClass<T>) {
        if (!isToken(tokenClass)) expecting(tokenClass)
    }

    fun parse(): StatementList = delimited(null, null)

    private fun parseBlock(): StatementList = delimited("{", "}")

    private fun delimited(start: String? = null, stop: String? = null): StatementList {
        val list = StatementList()
        if (start != null) {
            skipToken<Punctuation>(start)
        }
        while (reader.hasNext()) {
            if (stop != null && isToken<Punctuation>(stop)) break
            val statement = parseStatement()
            list.add(statement)
            if (statement !is IfStatement) skipToken<Punctuation>(";")
        }
        if (stop != null) {
            skipToken<Punctuation>(stop)
        }
        return list
    }


    private fun parseStatement(): Statement {
        return when {
            isToken<Operator>("@") -> parseAssignStatement()
            isToken<KeyWord>("if") -> parseIf()
            isToken<Punctuation>("(") || isToken<Identifier>() || isToken<IntegerExpression>() -> ExpressionStatement(parseExpression())
            else -> reader.error("unexpected statement: ${reader.peek()}")
        }
    }

    private fun parseAssignStatement(): AssignStatement {
        skipToken<Operator>("@")
        checkToken<Identifier>()
        val identifier = reader.next() as Identifier
        skipToken<Operator>("=")
        val expression = reader.next() ?: expecting<SimpleExpression<*>>()
        return AssignStatement(identifier, expression)
    }

    private fun parseAtom(): SimpleExpression<*> {
        return when {
            isToken<Punctuation>("(") -> {
                reader.next()
                val exp = if (!isToken<Punctuation>(")")) parseExpression() else IntegerExpression("0")
                skipToken<Punctuation>(")")
                exp
            }
            else -> {
                val token = reader.next()
                if (token is IntegerExpression || token is Identifier) {
                    return token
                }
                reader.error("Unexpected: $token")
            }
        }
    }

    private fun parseExpression(): SimpleExpression<*> = parseBinary(parseAtom(), 0)

    //
    //  x * 2 + second / x * 3;
    //
    private fun parseBinary(left: Expression, currentPrecedence: Int = 0): SimpleExpression<*> {
        val operator = reader.peek()
        if (operator is Operator) {
            val precedence = precedenceMap[operator.value] ?: -1
            if (precedence > currentPrecedence) {
                reader.next()
                val nextAtom = parseAtom()
                val nextBinary = parseBinary(nextAtom, precedence)
                val nextExp = BinaryExpression(operator, left, nextBinary)
                return parseBinary(nextExp, currentPrecedence)
            }
        }
        return left as? SimpleExpression<*> ?: error("$left can't be as simple expression")
    }

    private fun parseIf(): IfStatement {
        skipToken<KeyWord>("if")

        val condition = parseExpression()

        val then = if (isToken<Punctuation>("{")) {
            parseBlock()
        } else StatementList()

        val orElse = if (isToken<KeyWord>("else")) {
            skipToken<KeyWord>("else")
            if (isToken<Punctuation>("{")) {
                parseBlock()
            } else StatementList().also { it.add(parseStatement()) }
        } else null

        return IfStatement(condition, then, orElse)
    }
}
