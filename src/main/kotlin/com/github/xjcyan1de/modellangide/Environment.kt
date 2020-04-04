package com.github.xjcyan1de.modellangide

class Environment(val output: (Int)->Unit) {
    val vars = HashMap<String,Int>()

    fun get(name: String) = vars[name] ?: error("Undefined variable: $name")

    fun set(name: String, value: Int) {
        if(vars.containsKey(name)) {
            vars[name] = value
        } else error("Undefined variable: $name")
    }

    fun define(name: String, value: Int) {
        vars[name] = value
    }

    fun evaluate(list: StatementList) {
        list.forEach {
            evaluate(it)
        }
    }

    fun evaluate(statement: Statement): Unit = when(statement) {
        is AssignStatement -> define(statement.identifier.value, evaluate(statement.expression))
        is ExpressionStatement -> output(evaluate(statement.expression))
        is IfStatement -> {
            val result = evaluate(statement.condition)
            when {
                result > 0 -> evaluate(statement.then)
                statement.orElse != null -> evaluate(statement.orElse)
                else -> Unit
            }
        }
        else -> error("Can't evaluate statement: $statement")
    }

    fun evaluate(expression: Expression): Int = when(expression) {
        is IntegerExpression -> expression.value.toInt()
        is Identifier -> get(expression.value)
        is FramingExpression -> evaluate(expression.value)
        is ConditionExpression -> {
            val left = evaluate(expression.left)
            val right = evaluate(expression.right)
            val operator = expression.operator
            when(operator.value) {
                "+" -> left+right
                "-" -> left-right
                "*" -> left*right
                "/" -> {
                    if (right == 0) error("Divide by zero")
                    left / right
                }
                "<" -> if(left<right) 1 else 0
                ">" -> if(left>right) 1 else 0
                else -> error("Can't apply operator: '${operator.value}'")
            }
        }
        else -> error("Can't evaluate expression: $expression")
    }
}