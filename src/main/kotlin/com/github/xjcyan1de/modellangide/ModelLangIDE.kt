package com.github.xjcyan1de.modellangide

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
    val value: String? get() = null
}

interface Expression : Statement
interface SimpleExpression : Expression, Token

data class IntegerExpression(override val value: String) : SimpleExpression
data class ConditionExpression(val operator: Operator, val left: Expression, val right: Expression) : Expression

interface Statement
class StatementList(val list: MutableList<Statement> = ArrayList()) : Token, MutableList<Statement> by list {
    override fun toString(): String = list.toString()
}

data class ExpressionStatement(val expression: Expression) : Statement
data class AssignStatement(val identifier: Identifier, val expression: Expression) : Statement
data class IfStatement(val condition: Expression, val then: StatementList, val orElse: StatementList? = null) : Statement

data class Identifier(override val value: String) : SimpleExpression
data class KeyWord(override val value: String) : SimpleExpression
data class Punctuation(override val value: String) : SimpleExpression
data class Operator(override val value: String) : SimpleExpression

val keyWords = listOf("if","else")
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
        if (second - 19) {
          x + 1;
        }
        x*second + second/x*3;
        
        
    """.trimIndent()

    val charReader = CharReader(text.toCharArray())
    val expressionReader = ExpressionReader(charReader)

    Parser(expressionReader).parse()

//    while (expressionReader.hasNext()) {
//        println(expressionReader.next())
//    }
}

class Parser(val reader: ExpressionReader) {
    inline fun <reified T : Token> skipToken(value: String) = skipToken(T::class, value)
    fun <T : Token> skipToken(tokenClass: KClass<T>, value: String) {
        if (isToken(tokenClass, value)) reader.next()
        else reader.error("Expecting ${tokenClass::class.simpleName}: '$value'")
    }

    inline fun <reified T : Token> isToken(value: String? = null): Boolean = isToken(T::class, value)
    fun <T : Token> isToken(tokenClass: KClass<T>, value: String? = null): Boolean {
        val expression = reader.peek()
        return expression != null && expression::class.isSuperclassOf(tokenClass) && if (value != null) expression.value == value else true
    }

    fun parse() {
        parseBlock()
    }

    fun parseBlock(): StatementList {
        val statementList = StatementList()
        val closing: Boolean = isToken<Punctuation>("{")
        if (closing) {
            skipToken<Punctuation>("{")
        }
        while (reader.hasNext()) {
            if (closing && isToken<Punctuation>("}")) {
                skipToken<Punctuation>("}")
                break
            }
            val statement = parseStatement()
            statementList.add(statement)
            if (reader.hasNext()) skipToken<Punctuation>(";")
        }
        return statementList
    }

    fun parseStatement(): Statement {
        return when {
            isToken<Operator>("@") -> parseAssignStatement()
            isToken<KeyWord>("if") -> parseIf()
            isToken<Identifier>() -> ExpressionStatement(parseExpression())
            else -> reader.error("Unexpected statement ${reader.peek()}")
        }.also {
            println("Parsed statement = $it")
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

    fun parseExpression(): Expression {
        val left = reader.next() as SimpleExpression
        val operator = reader.next() as Operator
        val right = reader.next() as SimpleExpression

        return ConditionExpression(operator, left, right)
    }

    fun parseIf(): IfStatement {
        skipToken<KeyWord>("if")
        skipToken<Punctuation>("(")
        val condition = parseExpression()
        skipToken<Punctuation>(")")

        println("condition = $condition next=${reader.peek()}")

        val then = if (isToken<Punctuation>("{")) {
            parseBlock()
        } else error("sorry")

        if (isToken<KeyWord>("else")) {

        }

        println("MY CONDITIONNNN =$condition then=$then")

        TODO()
    }
}



