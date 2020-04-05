package com.github.xjcyan1de.modellangide

import java.awt.Font
import java.awt.GraphicsEnvironment
import javax.swing.JFrame
import javax.swing.JTextArea

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
data class IfStatement(val condition: Expression, val then: StatementList? = null, val orElse: StatementList? = null) : Statement


data class KeyWord(override val value: String) : SimpleExpression
data class Punctuation(override val value: String) : SimpleExpression
data class Operator(override val value: String) : SimpleExpression {
    override fun toString(): String = value
}

val precedenceMap = mapOf("=" to 1, "<" to 5, ">" to 5, "+" to 10, "-" to 10, "*" to 20, "/" to 20)
val keyWords = listOf("if", "else")
val opChars = listOf('@', '+', '-', '*', '/', '=', '!', '>', '<')
val punctuationChars = listOf(';', '{', '}', '(', ')')
val whiteSpaceChars = listOf(' ', '\t', '\n','\r')

fun Char.isDigit() = "[0-9]".toRegex().matches(toString())
fun Char.isLetter() = "[a-z]".toRegex().matches(toString())
fun Char.isOperator() = opChars.contains(this)
fun Char.isPunctuation() = punctuationChars.contains(this)
fun Char.isWhiteSpace() = whiteSpaceChars.contains(this)
fun String.isKeyWord() = keyWords.contains(this)

fun main() {
    GUI
    TextListener


}

