package com.github.xjcyan1de.modellangide

interface Token<T> {
    val value: T? get() = null
}

interface Expression
data class BinaryExpression(val operator: Operator, val left: Expression, val right: Expression) : SimpleExpression<String> {
    override fun toString(): String = "($left$operator$right)"
}

interface SimpleExpression<T> : Expression, Token<T>
data class Identifier(override val value: String) : SimpleExpression<String> {
    override fun toString() = value
}

data class IntegerExpression(override val value: String) : SimpleExpression<String> {
    override fun toString(): String = value
}

interface Statement
class StatementList(private val list: MutableList<Statement> = ArrayList()) : Token<MutableList<Statement>>, MutableList<Statement> by list {
    override fun toString(): String = list.toString()
}

data class ExpressionStatement(val expression: Expression) : Statement
data class AssignStatement(val identifier: Identifier, val expression: Expression) : Statement
data class IfStatement(val condition: Expression, val then: StatementList? = null, val orElse: StatementList? = null) : Statement


data class KeyWord(override val value: String) : SimpleExpression<String>
data class Punctuation(override val value: String) : SimpleExpression<String>
data class Operator(override val value: String) : SimpleExpression<String> {
    override fun toString(): String = value
}

val precedenceMap = mapOf("=" to 1, "<" to 5, ">" to 5, "+" to 10, "-" to 10, "*" to 20, "/" to 20)
val keyWords = listOf("if", "else")
val opChars = listOf('@', '+', '-', '*', '/', '=', '!', '>', '<')
val punctuationChars = listOf(';', '{', '}', '(', ')')
val whiteSpaceChars = listOf(' ', '\t', '\n','\r')

fun Char.isLetter() = "[a-z]".toRegex().matches(toString())
fun Char.isOperator() = opChars.contains(this)
fun Char.isPunctuation() = punctuationChars.contains(this)
fun Char.isWhiteSpace() = whiteSpaceChars.contains(this)
fun String.isKeyWord() = keyWords.contains(this)

fun main() {
    GUI
    TextListener


}

