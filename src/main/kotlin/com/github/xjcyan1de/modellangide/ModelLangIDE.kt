package com.github.xjcyan1de.modellangide

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

interface Token

interface Expression : Statement
interface SimpleExpression : Expression {
    val value: Any
}

data class IdentifierSimpleExpression(override val value: Identifier) : SimpleExpression
data class IntegerSimpleExpression(override val value: Int) : SimpleExpression
data class ConditionExpression(val operator: Operator, val left: Expression, val right: Expression) : Expression
data class BinaryExpression(val operator: Operator, val left: Expression, val right: Expression) : Expression

interface Statement : Token
class StatementList(val list: MutableList<Statement> = ArrayList()) : Token, MutableList<Statement> by list {
    override fun toString(): String = list.toString()
}

data class AssignStatement(val identifier: Identifier, val expression: Expression) : Statement
data class IfStatement(val expression: Expression) : Statement

data class Identifier(override val value: String) : SimpleExpression
data class KeyWord(override val value: String) : SimpleExpression
data class Punctuation(override val value: Char) : SimpleExpression
data class Operator(override val value: String) : SimpleExpression

val keyWords = listOf("if")
val opChars = listOf('@','+', '-', '*', '/', '=', '!')
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
        x + second;
        if (second - 19) {
          x + 1;
        }
        x*second + second/x*3;
    """.trimIndent()

    val charReader = CharReader(text.toCharArray())
    val tokenReader = TokenReader(charReader)


    while (tokenReader.hasNext()) {
        println(tokenReader.next())
    }
}



