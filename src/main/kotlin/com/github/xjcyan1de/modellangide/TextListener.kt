package com.github.xjcyan1de.modellangide

import GUI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object TextListener : CoroutineScope by GlobalScope {
    var lastText: String = ""
    var lastParseResult = StatementList()

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

    fun checkText(text: String) {
        try {
            val parser = Parser(TokenReader(CharReader(text.toCharArray())))
            val parseResult = parser.parse()
            checkStatements(parseResult)
            lastParseResult = parseResult
        } catch (e: CharReader.ReaderException) {
          println(e.localizedMessage)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun checkStatements(statementList: StatementList) {
        statementList.forEachIndexed { index, statement ->
            if (statement is IfStatement && statement.then != null && index < lastParseResult.size) {
                val oldIfStatement = lastParseResult[index] as? IfStatement
                if (oldIfStatement != null && oldIfStatement.then == null) {
                    var checkIf = true
                    statement.then.forEachIndexed { ifElementIndex, ifElementStatement ->
                        if(lastParseResult[index+1+ifElementIndex] != ifElementStatement) {
                            checkIf = false
                            return
                        }
                    }
                    if (checkIf) {
                        GUI.openDialogWindow("Добавлен обрамляющий IF")
                    }
                }
            }
        }
    }
}