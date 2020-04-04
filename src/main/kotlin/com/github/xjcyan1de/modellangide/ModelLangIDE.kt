package com.github.xjcyan1de.modellangide

import java.util.*
import kotlin.collections.ArrayList
import kotlin.reflect.KClass
import kotlin.reflect.full.isSuperclassOf

/*
Program → StatementList
StatementList → empty | StatementList Statement
Statement → ExpressionStatement | IfStatement | AssignStatement | BlockStatement
ExpressionStatement -> Expression ;
IfStatement → if ( Expression ) Statement
AssignStatement → @ Identifier = Expression ;
BlockStatement → { StatementList }
Expression → ConditionExpression
ConditionExpression -> PlusMinusExpression | PlusMinusExpression < PlusMinusExpression | PlusMinusExpression > PlusMinusExpression
PlusMinusExpression → MultiplyDivisionExpression | PlusMinusExpression + MultiplyDivisionExpression | PlusMinusExpression - MultiplyDivisionExpression
MultiplyDivisionExpression → SimpleExpression | MultiplyDivisionExpression * SimpleExpression | MultiplyDivisionExpression / SimpleExpression
SimpleExpression → Identifier | Integer | ( Expression )
Identifier → Идентификатор из латинских букв
Integer → Целое число, помещающееся в тип int в Java

Между любыми лексемами программы могут быть пробельные символы (собственно пробелы, табуляции, переносы строк).

Особенности семантики языка
* Если Statement является ExpressionStatement’ом, то результат выражения идёт в вывод программы на этом языке.
* Результаты всех выражений имеют тип int
* Тело if’а выполняется, если его условие не равно 0.
* Операторы сравнения < и > возвращают 1, если соотношение истинно и 0, если ложно.
 */

interface Token {
    val value: Any? get() = null
}

interface Expression
data class ConditionExpression(val operator: Operator, val left: Expression, val right: Expression) : Expression {
    override fun toString(): String = "($left$operator$right)"
}

interface SimpleExpression : Expression, Token
data class Identifier(override val value: String) : SimpleExpression {
    override fun toString() = value
}

data class IntegerExpression(override val value: String) : SimpleExpression {
    override fun toString(): String = value
}

data class FramingExpression(override val value: Expression) : SimpleExpression {
    override fun toString(): String = value.toString()
}


interface Statement
class StatementList(val list: MutableList<Statement> = ArrayList()) : Token, MutableList<Statement> by list {
    override fun toString(): String = list.toString()
}

data class ExpressionStatement(val expression: Expression) : Statement
data class AssignStatement(val identifier: Identifier, val expression: Expression) : Statement
data class IfStatement(val condition: Expression, val then: StatementList, val orElse: StatementList? = null) : Statement


data class KeyWord(override val value: String) : SimpleExpression
data class Punctuation(override val value: String) : SimpleExpression
data class Operator(override val value: String) : SimpleExpression {
    override fun toString(): String = value
}

val precedenceMap = mapOf("=" to 1, "<" to 5, ">" to 5, "+" to 10, "-" to 10, "*" to 20, "/" to 20)
val keyWords = listOf("if", "else")
val opChars = listOf('@', '+', '-', '*', '/', '=', '!')
val punctuationChars = listOf(';', '{', '}', '(', ')')
val whiteSpaceChars = listOf(' ', '\t', '\n')

fun Char.isDigit() = "[0-9]".toRegex().matches(toString())
fun Char.isLetter() = "[a-z]".toRegex().matches(toString())
fun Char.isOperator() = opChars.contains(this)
fun Char.isPunctuation() = punctuationChars.contains(this)
fun Char.isWhiteSpace() = whiteSpaceChars.contains(this)
fun String.isKeyWord() = keyWords.contains(this)

