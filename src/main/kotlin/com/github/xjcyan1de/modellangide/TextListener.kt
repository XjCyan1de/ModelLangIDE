package com.github.xjcyan1de.modellangide

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object TextListener : CoroutineScope by GlobalScope {
    private var lastText: String = ""
    private var lastParseResult = StatementList()

    init {
        launch {
            while (true) {
                delay(500)
                val newText = GUI.textPane.text.trim()
                if (lastText != newText) {
                    checkText(newText)
                    lastText = newText
                }
            }
        }
    }

    private fun checkText(text: String) {
        try {
            val parser = Parser(TokenReader(CharReader(text.toCharArray())))
            val parseResult = parser.parse()
            checkStatements(lastParseResult, parseResult)
            lastParseResult = parseResult
        } catch (e: CharReader.ReaderException) {
            println(e.localizedMessage)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun checkStatements(lastList: List<Statement>, currentList: List<Statement>) {
        val lastIterator = lastList.listIterator()
        val currentIterator = currentList.listIterator()

        while (lastIterator.hasNext() && currentIterator.hasNext()) {
            val lastStatement = lastIterator.next()
            val currentStatement = currentIterator.next()

            if (lastStatement == currentStatement) continue

            if (currentStatement is IfStatement) {
                if (lastStatement is IfStatement && lastStatement.then.isEmpty()) {
                    if (compareStatements(currentStatement.then.listIterator(), lastList.listIterator(lastIterator.nextIndex()))) {
                        showFramingIfNotification()
                    }
                } else if (compareStatements(currentStatement.then.listIterator(), lastList.listIterator(lastIterator.previousIndex()))) {
                    showFramingIfNotification()
                }
            }
        }
    }

    private fun compareStatements(first: ListIterator<Statement>, second: ListIterator<Statement>): Boolean {
        if (first.hasNext() != second.hasNext()) return false
        while (first.hasNext() && second.hasNext()) {
            if (first.next() != second.next()) return false
        }
        return true
    }

    private fun showFramingIfNotification() = GUI.openDialogWindow("Добавлен обрамляющий IF")
}