fun main() {
    val text = """
        @x = 10; @second = 20;

        x * 2 + second / x * 3;
        
        
    """.trimIndent()

    val charReader = CharReader(text.toCharArray())
    val expressionReader = TokenReader(charReader)

    val a = TokenReader(CharReader(text.toCharArray()))
    while (a.hasNext()) {
        println(a.next())
    }

    println("\nparsing...\n")
    Parser(expressionReader).parse()


//    while (expressionReader.hasNext()) {
//        println(expressionReader.next())
//    }
}

class Parser(val reader: TokenReader) {
    inline fun <reified T : Token> skipToken(value: String) = skipToken(T::class, value)
    fun <T : Token> skipToken(tokenClass: KClass<T>, value: String) {
        if (isToken(tokenClass, value)) reader.next()
        else reader.error("Expecting ${tokenClass.simpleName}: '$value'")
    }

    inline fun <reified T : Token> isToken(value: String? = null): Boolean = isToken(T::class, value)
    fun <T : Token> isToken(tokenClass: KClass<T>, value: String? = null): Boolean {
        val expression = reader.peek()
        return expression != null && expression::class.isSuperclassOf(tokenClass) && if (value != null) expression.value == value else true
    }

    fun parse() {
        parseBlock()
    }

    fun parseBlock(): StatementList = delemited("{", "}", ";")

    private fun delemited(start: String, stop: String, separator: String): StatementList {
        val list = StatementList()
        var first = true
        while (reader.hasNext()) {
            if (isToken<Punctuation>(stop)) break
            if (first) {
                first = false
            } else {
                skipToken<Punctuation>(separator)
            }
            if (isToken<Punctuation>(stop)) break
            list.add(parseStatement())
        }
        skipToken<Punctuation>(stop)
        return list
    }


    fun parseStatement(): Statement {
        return when {
            isToken<Operator>("@") -> parseAssignStatement()
            isToken<KeyWord>("if") -> parseIf()
            else -> ExpressionStatement(parseExpression())
        }
    }

    fun parseAssignStatement(): AssignStatement {
        skipToken<Operator>("@")
        isToken<Identifier>()
        val identifier = reader.next() as Identifier
        skipToken<Operator>("=")
        isToken<SimpleExpression>()
        val expression = reader.next() as SimpleExpression
        return AssignStatement(identifier, expression)
    }

    fun parseAtom(): SimpleExpression {
        return when {
            isToken<Punctuation>("(") -> {
                reader.next()
                val exp = parseExpression()
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

    fun parseExpression(): SimpleExpression = parseBinary(parseAtom(), 0)

    //
    //  x * 2 + second / x * 3;
    //
    fun parseBinary(left: Expression, currentPrecedence: Int = 0): SimpleExpression {
        val id = UUID.randomUUID().toString().substring(0,2)
        println("[$id] parse binary: $left prec=$currentPrecedence")

        val operator = reader.peek()

        if (operator is Operator) {
            println("[$id] operator: $operator")
            val precedence = precedenceMap[operator.value] ?: error("")
            if (precedence > currentPrecedence) {
                reader.next()
                println("[$id] priority higher $precedence>$currentPrecedence")
                val nextAtom = parseAtom()
                val nextBinary = parseBinary(nextAtom, precedence)
                val nextExp = ConditionExpression(operator, left, nextBinary)
                println("[$id] next exp = $nextExp")
                val result = parseBinary(nextExp, currentPrecedence)
                println()
                return result
            }
        }
        return if (left is SimpleExpression) left else FramingExpression(left)
    }

    fun parseIf(): IfStatement {
        skipToken<KeyWord>("if")
        skipToken<Punctuation>("(")
        val condition = parseBinary(reader.next()!!)
        skipToken<Punctuation>(")")

        val then = if (isToken<Punctuation>("{")) {
            parseBlock()
        } else StatementList().also { it.add(parseStatement()) }

        val orElse = if (isToken<KeyWord>("else")) {
            skipToken<KeyWord>("else")
            if (isToken<Punctuation>("{")) {
                parseBlock()
            } else StatementList().also { it.add(parseStatement()) }
        } else null

        return IfStatement(condition, then, orElse)
    }
}



